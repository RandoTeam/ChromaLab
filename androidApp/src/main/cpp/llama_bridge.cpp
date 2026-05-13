/**
 * llama_bridge.cpp — JNI bridge between Kotlin and llama.cpp + mtmd (multimodal).
 *
 * Uses the current (b9101, May 2026) API:
 *   - llama_model_load_from_file / llama_model_free
 *   - llama_init_from_model / llama_free
 *   - mtmd_init_from_file / mtmd_free (vision via mmproj)
 *   - mtmd_helper_bitmap_init_from_file + mtmd_tokenize + mtmd_helper_eval_chunks
 *
 * Thread safety: each ModelContext is used from a single thread at a time.
 * The Kotlin side ensures this via Dispatchers.IO + single-job pattern.
 */

#include <jni.h>
#include <string>
#include <vector>
#include <cstring>
#include <algorithm>
#include <atomic>
#include <chrono>
#include <cctype>
#include <thread>
#include <android/log.h>

#include "llama.h"
#include "mtmd.h"
#include "mtmd-helper.h"

#define LOG_TAG "LlamaBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * Holds all state for a loaded model + vision encoder.
 * Pointer cast to jlong for JNI handle.
 */
struct ModelContext {
    llama_model   *model   = nullptr;
    llama_context *ctx     = nullptr;
    mtmd_context  *mtmd    = nullptr;   // null if no mmproj provided
    int            n_ctx   = 0;
    int            n_batch = 512;
};

static llama_sampler * create_sampler(
    float temperature,
    float topP,
    int topK,
    float repeatPenalty,
    int repeatLastN) {

    const float rep_penalty = (repeatPenalty > 0.0f) ? repeatPenalty : 1.1f;
    const int rep_last_n = (repeatLastN > 0) ? repeatLastN : 64;

    llama_sampler_chain_params sparams = llama_sampler_chain_default_params();
    sparams.no_perf = true;
    llama_sampler *smpl = llama_sampler_chain_init(sparams);

    if (rep_penalty > 1.0f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_penalties(
            rep_last_n,
            rep_penalty,
            0.0f,
            0.0f
        ));
    }

    if (temperature <= 0.0f) {
        llama_sampler_chain_add(smpl, llama_sampler_init_greedy());
    } else {
        if (topK > 0) {
            llama_sampler_chain_add(smpl, llama_sampler_init_top_k(topK));
        }
        if (topP > 0.0f && topP < 1.0f) {
            llama_sampler_chain_add(smpl, llama_sampler_init_top_p(topP, 1));
        }
        llama_sampler_chain_add(smpl, llama_sampler_init_temp(temperature));
        llama_sampler_chain_add(smpl, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
    }

    return smpl;
}

static bool prompt_requests_json(const char *prompt) {
    if (!prompt) return false;
    std::string text(prompt);
    std::transform(text.begin(), text.end(), text.begin(), [](unsigned char c) {
        return (char)std::tolower(c);
    });
    return text.find("json") != std::string::npos ||
           text.find("\"x\"") != std::string::npos ||
           text.find("left_pct") != std::string::npos;
}

static bool json_object_complete(const std::string &text) {
    const size_t start = text.find('{');
    if (start == std::string::npos) return false;

    int depth = 0;
    bool in_string = false;
    bool escaped = false;

    for (size_t i = start; i < text.size(); ++i) {
        const char ch = text[i];
        if (escaped) {
            escaped = false;
            continue;
        }
        if (ch == '\\' && in_string) {
            escaped = true;
            continue;
        }
        if (ch == '"') {
            in_string = !in_string;
            continue;
        }
        if (in_string) continue;

        if (ch == '{') {
            depth++;
        } else if (ch == '}') {
            depth--;
            if (depth == 0) return true;
        }
    }
    return false;
}

static bool elapsed_over(
    const std::chrono::steady_clock::time_point &started,
    int limit_ms) {
    const auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now() - started
    ).count();
    return elapsed > limit_ms;
}

static long long elapsed_ms_since(
    const std::chrono::steady_clock::time_point &started) {
    return std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now() - started
    ).count();
}

static size_t count_occurrences(const std::string &text, const std::string &needle) {
    if (needle.empty()) return 0;
    size_t count = 0;
    size_t pos = text.find(needle);
    while (pos != std::string::npos) {
        ++count;
        pos = text.find(needle, pos + needle.size());
    }
    return count;
}

static std::string one_line_preview(const std::string &text, size_t max_chars = 180) {
    std::string out;
    out.reserve(std::min(text.size(), max_chars));
    bool last_space = false;
    for (char ch : text) {
        const bool is_space = std::isspace(static_cast<unsigned char>(ch));
        if (is_space) {
            if (!last_space && !out.empty()) {
                out.push_back(' ');
            }
            last_space = true;
        } else {
            out.push_back(ch);
            last_space = false;
        }
        if (out.size() >= max_chars) {
            out.append("...");
            break;
        }
    }
    return out;
}

static const char * chunk_type_name(enum mtmd_input_chunk_type type) {
    switch (type) {
        case MTMD_INPUT_CHUNK_TYPE_TEXT:  return "text";
        case MTMD_INPUT_CHUNK_TYPE_IMAGE: return "image";
        case MTMD_INPUT_CHUNK_TYPE_AUDIO: return "audio";
    }
    return "unknown";
}

class NativeStageWatchdog {
public:
    NativeStageWatchdog(
        const char *stageName,
        int warnAfterMs = 15000,
        int repeatEveryMs = 15000)
        : stage_name(stageName ? stageName : "unknown"),
          warn_after_ms(warnAfterMs),
          repeat_every_ms(repeatEveryMs),
          started(std::chrono::steady_clock::now()),
          done(false),
          worker(&NativeStageWatchdog::run, this) {
        LOGI("GGUF native stage start: %s", stage_name.c_str());
    }

    ~NativeStageWatchdog() {
        done.store(true, std::memory_order_release);
        if (worker.joinable()) {
            worker.join();
        }
        LOGI("GGUF native stage done: %s elapsed=%lld ms",
             stage_name.c_str(), elapsed_ms_since(started));
    }

    NativeStageWatchdog(const NativeStageWatchdog &) = delete;
    NativeStageWatchdog &operator=(const NativeStageWatchdog &) = delete;

private:
    void run() {
        long long last_warning_ms = 0;
        while (!done.load(std::memory_order_acquire)) {
            std::this_thread::sleep_for(std::chrono::milliseconds(500));
            if (done.load(std::memory_order_acquire)) {
                break;
            }

            const long long elapsed_ms = elapsed_ms_since(started);
            if (elapsed_ms < warn_after_ms) {
                continue;
            }

            if (last_warning_ms == 0 || elapsed_ms - last_warning_ms >= repeat_every_ms) {
                LOGW("GGUF native stage still running: %s elapsed=%lld ms",
                     stage_name.c_str(), elapsed_ms);
                last_warning_ms = elapsed_ms;
            }
        }
    }

    const std::string stage_name;
    const int warn_after_ms;
    const int repeat_every_ms;
    const std::chrono::steady_clock::time_point started;
    std::atomic<bool> done;
    std::thread worker;
};

static std::string build_multimodal_prompt(const char *prompt, const char *marker) {
    std::string text = prompt ? std::string(prompt) : std::string();
    const std::string marker_text = marker ? std::string(marker) : std::string("<__media__>");

    if (text.find(marker_text) != std::string::npos) {
        return text;
    }

    const std::string chatml_user = "<|im_start|>user\n";
    const size_t user_pos = text.find(chatml_user);
    if (user_pos != std::string::npos) {
        const size_t insert_pos = user_pos + chatml_user.size();
        text.insert(insert_pos, marker_text + "\n");
        return text;
    }

    return marker_text + "\n" + text;
}

static std::string run_text_completion(
    ModelContext *mc,
    const char *prompt,
    int maxTokens,
    float temperature,
    float topP,
    int topK,
    float repeatPenalty,
    int repeatLastN) {

    if (!mc || !mc->ctx || !mc->model || !prompt) {
        return "";
    }

    int n_predict = (maxTokens > 0) ? maxTokens : 512;
    if (mc->n_ctx > 0 && n_predict > mc->n_ctx / 2) {
        n_predict = mc->n_ctx / 2;
    }
    const llama_vocab *vocab = llama_model_get_vocab(mc->model);

    const int prompt_len = (int)strlen(prompt);
    std::vector<llama_token> tokens((size_t)prompt_len + 32);
    int n_tokens = llama_tokenize(
        vocab,
        prompt,
        prompt_len,
        tokens.data(),
        (int)tokens.size(),
        true,
        true
    );

    if (n_tokens < 0) {
        tokens.resize((size_t)-n_tokens);
        n_tokens = llama_tokenize(
            vocab,
            prompt,
            prompt_len,
            tokens.data(),
            (int)tokens.size(),
            true,
            true
        );
    }

    if (n_tokens <= 0) {
        LOGE("Text tokenization failed: %d", n_tokens);
        return "";
    }

    LOGI("Text prompt tokenized: chars=%d tokens=%d n_predict=%d ctx=%d batch=%d add_special=1 parse_special=1 preview=%s",
         prompt_len, n_tokens, n_predict, mc->n_ctx, mc->n_batch,
         one_line_preview(std::string(prompt)).c_str());

    tokens.resize((size_t)n_tokens);

    int max_prompt_tokens = mc->n_ctx - n_predict - 4;
    if (max_prompt_tokens > 0 && n_tokens > max_prompt_tokens) {
        tokens.erase(tokens.begin(), tokens.end() - max_prompt_tokens);
        n_tokens = (int)tokens.size();
        LOGI("Prompt truncated to %d tokens", n_tokens);
    }

    llama_memory_clear(llama_get_memory(mc->ctx), true);

    const int batch_size = (mc->n_batch > 0) ? mc->n_batch : 512;
    for (int offset = 0; offset < n_tokens; offset += batch_size) {
        const int n_chunk = std::min(batch_size, n_tokens - offset);
        llama_batch batch = llama_batch_get_one(tokens.data() + offset, n_chunk);
        if (llama_decode(mc->ctx, batch) != 0) {
            LOGE("Text prompt decode failed at offset %d", offset);
            return "";
        }
    }

    std::string result_text;
    llama_sampler *smpl = create_sampler(temperature, topP, topK, repeatPenalty, repeatLastN);
    const bool json_task = prompt_requests_json(prompt);
    const auto started = std::chrono::steady_clock::now();
    const int decode_limit_ms = json_task ? 90000 : 180000;

    for (int i = 0; i < n_predict; i++) {
        llama_token token_id = llama_sampler_sample(smpl, mc->ctx, -1);

        if (llama_vocab_is_eog(vocab, token_id)) {
            break;
        }

        char buf[256];
        int len = llama_token_to_piece(vocab, token_id, buf, sizeof(buf), 0, true);
        if (len > 0) {
            result_text.append(buf, len);
        }
        if (i == 0) {
            LOGI("Text first token: id=%d elapsed=%lld ms", (int)token_id, elapsed_ms_since(started));
        }

        if (json_task && json_object_complete(result_text)) {
            LOGI("Text JSON complete at token %d", i + 1);
            break;
        }
        if (elapsed_over(started, decode_limit_ms)) {
            LOGE("Text decode timed out after %d ms", decode_limit_ms);
            break;
        }

        llama_batch next = llama_batch_get_one(&token_id, 1);
        if (llama_decode(mc->ctx, next) != 0) {
            LOGE("Text decode failed at token %d", i);
            break;
        }
    }

    llama_sampler_free(smpl);
    return result_text;
}

extern "C" {

// ===== nativeGetAvailableBackends =====

JNIEXPORT jintArray JNICALL
Java_com_chromalab_feature_processing_inference_LlamaEngine_nativeGetAvailableBackends(
    JNIEnv *env, jclass /* clazz */) {

    // For now, report CPU only. Vulkan probing can be added later.
    jint backends[] = { 0 }; // 0=CPU
    int count = 1;
    jintArray result = env->NewIntArray(count);
    env->SetIntArrayRegion(result, 0, count, backends);
    return result;
}

// ===== nativeLoadModel =====

JNIEXPORT jlong JNICALL
Java_com_chromalab_feature_processing_inference_LlamaEngine_nativeLoadModel(
    JNIEnv *env, jclass /* clazz */,
    jstring basePath, jstring mmprojPath,
    jint threads, jint backendCode,
    jint contextSize, jint batchSize) {

    const char *base   = env->GetStringUTFChars(basePath, nullptr);
    const char *mmproj = env->GetStringUTFChars(mmprojPath, nullptr);

    LOGI("nativeLoadModel: base=%s, mmproj=%s, threads=%d, ctx=%d, batch=%d",
         base, mmproj ? mmproj : "", threads, contextSize, batchSize);

    // Init backend once (idempotent)
    static bool backend_inited = false;
    if (!backend_inited) {
        llama_backend_init();
        backend_inited = true;
    }

    auto *mc = new ModelContext();

    // 1) Load model
    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // CPU only for now
    LOGI("GGUF load params: backendCode=%d n_gpu_layers=%d threads=%d requested_ctx=%d requested_batch=%d",
         backendCode, model_params.n_gpu_layers, threads, contextSize, batchSize);
    {
        NativeStageWatchdog watchdog("llama_model_load", 30000, 30000);
        mc->model = llama_model_load_from_file(base, model_params);
    }
    if (!mc->model) {
        LOGE("Failed to load model: %s", base);
        env->ReleaseStringUTFChars(basePath, base);
        env->ReleaseStringUTFChars(mmprojPath, mmproj);
        delete mc;
        return 0;
    }

    // 2) Create context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx     = std::clamp((int)contextSize, 1024, 8192);
    ctx_params.n_batch   = std::clamp((int)batchSize, 64, 512);
    ctx_params.n_threads = (threads > 0) ? threads : 4;
    ctx_params.n_threads_batch = ctx_params.n_threads;
    LOGI("GGUF context params: n_ctx=%d n_batch=%d n_threads=%d n_threads_batch=%d",
         (int)ctx_params.n_ctx, (int)ctx_params.n_batch,
         (int)ctx_params.n_threads, (int)ctx_params.n_threads_batch);
    {
        NativeStageWatchdog watchdog("llama_context_init", 30000, 30000);
        mc->ctx = llama_init_from_model(mc->model, ctx_params);
    }
    if (!mc->ctx) {
        LOGE("Failed to create context");
        llama_model_free(mc->model);
        env->ReleaseStringUTFChars(basePath, base);
        env->ReleaseStringUTFChars(mmprojPath, mmproj);
        delete mc;
        return 0;
    }
    mc->n_ctx = (int)llama_n_ctx(mc->ctx);
    mc->n_batch = (int)ctx_params.n_batch;
    LOGI("Context created: n_ctx=%d", mc->n_ctx);

    // 3) Load vision encoder (mmproj) if provided
    if (mmproj && strlen(mmproj) > 0) {
        mtmd_context_params mtmd_params = mtmd_context_params_default();
        mtmd_params.n_threads = ctx_params.n_threads;
        mtmd_params.use_gpu = false; // CPU for now
        LOGI("MTMD params: use_gpu=%d warmup=%d n_threads=%d image_min_tokens=%d image_max_tokens=%d marker=%s",
             mtmd_params.use_gpu ? 1 : 0,
             mtmd_params.warmup ? 1 : 0,
             mtmd_params.n_threads,
             mtmd_params.image_min_tokens,
             mtmd_params.image_max_tokens,
             mtmd_params.media_marker ? mtmd_params.media_marker : "");
        {
            NativeStageWatchdog watchdog("mtmd_init_from_file", 30000, 30000);
            mc->mtmd = mtmd_init_from_file(mmproj, mc->model, mtmd_params);
        }
        if (mc->mtmd) {
            LOGI("MTMD vision encoder loaded: %s", mmproj);
            LOGI("MTMD support: vision=%d audio=%d mrope=%d",
                 mtmd_support_vision(mc->mtmd) ? 1 : 0,
                 mtmd_support_audio(mc->mtmd) ? 1 : 0,
                 mtmd_decode_use_mrope(mc->mtmd) ? 1 : 0);
        } else {
            LOGE("Failed to load mmproj: %s", mmproj);
            llama_free(mc->ctx);
            llama_model_free(mc->model);
            env->ReleaseStringUTFChars(basePath, base);
            env->ReleaseStringUTFChars(mmprojPath, mmproj);
            delete mc;
            return 0;
        }
    }

    env->ReleaseStringUTFChars(basePath, base);
    env->ReleaseStringUTFChars(mmprojPath, mmproj);

    LOGI("Model loaded OK, handle=%p", mc);
    return reinterpret_cast<jlong>(mc);
}

// ===== nativeUnloadModel =====

JNIEXPORT void JNICALL
Java_com_chromalab_feature_processing_inference_LlamaEngine_nativeUnloadModel(
    JNIEnv *env, jclass /* clazz */, jlong handle) {

    auto *mc = reinterpret_cast<ModelContext *>(handle);
    if (!mc) return;

    LOGI("Unloading model, handle=%p", mc);

    if (mc->mtmd) { mtmd_free(mc->mtmd); mc->mtmd = nullptr; }
    if (mc->ctx)  { llama_free(mc->ctx);  mc->ctx  = nullptr; }
    if (mc->model){ llama_model_free(mc->model); mc->model = nullptr; }

    delete mc;
    LOGI("Model unloaded");
}

// ===== nativeInferWithImage =====

/**
 * Run multimodal inference: image + text prompt → text response.
 *
 * Flow:
 * 1. Load image via mtmd_helper_bitmap_init_from_file
 * 2. Build prompt with <__media__> marker
 * 3. mtmd_tokenize → chunks (text + image tokens)
 * 4. mtmd_helper_eval_chunks → populate KV cache
 * 5. Greedy decode loop with repeat penalty → collect output tokens
 * 6. Return decoded text
 *
 * Sampling strategy: GREEDY (temperature=0) for deterministic factual output.
 * Repeat penalty prevents degenerate JSON loops.
 */
JNIEXPORT jstring JNICALL
Java_com_chromalab_feature_processing_inference_LlamaEngine_nativeInferWithImage(
    JNIEnv *env, jclass /* clazz */,
    jlong handle, jstring imagePath, jstring prompt,
    jint maxTokens,
    jfloat temperature, jfloat topP, jint topK,
    jfloat repeatPenalty, jint repeatLastN) {

    auto *mc = reinterpret_cast<ModelContext *>(handle);
    if (!mc || !mc->ctx || !mc->model) {
        LOGE("Invalid model context");
        return env->NewStringUTF("{\"x\": [], \"y\": []}");
    }

    const char *img_path = env->GetStringUTFChars(imagePath, nullptr);
    const char *pmt      = env->GetStringUTFChars(prompt, nullptr);

    int n_predict = (maxTokens > 0) ? maxTokens : 512;
    if (mc->n_ctx > 0 && n_predict > mc->n_ctx / 2) {
        n_predict = mc->n_ctx / 2;
    }
    const float rep_penalty = (repeatPenalty > 0.0f) ? repeatPenalty : 1.1f;
    const int rep_last_n = (repeatLastN > 0) ? repeatLastN : 64;
    const bool json_task = prompt_requests_json(pmt);
    const auto started = std::chrono::steady_clock::now();
    const int decode_limit_ms = json_task ? 90000 : 180000;

    LOGI("Infer: image=%s, maxTokens=%d, temp=%.2f, topP=%.2f, topK=%d, repeatPenalty=%.2f, repeatLastN=%d",
         img_path, n_predict, temperature, topP, topK, rep_penalty, rep_last_n);

    std::string result_text;

    if (mc->mtmd) {
        // ---- Multimodal path (VLM with vision) ----

        // 1. Load image
        LOGI("MTMD bitmap load start");
        mtmd_bitmap *bitmap = nullptr;
        {
            NativeStageWatchdog watchdog("mtmd_bitmap_load");
            bitmap = mtmd_helper_bitmap_init_from_file(mc->mtmd, img_path);
        }
        if (!bitmap) {
            LOGE("Failed to load image: %s", img_path);
            env->ReleaseStringUTFChars(imagePath, img_path);
            env->ReleaseStringUTFChars(prompt, pmt);
            return env->NewStringUTF("{\"x\": [], \"y\": []}");
        }
        LOGI("MTMD bitmap load done");

        // 2. Build prompt with media marker
        const char *marker = mtmd_default_marker();
        std::string full_prompt = build_multimodal_prompt(pmt, marker);
        LOGI("MTMD prompt prepared: chars=%zu marker=%s marker_count=%zu role_markers=%zu preview=%s",
             full_prompt.size(),
             marker,
             count_occurrences(full_prompt, marker ? std::string(marker) : std::string("<__media__>")),
             count_occurrences(full_prompt, "<|im_start|>"),
             one_line_preview(full_prompt).c_str());

        // 3. Tokenize
        mtmd_input_text input_text;
        input_text.text = full_prompt.c_str();
        input_text.add_special = true;
        input_text.parse_special = true;

        const mtmd_bitmap *bitmaps[] = { bitmap };

        mtmd_input_chunks *chunks = mtmd_input_chunks_init();
        const auto tokenize_started = std::chrono::steady_clock::now();
        LOGI("MTMD tokenize start");
        int32_t tok_result = 0;
        {
            NativeStageWatchdog watchdog("mtmd_tokenize", 30000, 15000);
            tok_result = mtmd_tokenize(mc->mtmd, chunks, &input_text, bitmaps, 1);
        }
        mtmd_bitmap_free(bitmap);
        LOGI("MTMD tokenize done: result=%d elapsed=%lld ms",
             tok_result, elapsed_ms_since(tokenize_started));

        if (tok_result != 0) {
            LOGE("mtmd_tokenize failed: %d", tok_result);
            mtmd_input_chunks_free(chunks);
            env->ReleaseStringUTFChars(imagePath, img_path);
            env->ReleaseStringUTFChars(prompt, pmt);
            return env->NewStringUTF("{\"x\": [], \"y\": []}");
        }

        const size_t chunk_count = mtmd_input_chunks_size(chunks);
        LOGI("MTMD chunks: count=%zu total_tokens=%zu total_pos=%d",
             chunk_count,
             mtmd_helper_get_n_tokens(chunks),
             (int)mtmd_helper_get_n_pos(chunks));
        for (size_t i = 0; i < std::min<size_t>(chunk_count, 8); ++i) {
            const mtmd_input_chunk *chunk = mtmd_input_chunks_get(chunks, i);
            if (!chunk) continue;
            const char *chunk_id = mtmd_input_chunk_get_id(chunk);
            LOGI("MTMD chunk[%zu]: type=%s tokens=%zu pos=%d id=%s",
                 i,
                 chunk_type_name(mtmd_input_chunk_get_type(chunk)),
                 mtmd_input_chunk_get_n_tokens(chunk),
                 (int)mtmd_input_chunk_get_n_pos(chunk),
                 chunk_id ? chunk_id : "");
        }

        // 4. Clear KV cache and eval chunks
        llama_memory_clear(llama_get_memory(mc->ctx), true);

        llama_pos n_past = 0;
        const auto eval_started = std::chrono::steady_clock::now();
        LOGI("MTMD eval chunks start: n_batch=%d n_ctx=%d", mc->n_batch, mc->n_ctx);
        int32_t eval_result = 0;
        {
            NativeStageWatchdog watchdog("mtmd_helper_eval_chunks", 30000, 15000);
            eval_result = mtmd_helper_eval_chunks(
                mc->mtmd, mc->ctx, chunks,
                n_past, 0, mc->n_batch, true, &n_past
            );
        }
        mtmd_input_chunks_free(chunks);
        LOGI("MTMD eval chunks done: result=%d n_past=%d elapsed=%lld ms",
             eval_result, (int)n_past, elapsed_ms_since(eval_started));

        if (eval_result != 0) {
            LOGE("mtmd_helper_eval_chunks failed: %d", eval_result);
            env->ReleaseStringUTFChars(imagePath, img_path);
            env->ReleaseStringUTFChars(prompt, pmt);
            return env->NewStringUTF("{\"x\": [], \"y\": []}");
        }

        // 5. Decode loop — greedy sampling with repeat penalty
        //
        // Sampling chain for chromatogram analysis:
        //   a) Repeat penalty — prevents degenerate JSON value loops
        //   b) Greedy — deterministic, temperature=0, no randomness
        //
        // This ensures maximum accuracy for numeric OCR extraction.
        if (elapsed_over(started, decode_limit_ms)) {
            LOGE("Image prompt eval exceeded %d ms before decode", decode_limit_ms);
            env->ReleaseStringUTFChars(imagePath, img_path);
            env->ReleaseStringUTFChars(prompt, pmt);
            return env->NewStringUTF(json_task ? "{}" : "");
        }

        const llama_vocab *vocab = llama_model_get_vocab(mc->model);

        llama_sampler *smpl = create_sampler(
            temperature,
            topP,
            topK,
            rep_penalty,
            rep_last_n
        );

        {
            NativeStageWatchdog watchdog("llama_image_decode", 30000, 30000);
            for (int i = 0; i < n_predict; i++) {
            // Sample token using sampler chain (-1 = last logits)
            llama_token token_id = llama_sampler_sample(smpl, mc->ctx, -1);

            // Check end-of-generation
            if (llama_vocab_is_eog(vocab, token_id)) {
                break;
            }

            // Decode token to text
            char buf[256];
            int len = llama_token_to_piece(vocab, token_id, buf, sizeof(buf), 0, true);
            if (len > 0) {
                result_text.append(buf, len);
            }
            if (i == 0) {
                LOGI("Image first token: id=%d elapsed=%lld ms", (int)token_id, elapsed_ms_since(started));
            }

            // Prepare next batch — single token
            if (json_task && json_object_complete(result_text)) {
                LOGI("Image JSON complete at token %d", i + 1);
                break;
            }
            if (elapsed_over(started, decode_limit_ms)) {
                LOGE("Image decode timed out after %d ms", decode_limit_ms);
                break;
            }

            llama_batch batch = llama_batch_get_one(&token_id, 1);
            if (llama_decode(mc->ctx, batch) != 0) {
                LOGE("llama_decode failed at token %d", i);
                break;
            }
            }
        }

        llama_sampler_free(smpl);
        LOGI("Generated %zu chars", result_text.size());
    } else {
        // ---- Text-only fallback (no vision encoder) ----
        LOGI("No mtmd loaded, returning empty result");
        result_text = "{\"x\": [], \"y\": []}";
    }

    env->ReleaseStringUTFChars(imagePath, img_path);
    env->ReleaseStringUTFChars(prompt, pmt);
    return env->NewStringUTF(result_text.c_str());
}

// ===== nativeInferText =====

JNIEXPORT jstring JNICALL
Java_com_chromalab_feature_processing_inference_LlamaEngine_nativeInferText(
    JNIEnv *env, jclass /* clazz */,
    jlong handle, jstring prompt,
    jint maxTokens,
    jfloat temperature, jfloat topP, jint topK,
    jfloat repeatPenalty, jint repeatLastN) {

    auto *mc = reinterpret_cast<ModelContext *>(handle);
    if (!mc || !mc->ctx || !mc->model) {
        LOGE("Invalid model context for text inference");
        return env->NewStringUTF("");
    }

    const char *pmt = env->GetStringUTFChars(prompt, nullptr);
    const int n_predict = (maxTokens > 0) ? maxTokens : 512;
    const float rep_penalty = (repeatPenalty > 0.0f) ? repeatPenalty : 1.1f;
    const int rep_last_n = (repeatLastN > 0) ? repeatLastN : 64;

    LOGI("Infer text-only: maxTokens=%d, temp=%.2f, topP=%.2f, topK=%d, repeatPenalty=%.2f, repeatLastN=%d",
         n_predict, temperature, topP, topK, rep_penalty, rep_last_n);

    std::string result_text = run_text_completion(
        mc,
        pmt,
        n_predict,
        temperature,
        topP,
        topK,
        rep_penalty,
        rep_last_n
    );

    env->ReleaseStringUTFChars(prompt, pmt);
    LOGI("Generated text-only %zu chars", result_text.size());
    return env->NewStringUTF(result_text.c_str());
}

} // extern "C"

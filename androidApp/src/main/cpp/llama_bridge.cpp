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
#include <android/log.h>

#include "llama.h"
#include "mtmd.h"
#include "mtmd-helper.h"

#define LOG_TAG "LlamaBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
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
    jint threads, jint backendCode) {

    const char *base   = env->GetStringUTFChars(basePath, nullptr);
    const char *mmproj = env->GetStringUTFChars(mmprojPath, nullptr);

    LOGI("nativeLoadModel: base=%s, threads=%d", base, threads);

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
    mc->model = llama_model_load_from_file(base, model_params);
    if (!mc->model) {
        LOGE("Failed to load model: %s", base);
        env->ReleaseStringUTFChars(basePath, base);
        env->ReleaseStringUTFChars(mmprojPath, mmproj);
        delete mc;
        return 0;
    }

    // 2) Create context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx     = 4096;
    ctx_params.n_batch   = 512;
    ctx_params.n_threads = (threads > 0) ? threads : 4;
    ctx_params.n_threads_batch = ctx_params.n_threads;
    mc->ctx = llama_init_from_model(mc->model, ctx_params);
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
        mc->mtmd = mtmd_init_from_file(mmproj, mc->model, mtmd_params);
        if (mc->mtmd) {
            LOGI("MTMD vision encoder loaded: %s", mmproj);
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

    LOGI("Infer: image=%s, maxTokens=%d, temp=%.2f, topP=%.2f, topK=%d, repeatPenalty=%.2f, repeatLastN=%d",
         img_path, n_predict, temperature, topP, topK, rep_penalty, rep_last_n);

    std::string result_text;

    if (mc->mtmd) {
        // ---- Multimodal path (VLM with vision) ----

        // 1. Load image
        mtmd_bitmap *bitmap = mtmd_helper_bitmap_init_from_file(mc->mtmd, img_path);
        if (!bitmap) {
            LOGE("Failed to load image: %s", img_path);
            env->ReleaseStringUTFChars(imagePath, img_path);
            env->ReleaseStringUTFChars(prompt, pmt);
            return env->NewStringUTF("{\"x\": [], \"y\": []}");
        }

        // 2. Build prompt with media marker
        const char *marker = mtmd_default_marker();
        std::string full_prompt = std::string(marker) + "\n" + pmt;

        // 3. Tokenize
        mtmd_input_text input_text;
        input_text.text = full_prompt.c_str();
        input_text.add_special = true;
        input_text.parse_special = true;

        const mtmd_bitmap *bitmaps[] = { bitmap };

        mtmd_input_chunks *chunks = mtmd_input_chunks_init();
        int32_t tok_result = mtmd_tokenize(mc->mtmd, chunks, &input_text, bitmaps, 1);
        mtmd_bitmap_free(bitmap);

        if (tok_result != 0) {
            LOGE("mtmd_tokenize failed: %d", tok_result);
            mtmd_input_chunks_free(chunks);
            env->ReleaseStringUTFChars(imagePath, img_path);
            env->ReleaseStringUTFChars(prompt, pmt);
            return env->NewStringUTF("{\"x\": [], \"y\": []}");
        }

        // 4. Clear KV cache and eval chunks
        llama_memory_clear(llama_get_memory(mc->ctx), true);

        llama_pos n_past = 0;
        int32_t eval_result = mtmd_helper_eval_chunks(
            mc->mtmd, mc->ctx, chunks,
            n_past, 0, 512, true, &n_past
        );
        mtmd_input_chunks_free(chunks);

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
        const llama_vocab *vocab = llama_model_get_vocab(mc->model);

        llama_sampler *smpl = create_sampler(
            temperature,
            topP,
            topK,
            rep_penalty,
            rep_last_n
        );

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

            // Prepare next batch — single token
            llama_batch batch = llama_batch_get_one(&token_id, 1);
            if (llama_decode(mc->ctx, batch) != 0) {
                LOGE("llama_decode failed at token %d", i);
                break;
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

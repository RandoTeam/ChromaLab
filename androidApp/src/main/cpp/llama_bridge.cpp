/**
 * llama_bridge.cpp — JNI bridge between Kotlin and llama.cpp + mtmd (multimodal).
 *
 * Uses the current (b9198+6, May 2026) API:
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
#include <vulkan/vulkan.h>

#include "llama.h"
#include "ggml-backend.h"
#include "common.h"
#include "sampling.h"
#include "speculative.h"
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
    llama_context *ctx_mtp = nullptr;
    common_speculative *spec = nullptr;
    mtmd_context  *mtmd    = nullptr;   // null if no mmproj provided
    int            n_ctx   = 0;
    int            n_batch = 512;
    int            mtp_draft_tokens = 0;
    std::string    backend_label = "llama.cpp CPU";
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

static common_sampler * create_common_sampler(
    const llama_model *model,
    float temperature,
    float topP,
    int topK,
    float repeatPenalty,
    int repeatLastN) {

    common_params_sampling params;
    params.no_perf = true;
    params.temp = temperature;
    params.top_p = (topP > 0.0f) ? topP : 1.0f;
    params.top_k = topK;
    params.penalty_repeat = (repeatPenalty > 0.0f) ? repeatPenalty : 1.1f;
    params.penalty_last_n = (repeatLastN > 0) ? repeatLastN : 64;
    params.penalty_freq = 0.0f;
    params.penalty_present = 0.0f;
    params.dry_multiplier = 0.0f;
    params.min_p = 0.0f;
    params.typ_p = 1.0f;
    params.xtc_probability = 0.0f;
    params.top_n_sigma = -1.0f;
    params.samplers = {
        COMMON_SAMPLER_TYPE_PENALTIES,
        COMMON_SAMPLER_TYPE_TOP_K,
        COMMON_SAMPLER_TYPE_TOP_P,
        COMMON_SAMPLER_TYPE_TEMPERATURE,
    };

    return common_sampler_init(model, params);
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

static std::vector<std::string> jstring_array_to_vector(JNIEnv *env, jobjectArray array) {
    std::vector<std::string> result;
    if (!env || !array) {
        return result;
    }

    const jsize count = env->GetArrayLength(array);
    result.reserve((size_t)count);
    for (jsize i = 0; i < count; ++i) {
        auto item = (jstring)env->GetObjectArrayElement(array, i);
        if (!item) {
            result.emplace_back();
            continue;
        }

        const char *chars = env->GetStringUTFChars(item, nullptr);
        result.emplace_back(chars ? chars : "");
        env->ReleaseStringUTFChars(item, chars);
        env->DeleteLocalRef(item);
    }
    return result;
}

static std::string apply_model_chat_template(
    ModelContext *mc,
    const std::vector<std::string> &roles,
    const std::vector<std::string> &contents) {

    if (!mc || !mc->model || roles.empty() || roles.size() != contents.size()) {
        return "";
    }

    std::vector<llama_chat_message> messages;
    messages.reserve(roles.size());
    for (size_t i = 0; i < roles.size(); ++i) {
        messages.push_back(llama_chat_message{
            roles[i].c_str(),
            contents[i].c_str(),
        });
    }

    const char *model_template = llama_model_chat_template(mc->model, nullptr);
    const bool has_model_template = model_template && std::strlen(model_template) > 0;
    const char *template_source = has_model_template ? model_template : "chatml";

    int32_t formatted_len = llama_chat_apply_template(
        template_source,
        messages.data(),
        messages.size(),
        true,
        nullptr,
        0
    );
    if (formatted_len < 0 && has_model_template) {
        LOGW("Model chat template was not supported by llama_chat_apply_template; falling back to chatml");
        template_source = "chatml";
        formatted_len = llama_chat_apply_template(
            template_source,
            messages.data(),
            messages.size(),
            true,
            nullptr,
            0
        );
    }
    if (formatted_len <= 0) {
        LOGE("Failed to apply chat template: result=%d messages=%zu", formatted_len, messages.size());
        return "";
    }

    std::vector<char> buffer((size_t)formatted_len + 1, '\0');
    const int32_t applied_len = llama_chat_apply_template(
        template_source,
        messages.data(),
        messages.size(),
        true,
        buffer.data(),
        (int32_t)buffer.size()
    );
    if (applied_len <= 0) {
        LOGE("Failed to render chat template: result=%d messages=%zu", applied_len, messages.size());
        return "";
    }
    buffer[(size_t)std::min<int32_t>(applied_len, (int32_t)buffer.size() - 1)] = '\0';

    std::string prompt(buffer.data(), (size_t)applied_len);
    LOGI("Applied native chat template: source=%s messages=%zu chars=%zu preview=%s",
         has_model_template ? "model" : "fallback-chatml",
         messages.size(),
         prompt.size(),
         one_line_preview(prompt).c_str());
    return prompt;
}

static jstring new_jstring_from_utf8_bytes(JNIEnv *env, const std::string &text) {
    if (!env) {
        return nullptr;
    }

    jbyteArray bytes = env->NewByteArray((jsize)text.size());
    if (!bytes) {
        LOGE("Failed to allocate UTF-8 byte array");
        return nullptr;
    }
    if (!text.empty()) {
        env->SetByteArrayRegion(
            bytes,
            0,
            (jsize)text.size(),
            reinterpret_cast<const jbyte *>(text.data())
        );
    }

    jclass string_class = env->FindClass("java/lang/String");
    if (!string_class) {
        env->DeleteLocalRef(bytes);
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(string_class, "<init>", "([BLjava/lang/String;)V");
    if (!ctor) {
        env->DeleteLocalRef(bytes);
        env->DeleteLocalRef(string_class);
        return nullptr;
    }
    jstring charset = env->NewStringUTF("UTF-8");
    if (!charset) {
        env->DeleteLocalRef(bytes);
        env->DeleteLocalRef(string_class);
        return nullptr;
    }

    auto result = (jstring)env->NewObject(string_class, ctor, bytes, charset);
    env->DeleteLocalRef(bytes);
    env->DeleteLocalRef(charset);
    env->DeleteLocalRef(string_class);
    return result;
}

static bool is_utf8_continuation(unsigned char value) {
    return (value & 0xC0) == 0x80;
}

static size_t utf8_complete_prefix_length(const std::string &text) {
    size_t index = 0;
    while (index < text.size()) {
        const unsigned char first = (unsigned char)text[index];
        if (first <= 0x7F) {
            index += 1;
            continue;
        }

        size_t expected = 0;
        if (first >= 0xC2 && first <= 0xDF) {
            expected = 2;
        } else if (first >= 0xE0 && first <= 0xEF) {
            expected = 3;
        } else if (first >= 0xF0 && first <= 0xF4) {
            expected = 4;
        } else {
            // Invalid byte. Java's UTF-8 decoder will replace it safely.
            index += 1;
            continue;
        }

        const size_t available = text.size() - index;
        if (available < expected) {
            bool could_be_incomplete = true;
            for (size_t offset = 1; offset < available; offset++) {
                if (!is_utf8_continuation((unsigned char)text[index + offset])) {
                    could_be_incomplete = false;
                    break;
                }
            }
            if (could_be_incomplete) {
                break;
            }
            index += 1;
            continue;
        }

        bool valid = true;
        for (size_t offset = 1; offset < expected; offset++) {
            if (!is_utf8_continuation((unsigned char)text[index + offset])) {
                valid = false;
                break;
            }
        }
        if (!valid) {
            index += 1;
            continue;
        }

        const unsigned char second = (unsigned char)text[index + 1];
        if ((first == 0xE0 && second < 0xA0) ||
            (first == 0xED && second >= 0xA0) ||
            (first == 0xF0 && second < 0x90) ||
            (first == 0xF4 && second > 0x8F)) {
            index += 1;
            continue;
        }

        index += expected;
    }
    return index;
}

static bool dispatch_token_callback(
    JNIEnv *env,
    jobject callback,
    jmethodID method,
    const std::string &text,
    int generated_tokens,
    long long elapsed_ms) {

    if (!env || !callback || !method || text.empty()) {
        return true;
    }

    jstring chunk = new_jstring_from_utf8_bytes(env, text);
    if (!chunk) {
        LOGE("Failed to allocate JNI token chunk");
        return false;
    }
    env->CallVoidMethod(callback, method, chunk, (jint)generated_tokens, (jlong)elapsed_ms);
    env->DeleteLocalRef(chunk);
    if (env->ExceptionCheck()) {
        LOGE("Exception thrown from Kotlin token callback");
        return false;
    }
    return true;
}

static const char * chunk_type_name(enum mtmd_input_chunk_type type) {
    switch (type) {
        case MTMD_INPUT_CHUNK_TYPE_TEXT:  return "text";
        case MTMD_INPUT_CHUNK_TYPE_IMAGE: return "image";
        case MTMD_INPUT_CHUNK_TYPE_AUDIO: return "audio";
    }
    return "unknown";
}

static const char * backend_device_type_name(enum ggml_backend_dev_type type) {
    switch (type) {
        case GGML_BACKEND_DEVICE_TYPE_CPU:   return "CPU";
        case GGML_BACKEND_DEVICE_TYPE_GPU:   return "GPU";
        case GGML_BACKEND_DEVICE_TYPE_IGPU:  return "IGPU";
        case GGML_BACKEND_DEVICE_TYPE_ACCEL: return "ACCEL";
        case GGML_BACKEND_DEVICE_TYPE_META:  return "META";
    }
    return "UNKNOWN";
}

static bool is_accelerated_device_type(enum ggml_backend_dev_type type) {
    return type == GGML_BACKEND_DEVICE_TYPE_GPU ||
           type == GGML_BACKEND_DEVICE_TYPE_IGPU ||
           type == GGML_BACKEND_DEVICE_TYPE_ACCEL;
}

static bool instance_extension_available(const char *name) {
    uint32_t count = 0;
    if (vkEnumerateInstanceExtensionProperties(nullptr, &count, nullptr) != VK_SUCCESS || count == 0) {
        return false;
    }

    std::vector<VkExtensionProperties> extensions(count);
    if (vkEnumerateInstanceExtensionProperties(nullptr, &count, extensions.data()) != VK_SUCCESS) {
        return false;
    }

    for (const auto &extension : extensions) {
        if (std::strcmp(extension.extensionName, name) == 0) {
            return true;
        }
    }
    return false;
}

static bool vulkan_has_ggml_required_features() {
    static int cached_result = -1;
    if (cached_result >= 0) {
        return cached_result == 1;
    }

    std::vector<const char *> instance_extensions;
    const bool has_get_physical_device_properties2 =
        instance_extension_available(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME);
    if (has_get_physical_device_properties2) {
        instance_extensions.push_back(VK_KHR_GET_PHYSICAL_DEVICE_PROPERTIES_2_EXTENSION_NAME);
    }

    VkApplicationInfo app_info{};
    app_info.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    app_info.pApplicationName = "ChromaLab GGUF Vulkan preflight";
    app_info.applicationVersion = 1;
    app_info.pEngineName = "llama.cpp";
    app_info.engineVersion = 1;
    app_info.apiVersion = VK_API_VERSION_1_1;

    VkInstanceCreateInfo create_info{};
    create_info.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    create_info.pApplicationInfo = &app_info;
    create_info.enabledExtensionCount = static_cast<uint32_t>(instance_extensions.size());
    create_info.ppEnabledExtensionNames = instance_extensions.empty() ? nullptr : instance_extensions.data();

    VkInstance instance = VK_NULL_HANDLE;
    VkResult result = vkCreateInstance(&create_info, nullptr, &instance);
    if (result != VK_SUCCESS) {
        app_info.apiVersion = VK_API_VERSION_1_0;
        result = vkCreateInstance(&create_info, nullptr, &instance);
    }
    if (result != VK_SUCCESS || instance == VK_NULL_HANDLE) {
        LOGW("GGUF Vulkan preflight: vkCreateInstance failed result=%d", (int)result);
        cached_result = 0;
        return false;
    }

    auto vk_get_features2 = reinterpret_cast<PFN_vkGetPhysicalDeviceFeatures2>(
        vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceFeatures2")
    );
    if (!vk_get_features2) {
        vk_get_features2 = reinterpret_cast<PFN_vkGetPhysicalDeviceFeatures2>(
            vkGetInstanceProcAddr(instance, "vkGetPhysicalDeviceFeatures2KHR")
        );
    }
    if (!vk_get_features2) {
        LOGW("GGUF Vulkan preflight: vkGetPhysicalDeviceFeatures2 unavailable");
        vkDestroyInstance(instance, nullptr);
        cached_result = 0;
        return false;
    }

    uint32_t device_count = 0;
    result = vkEnumeratePhysicalDevices(instance, &device_count, nullptr);
    if (result != VK_SUCCESS || device_count == 0) {
        LOGW("GGUF Vulkan preflight: no physical devices result=%d count=%u", (int)result, device_count);
        vkDestroyInstance(instance, nullptr);
        cached_result = 0;
        return false;
    }

    std::vector<VkPhysicalDevice> devices(device_count);
    result = vkEnumeratePhysicalDevices(instance, &device_count, devices.data());
    if (result != VK_SUCCESS) {
        LOGW("GGUF Vulkan preflight: enumerate devices failed result=%d", (int)result);
        vkDestroyInstance(instance, nullptr);
        cached_result = 0;
        return false;
    }

    bool supported = false;
    for (VkPhysicalDevice device : devices) {
        VkPhysicalDeviceProperties properties{};
        vkGetPhysicalDeviceProperties(device, &properties);

        VkPhysicalDevice16BitStorageFeatures storage_features{};
        storage_features.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_16BIT_STORAGE_FEATURES;

        VkPhysicalDeviceFeatures2 features2{};
        features2.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2;
        features2.pNext = &storage_features;
        vk_get_features2(device, &features2);

        LOGI("GGUF Vulkan preflight: device=%s storageBuffer16BitAccess=%d",
             properties.deviceName,
             storage_features.storageBuffer16BitAccess ? 1 : 0);

        if (storage_features.storageBuffer16BitAccess == VK_TRUE) {
            supported = true;
            break;
        }
    }

    vkDestroyInstance(instance, nullptr);
    cached_result = supported ? 1 : 0;
    return supported;
}

static void chromalab_llama_log_callback(
    enum ggml_log_level level,
    const char *text,
    void * /* user_data */) {

    if (!text || text[0] == '\0') return;

    if (level != GGML_LOG_LEVEL_WARN && level != GGML_LOG_LEVEL_ERROR) {
        return;
    }

    int priority = ANDROID_LOG_DEBUG;
    switch (level) {
        case GGML_LOG_LEVEL_ERROR:
            priority = ANDROID_LOG_ERROR;
            break;
        case GGML_LOG_LEVEL_WARN:
            priority = ANDROID_LOG_WARN;
            break;
        case GGML_LOG_LEVEL_INFO:
        case GGML_LOG_LEVEL_CONT:
            priority = ANDROID_LOG_INFO;
            break;
        case GGML_LOG_LEVEL_DEBUG:
        case GGML_LOG_LEVEL_NONE:
        default:
            priority = ANDROID_LOG_DEBUG;
            break;
    }

    std::string message(text);
    size_t start = 0;
    while (start < message.size()) {
        size_t end = message.find('\n', start);
        std::string line = message.substr(
            start,
            end == std::string::npos ? std::string::npos : end - start
        );
        if (!line.empty()) {
            __android_log_print(priority, "LlamaCpp", "%s", line.c_str());
        }
        if (end == std::string::npos) break;
        start = end + 1;
    }
}

static void ensure_llama_logging() {
    static bool logging_inited = false;
    if (logging_inited) return;
    llama_log_set(chromalab_llama_log_callback, nullptr);
    logging_inited = true;
}

static void ensure_backend_initialized() {
    static bool backend_inited = false;
    if (backend_inited) return;

    ensure_llama_logging();
    ggml_backend_load_all();
    llama_backend_init();
    backend_inited = true;

    const size_t dev_count = ggml_backend_dev_count();
    LOGI("GGUF backends initialized: device_count=%zu", dev_count);
    for (size_t i = 0; i < dev_count; ++i) {
        ggml_backend_dev_t dev = ggml_backend_dev_get(i);
        if (!dev) continue;

        ggml_backend_reg_t reg = ggml_backend_dev_backend_reg(dev);
        size_t free_mem = 0;
        size_t total_mem = 0;
        ggml_backend_dev_memory(dev, &free_mem, &total_mem);
        LOGI("GGUF backend device[%zu]: reg=%s name=%s desc=%s type=%s free=%zu total=%zu",
             i,
             reg ? ggml_backend_reg_name(reg) : "",
             ggml_backend_dev_name(dev) ? ggml_backend_dev_name(dev) : "",
             ggml_backend_dev_description(dev) ? ggml_backend_dev_description(dev) : "",
             backend_device_type_name(ggml_backend_dev_type(dev)),
             free_mem,
             total_mem);
    }
}

static bool has_accelerated_backend() {
    ensure_backend_initialized();
    if (!vulkan_has_ggml_required_features()) {
        return false;
    }

    const size_t dev_count = ggml_backend_dev_count();
    for (size_t i = 0; i < dev_count; ++i) {
        ggml_backend_dev_t dev = ggml_backend_dev_get(i);
        if (!dev) continue;
        if (is_accelerated_device_type(ggml_backend_dev_type(dev))) {
            return true;
        }
    }
    return false;
}

static std::string accelerated_backend_label() {
    ensure_backend_initialized();
    const size_t dev_count = ggml_backend_dev_count();
    for (size_t i = 0; i < dev_count; ++i) {
        ggml_backend_dev_t dev = ggml_backend_dev_get(i);
        if (!dev || !is_accelerated_device_type(ggml_backend_dev_type(dev))) continue;

        ggml_backend_reg_t reg = ggml_backend_dev_backend_reg(dev);
        const char *reg_name = reg ? ggml_backend_reg_name(reg) : nullptr;
        if (reg_name && std::strlen(reg_name) > 0) {
            std::string name(reg_name);
            if (!name.empty()) {
                name[0] = (char)std::toupper((unsigned char)name[0]);
            }
            return "llama.cpp " + name;
        }
        return "llama.cpp accelerated";
    }
    return "llama.cpp CPU";
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
    int repeatLastN,
    JNIEnv *env = nullptr,
    jobject callback = nullptr) {

    if (!mc || !mc->ctx || !mc->model || !prompt) {
        return "";
    }

    jmethodID callback_method = nullptr;
    if (env && callback) {
        jclass callback_class = env->GetObjectClass(callback);
        if (callback_class) {
            callback_method = env->GetMethodID(callback_class, "onToken", "(Ljava/lang/String;IJ)V");
            env->DeleteLocalRef(callback_class);
        }
        if (!callback_method) {
            LOGE("Token callback method not found");
            return "";
        }
    }

    int n_predict = (maxTokens > 0) ? maxTokens : 512;
    if (mc->n_ctx > 0 && n_predict > mc->n_ctx / 2) {
        n_predict = mc->n_ctx / 2;
    }
    const llama_vocab *vocab = llama_model_get_vocab(mc->model);
    const auto total_started = std::chrono::steady_clock::now();

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

    const auto prompt_eval_started = std::chrono::steady_clock::now();
    const int batch_size = (mc->n_batch > 0) ? mc->n_batch : 512;
    for (int offset = 0; offset < n_tokens; offset += batch_size) {
        const int n_chunk = std::min(batch_size, n_tokens - offset);
        LOGI("Text prompt eval batch start: offset=%d count=%d", offset, n_chunk);
        const auto batch_started = std::chrono::steady_clock::now();
        llama_batch batch = llama_batch_get_one(tokens.data() + offset, n_chunk);
        int decode_result = 0;
        {
            NativeStageWatchdog watchdog("llama_text_prompt_eval", 15000, 15000);
            decode_result = llama_decode(mc->ctx, batch);
        }
        LOGI("Text prompt eval batch done: offset=%d count=%d elapsed=%lld ms result=%d",
             offset,
             n_chunk,
             elapsed_ms_since(batch_started),
             decode_result);
        if (decode_result != 0) {
            LOGE("Text prompt decode failed at offset %d result=%d", offset, decode_result);
            return "";
        }
    }
    LOGI("Text prompt eval done: tokens=%d elapsed=%lld ms",
         n_tokens,
         elapsed_ms_since(prompt_eval_started));

    std::string result_text;
    std::string pending_callback_text;
    int generated_tokens = 0;
    bool callback_failed = false;
    llama_sampler *smpl = create_sampler(temperature, topP, topK, repeatPenalty, repeatLastN);
    const bool json_task = prompt_requests_json(prompt);
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
            generated_tokens = i + 1;
            if (callback && callback_method) {
                pending_callback_text.append(buf, (size_t)len);
                const size_t safe_prefix_len = utf8_complete_prefix_length(pending_callback_text);
                if (safe_prefix_len > 0) {
                    std::string token_text = pending_callback_text.substr(0, safe_prefix_len);
                    pending_callback_text.erase(0, safe_prefix_len);
                    if (!dispatch_token_callback(env, callback, callback_method, token_text, generated_tokens, elapsed_ms_since(total_started))) {
                        LOGE("Stopping text decode after callback failure at token %d", generated_tokens);
                        callback_failed = true;
                        break;
                    }
                }
            }
        }
        if (i == 0) {
            LOGI("Text first token: id=%d elapsed=%lld ms", (int)token_id, elapsed_ms_since(total_started));
        }

        if (json_task && json_object_complete(result_text)) {
            LOGI("Text JSON complete at token %d", i + 1);
            break;
        }
        if (elapsed_over(total_started, decode_limit_ms)) {
            LOGE("Text decode timed out after %d ms", decode_limit_ms);
            break;
        }

        llama_batch next = llama_batch_get_one(&token_id, 1);
        if (llama_decode(mc->ctx, next) != 0) {
            LOGE("Text decode failed at token %d", i);
            break;
        }
    }

    if (!callback_failed && !pending_callback_text.empty()) {
        dispatch_token_callback(
            env,
            callback,
            callback_method,
            pending_callback_text,
            generated_tokens,
            elapsed_ms_since(total_started)
        );
    }

    llama_sampler_free(smpl);
    return result_text;
}

static std::string run_text_completion_mtp(
    ModelContext *mc,
    const char *prompt,
    int maxTokens,
    float temperature,
    float topP,
    int topK,
    float repeatPenalty,
    int repeatLastN,
    JNIEnv *env = nullptr,
    jobject callback = nullptr) {

    if (!mc || !mc->ctx || !mc->ctx_mtp || !mc->spec || !mc->model || !prompt || mc->mtp_draft_tokens <= 0) {
        return run_text_completion(
            mc,
            prompt,
            maxTokens,
            temperature,
            topP,
            topK,
            repeatPenalty,
            repeatLastN,
            env,
            callback
        );
    }

    jmethodID callback_method = nullptr;
    if (env && callback) {
        jclass callback_class = env->GetObjectClass(callback);
        if (callback_class) {
            callback_method = env->GetMethodID(callback_class, "onToken", "(Ljava/lang/String;IJ)V");
            env->DeleteLocalRef(callback_class);
        }
        if (!callback_method) {
            LOGE("Token callback method not found");
            return "";
        }
    }

    int n_predict = (maxTokens > 0) ? maxTokens : 512;
    if (mc->n_ctx > 0 && n_predict > mc->n_ctx / 2) {
        n_predict = mc->n_ctx / 2;
    }

    const llama_vocab *vocab = llama_model_get_vocab(mc->model);
    const auto total_started = std::chrono::steady_clock::now();
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
        LOGE("MTP text tokenization failed: %d", n_tokens);
        return "";
    }

    tokens.resize((size_t)n_tokens);

    int max_prompt_tokens = mc->n_ctx - n_predict - mc->mtp_draft_tokens - 4;
    if (max_prompt_tokens > 0 && n_tokens > max_prompt_tokens) {
        tokens.erase(tokens.begin(), tokens.end() - max_prompt_tokens);
        n_tokens = (int)tokens.size();
        LOGI("MTP prompt truncated to %d tokens", n_tokens);
    }

    if (n_tokens <= 0) {
        return "";
    }

    LOGI("MTP text prompt tokenized: chars=%d tokens=%d n_predict=%d ctx=%d batch=%d draft_n_max=%d preview=%s",
         prompt_len, n_tokens, n_predict, mc->n_ctx, mc->n_batch, mc->mtp_draft_tokens,
         one_line_preview(std::string(prompt)).c_str());

    const llama_seq_id seq_id = 0;
    common_context_seq_rm_type ctx_tgt_seq_rm_type = common_context_can_seq_rm(mc->ctx);
    common_context_seq_rm_type ctx_dft_seq_rm_type = common_context_can_seq_rm(mc->ctx_mtp);
    if (ctx_tgt_seq_rm_type == COMMON_CONTEXT_SEQ_RM_TYPE_NO) {
        LOGE("MTP disabled for this generation: target context cannot remove sequence state");
        return run_text_completion(
            mc,
            prompt,
            maxTokens,
            temperature,
            topP,
            topK,
            repeatPenalty,
            repeatLastN,
            env,
            callback
        );
    }

    llama_memory_clear(llama_get_memory(mc->ctx), true);
    llama_memory_clear(llama_get_memory(mc->ctx_mtp), true);

    common_sampler_ptr smpl(create_common_sampler(
        mc->model,
        temperature,
        topP,
        topK,
        repeatPenalty,
        repeatLastN
    ));
    if (!smpl) {
        LOGE("MTP sampler initialization failed");
        return "";
    }

    const int batch_capacity = std::max(mc->n_batch, mc->mtp_draft_tokens + 1);
    llama_batch batch_tgt = llama_batch_init(batch_capacity, 0, 1);
    llama_tokens prompt_tgt;
    if (tokens.size() > 1) {
        prompt_tgt.assign(tokens.begin(), tokens.end() - 1);
    }
    prompt_tgt.reserve((size_t)mc->n_ctx);

    int n_past = 0;
    const int batch_size = (mc->n_batch > 0) ? mc->n_batch : 512;
    const auto prompt_eval_started = std::chrono::steady_clock::now();
    for (int offset = 0; offset < (int)prompt_tgt.size(); offset += batch_size) {
        const int n_chunk = std::min(batch_size, (int)prompt_tgt.size() - offset);
        common_batch_clear(batch_tgt);
        for (int i = 0; i < n_chunk; ++i) {
            common_batch_add(batch_tgt, prompt_tgt[(size_t)offset + i], n_past + i, { seq_id }, false);
        }

        LOGI("MTP prompt eval batch start: offset=%d count=%d", offset, n_chunk);
        const auto batch_started = std::chrono::steady_clock::now();
        int decode_result = 0;
        {
            NativeStageWatchdog watchdog("llama_mtp_prompt_eval", 15000, 15000);
            decode_result = llama_decode(mc->ctx, batch_tgt);
        }
        if (decode_result != 0 || !common_speculative_process(mc->spec, batch_tgt)) {
            LOGE("MTP prompt decode failed at offset %d result=%d", offset, decode_result);
            llama_batch_free(batch_tgt);
            return "";
        }
        n_past += n_chunk;
        LOGI("MTP prompt eval batch done: offset=%d count=%d elapsed=%lld ms result=%d",
             offset,
             n_chunk,
             elapsed_ms_since(batch_started),
             decode_result);
    }
    LOGI("MTP prompt eval done: tokens=%zu elapsed=%lld ms",
         prompt_tgt.size(),
         elapsed_ms_since(prompt_eval_started));

    llama_token id_last = tokens.back();
    common_speculative_begin(mc->spec, seq_id, prompt_tgt);

    std::string result_text;
    std::string pending_callback_text;
    int generated_tokens = 0;
    int drafted_tokens = 0;
    int accepted_draft_tokens = 0;
    bool callback_failed = false;
    bool has_eos = false;
    const bool json_task = prompt_requests_json(prompt);
    const int decode_limit_ms = json_task ? 90000 : 180000;

    llama_tokens draft;
    common_prompt_checkpoint ckpt;

    while (generated_tokens < n_predict && !has_eos) {
        if (draft.empty()) {
            ckpt.update_pos(
                prompt_tgt.size(),
                llama_memory_seq_pos_min(llama_get_memory(mc->ctx), seq_id),
                llama_memory_seq_pos_max(llama_get_memory(mc->ctx), seq_id)
            );

            if (ctx_dft_seq_rm_type == COMMON_CONTEXT_SEQ_RM_TYPE_FULL) {
                ckpt.update_dft(mc->ctx_mtp, seq_id, LLAMA_STATE_SEQ_FLAGS_PARTIAL_ONLY | LLAMA_STATE_SEQ_FLAGS_ON_DEVICE);
            }

            common_speculative_get_draft_params(mc->spec, seq_id) = {
                /* .drafting = */ true,
                /* .n_max    = */ mc->mtp_draft_tokens,
                /* .n_past   = */ n_past,
                /* .id_last  = */ id_last,
                /* .prompt   = */ &prompt_tgt,
                /* .result   = */ &draft,
            };
            common_speculative_draft(mc->spec);
            drafted_tokens += (int)draft.size();

            if (!draft.empty() && ctx_tgt_seq_rm_type == COMMON_CONTEXT_SEQ_RM_TYPE_FULL) {
                ckpt.update_tgt(mc->ctx, seq_id, LLAMA_STATE_SEQ_FLAGS_PARTIAL_ONLY | LLAMA_STATE_SEQ_FLAGS_ON_DEVICE);
            }
            if (ctx_dft_seq_rm_type == COMMON_CONTEXT_SEQ_RM_TYPE_FULL) {
                ckpt.load_dft(mc->ctx_mtp, seq_id, LLAMA_STATE_SEQ_FLAGS_PARTIAL_ONLY | LLAMA_STATE_SEQ_FLAGS_ON_DEVICE);
                common_context_seq_rm(mc->ctx_mtp, seq_id, ckpt.pos_max + 1, -1);
            }
        }

        common_batch_clear(batch_tgt);
        common_batch_add(batch_tgt, id_last, n_past++, { seq_id }, true);
        for (size_t i = 0; i < draft.size(); ++i) {
            common_batch_add(batch_tgt, draft[i], n_past + (int)i, { seq_id }, true);
        }

        common_sampler_ptr smpl_save;
        if (ctx_tgt_seq_rm_type == COMMON_CONTEXT_SEQ_RM_TYPE_FULL) {
            smpl_save.reset(common_sampler_clone(smpl.get()));
        }

        int decode_result = 0;
        {
            NativeStageWatchdog watchdog("llama_mtp_decode", 15000, 15000);
            decode_result = llama_decode(mc->ctx, batch_tgt);
        }
        if (decode_result != 0 || !common_speculative_process(mc->spec, batch_tgt)) {
            LOGE("MTP decode failed at generated=%d result=%d", generated_tokens, decode_result);
            break;
        }

        llama_tokens ids = common_sampler_sample_and_accept_n(smpl.get(), mc->ctx, draft);
        if (ids.empty()) {
            LOGE("MTP sampler returned no tokens");
            break;
        }

        const uint32_t n_rollback = (uint32_t)(draft.size() + 1 - ids.size());
        const bool use_ckpt_tgt =
            ctx_tgt_seq_rm_type == COMMON_CONTEXT_SEQ_RM_TYPE_FULL ||
            (ctx_tgt_seq_rm_type == COMMON_CONTEXT_SEQ_RM_TYPE_RS && n_rollback > llama_n_rs_seq(mc->ctx));

        if (n_rollback > 0 && use_ckpt_tgt) {
            draft = std::move(ids);
            ckpt.load_tgt(mc->ctx, seq_id, LLAMA_STATE_SEQ_FLAGS_PARTIAL_ONLY | LLAMA_STATE_SEQ_FLAGS_ON_DEVICE);
            common_context_seq_rm(mc->ctx, seq_id, ckpt.pos_max + 1, -1);
            if (ctx_dft_seq_rm_type == COMMON_CONTEXT_SEQ_RM_TYPE_FULL) {
                ckpt.load_dft(mc->ctx_mtp, seq_id, LLAMA_STATE_SEQ_FLAGS_PARTIAL_ONLY | LLAMA_STATE_SEQ_FLAGS_ON_DEVICE);
                common_context_seq_rm(mc->ctx_mtp, seq_id, ckpt.pos_max + 1, -1);
            }
            prompt_tgt.resize((size_t)ckpt.n_tokens);
            if (smpl_save) {
                smpl = std::move(smpl_save);
            }
            n_past = (int)prompt_tgt.size();
            continue;
        }

        const int accepted_count = std::max<int>(0, (int)ids.size() - 1);
        accepted_draft_tokens += std::min<int>(accepted_count, (int)draft.size());
        common_speculative_accept(mc->spec, seq_id, (uint16_t)accepted_count);
        n_past += accepted_count;

        for (llama_token token_id : ids) {
            prompt_tgt.push_back(id_last);
            id_last = token_id;

            if (llama_vocab_is_eog(vocab, id_last)) {
                has_eos = true;
                break;
            }

            char buf[256];
            int len = llama_token_to_piece(vocab, id_last, buf, sizeof(buf), 0, true);
            if (len > 0) {
                result_text.append(buf, len);
                generated_tokens += 1;
                if (callback && callback_method) {
                    pending_callback_text.append(buf, (size_t)len);
                    const size_t safe_prefix_len = utf8_complete_prefix_length(pending_callback_text);
                    if (safe_prefix_len > 0) {
                        std::string token_text = pending_callback_text.substr(0, safe_prefix_len);
                        pending_callback_text.erase(0, safe_prefix_len);
                        if (!dispatch_token_callback(env, callback, callback_method, token_text, generated_tokens, elapsed_ms_since(total_started))) {
                            LOGE("Stopping MTP text decode after callback failure at token %d", generated_tokens);
                            callback_failed = true;
                            has_eos = true;
                            break;
                        }
                    }
                }
            }
            if (generated_tokens == 1) {
                LOGI("MTP text first token: id=%d elapsed=%lld ms", (int)id_last, elapsed_ms_since(total_started));
            }

            if (json_task && json_object_complete(result_text)) {
                LOGI("MTP text JSON complete at token %d", generated_tokens);
                has_eos = true;
                break;
            }
            if (generated_tokens >= n_predict || elapsed_over(total_started, decode_limit_ms)) {
                if (elapsed_over(total_started, decode_limit_ms)) {
                    LOGE("MTP text decode timed out after %d ms", decode_limit_ms);
                }
                has_eos = true;
                break;
            }
        }

        draft.clear();
        common_context_seq_rm(mc->ctx, seq_id, n_past, -1);
        common_context_seq_rm(mc->ctx_mtp, seq_id, n_past, -1);
    }

    if (!callback_failed && !pending_callback_text.empty()) {
        dispatch_token_callback(
            env,
            callback,
            callback_method,
            pending_callback_text,
            generated_tokens,
            elapsed_ms_since(total_started)
        );
    }

    common_speculative_print_stats(mc->spec);
    LOGI("MTP text generated chars=%zu tokens=%d drafted=%d accepted=%d elapsed=%lld ms",
         result_text.size(),
         generated_tokens,
         drafted_tokens,
         accepted_draft_tokens,
         elapsed_ms_since(total_started));

    llama_batch_free(batch_tgt);
    return result_text;
}

extern "C" {

// ===== nativeGetAvailableBackends =====

JNIEXPORT jintArray JNICALL
Java_com_chromalab_feature_processing_inference_LlamaEngine_nativeGetAvailableBackends(
    JNIEnv *env, jclass /* clazz */) {

    // 0=CPU, 1=accelerated backend (Vulkan/other ggml device if available).
    std::vector<jint> backends = { 0 };
    if (has_accelerated_backend()) {
        backends.push_back(1);
    }
    jintArray result = env->NewIntArray((jsize)backends.size());
    env->SetIntArrayRegion(result, 0, (jsize)backends.size(), backends.data());
    return result;
}

// ===== nativeLoadModel =====

JNIEXPORT jlong JNICALL
Java_com_chromalab_feature_processing_inference_LlamaEngine_nativeLoadModel(
    JNIEnv *env, jclass /* clazz */,
    jstring basePath, jstring mmprojPath,
    jint threads, jint backendCode,
    jint contextSize, jint batchSize, jint mtpDraftTokens) {

    const char *base   = env->GetStringUTFChars(basePath, nullptr);
    const char *mmproj = env->GetStringUTFChars(mmprojPath, nullptr);

    LOGI("nativeLoadModel: base=%s, mmproj=%s, threads=%d, ctx=%d, batch=%d, mtpDraftTokens=%d",
         base, mmproj ? mmproj : "", threads, contextSize, batchSize, mtpDraftTokens);

    ensure_backend_initialized();
    const bool accelerated_available = has_accelerated_backend();
    const bool accelerated_requested = backendCode != 0;
    if (accelerated_requested && !accelerated_available) {
        LOGE("GGUF accelerated backend requested but no ggml-compatible accelerated device is available");
        env->ReleaseStringUTFChars(basePath, base);
        env->ReleaseStringUTFChars(mmprojPath, mmproj);
        return 0;
    }
    const bool use_accelerated = accelerated_requested && accelerated_available;
    const bool has_mmproj = mmproj && strlen(mmproj) > 0;
    const int requested_mtp_draft_tokens = std::clamp((int)mtpDraftTokens, 0, 16);
    const bool use_mtp = requested_mtp_draft_tokens > 0 && !has_mmproj;
    if (requested_mtp_draft_tokens > 0 && has_mmproj) {
        LOGE("MTP is currently supported only for text-only GGUF chat; mmproj vision analysis requested");
        env->ReleaseStringUTFChars(basePath, base);
        env->ReleaseStringUTFChars(mmprojPath, mmproj);
        return 0;
    }

    auto *mc = new ModelContext();
    mc->backend_label = use_accelerated ? accelerated_backend_label() : "llama.cpp CPU";

    // 1) Load model
    llama_model_params model_params = llama_model_default_params();
    if (!use_accelerated) {
        // With Vulkan registered, llama.cpp's default device discovery still keeps
        // GPU devices in the model even when n_gpu_layers is 0. Force a real
        // CPU-only session for diagnostics and stable production loading.
        model_params.split_mode = LLAMA_SPLIT_MODE_NONE;
        model_params.main_gpu = -1;
    }
    const std::vector<int> gpu_layer_attempts = use_accelerated
        ? std::vector<int>{4}
        : std::vector<int>{0};
    LOGI("GGUF load params: backendCode=%d accelerated_requested=%d accelerated_available=%d gpu_layer_attempts=%zu split_mode=%d main_gpu=%d threads=%d requested_ctx=%d requested_batch=%d",
         backendCode,
         accelerated_requested ? 1 : 0,
         accelerated_available ? 1 : 0,
         gpu_layer_attempts.size(),
         (int)model_params.split_mode,
         (int)model_params.main_gpu,
         threads,
         contextSize,
         batchSize);
    int loaded_gpu_layers = 0;
    for (int gpu_layers : gpu_layer_attempts) {
        model_params.n_gpu_layers = gpu_layers;
        LOGI("GGUF native stage start: llama_model_load gpu_layers=%d", gpu_layers);
        NativeStageWatchdog watchdog("llama_model_load", 30000, 30000);
        mc->model = llama_model_load_from_file(base, model_params);
        if (mc->model) {
            loaded_gpu_layers = gpu_layers;
            break;
        }
        LOGE("Failed to load model attempt: %s gpu_layers=%d", base, gpu_layers);
    }
    if (!mc->model) {
        LOGE("Failed to load model: %s", base);
        env->ReleaseStringUTFChars(basePath, base);
        env->ReleaseStringUTFChars(mmprojPath, mmproj);
        delete mc;
        return 0;
    }
    if (use_accelerated) {
        mc->backend_label += " (" + std::to_string(loaded_gpu_layers) + " layers)";
    }

    // 2) Create context
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx     = std::clamp((int)contextSize, 1024, 8192);
    ctx_params.n_batch   = std::clamp((int)batchSize, 64, 512);
    ctx_params.n_rs_seq  = use_mtp ? (uint32_t)requested_mtp_draft_tokens : 0u;
    ctx_params.n_threads = (threads > 0) ? threads : 4;
    ctx_params.n_threads_batch = ctx_params.n_threads;
    LOGI("GGUF context params: n_ctx=%d n_batch=%d n_rs_seq=%d n_threads=%d n_threads_batch=%d",
         (int)ctx_params.n_ctx, (int)ctx_params.n_batch,
         (int)ctx_params.n_rs_seq,
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

    if (use_mtp) {
        llama_context_params mtp_params = ctx_params;
        mtp_params.ctx_type = LLAMA_CONTEXT_TYPE_MTP;
        mtp_params.n_rs_seq = 0;

        LOGI("MTP draft context params: n_ctx=%d n_batch=%d draft_n_max=%d",
             (int)mtp_params.n_ctx,
             (int)mtp_params.n_batch,
             requested_mtp_draft_tokens);
        {
            NativeStageWatchdog watchdog("llama_mtp_context_init", 30000, 30000);
            mc->ctx_mtp = llama_init_from_model(mc->model, mtp_params);
        }
        if (!mc->ctx_mtp) {
            LOGE("Failed to create MTP draft context; the selected GGUF likely has no NextN/MTP head");
            llama_free(mc->ctx);
            llama_model_free(mc->model);
            env->ReleaseStringUTFChars(basePath, base);
            env->ReleaseStringUTFChars(mmprojPath, mmproj);
            delete mc;
            return 0;
        }

        common_params_speculative spec_params;
        spec_params.types = { COMMON_SPECULATIVE_TYPE_DRAFT_MTP };
        spec_params.draft.n_max = requested_mtp_draft_tokens;
        spec_params.draft.n_min = 0;
        spec_params.draft.ctx_tgt = mc->ctx;
        spec_params.draft.ctx_dft = mc->ctx_mtp;
        try {
            mc->spec = common_speculative_init(spec_params, 1);
        } catch (...) {
            LOGE("Failed to initialize MTP speculative context");
            llama_free(mc->ctx_mtp);
            llama_free(mc->ctx);
            llama_model_free(mc->model);
            env->ReleaseStringUTFChars(basePath, base);
            env->ReleaseStringUTFChars(mmprojPath, mmproj);
            delete mc;
            return 0;
        }

        if (!mc->spec) {
            LOGE("MTP speculative context unavailable");
            llama_free(mc->ctx_mtp);
            llama_free(mc->ctx);
            llama_model_free(mc->model);
            env->ReleaseStringUTFChars(basePath, base);
            env->ReleaseStringUTFChars(mmprojPath, mmproj);
            delete mc;
            return 0;
        }

        mc->mtp_draft_tokens = requested_mtp_draft_tokens;
        mc->backend_label += " + MTP draft-mtp(n=" + std::to_string(mc->mtp_draft_tokens) + ")";
        LOGI("MTP draft-mtp initialized: n_max=%d", mc->mtp_draft_tokens);
    }

    // 3) Load vision encoder (mmproj) if provided
    if (has_mmproj) {
        mtmd_context_params mtmd_params = mtmd_context_params_default();
        mtmd_params.n_threads = ctx_params.n_threads;
        mtmd_params.use_gpu = use_accelerated;
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

    if (mc->spec) { common_speculative_free(mc->spec); mc->spec = nullptr; }
    if (mc->mtmd) { mtmd_free(mc->mtmd); mc->mtmd = nullptr; }
    if (mc->ctx_mtp) { llama_free(mc->ctx_mtp); mc->ctx_mtp = nullptr; }
    if (mc->ctx)  { llama_free(mc->ctx);  mc->ctx  = nullptr; }
    if (mc->model){ llama_model_free(mc->model); mc->model = nullptr; }

    delete mc;
    LOGI("Model unloaded");
}

// ===== nativeGetLoadedBackendName =====

JNIEXPORT jstring JNICALL
Java_com_chromalab_feature_processing_inference_LlamaEngine_nativeGetLoadedBackendName(
    JNIEnv *env, jclass /* clazz */, jlong handle) {

    auto *mc = reinterpret_cast<ModelContext *>(handle);
    if (!mc) {
        return env->NewStringUTF("");
    }
    return env->NewStringUTF(mc->backend_label.c_str());
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
    return new_jstring_from_utf8_bytes(env, result_text);
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

    std::string result_text = run_text_completion_mtp(
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
    return new_jstring_from_utf8_bytes(env, result_text);
}

// ===== nativeInferChat =====

JNIEXPORT jstring JNICALL
Java_com_chromalab_feature_processing_inference_LlamaEngine_nativeInferChat(
    JNIEnv *env, jclass /* clazz */,
    jlong handle, jobjectArray roles, jobjectArray contents,
    jint maxTokens,
    jfloat temperature, jfloat topP, jint topK,
    jfloat repeatPenalty, jint repeatLastN) {

    auto *mc = reinterpret_cast<ModelContext *>(handle);
    if (!mc || !mc->ctx || !mc->model) {
        LOGE("Invalid model context for chat inference");
        return env->NewStringUTF("");
    }

    const std::vector<std::string> role_values = jstring_array_to_vector(env, roles);
    const std::vector<std::string> content_values = jstring_array_to_vector(env, contents);
    const std::string prompt = apply_model_chat_template(mc, role_values, content_values);
    if (prompt.empty()) {
        return env->NewStringUTF("");
    }

    const int n_predict = (maxTokens > 0) ? maxTokens : 512;
    const float rep_penalty = (repeatPenalty > 0.0f) ? repeatPenalty : 1.1f;
    const int rep_last_n = (repeatLastN > 0) ? repeatLastN : 64;

    std::string result_text = run_text_completion_mtp(
        mc,
        prompt.c_str(),
        n_predict,
        temperature,
        topP,
        topK,
        rep_penalty,
        rep_last_n
    );

    LOGI("Generated native-chat %zu chars", result_text.size());
    return new_jstring_from_utf8_bytes(env, result_text);
}

// ===== nativeInferChatStreaming =====

JNIEXPORT jstring JNICALL
Java_com_chromalab_feature_processing_inference_LlamaEngine_nativeInferChatStreaming(
    JNIEnv *env, jclass /* clazz */,
    jlong handle, jobjectArray roles, jobjectArray contents,
    jint maxTokens,
    jfloat temperature, jfloat topP, jint topK,
    jfloat repeatPenalty, jint repeatLastN,
    jobject callback) {

    auto *mc = reinterpret_cast<ModelContext *>(handle);
    if (!mc || !mc->ctx || !mc->model) {
        LOGE("Invalid model context for streaming chat inference");
        return env->NewStringUTF("");
    }

    const std::vector<std::string> role_values = jstring_array_to_vector(env, roles);
    const std::vector<std::string> content_values = jstring_array_to_vector(env, contents);
    const std::string prompt = apply_model_chat_template(mc, role_values, content_values);
    if (prompt.empty()) {
        return env->NewStringUTF("");
    }

    const int n_predict = (maxTokens > 0) ? maxTokens : 512;
    const float rep_penalty = (repeatPenalty > 0.0f) ? repeatPenalty : 1.1f;
    const int rep_last_n = (repeatLastN > 0) ? repeatLastN : 64;

    std::string result_text = run_text_completion_mtp(
        mc,
        prompt.c_str(),
        n_predict,
        temperature,
        topP,
        topK,
        rep_penalty,
        rep_last_n,
        env,
        callback
    );

    LOGI("Generated streaming native-chat %zu chars", result_text.size());
    return new_jstring_from_utf8_bytes(env, result_text);
}

} // extern "C"

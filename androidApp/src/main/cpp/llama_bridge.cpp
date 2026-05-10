/**
 * llama_bridge.cpp — JNI bridge between Kotlin and llama.cpp
 *
 * This is a STUB implementation for Phase 1.
 * Full llama.cpp integration requires:
 *   1. Cloning llama.cpp into the project (or as submodule)
 *   2. Building with CMake + Android NDK
 *   3. Linking against ggml + llama static libraries
 *
 * Vision (multimodal) support requires llava/clip headers from llama.cpp.
 */

#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "LlamaBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {

/**
 * Returns available acceleration backends on this device.
 * 0=CPU, 1=Vulkan, 2=OpenCL, 3=Hexagon
 */
JNIEXPORT jintArray JNICALL
Java_com_chromalab_feature_processing_inference_LlamaEngine_00024Companion_nativeGetAvailableBackends(
    JNIEnv *env, jobject /* this */) {

    LOGI("nativeGetAvailableBackends called");

    // TODO: Probe actual device capabilities (Vulkan support, OpenCL, Hexagon)
    // For now, report CPU only
    jint backends[] = { 0 }; // CPU=0
    int count = 1;

    jintArray result = env->NewIntArray(count);
    env->SetIntArrayRegion(result, 0, count, backends);
    return result;
}

/**
 * Load a GGUF model + vision projector.
 * Returns a handle (pointer cast to jlong), or 0 on failure.
 */
JNIEXPORT jlong JNICALL
Java_com_chromalab_feature_processing_inference_LlamaEngine_00024Companion_nativeLoadModel(
    JNIEnv *env, jobject /* this */,
    jstring basePath, jstring mmprojPath,
    jint threads, jint backendCode) {

    const char *base = env->GetStringUTFChars(basePath, nullptr);
    const char *mmproj = env->GetStringUTFChars(mmprojPath, nullptr);

    LOGI("nativeLoadModel: base=%s, mmproj=%s, threads=%d, backend=%d",
         base, mmproj, threads, backendCode);

    // TODO: Actual llama.cpp integration:
    // 1. llama_model_params params = llama_model_default_params();
    // 2. params.n_gpu_layers = (backendCode > 0) ? 99 : 0;
    // 3. llama_model *model = llama_model_load_from_file(base, params);
    // 4. Load mmproj via clip_model_load()
    // 5. Return model pointer as handle

    env->ReleaseStringUTFChars(basePath, base);
    env->ReleaseStringUTFChars(mmprojPath, mmproj);

    // Stub: return a non-zero placeholder handle
    LOGI("nativeLoadModel: STUB — returning placeholder handle");
    return (jlong) 1;
}

/**
 * Unload model and free resources.
 */
JNIEXPORT void JNICALL
Java_com_chromalab_feature_processing_inference_LlamaEngine_00024Companion_nativeUnloadModel(
    JNIEnv *env, jobject /* this */, jlong handle) {

    LOGI("nativeUnloadModel: handle=%lld", (long long) handle);

    // TODO: llama_model_free((llama_model*) handle);
    LOGI("nativeUnloadModel: STUB — freed");
}

/**
 * Run inference with an image + text prompt.
 * Returns the model's text response.
 */
JNIEXPORT jstring JNICALL
Java_com_chromalab_feature_processing_inference_LlamaEngine_00024Companion_nativeInferWithImage(
    JNIEnv *env, jobject /* this */,
    jlong handle, jstring imagePath, jstring prompt) {

    const char *img = env->GetStringUTFChars(imagePath, nullptr);
    const char *pmt = env->GetStringUTFChars(prompt, nullptr);

    LOGI("nativeInferWithImage: handle=%lld, image=%s", (long long) handle, img);

    // TODO: Actual inference:
    // 1. Load image via stb_image or Android bitmap
    // 2. Encode via clip_image_encode()
    // 3. Build prompt with image embeddings
    // 4. llama_decode() loop to generate tokens
    // 5. Collect output text

    env->ReleaseStringUTFChars(imagePath, img);
    env->ReleaseStringUTFChars(prompt, pmt);

    // Stub response
    LOGI("nativeInferWithImage: STUB — returning empty JSON");
    return env->NewStringUTF("{\"x\": [], \"y\": []}");
}

} // extern "C"

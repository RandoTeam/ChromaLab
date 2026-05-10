package com.chromalab.feature.processing.inference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * llama.cpp inference engine for .gguf models.
 * Uses JNI bridge to native llama.cpp library (b9101, May 2026).
 *
 * Supports multimodal inference via mtmd (Qwen3.5-VL, Gemma 4 VL, etc.).
 * Vision models require base .gguf + mmproj .gguf pair.
 */
class LlamaEngine : InferenceEngine {

    private var modelHandle: Long = 0L
    private var backendName: String = "llama.cpp CPU"
    private var loaded: Boolean = false

    companion object {
        private var nativeLoaded = false

        /**
         * Load the native llama_bridge library.
         * Must be called before any engine operations.
         */
        fun loadNativeLibrary() {
            if (!nativeLoaded) {
                try {
                    System.loadLibrary("llama_bridge")
                    nativeLoaded = true
                    println("LLAMA[NATIVE] Library loaded successfully")
                } catch (e: UnsatisfiedLinkError) {
                    println("LLAMA[NATIVE] Failed to load library: ${e.message}")
                }
            }
        }

        // JNI native methods
        @JvmStatic private external fun nativeGetAvailableBackends(): IntArray
        @JvmStatic private external fun nativeLoadModel(
            basePath: String,
            mmprojPath: String,
            threads: Int,
            backendCode: Int,
        ): Long
        @JvmStatic private external fun nativeUnloadModel(handle: Long)
        @JvmStatic private external fun nativeInferWithImage(
            handle: Long,
            imagePath: String,
            prompt: String,
        ): String
    }

    /**
     * Load a GGUF model + optional vision projector.
     *
     * @param basePath path to base model .gguf
     * @param mmprojPath path to vision projector .gguf (empty string if none)
     * @param threads number of CPU threads (1..N)
     */
    suspend fun loadModel(
        basePath: String,
        mmprojPath: String,
        threads: Int = 4,
    ) = withContext(Dispatchers.IO) {
        if (!nativeLoaded) {
            loadNativeLibrary()
        }

        require(nativeLoaded) { "Native library not available" }

        println("LLAMA[LOAD] Loading model: $basePath + $mmprojPath, threads=$threads")

        modelHandle = nativeLoadModel(basePath, mmprojPath, threads, 0)
        if (modelHandle == 0L) {
            loaded = false
            throw RuntimeException("Failed to load model: $basePath")
        }

        backendName = "llama.cpp CPU"
        loaded = true
        println("LLAMA[LOAD] Model loaded, handle=$modelHandle")
    }

    override suspend fun analyzeChart(imagePath: String, prompt: String): ChartAnalysis {
        check(loaded && nativeLoaded) { "Model not loaded" }

        return withContext(Dispatchers.IO) {
            println("LLAMA[INFER] Analyzing chart: $imagePath")

            val responseText = nativeInferWithImage(modelHandle, imagePath, prompt)
            println("LLAMA[INFER] Response length: ${responseText.length}")
            ChartPrompts.parseResponse(responseText)
        }
    }

    override fun isLoaded(): Boolean = loaded && nativeLoaded

    override fun unload() {
        if (nativeLoaded && modelHandle != 0L) {
            try {
                nativeUnloadModel(modelHandle)
            } catch (e: Exception) {
                println("LLAMA[UNLOAD] Error: ${e.message}")
            }
        }
        modelHandle = 0L
        loaded = false
        println("LLAMA[UNLOAD] Model unloaded")
    }

    override fun getBackendName(): String = backendName
}

package com.chromalab.feature.processing.inference

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * llama.cpp inference engine for .gguf models.
 * Uses JNI bridge to native llama.cpp library.
 * Supports CPU, Vulkan GPU, OpenCL, and Hexagon NPU backends.
 *
 * Models: Qwen3.5-VL (0.8B, 2B, 4B, 9B) from HuggingFace.
 * Vision models require base .gguf + mmproj .gguf pair.
 */
class LlamaEngine : InferenceEngine {

    private var modelHandle: Long = 0L
    private var backendName: String = "llama.cpp CPU"
    private var loaded: Boolean = false

    /**
     * Supported llama.cpp backends.
     */
    enum class Backend {
        CPU,
        VULKAN,     // Vulkan GPU — broad device support
        OPENCL,     // OpenCL — optimized for Qualcomm Adreno
        HEXAGON,    // Hexagon NPU — Snapdragon only
    }

    companion object {
        private var nativeLoaded = false

        /**
         * Load the native llama.cpp library.
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
                    println("LLAMA[NATIVE] llama.cpp NDK build may not be configured yet")
                }
            }
        }

        /**
         * Check which backends are available on this device.
         */
        fun getAvailableBackends(): List<Backend> {
            if (!nativeLoaded) return listOf(Backend.CPU)
            return try {
                val codes = nativeGetAvailableBackends()
                codes.toList().mapNotNull { code ->
                    when (code) {
                        0 -> Backend.CPU
                        1 -> Backend.VULKAN
                        2 -> Backend.OPENCL
                        3 -> Backend.HEXAGON
                        else -> null
                    }
                }
            } catch (e: Exception) {
                listOf(Backend.CPU)
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
     * Load a GGUF model + vision projector.
     *
     * @param basePath path to base model .gguf
     * @param mmprojPath path to vision projector .gguf
     * @param threads number of CPU threads (1..N)
     * @param backend acceleration backend
     */
    suspend fun loadModel(
        basePath: String,
        mmprojPath: String,
        threads: Int = 4,
        backend: Backend = Backend.CPU,
    ) = withContext(Dispatchers.IO) {
        if (!nativeLoaded) {
            loadNativeLibrary()
        }

        try {
            println("LLAMA[LOAD] Loading model: $basePath + $mmprojPath, threads=$threads, backend=$backend")

            if (nativeLoaded) {
                val backendCode = when (backend) {
                    Backend.CPU -> 0
                    Backend.VULKAN -> 1
                    Backend.OPENCL -> 2
                    Backend.HEXAGON -> 3
                }
                modelHandle = nativeLoadModel(basePath, mmprojPath, threads, backendCode)
                if (modelHandle == 0L) {
                    throw RuntimeException("nativeLoadModel returned null handle")
                }
            } else {
                println("LLAMA[LOAD] Native library not available — placeholder mode")
                modelHandle = -1L // Placeholder
            }

            backendName = "llama.cpp $backend"
            loaded = true
            println("LLAMA[LOAD] Model loaded successfully, handle=$modelHandle")
        } catch (e: Exception) {
            println("LLAMA[LOAD] Failed: ${e.message}")
            loaded = false
            throw e
        }
    }

    override suspend fun analyzeChart(imagePath: String, prompt: String): ChartAnalysis {
        check(loaded) { "llama.cpp model not loaded. Call loadModel() first." }

        return withContext(Dispatchers.IO) {
            try {
                println("LLAMA[INFER] Analyzing chart: $imagePath")

                if (nativeLoaded && modelHandle > 0) {
                    val responseText = nativeInferWithImage(modelHandle, imagePath, prompt)
                    println("LLAMA[INFER] Response length: ${responseText.length}")
                    ChartPrompts.parseResponse(responseText)
                } else {
                    println("LLAMA[INFER] Native not available — returning empty result")
                    ChartAnalysis(xValues = emptyList(), yValues = emptyList(), confidence = 0f)
                }
            } catch (e: Exception) {
                println("LLAMA[INFER] Error: ${e.message}")
                ChartAnalysis(xValues = emptyList(), yValues = emptyList(), confidence = 0f)
            }
        }
    }

    override fun isLoaded(): Boolean = loaded

    override fun unload() {
        if (nativeLoaded && modelHandle > 0) {
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

package com.chromalab.feature.processing.inference

import com.chromalab.feature.processing.inference.InferenceConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
    private var hasVisionProjector: Boolean = false
    private var config: InferenceConfig = InferenceConfig.DEFAULT
    private val nativeLock = ReentrantLock()

    @Volatile
    private var unloadRequested: Boolean = false

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
            contextSize: Int,
            batchSize: Int,
        ): Long
        @JvmStatic private external fun nativeUnloadModel(handle: Long)
        @JvmStatic private external fun nativeInferWithImage(
            handle: Long,
            imagePath: String,
            prompt: String,
            maxTokens: Int,
            temperature: Float,
            topP: Float,
            topK: Int,
            repeatPenalty: Float,
            repeatLastN: Int,
        ): String
        @JvmStatic private external fun nativeInferText(
            handle: Long,
            prompt: String,
            maxTokens: Int,
            temperature: Float,
            topP: Float,
            topK: Int,
            repeatPenalty: Float,
            repeatLastN: Int,
        ): String
    }

    /**
     * Load a GGUF model + optional vision projector.
     *
     * @param basePath path to base model .gguf
     * @param mmprojPath path to vision projector .gguf (empty string if none)
     * @param threads number of CPU threads (1..N)
     * @param modelFamily model family for auto-selecting InferenceConfig
     */
    suspend fun loadModel(
        basePath: String,
        mmprojPath: String,
        threads: Int = 4,
        modelFamily: String = "",
        contextSize: Int? = null,
        batchSize: Int? = null,
    ) = withContext(Dispatchers.IO) {
        nativeLock.withLock {
            if (!nativeLoaded) {
                loadNativeLibrary()
            }

            require(nativeLoaded) { "Native library not available" }

            if (modelHandle != 0L) {
                unloadLocked()
            }
            unloadRequested = false

            // Auto-select inference config for this model family
            config = InferenceConfig.forModelFamily(modelFamily)
            val ctx = contextSize ?: config.contextSize
            val batch = batchSize ?: config.batchSize
            println("LLAMA[LOAD] Loading model: $basePath + $mmprojPath, threads=$threads")
            println("LLAMA[LOAD] Config: maxTokens=${config.maxTokens}, repeatPenalty=${config.repeatPenalty}, repeatLastN=${config.repeatLastN}, ctx=$ctx, batch=$batch")

            modelHandle = nativeLoadModel(basePath, mmprojPath, threads, 0, ctx, batch)
            if (modelHandle == 0L) {
                loaded = false
                hasVisionProjector = false
                throw RuntimeException("Failed to load model: $basePath")
            }

            backendName = "llama.cpp CPU"
            loaded = true
            hasVisionProjector = mmprojPath.isNotBlank()
            println("LLAMA[LOAD] Model loaded, handle=$modelHandle")
        }
    }

    override suspend fun analyzeChart(imagePath: String, prompt: String): ChartAnalysis {
        return withContext(Dispatchers.IO) {
            nativeLock.withLock {
                check(loaded && nativeLoaded) { "Model not loaded" }
                check(hasVisionProjector) { "GGUF image analysis requires an mmproj vision projector" }

                println("LLAMA[INFER] Analyzing chart: $imagePath")
                println("LLAMA[INFER] Config: maxTokens=${config.maxTokens}, repeatPenalty=${config.repeatPenalty}")

                try {
                    val responseText = nativeInferWithImage(
                        modelHandle, imagePath, prompt,
                        config.maxTokens,
                        0f,
                        1f,
                        0,
                        config.repeatPenalty,
                        config.repeatLastN,
                    )
                    println("LLAMA[INFER] Response length: ${responseText.length}")
                    ChartPrompts.parseResponse(responseText)
                } finally {
                    unloadIfRequestedLocked()
                }
            }
        }
    }

    override suspend fun inferRaw(
        imagePath: String,
        prompt: String,
        options: GenerationOptions,
    ): String {
        return withContext(Dispatchers.IO) {
            nativeLock.withLock {
                check(loaded && nativeLoaded) { "Model not loaded" }

                println("LLAMA[RAW] Inferring: $imagePath")
                val maxTokens = options.maxTokens ?: config.maxTokens
                val temperature = options.temperature ?: 0f
                val topP = options.topP ?: 1f
                val topK = options.topK ?: 0
                val repeatPenalty = options.repeatPenalty ?: config.repeatPenalty
                val repeatLastN = options.repeatLastN ?: config.repeatLastN
                val hasImage = imagePath.isNotBlank() && File(imagePath).isFile
                try {
                    val responseText = if (hasImage && hasVisionProjector) {
                        nativeInferWithImage(
                            modelHandle, imagePath, prompt,
                            maxTokens,
                            temperature,
                            topP,
                            topK,
                            repeatPenalty,
                            repeatLastN,
                        )
                    } else if (hasImage) {
                        println("LLAMA[RAW] Image inference requested, but no mmproj is loaded")
                        ""
                    } else {
                        nativeInferText(
                            modelHandle, prompt,
                            maxTokens,
                            temperature,
                            topP,
                            topK,
                            repeatPenalty,
                            repeatLastN,
                        )
                    }
                    println("LLAMA[RAW] Response length: ${responseText.length}")
                    responseText
                } finally {
                    unloadIfRequestedLocked()
                }
            }
        }
    }

    override fun isLoaded(): Boolean = loaded && nativeLoaded

    override fun supportsImageInput(): Boolean = loaded && hasVisionProjector

    override fun unload() {
        if (!nativeLoaded || modelHandle == 0L) {
            loaded = false
            hasVisionProjector = false
            return
        }

        if (!nativeLock.tryLock()) {
            unloadRequested = true
            loaded = false
            hasVisionProjector = false
            println("LLAMA[UNLOAD] Deferred until active inference finishes")
            return
        }

        try {
            unloadLocked()
        } finally {
            nativeLock.unlock()
        }
    }

    private fun unloadIfRequestedLocked() {
        if (unloadRequested && modelHandle != 0L) {
            unloadLocked()
        }
    }

    private fun unloadLocked() {
        if (!nativeLoaded || modelHandle == 0L) return
        try {
            nativeUnloadModel(modelHandle)
        } catch (e: Exception) {
            println("LLAMA[UNLOAD] Error: ${e.message}")
        }
        modelHandle = 0L
        loaded = false
        hasVisionProjector = false
        unloadRequested = false
        println("LLAMA[UNLOAD] Model unloaded")
    }

    override fun getBackendName(): String = backendName
}

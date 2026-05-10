package com.chromalab.feature.processing.inference

import kotlinx.serialization.Serializable

/**
 * Abstraction over on-device VLM inference engines.
 * Two implementations:
 *  - LiteRTEngine  (.litertlm models, NPU/GPU accelerated)
 *  - LlamaEngine   (.gguf models, CPU/Vulkan)
 */
interface InferenceEngine {
    /** Analyze a chart image and return extracted axis data. */
    suspend fun analyzeChart(imagePath: String, prompt: String): ChartAnalysis

    /** Whether a model is currently loaded and ready. */
    fun isLoaded(): Boolean

    /** Unload the current model and free resources. */
    fun unload()

    /** Human-readable backend description, e.g. "LiteRT NPU", "llama.cpp Vulkan". */
    fun getBackendName(): String
}

/**
 * Structured result from VLM chart analysis.
 * Replaces heuristic-based OCR classification.
 */
@Serializable
data class ChartAnalysis(
    val xValues: List<Float>,
    val yValues: List<Float>,
    val xUnit: String? = null,
    val yUnit: String? = null,
    val xLabel: String? = null,
    val yLabel: String? = null,
    val confidence: Float = 0f,
)

/**
 * Which runtime a model uses.
 */
enum class ModelRuntime {
    /** Google LiteRT-LM — .litertlm format, NPU/GPU acceleration. */
    LITERT_LM,
    /** llama.cpp — .gguf format, CPU/Vulkan/Hexagon. */
    LLAMA_CPP,
}

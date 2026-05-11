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

    /**
     * Run raw inference — send prompt + image, return raw text response.
     * Used for non-axis prompts (graph region, axis structure).
     */
    suspend fun inferRaw(imagePath: String, prompt: String): String

    /** Whether a model is currently loaded and ready. */
    fun isLoaded(): Boolean

    /** Unload the current model and free resources. */
    fun unload()

    /** Human-readable backend description, e.g. "LiteRT NPU", "llama.cpp Vulkan". */
    fun getBackendName(): String
}

/**
 * Structured result from VLM chart analysis.
 *
 * Core fields (axis extraction):
 * - xValues, yValues: numeric tick labels read from the chart axes
 * - xUnit, yUnit: axis units (e.g. "min", "mAU")
 * - xLabel, yLabel: full axis title text (e.g. "Retention Time")
 *
 * Extended fields (structural analysis):
 * - graphRegion: VLM-detected bounding box of the plot area
 * - numGraphs: number of separate charts detected in the image
 * - axisStructure: VLM-detected axis positions and grid metadata
 *
 * Chromatography-specific fields:
 * - detectorType: detected detector type (FID, TCD, MS, UV, etc.)
 * - sampleInfo: any visible sample/method information text
 * - estimatedPeakCount: VLM estimate of visible peaks (rough hint)
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
    // Structural analysis
    val graphRegion: GraphBoundsData? = null,
    val numGraphs: Int? = null,
)

/**
 * Serializable graph bounds for embedding in ChartAnalysis.
 * Values are percentages of image dimensions (0–100).
 */
@Serializable
data class GraphBoundsData(
    val leftPct: Float,
    val topPct: Float,
    val rightPct: Float,
    val bottomPct: Float,
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

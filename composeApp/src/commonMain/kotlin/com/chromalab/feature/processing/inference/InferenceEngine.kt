package com.chromalab.feature.processing.inference

import kotlinx.coroutines.delay
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
    suspend fun inferRaw(
        imagePath: String,
        prompt: String,
        options: GenerationOptions = GenerationOptions(),
    ): String

    /**
     * Run raw inference and report incremental text updates when the runtime supports streaming.
     * Runtimes without token callbacks use a small chunked fallback so the chat UI can keep one
     * rendering path.
     */
    suspend fun inferRawStreaming(
        imagePath: String,
        prompt: String,
        options: GenerationOptions = GenerationOptions(),
        onPartial: (String) -> Unit,
    ): String {
        val response = inferRaw(imagePath, prompt, options)
        response.chunked(8).forEach { chunk ->
            onPartial(chunk)
            delay(12)
        }
        return response
    }

    /** Whether a model is currently loaded and ready. */
    fun isLoaded(): Boolean

    /** Whether the loaded runtime can accept image input without falling back to text-only mode. */
    fun supportsImageInput(): Boolean = true

    /** Unload the current model and free resources. */
    fun unload()

    /** Human-readable backend description, e.g. "LiteRT NPU", "llama.cpp Vulkan". */
    fun getBackendName(): String
}

/**
 * Optional generation controls for raw inference.
 *
 * Chart analysis uses the engine defaults, which are deterministic. Chat can
 * pass user-facing settings without changing the chromatography pipeline.
 */
data class GenerationOptions(
    val maxTokens: Int? = null,
    val timeoutMs: Long? = null,
    val temperature: Float? = null,
    val topP: Float? = null,
    val topK: Int? = null,
    val repeatPenalty: Float? = null,
    val repeatLastN: Int? = null,
    /** GGUF text-only MTP draft token budget. 0 disables MTP. */
    val mtpDraftTokens: Int? = null,
)

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
    val xTicks: List<ChartAxisTick> = emptyList(),
    val yTicks: List<ChartAxisTick> = emptyList(),
    val xLabel: String? = null,
    val yLabel: String? = null,
    val confidence: Float = 0f,
    // Structural analysis
    val graphRegion: GraphBoundsData? = null,
    val numGraphs: Int? = null,
)

/**
 * VLM axis tick with an optional normalized position in the selected graph region.
 *
 * X tick positions are normalized left-to-right. Y tick positions are normalized
 * top-to-bottom. Missing positions are allowed for legacy model responses and
 * force downstream code to use guarded geometry fallback instead of fake boxes.
 */
@Serializable
data class ChartAxisTick(
    val value: Float,
    val text: String? = null,
    val position: Float? = null,
    val confidence: Float? = null,
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

data class ActiveInferenceModel(
    val modelId: String,
    val modelName: String? = null,
    val runtime: ModelRuntime? = null,
    val backendLabel: String? = null,
)

data class ActiveInferenceModelSnapshot(
    val selectedModel: ActiveInferenceModel? = null,
    val executedModel: ActiveInferenceModel? = null,
)

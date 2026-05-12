package com.chromalab.feature.processing.inference

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.ocr.AxisOcrReader
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrStatus

/**
 * Android implementation of ChartAnalysisReader.
 *
 * VLM-first pipeline (Strategy A — always VLM first):
 *   1. Check if a VLM engine is loaded (via VlmEngineHolder singleton)
 *   2. If yes → run VLM chart analysis → convert to AxisOcrResult
 *   3. If VLM fails or unavailable → fall back to ML Kit OCR
 *
 * Prompt selection:
 *   - Qwen VL Instruct models → ChatML-wrapped prompt
 *   - Gemma / other models → raw prompt
 *   - Controlled by InferenceConfig.useChatML flag
 */
actual class ChartAnalysisReader actual constructor() {

    private val fallbackOcr = AxisOcrReader()

    actual suspend fun readAxisLabels(
        imagePath: String,
        graphRegion: GraphRegion,
    ): AxisOcrResult {
        // === Strategy A: VLM always first ===
        val engine = VlmEngineHolder.activeEngine
        val config = VlmEngineHolder.activeConfig

        if (engine != null && engine.isLoaded() && engine.supportsImageInput()) {
            try {
                val backend = engine.getBackendName()
                println("VLM[READER] Trying VLM analysis ($backend)...")

                // Select prompt based on model's prompt style
                val style = config?.promptStyle ?: PromptStyle.RAW
                val prompt = ChartPrompts.axisExtractionPrompt(style)
                println("VLM[READER] PromptStyle=$style, prompt=${prompt.take(60)}...")

                VlmEngineHolder.isInferring = true
                val analysis = try {
                    val rawResponse = engine.inferRaw(
                        imagePath = imagePath,
                        prompt = prompt,
                        options = optionsFor(VlmTask.AxisExtraction, config),
                    )
                    ChartPrompts.parseResponse(rawResponse)
                } finally {
                    VlmEngineHolder.isInferring = false
                }

                if (analysis.confidence > 0.5f &&
                    (analysis.xValues.isNotEmpty() || analysis.yValues.isNotEmpty())
                ) {
                    println("VLM[READER] Success: ${analysis.xValues.size} X, ${analysis.yValues.size} Y, conf=${analysis.confidence}")
                    return chartAnalysisToOcrResult(analysis)
                }

                val message = "AI axis extraction returned low confidence (${analysis.confidence})"
                println("VLM[READER] $message")
                if (VlmEngineHolder.requireVisionForAnalysis) {
                    throw IllegalStateException(message)
                }
            } catch (e: Exception) {
                VlmEngineHolder.isInferring = false
                println("VLM[READER] Failed: ${e.message}")
                if (VlmEngineHolder.requireVisionForAnalysis) {
                    throw IllegalStateException("AI axis extraction failed: ${e.message}", e)
                }
            }
        } else {
            val message = "AI vision model is not loaded for axis OCR"
            println("VLM[READER] $message")
            if (VlmEngineHolder.requireVisionForAnalysis) {
                throw IllegalStateException(message)
            }
        }

        // === Diagnostic fallback only when strict photo analysis is disabled ===
        return fallbackOcr.readAxisLabels(imagePath, graphRegion)
    }

    /**
     * VLM-based graph region detection.
     * Strategy A: always try VLM first, return null if unavailable.
     *
     * Returns bounding box hint for the CV-based GraphRegionDetector.
     * The CV detector will use this as a prior to refine its own detection.
     *
     * @param imagePath path to the chart image
     * @param imageWidth image width in pixels (for converting % to px)
     * @param imageHeight image height in pixels
     * @return GraphBounds if VLM succeeds, null if VLM unavailable or fails
     */
    actual suspend fun detectGraphRegion(
        imagePath: String,
        imageWidth: Int,
        imageHeight: Int,
    ): GraphBounds? {
        val engine = VlmEngineHolder.activeEngine
        val config = VlmEngineHolder.activeConfig

        if (engine == null || !engine.isLoaded() || !engine.supportsImageInput()) {
            val message = "AI vision model is not loaded for graph detection"
            println("VLM[REGION] $message")
            if (VlmEngineHolder.requireVisionForAnalysis) {
                throw IllegalStateException(message)
            }
            return null
        }

        return try {
            println("VLM[REGION] Detecting graph region via VLM...")

            val style = config?.promptStyle ?: PromptStyle.RAW
            val prompt = ChartPrompts.graphRegionPrompt(style)
            println("VLM[REGION] PromptStyle=$style, prompt=${prompt.take(60)}...")

            VlmEngineHolder.isInferring = true
            val rawResponse = try {
                engine.inferRaw(
                    imagePath = imagePath,
                    prompt = prompt,
                    options = optionsFor(VlmTask.GraphRegion, config),
                )
            } finally {
                VlmEngineHolder.isInferring = false
            }
            println("VLM[REGION] Raw response: ${rawResponse.take(200)}")

            val bounds = ChartPrompts.parseGraphRegion(rawResponse)
            if (bounds != null) {
                println("VLM[REGION] Detected: L=${bounds.leftPct}% T=${bounds.topPct}% R=${bounds.rightPct}% B=${bounds.bottomPct}% graphs=${bounds.numGraphs}")
            } else {
                val message = "AI graph detection did not return parseable bounds"
                println("VLM[REGION] $message")
                if (VlmEngineHolder.requireVisionForAnalysis) {
                    throw IllegalStateException(message)
                }
            }
            bounds
        } catch (e: Exception) {
            VlmEngineHolder.isInferring = false
            println("VLM[REGION] Error: ${e.message}")
            if (VlmEngineHolder.requireVisionForAnalysis) {
                throw IllegalStateException("AI graph detection failed: ${e.message}", e)
            }
            null
        }
    }

    /**
     * VLM-based axis structure detection.
     * Provides hints about axis positions and grid visibility.
     *
     * @return AxisStructure if VLM succeeds, null otherwise
     */
    actual suspend fun detectAxisStructure(imagePath: String): AxisStructure? {
        val engine = VlmEngineHolder.activeEngine
        val config = VlmEngineHolder.activeConfig

        if (engine == null || !engine.isLoaded() || !engine.supportsImageInput()) {
            if (VlmEngineHolder.requireVisionForAnalysis) {
                throw IllegalStateException("AI vision model is not loaded for axis structure detection")
            }
            return null
        }

        return try {
            println("VLM[STRUCT] Detecting axis structure...")

            val style = config?.promptStyle ?: PromptStyle.RAW
            val prompt = ChartPrompts.axisStructurePrompt(style)
            println("VLM[STRUCT] PromptStyle=$style, prompt=${prompt.take(60)}...")

            VlmEngineHolder.isInferring = true
            val rawResponse = try {
                engine.inferRaw(
                    imagePath = imagePath,
                    prompt = prompt,
                    options = optionsFor(VlmTask.AxisStructure, config),
                )
            } finally {
                VlmEngineHolder.isInferring = false
            }
            println("VLM[STRUCT] Raw response: ${rawResponse.take(200)}")

            val structure = ChartPrompts.parseAxisStructure(rawResponse)
            if (structure == null && VlmEngineHolder.requireVisionForAnalysis) {
                throw IllegalStateException("AI axis structure response was not parseable")
            }
            structure
        } catch (e: Exception) {
            VlmEngineHolder.isInferring = false
            println("VLM[STRUCT] Error: ${e.message}")
            if (VlmEngineHolder.requireVisionForAnalysis) {
                throw IllegalStateException("AI axis structure detection failed: ${e.message}", e)
            }
            null
        }
    }

    private fun chartAnalysisToOcrResult(analysis: ChartAnalysis): AxisOcrResult {
        return AxisOcrResult(
            // VLM values do not include reliable pixel boxes. Keep them as value hints only.
            rawElements = emptyList(),
            suggestedXValues = analysis.xValues,
            suggestedYValues = analysis.yValues,
            xUnit = analysis.xUnit,
            yUnit = analysis.yUnit,
            status = OcrStatus.ACCEPTED,
            confidence = analysis.confidence,
            timestamp = System.currentTimeMillis(),
        )
    }

    /**
     * Ensure a VLM model is loaded (lazy loading).
     * Delegates to ModelManagerController.activateForPipeline().
     */
    actual suspend fun ensureModelLoaded(onProgress: ((String) -> Unit)?): Boolean {
        // Already loaded?
        if (VlmEngineHolder.activeEngine?.isLoaded() == true &&
            VlmEngineHolder.activeEngine?.supportsImageInput() == true
        ) {
            return true
        }

        // Try lazy loading via controller
        val controller = VlmEngineHolder.controller
        if (controller == null) {
            println("VLM[LAZY] No controller — cannot auto-load model")
            return false
        }

        return try {
            controller.activateForPipeline(onProgress)
        } catch (e: Exception) {
            println("VLM[LAZY] Auto-load failed: ${e.message}")
            false
        }
    }

    actual fun currentModelSnapshot(): ActiveInferenceModelSnapshot =
        ActiveInferenceModelSnapshot(
            selectedModel = VlmEngineHolder.selectedModel,
            executedModel = VlmEngineHolder.executedModel,
        )
}

private enum class VlmTask {
    GraphRegion,
    AxisExtraction,
    AxisStructure,
}

private fun optionsFor(task: VlmTask, config: InferenceConfig?): GenerationOptions {
    val familyLimit = config?.maxTokens ?: 768
    val maxTokens = when (task) {
        VlmTask.GraphRegion -> minOf(familyLimit, 384)
        VlmTask.AxisExtraction -> minOf(familyLimit, 768)
        VlmTask.AxisStructure -> minOf(familyLimit, 384)
    }.coerceIn(128, 768)
    return GenerationOptions(
        maxTokens = maxTokens,
        timeoutMs = when (task) {
            VlmTask.GraphRegion -> 300_000L
            VlmTask.AxisExtraction -> 420_000L
            VlmTask.AxisStructure -> 300_000L
        },
        temperature = 0f,
        topP = 1f,
        topK = 0,
        repeatPenalty = config?.repeatPenalty ?: 1.1f,
        repeatLastN = config?.repeatLastN ?: 64,
    )
}

/**
 * Singleton holder for the active VLM inference engine.
 * Set by ModelManager when a model is loaded.
 */
object VlmEngineHolder {
    /** Currently active engine, or null if no model loaded. */
    var activeEngine: InferenceEngine? = null
        set(value) {
            field?.unload()
            field = value
            println("VLM[HOLDER] Active engine: ${value?.getBackendName() ?: "none"}")
        }

    /** Active model's inference config (for prompt format selection). */
    var activeConfig: InferenceConfig? = null

    var selectedModel: ActiveInferenceModel? = null

    var executedModel: ActiveInferenceModel? = null

    /** True while inference is running — prevents auto-unload. */
    @Volatile
    var isInferring: Boolean = false

    /**
     * Photo chromatogram analysis must not silently fall back to deterministic OCR
     * when the required vision model fails.
     */
    @Volatile
    var requireVisionForAnalysis: Boolean = true

    /**
     * Reference to ModelManagerController for lazy loading.
     * Set once during app initialization (from ModelManagerScreen or App).
     */
    var controller: com.chromalab.feature.settings.ModelManagerController? = null
}

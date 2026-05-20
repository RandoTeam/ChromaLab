package com.chromalab.feature.processing.inference

import android.util.Log
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.geometry.TickOcrCropArtifact
import com.chromalab.feature.processing.model.ModelRegistry
import com.chromalab.feature.processing.multimodal.StageJudgeTaskType
import com.chromalab.feature.processing.multimodal.VlmStructuredTaskContracts
import com.chromalab.feature.processing.ocr.AxisOcrReader
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrTextElement
import com.chromalab.feature.processing.ocr.OcrStatus

private const val TAG = "ChromaLabVLM"

private fun log(message: String) {
    Log.i(TAG, message)
}

private fun logError(message: String, throwable: Throwable? = null) {
    Log.e(TAG, message, throwable)
}

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
                log("Axis extraction start backend=$backend ${VlmEngineHolder.activeModelDiagnostics()}")

                // Select prompt based on model's prompt style
                val style = config?.promptStyle ?: PromptStyle.RAW
                val prompt = ChartPrompts.axisExtractionPrompt(style)
                log("Axis extraction promptStyle=$style prompt=${prompt.take(60)}...")

                var rawResponse = inferStructuredRaw(
                    engine = engine,
                    imagePath = imagePath,
                    prompt = prompt,
                    task = VlmTask.AxisExtraction,
                    config = config,
                )
                log("Axis extraction raw response chars=${rawResponse.length} preview=${rawResponse.take(200)}")
                var analysis = ChartPrompts.parseResponse(rawResponse)

                if (!analysis.hasCompleteAxisEvidence()) {
                    val retryPrompt = ChartPrompts.axisExtractionRetryPrompt(style)
                    log(
                        "Axis extraction incomplete before retry " +
                            "x=${analysis.xValues.size} y=${analysis.yValues.size} " +
                            "confidence=${analysis.confidence}",
                    )
                    log("Axis extraction retry promptStyle=$style prompt=${retryPrompt.take(60)}...")
                    val retryResponse = inferStructuredRaw(
                        engine = engine,
                        imagePath = imagePath,
                        prompt = retryPrompt,
                        task = VlmTask.AxisExtraction,
                        config = config,
                    )
                    log("Axis extraction retry raw response chars=${retryResponse.length} preview=${retryResponse.take(200)}")
                    val retryAnalysis = ChartPrompts.parseResponse(retryResponse)
                    analysis = analysis.mergeAxisEvidence(retryAnalysis)
                    log(
                        "Axis extraction merged VLM evidence " +
                            "x=${analysis.xValues.size} y=${analysis.yValues.size} " +
                            "confidence=${analysis.confidence}",
                    )
                }

                if (analysis.hasUsableAxisEvidence()) {
                    log("Axis extraction success x=${analysis.xValues.size} y=${analysis.yValues.size} confidence=${analysis.confidence}")
                    val vlmResult = chartAnalysisToOcrResult(analysis, graphRegion)
                    if (vlmResult.hasXSuggestions && vlmResult.hasYSuggestions) {
                        return vlmResult
                    }

                    log(
                        "Axis extraction incomplete; supplementing with ML Kit " +
                            "x=${vlmResult.suggestedXValues.size} y=${vlmResult.suggestedYValues.size}",
                    )
                    val fallbackResult = fallbackOcr.readAxisLabels(imagePath, graphRegion)
                    val merged = vlmResult.mergeSupplementalAxisOcr(fallbackResult)
                    log(
                        "Axis extraction merged x=${merged.suggestedXValues.size} " +
                            "y=${merged.suggestedYValues.size} warnings=${merged.warnings}",
                    )
                    if (merged.hasXSuggestions && merged.hasYSuggestions) {
                        return merged
                    }

                    val message = "AI axis extraction returned incomplete axes after supplemental OCR"
                    log(message)
                    return merged
                }

                val message = "AI axis extraction returned low confidence (${analysis.confidence})"
                log(message)
            } catch (e: Exception) {
                VlmEngineHolder.isInferring = false
                logError("Axis extraction failed: ${e.message}", e)
            }
        } else {
            val message = "AI vision model is not loaded for axis OCR"
            log(message)
        }

        // Missing VLM is a semantic-layer warning, not a blocker for deterministic/ML Kit OCR attempts.
        return fallbackOcr.readAxisLabels(imagePath, graphRegion)
    }

    actual suspend fun readTickLabelCrops(crops: List<TickOcrCropArtifact>): AxisOcrResult =
        fallbackOcr.readTickLabelCrops(crops)

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
            log(message)
            if (VlmEngineHolder.requireVisionForAnalysis) {
                throw IllegalStateException(message)
            }
            return null
        }

        return try {
            log(
                "Graph region start backend=${engine.getBackendName()} " +
                    "image=${imageWidth}x$imageHeight ${VlmEngineHolder.activeModelDiagnostics()}",
            )

            val style = config?.promptStyle ?: PromptStyle.RAW
            val prompt = ChartPrompts.graphRegionPrompt(style)
            log("Graph region promptStyle=$style prompt=${prompt.take(60)}...")

            var rawResponse = inferStructuredRaw(
                engine = engine,
                imagePath = imagePath,
                prompt = prompt,
                task = VlmTask.GraphRegion,
                config = config,
            )
            log("Graph region raw response chars=${rawResponse.length} preview=${rawResponse.take(200)}")

            var bounds = ChartPrompts.parseGraphRegion(rawResponse)
            if (bounds == null) {
                val retryPrompt = ChartPrompts.graphRegionRetryPrompt(style)
                log("Graph region retry promptStyle=$style prompt=${retryPrompt.take(60)}...")
                rawResponse = inferStructuredRaw(
                    engine = engine,
                    imagePath = imagePath,
                    prompt = retryPrompt,
                    task = VlmTask.GraphRegion,
                    config = config,
                )
                log("Graph region retry raw response chars=${rawResponse.length} preview=${rawResponse.take(200)}")
                bounds = ChartPrompts.parseGraphRegion(rawResponse)
            }
            if (bounds != null) {
                log("Graph region detected left=${bounds.leftPct} top=${bounds.topPct} right=${bounds.rightPct} bottom=${bounds.bottomPct} graphs=${bounds.numGraphs}")
            } else {
                val message = "AI graph detection did not return parseable bounds"
                log(message)
                if (VlmEngineHolder.requireVisionForAnalysis) {
                    throw IllegalStateException(message)
                }
            }
            bounds
        } catch (e: Exception) {
            VlmEngineHolder.isInferring = false
            logError("Graph region failed: ${e.message}", e)
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
            log("Axis structure start ${VlmEngineHolder.activeModelDiagnostics()}")

            val style = config?.promptStyle ?: PromptStyle.RAW
            val prompt = ChartPrompts.axisStructurePrompt(style)
            log("Axis structure promptStyle=$style prompt=${prompt.take(60)}...")

            var rawResponse = inferStructuredRaw(
                engine = engine,
                imagePath = imagePath,
                prompt = prompt,
                task = VlmTask.AxisStructure,
                config = config,
            )
            log("Axis structure raw response chars=${rawResponse.length} preview=${rawResponse.take(200)}")

            var structure = ChartPrompts.parseAxisStructure(rawResponse)
            if (structure == null) {
                val retryPrompt = ChartPrompts.axisStructureRetryPrompt(style)
                log("Axis structure retry promptStyle=$style prompt=${retryPrompt.take(60)}...")
                rawResponse = inferStructuredRaw(
                    engine = engine,
                    imagePath = imagePath,
                    prompt = retryPrompt,
                    task = VlmTask.AxisStructure,
                    config = config,
                )
                log("Axis structure retry raw response chars=${rawResponse.length} preview=${rawResponse.take(200)}")
                structure = ChartPrompts.parseAxisStructure(rawResponse)
            }
            if (structure == null && VlmEngineHolder.requireVisionForAnalysis) {
                throw IllegalStateException("AI axis structure response was not parseable")
            }
            structure
        } catch (e: Exception) {
            VlmEngineHolder.isInferring = false
            logError("Axis structure failed: ${e.message}", e)
            if (VlmEngineHolder.requireVisionForAnalysis) {
                throw IllegalStateException("AI axis structure detection failed: ${e.message}", e)
            }
            null
        }
    }

    private fun chartAnalysisToOcrResult(
        analysis: ChartAnalysis,
        graphRegion: GraphRegion,
    ): AxisOcrResult {
        val xElements = analysis.xTicks.mapNotNull { tick ->
            tick.toOcrElement(
                graphRegion = graphRegion,
                axis = AxisTickDirection.X,
                defaultConfidence = analysis.confidence,
            )
        }
        val yElements = analysis.yTicks.mapNotNull { tick ->
            tick.toOcrElement(
                graphRegion = graphRegion,
                axis = AxisTickDirection.Y,
                defaultConfidence = analysis.confidence,
            )
        }
        return AxisOcrResult(
            rawElements = xElements + yElements,
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
            if (VlmEngineHolder.activeExecutedModelIsChromatogramVision()) {
                log("Vision model already ready for chromatogram analysis: ${VlmEngineHolder.activeModelDiagnostics()}")
                return true
            }

            log("Rejecting active non-chromatogram vision model: ${VlmEngineHolder.activeModelDiagnostics()}")
            VlmEngineHolder.activeEngine = null
            VlmEngineHolder.activeConfig = null
            VlmEngineHolder.executedModel = null
        }

        // Try lazy loading via controller
        val controller = VlmEngineHolder.controller
        if (controller == null) {
            log("No controller; cannot auto-load chromatogram VLM")
            return false
        }

        return try {
            controller.activateForPipeline(onProgress)
        } catch (e: Exception) {
            logError("Auto-load failed: ${e.message}", e)
            false
        }
    }

    actual fun currentModelSnapshot(): ActiveInferenceModelSnapshot =
        ActiveInferenceModelSnapshot(
            selectedModel = VlmEngineHolder.selectedModel,
            executedModel = VlmEngineHolder.executedModel,
        )
}

private enum class AxisTickDirection {
    X,
    Y,
}

private fun ChartAxisTick.toOcrElement(
    graphRegion: GraphRegion,
    axis: AxisTickDirection,
    defaultConfidence: Float,
): OcrTextElement? {
    val normalized = position ?: return null
    val label = text ?: value.toString()
    val textWidth = (label.length * 7f).coerceAtLeast(14f)
    val textHeight = 12f
    val centerX = when (axis) {
        AxisTickDirection.X -> graphRegion.x + normalized.coerceIn(0f, 1f) * graphRegion.width
        AxisTickDirection.Y -> graphRegion.x + graphRegion.width * 0.08f
    }
    val centerY = when (axis) {
        AxisTickDirection.X -> graphRegion.y + graphRegion.height * 0.92f
        AxisTickDirection.Y -> graphRegion.y + normalized.coerceIn(0f, 1f) * graphRegion.height
    }
    return OcrTextElement(
        text = label,
        numericValue = value,
        x = centerX - textWidth / 2f,
        y = centerY - textHeight / 2f,
        width = textWidth,
        height = textHeight,
        confidence = confidence ?: defaultConfidence,
    )
}

private fun AxisOcrResult.mergeSupplementalAxisOcr(
    supplemental: AxisOcrResult,
): AxisOcrResult {
    val mergedX = if (hasXSuggestions) {
        suggestedXValues
    } else {
        supplemental.suggestedXValues
    }.stableAxisValues()
    val mergedY = if (hasYSuggestions) {
        suggestedYValues
    } else {
        supplemental.suggestedYValues
    }.stableAxisValues(descending = true)
    val mergedWarnings = buildList {
        addAll(warnings)
        addAll(supplemental.warnings)
        if (!hasXSuggestions && supplemental.hasXSuggestions) {
            add("axis_ocr.x_values_supplemented_by_mlkit")
        }
        if (!hasYSuggestions && supplemental.hasYSuggestions) {
            add("axis_ocr.y_values_supplemented_by_mlkit")
        }
        if (!hasXSuggestions && !supplemental.hasXSuggestions) {
            add("axis_ocr.x_values_missing_after_supplemental_mlkit")
        }
        if (!hasYSuggestions && !supplemental.hasYSuggestions) {
            add("axis_ocr.y_values_missing_after_supplemental_mlkit")
        }
    }.distinct()
    return copy(
        rawElements = rawElements + supplemental.rawElements,
        suggestedXValues = mergedX,
        suggestedYValues = mergedY,
        xUnit = xUnit ?: supplemental.xUnit,
        yUnit = yUnit ?: supplemental.yUnit,
        confidence = listOfNotNull(confidence, supplemental.confidence).averageOrNull(),
        warnings = mergedWarnings,
        timestamp = System.currentTimeMillis(),
    )
}

private fun List<Float>.stableAxisValues(descending: Boolean = false): List<Float> {
    val values = distinct().sorted()
    return if (descending) values.asReversed() else values
}

private fun List<Float>.averageOrNull(): Float? =
    takeIf { it.isNotEmpty() }?.average()?.toFloat()?.coerceIn(0f, 1f)

private fun ChartAnalysis.hasUsableAxisEvidence(): Boolean =
    confidence > 0.5f && (xValues.isNotEmpty() || yValues.isNotEmpty())

private fun ChartAnalysis.hasCompleteAxisEvidence(): Boolean =
    confidence > 0.5f && xValues.isNotEmpty() && yValues.isNotEmpty()

private fun ChartAnalysis.mergeAxisEvidence(other: ChartAnalysis): ChartAnalysis {
    val mergedX = (xValues + other.xValues).stableAxisValues()
    val mergedY = (yValues + other.yValues).stableAxisValues()
    val mergedXTicks = (xTicks + other.xTicks).dedupeAxisTicks()
    val mergedYTicks = (yTicks + other.yTicks).dedupeAxisTicks()
    val completenessBoost = if (mergedX.isNotEmpty() && mergedY.isNotEmpty()) 0.05f else 0f
    return copy(
        xValues = mergedX,
        yValues = mergedY,
        xTicks = mergedXTicks,
        yTicks = mergedYTicks,
        xUnit = xUnit ?: other.xUnit,
        yUnit = yUnit ?: other.yUnit,
        confidence = maxOf(confidence, other.confidence, completenessBoost).coerceIn(0f, 0.99f),
    )
}

private fun List<ChartAxisTick>.dedupeAxisTicks(): List<ChartAxisTick> =
    groupBy { tick ->
        val valueKey = (tick.value * 1000f).toInt()
        val positionKey = tick.position?.let { (it * 1000f).toInt() }
        valueKey to positionKey
    }.values.map { ticks ->
        ticks.maxBy { tick ->
            val confidence = tick.confidence ?: 0f
            val positioned = if (tick.position != null) 1f else 0f
            confidence + positioned
        }
    }.sortedBy { it.value }

private enum class VlmTask {
    GraphRegion,
    AxisExtraction,
    AxisStructure,
}

private fun VlmTask.stageJudgeTaskType(): StageJudgeTaskType =
    when (this) {
        VlmTask.GraphRegion -> StageJudgeTaskType.GRAPH_PANEL_CANDIDATE_JUDGE
        VlmTask.AxisExtraction -> StageJudgeTaskType.AXIS_TICK_VISIBILITY_JUDGE
        VlmTask.AxisStructure -> StageJudgeTaskType.AXIS_TICK_VISIBILITY_JUDGE
    }

private suspend fun inferStructuredRaw(
    engine: InferenceEngine,
    imagePath: String,
    prompt: String,
    task: VlmTask,
    config: InferenceConfig?,
): String {
    VlmEngineHolder.isInferring = true
    return try {
        engine.inferRaw(
            imagePath = imagePath,
            prompt = prompt,
            options = optionsFor(task, config),
        )
    } finally {
        VlmEngineHolder.isInferring = false
    }
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
        timeoutMs = VlmStructuredTaskContracts.contractFor(task.stageJudgeTaskType()).timeoutMillis,
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
            log("Active engine: ${value?.getBackendName() ?: "none"}")
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

    fun activeExecutedModelIsChromatogramVision(): Boolean {
        val modelId = executedModel?.modelId ?: selectedModel?.modelId ?: return false
        val knownModel = ModelRegistry.findById(modelId) ?: return true
        return ModelRegistry.isChromatogramVisionModel(knownModel)
    }

    fun activeModelDiagnostics(): String =
        "selected=${selectedModel?.modelId ?: "none"} " +
            "executed=${executedModel?.modelId ?: "none"} " +
            "backend=${executedModel?.backendLabel ?: activeEngine?.getBackendName() ?: "none"}"
}

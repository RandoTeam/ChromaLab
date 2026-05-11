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

        if (engine != null && engine.isLoaded()) {
            try {
                val backend = engine.getBackendName()
                println("VLM[READER] Trying VLM analysis ($backend)...")

                // Select prompt based on model's template requirement
                val prompt = if (config?.useChatML == true) {
                    ChartPrompts.AXIS_EXTRACTION
                } else {
                    ChartPrompts.AXIS_EXTRACTION_RAW
                }

                VlmEngineHolder.isInferring = true
                val analysis = try {
                    engine.analyzeChart(
                        imagePath = imagePath,
                        prompt = prompt,
                    )
                } finally {
                    VlmEngineHolder.isInferring = false
                }

                if (analysis.confidence > 0.5f &&
                    (analysis.xValues.isNotEmpty() || analysis.yValues.isNotEmpty())
                ) {
                    println("VLM[READER] Success: ${analysis.xValues.size} X, ${analysis.yValues.size} Y, conf=${analysis.confidence}")
                    return chartAnalysisToOcrResult(analysis)
                }

                println("VLM[READER] Low confidence (${analysis.confidence}), falling back to ML Kit OCR")
            } catch (e: Exception) {
                VlmEngineHolder.isInferring = false
                println("VLM[READER] Failed: ${e.message}, falling back to ML Kit OCR")
            }
        } else {
            println("VLM[READER] No model loaded, using ML Kit OCR")
        }

        // === Fallback: ML Kit OCR ===
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

        if (engine == null || !engine.isLoaded()) {
            println("VLM[REGION] No model loaded, skipping VLM graph detection")
            return null
        }

        return try {
            println("VLM[REGION] Detecting graph region via VLM...")

            val prompt = if (config?.useChatML == true) {
                ChartPrompts.GRAPH_REGION
            } else {
                ChartPrompts.GRAPH_REGION_RAW
            }

            VlmEngineHolder.isInferring = true
            val rawResponse = try {
                engine.inferRaw(imagePath, prompt)
            } finally {
                VlmEngineHolder.isInferring = false
            }
            println("VLM[REGION] Raw response: ${rawResponse.take(200)}")

            val bounds = ChartPrompts.parseGraphRegion(rawResponse)
            if (bounds != null) {
                println("VLM[REGION] Detected: L=${bounds.leftPct}% T=${bounds.topPct}% R=${bounds.rightPct}% B=${bounds.bottomPct}% graphs=${bounds.numGraphs}")
            } else {
                println("VLM[REGION] Failed to parse graph bounds")
            }
            bounds
        } catch (e: Exception) {
            VlmEngineHolder.isInferring = false
            println("VLM[REGION] Error: ${e.message}")
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

        if (engine == null || !engine.isLoaded()) {
            return null
        }

        return try {
            println("VLM[STRUCT] Detecting axis structure...")

            val prompt = if (config?.useChatML == true) {
                ChartPrompts.AXIS_STRUCTURE
            } else {
                ChartPrompts.AXIS_STRUCTURE_RAW
            }

            VlmEngineHolder.isInferring = true
            val rawResponse = try {
                engine.inferRaw(imagePath, prompt)
            } finally {
                VlmEngineHolder.isInferring = false
            }
            println("VLM[STRUCT] Raw response: ${rawResponse.take(200)}")

            ChartPrompts.parseAxisStructure(rawResponse)
        } catch (e: Exception) {
            VlmEngineHolder.isInferring = false
            println("VLM[STRUCT] Error: ${e.message}")
            null
        }
    }

    private fun chartAnalysisToOcrResult(analysis: ChartAnalysis): AxisOcrResult {
        val elements = (analysis.xValues + analysis.yValues).map { value ->
            com.chromalab.feature.processing.ocr.OcrTextElement(
                text = value.toString(),
                numericValue = value,
                x = 0f, y = 0f,
                width = 0f, height = 0f,
                confidence = analysis.confidence,
            )
        }

        return AxisOcrResult(
            rawElements = elements,
            suggestedXValues = analysis.xValues,
            suggestedYValues = analysis.yValues,
            xUnit = analysis.xUnit,
            yUnit = analysis.yUnit,
            status = OcrStatus.ACCEPTED,
            timestamp = System.currentTimeMillis(),
        )
    }

    /**
     * Ensure a VLM model is loaded (lazy loading).
     * Delegates to ModelManagerController.activateForPipeline().
     */
    actual suspend fun ensureModelLoaded(onProgress: ((String) -> Unit)?): Boolean {
        // Already loaded?
        if (VlmEngineHolder.activeEngine?.isLoaded() == true) {
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

    /** True while inference is running — prevents auto-unload. */
    @Volatile
    var isInferring: Boolean = false

    /**
     * Reference to ModelManagerController for lazy loading.
     * Set once during app initialization (from ModelManagerScreen or App).
     */
    var controller: com.chromalab.feature.settings.ModelManagerController? = null
}


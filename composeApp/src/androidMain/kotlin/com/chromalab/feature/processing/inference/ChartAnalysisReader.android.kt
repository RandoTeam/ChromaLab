package com.chromalab.feature.processing.inference

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.ocr.AxisOcrReader
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrStatus

/**
 * Android implementation of ChartAnalysisReader.
 *
 * VLM-first pipeline:
 *   1. Check if a VLM engine is loaded (via VlmEngineHolder singleton)
 *   2. If yes → run VLM chart analysis → convert to AxisOcrResult
 *   3. If VLM fails or unavailable → fall back to ML Kit OCR
 */
actual class ChartAnalysisReader actual constructor() {

    private val fallbackOcr = AxisOcrReader()

    actual suspend fun readAxisLabels(
        imagePath: String,
        graphRegion: GraphRegion,
    ): AxisOcrResult {
        // === Strategy 1: VLM inference ===
        val engine = VlmEngineHolder.activeEngine
        if (engine != null && engine.isLoaded()) {
            try {
                println("VLM[READER] Trying VLM analysis (${engine.getBackendName()})...")

                val analysis = engine.analyzeChart(
                    imagePath = imagePath,
                    prompt = ChartPrompts.AXIS_EXTRACTION,
                )

                if (analysis.confidence > 0.5f &&
                    (analysis.xValues.isNotEmpty() || analysis.yValues.isNotEmpty())
                ) {
                    println("VLM[READER] Success: ${analysis.xValues.size} X, ${analysis.yValues.size} Y, conf=${analysis.confidence}")
                    return chartAnalysisToOcrResult(analysis)
                }

                println("VLM[READER] Low confidence (${analysis.confidence}), falling back to ML Kit OCR")
            } catch (e: Exception) {
                println("VLM[READER] Failed: ${e.message}, falling back to ML Kit OCR")
            }
        } else {
            println("VLM[READER] No model loaded, using ML Kit OCR")
        }

        // === Strategy 2: ML Kit OCR fallback ===
        return fallbackOcr.readAxisLabels(imagePath, graphRegion)
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
}

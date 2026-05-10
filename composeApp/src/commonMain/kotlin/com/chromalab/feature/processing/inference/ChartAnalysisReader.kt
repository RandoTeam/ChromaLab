package com.chromalab.feature.processing.inference

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.ocr.AxisOcrReader
import com.chromalab.feature.processing.ocr.AxisOcrResult

/**
 * Platform-agnostic chart analysis reader.
 *
 * On Android: wraps VLM engine with ML Kit OCR fallback.
 * On Desktop: delegates directly to AxisOcrReader.
 *
 * This is the primary entry point for chart axis recognition
 * in the processing pipeline.
 */
expect class ChartAnalysisReader() {
    /**
     * Read axis labels using the best available method.
     * VLM-first on Android, ML Kit OCR fallback always available.
     */
    suspend fun readAxisLabels(imagePath: String, graphRegion: GraphRegion): AxisOcrResult
}

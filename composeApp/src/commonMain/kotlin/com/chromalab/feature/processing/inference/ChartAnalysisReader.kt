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
 *
 * VLM tasks provided:
 * 1. readAxisLabels — axis tick label OCR (VLM-first, ML Kit fallback)
 * 2. detectGraphRegion — chart plot area bounding box detection
 * 3. detectAxisStructure — axis position/grid metadata detection
 */
expect class ChartAnalysisReader() {
    /**
     * Read axis labels using the best available method.
     * VLM-first on Android, ML Kit OCR fallback always available.
     */
    suspend fun readAxisLabels(imagePath: String, graphRegion: GraphRegion): AxisOcrResult

    /**
     * Detect the graph plot area bounding box via VLM.
     * Strategy A: always try VLM first.
     *
     * @return GraphBounds (percentage-based) if VLM succeeds, null on desktop or failure
     */
    suspend fun detectGraphRegion(
        imagePath: String,
        imageWidth: Int,
        imageHeight: Int,
    ): GraphBounds?

    /**
     * Detect axis structure (positions, grid) via VLM.
     *
     * @return AxisStructure if VLM succeeds, null on desktop or failure
     */
    suspend fun detectAxisStructure(imagePath: String): AxisStructure?

    /**
     * Ensure a VLM model is loaded (lazy loading).
     * If no model is loaded, auto-loads the best available model.
     * Called at pipeline start to avoid manual model activation.
     *
     * @param onProgress optional callback for progress reporting
     * @return true if VLM is ready, false if no model available
     */
    suspend fun ensureModelLoaded(onProgress: ((String) -> Unit)? = null): Boolean
}

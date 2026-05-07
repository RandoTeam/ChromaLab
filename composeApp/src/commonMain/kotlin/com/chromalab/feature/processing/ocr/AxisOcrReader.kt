package com.chromalab.feature.processing.ocr

import com.chromalab.feature.processing.graph.GraphRegion

/**
 * Platform-specific OCR for axis labels.
 * Uses ML Kit Text Recognition on Android.
 *
 * DESIGN: OCR is a HINT, not a source of truth.
 * Results must be confirmed by the user before use.
 */
expect class AxisOcrReader() {
    /**
     * Run OCR on the image within the graph region.
     * Returns detected text elements + suggested numeric values.
     * Never throws — returns empty result on failure.
     */
    suspend fun readAxisLabels(imagePath: String, graphRegion: GraphRegion): AxisOcrResult
}

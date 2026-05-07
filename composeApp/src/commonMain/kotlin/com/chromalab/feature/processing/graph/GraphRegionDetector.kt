package com.chromalab.feature.processing.graph

/**
 * Platform-specific graph region detector.
 *
 * Detection strategy (lenient, multi-pass):
 * 1. Try Hough lines → find axis-like lines → infer bounded region
 * 2. Try contour analysis → find largest rectangular region
 * 3. Heuristic fallback → scan for high-density ink region
 * 4. Ultimate fallback → return full image with LOW confidence
 *
 * NEVER returns null — always provides a usable result.
 */
expect class GraphRegionDetector() {
    fun detect(imagePath: String, imageWidth: Int, imageHeight: Int): GraphRegionResult
}

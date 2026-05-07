package com.chromalab.feature.processing.axis

import com.chromalab.feature.processing.graph.GraphRegion

/**
 * Platform-specific axis detector.
 * Finds X and Y axes within a graph region using edge/line analysis.
 */
expect class AxisDetector() {
    /**
     * Detect axes within the given graph region of the image.
     * Returns result with detected or null axes + warnings.
     * NEVER blocks — returns partial results if only one axis found.
     */
    fun detect(imagePath: String, graphRegion: GraphRegion): AxesResult
}

package com.chromalab.feature.processing.curve

import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.graph.GraphRegion

/**
 * Platform-specific curve mask preparation.
 * Extracts candidate curve pixels from the graph region,
 * suppressing axes, grid lines, and text annotations.
 */
expect class CurveMaskPreparer() {
    /**
     * Prepare binary mask of curve candidate pixels.
     * @param imagePath Path to the preprocessed image
     * @param graphRegion The detected graph region
     * @param axes Detected axes (used for suppression)
     * @param outputDir Directory to save mask images
     * @return Mask result with raw and cleaned paths
     */
    fun prepare(
        imagePath: String,
        graphRegion: GraphRegion,
        axes: AxesResult,
        outputDir: String,
        textSuppressionRegions: List<CurveMaskTextSuppressionRegion> = emptyList(),
    ): CurveMaskResult
}

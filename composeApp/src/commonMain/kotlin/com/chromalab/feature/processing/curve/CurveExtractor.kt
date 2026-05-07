package com.chromalab.feature.processing.curve

/**
 * Platform-specific curve extractor.
 * Reads the cleaned curve mask and extracts a point-per-column polyline.
 */
expect class CurveExtractor() {
    /**
     * Extract curve points from the cleaned mask.
     * For each pixel column: find curve Y, interpolate gaps, remove outliers.
     * @param maskPath Path to the cleaned binary mask image
     * @param maskWidth Width of the mask
     * @param maskHeight Height of the mask
     * @param outputDir Directory to save visualization
     * @return Extraction result with ordered points left→right
     */
    fun extract(
        maskPath: String,
        maskWidth: Int,
        maskHeight: Int,
        outputDir: String,
    ): CurveExtractionResult
}

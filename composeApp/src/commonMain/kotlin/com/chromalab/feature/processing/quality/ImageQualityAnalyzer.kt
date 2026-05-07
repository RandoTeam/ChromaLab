package com.chromalab.feature.processing.quality

/**
 * Platform-specific image quality analyzer.
 * Android: uses Bitmap pixel analysis.
 * Desktop: stub returning default good quality.
 */
expect class ImageQualityAnalyzer() {
    /**
     * Analyze image quality from file path.
     * Returns a full quality report with 7 metrics.
     */
    fun analyze(imagePath: String, frameRatio: Float = 0.8f): ImageQualityReport
}

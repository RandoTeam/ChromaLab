package com.chromalab.feature.processing.quality

/**
 * Desktop stub — returns default good quality.
 */
actual class ImageQualityAnalyzer actual constructor() {

    actual fun analyze(imagePath: String, frameRatio: Float): ImageQualityReport {
        val good = QualityMetric("stub", 100f, QualityLevel.GOOD, "Desktop — анализ недоступен")
        return ImageQualityReport(
            blurScore = good, brightnessScore = good, contrastScore = good,
            glareScore = good, shadowScore = good, frameFillScore = good,
            skewScore = good, overallLevel = QualityLevel.GOOD,
            warnings = emptyList(), timestamp = 0L,
        )
    }
}

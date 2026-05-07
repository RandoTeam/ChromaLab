package com.chromalab.feature.processing.quality

import kotlinx.serialization.Serializable

/**
 * Quality level of an image check.
 */
enum class QualityLevel {
    GOOD,
    ACCEPTABLE,
    POOR,
}

/**
 * Individual quality metric result.
 */
@Serializable
data class QualityMetric(
    val name: String,
    val score: Float,
    val level: QualityLevel,
    val message: String,
)

/**
 * Full quality report for a captured/imported image.
 */
@Serializable
data class ImageQualityReport(
    val blurScore: QualityMetric,
    val brightnessScore: QualityMetric,
    val contrastScore: QualityMetric,
    val glareScore: QualityMetric,
    val shadowScore: QualityMetric,
    val frameFillScore: QualityMetric,
    val skewScore: QualityMetric,
    val overallLevel: QualityLevel,
    val warnings: List<String>,
    val timestamp: Long,
) {
    val canProceed: Boolean get() = true // always allow, but warn
    val hasWarnings: Boolean get() = warnings.isNotEmpty()
}

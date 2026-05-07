package com.chromalab.feature.processing.quality

import kotlinx.serialization.Serializable

/**
 * Overall digitization quality status.
 */
@Serializable
enum class QualityStatus {
    GOOD,
    ACCEPTABLE,
    RISKY,
    FAILED,
}

/**
 * Quality score for a single pipeline stage.
 */
@Serializable
data class StageQuality(
    val stage: String,
    val score: Float, // 0..1
    val warnings: List<String>,
) {
    val status: QualityStatus
        get() = when {
            score >= 0.8f -> QualityStatus.GOOD
            score >= 0.5f -> QualityStatus.ACCEPTABLE
            score >= 0.2f -> QualityStatus.RISKY
            else -> QualityStatus.FAILED
        }
}

/**
 * Complete digitization quality report.
 */
@Serializable
data class DigitizationQualityReport(
    val imageQuality: StageQuality,
    val documentDetection: StageQuality,
    val graphDetection: StageQuality,
    val axisCalibration: StageQuality,
    val curveExtraction: StageQuality,
    val overall: StageQuality,
    val timestamp: Long,
) {
    val allWarnings: List<String>
        get() = listOf(
            imageQuality, documentDetection, graphDetection,
            axisCalibration, curveExtraction,
        ).flatMap { it.warnings }
}

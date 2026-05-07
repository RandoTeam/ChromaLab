package com.chromalab.feature.processing.quality

import kotlinx.serialization.Serializable

/**
 * Severity level for pipeline warnings.
 */
@Serializable
enum class WarningSeverity {
    /** Informational — does not affect quality */
    INFO,
    /** May affect quality, user should be aware */
    WARNING,
    /** Likely affects quality, user should review */
    ERROR,
}

/**
 * Typed warning categories matching the spec.
 */
@Serializable
enum class WarningType {
    LOW_SHARPNESS,
    LOW_CONTRAST,
    STRONG_PERSPECTIVE,
    GLARE_DETECTED,
    UNEVEN_LIGHTING,
    GRAPH_MANUAL_SELECTION,
    OCR_NOT_USED,
    OCR_CORRECTED,
    AXES_MANUAL,
    CURVE_GAPS,
    CURVE_INTERPOLATED,
    MULTIPLE_LINE_CANDIDATES,
    QUALITY_INSUFFICIENT,
}

/**
 * A single pipeline warning with type, severity, and user-facing message.
 */
@Serializable
data class PipelineWarning(
    val type: WarningType,
    val severity: WarningSeverity,
    val message: String,
    val stage: String,
    val detail: String? = null,
)

/**
 * Builds typed warnings from pipeline state.
 * Each warning is explicitly defined — no hidden logic.
 */
object WarningCollector {

    /**
     * Collect all warnings from the quality report and pipeline flags.
     */
    fun collect(
        report: DigitizationQualityReport,
        ocrUsed: Boolean,
        ocrCorrected: Boolean,
        graphManual: Boolean,
        axesManual: Boolean,
        curveGapCount: Int,
        interpolatedRatio: Float,
        multipleLineCandidates: Boolean,
    ): List<PipelineWarning> {
        val warnings = mutableListOf<PipelineWarning>()

        // Image warnings
        if (report.imageQuality.score < 0.5f) {
            report.imageQuality.warnings.forEach { msg ->
                val type = when {
                    "резкость" in msg.lowercase() -> WarningType.LOW_SHARPNESS
                    "контраст" in msg.lowercase() -> WarningType.LOW_CONTRAST
                    else -> WarningType.LOW_SHARPNESS
                }
                warnings.add(
                    PipelineWarning(type, WarningSeverity.WARNING, msg, "image"),
                )
            }
        }

        // Document warnings
        report.documentDetection.warnings.forEach { msg ->
            val type = when {
                "перспектива" in msg.lowercase() -> WarningType.STRONG_PERSPECTIVE
                "блик" in msg.lowercase() -> WarningType.GLARE_DETECTED
                "освещение" in msg.lowercase() -> WarningType.UNEVEN_LIGHTING
                else -> WarningType.STRONG_PERSPECTIVE
            }
            val severity = if (report.documentDetection.score < 0.5f) {
                WarningSeverity.ERROR
            } else WarningSeverity.WARNING
            warnings.add(PipelineWarning(type, severity, msg, "document"))
        }

        // Graph detection
        if (graphManual) {
            warnings.add(
                PipelineWarning(
                    WarningType.GRAPH_MANUAL_SELECTION,
                    WarningSeverity.INFO,
                    "Область графика выбрана вручную",
                    "graph",
                ),
            )
        }

        // OCR
        if (!ocrUsed) {
            warnings.add(
                PipelineWarning(
                    WarningType.OCR_NOT_USED,
                    WarningSeverity.INFO,
                    "OCR не использован — значения осей введены вручную",
                    "calibration",
                ),
            )
        }
        if (ocrCorrected) {
            warnings.add(
                PipelineWarning(
                    WarningType.OCR_CORRECTED,
                    WarningSeverity.INFO,
                    "OCR исправлен пользователем",
                    "calibration",
                ),
            )
        }

        // Axes
        if (axesManual) {
            warnings.add(
                PipelineWarning(
                    WarningType.AXES_MANUAL,
                    WarningSeverity.INFO,
                    "Оси заданы вручную",
                    "calibration",
                ),
            )
        }

        // Curve
        if (curveGapCount > 0) {
            warnings.add(
                PipelineWarning(
                    WarningType.CURVE_GAPS,
                    WarningSeverity.WARNING,
                    "Найдены разрывы кривой ($curveGapCount)",
                    "curve",
                ),
            )
        }
        if (interpolatedRatio > 0.1f) {
            warnings.add(
                PipelineWarning(
                    WarningType.CURVE_INTERPOLATED,
                    WarningSeverity.WARNING,
                    "Часть кривой интерполирована (${(interpolatedRatio * 100).toInt()}%)",
                    "curve",
                ),
            )
        }
        if (multipleLineCandidates) {
            warnings.add(
                PipelineWarning(
                    WarningType.MULTIPLE_LINE_CANDIDATES,
                    WarningSeverity.WARNING,
                    "Найдено несколько кандидатов линии",
                    "curve",
                ),
            )
        }

        // Overall quality
        if (report.overall.status == QualityStatus.RISKY ||
            report.overall.status == QualityStatus.FAILED
        ) {
            warnings.add(
                PipelineWarning(
                    WarningType.QUALITY_INSUFFICIENT,
                    WarningSeverity.ERROR,
                    "Качество оцифровки недостаточно для надёжного расчёта",
                    "overall",
                ),
            )
        }

        return warnings.sortedBy { it.severity.ordinal }.reversed()
    }
}

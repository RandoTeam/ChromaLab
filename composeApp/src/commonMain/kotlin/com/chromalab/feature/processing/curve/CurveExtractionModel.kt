package com.chromalab.feature.processing.curve

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * A single extracted curve point in pixel coordinates.
 */
@Serializable
data class CurvePoint(
    val pixelX: Int,
    val pixelY: Float,
    val confidence: Float,
) {
    companion object {
        const val HIGH_CONFIDENCE = 1.0f
        const val INTERPOLATED = 0.5f
        const val LOW_CONFIDENCE = 0.3f
    }
}

/**
 * Result of curve extraction.
 */
@Serializable
data class CurveExtractionResult(
    val points: List<CurvePoint>,
    val maskImagePath: String?,
    val totalColumns: Int,
    val extractedColumns: Int,
    val interpolatedColumns: Int,
    val outlierCount: Int,
    val centerlineAudit: CurveCenterlineAudit = CurveCenterlineAudit(),
    val warnings: List<String>,
    val timestamp: Long,
) {
    /** Fraction of columns that have extracted data */
    val coverage: Float
        get() = if (totalColumns > 0) extractedColumns.toFloat() / totalColumns else 0f

    /** Horizontal span covered by the detected trace evidence. */
    val xSpanCoverage: Float
        get() {
            if (totalColumns <= 0 || points.isEmpty()) return 0f
            val minX = points.minOf { it.pixelX }
            val maxX = points.maxOf { it.pixelX }
            return (maxX - minX + 1).toFloat() / totalColumns.toFloat()
        }

    /** Sparse XIC/ion traces can be valid even when the baseline is not continuously visible. */
    val isSparseTraceUsable: Boolean
        get() = points.size >= 24 && coverage >= 0.05f

    /** Sparse trace evidence concentrated in a narrow time span needs later confidence review. */
    val isLocalizedSparseTrace: Boolean
        get() = isSparseTraceUsable && xSpanCoverage < 0.25f

    /** Whether extraction produced usable data */
    val isUsable: Boolean get() = points.size >= 10 && (coverage > 0.3f || isSparseTraceUsable)
}

@Serializable
data class CurveCenterlineAudit(
    val available: Boolean = false,
    val method: String = "not_available",
    val selectionDecision: String = "not_available",
    val selectedForSignal: Boolean = false,
    val parityCompared: Boolean = false,
    val matchedColumnCount: Int = 0,
    val matchedColumnRatio: Float = 0f,
    val medianAbsDeltaPx: Float = 0f,
    val p95AbsDeltaPx: Float = 0f,
    val maxAbsDeltaPx: Float = 0f,
    val skeletonPixelCount: Int = 0,
    val skeletonColumnCount: Int = 0,
    val centerlineColumnCount: Int = 0,
    val skeletonPointCount: Int = 0,
    val fallbackPointCount: Int = 0,
    val wideClusterColumnCount: Int = 0,
    val branchColumnCount: Int = 0,
    val centerlineCoverage: Float = 0f,
    val skeletonCoverage: Float = 0f,
    val warnings: List<String> = emptyList(),
)

fun CurveExtractionResult.scaledCoordinates(scale: Float): CurveExtractionResult {
    if (scale <= 0f || scale == 1f) return this
    return copy(
        points = points.map { point ->
            point.copy(
                pixelX = (point.pixelX * scale).roundToInt(),
                pixelY = point.pixelY * scale,
            )
        },
        totalColumns = (totalColumns * scale).roundToInt(),
        extractedColumns = (extractedColumns * scale).roundToInt(),
        interpolatedColumns = (interpolatedColumns * scale).roundToInt(),
    )
}

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
    val warnings: List<String>,
    val timestamp: Long,
) {
    /** Fraction of columns that have extracted data */
    val coverage: Float
        get() = if (totalColumns > 0) extractedColumns.toFloat() / totalColumns else 0f

    /** Whether extraction produced usable data */
    val isUsable: Boolean get() = points.size >= 10 && coverage > 0.3f
}

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

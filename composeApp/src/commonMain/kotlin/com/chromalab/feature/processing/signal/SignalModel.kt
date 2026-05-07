package com.chromalab.feature.processing.signal

import kotlinx.serialization.Serializable

/**
 * A single point in the digitized chromatogram signal.
 * Contains both pixel and calibrated (real-world) coordinates.
 */
@Serializable
data class GraphPoint(
    val index: Int,
    val pixelX: Int,
    val pixelY: Float,
    val time: Float,
    val intensity: Float,
    val confidence: Float,
    val isInterpolated: Boolean,
)

/**
 * Digitized signal — the result of applying PixelCalibration to extracted curve points.
 */
@Serializable
data class DigitalSignal(
    val points: List<GraphPoint>,
    val timeUnit: String,
    val intensityUnit: String,
    val metadata: SignalMetadata,
) {
    /** Total time span of the signal */
    val timeRange: Float
        get() = if (points.size >= 2) points.last().time - points.first().time else 0f

    /** Maximum intensity value */
    val maxIntensity: Float
        get() = points.maxOfOrNull { it.intensity } ?: 0f

    /** Minimum intensity value */
    val minIntensity: Float
        get() = points.minOfOrNull { it.intensity } ?: 0f

    /** Number of interpolated points */
    val interpolatedCount: Int
        get() = points.count { it.isInterpolated }

    /** Fraction of high-confidence points */
    val highConfidenceRatio: Float
        get() = if (points.isNotEmpty()) {
            points.count { it.confidence >= 0.9f }.toFloat() / points.size
        } else 0f
}

/**
 * Signal metadata for traceability.
 */
@Serializable
data class SignalMetadata(
    val sourceImage: String,
    val totalPoints: Int,
    val duplicatesRemoved: Int,
    val gapCount: Int,
    val sortValid: Boolean,
    val timestamp: Long,
)

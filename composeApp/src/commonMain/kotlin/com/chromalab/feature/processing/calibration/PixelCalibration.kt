package com.chromalab.feature.processing.calibration

import kotlinx.serialization.Serializable

/**
 * Unified coordinate system calibration.
 * Combines X (pixelX → time) and Y (pixelY → intensity) transforms
 * into a single, serializable object for the downstream pipeline.
 *
 * Deterministic: same calibration points always produce the same transform.
 */
@Serializable
data class PixelCalibration(
    val xCalibration: LinearCalibration,
    val yCalibration: LinearCalibration,
    val xUnit: String,
    val yUnit: String,
    val originPixelX: Float,
    val originPixelY: Float,
    val timestamp: Long,
) {
    /** Convert pixel X to time value */
    fun pixelToTime(pixelX: Float): Float = xCalibration.pixelToReal(pixelX)

    /** Convert pixel Y to intensity value */
    fun pixelToIntensity(pixelY: Float): Float = yCalibration.pixelToReal(pixelY)

    /** Convert time value to pixel X */
    fun timeToPixel(time: Float): Float = xCalibration.realToPixel(time)

    /** Convert intensity value to pixel Y */
    fun intensityToPixel(intensity: Float): Float = yCalibration.realToPixel(intensity)

    /** Convert a pixel point to (time, intensity) */
    fun pixelToReal(pixelX: Float, pixelY: Float): Pair<Float, Float> =
        pixelToTime(pixelX) to pixelToIntensity(pixelY)

    /** Convert (time, intensity) to pixel point */
    fun realToPixel(time: Float, intensity: Float): Pair<Float, Float> =
        timeToPixel(time) to intensityToPixel(intensity)

    /** X axis scale: units per pixel */
    val xScale: Float get() = xCalibration.scale

    /** Y axis scale: units per pixel (negative for inverted Y) */
    val yScale: Float get() = yCalibration.scale

    /** Whether both axes have valid calibration */
    val isValid: Boolean get() = xCalibration.isValid && yCalibration.isValid

    companion object {
        /**
         * Build from X and Y axis calibration results.
         */
        fun from(
            xAxis: XAxisCalibration,
            yAxis: YAxisCalibration,
            originPixelX: Float,
            originPixelY: Float,
        ): PixelCalibration = PixelCalibration(
            xCalibration = xAxis.calibration,
            yCalibration = yAxis.calibration,
            xUnit = xAxis.unit,
            yUnit = yAxis.unit,
            originPixelX = originPixelX,
            originPixelY = originPixelY,
            timestamp = maxOf(xAxis.timestamp, yAxis.timestamp),
        )
    }
}

package com.chromalab.feature.processing.axis

import com.chromalab.feature.processing.pipeline.DetectionMethod
import kotlinx.serialization.Serializable

/**
 * A line defined by two endpoints in image coordinates.
 */
@Serializable
data class AxisLine(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
) {
    /** Length of the line segment */
    val length: Float
        get() {
            val dx = x2 - x1; val dy = y2 - y1
            return kotlin.math.sqrt(dx * dx + dy * dy)
        }

    /** Angle in degrees (0 = horizontal, 90 = vertical) */
    val angle: Float
        get() = kotlin.math.atan2(
            (y2 - y1).toDouble(),
            (x2 - x1).toDouble(),
        ).toFloat() * 180f / kotlin.math.PI.toFloat()

    /** Is approximately horizontal (within ±5°) */
    val isHorizontal: Boolean get() = kotlin.math.abs(angle) < 5f || kotlin.math.abs(angle) > 175f

    /** Is approximately vertical (within ±5° of 90°) */
    val isVertical: Boolean get() = kotlin.math.abs(kotlin.math.abs(angle) - 90f) < 5f
}

/**
 * Detected axes of the graph.
 */
@Serializable
data class AxesResult(
    val xAxis: AxisLine?,
    val yAxis: AxisLine?,
    val origin: AxisOrigin?,
    val detectionMethod: DetectionMethod,
    val confidence: Float,
    val warnings: List<String> = emptyList(),
    val timestamp: Long,
) {
    val hasAxes: Boolean get() = xAxis != null && yAxis != null
    val hasOrigin: Boolean get() = origin != null
}

/**
 * The intersection point of X and Y axes.
 */
@Serializable
data class AxisOrigin(
    val x: Float,
    val y: Float,
)

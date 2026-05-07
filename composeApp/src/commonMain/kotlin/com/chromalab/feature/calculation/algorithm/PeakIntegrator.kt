package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.SignalPoint

/**
 * Peak integration (§2.19).
 *
 * Trapezoidal integration of the baseline-corrected raw signal
 * between peak boundaries.
 *
 * Rules:
 * - Uses raw − baseline by default (NOT smoothed)
 * - Supports irregular time spacing
 * - Linear interpolation at start/end boundaries
 * - Does NOT clamp negative regions unless explicitly requested
 *
 * Pure function: deterministic, no side effects.
 */
object PeakIntegrator {

    /**
     * Integrate a peak between boundaries.
     *
     * @param points         Signal points (baseline-corrected)
     * @param leftIndex      Left boundary index (inclusive)
     * @param rightIndex     Right boundary index (inclusive)
     * @param clampNegative  If true, treat negative corrected values as 0
     * @return IntegrationResult with area breakdown
     */
    fun integrate(
        points: List<SignalPoint>,
        leftIndex: Int,
        rightIndex: Int,
        clampNegative: Boolean = false,
    ): IntegrationResult {
        val left = leftIndex.coerceIn(0, points.size - 1)
        val right = rightIndex.coerceIn(left, points.size - 1)

        if (left >= right) {
            return IntegrationResult.empty()
        }

        var totalArea = 0.0
        var positiveArea = 0.0
        var negativeArea = 0.0

        for (i in left until right) {
            val t0 = points[i].time
            val t1 = points[i + 1].time
            val dt = t1 - t0
            if (dt <= 0.0) continue // skip invalid/duplicate time points

            val y0 = if (clampNegative) maxOf(0.0, points[i].intensity) else points[i].intensity
            val y1 = if (clampNegative) maxOf(0.0, points[i + 1].intensity) else points[i + 1].intensity

            // Trapezoidal rule: 0.5 × (y_i + y_{i+1}) × dt
            val trapezoid = 0.5 * (y0 + y1) * dt
            totalArea += trapezoid

            if (trapezoid >= 0.0) {
                positiveArea += trapezoid
            } else {
                negativeArea += trapezoid
            }
        }

        // Time span
        val startTime = points[left].time
        val endTime = points[right].time

        return IntegrationResult(
            totalArea = totalArea,
            positiveArea = positiveArea,
            negativeArea = negativeArea,
            method = IntegrationMethod.TRAPEZOIDAL,
            startTime = startTime,
            endTime = endTime,
            pointCount = right - left + 1,
            clampedNegative = clampNegative,
        )
    }

    /**
     * Integrate with linear interpolation at exact boundary times.
     *
     * Used when boundaries don't fall exactly on sample points.
     *
     * @param points         Signal points (baseline-corrected)
     * @param leftTime       Exact left boundary time
     * @param rightTime      Exact right boundary time
     * @param clampNegative  If true, treat negative corrected values as 0
     * @return IntegrationResult with interpolated boundaries
     */
    fun integrateInterpolated(
        points: List<SignalPoint>,
        leftTime: Double,
        rightTime: Double,
        clampNegative: Boolean = false,
    ): IntegrationResult {
        if (points.size < 2 || leftTime >= rightTime) {
            return IntegrationResult.empty()
        }

        // Find the range of points within [leftTime, rightTime]
        val firstIdx = points.indexOfFirst { it.time >= leftTime }.coerceAtLeast(0)
        val lastIdx = points.indexOfLast { it.time <= rightTime }.coerceAtLeast(firstIdx)

        var totalArea = 0.0
        var positiveArea = 0.0
        var negativeArea = 0.0

        // Helper to add a trapezoid
        fun addTrapezoid(t0: Double, y0: Double, t1: Double, y1: Double) {
            val dt = t1 - t0
            if (dt <= 0.0) return
            val cy0 = if (clampNegative) maxOf(0.0, y0) else y0
            val cy1 = if (clampNegative) maxOf(0.0, y1) else y1
            val area = 0.5 * (cy0 + cy1) * dt
            totalArea += area
            if (area >= 0.0) positiveArea += area else negativeArea += area
        }

        // Left edge interpolation
        if (firstIdx > 0 && points[firstIdx].time > leftTime) {
            val p0 = points[firstIdx - 1]
            val p1 = points[firstIdx]
            val frac = (leftTime - p0.time) / (p1.time - p0.time)
            val yInterp = p0.intensity + frac * (p1.intensity - p0.intensity)
            addTrapezoid(leftTime, yInterp, p1.time, p1.intensity)
        }

        // Middle trapezoids
        for (i in firstIdx until lastIdx) {
            addTrapezoid(points[i].time, points[i].intensity, points[i + 1].time, points[i + 1].intensity)
        }

        // Right edge interpolation
        if (lastIdx < points.size - 1 && points[lastIdx].time < rightTime) {
            val p0 = points[lastIdx]
            val p1 = points[lastIdx + 1]
            val frac = (rightTime - p0.time) / (p1.time - p0.time)
            val yInterp = p0.intensity + frac * (p1.intensity - p0.intensity)
            addTrapezoid(p0.time, p0.intensity, rightTime, yInterp)
        }

        return IntegrationResult(
            totalArea = totalArea,
            positiveArea = positiveArea,
            negativeArea = negativeArea,
            method = IntegrationMethod.TRAPEZOIDAL_INTERPOLATED,
            startTime = leftTime,
            endTime = rightTime,
            pointCount = lastIdx - firstIdx + 1,
            clampedNegative = clampNegative,
        )
    }
}

// ─── Data classes ───────────────────────────────────────────────

enum class IntegrationMethod(val label: String) {
    TRAPEZOIDAL("Трапецеидальный"),
    TRAPEZOIDAL_INTERPOLATED("Трапецеидальный (интерполированный)"),
}

data class IntegrationResult(
    val totalArea: Double,
    val positiveArea: Double,
    val negativeArea: Double,
    val method: IntegrationMethod,
    val startTime: Double,
    val endTime: Double,
    val pointCount: Int,
    val clampedNegative: Boolean,
) {
    companion object {
        fun empty() = IntegrationResult(
            totalArea = 0.0,
            positiveArea = 0.0,
            negativeArea = 0.0,
            method = IntegrationMethod.TRAPEZOIDAL,
            startTime = 0.0,
            endTime = 0.0,
            pointCount = 0,
            clampedNegative = false,
        )
    }
}

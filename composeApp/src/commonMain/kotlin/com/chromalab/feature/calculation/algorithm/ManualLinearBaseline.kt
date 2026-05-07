package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.SignalPoint

/**
 * Manual linear baseline estimator (§2.10).
 *
 * Draws a straight line between leftBoundary and rightBoundary.
 * Outside the boundaries, baseline equals the signal value at the nearest boundary.
 *
 * This is the simplest, most transparent baseline — used as:
 * - Control method for validating automatic baselines
 * - Fallback when ALS/SNIP produce questionable results
 * - Per-peak baseline when user manually sets boundaries
 *
 * Pure function: deterministic, no side effects.
 */
class ManualLinearBaselineEstimator(
    private val leftBoundaryTime: Double? = null,
    private val rightBoundaryTime: Double? = null,
) : BaselineEstimator {

    override val method = BaselineMethod.MANUAL_LINEAR

    override fun estimate(points: List<SignalPoint>): BaselineResult {
        if (points.isEmpty()) {
            return BaselineResult(
                method = method.name,
                baseline = emptyList(),
                params = emptyMap(),
                warnings = listOf("Нет точек для оценки baseline"),
                quality = null,
            )
        }

        val times = points.map { it.time }
        val intensities = points.map { it.intensity }

        // Determine boundary indices
        val leftTime = leftBoundaryTime ?: times.first()
        val rightTime = rightBoundaryTime ?: times.last()

        val leftIdx = times.indexOfFirst { it >= leftTime }.coerceAtLeast(0)
        val rightIdx = times.indexOfLast { it <= rightTime }.coerceAtLeast(leftIdx)

        val leftIntensity = intensities[leftIdx]
        val rightIntensity = intensities[rightIdx]
        val leftT = times[leftIdx]
        val rightT = times[rightIdx]

        val warnings = mutableListOf<String>()

        // Build baseline: linear interpolation between boundaries
        val baseline = DoubleArray(points.size) { i ->
            val t = times[i]
            when {
                i <= leftIdx -> leftIntensity
                i >= rightIdx -> rightIntensity
                else -> {
                    // Linear interpolation
                    val fraction = if (rightT != leftT) {
                        (t - leftT) / (rightT - leftT)
                    } else 0.0
                    leftIntensity + fraction * (rightIntensity - leftIntensity)
                }
            }
        }

        // Check if baseline crosses signal (negative corrected values)
        val crossingCount = baseline.indices.count { i ->
            i in leftIdx..rightIdx && baseline[i] > intensities[i]
        }
        if (crossingCount > 0) {
            warnings.add(
                "Baseline пересекает сигнал в $crossingCount точках — " +
                "площадь может быть некорректной"
            )
        }

        // Compute quality
        val regionIndices = leftIdx..rightIdx
        val residuals = regionIndices.map { intensities[it] - baseline[it] }
        val residualStd = if (residuals.size > 1) {
            val mean = residuals.average()
            kotlin.math.sqrt(residuals.sumOf { (it - mean) * (it - mean) } / (residuals.size - 1))
        } else 0.0

        val negativeValues = residuals.filter { it < 0.0 }
        val quality = BaselineQuality(
            flatnessScore = 1.0, // linear baseline is perfectly "flat" in its model
            residualStd = residualStd,
            negativeCount = negativeValues.size,
            maxNegativeDepth = if (negativeValues.isNotEmpty()) {
                negativeValues.minOf { it } // most negative value
            } else 0.0,
        )

        return BaselineResult(
            method = method.name,
            baseline = baseline.toList(),
            params = mapOf(
                "leftBoundaryTime" to leftT,
                "rightBoundaryTime" to rightT,
                "leftIntensity" to leftIntensity,
                "rightIntensity" to rightIntensity,
            ),
            warnings = warnings,
            quality = quality,
        )
    }

    companion object {
        /**
         * Create estimator using first and last points (auto-boundaries).
         */
        fun auto(): ManualLinearBaselineEstimator = ManualLinearBaselineEstimator()

        /**
         * Create estimator with explicit boundary times (user-set).
         */
        fun withBoundaries(left: Double, right: Double): ManualLinearBaselineEstimator =
            ManualLinearBaselineEstimator(leftBoundaryTime = left, rightBoundaryTime = right)
    }
}

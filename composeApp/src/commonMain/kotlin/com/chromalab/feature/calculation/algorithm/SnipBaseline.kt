package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.SignalPoint
import kotlin.math.ln
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sqrt

/**
 * SNIP baseline estimator (§2.12).
 *
 * Statistics-sensitive Non-linear Iterative Peak-clipping.
 * Alternative / experimental baseline method.
 *
 * Algorithm:
 * 1. Apply LLS (Log-Log-Sqrt) transform to compress dynamic range
 * 2. Iteratively clip peaks by comparing each point with the average
 *    of its neighbors at increasing window widths
 * 3. Inverse-transform to get baseline estimate
 *
 * Advantages over ALS:
 * - Fewer tunable parameters (just iterations)
 * - Better for non-linear / curved baselines
 * - No matrix inversion required
 *
 * Pure function: deterministic, no side effects.
 */
class SnipBaselineEstimator(
    private val iterations: Int = 40,
    private val useLlsTransform: Boolean = true,
) : BaselineEstimator {

    override val method = BaselineMethod.SNIP

    override fun estimate(points: List<SignalPoint>): BaselineResult {
        if (points.isEmpty()) {
            return emptyResult("Нет точек для SNIP")
        }

        val n = points.size
        if (n < 5) {
            return emptyResult("Слишком мало точек ($n) для SNIP — нужно минимум 5")
        }

        val y = points.map { it.intensity }.toDoubleArray()
        val warnings = mutableListOf<String>()

        // Step 1: LLS transform (optional, improves peak clipping)
        val transformed = if (useLlsTransform) {
            llsTransform(y)
        } else {
            y.copyOf()
        }

        // Step 2: Iterative peak clipping
        var working = transformed.copyOf()
        val effectiveIterations = min(iterations, n / 2)

        for (p in effectiveIterations downTo 1) {
            val updated = working.copyOf()
            for (i in p until n - p) {
                val avg = (working[i - p] + working[i + p]) / 2.0
                updated[i] = min(working[i], avg)
            }
            working = updated
        }

        // Step 3: Inverse LLS transform
        val baseline = if (useLlsTransform) {
            llsInverse(working)
        } else {
            working
        }

        // Warnings
        // Check for wide peaks that SNIP might not clip fully
        val aboveCount = (0 until n).count { baseline[it] > y[it] * 1.01 }
        if (aboveCount > 0) {
            warnings.add(
                "SNIP baseline выше сигнала в $aboveCount точках — " +
                "попробуйте увеличить количество итераций"
            )
        }

        // Check area difference hint (stored for cross-method comparison)
        val originalArea = y.sum()
        val correctedArea = (0 until n).sumOf { maxOf(0.0, y[it] - baseline[it]) }
        if (originalArea > 0 && correctedArea / originalArea < 0.2) {
            warnings.add(
                "SNIP baseline убирает >80% площади — " +
                "результат может быть ненадёжным для широких пиков"
            )
        }

        // Quality
        val residuals = (0 until n).map { y[it] - baseline[it] }
        val resMean = residuals.average()
        val resStd = if (n > 1) {
            sqrt(residuals.sumOf { (it - resMean) * (it - resMean) } / (n - 1))
        } else 0.0
        val negativeResiduals = residuals.filter { it < 0.0 }

        val quality = BaselineQuality(
            flatnessScore = 0.8, // SNIP follows signal shape — less "flat" by design
            residualStd = resStd,
            negativeCount = negativeResiduals.size,
            maxNegativeDepth = negativeResiduals.minOrNull() ?: 0.0,
        )

        return BaselineResult(
            method = method.name,
            baseline = baseline.toList(),
            params = mapOf(
                "iterations" to effectiveIterations.toDouble(),
                "useLlsTransform" to if (useLlsTransform) 1.0 else 0.0,
            ),
            warnings = warnings,
            quality = quality,
        )
    }

    /**
     * LLS (Log-Log-Sqrt) transform to compress dynamic range.
     * v = ln(ln(sqrt(y + 1) + 1) + 1)
     */
    private fun llsTransform(y: DoubleArray): DoubleArray {
        return DoubleArray(y.size) { i ->
            val v = maxOf(y[i], 0.0)
            ln(ln(sqrt(v + 1.0) + 1.0) + 1.0)
        }
    }

    /**
     * Inverse LLS transform.
     * y = (exp(exp(v) - 1) - 1)² - 1
     */
    private fun llsInverse(v: DoubleArray): DoubleArray {
        return DoubleArray(v.size) { i ->
            val a = exp(v[i]) - 1.0
            val b = exp(a) - 1.0
            val c = b * b - 1.0
            maxOf(c, 0.0)
        }
    }

    private fun emptyResult(warning: String) = BaselineResult(
        method = method.name,
        baseline = emptyList(),
        params = mapOf("iterations" to iterations.toDouble()),
        warnings = listOf(warning),
        quality = null,
    )

    companion object {
        /** Default SNIP with LLS transform */
        fun default() = SnipBaselineEstimator(iterations = 40, useLlsTransform = true)
        /** Fast SNIP — fewer iterations */
        fun fast() = SnipBaselineEstimator(iterations = 20, useLlsTransform = true)
        /** Raw SNIP without LLS transform */
        fun raw() = SnipBaselineEstimator(iterations = 40, useLlsTransform = false)
    }
}

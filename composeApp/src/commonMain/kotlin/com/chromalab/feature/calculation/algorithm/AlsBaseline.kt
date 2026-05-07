package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.SignalPoint
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Asymmetric Least Squares (ALS) baseline estimator (§2.11).
 *
 * Primary automatic baseline method.
 *
 * Model: minimize  Σ wᵢ(yᵢ - zᵢ)² + λ Σ (Δ²zᵢ)²
 *
 * Where:
 * - y = signal intensities
 * - z = estimated baseline
 * - w = asymmetric weights (p for signal below baseline, 1-p above)
 * - λ = smoothness parameter (higher = smoother baseline)
 * - Δ² = second-order finite difference operator
 *
 * Uses Whittaker smoother solved via Cholesky-banded decomposition.
 * Pure function: deterministic, no side effects.
 */
class AlsBaselineEstimator(
    private val lambda: Double = 1e6,
    private val p: Double = 0.01,
    private val maxIterations: Int = 10,
) : BaselineEstimator {

    override val method = BaselineMethod.ALS

    override fun estimate(points: List<SignalPoint>): BaselineResult {
        if (points.isEmpty()) {
            return emptyResult("Нет точек для ALS")
        }

        val n = points.size
        if (n < 5) {
            return emptyResult("Слишком мало точек ($n) для ALS — нужно минимум 5")
        }

        val y = points.map { it.intensity }.toDoubleArray()
        val w = DoubleArray(n) { 1.0 }

        // Iterative reweighted penalized least squares
        var z = y.copyOf()
        var converged = false
        var lastIter = 0

        for (iter in 0 until maxIterations) {
            lastIter = iter + 1
            z = solveWhittaker(y, w, lambda, n)

            // Update asymmetric weights
            var maxChange = 0.0
            for (i in 0 until n) {
                val newW = if (y[i] > z[i]) p else (1.0 - p)
                maxChange = maxOf(maxChange, abs(newW - w[i]))
                w[i] = newW
            }

            // Check convergence
            if (maxChange < 1e-6) {
                converged = true
                break
            }
        }

        // Build warnings
        val warnings = mutableListOf<String>()

        // Check if baseline above signal on large stretches
        val aboveCount = (0 until n).count { z[it] > y[it] }
        val aboveRatio = aboveCount.toDouble() / n
        if (aboveRatio > 0.2) {
            warnings.add(
                "Baseline выше сигнала в ${(aboveRatio * 100).toInt()}% точек — " +
                "попробуйте увеличить λ или уменьшить p"
            )
        }

        // Check if baseline is too aggressive (changes area dramatically)
        val originalArea = y.sum()
        val correctedArea = (0 until n).sumOf { maxOf(0.0, y[it] - z[it]) }
        if (originalArea > 0 && correctedArea / originalArea < 0.3) {
            warnings.add(
                "Baseline убирает >70% площади сигнала — " +
                "baseline может быть слишком агрессивной"
            )
        }

        if (!converged) {
            warnings.add("ALS не сошёлся за $maxIterations итераций")
        }

        // Quality metrics
        val residuals = (0 until n).map { y[it] - z[it] }
        val resMean = residuals.average()
        val resStd = if (n > 1) {
            sqrt(residuals.sumOf { (it - resMean) * (it - resMean) } / (n - 1))
        } else 0.0

        val negativeResiduals = residuals.filter { it < 0.0 }
        val quality = BaselineQuality(
            flatnessScore = if (converged) 1.0 - (lastIter.toDouble() / maxIterations) else 0.0,
            residualStd = resStd,
            negativeCount = negativeResiduals.size,
            maxNegativeDepth = negativeResiduals.minOrNull() ?: 0.0,
        )

        return BaselineResult(
            method = method.name,
            baseline = z.toList(),
            params = mapOf(
                "lambda" to lambda,
                "p" to p,
                "iterations" to lastIter.toDouble(),
                "converged" to if (converged) 1.0 else 0.0,
            ),
            warnings = warnings,
            quality = quality,
        )
    }

    /**
     * Solve the Whittaker smoother: (W + λ D'D) z = W y
     *
     * Uses tridiagonal-banded Cholesky factorization for O(n) time.
     * D = second-order finite difference matrix.
     */
    private fun solveWhittaker(y: DoubleArray, w: DoubleArray, lam: Double, n: Int): DoubleArray {
        // Build pentadiagonal system from W + λ D₂'D₂
        // D₂ is (n-2)×n second-difference matrix
        // D₂'D₂ is pentadiagonal with known structure

        // Diagonals of D₂'D₂:
        // main:    [1, 5, 6, 6, ..., 6, 5, 1]
        // off-1:   [-2, -4, -4, ..., -4, -2]
        // off-2:   [1, 1, ..., 1]

        val a = DoubleArray(n) // main diagonal
        val b = DoubleArray(n) // off-1 (upper)
        val c = DoubleArray(n) // off-2 (upper)

        // Fill D₂'D₂ structure
        for (i in 0 until n) {
            var d2sum = 0.0
            // D₂'D₂[i,i] contributions
            if (i >= 2) d2sum += 1.0
            if (i >= 1 && i <= n - 2) d2sum += 4.0
            if (i <= n - 3) d2sum += 1.0
            // Special cases at boundaries
            d2sum = when (i) {
                0 -> 1.0
                1 -> 5.0
                n - 2 -> 5.0
                n - 1 -> 1.0
                else -> 6.0
            }
            a[i] = w[i] + lam * d2sum
        }

        for (i in 0 until n - 1) {
            val d2off1 = when {
                i == 0 -> -2.0
                i == n - 2 -> -2.0
                else -> -4.0
            }
            b[i] = lam * d2off1
        }

        for (i in 0 until n - 2) {
            c[i] = lam * 1.0
        }

        // Right-hand side: W * y
        val rhs = DoubleArray(n) { w[it] * y[it] }

        // Solve pentadiagonal system using LDL' decomposition
        return solvePentadiagonal(a, b, c, rhs, n)
    }

    /**
     * Solve a symmetric pentadiagonal system Ax = rhs.
     * a = main diagonal, b = first superdiagonal, c = second superdiagonal.
     */
    private fun solvePentadiagonal(
        a: DoubleArray,
        b: DoubleArray,
        c: DoubleArray,
        rhs: DoubleArray,
        n: Int,
    ): DoubleArray {
        // Forward elimination (band Cholesky)
        val l1 = DoubleArray(n)  // multiplier for off-1
        val l2 = DoubleArray(n)  // multiplier for off-2
        val d = a.copyOf()       // diagonal after elimination
        val r = rhs.copyOf()     // RHS after elimination

        for (i in 0 until n) {
            if (i >= 2) {
                d[i] -= l2[i - 2] * c[i - 2]
                r[i] -= l2[i - 2] * r[i - 2]
            }
            if (i >= 1) {
                val bPrev = if (i >= 2) {
                    b[i - 1] - l2[i - 2] * c[i - 2]
                } else {
                    b[i - 1]
                }
                l1[i - 1] = bPrev / d[i - 1]
                d[i] -= l1[i - 1] * bPrev
                r[i] -= l1[i - 1] * r[i - 1]
            }
            if (i < n - 2) {
                l2[i] = c[i] / d[i]
            }
        }

        // Back substitution
        val x = DoubleArray(n)
        for (i in n - 1 downTo 0) {
            x[i] = r[i] / d[i]
            if (i < n - 1) {
                x[i] -= l1[i] * x[i + 1]
            }
            if (i < n - 2) {
                x[i] -= l2[i] * x[i + 2]
            }
        }

        return x
    }

    private fun emptyResult(warning: String) = BaselineResult(
        method = method.name,
        baseline = emptyList(),
        params = mapOf("lambda" to lambda, "p" to p),
        warnings = listOf(warning),
        quality = null,
    )

    companion object {
        /** Soft: preserves more signal shape, less aggressive correction */
        fun soft() = AlsBaselineEstimator(lambda = 1e4, p = 0.05)
        /** Medium: balanced baseline correction (default) */
        fun medium() = AlsBaselineEstimator(lambda = 1e6, p = 0.01)
        /** Stiff: very smooth baseline, aggressive peak separation */
        fun stiff() = AlsBaselineEstimator(lambda = 1e8, p = 0.001)
    }
}

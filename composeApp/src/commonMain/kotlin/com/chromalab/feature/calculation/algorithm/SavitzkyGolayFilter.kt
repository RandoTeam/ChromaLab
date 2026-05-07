package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.SignalPoint

/**
 * Savitzky-Golay smoothing filter (§2.8).
 *
 * Pure function: no side effects, deterministic.
 *
 * Fits a polynomial of given order to a window of points using least-squares,
 * then evaluates the polynomial at the center point. This preserves peak shape
 * better than simple moving average while reducing high-frequency noise.
 *
 * Edge handling: mirrored extension (reflect points at boundaries).
 */
object SavitzkyGolayFilter {

    /**
     * Apply Savitzky-Golay smoothing to signal points.
     *
     * @param points            Input signal points (sorted by time)
     * @param windowSize        Must be odd and >= 3
     * @param polynomialOrder   Must be < windowSize, typically 2 or 3
     * @return Smoothed signal points (new list, originals unchanged)
     * @throws IllegalArgumentException if parameters are invalid
     */
    fun smooth(
        points: List<SignalPoint>,
        windowSize: Int = 7,
        polynomialOrder: Int = 2,
    ): List<SignalPoint> {
        // Validate parameters
        require(windowSize >= 3) { "windowSize must be >= 3, got $windowSize" }
        require(windowSize % 2 == 1) { "windowSize must be odd, got $windowSize" }
        require(polynomialOrder >= 0) { "polynomialOrder must be >= 0, got $polynomialOrder" }
        require(polynomialOrder < windowSize) {
            "polynomialOrder ($polynomialOrder) must be < windowSize ($windowSize)"
        }
        require(points.size >= windowSize) {
            "Not enough points (${points.size}) for windowSize $windowSize"
        }

        val coefficients = computeCoefficients(windowSize, polynomialOrder)
        val halfWindow = windowSize / 2
        val intensities = points.map { it.intensity }
        val n = intensities.size

        // Apply convolution with mirrored edge handling
        val smoothed = DoubleArray(n)
        for (i in intensities.indices) {
            var sum = 0.0
            for (j in -halfWindow..halfWindow) {
                val idx = mirrorIndex(i + j, n)
                sum += coefficients[j + halfWindow] * intensities[idx]
            }
            smoothed[i] = sum
        }

        return points.mapIndexed { i, p ->
            p.copy(intensity = smoothed[i])
        }
    }

    /**
     * Compute Savitzky-Golay convolution coefficients.
     *
     * Uses the analytical approach: solve (J^T J) c = J^T e_0
     * where J is the Vandermonde matrix and e_0 selects the 0th derivative.
     */
    private fun computeCoefficients(windowSize: Int, order: Int): DoubleArray {
        val halfWindow = windowSize / 2
        val m = windowSize
        val k = order + 1 // number of polynomial terms

        // Build Vandermonde matrix J[i][j] = x_i^j where x_i = i - halfWindow
        val j = Array(m) { row ->
            DoubleArray(k) { col ->
                val x = (row - halfWindow).toDouble()
                power(x, col)
            }
        }

        // Compute J^T * J
        val jtj = Array(k) { r -> DoubleArray(k) { c ->
            var sum = 0.0
            for (i in 0 until m) sum += j[i][r] * j[i][c]
            sum
        }}

        // Compute J^T * e (where e is the column selecting derivative 0 = identity at center)
        // For smoothing (0th derivative), we need the row of (J^T J)^-1 * J^T corresponding to c_0
        // This simplifies to solving the normal equations for each data point

        // Use Gauss-Jordan elimination to invert J^T J
        val inv = invertMatrix(jtj, k)

        // Coefficients = first row of (J^T J)^-1 * J^T
        val coefficients = DoubleArray(m)
        for (i in 0 until m) {
            var sum = 0.0
            for (p in 0 until k) {
                sum += inv[0][p] * j[i][p]
            }
            coefficients[i] = sum
        }

        return coefficients
    }

    /**
     * Mirror index at boundaries for edge handling.
     * Example: for n=10, index -1 → 1, index 10 → 8
     */
    private fun mirrorIndex(index: Int, size: Int): Int {
        return when {
            index < 0 -> -index
            index >= size -> 2 * (size - 1) - index
            else -> index
        }.coerceIn(0, size - 1)
    }

    /**
     * Gauss-Jordan matrix inversion.
     */
    private fun invertMatrix(matrix: Array<DoubleArray>, n: Int): Array<DoubleArray> {
        // Augmented matrix [A | I]
        val aug = Array(n) { r ->
            DoubleArray(2 * n) { c ->
                if (c < n) matrix[r][c] else if (c - n == r) 1.0 else 0.0
            }
        }

        for (col in 0 until n) {
            // Partial pivoting
            var maxRow = col
            for (row in col + 1 until n) {
                if (kotlin.math.abs(aug[row][col]) > kotlin.math.abs(aug[maxRow][col])) {
                    maxRow = row
                }
            }
            val temp = aug[col]
            aug[col] = aug[maxRow]
            aug[maxRow] = temp

            // Scale pivot row
            val pivot = aug[col][col]
            if (kotlin.math.abs(pivot) < 1e-15) {
                // Singular — return identity (shouldn't happen with valid params)
                return Array(n) { r -> DoubleArray(n) { c -> if (r == c) 1.0 else 0.0 } }
            }
            for (c in 0 until 2 * n) {
                aug[col][c] /= pivot
            }

            // Eliminate column
            for (row in 0 until n) {
                if (row != col) {
                    val factor = aug[row][col]
                    for (c in 0 until 2 * n) {
                        aug[row][c] -= factor * aug[col][c]
                    }
                }
            }
        }

        // Extract inverse from right half
        return Array(n) { r -> DoubleArray(n) { c -> aug[r][c + n] } }
    }

    private fun power(base: Double, exp: Int): Double {
        var result = 1.0
        repeat(exp) { result *= base }
        return result
    }
}

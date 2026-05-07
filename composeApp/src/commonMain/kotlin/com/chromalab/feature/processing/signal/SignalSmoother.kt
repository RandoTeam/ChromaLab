package com.chromalab.feature.processing.signal

import kotlinx.serialization.Serializable

/**
 * Savitzky-Golay smoothing parameters.
 */
@Serializable
data class SmoothingParams(
    /** Window size (must be odd, >= 3) */
    val windowSize: Int = 7,
    /** Polynomial order (must be < windowSize) */
    val polyOrder: Int = 2,
) {
    val isValid: Boolean
        get() = windowSize >= 3 && windowSize % 2 == 1 && polyOrder < windowSize && polyOrder >= 0
}

/**
 * Result of signal smoothing.
 * Raw signal is NEVER modified — smoothed is a separate copy.
 */
@Serializable
data class SmoothedSignal(
    val raw: DigitalSignal,
    val smoothed: DigitalSignal,
    val params: SmoothingParams,
    val enabled: Boolean = true,
)

/**
 * Savitzky-Golay signal smoother.
 *
 * Pure Kotlin implementation — no external dependencies.
 * Preserves peak positions and shapes better than moving average.
 *
 * Implementation: convolves signal with polynomial-fit coefficients.
 * For simplicity, uses quadratic fit (order 2) with configurable window.
 */
object SignalSmoother {

    /**
     * Apply Savitzky-Golay smoothing to a digital signal.
     * Returns SmoothedSignal containing both raw and smoothed copies.
     * Raw signal is NEVER modified.
     */
    fun smooth(signal: DigitalSignal, params: SmoothingParams = SmoothingParams()): SmoothedSignal {
        if (!params.isValid || signal.points.size < params.windowSize) {
            return SmoothedSignal(raw = signal, smoothed = signal, params = params)
        }

        val coefficients = computeCoefficients(params.windowSize, params.polyOrder)
        val intensities = signal.points.map { it.intensity }.toFloatArray()
        val smoothed = convolve(intensities, coefficients)

        val smoothedPoints = signal.points.mapIndexed { i, gp ->
            gp.copy(intensity = smoothed[i])
        }

        val smoothedSignal = signal.copy(points = smoothedPoints)

        return SmoothedSignal(
            raw = signal,
            smoothed = smoothedSignal,
            params = params,
        )
    }

    /**
     * Compute Savitzky-Golay convolution coefficients.
     *
     * For quadratic fit (order 2), coefficients are:
     * c_i = (3m(m+1) - 1 - 5i²) / ((2m+1)(m(m+1)(2m-1)/3 + m(m+1)))
     *
     * Simplified: uses the classic SG coefficient formula for polynomial order 2.
     */
    internal fun computeCoefficients(windowSize: Int, polyOrder: Int): FloatArray {
        val m = windowSize / 2
        val n = windowSize

        return when (polyOrder) {
            0 -> {
                // Simple moving average
                FloatArray(n) { 1f / n }
            }
            1, 2 -> {
                // Quadratic/cubic SG coefficients (classic formula)
                val coeffs = FloatArray(n)
                val norm = (n * (n * n - 1)) / 12f

                for (i in -m..m) {
                    val idx = i + m
                    coeffs[idx] = ((3f * m * (m + 1) - 1f - 5f * i * i) /
                        ((2 * m + 1f) * (m * (m + 1f) / 3f)))
                }

                // Normalize
                val sum = coeffs.sum()
                if (sum != 0f) {
                    for (i in coeffs.indices) coeffs[i] /= sum
                }
                coeffs
            }
            else -> {
                // Fallback to moving average for higher orders
                FloatArray(n) { 1f / n }
            }
        }
    }

    /**
     * Convolve signal with coefficients.
     * Edges are handled by mirror-padding.
     */
    private fun convolve(data: FloatArray, coeffs: FloatArray): FloatArray {
        val n = data.size
        val m = coeffs.size / 2
        val result = FloatArray(n)

        for (i in data.indices) {
            var sum = 0f
            for (j in coeffs.indices) {
                val dataIdx = i + j - m
                // Mirror boundary
                val clampedIdx = when {
                    dataIdx < 0 -> -dataIdx
                    dataIdx >= n -> 2 * n - dataIdx - 2
                    else -> dataIdx
                }.coerceIn(0, n - 1)
                sum += data[clampedIdx] * coeffs[j]
            }
            result[i] = sum
        }

        return result
    }
}

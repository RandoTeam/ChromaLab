package com.chromalab.feature.processing.signal

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SignalSmootherTest {

    private val epsilon = 0.1f

    private fun makeSignal(intensities: List<Float>): DigitalSignal {
        val points = intensities.mapIndexed { i, v ->
            GraphPoint(
                index = i,
                pixelX = i * 10,
                pixelY = 0f,
                time = i.toFloat(),
                intensity = v,
                confidence = 1f,
                isInterpolated = false,
            )
        }
        return DigitalSignal(
            points = points,
            timeUnit = "мин",
            intensityUnit = "mAU",
            metadata = SignalMetadata("test", points.size, 0, 0, true, 0L),
        )
    }

    @Test
    fun smooth_rawSignalUnmodified() {
        val signal = makeSignal(listOf(1f, 10f, 1f, 10f, 1f, 10f, 1f, 10f, 1f))
        val result = SignalSmoother.smooth(signal, SmoothingParams(5, 2))

        // Raw must be exactly the same object data
        assertEquals(signal.points.map { it.intensity }, result.raw.points.map { it.intensity })
    }

    @Test
    fun smooth_reducesNoise() {
        // Noisy signal around baseline 100
        val noisy = listOf(100f, 105f, 95f, 110f, 90f, 105f, 95f, 100f, 98f)
        val signal = makeSignal(noisy)
        val result = SignalSmoother.smooth(signal, SmoothingParams(5, 2))

        // Smoothed should have less variance
        val rawVariance = variance(noisy)
        val smoothedVariance = variance(result.smoothed.points.map { it.intensity })
        assertTrue(
            smoothedVariance < rawVariance,
            "Smoothed variance ($smoothedVariance) should be < raw ($rawVariance)",
        )
    }

    @Test
    fun smooth_preservesPeak() {
        // Signal with a clear peak
        val peaked = listOf(0f, 0f, 0f, 0f, 100f, 0f, 0f, 0f, 0f)
        val signal = makeSignal(peaked)
        val result = SignalSmoother.smooth(signal, SmoothingParams(5, 2))

        // Peak should still be at index 4 (center)
        val peakIdx = result.smoothed.points.maxByOrNull { it.intensity }!!.index
        assertEquals(4, peakIdx, "Peak should remain at index 4")
    }

    @Test
    fun smooth_constantSignalUnchanged() {
        val constant = List(9) { 50f }
        val signal = makeSignal(constant)
        val result = SignalSmoother.smooth(signal, SmoothingParams(5, 2))

        result.smoothed.points.forEach { gp ->
            assertNear(50f, gp.intensity, "Constant signal should be unchanged")
        }
    }

    @Test
    fun smooth_invalidParams_returnsRawAsBoth() {
        val signal = makeSignal(listOf(1f, 2f, 3f))
        val result = SignalSmoother.smooth(signal, SmoothingParams(4, 2)) // Even window = invalid

        assertEquals(signal.points, result.smoothed.points)
    }

    @Test
    fun smooth_tooFewPoints_returnsRawAsBoth() {
        val signal = makeSignal(listOf(1f, 2f, 3f))
        val result = SignalSmoother.smooth(signal, SmoothingParams(5, 2)) // Need >=5 points

        assertEquals(signal.points, result.smoothed.points)
    }

    @Test
    fun params_validation() {
        assertTrue(SmoothingParams(5, 2).isValid)
        assertTrue(SmoothingParams(7, 2).isValid)
        assertTrue(SmoothingParams(3, 0).isValid)
        assertTrue(!SmoothingParams(4, 2).isValid) // Even
        assertTrue(!SmoothingParams(2, 2).isValid) // Too small
        assertTrue(!SmoothingParams(5, 5).isValid) // Order >= window
    }

    @Test
    fun coefficients_sumToOne() {
        val coeffs = SignalSmoother.computeCoefficients(7, 2)
        val sum = coeffs.sum()
        assertNear(1f, sum, "Coefficients should sum to 1.0")
    }

    private fun variance(values: List<Float>): Float {
        val mean = values.average().toFloat()
        return values.map { (it - mean) * (it - mean) }.average().toFloat()
    }

    private fun assertNear(expected: Float, actual: Float, msg: String = "") {
        assertTrue(abs(expected - actual) < epsilon, "$msg Expected $expected, got $actual")
    }
}

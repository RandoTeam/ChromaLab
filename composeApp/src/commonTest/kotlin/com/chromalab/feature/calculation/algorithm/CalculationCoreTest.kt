package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.SignalPoint
import com.chromalab.feature.calculation.core.SignalValidator
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.signal.GraphPoint
import com.chromalab.feature.processing.signal.SignalMetadata
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CalculationCoreTest {

    @Test
    fun signalValidatorSortsAndAuditsInvalidInput() {
        val signal = digitalSignal(
            listOf(
                graphPoint(index = 0, time = 2.0, intensity = 10.0),
                graphPoint(index = 1, time = 1.0, intensity = -3.0),
                graphPoint(index = 2, time = 1.0, intensity = 4.0),
            ),
        )

        val (validated, report) = SignalValidator.validate(signal, sourceId = "validator-test")

        assertEquals(listOf(1.0, 1.0, 2.0), validated.points.map { it.time })
        assertEquals(false, report.isSorted)
        assertEquals(1, report.duplicateTimeCount)
        assertEquals(1, report.negativeIntensityCount)
        assertEquals(false, report.isValid)
        assertTrue(report.warnings.isNotEmpty())
    }

    @Test
    fun savitzkyGolaySmoothKeepsShapeAndDoesNotMutateInput() {
        val points = pointsFromValues(listOf(0.0, 0.0, 12.0, 0.0, 0.0, 0.0, 0.0))
        val original = points.map { it.intensity }

        val smoothed = SavitzkyGolayFilter.smooth(points, windowSize = 5, polynomialOrder = 2)

        assertEquals(points.size, smoothed.size)
        assertEquals(original, points.map { it.intensity })
        assertEquals(points.map { it.time }, smoothed.map { it.time })
        assertTrue(smoothed[2].intensity < points[2].intensity)
        assertTrue(smoothed.any { it.intensity > 0.0 })
    }

    @Test
    fun baselineEstimatorsUseCurrentSignalPointApi() {
        val points = (0..49).map { index ->
            val time = index.toDouble()
            signalPoint(index, time, 15.0 + time * 0.2 + gaussian(time, center = 25.0, height = 80.0, sigma = 2.0))
        }

        val manual = ManualLinearBaselineEstimator.auto().estimate(points)
        val als = AlsBaselineEstimator(lambda = 1e4, p = 0.01, maxIterations = 5).estimate(points)
        val snip = SnipBaselineEstimator(iterations = 10, useLlsTransform = true).estimate(points)

        assertEquals(points.size, manual.baseline.size)
        assertEquals(points.first().intensity, manual.baseline.first(), 1e-9)
        assertEquals(points.last().intensity, manual.baseline.last(), 1e-9)
        assertEquals(points.size, als.baseline.size)
        assertEquals(points.size, snip.baseline.size)
        assertEquals(BaselineMethod.MANUAL_LINEAR.name, manual.method)
        assertEquals(BaselineMethod.ALS.name, als.method)
        assertEquals(BaselineMethod.SNIP.name, snip.method)
    }

    @Test
    fun peakDetectorBoundariesAndIntegrationUseCurrentApi() {
        val points = (0..100).map { index ->
            val time = index / 10.0
            signalPoint(index, time, gaussian(time, center = 5.0, height = 100.0, sigma = 0.35))
        }

        val detection = PeakDetector.detect(
            points = points,
            minHeight = 20.0,
            minProminence = 20.0,
            minDistance = 5,
            minWidth = 3,
            noiseLevel = 0.0,
        )

        val peak = detection.accepted.single()
        val boundary = PeakBoundaryDetector.detect(
            points = points,
            peakIndex = peak.index,
            method = BoundaryMethod.PERCENT_HEIGHT,
            percentHeight = 0.01,
        )
        val integration = PeakIntegrator.integrate(points, boundary.leftIndex, boundary.rightIndex)

        assertEquals(5.0, peak.apexTime, 1e-9)
        assertTrue(boundary.leftIndex < peak.index)
        assertTrue(boundary.rightIndex > peak.index)
        assertEquals(IntegrationMethod.TRAPEZOIDAL, integration.method)
        assertTrue(integration.totalArea > 80.0)
        assertTrue(integration.negativeArea >= 0.0)
    }

    @Test
    fun peakIntegratorCanClampNegativeArea() {
        val points = pointsFromValues(listOf(0.0, 10.0, -10.0, -10.0, 10.0, 0.0))

        val raw = PeakIntegrator.integrate(points, 0, points.lastIndex, clampNegative = false)
        val clamped = PeakIntegrator.integrate(points, 0, points.lastIndex, clampNegative = true)

        assertTrue(raw.negativeArea < 0.0)
        assertEquals(true, clamped.clampedNegative)
        assertEquals(0.0, clamped.negativeArea, 1e-9)
        assertTrue(clamped.totalArea > raw.totalArea)
        assertNotEquals(raw.totalArea, clamped.totalArea)
    }

    private fun pointsFromValues(values: List<Double>): List<SignalPoint> =
        values.mapIndexed { index, intensity ->
            signalPoint(index, time = index.toDouble(), intensity = intensity)
        }

    private fun signalPoint(index: Int, time: Double, intensity: Double): SignalPoint =
        SignalPoint(
            index = index,
            time = time,
            intensity = intensity,
            pixelX = index,
            pixelY = intensity,
            confidence = 1.0,
            isInterpolated = false,
        )

    private fun digitalSignal(points: List<GraphPoint>): DigitalSignal =
        DigitalSignal(
            points = points,
            timeUnit = "min",
            intensityUnit = "counts",
            metadata = SignalMetadata(
                sourceImage = "test",
                totalPoints = points.size,
                duplicatesRemoved = 0,
                gapCount = 0,
                sortValid = true,
                timestamp = 0L,
            ),
        )

    private fun graphPoint(index: Int, time: Double, intensity: Double): GraphPoint =
        GraphPoint(
            index = index,
            pixelX = index,
            pixelY = intensity.toFloat(),
            time = time.toFloat(),
            intensity = intensity.toFloat(),
            confidence = 1f,
            isInterpolated = false,
        )

    private fun gaussian(time: Double, center: Double, height: Double, sigma: Double): Double =
        height * exp(-0.5 * ((time - center) / sigma) * ((time - center) / sigma))
}

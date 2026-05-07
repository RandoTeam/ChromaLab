package com.chromalab.feature.processing.signal

import com.chromalab.feature.processing.calibration.CalibrationPoint
import com.chromalab.feature.processing.calibration.LinearCalibration
import com.chromalab.feature.processing.calibration.PixelCalibration
import com.chromalab.feature.processing.curve.CurvePoint
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignalConverterTest {

    private val epsilon = 0.01f

    private fun testCalibration(): PixelCalibration {
        return PixelCalibration(
            xCalibration = LinearCalibration(
                CalibrationPoint(0f, 0f),   // pixel 0 → time 0
                CalibrationPoint(1000f, 100f), // pixel 1000 → time 100
            ),
            yCalibration = LinearCalibration(
                CalibrationPoint(500f, 0f),   // pixel 500 (bottom) → intensity 0
                CalibrationPoint(0f, 500f),   // pixel 0 (top) → intensity 500
            ),
            xUnit = "мин",
            yUnit = "mAU",
            originPixelX = 0f,
            originPixelY = 500f,
            timestamp = 0L,
        )
    }

    @Test
    fun convert_basicPoints_correctTimeAndIntensity() {
        val cal = testCalibration()
        val points = listOf(
            CurvePoint(100, 250f, CurvePoint.HIGH_CONFIDENCE),
            CurvePoint(500, 100f, CurvePoint.HIGH_CONFIDENCE),
        )

        val signal = SignalConverter.convert(points, cal, "test.png")

        assertEquals(2, signal.points.size)

        // pixel 100 → time 10.0, pixel Y 250 → intensity 250
        assertNear(10f, signal.points[0].time)
        assertNear(250f, signal.points[0].intensity)

        // pixel 500 → time 50.0, pixel Y 100 → intensity 400
        assertNear(50f, signal.points[1].time)
        assertNear(400f, signal.points[1].intensity)
    }

    @Test
    fun convert_sortedByTime() {
        val cal = testCalibration()
        // Give points in reverse order
        val points = listOf(
            CurvePoint(800, 200f, CurvePoint.HIGH_CONFIDENCE),
            CurvePoint(200, 400f, CurvePoint.HIGH_CONFIDENCE),
            CurvePoint(500, 300f, CurvePoint.HIGH_CONFIDENCE),
        )

        val signal = SignalConverter.convert(points, cal, "test.png")
        assertTrue(signal.metadata.sortValid)
        assertTrue(signal.points.zipWithNext().all { (a, b) -> a.time <= b.time })
    }

    @Test
    fun convert_duplicateTimeRemoval() {
        val cal = testCalibration()
        // Two points at same pixelX
        val points = listOf(
            CurvePoint(100, 250f, CurvePoint.HIGH_CONFIDENCE),
            CurvePoint(100, 300f, CurvePoint.LOW_CONFIDENCE),
        )

        val signal = SignalConverter.convert(points, cal, "test.png")
        assertEquals(1, signal.points.size)
        assertEquals(1, signal.metadata.duplicatesRemoved)
        // Should keep HIGH_CONFIDENCE point
        assertNear(250f, signal.points[0].pixelY)
    }

    @Test
    fun convert_interpolatedPoints_markedCorrectly() {
        val cal = testCalibration()
        val points = listOf(
            CurvePoint(100, 250f, CurvePoint.HIGH_CONFIDENCE),
            CurvePoint(200, 300f, CurvePoint.INTERPOLATED),
            CurvePoint(300, 350f, CurvePoint.HIGH_CONFIDENCE),
        )

        val signal = SignalConverter.convert(points, cal, "test.png")
        assertEquals(1, signal.interpolatedCount)
        assertTrue(signal.points[1].isInterpolated)
    }

    @Test
    fun convert_indexing_sequential() {
        val cal = testCalibration()
        val points = (0 until 10).map { x ->
            CurvePoint(x * 100, 250f, CurvePoint.HIGH_CONFIDENCE)
        }

        val signal = SignalConverter.convert(points, cal, "test.png")
        signal.points.forEachIndexed { i, gp ->
            assertEquals(i, gp.index)
        }
    }

    @Test
    fun convert_emptyPoints_returnsEmptySignal() {
        val cal = testCalibration()
        val signal = SignalConverter.convert(emptyList(), cal, "test.png")
        assertTrue(signal.points.isEmpty())
        assertEquals("мин", signal.timeUnit)
        assertEquals("mAU", signal.intensityUnit)
    }

    @Test
    fun convert_gapDetection() {
        val cal = testCalibration()
        // Uniform spacing with one big gap
        val points = mutableListOf<CurvePoint>()
        for (x in 0..100 step 10) points.add(CurvePoint(x, 250f, CurvePoint.HIGH_CONFIDENCE))
        // Gap: 100 → 500
        for (x in 500..600 step 10) points.add(CurvePoint(x, 250f, CurvePoint.HIGH_CONFIDENCE))

        val signal = SignalConverter.convert(points, cal, "test.png")
        assertTrue(signal.metadata.gapCount > 0, "Should detect gap between x=100 and x=500")
    }

    private fun assertNear(expected: Float, actual: Float) {
        assertTrue(
            abs(expected - actual) < epsilon,
            "Expected $expected but was $actual",
        )
    }
}

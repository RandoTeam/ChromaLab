package com.chromalab.feature.processing.calibration

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PixelCalibrationTest {

    private val epsilon = 0.001f

    // --- LinearCalibration ---

    @Test
    fun linearCalibration_pixelToReal_correctTransform() {
        // pixel 100 → 35.0, pixel 500 → 65.0
        val cal = LinearCalibration(
            CalibrationPoint(100f, 35f),
            CalibrationPoint(500f, 65f),
        )
        // scale = (65-35)/(500-100) = 30/400 = 0.075
        // offset = 35 - 0.075 * 100 = 35 - 7.5 = 27.5
        assertNear(35f, cal.pixelToReal(100f))
        assertNear(65f, cal.pixelToReal(500f))
        assertNear(50f, cal.pixelToReal(300f)) // Midpoint
    }

    @Test
    fun linearCalibration_realToPixel_inverseTransform() {
        val cal = LinearCalibration(
            CalibrationPoint(100f, 35f),
            CalibrationPoint(500f, 65f),
        )
        assertNear(100f, cal.realToPixel(35f))
        assertNear(500f, cal.realToPixel(65f))
        assertNear(300f, cal.realToPixel(50f))
    }

    @Test
    fun linearCalibration_roundTrip_identity() {
        val cal = LinearCalibration(
            CalibrationPoint(200f, 10f),
            CalibrationPoint(800f, 100f),
        )
        val pixelValues = listOf(200f, 400f, 600f, 800f)
        for (px in pixelValues) {
            val real = cal.pixelToReal(px)
            val backToPixel = cal.realToPixel(real)
            assertNear(px, backToPixel)
        }
    }

    @Test
    fun linearCalibration_isValid_normalCase() {
        val cal = LinearCalibration(
            CalibrationPoint(100f, 35f),
            CalibrationPoint(500f, 65f),
        )
        assertTrue(cal.isValid)
    }

    @Test
    fun linearCalibration_isInvalid_samePixels() {
        val cal = LinearCalibration(
            CalibrationPoint(100f, 35f),
            CalibrationPoint(100f, 65f),
        )
        assertFalse(cal.isValid)
    }

    @Test
    fun linearCalibration_isInvalid_sameValues() {
        val cal = LinearCalibration(
            CalibrationPoint(100f, 35f),
            CalibrationPoint(500f, 35f),
        )
        assertFalse(cal.isValid)
    }

    // --- PixelCalibration ---

    @Test
    fun pixelCalibration_pixelToTime() {
        val pc = buildTestCalibration()
        assertNear(35f, pc.pixelToTime(100f))
        assertNear(65f, pc.pixelToTime(500f))
    }

    @Test
    fun pixelCalibration_pixelToIntensity() {
        val pc = buildTestCalibration()
        // Y axis: pixel 600 → 0, pixel 200 → 350
        // scale = (350-0)/(200-600) = 350/(-400) = -0.875
        assertNear(0f, pc.pixelToIntensity(600f))
        assertNear(350f, pc.pixelToIntensity(200f))
    }

    @Test
    fun pixelCalibration_invertedY_negativeScale() {
        val pc = buildTestCalibration()
        // Y scale must be negative (pixel down = intensity up)
        assertTrue(pc.yScale < 0, "Y scale should be negative for inverted Y")
    }

    @Test
    fun pixelCalibration_pixelToReal_pair() {
        val pc = buildTestCalibration()
        val (time, intensity) = pc.pixelToReal(300f, 400f)
        assertNear(50f, time)
        assertNear(175f, intensity)
    }

    @Test
    fun pixelCalibration_realToPixel_pair() {
        val pc = buildTestCalibration()
        val (px, py) = pc.realToPixel(50f, 175f)
        assertNear(300f, px)
        assertNear(400f, py)
    }

    @Test
    fun pixelCalibration_deterministic_samePoints_sameResult() {
        val pc1 = buildTestCalibration()
        val pc2 = buildTestCalibration()
        // Same calibration points must yield identical results
        val testPixels = listOf(100f, 200f, 300f, 400f, 500f)
        for (px in testPixels) {
            assertEquals(pc1.pixelToTime(px), pc2.pixelToTime(px))
            assertEquals(pc1.pixelToIntensity(px), pc2.pixelToIntensity(px))
        }
    }

    @Test
    fun pixelCalibration_isValid() {
        val pc = buildTestCalibration()
        assertTrue(pc.isValid)
    }

    // --- XAxisCalibration warnings ---

    @Test
    fun xCalibration_warning_pointOrderReversed() {
        val xCal = XAxisCalibration(
            calibration = LinearCalibration(
                CalibrationPoint(500f, 65f),
                CalibrationPoint(100f, 35f),
            ),
            unit = "мин",
            timestamp = 0L,
        )
        assertTrue(xCal.hasWarnings)
        assertTrue(xCal.warnings.any { "правее" in it })
    }

    @Test
    fun xCalibration_warning_timeDecreases() {
        val xCal = XAxisCalibration(
            calibration = LinearCalibration(
                CalibrationPoint(100f, 65f),
                CalibrationPoint(500f, 35f),
            ),
            unit = "мин",
            timestamp = 0L,
        )
        assertTrue(xCal.hasWarnings)
        assertTrue(xCal.warnings.any { "возрастать" in it })
    }

    // --- YAxisCalibration warnings ---

    @Test
    fun yCalibration_warning_wrongDirection() {
        // Positive scale means pixel up = intensity up, which is wrong
        val yCal = YAxisCalibration(
            calibration = LinearCalibration(
                CalibrationPoint(200f, 0f),
                CalibrationPoint(600f, 350f),
            ),
            unit = "mAU",
            timestamp = 0L,
        )
        assertTrue(yCal.hasWarnings)
        assertTrue(yCal.warnings.any { "направление" in it })
    }

    // --- Helpers ---

    private fun buildTestCalibration(): PixelCalibration {
        val xCal = LinearCalibration(
            CalibrationPoint(100f, 35f),
            CalibrationPoint(500f, 65f),
        )
        val yCal = LinearCalibration(
            CalibrationPoint(600f, 0f),   // Bottom: pixel 600 → intensity 0
            CalibrationPoint(200f, 350f), // Top: pixel 200 → intensity 350
        )
        return PixelCalibration(
            xCalibration = xCal,
            yCalibration = yCal,
            xUnit = "мин",
            yUnit = "mAU",
            originPixelX = 100f,
            originPixelY = 600f,
            timestamp = 1000L,
        )
    }

    private fun assertNear(expected: Float, actual: Float) {
        assertTrue(
            abs(expected - actual) < epsilon,
            "Expected $expected but was $actual (diff=${abs(expected - actual)})",
        )
    }
}

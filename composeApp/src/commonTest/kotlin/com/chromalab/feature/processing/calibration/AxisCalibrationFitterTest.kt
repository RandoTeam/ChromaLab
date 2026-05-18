package com.chromalab.feature.processing.calibration

import com.chromalab.feature.processing.geometry.CalibrationAnchorEvidence
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryAxis
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AxisCalibrationFitterTest {

    @Test
    fun multiAnchorCleanFitIsValid() {
        val fit = AxisCalibrationFitter().fit(
            axis = GeometryAxis.X,
            anchors = listOf(
                anchor(GeometryAxis.X, 0f, 0.0),
                anchor(GeometryAxis.X, 100f, 10.0),
                anchor(GeometryAxis.X, 200f, 20.0),
                anchor(GeometryAxis.X, 300f, 30.0),
            ),
            axisLengthPx = 300f,
            geometryCleanliness = 0.95f,
        )

        assertEquals(CalibrationFitStatus.VALID, fit.status)
        assertEquals(4, fit.acceptedAnchors.size)
        assertEquals(0, fit.rejectedAnchors.size)
        assertNear(0.1, fit.slope)
        assertNear(0.0, fit.intercept)
        assertTrue((fit.maxResidualPx ?: 999.0) <= 0.001)
        assertTrue((fit.r2 ?: 0.0) >= 0.999)
    }

    @Test
    fun outlierIsRejectedBeforeFinalFit() {
        val fit = AxisCalibrationFitter().fit(
            axis = GeometryAxis.X,
            anchors = listOf(
                anchor(GeometryAxis.X, 0f, 0.0),
                anchor(GeometryAxis.X, 100f, 10.0),
                anchor(GeometryAxis.X, 200f, 20.0),
                anchor(GeometryAxis.X, 300f, 80.0),
                anchor(GeometryAxis.X, 400f, 40.0),
            ),
            axisLengthPx = 400f,
        )

        assertEquals(CalibrationFitStatus.VALID, fit.status)
        assertEquals(4, fit.acceptedAnchors.size)
        assertEquals(1, fit.rejectedAnchors.size)
        assertEquals("calibration.outlier_residual_px", fit.rejectedAnchors.single().rejectionReason)
        assertNear(0.1, fit.slope)
    }

    @Test
    fun twoAnchorFitIsReviewByDefault() {
        val fit = AxisCalibrationFitter().fit(
            axis = GeometryAxis.Y,
            anchors = listOf(
                anchor(GeometryAxis.Y, 0f, 100.0),
                anchor(GeometryAxis.Y, 100f, 0.0),
            ),
            axisLengthPx = 100f,
        )

        assertEquals(CalibrationFitStatus.REVIEW, fit.status)
        assertEquals(2, fit.acceptedAnchors.size)
        assertTrue(fit.warnings.contains("calibration.y.two_anchor_review"))
    }

    @Test
    fun badResidualsBecomeReviewInsteadOfFakeValid() {
        val fit = AxisCalibrationFitter().fit(
            axis = GeometryAxis.X,
            anchors = listOf(
                anchor(GeometryAxis.X, 0f, 0.0),
                anchor(GeometryAxis.X, 100f, 10.0),
                anchor(GeometryAxis.X, 200f, 21.8),
            ),
            axisLengthPx = 200f,
        )

        assertEquals(CalibrationFitStatus.REVIEW, fit.status)
        assertTrue((fit.maxResidualPx ?: 0.0) > 3.0)
    }

    @Test
    fun invalidWhenTickCoordinatesAreMissing() {
        val fit = AxisCalibrationFitter().fit(
            axis = GeometryAxis.X,
            anchors = listOf(anchor(GeometryAxis.X, Float.NaN, 10.0)),
            axisLengthPx = 100f,
        )

        assertEquals(CalibrationFitStatus.INVALID, fit.status)
        assertTrue(fit.acceptedAnchors.isEmpty())
    }

    @Test
    fun fitCanConvertBackToExistingLinearCalibrationAdapter() {
        val fit = AxisCalibrationFitter().fit(
            axis = GeometryAxis.X,
            anchors = listOf(
                anchor(GeometryAxis.X, 50f, 5.0),
                anchor(GeometryAxis.X, 150f, 15.0),
                anchor(GeometryAxis.X, 250f, 25.0),
            ),
            axisLengthPx = 250f,
        )

        val linear = fit.toLinearCalibrationOrNull()

        assertNotNull(linear)
        assertNear(5f, linear.pixelToReal(50f))
        assertNear(25f, linear.pixelToReal(250f))
    }

    private fun anchor(axis: GeometryAxis, pixel: Float, value: Double): CalibrationAnchorEvidence =
        CalibrationAnchorEvidence(
            axis = axis,
            tickPixelPosition = pixel,
            value = value,
            rawText = value.toString(),
            confidence = 0.9f,
        )

    private fun assertNear(expected: Double, actual: Double?, epsilon: Double = 0.001) {
        assertNotNull(actual)
        assertTrue(kotlin.math.abs(expected - actual) <= epsilon, "expected=$expected actual=$actual")
    }

    private fun assertNear(expected: Float, actual: Float, epsilon: Float = 0.001f) {
        assertTrue(kotlin.math.abs(expected - actual) <= epsilon, "expected=$expected actual=$actual")
    }
}

package com.chromalab.feature.processing.pipeline

import com.chromalab.feature.processing.calibration.CalibrationPoint
import com.chromalab.feature.processing.calibration.LinearCalibration
import com.chromalab.feature.processing.calibration.PixelCalibration
import com.chromalab.feature.processing.curve.CurvePoint
import com.chromalab.feature.processing.signal.SignalConverter
import com.chromalab.feature.processing.signal.SignalSmoother
import com.chromalab.feature.processing.signal.SmoothingParams
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Determinism tests — verify that same inputs produce identical outputs.
 *
 * These tests enforce the core contract:
 * same image + same params + same manual edits = same signal
 */
class DeterminismTest {

    private fun testCalibration(): PixelCalibration {
        return PixelCalibration(
            xCalibration = LinearCalibration(
                CalibrationPoint(0f, 0f),
                CalibrationPoint(1000f, 100f),
            ),
            yCalibration = LinearCalibration(
                CalibrationPoint(500f, 0f),
                CalibrationPoint(0f, 500f),
            ),
            xUnit = "мин",
            yUnit = "mAU",
            originPixelX = 0f,
            originPixelY = 500f,
            timestamp = 0L,
        )
    }

    private fun testPoints(): List<CurvePoint> {
        return (0 until 100).map { x ->
            CurvePoint(x * 10, 250f + (x % 7) * 5f, CurvePoint.HIGH_CONFIDENCE)
        }
    }

    @Test
    fun signalConversion_identicalInputs_identicalOutputs() {
        val cal = testCalibration()
        val points = testPoints()

        val signal1 = SignalConverter.convert(points, cal, "test.png")
        val signal2 = SignalConverter.convert(points, cal, "test.png")

        assertEquals(signal1.points.size, signal2.points.size)
        signal1.points.zip(signal2.points).forEach { (a, b) ->
            assertEquals(a.time, b.time, "Time mismatch at index ${a.index}")
            assertEquals(a.intensity, b.intensity, "Intensity mismatch at index ${a.index}")
            assertEquals(a.confidence, b.confidence)
            assertEquals(a.isInterpolated, b.isInterpolated)
        }
    }

    @Test
    fun smoothing_identicalInputs_identicalOutputs() {
        val cal = testCalibration()
        val points = testPoints()
        val signal = SignalConverter.convert(points, cal, "test.png")
        val params = SmoothingParams(7, 2)

        val smoothed1 = SignalSmoother.smooth(signal, params)
        val smoothed2 = SignalSmoother.smooth(signal, params)

        smoothed1.smoothed.points.zip(smoothed2.smoothed.points).forEach { (a, b) ->
            assertEquals(a.intensity, b.intensity, "Smoothed intensity mismatch at index ${a.index}")
        }
    }

    @Test
    fun multipleRuns_sameOrder() {
        val cal = testCalibration()
        val points = testPoints()

        // Run 10 times, all must be identical
        val results = (1..10).map { SignalConverter.convert(points, cal, "test.png") }
        val reference = results.first()

        results.forEach { result ->
            assertEquals(reference.points.size, result.points.size)
            reference.points.zip(result.points).forEach { (a, b) ->
                assertEquals(a.time, b.time)
                assertEquals(a.intensity, b.intensity)
            }
        }
    }

    @Test
    fun pipelineVersion_exists() {
        val version = PipelineVersion.CURRENT
        assertTrue(version.major >= 1, "Pipeline version should be >= 1.0.0")
        assertTrue(version.versionString.isNotEmpty())
    }

    @Test
    fun fingerprint_sameInputs_sameHash() {
        val fp1 = PipelineFingerprint(
            PipelineVersion.CURRENT, "abc123", "def456", "ghi789",
        )
        val fp2 = PipelineFingerprint(
            PipelineVersion.CURRENT, "abc123", "def456", "ghi789",
        )
        assertEquals(fp1.combinedHash, fp2.combinedHash)
    }

    @Test
    fun fingerprint_differentInputs_differentHash() {
        val fp1 = PipelineFingerprint(
            PipelineVersion.CURRENT, "abc123", "def456", "ghi789",
        )
        val fp2 = PipelineFingerprint(
            PipelineVersion.CURRENT, "abc123", "CHANGED", "ghi789",
        )
        assertTrue(fp1.combinedHash != fp2.combinedHash)
    }
}

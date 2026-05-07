package com.chromalab.feature.processing.testing

import com.chromalab.feature.processing.calibration.CalibrationPoint
import com.chromalab.feature.processing.calibration.LinearCalibration
import com.chromalab.feature.processing.calibration.PixelCalibration
import com.chromalab.feature.processing.signal.SignalConverter
import com.chromalab.feature.processing.signal.SignalSmoother
import com.chromalab.feature.processing.signal.SmoothingParams
import com.chromalab.feature.processing.testing.SyntheticChromatogramGenerator.SyntheticPeak
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Golden tests — verify end-to-end pipeline accuracy against synthetic chromatograms
 * with known peak positions, heights, and widths.
 */
class GoldenChromatogramTest {

    private val cal = PixelCalibration(
        xCalibration = LinearCalibration(
            CalibrationPoint(0f, 0f),
            CalibrationPoint(500f, 50f),
        ),
        yCalibration = LinearCalibration(
            CalibrationPoint(400f, 0f),
            CalibrationPoint(0f, 400f),
        ),
        xUnit = "мин",
        yUnit = "mAU",
        originPixelX = 0f,
        originPixelY = 400f,
        timestamp = 0L,
    )

    @Test
    fun standardCurve_peakPositionsPreserved() {
        val curve = SyntheticChromatogramGenerator.standardTestCurve()
        val signal = SignalConverter.convert(curve, cal, "synthetic.png")

        // Known peak centers: pixel 100, 250, 400 → time 10, 25, 40
        val expectedPeakTimes = listOf(10f, 25f, 40f)

        // Find actual peaks (local maxima)
        val peaks = findPeaks(signal.points.map { it.time to it.intensity })

        assertTrue(peaks.size >= 3, "Should find at least 3 peaks, found ${peaks.size}")

        for (expected in expectedPeakTimes) {
            val nearest = peaks.minByOrNull { abs(it - expected) }!!
            assertTrue(
                abs(nearest - expected) < 1f,
                "Peak at time $expected not found. Nearest: $nearest",
            )
        }
    }

    @Test
    fun standardCurve_peakHeightsAccurate() {
        val curve = SyntheticChromatogramGenerator.standardTestCurve()
        val signal = SignalConverter.convert(curve, cal, "synthetic.png")

        // Known peak heights: 80, 200, 120 (in intensity units)
        val expectedHeights = listOf(80f, 200f, 120f)
        val expectedTimes = listOf(10f, 25f, 40f)

        for (i in expectedTimes.indices) {
            val peakIntensity = signal.points
                .filter { abs(it.time - expectedTimes[i]) < 1f }
                .maxOfOrNull { it.intensity } ?: 0f

            assertTrue(
                abs(peakIntensity - expectedHeights[i]) < 5f,
                "Peak $i: expected height ${expectedHeights[i]}, got $peakIntensity",
            )
        }
    }

    @Test
    fun noisyCurve_smoothingReducesError() {
        val clean = SyntheticChromatogramGenerator.standardTestCurve()
        val noisy = SyntheticChromatogramGenerator.addNoise(clean, seed = 42, amplitude = 5f)

        val cleanSignal = SignalConverter.convert(clean, cal, "clean.png")
        val noisySignal = SignalConverter.convert(noisy, cal, "noisy.png")
        val smoothed = SignalSmoother.smooth(noisySignal, SmoothingParams(7, 2))

        // Smoothed should be closer to clean than noisy
        val noisyError = meanError(cleanSignal.points.map { it.intensity }, noisySignal.points.map { it.intensity })
        val smoothedError = meanError(cleanSignal.points.map { it.intensity }, smoothed.smoothed.points.map { it.intensity })

        assertTrue(
            smoothedError < noisyError,
            "Smoothed error ($smoothedError) should be < noisy error ($noisyError)",
        )
    }

    @Test
    fun complexCurve_allPeaksDetected() {
        val curve = SyntheticChromatogramGenerator.complexTestCurve()
        val signal = SignalConverter.convert(curve, cal, "complex.png")

        // 5 peaks: pixel 80, 200, 230, 350, 450 → time 8, 20, 23, 35, 45
        val peaks = findPeaks(signal.points.map { it.time to it.intensity })

        assertTrue(peaks.size >= 4, "Should find at least 4 peaks in complex curve, found ${peaks.size}")
    }

    @Test
    fun weakContrastCurve_peaksStillDetectable() {
        val clean = SyntheticChromatogramGenerator.standardTestCurve()
        val weak = SyntheticChromatogramGenerator.weakContrast(clean, baseline = 400f, factor = 0.3f)
        val signal = SignalConverter.convert(weak, cal, "weak.png")

        val peaks = findPeaks(signal.points.map { it.time to it.intensity })
        assertTrue(peaks.isNotEmpty(), "Should still find peaks in weak contrast")
    }

    @Test
    fun tiltedCurve_hasTiltArtifact() {
        val clean = SyntheticChromatogramGenerator.standardTestCurve()
        val tilted = SyntheticChromatogramGenerator.addTilt(clean, slopePerPixel = 0.1f)
        val signal = SignalConverter.convert(tilted, cal, "tilted.png")

        // First and last point should differ due to tilt
        val first = signal.points.first().intensity
        val last = signal.points.last().intensity
        assertTrue(
            abs(first - last) > 10f,
            "Tilted curve should show baseline drift: first=$first, last=$last",
        )
    }

    @Test
    fun goldenPoints_deterministic() {
        val curve1 = SyntheticChromatogramGenerator.standardTestCurve()
        val curve2 = SyntheticChromatogramGenerator.standardTestCurve()

        curve1.zip(curve2).forEach { (a, b) ->
            assertTrue(a.pixelX == b.pixelX && a.pixelY == b.pixelY,
                "Golden points must be deterministic")
        }
    }

    // --- Helpers ---

    private fun findPeaks(data: List<Pair<Float, Float>>): List<Float> {
        if (data.size < 3) return emptyList()
        val peaks = mutableListOf<Float>()
        for (i in 1 until data.size - 1) {
            if (data[i].second > data[i - 1].second && data[i].second > data[i + 1].second) {
                // Only count significant peaks (> 5% of max)
                val maxIntensity = data.maxOf { it.second }
                if (data[i].second > maxIntensity * 0.05f) {
                    peaks.add(data[i].first)
                }
            }
        }
        return peaks
    }

    private fun meanError(expected: List<Float>, actual: List<Float>): Float {
        if (expected.size != actual.size) return Float.MAX_VALUE
        return expected.zip(actual).map { (e, a) -> abs(e - a) }.average().toFloat()
    }
}

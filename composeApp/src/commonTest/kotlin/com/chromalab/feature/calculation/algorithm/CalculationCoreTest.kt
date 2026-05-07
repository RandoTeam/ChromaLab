package com.chromalab.feature.calculation.algorithm

import kotlin.math.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for the Phase 2 calculation core (§2.32).
 *
 * Covers:
 * 1. Input validation & sorting
 * 2. Savitzky-Golay smoothing
 * 3. Baseline: Manual, ALS, SNIP
 * 4. Peak detection: single, close, noise-only
 * 5. Peak boundaries & prominence
 * 6. Trapezoidal integration: rectangle, triangle, Gaussian, irregular
 * 7. S/N: peak-to-peak, RMS, MAD
 * 8. Manual correction & rejected peak
 * 9. Determinism: identical input → identical output
 */

class CalculationCoreTest {

    private val eps = 1e-6

    // ─── Helper: generate signals ───────────────────────────────

    private fun gaussian(x: Double, center: Double, sigma: Double, height: Double): Double =
        height * exp(-(x - center).pow(2) / (2.0 * sigma.pow(2)))

    private fun makeSignal(n: Int, dt: Double = 0.1, generator: (Double) -> Double): Pair<DoubleArray, DoubleArray> {
        val times = DoubleArray(n) { it * dt }
        val intensities = DoubleArray(n) { generator(times[it]) }
        return times to intensities
    }

    // ═════════════════════════════════════════════════════════════
    // 1. Input validation & sorting
    // ═════════════════════════════════════════════════════════════

    @Test
    fun validation_emptySignalRejected() {
        val result = SignalValidator.validate(doubleArrayOf(), doubleArrayOf())
        assertTrue(!result.isValid, "Empty signal should be invalid")
    }

    @Test
    fun validation_mismatchedLengthsRejected() {
        val result = SignalValidator.validate(doubleArrayOf(1.0, 2.0), doubleArrayOf(1.0))
        assertTrue(!result.isValid, "Mismatched lengths should be invalid")
    }

    @Test
    fun validation_validSignalAccepted() {
        val (t, y) = makeSignal(100) { 50.0 }
        val result = SignalValidator.validate(t, y)
        assertTrue(result.isValid, "100-point constant signal should be valid")
    }

    @Test
    fun validation_monotonicTimeRequired() {
        val t = doubleArrayOf(1.0, 3.0, 2.0, 4.0)
        val y = doubleArrayOf(0.0, 0.0, 0.0, 0.0)
        val result = SignalValidator.validate(t, y)
        assertTrue(!result.isValid, "Non-monotonic time should be invalid")
    }

    // ═════════════════════════════════════════════════════════════
    // 2. Smoothing
    // ═════════════════════════════════════════════════════════════

    @Test
    fun smoothing_constantSignalUnchanged() {
        val (_, y) = makeSignal(50) { 100.0 }
        val smoothed = SavitzkyGolayFilter.smooth(y, windowSize = 7, polyOrder = 3)
        smoothed.forEach { v ->
            assertTrue(abs(v - 100.0) < 0.01, "Constant signal should be unchanged: $v")
        }
    }

    @Test
    fun smoothing_reducesNoise() {
        val rng = LinearCongruentialRng(42)
        val (_, y) = makeSignal(200) { 100.0 + rng.nextDouble() * 10.0 - 5.0 }
        val smoothed = SavitzkyGolayFilter.smooth(y, windowSize = 11, polyOrder = 3)
        val rawVar = variance(y)
        val smoothVar = variance(smoothed)
        assertTrue(smoothVar < rawVar, "Smoothed variance ($smoothVar) should be < raw ($rawVar)")
    }

    @Test
    fun smoothing_preservesPeakPosition() {
        val (_, y) = makeSignal(100) { x -> gaussian(x, 5.0, 0.5, 1000.0) }
        val smoothed = SavitzkyGolayFilter.smooth(y, windowSize = 7, polyOrder = 3)
        val rawPeak = y.indexOfMax()
        val smoothPeak = smoothed.indexOfMax()
        assertEquals(rawPeak, smoothPeak, "Peak position should be preserved after smoothing")
    }

    // ═════════════════════════════════════════════════════════════
    // 3. Baseline
    // ═════════════════════════════════════════════════════════════

    @Test
    fun baseline_manual_flatSignal() {
        val y = DoubleArray(100) { 50.0 }
        val baseline = ManualLinearBaseline.compute(y, 0, y.lastIndex)
        baseline.forEach { v ->
            assertTrue(abs(v - 50.0) < eps, "Manual baseline on flat signal should be 50.0")
        }
    }

    @Test
    fun baseline_als_flatSignal() {
        val y = DoubleArray(100) { 50.0 }
        val baseline = AlsBaseline.compute(y, lambda = 1e6, p = 0.01, iterations = 10)
        baseline.forEach { v ->
            assertTrue(abs(v - 50.0) < 1.0, "ALS baseline on flat signal should be ~50.0: $v")
        }
    }

    @Test
    fun baseline_snip_flatSignal() {
        val y = DoubleArray(100) { 50.0 }
        val baseline = SnipBaseline.compute(y, iterations = 40)
        baseline.forEach { v ->
            assertTrue(abs(v - 50.0) < 1.0, "SNIP baseline on flat signal should be ~50.0: $v")
        }
    }

    @Test
    fun baseline_correction_removesDrift() {
        // Linear drift: y = 10 + 0.5*x
        val (_, y) = makeSignal(100) { x -> 10.0 + 0.5 * x }
        val baseline = ManualLinearBaseline.compute(y, 0, y.lastIndex)
        val corrected = DoubleArray(y.size) { y[it] - baseline[it] }
        corrected.forEach { v ->
            assertTrue(abs(v) < 0.1, "Corrected signal should be ~0 after drift removal: $v")
        }
    }

    // ═════════════════════════════════════════════════════════════
    // 4. Peak detection
    // ═════════════════════════════════════════════════════════════

    @Test
    fun peakDetection_singleGaussian() {
        val (_, y) = makeSignal(200) { x -> gaussian(x, 10.0, 0.5, 1000.0) }
        val peaks = PeakDetector.detect(y, noiseLevel = 1.0, minDistance = 5, minWidth = 3)
        assertEquals(1, peaks.size, "Should detect exactly 1 peak in single Gaussian")
    }

    @Test
    fun peakDetection_twoPeaks() {
        val (_, y) = makeSignal(300) { x ->
            gaussian(x, 10.0, 0.5, 1000.0) + gaussian(x, 20.0, 0.5, 800.0)
        }
        val peaks = PeakDetector.detect(y, noiseLevel = 1.0, minDistance = 5, minWidth = 3)
        assertEquals(2, peaks.size, "Should detect 2 peaks")
    }

    @Test
    fun peakDetection_noiseOnly_noPeaks() {
        val rng = LinearCongruentialRng(123)
        val (_, y) = makeSignal(200) { rng.nextDouble() * 2.0 - 1.0 }
        val peaks = PeakDetector.detect(y, noiseLevel = 5.0, minDistance = 5, minWidth = 3)
        assertEquals(0, peaks.size, "Should detect 0 peaks in noise-only signal")
    }

    @Test
    fun peakDetection_closePeaks_respectsMinDistance() {
        // Two peaks very close
        val (_, y) = makeSignal(100) { x ->
            gaussian(x, 5.0, 0.3, 1000.0) + gaussian(x, 5.5, 0.3, 800.0)
        }
        val peaks = PeakDetector.detect(y, noiseLevel = 1.0, minDistance = 10, minWidth = 2)
        assertEquals(1, peaks.size, "minDistance=10 should merge close peaks into 1")
    }

    // ═════════════════════════════════════════════════════════════
    // 5. Boundaries & prominence
    // ═════════════════════════════════════════════════════════════

    @Test
    fun boundaries_singlePeak_coversFullWidth() {
        val (_, y) = makeSignal(200) { x -> gaussian(x, 10.0, 1.0, 1000.0) }
        val peaks = PeakDetector.detect(y, noiseLevel = 1.0, minDistance = 5, minWidth = 3)
        assertTrue(peaks.isNotEmpty(), "Should detect peak")
        val peak = peaks[0]
        val boundaries = PeakBoundaryDetector.findBoundaries(y, peak.index, PeakBoundaryDetector.Method.LOCAL_MINIMA)
        assertTrue(boundaries.leftIndex < peak.index, "Left boundary should be before peak")
        assertTrue(boundaries.rightIndex > peak.index, "Right boundary should be after peak")
    }

    // ═════════════════════════════════════════════════════════════
    // 6. Trapezoidal integration
    // ═════════════════════════════════════════════════════════════

    @Test
    fun integration_rectangle() {
        // Constant y=10, width=5 → area = 50
        val y = DoubleArray(51) { 10.0 }
        val result = PeakIntegrator.integrate(y, 0, 50, dx = 0.1)
        val expectedArea = 10.0 * 5.0 // width = 50 * 0.1 = 5.0
        assertTrue(abs(result.totalArea - expectedArea) < 0.1, "Rectangle area: ${result.totalArea} ≈ $expectedArea")
    }

    @Test
    fun integration_triangle() {
        // Triangle: rises from 0 to 100 at center, back to 0. area = 100 * 5 / 2 = 250
        val n = 101
        val y = DoubleArray(n) { i ->
            val x = i.toDouble() / (n - 1) * 10.0
            if (x <= 5.0) x * 20.0 else (10.0 - x) * 20.0
        }
        val result = PeakIntegrator.integrate(y, 0, n - 1, dx = 10.0 / (n - 1))
        val expectedArea = 100.0 * 10.0 / 2.0 // triangle
        assertTrue(abs(result.totalArea - expectedArea) / expectedArea < 0.05, "Triangle area: ${result.totalArea} ≈ $expectedArea")
    }

    @Test
    fun integration_gaussian() {
        // Gaussian: area = height * sigma * sqrt(2π) ≈ 1000 * 1 * 2.507
        val sigma = 1.0
        val height = 1000.0
        val n = 1000
        val dt = 20.0 / n
        val y = DoubleArray(n) { i -> gaussian(i * dt, 10.0, sigma, height) }
        val result = PeakIntegrator.integrate(y, 0, n - 1, dx = dt)
        val expectedArea = height * sigma * sqrt(2.0 * PI)
        assertTrue(abs(result.totalArea - expectedArea) / expectedArea < 0.02, "Gaussian area: ${result.totalArea} ≈ $expectedArea")
    }

    @Test
    fun integration_negativeAreaTracked() {
        val y = DoubleArray(11) { i -> if (i < 5) -10.0 else 10.0 }
        val result = PeakIntegrator.integrate(y, 0, 10, dx = 1.0)
        assertTrue(result.negativeArea > 0, "Negative area should be tracked: ${result.negativeArea}")
        assertTrue(result.positiveArea > 0, "Positive area should be tracked: ${result.positiveArea}")
    }

    // ═════════════════════════════════════════════════════════════
    // 7. S/N
    // ═════════════════════════════════════════════════════════════

    @Test
    fun snr_peakToPeak() {
        val noise = doubleArrayOf(1.0, -1.0, 0.5, -0.5, 0.3, -0.3)
        val result = NoiseEstimator.estimatePeakToPeak(noise)
        assertTrue(result > 0, "P2P noise should be positive: $result")
        assertTrue(abs(result - 2.0) < eps, "P2P of ±1 signal should be 2.0: $result")
    }

    @Test
    fun snr_rms() {
        val noise = doubleArrayOf(1.0, -1.0, 1.0, -1.0)
        val result = NoiseEstimator.estimateRms(noise)
        assertTrue(abs(result - 1.0) < eps, "RMS of ±1 signal should be 1.0: $result")
    }

    @Test
    fun snr_mad() {
        val noise = doubleArrayOf(0.0, 1.0, -1.0, 0.5, -0.5, 0.2, -0.2)
        val result = NoiseEstimator.estimateMad(noise)
        assertTrue(result > 0, "MAD should be positive: $result")
    }

    // ═════════════════════════════════════════════════════════════
    // 8. Manual correction
    // ═════════════════════════════════════════════════════════════

    @Test
    fun manualEdit_appendOnly() {
        val log = com.chromalab.feature.calculation.core.ManualEditLog()
        val edit = com.chromalab.feature.calculation.core.ManualEdit.boundaryLeft(0, 1.0, 1.5, "test")
        val log2 = log.append(edit)
        assertEquals(0, log.totalEdits, "Original log should be unchanged")
        assertEquals(1, log2.totalEdits, "New log should have 1 edit")
        assertTrue(log2.isEdited(0), "Peak 0 should be marked edited")
    }

    @Test
    fun manualEdit_rejectedPeak() {
        val log = com.chromalab.feature.calculation.core.ManualEditLog()
            .append(com.chromalab.feature.calculation.core.ManualEdit.peakRejected(0, "low quality"))
        assertTrue(log.isRejected(0), "Peak 0 should be rejected")
        assertTrue(!log.isRejected(1), "Peak 1 should NOT be rejected")
    }

    @Test
    fun manualEdit_csvExport() {
        val log = com.chromalab.feature.calculation.core.ManualEditLog()
            .append(com.chromalab.feature.calculation.core.ManualEdit.boundaryLeft(0, 1.0, 1.5))
        val csv = log.toCsv()
        assertTrue(csv.contains("PeakIndex,EditType"), "CSV should have header")
        assertTrue(csv.contains("BOUNDARY_LEFT"), "CSV should contain edit type")
    }

    // ═════════════════════════════════════════════════════════════
    // 9. Determinism
    // ═════════════════════════════════════════════════════════════

    @Test
    fun determinism_identicalInputIdenticalOutput() {
        val (_, y) = makeSignal(200) { x -> gaussian(x, 10.0, 0.5, 1000.0) + 50.0 }

        // Run smoothing twice
        val s1 = SavitzkyGolayFilter.smooth(y.copyOf(), 7, 3)
        val s2 = SavitzkyGolayFilter.smooth(y.copyOf(), 7, 3)
        assertTrue(s1.contentEquals(s2), "Smoothing should be deterministic")

        // Run peak detection twice
        val p1 = PeakDetector.detect(y.copyOf(), 1.0, 5, 3)
        val p2 = PeakDetector.detect(y.copyOf(), 1.0, 5, 3)
        assertEquals(p1.size, p2.size, "Peak count should be deterministic")
        p1.zip(p2).forEach { (a, b) ->
            assertEquals(a.index, b.index, "Peak indices should match")
            assertEquals(a.height, b.height, "Peak heights should match")
        }
    }

    @Test
    fun determinism_contractValidation() {
        val info = com.chromalab.feature.calculation.core.DeterminismInfo.phase2()
        val violations = info.validate()
        assertTrue(violations.isEmpty(), "Phase 2 default should have no violations: $violations")
    }

    // ─── Utilities ──────────────────────────────────────────────

    private fun variance(arr: DoubleArray): Double {
        val mean = arr.average()
        return arr.sumOf { (it - mean).pow(2) } / arr.size
    }

    private fun DoubleArray.indexOfMax(): Int {
        var maxIdx = 0
        for (i in indices) if (this[i] > this[maxIdx]) maxIdx = i
        return maxIdx
    }
}

/**
 * Simple deterministic PRNG for tests — no kotlin.random dependency.
 */
class LinearCongruentialRng(seed: Long) {
    private var state = seed
    fun nextDouble(): Double {
        state = (state * 6364136223846793005L + 1442695040888963407L)
        return ((state ushr 33).toDouble()) / Int.MAX_VALUE.toDouble()
    }
}

package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.CalculationEngine
import com.chromalab.feature.calculation.core.SignalSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyntheticFixtureRegressionTest {

    @Test
    fun isolatedGaussianFixtureMatchesKnownPeakMetrics() {
        val fixture = CalculationSyntheticFixtures.isolatedGaussian
        val expected = fixture.peaks.single()
        val run = CalculationEngine.execute(
            signal = fixture.signal(),
            sourceId = fixture.id,
            params = fixture.params,
            runId = "${fixture.id}-run",
        )

        val peak = run.peaks.single()

        assertEquals(expected.center, peak.rtApex, 0.021)
        assertEquals(expected.center, peak.rtCentroid ?: -1.0, 0.021)
        assertEquals(expected.height, peak.height, 0.5)
        assertEquals(expected.expectedArea, peak.area, 2.0)
        assertEquals(expected.expectedFwhm, peak.widthHalfHeight ?: -1.0, 0.04)
        assertEquals(expected.expectedBaseWidthAtOnePercent, peak.widthBase, 0.06)
        assertEquals(expected.expectedSnr ?: -1.0, peak.snr, 1.0)
        assertEquals(expected.expectedOverlap, peak.overlapStatus)
        assertEquals(100.0, peak.areaPercent, 1e-9)
    }

    @Test
    fun overlappingPairFixtureKeepsExpectedOverlapClassification() {
        val fixture = CalculationSyntheticFixtures.overlappingPair
        val run = CalculationEngine.execute(
            signal = fixture.signal(),
            sourceId = fixture.id,
            params = fixture.params,
            runId = "${fixture.id}-run",
        )

        assertEquals(fixture.peaks.size, run.peaks.size)
        run.peaks.zip(fixture.peaks).forEach { (actual, expected) ->
            assertEquals(expected.center, actual.rtApex, 0.08)
            assertTrue(actual.height > expected.height * 0.90)
            assertEquals(expected.expectedOverlap, actual.overlapStatus)
            assertTrue(actual.warnings.isNotEmpty())
        }
        assertNotNull(run.peaks[1].resolution)
        assertTrue(run.peaks[1].resolution!! < 2.0)
    }

    @Test
    fun equalAreaPairLocksAreaPercentFwhmNoiseAndConfidenceGates() {
        val fixture = CalculationSyntheticFixtures.equalAreaPair
        val run = CalculationEngine.execute(
            signal = fixture.signal(),
            sourceId = fixture.id,
            params = fixture.params,
            runId = "${fixture.id}-run",
        )

        assertEquals(fixture.peaks.size, run.peaks.size)
        assertNull(run.signals.baseline)
        assertNull(run.signals.baselineCorrected)
        assertEquals(SignalSource.RAW, run.signals.signalUsedForDetection)
        assertEquals(SignalSource.RAW, run.signals.signalUsedForIntegration)
        assertEquals(100.0, run.peaks.sumOf { it.areaPercent }, 1e-9)

        run.peaks.zip(fixture.peaks).forEach { (actual, expected) ->
            assertEquals(expected.center, actual.rtApex, 0.021)
            assertEquals(expected.height, actual.height, 0.5)
            assertEquals(expected.expectedFwhm, actual.widthHalfHeight ?: -1.0, 0.04)
            assertEquals(expected.expectedArea, actual.area, 1.5)
            assertEquals(50.0, actual.areaPercent, 1.5)
            assertEquals(expected.expectedSnr ?: -1.0, actual.snr, 1.0)
            assertEquals("Peak-to-peak", actual.snrMethod)
            assertEquals(ConfidenceGrade.HIGH, actual.confidence)
            assertEquals(OverlapStatus.ISOLATED, actual.overlapStatus)
            assertTrue(actual.warnings.isEmpty())
        }
    }

    @Test
    fun manualLinearBaselineCorrectionIsAuditedAndUsedForMetrics() {
        val fixture = CalculationSyntheticFixtures.linearBaselinePeak
        val expected = fixture.peaks.single()
        val run = CalculationEngine.execute(
            signal = fixture.signal(),
            sourceId = fixture.id,
            params = fixture.params,
            runId = "${fixture.id}-run",
        )

        val baseline = assertNotNull(run.signals.baseline)
        val corrected = assertNotNull(run.signals.baselineCorrected)
        val peak = run.peaks.single()

        assertEquals(run.signals.raw.size, baseline.size)
        assertEquals(SignalSource.BASELINE_CORRECTED, run.signals.signalUsedForDetection)
        assertEquals(SignalSource.BASELINE_CORRECTED, run.signals.signalUsedForIntegration)
        assertEquals(run.signals.raw.first().intensity, baseline.first(), 1e-9)
        assertEquals(run.signals.raw.last().intensity, baseline.last(), 1e-9)
        assertEquals(0.0, corrected.first().intensity, 1e-9)
        assertEquals(0.0, corrected.last().intensity, 1e-9)
        assertEquals(expected.center, peak.rtApex, 0.021)
        assertEquals(expected.height, peak.height, 0.5)
        assertEquals(expected.expectedFwhm, peak.widthHalfHeight ?: -1.0, 0.04)
        assertEquals("MANUAL_LINEAR", peak.baselineMethod)
        assertEquals("RMS", peak.snrMethod)
        assertEquals(ConfidenceGrade.HIGH, peak.confidence)
        assertTrue(peak.warnings.isEmpty())
    }
}

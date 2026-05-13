package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.CalculationEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
}

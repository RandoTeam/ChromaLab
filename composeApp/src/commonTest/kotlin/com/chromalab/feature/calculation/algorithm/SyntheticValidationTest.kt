package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.CalculationEngine
import com.chromalab.feature.calculation.core.CalculationParams
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.signal.GraphPoint
import com.chromalab.feature.processing.signal.SignalMetadata
import kotlin.math.exp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyntheticValidationTest {

    @Test
    fun calculationEngineDetectsSingleSyntheticPeak() {
        val signal = syntheticSignal(
            pointCount = 201,
            timeStep = 0.05,
            baseline = 25.0,
            peaks = listOf(SyntheticPeak(center = 5.0, height = 140.0, sigma = 0.18)),
        )

        val run = CalculationEngine.execute(
            signal = signal,
            sourceId = "synthetic-single",
            params = CalculationParams(
                smoothingEnabled = false,
                baselineMethod = "NONE",
                minPeakHeight = 60.0,
                minPeakProminence = 50.0,
                minPeakDistance = 20,
                minPeakWidth = 3,
                minSnr = 0.0,
                boundaryMethod = "PERCENT_HEIGHT",
                boundaryPercentHeight = 0.01,
                integrationMethod = "TRAPEZOIDAL",
            ),
            runId = "synthetic-single-run",
        )

        val peak = run.peaks.single()
        assertEquals("synthetic-single-run", run.id)
        assertEquals(5.0, peak.rtApex, 0.051)
        assertTrue(peak.height > 130.0)
        assertTrue(peak.area > 50.0)
        assertEquals("PERCENT_HEIGHT", peak.boundaryMethod)
        assertEquals("TRAPEZOIDAL", peak.integrationMethod)
        assertEquals(100.0, peak.areaPercent, 1e-9)
    }

    @Test
    fun calculationEngineSeparatesTwoSyntheticPeaks() {
        val signal = syntheticSignal(
            pointCount = 241,
            timeStep = 0.05,
            baseline = 10.0,
            peaks = listOf(
                SyntheticPeak(center = 4.0, height = 160.0, sigma = 0.20),
                SyntheticPeak(center = 7.0, height = 80.0, sigma = 0.16),
            ),
        )

        val run = CalculationEngine.execute(
            signal = signal,
            sourceId = "synthetic-double",
            params = CalculationParams(
                smoothingEnabled = false,
                baselineMethod = "NONE",
                minPeakHeight = 45.0,
                minPeakProminence = 35.0,
                minPeakDistance = 20,
                minPeakWidth = 3,
                minSnr = 0.0,
                boundaryMethod = "PERCENT_HEIGHT",
                boundaryPercentHeight = 0.01,
                integrationMethod = "TRAPEZOIDAL_INTERPOLATED",
            ),
            runId = "synthetic-double-run",
        )

        assertEquals(2, run.peaks.size)
        assertEquals(listOf("TRAPEZOIDAL_INTERPOLATED", "TRAPEZOIDAL_INTERPOLATED"), run.peaks.map { it.integrationMethod })
        assertEquals(4.0, run.peaks[0].rtApex, 0.051)
        assertEquals(7.0, run.peaks[1].rtApex, 0.051)
        assertTrue(run.peaks[0].height > run.peaks[1].height)
        assertEquals(100.0, run.peaks.sumOf { it.areaPercent }, 1e-6)
    }

    @Test
    fun maxPeakWidthRejectsOverWideSyntheticPeak() {
        val signal = syntheticSignal(
            pointCount = 201,
            timeStep = 0.05,
            baseline = 0.0,
            peaks = listOf(SyntheticPeak(center = 5.0, height = 100.0, sigma = 0.8)),
        )

        val accepted = CalculationEngine.execute(
            signal = signal,
            sourceId = "wide-accepted",
            params = CalculationParams(
                smoothingEnabled = false,
                baselineMethod = "NONE",
                minPeakHeight = 30.0,
                minPeakProminence = 20.0,
                minPeakDistance = 20,
                minPeakWidth = 3,
                maxPeakWidth = 0,
                minSnr = 0.0,
            ),
            runId = "wide-accepted-run",
        )
        val rejected = CalculationEngine.execute(
            signal = signal,
            sourceId = "wide-rejected",
            params = accepted.params.copy(maxPeakWidth = 12),
            runId = "wide-rejected-run",
        )

        assertEquals(1, accepted.peaks.size)
        assertEquals(0, rejected.peaks.size)
        assertTrue(rejected.warnings.isNotEmpty())
    }

    private fun syntheticSignal(
        pointCount: Int,
        timeStep: Double,
        baseline: Double,
        peaks: List<SyntheticPeak>,
    ): DigitalSignal {
        val points = (0 until pointCount).map { index ->
            val time = index * timeStep
            val intensity = baseline + peaks.sumOf { it.valueAt(time) }
            GraphPoint(
                index = index,
                pixelX = index,
                pixelY = intensity.toFloat(),
                time = time.toFloat(),
                intensity = intensity.toFloat(),
                confidence = 1f,
                isInterpolated = false,
            )
        }
        return DigitalSignal(
            points = points,
            timeUnit = "min",
            intensityUnit = "counts",
            metadata = SignalMetadata(
                sourceImage = "synthetic",
                totalPoints = pointCount,
                duplicatesRemoved = 0,
                gapCount = 0,
                sortValid = true,
                timestamp = 0L,
            ),
        )
    }

    private data class SyntheticPeak(
        val center: Double,
        val height: Double,
        val sigma: Double,
    ) {
        fun valueAt(time: Double): Double =
            height * exp(-0.5 * ((time - center) / sigma) * ((time - center) / sigma))
    }
}

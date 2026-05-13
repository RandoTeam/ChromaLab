package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.CalculationParams
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.signal.GraphPoint
import com.chromalab.feature.processing.signal.SignalMetadata
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

internal object CalculationSyntheticFixtures {

    val isolatedGaussian = SyntheticCalculationFixture(
        id = "isolated-gaussian-known-metrics",
        pointCount = 601,
        timeStep = 0.02,
        peaks = listOf(
            SyntheticPeakTruth(
                center = 5.0,
                height = 120.0,
                sigma = 0.20,
                expectedArea = gaussianArea(height = 120.0, sigma = 0.20),
                expectedFwhm = gaussianFwhm(sigma = 0.20),
                expectedBaseWidthAtOnePercent = gaussianWidthAtFraction(sigma = 0.20, fraction = 0.01),
                expectedSnr = 60.0,
                expectedOverlap = OverlapStatus.ISOLATED,
            ),
        ),
        noise = { index, time ->
            if (time < 2.0 || time > 10.0) {
                if (index % 2 == 0) 2.0 else -2.0
            } else {
                0.0
            }
        },
        params = CalculationParams(
            smoothingEnabled = false,
            baselineMethod = "NONE",
            minPeakHeight = 40.0,
            minPeakProminence = 40.0,
            minPeakDistance = 20,
            minPeakWidth = 3,
            minSnr = 0.0,
            noiseMethod = "PEAK_TO_PEAK",
            boundaryMethod = "PERCENT_HEIGHT",
            boundaryPercentHeight = 0.01,
            integrationMethod = "TRAPEZOIDAL",
            clampNegative = false,
        ),
    )

    val overlappingPair = SyntheticCalculationFixture(
        id = "overlapping-gaussian-pair",
        pointCount = 601,
        timeStep = 0.02,
        peaks = listOf(
            SyntheticPeakTruth(
                center = 5.0,
                height = 100.0,
                sigma = 0.22,
                expectedArea = gaussianArea(height = 100.0, sigma = 0.22),
                expectedFwhm = gaussianFwhm(sigma = 0.22),
                expectedBaseWidthAtOnePercent = gaussianWidthAtFraction(sigma = 0.22, fraction = 0.01),
                expectedSnr = null,
                expectedOverlap = OverlapStatus.PARTIALLY_OVERLAPPED,
            ),
            SyntheticPeakTruth(
                center = 5.64,
                height = 90.0,
                sigma = 0.22,
                expectedArea = gaussianArea(height = 90.0, sigma = 0.22),
                expectedFwhm = gaussianFwhm(sigma = 0.22),
                expectedBaseWidthAtOnePercent = gaussianWidthAtFraction(sigma = 0.22, fraction = 0.01),
                expectedSnr = null,
                expectedOverlap = OverlapStatus.PARTIALLY_OVERLAPPED,
            ),
        ),
        params = CalculationParams(
            smoothingEnabled = false,
            baselineMethod = "NONE",
            minPeakHeight = 35.0,
            minPeakProminence = 8.0,
            minPeakDistance = 10,
            minPeakWidth = 3,
            minSnr = 0.0,
            noiseMethod = "PEAK_TO_PEAK",
            boundaryMethod = "LOCAL_MINIMA",
            integrationMethod = "TRAPEZOIDAL",
            clampNegative = false,
        ),
    )

    val equalAreaPair = SyntheticCalculationFixture(
        id = "equal-area-gaussian-pair",
        pointCount = 601,
        timeStep = 0.02,
        peaks = listOf(
            SyntheticPeakTruth(
                center = 4.0,
                height = 120.0,
                sigma = 0.15,
                expectedArea = gaussianArea(height = 120.0, sigma = 0.15),
                expectedFwhm = gaussianFwhm(sigma = 0.15),
                expectedBaseWidthAtOnePercent = gaussianWidthAtFraction(sigma = 0.15, fraction = 0.01),
                expectedSnr = 120.0,
                expectedOverlap = OverlapStatus.ISOLATED,
            ),
            SyntheticPeakTruth(
                center = 8.0,
                height = 60.0,
                sigma = 0.30,
                expectedArea = gaussianArea(height = 60.0, sigma = 0.30),
                expectedFwhm = gaussianFwhm(sigma = 0.30),
                expectedBaseWidthAtOnePercent = gaussianWidthAtFraction(sigma = 0.30, fraction = 0.01),
                expectedSnr = 60.0,
                expectedOverlap = OverlapStatus.ISOLATED,
            ),
        ),
        noise = { index, time ->
            if (time < 1.5 || time > 10.5) {
                if (index % 2 == 0) 1.0 else -1.0
            } else {
                0.0
            }
        },
        params = CalculationParams(
            smoothingEnabled = false,
            baselineMethod = "NONE",
            minPeakHeight = 30.0,
            minPeakProminence = 25.0,
            minPeakDistance = 80,
            minPeakWidth = 3,
            minSnr = 0.0,
            noiseMethod = "PEAK_TO_PEAK",
            boundaryMethod = "PERCENT_HEIGHT",
            boundaryPercentHeight = 0.01,
            integrationMethod = "TRAPEZOIDAL",
            clampNegative = false,
        ),
    )

    val linearBaselinePeak = SyntheticCalculationFixture(
        id = "linear-baseline-gaussian-peak",
        pointCount = 601,
        timeStep = 0.02,
        baseline = { time -> 10.0 + time },
        peaks = listOf(
            SyntheticPeakTruth(
                center = 6.0,
                height = 100.0,
                sigma = 0.25,
                expectedArea = gaussianArea(height = 100.0, sigma = 0.25),
                expectedFwhm = gaussianFwhm(sigma = 0.25),
                expectedBaseWidthAtOnePercent = gaussianWidthAtFraction(sigma = 0.25, fraction = 0.01),
                expectedSnr = null,
                expectedOverlap = OverlapStatus.ISOLATED,
            ),
        ),
        params = CalculationParams(
            smoothingEnabled = false,
            baselineMethod = "MANUAL_LINEAR",
            minPeakHeight = 40.0,
            minPeakProminence = 35.0,
            minPeakDistance = 20,
            minPeakWidth = 3,
            minSnr = 0.0,
            noiseMethod = "RMS",
            boundaryMethod = "PERCENT_HEIGHT",
            boundaryPercentHeight = 0.01,
            integrationMethod = "TRAPEZOIDAL",
            clampNegative = false,
        ),
    )

    private fun gaussianArea(height: Double, sigma: Double): Double =
        height * sigma * sqrt(2.0 * PI)

    private fun gaussianFwhm(sigma: Double): Double =
        2.0 * sqrt(2.0 * ln(2.0)) * sigma

    private fun gaussianWidthAtFraction(sigma: Double, fraction: Double): Double =
        2.0 * sigma * sqrt(2.0 * ln(1.0 / fraction))
}

internal data class SyntheticCalculationFixture(
    val id: String,
    val pointCount: Int,
    val timeStep: Double,
    val peaks: List<SyntheticPeakTruth>,
    val baseline: (time: Double) -> Double = { 0.0 },
    val noise: (index: Int, time: Double) -> Double = { _, _ -> 0.0 },
    val params: CalculationParams,
) {
    fun signal(): DigitalSignal {
        val points = (0 until pointCount).map { index ->
            val time = index * timeStep
            val intensity = baseline(time) + peaks.sumOf { it.valueAt(time) } + noise(index, time)
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
                sourceImage = id,
                totalPoints = pointCount,
                duplicatesRemoved = 0,
                gapCount = 0,
                sortValid = true,
                timestamp = 0L,
            ),
        )
    }
}

internal data class SyntheticPeakTruth(
    val center: Double,
    val height: Double,
    val sigma: Double,
    val expectedArea: Double,
    val expectedFwhm: Double,
    val expectedBaseWidthAtOnePercent: Double,
    val expectedSnr: Double?,
    val expectedOverlap: OverlapStatus,
) {
    fun valueAt(time: Double): Double =
        height * exp(-0.5 * ((time - center) / sigma) * ((time - center) / sigma))
}

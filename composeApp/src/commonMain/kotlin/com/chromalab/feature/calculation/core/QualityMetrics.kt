package com.chromalab.feature.calculation.core

import kotlin.math.*

/**
 * Quality metrics for Phase 2 calculation pipeline (§2.35).
 *
 * Measures pipeline accuracy against known ground truth.
 * All metrics are pure functions — deterministic, no side effects.
 *
 * Metrics:
 * 1. RT: absolute and relative error
 * 2. Height/Area/Width: relative error
 * 3. S/N: relative difference
 * 4. Detection: precision, recall, F1
 * 5. Baseline: RMSE, area sensitivity
 * 6. Repeatability: determinism pass/fail
 */

// ─── Individual peak accuracy ───────────────────────────────────

data class PeakAccuracy(
    val rtAbsoluteError: Double,
    val rtRelativeError: Double,
    val heightRelativeError: Double,
    val areaRelativeError: Double,
    val widthRelativeError: Double,
    val snrRelativeDifference: Double,
) {
    val isAcceptable: Boolean
        get() = rtRelativeError < 0.05 &&
            heightRelativeError < 0.10 &&
            areaRelativeError < 0.10 &&
            widthRelativeError < 0.15
}

object PeakAccuracyCalculator {

    fun calculate(
        trueRt: Double, detectedRt: Double,
        trueHeight: Double, detectedHeight: Double,
        trueArea: Double, detectedArea: Double,
        trueWidth: Double, detectedWidth: Double,
        trueSnr: Double?, detectedSnr: Double?,
    ): PeakAccuracy = PeakAccuracy(
        rtAbsoluteError = abs(detectedRt - trueRt),
        rtRelativeError = if (trueRt != 0.0) abs(detectedRt - trueRt) / abs(trueRt) else 0.0,
        heightRelativeError = relativeError(trueHeight, detectedHeight),
        areaRelativeError = relativeError(trueArea, detectedArea),
        widthRelativeError = relativeError(trueWidth, detectedWidth),
        snrRelativeDifference = if (trueSnr != null && detectedSnr != null)
            relativeError(trueSnr, detectedSnr) else 0.0,
    )

    private fun relativeError(truth: Double, detected: Double): Double =
        if (truth != 0.0) abs(detected - truth) / abs(truth) else 0.0
}

// ─── Detection quality ──────────────────────────────────────────

data class DetectionMetrics(
    val truePositives: Int,
    val falsePositives: Int,
    val falseNegatives: Int,
) {
    val precision: Double
        get() = if (truePositives + falsePositives > 0)
            truePositives.toDouble() / (truePositives + falsePositives) else 1.0

    val recall: Double
        get() = if (truePositives + falseNegatives > 0)
            truePositives.toDouble() / (truePositives + falseNegatives) else 1.0

    val f1Score: Double
        get() = if (precision + recall > 0)
            2.0 * precision * recall / (precision + recall) else 0.0

    val isAcceptable: Boolean
        get() = precision >= 0.90 && recall >= 0.90
}

object DetectionMetricsCalculator {

    /**
     * Match detected peaks to ground truth peaks by RT proximity.
     *
     * @param trueRts Ground truth retention times
     * @param detectedRts Detected retention times
     * @param rtTolerance Maximum RT difference for a match
     */
    fun calculate(
        trueRts: List<Double>,
        detectedRts: List<Double>,
        rtTolerance: Double,
    ): DetectionMetrics {
        val matched = mutableSetOf<Int>()
        var tp = 0

        for (trueRt in trueRts) {
            val bestMatch = detectedRts.indices
                .filter { it !in matched }
                .minByOrNull { abs(detectedRts[it] - trueRt) }

            if (bestMatch != null && abs(detectedRts[bestMatch] - trueRt) <= rtTolerance) {
                tp++
                matched.add(bestMatch)
            }
        }

        return DetectionMetrics(
            truePositives = tp,
            falsePositives = detectedRts.size - tp,
            falseNegatives = trueRts.size - tp,
        )
    }
}

// ─── Baseline quality ───────────────────────────────────────────

data class BaselineMetrics(
    val rmse: Double,
    val maxError: Double,
    val areaSensitivity: Double,
) {
    val isAcceptable: Boolean
        get() = rmse < 5.0 && areaSensitivity < 0.10
}

object BaselineMetricsCalculator {

    /**
     * Compare estimated baseline to true baseline.
     */
    fun calculate(
        trueBaseline: DoubleArray,
        estimatedBaseline: DoubleArray,
        areaWithBaseline: Double,
        areaWithoutBaseline: Double,
    ): BaselineMetrics {
        val n = minOf(trueBaseline.size, estimatedBaseline.size)
        var sumSq = 0.0
        var maxErr = 0.0

        for (i in 0 until n) {
            val err = abs(trueBaseline[i] - estimatedBaseline[i])
            sumSq += err * err
            if (err > maxErr) maxErr = err
        }

        val rmse = sqrt(sumSq / n)
        val areaSens = if (areaWithoutBaseline != 0.0)
            abs(areaWithBaseline - areaWithoutBaseline) / abs(areaWithoutBaseline) else 0.0

        return BaselineMetrics(rmse, maxErr, areaSens)
    }
}

// ─── Repeatability ──────────────────────────────────────────────

data class RepeatabilityResult(
    val runCount: Int,
    val allIdentical: Boolean,
    val maxRtDeviation: Double,
    val maxAreaDeviation: Double,
) {
    val isAcceptable: Boolean
        get() = allIdentical || (maxRtDeviation < 1e-10 && maxAreaDeviation < 1e-10)
}

object RepeatabilityCalculator {

    /**
     * Run pipeline N times with same input, compare outputs.
     */
    fun <T> check(
        runs: Int = 3,
        pipeline: () -> T,
        extract: (T) -> Pair<List<Double>, List<Double>>, // (rts, areas)
    ): RepeatabilityResult {
        val results = (0 until runs).map { pipeline() }
        val pairs = results.map { extract(it) }

        val firstRts = pairs[0].first
        val firstAreas = pairs[0].second

        var maxRtDev = 0.0
        var maxAreaDev = 0.0
        var allSame = true

        for (i in 1 until runs) {
            val rts = pairs[i].first
            val areas = pairs[i].second
            if (rts.size != firstRts.size || areas.size != firstAreas.size) {
                allSame = false
                continue
            }
            rts.zip(firstRts).forEach { (a, b) ->
                val dev = abs(a - b)
                if (dev > maxRtDev) maxRtDev = dev
                if (dev > 0) allSame = false
            }
            areas.zip(firstAreas).forEach { (a, b) ->
                val dev = abs(a - b)
                if (dev > maxAreaDev) maxAreaDev = dev
                if (dev > 0) allSame = false
            }
        }

        return RepeatabilityResult(runs, allSame, maxRtDev, maxAreaDev)
    }
}

// ─── Full pipeline report ───────────────────────────────────────

data class QualityReport(
    val peakAccuracies: List<PeakAccuracy>,
    val detectionMetrics: DetectionMetrics,
    val baselineMetrics: BaselineMetrics?,
    val repeatability: RepeatabilityResult?,
) {
    val overallGrade: QualityGrade
        get() {
            val detOk = detectionMetrics.isAcceptable
            val peaksOk = peakAccuracies.all { it.isAcceptable }
            val baseOk = baselineMetrics?.isAcceptable ?: true
            val repOk = repeatability?.isAcceptable ?: true

            return when {
                detOk && peaksOk && baseOk && repOk -> QualityGrade.EXCELLENT
                detOk && baseOk -> QualityGrade.GOOD
                detectionMetrics.f1Score >= 0.7 -> QualityGrade.ACCEPTABLE
                else -> QualityGrade.POOR
            }
        }

    fun toSummary(): String = buildString {
        appendLine("═══ Quality Report ═══")
        appendLine("Detection: P=${detectionMetrics.precision.fmt()}, R=${detectionMetrics.recall.fmt()}, F1=${detectionMetrics.f1Score.fmt()}")
        appendLine("  TP=${detectionMetrics.truePositives}, FP=${detectionMetrics.falsePositives}, FN=${detectionMetrics.falseNegatives}")
        if (peakAccuracies.isNotEmpty()) {
            val avgRtErr = peakAccuracies.map { it.rtAbsoluteError }.average()
            val avgAreaErr = peakAccuracies.map { it.areaRelativeError }.average()
            appendLine("Peak accuracy: avg RT err=${avgRtErr.fmt()}, avg area err=${(avgAreaErr * 100).fmt()}%")
        }
        baselineMetrics?.let { appendLine("Baseline: RMSE=${it.rmse.fmt()}, area sensitivity=${(it.areaSensitivity * 100).fmt()}%") }
        repeatability?.let { appendLine("Repeatability: ${if (it.allIdentical) "PASS" else "FAIL"} (${it.runCount} runs)") }
        appendLine("Grade: $overallGrade")
    }

    private fun Double.fmt(): String = "%.4f".format(this)
}

enum class QualityGrade {
    EXCELLENT,
    GOOD,
    ACCEPTABLE,
    POOR,
}

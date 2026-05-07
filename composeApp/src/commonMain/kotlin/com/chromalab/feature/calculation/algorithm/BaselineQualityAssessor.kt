package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.SignalPoint
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Baseline quality assessment (§2.13).
 *
 * Pure function: takes signal + baseline → quality report.
 *
 * Metrics:
 * - residualRMS in noise regions
 * - baselineAboveSignalFraction
 * - negativeCorrectedFraction
 * - baselineCrossingCount
 * - areaSensitivityToBaseline
 * - overall grade: good / acceptable / risky / failed
 */
object BaselineQualityAssessor {

    /**
     * Assess the quality of a baseline estimate.
     */
    fun assess(
        points: List<SignalPoint>,
        baseline: List<Double>,
    ): BaselineQualityReport {
        require(points.size == baseline.size) {
            "Points (${points.size}) and baseline (${baseline.size}) must have same size"
        }

        val n = points.size
        if (n == 0) return BaselineQualityReport.empty()

        val intensities = points.map { it.intensity }
        val corrected = intensities.mapIndexed { i, y -> y - baseline[i] }

        // 1. Residual RMS in noise regions (first/last 10% of signal)
        val noiseSize = maxOf(3, n / 10)
        val noiseIndices = (0 until noiseSize) + (n - noiseSize until n)
        val noiseResiduals = noiseIndices.map { corrected[it] }
        val residualRms = if (noiseResiduals.isNotEmpty()) {
            sqrt(noiseResiduals.sumOf { it * it } / noiseResiduals.size)
        } else 0.0

        // 2. Baseline above signal fraction
        val aboveCount = (0 until n).count { baseline[it] > intensities[it] }
        val baselineAboveSignalFraction = aboveCount.toDouble() / n

        // 3. Negative corrected fraction
        val negativeCount = corrected.count { it < 0.0 }
        val negativeCorrectedFraction = negativeCount.toDouble() / n

        // 4. Baseline crossing count (sign changes in corrected signal)
        var crossingCount = 0
        for (i in 1 until n) {
            if ((corrected[i] >= 0.0) != (corrected[i - 1] >= 0.0)) {
                crossingCount++
            }
        }

        // 5. Area sensitivity: how much baseline changes total area
        val rawArea = intensities.sum()
        val correctedArea = corrected.sumOf { maxOf(0.0, it) }
        val areaSensitivity = if (rawArea > 0.0) {
            1.0 - (correctedArea / rawArea)
        } else 0.0

        // Overall grade
        val grade = computeGrade(
            baselineAboveSignalFraction,
            negativeCorrectedFraction,
            crossingCount,
            n,
            areaSensitivity,
        )

        return BaselineQualityReport(
            residualRms = residualRms,
            baselineAboveSignalFraction = baselineAboveSignalFraction,
            negativeCorrectedFraction = negativeCorrectedFraction,
            baselineCrossingCount = crossingCount,
            areaSensitivity = areaSensitivity,
            grade = grade,
        )
    }

    /**
     * Compare multiple baseline methods and warn if areas differ significantly.
     *
     * @return List of warnings about cross-method disagreements
     */
    fun compareMethodAreas(
        points: List<SignalPoint>,
        results: Map<BaselineMethod, BaselineResult>,
    ): List<String> {
        val warnings = mutableListOf<String>()
        val intensities = points.map { it.intensity }
        val rawArea = intensities.sum()
        if (rawArea <= 0.0) return warnings

        val areas = results.mapValues { (_, result) ->
            if (result.baseline.size == points.size) {
                result.baseline.indices.sumOf { i ->
                    maxOf(0.0, intensities[i] - result.baseline[i])
                }
            } else rawArea
        }

        val areaValues = areas.values.toList()
        if (areaValues.size < 2) return warnings

        val maxArea = areaValues.max()
        val minArea = areaValues.min()
        val spread = if (maxArea > 0) (maxArea - minArea) / maxArea else 0.0

        if (spread > 0.3) {
            val details = areas.entries.joinToString(", ") { (method, area) ->
                "${method.label}: ${area.toLong()}"
            }
            warnings.add(
                "Разные методы baseline дают площади с разбросом ${(spread * 100).toInt()}%: $details"
            )
        }

        return warnings
    }

    private fun computeGrade(
        aboveFraction: Double,
        negativeFraction: Double,
        crossings: Int,
        pointCount: Int,
        areaSensitivity: Double,
    ): BaselineGrade {
        // Failed: clearly broken baseline
        if (aboveFraction > 0.5 || negativeFraction > 0.5) return BaselineGrade.FAILED
        if (areaSensitivity > 0.9) return BaselineGrade.FAILED

        // Risky: questionable baseline
        if (aboveFraction > 0.2 || negativeFraction > 0.3) return BaselineGrade.RISKY
        if (areaSensitivity > 0.7) return BaselineGrade.RISKY
        val crossingRate = crossings.toDouble() / maxOf(pointCount, 1)
        if (crossingRate > 0.1) return BaselineGrade.RISKY

        // Acceptable: minor issues
        if (aboveFraction > 0.05 || negativeFraction > 0.1) return BaselineGrade.ACCEPTABLE
        if (areaSensitivity > 0.5) return BaselineGrade.ACCEPTABLE

        return BaselineGrade.GOOD
    }
}

// ─── Data classes ───────────────────────────────────────────────

enum class BaselineGrade(val label: String) {
    GOOD("Хорошо"),
    ACCEPTABLE("Приемлемо"),
    RISKY("Рискованно"),
    FAILED("Ошибка"),
}

data class BaselineQualityReport(
    val residualRms: Double,
    val baselineAboveSignalFraction: Double,
    val negativeCorrectedFraction: Double,
    val baselineCrossingCount: Int,
    val areaSensitivity: Double,
    val grade: BaselineGrade,
) {
    companion object {
        fun empty() = BaselineQualityReport(
            residualRms = 0.0,
            baselineAboveSignalFraction = 0.0,
            negativeCorrectedFraction = 0.0,
            baselineCrossingCount = 0,
            areaSensitivity = 0.0,
            grade = BaselineGrade.GOOD,
        )
    }
}

package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.PeakResult
import com.chromalab.feature.calculation.core.SignalBundle
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Method quality analyzer (§3.3).
 *
 * Evaluates chromatographic separation quality:
 * - Mean theoretical plates (N̄) — column efficiency
 * - Peak capacity (nc) — maximum separable peaks
 * - Capacity usage — how "full" the chromatogram is
 * - Mean resolution (R̄s) — separation quality
 * - Poor resolution % — fraction with Rs < 1.5
 * - Mean tailing (T̄) — peak symmetry quality
 * - Poor tailing % — fraction with T > 2.0
 * - Baseline drift — detector stability
 * - Baseline noise (RMS) — sensitivity
 * - Overall grade: EXCELLENT / GOOD / ACCEPTABLE / POOR
 *
 * Pure function: deterministic, no side effects.
 */
object MethodQualityAnalyzer {

    /**
     * Evaluate chromatographic method quality.
     *
     * @param peaks Detected peaks with USP metrics (tailing, plates, resolution)
     * @param signals Optional signal bundle for baseline analysis
     * @return MethodQualityResult with grade and all metrics, or null if no peaks
     */
    fun analyze(peaks: List<PeakResult>, signals: SignalBundle? = null): MethodQualityResult? {
        if (peaks.isEmpty()) return null

        val sorted = peaks.sortedBy { it.rtApex }

        // ─── Theoretical plates ──────────────────────────────────
        val plateCounts = peaks.mapNotNull { it.plateCount }
        val meanPlatesValue = if (plateCounts.isNotEmpty()) plateCounts.average() else 0.0
        val minPlates = plateCounts.minOrNull() ?: 0
        val maxPlates = plateCounts.maxOrNull() ?: 0
        val meanPlatesInterp = when {
            meanPlatesValue >= 10_000 -> "Высокая эффективность колонки"
            meanPlatesValue >= 5_000 -> "Хорошая эффективность колонки"
            meanPlatesValue >= 2_000 -> "Удовлетворительная эффективность"
            meanPlatesValue > 0 -> "Низкая эффективность колонки — рассмотрите замену или оптимизацию"
            else -> "Данные о тарелках отсутствуют"
        }
        val meanPlates = MetricValue(
            name = "Средние теор. тарелки (N̄)",
            value = meanPlatesValue,
            formatted = "%,.0f".format(meanPlatesValue),
            unit = "",
            interpretation = meanPlatesInterp,
        )

        // ─── Peak capacity ───────────────────────────────────────
        // nc = 1 + (√N̄ / 4) × ln(t_last / t_first)
        val rtFirst = sorted.first().rtApex
        val rtLast = sorted.last().rtApex
        val peakCapacityValue = if (meanPlatesValue > 0 && rtFirst > 0 && rtLast > rtFirst) {
            1.0 + (sqrt(meanPlatesValue) / 4.0) * ln(rtLast / rtFirst)
        } else {
            peaks.size.toDouble()
        }
        val peakCapacity = MetricValue(
            name = "Пиковая ёмкость (nc)",
            value = peakCapacityValue,
            formatted = "%.1f".format(peakCapacityValue),
            unit = "",
            interpretation = "Максимальное число пиков, которые метод может разделить",
        )

        // ─── Capacity usage ──────────────────────────────────────
        val capacityUsageValue = if (peakCapacityValue > 0) {
            (peaks.size.toDouble() / peakCapacityValue * 100.0).coerceAtMost(100.0)
        } else 0.0
        val capacityUsageInterp = when {
            capacityUsageValue > 80 -> "Хроматограмма перегружена — вероятны перекрытия"
            capacityUsageValue > 60 -> "Высокая загрузка — метод близок к пределу"
            capacityUsageValue > 30 -> "Оптимальная загрузка"
            else -> "Низкая загрузка — метод имеет запас по разделению"
        }
        val capacityUsage = MetricValue(
            name = "Использование ёмкости",
            value = capacityUsageValue,
            formatted = "%.1f".format(capacityUsageValue),
            unit = "%",
            interpretation = capacityUsageInterp,
        )

        // ─── Resolution ──────────────────────────────────────────
        val resolutions = peaks.mapNotNull { it.resolution }
        val meanRsValue = if (resolutions.isNotEmpty()) resolutions.average() else 0.0
        val poorRsCount = resolutions.count { it < 1.5 }
        val poorRsPct = if (resolutions.isNotEmpty()) poorRsCount.toDouble() / resolutions.size * 100.0 else 0.0

        val meanRsInterp = when {
            resolutions.isEmpty() -> "Данные о разрешении отсутствуют"
            meanRsValue >= 2.0 -> "Отличное разделение — все пики базово разрешены"
            meanRsValue >= 1.5 -> "Хорошее разделение"
            meanRsValue >= 1.0 -> "Удовлетворительное разделение — есть частичные перекрытия"
            else -> "Плохое разделение — значительные перекрытия"
        }
        val meanResolution = MetricValue(
            name = "Средняя Rs",
            value = meanRsValue,
            formatted = "%.2f".format(meanRsValue),
            unit = "",
            interpretation = meanRsInterp,
        )
        val poorResolutionPercent = MetricValue(
            name = "Пики с Rs < 1.5",
            value = poorRsPct,
            formatted = "%.0f".format(poorRsPct),
            unit = "%",
            interpretation = if (poorRsCount > 0) "$poorRsCount из ${resolutions.size} пар недоразделены" else "Все пары хорошо разделены",
        )

        // ─── Tailing ─────────────────────────────────────────────
        val tailings = peaks.map { it.tailingFactor }
        val meanTailingValue = tailings.average()
        val poorTCount = tailings.count { it > 2.0 }
        val poorTPct = poorTCount.toDouble() / peaks.size * 100.0

        val meanTailingInterp = when {
            meanTailingValue <= 1.2 -> "Отличная симметрия пиков"
            meanTailingValue <= 1.5 -> "Хорошая симметрия"
            meanTailingValue <= 2.0 -> "Умеренное хвостование — рассмотрите оптимизацию"
            else -> "Значительное хвостование — проблема с колонкой или пробой"
        }
        val meanTailing = MetricValue(
            name = "Средний Tailing (T̄)",
            value = meanTailingValue,
            formatted = "%.3f".format(meanTailingValue),
            unit = "",
            interpretation = meanTailingInterp,
        )
        val poorTailingPercent = MetricValue(
            name = "Пики с T > 2.0",
            value = poorTPct,
            formatted = "%.0f".format(poorTPct),
            unit = "%",
            interpretation = if (poorTCount > 0) "$poorTCount из ${peaks.size} пиков с проблемным хвостованием" else "Все пики в пределах нормы",
        )

        // ─── Baseline metrics ────────────────────────────────────
        val baselineDriftResult = analyzeBaselineDrift(signals)
        val baselineNoiseResult = analyzeBaselineNoise(signals)

        // ─── Overall grade ───────────────────────────────────────
        val (grade, gradeReason) = calculateGrade(
            meanPlatesValue = meanPlatesValue,
            meanRsValue = meanRsValue,
            meanTailingValue = meanTailingValue,
            capacityUsageValue = capacityUsageValue,
            poorRsPct = poorRsPct,
            poorTPct = poorTPct,
            hasPlateData = plateCounts.isNotEmpty(),
            hasRsData = resolutions.isNotEmpty(),
        )

        return MethodQualityResult(
            grade = grade,
            gradeReason = gradeReason,
            meanPlates = meanPlates,
            minMaxPlates = minPlates to maxPlates,
            peakCapacity = peakCapacity,
            capacityUsage = capacityUsage,
            meanResolution = meanResolution,
            poorResolutionPercent = poorResolutionPercent,
            meanTailing = meanTailing,
            poorTailingPercent = poorTailingPercent,
            baselineDrift = baselineDriftResult,
            baselineNoise = baselineNoiseResult,
        )
    }

    // ─── Baseline drift ──────────────────────────────────────────

    private fun analyzeBaselineDrift(signals: SignalBundle?): MetricValue {
        if (signals?.baseline == null || signals.baseline.size < 2) {
            return MetricValue("Drift baseline", 0.0, "—", "", "Данные baseline недоступны")
        }
        val bl = signals.baseline
        val first = bl.first()
        val last = bl.last()
        val range = bl.max() - bl.min()
        val driftValue = if (range > 0) kotlin.math.abs(last - first) / range else 0.0

        val interp = when {
            driftValue < 0.05 -> "Стабильный baseline — отличная детекция"
            driftValue < 0.15 -> "Небольшой drift — приемлемо"
            driftValue < 0.30 -> "Заметный drift — рекомендуется коррекция"
            else -> "Значительный drift — возможны артефакты"
        }
        return MetricValue(
            name = "Drift baseline",
            value = driftValue,
            formatted = "%.1f".format(driftValue * 100),
            unit = "%",
            interpretation = interp,
        )
    }

    // ─── Baseline noise ──────────────────────────────────────────

    private fun analyzeBaselineNoise(signals: SignalBundle?): MetricValue {
        if (signals?.raw == null || signals.raw.size < 10) {
            return MetricValue("Шум baseline (RMS)", 0.0, "—", "", "Данные недоступны")
        }

        // Estimate noise from first 10% of signal (assuming low-signal region)
        val pts = signals.raw
        val windowSize = (pts.size * 0.1).toInt().coerceAtLeast(10)
        val window = pts.take(windowSize).map { it.intensity }
        val mean = window.average()
        val rms = sqrt(window.map { (it - mean) * (it - mean) }.average())

        val signalRange = pts.maxOf { it.intensity } - pts.minOf { it.intensity }
        val noisePct = if (signalRange > 0) rms / signalRange * 100.0 else 0.0

        val interp = when {
            noisePct < 0.5 -> "Очень низкий шум — высокая чувствительность"
            noisePct < 2.0 -> "Нормальный уровень шума"
            noisePct < 5.0 -> "Повышенный шум — возможно влияние на малые пики"
            else -> "Высокий шум — рекомендуется фильтрация или повторное измерение"
        }
        return MetricValue(
            name = "Шум baseline (RMS)",
            value = noisePct,
            formatted = "%.2f".format(noisePct),
            unit = "%",
            interpretation = interp,
        )
    }

    // ─── Grade calculation ───────────────────────────────────────

    private fun calculateGrade(
        meanPlatesValue: Double,
        meanRsValue: Double,
        meanTailingValue: Double,
        capacityUsageValue: Double,
        poorRsPct: Double,
        poorTPct: Double,
        hasPlateData: Boolean,
        hasRsData: Boolean,
    ): Pair<MethodGrade, String> {
        // Score-based grading
        var score = 0
        val reasons = mutableListOf<String>()

        // Plates
        if (hasPlateData) {
            when {
                meanPlatesValue >= 10_000 -> { score += 3; reasons.add("N̄≥10K") }
                meanPlatesValue >= 5_000 -> { score += 2; reasons.add("N̄≥5K") }
                meanPlatesValue >= 2_000 -> { score += 1; reasons.add("N̄≥2K") }
                else -> reasons.add("N̄<2K")
            }
        }

        // Resolution
        if (hasRsData) {
            when {
                meanRsValue >= 2.0 -> { score += 3; reasons.add("Rs≥2.0") }
                meanRsValue >= 1.5 -> { score += 2; reasons.add("Rs≥1.5") }
                meanRsValue >= 1.0 -> { score += 1; reasons.add("Rs≥1.0") }
                else -> reasons.add("Rs<1.0")
            }
        }

        // Tailing
        when {
            meanTailingValue <= 1.3 -> { score += 2; reasons.add("T̄≤1.3") }
            meanTailingValue <= 1.5 -> { score += 1; reasons.add("T̄≤1.5") }
            meanTailingValue <= 2.0 -> reasons.add("T̄≤2.0")
            else -> { score -= 1; reasons.add("T̄>2.0") }
        }

        // Capacity
        if (capacityUsageValue < 60) { score += 1; reasons.add("cap<60%") }
        else if (capacityUsageValue > 80) { score -= 1; reasons.add("cap>80%") }

        val grade = when {
            score >= 7 -> MethodGrade.EXCELLENT
            score >= 4 -> MethodGrade.GOOD
            score >= 2 -> MethodGrade.ACCEPTABLE
            else -> MethodGrade.POOR
        }

        return grade to reasons.joinToString(" · ")
    }
}

// ─── Data models ────────────────────────────────────────────────

enum class MethodGrade {
    EXCELLENT,
    GOOD,
    ACCEPTABLE,
    POOR,
}

/**
 * Full method quality analysis result.
 */
data class MethodQualityResult(
    val grade: MethodGrade,
    val gradeReason: String,
    val meanPlates: MetricValue,
    val minMaxPlates: Pair<Int, Int>,
    val peakCapacity: MetricValue,
    val capacityUsage: MetricValue,
    val meanResolution: MetricValue,
    val poorResolutionPercent: MetricValue,
    val meanTailing: MetricValue,
    val poorTailingPercent: MetricValue,
    val baselineDrift: MetricValue,
    val baselineNoise: MetricValue,
) {
    /** All metrics as ordered list for iteration. */
    val allMetrics: List<MetricValue>
        get() = listOf(
            meanPlates,
            peakCapacity,
            capacityUsage,
            meanResolution,
            poorResolutionPercent,
            meanTailing,
            poorTailingPercent,
            baselineDrift,
            baselineNoise,
        )
}

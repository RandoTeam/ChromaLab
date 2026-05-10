package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.PeakResult
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Distribution statistics analyzer (§3.1).
 *
 * Calculates statistical properties of the peak area distribution:
 * - Dominance index: how much the largest peak dominates
 * - Shannon diversity: compositional complexity
 * - Pielou evenness: how uniform the distribution is
 * - Weighted mean RT: center of mass by area
 * - Median RT: RT at 50% cumulative area
 * - Skewness: asymmetry of the distribution
 * - Kurtosis: peakedness of the distribution
 * - Elution range: time window covered
 * - Peak density: peaks per minute
 *
 * All metrics include human-readable interpretations.
 * Pure function: deterministic, no side effects.
 */
object DistributionAnalyzer {

    /**
     * Analyze peak area distribution.
     *
     * @param peaks List of detected peaks (at least 1)
     * @return DistributionResult with all metrics and interpretations,
     *         or null if peaks list is empty
     */
    fun analyze(peaks: List<PeakResult>): DistributionResult? {
        if (peaks.isEmpty()) return null

        val areas = peaks.map { it.area }
        val rts = peaks.map { it.rtApex }
        val totalArea = areas.sum()
        if (totalArea <= 0.0) return null

        val n = peaks.size

        // ─── Dominant peak ───────────────────────────────────────
        val maxIdx = areas.indices.maxBy { areas[it] }
        val maxArea = areas[maxIdx]
        val dominantPeak = DominantPeakInfo(
            peakIndex = maxIdx,
            peakId = peaks[maxIdx].peakId,
            rtApex = rts[maxIdx],
            area = maxArea,
            areaPercent = maxArea / totalArea * 100.0,
        )

        // ─── Dominance index ─────────────────────────────────────
        val dominanceValue = maxArea / totalArea
        val dominanceInterp = when {
            dominanceValue > 0.7 -> "Один компонент резко преобладает (>70%)"
            dominanceValue > 0.5 -> "Монокомпонентная смесь — один доминант (>50%)"
            dominanceValue > 0.3 -> "Умеренное доминирование одного компонента"
            dominanceValue > 0.15 -> "Относительно равномерное распределение"
            else -> "Мультикомпонентная смесь — нет выраженного доминанта"
        }
        val dominanceIndex = MetricValue(
            name = "Индекс доминирования",
            value = dominanceValue,
            formatted = "%.3f".format(dominanceValue),
            unit = "",
            interpretation = dominanceInterp,
        )

        // ─── Shannon diversity index ─────────────────────────────
        // H = −Σ(pᵢ · ln(pᵢ)), where pᵢ = Areaᵢ / ΣArea
        val shannonValue = if (n == 1) 0.0 else {
            -areas.sumOf { a ->
                val p = a / totalArea
                if (p > 0) p * ln(p) else 0.0
            }
        }
        val shannonInterp = when {
            n == 1 -> "Один пик — разнообразие не определяется"
            shannonValue > 2.5 -> "Очень высокое разнообразие — сложная многокомпонентная смесь"
            shannonValue > 2.0 -> "Высокое разнообразие состава"
            shannonValue > 1.5 -> "Умеренное разнообразие"
            shannonValue > 1.0 -> "Низкое разнообразие — несколько преобладающих компонентов"
            else -> "Очень низкое разнообразие — 1-2 основных компонента"
        }
        val shannonIndex = MetricValue(
            name = "Индекс Шеннона (H')",
            value = shannonValue,
            formatted = "%.3f".format(shannonValue),
            unit = "",
            interpretation = shannonInterp,
        )

        // ─── Pielou evenness ─────────────────────────────────────
        // J = H / ln(N), where N = number of peaks
        val pielouValue = if (n <= 1) 0.0 else shannonValue / ln(n.toDouble())
        val pielouInterp = when {
            n <= 1 -> "Один пик — равномерность не определяется"
            pielouValue > 0.9 -> "Очень равномерное распределение — все пики примерно одинаковые"
            pielouValue > 0.7 -> "Равномерное распределение"
            pielouValue > 0.5 -> "Умеренная неравномерность"
            pielouValue > 0.3 -> "Неравномерное распределение — есть доминанты"
            else -> "Сильно неравномерное — 1-2 пика значительно преобладают"
        }
        val pielouEvenness = MetricValue(
            name = "Равномерность Пиелу (J)",
            value = pielouValue,
            formatted = "%.3f".format(pielouValue),
            unit = "",
            interpretation = pielouInterp,
        )

        // ─── Weighted mean RT ────────────────────────────────────
        // RT̄ = Σ(RTᵢ × Areaᵢ) / Σ Area
        val weightedMeanRtValue = rts.zip(areas).sumOf { (rt, a) -> rt * a } / totalArea
        val weightedMeanRt = MetricValue(
            name = "Средневзвешенный RT",
            value = weightedMeanRtValue,
            formatted = "%.3f".format(weightedMeanRtValue),
            unit = "мин",
            interpretation = "Центр масс хроматограммы по площади",
        )

        // ─── Median RT ───────────────────────────────────────────
        // RT at which cumulative area reaches 50%
        val sortedByRt = peaks.sortedBy { it.rtApex }
        var cumArea = 0.0
        val halfArea = totalArea / 2.0
        var medianRtValue = rts.first()
        for (peak in sortedByRt) {
            cumArea += peak.area
            if (cumArea >= halfArea) {
                medianRtValue = peak.rtApex
                break
            }
        }
        val medianRt = MetricValue(
            name = "Медианный RT",
            value = medianRtValue,
            formatted = "%.3f".format(medianRtValue),
            unit = "мин",
            interpretation = "RT, при котором кумулятивная площадь = 50%",
        )

        // ─── Skewness ────────────────────────────────────────────
        // Area-weighted skewness of RT distribution
        val skewnessValue = if (n < 3) 0.0 else {
            val meanRt = weightedMeanRtValue
            val variance = rts.zip(areas).sumOf { (rt, a) ->
                (a / totalArea) * (rt - meanRt) * (rt - meanRt)
            }
            val sd = sqrt(variance)
            if (sd > 0) {
                rts.zip(areas).sumOf { (rt, a) ->
                    (a / totalArea) * ((rt - meanRt) / sd) * ((rt - meanRt) / sd) * ((rt - meanRt) / sd)
                }
            } else 0.0
        }
        val skewnessInterp = when {
            n < 3 -> "Недостаточно пиков для определения асимметрии"
            skewnessValue > 1.0 -> "Сильное смещение к ранним фракциям (лёгкие компоненты)"
            skewnessValue > 0.5 -> "Умеренное смещение к ранним фракциям"
            skewnessValue > -0.5 -> "Симметричное распределение"
            skewnessValue > -1.0 -> "Умеренное смещение к поздним фракциям (тяжёлые компоненты)"
            else -> "Сильное смещение к поздним фракциям"
        }
        val skewness = MetricValue(
            name = "Асимметрия (Skewness)",
            value = skewnessValue,
            formatted = "%.3f".format(skewnessValue),
            unit = "",
            interpretation = skewnessInterp,
        )

        // ─── Kurtosis ────────────────────────────────────────────
        val kurtosisValue = if (n < 4) 0.0 else {
            val meanRt = weightedMeanRtValue
            val variance = rts.zip(areas).sumOf { (rt, a) ->
                (a / totalArea) * (rt - meanRt) * (rt - meanRt)
            }
            val sd = sqrt(variance)
            if (sd > 0) {
                rts.zip(areas).sumOf { (rt, a) ->
                    val z = (rt - meanRt) / sd
                    (a / totalArea) * z * z * z * z
                } - 3.0 // excess kurtosis
            } else 0.0
        }
        val kurtosisInterp = when {
            n < 4 -> "Недостаточно пиков"
            kurtosisValue > 1.0 -> "Острый пик распределения — компоненты сконцентрированы вблизи центра"
            kurtosisValue > -1.0 -> "Нормальное распределение"
            else -> "Плоское распределение — компоненты распределены широко"
        }
        val kurtosis = MetricValue(
            name = "Эксцесс (Kurtosis)",
            value = kurtosisValue,
            formatted = "%.3f".format(kurtosisValue),
            unit = "",
            interpretation = kurtosisInterp,
        )

        // ─── Elution range ───────────────────────────────────────
        val rtMin = rts.min()
        val rtMax = rts.max()
        val elutionRangeValue = rtMax - rtMin
        val elutionRange = MetricValue(
            name = "Диапазон элюирования",
            value = elutionRangeValue,
            formatted = "%.3f".format(elutionRangeValue),
            unit = "мин",
            interpretation = "Временное окно от первого до последнего пика",
        )

        // ─── Peak density ────────────────────────────────────────
        val peakDensityValue = if (elutionRangeValue > 0) n / elutionRangeValue else 0.0
        val densityInterp = when {
            elutionRangeValue <= 0 -> "Один пик — плотность не определяется"
            peakDensityValue > 2.0 -> "Очень плотное распределение — возможны перекрытия"
            peakDensityValue > 1.0 -> "Плотное распределение"
            peakDensityValue > 0.5 -> "Умеренная плотность"
            else -> "Разреженное распределение — пики хорошо разделены"
        }
        val peakDensity = MetricValue(
            name = "Плотность пиков",
            value = peakDensityValue,
            formatted = "%.2f".format(peakDensityValue),
            unit = "пиков/мин",
            interpretation = densityInterp,
        )

        return DistributionResult(
            dominantPeak = dominantPeak,
            dominanceIndex = dominanceIndex,
            shannonIndex = shannonIndex,
            pielouEvenness = pielouEvenness,
            medianRt = medianRt,
            weightedMeanRt = weightedMeanRt,
            skewness = skewness,
            kurtosis = kurtosis,
            elutionRange = elutionRange,
            peakDensity = peakDensity,
            peakCount = n,
        )
    }
}

// ─── Data models ────────────────────────────────────────────────

/**
 * A single metric with value, formatting, and human-readable interpretation.
 * Used across all extended analysis phases (13-16).
 */
data class MetricValue(
    val name: String,
    val value: Double,
    val formatted: String,
    val unit: String,
    val interpretation: String,
)

/**
 * Information about the dominant peak.
 */
data class DominantPeakInfo(
    val peakIndex: Int,
    val peakId: Int,
    val rtApex: Double,
    val area: Double,
    val areaPercent: Double,
)

/**
 * Full distribution analysis result.
 */
data class DistributionResult(
    val dominantPeak: DominantPeakInfo,
    val dominanceIndex: MetricValue,
    val shannonIndex: MetricValue,
    val pielouEvenness: MetricValue,
    val medianRt: MetricValue,
    val weightedMeanRt: MetricValue,
    val skewness: MetricValue,
    val kurtosis: MetricValue,
    val elutionRange: MetricValue,
    val peakDensity: MetricValue,
    val peakCount: Int,
) {
    /**
     * All metrics as ordered list for iteration in UI/export.
     */
    val allMetrics: List<MetricValue>
        get() = listOf(
            dominanceIndex,
            shannonIndex,
            pielouEvenness,
            weightedMeanRt,
            medianRt,
            skewness,
            kurtosis,
            elutionRange,
            peakDensity,
        )
}

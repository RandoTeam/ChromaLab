package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.PeakResult
import com.chromalab.feature.calculation.core.SignalBundle
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Pattern analyzer (§3.2).
 *
 * Detects structural patterns in the chromatogram:
 * 1. Homologous series — equally-spaced peaks (CV of ΔRT < threshold)
 * 2. Odd/even predominance — alternating area pattern within a series
 * 3. UCM (Unresolved Complex Mixture) — baseline hump under peaks
 * 4. Envelope shape — unimodal, bimodal, flat, decreasing
 * 5. Peak clustering — groups of closely-spaced peaks vs. isolated
 *
 * Pure function: deterministic, no side effects.
 */
object PatternAnalyzer {

    /**
     * Analyze peak distribution patterns.
     *
     * @param peaks Sorted list of detected peaks (at least 2 for series detection)
     * @param signals Optional signal bundle for UCM analysis
     * @return PatternResult with all pattern metrics, or null if < 2 peaks
     */
    fun analyze(peaks: List<PeakResult>, signals: SignalBundle? = null): PatternResult? {
        if (peaks.size < 2) return null

        val sorted = peaks.sortedBy { it.rtApex }

        val homologous = detectHomologousSeries(sorted)
        val oddEven = if (homologous.detected) detectOddEvenRatio(sorted) else null
        val ucm = detectUcm(sorted, signals)
        val envelope = analyzeEnvelope(sorted)
        val clusters = analyzeClusters(sorted)

        return PatternResult(
            homologousSeries = homologous,
            oddEvenRatio = oddEven,
            ucm = ucm,
            envelope = envelope,
            clusters = clusters,
        )
    }

    // ─── 1. Homologous series detection ─────────────────────────

    /**
     * Detect homologous series by checking if ΔRT intervals are regular.
     *
     * Algorithm:
     * 1. Compute ΔRT between consecutive peaks
     * 2. Calculate CV (coefficient of variation) = std(ΔRT) / mean(ΔRT)
     * 3. CV < 0.15 → HIGH confidence, CV < 0.30 → MEDIUM, else NONE
     */
    private fun detectHomologousSeries(sorted: List<PeakResult>): HomologousSeriesResult {
        if (sorted.size < 3) {
            return HomologousSeriesResult(
                detected = false,
                confidence = SeriesConfidence.NONE,
                meanDeltaRt = 0.0,
                cvDeltaRt = 1.0,
                memberCount = sorted.size,
                interpretation = "Недостаточно пиков для определения гомологического ряда (нужно ≥ 3)",
            )
        }

        val deltas = (1 until sorted.size).map { i ->
            sorted[i].rtApex - sorted[i - 1].rtApex
        }

        val meanDelta = deltas.average()
        val variance = deltas.map { (it - meanDelta) * (it - meanDelta) }.average()
        val sd = sqrt(variance)
        val cv = if (meanDelta > 0) sd / meanDelta else 1.0

        val confidence = when {
            cv < 0.15 -> SeriesConfidence.HIGH
            cv < 0.30 -> SeriesConfidence.MEDIUM
            else -> SeriesConfidence.NONE
        }

        val interpretation = when (confidence) {
            SeriesConfidence.HIGH ->
                "Обнаружен гомологический ряд (CV=${formatCv(cv)}). " +
                    "Средний интервал ΔRT=${"%.3f".format(meanDelta)} мин, ${sorted.size} членов"
            SeriesConfidence.MEDIUM ->
                "Возможный гомологический ряд (CV=${formatCv(cv)}). " +
                    "Средний ΔRT=${"%.3f".format(meanDelta)} мин — интервалы неравномерны"
            SeriesConfidence.NONE ->
                "Гомологический ряд не обнаружен (CV=${formatCv(cv)}). " +
                    "Пики расположены нерегулярно"
        }

        return HomologousSeriesResult(
            detected = confidence != SeriesConfidence.NONE,
            confidence = confidence,
            meanDeltaRt = meanDelta,
            cvDeltaRt = cv,
            memberCount = sorted.size,
            interpretation = interpretation,
        )
    }

    // ─── 2. Odd/even predominance ───────────────────────────────

    /**
     * Check for alternating area pattern (odd/even carbon preference).
     *
     * Numbering peaks as 1, 2, 3, ... and comparing:
     * ratio = Σ Area(odd-numbered) / Σ Area(even-numbered)
     */
    private fun detectOddEvenRatio(sorted: List<PeakResult>): OddEvenResult {
        var oddSum = 0.0
        var evenSum = 0.0

        sorted.forEachIndexed { i, peak ->
            if ((i + 1) % 2 == 1) oddSum += peak.area
            else evenSum += peak.area
        }

        val ratio = if (evenSum > 0) oddSum / evenSum else 0.0

        val interpretation = when {
            ratio > 1.5 -> "Выраженное нечётное преобладание (ratio=${"%.2f".format(ratio)}) — типично для незрелого наземного ОВ"
            ratio > 1.2 -> "Умеренное нечётное преобладание (ratio=${"%.2f".format(ratio)})"
            ratio > 0.8 -> "Нет выраженного чёт/нечёт преобладания (ratio=${"%.2f".format(ratio)}) — зрелое ОВ"
            ratio > 0.5 -> "Умеренное чётное преобладание (ratio=${"%.2f".format(ratio)})"
            else -> "Выраженное чётное преобладание (ratio=${"%.2f".format(ratio)})"
        }

        return OddEvenResult(
            oddAreaSum = oddSum,
            evenAreaSum = evenSum,
            ratio = ratio,
            interpretation = interpretation,
        )
    }

    // ─── 3. UCM detection ───────────────────────────────────────

    /**
     * Detect Unresolved Complex Mixture (UCM hump).
     *
     * UCM is the area between the baseline and the signal that is NOT
     * accounted for by resolved peaks. High UCM ratio indicates biodegradation.
     *
     * UCM ratio = (total_area - resolved_peaks_area) / total_area
     */
    private fun detectUcm(sorted: List<PeakResult>, signals: SignalBundle?): UcmResult {
        val resolvedArea = sorted.sumOf { it.area }

        // Estimate total area under the chromatogram from signal data
        val totalArea = if (signals?.baselineCorrected != null && signals.baselineCorrected.size >= 2) {
            val pts = signals.baselineCorrected
            var sum = 0.0
            for (i in 1 until pts.size) {
                val dt = pts[i].time - pts[i - 1].time
                val avgIntensity = (pts[i].intensity + pts[i - 1].intensity) / 2.0
                if (avgIntensity > 0) sum += avgIntensity * dt
            }
            sum
        } else {
            // Without signal data, we can't estimate UCM — return unknown
            return UcmResult(
                ucmRatio = 0.0,
                resolvedArea = resolvedArea,
                totalArea = resolvedArea,
                interpretation = "Данные сигнала недоступны для оценки UCM",
            )
        }

        val ucmRatio = if (totalArea > 0) {
            ((totalArea - resolvedArea) / totalArea).coerceIn(0.0, 1.0)
        } else 0.0

        val interpretation = when {
            ucmRatio > 0.6 -> "Сильный UCM-горб (${"%.0f".format(ucmRatio * 100)}%) — значительная биодеградация или сложная смесь"
            ucmRatio > 0.3 -> "Умеренный UCM (${"%.0f".format(ucmRatio * 100)}%) — частично разрешённая смесь"
            ucmRatio > 0.1 -> "Слабый UCM (${"%.0f".format(ucmRatio * 100)}%) — большинство пиков разрешены"
            else -> "UCM отсутствует (${"%.0f".format(ucmRatio * 100)}%) — все пики хорошо разрешены"
        }

        return UcmResult(
            ucmRatio = ucmRatio,
            resolvedArea = resolvedArea,
            totalArea = totalArea,
            interpretation = interpretation,
        )
    }

    // ─── 4. Envelope analysis ───────────────────────────────────

    /**
     * Analyze the shape of the peak area distribution envelope.
     *
     * Checks for:
     * - Unimodal: single maximum in area distribution
     * - Bimodal: two distinct maxima
     * - Flat: no clear maximum (Pielou > 0.9)
     * - Decreasing: monotonically decreasing from first peak
     */
    private fun analyzeEnvelope(sorted: List<PeakResult>): EnvelopeResult {
        val areas = sorted.map { it.area }
        val n = areas.size

        if (n <= 2) {
            return EnvelopeResult(
                shape = EnvelopeShape.UNIMODAL,
                interpretation = "Слишком мало пиков для анализа формы огибающей",
            )
        }

        // Find local maxima in area sequence
        val maxima = mutableListOf<Int>()
        for (i in 1 until n - 1) {
            if (areas[i] > areas[i - 1] && areas[i] > areas[i + 1]) {
                maxima.add(i)
            }
        }
        // Check edges
        if (areas[0] > areas[1]) maxima.add(0)
        if (areas[n - 1] > areas[n - 2]) maxima.add(n - 1)

        // Check if monotonically decreasing
        val isDecreasing = (1 until n).all { areas[it] <= areas[it - 1] * 1.1 } // 10% tolerance

        // Check if flat (high evenness)
        val maxArea = areas.max()
        val minArea = areas.min()
        val isFlat = maxArea > 0 && (minArea / maxArea) > 0.5

        val shape: EnvelopeShape
        val interpretation: String

        when {
            isFlat -> {
                shape = EnvelopeShape.FLAT
                interpretation = "Плоское распределение — все пики примерно одинаковой величины"
            }
            isDecreasing -> {
                shape = EnvelopeShape.DECREASING
                interpretation = "Убывающее распределение — максимум в начале (лёгкие фракции)"
            }
            maxima.size >= 2 -> {
                shape = EnvelopeShape.BIMODAL
                interpretation = "Бимодальное распределение — два максимума " +
                    "(RT=${"%.2f".format(sorted[maxima[0]].rtApex)} и ${"%.2f".format(sorted[maxima[1]].rtApex)} мин)"
            }
            else -> {
                shape = EnvelopeShape.UNIMODAL
                val maxIdx = areas.indices.maxBy { areas[it] }
                interpretation = "Унимодальное распределение — один максимум " +
                    "на RT=${"%.2f".format(sorted[maxIdx].rtApex)} мин"
            }
        }

        return EnvelopeResult(shape = shape, interpretation = interpretation)
    }

    // ─── 5. Peak clustering ─────────────────────────────────────

    /**
     * Identify clusters of closely-spaced peaks vs. isolated peaks.
     *
     * Algorithm:
     * 1. Compute ΔRT between consecutive peaks
     * 2. Mean ΔRT as threshold base
     * 3. If ΔRT < mean × 0.5 → peaks belong to same cluster
     * 4. Report number of clusters, sizes, isolated peaks
     */
    private fun analyzeClusters(sorted: List<PeakResult>): ClusterResult {
        if (sorted.size < 2) {
            return ClusterResult(
                clusterCount = if (sorted.isEmpty()) 0 else 1,
                isolatedCount = sorted.size,
                largestClusterSize = sorted.size,
                meanClusterSize = sorted.size.toDouble(),
                interpretation = "Один пик — кластеризация не применима",
            )
        }

        val deltas = (1 until sorted.size).map { i ->
            sorted[i].rtApex - sorted[i - 1].rtApex
        }
        val meanDelta = deltas.average()
        val threshold = meanDelta * 0.5

        // Build clusters
        val clusters = mutableListOf<Int>()
        var currentClusterSize = 1

        for (delta in deltas) {
            if (delta < threshold) {
                currentClusterSize++
            } else {
                clusters.add(currentClusterSize)
                currentClusterSize = 1
            }
        }
        clusters.add(currentClusterSize)

        val isolated = clusters.count { it == 1 }
        val realClusters = clusters.filter { it > 1 }
        val largestCluster = clusters.max()
        val meanSize = if (realClusters.isNotEmpty()) realClusters.average() else 1.0

        val interpretation = when {
            realClusters.isEmpty() ->
                "Все ${sorted.size} пиков изолированы — равномерное распределение"
            realClusters.size == 1 && isolated == 0 ->
                "Все пики образуют один кластер (${sorted.size} пиков)"
            else ->
                "${realClusters.size} кластеров (ср. ${"%,.1f".format(meanSize)} пиков), $isolated изолированных"
        }

        return ClusterResult(
            clusterCount = realClusters.size,
            isolatedCount = isolated,
            largestClusterSize = largestCluster,
            meanClusterSize = meanSize,
            interpretation = interpretation,
        )
    }

    private fun formatCv(cv: Double): String = "%.3f".format(cv)
}

// ─── Data models ────────────────────────────────────────────────

enum class SeriesConfidence { HIGH, MEDIUM, NONE }

enum class EnvelopeShape {
    UNIMODAL,
    BIMODAL,
    FLAT,
    DECREASING,
}

data class HomologousSeriesResult(
    val detected: Boolean,
    val confidence: SeriesConfidence,
    val meanDeltaRt: Double,
    val cvDeltaRt: Double,
    val memberCount: Int,
    val interpretation: String,
)

data class OddEvenResult(
    val oddAreaSum: Double,
    val evenAreaSum: Double,
    val ratio: Double,
    val interpretation: String,
)

data class UcmResult(
    val ucmRatio: Double,
    val resolvedArea: Double,
    val totalArea: Double,
    val interpretation: String,
)

data class EnvelopeResult(
    val shape: EnvelopeShape,
    val interpretation: String,
)

data class ClusterResult(
    val clusterCount: Int,
    val isolatedCount: Int,
    val largestClusterSize: Int,
    val meanClusterSize: Double,
    val interpretation: String,
)

/**
 * Full pattern analysis result.
 */
data class PatternResult(
    val homologousSeries: HomologousSeriesResult,
    val oddEvenRatio: OddEvenResult?,
    val ucm: UcmResult,
    val envelope: EnvelopeResult,
    val clusters: ClusterResult,
)

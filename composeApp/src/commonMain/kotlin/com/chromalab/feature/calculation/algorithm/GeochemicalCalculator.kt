package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.PeakResult

/**
 * Geochemical index calculator (§3.4).
 *
 * Computes petroleum geochemistry indices from peak area distributions.
 * Works ONLY when a homologous series is detected (PatternAnalyzer).
 *
 * The user must provide:
 * - firstCarbonNumber: carbon number of the first peak in the series
 *   (e.g., 10 if first peak = C₁₀)
 *
 * Calculated indices:
 * - CPI (Carbon Preference Index) — OM maturity
 * - OEP (Odd-Even Predominance) — odd/even preference at Cmax
 * - ACL (Average Chain Length) — mean carbon number weighted by area
 * - Cmax — carbon number with maximum area
 * - TAR (Terrestrial/Aquatic Ratio) — source indicator
 *
 * Pure function: deterministic, no side effects.
 */
object GeochemicalCalculator {

    /**
     * Calculate geochemical indices for a homologous series.
     *
     * @param peaks Sorted peaks (by RT)
     * @param firstCarbonNumber Carbon number assigned to the first peak
     * @return GeochemistryResult with all indices and interpretations, or null if < 3 peaks
     */
    fun calculate(
        peaks: List<PeakResult>,
        firstCarbonNumber: Int = 10,
    ): GeochemistryResult? {
        val sorted = peaks.sortedBy { it.rtApex }
        if (sorted.size < 3) return null

        // Map peak index → carbon number
        val carbonPeaks = sorted.mapIndexed { i, p ->
            CarbonPeak(
                carbonNumber = firstCarbonNumber + i,
                area = p.area,
                rtApex = p.rtApex,
                peakIndex = i,
            )
        }

        val cpi = calculateCpi(carbonPeaks)
        val oep = calculateOep(carbonPeaks)
        val acl = calculateAcl(carbonPeaks)
        val cmax = calculateCmax(carbonPeaks)
        val tar = calculateTar(carbonPeaks)

        return GeochemistryResult(
            firstCarbonNumber = firstCarbonNumber,
            lastCarbonNumber = firstCarbonNumber + sorted.size - 1,
            peakCount = sorted.size,
            cpi = cpi,
            oep = oep,
            acl = acl,
            cmax = cmax,
            tar = tar,
        )
    }

    // ─── CPI (Carbon Preference Index) ───────────────────────────

    /**
     * CPI = Σ Area(odd C) / Σ Area(even C)
     *
     * CPI ≈ 1.0 → mature OM (petroleum window)
     * CPI > 3.0 → immature terrestrial OM (higher plant waxes)
     * CPI < 1.0 → possible marine/microbial source
     */
    private fun calculateCpi(peaks: List<CarbonPeak>): MetricValue {
        var oddSum = 0.0
        var evenSum = 0.0
        for (p in peaks) {
            if (p.carbonNumber % 2 == 1) oddSum += p.area
            else evenSum += p.area
        }
        val value = if (evenSum > 0) oddSum / evenSum else 0.0

        val interpretation = when {
            value > 5.0 -> "Очень незрелое ОВ — сильное нечётное преобладание (высшие растения)"
            value > 3.0 -> "Незрелое наземное ОВ (воски высших растений)"
            value > 1.5 -> "Слабо зрелое ОВ — умеренное нечётное преобладание"
            value > 0.8 -> "Зрелое ОВ (нефтяной диапазон) — нет чёт/нечёт предпочтения"
            value > 0.5 -> "Чётное преобладание — возможный морской/микробный источник"
            else -> "Сильное чётное преобладание"
        }
        return MetricValue(
            name = "CPI",
            value = value,
            formatted = "%.2f".format(value),
            unit = "",
            interpretation = interpretation,
        )
    }

    // ─── OEP (Odd-Even Predominance) ─────────────────────────────

    /**
     * OEP at Cmax:
     * OEP = (A[i-1] + 6·A[i] + A[i+1]) / (4·A[i-1] + 4·A[i+1])
     * where i = Cmax (odd carbon)
     *
     * OEP > 1 → odd predominance, OEP ≈ 1 → mature
     */
    private fun calculateOep(peaks: List<CarbonPeak>): MetricValue {
        // Find Cmax among odd carbons
        val oddPeaks = peaks.filter { it.carbonNumber % 2 == 1 }
        if (oddPeaks.size < 2) {
            return MetricValue("OEP", 0.0, "—", "", "Недостаточно нечётных пиков")
        }

        val cmaxOdd = oddPeaks.maxBy { it.area }
        val idx = peaks.indexOf(cmaxOdd)

        if (idx <= 0 || idx >= peaks.size - 1) {
            return MetricValue("OEP", 0.0, "—", "", "Cmax на краю — OEP не определяется")
        }

        val aPrev = peaks[idx - 1].area
        val aCurr = peaks[idx].area
        val aNext = peaks[idx + 1].area

        val denominator = 4.0 * aPrev + 4.0 * aNext
        val value = if (denominator > 0) {
            (aPrev + 6.0 * aCurr + aNext) / denominator
        } else 0.0

        val interpretation = when {
            value > 2.0 -> "Сильное нечётное преобладание — незрелое ОВ"
            value > 1.2 -> "Умеренное нечётное преобладание"
            value > 0.8 -> "Нет выраженного преобладания — зрелое ОВ"
            else -> "Чётное преобладание"
        }
        return MetricValue(
            name = "OEP",
            value = value,
            formatted = "%.2f".format(value),
            unit = "",
            interpretation = interpretation,
        )
    }

    // ─── ACL (Average Chain Length) ───────────────────────────────

    /**
     * ACL = Σ(n × Aₙ) / Σ(Aₙ) for odd carbons C₂₅-C₃₃
     *
     * Higher ACL → longer chain → terrestrial source
     */
    private fun calculateAcl(peaks: List<CarbonPeak>): MetricValue {
        // Use C25-C33 odd carbons if available, otherwise all odd
        val targetRange = peaks.filter {
            it.carbonNumber % 2 == 1 &&
                it.carbonNumber in 25..33
        }
        val oddPeaks = targetRange.ifEmpty {
            peaks.filter { it.carbonNumber % 2 == 1 }
        }

        if (oddPeaks.isEmpty()) {
            return MetricValue("ACL", 0.0, "—", "", "Нет нечётных пиков для расчёта")
        }

        val sumNA = oddPeaks.sumOf { it.carbonNumber.toDouble() * it.area }
        val sumA = oddPeaks.sumOf { it.area }
        val value = if (sumA > 0) sumNA / sumA else 0.0

        val rangeLabel = if (targetRange.isNotEmpty()) "C₂₅-C₃₃" else "все нечётные"

        val interpretation = when {
            value >= 30 -> "Длинные цепи — наземные высшие растения ($rangeLabel)"
            value >= 27 -> "Смешанный источник ($rangeLabel)"
            else -> "Короткие цепи — водоросли, микроорганизмы ($rangeLabel)"
        }
        return MetricValue(
            name = "ACL",
            value = value,
            formatted = "%.1f".format(value),
            unit = "",
            interpretation = interpretation,
        )
    }

    // ─── Cmax ────────────────────────────────────────────────────

    /**
     * Cmax — carbon number with the largest peak area.
     */
    private fun calculateCmax(peaks: List<CarbonPeak>): MetricValue {
        val cmax = peaks.maxBy { it.area }
        val value = cmax.carbonNumber.toDouble()

        val interpretation = when {
            cmax.carbonNumber <= 17 -> "Cmax=C${cmax.carbonNumber} — преобладание водных организмов"
            cmax.carbonNumber <= 23 -> "Cmax=C${cmax.carbonNumber} — смешанный источник ОВ"
            cmax.carbonNumber <= 27 -> "Cmax=C${cmax.carbonNumber} — наземные растения (кутикулярные воски)"
            else -> "Cmax=C${cmax.carbonNumber} — высшие наземные растения"
        }
        return MetricValue(
            name = "Cmax",
            value = value,
            formatted = "C${cmax.carbonNumber}",
            unit = "",
            interpretation = interpretation,
        )
    }

    // ─── TAR (Terrestrial/Aquatic Ratio) ─────────────────────────

    /**
     * TAR = (A_C27 + A_C29 + A_C31) / (A_C15 + A_C17 + A_C19)
     *
     * TAR > 1 → terrestrial dominance
     * TAR < 1 → aquatic dominance
     */
    private fun calculateTar(peaks: List<CarbonPeak>): MetricValue {
        val terrestrial = listOf(27, 29, 31)
        val aquatic = listOf(15, 17, 19)

        val terrSum = peaks.filter { it.carbonNumber in terrestrial }.sumOf { it.area }
        val aquaSum = peaks.filter { it.carbonNumber in aquatic }.sumOf { it.area }

        if (aquaSum <= 0 && terrSum <= 0) {
            return MetricValue("TAR", 0.0, "—", "",
                "Пики C15-C19 и C27-C31 отсутствуют — TAR не определяется")
        }
        if (aquaSum <= 0) {
            return MetricValue("TAR", 99.0, ">99", "",
                "Только наземные пики — полное преобладание наземного ОВ")
        }

        val value = terrSum / aquaSum

        val interpretation = when {
            value > 5.0 -> "Сильное преобладание наземного ОВ"
            value > 1.0 -> "Преобладание наземного ОВ"
            value > 0.5 -> "Смешанный наземный/водный источник"
            else -> "Преобладание водного ОВ"
        }
        return MetricValue(
            name = "TAR",
            value = value,
            formatted = "%.2f".format(value),
            unit = "",
            interpretation = interpretation,
        )
    }
}

// ─── Data models ────────────────────────────────────────────────

/**
 * Internal: peak with assigned carbon number.
 */
private data class CarbonPeak(
    val carbonNumber: Int,
    val area: Double,
    val rtApex: Double,
    val peakIndex: Int,
)

/**
 * Source of compound identification.
 */
enum class CompoundSource {
    /** Not identified */
    NONE,
    /** User typed the name manually */
    MANUAL,
    /** Auto-assigned from compound template */
    TEMPLATE,
    /** Auto-numbered from homologous series detection */
    AUTO_SERIES,
}

/**
 * Predefined compound templates for common analyses.
 */
enum class CompoundTemplate(val label: String) {
    N_ALKANES("н-Алканы (C₁₀—C₄₀)"),
    CUSTOM("Пользовательский"),
}

/**
 * Full geochemistry analysis result.
 */
data class GeochemistryResult(
    val firstCarbonNumber: Int,
    val lastCarbonNumber: Int,
    val peakCount: Int,
    val cpi: MetricValue,
    val oep: MetricValue,
    val acl: MetricValue,
    val cmax: MetricValue,
    val tar: MetricValue,
) {
    /** All indices as ordered list. */
    val allIndices: List<MetricValue>
        get() = listOf(cpi, oep, acl, cmax, tar)
}

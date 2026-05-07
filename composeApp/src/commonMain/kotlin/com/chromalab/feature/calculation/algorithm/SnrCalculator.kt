package com.chromalab.feature.calculation.algorithm

/**
 * Signal-to-noise ratio calculator (§2.15).
 *
 * Three S/N formulas:
 * - Peak-to-peak: S/N = 2H / h  (pharmacopeia standard)
 * - RMS:          S/N = H / RMS_noise (engineering)
 * - Robust:       S/N = H / MAD_noise (outlier-resistant)
 *
 * Flags (informational only, NOT regulatory):
 * - <3:   low S/N — signal may not be distinguishable from noise
 * - 3–10: detectable-like — signal visible but quantitation unreliable
 * - ≥10:  quantitation-like — signal suitable for area measurement
 *
 * Pure function: deterministic, no side effects.
 */
object SnrCalculator {

    /**
     * Calculate S/N for a peak.
     *
     * @param peakHeight   Height of the peak above baseline (H)
     * @param noiseResult  Noise estimation result from NoiseEstimator
     * @param method       S/N calculation method
     * @return SnrResult with value, method, flag, and formula description
     */
    fun calculate(
        peakHeight: Double,
        noiseResult: NoiseResult,
        method: SnrMethod = SnrMethod.PEAK_TO_PEAK,
    ): SnrResult {
        if (peakHeight <= 0.0 || noiseResult.noiseValue <= 0.0) {
            return SnrResult(
                value = 0.0,
                method = method,
                flag = SnrFlag.LOW,
                formula = method.formula,
                noiseValue = noiseResult.noiseValue,
                noiseMethod = noiseResult.method,
                warnings = listOf("S/N не вычислен: высота или шум = 0"),
            )
        }

        val snr = when (method) {
            SnrMethod.PEAK_TO_PEAK -> 2.0 * peakHeight / noiseResult.noiseValue
            SnrMethod.RMS -> peakHeight / noiseResult.noiseValue
            SnrMethod.ROBUST -> peakHeight / noiseResult.noiseValue
        }

        val flag = classifySnr(snr)
        val warnings = mutableListOf<String>()

        if (flag == SnrFlag.LOW) {
            warnings.add("S/N < 3 — сигнал может быть неотличим от шума")
        }

        // Add noise result warnings
        warnings.addAll(noiseResult.warnings)

        return SnrResult(
            value = snr,
            method = method,
            flag = flag,
            formula = method.formula,
            noiseValue = noiseResult.noiseValue,
            noiseMethod = noiseResult.method,
            warnings = warnings,
        )
    }

    /**
     * Calculate S/N using all three methods for comparison.
     */
    fun calculateAll(
        peakHeight: Double,
        peakToPeakNoise: NoiseResult,
        rmsNoise: NoiseResult,
        madNoise: NoiseResult,
    ): Map<SnrMethod, SnrResult> {
        return mapOf(
            SnrMethod.PEAK_TO_PEAK to calculate(peakHeight, peakToPeakNoise, SnrMethod.PEAK_TO_PEAK),
            SnrMethod.RMS to calculate(peakHeight, rmsNoise, SnrMethod.RMS),
            SnrMethod.ROBUST to calculate(peakHeight, madNoise, SnrMethod.ROBUST),
        )
    }

    private fun classifySnr(snr: Double): SnrFlag = when {
        snr < 3.0 -> SnrFlag.LOW
        snr < 10.0 -> SnrFlag.DETECTABLE
        else -> SnrFlag.QUANTITATION
    }
}

// ─── Data classes ───────────────────────────────────────────────

enum class SnrMethod(val label: String, val formula: String) {
    PEAK_TO_PEAK("Peak-to-peak", "S/N = 2H / h"),
    RMS("RMS", "S/N = H / RMS(noise)"),
    ROBUST("Robust (MAD)", "S/N = H / MAD(noise)"),
}

/**
 * S/N classification flags.
 * These are informational ONLY — not regulatory or lab conclusions.
 */
enum class SnrFlag(val label: String, val description: String) {
    LOW(
        "Низкий",
        "S/N < 3 — сигнал может быть неотличим от шума"
    ),
    DETECTABLE(
        "Детектируемый",
        "S/N 3–10 — сигнал виден, но количественное определение ненадёжно"
    ),
    QUANTITATION(
        "Количественный",
        "S/N ≥ 10 — сигнал подходит для измерения площади"
    ),
}

data class SnrResult(
    val value: Double,
    val method: SnrMethod,
    val flag: SnrFlag,
    val formula: String,
    val noiseValue: Double,
    val noiseMethod: NoiseMethod,
    val warnings: List<String>,
)

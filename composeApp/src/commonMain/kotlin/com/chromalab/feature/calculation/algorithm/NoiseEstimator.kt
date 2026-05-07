package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.SignalPoint
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Noise estimation module (§2.14).
 *
 * Estimates noise level from designated noise regions of the signal.
 * Supports manual and automatic region selection.
 *
 * Methods:
 * - Peak-to-peak: h = max - min in noise region
 * - RMS: root mean square of deviations from mean
 * - MAD (robust): median absolute deviation × 1.4826
 *
 * Pure function: deterministic, no side effects.
 */
object NoiseEstimator {

    /**
     * Estimate noise from a specified region.
     *
     * @param points        Full signal (baseline-corrected)
     * @param regionStart   Start time of noise region (null = auto)
     * @param regionEnd     End time of noise region (null = auto)
     * @param peakRegions   List of (start, end) time pairs to exclude from noise
     * @param method        Noise estimation method
     * @return NoiseResult with noise value + metadata
     */
    fun estimate(
        points: List<SignalPoint>,
        regionStart: Double? = null,
        regionEnd: Double? = null,
        peakRegions: List<Pair<Double, Double>> = emptyList(),
        method: NoiseMethod = NoiseMethod.PEAK_TO_PEAK,
    ): NoiseResult {
        if (points.isEmpty()) {
            return NoiseResult.empty("Нет точек для оценки шума")
        }

        val warnings = mutableListOf<String>()

        // Select noise region
        val (regionPoints, actualStart, actualEnd, isManual) = selectRegion(
            points, regionStart, regionEnd, peakRegions, warnings
        )

        if (regionPoints.size < 5) {
            warnings.add("Noise region слишком короткий (${regionPoints.size} точек) — результат ненадёжный")
        }

        if (isManual) {
            warnings.add("Noise region выбран вручную")
        }

        // Check for suspicious peaks in noise region
        val suspiciousPeaks = detectSuspiciousPeaks(regionPoints)
        if (suspiciousPeaks > 0) {
            warnings.add("Noise region содержит $suspiciousPeaks подозрительных пиков")
        }

        // Calculate noise
        val intensities = regionPoints.map { it.intensity }
        val noiseValue = when (method) {
            NoiseMethod.PEAK_TO_PEAK -> peakToPeakNoise(intensities)
            NoiseMethod.RMS -> rmsNoise(intensities)
            NoiseMethod.MAD -> madNoise(intensities)
        }

        return NoiseResult(
            noiseValue = noiseValue,
            method = method,
            regionStartTime = actualStart,
            regionEndTime = actualEnd,
            regionPointCount = regionPoints.size,
            isManualRegion = isManual,
            warnings = warnings,
        )
    }

    // ─── Noise calculation methods ──────────────────────────────

    /**
     * Peak-to-peak noise: h = max(region) - min(region)
     */
    private fun peakToPeakNoise(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        return values.max() - values.min()
    }

    /**
     * RMS noise: sqrt(mean((x - mean(x))²))
     */
    private fun rmsNoise(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
    }

    /**
     * MAD-based robust noise: MAD × 1.4826
     * (consistent estimator for Gaussian σ)
     */
    private fun madNoise(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val median = median(values)
        val deviations = values.map { abs(it - median) }
        return median(deviations) * 1.4826
    }

    // ─── Region selection ───────────────────────────────────────

    private data class RegionSelection(
        val points: List<SignalPoint>,
        val startTime: Double,
        val endTime: Double,
        val isManual: Boolean,
    )

    private fun selectRegion(
        points: List<SignalPoint>,
        regionStart: Double?,
        regionEnd: Double?,
        peakRegions: List<Pair<Double, Double>>,
        warnings: MutableList<String>,
    ): RegionSelection {
        return if (regionStart != null && regionEnd != null) {
            // Manual region
            val filtered = points.filter { it.time in regionStart..regionEnd }
                .filter { p -> peakRegions.none { (s, e) -> p.time in s..e } }
            RegionSelection(filtered, regionStart, regionEnd, isManual = true)
        } else {
            // Auto region: first 10% and last 10%, excluding peaks
            autoSelectRegion(points, peakRegions, warnings)
        }
    }

    private fun autoSelectRegion(
        points: List<SignalPoint>,
        peakRegions: List<Pair<Double, Double>>,
        warnings: MutableList<String>,
    ): RegionSelection {
        val n = points.size
        val regionSize = maxOf(5, n / 10) // 10% of signal, minimum 5 points

        // Try first 10%
        val firstRegion = points.take(regionSize)
            .filter { p -> peakRegions.none { (s, e) -> p.time in s..e } }

        // Try last 10%
        val lastRegion = points.takeLast(regionSize)
            .filter { p -> peakRegions.none { (s, e) -> p.time in s..e } }

        // Use the quieter region (lower variance)
        val firstVar = variance(firstRegion.map { it.intensity })
        val lastVar = variance(lastRegion.map { it.intensity })

        val selected = if (firstRegion.size >= 5 && (firstVar <= lastVar || lastRegion.size < 5)) {
            firstRegion
        } else if (lastRegion.size >= 5) {
            lastRegion
        } else {
            // Fallback: combine both
            val combined = firstRegion + lastRegion
            if (combined.isEmpty()) {
                warnings.add("Не удалось найти подходящий noise region — используется весь сигнал")
                return RegionSelection(points, points.first().time, points.last().time, false)
            }
            combined
        }

        return RegionSelection(
            points = selected,
            startTime = selected.first().time,
            endTime = selected.last().time,
            isManual = false,
        )
    }

    // ─── Utilities ──────────────────────────────────────────────

    private fun detectSuspiciousPeaks(region: List<SignalPoint>): Int {
        if (region.size < 5) return 0
        val intensities = region.map { it.intensity }
        val mean = intensities.average()
        val std = sqrt(variance(intensities))
        if (std == 0.0) return 0

        // Count points > 3σ from mean
        return intensities.count { abs(it - mean) > 3.0 * std }
    }

    private fun variance(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        return values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
    }

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[mid - 1] + sorted[mid]) / 2.0
        } else {
            sorted[mid]
        }
    }
}

// ─── Data classes ───────────────────────────────────────────────

enum class NoiseMethod(val label: String) {
    PEAK_TO_PEAK("Peak-to-peak"),
    RMS("RMS"),
    MAD("MAD (robust)"),
}

data class NoiseResult(
    val noiseValue: Double,
    val method: NoiseMethod,
    val regionStartTime: Double,
    val regionEndTime: Double,
    val regionPointCount: Int,
    val isManualRegion: Boolean,
    val warnings: List<String>,
) {
    companion object {
        fun empty(warning: String) = NoiseResult(
            noiseValue = 0.0,
            method = NoiseMethod.PEAK_TO_PEAK,
            regionStartTime = 0.0,
            regionEndTime = 0.0,
            regionPointCount = 0,
            isManualRegion = false,
            warnings = listOf(warning),
        )
    }
}

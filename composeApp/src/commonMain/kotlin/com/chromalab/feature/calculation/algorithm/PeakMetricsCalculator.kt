package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.SignalPoint
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Peak metrics calculator (§2.20).
 *
 * Computes all quantitative metrics for a detected peak:
 * RT, height, area, widths, S/N, prominence, confidence.
 *
 * Pure function: deterministic, no side effects.
 */
object PeakMetricsCalculator {

    /**
     * Calculate all metrics for a peak.
     *
     * @param points          Full signal (baseline-corrected)
     * @param peak            Detected peak candidate
     * @param boundary        Peak boundaries
     * @param integration     Integration result
     * @param snr             Signal-to-noise result
     * @param overlap         Overlap classification
     * @param isManuallyEdited Whether boundaries/peak were manually adjusted
     * @return Complete PeakMetrics
     */
    fun calculate(
        points: List<SignalPoint>,
        peak: PeakCandidate,
        boundary: PeakBoundary,
        integration: IntegrationResult,
        snr: SnrResult?,
        overlap: PeakOverlapInfo?,
        isManuallyEdited: Boolean = false,
    ): PeakMetrics {
        val warnings = mutableListOf<String>()
        val intensities = points.map { it.intensity }

        // RT apex
        val rtApex = peak.apexTime

        // RT centroid (intensity-weighted mean time)
        val rtCentroid = computeCentroid(points, boundary.leftIndex, boundary.rightIndex)

        // Height (above baseline-corrected = raw corrected intensity at apex)
        val height = peak.apexIntensity

        // Area
        val area = integration.totalArea

        // Width at base
        val widthBase = boundary.widthTime

        // Width at half height
        val widthHalfHeight = computeWidthAtFraction(
            points, peak.index, boundary.leftIndex, boundary.rightIndex, 0.5
        )

        // Width at half prominence
        val widthHalfProminence = computeWidthAtProminenceFraction(
            intensities, peak.index, peak.prominence, 0.5
        )

        // Prominence
        val prominence = peak.prominence

        // S/N
        val snrValue = snr?.value ?: 0.0
        val snrFlag = snr?.flag ?: SnrFlag.LOW

        // Overlap
        val overlapStatus = overlap?.status ?: OverlapStatus.ISOLATED

        // Warnings from all sources
        warnings.addAll(boundary.warnings)
        snr?.warnings?.let { warnings.addAll(it) }
        overlap?.warnings?.let { warnings.addAll(it) }

        if (integration.negativeArea < 0.0) {
            val negFraction = if (integration.positiveArea > 0) {
                kotlin.math.abs(integration.negativeArea) / integration.positiveArea
            } else 0.0
            if (negFraction > 0.05) {
                warnings.add(
                    "Отрицательная площадь составляет ${(negFraction * 100).toInt()}% — " +
                    "проверьте baseline"
                )
            }
        }

        if (isManuallyEdited) {
            warnings.add("Пик изменён вручную")
        }

        // USP Tailing Factor (at 5% height)
        val tailingFactor = computeTailingFactor(
            points, peak.index, boundary.leftIndex, boundary.rightIndex, 0.05
        )

        // EP Asymmetry Factor (at 10% height)
        val asymmetryFactor = computeAsymmetryFactor(
            points, peak.index, boundary.leftIndex, boundary.rightIndex, 0.10
        )

        // USP Plate Count: N = 5.545 × (RT / W½)²
        val plateCount = if (widthHalfHeight > 0.0) {
            (5.545 * (rtApex / widthHalfHeight) * (rtApex / widthHalfHeight)).toInt()
        } else null

        // Tailing warnings
        if (tailingFactor > 2.0) {
            warnings.add("USP Tailing factor ${"%.2f".format(tailingFactor)} > 2.0 — пик сильно асимметричен")
        } else if (tailingFactor > 1.5) {
            warnings.add("USP Tailing factor ${"%.2f".format(tailingFactor)} > 1.5 — умеренная асимметрия")
        }

        return PeakMetrics(
            rtApex = rtApex,
            rtCentroid = rtCentroid,
            height = height,
            area = area,
            widthBase = widthBase,
            widthHalfHeight = widthHalfHeight,
            widthHalfProminence = widthHalfProminence,
            prominence = prominence,
            leftBaseTime = boundary.leftTime,
            rightBaseTime = boundary.rightTime,
            snrValue = snrValue,
            snrFlag = snrFlag,
            overlapStatus = overlapStatus,
            boundaryMethod = boundary.method,
            boundaryConfidence = boundary.confidence,
            isManuallyEdited = isManuallyEdited,
            tailingFactor = tailingFactor,
            asymmetryFactor = asymmetryFactor,
            plateCount = plateCount,
            warnings = warnings,
        )
    }

    // ─── Width calculations ─────────────────────────────────────

    /**
     * Width at a given fraction of peak height.
     * E.g., fraction=0.5 → width at half-maximum (FWHM).
     */
    private fun computeWidthAtFraction(
        points: List<SignalPoint>,
        peakIdx: Int,
        leftIdx: Int,
        rightIdx: Int,
        fraction: Double,
    ): Double {
        val threshold = points[peakIdx].intensity * fraction

        // Find left crossing
        var leftTime = points[leftIdx].time
        for (i in peakIdx downTo leftIdx + 1) {
            if (points[i - 1].intensity <= threshold && points[i].intensity > threshold) {
                // Linear interpolation
                val frac = (threshold - points[i - 1].intensity) /
                    (points[i].intensity - points[i - 1].intensity)
                leftTime = points[i - 1].time + frac * (points[i].time - points[i - 1].time)
                break
            }
        }

        // Find right crossing
        var rightTime = points[rightIdx].time
        for (i in peakIdx until rightIdx) {
            if (points[i].intensity > threshold && points[i + 1].intensity <= threshold) {
                val frac = (threshold - points[i + 1].intensity) /
                    (points[i].intensity - points[i + 1].intensity)
                rightTime = points[i + 1].time + frac * (points[i].time - points[i + 1].time)
                break
            }
        }

        return rightTime - leftTime
    }

    /**
     * Width at a given fraction of peak prominence.
     */
    private fun computeWidthAtProminenceFraction(
        intensities: List<Double>,
        peakIdx: Int,
        prominence: Double,
        fraction: Double,
    ): Double {
        val contour = intensities[peakIdx] - prominence * fraction

        var leftIdx = peakIdx
        for (i in peakIdx downTo 1) {
            if (intensities[i - 1] <= contour) {
                leftIdx = i
                break
            }
            leftIdx = i
        }

        var rightIdx = peakIdx
        for (i in peakIdx until intensities.size - 1) {
            if (intensities[i + 1] <= contour) {
                rightIdx = i
                break
            }
            rightIdx = i
        }

        return (rightIdx - leftIdx).toDouble()
    }

    // ─── USP Tailing Factor ─────────────────────────────────────

    /**
     * USP Tailing Factor: T = W / (2 × f)
     * where W is peak width at [heightFraction] of peak height,
     * and f is the front half-width (leading edge to apex at that height).
     *
     * @param heightFraction 0.05 for USP (5%), 0.10 for EP (10%)
     * @return Tailing factor. 1.0 = symmetric. >1 = tailing. <1 = fronting.
     */
    private fun computeTailingFactor(
        points: List<SignalPoint>,
        peakIdx: Int,
        leftIdx: Int,
        rightIdx: Int,
        heightFraction: Double,
    ): Double {
        val peakIntensity = points[peakIdx].intensity
        val threshold = peakIntensity * heightFraction
        if (threshold <= 0.0) return 1.0

        val peakTime = points[peakIdx].time

        // Find left crossing time (front)
        var leftTime = points[leftIdx].time
        for (i in peakIdx downTo leftIdx + 1) {
            if (points[i - 1].intensity <= threshold && points[i].intensity > threshold) {
                val frac = (threshold - points[i - 1].intensity) /
                    (points[i].intensity - points[i - 1].intensity)
                leftTime = points[i - 1].time + frac * (points[i].time - points[i - 1].time)
                break
            }
        }

        // Find right crossing time (tail)
        var rightTime = points[rightIdx].time
        for (i in peakIdx until rightIdx) {
            if (points[i].intensity > threshold && points[i + 1].intensity <= threshold) {
                val frac = (threshold - points[i + 1].intensity) /
                    (points[i].intensity - points[i + 1].intensity)
                rightTime = points[i + 1].time + frac * (points[i].time - points[i + 1].time)
                break
            }
        }

        val totalWidth = rightTime - leftTime
        val frontWidth = peakTime - leftTime

        return if (frontWidth > 0.0) totalWidth / (2.0 * frontWidth) else 1.0
    }

    // ─── EP Asymmetry Factor ────────────────────────────────────

    /**
     * Asymmetry Factor (EP): As = b / a
     * where a = front half-width, b = rear half-width at [heightFraction].
     *
     * @param heightFraction 0.10 for EP standard
     * @return Asymmetry factor. 1.0 = symmetric.
     */
    private fun computeAsymmetryFactor(
        points: List<SignalPoint>,
        peakIdx: Int,
        leftIdx: Int,
        rightIdx: Int,
        heightFraction: Double,
    ): Double {
        val peakIntensity = points[peakIdx].intensity
        val threshold = peakIntensity * heightFraction
        if (threshold <= 0.0) return 1.0

        val peakTime = points[peakIdx].time

        // Find left crossing
        var leftTime = points[leftIdx].time
        for (i in peakIdx downTo leftIdx + 1) {
            if (points[i - 1].intensity <= threshold && points[i].intensity > threshold) {
                val frac = (threshold - points[i - 1].intensity) /
                    (points[i].intensity - points[i - 1].intensity)
                leftTime = points[i - 1].time + frac * (points[i].time - points[i - 1].time)
                break
            }
        }

        // Find right crossing
        var rightTime = points[rightIdx].time
        for (i in peakIdx until rightIdx) {
            if (points[i].intensity > threshold && points[i + 1].intensity <= threshold) {
                val frac = (threshold - points[i + 1].intensity) /
                    (points[i].intensity - points[i + 1].intensity)
                rightTime = points[i + 1].time + frac * (points[i].time - points[i + 1].time)
                break
            }
        }

        val a = peakTime - leftTime  // front half
        val b = rightTime - peakTime // rear half

        return if (a > 0.0) b / a else 1.0
    }

    // ─── Resolution ─────────────────────────────────────────────

    /**
     * USP Resolution between two adjacent peaks:
     * Rs = 2 × (RT₂ - RT₁) / (W₁ + W₂)
     * where W is peak width at base.
     */
    fun calculateResolution(
        rt1: Double, widthBase1: Double,
        rt2: Double, widthBase2: Double,
    ): Double {
        val denom = widthBase1 + widthBase2
        return if (denom > 0.0) 2.0 * abs(rt2 - rt1) / denom else 0.0
    }

    // ─── Centroid ───────────────────────────────────────────────

    /**
     * Intensity-weighted mean retention time (centroid).
     */
    private fun computeCentroid(
        points: List<SignalPoint>,
        leftIdx: Int,
        rightIdx: Int,
    ): Double {
        var sumIT = 0.0
        var sumI = 0.0
        for (i in leftIdx..rightIdx) {
            val intensity = maxOf(0.0, points[i].intensity)
            sumIT += intensity * points[i].time
            sumI += intensity
        }
        return if (sumI > 0.0) sumIT / sumI else points[(leftIdx + rightIdx) / 2].time
    }
}

// ─── Data classes ───────────────────────────────────────────────

data class PeakMetrics(
    val rtApex: Double,
    val rtCentroid: Double,
    val height: Double,
    val area: Double,
    val widthBase: Double,
    val widthHalfHeight: Double,
    val widthHalfProminence: Double,
    val prominence: Double,
    val leftBaseTime: Double,
    val rightBaseTime: Double,
    val snrValue: Double,
    val snrFlag: SnrFlag,
    val overlapStatus: OverlapStatus,
    val boundaryMethod: BoundaryMethod,
    val boundaryConfidence: Double,
    val isManuallyEdited: Boolean,
    val tailingFactor: Double = 1.0,
    val asymmetryFactor: Double = 1.0,
    val plateCount: Int? = null,
    val warnings: List<String>,
)

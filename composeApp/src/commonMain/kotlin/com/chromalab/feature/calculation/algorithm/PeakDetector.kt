package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.SignalPoint
import kotlin.math.abs

/**
 * Peak detection algorithm (§2.16).
 *
 * Detects peaks in the baseline-corrected (optionally smoothed) signal.
 *
 * Algorithm:
 * 1. Find all local maxima
 * 2. Compute prominence for each candidate
 * 3. Filter by: minHeight, minProminence, minDistance, minWidth
 * 4. Score and classify each candidate
 *
 * Pure function: deterministic, no side effects.
 */
object PeakDetector {

    /**
     * Detect peaks in the signal.
     *
     * @param points         Signal points (baseline-corrected, optionally smoothed)
     * @param minHeight      Minimum absolute peak height
     * @param minProminence  Minimum prominence (absolute or k × noise)
     * @param minDistance     Minimum distance between peaks (in points)
     * @param minWidth       Minimum peak width (in points)
     * @param maxWidth       Maximum peak width (in points, 0 = no limit)
     * @param noiseLevel     Noise level for k × noise prominence threshold
     * @param noiseK         Multiplier for noise-based prominence (default 3.0)
     * @return PeakDetectionResult with accepted and rejected candidates
     */
    fun detect(
        points: List<SignalPoint>,
        minHeight: Double = 0.0,
        minProminence: Double = 0.0,
        minDistance: Int = 5,
        minWidth: Int = 3,
        maxWidth: Int = 0,
        noiseLevel: Double = 0.0,
        noiseK: Double = 3.0,
    ): PeakDetectionResult {
        if (points.size < 3) {
            return PeakDetectionResult(emptyList(), emptyList())
        }

        val intensities = points.map { it.intensity }

        // Step 1: Find all local maxima
        val localMaxima = findLocalMaxima(intensities)

        // Step 2: Compute prominence for each candidate
        val candidates = localMaxima.map { idx ->
            val prominence = computeProminence(intensities, idx)
            val (leftBase, rightBase) = findProminenceBases(intensities, idx)
            val width = rightBase - leftBase
            PeakCandidate(
                index = idx,
                apexTime = points[idx].time,
                apexIntensity = intensities[idx],
                prominence = prominence,
                leftBaseIndex = leftBase,
                rightBaseIndex = rightBase,
                widthPoints = width,
            )
        }

        // Step 3: Filter candidates
        val effectiveMinProminence = if (noiseLevel > 0.0) {
            maxOf(minProminence, noiseK * noiseLevel)
        } else {
            minProminence
        }

        val accepted = mutableListOf<PeakCandidate>()
        val rejected = mutableListOf<PeakCandidate>()

        // Sort by intensity descending for distance filtering
        val sorted = candidates.sortedByDescending { it.apexIntensity }

        for (candidate in sorted) {
            val reason = filterCandidate(
                candidate, effectiveMinProminence, minHeight, minWidth, maxWidth, accepted, minDistance
            )
            if (reason == null) {
                accepted.add(candidate.copy(
                    accepted = true,
                    detectionScore = scoreCandidate(candidate, effectiveMinProminence, noiseLevel),
                ))
            } else {
                rejected.add(candidate.copy(
                    accepted = false,
                    rejectReason = reason,
                ))
            }
        }

        // Sort accepted by time
        val sortedAccepted = accepted.sortedBy { it.apexTime }
        val sortedRejected = rejected.sortedBy { it.apexTime }

        return PeakDetectionResult(sortedAccepted, sortedRejected)
    }

    // ─── Local maxima ───────────────────────────────────────────

    private fun findLocalMaxima(values: List<Double>): List<Int> {
        val maxima = mutableListOf<Int>()
        for (i in 1 until values.size - 1) {
            if (values[i] > values[i - 1] && values[i] > values[i + 1]) {
                maxima.add(i)
            } else if (values[i] > values[i - 1] && values[i] == values[i + 1]) {
                // Handle flat tops: find end of plateau
                var j = i + 1
                while (j < values.size - 1 && values[j] == values[i]) j++
                if (j < values.size && values[j] < values[i]) {
                    maxima.add((i + j - 1) / 2) // center of plateau
                }
            }
        }
        return maxima
    }

    // ─── Prominence ─────────────────────────────────────────────

    private fun computeProminence(values: List<Double>, peakIdx: Int): Double {
        val peakValue = values[peakIdx]

        // Find the minimum value on each side before reaching a higher peak
        val leftMin = findSideMin(values, peakIdx, goLeft = true)
        val rightMin = findSideMin(values, peakIdx, goLeft = false)

        // Prominence = peak height - max(leftMin, rightMin)
        val highestContour = maxOf(leftMin, rightMin)
        return peakValue - highestContour
    }

    private fun findSideMin(values: List<Double>, peakIdx: Int, goLeft: Boolean): Double {
        var minVal = values[peakIdx]
        val range = if (goLeft) (peakIdx - 1 downTo 0) else (peakIdx + 1 until values.size)

        for (i in range) {
            minVal = minOf(minVal, values[i])
            if (values[i] > values[peakIdx]) break // reached a higher peak
        }
        return minVal
    }

    private fun findProminenceBases(values: List<Double>, peakIdx: Int): Pair<Int, Int> {
        val peakValue = values[peakIdx]
        val threshold = peakValue - computeProminence(values, peakIdx)

        // Left base: first point where value drops to threshold
        var leftBase = peakIdx
        for (i in peakIdx - 1 downTo 0) {
            leftBase = i
            if (values[i] <= threshold) break
        }

        // Right base: first point where value drops to threshold
        var rightBase = peakIdx
        for (i in peakIdx + 1 until values.size) {
            rightBase = i
            if (values[i] <= threshold) break
        }

        return leftBase to rightBase
    }

    // ─── Filtering ──────────────────────────────────────────────

    private fun filterCandidate(
        candidate: PeakCandidate,
        minProminence: Double,
        minHeight: Double,
        minWidth: Int,
        maxWidth: Int,
        accepted: List<PeakCandidate>,
        minDistance: Int,
    ): String? {
        if (candidate.apexIntensity < minHeight) {
            return "Высота (${candidate.apexIntensity.format()}) < минимум ($minHeight)"
        }
        if (candidate.prominence < minProminence) {
            return "Prominence (${candidate.prominence.format()}) < минимум ($minProminence)"
        }
        if (candidate.widthPoints < minWidth) {
            return "Ширина (${candidate.widthPoints} точек) < минимум ($minWidth)"
        }
        if (maxWidth > 0 && candidate.widthPoints > maxWidth) {
            return "Ширина (${candidate.widthPoints} точек) > максимум ($maxWidth)"
        }
        // Distance check: too close to an already accepted peak
        for (existing in accepted) {
            if (abs(candidate.index - existing.index) < minDistance) {
                return "Слишком близко к пику RT=${existing.apexTime.format()} (расстояние < $minDistance)"
            }
        }
        return null
    }

    // ─── Scoring ────────────────────────────────────────────────

    private fun scoreCandidate(
        candidate: PeakCandidate,
        minProminence: Double,
        noiseLevel: Double,
    ): Double {
        // Score 0..1 based on how much the candidate exceeds thresholds
        val prominenceScore = if (minProminence > 0) {
            (candidate.prominence / minProminence).coerceIn(0.0, 5.0) / 5.0
        } else 0.5

        val snrScore = if (noiseLevel > 0) {
            (candidate.apexIntensity / noiseLevel).coerceIn(0.0, 50.0) / 50.0
        } else 0.5

        return (prominenceScore * 0.6 + snrScore * 0.4).coerceIn(0.0, 1.0)
    }

    private fun Double.format(): String = "%.2f".format(this)
}

// ─── Data classes ───────────────────────────────────────────────

data class PeakCandidate(
    val index: Int,
    val apexTime: Double,
    val apexIntensity: Double,
    val prominence: Double,
    val leftBaseIndex: Int,
    val rightBaseIndex: Int,
    val widthPoints: Int,
    val detectionScore: Double = 0.0,
    val accepted: Boolean = false,
    val rejectReason: String? = null,
)

data class PeakDetectionResult(
    val accepted: List<PeakCandidate>,
    val rejected: List<PeakCandidate>,
) {
    val totalCandidates: Int get() = accepted.size + rejected.size
}

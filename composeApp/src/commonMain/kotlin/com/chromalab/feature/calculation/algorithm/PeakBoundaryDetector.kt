package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.SignalPoint

/**
 * Peak boundary detection (§2.17).
 *
 * Determines the left and right boundaries for each detected peak.
 * Multiple methods available — user selects or auto-detection picks best.
 *
 * Pure function: deterministic, no side effects.
 */
object PeakBoundaryDetector {

    /**
     * Detect boundaries for a peak.
     *
     * @param points          Full signal (baseline-corrected)
     * @param peakIndex       Index of the peak apex
     * @param method          Boundary detection method
     * @param percentHeight   For PERCENT_HEIGHT method: fraction (0.01 = 1%, 0.05 = 5%)
     * @param manualLeft      For MANUAL: user-specified left boundary time
     * @param manualRight     For MANUAL: user-specified right boundary time
     * @param neighborPeaks   Indices of adjacent peaks (for overlap warnings)
     * @return PeakBoundary with left/right indices, times, and metadata
     */
    fun detect(
        points: List<SignalPoint>,
        peakIndex: Int,
        method: BoundaryMethod = BoundaryMethod.LOCAL_MINIMA,
        percentHeight: Double = 0.01,
        manualLeft: Double? = null,
        manualRight: Double? = null,
        neighborPeaks: List<Int> = emptyList(),
    ): PeakBoundary {
        val warnings = mutableListOf<String>()

        val (leftIdx, rightIdx) = when (method) {
            BoundaryMethod.PROMINENCE_BASES -> prominenceBases(points, peakIndex)
            BoundaryMethod.LOCAL_MINIMA -> localMinima(points, peakIndex, neighborPeaks)
            BoundaryMethod.BASELINE_INTERSECTION -> baselineIntersection(points, peakIndex)
            BoundaryMethod.PERCENT_HEIGHT -> percentOfHeight(points, peakIndex, percentHeight)
            BoundaryMethod.MANUAL -> manual(points, manualLeft, manualRight, warnings)
        }

        // Clamp to valid range
        val safeLeft = leftIdx.coerceIn(0, points.size - 1)
        val safeRight = rightIdx.coerceIn(safeLeft, points.size - 1)

        // Generate warnings
        if (safeLeft == 0) {
            warnings.add("Левая граница обрезана краем сигнала")
        }
        if (safeRight == points.size - 1) {
            warnings.add("Правая граница обрезана краем сигнала")
        }

        // Check overlap with neighbors
        for (neighbor in neighborPeaks) {
            if (neighbor in safeLeft..safeRight && neighbor != peakIndex) {
                warnings.add("Граница пересекается с соседним пиком (индекс $neighbor)")
            }
        }

        // Confidence: higher when boundaries are far from peak and signal reaches baseline
        val confidence = computeConfidence(points, peakIndex, safeLeft, safeRight, method)

        return PeakBoundary(
            leftIndex = safeLeft,
            rightIndex = safeRight,
            leftTime = points[safeLeft].time,
            rightTime = points[safeRight].time,
            method = method,
            confidence = confidence,
            warnings = warnings,
        )
    }

    // ─── Boundary methods ───────────────────────────────────────

    /**
     * Prominence bases: use the contour level from prominence calculation.
     */
    private fun prominenceBases(points: List<SignalPoint>, peakIdx: Int): Pair<Int, Int> {
        val intensities = points.map { it.intensity }
        val peakVal = intensities[peakIdx]

        // Find prominence
        var leftMin = peakVal
        for (i in peakIdx - 1 downTo 0) {
            leftMin = minOf(leftMin, intensities[i])
            if (intensities[i] > peakVal) break
        }
        var rightMin = peakVal
        for (i in peakIdx + 1 until points.size) {
            rightMin = minOf(rightMin, intensities[i])
            if (intensities[i] > peakVal) break
        }
        val threshold = maxOf(leftMin, rightMin)

        // Find where signal crosses threshold
        var left = peakIdx
        for (i in peakIdx - 1 downTo 0) {
            left = i
            if (intensities[i] <= threshold) break
        }
        var right = peakIdx
        for (i in peakIdx + 1 until points.size) {
            right = i
            if (intensities[i] <= threshold) break
        }
        return left to right
    }

    /**
     * Local minima: find nearest local minimum on each side.
     */
    private fun localMinima(
        points: List<SignalPoint>,
        peakIdx: Int,
        neighborPeaks: List<Int>,
    ): Pair<Int, Int> {
        val intensities = points.map { it.intensity }

        // Left: find first local minimum
        var left = peakIdx
        for (i in peakIdx - 1 downTo 1) {
            if (intensities[i] <= intensities[i - 1] && intensities[i] <= intensities[i + 1]) {
                left = i
                break
            }
            // Stop if we reach a neighbor peak
            if (i in neighborPeaks) {
                left = i
                break
            }
            left = i
        }
        if (left == peakIdx && peakIdx > 0) left = 0

        // Right: find first local minimum
        var right = peakIdx
        for (i in peakIdx + 1 until points.size - 1) {
            if (intensities[i] <= intensities[i - 1] && intensities[i] <= intensities[i + 1]) {
                right = i
                break
            }
            if (i in neighborPeaks) {
                right = i
                break
            }
            right = i
        }
        if (right == peakIdx && peakIdx < points.size - 1) right = points.size - 1

        return left to right
    }

    /**
     * Baseline intersection: find where corrected signal drops to ~0.
     */
    private fun baselineIntersection(points: List<SignalPoint>, peakIdx: Int): Pair<Int, Int> {
        val intensities = points.map { it.intensity }
        val threshold = intensities[peakIdx] * 0.001 // 0.1% of peak height

        var left = 0
        for (i in peakIdx - 1 downTo 0) {
            if (intensities[i] <= threshold) {
                left = i
                break
            }
        }

        var right = points.size - 1
        for (i in peakIdx + 1 until points.size) {
            if (intensities[i] <= threshold) {
                right = i
                break
            }
        }

        return left to right
    }

    /**
     * Percent of height: boundary where signal drops below X% of peak height.
     */
    private fun percentOfHeight(
        points: List<SignalPoint>,
        peakIdx: Int,
        percent: Double,
    ): Pair<Int, Int> {
        val intensities = points.map { it.intensity }
        val threshold = intensities[peakIdx] * percent

        var left = 0
        for (i in peakIdx - 1 downTo 0) {
            if (intensities[i] <= threshold) {
                left = i
                break
            }
        }

        var right = points.size - 1
        for (i in peakIdx + 1 until points.size) {
            if (intensities[i] <= threshold) {
                right = i
                break
            }
        }

        return left to right
    }

    /**
     * Manual: user-specified boundary times.
     */
    private fun manual(
        points: List<SignalPoint>,
        leftTime: Double?,
        rightTime: Double?,
        warnings: MutableList<String>,
    ): Pair<Int, Int> {
        warnings.add("Границы установлены вручную")

        val left = if (leftTime != null) {
            points.indexOfFirst { it.time >= leftTime }.coerceAtLeast(0)
        } else 0

        val right = if (rightTime != null) {
            points.indexOfLast { it.time <= rightTime }.coerceAtLeast(left)
        } else points.size - 1

        return left to right
    }

    // ─── Confidence ─────────────────────────────────────────────

    private fun computeConfidence(
        points: List<SignalPoint>,
        peakIdx: Int,
        leftIdx: Int,
        rightIdx: Int,
        method: BoundaryMethod,
    ): Double {
        val peakIntensity = points[peakIdx].intensity
        if (peakIntensity <= 0.0) return 0.0

        // How close to baseline at boundaries (lower = better)
        val leftRatio = points[leftIdx].intensity / peakIntensity
        val rightRatio = points[rightIdx].intensity / peakIntensity

        val baselineScore = 1.0 - (leftRatio + rightRatio) / 2.0

        // Width adequacy
        val width = rightIdx - leftIdx
        val widthScore = if (width >= 5) 1.0 else width / 5.0

        // Manual gets lower base confidence
        val methodPenalty = if (method == BoundaryMethod.MANUAL) 0.8 else 1.0

        return (baselineScore * 0.6 + widthScore * 0.4) * methodPenalty
    }
}

// ─── Data classes ───────────────────────────────────────────────

enum class BoundaryMethod(val label: String) {
    PROMINENCE_BASES("Prominence bases"),
    LOCAL_MINIMA("Локальные минимумы"),
    BASELINE_INTERSECTION("Пересечение baseline"),
    PERCENT_HEIGHT("% от высоты"),
    MANUAL("Ручные"),
}

data class PeakBoundary(
    val leftIndex: Int,
    val rightIndex: Int,
    val leftTime: Double,
    val rightTime: Double,
    val method: BoundaryMethod,
    val confidence: Double,
    val warnings: List<String>,
) {
    val widthPoints: Int get() = rightIndex - leftIndex
    val widthTime: Double get() = rightTime - leftTime
}

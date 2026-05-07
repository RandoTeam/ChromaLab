package com.chromalab.feature.calculation.algorithm

import com.chromalab.feature.calculation.core.SignalPoint

/**
 * Overlapping / shoulder peak classifier (§2.18).
 *
 * Analyzes pairs of adjacent peaks to determine their overlap status.
 * Does NOT perform deconvolution — that is out of scope for Phase 2.
 *
 * For unresolved peaks:
 * - Area is marked as low confidence
 * - Only manual boundary adjustment is allowed
 *
 * Pure function: deterministic, no side effects.
 */
object PeakOverlapClassifier {

    /**
     * Classify overlap status for a list of detected peaks.
     *
     * @param points     Full signal (baseline-corrected)
     * @param peaks      Accepted peaks sorted by apexTime
     * @param boundaries Boundaries for each peak (same order as peaks)
     * @return List of PeakOverlapInfo for each peak
     */
    fun classify(
        points: List<SignalPoint>,
        peaks: List<PeakCandidate>,
        boundaries: List<PeakBoundary>,
    ): List<PeakOverlapInfo> {
        if (peaks.isEmpty()) return emptyList()
        if (peaks.size == 1) {
            return listOf(
                PeakOverlapInfo(
                    peakIndex = 0,
                    status = OverlapStatus.ISOLATED,
                    warnings = emptyList(),
                )
            )
        }

        val intensities = points.map { it.intensity }
        val results = mutableListOf<PeakOverlapInfo>()

        for (i in peaks.indices) {
            val warnings = mutableListOf<String>()
            var status = OverlapStatus.ISOLATED

            // Check left neighbor
            if (i > 0) {
                val leftStatus = classifyPair(
                    intensities, peaks[i - 1], peaks[i],
                    boundaries.getOrNull(i - 1), boundaries.getOrNull(i),
                    warnings, "левым"
                )
                if (leftStatus.ordinal > status.ordinal) status = leftStatus
            }

            // Check right neighbor
            if (i < peaks.size - 1) {
                val rightStatus = classifyPair(
                    intensities, peaks[i], peaks[i + 1],
                    boundaries.getOrNull(i), boundaries.getOrNull(i + 1),
                    warnings, "правым"
                )
                if (rightStatus.ordinal > status.ordinal) status = rightStatus
            }

            results.add(
                PeakOverlapInfo(
                    peakIndex = i,
                    status = status,
                    warnings = warnings,
                )
            )
        }

        return results
    }

    /**
     * Classify the overlap between two adjacent peaks.
     */
    private fun classifyPair(
        intensities: List<Double>,
        leftPeak: PeakCandidate,
        rightPeak: PeakCandidate,
        leftBoundary: PeakBoundary?,
        rightBoundary: PeakBoundary?,
        warnings: MutableList<String>,
        neighborLabel: String,
    ): OverlapStatus {
        val leftIdx = leftPeak.index
        val rightIdx = rightPeak.index

        // Check boundary overlap
        val boundariesOverlap = if (leftBoundary != null && rightBoundary != null) {
            leftBoundary.rightIndex > rightBoundary.leftIndex
        } else false

        // Find valley between peaks
        val valleyResult = findValley(intensities, leftIdx, rightIdx)

        // Classify
        return when {
            // No valley at all — unresolved
            valleyResult == null -> {
                warnings.add("Нет долины между текущим и $neighborLabel пиком — неразрешённый пик")
                OverlapStatus.UNRESOLVED
            }

            // Valley depth < 20% of smaller peak — shoulder
            valleyResult.depth < 0.2 -> {
                warnings.add(
                    "Плечевой пик рядом с $neighborLabel (глубина долины ${(valleyResult.depth * 100).toInt()}%)"
                )
                OverlapStatus.SHOULDER
            }

            // Boundaries overlap — partially overlapped
            boundariesOverlap -> {
                warnings.add("Границы пересекаются с $neighborLabel пиком")
                OverlapStatus.PARTIALLY_OVERLAPPED
            }

            // Valley depth < 50% — partial overlap
            valleyResult.depth < 0.5 -> {
                warnings.add(
                    "Частичное перекрытие с $neighborLabel пиком (глубина долины ${(valleyResult.depth * 100).toInt()}%)"
                )
                OverlapStatus.PARTIALLY_OVERLAPPED
            }

            else -> OverlapStatus.ISOLATED
        }
    }

    /**
     * Find the valley (minimum) between two peak indices.
     */
    private fun findValley(
        intensities: List<Double>,
        leftPeakIdx: Int,
        rightPeakIdx: Int,
    ): ValleyInfo? {
        if (leftPeakIdx >= rightPeakIdx) return null

        val range = (leftPeakIdx + 1) until rightPeakIdx
        if (range.isEmpty()) return null

        val minIdx = range.minByOrNull { intensities[it] } ?: return null
        val minVal = intensities[minIdx]
        val smallerPeak = minOf(intensities[leftPeakIdx], intensities[rightPeakIdx])

        // Depth = how much the valley drops relative to the smaller peak
        val depth = if (smallerPeak > 0) {
            (smallerPeak - minVal) / smallerPeak
        } else 0.0

        return ValleyInfo(
            index = minIdx,
            intensity = minVal,
            depth = depth,
        )
    }
}

// ─── Data classes ───────────────────────────────────────────────

enum class OverlapStatus(val label: String) {
    ISOLATED("Изолированный"),
    PARTIALLY_OVERLAPPED("Частично перекрытый"),
    SHOULDER("Плечевой"),
    UNRESOLVED("Неразрешённый"),
}

data class PeakOverlapInfo(
    val peakIndex: Int,
    val status: OverlapStatus,
    val warnings: List<String>,
) {
    val isLowConfidenceArea: Boolean
        get() = status == OverlapStatus.SHOULDER || status == OverlapStatus.UNRESOLVED

    val requiresManualBoundaries: Boolean
        get() = status == OverlapStatus.UNRESOLVED
}

private data class ValleyInfo(
    val index: Int,
    val intensity: Double,
    val depth: Double,
)

package com.chromalab.feature.processing.signal

import com.chromalab.feature.processing.calibration.PixelCalibration
import com.chromalab.feature.processing.curve.CurvePoint

/**
 * Converts extracted pixel-space curve points into calibrated time/intensity signal.
 *
 * Pipeline:
 * 1. Sort points by pixelX
 * 2. Apply PixelCalibration → (time, intensity) for each point
 * 3. Sort by time
 * 4. Remove duplicate time values (keep highest confidence)
 * 5. Check for gaps (missing time samples)
 * 6. Build DigitalSignal with metadata
 *
 * Pure Kotlin — no platform dependency.
 */
object SignalConverter {

    /**
     * Convert curve points to digital signal.
     */
    fun convert(
        curvePoints: List<CurvePoint>,
        calibration: PixelCalibration,
        sourceImage: String,
    ): DigitalSignal {
        if (curvePoints.isEmpty()) {
            return emptySignal(calibration, sourceImage)
        }

        // Sort by pixelX
        val sorted = curvePoints.sortedBy { it.pixelX }

        // Apply calibration
        val rawPoints = sorted.mapIndexed { index, cp ->
            val time = calibration.pixelToTime(cp.pixelX.toFloat())
            val intensity = calibration.pixelToIntensity(cp.pixelY)
            GraphPoint(
                index = index,
                pixelX = cp.pixelX,
                pixelY = cp.pixelY,
                time = time,
                intensity = intensity,
                confidence = cp.confidence,
                isInterpolated = cp.confidence == CurvePoint.INTERPOLATED,
            )
        }

        // Sort by time (should already be sorted if calibration is correct)
        val timeSorted = rawPoints.sortedBy { it.time }

        // Remove duplicate time values — keep highest confidence
        val deduped = mutableListOf<GraphPoint>()
        var duplicatesRemoved = 0
        var i = 0
        while (i < timeSorted.size) {
            var j = i + 1
            while (j < timeSorted.size && timeSorted[j].time == timeSorted[i].time) {
                j++
            }
            if (j - i == 1) {
                deduped.add(timeSorted[i])
            } else {
                // Multiple points with same time → keep best confidence
                val best = timeSorted.subList(i, j).maxByOrNull { it.confidence }!!
                deduped.add(best)
                duplicatesRemoved += (j - i - 1)
            }
            i = j
        }

        // Re-index
        val indexed = deduped.mapIndexed { idx, gp -> gp.copy(index = idx) }

        // Check sort validity
        val sortValid = indexed.zipWithNext().all { (a, b) -> a.time <= b.time }

        // Count gaps (where time delta is >2× median delta)
        val gapCount = countGaps(indexed)

        val metadata = SignalMetadata(
            sourceImage = sourceImage,
            totalPoints = indexed.size,
            duplicatesRemoved = duplicatesRemoved,
            gapCount = gapCount,
            sortValid = sortValid,
            timestamp = currentTimeMillis(),
        )

        return DigitalSignal(
            points = indexed,
            timeUnit = calibration.xUnit,
            intensityUnit = calibration.yUnit,
            metadata = metadata,
        )
    }

    /**
     * Count significant gaps in the signal.
     * A gap is where the time delta is >2× the median delta.
     */
    private fun countGaps(points: List<GraphPoint>): Int {
        if (points.size < 3) return 0

        val deltas = points.zipWithNext().map { (a, b) -> b.time - a.time }
        val sortedDeltas = deltas.sorted()
        val median = sortedDeltas[sortedDeltas.size / 2]

        if (median <= 0f) return 0

        return deltas.count { it > median * 2.5f }
    }

    private fun emptySignal(
        calibration: PixelCalibration,
        sourceImage: String,
    ): DigitalSignal = DigitalSignal(
        points = emptyList(),
        timeUnit = calibration.xUnit,
        intensityUnit = calibration.yUnit,
        metadata = SignalMetadata(
            sourceImage = sourceImage,
            totalPoints = 0,
            duplicatesRemoved = 0,
            gapCount = 0,
            sortValid = true,
            timestamp = currentTimeMillis(),
        ),
    )

    /** Cross-platform time */
    private fun currentTimeMillis(): Long = System.currentTimeMillis()
}

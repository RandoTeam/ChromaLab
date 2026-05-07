package com.chromalab.feature.processing.debug

import kotlinx.serialization.Serializable

/**
 * Debug mode configuration.
 * Controlled by user in settings — enables visual overlays and extra logging.
 */
@Serializable
data class DebugConfig(
    /** Master debug toggle */
    val enabled: Boolean = false,
    /** Show contour detection, document boundaries, homography corners */
    val showContours: Boolean = true,
    /** Show detected graph regions and axis lines */
    val showGraphRegions: Boolean = true,
    /** Show curve mask and extraction overlay */
    val showCurveMask: Boolean = true,
    /** Show processing timings */
    val showTimings: Boolean = true,
) {
    companion object {
        val DISABLED = DebugConfig(enabled = false)
        val FULL = DebugConfig(enabled = true)
    }
}

/**
 * Debug timing log for a single processing step.
 */
@Serializable
data class TimingEntry(
    val step: String,
    val durationMs: Long,
)

/**
 * Debug info collected during a pipeline run.
 */
@Serializable
data class DebugInfo(
    val timings: List<TimingEntry> = emptyList(),
    val contourCount: Int = 0,
    val documentCorners: List<Pair<Float, Float>> = emptyList(),
    val graphRegionsDetected: Int = 0,
    val axisLinesDetected: Int = 0,
    val curveMaskPixelCount: Int = 0,
    val extractedPointCount: Int = 0,
    val suppressedBlobCount: Int = 0,
)

/**
 * Collects debug timings during pipeline execution.
 */
class DebugTimer {
    private val entries = mutableListOf<TimingEntry>()
    private var start = 0L

    fun begin(step: String) {
        start = System.currentTimeMillis()
    }

    fun end(step: String) {
        entries.add(TimingEntry(step, System.currentTimeMillis() - start))
    }

    fun getTimings(): List<TimingEntry> = entries.toList()
    fun totalMs(): Long = entries.sumOf { it.durationMs }
}

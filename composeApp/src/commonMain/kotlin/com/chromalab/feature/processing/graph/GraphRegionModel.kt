package com.chromalab.feature.processing.graph

import com.chromalab.feature.processing.pipeline.DetectionMethod
import kotlinx.serialization.Serializable

/**
 * A rectangular region of interest in image coordinates.
 */
@Serializable
data class GraphRegion(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val label: String = "",
) {
    val right: Int get() = x + width
    val bottom: Int get() = y + height
    val area: Int get() = width * height
    val aspectRatio: Float get() = if (height > 0) width.toFloat() / height else 0f
}

/**
 * Confidence level for auto-detected graph region.
 */
enum class DetectionConfidence {
    /** Strong: clear axes, rectangular region, high contrast graph area */
    HIGH,
    /** Moderate: some features found, reasonable guess */
    MEDIUM,
    /** Low: weak signal, fallback heuristics used */
    LOW,
    /** Manual: user selected the region */
    MANUAL,
}

/**
 * Result of graph region detection.
 * Design principle: NEVER block the user.
 * - If auto-detection succeeds → show result with confidence
 * - If auto-detection partially succeeds → show best guess + suggest manual adjustment
 * - If auto-detection fails → offer manual selection (full image as fallback)
 */
@Serializable
data class GraphRegionResult(
    val regions: List<GraphRegion>,
    val selectedIndex: Int = 0,
    val detectionMethod: DetectionMethod,
    val confidence: DetectionConfidence,
    val imageWidth: Int,
    val imageHeight: Int,
    val graphImagePath: String? = null,
    val warnings: List<String> = emptyList(),
    val timestamp: Long,
) {
    val selectedRegion: GraphRegion?
        get() {
            if (regions.isEmpty()) return null
            // Select topmost region that looks like a real graph.
            // Filter out text headers, axis labels, and thin strips:
            // 1. Aspect ratio < 3.5 — real graphs are roughly 2:1, not 4:1+
            // 2. Min height: at least 15% of image height
            // 3. Min area: at least 5% of image area
            val minHeight = (imageHeight * 0.15f).toInt()
            val minArea = (imageWidth.toLong() * imageHeight * 0.05f).toInt()
            val graphs = sortedRegions.filter { r ->
                r.aspectRatio < 3.5f &&
                    r.height >= minHeight &&
                    r.area >= minArea
            }
            return graphs.firstOrNull() ?: sortedRegions.first()
        }

    /** Regions sorted top-to-bottom (by Y coordinate) — natural reading order */
    val sortedRegions: List<GraphRegion>
        get() = regions.sortedBy { it.y }

    /** Regions that pass quality filter — only real graph areas, no headers/labels */
    val filteredRegions: List<GraphRegion>
        get() {
            val minHeight = (imageHeight * 0.15f).toInt()
            val minArea = (imageWidth.toLong() * imageHeight * 0.05f).toInt()
            val filtered = sortedRegions.filter { r ->
                r.aspectRatio < 3.5f &&
                    r.height >= minHeight &&
                    r.area >= minArea
            }
            return filtered.ifEmpty { sortedRegions }
        }

    /** Always true — we never block the user */
    val canProceed: Boolean get() = true

    /** If no region found, the entire image is the fallback */
    val effectiveRegion: GraphRegion
        get() = selectedRegion ?: GraphRegion(0, 0, imageWidth, imageHeight, "Всё изображение")
}

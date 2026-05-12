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
    val centerY: Float get() = y + height / 2f
}

@Serializable
data class GraphRegionQuality(
    val region: GraphRegion,
    val accepted: Boolean,
    val widthRatio: Float,
    val heightRatio: Float,
    val areaRatio: Float,
    val aspectRatio: Float,
    val rejectionReasons: List<String> = emptyList(),
)

private const val MIN_WIDTH_RATIO = 0.25f
private const val MIN_HEIGHT_RATIO = 0.12f
private const val MIN_AREA_RATIO = 0.04f
private const val MAX_AREA_RATIO = 0.92f
private const val MAX_LOW_CONFIDENCE_AREA_RATIO = 0.55f
private const val MIN_ASPECT_RATIO = 0.65f
private const val MAX_ASPECT_RATIO = 5.0f
private const val SAME_ROW_MIN_VERTICAL_OVERLAP = 0.45f

private fun GraphRegion.evaluateQuality(
    imageWidth: Int,
    imageHeight: Int,
    confidence: DetectionConfidence,
): GraphRegionQuality {
    val safeWidth = imageWidth.coerceAtLeast(1)
    val safeHeight = imageHeight.coerceAtLeast(1)
    val imageArea = (safeWidth.toFloat() * safeHeight.toFloat()).coerceAtLeast(1f)
    val widthRatio = width.toFloat() / safeWidth
    val heightRatio = height.toFloat() / safeHeight
    val areaRatio = area.toFloat() / imageArea
    val reasons = mutableListOf<String>()

    if (x < 0 || y < 0 || right > safeWidth || bottom > safeHeight) reasons.add("out_of_image_bounds")
    if (widthRatio < MIN_WIDTH_RATIO) reasons.add("too_narrow")
    if (heightRatio < MIN_HEIGHT_RATIO) reasons.add("too_short")
    if (areaRatio < MIN_AREA_RATIO) reasons.add("area_too_small")
    if (areaRatio > MAX_AREA_RATIO) reasons.add("area_too_large")
    if (confidence == DetectionConfidence.LOW && areaRatio > MAX_LOW_CONFIDENCE_AREA_RATIO) {
        reasons.add("low_confidence_broad_region")
    }
    if (aspectRatio !in MIN_ASPECT_RATIO..MAX_ASPECT_RATIO) reasons.add("implausible_aspect_ratio")

    return GraphRegionQuality(
        region = this,
        accepted = reasons.isEmpty(),
        widthRatio = widthRatio,
        heightRatio = heightRatio,
        areaRatio = areaRatio,
        aspectRatio = aspectRatio,
        rejectionReasons = reasons,
    )
}

private class GraphRegionRow {
    val regions: MutableList<GraphRegion> = mutableListOf()
    var top: Int = Int.MAX_VALUE
        private set
    var bottom: Int = Int.MIN_VALUE
        private set

    val left: Int
        get() = regions.minOfOrNull { it.x } ?: 0

    fun add(region: GraphRegion) {
        regions += region
        top = minOf(top, region.y)
        bottom = maxOf(bottom, region.bottom)
    }

    fun belongsToSameRow(region: GraphRegion): Boolean {
        val overlap = minOf(bottom, region.bottom) - maxOf(top, region.y)
        if (overlap <= 0) return false

        val rowHeight = (bottom - top).coerceAtLeast(1)
        val smallerHeight = minOf(rowHeight, region.height.coerceAtLeast(1))
        val overlapRatio = overlap.toFloat() / smallerHeight.toFloat()
        return overlapRatio >= SAME_ROW_MIN_VERTICAL_OVERLAP
    }
}

private fun List<GraphRegion>.sortedByNaturalReadingOrder(): List<GraphRegion> {
    if (size <= 1) return this

    val rows = mutableListOf<GraphRegionRow>()
    sortedWith(compareBy<GraphRegion> { it.centerY }.thenBy { it.x }).forEach { region ->
        val row = rows.firstOrNull { it.belongsToSameRow(region) } ?: GraphRegionRow().also { rows += it }
        row.add(region)
    }

    return rows
        .sortedWith(compareBy<GraphRegionRow> { it.top }.thenBy { it.left })
        .flatMap { row -> row.regions.sortedWith(compareBy<GraphRegion> { it.x }.thenBy { it.y }) }
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
    val qualityEvaluations: List<GraphRegionQuality>
        get() = sortedRegions.map { region ->
            region.evaluateQuality(imageWidth, imageHeight, confidence)
        }

    val selectedRegion: GraphRegion?
        get() {
            if (regions.isEmpty()) return null
            return filteredRegions.firstOrNull() ?: sortedRegions.first()
        }

    /** Regions sorted in natural reading order: rows top-to-bottom, items left-to-right. */
    val sortedRegions: List<GraphRegion>
        get() = regions.sortedByNaturalReadingOrder()

    /** Regions that pass quality filters; fallback/manual guesses are not promoted here. */
    val filteredRegions: List<GraphRegion>
        get() = qualityEvaluations
            .filter { it.accepted }
            .map { it.region }

    /** Always true — we never block the user */
    val canProceed: Boolean get() = true

    /** If no region found, the entire image is the fallback */
    val effectiveRegion: GraphRegion
        get() = selectedRegion ?: GraphRegion(0, 0, imageWidth, imageHeight, "Всё изображение")
}

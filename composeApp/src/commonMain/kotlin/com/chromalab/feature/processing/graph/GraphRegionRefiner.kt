package com.chromalab.feature.processing.graph

import kotlin.math.max
import kotlin.math.roundToInt

data class GraphRegionRefinementResult(
    val originalRegion: GraphRegion,
    val refinedRegion: GraphRegion,
    val changed: Boolean,
    val areaReductionRatio: Float,
    val warnings: List<String> = emptyList(),
)

class GraphRegionRefiner {
    private val sampler = GraphRegionRefinementSampler()

    fun refine(
        imagePath: String,
        region: GraphRegion,
        imageWidth: Int,
        imageHeight: Int,
    ): GraphRegionRefinementResult {
        if (!region.shouldRefine(imageWidth, imageHeight)) {
            return GraphRegionRefinementResult(
                originalRegion = region,
                refinedRegion = region,
                changed = false,
                areaReductionRatio = 0f,
            )
        }

        val sampleResult = sampler.sample(imagePath, region)
        val sample = sampleResult.sample
            ?: return region.edgeTrimmedResult(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                warning = sampleResult.warning ?: "graph_refine.image_not_readable",
            )

        val refined = sample.refineRegion(region, imageWidth, imageHeight)
            ?: return region.edgeTrimmedResult(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                warning = "graph_refine.edge_trim_applied_after_unstable_content_bounds",
            )
        val areaReduction = 1f - refined.area.toFloat() / region.area.coerceAtLeast(1).toFloat()
        if (areaReduction < 0.08f) {
            return region.edgeTrimmedResult(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                warning = "graph_refine.edge_trim_applied_after_small_content_reduction",
                fallbackReduction = areaReduction.coerceAtLeast(0f),
            )
        }

        return GraphRegionRefinementResult(
            originalRegion = region,
            refinedRegion = refined,
            changed = true,
            areaReductionRatio = areaReduction.coerceIn(0f, 1f),
            warnings = listOf("graph_refine.applied"),
        )
    }
}

private fun GraphRegion.edgeTrimmedResult(
    imageWidth: Int,
    imageHeight: Int,
    warning: String,
    fallbackReduction: Float = 0f,
): GraphRegionRefinementResult {
    val trimmed = edgeTrimmed(imageWidth, imageHeight) ?: return GraphRegionRefinementResult(
        originalRegion = this,
        refinedRegion = this,
        changed = false,
        areaReductionRatio = fallbackReduction,
        warnings = listOf(warning, "graph_refine.edge_trim_not_available"),
    )
    return GraphRegionRefinementResult(
        originalRegion = this,
        refinedRegion = trimmed,
        changed = true,
        areaReductionRatio = trimmed.areaReductionFrom(this),
        warnings = listOf(warning),
    )
}

internal expect class GraphRegionRefinementSampler() {
    fun sample(
        imagePath: String,
        region: GraphRegion,
    ): GraphRegionRefinementSampleResult
}

internal data class GraphRegionRefinementSampleResult(
    val sample: GraphRegionRefinementSample?,
    val warning: String? = null,
)

internal data class GraphRegionRefinementSample(
    val width: Int,
    val height: Int,
    val gray: IntArray,
)

internal fun Int.toGraphRefinementGray(): Int =
    (
        0.299 * ((this shr 16) and 0xFF) +
            0.587 * ((this shr 8) and 0xFF) +
            0.114 * (this and 0xFF)
        ).toInt()

private fun GraphRegion.shouldRefine(imageWidth: Int, imageHeight: Int): Boolean {
    val safeArea = (imageWidth.toFloat() * imageHeight.toFloat()).coerceAtLeast(1f)
    val areaRatio = area.toFloat() / safeArea
    val edgeContacts = listOf(x <= 0, y <= 0, right >= imageWidth, bottom >= imageHeight).count { it }
    val fullImage = x == 0 && y == 0 && width == imageWidth && height == imageHeight
    val largeImage = max(imageWidth, imageHeight) >= 700
    return (fullImage && largeImage) || (!fullImage && areaRatio >= 0.45f && edgeContacts >= 2)
}

private fun GraphRegion.edgeTrimmed(imageWidth: Int, imageHeight: Int): GraphRegion? {
    val trimX = (width * 0.045f).roundToInt().coerceAtLeast(4)
    val trimY = (height * 0.040f).roundToInt().coerceAtLeast(4)
    val left = if (x <= 0) trimX else 0
    val top = if (y <= 0) trimY else 0
    val right = if (right >= imageWidth) trimX else 0
    val bottom = if (bottom >= imageHeight) trimY else 0
    if (left == 0 && top == 0 && right == 0 && bottom == 0) return null
    val nextWidth = width - left - right
    val nextHeight = height - top - bottom
    if (nextWidth < width * 0.70f || nextHeight < height * 0.70f) return null
    if (nextWidth <= 1 || nextHeight <= 1) return null
    val trimmed = copy(
        x = x + left,
        y = y + top,
        width = nextWidth,
        height = nextHeight,
        label = label.ifBlank { "Edge-trimmed graph region" },
    ).clampToImage(imageWidth, imageHeight)
    val reduction = trimmed.areaReductionFrom(this)
    return if (reduction >= 0.08f) trimmed else null
}

private fun GraphRegion.areaReductionFrom(original: GraphRegion): Float =
    (1f - area.toFloat() / original.area.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

private fun GraphRegionRefinementSample.refineRegion(
    sourceRegion: GraphRegion,
    imageWidth: Int,
    imageHeight: Int,
): GraphRegion? {
    if (width <= 8 || height <= 8 || gray.size != width * height) return null

    val threshold = estimateInkThreshold(gray)
    val rowInk = IntArray(height) { y ->
        (0 until width).count { x -> gray[y * width + x] < threshold }
    }
    val columnInk = IntArray(width) { x ->
        (0 until height).count { y -> gray[y * width + x] < threshold }
    }
    val xGuard = (width * 0.045f).roundToInt().coerceAtLeast(4)
    val yGuard = (height * 0.040f).roundToInt().coerceAtLeast(4)
    val xSearchStart = if (sourceRegion.x <= 0) xGuard else 0
    val xSearchEnd = if (sourceRegion.right >= imageWidth) width - xGuard - 1 else width - 1
    val ySearchStart = if (sourceRegion.y <= 0) yGuard else 0
    val ySearchEnd = if (sourceRegion.bottom >= imageHeight) height - yGuard - 1 else height - 1

    val rowBounds = rowInk.findContentBounds(
        minInk = (width * 0.030f).roundToInt().coerceAtLeast(3),
        startIndex = ySearchStart,
        endIndex = ySearchEnd,
    )
        ?: rowInk.findContentBounds(
            minInk = (width * 0.015f).roundToInt().coerceAtLeast(2),
            startIndex = ySearchStart,
            endIndex = ySearchEnd,
        )
        ?: return null
    val columnBounds = columnInk.findContentBounds(
        minInk = (height * 0.018f).roundToInt().coerceAtLeast(3),
        startIndex = xSearchStart,
        endIndex = xSearchEnd,
    )
        ?: columnInk.findContentBounds(
            minInk = (height * 0.010f).roundToInt().coerceAtLeast(2),
            startIndex = xSearchStart,
            endIndex = xSearchEnd,
        )
        ?: return null

    val padX = (width * 0.025f).roundToInt().coerceAtLeast(4)
    val padY = (height * 0.025f).roundToInt().coerceAtLeast(4)
    val left = (columnBounds.first - padX).coerceAtLeast(0)
    val top = (rowBounds.first - padY).coerceAtLeast(0)
    val right = (columnBounds.last + padX + 1).coerceAtMost(width)
    val bottom = (rowBounds.last + padY + 1).coerceAtMost(height)
    if (right - left < width * 0.25f || bottom - top < height * 0.18f) return null

    return GraphRegion(
        x = sourceRegion.x + left,
        y = sourceRegion.y + top,
        width = right - left,
        height = bottom - top,
        label = sourceRegion.label.ifBlank { "Refined graph region" },
    ).clampToImage(imageWidth, imageHeight)
}

private fun estimateInkThreshold(gray: IntArray): Int {
    val sorted = gray.copyOf().also { it.sort() }
    val percentile = sorted[(sorted.lastIndex * 0.24f).roundToInt()]
    return percentile.coerceIn(95, 190)
}

private fun IntArray.findContentBounds(
    minInk: Int,
    startIndex: Int,
    endIndex: Int,
): IntRange? {
    val smoothed = smooth(radius = 2)
    val safeStart = startIndex.coerceIn(indices)
    val safeEnd = endIndex.coerceIn(safeStart, lastIndex)
    val first = (safeStart..safeEnd).firstOrNull { smoothed[it] >= minInk } ?: -1
    val last = (safeEnd downTo safeStart).firstOrNull { smoothed[it] >= minInk } ?: -1
    return if (first >= 0 && last > first) first..last else null
}

private fun IntArray.smooth(radius: Int): IntArray =
    IntArray(size) { index ->
        var sum = 0
        var count = 0
        for (offset in -radius..radius) {
            val current = index + offset
            if (current in indices) {
                sum += this[current]
                count++
            }
        }
        sum / count.coerceAtLeast(1)
    }

private fun GraphRegion.clampToImage(imageWidth: Int, imageHeight: Int): GraphRegion {
    val clampedX = x.coerceIn(0, (imageWidth - 1).coerceAtLeast(0))
    val clampedY = y.coerceIn(0, (imageHeight - 1).coerceAtLeast(0))
    val clampedRight = right.coerceIn(clampedX + 1, imageWidth.coerceAtLeast(clampedX + 1))
    val clampedBottom = bottom.coerceIn(clampedY + 1, imageHeight.coerceAtLeast(clampedY + 1))
    return copy(
        x = clampedX,
        y = clampedY,
        width = clampedRight - clampedX,
        height = clampedBottom - clampedY,
    )
}

package com.chromalab.feature.processing.graph

import kotlin.math.roundToInt

data class GraphRegionBoundaryCorrectionResult(
    val originalRegion: GraphRegion,
    val correctedRegion: GraphRegion,
    val changed: Boolean,
    val warnings: List<String> = emptyList(),
)

class GraphRegionBoundaryCorrector {
    private val sampler = GraphRegionRefinementSampler()

    fun correct(
        imagePath: String,
        region: GraphRegion,
        imageWidth: Int,
        imageHeight: Int,
        preservePanelLabels: Boolean = false,
    ): GraphRegionBoundaryCorrectionResult {
        val searchRegion = region.axisSearchRegion(imageWidth, imageHeight)
        val sampleResult = sampler.sample(imagePath, searchRegion)
        val sample = sampleResult.sample ?: return GraphRegionBoundaryCorrectionResult(
            originalRegion = region,
            correctedRegion = region,
            changed = false,
            warnings = listOf(sampleResult.warning ?: "graph_boundary.image_not_readable"),
        )

        val bounds = sample.findAxisAlignedBounds(
            preservePanelLabels = preservePanelLabels,
        ) ?: return GraphRegionBoundaryCorrectionResult(
            originalRegion = region,
            correctedRegion = region,
            changed = false,
            warnings = listOf("graph_boundary.axis_bounds_not_found"),
        )

        val axisRegion = GraphRegion(
            x = searchRegion.x + bounds.left,
            y = searchRegion.y + bounds.top,
            width = bounds.right - bounds.left,
            height = bounds.bottom - bounds.top,
            label = region.label.ifBlank { "Axis corrected graph" },
        ).clampToImage(imageWidth, imageHeight)

        val corrected = region.union(axisRegion).clampToImage(imageWidth, imageHeight)
        val leftRecovery = region.x - corrected.x
        val topRecovery = region.y - corrected.y
        val significantRecovery = leftRecovery >= (region.width * 0.08f).roundToInt().coerceAtLeast(12) ||
            topRecovery >= (region.height * 0.04f).roundToInt().coerceAtLeast(12)
        if (!significantRecovery) {
            return GraphRegionBoundaryCorrectionResult(
                originalRegion = region,
                correctedRegion = region,
                changed = false,
            )
        }

        val changed = corrected != region
        val warnings = buildList {
            if (changed) add("graph_boundary.axis_aligned_expansion_applied")
            if (corrected.x < region.x) add("graph_boundary.expanded_left_to_axis")
            if (corrected.y < region.y) add("graph_boundary.expanded_top_to_axis")
            if (corrected.right > region.right) add("graph_boundary.expanded_right_to_axis")
            if (corrected.bottom > region.bottom) add("graph_boundary.expanded_bottom_to_axis")
        }

        return GraphRegionBoundaryCorrectionResult(
            originalRegion = region,
            correctedRegion = corrected,
            changed = changed,
            warnings = warnings,
        )
    }
}

private data class AxisAlignedBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

private data class AxisHorizontalRun(
    val row: Int,
    val startX: Int,
    val endX: Int,
) {
    val length: Int get() = endX - startX + 1
}

private data class AxisVerticalRun(
    val x: Int,
    val startY: Int,
    val endY: Int,
) {
    val length: Int get() = endY - startY + 1
}

private fun GraphRegion.axisSearchRegion(imageWidth: Int, imageHeight: Int): GraphRegion {
    val leftPad = (width * 0.55f).roundToInt().coerceAtLeast((imageWidth * 0.08f).roundToInt())
    val rightPad = (width * 0.20f).roundToInt().coerceAtLeast((imageWidth * 0.04f).roundToInt())
    val topPad = (height * 0.22f).roundToInt().coerceAtLeast((imageHeight * 0.04f).roundToInt())
    val bottomPad = (height * 0.12f).roundToInt().coerceAtLeast((imageHeight * 0.025f).roundToInt())
    return GraphRegion(
        x = x - leftPad,
        y = y - topPad,
        width = width + leftPad + rightPad,
        height = height + topPad + bottomPad,
        label = label,
    ).clampToImage(imageWidth, imageHeight)
}

private fun GraphRegionRefinementSample.findAxisAlignedBounds(
    preservePanelLabels: Boolean,
): AxisAlignedBounds? {
    if (width <= 16 || height <= 16 || gray.size != width * height) return null

    val threshold = estimateAxisThreshold()
    val horizontalAxis = findHorizontalAxisRun(threshold) ?: return null
    val verticalAxis = findVerticalAxisRun(
        threshold = threshold,
        horizontalAxis = horizontalAxis,
    ) ?: return null

    val padX = (width * 0.010f).roundToInt().coerceAtLeast(3)
    val padY = (height * 0.012f).roundToInt().coerceAtLeast(3)
    val axisLeft = minOf(horizontalAxis.startX, verticalAxis.x)
    val panelLeft = if (preservePanelLabels) {
        findLeftPanelBoundary(
            threshold = threshold,
            axisLeft = axisLeft,
            top = verticalAxis.startY,
            bottom = maxOf(horizontalAxis.row, verticalAxis.endY),
        )
    } else {
        axisLeft
    }
    val left = panelLeft - padX
    val top = verticalAxis.startY - padY
    val right = horizontalAxis.endX + padX + 1
    val bottom = maxOf(horizontalAxis.row, verticalAxis.endY) + padY + 1

    if (right - left < width * 0.30f || bottom - top < height * 0.22f) return null
    return AxisAlignedBounds(
        left = left.coerceAtLeast(0),
        top = top.coerceAtLeast(0),
        right = right.coerceAtMost(width),
        bottom = bottom.coerceAtMost(height),
    )
}

private fun GraphRegionRefinementSample.findLeftPanelBoundary(
    threshold: Int,
    axisLeft: Int,
    top: Int,
    bottom: Int,
): Int {
    val safeAxisLeft = axisLeft.coerceIn(0, width - 1)
    val safeTop = top.coerceIn(0, height - 1)
    val safeBottom = bottom.coerceIn(safeTop, height - 1)
    val minInk = (safeBottom - safeTop + 1)
        .times(0.004f)
        .roundToInt()
        .coerceIn(2, 9)
    val activeColumns = BooleanArray(safeAxisLeft + 1) { x ->
        var count = 0
        for (y in safeTop..safeBottom) {
            if (gray[y * width + x] < threshold) count++
        }
        count >= minInk
    }.closeSmallColumnGaps(radius = 2)

    var leftMost = safeAxisLeft
    var gap = 0
    val maxGap = (width * 0.020f).roundToInt().coerceIn(10, 24)
    var x = safeAxisLeft
    while (x >= 0) {
        if (activeColumns[x]) {
            leftMost = x
            gap = 0
        } else {
            gap++
            if (safeAxisLeft - x > 12 && gap > maxGap) break
        }
        x--
    }
    return leftMost
}

private fun GraphRegionRefinementSample.findHorizontalAxisRun(threshold: Int): AxisHorizontalRun? {
    val yStart = (height * 0.42f).roundToInt().coerceIn(0, height - 1)
    val yEnd = (height * 0.97f).roundToInt().coerceIn(yStart, height - 1)
    val minRun = (width * 0.45f).roundToInt().coerceAtLeast(80).coerceAtMost(width)
    val maxGap = (width * 0.006f).roundToInt().coerceIn(2, 8)
    var best: AxisHorizontalRun? = null

    for (y in yStart..yEnd) {
        val run = longestDarkRunInRow(y, threshold, maxGap)
        if (run.length >= minRun && run.length > (best?.length ?: 0)) {
            best = AxisHorizontalRun(row = y, startX = run.startX, endX = run.endX)
        }
    }

    return best
}

private fun GraphRegionRefinementSample.findVerticalAxisRun(
    threshold: Int,
    horizontalAxis: AxisHorizontalRun,
): AxisVerticalRun? {
    val searchStart = (horizontalAxis.startX - width * 0.03f).roundToInt().coerceAtLeast(0)
    val searchEnd = (horizontalAxis.startX + horizontalAxis.length * 0.30f)
        .roundToInt()
        .coerceIn(searchStart, width - 1)
    val endTolerance = (height * 0.045f).roundToInt().coerceAtLeast(6)
    val minRun = (height * 0.24f).roundToInt().coerceAtLeast(48)
    val maxGap = (height * 0.010f).roundToInt().coerceIn(2, 8)
    val candidates = mutableListOf<AxisVerticalRun>()

    for (x in searchStart..searchEnd) {
        val run = longestDarkRunEndingNear(
            x = x,
            threshold = threshold,
            targetY = horizontalAxis.row,
            endTolerance = endTolerance,
            maxGap = maxGap,
        )
        if (run != null && run.length >= minRun) {
            candidates += run
        }
    }
    if (candidates.isEmpty()) return null

    val maxLength = candidates.maxOf { it.length }
    return candidates
        .filter { it.length >= (maxLength * 0.60f).roundToInt().coerceAtLeast(minRun) }
        .minWithOrNull(compareBy<AxisVerticalRun> { it.x }.thenByDescending { it.length })
}

private data class RowRun(val startX: Int, val endX: Int) {
    val length: Int get() = endX - startX + 1
}

private fun GraphRegionRefinementSample.longestDarkRunInRow(
    y: Int,
    threshold: Int,
    maxGap: Int,
): RowRun {
    var bestStart = 0
    var bestEnd = -1
    var start = -1
    var lastDark = -1
    var gap = 0

    for (x in 0 until width) {
        if (gray[y * width + x] < threshold) {
            if (start < 0) start = x
            lastDark = x
            gap = 0
        } else if (start >= 0) {
            gap++
            if (gap > maxGap) {
                if (lastDark - start > bestEnd - bestStart) {
                    bestStart = start
                    bestEnd = lastDark
                }
                start = -1
                lastDark = -1
                gap = 0
            }
        }
    }
    if (start >= 0 && lastDark - start > bestEnd - bestStart) {
        bestStart = start
        bestEnd = lastDark
    }
    return RowRun(bestStart, bestEnd)
}

private fun GraphRegionRefinementSample.longestDarkRunEndingNear(
    x: Int,
    threshold: Int,
    targetY: Int,
    endTolerance: Int,
    maxGap: Int,
): AxisVerticalRun? {
    var best: AxisVerticalRun? = null
    var y = 0
    val bottomLimit = (targetY + endTolerance).coerceAtMost(height - 1)

    while (y <= bottomLimit) {
        while (y <= bottomLimit && gray[y * width + x] >= threshold) y++
        if (y > bottomLimit) break

        val start = y
        var lastDark = y
        var gap = 0
        while (y <= bottomLimit && gap <= maxGap) {
            if (gray[y * width + x] < threshold) {
                lastDark = y
                gap = 0
            } else {
                gap++
            }
            y++
        }
        if (lastDark >= targetY - endTolerance) {
            val candidate = AxisVerticalRun(x = x, startY = start, endY = minOf(lastDark, targetY))
            if (candidate.length > (best?.length ?: 0)) best = candidate
        }
    }

    return best
}

private fun GraphRegionRefinementSample.estimateAxisThreshold(): Int {
    val sorted = gray.copyOf().also { it.sort() }
    val percentile = sorted[(sorted.lastIndex * 0.20f).roundToInt()]
    return percentile.coerceIn(95, 185)
}

private fun BooleanArray.closeSmallColumnGaps(radius: Int): BooleanArray =
    BooleanArray(size) { index ->
        if (this[index]) {
            true
        } else {
            val start = (index - radius).coerceAtLeast(0)
            val end = (index + radius).coerceAtMost(lastIndex)
            (start..end).any { this[it] }
        }
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

private fun GraphRegion.union(other: GraphRegion): GraphRegion {
    val left = minOf(x, other.x)
    val top = minOf(y, other.y)
    val right = maxOf(right, other.right)
    val bottom = maxOf(bottom, other.bottom)
    return copy(
        x = left,
        y = top,
        width = right - left,
        height = bottom - top,
    )
}

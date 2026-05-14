package com.chromalab.feature.processing.graph

import kotlin.math.roundToInt

fun GraphRegion.requiresGraphPanelBoundaryMode(imageWidth: Int, imageHeight: Int): Boolean {
    if (maxOf(imageWidth, imageHeight) < 700) return false
    val safeArea = (imageWidth.toFloat() * imageHeight.toFloat()).coerceAtLeast(1f)
    val areaRatio = area.toFloat() / safeArea
    val widthRatio = width.toFloat() / imageWidth.coerceAtLeast(1)
    val heightRatio = height.toFloat() / imageHeight.coerceAtLeast(1)
    return areaRatio >= 0.42f && widthRatio >= 0.70f && heightRatio >= 0.25f
}

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
        val searchRegion = region.axisSearchRegion(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            preservePanelLabels = preservePanelLabels,
        )
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

        val detectedAxisRegion = GraphRegion(
            x = searchRegion.x + bounds.left,
            y = searchRegion.y + bounds.top,
            width = bounds.right - bounds.left,
            height = bounds.bottom - bounds.top,
            label = region.label.ifBlank { "Axis corrected graph" },
        ).clampToImage(imageWidth, imageHeight)
        val broadPanelCandidate = region.requiresGraphPanelBoundaryMode(imageWidth, imageHeight)
        val topAdjustedRegion = if (
            preservePanelLabels &&
            broadPanelCandidate &&
            detectedAxisRegion.y < region.y
        ) {
            detectedAxisRegion.withTop(region.y).clampToImage(imageWidth, imageHeight)
        } else {
            detectedAxisRegion
        }
        val axisRegion = if (
            preservePanelLabels &&
            !broadPanelCandidate &&
            topAdjustedRegion.right > region.right
        ) {
            topAdjustedRegion.withRight(region.right).clampToImage(imageWidth, imageHeight)
        } else {
            topAdjustedRegion
        }

        val corrected = if (preservePanelLabels) {
            axisRegion
        } else {
            region.union(axisRegion).clampToImage(imageWidth, imageHeight)
        }
        val leftRecovery = region.x - corrected.x
        val topRecovery = region.y - corrected.y
        val significantRecovery = leftRecovery >= (region.width * 0.08f).roundToInt().coerceAtLeast(12) ||
            topRecovery >= (region.height * 0.04f).roundToInt().coerceAtLeast(12)
        if (!preservePanelLabels && !significantRecovery) {
            return GraphRegionBoundaryCorrectionResult(
                originalRegion = region,
                correctedRegion = region,
                changed = false,
            )
        }

        val changed = corrected != region
        val warnings = buildList {
            if (preservePanelLabels && changed) add("graph_boundary.panel_bounds_applied")
            if (changed) add("graph_boundary.axis_aligned_expansion_applied")
            if (corrected.x < region.x) add("graph_boundary.expanded_left_to_axis")
            if (corrected.y < region.y) add("graph_boundary.expanded_top_to_axis")
            if (corrected.right > region.right) add("graph_boundary.expanded_right_to_axis")
            if (corrected.bottom > region.bottom) add("graph_boundary.expanded_bottom_to_axis")
            if (corrected.x > region.x) add("graph_boundary.trimmed_left_to_panel")
            if (corrected.y > region.y) add("graph_boundary.trimmed_top_to_panel")
            if (corrected.right < region.right) add("graph_boundary.trimmed_right_to_panel")
            if (corrected.bottom < region.bottom) add("graph_boundary.trimmed_bottom_to_panel")
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

private fun GraphRegion.axisSearchRegion(
    imageWidth: Int,
    imageHeight: Int,
    preservePanelLabels: Boolean,
): GraphRegion {
    val leftPadRatio = if (preservePanelLabels) 0.65f else 0.55f
    val rightPadRatio = if (preservePanelLabels) 0.24f else 0.20f
    val topPadRatio = if (preservePanelLabels) 0.48f else 0.22f
    val bottomPadRatio = if (preservePanelLabels) 0.18f else 0.12f
    val leftPad = (width * leftPadRatio).roundToInt().coerceAtLeast((imageWidth * 0.08f).roundToInt())
    val rightPad = (width * rightPadRatio).roundToInt().coerceAtLeast((imageWidth * 0.04f).roundToInt())
    val topPad = (height * topPadRatio).roundToInt().coerceAtLeast((imageHeight * 0.04f).roundToInt())
    val bottomPad = (height * bottomPadRatio).roundToInt().coerceAtLeast((imageHeight * 0.025f).roundToInt())
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
    val horizontalAxis = findHorizontalAxisRun(
        threshold = threshold,
        preservePanelLabels = preservePanelLabels,
    ) ?: return null
    val verticalAxis = findVerticalAxisRun(
        threshold = threshold,
        horizontalAxis = horizontalAxis,
        preservePanelLabels = preservePanelLabels,
    ) ?: return null

    val padX = (width * 0.010f).roundToInt().coerceAtLeast(3)
    val padY = (height * 0.012f).roundToInt().coerceAtLeast(3)
    val panelTopPad = if (preservePanelLabels) {
        (height * 0.050f).roundToInt().coerceAtLeast(14)
    } else {
        padY
    }
    val panelBottomPad = if (preservePanelLabels) {
        (height * 0.070f).roundToInt().coerceAtLeast(18)
    } else {
        padY
    }
    val axisLeft = minOf(horizontalAxis.startX, verticalAxis.x)
    val signalTop = if (preservePanelLabels) {
        findSignalTopBoundary(
            threshold = threshold,
            left = axisLeft,
            right = horizontalAxis.endX,
            axisRow = horizontalAxis.row,
        )
    } else {
        null
    }
    val axisBottom = maxOf(horizontalAxis.row, verticalAxis.endY)
    val contentTop = minOf(verticalAxis.startY, signalTop ?: verticalAxis.startY)
    val panelLeft = if (preservePanelLabels) {
        findLeftPanelBoundary(
            threshold = threshold,
            axisLeft = axisLeft,
            top = contentTop,
            bottom = axisBottom,
        )
    } else {
        axisLeft
    }
    val panelRight = if (preservePanelLabels) {
        findRightPanelBoundary(
            threshold = threshold,
            axisLeft = axisLeft,
            seedRight = horizontalAxis.endX,
            top = contentTop,
            bottom = axisBottom,
        )
    } else {
        horizontalAxis.endX
    }
    val left = panelLeft - padX
    val top = contentTop - panelTopPad
    val right = panelRight + padX + 1
    val bottom = axisBottom + panelBottomPad + 1

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
    val minPanelLeft = minOf(safeAxisLeft, (width * 0.040f).roundToInt().coerceAtLeast(10))
    return leftMost.coerceAtLeast(minPanelLeft)
}

private fun GraphRegionRefinementSample.findRightPanelBoundary(
    threshold: Int,
    axisLeft: Int,
    seedRight: Int,
    top: Int,
    bottom: Int,
): Int {
    val safeAxisLeft = axisLeft.coerceIn(0, width - 1)
    val safeSeedRight = seedRight.coerceIn(safeAxisLeft, width - 1)
    val safeTop = top.coerceIn(0, height - 1)
    val safeBottom = bottom.coerceIn(safeTop, height - 1)
    val maxRight = (safeSeedRight + width * 0.12f).roundToInt().coerceAtMost(width - 1)
    val minInk = (safeBottom - safeTop + 1)
        .times(0.0035f)
        .roundToInt()
        .coerceIn(2, 8)

    val activeColumns = BooleanArray(maxRight + 1) { x ->
        if (x < safeAxisLeft) return@BooleanArray false
        var count = 0
        for (y in safeTop..safeBottom) {
            if (gray[y * width + x] < threshold) count++
        }
        count >= minInk
    }.closeSmallColumnGaps(radius = 3)

    var rightMost = safeSeedRight
    var gap = 0
    val maxGap = (width * 0.030f).roundToInt().coerceIn(16, 36)
    for (x in safeSeedRight..maxRight) {
        if (activeColumns[x]) {
            rightMost = x
            gap = 0
        } else {
            gap++
            if (x - safeSeedRight > 12 && gap > maxGap) break
        }
    }

    return rightMost
}

private fun GraphRegionRefinementSample.findSignalTopBoundary(
    threshold: Int,
    left: Int,
    right: Int,
    axisRow: Int,
): Int? {
    val safeLeft = left.coerceIn(0, width - 1)
    val safeRight = right.coerceIn(safeLeft, width - 1)
    val upperLimit = (axisRow - height * 0.035f).roundToInt().coerceIn(0, height - 1)
    if (upperLimit <= 0) return null

    val minRun = (height * 0.055f).roundToInt().coerceIn(28, 90)
    val maxGap = (height * 0.008f).roundToInt().coerceIn(2, 8)
    val minSignalEndY = (axisRow * 0.45f).roundToInt().coerceIn(0, axisRow)
    var bestTop: Int? = null

    for (x in safeLeft..safeRight) {
        var y = 0
        while (y <= upperLimit) {
            while (y <= upperLimit && gray[y * width + x] >= threshold) y++
            if (y > upperLimit) break

            val start = y
            var lastDark = y
            var gap = 0
            while (y <= upperLimit && gap <= maxGap) {
                if (gray[y * width + x] < threshold) {
                    lastDark = y
                    gap = 0
                } else {
                    gap++
                }
                y++
            }
            val length = lastDark - start + 1
            if (length >= minRun && lastDark >= minSignalEndY) {
                bestTop = minOf(bestTop ?: start, start)
            }
        }
    }

    return bestTop
}

private fun GraphRegionRefinementSample.findHorizontalAxisRun(
    threshold: Int,
    preservePanelLabels: Boolean,
): AxisHorizontalRun? {
    val yStart = (height * 0.42f).roundToInt().coerceIn(0, height - 1)
    val yEndRatio = if (preservePanelLabels) 0.88f else 0.97f
    val yEnd = (height * yEndRatio).roundToInt().coerceIn(yStart, height - 1)
    val minRunRatio = if (preservePanelLabels) 0.25f else 0.45f
    val minRun = (width * minRunRatio).roundToInt().coerceAtLeast(80).coerceAtMost(width)
    val maxGap = if (preservePanelLabels) {
        (width * 0.030f).roundToInt().coerceIn(16, 42)
    } else {
        (width * 0.006f).roundToInt().coerceIn(2, 8)
    }
    var best: AxisHorizontalRun? = null
    val scanStartX = if (preservePanelLabels) {
        (width * 0.050f).roundToInt().coerceAtLeast(10)
    } else {
        0
    }

    for (y in yStart..yEnd) {
        val run = longestDarkRunInRow(y, threshold, maxGap, scanStartX)
        if (preservePanelLabels && run.isLikelyPageEdgeRun(width, y, height)) continue
        if (run.length >= minRun && run.length > (best?.length ?: 0)) {
            best = AxisHorizontalRun(row = y, startX = run.startX, endX = run.endX)
        }
    }

    return best
}

private fun GraphRegionRefinementSample.findVerticalAxisRun(
    threshold: Int,
    horizontalAxis: AxisHorizontalRun,
    preservePanelLabels: Boolean,
): AxisVerticalRun? {
    val leftGuard = if (preservePanelLabels) (width * 0.035f).roundToInt().coerceAtLeast(8) else 0
    val horizontalRunStartsLate = preservePanelLabels && horizontalAxis.startX > width * 0.35f
    val searchStart = if (preservePanelLabels && horizontalRunStartsLate) {
        leftGuard
    } else if (preservePanelLabels) {
        (horizontalAxis.startX - horizontalAxis.length * 0.34f).roundToInt().coerceAtLeast(leftGuard)
    } else {
        (horizontalAxis.startX - width * 0.03f).roundToInt().coerceAtLeast(0)
    }
    val searchEndRatio = if (preservePanelLabels) 0.22f else 0.30f
    val searchEnd = (horizontalAxis.startX + horizontalAxis.length * searchEndRatio)
        .roundToInt()
        .coerceIn(searchStart, width - 1)
    val endTolerance = (height * 0.045f).roundToInt().coerceAtLeast(6)
    val minRunRatio = if (preservePanelLabels) 0.18f else 0.24f
    val minRun = (height * minRunRatio).roundToInt().coerceAtLeast(48)
    val maxRun = if (preservePanelLabels) (height * 0.86f).roundToInt() else height
    val maxGap = if (preservePanelLabels) {
        (height * 0.025f).roundToInt().coerceIn(12, 32)
    } else {
        (height * 0.010f).roundToInt().coerceIn(2, 8)
    }
    val candidates = mutableListOf<AxisVerticalRun>()

    for (x in searchStart..searchEnd) {
        val run = longestDarkRunEndingNear(
            x = x,
            threshold = threshold,
            targetY = horizontalAxis.row,
            endTolerance = endTolerance,
            maxGap = maxGap,
        )
        if (run != null && preservePanelLabels && run.isLikelyPageVerticalEdge(height)) continue
        if (run != null && run.length >= minRun && run.length <= maxRun) {
            candidates += run
        }
    }
    if (candidates.isEmpty()) return null

    val maxLength = candidates.maxOf { it.length }
    val minComparableLengthRatio = if (preservePanelLabels) 0.40f else 0.60f
    val axisBand = if (preservePanelLabels) {
        val rightTolerance = (width * 0.015f).roundToInt().coerceAtLeast(12)
        candidates.filter { it.x <= horizontalAxis.startX + rightTolerance }.ifEmpty { candidates }
    } else {
        candidates
    }
    return axisBand
        .filter { it.length >= (maxLength * minComparableLengthRatio).roundToInt().coerceAtLeast(minRun) }
        .minWithOrNull(axisComparator(horizontalAxis, horizontalRunStartsLate))
}

private fun axisComparator(
    horizontalAxis: AxisHorizontalRun,
    horizontalRunStartsLate: Boolean,
): Comparator<AxisVerticalRun> =
    if (horizontalRunStartsLate) {
        compareBy<AxisVerticalRun> { it.x }.thenByDescending { it.length }
    } else {
        compareBy<AxisVerticalRun> { kotlin.math.abs(it.x - horizontalAxis.startX) }
            .thenBy { it.x }
            .thenByDescending { it.length }
    }

private data class RowRun(val startX: Int, val endX: Int) {
    val length: Int get() = endX - startX + 1
}

private fun AxisVerticalRun.isLikelyPageVerticalEdge(height: Int): Boolean {
    val startsAtSearchTop = startY <= (height * 0.035f).roundToInt().coerceAtLeast(8)
    val extendsLikePageEdge = length >= (height * 0.50f).roundToInt()
    return startsAtSearchTop && extendsLikePageEdge
}

private fun RowRun.isLikelyPageEdgeRun(width: Int, y: Int, height: Int): Boolean {
    val startsAtPageEdge = startX <= (width * 0.020f).roundToInt().coerceAtLeast(6)
    val veryWide = length >= (width * 0.92f).roundToInt()
    val inLowerSearchBand = y >= (height * 0.74f).roundToInt()
    return startsAtPageEdge && veryWide && inLowerSearchBand
}

private fun GraphRegionRefinementSample.longestDarkRunInRow(
    y: Int,
    threshold: Int,
    maxGap: Int,
    scanStartX: Int = 0,
): RowRun {
    var bestStart = 0
    var bestEnd = -1
    var start = -1
    var lastDark = -1
    var gap = 0

    for (x in scanStartX until width) {
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

private fun GraphRegion.withTop(newTop: Int): GraphRegion {
    val clampedTop = newTop.coerceAtMost(bottom - 1)
    return copy(
        y = clampedTop,
        height = bottom - clampedTop,
    )
}

private fun GraphRegion.withRight(newRight: Int): GraphRegion {
    val clampedRight = newRight.coerceAtLeast(x + 1)
    return copy(width = clampedRight - x)
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

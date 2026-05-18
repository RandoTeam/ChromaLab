package com.chromalab.feature.processing.graph

import kotlin.math.abs
import kotlin.math.roundToInt

data class GraphPlotAreaDetectionResult(
    val panelRegion: GraphRegion,
    val plotArea: GraphRegion?,
    val detected: Boolean,
    val warnings: List<String> = emptyList(),
)

class GraphPlotAreaDetector {
    private val sampler = GraphRegionRefinementSampler()

    fun detect(
        imagePath: String,
        panelRegion: GraphRegion,
        imageWidth: Int,
        imageHeight: Int,
    ): GraphPlotAreaDetectionResult {
        val clampedPanel = panelRegion.clampToImageOrNull(imageWidth, imageHeight)
            ?: return GraphPlotAreaDetectionResult(
                panelRegion = panelRegion,
                plotArea = null,
                detected = false,
                warnings = listOf("plot_area.panel_out_of_image"),
            )
        val sampleResult = sampler.sample(imagePath, clampedPanel)
        val sample = sampleResult.sample ?: return GraphPlotAreaDetectionResult(
            panelRegion = clampedPanel,
            plotArea = null,
            detected = false,
            warnings = listOf(sampleResult.warning ?: "plot_area.image_not_readable"),
        )

        val bounds = sample.findPlotAreaBounds()
        if (bounds == null) {
            val warning = sample.findPlotAreaFailureWarning()
            return GraphPlotAreaDetectionResult(
                panelRegion = clampedPanel,
                plotArea = null,
                detected = false,
                warnings = listOf(warning),
            )
        }
        val plotArea = GraphRegion(
            x = clampedPanel.x + bounds.left,
            y = clampedPanel.y + bounds.top,
            width = bounds.right - bounds.left,
            height = bounds.bottom - bounds.top,
            label = clampedPanel.label.ifBlank { "Plot area" },
        ).clampToImage(imageWidth, imageHeight)

        return GraphPlotAreaDetectionResult(
            panelRegion = clampedPanel,
            plotArea = plotArea,
            detected = true,
            warnings = bounds.warnings,
        )
    }
}

private data class PlotAreaBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val warnings: List<String> = emptyList(),
)

private data class PlotHorizontalAxisRun(
    val row: Int,
    val startX: Int,
    val endX: Int,
) {
    val length: Int get() = endX - startX + 1
}

private data class PlotVerticalAxisRun(
    val x: Int,
    val startY: Int,
    val endY: Int,
) {
    val length: Int get() = endY - startY + 1
}

private data class PlotRowRun(val startX: Int, val endX: Int) {
    val length: Int get() = endX - startX + 1
}

private fun GraphRegionRefinementSample.findPlotAreaBounds(): PlotAreaBounds? {
    if (width <= 16 || height <= 16 || gray.size != width * height) return null

    val threshold = estimatePlotAxisThreshold()
    val relaxedThreshold = (threshold + 32).coerceAtMost(215)
    var relaxedAxisThreshold = false
    val horizontalAxis = findPlotHorizontalAxis(threshold)
        ?: findPlotHorizontalAxis(relaxedThreshold)?.also { relaxedAxisThreshold = true }
        ?: return null
    var estimatedVerticalAxis = false
    val verticalAxis = findPlotVerticalAxis(threshold, horizontalAxis)
        ?: findPlotVerticalAxis(relaxedThreshold, horizontalAxis)?.also { relaxedAxisThreshold = true }
        ?: findPlotVerticalAxisByProjection(threshold, horizontalAxis)?.also { estimatedVerticalAxis = true }
        ?: findPlotVerticalAxisByProjection(relaxedThreshold, horizontalAxis)?.also {
            relaxedAxisThreshold = true
            estimatedVerticalAxis = true
        }
        ?: return null

    val signalTop = findPlotSignalTop(
        threshold = threshold,
        left = minOf(verticalAxis.x, horizontalAxis.startX),
        right = horizontalAxis.endX,
        axisRow = horizontalAxis.row,
    )
    val axisLeft = minOf(verticalAxis.x, horizontalAxis.startX)
    val axisBottom = maxOf(horizontalAxis.row, verticalAxis.endY)
    val padX = (width * 0.004f).roundToInt().coerceIn(1, 4)
    val padY = (height * 0.006f).roundToInt().coerceIn(1, 4)
    val left = axisLeft - padX
    val top = minOf(verticalAxis.startY, signalTop ?: verticalAxis.startY) - padY
    val right = horizontalAxis.endX + padX + 1
    val bottom = axisBottom + padY + 1

    if (right - left < width * 0.28f || bottom - top < height * 0.18f) return null

    val safeLeft = left.coerceAtLeast(0)
    val safeTop = top.coerceAtLeast(0)
    val safeRight = right.coerceAtMost(width)
    val safeBottom = bottom.coerceAtMost(height)
    if (safeRight - safeLeft <= 8 || safeBottom - safeTop <= 8) return null

    val warnings = buildList {
        if (signalTop != null && signalTop < verticalAxis.startY) {
            add("plot_area.signal_extends_above_detected_y_axis")
        }
        if (horizontalAxis.startX > width * 0.35f) {
            add("plot_area.x_axis_start_late")
        }
        if (verticalAxis.x > width * 0.25f) {
            add("plot_area.y_axis_far_from_left_panel")
        }
        if (relaxedAxisThreshold) {
            add("plot_area.relaxed_axis_threshold")
        }
        if (estimatedVerticalAxis) {
            add("plot_area.y_axis_estimated_from_panel_projection")
        }
    }

    return PlotAreaBounds(
        left = safeLeft,
        top = safeTop,
        right = safeRight,
        bottom = safeBottom,
        warnings = warnings,
    )
}

private fun GraphRegionRefinementSample.findPlotAreaFailureWarning(): String {
    if (width <= 16 || height <= 16 || gray.size != width * height) return "plot_area.invalid_sample"

    val threshold = estimatePlotAxisThreshold()
    val relaxedThreshold = (threshold + 32).coerceAtMost(215)
    val horizontalAxis = findPlotHorizontalAxis(threshold) ?: findPlotHorizontalAxis(relaxedThreshold)
        ?: return "plot_area.x_axis_not_found"
    val verticalAxis = findPlotVerticalAxis(threshold, horizontalAxis)
        ?: findPlotVerticalAxis(relaxedThreshold, horizontalAxis)
        ?: return "plot_area.y_axis_not_found"
    val axisLeft = minOf(verticalAxis.x, horizontalAxis.startX)
    val axisBottom = maxOf(horizontalAxis.row, verticalAxis.endY)
    if (horizontalAxis.endX - axisLeft < width * 0.28f || axisBottom - verticalAxis.startY < height * 0.18f) {
        return "plot_area.axis_bounds_too_small"
    }
    return "plot_area.axes_not_found"
}

private fun GraphRegionRefinementSample.findPlotHorizontalAxis(
    threshold: Int,
): PlotHorizontalAxisRun? {
    val yStart = (height * 0.34f).roundToInt().coerceIn(0, height - 1)
    val yEnd = (height * 0.98f).roundToInt().coerceIn(yStart, height - 1)
    val minRun = (width * 0.34f).roundToInt().coerceIn(48, width)
    val maxGap = (width * 0.060f).roundToInt().coerceIn(10, 64)
    val scanStartX = (width * 0.010f).roundToInt().coerceAtLeast(0)
    var best: PlotHorizontalAxisRun? = null
    var bestScore = Int.MIN_VALUE

    for (y in yStart..yEnd) {
        val run = longestPlotDarkRunInRow(
            y = y,
            threshold = threshold,
            maxGap = maxGap,
            scanStartX = scanStartX,
        )
        if (run.length < minRun) continue

        val startPenalty = (run.startX * 0.42f).roundToInt()
        val endBonus = (run.endX * 0.20f).roundToInt()
        val lowerBandBonus = ((y - yStart).toFloat() * 0.55f).roundToInt()
        val score = run.length + endBonus + lowerBandBonus - startPenalty
        if (score > bestScore) {
            bestScore = score
            best = PlotHorizontalAxisRun(row = y, startX = run.startX, endX = run.endX)
        }
    }

    return best
}

private fun GraphRegionRefinementSample.findPlotVerticalAxis(
    threshold: Int,
    horizontalAxis: PlotHorizontalAxisRun,
): PlotVerticalAxisRun? {
    val searchStart = 0
    val searchEnd = minOf(
        width - 1,
        (horizontalAxis.startX + width * 0.16f).roundToInt(),
        (width * 0.42f).roundToInt().coerceAtLeast(12),
    ).coerceAtLeast(searchStart)
    val endTolerance = (height * 0.055f).roundToInt().coerceIn(6, 28)
    val minRun = (height * 0.20f).roundToInt().coerceIn(24, height)
    val maxGap = (height * 0.020f).roundToInt().coerceIn(4, 18)
    val candidates = mutableListOf<PlotVerticalAxisRun>()

    for (x in searchStart..searchEnd) {
        val run = longestPlotDarkRunEndingNear(
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
    val comparable = (maxLength * 0.55f).roundToInt().coerceAtLeast(minRun)
    val axisBand = candidates
        .filter { it.x <= horizontalAxis.startX + (width * 0.10f).roundToInt().coerceAtLeast(8) }
        .ifEmpty { candidates.filter { it.x <= (width * 0.28f).roundToInt().coerceAtLeast(12) } }
        .ifEmpty { candidates }
    return axisBand
        .filter { it.length >= comparable }
        .minWithOrNull(
            compareBy<PlotVerticalAxisRun> { it.x }
                .thenBy { abs(it.x - horizontalAxis.startX) }
                .thenByDescending { it.length },
        )
}

private fun GraphRegionRefinementSample.findPlotVerticalAxisByProjection(
    threshold: Int,
    horizontalAxis: PlotHorizontalAxisRun,
): PlotVerticalAxisRun? {
    val searchEnd = minOf(
        width - 1,
        (width * 0.38f).roundToInt().coerceAtLeast(24),
        (horizontalAxis.startX + width * 0.22f).roundToInt().coerceAtLeast(24),
    )
    val minRun = (height * 0.16f).roundToInt().coerceIn(18, height)
    val maxGap = (height * 0.035f).roundToInt().coerceIn(6, 24)
    val targetY = horizontalAxis.row
    val endTolerance = (height * 0.14f).roundToInt().coerceIn(12, 54)
    val candidates = mutableListOf<PlotVerticalAxisRun>()

    for (x in 0..searchEnd) {
        var y = 0
        while (y <= targetY) {
            while (y <= targetY && gray[y * width + x] >= threshold) y++
            if (y > targetY) break

            val start = y
            var lastDark = y
            var gap = 0
            while (y <= targetY && gap <= maxGap) {
                if (gray[y * width + x] < threshold) {
                    lastDark = y
                    gap = 0
                } else {
                    gap++
                }
                y++
            }
            val candidate = PlotVerticalAxisRun(
                x = x,
                startY = start,
                endY = minOf(lastDark, targetY),
            )
            if (candidate.length >= minRun && candidate.endY >= targetY - endTolerance) {
                candidates += candidate
            }
        }
    }

    if (candidates.isEmpty()) return null
    val maxLength = candidates.maxOf { it.length }
    val comparable = (maxLength * 0.48f).roundToInt().coerceAtLeast(minRun)
    return candidates
        .filter { it.length >= comparable }
        .minWithOrNull(
            compareBy<PlotVerticalAxisRun> { it.x }
                .thenBy { abs(it.x - horizontalAxis.startX) }
                .thenByDescending { it.length },
        )
}

private fun GraphRegionRefinementSample.findPlotSignalTop(
    threshold: Int,
    left: Int,
    right: Int,
    axisRow: Int,
): Int? {
    val safeLeft = left.coerceIn(0, width - 1)
    val safeRight = right.coerceIn(safeLeft, width - 1)
    val upperLimit = (axisRow - height * 0.020f).roundToInt().coerceIn(0, height - 1)
    if (upperLimit <= 0) return null

    val minRun = (height * 0.060f).roundToInt().coerceIn(18, 90)
    val maxGap = (height * 0.010f).roundToInt().coerceIn(2, 8)
    val minSignalEndY = (axisRow * 0.30f).roundToInt().coerceIn(0, axisRow)
    var top: Int? = null

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
            if (lastDark - start + 1 >= minRun && lastDark >= minSignalEndY) {
                top = minOf(top ?: start, start)
            }
        }
    }

    return top
}

private fun GraphRegionRefinementSample.longestPlotDarkRunInRow(
    y: Int,
    threshold: Int,
    maxGap: Int,
    scanStartX: Int,
): PlotRowRun {
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
    return PlotRowRun(bestStart, bestEnd)
}

private fun GraphRegionRefinementSample.longestPlotDarkRunEndingNear(
    x: Int,
    threshold: Int,
    targetY: Int,
    endTolerance: Int,
    maxGap: Int,
): PlotVerticalAxisRun? {
    var best: PlotVerticalAxisRun? = null
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
            val candidate = PlotVerticalAxisRun(x = x, startY = start, endY = minOf(lastDark, targetY))
            if (candidate.length > (best?.length ?: 0)) best = candidate
        }
    }

    return best
}

private fun GraphRegionRefinementSample.estimatePlotAxisThreshold(): Int {
    val sorted = gray.copyOf().also { it.sort() }
    val percentile = sorted[(sorted.lastIndex * 0.20f).roundToInt()]
    return percentile.coerceIn(90, 185)
}

private fun GraphRegion.clampToImageOrNull(imageWidth: Int, imageHeight: Int): GraphRegion? {
    val x1 = x.coerceIn(0, imageWidth)
    val y1 = y.coerceIn(0, imageHeight)
    val x2 = right.coerceIn(x1, imageWidth)
    val y2 = bottom.coerceIn(y1, imageHeight)
    if (x2 - x1 <= 1 || y2 - y1 <= 1) return null
    return copy(x = x1, y = y1, width = x2 - x1, height = y2 - y1)
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

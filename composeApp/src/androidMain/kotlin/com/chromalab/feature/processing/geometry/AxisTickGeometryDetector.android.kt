package com.chromalab.feature.processing.geometry

import android.graphics.BitmapFactory
import com.chromalab.feature.processing.axis.AxisLine
import com.chromalab.feature.processing.axis.AxisOrigin
import com.chromalab.feature.processing.graph.GraphRegion
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

actual class AxisTickGeometryDetector actual constructor() {
    actual fun detect(
        imagePath: String,
        graphIndex: Int,
        panelRegion: GraphRegion,
        plotRegion: GraphRegion?,
    ): AxisTickGeometryResult {
        val bitmap = BitmapFactory.decodeFile(imagePath)
            ?: return unavailable(plotRegion, "axis_tick_geometry.image_not_readable")
        return try {
            val region = (plotRegion ?: panelRegion).clampedTo(bitmap.width, bitmap.height)
                ?: return unavailable(plotRegion, "axis_tick_geometry.region_out_of_image")
            val gray = IntArray(region.width * region.height)
            for (y in 0 until region.height) {
                for (x in 0 until region.width) {
                    gray[y * region.width + x] = bitmap.getPixel(region.x + x, region.y + y).toGray()
                }
            }

            val threshold = estimateDarkThreshold(gray)
            val xAxisRun = findXAxisRun(gray, region.width, region.height, threshold)
                ?: return unavailable(region, "axis_tick_geometry.x_axis_not_found")
            val yAxisRun = findYAxisRun(gray, region.width, region.height, threshold, xAxisRun.row)
                ?: return unavailable(region, "axis_tick_geometry.y_axis_not_found")

            val xAxis = AxisLine(
                x1 = region.x + xAxisRun.startX.toFloat(),
                y1 = region.y + xAxisRun.row.toFloat(),
                x2 = region.x + xAxisRun.endX.toFloat(),
                y2 = region.y + xAxisRun.row.toFloat(),
            )
            val yAxis = AxisLine(
                x1 = region.x + yAxisRun.x.toFloat(),
                y1 = region.y + yAxisRun.startY.toFloat(),
                x2 = region.x + yAxisRun.x.toFloat(),
                y2 = region.y + yAxisRun.endY.toFloat(),
            )
            val xTickMarks = detectXTicks(gray, region.width, region.height, region, threshold, xAxisRun.row)
            val yTickMarks = detectYTicks(gray, region.width, region.height, region, threshold, yAxisRun.x)
            val xLabelProjectionTicks = if (plotRegion != null && xTickMarks.size < 2) {
                detectXLabelColumnCenters(bitmap, panelRegion, plotRegion)
            } else {
                emptyList()
            }
            val yLabelProjectionTicks = if (plotRegion != null && yTickMarks.size < 2) {
                detectYLabelRowCenters(bitmap, panelRegion, plotRegion)
            } else {
                emptyList()
            }
            val xTicks = mergeTickPositions(xTickMarks, xLabelProjectionTicks, max(4, region.width / 90))
                .regularizeCrowdedTicks(region.x.toFloat(), region.right.toFloat(), maxCount = 12)
            val yTicks = mergeTickPositions(yTickMarks, yLabelProjectionTicks, max(4, region.height / 80))
                .regularizeCrowdedTicks(region.y.toFloat(), region.bottom.toFloat(), maxCount = 12)

            AxisTickGeometryResult(
                available = true,
                source = "android_projection_axis_tick_geometry",
                plotRegion = region,
                xAxis = xAxis,
                yAxis = yAxis,
                origin = AxisOrigin(yAxis.x1, xAxis.y1),
                lineSegmentCount = 0,
                horizontalLineCount = 1,
                verticalLineCount = 1,
                xTickPositions = xTicks,
                yTickPositions = yTicks,
                readyForOcrValueMatching = xTicks.size >= 2 && yTicks.size >= 2,
                warnings = buildList {
                    if (xLabelProjectionTicks.isNotEmpty()) add("axis_tick_geometry.x_label_projection_rescue")
                    if (yLabelProjectionTicks.isNotEmpty()) add("axis_tick_geometry.y_label_projection_rescue")
                    if (xTicks.size < 2) add("axis_tick_geometry.x_tick_positions_insufficient")
                    if (yTicks.size < 2) add("axis_tick_geometry.y_tick_positions_insufficient")
                },
            )
        } catch (error: Throwable) {
            unavailable(plotRegion, "axis_tick_geometry.backend_failed:${error::class.simpleName}")
        } finally {
            bitmap.recycle()
        }
    }

    private fun unavailable(region: GraphRegion?, warning: String): AxisTickGeometryResult =
        AxisTickGeometryResult(
            available = false,
            source = "android_projection_axis_tick_geometry",
            plotRegion = region,
            xAxis = null,
            yAxis = null,
            origin = null,
            lineSegmentCount = 0,
            horizontalLineCount = 0,
            verticalLineCount = 0,
            xTickPositions = emptyList(),
            yTickPositions = emptyList(),
            readyForOcrValueMatching = false,
            warnings = listOf(warning),
        )

    private fun findXAxisRun(
        gray: IntArray,
        width: Int,
        height: Int,
        threshold: Int,
    ): HorizontalRun? {
        val yStart = (height * 0.48f).roundToInt().coerceIn(0, height - 1)
        val yEnd = (height * 0.99f).roundToInt().coerceIn(yStart, height - 1)
        val minRun = (width * 0.30f).roundToInt().coerceIn(32, width)
        val maxGap = (width * 0.055f).roundToInt().coerceIn(8, 48)
        var best: HorizontalRun? = null
        var bestScore = Int.MIN_VALUE
        for (y in yStart..yEnd) {
            val run = longestDarkRunInRow(gray, width, y, threshold, maxGap)
            if (run.length < minRun) continue
            val lowerBias = ((y - yStart) * 0.55f).roundToInt()
            val score = run.length + lowerBias + (run.endX * 0.15f).roundToInt() - (run.startX * 0.35f).roundToInt()
            if (score > bestScore) {
                bestScore = score
                best = run.copy(row = y)
            }
        }
        return best
    }

    private fun findYAxisRun(
        gray: IntArray,
        width: Int,
        height: Int,
        threshold: Int,
        xAxisY: Int,
    ): VerticalRun? {
        val xEnd = (width * 0.38f).roundToInt().coerceIn(0, width - 1)
        val minRun = (height * 0.18f).roundToInt().coerceIn(24, height)
        val maxGap = (height * 0.025f).roundToInt().coerceIn(4, 18)
        val candidates = mutableListOf<VerticalRun>()
        for (x in 0..xEnd) {
            val run = longestDarkRunEndingNear(gray, width, height, x, threshold, xAxisY, maxGap)
            if (run != null && run.length >= minRun) candidates += run
        }
        return candidates.minWithOrNull(
            compareBy<VerticalRun> { it.x }
                .thenByDescending { it.length },
        )
    }

    private fun detectXTicks(
        gray: IntArray,
        width: Int,
        height: Int,
        region: GraphRegion,
        threshold: Int,
        xAxisY: Int,
    ): List<Float> {
        val top = (xAxisY - max(2, height / 90)).coerceAtLeast(0)
        val bottom = (xAxisY + max(4, height / 28)).coerceAtMost(height - 1)
        val positions = mutableListOf<Int>()
        val minRun = 2
        val maxRun = max(5, height / 10)
        for (x in 0 until width) {
            var current = 0
            var best = 0
            for (y in top..bottom) {
                if (gray[y * width + x] <= threshold) {
                    current++
                    best = max(best, current)
                } else {
                    current = 0
                }
            }
            if (best in minRun..maxRun) positions += x
        }
        return positions.toClusterCenters(max(4, width / 90))
            .map { region.x + it.toFloat() }
    }

    private fun detectYTicks(
        gray: IntArray,
        width: Int,
        height: Int,
        region: GraphRegion,
        threshold: Int,
        yAxisX: Int,
    ): List<Float> {
        val left = (yAxisX - max(4, width / 42)).coerceAtLeast(0)
        val right = (yAxisX + max(3, width / 58)).coerceAtMost(width - 1)
        val positions = mutableListOf<Int>()
        val minRun = 2
        val maxRun = max(5, width / 9)
        for (y in 0 until height) {
            var current = 0
            var best = 0
            for (x in left..right) {
                if (gray[y * width + x] <= threshold) {
                    current++
                    best = max(best, current)
                } else {
                    current = 0
                }
            }
            if (best in minRun..maxRun) positions += y
        }
        return positions.toClusterCenters(max(4, height / 80))
            .map { region.y + it.toFloat() }
    }

    private fun detectYLabelRowCenters(
        bitmap: android.graphics.Bitmap,
        panelRegion: GraphRegion,
        plotRegion: GraphRegion,
    ): List<Float> {
        val panel = panelRegion.clampedTo(bitmap.width, bitmap.height) ?: return emptyList()
        val plot = plotRegion.clampedTo(bitmap.width, bitmap.height) ?: return emptyList()
        val left = panel.x.coerceIn(0, bitmap.width - 1)
        val hasLeftLabelMargin = plot.x > panel.x + max(12, panel.width / 40)
        val right = if (hasLeftLabelMargin) {
            (plot.x - max(2, plot.width / 120)).coerceIn(left, bitmap.width - 1)
        } else {
            (panel.x + max(24, panel.width / 5)).coerceIn(left, bitmap.width - 1)
        }
        val top = plot.y.coerceIn(0, bitmap.height - 1)
        val bottom = plot.bottom.coerceIn(top + 1, bitmap.height)
        val bandWidth = right - left + 1
        if (bandWidth < 18 || bottom <= top) return emptyList()

        val band = IntArray(bandWidth * (bottom - top))
        for (y in top until bottom) {
            for (x in left..right) {
                band[(y - top) * bandWidth + (x - left)] = bitmap.getPixel(x, y).toGray()
            }
        }
        val threshold = estimateDarkThreshold(band)
        val candidateRows = mutableListOf<Int>()
        val minDarkPixels = max(2, (bandWidth * 0.018f).roundToInt())
        val maxDarkPixels = max(minDarkPixels + 1, (bandWidth * 0.18f).roundToInt())
        for (row in 0 until (bottom - top)) {
            var dark = 0
            for (x in 0 until bandWidth) {
                if (band[row * bandWidth + x] <= threshold) dark++
            }
            if (dark in minDarkPixels..maxDarkPixels) {
                candidateRows += top + row
            }
        }
        return candidateRows
            .toClusterCenters(max(4, plot.height / 80))
            .map { it.toFloat() }
            .filter { it in plot.y.toFloat()..plot.bottom.toFloat() }
    }

    private fun detectXLabelColumnCenters(
        bitmap: android.graphics.Bitmap,
        panelRegion: GraphRegion,
        plotRegion: GraphRegion,
    ): List<Float> {
        val panel = panelRegion.clampedTo(bitmap.width, bitmap.height) ?: return emptyList()
        val plot = plotRegion.clampedTo(bitmap.width, bitmap.height) ?: return emptyList()
        val left = plot.x.coerceIn(0, bitmap.width - 1)
        val right = plot.right.coerceIn(left + 1, bitmap.width)
        val top = (plot.bottom + 1).coerceIn(0, bitmap.height - 1)
        val bottom = panel.bottom.coerceIn(top + 1, bitmap.height)
        val bandHeight = bottom - top
        if (bandHeight < 14 || right <= left) return emptyList()

        val bandWidth = right - left
        val band = IntArray(bandWidth * bandHeight)
        for (y in top until bottom) {
            for (x in left until right) {
                band[(y - top) * bandWidth + (x - left)] = bitmap.getPixel(x, y).toGray()
            }
        }
        val threshold = estimateDarkThreshold(band)
        val candidateColumns = mutableListOf<Int>()
        val minDarkPixels = max(2, (bandHeight * 0.10f).roundToInt())
        val maxDarkPixels = max(minDarkPixels + 1, (bandHeight * 0.86f).roundToInt())
        for (column in 0 until bandWidth) {
            var dark = 0
            for (y in 0 until bandHeight) {
                if (band[y * bandWidth + column] <= threshold) dark++
            }
            if (dark in minDarkPixels..maxDarkPixels) {
                candidateColumns += left + column
            }
        }
        return candidateColumns
            .toClusterCenters(max(4, plot.width / 90))
            .map { it.toFloat() }
            .filter { it in plot.x.toFloat()..plot.right.toFloat() }
    }

    private fun mergeTickPositions(
        primary: List<Float>,
        rescue: List<Float>,
        maxGap: Int,
    ): List<Float> =
        (primary + rescue)
            .map { it.roundToInt() }
            .distinct()
            .sorted()
            .toClusterCenters(maxGap)
            .map { it.toFloat() }

    private fun List<Float>.regularizeCrowdedTicks(
        minValue: Float,
        maxValue: Float,
        maxCount: Int,
    ): List<Float> {
        val sorted = distinct().sorted()
        if (sorted.size <= maxCount) return sorted
        val span = (maxValue - minValue).coerceAtLeast(1f)
        val minStep = span / 16f
        val maxStep = span / 3f
        val tolerance = max(4f, span / 120f)
        var best = emptyList<Float>()
        var bestStep = 0f

        for (i in sorted.indices) {
            for (j in i + 1 until sorted.size) {
                val step = sorted[j] - sorted[i]
                if (step < minStep || step > maxStep) continue
                val base = sorted[i]
                val group = sorted.filter { position ->
                    val multiple = kotlin.math.round((position - base) / step)
                    val expected = base + multiple * step
                    kotlin.math.abs(position - expected) <= tolerance
                }
                val better = group.size > best.size ||
                    (group.size == best.size && step > bestStep && group.size >= 2)
                if (better) {
                    best = group
                    bestStep = step
                }
            }
        }

        if (best.size >= 2) return best.sorted().take(maxCount)
        val stride = kotlin.math.ceil(sorted.size / maxCount.toDouble()).toInt().coerceAtLeast(1)
        return sorted.filterIndexed { index, _ -> index % stride == 0 }.take(maxCount)
    }

    private fun longestDarkRunInRow(
        gray: IntArray,
        width: Int,
        y: Int,
        threshold: Int,
        maxGap: Int,
    ): HorizontalRun {
        var best = HorizontalRun(row = y, startX = 0, endX = -1)
        var start = -1
        var lastDark = -1
        var gap = 0
        for (x in 0 until width) {
            if (gray[y * width + x] <= threshold) {
                if (start < 0) start = x
                lastDark = x
                gap = 0
            } else if (start >= 0) {
                gap++
                if (gap > maxGap) {
                    if (lastDark - start > best.length) best = HorizontalRun(y, start, lastDark)
                    start = -1
                    lastDark = -1
                    gap = 0
                }
            }
        }
        if (start >= 0 && lastDark - start > best.length) best = HorizontalRun(y, start, lastDark)
        return best
    }

    private fun longestDarkRunEndingNear(
        gray: IntArray,
        width: Int,
        height: Int,
        x: Int,
        threshold: Int,
        targetY: Int,
        maxGap: Int,
    ): VerticalRun? {
        val bottomLimit = (targetY + max(6, height / 28)).coerceAtMost(height - 1)
        val endTolerance = max(6, height / 22)
        var best: VerticalRun? = null
        var y = 0
        while (y <= bottomLimit) {
            while (y <= bottomLimit && gray[y * width + x] > threshold) y++
            if (y > bottomLimit) break
            val start = y
            var lastDark = y
            var gap = 0
            while (y <= bottomLimit && gap <= maxGap) {
                if (gray[y * width + x] <= threshold) {
                    lastDark = y
                    gap = 0
                } else {
                    gap++
                }
                y++
            }
            if (lastDark >= targetY - endTolerance) {
                val candidate = VerticalRun(x, start, min(lastDark, targetY))
                if (candidate.length > (best?.length ?: 0)) best = candidate
            }
        }
        return best
    }

    private fun estimateDarkThreshold(gray: IntArray): Int {
        if (gray.isEmpty()) return 160
        val sorted = gray.copyOf().also { it.sort() }
        return sorted[(sorted.lastIndex * 0.20f).roundToInt()].coerceIn(75, 190)
    }

    private fun List<Int>.toClusterCenters(maxGap: Int): List<Int> {
        if (isEmpty()) return emptyList()
        val centers = mutableListOf<Int>()
        var start = first()
        var last = first()
        for (value in drop(1)) {
            if (value - last > maxGap) {
                centers += (start + last) / 2
                start = value
            }
            last = value
        }
        centers += (start + last) / 2
        return centers
    }

    private fun GraphRegion.clampedTo(width: Int, height: Int): GraphRegion? {
        val left = x.coerceIn(0, width - 1)
        val top = y.coerceIn(0, height - 1)
        val right = right.coerceIn(left + 1, width)
        val bottom = bottom.coerceIn(top + 1, height)
        if (right <= left || bottom <= top) return null
        return copy(x = left, y = top, width = right - left, height = bottom - top)
    }

    private fun Int.toGray(): Int =
        (
            0.299 * ((this shr 16) and 0xFF) +
                0.587 * ((this shr 8) and 0xFF) +
                0.114 * (this and 0xFF)
            ).toInt()

    private data class HorizontalRun(val row: Int, val startX: Int, val endX: Int) {
        val length: Int get() = endX - startX + 1
    }

    private data class VerticalRun(val x: Int, val startY: Int, val endY: Int) {
        val length: Int get() = endY - startY + 1
    }
}

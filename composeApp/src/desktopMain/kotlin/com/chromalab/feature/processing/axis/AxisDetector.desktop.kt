package com.chromalab.feature.processing.axis

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.pipeline.DetectionMethod
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

actual class AxisDetector actual constructor() {
    actual fun detect(imagePath: String, graphRegion: GraphRegion): AxesResult {
        val source = ImageIO.read(File(imagePath))
            ?: return noAxes("axis.image_not_readable")
        val region = graphRegion.clampTo(source.width, source.height)
            ?: return noAxes("axis.region_out_of_image").also { source.flush() }

        val width = region.width
        val height = region.height
        val gray = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                gray[y * width + x] = source.getRGB(region.x + x, region.y + y).toGray()
            }
        }
        source.flush()

        val threshold = estimateAxisThreshold(gray)
        val relaxedThreshold = (threshold + 34).coerceAtMost(220)
        var usedRelaxedThreshold = false
        val horizontalAxis = findHorizontalAxis(
            gray = gray,
            width = width,
            height = height,
            threshold = threshold,
        ) ?: findHorizontalAxis(
            gray = gray,
            width = width,
            height = height,
            threshold = relaxedThreshold,
        )?.also { usedRelaxedThreshold = true }

        val verticalAxis = horizontalAxis?.let { axis ->
            findVerticalAxis(
                gray = gray,
                width = width,
                height = height,
                threshold = threshold,
                horizontalAxis = axis,
            ) ?: findVerticalAxis(
                gray = gray,
                width = width,
                height = height,
                threshold = relaxedThreshold,
                horizontalAxis = axis,
            )?.also { usedRelaxedThreshold = true }
        }

        val xAxis = horizontalAxis?.let { axis ->
            AxisLine(
                x1 = (region.x + axis.startX).toFloat(),
                y1 = (region.y + axis.row).toFloat(),
                x2 = (region.x + axis.endX).toFloat(),
                y2 = (region.y + axis.row).toFloat(),
            )
        }
        val yAxis = verticalAxis?.let { axis ->
            AxisLine(
                x1 = (region.x + axis.x).toFloat(),
                y1 = (region.y + axis.startY).toFloat(),
                x2 = (region.x + axis.x).toFloat(),
                y2 = (region.y + axis.endY).toFloat(),
            )
        }
        val origin = if (horizontalAxis != null && verticalAxis != null) {
            AxisOrigin(
                x = (region.x + verticalAxis.x).toFloat(),
                y = (region.y + horizontalAxis.row).toFloat(),
            )
        } else {
            null
        }
        val confidence = axisConfidence(horizontalAxis, verticalAxis, width, height)
        val warnings = buildList {
            if (horizontalAxis == null) add("axis.x_not_detected")
            if (verticalAxis == null) add("axis.y_not_detected")
            if (usedRelaxedThreshold) add("axis.relaxed_threshold")
            if (confidence in 0.01f..0.55f) add("axis.low_confidence")
        }

        return AxesResult(
            xAxis = xAxis,
            yAxis = yAxis,
            origin = origin,
            detectionMethod = DetectionMethod.AUTO,
            confidence = confidence,
            warnings = warnings,
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun findHorizontalAxis(
        gray: IntArray,
        width: Int,
        height: Int,
        threshold: Int,
    ): HorizontalAxisRun? {
        if (width <= 8 || height <= 8) return null
        val yStart = (height * 0.30f).roundToInt().coerceIn(0, height - 1)
        val yEnd = (height * 0.995f).roundToInt().coerceIn(yStart, height - 1)
        val minRun = (width * 0.30f).roundToInt().coerceIn(18, width)
        val maxGap = (width * 0.070f).roundToInt().coerceIn(6, 72)
        var best: HorizontalAxisRun? = null
        var bestScore = Int.MIN_VALUE

        for (y in yStart..yEnd) {
            val run = longestDarkRunInRow(gray, width, y, threshold, maxGap)
            if (run.length < minRun) continue

            val lowerBandBonus = ((y - yStart).toFloat() * 0.55f).roundToInt()
            val startPenalty = (run.startX * 0.32f).roundToInt()
            val rightCoverageBonus = (run.endX * 0.18f).roundToInt()
            val score = run.length + lowerBandBonus + rightCoverageBonus - startPenalty
            if (score > bestScore) {
                bestScore = score
                best = HorizontalAxisRun(row = y, startX = run.startX, endX = run.endX)
            }
        }

        return best
    }

    private fun findVerticalAxis(
        gray: IntArray,
        width: Int,
        height: Int,
        threshold: Int,
        horizontalAxis: HorizontalAxisRun,
    ): VerticalAxisRun? {
        if (width <= 8 || height <= 8) return null
        val searchEnd = minOf(
            width - 1,
            (horizontalAxis.startX + width * 0.18f).roundToInt(),
            (width * 0.44f).roundToInt().coerceAtLeast(8),
        ).coerceAtLeast(0)
        val minRun = (height * 0.20f).roundToInt().coerceIn(16, height)
        val maxGap = (height * 0.024f).roundToInt().coerceIn(3, 22)
        val endTolerance = (height * 0.060f).roundToInt().coerceIn(5, 32)
        val candidates = mutableListOf<VerticalAxisRun>()

        for (x in 0..searchEnd) {
            val run = longestDarkRunEndingNear(
                gray = gray,
                width = width,
                height = height,
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
            .filter { it.x <= horizontalAxis.startX + (width * 0.10f).roundToInt().coerceAtLeast(6) }
            .ifEmpty { candidates.filter { it.x <= (width * 0.28f).roundToInt().coerceAtLeast(8) } }
            .ifEmpty { candidates }

        return axisBand
            .filter { it.length >= comparable }
            .minWithOrNull(
                compareBy<VerticalAxisRun> { it.x }
                    .thenBy { kotlin.math.abs(it.x - horizontalAxis.startX) }
                    .thenByDescending { it.length },
            )
    }

    private fun longestDarkRunInRow(
        gray: IntArray,
        width: Int,
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

    private fun longestDarkRunEndingNear(
        gray: IntArray,
        width: Int,
        height: Int,
        x: Int,
        threshold: Int,
        targetY: Int,
        endTolerance: Int,
        maxGap: Int,
    ): VerticalAxisRun? {
        var best: VerticalAxisRun? = null
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
                val candidate = VerticalAxisRun(x = x, startY = start, endY = minOf(lastDark, targetY))
                if (candidate.length > (best?.length ?: 0)) {
                    best = candidate
                }
            }
        }

        return best
    }

    private fun estimateAxisThreshold(gray: IntArray): Int {
        if (gray.isEmpty()) return 160
        val sorted = gray.copyOf().also { it.sort() }
        val percentile = sorted[(sorted.lastIndex * 0.20f).roundToInt()]
        return percentile.coerceIn(90, 185)
    }

    private fun axisConfidence(
        horizontal: HorizontalAxisRun?,
        vertical: VerticalAxisRun?,
        width: Int,
        height: Int,
    ): Float {
        if (horizontal == null && vertical == null) return 0f
        val horizontalScore = horizontal?.length?.toFloat()?.div(width.coerceAtLeast(1)) ?: 0f
        val verticalScore = vertical?.length?.toFloat()?.div(height.coerceAtLeast(1)) ?: 0f
        return when {
            horizontal != null && vertical != null -> (horizontalScore * 0.56f + verticalScore * 0.44f)
                .coerceIn(0.35f, 0.98f)
            horizontal != null -> (horizontalScore * 0.42f).coerceIn(0.12f, 0.45f)
            else -> (verticalScore * 0.42f).coerceIn(0.12f, 0.45f)
        }
    }

    private fun noAxes(warning: String): AxesResult =
        AxesResult(
            xAxis = null,
            yAxis = null,
            origin = null,
            detectionMethod = DetectionMethod.AUTO,
            confidence = 0f,
            warnings = listOf(warning),
            timestamp = System.currentTimeMillis(),
        )
}

private data class HorizontalAxisRun(
    val row: Int,
    val startX: Int,
    val endX: Int,
) {
    val length: Int get() = endX - startX + 1
}

private data class VerticalAxisRun(
    val x: Int,
    val startY: Int,
    val endY: Int,
) {
    val length: Int get() = endY - startY + 1
}

private data class RowRun(
    val startX: Int,
    val endX: Int,
) {
    val length: Int get() = endX - startX + 1
}

private fun Int.toGray(): Int =
    (
        0.299 * ((this shr 16) and 0xFF) +
            0.587 * ((this shr 8) and 0xFF) +
            0.114 * (this and 0xFF)
        ).toInt()

private fun GraphRegion.clampTo(imageWidth: Int, imageHeight: Int): GraphRegion? {
    val left = x.coerceIn(0, imageWidth)
    val top = y.coerceIn(0, imageHeight)
    val right = right.coerceIn(left, imageWidth)
    val bottom = bottom.coerceIn(top, imageHeight)
    if (right - left <= 1 || bottom - top <= 1) return null
    return copy(x = left, y = top, width = right - left, height = bottom - top)
}

package com.chromalab.feature.processing.curve

import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.graph.GraphRegion
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.roundToInt

actual class CurveMaskPreparer actual constructor() {
    actual fun prepare(
        imagePath: String,
        graphRegion: GraphRegion,
        axes: AxesResult,
        outputDir: String,
    ): CurveMaskResult {
        val source = ImageIO.read(File(imagePath))
            ?: return emptyResult(graphRegion)
        val region = graphRegion.clampTo(source.width, source.height)
            ?: return emptyResult(graphRegion).also { source.flush() }
        val width = region.width
        val height = region.height
        val gray = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                gray[y * width + x] = source.getRGB(region.x + x, region.y + y).toGray()
            }
        }
        source.flush()

        val adaptiveMask = adaptiveThreshold(gray, width, height, blockSize = 31, c = 9)
        val edgeMask = sobelEdgeMask(gray, width, height)
        val rawMask = BooleanArray(width * height) { index ->
            adaptiveMask[index] || edgeMask[index]
        }
        val rawCount = rawMask.count { it }

        val cleanMask = rawMask.copyOf()
        val suppressions = mutableListOf<String>()
        suppressLongHorizontalRows(cleanMask, width, height)
        suppressions += "horizontal_axes_grid"
        suppressLeftAxisLabelBand(cleanMask, width, height)
        suppressions += "left_axis_labels"
        suppressLeftAxisColumns(cleanMask, width, height)
        suppressions += "left_axis"
        suppressImageBorder(cleanMask, width, height)
        suppressions += "border"
        val removedFloating = suppressFloatingComponents(cleanMask, width, height, axes, region)
        if (removedFloating) suppressions += "floating_text_components"
        val removedSmall = suppressSmallComponents(cleanMask, width, height, maxSize = 5)
        if (removedSmall) suppressions += "small_components"
        val cleanCount = cleanMask.count { it }

        val dir = File(outputDir).also { it.mkdirs() }
        val rawPath = File(dir, "mask_raw.png").absolutePath
        val cleanPath = File(dir, "mask_clean.png").absolutePath
        saveMask(rawMask, width, height, rawPath)
        saveMask(cleanMask, width, height, cleanPath)

        return CurveMaskResult(
            rawMaskPath = rawPath,
            cleanMaskPath = cleanPath,
            graphRegion = region,
            maskWidth = width,
            maskHeight = height,
            coordinateScale = 1f,
            rawPixelCount = rawCount,
            cleanPixelCount = cleanCount,
            suppressionApplied = suppressions,
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun adaptiveThreshold(
        gray: IntArray,
        width: Int,
        height: Int,
        blockSize: Int,
        c: Int,
    ): BooleanArray {
        val integral = LongArray(width * height)
        for (y in 0 until height) {
            var rowSum = 0L
            for (x in 0 until width) {
                rowSum += gray[y * width + x]
                integral[y * width + x] = rowSum + if (y > 0) integral[(y - 1) * width + x] else 0L
            }
        }

        val mask = BooleanArray(width * height)
        val radius = blockSize / 2
        for (y in 0 until height) {
            for (x in 0 until width) {
                val left = (x - radius).coerceAtLeast(0)
                val right = (x + radius).coerceAtMost(width - 1)
                val top = (y - radius).coerceAtLeast(0)
                val bottom = (y + radius).coerceAtMost(height - 1)
                val area = (right - left + 1) * (bottom - top + 1)
                val localMean = integral.sumRect(width, left, top, right, bottom) / area
                mask[y * width + x] = gray[y * width + x] < localMean - c
            }
        }
        return mask
    }

    private fun sobelEdgeMask(gray: IntArray, width: Int, height: Int): BooleanArray {
        val magnitude = IntArray(width * height)
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val gx = -gray[(y - 1) * width + x - 1] + gray[(y - 1) * width + x + 1] -
                    2 * gray[y * width + x - 1] + 2 * gray[y * width + x + 1] -
                    gray[(y + 1) * width + x - 1] + gray[(y + 1) * width + x + 1]
                val gy = -gray[(y - 1) * width + x - 1] - 2 * gray[(y - 1) * width + x] -
                    gray[(y - 1) * width + x + 1] + gray[(y + 1) * width + x - 1] +
                    2 * gray[(y + 1) * width + x] + gray[(y + 1) * width + x + 1]
                magnitude[y * width + x] = abs(gx) + abs(gy)
            }
        }

        val threshold = magnitude.filter { it > 0 }
            .sorted()
            .let { values ->
                if (values.isEmpty()) 180 else values[(values.lastIndex * 0.76f).roundToInt()]
            }
            .coerceIn(90, 360)
        val mask = BooleanArray(width * height) { magnitude[it] >= threshold }
        dilate(mask, width, height)
        return mask
    }

    private fun suppressLongHorizontalRows(mask: BooleanArray, width: Int, height: Int) {
        val threshold = (width * 0.54f).roundToInt().coerceAtLeast(24)
        for (y in 0 until height) {
            val count = (0 until width).count { x -> mask[y * width + x] }
            if (count >= threshold) {
                eraseRowBand(mask, width, height, y, radius = 1)
            }
        }
    }

    private fun suppressLeftAxisColumns(mask: BooleanArray, width: Int, height: Int) {
        val maxAxisX = (width * 0.12f).roundToInt().coerceAtLeast(10).coerceAtMost(width - 1)
        val threshold = (height * 0.45f).roundToInt().coerceAtLeast(20)
        for (x in 0..maxAxisX) {
            val count = (0 until height).count { y -> mask[y * width + x] }
            if (count >= threshold) {
                eraseColumnBand(mask, width, height, x, radius = 1)
            }
        }
    }

    private fun suppressLeftAxisLabelBand(mask: BooleanArray, width: Int, height: Int) {
        val axisX = findLeftAxisColumn(mask, width, height) ?: return
        val clearTo = (axisX + 2).coerceAtMost(width - 1)
        for (y in 0 until height) {
            for (x in 0..clearTo) {
                mask[y * width + x] = false
            }
        }
    }

    private fun findLeftAxisColumn(mask: BooleanArray, width: Int, height: Int): Int? {
        val searchEnd = (width * 0.16f).roundToInt().coerceIn(4, width - 1)
        val minRun = (height * 0.30f).roundToInt().coerceAtLeast(18)
        return (0..searchEnd)
            .mapNotNull { x ->
                val run = longestColumnRun(mask, width, height, x)
                if (run >= minRun) x to run else null
            }
            .maxWithOrNull(compareBy<Pair<Int, Int>> { it.second }.thenByDescending { -it.first })
            ?.first
    }

    private fun longestColumnRun(mask: BooleanArray, width: Int, height: Int, column: Int): Int {
        var best = 0
        var current = 0
        for (y in 0 until height) {
            if (mask[y * width + column]) {
                current++
                if (current > best) best = current
            } else {
                current = 0
            }
        }
        return best
    }

    private fun suppressImageBorder(mask: BooleanArray, width: Int, height: Int) {
        val borderX = (width * 0.006f).roundToInt().coerceIn(1, 4)
        val borderY = (height * 0.008f).roundToInt().coerceIn(1, 4)
        for (y in 0 until height) {
            for (x in 0 until borderX) mask[y * width + x] = false
            for (x in width - borderX until width) mask[y * width + x] = false
        }
        for (x in 0 until width) {
            for (y in 0 until borderY) mask[y * width + x] = false
            for (y in height - borderY until height) mask[y * width + x] = false
        }
    }

    private fun suppressSmallComponents(
        mask: BooleanArray,
        width: Int,
        height: Int,
        maxSize: Int,
    ): Boolean {
        val labels = IntArray(width * height)
        val componentSizes = mutableMapOf<Int, Int>()
        var nextLabel = 1
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                if (mask[index] && labels[index] == 0) {
                    componentSizes[nextLabel] = floodFill(mask, labels, width, height, x, y, nextLabel)
                    nextLabel++
                }
            }
        }

        val smallLabels = componentSizes
            .filterValues { it <= maxSize }
            .keys
            .toSet()
        if (smallLabels.isEmpty()) return false

        for (index in labels.indices) {
            if (labels[index] in smallLabels) {
                mask[index] = false
            }
        }
        return true
    }

    private fun suppressFloatingComponents(
        mask: BooleanArray,
        width: Int,
        height: Int,
        axes: AxesResult,
        region: GraphRegion,
    ): Boolean {
        if (!isCompactLowResolutionPlot(width, height)) return false
        val axisY = axes.origin
            ?.let { (it.y - region.y).roundToInt() }
            ?.takeIf { it in 0 until height }
            ?: findLikelyXAxisRow(mask, width, height)
            ?: return false
        val labels = IntArray(width * height)
        val components = mutableListOf<ComponentBounds>()
        var nextLabel = 1
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                if (mask[index] && labels[index] == 0) {
                    components += floodFillBounds(mask, labels, width, height, x, y, nextLabel)
                    nextLabel++
                }
            }
        }

        val baselineTolerance = (height * 0.12f).roundToInt().coerceIn(4, 18)
        val tallSignalHeight = (height * 0.34f).roundToInt().coerceAtLeast(18)
        val lowerSignalBand = (axisY - height * 0.20f).roundToInt().coerceAtLeast(0)
        val removableLabels = components
            .filter { component ->
                val reachesBaseline = component.maxY >= axisY - baselineTolerance
                val tallSignalCandidate = component.height >= tallSignalHeight && component.maxY >= lowerSignalBand
                !reachesBaseline && !tallSignalCandidate
            }
            .map { it.label }
            .toSet()
        if (removableLabels.isEmpty()) return false
        if (wouldDropBelowUsableCoverage(labels, mask, width, height, removableLabels)) return false

        for (index in labels.indices) {
            if (labels[index] in removableLabels) {
                mask[index] = false
            }
        }
        return true
    }

    private fun findLikelyXAxisRow(mask: BooleanArray, width: Int, height: Int): Int? {
        val startY = (height * 0.55f).roundToInt().coerceIn(0, height - 1)
        var bestY: Int? = null
        var bestCount = 0
        for (y in startY until height) {
            val count = (0 until width).count { x -> mask[y * width + x] }
            if (count > bestCount) {
                bestCount = count
                bestY = y
            }
        }
        return bestY?.takeIf { bestCount >= (width * 0.08f).roundToInt().coerceAtLeast(8) }
    }

    private fun wouldDropBelowUsableCoverage(
        labels: IntArray,
        mask: BooleanArray,
        width: Int,
        height: Int,
        removableLabels: Set<Int>,
    ): Boolean {
        val retainedColumns = BooleanArray(width)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                if (mask[index] && labels[index] !in removableLabels) {
                    retainedColumns[x] = true
                }
            }
        }
        val retainedCoverage = retainedColumns.count { it }.toFloat() / width.coerceAtLeast(1).toFloat()
        return retainedCoverage < MIN_USABLE_COLUMN_COVERAGE
    }

    private fun isCompactLowResolutionPlot(width: Int, height: Int): Boolean =
        width <= MAX_COMPACT_PLOT_WIDTH && height <= MAX_COMPACT_PLOT_HEIGHT

    private fun floodFillBounds(
        mask: BooleanArray,
        labels: IntArray,
        width: Int,
        height: Int,
        startX: Int,
        startY: Int,
        label: Int,
    ): ComponentBounds {
        val stack = ArrayDeque<Int>()
        stack.add(startY * width + startX)
        labels[startY * width + startX] = label
        var minY = startY
        var maxY = startY
        while (stack.isNotEmpty()) {
            val index = stack.removeLast()
            val x = index % width
            val y = index / width
            minY = minOf(minY, y)
            maxY = maxOf(maxY, y)
            for ((dx, dy) in NEIGHBORS_4) {
                val nextX = x + dx
                val nextY = y + dy
                if (nextX !in 0 until width || nextY !in 0 until height) continue
                val nextIndex = nextY * width + nextX
                if (mask[nextIndex] && labels[nextIndex] == 0) {
                    labels[nextIndex] = label
                    stack.add(nextIndex)
                }
            }
        }
        return ComponentBounds(
            label = label,
            minY = minY,
            maxY = maxY,
        )
    }

    private fun floodFill(
        mask: BooleanArray,
        labels: IntArray,
        width: Int,
        height: Int,
        startX: Int,
        startY: Int,
        label: Int,
    ): Int {
        val stack = ArrayDeque<Int>()
        stack.add(startY * width + startX)
        labels[startY * width + startX] = label
        var count = 0
        while (stack.isNotEmpty()) {
            val index = stack.removeLast()
            count++
            val x = index % width
            val y = index / width
            for ((dx, dy) in NEIGHBORS_4) {
                val nextX = x + dx
                val nextY = y + dy
                if (nextX !in 0 until width || nextY !in 0 until height) continue
                val nextIndex = nextY * width + nextX
                if (mask[nextIndex] && labels[nextIndex] == 0) {
                    labels[nextIndex] = label
                    stack.add(nextIndex)
                }
            }
        }
        return count
    }

    private fun eraseRowBand(mask: BooleanArray, width: Int, height: Int, row: Int, radius: Int) {
        for (dy in -radius..radius) {
            val y = row + dy
            if (y !in 0 until height) continue
            for (x in 0 until width) {
                mask[y * width + x] = false
            }
        }
    }

    private fun eraseColumnBand(mask: BooleanArray, width: Int, height: Int, column: Int, radius: Int) {
        for (dx in -radius..radius) {
            val x = column + dx
            if (x !in 0 until width) continue
            for (y in 0 until height) {
                mask[y * width + x] = false
            }
        }
    }

    private fun dilate(mask: BooleanArray, width: Int, height: Int) {
        val source = mask.copyOf()
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                if (!source[y * width + x]) continue
                mask[(y - 1) * width + x] = true
                mask[(y + 1) * width + x] = true
                mask[y * width + x - 1] = true
                mask[y * width + x + 1] = true
            }
        }
    }

    private fun saveMask(mask: BooleanArray, width: Int, height: Int, path: String) {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                image.setRGB(x, y, if (mask[y * width + x]) WHITE else BLACK)
            }
        }
        ImageIO.write(image, "png", File(path))
        image.flush()
    }

    private fun emptyResult(graphRegion: GraphRegion): CurveMaskResult =
        CurveMaskResult(
            rawMaskPath = null,
            cleanMaskPath = null,
            graphRegion = graphRegion,
            maskWidth = 0,
            maskHeight = 0,
            rawPixelCount = 0,
            cleanPixelCount = 0,
            suppressionApplied = emptyList(),
            timestamp = System.currentTimeMillis(),
        )

    private companion object {
        private const val WHITE = -0x1
        private const val BLACK = -0x1000000
        private const val MIN_USABLE_COLUMN_COVERAGE = 0.30f
        private const val MAX_COMPACT_PLOT_WIDTH = 480
        private const val MAX_COMPACT_PLOT_HEIGHT = 180
        private val NEIGHBORS_4 = listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)
    }
}

private fun Int.toGray(): Int =
    (
        0.299 * ((this shr 16) and 0xFF) +
            0.587 * ((this shr 8) and 0xFF) +
            0.114 * (this and 0xFF)
        ).toInt()

private fun LongArray.sumRect(width: Int, left: Int, top: Int, right: Int, bottom: Int): Long {
    val bottomRight = this[bottom * width + right]
    val leftBand = if (left > 0) this[bottom * width + left - 1] else 0L
    val topBand = if (top > 0) this[(top - 1) * width + right] else 0L
    val overlap = if (left > 0 && top > 0) this[(top - 1) * width + left - 1] else 0L
    return bottomRight - leftBand - topBand + overlap
}

private fun GraphRegion.clampTo(imageWidth: Int, imageHeight: Int): GraphRegion? {
    val left = x.coerceIn(0, imageWidth)
    val top = y.coerceIn(0, imageHeight)
    val right = right.coerceIn(left, imageWidth)
    val bottom = bottom.coerceIn(top, imageHeight)
    if (right - left <= 1 || bottom - top <= 1) return null
    return copy(x = left, y = top, width = right - left, height = bottom - top)
}

private data class ComponentBounds(
    val label: Int,
    val minY: Int,
    val maxY: Int,
) {
    val height: Int get() = maxY - minY + 1
}

package com.chromalab.feature.processing.graph

import com.chromalab.feature.processing.pipeline.DetectionMethod
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

actual class GraphRegionDetector actual constructor() {

    actual fun detect(imagePath: String, imageWidth: Int, imageHeight: Int): GraphRegionResult {
        val image = ImageIO.read(File(imagePath)) ?: return fallbackResult(imageWidth, imageHeight)
        val sampleStep = chooseSampleStep(image.width, image.height)
        val sampledWidth = ((image.width + sampleStep - 1) / sampleStep).coerceAtLeast(1)
        val sampledHeight = ((image.height + sampleStep - 1) / sampleStep).coerceAtLeast(1)
        val gray = IntArray(sampledWidth * sampledHeight)

        for (y in 0 until sampledHeight) {
            for (x in 0 until sampledWidth) {
                val rgb = image.getRGB(
                    (x * sampleStep).coerceAtMost(image.width - 1),
                    (y * sampleStep).coerceAtMost(image.height - 1),
                )
                gray[y * sampledWidth + x] = (
                    0.299 * ((rgb shr 16) and 0xFF) +
                        0.587 * ((rgb shr 8) and 0xFF) +
                        0.114 * (rgb and 0xFF)
                    ).toInt()
            }
        }
        image.flush()

        tryBrightPanelDetection(
            gray = gray,
            sampledWidth = sampledWidth,
            sampledHeight = sampledHeight,
            scale = sampleStep.toFloat(),
            imageWidth = imageWidth,
            imageHeight = imageHeight,
        )?.let { return it }

        tryAxisPanelDetection(
            gray = gray,
            sampledWidth = sampledWidth,
            sampledHeight = sampledHeight,
            scale = sampleStep.toFloat(),
            imageWidth = imageWidth,
            imageHeight = imageHeight,
        )?.let { return it }

        tryLineDetection(
            gray = gray,
            sampledWidth = sampledWidth,
            sampledHeight = sampledHeight,
            scale = sampleStep.toFloat(),
            imageWidth = imageWidth,
            imageHeight = imageHeight,
        )?.let { return it }

        tryContourDetection(
            gray = gray,
            sampledWidth = sampledWidth,
            sampledHeight = sampledHeight,
            scale = sampleStep.toFloat(),
            imageWidth = imageWidth,
            imageHeight = imageHeight,
        )?.let { return it }

        tryDensityDetection(
            gray = gray,
            sampledWidth = sampledWidth,
            sampledHeight = sampledHeight,
            scale = sampleStep.toFloat(),
            imageWidth = imageWidth,
            imageHeight = imageHeight,
        )?.let { return it }

        return marginFallback(imageWidth, imageHeight)
    }

    private fun chooseSampleStep(width: Int, height: Int): Int {
        val longestSide = max(width, height)
        return when {
            longestSide >= 1800 -> 4
            longestSide >= 900 -> 2
            else -> 1
        }
    }

    private fun tryBrightPanelDetection(
        gray: IntArray,
        sampledWidth: Int,
        sampledHeight: Int,
        scale: Float,
        imageWidth: Int,
        imageHeight: Int,
    ): GraphRegionResult? {
        val darkFrameRatio = gray.count { it < 70 }.toFloat() / gray.size.coerceAtLeast(1).toFloat()
        if (darkFrameRatio < 0.12f) return null

        val brightRows = BooleanArray(sampledHeight) { y ->
            val brightCount = (0 until sampledWidth).count { x -> gray[y * sampledWidth + x] > 230 }
            brightCount >= (sampledWidth * 0.62f).roundToInt()
        }

        val bands = mutableListOf<IntRange>()
        var y = 0
        while (y < sampledHeight) {
            if (brightRows[y]) {
                val start = y
                while (y < sampledHeight && brightRows[y]) y++
                val end = y - 1
                if (end - start + 1 >= (sampledHeight * 0.12f).roundToInt().coerceAtLeast(20)) {
                    bands += start..end
                }
            } else {
                y++
            }
        }
        if (bands.isEmpty()) return null

        val regions = bands.mapNotNull { band ->
            val left = (0 until sampledWidth).indexOfFirst { x ->
                var brightCount = 0
                for (row in band) {
                    if (gray[row * sampledWidth + x] > 230) brightCount++
                }
                brightCount >= (band.count() * 0.55f).roundToInt().coerceAtLeast(1)
            }
            val right = (sampledWidth - 1 downTo 0).firstOrNull { x ->
                var brightCount = 0
                for (row in band) {
                    if (gray[row * sampledWidth + x] > 230) brightCount++
                }
                brightCount >= (band.count() * 0.55f).roundToInt().coerceAtLeast(1)
            } ?: -1
            if (left < 0 || right <= left) return@mapNotNull null

            GraphRegion(
                x = (left * scale).roundToInt(),
                y = (band.first * scale).roundToInt(),
                width = ((right - left + 1) * scale).roundToInt().coerceAtLeast(1),
                height = ((band.last - band.first + 1) * scale).roundToInt().coerceAtLeast(1),
                label = if (bands.size > 1) "Bright panel ${bands.indexOf(band) + 1}" else "Bright panel",
            ).clampToImage(imageWidth, imageHeight)
        }

        val filtered = regions.filter { region ->
            region.width >= imageWidth * 0.45f &&
                region.height >= imageHeight * 0.15f &&
                region.area.toFloat() / (imageWidth.toFloat() * imageHeight.toFloat()).coerceAtLeast(1f) >= 0.08f
        }
        if (filtered.isEmpty()) return null

        return GraphRegionResult(
            regions = filtered,
            detectionMethod = DetectionMethod.AUTO,
            confidence = DetectionConfidence.HIGH,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            warnings = listOf("graph.region_from_bright_panel"),
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun tryAxisPanelDetection(
        gray: IntArray,
        sampledWidth: Int,
        sampledHeight: Int,
        scale: Float,
        imageWidth: Int,
        imageHeight: Int,
    ): GraphRegionResult? {
        val darkThreshold = estimateDarkThreshold(gray).coerceAtMost(175)
        val horizontalAxisRows = findHorizontalAxisRows(
            gray = gray,
            sampledWidth = sampledWidth,
            sampledHeight = sampledHeight,
            darkThreshold = darkThreshold,
        )
        if (horizontalAxisRows.size < 2) return null

        val panels = mutableListOf<GraphRegion>()
        horizontalAxisRows.forEachIndexed { index, bottomRow ->
            val topLimit = if (index == 0) 0 else horizontalAxisRows[index - 1]
            val axisRun = findBestVerticalRunEndingNear(
                gray = gray,
                sampledWidth = sampledWidth,
                left = 0,
                right = (sampledWidth * 0.40f).roundToInt().coerceAtLeast(1),
                top = topLimit,
                bottomRow = bottomRow,
                darkThreshold = darkThreshold,
            ) ?: return@forEachIndexed

            val panelHeight = bottomRow - axisRun.startY
            if (panelHeight < (sampledHeight * 0.06f).roundToInt().coerceAtLeast(18)) return@forEachIndexed

            val span = findHorizontalSpanAtRow(
                gray = gray,
                sampledWidth = sampledWidth,
                row = bottomRow,
                startX = axisRun.x,
                darkThreshold = darkThreshold,
            ) ?: return@forEachIndexed
            if (span.last - span.first < (sampledWidth * 0.35f).roundToInt()) return@forEachIndexed

            val padX = (sampledWidth * 0.025f).roundToInt().coerceAtLeast(3)
            val padY = (sampledHeight * 0.010f).roundToInt().coerceAtLeast(2)
            val panelTop = if (panelHeight < (sampledHeight * 0.12f).roundToInt() && index > 0) {
                topLimit
            } else {
                axisRun.startY
            }
            panels += GraphRegion(
                x = (max(0, min(axisRun.x, span.first) - padX) * scale).roundToInt(),
                y = (max(0, panelTop - padY) * scale).roundToInt(),
                width = ((min(sampledWidth, span.last + padX) - max(0, min(axisRun.x, span.first) - padX)) * scale)
                    .roundToInt()
                    .coerceAtLeast(1),
                height = ((min(sampledHeight, bottomRow + padY) - max(0, panelTop - padY)) * scale)
                    .roundToInt()
                    .coerceAtLeast(1),
                label = "Axis panel ${panels.size + 1}",
            ).clampToImage(imageWidth, imageHeight)
        }

        val mergedPanels = mergeOverlappingRegions(panels)
            .filter { region ->
                region.area.toFloat() / (imageWidth.toFloat() * imageHeight.toFloat()).coerceAtLeast(1f) >= 0.035f &&
                    region.height >= imageHeight * 0.09f
            }
        if (mergedPanels.size < 2) return null

        return GraphRegionResult(
            regions = mergedPanels,
            detectionMethod = DetectionMethod.AUTO,
            confidence = DetectionConfidence.HIGH,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            warnings = listOf("graph.region_from_axis_panels"),
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun tryLineDetection(
        gray: IntArray,
        sampledWidth: Int,
        sampledHeight: Int,
        scale: Float,
        imageWidth: Int,
        imageHeight: Int,
    ): GraphRegionResult? {
        val edgeThreshold = 20
        val horizontalProjection = IntArray(sampledHeight)
        for (y in 1 until sampledHeight - 1) {
            var count = 0
            for (x in 1 until sampledWidth - 1) {
                if (abs(gray[(y + 1) * sampledWidth + x] - gray[(y - 1) * sampledWidth + x]) > edgeThreshold) {
                    count++
                }
            }
            horizontalProjection[y] = count
        }

        val verticalProjection = IntArray(sampledWidth)
        for (x in 1 until sampledWidth - 1) {
            var count = 0
            for (y in 1 until sampledHeight - 1) {
                if (abs(gray[y * sampledWidth + x + 1] - gray[y * sampledWidth + x - 1]) > edgeThreshold) {
                    count++
                }
            }
            verticalProjection[x] = count
        }

        val smoothedRows = smooth1D(horizontalProjection, radius = 3)
        val smoothedColumns = smooth1D(verticalProjection, radius = 3)
        val horizontalLines = findPeaks(
            data = smoothedRows,
            threshold = (sampledWidth * 0.15f).roundToInt().coerceAtLeast(4),
            minSpacing = (sampledHeight / 20).coerceAtLeast(2),
        )
        val verticalLines = findPeaks(
            data = smoothedColumns,
            threshold = (sampledHeight * 0.15f).roundToInt().coerceAtLeast(4),
            minSpacing = (sampledWidth / 20).coerceAtLeast(2),
        )

        if (horizontalLines.size < 2 || verticalLines.isEmpty()) return null

        val leftLine = verticalLines.first()
        val rightLine = if (verticalLines.size >= 2) verticalLines.last() else sampledWidth - 1
        if (rightLine - leftLine < sampledWidth / 6) return null

        val darkThreshold = estimateDarkThreshold(gray)
        val regions = mutableListOf<GraphRegion>()
        for (index in 0 until horizontalLines.lastIndex) {
            val top = horizontalLines[index]
            val bottom = horizontalLines[index + 1]
            if (bottom - top < sampledHeight / 10) continue

            val axisRun = findBestVerticalRun(
                gray = gray,
                sampledWidth = sampledWidth,
                left = leftLine,
                right = rightLine,
                top = top,
                bottom = bottom,
                darkThreshold = darkThreshold,
            ) ?: continue

            val regionHeight = bottom - top
            if (axisRun.length < (regionHeight * 0.30f).roundToInt().coerceAtLeast(18)) continue

            val padX = ((rightLine - leftLine) * 0.03f).roundToInt().coerceAtLeast(3)
            val padY = (sampledHeight * 0.012f).roundToInt().coerceAtLeast(2)
            val refinedLeft = max(0, axisRun.x - padX)
            val refinedTop = max(0, axisRun.startY - padY)
            val refinedBottom = min(sampledHeight, axisRun.endY + padY)
            val region = GraphRegion(
                x = (refinedLeft * scale).roundToInt().coerceIn(0, imageWidth - 1),
                y = (refinedTop * scale).roundToInt().coerceIn(0, imageHeight - 1),
                width = ((rightLine - refinedLeft) * scale).roundToInt().coerceAtLeast(1),
                height = ((refinedBottom - refinedTop) * scale).roundToInt().coerceAtLeast(1),
                label = if (horizontalLines.size > 2) "Graph ${index + 1}" else "",
            ).clampToImage(imageWidth, imageHeight)

            if (region.area.toFloat() / (imageWidth.toFloat() * imageHeight.toFloat()).coerceAtLeast(1f) >= 0.03f) {
                regions += region
            }
        }

        if (regions.isEmpty()) return null

        return GraphRegionResult(
            regions = mergeOverlappingRegions(regions),
            detectionMethod = DetectionMethod.AUTO,
            confidence = DetectionConfidence.HIGH,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun tryContourDetection(
        gray: IntArray,
        sampledWidth: Int,
        sampledHeight: Int,
        scale: Float,
        imageWidth: Int,
        imageHeight: Int,
    ): GraphRegionResult? {
        val edges = IntArray(sampledWidth * sampledHeight)
        for (y in 1 until sampledHeight - 1) {
            for (x in 1 until sampledWidth - 1) {
                val gx = abs(gray[y * sampledWidth + x + 1] - gray[y * sampledWidth + x - 1])
                val gy = abs(gray[(y + 1) * sampledWidth + x] - gray[(y - 1) * sampledWidth + x])
                edges[y * sampledWidth + x] = if (gx + gy > 30) 255 else 0
            }
        }

        val rowEdges = IntArray(sampledHeight) { y ->
            (0 until sampledWidth).count { x -> edges[y * sampledWidth + x] > 0 }
        }
        val columnEdges = IntArray(sampledWidth) { x ->
            (0 until sampledHeight).count { y -> edges[y * sampledWidth + x] > 0 }
        }

        val top = rowEdges.indexOfFirst { it > (sampledWidth * 0.03f).roundToInt().coerceAtLeast(2) }
        val bottom = rowEdges.indexOfLast { it > (sampledWidth * 0.03f).roundToInt().coerceAtLeast(2) }
        val left = columnEdges.indexOfFirst { it > (sampledHeight * 0.03f).roundToInt().coerceAtLeast(2) }
        val right = columnEdges.indexOfLast { it > (sampledHeight * 0.03f).roundToInt().coerceAtLeast(2) }

        if (top < 0 || bottom < 0 || left < 0 || right < 0) return null
        if (bottom - top < sampledHeight / 8 || right - left < sampledWidth / 8) return null

        val regions = splitByValleys(
            rowDensity = rowEdges,
            top = top,
            bottom = bottom,
            left = left,
            right = right,
            sampledWidth = sampledWidth,
            sampledHeight = sampledHeight,
            scale = scale,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
        )
        if (regions.isEmpty()) return null

        return GraphRegionResult(
            regions = regions,
            detectionMethod = DetectionMethod.AUTO,
            confidence = if (regions.size > 1) DetectionConfidence.HIGH else DetectionConfidence.MEDIUM,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            warnings = if (regions.size == 1) listOf("graph.region_approximate") else emptyList(),
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun tryDensityDetection(
        gray: IntArray,
        sampledWidth: Int,
        sampledHeight: Int,
        scale: Float,
        imageWidth: Int,
        imageHeight: Int,
    ): GraphRegionResult? {
        val darkThreshold = estimateDarkThreshold(gray)
        val rowDensity = IntArray(sampledHeight) { y ->
            (0 until sampledWidth).count { x -> gray[y * sampledWidth + x] < darkThreshold }
        }
        val columnDensity = IntArray(sampledWidth) { x ->
            (0 until sampledHeight).count { y -> gray[y * sampledWidth + x] < darkThreshold }
        }

        val top = rowDensity.indexOfFirst { it > (sampledWidth * 0.02f).roundToInt().coerceAtLeast(2) }
        val bottom = rowDensity.indexOfLast { it > (sampledWidth * 0.02f).roundToInt().coerceAtLeast(2) }
        val left = columnDensity.indexOfFirst { it > (sampledHeight * 0.02f).roundToInt().coerceAtLeast(2) }
        val right = columnDensity.indexOfLast { it > (sampledHeight * 0.02f).roundToInt().coerceAtLeast(2) }

        if (top < 0 || bottom < 0 || left < 0 || right < 0) return null
        if (bottom - top < sampledHeight / 8 || right - left < sampledWidth / 8) return null

        val regions = splitByValleys(
            rowDensity = rowDensity,
            top = top,
            bottom = bottom,
            left = left,
            right = right,
            sampledWidth = sampledWidth,
            sampledHeight = sampledHeight,
            scale = scale,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
        )
        if (regions.isEmpty()) return null

        return GraphRegionResult(
            regions = regions,
            detectionMethod = DetectionMethod.AUTO,
            confidence = if (regions.size > 1) DetectionConfidence.MEDIUM else DetectionConfidence.LOW,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            warnings = if (regions.size == 1) listOf("graph.region_from_density") else emptyList(),
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun estimateDarkThreshold(gray: IntArray): Int {
        val sorted = gray.copyOf().also { it.sort() }
        val percentile20 = sorted[(sorted.lastIndex * 0.20f).roundToInt()]
        return percentile20.coerceIn(120, 210)
    }

    private fun smooth1D(data: IntArray, radius: Int): IntArray {
        val result = IntArray(data.size)
        for (i in data.indices) {
            var sum = 0L
            var count = 0
            for (offset in -radius..radius) {
                val index = i + offset
                if (index in data.indices) {
                    sum += data[index]
                    count++
                }
            }
            result[i] = (sum / count.coerceAtLeast(1)).toInt()
        }
        return result
    }

    private fun findPeaks(data: IntArray, threshold: Int, minSpacing: Int): List<Int> {
        val peaks = mutableListOf<Int>()
        var index = 0
        while (index < data.size) {
            if (data[index] > threshold) {
                var scan = index
                var bestIndex = index
                var bestValue = data[index]
                while (scan < data.size && data[scan] > threshold / 2) {
                    if (data[scan] > bestValue) {
                        bestValue = data[scan]
                        bestIndex = scan
                    }
                    scan++
                }
                peaks += bestIndex
                index = scan + minSpacing
            } else {
                index++
            }
        }
        return peaks
    }

    private fun findHorizontalAxisRows(
        gray: IntArray,
        sampledWidth: Int,
        sampledHeight: Int,
        darkThreshold: Int,
    ): List<Int> {
        val rowDensity = IntArray(sampledHeight) { y ->
            (0 until sampledWidth).count { x -> gray[y * sampledWidth + x] < darkThreshold }
        }
        val threshold = (sampledWidth * 0.35f).roundToInt().coerceAtLeast(8)
        val minSpacing = (sampledHeight * 0.055f).roundToInt().coerceAtLeast(8)
        val rows = mutableListOf<Int>()

        var y = 0
        while (y < rowDensity.size) {
            if (rowDensity[y] >= threshold) {
                var scan = y
                var bestRow = y
                var bestDensity = rowDensity[y]
                while (scan < rowDensity.size && rowDensity[scan] >= threshold / 2) {
                    if (rowDensity[scan] > bestDensity) {
                        bestDensity = rowDensity[scan]
                        bestRow = scan
                    }
                    scan++
                }
                if (rows.none { abs(it - bestRow) < minSpacing }) {
                    rows += bestRow
                }
                y = scan + minSpacing / 2
            } else {
                y++
            }
        }

        return rows.sorted()
    }

    private fun findBestVerticalRun(
        gray: IntArray,
        sampledWidth: Int,
        left: Int,
        right: Int,
        top: Int,
        bottom: Int,
        darkThreshold: Int,
    ): VerticalRun? {
        val searchRight = min(right, left + ((right - left) * 0.35f).roundToInt().coerceAtLeast(8))
        val maxGap = ((bottom - top) * 0.025f).roundToInt().coerceAtLeast(2)
        var best: VerticalRun? = null

        for (x in left.coerceAtLeast(0)..searchRight.coerceAtMost(sampledWidth - 1)) {
            var y = top.coerceAtLeast(0)
            val endY = bottom.coerceAtMost(gray.size / sampledWidth)
            while (y < endY) {
                while (y < endY && gray[y * sampledWidth + x] >= darkThreshold) y++
                if (y >= endY) break

                val start = y
                var lastDark = y
                var gap = 0
                while (y < endY && gap <= maxGap) {
                    if (gray[y * sampledWidth + x] < darkThreshold) {
                        lastDark = y
                        gap = 0
                    } else {
                        gap++
                    }
                    y++
                }

                val candidate = VerticalRun(
                    x = x,
                    startY = start,
                    endY = lastDark,
                )
                if (candidate.length > (best?.length ?: 0)) {
                    best = candidate
                }
            }
        }

        return best
    }

    private fun findBestVerticalRunEndingNear(
        gray: IntArray,
        sampledWidth: Int,
        left: Int,
        right: Int,
        top: Int,
        bottomRow: Int,
        darkThreshold: Int,
    ): VerticalRun? {
        val maxGap = ((bottomRow - top) * 0.030f).roundToInt().coerceAtLeast(2)
        val endTolerance = ((bottomRow - top) * 0.035f).roundToInt().coerceAtLeast(4)
        var best: VerticalRun? = null
        val bottomLimit = (bottomRow + endTolerance).coerceAtMost(gray.size / sampledWidth - 1)

        for (x in left.coerceAtLeast(0)..right.coerceAtMost(sampledWidth - 1)) {
            var y = top.coerceAtLeast(0)
            while (y <= bottomLimit) {
                while (y <= bottomLimit && gray[y * sampledWidth + x] >= darkThreshold) y++
                if (y > bottomLimit) break

                val start = y
                var lastDark = y
                var gap = 0
                while (y <= bottomLimit && gap <= maxGap) {
                    if (gray[y * sampledWidth + x] < darkThreshold) {
                        lastDark = y
                        gap = 0
                    } else {
                        gap++
                    }
                    y++
                }

                if (lastDark >= bottomRow - endTolerance) {
                    val candidate = VerticalRun(
                        x = x,
                        startY = start,
                        endY = min(lastDark, bottomRow),
                    )
                    if (candidate.length > (best?.length ?: 0)) {
                        best = candidate
                    }
                }
            }
        }

        return best
    }

    private fun findHorizontalSpanAtRow(
        gray: IntArray,
        sampledWidth: Int,
        row: Int,
        startX: Int,
        darkThreshold: Int,
    ): IntRange? {
        val y = row.coerceIn(0, gray.size / sampledWidth - 1)
        var first = -1
        var last = -1
        val rowWindow = max(0, y - 1)..min(gray.size / sampledWidth - 1, y + 1)

        for (x in startX.coerceAtLeast(0) until sampledWidth) {
            val darkNearby = rowWindow.any { scanY -> gray[scanY * sampledWidth + x] < darkThreshold }
            if (darkNearby) {
                if (first < 0) first = x
                last = x
            }
        }

        return if (first >= 0 && last > first) first..last else null
    }

    private fun splitByValleys(
        rowDensity: IntArray,
        top: Int,
        bottom: Int,
        left: Int,
        right: Int,
        sampledWidth: Int,
        sampledHeight: Int,
        scale: Float,
        imageWidth: Int,
        imageHeight: Int,
    ): List<GraphRegion> {
        val regionHeight = bottom - top
        if (regionHeight < 20) return emptyList()

        val smoothed = smooth1D(IntArray(regionHeight) { rowDensity[top + it] }, radius = 5)
        val maxDensity = smoothed.maxOrNull() ?: return emptyList()
        if (maxDensity <= 0) return emptyList()

        val valleyThreshold = (maxDensity * 0.20f).roundToInt()
        val minValleyWidth = (regionHeight * 0.03f).roundToInt().coerceAtLeast(3)
        val valleyCenters = mutableListOf<Int>()
        var index = 0
        while (index < smoothed.size) {
            if (smoothed[index] <= valleyThreshold) {
                val start = index
                while (index < smoothed.size && smoothed[index] <= valleyThreshold) index++
                val end = index
                if (end - start >= minValleyWidth) {
                    val center = top + (start + end) / 2
                    if (center - top > regionHeight / 10 && bottom - center > regionHeight / 10) {
                        valleyCenters += center
                    }
                }
            } else {
                index++
            }
        }

        val splitPoints = mutableListOf(top)
        splitPoints += valleyCenters
        splitPoints += bottom

        val padX = (sampledWidth * 0.01f).roundToInt()
        val padY = (sampledHeight * 0.01f).roundToInt()
        val regions = mutableListOf<GraphRegion>()
        for (regionIndex in 0 until splitPoints.lastIndex) {
            val regionTop = splitPoints[regionIndex]
            val regionBottom = splitPoints[regionIndex + 1]
            if (regionBottom - regionTop < regionHeight / 10) continue

            regions += GraphRegion(
                x = (max(0, left - padX) * scale).roundToInt(),
                y = (max(0, regionTop - padY) * scale).roundToInt(),
                width = (min(sampledWidth, right - left + padX * 2) * scale).roundToInt().coerceAtLeast(1),
                height = (min(sampledHeight - regionTop, regionBottom - regionTop + padY * 2) * scale)
                    .roundToInt()
                    .coerceAtLeast(1),
                label = if (valleyCenters.isNotEmpty()) "Graph ${regionIndex + 1}" else "",
            ).clampToImage(imageWidth, imageHeight)
        }

        return mergeOverlappingRegions(regions)
    }

    private fun mergeOverlappingRegions(regions: List<GraphRegion>): List<GraphRegion> {
        if (regions.size <= 1) return regions

        val sorted = regions.sortedWith(compareBy<GraphRegion> { it.y }.thenBy { it.x })
        val merged = mutableListOf<GraphRegion>()
        sorted.forEach { region ->
            val previous = merged.lastOrNull()
            if (previous != null && previous.verticalOverlapRatio(region) > 0.65f) {
                merged[merged.lastIndex] = previous.union(region)
            } else {
                merged += region
            }
        }
        return merged
    }

    private fun marginFallback(imageWidth: Int, imageHeight: Int): GraphRegionResult {
        val marginX = (imageWidth * 0.10f).roundToInt()
        val marginY = (imageHeight * 0.10f).roundToInt()
        return GraphRegionResult(
            regions = listOf(
                GraphRegion(
                    x = marginX,
                    y = marginY,
                    width = imageWidth - marginX * 2,
                    height = imageHeight - marginY * 2,
                    label = "Auto region",
                ),
            ),
            detectionMethod = DetectionMethod.AUTO,
            confidence = DetectionConfidence.LOW,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            warnings = listOf("graph.region_fallback_margin"),
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun fallbackResult(imageWidth: Int, imageHeight: Int): GraphRegionResult =
        GraphRegionResult(
            regions = listOf(GraphRegion(0, 0, imageWidth, imageHeight, "Full image")),
            detectionMethod = DetectionMethod.MANUAL,
            confidence = DetectionConfidence.LOW,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            warnings = listOf("graph.image_load_failed"),
            timestamp = System.currentTimeMillis(),
        )
}

private data class VerticalRun(
    val x: Int,
    val startY: Int,
    val endY: Int,
) {
    val length: Int get() = endY - startY + 1
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

private fun GraphRegion.verticalOverlapRatio(other: GraphRegion): Float {
    val overlap = min(bottom, other.bottom) - max(y, other.y)
    if (overlap <= 0) return 0f
    return overlap.toFloat() / min(height, other.height).coerceAtLeast(1).toFloat()
}

private fun GraphRegion.union(other: GraphRegion): GraphRegion {
    val left = min(x, other.x)
    val top = min(y, other.y)
    val right = max(right, other.right)
    val bottom = max(bottom, other.bottom)
    return copy(
        x = left,
        y = top,
        width = right - left,
        height = bottom - top,
        label = label.ifBlank { other.label },
    )
}

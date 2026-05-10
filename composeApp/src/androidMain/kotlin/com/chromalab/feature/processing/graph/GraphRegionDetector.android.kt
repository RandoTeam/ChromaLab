package com.chromalab.feature.processing.graph

import android.graphics.BitmapFactory
import com.chromalab.feature.processing.pipeline.DetectionMethod
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Android graph region detector.
 *
 * Multi-pass, lenient strategy — tries progressively weaker methods:
 * 1. Hough-like line detection → find perpendicular line clusters → axis-bounded region
 * 2. Contour rectangles → largest rectangular contour inside the image
 * 3. Ink density heuristic → row/column projection to find graph content area
 * 4. Margin-based fallback → 10% inset from edges (common for printed graphs)
 *
 * DESIGN: Never fails. Low-quality photos get LOW confidence + warning,
 * but processing continues. User can always adjust manually.
 */
actual class GraphRegionDetector actual constructor() {

    actual fun detect(imagePath: String, imageWidth: Int, imageHeight: Int): GraphRegionResult {
        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
        val bitmap = BitmapFactory.decodeFile(imagePath, options)
            ?: return fallbackResult(imageWidth, imageHeight)

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()
        val scale = options.inSampleSize.toFloat()

        println("GRAPH[DETECT] image=${imageWidth}x${imageHeight}, downsampled=${w}x${h}, scale=$scale")

        // Grayscale
        val gray = IntArray(w * h) { i ->
            val p = pixels[i]
            (0.299 * ((p shr 16) and 0xFF) +
                0.587 * ((p shr 8) and 0xFF) +
                0.114 * (p and 0xFF)).toInt()
        }

        // Try method 1: Line-based detection (axes)
        val lineResult = tryLineDetection(gray, w, h, scale, imageWidth, imageHeight)
        if (lineResult != null) {
            println("GRAPH[RESULT] method=LINE, regions=${lineResult.regions.size}, confidence=${lineResult.confidence}")
            return lineResult
        }

        // Try method 2: Contour-based detection
        val contourResult = tryContourDetection(gray, w, h, scale, imageWidth, imageHeight)
        if (contourResult != null) {
            println("GRAPH[RESULT] method=CONTOUR, regions=${contourResult.regions.size}, confidence=${contourResult.confidence}")
            return contourResult
        }

        // Try method 3: Ink density projection
        val densityResult = tryDensityDetection(gray, w, h, scale, imageWidth, imageHeight)
        if (densityResult != null) {
            println("GRAPH[RESULT] method=DENSITY, regions=${densityResult.regions.size}, confidence=${densityResult.confidence}")
            return densityResult
        }

        // Method 4: Margin-based fallback — always succeeds
        println("GRAPH[RESULT] method=FALLBACK")
        return marginFallback(imageWidth, imageHeight)
    }

    /**
     * Method 1: Enhanced line detection via edge projection + peak finding.
     *
     * Improvements over original:
     * - Lower edge threshold (20 vs 30) to catch thin lines on photos
     * - Gaussian-smoothed projection to reduce noise peaks
     * - Lower peak threshold (15% vs 30%) to catch faint axis lines
     * - Multi-graph support: detects stacked graphs separated by axes
     */
    private fun tryLineDetection(
        gray: IntArray, w: Int, h: Int, scale: Float,
        imgW: Int, imgH: Int,
    ): GraphRegionResult? {
        // Sobel edges with lower threshold for photos
        val edgeThreshold = 20

        // Horizontal projection: count strong vertical gradient per row
        // (catches horizontal lines like X-axes)
        val hProjection = IntArray(h)
        for (y in 1 until h - 1) {
            var count = 0
            for (x in 1 until w - 1) {
                val gy = abs(gray[(y + 1) * w + x] - gray[(y - 1) * w + x])
                if (gy > edgeThreshold) count++
            }
            hProjection[y] = count
        }

        // Vertical projection: count strong horizontal gradient per column
        // (catches vertical lines like Y-axes)
        val vProjection = IntArray(w)
        for (x in 1 until w - 1) {
            var count = 0
            for (y in 1 until h - 1) {
                val gx = abs(gray[y * w + x + 1] - gray[y * w + x - 1])
                if (gx > edgeThreshold) count++
            }
            vProjection[x] = count
        }

        // Smooth projections with 1D Gaussian (σ=3) to reduce noise
        val smoothedH = smooth1D(hProjection, sigma = 3)
        val smoothedV = smooth1D(vProjection, sigma = 3)

        // Find peaks: 15% of width/height (vs old 30%)
        val hThreshold = (w * 0.15f).toInt()
        val vThreshold = (h * 0.15f).toInt()

        val hLines = findPeaks(smoothedH, hThreshold, h / 20)
        val vLines = findPeaks(smoothedV, vThreshold, w / 20)

        println("GRAPH[LINE] hLines=${hLines.size} at rows=${hLines.map { (it * scale).toInt() }}, vLines=${vLines.size}")

        if (hLines.size < 2 || vLines.isEmpty()) return null

        val leftLine = vLines.first()
        val rightLine = if (vLines.size >= 2) vLines.last() else w - 1

        // Build regions from consecutive pairs of horizontal lines
        val regions = mutableListOf<GraphRegion>()
        for (i in 0 until hLines.size - 1) {
            val topLine = hLines[i]
            val bottomLine = hLines[i + 1]
            if (bottomLine - topLine < h / 10) continue

            val region = GraphRegion(
                x = (leftLine * scale).roundToInt(),
                y = (topLine * scale).roundToInt(),
                width = ((rightLine - leftLine) * scale).roundToInt().coerceAtLeast(1),
                height = ((bottomLine - topLine) * scale).roundToInt().coerceAtLeast(1),
                label = if (hLines.size > 2) "График ${i + 1}" else "",
            )

            val areaRatio = region.area.toFloat() / (imgW * imgH)
            if (areaRatio >= 0.03f) {
                regions.add(region)
            }
        }

        if (regions.isEmpty()) return null

        return GraphRegionResult(
            regions = regions,
            detectionMethod = DetectionMethod.AUTO,
            confidence = DetectionConfidence.HIGH,
            imageWidth = imgW,
            imageHeight = imgH,
            timestamp = System.currentTimeMillis(),
        )
    }

    /**
     * 1D Gaussian smoothing to reduce noise in projection profiles.
     */
    private fun smooth1D(data: IntArray, sigma: Int): IntArray {
        val kernel = (2 * sigma + 1)
        val result = IntArray(data.size)
        for (i in data.indices) {
            var sum = 0L
            var count = 0
            for (k in -sigma..sigma) {
                val idx = i + k
                if (idx in data.indices) {
                    sum += data[idx]
                    count++
                }
            }
            result[i] = (sum / count).toInt()
        }
        return result
    }

    /**
     * Method 2: Find rectangular contours via edge detection.
     * Now with multi-graph splitting via valley detection.
     */
    private fun tryContourDetection(
        gray: IntArray, w: Int, h: Int, scale: Float,
        imgW: Int, imgH: Int,
    ): GraphRegionResult? {
        // Edge detection with lower threshold (30 vs 50) for photos
        val edges = IntArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val gx = abs(gray[y * w + x + 1] - gray[y * w + x - 1])
                val gy = abs(gray[(y + 1) * w + x] - gray[(y - 1) * w + x])
                edges[y * w + x] = if (gx + gy > 30) 255 else 0
            }
        }

        // Find bounding box of dense edge clusters
        val rowEdges = IntArray(h) { y ->
            (0 until w).count { x -> edges[y * w + x] > 0 }
        }
        val colEdges = IntArray(w) { x ->
            (0 until h).count { y -> edges[y * w + x] > 0 }
        }

        val edgeThresholdRow = (w * 0.03f).toInt()
        val edgeThresholdCol = (h * 0.03f).toInt()

        val topY = rowEdges.indexOfFirst { it > edgeThresholdRow }
        val bottomY = rowEdges.indexOfLast { it > edgeThresholdRow }
        val leftX = colEdges.indexOfFirst { it > edgeThresholdCol }
        val rightX = colEdges.indexOfLast { it > edgeThresholdCol }

        if (topY < 0 || bottomY < 0 || leftX < 0 || rightX < 0) return null
        if (bottomY - topY < h / 8 || rightX - leftX < w / 8) return null

        println("GRAPH[CONTOUR] bounds: top=$topY, bottom=$bottomY, left=$leftX, right=$rightX")

        // Try to split into multiple graphs by finding valleys in row edge density
        val regions = splitByValleys(
            rowDensity = rowEdges,
            topY = topY, bottomY = bottomY,
            leftX = leftX, rightX = rightX,
            w = w, h = h, scale = scale,
        )

        if (regions.isEmpty()) return null

        val confidence = if (regions.size > 1) DetectionConfidence.HIGH else DetectionConfidence.MEDIUM
        val warnings = if (regions.size == 1) {
            listOf("Область графика определена приблизительно — проверьте границы")
        } else emptyList()

        return GraphRegionResult(
            regions = regions,
            detectionMethod = DetectionMethod.AUTO,
            confidence = confidence,
            imageWidth = imgW,
            imageHeight = imgH,
            warnings = warnings,
            timestamp = System.currentTimeMillis(),
        )
    }

    /**
     * Method 3: Ink density projection — find the region with most dark pixels.
     * Works well for printed chromatograms even without clear axes.
     */
    private fun tryDensityDetection(
        gray: IntArray, w: Int, h: Int, scale: Float,
        imgW: Int, imgH: Int,
    ): GraphRegionResult? {
        val darkThreshold = 180 // Pixels darker than this are "ink"

        // Row ink density
        val rowDensity = IntArray(h) { y ->
            (0 until w).count { x -> gray[y * w + x] < darkThreshold }
        }
        // Column ink density
        val colDensity = IntArray(w) { x ->
            (0 until h).count { y -> gray[y * w + x] < darkThreshold }
        }

        val inkThresholdRow = (w * 0.02f).toInt() // At least 2% ink
        val inkThresholdCol = (h * 0.02f).toInt()

        val topY = rowDensity.indexOfFirst { it > inkThresholdRow }
        val bottomY = rowDensity.indexOfLast { it > inkThresholdRow }
        val leftX = colDensity.indexOfFirst { it > inkThresholdCol }
        val rightX = colDensity.indexOfLast { it > inkThresholdCol }

        if (topY < 0 || bottomY < 0 || leftX < 0 || rightX < 0) return null
        if (bottomY - topY < h / 8 || rightX - leftX < w / 8) return null

        println("GRAPH[DENSITY] bounds: top=$topY, bottom=$bottomY, left=$leftX, right=$rightX")

        // Try to split into multiple graphs by finding valleys
        val regions = splitByValleys(
            rowDensity = rowDensity,
            topY = topY, bottomY = bottomY,
            leftX = leftX, rightX = rightX,
            w = w, h = h, scale = scale,
        )

        if (regions.isEmpty()) return null

        val confidence = if (regions.size > 1) DetectionConfidence.MEDIUM else DetectionConfidence.LOW

        return GraphRegionResult(
            regions = regions,
            detectionMethod = DetectionMethod.AUTO,
            confidence = confidence,
            imageWidth = imgW,
            imageHeight = imgH,
            warnings = if (regions.size == 1) listOf(
                "Область определена по плотности содержимого",
                "Рекомендуется проверить и при необходимости скорректировать",
            ) else emptyList(),
            timestamp = System.currentTimeMillis(),
        )
    }

    /**
     * Method 4: Ultimate fallback — 10% inset from edges.
     * Common for printed chromatograms with margins.
     */
    private fun marginFallback(imgW: Int, imgH: Int): GraphRegionResult {
        val mx = (imgW * 0.10f).roundToInt()
        val my = (imgH * 0.10f).roundToInt()

        return GraphRegionResult(
            regions = listOf(
                GraphRegion(mx, my, imgW - mx * 2, imgH - my * 2, "Автоматическая область"),
            ),
            detectionMethod = DetectionMethod.AUTO,
            confidence = DetectionConfidence.LOW,
            imageWidth = imgW,
            imageHeight = imgH,
            warnings = listOf(
                "Не удалось автоматически определить область графика",
                "Выберите область вручную или используйте предложенную",
            ),
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun fallbackResult(imgW: Int, imgH: Int): GraphRegionResult =
        GraphRegionResult(
            regions = listOf(GraphRegion(0, 0, imgW, imgH, "Всё изображение")),
            detectionMethod = DetectionMethod.MANUAL,
            confidence = DetectionConfidence.LOW,
            imageWidth = imgW,
            imageHeight = imgH,
            warnings = listOf("Не удалось загрузить изображение"),
            timestamp = System.currentTimeMillis(),
        )

    /**
     * Find peaks in a projection that exceed threshold, with minimum spacing.
     */
    private fun findPeaks(data: IntArray, threshold: Int, minSpacing: Int): List<Int> {
        val peaks = mutableListOf<Int>()
        var i = 0
        while (i < data.size) {
            if (data[i] > threshold) {
                // Find the center of this peak cluster
                var j = i
                var bestIdx = i
                var bestVal = data[i]
                while (j < data.size && data[j] > threshold / 2) {
                    if (data[j] > bestVal) {
                        bestVal = data[j]
                        bestIdx = j
                    }
                    j++
                }
                peaks.add(bestIdx)
                i = j + minSpacing
            } else {
                i++
            }
        }
        return peaks
    }

    /**
     * Split a detected bounding box into multiple graph regions by finding
     * horizontal "valleys" (empty bands) in the row density profile.
     *
     * Chromatogram sheets with 2-3 stacked graphs always have a clear
     * empty horizontal separator between them (axis labels, whitespace).
     *
     * Algorithm:
     * 1. Smooth the row density profile to reduce noise
     * 2. Compute a threshold = 20% of max density in the region
     * 3. Find contiguous runs of rows below threshold ("valleys")
     * 4. Keep only valleys wider than 3% of the region height
     * 5. Split at valley centers to create sub-regions
     */
    private fun splitByValleys(
        rowDensity: IntArray,
        topY: Int, bottomY: Int,
        leftX: Int, rightX: Int,
        w: Int, h: Int, scale: Float,
    ): List<GraphRegion> {
        val regionH = bottomY - topY
        if (regionH < 20) return emptyList()

        // Smooth the density profile within the content bounds
        val smoothed = smooth1D(
            IntArray(regionH) { rowDensity[topY + it] },
            sigma = 5,
        )

        // Threshold = 20% of max density in the region
        val maxDensity = smoothed.maxOrNull() ?: return emptyList()
        val valleyThreshold = (maxDensity * 0.20f).toInt()

        // Minimum valley width = 3% of region height
        val minValleyWidth = (regionH * 0.03f).toInt().coerceAtLeast(3)

        // Find valleys: contiguous runs below threshold
        val valleyCenters = mutableListOf<Int>()
        var i = 0
        while (i < smoothed.size) {
            if (smoothed[i] <= valleyThreshold) {
                val start = i
                while (i < smoothed.size && smoothed[i] <= valleyThreshold) i++
                val end = i
                val width = end - start
                if (width >= minValleyWidth) {
                    val center = topY + (start + end) / 2
                    // Don't split too close to edges
                    if (center - topY > regionH / 10 && bottomY - center > regionH / 10) {
                        valleyCenters.add(center)
                    }
                }
            } else {
                i++
            }
        }

        println("GRAPH[SPLIT] regionH=$regionH, maxDensity=$maxDensity, valleyThreshold=$valleyThreshold, minValleyWidth=$minValleyWidth")
        println("GRAPH[SPLIT] valleys=${valleyCenters.size} at rows=${valleyCenters.map { (it * scale).toInt() }}")

        // Build regions from split points
        val splitPoints = mutableListOf(topY)
        splitPoints.addAll(valleyCenters)
        splitPoints.add(bottomY)

        val padX = (w * 0.01f).toInt()
        val padY = (h * 0.01f).toInt()

        val regions = mutableListOf<GraphRegion>()
        for (idx in 0 until splitPoints.size - 1) {
            val rTop = splitPoints[idx]
            val rBottom = splitPoints[idx + 1]
            if (rBottom - rTop < regionH / 10) continue // too small

            val region = GraphRegion(
                x = (max(0, leftX - padX) * scale).roundToInt(),
                y = (max(0, rTop - padY) * scale).roundToInt(),
                width = (min(w, rightX - leftX + padX * 2) * scale).roundToInt().coerceAtLeast(1),
                height = ((rBottom - rTop + padY * 2).coerceAtMost(h - rTop) * scale).roundToInt().coerceAtLeast(1),
                label = if (valleyCenters.isNotEmpty()) "График ${idx + 1}" else "",
            )
            regions.add(region)
            println("GRAPH[SPLIT] region ${idx + 1}: (${region.x},${region.y}) ${region.width}x${region.height}")
        }

        return regions
    }
}

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

        // Grayscale
        val gray = IntArray(w * h) { i ->
            val p = pixels[i]
            (0.299 * ((p shr 16) and 0xFF) +
                0.587 * ((p shr 8) and 0xFF) +
                0.114 * (p and 0xFF)).toInt()
        }

        // Try method 1: Line-based detection (axes)
        val lineResult = tryLineDetection(gray, w, h, scale, imageWidth, imageHeight)
        if (lineResult != null) return lineResult

        // Try method 2: Contour-based detection
        val contourResult = tryContourDetection(gray, w, h, scale, imageWidth, imageHeight)
        if (contourResult != null) return contourResult

        // Try method 3: Ink density projection
        val densityResult = tryDensityDetection(gray, w, h, scale, imageWidth, imageHeight)
        if (densityResult != null) return densityResult

        // Method 4: Margin-based fallback — always succeeds
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

        // Lowered from 5% to 3% — catches sparser edge distributions on photos
        val edgeThresholdRow = (w * 0.03f).toInt()
        val edgeThresholdCol = (h * 0.03f).toInt()

        val topY = rowEdges.indexOfFirst { it > edgeThresholdRow }
        val bottomY = rowEdges.indexOfLast { it > edgeThresholdRow }
        val leftX = colEdges.indexOfFirst { it > edgeThresholdCol }
        val rightX = colEdges.indexOfLast { it > edgeThresholdCol }

        if (topY < 0 || bottomY < 0 || leftX < 0 || rightX < 0) return null
        if (bottomY - topY < h / 8 || rightX - leftX < w / 8) return null

        val region = GraphRegion(
            x = (leftX * scale).roundToInt(),
            y = (topY * scale).roundToInt(),
            width = ((rightX - leftX) * scale).roundToInt().coerceAtLeast(1),
            height = ((bottomY - topY) * scale).roundToInt().coerceAtLeast(1),
        )

        return GraphRegionResult(
            regions = listOf(region),
            detectionMethod = DetectionMethod.AUTO,
            confidence = DetectionConfidence.MEDIUM,
            imageWidth = imgW,
            imageHeight = imgH,
            warnings = listOf("Область графика определена приблизительно — проверьте границы"),
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

        // Add small padding (3%)
        val padX = (w * 0.03f).toInt()
        val padY = (h * 0.03f).toInt()
        val region = GraphRegion(
            x = (max(0, leftX - padX) * scale).roundToInt(),
            y = (max(0, topY - padY) * scale).roundToInt(),
            width = (min(w, rightX - leftX + padX * 2) * scale).roundToInt().coerceAtLeast(1),
            height = (min(h, bottomY - topY + padY * 2) * scale).roundToInt().coerceAtLeast(1),
        )

        return GraphRegionResult(
            regions = listOf(region),
            detectionMethod = DetectionMethod.AUTO,
            confidence = DetectionConfidence.LOW,
            imageWidth = imgW,
            imageHeight = imgH,
            warnings = listOf(
                "Область определена по плотности содержимого",
                "Рекомендуется проверить и при необходимости скорректировать",
            ),
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
}

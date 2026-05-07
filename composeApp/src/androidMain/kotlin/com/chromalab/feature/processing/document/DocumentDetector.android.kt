package com.chromalab.feature.processing.document

import android.graphics.BitmapFactory
import com.chromalab.feature.processing.pipeline.DetectionMethod
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Android document detector using edge detection + contour analysis.
 * Finds the largest quadrilateral contour in the image (paper sheet).
 *
 * Algorithm:
 * 1. Downsample + grayscale
 * 2. Gaussian blur (3×3)
 * 3. Sobel edge detection
 * 4. Threshold to binary
 * 5. Find contours (border tracing)
 * 6. Approximate contour to polygon
 * 7. Select the largest 4-point polygon
 * 8. Validate area ratio and aspect ratio
 */
actual class DocumentDetector actual constructor() {

    actual fun detect(imagePath: String): DocumentBounds? {
        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
        val bitmap = BitmapFactory.decodeFile(imagePath, options) ?: return null

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()
        val scale = options.inSampleSize.toFloat()

        // 1. Grayscale
        val gray = IntArray(w * h) { i ->
            val p = pixels[i]
            (0.299 * ((p shr 16) and 0xFF) +
                0.587 * ((p shr 8) and 0xFF) +
                0.114 * (p and 0xFF)).toInt()
        }

        // 2. Gaussian blur 3×3
        val blurred = gaussianBlur3x3(gray, w, h)

        // 3. Sobel edge magnitude
        val edges = sobelEdges(blurred, w, h)

        // 4. Threshold
        val threshold = otsuThreshold(edges)
        val binary = IntArray(w * h) { if (edges[it] > threshold) 255 else 0 }

        // 5. Morphological dilation to connect edges
        val dilated = dilate(binary, w, h)

        // 6. Find contours via border tracing
        val contours = findContours(dilated, w, h)

        // 7. Find the largest quadrilateral
        val imageArea = (w * h).toFloat()
        var bestBounds: DocumentBounds? = null
        var bestArea = 0f

        for (contour in contours) {
            val approx = douglasPeucker(contour, contour.size * 0.02f)
            if (approx.size != 4) continue

            val area = polygonArea(approx)
            val areaRatio = area / imageArea

            // Filter: document should be at least 10% and at most 95% of image
            if (areaRatio < 0.10f || areaRatio > 0.95f) continue

            if (area > bestArea) {
                bestArea = area
                val sorted = sortCorners(approx)
                val aspectRatio = computeAspectRatio(sorted)

                bestBounds = DocumentBounds(
                    corners = DocumentCorners(
                        topLeft = ImagePoint(sorted[0].first * scale, sorted[0].second * scale),
                        topRight = ImagePoint(sorted[1].first * scale, sorted[1].second * scale),
                        bottomRight = ImagePoint(sorted[2].first * scale, sorted[2].second * scale),
                        bottomLeft = ImagePoint(sorted[3].first * scale, sorted[3].second * scale),
                    ),
                    areaRatio = areaRatio,
                    aspectRatio = aspectRatio,
                    isQuadrilateral = true,
                    detectionMethod = DetectionMethod.AUTO,
                    confidence = areaRatio.coerceIn(0f, 1f),
                    timestamp = System.currentTimeMillis(),
                )
            }
        }

        return bestBounds
    }

    private fun gaussianBlur3x3(src: IntArray, w: Int, h: Int): IntArray {
        val dst = IntArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                var sum = 0
                sum += src[(y - 1) * w + (x - 1)]
                sum += src[(y - 1) * w + x] * 2
                sum += src[(y - 1) * w + (x + 1)]
                sum += src[y * w + (x - 1)] * 2
                sum += src[y * w + x] * 4
                sum += src[y * w + (x + 1)] * 2
                sum += src[(y + 1) * w + (x - 1)]
                sum += src[(y + 1) * w + x] * 2
                sum += src[(y + 1) * w + (x + 1)]
                dst[y * w + x] = sum / 16
            }
        }
        return dst
    }

    private fun sobelEdges(src: IntArray, w: Int, h: Int): IntArray {
        val dst = IntArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val gx = -src[(y - 1) * w + (x - 1)] + src[(y - 1) * w + (x + 1)] +
                    -2 * src[y * w + (x - 1)] + 2 * src[y * w + (x + 1)] +
                    -src[(y + 1) * w + (x - 1)] + src[(y + 1) * w + (x + 1)]
                val gy = -src[(y - 1) * w + (x - 1)] - 2 * src[(y - 1) * w + x] - src[(y - 1) * w + (x + 1)] +
                    src[(y + 1) * w + (x - 1)] + 2 * src[(y + 1) * w + x] + src[(y + 1) * w + (x + 1)]
                dst[y * w + x] = sqrt((gx * gx + gy * gy).toFloat()).toInt().coerceAtMost(255)
            }
        }
        return dst
    }

    private fun otsuThreshold(data: IntArray): Int {
        val hist = IntArray(256)
        for (v in data) hist[v.coerceIn(0, 255)]++
        val total = data.size
        var sum = 0L
        for (i in 0..255) sum += i * hist[i]
        var sumB = 0L; var wB = 0; var maxVar = 0.0; var threshold = 0
        for (i in 0..255) {
            wB += hist[i]; if (wB == 0) continue
            val wF = total - wB; if (wF == 0) break
            sumB += i * hist[i]
            val mB = sumB.toDouble() / wB
            val mF = (sum - sumB).toDouble() / wF
            val v = wB.toDouble() * wF * (mB - mF) * (mB - mF)
            if (v > maxVar) { maxVar = v; threshold = i }
        }
        return threshold
    }

    private fun dilate(src: IntArray, w: Int, h: Int): IntArray {
        val dst = src.copyOf()
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                if (src[(y - 1) * w + x] == 255 || src[(y + 1) * w + x] == 255 ||
                    src[y * w + (x - 1)] == 255 || src[y * w + (x + 1)] == 255
                ) {
                    dst[y * w + x] = 255
                }
            }
        }
        return dst
    }

    /**
     * Simple contour finding: scan for connected white regions on the border.
     * Returns list of contours, each contour is a list of (x, y) pairs.
     */
    private fun findContours(binary: IntArray, w: Int, h: Int): List<List<Pair<Float, Float>>> {
        val visited = BooleanArray(w * h)
        val contours = mutableListOf<List<Pair<Float, Float>>>()

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val idx = y * w + x
                if (binary[idx] == 255 && !visited[idx] && hasBackground(binary, w, x, y)) {
                    val contour = traceContour(binary, visited, w, h, x, y)
                    if (contour.size >= 20) {
                        contours.add(contour)
                    }
                }
            }
        }
        return contours
    }

    private fun hasBackground(binary: IntArray, w: Int, x: Int, y: Int): Boolean =
        binary[(y - 1) * w + x] == 0 || binary[(y + 1) * w + x] == 0 ||
            binary[y * w + (x - 1)] == 0 || binary[y * w + (x + 1)] == 0

    private fun traceContour(
        binary: IntArray, visited: BooleanArray,
        w: Int, h: Int, startX: Int, startY: Int,
    ): List<Pair<Float, Float>> {
        val contour = mutableListOf<Pair<Float, Float>>()
        val dx = intArrayOf(1, 1, 0, -1, -1, -1, 0, 1)
        val dy = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)

        var cx = startX; var cy = startY
        var dir = 0
        val maxSteps = w * h / 4

        for (step in 0 until maxSteps) {
            val idx = cy * w + cx
            if (visited[idx] && step > 2 && cx == startX && cy == startY) break
            visited[idx] = true
            contour.add(cx.toFloat() to cy.toFloat())

            // Find next edge pixel
            var found = false
            val startDir = (dir + 6) % 8 // turn left
            for (i in 0 until 8) {
                val d = (startDir + i) % 8
                val nx = cx + dx[d]; val ny = cy + dy[d]
                if (nx in 0 until w && ny in 0 until h && binary[ny * w + nx] == 255) {
                    cx = nx; cy = ny; dir = d; found = true; break
                }
            }
            if (!found) break
        }
        return contour
    }

    /**
     * Douglas-Peucker polygon approximation.
     */
    private fun douglasPeucker(
        points: List<Pair<Float, Float>>,
        epsilon: Float,
    ): List<Pair<Float, Float>> {
        if (points.size <= 2) return points

        var maxDist = 0f; var maxIdx = 0
        val first = points.first(); val last = points.last()

        for (i in 1 until points.size - 1) {
            val d = pointLineDistance(points[i], first, last)
            if (d > maxDist) { maxDist = d; maxIdx = i }
        }

        return if (maxDist > epsilon) {
            val left = douglasPeucker(points.subList(0, maxIdx + 1), epsilon)
            val right = douglasPeucker(points.subList(maxIdx, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun pointLineDistance(p: Pair<Float, Float>, a: Pair<Float, Float>, b: Pair<Float, Float>): Float {
        val num = abs((b.second - a.second) * p.first - (b.first - a.first) * p.second +
            b.first * a.second - b.second * a.first)
        val den = sqrt(((b.second - a.second) * (b.second - a.second) +
            (b.first - a.first) * (b.first - a.first)).toFloat())
        return if (den > 0f) num / den else 0f
    }

    private fun polygonArea(pts: List<Pair<Float, Float>>): Float {
        var area = 0f
        val n = pts.size
        for (i in 0 until n) {
            val j = (i + 1) % n
            area += pts[i].first * pts[j].second
            area -= pts[j].first * pts[i].second
        }
        return abs(area) / 2f
    }

    /**
     * Sort 4 corners: topLeft, topRight, bottomRight, bottomLeft.
     */
    private fun sortCorners(pts: List<Pair<Float, Float>>): List<Pair<Float, Float>> {
        val cx = pts.sumOf { it.first.toDouble() }.toFloat() / pts.size
        val cy = pts.sumOf { it.second.toDouble() }.toFloat() / pts.size
        return pts.sortedBy { atan2((it.second - cy).toDouble(), (it.first - cx).toDouble()) }
            .let { sorted ->
                // Rearrange: TL (top-left) should be the one with smallest x+y
                val bySum = sorted.sortedBy { it.first + it.second }
                val tl = bySum[0]
                val br = bySum[3]
                val byDiff = sorted.sortedBy { it.second - it.first }
                val tr = byDiff[0]
                val bl = byDiff[3]
                listOf(tl, tr, br, bl)
            }
    }

    private fun computeAspectRatio(sorted: List<Pair<Float, Float>>): Float {
        val topW = dist(sorted[0], sorted[1])
        val bottomW = dist(sorted[3], sorted[2])
        val leftH = dist(sorted[0], sorted[3])
        val rightH = dist(sorted[1], sorted[2])
        val avgW = (topW + bottomW) / 2f
        val avgH = (leftH + rightH) / 2f
        return if (avgH > 0f) avgW / avgH else 1f
    }

    private fun dist(a: Pair<Float, Float>, b: Pair<Float, Float>): Float =
        sqrt((a.first - b.first) * (a.first - b.first) + (a.second - b.second) * (a.second - b.second))
}

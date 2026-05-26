package com.chromalab.feature.processing.axis

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.pipeline.DetectionMethod
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Android axis detector.
 *
 * Strategy:
 * 1. Crop to graph region → grayscale → edge detection
 * 2. Horizontal projection → find strongest horizontal line (X axis)
 *    - Usually at the bottom of the graph area
 * 3. Vertical projection → find strongest vertical line (Y axis)
 *    - Usually at the left of the graph area
 * 4. Compute intersection (origin)
 * 5. Validate and return with confidence
 *
 * Lenient: returns partial results if only one axis found.
 */
actual class AxisDetector actual constructor() {

    actual fun detect(imagePath: String, graphRegion: GraphRegion): AxesResult {
        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
        val fullBitmap = BitmapFactory.decodeFile(imagePath, options)
            ?: return noAxesResult()

        val scale = options.inSampleSize.toFloat()
        val rx = (graphRegion.x / scale).roundToInt().coerceIn(0, fullBitmap.width - 1)
        val ry = (graphRegion.y / scale).roundToInt().coerceIn(0, fullBitmap.height - 1)
        val rw = (graphRegion.width / scale).roundToInt().coerceIn(1, fullBitmap.width - rx)
        val rh = (graphRegion.height / scale).roundToInt().coerceIn(1, fullBitmap.height - ry)

        val cropped = Bitmap.createBitmap(fullBitmap, rx, ry, rw, rh)
        if (cropped != fullBitmap) {
            fullBitmap.recycle()
        }

        val w = cropped.width
        val h = cropped.height
        val pixels = IntArray(w * h)
        cropped.getPixels(pixels, 0, w, 0, 0, w, h)
        cropped.recycle()

        // Grayscale
        val gray = IntArray(w * h) { i ->
            val p = pixels[i]
            (0.299 * ((p shr 16) and 0xFF) +
                0.587 * ((p shr 8) and 0xFF) +
                0.114 * (p and 0xFF)).toInt()
        }

        // Sobel edge detection — separate horizontal/vertical components
        // Using 3×3 Sobel for better noise rejection vs simple difference
        val edgesH = IntArray(w * h) // Vertical gradient magnitude (finds horizontal lines)
        val edgesV = IntArray(w * h) // Horizontal gradient magnitude (finds vertical lines)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                // Sobel Y: finds horizontal lines
                val gy = abs(
                    -gray[(y-1)*w+x-1] - 2*gray[(y-1)*w+x] - gray[(y-1)*w+x+1] +
                     gray[(y+1)*w+x-1] + 2*gray[(y+1)*w+x] + gray[(y+1)*w+x+1]
                )
                // Sobel X: finds vertical lines
                val gx = abs(
                    -gray[(y-1)*w+x-1] + gray[(y-1)*w+x+1] +
                    -2*gray[y*w+x-1] + 2*gray[y*w+x+1] +
                    -gray[(y+1)*w+x-1] + gray[(y+1)*w+x+1]
                )
                edgesH[y * w + x] = gy
                edgesV[y * w + x] = gx
            }
        }

        // --- Find X axis (strongest horizontal line) ---
        // Project horizontal edges onto rows
        val hProjection = IntArray(h) { y ->
            var sum = 0
            for (x in 0 until w) sum += edgesH[y * w + x]
            sum
        }

        // X axis is typically in the bottom 70% of the graph
        // (widened from 60% to catch graphs with high baselines)
        val xSearchStart = (h * 0.3f).toInt()
        val xAxisY = findStrongestLine(hProjection, xSearchStart, h - 1)

        // --- Find Y axis (strongest vertical line) ---
        // Project vertical edges onto columns
        val vProjection = IntArray(w) { x ->
            var sum = 0
            for (y in 0 until h) sum += edgesV[y * w + x]
            sum
        }

        // Y axis is typically in the left 50% of the graph
        // (widened from 40% to catch offset layouts)
        val ySearchEnd = (w * 0.5f).toInt()
        val yAxisX = findStrongestLine(vProjection, 0, ySearchEnd)

        // Convert back to full image coordinates
        val xAxis = if (xAxisY >= 0) {
            AxisLine(
                x1 = graphRegion.x.toFloat(),
                y1 = graphRegion.y + xAxisY * scale,
                x2 = (graphRegion.x + graphRegion.width).toFloat(),
                y2 = graphRegion.y + xAxisY * scale,
            )
        } else null

        val yAxis = if (yAxisX >= 0) {
            AxisLine(
                x1 = graphRegion.x + yAxisX * scale,
                y1 = graphRegion.y.toFloat(),
                x2 = graphRegion.x + yAxisX * scale,
                y2 = (graphRegion.y + graphRegion.height).toFloat(),
            )
        } else null

        // Compute origin (intersection)
        val origin = if (xAxis != null && yAxis != null) {
            AxisOrigin(
                x = yAxis.x1, // X coordinate of the Y axis
                y = xAxis.y1, // Y coordinate of the X axis
            )
        } else null

        // Confidence
        val confidence = when {
            xAxis != null && yAxis != null -> 0.9f
            xAxis != null || yAxis != null -> 0.5f
            else -> 0.1f
        }

        val warnings = mutableListOf<String>()
        if (xAxis == null) warnings.add("Ось X не обнаружена — укажите вручную")
        if (yAxis == null) warnings.add("Ось Y не обнаружена — укажите вручную")
        if (xAxis != null && yAxis != null && origin == null) {
            warnings.add("Не удалось определить точку пересечения осей")
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

    /**
     * Find the index of the strongest line in a projection within [from, to].
     * Returns -1 if no significant line found.
     */
    private fun findStrongestLine(projection: IntArray, from: Int, to: Int): Int {
        if (from >= to || from < 0 || to >= projection.size) return -1

        val maxVal = projection.slice(from..to).maxOrNull() ?: return -1
        val avgVal = projection.slice(from..to).average()

        // Line must be above average (>1.5x) — relaxed from 2x
        // to catch thinner axis lines on photos
        if (maxVal < avgVal * 1.5) return -1

        // Find the peak
        for (i in from..to) {
            if (projection[i] == maxVal) return i
        }
        return -1
    }

    private fun noAxesResult(): AxesResult = AxesResult(
        xAxis = null,
        yAxis = null,
        origin = null,
        detectionMethod = DetectionMethod.AUTO,
        confidence = 0f,
        warnings = listOf("Не удалось загрузить изображение — укажите оси вручную"),
        timestamp = System.currentTimeMillis(),
    )
}

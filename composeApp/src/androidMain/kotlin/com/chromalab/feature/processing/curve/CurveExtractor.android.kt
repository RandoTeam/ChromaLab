package com.chromalab.feature.processing.curve

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Android curve extractor.
 *
 * Algorithm:
 * 1. Load cleaned mask → read white pixels (curve candidates)
 * 2. Column scan: for each x, find all y positions with mask=true
 * 3. Select representative y per column:
 *    - Single candidate → use directly (HIGH confidence)
 *    - Multiple candidates → use weighted center of largest cluster
 *    - No candidates → mark for interpolation
 * 4. Outlier removal: remove points that deviate >3σ from local median
 * 5. Gap interpolation: linear interpolation for missing columns
 * 6. Save curve overlay image
 */
actual class CurveExtractor actual constructor() {

    actual fun extract(
        maskPath: String,
        maskWidth: Int,
        maskHeight: Int,
        outputDir: String,
    ): CurveExtractionResult {
        val bitmap = BitmapFactory.decodeFile(maskPath)
            ?: return emptyResult(maskWidth)

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()

        // Build boolean mask (white = curve)
        val mask = BooleanArray(w * h) { i ->
            val p = pixels[i]
            ((p shr 16) and 0xFF) > 128 // R channel > 128 = white
        }

        // Step 1: Column scan — find candidate Y positions per column
        val rawPoints = mutableListOf<CurvePoint>()
        val gapColumns = mutableListOf<Int>()

        for (x in 0 until w) {
            val candidates = mutableListOf<Int>()
            for (y in 0 until h) {
                if (mask[y * w + x]) candidates.add(y)
            }

            when {
                candidates.isEmpty() -> {
                    gapColumns.add(x)
                }
                candidates.size == 1 -> {
                    rawPoints.add(
                        CurvePoint(x, candidates[0].toFloat(), CurvePoint.HIGH_CONFIDENCE),
                    )
                }
                else -> {
                    // Multiple candidates — find the largest continuous cluster
                    val cluster = findLargestCluster(candidates)
                    val centerY = cluster.average().toFloat()
                    val confidence = if (cluster.size <= 5) CurvePoint.HIGH_CONFIDENCE
                    else CurvePoint.LOW_CONFIDENCE // Wide blob = uncertain
                    rawPoints.add(CurvePoint(x, centerY, confidence))
                }
            }
        }

        // Step 2: Outlier removal
        val outlierCount = removeOutliers(rawPoints, windowSize = 15, threshold = 3.0f)

        // Step 3: Gap interpolation
        val allPoints = interpolateGaps(rawPoints, gapColumns, w)
        val interpolatedCount = allPoints.count { it.confidence == CurvePoint.INTERPOLATED }

        // Step 4: Save curve overlay
        val overlayPath = File(outputDir).also { it.mkdirs() }
            .let { File(it, "curve_overlay.png").absolutePath }
        saveCurveOverlay(mask, allPoints, w, h, overlayPath)

        val warnings = mutableListOf<String>()
        val coverage = if (w > 0) allPoints.size.toFloat() / w else 0f
        if (coverage < 0.5f) {
            warnings.add("Покрытие кривой менее 50% — проверьте результат")
        }
        if (outlierCount > allPoints.size * 0.1f) {
            warnings.add("Обнаружено много выбросов ($outlierCount) — возможны артефакты")
        }

        return CurveExtractionResult(
            points = allPoints.sortedBy { it.pixelX },
            maskImagePath = overlayPath,
            totalColumns = w,
            extractedColumns = rawPoints.size,
            interpolatedColumns = interpolatedCount,
            outlierCount = outlierCount,
            warnings = warnings,
            timestamp = System.currentTimeMillis(),
        )
    }

    /**
     * Find the largest continuous cluster of Y values.
     */
    private fun findLargestCluster(sorted: List<Int>): List<Int> {
        if (sorted.isEmpty()) return emptyList()

        val values = sorted.sorted()
        var bestStart = 0
        var bestLen = 1
        var curStart = 0
        var curLen = 1

        for (i in 1 until values.size) {
            if (values[i] - values[i - 1] <= 2) { // Gap ≤2px = same cluster
                curLen++
            } else {
                if (curLen > bestLen) {
                    bestLen = curLen
                    bestStart = curStart
                }
                curStart = i
                curLen = 1
            }
        }
        if (curLen > bestLen) {
            bestLen = curLen
            bestStart = curStart
        }

        return values.subList(bestStart, bestStart + bestLen)
    }

    /**
     * Remove outlier points using local median + threshold.
     * Returns the number of removed points.
     */
    private fun removeOutliers(
        points: MutableList<CurvePoint>,
        windowSize: Int,
        threshold: Float,
    ): Int {
        if (points.size < windowSize) return 0

        val toRemove = mutableSetOf<Int>()

        for (i in points.indices) {
            val start = maxOf(0, i - windowSize / 2)
            val end = minOf(points.size, i + windowSize / 2 + 1)
            val window = points.subList(start, end).map { it.pixelY }.sorted()
            val median = window[window.size / 2]

            // MAD (Median Absolute Deviation)
            val mad = window.map { abs(it - median) }.sorted()[window.size / 2]
            val madScale = if (mad > 0) mad * 1.4826f else 1f // Normalize to σ

            if (abs(points[i].pixelY - median) > threshold * madScale) {
                toRemove.add(i)
            }
        }

        // Remove in reverse order
        toRemove.sortedDescending().forEach { points.removeAt(it) }
        return toRemove.size
    }

    /**
     * Fill gaps using linear interpolation between neighboring known points.
     */
    private fun interpolateGaps(
        known: List<CurvePoint>,
        gaps: List<Int>,
        totalWidth: Int,
    ): List<CurvePoint> {
        if (known.isEmpty()) return emptyList()

        val result = known.toMutableList()

        for (gapX in gaps) {
            // Find nearest known points on each side
            val left = known.lastOrNull { it.pixelX < gapX }
            val right = known.firstOrNull { it.pixelX > gapX }

            val interpolatedY = when {
                left != null && right != null -> {
                    // Linear interpolation
                    val t = (gapX - left.pixelX).toFloat() / (right.pixelX - left.pixelX)
                    left.pixelY + t * (right.pixelY - left.pixelY)
                }
                left != null -> left.pixelY // Extend from left
                right != null -> right.pixelY // Extend from right
                else -> continue
            }

            result.add(CurvePoint(gapX, interpolatedY, CurvePoint.INTERPOLATED))
        }

        return result
    }

    /**
     * Save visualization: mask with curve overlay (red line).
     */
    private fun saveCurveOverlay(
        mask: BooleanArray,
        points: List<CurvePoint>,
        w: Int, h: Int,
        path: String,
    ) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        // Draw mask as dark background
        val bgPixels = IntArray(w * h) { i ->
            if (mask[i]) 0xFF_40_40_40.toInt() else 0xFF_10_10_10.toInt()
        }
        bmp.setPixels(bgPixels, 0, w, 0, 0, w, h)

        // Draw curve line
        val canvas = Canvas(bmp)
        val paint = Paint().apply {
            color = Color.RED
            strokeWidth = 2f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        val sorted = points.sortedBy { it.pixelX }
        for (i in 0 until sorted.size - 1) {
            val p1 = sorted[i]
            val p2 = sorted[i + 1]
            if (p2.pixelX - p1.pixelX <= 3) { // Only connect nearby points
                canvas.drawLine(
                    p1.pixelX.toFloat(), p1.pixelY,
                    p2.pixelX.toFloat(), p2.pixelY,
                    paint,
                )
            }
        }

        // Mark interpolated points in yellow
        val interpPaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.FILL
        }
        for (p in sorted) {
            if (p.confidence == CurvePoint.INTERPOLATED) {
                canvas.drawCircle(p.pixelX.toFloat(), p.pixelY, 2f, interpPaint)
            }
        }

        FileOutputStream(path).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        bmp.recycle()
    }

    private fun emptyResult(totalColumns: Int): CurveExtractionResult = CurveExtractionResult(
        points = emptyList(),
        maskImagePath = null,
        totalColumns = totalColumns,
        extractedColumns = 0,
        interpolatedColumns = 0,
        outlierCount = 0,
        warnings = listOf("Не удалось загрузить маску кривой"),
        timestamp = System.currentTimeMillis(),
    )
}

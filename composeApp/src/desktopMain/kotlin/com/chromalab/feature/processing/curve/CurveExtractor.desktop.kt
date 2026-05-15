package com.chromalab.feature.processing.curve

import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

actual class CurveExtractor actual constructor() {
    actual fun extract(
        maskPath: String,
        maskWidth: Int,
        maskHeight: Int,
        outputDir: String,
    ): CurveExtractionResult {
        val image = ImageIO.read(File(maskPath))
            ?: return emptyResult(maskWidth)
        val width = image.width
        val height = image.height
        val mask = BooleanArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                mask[y * width + x] = image.getRGB(x, y).isWhiteMaskPixel()
            }
        }
        image.flush()

        val rawPoints = mutableListOf<CurvePoint>()
        val gapColumns = mutableListOf<Int>()
        for (x in 0 until width) {
            val candidates = mutableListOf<Int>()
            for (y in 0 until height) {
                if (mask[y * width + x]) candidates += y
            }

            if (candidates.isEmpty()) {
                gapColumns += x
                continue
            }

            val cluster = candidates.bestSignalCluster(height)
            if (cluster == null) {
                gapColumns += x
                continue
            }
            rawPoints += CurvePoint(
                pixelX = x,
                pixelY = cluster.first().toFloat(),
                confidence = cluster.confidence(),
            )
        }

        val interpolatedPoints = interpolateShortGaps(rawPoints, gapColumns, maxGap = 6)
        val points = (rawPoints + interpolatedPoints).sortedBy { it.pixelX }
        val overlayPath = File(outputDir).also { it.mkdirs() }
            .resolve("curve_overlay.png")
            .absolutePath
        saveOverlay(mask, points, width, height, overlayPath)

        val result = CurveExtractionResult(
            points = points,
            maskImagePath = overlayPath,
            totalColumns = width,
            extractedColumns = rawPoints.size,
            interpolatedColumns = interpolatedPoints.size,
            outlierCount = 0,
            warnings = emptyList(),
            timestamp = System.currentTimeMillis(),
        )
        val warnings = buildList {
            if (result.coverage < 0.35f && !result.isSparseTraceUsable) add("curve_extract.low_column_coverage")
            if (result.isSparseTraceUsable && result.coverage <= 0.3f) {
                add("curve_extract.sparse_trace_low_column_coverage_accepted")
            }
            if (result.isLocalizedSparseTrace) add("curve_extract.sparse_trace_localized_review_required")
            if (interpolatedPoints.size > rawPoints.size * 0.35f) add("curve_extract.many_short_gap_interpolations")
            if (rawPoints.isEmpty()) add("curve_extract.no_curve_points")
        }

        return result.copy(warnings = warnings)
    }

    private fun List<Int>.bestSignalCluster(height: Int): List<Int>? {
        val clusters = clusters(maxGap = 2)
        val supportStart = height * 0.34f
        val supportedClusters = clusters.filter { it.last() >= supportStart }
        if (supportedClusters.isEmpty()) return null
        return supportedClusters.maxWithOrNull(
            compareBy<List<Int>> { cluster ->
                val span = cluster.last() - cluster.first() + 1
                val bottomSupport = (cluster.last().toFloat() / height.coerceAtLeast(1).toFloat() * 24f).toInt()
                span + bottomSupport
            }.thenBy { it.last() },
        )
    }

    private fun List<Int>.clusters(maxGap: Int): List<List<Int>> {
        if (isEmpty()) return emptyList()
        val sorted = sorted()
        val clusters = mutableListOf<List<Int>>()
        var start = 0
        for (index in 1 until sorted.size) {
            if (sorted[index] - sorted[index - 1] > maxGap) {
                clusters += sorted.subList(start, index)
                start = index
            }
        }
        clusters += sorted.subList(start, sorted.size)
        return clusters
    }

    private fun List<Int>.confidence(): Float =
        when {
            size <= 4 -> CurvePoint.HIGH_CONFIDENCE
            size <= 18 -> 0.8f
            else -> CurvePoint.LOW_CONFIDENCE
        }

    private fun interpolateShortGaps(
        known: List<CurvePoint>,
        gaps: List<Int>,
        maxGap: Int,
    ): List<CurvePoint> {
        if (known.size < 2 || gaps.isEmpty()) return emptyList()
        val byX = known.associateBy { it.pixelX }
        val result = mutableListOf<CurvePoint>()
        var index = 0
        while (index < gaps.size) {
            val startX = gaps[index]
            var endX = startX
            while (index + 1 < gaps.size && gaps[index + 1] == endX + 1) {
                index++
                endX = gaps[index]
            }
            val gapWidth = endX - startX + 1
            val left = byX[startX - 1]
            val right = byX[endX + 1]
            if (gapWidth <= maxGap && left != null && right != null) {
                for (x in startX..endX) {
                    val t = (x - left.pixelX).toFloat() / (right.pixelX - left.pixelX).toFloat()
                    result += CurvePoint(
                        pixelX = x,
                        pixelY = left.pixelY + t * (right.pixelY - left.pixelY),
                        confidence = CurvePoint.INTERPOLATED,
                    )
                }
            }
            index++
        }
        return result
    }

    private fun saveOverlay(
        mask: BooleanArray,
        points: List<CurvePoint>,
        width: Int,
        height: Int,
        path: String,
    ) {
        val overlay = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                overlay.setRGB(x, y, if (mask[y * width + x]) MASK_RGB else BACKGROUND_RGB)
            }
        }

        val graphics = overlay.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.color = Color(0xE5, 0x39, 0x35)
            graphics.stroke = BasicStroke(2f)
            val sorted = points.sortedBy { it.pixelX }
            for (i in 0 until sorted.lastIndex) {
                val first = sorted[i]
                val second = sorted[i + 1]
                if (second.pixelX - first.pixelX <= 3) {
                    graphics.drawLine(
                        first.pixelX,
                        first.pixelY.toInt(),
                        second.pixelX,
                        second.pixelY.toInt(),
                    )
                }
            }
            graphics.color = Color(0xFF, 0xD5, 0x4F)
            sorted.filter { it.confidence == CurvePoint.INTERPOLATED }.forEach { point ->
                graphics.fillOval(point.pixelX - 1, point.pixelY.toInt() - 1, 3, 3)
            }
        } finally {
            graphics.dispose()
        }
        ImageIO.write(overlay, "png", File(path))
        overlay.flush()
    }

    private fun emptyResult(totalColumns: Int): CurveExtractionResult =
        CurveExtractionResult(
            points = emptyList(),
            maskImagePath = null,
            totalColumns = totalColumns,
            extractedColumns = 0,
            interpolatedColumns = 0,
            outlierCount = 0,
            warnings = listOf("curve_extract.mask_not_readable"),
            timestamp = System.currentTimeMillis(),
        )

    private companion object {
        private const val BACKGROUND_RGB = -0x00EFEFF0
        private const val MASK_RGB = -0x00BFBFC0
    }
}

private fun Int.isWhiteMaskPixel(): Boolean =
    ((this shr 16) and 0xFF) > 128 ||
        ((this shr 8) and 0xFF) > 128 ||
        (this and 0xFF) > 128

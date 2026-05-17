package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.axis.AxisLine
import com.chromalab.feature.processing.axis.AxisOrigin
import com.chromalab.feature.processing.graph.GraphRegion
import nu.pattern.OpenCV
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Size
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
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
        return try {
            loadOpenCv()
            val source = Imgcodecs.imread(imagePath)
            if (source.empty()) {
                return unavailable(plotRegion, "axis_tick_geometry.image_not_readable")
            }
            val region = (plotRegion ?: panelRegion).clampedTo(source.width(), source.height())
                ?: return unavailable(plotRegion, "axis_tick_geometry.region_out_of_image").also { source.release() }

            val crop = Mat(source, Rect(region.x, region.y, region.width, region.height))
            val gray = crop.toGray()
            val blurred = Mat()
            val edges = Mat()
            Imgproc.GaussianBlur(gray, blurred, Size(3.0, 3.0), 0.0)
            Imgproc.Canny(blurred, edges, 40.0, 140.0)

            val lines = Mat()
            Imgproc.HoughLinesP(
                edges,
                lines,
                1.0,
                PI / 180.0,
                24,
                min(region.width, region.height) * 0.08,
                max(region.width, region.height) * 0.018,
            )
            val segments = lines.toSegments(region.x, region.y)
            val horizontal = segments.filter { it.isHorizontal }
            val vertical = segments.filter { it.isVertical }
            val xAxis = horizontal.selectXAxis(region) ?: boundaryXAxis(region)
            val yAxis = vertical.selectYAxis(region) ?: boundaryYAxis(region)
            val origin = AxisOrigin(yAxis.x1, xAxis.y1)
            val grayBytes = gray.toGrayArray()
            val xTicks = detectXTicks(
                gray = grayBytes,
                width = region.width,
                height = region.height,
                region = region,
                xAxisY = xAxis.y1,
            )
            val yTicks = detectYTicks(
                gray = grayBytes,
                width = region.width,
                height = region.height,
                region = region,
                yAxisX = yAxis.x1,
            )
            val usedBoundaryFallback = horizontal.selectXAxis(region) == null || vertical.selectYAxis(region) == null

            source.release()
            crop.release()
            gray.release()
            blurred.release()
            edges.release()
            lines.release()

            AxisTickGeometryResult(
                available = true,
                source = "opencv_hough_projection_axis_tick_geometry",
                plotRegion = region,
                xAxis = xAxis,
                yAxis = yAxis,
                origin = origin,
                lineSegmentCount = segments.size,
                horizontalLineCount = horizontal.size,
                verticalLineCount = vertical.size,
                xTickPositions = xTicks,
                yTickPositions = yTicks,
                readyForOcrValueMatching = xTicks.size >= 2 && yTicks.size >= 2,
                warnings = buildList {
                    if (usedBoundaryFallback) add("axis_tick_geometry.boundary_axis_fallback")
                    if (xTicks.size < 2) add("axis_tick_geometry.x_tick_positions_insufficient")
                    if (yTicks.size < 2) add("axis_tick_geometry.y_tick_positions_insufficient")
                },
            )
        } catch (error: Throwable) {
            unavailable(plotRegion, "axis_tick_geometry.backend_failed:${error::class.simpleName}")
        }
    }

    private fun unavailable(region: GraphRegion?, warning: String): AxisTickGeometryResult =
        AxisTickGeometryResult(
            available = false,
            source = "opencv_hough_projection_axis_tick_geometry",
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

    private fun List<DetectedLineSegment>.selectXAxis(region: GraphRegion): AxisLine? {
        val minLength = region.width * 0.28
        val lowerSearchStart = region.y + region.height * 0.52
        return filter { it.length >= minLength && it.midY >= lowerSearchStart }
            .maxByOrNull { segment ->
                val yBias = ((segment.midY - region.y) / region.height.toDouble()).coerceIn(0.0, 1.0)
                segment.length + yBias * region.width * 1.20
            }
            ?.toAxisLine()
    }

    private fun List<DetectedLineSegment>.selectYAxis(region: GraphRegion): AxisLine? {
        val minLength = region.height * 0.22
        val rightSearchLimit = region.x + region.width * 0.28
        return filter { it.length >= minLength && it.midX <= rightSearchLimit }
            .maxByOrNull { segment ->
                val leftBias = (1.0 - ((segment.midX - region.x) / region.width.toDouble())).coerceIn(0.0, 1.0)
                segment.length + leftBias * region.height * 1.10
            }
            ?.toAxisLine()
    }

    private fun boundaryXAxis(region: GraphRegion): AxisLine =
        AxisLine(
            x1 = region.x.toFloat(),
            y1 = region.bottom.toFloat(),
            x2 = region.right.toFloat(),
            y2 = region.bottom.toFloat(),
        )

    private fun boundaryYAxis(region: GraphRegion): AxisLine =
        AxisLine(
            x1 = region.x.toFloat(),
            y1 = region.y.toFloat(),
            x2 = region.x.toFloat(),
            y2 = region.bottom.toFloat(),
        )

    private fun detectXTicks(
        gray: IntArray,
        width: Int,
        height: Int,
        region: GraphRegion,
        xAxisY: Float,
    ): List<Float> {
        val axisY = (xAxisY - region.y).roundToInt().coerceIn(0, height - 1)
        val top = (axisY - max(2, height / 80)).coerceAtLeast(0)
        val bottom = (axisY + max(4, height / 32)).coerceAtMost(height - 1)
        val threshold = estimateDarkThreshold(gray)
        val positions = mutableListOf<Int>()
        val minRun = 2
        val maxRun = max(5, height / 12)
        for (x in 0 until width) {
            var run = 0
            var best = 0
            for (y in top..bottom) {
                if (gray[y * width + x] <= threshold) {
                    run++
                    best = max(best, run)
                } else {
                    run = 0
                }
            }
            if (best in minRun..maxRun) positions += x
        }
        return positions.toClusterCenters(max(4, width / 90))
            .map { region.x + it.toFloat() }
            .filter { it in region.x.toFloat()..region.right.toFloat() }
    }

    private fun detectYTicks(
        gray: IntArray,
        width: Int,
        height: Int,
        region: GraphRegion,
        yAxisX: Float,
    ): List<Float> {
        val axisX = (yAxisX - region.x).roundToInt().coerceIn(0, width - 1)
        val left = (axisX - max(4, width / 40)).coerceAtLeast(0)
        val right = (axisX + max(3, width / 60)).coerceAtMost(width - 1)
        val threshold = estimateDarkThreshold(gray)
        val positions = mutableListOf<Int>()
        val minRun = 2
        val maxRun = max(5, width / 10)
        for (y in 0 until height) {
            var run = 0
            var best = 0
            for (x in left..right) {
                if (gray[y * width + x] <= threshold) {
                    run++
                    best = max(best, run)
                } else {
                    run = 0
                }
            }
            if (best in minRun..maxRun) positions += y
        }
        return positions.toClusterCenters(max(4, height / 80))
            .map { region.y + it.toFloat() }
            .filter { it in region.y.toFloat()..region.bottom.toFloat() }
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

    private fun estimateDarkThreshold(gray: IntArray): Int {
        if (gray.isEmpty()) return 160
        val sorted = gray.copyOf().also { it.sort() }
        return sorted[(sorted.lastIndex * 0.20f).roundToInt()].coerceIn(75, 190)
    }

    private fun Mat.toGray(): Mat {
        if (channels() == 1) return clone()
        val gray = Mat()
        Imgproc.cvtColor(this, gray, Imgproc.COLOR_BGR2GRAY)
        return gray
    }

    private fun Mat.toGrayArray(): IntArray {
        val bytes = ByteArray((rows() * cols()).coerceAtLeast(0))
        get(0, 0, bytes)
        return IntArray(bytes.size) { index -> bytes[index].toInt() and 0xFF }
    }

    private fun Mat.toSegments(offsetX: Int, offsetY: Int): List<DetectedLineSegment> =
        buildList {
            for (row in 0 until rows()) {
                val values = get(row, 0) ?: continue
                if (values.size < 4) continue
                add(
                    DetectedLineSegment(
                        x1 = offsetX + values[0],
                        y1 = offsetY + values[1],
                        x2 = offsetX + values[2],
                        y2 = offsetY + values[3],
                    ),
                )
            }
        }

    private fun GraphRegion.clampedTo(width: Int, height: Int): GraphRegion? {
        val left = x.coerceIn(0, width - 1)
        val top = y.coerceIn(0, height - 1)
        val right = right.coerceIn(left + 1, width)
        val bottom = bottom.coerceIn(top + 1, height)
        if (right <= left || bottom <= top) return null
        return copy(x = left, y = top, width = right - left, height = bottom - top)
    }

    private data class DetectedLineSegment(
        val x1: Double,
        val y1: Double,
        val x2: Double,
        val y2: Double,
    ) {
        val dx: Double get() = x2 - x1
        val dy: Double get() = y2 - y1
        val length: Double get() = hypot(dx, dy)
        val midX: Double get() = (x1 + x2) / 2.0
        val midY: Double get() = (y1 + y2) / 2.0
        val isHorizontal: Boolean get() = abs(dy) <= max(2.0, abs(dx) * 0.08)
        val isVertical: Boolean get() = abs(dx) <= max(2.0, abs(dy) * 0.08)

        fun toAxisLine(): AxisLine =
            AxisLine(
                x1 = x1.toFloat(),
                y1 = y1.toFloat(),
                x2 = x2.toFloat(),
                y2 = y2.toFloat(),
            )
    }

    private companion object {
        private var loaded = false

        private fun loadOpenCv() {
            if (loaded) return
            OpenCV.loadLocally()
            loaded = true
            check(Core.NATIVE_LIBRARY_NAME.isNotBlank())
        }
    }
}

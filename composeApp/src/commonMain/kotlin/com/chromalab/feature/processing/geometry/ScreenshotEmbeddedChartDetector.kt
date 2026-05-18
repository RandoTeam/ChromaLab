package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion
import kotlin.math.roundToInt

data class ScreenshotEmbeddedChartDetectionResult(
    val candidates: List<ScreenshotEmbeddedChartCandidate> = emptyList(),
    val rejectedCandidates: List<ScreenshotEmbeddedChartRejectedCandidate> = emptyList(),
    val warnings: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
)

data class ScreenshotEmbeddedChartCandidate(
    val graphPanel: GraphRegion,
    val scoreBreakdown: RoiCandidateScoreBreakdown,
    val warnings: List<String> = emptyList(),
)

data class ScreenshotEmbeddedChartRejectedCandidate(
    val region: GraphRegion,
    val scoreBreakdown: RoiCandidateScoreBreakdown,
    val rejectionReasons: List<String>,
)

expect class ScreenshotEmbeddedChartDetector() {
    fun detect(
        imagePath: String,
        imageWidth: Int,
        imageHeight: Int,
    ): ScreenshotEmbeddedChartDetectionResult
}

object ScreenshotEmbeddedChartAnalyzer {
    fun analyze(
        gray: IntArray,
        sampledWidth: Int,
        sampledHeight: Int,
        scale: Float,
        imageWidth: Int,
        imageHeight: Int,
    ): ScreenshotEmbeddedChartDetectionResult {
        if (gray.isEmpty() || sampledWidth <= 0 || sampledHeight <= 0) {
            return ScreenshotEmbeddedChartDetectionResult(
                warnings = listOf("screenshot_chart.image_sample_empty"),
            )
        }

        val brightThreshold = estimateBrightThreshold(gray)
        val darkThreshold = estimateDarkThreshold(gray)
        val bright = BooleanArray(gray.size) { gray[it] >= brightThreshold }
        val components = findBrightComponents(bright, sampledWidth, sampledHeight)
        val imageArea = (imageWidth.toFloat() * imageHeight.toFloat()).coerceAtLeast(1f)
        val accepted = mutableListOf<ScreenshotEmbeddedChartCandidate>()
        val rejected = mutableListOf<ScreenshotEmbeddedChartRejectedCandidate>()
        detectAlreadyCroppedChartPanel(
            gray = gray,
            sampledWidth = sampledWidth,
            sampledHeight = sampledHeight,
            scale = scale,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            brightThreshold = brightThreshold,
            darkThreshold = darkThreshold,
        )?.let { accepted += it }

        components.forEach { component ->
            val region = component.toGraphRegion(scale, imageWidth, imageHeight)
                .expandForPanelLabels(imageWidth, imageHeight)
            val metrics = measureCandidate(
                gray = gray,
                sampledWidth = sampledWidth,
                sampledHeight = sampledHeight,
                component = component,
                scale = scale,
                imageArea = imageArea,
                darkThreshold = darkThreshold,
            )
            val score = RoiCandidateScoreBreakdown(
                axisVisibility = metrics.axisLikeLineScore,
                tickLabelVisibility = metrics.tickLikeTextScore,
                graphFramePlotRectangleConfidence = metrics.rectangularityScore,
                tracePixelDensity = metrics.traceDensityScore,
                textContaminationPenalty = metrics.phoneUiPenalty,
                marginSafety = metrics.marginSafetyScore,
                aspectRatioPlausibility = metrics.aspectScore,
                calibrationViability = metrics.peakLineScore,
                curveCoverageScore = metrics.traceDensityScore,
                total = metrics.totalScore,
                notes = metrics.notes,
            )
            val reasons = metrics.rejectionReasons
            if (reasons.isEmpty()) {
                accepted += ScreenshotEmbeddedChartCandidate(
                    graphPanel = region,
                    scoreBreakdown = score,
                    warnings = metrics.notes + "roi.screenshot_embedded_chart",
                )
            } else {
                rejected += ScreenshotEmbeddedChartRejectedCandidate(
                    region = region,
                    scoreBreakdown = score,
                    rejectionReasons = reasons,
                )
            }
        }

        return ScreenshotEmbeddedChartDetectionResult(
            candidates = accepted
                .sortedByDescending { it.scoreBreakdown.total }
                .take(6),
            rejectedCandidates = rejected
                .sortedByDescending { it.scoreBreakdown.total }
                .take(12),
            warnings = buildList {
                if (accepted.isEmpty()) add("screenshot_chart.no_accepted_bright_panel")
                if (accepted.any { "screenshot_chart.already_cropped_chart_panel" in it.warnings }) {
                    add("screenshot_chart.already_cropped_chart_panel")
                }
                add("screenshot_chart.components:${components.size}")
                add("screenshot_chart.bright_threshold:$brightThreshold")
                add("screenshot_chart.dark_threshold:$darkThreshold")
            },
        )
    }

    private fun detectAlreadyCroppedChartPanel(
        gray: IntArray,
        sampledWidth: Int,
        sampledHeight: Int,
        scale: Float,
        imageWidth: Int,
        imageHeight: Int,
        brightThreshold: Int,
        darkThreshold: Int,
    ): ScreenshotEmbeddedChartCandidate? {
        val imageArea = gray.size.coerceAtLeast(1)
        val lightRatio = gray.count { it >= brightThreshold - 12 }.toFloat() / imageArea.toFloat()
        val veryDarkRatio = gray.count { it <= darkThreshold }.toFloat() / imageArea.toFloat()
        val aspect = sampledWidth.toFloat() / sampledHeight.coerceAtLeast(1).toFloat()
        if (lightRatio < 0.54f || veryDarkRatio !in 0.004f..0.32f || aspect !in 0.75f..5.2f) {
            return null
        }

        val columnDarkCounts = IntArray(sampledWidth)
        val rowDarkCounts = IntArray(sampledHeight)
        for (y in 0 until sampledHeight) {
            for (x in 0 until sampledWidth) {
                if (gray[y * sampledWidth + x] <= darkThreshold) {
                    columnDarkCounts[x]++
                    rowDarkCounts[y]++
                }
            }
        }
        val verticalPeakLines = columnDarkCounts.count { it >= (sampledHeight * 0.13f).roundToInt().coerceAtLeast(6) }
        val horizontalAxisRows = rowDarkCounts.count { it >= (sampledWidth * 0.34f).roundToInt().coerceAtLeast(16) }
        val verticalAxisColumns = columnDarkCounts.count { it >= (sampledHeight * 0.34f).roundToInt().coerceAtLeast(16) }
        if (verticalPeakLines < 3 && horizontalAxisRows == 0 && verticalAxisColumns == 0) {
            return null
        }

        val score = RoiCandidateScoreBreakdown(
            axisVisibility = ((horizontalAxisRows.coerceAtMost(4) + verticalAxisColumns.coerceAtMost(4)) / 8f) * 16f,
            tickLabelVisibility = if (veryDarkRatio in 0.008f..0.20f) 7f else 2f,
            graphFramePlotRectangleConfidence = (lightRatio * 20f).coerceIn(0f, 20f),
            tracePixelDensity = 12f,
            textContaminationPenalty = 0f,
            marginSafety = 8f,
            aspectRatioPlausibility = 10f,
            calibrationViability = (verticalPeakLines.coerceAtMost(18) / 18f) * 14f,
            curveCoverageScore = 12f,
            total = (
                (lightRatio * 20f).coerceIn(0f, 20f) +
                    12f +
                    ((verticalPeakLines.coerceAtMost(18) / 18f) * 14f) +
                    (((horizontalAxisRows.coerceAtMost(4) + verticalAxisColumns.coerceAtMost(4)) / 8f) * 16f) +
                    7f +
                    8f +
                    10f
                ).coerceIn(0f, 100f),
            notes = listOf(
                "screenshot_chart.mode:already_cropped_chart_panel",
                "screenshot_chart.light_ratio:${lightRatio.format2()}",
                "screenshot_chart.dark_ratio:${veryDarkRatio.format2()}",
                "screenshot_chart.vertical_peak_lines:$verticalPeakLines",
                "screenshot_chart.axis_rows:$horizontalAxisRows",
                "screenshot_chart.axis_columns:$verticalAxisColumns",
            ),
        )

        val insetX = (sampledWidth * 0.004f * scale).roundToInt().coerceIn(0, 3)
        val insetY = (sampledHeight * 0.004f * scale).roundToInt().coerceIn(0, 3)
        return ScreenshotEmbeddedChartCandidate(
            graphPanel = GraphRegion(
                x = insetX,
                y = insetY,
                width = imageWidth - insetX * 2,
                height = imageHeight - insetY * 2,
                label = "Already cropped chart panel",
            ).clampToImage(imageWidth, imageHeight),
            scoreBreakdown = score,
            warnings = listOf(
                "screenshot_chart.already_cropped_chart_panel",
                "roi.screenshot_embedded_chart",
            ),
        )
    }

    private fun estimateBrightThreshold(gray: IntArray): Int {
        val histogram = histogram(gray)
        val p88 = percentile(histogram, gray.size, 0.88f)
        val p94 = percentile(histogram, gray.size, 0.94f)
        return maxOf(210, ((p88 + p94) / 2).coerceAtMost(246))
    }

    private fun estimateDarkThreshold(gray: IntArray): Int {
        val histogram = histogram(gray)
        val p18 = percentile(histogram, gray.size, 0.18f)
        val p35 = percentile(histogram, gray.size, 0.35f)
        return (p18 + ((p35 - p18) * 0.35f).roundToInt() + 28).coerceIn(72, 180)
    }

    private fun histogram(gray: IntArray): IntArray {
        val histogram = IntArray(256)
        gray.forEach { histogram[it.coerceIn(0, 255)]++ }
        return histogram
    }

    private fun percentile(histogram: IntArray, total: Int, percentile: Float): Int {
        val target = (total * percentile).roundToInt().coerceIn(1, total.coerceAtLeast(1))
        var seen = 0
        for (i in histogram.indices) {
            seen += histogram[i]
            if (seen >= target) return i
        }
        return 255
    }

    private fun findBrightComponents(
        bright: BooleanArray,
        width: Int,
        height: Int,
    ): List<BrightComponent> {
        val visited = BooleanArray(bright.size)
        val queue = IntArray(bright.size)
        val components = mutableListOf<BrightComponent>()

        for (index in bright.indices) {
            if (!bright[index] || visited[index]) continue
            var head = 0
            var tail = 0
            queue[tail++] = index
            visited[index] = true
            var minX = index % width
            var maxX = minX
            var minY = index / width
            var maxY = minY
            var count = 0

            while (head < tail) {
                val current = queue[head++]
                val x = current % width
                val y = current / width
                count++
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y

                visitBrightNeighbor(x - 1, y, width, height, bright, visited, queue, tail)?.let { tail = it }
                visitBrightNeighbor(x + 1, y, width, height, bright, visited, queue, tail)?.let { tail = it }
                visitBrightNeighbor(x, y - 1, width, height, bright, visited, queue, tail)?.let { tail = it }
                visitBrightNeighbor(x, y + 1, width, height, bright, visited, queue, tail)?.let { tail = it }
            }

            if (count >= 24) {
                components += BrightComponent(minX, minY, maxX, maxY, count)
            }
        }

        return components.sortedByDescending { it.pixelCount }
    }

    private fun visitBrightNeighbor(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        bright: BooleanArray,
        visited: BooleanArray,
        queue: IntArray,
        tail: Int,
    ): Int? {
        if (x !in 0 until width || y !in 0 until height) return null
        val index = y * width + x
        if (!bright[index] || visited[index]) return null
        visited[index] = true
        queue[tail] = index
        return tail + 1
    }

    private fun measureCandidate(
        gray: IntArray,
        sampledWidth: Int,
        sampledHeight: Int,
        component: BrightComponent,
        scale: Float,
        imageArea: Float,
        darkThreshold: Int,
    ): CandidateMetrics {
        val boxWidth = component.width.coerceAtLeast(1)
        val boxHeight = component.height.coerceAtLeast(1)
        val boxArea = boxWidth * boxHeight
        val brightRatio = component.pixelCount.toFloat() / boxArea.coerceAtLeast(1).toFloat()
        val regionAreaRatio = component.originalAreaRatio(scale, imageArea)
        val aspect = boxWidth.toFloat() / boxHeight.toFloat()
        val marginY = component.minY.toFloat() / sampledHeight.coerceAtLeast(1).toFloat()

        var darkPixels = 0
        val columnDarkCounts = IntArray(boxWidth)
        val rowDarkCounts = IntArray(boxHeight)
        for (y in component.minY..component.maxY) {
            for (x in component.minX..component.maxX) {
                if (gray[y * sampledWidth + x] <= darkThreshold) {
                    darkPixels++
                    columnDarkCounts[x - component.minX]++
                    rowDarkCounts[y - component.minY]++
                }
            }
        }
        val darkRatio = darkPixels.toFloat() / boxArea.coerceAtLeast(1).toFloat()
        val verticalPeakLines = columnDarkCounts.count { it >= (boxHeight * 0.16f).roundToInt().coerceAtLeast(6) }
        val horizontalAxisRows = rowDarkCounts.count { it >= (boxWidth * 0.42f).roundToInt().coerceAtLeast(16) }
        val verticalAxisColumns = columnDarkCounts.count { it >= (boxHeight * 0.42f).roundToInt().coerceAtLeast(16) }

        val rectangularityScore = (brightRatio * 20f).coerceIn(0f, 20f)
        val traceDensityScore = when (darkRatio) {
            in 0.010f..0.26f -> 12f
            in 0.004f..0.36f -> 7f
            else -> 0f
        }
        val peakLineScore = (verticalPeakLines.coerceAtMost(18) / 18f) * 14f
        val axisLikeLineScore = ((horizontalAxisRows.coerceAtMost(4) + verticalAxisColumns.coerceAtMost(4)) / 8f) * 16f
        val tickLikeTextScore = if (darkRatio in 0.01f..0.20f) 7f else 1f
        val marginSafetyScore = when {
            marginY < 0.10f && boxHeight < sampledHeight * 0.18f -> 0f
            marginY < 0.16f -> 4f
            else -> 8f
        }
        val aspectScore = when (aspect) {
            in 1.05f..3.8f -> 10f
            in 0.75f..5.2f -> 5f
            else -> 0f
        }
        val phoneUiPenalty = if (marginY < 0.12f && regionAreaRatio < 0.06f) 14f else 0f
        val total = (
            rectangularityScore +
                traceDensityScore +
                peakLineScore +
                axisLikeLineScore +
                tickLikeTextScore +
                marginSafetyScore +
                aspectScore -
                phoneUiPenalty
            ).coerceAtLeast(0f)

        val rejectionReasons = buildList {
            if (regionAreaRatio < 0.055f) add("screenshot_chart.area_too_small")
            if (regionAreaRatio > 0.72f) add("screenshot_chart.area_too_large")
            if (boxWidth < sampledWidth * 0.42f) add("screenshot_chart.too_narrow")
            if (boxHeight < sampledHeight * 0.12f) add("screenshot_chart.too_short")
            if (brightRatio < 0.48f) add("screenshot_chart.low_white_background_ratio")
            if (darkRatio !in 0.004f..0.36f) add("screenshot_chart.dark_trace_density_implausible")
            if (verticalPeakLines < 3 && horizontalAxisRows == 0) add("screenshot_chart.internal_trace_or_axis_missing")
            if (aspect !in 0.75f..5.2f) add("screenshot_chart.implausible_aspect_ratio")
            if (phoneUiPenalty > 0f) add("screenshot_chart.phone_ui_like_component")
        }

        return CandidateMetrics(
            totalScore = total,
            rectangularityScore = rectangularityScore,
            traceDensityScore = traceDensityScore,
            peakLineScore = peakLineScore,
            axisLikeLineScore = axisLikeLineScore,
            tickLikeTextScore = tickLikeTextScore,
            marginSafetyScore = marginSafetyScore,
            aspectScore = aspectScore,
            phoneUiPenalty = phoneUiPenalty,
            rejectionReasons = rejectionReasons,
            notes = listOf(
                "screenshot_chart.area_ratio:${regionAreaRatio.format2()}",
                "screenshot_chart.white_ratio:${brightRatio.format2()}",
                "screenshot_chart.dark_ratio:${darkRatio.format2()}",
                "screenshot_chart.vertical_peak_lines:$verticalPeakLines",
                "screenshot_chart.axis_rows:$horizontalAxisRows",
                "screenshot_chart.axis_columns:$verticalAxisColumns",
            ),
        )
    }

    private data class BrightComponent(
        val minX: Int,
        val minY: Int,
        val maxX: Int,
        val maxY: Int,
        val pixelCount: Int,
    ) {
        val width: Int get() = maxX - minX + 1
        val height: Int get() = maxY - minY + 1

        fun originalAreaRatio(scale: Float, imageArea: Float): Float =
            (width * scale * height * scale) / imageArea.coerceAtLeast(1f)

        fun toGraphRegion(scale: Float, imageWidth: Int, imageHeight: Int): GraphRegion =
            GraphRegion(
                x = (minX * scale).roundToInt(),
                y = (minY * scale).roundToInt(),
                width = (width * scale).roundToInt().coerceAtLeast(1),
                height = (height * scale).roundToInt().coerceAtLeast(1),
                label = "Screenshot embedded chart",
            ).clampToImage(imageWidth, imageHeight)
    }

    private data class CandidateMetrics(
        val totalScore: Float,
        val rectangularityScore: Float,
        val traceDensityScore: Float,
        val peakLineScore: Float,
        val axisLikeLineScore: Float,
        val tickLikeTextScore: Float,
        val marginSafetyScore: Float,
        val aspectScore: Float,
        val phoneUiPenalty: Float,
        val rejectionReasons: List<String>,
        val notes: List<String>,
    )
}

private fun Float.format2(): String =
    ((this * 100f).roundToInt() / 100f).toString()

private fun GraphRegion.expandForPanelLabels(imageWidth: Int, imageHeight: Int): GraphRegion {
    val dx = (width * 0.015f).roundToInt().coerceAtLeast(2)
    val dy = (height * 0.020f).roundToInt().coerceAtLeast(2)
    return GraphRegion(
        x = x - dx,
        y = y - dy,
        width = width + dx * 2,
        height = height + dy * 2,
        label = label,
    ).clampToImage(imageWidth, imageHeight)
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

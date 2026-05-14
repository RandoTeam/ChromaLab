package com.chromalab.feature.processing.graph

import kotlin.math.roundToInt

data class GraphCropBoundaryRisk(
    val topSignalClippingRisk: Boolean,
    val topTouchingDarkRunCount: Int,
    val topDarkPixelRatio: Float,
    val acceptedForCalculation: Boolean,
    val warnings: List<String> = emptyList(),
)

class GraphCropBoundaryAnalyzer {
    private val sampler = GraphRegionRefinementSampler()

    fun analyze(
        imagePath: String,
        region: GraphRegion,
    ): GraphCropBoundaryRisk {
        val sampleResult = sampler.sample(imagePath, region)
        val sample = sampleResult.sample ?: return GraphCropBoundaryRisk(
            topSignalClippingRisk = false,
            topTouchingDarkRunCount = 0,
            topDarkPixelRatio = 0f,
            acceptedForCalculation = false,
            warnings = listOf(sampleResult.warning ?: "crop_boundary.image_not_readable"),
        )

        val threshold = sample.estimateBoundaryThreshold()
        val topBandHeight = (sample.height * 0.040f).roundToInt().coerceIn(3, 18)
        val topDarkPixels = (0 until topBandHeight).sumOf { y ->
            (0 until sample.width).count { x -> sample.gray[y * sample.width + x] < threshold }
        }
        val topDarkRatio = topDarkPixels.toFloat() / (sample.width * topBandHeight).coerceAtLeast(1).toFloat()
        val longRuns = sample.countDarkRunsTouchingTop(
            threshold = threshold,
            topBandHeight = topBandHeight,
        )
        val topSignalClippingRisk = longRuns >= 2 && topDarkRatio in 0.0025f..0.12f
        val warnings = buildList {
            if (topSignalClippingRisk) add("crop.signal_touches_top_edge_possible_clipped_peaks")
        }

        return GraphCropBoundaryRisk(
            topSignalClippingRisk = topSignalClippingRisk,
            topTouchingDarkRunCount = longRuns,
            topDarkPixelRatio = topDarkRatio,
            acceptedForCalculation = !topSignalClippingRisk,
            warnings = warnings,
        )
    }
}

private fun GraphRegionRefinementSample.estimateBoundaryThreshold(): Int {
    val sorted = gray.copyOf().also { it.sort() }
    val percentile = sorted[(sorted.lastIndex * 0.22f).roundToInt()]
    return percentile.coerceIn(95, 185)
}

private fun GraphRegionRefinementSample.countDarkRunsTouchingTop(
    threshold: Int,
    topBandHeight: Int,
): Int {
    val xStart = (width * 0.06f).roundToInt().coerceAtLeast(0)
    val xEnd = (width * 0.96f).roundToInt().coerceAtMost(width - 1)
    val minRun = (height * 0.10f).roundToInt().coerceAtLeast(18)
    var count = 0
    var previousX = -100

    for (x in xStart..xEnd) {
        val firstDark = (0 until topBandHeight).firstOrNull { y ->
            gray[y * width + x] < threshold
        } ?: continue
        var y = firstDark
        var lastDark = firstDark
        var gap = 0
        while (y < height && gap <= 2) {
            if (gray[y * width + x] < threshold) {
                lastDark = y
                gap = 0
            } else {
                gap++
            }
            y++
        }

        if (lastDark - firstDark + 1 >= minRun && x - previousX > 2) {
            count++
            previousX = x
        }
    }

    return count
}

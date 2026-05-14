package com.chromalab.feature.processing.normalize

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.graph.GraphRegionRefinementSample
import com.chromalab.feature.processing.graph.GraphRegionRefinementSampler
import kotlin.math.roundToInt

data class ImageOrientationCorrectionResult(
    val imagePath: String,
    val originalPath: String,
    val width: Int,
    val height: Int,
    val wasRotated: Boolean,
    val rotationDegrees: Int,
    val horizontalRunCount: Int,
    val verticalRunCount: Int,
    val warnings: List<String> = emptyList(),
)

class ImageOrientationCorrector {
    private val sampler = GraphRegionRefinementSampler()
    private val rotator = ImageOrientationRotator()

    fun correct(
        normalized: NormalizedImageResult,
        outputDir: String,
    ): ImageOrientationCorrectionResult {
        val decision = analyzeRightAngleRotation(normalized)
            ?: return normalized.toOrientationResult(
                warnings = listOf("orientation.sample_not_available"),
            )

        if (!decision.rotateCounterClockwise) {
            return normalized.toOrientationResult(
                horizontalRunCount = decision.horizontalRunCount,
                verticalRunCount = decision.verticalRunCount,
            )
        }

        val rotated = rotator.rotateCounterClockwise90(
            imagePath = normalized.normalizedPath,
            outputDir = outputDir,
        ) ?: return normalized.toOrientationResult(
            horizontalRunCount = decision.horizontalRunCount,
            verticalRunCount = decision.verticalRunCount,
            warnings = listOf("orientation.rotation_failed"),
        )

        return ImageOrientationCorrectionResult(
            imagePath = rotated.imagePath,
            originalPath = normalized.normalizedPath,
            width = rotated.width,
            height = rotated.height,
            wasRotated = true,
            rotationDegrees = -90,
            horizontalRunCount = decision.horizontalRunCount,
            verticalRunCount = decision.verticalRunCount,
            warnings = listOf("orientation.rotated_90_counterclockwise"),
        )
    }

    private fun analyzeRightAngleRotation(normalized: NormalizedImageResult): OrientationDecision? {
        if (normalized.width <= normalized.height) return OrientationDecision.noRotation()
        if (maxOf(normalized.width, normalized.height) < 900) return OrientationDecision.noRotation()

        val sample = sampler.sample(
            imagePath = normalized.normalizedPath,
            region = GraphRegion(
                x = 0,
                y = 0,
                width = normalized.width,
                height = normalized.height,
            ),
        ).sample ?: return null

        val threshold = sample.estimateOrientationThreshold()
        val horizontalRuns = sample.countLongHorizontalRuns(threshold)
        val verticalRuns = sample.countLongVerticalRuns(threshold)
        val rotate = horizontalRuns >= 8 && horizontalRuns >= (verticalRuns * 1.35f).roundToInt().coerceAtLeast(1)

        return OrientationDecision(
            rotateCounterClockwise = rotate,
            horizontalRunCount = horizontalRuns,
            verticalRunCount = verticalRuns,
        )
    }
}

private data class OrientationDecision(
    val rotateCounterClockwise: Boolean,
    val horizontalRunCount: Int,
    val verticalRunCount: Int,
) {
    companion object {
        fun noRotation(): OrientationDecision =
            OrientationDecision(
                rotateCounterClockwise = false,
                horizontalRunCount = 0,
                verticalRunCount = 0,
            )
    }
}

internal expect class ImageOrientationRotator() {
    fun rotateCounterClockwise90(
        imagePath: String,
        outputDir: String,
    ): ImageOrientationRotationResult?
}

internal data class ImageOrientationRotationResult(
    val imagePath: String,
    val width: Int,
    val height: Int,
)

private fun NormalizedImageResult.toOrientationResult(
    horizontalRunCount: Int = 0,
    verticalRunCount: Int = 0,
    warnings: List<String> = emptyList(),
): ImageOrientationCorrectionResult =
    ImageOrientationCorrectionResult(
        imagePath = normalizedPath,
        originalPath = normalizedPath,
        width = width,
        height = height,
        wasRotated = false,
        rotationDegrees = 0,
        horizontalRunCount = horizontalRunCount,
        verticalRunCount = verticalRunCount,
        warnings = warnings,
    )

private fun GraphRegionRefinementSample.estimateOrientationThreshold(): Int {
    val sorted = gray.copyOf().also { it.sort() }
    val percentile = sorted[(sorted.lastIndex * 0.18f).roundToInt()]
    return percentile.coerceIn(105, 185)
}

private fun GraphRegionRefinementSample.countLongHorizontalRuns(threshold: Int): Int {
    val minRun = (width * 0.18f).roundToInt().coerceAtLeast(70)
    val candidates = BooleanArray(height) { y ->
        longestDarkRunInRow(y, threshold) >= minRun
    }
    return candidates.countClusters(minSpacing = (height * 0.010f).roundToInt().coerceAtLeast(2))
}

private fun GraphRegionRefinementSample.countLongVerticalRuns(threshold: Int): Int {
    val minRun = (height * 0.18f).roundToInt().coerceAtLeast(70)
    val candidates = BooleanArray(width) { x ->
        longestDarkRunInColumn(x, threshold) >= minRun
    }
    return candidates.countClusters(minSpacing = (width * 0.010f).roundToInt().coerceAtLeast(2))
}

private fun GraphRegionRefinementSample.longestDarkRunInRow(
    y: Int,
    threshold: Int,
): Int {
    var best = 0
    var current = 0
    for (x in 0 until width) {
        if (gray[y * width + x] < threshold) {
            current++
            if (current > best) best = current
        } else {
            current = 0
        }
    }
    return best
}

private fun GraphRegionRefinementSample.longestDarkRunInColumn(
    x: Int,
    threshold: Int,
): Int {
    var best = 0
    var current = 0
    for (y in 0 until height) {
        if (gray[y * width + x] < threshold) {
            current++
            if (current > best) best = current
        } else {
            current = 0
        }
    }
    return best
}

private fun BooleanArray.countClusters(minSpacing: Int): Int {
    var clusters = 0
    var index = 0
    while (index < size) {
        if (this[index]) {
            clusters++
            while (index < size && this[index]) index++
            index += minSpacing
        } else {
            index++
        }
    }
    return clusters
}

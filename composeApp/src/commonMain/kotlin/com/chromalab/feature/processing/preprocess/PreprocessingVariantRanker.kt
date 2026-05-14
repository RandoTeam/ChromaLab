package com.chromalab.feature.processing.preprocess

import com.chromalab.feature.processing.graph.GraphRegion
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

class PreprocessingVariantRanker {
    private val imageSampler = PreprocessingVariantImageSampler()

    fun rank(
        variants: List<PreprocessingVariant>,
        graphRegion: GraphRegion,
    ): List<PreprocessingVariantScore> {
        val scored = variants.map { variant -> scoreVariant(variant, graphRegion) }
        return markSelected(scored)
    }

    private fun scoreVariant(
        variant: PreprocessingVariant,
        graphRegion: GraphRegion,
    ): PreprocessingVariantScore {
        val sampleResult = imageSampler.sample(variant.imagePath, graphRegion)
        val sample = sampleResult.sample
            ?: return variant.unavailableScore(sampleResult.warning ?: "image_not_readable")

        val threshold = estimateDarkThreshold(sample.gray)
        val darkPixelRatio = sample.gray.count { it < threshold }.toFloat() / sample.gray.size.coerceAtLeast(1).toFloat()
        val contrast = sample.gray.contrastScore()
        val edgeDensity = sample.gray.edgeDensity(sample.width, sample.height)
        val horizontalLineStrength = sample.gray.maxRowDarkRatio(sample.width, sample.height, threshold)
        val verticalLineStrength = sample.gray.maxColumnDarkRatio(sample.width, sample.height, threshold)
        val score = (
            densityScore(darkPixelRatio) * 0.25f +
                (edgeDensity * 4f).coerceIn(0f, 1f) * 0.25f +
                contrast * 0.25f +
                max(horizontalLineStrength, verticalLineStrength) * 0.25f
            ).coerceIn(0f, 1f)

        return PreprocessingVariantScore(
            variantId = variant.id,
            kind = variant.kind,
            imagePath = variant.imagePath,
            rank = 0,
            score = score,
            darkPixelRatio = darkPixelRatio,
            edgeDensity = edgeDensity,
            contrast = contrast,
            horizontalLineStrength = horizontalLineStrength,
            verticalLineStrength = verticalLineStrength,
            selected = false,
            warnings = buildList {
                if (darkPixelRatio < 0.003f) add("too_sparse")
                if (darkPixelRatio > 0.60f) add("too_dense")
                if (edgeDensity < 0.002f) add("low_edge_density")
            },
        )
    }
}

internal expect class PreprocessingVariantImageSampler() {
    fun sample(
        imagePath: String,
        graphRegion: GraphRegion,
    ): PreprocessingVariantSampleResult
}

internal data class PreprocessingVariantSampleResult(
    val sample: PreprocessingVariantSample?,
    val warning: String? = null,
)

internal data class PreprocessingVariantSample(
    val width: Int,
    val height: Int,
    val gray: IntArray,
)

internal fun choosePreprocessingSampleStep(width: Int, height: Int): Int {
    val maxSide = max(width, height)
    return when {
        maxSide >= 1200 -> 4
        maxSide >= 500 -> 2
        else -> 1
    }
}

internal fun Int.toPreprocessingGray(): Int =
    (
        0.299 * ((this shr 16) and 0xFF) +
            0.587 * ((this shr 8) and 0xFF) +
            0.114 * (this and 0xFF)
        ).toInt()

private fun estimateDarkThreshold(gray: IntArray): Int {
    val sorted = gray.copyOf().also { it.sort() }
    return sorted[(sorted.lastIndex * 0.25f).roundToInt()].coerceIn(80, 190)
}

private fun IntArray.contrastScore(): Float {
    if (isEmpty()) return 0f
    val mean = average()
    val variance = fold(0.0) { acc, value ->
        val delta = value - mean
        acc + delta * delta
    } / size
    return (sqrt(variance) / 96.0).toFloat().coerceIn(0f, 1f)
}

private fun IntArray.edgeDensity(width: Int, height: Int): Float {
    if (width <= 1 || height <= 1) return 0f
    var edges = 0
    var comparisons = 0
    for (y in 0 until height - 1) {
        for (x in 0 until width - 1) {
            val current = this[y * width + x]
            val right = this[y * width + x + 1]
            val down = this[(y + 1) * width + x]
            if (abs(current - right) + abs(current - down) > 55) edges++
            comparisons++
        }
    }
    return edges.toFloat() / comparisons.coerceAtLeast(1).toFloat()
}

private fun IntArray.maxRowDarkRatio(width: Int, height: Int, threshold: Int): Float {
    if (width <= 0 || height <= 0) return 0f
    var maxRatio = 0f
    for (y in 0 until height) {
        val dark = (0 until width).count { x -> this[y * width + x] < threshold }
        maxRatio = max(maxRatio, dark.toFloat() / width.toFloat())
    }
    return maxRatio
}

private fun IntArray.maxColumnDarkRatio(width: Int, height: Int, threshold: Int): Float {
    if (width <= 0 || height <= 0) return 0f
    var maxRatio = 0f
    for (x in 0 until width) {
        val dark = (0 until height).count { y -> this[y * width + x] < threshold }
        maxRatio = max(maxRatio, dark.toFloat() / height.toFloat())
    }
    return maxRatio
}

private fun densityScore(ratio: Float): Float =
    when {
        ratio < 0.003f -> 0f
        ratio < 0.08f -> ratio / 0.08f
        ratio <= 0.28f -> 1f
        ratio <= 0.60f -> (1f - (ratio - 0.28f) / 0.32f).coerceIn(0.1f, 1f)
        else -> 0.05f
    }

private fun markSelected(scores: List<PreprocessingVariantScore>): List<PreprocessingVariantScore> =
    scores
        .sortedWith(compareByDescending<PreprocessingVariantScore> { it.score }.thenBy { it.variantId })
        .mapIndexed { index, score ->
            score.copy(rank = index + 1, selected = index == 0)
        }

private fun PreprocessingVariant.unavailableScore(warning: String): PreprocessingVariantScore =
    PreprocessingVariantScore(
        variantId = id,
        kind = kind,
        imagePath = imagePath,
        rank = 0,
        score = 0f,
        darkPixelRatio = 0f,
        edgeDensity = 0f,
        contrast = 0f,
        horizontalLineStrength = 0f,
        verticalLineStrength = 0f,
        selected = false,
        warnings = listOf(warning),
    )

package com.chromalab.feature.processing.preprocess

import android.graphics.BitmapFactory
import com.chromalab.feature.processing.graph.GraphRegion

internal actual class PreprocessingVariantImageSampler actual constructor() {
    actual fun sample(
        imagePath: String,
        graphRegion: GraphRegion,
    ): PreprocessingVariantSampleResult {
        val bitmap = BitmapFactory.decodeFile(imagePath)
            ?: return PreprocessingVariantSampleResult(sample = null, warning = "image_not_readable")
        val region = graphRegion.clampTo(bitmap.width, bitmap.height)
            ?: return PreprocessingVariantSampleResult(sample = null, warning = "region_out_of_image").also {
                bitmap.recycle()
            }
        val sampleStep = choosePreprocessingSampleStep(region.width, region.height)
        val sampleWidth = ((region.width + sampleStep - 1) / sampleStep).coerceAtLeast(1)
        val sampleHeight = ((region.height + sampleStep - 1) / sampleStep).coerceAtLeast(1)
        val gray = IntArray(sampleWidth * sampleHeight)

        for (sampleY in 0 until sampleHeight) {
            for (sampleX in 0 until sampleWidth) {
                val pixel = bitmap.getPixel(
                    (region.x + sampleX * sampleStep).coerceAtMost(region.right - 1),
                    (region.y + sampleY * sampleStep).coerceAtMost(region.bottom - 1),
                )
                gray[sampleY * sampleWidth + sampleX] = pixel.toPreprocessingGray()
            }
        }
        bitmap.recycle()

        return PreprocessingVariantSampleResult(
            sample = PreprocessingVariantSample(
                width = sampleWidth,
                height = sampleHeight,
                gray = gray,
            ),
        )
    }
}

private fun GraphRegion.clampTo(imageWidth: Int, imageHeight: Int): GraphRegion? {
    val x1 = x.coerceIn(0, imageWidth)
    val y1 = y.coerceIn(0, imageHeight)
    val x2 = right.coerceIn(x1, imageWidth)
    val y2 = bottom.coerceIn(y1, imageHeight)
    if (x2 - x1 <= 1 || y2 - y1 <= 1) return null
    return copy(x = x1, y = y1, width = x2 - x1, height = y2 - y1)
}

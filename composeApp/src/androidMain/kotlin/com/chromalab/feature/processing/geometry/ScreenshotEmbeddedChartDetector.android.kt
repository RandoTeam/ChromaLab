package com.chromalab.feature.processing.geometry

import android.graphics.BitmapFactory
import kotlin.math.max

actual class ScreenshotEmbeddedChartDetector actual constructor() {
    actual fun detect(
        imagePath: String,
        imageWidth: Int,
        imageHeight: Int,
    ): ScreenshotEmbeddedChartDetectionResult {
        val options = BitmapFactory.Options().apply {
            inSampleSize = chooseSampleStep(imageWidth, imageHeight)
        }
        val bitmap = BitmapFactory.decodeFile(imagePath, options)
            ?: return ScreenshotEmbeddedChartDetectionResult(
                warnings = listOf("screenshot_chart.android_image_not_readable"),
            )
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val gray = IntArray(pixels.size) { index ->
                val pixel = pixels[index]
                (
                    0.299f * ((pixel shr 16) and 0xFF) +
                        0.587f * ((pixel shr 8) and 0xFF) +
                        0.114f * (pixel and 0xFF)
                    ).toInt()
            }
            ScreenshotEmbeddedChartAnalyzer.analyze(
                gray = gray,
                sampledWidth = width,
                sampledHeight = height,
                scale = options.inSampleSize.toFloat(),
                imageWidth = imageWidth,
                imageHeight = imageHeight,
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun chooseSampleStep(width: Int, height: Int): Int {
        val longest = max(width, height)
        return when {
            longest >= 2200 -> 4
            longest >= 1100 -> 2
            else -> 1
        }
    }
}

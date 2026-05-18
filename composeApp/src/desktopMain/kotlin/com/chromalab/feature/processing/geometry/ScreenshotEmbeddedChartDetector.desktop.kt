package com.chromalab.feature.processing.geometry

import java.io.File
import javax.imageio.ImageIO
import kotlin.math.max

actual class ScreenshotEmbeddedChartDetector actual constructor() {
    actual fun detect(
        imagePath: String,
        imageWidth: Int,
        imageHeight: Int,
    ): ScreenshotEmbeddedChartDetectionResult {
        val source = ImageIO.read(File(imagePath))
            ?: return ScreenshotEmbeddedChartDetectionResult(
                warnings = listOf("screenshot_chart.desktop_image_not_readable"),
            )
        return try {
            val sampleStep = chooseSampleStep(source.width, source.height)
            val sampledWidth = ((source.width + sampleStep - 1) / sampleStep).coerceAtLeast(1)
            val sampledHeight = ((source.height + sampleStep - 1) / sampleStep).coerceAtLeast(1)
            val gray = IntArray(sampledWidth * sampledHeight)
            for (y in 0 until sampledHeight) {
                for (x in 0 until sampledWidth) {
                    val rgb = source.getRGB(
                        (x * sampleStep).coerceAtMost(source.width - 1),
                        (y * sampleStep).coerceAtMost(source.height - 1),
                    )
                    gray[y * sampledWidth + x] = (
                        0.299f * ((rgb shr 16) and 0xFF) +
                            0.587f * ((rgb shr 8) and 0xFF) +
                            0.114f * (rgb and 0xFF)
                        ).toInt()
                }
            }
            ScreenshotEmbeddedChartAnalyzer.analyze(
                gray = gray,
                sampledWidth = sampledWidth,
                sampledHeight = sampledHeight,
                scale = sampleStep.toFloat(),
                imageWidth = imageWidth,
                imageHeight = imageHeight,
            )
        } finally {
            source.flush()
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

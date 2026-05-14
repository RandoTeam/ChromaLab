package com.chromalab.feature.processing.normalize

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

internal actual class ImageOrientationRotator actual constructor() {
    actual fun rotateCounterClockwise90(
        imagePath: String,
        outputDir: String,
    ): ImageOrientationRotationResult? {
        val source = ImageIO.read(File(imagePath)) ?: return null
        val rotatedWidth = source.height
        val rotatedHeight = source.width
        val rotated = BufferedImage(rotatedWidth, rotatedHeight, BufferedImage.TYPE_INT_RGB)

        for (y in 0 until source.height) {
            for (x in 0 until source.width) {
                rotated.setRGB(y, source.width - 1 - x, source.getRGB(x, y))
            }
        }

        val outDir = File(outputDir).also { it.mkdirs() }
        val outFile = File(outDir, "orientation_corrected.jpg")
        ImageIO.write(rotated, "jpg", outFile)
        source.flush()
        rotated.flush()

        return ImageOrientationRotationResult(
            imagePath = outFile.absolutePath,
            width = rotatedWidth,
            height = rotatedHeight,
        )
    }
}

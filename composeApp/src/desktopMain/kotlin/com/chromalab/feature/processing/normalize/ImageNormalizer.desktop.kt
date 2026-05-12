package com.chromalab.feature.processing.normalize

import java.io.File
import javax.imageio.ImageIO

/**
 * Desktop normalizer copies file as-is and records real dimensions.
 * EXIF orientation correction is Android-only for now.
 */
actual class ImageNormalizer actual constructor() {

    actual fun normalize(imagePath: String, outputDir: String): NormalizedImageResult? {
        val file = File(imagePath)
        if (!file.exists()) return null
        val image = ImageIO.read(file) ?: return null

        val outDir = File(outputDir).also { it.mkdirs() }
        val outFile = File(outDir, "normalized.jpg")
        file.copyTo(outFile, overwrite = true)

        val result = NormalizedImageResult(
            normalizedPath = outFile.absolutePath,
            originalPath = imagePath,
            width = image.width,
            height = image.height,
            wasRotated = false,
            exifOrientation = 1,
            timestamp = System.currentTimeMillis(),
        )
        image.flush()
        return result
    }
}

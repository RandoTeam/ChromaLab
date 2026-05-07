package com.chromalab.feature.processing.normalize

import java.io.File

/**
 * Desktop stub — copies file as-is (no EXIF processing).
 */
actual class ImageNormalizer actual constructor() {

    actual fun normalize(imagePath: String, outputDir: String): NormalizedImageResult? {
        val file = File(imagePath)
        if (!file.exists()) return null

        val outDir = File(outputDir).also { it.mkdirs() }
        val outFile = File(outDir, "normalized.jpg")
        file.copyTo(outFile, overwrite = true)

        // No EXIF on desktop — assume already normalized
        return NormalizedImageResult(
            normalizedPath = outFile.absolutePath,
            originalPath = imagePath,
            width = 0,
            height = 0,
            wasRotated = false,
            exifOrientation = 1,
            timestamp = System.currentTimeMillis(),
        )
    }
}

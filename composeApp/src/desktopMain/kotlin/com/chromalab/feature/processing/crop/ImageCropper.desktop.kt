package com.chromalab.feature.processing.crop

import java.io.File

/**
 * Desktop stub — copies file as-is.
 */
actual class ImageCropper actual constructor() {

    actual fun crop(imagePath: String, cropRect: CropRect, outputDir: String): CropResult? {
        val file = File(imagePath)
        if (!file.exists()) return null

        val outDir = File(outputDir).also { it.mkdirs() }
        val outFile = File(outDir, "cropped.jpg")
        file.copyTo(outFile, overwrite = true)

        return CropResult(
            croppedPath = outFile.absolutePath,
            sourcePath = imagePath,
            sourceWidth = 0, sourceHeight = 0,
            cropRect = cropRect,
            croppedWidth = cropRect.width, croppedHeight = cropRect.height,
            timestamp = System.currentTimeMillis(),
        )
    }
}

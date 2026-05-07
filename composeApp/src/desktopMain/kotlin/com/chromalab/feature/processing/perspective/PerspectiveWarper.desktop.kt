package com.chromalab.feature.processing.perspective

import com.chromalab.feature.processing.document.DocumentCorners
import com.chromalab.feature.processing.document.ImagePoint
import java.io.File

actual class PerspectiveWarper actual constructor() {
    actual fun warp(
        imagePath: String,
        corners: DocumentCorners,
        outputDir: String,
    ): PerspectiveCorrectionResult? {
        val file = File(imagePath)
        if (!file.exists()) return null

        val outDir = File(outputDir).also { it.mkdirs() }
        val outFile = File(outDir, "perspective_corrected.jpg")
        file.copyTo(outFile, overwrite = true)

        return PerspectiveCorrectionResult(
            correctedPath = outFile.absolutePath,
            sourcePath = imagePath,
            homography = HomographyMatrix(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)),
            sourceCorners = corners,
            outputWidth = 0, outputHeight = 0,
            maxWarpDistance = 0f,
            correctedAspectRatio = 1f,
            isExcessiveWarp = false,
            timestamp = System.currentTimeMillis(),
        )
    }
}

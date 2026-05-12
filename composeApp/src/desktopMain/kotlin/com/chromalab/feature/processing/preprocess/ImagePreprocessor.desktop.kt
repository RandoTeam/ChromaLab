package com.chromalab.feature.processing.preprocess

import java.io.File
import javax.imageio.ImageIO

actual class ImagePreprocessor actual constructor() {
    actual fun preprocess(
        imagePath: String,
        outputDir: String,
        params: PreprocessingParams,
    ): PreprocessingResult? {
        val file = File(imagePath)
        if (!file.exists()) return null
        val image = ImageIO.read(file) ?: return null
        val outDir = File(outputDir).also { it.mkdirs() }

        // Copy as all stages
        val names = listOf(
            "grayscale.jpg",
            "contrast_enhanced.jpg",
            "binary.jpg",
            "morphology.jpg",
            "sharpened.jpg",
            "scan_style.jpg",
        )
        names.forEach { name -> file.copyTo(File(outDir, name), overwrite = true) }

        val result = PreprocessingResult(
            grayscalePath = File(outDir, names[0]).absolutePath,
            contrastEnhancedPath = File(outDir, names[1]).absolutePath,
            binaryPath = File(outDir, names[2]).absolutePath,
            morphologyPath = File(outDir, names[3]).absolutePath,
            sharpenedPath = File(outDir, names[4]).absolutePath,
            scanStylePath = File(outDir, names[5]).absolutePath,
            sourcePath = imagePath,
            params = params,
            width = image.width,
            height = image.height,
            timestamp = System.currentTimeMillis(),
        )
        image.flush()
        return result
    }
}

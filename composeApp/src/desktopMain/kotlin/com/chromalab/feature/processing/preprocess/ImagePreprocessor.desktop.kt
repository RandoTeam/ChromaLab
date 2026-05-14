package com.chromalab.feature.processing.preprocess

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

actual class ImagePreprocessor actual constructor() {
    actual fun preprocess(
        imagePath: String,
        outputDir: String,
        params: PreprocessingParams,
    ): PreprocessingResult? {
        val image = ImageIO.read(File(imagePath)) ?: return null
        val width = image.width
        val height = image.height
        val pixels = IntArray(width * height)
        image.getRGB(0, 0, width, height, pixels, 0, width)
        image.flush()

        val stages = buildPreprocessingStages(
            argbPixels = pixels,
            width = width,
            height = height,
            params = params,
        )
        val outDir = File(outputDir).also { it.mkdirs() }
        val grayscalePath = saveGray(stages.grayscale, width, height, File(outDir, "grayscale.jpg"))
        val contrastPath = saveGray(stages.contrastEnhanced, width, height, File(outDir, "contrast_enhanced.jpg"))
        val sharpenedPath = saveGray(stages.sharpened, width, height, File(outDir, "sharpened.jpg"))
        val scanStylePath = saveGray(stages.scanStyle, width, height, File(outDir, "scan_style.jpg"))
        val binaryPath = saveGray(stages.binary, width, height, File(outDir, "binary.jpg"))
        val morphologyPath = saveGray(stages.morphology, width, height, File(outDir, "morphology.jpg"))

        return PreprocessingResult(
            grayscalePath = grayscalePath,
            contrastEnhancedPath = contrastPath,
            binaryPath = binaryPath,
            morphologyPath = morphologyPath,
            sharpenedPath = sharpenedPath,
            scanStylePath = scanStylePath,
            sourcePath = imagePath,
            params = params,
            width = width,
            height = height,
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun saveGray(gray: IntArray, width: Int, height: Int, file: File): String {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val value = gray[y * width + x].coerceIn(0, 255)
                val rgb = (value shl 16) or (value shl 8) or value
                image.setRGB(x, y, rgb)
            }
        }
        ImageIO.write(image, "jpg", file)
        image.flush()
        return file.absolutePath
    }
}

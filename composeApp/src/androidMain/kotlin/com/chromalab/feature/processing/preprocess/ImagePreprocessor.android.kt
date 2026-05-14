package com.chromalab.feature.processing.preprocess

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

actual class ImagePreprocessor actual constructor() {

    actual fun preprocess(
        imagePath: String,
        outputDir: String,
        params: PreprocessingParams,
    ): PreprocessingResult? {
        val maxDim = 2560
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imagePath, boundsOptions)
        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) return null

        var sampleSize = 1
        while (rawWidth / sampleSize > maxDim || rawHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }
        val bitmap = BitmapFactory.decodeFile(
            imagePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ) ?: return null

        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        bitmap.recycle()

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
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val argb = IntArray(width * height) { index ->
            val value = gray[index].coerceIn(0, 255)
            (0xFF shl 24) or (value shl 16) or (value shl 8) or value
        }
        bitmap.setPixels(argb, 0, width, 0, 0, width, height)
        FileOutputStream(file).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        }
        bitmap.recycle()
        return file.absolutePath
    }
}

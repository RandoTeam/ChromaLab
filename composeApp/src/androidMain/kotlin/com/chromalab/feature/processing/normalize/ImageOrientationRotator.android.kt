package com.chromalab.feature.processing.normalize

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import java.io.File
import java.io.FileOutputStream

internal actual class ImageOrientationRotator actual constructor() {
    actual fun rotateCounterClockwise90(
        imagePath: String,
        outputDir: String,
    ): ImageOrientationRotationResult? {
        val source = BitmapFactory.decodeFile(imagePath) ?: return null
        val matrix = Matrix().apply { postRotate(-90f) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated !== source) source.recycle()

        val outDir = File(outputDir).also { it.mkdirs() }
        val outFile = File(outDir, "orientation_corrected.jpg")
        FileOutputStream(outFile).use { output ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 95, output)
        }

        val result = ImageOrientationRotationResult(
            imagePath = outFile.absolutePath,
            width = rotated.width,
            height = rotated.height,
        )
        rotated.recycle()
        return result
    }
}

package com.chromalab.feature.processing.crop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

/**
 * Android cropper using Bitmap.createBitmap for precise pixel-level crop.
 */
actual class ImageCropper actual constructor() {

    actual fun crop(imagePath: String, cropRect: CropRect, outputDir: String): CropResult? {
        val bitmap = BitmapFactory.decodeFile(imagePath) ?: return null

        // Clamp to image bounds
        val x = cropRect.x.coerceIn(0, bitmap.width - 1)
        val y = cropRect.y.coerceIn(0, bitmap.height - 1)
        val w = cropRect.width.coerceIn(1, bitmap.width - x)
        val h = cropRect.height.coerceIn(1, bitmap.height - y)

        val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)

        val outDir = File(outputDir).also { it.mkdirs() }
        val outFile = File(outDir, "cropped.jpg")
        FileOutputStream(outFile).use { fos ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }

        val result = CropResult(
            croppedPath = outFile.absolutePath,
            sourcePath = imagePath,
            sourceWidth = bitmap.width,
            sourceHeight = bitmap.height,
            cropRect = CropRect(x, y, w, h),
            croppedWidth = cropped.width,
            croppedHeight = cropped.height,
            timestamp = System.currentTimeMillis(),
        )

        cropped.recycle()
        bitmap.recycle()
        return result
    }
}

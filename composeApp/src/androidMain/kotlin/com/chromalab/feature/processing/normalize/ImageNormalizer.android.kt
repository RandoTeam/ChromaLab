package com.chromalab.feature.processing.normalize

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

/**
 * Android image normalizer using ExifInterface.
 * Reads EXIF orientation tag, rotates/flips the bitmap accordingly,
 * and saves a normalized JPEG. The result is deterministic:
 * same input file always produces same orientation and dimensions.
 */
actual class ImageNormalizer actual constructor() {

    actual fun normalize(imagePath: String, outputDir: String): NormalizedImageResult? {
        val file = File(imagePath)
        if (!file.exists()) return null

        // Read EXIF orientation
        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )

        // Decode bitmap
        val original = BitmapFactory.decodeFile(imagePath) ?: return null

        // Build rotation/flip matrix
        val matrix = Matrix()
        var wasRotated = false

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.postRotate(90f)
                wasRotated = true
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.postRotate(180f)
                wasRotated = true
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.postRotate(270f)
                wasRotated = true
            }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.postScale(-1f, 1f)
                wasRotated = true
            }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.postScale(1f, -1f)
                wasRotated = true
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
                wasRotated = true
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
                wasRotated = true
            }
        }

        val normalized = if (wasRotated) {
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)
                .also { if (it !== original) original.recycle() }
        } else {
            original
        }

        // Save normalized image
        val outDir = File(outputDir).also { it.mkdirs() }
        val outFile = File(outDir, "normalized.jpg")
        FileOutputStream(outFile).use { fos ->
            normalized.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }

        val result = NormalizedImageResult(
            normalizedPath = outFile.absolutePath,
            originalPath = imagePath,
            width = normalized.width,
            height = normalized.height,
            wasRotated = wasRotated,
            exifOrientation = orientation,
            timestamp = System.currentTimeMillis(),
        )

        normalized.recycle()
        return result
    }
}

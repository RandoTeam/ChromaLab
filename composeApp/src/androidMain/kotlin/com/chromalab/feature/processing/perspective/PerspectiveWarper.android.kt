package com.chromalab.feature.processing.perspective

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.chromalab.feature.processing.document.DocumentCorners
import com.chromalab.feature.processing.document.ImagePoint
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Android perspective warper using inverse homography + bilinear interpolation.
 * No OpenCV dependency — pure Kotlin pixel manipulation.
 */
actual class PerspectiveWarper actual constructor() {

    actual fun warp(
        imagePath: String,
        corners: DocumentCorners,
        outputDir: String,
    ): PerspectiveCorrectionResult? {
        val bitmap = BitmapFactory.decodeFile(imagePath) ?: return null
        val srcW = bitmap.width
        val srcH = bitmap.height
        val pixels = IntArray(srcW * srcH)
        bitmap.getPixels(pixels, 0, srcW, 0, 0, srcW, srcH)

        // Compute output dimensions from corner distances
        val topW = dist(corners.topLeft, corners.topRight)
        val bottomW = dist(corners.bottomLeft, corners.bottomRight)
        val leftH = dist(corners.topLeft, corners.bottomLeft)
        val rightH = dist(corners.topRight, corners.bottomRight)
        val dstW = max(topW, bottomW).roundToInt().coerceIn(100, 8000)
        val dstH = max(leftH, rightH).roundToInt().coerceIn(100, 8000)

        // Compute inverse homography: dst → src
        // We need to map each dst pixel back to src to sample
        val invH = computeHomography(
            src = DocumentCorners(
                topLeft = ImagePoint(0f, 0f),
                topRight = ImagePoint(dstW.toFloat(), 0f),
                bottomRight = ImagePoint(dstW.toFloat(), dstH.toFloat()),
                bottomLeft = ImagePoint(0f, dstH.toFloat()),
            ),
            dstW = corners.topRight.x - corners.topLeft.x + (corners.bottomRight.x - corners.bottomLeft.x),
            dstH = corners.bottomLeft.y - corners.topLeft.y + (corners.bottomRight.y - corners.topRight.y),
        )

        // Actually, use forward homography src→dst, then invert
        val fwdH = computeHomography(corners, dstW.toFloat(), dstH.toFloat())

        // For inverse mapping, compute inverse of fwdH
        val inv = invertHomography(fwdH)

        // Warp with bilinear interpolation
        val dstPixels = IntArray(dstW * dstH)
        val h = inv.values

        for (dy in 0 until dstH) {
            for (dx in 0 until dstW) {
                // Apply inverse homography: (dx,dy) → (sx,sy) in source
                val w = h[6] * dx + h[7] * dy + h[8]
                if (abs(w) < 1e-10f) continue
                val sx = (h[0] * dx + h[1] * dy + h[2]) / w
                val sy = (h[3] * dx + h[4] * dy + h[5]) / w

                // Bilinear interpolation
                dstPixels[dy * dstW + dx] = bilinearSample(pixels, srcW, srcH, sx, sy)
            }
        }

        bitmap.recycle()

        // Save result
        val outBitmap = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
        outBitmap.setPixels(dstPixels, 0, dstW, 0, 0, dstW, dstH)

        val outDir = File(outputDir).also { it.mkdirs() }
        val outFile = File(outDir, "perspective_corrected.jpg")
        FileOutputStream(outFile).use { fos ->
            outBitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }
        outBitmap.recycle()

        val warpDist = maxWarpDistance(corners, dstW.toFloat(), dstH.toFloat())
        val diagonal = sqrt((dstW * dstW + dstH * dstH).toFloat())
        val isExcessive = warpDist / diagonal > 0.3f // >30% of diagonal = excessive

        return PerspectiveCorrectionResult(
            correctedPath = outFile.absolutePath,
            sourcePath = imagePath,
            homography = fwdH,
            sourceCorners = corners,
            outputWidth = dstW,
            outputHeight = dstH,
            maxWarpDistance = warpDist,
            correctedAspectRatio = dstW.toFloat() / dstH,
            isExcessiveWarp = isExcessive,
            timestamp = System.currentTimeMillis(),
        )
    }

    /**
     * Invert a 3×3 homography matrix.
     */
    private fun invertHomography(m: HomographyMatrix): HomographyMatrix {
        val a = m.values
        val det = a[0] * (a[4] * a[8] - a[5] * a[7]) -
            a[1] * (a[3] * a[8] - a[5] * a[6]) +
            a[2] * (a[3] * a[7] - a[4] * a[6])

        if (abs(det) < 1e-10f) return m // Singular — return identity-like

        val invDet = 1f / det
        val inv = FloatArray(9)
        inv[0] = (a[4] * a[8] - a[5] * a[7]) * invDet
        inv[1] = (a[2] * a[7] - a[1] * a[8]) * invDet
        inv[2] = (a[1] * a[5] - a[2] * a[4]) * invDet
        inv[3] = (a[5] * a[6] - a[3] * a[8]) * invDet
        inv[4] = (a[0] * a[8] - a[2] * a[6]) * invDet
        inv[5] = (a[2] * a[3] - a[0] * a[5]) * invDet
        inv[6] = (a[3] * a[7] - a[4] * a[6]) * invDet
        inv[7] = (a[1] * a[6] - a[0] * a[7]) * invDet
        inv[8] = (a[0] * a[4] - a[1] * a[3]) * invDet

        return HomographyMatrix(inv)
    }

    /**
     * Bilinear interpolation sampling from source pixels.
     */
    private fun bilinearSample(pixels: IntArray, w: Int, h: Int, x: Float, y: Float): Int {
        if (x < 0 || y < 0 || x >= w - 1 || y >= h - 1) return 0 // Black for OOB

        val x0 = x.toInt(); val y0 = y.toInt()
        val x1 = (x0 + 1).coerceAtMost(w - 1)
        val y1 = (y0 + 1).coerceAtMost(h - 1)
        val fx = x - x0; val fy = y - y0

        val p00 = pixels[y0 * w + x0]
        val p10 = pixels[y0 * w + x1]
        val p01 = pixels[y1 * w + x0]
        val p11 = pixels[y1 * w + x1]

        return blendPixel(p00, p10, p01, p11, fx, fy)
    }

    private fun blendPixel(p00: Int, p10: Int, p01: Int, p11: Int, fx: Float, fy: Float): Int {
        val ifx = 1f - fx; val ify = 1f - fy
        val w00 = ifx * ify; val w10 = fx * ify; val w01 = ifx * fy; val w11 = fx * fy

        val a = (((p00 shr 24) and 0xFF) * w00 + ((p10 shr 24) and 0xFF) * w10 +
            ((p01 shr 24) and 0xFF) * w01 + ((p11 shr 24) and 0xFF) * w11).toInt()
        val r = (((p00 shr 16) and 0xFF) * w00 + ((p10 shr 16) and 0xFF) * w10 +
            ((p01 shr 16) and 0xFF) * w01 + ((p11 shr 16) and 0xFF) * w11).toInt()
        val g = (((p00 shr 8) and 0xFF) * w00 + ((p10 shr 8) and 0xFF) * w10 +
            ((p01 shr 8) and 0xFF) * w01 + ((p11 shr 8) and 0xFF) * w11).toInt()
        val b = ((p00 and 0xFF) * w00 + (p10 and 0xFF) * w10 +
            (p01 and 0xFF) * w01 + (p11 and 0xFF) * w11).toInt()

        return (a.coerceIn(0, 255) shl 24) or (r.coerceIn(0, 255) shl 16) or
            (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
    }

    private fun dist(a: ImagePoint, b: ImagePoint): Float =
        sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y))
}

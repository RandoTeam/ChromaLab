package com.chromalab.feature.processing.perspective

import com.chromalab.feature.processing.document.DocumentCorners
import com.chromalab.feature.processing.document.ImagePoint
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

actual class PerspectiveWarper actual constructor() {
    actual fun warp(
        imagePath: String,
        corners: DocumentCorners,
        outputDir: String,
    ): PerspectiveCorrectionResult? {
        val file = File(imagePath)
        if (!file.exists()) return null
        val source = ImageIO.read(file) ?: return null

        val topW = dist(corners.topLeft, corners.topRight)
        val bottomW = dist(corners.bottomLeft, corners.bottomRight)
        val leftH = dist(corners.topLeft, corners.bottomLeft)
        val rightH = dist(corners.topRight, corners.bottomRight)
        val dstW = max(topW, bottomW).roundToInt().coerceIn(100, 8000)
        val dstH = max(leftH, rightH).roundToInt().coerceIn(100, 8000)

        val fwdH = computeHomography(corners, dstW.toFloat(), dstH.toFloat())
        val invH = invertHomography(fwdH)
        val h = invH.values

        val warped = BufferedImage(dstW, dstH, BufferedImage.TYPE_INT_RGB)
        for (dy in 0 until dstH) {
            for (dx in 0 until dstW) {
                val w = h[6] * dx + h[7] * dy + h[8]
                if (abs(w) < 1e-10f) continue
                val sx = (h[0] * dx + h[1] * dy + h[2]) / w
                val sy = (h[3] * dx + h[4] * dy + h[5]) / w
                warped.setRGB(dx, dy, bilinearSample(source, sx, sy))
            }
        }

        val outDir = File(outputDir).also { it.mkdirs() }
        val outFile = File(outDir, "perspective_corrected.jpg")
        ImageIO.write(warped, "jpg", outFile)
        source.flush()
        warped.flush()

        val warpDist = maxWarpDistance(corners, dstW.toFloat(), dstH.toFloat())
        val diagonal = sqrt((dstW * dstW + dstH * dstH).toFloat())

        return PerspectiveCorrectionResult(
            correctedPath = outFile.absolutePath,
            sourcePath = imagePath,
            homography = fwdH,
            sourceCorners = corners,
            outputWidth = dstW,
            outputHeight = dstH,
            maxWarpDistance = warpDist,
            correctedAspectRatio = dstW.toFloat() / dstH.toFloat(),
            isExcessiveWarp = warpDist / diagonal > 0.3f,
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun invertHomography(matrix: HomographyMatrix): HomographyMatrix {
        val a = matrix.values
        val det = a[0] * (a[4] * a[8] - a[5] * a[7]) -
            a[1] * (a[3] * a[8] - a[5] * a[6]) +
            a[2] * (a[3] * a[7] - a[4] * a[6])
        if (abs(det) < 1e-10f) {
            return HomographyMatrix(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f))
        }

        val invDet = 1f / det
        return HomographyMatrix(
            floatArrayOf(
                (a[4] * a[8] - a[5] * a[7]) * invDet,
                (a[2] * a[7] - a[1] * a[8]) * invDet,
                (a[1] * a[5] - a[2] * a[4]) * invDet,
                (a[5] * a[6] - a[3] * a[8]) * invDet,
                (a[0] * a[8] - a[2] * a[6]) * invDet,
                (a[2] * a[3] - a[0] * a[5]) * invDet,
                (a[3] * a[7] - a[4] * a[6]) * invDet,
                (a[1] * a[6] - a[0] * a[7]) * invDet,
                (a[0] * a[4] - a[1] * a[3]) * invDet,
            ),
        )
    }

    private fun bilinearSample(image: BufferedImage, x: Float, y: Float): Int {
        if (x < 0f || y < 0f || x >= image.width - 1 || y >= image.height - 1) return 0x000000

        val x0 = x.toInt()
        val y0 = y.toInt()
        val x1 = (x0 + 1).coerceAtMost(image.width - 1)
        val y1 = (y0 + 1).coerceAtMost(image.height - 1)
        val fx = x - x0
        val fy = y - y0

        return blendPixel(
            p00 = image.getRGB(x0, y0),
            p10 = image.getRGB(x1, y0),
            p01 = image.getRGB(x0, y1),
            p11 = image.getRGB(x1, y1),
            fx = fx,
            fy = fy,
        )
    }

    private fun blendPixel(p00: Int, p10: Int, p01: Int, p11: Int, fx: Float, fy: Float): Int {
        val ifx = 1f - fx
        val ify = 1f - fy
        val w00 = ifx * ify
        val w10 = fx * ify
        val w01 = ifx * fy
        val w11 = fx * fy
        val r = (((p00 shr 16) and 0xFF) * w00 + ((p10 shr 16) and 0xFF) * w10 +
            ((p01 shr 16) and 0xFF) * w01 + ((p11 shr 16) and 0xFF) * w11).toInt()
        val g = (((p00 shr 8) and 0xFF) * w00 + ((p10 shr 8) and 0xFF) * w10 +
            ((p01 shr 8) and 0xFF) * w01 + ((p11 shr 8) and 0xFF) * w11).toInt()
        val b = ((p00 and 0xFF) * w00 + (p10 and 0xFF) * w10 +
            (p01 and 0xFF) * w01 + (p11 and 0xFF) * w11).toInt()
        return (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
    }

    private fun dist(a: ImagePoint, b: ImagePoint): Float =
        sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y))
}

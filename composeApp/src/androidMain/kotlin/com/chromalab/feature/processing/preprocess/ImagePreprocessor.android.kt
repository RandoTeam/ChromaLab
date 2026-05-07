package com.chromalab.feature.processing.preprocess

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Android preprocessor using pure Kotlin pixel operations.
 * Pipeline: grayscale → CLAHE → adaptive threshold → morphology → noise cleanup.
 * Each stage saves an intermediate image for debugging/reproducibility.
 */
actual class ImagePreprocessor actual constructor() {

    actual fun preprocess(
        imagePath: String,
        outputDir: String,
        params: PreprocessingParams,
    ): PreprocessingResult? {
        val bitmap = BitmapFactory.decodeFile(imagePath) ?: return null
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()

        val outDir = File(outputDir).also { it.mkdirs() }

        // 1. Grayscale
        val gray = IntArray(w * h) { i ->
            val p = pixels[i]
            (0.299 * ((p shr 16) and 0xFF) +
                0.587 * ((p shr 8) and 0xFF) +
                0.114 * (p and 0xFF)).toInt()
        }
        val grayscalePath = saveGray(gray, w, h, File(outDir, "grayscale.jpg"))

        // 2. CLAHE (Contrast Limited Adaptive Histogram Equalization)
        val enhanced = applyClahe(gray, w, h, params.claheClipLimit, params.claheTileSize)
        val contrastPath = saveGray(enhanced, w, h, File(outDir, "contrast_enhanced.jpg"))

        // 3. Adaptive threshold
        val binary = adaptiveThreshold(enhanced, w, h, params.adaptiveBlockSize, params.adaptiveC)
        val binaryPath = saveGray(binary, w, h, File(outDir, "binary.jpg"))

        // 4. Morphology (close: dilate then erode — fills small gaps)
        var morph = binary
        for (i in 0 until params.morphIterations) {
            morph = dilate(morph, w, h, params.morphKernelSize)
            morph = erode(morph, w, h, params.morphKernelSize)
        }

        // 5. Median filter for noise cleanup
        morph = medianFilter(morph, w, h, params.medianFilterSize)
        val morphPath = saveGray(morph, w, h, File(outDir, "morphology.jpg"))

        return PreprocessingResult(
            grayscalePath = grayscalePath,
            contrastEnhancedPath = contrastPath,
            binaryPath = binaryPath,
            morphologyPath = morphPath,
            sourcePath = imagePath,
            params = params,
            width = w,
            height = h,
            timestamp = System.currentTimeMillis(),
        )
    }

    /**
     * CLAHE — tile-based histogram equalization with clip limit.
     */
    private fun applyClahe(gray: IntArray, w: Int, h: Int, clipLimit: Float, tileSize: Int): IntArray {
        val result = IntArray(w * h)
        val tw = max(1, w / tileSize)
        val th = max(1, h / tileSize)

        for (ty in 0 until tileSize) {
            for (tx in 0 until tileSize) {
                val x0 = tx * tw; val y0 = ty * th
                val x1 = if (tx == tileSize - 1) w else (tx + 1) * tw
                val y1 = if (ty == tileSize - 1) h else (ty + 1) * th

                // Build histogram for tile
                val hist = IntArray(256)
                var count = 0
                for (y in y0 until y1) {
                    for (x in x0 until x1) {
                        hist[gray[y * w + x].coerceIn(0, 255)]++
                        count++
                    }
                }

                // Clip histogram
                val limit = max(1, (clipLimit * count / 256).toInt())
                var excess = 0
                for (i in 0..255) {
                    if (hist[i] > limit) {
                        excess += hist[i] - limit
                        hist[i] = limit
                    }
                }
                val increment = excess / 256
                for (i in 0..255) hist[i] += increment

                // Build CDF
                val cdf = IntArray(256)
                cdf[0] = hist[0]
                for (i in 1..255) cdf[i] = cdf[i - 1] + hist[i]
                val cdfMin = cdf.first { it > 0 }
                val denom = max(1, count - cdfMin)

                // Map pixels
                for (y in y0 until y1) {
                    for (x in x0 until x1) {
                        val v = gray[y * w + x].coerceIn(0, 255)
                        result[y * w + x] = ((cdf[v] - cdfMin) * 255 / denom).coerceIn(0, 255)
                    }
                }
            }
        }
        return result
    }

    /**
     * Adaptive threshold — compare each pixel to local mean.
     */
    private fun adaptiveThreshold(gray: IntArray, w: Int, h: Int, blockSize: Int, c: Int): IntArray {
        val half = blockSize / 2
        val result = IntArray(w * h)

        // Use integral image for fast mean computation
        val integral = LongArray(w * h)
        for (y in 0 until h) {
            var rowSum = 0L
            for (x in 0 until w) {
                rowSum += gray[y * w + x]
                integral[y * w + x] = rowSum + if (y > 0) integral[(y - 1) * w + x] else 0L
            }
        }

        for (y in 0 until h) {
            for (x in 0 until w) {
                val x0 = max(0, x - half); val y0 = max(0, y - half)
                val x1 = min(w - 1, x + half); val y1 = min(h - 1, y + half)
                val area = (x1 - x0 + 1) * (y1 - y0 + 1)

                var sum = integral[y1 * w + x1]
                if (x0 > 0) sum -= integral[y1 * w + (x0 - 1)]
                if (y0 > 0) sum -= integral[(y0 - 1) * w + x1]
                if (x0 > 0 && y0 > 0) sum += integral[(y0 - 1) * w + (x0 - 1)]

                val mean = (sum / area).toInt()
                result[y * w + x] = if (gray[y * w + x] > mean - c) 255 else 0
            }
        }
        return result
    }

    private fun dilate(src: IntArray, w: Int, h: Int, kernelSize: Int): IntArray {
        val half = kernelSize / 2
        val dst = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var maxVal = 0
                for (ky in -half..half) {
                    for (kx in -half..half) {
                        val nx = x + kx; val ny = y + ky
                        if (nx in 0 until w && ny in 0 until h) {
                            maxVal = max(maxVal, src[ny * w + nx])
                        }
                    }
                }
                dst[y * w + x] = maxVal
            }
        }
        return dst
    }

    private fun erode(src: IntArray, w: Int, h: Int, kernelSize: Int): IntArray {
        val half = kernelSize / 2
        val dst = IntArray(w * h)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var minVal = 255
                for (ky in -half..half) {
                    for (kx in -half..half) {
                        val nx = x + kx; val ny = y + ky
                        if (nx in 0 until w && ny in 0 until h) {
                            minVal = min(minVal, src[ny * w + nx])
                        }
                    }
                }
                dst[y * w + x] = minVal
            }
        }
        return dst
    }

    /**
     * Median filter for noise removal.
     */
    private fun medianFilter(src: IntArray, w: Int, h: Int, filterSize: Int): IntArray {
        val half = filterSize / 2
        val dst = IntArray(w * h)
        val window = mutableListOf<Int>()

        for (y in 0 until h) {
            for (x in 0 until w) {
                window.clear()
                for (ky in -half..half) {
                    for (kx in -half..half) {
                        val nx = x + kx; val ny = y + ky
                        if (nx in 0 until w && ny in 0 until h) {
                            window.add(src[ny * w + nx])
                        }
                    }
                }
                window.sort()
                dst[y * w + x] = window[window.size / 2]
            }
        }
        return dst
    }

    private fun saveGray(gray: IntArray, w: Int, h: Int, file: File): String {
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val argb = IntArray(w * h) { i ->
            val v = gray[i].coerceIn(0, 255)
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        bitmap.setPixels(argb, 0, w, 0, 0, w, h)
        FileOutputStream(file).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
        }
        bitmap.recycle()
        return file.absolutePath
    }
}

package com.chromalab.feature.processing.quality

import android.graphics.BitmapFactory
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Android image quality analyzer using Bitmap pixel analysis.
 * Implements: blur (Laplacian variance), brightness, contrast,
 * glare, shadow/uneven illumination, frame fill, perspective skew.
 */
actual class ImageQualityAnalyzer actual constructor() {

    actual fun analyze(imagePath: String, frameRatio: Float): ImageQualityReport {
        val options = BitmapFactory.Options().apply {
            // Downsample for speed — quality analysis doesn't need full res
            inSampleSize = 4
        }
        val bitmap = BitmapFactory.decodeFile(imagePath, options)
            ?: return fallbackReport("Не удалось загрузить изображение")

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()

        val gray = IntArray(w * h) { i ->
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            (0.299 * r + 0.587 * g + 0.114 * b).toInt()
        }

        val blur = analyzeBlur(gray, w, h)
        val brightness = analyzeBrightness(gray)
        val contrast = analyzeContrast(gray)
        val glare = analyzeGlare(gray)
        val shadow = analyzeShadow(gray, w, h)
        val frameFill = analyzeFrameFill(gray, w, h, frameRatio)
        val skew = analyzeSkew(gray, w, h)

        val metrics = listOf(blur, brightness, contrast, glare, shadow, frameFill, skew)
        val warnings = metrics.filter { it.level != QualityLevel.GOOD }.map { it.message }

        val poorCount = metrics.count { it.level == QualityLevel.POOR }
        val acceptableCount = metrics.count { it.level == QualityLevel.ACCEPTABLE }

        val overall = when {
            poorCount >= 2 -> QualityLevel.POOR
            poorCount >= 1 || acceptableCount >= 3 -> QualityLevel.ACCEPTABLE
            else -> QualityLevel.GOOD
        }

        return ImageQualityReport(
            blurScore = blur,
            brightnessScore = brightness,
            contrastScore = contrast,
            glareScore = glare,
            shadowScore = shadow,
            frameFillScore = frameFill,
            skewScore = skew,
            overallLevel = overall,
            warnings = warnings,
            timestamp = System.currentTimeMillis(),
        )
    }

    /**
     * Blur detection via variance of Laplacian approximation.
     * Low variance = blurry image.
     */
    private fun analyzeBlur(gray: IntArray, w: Int, h: Int): QualityMetric {
        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val laplacian = -4 * gray[y * w + x] +
                    gray[(y - 1) * w + x] +
                    gray[(y + 1) * w + x] +
                    gray[y * w + (x - 1)] +
                    gray[y * w + (x + 1)]
                sum += laplacian
                sumSq += laplacian.toDouble() * laplacian
                count++
            }
        }

        val mean = sum / count
        val variance = (sumSq / count - mean * mean).toFloat()

        val level = when {
            variance > 500f -> QualityLevel.GOOD
            variance > 100f -> QualityLevel.ACCEPTABLE
            else -> QualityLevel.POOR
        }

        return QualityMetric(
            name = "blur",
            score = variance,
            level = level,
            message = when (level) {
                QualityLevel.GOOD -> "Резкость хорошая"
                QualityLevel.ACCEPTABLE -> "Резкость допустимая"
                QualityLevel.POOR -> "Низкая резкость — изображение размыто"
            },
        )
    }

    /**
     * Brightness check via mean luminance.
     */
    private fun analyzeBrightness(gray: IntArray): QualityMetric {
        val mean = gray.average().toFloat()

        val level = when {
            mean in 60f..200f -> QualityLevel.GOOD
            mean in 30f..230f -> QualityLevel.ACCEPTABLE
            else -> QualityLevel.POOR
        }

        return QualityMetric(
            name = "brightness",
            score = mean,
            level = level,
            message = when {
                mean < 60f -> "Слишком темное изображение"
                mean > 200f -> "Слишком светлое изображение"
                else -> "Яркость в норме"
            },
        )
    }

    /**
     * Contrast check via standard deviation of luminance.
     */
    private fun analyzeContrast(gray: IntArray): QualityMetric {
        val mean = gray.average()
        val variance = gray.sumOf { (it - mean) * (it - mean) } / gray.size
        val stdDev = sqrt(variance).toFloat()

        val level = when {
            stdDev > 40f -> QualityLevel.GOOD
            stdDev > 20f -> QualityLevel.ACCEPTABLE
            else -> QualityLevel.POOR
        }

        return QualityMetric(
            name = "contrast",
            score = stdDev,
            level = level,
            message = when (level) {
                QualityLevel.GOOD -> "Контраст хороший"
                QualityLevel.ACCEPTABLE -> "Контраст допустимый"
                QualityLevel.POOR -> "Низкий контраст"
            },
        )
    }

    /**
     * Glare / overexposure: percentage of near-white pixels.
     */
    private fun analyzeGlare(gray: IntArray): QualityMetric {
        val overexposed = gray.count { it > 245 }
        val ratio = overexposed.toFloat() / gray.size

        val level = when {
            ratio < 0.02f -> QualityLevel.GOOD
            ratio < 0.08f -> QualityLevel.ACCEPTABLE
            else -> QualityLevel.POOR
        }

        return QualityMetric(
            name = "glare",
            score = ratio * 100f,
            level = level,
            message = when (level) {
                QualityLevel.GOOD -> "Бликов нет"
                QualityLevel.ACCEPTABLE -> "Небольшие блики"
                QualityLevel.POOR -> "Сильные блики / пересвет"
            },
        )
    }

    /**
     * Shadow / uneven illumination: compare brightness of quadrants.
     */
    private fun analyzeShadow(gray: IntArray, w: Int, h: Int): QualityMetric {
        val halfW = w / 2
        val halfH = h / 2

        val quadrantMeans = listOf(
            quadrantMean(gray, w, 0, 0, halfW, halfH),
            quadrantMean(gray, w, halfW, 0, w, halfH),
            quadrantMean(gray, w, 0, halfH, halfW, h),
            quadrantMean(gray, w, halfW, halfH, w, h),
        )

        val maxDiff = quadrantMeans.max() - quadrantMeans.min()

        val level = when {
            maxDiff < 30f -> QualityLevel.GOOD
            maxDiff < 60f -> QualityLevel.ACCEPTABLE
            else -> QualityLevel.POOR
        }

        return QualityMetric(
            name = "shadow",
            score = maxDiff,
            level = level,
            message = when (level) {
                QualityLevel.GOOD -> "Освещение равномерное"
                QualityLevel.ACCEPTABLE -> "Небольшая неравномерность освещения"
                QualityLevel.POOR -> "Неравномерное освещение / тени"
            },
        )
    }

    /**
     * Frame fill: how much of the image is "content" vs empty/white border.
     */
    private fun analyzeFrameFill(gray: IntArray, w: Int, h: Int, frameRatio: Float): QualityMetric {
        // Count non-white pixels in the frame region
        val frameX = ((1f - frameRatio) / 2 * w).toInt()
        val frameY = ((1f - frameRatio) / 2 * h).toInt()
        val frameW = (w * frameRatio).toInt()
        val frameH = (h * frameRatio).toInt()

        var contentPixels = 0
        var totalPixels = 0

        for (y in frameY until frameY + frameH) {
            for (x in frameX until frameX + frameW) {
                totalPixels++
                if (gray[y * w + x] < 230) contentPixels++
            }
        }

        val ratio = contentPixels.toFloat() / totalPixels

        val level = when {
            ratio > 0.3f -> QualityLevel.GOOD
            ratio > 0.15f -> QualityLevel.ACCEPTABLE
            else -> QualityLevel.POOR
        }

        return QualityMetric(
            name = "frameFill",
            score = ratio * 100f,
            level = level,
            message = when (level) {
                QualityLevel.GOOD -> "Рамка заполнена"
                QualityLevel.ACCEPTABLE -> "Рамка частично заполнена"
                QualityLevel.POOR -> "Рамка пуста — поместите лист в рамку"
            },
        )
    }

    /**
     * Perspective skew: compare left/right edge brightness gradients.
     * High asymmetry suggests tilted document.
     */
    private fun analyzeSkew(gray: IntArray, w: Int, h: Int): QualityMetric {
        // Compare vertical gradient of left vs right edge
        val sampleW = w / 10
        var leftGradient = 0.0
        var rightGradient = 0.0

        for (y in 1 until h) {
            for (x in 0 until sampleW) {
                leftGradient += abs(gray[y * w + x] - gray[(y - 1) * w + x])
            }
            for (x in w - sampleW until w) {
                rightGradient += abs(gray[y * w + x] - gray[(y - 1) * w + x])
            }
        }

        val asymmetry = abs(leftGradient - rightGradient) / (leftGradient + rightGradient + 1.0)
        val score = (asymmetry * 100).toFloat()

        val level = when {
            score < 10f -> QualityLevel.GOOD
            score < 25f -> QualityLevel.ACCEPTABLE
            else -> QualityLevel.POOR
        }

        return QualityMetric(
            name = "skew",
            score = score,
            level = level,
            message = when (level) {
                QualityLevel.GOOD -> "Перекос минимальный"
                QualityLevel.ACCEPTABLE -> "Небольшой перекос"
                QualityLevel.POOR -> "Сильный перекос — выровняйте камеру"
            },
        )
    }

    private fun quadrantMean(gray: IntArray, w: Int, x1: Int, y1: Int, x2: Int, y2: Int): Float {
        var sum = 0L
        var count = 0
        for (y in y1 until y2) {
            for (x in x1 until x2) {
                sum += gray[y * w + x]
                count++
            }
        }
        return if (count > 0) sum.toFloat() / count else 0f
    }

    private fun fallbackReport(message: String): ImageQualityReport {
        val poor = QualityMetric("error", 0f, QualityLevel.POOR, message)
        return ImageQualityReport(
            blurScore = poor, brightnessScore = poor, contrastScore = poor,
            glareScore = poor, shadowScore = poor, frameFillScore = poor,
            skewScore = poor, overallLevel = QualityLevel.POOR,
            warnings = listOf(message), timestamp = System.currentTimeMillis(),
        )
    }
}

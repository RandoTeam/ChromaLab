package com.chromalab.feature.processing.preprocess

import kotlin.math.max
import kotlin.math.min

internal data class PreprocessingStages(
    val grayscale: IntArray,
    val contrastEnhanced: IntArray,
    val sharpened: IntArray,
    val scanStyle: IntArray,
    val binary: IntArray,
    val morphology: IntArray,
)

internal fun buildPreprocessingStages(
    argbPixels: IntArray,
    width: Int,
    height: Int,
    params: PreprocessingParams,
): PreprocessingStages {
    val grayscale = IntArray(width * height) { index ->
        argbPixels[index].toPreprocessingGray()
    }
    val contrastEnhanced = applyClahe(
        grayscale = grayscale,
        width = width,
        height = height,
        clipLimit = params.claheClipLimit,
        tileSize = params.claheTileSize,
    )
    val sharpened = sharpen(contrastEnhanced, width, height)
    val scanStyle = medianFilter(
        adaptiveThreshold(
            grayscale = sharpened,
            width = width,
            height = height,
            blockSize = params.adaptiveBlockSize.coerceAtLeast(31),
            c = params.adaptiveC + 4,
        ),
        width,
        height,
        params.medianFilterSize,
    )
    val binary = adaptiveThreshold(
        grayscale = contrastEnhanced,
        width = width,
        height = height,
        blockSize = params.adaptiveBlockSize,
        c = params.adaptiveC,
    )
    var morphology = binary
    repeat(params.morphIterations) {
        morphology = dilate(morphology, width, height, params.morphKernelSize)
        morphology = erode(morphology, width, height, params.morphKernelSize)
    }
    morphology = medianFilter(morphology, width, height, params.medianFilterSize)

    return PreprocessingStages(
        grayscale = grayscale,
        contrastEnhanced = contrastEnhanced,
        sharpened = sharpened,
        scanStyle = scanStyle,
        binary = binary,
        morphology = morphology,
    )
}

private fun applyClahe(
    grayscale: IntArray,
    width: Int,
    height: Int,
    clipLimit: Float,
    tileSize: Int,
): IntArray {
    val result = IntArray(width * height)
    val tiles = tileSize.coerceAtLeast(1)
    val tileWidth = max(1, width / tiles)
    val tileHeight = max(1, height / tiles)

    for (tileY in 0 until tiles) {
        for (tileX in 0 until tiles) {
            val x0 = tileX * tileWidth
            val y0 = tileY * tileHeight
            val x1 = if (tileX == tiles - 1) width else (tileX + 1) * tileWidth
            val y1 = if (tileY == tiles - 1) height else (tileY + 1) * tileHeight

            val histogram = IntArray(256)
            var count = 0
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    histogram[grayscale[y * width + x].coerceIn(0, 255)]++
                    count++
                }
            }

            val limit = max(1, (clipLimit * count / 256).toInt())
            var excess = 0
            for (index in histogram.indices) {
                if (histogram[index] > limit) {
                    excess += histogram[index] - limit
                    histogram[index] = limit
                }
            }
            val increment = excess / 256
            for (index in histogram.indices) {
                histogram[index] += increment
            }

            val cumulative = IntArray(256)
            cumulative[0] = histogram[0]
            for (index in 1..255) {
                cumulative[index] = cumulative[index - 1] + histogram[index]
            }
            val cumulativeMin = cumulative.firstOrNull { it > 0 } ?: 0
            val denominator = max(1, count - cumulativeMin)

            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    val value = grayscale[y * width + x].coerceIn(0, 255)
                    result[y * width + x] = ((cumulative[value] - cumulativeMin) * 255 / denominator).coerceIn(0, 255)
                }
            }
        }
    }
    return result
}

private fun adaptiveThreshold(
    grayscale: IntArray,
    width: Int,
    height: Int,
    blockSize: Int,
    c: Int,
): IntArray {
    val half = blockSize / 2
    val result = IntArray(width * height)
    val integral = LongArray(width * height)

    for (y in 0 until height) {
        var rowSum = 0L
        for (x in 0 until width) {
            rowSum += grayscale[y * width + x]
            integral[y * width + x] = rowSum + if (y > 0) integral[(y - 1) * width + x] else 0L
        }
    }

    for (y in 0 until height) {
        for (x in 0 until width) {
            val x0 = max(0, x - half)
            val y0 = max(0, y - half)
            val x1 = min(width - 1, x + half)
            val y1 = min(height - 1, y + half)
            val area = (x1 - x0 + 1) * (y1 - y0 + 1)

            var sum = integral[y1 * width + x1]
            if (x0 > 0) sum -= integral[y1 * width + (x0 - 1)]
            if (y0 > 0) sum -= integral[(y0 - 1) * width + x1]
            if (x0 > 0 && y0 > 0) sum += integral[(y0 - 1) * width + (x0 - 1)]

            val mean = (sum / area).toInt()
            result[y * width + x] = if (grayscale[y * width + x] > mean - c) 255 else 0
        }
    }
    return result
}

private fun dilate(source: IntArray, width: Int, height: Int, kernelSize: Int): IntArray {
    val half = kernelSize / 2
    val result = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            var maxValue = 0
            for (kernelY in -half..half) {
                for (kernelX in -half..half) {
                    val nx = x + kernelX
                    val ny = y + kernelY
                    if (nx in 0 until width && ny in 0 until height) {
                        maxValue = max(maxValue, source[ny * width + nx])
                    }
                }
            }
            result[y * width + x] = maxValue
        }
    }
    return result
}

private fun erode(source: IntArray, width: Int, height: Int, kernelSize: Int): IntArray {
    val half = kernelSize / 2
    val result = IntArray(width * height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            var minValue = 255
            for (kernelY in -half..half) {
                for (kernelX in -half..half) {
                    val nx = x + kernelX
                    val ny = y + kernelY
                    if (nx in 0 until width && ny in 0 until height) {
                        minValue = min(minValue, source[ny * width + nx])
                    }
                }
            }
            result[y * width + x] = minValue
        }
    }
    return result
}

private fun medianFilter(source: IntArray, width: Int, height: Int, filterSize: Int): IntArray {
    val half = filterSize / 2
    val result = IntArray(width * height)
    val window = mutableListOf<Int>()

    for (y in 0 until height) {
        for (x in 0 until width) {
            window.clear()
            for (kernelY in -half..half) {
                for (kernelX in -half..half) {
                    val nx = x + kernelX
                    val ny = y + kernelY
                    if (nx in 0 until width && ny in 0 until height) {
                        window.add(source[ny * width + nx])
                    }
                }
            }
            window.sort()
            result[y * width + x] = window[window.size / 2]
        }
    }
    return result
}

private fun sharpen(source: IntArray, width: Int, height: Int): IntArray {
    val result = source.copyOf()
    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val center = source[y * width + x] * 5
            val value = center -
                source[(y - 1) * width + x] -
                source[(y + 1) * width + x] -
                source[y * width + (x - 1)] -
                source[y * width + (x + 1)]
            result[y * width + x] = value.coerceIn(0, 255)
        }
    }
    return result
}

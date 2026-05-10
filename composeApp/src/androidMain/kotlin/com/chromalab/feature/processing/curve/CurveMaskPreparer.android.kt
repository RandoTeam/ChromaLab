package com.chromalab.feature.processing.curve

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.graph.GraphRegion
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Android curve mask preparation.
 *
 * Pipeline:
 * 1. Crop image to graph ROI
 * 2. Grayscale → adaptive threshold → raw binary mask
 * 3. Suppress axes (erase thin horizontal/vertical lines at axis positions)
 * 4. Suppress grid (detect and erase periodic thin lines)
 * 5. Suppress text-like blobs (small connected components)
 * 6. Save both raw and cleaned masks
 */
actual class CurveMaskPreparer actual constructor() {

    actual fun prepare(
        imagePath: String,
        graphRegion: GraphRegion,
        axes: AxesResult,
        outputDir: String,
    ): CurveMaskResult {
        val bitmap = BitmapFactory.decodeFile(imagePath)
            ?: return emptyResult(graphRegion)

        // Crop to graph region
        val rx = graphRegion.x.coerceIn(0, bitmap.width - 1)
        val ry = graphRegion.y.coerceIn(0, bitmap.height - 1)
        val rw = graphRegion.width.coerceIn(1, bitmap.width - rx)
        val rh = graphRegion.height.coerceIn(1, bitmap.height - ry)

        val cropped = Bitmap.createBitmap(bitmap, rx, ry, rw, rh)
        bitmap.recycle()

        val w = cropped.width
        val h = cropped.height
        val pixels = IntArray(w * h)
        cropped.getPixels(pixels, 0, w, 0, 0, w, h)
        cropped.recycle()

        // Grayscale
        val gray = IntArray(w * h) { i ->
            val p = pixels[i]
            (0.299 * ((p shr 16) and 0xFF) +
                0.587 * ((p shr 8) and 0xFF) +
                0.114 * (p and 0xFF)).toInt()
        }
        // --- Dual-strategy mask ---
        // Strategy 1: Canny edge detection (best for photos — thin, precise edges)
        val cannyMask = cannyEdges(gray, w, h, lowThreshold = 20, highThreshold = 50)
        // Dilate Canny edges by 1px to form connected curve
        dilate(cannyMask, w, h)

        // Strategy 2: Adaptive threshold (good for high-contrast scans)
        val adaptiveMask = adaptiveThreshold(gray, w, h, blockSize = 31, c = 10)

        // Combine: use Canny as primary (less noise), add adaptive where Canny found edges
        // This catches the curve strongly while ignoring faint background noise
        val rawMask = BooleanArray(w * h) { i ->
            cannyMask[i] || (adaptiveMask[i] && hasNeighbor(cannyMask, w, h, i % w, i / w, radius = 3))
        }
        val rawCount = rawMask.count { it }

        // Save raw mask
        val dir = File(outputDir).also { it.mkdirs() }
        val rawPath = File(dir, "mask_raw.png").absolutePath
        saveMask(rawMask, w, h, rawPath)

        // --- Suppression passes ---
        val cleanMask = rawMask.copyOf()
        val suppressions = mutableListOf<String>()

        // Pass 1: Suppress axes (wider margin to cover tick marks)
        if (axes.xAxis != null || axes.yAxis != null) {
            suppressAxes(cleanMask, w, h, axes, graphRegion)
            suppressions.add("axes")
        }

        // Pass 2: Suppress grid lines (lowered threshold for partial grid)
        val gridSuppressed = suppressGrid(cleanMask, w, h)
        if (gridSuppressed) suppressions.add("grid")

        // Pass 3: Morphological opening (erode→dilate) to break thin connections
        morphologicalOpen(cleanMask, w, h, radius = 1)
        suppressions.add("morph_open")

        // Pass 4: Suppress text blobs (larger max — text chars are big)
        val textSuppressed = suppressSmallBlobs(cleanMask, w, h, minSize = 3, maxSize = 2000)
        if (textSuppressed) suppressions.add("text_blobs")

        // Pass 5: Keep only the largest connected component (the signal curve)
        keepLargestComponent(cleanMask, w, h)
        suppressions.add("largest_component")

        val cleanCount = cleanMask.count { it }
        val cleanPath = File(dir, "mask_clean.png").absolutePath
        saveMask(cleanMask, w, h, cleanPath)

        return CurveMaskResult(
            rawMaskPath = rawPath,
            cleanMaskPath = cleanPath,
            graphRegion = graphRegion,
            maskWidth = w,
            maskHeight = h,
            rawPixelCount = rawCount,
            cleanPixelCount = cleanCount,
            suppressionApplied = suppressions,
            timestamp = System.currentTimeMillis(),
        )
    }

    /**
     * Adaptive threshold using integral image.
     * Returns boolean mask: true = dark (ink/curve), false = background.
     */
    private fun adaptiveThreshold(
        gray: IntArray, w: Int, h: Int,
        blockSize: Int, c: Int,
    ): BooleanArray {
        // Build integral image
        val integral = LongArray(w * h)
        for (y in 0 until h) {
            var rowSum = 0L
            for (x in 0 until w) {
                rowSum += gray[y * w + x]
                integral[y * w + x] = rowSum +
                    if (y > 0) integral[(y - 1) * w + x] else 0L
            }
        }

        val mask = BooleanArray(w * h)
        val half = blockSize / 2

        for (y in 0 until h) {
            for (x in 0 until w) {
                val y1 = max(0, y - half)
                val y2 = min(h - 1, y + half)
                val x1 = max(0, x - half)
                val x2 = min(w - 1, x + half)

                val area = (y2 - y1 + 1) * (x2 - x1 + 1)
                val sum = integralSum(integral, w, x1, y1, x2, y2)
                val mean = sum / area

                // Dark pixel = below local mean - C
                mask[y * w + x] = gray[y * w + x] < (mean - c)
            }
        }
        return mask
    }

    private fun integralSum(integral: LongArray, w: Int, x1: Int, y1: Int, x2: Int, y2: Int): Long {
        val a = integral[y2 * w + x2]
        val b = if (x1 > 0) integral[y2 * w + x1 - 1] else 0L
        val c = if (y1 > 0) integral[(y1 - 1) * w + x2] else 0L
        val d = if (x1 > 0 && y1 > 0) integral[(y1 - 1) * w + x1 - 1] else 0L
        return a - b - c + d
    }

    /**
     * Suppress axis lines by erasing pixels along detected axis positions.
     */
    private fun suppressAxes(
        mask: BooleanArray, w: Int, h: Int,
        axes: AxesResult, graphRegion: GraphRegion,
    ) {
        val thickness = 8 // Erase ±8 pixels around axis position (covers tick marks too)

        // X axis (horizontal line)
        axes.xAxis?.let { xAxis ->
            val yPos = ((xAxis.y1 - graphRegion.y)).roundToInt().coerceIn(0, h - 1)
            for (dy in -thickness..thickness) {
                val y = (yPos + dy).coerceIn(0, h - 1)
                for (x in 0 until w) {
                    mask[y * w + x] = false
                }
            }
        }

        // Y axis (vertical line)
        axes.yAxis?.let { yAxis ->
            val xPos = ((yAxis.x1 - graphRegion.x)).roundToInt().coerceIn(0, w - 1)
            for (dx in -thickness..thickness) {
                val x = (xPos + dx).coerceIn(0, w - 1)
                for (y in 0 until h) {
                    mask[y * w + x] = false
                }
            }
        }
    }

    /**
     * Suppress grid lines: detect rows/columns where most pixels are set
     * (indicating a horizontal or vertical line) and erase them.
     */
    private fun suppressGrid(mask: BooleanArray, w: Int, h: Int): Boolean {
        var suppressed = false
        val lineThreshold = 0.25f // Row/col with >25% dark pixels is likely a grid line

        // Horizontal grid lines
        for (y in 0 until h) {
            val count = (0 until w).count { x -> mask[y * w + x] }
            if (count > w * lineThreshold) {
                for (x in 0 until w) mask[y * w + x] = false
                suppressed = true
            }
        }

        // Vertical grid lines
        for (x in 0 until w) {
            val count = (0 until h).count { y -> mask[y * w + x] }
            if (count > h * lineThreshold) {
                for (y in 0 until h) mask[y * w + x] = false
                suppressed = true
            }
        }

        return suppressed
    }

    /**
     * Suppress small connected components (likely text characters or noise).
     * Components between [minSize, maxSize] pixels are erased.
     */
    private fun suppressSmallBlobs(
        mask: BooleanArray, w: Int, h: Int,
        minSize: Int, maxSize: Int,
    ): Boolean {
        val labels = IntArray(w * h)
        var nextLabel = 1
        var suppressed = false

        // Simple flood-fill labeling
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (mask[y * w + x] && labels[y * w + x] == 0) {
                    val componentSize = floodFill(mask, labels, w, h, x, y, nextLabel)
                    if (componentSize in minSize..maxSize) {
                        // Erase this small blob
                        eraseLabel(mask, labels, w, h, nextLabel)
                        suppressed = true
                    }
                    nextLabel++
                }
            }
        }

        return suppressed
    }

    private fun floodFill(
        mask: BooleanArray, labels: IntArray,
        w: Int, h: Int, startX: Int, startY: Int, label: Int,
    ): Int {
        val stack = ArrayDeque<Int>()
        stack.addLast(startY * w + startX)
        labels[startY * w + startX] = label
        var count = 0

        while (stack.isNotEmpty()) {
            val idx = stack.removeLast()
            count++
            val x = idx % w
            val y = idx / w

            for ((dx, dy) in NEIGHBORS_4) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until w && ny in 0 until h) {
                    val nIdx = ny * w + nx
                    if (mask[nIdx] && labels[nIdx] == 0) {
                        labels[nIdx] = label
                        stack.addLast(nIdx)
                    }
                }
            }

            // Cap to prevent runaway on huge components
            if (count > 5000) break
        }
        return count
    }

    private fun eraseLabel(
        mask: BooleanArray, labels: IntArray,
        w: Int, h: Int, label: Int,
    ) {
        for (i in mask.indices) {
            if (labels[i] == label) mask[i] = false
        }
    }

    /**
     * Save a boolean mask as a grayscale PNG.
     */
    private fun saveMask(mask: BooleanArray, w: Int, h: Int, path: String) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val px = IntArray(w * h) { i ->
            if (mask[i]) 0xFF_FF_FF_FF.toInt() else 0xFF_00_00_00.toInt()
        }
        bmp.setPixels(px, 0, w, 0, 0, w, h)
        FileOutputStream(path).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        bmp.recycle()
    }

    private fun emptyResult(graphRegion: GraphRegion): CurveMaskResult = CurveMaskResult(
        rawMaskPath = null,
        cleanMaskPath = null,
        graphRegion = graphRegion,
        maskWidth = 0,
        maskHeight = 0,
        rawPixelCount = 0,
        cleanPixelCount = 0,
        suppressionApplied = emptyList(),
        timestamp = System.currentTimeMillis(),
    )

    companion object {
        private val NEIGHBORS_4 = listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)
    }

    /**
     * Canny edge detection with hysteresis thresholding.
     *
     * Steps:
     * 1. Gaussian blur (3×3) to suppress noise
     * 2. Sobel gradient magnitude
     * 3. Non-maximum suppression (thin edges to 1px)
     * 4. Hysteresis thresholding (strong edges + connected weak edges)
     */
    private fun cannyEdges(
        gray: IntArray, w: Int, h: Int,
        lowThreshold: Int, highThreshold: Int,
    ): BooleanArray {
        // 1. Gaussian blur 3×3
        val blurred = IntArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                blurred[y * w + x] = (
                    gray[(y-1)*w+x-1] + 2*gray[(y-1)*w+x] + gray[(y-1)*w+x+1] +
                    2*gray[y*w+x-1] + 4*gray[y*w+x] + 2*gray[y*w+x+1] +
                    gray[(y+1)*w+x-1] + 2*gray[(y+1)*w+x] + gray[(y+1)*w+x+1]
                ) / 16
            }
        }

        // 2. Sobel magnitude + direction
        val magnitude = IntArray(w * h)
        val direction = IntArray(w * h) // 0=horizontal, 1=45°, 2=vertical, 3=135°
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val gx = -blurred[(y-1)*w+x-1] + blurred[(y-1)*w+x+1] +
                         -2*blurred[y*w+x-1] + 2*blurred[y*w+x+1] +
                         -blurred[(y+1)*w+x-1] + blurred[(y+1)*w+x+1]
                val gy = -blurred[(y-1)*w+x-1] - 2*blurred[(y-1)*w+x] - blurred[(y-1)*w+x+1] +
                         blurred[(y+1)*w+x-1] + 2*blurred[(y+1)*w+x] + blurred[(y+1)*w+x+1]
                magnitude[y * w + x] = abs(gx) + abs(gy) // L1 norm (fast)

                // Quantize gradient direction to 4 bins
                val absGx = abs(gx)
                val absGy = abs(gy)
                direction[y * w + x] = when {
                    absGy < absGx / 3 -> 0    // ~horizontal edge
                    absGx < absGy / 3 -> 2    // ~vertical edge
                    (gx > 0 && gy > 0) || (gx < 0 && gy < 0) -> 1 // 45°
                    else -> 3 // 135°
                }
            }
        }

        // 3. Non-maximum suppression
        val nms = IntArray(w * h)
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val mag = magnitude[y * w + x]
                if (mag == 0) continue

                val (n1, n2) = when (direction[y * w + x]) {
                    0 -> magnitude[y*w+x-1] to magnitude[y*w+x+1]
                    1 -> magnitude[(y-1)*w+x+1] to magnitude[(y+1)*w+x-1]
                    2 -> magnitude[(y-1)*w+x] to magnitude[(y+1)*w+x]
                    else -> magnitude[(y-1)*w+x-1] to magnitude[(y+1)*w+x+1]
                }
                nms[y * w + x] = if (mag >= n1 && mag >= n2) mag else 0
            }
        }

        // 4. Hysteresis thresholding
        val result = BooleanArray(w * h)
        val strong = BooleanArray(w * h)

        // Mark strong edges
        for (i in nms.indices) {
            if (nms[i] >= highThreshold) {
                strong[i] = true
                result[i] = true
            }
        }

        // Connect weak edges adjacent to strong edges (BFS)
        val queue = ArrayDeque<Int>()
        for (i in strong.indices) {
            if (strong[i]) queue.addLast(i)
        }

        while (queue.isNotEmpty()) {
            val idx = queue.removeFirst()
            val cx = idx % w
            val cy = idx / w
            for (dy in -1..1) {
                for (dx in -1..1) {
                    if (dx == 0 && dy == 0) continue
                    val nx = cx + dx
                    val ny = cy + dy
                    if (nx !in 0 until w || ny !in 0 until h) continue
                    val ni = ny * w + nx
                    if (!result[ni] && nms[ni] >= lowThreshold) {
                        result[ni] = true
                        queue.addLast(ni)
                    }
                }
            }
        }

        return result
    }

    /**
     * Dilate boolean mask by 1 pixel (4-connected).
     */
    private fun dilate(mask: BooleanArray, w: Int, h: Int) {
        val copy = mask.copyOf()
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                if (copy[y * w + x]) {
                    mask[(y-1)*w+x] = true
                    mask[(y+1)*w+x] = true
                    mask[y*w+x-1] = true
                    mask[y*w+x+1] = true
                }
            }
        }
    }

    /**
     * Check if any pixel in the reference mask is set within radius of (x,y).
     */
    private fun hasNeighbor(ref: BooleanArray, w: Int, h: Int, x: Int, y: Int, radius: Int): Boolean {
        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until w && ny in 0 until h && ref[ny * w + nx]) return true
            }
        }
        return false
    }

    /**
     * Morphological opening (erode then dilate) to remove thin noise
     * and break connections between the curve and text/axes.
     */
    private fun morphologicalOpen(mask: BooleanArray, w: Int, h: Int, radius: Int) {
        // Erode
        val eroded = BooleanArray(w * h)
        for (y in radius until h - radius) {
            for (x in radius until w - radius) {
                var allSet = true
                outer@ for (dy in -radius..radius) {
                    for (dx in -radius..radius) {
                        if (!mask[(y + dy) * w + (x + dx)]) {
                            allSet = false
                            break@outer
                        }
                    }
                }
                eroded[y * w + x] = allSet
            }
        }
        // Dilate eroded back into mask
        mask.fill(false)
        for (y in radius until h - radius) {
            for (x in radius until w - radius) {
                if (eroded[y * w + x]) {
                    for (dy in -radius..radius) {
                        for (dx in -radius..radius) {
                            mask[(y + dy) * w + (x + dx)] = true
                        }
                    }
                }
            }
        }
    }

    /**
     * Keep only the largest connected component in the mask.
     * The chromatogram curve is typically the largest remaining blob.
     */
    private fun keepLargestComponent(mask: BooleanArray, w: Int, h: Int) {
        val labels = IntArray(w * h)
        var nextLabel = 1
        val sizes = mutableMapOf<Int, Int>()

        for (y in 0 until h) {
            for (x in 0 until w) {
                if (mask[y * w + x] && labels[y * w + x] == 0) {
                    val size = floodFill(mask, labels, w, h, x, y, nextLabel)
                    sizes[nextLabel] = size
                    nextLabel++
                }
            }
        }

        if (sizes.isEmpty()) return
        val largestLabel = sizes.maxByOrNull { it.value }?.key ?: return

        // Erase everything except the largest
        for (i in mask.indices) {
            if (labels[i] != largestLabel) mask[i] = false
        }
    }
}

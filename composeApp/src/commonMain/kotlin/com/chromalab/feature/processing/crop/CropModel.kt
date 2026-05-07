package com.chromalab.feature.processing.crop

import kotlinx.serialization.Serializable

/**
 * Rectangle in normalized image coordinates (pixels).
 */
@Serializable
data class CropRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

/**
 * Result of cropping an image by frame coordinates.
 */
@Serializable
data class CropResult(
    val croppedPath: String,
    val sourcePath: String,
    val sourceWidth: Int,
    val sourceHeight: Int,
    val cropRect: CropRect,
    val croppedWidth: Int,
    val croppedHeight: Int,
    val timestamp: Long,
)

/**
 * Transforms preview (screen) coordinates to image coordinates.
 *
 * The preview may have different aspect ratio / resolution than the actual image.
 * This class handles the mapping correctly for both portrait and landscape.
 */
class CoordinateTransform(
    private val previewWidth: Float,
    private val previewHeight: Float,
    private val imageWidth: Int,
    private val imageHeight: Int,
) {
    private val scaleX: Float
    private val scaleY: Float
    private val offsetX: Float
    private val offsetY: Float

    init {
        // Preview uses FILL_CENTER scaling — compute the effective mapping
        val previewAspect = previewWidth / previewHeight
        val imageAspect = imageWidth.toFloat() / imageHeight

        if (imageAspect > previewAspect) {
            // Image is wider — height fills, width is cropped
            scaleY = imageHeight.toFloat() / previewHeight
            scaleX = scaleY
            offsetX = (imageWidth - previewWidth * scaleX) / 2f
            offsetY = 0f
        } else {
            // Image is taller — width fills, height is cropped
            scaleX = imageWidth.toFloat() / previewWidth
            scaleY = scaleX
            offsetX = 0f
            offsetY = (imageHeight - previewHeight * scaleY) / 2f
        }
    }

    /**
     * Convert preview coordinates to image coordinates.
     */
    fun toImageCoords(previewX: Float, previewY: Float): Pair<Int, Int> {
        val imgX = (previewX * scaleX + offsetX).toInt().coerceIn(0, imageWidth - 1)
        val imgY = (previewY * scaleY + offsetY).toInt().coerceIn(0, imageHeight - 1)
        return imgX to imgY
    }

    /**
     * Convert a preview rect (frame overlay) to image CropRect.
     */
    fun toCropRect(
        previewFrameX: Float,
        previewFrameY: Float,
        previewFrameW: Float,
        previewFrameH: Float,
    ): CropRect {
        val (x1, y1) = toImageCoords(previewFrameX, previewFrameY)
        val (x2, y2) = toImageCoords(
            previewFrameX + previewFrameW,
            previewFrameY + previewFrameH,
        )
        return CropRect(
            x = x1,
            y = y1,
            width = (x2 - x1).coerceAtLeast(1),
            height = (y2 - y1).coerceAtLeast(1),
        )
    }
}

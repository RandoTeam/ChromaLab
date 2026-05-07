package com.chromalab.feature.capture

import kotlinx.serialization.Serializable

/**
 * Flash mode for camera capture.
 */
enum class FlashMode {
    AUTO, ON, OFF
}

/**
 * Metadata captured alongside the image.
 */
@Serializable
data class CaptureMetadata(
    val flashMode: String,
    val zoomRatio: Float,
    val imageWidth: Int,
    val imageHeight: Int,
    val timestamp: Long,
)

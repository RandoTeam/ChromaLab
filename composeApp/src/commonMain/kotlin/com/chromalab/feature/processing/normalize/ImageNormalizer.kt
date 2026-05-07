package com.chromalab.feature.processing.normalize

import kotlinx.serialization.Serializable

/**
 * Result of image normalization.
 */
@Serializable
data class NormalizedImageResult(
    val normalizedPath: String,
    val originalPath: String,
    val width: Int,
    val height: Int,
    val wasRotated: Boolean,
    val exifOrientation: Int,
    val timestamp: Long,
)

/**
 * Platform-specific image normalizer.
 * Applies EXIF orientation correction and saves a normalized copy.
 * All downstream coordinates must reference the normalized image.
 */
expect class ImageNormalizer() {
    /**
     * Normalize orientation and save to outputDir.
     * Returns null if the image cannot be loaded.
     */
    fun normalize(imagePath: String, outputDir: String): NormalizedImageResult?
}

package com.chromalab.feature.processing.document

import com.chromalab.feature.processing.pipeline.DetectionMethod
import kotlinx.serialization.Serializable

/**
 * A point in image coordinates.
 */
@Serializable
data class ImagePoint(val x: Float, val y: Float)

/**
 * Four corners of a detected document, in order:
 * topLeft, topRight, bottomRight, bottomLeft.
 */
@Serializable
data class DocumentCorners(
    val topLeft: ImagePoint,
    val topRight: ImagePoint,
    val bottomRight: ImagePoint,
    val bottomLeft: ImagePoint,
)

/**
 * Result of document bounds detection.
 */
@Serializable
data class DocumentBounds(
    val corners: DocumentCorners,
    val correctedCorners: DocumentCorners? = null,
    val areaRatio: Float,
    val aspectRatio: Float,
    val isQuadrilateral: Boolean,
    val detectionMethod: DetectionMethod,
    val confidence: Float,
    val timestamp: Long,
) {
    val effectiveCorners: DocumentCorners
        get() = correctedCorners ?: corners

    fun withCorrectedCorners(corners: DocumentCorners): DocumentBounds =
        copy(
            correctedCorners = corners,
            detectionMethod = when (detectionMethod) {
                DetectionMethod.AUTO -> DetectionMethod.AUTO_CORRECTED
                else -> detectionMethod
            },
        )
}

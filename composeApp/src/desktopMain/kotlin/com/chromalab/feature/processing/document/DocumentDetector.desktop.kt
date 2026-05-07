package com.chromalab.feature.processing.document

import com.chromalab.feature.processing.pipeline.DetectionMethod

actual class DocumentDetector actual constructor() {
    actual fun detect(imagePath: String): DocumentBounds? {
        // Desktop stub — returns full image as document
        return DocumentBounds(
            corners = DocumentCorners(
                topLeft = ImagePoint(0f, 0f),
                topRight = ImagePoint(1f, 0f),
                bottomRight = ImagePoint(1f, 1f),
                bottomLeft = ImagePoint(0f, 1f),
            ),
            areaRatio = 1f,
            aspectRatio = 1f,
            isQuadrilateral = true,
            detectionMethod = DetectionMethod.AUTO,
            confidence = 0f,
            timestamp = System.currentTimeMillis(),
        )
    }
}

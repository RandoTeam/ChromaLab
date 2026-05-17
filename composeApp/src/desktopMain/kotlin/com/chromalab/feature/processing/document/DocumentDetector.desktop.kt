package com.chromalab.feature.processing.document

import com.chromalab.feature.processing.pipeline.DetectionMethod
import java.io.File
import javax.imageio.ImageIO

actual class DocumentDetector actual constructor() {
    actual fun detect(imagePath: String): DocumentBounds? {
        val image = ImageIO.read(File(imagePath)) ?: return null
        val width = image.width.toFloat()
        val height = image.height.toFloat()
        image.flush()

        return DocumentBounds(
            corners = DocumentCorners(
                topLeft = ImagePoint(0f, 0f),
                topRight = ImagePoint(width, 0f),
                bottomRight = ImagePoint(width, height),
                bottomLeft = ImagePoint(0f, height),
            ),
            areaRatio = 1f,
            aspectRatio = width / height.coerceAtLeast(1f),
            isQuadrilateral = true,
            detectionMethod = DetectionMethod.AUTO,
            confidence = 0f,
            timestamp = System.currentTimeMillis(),
        )
    }
}

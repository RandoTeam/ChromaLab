package com.chromalab.feature.processing.document

/**
 * Platform-specific document bounds detector.
 * Uses edge detection + contour analysis to find a rectangular sheet.
 */
expect class DocumentDetector() {
    /**
     * Detect document bounds in the image.
     * Returns null if no suitable quadrilateral is found.
     */
    fun detect(imagePath: String): DocumentBounds?
}

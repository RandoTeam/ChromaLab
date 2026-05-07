package com.chromalab.feature.processing.perspective

import com.chromalab.feature.processing.document.DocumentCorners

/**
 * Platform-specific perspective warper.
 * Applies homography transform to produce a rectified image.
 */
expect class PerspectiveWarper() {
    /**
     * Warp the image using the detected document corners.
     * Returns null if warping fails.
     */
    fun warp(
        imagePath: String,
        corners: DocumentCorners,
        outputDir: String,
    ): PerspectiveCorrectionResult?
}

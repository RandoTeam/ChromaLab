package com.chromalab.feature.processing.crop

/**
 * Platform-specific image cropper.
 */
expect class ImageCropper() {
    /**
     * Crop the image at [imagePath] using [cropRect] coordinates.
     * Saves the cropped result to [outputDir]/cropped.jpg.
     */
    fun crop(imagePath: String, cropRect: CropRect, outputDir: String): CropResult?
}

package com.chromalab.feature.processing.preprocess

/**
 * Platform-specific image preprocessor.
 * Applies: grayscale → CLAHE → adaptive threshold → morphology → noise cleanup.
 */
expect class ImagePreprocessor() {
    fun preprocess(
        imagePath: String,
        outputDir: String,
        params: PreprocessingParams = PreprocessingParams(),
    ): PreprocessingResult?
}

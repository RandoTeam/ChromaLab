package com.chromalab.feature.processing.preprocess

import kotlinx.serialization.Serializable

/**
 * Parameters used for image preprocessing.
 * Recorded for deterministic reproducibility.
 */
@Serializable
data class PreprocessingParams(
    val claheClipLimit: Float = 2.0f,
    val claheTileSize: Int = 8,
    val adaptiveBlockSize: Int = 31,
    val adaptiveC: Int = 10,
    val morphKernelSize: Int = 3,
    val morphIterations: Int = 1,
    val medianFilterSize: Int = 3,
)

/**
 * Result of image preprocessing pipeline.
 */
@Serializable
data class PreprocessingResult(
    val grayscalePath: String,
    val contrastEnhancedPath: String,
    val binaryPath: String,
    val morphologyPath: String,
    val sharpenedPath: String? = null,
    val scanStylePath: String? = null,
    val sourcePath: String,
    val params: PreprocessingParams,
    val width: Int,
    val height: Int,
    val timestamp: Long,
)

@Serializable
enum class PreprocessingVariantKind {
    SOURCE,
    GRAYSCALE,
    CONTRAST_ENHANCED,
    SHARPENED,
    SCAN_STYLE,
    BINARY,
    MORPHOLOGY,
}

@Serializable
data class PreprocessingVariant(
    val id: String,
    val kind: PreprocessingVariantKind,
    val imagePath: String,
)

@Serializable
data class PreprocessingVariantScore(
    val variantId: String,
    val kind: PreprocessingVariantKind,
    val imagePath: String,
    val rank: Int,
    val score: Float,
    val darkPixelRatio: Float,
    val edgeDensity: Float,
    val contrast: Float,
    val horizontalLineStrength: Float,
    val verticalLineStrength: Float,
    val selected: Boolean,
    val warnings: List<String> = emptyList(),
)

fun PreprocessingResult.variants(): List<PreprocessingVariant> =
    listOfNotNull(
        PreprocessingVariant("source", PreprocessingVariantKind.SOURCE, sourcePath),
        PreprocessingVariant("grayscale", PreprocessingVariantKind.GRAYSCALE, grayscalePath),
        PreprocessingVariant("contrast_enhanced", PreprocessingVariantKind.CONTRAST_ENHANCED, contrastEnhancedPath),
        sharpenedPath?.let { PreprocessingVariant("sharpened", PreprocessingVariantKind.SHARPENED, it) },
        scanStylePath?.let { PreprocessingVariant("scan_style", PreprocessingVariantKind.SCAN_STYLE, it) },
        PreprocessingVariant("binary", PreprocessingVariantKind.BINARY, binaryPath),
        PreprocessingVariant("morphology", PreprocessingVariantKind.MORPHOLOGY, morphologyPath),
    )

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

package com.chromalab.feature.processing.model

/**
 * Result of a model file import operation.
 */
sealed class ModelImportResult {
    /**
     * Import succeeded. File validated and copied.
     */
    data class Success(
        val modelInfo: ModelInfo,
        val ggufInfo: GgufValidator.GgufInfo? = null,
        val warnings: List<String> = emptyList(),
    ) : ModelImportResult()

    /**
     * Import failed with validation error.
     */
    data class ValidationError(
        val message: String,
        val details: String? = null,
    ) : ModelImportResult()

    /**
     * Import failed with I/O error.
     */
    data class IoError(
        val message: String,
        val exception: Exception? = null,
    ) : ModelImportResult()
}

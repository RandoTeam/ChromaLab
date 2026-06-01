package com.chromalab.feature.processing.model

import kotlin.math.abs
import kotlin.math.max

enum class ModelDownloadPreflightIssueCode {
    EMPTY_MODEL_PACKAGE,
    MISSING_DOWNLOAD_URL,
    UNSUPPORTED_URL_SCHEME,
    INVALID_EXPECTED_SIZE,
    OBSERVED_SIZE_MISSING,
    OBSERVED_SIZE_MISMATCH,
    INSUFFICIENT_STORAGE,
}

data class ModelDownloadPreflightIssue(
    val code: ModelDownloadPreflightIssueCode,
    val fileName: String? = null,
    val message: String,
)

data class ModelDownloadPreflightResult(
    val requiredFreeBytes: Long,
    val availableFreeBytes: Long? = null,
    val issues: List<ModelDownloadPreflightIssue>,
) {
    val canProceed: Boolean get() = issues.isEmpty()
    fun requireCanProceed() {
        if (!canProceed) {
            throw IllegalStateException(issues.joinToString("; ") { it.message })
        }
    }
}

object ModelDownloadPreflightPolicy {
    const val DOWNLOAD_HEADROOM_BYTES: Long = 512L * 1024L * 1024L
    const val SIZE_TOLERANCE_BYTES: Long = 8L * 1024L * 1024L

    fun requiredFreeBytes(model: ModelInfo): Long =
        model.totalSizeBytes + DOWNLOAD_HEADROOM_BYTES

    fun partialDownloadFileName(file: ModelFile): String =
        "${file.fileName}.download"

    fun validateModelBeforeDownload(
        model: ModelInfo,
        availableFreeBytes: Long? = null,
    ): ModelDownloadPreflightResult {
        val issues = mutableListOf<ModelDownloadPreflightIssue>()

        if (model.files.isEmpty()) {
            issues += ModelDownloadPreflightIssue(
                code = ModelDownloadPreflightIssueCode.EMPTY_MODEL_PACKAGE,
                message = "Model package '${model.id}' has no files.",
            )
        }

        model.files.forEach { file ->
            if (file.downloadUrl.isBlank()) {
                issues += ModelDownloadPreflightIssue(
                    code = ModelDownloadPreflightIssueCode.MISSING_DOWNLOAD_URL,
                    fileName = file.fileName,
                    message = "Model file '${file.fileName}' has no download URL.",
                )
            } else if (!file.downloadUrl.startsWith("https://")) {
                issues += ModelDownloadPreflightIssue(
                    code = ModelDownloadPreflightIssueCode.UNSUPPORTED_URL_SCHEME,
                    fileName = file.fileName,
                    message = "Model file '${file.fileName}' must use an HTTPS download URL.",
                )
            }

            if (file.sizeBytes <= 0L) {
                issues += ModelDownloadPreflightIssue(
                    code = ModelDownloadPreflightIssueCode.INVALID_EXPECTED_SIZE,
                    fileName = file.fileName,
                    message = "Model file '${file.fileName}' has invalid expected size ${file.sizeBytes}.",
                )
            }
        }

        val required = requiredFreeBytes(model)
        if (availableFreeBytes != null && availableFreeBytes < required) {
            issues += ModelDownloadPreflightIssue(
                code = ModelDownloadPreflightIssueCode.INSUFFICIENT_STORAGE,
                message = "Model '${model.id}' requires ${required} bytes including download headroom, but only $availableFreeBytes bytes are free.",
            )
        }

        return ModelDownloadPreflightResult(
            requiredFreeBytes = required,
            availableFreeBytes = availableFreeBytes,
            issues = issues,
        )
    }

    fun validateObservedRemoteSize(
        file: ModelFile,
        observedSizeBytes: Long?,
    ): ModelDownloadPreflightIssue? {
        val observed = observedSizeBytes ?: return ModelDownloadPreflightIssue(
            code = ModelDownloadPreflightIssueCode.OBSERVED_SIZE_MISSING,
            fileName = file.fileName,
            message = "Remote size is missing for '${file.fileName}'.",
        )

        if (observed <= 0L) {
            return ModelDownloadPreflightIssue(
                code = ModelDownloadPreflightIssueCode.OBSERVED_SIZE_MISSING,
                fileName = file.fileName,
                message = "Remote size is invalid for '${file.fileName}': $observed.",
            )
        }

        val tolerance = max(SIZE_TOLERANCE_BYTES, (file.sizeBytes * 0.02).toLong())
        return if (abs(observed - file.sizeBytes) > tolerance) {
            ModelDownloadPreflightIssue(
                code = ModelDownloadPreflightIssueCode.OBSERVED_SIZE_MISMATCH,
                fileName = file.fileName,
                message = "Remote size mismatch for '${file.fileName}': expected ${file.sizeBytes}, observed $observed.",
            )
        } else {
            null
        }
    }
}

package com.chromalab.feature.processing.model

import kotlinx.serialization.Serializable

@Serializable
enum class ModelAvailabilityStatus {
    AVAILABLE,
    UNAVAILABLE,
    LOAD_FAILED,
    NOT_CONFIGURED,
    TIMEOUT,
    DISABLED,
}

@Serializable
enum class ModelAvailabilityMode {
    FULL_ANALYSIS,
    FAST,
    VALIDATION_FIXTURE,
}

@Serializable
data class ModelAvailabilityDiagnostic(
    val diagnosticId: String,
    val mode: ModelAvailabilityMode,
    val selectedModelId: String? = null,
    val executedModelId: String? = null,
    val expectedBackend: String? = null,
    val expectedPath: String? = null,
    val pathExists: Boolean? = null,
    val fileSizeBytes: Long? = null,
    val loadAttempted: Boolean,
    val loadResult: String? = null,
    val sanitizedErrorMessage: String? = null,
    val fallbackModelAttempted: Boolean = false,
    val fallbackResult: String? = null,
    val detectedDeviceTarget: String? = null,
    val selectedDeviceTarget: String? = null,
    val selectionReason: String? = null,
    val rejectedModelIds: List<String> = emptyList(),
    val status: ModelAvailabilityStatus,
    val timestampEpochMillis: Long,
)

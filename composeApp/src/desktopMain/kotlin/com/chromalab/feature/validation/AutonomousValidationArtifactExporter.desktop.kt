package com.chromalab.feature.validation

actual object AutonomousValidationArtifactExporter {
    actual fun activeRunId(): String? = null

    actual fun saveTextArtifact(
        fileName: String,
        content: String,
        mimeType: String,
    ): ValidationArtifactSaveResult =
        ValidationArtifactSaveResult(
            success = false,
            message = "Validation artifact export is only active on Android validation runs.",
        )

    actual fun copyFileArtifact(
        sourcePath: String?,
        fileName: String,
        mimeType: String,
    ): ValidationArtifactSaveResult =
        ValidationArtifactSaveResult(
            success = false,
            message = "Validation artifact export is only active on Android validation runs.",
        )
}

package com.chromalab.feature.validation

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

actual object AutonomousValidationArtifactExporter {
    var contextProvider: (() -> Context)? = null
    var validationRunIdProvider: (() -> String?)? = null

    actual fun activeRunId(): String? = validationRunIdProvider?.invoke()?.takeIf { it.isNotBlank() }

    actual fun saveTextArtifact(
        fileName: String,
        content: String,
        mimeType: String,
    ): ValidationArtifactSaveResult =
        saveBytes(fileName, content.toByteArray(Charsets.UTF_8), mimeType)

    actual fun copyFileArtifact(
        sourcePath: String?,
        fileName: String,
        mimeType: String,
    ): ValidationArtifactSaveResult {
        val source = sourcePath?.takeIf { it.isNotBlank() }?.let(::File)
            ?: return ValidationArtifactSaveResult(false, message = "Source path is missing.")
        if (!source.exists()) {
            return ValidationArtifactSaveResult(false, message = "Source file does not exist: $sourcePath")
        }
        return saveBytes(fileName, source.readBytes(), mimeType)
    }

    private fun saveBytes(
        fileName: String,
        bytes: ByteArray,
        mimeType: String,
    ): ValidationArtifactSaveResult {
        val context = contextProvider?.invoke()
            ?: return ValidationArtifactSaveResult(false, message = "Android context is not available.")
        val runId = activeRunId()
            ?: return ValidationArtifactSaveResult(false, message = "No active validation run id.")
        val safeName = fileName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().ifBlank {
            "validation_artifact.bin"
        }
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/ChromaLab/validation/$runId"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return ValidationArtifactSaveResult(false, message = "Could not create Downloads entry.")
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: return ValidationArtifactSaveResult(false, message = "Could not open Downloads output stream.")
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                ValidationArtifactSaveResult(true, uri.toString(), "Saved to $relativePath/$safeName")
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ChromaLab/validation/$runId",
                ).apply { mkdirs() }
                val file = File(dir, safeName)
                file.writeBytes(bytes)
                ValidationArtifactSaveResult(true, file.absolutePath, "Saved to ${file.absolutePath}")
            }
        } catch (e: Exception) {
            ValidationArtifactSaveResult(false, message = e.message ?: "Android validation export failed.")
        }
    }
}

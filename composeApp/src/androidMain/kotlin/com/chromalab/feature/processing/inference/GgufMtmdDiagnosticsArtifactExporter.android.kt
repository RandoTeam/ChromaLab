package com.chromalab.feature.processing.inference

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GgufMtmdDiagnosticsArtifactExporter(
    private val context: Context,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun export(summary: GgufMtmdDiagnosticsSummary): List<GgufMtmdDiagnosticsExportRecord> {
        val jsonRecord = saveText(
            runId = summary.runId,
            fileName = "gguf_mtmd_diagnostics_${summary.runId}.json",
            content = json.encodeToString(summary),
            mimeType = "application/json",
        )
        val markdownRecord = saveText(
            runId = summary.runId,
            fileName = "gguf_mtmd_diagnostics_${summary.runId}.md",
            content = GgufMtmdDiagnosticsMarkdownRenderer.render(summary),
            mimeType = "text/markdown",
        )
        return listOf(jsonRecord, markdownRecord)
    }

    private fun saveText(
        runId: String,
        fileName: String,
        content: String,
        mimeType: String,
    ): GgufMtmdDiagnosticsExportRecord {
        val safeName = fileName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
            .ifBlank { "gguf_mtmd_diagnostics.txt" }
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/ChromaLab/runtime/mtmd-diagnostics/$runId"
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
                    ?: return GgufMtmdDiagnosticsExportRecord(
                        fileName = safeName,
                        success = false,
                        message = "Could not create Downloads entry.",
                    )
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                } ?: return GgufMtmdDiagnosticsExportRecord(
                    fileName = safeName,
                    success = false,
                    message = "Could not open Downloads output stream.",
                )
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                GgufMtmdDiagnosticsExportRecord(
                    fileName = safeName,
                    success = true,
                    uriOrPath = uri.toString(),
                    message = "Saved to $relativePath/$safeName",
                )
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ChromaLab/runtime/mtmd-diagnostics/$runId",
                ).apply { mkdirs() }
                val file = File(dir, safeName)
                file.writeText(content)
                GgufMtmdDiagnosticsExportRecord(
                    fileName = safeName,
                    success = true,
                    uriOrPath = file.absolutePath,
                    message = "Saved to ${file.absolutePath}",
                )
            }
        } catch (e: Exception) {
            GgufMtmdDiagnosticsExportRecord(
                fileName = safeName,
                success = false,
                message = e.message ?: "mtmd diagnostics export failed.",
            )
        }
    }
}

data class GgufMtmdDiagnosticsExportRecord(
    val fileName: String,
    val success: Boolean,
    val uriOrPath: String? = null,
    val message: String,
)

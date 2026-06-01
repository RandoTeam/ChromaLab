package com.chromalab.feature.processing.inference

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MtpAbBenchmarkArtifactExporter(
    private val context: Context,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun export(summary: GgufMtpBenchmarkSummary): List<MtpAbBenchmarkExportRecord> {
        val jsonRecord = saveText(
            runId = summary.runId,
            fileName = "gguf_mtp_ab_${summary.runId}.json",
            content = json.encodeToString(summary),
            mimeType = "application/json",
        )
        val markdownRecord = saveText(
            runId = summary.runId,
            fileName = "gguf_mtp_ab_${summary.runId}.md",
            content = GgufMtpBenchmarkMarkdownRenderer.render(summary),
            mimeType = "text/markdown",
        )
        return listOf(jsonRecord, markdownRecord)
    }

    private fun saveText(
        runId: String,
        fileName: String,
        content: String,
        mimeType: String,
    ): MtpAbBenchmarkExportRecord {
        val safeName = fileName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
            .ifBlank { "gguf_mtp_ab.txt" }
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/ChromaLab/runtime/mtp-ab/$runId"
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
                    ?: return MtpAbBenchmarkExportRecord(
                        fileName = safeName,
                        success = false,
                        message = "Could not create Downloads entry.",
                    )
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                } ?: return MtpAbBenchmarkExportRecord(
                    fileName = safeName,
                    success = false,
                    message = "Could not open Downloads output stream.",
                )
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                MtpAbBenchmarkExportRecord(
                    fileName = safeName,
                    success = true,
                    uriOrPath = uri.toString(),
                    message = "Saved to $relativePath/$safeName",
                )
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ChromaLab/runtime/mtp-ab/$runId",
                ).apply { mkdirs() }
                val file = File(dir, safeName)
                file.writeText(content)
                MtpAbBenchmarkExportRecord(
                    fileName = safeName,
                    success = true,
                    uriOrPath = file.absolutePath,
                    message = "Saved to ${file.absolutePath}",
                )
            }
        } catch (e: Exception) {
            MtpAbBenchmarkExportRecord(
                fileName = safeName,
                success = false,
                message = e.message ?: "MTP A/B benchmark export failed.",
            )
        }
    }
}

data class MtpAbBenchmarkExportRecord(
    val fileName: String,
    val success: Boolean,
    val uriOrPath: String? = null,
    val message: String,
)

package com.chromalab.feature.processing.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

actual object FileSharer {
    /** Must be set from Activity/Application before use. */
    var contextProvider: (() -> Context)? = null

    actual fun share(filePath: String, mimeType: String) {
        val context = contextProvider?.invoke() ?: return
        val file = File(filePath)
        if (!file.exists()) return
        shareExistingFile(context, file, mimeType)
    }

    actual fun saveText(fileName: String, content: String, mimeType: String): ExportFileResult {
        val context = contextProvider?.invoke()
            ?: return ExportFileResult(success = false, message = "Android context is not available")
        return try {
            val safeName = safeFileName(fileName)
            val bytes = content.toByteArray(Charsets.UTF_8)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ChromaLab")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return ExportFileResult(success = false, message = "Could not create Downloads entry")
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                    ?: return ExportFileResult(success = false, message = "Could not open Downloads output stream")
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                ExportFileResult(
                    success = true,
                    location = uri.toString(),
                    message = "Saved to Downloads/ChromaLab/$safeName",
                )
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ChromaLab",
                ).apply { mkdirs() }
                val file = File(dir, safeName)
                file.writeBytes(bytes)
                ExportFileResult(
                    success = true,
                    location = file.absolutePath,
                    message = "Saved to ${file.absolutePath}",
                )
            }
        } catch (e: Exception) {
            ExportFileResult(success = false, message = e.message ?: "Android save failed")
        }
    }

    actual fun shareText(fileName: String, content: String, mimeType: String): ExportFileResult {
        val context = contextProvider?.invoke()
            ?: return ExportFileResult(success = false, message = "Android context is not available")
        return try {
            val file = File(
                File(context.cacheDir, "exports").apply { mkdirs() },
                safeFileName(fileName),
            )
            file.writeText(content, Charsets.UTF_8)
            shareExistingFile(context, file, mimeType)
            ExportFileResult(
                success = true,
                location = file.absolutePath,
                message = "Share sheet opened for ${file.name}",
            )
        } catch (e: Exception) {
            ExportFileResult(success = false, message = e.message ?: "Android share failed")
        }
    }

    private fun shareExistingFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Export").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun safeFileName(fileName: String): String {
        val cleaned = fileName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
        return cleaned.ifBlank { "chromalab_export.txt" }
    }
}

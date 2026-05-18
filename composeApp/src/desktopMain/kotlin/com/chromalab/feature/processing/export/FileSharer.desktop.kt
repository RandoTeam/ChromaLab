package com.chromalab.feature.processing.export

import java.io.File

actual object FileSharer {
    actual fun share(filePath: String, mimeType: String) {
        // Desktop: stub — no share sheet available
        println("FileSharer.share() — desktop stub: $filePath ($mimeType)")
    }

    actual fun saveText(fileName: String, content: String, mimeType: String): ExportFileResult {
        return try {
            val dir = File(System.getProperty("user.home"), "Downloads/ChromaLab").apply { mkdirs() }
            val file = File(dir, safeFileName(fileName))
            file.writeText(content)
            ExportFileResult(success = true, location = file.absolutePath, message = "Saved to ${file.absolutePath}")
        } catch (e: Exception) {
            ExportFileResult(success = false, message = e.message ?: "Desktop save failed")
        }
    }

    actual fun shareText(fileName: String, content: String, mimeType: String): ExportFileResult {
        val result = saveText(fileName, content, mimeType)
        if (result.success && result.location != null) {
            share(result.location, mimeType)
        }
        return result
    }

    private fun safeFileName(fileName: String): String =
        fileName.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "chromalab_export.txt" }
}

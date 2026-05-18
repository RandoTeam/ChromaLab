package com.chromalab.feature.processing.export

data class ExportFileResult(
    val success: Boolean,
    val location: String? = null,
    val message: String,
)

/**
 * Platform-specific file sharing.
 */
expect object FileSharer {
    /**
     * Share a file using the system share sheet.
     * @param filePath Absolute path to the file
     * @param mimeType MIME type (e.g. "text/csv", "application/json")
     */
    fun share(filePath: String, mimeType: String)

    /**
     * Save text content to the user's device-visible export location.
     */
    fun saveText(fileName: String, content: String, mimeType: String): ExportFileResult

    /**
     * Write text content to a temporary shared file and open the system share sheet.
     */
    fun shareText(fileName: String, content: String, mimeType: String): ExportFileResult
}

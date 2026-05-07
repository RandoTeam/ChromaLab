package com.chromalab.feature.processing.export

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
}

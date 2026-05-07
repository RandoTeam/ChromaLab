package com.chromalab.feature.processing.storage

/**
 * Platform-specific session directory writer.
 * Writes intermediate files to the session directory on disk.
 */
expect class SessionWriter(sessionDir: String) {
    /**
     * Write text content to a file in the session directory.
     * @param filename File name (not path)
     * @param content Text content
     * @return Absolute path to the written file
     */
    fun writeText(filename: String, content: String): String

    /**
     * Copy a file into the session directory.
     * @param sourcePath Absolute source path
     * @param filename Target filename in session dir
     * @return Absolute path to the copied file, or null on failure
     */
    fun copyFile(sourcePath: String, filename: String): String?

    /**
     * List all files in the session directory.
     */
    fun listFiles(): List<String>
}

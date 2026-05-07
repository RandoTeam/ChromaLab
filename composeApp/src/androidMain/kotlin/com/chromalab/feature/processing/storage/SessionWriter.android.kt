package com.chromalab.feature.processing.storage

import java.io.File

actual class SessionWriter actual constructor(private val sessionDir: String) {

    init {
        File(sessionDir).mkdirs()
    }

    actual fun writeText(filename: String, content: String): String {
        val file = File(sessionDir, filename)
        file.writeText(content)
        return file.absolutePath
    }

    actual fun copyFile(sourcePath: String, filename: String): String? {
        return try {
            val src = File(sourcePath)
            if (!src.exists()) return null
            val dst = File(sessionDir, filename)
            src.copyTo(dst, overwrite = true)
            dst.absolutePath
        } catch (_: Exception) {
            null
        }
    }

    actual fun listFiles(): List<String> {
        return File(sessionDir).listFiles()?.map { it.name } ?: emptyList()
    }
}

package com.chromalab.feature.processing.debug

import com.chromalab.feature.processing.storage.IntermediateFileSaver
import com.chromalab.feature.processing.storage.SessionWriter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Exports a complete debug package: all intermediate images + data files.
 * Used for diagnostics and reproducing issues.
 */
object DebugPackageExporter {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Export debug info to JSON.
     */
    fun exportDebugInfo(info: DebugInfo): String = json.encodeToString(info)

    /**
     * Write full debug package to the session directory.
     * Copies all intermediate images and writes debug_info.json.
     */
    fun writeDebugPackage(
        writer: SessionWriter,
        debugInfo: DebugInfo,
        intermediatePaths: Map<String, String>,
    ): List<String> {
        val files = mutableListOf<String>()

        // Write debug_info.json
        val infoPath = writer.writeText("debug_info.json", exportDebugInfo(debugInfo))
        files.add(infoPath)

        // Copy intermediate images
        for ((name, path) in intermediatePaths) {
            val copied = writer.copyFile(path, "debug_$name")
            if (copied != null) files.add(copied)
        }

        return files
    }
}

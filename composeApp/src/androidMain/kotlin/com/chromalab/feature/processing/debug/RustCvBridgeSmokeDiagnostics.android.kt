package com.chromalab.feature.processing.debug

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.chromalab.feature.reports.ReportExportPrivacyClass
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val RUST_BRIDGE_SMOKE_TAG = "ChromaLabRustSmoke"

class RustCvBridgeSmokeDiagnostics(
    private val context: Context,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun run(): RustCvBridgeSmokeSummary {
        val runId = "rust_bridge_smoke_${System.currentTimeMillis()}"
        val diagnostic = RustCvBridgeRuntimeProbe.diagnostic()
        val available = diagnostic.loadResult == "AVAILABLE"
        val summary = RustCvBridgeSmokeSummary(
            runId = runId,
            packageName = context.packageName,
            generatedAtEpochMillis = System.currentTimeMillis(),
            diagnostic = diagnostic.copy(
                fallbackReason = diagnostic.fallbackReason?.let(::sanitizeMessage),
                privacyClass = ReportExportPrivacyClass.TECHNICAL_EVIDENCE,
            ),
            accepted = available,
            decision = if (available) {
                "PASS"
            } else {
                "FAIL"
            },
            artifactDirectory = "/sdcard/Download/ChromaLab/runtime/rust-bridge-smoke/$runId",
        )

        val jsonRecord = saveText(
            runId = runId,
            fileName = "rust_bridge_smoke_$runId.json",
            content = json.encodeToString(summary),
            mimeType = "application/json",
        )
        val markdownRecord = saveText(
            runId = runId,
            fileName = "rust_bridge_smoke_$runId.md",
            content = RustCvBridgeSmokeMarkdownRenderer.render(summary),
            mimeType = "text/markdown",
        )
        val withExports = summary.copy(exportRecords = listOf(jsonRecord, markdownRecord))
        Log.i(
            RUST_BRIDGE_SMOKE_TAG,
            "runId=${withExports.runId} decision=${withExports.decision} " +
                "loadResult=${withExports.diagnostic.loadResult} artifacts=${withExports.artifactDirectory}",
        )
        return withExports
    }

    private fun saveText(
        runId: String,
        fileName: String,
        content: String,
        mimeType: String,
    ): RustCvBridgeSmokeExportRecord {
        val safeName = fileName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
            .ifBlank { "rust_bridge_smoke.txt" }
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/ChromaLab/runtime/rust-bridge-smoke/$runId"
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
                    ?: return RustCvBridgeSmokeExportRecord(
                        fileName = safeName,
                        success = false,
                        message = "Could not create Downloads entry.",
                    )
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                } ?: return RustCvBridgeSmokeExportRecord(
                    fileName = safeName,
                    success = false,
                    message = "Could not open Downloads output stream.",
                )
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                RustCvBridgeSmokeExportRecord(
                    fileName = safeName,
                    success = true,
                    uriOrPath = uri.toString(),
                    message = "Saved to $relativePath/$safeName",
                )
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ChromaLab/runtime/rust-bridge-smoke/$runId",
                ).apply { mkdirs() }
                val file = File(dir, safeName)
                file.writeText(content, Charsets.UTF_8)
                RustCvBridgeSmokeExportRecord(
                    fileName = safeName,
                    success = true,
                    uriOrPath = file.absolutePath,
                    message = "Saved to ${file.absolutePath}",
                )
            }
        } catch (e: Exception) {
            RustCvBridgeSmokeExportRecord(
                fileName = safeName,
                success = false,
                message = sanitizeMessage(e.message ?: "Rust bridge smoke export failed."),
            )
        }
    }

    private fun sanitizeMessage(message: String): String =
        message
            .replace(Regex("""/data/(data|user/0)/[^\\s;:]+"""), "/data/<private>")
            .replace(Regex("""C:/Users/[^\\s;:]+""", RegexOption.IGNORE_CASE), "C:/Users/<private>")
}

@Serializable
data class RustCvBridgeSmokeSummary(
    val runId: String,
    val packageName: String,
    val generatedAtEpochMillis: Long,
    val diagnostic: StructuredRuntimeDiagnostic,
    val accepted: Boolean,
    val decision: String,
    val artifactDirectory: String,
    val exportRecords: List<RustCvBridgeSmokeExportRecord> = emptyList(),
)

@Serializable
data class RustCvBridgeSmokeExportRecord(
    val fileName: String,
    val success: Boolean,
    val uriOrPath: String? = null,
    val message: String,
)

object RustCvBridgeSmokeMarkdownRenderer {
    fun render(summary: RustCvBridgeSmokeSummary): String = buildString {
        appendLine("# Rust CV Bridge Smoke Check")
        appendLine()
        appendLine("- Run id: `${summary.runId}`")
        appendLine("- Package: `${summary.packageName}`")
        appendLine("- Decision: `${summary.decision}`")
        appendLine("- Accepted: `${summary.accepted}`")
        appendLine("- Source: `${summary.diagnostic.source}`")
        appendLine("- Backend: `${summary.diagnostic.backend ?: "unknown"}`")
        appendLine("- Load attempted: `${summary.diagnostic.loadAttempted}`")
        appendLine("- Load result: `${summary.diagnostic.loadResult ?: "unknown"}`")
        appendLine("- Contract: `${summary.diagnostic.runtimeExposesMtp ?: "unknown"}`")
        appendLine("- MTP: `${summary.diagnostic.mtpEnabled ?: "not_applicable"}`")
        appendLine()
        appendLine("This smoke check verifies Android native packaging and JNI loading only.")
        appendLine("It does not grant Rust authority over graph geometry, calibration, peaks, or chromatographic math.")
    }
}

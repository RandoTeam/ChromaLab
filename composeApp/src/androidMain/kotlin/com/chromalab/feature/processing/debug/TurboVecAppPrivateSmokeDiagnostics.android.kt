package com.chromalab.feature.processing.debug

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.chromalab.feature.knowledge.ChromaLabKnowledgeSeedV2
import com.chromalab.feature.processing.rust.RustCvBridge
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

private const val TURBOVEC_APP_PRIVATE_TAG = "ChromaLabTurboVec"
private const val TV7_CONTRACT = "TV7_TURBOVEC_APP_PRIVATE_PROVIDER_V1"

private val TV7_ENTRY_ID_BY_DENSE_ID = mapOf(
    1001L to "kp2-term-chromatogram",
    1002L to "kp2-term-retention-time",
    1003L to "kp2-term-peak",
    1004L to "kp2-term-peak-apex",
)

class TurboVecAppPrivateSmokeDiagnostics(
    private val context: Context,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun run(): TurboVecAppPrivateSmokeSummary {
        val runId = "turbovec_app_private_${System.currentTimeMillis()}"
        val rustResponse = runCatching {
            RustCvBridge.turboVecAppPrivateProbeJson(
                appPrivateRoot = context.filesDir.absolutePath,
                cleanup = true,
            )
        }.getOrElse { error ->
            """{"status":"ERROR","ffiContract":"$TV7_CONTRACT","error":"${sanitizeMessage(error.message ?: "TurboVec app-private JNI call failed.")}"}"""
        }
        val parsed = runCatching {
            Json.parseToJsonElement(rustResponse).jsonObject
        }.getOrNull()
        val topIds = parsed?.get("topIds")?.jsonArray?.mapNotNull {
            it.jsonPrimitive.longOrNull
        }.orEmpty()
        val status = parsed?.get("status")?.jsonPrimitive?.contentOrNull ?: "ERROR"
        val contract = parsed?.get("ffiContract")?.jsonPrimitive?.contentOrNull ?: "unknown"
        val pathClass = parsed?.get("pathClass")?.jsonPrimitive?.contentOrNull ?: "unknown"
        val top1Ok = parsed?.get("top1Ok")?.jsonPrimitive?.booleanOrNull ?: false
        val nativeIdsValid = parsed?.get("allIdsValid")?.jsonPrimitive?.booleanOrNull ?: false
        val validKnowledgeEntryIds = ChromaLabKnowledgeSeedV2.pack.entries
            .map { it.entryId }
            .toSet()
        val topEntryIds = topIds.mapNotNull { id -> TV7_ENTRY_ID_BY_DENSE_ID[id] }
        val allEntryIdsValid = topIds.isNotEmpty() &&
            topIds.size == topEntryIds.size &&
            topEntryIds.all { entryId -> entryId in validKnowledgeEntryIds }
        val allIdsValid = nativeIdsValid && allEntryIdsValid
        val cleanupOk = parsed?.get("indexExistsAfterCleanup")?.jsonPrimitive?.booleanOrNull == false
        val runtimePromotion = parsed?.get("runtimePromotion")?.jsonPrimitive?.booleanOrNull ?: true
        val activeOwnerUnchanged =
            parsed?.get("activeRetrievalOwnerUnchanged")?.jsonPrimitive?.booleanOrNull ?: false
        val accepted = status == "PASS" &&
            contract == TV7_CONTRACT &&
            pathClass == "APP_PRIVATE" &&
            top1Ok &&
            allIdsValid &&
            cleanupOk &&
            !runtimePromotion &&
            activeOwnerUnchanged
        val baseSummary = TurboVecAppPrivateSmokeSummary(
            runId = runId,
            packageName = context.packageName,
            generatedAtEpochMillis = System.currentTimeMillis(),
            status = status,
            ffiContract = contract,
            backendId = parsed?.get("backendId")?.jsonPrimitive?.contentOrNull ?: "unknown",
            pathClass = pathClass,
            indexRelativePath = parsed?.get("indexRelativePath")?.jsonPrimitive?.contentOrNull,
            dim = parsed?.get("dim")?.jsonPrimitive?.intOrNull ?: 0,
            bitWidth = parsed?.get("bitWidth")?.jsonPrimitive?.intOrNull ?: 0,
            vectorCount = parsed?.get("vectorCount")?.jsonPrimitive?.intOrNull ?: 0,
            indexBytes = parsed?.get("indexBytes")?.jsonPrimitive?.longOrNull ?: 0,
            buildMs = parsed?.get("buildMs")?.jsonPrimitive?.longOrNull ?: 0,
            writeMs = parsed?.get("writeMs")?.jsonPrimitive?.longOrNull ?: 0,
            loadMs = parsed?.get("loadMs")?.jsonPrimitive?.longOrNull ?: 0,
            queryMs = parsed?.get("queryMs")?.jsonPrimitive?.longOrNull ?: 0,
            rssBeforeKb = parsed?.get("rssBeforeKb")?.jsonPrimitive?.longOrNull ?: 0,
            rssAfterKb = parsed?.get("rssAfterKb")?.jsonPrimitive?.longOrNull ?: 0,
            topIds = topIds,
            topEntryIds = topEntryIds,
            top1Expected = parsed?.get("top1Expected")?.jsonPrimitive?.longOrNull ?: 0,
            top1Ok = top1Ok,
            allIdsValid = allIdsValid,
            allEntryIdsValid = allEntryIdsValid,
            queryTimedOut = parsed?.get("queryTimedOut")?.jsonPrimitive?.booleanOrNull ?: false,
            cleanupRequested = parsed?.get("cleanupRequested")?.jsonPrimitive?.booleanOrNull ?: false,
            cleanupResult = parsed?.get("cleanupResult")?.jsonPrimitive?.contentOrNull,
            indexExistsAfterCleanup =
                parsed?.get("indexExistsAfterCleanup")?.jsonPrimitive?.booleanOrNull ?: true,
            runtimePromotion = runtimePromotion,
            activeRetrievalOwnerUnchanged = activeOwnerUnchanged,
            errorMessage = parsed?.get("error")?.jsonPrimitive?.contentOrNull?.let(::sanitizeMessage),
            accepted = accepted,
            decision = if (accepted) "PASS" else "FAIL",
            artifactDirectory = "/sdcard/Download/ChromaLab/runtime/turbovec-app-private/$runId",
        )

        val rustResponseRecord = saveText(
            runId = runId,
            fileName = "turbovec_app_private_response_$runId.json",
            content = rustResponse,
            mimeType = "application/json",
        )
        val summaryJsonRecord = saveText(
            runId = runId,
            fileName = "turbovec_app_private_smoke_$runId.json",
            content = json.encodeToString(baseSummary),
            mimeType = "application/json",
        )
        val summaryMarkdownRecord = saveText(
            runId = runId,
            fileName = "turbovec_app_private_smoke_$runId.md",
            content = TurboVecAppPrivateSmokeMarkdownRenderer.render(baseSummary),
            mimeType = "text/markdown",
        )
        val exportRecords = listOf(rustResponseRecord, summaryJsonRecord, summaryMarkdownRecord)
        val withExports = baseSummary.copy(
            exportRecords = exportRecords,
            accepted = baseSummary.accepted && exportRecords.all { it.success },
            decision = if (baseSummary.accepted && exportRecords.all { it.success }) "PASS" else "FAIL",
        )
        Log.i(
            TURBOVEC_APP_PRIVATE_TAG,
            "runId=${withExports.runId} decision=${withExports.decision} " +
                "status=${withExports.status} topIds=${withExports.topIds.joinToString()} " +
                "artifacts=${withExports.artifactDirectory}",
        )
        return withExports
    }

    private fun saveText(
        runId: String,
        fileName: String,
        content: String,
        mimeType: String,
    ): TurboVecAppPrivateSmokeExportRecord {
        val safeName = fileName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
            .ifBlank { "turbovec_app_private_smoke.txt" }
        val relativePath =
            "${Environment.DIRECTORY_DOWNLOADS}/ChromaLab/runtime/turbovec-app-private/$runId"
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
                    ?: return TurboVecAppPrivateSmokeExportRecord(
                        fileName = safeName,
                        success = false,
                        message = "Could not create Downloads entry.",
                    )
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                } ?: return TurboVecAppPrivateSmokeExportRecord(
                    fileName = safeName,
                    success = false,
                    message = "Could not open Downloads output stream.",
                )
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                TurboVecAppPrivateSmokeExportRecord(
                    fileName = safeName,
                    success = true,
                    uriOrPath = uri.toString(),
                    message = "Saved to $relativePath/$safeName",
                )
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ChromaLab/runtime/turbovec-app-private/$runId",
                ).apply { mkdirs() }
                val file = File(dir, safeName)
                file.writeText(content, Charsets.UTF_8)
                TurboVecAppPrivateSmokeExportRecord(
                    fileName = safeName,
                    success = true,
                    uriOrPath = file.absolutePath,
                    message = "Saved to ${file.absolutePath}",
                )
            }
        } catch (e: Exception) {
            TurboVecAppPrivateSmokeExportRecord(
                fileName = safeName,
                success = false,
                message = sanitizeMessage(e.message ?: "TurboVec app-private smoke export failed."),
            )
        }
    }

    private fun sanitizeMessage(message: String): String =
        message
            .replace(Regex("""/data/(data|user/0)/[^\\s;:]+"""), "/data/<private>")
            .replace(Regex("""C:/Users/[^\\s;:]+""", RegexOption.IGNORE_CASE), "C:/Users/<private>")
            .replace("\"", "'")
}

@Serializable
data class TurboVecAppPrivateSmokeSummary(
    val runId: String,
    val packageName: String,
    val generatedAtEpochMillis: Long,
    val status: String,
    val ffiContract: String,
    val backendId: String,
    val pathClass: String,
    val indexRelativePath: String? = null,
    val dim: Int,
    val bitWidth: Int,
    val vectorCount: Int,
    val indexBytes: Long,
    val buildMs: Long,
    val writeMs: Long,
    val loadMs: Long,
    val queryMs: Long,
    val rssBeforeKb: Long,
    val rssAfterKb: Long,
    val topIds: List<Long>,
    val topEntryIds: List<String>,
    val top1Expected: Long,
    val top1Ok: Boolean,
    val allIdsValid: Boolean,
    val allEntryIdsValid: Boolean,
    val queryTimedOut: Boolean,
    val cleanupRequested: Boolean,
    val cleanupResult: String? = null,
    val indexExistsAfterCleanup: Boolean,
    val runtimePromotion: Boolean,
    val activeRetrievalOwnerUnchanged: Boolean,
    val errorMessage: String? = null,
    val accepted: Boolean,
    val decision: String,
    val artifactDirectory: String,
    val exportRecords: List<TurboVecAppPrivateSmokeExportRecord> = emptyList(),
)

@Serializable
data class TurboVecAppPrivateSmokeExportRecord(
    val fileName: String,
    val success: Boolean,
    val uriOrPath: String? = null,
    val message: String,
)

object TurboVecAppPrivateSmokeMarkdownRenderer {
    fun render(summary: TurboVecAppPrivateSmokeSummary): String = buildString {
        appendLine("# TurboVec App-Private Provider Smoke Check")
        appendLine()
        appendLine("- Run id: `${summary.runId}`")
        appendLine("- Package: `${summary.packageName}`")
        appendLine("- Decision: `${summary.decision}`")
        appendLine("- Accepted: `${summary.accepted}`")
        appendLine("- Status: `${summary.status}`")
        appendLine("- Contract: `${summary.ffiContract}`")
        appendLine("- Backend id: `${summary.backendId}`")
        appendLine("- Path class: `${summary.pathClass}`")
        appendLine("- Index relative path: `${summary.indexRelativePath ?: "unknown"}`")
        appendLine("- Dimension: `${summary.dim}`")
        appendLine("- Bit width: `${summary.bitWidth}`")
        appendLine("- Vector count: `${summary.vectorCount}`")
        appendLine("- Index bytes: `${summary.indexBytes}`")
        appendLine("- Build ms: `${summary.buildMs}`")
        appendLine("- Write ms: `${summary.writeMs}`")
        appendLine("- Load ms: `${summary.loadMs}`")
        appendLine("- Query ms: `${summary.queryMs}`")
        appendLine("- RSS before KB: `${summary.rssBeforeKb}`")
        appendLine("- RSS after KB: `${summary.rssAfterKb}`")
        appendLine("- Top ids: `${summary.topIds.joinToString()}`")
        appendLine("- Top entry ids: `${summary.topEntryIds.joinToString()}`")
        appendLine("- Expected top-1: `${summary.top1Expected}`")
        appendLine("- Top-1 OK: `${summary.top1Ok}`")
        appendLine("- All ids valid: `${summary.allIdsValid}`")
        appendLine("- All entry ids valid: `${summary.allEntryIdsValid}`")
        appendLine("- Query timed out: `${summary.queryTimedOut}`")
        appendLine("- Cleanup requested: `${summary.cleanupRequested}`")
        appendLine("- Cleanup result: `${summary.cleanupResult ?: "unknown"}`")
        appendLine("- Index exists after cleanup: `${summary.indexExistsAfterCleanup}`")
        appendLine("- Runtime promotion: `${summary.runtimePromotion}`")
        appendLine("- Active retrieval owner unchanged: `${summary.activeRetrievalOwnerUnchanged}`")
        summary.errorMessage?.let { appendLine("- Error: `$it`") }
        appendLine()
        appendLine("This check verifies app-private TurboVec index load/query behavior only.")
        appendLine("It does not promote TurboVec into the active Knowledge retrieval owner.")
        appendLine("It does not affect graph detection, calibration, trace extraction, peak metrics, report gates, or CalculationEngine math.")
    }
}

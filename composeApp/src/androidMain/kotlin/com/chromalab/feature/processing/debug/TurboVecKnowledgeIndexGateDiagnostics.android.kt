package com.chromalab.feature.processing.debug

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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

private const val TURBOVEC_KNOWLEDGE_GATE_TAG = "ChromaLabTurboVec"
private const val TV8_CONTRACT = "TV8_TURBOVEC_REAL_KNOWLEDGE_INDEX_GATE_V1"
private const val TV8_INDEX_RELATIVE_PATH =
    "chromalab_tv8_knowledge/minilm/chromalab_knowledge_v2_minilm.tvim"
private const val TV8_SIDECAR_RELATIVE_PATH =
    "chromalab_tv8_knowledge/minilm/chromalab_knowledge_v2_minilm_sidecar.json"
private const val TV8_QUERY_RELATIVE_PATH =
    "chromalab_tv8_knowledge/minilm/chromalab_knowledge_v2_minilm_queries.json"

class TurboVecKnowledgeIndexGateDiagnostics(
    private val context: Context,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun run(): TurboVecKnowledgeIndexGateSummary {
        val runId = "turbovec_knowledge_gate_${System.currentTimeMillis()}"
        val rustResponse = runCatching {
            RustCvBridge.turboVecKnowledgeIndexGateJson(
                appPrivateRoot = context.filesDir.absolutePath,
                indexRelativePath = TV8_INDEX_RELATIVE_PATH,
                sidecarRelativePath = TV8_SIDECAR_RELATIVE_PATH,
                queryVectorsRelativePath = TV8_QUERY_RELATIVE_PATH,
                cleanup = true,
            )
        }.getOrElse { error ->
            """{"status":"ERROR","ffiContract":"$TV8_CONTRACT","error":"${sanitizeMessage(error.message ?: "TurboVec Knowledge gate JNI call failed.")}"}"""
        }
        val parsed = runCatching {
            Json.parseToJsonElement(rustResponse).jsonObject
        }.getOrNull()
        val status = parsed?.get("status")?.jsonPrimitive?.contentOrNull ?: "ERROR"
        val contract = parsed?.get("ffiContract")?.jsonPrimitive?.contentOrNull ?: "unknown"
        val pathClass = parsed?.get("pathClass")?.jsonPrimitive?.contentOrNull ?: "unknown"
        val realIndexPassed =
            parsed?.get("realKnowledgeIndexPassed")?.jsonPrimitive?.booleanOrNull ?: false
        val localEmbeddingAvailable =
            parsed?.get("localAndroidEmbeddingAvailable")?.jsonPrimitive?.booleanOrNull ?: false
        val runtimePromotion = parsed?.get("runtimePromotion")?.jsonPrimitive?.booleanOrNull ?: true
        val activeOwnerUnchanged =
            parsed?.get("activeRetrievalOwnerUnchanged")?.jsonPrimitive?.booleanOrNull ?: false
        val queryTimedOut = parsed?.get("queryTimedOut")?.jsonPrimitive?.booleanOrNull ?: true
        val filesExistAfterCleanup =
            parsed?.get("filesExistAfterCleanup")?.jsonPrimitive?.booleanOrNull ?: true
        val caseSummaries = parsed?.get("cases")?.jsonArray?.map { element ->
            val obj = element.jsonObject
            TurboVecKnowledgeIndexGateCaseSummary(
                queryId = obj["queryId"]?.jsonPrimitive?.contentOrNull ?: "unknown",
                status = obj["status"]?.jsonPrimitive?.contentOrNull ?: "ERROR",
                topEntryIds = obj["topEntryIds"]?.jsonArray?.mapNotNull {
                    it.jsonPrimitive.contentOrNull
                }.orEmpty(),
                missingRequiredEntryIds = obj["missingRequiredEntryIds"]?.jsonArray?.mapNotNull {
                    it.jsonPrimitive.contentOrNull
                }.orEmpty(),
                forbiddenEntryIdsPresent = obj["forbiddenEntryIdsPresent"]?.jsonArray?.mapNotNull {
                    it.jsonPrimitive.contentOrNull
                }.orEmpty(),
                hit = obj["hit"]?.jsonPrimitive?.booleanOrNull ?: false,
                top1Hit = obj["top1Hit"]?.jsonPrimitive?.booleanOrNull ?: false,
                allResultsValid = obj["allResultsValid"]?.jsonPrimitive?.booleanOrNull ?: false,
            )
        }.orEmpty()
        val realIndexGatePassed = status == "PASS" &&
            contract == TV8_CONTRACT &&
            pathClass == "APP_PRIVATE" &&
            realIndexPassed &&
            caseSummaries.isNotEmpty() &&
            caseSummaries.all { it.status == "PASS" && it.allResultsValid } &&
            !queryTimedOut &&
            !filesExistAfterCleanup &&
            !runtimePromotion &&
            activeOwnerUnchanged
        val promotionDecision = when {
            !realIndexGatePassed -> "TV8_REAL_INDEX_GATE_FAILED"
            !localEmbeddingAvailable -> "BLOCKED_LOCAL_QUERY_EMBEDDING_UNAVAILABLE"
            else -> "READY_FOR_TV9_CANDIDATE_POLICY"
        }
        val baseSummary = TurboVecKnowledgeIndexGateSummary(
            runId = runId,
            packageName = context.packageName,
            generatedAtEpochMillis = System.currentTimeMillis(),
            decision = promotionDecision,
            status = status,
            ffiContract = contract,
            backendId = parsed?.get("backendId")?.jsonPrimitive?.contentOrNull ?: "unknown",
            pathClass = pathClass,
            profileKey = parsed?.get("profileKey")?.jsonPrimitive?.contentOrNull ?: "unknown",
            modelId = parsed?.get("modelId")?.jsonPrimitive?.contentOrNull ?: "unknown",
            indexRelativePath = parsed?.get("indexRelativePath")?.jsonPrimitive?.contentOrNull,
            sidecarRelativePath = parsed?.get("sidecarRelativePath")?.jsonPrimitive?.contentOrNull,
            queryVectorsRelativePath =
                parsed?.get("queryVectorsRelativePath")?.jsonPrimitive?.contentOrNull,
            packHashSha256 = parsed?.get("packHashSha256")?.jsonPrimitive?.contentOrNull ?: "unknown",
            dim = parsed?.get("dim")?.jsonPrimitive?.intOrNull ?: 0,
            bitWidth = parsed?.get("bitWidth")?.jsonPrimitive?.intOrNull ?: 0,
            entryCount = parsed?.get("entryCount")?.jsonPrimitive?.intOrNull ?: 0,
            vectorCount = parsed?.get("vectorCount")?.jsonPrimitive?.intOrNull ?: 0,
            indexBytes = parsed?.get("indexBytes")?.jsonPrimitive?.longOrNull ?: 0,
            loadMs = parsed?.get("loadMs")?.jsonPrimitive?.longOrNull ?: 0,
            queryMs = parsed?.get("queryMs")?.jsonPrimitive?.longOrNull ?: 0,
            rssBeforeKb = parsed?.get("rssBeforeKb")?.jsonPrimitive?.longOrNull ?: 0,
            rssAfterKb = parsed?.get("rssAfterKb")?.jsonPrimitive?.longOrNull ?: 0,
            queryEmbeddingRuntime =
                parsed?.get("queryEmbeddingRuntime")?.jsonPrimitive?.contentOrNull ?: "unknown",
            localAndroidEmbeddingAvailable = localEmbeddingAvailable,
            realIndexGatePassed = realIndexGatePassed,
            queryTimedOut = queryTimedOut,
            cleanupRequested = parsed?.get("cleanupRequested")?.jsonPrimitive?.booleanOrNull ?: false,
            cleanupResult = parsed?.get("cleanupResult")?.jsonPrimitive?.contentOrNull,
            filesExistAfterCleanup = filesExistAfterCleanup,
            runtimePromotion = runtimePromotion,
            activeRetrievalOwnerUnchanged = activeOwnerUnchanged,
            cases = caseSummaries,
            errorMessage = parsed?.get("error")?.jsonPrimitive?.contentOrNull?.let(::sanitizeMessage),
            artifactDirectory = "/sdcard/Download/ChromaLab/runtime/turbovec-knowledge-index-gate/$runId",
        )

        val rustResponseRecord = saveText(
            runId = runId,
            fileName = "turbovec_knowledge_index_gate_response_$runId.json",
            content = rustResponse,
            mimeType = "application/json",
        )
        val summaryJsonRecord = saveText(
            runId = runId,
            fileName = "turbovec_knowledge_index_gate_$runId.json",
            content = json.encodeToString(baseSummary),
            mimeType = "application/json",
        )
        val summaryMarkdownRecord = saveText(
            runId = runId,
            fileName = "turbovec_knowledge_index_gate_$runId.md",
            content = TurboVecKnowledgeIndexGateMarkdownRenderer.render(baseSummary),
            mimeType = "text/markdown",
        )
        val exportRecords = listOf(rustResponseRecord, summaryJsonRecord, summaryMarkdownRecord)
        val withExports = baseSummary.copy(exportRecords = exportRecords)
        Log.i(
            TURBOVEC_KNOWLEDGE_GATE_TAG,
            "runId=${withExports.runId} decision=${withExports.decision} " +
                "status=${withExports.status} realIndex=${withExports.realIndexGatePassed} " +
                "localEmbedding=${withExports.localAndroidEmbeddingAvailable} " +
                "artifacts=${withExports.artifactDirectory}",
        )
        return withExports
    }

    private fun saveText(
        runId: String,
        fileName: String,
        content: String,
        mimeType: String,
    ): TurboVecKnowledgeIndexGateExportRecord {
        val safeName = fileName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
            .ifBlank { "turbovec_knowledge_index_gate.txt" }
        val relativePath =
            "${Environment.DIRECTORY_DOWNLOADS}/ChromaLab/runtime/turbovec-knowledge-index-gate/$runId"
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
                    ?: return TurboVecKnowledgeIndexGateExportRecord(
                        fileName = safeName,
                        success = false,
                        message = "Could not create Downloads entry.",
                    )
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                } ?: return TurboVecKnowledgeIndexGateExportRecord(
                    fileName = safeName,
                    success = false,
                    message = "Could not open Downloads output stream.",
                )
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                TurboVecKnowledgeIndexGateExportRecord(
                    fileName = safeName,
                    success = true,
                    uriOrPath = uri.toString(),
                    message = "Saved to $relativePath/$safeName",
                )
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ChromaLab/runtime/turbovec-knowledge-index-gate/$runId",
                ).apply { mkdirs() }
                val file = File(dir, safeName)
                file.writeText(content, Charsets.UTF_8)
                TurboVecKnowledgeIndexGateExportRecord(
                    fileName = safeName,
                    success = true,
                    uriOrPath = file.absolutePath,
                    message = "Saved to ${file.absolutePath}",
                )
            }
        } catch (e: Exception) {
            TurboVecKnowledgeIndexGateExportRecord(
                fileName = safeName,
                success = false,
                message = sanitizeMessage(e.message ?: "TurboVec Knowledge index gate export failed."),
            )
        }
    }

    private fun sanitizeMessage(message: String): String =
        message
            .replace(Regex("""/data/(data|user/0)/[^\s;:]+"""), "/data/<private>")
            .replace(Regex("""C:/Users/[^\s;:]+""", RegexOption.IGNORE_CASE), "C:/Users/<private>")
            .replace("\"", "'")
}

@Serializable
data class TurboVecKnowledgeIndexGateSummary(
    val runId: String,
    val packageName: String,
    val generatedAtEpochMillis: Long,
    val decision: String,
    val status: String,
    val ffiContract: String,
    val backendId: String,
    val pathClass: String,
    val profileKey: String,
    val modelId: String,
    val indexRelativePath: String? = null,
    val sidecarRelativePath: String? = null,
    val queryVectorsRelativePath: String? = null,
    val packHashSha256: String,
    val dim: Int,
    val bitWidth: Int,
    val entryCount: Int,
    val vectorCount: Int,
    val indexBytes: Long,
    val loadMs: Long,
    val queryMs: Long,
    val rssBeforeKb: Long,
    val rssAfterKb: Long,
    val queryEmbeddingRuntime: String,
    val localAndroidEmbeddingAvailable: Boolean,
    val realIndexGatePassed: Boolean,
    val queryTimedOut: Boolean,
    val cleanupRequested: Boolean,
    val cleanupResult: String? = null,
    val filesExistAfterCleanup: Boolean,
    val runtimePromotion: Boolean,
    val activeRetrievalOwnerUnchanged: Boolean,
    val cases: List<TurboVecKnowledgeIndexGateCaseSummary>,
    val errorMessage: String? = null,
    val artifactDirectory: String,
    val exportRecords: List<TurboVecKnowledgeIndexGateExportRecord> = emptyList(),
)

@Serializable
data class TurboVecKnowledgeIndexGateCaseSummary(
    val queryId: String,
    val status: String,
    val topEntryIds: List<String>,
    val missingRequiredEntryIds: List<String>,
    val forbiddenEntryIdsPresent: List<String>,
    val hit: Boolean,
    val top1Hit: Boolean,
    val allResultsValid: Boolean,
)

@Serializable
data class TurboVecKnowledgeIndexGateExportRecord(
    val fileName: String,
    val success: Boolean,
    val uriOrPath: String? = null,
    val message: String,
)

object TurboVecKnowledgeIndexGateMarkdownRenderer {
    fun render(summary: TurboVecKnowledgeIndexGateSummary): String = buildString {
        appendLine("# TV-8 TurboVec Real Knowledge Index Gate")
        appendLine()
        appendLine("- Run id: `${summary.runId}`")
        appendLine("- Package: `${summary.packageName}`")
        appendLine("- Decision: `${summary.decision}`")
        appendLine("- Status: `${summary.status}`")
        appendLine("- Contract: `${summary.ffiContract}`")
        appendLine("- Backend id: `${summary.backendId}`")
        appendLine("- Path class: `${summary.pathClass}`")
        appendLine("- Profile: `${summary.profileKey}`")
        appendLine("- Model: `${summary.modelId}`")
        appendLine("- Entry count: `${summary.entryCount}`")
        appendLine("- Dimension: `${summary.dim}`")
        appendLine("- Bit width: `${summary.bitWidth}`")
        appendLine("- Index bytes: `${summary.indexBytes}`")
        appendLine("- Load/query ms: `${summary.loadMs}/${summary.queryMs}`")
        appendLine("- RSS before/after KB: `${summary.rssBeforeKb}/${summary.rssAfterKb}`")
        appendLine("- Query embedding runtime: `${summary.queryEmbeddingRuntime}`")
        appendLine("- Local Android embedding available: `${summary.localAndroidEmbeddingAvailable}`")
        appendLine("- Real index gate passed: `${summary.realIndexGatePassed}`")
        appendLine("- Runtime promotion: `${summary.runtimePromotion}`")
        appendLine("- Active retrieval owner unchanged: `${summary.activeRetrievalOwnerUnchanged}`")
        appendLine("- Cleanup result: `${summary.cleanupResult ?: "unknown"}`")
        appendLine("- Files exist after cleanup: `${summary.filesExistAfterCleanup}`")
        summary.errorMessage?.let { appendLine("- Error: `$it`") }
        appendLine()
        appendLine("| Query | Status | Top entry ids | Missing required | Forbidden present |")
        appendLine("|---|---|---|---|---|")
        summary.cases.forEach { case ->
            appendLine(
                "| `${case.queryId}` | `${case.status}` | `${case.topEntryIds.joinToString()}` | " +
                    "`${case.missingRequiredEntryIds.joinToString()}` | " +
                    "`${case.forbiddenEntryIdsPresent.joinToString()}` |",
            )
        }
        appendLine()
        appendLine("This gate verifies real Knowledge Pack MiniLM TurboVec index load/query only.")
        appendLine("It does not promote TurboVec into the active Knowledge retrieval owner.")
        appendLine("It does not affect chromatogram analysis, report gates, or CalculationEngine math.")
    }
}

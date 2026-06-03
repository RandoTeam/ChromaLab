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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val RUST_AXIS_ELEMENT_CORPUS_TAG = "ChromaLabRustAxisCorpus"
private const val AXIS_ELEMENT_CROP_CORPUS_CONTRACT = "DR2F_AXIS_ELEMENT_CROP_PLAN_V1"
private const val CORPUS_MANIFEST_ASSET = "validation/rust_axis_element/corpus_manifest.json"

class RustAxisElementCropCorpusDiagnostics(
    private val context: Context,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun run(): RustAxisElementCropCorpusSummary {
        val runId = "rust_axis_element_corpus_${System.currentTimeMillis()}"
        val manifest = context.assets.open(CORPUS_MANIFEST_ASSET).bufferedReader().use { reader ->
            json.decodeFromString<RustAxisElementCropCorpusManifest>(
                reader.readText().removePrefix("\uFEFF"),
            )
        }
        val results = manifest.items.map { item ->
            runItem(item)
        }
        val passCount = results.count { it.decision == "PASS" }
        val failCount = results.size - passCount
        val baseSummary = RustAxisElementCropCorpusSummary(
            runId = runId,
            packageName = context.packageName,
            generatedAtEpochMillis = System.currentTimeMillis(),
            suiteId = manifest.suiteId,
            sourceRoot = manifest.sourceRoot,
            itemCount = results.size,
            passCount = passCount,
            failCount = failCount,
            decision = if (results.isNotEmpty() && failCount == 0) "PASS" else "FAIL",
            artifactDirectory = "/sdcard/Download/ChromaLab/runtime/rust-axis-element-corpus/$runId",
            results = results,
        )

        val summaryJsonRecord = saveText(
            runId = runId,
            fileName = "rust_axis_element_corpus_$runId.json",
            content = json.encodeToString(baseSummary),
            mimeType = "application/json",
        )
        val summaryMarkdownRecord = saveText(
            runId = runId,
            fileName = "rust_axis_element_corpus_$runId.md",
            content = RustAxisElementCropCorpusMarkdownRenderer.render(baseSummary),
            mimeType = "text/markdown",
        )
        val exportRecords = listOf(summaryJsonRecord, summaryMarkdownRecord)
        val withExports = baseSummary.copy(
            exportRecords = exportRecords,
            decision = if (baseSummary.decision == "PASS" && exportRecords.all { it.success }) {
                "PASS"
            } else {
                "FAIL"
            },
        )
        Log.i(
            RUST_AXIS_ELEMENT_CORPUS_TAG,
            "runId=${withExports.runId} decision=${withExports.decision} " +
                "items=${withExports.itemCount} pass=${withExports.passCount} " +
                "fail=${withExports.failCount} artifacts=${withExports.artifactDirectory}",
        )
        return withExports
    }

    private fun runItem(item: RustAxisElementCropCorpusItem): RustAxisElementCropCorpusItemResult {
        val graphJson = context.assets.open(item.assetGraphPath).bufferedReader().use { reader ->
            reader.readText()
        }
        val rustResponse = runCatching {
            RustCvBridge.planAxisElementCropsJson(
                imageWidth = item.imageWidth,
                imageHeight = item.imageHeight,
                axisElementGraphJson = graphJson,
            )
        }.getOrElse { error ->
            """{"status":"ERROR","ffiContract":"$AXIS_ELEMENT_CROP_CORPUS_CONTRACT","error":"${sanitizeMessage(error.message ?: "Rust JNI call failed.")}"}"""
        }
        val parsed = runCatching { Json.parseToJsonElement(rustResponse).jsonObject }.getOrNull()
        val report = parsed?.get("report")?.jsonObject
        val cropPlan = report?.get("crop_plan")?.jsonObject
        val acceptedCrops = cropPlan?.get("accepted")?.jsonArray ?: JsonArray(emptyList())
        val rejectedCrops = cropPlan?.get("rejected")?.jsonArray ?: JsonArray(emptyList())
        val rustStatus = parsed?.get("status")?.jsonPrimitive?.contentOrNull ?: "ERROR"
        val contract = parsed?.get("ffiContract")?.jsonPrimitive?.contentOrNull ?: "unknown"
        val graphIndex = report?.get("graph_index")?.jsonPrimitive?.intOrNull
        val sourceBandCount = report?.get("source_label_band_count")?.jsonPrimitive?.intOrNull ?: 0
        val acceptedCropSignature = cropSignature(acceptedCrops)
        val rejectedCropSignature = cropSignature(rejectedCrops)
        val accepted = rustStatus == "OK" &&
            contract == AXIS_ELEMENT_CROP_CORPUS_CONTRACT &&
            graphIndex == item.expectedGraphIndex &&
            sourceBandCount == item.expectedSourceLabelBandCount &&
            acceptedCrops.size == item.expectedAcceptedCropCount
        return RustAxisElementCropCorpusItemResult(
            fixtureId = item.fixtureId,
            graphId = item.graphId,
            assetGraphPath = item.assetGraphPath,
            imageWidth = item.imageWidth,
            imageHeight = item.imageHeight,
            rustStatus = rustStatus,
            ffiContract = contract,
            graphIndex = graphIndex,
            expectedGraphIndex = item.expectedGraphIndex,
            sourceLabelBandCount = sourceBandCount,
            expectedSourceLabelBandCount = item.expectedSourceLabelBandCount,
            acceptedCropCount = acceptedCrops.size,
            expectedAcceptedCropCount = item.expectedAcceptedCropCount,
            rejectedCropCount = rejectedCrops.size,
            cropVariantCount = acceptedCrops.sumOf { crop ->
                crop.jsonObject["variants"]?.jsonArray?.size ?: 0
            },
            acceptedCropSignature = acceptedCropSignature,
            rejectedCropSignature = rejectedCropSignature,
            errorMessage = parsed?.get("error")?.jsonPrimitive?.contentOrNull?.let(::sanitizeMessage),
            decision = if (accepted) "PASS" else "FAIL",
        )
    }

    private fun cropSignature(crops: JsonArray): String =
        crops.joinToString(separator = "|") { crop ->
            val cropObject = crop.jsonObject
            val bandKind = cropObject["band_kind"]?.jsonPrimitive?.contentOrNull ?: "unknown"
            val rect = cropObject["clamped_rect"]?.jsonObject ?: JsonObject(emptyMap())
            val variants = cropObject["variants"]?.jsonArray
                ?.joinToString(separator = "+") { it.jsonPrimitive.contentOrNull ?: "unknown" }
                ?: "none"
            val x = rect["x"]?.jsonPrimitive?.intOrNull ?: -1
            val y = rect["y"]?.jsonPrimitive?.intOrNull ?: -1
            val width = rect["width"]?.jsonPrimitive?.intOrNull ?: -1
            val height = rect["height"]?.jsonPrimitive?.intOrNull ?: -1
            "$bandKind:$x,$y,$width,$height:$variants"
        }

    private fun saveText(
        runId: String,
        fileName: String,
        content: String,
        mimeType: String,
    ): RustAxisElementCropCorpusExportRecord {
        val safeName = fileName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
            .ifBlank { "rust_axis_element_corpus.txt" }
        val relativePath =
            "${Environment.DIRECTORY_DOWNLOADS}/ChromaLab/runtime/rust-axis-element-corpus/$runId"
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
                    ?: return RustAxisElementCropCorpusExportRecord(
                        fileName = safeName,
                        success = false,
                        message = "Could not create Downloads entry.",
                    )
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                } ?: return RustAxisElementCropCorpusExportRecord(
                    fileName = safeName,
                    success = false,
                    message = "Could not open Downloads output stream.",
                )
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                RustAxisElementCropCorpusExportRecord(
                    fileName = safeName,
                    success = true,
                    uriOrPath = uri.toString(),
                    message = "Saved to $relativePath/$safeName",
                )
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ChromaLab/runtime/rust-axis-element-corpus/$runId",
                ).apply { mkdirs() }
                val file = File(dir, safeName)
                file.writeText(content, Charsets.UTF_8)
                RustAxisElementCropCorpusExportRecord(
                    fileName = safeName,
                    success = true,
                    uriOrPath = file.absolutePath,
                    message = "Saved to ${file.absolutePath}",
                )
            }
        } catch (e: Exception) {
            RustAxisElementCropCorpusExportRecord(
                fileName = safeName,
                success = false,
                message = sanitizeMessage(e.message ?: "Rust axis element corpus export failed."),
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
data class RustAxisElementCropCorpusManifest(
    val suiteId: String,
    val sourceRoot: String,
    val itemCount: Int,
    val items: List<RustAxisElementCropCorpusItem>,
)

@Serializable
data class RustAxisElementCropCorpusItem(
    val fixtureId: String,
    val graphId: String,
    val assetGraphPath: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val expectedGraphIndex: Int,
    val expectedSourceLabelBandCount: Int,
    val expectedAcceptedCropCount: Int,
    val source: String,
)

@Serializable
data class RustAxisElementCropCorpusSummary(
    val runId: String,
    val packageName: String,
    val generatedAtEpochMillis: Long,
    val suiteId: String,
    val sourceRoot: String,
    val itemCount: Int,
    val passCount: Int,
    val failCount: Int,
    val decision: String,
    val artifactDirectory: String,
    val results: List<RustAxisElementCropCorpusItemResult>,
    val exportRecords: List<RustAxisElementCropCorpusExportRecord> = emptyList(),
)

@Serializable
data class RustAxisElementCropCorpusItemResult(
    val fixtureId: String,
    val graphId: String,
    val assetGraphPath: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val rustStatus: String,
    val ffiContract: String,
    val graphIndex: Int?,
    val expectedGraphIndex: Int,
    val sourceLabelBandCount: Int,
    val expectedSourceLabelBandCount: Int,
    val acceptedCropCount: Int,
    val expectedAcceptedCropCount: Int,
    val rejectedCropCount: Int,
    val cropVariantCount: Int,
    val acceptedCropSignature: String,
    val rejectedCropSignature: String,
    val errorMessage: String? = null,
    val decision: String,
)

@Serializable
data class RustAxisElementCropCorpusExportRecord(
    val fileName: String,
    val success: Boolean,
    val uriOrPath: String? = null,
    val message: String,
)

object RustAxisElementCropCorpusMarkdownRenderer {
    fun render(summary: RustAxisElementCropCorpusSummary): String = buildString {
        appendLine("# Rust Axis Element Crop Corpus Parity")
        appendLine()
        appendLine("- Run id: `${summary.runId}`")
        appendLine("- Package: `${summary.packageName}`")
        appendLine("- Suite: `${summary.suiteId}`")
        appendLine("- Source root: `${summary.sourceRoot}`")
        appendLine("- Decision: `${summary.decision}`")
        appendLine("- Items: `${summary.itemCount}`")
        appendLine("- Pass: `${summary.passCount}`")
        appendLine("- Fail: `${summary.failCount}`")
        appendLine()
        appendLine("| Fixture | Graph | Status | Bands | Accepted | Rejected | Decision |")
        appendLine("|---|---:|---|---:|---:|---:|---|")
        summary.results.forEach { result ->
            appendLine(
                "| `${result.fixtureId}` | `${result.graphId}` | `${result.rustStatus}` | " +
                    "${result.sourceLabelBandCount} | ${result.acceptedCropCount} | " +
                    "${result.rejectedCropCount} | `${result.decision}` |",
            )
        }
        appendLine()
        appendLine("This suite compares the real DR-1R axis-element corpus through Android Rust JNI.")
        appendLine("Rust remains crop-planning-only: no calibration, peak metrics, report gates, or CalculationEngine math are delegated.")
    }
}

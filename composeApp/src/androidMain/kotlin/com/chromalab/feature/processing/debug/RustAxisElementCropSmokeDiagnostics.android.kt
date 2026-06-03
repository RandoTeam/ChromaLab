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
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val RUST_AXIS_ELEMENT_SMOKE_TAG = "ChromaLabRustAxis"
private const val AXIS_ELEMENT_CROP_CONTRACT = "DR2F_AXIS_ELEMENT_CROP_PLAN_V1"
private const val FIXTURE_ID = "white_tiger_ion71"
private const val FIXTURE_GRAPH_ASSET =
    "validation/rust_axis_element/white_tiger_ion71_axis_element_graph.json"
private const val IMAGE_WIDTH = 576
private const val IMAGE_HEIGHT = 1280
private const val EXPECTED_GRAPH_INDEX = 1
private const val EXPECTED_SOURCE_LABEL_BAND_COUNT = 3

class RustAxisElementCropSmokeDiagnostics(
    private val context: Context,
) {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun run(): RustAxisElementCropSmokeSummary {
        val runId = "rust_axis_element_crops_${System.currentTimeMillis()}"
        val graphJson = context.assets.open(FIXTURE_GRAPH_ASSET).bufferedReader().use { reader ->
            reader.readText()
        }
        val rustResponse = runCatching {
            RustCvBridge.planAxisElementCropsJson(
                imageWidth = IMAGE_WIDTH,
                imageHeight = IMAGE_HEIGHT,
                axisElementGraphJson = graphJson,
            )
        }.getOrElse { error ->
            """{"status":"ERROR","ffiContract":"$AXIS_ELEMENT_CROP_CONTRACT","error":"${sanitizeMessage(error.message ?: "Rust JNI call failed.")}"}"""
        }
        val parsed = runCatching {
            Json.parseToJsonElement(rustResponse).jsonObject
        }.getOrNull()
        val report = parsed?.get("report")?.jsonObject
        val cropPlan = report?.get("crop_plan")?.jsonObject
        val acceptedCrops = cropPlan?.get("accepted")?.jsonArray ?: emptyList()
        val rejectedCrops = cropPlan?.get("rejected")?.jsonArray ?: emptyList()
        val variantCount = acceptedCrops.sumOf { crop ->
            crop.jsonObject["variants"]?.jsonArray?.size ?: 0
        }
        val rustStatus = parsed?.get("status")?.jsonPrimitive?.contentOrNull ?: "ERROR"
        val contract = parsed?.get("ffiContract")?.jsonPrimitive?.contentOrNull ?: "unknown"
        val graphIndex = report?.get("graph_index")?.jsonPrimitive?.intOrNull
        val sourceBandCount = report?.get("source_label_band_count")?.jsonPrimitive?.intOrNull ?: 0
        val errorMessage = parsed?.get("error")?.jsonPrimitive?.contentOrNull?.let(::sanitizeMessage)
        val rustAccepted = rustStatus == "OK" &&
            contract == AXIS_ELEMENT_CROP_CONTRACT &&
            graphIndex == EXPECTED_GRAPH_INDEX &&
            sourceBandCount == EXPECTED_SOURCE_LABEL_BAND_COUNT &&
            acceptedCrops.isNotEmpty()
        val baseSummary = RustAxisElementCropSmokeSummary(
            runId = runId,
            packageName = context.packageName,
            generatedAtEpochMillis = System.currentTimeMillis(),
            fixtureId = FIXTURE_ID,
            graphAssetPath = FIXTURE_GRAPH_ASSET,
            imageWidth = IMAGE_WIDTH,
            imageHeight = IMAGE_HEIGHT,
            rustStatus = rustStatus,
            ffiContract = contract,
            graphIndex = graphIndex,
            sourceLabelBandCount = sourceBandCount,
            acceptedCropCount = acceptedCrops.size,
            rejectedCropCount = rejectedCrops.size,
            cropVariantCount = variantCount,
            errorMessage = errorMessage,
            accepted = rustAccepted,
            decision = if (rustAccepted) "PASS" else "FAIL",
            artifactDirectory = "/sdcard/Download/ChromaLab/runtime/rust-axis-element-crops/$runId",
        )

        val rustResponseRecord = saveText(
            runId = runId,
            fileName = "rust_axis_element_crop_response_$runId.json",
            content = rustResponse,
            mimeType = "application/json",
        )
        val summaryJsonRecord = saveText(
            runId = runId,
            fileName = "rust_axis_element_crop_smoke_$runId.json",
            content = json.encodeToString(baseSummary),
            mimeType = "application/json",
        )
        val summaryMarkdownRecord = saveText(
            runId = runId,
            fileName = "rust_axis_element_crop_smoke_$runId.md",
            content = RustAxisElementCropSmokeMarkdownRenderer.render(baseSummary),
            mimeType = "text/markdown",
        )
        val exportRecords = listOf(rustResponseRecord, summaryJsonRecord, summaryMarkdownRecord)
        val withExports = baseSummary.copy(
            exportRecords = exportRecords,
            accepted = baseSummary.accepted && exportRecords.all { it.success },
            decision = if (baseSummary.accepted && exportRecords.all { it.success }) "PASS" else "FAIL",
        )
        Log.i(
            RUST_AXIS_ELEMENT_SMOKE_TAG,
            "runId=${withExports.runId} decision=${withExports.decision} " +
                "rustStatus=${withExports.rustStatus} accepted=${withExports.acceptedCropCount} " +
                "rejected=${withExports.rejectedCropCount} artifacts=${withExports.artifactDirectory}",
        )
        return withExports
    }

    private fun saveText(
        runId: String,
        fileName: String,
        content: String,
        mimeType: String,
    ): RustAxisElementCropSmokeExportRecord {
        val safeName = fileName.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
            .ifBlank { "rust_axis_element_crop_smoke.txt" }
        val relativePath =
            "${Environment.DIRECTORY_DOWNLOADS}/ChromaLab/runtime/rust-axis-element-crops/$runId"
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
                    ?: return RustAxisElementCropSmokeExportRecord(
                        fileName = safeName,
                        success = false,
                        message = "Could not create Downloads entry.",
                    )
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                } ?: return RustAxisElementCropSmokeExportRecord(
                    fileName = safeName,
                    success = false,
                    message = "Could not open Downloads output stream.",
                )
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                RustAxisElementCropSmokeExportRecord(
                    fileName = safeName,
                    success = true,
                    uriOrPath = uri.toString(),
                    message = "Saved to $relativePath/$safeName",
                )
            } else {
                @Suppress("DEPRECATION")
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ChromaLab/runtime/rust-axis-element-crops/$runId",
                ).apply { mkdirs() }
                val file = File(dir, safeName)
                file.writeText(content, Charsets.UTF_8)
                RustAxisElementCropSmokeExportRecord(
                    fileName = safeName,
                    success = true,
                    uriOrPath = file.absolutePath,
                    message = "Saved to ${file.absolutePath}",
                )
            }
        } catch (e: Exception) {
            RustAxisElementCropSmokeExportRecord(
                fileName = safeName,
                success = false,
                message = sanitizeMessage(e.message ?: "Rust axis element crop smoke export failed."),
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
data class RustAxisElementCropSmokeSummary(
    val runId: String,
    val packageName: String,
    val generatedAtEpochMillis: Long,
    val fixtureId: String,
    val graphAssetPath: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val rustStatus: String,
    val ffiContract: String,
    val graphIndex: Int?,
    val sourceLabelBandCount: Int,
    val acceptedCropCount: Int,
    val rejectedCropCount: Int,
    val cropVariantCount: Int,
    val errorMessage: String? = null,
    val accepted: Boolean,
    val decision: String,
    val artifactDirectory: String,
    val exportRecords: List<RustAxisElementCropSmokeExportRecord> = emptyList(),
)

@Serializable
data class RustAxisElementCropSmokeExportRecord(
    val fileName: String,
    val success: Boolean,
    val uriOrPath: String? = null,
    val message: String,
)

object RustAxisElementCropSmokeMarkdownRenderer {
    fun render(summary: RustAxisElementCropSmokeSummary): String = buildString {
        appendLine("# Rust Axis Element Crop Smoke Check")
        appendLine()
        appendLine("- Run id: `${summary.runId}`")
        appendLine("- Package: `${summary.packageName}`")
        appendLine("- Fixture: `${summary.fixtureId}`")
        appendLine("- Graph asset: `${summary.graphAssetPath}`")
        appendLine("- Image: `${summary.imageWidth}x${summary.imageHeight}`")
        appendLine("- Decision: `${summary.decision}`")
        appendLine("- Accepted: `${summary.accepted}`")
        appendLine("- Rust status: `${summary.rustStatus}`")
        appendLine("- Contract: `${summary.ffiContract}`")
        appendLine("- Graph index: `${summary.graphIndex ?: "unknown"}`")
        appendLine("- Source label bands: `${summary.sourceLabelBandCount}`")
        appendLine("- Accepted crops: `${summary.acceptedCropCount}`")
        appendLine("- Rejected crops: `${summary.rejectedCropCount}`")
        appendLine("- Crop variants: `${summary.cropVariantCount}`")
        summary.errorMessage?.let { appendLine("- Error: `$it`") }
        appendLine()
        appendLine("This check verifies the Android JNI transfer of the DR-1R axis-element crop-planning block.")
        appendLine("Rust remains crop-planning-only here: no calibration, peak metrics, report gates, or CalculationEngine math are delegated.")
    }
}

package com.chromalab.feature.processing.inference

import kotlinx.serialization.Serializable

@Serializable
enum class GgufMtmdFitStatus {
    FITS_CONTEXT,
    EXCEEDS_CONTEXT,
    UNKNOWN,
}

@Serializable
enum class GgufMtmdResearchGateDecision {
    ADVISORY_DIAGNOSTICS_ALLOWED,
    RESEARCH_ONLY,
    BLOCKED_PRECONDITION,
}

@Serializable
data class GgufMtmdChunkDiagnostic(
    val index: Int,
    val type: String,
    val tokenCount: Int,
    val positionCount: Int,
    val id: String? = null,
)

@Serializable
data class GgufMtmdNativeProbeResult(
    val available: Boolean,
    val supportsVision: Boolean = false,
    val supportsAudio: Boolean = false,
    val usesMrope: Boolean = false,
    val promptChars: Int = 0,
    val mediaMarkerCount: Int = 0,
    val chunkCount: Int = 0,
    val imageChunkCount: Int = 0,
    val textChunkCount: Int = 0,
    val audioChunkCount: Int = 0,
    val imageTokenCount: Int = 0,
    val textTokenCount: Int = 0,
    val audioTokenCount: Int = 0,
    val totalTokenCount: Int = 0,
    val totalPositionCount: Int = 0,
    val contextTokens: Int = 0,
    val batchTokens: Int = 0,
    val fitStatus: GgufMtmdFitStatus = GgufMtmdFitStatus.UNKNOWN,
    val bitmapLoadMillis: Long? = null,
    val tokenizeMillis: Long? = null,
    val error: String? = null,
    val chunks: List<GgufMtmdChunkDiagnostic> = emptyList(),
)

@Serializable
data class GgufMtmdModelFileDiagnostic(
    val role: String,
    val fileName: String? = null,
    val exists: Boolean = false,
    val sizeBytes: Long? = null,
    val pathClass: String = "UNKNOWN",
)

@Serializable
data class GgufMtmdCropOcrProbe(
    val attempted: Boolean,
    val latencyMillis: Long? = null,
    val outputChars: Int = 0,
    val advisoryOnly: Boolean = true,
    val forbiddenNumericFieldDetected: Boolean = false,
    val forbiddenNumericFields: List<String> = emptyList(),
    val failureReason: String? = null,
)

@Serializable
data class GgufMtmdOcrResearchGate(
    val modelId: String,
    val modelAvailable: Boolean,
    val mmprojAvailable: Boolean,
    val expectedBaseFileName: String? = null,
    val expectedMmprojFileName: String? = null,
    val expectedDownloadBytes: Long? = null,
    val compatibility: String,
    val decision: GgufMtmdResearchGateDecision,
    val safetyBoundaries: List<String>,
    val sources: List<String> = emptyList(),
)

@Serializable
data class GgufMtmdDiagnosticsSummary(
    val schemaVersion: String = "gguf-mtmd-diagnostics-1.0",
    val runId: String,
    val generatedAtEpochMillis: Long,
    val modelId: String?,
    val modelFamily: String? = null,
    val backend: String? = null,
    val contextTokens: Int? = null,
    val batchTokens: Int? = null,
    val imagePathClass: String = "NOT_AVAILABLE",
    val promptChars: Int = 0,
    val loadAttempted: Boolean = false,
    val loadResult: String? = null,
    val loadTimeMillis: Long? = null,
    val baseModel: GgufMtmdModelFileDiagnostic? = null,
    val mmproj: GgufMtmdModelFileDiagnostic? = null,
    val nativeProbe: GgufMtmdNativeProbeResult? = null,
    val cropOcrProbe: GgufMtmdCropOcrProbe? = null,
    val ocrResearchGate: GgufMtmdOcrResearchGate,
    val gateDecision: GgufMtmdResearchGateDecision,
    val gateReasons: List<String>,
    val exportPrivacyClass: String = "TECHNICAL_EVIDENCE",
    val notes: List<String> = emptyList(),
)

data class GgufMtmdAdvisoryPolicyResult(
    val advisoryOnly: Boolean,
    val forbiddenNumericFieldDetected: Boolean,
    val forbiddenNumericFields: List<String>,
)

object GgufMtmdOcrAdvisoryPolicy {
    val forbiddenNumericAuthorityFields: Set<String> = setOf(
        "rt",
        "retention_time",
        "height",
        "area",
        "area_percent",
        "fwhm",
        "sn",
        "s_n",
        "baseline",
        "kovats",
        "calibration_coefficient",
        "calibration_coefficients",
        "x_pixel",
        "y_pixel",
        "graph_count",
        "peak_count",
    )

    fun evaluate(rawText: String): GgufMtmdAdvisoryPolicyResult {
        val normalized = rawText.lowercase()
        val detected = forbiddenNumericAuthorityFields
            .filter { field ->
                Regex("""["']?\b${Regex.escape(field)}\b["']?\s*[:=]""").containsMatchIn(normalized)
            }
            .sorted()
        return GgufMtmdAdvisoryPolicyResult(
            advisoryOnly = true,
            forbiddenNumericFieldDetected = detected.isNotEmpty(),
            forbiddenNumericFields = detected,
        )
    }
}

object GgufMtmdDiagnosticsGateEvaluator {
    fun evaluate(summary: GgufMtmdDiagnosticsSummary): GgufMtmdResearchGateDecision =
        when {
            summary.loadResult != null && summary.loadResult != "loaded" ->
                GgufMtmdResearchGateDecision.BLOCKED_PRECONDITION
            summary.nativeProbe?.available == true ->
                GgufMtmdResearchGateDecision.ADVISORY_DIAGNOSTICS_ALLOWED
            else -> GgufMtmdResearchGateDecision.RESEARCH_ONLY
        }
}

object GgufMtmdDiagnosticsMarkdownRenderer {
    fun render(summary: GgufMtmdDiagnosticsSummary): String = buildString {
        appendLine("# GGUF mtmd Multimodal Diagnostics Gate")
        appendLine()
        appendLine("- Run id: `${summary.runId}`")
        appendLine("- Model: `${summary.modelId ?: "not_resolved"}`")
        appendLine("- Family: `${summary.modelFamily ?: "-"}`")
        appendLine("- Backend: `${summary.backend ?: "-"}`")
        appendLine("- Gate decision: `${summary.gateDecision.name}`")
        appendLine("- Privacy class: `${summary.exportPrivacyClass}`")
        appendLine()
        appendLine("## Model Files")
        appendLine()
        appendLine("| Role | File | Exists | Bytes | Path class |")
        appendLine("| --- | --- | ---: | ---: | --- |")
        listOfNotNull(summary.baseModel, summary.mmproj).forEach { file ->
            appendLine(
                "| ${file.role} | `${file.fileName ?: "-"}` | ${file.exists} | " +
                    "${file.sizeBytes ?: "-"} | `${file.pathClass}` |",
            )
        }
        appendLine()
        appendLine("## mtmd Probe")
        val probe = summary.nativeProbe
        if (probe == null) {
            appendLine()
            appendLine("- Probe not run.")
        } else {
            appendLine()
            appendLine("- Available: `${probe.available}`")
            appendLine("- Vision/audio/mrope: `${probe.supportsVision}` / `${probe.supportsAudio}` / `${probe.usesMrope}`")
            appendLine("- Image tokens: `${probe.imageTokenCount}`")
            appendLine("- Total tokens/positions: `${probe.totalTokenCount}` / `${probe.totalPositionCount}`")
            appendLine("- Context/batch: `${probe.contextTokens}` / `${probe.batchTokens}`")
            appendLine("- Fit status: `${probe.fitStatus.name}`")
            appendLine("- Bitmap/tokenize ms: `${probe.bitmapLoadMillis ?: "-"}` / `${probe.tokenizeMillis ?: "-"}`")
            appendLine("- Error: `${probe.error ?: "-"}`")
            appendLine()
            appendLine("| Chunk | Type | Tokens | Positions | Id |")
            appendLine("| ---: | --- | ---: | ---: | --- |")
            probe.chunks.forEach { chunk ->
                appendLine(
                    "| ${chunk.index} | `${chunk.type}` | ${chunk.tokenCount} | " +
                        "${chunk.positionCount} | `${chunk.id ?: "-"}` |",
                )
            }
        }
        appendLine()
        appendLine("## Crop OCR Probe")
        val ocr = summary.cropOcrProbe
        if (ocr == null) {
            appendLine()
            appendLine("- OCR probe not run.")
        } else {
            appendLine()
            appendLine("- Attempted: `${ocr.attempted}`")
            appendLine("- Latency ms: `${ocr.latencyMillis ?: "-"}`")
            appendLine("- Output chars: `${ocr.outputChars}`")
            appendLine("- Advisory only: `${ocr.advisoryOnly}`")
            appendLine("- Forbidden numeric fields detected: `${ocr.forbiddenNumericFieldDetected}`")
            appendLine("- Failure: `${ocr.failureReason ?: "-"}`")
        }
        appendLine()
        appendLine("## OCR Research Gate")
        appendLine()
        appendLine("- Candidate: `${summary.ocrResearchGate.modelId}`")
        appendLine("- Model/mmproj available: `${summary.ocrResearchGate.modelAvailable}` / `${summary.ocrResearchGate.mmprojAvailable}`")
        appendLine("- Expected download bytes: `${summary.ocrResearchGate.expectedDownloadBytes ?: "-"}`")
        appendLine("- Compatibility: `${summary.ocrResearchGate.compatibility}`")
        appendLine("- Decision: `${summary.ocrResearchGate.decision.name}`")
        appendLine()
        appendLine("## Safety Boundaries")
        summary.ocrResearchGate.safetyBoundaries.forEach { appendLine("- $it") }
        if (summary.gateReasons.isNotEmpty()) {
            appendLine()
            appendLine("## Gate Reasons")
            summary.gateReasons.forEach { appendLine("- `$it`") }
        }
        if (summary.notes.isNotEmpty()) {
            appendLine()
            appendLine("## Notes")
            summary.notes.forEach { appendLine("- $it") }
        }
    }
}

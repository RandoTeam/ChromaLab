package com.chromalab.feature.processing.debug

import com.chromalab.feature.processing.inference.LiteRtRuntimeDiagnostics
import com.chromalab.feature.processing.inference.GgufMtpBenchmarkMode
import com.chromalab.feature.processing.inference.GgufMtpBenchmarkSummary
import com.chromalab.feature.processing.model.ModelAvailabilityDiagnostic
import com.chromalab.feature.reports.ReportExportPrivacyClass
import kotlinx.serialization.Serializable

@Serializable
enum class RuntimeDiagnosticSource {
    MODEL_DISCOVERY,
    LITERT_LM,
    LLAMA_CPP,
    MTMD_MMPROJ,
    GGUF_MTP_TEXT_ONLY,
    VULKAN_PREFLIGHT,
    UNKNOWN,
}

@Serializable
enum class RuntimeModelPathClass {
    APP_PRIVATE_MODEL,
    VALIDATION_PACKAGE_PRIVATE_MODEL,
    PUBLIC_DOWNLOAD_EXPORT,
    USER_SELECTED_EXTERNAL,
    NOT_AVAILABLE,
    UNKNOWN,
}

@Serializable
data class StructuredRuntimeDiagnostic(
    val diagnosticId: String,
    val source: RuntimeDiagnosticSource,
    val modelId: String? = null,
    val modelPathClass: RuntimeModelPathClass = RuntimeModelPathClass.UNKNOWN,
    val backend: String? = null,
    val loadAttempted: Boolean = false,
    val loadResult: String? = null,
    val loadTimeMillis: Long? = null,
    val firstResponseLatencyMillis: Long? = null,
    val totalResponseDurationMillis: Long? = null,
    val timeoutMillis: Long? = null,
    val timedOut: Boolean = false,
    val fallbackReason: String? = null,
    val modelSupportsMtp: String? = null,
    val runtimeExposesMtp: String? = null,
    val mtpEnabled: String? = null,
    val privacyClass: ReportExportPrivacyClass = ReportExportPrivacyClass.TECHNICAL_EVIDENCE,
    val safeUserReportSummary: String? = null,
)

object StructuredRuntimeDiagnosticMapper {
    fun fromModelAvailability(
        diagnostics: List<ModelAvailabilityDiagnostic>,
    ): List<StructuredRuntimeDiagnostic> =
        diagnostics.map { diagnostic ->
            StructuredRuntimeDiagnostic(
                diagnosticId = "runtime:${diagnostic.diagnosticId}",
                source = RuntimeDiagnosticSource.MODEL_DISCOVERY,
                modelId = diagnostic.executedModelId ?: diagnostic.selectedModelId,
                modelPathClass = classifyModelPath(diagnostic.expectedPath),
                backend = diagnostic.expectedBackend,
                loadAttempted = diagnostic.loadAttempted,
                loadResult = diagnostic.loadResult ?: diagnostic.status.name,
                timeoutMillis = null,
                timedOut = diagnostic.status.name == "TIMEOUT",
                fallbackReason = diagnostic.fallbackResult,
                privacyClass = ReportExportPrivacyClass.TECHNICAL_EVIDENCE,
                safeUserReportSummary = buildAvailabilitySummary(diagnostic),
            )
        }

    fun fromLiteRt(
        modelId: String?,
        diagnostics: LiteRtRuntimeDiagnostics,
        modelPath: String? = null,
    ): StructuredRuntimeDiagnostic =
        StructuredRuntimeDiagnostic(
            diagnosticId = "runtime:litert:${modelId ?: "unknown"}",
            source = RuntimeDiagnosticSource.LITERT_LM,
            modelId = modelId,
            modelPathClass = classifyModelPath(modelPath),
            backend = diagnostics.backendName,
            loadAttempted = diagnostics.performance.loadTimeMillis != null,
            loadResult = if (diagnostics.performance.loadTimeMillis != null) "loaded" else null,
            loadTimeMillis = diagnostics.performance.loadTimeMillis,
            firstResponseLatencyMillis = diagnostics.performance.firstResponseLatencyMillis,
            totalResponseDurationMillis = diagnostics.performance.totalResponseDurationMillis,
            timeoutMillis = diagnostics.performance.timeoutMillis,
            timedOut = diagnostics.performance.timedOut,
            fallbackReason = diagnostics.mtpCapability.reason,
            modelSupportsMtp = diagnostics.mtpCapability.modelSupportsMtp.name,
            runtimeExposesMtp = diagnostics.mtpCapability.runtimeExposesMtp.name,
            mtpEnabled = diagnostics.mtpCapability.mtpEnabled.name,
            privacyClass = ReportExportPrivacyClass.TECHNICAL_EVIDENCE,
            safeUserReportSummary = "LiteRT runtime ${diagnostics.backendName ?: "unknown backend"}; MTP ${diagnostics.mtpCapability.mtpEnabled.name}.",
        )

    fun fromGgufMtpBenchmark(summary: GgufMtpBenchmarkSummary): List<StructuredRuntimeDiagnostic> =
        summary.passes.map { pass ->
            StructuredRuntimeDiagnostic(
                diagnosticId = "runtime:gguf_mtp:${summary.runId}:${pass.label}",
                source = RuntimeDiagnosticSource.GGUF_MTP_TEXT_ONLY,
                modelId = pass.modelId,
                modelPathClass = runCatching { RuntimeModelPathClass.valueOf(pass.modelPathClass) }
                    .getOrDefault(RuntimeModelPathClass.UNKNOWN),
                backend = "llama.cpp ${pass.backend.name}",
                loadAttempted = true,
                loadResult = pass.failureReason ?: "completed",
                loadTimeMillis = pass.loadTimeMillis,
                firstResponseLatencyMillis = pass.firstTokenLatencyMillis,
                totalResponseDurationMillis = pass.totalResponseDurationMillis,
                timeoutMillis = pass.timeoutMillis,
                timedOut = pass.timedOut,
                fallbackReason = summary.gate.verdict,
                modelSupportsMtp = summary.modelSupportsMtp.toString(),
                runtimeExposesMtp = "TEXT_ONLY_BENCHMARK",
                mtpEnabled = if (pass.mode == GgufMtpBenchmarkMode.DRAFT_MTP && pass.mtpDraftTokens > 0) {
                    "ENABLED_DRAFT_${pass.mtpDraftTokens}"
                } else {
                    "DISABLED"
                },
                privacyClass = ReportExportPrivacyClass.TECHNICAL_EVIDENCE,
                safeUserReportSummary = "GGUF text-only ${pass.label}: ${summary.gate.decision.name}.",
            )
        }

    fun classifyModelPath(path: String?): RuntimeModelPathClass {
        val value = path?.trim()?.takeIf { it.isNotEmpty() } ?: return RuntimeModelPathClass.NOT_AVAILABLE
        val normalized = value.replace('\\', '/').lowercase()
        return when {
            "/data/data/com.chromalab.app.validation/" in normalized ||
                "/data/user/0/com.chromalab.app.validation/" in normalized ->
                RuntimeModelPathClass.VALIDATION_PACKAGE_PRIVATE_MODEL
            "/data/data/" in normalized || "/data/user/0/" in normalized || "/files/models/" in normalized ->
                RuntimeModelPathClass.APP_PRIVATE_MODEL
            "/download/chromalab/" in normalized || "/downloads/chromalab/" in normalized ->
                RuntimeModelPathClass.PUBLIC_DOWNLOAD_EXPORT
            normalized.startsWith("content://") ->
                RuntimeModelPathClass.USER_SELECTED_EXTERNAL
            else -> RuntimeModelPathClass.UNKNOWN
        }
    }

    fun containsPrivatePathLeak(diagnostic: StructuredRuntimeDiagnostic): Boolean {
        val fields = listOfNotNull(
            diagnostic.modelId,
            diagnostic.backend,
            diagnostic.loadResult,
            diagnostic.fallbackReason,
            diagnostic.safeUserReportSummary,
        )
        return fields.any { field ->
            val value = field.replace('\\', '/').lowercase()
            "/data/data/" in value ||
                "/data/user/0/" in value ||
                "/files/models/" in value ||
                "c:/users/" in value
        }
    }

    private fun buildAvailabilitySummary(diagnostic: ModelAvailabilityDiagnostic): String =
        "Model ${diagnostic.status.name.lowercase()} for ${diagnostic.mode.name}; backend ${diagnostic.expectedBackend ?: "unknown"}."
}

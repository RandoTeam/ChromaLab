package com.chromalab.feature.processing.debug

import com.chromalab.feature.processing.rust.RustCvBridge
import com.chromalab.feature.reports.ReportExportPrivacyClass
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.TimeSource

actual object RustCvBridgeRuntimeProbe {
    actual fun diagnostic(): StructuredRuntimeDiagnostic {
        val startedAt = TimeSource.Monotonic.markNow()
        return runCatching {
            val probe = RustCvBridge.probeJson()
            val parsed = Json.parseToJsonElement(probe).jsonObject
            val version = parsed["bridgeVersion"]?.jsonPrimitive?.content ?: "unknown"
            val contract = parsed["ffiContract"]?.jsonPrimitive?.content ?: "unknown"
            StructuredRuntimeDiagnostic(
                diagnosticId = "runtime:rust_cv_bridge:$version",
                source = RuntimeDiagnosticSource.RUST_CV_BRIDGE,
                backend = "Rust chromalab-cv-core",
                loadAttempted = true,
                loadResult = parsed["nativeStatus"]?.jsonPrimitive?.content ?: "AVAILABLE",
                loadTimeMillis = startedAt.elapsedNow().inWholeMilliseconds,
                runtimeExposesMtp = contract,
                mtpEnabled = "NOT_APPLICABLE",
                privacyClass = ReportExportPrivacyClass.TECHNICAL_EVIDENCE,
                safeUserReportSummary = "Rust CV bridge loaded; diagnostic-only JNI contract active.",
            )
        }.getOrElse { error ->
            StructuredRuntimeDiagnostic(
                diagnosticId = "runtime:rust_cv_bridge:unavailable",
                source = RuntimeDiagnosticSource.RUST_CV_BRIDGE,
                backend = "Rust chromalab-cv-core",
                loadAttempted = true,
                loadResult = "unavailable:${error::class.simpleName ?: "error"}",
                loadTimeMillis = startedAt.elapsedNow().inWholeMilliseconds,
                fallbackReason = RustCvBridge.loadError()?.message ?: error.message,
                runtimeExposesMtp = "DR2D_JNI_PROBE_V1",
                mtpEnabled = "NOT_APPLICABLE",
                privacyClass = ReportExportPrivacyClass.TECHNICAL_EVIDENCE,
                safeUserReportSummary = "Rust CV bridge unavailable; deterministic Kotlin pipeline remains authoritative.",
            )
        }
    }
}

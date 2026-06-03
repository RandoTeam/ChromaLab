package com.chromalab.feature.processing.debug

import com.chromalab.feature.reports.ReportExportPrivacyClass

actual object RustCvBridgeRuntimeProbe {
    actual fun diagnostic(): StructuredRuntimeDiagnostic =
        StructuredRuntimeDiagnostic(
            diagnosticId = "runtime:rust_cv_bridge:desktop_not_packaged",
            source = RuntimeDiagnosticSource.RUST_CV_BRIDGE,
            backend = "Rust chromalab-cv-core",
            loadAttempted = false,
            loadResult = "not_packaged_on_desktop",
            runtimeExposesMtp = "DR2D_JNI_PROBE_V1",
            mtpEnabled = "NOT_APPLICABLE",
            privacyClass = ReportExportPrivacyClass.TECHNICAL_EVIDENCE,
            safeUserReportSummary = "Rust CV bridge is Android-only in DR-2D.",
        )
}

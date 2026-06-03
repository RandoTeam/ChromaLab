package com.chromalab.feature.processing.debug

expect object RustCvBridgeRuntimeProbe {
    fun diagnostic(): StructuredRuntimeDiagnostic
}

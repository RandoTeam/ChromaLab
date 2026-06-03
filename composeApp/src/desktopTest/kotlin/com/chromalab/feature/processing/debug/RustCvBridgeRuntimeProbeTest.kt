package com.chromalab.feature.processing.debug

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RustCvBridgeRuntimeProbeTest {
    @Test
    fun desktopProbeReportsAndroidOnlyBridgeWithoutLoadingNativeLibrary() {
        val diagnostic = RustCvBridgeRuntimeProbe.diagnostic()

        assertEquals(RuntimeDiagnosticSource.RUST_CV_BRIDGE, diagnostic.source)
        assertEquals("DR2D_JNI_PROBE_V1", diagnostic.runtimeExposesMtp)
        assertEquals("NOT_APPLICABLE", diagnostic.mtpEnabled)
        assertEquals("not_packaged_on_desktop", diagnostic.loadResult)
        assertFalse(diagnostic.loadAttempted)
    }
}

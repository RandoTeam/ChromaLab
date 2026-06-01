package com.chromalab.feature.processing.debug

import com.chromalab.feature.processing.inference.GgufMtpBenchmarkBackend
import com.chromalab.feature.processing.inference.GgufMtpBenchmarkGate
import com.chromalab.feature.processing.inference.GgufMtpBenchmarkMode
import com.chromalab.feature.processing.inference.GgufMtpBenchmarkPass
import com.chromalab.feature.processing.inference.GgufMtpBenchmarkSummary
import com.chromalab.feature.processing.inference.GgufMtpGateDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class StructuredRuntimeDiagnosticGgufMtpTest {
    @Test
    fun ggufMtpBenchmarkMapsToTechnicalStructuredRuntimeDiagnostics() {
        val summary = GgufMtpBenchmarkSummary(
            runId = "mtp-test",
            generatedAtEpochMillis = 1L,
            modelId = "qwen35-mtp-4b-q4km",
            backend = GgufMtpBenchmarkBackend.VULKAN,
            promptChars = 22,
            modelSupportsMtp = true,
            passes = listOf(
                GgufMtpBenchmarkPass(
                    label = "mtp_draft_3",
                    mode = GgufMtpBenchmarkMode.DRAFT_MTP,
                    backend = GgufMtpBenchmarkBackend.VULKAN,
                    modelId = "qwen35-mtp-4b-q4km",
                    modelPathClass = RuntimeModelPathClass.APP_PRIVATE_MODEL.name,
                    contextTokens = 2048,
                    batchTokens = 64,
                    maxTokens = 32,
                    mtpDraftTokens = 3,
                    generatedTokens = 16,
                    firstTokenLatencyMillis = 200,
                    loadTimeMillis = 900,
                    totalResponseDurationMillis = 700,
                ),
            ),
            gate = GgufMtpBenchmarkGate(
                decision = GgufMtpGateDecision.REVIEW_ONLY,
                verdict = "MTP_TIMING_MEASURED_NATIVE_ACCEPTANCE_STATS_MISSING",
                reasons = listOf("native_mtp_drafted_accepted_stats_not_exported"),
            ),
        )

        val diagnostic = StructuredRuntimeDiagnosticMapper.fromGgufMtpBenchmark(summary).single()

        assertEquals(RuntimeDiagnosticSource.GGUF_MTP_TEXT_ONLY, diagnostic.source)
        assertEquals(RuntimeModelPathClass.APP_PRIVATE_MODEL, diagnostic.modelPathClass)
        assertEquals("ENABLED_DRAFT_3", diagnostic.mtpEnabled)
        assertFalse(StructuredRuntimeDiagnosticMapper.containsPrivatePathLeak(diagnostic))
    }
}

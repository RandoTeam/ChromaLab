package com.chromalab.feature.processing.inference

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GgufMtpBenchmarkGateTest {
    @Test
    fun gateRequiresBothNoMtpAndDraftMtpPasses() {
        val gate = GgufMtpBenchmarkGateEvaluator.evaluate(
            passes = listOf(noMtpPass()),
        )

        assertEquals(GgufMtpGateDecision.INCONCLUSIVE, gate.decision)
        assertTrue(gate.reasons.contains("benchmark_requires_no_mtp_and_draft_mtp_passes"))
    }

    @Test
    fun gateKeepsMtpReviewOnlyWhenNativeAcceptanceStatsAreMissing() {
        val gate = GgufMtpBenchmarkGateEvaluator.evaluate(
            passes = listOf(
                noMtpPass(totalMs = 1_000, firstMs = 200, generatedTokens = 20),
                mtpPass(totalMs = 700, firstMs = 210, generatedTokens = 20),
            ),
        )

        assertEquals(GgufMtpGateDecision.REVIEW_ONLY, gate.decision)
        assertEquals("MTP_TIMING_MEASURED_NATIVE_ACCEPTANCE_STATS_MISSING", gate.verdict)
        assertTrue(gate.reasons.contains("native_mtp_drafted_accepted_stats_not_exported"))
    }

    @Test
    fun gateEnablesMtpOnlyWithSpeedupAndAcceptanceEvidence() {
        val gate = GgufMtpBenchmarkGateEvaluator.evaluate(
            passes = listOf(
                noMtpPass(totalMs = 1_100, firstMs = 200, generatedTokens = 20),
                mtpPass(
                    totalMs = 800,
                    firstMs = 210,
                    generatedTokens = 20,
                    draftedTokens = 40,
                    acceptedTokens = 20,
                    acceptanceRate = 0.50,
                ),
            ),
        )

        assertEquals(GgufMtpGateDecision.ENABLE_FOR_PROFILE, gate.decision)
        assertEquals("MTP_FASTER", gate.verdict)
    }

    @Test
    fun gateKeepsMtpDisabledWhenFirstTokenRegresses() {
        val gate = GgufMtpBenchmarkGateEvaluator.evaluate(
            passes = listOf(
                noMtpPass(totalMs = 1_000, firstMs = 200, generatedTokens = 20),
                mtpPass(
                    totalMs = 700,
                    firstMs = 400,
                    generatedTokens = 20,
                    draftedTokens = 40,
                    acceptedTokens = 20,
                    acceptanceRate = 0.50,
                ),
            ),
        )

        assertEquals(GgufMtpGateDecision.KEEP_DISABLED, gate.decision)
        assertEquals("MTP_SLOW_FIRST_TOKEN", gate.verdict)
    }

    @Test
    fun safetyPolicyRejectsMtpForVisionAndStrictChromatogramAnalysis() {
        val vision = GgufMtpSafetyPolicy.resolveLoadPolicy(
            supportsMtp = true,
            requestedDraftTokens = 6,
            hasVisionProjector = true,
            strictChromatogramAnalysis = false,
        )
        val strictChromatogram = GgufMtpSafetyPolicy.resolveLoadPolicy(
            supportsMtp = true,
            requestedDraftTokens = 6,
            hasVisionProjector = false,
            strictChromatogramAnalysis = true,
        )

        assertEquals(0, vision.effectiveDraftTokens)
        assertEquals(false, vision.allowed)
        assertEquals("mtp_disallowed_for_mmproj_vision", vision.reason)
        assertEquals(0, strictChromatogram.effectiveDraftTokens)
        assertEquals(false, strictChromatogram.allowed)
        assertEquals("mtp_disallowed_for_strict_chromatogram_numeric_analysis", strictChromatogram.reason)
    }

    @Test
    fun markdownRendersGateAndPassRows() {
        val summary = GgufMtpBenchmarkSummary(
            runId = "test-run",
            generatedAtEpochMillis = 1L,
            modelId = "qwen35-mtp-4b-q4km",
            backend = GgufMtpBenchmarkBackend.CPU,
            promptChars = 22,
            modelSupportsMtp = true,
            passes = listOf(noMtpPass(), mtpPass()),
            gate = GgufMtpBenchmarkGateEvaluator.evaluate(listOf(noMtpPass(), mtpPass())),
        )

        val markdown = GgufMtpBenchmarkMarkdownRenderer.render(summary)

        assertTrue(markdown.contains("GGUF Text-Only MTP A/B Benchmark"))
        assertTrue(markdown.contains("qwen35-mtp-4b-q4km"))
        assertTrue(markdown.contains("MTP_TIMING_MEASURED_NATIVE_ACCEPTANCE_STATS_MISSING"))
    }

    private fun noMtpPass(
        totalMs: Long = 1_000,
        firstMs: Long = 200,
        generatedTokens: Int? = 20,
    ): GgufMtpBenchmarkPass =
        GgufMtpBenchmarkPass(
            label = "no_mtp",
            mode = GgufMtpBenchmarkMode.NO_MTP,
            backend = GgufMtpBenchmarkBackend.CPU,
            modelId = "qwen35-mtp-4b-q4km",
            modelPathClass = "APP_PRIVATE_MODEL",
            contextTokens = 2048,
            batchTokens = 64,
            maxTokens = 32,
            mtpDraftTokens = 0,
            generatedTokens = generatedTokens,
            firstTokenLatencyMillis = firstMs,
            loadTimeMillis = 100,
            totalResponseDurationMillis = totalMs,
        )

    private fun mtpPass(
        totalMs: Long = 800,
        firstMs: Long = 210,
        generatedTokens: Int? = 20,
        draftedTokens: Int? = null,
        acceptedTokens: Int? = null,
        acceptanceRate: Double? = null,
    ): GgufMtpBenchmarkPass =
        GgufMtpBenchmarkPass(
            label = "mtp_draft_3",
            mode = GgufMtpBenchmarkMode.DRAFT_MTP,
            backend = GgufMtpBenchmarkBackend.CPU,
            modelId = "qwen35-mtp-4b-q4km",
            modelPathClass = "APP_PRIVATE_MODEL",
            contextTokens = 2048,
            batchTokens = 64,
            maxTokens = 32,
            mtpDraftTokens = 3,
            generatedTokens = generatedTokens,
            firstTokenLatencyMillis = firstMs,
            loadTimeMillis = 120,
            totalResponseDurationMillis = totalMs,
            draftedTokens = draftedTokens,
            acceptedTokens = acceptedTokens,
            acceptanceRate = acceptanceRate,
        )
}

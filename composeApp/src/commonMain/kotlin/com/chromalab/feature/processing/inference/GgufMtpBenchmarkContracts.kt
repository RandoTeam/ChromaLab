package com.chromalab.feature.processing.inference

import kotlinx.serialization.Serializable

@Serializable
enum class GgufMtpBenchmarkBackend {
    CPU,
    VULKAN,
}

@Serializable
enum class GgufMtpBenchmarkMode {
    NO_MTP,
    DRAFT_MTP,
}

@Serializable
enum class GgufMtpGateDecision {
    ENABLE_FOR_PROFILE,
    REVIEW_ONLY,
    KEEP_DISABLED,
    INCONCLUSIVE,
}

@Serializable
data class GgufMtpBenchmarkPass(
    val label: String,
    val mode: GgufMtpBenchmarkMode,
    val backend: GgufMtpBenchmarkBackend,
    val modelId: String,
    val modelPathClass: String,
    val contextTokens: Int,
    val batchTokens: Int,
    val maxTokens: Int,
    val mtpDraftTokens: Int,
    val promptTokens: Int? = null,
    val generatedTokens: Int? = null,
    val firstTokenLatencyMillis: Long? = null,
    val loadTimeMillis: Long? = null,
    val totalResponseDurationMillis: Long,
    val totalTokensPerSecond: Double? = null,
    val draftedTokens: Int? = null,
    val acceptedTokens: Int? = null,
    val acceptanceRate: Double? = null,
    val tokenCallbackCount: Int = 0,
    val outputChars: Int = 0,
    val timedOut: Boolean = false,
    val timeoutMillis: Long? = null,
    val failureReason: String? = null,
    val missingMetricReasons: List<String> = emptyList(),
)

@Serializable
data class GgufMtpBenchmarkGate(
    val decision: GgufMtpGateDecision,
    val verdict: String,
    val speedup: Double? = null,
    val firstTokenSlowdown: Double? = null,
    val reasons: List<String> = emptyList(),
)

@Serializable
data class GgufMtpBenchmarkSummary(
    val schemaVersion: String = "gguf-mtp-benchmark-1.0",
    val runId: String,
    val generatedAtEpochMillis: Long,
    val modelId: String?,
    val backend: GgufMtpBenchmarkBackend,
    val promptChars: Int,
    val modelSupportsMtp: Boolean,
    val passes: List<GgufMtpBenchmarkPass>,
    val gate: GgufMtpBenchmarkGate,
    val exportPrivacyClass: String = "TECHNICAL_EVIDENCE",
    val notes: List<String> = emptyList(),
)

@Serializable
data class GgufMtpLoadPolicyDecision(
    val effectiveDraftTokens: Int,
    val allowed: Boolean,
    val reason: String,
)

object GgufMtpSafetyPolicy {
    fun resolveLoadPolicy(
        supportsMtp: Boolean,
        requestedDraftTokens: Int,
        hasVisionProjector: Boolean,
        strictChromatogramAnalysis: Boolean,
    ): GgufMtpLoadPolicyDecision {
        if (requestedDraftTokens <= 0) {
            return GgufMtpLoadPolicyDecision(
                effectiveDraftTokens = 0,
                allowed = true,
                reason = "mtp_not_requested",
            )
        }
        if (!supportsMtp) {
            return GgufMtpLoadPolicyDecision(
                effectiveDraftTokens = 0,
                allowed = false,
                reason = "model_does_not_advertise_mtp",
            )
        }
        if (hasVisionProjector) {
            return GgufMtpLoadPolicyDecision(
                effectiveDraftTokens = 0,
                allowed = false,
                reason = "mtp_disallowed_for_mmproj_vision",
            )
        }
        if (strictChromatogramAnalysis) {
            return GgufMtpLoadPolicyDecision(
                effectiveDraftTokens = 0,
                allowed = false,
                reason = "mtp_disallowed_for_strict_chromatogram_numeric_analysis",
            )
        }
        return GgufMtpLoadPolicyDecision(
            effectiveDraftTokens = requestedDraftTokens.coerceIn(1, 16),
            allowed = true,
            reason = "text_only_mtp_allowed",
        )
    }
}

object GgufMtpBenchmarkGateEvaluator {
    fun evaluate(
        passes: List<GgufMtpBenchmarkPass>,
        minimumSpeedup: Double = 1.10,
        maximumFirstTokenSlowdown: Double = 1.20,
        minimumAcceptanceRate: Double = 0.30,
    ): GgufMtpBenchmarkGate {
        val baseline = passes.firstOrNull { it.mode == GgufMtpBenchmarkMode.NO_MTP }
        val mtp = passes.firstOrNull { it.mode == GgufMtpBenchmarkMode.DRAFT_MTP && it.mtpDraftTokens > 0 }
        if (baseline == null || mtp == null) {
            return GgufMtpBenchmarkGate(
                decision = GgufMtpGateDecision.INCONCLUSIVE,
                verdict = "INCONCLUSIVE",
                reasons = listOf("benchmark_requires_no_mtp_and_draft_mtp_passes"),
            )
        }
        val failureReasons = listOfNotNull(baseline.failureReason, mtp.failureReason)
        if (failureReasons.isNotEmpty()) {
            return GgufMtpBenchmarkGate(
                decision = GgufMtpGateDecision.INCONCLUSIVE,
                verdict = "INCONCLUSIVE",
                reasons = failureReasons.map { "pass_failed:$it" },
            )
        }
        val baselineFirst = baseline.firstTokenLatencyMillis
        val mtpFirst = mtp.firstTokenLatencyMillis
        if (baselineFirst == null || mtpFirst == null) {
            return GgufMtpBenchmarkGate(
                decision = GgufMtpGateDecision.INCONCLUSIVE,
                verdict = "INCONCLUSIVE",
                reasons = listOf("first_token_latency_required"),
            )
        }
        val missingNativeStats = mtp.draftedTokens == null ||
            mtp.acceptedTokens == null ||
            mtp.acceptanceRate == null
        val speedup = if (baseline.totalResponseDurationMillis > 0 && mtp.totalResponseDurationMillis > 0) {
            baseline.totalResponseDurationMillis.toDouble() / mtp.totalResponseDurationMillis.toDouble()
        } else {
            null
        }
        val firstTokenSlowdown = if (baselineFirst > 0) {
            mtpFirst.toDouble() / baselineFirst.toDouble()
        } else {
            null
        }
        if (missingNativeStats) {
            return GgufMtpBenchmarkGate(
                decision = GgufMtpGateDecision.REVIEW_ONLY,
                verdict = "MTP_TIMING_MEASURED_NATIVE_ACCEPTANCE_STATS_MISSING",
                speedup = speedup,
                firstTokenSlowdown = firstTokenSlowdown,
                reasons = listOf("native_mtp_drafted_accepted_stats_not_exported"),
            )
        }
        if (firstTokenSlowdown != null && firstTokenSlowdown > maximumFirstTokenSlowdown) {
            return GgufMtpBenchmarkGate(
                decision = GgufMtpGateDecision.KEEP_DISABLED,
                verdict = "MTP_SLOW_FIRST_TOKEN",
                speedup = speedup,
                firstTokenSlowdown = firstTokenSlowdown,
                reasons = listOf("first_token_slowdown_exceeds_budget"),
            )
        }
        if (speedup == null || speedup < minimumSpeedup) {
            return GgufMtpBenchmarkGate(
                decision = GgufMtpGateDecision.KEEP_DISABLED,
                verdict = "MTP_NOT_FASTER_ENOUGH",
                speedup = speedup,
                firstTokenSlowdown = firstTokenSlowdown,
                reasons = listOf("speedup_below_threshold"),
            )
        }
        if (mtp.acceptanceRate < minimumAcceptanceRate) {
            return GgufMtpBenchmarkGate(
                decision = GgufMtpGateDecision.KEEP_DISABLED,
                verdict = "MTP_LOW_ACCEPTANCE",
                speedup = speedup,
                firstTokenSlowdown = firstTokenSlowdown,
                reasons = listOf("acceptance_rate_below_threshold"),
            )
        }
        return GgufMtpBenchmarkGate(
            decision = GgufMtpGateDecision.ENABLE_FOR_PROFILE,
            verdict = "MTP_FASTER",
            speedup = speedup,
            firstTokenSlowdown = firstTokenSlowdown,
            reasons = listOf("speedup_and_acceptance_pass"),
        )
    }
}

object GgufMtpBenchmarkMarkdownRenderer {
    fun render(summary: GgufMtpBenchmarkSummary): String = buildString {
        appendLine("# GGUF Text-Only MTP A/B Benchmark")
        appendLine()
        appendLine("- Run id: `${summary.runId}`")
        appendLine("- Model: `${summary.modelId ?: "not_resolved"}`")
        appendLine("- Backend: `${summary.backend.name}`")
        appendLine("- Gate decision: `${summary.gate.decision.name}`")
        appendLine("- Verdict: `${summary.gate.verdict}`")
        appendLine("- Privacy class: `${summary.exportPrivacyClass}`")
        appendLine()
        appendLine("## Passes")
        appendLine()
        appendLine("| Pass | Backend | Draft | Load ms | First token ms | Total ms | Tokens | TPS | Drafted | Accepted | Acceptance | Failure |")
        appendLine("| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |")
        summary.passes.forEach { pass ->
            appendLine(
                "| `${pass.label}` | ${pass.backend.name} | ${pass.mtpDraftTokens} | " +
                    "${pass.loadTimeMillis ?: "-"} | ${pass.firstTokenLatencyMillis ?: "-"} | " +
                    "${pass.totalResponseDurationMillis} | ${pass.generatedTokens ?: "-"} | " +
                    "${pass.totalTokensPerSecond?.format2() ?: "-"} | ${pass.draftedTokens ?: "-"} | " +
                    "${pass.acceptedTokens ?: "-"} | ${pass.acceptanceRate?.format2() ?: "-"} | " +
                    "${pass.failureReason ?: "-"} |",
            )
        }
        appendLine()
        appendLine("## Gate Reasons")
        appendLine()
        summary.gate.reasons.forEach { appendLine("- `$it`") }
        if (summary.notes.isNotEmpty()) {
            appendLine()
            appendLine("## Notes")
            summary.notes.forEach { appendLine("- $it") }
        }
    }

    private fun Double.format2(): String =
        (kotlin.math.round(this * 100.0) / 100.0).toString()
}

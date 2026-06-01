package com.chromalab.feature.processing.inference

import kotlinx.serialization.Serializable

@Serializable
enum class GgufVulkanMatrixProfile {
    CPU,
    EXPLICIT_VULKAN,
    AUTO,
}

@Serializable
enum class GgufVulkanMatrixDecision {
    VULKAN_BENEFICIAL,
    CPU_DEFAULT,
    REVIEW_ONLY,
    INCONCLUSIVE,
}

@Serializable
data class GgufVulkanPreflightEvidence(
    val deviceName: String,
    val sdkInt: Int,
    val availableBackendCodes: List<Int>,
    val acceleratedBackendAvailable: Boolean,
    val requiredFeatureFlags: List<String>,
    val vulkanHardwareLevelAvailable: Boolean,
    val vulkanHardwareVersionAvailable: Boolean,
    val vulkanDeqpLevelAvailable: Boolean,
    val selectedBackendHint: String,
    val fallbackReason: String? = null,
)

@Serializable
data class GgufVulkanMatrixPass(
    val profile: GgufVulkanMatrixProfile,
    val modelId: String,
    val modelPathClass: String,
    val contextTokens: Int,
    val batchTokens: Int,
    val maxTokens: Int,
    val requestedBackend: String,
    val selectedBackend: String? = null,
    val preferAccelerated: Boolean,
    val loadAttempted: Boolean,
    val loadTimeMillis: Long? = null,
    val firstTokenLatencyMillis: Long? = null,
    val totalResponseDurationMillis: Long? = null,
    val generatedTokens: Int? = null,
    val totalTokensPerSecond: Double? = null,
    val tokenCallbackCount: Int = 0,
    val outputChars: Int = 0,
    val timeoutMillis: Long? = null,
    val timedOut: Boolean = false,
    val fallbackReason: String? = null,
    val failureReason: String? = null,
    val missingMetricReasons: List<String> = emptyList(),
)

@Serializable
data class GgufVulkanMatrixGate(
    val decision: GgufVulkanMatrixDecision,
    val verdict: String,
    val explicitVulkanSpeedup: Double? = null,
    val autoSpeedup: Double? = null,
    val reasons: List<String> = emptyList(),
)

@Serializable
data class GgufVulkanMatrixSummary(
    val schemaVersion: String = "gguf-vulkan-runtime-matrix-1.0",
    val runId: String,
    val generatedAtEpochMillis: Long,
    val modelId: String?,
    val promptChars: Int,
    val preflight: GgufVulkanPreflightEvidence,
    val passes: List<GgufVulkanMatrixPass>,
    val gate: GgufVulkanMatrixGate,
    val exportPrivacyClass: String = "TECHNICAL_EVIDENCE",
    val notes: List<String> = emptyList(),
)

object GgufVulkanMatrixGateEvaluator {
    fun evaluate(
        preflight: GgufVulkanPreflightEvidence,
        passes: List<GgufVulkanMatrixPass>,
        minimumSpeedup: Double = 1.10,
        maximumFirstTokenSlowdown: Double = 1.25,
    ): GgufVulkanMatrixGate {
        val cpu = passes.firstOrNull { it.profile == GgufVulkanMatrixProfile.CPU }
        if (cpu == null || cpu.failureReason != null || cpu.totalResponseDurationMillis == null) {
            return GgufVulkanMatrixGate(
                decision = GgufVulkanMatrixDecision.INCONCLUSIVE,
                verdict = "CPU_BASELINE_REQUIRED",
                reasons = listOf("cpu_baseline_missing_or_failed"),
            )
        }
        if (!preflight.acceleratedBackendAvailable) {
            val auto = passes.firstOrNull { it.profile == GgufVulkanMatrixProfile.AUTO }
            val fallbackReason = auto?.fallbackReason ?: preflight.fallbackReason
            return GgufVulkanMatrixGate(
                decision = GgufVulkanMatrixDecision.CPU_DEFAULT,
                verdict = "VULKAN_UNAVAILABLE_CPU_FALLBACK",
                reasons = listOfNotNull(
                    "accelerated_backend_not_available",
                    fallbackReason,
                ),
            )
        }

        val explicit = passes.firstOrNull { it.profile == GgufVulkanMatrixProfile.EXPLICIT_VULKAN }
        val auto = passes.firstOrNull { it.profile == GgufVulkanMatrixProfile.AUTO }
        val explicitSpeedup = explicit?.speedupOver(cpu)
        val autoSpeedup = auto?.speedupOver(cpu)
        val failures = listOfNotNull(explicit?.failureReason, auto?.failureReason)
        if (failures.isNotEmpty()) {
            return GgufVulkanMatrixGate(
                decision = GgufVulkanMatrixDecision.REVIEW_ONLY,
                verdict = "VULKAN_PROFILE_FAILED",
                explicitVulkanSpeedup = explicitSpeedup,
                autoSpeedup = autoSpeedup,
                reasons = failures.map { "pass_failed:$it" },
            )
        }

        val bestSpeedup = listOfNotNull(explicitSpeedup, autoSpeedup).maxOrNull()
        if (bestSpeedup == null) {
            return GgufVulkanMatrixGate(
                decision = GgufVulkanMatrixDecision.INCONCLUSIVE,
                verdict = "VULKAN_TIMING_MISSING",
                reasons = listOf("vulkan_or_auto_timing_missing"),
            )
        }
        val slowFirstToken = listOfNotNull(explicit, auto).any { pass ->
            cpu.firstTokenLatencyMillis != null &&
                pass.firstTokenLatencyMillis != null &&
                pass.firstTokenLatencyMillis.toDouble() / cpu.firstTokenLatencyMillis.toDouble() > maximumFirstTokenSlowdown
        }
        if (slowFirstToken) {
            return GgufVulkanMatrixGate(
                decision = GgufVulkanMatrixDecision.CPU_DEFAULT,
                verdict = "VULKAN_SLOW_FIRST_TOKEN",
                explicitVulkanSpeedup = explicitSpeedup,
                autoSpeedup = autoSpeedup,
                reasons = listOf("first_token_slowdown_exceeds_budget"),
            )
        }
        if (bestSpeedup < minimumSpeedup) {
            return GgufVulkanMatrixGate(
                decision = GgufVulkanMatrixDecision.CPU_DEFAULT,
                verdict = "VULKAN_NOT_FASTER_ENOUGH",
                explicitVulkanSpeedup = explicitSpeedup,
                autoSpeedup = autoSpeedup,
                reasons = listOf("speedup_below_threshold"),
            )
        }
        return GgufVulkanMatrixGate(
            decision = GgufVulkanMatrixDecision.VULKAN_BENEFICIAL,
            verdict = "VULKAN_FASTER",
            explicitVulkanSpeedup = explicitSpeedup,
            autoSpeedup = autoSpeedup,
            reasons = listOf("vulkan_profile_beats_cpu_budget"),
        )
    }

    private fun GgufVulkanMatrixPass.speedupOver(cpu: GgufVulkanMatrixPass): Double? {
        val cpuMs = cpu.totalResponseDurationMillis ?: return null
        val profileMs = totalResponseDurationMillis ?: return null
        if (cpuMs <= 0 || profileMs <= 0) return null
        return cpuMs.toDouble() / profileMs.toDouble()
    }
}

object GgufVulkanMatrixMarkdownRenderer {
    fun render(summary: GgufVulkanMatrixSummary): String = buildString {
        appendLine("# GGUF llama.cpp Vulkan Runtime Matrix")
        appendLine()
        appendLine("- Run id: `${summary.runId}`")
        appendLine("- Model: `${summary.modelId ?: "not_resolved"}`")
        appendLine("- Device: `${summary.preflight.deviceName}`")
        appendLine("- Accelerated backend available: `${summary.preflight.acceleratedBackendAvailable}`")
        appendLine("- Selected backend hint: `${summary.preflight.selectedBackendHint}`")
        appendLine("- Gate decision: `${summary.gate.decision.name}`")
        appendLine("- Verdict: `${summary.gate.verdict}`")
        appendLine("- Privacy class: `${summary.exportPrivacyClass}`")
        appendLine()
        appendLine("## Preflight")
        appendLine()
        appendLine("- Available backend codes: `${summary.preflight.availableBackendCodes.joinToString()}`")
        appendLine("- Vulkan hardware level feature: `${summary.preflight.vulkanHardwareLevelAvailable}`")
        appendLine("- Vulkan hardware version feature: `${summary.preflight.vulkanHardwareVersionAvailable}`")
        appendLine("- Vulkan dEQP feature: `${summary.preflight.vulkanDeqpLevelAvailable}`")
        appendLine("- Fallback reason: `${summary.preflight.fallbackReason ?: "-"}`")
        appendLine()
        appendLine("## Matrix")
        appendLine()
        appendLine("| Profile | Requested | Selected | Load ms | First token ms | Total ms | Tokens | TPS | Fallback | Failure |")
        appendLine("| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |")
        summary.passes.forEach { pass ->
            appendLine(
                "| `${pass.profile.name}` | ${pass.requestedBackend} | ${pass.selectedBackend ?: "-"} | " +
                    "${pass.loadTimeMillis ?: "-"} | ${pass.firstTokenLatencyMillis ?: "-"} | " +
                    "${pass.totalResponseDurationMillis ?: "-"} | ${pass.generatedTokens ?: "-"} | " +
                    "${pass.totalTokensPerSecond?.format2() ?: "-"} | ${pass.fallbackReason ?: "-"} | " +
                    "${pass.failureReason ?: "-"} |",
            )
        }
        appendLine()
        appendLine("## Gate Reasons")
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

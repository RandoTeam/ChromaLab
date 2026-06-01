package com.chromalab.feature.processing.inference

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GgufVulkanRuntimeMatrixTest {
    @Test
    fun matrixFallsBackToCpuWhenAcceleratedBackendIsUnavailable() {
        val preflight = preflight(accelerated = false)
        val gate = GgufVulkanMatrixGateEvaluator.evaluate(
            preflight = preflight,
            passes = listOf(
                pass(GgufVulkanMatrixProfile.CPU, totalMs = 1_000),
                skippedExplicitVulkan(),
                pass(
                    GgufVulkanMatrixProfile.AUTO,
                    requestedBackend = "auto",
                    selectedBackend = "llama.cpp CPU",
                    fallbackReason = "auto_fell_back_to_cpu_no_accelerated_backend",
                    totalMs = 1_020,
                ),
            ),
        )

        assertEquals(GgufVulkanMatrixDecision.CPU_DEFAULT, gate.decision)
        assertEquals("VULKAN_UNAVAILABLE_CPU_FALLBACK", gate.verdict)
        assertTrue(gate.reasons.contains("auto_fell_back_to_cpu_no_accelerated_backend"))
    }

    @Test
    fun matrixEnablesVulkanOnlyWhenTimingBeatsCpuBudget() {
        val gate = GgufVulkanMatrixGateEvaluator.evaluate(
            preflight = preflight(accelerated = true),
            passes = listOf(
                pass(GgufVulkanMatrixProfile.CPU, totalMs = 1_200, firstMs = 200),
                pass(GgufVulkanMatrixProfile.EXPLICIT_VULKAN, totalMs = 800, firstMs = 220),
                pass(GgufVulkanMatrixProfile.AUTO, requestedBackend = "auto", totalMs = 820, firstMs = 215),
            ),
        )

        assertEquals(GgufVulkanMatrixDecision.VULKAN_BENEFICIAL, gate.decision)
        assertEquals("VULKAN_FASTER", gate.verdict)
    }

    @Test
    fun matrixKeepsCpuDefaultWhenVulkanFirstTokenRegresses() {
        val gate = GgufVulkanMatrixGateEvaluator.evaluate(
            preflight = preflight(accelerated = true),
            passes = listOf(
                pass(GgufVulkanMatrixProfile.CPU, totalMs = 1_200, firstMs = 200),
                pass(GgufVulkanMatrixProfile.EXPLICIT_VULKAN, totalMs = 800, firstMs = 400),
                pass(GgufVulkanMatrixProfile.AUTO, requestedBackend = "auto", totalMs = 820, firstMs = 410),
            ),
        )

        assertEquals(GgufVulkanMatrixDecision.CPU_DEFAULT, gate.decision)
        assertEquals("VULKAN_SLOW_FIRST_TOKEN", gate.verdict)
    }

    @Test
    fun markdownIncludesPreflightAndFallbackReasons() {
        val summary = GgufVulkanMatrixSummary(
            runId = "run-1",
            generatedAtEpochMillis = 1L,
            modelId = "qwen35-mtp-4b-q4km",
            promptChars = 16,
            preflight = preflight(accelerated = false),
            passes = listOf(pass(GgufVulkanMatrixProfile.CPU), skippedExplicitVulkan()),
            gate = GgufVulkanMatrixGate(
                decision = GgufVulkanMatrixDecision.CPU_DEFAULT,
                verdict = "VULKAN_UNAVAILABLE_CPU_FALLBACK",
                reasons = listOf("explicit_vulkan_skipped_preflight_failed"),
            ),
        )

        val markdown = GgufVulkanMatrixMarkdownRenderer.render(summary)

        assertTrue(markdown.contains("GGUF llama.cpp Vulkan Runtime Matrix"))
        assertTrue(markdown.contains("explicit_vulkan_skipped_preflight_failed"))
        assertTrue(markdown.contains("Accelerated backend available"))
    }

    private fun preflight(accelerated: Boolean): GgufVulkanPreflightEvidence =
        GgufVulkanPreflightEvidence(
            deviceName = "test-device",
            sdkInt = 35,
            availableBackendCodes = if (accelerated) listOf(0, 1) else listOf(0),
            acceleratedBackendAvailable = accelerated,
            requiredFeatureFlags = listOf(
                "android.hardware.vulkan.level",
                "android.hardware.vulkan.version",
                "android.software.vulkan.deqp.level",
            ),
            vulkanHardwareLevelAvailable = accelerated,
            vulkanHardwareVersionAvailable = accelerated,
            vulkanDeqpLevelAvailable = accelerated,
            selectedBackendHint = if (accelerated) "explicit_vulkan_and_auto_can_try_accelerated" else "cpu_only",
            fallbackReason = if (accelerated) null else "no_ggml_accelerated_backend_reported",
        )

    private fun pass(
        profile: GgufVulkanMatrixProfile,
        requestedBackend: String = profile.name.lowercase(),
        selectedBackend: String = "llama.cpp ${if (profile == GgufVulkanMatrixProfile.CPU) "CPU" else "Vulkan"}",
        fallbackReason: String? = null,
        totalMs: Long = 1_000,
        firstMs: Long = 200,
    ): GgufVulkanMatrixPass =
        GgufVulkanMatrixPass(
            profile = profile,
            modelId = "qwen35-mtp-4b-q4km",
            modelPathClass = "APP_PRIVATE_MODEL",
            contextTokens = 2048,
            batchTokens = 64,
            maxTokens = 32,
            requestedBackend = requestedBackend,
            selectedBackend = selectedBackend,
            preferAccelerated = profile != GgufVulkanMatrixProfile.CPU,
            loadAttempted = true,
            loadTimeMillis = 100,
            firstTokenLatencyMillis = firstMs,
            totalResponseDurationMillis = totalMs,
            generatedTokens = 20,
            totalTokensPerSecond = 20.0,
            fallbackReason = fallbackReason,
        )

    private fun skippedExplicitVulkan(): GgufVulkanMatrixPass =
        GgufVulkanMatrixPass(
            profile = GgufVulkanMatrixProfile.EXPLICIT_VULKAN,
            modelId = "qwen35-mtp-4b-q4km",
            modelPathClass = "APP_PRIVATE_MODEL",
            contextTokens = 2048,
            batchTokens = 64,
            maxTokens = 32,
            requestedBackend = "vulkan",
            preferAccelerated = true,
            loadAttempted = false,
            fallbackReason = "explicit_vulkan_skipped_preflight_failed",
        )
}

package com.chromalab.feature.processing.debug

import com.chromalab.feature.processing.inference.GgufVulkanMatrixDecision
import com.chromalab.feature.processing.inference.GgufVulkanMatrixGate
import com.chromalab.feature.processing.inference.GgufVulkanMatrixPass
import com.chromalab.feature.processing.inference.GgufVulkanMatrixProfile
import com.chromalab.feature.processing.inference.GgufVulkanMatrixSummary
import com.chromalab.feature.processing.inference.GgufVulkanPreflightEvidence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StructuredRuntimeDiagnosticVulkanMatrixTest {
    @Test
    fun mapperExportsPreflightAndFallbackReasonWithoutPrivatePaths() {
        val summary = GgufVulkanMatrixSummary(
            runId = "run-1",
            generatedAtEpochMillis = 1L,
            modelId = "qwen35-mtp-4b-q4km",
            promptChars = 16,
            preflight = GgufVulkanPreflightEvidence(
                deviceName = "test-device",
                sdkInt = 35,
                availableBackendCodes = listOf(0),
                acceleratedBackendAvailable = false,
                requiredFeatureFlags = listOf("android.hardware.vulkan.level"),
                vulkanHardwareLevelAvailable = false,
                vulkanHardwareVersionAvailable = false,
                vulkanDeqpLevelAvailable = false,
                selectedBackendHint = "cpu_only",
                fallbackReason = "no_ggml_accelerated_backend_reported",
            ),
            passes = listOf(
                GgufVulkanMatrixPass(
                    profile = GgufVulkanMatrixProfile.AUTO,
                    modelId = "qwen35-mtp-4b-q4km",
                    modelPathClass = "APP_PRIVATE_MODEL",
                    contextTokens = 2048,
                    batchTokens = 64,
                    maxTokens = 32,
                    requestedBackend = "auto",
                    selectedBackend = "llama.cpp CPU",
                    preferAccelerated = false,
                    loadAttempted = true,
                    fallbackReason = "auto_fell_back_to_cpu_no_accelerated_backend",
                ),
            ),
            gate = GgufVulkanMatrixGate(
                decision = GgufVulkanMatrixDecision.CPU_DEFAULT,
                verdict = "VULKAN_UNAVAILABLE_CPU_FALLBACK",
            ),
        )

        val diagnostics = StructuredRuntimeDiagnosticMapper.fromGgufVulkanMatrix(summary)

        assertEquals(2, diagnostics.size)
        assertTrue(diagnostics.any { it.source == RuntimeDiagnosticSource.VULKAN_PREFLIGHT })
        assertTrue(diagnostics.any { it.fallbackReason == "auto_fell_back_to_cpu_no_accelerated_backend" })
        assertFalse(diagnostics.any { StructuredRuntimeDiagnosticMapper.containsPrivatePathLeak(it) })
    }
}

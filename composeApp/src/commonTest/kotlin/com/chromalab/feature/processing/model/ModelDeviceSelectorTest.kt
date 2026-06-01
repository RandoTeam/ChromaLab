package com.chromalab.feature.processing.model

import com.chromalab.feature.processing.inference.ModelRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelDeviceSelectorTest {
    @Test
    fun selectsDeviceSpecificE2BWhenDeviceMatchesAndBundleIsDownloaded() {
        val generic = checkNotNull(ModelRegistry.findById("gemma4-e2b"))
        val sm8750 = checkNotNull(ModelRegistry.findById("gemma4-e2b-qualcomm-sm8750"))

        val result = ModelDeviceSelector.selectChromatogramModel(
            models = listOf(generic, sm8750),
            profile = ModelDeviceProfile(hardware = "qcom sm8750"),
        )

        assertEquals("gemma4-e2b-qualcomm-sm8750", result.selectedModelId)
        assertEquals(ModelDeviceTarget.QUALCOMM_SM8750, result.detectedDeviceTarget)
        assertEquals(ModelSelectionReason.DEVICE_SPECIFIC_MATCH, result.reason)
    }

    @Test
    fun fallsBackToGenericE2BWhenDeviceSpecificBundleIsMissing() {
        val generic = checkNotNull(ModelRegistry.findById("gemma4-e2b"))

        val result = ModelDeviceSelector.selectChromatogramModel(
            models = listOf(generic),
            profile = ModelDeviceProfile(hardware = "qcom sm8750"),
        )

        assertEquals("gemma4-e2b", result.selectedModelId)
        assertEquals(ModelSelectionReason.GENERIC_E2B_FAST_FALLBACK, result.reason)
        assertTrue(result.fallbackModelAttempted)
        assertEquals("device_specific_bundle_not_downloaded", result.fallbackResult)
    }

    @Test
    fun rejectsNonMatchingDeviceSpecificBundleForAutomaticSelection() {
        val sm8750 = checkNotNull(ModelRegistry.findById("gemma4-e2b-qualcomm-sm8750"))

        val result = ModelDeviceSelector.selectChromatogramModel(
            models = listOf(sm8750),
            profile = ModelDeviceProfile(hardware = "gs501"),
        )

        assertEquals(null, result.selectedModelId)
        assertEquals(ModelDeviceTarget.GOOGLE_TENSOR_G5, result.detectedDeviceTarget)
        assertEquals(ModelSelectionReason.NO_COMPATIBLE_MODEL, result.reason)
        assertEquals(listOf("gemma4-e2b-qualcomm-sm8750"), result.rejectedModelIds)
    }

    @Test
    fun explicitSelectionStillRequiresDeviceMatch() {
        val tensor = checkNotNull(ModelRegistry.findById("gemma4-e2b-google-tensor-g5"))

        val result = ModelDeviceSelector.selectChromatogramModel(
            models = listOf(tensor),
            profile = ModelDeviceProfile(hardware = "qcom sm8750"),
            explicitModelId = tensor.id,
        )

        assertEquals(null, result.selectedModelId)
        assertEquals(ModelSelectionReason.NO_COMPATIBLE_MODEL, result.reason)
        assertEquals("explicit_model_missing_or_device_mismatch:${tensor.id}", result.fallbackResult)
    }

    @Test
    fun fallsBackToChromatogramPriorityWhenGenericE2BIsNotDownloaded() {
        val fallback = ModelInfo(
            id = "fallback-litert",
            displayName = "Fallback LiteRT",
            family = "fastvlm-litert",
            runtime = ModelRuntime.LITERT_LM,
            files = listOf(ModelFile("fallback.litertlm", 100L, ModelFileType.LITERT_BUNDLE, "https://example.com/fallback.litertlm")),
            minRamMb = 4096,
            isBuiltin = true,
            supportsVision = true,
        )

        val result = ModelDeviceSelector.selectChromatogramModel(
            models = listOf(fallback),
            profile = ModelDeviceProfile(model = "generic phone"),
        )

        assertEquals("fallback-litert", result.selectedModelId)
        assertEquals(ModelSelectionReason.CHROMATOGRAM_PRIORITY_FALLBACK, result.reason)
        assertEquals("generic_e2b_not_downloaded", result.fallbackResult)
    }
}

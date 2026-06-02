package com.chromalab.feature.processing.model

import com.chromalab.feature.processing.inference.ModelRuntime

data class ModelDeviceProfile(
    val manufacturer: String = "",
    val brand: String = "",
    val model: String = "",
    val hardware: String = "",
    val board: String = "",
    val product: String = "",
    val supportedAbis: List<String> = emptyList(),
)

enum class ModelSelectionReason {
    EXPLICIT_SELECTION,
    DEVICE_SPECIFIC_MATCH,
    GENERIC_E2B_FAST_FALLBACK,
    CHROMATOGRAM_PRIORITY_FALLBACK,
    NO_COMPATIBLE_MODEL,
}

data class ModelSelectionResult(
    val selectedModelId: String?,
    val selectedDeviceTarget: ModelDeviceTarget?,
    val detectedDeviceTarget: ModelDeviceTarget,
    val reason: ModelSelectionReason,
    val fallbackModelAttempted: Boolean = false,
    val fallbackResult: String? = null,
    val rejectedModelIds: List<String> = emptyList(),
)

object ModelDeviceSelector {
    fun detectDeviceTarget(profile: ModelDeviceProfile): ModelDeviceTarget {
        val signal = listOf(
            profile.manufacturer,
            profile.brand,
            profile.model,
            profile.hardware,
            profile.board,
            profile.product,
        ).joinToString(" ").lowercase()

        return when {
            signal.contains("sm8750") || signal.contains("snapdragon 8 elite") ->
                ModelDeviceTarget.QUALCOMM_SM8750
            signal.contains("qcs8275") || signal.contains("dragonwing iq8") ->
                ModelDeviceTarget.QUALCOMM_QCS8275
            signal.contains("tensor g5") || signal.contains("gs501") ->
                ModelDeviceTarget.GOOGLE_TENSOR_G5
            else -> ModelDeviceTarget.GENERIC
        }
    }

    fun isAllowedOnDevice(model: ModelInfo, profile: ModelDeviceProfile): Boolean {
        val target = detectDeviceTarget(profile)
        return model.deviceTarget == ModelDeviceTarget.GENERIC || model.deviceTarget == target
    }

    fun selectChromatogramModel(
        models: List<ModelInfo>,
        profile: ModelDeviceProfile,
        explicitModelId: String? = null,
        runtimeSmokePassedModelIds: Set<String> = emptySet(),
    ): ModelSelectionResult {
        val detectedTarget = detectDeviceTarget(profile)
        val rejected = models
            .filterNot { isAllowedOnDevice(it, profile) }
            .map { it.id }

        if (explicitModelId != null) {
            val explicit = models.firstOrNull { it.id == explicitModelId && isAllowedOnDevice(it, profile) }
            return if (explicit != null) {
                ModelSelectionResult(
                    selectedModelId = explicit.id,
                    selectedDeviceTarget = explicit.deviceTarget,
                    detectedDeviceTarget = detectedTarget,
                    reason = ModelSelectionReason.EXPLICIT_SELECTION,
                    fallbackModelAttempted = false,
                    rejectedModelIds = rejected,
                )
            } else {
                ModelSelectionResult(
                    selectedModelId = null,
                    selectedDeviceTarget = null,
                    detectedDeviceTarget = detectedTarget,
                    reason = ModelSelectionReason.NO_COMPATIBLE_MODEL,
                    fallbackModelAttempted = true,
                    fallbackResult = "explicit_model_missing_or_device_mismatch:$explicitModelId",
                    rejectedModelIds = rejected,
                )
            }
        }

        val allowed = models.filter { isAllowedOnDevice(it, profile) }
        val deviceSpecific = allowed
            .filter {
                it.runtime == ModelRuntime.LITERT_LM &&
                    it.deploymentMode == ModelDeploymentMode.FAST &&
                    it.deviceTarget == detectedTarget &&
                    it.deviceTarget != ModelDeviceTarget.GENERIC &&
                    (!it.requiresDownloadSmokeCheck || it.id in runtimeSmokePassedModelIds)
            }
            .minByOrNull { it.totalSizeBytes }

        if (deviceSpecific != null) {
            return ModelSelectionResult(
                selectedModelId = deviceSpecific.id,
                selectedDeviceTarget = deviceSpecific.deviceTarget,
                detectedDeviceTarget = detectedTarget,
                reason = ModelSelectionReason.DEVICE_SPECIFIC_MATCH,
                fallbackModelAttempted = false,
                rejectedModelIds = rejected,
            )
        }

        val genericE2B = allowed.firstOrNull { it.id == "gemma4-e2b" }
        if (genericE2B != null) {
            return ModelSelectionResult(
                selectedModelId = genericE2B.id,
                selectedDeviceTarget = genericE2B.deviceTarget,
                detectedDeviceTarget = detectedTarget,
                reason = ModelSelectionReason.GENERIC_E2B_FAST_FALLBACK,
                fallbackModelAttempted = detectedTarget != ModelDeviceTarget.GENERIC,
                fallbackResult = if (detectedTarget == ModelDeviceTarget.GENERIC) {
                    null
                } else {
                    "device_specific_bundle_missing_or_runtime_smoke_not_passed"
                },
                rejectedModelIds = rejected,
            )
        }

        val priorityFallback = allowed
            .sortedWith(compareBy<ModelInfo> { ModelRegistry.chromatogramVisionPriority(it) }.thenBy { it.totalSizeBytes })
            .firstOrNull()

        return if (priorityFallback != null) {
            ModelSelectionResult(
                selectedModelId = priorityFallback.id,
                selectedDeviceTarget = priorityFallback.deviceTarget,
                detectedDeviceTarget = detectedTarget,
                reason = ModelSelectionReason.CHROMATOGRAM_PRIORITY_FALLBACK,
                fallbackModelAttempted = true,
                fallbackResult = "generic_e2b_not_downloaded",
                rejectedModelIds = rejected,
            )
        } else {
            ModelSelectionResult(
                selectedModelId = null,
                selectedDeviceTarget = null,
                detectedDeviceTarget = detectedTarget,
                reason = ModelSelectionReason.NO_COMPATIBLE_MODEL,
                fallbackModelAttempted = true,
                fallbackResult = "no_allowed_downloaded_model",
                rejectedModelIds = rejected,
            )
        }
    }
}

package com.chromalab.feature.processing.inference

import kotlinx.serialization.Serializable

@Serializable
enum class LiteRtMtpModelSupport {
    YES,
    NO,
    UNKNOWN,
}

@Serializable
enum class LiteRtMtpRuntimeExposure {
    EXPOSED,
    NOT_EXPOSED,
    UNKNOWN,
}

@Serializable
enum class LiteRtMtpEnabledState {
    ENABLED,
    DISABLED,
    UNAVAILABLE,
}

@Serializable
data class LiteRtMtpCapabilityDiagnostic(
    val modelSupportsMtp: LiteRtMtpModelSupport,
    val runtimeExposesMtp: LiteRtMtpRuntimeExposure,
    val mtpEnabled: LiteRtMtpEnabledState,
    val reason: String,
)

@Serializable
data class LiteRtPerformanceDiagnostic(
    val loadTimeMillis: Long? = null,
    val firstResponseLatencyMillis: Long? = null,
    val totalResponseDurationMillis: Long? = null,
    val timeoutMillis: Long? = null,
    val timedOut: Boolean = false,
    val responseChars: Int? = null,
)

@Serializable
data class LiteRtRuntimeDiagnostics(
    val runtimeId: String = "litert-lm",
    val backendName: String? = null,
    val mtpCapability: LiteRtMtpCapabilityDiagnostic = LiteRtMtpCapabilityPolicy.evaluate(
        modelSupportsSpeculativeDecoding = null,
        runtimeExposesSpeculativeDecoding = false,
        runtimeFlagEnabled = null,
    ),
    val performance: LiteRtPerformanceDiagnostic = LiteRtPerformanceDiagnostic(),
)

object LiteRtMtpCapabilityPolicy {
    fun evaluate(
        modelSupportsSpeculativeDecoding: Boolean?,
        runtimeExposesSpeculativeDecoding: Boolean,
        runtimeFlagEnabled: Boolean?,
    ): LiteRtMtpCapabilityDiagnostic {
        if (!runtimeExposesSpeculativeDecoding) {
            return LiteRtMtpCapabilityDiagnostic(
                modelSupportsMtp = LiteRtMtpModelSupport.UNKNOWN,
                runtimeExposesMtp = LiteRtMtpRuntimeExposure.NOT_EXPOSED,
                mtpEnabled = LiteRtMtpEnabledState.UNAVAILABLE,
                reason = "litert_api_no_speculative_decoding_control",
            )
        }

        val modelSupport = when (modelSupportsSpeculativeDecoding) {
            true -> LiteRtMtpModelSupport.YES
            false -> LiteRtMtpModelSupport.NO
            null -> LiteRtMtpModelSupport.UNKNOWN
        }

        if (modelSupport == LiteRtMtpModelSupport.NO) {
            return LiteRtMtpCapabilityDiagnostic(
                modelSupportsMtp = modelSupport,
                runtimeExposesMtp = LiteRtMtpRuntimeExposure.EXPOSED,
                mtpEnabled = LiteRtMtpEnabledState.UNAVAILABLE,
                reason = "model_no_speculative_decoding_support",
            )
        }

        if (modelSupport == LiteRtMtpModelSupport.UNKNOWN) {
            return LiteRtMtpCapabilityDiagnostic(
                modelSupportsMtp = modelSupport,
                runtimeExposesMtp = LiteRtMtpRuntimeExposure.EXPOSED,
                mtpEnabled = LiteRtMtpEnabledState.DISABLED,
                reason = "model_speculative_decoding_support_unknown",
            )
        }

        return if (runtimeFlagEnabled == true) {
            LiteRtMtpCapabilityDiagnostic(
                modelSupportsMtp = modelSupport,
                runtimeExposesMtp = LiteRtMtpRuntimeExposure.EXPOSED,
                mtpEnabled = LiteRtMtpEnabledState.ENABLED,
                reason = "experimental_speculative_decoding_flag_enabled",
            )
        } else {
            LiteRtMtpCapabilityDiagnostic(
                modelSupportsMtp = modelSupport,
                runtimeExposesMtp = LiteRtMtpRuntimeExposure.EXPOSED,
                mtpEnabled = LiteRtMtpEnabledState.DISABLED,
                reason = "experimental_speculative_decoding_flag_disabled",
            )
        }
    }
}

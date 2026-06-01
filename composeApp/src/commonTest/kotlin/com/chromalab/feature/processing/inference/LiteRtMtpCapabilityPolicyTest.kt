package com.chromalab.feature.processing.inference

import kotlin.test.Test
import kotlin.test.assertEquals

class LiteRtMtpCapabilityPolicyTest {
    @Test
    fun marksMtpUnavailableWhenRuntimeDoesNotExposeControl() {
        val diagnostic = LiteRtMtpCapabilityPolicy.evaluate(
            modelSupportsSpeculativeDecoding = null,
            runtimeExposesSpeculativeDecoding = false,
            runtimeFlagEnabled = null,
        )

        assertEquals(LiteRtMtpModelSupport.UNKNOWN, diagnostic.modelSupportsMtp)
        assertEquals(LiteRtMtpRuntimeExposure.NOT_EXPOSED, diagnostic.runtimeExposesMtp)
        assertEquals(LiteRtMtpEnabledState.UNAVAILABLE, diagnostic.mtpEnabled)
        assertEquals("litert_api_no_speculative_decoding_control", diagnostic.reason)
    }

    @Test
    fun keepsMtpDisabledWhenModelSupportIsUnknown() {
        val diagnostic = LiteRtMtpCapabilityPolicy.evaluate(
            modelSupportsSpeculativeDecoding = null,
            runtimeExposesSpeculativeDecoding = true,
            runtimeFlagEnabled = false,
        )

        assertEquals(LiteRtMtpModelSupport.UNKNOWN, diagnostic.modelSupportsMtp)
        assertEquals(LiteRtMtpRuntimeExposure.EXPOSED, diagnostic.runtimeExposesMtp)
        assertEquals(LiteRtMtpEnabledState.DISABLED, diagnostic.mtpEnabled)
        assertEquals("model_speculative_decoding_support_unknown", diagnostic.reason)
    }

    @Test
    fun marksMtpUnavailableWhenModelDoesNotSupportSpeculativeDecoding() {
        val diagnostic = LiteRtMtpCapabilityPolicy.evaluate(
            modelSupportsSpeculativeDecoding = false,
            runtimeExposesSpeculativeDecoding = true,
            runtimeFlagEnabled = true,
        )

        assertEquals(LiteRtMtpModelSupport.NO, diagnostic.modelSupportsMtp)
        assertEquals(LiteRtMtpRuntimeExposure.EXPOSED, diagnostic.runtimeExposesMtp)
        assertEquals(LiteRtMtpEnabledState.UNAVAILABLE, diagnostic.mtpEnabled)
        assertEquals("model_no_speculative_decoding_support", diagnostic.reason)
    }

    @Test
    fun reportsSupportedMtpAsDisabledWhenExperimentalFlagIsOff() {
        val diagnostic = LiteRtMtpCapabilityPolicy.evaluate(
            modelSupportsSpeculativeDecoding = true,
            runtimeExposesSpeculativeDecoding = true,
            runtimeFlagEnabled = false,
        )

        assertEquals(LiteRtMtpModelSupport.YES, diagnostic.modelSupportsMtp)
        assertEquals(LiteRtMtpRuntimeExposure.EXPOSED, diagnostic.runtimeExposesMtp)
        assertEquals(LiteRtMtpEnabledState.DISABLED, diagnostic.mtpEnabled)
        assertEquals("experimental_speculative_decoding_flag_disabled", diagnostic.reason)
    }

    @Test
    fun reportsSupportedMtpAsEnabledOnlyWhenExperimentalFlagIsOn() {
        val diagnostic = LiteRtMtpCapabilityPolicy.evaluate(
            modelSupportsSpeculativeDecoding = true,
            runtimeExposesSpeculativeDecoding = true,
            runtimeFlagEnabled = true,
        )

        assertEquals(LiteRtMtpModelSupport.YES, diagnostic.modelSupportsMtp)
        assertEquals(LiteRtMtpRuntimeExposure.EXPOSED, diagnostic.runtimeExposesMtp)
        assertEquals(LiteRtMtpEnabledState.ENABLED, diagnostic.mtpEnabled)
        assertEquals("experimental_speculative_decoding_flag_enabled", diagnostic.reason)
    }
}

package com.chromalab.feature.chat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChatMtpRuntimeProfileTest {
    @Test
    fun mobileMtpDraftWindowUsesCurrentSafeDefaults() {
        assertEquals(
            3,
            ChatMtpRuntimeProfile.coerceDraftTokens(
                requestedDraftTokens = 6,
                selectedAccelerator = ChatRuntimeAccelerator.CPU,
                isConservativeDevice = false,
            ),
        )
        assertEquals(
            2,
            ChatMtpRuntimeProfile.coerceDraftTokens(
                requestedDraftTokens = 6,
                selectedAccelerator = ChatRuntimeAccelerator.VULKAN,
                isConservativeDevice = false,
            ),
        )
        assertEquals(
            2,
            ChatMtpRuntimeProfile.coerceDraftTokens(
                requestedDraftTokens = 6,
                selectedAccelerator = ChatRuntimeAccelerator.CPU,
                isConservativeDevice = true,
            ),
        )
    }

    @Test
    fun androidMtpIsOnlyAutomaticForExplicitVulkanProfile() {
        assertFalse(
            ChatMtpRuntimeProfile.shouldEnableMtp(
                selectedAccelerator = ChatRuntimeAccelerator.CPU,
                isConservativeDevice = false,
            ),
        )
        assertFalse(
            ChatMtpRuntimeProfile.shouldEnableMtp(
                selectedAccelerator = ChatRuntimeAccelerator.AUTO,
                isConservativeDevice = false,
            ),
        )
        assertFalse(
            ChatMtpRuntimeProfile.shouldEnableMtp(
                selectedAccelerator = ChatRuntimeAccelerator.VULKAN,
                isConservativeDevice = true,
            ),
        )
        assertTrue(
            ChatMtpRuntimeProfile.shouldEnableMtp(
                selectedAccelerator = ChatRuntimeAccelerator.VULKAN,
                isConservativeDevice = false,
            ),
        )
    }

    @Test
    fun mobileMtpContextAndBatchAreBoundedForPromptPrefill() {
        assertEquals(
            4096,
            ChatMtpRuntimeProfile.coerceContextTokens(
                requestedContextTokens = 32768,
                modelContextLimit = 32768,
                isConservativeDevice = false,
            ),
        )
        assertEquals(
            2048,
            ChatMtpRuntimeProfile.coerceContextTokens(
                requestedContextTokens = 32768,
                modelContextLimit = 32768,
                isConservativeDevice = true,
            ),
        )
        assertEquals(128, ChatMtpRuntimeProfile.coerceBatchTokens(512, isConservativeDevice = false))
        assertEquals(64, ChatMtpRuntimeProfile.coerceBatchTokens(512, isConservativeDevice = true))
    }
}

package com.chromalab.feature.processing.inference

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GgufTextPromptFormatterTest {
    @Test
    fun chatMlStyleWrapsRawChatTranscript() {
        val formatted = formatGgufTextPrompt(
            prompt = """
                You are a concise assistant.

                User: Hello
                Assistant:
            """.trimIndent(),
            style = PromptStyle.CHATML,
        )

        assertTrue(formatted.startsWith("<|im_start|>system\nYou are a concise assistant."))
        assertTrue(formatted.contains("<|im_start|>user\nUser: Hello"))
        assertTrue(formatted.endsWith("<|im_start|>assistant\n"))
        assertFalse(formatted.contains("Assistant:\n<|im_end|>"))
    }

    @Test
    fun existingTemplateIsNotWrappedAgain() {
        val prompt = "<|im_start|>user\nHello<|im_end|>\n<|im_start|>assistant\n"

        assertEquals(prompt, formatGgufTextPrompt(prompt, PromptStyle.CHATML))
    }

    @Test
    fun rawStyleLeavesPromptUntouched() {
        val prompt = "User: Hello\nAssistant:"

        assertEquals(prompt, formatGgufTextPrompt(prompt, PromptStyle.RAW))
    }
}

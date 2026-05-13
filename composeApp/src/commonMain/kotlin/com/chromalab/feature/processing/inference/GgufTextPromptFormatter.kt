package com.chromalab.feature.processing.inference

internal fun formatGgufTextPrompt(prompt: String, style: PromptStyle): String {
    val trimmed = prompt.trim()
    if (trimmed.isEmpty() || trimmed.containsKnownRoleTemplate()) return prompt

    return when (style) {
        PromptStyle.CHATML -> trimmed.toChatMlPrompt()
        PromptStyle.TRIGGER,
        PromptStyle.DEEPSEEK_OCR,
        PromptStyle.DIRECT_QUESTION,
        PromptStyle.RAW,
        PromptStyle.LITERT -> prompt
    }
}

private fun String.containsKnownRoleTemplate(): Boolean =
    contains("<|im_start|>") ||
        contains("<|start_header_id|>") ||
        contains("[INST]") ||
        contains("<start_of_turn>")

private fun String.toChatMlPrompt(): String {
    val content = removeTrailingAssistantCue()
    val firstUserIndex = Regex("(?m)^User:").find(content)?.range?.first
    val system = firstUserIndex
        ?.takeIf { it > 0 }
        ?.let { content.substring(0, it).trim() }
    val user = firstUserIndex
        ?.let { content.substring(it).trim() }
        ?: content

    return buildString {
        if (!system.isNullOrBlank()) {
            append("<|im_start|>system\n")
            append(system)
            append("\n<|im_end|>\n")
        }
        append("<|im_start|>user\n")
        append(user)
        append("\n<|im_end|>\n")
        append("<|im_start|>assistant\n")
    }
}

private fun String.removeTrailingAssistantCue(): String {
    val withoutCue = replace(Regex("(?m)\\n?Assistant:\\s*$"), "")
    return withoutCue.trim()
}

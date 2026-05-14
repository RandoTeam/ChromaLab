package com.chromalab.feature.chat

import androidx.compose.runtime.Composable
import kotlinx.serialization.Serializable

@Serializable
enum class ChatRole {
    USER,
    ASSISTANT,
}

@Serializable
data class ChatSettings(
    val systemPrompt: String = "You are a concise, helpful on-device assistant.",
    val temperature: Float = 0.7f,
    val topP: Float = 0.95f,
    val topK: Int = 40,
    val maxTokens: Int = 1024,
    val repeatPenalty: Float = 1.05f,
    val repeatLastN: Int = 128,
    val enableThinking: Boolean = false,
)

@Serializable
data class ChatSession(
    val id: String,
    val title: String,
    val modelId: String? = null,
    val modelName: String? = null,
    val runtimeAccelerator: ChatRuntimeAccelerator = ChatRuntimeAccelerator.AUTO,
    val settings: ChatSettings = ChatSettings(),
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class ChatMessage(
    val id: String,
    val chatId: String,
    val role: ChatRole,
    val content: String,
    val thinkingContent: String = "",
    val createdAt: Long,
    val modelName: String? = null,
    val isStreaming: Boolean = false,
    val stats: ChatMessageStats? = null,
)

@Serializable
data class ChatMessageStats(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
    val durationMs: Long = 0,
    val tokensPerSecond: Double = 0.0,
    val modelName: String? = null,
    val backendLabel: String? = null,
    val acceleratorLabel: String? = null,
)

@Serializable
data class ChatArchive(
    val sessions: List<ChatSession> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
)

data class ChatGenerationPartial(
    val contentDelta: String = "",
    val thinkingDelta: String = "",
)

data class ChatState(
    val sessions: List<ChatSession> = emptyList(),
    val selectedChatId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val activeModelId: String? = null,
    val activeModelName: String? = null,
    val isGenerating: Boolean = false,
    val error: String? = null,
) {
    val selectedSession: ChatSession?
        get() = sessions.firstOrNull { it.id == selectedChatId }
}

data class ChatModelOption(
    val id: String,
    val name: String,
    val summary: String,
    val runtime: ChatRuntimeUiState = ChatRuntimeUiState(),
    val isActive: Boolean,
    val isActivating: Boolean = false,
)

enum class ChatRuntimeBackend(
    val label: String,
) {
    LITERT_LM("LiteRT-LM"),
    LLAMA_CPP("GGUF / llama.cpp"),
    IMPORTED("Imported"),
    UNKNOWN("Unknown"),
}

@Serializable
enum class ChatRuntimeAccelerator(
    val label: String,
) {
    AUTO("Auto"),
    CPU("CPU"),
    GPU("GPU"),
    NPU("NPU"),
    VULKAN("Vulkan"),
}

data class ChatModelCapabilities(
    val supportsTextChat: Boolean = false,
    val supportsVisionInput: Boolean = false,
    val supportsChromatogramAnalysis: Boolean = false,
    val supportsRuntimeSelection: Boolean = false,
    val supportsThinking: Boolean = false,
    val supportsNativeStreaming: Boolean = false,
)

data class ChatThinkingUiState(
    val modelSupportsThinking: Boolean = false,
    val runtimeCanExposeThinking: Boolean = false,
    val isEnabled: Boolean = false,
    val unavailableReason: String? = null,
) {
    val isSupported: Boolean
        get() = modelSupportsThinking && runtimeCanExposeThinking
}

data class ChatModelCompatibility(
    val isSelectableForChat: Boolean = true,
    val reason: String? = null,
)

data class ChatRuntimeUiState(
    val backend: ChatRuntimeBackend = ChatRuntimeBackend.UNKNOWN,
    val backendLabel: String = backend.label,
    val supportedAccelerators: List<ChatRuntimeAccelerator> = listOf(ChatRuntimeAccelerator.CPU),
    val selectedAccelerator: ChatRuntimeAccelerator = ChatRuntimeAccelerator.AUTO,
    val capabilities: ChatModelCapabilities = ChatModelCapabilities(),
    val thinking: ChatThinkingUiState = ChatThinkingUiState(),
    val compatibility: ChatModelCompatibility = ChatModelCompatibility(),
) {
    val acceleratorLabel: String
        get() = supportedAccelerators.joinToString("/") { it.label }

    val capabilitySummary: String
        get() = buildList {
            add(backendLabel)
            if (supportedAccelerators.isNotEmpty()) add(acceleratorLabel)
            if (capabilities.supportsVisionInput) add("Vision")
            if (capabilities.supportsChromatogramAnalysis) add("Chromatograms")
            if (thinking.isSupported) add("Thinking")
        }.joinToString(" · ")
}

data class ChatActions(
    val createChat: () -> Unit = {},
    val selectChat: (String?) -> Unit = {},
    val deleteChat: (String) -> Unit = {},
    val setChatModel: (String, String, String, ChatRuntimeAccelerator) -> Unit = { _, _, _, _ -> },
    val setChatRuntimeAccelerator: (String, ChatRuntimeAccelerator) -> Unit = { _, _ -> },
    val updateSettings: (String, ChatSettings) -> Unit = { _, _ -> },
    val sendMessage: (String) -> Unit = {},
    val stopGeneration: () -> Unit = {},
    val clearError: () -> Unit = {},
)

interface ChatRepository {
    suspend fun load(): ChatArchive
    suspend fun save(archive: ChatArchive)
}

interface ChatTextGenerator {
    suspend fun generate(
        messages: List<ChatMessage>,
        settings: ChatSettings,
        modelId: String,
        modelName: String?,
        runtimeAccelerator: ChatRuntimeAccelerator,
        onPartial: (ChatGenerationPartial) -> Unit,
    ): String
}

expect fun chatNowMillis(): Long

@Composable
expect fun rememberChatState(
    activeModelId: String?,
    activeModelName: String?,
): Pair<ChatState, ChatActions>

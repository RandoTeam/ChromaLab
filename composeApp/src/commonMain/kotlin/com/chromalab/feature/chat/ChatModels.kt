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
)

@Serializable
data class ChatSession(
    val id: String,
    val title: String,
    val modelId: String? = null,
    val modelName: String? = null,
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
    val createdAt: Long,
    val modelName: String? = null,
)

@Serializable
data class ChatArchive(
    val sessions: List<ChatSession> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
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
    val isActive: Boolean,
    val isActivating: Boolean = false,
)

data class ChatActions(
    val createChat: () -> Unit = {},
    val selectChat: (String?) -> Unit = {},
    val deleteChat: (String) -> Unit = {},
    val setChatModel: (String, String, String) -> Unit = { _, _, _ -> },
    val updateSettings: (String, ChatSettings) -> Unit = { _, _ -> },
    val sendMessage: (String) -> Unit = {},
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
        activeModelName: String?,
    ): String
}

expect fun chatNowMillis(): Long

@Composable
expect fun rememberChatState(
    activeModelId: String?,
    activeModelName: String?,
): Pair<ChatState, ChatActions>

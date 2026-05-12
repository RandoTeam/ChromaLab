package com.chromalab.feature.chat

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatController(
    private val repository: ChatRepository,
    private val generator: ChatTextGenerator,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var archive = ChatArchive()

    init {
        scope.launch {
            archive = repository.load()
            val selected = archive.sessions.maxByOrNull { it.updatedAt }?.id
            publish(selected)
        }
    }

    fun setActiveModel(modelId: String?, modelName: String?) {
        _state.update { it.copy(activeModelId = modelId, activeModelName = modelName) }
    }

    fun createChat() {
        scope.launch {
            val now = chatNowMillis()
            val session = ChatSession(
                id = "chat_${now}_${archive.sessions.size}",
                title = "Новый чат",
                modelId = state.value.activeModelId,
                modelName = state.value.activeModelName,
                createdAt = now,
                updatedAt = now,
            )
            archive = archive.copy(sessions = listOf(session) + archive.sessions)
            persist()
            publish(session.id)
        }
    }

    fun selectChat(chatId: String?) {
        publish(chatId)
    }

    fun deleteChat(chatId: String) {
        scope.launch {
            archive = archive.copy(
                sessions = archive.sessions.filterNot { it.id == chatId },
                messages = archive.messages.filterNot { it.chatId == chatId },
            )
            persist()
            val selected = if (state.value.selectedChatId == chatId) {
                archive.sessions.maxByOrNull { it.updatedAt }?.id
            } else {
                state.value.selectedChatId
            }
            publish(selected)
        }
    }

    fun updateSettings(chatId: String, settings: ChatSettings) {
        scope.launch {
            val now = chatNowMillis()
            archive = archive.copy(
                sessions = archive.sessions.map {
                    if (it.id == chatId) it.copy(settings = settings, updatedAt = now) else it
                },
            )
            persist()
            publish(chatId)
        }
    }

    fun setChatModel(chatId: String, modelId: String, modelName: String) {
        scope.launch {
            val now = chatNowMillis()
            archive = archive.copy(
                sessions = archive.sessions.map {
                    if (it.id == chatId) {
                        it.copy(
                            modelId = modelId,
                            modelName = modelName,
                            updatedAt = now,
                        )
                    } else {
                        it
                    }
                },
            )
            persist()
            publish(chatId)
        }
    }

    fun sendMessage(text: String) {
        val content = text.trim()
        val chatId = state.value.selectedChatId ?: return
        if (content.isEmpty() || state.value.isGenerating) return

        scope.launch {
            val now = chatNowMillis()
            val userMessage = ChatMessage(
                id = "msg_${now}_user",
                chatId = chatId,
                role = ChatRole.USER,
                content = content,
                createdAt = now,
            )
            archive = archive.copy(
                sessions = archive.sessions.map {
                    if (it.id == chatId) {
                        it.copy(
                            title = if (it.title == "Новый чат") content.take(48) else it.title,
                            modelId = state.value.activeModelId ?: it.modelId,
                            modelName = state.value.activeModelName ?: it.modelName,
                            updatedAt = now,
                        )
                    } else {
                        it
                    }
                },
                messages = archive.messages + userMessage,
            )
            persist()
            publish(chatId, isGenerating = true)

            val session = archive.sessions.firstOrNull { it.id == chatId }
            val contextMessages = archive.messages.filter { it.chatId == chatId }
            val response = runCatching {
                withContext(Dispatchers.Default) {
                    generator.generate(
                        messages = contextMessages,
                        settings = session?.settings ?: ChatSettings(),
                        activeModelName = state.value.activeModelName,
                    )
                }
            }

            response.fold(
                onSuccess = { answer ->
                    val done = chatNowMillis()
                    archive = archive.copy(
                        sessions = archive.sessions.map {
                            if (it.id == chatId) it.copy(updatedAt = done) else it
                        },
                        messages = archive.messages + ChatMessage(
                            id = "msg_${done}_assistant",
                            chatId = chatId,
                            role = ChatRole.ASSISTANT,
                            content = answer.ifBlank { "Модель вернула пустой ответ." },
                            createdAt = done,
                            modelName = state.value.activeModelName,
                        ),
                    )
                    persist()
                    publish(chatId)
                },
                onFailure = { error ->
                    publish(chatId, error = error.message ?: "Ошибка генерации")
                },
            )
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private suspend fun persist() {
        repository.save(archive)
    }

    private fun publish(
        selectedChatId: String?,
        isGenerating: Boolean = false,
        error: String? = null,
    ) {
        val messages = selectedChatId
            ?.let { id -> archive.messages.filter { it.chatId == id }.sortedBy { it.createdAt } }
            ?: emptyList()
        _state.update {
            it.copy(
                sessions = archive.sessions.sortedByDescending { session -> session.updatedAt },
                selectedChatId = selectedChatId,
                messages = messages,
                isGenerating = isGenerating,
                error = error,
            )
        }
    }
}

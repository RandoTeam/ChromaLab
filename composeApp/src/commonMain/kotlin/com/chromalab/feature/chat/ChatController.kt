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

    fun setChatModel(
        chatId: String,
        modelId: String,
        modelName: String,
        runtimeAccelerator: ChatRuntimeAccelerator,
    ) {
        scope.launch {
            val now = chatNowMillis()
            archive = archive.copy(
                sessions = archive.sessions.map {
                    if (it.id == chatId) {
                        it.copy(
                            modelId = modelId,
                            modelName = modelName,
                            runtimeAccelerator = runtimeAccelerator,
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

    fun setChatRuntimeAccelerator(
        chatId: String,
        runtimeAccelerator: ChatRuntimeAccelerator,
    ) {
        scope.launch {
            val now = chatNowMillis()
            archive = archive.copy(
                sessions = archive.sessions.map {
                    if (it.id == chatId) {
                        it.copy(runtimeAccelerator = runtimeAccelerator, updatedAt = now)
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
            val session = archive.sessions.firstOrNull { it.id == chatId }
            val selectedModelId = session?.modelId ?: state.value.activeModelId
            val selectedModelName = session?.modelName ?: state.value.activeModelName
            val selectedRuntimeAccelerator = session?.runtimeAccelerator ?: ChatRuntimeAccelerator.AUTO
            if (selectedModelId == null) {
                publish(chatId, error = "Выберите модель чата перед отправкой сообщения.")
                return@launch
            }

            val assistantId = "msg_${now}_assistant"
            val userMessage = ChatMessage(
                id = "msg_${now}_user",
                chatId = chatId,
                role = ChatRole.USER,
                content = content,
                createdAt = now,
            )
            val assistantMessage = ChatMessage(
                id = assistantId,
                chatId = chatId,
                role = ChatRole.ASSISTANT,
                content = "",
                createdAt = now + 1,
                modelName = selectedModelName,
                isStreaming = true,
            )

            archive = archive.copy(
                sessions = archive.sessions.map {
                    if (it.id == chatId) {
                        it.copy(
                            title = if (it.title == "Новый чат") content.take(48) else it.title,
                            modelId = selectedModelId,
                            modelName = selectedModelName ?: it.modelName,
                            updatedAt = now,
                        )
                    } else {
                        it
                    }
                },
                messages = archive.messages + userMessage + assistantMessage,
            )
            persist()
            publish(chatId, isGenerating = true)

            val settings = session?.settings ?: ChatSettings()
            val contextMessages = archive.messages.filter { it.chatId == chatId && it.id != assistantId }
            val promptTokens = estimatePromptTokens(contextMessages, settings)
            val startedAt = chatNowMillis()

            val response = runCatching {
                withContext(Dispatchers.Default) {
                    generator.generate(
                        messages = contextMessages,
                        settings = settings,
                        modelId = selectedModelId,
                        modelName = selectedModelName,
                        runtimeAccelerator = selectedRuntimeAccelerator,
                        onPartial = { chunk ->
                            if (chunk.isNotEmpty()) {
                                updateMessage(chatId, assistantId) { message ->
                                    message.copy(content = message.content + chunk)
                                }
                                publish(chatId, isGenerating = true)
                            }
                        },
                    )
                }
            }

            response.fold(
                onSuccess = { answer ->
                    val done = chatNowMillis()
                    val finalAnswer = answer.ifBlank {
                        archive.messages.firstOrNull { it.id == assistantId }?.content.orEmpty()
                    }.ifBlank {
                        "Модель вернула пустой ответ."
                    }
                    val completionTokens = estimateTokens(finalAnswer)
                    val durationMs = (done - startedAt).coerceAtLeast(1)
                    val stats = ChatMessageStats(
                        promptTokens = promptTokens,
                        completionTokens = completionTokens,
                        totalTokens = promptTokens + completionTokens,
                        durationMs = durationMs,
                        tokensPerSecond = completionTokens * 1000.0 / durationMs,
                    )

                    archive = archive.copy(
                        sessions = archive.sessions.map {
                            if (it.id == chatId) it.copy(updatedAt = done) else it
                        },
                        messages = archive.messages.map { message ->
                            if (message.id == assistantId) {
                                message.copy(
                                    content = finalAnswer,
                                    isStreaming = false,
                                    stats = stats,
                                    modelName = selectedModelName,
                                )
                            } else {
                                message
                            }
                        },
                    )
                    persist()
                    publish(chatId)
                },
                onFailure = { error ->
                    val message = error.message ?: "Ошибка генерации"
                    updateMessage(chatId, assistantId) {
                        it.copy(
                            content = "Ошибка генерации: $message",
                            isStreaming = false,
                        )
                    }
                    persist()
                    publish(chatId, error = message)
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

    private fun updateMessage(
        chatId: String,
        messageId: String,
        transform: (ChatMessage) -> ChatMessage,
    ) {
        archive = archive.copy(
            messages = archive.messages.map { message ->
                if (message.chatId == chatId && message.id == messageId) transform(message) else message
            },
        )
    }

    private fun estimatePromptTokens(
        messages: List<ChatMessage>,
        settings: ChatSettings,
    ): Int {
        val historyTokens = messages.takeLast(24).sumOf { estimateTokens(it.content) }
        return historyTokens + estimateTokens(settings.systemPrompt)
    }

    private fun estimateTokens(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0

        val wordEstimate = trimmed.splitToSequence(Regex("\\s+")).count()
        val charEstimate = (trimmed.length + 3) / 4
        return maxOf(1, maxOf(wordEstimate, charEstimate))
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

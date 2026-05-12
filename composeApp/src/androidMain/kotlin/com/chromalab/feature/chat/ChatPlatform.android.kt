package com.chromalab.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.chromalab.feature.processing.inference.GenerationOptions
import com.chromalab.feature.processing.inference.VlmEngineHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

actual fun chatNowMillis(): Long = System.currentTimeMillis()

@Composable
actual fun rememberChatState(
    activeModelId: String?,
    activeModelName: String?,
): Pair<ChatState, ChatActions> {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val controller = remember {
        ChatController(
            repository = JsonChatRepository(File(context.filesDir, "chats/chat_archive.json")),
            generator = AndroidChatTextGenerator(),
            scope = scope,
        )
    }
    LaunchedEffect(activeModelId, activeModelName) {
        controller.setActiveModel(activeModelId, activeModelName)
    }
    val state by controller.state.collectAsState()
    val actions = remember(controller) {
        ChatActions(
            createChat = controller::createChat,
            selectChat = controller::selectChat,
            deleteChat = controller::deleteChat,
            setChatModel = controller::setChatModel,
            updateSettings = controller::updateSettings,
            sendMessage = controller::sendMessage,
            clearError = controller::clearError,
        )
    }
    return state to actions
}

private class JsonChatRepository(
    private val file: File,
) : ChatRepository {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    override suspend fun load(): ChatArchive = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.exists()) return@withContext ChatArchive()
            json.decodeFromString(ChatArchive.serializer(), file.readText())
        }.getOrElse { error ->
            println("CHAT[STORE] Failed to load archive: ${error.message}")
            ChatArchive()
        }
    }

    override suspend fun save(archive: ChatArchive) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(ChatArchive.serializer(), archive))
    }
}

private class AndroidChatTextGenerator : ChatTextGenerator {
    override suspend fun generate(
        messages: List<ChatMessage>,
        settings: ChatSettings,
        activeModelName: String?,
    ): String {
        val engine = VlmEngineHolder.activeEngine
            ?: error("Active model is not loaded. Open Model Manager and activate a LiteRT or GGUF model.")

        VlmEngineHolder.isInferring = true
        return try {
            engine.inferRaw(
                imagePath = "__chromalab_text_only__",
                prompt = buildChatPrompt(messages, settings),
                options = settings.toGenerationOptions(),
            )
        } finally {
            VlmEngineHolder.isInferring = false
        }
    }
}

private fun buildChatPrompt(
    messages: List<ChatMessage>,
    settings: ChatSettings,
): String = buildString {
    appendLine(settings.systemPrompt.trim())
    appendLine()
    appendLine("Answer naturally and concisely. Preserve useful formatting when needed.")
    appendLine("Target response length: up to ${settings.maxTokens} tokens.")
    appendLine("Sampling preference: temperature=${"%.2f".format(settings.temperature)}, topP=${"%.2f".format(settings.topP)}, topK=${settings.topK}.")
    appendLine()
    messages.takeLast(24).forEach { message ->
        when (message.role) {
            ChatRole.USER -> appendLine("User: ${message.content}")
            ChatRole.ASSISTANT -> appendLine("Assistant: ${message.content}")
        }
    }
    append("Assistant:")
}

private fun ChatSettings.toGenerationOptions(): GenerationOptions =
    GenerationOptions(
        maxTokens = maxTokens.coerceIn(64, 4096),
        temperature = temperature.coerceIn(0f, 2f),
        topP = topP.coerceIn(0.05f, 1f),
        topK = topK.coerceIn(1, 256),
        repeatPenalty = 1.05f,
        repeatLastN = 128,
    )

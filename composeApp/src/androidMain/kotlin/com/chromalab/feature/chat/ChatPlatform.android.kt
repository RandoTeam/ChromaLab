package com.chromalab.feature.chat

import android.util.Log
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

private const val CHAT_TAG = "ChromaLabChat"

actual fun chatRuntimeLog(message: String) {
    Log.i(CHAT_TAG, message)
}

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
            setChatRuntimeAccelerator = controller::setChatRuntimeAccelerator,
            updateSettings = controller::updateSettings,
            sendMessage = controller::sendMessage,
            stopGeneration = controller::stopGeneration,
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
        modelId: String,
        modelName: String?,
        runtimeAccelerator: ChatRuntimeAccelerator,
        onPartial: (ChatGenerationPartial) -> Unit,
    ): String {
        chatRuntimeLog(
            "generate.start modelId=$modelId modelName=${modelName ?: "<unknown>"} " +
                "messages=${messages.size} accelerator=$runtimeAccelerator " +
                "ctx=${settings.contextSize} maxTokens=${settings.maxTokens} " +
                "mtpEnabled=${settings.enableMtp} requestedDraft=${settings.mtpDraftTokens}",
        )
        val controller = VlmEngineHolder.controller
            ?: error("Model controller is not ready.")
        val loaded = controller.activateForChat(
            modelId = modelId,
            runtimeAccelerator = runtimeAccelerator,
            contextSize = settings.contextSize,
            mtpDraftTokens = if (settings.enableMtp) settings.mtpDraftTokens.coerceIn(1, 6) else 0,
        )
        chatRuntimeLog("generate.activate.complete loaded=$loaded active=${VlmEngineHolder.activeModelDiagnostics()}")
        if (!loaded) {
            error("Unable to load chat model: ${modelName ?: modelId}")
        }

        val engine = VlmEngineHolder.activeEngine
            ?: error("Chat model is not loaded: ${modelName ?: modelId}")

        VlmEngineHolder.isInferring = true
        var partialCount = 0
        val startedAt = System.currentTimeMillis()
        val prompt = buildChatPrompt(messages, settings)
        return try {
            chatRuntimeLog(
                "generate.infer.start backend=${engine.getBackendName()} " +
                    "supportsImage=${engine.supportsImageInput()} promptChars=${prompt.length}",
            )
            engine.inferRawStreaming(
                imagePath = "__chromalab_text_only__",
                prompt = prompt,
                options = settings.toGenerationOptions(),
                onPartial = { chunk ->
                    partialCount += 1
                    if (partialCount == 1 || partialCount % 32 == 0) {
                        chatRuntimeLog(
                            "generate.partial count=$partialCount chars=${chunk.length} " +
                                "elapsedMs=${System.currentTimeMillis() - startedAt}",
                        )
                    }
                    onPartial(ChatGenerationPartial(contentDelta = chunk))
                },
            ).also { result ->
                chatRuntimeLog(
                    "generate.complete chars=${result.length} partials=$partialCount " +
                        "elapsedMs=${System.currentTimeMillis() - startedAt}",
                )
            }
        } catch (t: Throwable) {
            chatRuntimeLog("generate.error ${t::class.simpleName}: ${t.message}")
            throw t
        } finally {
            VlmEngineHolder.isInferring = false
            chatRuntimeLog("generate.end inferring=false partials=$partialCount elapsedMs=${System.currentTimeMillis() - startedAt}")
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
        maxTokens = maxTokens.coerceIn(64, maxOf(64, minOf(8192, contextSize / 2))),
        temperature = temperature.coerceIn(0f, 2f),
        topP = topP.coerceIn(0.05f, 1f),
        topK = topK.coerceIn(1, 256),
        repeatPenalty = repeatPenalty.coerceIn(0.8f, 1.5f),
        repeatLastN = repeatLastN.coerceIn(0, contextSize.coerceAtLeast(1024)),
        mtpDraftTokens = if (enableMtp) mtpDraftTokens.coerceIn(1, 6) else 0,
    )

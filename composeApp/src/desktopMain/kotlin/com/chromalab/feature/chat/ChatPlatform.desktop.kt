package com.chromalab.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    val scope = rememberCoroutineScope()
    val controller = remember {
        val home = System.getProperty("user.home") ?: "."
        ChatController(
            repository = JsonChatRepository(File(home, ".chromalab/chats/chat_archive.json")),
            generator = DesktopChatTextGenerator(),
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
        }.getOrElse { ChatArchive() }
    }

    override suspend fun save(archive: ChatArchive) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(ChatArchive.serializer(), archive))
    }
}

private class DesktopChatTextGenerator : ChatTextGenerator {
    override suspend fun generate(
        messages: List<ChatMessage>,
        settings: ChatSettings,
        modelId: String,
        modelName: String?,
        onPartial: (String) -> Unit,
    ): String {
        val response = "Desktop chat currently stores history only. On-device generation is wired on Android through LiteRT and GGUF."
        onPartial(response)
        return response
    }
}

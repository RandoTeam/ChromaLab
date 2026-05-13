package com.chromalab.feature.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val CHAT_STREAM_FADE_MS = 120

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatState,
    actions: ChatActions,
    modelOptions: List<ChatModelOption>,
    onOpenModelManager: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettings by remember { mutableStateOf(false) }
    var showModelPicker by remember { mutableStateOf(false) }
    val selected = state.selectedSession
    val selectedModelId = if (selected != null) selected.modelId else state.activeModelId
    val selectedModelName = if (selected != null) selected.modelName else state.activeModelName
    val selectedModelOption = modelOptions.firstOrNull { it.id == selectedModelId }
    val selectedAccelerator = selected?.runtimeAccelerator
        ?: selectedModelOption?.runtime?.selectedAccelerator
        ?: ChatRuntimeAccelerator.AUTO

    Scaffold(
        modifier = modifier,
        topBar = {
            ChatTopBar(
                selected = selected,
                selectedModelName = selectedModelName,
                selectedModelOption = selectedModelOption,
                isGenerating = state.isGenerating,
                onBack = { actions.selectChat(null) },
                onNewChat = actions.createChat,
                onOpenModelManager = onOpenModelManager,
                onModelPicker = { showModelPicker = true },
                onSettings = { showSettings = true },
            )
        },
        bottomBar = {
            if (selected != null) {
                ChatComposer(
                    enabled = !state.isGenerating && selected.modelId != null,
                    isGenerating = state.isGenerating,
                    onSend = actions.sendMessage,
                )
            }
        },
    ) { padding ->
        AnimatedContent(
            targetState = selected?.id,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "chat_content",
            modifier = Modifier.fillMaxSize().padding(padding),
        ) { selectedId ->
            if (selectedId == null) {
                ChatListContent(
                    state = state,
                    actions = actions,
                    onOpenModelManager = onOpenModelManager,
                )
            } else {
                ChatThreadContent(
                    state = state,
                    selected = selected,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (state.error != null) {
        ModalBottomSheet(
            onDismissRequest = actions.clearError,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text("Ошибка чата", style = MaterialTheme.typography.titleMedium)
                Text(state.error, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = actions.clearError, modifier = Modifier.align(Alignment.End)) {
                    Text("OK")
                }
            }
        }
    }

    if (showSettings && selected != null) {
        ChatSettingsSheet(
            settings = selected.settings,
            onDismiss = { showSettings = false },
            onApply = { settings ->
                actions.updateSettings(selected.id, settings)
                showSettings = false
            },
        )
    }

    if (showModelPicker && selected != null) {
        ChatModelPickerSheet(
            selectedModelId = selected.modelId,
            modelOptions = modelOptions,
            onSelectModel = { option ->
                actions.setChatModel(selected.id, option.id, option.name, option.runtime.selectedAccelerator)
                if (!option.runtime.thinking.isSupported && selected.settings.enableThinking) {
                    actions.updateSettings(selected.id, selected.settings.copy(enableThinking = false))
                }
                showModelPicker = false
            },
            selectedAccelerator = selectedAccelerator,
            onSelectAccelerator = { accelerator ->
                actions.setChatRuntimeAccelerator(selected.id, accelerator)
            },
            thinkingEnabled = selected.settings.enableThinking,
            onSetThinkingEnabled = { enabled ->
                actions.updateSettings(selected.id, selected.settings.copy(enableThinking = enabled))
            },
            onOpenModelManager = {
                showModelPicker = false
                onOpenModelManager()
            },
            onDismiss = { showModelPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    selected: ChatSession?,
    selectedModelName: String?,
    selectedModelOption: ChatModelOption?,
    isGenerating: Boolean,
    onBack: () -> Unit,
    onNewChat: () -> Unit,
    onOpenModelManager: () -> Unit,
    onModelPicker: () -> Unit,
    onSettings: () -> Unit,
) {
    CenterAlignedTopAppBar(
        navigationIcon = {
            if (selected != null) {
                IconButton(onClick = onBack, enabled = !isGenerating) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = selected?.title ?: "Чаты",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (selected != null) {
                    ChatModelChip(
                        modelName = selectedModelName,
                        option = selectedModelOption,
                        enabled = !isGenerating,
                        onClick = onModelPicker,
                    )
                } else {
                    Text(
                        text = selectedModelName ?: "Модель не выбрана",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        actions = {
            if (selected != null) {
                IconButton(onClick = onSettings, enabled = !isGenerating) {
                    Icon(Icons.Filled.Tune, contentDescription = "Настройки")
                }
            } else {
                IconButton(onClick = onOpenModelManager) {
                    Icon(Icons.Filled.SmartToy, contentDescription = "Модели")
                }
            }
            IconButton(onClick = onNewChat, enabled = !isGenerating) {
                Icon(Icons.Filled.Add, contentDescription = "Новый чат")
            }
        },
    )
}

@Composable
private fun ChatModelChip(
    modelName: String?,
    option: ChatModelOption?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.size(21.dp), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Filled.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (option?.isActivating == true) {
                CircularProgressIndicator(modifier = Modifier.size(21.dp), strokeWidth = 2.dp)
            }
        }
        Text(
            text = modelName ?: "Выбрать модель",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp),
        )
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChatListContent(
    state: ChatState,
    actions: ChatActions,
    onOpenModelManager: () -> Unit,
) {
    if (state.sessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                modifier = Modifier.padding(Spacing.lg),
            ) {
                Icon(
                    Icons.Filled.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text("Нет чатов", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Создайте чат и выберите модель из общего менеджера моделей.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = actions.createChat) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Новый чат")
                }
                TextButton(onClick = onOpenModelManager) {
                    Text("Менеджер моделей")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item {
            AssistChip(
                onClick = onOpenModelManager,
                label = { Text(state.activeModelName ?: "Модель не активна") },
                leadingIcon = {
                    Icon(Icons.Filled.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                },
            )
        }
        items(state.sessions, key = { it.id }) { session ->
            ChatSessionCard(
                session = session,
                onOpen = { actions.selectChat(session.id) },
                onDelete = { actions.deleteChat(session.id) },
            )
        }
    }
}

@Composable
private fun ChatSessionCard(
    session: ChatSession,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    session.modelName ?: "Модель не закреплена",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить")
            }
        }
    }
}

@Composable
private fun ChatThreadContent(
    state: ChatState,
    selected: ChatSession?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val lastMessageContent = state.messages.lastOrNull()?.content
    LaunchedEffect(state.messages.size, lastMessageContent, state.isGenerating) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (selected?.modelId == null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        "Выберите LiteRT или GGUF модель в настройках чата перед отправкой сообщения.",
                        modifier = Modifier.padding(Spacing.md),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        items(state.messages, key = { it.id }) { message ->
            MessageBubble(message)
        }
        if (state.isGenerating && state.messages.none { it.isStreaming }) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Модель отвечает...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    val contentModifier = if (isUser) {
        Modifier
            .fillMaxWidth(0.82f)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(24.dp),
            )
            .padding(Spacing.md)
    } else {
        Modifier
            .fillMaxWidth(0.92f)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = contentModifier,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                if (isUser) "Вы" else (message.modelName ?: "Ассистент"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isUser) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                StreamingMessageText(message)
            }
            if (!isUser && !message.isStreaming && message.stats != null) {
                Spacer(Modifier.height(2.dp))
                MessageStatsRow(message.stats)
            }
        }
    }
}

@Composable
private fun StreamingMessageText(message: ChatMessage) {
    var bufferedText by remember(message.id) {
        mutableStateOf(if (message.isStreaming) "" else message.content)
    }
    val latestContent by rememberUpdatedState(message.content)

    LaunchedEffect(message.id, message.isStreaming) {
        if (!message.isStreaming) {
            bufferedText = latestContent
            return@LaunchedEffect
        }

        while (true) {
            val next = latestContent
            if (next != bufferedText) {
                bufferedText = next
            }
            delay(CHAT_STREAM_FADE_MS.toLong())
        }
    }

    val text = when {
        bufferedText.isNotBlank() -> bufferedText
        message.isStreaming -> "Генерация..."
        else -> message.content
    }
    Crossfade(
        targetState = text,
        animationSpec = tween(durationMillis = CHAT_STREAM_FADE_MS),
        label = "chat_stream_text",
    ) { target ->
        val textStyle = if (target.looksLikeStructuredOutput()) {
            MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        } else {
            MaterialTheme.typography.bodyMedium
        }
        Text(
            text = target,
            modifier = Modifier.fillMaxWidth(),
            style = textStyle,
        )
    }
}

@Composable
private fun MessageStatsRow(stats: ChatMessageStats) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
        shape = RoundedCornerShape(24.dp),
    ) {
        Text(
            text = formatStatsText(stats),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatStatsText(stats: ChatMessageStats): String =
    buildList {
        stats.modelName?.takeIf { it.isNotBlank() }?.let(::add)
        stats.backendLabel?.takeIf { it.isNotBlank() }?.let(::add)
        stats.acceleratorLabel?.takeIf { it.isNotBlank() }?.let { add(it) }
        add("prompt ${stats.promptTokens} tok")
        add("answer ${stats.completionTokens} tok")
        add("total ${stats.totalTokens} tok")
        add(formatDuration(stats.durationMs))
        add("${formatRate(stats.tokensPerSecond)} tok/s")
    }.joinToString(" | ")

private fun String.looksLikeStructuredOutput(): Boolean {
    val trimmed = trim()
    if (trimmed.isEmpty()) return false
    if ("```" in trimmed) return true
    val lines = trimmed.lines()
    if (lines.any { it.count { char -> char == '|' } >= 2 }) return true
    return lines.any { line ->
        line.contains('\t') ||
            line.contains("Retention", ignoreCase = true) ||
            line.contains("Peak", ignoreCase = true) ||
            line.contains("Area", ignoreCase = true) ||
            line.contains("RT", ignoreCase = false)
    }
}

@Composable
private fun ChatComposer(
    enabled: Boolean,
    isGenerating: Boolean,
    onSend: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    Surface(
        modifier = Modifier.fillMaxWidth().imePadding(),
        tonalElevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                enabled = enabled,
                minLines = 1,
                maxLines = 5,
                placeholder = { Text(if (enabled) "Сообщение" else "Выберите активную модель") },
            )
            IconButton(
                enabled = enabled && text.isNotBlank() && !isGenerating,
                onClick = {
                    val message = text
                    text = ""
                    onSend(message)
                },
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Отправить")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatSettingsSheet(
    settings: ChatSettings,
    onDismiss: () -> Unit,
    onApply: (ChatSettings) -> Unit,
) {
    var local by remember(settings) { mutableStateOf(settings) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Text("Настройки чата", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item {
                OutlinedTextField(
                    value = local.systemPrompt,
                    onValueChange = { local = local.copy(systemPrompt = it) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text("System prompt") },
                )
            }
            item {
                SettingSlider("Temperature", local.temperature, 0f, 2f) {
                    local = local.copy(temperature = it)
                }
            }
            item {
                SettingSlider("Top P", local.topP, 0.1f, 1f) {
                    local = local.copy(topP = it)
                }
            }
            item {
                SettingSlider(
                    label = "Top K",
                    value = local.topK.toFloat(),
                    min = 1f,
                    max = 256f,
                    valueFormatter = { it.roundToInt().toString() },
                ) {
                    local = local.copy(topK = it.roundToInt())
                }
            }
            item {
                SettingSlider(
                    label = "Max tokens",
                    value = local.maxTokens.toFloat(),
                    min = 128f,
                    max = 4096f,
                    valueFormatter = { it.roundToInt().toString() },
                ) {
                    local = local.copy(maxTokens = it.roundToInt())
                }
            }
            item {
                SettingSlider("Repeat penalty", local.repeatPenalty, 0.8f, 1.5f) {
                    local = local.copy(repeatPenalty = it)
                }
            }
            item {
                SettingSlider(
                    label = "Repeat last N",
                    value = local.repeatLastN.toFloat(),
                    min = 0f,
                    max = 2048f,
                    valueFormatter = { it.roundToInt().toString() },
                ) {
                    local = local.copy(repeatLastN = it.roundToInt())
                }
            }
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                    Spacer(Modifier.width(Spacing.sm))
                    Button(onClick = { onApply(local) }) { Text("Применить") }
                }
            }
            item {
                Spacer(Modifier.height(Spacing.md))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatModelPickerSheet(
    selectedModelId: String?,
    modelOptions: List<ChatModelOption>,
    onSelectModel: (ChatModelOption) -> Unit,
    selectedAccelerator: ChatRuntimeAccelerator,
    onSelectAccelerator: (ChatRuntimeAccelerator) -> Unit,
    thinkingEnabled: Boolean,
    onSetThinkingEnabled: (Boolean) -> Unit,
    onOpenModelManager: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ChatModelPickerContent(
            selectedModelId = selectedModelId,
            modelOptions = modelOptions,
            onSelectModel = onSelectModel,
            selectedAccelerator = selectedAccelerator,
            onSelectAccelerator = onSelectAccelerator,
            thinkingEnabled = thinkingEnabled,
            onSetThinkingEnabled = onSetThinkingEnabled,
            onOpenModelManager = onOpenModelManager,
        )
    }
}

@Composable
private fun ChatModelPickerContent(
    selectedModelId: String?,
    modelOptions: List<ChatModelOption>,
    onSelectModel: (ChatModelOption) -> Unit,
    selectedAccelerator: ChatRuntimeAccelerator,
    onSelectAccelerator: (ChatRuntimeAccelerator) -> Unit,
    thinkingEnabled: Boolean,
    onSetThinkingEnabled: (Boolean) -> Unit,
    onOpenModelManager: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                Icons.Filled.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "Модели чата",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenModelManager) {
                Text("Управлять")
            }
        }

        if (modelOptions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text("Нет скачанных моделей", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Скачайте или импортируйте модель в общем менеджере моделей.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.md))
            return
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            modelOptions.forEach { option ->
                ChatModelPickerRow(
                    option = option,
                    selected = selectedModelId == option.id,
                    onSelectModel = onSelectModel,
                )
            }
        }
        modelOptions.firstOrNull { it.id == selectedModelId }?.let { selectedOption ->
            ChatAcceleratorSelector(
                option = selectedOption,
                selectedAccelerator = effectiveAccelerator(selectedOption, selectedAccelerator),
                onSelectAccelerator = onSelectAccelerator,
            )
            ChatThinkingToggle(
                option = selectedOption,
                thinkingEnabled = thinkingEnabled,
                onSetThinkingEnabled = onSetThinkingEnabled,
            )
        }
        Spacer(Modifier.height(Spacing.md))
    }
}

@Composable
private fun ChatThinkingToggle(
    option: ChatModelOption,
    thinkingEnabled: Boolean,
    onSetThinkingEnabled: (Boolean) -> Unit,
) {
    if (!option.runtime.thinking.isSupported) return

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Thinking",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Отдельный блок рассуждений будет показан только если runtime вернет thinking отдельно.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = thinkingEnabled,
            onCheckedChange = onSetThinkingEnabled,
        )
    }
}

@Composable
private fun ChatAcceleratorSelector(
    option: ChatModelOption,
    selectedAccelerator: ChatRuntimeAccelerator,
    onSelectAccelerator: (ChatRuntimeAccelerator) -> Unit,
) {
    if (
        !option.runtime.compatibility.isSelectableForChat ||
        !option.runtime.capabilities.supportsRuntimeSelection
    ) {
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = "Ускорение",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            option.runtime.supportedAccelerators.forEach { accelerator ->
                FilterChip(
                    selected = accelerator == selectedAccelerator,
                    onClick = { onSelectAccelerator(accelerator) },
                    label = { Text(accelerator.label) },
                )
            }
        }
        Text(
            text = acceleratorHelpText(option),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChatModelPickerRow(
    option: ChatModelOption,
    selected: Boolean,
    onSelectModel: (ChatModelOption) -> Unit,
) {
    val isSelectable = option.runtime.compatibility.isSelectableForChat
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .alpha(if (isSelectable) 1f else 0.62f)
            .clickable(enabled = isSelectable && !option.isActivating) { onSelectModel(option) }
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Icon(
            Icons.Filled.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = option.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = option.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = option.runtime.capabilitySummary,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!isSelectable) {
                Text(
                    text = option.runtime.compatibility.reason ?: "Модель недоступна для общего чата.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when {
            option.isActivating -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            !isSelectable -> Text(
                "Недоступна",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
            selected && option.isActive -> Text(
                "Загружена",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            selected -> Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Выбрана",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            option.isActive -> Text(
                "Загружена",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun effectiveAccelerator(
    option: ChatModelOption,
    selectedAccelerator: ChatRuntimeAccelerator,
): ChatRuntimeAccelerator =
    when {
        selectedAccelerator in option.runtime.supportedAccelerators -> selectedAccelerator
        option.runtime.selectedAccelerator in option.runtime.supportedAccelerators -> option.runtime.selectedAccelerator
        else -> option.runtime.supportedAccelerators.firstOrNull() ?: selectedAccelerator
    }

private fun acceleratorHelpText(option: ChatModelOption): String =
    when (option.runtime.backend) {
        ChatRuntimeBackend.LITERT_LM -> "Auto пробует NPU/GPU и откатывается на CPU; CPU фиксирует стабильный режим."
        ChatRuntimeBackend.LLAMA_CPP -> "Vulkan использует ускоренный llama.cpp backend, если устройство его реально поддерживает; CPU стабильнее."
        ChatRuntimeBackend.IMPORTED,
        ChatRuntimeBackend.UNKNOWN -> "Для этой модели доступен только объявленный runtime."
    }

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    valueFormatter: (Float) -> String = { "%.2f".format(it) },
    onChange: (Float) -> Unit,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(valueFormatter(value), style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}

private fun formatDuration(durationMs: Long): String =
    if (durationMs < 1000) {
        "${durationMs} ms"
    } else {
        "%.1f s".format(durationMs / 1000.0)
    }

private fun formatRate(tokensPerSecond: Double): String =
    if (tokensPerSecond < 10) {
        "%.1f".format(tokensPerSecond)
    } else {
        "%.0f".format(tokensPerSecond)
    }

package com.chromalab.feature.chat

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatState,
    actions: ChatActions,
    modelOptions: List<ChatModelOption>,
    onSelectModel: (ChatModelOption) -> Unit,
    onOpenModelManager: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSettings by remember { mutableStateOf(false) }
    val selected = state.selectedSession

    Scaffold(
        modifier = modifier,
        topBar = {
            ChatTopBar(
                selected = selected,
                activeModelName = selected?.modelName ?: state.activeModelName,
                onBack = { actions.selectChat(null) },
                onNewChat = actions.createChat,
                onOpenModelManager = onOpenModelManager,
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
            selectedModelId = selected.modelId,
            modelOptions = modelOptions,
            onSelectModel = { option ->
                actions.setChatModel(selected.id, option.id, option.name)
                onSelectModel(option)
            },
            onOpenModelManager = onOpenModelManager,
            onDismiss = { showSettings = false },
            onApply = { settings ->
                actions.updateSettings(selected.id, settings)
                showSettings = false
            },
        )
    }
}

@Composable
private fun ChatTopBar(
    selected: ChatSession?,
    activeModelName: String?,
    onBack: () -> Unit,
    onNewChat: () -> Unit,
    onOpenModelManager: () -> Unit,
    onSettings: () -> Unit,
) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (selected != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = selected?.title ?: "Чаты",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                if (selected == null) {
                    Text(
                        text = activeModelName ?: "Активная модель не выбрана",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            if (selected != null) {
                AssistChip(
                    onClick = onSettings,
                    label = {
                        Text(
                            text = activeModelName ?: "Выбрать модель",
                            maxLines = 1,
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Filled.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    modifier = Modifier.widthIn(max = 180.dp),
                )
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Tune, contentDescription = "Настройки")
                }
            }
            IconButton(onClick = onOpenModelManager) {
                Icon(Icons.Filled.SmartToy, contentDescription = "Модели")
            }
            IconButton(onClick = onNewChat) {
                Icon(Icons.Filled.Add, contentDescription = "Новый чат")
            }
        }
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
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .background(
                    color = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                if (isUser) "Вы" else (message.modelName ?: "Ассистент"),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isUser) {
                Text(message.content, style = MaterialTheme.typography.bodyMedium)
            } else {
                StreamingMessageText(message)
            }
            if (!isUser && !message.isStreaming && message.stats != null) {
                MessageStatsRow(message.stats)
            }
        }
    }
}

@Composable
private fun StreamingMessageText(message: ChatMessage) {
    var visibleText by remember(message.id) { mutableStateOf("") }
    var cursorVisible by remember(message.id) { mutableStateOf(true) }

    LaunchedEffect(message.id, message.content, message.isStreaming) {
        if (!message.isStreaming) {
            visibleText = message.content
            return@LaunchedEffect
        }
        if (!message.content.startsWith(visibleText)) {
            visibleText = ""
        }
        while (visibleText.length < message.content.length) {
            val nextLength = (visibleText.length + 3).coerceAtMost(message.content.length)
            visibleText = message.content.take(nextLength)
            delay(18)
        }
    }

    LaunchedEffect(message.id, message.isStreaming) {
        cursorVisible = true
        while (message.isStreaming) {
            delay(450)
            cursorVisible = !cursorVisible
        }
        cursorVisible = false
    }

    val text = when {
        visibleText.isNotBlank() -> visibleText
        message.isStreaming -> "Генерация..."
        else -> message.content
    }
    Text(
        text = if (message.isStreaming && cursorVisible) "$text|" else text,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun MessageStatsRow(stats: ChatMessageStats) {
    Text(
        text = "Prompt ${stats.promptTokens} tok · answer ${stats.completionTokens} tok · ${formatDuration(stats.durationMs)} · ${formatRate(stats.tokensPerSecond)} tok/s",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
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
    selectedModelId: String?,
    modelOptions: List<ChatModelOption>,
    onSelectModel: (ChatModelOption) -> Unit,
    onOpenModelManager: () -> Unit,
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
                ChatModelSelector(
                    selectedModelId = selectedModelId,
                    modelOptions = modelOptions,
                    onSelectModel = onSelectModel,
                    onOpenModelManager = onOpenModelManager,
                )
            }
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

@Composable
private fun ChatModelSelector(
    selectedModelId: String?,
    modelOptions: List<ChatModelOption>,
    onSelectModel: (ChatModelOption) -> Unit,
    onOpenModelManager: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Model", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = onOpenModelManager) {
                Text("Manage")
            }
        }

        if (modelOptions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
            return
        }

        modelOptions.forEach { option ->
            val selected = selectedModelId == option.id
            Card(
                modifier = Modifier.fillMaxWidth().clickable(
                    enabled = !option.isActivating,
                    onClick = { onSelectModel(option) },
                ),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    },
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Icon(Icons.Filled.SmartToy, contentDescription = null, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(option.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(
                            option.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    when {
                        option.isActivating -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        option.isActive -> Text("Loaded", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        selected -> Text("Selected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
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

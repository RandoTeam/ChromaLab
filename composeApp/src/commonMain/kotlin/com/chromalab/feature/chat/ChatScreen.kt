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
                activeModelName = state.activeModelName,
                onBack = { actions.selectChat(null) },
                onNewChat = actions.createChat,
                onOpenModelManager = onOpenModelManager,
                onSettings = { showSettings = true },
            )
        },
        bottomBar = {
            if (selected != null) {
                val selectedModelReady = selected.modelId == null || selected.modelId == state.activeModelId
                ChatComposer(
                    enabled = !state.isGenerating && state.activeModelName != null && selectedModelReady,
                    isGenerating = state.isGenerating,
                    modelReady = selectedModelReady,
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
                    Text("ОК")
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
                Text(
                    text = activeModelName ?: "Активная модель не выбрана",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            if (selected != null) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Tune, contentDescription = "Настройки")
                }
            }
            IconButton(onClick = onOpenModelManager) {
                Icon(Icons.Filled.SmartToy, contentDescription = "Models")
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
                    "Создайте чат и выберите активную модель в менеджере моделей.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = actions.createChat) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Новый чат")
                }
                TextButton(onClick = onOpenModelManager) {
                    Text("Model manager")
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
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.isGenerating) {
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
        if (state.activeModelName == null) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        "Активируйте LiteRT модель в менеджере моделей перед отправкой сообщения.",
                        modifier = Modifier.padding(Spacing.md),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
        items(state.messages, key = { it.id }) { message ->
            MessageBubble(message)
        }
        if (state.isGenerating) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Модель отвечает…", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(message.content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ChatComposer(
    enabled: Boolean,
    isGenerating: Boolean,
    modelReady: Boolean,
    onSend: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
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
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            ChatModelSelector(
                selectedModelId = selectedModelId,
                modelOptions = modelOptions,
                onSelectModel = onSelectModel,
                onOpenModelManager = onOpenModelManager,
            )
            Text("Настройки чата", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = local.systemPrompt,
                onValueChange = { local = local.copy(systemPrompt = it) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text("System prompt") },
            )
            SettingSlider("Temperature", local.temperature, 0f, 2f) {
                local = local.copy(temperature = it)
            }
            SettingSlider("Top P", local.topP, 0.1f, 1f) {
                local = local.copy(topP = it)
            }
            SettingSlider("Max tokens", local.maxTokens.toFloat(), 128f, 4096f) {
                local = local.copy(maxTokens = it.toInt())
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Отмена") }
                Spacer(Modifier.width(Spacing.sm))
                Button(onClick = { onApply(local) }) { Text("Применить") }
            }
            Spacer(Modifier.height(Spacing.md))
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
                    Text("No downloaded models", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Download or import a model in the shared Model Manager.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return
        }

        modelOptions.forEach { option ->
            val selected = selectedModelId == option.id || (selectedModelId == null && option.isActive)
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
                        option.isActive -> Text("Active", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
    onChange: (Float) -> Unit,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text("%.2f".format(value), style = MaterialTheme.typography.labelMedium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}

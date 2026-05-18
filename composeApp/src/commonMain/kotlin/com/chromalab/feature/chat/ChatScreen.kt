package com.chromalab.feature.chat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val CHAT_STREAM_FADE_MS = 120
private const val CHAT_STREAM_FADE_MAX_CHARS = 2400
private const val CHAT_STREAM_SCROLL_INTERVAL_MS = 250
private const val CHAT_KEYBOARD_BRING_INTO_VIEW_DELAY_MS = 180
private val CHAT_TOUCH_TARGET_SIZE = 48.dp
private val CHAT_COMPOSER_SHAPE = RoundedCornerShape(16.dp)

private data class ChatColorTokens(
    val background: Color,
    val panel: Color,
    val panelHigh: Color,
    val panelHighest: Color,
    val outline: Color,
    val text: Color,
    val mutedText: Color,
    val accent: Color,
    val onAccent: Color,
    val userBubble: Color,
    val onUserBubble: Color,
    val thinkingSurface: Color,
    val statsSurface: Color,
    val disabledSurface: Color,
    val disabledContent: Color,
    val destructiveSurface: Color,
    val onDestructiveSurface: Color,
    val error: Color,
)

@Composable
private fun chatColorTokens(): ChatColorTokens {
    val scheme = MaterialTheme.colorScheme
    return ChatColorTokens(
        background = scheme.surface,
        panel = scheme.surfaceContainerLow,
        panelHigh = scheme.surfaceContainerHigh,
        panelHighest = scheme.surfaceContainerHighest,
        outline = scheme.outlineVariant,
        text = scheme.onSurface,
        mutedText = scheme.onSurfaceVariant,
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        userBubble = scheme.primaryContainer,
        onUserBubble = scheme.onPrimaryContainer,
        thinkingSurface = scheme.surfaceContainerHigh.copy(alpha = 0.56f),
        statsSurface = scheme.surfaceContainerHigh.copy(alpha = 0.55f),
        disabledSurface = scheme.surfaceContainerHighest,
        disabledContent = scheme.onSurfaceVariant.copy(alpha = 0.45f),
        destructiveSurface = scheme.errorContainer,
        onDestructiveSurface = scheme.onErrorContainer,
        error = scheme.error,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatState,
    actions: ChatActions,
    modelOptions: List<ChatModelOption>,
    onOpenModelManager: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chatColors = chatColorTokens()
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
                    enabled = selected.modelId != null,
                    isGenerating = state.isGenerating,
                    onSend = actions.sendMessage,
                    onStop = actions.stopGeneration,
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
                    isSelectedModelLoading = selectedModelOption?.isActivating == true,
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
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = chatColors.destructiveSurface,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.SmartToy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = chatColors.onDestructiveSurface,
                            )
                        }
                    }
                    Text(
                        "Ошибка чата",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    state.error,
                    color = chatColors.mutedText,
                    style = MaterialTheme.typography.bodyMedium,
                )
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
    val chatColors = chatColorTokens()
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
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = selected?.title ?: "Чаты",
                    style = MaterialTheme.typography.titleSmall,
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
                        color = chatColors.mutedText,
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
    val chatColors = chatColorTokens()
    val isLoading = option?.isActivating == true
    val isLoaded = option?.isActive == true && !isLoading
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(chatColors.panelHigh)
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
                tint = chatColors.mutedText,
            )
            if (option?.isActivating == true) {
                CircularProgressIndicator(modifier = Modifier.size(21.dp), strokeWidth = 2.dp)
            } else if (isLoaded) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Модель загружена в память",
                    modifier = Modifier.size(14.dp).align(Alignment.BottomEnd),
                    tint = chatColors.accent,
                )
            }
        }
        Text(
            text = if (isLoading) "Загрузка..." else modelName ?: "Выбрать модель",
            style = MaterialTheme.typography.labelLarge,
            color = chatColors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp),
        )
        if (isLoaded) {
            Text(
                text = "В памяти",
                style = MaterialTheme.typography.labelSmall,
                color = chatColors.accent,
                maxLines = 1,
            )
        }
        Icon(
            Icons.Filled.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = chatColors.mutedText,
        )
    }
}

@Composable
private fun ChatListContent(
    state: ChatState,
    actions: ChatActions,
    onOpenModelManager: () -> Unit,
) {
    val chatColors = chatColorTokens()
    if (state.sessions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.padding(horizontal = 28.dp, vertical = Spacing.lg),
            ) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = chatColors.panelHighest,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.SmartToy,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = chatColors.accent,
                        )
                    }
                }
                Text(
                    "Нет чатов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Создайте чат и выберите модель из общего менеджера моделей.",
                    color = chatColors.mutedText,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = actions.createChat,
                    modifier = Modifier.defaultMinSize(minHeight = CHAT_TOUCH_TARGET_SIZE),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Новый чат")
                }
                TextButton(
                    onClick = onOpenModelManager,
                    modifier = Modifier.defaultMinSize(minHeight = CHAT_TOUCH_TARGET_SIZE),
                ) {
                    Text("Менеджер моделей")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Spacing.md),
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
    val chatColors = chatColorTokens()
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = chatColors.panelHigh.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 72.dp)
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(session.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    session.modelName ?: "Модель не закреплена",
                    style = MaterialTheme.typography.bodySmall,
                    color = chatColors.mutedText,
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
    isSelectedModelLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    val chatColors = chatColorTokens()
    val listState = rememberLazyListState()
    val hasModelWarning = selected?.modelId == null
    val showGeneratingPlaceholder = state.isGenerating && state.messages.none { it.isStreaming }
    val bottomAnchorIndex = state.messages.size +
        (if (hasModelWarning) 1 else 0) +
        (if (showGeneratingPlaceholder) 1 else 0)
    val latestBottomAnchorIndex by rememberUpdatedState(bottomAnchorIndex)

    LaunchedEffect(bottomAnchorIndex) {
        listState.animateScrollToItem(bottomAnchorIndex)
    }
    LaunchedEffect(state.isGenerating) {
        if (!state.isGenerating) return@LaunchedEffect

        while (true) {
            listState.scrollToItem(latestBottomAnchorIndex)
            delay(CHAT_STREAM_SCROLL_INTERVAL_MS.toLong())
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(chatColors.background),
        contentPadding = PaddingValues(vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        if (selected?.modelId == null) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = chatColors.destructiveSurface,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        "Выберите LiteRT или GGUF модель в настройках чата перед отправкой сообщения.",
                        modifier = Modifier.padding(Spacing.md),
                        color = chatColors.onDestructiveSurface,
                    )
                }
            }
        }
        items(state.messages, key = { it.id }) { message ->
            MessageBubble(message)
        }
        if (showGeneratingPlaceholder) {
            item {
                ChatActivitySurface(
                    label = if (isSelectedModelLoading) "Загружаю модель в память" else "Модель готовит ответ",
                )
            }
        }
        item(key = "chat_bottom_anchor") {
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}

@Composable
private fun ChatActivitySurface(label: String) {
    val chatColors = chatColorTokens()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = chatColors.panelHigh,
        shape = RoundedCornerShape(16.dp),
    ) {
        ChatActivityIndicator(
            label = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun ChatActivityIndicator(
    label: String,
    modifier: Modifier = Modifier,
) {
    val chatColors = chatColorTokens()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        AnimatedDots(color = chatColors.accent)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = chatColors.mutedText,
        )
    }
}

@Composable
private fun AnimatedDots(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "chat_dots")
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.32f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 640, delayMillis = index * 120, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "chat_dot_$index",
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(color, CircleShape),
            )
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val chatColors = chatColorTokens()
    val isUser = message.role == ChatRole.USER
    val contentModifier = if (isUser) {
        Modifier
            .fillMaxWidth(0.82f)
            .background(
                color = chatColors.userBubble,
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
                color = chatColors.mutedText,
            )
            if (isUser) {
                ChatMarkdownText(
                    text = message.content,
                    color = chatColors.onUserBubble,
                )
            } else {
                if (message.thinkingContent.isNotBlank()) {
                    ChatThinkingBlock(
                        text = message.thinkingContent,
                        isStreaming = message.isStreaming,
                    )
                }
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
private fun ChatThinkingBlock(
    text: String,
    isStreaming: Boolean,
) {
    val chatColors = chatColorTokens()
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = chatColors.thinkingSurface,
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Filled.SmartToy,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = chatColors.mutedText,
                )
                if (isStreaming) {
                    AnimatedDots(
                        color = chatColors.accent,
                        modifier = Modifier.width(24.dp),
                    )
                }
                Text(
                    text = "Thinking",
                    style = MaterialTheme.typography.labelMedium,
                    color = chatColors.mutedText,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse thinking" else "Expand thinking",
                    modifier = Modifier.size(18.dp).rotate(if (expanded) 180f else 0f),
                    tint = chatColors.mutedText,
                )
            }
            AnimatedVisibility(visible = expanded) {
                ChatMarkdownText(
                    text = text,
                    modifier = Modifier.fillMaxWidth(),
                    color = chatColors.mutedText,
                    small = true,
                )
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
        message.isStreaming -> ""
        else -> message.content
    }

    if (text.isBlank() && message.isStreaming) {
        ChatActivityIndicator(label = "Формирую ответ")
        return
    }

    val shouldFade = message.isStreaming && text.length <= CHAT_STREAM_FADE_MAX_CHARS

    if (shouldFade) {
        Crossfade(
            targetState = text,
            animationSpec = tween(durationMillis = CHAT_STREAM_FADE_MS),
            label = "chat_stream_text",
        ) { target ->
            ChatMarkdownText(target)
        }
    } else {
        ChatMarkdownText(text)
    }
}

@Composable
private fun ChatMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    small: Boolean = false,
) {
    val chatColors = chatColorTokens()
    val resolvedColor = color ?: chatColors.text
    val style = if (small) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
    SelectionContainer {
        ProvideTextStyle(value = style.copy(color = resolvedColor)) {
            RichText(modifier = modifier.fillMaxWidth()) {
                Markdown(content = text)
            }
        }
    }
}

@Composable
private fun MessageStatsRow(stats: ChatMessageStats) {
    val chatColors = chatColorTokens()
    Surface(
        color = chatColors.statsSurface,
        shape = RoundedCornerShape(24.dp),
    ) {
        Text(
            text = formatStatsText(stats),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = chatColors.mutedText,
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

@Composable
private fun ChatComposer(
    enabled: Boolean,
    isGenerating: Boolean,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
) {
    val chatColors = chatColorTokens()
    var text by remember { mutableStateOf("") }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    val canSend = enabled && text.isNotBlank() && !isGenerating
    val placeholderText = when {
        isGenerating -> "Модель отвечает..."
        enabled -> "Сообщение"
        else -> "Выберите активную модель"
    }

    Surface(
        modifier = Modifier.fillMaxWidth().imePadding(),
        color = chatColors.background,
        tonalElevation = 0.dp,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .heightIn(min = 58.dp),
            shape = CHAT_COMPOSER_SHAPE,
            color = chatColors.panel,
            border = BorderStroke(1.dp, chatColors.outline),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .weight(1f)
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                coroutineScope.launch {
                                    delay(CHAT_KEYBOARD_BRING_INTO_VIEW_DELAY_MS.toLong())
                                    bringIntoViewRequester.bringIntoView()
                                }
                            }
                        }
                        .padding(vertical = 6.dp),
                    enabled = enabled && !isGenerating,
                    minLines = 1,
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = if (enabled && !isGenerating) {
                            chatColors.text
                        } else {
                            chatColors.mutedText
                        },
                    ),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(
                                    text = placeholderText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = chatColors.mutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                if (isGenerating) {
                    FilledIconButton(
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = chatColors.destructiveSurface,
                            contentColor = chatColors.onDestructiveSurface,
                        ),
                        onClick = onStop,
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Остановить")
                    }
                } else {
                    FilledIconButton(
                        enabled = canSend,
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = chatColors.accent,
                            contentColor = chatColors.onAccent,
                            disabledContainerColor = chatColors.disabledSurface,
                            disabledContentColor = chatColors.disabledContent,
                        ),
                        onClick = {
                            val message = text
                            text = ""
                            onSend(message)
                        },
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Отправить")
                    }
                }
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
            contentPadding = PaddingValues(Spacing.lg),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MTP", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            "GGUF text-only draft acceleration",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = local.enableMtp,
                        onCheckedChange = { local = local.copy(enableMtp = it) },
                    )
                }
            }
            if (local.enableMtp) {
                item {
                    SettingSlider(
                        label = "MTP draft tokens",
                        value = local.mtpDraftTokens.toFloat(),
                        min = 1f,
                        max = 16f,
                        valueFormatter = { it.roundToInt().toString() },
                    ) {
                        local = local.copy(mtpDraftTokens = it.roundToInt())
                    }
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
    val chatColors = chatColorTokens()
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = CHAT_TOUCH_TARGET_SIZE)
                .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Icon(
                Icons.Filled.SmartToy,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = chatColors.accent,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = chatColors.panelHigh.copy(alpha = 0.45f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 80.dp)
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text("Нет скачанных моделей", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Скачайте или импортируйте модель в общем менеджере моделей.",
                        style = MaterialTheme.typography.bodySmall,
                        color = chatColors.mutedText,
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
    val chatColors = chatColorTokens()
    if (!option.runtime.thinking.isSupported) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Thinking",
                style = MaterialTheme.typography.labelLarge,
                color = chatColors.text,
            )
            Text(
                text = "Отдельный блок рассуждений будет показан только если runtime вернет thinking отдельно.",
                style = MaterialTheme.typography.bodySmall,
                color = chatColors.mutedText,
            )
        }
        Switch(
            checked = thinkingEnabled,
            onCheckedChange = onSetThinkingEnabled,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatAcceleratorSelector(
    option: ChatModelOption,
    selectedAccelerator: ChatRuntimeAccelerator,
    onSelectAccelerator: (ChatRuntimeAccelerator) -> Unit,
) {
    val chatColors = chatColorTokens()
    if (
        !option.runtime.compatibility.isSelectableForChat ||
        !option.runtime.capabilities.supportsRuntimeSelection
    ) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = "Ускорение",
            style = MaterialTheme.typography.labelLarge,
            color = chatColors.text,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
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
            color = chatColors.mutedText,
        )
    }
}

@Composable
private fun ChatModelPickerRow(
    option: ChatModelOption,
    selected: Boolean,
    onSelectModel: (ChatModelOption) -> Unit,
) {
    val chatColors = chatColorTokens()
    val isSelectable = option.runtime.compatibility.isSelectableForChat
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 72.dp)
            .background(
                color = if (selected) chatColors.panel else Color.Transparent,
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
            tint = if (selected) chatColors.accent else chatColors.mutedText,
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
                color = chatColors.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = option.runtime.capabilitySummary,
                style = MaterialTheme.typography.labelSmall,
                color = chatColors.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!isSelectable) {
                Text(
                    text = option.runtime.compatibility.reason ?: "Модель недоступна для общего чата.",
                    style = MaterialTheme.typography.labelSmall,
                    color = chatColors.error,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        when {
            option.isActivating -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    "Загрузка в память...",
                    style = MaterialTheme.typography.labelSmall,
                    color = chatColors.accent,
                )
            }
            !isSelectable -> Text(
                "Недоступна",
                style = MaterialTheme.typography.labelSmall,
                color = chatColors.error,
            )
            selected && option.isActive -> Text(
                "В памяти",
                style = MaterialTheme.typography.labelSmall,
                color = chatColors.accent,
            )
            selected -> Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "Выбрана",
                modifier = Modifier.size(18.dp),
                tint = chatColors.accent,
            )
            option.isActive -> Text(
                "В памяти",
                style = MaterialTheme.typography.labelSmall,
                color = chatColors.accent,
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
        ChatRuntimeBackend.LITERT_LM -> "Auto пробует GPU и откатывается на CPU; CPU фиксирует стабильный режим."
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

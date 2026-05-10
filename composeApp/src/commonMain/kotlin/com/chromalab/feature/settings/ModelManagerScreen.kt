package com.chromalab.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chromalab.feature.processing.inference.ModelRuntime
import com.chromalab.feature.processing.model.ModelFileType
import com.chromalab.feature.processing.model.ModelInfo
import com.chromalab.feature.processing.model.ModelRegistry

/**
 * Model Manager Screen — browse, download, delete, and manage AI models.
 *
 * NOTE: This is a commonMain composable using ModelRegistry data.
 * Actual download/delete operations require the Android-specific ModelManager,
 * which will be wired via callbacks from the parent navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    downloadedModelIds: Set<String>,
    activeModelId: String?,
    downloadingModelId: String?,
    downloadProgress: Float,
    downloadSpeedMbps: Float,
    deviceRamMb: Int,
    availableStorageGb: Float,
    totalModelDiskUsageGb: Float,
    onDownload: (ModelInfo) -> Unit,
    onDelete: (String) -> Unit,
    onActivate: (String) -> Unit,
    onDeactivate: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    val models = remember { ModelRegistry.builtinModels }
    val grouped = remember { ModelRegistry.groupedByRuntime() }
    var deleteConfirmId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Модели ИИ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ===== Active Model Banner =====
            val activeModel = models.find { it.id == activeModelId }
            if (activeModel != null) {
                item {
                    ActiveModelBanner(
                        model = activeModel,
                        onDeactivate = onDeactivate,
                    )
                }
            }

            // ===== LiteRT-LM Section =====
            item {
                SectionHeader(
                    title = "Рекомендованные",
                    subtitle = "LiteRT-LM · NPU/GPU ускорение",
                    icon = Icons.Filled.Bolt,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            val liteRTModels = grouped[ModelRuntime.LITERT_LM] ?: emptyList()
            items(liteRTModels, key = { it.id }) { model ->
                ModelCard(
                    model = model,
                    isDownloaded = model.id in downloadedModelIds,
                    isActive = model.id == activeModelId,
                    isDownloading = model.id == downloadingModelId,
                    downloadProgress = if (model.id == downloadingModelId) downloadProgress else 0f,
                    downloadSpeedMbps = if (model.id == downloadingModelId) downloadSpeedMbps else 0f,
                    deviceRamMb = deviceRamMb,
                    onDownload = { onDownload(model) },
                    onDelete = { deleteConfirmId = model.id },
                    onActivate = { onActivate(model.id) },
                    onCancelDownload = onCancelDownload,
                )
            }

            // ===== GGUF Section =====
            item { Spacer(Modifier.height(4.dp)) }
            item {
                SectionHeader(
                    title = "Универсальные",
                    subtitle = "llama.cpp · GGUF · Vulkan/CPU",
                    icon = Icons.Filled.Memory,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            val ggufModels = grouped[ModelRuntime.LLAMA_CPP] ?: emptyList()
            items(ggufModels, key = { it.id }) { model ->
                ModelCard(
                    model = model,
                    isDownloaded = model.id in downloadedModelIds,
                    isActive = model.id == activeModelId,
                    isDownloading = model.id == downloadingModelId,
                    downloadProgress = if (model.id == downloadingModelId) downloadProgress else 0f,
                    downloadSpeedMbps = if (model.id == downloadingModelId) downloadSpeedMbps else 0f,
                    deviceRamMb = deviceRamMb,
                    onDownload = { onDownload(model) },
                    onDelete = { deleteConfirmId = model.id },
                    onActivate = { onActivate(model.id) },
                    onCancelDownload = onCancelDownload,
                )
            }

            // ===== Custom Import =====
            item { Spacer(Modifier.height(4.dp)) }
            item {
                SectionHeader(
                    title = "Пользовательские",
                    subtitle = "Загрузите GGUF или LiteRT модель",
                    icon = Icons.Filled.FolderOpen,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            item {
                OutlinedCard(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            ),
                        ),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Загрузить модель с телефона",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }

            // ===== Device Info =====
            item { Spacer(Modifier.height(4.dp)) }
            item {
                DeviceInfoCard(
                    deviceRamMb = deviceRamMb,
                    availableStorageGb = availableStorageGb,
                    totalModelDiskUsageGb = totalModelDiskUsageGb,
                )
            }
        }
    }

    // ===== Delete Confirmation Dialog =====
    deleteConfirmId?.let { modelId ->
        val model = models.find { it.id == modelId }
        AlertDialog(
            onDismissRequest = { deleteConfirmId = null },
            icon = { Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить модель?") },
            text = {
                Text(
                    "${model?.displayName ?: modelId}\n" +
                    "Размер: ${formatBytes(model?.totalSizeBytes ?: 0)}\n\n" +
                    "Модель будет удалена с устройства. Вы сможете скачать её повторно.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(modelId)
                        deleteConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmId = null }) {
                    Text("Отмена")
                }
            },
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(color.copy(alpha = 0.6f), Color.Transparent),
                    )
                )
        )
    }
}

@Composable
private fun ModelCard(
    model: ModelInfo,
    isDownloaded: Boolean,
    isActive: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadSpeedMbps: Float,
    deviceRamMb: Int,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onActivate: () -> Unit,
    onCancelDownload: () -> Unit = {},
) {
    val ramOk = deviceRamMb >= model.minRamMb
    val animatedProgress by animateFloatAsState(targetValue = downloadProgress, label = "dl")

    // Active glow border
    val borderColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else Color.Transparent,
        label = "border",
    )

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isActive) Modifier.border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        ),
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Runtime chip
                        val runtimeColor = when (model.runtime) {
                            ModelRuntime.LITERT_LM -> MaterialTheme.colorScheme.primary
                            ModelRuntime.LLAMA_CPP -> MaterialTheme.colorScheme.secondary
                        }
                        val runtimeLabel = when (model.runtime) {
                            ModelRuntime.LITERT_LM -> "LiteRT"
                            ModelRuntime.LLAMA_CPP -> "GGUF"
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = runtimeColor.copy(alpha = 0.15f),
                        ) {
                            Text(
                                runtimeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = runtimeColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            model.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (isActive) {
                            Spacer(Modifier.width(8.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("✓", modifier = Modifier.padding(horizontal = 2.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Size badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Text(
                        formatBytes(model.totalSizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            // File info + vision badge
            if (model.files.size > 1 || model.supportsVision) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (model.supportsVision) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.Visibility,
                                    null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.tertiary,
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Vision",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    if (model.files.size > 1) {
                        Text(
                            model.files.joinToString(" + ") {
                                when (it.type) {
                                    ModelFileType.GGUF_BASE -> "base ${formatBytes(it.sizeBytes)}"
                                    ModelFileType.GGUF_MMPROJ -> "vision ${formatBytes(it.sizeBytes)}"
                                    ModelFileType.LITERT_BUNDLE -> formatBytes(it.sizeBytes)
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // RAM warning
            if (!ramOk) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Warning,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Требуется ${model.minRamMb / 1024} GB RAM (у вас ${deviceRamMb / 1024} GB)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Download progress
            AnimatedVisibility(
                visible = isDownloading,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Circular progress
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(40.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                            Text(
                                "${(animatedProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "%.1f MB/s".format(downloadSpeedMbps),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    isDownloading -> {
                        OutlinedButton(
                            onClick = onCancelDownload,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(Icons.Filled.Close, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Отмена")
                        }
                    }
                    isDownloaded && isActive -> {
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Удалить")
                        }
                    }
                    isDownloaded && !isActive -> {
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Удалить")
                        }
                        Spacer(Modifier.width(8.dp))
                        FilledTonalButton(onClick = onActivate) {
                            Icon(Icons.Filled.PlayArrow, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Активировать")
                        }
                    }
                    else -> {
                        FilledTonalButton(
                            onClick = onDownload,
                            enabled = ramOk,
                        ) {
                            Icon(Icons.Filled.Download, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Скачать")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveModelBanner(
    model: ModelInfo,
    onDeactivate: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = glowAlpha),
                            MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.3f),
                        ),
                    ),
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Psychology,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Активная модель",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        model.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    val runtime = when (model.runtime) {
                        ModelRuntime.LITERT_LM -> "LiteRT GPU"
                        ModelRuntime.LLAMA_CPP -> "llama.cpp"
                    }
                    Text(
                        "$runtime · ${formatBytes(model.totalSizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                // Deactivate button
                IconButton(onClick = onDeactivate) {
                    Icon(
                        Icons.Filled.PowerSettingsNew,
                        contentDescription = "Выгрузить",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(
    deviceRamMb: Int,
    availableStorageGb: Float,
    totalModelDiskUsageGb: Float,
) {
    val totalStorageGb = availableStorageGb + totalModelDiskUsageGb
    val storageFraction = if (totalStorageGb > 0) totalModelDiskUsageGb / totalStorageGb else 0f
    val animatedStorage by animateFloatAsState(storageFraction.coerceIn(0f, 1f), label = "s")

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Storage,
                    null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Устройство",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(12.dp))

            // Storage bar
            Text(
                "Хранилище",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedStorage)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                ),
                            )
                        ),
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Модели: %.1f GB".format(totalModelDiskUsageGb),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "Свободно: %.1f GB".format(availableStorageGb),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(Modifier.height(8.dp))

            // RAM info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Memory,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "RAM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${deviceRamMb / 1024} GB",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000f)
    bytes >= 1_000_000 -> "%.0f MB".format(bytes / 1_000_000f)
    else -> "%.0f KB".format(bytes / 1_000f)
}

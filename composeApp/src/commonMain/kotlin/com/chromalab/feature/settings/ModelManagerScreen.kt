package com.chromalab.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onImport: () -> Unit,
    onBack: () -> Unit,
) {
    val models = remember { ModelRegistry.builtinModels }
    val grouped = remember { ModelRegistry.groupedByRuntime() }

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
                    onDelete = { onDelete(model.id) },
                    onActivate = { onActivate(model.id) },
                )
            }

            // ===== GGUF Section =====
            item { Spacer(Modifier.height(8.dp)) }
            item {
                SectionHeader(
                    title = "Универсальные",
                    subtitle = "llama.cpp · GGUF · Vulkan/CPU",
                    icon = Icons.Filled.Build,
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
                    onDelete = { onDelete(model.id) },
                    onActivate = { onActivate(model.id) },
                )
            }

            // ===== Custom Import =====
            item { Spacer(Modifier.height(8.dp)) }
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
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Загрузить модель с телефона",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // ===== Device Info =====
            item { Spacer(Modifier.height(8.dp)) }
            item {
                DeviceInfoCard(
                    deviceRamMb = deviceRamMb,
                    availableStorageGb = availableStorageGb,
                    totalModelDiskUsageGb = totalModelDiskUsageGb,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
) {
    val ramOk = deviceRamMb >= model.minRamMb
    val animatedProgress by animateFloatAsState(targetValue = downloadProgress, label = "dl")

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            model.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (isActive) {
                            Spacer(Modifier.width(8.dp))
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ) {
                                Text("Активная", modifier = Modifier.padding(horizontal = 4.dp))
                            }
                        }
                    }

                    Spacer(Modifier.height(2.dp))

                    Text(
                        model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

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

            // File info for GGUF (base + mmproj)
            if (model.files.size > 1) {
                Spacer(Modifier.height(4.dp))
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

            // RAM warning
            if (!ramOk) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "⚠ Требуется ${model.minRamMb / 1024} GB RAM (у вас ${deviceRamMb / 1024} GB)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(12.dp))

            // Download progress
            AnimatedVisibility(visible = isDownloading) {
                Column {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "${(animatedProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            "%.1f MB/s".format(downloadSpeedMbps),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                            onClick = { /* cancel handled by parent */ },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
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
private fun DeviceInfoCard(
    deviceRamMb: Int,
    availableStorageGb: Float,
    totalModelDiskUsageGb: Float,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Информация",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))

            InfoRow("Занято моделями", "%.1f GB".format(totalModelDiskUsageGb))
            InfoRow("Свободно на устройстве", "%.1f GB".format(availableStorageGb))
            InfoRow("RAM устройства", "${deviceRamMb / 1024} GB")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000f)
    bytes >= 1_000_000 -> "%.0f MB".format(bytes / 1_000_000f)
    else -> "%.0f KB".format(bytes / 1_000f)
}

package com.chromalab.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chromalab.feature.processing.inference.ModelRuntime
import com.chromalab.feature.processing.model.ModelFileType
import com.chromalab.feature.processing.model.ModelGroup
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
    chromatogramModelId: String?,
    downloadJobs: Map<String, ModelDownloadUiState>,
    deviceRamMb: Int,
    availableStorageGb: Float,
    totalModelDiskUsageGb: Float,
    customModels: List<CustomModelEntry> = emptyList(),
    isImporting: Boolean = false,
    huggingFaceSearch: HuggingFaceSearchState = HuggingFaceSearchState(),
    onDownload: (ModelInfo) -> Unit,
    onDelete: (String) -> Unit,
    onUseForChromatograms: (String) -> Unit,
    onCancelDownload: (String) -> Unit = {},
    onExport: (String) -> Unit = {},
    onImport: () -> Unit,
    onHuggingFaceQueryChange: (String) -> Unit = {},
    onHuggingFaceSortChange: (HuggingFaceSortOption) -> Unit = {},
    onHuggingFaceSearch: () -> Unit = {},
    onBack: () -> Unit,
) {
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
        Box(modifier = Modifier.fillMaxSize()) {
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

            val liteRTModels = ModelRegistry.standaloneModels(ModelRuntime.LITERT_LM)
            items(liteRTModels, key = { it.id }) { model ->
                val downloadJob = downloadJobs[model.id]
                ModelCard(
                    model = model,
                    isDownloaded = model.id in downloadedModelIds,
                    isChromatogramModel = model.id == chromatogramModelId,
                    canUseForChromatograms = ModelRegistry.isChromatogramVisionModel(model),
                    isDownloading = downloadJob.isRunning(),
                    downloadProgress = downloadJob?.progress ?: 0f,
                    downloadSpeedMbps = downloadJob?.speedMbps ?: 0f,
                    deviceRamMb = deviceRamMb,
                    onDownload = { onDownload(model) },
                    onDelete = { deleteConfirmId = model.id },
                    onUseForChromatograms = { onUseForChromatograms(model.id) },
                    onExport = { onExport(model.id) },
                    onCancelDownload = { onCancelDownload(model.id) },
                )
            }

            // ===== GGUF Section — expandable groups =====
            item { Spacer(Modifier.height(4.dp)) }
            item {
                HuggingFaceSearchCard(
                    state = huggingFaceSearch,
                    downloadedModelIds = downloadedModelIds,
                    downloadJobs = downloadJobs,
                    deviceRamMb = deviceRamMb,
                    onQueryChange = onHuggingFaceQueryChange,
                    onSortChange = onHuggingFaceSortChange,
                    onSearch = onHuggingFaceSearch,
                    onDownload = onDownload,
                    onCancelDownload = onCancelDownload,
                )
            }
            item { Spacer(Modifier.height(4.dp)) }
            item {
                SectionHeader(
                    title = "Универсальные",
                    subtitle = "llama.cpp · GGUF · Vulkan/CPU",
                    icon = Icons.Filled.Memory,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }

            // Expandable model groups (3B, 7B)
            val ggufGroups = ModelRegistry.groupsForRuntime(ModelRuntime.LLAMA_CPP)
            for (group in ggufGroups) {
                item(key = "group_${group.groupId}") {
                    ExpandableModelGroup(
                        group = group,
                        downloadedModelIds = downloadedModelIds,
                        chromatogramModelId = chromatogramModelId,
                        downloadJobs = downloadJobs,
                        deviceRamMb = deviceRamMb,
                        onDownload = onDownload,
                        onDelete = { deleteConfirmId = it },
                        onUseForChromatograms = onUseForChromatograms,
                        onExport = onExport,
                        onCancelDownload = onCancelDownload,
                    )
                }
            }

            // Standalone GGUF models (e.g. Qwen3.5-VL-9B)
            val standaloneGguf = ModelRegistry.standaloneModels(ModelRuntime.LLAMA_CPP)
            items(standaloneGguf, key = { it.id }) { model ->
                val downloadJob = downloadJobs[model.id]
                ModelCard(
                    model = model,
                    isDownloaded = model.id in downloadedModelIds,
                    isChromatogramModel = model.id == chromatogramModelId,
                    canUseForChromatograms = ModelRegistry.isChromatogramVisionModel(model),
                    isDownloading = downloadJob.isRunning(),
                    downloadProgress = downloadJob?.progress ?: 0f,
                    downloadSpeedMbps = downloadJob?.speedMbps ?: 0f,
                    deviceRamMb = deviceRamMb,
                    onDownload = { onDownload(model) },
                    onDelete = { deleteConfirmId = model.id },
                    onUseForChromatograms = { onUseForChromatograms(model.id) },
                    onExport = { onExport(model.id) },
                    onCancelDownload = { onCancelDownload(model.id) },
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

            // Import progress indicator
            if (isImporting) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                "Импорт модели…",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }

            // Imported custom models
            items(customModels, key = { it.id }) { custom ->
                CustomModelCard(
                    custom = custom,
                    isChromatogramModel = custom.id == chromatogramModelId,
                    onUseForChromatograms = { onUseForChromatograms(custom.id) },
                    onDelete = { deleteConfirmId = custom.id },
                    onExport = { onExport(custom.id) },
                )
            }

            // Import button
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

        } // Box
    }

    // ===== Delete Confirmation Dialog =====
    deleteConfirmId?.let { modelId ->
        val model = ModelRegistry.findById(modelId)
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

private fun ModelDownloadUiState?.isRunning(): Boolean =
    this?.phase == ModelDownloadUiPhase.QUEUED ||
        this?.phase == ModelDownloadUiPhase.CONNECTING ||
        this?.phase == ModelDownloadUiPhase.DOWNLOADING ||
        this?.phase == ModelDownloadUiPhase.VALIDATING

@Composable
private fun HuggingFaceSearchCard(
    state: HuggingFaceSearchState,
    downloadedModelIds: Set<String>,
    downloadJobs: Map<String, ModelDownloadUiState>,
    deviceRamMb: Int,
    onQueryChange: (String) -> Unit,
    onSortChange: (HuggingFaceSortOption) -> Unit,
    onSearch: () -> Unit,
    onDownload: (ModelInfo) -> Unit,
    onCancelDownload: (String) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Hugging Face", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Search by model, author, downloads, likes, or update time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Model or author:name") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                )
                FilledTonalButton(
                    onClick = onSearch,
                    enabled = !state.isSearching,
                ) {
                    if (state.isSearching) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HuggingFaceSortOption.entries.forEach { option ->
                    FilterChip(
                        selected = state.sort == option,
                        onClick = { onSortChange(option) },
                        label = { Text(option.label) },
                    )
                }
            }

            if (state.error != null) {
                Text(
                    state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            if (state.results.isEmpty() && !state.isSearching && state.error == null) {
                Text(
                    "Enter a query and search. Use author:unsloth to filter by author.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.results.forEachIndexed { index, result ->
                val downloadJob = downloadJobs[result.modelInfo.id]
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
                HuggingFaceResultRow(
                    result = result,
                    isDownloaded = result.modelInfo.id in downloadedModelIds,
                    isDownloading = downloadJob.isRunning(),
                    downloadProgress = downloadJob?.progress ?: 0f,
                    deviceRamMb = deviceRamMb,
                    onDownload = { onDownload(result.modelInfo) },
                    onCancelDownload = { onCancelDownload(result.modelInfo.id) },
                )
            }
        }
    }
}

@Composable
private fun HuggingFaceResultRow(
    result: HuggingFaceSearchResult,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    deviceRamMb: Int,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    result.repoId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${formatCount(result.downloads)} downloads · ${formatCount(result.likes)} likes · ${formatHfDate(result.lastModified)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${result.selectedFileName} · ${formatBytes(result.modelInfo.totalSizeBytes)} · RAM ${deviceRamMb / 1024} GB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Badge(
                containerColor = if (result.isCompatible) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                },
            ) {
                Text(result.compatibilityLabel, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }

        if (isDownloading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)),
                )
                TextButton(onClick = onCancelDownload) {
                    Text("Cancel")
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                when {
                    isDownloaded -> {
                        Text(
                            "Downloaded",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 10.dp),
                        )
                    }
                    else -> {
                        FilledTonalButton(
                            onClick = onDownload,
                            enabled = result.isCompatible,
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Download")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Expandable group card for a model family (e.g. Qwen2.5-VL 3B).
 * Shows a header with model name, tapping expands to show quantization variants.
 */
@Composable
private fun ExpandableModelGroup(
    group: ModelGroup,
    downloadedModelIds: Set<String>,
    chromatogramModelId: String?,
    downloadJobs: Map<String, ModelDownloadUiState>,
    deviceRamMb: Int,
    onDownload: (ModelInfo) -> Unit,
    onDelete: (String) -> Unit,
    onUseForChromatograms: (String) -> Unit,
    onExport: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    // Count how many variants are downloaded
    val downloadedCount = group.variants.count { it.id in downloadedModelIds }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column {
            // Group header — tap to expand
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Runtime chip
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                ) {
                    Text(
                        "GGUF",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        group.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${group.variants.size} квантизаций" +
                            if (downloadedCount > 0) " · $downloadedCount загружено" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (group.supportsVision) {
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

                // Expand/collapse icon with animation
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    label = "chevron",
                )
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    modifier = Modifier.rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Expanded variants
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    group.variants.forEach { variant ->
                        val downloadJob = downloadJobs[variant.id]
                        QuantVariantCard(
                            variant = variant,
                            isDownloaded = variant.id in downloadedModelIds,
                            isChromatogramModel = variant.id == chromatogramModelId,
                            canUseForChromatograms = ModelRegistry.isChromatogramVisionModel(variant),
                            isDownloading = downloadJob.isRunning(),
                            downloadProgress = downloadJob?.progress ?: 0f,
                            downloadSpeedMbps = downloadJob?.speedMbps ?: 0f,
                            deviceRamMb = deviceRamMb,
                            onDownload = { onDownload(variant) },
                            onDelete = { onDelete(variant.id) },
                            onUseForChromatograms = { onUseForChromatograms(variant.id) },
                            onExport = { onExport(variant.id) },
                            onCancelDownload = { onCancelDownload(variant.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact card for a quantization variant within an expanded group.
 */
@Composable
private fun QuantVariantCard(
    variant: ModelInfo,
    isDownloaded: Boolean,
    isChromatogramModel: Boolean,
    canUseForChromatograms: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadSpeedMbps: Float,
    deviceRamMb: Int,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onUseForChromatograms: () -> Unit,
    onExport: () -> Unit,
    onCancelDownload: () -> Unit,
) {
    val ramOk = deviceRamMb >= variant.minRamMb
    val animatedProgress by animateFloatAsState(targetValue = downloadProgress, label = "dl")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Quant label
                Text(
                    variant.quantLabel ?: variant.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.weight(1f))

                // Size + RAM info
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatBytes(variant.totalSizeBytes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!ramOk) {
                        Text(
                            "Нужно ${variant.minRamMb / 1024} GB RAM",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            // Description
            Text(
                variant.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )

            // Download progress (compact)
            if (isDownloading) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${(animatedProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (downloadSpeedMbps > 0f) {
                    Text(
                        "%.1f MB/s".format(downloadSpeedMbps),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isDownloaded && canUseForChromatograms && ramOk) {
                Spacer(Modifier.height(6.dp))
                ChromatogramRoleRow(
                    isSelected = isChromatogramModel,
                    onSelect = onUseForChromatograms,
                )
            }

            // Action buttons (compact row)
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                when {
                    isDownloading -> {
                        TextButton(onClick = onCancelDownload, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(Icons.Filled.Close, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Отмена", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    isDownloaded -> {
                        TextButton(onClick = onExport, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(Icons.Filled.Upload, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Выгрузить", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 8.dp)) {
                            Icon(Icons.Filled.Delete, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Удалить", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    else -> {
                        FilledTonalButton(
                            onClick = onDownload,
                            enabled = ramOk,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Icon(Icons.Filled.Download, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Скачать", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UseCaseChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ChromatogramRoleRow(
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        FilterChip(
            selected = isSelected,
            onClick = { if (!isSelected) onSelect() },
            leadingIcon = {
                Icon(
                    Icons.Filled.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            },
            label = {
                Text(if (isSelected) "Для хроматограмм" else "Назначить для хроматограмм")
            },
        )
    }
}

@Composable
private fun ModelCard(
    model: ModelInfo,
    isDownloaded: Boolean,
    isChromatogramModel: Boolean,
    canUseForChromatograms: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadSpeedMbps: Float,
    deviceRamMb: Int,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onUseForChromatograms: () -> Unit,
    onExport: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
) {
    val ramOk = deviceRamMb >= model.minRamMb
    val animatedProgress by animateFloatAsState(targetValue = downloadProgress, label = "dl")

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                    if (ModelRegistry.isChromatogramVisionModel(model)) {
                        UseCaseChip(label = "Chroma", color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (ModelRegistry.chatModels().any { it.id == model.id }) {
                        UseCaseChip(label = "Chat", color = MaterialTheme.colorScheme.secondary)
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
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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

            if (isDownloaded && canUseForChromatograms && ramOk) {
                ChromatogramRoleRow(
                    isSelected = isChromatogramModel,
                    onSelect = onUseForChromatograms,
                )
                Spacer(Modifier.height(8.dp))
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
                    isDownloaded -> {
                        TextButton(onClick = onExport) {
                            Icon(Icons.Filled.Upload, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Выгрузить")
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Filled.Delete, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Удалить")
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

/**
 * Card for a custom/imported model.
 */
@Composable
private fun CustomModelCard(
    custom: CustomModelEntry,
    isChromatogramModel: Boolean,
    onUseForChromatograms: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.FolderOpen,
                    null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        custom.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (custom.description.isNotEmpty()) {
                        Text(
                            custom.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text(
                formatBytes(custom.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (custom.supportsVision) {
                Spacer(Modifier.height(6.dp))
                ChromatogramRoleRow(
                    isSelected = isChromatogramModel,
                    onSelect = onUseForChromatograms,
                )
            }

            // Action buttons
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onExport, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Filled.Upload, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Выгрузить", style = MaterialTheme.typography.labelSmall)
                }
                TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Filled.Delete, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("Удалить", style = MaterialTheme.typography.labelSmall)
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

private fun formatCount(value: Long): String = when {
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000f)
    value >= 1_000 -> "%.1fK".format(value / 1_000f)
    else -> value.toString()
}

private fun formatHfDate(value: String): String =
    value.take(10).ifBlank { "unknown" }

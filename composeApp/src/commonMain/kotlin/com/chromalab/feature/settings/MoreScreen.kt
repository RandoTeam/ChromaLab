package com.chromalab.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * "More" tab screen — settings hub with sections.
 */
@Composable
fun MoreScreen(
    activeModelName: String?,
    activeModelSummary: String?,
    threadCount: Int,
    downloadParallelism: Int,
    downloadSpeedLimitMbps: Int,
    autoUnloadMinutes: Int,
    onOpenModelManager: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenAbout: () -> Unit,
    onThreadCountChange: (Int) -> Unit,
    onDownloadParallelismChange: (Int) -> Unit,
    onDownloadSpeedLimitChange: (Int) -> Unit,
    onAutoUnloadChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ===== AI Section =====
        item {
            Text(
                "Распознавание",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        item {
            val subtitle = if (activeModelName != null) {
                "$activeModelName · ${activeModelSummary ?: ""}"
            } else {
                "Не выбрана"
            }
            SettingsCard(
                icon = Icons.Filled.Psychology,
                title = "Модели ИИ",
                subtitle = subtitle,
                onClick = onOpenModelManager,
            )
        }

        item {
            ThreadSliderCard(
                threadCount = threadCount,
                onThreadCountChange = onThreadCountChange,
            )
        }

        item {
            DownloadParallelismCard(
                downloadParallelism = downloadParallelism,
                onDownloadParallelismChange = onDownloadParallelismChange,
            )
        }

        item {
            DownloadSpeedLimitCard(
                downloadSpeedLimitMbps = downloadSpeedLimitMbps,
                onDownloadSpeedLimitChange = onDownloadSpeedLimitChange,
            )
        }

        item {
            AutoUnloadSliderCard(
                autoUnloadMinutes = autoUnloadMinutes,
                onAutoUnloadChange = onAutoUnloadChange,
            )
        }

        // ===== General Section =====
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                "Общее",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        item {
            SettingsCard(
                icon = Icons.Filled.Language,
                title = "Язык",
                subtitle = "Выбор языка интерфейса",
                onClick = onOpenLanguage,
            )
        }

        item {
            SettingsCard(
                icon = Icons.Filled.Info,
                title = "О приложении",
                subtitle = "ChromaLab v0.0.2",
                onClick = onOpenAbout,
            )
        }
    }
}

@Composable
private fun SettingsCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun ThreadSliderCard(
    threadCount: Int,
    onThreadCountChange: (Int) -> Unit,
) {
    val maxThreads = Runtime.getRuntime().availableProcessors().coerceAtMost(16)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Memory,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Потоки CPU",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "Для llama.cpp инференса: $threadCount / $maxThreads",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Slider(
                value = threadCount.toFloat(),
                onValueChange = { onThreadCountChange(it.toInt()) },
                valueRange = 1f..maxThreads.toFloat(),
                steps = maxThreads - 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DownloadParallelismCard(
    downloadParallelism: Int,
    onDownloadParallelismChange: (Int) -> Unit,
) {
    val options = listOf(1, 2, 4, 8, 10, 12, 16)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Параллельное скачивание",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "$downloadParallelism HTTP range-поток(ов) для одного файла",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options) { option ->
                    FilterChip(
                        selected = option == downloadParallelism,
                        onClick = { onDownloadParallelismChange(option) },
                        label = { Text("${option}x") },
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadSpeedLimitCard(
    downloadSpeedLimitMbps: Int,
    onDownloadSpeedLimitChange: (Int) -> Unit,
) {
    val normalizedLimit = downloadSpeedLimitMbps.coerceIn(0, 50)
    val subtitle = if (normalizedLimit == 0) {
        "Ð‘ÐµÐ· Ð¾Ð³Ñ€Ð°Ð½Ð¸Ñ‡ÐµÐ½Ð¸Ñ"
    } else {
        "ÐžÐ±Ñ‰Ð¸Ð¹ Ð»Ð¸Ð¼Ð¸Ñ‚: $normalizedLimit MB/s"
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Speed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Ð¡ÐºÐ¾Ñ€Ð¾ÑÑ‚ÑŒ ÑÐºÐ°Ñ‡Ð¸Ð²Ð°Ð½Ð¸Ñ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Slider(
                value = normalizedLimit.toFloat(),
                onValueChange = { onDownloadSpeedLimitChange(it.roundToInt().coerceIn(0, 50)) },
                valueRange = 0f..50f,
                steps = 49,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "∞",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "50 MB/s",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AutoUnloadSliderCard(
    autoUnloadMinutes: Int,
    onAutoUnloadChange: (Int) -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Автовыгрузка модели",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (autoUnloadMinutes == 0) "Выключено"
                        else "Через $autoUnloadMinutes мин после использования",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Slider(
                value = autoUnloadMinutes.toFloat(),
                onValueChange = { onAutoUnloadChange(it.toInt()) },
                valueRange = 0f..30f,
                steps = 29,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

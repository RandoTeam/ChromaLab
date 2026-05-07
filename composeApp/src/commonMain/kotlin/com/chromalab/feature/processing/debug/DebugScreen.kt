package com.chromalab.feature.processing.debug

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing

/**
 * Debug information screen — shows pipeline timings, stats, and toggles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    config: DebugConfig,
    debugInfo: DebugInfo,
    onConfigChange: (DebugConfig) -> Unit,
    onExportPackage: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Master toggle
            item {
                Card {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Debug Mode", style = MaterialTheme.typography.titleSmall)
                        Switch(
                            checked = config.enabled,
                            onCheckedChange = { onConfigChange(config.copy(enabled = it)) },
                        )
                    }
                }
            }

            // Sub-toggles (only if enabled)
            if (config.enabled) {
                item { DebugToggle("Контуры и границы", config.showContours) { onConfigChange(config.copy(showContours = it)) } }
                item { DebugToggle("Области графиков и оси", config.showGraphRegions) { onConfigChange(config.copy(showGraphRegions = it)) } }
                item { DebugToggle("Маска и оверлей кривой", config.showCurveMask) { onConfigChange(config.copy(showCurveMask = it)) } }
                item { DebugToggle("Таймингы обработки", config.showTimings) { onConfigChange(config.copy(showTimings = it)) } }
            }

            // Pipeline stats
            item {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text("Статистика пайплайна", style = MaterialTheme.typography.titleSmall)
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(Spacing.md)) {
                        StatRow("Контуров найдено", "${debugInfo.contourCount}")
                        StatRow("Углов документа", "${debugInfo.documentCorners.size}")
                        StatRow("Областей графиков", "${debugInfo.graphRegionsDetected}")
                        StatRow("Линий осей", "${debugInfo.axisLinesDetected}")
                        StatRow("Пикселей маски", "${debugInfo.curveMaskPixelCount}")
                        StatRow("Точек кривой", "${debugInfo.extractedPointCount}")
                        StatRow("Подавлено блобов", "${debugInfo.suppressedBlobCount}")
                    }
                }
            }

            // Timings
            if (debugInfo.timings.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text("Таймингы", style = MaterialTheme.typography.titleSmall)
                }

                items(debugInfo.timings) { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(entry.step, style = MaterialTheme.typography.bodySmall)
                        Text(
                            "${entry.durationMs} мс",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Итого", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${debugInfo.timings.sumOf { it.durationMs }} мс",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            // Export
            item {
                Spacer(modifier = Modifier.height(Spacing.md))
                Button(
                    onClick = onExportPackage,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Экспорт debug-пакета")
                }
            }
        }
    }
}

@Composable
private fun DebugToggle(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
    }
}

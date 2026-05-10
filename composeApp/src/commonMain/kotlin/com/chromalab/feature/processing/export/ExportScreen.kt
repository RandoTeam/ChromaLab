package com.chromalab.feature.processing.export

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.storage.SessionWriter

/**
 * Export screen — share points as CSV or JSON.
 * Supports multi-graph: shows tabs when multiple signals are available.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    signal: DigitalSignal,
    bundle: ExportBundle,
    sessionWriter: SessionWriter,
    onBack: () -> Unit,
    allSignals: List<DigitalSignal> = listOf(signal),
    modifier: Modifier = Modifier,
) {
    val signals = allSignals.ifEmpty { listOf(signal) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val currentSignal = signals.getOrElse(selectedTab) { signal }

    var exportedPaths by remember { mutableStateOf(mapOf<String, String>()) }
    var snackMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackMessage) {
        snackMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (signals.size > 1) {
                        Text("Экспорт — ${signals.size} графиков")
                    } else {
                        Text("Экспорт данных")
                    }
                },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Graph tabs (only when multi-graph)
            if (signals.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    edgePadding = 0.dp,
                ) {
                    signals.forEachIndexed { index, sig ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    "График ${index + 1}",
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                        )
                    }
                }
            }

            // Signal summary
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        if (signals.size > 1) "График ${selectedTab + 1}" else "Сигнал",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text("Точек: ${currentSignal.points.size}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Диапазон: ${"%.2f".format(currentSignal.points.firstOrNull()?.time ?: 0f)} — ${"%.2f".format(currentSignal.points.lastOrNull()?.time ?: 0f)} ${currentSignal.timeUnit}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Интенсивность: ${"%.1f".format(currentSignal.minIntensity)} — ${"%.1f".format(currentSignal.maxIntensity)} ${currentSignal.intensityUnit}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            // CSV export — current graph
            val csvKey = "csv_$selectedTab"
            val csvFileName = if (signals.size > 1) "graph_${selectedTab + 1}_points.csv" else "points.csv"
            ExportCard(
                title = csvFileName,
                description = "index, pixel_x, pixel_y, time, intensity, confidence",
                exported = exportedPaths.containsKey(csvKey),
                onExport = {
                    val csv = PointExporter.exportCsv(currentSignal)
                    val path = sessionWriter.writeText(csvFileName, csv)
                    exportedPaths = exportedPaths + (csvKey to path)
                    snackMessage = "CSV сохранён: $csvFileName"
                },
                onShare = {
                    exportedPaths[csvKey]?.let { FileSharer.share(it, "text/csv") }
                },
            )

            // Export ALL — when multi-graph
            if (signals.size > 1) {
                val allKey = "csv_all"
                ExportCard(
                    title = "Все графики (${signals.size} CSV)",
                    description = "Сохранить все графики в отдельные файлы",
                    exported = exportedPaths.containsKey(allKey),
                    onExport = {
                        signals.forEachIndexed { idx, sig ->
                            val name = "graph_${idx + 1}_points.csv"
                            val csv = PointExporter.exportCsv(sig)
                            sessionWriter.writeText(name, csv)
                        }
                        exportedPaths = exportedPaths + (allKey to "all")
                        snackMessage = "Все ${signals.size} графиков сохранены"
                    },
                    onShare = {},
                )
            }

            // JSON export
            val jsonKey = "json_$selectedTab"
            val jsonFileName = if (signals.size > 1) "graph_${selectedTab + 1}_points.json" else "points.json"
            ExportCard(
                title = jsonFileName,
                description = "Полный пакет: метаданные, калибровка, параметры, точки",
                exported = exportedPaths.containsKey(jsonKey),
                onExport = {
                    val currentBundle = bundle.copy(signal = currentSignal)
                    val jsonStr = PointExporter.exportJson(currentBundle)
                    val path = sessionWriter.writeText(jsonFileName, jsonStr)
                    exportedPaths = exportedPaths + (jsonKey to path)
                    snackMessage = "JSON сохранён: $jsonFileName"
                },
                onShare = {
                    exportedPaths[jsonKey]?.let { FileSharer.share(it, "application/json") }
                },
            )

            Spacer(modifier = Modifier.weight(1f))

            // CSV preview (first 5 lines)
            if (exportedPaths.containsKey(csvKey)) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(Spacing.sm)) {
                        Text("Предпросмотр CSV:", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        val preview = PointExporter.exportCsv(currentSignal)
                            .lines()
                            .take(6)
                            .joinToString("\n")
                        Text(
                            preview,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                        if (currentSignal.points.size > 5) {
                            Text(
                                "… ещё ${currentSignal.points.size - 5} строк",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportCard(
    title: String,
    description: String,
    exported: Boolean,
    onExport: () -> Unit,
    onShare: () -> Unit,
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                if (!exported) {
                    Button(onClick = onExport) {
                        Text("Сохранить")
                    }
                } else {
                    OutlinedButton(onClick = onExport) {
                        Text("Обновить")
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Filled.Share, contentDescription = "Поделиться")
                    }
                }
            }
        }
    }
}

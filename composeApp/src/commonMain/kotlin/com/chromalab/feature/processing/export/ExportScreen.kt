package com.chromalab.feature.processing.export

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.storage.SessionWriter

/**
 * Export screen — share points as CSV or JSON.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    signal: DigitalSignal,
    bundle: ExportBundle,
    sessionWriter: SessionWriter,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var exportedCsvPath by remember { mutableStateOf<String?>(null) }
    var exportedJsonPath by remember { mutableStateOf<String?>(null) }
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
                title = { Text("Экспорт данных") },
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
            // Signal summary
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text("Сигнал", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text("Точек: ${signal.points.size}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Диапазон: ${"%.2f".format(signal.points.firstOrNull()?.time ?: 0f)} — ${"%.2f".format(signal.points.lastOrNull()?.time ?: 0f)} ${signal.timeUnit}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Интенсивность: ${"%.1f".format(signal.minIntensity)} — ${"%.1f".format(signal.maxIntensity)} ${signal.intensityUnit}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            // CSV export
            ExportCard(
                title = "points.csv",
                description = "index, pixel_x, pixel_y, time, intensity, confidence",
                exported = exportedCsvPath != null,
                onExport = {
                    val csv = PointExporter.exportCsv(signal)
                    exportedCsvPath = sessionWriter.writeText("points.csv", csv)
                    snackMessage = "CSV сохранён"
                },
                onShare = {
                    exportedCsvPath?.let { FileSharer.share(it, "text/csv") }
                },
            )

            // JSON export
            ExportCard(
                title = "points.json",
                description = "Полный пакет: метаданные, калибровка, параметры, точки",
                exported = exportedJsonPath != null,
                onExport = {
                    val jsonStr = PointExporter.exportJson(bundle)
                    exportedJsonPath = sessionWriter.writeText("points.json", jsonStr)
                    snackMessage = "JSON сохранён"
                },
                onShare = {
                    exportedJsonPath?.let { FileSharer.share(it, "application/json") }
                },
            )

            Spacer(modifier = Modifier.weight(1f))

            // CSV preview (first 5 lines)
            if (exportedCsvPath != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.padding(Spacing.sm)) {
                        Text("Предпросмотр CSV:", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        val preview = PointExporter.exportCsv(signal)
                            .lines()
                            .take(6)
                            .joinToString("\n")
                        Text(
                            preview,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                        if (signal.points.size > 5) {
                            Text(
                                "… ещё ${signal.points.size - 5} строк",
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

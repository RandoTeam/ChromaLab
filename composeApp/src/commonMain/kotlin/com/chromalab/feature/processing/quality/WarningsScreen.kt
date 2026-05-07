package com.chromalab.feature.processing.quality

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing

/**
 * Warnings list screen.
 * Shows all pipeline warnings grouped by severity.
 * Technical warnings are never hidden.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarningsScreen(
    report: DigitizationQualityReport,
    warnings: List<PipelineWarning>,
    onAccept: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Качество оцифровки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onAccept) {
                        Icon(Icons.Filled.Check, contentDescription = "Продолжить")
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
            // Overall quality card
            item {
                QualityOverviewCard(report)
            }

            // Stage scores
            item {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text("Оценки по этапам", style = MaterialTheme.typography.titleSmall)
            }

            item {
                StageScoreRow("Изображение", report.imageQuality)
                StageScoreRow("Документ", report.documentDetection)
                StageScoreRow("График", report.graphDetection)
                StageScoreRow("Калибровка", report.axisCalibration)
                StageScoreRow("Кривая", report.curveExtraction)
            }

            // Warnings list
            if (warnings.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        "Предупреждения (${warnings.size})",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                items(warnings) { warning ->
                    WarningItem(warning)
                }
            } else {
                item {
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Предупреждений нет — оцифровка выполнена успешно")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QualityOverviewCard(report: DigitizationQualityReport) {
    val (color, label) = when (report.overall.status) {
        QualityStatus.GOOD -> Color(0xFF4CAF50) to "Хорошо"
        QualityStatus.ACCEPTABLE -> Color(0xFFFF9800) to "Приемлемо"
        QualityStatus.RISKY -> Color(0xFFFF5722) to "Рискованно"
        QualityStatus.FAILED -> Color(0xFFF44336) to "Неудача"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    "Общее качество",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = color,
                )
            }
            Text(
                "${(report.overall.score * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                color = color,
            )
        }
    }
}

@Composable
private fun StageScoreRow(label: String, stage: StageQuality) {
    val color = when (stage.status) {
        QualityStatus.GOOD -> Color(0xFF4CAF50)
        QualityStatus.ACCEPTABLE -> Color(0xFFFF9800)
        QualityStatus.RISKY -> Color(0xFFFF5722)
        QualityStatus.FAILED -> Color(0xFFF44336)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { stage.score },
                modifier = Modifier.width(80.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "${(stage.score * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
        }
    }
}

@Composable
private fun WarningItem(warning: PipelineWarning) {
    val (icon, color) = when (warning.severity) {
        WarningSeverity.ERROR -> Icons.Filled.Warning to MaterialTheme.colorScheme.error
        WarningSeverity.WARNING -> Icons.Filled.Warning to Color(0xFFFF9800)
        WarningSeverity.INFO -> Icons.Filled.Info to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Column {
            Text(
                warning.message,
                style = MaterialTheme.typography.bodySmall,
                color = color,
            )
            warning.detail?.let { detail ->
                Text(
                    detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

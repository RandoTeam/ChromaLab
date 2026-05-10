package com.chromalab.feature.calculation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.calculation.export.*

/**
 * Export calculation screen — CSV, JSON, HTML export + Share.
 *
 * Connected to real ExportEngine logic:
 * - peaks.csv via PeaksCsvExporter
 * - calculation.json via CalculationJsonExporter
 * - report.html via ReportExporter
 * - Share via onShare callback with generated content
 */
@Composable
fun ExportCalculationScreen(
    run: CalculationRun,
    onFileSave: (fileName: String, content: String) -> Unit,
    onShare: (fileName: String, content: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    // Pre-build export data
    val exportData = remember(run) {
        CalculationRunToExportMapper.map(run)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Header
        Text(
            "Экспорт результатов",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "${run.peaks.size} пиков · Σ Area: ${formatExportNumber(run.peaks.sumOf { it.area })}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Export buttons
        ExportButton(
            title = "Таблица пиков (CSV)",
            subtitle = "peaks.csv — все метрики в табличном формате",
            icon = Icons.Filled.TableChart,
            color = Color(0xFF4CAF50),
            onClick = {
                val csv = PeaksCsvExporter.export(exportData.peaks)
                onFileSave("peaks_${run.id}.csv", csv)
                exportStatus = "✓ peaks.csv сохранён"
            },
        )

        ExportButton(
            title = "Полный расчёт (JSON)",
            subtitle = "calculation.json — все данные + параметры",
            icon = Icons.Filled.Code,
            color = Color(0xFF42A5F5),
            onClick = {
                val json = CalculationJsonExporter.export(exportData)
                onFileSave("calculation_${run.id}.json", json)
                exportStatus = "✓ calculation.json сохранён"
            },
        )

        ExportButton(
            title = "Отчёт (HTML)",
            subtitle = "report.html — профессиональный отчёт для печати",
            icon = Icons.Filled.Description,
            color = Color(0xFFAB47BC),
            onClick = {
                val html = ReportExporter.export(run)
                onFileSave("report_${run.id}.html", html)
                exportStatus = "✓ report.html сохранён"
            },
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))

        // Share button
        ExportButton(
            title = "Поделиться отчётом",
            subtitle = "Отправить HTML через системный диалог",
            icon = Icons.Filled.Share,
            color = MaterialTheme.colorScheme.primary,
            outlined = true,
            onClick = {
                val html = ReportExporter.export(run)
                onShare("chromalab_report_${run.id}.html", html)
                exportStatus = "✓ Отправлено"
            },
        )

        // Status
        if (exportStatus != null) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    exportStatus!!,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ExportButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    outlined: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (outlined) Color.Transparent
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (outlined) ButtonDefaults.outlinedButtonBorder(true)
        else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatExportNumber(value: Double): String = when {
    value >= 1_000_000 -> "%.2fM".format(value / 1_000_000)
    value >= 1_000 -> "%,.0f".format(value)
    else -> "%.1f".format(value)
}

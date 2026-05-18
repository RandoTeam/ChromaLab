package com.chromalab.feature.calculation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import com.chromalab.feature.calculation.flow.CalculationToChartMapper
import com.chromalab.feature.processing.export.ExportFileResult
import com.chromalab.feature.processing.export.FileSharer
import com.chromalab.feature.reports.CalculationRunReportOptions
import com.chromalab.feature.reports.StructuredReportPreview

/**
 * Export calculation screen — CSV, JSON, HTML export + Share.
 *
 * Connected to real ExportEngine logic:
 * - peaks.csv via PeaksCsvExporter
 * - calculation.json via CalculationJsonExporter
 * - chromatogram_report_ui_contract.json via CalculationRunReportExporter
 * - chromatogram_report.md via CalculationRunReportExporter
 * - chromatogram_report.html via CalculationRunReportExporter
 * - Share via onShare callback with generated content
 */
@Composable
fun ExportCalculationScreen(
    run: CalculationRun,
    modifier: Modifier = Modifier,
    reportOptions: CalculationRunReportOptions = CalculationRunReportOptions(),
    onFileSave: (fileName: String, content: String, mimeType: String) -> ExportFileResult = FileSharer::saveText,
    onShare: (fileName: String, content: String, mimeType: String) -> ExportFileResult = FileSharer::shareText,
) {
    var exportStatus by remember { mutableStateOf<String?>(null) }
    var showStructuredPreview by remember { mutableStateOf(true) }
    var showExportActions by remember { mutableStateOf(false) }

    // Pre-build export data
    val exportData = remember(run) {
        CalculationRunToExportMapper.map(run)
    }
    val structuredReport = remember(run, reportOptions) {
        CalculationRunReportExporter.buildReport(run, reportOptions)
    }
    val structuredValidation = remember(run, reportOptions) {
        CalculationRunReportExporter.validate(run, reportOptions)
    }
    val reportUiContract = remember(run, reportOptions) {
        CalculationRunReportExporter.buildUiContract(run, reportOptions)
    }
    val reportGraphOverlays = remember(run, structuredReport, reportOptions) {
        val graphIndex = structuredReport.graphs.singleOrNull()?.graphIndex ?: reportOptions.graphIndex.coerceAtLeast(1)
        mapOf(
            graphIndex to CalculationToChartMapper.buildChartState(
                run = run,
                visibleLayers = setOf("raw", "baseline", "corrected"),
            ),
        )
    }
    fun save(fileName: String, content: String, mimeType: String) {
        exportStatus = onFileSave(fileName, content, mimeType).statusText()
    }

    fun share(fileName: String, content: String, mimeType: String) {
        exportStatus = onShare(fileName, content, mimeType).statusText()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Header
        Text(
            "Final report",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "${run.peaks.size} peaks · Σ Area: ${formatExportNumber(run.peaks.sumOf { it.area })}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TextButton(onClick = { showStructuredPreview = !showStructuredPreview }) {
            Icon(
                imageVector = if (showStructuredPreview) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
            )
            Text(if (showStructuredPreview) "Hide report preview" else "Show report preview")
        }

        if (showStructuredPreview) {
            StructuredReportPreview(
                report = structuredReport,
                validation = structuredValidation,
                graphOverlays = reportGraphOverlays,
                uiContract = reportUiContract,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))

        TextButton(onClick = { showExportActions = !showExportActions }) {
            Icon(
                imageVector = if (showExportActions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
            )
            Text(if (showExportActions) "Hide export actions" else "Export or share")
        }

        if (showExportActions) {

        // Export buttons
        ExportButton(
            title = "Таблица пиков (CSV)",
            subtitle = "peaks.csv — все метрики в табличном формате",
            icon = Icons.Filled.TableChart,
            color = Color(0xFF4CAF50),
            onClick = {
                val csv = PeaksCsvExporter.export(exportData.peaks)
                save("peaks_${run.id}.csv", csv, "text/csv")
            },
            onShare = {
                val csv = PeaksCsvExporter.export(exportData.peaks)
                share("peaks_${run.id}.csv", csv, "text/csv")
            },
        )

        ExportButton(
            title = "Полный расчёт (JSON)",
            subtitle = "calculation.json — все данные + параметры",
            icon = Icons.Filled.Code,
            color = Color(0xFF42A5F5),
            onClick = {
                val json = CalculationJsonExporter.export(exportData)
                save("calculation_${run.id}.json", json, "application/json")
            },
            onShare = {
                val json = CalculationJsonExporter.export(exportData)
                share("calculation_${run.id}.json", json, "application/json")
            },
        )

        ExportButton(
            title = "Final report (HTML / PDF-ready)",
            subtitle = "chromatogram_report.html - user-facing report rendered from the UI contract",
            icon = Icons.Filled.Description,
            color = Color(0xFFAB47BC),
            onClick = {
                val html = CalculationRunReportExporter.exportHtml(run, reportOptions)
                save("chromatogram_report_${run.id}.html", html, "text/html")
            },
            onShare = {
                val html = CalculationRunReportExporter.exportHtml(run, reportOptions)
                share("chromatogram_report_${run.id}.html", html, "text/html")
            },
        )

        ExportButton(
            title = "Report UI contract (JSON)",
            subtitle = "chromatogram_report_ui_contract.json - structured surface map for UI/export",
            icon = Icons.Filled.Code,
            color = Color(0xFF26A69A),
            onClick = {
                val contractJson = CalculationRunReportExporter.exportUiContractJson(run, reportOptions)
                save("chromatogram_report_ui_contract_${run.id}.json", contractJson, "application/json")
            },
            onShare = {
                val contractJson = CalculationRunReportExporter.exportUiContractJson(run, reportOptions)
                share("chromatogram_report_ui_contract_${run.id}.json", contractJson, "application/json")
            },
        )

        ExportButton(
            title = "Structured report (Markdown)",
            subtitle = "chromatogram_report.md - strict report contract with explicit missing data",
            icon = Icons.Filled.Description,
            color = Color(0xFF7E57C2),
            onClick = {
                val markdown = CalculationRunReportExporter.exportMarkdown(run, reportOptions)
                save("chromatogram_report_${run.id}.md", markdown, "text/markdown")
            },
            onShare = {
                val markdown = CalculationRunReportExporter.exportMarkdown(run, reportOptions)
                share("chromatogram_report_${run.id}.md", markdown, "text/markdown")
            },
        )

        // Share button
        ExportButton(
            title = "Поделиться отчётом",
            subtitle = "Share the same structured HTML report",
            icon = Icons.Filled.Share,
            color = MaterialTheme.colorScheme.primary,
            outlined = true,
            onClick = {
                val html = CalculationRunReportExporter.exportHtml(run, reportOptions)
                share("chromatogram_report_${run.id}.html", html, "text/html")
            },
        )
        }

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
    onShare: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (outlined) Color.Transparent
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (outlined) ButtonDefaults.outlinedButtonBorder(true)
        else null,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
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
            if (onShare != null) {
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "Поделиться",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatExportNumber(value: Double): String = when {
    value >= 1_000_000 -> "%.2fM".format(value / 1_000_000)
    value >= 1_000 -> "%,.0f".format(value)
    else -> "%.1f".format(value)
}

private fun ExportFileResult.statusText(): String =
    if (success) "✓ $message" else "Export failed: $message"

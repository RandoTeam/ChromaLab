package com.chromalab.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing
import kotlin.math.abs

@Composable
fun StructuredReportPreview(
    report: ChromatogramReport,
    validation: ReportContractValidationResult,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Structured report preview",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = report.metadata.reportId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ValidationBadge(validation)
            }

            KeyValueGrid(
                rows = listOf(
                    "Schema" to report.metadata.schemaVersion,
                    "Source" to (report.metadata.sourceName ?: report.metadata.inputSourceType.name),
                    "Runtime" to report.metadata.executedRuntime.name,
                    "Graphs" to "${report.graphs.size}/${report.metadata.detectedGraphCount}",
                ),
            )

            report.graphs.forEach { graph ->
                GraphPreview(graph)
            }

            WarningPreview(report)
        }
    }
}

@Composable
private fun ValidationBadge(validation: ReportContractValidationResult) {
    val color = when {
        validation.errorCount > 0 -> MaterialTheme.colorScheme.errorContainer
        validation.warningCount > 0 -> Color(0xFFFFE0B2)
        else -> Color(0xFFC8E6C9)
    }
    val contentColor = when {
        validation.errorCount > 0 -> MaterialTheme.colorScheme.onErrorContainer
        validation.warningCount > 0 -> Color(0xFF5D4037)
        else -> Color(0xFF1B5E20)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color,
        contentColor = contentColor,
    ) {
        Text(
            text = "${validation.errorCount} errors / ${validation.warningCount} warnings",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun GraphPreview(graph: GraphReport) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = "Graph ${graph.graphIndex}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        KeyValueGrid(
            rows = listOf(
                "Title" to graph.identification.chromatogramTitle.renderText(),
                "Mode" to graph.identification.chromatogramMode.renderText(),
                "Ion" to graph.identification.ionOrChannel.renderText(),
                "Sample" to graph.identification.sampleName.renderText(),
                "Peaks" to (graph.quality.totalDetectedPeaks?.toString() ?: graph.peaks.size.toString()),
                "Dominant" to (graph.quality.dominantPeakNumber?.let { "#$it" } ?: "not calculated"),
                "Baseline" to graph.quality.baselineQuality.renderText(),
                "Mean S/N" to graph.quality.meanSnr.renderNumber(),
                "Total area" to graph.quality.globalIntegratedArea.renderNumber(),
            ),
        )

        PeakPreviewTable(graph.peaks)
    }
}

@Composable
private fun PeakPreviewTable(peaks: List<ReportPeak>) {
    if (peaks.isEmpty()) {
        Text(
            text = "No peaks available.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TableRow(
            cells = listOf("#", "RT", "Height", "Area %", "S/N", "Flags"),
            header = true,
        )
        peaks.take(8).forEach { peak ->
            TableRow(
                cells = listOf(
                    peak.number.toString(),
                    peak.retentionTime.renderNumber(),
                    peak.heightAboveBaseline.renderNumber(),
                    peak.areaPercent.renderNumber(),
                    peak.signalToNoise.renderNumber(),
                    peak.flags.takeIf { it.isNotEmpty() }?.joinToString("; ") ?: "",
                ),
                header = false,
            )
        }
        if (peaks.size > 8) {
            Text(
                text = "+ ${peaks.size - 8} more peaks in full export",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WarningPreview(report: ChromatogramReport) {
    val warnings = (report.warnings + report.graphs.flatMap { it.warnings })
        .sortedWith(compareByDescending<ReportWarning> { it.severity.rank() }.thenBy { it.code })

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Warnings",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (warnings.isEmpty()) {
            Text(
                text = "No warnings recorded.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }

        warnings.take(5).forEach { warning ->
            Text(
                text = "[${warning.severity.name}] ${warning.message}",
                style = MaterialTheme.typography.bodySmall,
                color = warning.severity.color(),
            )
        }
        if (warnings.size > 5) {
            Text(
                text = "+ ${warnings.size - 5} more warnings in full export",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun KeyValueGrid(rows: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        rows.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                pair.forEach { (label, value) ->
                    KeyValueCell(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) {
                    Column(modifier = Modifier.weight(1f)) {}
                }
            }
        }
    }
}

@Composable
private fun KeyValueCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TableRow(cells: List<String>, header: Boolean) {
    val textStyle = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    val color = if (header) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        cells.forEachIndexed { index, value ->
            Text(
                text = value,
                modifier = Modifier.weight(if (index == cells.lastIndex) 1.6f else 1f),
                style = textStyle,
                color = color,
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                fontFamily = if (index in 0..4) FontFamily.Monospace else FontFamily.Default,
                maxLines = if (index == cells.lastIndex) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun ReportTextValue.renderText(): String {
    val text = value?.takeIf { it.isNotBlank() }
    return when {
        text != null && status.isUsable() -> text
        text != null -> "${status.missingLabel()} ($text)"
        else -> status.missingLabel()
    }
}

private fun ReportDoubleValue.renderNumber(): String {
    val number = value
    if (number == null || number.isNaN() || number.isInfinite()) {
        return status.missingLabel()
    }
    val suffix = unit?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
    return "${number.formatReportNumber()}$suffix"
}

private fun ReportValueStatus.isUsable(): Boolean =
    this == ReportValueStatus.CALCULATED ||
        this == ReportValueStatus.DETECTED ||
        this == ReportValueStatus.INFERRED

private fun ReportValueStatus.missingLabel(): String =
    when (this) {
        ReportValueStatus.NOT_DETECTED -> "not detected"
        ReportValueStatus.INSUFFICIENT_CONFIDENCE -> "insufficient confidence"
        ReportValueStatus.FAILED -> "failed"
        ReportValueStatus.NOT_CALCULATED,
        ReportValueStatus.CALCULATED,
        ReportValueStatus.DETECTED,
        ReportValueStatus.INFERRED -> "not calculated"
    }

private fun ReportSeverity.rank(): Int =
    when (this) {
        ReportSeverity.FAILED -> 4
        ReportSeverity.SERIOUS -> 3
        ReportSeverity.WARNING -> 2
        ReportSeverity.INFO -> 1
    }

@Composable
private fun ReportSeverity.color(): Color =
    when (this) {
        ReportSeverity.FAILED,
        ReportSeverity.SERIOUS -> MaterialTheme.colorScheme.error
        ReportSeverity.WARNING -> Color(0xFF8A5A00)
        ReportSeverity.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }

private fun Double.formatReportNumber(): String =
    when {
        abs(this) >= 1000.0 -> "%.0f".format(this)
        abs(this) >= 10.0 -> "%.2f".format(this)
        else -> "%.4f".format(this)
    }

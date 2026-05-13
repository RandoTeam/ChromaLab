package com.chromalab.feature.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.calculation.ui.ChartLayer
import com.chromalab.feature.calculation.ui.ChartPeakMarker
import com.chromalab.feature.calculation.ui.ChartPoint
import com.chromalab.feature.calculation.ui.ChromatogramChart
import com.chromalab.feature.calculation.ui.ChromatogramChartState
import kotlin.math.abs

@Composable
fun StructuredReportPreview(
    report: ChromatogramReport,
    validation: ReportContractValidationResult,
    modifier: Modifier = Modifier,
    graphOverlays: Map<Int, ChromatogramChartState> = emptyMap(),
) {
    val allWarnings = remember(report) { report.allWarnings() }
    val qualityState = remember(report, validation) {
        buildQualityState(validation, allWarnings)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ReportHeader(
            report = report,
            validation = validation,
            qualityState = qualityState,
            warnings = allWarnings,
        )

        ReportSection(title = "Overview") {
            MetricGrid(
                rows = listOf(
                    "Source" to (report.metadata.sourceName ?: report.metadata.inputSourceType.name),
                    "Runtime" to report.metadata.executedRuntime.name,
                    "Model" to report.metadata.executedModel.renderModelName(),
                    "Analysis time" to report.metadata.totalAnalysisDurationMillis.renderDuration(),
                    "Graphs" to "${report.graphs.size}/${report.metadata.detectedGraphCount}",
                    "Schema" to report.metadata.schemaVersion,
                ),
            )
        }

        WarningSummary(warnings = allWarnings)

        report.graphs.forEach { graph ->
            GraphReportSection(
                graph = graph,
                overlay = graphOverlays[graph.graphIndex],
            )
        }

        TechnicalAppendix(report = report, warnings = allWarnings)
    }
}

@Composable
private fun ReportHeader(
    report: ChromatogramReport,
    validation: ReportContractValidationResult,
    qualityState: QualityState,
    warnings: List<ReportWarning>,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = report.primaryTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = report.metadata.reportId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.sm))
                SeverityBadge(
                    label = qualityState.label,
                    severity = qualityState.severity,
                )
            }

            Text(
                text = qualityState.message,
                style = MaterialTheme.typography.bodySmall,
                color = qualityState.severity.contentColor(),
            )

            MetricGrid(
                rows = listOf(
                    "Detected peaks" to report.totalPeakCount().toString(),
                    "Critical warnings" to warnings.count { it.severity == ReportSeverity.FAILED || it.severity == ReportSeverity.SERIOUS }.toString(),
                    "Validation" to "${validation.errorCount} errors / ${validation.warningCount} warnings",
                    "Input" to report.metadata.inputSourceType.name,
                ),
            )
        }
    }
}

@Composable
private fun GraphReportSection(
    graph: GraphReport,
    overlay: ChromatogramChartState?,
) {
    ReportSection(title = "Graph ${graph.graphIndex}") {
        IdentificationBlock(graph)
        HorizontalDivider()
        PreparationBlock(graph)
        HorizontalDivider()
        AxisBlock(graph)
        HorizontalDivider()
        GraphOverlayBlock(graph = graph, overlay = overlay)
        HorizontalDivider()
        PeakTable(peaks = graph.peaks)
        HorizontalDivider()
        QualityBlock(graph)
        HorizontalDivider()
        KovatsBlock(graph)
        HorizontalDivider()
        InterpretationBlock(graph)
    }
}

@Composable
private fun GraphOverlayBlock(
    graph: GraphReport,
    overlay: ChromatogramChartState?,
) {
    SectionBlock(title = "Graph overlay") {
        val chartState = remember(graph, overlay) {
            overlay?.withReportLabels(graph) ?: graph.toMetricOverlayState()
        }
        val hasRenderableData = chartState.layers.any { it.points.size >= 2 } || chartState.peaks.isNotEmpty()
        if (!hasRenderableData) {
            EmptyText("No graph overlay data available.")
            return@SectionBlock
        }

        MetricGrid(
            rows = listOf(
                "Overlay source" to if (overlay != null) "calculation signal" else "report metrics",
                "Signal layers" to chartState.layers.count { it.visible }.toString(),
            ),
        )
        ChromatogramChart(
            state = chartState,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            interactive = false,
            showPeakLabels = true,
            axisPadding = 44f,
        )
        OverlayLegend(chartState.layers)
    }
}

@Composable
private fun IdentificationBlock(graph: GraphReport) {
    SectionBlock(title = "Identification") {
        MetricGrid(
            rows = listOf(
                "Title" to graph.identification.chromatogramTitle.renderText(),
                "Analysis" to graph.identification.analysisType.renderText(),
                "Mode" to graph.identification.chromatogramMode.renderText(),
                "Ion/channel" to graph.identification.ionOrChannel.renderText(),
                "Ion range" to graph.identification.ionRange.renderText(),
                "Sample" to graph.identification.sampleName.renderText(),
                "Matrix" to graph.identification.matrix.renderText(),
                "Target class" to graph.identification.targetCompoundClass.renderText(),
            ),
        )
    }
}

@Composable
private fun PreparationBlock(graph: GraphReport) {
    SectionBlock(title = "Source and preparation") {
        MetricGrid(
            rows = listOf(
                "Crop confidence" to graph.source.cropConfidence.renderPercent(),
                "Scan mode" to (graph.source.scanMode ?: "not recorded"),
                "Title OCR" to graph.source.titleOcrConfidence.renderPercent(),
                "Axis OCR" to graph.source.axisOcrConfidence.renderPercent(),
                "Tick OCR" to graph.source.tickOcrConfidence.renderPercent(),
                "Manual review" to if (graph.source.manuallyAdjusted) "yes" else "no",
            ),
        )

        val selectedVariant = graph.source.selectedPreparationVariant
        if (selectedVariant != null) {
            DetailLine(
                label = "Selected variant",
                value = "${selectedVariant.configName} (${selectedVariant.inputVariant}, score ${selectedVariant.score.formatReportNumber()})",
            )
        }
        if (graph.source.preprocessingSteps.isNotEmpty()) {
            Text(
                text = graph.source.preprocessingSteps.joinToString(" -> "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AxisBlock(graph: GraphReport) {
    SectionBlock(title = "Axis calibration") {
        MetricGrid(
            rows = listOf(
                "X label" to graph.axisCalibration.xAxis.label.renderText(),
                "X range" to graph.axisCalibration.xAxis.renderRange(),
                "Y label" to graph.axisCalibration.yAxis.label.renderText(),
                "Y range" to graph.axisCalibration.yAxis.renderRange(),
                "Calibration" to graph.axisCalibration.calibrationConfidence.renderPercent(),
                "Transform" to (graph.axisCalibration.pixelToUnitTransform?.method ?: "not calculated"),
            ),
        )

        if (graph.axisCalibration.warnings.isNotEmpty()) {
            CompactWarningList(graph.axisCalibration.warnings, maxItems = 3)
        }
    }
}

@Composable
private fun OverlayLegend(layers: List<ChartLayer>) {
    val visible = layers.filter { it.visible }
    if (visible.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visible.forEach { layer ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(8.dp),
                    shape = RoundedCornerShape(50),
                    color = layer.color,
                    content = {},
                )
                Text(
                    text = layer.id.overlayLabel(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PeakTable(peaks: List<ReportPeak>) {
    SectionBlock(title = "Peak table") {
        if (peaks.isEmpty()) {
            EmptyText("No peaks available.")
            return@SectionBlock
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TableRow(
                cells = listOf("#", "RT", "Height", "Area %", "S/N", "Overlap", "Compound", "Flags"),
                widths = listOf(44.dp, 82.dp, 98.dp, 82.dp, 78.dp, 136.dp, 170.dp, 180.dp),
                header = true,
            )
            peaks.forEach { peak ->
                TableRow(
                    cells = listOf(
                        peak.number.toString(),
                        peak.retentionTime.renderNumber(),
                        peak.heightAboveBaseline.renderNumber(),
                        peak.areaPercent.renderNumber(),
                        peak.signalToNoise.renderNumber(),
                        peak.overlapClass.renderText(),
                        peak.compound?.probableName?.renderText() ?: "not assigned",
                        peak.flags.joinToString("; ").ifBlank { peak.warningSummary() },
                    ),
                    widths = listOf(44.dp, 82.dp, 98.dp, 82.dp, 78.dp, 136.dp, 170.dp, 180.dp),
                    header = false,
                )
            }
        }
    }
}

@Composable
private fun QualityBlock(graph: GraphReport) {
    SectionBlock(title = "Chromatographic quality") {
        MetricGrid(
            rows = listOf(
                "Total peaks" to (graph.quality.totalDetectedPeaks?.toString() ?: graph.peaks.size.toString()),
                "Significant" to (graph.quality.significantPeakCount?.toString() ?: "not calculated"),
                "Mean S/N" to graph.quality.meanSnr.renderNumber(),
                "Median S/N" to graph.quality.medianSnr.renderNumber(),
                "Max height" to graph.quality.maximumPeakHeight.renderNumber(),
                "Dominant peak" to (graph.quality.dominantPeakNumber?.let { "#$it" } ?: "not calculated"),
                "Baseline" to graph.quality.baselineQuality.renderText(),
                "Minimum Rs" to graph.quality.minimumResolution.renderNumber(),
                "Global area" to graph.quality.globalIntegratedArea.renderNumber(),
                "Area status" to graph.quality.areaNormalizationStatus.renderText(),
            ),
        )

        if (graph.quality.anomalies.isNotEmpty()) {
            CompactAnomalyList(graph.quality.anomalies, maxItems = 4)
        }
    }
}

@Composable
private fun KovatsBlock(graph: GraphReport) {
    SectionBlock(title = "Kovats index") {
        MetricGrid(
            rows = listOf(
                "Status" to graph.kovats.status.name,
                "Formula" to (graph.kovats.formula ?: "not calculated"),
                "Reference" to (graph.kovats.referenceSeries ?: "not supplied"),
                "Linearity R2" to graph.kovats.trendLinearityR2.renderNumber(),
            ),
        )

        if (graph.kovats.results.isNotEmpty()) {
            graph.kovats.results.take(6).forEach { result ->
                DetailLine(
                    label = "Peak #${result.peakNumber}",
                    value = listOfNotNull(
                        result.compoundName,
                        result.carbonNumber,
                        result.calculatedIndex.renderNumber(),
                        result.calculationKind.name,
                    ).joinToString(" / "),
                )
            }
        }

        if (graph.kovats.missingDataNotes.isNotEmpty()) {
            Text(
                text = graph.kovats.missingDataNotes.take(2).joinToString(" "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InterpretationBlock(graph: GraphReport) {
    SectionBlock(title = "Distribution and interpretation") {
        MetricGrid(
            rows = listOf(
                "Likely class" to graph.interpretation.likelyCompoundClass.renderText(),
                "Unresolved" to graph.interpretation.unresolvedAssignments.size.toString(),
            ),
        )

        if (graph.interpretation.distributionByCarbonNumber.isNotEmpty()) {
            graph.interpretation.distributionByCarbonNumber.take(6).forEach { bucket ->
                DetailLine(
                    label = bucket.label,
                    value = "${bucket.areaPercent.renderNumber()} / ${bucket.peakCount ?: 0} peaks",
                )
            }
        }

        val notes = graph.interpretation.homologSeriesNotes + graph.interpretation.domainContextNotes
        notes.take(3).forEach { note ->
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WarningSummary(warnings: List<ReportWarning>) {
    ReportSection(title = "Warnings and red flags") {
        if (warnings.isEmpty()) {
            EmptyText("No warnings recorded.")
            return@ReportSection
        }

        ReportSeverity.entries
            .sortedByDescending { it.rank() }
            .forEach { severity ->
                val group = warnings.filter { it.severity == severity }
                if (group.isNotEmpty()) {
                    SeverityBadge(
                        label = "${severity.label()} (${group.size})",
                        severity = severity,
                    )
                    CompactWarningList(group, maxItems = 4)
                }
            }
    }
}

@Composable
private fun TechnicalAppendix(
    report: ChromatogramReport,
    warnings: List<ReportWarning>,
) {
    var expanded by remember { mutableStateOf(false) }
    ReportSection(title = "Technical appendix") {
        TextButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
            )
            Text(if (expanded) "Hide appendix" else "Show appendix")
        }

        if (!expanded) {
            Text(
                text = "Model, runtime, stage timing, preparation variants, warning codes, and value provenance are kept here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@ReportSection
        }

        SectionBlock(title = "Runtime") {
            MetricGrid(
                rows = listOf(
                    "Selected model" to report.metadata.selectedModel.renderModelName(),
                    "Selected runtime" to (report.metadata.selectedModel?.runtime?.name ?: "not recorded"),
                    "Executed model" to report.metadata.executedModel.renderModelName(),
                    "Executed runtime" to report.metadata.executedRuntime.name,
                    "Device" to (report.metadata.deviceName ?: "not recorded"),
                    "Mode" to report.metadata.processingMode.name,
                ),
            )
        }

        SectionBlock(title = "Stage timings") {
            if (report.metadata.stageTimings.isEmpty()) {
                EmptyText("No stage timings recorded.")
            } else {
                report.metadata.stageTimings.forEach { timing ->
                    DetailLine(
                        label = timing.stageName ?: timing.stageId,
                        value = timing.durationMillis.renderDuration(),
                    )
                }
            }
        }

        SectionBlock(title = "Warning codes") {
            CompactWarningList(warnings, maxItems = 20, includeCode = true)
        }
    }
}

@Composable
private fun ReportSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun SectionBlock(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        content()
    }
}

@Composable
private fun MetricGrid(rows: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        rows.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                pair.forEach { (label, value) ->
                    MetricCell(
                        label = label,
                        value = value,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
private fun TableRow(
    cells: List<String>,
    widths: List<Dp>,
    header: Boolean,
) {
    val textStyle = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    val textColor = if (header) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier.widthIn(min = widths.fold(0.dp) { total, width -> total + width }),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        cells.forEachIndexed { index, value ->
            Text(
                text = value,
                modifier = Modifier.width(widths[index]),
                style = textStyle,
                color = textColor,
                fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal,
                fontFamily = if (index <= 4) FontFamily.Monospace else FontFamily.Default,
                maxLines = if (index >= 5) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CompactWarningList(
    warnings: List<ReportWarning>,
    maxItems: Int,
    includeCode: Boolean = false,
) {
    if (warnings.isEmpty()) {
        EmptyText("No warnings recorded.")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        warnings.take(maxItems).forEach { warning ->
            Text(
                text = warning.renderWarning(includeCode),
                style = MaterialTheme.typography.bodySmall,
                color = warning.severity.contentColor(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (warnings.size > maxItems) {
            Text(
                text = "+ ${warnings.size - maxItems} more",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompactAnomalyList(
    anomalies: List<ReportAnomaly>,
    maxItems: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        anomalies.take(maxItems).forEach { anomaly ->
            Text(
                text = anomaly.message,
                style = MaterialTheme.typography.bodySmall,
                color = anomaly.severity.contentColor(),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (anomalies.size > maxItems) {
            Text(
                text = "+ ${anomalies.size - maxItems} more anomalies",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.8f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SeverityBadge(label: String, severity: ReportSeverity) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = severity.containerColor(),
        contentColor = severity.contentColor(),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun buildQualityState(
    validation: ReportContractValidationResult,
    warnings: List<ReportWarning>,
): QualityState {
    val failed = warnings.count { it.severity == ReportSeverity.FAILED }
    val serious = warnings.count { it.severity == ReportSeverity.SERIOUS }
    return when {
        validation.errorCount > 0 || failed > 0 -> QualityState(
            label = "Blocked",
            message = "The report has failed validation or failed analysis stages. Treat it as not release-ready.",
            severity = ReportSeverity.FAILED,
        )
        serious > 0 -> QualityState(
            label = "Review",
            message = "Critical analytical warnings are present. Review graph preparation, runtime, baseline, and peak integration before using conclusions.",
            severity = ReportSeverity.SERIOUS,
        )
        validation.warningCount > 0 || warnings.any { it.severity == ReportSeverity.WARNING } -> QualityState(
            label = "Caution",
            message = "The report is structured, but warnings remain in the analytical audit trail.",
            severity = ReportSeverity.WARNING,
        )
        else -> QualityState(
            label = "Ready",
            message = "No blocking validation or analytical warnings are recorded.",
            severity = ReportSeverity.INFO,
        )
    }
}

private fun ChromatogramChartState.withReportLabels(graph: GraphReport): ChromatogramChartState {
    val labelsByTime = graph.peaks.mapNotNull { peak ->
        peak.retentionTime.value.takeIfUsable()?.let { time ->
            time to peak.renderOverlayLabel()
        }
    }
    if (labelsByTime.isEmpty()) return this

    return copy(
        peaks = peaks.map { marker ->
            val label = labelsByTime.minByOrNull { abs(it.first - marker.apexTime) }?.second
            marker.copy(label = label ?: marker.label)
        },
    )
}

private fun GraphReport.toMetricOverlayState(): ChromatogramChartState {
    val sortedPeaks = peaks.sortedBy { it.retentionTime.value ?: Double.MAX_VALUE }
    if (sortedPeaks.isEmpty()) return ChromatogramChartState()

    val baseline = sortedPeaks.mapNotNull { it.baselineAtApex.value.takeIfUsable() }.takeIf { it.isNotEmpty() }?.average()
        ?: signal.baselineMean.value.takeIfUsable()
        ?: 0.0
    val xMinimum = axisCalibration.xAxis.visibleMinimum.value.takeIfUsable()
        ?: sortedPeaks.firstNotNullOfOrNull { it.startRetentionTime.value.takeIfUsable() }
        ?: sortedPeaks.firstNotNullOfOrNull { it.retentionTime.value.takeIfUsable() }
        ?: 0.0
    val xMaximum = axisCalibration.xAxis.visibleMaximum.value.takeIfUsable()
        ?: sortedPeaks.mapNotNull { it.endRetentionTime.value.takeIfUsable() }.lastOrNull()
        ?: sortedPeaks.mapNotNull { it.retentionTime.value.takeIfUsable() }.lastOrNull()
        ?: (xMinimum + 1.0)
    val signalPoints = sortedPeaks.flatMap { peak ->
        peak.toMetricSignalPoints(defaultBaseline = baseline)
    }.plus(
        listOf(
            ChartPoint(xMinimum, baseline),
            ChartPoint(xMaximum, baseline),
        ),
    ).sortedBy { it.time }

    val markerData = sortedPeaks.mapNotNull { peak ->
        val apexTime = peak.retentionTime.value.takeIfUsable() ?: return@mapNotNull null
        val peakBaseline = peak.baselineAtApex.value.takeIfUsable() ?: baseline
        val height = peak.heightAboveBaseline.value.takeIfUsable()
            ?: peak.absoluteApexIntensity.value.takeIfUsable()?.minus(peakBaseline)
            ?: return@mapNotNull null
        ChartPeakMarker(
            apexTime = apexTime,
            apexIntensity = peakBaseline + height,
            leftBoundaryTime = peak.startRetentionTime.value.takeIfUsable() ?: apexTime,
            rightBoundaryTime = peak.endRetentionTime.value.takeIfUsable() ?: apexTime,
            label = peak.renderOverlayLabel(),
            confidenceColor = peak.overlayColor(),
        )
    }

    return ChromatogramChartState(
        layers = listOf(
            ChartLayer(
                id = "reported_signal",
                points = signalPoints,
                color = Color(0xFF1E88E5),
                strokeWidth = 2.2f,
                visible = true,
            ),
            ChartLayer(
                id = "baseline",
                points = listOf(
                    ChartPoint(xMinimum, baseline),
                    ChartPoint(xMaximum, baseline),
                ),
                color = Color(0xFF757575),
                strokeWidth = 1.4f,
                visible = true,
            ),
        ),
        peaks = markerData,
    )
}

private fun ReportPeak.toMetricSignalPoints(defaultBaseline: Double): List<ChartPoint> {
    val apexTime = retentionTime.value.takeIfUsable() ?: return emptyList()
    val baseline = baselineAtApex.value.takeIfUsable() ?: defaultBaseline
    val height = heightAboveBaseline.value.takeIfUsable()
        ?: absoluteApexIntensity.value.takeIfUsable()?.minus(baseline)
        ?: return emptyList()
    val left = startRetentionTime.value.takeIfUsable() ?: (apexTime - widthAtBase.value.takeIfUsable().orDefaultWidth() / 2.0)
    val right = endRetentionTime.value.takeIfUsable() ?: (apexTime + widthAtBase.value.takeIfUsable().orDefaultWidth() / 2.0)
    if (right <= left) return listOf(ChartPoint(apexTime, baseline + height))

    val leftMid = left + (apexTime - left) * 0.55
    val rightMid = apexTime + (right - apexTime) * 0.55
    return listOf(
        ChartPoint(left, baseline),
        ChartPoint(leftMid, baseline + height * 0.35),
        ChartPoint(apexTime, baseline + height),
        ChartPoint(rightMid, baseline + height * 0.35),
        ChartPoint(right, baseline),
    )
}

private fun ChromatogramReport.allWarnings(): List<ReportWarning> =
    (warnings + graphs.flatMap { graph ->
        graph.warnings + graph.axisCalibration.warnings + graph.peaks.flatMap { it.warnings }
    }).distinctBy { warning ->
        listOf(
            warning.code,
            warning.stage.orEmpty(),
            warning.graphIndex?.toString().orEmpty(),
            warning.peakNumber?.toString().orEmpty(),
        ).joinToString("|")
    }.sortedWith(
        compareByDescending<ReportWarning> { it.severity.rank() }
            .thenBy { it.graphIndex ?: Int.MAX_VALUE }
            .thenBy { it.peakNumber ?: Int.MAX_VALUE }
            .thenBy { it.code },
    )

private fun ChromatogramReport.primaryTitle(): String =
    graphs.firstOrNull()
        ?.identification
        ?.chromatogramTitle
        ?.value
        ?.takeIf { it.isNotBlank() }
        ?: graphs.firstOrNull()
            ?.identification
            ?.ionOrChannel
            ?.value
            ?.takeIf { it.isNotBlank() }
        ?: "Chromatogram report"

private fun ChromatogramReport.totalPeakCount(): Int =
    graphs.sumOf { graph -> graph.quality.totalDetectedPeaks ?: graph.peaks.size }

private fun ModelExecutionInfo?.renderModelName(): String =
    this?.modelName?.takeIf { it.isNotBlank() }
        ?: this?.modelId?.takeIf { it.isNotBlank() }
        ?: "not recorded"

private fun ReportWarning.renderWarning(includeCode: Boolean): String {
    val location = when {
        graphIndex != null && peakNumber != null -> "G$graphIndex P$peakNumber"
        graphIndex != null -> "G$graphIndex"
        else -> null
    }
    val prefix = listOfNotNull(
        severity.label(),
        location,
        code.takeIf { includeCode },
    ).joinToString(" / ")
    return "$prefix: $message"
}

private fun ReportPeak.warningSummary(): String =
    warnings.takeIf { it.isNotEmpty() }
        ?.joinToString("; ") { it.code }
        .orEmpty()

private fun ReportPeak.renderOverlayLabel(): String =
    compound?.carbonNumber?.value?.takeIf { it.isNotBlank() }
        ?: compound?.probableName?.value?.takeIf { it.isNotBlank() }?.take(10)
        ?: "#$number"

private fun ReportPeak.overlayColor(): Color {
    val overlap = overlapClass.value?.uppercase().orEmpty()
    return when {
        confidence != null && confidence < 0.5 -> Color(0xFFD32F2F)
        "UNRESOLVED" in overlap || "SHOULDER" in overlap || "PARTIALLY" in overlap -> Color(0xFFFF9800)
        else -> Color(0xFF2E7D32)
    }
}

private fun String.overlayLabel(): String =
    when (this) {
        "raw" -> "Raw"
        "smoothed" -> "Smoothed"
        "baseline" -> "Baseline"
        "corrected" -> "Corrected"
        "reported_signal" -> "Signal"
        else -> replace('_', ' ').replaceFirstChar { it.titlecase() }
    }

private fun AxisReport.renderRange(): String {
    val minimum = visibleMinimum.renderNumberWithoutUnit()
    val maximum = visibleMaximum.renderNumberWithoutUnit()
    val unit = unit.value?.takeIf { it.isNotBlank() }
    return listOfNotNull(minimum, maximum).joinToString(" - ") + unit?.let { " $it" }.orEmpty()
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

private fun ReportDoubleValue.renderNumberWithoutUnit(): String {
    val number = value
    return if (number == null || number.isNaN() || number.isInfinite()) {
        status.missingLabel()
    } else {
        number.formatReportNumber()
    }
}

private fun Double?.renderPercent(): String {
    val value = this
    return if (value == null || value.isNaN() || value.isInfinite()) {
        "not recorded"
    } else {
        "${(value * 100.0).formatReportNumber()}%"
    }
}

private fun Double?.takeIfUsable(): Double? =
    if (this != null && !isNaN() && !isInfinite()) this else null

private fun Double?.orDefaultWidth(): Double =
    this?.takeIf { it > 0.0 } ?: 0.2

private fun Long?.renderDuration(): String {
    val millis = this ?: return "not recorded"
    if (millis < 1_000L) return "${millis} ms"
    val seconds = millis / 1_000.0
    return if (seconds < 60.0) {
        "${seconds.formatReportNumber()} s"
    } else {
        val minutes = seconds / 60.0
        "${minutes.formatReportNumber()} min"
    }
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

private fun ReportSeverity.label(): String =
    when (this) {
        ReportSeverity.FAILED -> "Failed"
        ReportSeverity.SERIOUS -> "Serious"
        ReportSeverity.WARNING -> "Warning"
        ReportSeverity.INFO -> "Info"
    }

@Composable
private fun ReportSeverity.containerColor(): Color =
    when (this) {
        ReportSeverity.FAILED -> MaterialTheme.colorScheme.errorContainer
        ReportSeverity.SERIOUS -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
        ReportSeverity.WARNING -> Color(0xFFFFE0B2)
        ReportSeverity.INFO -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.70f)
    }

@Composable
private fun ReportSeverity.contentColor(): Color =
    when (this) {
        ReportSeverity.FAILED,
        ReportSeverity.SERIOUS -> MaterialTheme.colorScheme.onErrorContainer
        ReportSeverity.WARNING -> Color(0xFF5D4037)
        ReportSeverity.INFO -> MaterialTheme.colorScheme.onPrimaryContainer
    }

private fun Double.formatReportNumber(): String =
    when {
        abs(this) >= 1000.0 -> "%.0f".format(this)
        abs(this) >= 10.0 -> "%.2f".format(this)
        else -> "%.4f".format(this)
    }

private data class QualityState(
    val label: String,
    val message: String,
    val severity: ReportSeverity,
)

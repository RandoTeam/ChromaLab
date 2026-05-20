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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.chromalab.feature.processing.geometry.AxisCalibrationFit
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import com.chromalab.feature.processing.geometry.GeometryTrace
import com.chromalab.feature.processing.graph.GraphRegion
import kotlin.math.abs

@Composable
fun StructuredReportPreview(
    report: ChromatogramReport,
    validation: ReportContractValidationResult,
    modifier: Modifier = Modifier,
    graphOverlays: Map<Int, ChromatogramChartState> = emptyMap(),
    uiContract: ChromatogramReportUiContract? = null,
) {
    val reportUiContract = uiContract ?: remember(report, validation) {
        ChromatogramReportUiContractBuilder.build(report, validation)
    }
    val allWarnings = remember(report) { report.allWarnings() }
    val qualityState = remember(report, validation, reportUiContract) {
        buildQualityState(reportUiContract, validation, allWarnings)
    }
    val graphsByIndex = remember(report) { report.graphs.associateBy { it.graphIndex } }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ReportHeader(
            report = report,
            validation = validation,
            qualityState = qualityState,
            warnings = allWarnings,
            uiContract = reportUiContract,
        )

        CompactReportMetadata(
            report = report,
            graphOverlays = graphOverlays,
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

        QualitySummary(
            report = report,
            qualityState = qualityState,
            warnings = allWarnings,
            uiContract = reportUiContract,
        )

        reportUiContract.graphs.forEach { graphContract ->
            val graph = graphsByIndex[graphContract.graphIndex] ?: return@forEach
            GraphReportSection(
                graph = graph,
                overlay = graphOverlays[graph.graphIndex],
                uiContract = graphContract,
            )
        }

        TechnicalAppendix(report = report, warnings = allWarnings, uiContract = reportUiContract)
    }
}

@Composable
private fun CompactReportMetadata(
    report: ChromatogramReport,
    graphOverlays: Map<Int, ChromatogramChartState>,
) {
    ReportSection(title = "Run metadata") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            MetadataTile(
                label = "Model",
                value = report.metadata.executedModel.renderModelName(),
                supporting = "selected: ${report.metadata.selectedModel.renderModelName()}",
            )
            MetadataTile(
                label = "Runtime",
                value = report.metadata.executedRuntime.name,
                supporting = report.metadata.executedModel?.backendLabel?.takeIf { it.isNotBlank() }
                    ?: "backend not recorded",
            )
            MetadataTile(
                label = "Analysis time",
                value = report.metadata.totalAnalysisDurationMillis.renderDuration(),
                supporting = report.metadata.timingWindowLabel(),
            )
            MetadataTile(
                label = "Device",
                value = report.metadata.deviceName ?: "not recorded",
                supporting = report.metadata.processingMode.name,
            )
            MetadataTile(
                label = "Stages",
                value = report.metadata.stageTimings.size.toString(),
                supporting = report.metadata.stageTimingPreview(),
            )
        }

        GraphPreviewStrip(
            graphs = report.graphs,
            graphOverlays = graphOverlays,
        )
    }
}

@Composable
private fun MetadataTile(
    label: String,
    value: String,
    supporting: String,
) {
    Surface(
        modifier = Modifier.width(170.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(4.dp),
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
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GraphPreviewStrip(
    graphs: List<GraphReport>,
    graphOverlays: Map<Int, ChromatogramChartState>,
) {
    if (graphs.isEmpty()) {
        EmptyText("No graphs recorded.")
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = "Graph previews",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.Top,
        ) {
            graphs.forEach { graph ->
                GraphPreviewTile(
                    graph = graph,
                    overlaySource = if (graphOverlays.containsKey(graph.graphIndex)) {
                        "calculation signal"
                    } else {
                        "report metrics"
                    },
                )
            }
        }
    }
}

@Composable
private fun GraphPreviewTile(
    graph: GraphReport,
    overlaySource: String,
) {
    Surface(
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Graph ${graph.graphIndex}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                SeverityBadge(
                    label = graph.graphPreviewQualityLabel(),
                    severity = graph.graphPreviewSeverity(),
                )
            }
            Text(
                text = graph.graphPreviewTitle(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            DetailLine(label = "Crop", value = graph.source.cropConfidence.renderPercent())
            DetailLine(label = "Bounds", value = graph.source.detectedGraphBounds.renderRect())
            DetailLine(label = "Peaks", value = (graph.quality.totalDetectedPeaks ?: graph.peaks.size).toString())
            DetailLine(label = "Overlay", value = overlaySource)
        }
    }
}

@Composable
private fun ReportHeader(
    report: ChromatogramReport,
    validation: ReportContractValidationResult,
    qualityState: QualityState,
    warnings: List<ReportWarning>,
    uiContract: ChromatogramReportUiContract,
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
                    label = uiContract.reportGateStatus.name,
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
                    "Gate reasons" to (uiContract.reportGateBlockingReasons.size + uiContract.reportGateReviewReasons.size).toString(),
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
    uiContract: GraphReportUiContract,
) {
    ReportSection(title = "Graph ${graph.graphIndex}") {
        IdentificationBlock(graph)
        HorizontalDivider()
        PreparationBlock(graph)
        VisualEvidenceStrip(uiContract.visualEvidenceFor("source_and_graph_preparation"))
        HorizontalDivider()
        AxisBlock(graph)
        VisualEvidenceStrip(uiContract.visualEvidenceFor("axis_calibration"))
        HorizontalDivider()
        GraphOverlayBlock(
            graph = graph,
            overlay = overlay,
            visualEvidence = uiContract.visualEvidenceFor("interactive_or_rendered_graph"),
        )
        HorizontalDivider()
        PeakTable(
            graph = graph,
            visualEvidence = uiContract.visualEvidenceFor("peak_table"),
        )
        HorizontalDivider()
        PeakRecoveryBlock(
            graph = graph,
            visualEvidence = uiContract.visualEvidenceFor("peak_label_evidence_and_recovery"),
        )
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
    visualEvidence: List<ReportVisualEvidenceContract>,
) {
    SectionBlock(title = "Graph overlay") {
        VisualEvidenceStrip(visualEvidence)
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
                "Geometry" to graph.source.geometryReportStatus.renderGeometryStatus(),
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
        graph.source.geometryTrace?.warnings?.takeIf { it.isNotEmpty() }?.let { warnings ->
            QualityNotice(
                title = "Geometry evidence needs review",
                message = warnings.take(3).joinToString("; "),
                severity = ReportSeverity.WARNING,
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
                "X fit" to graph.axisCalibration.xCalibrationFit.renderCalibrationFitStatus(),
                "Y fit" to graph.axisCalibration.yCalibrationFit.renderCalibrationFitStatus(),
                "Transform" to (graph.axisCalibration.pixelToUnitTransform?.method ?: "not calculated"),
            ),
        )

        if (graph.axisCalibration.warnings.isNotEmpty()) {
            QualityNotice(
                title = "Axis calibration needs review",
                message = "Axis labels, ticks, or geometry were not fully reliable. See the technical appendix for exact warning codes.",
                severity = graph.axisCalibration.warnings.maxSeverity(),
            )
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
private fun PeakTable(
    graph: GraphReport,
    visualEvidence: List<ReportVisualEvidenceContract>,
) {
    SectionBlock(title = "Peak table") {
        VisualEvidenceStrip(visualEvidence)
        val peaks = graph.peaks
        if (peaks.isEmpty()) {
            EmptyText("No peaks available.")
            return@SectionBlock
        }
        val evidenceByPeak = graph.peakRecovery.peakEvidenceTable.associateBy { it.peakNumber }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TableRow(
                cells = listOf("#", "Evidence", "Gate", "RT", "Height", "Area %", "S/N", "Overlap", "Compound", "Flags"),
                widths = listOf(44.dp, 118.dp, 76.dp, 82.dp, 98.dp, 82.dp, 78.dp, 136.dp, 170.dp, 180.dp),
                header = true,
            )
            peaks.forEach { peak ->
                val evidence = evidenceByPeak[peak.number]
                TableRow(
                    cells = listOf(
                        peak.number.toString(),
                        evidence?.status?.name ?: "UNKNOWN",
                        evidence?.gateStatus?.name ?: "MISSING",
                        peak.retentionTime.renderNumber(),
                        peak.heightAboveBaseline.renderNumber(),
                        peak.areaPercent.renderNumber(),
                        peak.signalToNoise.renderNumber(),
                        peak.overlapClass.renderText(),
                        peak.compound.renderCompoundAssignment(),
                        peak.flags.joinToString("; ").ifBlank { peak.warningSummary() },
                    ),
                    widths = listOf(44.dp, 118.dp, 76.dp, 82.dp, 98.dp, 82.dp, 78.dp, 136.dp, 170.dp, 180.dp),
                    header = false,
                )
            }
        }
    }
}

@Composable
private fun PeakRecoveryBlock(
    graph: GraphReport,
    visualEvidence: List<ReportVisualEvidenceContract>,
) {
    SectionBlock(title = "Peak label evidence and recovery") {
        VisualEvidenceStrip(visualEvidence)
        val recovery = graph.peakRecovery
        MetricGrid(
            rows = listOf(
                "Raw detected" to (recovery.rawDetectedPeaks?.toString() ?: graph.peaks.size.toString()),
                "Validated" to (recovery.validatedPeaks?.toString() ?: "not calculated"),
                "Production reportable" to (recovery.productionReportablePeaks?.toString() ?: "not calculated"),
                "Runtime recovered" to recovery.runtimeRecoveredPeaks.size.toString(),
                "Test-only recovered" to recovery.testOnlyRecoveredPeaks.size.toString(),
                "Rejected candidates" to recovery.rejectedRecoveredCandidates.size.toString(),
            ),
        )
        val candidates = recovery.runtimeRecoveredPeaks +
            recovery.testOnlyRecoveredPeaks +
            recovery.rejectedRecoveredCandidates
        if (candidates.isEmpty()) {
            EmptyText("No recovered peak candidates.")
            return@SectionBlock
        }
        candidates.take(6).forEach { candidate ->
            DetailLine(
                label = candidate.sourceEvidence?.rawText ?: candidate.labelRt.formatReportNumber(),
                value = listOfNotNull(
                    candidate.status.name,
                    candidate.nearestLocalMaximumRt?.let { "max ${it.formatReportNumber()}" },
                    candidate.localSNR?.let { "S/N ${it.formatReportNumber()}" },
                    candidate.rejectionReason,
                ).joinToString(" / "),
            )
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
private fun QualitySummary(
    report: ChromatogramReport,
    qualityState: QualityState,
    warnings: List<ReportWarning>,
    uiContract: ChromatogramReportUiContract,
) {
    ReportSection(title = "Quality summary") {
        QualityNotice(
            title = qualityState.label,
            message = qualityState.message,
            severity = qualityState.severity,
        )

        MetricGrid(
            rows = listOf(
                "Report gate" to uiContract.reportGateStatus.name,
                "Critical issues" to warnings.countCritical().toString(),
                "Review items" to warnings.countReviewItems().toString(),
                "Graph checks" to report.graphs.count { it.graphPreviewSeverity().rank() >= ReportSeverity.WARNING.rank() }.toString(),
                "Peak checks" to warnings.count { it.peakNumber != null }.toString(),
            ),
        )

        GateEvidenceSummary(uiContract)

        report.userQualityItems(warnings).forEach { item ->
            QualityNotice(
                title = item.title,
                message = item.message,
                severity = item.severity,
            )
        }

        Text(
            text = "Detailed warning codes, stages, and value provenance are kept in the technical appendix.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GateEvidenceSummary(uiContract: ChromatogramReportUiContract) {
    SectionBlock(title = "Release evidence gates") {
        val rows = uiContract.reportGateEvidence.rows()
        MetricGrid(
            rows = rows.take(8).map { row -> row.label to row.status.name },
        )
        val reasons = uiContract.reportGateBlockingReasons + uiContract.reportGateReviewReasons
        if (reasons.isNotEmpty()) {
            Text(
                text = reasons.take(4).joinToString("; "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TechnicalAppendix(
    report: ChromatogramReport,
    warnings: List<ReportWarning>,
    uiContract: ChromatogramReportUiContract,
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

        SectionBlock(title = "Geometry evidence") {
            val traces = report.graphs.mapNotNull { graph ->
                graph.source.geometryTrace?.let { graph.graphIndex to it }
            }
            if (traces.isEmpty()) {
                EmptyText("No geometry trace recorded.")
            } else {
                traces.forEach { (graphIndex, trace) ->
                    DetailLine(
                        label = "Graph $graphIndex status",
                        value = report.graphs
                            .firstOrNull { it.graphIndex == graphIndex }
                            ?.source
                            ?.geometryReportStatus
                            .renderGeometryStatus(),
                    )
                    DetailLine(
                        label = "Selected panel",
                        value = trace.selectedGraphPanelBounds?.region.renderGraphRegion(),
                    )
                    DetailLine(
                        label = "Selected plot",
                        value = trace.selectedPlotAreaBounds?.region.renderGraphRegion(),
                    )
                    DetailLine(
                        label = "Ticks",
                        value = "x=${trace.tickGeometry?.xTicks?.size ?: 0}, y=${trace.tickGeometry?.yTicks?.size ?: 0}",
                    )
                    DetailLine(
                        label = "Calibration",
                        value = "x=${trace.xCalibrationFit.renderCalibrationFitStatus()}, y=${trace.yCalibrationFit.renderCalibrationFitStatus()}",
                    )
                    DetailLine(
                        label = "Evidence artifacts",
                        value = trace.geometryArtifactCount().toString(),
                    )
                }
            }
        }

        SectionBlock(title = "Warning codes") {
            CompactWarningList(warnings, maxItems = 20, includeCode = true)
        }

        SectionBlock(title = "Export manifest") {
            uiContract.exportArtifacts.forEach { artifact ->
                DetailLine(
                    label = artifact.label,
                    value = "${artifact.artifactPath} / ${artifact.privacyClass.name} / ${if (artifact.userFacing) "user-facing" else "technical"}",
                )
            }
        }
    }
}

@Composable
private fun VisualEvidenceStrip(visualEvidence: List<ReportVisualEvidenceContract>) {
    if (visualEvidence.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visualEvidence.forEach { evidence ->
            EvidenceChip(evidence)
        }
    }
}

@Composable
private fun EvidenceChip(evidence: ReportVisualEvidenceContract) {
    val ready = evidence.generatedStatus == "rendered" || evidence.generatedStatus == "generated"
    Surface(
        modifier = Modifier.semantics {
            contentDescription = "${evidence.label}, ${evidence.generatedStatus}"
        },
        shape = RoundedCornerShape(6.dp),
        color = if (ready) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.70f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.70f)
        },
        contentColor = if (ready) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Text(
            text = evidence.label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
            modifier = Modifier.semantics { heading() },
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
private fun QualityNotice(
    title: String,
    message: String,
    severity: ReportSeverity,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        SeverityBadge(
            label = severity.qualityLabel(),
            severity = severity,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
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
    uiContract: ChromatogramReportUiContract,
    validation: ReportContractValidationResult,
    warnings: List<ReportWarning>,
): QualityState {
    val failed = warnings.count { it.severity == ReportSeverity.FAILED }
    val serious = warnings.count { it.severity == ReportSeverity.SERIOUS }
    return when {
        uiContract.reportGateStatus == ReportGateStatus.RELEASE_READY -> QualityState(
            label = "Release ready",
            message = "All required graph, calibration, trace, peak, provenance, and validator gates passed.",
            severity = ReportSeverity.INFO,
        )
        validation.errorCount > 0 || failed > 0 || uiContract.reportGateStatus == ReportGateStatus.BLOCKED -> QualityState(
            label = "Blocked",
            message = "The report has failed validation or failed analysis stages. Treat it as not release-ready.",
            severity = ReportSeverity.FAILED,
        )
        serious > 0 || uiContract.reportGateStatus == ReportGateStatus.DIAGNOSTIC_ONLY -> QualityState(
            label = "Review",
            message = "Required release evidence is missing or invalid. Use this report as diagnostic evidence only.",
            severity = ReportSeverity.SERIOUS,
        )
        validation.warningCount > 0 ||
            warnings.any { it.severity == ReportSeverity.WARNING } ||
            uiContract.reportGateStatus == ReportGateStatus.REVIEW_ONLY -> QualityState(
            label = "Caution",
            message = "The report is structured, but one or more evidence gates require review before release use.",
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

private fun GraphReportUiContract.visualEvidenceFor(sectionId: String): List<ReportVisualEvidenceContract> =
    visualEvidence.filter { it.nearSectionId == sectionId && it.placement == ReportUiPlacement.MAIN_REPORT }

private data class GateEvidenceRow(
    val label: String,
    val status: EvidenceGateStatus,
)

private fun GateEvidence.rows(): List<GateEvidenceRow> =
    listOf(
        GateEvidenceRow("Graph panel", graphPanelStatus),
        GateEvidenceRow("Plot area", plotAreaStatus),
        GateEvidenceRow("Axis geometry", axisStatus),
        GateEvidenceRow("Tick labels", tickStatus),
        GateEvidenceRow("X calibration", xCalibrationStatus),
        GateEvidenceRow("Y calibration", yCalibrationStatus),
        GateEvidenceRow("Trace", traceStatus),
        GateEvidenceRow("Peak evidence", peakReviewStatus),
        GateEvidenceRow("Evidence package", evidencePackageStatus),
        GateEvidenceRow("Source provenance", sourceProvenanceStatus),
        GateEvidenceRow("User confirmation", userConfirmationStatus),
        GateEvidenceRow("VLM/OCR semantic evidence", vlmEvidenceStatus),
    )

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

private fun List<ReportWarning>.countCritical(): Int =
    count { it.severity == ReportSeverity.FAILED || it.severity == ReportSeverity.SERIOUS }

private fun List<ReportWarning>.countReviewItems(): Int =
    count { it.severity == ReportSeverity.WARNING }

private fun List<ReportWarning>.maxSeverity(): ReportSeverity =
    maxByOrNull { it.severity.rank() }?.severity ?: ReportSeverity.INFO

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

private fun ChromatogramReport.userQualityItems(warnings: List<ReportWarning>): List<QualitySummaryItem> {
    val criticalCount = warnings.countCritical()
    val reviewCount = warnings.countReviewItems()
    val graphReviewCount = graphs.count { it.graphPreviewSeverity().rank() >= ReportSeverity.WARNING.rank() }
    val peakReviewCount = warnings.count { it.peakNumber != null } + graphs.sumOf { graph ->
        graph.peaks.count { peak -> peak.confidence != null && peak.confidence < 0.50 }
    }

    return listOf(
        QualitySummaryItem(
            title = "Analysis integrity",
            message = when {
                criticalCount > 0 -> "Do not use final conclusions until the failed or serious analysis checks are resolved."
                reviewCount > 0 -> "The report can be read, but some analytical checks need review before conclusions are trusted."
                else -> "No blocking analysis checks are recorded."
            },
            severity = when {
                warnings.any { it.severity == ReportSeverity.FAILED } -> ReportSeverity.FAILED
                criticalCount > 0 -> ReportSeverity.SERIOUS
                reviewCount > 0 -> ReportSeverity.WARNING
                else -> ReportSeverity.INFO
            },
        ),
        QualitySummaryItem(
            title = "Graph preparation",
            message = if (graphReviewCount > 0) {
                "$graphReviewCount graph(s) need crop, axis, or preparation review before relying on exact values."
            } else {
                "Graph crop and axis preparation have no visible review flags."
            },
            severity = if (graphReviewCount > 0) ReportSeverity.WARNING else ReportSeverity.INFO,
        ),
        QualitySummaryItem(
            title = "Peak integration",
            message = if (peakReviewCount > 0) {
                "$peakReviewCount peak-level item(s) need boundary, baseline, or confidence review."
            } else {
                "No peak-level review items are recorded."
            },
            severity = if (peakReviewCount > 0) ReportSeverity.WARNING else ReportSeverity.INFO,
        ),
    )
}

private fun ReportMetadata.timingWindowLabel(): String =
    when {
        analysisStartedAtEpochMillis != null && analysisCompletedAtEpochMillis != null -> {
            val elapsed = (analysisCompletedAtEpochMillis - analysisStartedAtEpochMillis)
                .takeIf { it >= 0L }
                .renderDuration()
            "window: $elapsed"
        }
        analysisStartedAtEpochMillis != null -> "start recorded"
        analysisCompletedAtEpochMillis != null -> "completion recorded"
        else -> "timestamps not recorded"
    }

private fun ReportMetadata.stageTimingPreview(): String =
    if (stageTimings.isEmpty()) {
        "no stage timings"
    } else {
        stageTimings
            .take(2)
            .joinToString(" / ") { timing ->
                "${timing.stageName ?: timing.stageId}: ${timing.durationMillis.renderDuration()}"
            }
    }

private fun ModelExecutionInfo?.renderModelName(): String =
    this?.modelName?.takeIf { it.isNotBlank() }
        ?: this?.modelId?.takeIf { it.isNotBlank() }
        ?: "not recorded"

private fun GraphReport.graphPreviewTitle(): String =
    identification.chromatogramTitle.value?.takeIf { it.isNotBlank() }
        ?: identification.ionOrChannel.value?.takeIf { it.isNotBlank() }
        ?: identification.sampleName.value?.takeIf { it.isNotBlank() }
        ?: "Untitled graph"

private fun GraphReport.graphPreviewSeverity(): ReportSeverity {
    val allGraphWarnings = warnings + axisCalibration.warnings + peaks.flatMap { it.warnings }
    return allGraphWarnings.maxByOrNull { it.severity.rank() }?.severity
        ?: when {
            source.cropConfidence != null && source.cropConfidence < 0.70 -> ReportSeverity.SERIOUS
            source.cropConfidence != null && source.cropConfidence < 0.85 -> ReportSeverity.WARNING
            else -> ReportSeverity.INFO
        }
}

private fun GraphReport.graphPreviewQualityLabel(): String =
    when (graphPreviewSeverity()) {
        ReportSeverity.FAILED -> "Failed"
        ReportSeverity.SERIOUS -> "Review"
        ReportSeverity.WARNING -> "Check"
        ReportSeverity.INFO -> "Ready"
    }

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

private fun PixelRect?.renderRect(): String =
    this?.let { "${it.width}x${it.height} @ ${it.x},${it.y}" } ?: "not recorded"

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

private fun CompoundAssignment?.renderCompoundAssignment(): String {
    val compound = this ?: return "not assigned"
    val name = compound.probableName.renderText()
    val source = compound.probableName.source
    val status = compound.probableName.status
    val evidenceBasis = compound.assignmentBasis.orEmpty().lowercase()
    val explicitEvidence = listOf("library", "spectrum", "spectral", "reference", "retention index", "user")
        .any { it in evidenceBasis }
    return if (
        status == ReportValueStatus.INFERRED &&
        source in setOf(ReportValueSource.LOCAL_KNOWLEDGE, ReportValueSource.MODEL_SUGGESTED, ReportValueSource.VISION_MODEL) &&
        !explicitEvidence
    ) {
        "not assigned; candidate: $name"
    } else {
        name
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

private fun GeometryReportStatus?.renderGeometryStatus(): String =
    when (this) {
        GeometryReportStatus.SCIENTIFIC_READY -> "scientific-ready"
        GeometryReportStatus.REVIEW_READY -> "review-grade"
        GeometryReportStatus.DIAGNOSTIC_ONLY -> "diagnostic-only"
        null -> "not recorded"
    }

private fun AxisCalibrationFit?.renderCalibrationFitStatus(): String {
    val fit = this ?: return "not recorded"
    val residual = fit.rmsePx?.let { ", rmse=${it.formatReportNumber()}px" }.orEmpty()
    return when (fit.status) {
        CalibrationFitStatus.VALID -> "valid$residual"
        CalibrationFitStatus.REVIEW -> "review$residual"
        CalibrationFitStatus.INVALID -> "invalid$residual"
    }
}

private fun GraphRegion?.renderGraphRegion(): String {
    val region = this ?: return "not recorded"
    return "${region.x},${region.y} ${region.width}x${region.height}"
}

private fun GeometryTrace.geometryArtifactCount(): Int =
    listOfNotNull(
        originalImagePath,
        normalizedImagePath,
        rectifiedImagePath,
        selectedGraphPanelOverlayPath,
        selectedPlotAreaOverlayPath,
        axisOverlayPath,
        tickOverlayPath,
        peakLabelCropBoundsOverlayPath,
        peakLabelTextClassificationOverlayPath,
        calibrationFitOverlayPath,
        curveMaskRawPath,
        curveMaskCleanPath,
        curveTextSuppressionOverlayPath,
        curveRejectedComponentsPath,
        curveSelectedComponentPath,
        curveSkeletonPath,
        finalCenterlineOverlayPath,
    ).size + ocrCropPaths.size + peakLabelCropPaths.size

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

private fun ReportSeverity.qualityLabel(): String =
    when (this) {
        ReportSeverity.FAILED -> "Blocked"
        ReportSeverity.SERIOUS -> "Review"
        ReportSeverity.WARNING -> "Check"
        ReportSeverity.INFO -> "Ready"
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

private data class QualitySummaryItem(
    val title: String,
    val message: String,
    val severity: ReportSeverity,
)

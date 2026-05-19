package com.chromalab.feature.reports

import kotlin.math.abs

/**
 * Print-ready HTML renderer backed by the report UI contract.
 *
 * Markdown remains a portable text export, but it is not parsed to create the phone/export report
 * surface. This renderer follows [ChromatogramReportUiContract] section ordering directly.
 */
object ReportHtmlRenderer {

    fun render(
        report: ChromatogramReport,
        uiContract: ChromatogramReportUiContract = ChromatogramReportUiContractBuilder.build(report),
    ): String {
        val title = report.documentTitle()
        val graphsByIndex = report.graphs.associateBy { it.graphIndex }

        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"en\">")
            appendLine("<head>")
            appendLine("<meta charset=\"utf-8\">")
            appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
            appendLine("<title>${title.escapeHtml()}</title>")
            appendLine(STYLE)
            appendLine("</head>")
            appendLine("<body>")
            appendLine("<article class=\"report\" data-ui-schema=\"${uiContract.schemaVersion.escapeHtml()}\">")
            renderCover(report, title, uiContract)
            appendLine("<main class=\"content\">")
            renderPrimarySurface(report, uiContract)
            uiContract.graphs.forEach { graphContract ->
                graphsByIndex[graphContract.graphIndex]?.let { graph ->
                    renderGraphReport(graph, graphContract)
                }
            }
            renderTechnicalAppendix(report, uiContract)
            appendLine("</main>")
            appendLine("</article>")
            appendLine("</body>")
            appendLine("</html>")
        }
    }

    private fun StringBuilder.renderCover(
        report: ChromatogramReport,
        title: String,
        uiContract: ChromatogramReportUiContract,
    ) {
        val metadata = report.metadata
        appendLine("<header class=\"cover\">")
        appendLine("<div class=\"eyebrow\">ChromaLab offline analytical report</div>")
        appendLine("<h1>${title.escapeHtml()}</h1>")
        appendLine("<p class=\"cover-summary\">${report.summaryLine().escapeHtml()}</p>")
        appendLine("<div class=\"meta-grid\">")
        metaTile("Report ID", metadata.reportId)
        metaTile("Graphs", uiContract.graphCount.toString())
        metaTile("Source", metadata.sourceName ?: metadata.inputSourceType.name)
        metaTile("Runtime", metadata.executedRuntime.name)
        metaTile("Model", metadata.executedModel.renderModelLabel())
        metaTile("Duration", metadata.totalAnalysisDurationMillis.renderDuration())
        metaTile("Device", metadata.deviceName ?: "not recorded")
        metaTile("Mode", metadata.processingMode.name)
        appendLine("</div>")
        appendLine("</header>")
    }

    private fun StringBuilder.renderPrimarySurface(
        report: ChromatogramReport,
        uiContract: ChromatogramReportUiContract,
    ) {
        uiContract.primarySurface.sections.forEachIndexed { index, section ->
            when (section.sectionId) {
                "release_gate" -> {
                    appendLine("<section class=\"report-section\" data-section=\"release_gate\">")
                    appendLine("<h2>${index + 1}. ${section.title.escapeHtml()}</h2>")
                    renderReleaseGate(report, uiContract)
                    appendLine("</section>")
                }
                "overview" -> {
                    appendLine("<section class=\"report-section\" data-section=\"overview\">")
                    appendLine("<h2>${index + 1}. ${section.title.escapeHtml()}</h2>")
                    renderOverview(report)
                    appendLine("</section>")
                }
                "key_warnings" -> {
                    appendLine("<section class=\"report-section\" data-section=\"key_warnings\">")
                    appendLine("<h2>${index + 1}. ${section.title.escapeHtml()}</h2>")
                    renderPublicWarnings(report)
                    appendLine("</section>")
                }
                "graph_report_sequence" -> {
                    appendLine("<section class=\"report-section\" data-section=\"graph_report_sequence\">")
                    appendLine("<h2>${index + 1}. ${section.title.escapeHtml()}</h2>")
                    renderGraphSequence(report, uiContract)
                    appendLine("</section>")
                }
            }
        }
    }

    private fun StringBuilder.renderReleaseGate(
        report: ChromatogramReport,
        uiContract: ChromatogramReportUiContract,
    ) {
        renderTable(
            headers = listOf("Field", "Value"),
            rows = listOf(
                "Report gate" to uiContract.reportGateStatus.name,
                "Processing mode" to report.metadata.processingMode.name,
                "Release-quality claim" to if (uiContract.reportGateStatus == ReportGateStatus.RELEASE_READY) {
                    "allowed by current evidence gate"
                } else {
                    "blocked; use as review or diagnostic evidence only"
                },
            ).map { listOf(it.first, it.second) },
        )
    }

    private fun StringBuilder.renderGraphReport(
        graph: GraphReport,
        uiContract: GraphReportUiContract,
    ) {
        appendLine("<section class=\"graph-report\" data-graph=\"${graph.graphIndex}\">")
        appendLine("<h2>Graph ${graph.graphIndex}</h2>")
        uiContract.sections.forEach { section ->
            when (section.sectionId) {
                "identification" -> renderIdentification(graph, section)
                "source_and_graph_preparation" -> renderPreparation(graph, uiContract, section)
                "axis_calibration" -> renderAxisCalibration(graph, uiContract, section)
                "interactive_or_rendered_graph" -> renderGraphOverlay(graph, uiContract, section)
                "peak_table" -> renderPeakTable(graph, uiContract, section)
                "peak_label_evidence_and_recovery" -> renderPeakRecovery(graph, uiContract, section)
                "chromatographic_quality" -> renderQuality(graph, section)
                "kovats_index_analysis" -> renderKovats(graph, section)
                "distribution_and_chemical_interpretation" -> renderInterpretation(graph, section)
                "warnings_and_red_flags" -> renderGraphWarnings(graph, section)
            }
        }
        appendLine("</section>")
    }

    private fun StringBuilder.renderOverview(report: ChromatogramReport) {
        val metadata = report.metadata
        val firstGraph = report.graphs.firstOrNull()
        renderTable(
            headers = listOf("Field", "Value"),
            rows = listOf(
                "Report ID" to metadata.reportId,
                "Schema" to metadata.schemaVersion,
                "Source" to (metadata.sourceName ?: metadata.inputSourceType.name),
                "Graphs detected" to metadata.detectedGraphCount.toString(),
                "Chromatogram title" to firstGraph?.identification?.chromatogramTitle.renderTextValue(),
                "Analysis type" to firstGraph?.identification?.analysisType.renderTextValue(),
                "Mode" to firstGraph?.identification?.chromatogramMode.renderTextValue(),
                "Ion/channel" to firstGraph?.identification?.ionOrChannel.renderTextValue(),
                "Sample" to firstGraph?.identification?.sampleName.renderTextValue(),
                "Detected peaks" to (firstGraph?.quality?.totalDetectedPeaks?.toString() ?: "not calculated"),
                "Total analysis time" to metadata.totalAnalysisDurationMillis.renderDuration(),
                "Selected model" to metadata.selectedModel.renderModelLabel(),
                "Executed model" to metadata.executedModel.renderModelLabel(),
                "Executed runtime" to metadata.executedRuntime.name,
            ).map { listOf(it.first, it.second) },
        )
    }

    private fun StringBuilder.renderPublicWarnings(report: ChromatogramReport) {
        val warnings = report.publicWarnings()
        if (warnings.isEmpty()) {
            appendLine("<p class=\"muted\">No public warnings recorded. Raw warning codes are kept in the technical appendix.</p>")
            return
        }
        renderTable(
            headers = listOf("Severity", "Graph", "Peak", "Message"),
            rows = warnings.map { warning ->
                listOf(
                    warning.severity.name,
                    warning.graphIndex?.toString() ?: "",
                    warning.peakNumber?.let { "#$it" } ?: "",
                    warning.message,
                )
            },
        )
    }

    private fun StringBuilder.renderGraphSequence(
        report: ChromatogramReport,
        uiContract: ChromatogramReportUiContract,
    ) {
        val graphsByIndex = report.graphs.associateBy { it.graphIndex }
        renderTable(
            headers = listOf("Graph", "Title", "Ion/channel", "Peaks", "Status"),
            rows = uiContract.graphs.mapNotNull { graphContract ->
                val graph = graphsByIndex[graphContract.graphIndex] ?: return@mapNotNull null
                listOf(
                    graph.graphIndex.toString(),
                    graph.identification.chromatogramTitle.renderTextValue(),
                    graph.identification.ionOrChannel.renderTextValue(),
                    (graph.quality.totalDetectedPeaks ?: graph.peaks.size).toString(),
                    graphContract.sections.maxStatus(),
                )
            },
        )
    }

    private fun StringBuilder.renderIdentification(graph: GraphReport, section: ReportUiSectionContract) {
        appendGraphSubsection(section)
        renderTable(
            headers = listOf("Field", "Value"),
            rows = listOf(
                "Title" to graph.identification.chromatogramTitle.renderTextValue(),
                "Analysis" to graph.identification.analysisType.renderTextValue(),
                "Mode" to graph.identification.chromatogramMode.renderTextValue(),
                "Ion/channel" to graph.identification.ionOrChannel.renderTextValue(),
                "Ion range" to graph.identification.ionRange.renderTextValue(),
                "Sample" to graph.identification.sampleName.renderTextValue(),
                "Matrix" to graph.identification.matrix.renderTextValue(),
                "Target class" to graph.identification.targetCompoundClass.renderTextValue(),
            ).map { listOf(it.first, it.second) },
        )
        appendLine("</section>")
    }

    private fun StringBuilder.renderPreparation(
        graph: GraphReport,
        uiContract: GraphReportUiContract,
        section: ReportUiSectionContract,
    ) {
        appendGraphSubsection(section)
        renderVisualEvidence(uiContract.visualEvidenceFor(section.sectionId))
        renderTable(
            headers = listOf("Field", "Value"),
            rows = listOf(
                "Source bounds" to graph.source.sourceImageBounds.renderRect(),
                "Detected graph bounds" to graph.source.detectedGraphBounds.renderRect(),
                "Crop confidence" to graph.source.cropConfidence.renderPercent(),
                "Scan mode" to (graph.source.scanMode ?: "not recorded"),
                "Title OCR" to graph.source.titleOcrConfidence.renderPercent(),
                "Axis OCR" to graph.source.axisOcrConfidence.renderPercent(),
                "Tick OCR" to graph.source.tickOcrConfidence.renderPercent(),
                "Manual review" to if (graph.source.manuallyAdjusted) "yes" else "no",
                "Preprocessing" to graph.source.preprocessingSteps.joinToString("; ").ifBlank { "not recorded" },
            ).map { listOf(it.first, it.second) },
        )
        appendLine("</section>")
    }

    private fun StringBuilder.renderAxisCalibration(
        graph: GraphReport,
        uiContract: GraphReportUiContract,
        section: ReportUiSectionContract,
    ) {
        appendGraphSubsection(section)
        renderVisualEvidence(uiContract.visualEvidenceFor(section.sectionId))
        renderTable(
            headers = listOf("Axis", "Label", "Unit", "Min", "Max", "Major ticks"),
            rows = listOf(
                graph.axisCalibration.xAxis.axisRow("X"),
                graph.axisCalibration.yAxis.axisRow("Y"),
            ),
        )
        renderTable(
            headers = listOf("Field", "Value"),
            rows = listOf(
                listOf("Calibration confidence", graph.axisCalibration.calibrationConfidence.renderPercent()),
                listOf("Pixel transform", graph.axisCalibration.pixelToUnitTransform.renderTransform()),
            ),
        )
        appendLine("</section>")
    }

    private fun StringBuilder.renderGraphOverlay(
        graph: GraphReport,
        uiContract: GraphReportUiContract,
        section: ReportUiSectionContract,
    ) {
        appendGraphSubsection(section)
        renderVisualEvidence(uiContract.visualEvidenceFor(section.sectionId))
        appendLine("<figure class=\"chart-figure\" data-render-surface=\"graph_${graph.graphIndex}_curve_overlay\">")
        appendLine(graph.toInlineSvg())
        appendLine("<figcaption>Rendered from structured peak and axis metrics. Exact source image artifacts remain linked through the UI contract when available.</figcaption>")
        appendLine("</figure>")
        renderTable(
            headers = listOf("Field", "Value"),
            rows = listOf(
                listOf("Extracted signal points", graph.signal.pointCount?.toString() ?: "not calculated"),
                listOf("Corrected signal available", if (graph.signal.correctedSignalAvailable) "yes" else "no"),
                listOf("Baseline method", graph.signal.baselineMethod ?: "not calculated"),
                listOf("Peak markers", graph.peaks.size.toString()),
                listOf("Integration boundaries", if (graph.peaks.any { it.startRetentionTime.value != null && it.endRetentionTime.value != null }) "available" else "not calculated"),
            ),
        )
        appendLine("</section>")
    }

    private fun StringBuilder.renderPeakTable(
        graph: GraphReport,
        uiContract: GraphReportUiContract,
        section: ReportUiSectionContract,
    ) {
        appendGraphSubsection(section)
        renderVisualEvidence(uiContract.visualEvidenceFor(section.sectionId))
        if (graph.peaks.isEmpty()) {
            appendLine("<p class=\"muted\">No peaks available.</p>")
        } else {
            renderTable(
                headers = listOf("#", "RT", "Height", "Area", "Area %", "FWHM", "W_base", "S/N", "Asymmetry", "Compound", "C#", "Kovats", "Confidence", "Flags"),
                rows = graph.peaks.sortedBy { it.retentionTime.value ?: Double.MAX_VALUE }.map { peak ->
                    listOf(
                        peak.number.toString(),
                        peak.retentionTime.renderNumber(),
                        peak.heightAboveBaseline.renderNumber(),
                        peak.integratedArea.renderNumber(),
                        peak.areaPercent.renderNumber(),
                        peak.fwhm.renderNumber(),
                        peak.widthAtBase.renderNumber(),
                        peak.signalToNoise.renderNumber(),
                        peak.asymmetry.renderNumber(),
                        peak.compound?.probableName.renderTextValue(),
                        peak.compound?.carbonNumber.renderTextValue(),
                        peak.compound?.kovatsIndex.renderNumber(),
                        peak.confidence.renderPercent(),
                        peak.flags.joinToString("; "),
                    )
                },
            )
        }
        appendLine("</section>")
    }

    private fun StringBuilder.renderPeakRecovery(
        graph: GraphReport,
        uiContract: GraphReportUiContract,
        section: ReportUiSectionContract,
    ) {
        appendGraphSubsection(section)
        renderVisualEvidence(uiContract.visualEvidenceFor(section.sectionId))
        val recovery = graph.peakRecovery
        renderTable(
            headers = listOf("Metric", "Count"),
            rows = listOf(
                "Raw detected peaks" to (recovery.rawDetectedPeaks?.toString() ?: "not calculated"),
                "Validated peaks" to (recovery.validatedPeaks?.toString() ?: "not calculated"),
                "Production reportable peaks" to (recovery.productionReportablePeaks?.toString() ?: "not calculated"),
                "Runtime recovered review peaks" to recovery.runtimeRecoveredPeaks.size.toString(),
                "Test-only recovered peaks" to recovery.testOnlyRecoveredPeaks.size.toString(),
                "Rejected recovered candidates" to recovery.rejectedRecoveredCandidates.size.toString(),
            ).map { listOf(it.first, it.second) },
        )
        val candidates = recovery.runtimeRecoveredPeaks +
            recovery.testOnlyRecoveredPeaks +
            recovery.rejectedRecoveredCandidates
        if (candidates.isNotEmpty()) {
            renderTable(
                headers = listOf("Label", "Source", "Local max RT", "Height", "S/N", "Prominence", "Decision", "Flags", "Reason"),
                rows = candidates.map { candidate ->
                    listOf(
                        candidate.sourceEvidence?.rawText ?: candidate.labelRt.formatNumber(),
                        candidate.sourceEvidence?.source?.name ?: "UNKNOWN",
                        candidate.nearestLocalMaximumRt?.formatNumber() ?: "not calculated",
                        candidate.localHeight?.formatNumber() ?: "not calculated",
                        candidate.localSNR?.formatNumber() ?: "not calculated",
                        candidate.localProminence?.formatNumber() ?: "not calculated",
                        candidate.status.name,
                        candidate.flags.joinToString("; ") { it.name },
                        candidate.rejectionReason ?: "",
                    )
                },
            )
        } else if (recovery.labelEvidence.isEmpty()) {
            appendLine("<p class=\"muted\">No peak-label OCR/VLM recovery evidence recorded.</p>")
        }
        appendLine("</section>")
    }

    private fun StringBuilder.renderQuality(graph: GraphReport, section: ReportUiSectionContract) {
        appendGraphSubsection(section)
        renderTable(
            headers = listOf("Metric", "Value"),
            rows = listOf(
                "Total peaks" to (graph.quality.totalDetectedPeaks?.toString() ?: graph.peaks.size.toString()),
                "Significant peaks" to (graph.quality.significantPeakCount?.toString() ?: "not calculated"),
                "Mean S/N" to graph.quality.meanSnr.renderNumber(),
                "Median S/N" to graph.quality.medianSnr.renderNumber(),
                "Maximum height" to graph.quality.maximumPeakHeight.renderNumber(),
                "Dominant peak" to (graph.quality.dominantPeakNumber?.let { "#$it" } ?: "not calculated"),
                "Baseline quality" to graph.quality.baselineQuality.renderTextValue(),
                "Minimum Rs" to graph.quality.minimumResolution.renderNumber(),
                "Global area" to graph.quality.globalIntegratedArea.renderNumber(),
                "Area normalization" to graph.quality.areaNormalizationStatus.renderTextValue(),
            ).map { listOf(it.first, it.second) },
        )
        if (graph.quality.anomalies.isNotEmpty()) {
            renderTable(
                headers = listOf("Anomaly", "Peak", "Severity"),
                rows = graph.quality.anomalies.map { anomaly ->
                    listOf(anomaly.message, anomaly.peakNumber?.let { "#$it" } ?: "", anomaly.severity.name)
                },
            )
        }
        appendLine("</section>")
    }

    private fun StringBuilder.renderKovats(graph: GraphReport, section: ReportUiSectionContract) {
        appendGraphSubsection(section)
        renderTable(
            headers = listOf("Field", "Value"),
            rows = listOf(
                "Status" to graph.kovats.status.name,
                "Formula" to (graph.kovats.formula ?: "not calculated"),
                "Reference" to (graph.kovats.referenceSeries ?: "not supplied"),
                "Trend linearity R2" to graph.kovats.trendLinearityR2.renderNumber(),
            ).map { listOf(it.first, it.second) },
        )
        if (graph.kovats.results.isNotEmpty()) {
            renderTable(
                headers = listOf("Peak", "Compound", "C#", "RT", "Kovats", "Literature", "Delta", "Kind", "Confidence"),
                rows = graph.kovats.results.map { result ->
                    listOf(
                        "#${result.peakNumber}",
                        result.compoundName ?: "not calculated",
                        result.carbonNumber ?: "not calculated",
                        result.retentionTime?.formatNumber() ?: "not calculated",
                        result.calculatedIndex.renderNumber(),
                        result.literatureRange.renderRange(),
                        result.deltaFromLiterature.renderNumber(),
                        result.calculationKind.name,
                        result.confidence.renderPercent(),
                    )
                },
            )
        }
        appendLine("</section>")
    }

    private fun StringBuilder.renderInterpretation(graph: GraphReport, section: ReportUiSectionContract) {
        appendGraphSubsection(section)
        renderTable(
            headers = listOf("Field", "Value"),
            rows = listOf(
                "Likely compound class" to graph.interpretation.likelyCompoundClass.renderTextValue(),
                "Unresolved assignments" to graph.interpretation.unresolvedAssignments.size.toString(),
            ).map { listOf(it.first, it.second) },
        )
        if (graph.interpretation.distributionByCarbonNumber.isNotEmpty()) {
            renderTable(
                headers = listOf("Carbon bucket", "Area", "Area %", "Peaks"),
                rows = graph.interpretation.distributionByCarbonNumber.map { bucket ->
                    listOf(
                        bucket.label,
                        bucket.area.renderNumber(),
                        bucket.areaPercent.renderNumber(),
                        bucket.peakCount?.toString() ?: "not calculated",
                    )
                },
            )
        }
        (graph.interpretation.homologSeriesNotes + graph.interpretation.domainContextNotes).takeIf { it.isNotEmpty() }
            ?.let { notes ->
                appendLine("<ul>")
                notes.forEach { note -> appendLine("<li>${note.escapeHtml()}</li>") }
                appendLine("</ul>")
            }
        appendLine("</section>")
    }

    private fun StringBuilder.renderGraphWarnings(graph: GraphReport, section: ReportUiSectionContract) {
        appendGraphSubsection(section)
        val peakWarnings = graph.peaks.flatMap { peak ->
            peak.warnings.map { warning ->
                warning.copy(
                    graphIndex = warning.graphIndex ?: graph.graphIndex,
                    peakNumber = warning.peakNumber ?: peak.number,
                )
            }
        }
        val warnings = graph.warnings + graph.axisCalibration.warnings + peakWarnings
        if (warnings.isEmpty()) {
            appendLine("<p class=\"muted\">No graph warnings recorded.</p>")
        } else {
            renderTable(
                headers = listOf("Severity", "Peak", "Message"),
                rows = warnings.map { warning ->
                    listOf(warning.severity.name, warning.peakNumber?.let { "#$it" } ?: "", warning.message)
                },
            )
        }
        appendLine("</section>")
    }

    private fun StringBuilder.renderVisualEvidence(evidence: List<ReportVisualEvidenceContract>) {
        if (evidence.isEmpty()) return
        appendLine("<div class=\"evidence-strip\">")
        evidence.forEach { item ->
            val state = if (item.generatedStatus == "rendered" || item.generatedStatus == "generated") "ready" else "missing"
            appendLine("<span class=\"evidence $state\" data-evidence=\"${item.evidenceId.escapeHtml()}\">${item.label.escapeHtml()}</span>")
        }
        appendLine("</div>")
    }

    private fun StringBuilder.renderTechnicalAppendix(
        report: ChromatogramReport,
        uiContract: ChromatogramReportUiContract,
    ) {
        appendLine("<details class=\"appendix\">")
        appendLine("<summary>Technical appendix</summary>")
        appendLine("<section data-section=\"runtime_and_model_trace\">")
        appendLine("<h3>Runtime and model trace</h3>")
        renderTable(
            headers = listOf("Field", "Value"),
            rows = listOf(
                "Selected model" to report.metadata.selectedModel.renderModelLabel(),
                "Selected runtime" to (report.metadata.selectedModel?.runtime?.name ?: "not recorded"),
                "Executed model" to report.metadata.executedModel.renderModelLabel(),
                "Executed runtime" to report.metadata.executedRuntime.name,
                "Device" to (report.metadata.deviceName ?: "not recorded"),
                "Mode" to report.metadata.processingMode.name,
            ).map { listOf(it.first, it.second) },
        )
        appendLine("</section>")
        appendLine("<section data-section=\"stage_timeline\">")
        appendLine("<h3>Stage timeline</h3>")
        if (report.metadata.stageTimings.isEmpty()) {
            appendLine("<p class=\"muted\">No stage timings recorded.</p>")
        } else {
            renderTable(
                headers = listOf("Stage", "Duration"),
                rows = report.metadata.stageTimings.map { timing ->
                    listOf(timing.stageName ?: timing.stageId, timing.durationMillis.renderDuration())
                },
            )
        }
        appendLine("</section>")
        appendLine("<section data-section=\"raw_warning_codes\">")
        appendLine("<h3>Raw warning codes</h3>")
        renderRawWarningCodes(report)
        appendLine("</section>")
        appendLine("<section data-section=\"value_provenance\">")
        appendLine("<h3>Value provenance</h3>")
        renderValueProvenance(report)
        appendLine("</section>")
        appendLine("<section data-section=\"export_manifest\">")
        appendLine("<h3>Export manifest</h3>")
        renderTable(
            headers = listOf("Artifact", "Purpose", "Visibility"),
            rows = uiContract.exportArtifacts.map { artifact ->
                listOf(
                    artifact.artifactPath,
                    artifact.purpose,
                    if (artifact.userFacing) "user-facing" else "technical",
                )
            },
        )
        appendLine("</section>")
        appendLine("</details>")
    }

    private fun StringBuilder.renderRawWarningCodes(report: ChromatogramReport) {
        val warnings = report.allWarningsWithCodes()
        if (warnings.isEmpty()) {
            appendLine("<p class=\"muted\">No warning codes recorded.</p>")
            return
        }
        renderTable(
            headers = listOf("Severity", "Code", "Stage", "Graph", "Peak", "Message"),
            rows = warnings.map { warning ->
                listOf(
                    warning.severity.name,
                    warning.code,
                    warning.stage ?: "",
                    warning.graphIndex?.toString() ?: "",
                    warning.peakNumber?.let { "#$it" } ?: "",
                    warning.message,
                )
            },
        )
    }

    private fun StringBuilder.renderValueProvenance(report: ChromatogramReport) {
        val rows = report.valueProvenanceRows()
        if (rows.isEmpty()) {
            appendLine("<p class=\"muted\">No value provenance recorded.</p>")
            return
        }
        renderTable(
            headers = listOf("Graph", "Field", "Status", "Source", "Confidence"),
            rows = rows.map { row ->
                listOf(
                    row.graphIndex?.toString() ?: "",
                    row.field,
                    row.status.name,
                    row.source.name,
                    row.confidence.renderPercent(),
                )
            },
        )
    }

    private fun StringBuilder.appendGraphSubsection(section: ReportUiSectionContract) {
        appendLine("<section class=\"graph-subsection\" data-section=\"${section.sectionId.escapeHtml()}\">")
        appendLine("<h3>${section.title.escapeHtml()}</h3>")
        section.contractStatus?.let {
            appendLine("<div class=\"section-status\">Status: ${it.escapeHtml()}</div>")
        }
    }

    private fun StringBuilder.renderTable(headers: List<String>, rows: List<List<String>>) {
        appendLine("<div class=\"table-wrap\">")
        appendLine("<table>")
        appendLine("<thead><tr>")
        headers.forEach { header -> appendLine("<th>${header.escapeHtml()}</th>") }
        appendLine("</tr></thead>")
        appendLine("<tbody>")
        rows.forEach { row ->
            appendLine("<tr>")
            row.forEach { cell -> appendLine("<td>${cell.escapeHtml()}</td>") }
            appendLine("</tr>")
        }
        appendLine("</tbody>")
        appendLine("</table>")
        appendLine("</div>")
    }

    private fun StringBuilder.metaTile(label: String, value: String) {
        appendLine("<section class=\"meta-tile\">")
        appendLine("<span>${label.escapeHtml()}</span>")
        appendLine("<strong>${value.escapeHtml()}</strong>")
        appendLine("</section>")
    }

    private fun GraphReport.toInlineSvg(): String {
        val sortedPeaks = peaks.sortedBy { it.retentionTime.value ?: Double.MAX_VALUE }
        if (sortedPeaks.isEmpty()) {
            return "<div class=\"chart-empty\">No graph overlay data available.</div>"
        }

        val minX = axisCalibration.xAxis.visibleMinimum.value ?: sortedPeaks.firstNotNullOfOrNull { it.startRetentionTime.value } ?: 0.0
        val maxX = axisCalibration.xAxis.visibleMaximum.value
            ?: sortedPeaks.mapNotNull { it.endRetentionTime.value }.lastOrNull()
            ?: (minX + 1.0)
        val maxY = axisCalibration.yAxis.visibleMaximum.value
            ?: sortedPeaks.maxOfOrNull { it.absoluteApexIntensity.value ?: it.heightAboveBaseline.value ?: 0.0 }
            ?: 1.0
        val width = 760.0
        val height = 260.0
        val left = 54.0
        val top = 18.0
        val chartWidth = width - left - 18.0
        val chartHeight = height - top - 38.0

        fun sx(value: Double): Double = left + ((value - minX) / (maxX - minX).takeIf { it > 0.0 }.orDefault(1.0)) * chartWidth
        fun sy(value: Double): Double = top + chartHeight - (value / maxY.takeIf { it > 0.0 }.orDefault(1.0)) * chartHeight

        val baselineY = sy(0.0)
        val path = sortedPeaks.joinToString(" ") { peak ->
            val x = sx(peak.retentionTime.value ?: peak.startRetentionTime.value ?: minX)
            val y = sy(peak.absoluteApexIntensity.value ?: peak.heightAboveBaseline.value ?: 0.0)
            "M ${x.formatSvg()} ${baselineY.formatSvg()} L ${x.formatSvg()} ${y.formatSvg()}"
        }
        val labels = sortedPeaks.take(24).joinToString("\n") { peak ->
            val x = sx(peak.retentionTime.value ?: peak.startRetentionTime.value ?: minX)
            val y = sy(peak.absoluteApexIntensity.value ?: peak.heightAboveBaseline.value ?: 0.0)
            """<text x="${x.formatSvg()}" y="${(y - 6.0).formatSvg()}" text-anchor="middle">#${peak.number}</text>"""
        }

        return """
            <svg class="chromatogram-svg" viewBox="0 0 ${width.formatSvg()} ${height.formatSvg()}" role="img" aria-label="Rendered chromatogram peak overlay">
              <line class="axis" x1="${left.formatSvg()}" y1="${baselineY.formatSvg()}" x2="${(left + chartWidth).formatSvg()}" y2="${baselineY.formatSvg()}" />
              <line class="axis" x1="${left.formatSvg()}" y1="${top.formatSvg()}" x2="${left.formatSvg()}" y2="${baselineY.formatSvg()}" />
              <path class="peak-lines" d="$path" />
              $labels
              <text class="axis-label" x="${(left + chartWidth / 2.0).formatSvg()}" y="${(height - 8.0).formatSvg()}" text-anchor="middle">${axisCalibration.xAxis.label.renderTextValue().escapeHtml()}</text>
              <text class="axis-label" x="12" y="${top.formatSvg()}" transform="rotate(-90 12 ${top.formatSvg()})">${axisCalibration.yAxis.label.renderTextValue().escapeHtml()}</text>
            </svg>
        """.trimIndent()
    }

    private fun List<ReportUiSectionContract>.maxStatus(): String =
        when {
            any { it.contractStatus == "FAILED" } -> "FAILED"
            any { it.contractStatus == "REVIEW" } -> "REVIEW"
            else -> "READY"
        }

    private fun GraphReportUiContract.visualEvidenceFor(sectionId: String): List<ReportVisualEvidenceContract> =
        visualEvidence.filter { it.nearSectionId == sectionId && it.placement == ReportUiPlacement.MAIN_REPORT }

    private fun ChromatogramReport.documentTitle(): String =
        graphs.asSequence()
            .mapNotNull { it.identification.chromatogramTitle.value?.takeIf { value -> value.isNotBlank() } }
            .firstOrNull()
            ?: "ChromaLab chromatogram report"

    private fun ChromatogramReport.summaryLine(): String {
        val peakCount = graphs.sumOf { graph -> graph.quality.totalDetectedPeaks ?: graph.peaks.size }
        return "${metadata.detectedGraphCount} graph(s), $peakCount peak(s), model ${metadata.executedModel.renderModelLabel()}, duration ${metadata.totalAnalysisDurationMillis.renderDuration()}"
    }

    private fun ChromatogramReport.publicWarnings(): List<ReportWarning> =
        allWarningsWithCodes().sortedWarnings()

    private fun ChromatogramReport.allWarningsWithCodes(): List<ReportWarning> =
        (warnings + graphs.flatMap { graph ->
            graph.warnings +
                graph.axisCalibration.warnings.map { warning ->
                    if (warning.graphIndex == null) warning.copy(graphIndex = graph.graphIndex) else warning
                } +
                graph.peaks.flatMap { peak ->
                    peak.warnings.map { warning ->
                        warning.copy(
                            graphIndex = warning.graphIndex ?: graph.graphIndex,
                            peakNumber = warning.peakNumber ?: peak.number,
                        )
                    }
                }
        }).distinctBy { "${it.code}|${it.graphIndex}|${it.peakNumber}|${it.message}" }

    private fun List<ReportWarning>.sortedWarnings(): List<ReportWarning> =
        sortedWith(
            compareByDescending<ReportWarning> { it.severity.rank() }
                .thenBy { it.graphIndex ?: Int.MAX_VALUE }
                .thenBy { it.peakNumber ?: Int.MAX_VALUE }
                .thenBy { it.code },
        )

    private fun ReportSeverity.rank(): Int =
        when (this) {
            ReportSeverity.FAILED -> 4
            ReportSeverity.SERIOUS -> 3
            ReportSeverity.WARNING -> 2
            ReportSeverity.INFO -> 1
        }

    private fun ChromatogramReport.valueProvenanceRows(): List<ValueProvenanceRow> =
        buildList {
            graphs.forEach { graph ->
                addText(graph.graphIndex, "identification.chromatogramTitle", graph.identification.chromatogramTitle)
                addText(graph.graphIndex, "identification.analysisType", graph.identification.analysisType)
                addText(graph.graphIndex, "identification.chromatogramMode", graph.identification.chromatogramMode)
                addText(graph.graphIndex, "identification.ionOrChannel", graph.identification.ionOrChannel)
                addText(graph.graphIndex, "identification.ionRange", graph.identification.ionRange)
                addText(graph.graphIndex, "identification.sampleName", graph.identification.sampleName)
                addText(graph.graphIndex, "identification.matrix", graph.identification.matrix)
                addText(graph.graphIndex, "identification.targetCompoundClass", graph.identification.targetCompoundClass)
                addDouble(graph.graphIndex, "signal.baselineMean", graph.signal.baselineMean)
                addDouble(graph.graphIndex, "signal.baselineDrift", graph.signal.baselineDrift)
                addDouble(graph.graphIndex, "signal.rmsNoise", graph.signal.rmsNoise)
                graph.peaks.forEach { peak ->
                    addDouble(graph.graphIndex, "peak[${peak.number}].retentionTime", peak.retentionTime)
                    peak.compound?.let { compound ->
                        addText(graph.graphIndex, "peak[${peak.number}].compound.probableName", compound.probableName)
                        addText(graph.graphIndex, "peak[${peak.number}].compound.carbonNumber", compound.carbonNumber)
                        addDouble(graph.graphIndex, "peak[${peak.number}].compound.kovatsIndex", compound.kovatsIndex)
                    }
                }
                addText(graph.graphIndex, "interpretation.likelyCompoundClass", graph.interpretation.likelyCompoundClass)
            }
        }.filter { it.shouldRender() }

    private fun MutableList<ValueProvenanceRow>.addText(
        graphIndex: Int,
        field: String,
        value: ReportTextValue,
    ) {
        add(ValueProvenanceRow(graphIndex, field, value.status, value.source, value.confidence))
    }

    private fun MutableList<ValueProvenanceRow>.addDouble(
        graphIndex: Int,
        field: String,
        value: ReportDoubleValue,
    ) {
        add(ValueProvenanceRow(graphIndex, field, value.status, value.source, value.confidence))
    }

    private fun AxisReport.axisRow(name: String): List<String> =
        listOf(
            name,
            label.renderTextValue(),
            unit.renderTextValue(),
            visibleMinimum.renderNumber(),
            visibleMaximum.renderNumber(),
            majorTicks.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.renderNumber() } ?: "not calculated",
        )

    private fun ReportTextValue?.renderTextValue(): String {
        if (this == null) return "not calculated"
        val text = value?.takeIf { it.isNotBlank() } ?: return status.renderMissing()
        return if (status.isUsable()) text else "${status.renderMissing()} ($text)"
    }

    private fun ReportDoubleValue?.renderNumber(): String {
        if (this == null) return "not calculated"
        val number = value ?: return status.renderMissing()
        val suffix = unit?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
        return "${number.formatNumber()}$suffix"
    }

    private fun DoubleRangeValue?.renderRange(): String =
        this?.let {
            val suffix = it.unit?.takeIf { unit -> unit.isNotBlank() }?.let { unit -> " $unit" } ?: ""
            "${it.minimum.formatNumber()}-${it.maximum.formatNumber()}$suffix"
        } ?: "not calculated"

    private fun PixelRect?.renderRect(): String =
        this?.let { "x=${it.x}, y=${it.y}, ${it.width}x${it.height}" } ?: "not recorded"

    private fun PixelToUnitTransform?.renderTransform(): String =
        this?.let {
            "${it.method}: x=${it.xScale.formatNumber()}*px+${it.xOffset.formatNumber()}, y=${it.yScale.formatNumber()}*px+${it.yOffset.formatNumber()}"
        } ?: "not calculated"

    private fun ModelExecutionInfo?.renderModelLabel(): String =
        this?.modelName?.takeIf { it.isNotBlank() }
            ?: this?.modelId?.takeIf { it.isNotBlank() }
            ?: "not recorded"

    private fun Double?.renderPercent(): String =
        this?.let { "${(it * 100.0).formatNumber()}%" } ?: "not recorded"

    private fun Long?.renderDuration(): String =
        when {
            this == null -> "not recorded"
            this < 1_000L -> "$this ms"
            this < 60_000L -> "${this / 1_000L}.${(this % 1_000L) / 100L} s"
            else -> "${this / 60_000L} min ${(this % 60_000L) / 1_000L} s"
        }

    private fun ReportValueStatus.isUsable(): Boolean =
        this == ReportValueStatus.CALCULATED ||
            this == ReportValueStatus.DETECTED ||
            this == ReportValueStatus.INFERRED

    private fun ReportValueStatus.renderMissing(): String =
        when (this) {
            ReportValueStatus.NOT_DETECTED -> "not detected"
            ReportValueStatus.INSUFFICIENT_CONFIDENCE -> "insufficient confidence"
            ReportValueStatus.FAILED -> "failed"
            ReportValueStatus.NOT_CALCULATED,
            ReportValueStatus.CALCULATED,
            ReportValueStatus.DETECTED,
            ReportValueStatus.INFERRED -> "not calculated"
        }

    private fun Double.formatNumber(): String =
        when {
            abs(this) >= 1000.0 -> "%.0f".format(this)
            abs(this) >= 10.0 -> "%.2f".format(this)
            else -> "%.4f".format(this)
        }

    private fun Double.formatSvg(): String = "%.2f".format(this)

    private fun Double?.orDefault(default: Double): Double = this ?: default

    private fun String.escapeHtml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    private data class ValueProvenanceRow(
        val graphIndex: Int?,
        val field: String,
        val status: ReportValueStatus,
        val source: ReportValueSource,
        val confidence: Double?,
    ) {
        fun shouldRender(): Boolean =
            status != ReportValueStatus.NOT_CALCULATED ||
                source != ReportValueSource.UNKNOWN ||
                confidence != null
    }

    private val STYLE = """
        <style>
          @page { size: A4; margin: 14mm; }
          :root {
            --page: #f6f8fb;
            --paper: #ffffff;
            --ink: #172033;
            --muted: #657084;
            --line: #d9e0ea;
            --soft: #eef3f8;
            --accent: #245b7d;
            --accent-soft: #e5f1f6;
            --good: #1b7f54;
            --warn: #8a5a0a;
          }
          * { box-sizing: border-box; }
          body {
            margin: 0;
            background: var(--page);
            color: var(--ink);
            font-family: Inter, "Segoe UI", Roboto, Arial, sans-serif;
            line-height: 1.5;
          }
          .report {
            width: min(1100px, calc(100% - 32px));
            margin: 24px auto;
            background: var(--paper);
            border: 1px solid var(--line);
            border-radius: 10px;
            box-shadow: 0 18px 50px rgba(27, 43, 65, 0.10);
            overflow: hidden;
          }
          .cover {
            padding: 32px 36px;
            border-bottom: 1px solid var(--line);
            background: linear-gradient(180deg, #ffffff 0%, #f7fbfd 100%);
          }
          .eyebrow {
            color: var(--accent);
            font-size: 12px;
            font-weight: 700;
            letter-spacing: 0;
            text-transform: uppercase;
          }
          h1, h2, h3 { letter-spacing: 0; line-height: 1.25; }
          h1 { margin: 8px 0 10px; font-size: 30px; }
          h2 {
            margin: 30px 0 12px;
            padding-bottom: 8px;
            border-bottom: 1px solid var(--line);
            color: var(--accent);
            font-size: 20px;
          }
          h3 { margin: 22px 0 10px; font-size: 16px; }
          .cover-summary, .muted, figcaption { color: var(--muted); }
          .meta-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            gap: 10px;
          }
          .meta-tile {
            min-height: 72px;
            padding: 10px 12px;
            border: 1px solid var(--line);
            border-radius: 8px;
            background: rgba(255, 255, 255, 0.78);
          }
          .meta-tile span {
            display: block;
            margin-bottom: 4px;
            color: var(--muted);
            font-size: 11px;
            font-weight: 700;
            text-transform: uppercase;
          }
          .meta-tile strong {
            display: block;
            font-size: 13px;
            overflow-wrap: anywhere;
          }
          .content { padding: 0 36px 36px; }
          .graph-report {
            margin-top: 24px;
            padding-top: 2px;
            border-top: 2px solid var(--accent-soft);
          }
          .graph-subsection { margin-bottom: 18px; }
          .section-status {
            display: inline-block;
            margin-bottom: 8px;
            padding: 3px 8px;
            border-radius: 6px;
            background: var(--accent-soft);
            color: var(--accent);
            font-size: 11px;
            font-weight: 700;
          }
          .evidence-strip {
            display: flex;
            flex-wrap: wrap;
            gap: 6px;
            margin: 8px 0 10px;
          }
          .evidence {
            display: inline-block;
            padding: 4px 8px;
            border-radius: 6px;
            border: 1px solid var(--line);
            font-size: 11px;
            font-weight: 700;
          }
          .evidence.ready {
            border-color: #c5dfd2;
            background: #eef8f2;
            color: var(--good);
          }
          .evidence.missing {
            background: #f8f5ef;
            color: var(--warn);
          }
          .table-wrap {
            width: 100%;
            margin: 10px 0 18px;
            overflow-x: auto;
            border: 1px solid var(--line);
            border-radius: 8px;
          }
          table {
            width: 100%;
            border-collapse: collapse;
            font-size: 13px;
          }
          th, td {
            padding: 8px 10px;
            border-bottom: 1px solid var(--line);
            text-align: left;
            vertical-align: top;
          }
          th {
            background: var(--soft);
            color: #344154;
            font-weight: 700;
          }
          tr:last-child td { border-bottom: 0; }
          tbody tr:nth-child(even) td { background: #fbfdff; }
          .chart-figure {
            margin: 8px 0 16px;
            padding: 12px;
            border: 1px solid var(--line);
            border-radius: 8px;
            background: #fbfdff;
          }
          .chromatogram-svg {
            display: block;
            width: 100%;
            height: auto;
          }
          .axis { stroke: #6b7280; stroke-width: 1.2; }
          .peak-lines {
            fill: none;
            stroke: #1f2937;
            stroke-width: 1.6;
            vector-effect: non-scaling-stroke;
          }
          .axis-label, .chromatogram-svg text {
            fill: #536174;
            font: 10px Inter, "Segoe UI", Arial, sans-serif;
          }
          .chart-empty {
            padding: 24px;
            color: var(--muted);
            text-align: center;
          }
          .appendix {
            margin-top: 30px;
            padding: 14px 16px;
            border: 1px solid var(--line);
            border-radius: 8px;
            background: #fbfdff;
          }
          .appendix summary {
            cursor: pointer;
            color: var(--accent);
            font-weight: 800;
          }
          ul { margin: 8px 0 14px; padding-left: 22px; }
          li { margin: 4px 0; }

          @media print {
            body { background: #ffffff; }
            .report {
              width: 100%;
              margin: 0;
              border: 0;
              border-radius: 0;
              box-shadow: none;
            }
            .cover { padding: 0 0 14px; background: #ffffff; }
            .content { padding: 0; }
            h1 { font-size: 22pt; }
            h2 { break-after: avoid; font-size: 15pt; }
            h3 { break-after: avoid; font-size: 12pt; }
            table { font-size: 8.5pt; }
            tr, .meta-tile, .chart-figure { break-inside: avoid; }
            .table-wrap { overflow: visible; border-radius: 0; }
          }
        </style>
    """.trimIndent()
}

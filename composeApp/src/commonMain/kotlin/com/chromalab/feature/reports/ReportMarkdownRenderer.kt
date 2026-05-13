package com.chromalab.feature.reports

/**
 * Reference Markdown renderer for the phase 1 report contract.
 *
 * The renderer is deliberately pure and conservative: it renders missing values explicitly instead
 * of filling them with assumptions. UI and PDF export can later reuse the same section ordering.
 */
object ReportMarkdownRenderer {

    fun render(report: ChromatogramReport): String = buildString {
        appendLine("# ChromaLab chromatogram report")
        appendLine()
        appendLine("## 1. Overview")
        renderOverview(report)
        appendLine()

        appendLine("## 2. Source and graph preparation")
        report.graphs.forEach { renderGraphPreparation(it, report.graphs.size > 1) }
        appendLine()

        appendLine("## 3. Axis calibration")
        report.graphs.forEach { renderAxisCalibration(it, report.graphs.size > 1) }
        appendLine()

        appendLine("## 4. Peak table")
        report.graphs.forEach { renderPeakTable(it, report.graphs.size > 1) }
        appendLine()

        appendLine("## 5. Interactive or rendered graph")
        report.graphs.forEach { renderGraphOverlay(it, report.graphs.size > 1) }
        appendLine()

        appendLine("## 6. Chromatographic quality")
        report.graphs.forEach { renderChromatographicQuality(it, report.graphs.size > 1) }
        appendLine()

        appendLine("## 7. Kovats index analysis")
        report.graphs.forEach { renderKovats(it, report.graphs.size > 1) }
        appendLine()

        appendLine("## 8. Distribution and chemical interpretation")
        report.graphs.forEach { renderInterpretation(it, report.graphs.size > 1) }
        appendLine()

        appendLine("## 9. Warnings and red flags")
        renderWarnings(report)
        appendLine()

        appendLine("## 10. Technical appendix")
        renderTechnicalAppendix(report)
    }

    private fun StringBuilder.renderOverview(report: ChromatogramReport) {
        val metadata = report.metadata
        val firstGraph = report.graphs.firstOrNull()

        appendLine("| Field | Value |")
        appendLine("| --- | --- |")
        row("Report ID", metadata.reportId)
        row("Schema", metadata.schemaVersion)
        row("Source", metadata.sourceName ?: metadata.inputSourceType.name)
        row("Graphs detected", metadata.detectedGraphCount.toString())
        row("Chromatogram title", firstGraph?.identification?.chromatogramTitle.render())
        row("Analysis type", firstGraph?.identification?.analysisType.render())
        row("Mode", firstGraph?.identification?.chromatogramMode.render())
        row("Ion/channel", firstGraph?.identification?.ionOrChannel.render())
        row("Sample", firstGraph?.identification?.sampleName.render())
        row("Sample path/label", firstGraph?.identification?.samplePathOrInstrumentLabel.render())
        row("Detected peaks", firstGraph?.quality?.totalDetectedPeaks?.toString() ?: notCalculated())
        row("Dominant peak", firstGraph?.quality?.dominantPeakNumber?.let { "#$it" } ?: notCalculated())
        row("Total analysis time", metadata.totalAnalysisDurationMillis?.let { "${it} ms" } ?: notCalculated())
        row("Selected model", metadata.selectedModel.render())
        row("Executed model", metadata.executedModel.render())
        row("Executed runtime", metadata.executedRuntime.name)
    }

    private fun StringBuilder.renderGraphPreparation(graph: GraphReport, showGraphHeader: Boolean) {
        graphHeader(graph, showGraphHeader)
        appendLine("| Field | Value |")
        appendLine("| --- | --- |")
        row("Source bounds", graph.source.sourceImageBounds.render())
        row("Detected graph bounds", graph.source.detectedGraphBounds.render())
        row("Crop confidence", graph.source.cropConfidence.renderPercent())
        row("Scan mode", graph.source.scanMode ?: notCalculated())
        row("Title OCR confidence", graph.source.titleOcrConfidence.renderPercent())
        row("Axis OCR confidence", graph.source.axisOcrConfidence.renderPercent())
        row("Tick OCR confidence", graph.source.tickOcrConfidence.renderPercent())
        row("Manual adjustment", if (graph.source.manuallyAdjusted) "yes" else "no")
        row(
            "Preprocessing",
            graph.source.preprocessingSteps.takeIf { it.isNotEmpty() }?.joinToString("; ") ?: notCalculated(),
        )
    }

    private fun StringBuilder.renderAxisCalibration(graph: GraphReport, showGraphHeader: Boolean) {
        graphHeader(graph, showGraphHeader)
        appendLine("| Axis | Label | Unit | Min | Max | Major ticks |")
        appendLine("| --- | --- | --- | --- | --- | --- |")
        axisRow("X", graph.axisCalibration.xAxis)
        axisRow("Y", graph.axisCalibration.yAxis)
        appendLine()
        appendLine("- Calibration confidence: ${graph.axisCalibration.calibrationConfidence.renderPercent()}")
        appendLine("- Pixel transform: ${graph.axisCalibration.pixelToUnitTransform.render()}")
        renderCalibrationCandidates(graph.axisCalibration.calibrationCandidates)
        renderAxisCalibrationWarnings(graph.axisCalibration.warnings)
    }

    private fun StringBuilder.renderPeakTable(graph: GraphReport, showGraphHeader: Boolean) {
        graphHeader(graph, showGraphHeader)
        if (graph.peaks.isEmpty()) {
            appendLine(notCalculated())
            return
        }

        appendLine("| # | RT | Height | Area | Area % | FWHM | W_base | S/N | Asymmetry | Compound | Formula | C# | Kovats | Confidence | Flags |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
        graph.peaks.sortedBy { it.retentionTime.value ?: Double.MAX_VALUE }.forEach { peak ->
            val compound = peak.compound
            appendLine(
                listOf(
                    peak.number.toString(),
                    peak.retentionTime.render(),
                    peak.heightAboveBaseline.render(),
                    peak.integratedArea.render(),
                    peak.areaPercent.render(),
                    peak.fwhm.render(),
                    peak.widthAtBase.render(),
                    peak.signalToNoise.render(),
                    peak.asymmetry.render(),
                    compound?.probableName.render(),
                    compound?.formula.render(),
                    compound?.carbonNumber.render(),
                    compound?.kovatsIndex.render(),
                    peak.confidence.renderPercent(),
                    peak.flags.takeIf { it.isNotEmpty() }?.joinToString("; ") ?: "",
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |") { it.escapeTable() },
            )
        }
    }

    private fun StringBuilder.renderGraphOverlay(graph: GraphReport, showGraphHeader: Boolean) {
        graphHeader(graph, showGraphHeader)
        appendLine("- Extracted signal points: ${graph.signal.pointCount?.toString() ?: notCalculated()}")
        appendLine("- Corrected signal available: ${if (graph.signal.correctedSignalAvailable) "yes" else "no"}")
        appendLine("- Baseline method: ${graph.signal.baselineMethod ?: notCalculated()}")
        appendLine("- Peak markers: ${graph.peaks.size}")
        appendLine("- Integration boundaries: ${if (graph.peaks.any { it.startRetentionTime.value != null && it.endRetentionTime.value != null }) "available" else notCalculated()}")
        appendLine("- Anomaly markers: ${graph.quality.anomalies.size}")
    }

    private fun StringBuilder.renderChromatographicQuality(graph: GraphReport, showGraphHeader: Boolean) {
        graphHeader(graph, showGraphHeader)
        appendLine("| Metric | Value |")
        appendLine("| --- | --- |")
        row("Total peaks", graph.quality.totalDetectedPeaks?.toString() ?: notCalculated())
        row("Significant peaks", graph.quality.significantPeakCount?.toString() ?: notCalculated())
        row("S/N threshold", graph.quality.significantPeakSnrThreshold?.formatDouble() ?: notCalculated())
        row("Mean S/N", graph.quality.meanSnr.render())
        row("Median S/N", graph.quality.medianSnr.render())
        row("Maximum height", graph.quality.maximumPeakHeight.render())
        row("Dominant peak", graph.quality.dominantPeakNumber?.let { "#$it" } ?: notCalculated())
        row("Baseline quality", graph.quality.baselineQuality.render())
        row("Average Rs", graph.quality.averageResolution.render())
        row("Minimum Rs", graph.quality.minimumResolution.render())
        row("Theoretical plates", graph.quality.theoreticalPlates.render())
        row("HETP", graph.quality.hetp.render())
        row("Global area", graph.quality.globalIntegratedArea.render())
        row("Area normalization", graph.quality.areaNormalizationStatus.render())

        if (graph.quality.anomalies.isNotEmpty()) {
            appendLine()
            appendLine("| Anomaly | Peak | Severity |")
            appendLine("| --- | --- | --- |")
            graph.quality.anomalies.forEach { anomaly ->
                appendLine("| ${anomaly.message.escapeTable()} | ${anomaly.peakNumber?.let { "#$it" } ?: ""} | ${anomaly.severity.name} |")
            }
        }
    }

    private fun StringBuilder.renderKovats(graph: GraphReport, showGraphHeader: Boolean) {
        graphHeader(graph, showGraphHeader)
        appendLine("- Status: ${graph.kovats.status.name}")
        appendLine("- Formula: ${graph.kovats.formula ?: notCalculated()}")
        appendLine("- Reference series: ${graph.kovats.referenceSeries ?: notCalculated()}")
        appendLine("- Trend linearity R2: ${graph.kovats.trendLinearityR2.render()}")

        if (graph.kovats.results.isNotEmpty()) {
            appendLine()
            appendLine("| Peak | Compound | C# | RT | Kovats | Literature | Delta | Kind | Confidence |")
            appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- |")
            graph.kovats.results.forEach { result ->
                appendLine(
                    listOf(
                        "#${result.peakNumber}",
                        result.compoundName ?: notCalculated(),
                        result.carbonNumber ?: notCalculated(),
                        result.retentionTime?.formatDouble() ?: notCalculated(),
                        result.calculatedIndex.render(),
                        result.literatureRange.render(),
                        result.deltaFromLiterature.render(),
                        result.calculationKind.name,
                        result.confidence.renderPercent(),
                    ).joinToString(prefix = "| ", separator = " | ", postfix = " |") { it.escapeTable() },
                )
            }
        }

        if (graph.kovats.missingDataNotes.isNotEmpty()) {
            appendLine()
            graph.kovats.missingDataNotes.forEach { appendLine("- ${it.escapeMarkdown()}") }
        }
    }

    private fun StringBuilder.renderInterpretation(graph: GraphReport, showGraphHeader: Boolean) {
        graphHeader(graph, showGraphHeader)
        appendLine("- Likely compound class: ${graph.interpretation.likelyCompoundClass.render()}")

        if (graph.interpretation.distributionByCarbonNumber.isNotEmpty()) {
            appendLine()
            appendLine("| Carbon bucket | Area | Area % | Peaks |")
            appendLine("| --- | --- | --- | --- |")
            graph.interpretation.distributionByCarbonNumber.forEach { bucket ->
                appendLine("| ${bucket.label.escapeTable()} | ${bucket.area.render()} | ${bucket.areaPercent.render()} | ${bucket.peakCount?.toString() ?: notCalculated()} |")
            }
        }

        renderList("Homolog series notes", graph.interpretation.homologSeriesNotes)
        renderList("Domain context notes", graph.interpretation.domainContextNotes)
        renderList("Unresolved assignments", graph.interpretation.unresolvedAssignments)
    }

    private fun StringBuilder.renderWarnings(report: ChromatogramReport) {
        val warnings = report.warnings + report.graphs.flatMap { graph ->
            graph.warnings + graph.axisCalibration.warnings.map { warning ->
                if (warning.graphIndex == null) warning.copy(graphIndex = graph.graphIndex) else warning
            }
        }
        if (warnings.isEmpty()) {
            appendLine("No warnings recorded.")
            return
        }

        appendLine("| Severity | Code | Stage | Graph | Peak | Message |")
        appendLine("| --- | --- | --- | --- | --- | --- |")
        warnings.forEach { warning ->
            appendLine(
                listOf(
                    warning.severity.name,
                    warning.code,
                    warning.stage ?: "",
                    warning.graphIndex?.toString() ?: "",
                    warning.peakNumber?.let { "#$it" } ?: "",
                    warning.message,
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |") { it.escapeTable() },
            )
        }
    }

    private fun StringBuilder.renderTechnicalAppendix(report: ChromatogramReport) {
        val metadata = report.metadata
        appendLine("| Field | Value |")
        appendLine("| --- | --- |")
        row("App version", metadata.appVersion ?: notCalculated())
        row("Schema version", metadata.schemaVersion)
        row("Started", metadata.analysisStartedAtEpochMillis?.toString() ?: notCalculated())
        row("Completed", metadata.analysisCompletedAtEpochMillis?.toString() ?: notCalculated())
        row("Duration", metadata.totalAnalysisDurationMillis?.let { "$it ms" } ?: notCalculated())
        row("Input source", metadata.inputSourceType.name)
        row("Device", metadata.deviceName ?: notCalculated())
        row("Processing mode", metadata.processingMode.name)
        row("Selected model", metadata.selectedModel.render())
        row("Executed model", metadata.executedModel.render())
        row("Executed runtime", metadata.executedRuntime.name)
        renderStageTimings(metadata.stageTimings)

        report.graphs.forEach { graph ->
            appendLine()
            appendLine("### Graph ${graph.graphIndex} calculation settings")
            appendLine("- Smoothing: ${graph.signal.smoothingMethod ?: notCalculated()}")
            appendLine("- Smoothing parameters: ${graph.signal.smoothingParameters.renderMap()}")
            appendLine("- Baseline: ${graph.signal.baselineMethod ?: notCalculated()}")
            appendLine("- Baseline parameters: ${graph.signal.baselineParameters.renderMap()}")
            appendLine("- Noise method: ${graph.signal.noiseMethod ?: notCalculated()}")
            appendLine("- Signal extraction confidence: ${graph.signal.signalExtractionConfidence.renderPercent()}")
        }
    }

    private fun StringBuilder.renderStageTimings(stageTimings: List<ReportStageTiming>) {
        appendLine()
        appendLine("### Stage timings")
        if (stageTimings.isEmpty()) {
            appendLine(notCalculated())
            return
        }

        appendLine("| Stage | Duration |")
        appendLine("| --- | --- |")
        stageTimings.forEach { timing ->
            val stage = timing.stageName?.takeIf { it.isNotBlank() } ?: timing.stageId
            appendLine("| ${stage.escapeTable()} | ${timing.durationMillis} ms |")
        }
    }

    private fun StringBuilder.renderList(title: String, values: List<String>) {
        appendLine()
        appendLine("$title:")
        if (values.isEmpty()) {
            appendLine("- ${notCalculated()}")
        } else {
            values.forEach { appendLine("- ${it.escapeMarkdown()}") }
        }
    }

    private fun StringBuilder.axisRow(name: String, axis: AxisReport) {
        appendLine(
            listOf(
                name,
                axis.label.render(),
                axis.unit.render(),
                axis.visibleMinimum.render(),
                axis.visibleMaximum.render(),
                axis.majorTicks.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.render() } ?: notCalculated(),
            ).joinToString(prefix = "| ", separator = " | ", postfix = " |") { it.escapeTable() },
        )
    }

    private fun StringBuilder.renderCalibrationCandidates(candidates: List<AxisCalibrationCandidate>) {
        if (candidates.isEmpty()) return

        appendLine()
        appendLine("Calibration candidates:")
        appendLine("| Candidate | Axis | Source | Status | Unit | Points | Confidence | Notes |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- |")
        candidates.forEach { candidate ->
            appendLine(
                listOf(
                    candidate.candidateId,
                    candidate.axis.name,
                    candidate.source.name,
                    candidate.status.name,
                    candidate.unit ?: notCalculated(),
                    candidate.points.renderCandidatePoints(candidate.unit),
                    candidate.confidence.renderPercent(),
                    candidate.rejectionReasons.takeIf { it.isNotEmpty() }?.joinToString("; ") ?: "",
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |") { it.escapeTable() },
            )
        }
    }

    private fun StringBuilder.renderAxisCalibrationWarnings(warnings: List<ReportWarning>) {
        if (warnings.isEmpty()) return

        appendLine()
        appendLine("Axis calibration warnings:")
        appendLine("| Severity | Code | Message |")
        appendLine("| --- | --- | --- |")
        warnings.forEach { warning ->
            appendLine(
                listOf(
                    warning.severity.name,
                    warning.code,
                    warning.message,
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |") { it.escapeTable() },
            )
        }
    }

    private fun List<AxisCalibrationCandidatePoint>.renderCandidatePoints(unit: String?): String {
        if (isEmpty()) return notCalculated()
        val suffix = unit?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
        return joinToString("; ") { point ->
            val pixel = point.pixel?.let { "@ ${it.formatDouble()} px" } ?: "@ pixel not localized"
            "${point.value.formatDouble()}$suffix $pixel"
        }
    }

    private fun StringBuilder.row(label: String, value: String?) {
        appendLine("| ${label.escapeTable()} | ${(value ?: notCalculated()).escapeTable()} |")
    }

    private fun StringBuilder.graphHeader(graph: GraphReport, showGraphHeader: Boolean) {
        if (showGraphHeader) {
            appendLine()
            appendLine("### Graph ${graph.graphIndex}")
        }
    }

    private fun ReportTextValue?.render(): String {
        if (this == null) return notCalculated()
        val text = value?.takeIf { it.isNotBlank() } ?: return status.renderMissing()
        return if (status == ReportValueStatus.CALCULATED ||
            status == ReportValueStatus.DETECTED ||
            status == ReportValueStatus.INFERRED
        ) {
            text
        } else {
            "${status.renderMissing()} ($text)"
        }
    }

    private fun ReportDoubleValue?.render(): String {
        if (this == null) return notCalculated()
        val number = value ?: return status.renderMissing()
        val suffix = unit?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
        return "${number.formatDouble()}$suffix"
    }

    private fun ReportValueStatus.renderMissing(): String = when (this) {
        ReportValueStatus.NOT_DETECTED -> "not detected"
        ReportValueStatus.INSUFFICIENT_CONFIDENCE -> "insufficient confidence"
        ReportValueStatus.FAILED -> "failed"
        ReportValueStatus.NOT_CALCULATED -> notCalculated()
        ReportValueStatus.CALCULATED,
        ReportValueStatus.DETECTED,
        ReportValueStatus.INFERRED -> notCalculated()
    }

    private fun ModelExecutionInfo?.render(): String {
        if (this == null) return notCalculated()
        val name = modelName ?: modelId
        val backend = backendLabel?.let { ", $it" } ?: ""
        return "$name (${runtime.name}$backend)"
    }

    private fun PixelRect?.render(): String =
        this?.let { "x=${it.x}, y=${it.y}, ${it.width}x${it.height}" } ?: notCalculated()

    private fun PixelToUnitTransform?.render(): String =
        this?.let { "${it.method}: x=${it.xScale.formatDouble()}*px+${it.xOffset.formatDouble()}, y=${it.yScale.formatDouble()}*px+${it.yOffset.formatDouble()}" }
            ?: notCalculated()

    private fun DoubleRangeValue?.render(): String =
        this?.let {
            val suffix = it.unit?.let { unit -> " $unit" } ?: ""
            "${it.minimum.formatDouble()}-${it.maximum.formatDouble()}$suffix"
        } ?: notCalculated()

    private fun Double?.renderPercent(): String =
        this?.let { "${(it * 100.0).formatDouble()}%" } ?: notCalculated()

    private fun Map<String, String>.renderMap(): String =
        takeIf { it.isNotEmpty() }?.entries?.joinToString("; ") { "${it.key}=${it.value}" } ?: notCalculated()

    private fun Double.formatDouble(): String =
        when {
            kotlin.math.abs(this) >= 1000.0 -> "%.0f".format(this)
            kotlin.math.abs(this) >= 10.0 -> "%.2f".format(this)
            else -> "%.4f".format(this)
        }

    private fun String.escapeTable(): String =
        escapeMarkdown().replace("|", "\\|")

    private fun String.escapeMarkdown(): String =
        replace("\n", " ").trim()

    private fun notCalculated(): String = "not calculated"
}

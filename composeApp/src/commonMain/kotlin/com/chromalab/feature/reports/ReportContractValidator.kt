package com.chromalab.feature.reports

import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryReportStatus

/**
 * Contract-level validation for the structured chromatogram report.
 *
 * This validator checks report completeness, not scientific correctness. Later phases should add
 * numeric fixture checks after graph extraction and deterministic calculations are validated.
 */
object ReportContractValidator {

    fun validate(report: ChromatogramReport): ReportContractValidationResult {
        val findings = mutableListOf<ReportContractFinding>()

        if (report.graphs.isEmpty()) {
            findings += error(
                code = "report.graphs.empty",
                section = "overview",
                message = "Report contains no graph reports.",
            )
        }

        if (report.metadata.detectedGraphCount != report.graphs.size) {
            findings += warning(
                code = "metadata.graph_count_mismatch",
                section = "overview",
                message = "Detected graph count does not match the number of graph reports.",
            )
        }

        if (report.metadata.totalAnalysisDurationMillis == null) {
            findings += warning(
                code = "metadata.duration_missing",
                section = "technical_appendix",
                message = "Total analysis duration is missing.",
            )
        }

        if (report.metadata.executedRuntime == ExecutedRuntime.UNKNOWN) {
            findings += warning(
                code = "metadata.executed_runtime_unknown",
                section = "technical_appendix",
                message = "Executed runtime is unknown.",
            )
        }

        val selectedRuntime = report.metadata.selectedModel?.runtime
        val executedRuntime = report.metadata.executedModel?.runtime ?: report.metadata.executedRuntime
        if (selectedRuntime != null &&
            selectedRuntime != ExecutedRuntime.UNKNOWN &&
            executedRuntime != ExecutedRuntime.UNKNOWN &&
            selectedRuntime != executedRuntime
        ) {
            findings += warning(
                code = "metadata.model_runtime_mismatch",
                section = "technical_appendix",
                message = "Selected model runtime differs from the executed runtime.",
            )
        }

        report.graphs.forEach { graph ->
            validateGraph(graph, findings)
        }
        validateKnowledgeCitations(report, findings)

        return ReportContractValidationResult(
            isComplete = findings.none { it.severity == ReportContractSeverity.ERROR },
            findings = findings,
        )
    }

    private fun validateKnowledgeCitations(
        report: ChromatogramReport,
        findings: MutableList<ReportContractFinding>,
    ) {
        report.knowledgeCitations.forEach { citation ->
            if (citation.generatedBy == ReportKnowledgeGeneratedBy.VLM_WITH_KNOWLEDGE &&
                citation.usedEntryIds.isEmpty()
            ) {
                findings += warning(
                    code = "knowledge.used_entry_ids_missing",
                    section = "technical_appendix",
                    message = "VLM-generated scientific/domain explanation is missing Knowledge Pack entry IDs.",
                )
            }
            if (citation.usedEntryIds.any { id ->
                    citation.usedEntryRecords.none { record -> record.entryId == id }
                }
            ) {
                findings += warning(
                    code = "knowledge.used_entry_record_missing",
                    section = "technical_appendix",
                    message = "Knowledge citation references entry IDs that are not represented as full citation records.",
                )
            }
            if (citation.unsupportedClaims.isNotEmpty()) {
                findings += warning(
                    code = "knowledge.unsupported_claims_present",
                    section = "technical_appendix",
                    message = "Knowledge/VLM explanation contains unsupported claims and must remain review-only or be omitted.",
                )
            }
            if (citation.rejectionReason != null) {
                findings += warning(
                    code = "knowledge.explanation_rejected",
                    section = "technical_appendix",
                    message = "Knowledge/VLM explanation was rejected: ${citation.rejectionReason}.",
                )
            }
            if (citation.attemptedNumericMetricUse) {
                findings += error(
                    code = "knowledge.numeric_metric_forbidden",
                    section = "technical_appendix",
                    message = "Knowledge Pack citations cannot create measured RT, height, area, FWHM, S/N, baseline, Kovats, calibration, or peak metrics.",
                )
            }
        }
    }

    private fun validateGraph(
        graph: GraphReport,
        findings: MutableList<ReportContractFinding>,
    ) {
        if (graph.source.detectedGraphBounds == null) {
            findings += warning(
                code = "graph.crop_bounds_missing",
                graphIndex = graph.graphIndex,
                section = "source_and_graph_preparation",
                message = "Detected graph bounds are missing.",
            )
        }

        if (graph.source.cropConfidence == null) {
            findings += warning(
                code = "graph.crop_confidence_missing",
                graphIndex = graph.graphIndex,
                section = "source_and_graph_preparation",
                message = "Crop confidence is missing.",
            )
        }

        requireText(
            value = graph.identification.analysisType,
            code = "identification.analysis_type_missing",
            section = "overview",
            graphIndex = graph.graphIndex,
            label = "Analysis type",
            findings = findings,
        )
        requireText(
            value = graph.identification.chromatogramMode,
            code = "identification.mode_missing",
            section = "overview",
            graphIndex = graph.graphIndex,
            label = "Chromatogram mode",
            findings = findings,
        )
        requireText(
            value = graph.identification.ionOrChannel,
            code = "identification.ion_missing",
            section = "overview",
            graphIndex = graph.graphIndex,
            label = "Ion or channel",
            findings = findings,
        )
        requireText(
            value = graph.identification.sampleName,
            code = "identification.sample_missing",
            section = "overview",
            graphIndex = graph.graphIndex,
            label = "Sample name",
            findings = findings,
        )

        validateAxis(graph.graphIndex, "x_axis", graph.axisCalibration.xAxis, findings)
        validateAxis(graph.graphIndex, "y_axis", graph.axisCalibration.yAxis, findings)

        if (graph.axisCalibration.calibrationConfidence == null) {
            findings += warning(
                code = "axis.calibration_confidence_missing",
                graphIndex = graph.graphIndex,
                section = "axis_calibration",
                message = "Axis calibration confidence is missing.",
            )
        }
        when (graph.source.geometryReportStatus) {
            GeometryReportStatus.DIAGNOSTIC_ONLY -> findings += error(
                code = "geometry.diagnostic_only",
                graphIndex = graph.graphIndex,
                section = "source_and_graph_preparation",
                message = "Geometry pipeline marked this graph diagnostic-only; release-quality peak table is blocked.",
            )
            GeometryReportStatus.REVIEW_READY -> findings += warning(
                code = "geometry.review_grade",
                graphIndex = graph.graphIndex,
                section = "source_and_graph_preparation",
                message = "Geometry pipeline marked numeric values as review-grade.",
            )
            GeometryReportStatus.SCIENTIFIC_READY,
            null -> Unit
        }
        if (graph.axisCalibration.xCalibrationFit?.status == CalibrationFitStatus.INVALID ||
            graph.axisCalibration.yCalibrationFit?.status == CalibrationFitStatus.INVALID
        ) {
            findings += error(
                code = "axis.calibration_fit_invalid",
                graphIndex = graph.graphIndex,
                section = "axis_calibration",
                message = "At least one axis calibration fit is invalid.",
            )
        }

        if (graph.signal.pointCount == null || graph.signal.pointCount <= 0) {
            findings += warning(
                code = "signal.point_count_missing",
                graphIndex = graph.graphIndex,
                section = "source_and_graph_preparation",
                message = "Extracted signal point count is missing.",
            )
        }

        requireDouble(
            value = graph.signal.baselineMean,
            code = "signal.baseline_mean_missing",
            section = "chromatographic_quality",
            graphIndex = graph.graphIndex,
            label = "Baseline mean",
            findings = findings,
        )
        requireDouble(
            value = graph.signal.rmsNoise,
            code = "signal.rms_noise_missing",
            section = "chromatographic_quality",
            graphIndex = graph.graphIndex,
            label = "RMS noise",
            findings = findings,
        )

        if (graph.peaks.isEmpty()) {
            findings += error(
                code = "peaks.empty",
                graphIndex = graph.graphIndex,
                section = "peak_table",
                message = "Peak table is empty.",
            )
        } else {
            graph.peaks.forEach { peak ->
                validatePeak(graph.graphIndex, peak, findings)
            }
        }

        if (graph.quality.totalDetectedPeaks == null) {
            findings += warning(
                code = "quality.total_peaks_missing",
                graphIndex = graph.graphIndex,
                section = "chromatographic_quality",
                message = "Total detected peak count is missing.",
            )
        }

        if (graph.quality.dominantPeakNumber == null) {
            findings += warning(
                code = "quality.dominant_peak_missing",
                graphIndex = graph.graphIndex,
                section = "chromatographic_quality",
                message = "Dominant peak is not identified.",
            )
        }

        if (graph.kovats.status == ReportValueStatus.NOT_CALCULATED &&
            graph.kovats.missingDataNotes.isEmpty()
        ) {
            findings += warning(
                code = "kovats.not_calculated_without_note",
                graphIndex = graph.graphIndex,
                section = "kovats_index_analysis",
                message = "Kovats analysis is not calculated and no missing-data note is provided.",
            )
        }
        if (graph.kovats.results.any { it.calculatedIndex.isAvailable() } &&
            graph.kovats.referenceRetentionTimes.isEmpty()
        ) {
            findings += error(
                code = "kovats.reference_series_missing_for_calculated_index",
                graphIndex = graph.graphIndex,
                section = "kovats_index_analysis",
                message = "Calculated Kovats/retention index values require explicit same-method reference retention times.",
            )
        }

        if (!graph.interpretation.likelyCompoundClass.isAvailable() &&
            graph.interpretation.homologSeriesNotes.isEmpty() &&
            graph.interpretation.domainContextNotes.isEmpty()
        ) {
            findings += warning(
                code = "interpretation.empty",
                graphIndex = graph.graphIndex,
                section = "distribution_and_chemical_interpretation",
                message = "Chemical interpretation is empty.",
            )
        }
    }

    private fun validateAxis(
        graphIndex: Int,
        axisName: String,
        axis: AxisReport,
        findings: MutableList<ReportContractFinding>,
    ) {
        requireText(
            value = axis.label,
            code = "axis.$axisName.label_missing",
            section = "axis_calibration",
            graphIndex = graphIndex,
            label = "$axisName label",
            findings = findings,
        )
        requireText(
            value = axis.unit,
            code = "axis.$axisName.unit_missing",
            section = "axis_calibration",
            graphIndex = graphIndex,
            label = "$axisName unit",
            findings = findings,
        )
        requireDouble(
            value = axis.visibleMinimum,
            code = "axis.$axisName.minimum_missing",
            section = "axis_calibration",
            graphIndex = graphIndex,
            label = "$axisName visible minimum",
            findings = findings,
        )
        requireDouble(
            value = axis.visibleMaximum,
            code = "axis.$axisName.maximum_missing",
            section = "axis_calibration",
            graphIndex = graphIndex,
            label = "$axisName visible maximum",
            findings = findings,
        )
        if (axis.majorTicks.isEmpty()) {
            findings += warning(
                code = "axis.$axisName.major_ticks_missing",
                graphIndex = graphIndex,
                section = "axis_calibration",
                message = "$axisName major ticks are missing.",
            )
        }
    }

    private fun validatePeak(
        graphIndex: Int,
        peak: ReportPeak,
        findings: MutableList<ReportContractFinding>,
    ) {
        requireDouble(peak.retentionTime, "peak.retention_time_missing", "peak_table", graphIndex, "RT", findings, peak.number)
        requireDouble(peak.heightAboveBaseline, "peak.height_missing", "peak_table", graphIndex, "Height", findings, peak.number)
        requireDouble(peak.integratedArea, "peak.area_missing", "peak_table", graphIndex, "Area", findings, peak.number)
        requireDouble(peak.areaPercent, "peak.area_percent_missing", "peak_table", graphIndex, "Area percent", findings, peak.number)
        requireDouble(peak.fwhm, "peak.fwhm_missing", "peak_table", graphIndex, "FWHM", findings, peak.number)
        requireDouble(peak.widthAtBase, "peak.width_base_missing", "peak_table", graphIndex, "Base width", findings, peak.number)
        requireDouble(peak.signalToNoise, "peak.snr_missing", "peak_table", graphIndex, "S/N", findings, peak.number)
        requireDouble(peak.asymmetry, "peak.asymmetry_missing", "peak_table", graphIndex, "Asymmetry", findings, peak.number)
        listOf(
            "RT" to peak.retentionTime,
            "Height" to peak.heightAboveBaseline,
            "Area" to peak.integratedArea,
            "FWHM" to peak.fwhm,
            "Base width" to peak.widthAtBase,
            "S/N" to peak.signalToNoise,
            "Baseline" to peak.baselineAtApex,
        ).forEach { (label, value) ->
            validateModelNotNumericSource(value, "peak.model_numeric_source_forbidden", "peak_table", graphIndex, label, findings, peak.number)
        }
        peak.compound?.let { compound ->
            validateModelNotNumericSource(compound.kovatsIndex, "peak.model_numeric_source_forbidden", "peak_table", graphIndex, "Kovats", findings, peak.number)
        }

        if (peak.confidence == null) {
            findings += warning(
                code = "peak.confidence_missing",
                graphIndex = graphIndex,
                peakNumber = peak.number,
                section = "peak_table",
                message = "Peak confidence is missing.",
            )
        }

        if (peak.compound == null) {
            findings += warning(
                code = "peak.compound_assignment_missing",
                graphIndex = graphIndex,
                peakNumber = peak.number,
                section = "peak_table",
                message = "Compound assignment block is missing.",
            )
        } else {
            validateCompoundAssignment(graphIndex, peak.number, peak.compound, findings)
        }
    }

    private fun validateCompoundAssignment(
        graphIndex: Int,
        peakNumber: Int,
        compound: CompoundAssignment,
        findings: MutableList<ReportContractFinding>,
    ) {
        val source = compound.probableName.source
        val status = compound.probableName.status
        val basis = compound.assignmentBasis.orEmpty().lowercase()
        val hasExplicitIdentityEvidence = listOf("library", "spectrum", "spectral", "reference", "retention index", "user")
            .any { it in basis }
        if (
            compound.probableName.isAvailable() &&
            status == ReportValueStatus.INFERRED &&
            source in setOf(ReportValueSource.LOCAL_KNOWLEDGE, ReportValueSource.MODEL_SUGGESTED, ReportValueSource.VISION_MODEL) &&
            !hasExplicitIdentityEvidence
        ) {
            findings += warning(
                code = "peak.compound_assignment_evidence_missing",
                graphIndex = graphIndex,
                peakNumber = peakNumber,
                section = "peak_table",
                message = "Compound name is a hypothesis from semantic knowledge/model context, not an identified compound.",
            )
        }
    }

    private fun requireText(
        value: ReportTextValue,
        code: String,
        section: String,
        graphIndex: Int,
        label: String,
        findings: MutableList<ReportContractFinding>,
    ) {
        if (!value.isAvailable()) {
            findings += warning(
                code = code,
                graphIndex = graphIndex,
                section = section,
                message = "$label is not available.",
            )
        }
    }

    private fun requireDouble(
        value: ReportDoubleValue,
        code: String,
        section: String,
        graphIndex: Int,
        label: String,
        findings: MutableList<ReportContractFinding>,
        peakNumber: Int? = null,
    ) {
        if (!value.isAvailable()) {
            findings += warning(
                code = code,
                graphIndex = graphIndex,
                peakNumber = peakNumber,
                section = section,
                message = "$label is not available.",
            )
        }
    }

    private fun validateModelNotNumericSource(
        value: ReportDoubleValue,
        code: String,
        section: String,
        graphIndex: Int,
        label: String,
        findings: MutableList<ReportContractFinding>,
        peakNumber: Int? = null,
    ) {
        if (!value.isAvailable()) return
        if (!VlmBoundaryPolicy.canUseValueSourceForNumericMetric(value.source)) {
            findings += error(
                code = code,
                graphIndex = graphIndex,
                peakNumber = peakNumber,
                section = section,
                message = "$label cannot use VLM, OCR, model-suggested, local-knowledge, or unknown output as numeric chromatographic evidence.",
            )
        }
    }

    private fun warning(
        code: String,
        section: String,
        message: String,
        graphIndex: Int? = null,
        peakNumber: Int? = null,
    ): ReportContractFinding = ReportContractFinding(
        code = code,
        severity = ReportContractSeverity.WARNING,
        section = section,
        message = message,
        graphIndex = graphIndex,
        peakNumber = peakNumber,
    )

    private fun error(
        code: String,
        section: String,
        message: String,
        graphIndex: Int? = null,
        peakNumber: Int? = null,
    ): ReportContractFinding = ReportContractFinding(
        code = code,
        severity = ReportContractSeverity.ERROR,
        section = section,
        message = message,
        graphIndex = graphIndex,
        peakNumber = peakNumber,
    )
}

data class ReportContractValidationResult(
    val isComplete: Boolean,
    val findings: List<ReportContractFinding>,
) {
    val warningCount: Int = findings.count { it.severity == ReportContractSeverity.WARNING }
    val errorCount: Int = findings.count { it.severity == ReportContractSeverity.ERROR }
}

data class ReportContractFinding(
    val code: String,
    val severity: ReportContractSeverity,
    val section: String,
    val message: String,
    val graphIndex: Int? = null,
    val peakNumber: Int? = null,
)

enum class ReportContractSeverity {
    WARNING,
    ERROR,
}

private fun ReportTextValue.isAvailable(): Boolean =
    status.isAvailable() && !value.isNullOrBlank()

private fun ReportDoubleValue.isAvailable(): Boolean =
    status.isAvailable() && value != null

private fun ReportValueStatus.isAvailable(): Boolean =
    this == ReportValueStatus.CALCULATED ||
        this == ReportValueStatus.DETECTED ||
        this == ReportValueStatus.INFERRED

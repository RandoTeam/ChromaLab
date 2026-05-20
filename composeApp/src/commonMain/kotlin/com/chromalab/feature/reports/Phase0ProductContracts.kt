package com.chromalab.feature.reports

import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import kotlinx.serialization.Serializable

@Serializable
enum class ReportGateStatus {
    RELEASE_READY,
    REVIEW_ONLY,
    DIAGNOSTIC_ONLY,
    BLOCKED,
}

@Serializable
enum class EvidenceGateStatus {
    VALID,
    USER_CONFIRMED,
    REVIEW,
    INVALID,
    MISSING,
    NOT_APPLICABLE,
}

@Serializable
enum class RuntimeTerminalState {
    PASS,
    REVIEW,
    FAIL,
    DIAGNOSTIC_ONLY,
    ROI_FAILURE,
    CALIBRATION_FAILURE,
    CURVE_FAILURE,
    OCR_FAILURE,
    VLM_TIMEOUT,
    FATAL_PIPELINE_ERROR,
}

@Serializable
enum class RuntimeFailureClass {
    IMAGE_DECODE_FAILURE,
    ORIENTATION_FAILURE,
    GRAPH_PANEL_FAILURE,
    MULTI_GRAPH_SPLIT_FAILURE,
    PLOT_AREA_FAILURE,
    AXIS_DETECTION_FAILURE,
    TICK_LOCALIZATION_FAILURE,
    OCR_TICK_FAILURE,
    CALIBRATION_FAILURE,
    TRACE_EXTRACTION_FAILURE,
    SPARSE_TRACE_REVIEW,
    PEAK_DETECTION_FAILURE,
    PEAK_EVIDENCE_FAILURE,
    KNOWLEDGE_GROUNDING_FAILURE,
    VLM_MODEL_UNAVAILABLE,
    VLM_TIMEOUT,
    VLM_UNSUPPORTED_CLAIM,
    REPORT_GATE_FAILURE,
    EXPORT_PRIVACY_FAILURE,
    PERFORMANCE_TIMEOUT,
    UNKNOWN_FAILURE,
}

@Serializable
enum class VlmEvidenceTaskType {
    OCR_CROP,
    TEXT_CLASSIFICATION,
    OVERLAY_JUDGE,
    WARNING_SUMMARY,
    GRAPH_HINT,
}

@Serializable
enum class NumericChromatographicMetric {
    RT,
    HEIGHT,
    AREA,
    FWHM,
    SIGNAL_TO_NOISE,
    BASELINE,
    KOVATS,
    EXACT_PIXEL_GEOMETRY,
}

@Serializable
data class GateEvidence(
    val graphPanelStatus: EvidenceGateStatus = EvidenceGateStatus.MISSING,
    val plotAreaStatus: EvidenceGateStatus = EvidenceGateStatus.MISSING,
    val axisStatus: EvidenceGateStatus = EvidenceGateStatus.MISSING,
    val tickStatus: EvidenceGateStatus = EvidenceGateStatus.MISSING,
    val xCalibrationStatus: EvidenceGateStatus = EvidenceGateStatus.MISSING,
    val yCalibrationStatus: EvidenceGateStatus = EvidenceGateStatus.MISSING,
    val traceStatus: EvidenceGateStatus = EvidenceGateStatus.MISSING,
    val peakReviewStatus: EvidenceGateStatus = EvidenceGateStatus.MISSING,
    val evidencePackageStatus: EvidenceGateStatus = EvidenceGateStatus.MISSING,
    val sourceProvenanceStatus: EvidenceGateStatus = EvidenceGateStatus.MISSING,
    val userConfirmationStatus: EvidenceGateStatus = EvidenceGateStatus.NOT_APPLICABLE,
    val vlmEvidenceStatus: EvidenceGateStatus = EvidenceGateStatus.NOT_APPLICABLE,
)

@Serializable
data class ReportGateEvaluation(
    val status: ReportGateStatus,
    val evidence: GateEvidence,
    val blockingReasons: List<String> = emptyList(),
    val reviewReasons: List<String> = emptyList(),
)

object ReportReleaseGateEvaluator {
    fun evaluate(
        report: ChromatogramReport,
        validation: ReportContractValidationResult = ReportContractValidator.validate(report),
        evidencePackageStatus: EvidenceGateStatus = EvidenceGateStatus.MISSING,
        userConfirmationStatus: EvidenceGateStatus = EvidenceGateStatus.NOT_APPLICABLE,
    ): ReportGateEvaluation {
        val autonomousProduction = report.metadata.processingMode == ProcessingMode.AUTONOMOUS_PRODUCTION
        val evidence = GateEvidence(
            graphPanelStatus = report.aggregateGraphGate { graph ->
                val trace = graph.source.geometryTrace
                when {
                    graph.source.manuallyAdjusted && !autonomousProduction -> EvidenceGateStatus.USER_CONFIRMED
                    trace?.selectedGraphPanelBounds != null || graph.source.detectedGraphBounds != null ->
                        graph.source.geometryReportStatus.toEvidenceGate()
                    else -> EvidenceGateStatus.MISSING
                }
            },
            plotAreaStatus = report.aggregateGraphGate { graph ->
                when {
                    graph.source.manuallyAdjusted && !autonomousProduction -> EvidenceGateStatus.USER_CONFIRMED
                    graph.source.geometryTrace?.selectedPlotAreaBounds != null ->
                        graph.source.geometryReportStatus.toEvidenceGate()
                    else -> EvidenceGateStatus.MISSING
                }
            },
            axisStatus = report.aggregateGraphGate { graph ->
                val axis = graph.source.geometryTrace?.axisGeometry
                when {
                    graph.source.manuallyAdjusted && !autonomousProduction -> EvidenceGateStatus.USER_CONFIRMED
                    axis?.xAxisLinePx != null && axis.yAxisLinePx != null -> EvidenceGateStatus.VALID
                    graph.axisCalibration.pixelToUnitTransform != null -> EvidenceGateStatus.REVIEW
                    else -> EvidenceGateStatus.MISSING
                }
            },
            tickStatus = report.aggregateGraphGate { graph ->
                val ticks = graph.source.geometryTrace?.tickGeometry
                when {
                    graph.source.manuallyAdjusted && !autonomousProduction -> EvidenceGateStatus.USER_CONFIRMED
                    ticks?.xTicks?.isNotEmpty() == true && ticks.yTicks.isNotEmpty() -> EvidenceGateStatus.VALID
                    graph.axisCalibration.xAxis.majorTicks.isNotEmpty() ||
                        graph.axisCalibration.yAxis.majorTicks.isNotEmpty() -> EvidenceGateStatus.REVIEW
                    else -> EvidenceGateStatus.MISSING
                }
            },
            xCalibrationStatus = report.aggregateGraphGate { graph ->
                graph.axisCalibration.xCalibrationFit?.status.toEvidenceGate(
                    graph.source.manuallyAdjusted && !autonomousProduction,
                )
            },
            yCalibrationStatus = report.aggregateGraphGate { graph ->
                graph.axisCalibration.yCalibrationFit?.status.toEvidenceGate(
                    graph.source.manuallyAdjusted && !autonomousProduction,
                )
            },
            traceStatus = report.aggregateGraphGate { graph ->
                val trace = graph.source.geometryTrace
                when {
                    graph.source.manuallyAdjusted && !autonomousProduction -> EvidenceGateStatus.USER_CONFIRMED
                    trace?.finalCenterlineOverlayPath != null ||
                        trace?.curveSelectedComponentPath != null ||
                        trace?.curveSkeletonPath != null -> EvidenceGateStatus.VALID
                    graph.signal.pointCount?.let { it > 0 } == true -> EvidenceGateStatus.REVIEW
                    else -> EvidenceGateStatus.MISSING
                }
            },
            peakReviewStatus = report.aggregateGraphGate { graph ->
                graph.toPeakEvidenceGate()
            },
            evidencePackageStatus = evidencePackageStatus,
            sourceProvenanceStatus = report.aggregateGraphGate { graph ->
                val trace = graph.source.geometryTrace
                val provenance = trace?.sourceProvenance
                when {
                    provenance?.originalImagePath?.isNotBlank() == true &&
                        provenance.normalizedImagePath.isNotBlank() -> EvidenceGateStatus.VALID
                    trace?.originalImagePath?.isNotBlank() == true &&
                        trace.normalizedImagePath?.isNotBlank() == true -> EvidenceGateStatus.VALID
                    else -> EvidenceGateStatus.MISSING
                }
            },
            userConfirmationStatus = userConfirmationStatus,
            vlmEvidenceStatus = report.vlmEvidenceStatus(),
        )
        val blocking = evidence.blockingReasons() + validation.findings
            .filter { it.severity == ReportContractSeverity.ERROR }
            .map { it.code }
        val review = evidence.reviewReasons() + validation.findings
            .filter { it.severity == ReportContractSeverity.WARNING }
            .map { it.code }
        val status = when {
            validation.findings.any {
                it.severity == ReportContractSeverity.ERROR && it.code == "report.graphs.empty"
            } -> ReportGateStatus.BLOCKED
            blocking.isNotEmpty() -> ReportGateStatus.DIAGNOSTIC_ONLY
            evidence.releaseRequiredStatuses().any { it == EvidenceGateStatus.REVIEW } || review.isNotEmpty() ->
                ReportGateStatus.REVIEW_ONLY
            else -> ReportGateStatus.RELEASE_READY
        }
        return ReportGateEvaluation(
            status = status,
            evidence = evidence,
            blockingReasons = blocking.distinct(),
            reviewReasons = review.distinct(),
        )
    }

    fun terminalStateFor(gateStatus: ReportGateStatus): RuntimeTerminalState =
        when (gateStatus) {
            ReportGateStatus.RELEASE_READY -> RuntimeTerminalState.PASS
            ReportGateStatus.REVIEW_ONLY -> RuntimeTerminalState.REVIEW
            ReportGateStatus.DIAGNOSTIC_ONLY -> RuntimeTerminalState.DIAGNOSTIC_ONLY
            ReportGateStatus.BLOCKED -> RuntimeTerminalState.FAIL
        }

    private fun ChromatogramReport.aggregateGraphGate(
        selector: (GraphReport) -> EvidenceGateStatus,
    ): EvidenceGateStatus =
        graphs.map(selector).aggregateGate()

    private fun List<EvidenceGateStatus>.aggregateGate(): EvidenceGateStatus =
        when {
            isEmpty() -> EvidenceGateStatus.MISSING
            any { it == EvidenceGateStatus.INVALID || it == EvidenceGateStatus.MISSING } -> first {
                it == EvidenceGateStatus.INVALID || it == EvidenceGateStatus.MISSING
            }
            any { it == EvidenceGateStatus.REVIEW } -> EvidenceGateStatus.REVIEW
            any { it == EvidenceGateStatus.USER_CONFIRMED } && all {
                it == EvidenceGateStatus.USER_CONFIRMED || it == EvidenceGateStatus.VALID
            } -> EvidenceGateStatus.USER_CONFIRMED
            all { it == EvidenceGateStatus.NOT_APPLICABLE } -> EvidenceGateStatus.NOT_APPLICABLE
            else -> EvidenceGateStatus.VALID
        }

    private fun GeometryReportStatus?.toEvidenceGate(): EvidenceGateStatus =
        when (this) {
            GeometryReportStatus.SCIENTIFIC_READY -> EvidenceGateStatus.VALID
            GeometryReportStatus.REVIEW_READY -> EvidenceGateStatus.REVIEW
            GeometryReportStatus.DIAGNOSTIC_ONLY -> EvidenceGateStatus.INVALID
            null -> EvidenceGateStatus.REVIEW
        }

    private fun CalibrationFitStatus?.toEvidenceGate(userConfirmed: Boolean): EvidenceGateStatus =
        when {
            userConfirmed -> EvidenceGateStatus.USER_CONFIRMED
            this == CalibrationFitStatus.VALID -> EvidenceGateStatus.VALID
            this == CalibrationFitStatus.REVIEW -> EvidenceGateStatus.REVIEW
            this == CalibrationFitStatus.INVALID -> EvidenceGateStatus.INVALID
            else -> EvidenceGateStatus.MISSING
        }

    private fun ChromatogramReport.vlmEvidenceStatus(): EvidenceGateStatus {
        val vlmWarnings = (warnings + graphs.flatMap { it.warnings })
            .filter { warning -> warning.stage?.contains("model") == true || warning.code.contains("model.") }
        return when {
            vlmWarnings.any { it.severity == ReportSeverity.FAILED } -> EvidenceGateStatus.INVALID
            vlmWarnings.any { it.severity == ReportSeverity.SERIOUS || it.severity == ReportSeverity.WARNING } ->
                EvidenceGateStatus.REVIEW
            else -> EvidenceGateStatus.NOT_APPLICABLE
        }
    }

    private fun GateEvidence.releaseRequiredStatuses(): List<EvidenceGateStatus> =
        listOf(
            graphPanelStatus,
            plotAreaStatus,
            xCalibrationStatus,
            yCalibrationStatus,
            traceStatus,
            peakReviewStatus,
            evidencePackageStatus,
            sourceProvenanceStatus,
        )

    private fun GateEvidence.blockingReasons(): List<String> =
        releaseRequiredStatuses()
            .mapIndexedNotNull { index, status ->
                val code = requiredGateNames[index]
                when (status) {
                    EvidenceGateStatus.INVALID -> "$code.invalid"
                    EvidenceGateStatus.MISSING -> "$code.missing"
                    else -> null
                }
            }

    private fun GateEvidence.reviewReasons(): List<String> =
        listOf(
            "graph_panel" to graphPanelStatus,
            "plot_area" to plotAreaStatus,
            "axis" to axisStatus,
            "ticks" to tickStatus,
            "x_calibration" to xCalibrationStatus,
            "y_calibration" to yCalibrationStatus,
            "trace" to traceStatus,
            "peak_review" to peakReviewStatus,
            "source_provenance" to sourceProvenanceStatus,
            "vlm_evidence" to vlmEvidenceStatus,
        ).mapNotNull { (name, status) ->
            if (status == EvidenceGateStatus.REVIEW) "$name.review" else null
        }

    private val requiredGateNames = listOf(
        "graph_panel",
        "plot_area",
        "x_calibration",
        "y_calibration",
        "trace",
        "peak_review",
        "evidence_package",
        "source_provenance",
    )
}

private fun GraphReport.toPeakEvidenceGate(): EvidenceGateStatus {
    val evidence = peakRecovery.peakEvidenceTable
    if (evidence.isNotEmpty()) {
        return when {
            evidence.any { it.gateStatus == PeakGateStatus.INVALID } -> EvidenceGateStatus.INVALID
            evidence.any { it.gateStatus == PeakGateStatus.REVIEW } -> EvidenceGateStatus.REVIEW
            evidence.all { it.gateStatus == PeakGateStatus.VALID } -> {
                if (evidence.any { it.status == PeakEvidenceStatus.USER_CONFIRMED || it.status == PeakEvidenceStatus.USER_EDITED }) {
                    EvidenceGateStatus.USER_CONFIRMED
                } else {
                    EvidenceGateStatus.VALID
                }
            }
            else -> EvidenceGateStatus.MISSING
        }
    }
    return when {
        peaks.isNotEmpty() &&
            source.geometryReportStatus == GeometryReportStatus.SCIENTIFIC_READY -> EvidenceGateStatus.REVIEW
        peaks.isNotEmpty() -> EvidenceGateStatus.REVIEW
        else -> EvidenceGateStatus.MISSING
    }
}

object VlmBoundaryPolicy {
    val allowedTaskTypes: Set<VlmEvidenceTaskType> = setOf(
        VlmEvidenceTaskType.OCR_CROP,
        VlmEvidenceTaskType.TEXT_CLASSIFICATION,
        VlmEvidenceTaskType.OVERLAY_JUDGE,
        VlmEvidenceTaskType.WARNING_SUMMARY,
        VlmEvidenceTaskType.GRAPH_HINT,
    )

    val forbiddenNumericMetrics: Set<NumericChromatographicMetric> =
        NumericChromatographicMetric.entries.toSet()

    fun isAllowedTask(taskType: VlmEvidenceTaskType): Boolean =
        taskType in allowedTaskTypes

    fun canPopulateNumericMetric(
        taskType: VlmEvidenceTaskType,
        metric: NumericChromatographicMetric,
    ): Boolean =
        isAllowedTask(taskType) && metric !in forbiddenNumericMetrics

    fun canUseValueSourceForNumericMetric(source: ReportValueSource): Boolean =
        source == ReportValueSource.DETERMINISTIC ||
            source == ReportValueSource.USER ||
            source == ReportValueSource.IMPORTED_FILE
}

package com.chromalab.feature.processing.debug

import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.peaks.PeakLabelEvidence
import com.chromalab.feature.processing.peaks.PeakLabelEvidenceSource
import com.chromalab.feature.processing.peaks.PeakLabelTextClassification
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidate
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidateFlag
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidateStatus
import com.chromalab.feature.reports.ExecutedRuntime
import com.chromalab.feature.reports.ReportPeak
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
enum class RuntimeEvidenceValidationVerdict {
    PASS,
    REVIEW,
    FAIL,
}

@Serializable
enum class RuntimeEvidenceValidationIssueSeverity {
    BLOCKING,
    WARNING,
}

@Serializable
data class RuntimeEvidenceValidationIssue(
    val code: String,
    val message: String,
    val severity: RuntimeEvidenceValidationIssueSeverity,
    val graphIndex: Int? = null,
    val candidateId: String? = null,
)

@Serializable
data class RuntimeEvidenceRecoveryCandidateRow(
    val graphIndex: Int,
    val sourceEvidenceId: String,
    val evidenceSource: String?,
    val labelRt: Double,
    val nearestLocalMaximumRt: Double? = null,
    val status: String,
    val localCropPath: String? = null,
    val localSignalWindow: String? = null,
    val flags: List<String> = emptyList(),
    val rejectionReason: String? = null,
    val productionReportable: Boolean = false,
)

@Serializable
data class RuntimeEvidenceGraphValidationSummary(
    val graphIndex: Int,
    val rawDetectedPeaks: Int? = null,
    val validatedPeaks: Int? = null,
    val runtimeRecoveredPeaks: Int = 0,
    val testOnlyRecoveredPeaks: Int = 0,
    val rejectedRecoveredCandidates: Int = 0,
    val productionReportablePeaks: Int? = null,
    val reviewGradePeaks: Int? = null,
    val calibrationStatuses: List<String> = emptyList(),
    val recoveryCandidates: List<RuntimeEvidenceRecoveryCandidateRow> = emptyList(),
)

@Serializable
data class RuntimeEvidenceValidationSummary(
    val schemaVersion: String = "runtime-evidence-validation-1.0",
    val packageSchemaVersion: String? = null,
    val reportId: String? = null,
    val verdict: RuntimeEvidenceValidationVerdict,
    val blockingIssues: List<RuntimeEvidenceValidationIssue> = emptyList(),
    val warnings: List<RuntimeEvidenceValidationIssue> = emptyList(),
    val graphSummaries: List<RuntimeEvidenceGraphValidationSummary> = emptyList(),
)

object RuntimeEvidencePackageValidator {
    private const val DUPLICATE_RT_TOLERANCE = 0.025

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun validateJson(
        content: String,
        fileExists: (String) -> Boolean = RuntimeEvidenceFileProbe::exists,
    ): RuntimeEvidenceValidationSummary =
        runCatching { json.decodeFromString<RuntimeEvidencePackage>(content) }
            .fold(
                onSuccess = { validate(it, fileExists) },
                onFailure = {
                    RuntimeEvidenceValidationSummary(
                        packageSchemaVersion = null,
                        reportId = null,
                        verdict = RuntimeEvidenceValidationVerdict.FAIL,
                        blockingIssues = listOf(
                            RuntimeEvidenceValidationIssue(
                                code = "package.json_decode_failed",
                                message = it.message ?: "Runtime evidence package JSON could not be decoded.",
                                severity = RuntimeEvidenceValidationIssueSeverity.BLOCKING,
                            ),
                        ),
                    )
                },
            )

    fun validate(
        evidencePackage: RuntimeEvidencePackage,
        fileExists: (String) -> Boolean = RuntimeEvidenceFileProbe::exists,
    ): RuntimeEvidenceValidationSummary {
        val issues = mutableListOf<RuntimeEvidenceValidationIssue>()
        val graphSummaries = evidencePackage.graphs.map { graphPackage ->
            validateGraph(evidencePackage, graphPackage, fileExists, issues)
        }
        validatePackageMetadata(evidencePackage, issues)
        val blocking = issues.filter { it.severity == RuntimeEvidenceValidationIssueSeverity.BLOCKING }
        val warnings = issues.filter { it.severity == RuntimeEvidenceValidationIssueSeverity.WARNING }
        return RuntimeEvidenceValidationSummary(
            packageSchemaVersion = evidencePackage.schemaVersion,
            reportId = evidencePackage.reportId,
            verdict = when {
                blocking.isNotEmpty() -> RuntimeEvidenceValidationVerdict.FAIL
                warnings.isNotEmpty() -> RuntimeEvidenceValidationVerdict.REVIEW
                else -> RuntimeEvidenceValidationVerdict.PASS
            },
            blockingIssues = blocking,
            warnings = warnings,
            graphSummaries = graphSummaries,
        )
    }

    fun exportJson(summary: RuntimeEvidenceValidationSummary): String = json.encodeToString(summary)

    fun renderMarkdown(summary: RuntimeEvidenceValidationSummary): String = buildString {
        appendLine("# Runtime Evidence Package Validation")
        appendLine()
        appendLine("- Verdict: `${summary.verdict}`")
        appendLine("- Package schema: `${summary.packageSchemaVersion ?: "missing"}`")
        appendLine("- Report id: `${summary.reportId ?: "missing"}`")
        appendLine("- Blocking issues: ${summary.blockingIssues.size}")
        appendLine("- Warnings: ${summary.warnings.size}")
        appendLine()
        appendIssueSection("Blocking Issues", summary.blockingIssues)
        appendIssueSection("Warnings", summary.warnings)
        appendLine("## Recovery Candidates")
        appendLine()
        appendLine("| Graph | Evidence | Source | Label RT | Local max RT | Status | Production | Flags | Rejection |")
        appendLine("| --- | --- | --- | ---: | ---: | --- | --- | --- | --- |")
        summary.graphSummaries.flatMap { it.recoveryCandidates }.forEach { row ->
            appendLine(
                "| ${row.graphIndex} | `${row.sourceEvidenceId}` | ${row.evidenceSource ?: "-"} | " +
                    "${row.labelRt.formatOrDash()} | ${row.nearestLocalMaximumRt.formatOrDash()} | " +
                    "${row.status} | ${row.productionReportable} | ${row.flags.joinToString(", ").ifBlank { "-" }} | " +
                    "${row.rejectionReason ?: "-"} |",
            )
        }
        if (summary.graphSummaries.all { it.recoveryCandidates.isEmpty() }) {
            appendLine("| - | - | - | - | - | - | - | - | - |")
        }
        appendLine()
        appendLine("## Graph Counts")
        appendLine()
        appendLine("| Graph | Raw | Validated | Runtime recovered | Test-only recovered | Rejected recovery | Production reportable | Review-grade | Calibration |")
        appendLine("| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |")
        summary.graphSummaries.forEach { graph ->
            appendLine(
                "| ${graph.graphIndex} | ${graph.rawDetectedPeaks ?: "-"} | ${graph.validatedPeaks ?: "-"} | " +
                    "${graph.runtimeRecoveredPeaks} | ${graph.testOnlyRecoveredPeaks} | " +
                    "${graph.rejectedRecoveredCandidates} | ${graph.productionReportablePeaks ?: "-"} | " +
                    "${graph.reviewGradePeaks ?: "-"} | ${graph.calibrationStatuses.joinToString(", ").ifBlank { "-" }} |",
            )
        }
    }

    private fun validatePackageMetadata(
        evidencePackage: RuntimeEvidencePackage,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ) {
        if (evidencePackage.reportId.isBlank()) {
            issues.block("package.report_id_missing", "Runtime evidence package report id is missing.")
        }
        if (evidencePackage.deviceName.isNullOrBlank()) {
            issues.block("package.device_metadata_missing", "Device/runtime metadata is missing deviceName.")
        }
        if (evidencePackage.executedRuntime.isBlank() || evidencePackage.executedRuntime == ExecutedRuntime.UNKNOWN.name) {
            issues.block("package.executed_runtime_missing", "Executed runtime is missing or UNKNOWN.")
        }
        if (evidencePackage.selectedModelId.isNullOrBlank() && evidencePackage.executedModelId.isNullOrBlank()) {
            issues.block("package.model_metadata_missing", "Model/runtime metadata is missing selectedModelId and executedModelId.")
        }
        if (evidencePackage.graphs.isEmpty()) {
            issues.block("package.graphs_missing", "Runtime evidence package contains no graph packages.")
        }
    }

    private fun validateGraph(
        evidencePackage: RuntimeEvidencePackage,
        graphPackage: RuntimeEvidenceGraphPackage,
        fileExists: (String) -> Boolean,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ): RuntimeEvidenceGraphValidationSummary {
        val graph = evidencePackage.reportContract.graphs.firstOrNull { it.graphIndex == graphPackage.graphIndex }
        if (graph == null) {
            issues.block(
                code = "graph.report_contract_missing",
                message = "Graph ${graphPackage.graphIndex} has package artifacts but no matching report contract graph.",
                graphIndex = graphPackage.graphIndex,
            )
        }
        val paths = graphPackage.artifactPaths
        issues.requirePath(paths.originalImagePath, "source.original_image_missing", "Original image is missing.", graphPackage.graphIndex, fileExists)
        issues.requirePath(paths.normalizedImagePath, "source.normalized_image_missing", "Normalized image is missing.", graphPackage.graphIndex, fileExists)
        issues.requirePath(paths.graphPanelOverlayPath, "geometry.graph_panel_overlay_missing", "Graph panel overlay is missing.", graphPackage.graphIndex, fileExists)
        issues.requirePath(paths.plotAreaOverlayPath, "geometry.plot_area_overlay_missing", "Plot area overlay is missing.", graphPackage.graphIndex, fileExists)
        issues.requirePath(paths.axisOverlayPath, "geometry.axis_overlay_missing", "Axis overlay is missing.", graphPackage.graphIndex, fileExists)
        issues.requirePath(paths.tickOverlayPath, "geometry.tick_overlay_missing", "Tick overlay is missing.", graphPackage.graphIndex, fileExists)
        issues.requirePath(paths.rawCurveMaskPath, "curve.raw_mask_missing", "Mask before text suppression is missing.", graphPackage.graphIndex, fileExists)
        issues.requirePath(paths.cleanCurveMaskPath, "curve.clean_mask_missing", "Mask after text suppression is missing.", graphPackage.graphIndex, fileExists)
        issues.requirePath(paths.selectedTraceOverlayPath, "curve.selected_trace_overlay_missing", "Selected trace overlay is missing.", graphPackage.graphIndex, fileExists)
        if (paths.textSuppressionOverlayPath != null) {
            issues.requirePath(paths.textSuppressionOverlayPath, "curve.text_suppression_overlay_missing", "Text suppression overlay is missing.", graphPackage.graphIndex, fileExists)
            if (graphPackage.suppressedTextBoxes.isEmpty()) {
                issues.block(
                    code = "curve.suppressed_text_boxes_missing",
                    message = "Text suppression overlay exists, but suppressed text boxes are not listed.",
                    graphIndex = graphPackage.graphIndex,
                )
            }
        }
        paths.calibrationAnchorsOverlayPath?.let {
            issues.requirePath(it, "geometry.calibration_overlay_missing", "Calibration anchors overlay path does not exist.", graphPackage.graphIndex, fileExists)
        }
        validateGeometry(graphPackage, graph?.source?.geometryReportStatus, issues)
        validateCalibration(graphPackage, graph, issues)
        validatePeakLabelEvidence(graphPackage, fileExists, issues)
        val recoveryRows = validateRecovery(graphPackage, graph?.peaks.orEmpty(), issues)
        validateReportCounts(graphPackage, issues)
        return RuntimeEvidenceGraphValidationSummary(
            graphIndex = graphPackage.graphIndex,
            rawDetectedPeaks = graphPackage.summaryCounts.rawDetectedPeaks,
            validatedPeaks = graphPackage.summaryCounts.validatedPeaks,
            runtimeRecoveredPeaks = graphPackage.summaryCounts.runtimeRecoveredPeaks,
            testOnlyRecoveredPeaks = graphPackage.summaryCounts.testOnlyRecoveredPeaks,
            rejectedRecoveredCandidates = graphPackage.summaryCounts.rejectedRecoveredCandidates,
            productionReportablePeaks = graphPackage.summaryCounts.productionReportablePeaks,
            reviewGradePeaks = graphPackage.summaryCounts.reviewGradePeaks,
            calibrationStatuses = listOfNotNull(
                graph?.axisCalibration?.xCalibrationFit?.status?.name,
                graph?.axisCalibration?.yCalibrationFit?.status?.name,
            ),
            recoveryCandidates = recoveryRows,
        )
    }

    private fun validateGeometry(
        graphPackage: RuntimeEvidenceGraphPackage,
        status: GeometryReportStatus?,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ) {
        val graphPanel = graphPackage.peakLabelEvidence.firstNotNullOfOrNull { it.linkedGraphPanelBounds }
        val plotArea = graphPackage.peakLabelEvidence.firstNotNullOfOrNull { it.linkedPlotAreaBounds }
        if (graphPanel != null && plotArea != null && !plotArea.isInside(graphPanel)) {
            issues.block(
                code = "geometry.plot_area_outside_graph_panel",
                message = "Plot area is not inside graph panel.",
                graphIndex = graphPackage.graphIndex,
            )
        }
        val titleInsidePlot = graphPackage.peakLabelEvidence.any { evidence ->
            evidence.textClassification == PeakLabelTextClassification.TITLE_OR_CHANNEL &&
                evidence.boundsForValidation()?.let { box ->
                    plotArea?.let { box.intersects(it) } == true
                } == true
        }
        if (titleInsidePlot && status != GeometryReportStatus.REVIEW_READY && status != GeometryReportStatus.DIAGNOSTIC_ONLY) {
            issues.block(
                code = "geometry.title_text_inside_scientific_plot_area",
                message = "Plot area contains title/channel text but geometry is not marked REVIEW/DIAGNOSTIC.",
                graphIndex = graphPackage.graphIndex,
            )
        } else if (titleInsidePlot) {
            issues.warn(
                code = "geometry.title_text_inside_review_plot_area",
                message = "Plot area contains title/channel text and is marked review/diagnostic.",
                graphIndex = graphPackage.graphIndex,
            )
        }
    }

    private fun validateCalibration(
        graphPackage: RuntimeEvidenceGraphPackage,
        graph: com.chromalab.feature.reports.GraphReport?,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ) {
        val xStatus = graph?.axisCalibration?.xCalibrationFit?.status
        val yStatus = graph?.axisCalibration?.yCalibrationFit?.status
        if (xStatus == null) {
            issues.block("calibration.x_status_missing", "X calibration status is missing.", graphPackage.graphIndex)
        } else if (xStatus == CalibrationFitStatus.INVALID) {
            issues.warn("calibration.x_invalid", "X calibration is INVALID.", graphPackage.graphIndex)
        }
        if (yStatus == null) {
            issues.block("calibration.y_status_missing", "Y calibration status is missing.", graphPackage.graphIndex)
        } else if (yStatus == CalibrationFitStatus.INVALID) {
            issues.warn("calibration.y_invalid", "Y calibration is INVALID.", graphPackage.graphIndex)
        }
    }

    private fun validatePeakLabelEvidence(
        graphPackage: RuntimeEvidenceGraphPackage,
        fileExists: (String) -> Boolean,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ) {
        graphPackage.peakLabelEvidence.forEachIndexed { index, evidence ->
            val id = "graph_${graphPackage.graphIndex}_evidence_$index"
            if (evidence.source == PeakLabelEvidenceSource.FIXTURE_HINT || !evidence.isRuntimeEvidence) {
                issues.block(
                    code = "ocr.fixture_hint_in_runtime_package",
                    message = "Runtime PeakLabelEvidence contains FIXTURE_HINT or non-runtime evidence.",
                    graphIndex = graphPackage.graphIndex,
                    candidateId = id,
                )
            }
            if (evidence.source !in runtimeSources) {
                issues.block(
                    code = "ocr.invalid_runtime_source",
                    message = "Runtime PeakLabelEvidence source must be ML_KIT, VLM, or BOTH.",
                    graphIndex = graphPackage.graphIndex,
                    candidateId = id,
                )
            }
            issues.requirePath(
                path = evidence.localCropPath,
                code = "ocr.local_crop_missing",
                message = "Runtime PeakLabelEvidence localCropPath is missing.",
                graphIndex = graphPackage.graphIndex,
                fileExists = fileExists,
                candidateId = id,
            )
            if (evidence.parsedRetentionTime == null && evidence.rejectionReason.isNullOrBlank()) {
                issues.block(
                    code = "ocr.no_parsed_rt_or_rejection_reason",
                    message = "PeakLabelEvidence needs parsedRetentionTime or rejectionReason.",
                    graphIndex = graphPackage.graphIndex,
                    candidateId = id,
                )
            }
            if (evidence.textClassification == PeakLabelTextClassification.UNKNOWN_TEXT && evidence.rejectionReason.isNullOrBlank()) {
                issues.warn(
                    code = "ocr.unknown_text_without_reason",
                    message = "PeakLabelEvidence text classification is UNKNOWN_TEXT without a rejection reason.",
                    graphIndex = graphPackage.graphIndex,
                    candidateId = id,
                )
            }
            if (evidence.source == PeakLabelEvidenceSource.VLM || evidence.source == PeakLabelEvidenceSource.BOTH) {
                if (evidence.rawText.isBlank()) {
                    issues.block(
                        code = "vlm.raw_text_missing",
                        message = "VLM fallback evidence has no raw text.",
                        graphIndex = graphPackage.graphIndex,
                        candidateId = id,
                    )
                }
                if (evidence.warnings.none { it.contains("vlm_text_only_no_peak_metrics") }) {
                    issues.warn(
                        code = "vlm.text_only_contract_not_marked",
                        message = "VLM evidence is not marked with the text-only/no-peak-metrics contract warning.",
                        graphIndex = graphPackage.graphIndex,
                        candidateId = id,
                    )
                }
            }
        }
    }

    private fun validateRecovery(
        graphPackage: RuntimeEvidenceGraphPackage,
        existingPeaks: List<ReportPeak>,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ): List<RuntimeEvidenceRecoveryCandidateRow> {
        val allCandidates = graphPackage.runtimeRecoveredPeaks +
            graphPackage.testOnlyRecoveredPeaks +
            graphPackage.rejectedRecoveredCandidates
        val existingPeakRts = existingPeaks.mapNotNull { it.retentionTime.value }
        return allCandidates.map { candidate ->
            val row = candidate.toValidationRow(graphPackage.graphIndex)
            if (candidate.sourceEvidenceId.isBlank()) {
                issues.block("recovery.source_evidence_id_missing", "RecoveredPeakCandidate sourceEvidenceId is missing.", graphPackage.graphIndex)
            }
            if (!candidate.labelRt.isFinite()) {
                issues.block("recovery.label_rt_invalid", "RecoveredPeakCandidate labelRt is invalid.", graphPackage.graphIndex, candidate.sourceEvidenceId)
            }
            if (candidate.status != RecoveredPeakCandidateStatus.REJECTED && candidate.integrationWindow == null) {
                issues.block("recovery.local_signal_window_missing", "Accepted/review recovered peak has no local signal window.", graphPackage.graphIndex, candidate.sourceEvidenceId)
            }
            if (candidate.nearestLocalMaximumRt == null && candidate.rejectionReason.isNullOrBlank()) {
                issues.block("recovery.no_local_maximum_or_rejection", "RecoveredPeakCandidate needs nearest local maximum or rejection reason.", graphPackage.graphIndex, candidate.sourceEvidenceId)
            }
            if (candidate.flags.isEmpty()) {
                issues.block("recovery.flags_missing", "RecoveredPeakCandidate flags are missing.", graphPackage.graphIndex, candidate.sourceEvidenceId)
            }
            if (candidate.sourceEvidence?.source == PeakLabelEvidenceSource.FIXTURE_HINT && candidate.isProductionReportable) {
                issues.block("recovery.fixture_hint_reportable", "FIXTURE_HINT candidate is production reportable.", graphPackage.graphIndex, candidate.sourceEvidenceId)
            }
            val candidateRt = candidate.nearestLocalMaximumRt ?: candidate.labelRt
            val duplicatesExisting = candidate.status != RecoveredPeakCandidateStatus.REJECTED &&
                existingPeakRts.any { abs(it - candidateRt) <= DUPLICATE_RT_TOLERANCE }
            if (duplicatesExisting && RecoveredPeakCandidateFlag.DUPLICATE_REJECTED !in candidate.flags) {
                issues.block("recovery.duplicate_existing_peak", "Recovered peak duplicates an existing validated peak.", graphPackage.graphIndex, candidate.sourceEvidenceId)
            }
            row
        }
    }

    private fun validateReportCounts(
        graphPackage: RuntimeEvidenceGraphPackage,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ) {
        if (graphPackage.summaryCounts.rawDetectedPeaks == null) {
            issues.block("report.raw_detected_count_missing", "rawDetectedPeaks count is missing.", graphPackage.graphIndex)
        }
        if (graphPackage.summaryCounts.validatedPeaks == null) {
            issues.block("report.validated_count_missing", "validatedPeaks count is missing.", graphPackage.graphIndex)
        }
        if (graphPackage.summaryCounts.productionReportablePeaks == null) {
            issues.block("report.production_reportable_count_missing", "productionReportablePeaks count is missing.", graphPackage.graphIndex)
        }
        if (graphPackage.summaryCounts.reviewGradePeaks == null) {
            issues.block("report.review_grade_count_missing", "reviewGradePeaks count is missing.", graphPackage.graphIndex)
        }
    }

    private val runtimeSources = setOf(
        PeakLabelEvidenceSource.ML_KIT,
        PeakLabelEvidenceSource.VLM,
        PeakLabelEvidenceSource.BOTH,
    )
}

private fun MutableList<RuntimeEvidenceValidationIssue>.requirePath(
    path: String?,
    code: String,
    message: String,
    graphIndex: Int?,
    fileExists: (String) -> Boolean,
    candidateId: String? = null,
) {
    if (path.isNullOrBlank()) {
        block(code, message, graphIndex, candidateId)
    } else if (!fileExists(path)) {
        block("$code.file_not_found", "$message Path does not exist: $path", graphIndex, candidateId)
    }
}

private fun MutableList<RuntimeEvidenceValidationIssue>.block(
    code: String,
    message: String,
    graphIndex: Int? = null,
    candidateId: String? = null,
) {
    add(RuntimeEvidenceValidationIssue(code, message, RuntimeEvidenceValidationIssueSeverity.BLOCKING, graphIndex, candidateId))
}

private fun MutableList<RuntimeEvidenceValidationIssue>.warn(
    code: String,
    message: String,
    graphIndex: Int? = null,
    candidateId: String? = null,
) {
    add(RuntimeEvidenceValidationIssue(code, message, RuntimeEvidenceValidationIssueSeverity.WARNING, graphIndex, candidateId))
}

private fun StringBuilder.appendIssueSection(
    title: String,
    issues: List<RuntimeEvidenceValidationIssue>,
) {
    appendLine("## $title")
    appendLine()
    if (issues.isEmpty()) {
        appendLine("None.")
        appendLine()
        return
    }
    issues.forEach { issue ->
        appendLine("- `${issue.code}`${issue.graphIndex?.let { " graph=$it" }.orEmpty()}: ${issue.message}")
    }
    appendLine()
}

private fun GraphRegion.isInside(parent: GraphRegion): Boolean =
    x >= parent.x && y >= parent.y && right <= parent.right && bottom <= parent.bottom

private fun GraphRegion.intersects(other: GraphRegion): Boolean =
    x < other.right && right > other.x && y < other.bottom && bottom > other.y

private fun PeakLabelEvidence.boundsForValidation(): GraphRegion? = labelBoxPx ?: cropBoundsPx

private fun RecoveredPeakCandidate.toValidationRow(graphIndex: Int): RuntimeEvidenceRecoveryCandidateRow =
    RuntimeEvidenceRecoveryCandidateRow(
        graphIndex = graphIndex,
        sourceEvidenceId = sourceEvidenceId,
        evidenceSource = sourceEvidence?.source?.name,
        labelRt = labelRt,
        nearestLocalMaximumRt = nearestLocalMaximumRt,
        status = status.name,
        localCropPath = sourceEvidence?.localCropPath,
        localSignalWindow = integrationWindow?.let { "${it.startRt}..${it.endRt}" },
        flags = flags.map { it.name },
        rejectionReason = rejectionReason,
        productionReportable = isProductionReportable,
    )

private fun Double?.formatOrDash(): String =
    this?.let { (it * 10_000.0).roundToInt().toDouble().div(10_000.0).toString() } ?: "-"

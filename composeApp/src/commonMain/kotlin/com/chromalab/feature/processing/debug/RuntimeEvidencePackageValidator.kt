package com.chromalab.feature.processing.debug

import com.chromalab.feature.knowledge.KnowledgeGroundedVlmOutput
import com.chromalab.feature.knowledge.KnowledgeUsePolicyValidator
import com.chromalab.feature.knowledge.KnowledgeUseValidationVerdict
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.multimodal.AutonomousStageJudgeResult
import com.chromalab.feature.processing.multimodal.ForbiddenVlmNumericField
import com.chromalab.feature.processing.multimodal.ModelRuntimeProfile
import com.chromalab.feature.processing.multimodal.StageJudgeSource
import com.chromalab.feature.processing.multimodal.StageJudgeTaskType
import com.chromalab.feature.processing.multimodal.StageJudgeVerdict
import com.chromalab.feature.processing.multimodal.StageRetryPolicy
import com.chromalab.feature.processing.multimodal.VlmOcrCropResult
import com.chromalab.feature.processing.model.ModelAvailabilityDiagnostic
import com.chromalab.feature.processing.model.ModelAvailabilityStatus
import com.chromalab.feature.processing.peaks.PeakLabelEvidence
import com.chromalab.feature.processing.peaks.PeakLabelEvidenceSource
import com.chromalab.feature.processing.peaks.PeakLabelTextClassification
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidate
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidateFlag
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidateStatus
import com.chromalab.feature.reports.ExecutedRuntime
import com.chromalab.feature.reports.PeakEvidence
import com.chromalab.feature.reports.PeakEvidenceStatus
import com.chromalab.feature.reports.PeakGateStatus
import com.chromalab.feature.reports.PeakMetricEvidenceStatus
import com.chromalab.feature.reports.ReportPeak
import com.chromalab.feature.reports.ReportGateStatus
import com.chromalab.feature.reports.RuntimeFailureClass
import com.chromalab.feature.reports.RuntimeTerminalState
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
data class RuntimeEvidencePeakRow(
    val graphIndex: Int,
    val evidenceId: String,
    val peakId: String,
    val peakNumber: Int,
    val status: String,
    val gateStatus: String,
    val rt: Double? = null,
    val apexPointIndex: Int? = null,
    val localMaximumEvidence: Boolean = false,
    val heightStatus: String,
    val areaStatus: String,
    val boundaryStatus: String,
    val reportable: Boolean,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class RuntimeEvidenceStageJudgeRow(
    val graphIndex: Int,
    val taskId: String,
    val taskType: String,
    val source: String,
    val verdict: String,
    val cropPath: String? = null,
    val overlayPath: String? = null,
    val runtimeProfileId: String? = null,
    val retryRecommendations: List<String> = emptyList(),
    val rejectedForbiddenFields: List<String> = emptyList(),
)

@Serializable
data class RuntimeEvidenceModelAvailabilityRow(
    val diagnosticId: String,
    val mode: String,
    val selectedModelId: String? = null,
    val executedModelId: String? = null,
    val expectedBackend: String? = null,
    val expectedPath: String? = null,
    val pathExists: Boolean? = null,
    val fileSizeBytes: Long? = null,
    val loadAttempted: Boolean,
    val loadResult: String? = null,
    val fallbackModelAttempted: Boolean = false,
    val fallbackResult: String? = null,
    val status: String,
)

@Serializable
data class RuntimeEvidenceKnowledgeRow(
    val graphIndex: Int,
    val outputId: String,
    val taskId: String,
    val usedEntryIds: List<String>,
    val decision: String,
    val unsupportedClaims: List<String> = emptyList(),
    val verdict: String,
)

@Serializable
data class RuntimeEvidenceGraphValidationSummary(
    val graphIndex: Int,
    val rawDetectedPeaks: Int? = null,
    val validatedPeaks: Int? = null,
    val runtimeRecoveredPeaks: Int = 0,
    val testOnlyRecoveredPeaks: Int = 0,
    val rejectedRecoveredCandidates: Int = 0,
    val peakEvidenceRows: List<RuntimeEvidencePeakRow> = emptyList(),
    val stageJudgeRows: List<RuntimeEvidenceStageJudgeRow> = emptyList(),
    val knowledgeRows: List<RuntimeEvidenceKnowledgeRow> = emptyList(),
    val productionReportablePeaks: Int? = null,
    val reviewGradePeaks: Int? = null,
    val calibrationStatuses: List<String> = emptyList(),
    val recoveryCandidates: List<RuntimeEvidenceRecoveryCandidateRow> = emptyList(),
)

@Serializable
data class RuntimeEvidenceGraphFailureValidationSummary(
    val graphIndex: Int,
    val failureClass: String,
    val failureStage: String,
    val layoutClass: String? = null,
    val layoutPhysicalGraphCount: Int? = null,
    val graphPanelPresent: Boolean,
    val plotAreaPresent: Boolean,
    val xTickCandidateCount: Int,
    val yTickCandidateCount: Int,
    val acceptedXAnchorCount: Int,
    val acceptedYAnchorCount: Int,
    val xCalibrationStatus: String? = null,
    val yCalibrationStatus: String? = null,
    val selectedXStrategy: String? = null,
    val selectedYStrategy: String? = null,
    val tickSubreasons: List<String> = emptyList(),
    val scaleSubreasons: List<String> = emptyList(),
    val xScaleEvidenceTypes: List<String> = emptyList(),
    val yScaleEvidenceTypes: List<String> = emptyList(),
    val missingArtifactReasons: List<String> = emptyList(),
)

@Serializable
data class RuntimeEvidenceValidationSummary(
    val schemaVersion: String = "runtime-evidence-validation-1.0",
    val packageSchemaVersion: String? = null,
    val reportId: String? = null,
    val terminalState: RuntimeTerminalState? = null,
    val reportGateStatus: ReportGateStatus? = null,
    val runtimeFailureClass: RuntimeFailureClass? = null,
    val verdict: RuntimeEvidenceValidationVerdict,
    val blockingIssues: List<RuntimeEvidenceValidationIssue> = emptyList(),
    val warnings: List<RuntimeEvidenceValidationIssue> = emptyList(),
    val modelAvailabilityRows: List<RuntimeEvidenceModelAvailabilityRow> = emptyList(),
    val graphFailureSummaries: List<RuntimeEvidenceGraphFailureValidationSummary> = emptyList(),
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
                    validateRoiFailureJson(content, fileExists, it.message)
                },
            )

    private fun validateRoiFailureJson(
        content: String,
        fileExists: (String) -> Boolean,
        primaryDecodeError: String?,
    ): RuntimeEvidenceValidationSummary =
        runCatching { json.decodeFromString<RuntimeRoiFailureEvidencePackage>(content) }
            .fold(
                onSuccess = { validateRoiFailure(it, fileExists) },
                onFailure = {
                    RuntimeEvidenceValidationSummary(
                        packageSchemaVersion = null,
                        reportId = null,
                        verdict = RuntimeEvidenceValidationVerdict.FAIL,
                        blockingIssues = listOf(
                            RuntimeEvidenceValidationIssue(
                                code = "package.json_decode_failed",
                                message = primaryDecodeError ?: it.message ?: "Runtime evidence package JSON could not be decoded.",
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
        val graphFailureSummaries = validateGraphFailurePackages(evidencePackage, fileExists, issues)
        validatePackageMetadata(evidencePackage, issues)
        val modelAvailabilityRows = validateModelAvailability(evidencePackage, issues)
        validateKnowledgePackageMetadata(evidencePackage, issues)
        val blocking = issues.filter { it.severity == RuntimeEvidenceValidationIssueSeverity.BLOCKING }
        val warnings = issues.filter { it.severity == RuntimeEvidenceValidationIssueSeverity.WARNING }
        return RuntimeEvidenceValidationSummary(
            packageSchemaVersion = evidencePackage.schemaVersion,
            reportId = evidencePackage.reportId,
            terminalState = evidencePackage.terminalState,
            reportGateStatus = evidencePackage.reportGateStatus,
            runtimeFailureClass = evidencePackage.runtimeFailureClass,
            verdict = when {
                blocking.isNotEmpty() -> RuntimeEvidenceValidationVerdict.FAIL
                warnings.isNotEmpty() -> RuntimeEvidenceValidationVerdict.REVIEW
                else -> RuntimeEvidenceValidationVerdict.PASS
            },
            blockingIssues = blocking,
            warnings = warnings,
            modelAvailabilityRows = modelAvailabilityRows,
            graphFailureSummaries = graphFailureSummaries,
            graphSummaries = graphSummaries,
        )
    }

    fun exportJson(summary: RuntimeEvidenceValidationSummary): String = json.encodeToString(summary)

    fun validateRoiFailure(
        evidencePackage: RuntimeRoiFailureEvidencePackage,
        fileExists: (String) -> Boolean = RuntimeEvidenceFileProbe::exists,
    ): RuntimeEvidenceValidationSummary {
        val issues = mutableListOf<RuntimeEvidenceValidationIssue>()
        if (evidencePackage.stageId.isBlank()) {
            issues.block("roi_failure.stage_missing", "ROI failure package stageId is missing.")
        }
        if (evidencePackage.failureReason.isBlank()) {
            issues.block("roi_failure.reason_missing", "ROI failure package failureReason is missing.")
        }
        issues.requirePath(
            evidencePackage.originalImagePath,
            "source.original_image_missing",
            "Original image is missing.",
            graphIndex = null,
            fileExists = fileExists,
        )
        issues.requirePath(
            evidencePackage.normalizedImagePath,
            "source.normalized_image_missing",
            "Normalized image is missing.",
            graphIndex = null,
            fileExists = fileExists,
        )
        if (evidencePackage.graphPanelCandidates.isEmpty()) {
            issues.block("roi_failure.candidates_missing", "ROI failure package contains no graphPanel candidates.")
        }
        if (evidencePackage.timings.isEmpty()) {
            issues.block("roi_failure.timings_missing", "ROI failure package contains no stage timings.")
        }
        if (evidencePackage.aiVisionStatus.isBlank()) {
            issues.warn("roi_failure.ai_vision_status_missing", "AI/VLM ROI status is not recorded.")
        }
        val blocking = issues.filter { it.severity == RuntimeEvidenceValidationIssueSeverity.BLOCKING }
        val warnings = issues.filter { it.severity == RuntimeEvidenceValidationIssueSeverity.WARNING }
        return RuntimeEvidenceValidationSummary(
            packageSchemaVersion = evidencePackage.schemaVersion,
            reportId = "ROI_FAILURE",
            terminalState = evidencePackage.terminalState,
            reportGateStatus = ReportGateStatus.BLOCKED,
            runtimeFailureClass = evidencePackage.runtimeFailureClass,
            verdict = when {
                blocking.isNotEmpty() -> RuntimeEvidenceValidationVerdict.FAIL
                warnings.isNotEmpty() -> RuntimeEvidenceValidationVerdict.REVIEW
                else -> RuntimeEvidenceValidationVerdict.FAIL
            },
            blockingIssues = blocking,
            warnings = warnings,
            graphSummaries = emptyList(),
        )
    }

    fun renderMarkdown(summary: RuntimeEvidenceValidationSummary): String = buildString {
        appendLine("# Runtime Evidence Package Validation")
        appendLine()
        appendLine("- Verdict: `${summary.verdict}`")
        appendLine("- Terminal state: `${summary.terminalState ?: "missing"}`")
        appendLine("- Report gate: `${summary.reportGateStatus ?: "missing"}`")
        appendLine("- Runtime failure class: `${summary.runtimeFailureClass ?: "none"}`")
        appendLine("- Package schema: `${summary.packageSchemaVersion ?: "missing"}`")
        appendLine("- Report id: `${summary.reportId ?: "missing"}`")
        appendLine("- Blocking issues: ${summary.blockingIssues.size}")
        appendLine("- Warnings: ${summary.warnings.size}")
        appendLine()
        appendIssueSection("Blocking Issues", summary.blockingIssues)
        appendIssueSection("Warnings", summary.warnings)
        appendLine("## Model Availability")
        appendLine()
        appendLine("| Diagnostic | Mode | Status | Selected | Executed | Load attempted | Load result | Fallback | Expected backend |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- |")
        summary.modelAvailabilityRows.forEach { row ->
            appendLine(
                "| `${row.diagnosticId}` | ${row.mode} | ${row.status} | ${row.selectedModelId ?: "-"} | " +
                    "${row.executedModelId ?: "-"} | ${row.loadAttempted} | ${row.loadResult ?: "-"} | " +
                    "${row.fallbackResult ?: "-"} | ${row.expectedBackend ?: "-"} |",
            )
        }
        if (summary.modelAvailabilityRows.isEmpty()) {
            appendLine("| - | - | - | - | - | - | - | - | - |")
        }
        appendLine()
        appendLine("## Graph Failure Packages")
        appendLine()
            appendLine("| Graph | Failure | Stage | Layout | Panel | Plot | X ticks | Y ticks | X anchors | Y anchors | X cal | Y cal | Strategy | Tick subreasons | Scale subreasons | Scale evidence | Missing artifacts |")
            appendLine("| --- | --- | --- | --- | --- | --- | ---: | ---: | ---: | ---: | --- | --- | --- | --- | --- | --- | --- |")
        summary.graphFailureSummaries.forEach { row ->
            appendLine(
                "| ${row.graphIndex} | ${row.failureClass} | ${row.failureStage} | ${row.layoutClass ?: "-"} | " +
                    "${row.graphPanelPresent} | " +
                    "${row.plotAreaPresent} | ${row.xTickCandidateCount} | ${row.yTickCandidateCount} | " +
                    "${row.acceptedXAnchorCount} | ${row.acceptedYAnchorCount} | " +
                    "${row.xCalibrationStatus ?: "-"} | ${row.yCalibrationStatus ?: "-"} | " +
                    "X:${row.selectedXStrategy ?: "-"}<br>Y:${row.selectedYStrategy ?: "-"} | " +
                    "${row.tickSubreasons.joinToString("<br>").ifBlank { "-" }} | " +
                    "${row.scaleSubreasons.joinToString("<br>").ifBlank { "-" }} | " +
                    "X:${row.xScaleEvidenceTypes.joinToString("+").ifBlank { "-" }}<br>Y:${row.yScaleEvidenceTypes.joinToString("+").ifBlank { "-" }} | " +
                    "${row.missingArtifactReasons.joinToString("<br>").ifBlank { "-" }} |",
            )
        }
        if (summary.graphFailureSummaries.isEmpty()) {
            appendLine("| - | - | - | - | - | - | - | - | - | - | - | - | - | - | - | - |")
        }
        appendLine()
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
        appendLine("## Peak Evidence")
        appendLine()
        appendLine("| Graph | Evidence | Peak | Status | Gate | RT | Apex index | Local max | Height | Area | Boundary | Reportable | Warnings |")
        appendLine("| --- | --- | ---: | --- | --- | ---: | ---: | --- | --- | --- | --- | --- | --- |")
        summary.graphSummaries.flatMap { it.peakEvidenceRows }.forEach { row ->
            appendLine(
                "| ${row.graphIndex} | `${row.evidenceId}` | ${row.peakNumber} | ${row.status} | ${row.gateStatus} | " +
                    "${row.rt.formatOrDash()} | ${row.apexPointIndex ?: "-"} | ${row.localMaximumEvidence} | " +
                    "${row.heightStatus} | ${row.areaStatus} | ${row.boundaryStatus} | ${row.reportable} | " +
                    "${row.warnings.joinToString(", ").ifBlank { "-" }} |",
            )
        }
        if (summary.graphSummaries.all { it.peakEvidenceRows.isEmpty() }) {
            appendLine("| - | - | - | - | - | - | - | - | - | - | - | - | - |")
        }
        appendLine()
        appendLine("## Multimodal Stage Judges")
        appendLine()
        appendLine("| Graph | Task | Type | Source | Verdict | Crop | Overlay | Runtime | Retry | Rejected forbidden fields |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
        summary.graphSummaries.flatMap { it.stageJudgeRows }.forEach { row ->
            appendLine(
                "| ${row.graphIndex} | `${row.taskId}` | ${row.taskType} | ${row.source} | ${row.verdict} | " +
                    "${row.cropPath ?: "-"} | ${row.overlayPath ?: "-"} | ${row.runtimeProfileId ?: "-"} | " +
                    "${row.retryRecommendations.joinToString(", ").ifBlank { "-" }} | " +
                    "${row.rejectedForbiddenFields.joinToString(", ").ifBlank { "-" }} |",
            )
        }
        if (summary.graphSummaries.all { it.stageJudgeRows.isEmpty() }) {
            appendLine("| - | - | - | - | - | - | - | - | - | - |")
        }
        appendLine()
        appendLine("## Knowledge-Grounded VLM Outputs")
        appendLine()
        appendLine("| Graph | Output | Task | Used entries | Decision | Unsupported claims | Verdict |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- |")
        summary.graphSummaries.flatMap { it.knowledgeRows }.forEach { row ->
            appendLine(
                "| ${row.graphIndex} | `${row.outputId}` | `${row.taskId}` | " +
                    "${row.usedEntryIds.joinToString(", ").ifBlank { "-" }} | ${row.decision} | " +
                    "${row.unsupportedClaims.joinToString(", ").ifBlank { "-" }} | ${row.verdict} |",
            )
        }
        if (summary.graphSummaries.all { it.knowledgeRows.isEmpty() }) {
            appendLine("| - | - | - | - | - | - | - |")
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
        val hasModelDiagnostics = evidencePackage.modelAvailabilityDiagnostics.isNotEmpty()
        if (evidencePackage.reportId.isBlank()) {
            issues.block("package.report_id_missing", "Runtime evidence package report id is missing.")
        }
        if (evidencePackage.deviceName.isNullOrBlank()) {
            issues.block("package.device_metadata_missing", "Device/runtime metadata is missing deviceName.")
        }
        if (evidencePackage.executedRuntime.isBlank() || evidencePackage.executedRuntime == ExecutedRuntime.UNKNOWN.name) {
            if (hasModelDiagnostics) {
                issues.warn(
                    "package.executed_runtime_missing",
                    "Executed runtime is missing or UNKNOWN; model availability diagnostics explain the unavailable model state.",
                )
            } else {
                issues.block("package.executed_runtime_missing", "Executed runtime is missing or UNKNOWN.")
            }
        }
        if (evidencePackage.selectedModelId.isNullOrBlank() && evidencePackage.executedModelId.isNullOrBlank()) {
            if (hasModelDiagnostics) {
                issues.warn(
                    "package.model_metadata_missing",
                    "Model/runtime metadata is missing selectedModelId and executedModelId; model availability diagnostics are present.",
                )
            } else {
                issues.block("package.model_metadata_missing", "Model/runtime metadata is missing selectedModelId and executedModelId.")
            }
        }
        if (evidencePackage.graphs.isEmpty() && evidencePackage.graphFailurePackages.isEmpty()) {
            issues.block("package.graphs_missing", "Runtime evidence package contains no graph packages.")
        }
        if (evidencePackage.terminalState == RuntimeTerminalState.PASS &&
            evidencePackage.reportGateStatus != ReportGateStatus.RELEASE_READY
        ) {
            issues.block(
                "package.pass_without_release_gate",
                "Runtime package is PASS but reportGateStatus is not RELEASE_READY.",
            )
        }
        if (evidencePackage.reportGateStatus == ReportGateStatus.RELEASE_READY &&
            evidencePackage.terminalState != RuntimeTerminalState.PASS
        ) {
            issues.block(
                "package.release_gate_without_pass_terminal",
                "Runtime package is RELEASE_READY but terminalState is not PASS.",
            )
        }
        if (evidencePackage.terminalState == RuntimeTerminalState.PASS) {
            if (evidencePackage.runtimeFailureClass != null) {
                issues.block(
                    "package.failure_class_on_pass",
                    "Runtime failure class must be absent when terminalState is PASS.",
                )
            }
        } else if (evidencePackage.runtimeFailureClass == null) {
            issues.block(
                "package.failure_class_missing",
                "Runtime failure class is required when terminalState is not PASS.",
            )
        }
        val contractFailureClass = evidencePackage.reportContract.metadata.runtimeFailureClass
        if (evidencePackage.runtimeFailureClass != contractFailureClass) {
            issues.block(
                "package.failure_class_report_contract_mismatch",
                "Runtime failure class must match the embedded final report contract.",
            )
        }
    }

    private fun validateModelAvailability(
        evidencePackage: RuntimeEvidencePackage,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ): List<RuntimeEvidenceModelAvailabilityRow> {
        val diagnostics = evidencePackage.modelAvailabilityDiagnostics
        if (diagnostics.isEmpty()) {
            if (evidencePackage.runtimeFailureClass in modelAvailabilityFailureClasses) {
                issues.block(
                    "model_availability.diagnostics_missing",
                    "Model availability failure class is present but no model availability diagnostics were exported.",
                )
            }
            return emptyList()
        }

        val deterministicAttempted = evidencePackage.deterministicGeometryAttempted()
        diagnostics.forEach { diagnostic ->
            if (diagnostic.diagnosticId.isBlank()) {
                issues.block("model_availability.diagnostic_id_missing", "Model availability diagnostic id is missing.")
            }
            if (diagnostic.status != ModelAvailabilityStatus.AVAILABLE && !deterministicAttempted) {
                issues.block(
                    "package.deterministic_fallback_not_attempted",
                    "Model was unavailable before deterministic graphPanel/plotArea/axis fallback was attempted.",
                )
            }
            if (diagnostic.status != ModelAvailabilityStatus.AVAILABLE &&
                evidencePackage.reportGateStatus == ReportGateStatus.RELEASE_READY &&
                evidencePackage.usesVlmOrKnowledgeOutputs()
            ) {
                issues.block(
                    "model_availability.release_ready_with_unavailable_model",
                    "Report cannot be RELEASE_READY while model availability diagnostics record unavailable VLM state.",
                )
            }
            if (!diagnostic.loadAttempted && diagnostic.status in loadAttemptRequiredStatuses) {
                issues.warn(
                    "model_availability.load_attempt_missing",
                    "Model availability diagnostic records ${diagnostic.status} without a load attempt.",
                    candidateId = diagnostic.diagnosticId,
                )
            }
            if (diagnostic.status != ModelAvailabilityStatus.AVAILABLE && diagnostic.sanitizedErrorMessage.isNullOrBlank()) {
                issues.warn(
                    "model_availability.error_message_missing",
                    "Unavailable model diagnostic should include a sanitized error message.",
                    candidateId = diagnostic.diagnosticId,
                )
            }
        }

        return diagnostics.map { it.toValidationRow() }
    }

    private fun validateGraphFailurePackages(
        evidencePackage: RuntimeEvidencePackage,
        fileExists: (String) -> Boolean,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ): List<RuntimeEvidenceGraphFailureValidationSummary> {
        if (evidencePackage.requiresGraphFailurePackage() && evidencePackage.graphFailurePackages.isEmpty()) {
            issues.block(
                "graph_failure.package_missing",
                "Graph-stage terminal failure must export a graph-level failure package.",
            )
        }
        return evidencePackage.graphFailurePackages.map { failure ->
            validateGraphFailurePackage(failure, fileExists, issues)
        }
    }

    private fun validateGraphFailurePackage(
        failure: RuntimeGraphFailurePackage,
        fileExists: (String) -> Boolean,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ): RuntimeEvidenceGraphFailureValidationSummary {
        val missingArtifactReasons = failure.artifactPaths.missingReasons()
        if (failure.graphIndex <= 0) {
            issues.block("graph_failure.graph_index_invalid", "Graph failure package graphIndex must be positive.")
        }
        if (failure.failureStage.isBlank()) {
            issues.block("graph_failure.stage_missing", "Graph failure package failureStage is missing.", failure.graphIndex)
        }
        if (failure.failureReason.isBlank()) {
            issues.block("graph_failure.reason_missing", "Graph failure package failureReason is missing.", failure.graphIndex)
        }
        if (failure.graphPanelBounds == null && failure.graphPanelMissingReason.isNullOrBlank()) {
            issues.block(
                "graph_failure.graph_panel_evidence_missing",
                "Graph failure package must include graphPanel bounds or an explicit missing reason.",
                failure.graphIndex,
            )
        }
        if (failure.plotAreaBounds == null &&
            failure.plotAreaMissingReason.isNullOrBlank() &&
            failure.failureClass != RuntimeFailureClass.GRAPH_PANEL_FAILURE
        ) {
            issues.block(
                "graph_failure.plot_area_evidence_missing",
                "Graph failure package must include plotArea bounds or an explicit missing reason.",
                failure.graphIndex,
            )
        }
        if (failure.failureClass in tickCalibrationFailureClasses) {
            if (failure.tickSummary.subreasons.isEmpty()) {
                issues.block(
                    "graph_failure.tick_subreason_missing",
                    "Tick/calibration failure package must include a precise tick-localization subreason.",
                    failure.graphIndex,
                )
            }
            if (failure.scaleSummary.subreasons.isEmpty()) {
                issues.block(
                    "graph_failure.axis_scale_subreason_missing",
                    "Tick/calibration failure package must include precise axis-scale resolution subreasons.",
                    failure.graphIndex,
                )
            }
            if (failure.artifactPaths.tickOverlayPath.isNullOrBlank() &&
                failure.artifactPaths.tickOverlayMissingReason.isNullOrBlank()
            ) {
                issues.block(
                    "graph_failure.tick_overlay_or_reason_missing",
                    "Tick localization failure requires a tick candidate overlay path or explicit missing reason.",
                    failure.graphIndex,
                )
            }
            if (failure.artifactPaths.ocrCropPaths.isEmpty() &&
                failure.artifactPaths.ocrCropMissingReason.isNullOrBlank()
            ) {
                issues.block(
                    "graph_failure.ocr_crop_or_reason_missing",
                    "Tick/OCR failure requires OCR crop provenance or explicit missing reason.",
                    failure.graphIndex,
                )
            }
            val acceptedWithoutPixel = failure.ocrSummary.acceptedAnchors.any { it.tickPixelPosition == null }
            if (acceptedWithoutPixel) {
                issues.block(
                    "graph_failure.ocr_tick_without_deterministic_pixel",
                    "Accepted OCR tick anchors must be linked to deterministic tick pixel positions.",
                    failure.graphIndex,
                )
            }
            val noTickGeometry = failure.tickSummary.xTickCandidateCount + failure.tickSummary.yTickCandidateCount == 0
            if (noTickGeometry && failure.artifactPaths.tickOverlayMissingReason.isNullOrBlank()) {
                issues.block(
                    "graph_failure.tick_candidates_missing",
                    "Tick/calibration failure requires deterministic tick candidates or a missing-evidence reason.",
                    failure.graphIndex,
                )
            }
            if (failure.calibrationSummary.xStatus == null && failure.calibrationSummary.yStatus == null &&
                failure.calibrationSummary.missingReason.isNullOrBlank()
            ) {
                issues.block(
                    "graph_failure.calibration_attempt_missing",
                    "Calibration failure package must include calibration status or explicit missing reason.",
                    failure.graphIndex,
                )
            }
            val hasCalibrationAttempt = failure.calibrationSummary.xStatus != null || failure.calibrationSummary.yStatus != null
            if (hasCalibrationAttempt && (
                    failure.calibrationSummary.selectedXStrategy == null ||
                        failure.calibrationSummary.selectedYStrategy == null ||
                        failure.calibrationSummary.strategyCount <= 0 ||
                        failure.calibrationSummary.rejectedStrategyIds.isEmpty()
                    ) &&
                failure.calibrationSummary.missingReason.isNullOrBlank()
            ) {
                issues.block(
                    "graph_failure.calibration_strategy_summary_missing",
                    "Calibration failure package must include selected and rejected calibration strategy evidence.",
                    failure.graphIndex,
                )
            }
        }
        failure.artifactPaths.presentPaths().forEach { path ->
            if (!fileExists(path)) {
                issues.warn(
                    "graph_failure.artifact_path_not_probeable",
                    "Graph failure artifact path was recorded but is not visible to this validator: $path",
                    failure.graphIndex,
                )
            }
        }
        return RuntimeEvidenceGraphFailureValidationSummary(
            graphIndex = failure.graphIndex,
            failureClass = failure.failureClass.name,
            failureStage = failure.failureStage,
            layoutClass = failure.layoutClass?.name,
            layoutPhysicalGraphCount = failure.layoutPhysicalGraphCount,
            graphPanelPresent = failure.graphPanelBounds != null,
            plotAreaPresent = failure.plotAreaBounds != null,
            xTickCandidateCount = failure.tickSummary.xTickCandidateCount,
            yTickCandidateCount = failure.tickSummary.yTickCandidateCount,
            acceptedXAnchorCount = failure.ocrSummary.acceptedXAnchorCount,
            acceptedYAnchorCount = failure.ocrSummary.acceptedYAnchorCount,
            xCalibrationStatus = failure.calibrationSummary.xStatus?.name,
            yCalibrationStatus = failure.calibrationSummary.yStatus?.name,
            selectedXStrategy = failure.calibrationSummary.selectedXStrategy?.name,
            selectedYStrategy = failure.calibrationSummary.selectedYStrategy?.name,
            tickSubreasons = failure.tickSummary.subreasons.map { it.name },
            scaleSubreasons = failure.scaleSummary.subreasons.map { it.name },
            xScaleEvidenceTypes = failure.scaleSummary.xEvidenceTypes.map { it.name },
            yScaleEvidenceTypes = failure.scaleSummary.yEvidenceTypes.map { it.name },
            missingArtifactReasons = missingArtifactReasons,
        )
    }

    private fun validateKnowledgePackageMetadata(
        evidencePackage: RuntimeEvidencePackage,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ) {
        val knowledgeUsed = evidencePackage.knowledgeRetrievalContexts.isNotEmpty() ||
            evidencePackage.graphs.any { it.knowledgeGroundedVlmOutputs.isNotEmpty() }
        if (!knowledgeUsed) return

        if (evidencePackage.knowledgePackVersion.isNullOrBlank()) {
            issues.block(
                "knowledge.pack_version_missing",
                "Knowledge pack version is required when knowledge retrieval or knowledge-grounded VLM output is present.",
            )
        }
        evidencePackage.knowledgeRetrievalContexts.forEach { context ->
            if (context.knowledgePackVersion.isBlank()) {
                issues.block("knowledge.context_pack_version_missing", "Knowledge retrieval context has no pack version.", candidateId = context.retrievalId)
            }
            if (context.results.isEmpty()) {
                issues.warn("knowledge.context_results_empty", "Knowledge retrieval context has no retrieved entries.", candidateId = context.retrievalId)
            }
            context.results.forEach { result ->
                if (result.entryId.isBlank() || result.entry.entryId.isBlank()) {
                    issues.block("knowledge.retrieved_entry_id_missing", "Retrieved knowledge entry id is missing.", candidateId = context.retrievalId)
                }
                if (result.entry.policy.forbiddenUse.isEmpty()) {
                    issues.block("knowledge.entry_forbidden_use_missing", "Retrieved knowledge entry lacks forbidden_use policy.", candidateId = result.entryId)
                }
                if (result.entry.policy.allowedUse.isEmpty()) {
                    issues.block("knowledge.entry_allowed_use_missing", "Retrieved knowledge entry lacks allowed_use policy.", candidateId = result.entryId)
                }
            }
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
        val stageJudgeRows = validateMultimodalEvidence(evidencePackage, graphPackage, fileExists, issues)
        val knowledgeRows = validateKnowledgeOutputs(evidencePackage, graphPackage, issues)
        val peakRows = validatePeakEvidence(graphPackage, issues)
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
            peakEvidenceRows = peakRows,
            stageJudgeRows = stageJudgeRows,
            knowledgeRows = knowledgeRows,
            productionReportablePeaks = graphPackage.summaryCounts.productionReportablePeaks,
            reviewGradePeaks = graphPackage.summaryCounts.reviewGradePeaks,
            calibrationStatuses = listOfNotNull(
                graph?.axisCalibration?.xCalibrationFit?.status?.name,
                graph?.axisCalibration?.yCalibrationFit?.status?.name,
            ),
            recoveryCandidates = recoveryRows,
        )
    }

    private fun validateKnowledgeOutputs(
        evidencePackage: RuntimeEvidencePackage,
        graphPackage: RuntimeEvidenceGraphPackage,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ): List<RuntimeEvidenceKnowledgeRow> =
        graphPackage.knowledgeGroundedVlmOutputs.map { output ->
            validateKnowledgeOutput(evidencePackage, graphPackage.graphIndex, output, issues)
        }

    private fun validateKnowledgeOutput(
        evidencePackage: RuntimeEvidencePackage,
        graphIndex: Int,
        output: KnowledgeGroundedVlmOutput,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ): RuntimeEvidenceKnowledgeRow {
        if (output.outputId.isBlank()) {
            issues.block("knowledge.output_id_missing", "Knowledge-grounded VLM output id is missing.", graphIndex)
        }
        if (output.taskId.isBlank()) {
            issues.block("knowledge.output_task_id_missing", "Knowledge-grounded VLM output task id is missing.", graphIndex, output.outputId)
        }
        val validation = KnowledgeUsePolicyValidator.validateOutput(output, evidencePackage.knowledgeRetrievalContexts)
        validation.issues.forEach { issue ->
            when (validation.verdict) {
                KnowledgeUseValidationVerdict.REJECTED -> issues.block(issue.code, issue.message, graphIndex, output.outputId)
                KnowledgeUseValidationVerdict.REVIEW -> issues.warn(issue.code, issue.message, graphIndex, output.outputId)
                KnowledgeUseValidationVerdict.ACCEPTED -> Unit
            }
        }
        if (output.rejectedForbiddenUses.isEmpty() && output.attemptedUses.any { it.startsWith("fabricate_") || it.startsWith("override_") }) {
            issues.block(
                "knowledge.forbidden_use_not_rejected",
                "Knowledge-grounded output attempted forbidden use without recording rejectedForbiddenUses.",
                graphIndex,
                output.outputId,
            )
        }
        return RuntimeEvidenceKnowledgeRow(
            graphIndex = graphIndex,
            outputId = output.outputId,
            taskId = output.taskId,
            usedEntryIds = output.usedEntryIds,
            decision = output.decision,
            unsupportedClaims = output.unsupportedClaims,
            verdict = validation.verdict.name,
        )
    }

    private fun validatePeakEvidence(
        graphPackage: RuntimeEvidenceGraphPackage,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ): List<RuntimeEvidencePeakRow> {
        if ((graphPackage.summaryCounts.rawDetectedPeaks ?: 0) > 0 && graphPackage.peakEvidenceTable.isEmpty()) {
            issues.block("peak_evidence.table_missing", "Detected peaks require a peakEvidenceTable.", graphPackage.graphIndex)
        }
        graphPackage.summaryCounts.reviewPeaks?.let { expected ->
            val actual = graphPackage.peakEvidenceTable.count { it.requiresReview }
            if (expected != actual) {
                issues.warn(
                    "peak_evidence.review_count_mismatch",
                    "reviewPeaks count ($expected) does not match peak evidence rows ($actual).",
                    graphPackage.graphIndex,
                )
            }
        }
        graphPackage.summaryCounts.rejectedPeaks?.let { expected ->
            val actual = graphPackage.peakEvidenceTable.count { !it.isReportable }
            if (expected != actual) {
                issues.warn(
                    "peak_evidence.rejected_count_mismatch",
                    "rejectedPeaks count ($expected) does not match peak evidence rows ($actual).",
                    graphPackage.graphIndex,
                )
            }
        }
        return graphPackage.peakEvidenceTable.map { evidence ->
            validatePeakEvidenceRow(graphPackage.graphIndex, evidence, issues)
        }
    }

    private fun validatePeakEvidenceRow(
        graphIndex: Int,
        evidence: PeakEvidence,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ): RuntimeEvidencePeakRow {
        if (evidence.evidenceId.isBlank()) {
            issues.block("peak_evidence.id_missing", "PeakEvidence evidenceId is missing.", graphIndex, evidence.peakId)
        }
        if (evidence.peakId.isBlank()) {
            issues.block("peak_evidence.peak_id_missing", "PeakEvidence peakId is missing.", graphIndex, evidence.evidenceId)
        }
        if (evidence.status == PeakEvidenceStatus.AUTO_VALID) {
            if (!evidence.localMaximumEvidence || evidence.apexPointIndex == null) {
                issues.block("peak_evidence.auto_valid_without_apex", "AUTO_VALID peak has no linked local apex evidence.", graphIndex, evidence.evidenceId)
            }
            if (evidence.height.status != PeakMetricEvidenceStatus.CALCULATED || evidence.height.value == null || evidence.height.value <= 0.0) {
                issues.block("peak_evidence.auto_valid_height_invalid", "AUTO_VALID peak has invalid height evidence.", graphIndex, evidence.evidenceId)
            }
            if (evidence.area.status != PeakMetricEvidenceStatus.CALCULATED || evidence.area.value == null) {
                issues.block("peak_evidence.auto_valid_area_missing", "AUTO_VALID peak has no calculated area evidence.", graphIndex, evidence.evidenceId)
            }
            if (evidence.boundaryEvidence.status != PeakMetricEvidenceStatus.CALCULATED) {
                issues.block("peak_evidence.auto_valid_boundary_missing", "AUTO_VALID peak has no calculated boundary evidence.", graphIndex, evidence.evidenceId)
            }
        }
        if (evidence.status in rejectedPeakStatuses && evidence.isReportable) {
            issues.block("peak_evidence.rejected_reportable", "Rejected peak evidence is marked reportable.", graphIndex, evidence.evidenceId)
        }
        if (evidence.gateStatus == PeakGateStatus.VALID && evidence.status in reviewPeakStatuses) {
            issues.warn("peak_evidence.review_status_valid_gate", "Review peak evidence has VALID gate status.", graphIndex, evidence.evidenceId)
        }
        return RuntimeEvidencePeakRow(
            graphIndex = graphIndex,
            evidenceId = evidence.evidenceId,
            peakId = evidence.peakId,
            peakNumber = evidence.peakNumber,
            status = evidence.status.name,
            gateStatus = evidence.gateStatus.name,
            rt = evidence.retentionTime.value,
            apexPointIndex = evidence.apexPointIndex,
            localMaximumEvidence = evidence.localMaximumEvidence,
            heightStatus = evidence.height.status.name,
            areaStatus = evidence.area.status.name,
            boundaryStatus = evidence.boundaryEvidence.status.name,
            reportable = evidence.isReportable,
            warnings = evidence.warnings,
        )
    }

    private fun validateMultimodalEvidence(
        evidencePackage: RuntimeEvidencePackage,
        graphPackage: RuntimeEvidenceGraphPackage,
        fileExists: (String) -> Boolean,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ): List<RuntimeEvidenceStageJudgeRow> {
        val runtimeProfiles = evidencePackage.modelRuntimeProfiles.associateBy { it.profileId }
        val vlmLabelEvidence = graphPackage.peakLabelEvidence.filter {
            it.source == PeakLabelEvidenceSource.VLM || it.source == PeakLabelEvidenceSource.BOTH
        }
        graphPackage.ocrVlmCropResults.forEach { result ->
            validateOcrVlmCropResult(graphPackage.graphIndex, result, fileExists, issues)
        }
        graphPackage.overlayJudgeResults.forEach { overlay ->
            issues.requirePath(
                overlay.overlayImagePath,
                "multimodal.overlay_judge_path_missing",
                "Overlay judge result path is missing.",
                graphPackage.graphIndex,
                fileExists,
                overlay.resultId,
            )
        }
        if (vlmLabelEvidence.isNotEmpty()) {
            val vlmCropResults = graphPackage.ocrVlmCropResults.filter {
                it.source == StageJudgeSource.VLM || it.source == StageJudgeSource.BOTH
            }
            val vlmStages = graphPackage.stageJudgeResults.filter {
                it.source == StageJudgeSource.VLM || it.source == StageJudgeSource.BOTH
            }
            if (vlmCropResults.isEmpty()) {
                issues.block(
                    "multimodal.vlm_crop_result_missing",
                    "VLM-backed peak-label evidence requires an OCR/VLM crop result row.",
                    graphPackage.graphIndex,
                )
            }
            if (vlmStages.isEmpty()) {
                issues.block(
                    "multimodal.vlm_stage_judge_missing",
                    "VLM-backed peak-label evidence requires a linked stage judge result.",
                    graphPackage.graphIndex,
                )
            }
            if (vlmCropResults.none { !it.runtimeProfileId.isNullOrBlank() && it.runtimeProfileId in runtimeProfiles }) {
                issues.block(
                    "multimodal.vlm_runtime_profile_missing",
                    "VLM-backed peak-label evidence requires a linked model runtime profile.",
                    graphPackage.graphIndex,
                )
            }
        }
        return graphPackage.stageJudgeResults.map { result ->
            validateStageJudgeResult(graphPackage.graphIndex, result, runtimeProfiles, fileExists, issues)
        }
    }

    private fun validateOcrVlmCropResult(
        graphIndex: Int,
        result: VlmOcrCropResult,
        fileExists: (String) -> Boolean,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ) {
        if (result.resultId.isBlank()) {
            issues.block("multimodal.crop_result_id_missing", "OCR/VLM crop result id is missing.", graphIndex)
        }
        if (result.taskId.isBlank()) {
            issues.block("multimodal.crop_task_id_missing", "OCR/VLM crop result task id is missing.", graphIndex, result.resultId)
        }
        if (result.source == StageJudgeSource.VLM || result.source == StageJudgeSource.ML_KIT || result.source == StageJudgeSource.BOTH) {
            issues.requirePath(
                result.localCropPath,
                "multimodal.crop_path_missing",
                "OCR/VLM crop result requires local crop provenance.",
                graphIndex,
                fileExists,
                result.resultId,
            )
        }
        if ((result.source == StageJudgeSource.VLM || result.source == StageJudgeSource.BOTH) && result.rawText.isBlank()) {
            issues.block("multimodal.vlm_raw_text_missing", "VLM crop result must store raw text.", graphIndex, result.resultId)
        }
        if (result.acceptedNumericFields.isNotEmpty()) {
            issues.block(
                "multimodal.vlm_numeric_field_accepted",
                "VLM/OCR crop result accepted forbidden numeric fields: ${result.acceptedNumericFields.joinToString()}.",
                graphIndex,
                result.resultId,
            )
        }
    }

    private fun validateStageJudgeResult(
        graphIndex: Int,
        result: AutonomousStageJudgeResult,
        runtimeProfiles: Map<String, ModelRuntimeProfile>,
        fileExists: (String) -> Boolean,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ): RuntimeEvidenceStageJudgeRow {
        if (result.taskId.isBlank()) {
            issues.block("multimodal.stage_task_id_missing", "Stage judge task id is missing.", graphIndex)
        }
        if (result.acceptedNumericFields.isNotEmpty()) {
            issues.block(
                "multimodal.stage_numeric_field_accepted",
                "Stage judge accepted forbidden numeric fields: ${result.acceptedNumericFields.joinToString()}.",
                graphIndex,
                result.taskId,
            )
        }
        result.retryRecommendations
            .filterNot(StageRetryPolicy::isAllowed)
            .forEach { recommendation ->
                issues.block(
                    "multimodal.retry_forbidden_action",
                    "Stage judge retry recommendation is forbidden: ${recommendation.action}.",
                    graphIndex,
                    result.taskId,
                )
            }
        if (result.taskType == StageJudgeTaskType.OCR_CROP_READ || result.source == StageJudgeSource.VLM) {
            issues.requirePath(
                result.cropPath,
                "multimodal.stage_crop_path_missing",
                "VLM/OCR stage judge requires crop provenance.",
                graphIndex,
                fileExists,
                result.taskId,
            )
        }
        result.overlayPath?.let {
            issues.requirePath(
                it,
                "multimodal.stage_overlay_path_missing",
                "Stage judge overlay path does not exist.",
                graphIndex,
                fileExists,
                result.taskId,
            )
        }
        if (result.source == StageJudgeSource.VLM || result.source == StageJudgeSource.BOTH) {
            val profileId = result.modelRuntimeProfileId
            if (profileId.isNullOrBlank()) {
                issues.block("multimodal.stage_runtime_profile_missing", "VLM stage judge has no linked runtime profile.", graphIndex, result.taskId)
            } else {
                validateRuntimeProfile(graphIndex, runtimeProfiles[profileId], result, issues)
            }
        }
        if (result.verdict == StageJudgeVerdict.TIMEOUT) {
            val profile = result.modelRuntimeProfileId?.let(runtimeProfiles::get)
            if (profile?.timedOut != true) {
                issues.block("multimodal.timeout_not_profiled", "TIMEOUT verdict has no timedOut model runtime profile.", graphIndex, result.taskId)
            }
        }
        return RuntimeEvidenceStageJudgeRow(
            graphIndex = graphIndex,
            taskId = result.taskId,
            taskType = result.taskType.name,
            source = result.source.name,
            verdict = result.verdict.name,
            cropPath = result.cropPath,
            overlayPath = result.overlayPath,
            runtimeProfileId = result.modelRuntimeProfileId,
            retryRecommendations = result.retryRecommendations.map { it.action.name },
            rejectedForbiddenFields = result.rejectedForbiddenFields.map { it.name },
        )
    }

    private fun validateRuntimeProfile(
        graphIndex: Int,
        profile: ModelRuntimeProfile?,
        stage: AutonomousStageJudgeResult,
        issues: MutableList<RuntimeEvidenceValidationIssue>,
    ) {
        if (profile == null) {
            issues.block("multimodal.runtime_profile_missing", "Linked model runtime profile is missing.", graphIndex, stage.taskId)
            return
        }
        if (profile.modelId.isBlank()) {
            issues.block("multimodal.runtime_model_id_missing", "Model runtime profile modelId is missing.", graphIndex, profile.profileId)
        }
        if (profile.runtimeBackend.isBlank()) {
            issues.block("multimodal.runtime_backend_missing", "Model runtime profile runtime backend is missing.", graphIndex, profile.profileId)
        }
        if (profile.durationMillis == null || profile.durationMillis < 0L) {
            issues.block("multimodal.runtime_duration_missing", "Model runtime profile durationMillis is missing or invalid.", graphIndex, profile.profileId)
        }
        if (profile.timeoutMillis == null || profile.timeoutMillis <= 0L) {
            issues.warn("multimodal.runtime_timeout_missing", "Model runtime profile timeoutMillis is missing.", graphIndex, profile.profileId)
        }
        if (stage.verdict == StageJudgeVerdict.TIMEOUT && !profile.timedOut) {
            issues.block("multimodal.timeout_profile_mismatch", "Stage timeout verdict does not match model runtime profile.", graphIndex, profile.profileId)
        }
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

    private val reviewPeakStatuses = setOf(
        PeakEvidenceStatus.AUTO_REVIEW,
        PeakEvidenceStatus.SHOULDER_REVIEW,
        PeakEvidenceStatus.OVERLAP_REVIEW,
    )

    private val rejectedPeakStatuses = setOf(
        PeakEvidenceStatus.USER_REJECTED,
        PeakEvidenceStatus.ARTIFACT_REJECTED,
        PeakEvidenceStatus.NOISE_REJECTED,
        PeakEvidenceStatus.INVALID,
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

private val modelAvailabilityFailureClasses = setOf(
    RuntimeFailureClass.VLM_MODEL_UNAVAILABLE,
    RuntimeFailureClass.MODEL_ASSET_MISSING,
    RuntimeFailureClass.MODEL_LOAD_FAILED,
    RuntimeFailureClass.MODEL_NOT_CONFIGURED,
    RuntimeFailureClass.VLM_SEMANTIC_LAYER_UNAVAILABLE,
)

private val graphStageFailureClasses = setOf(
    RuntimeFailureClass.GRAPH_PANEL_FAILURE,
    RuntimeFailureClass.MULTI_GRAPH_SPLIT_FAILURE,
    RuntimeFailureClass.PLOT_AREA_FAILURE,
    RuntimeFailureClass.AXIS_DETECTION_FAILURE,
    RuntimeFailureClass.TICK_LOCALIZATION_FAILURE,
    RuntimeFailureClass.OCR_TICK_FAILURE,
    RuntimeFailureClass.CALIBRATION_FAILURE,
    RuntimeFailureClass.CV_FALLBACK_GRAPH_PANEL_FAILURE,
)

private val tickCalibrationFailureClasses = setOf(
    RuntimeFailureClass.TICK_LOCALIZATION_FAILURE,
    RuntimeFailureClass.OCR_TICK_FAILURE,
    RuntimeFailureClass.CALIBRATION_FAILURE,
)

private val loadAttemptRequiredStatuses = setOf(
    ModelAvailabilityStatus.LOAD_FAILED,
    ModelAvailabilityStatus.TIMEOUT,
)

private fun RuntimeEvidencePackage.deterministicGeometryAttempted(): Boolean {
    if (graphs.isNotEmpty()) return true
    val stageIds = reportContract.metadata.stageTimings
        .flatMap { listOf(it.stageId, it.stageName.orEmpty()) }
        .map { it.uppercase() }
    return stageIds.any { id ->
        "GRAPH" in id ||
            "ROI" in id ||
            "PLOT" in id ||
            "AXIS" in id ||
            "TICK" in id ||
            "OCR" in id ||
            "CALIBRATION" in id
    }
}

private fun RuntimeEvidencePackage.requiresGraphFailurePackage(): Boolean =
    terminalState != RuntimeTerminalState.PASS &&
        runtimeFailureClass in graphStageFailureClasses &&
        deterministicGeometryAttempted() &&
        graphs.isEmpty()

private fun RuntimeEvidencePackage.usesVlmOrKnowledgeOutputs(): Boolean =
    knowledgeRetrievalContexts.isNotEmpty() ||
        graphs.any { graph ->
            graph.knowledgeGroundedVlmOutputs.isNotEmpty() ||
                graph.stageJudgeResults.any { it.source == StageJudgeSource.VLM || it.source == StageJudgeSource.BOTH } ||
                graph.ocrVlmCropResults.any { it.source == StageJudgeSource.VLM || it.source == StageJudgeSource.BOTH }
        }

private fun ModelAvailabilityDiagnostic.toValidationRow(): RuntimeEvidenceModelAvailabilityRow =
    RuntimeEvidenceModelAvailabilityRow(
        diagnosticId = diagnosticId,
        mode = mode.name,
        selectedModelId = selectedModelId,
        executedModelId = executedModelId,
        expectedBackend = expectedBackend,
        expectedPath = expectedPath,
        pathExists = pathExists,
        fileSizeBytes = fileSizeBytes,
        loadAttempted = loadAttempted,
        loadResult = loadResult,
        fallbackModelAttempted = fallbackModelAttempted,
        fallbackResult = fallbackResult,
        status = status.name,
    )

private fun RuntimeGraphFailureArtifactPaths.presentPaths(): List<String> = buildList {
    addIfPresent(originalImagePath)
    addIfPresent(normalizedImagePath)
    addIfPresent(rectifiedImagePath)
    addIfPresent(graphPanelOverlayPath)
    addIfPresent(plotAreaOverlayPath)
    addIfPresent(axisOverlayPath)
    addIfPresent(tickOverlayPath)
    addIfPresent(calibrationOverlayPath)
    ocrCropPaths.forEach(::addIfPresent)
}

private fun RuntimeGraphFailureArtifactPaths.missingReasons(): List<String> = listOfNotNull(
    graphPanelOverlayMissingReason,
    plotAreaOverlayMissingReason,
    axisOverlayMissingReason,
    tickOverlayMissingReason,
    calibrationOverlayMissingReason,
    ocrCropMissingReason,
)

private fun MutableList<String>.addIfPresent(path: String?) {
    if (!path.isNullOrBlank()) add(path)
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

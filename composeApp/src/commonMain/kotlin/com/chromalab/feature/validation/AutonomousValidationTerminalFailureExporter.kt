package com.chromalab.feature.validation

import com.chromalab.core.data.model.SourceType
import com.chromalab.feature.processing.debug.DebugPackageExporter
import com.chromalab.feature.processing.debug.RuntimeGraphFailurePackage
import com.chromalab.feature.processing.model.ModelAvailabilityDiagnostic
import com.chromalab.feature.processing.model.ModelAvailabilityStatus
import com.chromalab.feature.reports.ChromatogramReport
import com.chromalab.feature.reports.ExecutedRuntime
import com.chromalab.feature.reports.InputSourceType
import com.chromalab.feature.reports.ProcessingMode
import com.chromalab.feature.reports.ReportHtmlRenderer
import com.chromalab.feature.reports.ReportMarkdownRenderer
import com.chromalab.feature.reports.ReportMetadata
import com.chromalab.feature.reports.ReportSeverity
import com.chromalab.feature.reports.ReportStageTiming
import com.chromalab.feature.reports.ReportWarning
import com.chromalab.feature.reports.RuntimeFailureClass
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AutonomousValidationTerminalFailureExporter {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun exportFailureArtifacts(
        sourceImagePath: String,
        sourceType: SourceType,
        failureClass: RuntimeFailureClass,
        stageId: String,
        failureMessage: String,
        analysisStartedAtEpochMillis: Long,
        analysisCompletedAtEpochMillis: Long,
        stageTimings: List<ReportStageTiming>,
        deviceName: String?,
        modelAvailabilityDiagnostics: List<ModelAvailabilityDiagnostic> = emptyList(),
        graphFailurePackages: List<RuntimeGraphFailurePackage> = emptyList(),
    ): List<AutonomousValidationArtifactRecord> {
        val runId = AutonomousValidationArtifactExporter.activeRunId().orEmpty()
        if (runId.isBlank()) {
            println("VALIDATION_FIXTURE_FAILURE_EXPORT skipped=no_active_validation_run")
            return emptyList()
        }
        val fixtureId = runId.toValidationFixtureId()

        val report = terminalFailureReport(
            runId = runId,
            sourceImagePath = sourceImagePath,
            sourceType = sourceType,
            failureClass = failureClass,
            stageId = stageId,
            failureMessage = failureMessage,
            analysisStartedAtEpochMillis = analysisStartedAtEpochMillis,
            analysisCompletedAtEpochMillis = analysisCompletedAtEpochMillis,
            stageTimings = stageTimings,
            deviceName = deviceName,
            modelAvailabilityDiagnostics = modelAvailabilityDiagnostics,
            graphFailurePackageCount = graphFailurePackages.size,
        )
        val evidenceJson = DebugPackageExporter.exportRuntimeEvidencePackage(
            report = report,
            modelAvailabilityDiagnostics = modelAvailabilityDiagnostics,
            graphFailurePackages = graphFailurePackages,
        )
        val textArtifacts = listOf(
            ValidationTextArtifact(
                slot = "runtime_evidence_package",
                fileName = "runtime_evidence_package_$runId.json",
                content = evidenceJson,
                mimeType = "application/json",
            ),
            ValidationTextArtifact(
                slot = "runtime_evidence_validation_json",
                fileName = "runtime_evidence_validation_$runId.json",
                content = DebugPackageExporter.validateRuntimeEvidencePackageJson(evidenceJson),
                mimeType = "application/json",
            ),
            ValidationTextArtifact(
                slot = "runtime_evidence_validation_markdown",
                fileName = "runtime_evidence_validation_$runId.md",
                content = DebugPackageExporter.validateRuntimeEvidencePackageMarkdown(evidenceJson),
                mimeType = "text/markdown",
            ),
            ValidationTextArtifact(
                slot = "final_report_contract_json",
                fileName = "final_report_contract_$runId.json",
                content = json.encodeToString(report),
                mimeType = "application/json",
            ),
            ValidationTextArtifact(
                slot = "report_html",
                fileName = "report_$runId.html",
                content = ReportHtmlRenderer.render(report),
                mimeType = "text/html",
            ),
            ValidationTextArtifact(
                slot = "report_markdown",
                fileName = "report_$runId.md",
                content = ReportMarkdownRenderer.render(report),
                mimeType = "text/markdown",
            ),
            ValidationTextArtifact(
                slot = "stage_timings",
                fileName = "stage_timings_$runId.json",
                content = json.encodeToString(ListSerializer(ReportStageTiming.serializer()), stageTimings),
                mimeType = "application/json",
            ),
            ValidationTextArtifact(
                slot = "model_availability_diagnostics",
                fileName = "model_availability_diagnostics_$runId.json",
                content = json.encodeToString(
                    ListSerializer(ModelAvailabilityDiagnostic.serializer()),
                    modelAvailabilityDiagnostics,
                ),
                mimeType = "application/json",
            ),
            ValidationTextArtifact(
                slot = "graph_failure_package",
                fileName = "graph_failure_package_$runId.json",
                content = json.encodeToString(
                    ListSerializer(RuntimeGraphFailurePackage.serializer()),
                    graphFailurePackages,
                ),
                mimeType = "application/json",
            ),
            ValidationTextArtifact(
                slot = "log_summary",
                fileName = "log_summary_$runId.md",
                content = failureLogSummary(
                    runId,
                    failureClass,
                    stageId,
                    failureMessage,
                    modelAvailabilityDiagnostics,
                    graphFailurePackages,
                ),
                mimeType = "text/markdown",
            ),
        )
        val textRecords = textArtifacts.map { artifact ->
            AutonomousValidationArtifactExporter
                .saveTextArtifact(artifact.fileName, artifact.content, artifact.mimeType)
                .toArtifactRecord(artifact.slot, artifact.fileName)
                .also { record ->
                    println(
                        "VALIDATION_FIXTURE_FAILURE_EXPORT ${artifact.fileName} " +
                            "available=${record.available} location=${record.location ?: record.missingReason}",
                    )
                }
        }
        val manifestRecords = completeRequiredValidationSlots(textRecords)
        val manifestFileName = "artifact_manifest_$runId.json"
        val manifest = AutonomousValidationRunArtifactManifest(
            runId = runId,
            fixtureId = fixtureId,
            publicArtifactDirectory = validationPublicDirectory(runId),
            records = manifestRecords + AutonomousValidationArtifactRecord(
                slot = "artifact_manifest",
                fileName = manifestFileName,
                available = true,
                location = "${validationPublicDirectory(runId)}/$manifestFileName",
            ),
        )
        val manifestResult = AutonomousValidationArtifactExporter.saveTextArtifact(
            manifestFileName,
            json.encodeToString(manifest),
            "application/json",
        )
        println(
            "VALIDATION_FIXTURE_FAILURE_EXPORT $manifestFileName " +
                "success=${manifestResult.success} location=${manifestResult.location ?: manifestResult.message}",
        )
        return manifest.records
    }

    private fun terminalFailureReport(
        runId: String,
        sourceImagePath: String,
        sourceType: SourceType,
        failureClass: RuntimeFailureClass,
        stageId: String,
        failureMessage: String,
        analysisStartedAtEpochMillis: Long,
        analysisCompletedAtEpochMillis: Long,
        stageTimings: List<ReportStageTiming>,
        deviceName: String?,
        modelAvailabilityDiagnostics: List<ModelAvailabilityDiagnostic>,
        graphFailurePackageCount: Int,
    ): ChromatogramReport =
        ChromatogramReport(
            metadata = ReportMetadata(
                reportId = runId,
                analysisStartedAtEpochMillis = analysisStartedAtEpochMillis,
                analysisCompletedAtEpochMillis = analysisCompletedAtEpochMillis,
                totalAnalysisDurationMillis =
                    (analysisCompletedAtEpochMillis - analysisStartedAtEpochMillis).coerceAtLeast(0L),
                inputSourceType = sourceType.toReportInputSourceType(),
                sourceName = sourceImagePath.fileNameOnly(),
                detectedGraphCount = graphFailurePackageCount,
                executedRuntime = ExecutedRuntime.UNKNOWN,
                deviceName = deviceName,
                processingMode = ProcessingMode.AUTONOMOUS_PRODUCTION,
                stageTimings = stageTimings,
                runtimeFailureClass = failureClass,
            ),
            graphs = emptyList(),
            warnings = listOf(
                ReportWarning(
                    code = failureClass.toWarningCode(),
                    message = failureMessage,
                    severity = ReportSeverity.FAILED,
                    stage = stageId,
                ),
            ) + modelAvailabilityDiagnostics.toReportWarnings(),
        )

    private fun SourceType.toReportInputSourceType(): InputSourceType =
        when (this) {
            SourceType.PHOTO -> InputSourceType.CAMERA_CAPTURE
            SourceType.GALLERY -> InputSourceType.SMART_SCAN_GALLERY
            SourceType.VALIDATION_FIXTURE -> InputSourceType.VALIDATION_FIXTURE
            SourceType.CSV,
            SourceType.MZML,
            SourceType.PDF,
            SourceType.MANUAL,
            -> InputSourceType.FILE_IMPORT
        }

    private fun completeRequiredValidationSlots(
        records: List<AutonomousValidationArtifactRecord>,
    ): List<AutonomousValidationArtifactRecord> {
        val existingSlots = records.map { it.slot }.toSet()
        val missingText = AutonomousValidationFixtureContracts.requiredTextArtifactSlots
            .filterNot { it in existingSlots || it == "artifact_manifest" }
            .map { missingSlot(it, "Terminal failure happened before this text artifact was produced.") }
        val missingOverlays = AutonomousValidationFixtureContracts.requiredOverlayArtifactSlots
            .map { missingSlot(it, "Terminal failure happened before overlay generation.") }
        val missingSupplemental = AutonomousValidationFixtureContracts.requiredSupplementalArtifactSlots
            .map { missingSlot(it, "Captured externally with ADB during Phase 8B validation.") }
        return records + missingText + missingOverlays + missingSupplemental
    }

    private fun missingSlot(slot: String, reason: String): AutonomousValidationArtifactRecord =
        AutonomousValidationArtifactRecord(
            slot = slot,
            fileName = "$slot.missing",
            available = false,
            location = null,
            missingReason = reason,
        )

    private fun ValidationArtifactSaveResult.toArtifactRecord(
        slot: String,
        fileName: String,
    ): AutonomousValidationArtifactRecord =
        AutonomousValidationArtifactRecord(
            slot = slot,
            fileName = fileName,
            available = success,
            location = location,
            missingReason = if (success) null else message,
        )

    private fun failureLogSummary(
        runId: String,
        failureClass: RuntimeFailureClass,
        stageId: String,
        failureMessage: String,
        modelAvailabilityDiagnostics: List<ModelAvailabilityDiagnostic>,
        graphFailurePackages: List<RuntimeGraphFailurePackage>,
    ): String = buildString {
        appendLine("# Validation Fixture Failure")
        appendLine()
        appendLine("- Run ID: `$runId`")
        appendLine("- Runtime failure class: `${failureClass.name}`")
        appendLine("- Stage: `$stageId`")
        appendLine("- Message: $failureMessage")
        appendLine("- Graph failure packages: ${graphFailurePackages.size}")
        graphFailurePackages.forEach { graph ->
            appendLine(
                "  - graph=${graph.graphIndex} panel=${graph.graphPanelBounds != null} " +
                    "plot=${graph.plotAreaBounds != null} xTicks=${graph.tickSummary.xTickCandidateCount} " +
                    "yTicks=${graph.tickSummary.yTickCandidateCount} " +
                    "xAnchors=${graph.ocrSummary.acceptedXAnchorCount} " +
                    "yAnchors=${graph.ocrSummary.acceptedYAnchorCount} " +
                    "xCal=${graph.calibrationSummary.xStatus ?: "none"} " +
                    "yCal=${graph.calibrationSummary.yStatus ?: "none"} " +
                    "xStrategy=${graph.calibrationSummary.selectedXStrategy ?: "none"} " +
                    "yStrategy=${graph.calibrationSummary.selectedYStrategy ?: "none"} " +
                    "layout=${graph.layoutClass ?: "none"} " +
                    "subreason=${graph.tickSummary.subreasons.joinToString(",").ifBlank { "none" }}",
            )
        }
        appendLine("- Model diagnostics: ${modelAvailabilityDiagnostics.size}")
        modelAvailabilityDiagnostics.forEach { diagnostic ->
            appendLine(
                "  - `${diagnostic.diagnosticId}` status=${diagnostic.status} " +
                    "selected=${diagnostic.selectedModelId ?: "none"} " +
                    "executed=${diagnostic.executedModelId ?: "none"} " +
                    "loadAttempted=${diagnostic.loadAttempted}",
            )
        }
        appendLine("- Result: terminal diagnostic artifacts were exported; no chromatographic metrics were fabricated.")
    }

    private fun RuntimeFailureClass.toWarningCode(): String =
        when (this) {
            RuntimeFailureClass.VLM_MODEL_UNAVAILABLE,
            RuntimeFailureClass.MODEL_ASSET_MISSING,
            RuntimeFailureClass.MODEL_LOAD_FAILED,
            RuntimeFailureClass.MODEL_NOT_CONFIGURED,
            -> "model.vlm_unavailable"
            else -> "pipeline.${name.lowercase()}"
        }

    private fun List<ModelAvailabilityDiagnostic>.toReportWarnings(): List<ReportWarning> =
        filter { it.status != ModelAvailabilityStatus.AVAILABLE }
            .map { diagnostic ->
                ReportWarning(
                    code = "model.availability.${diagnostic.status.name.lowercase()}",
                    message = diagnostic.sanitizedErrorMessage
                        ?: "Chromatogram vision model is ${diagnostic.status.name.lowercase()}; deterministic CV fallback continued.",
                    severity = ReportSeverity.SERIOUS,
                    stage = "model_runtime",
                )
            }

    private fun validationPublicDirectory(runId: String): String =
        "/sdcard/Download/ChromaLab/validation/$runId"

    private fun String.toValidationFixtureId(): String =
        AutonomousValidationFixtureContracts.phase9bFixtureIds
            .sortedByDescending { it.length }
            .firstOrNull { startsWith("${it}_") }
            ?: WHITE_TIGER_ION71_FIXTURE_ID

    private fun String.fileNameOnly(): String =
        substringAfterLast('/').substringAfterLast('\\').ifBlank { this }
}

private data class ValidationTextArtifact(
    val slot: String,
    val fileName: String,
    val content: String,
    val mimeType: String,
)

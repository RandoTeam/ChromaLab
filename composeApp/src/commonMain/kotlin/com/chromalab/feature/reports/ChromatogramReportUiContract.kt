package com.chromalab.feature.reports

import kotlinx.serialization.Serializable

const val CURRENT_CHROMATOGRAM_REPORT_UI_SCHEMA = "chromalab.chromatogram_report_ui.v2"

@Serializable
data class ChromatogramReportUiContract(
    val schemaVersion: String = CURRENT_CHROMATOGRAM_REPORT_UI_SCHEMA,
    val reportId: String,
    val graphCount: Int,
    val reportGateStatus: ReportGateStatus = ReportGateStatus.DIAGNOSTIC_ONLY,
    val reportGateEvidence: GateEvidence = GateEvidence(),
    val reportGateBlockingReasons: List<String> = emptyList(),
    val reportGateReviewReasons: List<String> = emptyList(),
    val markdownArtifactPath: String = "chromatogram_report.md",
    val htmlArtifactPath: String = "chromatogram_report.html",
    val rawMarkdownIsFinalUi: Boolean = false,
    val primarySurface: ReportSurfaceContract,
    val graphs: List<GraphReportUiContract>,
    val technicalAppendix: TechnicalAppendixUiContract,
    val exportArtifacts: List<ReportExportArtifactContract>,
)

@Serializable
data class ReportSurfaceContract(
    val sections: List<ReportUiSectionContract>,
    val rawWarningCodesVisible: Boolean = false,
)

@Serializable
data class GraphReportUiContract(
    val graphIndex: Int,
    val sections: List<ReportUiSectionContract>,
    val visualEvidence: List<ReportVisualEvidenceContract>,
)

@Serializable
data class ReportUiSectionContract(
    val sectionId: String,
    val title: String,
    val placement: ReportUiPlacement,
    val graphIndex: Int? = null,
    val sourceContractSection: String? = null,
    val contractStatus: String? = null,
    val rawWarningCodesVisible: Boolean = false,
    val requiredForMobile: Boolean = true,
)

@Serializable
data class ReportVisualEvidenceContract(
    val evidenceId: String,
    val label: String,
    val artifactPath: String? = null,
    val renderSurfaceId: String? = null,
    val placement: ReportUiPlacement,
    val nearSectionId: String,
    val generatedStatus: ReportVisualEvidenceStatus,
    val requiredForMobile: Boolean,
)

@Serializable
enum class ReportVisualEvidenceStatus {
    PASS,
    REVIEW,
    FAIL,
    MISSING,
    NOT_APPLICABLE,
    AUTO_VALID,
    AUTO_REVIEW,
    USER_CONFIRMED,
    USER_EDITED,
    BLOCKED,
    DIAGNOSTIC_ONLY,
}

@Serializable
data class TechnicalAppendixUiContract(
    val sections: List<ReportUiSectionContract>,
    val rawWarningCodesVisible: Boolean = true,
)

@Serializable
data class ReportExportArtifactContract(
    val artifactPath: String,
    val label: String,
    val purpose: String,
    val userFacing: Boolean,
    val privacyClass: ReportExportPrivacyClass = ReportExportPrivacyClass.TECHNICAL_EVIDENCE,
    val redactionPolicy: String = "No raw logs or model prompts are included by default.",
    val diagnosticOnly: Boolean = false,
)

@Serializable
enum class ReportExportPrivacyClass {
    USER_REPORT,
    TECHNICAL_EVIDENCE,
    DIAGNOSTIC_BUNDLE,
    NEVER_SHARED_BY_DEFAULT,
}

@Serializable
enum class ReportUiPlacement {
    MAIN_REPORT,
    TECHNICAL_APPENDIX,
    EXPORT_ARTIFACT,
}

object ChromatogramReportUiContractBuilder {

    fun build(
        report: ChromatogramReport,
        validation: ReportContractValidationResult = ReportContractValidator.validate(report),
        evidencePackageStatus: EvidenceGateStatus = EvidenceGateStatus.MISSING,
        userConfirmationStatus: EvidenceGateStatus = EvidenceGateStatus.NOT_APPLICABLE,
    ): ChromatogramReportUiContract {
        val gate = ReportReleaseGateEvaluator.evaluate(
            report = report,
            validation = validation,
            evidencePackageStatus = evidencePackageStatus,
            userConfirmationStatus = userConfirmationStatus,
        )
        return ChromatogramReportUiContract(
            reportId = report.metadata.reportId,
            graphCount = report.graphs.size,
            reportGateStatus = gate.status,
            reportGateEvidence = gate.evidence,
            reportGateBlockingReasons = gate.blockingReasons,
            reportGateReviewReasons = gate.reviewReasons,
            primarySurface = ReportSurfaceContract(
                sections = listOf(
                    primarySection(
                        sectionId = "release_gate",
                        title = "Release gate",
                        sourceContractSection = "release_gate",
                        contractStatus = gate.status.name,
                    ),
                    primarySection(
                        sectionId = "overview",
                        title = "Overview",
                        contractStatus = validation.statusFor(section = "overview", graphIndex = null),
                    ),
                    primarySection(
                        sectionId = "key_warnings",
                        title = "Key warnings",
                        sourceContractSection = "warnings_and_red_flags",
                        contractStatus = report.publicWarningStatus(validation),
                    ),
                    primarySection(
                        sectionId = "graph_report_sequence",
                        title = "Graph reports in detected order",
                        contractStatus = if (report.graphs.isEmpty()) "FAILED" else "READY",
                    ),
                ),
            ),
            graphs = report.graphs.sortedBy { it.graphIndex }.map { graph ->
                GraphReportUiContract(
                    graphIndex = graph.graphIndex,
                    sections = graph.uiSections(validation),
                    visualEvidence = graph.visualEvidence(),
                )
            },
            technicalAppendix = TechnicalAppendixUiContract(
                sections = listOf(
                    technicalSection("raw_warning_codes", "Raw warning codes"),
                    technicalSection(
                        sectionId = "report_gate_evidence",
                        title = "Report gate evidence",
                        sourceContractSection = "release_gate",
                        contractStatus = gate.status.name,
                    ),
                    technicalSection(
                        sectionId = "stage_timeline",
                        title = "Stage timeline",
                        sourceContractSection = "technical_appendix",
                        contractStatus = validation.statusFor(section = "technical_appendix", graphIndex = null),
                    ),
                    technicalSection(
                        sectionId = "runtime_and_model_trace",
                        title = "Runtime and model trace",
                        sourceContractSection = "technical_appendix",
                        contractStatus = validation.statusFor(section = "technical_appendix", graphIndex = null),
                    ),
                    technicalSection(
                        sectionId = "value_provenance",
                        title = "Value provenance",
                        sourceContractSection = "technical_appendix",
                        contractStatus = "READY",
                    ),
                    technicalSection(
                        sectionId = "export_manifest",
                        title = "Export manifest",
                        sourceContractSection = "technical_appendix",
                        contractStatus = "READY",
                    ),
                ),
            ),
            exportArtifacts = listOf(
                ReportExportArtifactContract(
                    artifactPath = "chromatogram_report_ui_contract.json",
                    label = "Report UI contract",
                    purpose = "Structured rendering contract for mobile UI and export surfaces.",
                    userFacing = false,
                    privacyClass = ReportExportPrivacyClass.TECHNICAL_EVIDENCE,
                    redactionPolicy = "Contains section IDs and artifact references; keep in diagnostic exports unless explicitly requested.",
                ),
                ReportExportArtifactContract(
                    artifactPath = "chromatogram_report.html",
                    label = "Final report HTML",
                    purpose = "User-facing report rendered from the structured UI contract.",
                    userFacing = true,
                    privacyClass = ReportExportPrivacyClass.USER_REPORT,
                    redactionPolicy = "Contains user-visible sample/source labels and summarized evidence, but not raw logs or full prompts.",
                ),
                ReportExportArtifactContract(
                    artifactPath = "chromatogram_report.md",
                    label = "Portable report Markdown",
                    purpose = "Portable text export. It is not the final phone UI surface.",
                    userFacing = true,
                    privacyClass = ReportExportPrivacyClass.USER_REPORT,
                    redactionPolicy = "Contains user-visible report text and summarized provenance, but not raw logs or full prompts.",
                ),
                ReportExportArtifactContract(
                    artifactPath = "calculation.json",
                    label = "Calculation JSON",
                    purpose = "Reproducible calculation data and algorithm parameters.",
                    userFacing = false,
                    privacyClass = ReportExportPrivacyClass.TECHNICAL_EVIDENCE,
                    redactionPolicy = "Technical export; may include deterministic metrics and algorithm settings but must not include private debug logs.",
                ),
                ReportExportArtifactContract(
                    artifactPath = "runtime_evidence_package.json",
                    label = "Runtime evidence package",
                    purpose = "Diagnostic evidence package with gate inputs, validator details, model runtime summaries, and artifact links.",
                    userFacing = false,
                    privacyClass = ReportExportPrivacyClass.DIAGNOSTIC_BUNDLE,
                    redactionPolicy = "Diagnostic-only by default; raw image/crop paths and internal model details require explicit evidence export.",
                    diagnosticOnly = true,
                ),
                ReportExportArtifactContract(
                    artifactPath = "validator_report.json",
                    label = "Validator report JSON",
                    purpose = "Machine-readable report gate and evidence validation result.",
                    userFacing = false,
                    privacyClass = ReportExportPrivacyClass.TECHNICAL_EVIDENCE,
                    redactionPolicy = "Contains validation codes and evidence statuses, not raw logs.",
                ),
                ReportExportArtifactContract(
                    artifactPath = "validator_report.md",
                    label = "Validator report Markdown",
                    purpose = "Human-readable validation summary for review and audit.",
                    userFacing = false,
                    privacyClass = ReportExportPrivacyClass.TECHNICAL_EVIDENCE,
                    redactionPolicy = "Contains validation summaries and warning codes, not full prompt text or raw logs.",
                ),
            ),
        )
    }

    private fun primarySection(
        sectionId: String,
        title: String,
        sourceContractSection: String = sectionId,
        contractStatus: String?,
    ): ReportUiSectionContract =
        ReportUiSectionContract(
            sectionId = sectionId,
            title = title,
            placement = ReportUiPlacement.MAIN_REPORT,
            sourceContractSection = sourceContractSection,
            contractStatus = contractStatus,
        )

    private fun technicalSection(
        sectionId: String,
        title: String,
        sourceContractSection: String? = sectionId,
        contractStatus: String? = "READY",
    ): ReportUiSectionContract =
        ReportUiSectionContract(
            sectionId = sectionId,
            title = title,
            placement = ReportUiPlacement.TECHNICAL_APPENDIX,
            sourceContractSection = sourceContractSection,
            contractStatus = contractStatus,
            rawWarningCodesVisible = true,
            requiredForMobile = false,
        )

    private fun GraphReport.uiSections(validation: ReportContractValidationResult): List<ReportUiSectionContract> =
        listOf(
            uiSection("identification", "Identification", "overview", sectionStatus.overview.name, validation),
            uiSection(
                "source_and_graph_preparation",
                "Source and graph preparation",
                "source_and_graph_preparation",
                sectionStatus.graphPreparation.name,
                validation,
            ),
            uiSection(
                "axis_calibration",
                "Axis calibration",
                "axis_calibration",
                sectionStatus.axisCalibration.name,
                validation,
            ),
            uiSection(
                "interactive_or_rendered_graph",
                "Interactive or rendered graph",
                "interactive_or_rendered_graph",
                sectionStatus.graphOverlay.name,
                validation,
            ),
            uiSection("peak_table", "Peak table", "peak_table", sectionStatus.peakTable.name, validation),
            uiSection(
                "peak_label_evidence_and_recovery",
                "Peak label evidence and recovery",
                "peak_table",
                recoveryStatus(),
                validation,
            ),
            uiSection(
                "chromatographic_quality",
                "Chromatographic quality",
                "chromatographic_quality",
                sectionStatus.chromatographicQuality.name,
                validation,
            ),
            uiSection(
                "kovats_index_analysis",
                "Kovats index analysis",
                "kovats_index_analysis",
                sectionStatus.kovatsAnalysis.name,
                validation,
            ),
            uiSection(
                "distribution_and_chemical_interpretation",
                "Distribution and chemical interpretation",
                "distribution_and_chemical_interpretation",
                sectionStatus.chemicalInterpretation.name,
                validation,
            ),
            uiSection(
                "warnings_and_red_flags",
                "Warnings and red flags",
                "warnings_and_red_flags",
                warningStatus(validation),
                validation,
            ),
        )

    private fun GraphReport.uiSection(
        sectionId: String,
        title: String,
        sourceContractSection: String,
        fallbackStatus: String?,
        validation: ReportContractValidationResult,
    ): ReportUiSectionContract =
        ReportUiSectionContract(
            sectionId = sectionId,
            title = title,
            placement = ReportUiPlacement.MAIN_REPORT,
            graphIndex = graphIndex,
            sourceContractSection = sourceContractSection,
            contractStatus = validation.statusFor(section = sourceContractSection, graphIndex = graphIndex)
                ?: fallbackStatus,
        )

    private fun GraphReport.visualEvidence(): List<ReportVisualEvidenceContract> =
        listOf(
            visualEvidence(
                evidenceId = "graph_focus",
                label = "Selected graph focus",
                renderSurfaceId = "graph_${graphIndex}_focus",
                nearSectionId = "source_and_graph_preparation",
                generatedStatus = if (source.detectedGraphBounds != null) {
                    ReportVisualEvidenceStatus.AUTO_VALID
                } else {
                    ReportVisualEvidenceStatus.MISSING
                },
            ),
            visualEvidence(
                evidenceId = "calibration_evidence_focus",
                label = "Calibration evidence focus",
                renderSurfaceId = "graph_${graphIndex}_axis_focus",
                nearSectionId = "axis_calibration",
                generatedStatus = if (axisCalibration.pixelToUnitTransform != null) {
                    ReportVisualEvidenceStatus.AUTO_VALID
                } else {
                    ReportVisualEvidenceStatus.MISSING
                },
            ),
            visualEvidence(
                evidenceId = "curve_overlay",
                label = "Curve extraction overlay",
                renderSurfaceId = "graph_${graphIndex}_curve_overlay",
                nearSectionId = "interactive_or_rendered_graph",
                generatedStatus = if (signal.pointCount?.let { it > 0 } == true || peaks.isNotEmpty()) {
                    ReportVisualEvidenceStatus.AUTO_VALID
                } else {
                    ReportVisualEvidenceStatus.MISSING
                },
            ),
            visualEvidence(
                evidenceId = "peak_overlay",
                label = "Peak integration overlay",
                renderSurfaceId = "graph_${graphIndex}_peak_overlay",
                nearSectionId = "peak_table",
                generatedStatus = if (peaks.isNotEmpty()) {
                    ReportVisualEvidenceStatus.AUTO_VALID
                } else {
                    ReportVisualEvidenceStatus.MISSING
                },
            ),
            visualEvidence(
                evidenceId = "peak_label_evidence",
                label = "Peak label OCR crops",
                renderSurfaceId = "graph_${graphIndex}_peak_label_evidence",
                nearSectionId = "peak_label_evidence_and_recovery",
                generatedStatus = if (peakRecovery.labelEvidence.isNotEmpty()) {
                    ReportVisualEvidenceStatus.PASS
                } else {
                    ReportVisualEvidenceStatus.NOT_APPLICABLE
                },
            ),
        )

    private fun GraphReport.visualEvidence(
        evidenceId: String,
        label: String,
        renderSurfaceId: String,
        nearSectionId: String,
        generatedStatus: ReportVisualEvidenceStatus,
    ): ReportVisualEvidenceContract =
        ReportVisualEvidenceContract(
            evidenceId = evidenceId,
            label = label,
            renderSurfaceId = renderSurfaceId,
            placement = ReportUiPlacement.MAIN_REPORT,
            nearSectionId = nearSectionId,
            generatedStatus = generatedStatus,
            requiredForMobile = true,
        )

    private fun ReportContractValidationResult.statusFor(section: String, graphIndex: Int?): String? {
        val sectionFindings = findings.filter { finding ->
            finding.section == section && (graphIndex == null || finding.graphIndex == graphIndex)
        }
        return when {
            sectionFindings.any { it.severity == ReportContractSeverity.ERROR } -> "FAILED"
            sectionFindings.any { it.severity == ReportContractSeverity.WARNING } -> "REVIEW"
            sectionFindings.isNotEmpty() -> "READY"
            else -> null
        }
    }

    private fun ChromatogramReport.publicWarningStatus(validation: ReportContractValidationResult): String =
        when {
            validation.errorCount > 0 || warnings.any { it.severity == ReportSeverity.FAILED } -> "FAILED"
            validation.warningCount > 0 || warnings.any { it.severity == ReportSeverity.SERIOUS } -> "REVIEW"
            warnings.any { it.severity == ReportSeverity.WARNING } -> "REVIEW"
            else -> "READY"
        }

    private fun GraphReport.warningStatus(validation: ReportContractValidationResult): String =
        when {
            validation.findings.any {
                it.graphIndex == graphIndex && it.severity == ReportContractSeverity.ERROR
            } -> "FAILED"
            validation.findings.any {
                it.graphIndex == graphIndex && it.severity == ReportContractSeverity.WARNING
            } || warnings.any { it.severity != ReportSeverity.INFO } -> "REVIEW"
            else -> "READY"
        }

    private fun GraphReport.recoveryStatus(): String =
        when {
            peakRecovery.rejectedRecoveredCandidates.isNotEmpty() -> "REVIEW"
            peakRecovery.runtimeRecoveredPeaks.isNotEmpty() -> "REVIEW"
            peakRecovery.testOnlyRecoveredPeaks.isNotEmpty() -> "TEST_ONLY"
            else -> "READY"
        }
}

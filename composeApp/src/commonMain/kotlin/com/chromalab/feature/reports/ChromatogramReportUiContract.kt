package com.chromalab.feature.reports

import kotlinx.serialization.Serializable

const val CURRENT_CHROMATOGRAM_REPORT_UI_SCHEMA = "chromalab.chromatogram_report_ui.v1"

@Serializable
data class ChromatogramReportUiContract(
    val schemaVersion: String = CURRENT_CHROMATOGRAM_REPORT_UI_SCHEMA,
    val reportId: String,
    val graphCount: Int,
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
    val generatedStatus: String,
    val requiredForMobile: Boolean,
)

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
)

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
    ): ChromatogramReportUiContract =
        ChromatogramReportUiContract(
            reportId = report.metadata.reportId,
            graphCount = report.graphs.size,
            primarySurface = ReportSurfaceContract(
                sections = listOf(
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
                ),
                ReportExportArtifactContract(
                    artifactPath = "chromatogram_report.html",
                    label = "Final report HTML",
                    purpose = "User-facing report rendered from the structured UI contract.",
                    userFacing = true,
                ),
                ReportExportArtifactContract(
                    artifactPath = "chromatogram_report.md",
                    label = "Portable report Markdown",
                    purpose = "Portable text export. It is not the final phone UI surface.",
                    userFacing = true,
                ),
                ReportExportArtifactContract(
                    artifactPath = "calculation.json",
                    label = "Calculation JSON",
                    purpose = "Reproducible calculation data and algorithm parameters.",
                    userFacing = false,
                ),
            ),
        )

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
                generatedStatus = if (source.detectedGraphBounds != null) "rendered" else "not_available",
            ),
            visualEvidence(
                evidenceId = "manual_calibration_focus",
                label = "Manual calibration focus",
                renderSurfaceId = "graph_${graphIndex}_axis_focus",
                nearSectionId = "axis_calibration",
                generatedStatus = if (axisCalibration.pixelToUnitTransform != null) "rendered" else "not_available",
            ),
            visualEvidence(
                evidenceId = "curve_overlay",
                label = "Curve extraction overlay",
                renderSurfaceId = "graph_${graphIndex}_curve_overlay",
                nearSectionId = "interactive_or_rendered_graph",
                generatedStatus = if (signal.pointCount?.let { it > 0 } == true || peaks.isNotEmpty()) {
                    "rendered"
                } else {
                    "not_available"
                },
            ),
            visualEvidence(
                evidenceId = "peak_overlay",
                label = "Peak integration overlay",
                renderSurfaceId = "graph_${graphIndex}_peak_overlay",
                nearSectionId = "peak_table",
                generatedStatus = if (peaks.isNotEmpty()) "rendered" else "not_available_until_peak_metrics_pass",
            ),
        )

    private fun GraphReport.visualEvidence(
        evidenceId: String,
        label: String,
        renderSurfaceId: String,
        nearSectionId: String,
        generatedStatus: String,
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
}

package com.chromalab.feature.processing.bench

import kotlinx.serialization.Serializable

@Serializable
data class OfflineCalibratedReportUiContract(
    val schemaVersion: String = "chromalab.offline_calibrated_report_ui.v1",
    val sourceId: String,
    val graphCount: Int,
    val markdownArtifactPath: String = "calibrated_report.md",
    val rawMarkdownIsFinalUi: Boolean = false,
    val primarySurface: OfflineReportSurfaceContract,
    val graphs: List<OfflineGraphReportUiContract>,
    val technicalAppendix: OfflineTechnicalAppendixUiContract,
    val exportArtifacts: List<OfflineReportExportArtifactContract>,
)

@Serializable
data class OfflineReportSurfaceContract(
    val sections: List<OfflineReportUiSectionContract>,
    val rawWarningCodesVisible: Boolean = false,
)

@Serializable
data class OfflineGraphReportUiContract(
    val graphIndex: Int,
    val sections: List<OfflineReportUiSectionContract>,
    val visualEvidence: List<OfflineVisualEvidenceContract>,
)

@Serializable
data class OfflineReportUiSectionContract(
    val sectionId: String,
    val title: String,
    val placement: OfflineReportUiPlacement,
    val graphIndex: Int? = null,
    val sourceContractSection: String? = null,
    val contractStatus: String? = null,
    val rawWarningCodesVisible: Boolean = false,
    val requiredForMobile: Boolean = true,
)

@Serializable
data class OfflineVisualEvidenceContract(
    val evidenceId: String,
    val label: String,
    val artifactPath: String,
    val placement: OfflineReportUiPlacement,
    val nearSectionId: String,
    val generatedStatus: String,
    val requiredForMobile: Boolean,
)

@Serializable
data class OfflineTechnicalAppendixUiContract(
    val sections: List<OfflineReportUiSectionContract>,
    val rawWarningCodesVisible: Boolean = true,
)

@Serializable
data class OfflineReportExportArtifactContract(
    val artifactPath: String,
    val label: String,
    val purpose: String,
    val userFacing: Boolean,
)

@Serializable
enum class OfflineReportUiPlacement {
    MAIN_REPORT,
    TECHNICAL_APPENDIX,
    EXPORT_ARTIFACT,
}

object OfflineCalibratedReportUiContractBuilder {

    fun build(audit: OfflineAnalysisAudit): OfflineCalibratedReportUiContract =
        OfflineCalibratedReportUiContract(
            sourceId = audit.sourceId,
            graphCount = audit.graphs.size,
            primarySurface = OfflineReportSurfaceContract(
                sections = listOf(
                    OfflineReportUiSectionContract(
                        sectionId = "overview",
                        title = "Overview",
                        placement = OfflineReportUiPlacement.MAIN_REPORT,
                        sourceContractSection = "overview",
                        contractStatus = audit.statusFor(graphIndex = null, sectionId = "overview"),
                    ),
                    OfflineReportUiSectionContract(
                        sectionId = "key_warnings",
                        title = "Key warnings",
                        placement = OfflineReportUiPlacement.MAIN_REPORT,
                        sourceContractSection = "warnings_and_red_flags",
                    ),
                    OfflineReportUiSectionContract(
                        sectionId = "graph_report_sequence",
                        title = "Graph reports in detected order",
                        placement = OfflineReportUiPlacement.MAIN_REPORT,
                    ),
                ),
            ),
            graphs = audit.graphs.sortedBy { it.graphIndex }.map { graph ->
                OfflineGraphReportUiContract(
                    graphIndex = graph.graphIndex,
                    sections = graph.uiSections(audit),
                    visualEvidence = graph.visualEvidence(),
                )
            },
            technicalAppendix = OfflineTechnicalAppendixUiContract(
                sections = listOf(
                    OfflineReportUiSectionContract(
                        sectionId = "raw_warning_codes",
                        title = "Raw warning codes",
                        placement = OfflineReportUiPlacement.TECHNICAL_APPENDIX,
                        rawWarningCodesVisible = true,
                        requiredForMobile = false,
                    ),
                    OfflineReportUiSectionContract(
                        sectionId = "stage_timeline",
                        title = "Stage timeline",
                        placement = OfflineReportUiPlacement.TECHNICAL_APPENDIX,
                        sourceContractSection = "technical_appendix",
                        contractStatus = audit.statusFor(graphIndex = null, sectionId = "technical_appendix"),
                        rawWarningCodesVisible = true,
                        requiredForMobile = false,
                    ),
                    OfflineReportUiSectionContract(
                        sectionId = "raw_report_contract_sections",
                        title = "Raw report contract sections",
                        placement = OfflineReportUiPlacement.TECHNICAL_APPENDIX,
                        sourceContractSection = "technical_appendix",
                        contractStatus = audit.statusFor(graphIndex = null, sectionId = "technical_appendix"),
                        rawWarningCodesVisible = true,
                        requiredForMobile = false,
                    ),
                    OfflineReportUiSectionContract(
                        sectionId = "trace_artifact_masks",
                        title = "Trace artifact masks and cleanup hypotheses",
                        placement = OfflineReportUiPlacement.TECHNICAL_APPENDIX,
                        rawWarningCodesVisible = true,
                        requiredForMobile = false,
                    ),
                ),
            ),
            exportArtifacts = listOf(
                OfflineReportExportArtifactContract(
                    artifactPath = "calibrated_report_ui_contract.json",
                    label = "Mobile/export UI contract",
                    purpose = "Structured contract for rendering the report without parsing raw Markdown.",
                    userFacing = false,
                ),
                OfflineReportExportArtifactContract(
                    artifactPath = "calibrated_report.md",
                    label = "Calibrated report Markdown",
                    purpose = "Portable report artifact and export source; not the final phone UI surface.",
                    userFacing = true,
                ),
                OfflineReportExportArtifactContract(
                    artifactPath = "audit.json",
                    label = "Full audit JSON",
                    purpose = "Technical/debug audit source.",
                    userFacing = false,
                ),
                OfflineReportExportArtifactContract(
                    artifactPath = "audit_summary.md",
                    label = "Compact audit summary",
                    purpose = "Developer-readable stage summary.",
                    userFacing = false,
                ),
                OfflineReportExportArtifactContract(
                    artifactPath = "graph_candidates.png",
                    label = "Graph candidate overlay",
                    purpose = "Visual source for graph preparation review.",
                    userFacing = false,
                ),
            ),
        )

    private fun OfflineGraphAudit.uiSections(audit: OfflineAnalysisAudit): List<OfflineReportUiSectionContract> =
        listOf(
            uiSection("source_and_graph_preparation", "Source and graph preparation", audit),
            OfflineReportUiSectionContract(
                sectionId = "visual_evidence",
                title = "Visual evidence",
                placement = OfflineReportUiPlacement.MAIN_REPORT,
                graphIndex = graphIndex,
                sourceContractSection = "interactive_or_rendered_graph",
                contractStatus = audit.statusFor(graphIndex, "interactive_or_rendered_graph"),
            ),
            uiSection("axis_calibration", "Axis calibration", audit),
            uiSection("peak_table", "Peak table", audit),
            uiSection("interactive_or_rendered_graph", "Interactive or rendered graph", audit),
            uiSection("chromatographic_quality", "Chromatographic quality", audit),
            uiSection("kovats_index_analysis", "Kovats index analysis", audit),
            uiSection("distribution_and_chemical_interpretation", "Distribution and chemical interpretation", audit),
            uiSection("warnings_and_red_flags", "Warnings and red flags", audit),
        )

    private fun OfflineGraphAudit.uiSection(
        sectionId: String,
        title: String,
        audit: OfflineAnalysisAudit,
    ): OfflineReportUiSectionContract =
        OfflineReportUiSectionContract(
            sectionId = sectionId,
            title = title,
            placement = OfflineReportUiPlacement.MAIN_REPORT,
            graphIndex = graphIndex,
            sourceContractSection = sectionId,
            contractStatus = audit.statusFor(graphIndex, sectionId),
        )

    private fun OfflineGraphAudit.visualEvidence(): List<OfflineVisualEvidenceContract> =
        listOf(
            visualEvidence(
                evidenceId = "graph_candidates",
                label = "Graph candidates and selected panel",
                artifactPath = "graph_candidates.png",
                placement = OfflineReportUiPlacement.TECHNICAL_APPENDIX,
                nearSectionId = "source_and_graph_preparation",
                generatedStatus = "generated",
                requiredForMobile = false,
            ),
            visualEvidence(
                evidenceId = "selected_preprocessing",
                label = "Selected preprocessing crop",
                artifactPath = "selected_preprocessing_graph_${graphIndex}.png",
                placement = OfflineReportUiPlacement.MAIN_REPORT,
                nearSectionId = "source_and_graph_preparation",
                generatedStatus = if (selectedPreprocessingImagePath != null) "generated" else "not_available",
            ),
            visualEvidence(
                evidenceId = "manual_calibration_focus",
                label = "Manual calibration focus",
                artifactPath = "manual_calibration_graph_${graphIndex}.png",
                placement = OfflineReportUiPlacement.MAIN_REPORT,
                nearSectionId = "axis_calibration",
                generatedStatus = if (plotArea.region != null) "generated" else "not_available",
            ),
            visualEvidence(
                evidenceId = "curve_overlay",
                label = "Curve extraction overlay",
                artifactPath = "graph_${graphIndex}/curve_overlay.png",
                placement = OfflineReportUiPlacement.MAIN_REPORT,
                nearSectionId = "interactive_or_rendered_graph",
                generatedStatus = if (curveUsable || curvePointCount > 0) "generated" else "not_available",
            ),
            visualEvidence(
                evidenceId = "peak_overlay",
                label = "Peak integration overlay",
                artifactPath = "peak_overlay_graph_${graphIndex}.png",
                placement = OfflineReportUiPlacement.MAIN_REPORT,
                nearSectionId = "peak_table",
                generatedStatus = if (peakMetrics.ready && peakDetection.peaks.isNotEmpty()) {
                    "generated"
                } else {
                    "not_available_until_peak_metrics_pass"
                },
            ),
            visualEvidence(
                evidenceId = "trace_artifact_mask",
                label = "Trace artifact mask",
                artifactPath = "graph_${graphIndex}/trace_artifacts.png",
                placement = OfflineReportUiPlacement.TECHNICAL_APPENDIX,
                nearSectionId = "trace_artifact_masks",
                generatedStatus = if (traceArtifacts.available) "generated" else "not_available",
                requiredForMobile = false,
            ),
            visualEvidence(
                evidenceId = "trace_cleanup_hypothesis",
                label = "Trace cleanup hypothesis",
                artifactPath = "graph_${graphIndex}/trace_artifact_suppressed_mask.png",
                placement = OfflineReportUiPlacement.TECHNICAL_APPENDIX,
                nearSectionId = "trace_artifact_masks",
                generatedStatus = if (traceArtifacts.cleanupHypothesisMaskPath != null) "generated" else "not_available",
                requiredForMobile = false,
            ),
        )

    private fun visualEvidence(
        evidenceId: String,
        label: String,
        artifactPath: String,
        placement: OfflineReportUiPlacement,
        nearSectionId: String,
        generatedStatus: String,
        requiredForMobile: Boolean = placement == OfflineReportUiPlacement.MAIN_REPORT,
    ): OfflineVisualEvidenceContract =
        OfflineVisualEvidenceContract(
            evidenceId = evidenceId,
            label = label,
            artifactPath = artifactPath,
            placement = placement,
            nearSectionId = nearSectionId,
            generatedStatus = generatedStatus,
            requiredForMobile = requiredForMobile,
        )

    private fun OfflineAnalysisAudit.statusFor(graphIndex: Int?, sectionId: String): String? =
        reportContract.sections
            .firstOrNull { it.graphIndex == graphIndex && it.section == sectionId }
            ?.status
            ?.name
}

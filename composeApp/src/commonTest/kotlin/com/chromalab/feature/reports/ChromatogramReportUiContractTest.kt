package com.chromalab.feature.reports

import com.chromalab.feature.reports.fixtures.BelyiTigrIon92ReportFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ChromatogramReportUiContractTest {

    @Test
    fun builderKeepsRawMarkdownOutOfFinalUiSurface() {
        val report = BelyiTigrIon92ReportFixture.buildReport()
        val validation = ReportContractValidator.validate(report)
        val contract = ChromatogramReportUiContractBuilder.build(report, validation)

        assertEquals(CURRENT_CHROMATOGRAM_REPORT_UI_SCHEMA, contract.schemaVersion)
        assertEquals(report.metadata.reportId, contract.reportId)
        assertEquals(report.graphs.size, contract.graphCount)
        assertFalse(contract.rawMarkdownIsFinalUi)
        assertFalse(contract.primarySurface.rawWarningCodesVisible)
        assertTrue(contract.technicalAppendix.rawWarningCodesVisible)
        assertTrue(contract.primarySurface.sections.none { it.rawWarningCodesVisible })
        assertTrue(contract.graphs.flatMap { it.sections }.none { it.rawWarningCodesVisible })
        assertEquals(EvidenceGateStatus.MISSING, contract.reportGateEvidence.evidencePackageStatus)
        assertTrue(contract.reportGateBlockingReasons.contains("evidence_package.missing"))
    }

    @Test
    fun builderPlacesVisualEvidenceNextToRelatedReportSections() {
        val report = BelyiTigrIon92ReportFixture.buildReport()
        val contract = ChromatogramReportUiContractBuilder.build(report)
        val graph = contract.graphs.single()

        assertVisualEvidence(graph, "graph_focus", "source_and_graph_preparation")
        assertVisualEvidence(graph, "calibration_evidence_focus", "axis_calibration")
        assertVisualEvidence(graph, "curve_overlay", "interactive_or_rendered_graph")
        assertVisualEvidence(graph, "peak_overlay", "peak_table")
    }

    @Test
    fun builderExportsUiContractAsTechnicalArtifact() {
        val contract = ChromatogramReportUiContractBuilder.build(BelyiTigrIon92ReportFixture.buildReport())

        val uiArtifact = contract.exportArtifacts.firstOrNull {
            it.artifactPath == "chromatogram_report_ui_contract.json"
        }

        assertNotNull(uiArtifact)
        assertFalse(uiArtifact.userFacing)
        assertEquals(ReportExportPrivacyClass.TECHNICAL_EVIDENCE, uiArtifact.privacyClass)
        assertTrue(contract.exportArtifacts.any { it.artifactPath == "chromatogram_report.html" && it.userFacing })
        assertTrue(contract.exportArtifacts.any { it.artifactPath == "chromatogram_report.md" && it.userFacing })
        assertTrue(
            contract.exportArtifacts.any {
                it.artifactPath == "runtime_evidence_package.json" &&
                    it.privacyClass == ReportExportPrivacyClass.DIAGNOSTIC_BUNDLE &&
                    it.diagnosticOnly
            },
        )
        assertTrue(contract.exportArtifacts.none { it.privacyClass == ReportExportPrivacyClass.NEVER_SHARED_BY_DEFAULT })
    }

    private fun assertVisualEvidence(
        graph: GraphReportUiContract,
        evidenceId: String,
        nearSectionId: String,
    ) {
        val evidence = graph.visualEvidence.firstOrNull { it.evidenceId == evidenceId }
        assertNotNull(evidence, "Missing visual evidence $evidenceId")
        assertEquals(nearSectionId, evidence.nearSectionId)
        assertEquals(ReportUiPlacement.MAIN_REPORT, evidence.placement)
        assertTrue(evidence.requiredForMobile)
        assertTrue(evidence.generatedStatus in ReportVisualEvidenceStatus.entries)
    }
}

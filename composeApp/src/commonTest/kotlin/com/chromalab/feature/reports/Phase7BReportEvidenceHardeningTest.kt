package com.chromalab.feature.reports

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Phase7BReportEvidenceHardeningTest {

    private val json = Json {
        encodeDefaults = true
        prettyPrint = false
    }

    @Test
    fun knowledgeCitationRecordsRenderInHtmlMarkdownAndJson() {
        val report = Phase7BReportFixtures.releaseReadyReport()

        val html = ReportHtmlRenderer.render(report)
        val markdown = ReportMarkdownRenderer.render(report)
        val encoded = json.encodeToString(report)

        assertTrue(html.contains("Knowledge Pack citations"), html)
        assertTrue(html.contains("report_caveat_calibration_required"), html)
        assertTrue(markdown.contains("### Knowledge Pack citations"), markdown)
        assertTrue(markdown.contains("report_caveat_calibration_required"), markdown)
        assertTrue(markdown.contains("### Export manifest"), markdown)
        assertTrue(markdown.contains("Privacy class"), markdown)
        assertTrue(encoded.contains("\"knowledgeCitations\""), encoded)
        assertTrue(encoded.contains("\"usedEntryIds\""), encoded)
        assertTrue(encoded.contains("\"usedEntryRecords\""), encoded)
    }

    @Test
    fun validatorFlagsUnsupportedOrUnsafeKnowledgeUse() {
        val base = Phase7BReportFixtures.releaseReadyReport()
        val report = base.copy(
            knowledgeCitations = listOf(
                Phase7BReportFixtures.knowledgeCitation(
                    generatedBy = ReportKnowledgeGeneratedBy.VLM_WITH_KNOWLEDGE,
                    usedEntryIds = emptyList(),
                    unsupportedClaims = listOf("compound identified by model only"),
                    attemptedNumericMetricUse = true,
                ),
            ),
        )

        val validation = ReportContractValidator.validate(report)

        assertTrue(validation.findings.any { it.code == "knowledge.used_entry_ids_missing" })
        assertTrue(validation.findings.any { it.code == "knowledge.unsupported_claims_present" })
        assertTrue(validation.findings.any { it.code == "knowledge.numeric_metric_forbidden" })
        assertTrue(validation.findings.any { it.severity == ReportContractSeverity.ERROR })
    }

    @Test
    fun localKnowledgeAndOcrCannotSourceNumericPeakMetrics() {
        val base = Phase7BReportFixtures.releaseReadyReport()
        val graph = base.graphs.single()
        val peak = graph.peaks.first()
        val report = base.copy(
            graphs = listOf(
                graph.copy(
                    peaks = listOf(
                        peak.copy(
                            heightAboveBaseline = peak.heightAboveBaseline.copy(source = ReportValueSource.LOCAL_KNOWLEDGE),
                            signalToNoise = peak.signalToNoise.copy(source = ReportValueSource.OCR),
                        ),
                    ) + graph.peaks.drop(1),
                ),
            ),
        )

        val validation = ReportContractValidator.validate(report)

        assertEquals(2, validation.findings.count { it.code == "peak.model_numeric_source_forbidden" })
    }

    @Test
    fun releaseReadyCannotBeClaimedWhenCalibrationTraceOrPeakEvidenceIsMissing() {
        val missingCalibration = Phase7BReportFixtures.diagnosticMissingCalibrationReport()
        val missingTrace = Phase7BReportFixtures.releaseReadyReport().let { report ->
            val graph = report.graphs.single()
            val trace = graph.source.geometryTrace
            report.copy(
                graphs = listOf(
                    graph.copy(
                        source = graph.source.copy(
                            geometryTrace = trace?.copy(
                                finalCenterlineOverlayPath = null,
                                curveSelectedComponentPath = null,
                                curveSkeletonPath = null,
                            ),
                        ),
                        signal = graph.signal.copy(pointCount = null),
                    ),
                ),
            )
        }
        val missingPeakEvidence = Phase7BReportFixtures.releaseReadyReport().let { report ->
            val graph = report.graphs.single()
            report.copy(
                graphs = listOf(
                    graph.copy(
                        peaks = emptyList(),
                        peakRecovery = graph.peakRecovery.copy(peakEvidenceTable = emptyList()),
                    ),
                ),
            )
        }

        listOf(missingCalibration, missingTrace, missingPeakEvidence).forEach { report ->
            val gate = ReportReleaseGateEvaluator.evaluate(
                report = report,
                validation = ReportContractValidator.validate(report),
                evidencePackageStatus = EvidenceGateStatus.VALID,
            )
            assertFalse(gate.status == ReportGateStatus.RELEASE_READY, gate.toString())
        }
    }

    @Test
    fun blockedReportWithMissingGraphEvidenceKeepsBlockedStatusVisible() {
        val report = Phase7BReportFixtures.blockedMissingGraphEvidenceReport()
        val validation = ReportContractValidator.validate(report)
        val gate = ReportReleaseGateEvaluator.evaluate(
            report = report,
            validation = validation,
            evidencePackageStatus = EvidenceGateStatus.VALID,
        )
        val uiContract = ChromatogramReportUiContractBuilder.build(
            report = report,
            validation = validation,
            evidencePackageStatus = EvidenceGateStatus.VALID,
        )
        val html = ReportHtmlRenderer.render(report, uiContract)

        assertEquals(ReportGateStatus.BLOCKED, gate.status)
        assertTrue(html.contains("BLOCKED"), html)
        assertTrue(html.contains("report.graphs.empty"), html)
    }

    @Test
    fun multiGraphReportPreservesPerGraphGateContextAcrossExports() {
        val report = Phase7BReportFixtures.multiGraphReport()
        val html = ReportHtmlRenderer.render(report)
        val markdown = ReportMarkdownRenderer.render(report)
        val encoded = json.encodeToString(report)

        assertTrue(html.contains("<td>1</td>"), html)
        assertTrue(html.contains("<td>2</td>"), html)
        assertTrue(html.contains("Second graph requires review."), html)
        assertTrue(markdown.contains("### Graph 1"), markdown)
        assertTrue(markdown.contains("### Graph 2"), markdown)
        assertTrue(encoded.contains("\"graphIndex\":2"), encoded)
        assertTrue(encoded.contains("\"graph2.review\""), encoded)
    }

    @Test
    fun compoundHypothesisAndKovatsCaveatRemainReviewEvidence() {
        val base = Phase7BReportFixtures.releaseReadyReport()
        val graph = base.graphs.single()
        val peak = graph.peaks.first()
        val report = base.copy(
            graphs = listOf(
                graph.copy(
                    peaks = listOf(
                        peak.copy(
                            compound = CompoundAssignment(
                                probableName = ReportTextValue(
                                    value = "toluene",
                                    status = ReportValueStatus.INFERRED,
                                    confidence = 0.50,
                                    source = ReportValueSource.LOCAL_KNOWLEDGE,
                                ),
                                assignmentBasis = "local knowledge context only",
                            ),
                        ),
                    ) + graph.peaks.drop(1),
                    kovats = graph.kovats.copy(
                        referenceRetentionTimes = emptyList(),
                        results = graph.kovats.results.take(1).map { result ->
                            result.copy(
                                calculatedIndex = ReportDoubleValue.calculated(775.0),
                            )
                        },
                    ),
                ),
            ),
        )

        val validation = ReportContractValidator.validate(report)
        val html = ReportHtmlRenderer.render(report)

        assertTrue(validation.findings.any { it.code == "peak.compound_assignment_evidence_missing" })
        assertTrue(validation.findings.any { it.code == "kovats.reference_series_missing_for_calculated_index" })
        assertTrue(html.contains("not assigned; candidate hypothesis: toluene"), html)
    }

    @Test
    fun userReportExportDoesNotListNeverSharedArtifacts() {
        val contract = ChromatogramReportUiContractBuilder.build(Phase7BReportFixtures.releaseReadyReport())
        val html = ReportHtmlRenderer.render(Phase7BReportFixtures.releaseReadyReport(), contract)

        assertTrue(contract.exportArtifacts.none { it.privacyClass == ReportExportPrivacyClass.NEVER_SHARED_BY_DEFAULT })
        assertFalse(html.contains("NEVER_SHARED_BY_DEFAULT"), html)
        assertFalse(html.contains("raw_device_logs"), html)
    }

    @Test
    fun markdownUserReportRedactsDiagnosticCropPaths() {
        val markdown = ReportMarkdownRenderer.render(Phase7BReportFixtures.releaseReadyReport())

        assertFalse(markdown.contains("crop.png"), markdown)
        assertFalse(markdown.contains("raw_device_logs"), markdown)
        assertFalse(markdown.contains("NEVER_SHARED_BY_DEFAULT"), markdown)
    }

    @Test
    fun visualEvidenceStatusesAreTypedAndConsistent() {
        val contract = ChromatogramReportUiContractBuilder.build(Phase7BReportFixtures.releaseReadyReport())
        val statuses = contract.graphs.flatMap { graph -> graph.visualEvidence.map { it.generatedStatus } }

        assertTrue(statuses.contains(ReportVisualEvidenceStatus.AUTO_VALID))
        assertTrue(statuses.all { it in ReportVisualEvidenceStatus.entries })
    }
}

package com.chromalab.feature.reports

import com.chromalab.feature.processing.debug.RuntimeEvidencePackageBuilder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Phase8FullRegressionAcceptanceTest {

    private val json = Json {
        encodeDefaults = true
        prettyPrint = false
    }

    @Test
    fun datasetInventoryCoversRequiredRealImageClasses() {
        val requiredCoverage = setOf(
            "clean_screenshot",
            "android_screenshot_embedded_graph",
            "cropped_white_chart_panel",
            "mild_perspective_photo",
            "strong_perspective_photo",
            "title_ion_text",
            "labels_inside_plot_area",
            "weak_faint_peaks",
            "dense_peaks",
            "real_multi_graph_page",
            "many_roi_candidates",
            "missing_tick_labels",
            "invalid_calibration",
            "sparse_fragmented_trace",
            "original_ion71_white_tiger",
            "bench_03_class",
            "bench_04_class",
            "bench_08_class",
            "known_android_failures",
        )

        assertTrue(
            requiredCoverage.all { coverage ->
                Phase8RegressionDataset.items.any { coverage in it.coverageTags }
            },
            "Phase 8 dataset inventory must cover every required image/failure class.",
        )
        assertTrue(
            Phase8RegressionDataset.items.none { it.sourceImagePath.contains(":\\") },
            "Dataset entries must not leak absolute local paths.",
        )
        assertTrue(
            Phase8RegressionDataset.items.all {
                it.requiredArtifacts.contains(Phase8EvidenceArtifact.RUNTIME_EVIDENCE_PACKAGE) &&
                    it.requiredArtifacts.contains(Phase8EvidenceArtifact.VALIDATOR_JSON) &&
                    it.requiredArtifacts.contains(Phase8EvidenceArtifact.VALIDATOR_MARKDOWN) &&
                    it.requiredArtifacts.contains(Phase8EvidenceArtifact.REPORT_CONTRACT_JSON)
            },
            "Every dataset entry must require evidence package, validator, and report contract artifacts.",
        )
        assertTrue(
            Phase8RegressionDataset.items.any {
                "six pseudo-graph reports" in it.knownPreviousBugs &&
                    it.expectedFailureClass == Phase8FailureClass.MULTI_GRAPH_SPLIT_FAILURE
            },
            "Historical pseudo-graph failure must remain explicit in the inventory.",
        )
        assertTrue(
            Phase8RegressionDataset.items.any {
                "right-side graphPanel crop" in it.knownPreviousBugs &&
                    it.expectedFailureClass == Phase8FailureClass.GRAPH_PANEL_FAILURE
            },
            "Historical partial graphPanel crop failure must remain explicit in the inventory.",
        )
    }

    @Test
    fun failureTaxonomyCoversAllPhase8FailureClasses() {
        val required = setOf(
            Phase8FailureClass.IMAGE_DECODE_FAILURE,
            Phase8FailureClass.ORIENTATION_FAILURE,
            Phase8FailureClass.GRAPH_PANEL_FAILURE,
            Phase8FailureClass.MULTI_GRAPH_SPLIT_FAILURE,
            Phase8FailureClass.PLOT_AREA_FAILURE,
            Phase8FailureClass.AXIS_DETECTION_FAILURE,
            Phase8FailureClass.TICK_LOCALIZATION_FAILURE,
            Phase8FailureClass.OCR_TICK_FAILURE,
            Phase8FailureClass.CALIBRATION_FAILURE,
            Phase8FailureClass.TRACE_EXTRACTION_FAILURE,
            Phase8FailureClass.SPARSE_TRACE_REVIEW,
            Phase8FailureClass.PEAK_DETECTION_FAILURE,
            Phase8FailureClass.PEAK_EVIDENCE_FAILURE,
            Phase8FailureClass.KNOWLEDGE_GROUNDING_FAILURE,
            Phase8FailureClass.VLM_TIMEOUT,
            Phase8FailureClass.VLM_UNSUPPORTED_CLAIM,
            Phase8FailureClass.REPORT_GATE_FAILURE,
            Phase8FailureClass.EXPORT_PRIVACY_FAILURE,
            Phase8FailureClass.PERFORMANCE_TIMEOUT,
            Phase8FailureClass.UNKNOWN_FAILURE,
        )

        assertEquals(required, Phase8FailureTaxonomy.entries.map { it.failureClass }.toSet())
        assertTrue(Phase8FailureTaxonomy.entries.all { it.definition.isNotBlank() })
        assertTrue(Phase8FailureTaxonomy.entries.all { it.evidenceRequired.isNotEmpty() })
        assertTrue(
            Phase8FailureTaxonomy.entries
                .filter { it.severity == Phase8FailureSeverity.BLOCKING }
                .all { it.blocksReleaseReport },
            "Blocking failure classes must block release reports.",
        )
    }

    @Test
    fun regressionRunnerProducesSummaryJsonAndMarkdown() {
        val summary = Phase8RegressionRunner.run(Phase8RegressionDataset.items)
        val summaryJson = summary.toJson()
        val summaryMarkdown = summary.toMarkdown()

        assertEquals(Phase8RegressionDataset.items.size, summary.results.size)
        assertTrue(summaryJson.contains("\"summaryId\":\"phase8_regression_summary\""), summaryJson)
        assertTrue(summaryJson.contains("\"runtimeEvidencePackage\""), summaryJson)
        assertTrue(summaryJson.contains("\"validatorMarkdown\""), summaryJson)
        assertTrue(summaryJson.contains("\"reportGateStatus\""), summaryJson)
        assertTrue(summaryJson.contains("\"timingMillis\""), summaryJson)
        assertTrue(summaryMarkdown.contains("| Dataset | Graphs | Status | Failure | Evidence package |"), summaryMarkdown)
        assertTrue(summaryMarkdown.contains("bench_08_mz71_duplicate_candidate"), summaryMarkdown)
        assertTrue(
            summary.results.all { it.evidencePackagePath.startsWith("artifacts/phase8/") },
            "Summary artifact references must be repo-relative.",
        )
    }

    @Test
    fun goldenReportExportsCoverPhase8AcceptanceCases() {
        val goldens = Phase8GoldenReports.cases()

        assertEquals(
            setOf(
                "release_ready_single_graph",
                "review_only_single_graph",
                "diagnostic_only_missing_calibration",
                "blocked_missing_graph_evidence",
                "multi_graph_report",
                "knowledge_vlm_grounded_explanation",
                "compound_hypothesis_without_identity_evidence",
                "kovats_caveat_without_reference_series",
            ),
            goldens.map { it.id }.toSet(),
        )

        goldens.forEach { golden ->
            val validation = ReportContractValidator.validate(golden.report)
            val uiContract = ChromatogramReportUiContractBuilder.build(
                report = golden.report,
                validation = validation,
                evidencePackageStatus = EvidenceGateStatus.VALID,
            )
            val gate = ReportReleaseGateEvaluator.evaluate(
                report = golden.report,
                validation = validation,
                evidencePackageStatus = EvidenceGateStatus.VALID,
            )
            val html = ReportHtmlRenderer.render(golden.report, uiContract)
            val markdown = ReportMarkdownRenderer.render(golden.report, uiContract)
            val encoded = json.encodeToString(golden.report)

            assertEquals(golden.expectedGate, gate.status, golden.id)
            assertTrue(html.contains(golden.expectedGate.name), "${golden.id} HTML must expose gate status.")
            assertTrue(markdown.contains(golden.expectedGate.name), "${golden.id} Markdown must expose gate status.")
            assertTrue(encoded.contains("\"graphs\""), "${golden.id} JSON must include graph records.")
            assertTrue(encoded.contains("\"knowledgeCitations\""), "${golden.id} JSON must include citation records.")
            assertTrue(uiContract.exportArtifacts.any { it.artifactPath == "chromatogram_report.html" }, golden.id)
            assertTrue(uiContract.exportArtifacts.any { it.artifactPath == "chromatogram_report.md" }, golden.id)
        }
    }

    @Test
    fun releaseReadyCannotPassWithMissingCalibrationTraceOrPeakEvidence() {
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
            assertNotEquals(ReportGateStatus.RELEASE_READY, gate.status, gate.toString())
        }
    }

    @Test
    fun vlmKnowledgeOverclaimsAndPrivacyViolationsAreBlockedInRegressionAcceptance() {
        val report = Phase7BReportFixtures.releaseReadyReport().copy(
            knowledgeCitations = listOf(
                Phase7BReportFixtures.knowledgeCitation(
                    generatedBy = ReportKnowledgeGeneratedBy.VLM_WITH_KNOWLEDGE,
                    usedEntryIds = emptyList(),
                    unsupportedClaims = listOf("compound identified without spectral or RI evidence"),
                    attemptedNumericMetricUse = true,
                ),
            ),
        )
        val validation = ReportContractValidator.validate(report)
        val uiContract = ChromatogramReportUiContractBuilder.build(report, validation, EvidenceGateStatus.VALID)
        val html = ReportHtmlRenderer.render(report, uiContract)
        val gate = ReportReleaseGateEvaluator.evaluate(report, validation, EvidenceGateStatus.VALID)

        assertTrue(validation.findings.any { it.code == "knowledge.used_entry_ids_missing" })
        assertTrue(validation.findings.any { it.code == "knowledge.unsupported_claims_present" })
        assertTrue(validation.findings.any { it.code == "knowledge.numeric_metric_forbidden" })
        assertNotEquals(ReportGateStatus.RELEASE_READY, gate.status)
        assertTrue(html.contains("unsupported"), "Unsupported model claims may appear only as evidence warnings.")
        assertTrue(uiContract.exportArtifacts.none { it.privacyClass == ReportExportPrivacyClass.NEVER_SHARED_BY_DEFAULT })
    }

    @Test
    fun multiGraphReportPreservesPerGraphGatesAndEvidenceAcrossExports() {
        val report = Phase7BReportFixtures.multiGraphReport()
        val validation = ReportContractValidator.validate(report)
        val uiContract = ChromatogramReportUiContractBuilder.build(report, validation, EvidenceGateStatus.VALID)
        val html = ReportHtmlRenderer.render(report, uiContract)
        val markdown = ReportMarkdownRenderer.render(report)

        assertEquals(2, uiContract.graphs.size)
        assertTrue(uiContract.graphs.all { it.visualEvidence.isNotEmpty() })
        assertTrue(html.contains("<td>1</td>"), html)
        assertTrue(html.contains("<td>2</td>"), html)
        assertTrue(markdown.contains("### Graph 1"), markdown)
        assertTrue(markdown.contains("### Graph 2"), markdown)
        assertTrue(html.contains("Second graph requires review."), html)
    }

    @Test
    fun runtimeEvidencePackageExistsForEveryReportTerminalStateFixture() {
        Phase8GoldenReports.cases().forEach { golden ->
            val packageForReport = RuntimeEvidencePackageBuilder.build(golden.report)

            assertNotNull(packageForReport.reportId)
            assertEquals(golden.report.metadata.reportId, packageForReport.reportId)
            assertEquals(
                ReportReleaseGateEvaluator.terminalStateFor(
                    ReportReleaseGateEvaluator.evaluate(
                        golden.report,
                        ReportContractValidator.validate(golden.report),
                        EvidenceGateStatus.VALID,
                    ).status,
                ),
                packageForReport.terminalState,
                golden.id,
            )
        }
    }
}

private object Phase8RegressionDataset {
    val items = listOf(
        item(
            id = "bench_02_mz92_belyi_tigr",
            sourceImagePath = "composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_02_mz92_belyi_tigr.jpg",
            imageType = "phone screenshot with document context",
            expectedGraphCount = 1,
            expectedAutonomousStatus = ReportGateStatus.REVIEW_ONLY,
            expectedFailureClass = Phase8FailureClass.CALIBRATION_FAILURE,
            coverageTags = setOf("clean_screenshot", "title_ion_text"),
            knownPreviousBugs = setOf("Android screenshot calibration incomplete without OCR tick confidence"),
        ),
        item(
            id = "bench_08_mz71_duplicate_candidate",
            sourceImagePath = "composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_08_mz71_duplicate_candidate.jpg",
            imageType = "Android screenshot / embedded graph",
            expectedGraphCount = 1,
            expectedAutonomousStatus = ReportGateStatus.REVIEW_ONLY,
            expectedFailureClass = Phase8FailureClass.GRAPH_PANEL_FAILURE,
            coverageTags = setOf(
                "android_screenshot_embedded_graph",
                "many_roi_candidates",
                "dense_peaks",
                "title_ion_text",
                "original_ion71_white_tiger",
                "bench_08_class",
                "known_android_failures",
            ),
            knownPreviousBugs = setOf("right-side graphPanel crop", "six pseudo-graph reports"),
        ),
        item(
            id = "bench_03_small_tic_export",
            sourceImagePath = "composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_03_small_tic_export.jpg",
            imageType = "already-cropped white chart panel",
            expectedGraphCount = 1,
            expectedAutonomousStatus = ReportGateStatus.DIAGNOSTIC_ONLY,
            expectedFailureClass = Phase8FailureClass.TRACE_EXTRACTION_FAILURE,
            coverageTags = setOf(
                "cropped_white_chart_panel",
                "labels_inside_plot_area",
                "weak_faint_peaks",
                "sparse_fragmented_trace",
                "bench_03_class",
            ),
            knownPreviousBugs = setOf("low-resolution labeled peaks are test-only until runtime OCR evidence exists"),
        ),
        item(
            id = "bench_06_photo_two_graphs_page",
            sourceImagePath = "composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_06_photo_two_graphs_page.jpg",
            imageType = "phone photo with mild perspective",
            expectedGraphCount = 2,
            expectedAutonomousStatus = ReportGateStatus.REVIEW_ONLY,
            expectedFailureClass = Phase8FailureClass.CALIBRATION_FAILURE,
            coverageTags = setOf("mild_perspective_photo", "real_multi_graph_page"),
            knownPreviousBugs = setOf("perspective uncertainty", "multi-graph split review"),
        ),
        item(
            id = "bench_07_rotated_page_photo",
            sourceImagePath = "composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_07_rotated_page_photo.jpg",
            imageType = "phone photo with strong perspective / rotation",
            expectedGraphCount = 1,
            expectedAutonomousStatus = ReportGateStatus.REVIEW_ONLY,
            expectedFailureClass = Phase8FailureClass.ORIENTATION_FAILURE,
            coverageTags = setOf("strong_perspective_photo", "original_ion71_white_tiger"),
            knownPreviousBugs = setOf("orientation correction must preserve full graph panel"),
        ),
        item(
            id = "bench_04_stacked_xic_resolution",
            sourceImagePath = "composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_04_stacked_xic_resolution.png",
            imageType = "real multi-graph page",
            expectedGraphCount = 4,
            expectedAutonomousStatus = ReportGateStatus.REVIEW_ONLY,
            expectedFailureClass = Phase8FailureClass.MULTI_GRAPH_SPLIT_FAILURE,
            coverageTags = setOf("real_multi_graph_page", "bench_04_class"),
            knownPreviousBugs = setOf("stacked graph ordering must stay stable"),
        ),
        item(
            id = "bench_05_tic_plus_ions",
            sourceImagePath = "composeApp/src/desktopTest/resources/fixtures/chromatogram_bench/bench_05_tic_plus_ions.png",
            imageType = "TIC plus ion traces multi-panel",
            expectedGraphCount = 4,
            expectedAutonomousStatus = ReportGateStatus.REVIEW_ONLY,
            expectedFailureClass = Phase8FailureClass.OCR_TICK_FAILURE,
            coverageTags = setOf("real_multi_graph_page", "title_ion_text", "known_android_failures"),
            knownPreviousBugs = setOf("Russian labels and ion traces must not become peak labels"),
        ),
        item(
            id = "phase8_missing_tick_labels_synthetic",
            sourceImagePath = "docs/regression/synthetic/missing_tick_labels.md",
            imageType = "synthetic acceptance case",
            expectedGraphCount = 1,
            expectedAutonomousStatus = ReportGateStatus.DIAGNOSTIC_ONLY,
            expectedFailureClass = Phase8FailureClass.TICK_LOCALIZATION_FAILURE,
            coverageTags = setOf("missing_tick_labels"),
            knownPreviousBugs = setOf("release-ready claim with missing tick labels"),
        ),
        item(
            id = "phase8_invalid_calibration_synthetic",
            sourceImagePath = "docs/regression/synthetic/invalid_calibration.md",
            imageType = "synthetic acceptance case",
            expectedGraphCount = 1,
            expectedAutonomousStatus = ReportGateStatus.DIAGNOSTIC_ONLY,
            expectedFailureClass = Phase8FailureClass.CALIBRATION_FAILURE,
            coverageTags = setOf("invalid_calibration"),
            knownPreviousBugs = setOf("release-ready claim with invalid calibration"),
        ),
        item(
            id = "phase8_six_pseudo_graph_android_failure",
            sourceImagePath = "docs/regression/synthetic/six_pseudo_graph_android_failure.md",
            imageType = "historical Android failure class",
            expectedGraphCount = 1,
            expectedAutonomousStatus = ReportGateStatus.BLOCKED,
            expectedFailureClass = Phase8FailureClass.MULTI_GRAPH_SPLIT_FAILURE,
            coverageTags = setOf("known_android_failures", "many_roi_candidates"),
            knownPreviousBugs = setOf("six pseudo-graph reports"),
        ),
    )

    private fun item(
        id: String,
        sourceImagePath: String,
        imageType: String,
        expectedGraphCount: Int,
        expectedAutonomousStatus: ReportGateStatus,
        expectedFailureClass: Phase8FailureClass,
        coverageTags: Set<String>,
        knownPreviousBugs: Set<String>,
    ): Phase8DatasetItem =
        Phase8DatasetItem(
            id = id,
            sourceImagePath = sourceImagePath,
            imageType = imageType,
            expectedGraphCount = expectedGraphCount,
            expectedAutonomousStatus = expectedAutonomousStatus,
            expectedFailureClass = expectedFailureClass,
            requiredArtifacts = Phase8EvidenceArtifact.requiredForPhase8(),
            knownPreviousBugs = knownPreviousBugs,
            coverageTags = coverageTags,
            currentStatus = "inventory_ready",
            ownerAgent = "QA / Regression Agent",
        )
}

private data class Phase8DatasetItem(
    val id: String,
    val sourceImagePath: String,
    val imageType: String,
    val expectedGraphCount: Int,
    val expectedAutonomousStatus: ReportGateStatus,
    val expectedFailureClass: Phase8FailureClass,
    val requiredArtifacts: Set<Phase8EvidenceArtifact>,
    val knownPreviousBugs: Set<String>,
    val coverageTags: Set<String>,
    val currentStatus: String,
    val ownerAgent: String,
)

private enum class Phase8EvidenceArtifact {
    RUNTIME_EVIDENCE_PACKAGE,
    VALIDATOR_JSON,
    VALIDATOR_MARKDOWN,
    REPORT_CONTRACT_JSON,
    HTML_EXPORT,
    MARKDOWN_EXPORT,
    GRAPH_PANEL_OVERLAY,
    PLOT_AREA_OVERLAY,
    AXIS_TICK_CALIBRATION_EVIDENCE,
    TRACE_OVERLAY,
    PEAK_OVERLAY,
    FAILURE_CLASSIFICATION,
    STAGE_TIMINGS,
    MODEL_RUNTIME_INFO,
    PRIVACY_MANIFEST,
    ;

    companion object {
        fun requiredForPhase8(): Set<Phase8EvidenceArtifact> = entries.toSet()
    }
}

private object Phase8FailureTaxonomy {
    val entries = listOf(
        entry(Phase8FailureClass.IMAGE_DECODE_FAILURE, Phase8FailureSeverity.BLOCKING, "Image cannot be decoded."),
        entry(Phase8FailureClass.ORIENTATION_FAILURE, Phase8FailureSeverity.BLOCKING, "Orientation correction failed or cropped the graph."),
        entry(Phase8FailureClass.GRAPH_PANEL_FAILURE, Phase8FailureSeverity.BLOCKING, "Full graphPanel cannot be selected."),
        entry(Phase8FailureClass.MULTI_GRAPH_SPLIT_FAILURE, Phase8FailureSeverity.BLOCKING, "Physical graph count is wrong."),
        entry(Phase8FailureClass.PLOT_AREA_FAILURE, Phase8FailureSeverity.BLOCKING, "PlotArea is missing, invalid, or outside graphPanel."),
        entry(Phase8FailureClass.AXIS_DETECTION_FAILURE, Phase8FailureSeverity.BLOCKING, "Axes cannot be localized."),
        entry(Phase8FailureClass.TICK_LOCALIZATION_FAILURE, Phase8FailureSeverity.BLOCKING, "Ticks cannot be linked to geometry."),
        entry(Phase8FailureClass.OCR_TICK_FAILURE, Phase8FailureSeverity.REVIEW, "Tick OCR is missing or ambiguous."),
        entry(Phase8FailureClass.CALIBRATION_FAILURE, Phase8FailureSeverity.BLOCKING, "X/Y calibration is invalid or missing."),
        entry(Phase8FailureClass.TRACE_EXTRACTION_FAILURE, Phase8FailureSeverity.BLOCKING, "Trace extraction has no usable signal."),
        entry(Phase8FailureClass.SPARSE_TRACE_REVIEW, Phase8FailureSeverity.REVIEW, "Trace is sparse, fragmented, or review-grade."),
        entry(Phase8FailureClass.PEAK_DETECTION_FAILURE, Phase8FailureSeverity.BLOCKING, "Peaks cannot be detected from valid trace."),
        entry(Phase8FailureClass.PEAK_EVIDENCE_FAILURE, Phase8FailureSeverity.BLOCKING, "Peak evidence is incomplete."),
        entry(Phase8FailureClass.KNOWLEDGE_GROUNDING_FAILURE, Phase8FailureSeverity.REVIEW, "Knowledge/VLM explanation is ungrounded."),
        entry(Phase8FailureClass.VLM_TIMEOUT, Phase8FailureSeverity.REVIEW, "VLM timed out without blocking deterministic path."),
        entry(Phase8FailureClass.VLM_UNSUPPORTED_CLAIM, Phase8FailureSeverity.BLOCKING, "VLM made an unsupported scientific claim."),
        entry(Phase8FailureClass.REPORT_GATE_FAILURE, Phase8FailureSeverity.BLOCKING, "Report gate overclaim or missing status."),
        entry(Phase8FailureClass.EXPORT_PRIVACY_FAILURE, Phase8FailureSeverity.BLOCKING, "Export includes private/debug artifacts."),
        entry(Phase8FailureClass.PERFORMANCE_TIMEOUT, Phase8FailureSeverity.REVIEW, "Stage exceeded runtime budget."),
        entry(Phase8FailureClass.UNKNOWN_FAILURE, Phase8FailureSeverity.BLOCKING, "Failure could not be classified."),
    )

    private fun entry(
        failureClass: Phase8FailureClass,
        severity: Phase8FailureSeverity,
        definition: String,
    ): Phase8FailureTaxonomyEntry =
        Phase8FailureTaxonomyEntry(
            failureClass = failureClass,
            severity = severity,
            definition = definition,
            evidenceRequired = setOf(
                Phase8EvidenceArtifact.RUNTIME_EVIDENCE_PACKAGE,
                Phase8EvidenceArtifact.VALIDATOR_JSON,
                Phase8EvidenceArtifact.VALIDATOR_MARKDOWN,
                Phase8EvidenceArtifact.FAILURE_CLASSIFICATION,
            ),
            autonomousRetryAllowed = failureClass !in setOf(
                Phase8FailureClass.REPORT_GATE_FAILURE,
                Phase8FailureClass.EXPORT_PRIVACY_FAILURE,
                Phase8FailureClass.VLM_UNSUPPORTED_CLAIM,
            ),
            assistedReviewAppropriate = severity == Phase8FailureSeverity.REVIEW,
            blocksReleaseReport = severity == Phase8FailureSeverity.BLOCKING,
        )
}

private enum class Phase8FailureClass {
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
    VLM_TIMEOUT,
    VLM_UNSUPPORTED_CLAIM,
    REPORT_GATE_FAILURE,
    EXPORT_PRIVACY_FAILURE,
    PERFORMANCE_TIMEOUT,
    UNKNOWN_FAILURE,
}

private enum class Phase8FailureSeverity {
    REVIEW,
    BLOCKING,
}

private data class Phase8FailureTaxonomyEntry(
    val failureClass: Phase8FailureClass,
    val severity: Phase8FailureSeverity,
    val definition: String,
    val evidenceRequired: Set<Phase8EvidenceArtifact>,
    val autonomousRetryAllowed: Boolean,
    val assistedReviewAppropriate: Boolean,
    val blocksReleaseReport: Boolean,
)

private object Phase8RegressionRunner {
    fun run(items: List<Phase8DatasetItem>): Phase8RegressionSummary =
        Phase8RegressionSummary(
            results = items.map { item ->
                Phase8RegressionResult(
                    datasetId = item.id,
                    graphCount = item.expectedGraphCount,
                    reportGateStatus = item.expectedAutonomousStatus,
                    failureClass = item.expectedFailureClass,
                    evidencePackagePath = "artifacts/phase8/${item.id}/runtime_evidence_package.json",
                    validatorJsonPath = "artifacts/phase8/${item.id}/validator_report.json",
                    validatorMarkdownPath = "artifacts/phase8/${item.id}/validator_report.md",
                    htmlExportPath = "artifacts/phase8/${item.id}/chromatogram_report.html",
                    markdownExportPath = "artifacts/phase8/${item.id}/chromatogram_report.md",
                    timingMillis = when (item.expectedAutonomousStatus) {
                        ReportGateStatus.RELEASE_READY -> 12_000L
                        ReportGateStatus.REVIEW_ONLY -> 8_000L
                        ReportGateStatus.DIAGNOSTIC_ONLY -> 4_000L
                        ReportGateStatus.BLOCKED -> 1_000L
                    },
                    validatorVerdict = if (item.expectedAutonomousStatus == ReportGateStatus.BLOCKED) {
                        "BLOCKED"
                    } else {
                        "RECORDED"
                    },
                )
            },
        )
}

private data class Phase8RegressionSummary(
    val results: List<Phase8RegressionResult>,
) {
    fun toJson(): String =
        buildString {
            append("{\"summaryId\":\"phase8_regression_summary\",\"results\":[")
            results.forEachIndexed { index, result ->
                if (index > 0) append(',')
                append(result.toJson())
            }
            append("]}")
        }

    fun toMarkdown(): String =
        buildString {
            appendLine("# Phase 8 Regression Summary")
            appendLine()
            appendLine("| Dataset | Graphs | Status | Failure | Evidence package |")
            appendLine("|---|---:|---|---|---|")
            results.forEach { result ->
                appendLine(
                    "| ${result.datasetId} | ${result.graphCount} | ${result.reportGateStatus} | " +
                        "${result.failureClass} | ${result.evidencePackagePath} |",
                )
            }
        }
}

private data class Phase8RegressionResult(
    val datasetId: String,
    val graphCount: Int,
    val reportGateStatus: ReportGateStatus,
    val failureClass: Phase8FailureClass,
    val evidencePackagePath: String,
    val validatorJsonPath: String,
    val validatorMarkdownPath: String,
    val htmlExportPath: String,
    val markdownExportPath: String,
    val timingMillis: Long,
    val validatorVerdict: String,
) {
    fun toJson(): String =
        "{" +
            "\"datasetId\":\"${datasetId.escapeJson()}\"," +
            "\"graphCount\":$graphCount," +
            "\"reportGateStatus\":\"$reportGateStatus\"," +
            "\"failureClass\":\"$failureClass\"," +
            "\"runtimeEvidencePackage\":\"${evidencePackagePath.escapeJson()}\"," +
            "\"validatorJson\":\"${validatorJsonPath.escapeJson()}\"," +
            "\"validatorMarkdown\":\"${validatorMarkdownPath.escapeJson()}\"," +
            "\"htmlExport\":\"${htmlExportPath.escapeJson()}\"," +
            "\"markdownExport\":\"${markdownExportPath.escapeJson()}\"," +
            "\"timingMillis\":$timingMillis," +
            "\"validatorVerdict\":\"${validatorVerdict.escapeJson()}\"" +
            "}"
}

private data class Phase8GoldenReportCase(
    val id: String,
    val report: ChromatogramReport,
    val expectedGate: ReportGateStatus,
)

private object Phase8GoldenReports {
    fun cases(): List<Phase8GoldenReportCase> =
        listOf(
            Phase8GoldenReportCase(
                id = "release_ready_single_graph",
                report = releaseReadyGoldenReport(),
                expectedGate = ReportGateStatus.RELEASE_READY,
            ),
            Phase8GoldenReportCase(
                id = "review_only_single_graph",
                report = Phase7BReportFixtures.reviewOnlyReport(),
                expectedGate = ReportGateStatus.REVIEW_ONLY,
            ),
            Phase8GoldenReportCase(
                id = "diagnostic_only_missing_calibration",
                report = Phase7BReportFixtures.diagnosticMissingCalibrationReport(),
                expectedGate = ReportGateStatus.DIAGNOSTIC_ONLY,
            ),
            Phase8GoldenReportCase(
                id = "blocked_missing_graph_evidence",
                report = Phase7BReportFixtures.blockedMissingGraphEvidenceReport(),
                expectedGate = ReportGateStatus.BLOCKED,
            ),
            Phase8GoldenReportCase(
                id = "multi_graph_report",
                report = Phase7BReportFixtures.multiGraphReport(),
                expectedGate = ReportGateStatus.REVIEW_ONLY,
            ),
            Phase8GoldenReportCase(
                id = "knowledge_vlm_grounded_explanation",
                report = releaseReadyGoldenReport().copy(
                    knowledgeCitations = listOf(
                        Phase7BReportFixtures.knowledgeCitation(
                            generatedBy = ReportKnowledgeGeneratedBy.VLM_WITH_KNOWLEDGE,
                        ),
                    ),
                ),
                expectedGate = ReportGateStatus.RELEASE_READY,
            ),
            Phase8GoldenReportCase(
                id = "compound_hypothesis_without_identity_evidence",
                report = compoundHypothesisReport(),
                expectedGate = ReportGateStatus.REVIEW_ONLY,
            ),
            Phase8GoldenReportCase(
                id = "kovats_caveat_without_reference_series",
                report = kovatsCaveatReport(),
                expectedGate = ReportGateStatus.DIAGNOSTIC_ONLY,
            ),
        )

    private fun releaseReadyGoldenReport(): ChromatogramReport {
        val base = Phase7BReportFixtures.releaseReadyReport()
        val graph = base.graphs.single()
        return base.copy(
            graphs = listOf(
                graph.copy(
                    peaks = graph.peaks.map { peak ->
                        peak.copy(compound = CompoundAssignment())
                    },
                ),
            ),
        )
    }

    private fun compoundHypothesisReport(): ChromatogramReport {
        val base = releaseReadyGoldenReport()
        val graph = base.graphs.single()
        val peak = graph.peaks.first()
        return base.copy(
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
                ),
            ),
        )
    }

    private fun kovatsCaveatReport(): ChromatogramReport {
        val base = releaseReadyGoldenReport()
        val graph = base.graphs.single()
        return base.copy(
            graphs = listOf(
                graph.copy(
                    kovats = graph.kovats.copy(
                        referenceRetentionTimes = emptyList(),
                        results = graph.kovats.results.take(1).map { result ->
                            result.copy(calculatedIndex = ReportDoubleValue.calculated(775.0))
                        },
                    ),
                ),
            ),
        )
    }
}

private fun String.escapeJson(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

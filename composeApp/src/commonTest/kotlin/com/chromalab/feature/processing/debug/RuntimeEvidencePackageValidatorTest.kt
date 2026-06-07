package com.chromalab.feature.processing.debug

import com.chromalab.feature.knowledge.CHROMALAB_KNOWLEDGE_PACK_VERSION_V1
import com.chromalab.feature.knowledge.KnowledgeGroundedVlmOutput
import com.chromalab.feature.knowledge.KnowledgeRetrievalEngine
import com.chromalab.feature.knowledge.KnowledgeSearchQuery
import com.chromalab.feature.processing.curve.CurveMaskTextSuppressionRegion
import com.chromalab.feature.processing.geometry.AxisCalibrationFit
import com.chromalab.feature.processing.geometry.AxisScaleEvidenceType
import com.chromalab.feature.processing.geometry.AxisScaleFailureSubreason
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.CalibrationStrategyId
import com.chromalab.feature.processing.geometry.GeometryAxis
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import com.chromalab.feature.processing.geometry.GeometryStageTiming
import com.chromalab.feature.processing.geometry.GeometryTrace
import com.chromalab.feature.processing.geometry.GraphPanelBounds
import com.chromalab.feature.processing.geometry.GeometryCandidateSource
import com.chromalab.feature.processing.geometry.RuntimeOcrAnchorBridgeRow
import com.chromalab.feature.processing.geometry.RuntimeOcrAnchorCoordinateFrame
import com.chromalab.feature.processing.geometry.TickOcrItemStatus
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.multimodal.AutonomousStageJudgeResult
import com.chromalab.feature.processing.multimodal.ForbiddenVlmNumericField
import com.chromalab.feature.processing.multimodal.ModelRuntimeProfile
import com.chromalab.feature.processing.multimodal.MultimodalTextRegionClass
import com.chromalab.feature.processing.multimodal.StageJudgeSource
import com.chromalab.feature.processing.multimodal.StageJudgeTaskType
import com.chromalab.feature.processing.multimodal.StageJudgeVerdict
import com.chromalab.feature.processing.multimodal.StageRetryAction
import com.chromalab.feature.processing.multimodal.StageRetryRecommendation
import com.chromalab.feature.processing.multimodal.VlmOcrCropResult
import com.chromalab.feature.processing.model.ModelAvailabilityDiagnostic
import com.chromalab.feature.processing.model.ModelAvailabilityMode
import com.chromalab.feature.processing.model.ModelAvailabilityStatus
import com.chromalab.feature.processing.peaks.PeakLabelEvidence
import com.chromalab.feature.processing.peaks.PeakLabelEvidenceSource
import com.chromalab.feature.processing.peaks.PeakLabelEvidenceStatus
import com.chromalab.feature.processing.peaks.PeakLabelTextClassification
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidate
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidateFlag
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidateStatus
import com.chromalab.feature.processing.peaks.RecoveredPeakIntegrationWindow
import com.chromalab.feature.reports.ChemicalInterpretationReport
import com.chromalab.feature.reports.ChromatogramIdentification
import com.chromalab.feature.reports.ChromatogramReport
import com.chromalab.feature.reports.ChromatographicQualityReport
import com.chromalab.feature.reports.ExecutedRuntime
import com.chromalab.feature.reports.GraphReport
import com.chromalab.feature.reports.GraphSourceMetadata
import com.chromalab.feature.reports.InputSourceType
import com.chromalab.feature.reports.KovatsIndexReport
import com.chromalab.feature.reports.ModelExecutionInfo
import com.chromalab.feature.reports.PeakBoundaryEvidence
import com.chromalab.feature.reports.PeakEvidence
import com.chromalab.feature.reports.PeakEvidenceAndRecoveryReport
import com.chromalab.feature.reports.PeakEvidenceStatus
import com.chromalab.feature.reports.PeakGateStatus
import com.chromalab.feature.reports.PeakMetricEvidence
import com.chromalab.feature.reports.PeakMetricEvidenceStatus
import com.chromalab.feature.reports.ReportAxisCalibration
import com.chromalab.feature.reports.ReportDoubleValue
import com.chromalab.feature.reports.ReportMetadata
import com.chromalab.feature.reports.ReportPeak
import com.chromalab.feature.reports.SignalAndBaselineReport
import com.chromalab.feature.reports.ReportGateStatus
import com.chromalab.feature.reports.ReportExportPrivacyClass
import com.chromalab.feature.reports.ReportMarkdownRenderer
import com.chromalab.feature.reports.ReportStageTiming
import com.chromalab.feature.reports.RuntimeFailureClass
import com.chromalab.feature.reports.RuntimeTerminalState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuntimeEvidencePackageValidatorTest {
    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }

    @Test
    fun validatorPassesCompleteRuntimePackage() {
        val evidence = runtimeEvidence()
        val report = reportWithRecovery(evidence)
        val packageJson = DebugPackageExporter.exportRuntimeEvidencePackage(report)
        val result = RuntimeEvidencePackageValidator.validateJson(packageJson, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.PASS, result.verdict)
        assertTrue(result.blockingIssues.isEmpty())
        assertEquals(1, result.graphSummaries.single().runtimeRecoveredPeaks)
        assertTrue(RuntimeEvidencePackageValidator.renderMarkdown(result).contains("Recovery Candidates"))
    }

    @Test
    fun builderPopulatesMultimodalEvidenceFromRuntimeTrace() {
        val evidencePackage = RuntimeEvidencePackageBuilder.build(reportWithRecovery(runtimeEvidence()))
        val graph = evidencePackage.graphs.single()
        val validation = RuntimeEvidencePackageValidator.validate(evidencePackage, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.PASS, validation.verdict)
        assertTrue(graph.stageJudgeResults.any { it.taskType == StageJudgeTaskType.GRAPH_PANEL_CANDIDATE_JUDGE })
        assertTrue(graph.stageJudgeResults.any { it.taskType == StageJudgeTaskType.PLOT_AREA_CANDIDATE_JUDGE })
        assertTrue(graph.stageJudgeResults.any { it.taskType == StageJudgeTaskType.OCR_CROP_READ })
        assertTrue(graph.stageJudgeResults.any { it.taskType == StageJudgeTaskType.TRACE_OVERLAY_JUDGE })
        assertTrue(graph.stageJudgeResults.any { it.taskType == StageJudgeTaskType.PEAK_EVIDENCE_JUDGE })
        assertEquals(1, graph.ocrVlmCropResults.size)
        assertEquals("crop.png", graph.ocrVlmCropResults.single().localCropPath)
        assertEquals(2, graph.runtimeOcrAnchorRows.size)
        assertEquals(2, validation.graphSummaries.single().runtimeOcrAnchorRows.size)
        assertTrue(graph.overlayJudgeResults.any { it.overlayImagePath == "selected_trace.png" })
    }

    @Test
    fun validatorFailsAcceptedRuntimeOcrAnchorWithoutDeterministicPixel() {
        val evidencePackage = RuntimeEvidencePackageBuilder.build(reportWithRecovery(runtimeEvidence()))
        val graph = evidencePackage.graphs.single()
        val brokenRow = graph.runtimeOcrAnchorRows.first().copy(
            pixelCoordinate = null,
        )

        val result = RuntimeEvidencePackageValidator.validate(
            evidencePackage.copy(
                graphs = listOf(graph.copy(runtimeOcrAnchorRows = listOf(brokenRow))),
            ),
            existingPaths::contains,
        )

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "runtime_ocr_anchor.accepted_pixel_geometry_missing" })
    }

    @Test
    fun validatorFailsAcceptedRuntimeOcrAnchorFromForbiddenScaleText() {
        val evidencePackage = RuntimeEvidencePackageBuilder.build(reportWithRecovery(runtimeEvidence()))
        val graph = evidencePackage.graphs.single()
        val brokenRow = graph.runtimeOcrAnchorRows.first().copy(
            rawText = "m/z 71",
        )

        val result = RuntimeEvidencePackageValidator.validate(
            evidencePackage.copy(
                graphs = listOf(graph.copy(runtimeOcrAnchorRows = listOf(brokenRow))),
            ),
            existingPaths::contains,
        )

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "runtime_ocr_anchor.forbidden_text_accepted" })
    }

    @Test
    fun validatorFailsRuntimeOcrAnchorWithoutCoordinateFrame() {
        val evidencePackage = RuntimeEvidencePackageBuilder.build(reportWithRecovery(runtimeEvidence()))
        val graph = evidencePackage.graphs.single()
        val brokenRow = graph.runtimeOcrAnchorRows.first().copy(
            coordinateFrame = null,
        )

        val result = RuntimeEvidencePackageValidator.validate(
            evidencePackage.copy(
                graphs = listOf(graph.copy(runtimeOcrAnchorRows = listOf(brokenRow))),
            ),
            existingPaths::contains,
        )

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "runtime_ocr_anchor.coordinate_frame_missing" })
    }

    @Test
    fun builderCarriesVlmRuntimeProfileAndForbiddenFieldRejections() {
        val vlmEvidence = runtimeEvidence().copy(
            source = PeakLabelEvidenceSource.VLM,
            warnings = listOf("peak_label_ocr.vlm_text_only_no_peak_metrics"),
            rejectedForbiddenFields = listOf(ForbiddenVlmNumericField.RT),
            runtimeProfile = runtimeProfile(),
        )
        val evidencePackage = RuntimeEvidencePackageBuilder.build(reportWithRecovery(vlmEvidence))
        val graph = evidencePackage.graphs.single()
        val validation = RuntimeEvidencePackageValidator.validate(evidencePackage, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.PASS, validation.verdict)
        assertEquals(listOf("runtime:vlm:crop:1"), evidencePackage.modelRuntimeProfiles.map { it.profileId })
        assertTrue(graph.stageJudgeResults.any { it.modelRuntimeProfileId == "runtime:vlm:crop:1" })
        assertTrue(graph.ocrVlmCropResults.any { ForbiddenVlmNumericField.RT in it.rejectedForbiddenFields })
    }

    @Test
    fun runtimeEvidenceExportsStructuredRuntimeDiagnosticsWithoutPrivatePath() {
        val diagnostic = StructuredRuntimeDiagnosticMapper.fromLiteRt(
            modelId = "gemma4-e2b",
            diagnostics = com.chromalab.feature.processing.inference.LiteRtRuntimeDiagnostics(
                backendName = "LiteRT GPU",
                performance = com.chromalab.feature.processing.inference.LiteRtPerformanceDiagnostic(
                    loadTimeMillis = 1200,
                    firstResponseLatencyMillis = 300,
                    totalResponseDurationMillis = 900,
                    timeoutMillis = 6000,
                    timedOut = false,
                    responseChars = 42,
                ),
            ),
            modelPath = "/data/user/0/com.chromalab.app.validation/files/models/gemma4-e2b/gemma.litertlm",
        )
        val evidencePackage = RuntimeEvidencePackageBuilder.build(
            report = reportWithRecovery(runtimeEvidence()),
            structuredRuntimeDiagnostics = listOf(diagnostic),
        )
        val encoded = json.encodeToString(evidencePackage)
        val validation = RuntimeEvidencePackageValidator.validate(evidencePackage, existingPaths::contains)
        val exportedDiagnostic = evidencePackage.structuredRuntimeDiagnostics
            .single { it.diagnosticId == diagnostic.diagnosticId }
        val validationRow = validation.structuredRuntimeRows
            .single { it.diagnosticId == diagnostic.diagnosticId }

        assertEquals(RuntimeEvidenceValidationVerdict.PASS, validation.verdict)
        assertEquals("VALIDATION_PACKAGE_PRIVATE_MODEL", exportedDiagnostic.modelPathClass.name)
        assertTrue(encoded.contains("\"modelPathClass\""))
        assertTrue(!encoded.contains("/data/user/0/com.chromalab.app.validation/files/models"))
        assertTrue(evidencePackage.structuredRuntimeDiagnostics.none { StructuredRuntimeDiagnosticMapper.containsPrivatePathLeak(it) })
        assertEquals("TECHNICAL_EVIDENCE", validationRow.privacyClass)
    }

    @Test
    fun runtimeEvidenceDerivesStructuredDiagnosticsFromModelAvailability() {
        val evidencePackage = RuntimeEvidencePackageBuilder.build(
            report = reportWithRecovery(runtimeEvidence()),
            modelAvailabilityDiagnostics = listOf(missingModelDiagnostic()),
        )

        assertTrue(evidencePackage.structuredRuntimeDiagnostics.any { it.source == RuntimeDiagnosticSource.MODEL_DISCOVERY })
        assertTrue(evidencePackage.structuredRuntimeDiagnostics.none { StructuredRuntimeDiagnosticMapper.containsPrivatePathLeak(it) })
    }

    @Test
    fun validatorBlocksStructuredRuntimeDiagnosticsMarkedAsUserReport() {
        val evidencePackage = RuntimeEvidencePackageBuilder.build(
            report = reportWithRecovery(runtimeEvidence()),
            structuredRuntimeDiagnostics = listOf(
                StructuredRuntimeDiagnostic(
                    diagnosticId = "runtime:test:user_report",
                    source = RuntimeDiagnosticSource.LITERT_LM,
                    modelId = "gemma4-e2b",
                    modelPathClass = RuntimeModelPathClass.APP_PRIVATE_MODEL,
                    backend = "LiteRT GPU",
                    loadAttempted = true,
                    loadResult = "loaded",
                    privacyClass = ReportExportPrivacyClass.USER_REPORT,
                ),
            ),
        )
        val validation = RuntimeEvidencePackageValidator.validate(evidencePackage, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, validation.verdict)
        assertTrue(validation.blockingIssues.any { it.code == "runtime_diagnostics.user_report_privacy_class" })
    }

    @Test
    fun userReportMarkdownDoesNotRenderStructuredRuntimeDiagnostics() {
        val report = reportWithRecovery(runtimeEvidence())
        RuntimeEvidencePackageBuilder.build(
            report = report,
            structuredRuntimeDiagnostics = listOf(
                StructuredRuntimeDiagnostic(
                    diagnosticId = "runtime:test:private",
                    source = RuntimeDiagnosticSource.LITERT_LM,
                    modelId = "gemma4-e2b",
                    modelPathClass = RuntimeModelPathClass.APP_PRIVATE_MODEL,
                    backend = "LiteRT GPU",
                    loadAttempted = true,
                    loadResult = "loaded",
                    safeUserReportSummary = "LiteRT runtime LiteRT GPU; MTP DISABLED.",
                ),
            ),
        )

        val markdown = ReportMarkdownRenderer.render(report)
        assertTrue(!markdown.contains("Structured Runtime Diagnostics"))
        assertTrue(!markdown.contains("LiteRT runtime LiteRT GPU; MTP DISABLED."))
    }

    @Test
    fun validatorFailsVlmPeakLabelEvidenceWithoutMultimodalRows() {
        val vlmEvidence = runtimeEvidence().copy(
            source = PeakLabelEvidenceSource.VLM,
            warnings = listOf("peak_label_ocr.vlm_text_only_no_peak_metrics"),
            runtimeProfile = runtimeProfile(),
        )
        val evidencePackage = RuntimeEvidencePackageBuilder.build(reportWithRecovery(vlmEvidence))
        val graph = evidencePackage.graphs.single()
        val broken = evidencePackage.copy(
            modelRuntimeProfiles = emptyList(),
            graphs = listOf(
                graph.copy(
                    stageJudgeResults = emptyList(),
                    ocrVlmCropResults = emptyList(),
                ),
            ),
        )

        val result = RuntimeEvidencePackageValidator.validate(broken, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "multimodal.vlm_crop_result_missing" })
        assertTrue(result.blockingIssues.any { it.code == "multimodal.vlm_stage_judge_missing" })
        assertTrue(result.blockingIssues.any { it.code == "multimodal.vlm_runtime_profile_missing" })
    }

    @Test
    fun validatorFailsFixtureHintEvidenceInRuntimePackage() {
        val fixtureHint = runtimeEvidence().copy(
            source = PeakLabelEvidenceSource.FIXTURE_HINT,
            isRuntimeEvidence = false,
            localCropPath = "fixture_hint.png",
        )
        val report = reportWithRecovery(
            evidence = fixtureHint,
            runtimeRecovered = emptyList(),
            testOnlyRecovered = listOf(recoveredCandidate(fixtureHint)),
        )
        val result = RuntimeEvidencePackageValidator.validate(
            RuntimeEvidencePackageBuilder.build(report),
            existingPaths::contains,
        )

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "ocr.fixture_hint_in_runtime_package" })
    }

    @Test
    fun validatorRequiresRecoveredPeaksToAvoidExistingPeakDuplicates() {
        val evidence = runtimeEvidence()
        val duplicate = recoveredCandidate(evidence).copy(
            labelRt = 3.890,
            nearestLocalMaximumRt = 3.890,
        )
        val report = reportWithRecovery(evidence, runtimeRecovered = listOf(duplicate))
        val result = RuntimeEvidencePackageValidator.validate(
            RuntimeEvidencePackageBuilder.build(report),
            existingPaths::contains,
        )

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "recovery.duplicate_existing_peak" })
    }

    @Test
    fun validatorChecksVlmCropEvidenceAsTextOnly() {
        val evidence = runtimeEvidence().copy(
            source = PeakLabelEvidenceSource.VLM,
            warnings = listOf("peak_label_ocr.vlm_text_only_no_peak_metrics"),
            runtimeProfile = runtimeProfile(),
        )
        val report = reportWithRecovery(evidence)
        val result = RuntimeEvidencePackageValidator.validate(
            RuntimeEvidencePackageBuilder.build(report),
            existingPaths::contains,
        )

        assertEquals(RuntimeEvidenceValidationVerdict.PASS, result.verdict)
        assertTrue(result.blockingIssues.none { it.code.startsWith("vlm.") })
    }

    @Test
    fun validatorFailsAutoValidPeakWithoutApexEvidence() {
        val evidence = runtimeEvidence()
        val report = reportWithRecovery(evidence)
        val graph = report.graphs.single()
        val brokenPeakEvidence = autoValidPeakEvidence().copy(
            apexPointIndex = null,
            localMaximumEvidence = false,
        )
        val brokenReport = report.copy(
            graphs = listOf(
                graph.copy(
                    peakRecovery = graph.peakRecovery.copy(
                        peakEvidenceTable = listOf(brokenPeakEvidence),
                    ),
                ),
            ),
        )

        val result = RuntimeEvidencePackageValidator.validate(
            RuntimeEvidencePackageBuilder.build(brokenReport),
            existingPaths::contains,
        )

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "peak_evidence.auto_valid_without_apex" })
    }

    @Test
    fun validatorFailsForbiddenVlmNumericOutput() {
        val packageWithVlm = RuntimeEvidencePackageBuilder.build(reportWithRecovery(runtimeEvidence()))
        val graph = packageWithVlm.graphs.single()
        val stage = vlmCropStage().copy(
            acceptedNumericFields = listOf(ForbiddenVlmNumericField.RT),
        )
        val result = RuntimeEvidencePackageValidator.validate(
            packageWithVlm.copy(
                modelRuntimeProfiles = listOf(runtimeProfile()),
                graphs = listOf(
                    graph.copy(
                        stageJudgeResults = listOf(stage),
                        ocrVlmCropResults = listOf(vlmCropResult()),
                    ),
                ),
            ),
            existingPaths::contains,
        )

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "multimodal.stage_numeric_field_accepted" })
    }

    @Test
    fun validatorRequiresVlmCropProvenance() {
        val packageWithVlm = RuntimeEvidencePackageBuilder.build(reportWithRecovery(runtimeEvidence()))
        val graph = packageWithVlm.graphs.single()
        val result = RuntimeEvidencePackageValidator.validate(
            packageWithVlm.copy(
                modelRuntimeProfiles = listOf(runtimeProfile()),
                graphs = listOf(
                    graph.copy(
                        stageJudgeResults = listOf(vlmCropStage()),
                        ocrVlmCropResults = listOf(vlmCropResult().copy(localCropPath = null)),
                    ),
                ),
            ),
            existingPaths::contains,
        )

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "multimodal.crop_path_missing" })
    }

    @Test
    fun validatorRecordsTimeoutProfile() {
        val packageWithVlm = RuntimeEvidencePackageBuilder.build(reportWithRecovery(runtimeEvidence()))
        val graph = packageWithVlm.graphs.single()
        val result = RuntimeEvidencePackageValidator.validate(
            packageWithVlm.copy(
                modelRuntimeProfiles = listOf(runtimeProfile().copy(timedOut = true, success = false, durationMillis = 6_000L)),
                graphs = listOf(
                    graph.copy(
                        stageJudgeResults = listOf(vlmCropStage().copy(verdict = StageJudgeVerdict.TIMEOUT)),
                        ocrVlmCropResults = listOf(vlmCropResult()),
                    ),
                ),
            ),
            existingPaths::contains,
        )

        assertTrue(result.blockingIssues.none { it.code.startsWith("multimodal.timeout") })
        assertEquals(StageJudgeVerdict.TIMEOUT.name, result.graphSummaries.single().stageJudgeRows.single().verdict)
    }

    @Test
    fun validatorFailsForbiddenRetryRecommendation() {
        val packageWithVlm = RuntimeEvidencePackageBuilder.build(reportWithRecovery(runtimeEvidence()))
        val graph = packageWithVlm.graphs.single()
        val result = RuntimeEvidencePackageValidator.validate(
            packageWithVlm.copy(
                modelRuntimeProfiles = listOf(runtimeProfile()),
                graphs = listOf(
                    graph.copy(
                        stageJudgeResults = listOf(
                            vlmCropStage().copy(
                                retryRecommendations = listOf(
                                    StageRetryRecommendation(
                                        action = StageRetryAction.CREATE_PEAK_FROM_TEXT,
                                        reason = "VLM cannot fabricate peaks.",
                                    ),
                                ),
                            ),
                        ),
                        ocrVlmCropResults = listOf(vlmCropResult()),
                    ),
                ),
            ),
            existingPaths::contains,
        )

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "multimodal.retry_forbidden_action" })
    }

    @Test
    fun validatorCatchesForbiddenKnowledgeUse() {
        val packageWithKnowledge = RuntimeEvidencePackageBuilder.build(reportWithRecovery(runtimeEvidence()))
        val graph = packageWithKnowledge.graphs.single()
        val context = KnowledgeRetrievalEngine.search(
            query = KnowledgeSearchQuery("knowledge cannot measure peak area"),
        )
        val result = RuntimeEvidencePackageValidator.validate(
            packageWithKnowledge.copy(
                knowledgePackVersion = CHROMALAB_KNOWLEDGE_PACK_VERSION_V1,
                knowledgeRetrievalContexts = listOf(context),
                graphs = listOf(
                    graph.copy(
                        knowledgeGroundedVlmOutputs = listOf(
                            KnowledgeGroundedVlmOutput(
                                outputId = "knowledge-output:forbidden",
                                taskId = "report-warning:1",
                                usedEntryIds = listOf("kp-safety-knowledge-cannot-measure"),
                                decision = "reject_metric",
                                confidence = 0.95f,
                                explanation = "Knowledge cannot measure peak area.",
                                attemptedUses = listOf("create_numeric_peak_metric"),
                                createdNumericPeakMetric = true,
                            ),
                        ),
                    ),
                ),
            ),
            existingPaths::contains,
        )

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "knowledge.created_numeric_peak_metric" })
    }

    @Test
    fun validatorMarksKnowledgeExplanationWithoutCitationsReview() {
        val packageWithKnowledge = RuntimeEvidencePackageBuilder.build(reportWithRecovery(runtimeEvidence()))
        val graph = packageWithKnowledge.graphs.single()
        val result = RuntimeEvidencePackageValidator.validate(
            packageWithKnowledge.copy(
                knowledgePackVersion = CHROMALAB_KNOWLEDGE_PACK_VERSION_V1,
                graphs = listOf(
                    graph.copy(
                        knowledgeGroundedVlmOutputs = listOf(
                            KnowledgeGroundedVlmOutput(
                                outputId = "knowledge-output:uncited",
                                taskId = "report-warning:2",
                                usedEntryIds = emptyList(),
                                decision = "explain_warning",
                                confidence = 0.6f,
                                explanation = "No Kovats can be reported.",
                            ),
                        ),
                    ),
                ),
            ),
            existingPaths::contains,
        )

        assertEquals(RuntimeEvidenceValidationVerdict.REVIEW, result.verdict)
        assertTrue(result.warnings.any { it.code == "knowledge.used_entry_ids_missing" })
    }

    @Test
    fun validatorFailsWhenTextSuppressionOverlayHasNoSuppressedBoxList() {
        val evidence = runtimeEvidence()
        val report = reportWithRecovery(
            evidence = evidence,
            suppressedTextBoxes = emptyList(),
        )
        val result = RuntimeEvidencePackageValidator.validate(
            RuntimeEvidencePackageBuilder.build(report),
            existingPaths::contains,
        )

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "curve.suppressed_text_boxes_missing" })
    }

    @Test
    fun validatorExportsJsonSummary() {
        val result = RuntimeEvidencePackageValidator.validate(
            RuntimeEvidencePackageBuilder.build(reportWithRecovery(runtimeEvidence())),
            existingPaths::contains,
        )
        val encoded = RuntimeEvidencePackageValidator.exportJson(result)

        assertTrue(encoded.contains("runtime-evidence-validation-1.0"))
        assertEquals(RuntimeEvidenceValidationVerdict.PASS, json.decodeFromString(RuntimeEvidenceValidationSummary.serializer(), encoded).verdict)
    }

    @Test
    fun validatorSupportsRoiFailurePackageWithEvidence() {
        val roiFailurePackage = RuntimeRoiFailureEvidencePackage(
            generatedAtEpochMillis = 123L,
            stageId = "GRAPH_ROI",
            failureReason = "No deterministic graph ROI candidate passed geometry checks.",
            originalImagePath = "original.png",
            normalizedImagePath = "normalized.png",
            graphPanelCandidates = listOf(
                GraphPanelBounds(
                    region = GraphRegion(10, 20, 120, 80),
                    candidateSource = GeometryCandidateSource.SCREENSHOT_EMBEDDED_CHART,
                    confidence = 0.72f,
                ),
            ),
            warnings = listOf("roi.failure.diagnostic_package"),
            timings = listOf(GeometryStageTiming("graph_panel.screenshot_embedded_detector", 42L)),
        )

        val encoded = json.encodeToString(RuntimeRoiFailureEvidencePackage.serializer(), roiFailurePackage)
        val result = RuntimeEvidencePackageValidator.validateJson(encoded, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertEquals("runtime-evidence-roi-failure-1.0", result.packageSchemaVersion)
        assertTrue(result.blockingIssues.isEmpty())
    }

    @Test
    fun validatorSupportsDiagnosticOnlyRuntimePackage() {
        val evidence = runtimeEvidence()
        val report = reportWithRecovery(
            evidence = evidence,
            geometryReportStatus = GeometryReportStatus.DIAGNOSTIC_ONLY,
        )

        val result = RuntimeEvidencePackageValidator.validate(
            RuntimeEvidencePackageBuilder.build(report),
            existingPaths::contains,
        )

        assertEquals(RuntimeEvidenceValidationVerdict.PASS, result.verdict)
        assertEquals(1, result.graphSummaries.single().runtimeRecoveredPeaks)
    }

    @Test
    fun builderAddsFailureClassForDiagnosticRuntimePackage() {
        val evidence = runtimeEvidence()
        val report = reportWithRecovery(
            evidence = evidence,
            geometryReportStatus = GeometryReportStatus.DIAGNOSTIC_ONLY,
        )

        val packageWithFailureClass = RuntimeEvidencePackageBuilder.build(report)

        assertEquals(RuntimeFailureClass.GRAPH_PANEL_FAILURE, packageWithFailureClass.runtimeFailureClass)
        assertEquals(
            packageWithFailureClass.runtimeFailureClass,
            packageWithFailureClass.reportContract.metadata.runtimeFailureClass,
        )
    }

    @Test
    fun builderPreservesExplicitModelUnavailableFailureClass() {
        val report = reportWithRecovery(
            evidence = runtimeEvidence(),
            geometryReportStatus = GeometryReportStatus.DIAGNOSTIC_ONLY,
        ).let { diagnostic ->
            diagnostic.copy(
                metadata = diagnostic.metadata.copy(
                    runtimeFailureClass = RuntimeFailureClass.VLM_MODEL_UNAVAILABLE,
                ),
            )
        }

        val packageWithFailureClass = RuntimeEvidencePackageBuilder.build(report)

        assertEquals(RuntimeFailureClass.VLM_MODEL_UNAVAILABLE, packageWithFailureClass.runtimeFailureClass)
        assertEquals(
            RuntimeFailureClass.VLM_MODEL_UNAVAILABLE,
            packageWithFailureClass.reportContract.metadata.runtimeFailureClass,
        )
    }

    @Test
    fun validatorFailsWhenModelUnavailablePreventsDeterministicFallback() {
        val report = reportWithRecovery(runtimeEvidence()).copy(
            metadata = reportWithRecovery(runtimeEvidence()).metadata.copy(
                detectedGraphCount = 0,
                executedModel = null,
                executedRuntime = ExecutedRuntime.UNKNOWN,
                runtimeFailureClass = RuntimeFailureClass.VLM_MODEL_UNAVAILABLE,
                stageTimings = listOf(ReportStageTiming("IMAGE_QUALITY", "IMAGE_QUALITY", 41L)),
            ),
            graphs = emptyList(),
        )
        val evidencePackage = RuntimeEvidencePackageBuilder.build(
            report = report,
            modelAvailabilityDiagnostics = listOf(missingModelDiagnostic()),
        )

        val result = RuntimeEvidencePackageValidator.validate(evidencePackage, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.modelAvailabilityRows.single().status == ModelAvailabilityStatus.NOT_CONFIGURED.name)
        assertTrue(result.blockingIssues.any { it.code == "package.deterministic_fallback_not_attempted" })
    }

    @Test
    fun validatorRecordsModelUnavailableAfterDeterministicFallbackAsWarning() {
        val report = reportWithRecovery(runtimeEvidence()).copy(
            metadata = reportWithRecovery(runtimeEvidence()).metadata.copy(
                executedModel = null,
                executedRuntime = ExecutedRuntime.UNKNOWN,
                stageTimings = listOf(
                    ReportStageTiming("IMAGE_QUALITY", "IMAGE_QUALITY", 41L),
                    ReportStageTiming("GRAPH_SELECTION", "GRAPH_SELECTION", 90L),
                ),
            ),
        )
        val evidencePackage = RuntimeEvidencePackageBuilder.build(
            report = report,
            modelAvailabilityDiagnostics = listOf(missingModelDiagnostic()),
        )

        val result = RuntimeEvidencePackageValidator.validate(evidencePackage, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.REVIEW, result.verdict)
        assertTrue(result.blockingIssues.none { it.code == "package.deterministic_fallback_not_attempted" })
        assertTrue(result.warnings.any { it.code == "package.executed_runtime_missing" })
    }

    @Test
    fun validatorFailsGraphStageFailureWithoutGraphFailurePackage() {
        val evidencePackage = RuntimeEvidencePackageBuilder.build(
            report = terminalGraphFailureReport(RuntimeFailureClass.TICK_LOCALIZATION_FAILURE),
            modelAvailabilityDiagnostics = listOf(missingModelDiagnostic()),
        )

        val result = RuntimeEvidencePackageValidator.validate(evidencePackage, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "graph_failure.package_missing" })
        assertTrue(result.blockingIssues.any { it.code == "package.graphs_missing" })
    }

    @Test
    fun validatorAcceptsGraphStageFailurePackageWithExplicitEvidence() {
        val evidencePackage = RuntimeEvidencePackageBuilder.build(
            report = terminalGraphFailureReport(RuntimeFailureClass.TICK_LOCALIZATION_FAILURE),
            modelAvailabilityDiagnostics = listOf(missingModelDiagnostic()),
            graphFailurePackages = listOf(tickLocalizationFailurePackage()),
        )

        val result = RuntimeEvidencePackageValidator.validate(evidencePackage, existingPaths::contains)

        assertTrue(result.blockingIssues.none { it.code == "graph_failure.package_missing" })
        assertTrue(result.blockingIssues.none { it.code == "package.graphs_missing" })
        assertEquals(1, result.graphFailureSummaries.single().graphIndex)
        assertEquals(3, result.graphFailureSummaries.single().xTickCandidateCount)
        assertEquals(1, result.graphFailureSummaries.single().acceptedYAnchorCount)
        assertEquals(
            CalibrationStrategyId.LEGACY_TICK_LOCALIZATION.name,
            result.graphFailureSummaries.single().selectedXStrategy,
        )
        assertEquals(
            CalibrationStrategyId.AXIS_SCALE_RESOLVER.name,
            result.graphFailureSummaries.single().selectedYStrategy,
        )
    }

    @Test
    fun validatorRequiresTickLocalizationSubreasonForTickFailures() {
        val badPackage = tickLocalizationFailurePackage().copy(
            tickSummary = tickLocalizationFailurePackage().tickSummary.copy(subreasons = emptyList()),
        )
        val evidencePackage = RuntimeEvidencePackageBuilder.build(
            report = terminalGraphFailureReport(RuntimeFailureClass.TICK_LOCALIZATION_FAILURE),
            modelAvailabilityDiagnostics = listOf(missingModelDiagnostic()),
            graphFailurePackages = listOf(badPackage),
        )

        val result = RuntimeEvidencePackageValidator.validate(evidencePackage, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "graph_failure.tick_subreason_missing" })
    }

    @Test
    fun validatorRequiresAxisScaleSubreasonForTickFailures() {
        val badPackage = tickLocalizationFailurePackage().copy(
            scaleSummary = tickLocalizationFailurePackage().scaleSummary.copy(subreasons = emptyList()),
        )
        val evidencePackage = RuntimeEvidencePackageBuilder.build(
            report = terminalGraphFailureReport(RuntimeFailureClass.TICK_LOCALIZATION_FAILURE),
            modelAvailabilityDiagnostics = listOf(missingModelDiagnostic()),
            graphFailurePackages = listOf(badPackage),
        )

        val result = RuntimeEvidencePackageValidator.validate(evidencePackage, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "graph_failure.axis_scale_subreason_missing" })
    }

    @Test
    fun validatorRequiresCalibrationStrategySummaryForGraphFailures() {
        val badPackage = tickLocalizationFailurePackage().copy(
            calibrationSummary = tickLocalizationFailurePackage().calibrationSummary.copy(
                selectedXStrategy = null,
                selectedYStrategy = null,
                strategyCount = 0,
            ),
        )
        val evidencePackage = RuntimeEvidencePackageBuilder.build(
            report = terminalGraphFailureReport(RuntimeFailureClass.TICK_LOCALIZATION_FAILURE),
            modelAvailabilityDiagnostics = listOf(missingModelDiagnostic()),
            graphFailurePackages = listOf(badPackage),
        )

        val result = RuntimeEvidencePackageValidator.validate(evidencePackage, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "graph_failure.calibration_strategy_summary_missing" })
    }

    @Test
    fun validatorRequiresRejectedCalibrationStrategyEvidenceForGraphFailures() {
        val badPackage = tickLocalizationFailurePackage().copy(
            calibrationSummary = tickLocalizationFailurePackage().calibrationSummary.copy(
                rejectedStrategyIds = emptyList(),
            ),
        )
        val evidencePackage = RuntimeEvidencePackageBuilder.build(
            report = terminalGraphFailureReport(RuntimeFailureClass.TICK_LOCALIZATION_FAILURE),
            modelAvailabilityDiagnostics = listOf(missingModelDiagnostic()),
            graphFailurePackages = listOf(badPackage),
        )

        val result = RuntimeEvidencePackageValidator.validate(evidencePackage, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "graph_failure.calibration_strategy_summary_missing" })
    }

    @Test
    fun validatorRejectsAcceptedOcrTickWithoutDeterministicPixel() {
        val badPackage = tickLocalizationFailurePackage().copy(
            ocrSummary = tickLocalizationFailurePackage().ocrSummary.copy(
                acceptedAnchors = listOf(
                    RuntimeTickAnchorEvidenceSummary(
                        axis = GeometryAxis.Y,
                        tickPixelPosition = null,
                        rawText = "400000",
                        parsedNumericValue = 400000.0,
                        localCropPath = "crop.png",
                        status = com.chromalab.feature.processing.geometry.TickOcrItemStatus.ACCEPTED,
                    ),
                ),
            ),
        )
        val evidencePackage = RuntimeEvidencePackageBuilder.build(
            report = terminalGraphFailureReport(RuntimeFailureClass.OCR_TICK_FAILURE),
            modelAvailabilityDiagnostics = listOf(missingModelDiagnostic()),
            graphFailurePackages = listOf(badPackage),
        )

        val result = RuntimeEvidencePackageValidator.validate(evidencePackage, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "graph_failure.ocr_tick_without_deterministic_pixel" })
    }

    @Test
    fun validatorRequiresFailureClassForNonPassRuntimePackage() {
        val packageWithFailureClass = RuntimeEvidencePackageBuilder.build(
            reportWithRecovery(
                evidence = runtimeEvidence(),
                geometryReportStatus = GeometryReportStatus.DIAGNOSTIC_ONLY,
            ),
        )
        val broken = packageWithFailureClass.copy(
            runtimeFailureClass = null,
            reportContract = packageWithFailureClass.reportContract.copy(
                metadata = packageWithFailureClass.reportContract.metadata.copy(runtimeFailureClass = null),
            ),
        )

        val result = RuntimeEvidencePackageValidator.validate(broken, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "package.failure_class_missing" })
    }

    @Test
    fun validatorRejectsFailureClassOnPassRuntimePackage() {
        val complete = RuntimeEvidencePackageBuilder.build(reportWithRecovery(runtimeEvidence()))
        val broken = complete.copy(
            terminalState = RuntimeTerminalState.PASS,
            reportGateStatus = ReportGateStatus.RELEASE_READY,
            runtimeFailureClass = RuntimeFailureClass.CALIBRATION_FAILURE,
            reportContract = complete.reportContract.copy(
                metadata = complete.reportContract.metadata.copy(
                    runtimeFailureClass = RuntimeFailureClass.CALIBRATION_FAILURE,
                ),
            ),
        )

        val result = RuntimeEvidencePackageValidator.validate(broken, existingPaths::contains)

        assertEquals(RuntimeEvidenceValidationVerdict.FAIL, result.verdict)
        assertTrue(result.blockingIssues.any { it.code == "package.failure_class_on_pass" })
    }

    private fun reportWithRecovery(
        evidence: PeakLabelEvidence,
        runtimeRecovered: List<RecoveredPeakCandidate> = listOf(recoveredCandidate(evidence)),
        testOnlyRecovered: List<RecoveredPeakCandidate> = emptyList(),
        suppressedTextBoxes: List<CurveMaskTextSuppressionRegion> = listOf(
            CurveMaskTextSuppressionRegion(
                region = GraphRegion(20, 24, 24, 10),
                classification = PeakLabelTextClassification.PEAK_ANNOTATION.name,
                source = evidence.source.name,
                reason = "suppress_peak_annotation_text_before_curve_mask",
            ),
        ),
        geometryReportStatus: GeometryReportStatus = GeometryReportStatus.SCIENTIFIC_READY,
    ): ChromatogramReport {
        val trace = GeometryTrace(
            originalImagePath = "original.png",
            normalizedImagePath = "normalized.png",
            selectedGraphPanelOverlayPath = "graph_panel.png",
            selectedPlotAreaOverlayPath = "plot_area.png",
            axisOverlayPath = "axis.png",
            tickOverlayPath = "tick.png",
            calibrationFitOverlayPath = "calibration.png",
            plotAreaCropPath = "plot_crop.png",
            curveMaskRawPath = "mask_raw.png",
            curveMaskCleanPath = "mask_clean.png",
            curveTextSuppressionOverlayPath = "text_suppression.png",
            curveTextSuppressionRegions = suppressedTextBoxes,
            curveSelectedComponentPath = "selected_trace.png",
            curveSkeletonPath = "skeleton.png",
            finalCenterlineOverlayPath = "final_centerline.png",
            peakLabelCropPaths = listOf("crop.png"),
            peakLabelCropBoundsOverlayPath = "crop_bounds.png",
            peakLabelTextClassificationOverlayPath = "text_classification.png",
            peakLabelEvidence = listOf(evidence),
            runtimeOcrAnchorRows = runtimeOcrAnchorRows(),
            xCalibrationFit = AxisCalibrationFit(GeometryAxis.X, status = CalibrationFitStatus.VALID),
            yCalibrationFit = AxisCalibrationFit(GeometryAxis.Y, status = CalibrationFitStatus.VALID),
        )
        return ChromatogramReport(
            metadata = ReportMetadata(
                reportId = "runtime-validation-test",
                inputSourceType = InputSourceType.CAMERA_CAPTURE,
                detectedGraphCount = 1,
                selectedModel = ModelExecutionInfo("gemma-litert", "Gemma LiteRT", ExecutedRuntime.LITERT),
                executedModel = ModelExecutionInfo("gemma-litert", "Gemma LiteRT", ExecutedRuntime.LITERT),
                executedRuntime = ExecutedRuntime.LITERT,
                deviceName = "Android test device",
            ),
            graphs = listOf(
                GraphReport(
                    graphIndex = 1,
                    source = GraphSourceMetadata(
                        geometryReportStatus = geometryReportStatus,
                        geometryTrace = trace,
                    ),
                    identification = ChromatogramIdentification(),
                    axisCalibration = ReportAxisCalibration(
                        xCalibrationFit = AxisCalibrationFit(GeometryAxis.X, status = CalibrationFitStatus.VALID),
                        yCalibrationFit = AxisCalibrationFit(GeometryAxis.Y, status = CalibrationFitStatus.VALID),
                    ),
                    signal = SignalAndBaselineReport(),
                    peaks = listOf(
                        ReportPeak(
                            number = 1,
                            retentionTime = ReportDoubleValue.calculated(3.890, "min"),
                        ),
                    ),
                    peakRecovery = PeakEvidenceAndRecoveryReport(
                        rawDetectedPeaks = 1,
                        validatedPeaks = 1,
                        peakEvidenceTable = listOf(autoValidPeakEvidence()),
                        reviewPeaks = 0,
                        rejectedPeaks = 0,
                        userConfirmedPeaks = 0,
                        userEditedPeaks = 0,
                        runtimeRecoveredPeaks = runtimeRecovered,
                        testOnlyRecoveredPeaks = testOnlyRecovered,
                        rejectedRecoveredCandidates = emptyList(),
                        productionReportablePeaks = 1 + runtimeRecovered.count { it.isProductionReportable },
                        reviewGradePeaks = runtimeRecovered.count { it.status == RecoveredPeakCandidateStatus.REVIEW },
                        labelEvidence = listOf(evidence),
                    ),
                    quality = ChromatographicQualityReport(totalDetectedPeaks = 1),
                    kovats = KovatsIndexReport(),
                    interpretation = ChemicalInterpretationReport(),
                ),
            ),
        )
    }

    private fun terminalGraphFailureReport(failureClass: RuntimeFailureClass): ChromatogramReport =
        reportWithRecovery(runtimeEvidence()).let { report ->
            report.copy(
                metadata = report.metadata.copy(
                    detectedGraphCount = 0,
                    selectedModel = null,
                    executedModel = null,
                    executedRuntime = ExecutedRuntime.UNKNOWN,
                    runtimeFailureClass = failureClass,
                    stageTimings = listOf(
                        ReportStageTiming("GRAPH_SELECTION", "GRAPH_SELECTION", 100L),
                        ReportStageTiming("GRAPH_ROI", "GRAPH_ROI", 20L),
                        ReportStageTiming("AXIS_DETECTION", "AXIS_DETECTION", 12L),
                        ReportStageTiming("OCR_SUGGESTION", "OCR_SUGGESTION", 8L),
                        ReportStageTiming("Y_CALIBRATION", "Y_CALIBRATION", 4L),
                    ),
                ),
                graphs = emptyList(),
            )
        }

    private fun tickLocalizationFailurePackage(): RuntimeGraphFailurePackage =
        RuntimeGraphFailurePackage(
            graphIndex = 1,
            failureClass = RuntimeFailureClass.TICK_LOCALIZATION_FAILURE,
            failureStage = "Y_CALIBRATION",
            failureReason = "Automatic axis calibration failed: at least two Y tick labels are required.",
            graphPanelBounds = GraphRegion(0, 0, 100, 100),
            plotAreaBounds = GraphRegion(10, 20, 80, 60),
            axisSummary = RuntimeAxisFailureSummary(
                xAxisLineAvailable = true,
                yAxisLineAvailable = true,
                originAvailable = true,
                axisConfidence = 0.72f,
            ),
            tickSummary = RuntimeTickFailureSummary(
                sourceMethod = "deterministic_cv_projection",
                xTickCandidateCount = 3,
                yTickCandidateCount = 1,
                xTickPixelPositions = listOf(20f, 50f, 80f),
                yTickPixelPositions = listOf(40f),
                readyForOcrValueMatching = false,
                subreasons = listOf(com.chromalab.feature.processing.geometry.TickLocalizationFailureSubreason.INSUFFICIENT_Y_ANCHORS),
                warnings = listOf("axis_tick_geometry.y_tick_positions_insufficient"),
            ),
            scaleSummary = RuntimeAxisScaleFailureSummary(
                status = CalibrationFitStatus.INVALID,
                xAnchorCount = 3,
                yAnchorCount = 1,
                rejectedAnchorCount = 1,
                xEvidenceTypes = listOf(AxisScaleEvidenceType.EXPLICIT_TICK_MARK),
                yEvidenceTypes = listOf(AxisScaleEvidenceType.EXPLICIT_TICK_MARK),
                subreasons = listOf(AxisScaleFailureSubreason.INSUFFICIENT_SCALE_ANCHORS),
                warnings = listOf("axis_scale.insufficient_scale_anchors"),
            ),
            ocrSummary = RuntimeTickOcrFailureSummary(
                rawElementCount = 4,
                numericElementCount = 4,
                acceptedXAnchorCount = 3,
                acceptedYAnchorCount = 1,
                acceptedAnchors = listOf(
                    RuntimeTickAnchorEvidenceSummary(
                        axis = GeometryAxis.X,
                        tickPixelPosition = 20f,
                        rawText = "10.00",
                        parsedNumericValue = 10.0,
                        localCropPath = "crop.png",
                        status = com.chromalab.feature.processing.geometry.TickOcrItemStatus.ACCEPTED,
                    ),
                    RuntimeTickAnchorEvidenceSummary(
                        axis = GeometryAxis.Y,
                        tickPixelPosition = 40f,
                        rawText = "400000",
                        parsedNumericValue = 400000.0,
                        localCropPath = "crop.png",
                        status = com.chromalab.feature.processing.geometry.TickOcrItemStatus.ACCEPTED,
                    ),
                ),
                rejectedAnchors = listOf(
                    RuntimeTickAnchorEvidenceSummary(
                        axis = GeometryAxis.Y,
                        rawText = "71.00",
                        parsedNumericValue = 71.0,
                        status = com.chromalab.feature.processing.geometry.TickOcrItemStatus.SEMANTIC_ONLY,
                        rejectionReason = "tick_ocr.numeric_value_without_deterministic_tick_position",
                    ),
                ),
            ),
            runtimeOcrAnchorRows = runtimeOcrAnchorRows(),
            calibrationSummary = RuntimeCalibrationFailureSummary(
                xStatus = CalibrationFitStatus.REVIEW,
                yStatus = CalibrationFitStatus.INVALID,
                selectedXStrategy = CalibrationStrategyId.LEGACY_TICK_LOCALIZATION,
                selectedYStrategy = CalibrationStrategyId.AXIS_SCALE_RESOLVER,
                strategyCount = 6,
                rejectedStrategyIds = listOf(
                    CalibrationStrategyId.AXIS_SCALE_RESOLVER,
                    CalibrationStrategyId.OCR_LABEL_BOX_DIRECT_FIT,
                ),
                xAcceptedAnchorCount = 3,
                yAcceptedAnchorCount = 1,
                yRejectedAnchorCount = 1,
                yWarnings = listOf("calibration.y.not_enough_anchors"),
            ),
            artifactPaths = RuntimeGraphFailureArtifactPaths(
                originalImagePath = "original.png",
                normalizedImagePath = "normalized.png",
                graphPanelOverlayPath = "graph_panel.png",
                plotAreaOverlayPath = "plot_area.png",
                axisOverlayPath = "axis.png",
                tickOverlayPath = "tick.png",
                calibrationOverlayPath = "calibration.png",
                ocrCropPaths = listOf("crop.png"),
            ),
            rejectionReasons = listOf("tick_ocr.numeric_value_without_deterministic_tick_position"),
        )

    private fun runtimeOcrAnchorRows(): List<RuntimeOcrAnchorBridgeRow> =
        listOf(
            RuntimeOcrAnchorBridgeRow(
                runtimeRowId = "runtime-ocr-anchor:1:1",
                graphId = "graph:1",
                graphIndex = 1,
                axis = GeometryAxis.X,
                rawText = "10.00",
                parsedNumericValue = 10.0,
                pixelCoordinate = 20f,
                coordinateFrame = RuntimeOcrAnchorCoordinateFrame.PLOT_RELATIVE,
                sourceCropRef = "crop:crop.png",
                sourceCropPath = "crop.png",
                cropFileAvailable = true,
                confidence = 0.88f,
                geometrySource = AxisScaleEvidenceType.EXPLICIT_TICK_MARK,
                numericSource = "LOCAL_TICK_CROP_OCR",
                status = TickOcrItemStatus.ACCEPTED,
            ),
            RuntimeOcrAnchorBridgeRow(
                runtimeRowId = "runtime-ocr-anchor:1:2",
                graphId = "graph:1",
                graphIndex = 1,
                axis = GeometryAxis.Y,
                rawText = "71.00",
                parsedNumericValue = 71.0,
                pixelCoordinate = null,
                coordinateFrame = RuntimeOcrAnchorCoordinateFrame.IMAGE_ABSOLUTE,
                sourceCropRef = "graph:1:Y:no_pixel",
                sourceCropPath = null,
                cropFileAvailable = false,
                cropMissingReason = "runtime_ocr_anchor.crop_path_missing",
                confidence = 0.51f,
                geometrySource = AxisScaleEvidenceType.OCR_VALUE_ONLY_REJECTED,
                numericSource = "LOCAL_OCR_TEXT",
                status = TickOcrItemStatus.SEMANTIC_ONLY,
                rejectionReason = "tick_ocr.numeric_value_without_deterministic_tick_position",
            ),
        )

    private fun runtimeEvidence(): PeakLabelEvidence =
        PeakLabelEvidence(
            rawText = "5.610",
            normalizedText = "5.610",
            parsedRetentionTime = 5.610,
            labelBoxPx = GraphRegion(20, 24, 24, 10),
            cropBoundsPx = GraphRegion(10, 20, 80, 40),
            linkedGraphPanelBounds = GraphRegion(0, 0, 100, 100),
            linkedPlotAreaBounds = GraphRegion(10, 20, 80, 60),
            localCropPath = "crop.png",
            source = PeakLabelEvidenceSource.ML_KIT,
            confidence = 0.84f,
            status = PeakLabelEvidenceStatus.VALID_TEXT,
            textClassification = PeakLabelTextClassification.PEAK_ANNOTATION,
            isRuntimeEvidence = true,
        )

    private fun recoveredCandidate(evidence: PeakLabelEvidence): RecoveredPeakCandidate =
        RecoveredPeakCandidate(
            sourceEvidenceId = "label:${evidence.parsedRetentionTime}",
            labelRt = evidence.parsedRetentionTime ?: 5.610,
            nearestLocalMaximumRt = evidence.parsedRetentionTime ?: 5.610,
            rtDelta = 0.0,
            localHeight = 12.0,
            localSNR = 5.0,
            localProminence = 8.0,
            localCurvatureScore = 0.75,
            integrationWindow = RecoveredPeakIntegrationWindow(5.55, 5.68),
            sourceEvidence = evidence,
            status = RecoveredPeakCandidateStatus.REVIEW,
            flags = listOf(
                RecoveredPeakCandidateFlag.LOW_RESOLUTION_RECOVERED,
                RecoveredPeakCandidateFlag.LABEL_EVIDENCE_VERIFIED,
                RecoveredPeakCandidateFlag.RUNTIME_OCR_VERIFIED,
            ),
        )

    private fun autoValidPeakEvidence(): PeakEvidence =
        PeakEvidence(
            evidenceId = "calculation:test:peak:1",
            peakId = "1",
            peakNumber = 1,
            status = PeakEvidenceStatus.AUTO_VALID,
            gateStatus = PeakGateStatus.VALID,
            retentionTime = PeakMetricEvidence.calculated(3.890, "min"),
            apexPointIndex = 15,
            localMaximumEvidence = true,
            height = PeakMetricEvidence.calculated(100.0, "a.u."),
            area = PeakMetricEvidence.calculated(300.0),
            boundaryEvidence = PeakBoundaryEvidence(
                startRetentionTime = PeakMetricEvidence.calculated(3.80, "min"),
                endRetentionTime = PeakMetricEvidence.calculated(3.95, "min"),
                status = PeakMetricEvidenceStatus.CALCULATED,
            ),
        )

    private fun runtimeProfile(): ModelRuntimeProfile =
        ModelRuntimeProfile(
            profileId = "runtime:vlm:crop:1",
            taskId = "stage:vlm_crop:1",
            modelId = "gemma-litert",
            runtimeBackend = "LITERT",
            cropWidth = 64,
            cropHeight = 32,
            durationMillis = 120L,
            timeoutMillis = 6_000L,
            timedOut = false,
            success = true,
            cacheHit = false,
        )

    private fun missingModelDiagnostic(): ModelAvailabilityDiagnostic =
        ModelAvailabilityDiagnostic(
            diagnosticId = "model-availability:test",
            mode = ModelAvailabilityMode.VALIDATION_FIXTURE,
            expectedBackend = "Gemma-4-E4B LiteRT-LM FULL_ANALYSIS primary",
            loadAttempted = true,
            loadResult = "not_loaded",
            sanitizedErrorMessage = "Chromatogram VLM is unavailable; deterministic fallback must continue.",
            fallbackModelAttempted = false,
            fallbackResult = "unavailable",
            status = ModelAvailabilityStatus.NOT_CONFIGURED,
            timestampEpochMillis = 123L,
        )

    private fun vlmCropStage(): AutonomousStageJudgeResult =
        AutonomousStageJudgeResult(
            taskId = "stage:vlm_crop:1",
            graphIndex = 1,
            taskType = StageJudgeTaskType.OCR_CROP_READ,
            source = StageJudgeSource.VLM,
            verdict = StageJudgeVerdict.PASS,
            cropPath = "crop.png",
            modelRuntimeProfileId = "runtime:vlm:crop:1",
            ocrCropResultIds = listOf("ocr-vlm:1"),
            rejectedForbiddenFields = listOf(ForbiddenVlmNumericField.RT),
        )

    private fun vlmCropResult(): VlmOcrCropResult =
        VlmOcrCropResult(
            resultId = "ocr-vlm:1",
            taskId = "stage:vlm_crop:1",
            source = StageJudgeSource.VLM,
            localCropPath = "crop.png",
            rawText = "5.610",
            normalizedText = "5.610",
            parsedText = "5.610",
            textClass = MultimodalTextRegionClass.PEAK_ANNOTATION,
            rejectedForbiddenFields = listOf(ForbiddenVlmNumericField.RT),
            runtimeProfileId = "runtime:vlm:crop:1",
        )

    private val existingPaths = setOf(
        "original.png",
        "normalized.png",
        "graph_panel.png",
        "plot_area.png",
        "axis.png",
        "tick.png",
        "calibration.png",
        "plot_crop.png",
        "mask_raw.png",
        "mask_clean.png",
        "text_suppression.png",
        "selected_trace.png",
        "skeleton.png",
        "final_centerline.png",
        "crop.png",
        "crop_bounds.png",
        "text_classification.png",
    )
}

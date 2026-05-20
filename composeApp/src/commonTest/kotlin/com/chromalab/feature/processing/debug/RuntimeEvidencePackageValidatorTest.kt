package com.chromalab.feature.processing.debug

import com.chromalab.feature.processing.curve.CurveMaskTextSuppressionRegion
import com.chromalab.feature.processing.geometry.AxisCalibrationFit
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryAxis
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import com.chromalab.feature.processing.geometry.GeometryStageTiming
import com.chromalab.feature.processing.geometry.GeometryTrace
import com.chromalab.feature.processing.geometry.GraphPanelBounds
import com.chromalab.feature.processing.geometry.GeometryCandidateSource
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

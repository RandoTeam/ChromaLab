package com.chromalab.feature.processing.debug

import com.chromalab.feature.processing.curve.CurveMaskTextSuppressionRegion
import com.chromalab.feature.knowledge.KnowledgeGroundedVlmOutput
import com.chromalab.feature.knowledge.KnowledgeRetrievalContext
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.CalibrationStrategyId
import com.chromalab.feature.processing.geometry.AxisScaleEvidenceType
import com.chromalab.feature.processing.geometry.AxisScaleFailureSubreason
import com.chromalab.feature.processing.geometry.GeometryAxis
import com.chromalab.feature.processing.geometry.GraphLayoutClass
import com.chromalab.feature.processing.geometry.GraphMultiplicityResolution
import com.chromalab.feature.processing.geometry.GeometryStageTiming
import com.chromalab.feature.processing.geometry.TickLocalizationFailureSubreason
import com.chromalab.feature.processing.geometry.TickOcrItemStatus
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.multimodal.AutonomousStageJudgeResult
import com.chromalab.feature.processing.multimodal.ModelRuntimeProfile
import com.chromalab.feature.processing.multimodal.MultimodalTextRegionClass
import com.chromalab.feature.processing.multimodal.OcrVlmDisagreement
import com.chromalab.feature.processing.multimodal.OverlayJudgeResult
import com.chromalab.feature.processing.multimodal.StageJudgeConfidence
import com.chromalab.feature.processing.multimodal.VlmOcrCropResult
import com.chromalab.feature.processing.multimodal.StageJudgeSource
import com.chromalab.feature.processing.multimodal.StageJudgeTaskType
import com.chromalab.feature.processing.multimodal.StageJudgeVerdict
import com.chromalab.feature.processing.model.ModelAvailabilityDiagnostic
import com.chromalab.feature.processing.peaks.PeakLabelEvidence
import com.chromalab.feature.processing.peaks.PeakLabelEvidenceSource
import com.chromalab.feature.processing.peaks.PeakLabelTextClassification
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidate
import com.chromalab.feature.reports.ChromatogramReport
import com.chromalab.feature.reports.EvidenceGateStatus
import com.chromalab.feature.reports.GateEvidence
import com.chromalab.feature.reports.GraphReport
import com.chromalab.feature.reports.PeakEvidence
import com.chromalab.feature.reports.ReportWarning
import com.chromalab.feature.reports.ReportGateStatus
import com.chromalab.feature.reports.ReportReleaseGateEvaluator
import com.chromalab.feature.reports.ReportStageTiming
import com.chromalab.feature.reports.RuntimeFailureClass
import com.chromalab.feature.reports.RuntimeTerminalState
import kotlinx.serialization.Serializable

@Serializable
data class RuntimeEvidencePackage(
    val schemaVersion: String = "runtime-evidence-1.2",
    val generatedAtEpochMillis: Long,
    val reportId: String,
    val sourceName: String? = null,
    val deviceName: String? = null,
    val selectedModelId: String? = null,
    val executedModelId: String? = null,
    val executedRuntime: String,
    val terminalState: RuntimeTerminalState = RuntimeTerminalState.DIAGNOSTIC_ONLY,
    val reportGateStatus: ReportGateStatus = ReportGateStatus.DIAGNOSTIC_ONLY,
    val runtimeFailureClass: RuntimeFailureClass? = null,
    val gateEvidence: GateEvidence = GateEvidence(),
    val modelAvailabilityDiagnostics: List<ModelAvailabilityDiagnostic> = emptyList(),
    val modelRuntimeProfiles: List<ModelRuntimeProfile> = emptyList(),
    val knowledgePackVersion: String? = null,
    val knowledgeRetrievalContexts: List<KnowledgeRetrievalContext> = emptyList(),
    val graphFailurePackages: List<RuntimeGraphFailurePackage> = emptyList(),
    val graphs: List<RuntimeEvidenceGraphPackage>,
    val reportContract: ChromatogramReport,
)

@Serializable
data class RuntimeGraphFailurePackage(
    val schemaVersion: String = "runtime-graph-failure-1.0",
    val graphIndex: Int,
    val failureClass: RuntimeFailureClass,
    val failureStage: String,
    val failureReason: String,
    val layoutClass: GraphLayoutClass? = null,
    val layoutPhysicalGraphCount: Int? = null,
    val graphPanelBounds: GraphRegion? = null,
    val graphPanelMissingReason: String? = null,
    val plotAreaBounds: GraphRegion? = null,
    val plotAreaMissingReason: String? = null,
    val axisSummary: RuntimeAxisFailureSummary = RuntimeAxisFailureSummary(),
    val tickSummary: RuntimeTickFailureSummary = RuntimeTickFailureSummary(),
    val scaleSummary: RuntimeAxisScaleFailureSummary = RuntimeAxisScaleFailureSummary(),
    val ocrSummary: RuntimeTickOcrFailureSummary = RuntimeTickOcrFailureSummary(),
    val calibrationSummary: RuntimeCalibrationFailureSummary = RuntimeCalibrationFailureSummary(),
    val artifactPaths: RuntimeGraphFailureArtifactPaths = RuntimeGraphFailureArtifactPaths(),
    val rejectionReasons: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val stageTimings: List<ReportStageTiming> = emptyList(),
)

@Serializable
data class RuntimeGraphFailureArtifactPaths(
    val originalImagePath: String? = null,
    val normalizedImagePath: String? = null,
    val rectifiedImagePath: String? = null,
    val graphPanelOverlayPath: String? = null,
    val graphPanelOverlayMissingReason: String? = null,
    val plotAreaOverlayPath: String? = null,
    val plotAreaOverlayMissingReason: String? = null,
    val axisOverlayPath: String? = null,
    val axisOverlayMissingReason: String? = null,
    val tickOverlayPath: String? = null,
    val tickOverlayMissingReason: String? = null,
    val calibrationOverlayPath: String? = null,
    val calibrationOverlayMissingReason: String? = null,
    val ocrCropPaths: List<String> = emptyList(),
    val ocrCropMissingReason: String? = null,
)

@Serializable
data class RuntimeAxisFailureSummary(
    val xAxisLineAvailable: Boolean = false,
    val yAxisLineAvailable: Boolean = false,
    val originAvailable: Boolean = false,
    val axisConfidence: Float = 0f,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class RuntimeTickFailureSummary(
    val sourceMethod: String = "deterministic_cv_unavailable",
    val xTickCandidateCount: Int = 0,
    val yTickCandidateCount: Int = 0,
    val xTickPixelPositions: List<Float> = emptyList(),
    val yTickPixelPositions: List<Float> = emptyList(),
    val readyForOcrValueMatching: Boolean = false,
    val subreasons: List<TickLocalizationFailureSubreason> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class RuntimeAxisScaleFailureSummary(
    val status: CalibrationFitStatus? = null,
    val xAnchorCount: Int = 0,
    val yAnchorCount: Int = 0,
    val rejectedAnchorCount: Int = 0,
    val xEvidenceTypes: List<AxisScaleEvidenceType> = emptyList(),
    val yEvidenceTypes: List<AxisScaleEvidenceType> = emptyList(),
    val subreasons: List<AxisScaleFailureSubreason> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class RuntimeTickOcrFailureSummary(
    val rawElementCount: Int = 0,
    val numericElementCount: Int = 0,
    val acceptedXAnchorCount: Int = 0,
    val acceptedYAnchorCount: Int = 0,
    val semanticOnlyCount: Int = 0,
    val acceptedAnchors: List<RuntimeTickAnchorEvidenceSummary> = emptyList(),
    val rejectedAnchors: List<RuntimeTickAnchorEvidenceSummary> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class RuntimeTickAnchorEvidenceSummary(
    val axis: GeometryAxis,
    val tickPixelPosition: Float? = null,
    val rawText: String,
    val parsedNumericValue: Double? = null,
    val localCropPath: String? = null,
    val confidence: Float = 0f,
    val status: TickOcrItemStatus,
    val rejectionReason: String? = null,
)

@Serializable
data class RuntimeCalibrationFailureSummary(
    val xStatus: CalibrationFitStatus? = null,
    val yStatus: CalibrationFitStatus? = null,
    val selectedXStrategy: CalibrationStrategyId? = null,
    val selectedYStrategy: CalibrationStrategyId? = null,
    val strategyCount: Int = 0,
    val rejectedStrategyIds: List<CalibrationStrategyId> = emptyList(),
    val xAcceptedAnchorCount: Int = 0,
    val yAcceptedAnchorCount: Int = 0,
    val xRejectedAnchorCount: Int = 0,
    val yRejectedAnchorCount: Int = 0,
    val xMaxResidualPx: Double? = null,
    val yMaxResidualPx: Double? = null,
    val xRmsePx: Double? = null,
    val yRmsePx: Double? = null,
    val xWarnings: List<String> = emptyList(),
    val yWarnings: List<String> = emptyList(),
    val missingReason: String? = null,
)

@Serializable
data class RuntimeEvidenceGraphPackage(
    val graphIndex: Int,
    val artifactPaths: RuntimeEvidenceArtifactPaths,
    val stageJudgeResults: List<AutonomousStageJudgeResult> = emptyList(),
    val ocrVlmCropResults: List<VlmOcrCropResult> = emptyList(),
    val ocrVlmDisagreements: List<OcrVlmDisagreement> = emptyList(),
    val overlayJudgeResults: List<OverlayJudgeResult> = emptyList(),
    val knowledgeGroundedVlmOutputs: List<KnowledgeGroundedVlmOutput> = emptyList(),
    val peakEvidenceTable: List<PeakEvidence> = emptyList(),
    val peakLabelEvidence: List<PeakLabelEvidence>,
    val runtimeRecoveredPeaks: List<RecoveredPeakCandidate>,
    val testOnlyRecoveredPeaks: List<RecoveredPeakCandidate>,
    val rejectedRecoveredCandidates: List<RecoveredPeakCandidate>,
    val suppressedTextBoxes: List<CurveMaskTextSuppressionRegion> = emptyList(),
    val multiplicityResolution: GraphMultiplicityResolution? = null,
    val summaryCounts: RuntimeEvidenceSummaryCounts,
    val warnings: List<ReportWarning> = emptyList(),
    val fixtureHintCount: Int = 0,
    val productionRuntimeEvidenceOnly: Boolean = false,
)

@Serializable
data class RuntimeEvidenceArtifactPaths(
    val originalImagePath: String? = null,
    val normalizedImagePath: String? = null,
    val rectifiedImagePath: String? = null,
    val graphPanelOverlayPath: String? = null,
    val plotAreaOverlayPath: String? = null,
    val axisOverlayPath: String? = null,
    val tickOverlayPath: String? = null,
    val calibrationAnchorsOverlayPath: String? = null,
    val ocrCropPaths: List<String> = emptyList(),
    val peakLabelCropPaths: List<String> = emptyList(),
    val peakLabelCropBoundsOverlayPath: String? = null,
    val peakLabelTextClassificationOverlayPath: String? = null,
    val rawPlotAreaCropPath: String? = null,
    val rawCurveMaskPath: String? = null,
    val cleanCurveMaskPath: String? = null,
    val textSuppressionOverlayPath: String? = null,
    val rejectedComponentsOverlayPath: String? = null,
    val selectedTraceOverlayPath: String? = null,
    val skeletonOrCenterlineOverlayPath: String? = null,
    val finalPeakOverlayPath: String? = null,
)

@Serializable
data class RuntimeEvidenceSummaryCounts(
    val rawDetectedPeaks: Int? = null,
    val validatedPeaks: Int? = null,
    val reviewPeaks: Int? = null,
    val rejectedPeaks: Int? = null,
    val userConfirmedPeaks: Int? = null,
    val userEditedPeaks: Int? = null,
    val runtimeRecoveredPeaks: Int = 0,
    val testOnlyRecoveredPeaks: Int = 0,
    val rejectedRecoveredCandidates: Int = 0,
    val productionReportablePeaks: Int? = null,
    val reviewGradePeaks: Int? = null,
)

@Serializable
data class RuntimeRoiFailureEvidencePackage(
    val schemaVersion: String = "runtime-evidence-roi-failure-1.0",
    val generatedAtEpochMillis: Long,
    val terminalState: RuntimeTerminalState = RuntimeTerminalState.ROI_FAILURE,
    val runtimeFailureClass: RuntimeFailureClass = RuntimeFailureClass.GRAPH_PANEL_FAILURE,
    val stageId: String,
    val failureReason: String,
    val originalImagePath: String? = null,
    val normalizedImagePath: String? = null,
    val graphPanelCandidates: List<com.chromalab.feature.processing.geometry.GraphPanelBounds> = emptyList(),
    val selectedGraphPanel: com.chromalab.feature.processing.geometry.GraphPanelBounds? = null,
    val selectedPlotArea: com.chromalab.feature.processing.geometry.PlotAreaBounds? = null,
    val warnings: List<String> = emptyList(),
    val timings: List<GeometryStageTiming> = emptyList(),
    val aiVisionStatus: String = "OPTIONAL_NOT_REQUIRED_FOR_ROI",
)

object RuntimeEvidencePackageBuilder {
    fun build(
        report: ChromatogramReport,
        modelAvailabilityDiagnostics: List<ModelAvailabilityDiagnostic> = emptyList(),
        graphFailurePackages: List<RuntimeGraphFailurePackage> = emptyList(),
    ): RuntimeEvidencePackage {
        val gate = ReportReleaseGateEvaluator.evaluate(
            report = report,
            evidencePackageStatus = EvidenceGateStatus.VALID,
        )
        val failureClass = report.metadata.runtimeFailureClass ?: gate.toRuntimeFailureClass()
        val reportWithFailureClass = report.copy(
            metadata = report.metadata.copy(runtimeFailureClass = failureClass),
        )
        val graphBuilds = report.graphs.map(::buildGraphPackage)
        return RuntimeEvidencePackage(
            generatedAtEpochMillis = currentTimeMillis(),
            reportId = report.metadata.reportId,
            sourceName = report.metadata.sourceName,
            deviceName = report.metadata.deviceName,
            selectedModelId = report.metadata.selectedModel?.modelId,
            executedModelId = report.metadata.executedModel?.modelId,
            executedRuntime = report.metadata.executedRuntime.name,
            terminalState = ReportReleaseGateEvaluator.terminalStateFor(gate.status),
            reportGateStatus = gate.status,
            runtimeFailureClass = failureClass,
            gateEvidence = gate.evidence,
            modelAvailabilityDiagnostics = modelAvailabilityDiagnostics,
            modelRuntimeProfiles = graphBuilds
                .flatMap { it.modelRuntimeProfiles }
                .distinctBy { it.profileId },
            graphFailurePackages = graphFailurePackages,
            graphs = graphBuilds.map { it.graphPackage },
            reportContract = reportWithFailureClass,
        )
    }

    private fun buildGraphPackage(graph: GraphReport): RuntimeGraphPackageBuild {
        val trace = graph.source.geometryTrace
        val recovery = graph.peakRecovery
        val fixtureHintCount = recovery.labelEvidence.count { !it.isRuntimeEvidence || it.source.name == "FIXTURE_HINT" }
        val artifactPaths = RuntimeEvidenceArtifactPaths(
            originalImagePath = trace?.originalImagePath,
            normalizedImagePath = trace?.normalizedImagePath,
            rectifiedImagePath = trace?.rectifiedImagePath,
            graphPanelOverlayPath = trace?.selectedGraphPanelOverlayPath,
            plotAreaOverlayPath = trace?.selectedPlotAreaOverlayPath,
            axisOverlayPath = trace?.axisOverlayPath,
            tickOverlayPath = trace?.tickOverlayPath,
            calibrationAnchorsOverlayPath = trace?.calibrationFitOverlayPath,
            ocrCropPaths = trace?.ocrCropPaths.orEmpty(),
            peakLabelCropPaths = trace?.peakLabelCropPaths.orEmpty(),
            peakLabelCropBoundsOverlayPath = trace?.peakLabelCropBoundsOverlayPath,
            peakLabelTextClassificationOverlayPath = trace?.peakLabelTextClassificationOverlayPath,
            rawPlotAreaCropPath = trace?.plotAreaCropPath,
            rawCurveMaskPath = trace?.curveMaskRawPath,
            cleanCurveMaskPath = trace?.curveMaskCleanPath,
            textSuppressionOverlayPath = trace?.curveTextSuppressionOverlayPath,
            rejectedComponentsOverlayPath = trace?.curveRejectedComponentsPath,
            selectedTraceOverlayPath = trace?.curveSelectedComponentPath
                ?: trace?.curveSkeletonPath
                ?: trace?.finalCenterlineOverlayPath,
            skeletonOrCenterlineOverlayPath = trace?.curveSkeletonPath,
            finalPeakOverlayPath = trace?.finalCenterlineOverlayPath,
        )
        val modelRuntimeProfiles = recovery.labelEvidence
            .mapNotNull { it.runtimeProfile }
            .distinctBy { it.profileId }
        val ocrVlmCropResults = recovery.labelEvidence.mapIndexedNotNull { index, evidence ->
            evidence.toCropResult(graph.graphIndex, index)
        }
        val overlayJudgeResults = buildOverlayJudgeResults(graph.graphIndex, artifactPaths)
        val stageJudgeResults = buildStageJudgeResults(
            graph = graph,
            artifactPaths = artifactPaths,
            cropResults = ocrVlmCropResults,
            overlayJudgeResults = overlayJudgeResults,
        )
        return RuntimeGraphPackageBuild(
            graphPackage = RuntimeEvidenceGraphPackage(
                graphIndex = graph.graphIndex,
                artifactPaths = artifactPaths,
                stageJudgeResults = stageJudgeResults,
                ocrVlmCropResults = ocrVlmCropResults,
                overlayJudgeResults = overlayJudgeResults,
                peakEvidenceTable = recovery.peakEvidenceTable,
                peakLabelEvidence = recovery.labelEvidence,
                runtimeRecoveredPeaks = recovery.runtimeRecoveredPeaks,
                testOnlyRecoveredPeaks = recovery.testOnlyRecoveredPeaks,
                rejectedRecoveredCandidates = recovery.rejectedRecoveredCandidates,
                suppressedTextBoxes = trace?.curveTextSuppressionRegions.orEmpty(),
                multiplicityResolution = trace?.multiplicityResolution,
                summaryCounts = RuntimeEvidenceSummaryCounts(
                    rawDetectedPeaks = recovery.rawDetectedPeaks,
                    validatedPeaks = recovery.validatedPeaks,
                    reviewPeaks = recovery.reviewPeaks,
                    rejectedPeaks = recovery.rejectedPeaks,
                    userConfirmedPeaks = recovery.userConfirmedPeaks,
                    userEditedPeaks = recovery.userEditedPeaks,
                    runtimeRecoveredPeaks = recovery.runtimeRecoveredPeaks.size,
                    testOnlyRecoveredPeaks = recovery.testOnlyRecoveredPeaks.size,
                    rejectedRecoveredCandidates = recovery.rejectedRecoveredCandidates.size,
                    productionReportablePeaks = recovery.productionReportablePeaks,
                    reviewGradePeaks = recovery.reviewGradePeaks,
                ),
                warnings = graph.warnings + recovery.warnings,
                fixtureHintCount = fixtureHintCount,
                productionRuntimeEvidenceOnly = fixtureHintCount == 0 &&
                    recovery.runtimeRecoveredPeaks.all { it.isProductionEvidence } &&
                    recovery.testOnlyRecoveredPeaks.isEmpty(),
            ),
            modelRuntimeProfiles = modelRuntimeProfiles,
        )
    }

    private data class RuntimeGraphPackageBuild(
        val graphPackage: RuntimeEvidenceGraphPackage,
        val modelRuntimeProfiles: List<ModelRuntimeProfile>,
    )

    private fun buildStageJudgeResults(
        graph: GraphReport,
        artifactPaths: RuntimeEvidenceArtifactPaths,
        cropResults: List<VlmOcrCropResult>,
        overlayJudgeResults: List<OverlayJudgeResult>,
    ): List<AutonomousStageJudgeResult> {
        val trace = graph.source.geometryTrace
        val recovery = graph.peakRecovery
        return buildList {
            add(
                AutonomousStageJudgeResult(
                    taskId = "graph:${graph.graphIndex}:graph_panel",
                    graphIndex = graph.graphIndex,
                    taskType = StageJudgeTaskType.GRAPH_PANEL_CANDIDATE_JUDGE,
                    source = StageJudgeSource.CV,
                    verdict = if (trace?.selectedGraphPanelBounds != null) StageJudgeVerdict.PASS else StageJudgeVerdict.REVIEW,
                    confidence = StageJudgeConfidence(trace?.selectedGraphPanelBounds?.confidence ?: 0f),
                    overlayPath = artifactPaths.graphPanelOverlayPath,
                ),
            )
            add(
                AutonomousStageJudgeResult(
                    taskId = "graph:${graph.graphIndex}:plot_area",
                    graphIndex = graph.graphIndex,
                    taskType = StageJudgeTaskType.PLOT_AREA_CANDIDATE_JUDGE,
                    source = StageJudgeSource.CV,
                    verdict = if (trace?.selectedPlotAreaBounds != null) StageJudgeVerdict.PASS else StageJudgeVerdict.REVIEW,
                    confidence = StageJudgeConfidence(trace?.selectedPlotAreaBounds?.confidence ?: 0f),
                    overlayPath = artifactPaths.plotAreaOverlayPath,
                ),
            )
            add(
                AutonomousStageJudgeResult(
                    taskId = "graph:${graph.graphIndex}:axis_tick_visibility",
                    graphIndex = graph.graphIndex,
                    taskType = StageJudgeTaskType.AXIS_TICK_VISIBILITY_JUDGE,
                    source = StageJudgeSource.ML_KIT,
                    verdict = if (trace?.tickOcrResult?.acceptedItems.orEmpty().isNotEmpty()) StageJudgeVerdict.PASS else StageJudgeVerdict.REVIEW,
                    confidence = StageJudgeConfidence(trace?.tickOcrResult?.acceptedItems.orEmpty().map { it.confidence }.averageOrNull() ?: 0f),
                    overlayPath = artifactPaths.tickOverlayPath,
                ),
            )
            cropResults.forEach { crop ->
                add(
                    AutonomousStageJudgeResult(
                        taskId = crop.taskId,
                        graphIndex = graph.graphIndex,
                        taskType = StageJudgeTaskType.OCR_CROP_READ,
                        source = crop.source,
                        verdict = when {
                            crop.source == StageJudgeSource.VLM && crop.rawText.isBlank() -> StageJudgeVerdict.REVIEW
                            crop.textClass == MultimodalTextRegionClass.PEAK_ANNOTATION -> StageJudgeVerdict.PASS
                            else -> StageJudgeVerdict.REVIEW
                        },
                        confidence = crop.confidence,
                        cropPath = crop.localCropPath,
                        modelRuntimeProfileId = crop.runtimeProfileId,
                        ocrCropResultIds = listOf(crop.resultId),
                        rejectedForbiddenFields = crop.rejectedForbiddenFields,
                    ),
                )
            }
            artifactPaths.selectedTraceOverlayPath?.let { traceOverlayPath ->
                add(
                    AutonomousStageJudgeResult(
                        taskId = "graph:${graph.graphIndex}:trace_overlay",
                        graphIndex = graph.graphIndex,
                        taskType = StageJudgeTaskType.TRACE_OVERLAY_JUDGE,
                        source = StageJudgeSource.DETERMINISTIC,
                        verdict = StageJudgeVerdict.PASS,
                        confidence = StageJudgeConfidence(1f),
                        overlayPath = traceOverlayPath,
                        overlayJudgeResultIds = overlayJudgeResults.map { it.resultId },
                    ),
                )
            }
            add(
                AutonomousStageJudgeResult(
                    taskId = "graph:${graph.graphIndex}:peak_evidence",
                    graphIndex = graph.graphIndex,
                    taskType = StageJudgeTaskType.PEAK_EVIDENCE_JUDGE,
                    source = StageJudgeSource.DETERMINISTIC,
                    verdict = if (recovery.productionReportablePeaks != null) StageJudgeVerdict.PASS else StageJudgeVerdict.REVIEW,
                    confidence = StageJudgeConfidence(if (recovery.productionReportablePeaks != null) 1f else 0.4f),
                    overlayPath = artifactPaths.finalPeakOverlayPath,
                    linkedEvidenceIds = recovery.peakEvidenceTable.map { it.evidenceId },
                ),
            )
        }
    }

    private fun buildOverlayJudgeResults(
        graphIndex: Int,
        artifactPaths: RuntimeEvidenceArtifactPaths,
    ): List<OverlayJudgeResult> = buildList {
        artifactPaths.selectedTraceOverlayPath?.let { path ->
            add(
                OverlayJudgeResult(
                    resultId = "overlay:graph:$graphIndex:trace",
                    taskId = "graph:$graphIndex:trace_overlay",
                    overlayImagePath = path,
                    verdict = StageJudgeVerdict.PASS,
                    confidence = StageJudgeConfidence(1f),
                ),
            )
        }
    }

    private fun PeakLabelEvidence.toCropResult(graphIndex: Int, index: Int): VlmOcrCropResult? {
        if (!isRuntimeEvidence || source == PeakLabelEvidenceSource.FIXTURE_HINT) return null
        val taskId = "graph:$graphIndex:ocr_crop:$index"
        val resultSource = when (source) {
            PeakLabelEvidenceSource.ML_KIT -> StageJudgeSource.ML_KIT
            PeakLabelEvidenceSource.VLM -> StageJudgeSource.VLM
            PeakLabelEvidenceSource.BOTH -> StageJudgeSource.BOTH
            PeakLabelEvidenceSource.FIXTURE_HINT -> StageJudgeSource.SYSTEM
        }
        return VlmOcrCropResult(
            resultId = "ocr-vlm:graph:$graphIndex:$index",
            taskId = taskId,
            source = resultSource,
            localCropPath = localCropPath,
            rawText = rawText,
            normalizedText = normalizedText,
            parsedText = parsedRetentionTime?.toString(),
            textClass = textClassification.toMultimodalClass(),
            confidence = StageJudgeConfidence(confidence.coerceIn(0f, 1f)),
            durationMillis = runtimeProfile?.durationMillis,
            runtimeProfileId = runtimeProfile?.profileId,
            rejectedForbiddenFields = rejectedForbiddenFields,
        )
    }

    private fun PeakLabelTextClassification.toMultimodalClass(): MultimodalTextRegionClass =
        when (this) {
            PeakLabelTextClassification.PEAK_ANNOTATION -> MultimodalTextRegionClass.PEAK_ANNOTATION
            PeakLabelTextClassification.TICK_LABEL -> MultimodalTextRegionClass.TICK_LABEL
            PeakLabelTextClassification.AXIS_LABEL -> MultimodalTextRegionClass.AXIS_LABEL
            PeakLabelTextClassification.TITLE_OR_CHANNEL -> MultimodalTextRegionClass.TITLE_OR_CHANNEL
            PeakLabelTextClassification.PAGE_TEXT -> MultimodalTextRegionClass.PAGE_TEXT
            PeakLabelTextClassification.UNKNOWN_TEXT -> MultimodalTextRegionClass.UNKNOWN_TEXT
        }
}

private fun currentTimeMillis(): Long = System.currentTimeMillis()

private fun com.chromalab.feature.reports.ReportGateEvaluation.toRuntimeFailureClass(): RuntimeFailureClass? {
    if (status == ReportGateStatus.RELEASE_READY) return null
    val releaseEvidenceComplete = listOf(
        evidence.graphPanelStatus,
        evidence.plotAreaStatus,
        evidence.xCalibrationStatus,
        evidence.yCalibrationStatus,
        evidence.traceStatus,
        evidence.peakReviewStatus,
        evidence.evidencePackageStatus,
        evidence.sourceProvenanceStatus,
    ).all { status ->
        status == EvidenceGateStatus.VALID ||
            status == EvidenceGateStatus.USER_CONFIRMED ||
            status == EvidenceGateStatus.NOT_APPLICABLE
    }
    if (releaseEvidenceComplete && evidence.vlmEvidenceStatus == EvidenceGateStatus.INVALID) {
        return RuntimeFailureClass.VLM_SEMANTIC_LAYER_UNAVAILABLE
    }
    val reason = (blockingReasons + reviewReasons).firstOrNull()
    return when {
        reason == null && evidence.vlmEvidenceStatus == EvidenceGateStatus.INVALID ->
            RuntimeFailureClass.VLM_SEMANTIC_LAYER_UNAVAILABLE
        reason == null && evidence.traceStatus == EvidenceGateStatus.REVIEW ->
            RuntimeFailureClass.SPARSE_TRACE_REVIEW
        reason == null -> RuntimeFailureClass.UNKNOWN_FAILURE
        "graph_panel" in reason -> RuntimeFailureClass.GRAPH_PANEL_FAILURE
        "plot_area" in reason -> RuntimeFailureClass.PLOT_AREA_FAILURE
        "axis" in reason -> RuntimeFailureClass.AXIS_DETECTION_FAILURE
        "tick" in reason -> RuntimeFailureClass.TICK_LOCALIZATION_FAILURE
        "ocr" in reason -> RuntimeFailureClass.OCR_TICK_FAILURE
        "calibration" in reason -> RuntimeFailureClass.CALIBRATION_FAILURE
        "trace" in reason -> RuntimeFailureClass.TRACE_EXTRACTION_FAILURE
        "peak" in reason -> RuntimeFailureClass.PEAK_EVIDENCE_FAILURE
        "model_not_configured" in reason -> RuntimeFailureClass.MODEL_NOT_CONFIGURED
        "model_asset_missing" in reason -> RuntimeFailureClass.MODEL_ASSET_MISSING
        "model_load_failed" in reason -> RuntimeFailureClass.MODEL_LOAD_FAILED
        "vlm_model_unavailable" in reason -> RuntimeFailureClass.VLM_MODEL_UNAVAILABLE
        "vlm_evidence" in reason -> RuntimeFailureClass.VLM_SEMANTIC_LAYER_UNAVAILABLE
        "vlm" in reason || "model" in reason -> RuntimeFailureClass.VLM_UNSUPPORTED_CLAIM
        "knowledge" in reason -> RuntimeFailureClass.KNOWLEDGE_GROUNDING_FAILURE
        "evidence_package" in reason || "source_provenance" in reason || "report" in reason ->
            RuntimeFailureClass.REPORT_GATE_FAILURE
        else -> RuntimeFailureClass.UNKNOWN_FAILURE
    }
}

private fun List<Float>.averageOrNull(): Float? =
    takeIf { it.isNotEmpty() }?.average()?.toFloat()?.coerceIn(0f, 1f)

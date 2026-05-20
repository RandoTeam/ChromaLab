package com.chromalab.feature.processing.debug

import com.chromalab.feature.processing.curve.CurveMaskTextSuppressionRegion
import com.chromalab.feature.processing.geometry.GraphMultiplicityResolution
import com.chromalab.feature.processing.geometry.GeometryStageTiming
import com.chromalab.feature.processing.multimodal.AutonomousStageJudgeResult
import com.chromalab.feature.processing.multimodal.ModelRuntimeProfile
import com.chromalab.feature.processing.multimodal.OcrVlmDisagreement
import com.chromalab.feature.processing.multimodal.OverlayJudgeResult
import com.chromalab.feature.processing.multimodal.VlmOcrCropResult
import com.chromalab.feature.processing.peaks.PeakLabelEvidence
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidate
import com.chromalab.feature.reports.ChromatogramReport
import com.chromalab.feature.reports.EvidenceGateStatus
import com.chromalab.feature.reports.GateEvidence
import com.chromalab.feature.reports.GraphReport
import com.chromalab.feature.reports.PeakEvidence
import com.chromalab.feature.reports.ReportWarning
import com.chromalab.feature.reports.ReportGateStatus
import com.chromalab.feature.reports.ReportReleaseGateEvaluator
import com.chromalab.feature.reports.RuntimeTerminalState
import kotlinx.serialization.Serializable

@Serializable
data class RuntimeEvidencePackage(
    val schemaVersion: String = "runtime-evidence-1.1",
    val generatedAtEpochMillis: Long,
    val reportId: String,
    val sourceName: String? = null,
    val deviceName: String? = null,
    val selectedModelId: String? = null,
    val executedModelId: String? = null,
    val executedRuntime: String,
    val terminalState: RuntimeTerminalState = RuntimeTerminalState.DIAGNOSTIC_ONLY,
    val reportGateStatus: ReportGateStatus = ReportGateStatus.DIAGNOSTIC_ONLY,
    val gateEvidence: GateEvidence = GateEvidence(),
    val modelRuntimeProfiles: List<ModelRuntimeProfile> = emptyList(),
    val graphs: List<RuntimeEvidenceGraphPackage>,
    val reportContract: ChromatogramReport,
)

@Serializable
data class RuntimeEvidenceGraphPackage(
    val graphIndex: Int,
    val artifactPaths: RuntimeEvidenceArtifactPaths,
    val stageJudgeResults: List<AutonomousStageJudgeResult> = emptyList(),
    val ocrVlmCropResults: List<VlmOcrCropResult> = emptyList(),
    val ocrVlmDisagreements: List<OcrVlmDisagreement> = emptyList(),
    val overlayJudgeResults: List<OverlayJudgeResult> = emptyList(),
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
    fun build(report: ChromatogramReport): RuntimeEvidencePackage {
        val gate = ReportReleaseGateEvaluator.evaluate(
            report = report,
            evidencePackageStatus = EvidenceGateStatus.VALID,
        )
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
            gateEvidence = gate.evidence,
            graphs = report.graphs.map(::buildGraphPackage),
            reportContract = report,
        )
    }

    private fun buildGraphPackage(graph: GraphReport): RuntimeEvidenceGraphPackage {
        val trace = graph.source.geometryTrace
        val recovery = graph.peakRecovery
        val fixtureHintCount = recovery.labelEvidence.count { !it.isRuntimeEvidence || it.source.name == "FIXTURE_HINT" }
        return RuntimeEvidenceGraphPackage(
            graphIndex = graph.graphIndex,
            artifactPaths = RuntimeEvidenceArtifactPaths(
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
            ),
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
        )
    }
}

private fun currentTimeMillis(): Long = System.currentTimeMillis()

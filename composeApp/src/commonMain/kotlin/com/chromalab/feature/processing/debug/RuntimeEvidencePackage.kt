package com.chromalab.feature.processing.debug

import com.chromalab.feature.processing.peaks.PeakLabelEvidence
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidate
import com.chromalab.feature.reports.ChromatogramReport
import com.chromalab.feature.reports.GraphReport
import com.chromalab.feature.reports.ReportWarning
import kotlinx.serialization.Serializable

@Serializable
data class RuntimeEvidencePackage(
    val schemaVersion: String = "runtime-evidence-1.0",
    val generatedAtEpochMillis: Long,
    val reportId: String,
    val sourceName: String? = null,
    val deviceName: String? = null,
    val selectedModelId: String? = null,
    val executedModelId: String? = null,
    val executedRuntime: String,
    val graphs: List<RuntimeEvidenceGraphPackage>,
    val reportContract: ChromatogramReport,
)

@Serializable
data class RuntimeEvidenceGraphPackage(
    val graphIndex: Int,
    val artifactPaths: RuntimeEvidenceArtifactPaths,
    val peakLabelEvidence: List<PeakLabelEvidence>,
    val runtimeRecoveredPeaks: List<RecoveredPeakCandidate>,
    val testOnlyRecoveredPeaks: List<RecoveredPeakCandidate>,
    val rejectedRecoveredCandidates: List<RecoveredPeakCandidate>,
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
    val runtimeRecoveredPeaks: Int = 0,
    val testOnlyRecoveredPeaks: Int = 0,
    val rejectedRecoveredCandidates: Int = 0,
    val productionReportablePeaks: Int? = null,
    val reviewGradePeaks: Int? = null,
)

object RuntimeEvidencePackageBuilder {
    fun build(report: ChromatogramReport): RuntimeEvidencePackage =
        RuntimeEvidencePackage(
            generatedAtEpochMillis = currentTimeMillis(),
            reportId = report.metadata.reportId,
            sourceName = report.metadata.sourceName,
            deviceName = report.metadata.deviceName,
            selectedModelId = report.metadata.selectedModel?.modelId,
            executedModelId = report.metadata.executedModel?.modelId,
            executedRuntime = report.metadata.executedRuntime.name,
            graphs = report.graphs.map(::buildGraphPackage),
            reportContract = report,
        )

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
                selectedTraceOverlayPath = trace?.curveSelectedComponentPath,
                skeletonOrCenterlineOverlayPath = trace?.curveSkeletonPath,
                finalPeakOverlayPath = trace?.finalCenterlineOverlayPath,
            ),
            peakLabelEvidence = recovery.labelEvidence,
            runtimeRecoveredPeaks = recovery.runtimeRecoveredPeaks,
            testOnlyRecoveredPeaks = recovery.testOnlyRecoveredPeaks,
            rejectedRecoveredCandidates = recovery.rejectedRecoveredCandidates,
            summaryCounts = RuntimeEvidenceSummaryCounts(
                rawDetectedPeaks = recovery.rawDetectedPeaks,
                validatedPeaks = recovery.validatedPeaks,
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

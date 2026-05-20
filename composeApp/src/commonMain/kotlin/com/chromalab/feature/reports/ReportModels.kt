package com.chromalab.feature.reports

import com.chromalab.feature.processing.geometry.AxisCalibrationFit
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import com.chromalab.feature.processing.geometry.GeometryTrace
import com.chromalab.feature.processing.peaks.PeakLabelEvidence
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidate
import kotlinx.serialization.Serializable

const val CURRENT_CHROMATOGRAM_REPORT_SCHEMA = "1.0.0-phase-5"

/**
 * Strict report contract for chromatogram analysis.
 *
 * This model is intentionally independent from the current calculation UI. Later phases should map
 * image-processing, CalculationRun, local knowledge, and model-runtime metadata into this schema.
 */
@Serializable
data class ChromatogramReport(
    val metadata: ReportMetadata,
    val graphs: List<GraphReport>,
    val warnings: List<ReportWarning> = emptyList(),
)

@Serializable
data class ReportMetadata(
    val reportId: String,
    val appVersion: String? = null,
    val schemaVersion: String = CURRENT_CHROMATOGRAM_REPORT_SCHEMA,
    val analysisStartedAtEpochMillis: Long? = null,
    val analysisCompletedAtEpochMillis: Long? = null,
    val totalAnalysisDurationMillis: Long? = null,
    val inputSourceType: InputSourceType,
    val sourceName: String? = null,
    val detectedGraphCount: Int,
    val selectedModel: ModelExecutionInfo? = null,
    val executedModel: ModelExecutionInfo? = null,
    val executedRuntime: ExecutedRuntime = ExecutedRuntime.UNKNOWN,
    val deviceName: String? = null,
    val processingMode: ProcessingMode = ProcessingMode.UNKNOWN,
    val stageTimings: List<ReportStageTiming> = emptyList(),
)

@Serializable
data class ModelExecutionInfo(
    val modelId: String,
    val modelName: String? = null,
    val runtime: ExecutedRuntime = ExecutedRuntime.UNKNOWN,
    val backendLabel: String? = null,
)

@Serializable
data class ReportStageTiming(
    val stageId: String,
    val stageName: String? = null,
    val durationMillis: Long,
)

@Serializable
enum class InputSourceType {
    CAMERA_CAPTURE,
    SMART_SCAN_GALLERY,
    FILE_IMPORT,
    TEST_FIXTURE,
    UNKNOWN,
}

@Serializable
enum class ProcessingMode {
    AUTONOMOUS_PRODUCTION,
    AUTO_DIAGNOSTIC,
    ASSISTED_REVIEW,
    @Deprecated("GUIDED_PRODUCTION is a compatibility alias. Use ASSISTED_REVIEW.")
    GUIDED_PRODUCTION,
    MANUAL_ADVANCED,
    FULL_ANALYSIS,
    DIAGNOSTIC,
    MANUAL_REVIEW,
    EXPORT_ONLY,
    UNKNOWN,
}

@Serializable
enum class ExecutedRuntime {
    LITERT,
    GGUF,
    OCR,
    DETERMINISTIC,
    MIXED,
    UNKNOWN,
}

@Serializable
data class GraphReport(
    val graphIndex: Int,
    val source: GraphSourceMetadata,
    val identification: ChromatogramIdentification,
    val axisCalibration: ReportAxisCalibration,
    val signal: SignalAndBaselineReport,
    val peaks: List<ReportPeak>,
    val peakRecovery: PeakEvidenceAndRecoveryReport = PeakEvidenceAndRecoveryReport(),
    val quality: ChromatographicQualityReport,
    val kovats: KovatsIndexReport,
    val interpretation: ChemicalInterpretationReport,
    val sectionStatus: ReportSectionStatus = ReportSectionStatus(),
    val warnings: List<ReportWarning> = emptyList(),
)

@Serializable
data class PeakEvidenceAndRecoveryReport(
    val rawDetectedPeaks: Int? = null,
    val validatedPeaks: Int? = null,
    val peakEvidenceTable: List<PeakEvidence> = emptyList(),
    val reviewPeaks: Int? = null,
    val rejectedPeaks: Int? = null,
    val userConfirmedPeaks: Int? = null,
    val userEditedPeaks: Int? = null,
    val runtimeRecoveredPeaks: List<RecoveredPeakCandidate> = emptyList(),
    val testOnlyRecoveredPeaks: List<RecoveredPeakCandidate> = emptyList(),
    val rejectedRecoveredCandidates: List<RecoveredPeakCandidate> = emptyList(),
    val productionReportablePeaks: Int? = null,
    val reviewGradePeaks: Int? = null,
    val denseSeriesMembers: Int? = null,
    val rejectedArtifactPeaks: Int? = null,
    val labelEvidence: List<PeakLabelEvidence> = emptyList(),
    val warnings: List<ReportWarning> = emptyList(),
)

@Serializable
enum class PeakEvidenceStatus {
    AUTO_VALID,
    AUTO_REVIEW,
    USER_CONFIRMED,
    USER_EDITED,
    USER_REJECTED,
    ARTIFACT_REJECTED,
    NOISE_REJECTED,
    SHOULDER_REVIEW,
    OVERLAP_REVIEW,
    INVALID,
}

@Serializable
enum class PeakMetricEvidenceStatus {
    CALCULATED,
    UNKNOWN,
    REVIEW,
    INVALID,
}

@Serializable
enum class PeakArtifactStatus {
    NONE,
    ARTIFACT_REJECTED,
    NOISE_REJECTED,
    UNKNOWN,
}

@Serializable
enum class PeakOverlapEvidenceStatus {
    ISOLATED,
    SHOULDER_REVIEW,
    OVERLAP_REVIEW,
    UNRESOLVED_REVIEW,
    UNKNOWN,
}

@Serializable
enum class PeakGateStatus {
    VALID,
    REVIEW,
    INVALID,
    MISSING,
}

@Serializable
data class PeakMetricEvidence(
    val value: Double? = null,
    val unit: String? = null,
    val status: PeakMetricEvidenceStatus = PeakMetricEvidenceStatus.UNKNOWN,
    val source: ReportValueSource = ReportValueSource.UNKNOWN,
    val warning: String? = null,
) {
    companion object {
        fun calculated(value: Double?, unit: String? = null): PeakMetricEvidence =
            if (value != null && value.isFinite()) {
                PeakMetricEvidence(value, unit, PeakMetricEvidenceStatus.CALCULATED, ReportValueSource.DETERMINISTIC)
            } else {
                unknown(unit, "metric unavailable")
            }

        fun review(value: Double?, unit: String? = null, warning: String): PeakMetricEvidence =
            PeakMetricEvidence(value?.takeIf { it.isFinite() }, unit, PeakMetricEvidenceStatus.REVIEW, ReportValueSource.DETERMINISTIC, warning)

        fun invalid(value: Double?, unit: String? = null, warning: String): PeakMetricEvidence =
            PeakMetricEvidence(value?.takeIf { it.isFinite() }, unit, PeakMetricEvidenceStatus.INVALID, ReportValueSource.DETERMINISTIC, warning)

        fun unknown(unit: String? = null, warning: String? = null): PeakMetricEvidence =
            PeakMetricEvidence(null, unit, PeakMetricEvidenceStatus.UNKNOWN, ReportValueSource.UNKNOWN, warning)
    }
}

@Serializable
data class PeakBoundaryEvidence(
    val startRetentionTime: PeakMetricEvidence = PeakMetricEvidence.unknown("min"),
    val endRetentionTime: PeakMetricEvidence = PeakMetricEvidence.unknown("min"),
    val method: String? = null,
    val integrationMethod: String? = null,
    val baselineMethod: String? = null,
    val status: PeakMetricEvidenceStatus = PeakMetricEvidenceStatus.UNKNOWN,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class PeakProvenance(
    val source: PeakEvidenceSource = PeakEvidenceSource.AUTO_DETECTED,
    val metricSource: String = "CalculationRun",
    val calculationRunId: String? = null,
    val sourceSignalId: String? = null,
    val pipelineVersion: String? = null,
    val algorithmVersion: String? = null,
    val traceSourceId: String? = null,
    val userDecisionId: String? = null,
    val userVisibleIntervention: Boolean = false,
)

@Serializable
enum class PeakEvidenceSource {
    AUTO_DETECTED,
    LABEL_RECOVERED,
    USER_CONFIRMED,
    USER_EDITED,
    USER_REJECTED,
    IMPORTED,
}

@Serializable
data class PeakEvidence(
    val evidenceId: String,
    val peakId: String,
    val peakNumber: Int,
    val status: PeakEvidenceStatus,
    val gateStatus: PeakGateStatus,
    val retentionTime: PeakMetricEvidence = PeakMetricEvidence.unknown("min"),
    val apexPixel: PixelPoint? = null,
    val apexPointIndex: Int? = null,
    val localMaximumEvidence: Boolean = false,
    val height: PeakMetricEvidence = PeakMetricEvidence.unknown("a.u."),
    val area: PeakMetricEvidence = PeakMetricEvidence.unknown(),
    val areaPercent: PeakMetricEvidence = PeakMetricEvidence.unknown("%"),
    val widthAtBase: PeakMetricEvidence = PeakMetricEvidence.unknown("min"),
    val fwhm: PeakMetricEvidence = PeakMetricEvidence.unknown("min"),
    val signalToNoise: PeakMetricEvidence = PeakMetricEvidence.unknown(),
    val prominence: PeakMetricEvidence = PeakMetricEvidence.unknown("a.u."),
    val baselineAtApex: PeakMetricEvidence = PeakMetricEvidence.unknown("a.u."),
    val boundaryEvidence: PeakBoundaryEvidence = PeakBoundaryEvidence(),
    val artifactStatus: PeakArtifactStatus = PeakArtifactStatus.UNKNOWN,
    val overlapStatus: PeakOverlapEvidenceStatus = PeakOverlapEvidenceStatus.UNKNOWN,
    val provenance: PeakProvenance = PeakProvenance(),
    val flags: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
) {
    val isReportable: Boolean
        get() = gateStatus != PeakGateStatus.INVALID &&
            status !in setOf(
                PeakEvidenceStatus.USER_REJECTED,
                PeakEvidenceStatus.ARTIFACT_REJECTED,
                PeakEvidenceStatus.NOISE_REJECTED,
                PeakEvidenceStatus.INVALID,
            )

    val requiresReview: Boolean
        get() = gateStatus == PeakGateStatus.REVIEW ||
            status in setOf(
                PeakEvidenceStatus.AUTO_REVIEW,
                PeakEvidenceStatus.SHOULDER_REVIEW,
                PeakEvidenceStatus.OVERLAP_REVIEW,
            )
}

@Serializable
data class GraphSourceMetadata(
    val sourceImageBounds: PixelRect? = null,
    val detectedGraphBounds: PixelRect? = null,
    val cropConfidence: Double? = null,
    val geometryReportStatus: GeometryReportStatus? = null,
    val geometryTrace: GeometryTrace? = null,
    val preprocessingSteps: List<String> = emptyList(),
    val selectedPreparationVariant: GraphPreparationVariantMetadata? = null,
    val rejectedPreparationVariants: List<GraphPreparationVariantMetadata> = emptyList(),
    val scanMode: String? = null,
    val titleOcrConfidence: Double? = null,
    val axisOcrConfidence: Double? = null,
    val tickOcrConfidence: Double? = null,
    val manuallyAdjusted: Boolean = false,
)

@Serializable
data class GraphPreparationVariantMetadata(
    val rank: Int,
    val configName: String,
    val inputVariant: String,
    val score: Double,
    val selected: Boolean,
    val scoreBreakdown: String? = null,
)

@Serializable
data class PixelRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

@Serializable
data class ChromatogramIdentification(
    val chromatogramTitle: ReportTextValue = ReportTextValue.notCalculated(),
    val analysisType: ReportTextValue = ReportTextValue.notCalculated(),
    val chromatogramMode: ReportTextValue = ReportTextValue.notCalculated(),
    val ionOrChannel: ReportTextValue = ReportTextValue.notCalculated(),
    val ionRange: ReportTextValue = ReportTextValue.notCalculated(),
    val sampleName: ReportTextValue = ReportTextValue.notCalculated(),
    val samplePathOrInstrumentLabel: ReportTextValue = ReportTextValue.notCalculated(),
    val matrix: ReportTextValue = ReportTextValue.notCalculated(),
    val targetCompoundClass: ReportTextValue = ReportTextValue.notCalculated(),
)

@Serializable
data class ReportAxisCalibration(
    val xAxis: AxisReport = AxisReport(),
    val yAxis: AxisReport = AxisReport(),
    val calibrationConfidence: Double? = null,
    val xCalibrationFit: AxisCalibrationFit? = null,
    val yCalibrationFit: AxisCalibrationFit? = null,
    val calibrationCandidates: List<AxisCalibrationCandidate> = emptyList(),
    val pixelToUnitTransform: PixelToUnitTransform? = null,
    val warnings: List<ReportWarning> = emptyList(),
)

@Serializable
data class AxisCalibrationCandidate(
    val candidateId: String,
    val axis: ReportAxisName,
    val source: ReportValueSource = ReportValueSource.UNKNOWN,
    val status: AxisCalibrationCandidateStatus = AxisCalibrationCandidateStatus.CANDIDATE,
    val unit: String? = null,
    val confidence: Double? = null,
    val points: List<AxisCalibrationCandidatePoint> = emptyList(),
    val rejectionReasons: List<String> = emptyList(),
)

@Serializable
data class AxisCalibrationCandidatePoint(
    val value: Double,
    val pixel: Double? = null,
    val text: String? = null,
    val confidence: Double? = null,
)

@Serializable
enum class ReportAxisName {
    X,
    Y,
}

@Serializable
enum class AxisCalibrationCandidateStatus {
    CANDIDATE,
    VALIDATED,
    INSUFFICIENT_DATA,
    REJECTED,
}

@Serializable
data class AxisReport(
    val label: ReportTextValue = ReportTextValue.notCalculated(),
    val unit: ReportTextValue = ReportTextValue.notCalculated(),
    val visibleMinimum: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val visibleMaximum: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val majorTicks: List<ReportDoubleValue> = emptyList(),
    val minorTicks: List<ReportDoubleValue> = emptyList(),
)

@Serializable
data class PixelToUnitTransform(
    val xScale: Double,
    val xOffset: Double,
    val yScale: Double,
    val yOffset: Double,
    val method: String,
)

@Serializable
data class SignalAndBaselineReport(
    val pointCount: Int? = null,
    val smoothingMethod: String? = null,
    val smoothingParameters: Map<String, String> = emptyMap(),
    val baselineMethod: String? = null,
    val baselineParameters: Map<String, String> = emptyMap(),
    val baselineMean: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val baselineDrift: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val rmsNoise: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val noiseMethod: String? = null,
    val correctedSignalAvailable: Boolean = false,
    val signalExtractionConfidence: Double? = null,
)

@Serializable
data class ReportPeak(
    val number: Int,
    val retentionTime: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val apexPixel: PixelPoint? = null,
    val absoluteApexIntensity: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val baselineAtApex: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val heightAboveBaseline: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val startRetentionTime: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val endRetentionTime: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val widthAtBase: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val fwhm: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val integratedArea: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val areaPercent: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val signalToNoise: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val asymmetry: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val overlapClass: ReportTextValue = ReportTextValue.notCalculated(),
    val boundaryMethod: String? = null,
    val integrationMethod: String? = null,
    val confidence: Double? = null,
    val compound: CompoundAssignment? = null,
    val flags: List<String> = emptyList(),
    val warnings: List<ReportWarning> = emptyList(),
)

@Serializable
data class PixelPoint(
    val x: Int,
    val y: Double,
)

@Serializable
data class CompoundAssignment(
    val probableName: ReportTextValue = ReportTextValue.notCalculated(),
    val formula: ReportTextValue = ReportTextValue.notCalculated(),
    val compoundClass: ReportTextValue = ReportTextValue.notCalculated(),
    val carbonNumber: ReportTextValue = ReportTextValue.notCalculated(),
    val kovatsIndex: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val literatureKovatsRange: DoubleRangeValue? = null,
    val literatureDelta: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val assignmentConfidence: Double? = null,
    val assignmentBasis: String? = null,
)

@Serializable
data class DoubleRangeValue(
    val minimum: Double,
    val maximum: Double,
    val unit: String? = null,
)

@Serializable
data class ChromatographicQualityReport(
    val totalDetectedPeaks: Int? = null,
    val significantPeakCount: Int? = null,
    val significantPeakSnrThreshold: Double? = null,
    val meanSnr: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val medianSnr: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val maximumPeakHeight: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val dominantPeakNumber: Int? = null,
    val baselineQuality: ReportTextValue = ReportTextValue.notCalculated(),
    val averageResolution: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val minimumResolution: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val theoreticalPlates: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val hetp: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val globalIntegratedArea: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val areaNormalizationStatus: ReportTextValue = ReportTextValue.notCalculated(),
    val anomalies: List<ReportAnomaly> = emptyList(),
)

@Serializable
data class ReportAnomaly(
    val code: String,
    val message: String,
    val peakNumber: Int? = null,
    val severity: ReportSeverity = ReportSeverity.WARNING,
)

@Serializable
data class KovatsIndexReport(
    val status: ReportValueStatus = ReportValueStatus.NOT_CALCULATED,
    val formula: String? = null,
    val referenceSeries: String? = null,
    val referenceRetentionTimes: List<KovatsReferencePoint> = emptyList(),
    val results: List<KovatsIndexResult> = emptyList(),
    val trendLinearityR2: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val missingDataNotes: List<String> = emptyList(),
)

@Serializable
data class KovatsReferencePoint(
    val compoundName: String,
    val carbonNumber: String,
    val retentionTime: Double,
    val kovatsIndex: Double,
)

@Serializable
data class KovatsIndexResult(
    val peakNumber: Int,
    val compoundName: String? = null,
    val carbonNumber: String? = null,
    val retentionTime: Double? = null,
    val calculatedIndex: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val literatureRange: DoubleRangeValue? = null,
    val deltaFromLiterature: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val calculationKind: KovatsCalculationKind = KovatsCalculationKind.NOT_CALCULABLE,
    val confidence: Double? = null,
)

@Serializable
enum class KovatsCalculationKind {
    DIRECT,
    INTERPOLATED,
    LITERATURE_LOOKUP,
    NOT_CALCULABLE,
}

@Serializable
data class ChemicalInterpretationReport(
    val distributionByCarbonNumber: List<DistributionBucket> = emptyList(),
    val homologSeriesNotes: List<String> = emptyList(),
    val likelyCompoundClass: ReportTextValue = ReportTextValue.notCalculated(),
    val domainContextNotes: List<String> = emptyList(),
    val unresolvedAssignments: List<String> = emptyList(),
)

@Serializable
data class DistributionBucket(
    val label: String,
    val area: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val areaPercent: ReportDoubleValue = ReportDoubleValue.notCalculated(),
    val peakCount: Int? = null,
)

@Serializable
data class ReportSectionStatus(
    val overview: ReportValueStatus = ReportValueStatus.NOT_CALCULATED,
    val graphPreparation: ReportValueStatus = ReportValueStatus.NOT_CALCULATED,
    val axisCalibration: ReportValueStatus = ReportValueStatus.NOT_CALCULATED,
    val peakTable: ReportValueStatus = ReportValueStatus.NOT_CALCULATED,
    val graphOverlay: ReportValueStatus = ReportValueStatus.NOT_CALCULATED,
    val chromatographicQuality: ReportValueStatus = ReportValueStatus.NOT_CALCULATED,
    val kovatsAnalysis: ReportValueStatus = ReportValueStatus.NOT_CALCULATED,
    val chemicalInterpretation: ReportValueStatus = ReportValueStatus.NOT_CALCULATED,
    val technicalAppendix: ReportValueStatus = ReportValueStatus.NOT_CALCULATED,
)

@Serializable
data class ReportTextValue(
    val value: String? = null,
    val status: ReportValueStatus = ReportValueStatus.NOT_CALCULATED,
    val confidence: Double? = null,
    val source: ReportValueSource = ReportValueSource.UNKNOWN,
) {
    companion object {
        fun calculated(
            value: String,
            confidence: Double? = null,
            source: ReportValueSource = ReportValueSource.DETERMINISTIC,
        ): ReportTextValue = ReportTextValue(value, ReportValueStatus.CALCULATED, confidence, source)

        fun notCalculated(): ReportTextValue = ReportTextValue()
    }
}

@Serializable
data class ReportDoubleValue(
    val value: Double? = null,
    val unit: String? = null,
    val status: ReportValueStatus = ReportValueStatus.NOT_CALCULATED,
    val confidence: Double? = null,
    val source: ReportValueSource = ReportValueSource.UNKNOWN,
) {
    companion object {
        fun calculated(
            value: Double,
            unit: String? = null,
            confidence: Double? = null,
            source: ReportValueSource = ReportValueSource.DETERMINISTIC,
        ): ReportDoubleValue = ReportDoubleValue(value, unit, ReportValueStatus.CALCULATED, confidence, source)

        fun notCalculated(unit: String? = null): ReportDoubleValue = ReportDoubleValue(unit = unit)
    }
}

@Serializable
enum class ReportValueStatus {
    CALCULATED,
    DETECTED,
    INFERRED,
    NOT_DETECTED,
    NOT_CALCULATED,
    INSUFFICIENT_CONFIDENCE,
    FAILED,
}

@Serializable
enum class ReportValueSource {
    DETERMINISTIC,
    VISION_MODEL,
    MODEL_SUGGESTED,
    OCR,
    LOCAL_KNOWLEDGE,
    USER,
    IMPORTED_FILE,
    UNKNOWN,
}

@Serializable
data class ReportWarning(
    val code: String,
    val message: String,
    val severity: ReportSeverity,
    val stage: String? = null,
    val graphIndex: Int? = null,
    val peakNumber: Int? = null,
)

@Serializable
enum class ReportSeverity {
    INFO,
    WARNING,
    SERIOUS,
    FAILED,
}

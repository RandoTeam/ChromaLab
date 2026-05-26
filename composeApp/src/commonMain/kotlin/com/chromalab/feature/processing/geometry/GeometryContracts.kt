package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.axis.AxisLine
import com.chromalab.feature.processing.axis.AxisOrigin
import com.chromalab.feature.processing.curve.CurveMaskTextSuppressionRegion
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.peaks.PeakLabelEvidence
import kotlinx.serialization.Serializable

@Serializable
enum class SourceType {
    CAMERA,
    GALLERY,
    SCREENSHOT,
    VALIDATION_FIXTURE,
    ML_KIT_SCAN,
    UNKNOWN,
}

@Serializable
enum class GeometryStageStatus {
    SKIPPED_NOT_NEEDED,
    SKIPPED_NOT_CONFIDENT,
    APPLIED,
    FAILED,
}

@Serializable
enum class GeometryCandidateSource {
    CV,
    SCREENSHOT_EMBEDDED_CHART,
    ML_KIT,
    VLM_HINT,
    FULL_IMAGE_FALLBACK,
    USER,
}

@Serializable
enum class GeometryAxis {
    X,
    Y,
}

@Serializable
enum class TickDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    UNKNOWN,
}

@Serializable
enum class TickOcrEngine {
    ML_KIT,
    VLM,
    BOTH,
}

@Serializable
enum class TickOcrItemStatus {
    ACCEPTED,
    REJECTED,
    SEMANTIC_ONLY,
}

@Serializable
enum class CalibrationFitStatus {
    VALID,
    REVIEW,
    INVALID,
}

@Serializable
enum class TickLocalizationFailureSubreason {
    AXIS_LINE_MISSING,
    PLOT_FRAME_MISSING,
    TICK_MARKS_MISSING,
    LABEL_BAND_MISSING,
    OCR_NO_NUMERIC_TEXT,
    OCR_NUMERIC_NO_TICK_PIXEL,
    NON_MONOTONIC_TICK_VALUES,
    INSUFFICIENT_X_ANCHORS,
    INSUFFICIENT_Y_ANCHORS,
    HIGH_RESIDUALS,
    TITLE_OR_ION_TEXT_REJECTED,
    GRID_ONLY_NO_TICKS,
    LOW_RESOLUTION_LABELS_UNREADABLE,
}

@Serializable
enum class AxisScaleEvidenceType {
    EXPLICIT_TICK_MARK,
    GRID_LINE,
    OCR_LABEL_BOX,
    LABEL_PROJECTION,
    FRAME_ENDPOINT,
    REGULAR_SEQUENCE,
    AXIS_ENDPOINT,
    PLOT_FRAME_EDGE,
    OCR_VALUE_ONLY_REJECTED,
    SEMANTIC_TEXT_REJECTED,
}

@Serializable
enum class AxisScaleFailureSubreason {
    NUMERIC_LABELS_MISSING,
    NUMERIC_LABELS_UNREADABLE,
    LABEL_BOXES_FOUND_NO_GEOMETRY,
    LABEL_SEQUENCE_NON_MONOTONIC,
    LABEL_PROJECTION_FAILED,
    GRID_LINES_FOUND_LABELS_MISSING,
    TICK_MARKS_MISSING_BUT_LABELS_AVAILABLE,
    INSUFFICIENT_SCALE_ANCHORS,
    SCALE_FIT_HIGH_RESIDUAL,
    AXIS_FRAME_INCONSISTENT,
    TITLE_ION_TEXT_REJECTED_AS_SCALE_LABEL,
}

@Serializable
enum class CalibrationStrategyId {
    LEGACY_TICK_LOCALIZATION,
    AXIS_SCALE_RESOLVER,
    OCR_LABEL_BOX_DIRECT_FIT,
    GRID_FRAME_PROJECTION,
    REGULAR_SEQUENCE_FIT,
    FRAME_ENDPOINT_REVIEW_FALLBACK,
}

@Serializable
enum class CalibrationRejectionReason {
    INSUFFICIENT_ANCHORS,
    NON_MONOTONIC_VALUES,
    HIGH_RESIDUALS,
    NO_PIXEL_GEOMETRY,
    FORBIDDEN_TEXT_LABEL,
    AXIS_FRAME_INCONSISTENT,
    STRATEGY_NOT_APPLICABLE,
    REJECTED_INVALID_RESIDUAL,
    REJECTED_INSUFFICIENT_ANCHORS,
    REJECTED_FORBIDDEN_TEXT,
    REJECTED_NO_PIXEL_GEOMETRY,
    REJECTED_WORSE_THAN_SELECTED,
}

@Serializable
enum class CalibrationSelectionReason {
    VALID_STATUS,
    REVIEW_STATUS,
    LOWER_RESIDUALS,
    HIGHER_ANCHOR_COUNT,
    HIGHER_CONFIDENCE,
    LEGACY_REGRESSION_SHIELD,
    ONLY_AVAILABLE,
    SELECTED_VALID_LOW_RESIDUAL,
    SELECTED_REVIEW_LEGACY_FALLBACK,
    SELECTED_REVIEW_TWO_ANCHOR_FIT,
    SELECTED_REVIEW_BEST_AVAILABLE,
}

@Serializable
enum class GeometryReportStatus {
    SCIENTIFIC_READY,
    REVIEW_READY,
    DIAGNOSTIC_ONLY,
}

@Serializable
enum class GraphMultiplicityStatus {
    SINGLE_GRAPH,
    MULTI_GRAPH_VALID,
    MULTI_GRAPH_REVIEW,
    INVALID,
}

@Serializable
enum class GraphLayoutClass {
    SINGLE_TRACE_SINGLE_AXIS,
    DENSE_PEAK_SINGLE_AXIS,
    STACKED_TRACES_SHARED_AXIS,
    MULTI_PANEL_SEPARATE_AXES,
    TIC_PLUS_ION_PANELS,
    TWO_GRAPH_PAGE,
    EMBEDDED_SCREENSHOT_GRAPH,
    ROTATED_PAGE_GRAPH,
    LOW_RES_EXPORT_GRAPH,
    UNKNOWN_REVIEW,
}

@Serializable
enum class GraphPanelRejectionReason {
    DUPLICATE_IOU,
    NESTED_INSIDE_SELECTED_PANEL,
    SAME_AXIS_SYSTEM,
    SUBREGION_NOT_GRAPH_PANEL,
    LOW_FRAME_EVIDENCE,
    PEAK_CLUSTER_NOT_GRAPH,
    TEXT_ONLY_REGION,
}

@Serializable
data class GeometryPoint(
    val x: Float,
    val y: Float,
)

@Serializable
data class SourceProvenance(
    val originalImagePath: String,
    val normalizedImagePath: String,
    val sourceType: SourceType = SourceType.UNKNOWN,
    val originalWidth: Int,
    val originalHeight: Int,
    val normalizedWidth: Int,
    val normalizedHeight: Int,
    val exifRotation: Int = 0,
    val appliedRotation: Int = 0,
    val mlKitSmartScanUsed: Boolean = false,
    val mlKitCropFilterDeskewMetadataAvailable: Boolean = false,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class PageRectificationResult(
    val pageQuadPx: List<GeometryPoint>? = null,
    val homography: List<Float>? = null,
    val rectifiedImagePath: String,
    val perspectiveApplied: Boolean = false,
    val excessiveWarp: Boolean = false,
    val skewResidualPx: Float? = null,
    val straightnessScore: Float? = null,
    val confidence: Float = 0f,
    val status: GeometryStageStatus = GeometryStageStatus.SKIPPED_NOT_CONFIDENT,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class RoiCandidateScoreBreakdown(
    val axisVisibility: Float = 0f,
    val tickLabelVisibility: Float = 0f,
    val graphFramePlotRectangleConfidence: Float = 0f,
    val tracePixelDensity: Float = 0f,
    val textContaminationPenalty: Float = 0f,
    val marginSafety: Float = 0f,
    val aspectRatioPlausibility: Float = 0f,
    val calibrationViability: Float = 0f,
    val curveCoverageScore: Float = 0f,
    val containsYAxisRegionScore: Float = 0f,
    val containsYTickLabelsScore: Float = 0f,
    val containsXAxisRegionScore: Float = 0f,
    val containsXTickLabelsScore: Float = 0f,
    val titleOrIonPreservedScore: Float = 0f,
    val fullTraceHorizontalCoverageScore: Float = 0f,
    val leftMarginSafetyScore: Float = 0f,
    val bottomMarginSafetyScore: Float = 0f,
    val subregionPenalty: Float = 0f,
    val axisViabilityScore: Float = 0f,
    val calibrationViabilityScore: Float = 0f,
    val total: Float = 0f,
    val notes: List<String> = emptyList(),
)

@Serializable
data class GraphPanelBounds(
    val region: GraphRegion,
    val candidateSource: GeometryCandidateSource,
    val confidence: Float,
    val scoreBreakdown: RoiCandidateScoreBreakdown = RoiCandidateScoreBreakdown(),
    val overlayArtifactPath: String? = null,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class RejectedGraphPanelCandidate(
    val candidate: GraphPanelBounds,
    val rejectedAgainst: GraphPanelBounds? = null,
    val reasons: List<GraphPanelRejectionReason>,
    val iou: Float = 0f,
    val containmentRatio: Float = 0f,
    val notes: List<String> = emptyList(),
)

@Serializable
data class GraphMultiplicityResolution(
    val resolvedGraphPanels: List<GraphPanelBounds> = emptyList(),
    val rejectedDuplicatePanels: List<RejectedGraphPanelCandidate> = emptyList(),
    val rejectedNestedPanels: List<RejectedGraphPanelCandidate> = emptyList(),
    val rejectedSubregions: List<RejectedGraphPanelCandidate> = emptyList(),
    val multiplicityStatus: GraphMultiplicityStatus = GraphMultiplicityStatus.INVALID,
    val layoutClassification: GraphLayoutClassification? = null,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class GraphLayoutPanelGroup(
    val groupId: String,
    val panelIndexes: List<Int>,
    val sharedXAxis: Boolean = false,
    val sharedYAxis: Boolean = false,
    val notes: List<String> = emptyList(),
)

@Serializable
data class GraphLayoutClassification(
    val layoutClass: GraphLayoutClass,
    val physicalGraphCount: Int,
    val panelGroups: List<GraphLayoutPanelGroup> = emptyList(),
    val confidence: Float = 0f,
    val reviewReasons: List<String> = emptyList(),
)

@Serializable
data class PlotAreaBounds(
    val region: GraphRegion,
    val parentGraphPanelRegion: GraphRegion,
    val confidence: Float,
    val overlayArtifactPath: String? = null,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class RejectedLineCandidate(
    val line: AxisLine,
    val reason: String,
    val confidence: Float = 0f,
)

@Serializable
data class AxisGeometry(
    val xAxisLinePx: AxisLine? = null,
    val yAxisLinePx: AxisLine? = null,
    val originPx: AxisOrigin? = null,
    val axisConfidence: Float = 0f,
    val detectedFrameGridLines: List<AxisLine> = emptyList(),
    val rejectedLineCandidates: List<RejectedLineCandidate> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class TickPixelPosition(
    val pixelCoordinate: Float,
    val direction: TickDirection = TickDirection.UNKNOWN,
    val confidence: Float = 0f,
)

@Serializable
data class TickGeometry(
    val xTicks: List<TickPixelPosition> = emptyList(),
    val yTicks: List<TickPixelPosition> = emptyList(),
    val source: String = "deterministic_cv",
    val warnings: List<String> = emptyList(),
)

@Serializable
data class TickOcrItem(
    val axis: GeometryAxis,
    val tickPixelPosition: Float? = null,
    val localCropPath: String? = null,
    val rawText: String,
    val parsedNumericValue: Double? = null,
    val ocrEngine: TickOcrEngine = TickOcrEngine.ML_KIT,
    val confidence: Float = 0f,
    val status: TickOcrItemStatus = TickOcrItemStatus.REJECTED,
    val rejectionReason: String? = null,
)

@Serializable
data class TickOcrResult(
    val items: List<TickOcrItem> = emptyList(),
    val warnings: List<String> = emptyList(),
    val timestamp: Long = 0L,
) {
    val acceptedItems: List<TickOcrItem>
        get() = items.filter { it.status == TickOcrItemStatus.ACCEPTED }
}

@Serializable
data class TickLocalizationResult(
    val status: CalibrationFitStatus,
    val subreasons: List<TickLocalizationFailureSubreason> = emptyList(),
    val xTickCandidateCount: Int = 0,
    val yTickCandidateCount: Int = 0,
    val xAcceptedAnchorCount: Int = 0,
    val yAcceptedAnchorCount: Int = 0,
    val xRejectedAnchorCount: Int = 0,
    val yRejectedAnchorCount: Int = 0,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class AxisScaleAnchor(
    val axis: GeometryAxis,
    val pixelCoordinate: Float,
    val numericValue: Double?,
    val evidenceType: AxisScaleEvidenceType,
    val confidence: Float,
    val rawText: String? = null,
    val cropPath: String? = null,
    val projectionSource: String? = null,
    val rejectionReason: String? = null,
)

@Serializable
data class CalibrationAnchorEvidence(
    val axis: GeometryAxis,
    val tickPixelPosition: Float,
    val value: Double,
    val rawText: String? = null,
    val localCropPath: String? = null,
    val confidence: Float = 0f,
    val rejectionReason: String? = null,
    val evidenceType: AxisScaleEvidenceType = AxisScaleEvidenceType.EXPLICIT_TICK_MARK,
    val evidenceSource: String = "deterministic_tick",
    val projectionSource: String? = null,
)

@Serializable
data class AxisCalibrationFit(
    val axis: GeometryAxis,
    val acceptedAnchors: List<CalibrationAnchorEvidence> = emptyList(),
    val rejectedAnchors: List<CalibrationAnchorEvidence> = emptyList(),
    val model: String = "linear",
    val slope: Double? = null,
    val intercept: Double? = null,
    val residualsPx: List<Double> = emptyList(),
    val residualsUnit: List<Double> = emptyList(),
    val maxResidualPx: Double? = null,
    val rmsePx: Double? = null,
    val r2: Double? = null,
    val confidence: Float = 0f,
    val status: CalibrationFitStatus = CalibrationFitStatus.INVALID,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class CalibrationCandidate(
    val strategyId: CalibrationStrategyId,
    val axis: GeometryAxis,
    val fit: AxisCalibrationFit,
    val score: Float = 0f,
    val rejectionReasons: List<CalibrationRejectionReason> = emptyList(),
)

@Serializable
data class CalibrationStrategyResult(
    val strategyId: CalibrationStrategyId,
    val xCandidate: CalibrationCandidate,
    val yCandidate: CalibrationCandidate,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class CalibrationCandidateSet(
    val candidates: List<CalibrationCandidate> = emptyList(),
)

@Serializable
data class CalibrationArbitrationResult(
    val xFit: AxisCalibrationFit,
    val yFit: AxisCalibrationFit,
    val selectedXStrategy: CalibrationStrategyId? = null,
    val selectedYStrategy: CalibrationStrategyId? = null,
    val strategyResults: List<CalibrationStrategyResult> = emptyList(),
    val selectionReasons: List<CalibrationSelectionReason> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class AxisScaleResolutionResult(
    val status: CalibrationFitStatus,
    val xFit: AxisCalibrationFit = AxisCalibrationFit(axis = GeometryAxis.X),
    val yFit: AxisCalibrationFit = AxisCalibrationFit(axis = GeometryAxis.Y),
    val xAnchors: List<AxisScaleAnchor> = emptyList(),
    val yAnchors: List<AxisScaleAnchor> = emptyList(),
    val rejectedAnchors: List<AxisScaleAnchor> = emptyList(),
    val subreasons: List<AxisScaleFailureSubreason> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class GeometryStageTiming(
    val stageId: String,
    val durationMillis: Long,
)

@Serializable
data class GeometryTrace(
    val sourceProvenance: SourceProvenance? = null,
    val pageRectification: PageRectificationResult? = null,
    val roiCandidates: List<GraphPanelBounds> = emptyList(),
    val multiplicityResolution: GraphMultiplicityResolution? = null,
    val selectedGraphPanelBounds: GraphPanelBounds? = null,
    val selectedPlotAreaBounds: PlotAreaBounds? = null,
    val axisGeometry: AxisGeometry? = null,
    val tickGeometry: TickGeometry? = null,
    val tickOcrResult: TickOcrResult? = null,
    val axisScaleResolution: AxisScaleResolutionResult? = null,
    val calibrationArbitration: CalibrationArbitrationResult? = null,
    val xCalibrationFit: AxisCalibrationFit? = null,
    val yCalibrationFit: AxisCalibrationFit? = null,
    val originalImagePath: String? = null,
    val normalizedImagePath: String? = null,
    val rectifiedImagePath: String? = null,
    val selectedGraphPanelOverlayPath: String? = null,
    val selectedPlotAreaOverlayPath: String? = null,
    val plotAreaCropPath: String? = null,
    val axisOverlayPath: String? = null,
    val tickOverlayPath: String? = null,
    val ocrCropPaths: List<String> = emptyList(),
    val peakLabelEvidence: List<PeakLabelEvidence> = emptyList(),
    val peakLabelCropPaths: List<String> = emptyList(),
    val peakLabelCropBoundsOverlayPath: String? = null,
    val peakLabelTextClassificationOverlayPath: String? = null,
    val calibrationFitOverlayPath: String? = null,
    val curveMaskRawPath: String? = null,
    val curveMaskCleanPath: String? = null,
    val curveTextSuppressionOverlayPath: String? = null,
    val curveTextSuppressionRegions: List<CurveMaskTextSuppressionRegion> = emptyList(),
    val curveRejectedComponentsPath: String? = null,
    val curveSelectedComponentPath: String? = null,
    val curveSkeletonPath: String? = null,
    val finalCenterlineOverlayPath: String? = null,
    val warnings: List<String> = emptyList(),
    val timings: List<GeometryStageTiming> = emptyList(),
)

@Serializable
data class GeometryPipelineResult(
    val trace: GeometryTrace,
    val reportStatus: GeometryReportStatus,
    val graphPanelBounds: GraphPanelBounds? = null,
    val plotAreaBounds: PlotAreaBounds? = null,
    val axisGeometry: AxisGeometry? = null,
    val tickGeometry: TickGeometry? = null,
    val tickOcrResult: TickOcrResult? = null,
    val xCalibrationFit: AxisCalibrationFit? = null,
    val yCalibrationFit: AxisCalibrationFit? = null,
    val warnings: List<String> = emptyList(),
) {
    val isScientificReady: Boolean
        get() = reportStatus == GeometryReportStatus.SCIENTIFIC_READY

    val isReviewReady: Boolean
        get() = reportStatus == GeometryReportStatus.REVIEW_READY
}

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
enum class GeometryReportStatus {
    SCIENTIFIC_READY,
    REVIEW_READY,
    DIAGNOSTIC_ONLY,
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
data class CalibrationAnchorEvidence(
    val axis: GeometryAxis,
    val tickPixelPosition: Float,
    val value: Double,
    val rawText: String? = null,
    val localCropPath: String? = null,
    val confidence: Float = 0f,
    val rejectionReason: String? = null,
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
data class GeometryStageTiming(
    val stageId: String,
    val durationMillis: Long,
)

@Serializable
data class GeometryTrace(
    val sourceProvenance: SourceProvenance? = null,
    val pageRectification: PageRectificationResult? = null,
    val roiCandidates: List<GraphPanelBounds> = emptyList(),
    val selectedGraphPanelBounds: GraphPanelBounds? = null,
    val selectedPlotAreaBounds: PlotAreaBounds? = null,
    val axisGeometry: AxisGeometry? = null,
    val tickGeometry: TickGeometry? = null,
    val tickOcrResult: TickOcrResult? = null,
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

package com.chromalab.feature.processing.bench

import com.chromalab.feature.calculation.core.CalculationEngine
import com.chromalab.feature.calculation.core.CalculationParams
import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.calculation.core.PeakResult
import com.chromalab.feature.calculation.core.SignalModelBuilder
import com.chromalab.feature.calculation.algorithm.ConfidenceGrade
import com.chromalab.feature.calculation.algorithm.NoiseEstimator
import com.chromalab.feature.calculation.algorithm.NoiseMethod
import com.chromalab.feature.calculation.algorithm.OverlapStatus
import com.chromalab.feature.calculation.algorithm.PeakDetector
import com.chromalab.feature.processing.axis.AxisDetector
import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.calibration.CalibrationPoint
import com.chromalab.feature.processing.calibration.LinearCalibration
import com.chromalab.feature.processing.calibration.PixelCalibration
import com.chromalab.feature.processing.curve.CurveExtractor
import com.chromalab.feature.processing.curve.CurvePoint
import com.chromalab.feature.processing.curve.CurveMaskPreparer
import com.chromalab.feature.processing.curve.CurveTraceArtifactAudit
import com.chromalab.feature.processing.document.DocumentBounds
import com.chromalab.feature.processing.document.DocumentCorners
import com.chromalab.feature.processing.document.DocumentDetector
import com.chromalab.feature.processing.document.ImagePoint
import com.chromalab.feature.processing.geometry.CvQuadrilateralCandidate
import com.chromalab.feature.processing.geometry.CvQuadrilateralCandidateDetector
import com.chromalab.feature.processing.geometry.CvQuadrilateralCandidateKind
import com.chromalab.feature.processing.geometry.CvQuadrilateralCandidateResult
import com.chromalab.feature.processing.geometry.CvQuadrilateralInputRegion
import com.chromalab.feature.processing.graph.GraphCropBoundaryAnalyzer
import com.chromalab.feature.processing.graph.GraphCropBoundaryRisk
import com.chromalab.feature.processing.graph.GraphPlotAreaDetector
import com.chromalab.feature.processing.graph.GraphPlotAreaDetectionResult
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.graph.GraphRegionBoundaryCorrector
import com.chromalab.feature.processing.graph.GraphRegionDetector
import com.chromalab.feature.processing.graph.GraphRegionQuality
import com.chromalab.feature.processing.graph.GraphRegionRefiner
import com.chromalab.feature.processing.graph.GraphRegionRefinementResult
import com.chromalab.feature.processing.graph.requiresGraphPanelBoundaryMode
import com.chromalab.feature.processing.normalize.ImageOrientationCorrectionResult
import com.chromalab.feature.processing.normalize.ImageOrientationCorrector
import com.chromalab.feature.processing.normalize.ImageNormalizer
import com.chromalab.feature.processing.normalize.NormalizedImageResult
import com.chromalab.feature.processing.ocr.AxisOcrReader
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrTextElement
import com.chromalab.feature.processing.ocr.OcrStatus
import com.chromalab.feature.processing.preprocess.ImagePreprocessor
import com.chromalab.feature.processing.preprocess.PreprocessingResult
import com.chromalab.feature.processing.preprocess.PreprocessingVariantRanker
import com.chromalab.feature.processing.preprocess.PreprocessingVariantScore
import com.chromalab.feature.processing.preprocess.variants
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.signal.SignalConverter
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Serializable
data class OfflineAnalysisInput(
    val sourceId: String,
    val imagePath: String,
    val outputDir: String,
    val expectedGraphCount: Int? = null,
    val manualAxisCalibrations: List<OfflineManualAxisCalibrationInput> = emptyList(),
    val peakSanityExpectations: List<OfflinePeakSanityExpectationInput> = emptyList(),
)

@Serializable
data class OfflineManualAxisCalibrationInput(
    val graphIndex: Int,
    val xPoints: List<OfflineManualAxisCalibrationPointInput>,
    val yPoints: List<OfflineManualAxisCalibrationPointInput>,
    val xUnit: String? = null,
    val yUnit: String? = null,
)

@Serializable
data class OfflineManualAxisCalibrationPointInput(
    val pixel: Float,
    val value: Float,
    val label: String? = null,
)

@Serializable
data class OfflinePeakSanityExpectationInput(
    val graphIndex: Int,
    val expectedApexTimes: List<Double> = emptyList(),
    val apexTolerance: Double = 0.0,
    val minPeakCount: Int? = null,
    val lockExpectedApexTimes: Boolean = true,
)

@Serializable
data class OfflineAnalysisAudit(
    val sourceId: String,
    val imagePath: String,
    val outputDir: String,
    val normalizedImagePath: String?,
    val orientationCorrection: OfflineOrientationCorrectionAudit?,
    val perspectiveGeometry: OfflinePerspectiveGeometryAudit = OfflinePerspectiveGeometryAudit(),
    val imageWidth: Int?,
    val imageHeight: Int?,
    val expectedGraphCount: Int?,
    val detectedGraphCount: Int,
    val graphCandidates: List<OfflineGraphCandidateAudit>,
    val graphs: List<OfflineGraphAudit>,
    val stages: List<OfflineStageAudit>,
    val reportContract: OfflineReportContractAudit = OfflineReportContractAudit(),
    val warnings: List<String>,
    val blockedAtStage: String?,
    val readyForCalculation: Boolean,
)

@Serializable
data class OfflineReportContractAudit(
    val ready: Boolean = false,
    val graphCount: Int = 0,
    val sectionCount: Int = 0,
    val readySectionCount: Int = 0,
    val warningSectionCount: Int = 0,
    val blockedSectionCount: Int = 0,
    val sections: List<OfflineReportContractSectionAudit> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OfflineReportContractSectionAudit(
    val graphIndex: Int? = null,
    val section: String,
    val status: OfflineReportContractSectionStatus,
    val missingFields: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@Serializable
enum class OfflineReportContractSectionStatus {
    READY,
    WARNING,
    BLOCKED,
}

@Serializable
data class OfflineOrientationCorrectionAudit(
    val imagePath: String,
    val wasRotated: Boolean,
    val rotationDegrees: Int,
    val horizontalRunCount: Int,
    val verticalRunCount: Int,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OfflinePerspectiveGeometryAudit(
    val ready: Boolean = false,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val documentDetected: Boolean = false,
    val documentTrusted: Boolean = false,
    val documentMethod: String? = null,
    val documentConfidence: Float = 0f,
    val documentAreaRatio: Float = 0f,
    val documentAspectRatio: Float = 0f,
    val documentQuadrilateral: Boolean = false,
    val documentCorners: DocumentCorners? = null,
    val correctedCorners: DocumentCorners? = null,
    val graphPanelCount: Int = 0,
    val plotAreaCount: Int = 0,
    val plotGeometryReady: Boolean = false,
    val perspectiveTransformRequired: Boolean = false,
    val perspectiveApplied: Boolean = false,
    val normalizedCornerDisplacement: Float = 0f,
    val maxSkewAngleDegrees: Float = 0f,
    val candidates: List<OfflineGeometryQuadrilateralCandidateAudit> = emptyList(),
    val residualMetrics: OfflinePerspectiveResidualMetricsAudit = OfflinePerspectiveResidualMetricsAudit(),
    val residualMetricsRequired: Boolean = true,
    val warnings: List<String> = emptyList(),
)

@Serializable
enum class OfflineGeometryCandidateKind {
    DOCUMENT,
    GRAPH_PANEL,
    PLOT_AREA,
}

@Serializable
data class OfflineGeometryQuadrilateralCandidateAudit(
    val kind: OfflineGeometryCandidateKind,
    val graphIndex: Int? = null,
    val source: String,
    val bounds: GraphRegion,
    val corners: DocumentCorners,
    val accepted: Boolean,
    val areaRatio: Float,
    val aspectRatio: Float,
    val normalizedCornerDisplacement: Float,
    val maxSkewAngleDegrees: Float,
    val maxSideStraightnessResidualPx: Float,
    val orthogonalityResidualDegrees: Float,
    val score: Float,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OfflinePerspectiveResidualMetricsAudit(
    val candidateCount: Int = 0,
    val acceptedCandidateCount: Int = 0,
    val documentCandidateCount: Int = 0,
    val graphPanelCandidateCount: Int = 0,
    val plotAreaCandidateCount: Int = 0,
    val acceptedPlotAreaCandidateCount: Int = 0,
    val maxNormalizedCornerDisplacement: Float = 0f,
    val maxSkewAngleDegrees: Float = 0f,
    val maxOrthogonalityResidualDegrees: Float = 0f,
    val maxSideStraightnessResidualPx: Float = 0f,
    val minAcceptedPlotAreaRatio: Float = 0f,
    val maxAcceptedPlotAreaRatio: Float = 0f,
    val residualQualityReady: Boolean = false,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OfflineGraphCandidateAudit(
    val graphIndex: Int,
    val region: GraphRegion,
    val accepted: Boolean,
    val areaRatio: Float,
    val aspectRatio: Float,
    val rejectionReasons: List<String>,
)

@Serializable
data class OfflineGraphAudit(
    val graphIndex: Int,
    val originalRegion: GraphRegion,
    val region: GraphRegion,
    val refinement: OfflineGraphRefinementAudit,
    val cropQuality: OfflineGraphCropQualityAudit,
    val cropBoundaryRisk: OfflineGraphCropBoundaryRiskAudit,
    val plotArea: OfflineGraphPlotAreaAudit,
    val selectedPreprocessingVariant: String?,
    val selectedPreprocessingImagePath: String?,
    val preprocessingVariantScores: List<PreprocessingVariantScore>,
    val ocrStatus: OcrStatus,
    val xSuggestionCount: Int,
    val ySuggestionCount: Int,
    val axesDetected: Boolean,
    val originDetected: Boolean,
    val axisConfidence: Float,
    val axisCalibration: OfflineAxisCalibrationAudit,
    val curveMaskAvailable: Boolean,
    val curveMaskRawPixelCount: Int,
    val curveMaskCleanPixelCount: Int,
    val curveMaskSuppressionApplied: List<String>,
    val traceArtifacts: CurveTraceArtifactAudit,
    val curvePointCount: Int,
    val curveCoverage: Float,
    val curveUsable: Boolean,
    val signal: OfflineSignalAudit,
    val peakDetection: OfflinePeakDetectionAudit,
    val peakMetrics: OfflinePeakMetricsAudit,
    val peakSanity: OfflinePeakSanityAudit,
    val warnings: List<String>,
)

@Serializable
data class OfflineSignalAudit(
    val ready: Boolean,
    val pointCount: Int,
    val timeStart: Float?,
    val timeEnd: Float?,
    val timeRange: Float,
    val intensityMin: Float?,
    val intensityMax: Float?,
    val intensityRange: Float,
    val duplicateCount: Int,
    val gapCount: Int,
    val sortValid: Boolean,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OfflinePeakDetectionAudit(
    val ready: Boolean,
    val peakCount: Int,
    val significantPeakCount: Int,
    val candidateCount: Int? = null,
    val rejectedCandidateCount: Int? = null,
    val dominantPeakTime: Double?,
    val dominantPeakHeight: Double?,
    val dominantPeakAreaPercent: Double?,
    val detectionSignalSource: String? = null,
    val detectionProfile: String? = null,
    val noiseLevel: Double? = null,
    val noiseMethod: String? = null,
    val basePeakCount: Int? = null,
    val tunedPeakCount: Int? = null,
    val controlledTuningApplied: Boolean = false,
    val controlledTuningReason: String? = null,
    val guardedQualityReview: OfflineGuardedPeakQualityAudit = OfflineGuardedPeakQualityAudit(),
    val sparseTraceQualityReview: OfflineSparseTracePeakQualityAudit = OfflineSparseTracePeakQualityAudit(),
    val thresholdRelaxationAllowed: Boolean? = null,
    val thresholdRelaxationGuardReason: String? = null,
    val baselineMethod: String?,
    val boundaryMethod: String?,
    val integrationMethod: String?,
    val clampNegative: Boolean?,
    val maxPeakWidth: Int?,
    val minSnr: Double?,
    val peaks: List<OfflinePeakAudit> = emptyList(),
    val rejectionReasons: List<OfflinePeakRejectionAudit> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OfflinePeakRejectionAudit(
    val reason: String,
    val count: Int,
)

@Serializable
data class OfflineGuardedPeakQualityAudit(
    val available: Boolean = false,
    val reviewPeakCount: Int = 0,
    val lowDefaultSnrCount: Int = 0,
    val lowAreaShareCount: Int = 0,
    val narrowBoundaryCount: Int = 0,
    val acceptedForGuardedCompleteness: Boolean = false,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OfflineSparseTracePeakQualityAudit(
    val available: Boolean = false,
    val sparseTrace: Boolean = false,
    val localizedSparseTrace: Boolean = false,
    val reviewPeakCount: Int = 0,
    val lowSnrCount: Int = 0,
    val lowAreaShareCount: Int = 0,
    val lowConfidenceCount: Int = 0,
    val overlapReviewCount: Int = 0,
    val requiresReportConfidenceText: Boolean = false,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OfflinePeakAudit(
    val peakNumber: Int,
    val rtApex: Double,
    val leftBoundaryTime: Double,
    val rightBoundaryTime: Double,
    val height: Double,
    val area: Double,
    val areaPercent: Double,
    val widthBase: Double,
    val widthHalfHeight: Double?,
    val tailingFactor: Double,
    val asymmetryFactor: Double,
    val snr: Double,
    val confidence: String,
    val overlapStatus: String,
    val assignment: OfflinePeakAssignmentAudit = OfflinePeakAssignmentAudit(),
    val warningCount: Int,
    val qualityFlags: List<String> = emptyList(),
)

@Serializable
data class OfflinePeakAssignmentAudit(
    val probableCompoundName: String? = null,
    val probableCompoundStatus: String = REPORT_VALUE_NOT_CALCULATED,
    val formula: String? = null,
    val formulaStatus: String = REPORT_VALUE_NOT_CALCULATED,
    val compoundClass: String? = null,
    val compoundClassStatus: String = REPORT_VALUE_NOT_CALCULATED,
    val carbonNumber: String? = null,
    val carbonNumberStatus: String = REPORT_VALUE_NOT_CALCULATED,
    val kovatsIndex: Double? = null,
    val kovatsIndexStatus: String = REPORT_VALUE_NOT_CALCULATED,
    val assignmentSource: String? = null,
)

@Serializable
data class OfflinePeakMetricsAudit(
    val ready: Boolean,
    val orderedByRetentionTime: Boolean,
    val totalAbsArea: Double,
    val areaPercentSum: Double,
    val maximumHeight: Double?,
    val firstPeakTime: Double?,
    val lastPeakTime: Double?,
    val minBoundaryWidth: Double?,
    val maxBoundaryWidth: Double?,
    val invalidNumericCount: Int,
    val invalidBoundaryCount: Int,
    val nonPositiveAreaCount: Int,
    val nonPositiveHeightCount: Int,
    val missingWidthCount: Int,
    val lowSnrCount: Int,
    val lowConfidenceCount: Int,
    val unresolvedOverlapCount: Int,
    val peakWarningCount: Int,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OfflinePeakSanityAudit(
    val ready: Boolean,
    val expectationProvided: Boolean,
    val minPeakCount: Int?,
    val expectedApexTimes: List<Double>,
    val detectedExpectedPeakCount: Int,
    val missingExpectedApexTimes: List<Double>,
    val unexpectedPeakCount: Int,
    val apexTolerance: Double,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OfflineAxisCalibrationAudit(
    val ready: Boolean,
    val xReady: Boolean,
    val yReady: Boolean,
    val source: OfflineAxisCalibrationSource,
    val xCandidateCount: Int,
    val yCandidateCount: Int,
    val xPixelSpan: Float,
    val yPixelSpan: Float,
    val xValueSpan: Float,
    val yValueSpan: Float,
    val xUnit: String?,
    val yUnit: String?,
    val xCandidates: List<OfflineAxisCalibrationPointAudit>,
    val yCandidates: List<OfflineAxisCalibrationPointAudit>,
    val warnings: List<String> = emptyList(),
)

@Serializable
enum class OfflineAxisCalibrationSource {
    CONFIRMED,
    MANUAL_CONFIRMED,
    OCR_CANDIDATE_REQUIRES_REVIEW,
    MANUAL_REQUIRED,
}

@Serializable
data class OfflineAxisCalibrationPointAudit(
    val pixel: Float,
    val value: Float,
    val text: String,
    val confidence: Float,
)

@Serializable
data class OfflineGraphRefinementAudit(
    val changed: Boolean,
    val areaReductionRatio: Float,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OfflineGraphCropQualityAudit(
    val areaRatio: Float,
    val originalAreaRatio: Float,
    val edgeContactCount: Int,
    val touchesTop: Boolean,
    val touchesRight: Boolean,
    val touchesBottom: Boolean,
    val touchesLeft: Boolean,
    val fullImage: Boolean,
    val largeFullImage: Boolean,
    val broadEdgeCrop: Boolean,
    val possibleRotatedPage: Boolean,
    val originalBroadContext: Boolean,
    val refinementOnlyEdgeTrim: Boolean,
    val unresolvedBroadContext: Boolean,
    val rightAngleRotationSuspected: Boolean,
    val acceptedForCalculation: Boolean,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OfflineGraphCropBoundaryRiskAudit(
    val topSignalClippingRisk: Boolean,
    val topTouchingDarkRunCount: Int,
    val topDarkPixelRatio: Float,
    val acceptedForCalculation: Boolean,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OfflineGraphPlotAreaAudit(
    val region: GraphRegion?,
    val detected: Boolean,
    val areaRatioWithinPanel: Float,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class OfflineStageAudit(
    val stage: String,
    val graphIndex: Int? = null,
    val status: OfflineStageStatus,
    val startedAtMillis: Long,
    val durationMillis: Long,
    val message: String,
    val warnings: List<String> = emptyList(),
)

enum class OfflineStageStatus {
    SUCCESS,
    FAILED,
    SKIPPED,
}

class OfflineAnalysisRunner(
    private val normalizer: ImageNormalizer = ImageNormalizer(),
    private val orientationCorrector: ImageOrientationCorrector = ImageOrientationCorrector(),
    private val documentDetector: DocumentDetector = DocumentDetector(),
    private val preprocessor: ImagePreprocessor = ImagePreprocessor(),
    private val graphRefiner: GraphRegionRefiner = GraphRegionRefiner(),
    private val graphBoundaryCorrector: GraphRegionBoundaryCorrector = GraphRegionBoundaryCorrector(),
    private val cropBoundaryAnalyzer: GraphCropBoundaryAnalyzer = GraphCropBoundaryAnalyzer(),
    private val plotAreaDetector: GraphPlotAreaDetector = GraphPlotAreaDetector(),
    private val cvQuadrilateralDetector: CvQuadrilateralCandidateDetector = CvQuadrilateralCandidateDetector(),
    private val variantRanker: PreprocessingVariantRanker = PreprocessingVariantRanker(),
    private val graphDetector: GraphRegionDetector = GraphRegionDetector(),
    private val ocrReader: AxisOcrReader = AxisOcrReader(),
    private val axisDetector: AxisDetector = AxisDetector(),
    private val curveMaskPreparer: CurveMaskPreparer = CurveMaskPreparer(),
    private val curveExtractor: CurveExtractor = CurveExtractor(),
) {

    suspend fun run(input: OfflineAnalysisInput): OfflineAnalysisAudit {
        val stages = mutableListOf<OfflineStageAudit>()
        val warnings = mutableListOf<String>()

        val normalized = runStage(
            stage = "normalize",
            stages = stages,
        ) {
            normalizer.normalize(input.imagePath, "${input.outputDir}/normalize")
                ?: error("Image could not be loaded or normalized.")
        } ?: return input.blockedAudit(
            stages = stages,
            warnings = warnings,
            blockedAtStage = "normalize",
        )

        val documentBounds = runNullableStage(
            stage = "document_detect",
            skippedMessage = "Document bounds were not detected.",
            stages = stages,
        ) {
            documentDetector.detect(normalized.normalizedPath)
        }
        if (documentBounds == null) {
            warnings += "document.bounds_not_detected"
        } else if (documentBounds.confidence <= 0f) {
            warnings += "document.detector_low_confidence"
        }

        val orientation = runStage(
            stage = "orientation_correct",
            stages = stages,
            successMessage = { result ->
                "rotated=${result.wasRotated}, degrees=${result.rotationDegrees}, horizontalRuns=${result.horizontalRunCount}, verticalRuns=${result.verticalRunCount}."
            },
        ) {
            orientationCorrector.correct(
                normalized = normalized,
                outputDir = "${input.outputDir}/orientation",
            )
        } ?: normalized.toOrientationCorrectionResult(
            warnings = listOf("orientation.stage_failed"),
        )
        warnings += orientation.warnings

        val preprocessing = runStage(
            stage = "preprocess",
            stages = stages,
        ) {
            preprocessor.preprocess(
                imagePath = orientation.imagePath,
                outputDir = "${input.outputDir}/preprocess",
            ) ?: error("Image preprocessing did not produce artifacts.")
        } ?: return input.blockedAudit(
            stages = stages,
            warnings = warnings,
            blockedAtStage = "preprocess",
            normalizedImagePath = normalized.normalizedPath,
            orientationCorrection = orientation.toAudit(),
            imageWidth = orientation.width,
            imageHeight = orientation.height,
        )

        val graphResult = runStage(
            stage = "graph_region",
            stages = stages,
        ) {
            graphDetector.detect(
                imagePath = preprocessing.contrastEnhancedPath,
                imageWidth = orientation.width,
                imageHeight = orientation.height,
            )
        } ?: return input.blockedAudit(
            stages = stages,
            warnings = warnings,
            blockedAtStage = "graph_region",
            normalizedImagePath = normalized.normalizedPath,
            orientationCorrection = orientation.toAudit(),
            imageWidth = orientation.width,
            imageHeight = orientation.height,
        )

        val graphCandidates = graphResult.qualityEvaluations.mapIndexed { index, quality ->
            quality.toAudit(index + 1)
        }
        val filteredRegions = graphResult.filteredRegions
        val selectedRegions = if (filteredRegions.isNotEmpty()) {
            if (orientation.wasRotated) {
                val nonEdgeRegions = filteredRegions.filter {
                    it.edgeContactCount(orientation.width, orientation.height) == 0
                }
                if (nonEdgeRegions.isNotEmpty() && nonEdgeRegions.size < filteredRegions.size) {
                    warnings += "graph.orientation_filtered_edge_context_candidates"
                    nonEdgeRegions
                } else {
                    filteredRegions
                }
            } else {
                filteredRegions
            }
        } else {
            warnings += "graph.no_accepted_candidate_using_full_image"
            listOf(GraphRegion(0, 0, orientation.width, orientation.height, "Full image fallback"))
        }

        if (graphResult.sortedRegions.size == 1 && graphResult.effectiveRegion.isFullImage(orientation.width, orientation.height)) {
            warnings += "graph.full_image_region_selected"
        }
        if (input.expectedGraphCount != null && selectedRegions.size != input.expectedGraphCount) {
            warnings += "graph.count_mismatch.expected_${input.expectedGraphCount}_actual_${selectedRegions.size}"
        }

        val preservePanelLabelsForRun = orientation.wasRotated ||
            selectedRegions.any { it.requiresGraphPanelBoundaryMode(orientation.width, orientation.height) }

        val correctedRegions = selectedRegions.mapIndexed { index, region ->
            runStage(
                stage = "graph_boundary",
                graphIndex = index + 1,
                stages = stages,
                successMessage = { result ->
                    "changed=${result.changed}, region=${result.correctedRegion.x},${result.correctedRegion.y} ${result.correctedRegion.width}x${result.correctedRegion.height}."
                },
            ) {
                graphBoundaryCorrector.correct(
                    imagePath = orientation.imagePath,
                    region = region,
                    imageWidth = orientation.width,
                    imageHeight = orientation.height,
                    preservePanelLabels = preservePanelLabelsForRun,
                )
            }?.also { result ->
                warnings += result.warnings.map { "graph_${index + 1}.$it" }
            }?.correctedRegion ?: region
        }

        val refinedRegions = correctedRegions.mapIndexed { index, region ->
            runStage(
                stage = "graph_refine",
                graphIndex = index + 1,
                stages = stages,
                successMessage = { result ->
                    "changed=${result.changed}, reduction=${result.areaReductionRatio}."
                },
            ) {
                graphRefiner.refine(
                    imagePath = preprocessing.contrastEnhancedPath,
                    region = region,
                    imageWidth = orientation.width,
                    imageHeight = orientation.height,
                )
            } ?: GraphRegionRefinementResult(
                originalRegion = region,
                refinedRegion = region,
                changed = false,
                areaReductionRatio = 0f,
                warnings = listOf("graph_refine.failed"),
            )
        }

        val manualCalibrationsByGraph = input.manualAxisCalibrations.associateBy { it.graphIndex }
        val peakSanityByGraph = input.peakSanityExpectations.associateBy { it.graphIndex }
        val graphAudits = refinedRegions.mapIndexed { index, refinement ->
            auditGraph(
                graphIndex = index + 1,
                preprocessing = preprocessing,
                outputDir = "${input.outputDir}/graph_${index + 1}",
                refinement = refinement,
                imageWidth = orientation.width,
                imageHeight = orientation.height,
                manualCalibration = manualCalibrationsByGraph[index + 1],
                peakSanityExpectation = peakSanityByGraph[index + 1],
                stages = stages,
            )
        }

        val perspectiveGeometry = runStage(
            stage = "perspective_geometry",
            stages = stages,
            successMessage = { result ->
                "ready=${result.ready}, trustedDocument=${result.documentTrusted}, graphs=${result.graphPanelCount}, plots=${result.plotAreaCount}."
            },
        ) {
            buildPerspectiveGeometryAudit(
                documentBounds = documentDetector.detect(orientation.imagePath),
                imageWidth = orientation.width,
                imageHeight = orientation.height,
                graphAudits = graphAudits,
                cvCandidates = cvQuadrilateralDetector.detect(
                    imagePath = orientation.imagePath,
                    imageWidth = orientation.width,
                    imageHeight = orientation.height,
                    graphRegions = graphAudits.map { graph ->
                        CvQuadrilateralInputRegion(
                            graphIndex = graph.graphIndex,
                            panelRegion = graph.region,
                            plotRegion = graph.plotArea.region,
                        )
                    },
                ),
            )
        } ?: OfflinePerspectiveGeometryAudit(
            imageWidth = orientation.width,
            imageHeight = orientation.height,
            graphPanelCount = graphAudits.size,
            plotAreaCount = graphAudits.count { it.plotArea.detected && it.plotArea.region != null },
            warnings = listOf("perspective_geometry.stage_failed"),
        )

        val cropReady = graphAudits.isNotEmpty() &&
            graphAudits.all { it.cropQuality.acceptedForCalculation && it.cropBoundaryRisk.acceptedForCalculation }
        val plotAreaReady = graphAudits.isNotEmpty() && graphAudits.all { it.plotArea.detected && it.plotArea.region != null }
        val axisGeometryReady = graphAudits.isNotEmpty() &&
            graphAudits.all { it.axesDetected && it.originDetected }
        val axisCalibrationReady = graphAudits.isNotEmpty() &&
            graphAudits.all { it.axisCalibration.ready }
        val curveReady = graphAudits.isNotEmpty() && graphAudits.all { it.curveUsable }
        val signalReady = graphAudits.isNotEmpty() && graphAudits.all { it.signal.ready }
        val peakDetectionReady = graphAudits.isNotEmpty() && graphAudits.all { it.peakDetection.ready }
        val peakMetricsReady = graphAudits.isNotEmpty() && graphAudits.all { it.peakMetrics.ready }
        val peakSanityReady = graphAudits.isNotEmpty() && graphAudits.all { it.peakSanity.ready }
        val readyForCalculation = cropReady &&
            plotAreaReady &&
            axisGeometryReady &&
            axisCalibrationReady &&
            curveReady &&
            signalReady &&
            peakDetectionReady &&
            peakMetricsReady &&
            peakSanityReady
        if (!readyForCalculation) {
            stages += skippedStage(
                stage = "calculation",
                message = when {
                    !cropReady -> "Calculation is blocked until every graph has accepted crop bounds."
                    !plotAreaReady -> "Calculation is blocked until every graph has audited plot-area bounds."
                    !axisGeometryReady -> "Calculation is blocked until every graph has detected axis geometry and origin."
                    !axisCalibrationReady -> "Calculation is blocked until every graph has confirmed pixel-to-axis calibration."
                    !curveReady -> "Calculation is blocked until every graph has usable extracted curve data."
                    !signalReady -> "Calculation is blocked until every graph has calibrated signal data."
                    !peakDetectionReady -> "Calculation is blocked until every graph has detected peaks from calibrated signal data."
                    !peakMetricsReady -> "Calculation is blocked until every graph has auditable peak metrics and integration output."
                    else -> "Calculation is blocked until every graph passes peak sanity expectations."
                },
            )
            stages += skippedStage(
                stage = "report_validation",
                message = "Report validation is blocked until calculation output exists.",
            )
        }
        val auditWarnings = warnings + graphAudits.flatMap { graph -> graph.warnings.map { "graph_${graph.graphIndex}.$it" } }
        val reportContractAudit = if (readyForCalculation) {
            runStage(
                stage = "report_validation",
                stages = stages,
                successMessage = { result ->
                    "ready=${result.ready}, sections=${result.sectionCount}, blocked=${result.blockedSectionCount}, warnings=${result.warningSectionCount}."
                },
            ) {
                buildOfflineReportContractAudit(
                    imageWidth = orientation.width,
                    imageHeight = orientation.height,
                    expectedGraphCount = input.expectedGraphCount,
                    detectedGraphCount = selectedRegions.size,
                    graphAudits = graphAudits,
                    stages = stages,
                    warnings = auditWarnings,
                )
            } ?: OfflineReportContractAudit(
                warnings = listOf("report_validation.stage_failed"),
            )
        } else {
            buildOfflineReportContractAudit(
                imageWidth = orientation.width,
                imageHeight = orientation.height,
                expectedGraphCount = input.expectedGraphCount,
                detectedGraphCount = selectedRegions.size,
                graphAudits = graphAudits,
                stages = stages,
                warnings = auditWarnings,
            )
        }

        return OfflineAnalysisAudit(
            sourceId = input.sourceId,
            imagePath = input.imagePath,
            outputDir = input.outputDir,
            normalizedImagePath = normalized.normalizedPath,
            orientationCorrection = orientation.toAudit(),
            perspectiveGeometry = perspectiveGeometry,
            imageWidth = orientation.width,
            imageHeight = orientation.height,
            expectedGraphCount = input.expectedGraphCount,
            detectedGraphCount = selectedRegions.size,
            graphCandidates = graphCandidates,
            graphs = graphAudits,
            stages = stages,
            reportContract = reportContractAudit,
            warnings = auditWarnings,
            blockedAtStage = when {
                readyForCalculation -> null
                !cropReady -> "crop_quality"
                !plotAreaReady -> "plot_area"
                !axisGeometryReady -> "axis_detect"
                !axisCalibrationReady -> "axis_calibration"
                !curveReady -> "curve_extract"
                !signalReady -> "signal_convert"
                !peakDetectionReady -> "peak_detection"
                !peakMetricsReady -> "peak_metrics"
                else -> "peak_sanity"
            },
            readyForCalculation = readyForCalculation,
        )
    }

    private suspend fun auditGraph(
        graphIndex: Int,
        preprocessing: PreprocessingResult,
        outputDir: String,
        refinement: GraphRegionRefinementResult,
        imageWidth: Int,
        imageHeight: Int,
        manualCalibration: OfflineManualAxisCalibrationInput?,
        peakSanityExpectation: OfflinePeakSanityExpectationInput?,
        stages: MutableList<OfflineStageAudit>,
    ): OfflineGraphAudit {
        val graphWarnings = mutableListOf<String>()
        val region = refinement.refinedRegion
        graphWarnings += refinement.warnings
        val cropQuality = refinement.cropQualityAudit(imageWidth, imageHeight)
        graphWarnings += cropQuality.warnings

        val variantScores = runStage(
            stage = "preprocess_rank",
            graphIndex = graphIndex,
            stages = stages,
            successMessage = { scores ->
                val selected = scores.firstOrNull { it.selected }
                "selected=${selected?.variantId ?: "none"}, variants=${scores.size}."
            },
        ) {
            variantRanker.rank(preprocessing.variants(), region)
        }.orEmpty()
        val selectedVariant = variantScores.firstOrNull { it.selected } ?: variantScores.firstOrNull()
        val analysisImagePath = selectedVariant?.imagePath ?: preprocessing.contrastEnhancedPath
        val ocrImagePath = preprocessing.sourcePath
        if (variantScores.isEmpty()) {
            graphWarnings += "preprocess_variant_ranking_not_available"
        }

        val cropBoundaryRisk = runStage(
            stage = "crop_boundary_risk",
            graphIndex = graphIndex,
            stages = stages,
            successMessage = { risk ->
                "topClipping=${risk.topSignalClippingRisk}, topRuns=${risk.topTouchingDarkRunCount}."
            },
        ) {
            cropBoundaryAnalyzer.analyze(analysisImagePath, region)
        } ?: GraphCropBoundaryRisk(
            topSignalClippingRisk = false,
            topTouchingDarkRunCount = 0,
            topDarkPixelRatio = 0f,
            acceptedForCalculation = false,
            warnings = listOf("crop_boundary.failed"),
        )
        graphWarnings += cropBoundaryRisk.warnings

        val plotAreaResult = runStage(
            stage = "plot_area",
            graphIndex = graphIndex,
            stages = stages,
            successMessage = { result ->
                "detected=${result.detected}, region=${result.plotArea?.let { "${it.x},${it.y} ${it.width}x${it.height}" } ?: "none"}."
            },
        ) {
            plotAreaDetector.detect(
                imagePath = analysisImagePath,
                panelRegion = region,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
            )
        }
        if (plotAreaResult?.detected != true) {
            graphWarnings += "plot_area_not_detected"
        }
        graphWarnings += plotAreaResult?.warnings.orEmpty()
        val plotAreaAudit = plotAreaResult.toAudit(region)
        val calculationRegion = plotAreaResult?.plotArea ?: region

        val ocrResult = runStage(
            stage = "axis_ocr",
            graphIndex = graphIndex,
            stages = stages,
            successMessage = { result ->
                "OCR status=${result.status}, x=${result.suggestedXValues.size}, y=${result.suggestedYValues.size}."
            },
        ) {
            ocrReader.readAxisLabels(ocrImagePath, region)
        }
        if (ocrResult == null || ocrResult.status == OcrStatus.NOT_AVAILABLE) {
            graphWarnings += "axis_ocr_not_available"
        }
        graphWarnings += ocrResult?.warnings.orEmpty()

        val axesResult = runStage(
            stage = "axis_detect",
            graphIndex = graphIndex,
            stages = stages,
            successMessage = { result ->
                "axesDetected=${result.hasAxes}, originDetected=${result.hasOrigin}."
            },
        ) {
            axisDetector.detect(analysisImagePath, calculationRegion)
        }
        if (axesResult?.hasAxes != true) {
            graphWarnings += "axes_not_detected"
        }
        graphWarnings += axesResult?.warnings.orEmpty()

        val axisCalibration = runStage(
            stage = "axis_calibration",
            graphIndex = graphIndex,
            stages = stages,
            successMessage = { result ->
                "ready=${result.ready}, source=${result.source}, x=${result.xCandidateCount}, y=${result.yCandidateCount}."
            },
        ) {
            buildAxisCalibrationAudit(
                manualCalibration = manualCalibration,
                ocrResult = ocrResult,
                axesResult = axesResult,
                panelRegion = region,
                plotRegion = calculationRegion,
            )
        } ?: missingAxisCalibration()
        graphWarnings += axisCalibration.warnings

        val maskResult = runStage(
            stage = "curve_mask",
            graphIndex = graphIndex,
            stages = stages,
            successMessage = { result ->
                "mask=${result.maskWidth}x${result.maskHeight}, cleanPixels=${result.cleanPixelCount}."
            },
        ) {
            curveMaskPreparer.prepare(analysisImagePath, calculationRegion, axesResult ?: emptyAxes(), outputDir)
        }
        val maskPath = maskResult?.cleanMaskPath ?: maskResult?.rawMaskPath
        val availableMask = maskResult?.takeIf {
            maskPath != null && it.maskWidth > 0 && it.maskHeight > 0
        }
        val maskAvailable = availableMask != null
        if (!maskAvailable) {
            graphWarnings += "curve_mask_not_available"
            stages += skippedStage(
                stage = "curve_extract",
                graphIndex = graphIndex,
                message = "Curve extraction skipped because curve mask is unavailable.",
            )
        }
        val traceArtifactWarnings = maskResult?.traceArtifactAudit?.let { audit ->
            audit.warnings + audit.cleanupHypothesisWarnings
        }.orEmpty()
        traceArtifactWarnings.forEach { warning ->
            if (warning !in graphWarnings) graphWarnings += warning
        }

        val curveResult = if (availableMask != null) {
            runStage(
                stage = "curve_extract",
                graphIndex = graphIndex,
                stages = stages,
                successMessage = { result ->
                    "points=${result.points.size}, coverage=${result.coverage}."
                },
            ) {
                curveExtractor.extract(
                    maskPath = requireNotNull(maskPath),
                    maskWidth = availableMask.maskWidth,
                    maskHeight = availableMask.maskHeight,
                    outputDir = outputDir,
                )
            }
        } else {
            null
        }
        if (curveResult?.isUsable != true) {
            graphWarnings += "curve_not_usable"
        }
        curveResult?.warnings.orEmpty().forEach { warning ->
            if (warning !in graphWarnings) graphWarnings += warning
        }

        val signalConversion = when {
            !axisCalibration.ready -> {
                val warnings = listOf("signal_convert.axis_calibration_required")
                graphWarnings += warnings
                stages += skippedStage(
                    stage = "signal_convert",
                    graphIndex = graphIndex,
                    message = "Signal conversion skipped until axis calibration is confirmed.",
                    warnings = warnings,
                )
                OfflineSignalConversion(missingSignal(warnings), signal = null)
            }
            curveResult?.isUsable != true -> {
                val warnings = listOf("signal_convert.curve_points_required")
                graphWarnings += warnings
                stages += skippedStage(
                    stage = "signal_convert",
                    graphIndex = graphIndex,
                    message = "Signal conversion skipped until usable curve points are available.",
                    warnings = warnings,
                )
                OfflineSignalConversion(missingSignal(warnings), signal = null)
            }
            else -> {
                runStage(
                    stage = "signal_convert",
                    graphIndex = graphIndex,
                    stages = stages,
                    successMessage = { conversion ->
                        "points=${conversion.audit.pointCount}, time=${conversion.audit.timeRange}, intensity=${conversion.audit.intensityRange}."
                    },
                ) {
                    buildSignalAudit(
                        curvePoints = curveResult.points,
                        axisCalibration = axisCalibration,
                        sourceImage = analysisImagePath,
                    )
                } ?: OfflineSignalConversion(missingSignal(listOf("signal_convert.stage_failed")), signal = null)
            }
        }
        val signalAudit = signalConversion.audit
        graphWarnings += signalAudit.warnings.filterNot { it in graphWarnings }

        val peakDetectionResult = if (signalAudit.ready && signalConversion.signal != null) {
            val params = defaultOfflineCalculationParams()
            runStage(
                stage = "peak_detection",
                graphIndex = graphIndex,
                stages = stages,
                successMessage = { result ->
                    "ready=${result.audit.ready}, peaks=${result.audit.peakCount}, significant=${result.audit.significantPeakCount}."
                },
            ) {
                buildPeakDetectionAudit(
                    signal = signalConversion.signal,
                    sourceId = "graph_${graphIndex}_${analysisImagePath.substringAfterLast('/').substringAfterLast('\\')}",
                    params = params,
                    traceArtifactAudit = maskResult?.traceArtifactAudit,
                    curveWarnings = curveResult?.warnings.orEmpty(),
                )
            } ?: OfflinePeakDetectionResult(
                audit = missingPeakDetection(listOf("peak_detection.stage_failed")),
                run = null,
                params = null,
            )
        } else {
            val warnings = listOf("peak_detection.signal_required")
            graphWarnings += warnings
            stages += skippedStage(
                stage = "peak_detection",
                graphIndex = graphIndex,
                message = "Peak detection skipped until calibrated signal data is available.",
                warnings = warnings,
            )
            OfflinePeakDetectionResult(
                audit = missingPeakDetection(warnings),
                run = null,
                params = null,
            )
        }
        val peakDetectionAudit = peakDetectionResult.audit
        graphWarnings += peakDetectionAudit.warnings.filterNot { it in graphWarnings }

        val peakMetricsAudit = if (peakDetectionAudit.ready && peakDetectionResult.run != null && peakDetectionResult.params != null) {
            runStage(
                stage = "peak_metrics",
                graphIndex = graphIndex,
                stages = stages,
                successMessage = { result ->
                    "ready=${result.ready}, invalidBoundaries=${result.invalidBoundaryCount}, nonPositiveArea=${result.nonPositiveAreaCount}."
                },
            ) {
                buildPeakMetricsAudit(
                    run = peakDetectionResult.run,
                    params = peakDetectionResult.params,
                )
            } ?: missingPeakMetrics(listOf("peak_metrics.stage_failed"))
        } else {
            val warnings = listOf("peak_metrics.peak_detection_required")
            graphWarnings += warnings
            stages += skippedStage(
                stage = "peak_metrics",
                graphIndex = graphIndex,
                message = "Peak metrics review skipped until peak detection is ready.",
                warnings = warnings,
            )
            missingPeakMetrics(warnings)
        }
        graphWarnings += peakMetricsAudit.warnings.filterNot { it in graphWarnings }

        val peakSanityAudit = if (peakMetricsAudit.ready) {
            runStage(
                stage = "peak_sanity",
                graphIndex = graphIndex,
                stages = stages,
                successMessage = { result ->
                    "ready=${result.ready}, expected=${result.expectedApexTimes.size}, missing=${result.missingExpectedApexTimes.size}."
                },
            ) {
                buildPeakSanityAudit(
                    peakDetection = peakDetectionAudit,
                    peakMetrics = peakMetricsAudit,
                    expectation = peakSanityExpectation,
                )
            } ?: missingPeakSanity(listOf("peak_sanity.stage_failed"))
        } else {
            val warnings = listOf("peak_sanity.peak_metrics_required")
            graphWarnings += warnings
            stages += skippedStage(
                stage = "peak_sanity",
                graphIndex = graphIndex,
                message = "Peak sanity review skipped until peak metrics are ready.",
                warnings = warnings,
            )
            missingPeakSanity(warnings)
        }
        graphWarnings += peakSanityAudit.warnings.filterNot { it in graphWarnings }

        return OfflineGraphAudit(
            graphIndex = graphIndex,
            originalRegion = refinement.originalRegion,
            region = region,
            refinement = refinement.toAudit(),
            cropQuality = cropQuality,
            cropBoundaryRisk = cropBoundaryRisk.toAudit(),
            plotArea = plotAreaAudit,
            selectedPreprocessingVariant = selectedVariant?.variantId,
            selectedPreprocessingImagePath = selectedVariant?.imagePath,
            preprocessingVariantScores = variantScores,
            ocrStatus = ocrResult?.status ?: OcrStatus.NOT_AVAILABLE,
            xSuggestionCount = ocrResult?.suggestedXValues?.size ?: 0,
            ySuggestionCount = ocrResult?.suggestedYValues?.size ?: 0,
            axesDetected = axesResult?.hasAxes == true,
            originDetected = axesResult?.hasOrigin == true,
            axisConfidence = axesResult?.confidence ?: 0f,
            axisCalibration = axisCalibration,
            curveMaskAvailable = maskAvailable,
            curveMaskRawPixelCount = maskResult?.rawPixelCount ?: 0,
            curveMaskCleanPixelCount = maskResult?.cleanPixelCount ?: 0,
            curveMaskSuppressionApplied = maskResult?.suppressionApplied.orEmpty(),
            traceArtifacts = maskResult?.traceArtifactAudit ?: CurveTraceArtifactAudit(),
            curvePointCount = curveResult?.points?.size ?: 0,
            curveCoverage = curveResult?.coverage ?: 0f,
            curveUsable = curveResult?.isUsable == true,
            signal = signalAudit,
            peakDetection = peakDetectionAudit,
            peakMetrics = peakMetricsAudit,
            peakSanity = peakSanityAudit,
            warnings = graphWarnings,
        )
    }
}

private data class OfflineSignalConversion(
    val audit: OfflineSignalAudit,
    val signal: DigitalSignal?,
)

private data class OfflinePeakDetectionResult(
    val audit: OfflinePeakDetectionAudit,
    val run: CalculationRun?,
    val params: CalculationParams?,
)

private enum class CalibrationAxis {
    X,
    Y,
}

private fun buildAxisCalibrationAudit(
    manualCalibration: OfflineManualAxisCalibrationInput?,
    ocrResult: AxisOcrResult?,
    axesResult: AxesResult?,
    panelRegion: GraphRegion,
    plotRegion: GraphRegion,
): OfflineAxisCalibrationAudit {
    if (manualCalibration != null) {
        return buildManualAxisCalibrationAudit(manualCalibration)
    }

    val warnings = mutableListOf<String>()
    if (ocrResult == null || ocrResult.status == OcrStatus.NOT_AVAILABLE) {
        warnings += "axis_calibration.ocr_not_available"
    }
    if (axesResult?.hasAxes != true || axesResult.origin == null) {
        warnings += "axis_calibration.axis_geometry_missing"
    }

    val xCandidates = if (ocrResult != null && axesResult?.hasAxes == true && axesResult.origin != null) {
        ocrResult.rawElements.toCalibrationCandidates(
            axis = CalibrationAxis.X,
            axesResult = axesResult,
            panelRegion = panelRegion,
            plotRegion = plotRegion,
        )
    } else {
        emptyList()
    }
    val yCandidates = if (ocrResult != null && axesResult?.hasAxes == true && axesResult.origin != null) {
        ocrResult.rawElements.toCalibrationCandidates(
            axis = CalibrationAxis.Y,
            axesResult = axesResult,
            panelRegion = panelRegion,
            plotRegion = plotRegion,
        )
    } else {
        emptyList()
    }

    val xStats = xCandidates.axisStats()
    val yStats = yCandidates.axisStats()
    val xReady = xStats.ready
    val yReady = yStats.ready
    val calibrationAccepted = ocrResult?.status == OcrStatus.ACCEPTED ||
        ocrResult?.status == OcrStatus.CORRECTED ||
        ocrResult?.status == OcrStatus.AUTO_ACCEPTED
    val ready = xReady && yReady && calibrationAccepted

    if (!xReady) warnings += "axis_calibration.x_requires_two_pixel_value_points"
    if (!yReady) warnings += "axis_calibration.y_requires_two_pixel_value_points"
    if (xReady && yReady && !calibrationAccepted) warnings += "axis_calibration.auto_acceptance_required"
    if (!ready) warnings += "axis_calibration.manual_required"

    val source = when {
        ready -> OfflineAxisCalibrationSource.CONFIRMED
        xReady || yReady -> OfflineAxisCalibrationSource.OCR_CANDIDATE_REQUIRES_REVIEW
        else -> OfflineAxisCalibrationSource.MANUAL_REQUIRED
    }

    return OfflineAxisCalibrationAudit(
        ready = ready,
        xReady = xReady,
        yReady = yReady,
        source = source,
        xCandidateCount = xCandidates.size,
        yCandidateCount = yCandidates.size,
        xPixelSpan = xStats.pixelSpan,
        yPixelSpan = yStats.pixelSpan,
        xValueSpan = xStats.valueSpan,
        yValueSpan = yStats.valueSpan,
        xUnit = ocrResult?.xUnit,
        yUnit = ocrResult?.yUnit,
        xCandidates = xCandidates,
        yCandidates = yCandidates,
        warnings = warnings.distinct(),
    )
}

private fun buildManualAxisCalibrationAudit(
    manualCalibration: OfflineManualAxisCalibrationInput,
): OfflineAxisCalibrationAudit {
    val xCandidates = manualCalibration.xPoints.toAuditPoints()
    val yCandidates = manualCalibration.yPoints.toAuditPoints()
    val xStats = xCandidates.axisStats()
    val yStats = yCandidates.axisStats()
    val xReady = xStats.ready
    val yReady = yStats.ready
    val ready = xReady && yReady
    val warnings = buildList {
        if (!xReady) add("axis_calibration.x_requires_two_pixel_value_points")
        if (!yReady) add("axis_calibration.y_requires_two_pixel_value_points")
        if (!ready) add("axis_calibration.manual_required")
    }

    return OfflineAxisCalibrationAudit(
        ready = ready,
        xReady = xReady,
        yReady = yReady,
        source = if (ready) {
            OfflineAxisCalibrationSource.MANUAL_CONFIRMED
        } else {
            OfflineAxisCalibrationSource.MANUAL_REQUIRED
        },
        xCandidateCount = xCandidates.size,
        yCandidateCount = yCandidates.size,
        xPixelSpan = xStats.pixelSpan,
        yPixelSpan = yStats.pixelSpan,
        xValueSpan = xStats.valueSpan,
        yValueSpan = yStats.valueSpan,
        xUnit = manualCalibration.xUnit,
        yUnit = manualCalibration.yUnit,
        xCandidates = xCandidates,
        yCandidates = yCandidates,
        warnings = warnings,
    )
}

private fun List<OfflineManualAxisCalibrationPointInput>.toAuditPoints(): List<OfflineAxisCalibrationPointAudit> =
    map { point ->
        OfflineAxisCalibrationPointAudit(
            pixel = point.pixel,
            value = point.value,
            text = point.label ?: point.value.toString(),
            confidence = 1f,
        )
    }
        .filter { it.pixel >= 0f }
        .groupBy { candidate ->
            "${candidate.value.roundToBucket()}:${candidate.pixel.roundToInt()}"
        }
        .values
        .map { group -> group.maxBy { it.confidence } }
        .sortedBy { it.pixel }

private fun List<OcrTextElement>.toCalibrationCandidates(
    axis: CalibrationAxis,
    axesResult: AxesResult,
    panelRegion: GraphRegion,
    plotRegion: GraphRegion,
): List<OfflineAxisCalibrationPointAudit> {
    val origin = axesResult.origin ?: return emptyList()
    val xAxis = axesResult.xAxis ?: return emptyList()
    val yAxis = axesResult.yAxis ?: return emptyList()
    val xAxisY = xAxis.y1
    val yAxisX = yAxis.x1
    val xLeft = minOf(xAxis.x1, xAxis.x2) - plotRegion.width * 0.06f
    val xRight = maxOf(xAxis.x1, xAxis.x2) + plotRegion.width * 0.06f
    val xTop = xAxisY - plotRegion.height * 0.10f
    val xBottom = panelRegion.bottom + panelRegion.height * 0.08f
    val yLeft = panelRegion.x - panelRegion.width * 0.08f
    val yRight = yAxisX + plotRegion.width * 0.12f
    val yTop = minOf(yAxis.y1, yAxis.y2) - plotRegion.height * 0.06f
    val yBottom = origin.y + plotRegion.height * 0.08f

    return mapNotNull { element ->
        val value = element.numericValue ?: return@mapNotNull null
        val centerX = element.x + element.width / 2f
        val centerY = element.y + element.height / 2f
        val accepted = when (axis) {
            CalibrationAxis.X -> centerX in xLeft..xRight && centerY in xTop..xBottom
            CalibrationAxis.Y -> centerX in yLeft..yRight && centerY in yTop..yBottom
        }
        if (!accepted) return@mapNotNull null

        val pixel = when (axis) {
            CalibrationAxis.X -> centerX - origin.x
            CalibrationAxis.Y -> origin.y - centerY
        }
        if (pixel < 0f) return@mapNotNull null

        OfflineAxisCalibrationPointAudit(
            pixel = pixel,
            value = value,
            text = element.text,
            confidence = element.confidence,
        )
    }
        .groupBy { candidate ->
            "${candidate.value.roundToBucket()}:${candidate.pixel.roundToInt()}"
        }
        .values
        .map { group -> group.maxBy { it.confidence } }
        .sortedBy { it.pixel }
}

private data class AxisCalibrationStats(
    val ready: Boolean,
    val pixelSpan: Float,
    val valueSpan: Float,
)

private fun List<OfflineAxisCalibrationPointAudit>.axisStats(): AxisCalibrationStats {
    if (size < 2) return AxisCalibrationStats(ready = false, pixelSpan = 0f, valueSpan = 0f)
    val sorted = sortedBy { it.pixel }
    val pixelSpan = sorted.last().pixel - sorted.first().pixel
    val valueSpan = sorted.last().value - sorted.first().value
    val minPixelSpan = maxOf(24f, pixelSpan * 0.05f)
    return AxisCalibrationStats(
        ready = pixelSpan >= minPixelSpan && abs(valueSpan) > 0f,
        pixelSpan = pixelSpan.coerceAtLeast(0f),
        valueSpan = valueSpan,
    )
}

private fun Float.roundToBucket(): Int =
    (this * 100f).toInt()

private fun missingAxisCalibration(): OfflineAxisCalibrationAudit =
    OfflineAxisCalibrationAudit(
        ready = false,
        xReady = false,
        yReady = false,
        source = OfflineAxisCalibrationSource.MANUAL_REQUIRED,
        xCandidateCount = 0,
        yCandidateCount = 0,
        xPixelSpan = 0f,
        yPixelSpan = 0f,
        xValueSpan = 0f,
        yValueSpan = 0f,
        xUnit = null,
        yUnit = null,
        xCandidates = emptyList(),
        yCandidates = emptyList(),
        warnings = listOf("axis_calibration.stage_failed", "axis_calibration.manual_required"),
    )

private fun buildSignalAudit(
    curvePoints: List<CurvePoint>,
    axisCalibration: OfflineAxisCalibrationAudit,
    sourceImage: String,
): OfflineSignalConversion {
    val pixelCalibration = axisCalibration.toPixelCalibration()
        ?: return OfflineSignalConversion(
            audit = missingSignal(listOf("signal_convert.axis_calibration_invalid")),
            signal = null,
        )
    val signal = SignalConverter.convert(
        curvePoints = curvePoints,
        calibration = pixelCalibration,
        sourceImage = sourceImage,
    )
    val intensityRange = signal.maxIntensity - signal.minIntensity
    val warnings = buildList {
        if (signal.points.isEmpty()) add("signal_convert.no_points")
        if (!signal.metadata.sortValid) add("signal_convert.time_sort_invalid")
        if (signal.timeRange <= 0f) add("signal_convert.non_positive_time_range")
        if (intensityRange < 0f) add("signal_convert.negative_intensity_range")
    }

    val audit = OfflineSignalAudit(
        ready = signal.points.isNotEmpty() &&
            signal.metadata.sortValid &&
            signal.timeRange > 0f &&
            warnings.isEmpty(),
        pointCount = signal.points.size,
        timeStart = signal.points.firstOrNull()?.time,
        timeEnd = signal.points.lastOrNull()?.time,
        timeRange = signal.timeRange,
        intensityMin = signal.points.minOfOrNull { it.intensity },
        intensityMax = signal.points.maxOfOrNull { it.intensity },
        intensityRange = intensityRange.coerceAtLeast(0f),
        duplicateCount = signal.metadata.duplicatesRemoved,
        gapCount = signal.metadata.gapCount,
        sortValid = signal.metadata.sortValid,
        warnings = warnings,
    )

    return OfflineSignalConversion(
        audit = audit,
        signal = signal.takeIf { audit.ready },
    )
}

private fun OfflineAxisCalibrationAudit.toPixelCalibration(): PixelCalibration? {
    if (!ready) return null
    val xPair = xCandidates.maxSpanPair() ?: return null
    val yPair = yCandidates.maxSpanPair() ?: return null
    val xCalibration = LinearCalibration(
        CalibrationPoint(xPair.first.pixel, xPair.first.value),
        CalibrationPoint(xPair.second.pixel, xPair.second.value),
    )
    val yCalibration = LinearCalibration(
        CalibrationPoint(yPair.first.pixel, yPair.first.value),
        CalibrationPoint(yPair.second.pixel, yPair.second.value),
    )
    if (!xCalibration.isValid || !yCalibration.isValid) return null

    return PixelCalibration(
        xCalibration = xCalibration,
        yCalibration = yCalibration,
        xUnit = xUnit ?: "unknown",
        yUnit = yUnit ?: "unknown",
        originPixelX = 0f,
        originPixelY = 0f,
        timestamp = 0L,
    )
}

private fun List<OfflineAxisCalibrationPointAudit>.maxSpanPair():
    Pair<OfflineAxisCalibrationPointAudit, OfflineAxisCalibrationPointAudit>? {
    if (size < 2) return null
    val sorted = sortedBy { it.pixel }
    return sorted.first() to sorted.last()
}

private fun missingSignal(warnings: List<String>): OfflineSignalAudit =
    OfflineSignalAudit(
        ready = false,
        pointCount = 0,
        timeStart = null,
        timeEnd = null,
        timeRange = 0f,
        intensityMin = null,
        intensityMax = null,
        intensityRange = 0f,
        duplicateCount = 0,
        gapCount = 0,
        sortValid = false,
        warnings = warnings,
    )

private fun defaultOfflineCalculationParams(): CalculationParams =
    CalculationParams(
        smoothingEnabled = true,
        smoothingWindowSize = 7,
        smoothingPolynomialOrder = 2,
        baselineMethod = "ALS",
        baselineLambda = 1e6,
        baselineP = 0.01,
        baselineIterations = 10,
        minPeakHeight = 0.0,
        minPeakProminence = 0.0,
        minPeakDistance = 5,
        minPeakWidth = 3,
        maxPeakWidth = 0,
        minSnr = 3.0,
        noiseMethod = "MAD",
        integrationMethod = "TRAPEZOIDAL",
        boundaryMethod = "LOCAL_MINIMA",
        boundaryPercentHeight = 0.01,
        clampNegative = false,
        useSmoothedForIntegration = false,
        presetName = "Offline bench balanced",
    )

private fun buildPeakDetectionAudit(
    signal: DigitalSignal,
    sourceId: String,
    params: CalculationParams,
    traceArtifactAudit: CurveTraceArtifactAudit?,
    curveWarnings: List<String>,
): OfflinePeakDetectionResult {
    val choice = choosePeakDetectionRun(
        signal = signal,
        sourceId = sourceId,
        params = params,
        traceArtifactAudit = traceArtifactAudit,
    )
    val run = choice.run
    val selectedParams = choice.params
    val candidateDiagnostics = choice.candidateDiagnostics
    val guardedQualityReview = choice.guardedQualityReview
    val sparseTraceQualityReview = buildSparseTracePeakQualityAudit(
        peaks = run.peaks,
        curveWarnings = curveWarnings,
        minSnr = selectedParams.minSnr,
    )
    val peaksByArea = run.peaks.sortedByDescending { abs(it.area) }
    val dominantPeak = peaksByArea.firstOrNull()
    val warnings = buildList {
        if (!run.validation.isValid) add("peak_detection.signal_validation_failed")
        if (run.peaks.isEmpty()) add("peak_detection.no_peaks_detected")
        if (choice.controlledTuningApplied) {
            add("peak_detection.controlled_threshold_relaxation_applied")
        }
        if (traceArtifactAudit?.thresholdRelaxationAllowed == false) {
            add("peak_detection.threshold_relaxation_blocked_by_trace_artifacts")
        }
        guardedQualityReview.warnings.forEach { add(it) }
        sparseTraceQualityReview.warnings.forEach { add(it) }
        run.validation.warnings.forEach { add("validation.$it") }
        run.warnings.forEach { warning ->
            add("${warning.stage}.${warning.message}")
        }
    }

    val audit = OfflinePeakDetectionAudit(
        ready = run.validation.isValid && run.peaks.isNotEmpty(),
        peakCount = run.peaks.size,
        significantPeakCount = run.peaks.count { it.snr >= selectedParams.minSnr },
        candidateCount = candidateDiagnostics.candidateCount,
        rejectedCandidateCount = candidateDiagnostics.rejectedCandidateCount,
        dominantPeakTime = dominantPeak?.rtApex,
        dominantPeakHeight = dominantPeak?.height,
        dominantPeakAreaPercent = dominantPeak?.areaPercent,
        detectionSignalSource = candidateDiagnostics.detectionSignalSource,
        detectionProfile = choice.detectionProfile,
        noiseLevel = candidateDiagnostics.noiseLevel,
        noiseMethod = candidateDiagnostics.noiseMethod,
        basePeakCount = choice.basePeakCount,
        tunedPeakCount = choice.tunedPeakCount,
        controlledTuningApplied = choice.controlledTuningApplied,
        controlledTuningReason = choice.controlledTuningReason,
        guardedQualityReview = guardedQualityReview,
        sparseTraceQualityReview = sparseTraceQualityReview,
        thresholdRelaxationAllowed = traceArtifactAudit?.thresholdRelaxationAllowed,
        thresholdRelaxationGuardReason = traceArtifactAudit?.cleanupHypothesisWarnings
            ?.firstOrNull { it == "trace_artifact.threshold_relaxation_blocked" },
        baselineMethod = selectedParams.baselineMethod,
        boundaryMethod = selectedParams.boundaryMethod,
        integrationMethod = selectedParams.integrationMethod,
        clampNegative = selectedParams.clampNegative,
        maxPeakWidth = selectedParams.maxPeakWidth,
        minSnr = selectedParams.minSnr,
        peaks = run.peaks.mapIndexed { index, peak ->
            OfflinePeakAudit(
                peakNumber = index + 1,
                rtApex = peak.rtApex,
                leftBoundaryTime = peak.leftBoundaryTime,
                rightBoundaryTime = peak.rightBoundaryTime,
                height = peak.height,
                area = peak.area,
                areaPercent = peak.areaPercent,
                widthBase = peak.widthBase,
                widthHalfHeight = peak.widthHalfHeight,
                tailingFactor = peak.tailingFactor,
                asymmetryFactor = peak.asymmetryFactor,
                snr = peak.snr,
                confidence = peak.confidence.name,
                overlapStatus = peak.overlapStatus.name,
                assignment = peak.toOfflinePeakAssignmentAudit(),
                warningCount = peak.warnings.size,
                qualityFlags = guardedPeakQualityFlags(
                    peak = peak,
                    guardedCompletenessApplied = choice.controlledTuningApplied,
                ) + sparseTracePeakQualityFlags(
                    peak = peak,
                    sparseTraceQualityReview = sparseTraceQualityReview,
                    minSnr = selectedParams.minSnr,
                ),
            )
        },
        rejectionReasons = candidateDiagnostics.rejectionReasons,
        warnings = warnings.distinct(),
    )

    return OfflinePeakDetectionResult(
        audit = audit,
        run = run,
        params = selectedParams,
    )
}

private fun buildSparseTracePeakQualityAudit(
    peaks: List<PeakResult>,
    curveWarnings: List<String>,
    minSnr: Double,
): OfflineSparseTracePeakQualityAudit {
    val sparseTrace = "curve_extract.sparse_trace_low_column_coverage_accepted" in curveWarnings
    if (!sparseTrace) return OfflineSparseTracePeakQualityAudit()

    val localizedSparseTrace = "curve_extract.sparse_trace_localized_review_required" in curveWarnings
    val lowSnrCount = peaks.count { it.snr < minSnr }
    val lowAreaShareCount = peaks.count { it.areaPercent < SPARSE_TRACE_MIN_AREA_PERCENT_REVIEW }
    val lowConfidenceCount = peaks.count {
        it.confidence == ConfidenceGrade.LOW || it.confidence == ConfidenceGrade.FAILED
    }
    val overlapReviewCount = peaks.count {
        it.overlapStatus == OverlapStatus.SHOULDER || it.overlapStatus == OverlapStatus.UNRESOLVED
    }
    val warnings = buildList {
        add("peak_detection.sparse_trace_report_confidence_required")
        if (localizedSparseTrace) add("peak_detection.sparse_trace_localized_review_required")
        if (peaks.isEmpty()) add("peak_detection.sparse_trace.no_peaks")
        if (lowSnrCount > 0) add("peak_detection.sparse_trace_low_snr_peaks")
        if (lowAreaShareCount > 0) add("peak_detection.sparse_trace_low_area_share_peaks")
        if (lowConfidenceCount > 0) add("peak_detection.sparse_trace_low_confidence_peaks")
        if (overlapReviewCount > 0) add("peak_detection.sparse_trace_overlap_review_required")
    }

    return OfflineSparseTracePeakQualityAudit(
        available = true,
        sparseTrace = true,
        localizedSparseTrace = localizedSparseTrace,
        reviewPeakCount = peaks.size,
        lowSnrCount = lowSnrCount,
        lowAreaShareCount = lowAreaShareCount,
        lowConfidenceCount = lowConfidenceCount,
        overlapReviewCount = overlapReviewCount,
        requiresReportConfidenceText = true,
        warnings = warnings,
    )
}

private fun choosePeakDetectionRun(
    signal: DigitalSignal,
    sourceId: String,
    params: CalculationParams,
    traceArtifactAudit: CurveTraceArtifactAudit?,
): OfflinePeakDetectionChoice {
    val baseRun = CalculationEngine.execute(
        signal = signal,
        sourceId = sourceId,
        params = params,
    )
    val baseDiagnostics = buildPeakCandidateDiagnostics(baseRun, params)
    val baseChoice = OfflinePeakDetectionChoice(
        run = baseRun,
        params = params,
        candidateDiagnostics = baseDiagnostics,
        detectionProfile = "default",
        basePeakCount = baseRun.peaks.size,
        tunedPeakCount = null,
        controlledTuningApplied = false,
        controlledTuningReason = null,
        guardedQualityReview = OfflineGuardedPeakQualityAudit(),
    )
    if (traceArtifactAudit?.thresholdRelaxationAllowed != true) return baseChoice
    if (!baseRun.validation.isValid) return baseChoice
    if (baseRun.peaks.size > CONTROLLED_TUNING_MAX_BASE_PEAKS) return baseChoice

    val prominenceRejects = baseDiagnostics.rejectionReasons
        .firstOrNull { it.reason == "prominence_below_threshold" }
        ?.count ?: 0
    if (prominenceRejects < CONTROLLED_TUNING_MIN_PROMINENCE_REJECTS) return baseChoice

    val tunedParams = params.copy(
        minSnr = CONTROLLED_TUNING_MIN_SNR,
        presetName = "${params.presetName} guarded completeness",
    )
    val tunedRun = CalculationEngine.execute(
        signal = signal,
        sourceId = "${sourceId}_guarded_completeness",
        params = tunedParams,
    )
    val tunedDiagnostics = buildPeakCandidateDiagnostics(tunedRun, tunedParams)
    val addedPeaks = tunedRun.peaks.size - baseRun.peaks.size
    val guardedQualityReview = buildGuardedPeakQualityAudit(tunedRun.peaks)
    val tunedIsControlled = tunedRun.validation.isValid &&
        addedPeaks in 1..CONTROLLED_TUNING_MAX_EXTRA_PEAKS &&
        tunedRun.peaks.size <= CONTROLLED_TUNING_MAX_TOTAL_PEAKS &&
        guardedQualityReview.acceptedForGuardedCompleteness
    if (!tunedIsControlled) {
        return baseChoice.copy(
            tunedPeakCount = tunedRun.peaks.size,
            controlledTuningReason = if (guardedQualityReview.acceptedForGuardedCompleteness) {
                "controlled_threshold_relaxation_rejected"
            } else {
                "controlled_threshold_relaxation_rejected_by_peak_quality"
            },
            guardedQualityReview = guardedQualityReview,
        )
    }

    return OfflinePeakDetectionChoice(
        run = tunedRun,
        params = tunedParams,
        candidateDiagnostics = tunedDiagnostics,
        detectionProfile = "guarded_completeness",
        basePeakCount = baseRun.peaks.size,
        tunedPeakCount = tunedRun.peaks.size,
        controlledTuningApplied = true,
        controlledTuningReason = "threshold_relaxation_allowed_by_trace_artifact_guard",
        guardedQualityReview = guardedQualityReview,
    )
}

private data class OfflinePeakCandidateDiagnostics(
    val candidateCount: Int,
    val rejectedCandidateCount: Int,
    val detectionSignalSource: String,
    val noiseLevel: Double,
    val noiseMethod: String,
    val rejectionReasons: List<OfflinePeakRejectionAudit>,
)

private data class OfflinePeakDetectionChoice(
    val run: CalculationRun,
    val params: CalculationParams,
    val candidateDiagnostics: OfflinePeakCandidateDiagnostics,
    val detectionProfile: String,
    val basePeakCount: Int,
    val tunedPeakCount: Int?,
    val controlledTuningApplied: Boolean,
    val controlledTuningReason: String?,
    val guardedQualityReview: OfflineGuardedPeakQualityAudit,
)

private fun buildGuardedPeakQualityAudit(peaks: List<PeakResult>): OfflineGuardedPeakQualityAudit {
    if (peaks.isEmpty()) {
        return OfflineGuardedPeakQualityAudit(
            available = true,
            acceptedForGuardedCompleteness = false,
            warnings = listOf("peak_detection.guarded_quality.no_peaks"),
        )
    }
    val lowDefaultSnrCount = peaks.count {
        "guarded_peak.low_default_snr" in guardedPeakQualityFlags(it, guardedCompletenessApplied = true)
    }
    val lowAreaShareCount = peaks.count {
        "guarded_peak.low_area_share" in guardedPeakQualityFlags(it, guardedCompletenessApplied = true)
    }
    val narrowBoundaryCount = peaks.count {
        "guarded_peak.narrow_boundary" in guardedPeakQualityFlags(it, guardedCompletenessApplied = true)
    }
    val warnings = buildList {
        if (lowDefaultSnrCount > maxAllowedGuardedReviewCount(peaks.size)) {
            add("peak_detection.guarded_quality.too_many_low_default_snr_peaks")
        }
        if (lowAreaShareCount > maxAllowedGuardedReviewCount(peaks.size)) {
            add("peak_detection.guarded_quality.too_many_low_area_peaks")
        }
        if (narrowBoundaryCount > maxAllowedGuardedReviewCount(peaks.size)) {
            add("peak_detection.guarded_quality.too_many_narrow_peaks")
        }
    }
    return OfflineGuardedPeakQualityAudit(
        available = true,
        reviewPeakCount = peaks.size,
        lowDefaultSnrCount = lowDefaultSnrCount,
        lowAreaShareCount = lowAreaShareCount,
        narrowBoundaryCount = narrowBoundaryCount,
        acceptedForGuardedCompleteness = warnings.isEmpty(),
        warnings = warnings,
    )
}

private fun guardedPeakQualityFlags(
    peak: PeakResult,
    guardedCompletenessApplied: Boolean,
): List<String> {
    if (!guardedCompletenessApplied) return emptyList()
    return buildList {
        if (peak.snr < CONTROLLED_TUNING_REFERENCE_MIN_SNR) add("guarded_peak.low_default_snr")
        if (peak.areaPercent < CONTROLLED_TUNING_MIN_AREA_PERCENT_REVIEW) add("guarded_peak.low_area_share")
        if (peak.widthBase < CONTROLLED_TUNING_MIN_WIDTH_REVIEW) add("guarded_peak.narrow_boundary")
    }
}

private fun sparseTracePeakQualityFlags(
    peak: PeakResult,
    sparseTraceQualityReview: OfflineSparseTracePeakQualityAudit,
    minSnr: Double,
): List<String> {
    if (!sparseTraceQualityReview.available) return emptyList()
    return buildList {
        add("sparse_trace.low_column_coverage")
        if (sparseTraceQualityReview.localizedSparseTrace) add("sparse_trace.localized_evidence")
        if (peak.snr < minSnr) add("sparse_peak.low_snr")
        if (peak.areaPercent < SPARSE_TRACE_MIN_AREA_PERCENT_REVIEW) add("sparse_peak.low_area_share")
        if (peak.confidence == ConfidenceGrade.LOW || peak.confidence == ConfidenceGrade.FAILED) {
            add("sparse_peak.low_confidence")
        }
        if (peak.overlapStatus == OverlapStatus.SHOULDER || peak.overlapStatus == OverlapStatus.UNRESOLVED) {
            add("sparse_peak.overlap_review")
        }
    }
}

private fun PeakResult.toOfflinePeakAssignmentAudit(): OfflinePeakAssignmentAudit {
    val compound = compoundName?.takeIf { it.isNotBlank() }
    return OfflinePeakAssignmentAudit(
        probableCompoundName = compound,
        probableCompoundStatus = if (compound != null) REPORT_VALUE_INFERRED else REPORT_VALUE_NOT_CALCULATED,
        assignmentSource = if (compound != null) compoundSource.name else null,
    )
}

private fun maxAllowedGuardedReviewCount(peakCount: Int): Int =
    maxOf(2, (peakCount * CONTROLLED_TUNING_MAX_REVIEW_FRACTION).toInt())

private fun buildPeakCandidateDiagnostics(
    run: CalculationRun,
    params: CalculationParams,
): OfflinePeakCandidateDiagnostics {
    val detectionPoints = SignalModelBuilder.getSignal(
        bundle = run.signals,
        source = run.signals.signalUsedForDetection,
    )
    val maxPeakWidth = params.maxPeakWidth.takeIf { it > 0 && it < Int.MAX_VALUE } ?: 0
    val noiseMethod = offlineNoiseMethod(params.noiseMethod)
    val noiseResult = NoiseEstimator.estimate(
        points = detectionPoints,
        method = noiseMethod,
    )
    val detection = PeakDetector.detect(
        points = detectionPoints,
        minHeight = params.minPeakHeight,
        minProminence = params.minPeakProminence,
        minDistance = params.minPeakDistance,
        minWidth = params.minPeakWidth,
        maxWidth = maxPeakWidth,
        noiseLevel = noiseResult.noiseValue,
        noiseK = params.minSnr,
    )
    val rejectionReasons = detection.rejected
        .groupingBy { rejectionReasonKey(it.rejectReason) }
        .eachCount()
        .entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .map { OfflinePeakRejectionAudit(reason = it.key, count = it.value) }

    return OfflinePeakCandidateDiagnostics(
        candidateCount = detection.totalCandidates,
        rejectedCandidateCount = detection.rejected.size,
        detectionSignalSource = run.signals.signalUsedForDetection.name,
        noiseLevel = noiseResult.noiseValue,
        noiseMethod = noiseResult.method.name,
        rejectionReasons = rejectionReasons,
    )
}

private fun buildOfflineReportContractAudit(
    imageWidth: Int?,
    imageHeight: Int?,
    expectedGraphCount: Int?,
    detectedGraphCount: Int,
    graphAudits: List<OfflineGraphAudit>,
    stages: List<OfflineStageAudit>,
    warnings: List<String>,
): OfflineReportContractAudit {
    val sections = buildList {
        add(
            reportContractSection(
                section = "overview",
                missingFields = buildList {
                    if (imageWidth == null || imageHeight == null) add("source_image_dimensions")
                    if (detectedGraphCount <= 0 || graphAudits.isEmpty()) add("detected_graphs")
                },
                warnings = buildList {
                    if (expectedGraphCount != null && expectedGraphCount != detectedGraphCount) {
                        add("report.overview.graph_count_mismatch")
                    }
                    if (warnings.isNotEmpty()) add("report.overview.pipeline_warnings_present")
                },
            ),
        )

        graphAudits.forEach { graph ->
            add(buildReportSourcePreparationSection(graph))
            add(buildReportAxisCalibrationSection(graph))
            add(buildReportPeakTableSection(graph))
            add(buildReportGraphOverlaySection(graph))
            add(buildReportChromatographicQualitySection(graph))
            add(buildReportKovatsSection(graph))
            add(buildReportInterpretationSection(graph))
            add(buildReportWarningsSection(graph))
        }

        add(
            reportContractSection(
                section = "technical_appendix",
                missingFields = buildList {
                    if (stages.isEmpty()) add("stage_timeline")
                    if (graphAudits.isEmpty()) add("per_graph_audit")
                },
                warnings = buildList {
                    if (stages.any { it.status == OfflineStageStatus.FAILED }) add("report.appendix.failed_stages_present")
                },
            ),
        )
    }

    val readySectionCount = sections.count { it.status == OfflineReportContractSectionStatus.READY }
    val warningSectionCount = sections.count { it.status == OfflineReportContractSectionStatus.WARNING }
    val blockedSectionCount = sections.count { it.status == OfflineReportContractSectionStatus.BLOCKED }
    return OfflineReportContractAudit(
        ready = blockedSectionCount == 0,
        graphCount = graphAudits.size,
        sectionCount = sections.size,
        readySectionCount = readySectionCount,
        warningSectionCount = warningSectionCount,
        blockedSectionCount = blockedSectionCount,
        sections = sections,
        warnings = sections.flatMap { section ->
            section.missingFields.map { "${section.section}.$it" } + section.warnings.map { "${section.section}.$it" }
        }.distinct(),
    )
}

private fun buildReportSourcePreparationSection(graph: OfflineGraphAudit): OfflineReportContractSectionAudit =
    reportContractSection(
        graphIndex = graph.graphIndex,
        section = "source_and_graph_preparation",
        missingFields = buildList {
            if (graph.region.width <= 0 || graph.region.height <= 0) add("detected_graph_bounds")
            if (!graph.cropQuality.acceptedForCalculation) add("accepted_crop_bounds")
            if (!graph.cropBoundaryRisk.acceptedForCalculation) add("crop_boundary_clearance")
            if (!graph.plotArea.detected || graph.plotArea.region == null) add("plot_area_bounds")
            if (graph.selectedPreprocessingVariant == null) add("selected_preprocessing_variant")
        },
        warnings = (
            graph.refinement.warnings +
                graph.cropQuality.warnings +
                graph.cropBoundaryRisk.warnings +
                graph.plotArea.warnings
            ).distinct(),
    )

private fun buildReportAxisCalibrationSection(graph: OfflineGraphAudit): OfflineReportContractSectionAudit =
    reportContractSection(
        graphIndex = graph.graphIndex,
        section = "axis_calibration",
        missingFields = buildList {
            if (!graph.axisCalibration.ready) add("confirmed_axis_calibration")
            if (!graph.axisCalibration.xReady) add("x_axis_calibration")
            if (!graph.axisCalibration.yReady) add("y_axis_calibration")
            if (graph.axisCalibration.xCandidateCount < 2) add("x_axis_ticks")
            if (graph.axisCalibration.yCandidateCount < 2) add("y_axis_ticks")
            if (graph.axisCalibration.xUnit.isNullOrBlank()) add("x_axis_unit")
            if (graph.axisCalibration.yUnit.isNullOrBlank()) add("y_axis_unit")
            if (graph.axisCalibration.xPixelSpan <= 0f || abs(graph.axisCalibration.xValueSpan) <= 0f) {
                add("x_pixel_to_unit_transform")
            }
            if (graph.axisCalibration.yPixelSpan <= 0f || abs(graph.axisCalibration.yValueSpan) <= 0f) {
                add("y_pixel_to_unit_transform")
            }
        },
        warnings = buildList {
            addAll(graph.axisCalibration.warnings)
            if (graph.axisConfidence < REPORT_AXIS_CONFIDENCE_REVIEW_THRESHOLD) add("report.axis.low_geometry_confidence")
        }.distinct(),
    )

private fun buildReportPeakTableSection(graph: OfflineGraphAudit): OfflineReportContractSectionAudit =
    reportContractSection(
        graphIndex = graph.graphIndex,
        section = "peak_table",
        missingFields = buildList {
            if (!graph.peakDetection.ready) add("peak_detection")
            if (!graph.peakMetrics.ready) add("peak_metrics")
            if (graph.peakDetection.peaks.isEmpty()) add("peak_rows")
            if (!graph.peakMetrics.orderedByRetentionTime) add("retention_time_order")
            if (graph.peakDetection.peaks.any { it.widthHalfHeight == null || !it.widthHalfHeight.isUsableNumber() || it.widthHalfHeight <= 0.0 }) {
                add("peak_fwhm_column")
            }
            if (graph.peakDetection.peaks.any { !it.tailingFactor.isUsableNumber() || it.tailingFactor <= 0.0 }) {
                add("peak_tailing_column")
            }
            if (graph.peakDetection.peaks.any { !it.asymmetryFactor.isUsableNumber() || it.asymmetryFactor <= 0.0 }) {
                add("peak_asymmetry_column")
            }
            if (graph.peakDetection.peaks.any { !it.assignment.hasExplicitReportStatuses() }) {
                add("compound_candidate_columns")
            }
        },
        warnings = buildList {
            if (graph.peakDetection.peaks.any { it.assignment.probableCompoundStatus == REPORT_VALUE_NOT_CALCULATED }) {
                add("report.peak_table.compound_assignments_not_calculated")
            }
            if (graph.peakDetection.peaks.any { it.assignment.kovatsIndexStatus == REPORT_VALUE_NOT_CALCULATED }) {
                add("report.peak_table.kovats_values_not_calculated")
            }
            if (graph.peakDetection.sparseTraceQualityReview.requiresReportConfidenceText) {
                add("report.peak_table.sparse_trace_confidence_text_required")
            }
            if (graph.peakDetection.controlledTuningApplied) {
                add("report.peak_table.guarded_completeness_confidence_text_required")
            }
            if (graph.peakMetrics.lowSnrCount > 0) add("report.peak_table.low_snr_review_required")
            if (graph.peakMetrics.lowConfidenceCount > 0) add("report.peak_table.low_confidence_review_required")
            if (graph.peakMetrics.unresolvedOverlapCount > 0) add("report.peak_table.overlap_review_required")
            addAll(graph.peakDetection.warnings)
            addAll(graph.peakMetrics.warnings)
        }.distinct(),
    )

private fun buildReportGraphOverlaySection(graph: OfflineGraphAudit): OfflineReportContractSectionAudit =
    reportContractSection(
        graphIndex = graph.graphIndex,
        section = "interactive_or_rendered_graph",
        missingFields = buildList {
            if (!graph.curveMaskAvailable) add("curve_mask")
            if (!graph.curveUsable || graph.curvePointCount <= 0) add("curve_points")
            if (graph.peakDetection.peaks.isEmpty()) add("peak_markers")
            if (graph.plotArea.region == null) add("plot_area_overlay_bounds")
        },
        warnings = buildList {
            if (graph.peakDetection.sparseTraceQualityReview.requiresReportConfidenceText) {
                add("report.graph_overlay.sparse_trace_visual_review_required")
            }
        },
    )

private fun buildReportChromatographicQualitySection(graph: OfflineGraphAudit): OfflineReportContractSectionAudit =
    reportContractSection(
        graphIndex = graph.graphIndex,
        section = "chromatographic_quality",
        missingFields = buildList {
            if (!graph.peakMetrics.ready) add("peak_metrics")
            if (graph.peakDetection.noiseLevel == null) add("noise_estimate")
            if (graph.peakMetrics.totalAbsArea <= 0.0) add("global_integrated_area")
            if (graph.peakDetection.dominantPeakTime == null) add("dominant_peak")
        },
        warnings = buildList {
            addAll(graph.peakMetrics.warnings)
            if (graph.peakDetection.sparseTraceQualityReview.requiresReportConfidenceText) {
                add("report.quality.sparse_trace_quality_text_required")
            }
            if (graph.peakDetection.guardedQualityReview.available) {
                addAll(graph.peakDetection.guardedQualityReview.warnings)
            }
        }.distinct(),
    )

private fun buildReportKovatsSection(graph: OfflineGraphAudit): OfflineReportContractSectionAudit =
    reportContractSection(
        graphIndex = graph.graphIndex,
        section = "kovats_index_analysis",
        warnings = listOf(
            "report.kovats.reference_series_missing",
            "report.kovats.must_render_not_calculated_state",
        ),
    )

private fun buildReportInterpretationSection(graph: OfflineGraphAudit): OfflineReportContractSectionAudit =
    reportContractSection(
        graphIndex = graph.graphIndex,
        section = "distribution_and_chemical_interpretation",
        warnings = buildList {
            add("report.interpretation.local_knowledge_pack_required")
            add("report.interpretation.compound_assignments_missing")
            if (graph.peakDetection.sparseTraceQualityReview.requiresReportConfidenceText) {
                add("report.interpretation.sparse_trace_limits_interpretation_confidence")
            }
        },
    )

private fun buildReportWarningsSection(graph: OfflineGraphAudit): OfflineReportContractSectionAudit =
    reportContractSection(
        graphIndex = graph.graphIndex,
        section = "warnings_and_red_flags",
        warnings = buildList {
            addAll(graph.warnings.map { "pipeline.$it" })
            addAll(graph.peakDetection.warnings.map { "peak.$it" })
            addAll(graph.peakMetrics.warnings.map { "metrics.$it" })
            addAll(graph.peakSanity.warnings.map { "sanity.$it" })
        }.distinct(),
    )

private fun reportContractSection(
    section: String,
    graphIndex: Int? = null,
    missingFields: List<String> = emptyList(),
    warnings: List<String> = emptyList(),
): OfflineReportContractSectionAudit =
    OfflineReportContractSectionAudit(
        graphIndex = graphIndex,
        section = section,
        status = when {
            missingFields.isNotEmpty() -> OfflineReportContractSectionStatus.BLOCKED
            warnings.isNotEmpty() -> OfflineReportContractSectionStatus.WARNING
            else -> OfflineReportContractSectionStatus.READY
        },
        missingFields = missingFields.distinct(),
        warnings = warnings.distinct(),
    )

private fun OfflinePeakAssignmentAudit.hasExplicitReportStatuses(): Boolean =
    probableCompoundStatus.isNotBlank() &&
        formulaStatus.isNotBlank() &&
        compoundClassStatus.isNotBlank() &&
        carbonNumberStatus.isNotBlank() &&
        kovatsIndexStatus.isNotBlank()

private fun offlineNoiseMethod(value: String): NoiseMethod =
    when (value.trim().lowercase()) {
        "mad", "robust", "mad_robust", "mad (robust)" -> NoiseMethod.MAD
        "rms" -> NoiseMethod.RMS
        else -> NoiseMethod.PEAK_TO_PEAK
    }

private fun rejectionReasonKey(reason: String?): String {
    if (reason.isNullOrBlank()) return "unknown"
    return when {
        reason.startsWith("Prominence") -> "prominence_below_threshold"
        "RT=" in reason -> "too_close_to_stronger_peak"
        "Ñ‚Ð¾Ñ‡ÐµÐº" in reason && "<" in reason -> "width_below_threshold"
        "Ñ‚Ð¾Ñ‡ÐµÐº" in reason && ">" in reason -> "width_above_threshold"
        "Ð’Ñ‹ÑÐ¾Ñ‚Ð°" in reason -> "height_below_threshold"
        else -> "other"
    }
}

private fun missingPeakDetection(warnings: List<String>): OfflinePeakDetectionAudit =
    OfflinePeakDetectionAudit(
        ready = false,
        peakCount = 0,
        significantPeakCount = 0,
        candidateCount = null,
        rejectedCandidateCount = null,
        dominantPeakTime = null,
        dominantPeakHeight = null,
        dominantPeakAreaPercent = null,
        detectionSignalSource = null,
        detectionProfile = null,
        noiseLevel = null,
        noiseMethod = null,
        basePeakCount = null,
        tunedPeakCount = null,
        controlledTuningApplied = false,
        controlledTuningReason = null,
        thresholdRelaxationAllowed = null,
        thresholdRelaxationGuardReason = null,
        baselineMethod = null,
        boundaryMethod = null,
        integrationMethod = null,
        clampNegative = null,
        maxPeakWidth = null,
        minSnr = null,
        peaks = emptyList(),
        rejectionReasons = emptyList(),
        warnings = warnings,
    )

private fun buildPeakMetricsAudit(
    run: CalculationRun,
    params: CalculationParams,
): OfflinePeakMetricsAudit {
    val peaks = run.peaks
    val orderedByRetentionTime = peaks.zipWithNext().all { (a, b) -> a.rtApex <= b.rtApex }
    val invalidNumericCount = peaks.count { peak ->
        listOf(
            peak.rtApex,
            peak.height,
            peak.area,
            peak.widthBase,
            peak.prominence,
            peak.snr,
            peak.leftBoundaryTime,
            peak.rightBoundaryTime,
            peak.areaPercent,
        ).any { !it.isUsableNumber() } || peak.widthHalfHeight?.isUsableNumber() == false
    }
    val invalidBoundaryCount = peaks.count { peak ->
        peak.leftBoundaryTime >= peak.rtApex ||
            peak.rightBoundaryTime <= peak.rtApex ||
            peak.rightBoundaryTime <= peak.leftBoundaryTime ||
            peak.widthBase <= 0.0
    }
    val nonPositiveAreaCount = peaks.count { abs(it.area) <= 0.0 }
    val nonPositiveHeightCount = peaks.count { it.height <= 0.0 }
    val missingWidthCount = peaks.count { peak ->
        peak.widthBase <= 0.0 || (peak.widthHalfHeight ?: 0.0) <= 0.0
    }
    val lowSnrCount = peaks.count { it.snr < params.minSnr }
    val lowConfidenceCount = peaks.count {
        it.confidence == ConfidenceGrade.LOW || it.confidence == ConfidenceGrade.FAILED
    }
    val unresolvedOverlapCount = peaks.count {
        it.overlapStatus == OverlapStatus.SHOULDER || it.overlapStatus == OverlapStatus.UNRESOLVED
    }
    val areaPercentSum = peaks.sumOf { it.areaPercent }
    val areaPercentValid = peaks.isNotEmpty() && areaPercentSum in 99.0..101.0
    val warnings = buildList {
        if (peaks.isEmpty()) add("peak_metrics.no_peaks")
        if (!run.validation.isValid) add("peak_metrics.signal_validation_failed")
        if (!orderedByRetentionTime) add("peak_metrics.retention_time_order_invalid")
        if (invalidNumericCount > 0) add("peak_metrics.invalid_numeric_values")
        if (invalidBoundaryCount > 0) add("peak_metrics.invalid_boundaries")
        if (nonPositiveAreaCount > 0) add("peak_metrics.non_positive_area")
        if (nonPositiveHeightCount > 0) add("peak_metrics.non_positive_height")
        if (missingWidthCount > 0) add("peak_metrics.missing_width")
        if (!areaPercentValid) add("peak_metrics.area_percent_sum_invalid")
        if (lowSnrCount > 0) add("peak_metrics.low_snr_review_required")
        if (lowConfidenceCount > 0) add("peak_metrics.low_confidence_review_required")
        if (unresolvedOverlapCount > 0) add("peak_metrics.overlap_review_required")
    }

    return OfflinePeakMetricsAudit(
        ready = run.validation.isValid &&
            peaks.isNotEmpty() &&
            orderedByRetentionTime &&
            invalidNumericCount == 0 &&
            invalidBoundaryCount == 0 &&
            nonPositiveAreaCount == 0 &&
            nonPositiveHeightCount == 0 &&
            missingWidthCount == 0 &&
            areaPercentValid,
        orderedByRetentionTime = orderedByRetentionTime,
        totalAbsArea = peaks.sumOf { abs(it.area) },
        areaPercentSum = areaPercentSum,
        maximumHeight = peaks.maxOfOrNull { it.height },
        firstPeakTime = peaks.firstOrNull()?.rtApex,
        lastPeakTime = peaks.lastOrNull()?.rtApex,
        minBoundaryWidth = peaks.minOfOrNull { it.widthBase },
        maxBoundaryWidth = peaks.maxOfOrNull { it.widthBase },
        invalidNumericCount = invalidNumericCount,
        invalidBoundaryCount = invalidBoundaryCount,
        nonPositiveAreaCount = nonPositiveAreaCount,
        nonPositiveHeightCount = nonPositiveHeightCount,
        missingWidthCount = missingWidthCount,
        lowSnrCount = lowSnrCount,
        lowConfidenceCount = lowConfidenceCount,
        unresolvedOverlapCount = unresolvedOverlapCount,
        peakWarningCount = peaks.sumOf { it.warnings.size },
        warnings = warnings.distinct(),
    )
}

private fun missingPeakMetrics(warnings: List<String>): OfflinePeakMetricsAudit =
    OfflinePeakMetricsAudit(
        ready = false,
        orderedByRetentionTime = false,
        totalAbsArea = 0.0,
        areaPercentSum = 0.0,
        maximumHeight = null,
        firstPeakTime = null,
        lastPeakTime = null,
        minBoundaryWidth = null,
        maxBoundaryWidth = null,
        invalidNumericCount = 0,
        invalidBoundaryCount = 0,
        nonPositiveAreaCount = 0,
        nonPositiveHeightCount = 0,
        missingWidthCount = 0,
        lowSnrCount = 0,
        lowConfidenceCount = 0,
        unresolvedOverlapCount = 0,
        peakWarningCount = 0,
        warnings = warnings,
    )

private fun buildPeakSanityAudit(
    peakDetection: OfflinePeakDetectionAudit,
    peakMetrics: OfflinePeakMetricsAudit,
    expectation: OfflinePeakSanityExpectationInput?,
): OfflinePeakSanityAudit {
    val detectedTimes = peakDetection.peaks.map { it.rtApex }
    val expectedTimes = expectation?.expectedApexTimes.orEmpty()
    val tolerance = expectation?.apexTolerance ?: 0.0
    val missing = if (expectedTimes.isNotEmpty()) {
        if (tolerance > 0.0) {
            expectedTimes.filter { expected ->
                detectedTimes.none { detected -> abs(detected - expected) <= tolerance }
            }
        } else {
            expectedTimes
        }
    } else {
        emptyList()
    }
    val matchedCount = expectedTimes.size - missing.size
    val minPeakCount = expectation?.minPeakCount
    val unexpectedPeakCount = if (expectedTimes.isNotEmpty() && tolerance > 0.0) {
        detectedTimes.count { detected ->
            expectedTimes.none { expected -> abs(detected - expected) <= tolerance }
        }
    } else {
        0
    }
    val warnings = buildList {
        if (!peakMetrics.ready) add("peak_sanity.peak_metrics_required")
        if (expectedTimes.isNotEmpty() && tolerance <= 0.0) add("peak_sanity.apex_tolerance_required")
        if (minPeakCount != null && peakDetection.peakCount < minPeakCount) add("peak_sanity.min_peak_count_not_met")
        if (missing.isNotEmpty()) add("peak_sanity.expected_apex_missing")
        if (unexpectedPeakCount > 0 && expectedTimes.isNotEmpty()) add("peak_sanity.unexpected_apex_candidates")
    }
    val expectedReady = expectation?.lockExpectedApexTimes != true || missing.isEmpty()
    val minCountReady = minPeakCount == null || peakDetection.peakCount >= minPeakCount

    return OfflinePeakSanityAudit(
        ready = peakMetrics.ready && expectedReady && minCountReady,
        expectationProvided = expectation != null,
        minPeakCount = minPeakCount,
        expectedApexTimes = expectedTimes,
        detectedExpectedPeakCount = matchedCount,
        missingExpectedApexTimes = missing,
        unexpectedPeakCount = unexpectedPeakCount,
        apexTolerance = tolerance,
        warnings = warnings.distinct(),
    )
}

private fun missingPeakSanity(warnings: List<String>): OfflinePeakSanityAudit =
    OfflinePeakSanityAudit(
        ready = false,
        expectationProvided = false,
        minPeakCount = null,
        expectedApexTimes = emptyList(),
        detectedExpectedPeakCount = 0,
        missingExpectedApexTimes = emptyList(),
        unexpectedPeakCount = 0,
        apexTolerance = 0.0,
        warnings = warnings,
    )

private fun Double.isUsableNumber(): Boolean =
    !isNaN() && !isInfinite()

private fun GraphRegionQuality.toAudit(graphIndex: Int): OfflineGraphCandidateAudit =
    OfflineGraphCandidateAudit(
        graphIndex = graphIndex,
        region = region,
        accepted = accepted,
        areaRatio = areaRatio,
        aspectRatio = aspectRatio,
        rejectionReasons = rejectionReasons,
    )

private fun buildPerspectiveGeometryAudit(
    documentBounds: DocumentBounds?,
    imageWidth: Int,
    imageHeight: Int,
    graphAudits: List<OfflineGraphAudit>,
    cvCandidates: CvQuadrilateralCandidateResult,
): OfflinePerspectiveGeometryAudit {
    val documentTrusted = documentBounds != null &&
        documentBounds.isQuadrilateral &&
        documentBounds.confidence > 0.15f &&
        documentBounds.areaRatio in 0.10f..0.98f
    val plotAreaCount = graphAudits.count { it.plotArea.detected && it.plotArea.region != null }
    val plotGeometryReady = graphAudits.isNotEmpty() && plotAreaCount == graphAudits.size
    val candidates = buildGeometryQuadrilateralCandidates(
        documentBounds = documentBounds,
        documentTrusted = documentTrusted,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        graphAudits = graphAudits,
        cvCandidates = cvCandidates.candidates,
    )
    val residualMetrics = buildPerspectiveResidualMetrics(candidates)
    val normalizedCornerDisplacement = documentBounds?.let { bounds ->
        normalizedCornerDisplacement(
            corners = bounds.effectiveCorners,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
        )
    } ?: 0f
    val maxSkewAngle = documentBounds?.effectiveCorners?.maxSkewAngleDegrees() ?: 0f
    val perspectiveRequired = documentTrusted &&
        (normalizedCornerDisplacement > 0.025f || maxSkewAngle > 1.5f)
    val warnings = buildList {
        if (documentBounds == null) add("perspective_geometry.document_bounds_missing")
        if (documentBounds != null && documentBounds.confidence <= 0.15f) {
            add("perspective_geometry.document_bounds_not_trusted")
        }
        if (documentBounds != null && !documentBounds.isQuadrilateral) {
            add("perspective_geometry.document_not_quadrilateral")
        }
        if (!plotGeometryReady) add("perspective_geometry.plot_geometry_incomplete")
        if (perspectiveRequired) add("perspective_geometry.perspective_transform_required")
        addAll(cvCandidates.warnings)
        addAll(residualMetrics.warnings)
        add("perspective_geometry.residual_metrics_required_before_production_acceptance")
    }.distinct()

    return OfflinePerspectiveGeometryAudit(
        ready = documentTrusted && plotGeometryReady && !perspectiveRequired && residualMetrics.residualQualityReady,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        documentDetected = documentBounds != null,
        documentTrusted = documentTrusted,
        documentMethod = documentBounds?.detectionMethod?.name,
        documentConfidence = documentBounds?.confidence ?: 0f,
        documentAreaRatio = documentBounds?.areaRatio ?: 0f,
        documentAspectRatio = documentBounds?.aspectRatio ?: 0f,
        documentQuadrilateral = documentBounds?.isQuadrilateral ?: false,
        documentCorners = documentBounds?.corners,
        correctedCorners = documentBounds?.correctedCorners,
        graphPanelCount = graphAudits.size,
        plotAreaCount = plotAreaCount,
        plotGeometryReady = plotGeometryReady,
        perspectiveTransformRequired = perspectiveRequired,
        perspectiveApplied = false,
        normalizedCornerDisplacement = normalizedCornerDisplacement,
        maxSkewAngleDegrees = maxSkewAngle,
        candidates = candidates,
        residualMetrics = residualMetrics,
        residualMetricsRequired = true,
        warnings = warnings,
    )
}

private fun buildGeometryQuadrilateralCandidates(
    documentBounds: DocumentBounds?,
    documentTrusted: Boolean,
    imageWidth: Int,
    imageHeight: Int,
    graphAudits: List<OfflineGraphAudit>,
    cvCandidates: List<CvQuadrilateralCandidate>,
): List<OfflineGeometryQuadrilateralCandidateAudit> = buildList {
    if (documentBounds != null) {
        val corners = documentBounds.effectiveCorners
        add(
            quadrilateralCandidate(
                kind = OfflineGeometryCandidateKind.DOCUMENT,
                graphIndex = null,
                source = "document_detector",
                corners = corners,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                accepted = documentTrusted,
                score = documentBounds.confidence,
                warnings = buildList {
                    if (!documentTrusted) add("quadrilateral.document_not_trusted")
                    if (documentBounds.confidence <= 0.15f) add("quadrilateral.low_document_confidence")
                    if (!documentBounds.isQuadrilateral) add("quadrilateral.document_not_four_corners")
                    if (documentBounds.areaRatio !in 0.10f..0.98f) add("quadrilateral.document_area_out_of_range")
                },
            ),
        )
    }

    graphAudits.forEach { graph ->
        add(
            quadrilateralCandidate(
                kind = OfflineGeometryCandidateKind.GRAPH_PANEL,
                graphIndex = graph.graphIndex,
                source = "graph_region",
                corners = graph.region.toDocumentCorners(),
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                accepted = graph.cropQuality.acceptedForCalculation && graph.cropBoundaryRisk.acceptedForCalculation,
                score = (1f - graph.cropQuality.areaRatio.coerceIn(0f, 1f) * 0.15f).coerceIn(0f, 1f),
                warnings = graph.cropQuality.warnings + graph.cropBoundaryRisk.warnings,
            ),
        )

        val plotRegion = graph.plotArea.region
        add(
            quadrilateralCandidate(
                kind = OfflineGeometryCandidateKind.PLOT_AREA,
                graphIndex = graph.graphIndex,
                source = "plot_area_detector",
                corners = (plotRegion ?: graph.region).toDocumentCorners(),
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                accepted = graph.plotArea.detected && plotRegion != null,
                score = if (graph.plotArea.detected && plotRegion != null) {
                    graph.plotArea.areaRatioWithinPanel.coerceIn(0f, 1f)
                } else {
                    0f
                },
                warnings = graph.plotArea.warnings + if (plotRegion == null) {
                    listOf("quadrilateral.plot_area_missing")
                } else {
                    emptyList()
                },
            ),
        )
    }

    cvCandidates.forEach { candidate ->
        add(candidate.toOfflineGeometryQuadrilateralCandidateAudit(imageWidth, imageHeight))
    }
}

private fun CvQuadrilateralCandidate.toOfflineGeometryQuadrilateralCandidateAudit(
    imageWidth: Int,
    imageHeight: Int,
): OfflineGeometryQuadrilateralCandidateAudit =
    quadrilateralCandidate(
        kind = when (kind) {
            CvQuadrilateralCandidateKind.DOCUMENT -> OfflineGeometryCandidateKind.DOCUMENT
            CvQuadrilateralCandidateKind.PLOT_AREA -> OfflineGeometryCandidateKind.PLOT_AREA
        },
        graphIndex = graphIndex,
        source = source,
        corners = corners,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        accepted = accepted,
        score = score,
        warnings = warnings,
    )

private fun quadrilateralCandidate(
    kind: OfflineGeometryCandidateKind,
    graphIndex: Int?,
    source: String,
    corners: DocumentCorners,
    imageWidth: Int,
    imageHeight: Int,
    accepted: Boolean,
    score: Float,
    warnings: List<String>,
): OfflineGeometryQuadrilateralCandidateAudit {
    val bounds = corners.toBoundingRegion(kind.name.lowercase())
    return OfflineGeometryQuadrilateralCandidateAudit(
        kind = kind,
        graphIndex = graphIndex,
        source = source,
        bounds = bounds,
        corners = corners,
        accepted = accepted,
        areaRatio = corners.polygonAreaRatio(imageWidth, imageHeight),
        aspectRatio = corners.aspectRatio(),
        normalizedCornerDisplacement = corners.normalizedCornerDisplacementWithin(bounds),
        maxSkewAngleDegrees = corners.maxSkewAngleDegrees(),
        maxSideStraightnessResidualPx = 0f,
        orthogonalityResidualDegrees = corners.maxOrthogonalityResidualDegrees(),
        score = score.coerceIn(0f, 1f),
        warnings = warnings.distinct(),
    )
}

private fun buildPerspectiveResidualMetrics(
    candidates: List<OfflineGeometryQuadrilateralCandidateAudit>,
): OfflinePerspectiveResidualMetricsAudit {
    val accepted = candidates.filter { it.accepted }
    val acceptedPlots = accepted.filter { it.kind == OfflineGeometryCandidateKind.PLOT_AREA }
    val warnings = buildList {
        if (candidates.none { it.kind == OfflineGeometryCandidateKind.DOCUMENT }) {
            add("perspective_residual.document_candidate_missing")
        }
        if (acceptedPlots.isEmpty()) {
            add("perspective_residual.no_accepted_plot_candidates")
        }
        if (accepted.any { it.maxSkewAngleDegrees > 2f }) {
            add("perspective_residual.skew_review_required")
        }
        if (accepted.any { it.orthogonalityResidualDegrees > 2f }) {
            add("perspective_residual.orthogonality_review_required")
        }
    }
    val residualQualityReady = acceptedPlots.isNotEmpty() &&
        accepted.none { it.maxSkewAngleDegrees > 2f || it.orthogonalityResidualDegrees > 2f }

    return OfflinePerspectiveResidualMetricsAudit(
        candidateCount = candidates.size,
        acceptedCandidateCount = accepted.size,
        documentCandidateCount = candidates.count { it.kind == OfflineGeometryCandidateKind.DOCUMENT },
        graphPanelCandidateCount = candidates.count { it.kind == OfflineGeometryCandidateKind.GRAPH_PANEL },
        plotAreaCandidateCount = candidates.count { it.kind == OfflineGeometryCandidateKind.PLOT_AREA },
        acceptedPlotAreaCandidateCount = acceptedPlots.size,
        maxNormalizedCornerDisplacement = accepted.maxOfOrNull { it.normalizedCornerDisplacement } ?: 0f,
        maxSkewAngleDegrees = accepted.maxOfOrNull { it.maxSkewAngleDegrees } ?: 0f,
        maxOrthogonalityResidualDegrees = accepted.maxOfOrNull { it.orthogonalityResidualDegrees } ?: 0f,
        maxSideStraightnessResidualPx = accepted.maxOfOrNull { it.maxSideStraightnessResidualPx } ?: 0f,
        minAcceptedPlotAreaRatio = acceptedPlots.minOfOrNull { it.areaRatio } ?: 0f,
        maxAcceptedPlotAreaRatio = acceptedPlots.maxOfOrNull { it.areaRatio } ?: 0f,
        residualQualityReady = residualQualityReady,
        warnings = warnings,
    )
}

private fun GraphRegionRefinementResult.toAudit(): OfflineGraphRefinementAudit =
    OfflineGraphRefinementAudit(
        changed = changed,
        areaReductionRatio = areaReductionRatio,
        warnings = warnings,
    )

private fun GraphCropBoundaryRisk.toAudit(): OfflineGraphCropBoundaryRiskAudit =
    OfflineGraphCropBoundaryRiskAudit(
        topSignalClippingRisk = topSignalClippingRisk,
        topTouchingDarkRunCount = topTouchingDarkRunCount,
        topDarkPixelRatio = topDarkPixelRatio,
        acceptedForCalculation = acceptedForCalculation,
        warnings = warnings,
    )

private fun GraphPlotAreaDetectionResult?.toAudit(panelRegion: GraphRegion): OfflineGraphPlotAreaAudit {
    val plotArea = this?.plotArea
    val panelArea = panelRegion.area.coerceAtLeast(1)
    return OfflineGraphPlotAreaAudit(
        region = plotArea,
        detected = this?.detected == true && plotArea != null,
        areaRatioWithinPanel = plotArea?.area?.toFloat()?.div(panelArea.toFloat()) ?: 0f,
        warnings = this?.warnings ?: listOf("plot_area.failed"),
    )
}

private fun ImageOrientationCorrectionResult.toAudit(): OfflineOrientationCorrectionAudit =
    OfflineOrientationCorrectionAudit(
        imagePath = imagePath,
        wasRotated = wasRotated,
        rotationDegrees = rotationDegrees,
        horizontalRunCount = horizontalRunCount,
        verticalRunCount = verticalRunCount,
        warnings = warnings,
    )

private fun NormalizedImageResult.toOrientationCorrectionResult(
    warnings: List<String> = emptyList(),
): ImageOrientationCorrectionResult =
    ImageOrientationCorrectionResult(
        imagePath = normalizedPath,
        originalPath = normalizedPath,
        width = width,
        height = height,
        wasRotated = false,
        rotationDegrees = 0,
        horizontalRunCount = 0,
        verticalRunCount = 0,
        warnings = warnings,
    )

private fun GraphRegion.isFullImage(imageWidth: Int, imageHeight: Int): Boolean =
    x == 0 && y == 0 && width == imageWidth && height == imageHeight

private fun GraphRegionRefinementResult.cropQualityAudit(
    imageWidth: Int,
    imageHeight: Int,
): OfflineGraphCropQualityAudit {
    val safeArea = (imageWidth.toFloat() * imageHeight.toFloat()).coerceAtLeast(1f)
    val region = refinedRegion
    val originalAreaRatio = originalRegion.area.toFloat() / safeArea
    val originalEdgeContactCount = originalRegion.edgeContactCount(imageWidth, imageHeight)
    val originalFullImage = originalRegion.isFullImage(imageWidth, imageHeight)
    val originalLargeFullImage = originalFullImage && maxOf(imageWidth, imageHeight) >= 700
    val originalBroadEdgeCrop = !originalFullImage && originalAreaRatio >= 0.45f && originalEdgeContactCount >= 2
    val originalBroadContext = originalLargeFullImage || originalBroadEdgeCrop
    val refinementOnlyEdgeTrim = changed && warnings.any { it.startsWith("graph_refine.edge_trim") }
    val unresolvedBroadContext = originalBroadContext && (!changed || refinementOnlyEdgeTrim)
    val areaRatio = region.area.toFloat() / safeArea
    val touchesLeft = region.x <= 0
    val touchesTop = region.y <= 0
    val touchesRight = region.right >= imageWidth
    val touchesBottom = region.bottom >= imageHeight
    val edgeContactCount = region.edgeContactCount(imageWidth, imageHeight)
    val fullImage = region.isFullImage(imageWidth, imageHeight)
    val largeImage = maxOf(imageWidth, imageHeight) >= 700
    val largeFullImage = fullImage && largeImage
    val broadEdgeCrop = !fullImage && areaRatio >= 0.45f && edgeContactCount >= 2
    val possibleRotatedPage = imageWidth > imageHeight &&
        maxOf(imageWidth, imageHeight) >= 900 &&
        (fullImage || edgeContactCount >= 2 || areaRatio >= 0.45f)
    val rightAngleRotationSuspected = possibleRotatedPage &&
        imageWidth.toFloat() / imageHeight.coerceAtLeast(1).toFloat() >= 1.12f
    val acceptedForCalculation = !largeFullImage &&
        !broadEdgeCrop &&
        !possibleRotatedPage &&
        !rightAngleRotationSuspected &&
        !unresolvedBroadContext
    val warnings = buildList {
        if (fullImage) add("crop.full_image")
        if (largeFullImage) add("crop.large_full_image_not_calculation_ready")
        if (broadEdgeCrop) add("crop.broad_edge_context_not_calculation_ready")
        if (possibleRotatedPage) add("crop.possible_rotated_page_or_landscape_scan")
        if (rightAngleRotationSuspected) add("crop.right_angle_rotation_required_before_analysis")
        if (edgeContactCount >= 2 && !fullImage) add("crop.touches_multiple_image_edges")
        if (refinementOnlyEdgeTrim) add("crop.refinement_only_edge_trim")
        if (unresolvedBroadContext) add("crop.refinement_not_precise_for_broad_context")
    }

    return OfflineGraphCropQualityAudit(
        areaRatio = areaRatio,
        originalAreaRatio = originalAreaRatio,
        edgeContactCount = edgeContactCount,
        touchesTop = touchesTop,
        touchesRight = touchesRight,
        touchesBottom = touchesBottom,
        touchesLeft = touchesLeft,
        fullImage = fullImage,
        largeFullImage = largeFullImage,
        broadEdgeCrop = broadEdgeCrop,
        possibleRotatedPage = possibleRotatedPage,
        originalBroadContext = originalBroadContext,
        refinementOnlyEdgeTrim = refinementOnlyEdgeTrim,
        unresolvedBroadContext = unresolvedBroadContext,
        rightAngleRotationSuspected = rightAngleRotationSuspected,
        acceptedForCalculation = acceptedForCalculation,
        warnings = warnings,
    )
}

private fun GraphRegion.edgeContactCount(imageWidth: Int, imageHeight: Int): Int =
    listOf(y <= 0, right >= imageWidth, bottom >= imageHeight, x <= 0).count { it }

private fun GraphRegion.toDocumentCorners(): DocumentCorners =
    DocumentCorners(
        topLeft = ImagePoint(x.toFloat(), y.toFloat()),
        topRight = ImagePoint(right.toFloat(), y.toFloat()),
        bottomRight = ImagePoint(right.toFloat(), bottom.toFloat()),
        bottomLeft = ImagePoint(x.toFloat(), bottom.toFloat()),
    )

private fun DocumentCorners.toBoundingRegion(label: String): GraphRegion {
    val xs = listOf(topLeft.x, topRight.x, bottomRight.x, bottomLeft.x)
    val ys = listOf(topLeft.y, topRight.y, bottomRight.y, bottomLeft.y)
    val left = xs.minOrNull()?.roundToInt() ?: 0
    val top = ys.minOrNull()?.roundToInt() ?: 0
    val right = xs.maxOrNull()?.roundToInt() ?: left
    val bottom = ys.maxOrNull()?.roundToInt() ?: top
    return GraphRegion(
        x = left,
        y = top,
        width = (right - left).coerceAtLeast(1),
        height = (bottom - top).coerceAtLeast(1),
        label = label,
    )
}

private fun DocumentCorners.polygonAreaRatio(imageWidth: Int, imageHeight: Int): Float {
    val area = polygonArea(listOf(topLeft, topRight, bottomRight, bottomLeft))
    val imageArea = (imageWidth.toFloat() * imageHeight.toFloat()).coerceAtLeast(1f)
    return area / imageArea
}

private fun DocumentCorners.aspectRatio(): Float {
    val topWidth = distance(topLeft, topRight)
    val bottomWidth = distance(bottomLeft, bottomRight)
    val leftHeight = distance(topLeft, bottomLeft)
    val rightHeight = distance(topRight, bottomRight)
    val width = (topWidth + bottomWidth) / 2f
    val height = ((leftHeight + rightHeight) / 2f).coerceAtLeast(1f)
    return width / height
}

private fun DocumentCorners.normalizedCornerDisplacementWithin(bounds: GraphRegion): Float {
    val diagonal = sqrt((bounds.width * bounds.width + bounds.height * bounds.height).toFloat()).coerceAtLeast(1f)
    val expected = listOf(
        bounds.x.toFloat() to bounds.y.toFloat(),
        bounds.right.toFloat() to bounds.y.toFloat(),
        bounds.right.toFloat() to bounds.bottom.toFloat(),
        bounds.x.toFloat() to bounds.bottom.toFloat(),
    )
    val actual = listOf(topLeft, topRight, bottomRight, bottomLeft)
    return actual.zip(expected)
        .maxOf { (point, expectedPoint) ->
            val dx = point.x - expectedPoint.first
            val dy = point.y - expectedPoint.second
            sqrt(dx * dx + dy * dy)
        } / diagonal
}

private fun normalizedCornerDisplacement(
    corners: DocumentCorners,
    imageWidth: Int,
    imageHeight: Int,
): Float {
    val diagonal = sqrt((imageWidth * imageWidth + imageHeight * imageHeight).toFloat()).coerceAtLeast(1f)
    val expected = listOf(
        0f to 0f,
        imageWidth.toFloat() to 0f,
        imageWidth.toFloat() to imageHeight.toFloat(),
        0f to imageHeight.toFloat(),
    )
    val actual = listOf(corners.topLeft, corners.topRight, corners.bottomRight, corners.bottomLeft)
    return actual.zip(expected)
        .maxOf { (point, expectedPoint) ->
            val dx = point.x - expectedPoint.first
            val dy = point.y - expectedPoint.second
            sqrt(dx * dx + dy * dy)
        } / diagonal
}

private fun DocumentCorners.maxSkewAngleDegrees(): Float {
    val horizontalAngles = listOf(
        lineAngleDegrees(topLeft, topRight),
        lineAngleDegrees(bottomLeft, bottomRight),
    ).map { normalizeAngleDistance(it, 0f) }
    val verticalAngles = listOf(
        lineAngleDegrees(topLeft, bottomLeft),
        lineAngleDegrees(topRight, bottomRight),
    ).map { normalizeAngleDistance(it, 90f) }
    return (horizontalAngles + verticalAngles).maxOrNull() ?: 0f
}

private fun DocumentCorners.maxOrthogonalityResidualDegrees(): Float {
    val sides = listOf(
        lineAngleDegrees(topLeft, topRight),
        lineAngleDegrees(topRight, bottomRight),
        lineAngleDegrees(bottomRight, bottomLeft),
        lineAngleDegrees(bottomLeft, topLeft),
    )
    return listOf(
        adjacentAngleResidual(sides[0], sides[1]),
        adjacentAngleResidual(sides[1], sides[2]),
        adjacentAngleResidual(sides[2], sides[3]),
        adjacentAngleResidual(sides[3], sides[0]),
    ).maxOrNull() ?: 0f
}

private fun lineAngleDegrees(a: ImagePoint, b: ImagePoint): Float =
    (atan2((b.y - a.y).toDouble(), (b.x - a.x).toDouble()) * 180.0 / kotlin.math.PI).toFloat()

private fun normalizeAngleDistance(angle: Float, target: Float): Float {
    var delta = abs(angle - target) % 180f
    if (delta > 90f) delta = 180f - delta
    return delta
}

private fun adjacentAngleResidual(a: Float, b: Float): Float {
    var delta = abs(a - b) % 180f
    if (delta > 90f) delta = 180f - delta
    return abs(90f - delta)
}

private fun polygonArea(points: List<ImagePoint>): Float {
    if (points.size < 3) return 0f
    var area = 0f
    for (index in points.indices) {
        val current = points[index]
        val next = points[(index + 1) % points.size]
        area += current.x * next.y - next.x * current.y
    }
    return abs(area) / 2f
}

private fun distance(a: ImagePoint, b: ImagePoint): Float =
    sqrt((a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y))

private const val CONTROLLED_TUNING_MIN_SNR = 2.0
private const val CONTROLLED_TUNING_REFERENCE_MIN_SNR = 3.0
private const val CONTROLLED_TUNING_MIN_PROMINENCE_REJECTS = 8
private const val CONTROLLED_TUNING_MIN_AREA_PERCENT_REVIEW = 2.5
private const val SPARSE_TRACE_MIN_AREA_PERCENT_REVIEW = 2.5
private const val CONTROLLED_TUNING_MIN_WIDTH_REVIEW = 0.20
private const val CONTROLLED_TUNING_MAX_REVIEW_FRACTION = 0.35
private const val CONTROLLED_TUNING_MAX_BASE_PEAKS = 6
private const val CONTROLLED_TUNING_MAX_EXTRA_PEAKS = 20
private const val CONTROLLED_TUNING_MAX_TOTAL_PEAKS = 32
private const val REPORT_AXIS_CONFIDENCE_REVIEW_THRESHOLD = 0.55f
private const val REPORT_VALUE_INFERRED = "INFERRED"
private const val REPORT_VALUE_NOT_CALCULATED = "NOT_CALCULATED"

private fun emptyAxes(): AxesResult =
    AxesResult(
        xAxis = null,
        yAxis = null,
        origin = null,
        detectionMethod = com.chromalab.feature.processing.pipeline.DetectionMethod.AUTO,
        confidence = 0f,
        timestamp = System.currentTimeMillis(),
    )

private inline fun <T> runStage(
    stage: String,
    stages: MutableList<OfflineStageAudit>,
    graphIndex: Int? = null,
    noinline successMessage: (T) -> String = { "Stage completed." },
    block: () -> T,
): T? {
    val started = System.currentTimeMillis()
    return try {
        val result = block()
        stages += OfflineStageAudit(
            stage = stage,
            graphIndex = graphIndex,
            status = OfflineStageStatus.SUCCESS,
            startedAtMillis = started,
            durationMillis = System.currentTimeMillis() - started,
            message = successMessage(result),
        )
        result
    } catch (error: Throwable) {
        stages += OfflineStageAudit(
            stage = stage,
            graphIndex = graphIndex,
            status = OfflineStageStatus.FAILED,
            startedAtMillis = started,
            durationMillis = System.currentTimeMillis() - started,
            message = error.message ?: error::class.simpleName.orEmpty(),
        )
        null
    }
}

private inline fun <T> runNullableStage(
    stage: String,
    stages: MutableList<OfflineStageAudit>,
    skippedMessage: String,
    block: () -> T?,
): T? {
    val started = System.currentTimeMillis()
    return try {
        val result = block()
        stages += OfflineStageAudit(
            stage = stage,
            status = if (result == null) OfflineStageStatus.SKIPPED else OfflineStageStatus.SUCCESS,
            startedAtMillis = started,
            durationMillis = System.currentTimeMillis() - started,
            message = if (result == null) skippedMessage else "Stage completed.",
        )
        result
    } catch (error: Throwable) {
        stages += OfflineStageAudit(
            stage = stage,
            status = OfflineStageStatus.FAILED,
            startedAtMillis = started,
            durationMillis = System.currentTimeMillis() - started,
            message = error.message ?: error::class.simpleName.orEmpty(),
        )
        null
    }
}

private fun skippedStage(
    stage: String,
    graphIndex: Int? = null,
    message: String,
    warnings: List<String> = emptyList(),
): OfflineStageAudit =
    OfflineStageAudit(
        stage = stage,
        graphIndex = graphIndex,
        status = OfflineStageStatus.SKIPPED,
        startedAtMillis = System.currentTimeMillis(),
        durationMillis = 0L,
        message = message,
        warnings = warnings,
    )

private fun OfflineAnalysisInput.blockedAudit(
    stages: List<OfflineStageAudit>,
    warnings: List<String>,
    blockedAtStage: String,
    normalizedImagePath: String? = null,
    orientationCorrection: OfflineOrientationCorrectionAudit? = null,
    imageWidth: Int? = null,
    imageHeight: Int? = null,
): OfflineAnalysisAudit =
    OfflineAnalysisAudit(
        sourceId = sourceId,
        imagePath = imagePath,
        outputDir = outputDir,
        normalizedImagePath = normalizedImagePath,
        orientationCorrection = orientationCorrection,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        expectedGraphCount = expectedGraphCount,
        detectedGraphCount = 0,
        graphCandidates = emptyList(),
        graphs = emptyList(),
        stages = stages,
        warnings = warnings,
        blockedAtStage = blockedAtStage,
        readyForCalculation = false,
    )

package com.chromalab.feature.processing.bench

import com.chromalab.feature.calculation.core.CalculationEngine
import com.chromalab.feature.calculation.core.CalculationParams
import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.calculation.algorithm.ConfidenceGrade
import com.chromalab.feature.calculation.algorithm.OverlapStatus
import com.chromalab.feature.processing.axis.AxisDetector
import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.calibration.CalibrationPoint
import com.chromalab.feature.processing.calibration.LinearCalibration
import com.chromalab.feature.processing.calibration.PixelCalibration
import com.chromalab.feature.processing.curve.CurveExtractor
import com.chromalab.feature.processing.curve.CurvePoint
import com.chromalab.feature.processing.curve.CurveMaskPreparer
import com.chromalab.feature.processing.document.DocumentDetector
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
import kotlin.math.roundToInt

@Serializable
data class OfflineAnalysisInput(
    val sourceId: String,
    val imagePath: String,
    val outputDir: String,
    val expectedGraphCount: Int? = null,
    val manualAxisCalibrations: List<OfflineManualAxisCalibrationInput> = emptyList(),
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
data class OfflineAnalysisAudit(
    val sourceId: String,
    val imagePath: String,
    val outputDir: String,
    val normalizedImagePath: String?,
    val orientationCorrection: OfflineOrientationCorrectionAudit?,
    val imageWidth: Int?,
    val imageHeight: Int?,
    val expectedGraphCount: Int?,
    val detectedGraphCount: Int,
    val graphCandidates: List<OfflineGraphCandidateAudit>,
    val graphs: List<OfflineGraphAudit>,
    val stages: List<OfflineStageAudit>,
    val warnings: List<String>,
    val blockedAtStage: String?,
    val readyForCalculation: Boolean,
)

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
    val curvePointCount: Int,
    val curveCoverage: Float,
    val curveUsable: Boolean,
    val signal: OfflineSignalAudit,
    val peakDetection: OfflinePeakDetectionAudit,
    val peakMetrics: OfflinePeakMetricsAudit,
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
    val dominantPeakTime: Double?,
    val dominantPeakHeight: Double?,
    val dominantPeakAreaPercent: Double?,
    val baselineMethod: String?,
    val boundaryMethod: String?,
    val integrationMethod: String?,
    val clampNegative: Boolean?,
    val maxPeakWidth: Int?,
    val minSnr: Double?,
    val peaks: List<OfflinePeakAudit> = emptyList(),
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
    val snr: Double,
    val confidence: String,
    val overlapStatus: String,
    val warningCount: Int,
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
        val graphAudits = refinedRegions.mapIndexed { index, refinement ->
            auditGraph(
                graphIndex = index + 1,
                preprocessing = preprocessing,
                outputDir = "${input.outputDir}/graph_${index + 1}",
                refinement = refinement,
                imageWidth = orientation.width,
                imageHeight = orientation.height,
                manualCalibration = manualCalibrationsByGraph[index + 1],
                stages = stages,
            )
        }

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
        val readyForCalculation = cropReady &&
            plotAreaReady &&
            axisGeometryReady &&
            axisCalibrationReady &&
            curveReady &&
            signalReady &&
            peakDetectionReady &&
            peakMetricsReady
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
                    else -> "Calculation is blocked until every graph has auditable peak metrics and integration output."
                },
            )
            stages += skippedStage(
                stage = "report_validation",
                message = "Report validation is blocked until calculation output exists.",
            )
        }

        return OfflineAnalysisAudit(
            sourceId = input.sourceId,
            imagePath = input.imagePath,
            outputDir = input.outputDir,
            normalizedImagePath = normalized.normalizedPath,
            orientationCorrection = orientation.toAudit(),
            imageWidth = orientation.width,
            imageHeight = orientation.height,
            expectedGraphCount = input.expectedGraphCount,
            detectedGraphCount = selectedRegions.size,
            graphCandidates = graphCandidates,
            graphs = graphAudits,
            stages = stages,
            warnings = warnings + graphAudits.flatMap { graph -> graph.warnings.map { "graph_${graph.graphIndex}.$it" } },
            blockedAtStage = when {
                readyForCalculation -> null
                !cropReady -> "crop_quality"
                !plotAreaReady -> "plot_area"
                !axisGeometryReady -> "axis_detect"
                !axisCalibrationReady -> "axis_calibration"
                !curveReady -> "curve_extract"
                !signalReady -> "signal_convert"
                !peakDetectionReady -> "peak_detection"
                else -> "peak_metrics"
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
            ocrReader.readAxisLabels(analysisImagePath, region)
        }
        if (ocrResult == null || ocrResult.status == OcrStatus.NOT_AVAILABLE) {
            graphWarnings += "axis_ocr_not_available"
        }

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
            curvePointCount = curveResult?.points?.size ?: 0,
            curveCoverage = curveResult?.coverage ?: 0f,
            curveUsable = curveResult?.isUsable == true,
            signal = signalAudit,
            peakDetection = peakDetectionAudit,
            peakMetrics = peakMetricsAudit,
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
    val userConfirmed = ocrResult?.status == OcrStatus.ACCEPTED || ocrResult?.status == OcrStatus.CORRECTED
    val ready = xReady && yReady && userConfirmed

    if (!xReady) warnings += "axis_calibration.x_requires_two_pixel_value_points"
    if (!yReady) warnings += "axis_calibration.y_requires_two_pixel_value_points"
    if (xReady && yReady && !userConfirmed) warnings += "axis_calibration.user_confirmation_required"
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
): OfflinePeakDetectionResult {
    val run = CalculationEngine.execute(
        signal = signal,
        sourceId = sourceId,
        params = params,
    )
    val peaksByArea = run.peaks.sortedByDescending { abs(it.area) }
    val dominantPeak = peaksByArea.firstOrNull()
    val warnings = buildList {
        if (!run.validation.isValid) add("peak_detection.signal_validation_failed")
        if (run.peaks.isEmpty()) add("peak_detection.no_peaks_detected")
        run.validation.warnings.forEach { add("validation.$it") }
        run.warnings.forEach { warning ->
            add("${warning.stage}.${warning.message}")
        }
    }

    val audit = OfflinePeakDetectionAudit(
        ready = run.validation.isValid && run.peaks.isNotEmpty(),
        peakCount = run.peaks.size,
        significantPeakCount = run.peaks.count { it.snr >= params.minSnr },
        dominantPeakTime = dominantPeak?.rtApex,
        dominantPeakHeight = dominantPeak?.height,
        dominantPeakAreaPercent = dominantPeak?.areaPercent,
        baselineMethod = params.baselineMethod,
        boundaryMethod = params.boundaryMethod,
        integrationMethod = params.integrationMethod,
        clampNegative = params.clampNegative,
        maxPeakWidth = params.maxPeakWidth,
        minSnr = params.minSnr,
        peaks = run.peaks.mapIndexed { index, peak ->
            OfflinePeakAudit(
                peakNumber = index + 1,
                rtApex = peak.rtApex,
                leftBoundaryTime = peak.leftBoundaryTime,
                rightBoundaryTime = peak.rightBoundaryTime,
                height = peak.height,
                area = peak.area,
                areaPercent = peak.areaPercent,
                snr = peak.snr,
                confidence = peak.confidence.name,
                overlapStatus = peak.overlapStatus.name,
                warningCount = peak.warnings.size,
            )
        },
        warnings = warnings.distinct(),
    )

    return OfflinePeakDetectionResult(
        audit = audit,
        run = run,
        params = params,
    )
}

private fun missingPeakDetection(warnings: List<String>): OfflinePeakDetectionAudit =
    OfflinePeakDetectionAudit(
        ready = false,
        peakCount = 0,
        significantPeakCount = 0,
        dominantPeakTime = null,
        dominantPeakHeight = null,
        dominantPeakAreaPercent = null,
        baselineMethod = null,
        boundaryMethod = null,
        integrationMethod = null,
        clampNegative = null,
        maxPeakWidth = null,
        minSnr = null,
        peaks = emptyList(),
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

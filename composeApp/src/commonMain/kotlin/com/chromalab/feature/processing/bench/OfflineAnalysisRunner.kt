package com.chromalab.feature.processing.bench

import com.chromalab.feature.processing.axis.AxisDetector
import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.curve.CurveExtractor
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
import com.chromalab.feature.processing.ocr.OcrStatus
import com.chromalab.feature.processing.preprocess.ImagePreprocessor
import com.chromalab.feature.processing.preprocess.PreprocessingResult
import com.chromalab.feature.processing.preprocess.PreprocessingVariantRanker
import com.chromalab.feature.processing.preprocess.PreprocessingVariantScore
import com.chromalab.feature.processing.preprocess.variants
import kotlinx.serialization.Serializable

@Serializable
data class OfflineAnalysisInput(
    val sourceId: String,
    val imagePath: String,
    val outputDir: String,
    val expectedGraphCount: Int? = null,
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
    val curveMaskAvailable: Boolean,
    val curveMaskRawPixelCount: Int,
    val curveMaskCleanPixelCount: Int,
    val curveMaskSuppressionApplied: List<String>,
    val curvePointCount: Int,
    val curveCoverage: Float,
    val curveUsable: Boolean,
    val warnings: List<String>,
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

        val graphAudits = refinedRegions.mapIndexed { index, refinement ->
            auditGraph(
                graphIndex = index + 1,
                preprocessing = preprocessing,
                outputDir = "${input.outputDir}/graph_${index + 1}",
                refinement = refinement,
                imageWidth = orientation.width,
                imageHeight = orientation.height,
                stages = stages,
            )
        }

        val cropReady = graphAudits.isNotEmpty() &&
            graphAudits.all { it.cropQuality.acceptedForCalculation && it.cropBoundaryRisk.acceptedForCalculation }
        val plotAreaReady = graphAudits.isNotEmpty() && graphAudits.all { it.plotArea.detected && it.plotArea.region != null }
        val axisLabelsReady = graphAudits.isNotEmpty() &&
            graphAudits.all { it.xSuggestionCount >= 2 && it.ySuggestionCount >= 2 }
        val axisGeometryReady = graphAudits.isNotEmpty() &&
            graphAudits.all { it.axesDetected && it.originDetected }
        val curveReady = graphAudits.isNotEmpty() && graphAudits.all { it.curveUsable }
        val readyForCalculation = cropReady && plotAreaReady && axisLabelsReady && axisGeometryReady && curveReady
        if (!readyForCalculation) {
            stages += skippedStage(
                stage = "calculation",
                message = when {
                    !cropReady -> "Calculation is blocked until every graph has accepted crop bounds."
                    !plotAreaReady -> "Calculation is blocked until every graph has audited plot-area bounds."
                    !axisLabelsReady -> "Calculation is blocked until every graph has usable axis OCR labels."
                    !axisGeometryReady -> "Calculation is blocked until every graph has detected axis geometry and origin."
                    else -> "Calculation is blocked until every graph has usable extracted curve data."
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
                !axisLabelsReady -> "axis_ocr"
                !axisGeometryReady -> "axis_detect"
                else -> "curve_extract"
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
            curveMaskAvailable = maskAvailable,
            curveMaskRawPixelCount = maskResult?.rawPixelCount ?: 0,
            curveMaskCleanPixelCount = maskResult?.cleanPixelCount ?: 0,
            curveMaskSuppressionApplied = maskResult?.suppressionApplied.orEmpty(),
            curvePointCount = curveResult?.points?.size ?: 0,
            curveCoverage = curveResult?.coverage ?: 0f,
            curveUsable = curveResult?.isUsable == true,
            warnings = graphWarnings,
        )
    }
}

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
): OfflineStageAudit =
    OfflineStageAudit(
        stage = stage,
        graphIndex = graphIndex,
        status = OfflineStageStatus.SKIPPED,
        startedAtMillis = System.currentTimeMillis(),
        durationMillis = 0L,
        message = message,
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

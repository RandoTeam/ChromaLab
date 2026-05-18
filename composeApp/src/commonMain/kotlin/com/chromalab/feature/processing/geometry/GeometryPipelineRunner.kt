package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.axis.AxisDetector
import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.calibration.AxisCalibrationFitter
import com.chromalab.feature.processing.graph.DetectionConfidence
import com.chromalab.feature.processing.graph.GraphPlotAreaDetector
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.graph.GraphRegionBoundaryCorrector
import com.chromalab.feature.processing.graph.GraphRegionDetector
import com.chromalab.feature.processing.graph.GraphRegionResult
import com.chromalab.feature.processing.graph.requiresGraphPanelBoundaryMode
import com.chromalab.feature.processing.inference.ChartAnalysisReader
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrTextElement
import com.chromalab.feature.processing.peaks.PeakLabelEvidence
import com.chromalab.feature.processing.peaks.PeakLabelEvidenceReader
import com.chromalab.feature.processing.pipeline.DetectionMethod
import kotlin.math.abs
import kotlin.math.roundToInt

class GeometryPipelineRunner(
    private val graphDetector: GraphRegionDetector = GraphRegionDetector(),
    private val graphBoundaryCorrector: GraphRegionBoundaryCorrector = GraphRegionBoundaryCorrector(),
    private val plotAreaDetector: GraphPlotAreaDetector = GraphPlotAreaDetector(),
    private val axisDetector: AxisDetector = AxisDetector(),
    private val axisTickGeometryDetector: AxisTickGeometryDetector = AxisTickGeometryDetector(),
    private val tickCropArtifactWriter: TickOcrCropArtifactWriter = TickOcrCropArtifactWriter(),
    private val chartReader: ChartAnalysisReader = ChartAnalysisReader(),
    private val peakLabelEvidenceReader: PeakLabelEvidenceReader = PeakLabelEvidenceReader(),
    private val calibrationFitter: AxisCalibrationFitter = AxisCalibrationFitter(),
) {
    suspend fun run(
        imagePath: String,
        outputDir: String,
        imageWidth: Int,
        imageHeight: Int,
        originalImagePath: String = imagePath,
        normalizedImagePath: String = imagePath,
        sourceType: SourceType = SourceType.UNKNOWN,
        originalWidth: Int = imageWidth,
        originalHeight: Int = imageHeight,
        exifRotation: Int = 0,
        appliedRotation: Int = 0,
        mlKitSmartScanUsed: Boolean = false,
        mlKitCropFilterDeskewMetadataAvailable: Boolean = false,
        cachedGraphResult: GraphRegionResult? = null,
        overridePanel: GraphRegion? = null,
        preservePanelLabels: Boolean = false,
        runVlmHint: Boolean = true,
        runTickOcr: Boolean = true,
    ): GeometryPipelineResult {
        require(imageWidth > 0 && imageHeight > 0) {
            "Image dimensions are required before geometry pipeline."
        }

        val startedAt = System.currentTimeMillis()
        val provenance = SourceProvenance(
            originalImagePath = originalImagePath,
            normalizedImagePath = normalizedImagePath,
            sourceType = sourceType,
            originalWidth = originalWidth,
            originalHeight = originalHeight,
            normalizedWidth = imageWidth,
            normalizedHeight = imageHeight,
            exifRotation = exifRotation,
            appliedRotation = appliedRotation,
            mlKitSmartScanUsed = mlKitSmartScanUsed,
            mlKitCropFilterDeskewMetadataAvailable = mlKitCropFilterDeskewMetadataAvailable,
            warnings = buildList {
                if (mlKitSmartScanUsed && !mlKitCropFilterDeskewMetadataAvailable) {
                    add("source.mlkit_metadata_missing")
                }
            },
        )
        val rectification = buildIdentityRectification(
            imagePath = imagePath,
            sourceType = sourceType,
            mlKitSmartScanUsed = mlKitSmartScanUsed,
        )

        val graphResult = cachedGraphResult ?: runCatching {
            graphDetector.detect(imagePath, imageWidth, imageHeight)
        }.getOrNull()
        val vlmHintResult = if (runVlmHint && overridePanel == null) {
            runCatching {
                chartReader.detectGraphRegion(imagePath, imageWidth, imageHeight)
                    ?.toGraphRegionResult(imageWidth, imageHeight)
            }.getOrNull()
        } else {
            null
        }
        val candidates = buildRoiCandidates(
            imagePath = imagePath,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            overridePanel = overridePanel,
            cvGraphResult = graphResult,
            vlmGraphResult = vlmHintResult,
            preservePanelLabels = preservePanelLabels,
        )
        val evaluatedCandidates = candidates
            .take(MAX_GEOMETRY_RETRY_CANDIDATES)
            .mapIndexed { retryIndex, candidate ->
                evaluateCandidate(
                    imagePath = imagePath,
                    imageWidth = imageWidth,
                    imageHeight = imageHeight,
                    candidate = candidate,
                    retryIndex = retryIndex,
                    runTickOcr = runTickOcr,
                    outputDir = outputDir,
                )
            }
        val selectedEvaluation = evaluatedCandidates.maxWithOrNull(
            compareBy<GeometryCandidateEvaluation> { it.reportStatus.selectionPriority }
                .thenBy { it.selectionScore },
        )
        val selected = selectedEvaluation?.panel
        val selectedPlot = selectedEvaluation?.plot
        val axisGeometry = selectedEvaluation?.axisGeometry ?: (null as AxesResult?).toAxisGeometry(null)
        val tickGeometry = selectedEvaluation?.tickGeometry ?: (null as AxisTickGeometryResult?).toTickGeometry()
        val tickOcr = selectedEvaluation?.tickOcr ?: TickOcrResult(
            warnings = listOf("tick_ocr.not_available"),
            timestamp = System.currentTimeMillis(),
        )
        val xFit = selectedEvaluation?.xFit ?: calibrationFitter.fit(GeometryAxis.X, emptyList())
        val yFit = selectedEvaluation?.yFit ?: calibrationFitter.fit(GeometryAxis.Y, emptyList())
        val reportStatus = selectedEvaluation?.reportStatus ?: GeometryReportStatus.DIAGNOSTIC_ONLY
        val warnings = buildList {
            addAll(provenance.warnings)
            addAll(rectification.warnings)
            if (selected == null) add("geometry.roi.no_candidate_selected")
            if (selectedPlot == null) add("geometry.plot_area.not_validated")
            if (selectedEvaluation != null && selectedEvaluation.retryIndex > 0) {
                add("geometry.retry_ladder_selected_candidate_${selectedEvaluation.retryIndex + 1}")
            }
            if (reportStatus == GeometryReportStatus.DIAGNOSTIC_ONLY && evaluatedCandidates.size > 1) {
                add("geometry.retry_ladder_exhausted")
            }
            if (candidates.size > evaluatedCandidates.size) {
                add("geometry.retry_ladder_limited:${evaluatedCandidates.size}/${candidates.size}")
            }
            addAll(selected?.warnings.orEmpty())
            addAll(selectedPlot?.warnings.orEmpty())
            addAll(axisGeometry.warnings)
            addAll(tickGeometry.warnings)
            addAll(tickOcr.warnings)
            selectedEvaluation?.peakLabelEvidence
                ?.flatMap { it.warnings }
                ?.let { addAll(it) }
            addAll(xFit.warnings)
            addAll(yFit.warnings)
        }.distinct()
        val trace = GeometryTrace(
            sourceProvenance = provenance,
            pageRectification = rectification,
            roiCandidates = candidates,
            selectedGraphPanelBounds = selected,
            selectedPlotAreaBounds = selectedPlot,
            axisGeometry = axisGeometry,
            tickGeometry = tickGeometry,
            tickOcrResult = tickOcr,
            xCalibrationFit = xFit,
            yCalibrationFit = yFit,
            originalImagePath = originalImagePath,
            normalizedImagePath = normalizedImagePath,
            rectifiedImagePath = rectification.rectifiedImagePath,
            selectedGraphPanelOverlayPath = selected?.overlayArtifactPath,
            selectedPlotAreaOverlayPath = selectedPlot?.overlayArtifactPath,
            ocrCropPaths = (
                selectedEvaluation?.tickCropArtifacts?.map { it.path }.orEmpty() +
                    tickOcr.items.mapNotNull { it.localCropPath }
                ).distinct(),
            peakLabelEvidence = selectedEvaluation?.peakLabelEvidence.orEmpty(),
            peakLabelCropPaths = selectedEvaluation?.peakLabelCropPaths.orEmpty(),
            warnings = warnings,
            timings = listOf(
                GeometryStageTiming(
                    stageId = "geometry_pipeline",
                    durationMillis = System.currentTimeMillis() - startedAt,
                ),
            ),
        )
        return GeometryPipelineResult(
            trace = trace,
            reportStatus = reportStatus,
            graphPanelBounds = selected,
            plotAreaBounds = selectedPlot,
            axisGeometry = axisGeometry,
            tickGeometry = tickGeometry,
            tickOcrResult = tickOcr,
            xCalibrationFit = xFit,
            yCalibrationFit = yFit,
            warnings = warnings,
        )
    }

    private suspend fun evaluateCandidate(
        imagePath: String,
        imageWidth: Int,
        imageHeight: Int,
        candidate: GraphPanelBounds,
        retryIndex: Int,
        runTickOcr: Boolean,
        outputDir: String,
    ): GeometryCandidateEvaluation {
        val plot = runCatching {
            plotAreaDetector.detect(imagePath, candidate.region, imageWidth, imageHeight)
        }.getOrNull()?.takeIf { it.detected && it.plotArea != null }?.let {
            PlotAreaBounds(
                region = it.plotArea!!,
                parentGraphPanelRegion = it.panelRegion,
                confidence = (candidate.confidence * 0.85f + 0.10f).coerceIn(0f, 1f),
                warnings = it.warnings,
            )
        }
        val axes = runCatching {
            axisDetector.detect(imagePath, plot?.region ?: candidate.region)
        }.getOrNull()
        val axisTick = runCatching {
            axisTickGeometryDetector.detect(
                imagePath = imagePath,
                graphIndex = 1,
                panelRegion = candidate.region,
                plotRegion = plot?.region,
            )
        }.getOrNull()
        val axisGeometry = axes.toAxisGeometry(axisTick)
        val tickGeometry = axisTick.toTickGeometry()
        val tickCropArtifacts = if (plot != null) {
            runCatching {
                tickCropArtifactWriter.writeTickCrops(
                    imagePath = imagePath,
                    outputDir = outputDir,
                    panelRegion = candidate.region,
                    plotRegion = plot.region,
                    tickGeometry = tickGeometry,
                    candidateIndex = retryIndex,
                )
            }.getOrElse { emptyList() }
        } else {
            emptyList()
        }
        val axisOcr = if (runTickOcr && tickCropArtifacts.isNotEmpty()) {
            runCatching { chartReader.readTickLabelCrops(tickCropArtifacts) }.getOrNull()
        } else {
            null
        }
        val peakLabelEvidence = if (plot != null) {
            runCatching {
                peakLabelEvidenceReader.readPeakLabels(
                    imagePath = imagePath,
                    outputDir = outputDir,
                    graphPanelBounds = candidate.region,
                    plotAreaBounds = plot.region,
                )
            }.getOrElse {
                com.chromalab.feature.processing.peaks.PeakLabelEvidenceResult(
                    warnings = listOf("peak_label_ocr.runtime_reader_failed"),
                )
            }
        } else {
            com.chromalab.feature.processing.peaks.PeakLabelEvidenceResult(
                warnings = listOf("peak_label_ocr.plot_area_missing"),
            )
        }
        val tickOcr = axisOcr.toTickOcrResult(candidate.region, tickGeometry, tickCropArtifacts)
        val xFit = calibrationFitter.fit(
            axis = GeometryAxis.X,
            anchors = tickOcr.acceptedItems
                .filter { it.axis == GeometryAxis.X && it.parsedNumericValue != null && it.tickPixelPosition != null }
                .map {
                    CalibrationAnchorEvidence(
                        axis = GeometryAxis.X,
                        tickPixelPosition = (it.tickPixelPosition!! - (plot?.region?.x ?: 0)).coerceAtLeast(0f),
                        value = it.parsedNumericValue!!,
                        rawText = it.rawText,
                        localCropPath = it.localCropPath,
                        confidence = it.confidence,
                    )
                },
            axisLengthPx = plot?.region?.width?.toFloat() ?: candidate.region.width.toFloat(),
            geometryCleanliness = axisGeometry.axisConfidence,
        )
        val yFit = calibrationFitter.fit(
            axis = GeometryAxis.Y,
            anchors = tickOcr.acceptedItems
                .filter { it.axis == GeometryAxis.Y && it.parsedNumericValue != null && it.tickPixelPosition != null }
                .map {
                    CalibrationAnchorEvidence(
                        axis = GeometryAxis.Y,
                        tickPixelPosition = (it.tickPixelPosition!! - (plot?.region?.y ?: 0)).coerceAtLeast(0f),
                        value = it.parsedNumericValue!!,
                        rawText = it.rawText,
                        localCropPath = it.localCropPath,
                        confidence = it.confidence,
                    )
                },
            axisLengthPx = plot?.region?.height?.toFloat() ?: candidate.region.height.toFloat(),
            geometryCleanliness = axisGeometry.axisConfidence,
        )
        val reportStatus = when {
            plot == null -> GeometryReportStatus.DIAGNOSTIC_ONLY
            xFit.status == CalibrationFitStatus.VALID && yFit.status == CalibrationFitStatus.VALID ->
                GeometryReportStatus.SCIENTIFIC_READY
            xFit.status == CalibrationFitStatus.INVALID || yFit.status == CalibrationFitStatus.INVALID ->
                GeometryReportStatus.DIAGNOSTIC_ONLY
            else -> GeometryReportStatus.REVIEW_READY
        }
        val calibrationScore = xFit.confidence * 18f + yFit.confidence * 18f
        val tickScore = (tickGeometry.xTicks.size.coerceAtMost(8) + tickGeometry.yTicks.size.coerceAtMost(8)) * 1.5f
        return GeometryCandidateEvaluation(
            panel = candidate,
            plot = plot,
            axisGeometry = axisGeometry,
            tickGeometry = tickGeometry,
            tickOcr = tickOcr,
            tickCropArtifacts = tickCropArtifacts,
            peakLabelEvidence = peakLabelEvidence.labels,
            peakLabelCropPaths = peakLabelEvidence.cropPaths,
            xFit = xFit,
            yFit = yFit,
            reportStatus = reportStatus,
            retryIndex = retryIndex,
            selectionScore = candidate.scoreBreakdown.total + calibrationScore + tickScore,
        )
    }

    private fun buildRoiCandidates(
        imagePath: String,
        imageWidth: Int,
        imageHeight: Int,
        overridePanel: GraphRegion?,
        cvGraphResult: GraphRegionResult?,
        vlmGraphResult: GraphRegionResult?,
        preservePanelLabels: Boolean,
    ): List<GraphPanelBounds> {
        val rawCandidates = buildList {
            overridePanel?.let { add(RawRoiCandidate(it, GeometryCandidateSource.USER, 1.0f, listOf("roi.user_override"))) }
            cvGraphResult?.let { result ->
                result.sortedRegions.forEach {
                    add(RawRoiCandidate(it, GeometryCandidateSource.CV, result.confidence.toBaseConfidence(), result.warnings))
                }
            }
            vlmGraphResult?.let { result ->
                result.sortedRegions.forEach {
                    add(RawRoiCandidate(it, GeometryCandidateSource.VLM_HINT, 0.48f, result.warnings))
                }
            }
            add(
                RawRoiCandidate(
                    GraphRegion(0, 0, imageWidth, imageHeight, "Full image fallback"),
                    GeometryCandidateSource.FULL_IMAGE_FALLBACK,
                    0.18f,
                    listOf("roi.full_image_fallback"),
                ),
            )
        }
            .flatMap { candidate -> listOf(candidate) + candidate.expandedVariants(imageWidth, imageHeight) }
            .distinctBy { "${it.source}:${it.region.x}:${it.region.y}:${it.region.width}:${it.region.height}" }

        return rawCandidates.map { candidate ->
            val corrected = if (candidate.source == GeometryCandidateSource.CV || candidate.source == GeometryCandidateSource.USER) {
                runCatching {
                    graphBoundaryCorrector.correct(
                        imagePath = imagePath,
                        region = candidate.region,
                        imageWidth = imageWidth,
                        imageHeight = imageHeight,
                        preservePanelLabels = preservePanelLabels ||
                            candidate.region.requiresGraphPanelBoundaryMode(imageWidth, imageHeight),
                    )
                }.getOrNull()?.correctedRegion ?: candidate.region
            } else {
                candidate.region
            }.clampToImage(imageWidth, imageHeight)
            val plot = runCatching {
                plotAreaDetector.detect(imagePath, corrected, imageWidth, imageHeight)
            }.getOrNull()
            val ticks = runCatching {
                axisTickGeometryDetector.detect(imagePath, 1, corrected, plot?.plotArea)
            }.getOrNull()
            val score = scoreCandidate(
                region = corrected,
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                baseConfidence = candidate.baseConfidence,
                plotDetected = plot?.detected == true && plot.plotArea != null,
                xTicks = ticks?.xTickPositions?.size ?: 0,
                yTicks = ticks?.yTickPositions?.size ?: 0,
                warnings = candidate.warnings + plot?.warnings.orEmpty() + ticks?.warnings.orEmpty(),
            )
            GraphPanelBounds(
                region = corrected,
                candidateSource = candidate.source,
                confidence = score.total.coerceIn(0f, 100f) / 100f,
                scoreBreakdown = score,
                warnings = candidate.warnings + plot?.warnings.orEmpty() + ticks?.warnings.orEmpty(),
            )
        }.sortedByDescending { it.scoreBreakdown.total }
    }
}

private data class RawRoiCandidate(
    val region: GraphRegion,
    val source: GeometryCandidateSource,
    val baseConfidence: Float,
    val warnings: List<String> = emptyList(),
)

private data class GeometryCandidateEvaluation(
    val panel: GraphPanelBounds,
    val plot: PlotAreaBounds?,
    val axisGeometry: AxisGeometry,
    val tickGeometry: TickGeometry,
    val tickOcr: TickOcrResult,
    val tickCropArtifacts: List<TickOcrCropArtifact>,
    val peakLabelEvidence: List<PeakLabelEvidence>,
    val peakLabelCropPaths: List<String>,
    val xFit: AxisCalibrationFit,
    val yFit: AxisCalibrationFit,
    val reportStatus: GeometryReportStatus,
    val retryIndex: Int,
    val selectionScore: Float,
)

private const val MAX_GEOMETRY_RETRY_CANDIDATES = 6

private val GeometryReportStatus.selectionPriority: Int
    get() = when (this) {
        GeometryReportStatus.SCIENTIFIC_READY -> 3
        GeometryReportStatus.REVIEW_READY -> 2
        GeometryReportStatus.DIAGNOSTIC_ONLY -> 1
    }

private fun RawRoiCandidate.expandedVariants(imageWidth: Int, imageHeight: Int): List<RawRoiCandidate> {
    if (source == GeometryCandidateSource.FULL_IMAGE_FALLBACK) return emptyList()
    return listOf(0.035f, 0.075f).mapNotNull { ratio ->
        val dx = (region.width * ratio).roundToInt().coerceAtLeast(2)
        val dy = (region.height * ratio).roundToInt().coerceAtLeast(2)
        val expanded = GraphRegion(
            x = region.x - dx,
            y = region.y - dy,
            width = region.width + dx * 2,
            height = region.height + dy * 2,
            label = region.label.ifBlank { "Expanded ROI" },
        ).clampToImage(imageWidth, imageHeight)
        if (expanded == region) null else copy(
            region = expanded,
            baseConfidence = (baseConfidence * 0.96f).coerceIn(0f, 1f),
            warnings = warnings + "roi.expanded_${(ratio * 100).roundToInt()}pct",
        )
    }
}

private fun scoreCandidate(
    region: GraphRegion,
    imageWidth: Int,
    imageHeight: Int,
    baseConfidence: Float,
    plotDetected: Boolean,
    xTicks: Int,
    yTicks: Int,
    warnings: List<String>,
): RoiCandidateScoreBreakdown {
    val axisVisibility = if (plotDetected) 18f else 4f
    val tickLabelVisibility = (minOf(xTicks, 6) + minOf(yTicks, 6)) * 2.2f
    val plotScore = if (plotDetected) 20f else 0f
    val traceDensity = 8f
    val textPenalty = if (region.area.toFloat() / (imageWidth * imageHeight).coerceAtLeast(1).toFloat() > 0.92f) 8f else 0f
    val marginSafety = region.marginSafety(imageWidth, imageHeight) * 10f
    val aspect = when (region.aspectRatio) {
        in 1.0f..4.6f -> 10f
        in 0.65f..5.5f -> 5f
        else -> 0f
    }
    val calibrationViability = when {
        xTicks >= 3 && yTicks >= 3 -> 14f
        xTicks >= 2 && yTicks >= 2 -> 8f
        else -> 0f
    }
    val warningPenalty = warnings.distinct().size * 0.7f
    val total = (
        baseConfidence * 18f +
            axisVisibility +
            tickLabelVisibility +
            plotScore +
            traceDensity +
            marginSafety +
            aspect +
            calibrationViability -
            textPenalty -
            warningPenalty
        ).coerceAtLeast(0f)
    return RoiCandidateScoreBreakdown(
        axisVisibility = axisVisibility,
        tickLabelVisibility = tickLabelVisibility,
        graphFramePlotRectangleConfidence = plotScore,
        tracePixelDensity = traceDensity,
        textContaminationPenalty = textPenalty,
        marginSafety = marginSafety,
        aspectRatioPlausibility = aspect,
        calibrationViability = calibrationViability,
        curveCoverageScore = traceDensity,
        total = total,
        notes = warnings.distinct(),
    )
}

private fun buildIdentityRectification(
    imagePath: String,
    sourceType: SourceType,
    mlKitSmartScanUsed: Boolean,
): PageRectificationResult {
    val status = when {
        sourceType == SourceType.SCREENSHOT || mlKitSmartScanUsed -> GeometryStageStatus.SKIPPED_NOT_NEEDED
        else -> GeometryStageStatus.SKIPPED_NOT_CONFIDENT
    }
    return PageRectificationResult(
        pageQuadPx = null,
        homography = null,
        rectifiedImagePath = imagePath,
        perspectiveApplied = false,
        excessiveWarp = false,
        skewResidualPx = null,
        straightnessScore = null,
        confidence = if (status == GeometryStageStatus.SKIPPED_NOT_NEEDED) 0.75f else 0.0f,
        status = status,
        warnings = if (status == GeometryStageStatus.SKIPPED_NOT_CONFIDENT) {
            listOf("perspective.no_reliable_quadrilateral_identity_preserved")
        } else {
            listOf("perspective.identity_no_warp_needed")
        },
    )
}

private fun GraphRegionResult.toGraphPanelBoundsSource(): GeometryCandidateSource =
    if (confidence == DetectionConfidence.MANUAL) GeometryCandidateSource.USER else GeometryCandidateSource.CV

private fun DetectionConfidence.toBaseConfidence(): Float =
    when (this) {
        DetectionConfidence.HIGH -> 0.88f
        DetectionConfidence.MEDIUM -> 0.68f
        DetectionConfidence.LOW -> 0.38f
        DetectionConfidence.MANUAL -> 0.95f
    }

private fun AxesResult?.toAxisGeometry(axisTick: AxisTickGeometryResult?): AxisGeometry {
    val axes = this
    return AxisGeometry(
        xAxisLinePx = axisTick?.xAxis ?: axes?.xAxis,
        yAxisLinePx = axisTick?.yAxis ?: axes?.yAxis,
        originPx = axisTick?.origin ?: axes?.origin,
        axisConfidence = maxOf(axes?.confidence ?: 0f, if (axisTick?.available == true) 0.72f else 0f),
        warnings = buildList {
            addAll(axes?.warnings.orEmpty())
            addAll(axisTick?.warnings.orEmpty())
            if (axisTick?.available != true && axes?.hasAxes != true) add("axis_geometry.not_available")
        }.distinct(),
    )
}

private fun AxisTickGeometryResult?.toTickGeometry(): TickGeometry =
    TickGeometry(
        xTicks = this?.xTickPositions.orEmpty().map {
            TickPixelPosition(it, TickDirection.DOWN, confidence = 0.72f)
        },
        yTicks = this?.yTickPositions.orEmpty().map {
            TickPixelPosition(it, TickDirection.LEFT, confidence = 0.72f)
        },
        source = this?.source ?: "deterministic_cv_unavailable",
        warnings = this?.warnings.orEmpty(),
    )

private fun AxisOcrResult?.toTickOcrResult(
    panelRegion: GraphRegion?,
    ticks: TickGeometry,
    cropArtifacts: List<TickOcrCropArtifact> = emptyList(),
): TickOcrResult {
    val ocr = this ?: return TickOcrResult(
        warnings = listOf("tick_ocr.not_available"),
        timestamp = System.currentTimeMillis(),
    )
    val panel = panelRegion ?: return TickOcrResult(
        warnings = listOf("tick_ocr.panel_missing"),
        timestamp = ocr.timestamp,
    )
    val xTolerance = (panel.width * 0.035f).coerceAtLeast(8f)
    val yTolerance = (panel.height * 0.035f).coerceAtLeast(8f)
    val items = ocr.rawElements.mapNotNull { element ->
        val value = element.numericValue ?: return@mapNotNull null
        val xItem = element.toTickOcrItemOrNull(
            axis = GeometryAxis.X,
            ticks = ticks.xTicks.map { it.pixelCoordinate },
            tolerance = xTolerance,
            coordinate = element.centerX,
            expectedBand = element.centerY >= panel.y + panel.height * 0.45f,
            cropArtifacts = cropArtifacts,
        )
        val yItem = element.toTickOcrItemOrNull(
            axis = GeometryAxis.Y,
            ticks = ticks.yTicks.map { it.pixelCoordinate },
            tolerance = yTolerance,
            coordinate = element.centerY,
            expectedBand = element.centerX <= panel.x + panel.width * 0.42f,
            cropArtifacts = cropArtifacts,
        )
        val item = listOfNotNull(xItem, yItem).maxByOrNull {
            if (it.status == TickOcrItemStatus.ACCEPTED) 1 else 0
        }
        item?.copy(parsedNumericValue = value.toDouble())
    }
    val semanticOnlyCount = items.count { it.status == TickOcrItemStatus.SEMANTIC_ONLY }
    return TickOcrResult(
        items = items,
        warnings = buildList {
            addAll(ocr.warnings)
            if (ticks.xTicks.size < 2) add("tick_geometry.x_positions_insufficient")
            if (ticks.yTicks.size < 2) add("tick_geometry.y_positions_insufficient")
            if ((ticks.xTicks.isNotEmpty() || ticks.yTicks.isNotEmpty()) && cropArtifacts.isEmpty()) {
                add("tick_ocr.local_crops_missing")
            }
            if (semanticOnlyCount > 0) add("tick_ocr.semantic_only:$semanticOnlyCount")
        }.distinct(),
        timestamp = ocr.timestamp,
    )
}

private fun OcrTextElement.toTickOcrItemOrNull(
    axis: GeometryAxis,
    ticks: List<Float>,
    tolerance: Float,
    coordinate: Float,
    expectedBand: Boolean,
    cropArtifacts: List<TickOcrCropArtifact>,
): TickOcrItem? {
    if (!expectedBand) return null
    val nearest = ticks.minByOrNull { abs(it - coordinate) }
    val distance = nearest?.let { abs(it - coordinate) }
    val status = when {
        nearest == null -> TickOcrItemStatus.SEMANTIC_ONLY
        distance != null && distance <= tolerance -> TickOcrItemStatus.ACCEPTED
        else -> TickOcrItemStatus.SEMANTIC_ONLY
    }
    return TickOcrItem(
        axis = axis,
        tickPixelPosition = nearest?.takeIf { status == TickOcrItemStatus.ACCEPTED },
        localCropPath = nearest
            ?.takeIf { status == TickOcrItemStatus.ACCEPTED }
            ?.let { cropArtifacts.pathFor(axis, it) },
        rawText = text,
        parsedNumericValue = numericValue?.toDouble(),
        ocrEngine = TickOcrEngine.BOTH,
        confidence = confidence,
        status = status,
        rejectionReason = if (status == TickOcrItemStatus.SEMANTIC_ONLY) {
            "tick_ocr.numeric_value_without_deterministic_tick_position"
        } else {
            null
        },
    )
}

private fun List<TickOcrCropArtifact>.pathFor(axis: GeometryAxis, tickPixelPosition: Float): String? =
    firstOrNull {
        it.axis == axis && abs(it.tickPixelPosition - tickPixelPosition) <= 0.5f
    }?.path

private fun com.chromalab.feature.processing.inference.GraphBounds.toGraphRegionResult(
    imageWidth: Int,
    imageHeight: Int,
): GraphRegionResult {
    val left = (leftPct / 100f * imageWidth).roundToInt().coerceIn(0, imageWidth)
    val top = (topPct / 100f * imageHeight).roundToInt().coerceIn(0, imageHeight)
    val right = (rightPct / 100f * imageWidth).roundToInt().coerceIn(left + 1, imageWidth)
    val bottom = (bottomPct / 100f * imageHeight).roundToInt().coerceIn(top + 1, imageHeight)
    val count = numGraphs.coerceIn(1, 6)
    val fullWidth = (right - left).coerceAtLeast(1)
    val fullHeight = (bottom - top).coerceAtLeast(1)
    val regions = if (count == 1) {
        listOf(GraphRegion(left, top, fullWidth, fullHeight, "VLM hint"))
    } else {
        (0 until count).map { index ->
            val y0 = top + (fullHeight * index / count)
            val y1 = top + (fullHeight * (index + 1) / count)
            GraphRegion(left, y0, fullWidth, (y1 - y0).coerceAtLeast(1), "VLM hint ${index + 1}")
        }
    }
    return GraphRegionResult(
        regions = regions,
        detectionMethod = DetectionMethod.AUTO,
        confidence = DetectionConfidence.LOW,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        warnings = listOf("roi.vlm_hint_not_pixel_truth"),
        timestamp = System.currentTimeMillis(),
    )
}

private fun GraphRegion.marginSafety(imageWidth: Int, imageHeight: Int): Float {
    val horizontal = minOf(x, imageWidth - right).coerceAtLeast(0).toFloat() / imageWidth.coerceAtLeast(1)
    val vertical = minOf(y, imageHeight - bottom).coerceAtLeast(0).toFloat() / imageHeight.coerceAtLeast(1)
    return ((horizontal + vertical) * 8f).coerceIn(0f, 1f)
}

private fun GraphRegion.clampToImage(imageWidth: Int, imageHeight: Int): GraphRegion {
    val clampedX = x.coerceIn(0, (imageWidth - 1).coerceAtLeast(0))
    val clampedY = y.coerceIn(0, (imageHeight - 1).coerceAtLeast(0))
    val clampedRight = right.coerceIn(clampedX + 1, imageWidth.coerceAtLeast(clampedX + 1))
    val clampedBottom = bottom.coerceIn(clampedY + 1, imageHeight.coerceAtLeast(clampedY + 1))
    return copy(
        x = clampedX,
        y = clampedY,
        width = clampedRight - clampedX,
        height = clampedBottom - clampedY,
    )
}

private val OcrTextElement.centerX: Float
    get() = x + width / 2f

private val OcrTextElement.centerY: Float
    get() = y + height / 2f

package com.chromalab.feature.processing.flow

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.axis.AxisDetector
import com.chromalab.feature.processing.axis.AxisEditorScreen
import com.chromalab.feature.processing.axis.AxisLine
import com.chromalab.feature.processing.axis.AxisOrigin
import com.chromalab.feature.processing.calibration.XAxisCalibration
import com.chromalab.feature.processing.calibration.XCalibrationScreen
import com.chromalab.feature.processing.calibration.YAxisCalibration
import com.chromalab.feature.processing.calibration.YCalibrationScreen
import com.chromalab.feature.processing.crop.CropRect
import com.chromalab.feature.processing.crop.CropResult
import com.chromalab.feature.processing.crop.CropReviewScreen
import com.chromalab.feature.processing.crop.ImageCropper
import com.chromalab.feature.processing.curve.CurveEditorScreen
import com.chromalab.feature.processing.curve.CurveExtractionResult
import com.chromalab.feature.processing.curve.CurveExtractor
import com.chromalab.feature.processing.curve.CurveMaskPreparer
import com.chromalab.feature.processing.curve.CurveOverlayScreen
import com.chromalab.feature.processing.curve.CurvePoint
import com.chromalab.feature.processing.curve.scaledCoordinates
import com.chromalab.feature.processing.document.DocumentDetector
import com.chromalab.feature.processing.graph.DetectionConfidence
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.graph.GraphRegionDetector
import com.chromalab.feature.processing.graph.GraphRegionResult
import com.chromalab.feature.processing.graph.GraphRoiEditorScreen
import com.chromalab.feature.processing.graph.GraphSelectionScreen
import com.chromalab.feature.processing.inference.ChartAnalysisReader
import com.chromalab.feature.processing.inference.ActiveInferenceModel
import com.chromalab.feature.processing.inference.ModelRuntime
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrSuggestionScreen
import com.chromalab.feature.processing.perspective.PerspectiveCorrectionResult
import com.chromalab.feature.processing.perspective.PerspectiveReviewScreen
import com.chromalab.feature.processing.perspective.PerspectiveWarper
import com.chromalab.feature.processing.perspective.HomographyMatrix
import com.chromalab.feature.processing.document.DocumentCorners
import com.chromalab.feature.processing.document.ImagePoint
import com.chromalab.feature.processing.pipeline.DetectionMethod
import com.chromalab.feature.processing.preprocess.ImagePreprocessor
import com.chromalab.feature.processing.sweep.AutoSweepEngine
import com.chromalab.feature.processing.quality.ImageQualityAnalyzer
import com.chromalab.feature.processing.quality.ImageQualityReport
import com.chromalab.feature.processing.quality.ImageQualityScreen
import com.chromalab.feature.processing.quality.QualityLevel
import com.chromalab.feature.processing.quality.QualityMetric
import com.chromalab.feature.processing.quality.QualityCalculator
import com.chromalab.feature.processing.quality.DigitizationQualityReport
import com.chromalab.feature.processing.quality.QualityStatus
import com.chromalab.feature.processing.quality.QualityReportContent
import com.chromalab.feature.processing.quality.StageQuality
import com.chromalab.feature.processing.export.ExportScreen
import com.chromalab.feature.processing.export.ExportBundle
import com.chromalab.feature.processing.storage.SessionWriter
import com.chromalab.feature.processing.storage.ProcessingParams
import com.chromalab.feature.processing.signal.SmoothingParams
import com.chromalab.feature.processing.signal.SignalPreviewScreen
import com.chromalab.feature.processing.signal.SmoothedSignal
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.signal.SignalConverter
import com.chromalab.feature.processing.signal.SignalSmoother
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.processing.normalize.ImageNormalizer
import com.chromalab.feature.processing.normalize.NormalizedImageResult
import com.chromalab.feature.processing.preprocess.PreprocessingResult
import com.chromalab.feature.processing.calibration.PixelCalibration
import com.chromalab.feature.processing.report.buildProcessingReportMetadataConfig
import com.chromalab.feature.processing.report.currentReportDeviceName
import com.chromalab.feature.reports.ExecutedRuntime
import com.chromalab.feature.reports.GraphPreparationVariantMetadata
import com.chromalab.feature.reports.ModelExecutionInfo
import com.chromalab.feature.reports.PixelRect
import com.chromalab.feature.reports.ReportSeverity
import com.chromalab.feature.reports.ReportStageTiming
import com.chromalab.feature.reports.ReportWarning
import com.chromalab.core.data.DatabaseProvider
import com.chromalab.core.data.entity.ChromatogramEntity
import com.chromalab.core.data.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Processing flow orchestrator.
 *
 * Manages the step-by-step pipeline:
 * - Routes each step to its real UI screen
 * - Invokes actual platform processors (quality, crop, perspective, etc.)
 * - Shows loading indicator during processing
 * - Passes intermediate results between steps
 */
@Composable
fun ProcessingFlowScreen(
    imagePath: String,
    onFinish: (signalId: Long) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val flowStartedAt = remember(imagePath) { System.currentTimeMillis() }
    val stageDurationMillis = remember(imagePath) { mutableStateMapOf<ProcessingStep, Long>() }
    var currentStep by remember { mutableStateOf(ProcessingStep.FIRST) }
    var isProcessing by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }

    // Elapsed time counter — ticks every second while processing
    LaunchedEffect(isProcessing, currentStep) {
        if (isProcessing) {
            elapsedSeconds = 0
            while (true) {
                delay(1000L)
                elapsedSeconds++
            }
        }
    }

    // Keep screen on during processing
    KeepScreenOn(isProcessing)

    // Working directory for intermediate files
    val outputDir = remember {
        val sep = if (imagePath.contains('\\')) '\\' else '/'
        imagePath.substringBeforeLast(sep)
    }

    // Platform processors â€” instantiated once
    val qualityAnalyzer = remember { ImageQualityAnalyzer() }
    val imageCropper = remember { ImageCropper() }
    val imageNormalizer = remember { ImageNormalizer() }
    val documentDetector = remember { DocumentDetector() }
    val perspectiveWarper = remember { PerspectiveWarper() }
    val preprocessor = remember { ImagePreprocessor() }
    val graphDetector = remember { GraphRegionDetector() }
    val axisDetector = remember { AxisDetector() }
    val chartReader = remember { ChartAnalysisReader() }
    val curveMaskPreparer = remember { CurveMaskPreparer() }
    val curveExtractor = remember { CurveExtractor() }

    // --- Intermediate results populated by real processors ---
    var normalizedResult by remember { mutableStateOf<NormalizedImageResult?>(null) }
    var normalizedPath by remember { mutableStateOf(imagePath) }
    var imageWidth by remember { mutableStateOf(0) }
    var imageHeight by remember { mutableStateOf(0) }
    var qualityReport by remember { mutableStateOf<ImageQualityReport?>(null) }
    var documentBounds by remember { mutableStateOf<com.chromalab.feature.processing.document.DocumentBounds?>(null) }
    var cropResult by remember { mutableStateOf<CropResult?>(null) }
    var currentImagePath by remember { mutableStateOf(imagePath) }
    var perspectiveResult by remember { mutableStateOf<PerspectiveCorrectionResult?>(null) }
    var graphResult by remember { mutableStateOf<GraphRegionResult?>(null) }
    var preprocessingResult by remember { mutableStateOf<PreprocessingResult?>(null) }
    var selectedRegion by remember { mutableStateOf(GraphRegion(0, 0, 1, 1)) }
    var axesResult by remember { mutableStateOf<AxesResult?>(null) }
    var xCalibration by remember { mutableStateOf<XAxisCalibration?>(null) }
    var yCalibration by remember { mutableStateOf<YAxisCalibration?>(null) }
    var ocrResult by remember { mutableStateOf<AxisOcrResult?>(null) }
    var pixelCalibration by remember { mutableStateOf<PixelCalibration?>(null) }
    var curveExtractionResult by remember { mutableStateOf<CurveExtractionResult?>(null) }
    var curvePoints by remember { mutableStateOf<List<CurvePoint>>(emptyList()) }
    var smoothedSignal by remember { mutableStateOf<SmoothedSignal?>(null) }
    var digitizationReport by remember { mutableStateOf<DigitizationQualityReport?>(null) }

    // Auto-Sweep state
    val sweepEngine = remember { AutoSweepEngine() }
    var sweepProgress by remember { mutableStateOf<AutoSweepEngine.SweepProgress?>(null) }
    var sweepCompleted by remember { mutableStateOf(false) }
    var bestSweepConfig by remember { mutableStateOf<String?>(null) }
    var sweepPreparationVariants by remember { mutableStateOf<List<GraphPreparationVariantMetadata>>(emptyList()) }

    // Multi-graph support
    var currentGraphIndex by remember { mutableIntStateOf(0) }
    val processedGraphs = remember { mutableStateListOf<ProcessedGraphSnapshot>() }

    // VLM model loading status (25.2B: lazy loading)
    var vlmLoadingStatus by remember { mutableStateOf<String?>(null) }
    var vlmReady by remember { mutableStateOf(false) }

    // --- Run real processing on step entry ---
    var processingError by remember { mutableStateOf<String?>(null) }
    var savedSignalId by remember { mutableStateOf<Long?>(null) }

    val captureCurrentGraphSnapshot = {
        smoothedSignal?.let { signal ->
            val graphNumber = currentGraphIndex + 1
            processedGraphs.removeAll { it.graphIndex == graphNumber }
            processedGraphs.add(
                ProcessedGraphSnapshot(
                    graphIndex = graphNumber,
                    signal = signal,
                    selectedRegion = selectedRegion,
                    ocrResult = ocrResult,
                    preprocessingResult = preprocessingResult,
                    bestSweepConfig = bestSweepConfig,
                    warnings = buildReportGraphWarnings(
                        graphIndex = graphNumber,
                        digitizationReport = digitizationReport,
                        graphResult = graphResult,
                        ocrResult = ocrResult,
                    ),
                    preparationVariants = sweepPreparationVariants,
                ),
            )
        }
    }

    // When signal is saved, navigate to analysis
    LaunchedEffect(savedSignalId) {
        savedSignalId?.let { onFinish(it) }
    }

    LaunchedEffect(currentStep) {
        isProcessing = true
        processingError = null  // clear previous error on step entry
        val timedStep = currentStep
        val stepStartedAt = System.currentTimeMillis()
        try {
            withContext(Dispatchers.Default) {
                when (currentStep) {
                    ProcessingStep.IMAGE_QUALITY -> {
                        // 25.2B: Lazy VLM model loading at pipeline start
                        if (!vlmReady) {
                            vlmLoadingStatus = "Загрузка AI модели..."
                            vlmReady = chartReader.ensureModelLoaded { status ->
                                vlmLoadingStatus = status
                            }
                            vlmLoadingStatus = if (vlmReady) "AI модель готова" else null
                            println("PIPELINE[VLM] Model ready: $vlmReady")
                            if (!vlmReady) {
                                error("AI vision model is required for photo chromatogram analysis. Download or activate a chromatography VLM first.")
                            }
                        }

                        // Normalize first: fix EXIF orientation
                        if (normalizedResult == null) {
                            val norm = imageNormalizer.normalize(imagePath, outputDir)
                                ?: error("Image normalization failed: unable to load source image.")
                            if (norm.width <= 0 || norm.height <= 0) {
                                error("Image normalization failed: invalid source dimensions ${norm.width}x${norm.height}.")
                            }
                            normalizedResult = norm
                            normalizedPath = norm.normalizedPath
                            currentImagePath = norm.normalizedPath
                            imageWidth = norm.width
                            imageHeight = norm.height
                            selectedRegion = GraphRegion(
                                0, 0, norm.width, norm.height,
                            )
                        }
                        qualityReport = qualityAnalyzer.analyze(currentImagePath)
                        println("PIPELINE[IMAGE_QUALITY] done: blur=${qualityReport?.blurScore?.score}")
                    }

                    ProcessingStep.CROP_REVIEW -> {
                        if (cropResult == null) {
                            currentImagePath = normalizedPath
                            // Auto-pipeline: skip document detection
                            // ML Kit images are pre-cropped, gallery images are user-selected
                            cropResult = fallbackCropResult(currentImagePath, imageWidth, imageHeight)
                            println("PIPELINE[CROP] skip: using image as-is, w=$imageWidth h=$imageHeight")
                        }
                    }

                    ProcessingStep.PERSPECTIVE -> {
                        if (perspectiveResult == null) {
                            // Auto-pipeline: skip perspective correction
                            perspectiveResult = fallbackPerspectiveResult(
                                currentImagePath, imageWidth, imageHeight,
                            )
                            println("PIPELINE[PERSPECTIVE] skip")
                        }
                    }

                    ProcessingStep.GRAPH_SELECTION, ProcessingStep.GRAPH_ROI -> {
                        // === AUTO-SWEEP: try all preprocessing configs, pick best ===
                        if (!sweepCompleted) {
                            val isSubsequentGraph = currentGraphIndex > 0
                            val totalRegions = graphResult?.filteredRegions?.size ?: 0
                            println("PIPELINE[SWEEP] Starting sweep for graph ${currentGraphIndex + 1}/${maxOf(totalRegions, 1)}, configs=${sweepEngine.configs.size}")
                            val w = imageWidth.takeIf { it > 0 }
                                ?: error("Normalized image width is required before graph detection.")
                            val h = imageHeight.takeIf { it > 0 }
                                ?: error("Normalized image height is required before graph detection.")
                            val graphOutputDir = graphProcessingOutputDir(outputDir, currentGraphIndex)

                            val sweepResults = sweepEngine.sweep(
                                imagePath = currentImagePath,
                                outputDir = graphOutputDir,
                                imageWidth = w,
                                imageHeight = h,
                                // Multi-graph: reuse graph detection, target specific region
                                cachedGraphResult = if (isSubsequentGraph) graphResult else null,
                                overrideRegion = if (isSubsequentGraph) selectedRegion else null,
                                requireVlmForAnalysis = true,
                                onProgress = { progress ->
                                    sweepProgress = progress
                                },
                            )

                            if (sweepResults.isNotEmpty()) {
                                val best = sweepResults.first()
                                bestSweepConfig = best.config.name
                                sweepPreparationVariants = sweepResults.toGraphPreparationVariants()
                                println("PIPELINE[SWEEP] Winner: '${best.config.name}' score=${best.score} (${best.scoreBreakdown})")

                                // Apply sweep results to pipeline state
                                preprocessingResult = best.preprocessingResult
                                // Only update graphResult on first graph
                                if (!isSubsequentGraph) {
                                    graphResult = best.graphResult
                                    best.selectedRegion?.let { selectedRegion = it }
                                }
                                ocrResult = best.ocrResult
                                axesResult = best.axesResult
                                curveExtractionResult = best.curveResult
                                curvePoints = best.curveResult?.points ?: emptyList()

                                val allRegions = graphResult?.filteredRegions ?: emptyList()
                                if (!isSubsequentGraph) {
                                    println("PIPELINE[GRAPH] regions=${allRegions.size}, confidence=${graphResult?.confidence}")
                                    allRegions.forEachIndexed { idx, r ->
                                        println("PIPELINE[GRAPH]   [$idx] (${r.x},${r.y}) ${r.width}x${r.height} area=${r.area} label='${r.label}'")
                                    }
                                }
                                println("PIPELINE[GRAPH] selected=$selectedRegion")
                                println("PIPELINE[OCR] x=${ocrResult?.suggestedXValues}, y=${ocrResult?.suggestedYValues}")
                                println("PIPELINE[CURVE] points=${curvePoints.size}")
                            } else {
                                error("AI vision analysis did not produce a usable graph and axis result.")
                            }
                            sweepCompleted = true
                        }
                    }

                    ProcessingStep.AXIS_DETECTION -> {
                        // Axis detection already done by sweep
                        if (axesResult == null) {
                            axesResult = axisDetector.detect(currentImagePath, selectedRegion)
                            println("PIPELINE[AXES] fallback: origin=${axesResult?.origin}")
                        } else {
                            println("PIPELINE[AXES] using sweep result")
                        }
                    }

                    ProcessingStep.OCR_SUGGESTION -> {
                        // OCR already done by sweep
                        if (ocrResult != null) {
                            println("PIPELINE[OCR] using sweep result: x=${ocrResult?.suggestedXValues?.size} values, y=${ocrResult?.suggestedYValues?.size} values")
                        } else {
                            ocrResult = chartReader.readAxisLabels(currentImagePath, selectedRegion)
                            println("PIPELINE[OCR] fallback: x=${ocrResult?.suggestedXValues}, y=${ocrResult?.suggestedYValues}")
                        }
                    }

                    ProcessingStep.X_CALIBRATION -> {
                        // Auto-calibrate X axis from OCR results
                        if (xCalibration == null) {
                            val ocr = ocrResult
                            val axes = axesResult
                            val xValues = ocr?.suggestedXValues ?: emptyList()
                            val origin = axes?.origin
                            val regionWidth = selectedRegion.width.toFloat()

                            if (xValues.size >= 2) {
                                val sorted = xValues.sorted()
                                val minVal = sorted.first()
                                val maxVal = sorted.last()

                                // Find OCR elements with matching values to get pixel positions
                                val rawElems = ocr?.rawElements ?: emptyList()
                                val xElems = rawElems
                                    .filter { it.numericValue != null && it.numericValue in sorted }
                                    .sortedBy { it.numericValue }

                                // Convert from full-image coords to graphRegion-relative coords
                                val regionX = selectedRegion.x.toFloat()

                                // Build calibration points from OCR elements
                                // Filter: only use elements that map to valid pixel positions (inside the region)
                                val validPoints = xElems.mapNotNull { elem ->
                                    val px = (elem.x + elem.width / 2f) - regionX
                                    val value = elem.numericValue ?: return@mapNotNull null
                                    // Reject if pixel position is outside the region (±10% tolerance)
                                    if (px < -regionWidth * 0.1f || px > regionWidth * 1.1f) {
                                        println("PIPELINE[X_CAL] rejected: value=$value px=$px (outside region 0..${regionWidth.toInt()})")
                                        return@mapNotNull null
                                    }
                                    px to value
                                }.distinctBy { it.second } // one point per unique value
                                    .sortedBy { it.second }

                                val hasDistinctPixels = validPoints
                                    .distinctBy { it.first.roundToInt() }
                                    .size >= 2

                                if (validPoints.size >= 2 && hasDistinctPixels) {
                                    val first = validPoints.first()
                                    val last = validPoints.last()
                                    println("PIPELINE[X_CAL] OCR-based: px1=${first.first}→${first.second}, px2=${last.first}→${last.second} (regionX=$regionX, ${validPoints.size} points)")

                                    xCalibration = XAxisCalibration(
                                        calibration = com.chromalab.feature.processing.calibration.LinearCalibration(
                                            point1 = com.chromalab.feature.processing.calibration.CalibrationPoint(first.first, first.second),
                                            point2 = com.chromalab.feature.processing.calibration.CalibrationPoint(last.first, last.second),
                                        ),
                                        unit = ocr?.xUnit ?: "мин",
                                        timestamp = System.currentTimeMillis(),
                                    )
                                } else {
                                    // Not enough valid OCR points → pixel-based fallback
                                    println("PIPELINE[X_CAL] fallback: not enough valid OCR points (${validPoints.size})")
                                    xCalibration = XAxisCalibration(
                                        calibration = com.chromalab.feature.processing.calibration.LinearCalibration(
                                            point1 = com.chromalab.feature.processing.calibration.CalibrationPoint(0f, minVal),
                                            point2 = com.chromalab.feature.processing.calibration.CalibrationPoint(regionWidth, maxVal),
                                        ),
                                        unit = ocr?.xUnit ?: "мин",
                                        timestamp = System.currentTimeMillis(),
                                    )
                                }
                            } else {
                                // No OCR → identity calibration (pixels)
                                xCalibration = XAxisCalibration(
                                    calibration = com.chromalab.feature.processing.calibration.LinearCalibration(
                                        point1 = com.chromalab.feature.processing.calibration.CalibrationPoint(0f, 0f),
                                        point2 = com.chromalab.feature.processing.calibration.CalibrationPoint(regionWidth, regionWidth),
                                    ),
                                    unit = "px",
                                    timestamp = System.currentTimeMillis(),
                                )
                            }
                        }
                    }

                    ProcessingStep.Y_CALIBRATION -> {
                        // Auto-calibrate Y axis from OCR results
                        if (yCalibration == null) {
                            val ocr = ocrResult
                            val axes = axesResult
                            val yValues = ocr?.suggestedYValues ?: emptyList()
                            val origin = axes?.origin

                            if (yValues.size >= 2) {
                                val sorted = yValues.sorted()
                                val maxVal = sorted.last()

                                // Find OCR element for the highest Y value to get its pixel position
                                val rawElems = ocr?.rawElements ?: emptyList()
                                val topElem = rawElems
                                    .filter { it.numericValue == maxVal && (it.width > 0f || it.height > 0f) }
                                    .minByOrNull { it.y } // highest on screen = smallest y

                                // Convert to graphRegion-relative coords
                                val regionY = selectedRegion.y.toFloat()

                                // Top of Y scale: position of highest OCR label
                                val py1 = if (topElem != null) {
                                    (topElem.y + topElem.height / 2f) - regionY
                                } else {
                                    0f
                                }

                                // Bottom of Y scale: origin = zero line
                                // origin.y is in full-image coords, convert to region-relative
                                val py2 = if (origin != null) {
                                    origin.y - regionY
                                } else {
                                    selectedRegion.height.toFloat()
                                }

                                // Y axis: py1 (top, small pixel) → maxVal, py2 (bottom, origin) → 0
                                println("PIPELINE[Y_CAL] OCR-based: py1=$py1→$maxVal, py2=$py2→0 (regionY=$regionY)")

                                val candidateCalibration = YAxisCalibration(
                                    calibration = com.chromalab.feature.processing.calibration.LinearCalibration(
                                        point1 = com.chromalab.feature.processing.calibration.CalibrationPoint(py1, maxVal),
                                        point2 = com.chromalab.feature.processing.calibration.CalibrationPoint(py2, 0f),
                                    ),
                                    unit = ocr?.yUnit ?: "mAU",
                                    timestamp = System.currentTimeMillis(),
                                )
                                yCalibration = if (candidateCalibration.calibration.isValid) {
                                    candidateCalibration
                                } else {
                                    val fh = selectedRegion.height.toFloat()
                                    println("PIPELINE[Y_CAL] fallback: invalid OCR pixels py1=$py1 py2=$py2")
                                    YAxisCalibration(
                                        calibration = com.chromalab.feature.processing.calibration.LinearCalibration(
                                            point1 = com.chromalab.feature.processing.calibration.CalibrationPoint(0f, maxVal),
                                            point2 = com.chromalab.feature.processing.calibration.CalibrationPoint(fh, 0f),
                                        ),
                                        unit = ocr?.yUnit ?: "mAU",
                                        timestamp = System.currentTimeMillis(),
                                    )
                                }
                            } else {
                                // No OCR → identity (pixels, inverted)
                                val fh = selectedRegion.height.toFloat()
                                yCalibration = YAxisCalibration(
                                    calibration = com.chromalab.feature.processing.calibration.LinearCalibration(
                                        point1 = com.chromalab.feature.processing.calibration.CalibrationPoint(0f, fh),
                                        point2 = com.chromalab.feature.processing.calibration.CalibrationPoint(fh, 0f),
                                    ),
                                    unit = "px",
                                    timestamp = System.currentTimeMillis(),
                                )
                            }

                            // Build unified PixelCalibration
                            val xCal = xCalibration
                            val yCal = yCalibration
                            if (xCal != null && yCal != null && xCal.calibration.isValid && yCal.calibration.isValid) {
                                pixelCalibration = PixelCalibration.from(
                                    xAxis = xCal,
                                    yAxis = yCal,
                                    // Convert origin to region-relative coords
                                    originPixelX = (origin?.x ?: selectedRegion.x.toFloat()) - selectedRegion.x.toFloat(),
                                    originPixelY = (origin?.y ?: (selectedRegion.y + selectedRegion.height).toFloat()) - selectedRegion.y.toFloat(),
                                )
                            } else {
                                println("PIPELINE[CAL] fallback: invalid axis calibration")
                                pixelCalibration = fallbackCalibration(selectedRegion.width, selectedRegion.height)
                            }
                        }
                    }

                    ProcessingStep.CURVE_EXTRACTION -> {
                        if (curveExtractionResult == null) {
                            // Use fallback axes if axis detection failed
                            if (axesResult == null) {
                                axesResult = fallbackAxesResult()
                                println("PIPELINE[CURVE] using fallback axes")
                            }
                            // Use contrast-enhanced image from preprocessing if available.
                            // CLAHE improves edge detection for Canny in CurveMaskPreparer.
                            val inputForMask = preprocessingResult?.contrastEnhancedPath ?: currentImagePath
                            val graphOutputDir = graphProcessingOutputDir(outputDir, currentGraphIndex)
                            println("PIPELINE[CURVE] input=$inputForMask, region=$selectedRegion")
                            val mask = curveMaskPreparer.prepare(
                                inputForMask, selectedRegion,
                                axesResult!!, graphOutputDir,
                            )
                            println("PIPELINE[CURVE] mask: raw=${mask.rawMaskPath}, clean=${mask.cleanMaskPath}")
                            val maskPath = mask.cleanMaskPath ?: mask.rawMaskPath ?: inputForMask
                            curveExtractionResult = curveExtractor.extract(
                                maskPath, selectedRegion.width,
                                selectedRegion.height, graphOutputDir,
                            ).scaledCoordinates(mask.coordinateScale)
                            curvePoints = curveExtractionResult?.points ?: emptyList()
                            println("PIPELINE[CURVE] points=${curvePoints.size}, interpolated=${curveExtractionResult?.interpolatedColumns}")
                        } else {
                            println("PIPELINE[CURVE] using sweep result: ${curvePoints.size} points")
                        }
                    }

                    ProcessingStep.SIGNAL_PREVIEW -> {
                        if (smoothedSignal == null) {
                            println("PIPELINE[SIGNAL] curvePoints=${curvePoints.size}, pixelCal=${pixelCalibration != null}")
                            if (curvePoints.isNotEmpty()) {
                                val cal = pixelCalibration ?: fallbackCalibration(
                                    selectedRegion.width, selectedRegion.height,
                                )
                                val signal = SignalConverter.convert(
                                    curvePoints, cal, currentImagePath,
                                )
                                smoothedSignal = SignalSmoother.smooth(signal)
                                println("PIPELINE[SIGNAL] done: points=${smoothedSignal?.smoothed?.points?.size}")
                            } else {
                                println("PIPELINE[SIGNAL] FAILED: no curve points!")
                            }
                        }
                    }

                    else -> {
                        // Build quality report when entering QUALITY_REPORT step
                        if (currentStep == ProcessingStep.QUALITY_REPORT && digitizationReport == null) {
                            val imageStage = QualityCalculator.imageQuality(
                                sharpness = qualityReport?.blurScore?.score?.times(100f) ?: 50f,
                                contrast = qualityReport?.contrastScore?.score?.times(100f) ?: 50f,
                            )
                            val docStage = QualityCalculator.documentDetection(
                                detected = documentBounds != null,
                                perspectiveAngle = perspectiveResult?.maxWarpDistance ?: 0f,
                                hasGlare = false,
                                unevenLighting = false,
                            )
                            val graphStage = QualityCalculator.graphDetection(
                                autoDetected = graphResult?.confidence?.name != "MANUAL",
                                graphCount = graphResult?.regions?.size ?: 1,
                            )
                            val calStage = QualityCalculator.axisCalibration(
                                xCalibrated = xCalibration != null,
                                yCalibrated = yCalibration != null,
                                ocrUsed = ocrResult != null,
                                ocrCorrected = false,
                                manualAxes = axesResult?.detectionMethod?.name == "MANUAL",
                            )
                            val curveStage = curveExtractionResult?.let {
                                QualityCalculator.curveExtraction(it)
                            } ?: com.chromalab.feature.processing.quality.StageQuality(
                                "curve", 0f, listOf("Кривая не извлечена"),
                            )
                            digitizationReport = QualityCalculator.buildReport(
                                imageStage, docStage, graphStage, calStage, curveStage,
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            processingError = "${currentStep.label}: ${e.message ?: "неизвестная ошибка"}"
        }
        val stepDuration = (System.currentTimeMillis() - stepStartedAt).coerceAtLeast(0L)
        stageDurationMillis[timedStep] = (stageDurationMillis[timedStep] ?: 0L) + stepDuration
        isProcessing = false

        // --- Auto-advance logic ---
        // All steps auto-advance except EXPORT (the final dashboard).
        if (processingError == null) {
            val shouldAutoAdvance = currentStep.autoAdvance == AutoAdvancePolicy.ALWAYS
            if (shouldAutoAdvance) {
                kotlinx.coroutines.delay(150L)
                if (currentStep == ProcessingStep.QUALITY_REPORT) {
                    // Multi-graph: check for more regions before advancing to EXPORT
                    captureCurrentGraphSnapshot()
                    val totalRegions = graphResult?.filteredRegions?.size ?: 1
                    if (currentGraphIndex + 1 < totalRegions) {
                        currentGraphIndex++
                        val nextRegion = graphResult!!.filteredRegions[currentGraphIndex]
                        selectedRegion = nextRegion
                        println("PIPELINE[MULTI] Advancing to graph ${currentGraphIndex + 1}/$totalRegions: ${nextRegion.label}")
                        // Reset per-graph state
                        axesResult = null
                        xCalibration = null
                        yCalibration = null
                        ocrResult = null
                        pixelCalibration = null
                        curveExtractionResult = null
                        curvePoints = emptyList()
                        smoothedSignal = null
                        digitizationReport = null
                        sweepCompleted = false
                        sweepProgress = null
                        bestSweepConfig = null
                        sweepPreparationVariants = emptyList()
                        currentStep = ProcessingStep.GRAPH_ROI
                    } else {
                        // All graphs processed → auto-save to Room + navigate to Analysis
                        println("PIPELINE[AUTO-SAVE] All ${processedGraphs.size} graphs processed, saving to Room...")
                        val graphsToSave = processedGraphs.toReportSaveEntries()
                        val modelSnapshot = chartReader.currentModelSnapshot()
                        val selectedReportModel = modelSnapshot.selectedModel.toReportModelExecutionInfo()
                        val executedReportModel = modelSnapshot.executedModel.toReportModelExecutionInfo()
                        val executedRuntime = executedReportModel?.runtime
                            ?: selectedReportModel?.runtime
                            ?: ExecutedRuntime.UNKNOWN
                        val reportStageTimings = stageDurationMillis.toReportStageTimings()
                        val reportDeviceName = currentReportDeviceName()
                        val saveResult = withContext(Dispatchers.IO) {
                            val now = System.currentTimeMillis()
                            try {
                                if (graphsToSave.isEmpty()) {
                                    println("PIPELINE[AUTO-SAVE] No usable signals to save")
                                    return@withContext null
                                }

                                val db = DatabaseProvider.getDatabase()

                                // Create parent Project + Sample to satisfy FK constraints
                                val projectId = db.projectDao().insert(
                                    com.chromalab.core.data.entity.ProjectEntity(
                                        name = "Фото-анализ",
                                        date = now,
                                        createdAt = now,
                                        updatedAt = now,
                                    )
                                )
                                val sId = db.sampleDao().insert(
                                    com.chromalab.core.data.entity.SampleEntity(
                                        projectId = projectId,
                                        name = "Образец ${java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(now)}",
                                        createdAt = now,
                                        updatedAt = now,
                                    )
                                )
                                println("PIPELINE[AUTO-SAVE] Created project=$projectId, sample=$sId")

                                var firstId: Long? = null
                                val detectedGraphCount = graphResult?.filteredRegions?.size
                                    ?.takeIf { it > 0 }
                                    ?: graphsToSave.size
                                val sourceImageBounds = reportSourceImageBounds(imageWidth, imageHeight)
                                val cropConfidence = graphResult?.confidence.toReportCropConfidence()
                                for ((idx, entry) in graphsToSave.withIndex()) {
                                    val signal = entry.signal.smoothed
                                    val preprocessingSteps = buildReportPreprocessingSteps(
                                        normalizedResult = normalizedResult,
                                        cropResult = cropResult,
                                        perspectiveResult = perspectiveResult,
                                        preprocessingResult = entry.preprocessingResult,
                                        bestSweepConfig = entry.bestSweepConfig,
                                    )
                                    val entity = ChromatogramEntity(
                                        sampleId = sId,
                                        sourceType = SourceType.PHOTO,
                                        filePath = currentImagePath,
                                        timeRangeStart = signal.points.firstOrNull()?.time?.toDouble(),
                                        timeRangeEnd = signal.points.lastOrNull()?.time?.toDouble(),
                                        intensityUnit = signal.intensityUnit,
                                        qualityScore = null,
                                        dataPoints = kotlinx.serialization.json.Json.encodeToString(
                                            kotlinx.serialization.builtins.ListSerializer(
                                                com.chromalab.feature.processing.signal.GraphPoint.serializer(),
                                            ),
                                            signal.points,
                                        ),
                                        algorithmConfig = buildProcessingReportMetadataConfig(
                                            sourcePath = imagePath,
                                            processedPath = currentImagePath,
                                            sourceType = SourceType.PHOTO,
                                            graphIndex = entry.graphIndex,
                                            detectedGraphCount = detectedGraphCount,
                                            signalPointCount = signal.points.size,
                                            analysisStartedAtEpochMillis = flowStartedAt,
                                            analysisCompletedAtEpochMillis = now,
                                            sourceImageBounds = sourceImageBounds,
                                            detectedGraphBounds = entry.selectedRegion.toReportPixelRect(),
                                            cropConfidence = cropConfidence,
                                            preprocessingSteps = preprocessingSteps,
                                            preparationVariants = entry.preparationVariants,
                                            axisOcrResult = entry.ocrResult,
                                            titleOcrConfidence = null,
                                            axisOcrConfidence = entry.ocrResult.toReportAxisOcrConfidence(),
                                            tickOcrConfidence = entry.ocrResult.toReportTickOcrConfidence(),
                                            selectedModel = selectedReportModel,
                                            executedModel = executedReportModel,
                                            executedRuntime = executedRuntime,
                                            deviceName = reportDeviceName,
                                            stageTimings = reportStageTimings,
                                            graphWarnings = entry.warnings.withReportGraphIndex(entry.graphIndex),
                                        ),
                                        createdAt = now,
                                        updatedAt = now,
                                    )
                                    val id = db.chromatogramDao().insert(entity)
                                    if (firstId == null) firstId = id
                                    println("PIPELINE[AUTO-SAVE] graph ${idx + 1}/${graphsToSave.size} saved, id=$id, points=${signal.points.size}")
                                }
                                firstId
                            } catch (e: Exception) {
                                println("PIPELINE[AUTO-SAVE] Error: ${e.message}")
                                e.printStackTrace()
                                null
                            }
                        }
                        // Back on Main — navigate or fallback
                        if (saveResult != null) {
                            println("PIPELINE[AUTO-SAVE] Success! Navigating to Analysis, signalId=$saveResult")
                            savedSignalId = saveResult
                        } else {
                            println("PIPELINE[AUTO-SAVE] Failed, falling back to EXPORT")
                            val next = currentStep.next()
                            if (next != null) currentStep = next
                        }
                    }
                } else {
                    val next = currentStep.next()
                    if (next != null) currentStep = next
                }
            }
        }
    }

    val scope = rememberCoroutineScope()

    val advance = { step: ProcessingStep ->
        val next = step.next()
        // Multi-graph loop: after QUALITY_REPORT, check for more regions
        if (step == ProcessingStep.QUALITY_REPORT) {
            // Save current signal to the batch
            captureCurrentGraphSnapshot()
            val totalRegions = graphResult?.filteredRegions?.size ?: 1
            if (currentGraphIndex + 1 < totalRegions) {
                // More regions — reset per-graph state and loop back
                currentGraphIndex++
                val nextRegion = graphResult!!.filteredRegions[currentGraphIndex]
                selectedRegion = nextRegion
                println("PIPELINE[MULTI] Advancing to graph ${currentGraphIndex + 1}/$totalRegions: ${nextRegion.label} (${nextRegion.width}x${nextRegion.height})")
                // Reset per-graph results
                axesResult = null
                xCalibration = null
                yCalibration = null
                ocrResult = null
                pixelCalibration = null
                curveExtractionResult = null
                curvePoints = emptyList()
                smoothedSignal = null
                digitizationReport = null
                // Reset sweep so it runs for the new region
                sweepCompleted = false
                sweepProgress = null
                bestSweepConfig = null
                sweepPreparationVariants = emptyList()
                currentStep = ProcessingStep.GRAPH_ROI
            } else {
                // All graphs done — auto-save to Room, then navigate to Analysis
                println("PIPELINE[AUTO-SAVE] All ${processedGraphs.size} graphs processed, saving to Room...")
                scope.launch {
                    val graphsToSave = processedGraphs.toReportSaveEntries()
                    val modelSnapshot = chartReader.currentModelSnapshot()
                    val selectedReportModel = modelSnapshot.selectedModel.toReportModelExecutionInfo()
                    val executedReportModel = modelSnapshot.executedModel.toReportModelExecutionInfo()
                    val executedRuntime = executedReportModel?.runtime
                        ?: selectedReportModel?.runtime
                        ?: ExecutedRuntime.UNKNOWN
                    val reportStageTimings = stageDurationMillis.toReportStageTimings()
                    val reportDeviceName = currentReportDeviceName()
                    val resultId = withContext(Dispatchers.IO) {
                        val now = System.currentTimeMillis()
                        try {
                            if (graphsToSave.isEmpty()) {
                                println("PIPELINE[AUTO-SAVE] No usable signals to save")
                                return@withContext null
                            }

                            val db = DatabaseProvider.getDatabase()

                            // Create parent Project + Sample to satisfy FK constraints
                            val projectId = db.projectDao().insert(
                                com.chromalab.core.data.entity.ProjectEntity(
                                    name = "Фото-анализ",
                                    date = now,
                                    createdAt = now,
                                    updatedAt = now,
                                )
                            )
                            val sampleId = db.sampleDao().insert(
                                com.chromalab.core.data.entity.SampleEntity(
                                    projectId = projectId,
                                    name = "Образец ${java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(now)}",
                                    createdAt = now,
                                    updatedAt = now,
                                )
                            )
                            println("PIPELINE[AUTO-SAVE] Created project=$projectId, sample=$sampleId")

                            var firstId: Long? = null
                            val detectedGraphCount = graphResult?.filteredRegions?.size
                                ?.takeIf { it > 0 }
                                ?: graphsToSave.size
                            val sourceImageBounds = reportSourceImageBounds(imageWidth, imageHeight)
                            val cropConfidence = graphResult?.confidence.toReportCropConfidence()
                            for ((idx, entry) in graphsToSave.withIndex()) {
                                val signal = entry.signal.smoothed
                                val preprocessingSteps = buildReportPreprocessingSteps(
                                    normalizedResult = normalizedResult,
                                    cropResult = cropResult,
                                    perspectiveResult = perspectiveResult,
                                    preprocessingResult = entry.preprocessingResult,
                                    bestSweepConfig = entry.bestSweepConfig,
                                )
                                val entity = ChromatogramEntity(
                                    sampleId = sampleId,
                                    sourceType = SourceType.PHOTO,
                                    filePath = currentImagePath,
                                    timeRangeStart = signal.points.firstOrNull()?.time?.toDouble(),
                                    timeRangeEnd = signal.points.lastOrNull()?.time?.toDouble(),
                                    intensityUnit = signal.intensityUnit,
                                    qualityScore = null,
                                    dataPoints = kotlinx.serialization.json.Json.encodeToString(
                                        kotlinx.serialization.builtins.ListSerializer(
                                            com.chromalab.feature.processing.signal.GraphPoint.serializer(),
                                        ),
                                        signal.points,
                                    ),
                                    algorithmConfig = buildProcessingReportMetadataConfig(
                                        sourcePath = imagePath,
                                        processedPath = currentImagePath,
                                        sourceType = SourceType.PHOTO,
                                        graphIndex = entry.graphIndex,
                                        detectedGraphCount = detectedGraphCount,
                                        signalPointCount = signal.points.size,
                                        analysisStartedAtEpochMillis = flowStartedAt,
                                        analysisCompletedAtEpochMillis = now,
                                        sourceImageBounds = sourceImageBounds,
                                        detectedGraphBounds = entry.selectedRegion.toReportPixelRect(),
                                        cropConfidence = cropConfidence,
                                        preprocessingSteps = preprocessingSteps,
                                        preparationVariants = entry.preparationVariants,
                                        axisOcrResult = entry.ocrResult,
                                        titleOcrConfidence = null,
                                        axisOcrConfidence = entry.ocrResult.toReportAxisOcrConfidence(),
                                        tickOcrConfidence = entry.ocrResult.toReportTickOcrConfidence(),
                                        selectedModel = selectedReportModel,
                                        executedModel = executedReportModel,
                                        executedRuntime = executedRuntime,
                                        deviceName = reportDeviceName,
                                        stageTimings = reportStageTimings,
                                        graphWarnings = entry.warnings.withReportGraphIndex(entry.graphIndex),
                                    ),
                                    createdAt = now,
                                    updatedAt = now,
                                )
                                val id = db.chromatogramDao().insert(entity)
                                if (firstId == null) firstId = id
                                println("PIPELINE[AUTO-SAVE] graph ${idx + 1}/${graphsToSave.size} saved, id=$id, points=${signal.points.size}")
                            }
                            firstId
                        } catch (e: Exception) {
                            e.printStackTrace()
                            println("PIPELINE[AUTO-SAVE] Error: ${e.message}")
                            null
                        }
                    }

                    // Back on Main thread — update Compose state
                    if (resultId != null) {
                        println("PIPELINE[AUTO-SAVE] Success! Navigating to Analysis, signalId=$resultId")
                        savedSignalId = resultId
                    } else {
                        println("PIPELINE[AUTO-SAVE] Failed, falling back to EXPORT")
                        currentStep = ProcessingStep.EXPORT
                    }
                }
            }
        } else if (next != null) {
            currentStep = next
        }
    }
    val goBack = { step: ProcessingStep ->
        val prev = step.prev()
        if (prev != null) currentStep = prev else onCancel()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Step content
        Box(modifier = Modifier.weight(1f)) {
            // All steps auto-advance — show processing overlay
            if (currentStep != ProcessingStep.EXPORT) {
                // Full-screen progress overlay
                AutoProgressOverlay(
                    currentStep = currentStep,
                    isProcessing = true,
                    sweepProgress = sweepProgress,
                    bestSweepConfig = bestSweepConfig,
                    currentGraphIndex = currentGraphIndex,
                    totalGraphs = graphResult?.filteredRegions?.size ?: 1,
                    vlmLoadingStatus = vlmLoadingStatus,
                    elapsedSeconds = elapsedSeconds,
                )

                // Error overlay on top
                val error = processingError
                if (error != null) {
                    val blocksSkip = blocksFullAnalysisSkip(error)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.lg),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                            ),
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.lg),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                Text(
                                    "Ошибка: ${currentStep.label}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                Text(
                                    error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                                if (blocksSkip) {
                                    Text(
                                        "Full photo analysis requires a working vision model. This stage cannot be skipped.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                ) {
                                    OutlinedButton(onClick = { onCancel() }) {
                                        Text("Отмена")
                                    }
                                    Button(onClick = {
                                        processingError = null
                                        val s = currentStep
                                        currentStep = ProcessingStep.FIRST
                                        currentStep = s
                                    }) {
                                        Text("Повторить")
                                    }
                                    if (!blocksSkip) {
                                        TextButton(onClick = {
                                            processingError = null
                                            advance(currentStep)
                                        }) {
                                            Text("Пропустить")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // EXPORT: error fallback only (25.2A)
                // Shown only when auto-save to Room failed.
                // Normal flow: QUALITY_REPORT → auto-save → onFinish → AnalysisFlowScreen
                val allSignals = remember(processedGraphs.size) {
                    processedGraphs.sortedBy { it.graphIndex }.map { it.signal.smoothed }
                }
                val displaySignal = allSignals.lastOrNull() ?: smoothedSignal?.smoothed
                if (displaySignal != null) {
                    val cal = pixelCalibration ?: fallbackCalibration(
                        imageWidth, imageHeight,
                    )
                    val bundle = ExportBundle(
                        signal = displaySignal,
                        calibration = cal,
                        processingParams = ProcessingParams(
                            calibration = cal,
                            smoothingParams = smoothedSignal?.params ?: SmoothingParams(),
                            timestamp = System.currentTimeMillis(),
                        ),
                        timestamp = System.currentTimeMillis(),
                    )
                    val writer = remember { SessionWriter(outputDir) }
                    ExportScreen(
                        signal = displaySignal,
                        bundle = bundle,
                        sessionWriter = writer,
                        onBack = { onCancel() },
                        allSignals = allSignals,
                    )
                } else {
                    // Pipeline produced no signal — show error
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.md),
                        ) {
                            Text(
                                "Не удалось извлечь сигнал",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                "Попробуйте сделать более чёткое фото",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(onClick = { onCancel() }) {
                                Text("Вернуться")
                            }
                        }
                    }
                }
            }
        }
    }
}



// --- Fallback data when processing fails or isn't applicable --------

private fun reportSourceImageBounds(width: Int, height: Int): PixelRect? =
    if (width > 0 && height > 0) {
        PixelRect(x = 0, y = 0, width = width, height = height)
    } else {
        null
    }

private fun graphProcessingOutputDir(outputDir: String, graphIndex: Int): String =
    "$outputDir/graph_$graphIndex"

private fun GraphRegion.toReportPixelRect(): PixelRect =
    PixelRect(x = x, y = y, width = width, height = height)

private fun DetectionConfidence?.toReportCropConfidence(): Double? =
    when (this) {
        DetectionConfidence.MANUAL -> 1.0
        DetectionConfidence.HIGH -> 0.9
        DetectionConfidence.MEDIUM -> 0.65
        DetectionConfidence.LOW -> 0.35
        null -> null
    }

private fun buildReportPreprocessingSteps(
    normalizedResult: NormalizedImageResult?,
    cropResult: CropResult?,
    perspectiveResult: PerspectiveCorrectionResult?,
    preprocessingResult: PreprocessingResult?,
    bestSweepConfig: String?,
): List<String> = buildList {
    normalizedResult?.let {
        add("Image orientation normalized: rotated=${it.wasRotated}, exif=${it.exifOrientation}")
    }
    cropResult?.let {
        add("Crop stage completed: ${it.cropRect.width}x${it.cropRect.height}")
    }
    perspectiveResult?.let {
        add("Perspective stage completed: ${it.outputWidth}x${it.outputHeight}, excessiveWarp=${it.isExcessiveWarp}")
    }
    preprocessingResult?.let {
        add(
            "Preprocessing: CLAHE=${it.params.claheClipLimit}, adaptiveBlock=${it.params.adaptiveBlockSize}, " +
                "morph=${it.params.morphKernelSize}x${it.params.morphIterations}",
        )
    }
    bestSweepConfig?.takeIf { it.isNotBlank() }?.let {
        add("Auto-sweep selected config: $it")
    }
}

private fun AxisOcrResult?.toReportAxisOcrConfidence(): Double? {
    val result = this ?: return null
    result.confidence.toReportConfidence()?.let { return it }
    if (!result.hasXSuggestions && !result.hasYSuggestions) return null

    val elementConfidences = result.rawElements.mapNotNull { it.confidence.toReportConfidence() }
    return elementConfidences.takeIf { it.isNotEmpty() }?.average()
}

private fun AxisOcrResult?.toReportTickOcrConfidence(): Double? {
    val result = this ?: return null
    val numericConfidences = result.rawElements
        .filter { it.numericValue != null }
        .mapNotNull { it.confidence.toReportConfidence() }
    if (numericConfidences.isNotEmpty()) {
        return numericConfidences.average()
    }

    val hasTickSuggestions = result.suggestedXValues.isNotEmpty() || result.suggestedYValues.isNotEmpty()
    return if (hasTickSuggestions) result.confidence.toReportConfidence() else null
}

private fun Float?.toReportConfidence(): Double? {
    val value = this ?: return null
    if (value.isNaN() || value.isInfinite() || value < 0f) return null
    val normalized = if (value > 1f) value / 100f else value
    return normalized.toDouble().coerceIn(0.0, 1.0)
}

private fun ActiveInferenceModel?.toReportModelExecutionInfo(): ModelExecutionInfo? {
    val model = this ?: return null
    return ModelExecutionInfo(
        modelId = model.modelId,
        modelName = model.modelName,
        runtime = model.runtime.toReportExecutedRuntime(),
        backendLabel = model.backendLabel,
    )
}

private fun ModelRuntime?.toReportExecutedRuntime(): ExecutedRuntime =
    when (this) {
        ModelRuntime.LITERT_LM -> ExecutedRuntime.LITERT
        ModelRuntime.LLAMA_CPP -> ExecutedRuntime.GGUF
        null -> ExecutedRuntime.UNKNOWN
    }

private fun Map<ProcessingStep, Long>.toReportStageTimings(): List<ReportStageTiming> =
    ProcessingStep.entries.mapNotNull { step ->
        val duration = this[step]?.takeIf { it >= 0L } ?: return@mapNotNull null
        ReportStageTiming(
            stageId = step.name,
            stageName = step.name,
            durationMillis = duration,
        )
    }

private data class ProcessedGraphSnapshot(
    val graphIndex: Int,
    val signal: SmoothedSignal,
    val selectedRegion: GraphRegion,
    val ocrResult: AxisOcrResult?,
    val preprocessingResult: PreprocessingResult?,
    val bestSweepConfig: String?,
    val warnings: List<ReportWarning>,
    val preparationVariants: List<GraphPreparationVariantMetadata>,
)

private fun List<ProcessedGraphSnapshot>.toReportSaveEntries(): List<ProcessedGraphSnapshot> =
    sortedBy { it.graphIndex }.filter { it.signal.smoothed.points.size >= 10 }

private fun List<AutoSweepEngine.SweepResult>.toGraphPreparationVariants(): List<GraphPreparationVariantMetadata> =
    mapIndexed { index, result ->
        GraphPreparationVariantMetadata(
            rank = index + 1,
            configName = result.config.name,
            inputVariant = result.config.inputVariant.name.lowercase(),
            score = result.score.toDouble(),
            selected = index == 0,
            scoreBreakdown = result.scoreBreakdown.takeIf { it.isNotBlank() },
        )
    }

private fun buildReportGraphWarnings(
    graphIndex: Int,
    digitizationReport: DigitizationQualityReport?,
    graphResult: GraphRegionResult?,
    ocrResult: AxisOcrResult?,
): List<ReportWarning> = buildList {
    digitizationReport?.let { report ->
        addAll(report.imageQuality.toReportWarnings("image_quality", graphIndex))
        addAll(report.documentDetection.toReportWarnings("document_detection", graphIndex))
        addAll(report.graphDetection.toReportWarnings("graph_detection", graphIndex))
        addAll(report.axisCalibration.toReportWarnings("axis_calibration", graphIndex))
        addAll(report.curveExtraction.toReportWarnings("curve_extraction", graphIndex))
    }

    graphResult?.warnings.orEmpty().forEachIndexed { index, message ->
        add(
            ReportWarning(
                code = "graph.detector.warning_${index + 1}",
                message = message,
                severity = ReportSeverity.WARNING,
                stage = "graph_detection",
                graphIndex = graphIndex,
            ),
        )
    }

    when (graphResult?.confidence) {
        DetectionConfidence.LOW -> add(
            ReportWarning(
                code = "graph.crop_confidence_low",
                message = "Graph crop confidence is low.",
                severity = ReportSeverity.SERIOUS,
                stage = "graph_detection",
                graphIndex = graphIndex,
            ),
        )
        DetectionConfidence.MANUAL -> add(
            ReportWarning(
                code = "graph.crop_manual",
                message = "Graph region was selected manually.",
                severity = ReportSeverity.INFO,
                stage = "graph_detection",
                graphIndex = graphIndex,
            ),
        )
        DetectionConfidence.HIGH,
        DetectionConfidence.MEDIUM,
        null -> Unit
    }

    if (ocrResult == null) {
        add(
            ReportWarning(
                code = "axis.ocr_missing",
                message = "Axis OCR result is missing.",
                severity = ReportSeverity.WARNING,
                stage = "axis_ocr",
                graphIndex = graphIndex,
            ),
        )
    } else {
        if (!ocrResult.hasXSuggestions || !ocrResult.hasYSuggestions) {
            add(
                ReportWarning(
                    code = "axis.ocr_ticks_incomplete",
                    message = "Axis OCR did not produce enough X/Y tick suggestions.",
                    severity = ReportSeverity.WARNING,
                    stage = "axis_ocr",
                    graphIndex = graphIndex,
                ),
            )
        }
        val confidence = ocrResult.confidence
        if (confidence != null && confidence < 0.5f) {
            add(
                ReportWarning(
                    code = "axis.ocr_confidence_low",
                    message = "Axis OCR confidence is below 50%.",
                    severity = ReportSeverity.WARNING,
                    stage = "axis_ocr",
                    graphIndex = graphIndex,
                ),
            )
        }
    }
}.distinctBy { warning ->
    listOf(warning.code, warning.stage.orEmpty(), warning.graphIndex.toString()).joinToString("|")
}

private fun StageQuality.toReportWarnings(stageId: String, graphIndex: Int): List<ReportWarning> =
    warnings.mapIndexed { index, message ->
        ReportWarning(
            code = "processing.$stageId.warning_${index + 1}",
            message = message,
            severity = status.toReportSeverity(),
            stage = stageId,
            graphIndex = graphIndex,
        )
    }

private fun QualityStatus.toReportSeverity(): ReportSeverity =
    when (this) {
        QualityStatus.GOOD -> ReportSeverity.INFO
        QualityStatus.ACCEPTABLE -> ReportSeverity.WARNING
        QualityStatus.RISKY -> ReportSeverity.SERIOUS
        QualityStatus.FAILED -> ReportSeverity.FAILED
    }

private fun List<ReportWarning>.withReportGraphIndex(graphIndex: Int): List<ReportWarning> =
    map { warning -> warning.copy(graphIndex = graphIndex) }

private fun fallbackCropResult(path: String, w: Int, h: Int): CropResult {
    val width = w.coerceAtLeast(1)
    val height = h.coerceAtLeast(1)
    return CropResult(
        croppedPath = path, sourcePath = path,
        sourceWidth = width, sourceHeight = height,
        cropRect = CropRect(0, 0, width, height),
        croppedWidth = width, croppedHeight = height,
        timestamp = System.currentTimeMillis(),
    )
}

private fun fallbackPerspectiveResult(path: String, w: Int, h: Int): PerspectiveCorrectionResult {
    val width = w.coerceAtLeast(1)
    val height = h.coerceAtLeast(1)
    val fw = width.toFloat()
    val fh = height.toFloat()
    return PerspectiveCorrectionResult(
        correctedPath = path, sourcePath = path,
        homography = HomographyMatrix(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)),
        sourceCorners = DocumentCorners(
            topLeft = ImagePoint(0f, 0f), topRight = ImagePoint(fw, 0f),
            bottomLeft = ImagePoint(0f, fh), bottomRight = ImagePoint(fw, fh),
        ),
        outputWidth = width, outputHeight = height,
        maxWarpDistance = 0f,
        correctedAspectRatio = if (height > 0) fw / fh else 1f,
        isExcessiveWarp = false, timestamp = System.currentTimeMillis(),
    )
}

private fun fallbackAxesResult() = AxesResult(
    xAxis = AxisLine(0f, 100f, 1600f, 100f),
    yAxis = AxisLine(0f, 0f, 0f, 100f),
    origin = AxisOrigin(0f, 100f),
    detectionMethod = DetectionMethod.MANUAL,
    confidence = 0.5f,
    timestamp = System.currentTimeMillis(),
)

/**
 * Identity calibration: 1 pixel = 1 unit (px).
 * Used when user skips X/Y calibration steps.
 */
private fun fallbackCalibration(w: Int, h: Int): PixelCalibration {
    val fw = w.coerceAtLeast(1).toFloat()
    val fh = h.coerceAtLeast(1).toFloat()
    return PixelCalibration(
        xCalibration = com.chromalab.feature.processing.calibration.LinearCalibration(
            point1 = com.chromalab.feature.processing.calibration.CalibrationPoint(0f, 0f),
            point2 = com.chromalab.feature.processing.calibration.CalibrationPoint(fw, fw),
        ),
        yCalibration = com.chromalab.feature.processing.calibration.LinearCalibration(
            point1 = com.chromalab.feature.processing.calibration.CalibrationPoint(0f, fh),
            point2 = com.chromalab.feature.processing.calibration.CalibrationPoint(fh, 0f),
        ),
        xUnit = "px",
        yUnit = "px",
        originPixelX = 0f,
        originPixelY = fh,
        timestamp = System.currentTimeMillis(),
    )
}

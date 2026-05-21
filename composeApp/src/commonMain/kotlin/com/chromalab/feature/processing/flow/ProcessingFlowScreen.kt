package com.chromalab.feature.processing.flow

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.axis.AxisDetector
import com.chromalab.feature.processing.axis.AxisEditorScreen
import com.chromalab.feature.processing.axis.AxisLine
import com.chromalab.feature.processing.axis.AxisOrigin
import com.chromalab.feature.processing.calibration.XAxisCalibration
import com.chromalab.feature.processing.calibration.YAxisCalibration
import com.chromalab.feature.processing.calibration.buildAutomaticXAxisCalibration
import com.chromalab.feature.processing.calibration.buildAutomaticYAxisCalibration
import com.chromalab.feature.processing.calibration.toLinearCalibrationOrNull
import com.chromalab.feature.processing.crop.CropRect
import com.chromalab.feature.processing.crop.CropResult
import com.chromalab.feature.processing.crop.CropReviewScreen
import com.chromalab.feature.processing.crop.ImageCropper
import com.chromalab.feature.processing.curve.CurveEditorScreen
import com.chromalab.feature.processing.curve.CurveExtractionResult
import com.chromalab.feature.processing.curve.CurveExtractor
import com.chromalab.feature.processing.curve.CurveMaskPreparer
import com.chromalab.feature.processing.curve.CurveMaskTextSuppressionRegion
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
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryPipelineResult
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import com.chromalab.feature.processing.geometry.GeometryStageStatus
import com.chromalab.feature.processing.geometry.TickLocalizationPipeline
import com.chromalab.feature.processing.inference.ChartAnalysisReader
import com.chromalab.feature.processing.inference.ActiveInferenceModel
import com.chromalab.feature.processing.inference.ModelRuntime
import com.chromalab.feature.processing.inference.ActiveInferenceModelSnapshot
import com.chromalab.feature.processing.model.ModelAvailabilityDiagnostic
import com.chromalab.feature.processing.model.ModelAvailabilityMode
import com.chromalab.feature.processing.model.ModelAvailabilityStatus
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrSuggestionScreen
import com.chromalab.feature.processing.peaks.PeakLabelTextClassification
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
import com.chromalab.feature.processing.debug.DebugPackageExporter
import com.chromalab.feature.processing.debug.RuntimeAxisFailureSummary
import com.chromalab.feature.processing.debug.RuntimeCalibrationFailureSummary
import com.chromalab.feature.processing.debug.RuntimeGraphFailureArtifactPaths
import com.chromalab.feature.processing.debug.RuntimeGraphFailurePackage
import com.chromalab.feature.processing.debug.RuntimeTickAnchorEvidenceSummary
import com.chromalab.feature.processing.debug.RuntimeTickFailureSummary
import com.chromalab.feature.processing.debug.RuntimeTickOcrFailureSummary
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
import com.chromalab.feature.processing.normalize.ImageOrientationCorrector
import com.chromalab.feature.processing.normalize.NormalizedImageResult
import com.chromalab.feature.processing.preprocess.PreprocessingResult
import com.chromalab.feature.processing.calibration.PixelCalibration
import com.chromalab.feature.processing.report.buildProcessingReportMetadataConfig
import com.chromalab.feature.processing.report.buildProcessingReportMetadataAuditLine
import com.chromalab.feature.processing.report.currentReportDeviceName
import com.chromalab.feature.reports.ExecutedRuntime
import com.chromalab.feature.reports.GraphPreparationVariantMetadata
import com.chromalab.feature.reports.ModelExecutionInfo
import com.chromalab.feature.reports.PixelRect
import com.chromalab.feature.reports.ReportSeverity
import com.chromalab.feature.reports.ReportStageTiming
import com.chromalab.feature.reports.ReportWarning
import com.chromalab.feature.reports.RuntimeFailureClass
import com.chromalab.feature.validation.AutonomousValidationModelMode
import com.chromalab.feature.validation.AutonomousValidationTerminalFailureExporter
import com.chromalab.core.data.DatabaseProvider
import com.chromalab.core.data.entity.ChromatogramEntity
import com.chromalab.core.data.model.SourceType
import com.chromalab.feature.processing.geometry.SourceType as GeometrySourceType
import com.chromalab.feature.processing.geometry.TickOcrItemStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

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
    sourceType: SourceType = SourceType.PHOTO,
    validationModelMode: AutonomousValidationModelMode = AutonomousValidationModelMode.DETERMINISTIC_ONLY,
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
    val orientationCorrector = remember { ImageOrientationCorrector() }
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
    var geometryResult by remember { mutableStateOf<GeometryPipelineResult?>(null) }
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
    var modelAvailabilityDiagnostics by remember(imagePath) {
        mutableStateOf<List<ModelAvailabilityDiagnostic>>(emptyList())
    }

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
                    ) + geometryResult.toReportWarnings(graphNumber),
                    preparationVariants = sweepPreparationVariants,
                    geometryResult = geometryResult,
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
                        // Lazy VLM loading; validation fixtures can explicitly run either
                        // deterministic baseline or model-enabled comparison mode.
                            if (
                                sourceType == SourceType.VALIDATION_FIXTURE &&
                                modelAvailabilityDiagnostics.isEmpty()
                            ) {
                                val modelSnapshot = chartReader.currentModelSnapshot()
                                val deferredMessage = when (validationModelMode) {
                                    AutonomousValidationModelMode.DETERMINISTIC_ONLY ->
                                        "Validation fixture ran deterministic no-model baseline; graphPanel, plotArea, axis, and tick stages still executed."
                                    AutonomousValidationModelMode.MODEL_ENABLED ->
                                        "Validation fixture defers Gemma model activation until deterministic calibration completes; model output cannot affect graphPanel, plotArea, tick, or calibration geometry."
                                }
                                val diagnostic = modelSnapshot.toModelAvailabilityDiagnostic(
                                    ready = false,
                                    mode = sourceType.toModelAvailabilityMode(),
                                    loadAttempted = false,
                                    forcedStatus = ModelAvailabilityStatus.DISABLED,
                                    forcedMessage = deferredMessage,
                                )
                                modelAvailabilityDiagnostics = listOf(diagnostic)
                                vlmLoadingStatus = when (validationModelMode) {
                                    AutonomousValidationModelMode.DETERMINISTIC_ONLY ->
                                        "AI model deferred for validation fixture; deterministic CV is running"
                                    AutonomousValidationModelMode.MODEL_ENABLED ->
                                        "AI model activation scheduled after deterministic calibration"
                                }
                                println(
                                    "PIPELINE[VLM] startup load deferred for validation fixture " +
                                        "mode=$validationModelMode status=${diagnostic.status} deterministicFallback=continue",
                                )
                            } else if (!vlmReady) {
                                vlmLoadingStatus = "Загрузка AI модели..."
                                vlmReady = chartReader.ensureModelLoaded { status ->
                                    vlmLoadingStatus = status
                                }
                                val modelSnapshot = chartReader.currentModelSnapshot()
                                val diagnostic = modelSnapshot.toModelAvailabilityDiagnostic(
                                    ready = vlmReady,
                                    mode = sourceType.toModelAvailabilityMode(),
                                    loadAttempted = true,
                                )
                                modelAvailabilityDiagnostics = listOf(diagnostic)
                                vlmLoadingStatus = if (vlmReady) {
                                    "AI модель готова"
                                } else {
                                    "AI модель недоступна; продолжается deterministic CV"
                                }
                                println(
                                    "PIPELINE[VLM] Model ready: $vlmReady " +
                                        "status=${diagnostic.status} deterministicFallback=continue",
                                )
                            }

                        // Normalize first: fix EXIF orientation
                        if (normalizedResult == null) {
                            val norm = imageNormalizer.normalize(imagePath, outputDir)
                                ?: error("Image normalization failed: unable to load source image.")
                            if (norm.width <= 0 || norm.height <= 0) {
                                error("Image normalization failed: invalid source dimensions ${norm.width}x${norm.height}.")
                            }
                            val orientation = orientationCorrector.correct(norm, "$outputDir/orientation")
                            val orientedNorm = norm.copy(
                                normalizedPath = orientation.imagePath,
                                width = orientation.width,
                                height = orientation.height,
                                wasRotated = norm.wasRotated || orientation.wasRotated,
                            )
                            normalizedResult = orientedNorm
                            normalizedPath = orientation.imagePath
                            currentImagePath = orientation.imagePath
                            imageWidth = orientation.width
                            imageHeight = orientation.height
                            selectedRegion = GraphRegion(
                                0, 0, orientation.width, orientation.height,
                            )
                            if (orientation.wasRotated) {
                                println("PIPELINE[ORIENTATION] rotated=${orientation.rotationDegrees}, horizontalRuns=${orientation.horizontalRunCount}, verticalRuns=${orientation.verticalRunCount}")
                            }
                        }
                        qualityReport = qualityAnalyzer.analyze(currentImagePath)
                        println("PIPELINE[IMAGE_QUALITY] done: blur=${qualityReport?.blurScore?.score}")
                    }

                    ProcessingStep.CROP_REVIEW -> {
                        if (cropResult == null) {
                            currentImagePath = normalizedPath
                            cropResult = skippedCropResult(
                                path = currentImagePath,
                                w = imageWidth,
                                h = imageHeight,
                                status = GeometryStageStatus.SKIPPED_NOT_CONFIDENT,
                                warning = "crop.no_runtime_document_quad_identity_preserved",
                            )
                            println("PIPELINE[CROP] ${cropResult?.status}: using image as-is, w=$imageWidth h=$imageHeight")
                        }
                    }

                    ProcessingStep.PERSPECTIVE -> {
                        if (perspectiveResult == null) {
                            perspectiveResult = skippedPerspectiveResult(
                                path = currentImagePath,
                                w = imageWidth,
                                h = imageHeight,
                                status = GeometryStageStatus.SKIPPED_NOT_CONFIDENT,
                                warning = "perspective.no_runtime_quad_identity_preserved",
                            )
                            println("PIPELINE[PERSPECTIVE] ${perspectiveResult?.status}: identity geometry preserved")
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
                                sourceType = sourceType.toGeometrySourceType(),
                                // Multi-graph: reuse graph detection, target specific region
                                cachedGraphResult = if (isSubsequentGraph) graphResult else null,
                                overrideRegion = if (isSubsequentGraph) selectedRegion else null,
                                requireVlmForAnalysis = false,
                                preservePanelLabels = normalizedResult?.wasRotated == true,
                                runVlmGeometryHint = sourceType != SourceType.VALIDATION_FIXTURE,
                                onProgress = { progress ->
                                    sweepProgress = progress
                                },
                            )

                            if (sweepResults.isNotEmpty()) {
                                val best = sweepResults.first()
                                geometryResult = best.geometryResult
                                if (best.selectedRegion == null) {
                                    error("No deterministic graph ROI candidate passed geometry checks; ROI failure evidence should be exported.")
                                }
                                bestSweepConfig = best.config.name
                                sweepPreparationVariants = sweepResults.toGraphPreparationVariants()
                                println("PIPELINE[SWEEP] Winner: '${best.config.name}' score=${best.score} (${best.scoreBreakdown})")

                                // Apply sweep results to pipeline state
                                preprocessingResult = best.preprocessingResult
                                // Only update graphResult on first graph
                                if (!isSubsequentGraph) {
                                    graphResult = best.graphResult
                                    selectedRegion = best.selectedRegion
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
                                error("No deterministic graph ROI candidate passed geometry checks; ROI failure evidence should be exported.")
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
                            val calibrationRegion = geometryResult?.plotAreaBounds?.region ?: selectedRegion
                            xCalibration = geometryResult
                                ?.xCalibrationFit
                                ?.takeIf { it.status != CalibrationFitStatus.INVALID }
                                ?.toLinearCalibrationOrNull()
                                ?.takeIf { it.isValid && it.scale > 0f }
                                ?.let {
                                    XAxisCalibration(
                                        calibration = it,
                                        unit = ocr?.xUnit ?: "min",
                                        timestamp = System.currentTimeMillis(),
                                    )
                                }
                                ?: ocr.buildAutomaticXAxisCalibration(calibrationRegion, axes)
                            if (xCalibration != null) {
                                val cal = xCalibration!!.calibration
                                println("PIPELINE[X_CAL] auto: px1=${cal.point1.pixelPos}->${cal.point1.realValue}, px2=${cal.point2.pixelPos}->${cal.point2.realValue}, unit=${xCalibration!!.unit}")
                            } else {
                                failAutomaticAxisCalibration("at least two X tick labels are required before signal conversion")
                            }
                        }
                    }

                    ProcessingStep.Y_CALIBRATION -> {
                        // Auto-calibrate Y axis from OCR results
                        if (yCalibration == null) {
                            val ocr = ocrResult
                            val axes = axesResult
                            val calibrationRegion = geometryResult?.plotAreaBounds?.region ?: selectedRegion
                            yCalibration = geometryResult
                                ?.yCalibrationFit
                                ?.takeIf { it.status != CalibrationFitStatus.INVALID }
                                ?.toLinearCalibrationOrNull()
                                ?.takeIf { it.isValid && it.scale < 0f }
                                ?.let {
                                    YAxisCalibration(
                                        calibration = it,
                                        unit = ocr?.yUnit ?: "counts",
                                        timestamp = System.currentTimeMillis(),
                                    )
                                }
                                ?: ocr.buildAutomaticYAxisCalibration(calibrationRegion, axes)
                            if (yCalibration != null) {
                                val cal = yCalibration!!.calibration
                                println("PIPELINE[Y_CAL] auto: py1=${cal.point1.pixelPos}->${cal.point1.realValue}, py2=${cal.point2.pixelPos}->${cal.point2.realValue}, unit=${yCalibration!!.unit}")
                            } else {
                                failAutomaticAxisCalibration("at least two Y tick labels are required before signal conversion")
                            }

                            pixelCalibration = buildConfirmedPixelCalibration(
                                xCalibration = xCalibration,
                                yCalibration = yCalibration,
                                axesResult = axes,
                                selectedRegion = calibrationRegion,
                            ) ?: failAutomaticAxisCalibration("confirmed X and Y calibration are required before signal conversion")

                            val validationModelCanLoadNow = if (sourceType == SourceType.VALIDATION_FIXTURE) {
                                val candidateRegions = graphResult?.filteredRegions.orEmpty()
                                val completedRegions = buildList {
                                    addAll(processedGraphs.map { it.selectedRegion })
                                    add(selectedRegion)
                                }
                                nextUnprocessedGraphRegionIndex(
                                    candidateRegions = candidateRegions,
                                    currentIndex = currentGraphIndex,
                                    processedRegions = completedRegions,
                                ) == null
                            } else {
                                true
                            }
                            if (
                                sourceType == SourceType.VALIDATION_FIXTURE &&
                                validationModelMode == AutonomousValidationModelMode.MODEL_ENABLED &&
                                validationModelCanLoadNow &&
                                !vlmReady
                            ) {
                                vlmLoadingStatus = "Loading AI model after deterministic calibration..."
                                vlmReady = chartReader.ensureModelLoaded { status ->
                                    vlmLoadingStatus = status
                                }
                                val modelSnapshot = chartReader.currentModelSnapshot()
                                val diagnostic = modelSnapshot.toModelAvailabilityDiagnostic(
                                    ready = vlmReady,
                                    mode = sourceType.toModelAvailabilityMode(),
                                    loadAttempted = true,
                                )
                                modelAvailabilityDiagnostics = listOf(diagnostic)
                                vlmLoadingStatus = if (vlmReady) {
                                    "AI model ready after deterministic calibration"
                                } else {
                                    "AI model unavailable after deterministic calibration; deterministic report continues"
                                }
                                println(
                                    "PIPELINE[VLM] post-calibration model ready: $vlmReady " +
                                        "status=${diagnostic.status} deterministicFallback=preserved",
                                )
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
                            val curveRegion = geometryResult?.plotAreaBounds?.region ?: selectedRegion
                            println("PIPELINE[CURVE] input=$inputForMask, plotArea=$curveRegion, graphPanel=$selectedRegion")
                            val mask = curveMaskPreparer.prepare(
                                inputForMask, curveRegion,
                                axesResult!!, graphOutputDir,
                                geometryResult.toCurveTextSuppressionRegions(),
                            )
                            println("PIPELINE[CURVE] mask: raw=${mask.rawMaskPath}, clean=${mask.cleanMaskPath}")
                            val maskPath = mask.cleanMaskPath ?: mask.rawMaskPath ?: inputForMask
                            curveExtractionResult = curveExtractor.extract(
                                maskPath,
                                mask.maskWidth.takeIf { it > 0 } ?: curveRegion.width,
                                mask.maskHeight.takeIf { it > 0 } ?: curveRegion.height,
                                graphOutputDir,
                            ).scaledCoordinates(mask.coordinateScale)
                            geometryResult = geometryResult?.let { result ->
                                result.copy(
                                    trace = result.trace.copy(
                                        plotAreaCropPath = mask.plotAreaCropPath ?: result.trace.plotAreaCropPath,
                                        curveMaskRawPath = mask.rawMaskPath ?: result.trace.curveMaskRawPath,
                                        curveMaskCleanPath = mask.cleanMaskPath ?: result.trace.curveMaskCleanPath,
                                        curveTextSuppressionOverlayPath = mask.textSuppressionOverlayPath
                                            ?: result.trace.curveTextSuppressionOverlayPath,
                                        curveTextSuppressionRegions = mask.textSuppressionRegions
                                            .ifEmpty { result.trace.curveTextSuppressionRegions },
                                        curveRejectedComponentsPath = mask.textSuppressionOverlayPath
                                            ?: mask.traceArtifactAudit.artifactMaskPath
                                            ?: result.trace.curveRejectedComponentsPath,
                                        curveSelectedComponentPath = mask.traceArtifactAudit.cleanupHypothesisMaskPath
                                            ?: result.trace.curveSelectedComponentPath,
                                        curveSkeletonPath = "$graphOutputDir/centerline_parity_overlay.png",
                                        finalCenterlineOverlayPath = curveExtractionResult?.maskImagePath
                                            ?: result.trace.finalCenterlineOverlayPath,
                                    ),
                                )
                            }
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
                                val cal = pixelCalibration
                                    ?: error("Axis calibration is required before building a signal preview.")
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
            runCatching {
                val failureDuration = (System.currentTimeMillis() - stepStartedAt).coerceAtLeast(0L)
                val failureTimings = stageDurationMillis.toMap().toMutableMap().apply {
                    this[timedStep] = (this[timedStep] ?: 0L) + failureDuration
                }.toReportStageTimings()
                AutonomousValidationTerminalFailureExporter.exportFailureArtifacts(
                    sourceImagePath = imagePath,
                    sourceType = sourceType,
                    failureClass = e.toRuntimeFailureClass(timedStep),
                    stageId = timedStep.name,
                    failureMessage = e.message ?: "Unknown processing failure.",
                    analysisStartedAtEpochMillis = flowStartedAt,
                    analysisCompletedAtEpochMillis = System.currentTimeMillis(),
                    stageTimings = failureTimings,
                    deviceName = currentReportDeviceName(),
                    modelAvailabilityDiagnostics = modelAvailabilityDiagnostics,
                    graphFailurePackages = buildRuntimeGraphFailurePackages(
                        failureClass = e.toRuntimeFailureClass(timedStep),
                        failureStage = timedStep.name,
                        failureReason = e.message ?: "Unknown processing failure.",
                        imagePath = imagePath,
                        normalizedImagePath = currentImagePath,
                        currentGraphIndex = currentGraphIndex,
                        selectedRegion = selectedRegion,
                        graphResult = graphResult,
                        geometryResult = geometryResult,
                        ocrResult = ocrResult,
                        xCalibration = xCalibration,
                        yCalibration = yCalibration,
                        stageTimings = failureTimings,
                    ),
                )
            }.onFailure { exportError ->
                println("PIPELINE[VALIDATION_FAILURE_EVIDENCE] export_failed=${exportError.message}")
            }
            if (currentStep == ProcessingStep.GRAPH_SELECTION || currentStep == ProcessingStep.GRAPH_ROI) {
                runCatching {
                    val path = DebugPackageExporter.writeRoiFailureEvidencePackage(
                        writer = SessionWriter(outputDir),
                        stageId = currentStep.name,
                        failureReason = e.message ?: "unknown ROI failure",
                        geometryResult = geometryResult,
                        originalImagePath = imagePath,
                        normalizedImagePath = currentImagePath,
                    )
                    println("PIPELINE[ROI_FAILURE_EVIDENCE] exported=$path")
                }.onFailure { exportError ->
                    println("PIPELINE[ROI_FAILURE_EVIDENCE] export_failed=${exportError.message}")
                }
            }
            processingError = "${currentStep.label}: ${e.message ?: "неизвестная ошибка"}"
        }
        val stepDuration = (System.currentTimeMillis() - stepStartedAt).coerceAtLeast(0L)
        stageDurationMillis[timedStep] = (stageDurationMillis[timedStep] ?: 0L) + stepDuration
        isProcessing = false

        // --- Auto-advance logic ---
        // All steps auto-advance except EXPORT (the final dashboard).
        if (processingError == null) {
            val shouldAutoAdvance = currentStep.autoAdvance == AutoAdvancePolicy.ALWAYS &&
                currentStep.hasRequiredCalibrationOutput(
                    xCalibration = xCalibration,
                    yCalibration = yCalibration,
                    pixelCalibration = pixelCalibration,
                )
            if (shouldAutoAdvance) {
                kotlinx.coroutines.delay(150L)
                if (currentStep == ProcessingStep.QUALITY_REPORT) {
                    // Multi-graph: check for more regions before advancing to EXPORT
                    captureCurrentGraphSnapshot()
                    val candidateRegions = graphResult?.filteredRegions.orEmpty()
                    val nextGraphIndex = nextUnprocessedGraphRegionIndex(
                        candidateRegions = candidateRegions,
                        currentIndex = currentGraphIndex,
                        processedRegions = processedGraphs.map { it.selectedRegion },
                    )
                    if (nextGraphIndex != null) {
                        currentGraphIndex = nextGraphIndex
                        val nextRegion = candidateRegions[nextGraphIndex]
                        selectedRegion = nextRegion
                        println("PIPELINE[MULTI] Advancing to graph ${currentGraphIndex + 1}/${candidateRegions.size}: ${nextRegion.label}")
                        // Reset per-graph state
                        axesResult = null
                        xCalibration = null
                        yCalibration = null
                        ocrResult = null
                        pixelCalibration = null
                        curveExtractionResult = null
                        curvePoints = emptyList()
                        geometryResult = null
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
                            ?: ExecutedRuntime.DETERMINISTIC
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
                                val detectedGraphCount = graphsToSave.size.coerceAtLeast(1)
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
                                    val algorithmConfig = buildProcessingReportMetadataConfig(
                                        sourcePath = imagePath,
                                        processedPath = currentImagePath,
                                        sourceType = sourceType,
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
                                        geometryReportStatus = entry.geometryResult?.reportStatus,
                                        geometryTrace = entry.geometryResult?.trace,
                                        selectedModel = selectedReportModel,
                                        executedModel = executedReportModel,
                                        executedRuntime = executedRuntime,
                                        modelAvailabilityDiagnostics = modelAvailabilityDiagnostics,
                                        deviceName = reportDeviceName,
                                        stageTimings = reportStageTimings,
                                        graphWarnings = entry.warnings.withReportGraphIndex(entry.graphIndex),
                                    )
                                    val entity = ChromatogramEntity(
                                        sampleId = sId,
                                        sourceType = sourceType,
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
                                        algorithmConfig = algorithmConfig,
                                        createdAt = now,
                                        updatedAt = now,
                                    )
                                    val id = db.chromatogramDao().insert(entity)
                                    if (firstId == null) firstId = id
                                    println(
                                        "PIPELINE[REPORT_AUDIT] " +
                                            buildProcessingReportMetadataAuditLine(algorithmConfig, id),
                                    )
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
            val candidateRegions = graphResult?.filteredRegions.orEmpty()
            val nextGraphIndex = nextUnprocessedGraphRegionIndex(
                candidateRegions = candidateRegions,
                currentIndex = currentGraphIndex,
                processedRegions = processedGraphs.map { it.selectedRegion },
            )
            if (nextGraphIndex != null) {
                // More regions — reset per-graph state and loop back
                currentGraphIndex = nextGraphIndex
                val nextRegion = candidateRegions[nextGraphIndex]
                selectedRegion = nextRegion
                println("PIPELINE[MULTI] Advancing to graph ${currentGraphIndex + 1}/${candidateRegions.size}: ${nextRegion.label} (${nextRegion.width}x${nextRegion.height})")
                // Reset per-graph results
                axesResult = null
                xCalibration = null
                yCalibration = null
                ocrResult = null
                pixelCalibration = null
                curveExtractionResult = null
                curvePoints = emptyList()
                geometryResult = null
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
                        ?: ExecutedRuntime.DETERMINISTIC
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
                            val detectedGraphCount = graphsToSave.size.coerceAtLeast(1)
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
                                val algorithmConfig = buildProcessingReportMetadataConfig(
                                    sourcePath = imagePath,
                                    processedPath = currentImagePath,
                                    sourceType = sourceType,
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
                                    geometryReportStatus = entry.geometryResult?.reportStatus,
                                    geometryTrace = entry.geometryResult?.trace,
                                    selectedModel = selectedReportModel,
                                    executedModel = executedReportModel,
                                    executedRuntime = executedRuntime,
                                    modelAvailabilityDiagnostics = modelAvailabilityDiagnostics,
                                    deviceName = reportDeviceName,
                                    stageTimings = reportStageTimings,
                                    graphWarnings = entry.warnings.withReportGraphIndex(entry.graphIndex),
                                )
                                val entity = ChromatogramEntity(
                                    sampleId = sampleId,
                                    sourceType = sourceType,
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
                                    algorithmConfig = algorithmConfig,
                                    createdAt = now,
                                    updatedAt = now,
                                )
                                val id = db.chromatogramDao().insert(entity)
                                if (firstId == null) firstId = id
                                println(
                                    "PIPELINE[REPORT_AUDIT] " +
                                        buildProcessingReportMetadataAuditLine(algorithmConfig, id),
                                )
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
    Column(modifier = modifier.fillMaxSize()) {
        // Step content
        Box(modifier = Modifier.weight(1f)) {
            // All steps auto-advance — show processing overlay
            if (currentStep != ProcessingStep.EXPORT) {
                AutoProgressOverlay(
                    currentStep = currentStep,
                    isProcessing = isProcessing,
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
                                        "Этот этап обязателен для полного анализа и не может быть пропущен.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                                ProcessingErrorActions(
                                    blocksSkip = blocksSkip,
                                    onCancel = onCancel,
                                    onRetry = {
                                        processingError = null
                                        val s = currentStep
                                        currentStep = ProcessingStep.FIRST
                                        currentStep = s
                                    },
                                    onSkip = {
                                        processingError = null
                                        advance(currentStep)
                                    },
                                )
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
                val exportCalibration = pixelCalibration
                if (displaySignal != null && exportCalibration != null) {
                    val cal = exportCalibration
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
                                if (displaySignal == null) {
                                    "Не удалось извлечь сигнал"
                                } else {
                                    "Не подтверждена калибровка осей"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                if (displaySignal == null) {
                                    "Попробуйте сделать более четкое фото"
                                } else {
                                    "Расчет и экспорт заблокированы, пока шкалы X/Y не подтверждены."
                                },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProcessingErrorActions(
    blocksSkip: Boolean,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onSkip: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        maxItemsInEachRow = 2,
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.widthIn(min = 112.dp),
        ) {
            Text("Отмена", maxLines = 1)
        }
        Button(
            onClick = onRetry,
            modifier = Modifier.widthIn(min = 128.dp),
        ) {
            Text("Повторить", maxLines = 1)
        }
        if (!blocksSkip) {
            TextButton(
                onClick = onSkip,
                modifier = Modifier.widthIn(min = 128.dp),
            ) {
                Text("Пропустить", maxLines = 1)
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
        add("Crop stage ${it.status}: ${it.cropRect.width}x${it.cropRect.height}")
        it.warnings.forEach { warning -> add("Crop warning: $warning") }
    }
    perspectiveResult?.let {
        add("Perspective stage ${it.status}: ${it.outputWidth}x${it.outputHeight}, excessiveWarp=${it.isExcessiveWarp}")
        it.warnings.forEach { warning -> add("Perspective warning: $warning") }
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

private fun ActiveInferenceModelSnapshot.toModelAvailabilityDiagnostic(
    ready: Boolean,
    mode: ModelAvailabilityMode,
    loadAttempted: Boolean,
    forcedStatus: ModelAvailabilityStatus? = null,
    forcedMessage: String? = null,
): ModelAvailabilityDiagnostic {
    val selected = selectedModel
    val executed = executedModel
    val status = forcedStatus ?: when {
        ready -> ModelAvailabilityStatus.AVAILABLE
        selected == null && executed == null -> ModelAvailabilityStatus.NOT_CONFIGURED
        executed == null -> ModelAvailabilityStatus.UNAVAILABLE
        else -> ModelAvailabilityStatus.LOAD_FAILED
    }
    return ModelAvailabilityDiagnostic(
        diagnosticId = "model-availability:${mode.name.lowercase()}",
        mode = mode,
        selectedModelId = selected?.modelId,
        executedModelId = executed?.modelId,
        expectedBackend = "Gemma-4-E4B LiteRT-LM FULL_ANALYSIS primary; Gemma-4-E2B LiteRT-LM FAST fallback; GGUF optional compatibility.",
        loadAttempted = loadAttempted,
        loadResult = if (ready) "loaded" else "not_loaded",
        sanitizedErrorMessage = if (ready) {
            null
        } else {
            forcedMessage
                ?: "Chromatogram VLM is unavailable. Deterministic graphPanel, plotArea, axis, and tick stages must still run."
        },
        fallbackModelAttempted = selected != null,
        fallbackResult = if (ready) "not_needed" else "unavailable",
        status = status,
        timestampEpochMillis = System.currentTimeMillis(),
    )
}

private fun SourceType.toModelAvailabilityMode(): ModelAvailabilityMode =
    when (this) {
        SourceType.VALIDATION_FIXTURE -> ModelAvailabilityMode.VALIDATION_FIXTURE
        else -> ModelAvailabilityMode.FULL_ANALYSIS
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

private fun Throwable.toRuntimeFailureClass(step: ProcessingStep): RuntimeFailureClass {
    val message = this.message.orEmpty().lowercase()
    return when {
        "model asset missing" in message || "asset missing" in message || "file missing" in message ->
            RuntimeFailureClass.MODEL_ASSET_MISSING
        "model not configured" in message || "no controller" in message ->
            RuntimeFailureClass.MODEL_NOT_CONFIGURED
        "model load failed" in message || "load failed" in message ->
            RuntimeFailureClass.MODEL_LOAD_FAILED
        "vision model is required" in message ||
            "model unavailable" in message ||
            "no chromatogram vision model" in message -> RuntimeFailureClass.VLM_MODEL_UNAVAILABLE
        "decode" in message || "load source image" in message -> RuntimeFailureClass.IMAGE_DECODE_FAILURE
        "orientation" in message -> RuntimeFailureClass.ORIENTATION_FAILURE
        "tick" in message -> RuntimeFailureClass.TICK_LOCALIZATION_FAILURE
        "ocr" in message -> RuntimeFailureClass.OCR_TICK_FAILURE
        "calibration" in message -> RuntimeFailureClass.CALIBRATION_FAILURE
        "trace" in message || "curve" in message -> RuntimeFailureClass.TRACE_EXTRACTION_FAILURE
        "peak" in message -> RuntimeFailureClass.PEAK_EVIDENCE_FAILURE
        "timeout" in message -> RuntimeFailureClass.PERFORMANCE_TIMEOUT
        step == ProcessingStep.GRAPH_SELECTION -> RuntimeFailureClass.MULTI_GRAPH_SPLIT_FAILURE
        step == ProcessingStep.GRAPH_ROI -> RuntimeFailureClass.GRAPH_PANEL_FAILURE
        step == ProcessingStep.AXIS_DETECTION -> RuntimeFailureClass.AXIS_DETECTION_FAILURE
        step == ProcessingStep.X_CALIBRATION || step == ProcessingStep.Y_CALIBRATION ->
            RuntimeFailureClass.CALIBRATION_FAILURE
        step == ProcessingStep.CURVE_EXTRACTION || step == ProcessingStep.SIGNAL_PREVIEW ->
            RuntimeFailureClass.TRACE_EXTRACTION_FAILURE
        else -> RuntimeFailureClass.UNKNOWN_FAILURE
    }
}

private fun SourceType.toGeometrySourceType(): GeometrySourceType =
    when (this) {
        SourceType.PHOTO -> GeometrySourceType.CAMERA
        SourceType.GALLERY -> GeometrySourceType.GALLERY
        SourceType.VALIDATION_FIXTURE -> GeometrySourceType.VALIDATION_FIXTURE
        SourceType.PDF,
        SourceType.CSV,
        SourceType.MZML,
        SourceType.MANUAL -> GeometrySourceType.UNKNOWN
    }

private fun ProcessingStep.hasRequiredCalibrationOutput(
    xCalibration: XAxisCalibration?,
    yCalibration: YAxisCalibration?,
    pixelCalibration: PixelCalibration?,
): Boolean =
    when (this) {
        ProcessingStep.X_CALIBRATION -> xCalibration?.calibration?.isValid == true
        ProcessingStep.Y_CALIBRATION -> yCalibration?.calibration?.isValid == true &&
            pixelCalibration?.isValid == true
        else -> true
    }

private fun buildConfirmedPixelCalibration(
    xCalibration: XAxisCalibration?,
    yCalibration: YAxisCalibration?,
    axesResult: AxesResult?,
    selectedRegion: GraphRegion,
): PixelCalibration? {
    val xCal = xCalibration?.takeIf { it.calibration.isValid } ?: return null
    val yCal = yCalibration?.takeIf { it.calibration.isValid } ?: return null
    val origin = axesResult?.origin
    return PixelCalibration.from(
        xAxis = xCal,
        yAxis = yCal,
        originPixelX = (origin?.x ?: selectedRegion.x.toFloat()) - selectedRegion.x.toFloat(),
        originPixelY = (origin?.y ?: (selectedRegion.y + selectedRegion.height).toFloat()) -
        selectedRegion.y.toFloat(),
    )
}

private fun buildRuntimeGraphFailurePackages(
    failureClass: RuntimeFailureClass,
    failureStage: String,
    failureReason: String,
    imagePath: String,
    normalizedImagePath: String,
    currentGraphIndex: Int,
    selectedRegion: GraphRegion,
    graphResult: GraphRegionResult?,
    geometryResult: GeometryPipelineResult?,
    ocrResult: AxisOcrResult?,
    xCalibration: XAxisCalibration?,
    yCalibration: YAxisCalibration?,
    stageTimings: List<ReportStageTiming>,
): List<RuntimeGraphFailurePackage> {
    val trace = geometryResult?.trace
    val graphPanelBounds = geometryResult?.graphPanelBounds?.region
        ?: trace?.selectedGraphPanelBounds?.region
        ?: graphResult?.filteredRegions?.getOrNull(currentGraphIndex)
        ?: selectedRegion.takeIf { it.width > 1 && it.height > 1 }
    val plotAreaBounds = geometryResult?.plotAreaBounds?.region
        ?: trace?.selectedPlotAreaBounds?.region

    if (graphPanelBounds == null && geometryResult == null) return emptyList()

    val axisGeometry = geometryResult?.axisGeometry ?: trace?.axisGeometry
    val tickGeometry = geometryResult?.tickGeometry ?: trace?.tickGeometry
    val tickOcr = geometryResult?.tickOcrResult ?: trace?.tickOcrResult
    val xFit = geometryResult?.xCalibrationFit ?: trace?.xCalibrationFit
    val yFit = geometryResult?.yCalibrationFit ?: trace?.yCalibrationFit
    val layoutClassification = trace?.multiplicityResolution?.layoutClassification
    val tickLocalization = TickLocalizationPipeline.evaluate(
        plotAreaBounds = geometryResult?.plotAreaBounds ?: trace?.selectedPlotAreaBounds,
        axisGeometry = axisGeometry,
        tickGeometry = tickGeometry,
        tickOcrResult = tickOcr,
        xCalibrationFit = xFit,
        yCalibrationFit = yFit,
    )
    val acceptedAnchors = tickOcr?.acceptedItems.orEmpty()
    val rejectedAnchors = tickOcr?.items.orEmpty()
        .filterNot { it.status == TickOcrItemStatus.ACCEPTED }

    return listOf(
        RuntimeGraphFailurePackage(
            graphIndex = currentGraphIndex + 1,
            failureClass = failureClass,
            failureStage = failureStage,
            failureReason = failureReason,
            layoutClass = layoutClassification?.layoutClass,
            layoutPhysicalGraphCount = layoutClassification?.physicalGraphCount,
            graphPanelBounds = graphPanelBounds,
            graphPanelMissingReason = graphPanelBounds.missingReason("graphPanel bounds were not available at failure time."),
            plotAreaBounds = plotAreaBounds,
            plotAreaMissingReason = plotAreaBounds.missingReason("plotArea bounds were not available at failure time."),
            axisSummary = RuntimeAxisFailureSummary(
                xAxisLineAvailable = axisGeometry?.xAxisLinePx != null,
                yAxisLineAvailable = axisGeometry?.yAxisLinePx != null,
                originAvailable = axisGeometry?.originPx != null,
                axisConfidence = axisGeometry?.axisConfidence ?: 0f,
                warnings = axisGeometry?.warnings.orEmpty(),
            ),
            tickSummary = RuntimeTickFailureSummary(
                sourceMethod = tickGeometry?.source ?: "deterministic_cv_unavailable",
                xTickCandidateCount = tickGeometry?.xTicks.orEmpty().size,
                yTickCandidateCount = tickGeometry?.yTicks.orEmpty().size,
                xTickPixelPositions = tickGeometry?.xTicks.orEmpty().map { it.pixelCoordinate },
                yTickPixelPositions = tickGeometry?.yTicks.orEmpty().map { it.pixelCoordinate },
                readyForOcrValueMatching = tickGeometry?.let {
                    it.xTicks.size >= 2 && it.yTicks.size >= 2
                } == true,
                subreasons = tickLocalization.subreasons,
                warnings = tickGeometry?.warnings.orEmpty() + tickLocalization.warnings,
            ),
            ocrSummary = RuntimeTickOcrFailureSummary(
                rawElementCount = ocrResult?.rawElements?.size ?: tickOcr?.items.orEmpty().size,
                numericElementCount = ocrResult?.rawElements?.count { it.numericValue != null }
                    ?: tickOcr?.items.orEmpty().count { it.parsedNumericValue != null },
                acceptedXAnchorCount = acceptedAnchors.count { it.axis.name == "X" },
                acceptedYAnchorCount = acceptedAnchors.count { it.axis.name == "Y" },
                semanticOnlyCount = tickOcr?.items.orEmpty().count { it.status == TickOcrItemStatus.SEMANTIC_ONLY },
                acceptedAnchors = acceptedAnchors.map { item ->
                    RuntimeTickAnchorEvidenceSummary(
                        axis = item.axis,
                        tickPixelPosition = item.tickPixelPosition,
                        rawText = item.rawText,
                        parsedNumericValue = item.parsedNumericValue,
                        localCropPath = item.localCropPath,
                        confidence = item.confidence,
                        status = item.status,
                        rejectionReason = item.rejectionReason,
                    )
                },
                rejectedAnchors = rejectedAnchors.map { item ->
                    RuntimeTickAnchorEvidenceSummary(
                        axis = item.axis,
                        tickPixelPosition = item.tickPixelPosition,
                        rawText = item.rawText,
                        parsedNumericValue = item.parsedNumericValue,
                        localCropPath = item.localCropPath,
                        confidence = item.confidence,
                        status = item.status,
                        rejectionReason = item.rejectionReason,
                    )
                },
                warnings = tickOcr?.warnings.orEmpty() + ocrResult?.warnings.orEmpty(),
            ),
            calibrationSummary = RuntimeCalibrationFailureSummary(
                xStatus = xFit?.status,
                yStatus = yFit?.status,
                xAcceptedAnchorCount = xFit?.acceptedAnchors.orEmpty().size,
                yAcceptedAnchorCount = yFit?.acceptedAnchors.orEmpty().size,
                xRejectedAnchorCount = xFit?.rejectedAnchors.orEmpty().size,
                yRejectedAnchorCount = yFit?.rejectedAnchors.orEmpty().size,
                xMaxResidualPx = xFit?.maxResidualPx,
                yMaxResidualPx = yFit?.maxResidualPx,
                xRmsePx = xFit?.rmsePx,
                yRmsePx = yFit?.rmsePx,
                xWarnings = xFit?.warnings.orEmpty() + xCalibration?.warnings.orEmpty(),
                yWarnings = yFit?.warnings.orEmpty() + yCalibration?.warnings.orEmpty(),
                missingReason = if (xFit == null || yFit == null) {
                    "calibration fit evidence was incomplete at failure time."
                } else {
                    null
                },
            ),
            artifactPaths = RuntimeGraphFailureArtifactPaths(
                originalImagePath = trace?.originalImagePath ?: imagePath,
                normalizedImagePath = trace?.normalizedImagePath ?: normalizedImagePath,
                rectifiedImagePath = trace?.rectifiedImagePath,
                graphPanelOverlayPath = trace?.selectedGraphPanelOverlayPath,
                graphPanelOverlayMissingReason = trace?.selectedGraphPanelOverlayPath
                    .missingReason("graphPanel overlay was not produced before $failureStage."),
                plotAreaOverlayPath = trace?.selectedPlotAreaOverlayPath,
                plotAreaOverlayMissingReason = trace?.selectedPlotAreaOverlayPath
                    .missingReason("plotArea overlay was not produced before $failureStage."),
                axisOverlayPath = trace?.axisOverlayPath,
                axisOverlayMissingReason = trace?.axisOverlayPath
                    .missingReason("axis candidate overlay was not produced before $failureStage."),
                tickOverlayPath = trace?.tickOverlayPath,
                tickOverlayMissingReason = trace?.tickOverlayPath
                    .missingReason("tick candidate overlay was not produced before $failureStage."),
                calibrationOverlayPath = trace?.calibrationFitOverlayPath,
                calibrationOverlayMissingReason = trace?.calibrationFitOverlayPath
                    .missingReason("calibration overlay was not produced before $failureStage."),
                ocrCropPaths = trace?.ocrCropPaths.orEmpty(),
                ocrCropMissingReason = trace?.ocrCropPaths.orEmpty()
                    .takeIf { it.isEmpty() }
                    ?.let { "OCR crop grid was not produced before $failureStage." },
            ),
            rejectionReasons = buildList {
                add(failureReason)
                addAll(tickLocalization.subreasons.map { "tick_localization.subreason:${it.name}" })
                addAll(tickOcr?.items.orEmpty().mapNotNull { it.rejectionReason })
                addAll(xFit?.rejectedAnchors.orEmpty().mapNotNull { it.rejectionReason })
                addAll(yFit?.rejectedAnchors.orEmpty().mapNotNull { it.rejectionReason })
            }.distinct(),
            warnings = buildList {
                addAll(geometryResult?.warnings.orEmpty())
                addAll(trace?.warnings.orEmpty())
                addAll(axisGeometry?.warnings.orEmpty())
                addAll(tickGeometry?.warnings.orEmpty())
                addAll(tickOcr?.warnings.orEmpty())
                addAll(ocrResult?.warnings.orEmpty())
                addAll(xFit?.warnings.orEmpty())
                addAll(yFit?.warnings.orEmpty())
                addAll(layoutClassification?.reviewReasons.orEmpty())
            }.distinct(),
            stageTimings = stageTimings,
        ),
    )
}

private fun GraphRegion?.missingReason(reason: String): String? =
    if (this == null) reason else null

private fun String?.missingReason(reason: String): String? =
    if (isNullOrBlank()) reason else null

private fun failAutomaticAxisCalibration(reason: String): Nothing {
    error(
        "Automatic axis calibration failed: $reason. " +
            "The report was not saved because X/Y axes must be calibrated automatically.",
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
    val geometryResult: GeometryPipelineResult?,
)

private fun nextUnprocessedGraphRegionIndex(
    candidateRegions: List<GraphRegion>,
    currentIndex: Int,
    processedRegions: List<GraphRegion>,
): Int? =
    candidateRegions.indices
        .drop(currentIndex + 1)
        .firstOrNull { index ->
            val candidate = candidateRegions[index]
            processedRegions.none { processed -> !candidate.isDistinctGraphRegionFrom(processed) }
        }

private fun GraphRegion.isDistinctGraphRegionFrom(other: GraphRegion): Boolean {
    val overlapWidth = (min(right, other.right) - max(x, other.x)).coerceAtLeast(0)
    val overlapHeight = (min(bottom, other.bottom) - max(y, other.y)).coerceAtLeast(0)
    val overlapArea = overlapWidth * overlapHeight
    if (overlapArea <= 0) return true
    val smallerArea = min(area, other.area).coerceAtLeast(1)
    val overlapRatio = overlapArea.toFloat() / smallerArea.toFloat()
    return overlapRatio < 0.72f
}

private fun List<ProcessedGraphSnapshot>.toReportSaveEntries(): List<ProcessedGraphSnapshot> =
    sortedBy { it.graphIndex }.filter { it.signal.smoothed.points.size >= 10 }

private fun GeometryPipelineResult?.toCurveTextSuppressionRegions(): List<CurveMaskTextSuppressionRegion> =
    this
        ?.trace
        ?.peakLabelEvidence
        .orEmpty()
        .mapNotNull { evidence ->
            val region = evidence.labelBoxPx ?: return@mapNotNull null
            val classification = evidence.textClassification.name
            val reason = when (evidence.textClassification) {
                PeakLabelTextClassification.PEAK_ANNOTATION ->
                    "suppress_peak_annotation_text_before_curve_mask"
                PeakLabelTextClassification.TITLE_OR_CHANNEL,
                PeakLabelTextClassification.AXIS_LABEL,
                PeakLabelTextClassification.TICK_LABEL ->
                    "suppress_non_signal_text_before_curve_mask"
                PeakLabelTextClassification.PAGE_TEXT,
                PeakLabelTextClassification.UNKNOWN_TEXT ->
                    "suppress_unclassified_ocr_text_before_curve_mask"
            }
            CurveMaskTextSuppressionRegion(
                region = region,
                classification = classification,
                source = evidence.source.name,
                reason = reason,
            )
        }

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

private fun GeometryPipelineResult?.toReportWarnings(graphIndex: Int): List<ReportWarning> {
    val result = this ?: return emptyList()
    val severity = when (result.reportStatus) {
        GeometryReportStatus.SCIENTIFIC_READY -> ReportSeverity.INFO
        GeometryReportStatus.REVIEW_READY -> ReportSeverity.WARNING
        GeometryReportStatus.DIAGNOSTIC_ONLY -> ReportSeverity.FAILED
    }
    return result.warnings
        .ifEmpty { listOf("geometry.${result.reportStatus.name.lowercase()}") }
        .take(12)
        .mapIndexed { index, warning ->
            ReportWarning(
                code = "geometry.${warning.substringBefore(':').take(48).ifBlank { "warning_${index + 1}" }}",
                message = warning,
                severity = severity,
                stage = "geometry_pipeline",
                graphIndex = graphIndex,
            )
        }
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

private fun skippedCropResult(
    path: String,
    w: Int,
    h: Int,
    status: GeometryStageStatus,
    warning: String,
): CropResult {
    val width = w.coerceAtLeast(1)
    val height = h.coerceAtLeast(1)
    return CropResult(
        croppedPath = path, sourcePath = path,
        sourceWidth = width, sourceHeight = height,
        cropRect = CropRect(0, 0, width, height),
        croppedWidth = width, croppedHeight = height,
        timestamp = System.currentTimeMillis(),
        status = status,
        warnings = listOf(warning),
    )
}

private fun skippedPerspectiveResult(
    path: String,
    w: Int,
    h: Int,
    status: GeometryStageStatus,
    warning: String,
): PerspectiveCorrectionResult {
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
        status = status,
        warnings = listOf(warning),
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

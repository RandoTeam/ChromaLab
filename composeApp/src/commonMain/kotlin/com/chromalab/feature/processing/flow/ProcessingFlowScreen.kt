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
import com.chromalab.feature.processing.document.DocumentDetector
import com.chromalab.feature.processing.graph.DetectionConfidence
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.graph.GraphRegionDetector
import com.chromalab.feature.processing.graph.GraphRegionResult
import com.chromalab.feature.processing.graph.GraphRoiEditorScreen
import com.chromalab.feature.processing.graph.GraphSelectionScreen
import com.chromalab.feature.processing.ocr.AxisOcrReader
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
import com.chromalab.feature.processing.quality.ImageQualityAnalyzer
import com.chromalab.feature.processing.quality.ImageQualityReport
import com.chromalab.feature.processing.quality.ImageQualityScreen
import com.chromalab.feature.processing.quality.QualityLevel
import com.chromalab.feature.processing.quality.QualityMetric
import com.chromalab.feature.processing.quality.QualityCalculator
import com.chromalab.feature.processing.quality.DigitizationQualityReport
import com.chromalab.feature.processing.quality.QualityReportContent
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
import com.chromalab.core.data.DatabaseProvider
import com.chromalab.core.data.entity.ChromatogramEntity
import com.chromalab.core.data.model.SourceType
import kotlinx.coroutines.Dispatchers
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
    var currentStep by remember { mutableStateOf(ProcessingStep.FIRST) }
    var isProcessing by remember { mutableStateOf(false) }

    // Working directory for intermediate files
    val outputDir = remember {
        val sep = if (imagePath.contains('\\')) '\\' else '/'
        imagePath.substringBeforeLast(sep)
    }

    // Platform processors — instantiated once
    val qualityAnalyzer = remember { ImageQualityAnalyzer() }
    val imageCropper = remember { ImageCropper() }
    val imageNormalizer = remember { ImageNormalizer() }
    val documentDetector = remember { DocumentDetector() }
    val perspectiveWarper = remember { PerspectiveWarper() }
    val preprocessor = remember { ImagePreprocessor() }
    val graphDetector = remember { GraphRegionDetector() }
    val axisDetector = remember { AxisDetector() }
    val ocrReader = remember { AxisOcrReader() }
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

    // Multi-graph support
    var currentGraphIndex by remember { mutableIntStateOf(0) }
    val processedSignals = remember { mutableStateListOf<SmoothedSignal>() }

    // --- Run real processing on step entry ---
    var processingError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentStep) {
        isProcessing = true
        processingError = null  // clear previous error on step entry
        try {
            withContext(Dispatchers.Default) {
                when (currentStep) {
                    ProcessingStep.IMAGE_QUALITY -> {
                        // Normalize first: fix EXIF orientation
                        if (normalizedResult == null) {
                            val norm = imageNormalizer.normalize(imagePath, outputDir)
                            if (norm != null) {
                                normalizedResult = norm
                                normalizedPath = norm.normalizedPath
                                currentImagePath = norm.normalizedPath
                                imageWidth = norm.width
                                imageHeight = norm.height
                                selectedRegion = GraphRegion(
                                    0, 0, norm.width, norm.height,
                                )
                            } else {
                                normalizedPath = imagePath
                                currentImagePath = imagePath
                            }
                        }
                        qualityReport = qualityAnalyzer.analyze(currentImagePath)
                    }

                    ProcessingStep.CROP_REVIEW -> {
                        if (cropResult == null) {
                            // Reset to normalized path (in case of recrop)
                            currentImagePath = normalizedPath
                            val bounds = documentDetector.detect(currentImagePath)
                            documentBounds = bounds  // cache for perspective step
                            if (bounds != null) {
                                val c = bounds.corners
                                val x1 = min(c.topLeft.x, c.bottomLeft.x).roundToInt()
                                val y1 = min(c.topLeft.y, c.topRight.y).roundToInt()
                                val x2 = max(c.topRight.x, c.bottomRight.x).roundToInt()
                                val y2 = max(c.bottomLeft.y, c.bottomRight.y).roundToInt()
                                val rect = CropRect(
                                    x = x1.coerceAtLeast(0),
                                    y = y1.coerceAtLeast(0),
                                    width = (x2 - x1).coerceAtLeast(1),
                                    height = (y2 - y1).coerceAtLeast(1),
                                )
                                val result = imageCropper.crop(currentImagePath, rect, outputDir)
                                if (result != null) {
                                    cropResult = result
                                    currentImagePath = result.croppedPath
                                } else {
                                    cropResult = fallbackCropResult(currentImagePath, imageWidth, imageHeight)
                                }
                            } else {
                                // No document found — proceed with full image
                                cropResult = fallbackCropResult(currentImagePath, imageWidth, imageHeight)
                            }
                        }
                    }

                    ProcessingStep.PERSPECTIVE -> {
                        if (perspectiveResult == null) {
                            // Reuse cached bounds from crop step (avoid double detect)
                            val bounds = documentBounds
                            if (bounds != null) {
                                val result = perspectiveWarper.warp(
                                    currentImagePath, bounds.corners, outputDir,
                                )
                                if (result != null) {
                                    perspectiveResult = result
                                    currentImagePath = result.correctedPath
                                } else {
                                    perspectiveResult = fallbackPerspectiveResult(
                                        currentImagePath, imageWidth, imageHeight,
                                    )
                                }
                            } else {
                                perspectiveResult = fallbackPerspectiveResult(
                                    currentImagePath, imageWidth, imageHeight,
                                )
                            }
                        }
                    }

                    ProcessingStep.GRAPH_SELECTION, ProcessingStep.GRAPH_ROI -> {
                        // Run preprocessing (grayscale→CLAHE→binary) once
                        if (preprocessingResult == null) {
                            preprocessingResult = preprocessor.preprocess(
                                currentImagePath, outputDir,
                            )
                        }
                        if (graphResult == null) {
                            val w = imageWidth.takeIf { it > 0 } ?: 1920
                            val h = imageHeight.takeIf { it > 0 } ?: 1080
                            graphResult = graphDetector.detect(currentImagePath, w, h)
                            graphResult?.selectedRegion?.let { selectedRegion = it }
                        }
                    }

                    ProcessingStep.AXIS_DETECTION -> {
                        if (axesResult == null) {
                            axesResult = axisDetector.detect(currentImagePath, selectedRegion)
                        }
                    }

                    ProcessingStep.OCR_SUGGESTION -> {
                        if (ocrResult == null) {
                            ocrResult = ocrReader.readAxisLabels(
                                currentImagePath, selectedRegion,
                            )
                        }
                    }

                    ProcessingStep.CURVE_EXTRACTION -> {
                        if (curveExtractionResult == null && axesResult != null) {
                            // Use preprocessed binary image for mask preparation
                            val inputForMask = preprocessingResult?.binaryPath ?: currentImagePath
                            val mask = curveMaskPreparer.prepare(
                                inputForMask, selectedRegion,
                                axesResult!!, outputDir,
                            )
                            val maskPath = mask.cleanMaskPath ?: mask.rawMaskPath ?: inputForMask
                            curveExtractionResult = curveExtractor.extract(
                                maskPath, selectedRegion.width,
                                selectedRegion.height, outputDir,
                            )
                            curvePoints = curveExtractionResult?.points ?: emptyList()
                        }
                    }

                    ProcessingStep.SIGNAL_PREVIEW -> {
                        if (smoothedSignal == null && curvePoints.isNotEmpty()) {
                            // Build or use fallback identity calibration
                            val cal = pixelCalibration ?: fallbackCalibration(
                                imageWidth, imageHeight,
                            )
                            val signal = SignalConverter.convert(
                                curvePoints, cal, currentImagePath,
                            )
                            smoothedSignal = SignalSmoother.smooth(signal)
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
        isProcessing = false
    }

    var savedSignalId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    // When signal is saved, navigate to analysis
    LaunchedEffect(savedSignalId) {
        savedSignalId?.let { onFinish(it) }
    }

    val advance = { step: ProcessingStep ->
        val next = step.next()
        // Multi-graph loop: after QUALITY_REPORT, check for more regions
        if (step == ProcessingStep.QUALITY_REPORT) {
            // Save current signal to the batch
            smoothedSignal?.let { processedSignals.add(it) }
            val totalRegions = graphResult?.regions?.size ?: 1
            if (currentGraphIndex + 1 < totalRegions) {
                // More regions — reset per-graph state and loop back
                currentGraphIndex++
                val nextRegion = graphResult!!.regions[currentGraphIndex]
                selectedRegion = nextRegion
                axesResult = null
                xCalibration = null
                yCalibration = null
                ocrResult = null
                pixelCalibration = null
                curveExtractionResult = null
                curvePoints = emptyList()
                smoothedSignal = null
                digitizationReport = null
                currentStep = ProcessingStep.GRAPH_ROI
            } else if (next != null) {
                currentStep = next
            } else {
                // Shouldn't happen — EXPORT is after QUALITY_REPORT
                currentStep = ProcessingStep.EXPORT
            }
        } else if (next != null) {
            currentStep = next
        } else {
            // Last step — save signal to Room, then navigate
            scope.launch {
                val signal = smoothedSignal?.smoothed
                val now = System.currentTimeMillis()
                if (signal != null) {
                    try {
                        val dao = DatabaseProvider.getDatabase().chromatogramDao()
                        val entity = ChromatogramEntity(
                            sampleId = 0, // standalone, no sample yet
                            sourceType = SourceType.PHOTO,
                            filePath = currentImagePath,
                            timeRangeStart = signal.points.firstOrNull()?.time?.toDouble(),
                            timeRangeEnd = signal.points.lastOrNull()?.time?.toDouble(),
                            intensityUnit = signal.intensityUnit,
                            qualityScore = digitizationReport?.overall?.score,
                            dataPoints = kotlinx.serialization.json.Json.encodeToString(
                                kotlinx.serialization.builtins.ListSerializer(
                                    com.chromalab.feature.processing.signal.GraphPoint.serializer(),
                                ),
                                signal.points,
                            ),
                            createdAt = now,
                            updatedAt = now,
                        )
                        savedSignalId = dao.insert(entity)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Fallback: navigate with timestamp-based ID
                        savedSignalId = now
                    }
                } else {
                    savedSignalId = now
                }
            }
        }
    }
    val goBack = { step: ProcessingStep ->
        val prev = step.prev()
        if (prev != null) currentStep = prev else onCancel()
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Progress header — always visible
        StepProgressHeader(currentStep)

        // Step content — routed to real screens
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState.index > initialState.index) {
                        slideInHorizontally { it / 3 } + fadeIn() togetherWith
                            slideOutHorizontally { -it / 3 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                            slideOutHorizontally { it / 3 } + fadeOut()
                    }
                },
                label = "step_transition",
            ) { step ->
                when (step) {
                    ProcessingStep.IMAGE_QUALITY -> {
                        val report = qualityReport
                        if (report != null) {
                            ImageQualityScreen(
                                report = report,
                                onProceed = { advance(step) },
                                onRetake = { onCancel() },
                                onBack = { goBack(step) },
                            )
                        } else {
                            ProcessingIndicator("Анализ качества изображения...")
                        }
                    }

                    ProcessingStep.CROP_REVIEW -> {
                        val result = cropResult
                        if (result != null) {
                            CropReviewScreen(
                                cropResult = result,
                                onAccept = { advance(step) },
                                onRecrop = {
                                    cropResult = null
                                    perspectiveResult = null  // perspective depends on crop
                                    documentBounds = null
                                },
                                onBack = { goBack(step) },
                            )
                        } else {
                            ProcessingIndicator("Определение границ документа...")
                        }
                    }

                    ProcessingStep.PERSPECTIVE -> {
                        val result = perspectiveResult
                        if (result != null) {
                            PerspectiveReviewScreen(
                                result = result,
                                onAccept = { advance(step) },
                                onAdjustCorners = {
                                    perspectiveResult = null
                                },
                                onBack = { goBack(step) },
                            )
                        } else {
                            ProcessingIndicator("Коррекция перспективы...")
                        }
                    }

                    ProcessingStep.GRAPH_SELECTION -> {
                        val result = graphResult
                        if (result != null) {
                            GraphSelectionScreen(
                                imagePath = currentImagePath,
                                result = result,
                                onSelect = { selection ->
                                    selectedRegion = selection.region
                                    advance(step)
                                },
                                onEditRoi = { currentStep = ProcessingStep.GRAPH_ROI },
                                onBack = { goBack(step) },
                            )
                        } else {
                            ProcessingIndicator("Поиск графиков на изображении...")
                        }
                    }

                    ProcessingStep.GRAPH_ROI -> {
                        val result = graphResult
                        if (result != null) {
                            GraphRoiEditorScreen(
                                imagePath = currentImagePath,
                                autoResult = result,
                                onAccept = { region ->
                                    selectedRegion = region
                                    advance(step)
                                },
                                onBack = { goBack(step) },
                            )
                        } else {
                            ProcessingIndicator("Поиск графиков...")
                        }
                    }

                    ProcessingStep.AXIS_DETECTION -> {
                        val result = axesResult
                        if (result != null) {
                            AxisEditorScreen(
                                imagePath = currentImagePath,
                                graphRegion = selectedRegion,
                                autoResult = result,
                                onAccept = { axes ->
                                    axesResult = axes
                                    advance(step)
                                },
                                onBack = { goBack(step) },
                            )
                        } else {
                            ProcessingIndicator("Определение осей...")
                        }
                    }

                    ProcessingStep.X_CALIBRATION -> {
                        val axes = axesResult ?: fallbackAxesResult()
                        XCalibrationScreen(
                            imagePath = currentImagePath,
                            graphRegion = selectedRegion,
                            axes = axes,
                            onAccept = { cal ->
                                xCalibration = cal
                                advance(step)
                            },
                            onBack = { goBack(step) },
                        )
                    }

                    ProcessingStep.Y_CALIBRATION -> {
                        val axes = axesResult ?: fallbackAxesResult()
                        YCalibrationScreen(
                            imagePath = currentImagePath,
                            graphRegion = selectedRegion,
                            axes = axes,
                            onAccept = { cal ->
                                yCalibration = cal
                                // Build unified PixelCalibration from X + Y
                                val xCal = xCalibration
                                if (xCal != null) {
                                    val origin = axesResult?.origin
                                    pixelCalibration = PixelCalibration.from(
                                        xAxis = xCal,
                                        yAxis = cal,
                                        originPixelX = origin?.x ?: 0f,
                                        originPixelY = origin?.y ?: imageHeight.toFloat(),
                                    )
                                }
                                advance(step)
                            },
                            onBack = { goBack(step) },
                        )
                    }

                    ProcessingStep.OCR_SUGGESTION -> {
                        val result = ocrResult
                        if (result != null) {
                            OcrSuggestionScreen(
                                ocrResult = result,
                                onAccept = { accepted ->
                                    ocrResult = accepted
                                    advance(step)
                                },
                                onSkip = { advance(step) },
                                onBack = { goBack(step) },
                            )
                        } else {
                            ProcessingIndicator("Распознавание подписей осей...")
                        }
                    }

                    ProcessingStep.CURVE_EXTRACTION -> {
                        val result = curveExtractionResult
                        if (result != null) {
                            CurveOverlayScreen(
                                graphImagePath = currentImagePath,
                                result = result,
                                graphWidth = selectedRegion.width,
                                graphHeight = selectedRegion.height,
                                onAccept = { advance(step) },
                                onBack = { goBack(step) },
                            )
                        } else {
                            ProcessingIndicator("Извлечение кривой хроматограммы...")
                        }
                    }

                    ProcessingStep.CURVE_EDITOR -> {
                        CurveEditorScreen(
                            graphImagePath = currentImagePath,
                            initialPoints = curvePoints,
                            graphWidth = selectedRegion.width,
                            graphHeight = selectedRegion.height,
                            onAccept = { points, _ ->
                                curvePoints = points
                                advance(step)
                            },
                            onBack = { goBack(step) },
                        )
                    }

                    ProcessingStep.SIGNAL_PREVIEW -> {
                        val signal = smoothedSignal
                        if (signal != null) {
                            SignalPreviewScreen(
                                smoothedSignal = signal,
                                graphImagePath = currentImagePath,
                                graphWidth = selectedRegion.width,
                                graphHeight = selectedRegion.height,
                                onAccept = { advance(step) },
                                onBack = { goBack(step) },
                            )
                        } else {
                            ProcessingIndicator("Подготовка цифрового сигнала...")
                        }
                    }

                    ProcessingStep.QUALITY_REPORT -> {
                        val report = digitizationReport
                        if (report != null) {
                            QualityReportContent(
                                report = report,
                                onAccept = { advance(step) },
                                onBack = { goBack(step) },
                            )
                        } else {
                            ProcessingIndicator("Формирование отчёта о качестве...")
                        }
                    }

                    ProcessingStep.EXPORT -> {
                        val signal = smoothedSignal?.smoothed
                        if (signal != null) {
                            val cal = pixelCalibration ?: fallbackCalibration(
                                imageWidth, imageHeight,
                            )
                            val bundle = ExportBundle(
                                signal = signal,
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
                                signal = signal,
                                bundle = bundle,
                                sessionWriter = writer,
                                onBack = { goBack(step) },
                            )
                        } else {
                            StepPlaceholder(
                                step = step,
                                onAccept = { advance(step) },
                                onBack = { goBack(step) },
                            )
                        }
                    }
                }
            }

            // Loading overlay during processing
            if (isProcessing) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Error recovery overlay
            val error = processingError
            if (error != null && !isProcessing) {
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
                                "Ошибка обработки",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                OutlinedButton(onClick = { goBack(currentStep) }) {
                                    Text("Назад")
                                }
                                Button(onClick = {
                                    processingError = null
                                    // Re-trigger LaunchedEffect by toggling step
                                    val step = currentStep
                                    currentStep = ProcessingStep.FIRST
                                    currentStep = step
                                }) {
                                    Text("Повторить")
                                }
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
    }
}

/**
 * Loading indicator shown while a processor runs.
 */
@Composable
private fun ProcessingIndicator(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Fallback placeholder for steps not yet fully wired.
 */
@Composable
private fun StepPlaceholder(
    step: ProcessingStep,
    onAccept: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        bottomBar = {
            StepBottomBar(
                onAccept = onAccept,
                onBack = onBack,
                acceptLabel = if (step.next() != null) "Принять" else "Завершить",
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(step.label, style = MaterialTheme.typography.headlineSmall)
            Text(
                "Будет реализовано в следующей фазе",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Fallback data when processing fails or isn't applicable ────────

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

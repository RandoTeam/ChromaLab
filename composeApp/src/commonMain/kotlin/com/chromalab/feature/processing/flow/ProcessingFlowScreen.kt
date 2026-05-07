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
import com.chromalab.feature.processing.signal.SignalPreviewScreen
import com.chromalab.feature.processing.signal.SmoothedSignal
import com.chromalab.feature.processing.signal.SmoothingParams
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.signal.GraphPoint
import com.chromalab.feature.processing.signal.SignalMetadata
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.processing.normalize.ImageNormalizer
import com.chromalab.feature.processing.normalize.NormalizedImageResult
import com.chromalab.feature.processing.preprocess.PreprocessingResult
import kotlinx.coroutines.Dispatchers
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
    onFinish: () -> Unit,
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
    var curveExtractionResult by remember { mutableStateOf<CurveExtractionResult?>(null) }
    var curvePoints by remember { mutableStateOf<List<CurvePoint>>(emptyList()) }
    var smoothedSignal by remember { mutableStateOf<SmoothedSignal?>(null) }

    // --- Run real processing on step entry ---
    LaunchedEffect(currentStep) {
        isProcessing = true
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
                            val points = curvePoints.mapIndexed { i, cp ->
                                GraphPoint(
                                    index = i, pixelX = cp.pixelX, pixelY = cp.pixelY,
                                    time = cp.pixelX.toFloat(),
                                    intensity = cp.pixelY,
                                    confidence = cp.confidence,
                                    isInterpolated = false,
                                )
                            }
                            val signal = DigitalSignal(
                                points = points, timeUnit = "px", intensityUnit = "px",
                                metadata = SignalMetadata(
                                    currentImagePath, points.size, 0, 0, true,
                                    System.currentTimeMillis(),
                                ),
                            )
                            smoothedSignal = SmoothedSignal(
                                raw = signal, smoothed = signal,
                                params = SmoothingParams(),
                            )
                        }
                    }

                    else -> { /* no async processing needed */ }
                }
            }
        } catch (e: Exception) {
            // Processing failed — user can still proceed with fallback data
            e.printStackTrace()
        }
        isProcessing = false
    }

    val advance = { step: ProcessingStep ->
        val next = step.next()
        if (next != null) currentStep = next else onFinish()
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

                    ProcessingStep.QUALITY_REPORT -> StepPlaceholder(
                        step = step,
                        onAccept = { advance(step) },
                        onBack = { goBack(step) },
                    )

                    ProcessingStep.EXPORT -> StepPlaceholder(
                        step = step,
                        onAccept = { advance(step) },
                        onBack = { goBack(step) },
                    )
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

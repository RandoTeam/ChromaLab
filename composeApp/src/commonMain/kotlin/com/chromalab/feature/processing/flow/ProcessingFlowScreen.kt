package com.chromalab.feature.processing.flow

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.axis.AxisEditorScreen
import com.chromalab.feature.processing.axis.AxisLine
import com.chromalab.feature.processing.axis.AxisOrigin
import com.chromalab.feature.processing.calibration.XAxisCalibration
import com.chromalab.feature.processing.calibration.XCalibrationScreen
import com.chromalab.feature.processing.calibration.YAxisCalibration
import com.chromalab.feature.processing.calibration.YCalibrationScreen
import com.chromalab.feature.processing.crop.CropResult
import com.chromalab.feature.processing.crop.CropRect
import com.chromalab.feature.processing.crop.CropReviewScreen
import com.chromalab.feature.processing.curve.CurveEditorScreen
import com.chromalab.feature.processing.curve.CurveExtractionResult
import com.chromalab.feature.processing.curve.CurveOverlayScreen
import com.chromalab.feature.processing.curve.CurvePoint
import com.chromalab.feature.processing.graph.DetectionConfidence
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.graph.GraphRegionResult
import com.chromalab.feature.processing.graph.GraphRoiEditorScreen
import com.chromalab.feature.processing.graph.GraphSelection
import com.chromalab.feature.processing.graph.GraphSelectionScreen
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrSuggestionScreen
import com.chromalab.feature.processing.perspective.PerspectiveCorrectionResult
import com.chromalab.feature.processing.perspective.PerspectiveReviewScreen
import com.chromalab.feature.processing.perspective.HomographyMatrix
import com.chromalab.feature.processing.document.DocumentCorners
import com.chromalab.feature.processing.document.ImagePoint
import com.chromalab.feature.processing.pipeline.DetectionMethod
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

/**
 * Processing flow orchestrator.
 *
 * Manages the step-by-step pipeline with consistent UX:
 * - Routes each step to its real UI screen
 * - Passes intermediate results between steps via FlowState
 * - Backend processing will be wired in Milestone 2 (OpenCV)
 * - Until then, stub data is used for demonstration
 */
@Composable
fun ProcessingFlowScreen(
    imagePath: String,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableStateOf(ProcessingStep.FIRST) }

    // Intermediate results — populated as user advances
    var qualityReport by remember { mutableStateOf(stubQualityReport()) }
    var cropResult by remember { mutableStateOf(stubCropResult(imagePath)) }
    var perspectiveResult by remember { mutableStateOf(stubPerspectiveResult(imagePath)) }
    var graphResult by remember { mutableStateOf(stubGraphResult(imagePath)) }
    var selectedRegion by remember { mutableStateOf(graphResult.effectiveRegion) }
    var axesResult by remember { mutableStateOf(stubAxesResult()) }
    var xCalibration by remember { mutableStateOf<XAxisCalibration?>(null) }
    var yCalibration by remember { mutableStateOf<YAxisCalibration?>(null) }
    var ocrResult by remember { mutableStateOf(stubOcrResult()) }
    var curvePoints by remember { mutableStateOf(stubCurvePoints()) }

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
            modifier = Modifier.weight(1f),
        ) { step ->
            when (step) {
                ProcessingStep.IMAGE_QUALITY -> ImageQualityScreen(
                    report = qualityReport,
                    onProceed = { advance(step) },
                    onRetake = { onCancel() },
                    onBack = { goBack(step) },
                )

                ProcessingStep.CROP_REVIEW -> CropReviewScreen(
                    cropResult = cropResult,
                    onAccept = { advance(step) },
                    onRecrop = { /* manual crop — will be wired with OpenCV */ },
                    onBack = { goBack(step) },
                )

                ProcessingStep.PERSPECTIVE -> PerspectiveReviewScreen(
                    result = perspectiveResult,
                    onAccept = { advance(step) },
                    onAdjustCorners = { /* corner editor — will be wired with OpenCV */ },
                    onBack = { goBack(step) },
                )

                ProcessingStep.GRAPH_SELECTION -> GraphSelectionScreen(
                    imagePath = imagePath,
                    result = graphResult,
                    onSelect = { selection ->
                        selectedRegion = selection.region
                        advance(step)
                    },
                    onEditRoi = { /* navigate to ROI editor */ },
                    onBack = { goBack(step) },
                )

                ProcessingStep.GRAPH_ROI -> GraphRoiEditorScreen(
                    imagePath = imagePath,
                    autoResult = graphResult,
                    onAccept = { region ->
                        selectedRegion = region
                        advance(step)
                    },
                    onBack = { goBack(step) },
                )

                ProcessingStep.AXIS_DETECTION -> AxisEditorScreen(
                    imagePath = imagePath,
                    graphRegion = selectedRegion,
                    autoResult = axesResult,
                    onAccept = { result ->
                        axesResult = result
                        advance(step)
                    },
                    onBack = { goBack(step) },
                )

                ProcessingStep.X_CALIBRATION -> XCalibrationScreen(
                    imagePath = imagePath,
                    graphRegion = selectedRegion,
                    axes = axesResult,
                    onAccept = { cal ->
                        xCalibration = cal
                        advance(step)
                    },
                    onBack = { goBack(step) },
                )

                ProcessingStep.Y_CALIBRATION -> YCalibrationScreen(
                    imagePath = imagePath,
                    graphRegion = selectedRegion,
                    axes = axesResult,
                    onAccept = { cal ->
                        yCalibration = cal
                        advance(step)
                    },
                    onBack = { goBack(step) },
                )

                ProcessingStep.OCR_SUGGESTION -> OcrSuggestionScreen(
                    ocrResult = ocrResult,
                    onAccept = { result ->
                        ocrResult = result
                        advance(step)
                    },
                    onSkip = { advance(step) },
                    onBack = { goBack(step) },
                )

                ProcessingStep.CURVE_EXTRACTION -> CurveOverlayScreen(
                    graphImagePath = imagePath,
                    result = stubCurveExtractionResult(),
                    graphWidth = selectedRegion.width,
                    graphHeight = selectedRegion.height,
                    onAccept = { advance(step) },
                    onBack = { goBack(step) },
                )

                ProcessingStep.CURVE_EDITOR -> CurveEditorScreen(
                    graphImagePath = imagePath,
                    initialPoints = curvePoints,
                    graphWidth = selectedRegion.width,
                    graphHeight = selectedRegion.height,
                    onAccept = { points, _ ->
                        curvePoints = points
                        advance(step)
                    },
                    onBack = { goBack(step) },
                )

                ProcessingStep.SIGNAL_PREVIEW -> SignalPreviewScreen(
                    smoothedSignal = stubSmoothedSignal(),
                    graphImagePath = imagePath,
                    graphWidth = selectedRegion.width,
                    graphHeight = selectedRegion.height,
                    onAccept = { advance(step) },
                    onBack = { goBack(step) },
                )

                ProcessingStep.QUALITY_REPORT -> {
                    // Placeholder — quality report at the end
                    StepPlaceholder(
                        step = step,
                        imagePath = imagePath,
                        onAccept = { advance(step) },
                        onBack = { goBack(step) },
                    )
                }

                ProcessingStep.EXPORT -> {
                    // Placeholder — export requires Room + FileSharer
                    StepPlaceholder(
                        step = step,
                        imagePath = imagePath,
                        onAccept = { advance(step) },
                        onBack = { goBack(step) },
                    )
                }
            }
        }
    }
}

/**
 * Fallback placeholder for steps not yet wired to real screens.
 */
@Composable
private fun StepPlaceholder(
    step: ProcessingStep,
    imagePath: String,
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
                .padding(com.chromalab.core.ui.theme.Spacing.lg),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
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

// ─── Stub data for Milestone 1 (UI demo, no backend yet) ────────────

private fun stubMetric(name: String, score: Float = 0.85f) = QualityMetric(
    name = name, score = score,
    level = if (score > 0.7f) QualityLevel.GOOD else QualityLevel.ACCEPTABLE,
    message = if (score > 0.7f) "В пределах нормы" else "Требует внимания",
)

private fun stubQualityReport() = ImageQualityReport(
    blurScore = stubMetric("Резкость", 0.82f),
    brightnessScore = stubMetric("Яркость", 0.78f),
    contrastScore = stubMetric("Контраст", 0.85f),
    glareScore = stubMetric("Блики", 0.90f),
    shadowScore = stubMetric("Освещение", 0.88f),
    frameFillScore = stubMetric("Заполнение", 0.75f),
    skewScore = stubMetric("Перекос", 0.92f),
    overallLevel = QualityLevel.GOOD,
    warnings = listOf("Рекомендуется лучшее освещение"),
    timestamp = System.currentTimeMillis(),
)

private fun stubCropResult(path: String) = CropResult(
    croppedPath = path, sourcePath = path,
    sourceWidth = 1920, sourceHeight = 1080,
    cropRect = CropRect(100, 50, 1720, 980),
    croppedWidth = 1720, croppedHeight = 980,
    timestamp = System.currentTimeMillis(),
)

private fun stubPerspectiveResult(path: String) = PerspectiveCorrectionResult(
    correctedPath = path, sourcePath = path,
    homography = HomographyMatrix(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)),
    sourceCorners = DocumentCorners(
        topLeft = ImagePoint(0f, 0f), topRight = ImagePoint(1920f, 0f),
        bottomLeft = ImagePoint(0f, 1080f), bottomRight = ImagePoint(1920f, 1080f),
    ),
    outputWidth = 1920, outputHeight = 1080,
    maxWarpDistance = 5f, correctedAspectRatio = 1.78f,
    isExcessiveWarp = false, timestamp = System.currentTimeMillis(),
)

private fun stubGraphResult(path: String) = GraphRegionResult(
    regions = listOf(
        GraphRegion(100, 100, 1600, 400, "Ионный канал 1"),
        GraphRegion(100, 550, 1600, 400, "Ионный канал 2"),
    ),
    selectedIndex = 0,
    detectionMethod = DetectionMethod.AUTO,
    confidence = DetectionConfidence.MEDIUM,
    imageWidth = 1920, imageHeight = 1080,
    graphImagePath = path,
    warnings = listOf("Обнаружено 2 графика — выберите нужный"),
    timestamp = System.currentTimeMillis(),
)

private fun stubAxesResult() = AxesResult(
    xAxis = AxisLine(100f, 480f, 1700f, 480f),
    yAxis = AxisLine(100f, 100f, 100f, 480f),
    origin = AxisOrigin(100f, 480f),
    detectionMethod = DetectionMethod.AUTO,
    confidence = 0.75f,
    timestamp = System.currentTimeMillis(),
)

private fun stubOcrResult() = AxisOcrResult(
    rawElements = emptyList(),
    suggestedXValues = listOf(35.0f, 40.0f, 45.0f, 50.0f, 55.0f, 60.0f, 65.0f),
    suggestedYValues = listOf(0.0f, 50.0f, 100.0f, 150.0f, 200.0f, 250.0f, 300.0f, 350.0f),
    xUnit = "мин",
    yUnit = "mAU",
    timestamp = System.currentTimeMillis(),
)

private fun stubCurvePoints(): List<CurvePoint> {
    val n = 200
    return (0 until n).map { i ->
        val x = (i.toFloat() / n * 1600f).toInt()
        val y = 200f + 100f * kotlin.math.sin(x / 100f).toFloat()
        CurvePoint(pixelX = x, pixelY = y, confidence = 0.9f)
    }
}

private fun stubCurveExtractionResult() = CurveExtractionResult(
    points = stubCurvePoints(),
    maskImagePath = null,
    totalColumns = 1600,
    extractedColumns = 200,
    interpolatedColumns = 0,
    outlierCount = 0,
    warnings = emptyList(),
    timestamp = System.currentTimeMillis(),
)

private fun stubSmoothedSignal(): SmoothedSignal {
    val n = 200
    val points = (0 until n).map { i ->
        GraphPoint(
            index = i, pixelX = i * 8, pixelY = 0f,
            time = 35f + i * 0.15f,
            intensity = 100f + 150f * kotlin.math.exp(
                -((i - 100f) * (i - 100f)) / 500f,
            ).toFloat(),
            confidence = 1f, isInterpolated = false,
        )
    }
    val signal = DigitalSignal(
        points = points, timeUnit = "мин", intensityUnit = "mAU",
        metadata = SignalMetadata("stub", n, 0, 0, true, System.currentTimeMillis()),
    )
    return SmoothedSignal(raw = signal, smoothed = signal, params = SmoothingParams())
}


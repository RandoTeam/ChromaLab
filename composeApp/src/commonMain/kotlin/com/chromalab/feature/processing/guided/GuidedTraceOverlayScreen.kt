package com.chromalab.feature.processing.guided

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chromalab.core.common.AppLanguage
import com.chromalab.core.common.Strings
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.processing.graph.GraphRegion
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val TRACE_MIN_ZOOM = 1f
private const val TRACE_MAX_ZOOM = 10f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedTraceOverlayScreen(
    imagePath: String,
    guidedState: GuidedDigitizationState,
    sourceTraceId: String,
    tracePoints: List<TraceOverlayPoint>,
    qualitySummary: TraceQualitySummary,
    userProvenance: GuidedUserProvenance,
    onGuidedStateChange: (GuidedDigitizationState) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    overlayArtifactPath: String? = null,
    maskArtifactPath: String? = null,
    centerlineArtifactPath: String? = null,
    warnings: List<String> = emptyList(),
) {
    val imageWidth = guidedState.image?.width
        ?: guidedState.graphPanelConfirmation?.confirmedGraphPanel?.bounds?.right
        ?: guidedState.plotAreaConfirmation?.confirmedPlotArea?.bounds?.right
        ?: 1
    val imageHeight = guidedState.image?.height
        ?: guidedState.graphPanelConfirmation?.confirmedGraphPanel?.bounds?.bottom
        ?: guidedState.plotAreaConfirmation?.confirmedPlotArea?.bounds?.bottom
        ?: 1
    val graphPanel = guidedState.graphPanelConfirmation?.confirmedGraphPanel?.bounds
    val plotArea = guidedState.plotAreaConfirmation?.confirmedPlotArea?.bounds
    var snapshot by remember(guidedState.stateId, imagePath, sourceTraceId) {
        mutableStateOf(
            GuidedTraceOverlayReducer.initialSnapshot(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                graphPanelBounds = graphPanel,
                plotAreaBounds = plotArea,
                sourceTraceId = sourceTraceId,
                tracePoints = tracePoints,
                qualitySummary = qualitySummary,
                calibrationSetId = guidedState.calibration?.calibrationSet?.calibrationSetId,
                overlayArtifactPath = overlayArtifactPath,
                maskArtifactPath = maskArtifactPath,
                centerlineArtifactPath = centerlineArtifactPath,
                warnings = warnings,
            ),
        )
    }
    val strings = guidedTraceOverlayStrings()
    val colors = guidedTraceOverlayColors()

    Scaffold(
        modifier = modifier.background(colors.background),
        topBar = {
            TopAppBar(
                title = { Text(strings.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.panelSurface),
                modifier = Modifier.statusBarsPadding(),
            )
        },
        bottomBar = {
            GuidedTraceBottomBar(
                snapshot = snapshot,
                onSnapshotChange = { snapshot = it },
                onAcceptValid = {
                    onGuidedStateChange(
                        GuidedTraceOverlayReducer.acceptTrace(
                            state = guidedState,
                            snapshot = snapshot,
                            userProvenance = userProvenance,
                            timestampEpochMillis = System.currentTimeMillis(),
                            reviewGrade = false,
                        ),
                    )
                },
                onAcceptReview = {
                    onGuidedStateChange(
                        GuidedTraceOverlayReducer.acceptTrace(
                            state = guidedState,
                            snapshot = snapshot,
                            userProvenance = userProvenance,
                            timestampEpochMillis = System.currentTimeMillis(),
                            reviewGrade = true,
                        ),
                    )
                },
                onReject = {
                    onGuidedStateChange(
                        GuidedTraceOverlayReducer.rejectTrace(
                            state = guidedState,
                            snapshot = snapshot,
                            userProvenance = userProvenance,
                            timestampEpochMillis = System.currentTimeMillis(),
                            reason = "user_rejected_trace_overlay",
                        ),
                    )
                },
                strings = strings,
                colors = colors,
            )
        },
        containerColor = colors.background,
    ) { padding ->
        GuidedTraceOverlayContent(
            imagePath = imagePath,
            snapshot = snapshot,
            strings = strings,
            colors = colors,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@Composable
fun GuidedTraceOverlayContent(
    imagePath: String,
    snapshot: TraceOverlayEditorSnapshot,
    strings: GuidedTraceOverlayStrings = guidedTraceOverlayStrings(),
    colors: GuidedTraceOverlayColors = guidedTraceOverlayColors(),
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.background(colors.background)) {
        GuidedTraceInstructionBar(
            evaluation = snapshot.evaluation,
            strings = strings,
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
        )
        GuidedTraceImageOverlay(
            imagePath = imagePath,
            snapshot = snapshot,
            strings = strings,
            colors = colors,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

@Composable
private fun GuidedTraceInstructionBar(
    evaluation: TraceOverlayEvaluation,
    strings: GuidedTraceOverlayStrings,
    colors: GuidedTraceOverlayColors,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = colors.panelSurface,
        tonalElevation = 2.dp,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TraceStatusChip(evaluation.qualityStatus, strings, colors)
                AssistChip(
                    onClick = {},
                    label = { Text("${strings.points}: ${evaluation.qualitySummary.pointCount ?: 0}") },
                )
                evaluation.qualitySummary.columnCoverageRatio?.let { coverage ->
                    AssistChip(
                        onClick = {},
                        label = { Text("${strings.coverage}: ${coverage.renderPercent()}") },
                    )
                }
            }
            Text(
                text = strings.instructions,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GuidedTraceImageOverlay(
    imagePath: String,
    snapshot: TraceOverlayEditorSnapshot,
    strings: GuidedTraceOverlayStrings,
    colors: GuidedTraceOverlayColors,
    modifier: Modifier = Modifier,
) {
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(snapshot.imageWidth, snapshot.imageHeight) {
        zoom = 1f
        pan = Offset.Zero
    }

    val transform = remember(viewportSize, snapshot.imageWidth, snapshot.imageHeight, zoom, pan) {
        TraceViewportTransform(
            viewportWidth = viewportSize.width.toFloat(),
            viewportHeight = viewportSize.height.toFloat(),
            imageWidth = snapshot.imageWidth.toFloat().coerceAtLeast(1f),
            imageHeight = snapshot.imageHeight.toFloat().coerceAtLeast(1f),
            zoom = zoom,
            pan = pan,
        )
    }

    Box(
        modifier = modifier
            .background(colors.imageBackground)
            .onSizeChanged { viewportSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    zoom = (zoom * zoomChange).coerceIn(TRACE_MIN_ZOOM, TRACE_MAX_ZOOM)
                    pan += panChange
                }
            }
            .semantics {
                contentDescription = strings.imageContentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imagePath,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .offset {
                    IntOffset(
                        transform.imageLeft.roundToInt(),
                        transform.imageTop.roundToInt(),
                    )
                }
                .size(
                    width = transform.imageDrawWidth.dpFromPx(),
                    height = transform.imageDrawHeight.dpFromPx(),
                ),
        )
        Canvas(Modifier.fillMaxSize()) {
            snapshot.graphPanelBounds?.let { region ->
                val rect = transform.regionToViewRect(region)
                drawRect(
                    color = colors.graphPanel,
                    topLeft = Offset(rect.left, rect.top),
                    size = rect.size,
                    style = Stroke(width = 1.4.dp.toPx()),
                )
            }
            snapshot.plotAreaBounds?.let { region ->
                val rect = transform.regionToViewRect(region)
                drawRect(
                    color = colors.plotArea.copy(alpha = 0.10f),
                    topLeft = Offset(rect.left, rect.top),
                    size = rect.size,
                )
                drawRect(
                    color = colors.plotArea,
                    topLeft = Offset(rect.left, rect.top),
                    size = rect.size,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
            drawTraceOverlay(snapshot.tracePoints, transform, colors.trace)
        }
    }
}

@Composable
private fun GuidedTraceBottomBar(
    snapshot: TraceOverlayEditorSnapshot,
    onSnapshotChange: (TraceOverlayEditorSnapshot) -> Unit,
    onAcceptValid: () -> Unit,
    onAcceptReview: () -> Unit,
    onReject: () -> Unit,
    strings: GuidedTraceOverlayStrings,
    colors: GuidedTraceOverlayColors,
) {
    val evaluation = snapshot.evaluation
    Surface(
        color = colors.panelSurface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            TraceWarnings(evaluation, strings)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { onSnapshotChange(GuidedTraceOverlayReducer.resetToSuggestion(snapshot)) },
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = 0.dp),
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(strings.reset)
                }
                OutlinedButton(
                    onClick = onReject,
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = 0.dp),
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(strings.reject)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onAcceptReview,
                    enabled = evaluation.canAcceptReview,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.review,
                        contentColor = Color.Black,
                    ),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(strings.acceptReview)
                }
                Button(
                    onClick = onAcceptValid,
                    enabled = evaluation.canAcceptValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.valid,
                        contentColor = Color.Black,
                    ),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(strings.acceptValid)
                }
            }
            ElevatedAssistChip(
                onClick = {},
                enabled = false,
                label = { Text(strings.nextPeakReviewDisabled) },
            )
        }
    }
}

@Composable
private fun TraceWarnings(
    evaluation: TraceOverlayEvaluation,
    strings: GuidedTraceOverlayStrings,
) {
    val issues = evaluation.issues
    if (issues.isEmpty()) {
        Text(
            text = strings.noWarnings,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        issues.take(4).forEach { issue ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    tint = when (issue.severity) {
                        GuidedRoiValidationSeverity.INFO -> MaterialTheme.colorScheme.primary
                        GuidedRoiValidationSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
                        GuidedRoiValidationSeverity.ERROR -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = issue.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TraceStatusChip(
    status: TraceQualityStatus,
    strings: GuidedTraceOverlayStrings,
    colors: GuidedTraceOverlayColors,
) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                when (status) {
                    TraceQualityStatus.VALID -> strings.valid
                    TraceQualityStatus.REVIEW -> strings.review
                    TraceQualityStatus.INVALID -> strings.invalid
                    TraceQualityStatus.MISSING -> strings.missing
                },
            )
        },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(status.statusColor(colors), CircleShape),
            )
        },
    )
}

data class GuidedTraceOverlayStrings(
    val title: String,
    val back: String,
    val instructions: String,
    val imageContentDescription: String,
    val valid: String,
    val review: String,
    val invalid: String,
    val missing: String,
    val points: String,
    val coverage: String,
    val reset: String,
    val reject: String,
    val acceptReview: String,
    val acceptValid: String,
    val noWarnings: String,
    val nextPeakReviewDisabled: String,
)

@Composable
fun guidedTraceOverlayStrings(): GuidedTraceOverlayStrings =
    if (Strings.language == AppLanguage.EN) {
        GuidedTraceOverlayStrings(
            title = "Trace overlay",
            back = "Back",
            instructions = "Review the extracted trace over the confirmed plot area. Accept only if it follows the signal.",
            imageContentDescription = "Chromatogram image with extracted trace overlay",
            valid = "VALID",
            review = "REVIEW",
            invalid = "INVALID",
            missing = "Missing",
            points = "Points",
            coverage = "Coverage",
            reset = "Reset",
            reject = "Reject",
            acceptReview = "Accept review",
            acceptValid = "Accept valid",
            noWarnings = "No trace warnings.",
            nextPeakReviewDisabled = "Next: peak review (Phase 5)",
        )
    } else {
        GuidedTraceOverlayStrings(
            title = "\u041F\u043E\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043D\u0438\u0435 trace",
            back = "\u041D\u0430\u0437\u0430\u0434",
            instructions = "\u041F\u0440\u043E\u0432\u0435\u0440\u044C\u0442\u0435, \u0447\u0442\u043E \u043B\u0438\u043D\u0438\u044F trace \u0441\u043E\u0432\u043F\u0430\u0434\u0430\u0435\u0442 \u0441 \u0441\u0438\u0433\u043D\u0430\u043B\u043E\u043C \u0432\u043D\u0443\u0442\u0440\u0438 plotArea.",
            imageContentDescription = "\u0425\u0440\u043E\u043C\u0430\u0442\u043E\u0433\u0440\u0430\u043C\u043C\u0430 \u0441 \u043D\u0430\u043B\u043E\u0436\u0435\u043D\u043D\u044B\u043C trace",
            valid = "VALID",
            review = "REVIEW",
            invalid = "INVALID",
            missing = "\u041D\u0435\u0442 \u0434\u0430\u043D\u043D\u044B\u0445",
            points = "\u0422\u043E\u0447\u043A\u0438",
            coverage = "\u041F\u043E\u043A\u0440\u044B\u0442\u0438\u0435",
            reset = "\u0421\u0431\u0440\u043E\u0441",
            reject = "\u041E\u0442\u043A\u043B\u043E\u043D\u0438\u0442\u044C",
            acceptReview = "\u041F\u0440\u0438\u043D\u044F\u0442\u044C REVIEW",
            acceptValid = "\u041F\u0440\u0438\u043D\u044F\u0442\u044C VALID",
            noWarnings = "\u041F\u0440\u0435\u0434\u0443\u043F\u0440\u0435\u0436\u0434\u0435\u043D\u0438\u0439 trace \u043D\u0435\u0442.",
            nextPeakReviewDisabled = "\u0414\u0430\u043B\u0435\u0435: \u043F\u0440\u043E\u0432\u0435\u0440\u043A\u0430 \u043F\u0438\u043A\u043E\u0432 (Phase 5)",
        )
    }

data class GuidedTraceOverlayColors(
    val background: Color,
    val panelSurface: Color,
    val imageBackground: Color,
    val graphPanel: Color,
    val plotArea: Color,
    val trace: Color,
    val valid: Color,
    val review: Color,
    val invalid: Color,
)

@Composable
fun guidedTraceOverlayColors(): GuidedTraceOverlayColors =
    GuidedTraceOverlayColors(
        background = MaterialTheme.colorScheme.background,
        panelSurface = MaterialTheme.colorScheme.surface,
        imageBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
        graphPanel = Color(0xFF83D8FF),
        plotArea = Color(0xFF9BE58A),
        trace = Color(0xFFFF6B9A),
        valid = Color(0xFF9BE58A),
        review = Color(0xFFFFD166),
        invalid = MaterialTheme.colorScheme.error,
    )

private data class TraceViewportTransform(
    val viewportWidth: Float,
    val viewportHeight: Float,
    val imageWidth: Float,
    val imageHeight: Float,
    val zoom: Float,
    val pan: Offset,
) {
    private val baseScale: Float =
        min(viewportWidth / imageWidth, viewportHeight / imageHeight)
            .takeIf { it.isFinite() && it > 0f } ?: 1f

    val imageScale: Float = baseScale * zoom
    val imageDrawWidth: Float = imageWidth * imageScale
    val imageDrawHeight: Float = imageHeight * imageScale
    val imageLeft: Float = (viewportWidth - imageWidth * baseScale) / 2f + pan.x
    val imageTop: Float = (viewportHeight - imageHeight * baseScale) / 2f + pan.y

    fun pointToView(point: TraceOverlayPoint): Offset =
        Offset(
            x = imageLeft + point.x * imageScale,
            y = imageTop + point.y * imageScale,
        )

    fun regionToViewRect(region: GraphRegion): TraceViewRect =
        TraceViewRect(
            left = imageLeft + region.x * imageScale,
            top = imageTop + region.y * imageScale,
            width = region.width * imageScale,
            height = region.height * imageScale,
        )
}

private data class TraceViewRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val size: Size get() = Size(max(0f, width), max(0f, height))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTraceOverlay(
    points: List<TraceOverlayPoint>,
    transform: TraceViewportTransform,
    color: Color,
) {
    if (points.isEmpty()) return
    val path = Path()
    points.forEachIndexed { index, point ->
        val offset = transform.pointToView(point)
        if (index == 0) {
            path.moveTo(offset.x, offset.y)
        } else {
            path.lineTo(offset.x, offset.y)
        }
    }
    drawPath(path, color, style = Stroke(width = 2.6.dp.toPx()))
    val stride = (points.size / 80).coerceAtLeast(1)
    points.forEachIndexed { index, point ->
        if (index % stride == 0) {
            val center = transform.pointToView(point)
            drawCircle(
                color = color.copy(alpha = 0.72f),
                radius = 2.4.dp.toPx(),
                center = center,
            )
        }
    }
}

private fun TraceQualityStatus.statusColor(colors: GuidedTraceOverlayColors): Color =
    when (this) {
        TraceQualityStatus.VALID -> colors.valid
        TraceQualityStatus.REVIEW -> colors.review
        TraceQualityStatus.INVALID -> colors.invalid
        TraceQualityStatus.MISSING -> colors.review.copy(alpha = 0.72f)
    }

private fun Double.renderPercent(): String =
    "${(this * 100.0).roundToInt()}%"

@Composable
private fun Float.dpFromPx(): androidx.compose.ui.unit.Dp =
    with(LocalDensity.current) { this@dpFromPx.toDp() }

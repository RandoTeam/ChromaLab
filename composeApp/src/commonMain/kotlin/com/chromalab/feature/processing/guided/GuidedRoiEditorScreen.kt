package com.chromalab.feature.processing.guided

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.processing.graph.GraphRegion
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 8f
private const val MIN_REGION_SIZE_PX = 4
private val HandleTouchTarget = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedRoiEditorScreen(
    imagePath: String,
    guidedState: GuidedDigitizationState,
    graphPanelSuggestion: GraphRegion?,
    plotAreaSuggestion: GraphRegion?,
    userProvenance: GuidedUserProvenance,
    onGuidedStateChange: (GuidedDigitizationState) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val imageWidth = guidedState.image?.width ?: graphPanelSuggestion?.right ?: plotAreaSuggestion?.right ?: 1
    val imageHeight = guidedState.image?.height ?: graphPanelSuggestion?.bottom ?: plotAreaSuggestion?.bottom ?: 1
    var snapshot by remember(guidedState.stateId, imagePath, imageWidth, imageHeight) {
        mutableStateOf(
            GuidedRoiEditorReducer.initialSnapshot(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                graphPanelSuggestion = graphPanelSuggestion,
                plotAreaSuggestion = plotAreaSuggestion,
            ),
        )
    }
    val strings = guidedRoiEditorStrings()
    val colors = guidedRoiEditorColors()

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.panelSurface,
                ),
                modifier = Modifier.statusBarsPadding(),
            )
        },
        bottomBar = {
            GuidedRoiBottomBar(
                snapshot = snapshot,
                onSnapshotChange = { snapshot = it },
                onConfirmGraphPanel = {
                    onGuidedStateChange(
                        GuidedRoiEditorReducer.confirmGraphPanel(
                            state = guidedState,
                            snapshot = snapshot,
                            userProvenance = userProvenance,
                            timestampEpochMillis = System.currentTimeMillis(),
                        ),
                    )
                    snapshot = snapshot.copy(activeStage = GuidedRoiEditorStage.PLOT_AREA)
                },
                onConfirmPlotArea = {
                    onGuidedStateChange(
                        GuidedRoiEditorReducer.confirmPlotArea(
                            state = guidedState,
                            snapshot = snapshot,
                            userProvenance = userProvenance,
                            timestampEpochMillis = System.currentTimeMillis(),
                        ),
                    )
                },
                strings = strings,
                colors = colors,
            )
        },
        containerColor = colors.background,
    ) { padding ->
        GuidedRoiEditorContent(
            imagePath = imagePath,
            snapshot = snapshot,
            onSnapshotChange = { snapshot = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            strings = strings,
            colors = colors,
        )
    }
}

@Composable
fun GuidedRoiEditorContent(
    imagePath: String,
    snapshot: GuidedRoiEditorSnapshot,
    onSnapshotChange: (GuidedRoiEditorSnapshot) -> Unit,
    modifier: Modifier = Modifier,
    strings: GuidedRoiEditorStrings = guidedRoiEditorStrings(),
    colors: GuidedRoiEditorColors = guidedRoiEditorColors(),
) {
    Column(
        modifier = modifier.background(colors.background),
    ) {
        GuidedRoiInstructionBar(
            snapshot = snapshot,
            strings = strings,
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
        )

        GuidedRoiImageEditor(
            imagePath = imagePath,
            snapshot = snapshot,
            onSnapshotChange = onSnapshotChange,
            strings = strings,
            colors = colors,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

@Composable
fun GuidedRoiImageEditor(
    imagePath: String,
    snapshot: GuidedRoiEditorSnapshot,
    onSnapshotChange: (GuidedRoiEditorSnapshot) -> Unit,
    modifier: Modifier = Modifier,
    strings: GuidedRoiEditorStrings = guidedRoiEditorStrings(),
    colors: GuidedRoiEditorColors = guidedRoiEditorColors(),
) {
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(snapshot.imageWidth, snapshot.imageHeight) {
        zoom = 1f
        pan = Offset.Zero
    }

    val transform = remember(viewportSize, snapshot.imageWidth, snapshot.imageHeight, zoom, pan) {
        ImageViewportTransform(
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
            .background(Color.Black)
            .onSizeChanged { viewportSize = it }
            .semantics { contentDescription = strings.zoomHint }
            .pointerInput(snapshot.imageWidth, snapshot.imageHeight) {
                detectTransformGestures { _, panChange, zoomChange, _ ->
                    val nextZoom = (zoom * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
                    zoom = nextZoom
                    pan += panChange
                }
            },
    ) {
        AsyncImage(
            model = imagePath,
            contentDescription = strings.imageDescription,
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

        Canvas(modifier = Modifier.fillMaxSize()) {
            val graphPanelRect = snapshot.graphPanelBounds?.let(transform::regionToViewRect)
            val plotAreaRect = snapshot.plotAreaBounds?.let(transform::regionToViewRect)

            graphPanelRect?.let { rect ->
                drawOutsideScrim(rect, colors.outsideScrim)
            }

            drawGuideGrid(transform, colors.guideLine)

            snapshot.graphPanelBounds?.let { region ->
                val statusColor = snapshot.graphPanelStatus.statusColor(colors)
                val rect = transform.regionToViewRect(region)
                drawRect(
                    color = statusColor.copy(alpha = 0.16f),
                    topLeft = rect.topLeft,
                    size = rect.size,
                )
                drawRect(
                    color = statusColor,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }

            snapshot.plotAreaBounds?.let { region ->
                val statusColor = snapshot.plotAreaStatus.statusColor(colors)
                val rect = transform.regionToViewRect(region)
                drawRect(
                    color = statusColor.copy(alpha = 0.12f),
                    topLeft = rect.topLeft,
                    size = rect.size,
                )
                drawRect(
                    color = statusColor,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }

            plotAreaRect?.let { rect ->
                drawLine(colors.plotArea, rect.topLeft, rect.bottomRight, strokeWidth = 1.dp.toPx())
                drawLine(colors.plotArea, Offset(rect.left, rect.bottom), Offset(rect.right, rect.top), strokeWidth = 1.dp.toPx())
            }
        }

        val activeRegion = when (snapshot.activeStage) {
            GuidedRoiEditorStage.GRAPH_PANEL -> snapshot.graphPanelBounds
            GuidedRoiEditorStage.PLOT_AREA -> snapshot.plotAreaBounds
        }
        activeRegion?.let { region ->
            GuidedRoiDragLayer(
                snapshot = snapshot,
                region = region,
                transform = transform,
                onSnapshotChange = onSnapshotChange,
                strings = strings,
                colors = colors,
            )
        }
    }
}

@Composable
private fun GuidedRoiInstructionBar(
    snapshot: GuidedRoiEditorSnapshot,
    strings: GuidedRoiEditorStrings,
    colors: GuidedRoiEditorColors,
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
                GuidedStatusChip(
                    label = strings.graphPanel,
                    status = snapshot.graphPanelStatus,
                    strings = strings,
                    colors = colors,
                    selected = snapshot.activeStage == GuidedRoiEditorStage.GRAPH_PANEL,
                )
                GuidedStatusChip(
                    label = strings.plotArea,
                    status = snapshot.plotAreaStatus,
                    strings = strings,
                    colors = colors,
                    selected = snapshot.activeStage == GuidedRoiEditorStage.PLOT_AREA,
                )
            }
            Text(
                text = if (snapshot.activeStage == GuidedRoiEditorStage.GRAPH_PANEL) {
                    strings.graphPanelHint
                } else {
                    strings.plotAreaHint
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GuidedRoiBottomBar(
    snapshot: GuidedRoiEditorSnapshot,
    onSnapshotChange: (GuidedRoiEditorSnapshot) -> Unit,
    onConfirmGraphPanel: () -> Unit,
    onConfirmPlotArea: () -> Unit,
    strings: GuidedRoiEditorStrings,
    colors: GuidedRoiEditorColors,
) {
    Surface(
        color = colors.panelSurface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            ValidationSummary(snapshot)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = {
                        onSnapshotChange(
                            if (snapshot.activeStage == GuidedRoiEditorStage.GRAPH_PANEL) {
                                GuidedRoiEditorReducer.resetGraphPanel(snapshot)
                            } else {
                                GuidedRoiEditorReducer.resetPlotArea(snapshot)
                            },
                        )
                    },
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = 0.dp),
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(strings.reset)
                }

                Button(
                    onClick = {
                        if (snapshot.activeStage == GuidedRoiEditorStage.GRAPH_PANEL) {
                            onConfirmGraphPanel()
                        } else {
                            onConfirmPlotArea()
                        }
                    },
                    enabled = if (snapshot.activeStage == GuidedRoiEditorStage.GRAPH_PANEL) {
                        snapshot.canConfirmGraphPanel
                    } else {
                        snapshot.canConfirmPlotArea
                    },
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (snapshot.activeStage == GuidedRoiEditorStage.GRAPH_PANEL) {
                            colors.graphPanel
                        } else {
                            colors.plotArea
                        },
                        contentColor = Color.Black,
                    ),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        if (snapshot.activeStage == GuidedRoiEditorStage.GRAPH_PANEL) {
                            strings.confirmGraphPanel
                        } else {
                            strings.confirmPlotArea
                        },
                    )
                }
            }

            ElevatedAssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text("${strings.nextCalibrationAnchors} - ${strings.nextDisabledReason}")
                },
            )
        }
    }
}

@Composable
private fun ValidationSummary(snapshot: GuidedRoiEditorSnapshot) {
    val issues = if (snapshot.activeStage == GuidedRoiEditorStage.GRAPH_PANEL) {
        snapshot.graphPanelValidation.issues
    } else {
        snapshot.plotAreaValidation.issues
    }
    if (issues.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        issues.forEach { issue ->
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
private fun GuidedStatusChip(
    label: String,
    status: GuidedRoiEditorStatus,
    strings: GuidedRoiEditorStrings,
    colors: GuidedRoiEditorColors,
    selected: Boolean,
) {
    val statusText = when (status) {
        GuidedRoiEditorStatus.SUGGESTED -> strings.suggested
        GuidedRoiEditorStatus.USER_CONFIRMED -> strings.confirmed
        GuidedRoiEditorStatus.REVIEW -> strings.review
        GuidedRoiEditorStatus.INVALID -> strings.invalid
    }
    AssistChip(
        onClick = {},
        label = { Text("$label: $statusText") },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(status.statusColor(colors), CircleShape),
            )
        },
        modifier = if (selected) {
            Modifier.border(1.dp, status.statusColor(colors), RoundedCornerShape(8.dp))
        } else {
            Modifier
        },
    )
}

@Composable
private fun GuidedRoiDragLayer(
    snapshot: GuidedRoiEditorSnapshot,
    region: GraphRegion,
    transform: ImageViewportTransform,
    onSnapshotChange: (GuidedRoiEditorSnapshot) -> Unit,
    strings: GuidedRoiEditorStrings,
    colors: GuidedRoiEditorColors,
) {
    val rect = transform.regionToViewRect(region)
    val handlePx = with(LocalDensity.current) { HandleTouchTarget.toPx() }
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (rect.left + rect.width / 2f - handlePx / 2f).roundToInt(),
                    (rect.top + rect.height / 2f - handlePx / 2f).roundToInt(),
                )
            }
            .size(HandleTouchTarget)
            .semantics {
                role = Role.Button
                contentDescription = strings.moveSelection
            }
            .pointerInput(snapshot.activeStage, region) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val dx = (dragAmount.x / transform.imageScale).roundToInt()
                    val dy = (dragAmount.y / transform.imageScale).roundToInt()
                    val moved = region.copy(x = region.x + dx, y = region.y + dy)
                    onSnapshotChange(snapshot.updateActiveRegion(moved))
                }
            }
            .background(colors.handle.copy(alpha = 0.72f), CircleShape),
    )

    HandleKind.entries.forEach { handle ->
        val center = handle.center(rect)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (center.x - handlePx / 2f).roundToInt(),
                        (center.y - handlePx / 2f).roundToInt(),
                    )
                }
                .size(HandleTouchTarget)
                .semantics {
                    role = Role.Button
                    contentDescription = strings.resizeSelection
                }
                .pointerInput(snapshot.activeStage, region, handle) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val dx = (dragAmount.x / transform.imageScale).roundToInt()
                        val dy = (dragAmount.y / transform.imageScale).roundToInt()
                        onSnapshotChange(snapshot.updateActiveRegion(region.resizeBy(handle, dx, dy)))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(colors.handle, CircleShape)
                    .border(2.dp, activeRegionColor(snapshot, colors), CircleShape),
            )
        }
    }
}

private fun GuidedRoiEditorSnapshot.updateActiveRegion(region: GraphRegion): GuidedRoiEditorSnapshot =
    when (activeStage) {
        GuidedRoiEditorStage.GRAPH_PANEL -> GuidedRoiEditorReducer.updateGraphPanel(this, region)
        GuidedRoiEditorStage.PLOT_AREA -> GuidedRoiEditorReducer.updatePlotArea(this, region)
    }

private fun activeRegionColor(
    snapshot: GuidedRoiEditorSnapshot,
    colors: GuidedRoiEditorColors,
): Color =
    when (snapshot.activeStage) {
        GuidedRoiEditorStage.GRAPH_PANEL -> snapshot.graphPanelStatus.statusColor(colors)
        GuidedRoiEditorStage.PLOT_AREA -> snapshot.plotAreaStatus.statusColor(colors)
    }

private fun GraphRegion.resizeBy(handle: HandleKind, dx: Int, dy: Int): GraphRegion {
    var left = x
    var top = y
    var rightEdge = right
    var bottomEdge = bottom

    if (handle.affectsLeft) left += dx
    if (handle.affectsRight) rightEdge += dx
    if (handle.affectsTop) top += dy
    if (handle.affectsBottom) bottomEdge += dy

    if (rightEdge - left < MIN_REGION_SIZE_PX) {
        if (handle.affectsLeft) left = rightEdge - MIN_REGION_SIZE_PX else rightEdge = left + MIN_REGION_SIZE_PX
    }
    if (bottomEdge - top < MIN_REGION_SIZE_PX) {
        if (handle.affectsTop) top = bottomEdge - MIN_REGION_SIZE_PX else bottomEdge = top + MIN_REGION_SIZE_PX
    }

    return copy(
        x = left,
        y = top,
        width = rightEdge - left,
        height = bottomEdge - top,
    )
}

private enum class HandleKind(
    val affectsLeft: Boolean,
    val affectsTop: Boolean,
    val affectsRight: Boolean,
    val affectsBottom: Boolean,
) {
    TopLeft(true, true, false, false),
    Top(false, true, false, false),
    TopRight(false, true, true, false),
    Right(false, false, true, false),
    BottomRight(false, false, true, true),
    Bottom(false, false, false, true),
    BottomLeft(true, false, false, true),
    Left(true, false, false, false);

    fun center(rect: ViewRect): Offset =
        when (this) {
            TopLeft -> rect.topLeft
            Top -> Offset(rect.left + rect.width / 2f, rect.top)
            TopRight -> Offset(rect.right, rect.top)
            Right -> Offset(rect.right, rect.top + rect.height / 2f)
            BottomRight -> rect.bottomRight
            Bottom -> Offset(rect.left + rect.width / 2f, rect.bottom)
            BottomLeft -> Offset(rect.left, rect.bottom)
            Left -> Offset(rect.left, rect.top + rect.height / 2f)
        }
}

private data class ImageViewportTransform(
    val viewportWidth: Float,
    val viewportHeight: Float,
    val imageWidth: Float,
    val imageHeight: Float,
    val zoom: Float,
    val pan: Offset,
) {
    private val baseScale: Float =
        min(
            viewportWidth / imageWidth,
            viewportHeight / imageHeight,
        ).takeIf { it.isFinite() && it > 0f } ?: 1f

    val imageScale: Float = baseScale * zoom
    val imageDrawWidth: Float = imageWidth * imageScale
    val imageDrawHeight: Float = imageHeight * imageScale
    val imageLeft: Float = (viewportWidth - imageWidth * baseScale) / 2f + pan.x
    val imageTop: Float = (viewportHeight - imageHeight * baseScale) / 2f + pan.y

    fun regionToViewRect(region: GraphRegion): ViewRect =
        ViewRect(
            left = imageLeft + region.x * imageScale,
            top = imageTop + region.y * imageScale,
            width = region.width * imageScale,
            height = region.height * imageScale,
        )
}

private data class ViewRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
    val topLeft: Offset get() = Offset(left, top)
    val bottomRight: Offset get() = Offset(right, bottom)
    val size: Size get() = Size(max(0f, width), max(0f, height))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawOutsideScrim(
    rect: ViewRect,
    color: Color,
) {
    drawRect(color, Offset.Zero, Size(size.width, max(0f, rect.top)))
    drawRect(color, Offset(0f, rect.bottom), Size(size.width, max(0f, size.height - rect.bottom)))
    drawRect(color, Offset(0f, rect.top), Size(max(0f, rect.left), max(0f, rect.height)))
    drawRect(color, Offset(rect.right, rect.top), Size(max(0f, size.width - rect.right), max(0f, rect.height)))
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGuideGrid(
    transform: ImageViewportTransform,
    color: Color,
) {
    val step = (64f * transform.imageScale).coerceAtLeast(28f)
    var x = transform.imageLeft
    while (x < 0f) x += step
    while (x < size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 0.6.dp.toPx())
        x += step
    }
    var y = transform.imageTop
    while (y < 0f) y += step
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.6.dp.toPx())
        y += step
    }
}

@Composable
private fun Float.dpFromPx(): androidx.compose.ui.unit.Dp =
    with(androidx.compose.ui.platform.LocalDensity.current) { this@dpFromPx.toDp() }

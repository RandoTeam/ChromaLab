package com.chromalab.feature.processing.guided

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.chromalab.feature.processing.geometry.GeometryPoint
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val CALIBRATION_MIN_ZOOM = 1f
private const val CALIBRATION_MAX_ZOOM = 10f
private val CalibrationAnchorTouchTarget = 48.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidedCalibrationEditorScreen(
    imagePath: String,
    guidedState: GuidedDigitizationState,
    userProvenance: GuidedUserProvenance,
    onGuidedStateChange: (GuidedDigitizationState) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
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
    var snapshot by remember(guidedState.stateId, imagePath, imageWidth, imageHeight) {
        mutableStateOf(
            GuidedCalibrationEditorReducer.initialSnapshot(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                graphPanelBounds = graphPanel,
                plotAreaBounds = plotArea,
                anchors = guidedState.calibration?.calibrationSet?.anchors.orEmpty(),
            ),
        )
    }
    val strings = guidedCalibrationEditorStrings()
    val colors = guidedCalibrationEditorColors()

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
            GuidedCalibrationBottomBar(
                snapshot = snapshot,
                onSnapshotChange = { snapshot = it },
                onConfirmCalibration = {
                    val confirmed = GuidedCalibrationEditorReducer.confirmCalibration(
                        state = guidedState,
                        snapshot = snapshot,
                        userProvenance = userProvenance,
                        timestampEpochMillis = System.currentTimeMillis(),
                    )
                    onGuidedStateChange(confirmed)
                },
                strings = strings,
                colors = colors,
            )
        },
        containerColor = colors.background,
    ) { padding ->
        GuidedCalibrationEditorContent(
            imagePath = imagePath,
            snapshot = snapshot,
            onSnapshotChange = { snapshot = it },
            userProvenance = userProvenance,
            relatedImageId = guidedState.image?.imageId ?: guidedState.stateId,
            strings = strings,
            colors = colors,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        )
    }
}

@Composable
fun GuidedCalibrationEditorContent(
    imagePath: String,
    snapshot: CalibrationAnchorEditorSnapshot,
    onSnapshotChange: (CalibrationAnchorEditorSnapshot) -> Unit,
    userProvenance: GuidedUserProvenance,
    relatedImageId: String,
    modifier: Modifier = Modifier,
    strings: GuidedCalibrationEditorStrings = guidedCalibrationEditorStrings(),
    colors: GuidedCalibrationEditorColors = guidedCalibrationEditorColors(),
) {
    Column(
        modifier = modifier.background(colors.background),
    ) {
        GuidedCalibrationInstructionBar(
            snapshot = snapshot,
            strings = strings,
            colors = colors,
            modifier = Modifier.fillMaxWidth(),
        )
        GuidedCalibrationImageEditor(
            imagePath = imagePath,
            snapshot = snapshot,
            onSnapshotChange = onSnapshotChange,
            userProvenance = userProvenance,
            relatedImageId = relatedImageId,
            strings = strings,
            colors = colors,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

@Composable
private fun GuidedCalibrationInstructionBar(
    snapshot: CalibrationAnchorEditorSnapshot,
    strings: GuidedCalibrationEditorStrings,
    colors: GuidedCalibrationEditorColors,
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
                GuidedCalibrationStatusChip(strings.xAxis, snapshot.evaluation.xFit.status, strings, colors)
                GuidedCalibrationStatusChip(strings.yAxis, snapshot.evaluation.yFit.status, strings, colors)
                GuidedCalibrationGateChip(snapshot.evaluation.editorStatus, strings, colors)
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
private fun GuidedCalibrationImageEditor(
    imagePath: String,
    snapshot: CalibrationAnchorEditorSnapshot,
    onSnapshotChange: (CalibrationAnchorEditorSnapshot) -> Unit,
    userProvenance: GuidedUserProvenance,
    relatedImageId: String,
    strings: GuidedCalibrationEditorStrings,
    colors: GuidedCalibrationEditorColors,
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
        CalibrationViewportTransform(
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
                    zoom = (zoom * zoomChange).coerceIn(CALIBRATION_MIN_ZOOM, CALIBRATION_MAX_ZOOM)
                    pan += panChange
                }
            }
            .pointerInput(snapshot.placement.selectedAxis, snapshot.anchors, transform) {
                detectTapGestures { offset ->
                    val plotArea = snapshot.plotAreaBounds ?: return@detectTapGestures
                    val point = transform.viewToImagePoint(offset)
                    if (point.isInside(plotArea)) {
                        onSnapshotChange(
                            GuidedCalibrationEditorReducer.addAnchor(
                                snapshot = snapshot,
                                pixel = point,
                                userProvenance = userProvenance,
                                relatedImageId = relatedImageId,
                                timestampEpochMillis = System.currentTimeMillis(),
                            ),
                        )
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imagePath,
            contentDescription = strings.imageContentDescription,
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
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
            snapshot.plotAreaBounds?.let { region ->
                val rect = transform.regionToViewRect(region)
                drawRect(
                    color = colors.plotArea.copy(alpha = 0.12f),
                    topLeft = Offset(rect.left, rect.top),
                    size = rect.size,
                )
                drawRect(
                    color = colors.plotArea,
                    topLeft = Offset(rect.left, rect.top),
                    size = rect.size,
                    style = Stroke(width = 2.dp.toPx()),
                )
                drawCalibrationGuideGrid(rect, colors.guide)
            }
        }

        snapshot.anchors.forEach { anchor ->
            CalibrationAnchorHandle(
                anchor = anchor,
                selected = anchor.anchorId == snapshot.placement.selectedAnchorId,
                transform = transform,
                onMove = { point ->
                    onSnapshotChange(
                        GuidedCalibrationEditorReducer.moveAnchor(snapshot, anchor.anchorId, point),
                    )
                },
                onSelect = {
                    onSnapshotChange(
                        snapshot.copy(
                            placement = snapshot.placement.copy(
                                selectedAnchorId = anchor.anchorId,
                                selectedAxis = anchor.axis,
                            ),
                        ),
                    )
                },
                strings = strings,
                colors = colors,
            )
        }
    }
}

@Composable
private fun CalibrationAnchorHandle(
    anchor: ManualCalibrationAnchor,
    selected: Boolean,
    transform: CalibrationViewportTransform,
    onMove: (GeometryPoint) -> Unit,
    onSelect: () -> Unit,
    strings: GuidedCalibrationEditorStrings,
    colors: GuidedCalibrationEditorColors,
) {
    val center = transform.pointToView(anchor.pixel)
    val targetPx = with(LocalDensity.current) { CalibrationAnchorTouchTarget.toPx() }
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (center.x - targetPx / 2f).roundToInt(),
                    (center.y - targetPx / 2f).roundToInt(),
                )
            }
            .size(CalibrationAnchorTouchTarget)
            .semantics {
                role = Role.Button
                contentDescription = "${strings.anchor}: ${anchor.axis.name}"
            }
            .pointerInput(anchor.anchorId, selected, transform) {
                detectDragGestures(
                    onDragStart = { onSelect() },
                ) { change, dragAmount ->
                    change.consume()
                    val next = GeometryPoint(
                        x = anchor.pixel.x + dragAmount.x / transform.imageScale,
                        y = anchor.pixel.y + dragAmount.y / transform.imageScale,
                    )
                    onMove(next)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(if (selected) 26.dp else 22.dp)
                .background(anchor.axis.axisColor(colors), CircleShape)
                .border(
                    width = if (selected) 3.dp else 2.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else colors.anchorBorder,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = anchor.axis.name,
                color = Color.Black,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun GuidedCalibrationBottomBar(
    snapshot: CalibrationAnchorEditorSnapshot,
    onSnapshotChange: (CalibrationAnchorEditorSnapshot) -> Unit,
    onConfirmCalibration: () -> Unit,
    strings: GuidedCalibrationEditorStrings,
    colors: GuidedCalibrationEditorColors,
) {
    val selected = snapshot.selectedAnchor
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
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterChip(
                    selected = snapshot.placement.selectedAxis == CalibrationAxis.X,
                    onClick = {
                        onSnapshotChange(GuidedCalibrationEditorReducer.setSelectedAxis(snapshot, CalibrationAxis.X))
                    },
                    label = { Text(strings.placeX) },
                )
                FilterChip(
                    selected = snapshot.placement.selectedAxis == CalibrationAxis.Y,
                    onClick = {
                        onSnapshotChange(GuidedCalibrationEditorReducer.setSelectedAxis(snapshot, CalibrationAxis.Y))
                    },
                    label = { Text(strings.placeY) },
                )
            }

            selected?.let { anchor ->
                SelectedCalibrationAnchorEditor(
                    snapshot = snapshot,
                    anchor = anchor,
                    onSnapshotChange = onSnapshotChange,
                    strings = strings,
                )
            } ?: Text(
                text = strings.noAnchorSelected,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            CalibrationValidationSummary(snapshot.evaluation)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = { onSnapshotChange(GuidedCalibrationEditorReducer.resetAnchors(snapshot)) },
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = 0.dp),
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = null)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(strings.reset)
                }
                Button(
                    onClick = onConfirmCalibration,
                    enabled = snapshot.evaluation.canConfirm,
                    contentPadding = PaddingValues(horizontal = Spacing.md, vertical = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when (snapshot.evaluation.editorStatus) {
                            CalibrationEditorStatus.VALID -> colors.valid
                            CalibrationEditorStatus.REVIEW -> colors.review
                            CalibrationEditorStatus.INVALID -> MaterialTheme.colorScheme.error
                            CalibrationEditorStatus.INCOMPLETE -> MaterialTheme.colorScheme.primary
                        },
                        contentColor = Color.Black,
                    ),
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(strings.confirm)
                }
            }

            ElevatedAssistChip(
                onClick = {},
                enabled = false,
                label = { Text(strings.nextTraceDisabled) },
            )
        }
    }
}

@Composable
private fun SelectedCalibrationAnchorEditor(
    snapshot: CalibrationAnchorEditorSnapshot,
    anchor: ManualCalibrationAnchor,
    onSnapshotChange: (CalibrationAnchorEditorSnapshot) -> Unit,
    strings: GuidedCalibrationEditorStrings,
) {
    var valueText by remember(anchor.anchorId, anchor.value) {
        mutableStateOf(if (anchor.value.isFinite()) anchor.value.toString() else "")
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = anchor.axis == CalibrationAxis.X,
            onClick = {
                onSnapshotChange(GuidedCalibrationEditorReducer.setAnchorAxis(snapshot, anchor.anchorId, CalibrationAxis.X))
            },
            label = { Text(strings.xAxis) },
        )
        FilterChip(
            selected = anchor.axis == CalibrationAxis.Y,
            onClick = {
                onSnapshotChange(GuidedCalibrationEditorReducer.setAnchorAxis(snapshot, anchor.anchorId, CalibrationAxis.Y))
            },
            label = { Text(strings.yAxis) },
        )
        OutlinedTextField(
            value = valueText,
            onValueChange = { next ->
                valueText = next
                onSnapshotChange(
                    GuidedCalibrationEditorReducer.setAnchorValue(
                        snapshot,
                        anchor.anchorId,
                        next.toDoubleOrNull() ?: Double.NaN,
                    ),
                )
            },
            singleLine = true,
            label = { Text(strings.anchorValue) },
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick = {
                onSnapshotChange(GuidedCalibrationEditorReducer.removeAnchor(snapshot, anchor.anchorId))
            },
            modifier = Modifier.height(48.dp),
        ) {
            Text(strings.remove)
        }
    }
}

@Composable
private fun CalibrationValidationSummary(evaluation: CalibrationEditorEvaluation) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        CalibrationFitLine("X", evaluation.xFit)
        CalibrationFitLine("Y", evaluation.yFit)
        evaluation.issues.forEach { issue ->
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
private fun CalibrationFitLine(
    label: String,
    fit: CalibrationAxisFitSummary,
) {
    val text = buildString {
        append("$label: ${fit.status.name}")
        fit.residualReport.rmsePx?.let { append(" · RMSE ${it.formatShort()} px") }
        fit.residualReport.maxResidualPx?.let { append(" · max ${it.formatShort()} px") }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun GuidedCalibrationStatusChip(
    label: String,
    status: CalibrationFitStatus,
    strings: GuidedCalibrationEditorStrings,
    colors: GuidedCalibrationEditorColors,
) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                "$label: ${
                    when (status) {
                        CalibrationFitStatus.VALID -> strings.valid
                        CalibrationFitStatus.REVIEW -> strings.review
                        CalibrationFitStatus.INVALID -> strings.invalid
                    }
                }",
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

@Composable
private fun GuidedCalibrationGateChip(
    status: CalibrationEditorStatus,
    strings: GuidedCalibrationEditorStrings,
    colors: GuidedCalibrationEditorColors,
) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                when (status) {
                    CalibrationEditorStatus.INCOMPLETE -> strings.incomplete
                    CalibrationEditorStatus.VALID -> strings.valid
                    CalibrationEditorStatus.REVIEW -> strings.review
                    CalibrationEditorStatus.INVALID -> strings.invalid
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

data class GuidedCalibrationEditorStrings(
    val title: String = "Калибровка X/Y",
    val back: String = "Назад",
    val imageContentDescription: String = "Изображение хроматограммы для ручной калибровки",
    val instructions: String = "Поставьте минимум две точки X и две точки Y. Три и более точки дают проверку остатка.",
    val xAxis: String = "Ось X",
    val yAxis: String = "Ось Y",
    val placeX: String = "Ставить X",
    val placeY: String = "Ставить Y",
    val anchor: String = "Калибровочная точка",
    val anchorValue: String = "Значение",
    val noAnchorSelected: String = "Коснитесь plotArea, чтобы поставить точку, затем введите значение.",
    val reset: String = "Сброс",
    val remove: String = "Удалить",
    val confirm: String = "Подтвердить калибровку",
    val valid: String = "VALID",
    val review: String = "REVIEW",
    val invalid: String = "INVALID",
    val incomplete: String = "Не готово",
    val nextTraceDisabled: String = "Далее: подтверждение трассы (Phase 4)",
)

@Composable
fun guidedCalibrationEditorStrings(): GuidedCalibrationEditorStrings = GuidedCalibrationEditorStrings()

data class GuidedCalibrationEditorColors(
    val background: Color,
    val panelSurface: Color,
    val imageBackground: Color,
    val graphPanel: Color,
    val plotArea: Color,
    val xAnchor: Color,
    val yAnchor: Color,
    val anchorBorder: Color,
    val guide: Color,
    val valid: Color,
    val review: Color,
    val invalid: Color,
) {
    val handle: Color = Color.White
}

@Composable
fun guidedCalibrationEditorColors(): GuidedCalibrationEditorColors =
    GuidedCalibrationEditorColors(
        background = MaterialTheme.colorScheme.background,
        panelSurface = MaterialTheme.colorScheme.surface,
        imageBackground = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.46f),
        graphPanel = Color(0xFF83D8FF),
        plotArea = Color(0xFF9BE58A),
        xAnchor = Color(0xFF80CBC4),
        yAnchor = Color(0xFFFFD166),
        anchorBorder = MaterialTheme.colorScheme.surface,
        guide = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
        valid = Color(0xFF9BE58A),
        review = Color(0xFFFFD166),
        invalid = MaterialTheme.colorScheme.error,
    )

private data class CalibrationViewportTransform(
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

    fun regionToViewRect(region: GraphRegion): CalibrationViewRect =
        CalibrationViewRect(
            left = imageLeft + region.x * imageScale,
            top = imageTop + region.y * imageScale,
            width = region.width * imageScale,
            height = region.height * imageScale,
        )

    fun pointToView(point: GeometryPoint): Offset =
        Offset(
            x = imageLeft + point.x * imageScale,
            y = imageTop + point.y * imageScale,
        )

    fun viewToImagePoint(point: Offset): GeometryPoint =
        GeometryPoint(
            x = ((point.x - imageLeft) / imageScale).coerceIn(0f, imageWidth),
            y = ((point.y - imageTop) / imageScale).coerceIn(0f, imageHeight),
        )
}

private data class CalibrationViewRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height
    val size: Size get() = Size(max(0f, width), max(0f, height))
}

private fun GeometryPoint.isInside(region: GraphRegion): Boolean =
    x >= region.x &&
        y >= region.y &&
        x <= region.right &&
        y <= region.bottom

private fun CalibrationAxis.axisColor(colors: GuidedCalibrationEditorColors): Color =
    when (this) {
        CalibrationAxis.X -> colors.xAnchor
        CalibrationAxis.Y -> colors.yAnchor
    }

private fun CalibrationFitStatus.statusColor(colors: GuidedCalibrationEditorColors): Color =
    when (this) {
        CalibrationFitStatus.VALID -> colors.valid
        CalibrationFitStatus.REVIEW -> colors.review
        CalibrationFitStatus.INVALID -> colors.invalid
    }

private fun CalibrationEditorStatus.statusColor(colors: GuidedCalibrationEditorColors): Color =
    when (this) {
        CalibrationEditorStatus.VALID -> colors.valid
        CalibrationEditorStatus.REVIEW -> colors.review
        CalibrationEditorStatus.INVALID -> colors.invalid
        CalibrationEditorStatus.INCOMPLETE -> colors.review.copy(alpha = 0.72f)
    }

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCalibrationGuideGrid(
    rect: CalibrationViewRect,
    color: Color,
) {
    val xStep = (rect.width / 4f).coerceAtLeast(24f)
    val yStep = (rect.height / 4f).coerceAtLeast(24f)
    var x = rect.left
    while (x <= rect.right) {
        drawLine(color, Offset(x, rect.top), Offset(x, rect.bottom), strokeWidth = 0.6.dp.toPx())
        x += xStep
    }
    var y = rect.top
    while (y <= rect.bottom) {
        drawLine(color, Offset(rect.left, y), Offset(rect.right, y), strokeWidth = 0.6.dp.toPx())
        y += yStep
    }
}

@Composable
private fun Float.dpFromPx(): androidx.compose.ui.unit.Dp =
    with(LocalDensity.current) { this@dpFromPx.toDp() }

private fun Double.formatShort(): String =
    if (kotlin.math.abs(this) >= 100.0) {
        roundToInt().toString()
    } else {
        kotlin.math.round(this * 100.0).div(100.0).toString()
    }

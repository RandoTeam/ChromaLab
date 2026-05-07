package com.chromalab.feature.processing.signal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.chromalab.core.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * Display mode for the chart preview.
 */
enum class ChartDisplayMode {
    /** Pure digital chart */
    DIGITAL,
    /** Original image with curve overlay */
    IMAGE_OVERLAY,
}

/**
 * Interactive digital chromatogram preview.
 *
 * Features:
 * - Raw and smoothed signal rendering
 * - Pinch-to-zoom
 * - Tap to read point value
 * - Toggle raw/smoothed visibility
 * - Switch between digital chart and image overlay
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalPreviewScreen(
    smoothedSignal: SmoothedSignal,
    graphImagePath: String?,
    graphWidth: Int,
    graphHeight: Int,
    onAccept: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewW by remember { mutableFloatStateOf(1f) }
    var viewH by remember { mutableFloatStateOf(1f) }

    var displayMode by remember { mutableStateOf(ChartDisplayMode.DIGITAL) }
    var showRaw by remember { mutableStateOf(true) }
    var showSmoothed by remember { mutableStateOf(true) }

    // Zoom state
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }

    // Tapped point info
    var tappedPoint by remember { mutableStateOf<GraphPoint?>(null) }

    val activeSignal = if (showSmoothed && smoothedSignal.enabled) {
        smoothedSignal.smoothed
    } else {
        smoothedSignal.raw
    }

    // Compute data bounds
    val points = activeSignal.points
    val minTime = points.minOfOrNull { it.time } ?: 0f
    val maxTime = points.maxOfOrNull { it.time } ?: 1f
    val minInt = points.minOfOrNull { it.intensity } ?: 0f
    val maxInt = points.maxOfOrNull { it.intensity } ?: 1f
    val timeRange = (maxTime - minTime).coerceAtLeast(0.01f)
    val intRange = (maxInt - minInt).coerceAtLeast(0.01f)

    // Padding for axis labels
    val padLeft = 56f
    val padBottom = 36f
    val padTop = 16f
    val padRight = 16f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Цифровой график") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onAccept) {
                        Icon(Icons.Filled.Check, contentDescription = "Принять")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // Display mode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        FilterChip(
                            selected = displayMode == ChartDisplayMode.DIGITAL,
                            onClick = { displayMode = ChartDisplayMode.DIGITAL },
                            label = { Text("График") },
                        )
                        if (graphImagePath != null) {
                            FilterChip(
                                selected = displayMode == ChartDisplayMode.IMAGE_OVERLAY,
                                onClick = { displayMode = ChartDisplayMode.IMAGE_OVERLAY },
                                label = { Text("Изображение") },
                            )
                        }
                    }

                    // Signal toggles
                    if (displayMode == ChartDisplayMode.DIGITAL) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        ) {
                            FilterChip(
                                selected = showRaw,
                                onClick = { showRaw = !showRaw },
                                label = { Text("Raw") },
                                leadingIcon = {
                                    Box(
                                        Modifier
                                            .size(8.dp)
                                            .background(Color(0x80FFFFFF), shape = MaterialTheme.shapes.small),
                                    )
                                },
                            )
                            FilterChip(
                                selected = showSmoothed,
                                onClick = { showSmoothed = !showSmoothed },
                                label = { Text("Smoothed") },
                                leadingIcon = {
                                    Box(
                                        Modifier
                                            .size(8.dp)
                                            .background(Color(0xFFFF1744), shape = MaterialTheme.shapes.small),
                                    )
                                },
                            )
                        }
                    }

                    // Tapped point info
                    tappedPoint?.let { gp ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(Spacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            ) {
                                Text(
                                    "t = ${"%.2f".format(gp.time)} ${activeSignal.timeUnit}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    "I = ${"%.1f".format(gp.intensity)} ${activeSignal.intensityUnit}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                if (gp.isInterpolated) {
                                    Text(
                                        "(интерп.)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }

                    // Zoom info
                    if (zoom > 1.05f) {
                        Text(
                            "Zoom: ${"%.1f".format(zoom)}× (щипок для сброса)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .onSizeChanged { size ->
                    viewW = size.width.toFloat()
                    viewH = size.height.toFloat()
                },
        ) {
            when (displayMode) {
                ChartDisplayMode.IMAGE_OVERLAY -> {
                    // Image with curve overlay
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        AsyncImage(
                            model = graphImagePath,
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier.fillMaxSize(),
                        )
                        // Overlay smoothed curve
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val sx = size.width / graphWidth
                            val sy = size.height / graphHeight
                            val sorted = smoothedSignal.smoothed.points.sortedBy { it.pixelX }
                            for (i in 0 until sorted.size - 1) {
                                val p1 = sorted[i]
                                val p2 = sorted[i + 1]
                                if (p2.pixelX - p1.pixelX > 5) continue
                                drawLine(
                                    Color(0xFFFF1744),
                                    start = Offset(p1.pixelX * sx, p1.pixelY * sy),
                                    end = Offset(p2.pixelX * sx, p2.pixelY * sy),
                                    strokeWidth = 2.dp.toPx(),
                                )
                            }
                        }
                    }
                }

                ChartDisplayMode.DIGITAL -> {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF121212))
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, gestureZoom, _ ->
                                    zoom = (zoom * gestureZoom).coerceIn(1f, 10f)
                                    panX += pan.x
                                    panY += pan.y
                                    if (zoom < 1.05f) {
                                        panX = 0f
                                        panY = 0f
                                    }
                                }
                            }
                            .pointerInput(points) {
                                detectTapGestures { offset ->
                                    // Find nearest point to tap
                                    val chartW = viewW - padLeft - padRight
                                    val chartH = viewH - padTop - padBottom
                                    if (chartW <= 0 || chartH <= 0) return@detectTapGestures

                                    val tapTime = minTime + ((offset.x - padLeft) / chartW / zoom + panX / chartW) * timeRange
                                    tappedPoint = points.minByOrNull {
                                        kotlin.math.abs(it.time - tapTime)
                                    }
                                }
                            },
                    ) {
                        val chartW = size.width - padLeft - padRight
                        val chartH = size.height - padTop - padBottom
                        if (chartW <= 0 || chartH <= 0) return@Canvas

                        // Background grid
                        drawGrid(
                            minTime, maxTime, minInt, maxInt,
                            padLeft, padTop, chartW, chartH,
                            activeSignal.timeUnit, activeSignal.intensityUnit,
                        )

                        // Draw raw signal (grey, thin)
                        if (showRaw) {
                            drawSignalPath(
                                smoothedSignal.raw.points,
                                minTime, timeRange, minInt, intRange,
                                padLeft, padTop, chartW, chartH,
                                color = Color(0x60FFFFFF),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }

                        // Draw smoothed signal (red, bold)
                        if (showSmoothed && smoothedSignal.enabled) {
                            drawSignalPath(
                                smoothedSignal.smoothed.points,
                                minTime, timeRange, minInt, intRange,
                                padLeft, padTop, chartW, chartH,
                                color = Color(0xFFFF1744),
                                strokeWidth = 2.dp.toPx(),
                            )
                        }

                        // Draw tapped point marker
                        tappedPoint?.let { gp ->
                            val x = padLeft + ((gp.time - minTime) / timeRange) * chartW
                            val y = padTop + (1f - (gp.intensity - minInt) / intRange) * chartH
                            drawCircle(Color.White, 6.dp.toPx(), Offset(x, y))
                            drawCircle(Color(0xFFFF1744), 4.dp.toPx(), Offset(x, y))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draw a signal path on the chart canvas.
 */
private fun DrawScope.drawSignalPath(
    points: List<GraphPoint>,
    minTime: Float, timeRange: Float,
    minInt: Float, intRange: Float,
    padLeft: Float, padTop: Float,
    chartW: Float, chartH: Float,
    color: Color, strokeWidth: Float,
) {
    if (points.size < 2) return

    val sorted = points.sortedBy { it.time }
    val path = Path()
    var started = false

    for (gp in sorted) {
        val x = padLeft + ((gp.time - minTime) / timeRange) * chartW
        val y = padTop + (1f - (gp.intensity - minInt) / intRange) * chartH

        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(path, color, style = Stroke(width = strokeWidth))
}

/**
 * Draw axis grid lines and labels.
 */
private fun DrawScope.drawGrid(
    minTime: Float, maxTime: Float,
    minInt: Float, maxInt: Float,
    padLeft: Float, padTop: Float,
    chartW: Float, chartH: Float,
    timeUnit: String, intUnit: String,
) {
    val gridColor = Color(0x30FFFFFF)
    val labelColor = Color(0x80FFFFFF)

    // Horizontal grid (intensity)
    val intSteps = 5
    val intStep = (maxInt - minInt) / intSteps
    for (i in 0..intSteps) {
        val v = minInt + i * intStep
        val y = padTop + (1f - (v - minInt) / (maxInt - minInt)) * chartH
        drawLine(gridColor, Offset(padLeft, y), Offset(padLeft + chartW, y))
    }

    // Vertical grid (time)
    val timeSteps = 5
    val timeStep = (maxTime - minTime) / timeSteps
    for (i in 0..timeSteps) {
        val v = minTime + i * timeStep
        val x = padLeft + ((v - minTime) / (maxTime - minTime)) * chartW
        drawLine(gridColor, Offset(x, padTop), Offset(x, padTop + chartH))
    }

    // Axes
    drawLine(Color(0x60FFFFFF), Offset(padLeft, padTop), Offset(padLeft, padTop + chartH), strokeWidth = 1f)
    drawLine(Color(0x60FFFFFF), Offset(padLeft, padTop + chartH), Offset(padLeft + chartW, padTop + chartH), strokeWidth = 1f)
}

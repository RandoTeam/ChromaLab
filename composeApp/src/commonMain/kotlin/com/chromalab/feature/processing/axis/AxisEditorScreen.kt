package com.chromalab.feature.processing.axis

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.pipeline.DetectionMethod
import kotlin.math.abs

/**
 * Axis editor screen.
 * Shows detected axes as overlay lines on the graph image.
 * User can drag to adjust X and Y axis positions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxisEditorScreen(
    imagePath: String,
    graphRegion: GraphRegion,
    autoResult: AxesResult,
    onAccept: (AxesResult) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewW by remember { mutableFloatStateOf(1f) }
    var viewH by remember { mutableFloatStateOf(1f) }

    val sx by remember(viewW, graphRegion.width) {
        mutableFloatStateOf(if (graphRegion.width > 0) viewW / graphRegion.width else 1f)
    }
    val sy by remember(viewH, graphRegion.height) {
        mutableFloatStateOf(if (graphRegion.height > 0) viewH / graphRegion.height else 1f)
    }

    // Axis positions in view coordinates (relative to graph region)
    val defaultXAxisY = autoResult.xAxis?.let { (it.y1 - graphRegion.y) * sy }
        ?: (viewH * 0.85f) // Default: 85% down
    val defaultYAxisX = autoResult.yAxis?.let { (it.x1 - graphRegion.x) * sx }
        ?: (viewW * 0.10f) // Default: 10% from left

    var xAxisViewY by remember { mutableFloatStateOf(defaultXAxisY) }
    var yAxisViewX by remember { mutableFloatStateOf(defaultYAxisX) }

    LaunchedEffect(sx, sy) {
        xAxisViewY = autoResult.xAxis?.let { (it.y1 - graphRegion.y) * sy } ?: (viewH * 0.85f)
        yAxisViewX = autoResult.yAxis?.let { (it.x1 - graphRegion.x) * sx } ?: (viewW * 0.10f)
    }

    var isModified by remember { mutableStateOf(false) }

    val xColor = Color(0xFF4CAF50) // Green for X axis
    val yColor = Color(0xFF2196F3) // Blue for Y axis
    val originColor = Color(0xFFFF9800) // Orange for origin

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Оси графика") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val method = if (isModified) DetectionMethod.AUTO_CORRECTED
                        else autoResult.detectionMethod
                        val xAxis = AxisLine(
                            x1 = graphRegion.x.toFloat(),
                            y1 = graphRegion.y + xAxisViewY / sy,
                            x2 = (graphRegion.x + graphRegion.width).toFloat(),
                            y2 = graphRegion.y + xAxisViewY / sy,
                        )
                        val yAxis = AxisLine(
                            x1 = graphRegion.x + yAxisViewX / sx,
                            y1 = graphRegion.y.toFloat(),
                            x2 = graphRegion.x + yAxisViewX / sx,
                            y2 = (graphRegion.y + graphRegion.height).toFloat(),
                        )
                        onAccept(
                            AxesResult(
                                xAxis = xAxis,
                                yAxis = yAxis,
                                origin = AxisOrigin(yAxis.x1, xAxis.y1),
                                detectionMethod = method,
                                confidence = if (isModified) 1f else autoResult.confidence,
                                timestamp = System.currentTimeMillis(),
                            ),
                        )
                    }) {
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
                ) {
                    // Status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (autoResult.hasAxes) Icons.Filled.Info else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (autoResult.hasAxes)
                                MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            when {
                                autoResult.hasAxes && !isModified -> "Оси определены автоматически"
                                isModified -> "Оси скорректированы вручную"
                                else -> "Переместите линии на оси графика"
                            },
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }

                    autoResult.warnings.forEach { warning ->
                        Text(
                            warning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Legend
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        LegendItem("Ось X", xColor)
                        LegendItem("Ось Y", yColor)
                        LegendItem("Начало", originColor)
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
                .background(Color.Black)
                .onSizeChanged { size ->
                    viewW = size.width.toFloat()
                    viewH = size.height.toFloat()
                },
        ) {
            AsyncImage(
                model = imagePath,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )

            // Axis overlay + drag handlers
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        val threshold = 30f
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val pos = change.position - dragAmount

                            // Check which axis is being dragged
                            val nearXAxis = abs(pos.y - xAxisViewY) < threshold
                            val nearYAxis = abs(pos.x - yAxisViewX) < threshold

                            if (nearXAxis) {
                                xAxisViewY = (xAxisViewY + dragAmount.y)
                                    .coerceIn(0f, viewH)
                                isModified = true
                            } else if (nearYAxis) {
                                yAxisViewX = (yAxisViewX + dragAmount.x)
                                    .coerceIn(0f, viewW)
                                isModified = true
                            }
                        }
                    },
            ) {
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)

                // X axis (horizontal green line)
                drawLine(
                    xColor,
                    start = Offset(0f, xAxisViewY),
                    end = Offset(size.width, xAxisViewY),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = dashEffect,
                )

                // Y axis (vertical blue line)
                drawLine(
                    yColor,
                    start = Offset(yAxisViewX, 0f),
                    end = Offset(yAxisViewX, size.height),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = dashEffect,
                )

                // Origin point
                val originPos = Offset(yAxisViewX, xAxisViewY)
                drawCircle(Color.White, radius = 8.dp.toPx(), center = originPos)
                drawCircle(originColor, radius = 5.dp.toPx(), center = originPos)

                // Drag handles on axes
                val handleR = 10.dp.toPx()
                // X axis handle (center)
                val xHandle = Offset(size.width / 2, xAxisViewY)
                drawCircle(Color.White, radius = handleR, center = xHandle)
                drawCircle(xColor, radius = handleR * 0.6f, center = xHandle)

                // Y axis handle (center)
                val yHandle = Offset(yAxisViewX, size.height / 2)
                drawCircle(Color.White, radius = handleR, center = yHandle)
                drawCircle(yColor, radius = handleR * 0.6f, center = yHandle)
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color, radius = size.minDimension / 2)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

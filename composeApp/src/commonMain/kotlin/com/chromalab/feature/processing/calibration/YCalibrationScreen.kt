package com.chromalab.feature.processing.calibration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.ocr.AxisOcrResult
import kotlin.math.pow

/**
 * Y axis calibration screen.
 * The user taps two points on the Y axis and enters intensity values.
 * A linear transform pixelY → intensity is computed.
 *
 * KEY: pixelY grows DOWN, but intensity grows UP.
 * So point at the TOP of the graph has HIGHER intensity,
 * and point at the BOTTOM has LOWER intensity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YCalibrationScreen(
    imagePath: String,
    graphRegion: GraphRegion,
    axes: AxesResult,
    ocrSuggestion: AxisOcrResult?,
    onAccept: (YAxisCalibration) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    sourceImageWidth: Int = graphRegion.x + graphRegion.width,
    sourceImageHeight: Int = graphRegion.y + graphRegion.height,
    allowSkip: Boolean = true,
    allowWarningAccept: Boolean = true,
) {
    var viewW by remember { mutableFloatStateOf(1f) }
    var viewH by remember { mutableFloatStateOf(1f) }

    val mappedSourceImageWidth = sourceImageWidth
        .coerceAtLeast(graphRegion.x + graphRegion.width)
        .coerceAtLeast(1)
    val mappedSourceImageHeight = sourceImageHeight
        .coerceAtLeast(graphRegion.y + graphRegion.height)
        .coerceAtLeast(1)
    val ocrAnchors = remember(ocrSuggestion, graphRegion) {
        ocrSuggestion.yCalibrationAnchors(graphRegion)
    }

    // Two calibration points (view Y coordinates)
    var point1Y by remember { mutableFloatStateOf(-1f) }
    var point2Y by remember { mutableFloatStateOf(-1f) }
    var value1 by remember { mutableStateOf("") }
    var value2 by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("mAU") }

    // Pre-fill from OCR on first composition
    LaunchedEffect(ocrSuggestion, viewW, viewH, mappedSourceImageWidth, mappedSourceImageHeight) {
        if (viewW <= 1f || viewH <= 1f) return@LaunchedEffect
        val ocr = ocrSuggestion ?: return@LaunchedEffect
        if (ocr.hasYSuggestions && value1.isEmpty() && point1Y <= 0f && point2Y <= 0f) {
            val sorted = ocr.suggestedYValues.sorted()
            val bottomAnchor = ocrAnchors.firstOrNull()
            val topAnchor = ocrAnchors.lastOrNull()
            value1 = (bottomAnchor?.value ?: sorted.first()).toBigDecimal().toPlainString()
            value2 = (topAnchor?.value ?: sorted.last()).toBigDecimal().toPlainString()
            point1Y = bottomAnchor?.let {
                sourceYToView(it.sourceY, viewH, mappedSourceImageHeight)
            } ?: regionPixelYToView(
                graphRegion.height * 0.90f,
                viewH,
                mappedSourceImageHeight,
                graphRegion,
            )
            point2Y = topAnchor?.let {
                sourceYToView(it.sourceY, viewH, mappedSourceImageHeight)
            } ?: regionPixelYToView(
                graphRegion.height * 0.10f,
                viewH,
                mappedSourceImageHeight,
                graphRegion,
            )
        }
        if (ocr.yUnit != null && unit == "mAU") {
            unit = ocr.yUnit
        }
    }

    var settingPoint by remember { mutableIntStateOf(1) }
    var draggingPoint by remember { mutableIntStateOf(0) }

    val yAxisX = axes.yAxis?.let {
        sourceXToView(it.x1, viewW, mappedSourceImageWidth)
    } ?: regionPixelXToView(
        graphRegion.width * 0.10f,
        viewW,
        mappedSourceImageWidth,
        graphRegion,
    )

    val p1Color = Color(0xFFFF5722) // Orange
    val p2Color = Color(0xFF2196F3) // Blue
    val controlColor = Color(0xFF4CAF50) // Green

    // Build calibration for preview
    val v1 = value1.toFloatOrNull()
    val v2 = value2.toFloatOrNull()
    val calibration = if (point1Y > 0 && point2Y > 0 && v1 != null && v2 != null) {
        val pixelY1 = viewYToRegionPixel(point1Y, viewH, mappedSourceImageHeight, graphRegion)
        val pixelY2 = viewYToRegionPixel(point2Y, viewH, mappedSourceImageHeight, graphRegion)
        LinearCalibration(
            CalibrationPoint(pixelY1, v1),
            CalibrationPoint(pixelY2, v2),
        )
    } else null

    val result = calibration?.let {
        YAxisCalibration(it, unit, System.currentTimeMillis())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Калибровка оси Y") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (result != null && !result.hasWarnings) {
                        IconButton(onClick = { onAccept(result) }) {
                            Icon(Icons.Filled.Check, contentDescription = "Принять")
                        }
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
                    // Warnings
                    result?.warnings?.forEach { warning ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                warning,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    // Point 1 (lower point — baseline)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawCircle(p1Color)
                        }
                        Text(
                            "Точка 1:",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(60.dp),
                        )
                        OutlinedTextField(
                            value = value1,
                            onValueChange = { value1 = it },
                            label = { Text("Нижнее (напр. 0)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (point1Y > 0) "✓" else "←тап",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (point1Y > 0) controlColor
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Point 2 (upper point — peak)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawCircle(p2Color)
                        }
                        Text(
                            "Точка 2:",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(60.dp),
                        )
                        OutlinedTextField(
                            value = value2,
                            onValueChange = { value2 = it },
                            label = { Text("Верхнее (напр. 350)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (point2Y > 0) "✓" else "←тап",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (point2Y > 0) controlColor
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Unit selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Text("Единица:", style = MaterialTheme.typography.labelMedium)
                        listOf("mAU", "mV", "µV", "%").forEach { u ->
                            FilterChip(
                                selected = unit == u,
                                onClick = { unit = u },
                                label = { Text(u) },
                            )
                        }
                    }

                    // Hint about inverted Y
                    Text(
                        "Совет: точка 1 — на базовой линии (внизу), точка 2 — на максимуме (вверху)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Instruction
                    Text(
                        when {
                            point1Y <= 0 -> "Нажмите на ось Y, чтобы отметить нижнюю точку"
                            point2Y <= 0 -> "Нажмите на ось Y, чтобы отметить верхнюю точку"
                            v1 == null || v2 == null -> "Введите значения для обеих точек"
                            result?.hasWarnings == true -> "Исправьте предупреждения"
                            else -> "Готово — нажмите ✓ для подтверждения"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    // Accept with warnings
                    if (allowWarningAccept && result != null && result.hasWarnings) {
                        OutlinedButton(
                            onClick = { onAccept(result) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Принять с предупреждениями")
                        }
                    }

                    // Skip button
                    if (allowSkip) {
                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Пропустить калибровку Y")
                        }
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

            // Overlay — supports tap-to-place and drag-to-adjust
            val dragThreshold = 40f

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val dist1 = if (point1Y > 0) kotlin.math.abs(offset.y - point1Y) else Float.MAX_VALUE
                                val dist2 = if (point2Y > 0) kotlin.math.abs(offset.y - point2Y) else Float.MAX_VALUE
                                val grabRadius = dragThreshold * density

                                when {
                                    dist1 < grabRadius && dist1 <= dist2 -> draggingPoint = 1
                                    dist2 < grabRadius && dist2 < dist1 -> draggingPoint = 2
                                    point1Y <= 0 -> { point1Y = offset.y; draggingPoint = 1 }
                                    point2Y <= 0 -> { point2Y = offset.y; draggingPoint = 2 }
                                    else -> draggingPoint = if (dist1 <= dist2) 1 else 2
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                when (draggingPoint) {
                                    1 -> point1Y = change.position.y.coerceIn(0f, size.height.toFloat())
                                    2 -> point2Y = change.position.y.coerceIn(0f, size.height.toFloat())
                                }
                            },
                            onDragEnd = { draggingPoint = 0 },
                            onDragCancel = { draggingPoint = 0 },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val dist1 = if (point1Y > 0) kotlin.math.abs(offset.y - point1Y) else Float.MAX_VALUE
                            val dist2 = if (point2Y > 0) kotlin.math.abs(offset.y - point2Y) else Float.MAX_VALUE
                            val grabRadius = dragThreshold * density

                            when {
                                dist1 < grabRadius && dist1 <= dist2 -> settingPoint = 1
                                dist2 < grabRadius && dist2 < dist1 -> settingPoint = 2
                                point1Y <= 0 || settingPoint == 1 -> {
                                    point1Y = offset.y; settingPoint = 2
                                }
                                point2Y <= 0 || settingPoint == 2 -> {
                                    point2Y = offset.y; settingPoint = 1
                                }
                                else -> {
                                    if (settingPoint == 1) { point1Y = offset.y; settingPoint = 2 }
                                    else { point2Y = offset.y; settingPoint = 1 }
                                }
                            }
                        }
                    },
            ) {
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                val activeRadius = 12.dp.toPx()
                val inactiveRadius = 7.dp.toPx()

                // Y axis line
                drawLine(
                    Color(0xFF2196F3).copy(alpha = 0.5f),
                    start = Offset(yAxisX, 0f),
                    end = Offset(yAxisX, size.height),
                    strokeWidth = 1.5f.dp.toPx(),
                    pathEffect = dashEffect,
                )

                ocrAnchors.forEach { anchor ->
                    val anchorY = sourceYToView(anchor.sourceY, size.height, mappedSourceImageHeight)
                    if (anchorY in 0f..size.height) {
                        drawLine(
                            controlColor.copy(alpha = 0.45f),
                            start = Offset(yAxisX - 10.dp.toPx(), anchorY),
                            end = Offset(yAxisX + 10.dp.toPx(), anchorY),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }

                // Point 1 (lower)
                if (point1Y > 0) {
                    val isActive = draggingPoint == 1
                    drawLine(
                        p1Color.copy(alpha = if (isActive) 1f else 0.7f),
                        start = Offset(0f, point1Y),
                        end = Offset(size.width, point1Y),
                        strokeWidth = if (isActive) 2.5f.dp.toPx() else 1.5f.dp.toPx(),
                        pathEffect = dashEffect,
                    )
                    val r = if (isActive) activeRadius else inactiveRadius
                    drawCircle(Color.White, radius = r + 3.dp.toPx(), center = Offset(yAxisX, point1Y))
                    drawCircle(p1Color, radius = r, center = Offset(yAxisX, point1Y))
                }

                // Point 2 (upper)
                if (point2Y > 0) {
                    val isActive = draggingPoint == 2
                    drawLine(
                        p2Color.copy(alpha = if (isActive) 1f else 0.7f),
                        start = Offset(0f, point2Y),
                        end = Offset(size.width, point2Y),
                        strokeWidth = if (isActive) 2.5f.dp.toPx() else 1.5f.dp.toPx(),
                        pathEffect = dashEffect,
                    )
                    val r = if (isActive) activeRadius else inactiveRadius
                    drawCircle(Color.White, radius = r + 3.dp.toPx(), center = Offset(yAxisX, point2Y))
                    drawCircle(p2Color, radius = r, center = Offset(yAxisX, point2Y))
                }

                // Control tick marks
                if (calibration != null && calibration.isValid) {
                    val minVal = minOf(v1 ?: 0f, v2 ?: 0f)
                    val maxVal = maxOf(v1 ?: 0f, v2 ?: 0f)
                    val range = maxVal - minVal
                    if (range > 0) {
                        val step = niceStep(range / 5)
                        var tick = (minVal / step).toInt() * step
                        while (tick <= maxVal + step) {
                            val pixelY = calibration.realToPixel(tick)
                            val viewY = regionPixelYToView(
                                pixelY,
                                size.height,
                                mappedSourceImageHeight,
                                graphRegion,
                            )
                            if (viewY in 0f..size.height) {
                                drawLine(
                                    controlColor,
                                    start = Offset(yAxisX - 6.dp.toPx(), viewY),
                                    end = Offset(yAxisX + 6.dp.toPx(), viewY),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            }
                            tick += step
                        }
                    }
                }
            }
        }
    }
}

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.ocr.AxisOcrResult
import kotlin.math.pow

/**
 * X axis calibration screen.
 * The user taps two points on the X axis and enters time values.
 * A linear transform pixelX → time is computed.
 * Control values are shown along the axis for verification.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XCalibrationScreen(
    imagePath: String,
    graphRegion: GraphRegion,
    axes: AxesResult,
    ocrSuggestion: AxisOcrResult?,
    onAccept: (XAxisCalibration) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewW by remember { mutableFloatStateOf(1f) }
    var viewH by remember { mutableFloatStateOf(1f) }

    val sx by remember(viewW, graphRegion.width) {
        mutableFloatStateOf(if (graphRegion.width > 0) viewW / graphRegion.width else 1f)
    }

    // Two calibration points (view X coordinates)
    var point1X by remember { mutableFloatStateOf(-1f) }
    var point2X by remember { mutableFloatStateOf(-1f) }
    var value1 by remember { mutableStateOf("") }
    var value2 by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("мин") }

    // Pre-fill from OCR on first composition
    LaunchedEffect(ocrSuggestion) {
        val ocr = ocrSuggestion ?: return@LaunchedEffect
        if (ocr.hasXSuggestions && value1.isEmpty()) {
            val sorted = ocr.suggestedXValues.sorted()
            value1 = sorted.first().toBigDecimal().toPlainString()
            value2 = sorted.last().toBigDecimal().toPlainString()
            // Auto-place points at axis extremes
            point1X = 0f + viewW * 0.05f  // near left edge
            point2X = viewW * 0.95f        // near right edge
        }
        if (ocr.xUnit != null && unit == "мин") {
            unit = ocr.xUnit
        }
    }

    // Which point we're setting / dragging
    var settingPoint by remember { mutableIntStateOf(1) }
    var draggingPoint by remember { mutableIntStateOf(0) }

    val xAxisY = axes.xAxis?.let { (it.y1 - graphRegion.y) * (viewH / graphRegion.height) }
        ?: (viewH * 0.85f)

    val p1Color = Color(0xFFFF5722) // Orange
    val p2Color = Color(0xFF2196F3) // Blue
    val controlColor = Color(0xFF4CAF50) // Green

    // Build calibration for preview
    val v1 = value1.toFloatOrNull()
    val v2 = value2.toFloatOrNull()
    val calibration = if (point1X > 0 && point2X > 0 && v1 != null && v2 != null) {
        val pixelX1 = graphRegion.x + point1X / sx
        val pixelX2 = graphRegion.x + point2X / sx
        LinearCalibration(
            CalibrationPoint(pixelX1, v1),
            CalibrationPoint(pixelX2, v2),
        )
    } else null

    val result = calibration?.let {
        XAxisCalibration(it, unit, System.currentTimeMillis())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Калибровка оси X") },
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

                    // Point 1 input
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
                            label = { Text("Значение") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (point1X > 0) "✓" else "←тап",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (point1X > 0) controlColor
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Point 2 input
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
                            label = { Text("Значение") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            if (point2X > 0) "✓" else "←тап",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (point2X > 0) controlColor
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    // Unit selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Text("Единица:", style = MaterialTheme.typography.labelMedium)
                        listOf("мин", "сек", "с").forEach { u ->
                            FilterChip(
                                selected = unit == u,
                                onClick = { unit = u },
                                label = { Text(u) },
                            )
                        }
                    }

                    // Instruction
                    Text(
                        when {
                            point1X <= 0 -> "Нажмите на ось X, чтобы отметить точку 1"
                            point2X <= 0 -> "Нажмите на ось X, чтобы отметить точку 2"
                            v1 == null || v2 == null -> "Введите значения для обеих точек"
                            result?.hasWarnings == true -> "Исправьте предупреждения"
                            else -> "Готово — нажмите ✓ для подтверждения"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Accept button (when warnings present)
                    if (result != null && result.hasWarnings) {
                        OutlinedButton(
                            onClick = { onAccept(result) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Принять с предупреждениями")
                        }
                    }

                    // Skip button
                    TextButton(
                        onClick = onSkip,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Пропустить калибровку X")
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

            // Overlay — supports both tap-to-place and drag-to-adjust
            val dragThreshold = 40f // dp-like threshold for grab detection

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                // Check if near an existing point → grab it
                                val dist1 = if (point1X > 0) kotlin.math.abs(offset.x - point1X) else Float.MAX_VALUE
                                val dist2 = if (point2X > 0) kotlin.math.abs(offset.x - point2X) else Float.MAX_VALUE
                                val grabRadius = dragThreshold * density

                                when {
                                    dist1 < grabRadius && dist1 <= dist2 -> draggingPoint = 1
                                    dist2 < grabRadius && dist2 < dist1 -> draggingPoint = 2
                                    point1X <= 0 -> { point1X = offset.x; draggingPoint = 1 }
                                    point2X <= 0 -> { point2X = offset.x; draggingPoint = 2 }
                                    else -> {
                                        // Both placed, grab nearest
                                        draggingPoint = if (dist1 <= dist2) 1 else 2
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                when (draggingPoint) {
                                    1 -> point1X = change.position.x.coerceIn(0f, size.width.toFloat())
                                    2 -> point2X = change.position.x.coerceIn(0f, size.width.toFloat())
                                }
                            },
                            onDragEnd = { draggingPoint = 0 },
                            onDragCancel = { draggingPoint = 0 },
                        )
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            // Tap: if near existing point, select it; otherwise place next
                            val dist1 = if (point1X > 0) kotlin.math.abs(offset.x - point1X) else Float.MAX_VALUE
                            val dist2 = if (point2X > 0) kotlin.math.abs(offset.x - point2X) else Float.MAX_VALUE
                            val grabRadius = dragThreshold * density

                            when {
                                dist1 < grabRadius && dist1 <= dist2 -> settingPoint = 1
                                dist2 < grabRadius && dist2 < dist1 -> settingPoint = 2
                                point1X <= 0 || settingPoint == 1 -> {
                                    point1X = offset.x; settingPoint = 2
                                }
                                point2X <= 0 || settingPoint == 2 -> {
                                    point2X = offset.x; settingPoint = 1
                                }
                                else -> {
                                    if (settingPoint == 1) { point1X = offset.x; settingPoint = 2 }
                                    else { point2X = offset.x; settingPoint = 1 }
                                }
                            }
                        }
                    },
            ) {
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                val activeRadius = 12.dp.toPx()
                val inactiveRadius = 7.dp.toPx()

                // X axis line
                drawLine(
                    Color(0xFF4CAF50).copy(alpha = 0.5f),
                    start = Offset(0f, xAxisY),
                    end = Offset(size.width, xAxisY),
                    strokeWidth = 1.5f.dp.toPx(),
                    pathEffect = dashEffect,
                )

                // Point 1
                if (point1X > 0) {
                    val isActive = draggingPoint == 1
                    drawLine(
                        p1Color.copy(alpha = if (isActive) 1f else 0.7f),
                        start = Offset(point1X, 0f),
                        end = Offset(point1X, size.height),
                        strokeWidth = if (isActive) 2.5f.dp.toPx() else 1.5f.dp.toPx(),
                        pathEffect = dashEffect,
                    )
                    val r = if (isActive) activeRadius else inactiveRadius
                    drawCircle(Color.White, radius = r + 3.dp.toPx(), center = Offset(point1X, xAxisY))
                    drawCircle(p1Color, radius = r, center = Offset(point1X, xAxisY))
                }

                // Point 2
                if (point2X > 0) {
                    val isActive = draggingPoint == 2
                    drawLine(
                        p2Color.copy(alpha = if (isActive) 1f else 0.7f),
                        start = Offset(point2X, 0f),
                        end = Offset(point2X, size.height),
                        strokeWidth = if (isActive) 2.5f.dp.toPx() else 1.5f.dp.toPx(),
                        pathEffect = dashEffect,
                    )
                    val r = if (isActive) activeRadius else inactiveRadius
                    drawCircle(Color.White, radius = r + 3.dp.toPx(), center = Offset(point2X, xAxisY))
                    drawCircle(p2Color, radius = r, center = Offset(point2X, xAxisY))
                }

                // Control tick marks (if calibration is ready)
                if (calibration != null && calibration.isValid) {
                    val minVal = minOf(v1 ?: 0f, v2 ?: 0f)
                    val maxVal = maxOf(v1 ?: 0f, v2 ?: 0f)
                    val range = maxVal - minVal
                    if (range > 0) {
                        val step = niceStep(range / 5)
                        var tick = (minVal / step).toInt() * step
                        while (tick <= maxVal + step) {
                            val pixelX = calibration.realToPixel(tick)
                            val viewX = (pixelX - graphRegion.x) * sx
                            if (viewX in 0f..size.width) {
                                drawLine(
                                    controlColor,
                                    start = Offset(viewX, xAxisY - 6.dp.toPx()),
                                    end = Offset(viewX, xAxisY + 6.dp.toPx()),
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

/**
 * Compute a "nice" tick step for a given range.
 */
internal fun niceStep(rough: Float): Float {
    val mag = 10.0.pow(kotlin.math.floor(kotlin.math.log10(rough.toDouble()))).toFloat()
    val frac = rough / mag
    return when {
        frac <= 1.5f -> mag
        frac <= 3.5f -> 2f * mag
        frac <= 7.5f -> 5f * mag
        else -> 10f * mag
    }
}

package com.chromalab.feature.processing.calibration

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
    onAccept: (YAxisCalibration) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewW by remember { mutableFloatStateOf(1f) }
    var viewH by remember { mutableFloatStateOf(1f) }

    val sy by remember(viewH, graphRegion.height) {
        mutableFloatStateOf(if (graphRegion.height > 0) viewH / graphRegion.height else 1f)
    }

    // Two calibration points (view Y coordinates)
    var point1Y by remember { mutableFloatStateOf(-1f) }
    var point2Y by remember { mutableFloatStateOf(-1f) }
    var value1 by remember { mutableStateOf("") }
    var value2 by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("mAU") }

    var settingPoint by remember { mutableIntStateOf(1) }

    val yAxisX = axes.yAxis?.let { (it.x1 - graphRegion.x) * (viewW / graphRegion.width) }
        ?: (viewW * 0.10f)

    val p1Color = Color(0xFFFF5722) // Orange
    val p2Color = Color(0xFF2196F3) // Blue
    val controlColor = Color(0xFF4CAF50) // Green

    // Build calibration for preview
    val v1 = value1.toFloatOrNull()
    val v2 = value2.toFloatOrNull()
    val calibration = if (point1Y > 0 && point2Y > 0 && v1 != null && v2 != null) {
        val pixelY1 = graphRegion.y + point1Y / sy
        val pixelY2 = graphRegion.y + point2Y / sy
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
                    if (result != null && result.hasWarnings) {
                        OutlinedButton(
                            onClick = { onAccept(result) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Принять с предупреждениями")
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

            // Overlay
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            if (settingPoint == 1) {
                                point1Y = offset.y
                                settingPoint = 2
                            } else {
                                point2Y = offset.y
                                settingPoint = 1
                            }
                        }
                    },
            ) {
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)

                // Y axis line
                drawLine(
                    Color(0xFF2196F3).copy(alpha = 0.5f),
                    start = Offset(yAxisX, 0f),
                    end = Offset(yAxisX, size.height),
                    strokeWidth = 1.5f.dp.toPx(),
                    pathEffect = dashEffect,
                )

                // Point 1 (lower)
                if (point1Y > 0) {
                    drawLine(
                        p1Color,
                        start = Offset(0f, point1Y),
                        end = Offset(size.width, point1Y),
                        strokeWidth = 1.5f.dp.toPx(),
                        pathEffect = dashEffect,
                    )
                    drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(yAxisX, point1Y))
                    drawCircle(p1Color, radius = 5.dp.toPx(), center = Offset(yAxisX, point1Y))
                }

                // Point 2 (upper)
                if (point2Y > 0) {
                    drawLine(
                        p2Color,
                        start = Offset(0f, point2Y),
                        end = Offset(size.width, point2Y),
                        strokeWidth = 1.5f.dp.toPx(),
                        pathEffect = dashEffect,
                    )
                    drawCircle(Color.White, radius = 8.dp.toPx(), center = Offset(yAxisX, point2Y))
                    drawCircle(p2Color, radius = 5.dp.toPx(), center = Offset(yAxisX, point2Y))
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
                            val viewY = (pixelY - graphRegion.y) * sy
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

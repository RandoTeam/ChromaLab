package com.chromalab.feature.processing.graph

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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chromalab.core.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * ROI editor for graph region selection.
 * Shows the image with a draggable/resizable rectangle.
 * The user can:
 * - Accept the auto-detected region
 * - Drag to move the region
 * - Drag edges/corners to resize
 * - See confidence level and warnings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphRoiEditorScreen(
    imagePath: String,
    autoResult: GraphRegionResult,
    onAccept: (GraphRegion) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewW by remember { mutableFloatStateOf(1f) }
    var viewH by remember { mutableFloatStateOf(1f) }

    val sx by remember(viewW, autoResult.imageWidth) {
        mutableFloatStateOf(if (autoResult.imageWidth > 0) viewW / autoResult.imageWidth else 1f)
    }
    val sy by remember(viewH, autoResult.imageHeight) {
        mutableFloatStateOf(if (autoResult.imageHeight > 0) viewH / autoResult.imageHeight else 1f)
    }

    val initial = autoResult.effectiveRegion
    var roiLeft by remember { mutableFloatStateOf(initial.x * sx) }
    var roiTop by remember { mutableFloatStateOf(initial.y * sy) }
    var roiRight by remember { mutableFloatStateOf((initial.x + initial.width) * sx) }
    var roiBottom by remember { mutableFloatStateOf((initial.y + initial.height) * sy) }

    LaunchedEffect(sx, sy) {
        roiLeft = initial.x * sx
        roiTop = initial.y * sy
        roiRight = (initial.x + initial.width) * sx
        roiBottom = (initial.y + initial.height) * sy
    }

    val accentColor = MaterialTheme.colorScheme.primary
    val confidenceColor = when (autoResult.confidence) {
        DetectionConfidence.HIGH -> MaterialTheme.colorScheme.tertiary
        DetectionConfidence.MEDIUM -> MaterialTheme.colorScheme.secondary
        DetectionConfidence.LOW -> MaterialTheme.colorScheme.error
        DetectionConfidence.MANUAL -> MaterialTheme.colorScheme.primary
    }
    val confidenceText = when (autoResult.confidence) {
        DetectionConfidence.HIGH -> "Высокая точность"
        DetectionConfidence.MEDIUM -> "Средняя точность"
        DetectionConfidence.LOW -> "Низкая точность"
        DetectionConfidence.MANUAL -> "Ручной выбор"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Область графика") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val region = GraphRegion(
                            x = (roiLeft / sx).toInt(),
                            y = (roiTop / sy).toInt(),
                            width = ((roiRight - roiLeft) / sx).toInt().coerceAtLeast(1),
                            height = ((roiBottom - roiTop) / sy).toInt().coerceAtLeast(1),
                        )
                        onAccept(region)
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
                    // Confidence badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (autoResult.confidence == DetectionConfidence.LOW) Icons.Filled.Warning
                            else Icons.Filled.Info,
                            contentDescription = null,
                            tint = confidenceColor,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            confidenceText,
                            style = MaterialTheme.typography.labelMedium,
                            color = confidenceColor,
                        )
                    }

                    // Warnings
                    autoResult.warnings.forEach { warning ->
                        Text(
                            warning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        "Перетащите рамку или измените её размер",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

            // ROI overlay
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        val edgeThreshold = 40f
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val pos = change.position - dragAmount

                            // Determine what to drag: edge or whole rect
                            val nearLeft = abs(pos.x - roiLeft) < edgeThreshold
                            val nearRight = abs(pos.x - roiRight) < edgeThreshold
                            val nearTop = abs(pos.y - roiTop) < edgeThreshold
                            val nearBottom = abs(pos.y - roiBottom) < edgeThreshold

                            if (nearLeft || nearRight || nearTop || nearBottom) {
                                // Resize
                                if (nearLeft) roiLeft = (roiLeft + dragAmount.x).coerceIn(0f, roiRight - 20f)
                                if (nearRight) roiRight = (roiRight + dragAmount.x).coerceIn(roiLeft + 20f, viewW)
                                if (nearTop) roiTop = (roiTop + dragAmount.y).coerceIn(0f, roiBottom - 20f)
                                if (nearBottom) roiBottom = (roiBottom + dragAmount.y).coerceIn(roiTop + 20f, viewH)
                            } else if (pos.x in roiLeft..roiRight && pos.y in roiTop..roiBottom) {
                                // Move whole rect
                                val w = roiRight - roiLeft
                                val h = roiBottom - roiTop
                                val newLeft = (roiLeft + dragAmount.x).coerceIn(0f, viewW - w)
                                val newTop = (roiTop + dragAmount.y).coerceIn(0f, viewH - h)
                                roiLeft = newLeft; roiTop = newTop
                                roiRight = newLeft + w; roiBottom = newTop + h
                            }
                        }
                    },
            ) {
                // Dim outside ROI
                val dimColor = Color.Black.copy(alpha = 0.5f)
                // Top
                drawRect(dimColor, Offset.Zero, Size(size.width, roiTop))
                // Bottom
                drawRect(dimColor, Offset(0f, roiBottom), Size(size.width, size.height - roiBottom))
                // Left
                drawRect(dimColor, Offset(0f, roiTop), Size(roiLeft, roiBottom - roiTop))
                // Right
                drawRect(dimColor, Offset(roiRight, roiTop), Size(size.width - roiRight, roiBottom - roiTop))

                // ROI border
                drawRect(
                    accentColor,
                    Offset(roiLeft, roiTop),
                    Size(roiRight - roiLeft, roiBottom - roiTop),
                    style = Stroke(width = 2.dp.toPx()),
                )

                // Corner handles
                val r = 8.dp.toPx()
                listOf(
                    Offset(roiLeft, roiTop),
                    Offset(roiRight, roiTop),
                    Offset(roiRight, roiBottom),
                    Offset(roiLeft, roiBottom),
                ).forEach { corner ->
                    drawCircle(Color.White, radius = r, center = corner)
                    drawCircle(accentColor, radius = r * 0.6f, center = corner)
                }

                // Edge midpoint handles
                val midR = 5.dp.toPx()
                listOf(
                    Offset((roiLeft + roiRight) / 2, roiTop),
                    Offset((roiLeft + roiRight) / 2, roiBottom),
                    Offset(roiLeft, (roiTop + roiBottom) / 2),
                    Offset(roiRight, (roiTop + roiBottom) / 2),
                ).forEach { mid ->
                    drawCircle(Color.White, radius = midR, center = mid)
                    drawCircle(accentColor.copy(alpha = 0.7f), radius = midR * 0.6f, center = mid)
                }
            }
        }
    }
}

package com.chromalab.feature.processing.curve

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chromalab.core.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * Editing mode for the curve editor.
 */
enum class CurveEditMode {
    /** View only — no editing */
    VIEW,
    /** Drag points to correct position */
    DRAG,
    /** Select region to delete */
    DELETE,
    /** Select region to mark as unreliable */
    MARK_UNRELIABLE,
}

/**
 * Manual curve review and correction screen.
 *
 * Features:
 * - Original graph as background
 * - Extracted curve overlay
 * - Mode-based editing: drag, delete, mark unreliable
 * - All edits logged for reproducibility
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurveEditorScreen(
    graphImagePath: String,
    initialPoints: List<CurvePoint>,
    graphWidth: Int,
    graphHeight: Int,
    onAccept: (List<CurvePoint>, ManualEditLog) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewW by remember { mutableFloatStateOf(1f) }
    var viewH by remember { mutableFloatStateOf(1f) }

    val sx by remember(viewW, graphWidth) {
        mutableFloatStateOf(if (graphWidth > 0) viewW / graphWidth else 1f)
    }
    val sy by remember(viewH, graphHeight) {
        mutableFloatStateOf(if (graphHeight > 0) viewH / graphHeight else 1f)
    }

    var points by remember { mutableStateOf(initialPoints.toMutableList()) }
    val edits = remember { mutableStateListOf<CurveEdit>() }
    var mode by remember { mutableStateOf(CurveEditMode.VIEW) }

    // Selection range for delete/mark
    var selectionStartX by remember { mutableFloatStateOf(-1f) }
    var selectionEndX by remember { mutableFloatStateOf(-1f) }

    // Unreliable regions
    val unreliableRanges = remember { mutableStateListOf<Pair<Int, Int>>() }

    val originalCount = initialPoints.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Коррекция кривой") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val log = ManualEditLog(
                            edits = edits.toList(),
                            originalPointCount = originalCount,
                            finalPointCount = points.size,
                            timestamp = System.currentTimeMillis(),
                        )
                        onAccept(points.toList(), log)
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
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    // Mode selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        FilterChip(
                            selected = mode == CurveEditMode.VIEW,
                            onClick = { mode = CurveEditMode.VIEW },
                            label = { Text("Просмотр") },
                        )
                        FilterChip(
                            selected = mode == CurveEditMode.DRAG,
                            onClick = { mode = CurveEditMode.DRAG },
                            label = { Text("Корректировка") },
                        )
                        FilterChip(
                            selected = mode == CurveEditMode.DELETE,
                            onClick = { mode = CurveEditMode.DELETE },
                            label = { Text("Удалить") },
                            leadingIcon = {
                                Icon(Icons.Filled.Delete, null, modifier = Modifier.size(16.dp))
                            },
                        )
                        FilterChip(
                            selected = mode == CurveEditMode.MARK_UNRELIABLE,
                            onClick = { mode = CurveEditMode.MARK_UNRELIABLE },
                            label = { Text("⚠") },
                        )
                    }

                    // Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            "Точек: ${points.size}/$originalCount",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            "Правок: ${edits.size}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                        if (unreliableRanges.isNotEmpty()) {
                            Text(
                                "Ненадёжных: ${unreliableRanges.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }

                    // Hint
                    Text(
                        when (mode) {
                            CurveEditMode.VIEW -> "Просмотр — переключите режим для редактирования"
                            CurveEditMode.DRAG -> "Перетащите точку кривой для коррекции"
                            CurveEditMode.DELETE -> "Проведите по участку для удаления"
                            CurveEditMode.MARK_UNRELIABLE -> "Проведите по участку для отметки как ненадёжный"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    // Reset button
                    if (edits.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                points = initialPoints.toMutableList()
                                edits.clear()
                                unreliableRanges.clear()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Сбросить все правки")
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
            // Background graph image
            AsyncImage(
                model = graphImagePath,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )

            // Interactive overlay
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(mode) {
                        when (mode) {
                            CurveEditMode.DRAG -> {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val tapX = change.position.x / sx
                                    val tapY = change.position.y / sy

                                    // Find nearest point within ±10px
                                    val nearest = points
                                        .withIndex()
                                        .minByOrNull {
                                            kotlin.math.abs(it.value.pixelX - tapX)
                                        }

                                    if (nearest != null &&
                                        kotlin.math.abs(nearest.value.pixelX - tapX) < 15
                                    ) {
                                        val newY = tapY.coerceIn(0f, graphHeight.toFloat())
                                        val idx = nearest.index
                                        points = points.toMutableList().also { list ->
                                            list[idx] = nearest.value.copy(
                                                pixelY = newY,
                                                confidence = CurvePoint.HIGH_CONFIDENCE,
                                            )
                                        }
                                        edits.add(
                                            CurveEdit(
                                                type = CurveEdit.EditType.MOVE,
                                                fromX = nearest.value.pixelX,
                                                toX = nearest.value.pixelX,
                                                newY = newY,
                                                timestamp = System.currentTimeMillis(),
                                            ),
                                        )
                                    }
                                }
                            }

                            CurveEditMode.DELETE, CurveEditMode.MARK_UNRELIABLE -> {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        selectionStartX = offset.x
                                        selectionEndX = offset.x
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        selectionEndX = change.position.x
                                    },
                                    onDragEnd = {
                                        val x1 = (minOf(selectionStartX, selectionEndX) / sx).roundToInt()
                                        val x2 = (maxOf(selectionStartX, selectionEndX) / sx).roundToInt()

                                        if (mode == CurveEditMode.DELETE) {
                                            points = points.filterNot { it.pixelX in x1..x2 }.toMutableList()
                                            edits.add(
                                                CurveEdit(
                                                    type = CurveEdit.EditType.DELETE,
                                                    fromX = x1,
                                                    toX = x2,
                                                    timestamp = System.currentTimeMillis(),
                                                ),
                                            )
                                        } else {
                                            unreliableRanges.add(x1 to x2)
                                            // Lower confidence for points in range
                                            points = points.map { p ->
                                                if (p.pixelX in x1..x2) {
                                                    p.copy(confidence = CurvePoint.LOW_CONFIDENCE)
                                                } else p
                                            }.toMutableList()
                                            edits.add(
                                                CurveEdit(
                                                    type = CurveEdit.EditType.MARK_UNRELIABLE,
                                                    fromX = x1,
                                                    toX = x2,
                                                    timestamp = System.currentTimeMillis(),
                                                ),
                                            )
                                        }
                                        selectionStartX = -1f
                                        selectionEndX = -1f
                                    },
                                )
                            }

                            else -> {
                                // VIEW mode — no interaction
                                detectTapGestures { }
                            }
                        }
                    },
            ) {
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 3f), 0f)

                // Draw unreliable regions (orange translucent)
                for ((rx1, rx2) in unreliableRanges) {
                    drawRect(
                        Color(0x30FF9800),
                        topLeft = Offset(rx1 * sx, 0f),
                        size = androidx.compose.ui.geometry.Size(
                            (rx2 - rx1) * sx,
                            size.height,
                        ),
                    )
                }

                // Draw selection region
                if (selectionStartX >= 0 && selectionEndX >= 0) {
                    val left = minOf(selectionStartX, selectionEndX)
                    val right = maxOf(selectionStartX, selectionEndX)
                    val selColor = if (mode == CurveEditMode.DELETE) {
                        Color(0x40FF1744)
                    } else {
                        Color(0x40FF9800)
                    }
                    drawRect(
                        selColor,
                        topLeft = Offset(left, 0f),
                        size = androidx.compose.ui.geometry.Size(right - left, size.height),
                    )
                }

                // Draw curve
                val sorted = points.sortedBy { it.pixelX }
                for (i in 0 until sorted.size - 1) {
                    val p1 = sorted[i]
                    val p2 = sorted[i + 1]
                    if (p2.pixelX - p1.pixelX > 5) continue

                    val color = when {
                        p1.confidence == CurvePoint.INTERPOLATED -> Color(0xFFFFEB3B)
                        p1.confidence < CurvePoint.HIGH_CONFIDENCE -> Color(0xFFFF9800)
                        else -> Color(0xFFFF1744)
                    }

                    drawLine(
                        color,
                        start = Offset(p1.pixelX * sx, p1.pixelY * sy),
                        end = Offset(p2.pixelX * sx, p2.pixelY * sy),
                        strokeWidth = 2.dp.toPx(),
                    )
                }

                // In DRAG mode, show handle dots
                if (mode == CurveEditMode.DRAG) {
                    // Show every Nth point to avoid clutter
                    val step = maxOf(1, sorted.size / 50)
                    for (i in sorted.indices step step) {
                        val p = sorted[i]
                        drawCircle(
                            Color.White,
                            radius = 5.dp.toPx(),
                            center = Offset(p.pixelX * sx, p.pixelY * sy),
                        )
                        drawCircle(
                            Color(0xFFFF1744),
                            radius = 3.dp.toPx(),
                            center = Offset(p.pixelX * sx, p.pixelY * sy),
                        )
                    }
                }
            }
        }
    }
}

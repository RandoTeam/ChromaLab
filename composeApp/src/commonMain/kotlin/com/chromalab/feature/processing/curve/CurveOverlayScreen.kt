package com.chromalab.feature.processing.curve

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chromalab.core.ui.theme.Spacing

/**
 * Curve extraction result screen.
 * Shows the extracted curve overlaid on the graph image.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurveOverlayScreen(
    graphImagePath: String,
    result: CurveExtractionResult,
    graphWidth: Int,
    graphHeight: Int,
    onAccept: () -> Unit,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Извлечённая кривая") },
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
                ) {
                    // Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        StatItem("Точек", "${result.points.size}")
                        StatItem("Покрытие", "${(result.coverage * 100).toInt()}%")
                        StatItem("Интерполировано", "${result.interpolatedColumns}")
                        StatItem("Выбросов", "${result.outlierCount}")
                    }

                    // Warnings
                    result.warnings.forEach { warning ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
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
                model = graphImagePath,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )

            // Curve overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sorted = result.points.sortedBy { it.pixelX }

                for (i in 0 until sorted.size - 1) {
                    val p1 = sorted[i]
                    val p2 = sorted[i + 1]
                    if (p2.pixelX - p1.pixelX > 3) continue

                    val color = when {
                        p1.confidence == CurvePoint.INTERPOLATED -> Color(0xFFFFEB3B) // Yellow
                        p1.confidence < CurvePoint.HIGH_CONFIDENCE -> Color(0xFFFF9800) // Orange
                        else -> Color(0xFFFF1744) // Red
                    }

                    drawLine(
                        color,
                        start = Offset(p1.pixelX * sx, p1.pixelY * sy),
                        end = Offset(p2.pixelX * sx, p2.pixelY * sy),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

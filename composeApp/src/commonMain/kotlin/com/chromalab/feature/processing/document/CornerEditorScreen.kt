package com.chromalab.feature.processing.document

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.chromalab.core.ui.theme.Spacing
import kotlin.math.sqrt

/**
 * Manual corner editor for document bounds.
 * Shows the image with draggable corner handles.
 * User can adjust or set all 4 corners manually.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CornerEditorScreen(
    imagePath: String,
    initialBounds: DocumentBounds?,
    imageWidth: Int,
    imageHeight: Int,
    onAccept: (DocumentCorners) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewW by remember { mutableFloatStateOf(1f) }
    var viewH by remember { mutableFloatStateOf(1f) }

    // Scale factors: image coords → view coords
    val sx by remember(viewW, imageWidth) { mutableFloatStateOf(if (imageWidth > 0) viewW / imageWidth else 1f) }
    val sy by remember(viewH, imageHeight) { mutableFloatStateOf(if (imageHeight > 0) viewH / imageHeight else 1f) }

    // Corner positions in view coordinates
    val defaultCorners = initialBounds?.effectiveCorners ?: DocumentCorners(
        topLeft = ImagePoint(imageWidth * 0.1f, imageHeight * 0.1f),
        topRight = ImagePoint(imageWidth * 0.9f, imageHeight * 0.1f),
        bottomRight = ImagePoint(imageWidth * 0.9f, imageHeight * 0.9f),
        bottomLeft = ImagePoint(imageWidth * 0.1f, imageHeight * 0.9f),
    )

    var tl by remember { mutableStateOf(Offset(defaultCorners.topLeft.x * sx, defaultCorners.topLeft.y * sy)) }
    var tr by remember { mutableStateOf(Offset(defaultCorners.topRight.x * sx, defaultCorners.topRight.y * sy)) }
    var br by remember { mutableStateOf(Offset(defaultCorners.bottomRight.x * sx, defaultCorners.bottomRight.y * sy)) }
    var bl by remember { mutableStateOf(Offset(defaultCorners.bottomLeft.x * sx, defaultCorners.bottomLeft.y * sy)) }

    // Update when view size changes
    LaunchedEffect(sx, sy) {
        tl = Offset(defaultCorners.topLeft.x * sx, defaultCorners.topLeft.y * sy)
        tr = Offset(defaultCorners.topRight.x * sx, defaultCorners.topRight.y * sy)
        br = Offset(defaultCorners.bottomRight.x * sx, defaultCorners.bottomRight.y * sy)
        bl = Offset(defaultCorners.bottomLeft.x * sx, defaultCorners.bottomLeft.y * sy)
    }

    val accentColor = MaterialTheme.colorScheme.primary
    val handleRadius = 16.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Углы документа") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Reset to defaults
                        tl = Offset(viewW * 0.1f, viewH * 0.1f)
                        tr = Offset(viewW * 0.9f, viewH * 0.1f)
                        br = Offset(viewW * 0.9f, viewH * 0.9f)
                        bl = Offset(viewW * 0.1f, viewH * 0.9f)
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Сброс")
                    }
                    IconButton(onClick = {
                        onAccept(
                            DocumentCorners(
                                topLeft = ImagePoint(tl.x / sx, tl.y / sy),
                                topRight = ImagePoint(tr.x / sx, tr.y / sy),
                                bottomRight = ImagePoint(br.x / sx, br.y / sy),
                                bottomLeft = ImagePoint(bl.x / sx, bl.y / sy),
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
                Text(
                    "Перетащите углы для точной разметки листа",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(Spacing.md),
                )
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
            // Image
            AsyncImage(
                model = imagePath,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )

            // Quad overlay + handles
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val pos = change.position
                            val r = handleRadius.toPx()

                            // Find nearest corner
                            val corners = listOf(tl, tr, br, bl)
                            val nearest = corners.minByOrNull {
                                sqrt(
                                    (it.x - pos.x + dragAmount.x) * (it.x - pos.x + dragAmount.x) +
                                        (it.y - pos.y + dragAmount.y) * (it.y - pos.y + dragAmount.y),
                                )
                            }
                            when (nearest) {
                                tl -> tl = (tl + dragAmount).coerceIn(viewW, viewH)
                                tr -> tr = (tr + dragAmount).coerceIn(viewW, viewH)
                                br -> br = (br + dragAmount).coerceIn(viewW, viewH)
                                bl -> bl = (bl + dragAmount).coerceIn(viewW, viewH)
                            }
                        }
                    },
            ) {
                val r = handleRadius.toPx()

                // Draw quadrilateral
                val path = Path().apply {
                    moveTo(tl.x, tl.y)
                    lineTo(tr.x, tr.y)
                    lineTo(br.x, br.y)
                    lineTo(bl.x, bl.y)
                    close()
                }
                drawPath(path, accentColor.copy(alpha = 0.2f))
                drawPath(path, accentColor, style = Stroke(width = 2.dp.toPx()))

                // Draw handles
                listOf(tl, tr, br, bl).forEach { corner ->
                    drawCircle(Color.White, radius = r, center = corner)
                    drawCircle(accentColor, radius = r * 0.6f, center = corner)
                }
            }
        }
    }
}

private operator fun Offset.plus(other: Offset): Offset =
    Offset(x + other.x, y + other.y)

private fun Offset.coerceIn(maxW: Float, maxH: Float): Offset =
    Offset(x.coerceIn(0f, maxW), y.coerceIn(0f, maxH))

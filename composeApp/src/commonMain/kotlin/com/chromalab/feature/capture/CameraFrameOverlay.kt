package com.chromalab.feature.capture

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.chromalab.core.common.Strings

/**
 * Semi-transparent overlay with animated frame guide for camera viewfinder.
 * The frame indicates where the chromatogram sheet should be placed.
 */
@Composable
fun CameraFrameOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "frame")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    val frameColor = MaterialTheme.colorScheme.primary
    val dimColor = Color.Black.copy(alpha = 0.5f)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val padding = 32.dp.toPx()
            val cornerLen = 40.dp.toPx()
            val strokeW = 3.dp.toPx()
            val cornerR = 12.dp.toPx()

            // Frame rect — 80% width, ~60% height, centered
            val frameW = size.width - padding * 2
            val frameH = frameW * 1.35f // ~A4 portrait ratio
            val frameX = padding
            val frameY = (size.height - frameH) / 2

            val frameRect = Rect(frameX, frameY, frameX + frameW, frameY + frameH)

            // Dim outside frame
            val framePath = Path().apply {
                addRoundRect(RoundRect(frameRect, CornerRadius(cornerR)))
            }
            clipPath(framePath, clipOp = ClipOp.Difference) {
                drawRect(dimColor)
            }

            // Animated corner brackets
            val color = frameColor.copy(alpha = pulseAlpha)
            val stroke = Stroke(width = strokeW, cap = StrokeCap.Round)

            // Top-left corner
            drawLine(color, Offset(frameX, frameY + cornerLen), Offset(frameX, frameY + cornerR), stroke.width)
            drawLine(color, Offset(frameX + cornerR, frameY), Offset(frameX + cornerLen, frameY), stroke.width)

            // Top-right corner
            val rx = frameX + frameW
            drawLine(color, Offset(rx, frameY + cornerLen), Offset(rx, frameY + cornerR), stroke.width)
            drawLine(color, Offset(rx - cornerR, frameY), Offset(rx - cornerLen, frameY), stroke.width)

            // Bottom-left corner
            val by2 = frameY + frameH
            drawLine(color, Offset(frameX, by2 - cornerLen), Offset(frameX, by2 - cornerR), stroke.width)
            drawLine(color, Offset(frameX + cornerR, by2), Offset(frameX + cornerLen, by2), stroke.width)

            // Bottom-right corner
            drawLine(color, Offset(rx, by2 - cornerLen), Offset(rx, by2 - cornerR), stroke.width)
            drawLine(color, Offset(rx - cornerR, by2), Offset(rx - cornerLen, by2), stroke.width)
        }

        // Hint text
        Text(
            text = Strings.hintPlaceChromatogram,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp),
        )
    }
}

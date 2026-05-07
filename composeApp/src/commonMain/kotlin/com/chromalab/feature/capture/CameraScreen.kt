package com.chromalab.feature.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific camera screen.
 * Android: CameraX Preview + ImageCapture with overlay frame.
 * Desktop: Stub placeholder.
 */
@Composable
expect fun CameraScreen(
    onImageCaptured: (imagePath: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
)

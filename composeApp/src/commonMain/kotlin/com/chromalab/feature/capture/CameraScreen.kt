package com.chromalab.feature.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific camera screen.
 * Android: Smart Scan document scanner route; manual CameraX lives in ManualCameraScreen.
 * Desktop: Stub placeholder.
 */
@Composable
expect fun CameraScreen(
    onImageCaptured: (imagePath: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
)

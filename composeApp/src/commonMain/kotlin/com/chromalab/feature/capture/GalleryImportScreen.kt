package com.chromalab.feature.capture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Platform-specific gallery image picker.
 * Android: uses ActivityResultContracts.PickVisualMedia.
 * Desktop: stub.
 */
@Composable
expect fun GalleryImportScreen(
    onImageSelected: (imagePath: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
)

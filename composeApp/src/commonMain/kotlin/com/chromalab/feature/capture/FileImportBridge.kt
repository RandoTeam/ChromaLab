package com.chromalab.feature.capture

import androidx.compose.runtime.Composable

/**
 * Platform-specific file import composable.
 *
 * On Android: wraps FileImportScreen with SAF file picker.
 * Reads selected file content and passes it to the common screen.
 */
@Composable
expect fun FileImportScreenWithPicker(
    onSaved: (signalId: Long) -> Unit,
    onBack: () -> Unit,
)

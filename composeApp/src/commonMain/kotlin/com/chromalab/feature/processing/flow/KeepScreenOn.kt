package com.chromalab.feature.processing.flow

import androidx.compose.runtime.Composable

/**
 * Platform-specific composable that prevents the screen from turning off
 * while [enabled] is true. Used during long-running inference / processing.
 */
@Composable
expect fun KeepScreenOn(enabled: Boolean)

package com.chromalab.feature.processing.flow

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import android.view.WindowManager

/**
 * Android implementation: sets FLAG_KEEP_SCREEN_ON on the Activity window.
 * Automatically clears the flag when [enabled] becomes false or the composable
 * leaves composition.
 */
@Composable
actual fun KeepScreenOn(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val window = (context as? Activity)?.window
        if (enabled) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

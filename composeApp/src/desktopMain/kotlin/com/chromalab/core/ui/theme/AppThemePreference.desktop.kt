package com.chromalab.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
actual fun rememberAppThemePreference(): AppThemePreference {
    var mode by remember { mutableStateOf(AppThemeMode.SYSTEM) }
    return AppThemePreference(
        mode = mode,
        setMode = { next -> mode = next },
    )
}

package com.chromalab.core.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

private const val THEME_PREFS = "chromalab_ui_preferences"
private const val THEME_MODE_KEY = "theme_mode"

@Composable
actual fun rememberAppThemePreference(): AppThemePreference {
    val context = LocalContext.current.applicationContext
    val prefs = remember(context) {
        context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
    }
    var mode by remember(prefs) {
        mutableStateOf(AppThemeMode.fromStoredName(prefs.getString(THEME_MODE_KEY, null)))
    }
    val setMode: (AppThemeMode) -> Unit = remember(prefs) {
        { next ->
            mode = next
            prefs.edit().putString(THEME_MODE_KEY, next.name).apply()
        }
    }

    return AppThemePreference(
        mode = mode,
        setMode = setMode,
    )
}

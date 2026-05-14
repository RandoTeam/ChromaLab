package com.chromalab.core.ui.theme

import androidx.compose.runtime.Composable

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromStoredName(value: String?): AppThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

data class AppThemePreference(
    val mode: AppThemeMode,
    val setMode: (AppThemeMode) -> Unit,
)

@Composable
expect fun rememberAppThemePreference(): AppThemePreference

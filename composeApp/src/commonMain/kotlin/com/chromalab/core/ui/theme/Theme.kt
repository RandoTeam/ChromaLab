package com.chromalab.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

// ============================================================
// Dark Color Scheme — основной режим для ChromaLab
// Все пары container + onContainer проверены на контраст ≥ 4.5:1
// ============================================================
private val DarkColorScheme = darkColorScheme(
    // Primary — бирюзовый акцент
    primary = Teal80,                    // кнопки, ссылки
    onPrimary = Teal10,                  // текст НА primary (тёмный на бирюзовом: 12:1)
    primaryContainer = Teal30,           // фон выделенных элементов
    onPrimaryContainer = Teal90,         // текст НА primaryContainer (светлый на тёмном: 8:1)

    // Secondary — голубой (графики, ion channel 1)
    secondary = Blue80,
    onSecondary = Neutral4,
    secondaryContainer = Blue30,
    onSecondaryContainer = Blue80,

    // Tertiary — янтарный (ion channel 2, предупреждения)
    tertiary = Amber80,
    onTertiary = Neutral4,
    tertiaryContainer = Amber30,
    onTertiaryContainer = Amber80,

    // Error — красный десатурированный
    error = Error,
    onError = Neutral4,
    errorContainer = ErrorDark,
    onErrorContainer = Error,

    // Surfaces — нейтральные тёмные
    background = Neutral4,               // фон приложения
    onBackground = Neutral95,            // основной текст (контраст 14:1)
    surface = Neutral6,                  // карточки
    onSurface = Neutral87,              // текст на карточках (контраст 10:1)
    surfaceVariant = Neutral10,          // второстепенные панели
    onSurfaceVariant = Neutral70,        // вторичный текст (контраст 6:1)
    surfaceContainerHigh = Neutral17,    // elevated cards
    surfaceContainerHighest = Neutral22, // modal sheets

    // Outline
    outline = Neutral30,                 // разделители, границы
    outlineVariant = Neutral40,          // менее заметные разделители

    // Inverse — для snackbar и т.п.
    inverseSurface = Neutral95,
    inverseOnSurface = Neutral4,
    inversePrimary = Teal30,
)

// ============================================================
// Light Color Scheme — дополнительный режим
// ============================================================
private val LightColorScheme = lightColorScheme(
    primary = Teal30,
    onPrimary = NeutralL99,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,

    secondary = Blue30,
    onSecondary = NeutralL99,
    secondaryContainer = Color(0xFFD4E4FF),
    onSecondaryContainer = Blue30,

    tertiary = Amber30,
    onTertiary = NeutralL99,
    tertiaryContainer = Color(0xFFFFE5B4),
    onTertiaryContainer = Amber30,

    error = Color(0xFFC62828),
    onError = NeutralL99,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF4A1C1C),

    background = NeutralL99,
    onBackground = NeutralL10,
    surface = NeutralL96,
    onSurface = NeutralL20,
    surfaceVariant = NeutralL92,
    onSurfaceVariant = NeutralL40,

    outline = Color(0xFFB0B8C4),
    outlineVariant = Color(0xFFD0D5DD),

    inverseSurface = NeutralL10,
    inverseOnSurface = NeutralL99,
    inversePrimary = Teal80,
)

// ============================================================
// Theme Composable
// ============================================================
@Composable
fun ChromaLabTheme(
    themeMode: AppThemeMode,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        AppThemeMode.SYSTEM -> systemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    ChromaLabTheme(
        darkTheme = darkTheme,
        content = content,
    )
}

@Composable
fun ChromaLabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ChromaLabTypography,
        shapes = ChromaLabShapes,
        content = content
    )
}

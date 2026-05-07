package com.chromalab.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

// --- Colors ---
private val Primary = Color(0xFF00D4AA)
private val PrimaryContainer = Color(0xFF003D32)
private val Secondary = Color(0xFF64B5F6)
private val Tertiary = Color(0xFFFFB74D)
private val ErrorColor = Color(0xFFEF5350)
private val BackgroundDark = Color(0xFF0F1318)
private val SurfaceDark = Color(0xFF1A1F27)
private val OnBackgroundDark = Color(0xFFE8ECF0)
private val OnSurfaceDark = Color(0xFFC5CBD3)
private val OutlineDark = Color(0xFF3A4250)
private val BackgroundLight = Color(0xFFFAFCFE)
private val SurfaceLight = Color(0xFFF0F3F7)
private val PrimaryLight = Color(0xFF00897B)
private val OnBackgroundLight = Color(0xFF1A1F27)

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    primaryContainer = PrimaryContainer,
    secondary = Secondary,
    tertiary = Tertiary,
    error = ErrorColor,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark,
    outline = OutlineDark,
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    primaryContainer = PrimaryContainer,
    secondary = Secondary,
    tertiary = Tertiary,
    error = ErrorColor,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = OnBackgroundLight,
    onSurface = OnBackgroundLight,
)

@Composable
fun ChromaLabTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

package com.chromalab.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Science
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Bottom navigation tab definition.
 */
enum class BottomTab(
    val label: String,
    val icon: ImageVector,
    val route: Route,
) {
    Projects(
        label = "Проекты",
        icon = Icons.Filled.Science,
        route = Route.ProjectList,
    ),
    Capture(
        label = "Съёмка",
        icon = Icons.Filled.PhotoCamera,
        route = Route.Capture,
    ),
    Calculations(
        label = "Расчёты",
        icon = Icons.Filled.Analytics,
        route = Route.Calculations,
    ),
    More(
        label = "Ещё",
        icon = Icons.Filled.MoreHoriz,
        route = Route.More,
    ),
}

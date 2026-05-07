package com.chromalab.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Science
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.chromalab.core.common.Strings

/**
 * Bottom navigation tab definition.
 */
enum class BottomTab(
    val icon: ImageVector,
    val route: Route,
) {
    Projects(icon = Icons.Filled.Science, route = Route.ProjectList),
    Capture(icon = Icons.Filled.PhotoCamera, route = Route.Capture),
    Calculations(icon = Icons.Filled.Analytics, route = Route.Calculations),
    More(icon = Icons.Filled.MoreHoriz, route = Route.More),
    ;

    val label: String
        @Composable get() = when (this) {
            Projects -> Strings.tabProjects
            Capture -> Strings.tabCapture
            Calculations -> Strings.tabCalculations
            More -> Strings.tabMore
        }
}

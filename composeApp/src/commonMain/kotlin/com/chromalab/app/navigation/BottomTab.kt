package com.chromalab.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.chromalab.core.common.AppLanguage
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
    Chats(icon = Icons.Filled.Chat, route = Route.Chats),
    More(icon = Icons.Filled.Settings, route = Route.More),
    ;

    val label: String
        @Composable get() = when (this) {
            Projects -> Strings.tabProjects
            Capture -> Strings.tabCapture
            Calculations -> Strings.tabCalculations
            Chats -> if (Strings.language == AppLanguage.RU) "\u0427\u0430\u0442\u044b" else "Chats"
            More -> Strings.tabMore
        }
}

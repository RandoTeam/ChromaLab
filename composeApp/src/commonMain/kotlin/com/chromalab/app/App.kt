package com.chromalab.app

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.chromalab.app.navigation.BottomTab
import com.chromalab.app.navigation.PlaceholderScreen
import com.chromalab.app.navigation.Route
import com.chromalab.core.common.Strings
import com.chromalab.core.ui.theme.ChromaLabTheme
import com.chromalab.feature.capture.CameraScreen
import com.chromalab.feature.settings.LanguageScreen

@Composable
fun App() {
    ChromaLabTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    BottomTab.entries.forEach { tab ->
                        val selected = backStackEntry?.destination?.hasRoute(tab.route::class) == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Route.ProjectList,
                modifier = Modifier.fillMaxSize().padding(padding),
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                // --- Bottom tabs ---
                composable<Route.ProjectList> {
                    PlaceholderScreen(Strings.tabProjects)
                }
                composable<Route.Capture> {
                    PlaceholderScreen(Strings.tabCapture)
                }
                composable<Route.Calculations> {
                    PlaceholderScreen(Strings.tabCalculations)
                }
                composable<Route.More> {
                    PlaceholderScreen(Strings.tabMore)
                }

                // --- Projects ---
                composable<Route.ProjectDetail> {
                    PlaceholderScreen("Проект — детали")
                }
                composable<Route.SampleDetail> {
                    PlaceholderScreen("Образец — детали")
                }
                composable<Route.NewProject> {
                    PlaceholderScreen("Новый проект")
                }

                // --- Capture ---
                composable<Route.Camera> {
                    CameraScreen(
                        onImageCaptured = { path ->
                            // TODO: navigate to processing with captured image
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<Route.GalleryFrame> {
                    PlaceholderScreen("Галерея → Рамка")
                }
                composable<Route.FileImport> {
                    PlaceholderScreen("Импорт файла")
                }
                composable<Route.Processing> {
                    PlaceholderScreen("Обработка")
                }

                // --- Calculations ---
                composable<Route.ChromatogramView> {
                    PlaceholderScreen("Хроматограмма")
                }
                composable<Route.IonRatio> {
                    PlaceholderScreen("Ion Ratio")
                }
                composable<Route.Calibration> {
                    PlaceholderScreen("Калибровка")
                }

                // --- More ---
                composable<Route.Reports> {
                    PlaceholderScreen("Отчёты")
                }
                composable<Route.Settings> {
                    PlaceholderScreen(Strings.settingsAbout)
                }
                composable<Route.Language> {
                    LanguageScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<Route.About> {
                    PlaceholderScreen(Strings.settingsAbout)
                }
            }
        }
    }
}

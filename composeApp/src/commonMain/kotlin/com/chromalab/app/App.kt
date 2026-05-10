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
import androidx.navigation.toRoute
import com.chromalab.app.navigation.BottomTab
import com.chromalab.app.navigation.PlaceholderScreen
import com.chromalab.app.navigation.Route
import com.chromalab.feature.processing.flow.ProcessingFlowScreen
import com.chromalab.feature.calculation.flow.AnalysisFlowScreen
import com.chromalab.core.common.Strings
import com.chromalab.core.ui.theme.ChromaLabTheme
import com.chromalab.feature.capture.CameraScreen
import com.chromalab.feature.capture.CaptureHubScreen

import com.chromalab.feature.settings.LanguageScreen
import com.chromalab.feature.settings.MoreScreen
import com.chromalab.feature.settings.ModelManagerScreen

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
                    CaptureHubScreen(
                        onCamera = { navController.navigate(Route.Camera) },
                    )
                }
                composable<Route.Calculations> {
                    PlaceholderScreen(Strings.tabCalculations)
                }
                composable<Route.More> {
                    MoreScreen(
                        activeModelName = null, // TODO: wire from ModelManager
                        threadCount = 4,        // TODO: wire from ModelManager
                        onOpenModelManager = { navController.navigate(Route.ModelManager) },
                        onOpenLanguage = { navController.navigate(Route.Language) },
                        onOpenAbout = { navController.navigate(Route.About) },
                        onThreadCountChange = { /* TODO: wire to ModelManager.threadCount */ },
                    )
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
                            navController.navigate(Route.Processing(path)) {
                                popUpTo(Route.Capture) { inclusive = false }
                            }
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable<Route.FileImport> {
                    PlaceholderScreen("Импорт файла")
                }
                composable<Route.Processing> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.Processing>()
                    val imageUri = route.imageUri
                    ProcessingFlowScreen(
                        imagePath = imageUri,
                        onFinish = { signalId ->
                            // Phase 1→2 bridge: navigate to analysis with Room signal ID
                            navController.navigate(Route.Analysis(signalId.toString())) {
                                popUpTo(Route.Capture) { inclusive = false }
                            }
                        },
                        onCancel = {
                            navController.popBackStack(Route.Capture, inclusive = false)
                        },
                    )
                }

                // --- Calculations ---
                composable<Route.ChromatogramView> {
                    PlaceholderScreen("Хроматограмма")
                }
                composable<Route.Analysis> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.Analysis>()
                    val signalId = route.signalId
                    AnalysisFlowScreen(
                        signalId = signalId,
                        onFinish = {
                            navController.popBackStack(Route.Calculations, inclusive = false)
                        },
                        onCancel = {
                            navController.popBackStack(Route.Calculations, inclusive = false)
                        },
                    )
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
                composable<Route.ModelManager> {
                    ModelManagerScreen(
                        downloadedModelIds = emptySet(), // TODO: wire from ModelManager
                        activeModelId = null,
                        downloadingModelId = null,
                        downloadProgress = 0f,
                        downloadSpeedMbps = 0f,
                        deviceRamMb = 8192,
                        availableStorageGb = 32f,
                        totalModelDiskUsageGb = 0f,
                        onDownload = { /* TODO: wire download */ },
                        onDelete = { /* TODO: wire delete */ },
                        onActivate = { /* TODO: wire activate */ },
                        onImport = { /* TODO: wire file picker */ },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

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
import com.chromalab.feature.calculation.screen.CalculationsListScreen
import com.chromalab.core.common.Strings
import com.chromalab.core.ui.theme.ChromaLabTheme
import com.chromalab.feature.capture.CameraScreen
import com.chromalab.feature.capture.CaptureHubScreen
import com.chromalab.feature.capture.FileImportScreenWithPicker
import com.chromalab.feature.chat.ChatModelOption
import com.chromalab.feature.chat.ChatScreen
import com.chromalab.feature.chat.rememberChatState
import com.chromalab.feature.processing.inference.ModelRuntime
import com.chromalab.feature.processing.model.ModelRegistry

import com.chromalab.feature.settings.LanguageScreen
import com.chromalab.feature.settings.AboutScreen
import com.chromalab.feature.settings.MoreScreen
import com.chromalab.feature.settings.ModelManagerScreen
import com.chromalab.feature.settings.ModelManagerState
import com.chromalab.feature.settings.rememberModelManagerState

@Composable
fun App() {
    ChromaLabTheme {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()

        // Model Manager — platform-specific state + actions
        val (modelState, modelActions) = rememberModelManagerState()
        val (chatState, chatActions) = rememberChatState(
            activeModelId = modelState.activeModelId,
            activeModelName = modelState.activeModelName,
        )

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
                        onImportFile = { navController.navigate(Route.FileImport) },
                    )
                }
                composable<Route.Calculations> {
                    CalculationsListScreen(
                        onAnalyze = { signalId ->
                            navController.navigate(Route.Analysis(signalId))
                        },
                    )
                }
                composable<Route.Chats> {
                    val chatModelOptions = modelState.toChatModelOptions()
                    ChatScreen(
                        state = chatState,
                        actions = chatActions,
                        modelOptions = chatModelOptions,
                        onSelectModel = { option ->
                            if (option.id != modelState.activeModelId) {
                                modelActions.activate(option.id)
                            }
                        },
                        onOpenModelManager = { navController.navigate(Route.ModelManager) },
                    )
                }
                composable<Route.More> {
                    MoreScreen(
                        activeModelName = modelState.activeModelName,
                        activeModelSummary = modelState.activeModelSummary,
                        threadCount = modelState.threadCount,
                        autoUnloadMinutes = modelState.autoUnloadMinutes,
                        onOpenModelManager = { navController.navigate(Route.ModelManager) },
                        onOpenLanguage = { navController.navigate(Route.Language) },
                        onOpenAbout = { navController.navigate(Route.About) },
                        onThreadCountChange = { modelActions.setThreadCount(it) },
                        onAutoUnloadChange = { modelActions.setAutoUnloadMinutes(it) },
                    )
                }

                // --- Capture flow ---
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
                    FileImportScreenWithPicker(
                        onSaved = { signalId ->
                            navController.navigate(Route.Analysis(signalId.toString())) {
                                popUpTo(Route.Capture) { inclusive = false }
                            }
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<Route.Processing> { backStackEntry ->
                    val route = backStackEntry.toRoute<Route.Processing>()
                    val imageUri = route.imageUri
                    ProcessingFlowScreen(
                        imagePath = imageUri,
                        onFinish = { signalId ->
                            navController.navigate(Route.Analysis(signalId.toString())) {
                                popUpTo(Route.Capture) { inclusive = false }
                            }
                        },
                        onCancel = {
                            navController.popBackStack(Route.Capture, inclusive = false)
                        },
                    )
                }

                // --- Analysis ---
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

                // --- More ---
                composable<Route.Language> {
                    LanguageScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<Route.About> {
                    AboutScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
                composable<Route.ModelManager> {
                    ModelManagerScreen(
                        downloadedModelIds = modelState.downloadedModelIds,
                        activeModelId = modelState.activeModelId,
                        downloadingModelId = modelState.downloadingModelId,
                        downloadProgress = modelState.downloadProgress,
                        downloadSpeedMbps = modelState.downloadSpeedMbps,
                        activatingModelId = modelState.activatingModelId,
                        activationError = modelState.activationError,
                        deviceRamMb = modelState.deviceRamMb,
                        availableStorageGb = modelState.availableStorageGb,
                        totalModelDiskUsageGb = modelState.totalModelDiskUsageGb,
                        customModels = modelState.customModels,
                        isImporting = modelState.isImporting,
                        huggingFaceSearch = modelState.huggingFaceSearch,
                        onDownload = { modelActions.download(it) },
                        onDelete = { modelActions.delete(it) },
                        onActivate = { modelActions.activate(it) },
                        onDeactivate = { modelActions.deactivate() },
                        onCancelDownload = { modelActions.cancelDownload() },
                        onExport = { modelActions.onExport(it) },
                        onImport = { modelActions.onImport() },
                        onHuggingFaceQueryChange = { modelActions.onHuggingFaceQueryChange(it) },
                        onHuggingFaceSortChange = { modelActions.onHuggingFaceSortChange(it) },
                        onHuggingFaceSearch = { modelActions.onHuggingFaceSearch() },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}

private fun ModelManagerState.toChatModelOptions(): List<ChatModelOption> {
    val builtinOptions = ModelRegistry.builtinModels
        .filter { it.id in downloadedModelIds }
        .map { model ->
            ChatModelOption(
                id = model.id,
                name = model.displayName,
                summary = "${model.runtime.shortLabel()} · ${formatModelBytes(model.totalSizeBytes)}",
                isActive = model.id == activeModelId,
                isActivating = model.id == activatingModelId,
            )
        }

    val customOptions = customModels
        .filterNot { custom -> builtinOptions.any { it.id == custom.id } }
        .map { custom ->
            ChatModelOption(
                id = custom.id,
                name = custom.displayName,
                summary = "Imported · ${formatModelBytes(custom.sizeBytes)}",
                isActive = custom.id == activeModelId,
                isActivating = custom.id == activatingModelId,
            )
        }

    return (builtinOptions + customOptions).sortedWith(
        compareByDescending<ChatModelOption> { it.isActive }
            .thenBy { it.name.lowercase() }
    )
}

private fun ModelRuntime.shortLabel(): String = when (this) {
    ModelRuntime.LITERT_LM -> "LiteRT"
    ModelRuntime.LLAMA_CPP -> "GGUF"
}

private fun formatModelBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000f)
    bytes >= 1_000_000 -> "%.0f MB".format(bytes / 1_000_000f)
    else -> "%.0f KB".format(bytes / 1_000f)
}

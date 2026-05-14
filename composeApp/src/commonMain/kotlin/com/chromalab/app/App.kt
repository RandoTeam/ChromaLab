package com.chromalab.app

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.chromalab.core.ui.theme.rememberAppThemePreference
import com.chromalab.feature.capture.CameraScreen
import com.chromalab.feature.capture.CaptureHubScreen
import com.chromalab.feature.capture.FileImportScreenWithPicker
import com.chromalab.feature.chat.ChatModelCapabilities
import com.chromalab.feature.chat.ChatModelCompatibility
import com.chromalab.feature.chat.ChatModelOption
import com.chromalab.feature.chat.ChatRuntimeAccelerator
import com.chromalab.feature.chat.ChatRuntimeBackend
import com.chromalab.feature.chat.ChatRuntimeUiState
import com.chromalab.feature.chat.ChatThinkingUiState
import com.chromalab.feature.chat.ChatScreen
import com.chromalab.feature.chat.rememberChatState
import com.chromalab.feature.processing.inference.ModelRuntime
import com.chromalab.feature.processing.model.ModelInfo
import com.chromalab.feature.processing.model.ModelRegistry

import com.chromalab.feature.settings.LanguageScreen
import com.chromalab.feature.settings.AboutScreen
import com.chromalab.feature.settings.MoreScreen
import com.chromalab.feature.settings.ModelManagerScreen
import com.chromalab.feature.settings.ModelManagerState
import com.chromalab.feature.settings.rememberModelManagerState

@Composable
fun App() {
    val themePreference = rememberAppThemePreference()

    ChromaLabTheme(themeMode = themePreference.mode) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()

        // Model Manager — platform-specific state + actions
        val (modelState, modelActions) = rememberModelManagerState()
        val activeChatModelId = modelState.activeChatModelId()
        val activeChatModelName = modelState.activeChatModelName()
        val (chatState, chatActions) = rememberChatState(
            activeModelId = activeChatModelId,
            activeModelName = activeChatModelName,
        )
        val destination = backStackEntry?.destination
        val isChatRoute = destination?.hasRoute(Route.Chats::class) == true
        val isChromatogramWorkflowRoute = destination?.let {
            it.hasRoute(Route.Capture::class) ||
                it.hasRoute(Route.Camera::class) ||
                it.hasRoute(Route.FileImport::class) ||
                it.hasRoute(Route.Processing::class) ||
                it.hasRoute(Route.Analysis::class)
        } == true
        LaunchedEffect(isChatRoute, isChromatogramWorkflowRoute) {
            if (isChatRoute) {
                modelActions.cancelAutoUnloadTimer()
            } else if (isChromatogramWorkflowRoute) {
                modelActions.prepareForChromatogramWorkflow()
            } else {
                modelActions.scheduleAutoUnload()
            }
        }

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
                        onOpenModelManager = { navController.navigate(Route.ModelManager) },
                    )
                }
                composable<Route.More> {
                    MoreScreen(
                        activeModelName = modelState.activeModelName,
                        activeModelSummary = modelState.activeModelSummary,
                        threadCount = modelState.threadCount,
                        downloadParallelism = modelState.downloadParallelism,
                        downloadSpeedLimitMbps = modelState.downloadSpeedLimitMbps,
                        autoUnloadMinutes = modelState.autoUnloadMinutes,
                        themeMode = themePreference.mode,
                        onOpenModelManager = { navController.navigate(Route.ModelManager) },
                        onOpenLanguage = { navController.navigate(Route.Language) },
                        onOpenAbout = { navController.navigate(Route.About) },
                        onThreadCountChange = { modelActions.setThreadCount(it) },
                        onDownloadParallelismChange = { modelActions.setDownloadParallelism(it) },
                        onDownloadSpeedLimitChange = { modelActions.setDownloadSpeedLimit(it) },
                        onAutoUnloadChange = { modelActions.setAutoUnloadMinutes(it) },
                        onThemeModeChange = themePreference.setMode,
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
                            modelActions.unloadChromatogramModelAfterAnalysis()
                            navController.navigate(Route.Analysis(signalId.toString())) {
                                popUpTo(Route.Capture) { inclusive = false }
                            }
                        },
                        onCancel = {
                            modelActions.unloadChromatogramModelAfterAnalysis()
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
                            modelActions.unloadChromatogramModelAfterAnalysis()
                            navController.popBackStack(Route.Calculations, inclusive = false)
                        },
                        onCancel = {
                            modelActions.unloadChromatogramModelAfterAnalysis()
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
                        chromatogramModelId = modelState.chromatogramModelId,
                        downloadJobs = modelState.downloadJobs,
                        deviceRamMb = modelState.deviceRamMb,
                        availableStorageGb = modelState.availableStorageGb,
                        totalModelDiskUsageGb = modelState.totalModelDiskUsageGb,
                        customModels = modelState.customModels,
                        isImporting = modelState.isImporting,
                        huggingFaceSearch = modelState.huggingFaceSearch,
                        onDownload = { modelActions.download(it) },
                        onDelete = { modelActions.delete(it) },
                        onUseForChromatograms = { modelActions.setChromatogramModel(it) },
                        onCancelDownload = { modelActions.cancelDownload(it) },
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
                summary = formatChatModelSummary(
                    runtime = model.runtime.shortLabel(),
                    sizeBytes = model.totalSizeBytes,
                ),
                runtime = model.toChatRuntimeUiState(),
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
                summary = formatChatModelSummary(runtime = "Imported", sizeBytes = custom.sizeBytes),
                runtime = ChatRuntimeUiState(
                    backend = ChatRuntimeBackend.IMPORTED,
                    backendLabel = "Imported",
                    supportedAccelerators = listOf(ChatRuntimeAccelerator.CPU),
                    selectedAccelerator = ChatRuntimeAccelerator.CPU,
                    capabilities = ChatModelCapabilities(
                        supportsTextChat = custom.supportsTextChat,
                        supportsVisionInput = custom.supportsVision,
                    ),
                    thinking = ChatThinkingUiState(
                        modelSupportsThinking = false,
                        runtimeCanExposeThinking = false,
                        unavailableReason = "Thinking support is unknown for imported models.",
                    ),
                    compatibility = ChatModelCompatibility(
                        isSelectableForChat = custom.supportsTextChat,
                        reason = if (custom.supportsTextChat) null else "Imported model is not marked as chat-capable.",
                    ),
                ),
                isActive = custom.id == activeModelId,
                isActivating = custom.id == activatingModelId,
            )
        }

    return (builtinOptions + customOptions).sortedWith(
        compareByDescending<ChatModelOption> { it.isActive }
            .thenByDescending { it.runtime.compatibility.isSelectableForChat }
            .thenBy { it.name.lowercase() }
    )
}

private fun ModelManagerState.activeChatModelId(): String? {
    val id = activeModelId ?: return null
    val builtin = ModelRegistry.findById(id)
    if (builtin != null) {
        return id.takeIf { ModelRegistry.isChatModel(builtin) }
    }
    return id.takeIf { customModels.any { custom -> custom.id == id && custom.supportsTextChat } }
}

private fun ModelManagerState.activeChatModelName(): String? =
    activeModelName.takeIf { activeChatModelId() != null }

private fun ModelRuntime.shortLabel(): String = when (this) {
    ModelRuntime.LITERT_LM -> "LiteRT"
    ModelRuntime.LLAMA_CPP -> "GGUF"
}

private fun ModelInfo.toChatRuntimeUiState(): ChatRuntimeUiState {
    val supportsChromatograms = ModelRegistry.isChromatogramVisionModel(this)
    val supportsTextChat = ModelRegistry.isChatModel(this)
    val acceleratorOptions = runtime.chatAccelerators()
    val modelSupportsThinking = supportsChatThinking()
    return ChatRuntimeUiState(
        backend = runtime.toChatRuntimeBackend(),
        backendLabel = runtime.chatBackendLabel(),
        supportedAccelerators = acceleratorOptions,
        selectedAccelerator = runtime.defaultChatAccelerator(),
        capabilities = ChatModelCapabilities(
            supportsTextChat = supportsTextChat,
            supportsVisionInput = supportsVision,
            supportsChromatogramAnalysis = supportsChromatograms,
            supportsRuntimeSelection = acceleratorOptions.size > 1,
            supportsThinking = modelSupportsThinking,
            supportsNativeStreaming = runtime == ModelRuntime.LITERT_LM,
        ),
        thinking = ChatThinkingUiState(
            modelSupportsThinking = modelSupportsThinking,
            runtimeCanExposeThinking = false,
            unavailableReason = if (modelSupportsThinking) {
                "Thinking output is not separated by the chat runtime yet."
            } else {
                "Thinking is not validated for this model."
            },
        ),
        compatibility = ChatModelCompatibility(
            isSelectableForChat = supportsTextChat,
            reason = if (supportsTextChat) null else "Model is not chat-capable.",
        ),
    )
}

private fun ModelRuntime.toChatRuntimeBackend(): ChatRuntimeBackend = when (this) {
    ModelRuntime.LITERT_LM -> ChatRuntimeBackend.LITERT_LM
    ModelRuntime.LLAMA_CPP -> ChatRuntimeBackend.LLAMA_CPP
}

private fun ModelRuntime.chatBackendLabel(): String = when (this) {
    ModelRuntime.LITERT_LM -> "LiteRT-LM"
    ModelRuntime.LLAMA_CPP -> "GGUF / llama.cpp"
}

private fun ModelRuntime.chatAccelerators(): List<ChatRuntimeAccelerator> = when (this) {
    ModelRuntime.LITERT_LM -> listOf(
        ChatRuntimeAccelerator.AUTO,
        ChatRuntimeAccelerator.CPU,
    )
    ModelRuntime.LLAMA_CPP -> listOf(
        ChatRuntimeAccelerator.CPU,
        ChatRuntimeAccelerator.VULKAN,
    )
}

private fun ModelRuntime.defaultChatAccelerator(): ChatRuntimeAccelerator = when (this) {
    ModelRuntime.LITERT_LM -> ChatRuntimeAccelerator.AUTO
    ModelRuntime.LLAMA_CPP -> ChatRuntimeAccelerator.CPU
}

private fun ModelInfo.supportsChatThinking(): Boolean {
    val normalized = "$family $id ${displayName}".lowercase()
    return runtime == ModelRuntime.LLAMA_CPP && "qwen3" in normalized
}

private fun formatChatModelSummary(runtime: String, sizeBytes: Long): String =
    "$runtime · ${formatModelBytes(sizeBytes)}"

private fun formatModelBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.1f GB".format(bytes / 1_000_000_000f)
    bytes >= 1_000_000 -> "%.0f MB".format(bytes / 1_000_000f)
    else -> "%.0f KB".format(bytes / 1_000f)
}

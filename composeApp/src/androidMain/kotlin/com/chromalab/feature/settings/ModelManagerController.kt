package com.chromalab.feature.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import com.chromalab.feature.processing.inference.InferenceConfig
import com.chromalab.feature.processing.inference.InferenceEngine
import com.chromalab.feature.processing.inference.LlamaEngine
import com.chromalab.feature.processing.inference.LiteRTEngine
import com.chromalab.feature.processing.inference.ModelRuntime
import com.chromalab.feature.processing.inference.ActiveInferenceModel
import com.chromalab.feature.processing.inference.VlmEngineHolder
import com.chromalab.feature.processing.model.ModelInfo
import com.chromalab.feature.processing.model.ModelManager
import com.chromalab.feature.processing.model.ModelRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ChromaLabModels"

private fun logModel(message: String) {
    Log.i(TAG, message)
}

private fun logModelError(message: String, throwable: Throwable? = null) {
    Log.e(TAG, message, throwable)
}

/**
 * Controller bridging ModelManager (Android) → ModelManagerState (common UI).
 *
 * Owns the coroutine scope for downloads and engine loading.
 * Produces a StateFlow<ModelManagerState> consumed by composables.
 */
class ModelManagerController(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val manager = ModelManager(context)
    private val hfSearchClient = HuggingFaceSearchClient()

    private val _state = MutableStateFlow(ModelManagerState())
    val state: StateFlow<ModelManagerState> = _state.asStateFlow()

    private var hfSearchJob: Job? = null
    private var unloadTimerJob: Job? = null

    init {
        refresh()
        observeForegroundDownloads()
        ModelDownloadForegroundService.resumePendingDownloads(context)
    }

    /** Refresh state from disk / prefs. */
    fun refresh() {
        val downloaded = manager.getDownloadedModels()
        val loadedModelId = if (VlmEngineHolder.activeEngine?.isLoaded() == true) {
            VlmEngineHolder.executedModel?.modelId ?: VlmEngineHolder.selectedModel?.modelId
        } else {
            null
        }
        val active = loadedModelId
            ?.let { id -> downloaded.find { it.info.id == id } }
        val chromatogramModelId = manager.getChromatogramModelId()
        val chromatogramModel = chromatogramModelId
            ?.let { id -> downloaded.find { it.info.id == id } }
            ?.info
        val builtinIds = ModelRegistry.builtinModels.map { it.id }.toSet()

        // Build list of custom (non-builtin) models for UI
        val customs = downloaded
            .filter { it.info.id !in builtinIds }
            .map { m ->
                CustomModelEntry(
                    id = m.info.id,
                    displayName = m.info.displayName,
                    sizeBytes = m.info.totalSizeBytes,
                    description = m.info.description,
                    supportsTextChat = ModelRegistry.isChatModel(m.info),
                    supportsVision = m.info.supportsVision,
                )
            }

        _state.update {
            it.copy(
                downloadedModelIds = downloaded.map { m -> m.info.id }.toSet(),
                activeModelId = active?.info?.id,
                activeModelName = active?.info?.displayName,
                activeModelSummary = active?.let { m ->
                    val runtime = when (m.info.runtime) {
                        ModelRuntime.LITERT_LM -> "LiteRT"
                        ModelRuntime.LLAMA_CPP -> "llama.cpp"
                    }
                    val sizeGb = m.info.totalSizeBytes / 1_000_000_000f
                    "$runtime · %.1f GB".format(sizeGb)
                },
                chromatogramModelId = chromatogramModel?.id,
                chromatogramModelName = chromatogramModel?.displayName,
                deviceRamMb = manager.getDeviceRamMb(),
                availableStorageGb = manager.getAvailableStorageBytes() / (1024f * 1024 * 1024),
                totalModelDiskUsageGb = manager.getTotalModelDiskUsage() / (1024f * 1024 * 1024),
                threadCount = manager.threadCount,
                downloadParallelism = manager.downloadParallelism,
                downloadSpeedLimitMbps = manager.downloadSpeedLimitMbps,
                autoUnloadMinutes = manager.autoUnloadMinutes,
                customModels = customs,
            )
        }
    }

    private fun observeForegroundDownloads() {
        scope.launch {
            ModelDownloadForegroundState.downloadJobs.collect { jobs ->
                val nextJob = jobs.values.firstOrNull { it.isRunningDownload() }
                    ?: jobs.values.firstOrNull()
                _state.update {
                    it.copy(
                        downloadJobs = jobs,
                        downloadingModelId = nextJob?.modelId,
                        downloadProgress = nextJob?.progress ?: 0f,
                        downloadSpeedMbps = nextJob?.speedMbps ?: 0f,
                        downloadFileName = nextJob?.fileName.orEmpty(),
                        downloadError = jobs.values.firstOrNull { job -> job.error != null }?.error,
                    )
                }

                if (jobs.isEmpty() || jobs.values.any { it.phase == ModelDownloadUiPhase.COMPLETE }) {
                    refresh()
                }
            }
        }
    }

    /** Start downloading a model. */
    fun download(model: ModelInfo) {
        if (ModelDownloadForegroundState.snapshot()[model.id]?.isRunningDownload() == true) return
        if (manager.isDownloaded(model.id)) return

        _state.update {
            it.copy(
                downloadJobs = it.downloadJobs + (
                    model.id to ModelDownloadUiState(
                        modelId = model.id,
                        phase = ModelDownloadUiPhase.QUEUED,
                    )
                ),
                downloadingModelId = it.downloadingModelId ?: model.id,
                downloadError = null,
            )
        }

        ModelDownloadForegroundService.startDownload(
            context = context,
            model = model,
            parallelism = manager.downloadParallelism,
        )
    }

    /** Cancel a specific ongoing download. */
    fun cancelDownload(modelId: String) {
        ModelDownloadForegroundService.cancelDownload(context, modelId)
        refresh()
    }

    fun updateHuggingFaceQuery(query: String) {
        _state.update {
            it.copy(
                huggingFaceSearch = it.huggingFaceSearch.copy(
                    query = query,
                    error = null,
                ),
            )
        }
    }

    fun setHuggingFaceSort(sort: HuggingFaceSortOption) {
        _state.update {
            it.copy(
                huggingFaceSearch = it.huggingFaceSearch.copy(sort = sort),
            )
        }
    }

    fun searchHuggingFace() {
        val snapshot = _state.value.huggingFaceSearch
        hfSearchJob?.cancel()
        _state.update {
            it.copy(
                huggingFaceSearch = snapshot.copy(
                    isSearching = true,
                    error = null,
                ),
            )
        }

        hfSearchJob = scope.launch {
            try {
                val results = hfSearchClient.search(
                    query = snapshot.query,
                    sort = snapshot.sort,
                    deviceRamMb = manager.getDeviceRamMb(),
                    availableStorageBytes = manager.getAvailableStorageBytes(),
                )
                _state.update {
                    it.copy(
                        huggingFaceSearch = it.huggingFaceSearch.copy(
                            isSearching = false,
                            results = results,
                            error = null,
                        ),
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        huggingFaceSearch = it.huggingFaceSearch.copy(
                            isSearching = false,
                            error = e.message ?: "Hugging Face search failed",
                        ),
                    )
                }
            }
        }
    }

    /** Activate a downloaded chat model — creates inference engine + sets VlmEngineHolder. */
    fun activate(modelId: String) {
        scope.launch {
            activateForChat(modelId)
        }
    }

    suspend fun activateForChat(modelId: String): Boolean {
        val loadedId = VlmEngineHolder.executedModel?.modelId ?: VlmEngineHolder.selectedModel?.modelId
        if (loadedId == modelId && VlmEngineHolder.activeEngine?.isLoaded() == true) {
            cancelAutoUnloadTimer()
            refresh()
            logModel("Chat model already loaded: ${VlmEngineHolder.activeModelDiagnostics()}")
            return true
        }

        if (_state.value.activatingModelId != null) return false
        _state.update {
            it.copy(activatingModelId = modelId, activationError = null)
        }

        return try {
            val model = manager.getDownloadedModels().find { it.info.id == modelId }
                ?: run {
                    _state.update {
                        it.copy(
                            activatingModelId = null,
                            activationError = "Модель не найдена",
                        )
                    }
                    return false
                }

            if (!manager.canLoadForText(model.info)) {
                _state.update {
                    it.copy(
                        activatingModelId = null,
                        activationError = manager.compatibilityMessage(model.info, forVision = false),
                    )
                }
                return false
            }

            if (VlmEngineHolder.activeEngine?.isLoaded() == true && loadedId != modelId) {
                logModel("Unloading previous model before chat load: ${VlmEngineHolder.activeModelDiagnostics()}")
                VlmEngineHolder.activeEngine = null
                VlmEngineHolder.activeConfig = null
                VlmEngineHolder.selectedModel = null
                VlmEngineHolder.executedModel = null
            }

            cancelAutoUnloadTimer()
            manager.setActiveModel(modelId)

            // Create engine based on runtime. Chat loads text-only to keep memory lower than
            // chromatogram vision analysis, which loads its own model/mmproj later.
            val engine: InferenceEngine? = when (model.info.runtime) {
                ModelRuntime.LLAMA_CPP -> {
                    val llama = LlamaEngine()
                    withContext(Dispatchers.IO) {
                        llama.loadModel(
                            basePath = model.primaryPath,
                            mmprojPath = "",
                            threads = manager.threadCount,
                            modelFamily = model.info.family,
                            contextSize = manager.llamaContextSize(model.info, forVision = false),
                            batchSize = manager.llamaBatchSize(model.info, forVision = false),
                        )
                    }
                    if (model.mmprojPath != null) {
                        println("MODEL[CTRL] GGUF loaded text-only for chat: ${model.info.displayName}; chromatogram analysis loads mmproj separately")
                    }
                    llama
                }
                ModelRuntime.LITERT_LM -> {
                    val liteRT = LiteRTEngine()
                    withContext(Dispatchers.IO) {
                        liteRT.loadModel(
                            modelPath = model.primaryPath,
                            preferGpu = manager.liteRtPreferAccelerator(model.info),
                            enableVision = false,
                            maxNumTokens = manager.liteRtMaxTokens(model.info),
                        )
                    }
                    liteRT
                }
            }

            if (engine != null) {
                VlmEngineHolder.activeEngine = engine
                VlmEngineHolder.activeConfig = InferenceConfig.forModelFamily(model.info.family)
                VlmEngineHolder.selectedModel = model.info.toActiveInferenceModel()
                VlmEngineHolder.executedModel = model.info.toActiveInferenceModel(engine.getBackendName())
                logModel("Chat engine loaded: ${model.info.displayName} (family=${model.info.family}, backend=${engine.getBackendName()})")
            }

            _state.update { it.copy(activatingModelId = null, activationError = null) }
            refresh()
            true
        } catch (e: Throwable) {
            println("MODEL[CTRL] Chat load failed: ${e.message}")
            manager.clearActiveModel()
            VlmEngineHolder.activeEngine = null
            VlmEngineHolder.activeConfig = null
            VlmEngineHolder.selectedModel = null
            VlmEngineHolder.executedModel = null
            _state.update {
                it.copy(
                    activatingModelId = null,
                    activationError = e.message ?: "Ошибка загрузки модели чата",
                    )
            }
            refresh()
            false
        }
    }

    /** Deactivate (unload) the current model without deleting files. */
    fun deactivate() {
        VlmEngineHolder.activeEngine = null
        VlmEngineHolder.activeConfig = null
        VlmEngineHolder.selectedModel = null
        VlmEngineHolder.executedModel = null
        manager.clearActiveModel()
        refresh()
        println("MODEL[CTRL] Model deactivated")
    }

    /** Delete a downloaded model. */
    fun delete(modelId: String) {
        cancelDownload(modelId)
        // Unload engine if this is the active model
        if (manager.getActiveModelId() == modelId || manager.getChromatogramModelId() == modelId) {
            VlmEngineHolder.activeEngine = null
            VlmEngineHolder.activeConfig = null
            VlmEngineHolder.selectedModel = null
            VlmEngineHolder.executedModel = null
        }
        manager.delete(modelId)
        refresh()
    }

    /** Update thread count. */
    fun setThreadCount(count: Int) {
        manager.threadCount = count
        _state.update { it.copy(threadCount = count) }
    }

    /** Update HTTP range chunk count for future model downloads. */
    fun setDownloadParallelism(parallelism: Int) {
        manager.downloadParallelism = parallelism
        _state.update { it.copy(downloadParallelism = manager.downloadParallelism) }
    }

    /** Update total download speed limit in MiB/s. 0 = unlimited. */
    fun setDownloadSpeedLimit(mbps: Int) {
        manager.downloadSpeedLimitMbps = mbps
        _state.update { it.copy(downloadSpeedLimitMbps = manager.downloadSpeedLimitMbps) }
    }

    /** Update auto-unload timeout (minutes). 0 = disabled. */
    fun setAutoUnloadMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(0, 30)
        manager.autoUnloadMinutes = clamped
        _state.update { it.copy(autoUnloadMinutes = clamped) }
        // Reschedule timer if model is loaded
        if (VlmEngineHolder.activeEngine?.isLoaded() == true) {
            scheduleAutoUnload()
        }
        println("MODEL[TIMER] Auto-unload set to $clamped min")
    }

    /** Select the downloaded model used by chromatogram photo analysis. */
    fun setChromatogramModel(modelId: String) {
        val model = manager.getDownloadedModels().find { it.info.id == modelId } ?: return
        if (!manager.canLoadForChromatogramVision(model.info)) {
            logModel("Rejected chromatogram model selection: ${model.info.displayName}")
            return
        }
        manager.setChromatogramModel(modelId)
        _state.update {
            it.copy(
                chromatogramModelId = model.info.id,
                chromatogramModelName = model.info.displayName,
            )
        }
    }

    /**
     * Schedule auto-unload of VLM model after configured timeout.
     * Cancels any existing timer, starts a new one.
     * Called after pipeline completes or when setting changes.
     */
    fun scheduleAutoUnload() {
        unloadTimerJob?.cancel()
        val minutes = manager.autoUnloadMinutes
        if (minutes <= 0) {
            println("MODEL[TIMER] Auto-unload disabled")
            return
        }
        if (VlmEngineHolder.activeEngine?.isLoaded() != true) {
            return  // No model loaded — nothing to schedule
        }
        println("MODEL[TIMER] Scheduling auto-unload in $minutes min")
        unloadTimerJob = scope.launch {
            kotlinx.coroutines.delay(minutes * 60_000L)
            val engine = VlmEngineHolder.activeEngine
            if (engine != null && engine.isLoaded()) {
                if (VlmEngineHolder.isInferring) {
                    // Inference running — defer unload, retry in 1 min
                    println("MODEL[TIMER] Inference active, deferring auto-unload by 1 min")
                    kotlinx.coroutines.delay(60_000L)
                    if (VlmEngineHolder.isInferring) {
                        println("MODEL[TIMER] Still inferring, rescheduling")
                        scheduleAutoUnload()
                        return@launch
                    }
                }
                println("MODEL[TIMER] Auto-unloading model after $minutes min of inactivity")
                VlmEngineHolder.activeEngine = null
                VlmEngineHolder.activeConfig = null
                VlmEngineHolder.selectedModel = null
                VlmEngineHolder.executedModel = null
                manager.clearActiveModel()
                refresh()
            }
        }
    }

    /** Cancel the auto-unload timer (called when pipeline starts). */
    fun cancelAutoUnloadTimer() {
        unloadTimerJob?.cancel()
        unloadTimerJob = null
    }

    /**
     * Free chat/text runtime memory before camera capture or chromatogram analysis.
     * This does not load a chromatogram model; the neural pipeline still lazy-loads
     * the selected vision model exactly at the VLM stage.
     */
    fun prepareForChromatogramWorkflow() {
        cancelAutoUnloadTimer()

        val engine = VlmEngineHolder.activeEngine
        if (engine?.isLoaded() != true) return

        if (VlmEngineHolder.isInferring) {
            logModel("Keeping active model during chromatogram handoff because inference is running")
            return
        }

        val loadedModelId = VlmEngineHolder.executedModel?.modelId ?: VlmEngineHolder.selectedModel?.modelId
        val requestedChromatogramId = manager.getChromatogramModelId()
        val reusableChromatogramVision =
            engine.supportsImageInput() &&
                VlmEngineHolder.activeExecutedModelIsChromatogramVision() &&
                (requestedChromatogramId == null || loadedModelId == requestedChromatogramId)

        if (reusableChromatogramVision) {
            logModel("Keeping reusable chromatogram VLM for analysis: ${VlmEngineHolder.activeModelDiagnostics()}")
            refresh()
            return
        }

        logModel("Unloading active model before chromatogram workflow: ${VlmEngineHolder.activeModelDiagnostics()}")
        VlmEngineHolder.activeEngine = null
        VlmEngineHolder.activeConfig = null
        VlmEngineHolder.selectedModel = null
        VlmEngineHolder.executedModel = null
        manager.clearActiveModel()
        refresh()
    }

    /**
     * Release the chromatogram vision runtime after the image/report workflow has produced
     * its saved result. This keeps the app idle without a heavy VLM in memory.
     */
    fun unloadChromatogramModelAfterAnalysis() {
        cancelAutoUnloadTimer()

        val engine = VlmEngineHolder.activeEngine
        if (engine?.isLoaded() != true) return

        if (VlmEngineHolder.isInferring) {
            logModel("Post-analysis unload skipped because inference is still running")
            scheduleAutoUnload()
            return
        }

        if (!engine.supportsImageInput() || !VlmEngineHolder.activeExecutedModelIsChromatogramVision()) {
            return
        }

        logModel("Unloading chromatogram VLM after analysis: ${VlmEngineHolder.activeModelDiagnostics()}")
        VlmEngineHolder.activeEngine = null
        VlmEngineHolder.activeConfig = null
        VlmEngineHolder.selectedModel = null
        VlmEngineHolder.executedModel = null
        manager.clearActiveModel()
        refresh()
    }

    /** Import a model file from a user-selected URI. */
    fun importFile(uri: Uri) {
        scope.launch {
            try {
                _state.update { it.copy(isImporting = true) }
                println("MODEL[CTRL] Importing file: $uri")
                val result = manager.importFile(uri)
                if (result != null) {
                    println("MODEL[CTRL] Import success: ${result.displayName}")
                } else {
                    println("MODEL[CTRL] Import failed — unknown format or invalid file")
                }
                refresh()
            } catch (e: Exception) {
                println("MODEL[CTRL] Import error: ${e.message}")
            } finally {
                _state.update { it.copy(isImporting = false) }
            }
        }
    }

    /**
     * Activate the best available model for pipeline use (lazy loading).
     * Called automatically at pipeline start if no model is loaded.
     *
     * Strategy:
     * 1. Check if a model is already loaded → skip
     * 2. Check previously active model (saved in prefs) → load it
     * 3. Find any downloaded model → load it
     * 4. No models available → return false (pipeline continues without VLM)
     *
     * @param onProgress optional callback for progress reporting
     * @return true if a model is loaded and ready
     */
    suspend fun activateForPipeline(
        onProgress: ((String) -> Unit)? = null,
    ): Boolean {
        val requestedChromatogramId = manager.getChromatogramModelId()

        // Already loaded?
        if (VlmEngineHolder.activeEngine?.isLoaded() == true &&
            VlmEngineHolder.activeEngine?.supportsImageInput() == true
        ) {
            if (VlmEngineHolder.activeExecutedModelIsChromatogramVision()) {
                val loadedModelId = VlmEngineHolder.executedModel?.modelId ?: VlmEngineHolder.selectedModel?.modelId
                if (requestedChromatogramId == null || loadedModelId == requestedChromatogramId) {
                    logModel("Chromatogram VLM already loaded: ${VlmEngineHolder.activeModelDiagnostics()}")
                    // Reset auto-unload timer (model is being used)
                    cancelAutoUnloadTimer()
                    return true
                }

                logModel("Unloading previous chromatogram VLM before selected model load: ${VlmEngineHolder.activeModelDiagnostics()}")
                VlmEngineHolder.activeEngine = null
                VlmEngineHolder.activeConfig = null
                VlmEngineHolder.executedModel = null
                VlmEngineHolder.selectedModel = null
            } else {
                logModel("Unloading active non-chromatogram vision model before pipeline: ${VlmEngineHolder.activeModelDiagnostics()}")
                VlmEngineHolder.activeEngine = null
                VlmEngineHolder.activeConfig = null
                VlmEngineHolder.executedModel = null
            }
        }

        if (VlmEngineHolder.activeEngine?.isLoaded() == true &&
            VlmEngineHolder.activeEngine?.supportsImageInput() != true
        ) {
            VlmEngineHolder.activeEngine = null
            VlmEngineHolder.activeConfig = null
            VlmEngineHolder.executedModel = null
        }

        // Find a model to load for chromatogram vision. This path must never
        // load a GGUF model text-only: photo analysis requires image input.
        val downloadedModels = manager.getDownloadedModels()
        val selectedDownloadedModel = requestedChromatogramId
            ?.let { id -> downloadedModels.find { it.info.id == id } }

        if (requestedChromatogramId != null && selectedDownloadedModel == null) {
            logModel("Selected chromatogram model is not downloaded: $requestedChromatogramId")
            onProgress?.invoke("Selected chromatogram VLM is not downloaded")
            return false
        }

        if (selectedDownloadedModel != null && !manager.canLoadForChromatogramVision(selectedDownloadedModel.info)) {
            logModel("Selected chromatogram model cannot load on this device: ${selectedDownloadedModel.info.displayName}")
            onProgress?.invoke(manager.compatibilityMessage(selectedDownloadedModel.info, forVision = true))
            return false
        }

        val models = if (selectedDownloadedModel != null) {
            listOf(selectedDownloadedModel)
        } else {
            downloadedModels
                .filter { manager.canLoadForChromatogramVision(it.info) }
                .sortedWith(
                    compareBy<com.chromalab.feature.processing.model.DownloadedModel> {
                        ModelRegistry.chromatogramVisionPriority(it.info)
                    }.thenBy { it.info.totalSizeBytes }
                )
        }
        if (models.isEmpty()) {
            logModel("No chromatogram vision model can be loaded on this device")
            onProgress?.invoke("No loaded/downloaded chromatogram VLM fits this device")
            return false
        }

        // Priority: selected chromatogram model > chromatography ranking > package size.
        val model = models.first()

        logModel("Auto-loading chromatogram VLM: ${model.info.displayName} (${model.info.family}, ${model.info.runtime})")
        onProgress?.invoke("Загрузка AI модели: ${model.info.displayName}")

        return try {
            val engine: InferenceEngine? = when (model.info.runtime) {
                ModelRuntime.LLAMA_CPP -> {
                    onProgress?.invoke("Загрузка GGUF модели...")
                    val llama = LlamaEngine()
                    if (!manager.llamaShouldLoadVisionProjector(model.info)) {
                        throw IllegalStateException(manager.compatibilityMessage(model.info, forVision = true))
                    }
                    val visionPackage = manager.requireGgufVisionPackage(model)
                    withContext(Dispatchers.IO) {
                        llama.loadModel(
                            basePath = visionPackage.basePath,
                            mmprojPath = visionPackage.mmprojPath,
                            threads = manager.threadCount,
                            modelFamily = model.info.family,
                            contextSize = manager.llamaContextSize(model.info, forVision = true),
                            batchSize = manager.llamaBatchSize(model.info, forVision = true),
                        )
                    }
                    if (!llama.supportsImageInput()) {
                        throw IllegalStateException("GGUF model loaded without vision support: ${model.info.displayName}")
                    }
                    llama
                }
                ModelRuntime.LITERT_LM -> {
                    onProgress?.invoke("Загрузка LiteRT модели...")
                    val liteRT = LiteRTEngine()
                    withContext(Dispatchers.IO) {
                        liteRT.loadModel(
                            modelPath = model.primaryPath,
                            preferGpu = manager.liteRtPreferAccelerator(model.info),
                            enableVision = true,
                            maxNumTokens = manager.liteRtMaxTokens(model.info),
                        )
                    }
                    liteRT
                }
            }

            if (engine != null) {
                VlmEngineHolder.activeEngine = engine
                VlmEngineHolder.activeConfig = InferenceConfig.forModelFamily(model.info.family)
                VlmEngineHolder.selectedModel = model.info.toActiveInferenceModel()
                VlmEngineHolder.executedModel = model.info.toActiveInferenceModel(engine.getBackendName())
                onProgress?.invoke("AI модель готова")
                logModel("Loaded chromatogram VLM: ${model.info.displayName} backend=${engine.getBackendName()} promptStyle=${VlmEngineHolder.activeConfig?.promptStyle}")
                // Schedule auto-unload timer
                scheduleAutoUnload()
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            logModelError("Auto-load failed: ${e.message}", e)
            VlmEngineHolder.selectedModel = model.info.toActiveInferenceModel()
            VlmEngineHolder.executedModel = null
            false
        }
    }
}

private fun ModelInfo.toActiveInferenceModel(backendLabel: String? = null): ActiveInferenceModel =
    ActiveInferenceModel(
        modelId = id,
        modelName = displayName,
        runtime = runtime,
        backendLabel = backendLabel?.takeIf { it.isNotBlank() },
    )

private fun ModelDownloadUiState.isRunningDownload(): Boolean =
    phase == ModelDownloadUiPhase.QUEUED ||
        phase == ModelDownloadUiPhase.CONNECTING ||
        phase == ModelDownloadUiPhase.DOWNLOADING ||
        phase == ModelDownloadUiPhase.VALIDATING

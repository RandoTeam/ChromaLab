package com.chromalab.feature.settings

import android.content.Context
import android.net.Uri
import com.chromalab.feature.processing.inference.InferenceConfig
import com.chromalab.feature.processing.inference.InferenceEngine
import com.chromalab.feature.processing.inference.LlamaEngine
import com.chromalab.feature.processing.inference.LiteRTEngine
import com.chromalab.feature.processing.inference.ModelRuntime
import com.chromalab.feature.processing.inference.VlmEngineHolder
import com.chromalab.feature.processing.model.ModelDownloader
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
    private val downloader = ModelDownloader()
    private val hfSearchClient = HuggingFaceSearchClient()

    private val _state = MutableStateFlow(ModelManagerState())
    val state: StateFlow<ModelManagerState> = _state.asStateFlow()

    private var downloadJob: Job? = null
    private var hfSearchJob: Job? = null
    private var unloadTimerJob: Job? = null

    init {
        refresh()
    }

    /** Refresh state from disk / prefs. */
    fun refresh() {
        val downloaded = manager.getDownloadedModels()
        val active = manager.getActiveModel()
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
                deviceRamMb = manager.getDeviceRamMb(),
                availableStorageGb = manager.getAvailableStorageBytes() / (1024f * 1024 * 1024),
                totalModelDiskUsageGb = manager.getTotalModelDiskUsage() / (1024f * 1024 * 1024),
                threadCount = manager.threadCount,
                autoUnloadMinutes = manager.autoUnloadMinutes,
                customModels = customs,
            )
        }
    }

    /** Start downloading a model. */
    fun download(model: ModelInfo) {
        if (downloadJob?.isActive == true) return // one at a time

        _state.update {
            it.copy(
                downloadingModelId = model.id,
                downloadProgress = 0f,
                downloadSpeedMbps = 0f,
                downloadFileName = "",
                downloadError = null,
            )
        }

        downloadJob = scope.launch {
            try {
                val targetDir = manager.getModelDir(model.id)
                downloader.downloadModel(model, targetDir) { progress ->
                    _state.update {
                        it.copy(
                            downloadProgress = progress.fraction,
                            downloadSpeedMbps = progress.speedMbPerSec,
                            downloadFileName = progress.currentFileName,
                            downloadError = progress.error,
                        )
                    }
                }
                println("MODEL[CTRL] Download complete: ${model.id}")
            } catch (e: kotlinx.coroutines.CancellationException) {
                println("MODEL[CTRL] Download cancelled: ${model.id}")
                // Clean up partial download
                manager.delete(model.id)
            } catch (e: Exception) {
                println("MODEL[CTRL] Download failed: ${e.message}")
                _state.update {
                    it.copy(
                        downloadError = e.message ?: "Неизвестная ошибка загрузки",
                    )
                }
                // Clean up corrupt files
                manager.delete(model.id)
            } finally {
                _state.update {
                    it.copy(
                        downloadingModelId = null,
                        downloadProgress = 0f,
                        downloadSpeedMbps = 0f,
                        downloadFileName = "",
                    )
                }
                refresh()
            }
        }
    }

    /** Cancel ongoing download. */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _state.update {
            it.copy(downloadingModelId = null, downloadProgress = 0f, downloadSpeedMbps = 0f)
        }
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

    /** Activate a downloaded model — creates inference engine + sets VlmEngineHolder. */
    fun activate(modelId: String) {
        // Don't activate if already activating
        if (_state.value.activatingModelId != null) return

        _state.update {
            it.copy(activatingModelId = modelId, activationError = null)
        }

        scope.launch {
            try {
                val model = manager.getDownloadedModels().find { it.info.id == modelId }
                    ?: run {
                        _state.update {
                            it.copy(
                                activatingModelId = null,
                                activationError = "Модель не найдена",
                            )
                        }
                        return@launch
                    }

                if (!manager.canLoadForText(model.info)) {
                    _state.update {
                        it.copy(
                            activatingModelId = null,
                            activationError = manager.compatibilityMessage(model.info, forVision = false),
                        )
                    }
                    return@launch
                }

                manager.setActiveModel(modelId)

                // Create engine based on runtime
                val engine: InferenceEngine? = when (model.info.runtime) {
                    ModelRuntime.LLAMA_CPP -> {
                        val llama = LlamaEngine()
                        val enableVision = manager.llamaShouldLoadVisionProjector(model.info)
                        withContext(Dispatchers.IO) {
                            llama.loadModel(
                                basePath = model.primaryPath,
                                mmprojPath = if (enableVision) model.mmprojPath ?: "" else "",
                                threads = manager.threadCount,
                                modelFamily = model.info.family,
                                contextSize = manager.llamaContextSize(model.info, enableVision),
                                batchSize = manager.llamaBatchSize(model.info, enableVision),
                            )
                        }
                        if (!enableVision && model.mmprojPath != null) {
                            println("MODEL[CTRL] GGUF vision disabled for ${model.info.displayName} on this device; loaded text-only")
                        }
                        llama
                    }
                    ModelRuntime.LITERT_LM -> {
                        val liteRT = LiteRTEngine()
                        val enableVision = manager.canLoadForVision(model.info)
                        withContext(Dispatchers.IO) {
                            liteRT.loadModel(
                                modelPath = model.primaryPath,
                                preferGpu = manager.liteRtPreferAccelerator(model.info),
                                enableVision = enableVision,
                                maxNumTokens = manager.liteRtMaxTokens(model.info),
                            )
                        }
                        if (!enableVision) {
                            println("MODEL[CTRL] Vision disabled for ${model.info.displayName} on this device")
                        }
                        liteRT
                    }
                }

                if (engine != null) {
                    VlmEngineHolder.activeEngine = engine
                    VlmEngineHolder.activeConfig = InferenceConfig.forModelFamily(model.info.family)
                    println("MODEL[CTRL] Engine activated: ${model.info.displayName} (family=${model.info.family}, promptStyle=${VlmEngineHolder.activeConfig?.promptStyle})")
                }

                _state.update { it.copy(activatingModelId = null, activationError = null) }
                refresh()
            } catch (e: Throwable) {
                println("MODEL[CTRL] Activate failed: ${e.message}")
                manager.clearActiveModel()
                _state.update {
                    it.copy(
                        activatingModelId = null,
                        activationError = e.message ?: "Ошибка активации",
                    )
                }
                refresh()
            }
        }
    }

    /** Deactivate (unload) the current model without deleting files. */
    fun deactivate() {
        VlmEngineHolder.activeEngine = null
        VlmEngineHolder.activeConfig = null
        manager.clearActiveModel()
        refresh()
        println("MODEL[CTRL] Model deactivated")
    }

    /** Delete a downloaded model. */
    fun delete(modelId: String) {
        // Unload engine if this is the active model
        if (manager.getActiveModelId() == modelId) {
            VlmEngineHolder.activeEngine = null
            VlmEngineHolder.activeConfig = null
        }
        manager.delete(modelId)
        refresh()
    }

    /** Update thread count. */
    fun setThreadCount(count: Int) {
        manager.threadCount = count
        _state.update { it.copy(threadCount = count) }
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
                refresh()
            }
        }
    }

    /** Cancel the auto-unload timer (called when pipeline starts). */
    fun cancelAutoUnloadTimer() {
        unloadTimerJob?.cancel()
        unloadTimerJob = null
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
        // Already loaded?
        if (VlmEngineHolder.activeEngine?.isLoaded() == true &&
            VlmEngineHolder.activeEngine?.supportsImageInput() == true
        ) {
            println("MODEL[LAZY] Already loaded")
            // Reset auto-unload timer (model is being used)
            cancelAutoUnloadTimer()
            return true
        }

        if (VlmEngineHolder.activeEngine?.isLoaded() == true &&
            VlmEngineHolder.activeEngine?.supportsImageInput() != true
        ) {
            VlmEngineHolder.activeEngine = null
            VlmEngineHolder.activeConfig = null
        }

        // Find a model to load for chromatogram vision. This path must never
        // load a GGUF model text-only: photo analysis requires image input.
        val activeId = manager.getActiveModelId()
        val models = manager.getDownloadedModels()
            .filter { manager.canLoadForChromatogramVision(it.info) }
            .sortedWith(
                compareByDescending<com.chromalab.feature.processing.model.DownloadedModel> {
                    it.info.id == activeId
                }
                    .thenBy { ModelRegistry.chromatogramVisionPriority(it.info) }
                    .thenBy { it.info.totalSizeBytes }
            )
        if (models.isEmpty()) {
            println("MODEL[LAZY] No chromatogram vision model can be loaded on this device")
            onProgress?.invoke("No loaded/downloaded chromatogram VLM fits this device")
            return false
        }

        // Priority: active choice > chromatography ranking > package size.
        val model = models.first()

        println("MODEL[LAZY] Auto-loading: ${model.info.displayName} (${model.info.family})")
        onProgress?.invoke("Загрузка AI модели: ${model.info.displayName}")

        return try {
            manager.setActiveModel(model.info.id)

            val engine: InferenceEngine? = when (model.info.runtime) {
                ModelRuntime.LLAMA_CPP -> {
                    onProgress?.invoke("Загрузка GGUF модели...")
                    val llama = LlamaEngine()
                    val mmprojPath = model.mmprojPath
                        ?: throw IllegalStateException("Vision projector is missing for ${model.info.displayName}")
                    if (!manager.llamaShouldLoadVisionProjector(model.info)) {
                        throw IllegalStateException(manager.compatibilityMessage(model.info, forVision = true))
                    }
                    withContext(Dispatchers.IO) {
                        llama.loadModel(
                            basePath = model.primaryPath,
                            mmprojPath = mmprojPath,
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
                onProgress?.invoke("AI модель готова")
                println("MODEL[LAZY] Loaded: ${model.info.displayName} (promptStyle=${VlmEngineHolder.activeConfig?.promptStyle})")
                // Schedule auto-unload timer
                scheduleAutoUnload()
                true
            } else {
                false
            }
        } catch (e: Throwable) {
            println("MODEL[LAZY] Auto-load failed: ${e.message}")
            manager.clearActiveModel()
            false
        }
    }
}

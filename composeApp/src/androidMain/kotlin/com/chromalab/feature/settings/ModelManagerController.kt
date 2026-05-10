package com.chromalab.feature.settings

import android.content.Context
import com.chromalab.feature.processing.inference.InferenceEngine
import com.chromalab.feature.processing.inference.LlamaEngine
import com.chromalab.feature.processing.inference.LiteRTEngine
import com.chromalab.feature.processing.inference.ModelRuntime
import com.chromalab.feature.processing.inference.VlmEngineHolder
import com.chromalab.feature.processing.model.ModelDownloader
import com.chromalab.feature.processing.model.ModelInfo
import com.chromalab.feature.processing.model.ModelManager
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

    private val _state = MutableStateFlow(ModelManagerState())
    val state: StateFlow<ModelManagerState> = _state.asStateFlow()

    private var downloadJob: Job? = null

    init {
        refresh()
    }

    /** Refresh state from disk / prefs. */
    fun refresh() {
        val downloaded = manager.getDownloadedModels()
        val active = manager.getActiveModel()

        _state.update {
            it.copy(
                downloadedModelIds = downloaded.map { m -> m.info.id }.toSet(),
                activeModelId = active?.info?.id,
                activeModelName = active?.info?.displayName,
                deviceRamMb = manager.getDeviceRamMb(),
                availableStorageGb = manager.getAvailableStorageBytes() / (1024f * 1024 * 1024),
                totalModelDiskUsageGb = manager.getTotalModelDiskUsage() / (1024f * 1024 * 1024),
                threadCount = manager.threadCount,
            )
        }
    }

    /** Start downloading a model. */
    fun download(model: ModelInfo) {
        if (downloadJob?.isActive == true) return // one at a time

        _state.update {
            it.copy(downloadingModelId = model.id, downloadProgress = 0f, downloadSpeedMbps = 0f)
        }

        downloadJob = scope.launch {
            try {
                val targetDir = manager.getModelDir(model.id)
                downloader.downloadModel(model, targetDir) { progress ->
                    _state.update {
                        it.copy(
                            downloadProgress = progress.fraction,
                            downloadSpeedMbps = progress.speedMbPerSec,
                        )
                    }
                }
                println("MODEL[CTRL] Download complete: ${model.id}")
            } catch (e: Exception) {
                println("MODEL[CTRL] Download failed: ${e.message}")
            } finally {
                _state.update {
                    it.copy(downloadingModelId = null, downloadProgress = 0f, downloadSpeedMbps = 0f)
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
    }

    /** Activate a downloaded model — creates inference engine + sets VlmEngineHolder. */
    fun activate(modelId: String) {
        scope.launch {
            try {
                val model = manager.getDownloadedModels().find { it.info.id == modelId }
                    ?: return@launch

                manager.setActiveModel(modelId)

                // Create engine based on runtime
                val engine: InferenceEngine? = when (model.info.runtime) {
                    ModelRuntime.LLAMA_CPP -> {
                        val llama = LlamaEngine()
                        withContext(Dispatchers.IO) {
                            llama.loadModel(
                                basePath = model.primaryPath,
                                mmprojPath = model.mmprojPath ?: "",
                                threads = manager.threadCount,
                            )
                        }
                        llama
                    }
                    ModelRuntime.LITERT_LM -> {
                        val liteRT = LiteRTEngine()
                        withContext(Dispatchers.IO) {
                            liteRT.loadModel(
                                modelPath = model.primaryPath,
                                preferGpu = true,
                            )
                        }
                        liteRT
                    }
                }

                if (engine != null) {
                    VlmEngineHolder.activeEngine = engine
                    println("MODEL[CTRL] Engine activated: ${model.info.displayName}")
                }

                refresh()
            } catch (e: Exception) {
                println("MODEL[CTRL] Activate failed: ${e.message}")
            }
        }
    }

    /** Delete a downloaded model. */
    fun delete(modelId: String) {
        // Unload engine if this is the active model
        if (manager.getActiveModelId() == modelId) {
            VlmEngineHolder.activeEngine = null
        }
        manager.delete(modelId)
        refresh()
    }

    /** Update thread count. */
    fun setThreadCount(count: Int) {
        manager.threadCount = count
        _state.update { it.copy(threadCount = count) }
    }
}

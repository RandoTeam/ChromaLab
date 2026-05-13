package com.chromalab.feature.settings

/**
 * Platform-provided state for the Model Manager UI.
 *
 * This is a common interface consumed by App.kt / ModelManagerScreen / MoreScreen.
 * The actual implementation lives in androidMain where ModelManager is available.
 */
data class ModelManagerState(
    val downloadedModelIds: Set<String> = emptySet(),
    val activeModelId: String? = null,
    val activeModelName: String? = null,
    /** Short summary for MoreScreen: e.g. "LiteRT GPU · 2.6 GB" */
    val activeModelSummary: String? = null,
    val downloadJobs: Map<String, ModelDownloadUiState> = emptyMap(),
    val downloadingModelId: String? = null,
    val downloadProgress: Float = 0f,
    val downloadSpeedMbps: Float = 0f,
    val downloadFileName: String = "",
    val downloadError: String? = null,
    /** Non-null while a model is being loaded into RAM (activate in progress). */
    val activatingModelId: String? = null,
    /** Error message from last activation attempt. */
    val activationError: String? = null,
    val deviceRamMb: Int = 4096,
    val availableStorageGb: Float = 0f,
    val totalModelDiskUsageGb: Float = 0f,
    val threadCount: Int = 4,
    /** Auto-unload VLM model after N minutes of inactivity (1–30, 0 = disabled). */
    val autoUnloadMinutes: Int = 5,
    /** Custom (imported) models that aren't in the builtin registry. */
    val customModels: List<CustomModelEntry> = emptyList(),
    /** True while an import is in progress. */
    val isImporting: Boolean = false,
    /** Hugging Face model search state. */
    val huggingFaceSearch: HuggingFaceSearchState = HuggingFaceSearchState(),
)

enum class ModelDownloadUiPhase {
    QUEUED,
    CONNECTING,
    DOWNLOADING,
    VALIDATING,
    COMPLETE,
    ERROR,
}

data class ModelDownloadUiState(
    val modelId: String,
    val phase: ModelDownloadUiPhase,
    val progress: Float = 0f,
    val speedMbps: Float = 0f,
    val fileName: String = "",
    val error: String? = null,
)

/**
 * Minimal info about a custom/imported model for UI display.
 * Kept separate from ModelInfo to avoid commonMain depending on android types.
 */
data class CustomModelEntry(
    val id: String,
    val displayName: String,
    val sizeBytes: Long,
    val description: String = "",
    val supportsTextChat: Boolean = true,
)

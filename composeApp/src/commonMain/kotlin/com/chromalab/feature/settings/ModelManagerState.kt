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
    val downloadingModelId: String? = null,
    val downloadProgress: Float = 0f,
    val downloadSpeedMbps: Float = 0f,
    val downloadFileName: String = "",
    val downloadError: String? = null,
    val deviceRamMb: Int = 4096,
    val availableStorageGb: Float = 0f,
    val totalModelDiskUsageGb: Float = 0f,
    val threadCount: Int = 4,
)

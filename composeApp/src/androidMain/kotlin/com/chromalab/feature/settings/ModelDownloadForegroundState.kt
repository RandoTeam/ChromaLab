package com.chromalab.feature.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-wide bridge between the Android foreground download service and UI controllers.
 *
 * The service owns download execution. Controllers only observe this state, so downloads can
 * continue after the model manager screen leaves composition.
 */
internal object ModelDownloadForegroundState {
    private val _downloadJobs = MutableStateFlow<Map<String, ModelDownloadUiState>>(emptyMap())
    val downloadJobs: StateFlow<Map<String, ModelDownloadUiState>> = _downloadJobs.asStateFlow()

    fun upsert(state: ModelDownloadUiState) {
        _downloadJobs.update { it + (state.modelId to state) }
    }

    fun remove(modelId: String) {
        _downloadJobs.update { it - modelId }
    }

    fun snapshot(): Map<String, ModelDownloadUiState> = _downloadJobs.value
}

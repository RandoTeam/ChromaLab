package com.chromalab.feature.settings

import androidx.compose.runtime.Composable
import com.chromalab.feature.processing.model.ModelInfo

/**
 * Platform-provided state + actions for Model Manager UI.
 *
 * commonMain composables call rememberModelManagerState() to get a Pair of
 * (state, actions) that they can use without knowing the platform details.
 */
data class ModelManagerActions(
    val download: (ModelInfo) -> Unit = {},
    val cancelDownload: () -> Unit = {},
    val activate: (String) -> Unit = {},
    val deactivate: () -> Unit = {},
    val delete: (String) -> Unit = {},
    val setThreadCount: (Int) -> Unit = {},
    val setAutoUnloadMinutes: (Int) -> Unit = {},
    val onImport: () -> Unit = {},
)

/**
 * Platform-specific composable that provides model manager state + actions.
 */
@Composable
expect fun rememberModelManagerState(): Pair<ModelManagerState, ModelManagerActions>

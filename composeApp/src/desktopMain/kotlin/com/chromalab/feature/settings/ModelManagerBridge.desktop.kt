package com.chromalab.feature.settings

import androidx.compose.runtime.Composable

/**
 * Desktop stub — no model manager on desktop.
 */
@Composable
actual fun rememberModelManagerState(): Pair<ModelManagerState, ModelManagerActions> {
    return ModelManagerState() to ModelManagerActions()
}

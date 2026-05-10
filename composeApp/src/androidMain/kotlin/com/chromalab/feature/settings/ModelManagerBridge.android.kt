package com.chromalab.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation — creates ModelManagerController and provides state + actions.
 */
@Composable
actual fun rememberModelManagerState(): Pair<ModelManagerState, ModelManagerActions> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val controller = remember {
        ModelManagerController(context.applicationContext, scope)
    }

    val state by controller.state.collectAsState()

    val actions = remember(controller) {
        ModelManagerActions(
            download = { controller.download(it) },
            cancelDownload = { controller.cancelDownload() },
            activate = { controller.activate(it) },
            deactivate = { controller.deactivate() },
            delete = { controller.delete(it) },
            setThreadCount = { controller.setThreadCount(it) },
            onImport = { println("MODEL[BRIDGE] Import requested — file picker not yet wired") },
        )
    }

    return state to actions
}

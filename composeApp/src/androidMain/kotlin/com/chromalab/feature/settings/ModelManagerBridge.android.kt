package com.chromalab.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/**
 * Android implementation — creates ModelManagerController and provides state + actions.
 * File picker is wired via ActivityResultContracts.OpenDocument().
 */
@Composable
actual fun rememberModelManagerState(): Pair<ModelManagerState, ModelManagerActions> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val controller = remember {
        ModelManagerController(context.applicationContext, scope).also {
            com.chromalab.feature.processing.inference.VlmEngineHolder.controller = it
        }
    }

    val state by controller.state.collectAsState()

    // File picker launcher — accepts .gguf and .litertlm files
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { controller.importFile(it) }
    }

    val actions = remember(controller, importLauncher) {
        ModelManagerActions(
            download = { controller.download(it) },
            cancelDownload = { controller.cancelDownload() },
            activate = { controller.activate(it) },
            deactivate = { controller.deactivate() },
            delete = { controller.delete(it) },
            setThreadCount = { controller.setThreadCount(it) },
            onImport = { importLauncher.launch(arrayOf("*/*")) },
        )
    }

    return state to actions
}

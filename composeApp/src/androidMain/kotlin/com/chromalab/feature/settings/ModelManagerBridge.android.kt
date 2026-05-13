package com.chromalab.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android implementation — creates ModelManagerController and provides state + actions.
 * File picker is wired via ActivityResultContracts.OpenDocument().
 * Export uses CreateDocument for each model file sequentially.
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

    // Export state: list of (fileName, srcFile) pairs and current index
    val exportFiles = remember { mutableStateOf<List<Pair<String, java.io.File>>>(emptyList()) }
    val exportIndex = remember { mutableIntStateOf(0) }

    // File picker launcher — accepts .gguf and .litertlm files
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { controller.importFile(it) }
    }

    // CreateDocument launcher for exporting one model file at a time
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { targetUri: Uri? ->
        val files = exportFiles.value
        val idx = exportIndex.intValue

        if (targetUri != null && idx < files.size) {
            val (fileName, srcFile) = files[idx]
            scope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(targetUri)?.use { out ->
                            srcFile.inputStream().use { input ->
                                input.copyTo(out, bufferSize = 65536)
                            }
                        }
                        println("EXPORT[OK] $fileName exported")
                    } catch (e: Exception) {
                        println("EXPORT[ERROR] $fileName: ${e.message}")
                        withContext(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                context, "Ошибка экспорта: $fileName",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
            }
        }
        // Move to next file
        exportIndex.intValue = idx + 1
    }

    // Auto-launch NEXT file picker when index advances (idx > 0 only)
    LaunchedEffect(exportIndex.intValue) {
        val files = exportFiles.value
        val idx = exportIndex.intValue
        if (idx > 0 && idx < files.size) {
            exportLauncher.launch(files[idx].first)
        } else if (idx > 0 && idx >= files.size) {
            // All done — reset and show toast
            val count = files.size
            exportFiles.value = emptyList()
            exportIndex.intValue = 0
            android.widget.Toast.makeText(
                context,
                "Экспорт завершён ($count файл${if (count > 1) "ов" else ""})",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }

    val actions = remember(controller, importLauncher, exportLauncher) {
        ModelManagerActions(
            download = { controller.download(it) },
            cancelDownload = { controller.cancelDownload(it) },
            activate = { controller.activate(it) },
            deactivate = { controller.deactivate() },
            delete = { controller.delete(it) },
            setThreadCount = { controller.setThreadCount(it) },
            setDownloadParallelism = { controller.setDownloadParallelism(it) },
            setDownloadSpeedLimit = { controller.setDownloadSpeedLimit(it) },
            setAutoUnloadMinutes = { controller.setAutoUnloadMinutes(it) },
            setChromatogramModel = { controller.setChromatogramModel(it) },
            onImport = { importLauncher.launch(arrayOf("*/*")) },
            onHuggingFaceQueryChange = { controller.updateHuggingFaceQuery(it) },
            onHuggingFaceSortChange = { controller.setHuggingFaceSort(it) },
            onHuggingFaceSearch = { controller.searchHuggingFace() },
            onExport = { modelId ->
                val manager = com.chromalab.feature.processing.model.ModelManager(context.applicationContext)
                val model = manager.getDownloadedModels().find { it.info.id == modelId }
                if (model != null) {
                    val filePairs = model.info.files.mapNotNull { file ->
                        val srcFile = java.io.File(model.localDir, file.fileName)
                        if (srcFile.exists()) file.fileName to srcFile else null
                    }
                    if (filePairs.isNotEmpty()) {
                        exportFiles.value = filePairs
                        exportIndex.intValue = 0
                        // Launch first file picker directly
                        exportLauncher.launch(filePairs.first().first)
                    }
                }
            },
        )
    }

    return state to actions
}

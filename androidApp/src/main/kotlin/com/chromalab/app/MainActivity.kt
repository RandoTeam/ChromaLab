package com.chromalab.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.chromalab.feature.processing.inference.GgufParityDiagnostics
import com.chromalab.feature.processing.inference.VlmEngineHolder
import com.chromalab.feature.settings.ModelManagerController

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "ChromaLabMain"
        private const val ACTION_DEBUG_GGUF_PARITY = "com.chromalab.app.DEBUG_GGUF_PARITY"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.chromalab.core.data.DatabaseProvider.init(application)

        // Initialize ModelManagerController for VLM lazy loading (25.2B)
        // This must happen at startup so the pipeline can auto-load models
        if (VlmEngineHolder.controller == null) {
            VlmEngineHolder.controller = ModelManagerController(applicationContext, lifecycleScope)
        }

        setContent {
            App()
        }

        handleDebugIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDebugIntent(intent)
    }

    // 25.2C: Unload VLM model under memory pressure to prevent OOM
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // TRIM_MEMORY_RUNNING_LOW = 10
        if (level >= 10) {
            val engine = VlmEngineHolder.activeEngine
            if (engine != null && engine.isLoaded()) {
                println("VLM[MEMORY] Unloading model due to TRIM_MEMORY (level=$level)")
                VlmEngineHolder.activeEngine = null
                VlmEngineHolder.activeConfig = null
            }
        }
    }

    override fun onDestroy() {
        // Unload VLM engine on activity destruction
        VlmEngineHolder.activeEngine = null
        VlmEngineHolder.activeConfig = null
        super.onDestroy()
    }

    private fun handleDebugIntent(intent: Intent?) {
        if (intent?.action != ACTION_DEBUG_GGUF_PARITY) return
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isDebuggable) {
            Log.w(TAG, "Ignoring GGUF parity diagnostics intent in non-debug build")
            return
        }

        val modelId = intent.getStringExtra("modelId")
        val imagePath = intent.getStringExtra("imagePath")
        Log.i(TAG, "Starting GGUF parity diagnostics modelId=${modelId.orEmpty()} imagePath=${imagePath.orEmpty()}")
        lifecycleScope.launch {
            GgufParityDiagnostics(applicationContext).run(
                requestedModelId = modelId,
                requestedImagePath = imagePath,
            )
        }
    }
}

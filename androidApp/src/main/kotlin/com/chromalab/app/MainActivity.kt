package com.chromalab.app

import android.content.ComponentCallbacks2
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.chromalab.feature.processing.inference.VlmEngineHolder
import com.chromalab.feature.settings.ModelManagerController

class MainActivity : ComponentActivity() {
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
    }

    // 25.2C: Unload VLM model under memory pressure to prevent OOM
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
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
}

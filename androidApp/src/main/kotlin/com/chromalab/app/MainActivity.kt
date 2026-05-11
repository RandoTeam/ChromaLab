package com.chromalab.app

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
}

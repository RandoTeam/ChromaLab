package com.chromalab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.chromalab.core.data.DatabaseProvider.init(application)
        setContent {
            App()
        }
    }
}

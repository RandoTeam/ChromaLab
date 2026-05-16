package com.chromalab.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main(args: Array<String>) {
    if (runDesktopOfflineAnalysisCli(args)) return

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ChromaLab"
        ) {
            App()
        }
    }
}

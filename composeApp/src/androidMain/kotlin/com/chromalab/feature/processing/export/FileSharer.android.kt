package com.chromalab.feature.processing.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

actual object FileSharer {
    /** Must be set from Activity/Application before use */
    var contextProvider: (() -> Context)? = null

    actual fun share(filePath: String, mimeType: String) {
        val context = contextProvider?.invoke() ?: return
        val file = File(filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Экспорт").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}

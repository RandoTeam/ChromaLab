package com.chromalab.feature.capture

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation — wraps FileImportScreen with SAF OpenDocument picker.
 *
 * Reads selected file content via ContentResolver and passes it
 * to the common FileImportScreen for parsing and display.
 */
@Composable
actual fun FileImportScreenWithPicker(
    onSaved: (signalId: Long) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var fileContent by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Read file name from URI
            fileName = getFileNameFromUri(context, it)
            // Read full content as text
            fileContent = try {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.readText()
            } catch (e: Exception) {
                println("IMPORT[PICKER] Read error: ${e.message}")
                null
            }
        }
    }

    FileImportScreen(
        onPickFile = {
            launcher.launch(arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "application/json",
                "text/plain",
                "*/*",
            ))
        },
        fileContent = fileContent,
        fileName = fileName,
        onSaved = onSaved,
        onBack = onBack,
    )
}

/**
 * Extract display name from a content URI.
 */
private fun getFileNameFromUri(context: android.content.Context, uri: Uri): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    return cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) it.getString(nameIndex) else null
        } else null
    }
}

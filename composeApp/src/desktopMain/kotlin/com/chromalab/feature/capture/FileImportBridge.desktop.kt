package com.chromalab.feature.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun FileImportScreenWithPicker(
    onSaved: (signalId: Long) -> Unit,
    onBack: () -> Unit,
) {
    var fileContent by remember { mutableStateOf<String?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }

    FileImportScreen(
        onPickFile = {
            val chooser = JFileChooser().apply {
                dialogTitle = "Select chromatogram file"
                fileFilter = FileNameExtensionFilter(
                    "Chromatogram data (*.csv, *.txt, *.json)",
                    "csv",
                    "txt",
                    "json",
                )
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val selectedFile: File = chooser.selectedFile
                fileContent = selectedFile.readText()
                fileName = selectedFile.name
            }
        },
        fileContent = fileContent,
        fileName = fileName,
        onSaved = onSaved,
        onBack = onBack,
    )
}

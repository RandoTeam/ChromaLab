package com.chromalab.feature.calculation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chromalab.core.ui.theme.Spacing

/**
 * Export calculation screen — CSV/JSON export of peaks, signals, baseline, warnings.
 */
@Composable
fun ExportCalculationScreen(
    runId: String,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onShare: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Экспорт результатов", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(Spacing.md))
        Button(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
            Text("Экспорт peaks.csv")
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Button(onClick = onExportJson, modifier = Modifier.fillMaxWidth()) {
            Text("Экспорт calculation.json")
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        OutlinedButton(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
            Text("Поделиться")
        }
    }
}

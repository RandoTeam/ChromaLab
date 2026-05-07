package com.chromalab.feature.calculation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chromalab.core.ui.theme.Spacing

/**
 * Main chromatogram analysis screen — interactive chart + layers + peak detection.
 */
@Composable
fun ChromatogramAnalysisScreen(
    signalId: String,
    onPeakTap: (peakId: Int) -> Unit,
    onFindPeaks: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Анализ хроматограммы", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text("Signal: $signalId", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(Spacing.md))
        Text("Интерактивный график будет здесь",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

package com.chromalab.feature.calculation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chromalab.core.ui.theme.Spacing

/**
 * Peak table — sortable/filterable list of detected peaks with status, RT, height, area, S/N.
 */
@Composable
fun PeakTableScreen(
    onPeakTap: (peakId: Int) -> Unit,
    onExportCsv: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Таблица пиков", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text("Status • RT • Height • Area • Width • S/N • Confidence",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

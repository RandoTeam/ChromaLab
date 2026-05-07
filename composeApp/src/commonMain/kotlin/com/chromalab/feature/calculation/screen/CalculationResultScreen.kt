package com.chromalab.feature.calculation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chromalab.core.ui.theme.Spacing

/**
 * Calculation result screen — summary of a CalculationRun.
 */
@Composable
fun CalculationResultScreen(
    runId: String,
    onExport: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Результат расчёта", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text("Пики • Метрики • Warnings • Confidence",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

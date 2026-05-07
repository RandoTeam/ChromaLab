package com.chromalab.feature.calculation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chromalab.core.ui.theme.Spacing

/**
 * Noise region selection — manual/auto noise region for S/N calculation.
 */
@Composable
fun NoiseRegionScreen(
    onSelectRegion: (startTime: Double, endTime: Double) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Выбор области шума", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text("Ручной или автоматический выбор noise region",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

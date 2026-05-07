package com.chromalab.feature.calculation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing

/**
 * Peak details bottom sheet — RT, height, area, width, S/N, prominence, confidence, warnings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeakDetailsBottomSheet(
    peakId: Int,
    onDismiss: () -> Unit,
    onCorrectBoundaries: () -> Unit,
    onReject: () -> Unit,
    onAccept: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text("Пик #$peakId", style = MaterialTheme.typography.titleLarge)
            HorizontalDivider()
            MetricRow("RT apex", "—")
            MetricRow("Высота", "—")
            MetricRow("Площадь", "—")
            MetricRow("Ширина", "—")
            MetricRow("S/N", "—")
            MetricRow("Prominence", "—")
            MetricRow("Confidence", "—")
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                    Text("Отклонить")
                }
                OutlinedButton(onClick = onCorrectBoundaries, modifier = Modifier.weight(1f)) {
                    Text("Границы")
                }
                Button(onClick = onAccept, modifier = Modifier.weight(1f)) {
                    Text("Принять")
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

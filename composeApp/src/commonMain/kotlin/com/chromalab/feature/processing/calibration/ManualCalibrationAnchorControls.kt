package com.chromalab.feature.processing.calibration

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing

@Composable
internal fun ManualCalibrationAnchorControls(
    anchors: List<AxisCalibrationAnchor>,
    selectedPoint: Int,
    usedValues: Set<String>,
    onSelectedPointChange: (Int) -> Unit,
    onAnchorSelected: (AxisCalibrationAnchor) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (anchors.isEmpty()) return

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                "OCR:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilterChip(
                selected = selectedPoint == 1,
                onClick = { onSelectedPointChange(1) },
                label = { Text("P1") },
            )
            FilterChip(
                selected = selectedPoint == 2,
                onClick = { onSelectedPointChange(2) },
                label = { Text("P2") },
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "найденные деления",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            anchors.forEach { anchor ->
                val label = formatCalibrationAnchorValue(anchor.value)
                FilterChip(
                    selected = label in usedValues,
                    onClick = { onAnchorSelected(anchor) },
                    label = { Text(label) },
                )
            }
        }
    }
}

internal fun formatCalibrationAnchorValue(value: Float): String =
    value.toBigDecimal()
        .stripTrailingZeros()
        .toPlainString()

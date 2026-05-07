package com.chromalab.feature.calculation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.calculation.algorithm.ConfidenceGrade
import com.chromalab.feature.calculation.core.PeakStatus
import com.chromalab.feature.calculation.core.WarningSeverity

/**
 * Phase 2 UX components — encoding the UX principles from §2.4.
 *
 * Rules:
 * - Chart is primary, table is secondary
 * - Tech params hidden by default (expandable)
 * - Presets for common users
 * - Human-readable warnings
 * - Clear visual markers on chart
 * - Status badges on every peak
 */

// ─── Peak Status Badge ──────────────────────────────────────────

@Composable
fun PeakStatusBadge(status: PeakStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        PeakStatus.AUTO -> "Авто" to MaterialTheme.colorScheme.primary
        PeakStatus.MANUAL -> "Ручной" to MaterialTheme.colorScheme.tertiary
        PeakStatus.CORRECTED -> "Исправлен" to Color(0xFFFFA726)
        PeakStatus.REJECTED -> "Отклонён" to MaterialTheme.colorScheme.error
        PeakStatus.LOW_CONFIDENCE -> "Низкая уверенность" to Color(0xFFEF5350)
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// ─── Confidence Badge ───────────────────────────────────────────

@Composable
fun ConfidenceBadge(confidence: ConfidenceGrade, modifier: Modifier = Modifier) {
    val (label, color) = when (confidence) {
        ConfidenceGrade.HIGH -> "Высокая" to Color(0xFF4CAF50)
        ConfidenceGrade.MEDIUM -> "Средняя" to Color(0xFFFFA726)
        ConfidenceGrade.LOW -> "Низкая" to Color(0xFFEF5350)
        ConfidenceGrade.FAILED -> "Ошибка" to MaterialTheme.colorScheme.error
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
        modifier = modifier,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// ─── Warning Chip ───────────────────────────────────────────────

@Composable
fun WarningChip(message: String, severity: WarningSeverity, modifier: Modifier = Modifier) {
    val (icon, color) = when (severity) {
        WarningSeverity.INFO -> Icons.Filled.Info to MaterialTheme.colorScheme.primary
        WarningSeverity.CAUTION -> Icons.Filled.Warning to Color(0xFFFFA726)
        WarningSeverity.SERIOUS -> Icons.Filled.Warning to Color(0xFFEF5350)
        WarningSeverity.FAILED -> Icons.Filled.Error to MaterialTheme.colorScheme.error
    }
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
        Text(message, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

// ─── Preset Selector ────────────────────────────────────────────

enum class AlgorithmPreset(val label: String, val description: String) {
    CONSERVATIVE("Консервативный", "Меньше ложных срабатываний"),
    BALANCED("Сбалансированный", "Настройки по умолчанию"),
    SENSITIVE("Чувствительный", "Больше кандидатов, больше проверки"),
    MANUAL_REVIEW("Ручной", "Минимум авто, акцент на ручные правки"),
}

@Composable
fun PresetSelector(
    selected: AlgorithmPreset,
    onSelect: (AlgorithmPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("Профиль анализа", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            AlgorithmPreset.entries.forEach { preset ->
                FilterChip(
                    selected = preset == selected,
                    onClick = { onSelect(preset) },
                    label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ─── Layer Toggle Row ───────────────────────────────────────────

data class LayerToggle(
    val id: String,
    val label: String,
    val color: Color,
    val enabled: Boolean,
)

@Composable
fun LayerToggleRow(
    layers: List<LayerToggle>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        layers.forEach { layer ->
            FilterChip(
                selected = layer.enabled,
                onClick = { onToggle(layer.id) },
                label = { Text(layer.label, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(layer.color, RoundedCornerShape(50)),
                    )
                },
            )
        }
    }
}

// ─── Chart Marker Legend ────────────────────────────────────────

@Composable
fun ChartMarkerLegend(modifier: Modifier = Modifier) {
    val markers = listOf(
        "▲ Apex" to MaterialTheme.colorScheme.primary,
        "◀ Левая граница" to Color(0xFF66BB6A),
        "▶ Правая граница" to Color(0xFF66BB6A),
        "── Baseline" to Color(0xFF42A5F5),
        "░ Noise region" to Color(0xFFBDBDBD),
        "✕ Отклонён" to MaterialTheme.colorScheme.error,
        "★ Ручной" to MaterialTheme.colorScheme.tertiary,
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        markers.forEach { (label, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Box(modifier = Modifier.size(6.dp).background(color, RoundedCornerShape(50)))
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─── Expandable Tech Details ────────────────────────────────────

@Composable
fun ExpandableTechDetails(
    title: String = "Подробнее",
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        TextButton(onClick = { expanded = !expanded }) {
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
        }
        if (expanded) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.sm),
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    content = content,
                )
            }
        }
    }
}

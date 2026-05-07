package com.chromalab.feature.calculation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.calculation.algorithm.*

/**
 * Peak details bottom sheet (§2.23).
 *
 * Shows all peak information when user taps on a peak.
 * Primary metrics visible immediately; tech details expandable.
 */

// ─── Data model ─────────────────────────────────────────────────

data class PeakDetailsData(
    val peakId: Int,
    val metrics: PeakMetrics,
    val confidence: PeakConfidence,
    val integration: IntegrationResult,
    val noiseMethod: NoiseMethod = NoiseMethod.PEAK_TO_PEAK,
    val baselineMethod: String = "ALS",
)

// ─── Bottom Sheet Content ───────────────────────────────────────

@Composable
fun PeakDetailsContent(
    data: PeakDetailsData,
    onEditBoundaries: (() -> Unit)? = null,
    onSelectNoiseRegion: (() -> Unit)? = null,
    onChangeBaseline: (() -> Unit)? = null,
    onAcceptPeak: (() -> Unit)? = null,
    onRejectPeak: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Header
        PeakHeader(data)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // Primary metrics
        PrimaryMetrics(data.metrics)

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // Confidence
        ConfidenceSection(data.confidence)

        // Warnings
        if (data.metrics.warnings.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            WarningsSection(data.metrics.warnings)
        }

        // Action buttons
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ActionButtons(
            onEditBoundaries = onEditBoundaries,
            onSelectNoiseRegion = onSelectNoiseRegion,
            onChangeBaseline = onChangeBaseline,
            onAcceptPeak = onAcceptPeak,
            onRejectPeak = onRejectPeak,
        )

        // Tech details (expandable)
        ExpandableTechDetails("Технические детали") {
            TechDetailsContent(data)
        }

        Spacer(modifier = Modifier.height(Spacing.md))
    }
}

// ─── Header ─────────────────────────────────────────────────────

@Composable
private fun PeakHeader(data: PeakDetailsData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                "Пик #${data.peakId + 1}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "RT = ${"%.3f".format(data.metrics.rtApex)} мин",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = confidenceColor(data.confidence.grade).copy(alpha = 0.15f),
        ) {
            Text(
                data.confidence.grade.label,
                style = MaterialTheme.typography.labelMedium,
                color = confidenceColor(data.confidence.grade),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

// ─── Primary Metrics ────────────────────────────────────────────

@Composable
private fun PrimaryMetrics(metrics: PeakMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Основные параметры", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            MetricCard("Высота", formatValue(metrics.height), Modifier.weight(1f))
            MetricCard("Площадь", formatValue(metrics.area), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            MetricCard("Ширина (база)", "%.3f".format(metrics.widthBase), Modifier.weight(1f))
            MetricCard("FWHM", "%.3f".format(metrics.widthHalfHeight), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            MetricCard("S/N", "%.1f".format(metrics.snrValue), Modifier.weight(1f))
            MetricCard("Prominence", formatValue(metrics.prominence), Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
        }
    }
}

// ─── Confidence ─────────────────────────────────────────────────

@Composable
private fun ConfidenceSection(confidence: PeakConfidence) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Уверенность", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Factor bars
        confidence.factors.forEach { factor ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    factor.name,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(80.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { factor.score.toFloat() },
                    modifier = Modifier.weight(1f).height(6.dp),
                    color = factorColor(factor.score),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text(
                    "${(factor.score * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(32.dp),
                )
            }
        }
    }
}

// ─── Warnings ───────────────────────────────────────────────────

@Composable
private fun WarningsSection(warnings: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Предупреждения", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        warnings.forEach { warning ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color(0xFFFFA726),
                )
                Text(
                    warning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── Action Buttons ─────────────────────────────────────────────

@Composable
private fun ActionButtons(
    onEditBoundaries: (() -> Unit)?,
    onSelectNoiseRegion: (() -> Unit)?,
    onChangeBaseline: (() -> Unit)?,
    onAcceptPeak: (() -> Unit)?,
    onRejectPeak: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            onEditBoundaries?.let {
                OutlinedButton(onClick = it, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Границы", style = MaterialTheme.typography.labelSmall)
                }
            }
            onSelectNoiseRegion?.let {
                OutlinedButton(onClick = it, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Waves, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Шум", style = MaterialTheme.typography.labelSmall)
                }
            }
            onChangeBaseline?.let {
                OutlinedButton(onClick = it, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Timeline, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Baseline", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            onAcceptPeak?.let {
                Button(
                    onClick = it,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Принять", style = MaterialTheme.typography.labelSmall)
                }
            }
            onRejectPeak?.let {
                Button(
                    onClick = it,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Отклонить", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ─── Tech Details ───────────────────────────────────────────────

@Composable
private fun TechDetailsContent(data: PeakDetailsData) {
    DetailRow("RT centroid", "%.4f мин".format(data.metrics.rtCentroid))
    DetailRow("Ширина полупроминенции", "%.3f".format(data.metrics.widthHalfProminence))
    DetailRow("Метод границ", data.metrics.boundaryMethod.label)
    DetailRow("Уверенность границ", "${(data.metrics.boundaryConfidence * 100).toInt()}%")
    DetailRow("Перекрытие", data.metrics.overlapStatus.label)
    DetailRow("S/N флаг", data.metrics.snrFlag.label)
    DetailRow("Baseline метод", data.baselineMethod)
    DetailRow("Noise метод", data.noiseMethod.label)
    DetailRow("Интеграция метод", data.integration.method.label)
    DetailRow("Площадь (+)", formatValue(data.integration.positiveArea))
    DetailRow("Площадь (−)", formatValue(data.integration.negativeArea))
    DetailRow("Ручные правки", if (data.metrics.isManuallyEdited) "Да" else "Нет")
    DetailRow("Confidence score", "%.3f".format(data.confidence.score))
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium)
    }
}

// ─── Utilities ──────────────────────────────────────────────────

private fun confidenceColor(grade: ConfidenceGrade): Color = when (grade) {
    ConfidenceGrade.HIGH -> Color(0xFF4CAF50)
    ConfidenceGrade.MEDIUM -> Color(0xFFFFA726)
    ConfidenceGrade.LOW -> Color(0xFFEF5350)
    ConfidenceGrade.FAILED -> Color(0xFFD32F2F)
}

private fun factorColor(score: Double): Color = when {
    score >= 0.7 -> Color(0xFF4CAF50)
    score >= 0.4 -> Color(0xFFFFA726)
    else -> Color(0xFFEF5350)
}

private fun formatValue(value: Double): String = when {
    kotlin.math.abs(value) >= 1_000_000 -> "%.2fM".format(value / 1_000_000)
    kotlin.math.abs(value) >= 1_000 -> "%.1fK".format(value / 1_000)
    kotlin.math.abs(value) >= 1 -> "%.1f".format(value)
    else -> "%.3f".format(value)
}

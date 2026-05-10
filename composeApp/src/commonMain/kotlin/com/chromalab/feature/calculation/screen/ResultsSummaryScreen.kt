package com.chromalab.feature.calculation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.log10
import kotlin.math.pow
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.calculation.algorithm.ConfidenceGrade
import com.chromalab.feature.calculation.algorithm.OverlapStatus
import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.calculation.core.PeakResult
import com.chromalab.feature.calculation.core.WarningSeverity
import com.chromalab.feature.calculation.flow.CalculationToChartMapper
import com.chromalab.feature.calculation.ui.ChromatogramChart

/**
 * Results summary screen — professional chromatographic analysis report.
 *
 * Sections:
 * 1. Summary card — key metrics
 * 2. Chromatogram chart — corrected + peaks
 * 3. Peak results table
 * 4. System suitability table (USP)
 * 5. Quality overview
 * 6. Algorithm parameters (expandable)
 */
@Composable
fun ResultsSummaryScreen(
    run: CalculationRun,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Section 1: Summary
        SummarySection(run)

        // Section 2: Chromatogram
        ChartSection(run)

        // Section 3: Peak results table
        PeakResultsTableSection(run.peaks)

        // Section 4: System suitability (USP)
        SystemSuitabilitySection(run.peaks)

        // Section 5: Quality overview
        QualitySection(run)

        // Section 6: Parameters (expandable)
        ParametersSection(run)

        Spacer(modifier = Modifier.height(Spacing.lg))
    }
}

// ─── Section 1: Summary Card ────────────────────────────────────

@Composable
private fun SummarySection(run: CalculationRun) {
    SectionHeader("Сводка анализа")

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val peaks = run.peaks
            val totalArea = peaks.sumOf { it.area }
            val autoCount = peaks.count { it.status == com.chromalab.feature.calculation.core.PeakStatus.AUTO }
            val manualCount = peaks.count {
                it.status == com.chromalab.feature.calculation.core.PeakStatus.MANUAL ||
                    it.status == com.chromalab.feature.calculation.core.PeakStatus.CORRECTED
            }
            val rejectedCount = peaks.count { it.status == com.chromalab.feature.calculation.core.PeakStatus.REJECTED }

            val rtMin = peaks.minOfOrNull { it.rtApex } ?: 0.0
            val rtMax = peaks.maxOfOrNull { it.rtApex } ?: 0.0

            val timestamp = run.timestamp
            val dateStr = formatTimestamp(timestamp)

            SummaryRow("Дата", dateStr)
            SummaryRow("Пресет", run.params.presetName)
            SummaryRow("Pipeline", "${run.pipelineVersion} / ${run.algorithmVersion}")
            SummaryRow("Пиков", "${peaks.size} (${autoCount}A · ${manualCount}M · ${rejectedCount}R)")
            SummaryRow("Σ Площадь", formatLargeNumber(totalArea))
            SummaryRow("Диапазон RT", "${"%.2f".format(rtMin)} — ${"%.2f".format(rtMax)} мин")
            if (run.warnings.isNotEmpty()) {
                SummaryRow("Предупреждений", "${run.warnings.size}")
            }
        }
    }
}

// ─── Section 2: Chart ───────────────────────────────────────────

@Composable
private fun ChartSection(run: CalculationRun) {
    SectionHeader("Хроматограмма")

    val chartState = remember(run) {
        CalculationToChartMapper.buildChartState(
            run = run,
            visibleLayers = setOf("corrected"),
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        ChromatogramChart(
            state = chartState,
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(8.dp),
        )
    }
}

// ─── Section 3: Peak Results Table ──────────────────────────────

@Composable
private fun PeakResultsTableSection(peaks: List<PeakResult>) {
    SectionHeader("Таблица пиков")

    val totalArea = peaks.sumOf { it.area }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            ) {
                TableHeader("#", 32.dp)
                TableHeader("RT", 60.dp)
                TableHeader("Высота", 68.dp)
                TableHeader("Площадь", 76.dp)
                TableHeader("Area%", 52.dp)
                TableHeader("S/N", 48.dp)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Rows
            peaks.forEachIndexed { i, peak ->
                val areaPercent = if (totalArea > 0) peak.area / totalArea * 100.0 else 0.0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    TableCell("${i + 1}", 32.dp)
                    TableCell("%.3f".format(peak.rtApex), 60.dp)
                    TableCell(formatCompact(peak.height), 68.dp)
                    TableCell(formatCompact(peak.area), 76.dp)
                    TableCell("%.1f".format(areaPercent), 52.dp)
                    TableCell("%.1f".format(peak.snr), 48.dp)
                }
            }

            // Footer
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Σ: ${peaks.size} пиков",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Σ Area: ${formatLargeNumber(totalArea)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ─── Section 4: System Suitability ──────────────────────────────

@Composable
private fun SystemSuitabilitySection(peaks: List<PeakResult>) {
    SectionHeader("Системная пригодность (USP)")

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
            ) {
                TableHeader("Пик", 40.dp)
                TableHeader("Tailing", 60.dp)
                TableHeader("Asym.", 56.dp)
                TableHeader("N", 64.dp)
                TableHeader("Rs", 48.dp)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Rows
            peaks.forEachIndexed { i, peak ->
                val tailingColor = when {
                    peak.tailingFactor > 2.0 -> MaterialTheme.colorScheme.error
                    peak.tailingFactor > 1.5 -> Color(0xFFFFA726)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    TableCell("#${i + 1}", 40.dp)
                    TableCell(
                        "%.3f".format(peak.tailingFactor),
                        60.dp,
                        color = tailingColor,
                    )
                    TableCell("%.3f".format(peak.asymmetryFactor), 56.dp)
                    TableCell(
                        peak.plateCount?.let { formatLargeNumber(it.toDouble()) } ?: "—",
                        64.dp,
                    )
                    TableCell(
                        peak.resolution?.let { "%.2f".format(it) } ?: "—",
                        48.dp,
                    )
                }
            }
        }
    }
}

// ─── Section 5: Quality ─────────────────────────────────────────

@Composable
private fun QualitySection(run: CalculationRun) {
    SectionHeader("Качество")

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Confidence distribution
            val highCount = run.peaks.count { it.confidence == ConfidenceGrade.HIGH }
            val medCount = run.peaks.count { it.confidence == ConfidenceGrade.MEDIUM }
            val lowCount = run.peaks.count { it.confidence == ConfidenceGrade.LOW }
            val failCount = run.peaks.count { it.confidence == ConfidenceGrade.FAILED }

            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ConfidenceBadge("HIGH", highCount, Color(0xFF4CAF50), Modifier.weight(1f))
                ConfidenceBadge("MED", medCount, Color(0xFFFFA726), Modifier.weight(1f))
                ConfidenceBadge("LOW", lowCount, Color(0xFFEF5350), Modifier.weight(1f))
                if (failCount > 0) {
                    ConfidenceBadge("FAIL", failCount, Color(0xFFD32F2F), Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            // Overlap summary
            val shoulders = run.peaks.count { it.overlapStatus == OverlapStatus.SHOULDER }
            val unresolved = run.peaks.count { it.overlapStatus == OverlapStatus.UNRESOLVED }
            val partial = run.peaks.count { it.overlapStatus == OverlapStatus.PARTIALLY_OVERLAPPED }

            if (shoulders + unresolved + partial > 0) {
                Text(
                    buildString {
                        append("Перекрытия: ")
                        val parts = mutableListOf<String>()
                        if (shoulders > 0) parts.add("$shoulders shoulder")
                        if (partial > 0) parts.add("$partial partial")
                        if (unresolved > 0) parts.add("$unresolved unresolved")
                        append(parts.joinToString(", "))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Warning summary
            if (run.warnings.isNotEmpty()) {
                val serious = run.warnings.count { it.severity == WarningSeverity.SERIOUS || it.severity == WarningSeverity.FAILED }
                val caution = run.warnings.count { it.severity == WarningSeverity.CAUTION }
                val info = run.warnings.count { it.severity == WarningSeverity.INFO }

                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    "Предупреждений: ${run.warnings.size}" +
                        if (serious > 0) " ($serious серьёзных)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (serious > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Section 6: Parameters ──────────────────────────────────────

@Composable
private fun ParametersSection(run: CalculationRun) {
    var expanded by remember { mutableStateOf(false) }
    val params = run.params

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Параметры алгоритма",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Свернуть" else "Развернуть",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Always-visible summary
            Text(
                "${params.presetName} · ${params.baselineMethod} · ${params.noiseMethod}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ParamRow("Smoothing", if (params.smoothingEnabled) "SG(${params.smoothingWindowSize},${params.smoothingPolynomialOrder})" else "Выкл.")
                    ParamRow("Baseline", "${params.baselineMethod} (λ=${formatSci(params.baselineLambda)}, p=${params.baselineP})")
                    ParamRow("Baseline iter.", "${params.baselineIterations}")
                    ParamRow("Integration", params.integrationMethod)
                    ParamRow("Noise method", params.noiseMethod)
                    ParamRow("Min S/N", "%.1f".format(params.minSnr))
                    ParamRow("Min peak height", "%.1f".format(params.minPeakHeight))
                    ParamRow("Min prominence", "%.1f".format(params.minPeakProminence))
                    ParamRow("Min distance", "${params.minPeakDistance} pts")
                    ParamRow("Min width", "${params.minPeakWidth} pts")
                    ParamRow("Smoothed integration", if (params.useSmoothedForIntegration) "Да" else "Нет")
                }
            }
        }
    }
}

@Composable
private fun ParamRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ─── Shared components ──────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun TableHeader(text: String, width: androidx.compose.ui.unit.Dp) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.width(width).padding(horizontal = 2.dp),
        textAlign = TextAlign.Start,
    )
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = color,
        modifier = Modifier.width(width).padding(horizontal = 2.dp),
        maxLines = 1,
    )
}

// ─── Formatters ─────────────────────────────────────────────────

private fun formatCompact(value: Double): String = when {
    value >= 1_000_000 -> "%.2fM".format(value / 1_000_000)
    value >= 1_000 -> "%.1fK".format(value / 1_000)
    value >= 1.0 -> "%.1f".format(value)
    else -> "%.3f".format(value)
}

private fun formatLargeNumber(value: Double): String = when {
    value >= 1_000_000 -> "%.2fM".format(value / 1_000_000)
    value >= 1_000 -> "%,.0f".format(value)
    else -> "%.1f".format(value)
}

private fun formatSci(value: Double): String {
    if (value == 0.0) return "0"
    val exp = log10(value).toInt()
    val divisor = 10.0.pow(exp.toDouble())
    val mantissa = value / divisor
    return if (mantissa == 1.0) "1e$exp" else "%.0fe%d".format(mantissa, exp)
}

private fun formatTimestamp(ts: Long): String {
    // Simple ISO-like format from epoch millis
    val totalSeconds = ts / 1000
    val seconds = (totalSeconds % 60).toInt()
    val minutes = ((totalSeconds / 60) % 60).toInt()
    val hours = ((totalSeconds / 3600) % 24).toInt()
    val days = (totalSeconds / 86400).toInt()
    // Approximate date from epoch days (1970-01-01 base)
    val year = 1970 + days / 365
    val dayOfYear = days % 365
    val month = (dayOfYear / 30 + 1).coerceAtMost(12)
    val day = (dayOfYear % 30 + 1).coerceAtMost(28)
    return "%04d-%02d-%02d %02d:%02d".format(year, month, day, hours, minutes)
}

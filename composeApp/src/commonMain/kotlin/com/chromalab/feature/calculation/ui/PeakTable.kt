package com.chromalab.feature.calculation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.feature.calculation.algorithm.*

/**
 * Peak table composable (§2.24).
 *
 * Scrollable table of all detected peaks.
 * Columns: status, RT, height, area, width, S/N, prominence, confidence, warnings.
 * Sortable by RT, filterable by status, tap row → chart jump.
 */

// ─── Data model ─────────────────────────────────────────────────

data class PeakTableRow(
    val index: Int,
    val rtApex: Double,
    val height: Double,
    val area: Double,
    val areaPercent: Double,
    val widthBase: Double,
    val snr: Double,
    val prominence: Double,
    val tailingFactor: Double,
    val confidenceGrade: ConfidenceGrade,
    val overlapStatus: OverlapStatus,
    val isManuallyEdited: Boolean,
    val warningCount: Int,
)

enum class PeakTableSort { RT_ASC, RT_DESC, AREA_DESC, SNR_DESC }
enum class PeakTableFilter { ALL, AUTO, MANUAL, LOW_CONFIDENCE }

// ─── Table Composable ───────────────────────────────────────────

@Composable
fun PeakTable(
    rows: List<PeakTableRow>,
    sort: PeakTableSort = PeakTableSort.RT_ASC,
    filter: PeakTableFilter = PeakTableFilter.ALL,
    onRowTap: ((Int) -> Unit)? = null,
    onSortChange: ((PeakTableSort) -> Unit)? = null,
    onFilterChange: ((PeakTableFilter) -> Unit)? = null,
    selectedIndex: Int = -1,
    modifier: Modifier = Modifier,
) {
    val filtered = remember(rows, filter) { applyFilter(rows, filter) }
    val sorted = remember(filtered, sort) { applySort(filtered, sort) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Toolbar: filter chips + sort
        TableToolbar(sort, filter, onSortChange, onFilterChange)

        Spacer(modifier = Modifier.height(Spacing.xs))

        // Header row
        val scrollState = rememberScrollState()

        Column(modifier = Modifier.horizontalScroll(scrollState)) {
            TableHeaderRow()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Data rows
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                itemsIndexed(sorted) { _, row ->
                    TableDataRow(
                        row = row,
                        isSelected = row.index == selectedIndex,
                        onClick = { onRowTap?.invoke(row.index) },
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    )
                }
            }
        }

        // Summary
        Text(
            "${sorted.size} из ${rows.size} пиков",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Spacing.xs),
        )
    }
}

// ─── Toolbar ────────────────────────────────────────────────────

@Composable
private fun TableToolbar(
    sort: PeakTableSort,
    filter: PeakTableFilter,
    onSortChange: ((PeakTableSort) -> Unit)?,
    onFilterChange: ((PeakTableFilter) -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Filter chips
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            PeakTableFilter.entries.forEach { f ->
                FilterChip(
                    selected = f == filter,
                    onClick = { onFilterChange?.invoke(f) },
                    label = {
                        Text(
                            when (f) {
                                PeakTableFilter.ALL -> "Все"
                                PeakTableFilter.AUTO -> "Авто"
                                PeakTableFilter.MANUAL -> "Ручные"
                                PeakTableFilter.LOW_CONFIDENCE -> "Низкая ув."
                            },
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )
            }
        }

        // Sort button
        var sortExpanded by remember { mutableStateOf(false) }
        Box {
            IconButton(onClick = { sortExpanded = true }) {
                Icon(Icons.Filled.Sort, contentDescription = "Сортировка", modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                listOf(
                    PeakTableSort.RT_ASC to "RT ↑",
                    PeakTableSort.RT_DESC to "RT ↓",
                    PeakTableSort.AREA_DESC to "Площадь ↓",
                    PeakTableSort.SNR_DESC to "S/N ↓",
                ).forEach { (s, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { onSortChange?.invoke(s); sortExpanded = false },
                        leadingIcon = {
                            if (s == sort) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                    )
                }
            }
        }
    }
}

// ─── Table rows ─────────────────────────────────────────────────

private val colWidths = listOf(44.dp, 70.dp, 72.dp, 80.dp, 56.dp, 60.dp, 52.dp, 52.dp, 64.dp, 28.dp)
private val colLabels = listOf("", "RT", "Высота", "Площадь", "Area%", "Ширина", "S/N", "Tail.", "Уверен.", "⚠")

@Composable
private fun TableHeaderRow() {
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        colLabels.forEachIndexed { i, label ->
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(colWidths[i]).padding(horizontal = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TableDataRow(
    row: PeakTableRow,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    } else Color.Transparent

    Row(
        modifier = Modifier
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status icon
        Box(modifier = Modifier.width(colWidths[0]).padding(horizontal = 4.dp)) {
            StatusDot(row)
        }
        // RT
        CellText("%.3f".format(row.rtApex), colWidths[1])
        // Height
        CellText(formatCompact(row.height), colWidths[2])
        // Area
        CellText(formatCompact(row.area), colWidths[3])
        // Area%
        CellText("%.1f".format(row.areaPercent), colWidths[4])
        // Width
        CellText("%.3f".format(row.widthBase), colWidths[5])
        // S/N
        CellText("%.1f".format(row.snr), colWidths[6])
        // Tailing
        CellText("%.2f".format(row.tailingFactor), colWidths[7])
        // Confidence
        Box(modifier = Modifier.width(colWidths[8]).padding(horizontal = 4.dp)) {
            ConfidenceDot(row.confidenceGrade)
        }
        // Warnings
        if (row.warningCount > 0) {
            Text(
                "${row.warningCount}",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFFA726),
                modifier = Modifier.width(colWidths[9]).padding(horizontal = 4.dp),
            )
        } else {
            Spacer(modifier = Modifier.width(colWidths[9]))
        }
    }
}

@Composable
private fun CellText(text: String, width: Dp) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.width(width).padding(horizontal = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StatusDot(row: PeakTableRow) {
    val color = when {
        row.isManuallyEdited -> MaterialTheme.colorScheme.tertiary
        row.overlapStatus == OverlapStatus.UNRESOLVED -> MaterialTheme.colorScheme.error
        row.overlapStatus == OverlapStatus.SHOULDER -> Color(0xFFFFA726)
        else -> MaterialTheme.colorScheme.primary
    }
    val label = when {
        row.isManuallyEdited -> "M"
        row.overlapStatus == OverlapStatus.UNRESOLVED -> "!"
        row.overlapStatus == OverlapStatus.SHOULDER -> "S"
        else -> "A"
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun ConfidenceDot(grade: ConfidenceGrade) {
    val (label, color) = when (grade) {
        ConfidenceGrade.HIGH -> "●" to Color(0xFF4CAF50)
        ConfidenceGrade.MEDIUM -> "●" to Color(0xFFFFA726)
        ConfidenceGrade.LOW -> "●" to Color(0xFFEF5350)
        ConfidenceGrade.FAILED -> "✕" to Color(0xFFD32F2F)
    }
    Text(label, color = color, style = MaterialTheme.typography.labelSmall)
}

// ─── CSV Export ──────────────────────────────────────────────────

fun exportPeakTableCsv(rows: List<PeakTableRow>): String {
    val header = "Peak,RT,Height,Area,Width,S/N,Prominence,Confidence,Overlap,Manual,Warnings"
    val lines = rows.map { row ->
        listOf(
            row.index + 1,
            "%.4f".format(row.rtApex),
            "%.2f".format(row.height),
            "%.2f".format(row.area),
            "%.4f".format(row.widthBase),
            "%.2f".format(row.snr),
            "%.2f".format(row.prominence),
            row.confidenceGrade.name,
            row.overlapStatus.name,
            if (row.isManuallyEdited) "yes" else "no",
            row.warningCount,
        ).joinToString(",")
    }
    return (listOf(header) + lines).joinToString("\n")
}

// ─── Utilities ──────────────────────────────────────────────────

private fun applyFilter(rows: List<PeakTableRow>, filter: PeakTableFilter): List<PeakTableRow> =
    when (filter) {
        PeakTableFilter.ALL -> rows
        PeakTableFilter.AUTO -> rows.filter { !it.isManuallyEdited }
        PeakTableFilter.MANUAL -> rows.filter { it.isManuallyEdited }
        PeakTableFilter.LOW_CONFIDENCE -> rows.filter {
            it.confidenceGrade == ConfidenceGrade.LOW || it.confidenceGrade == ConfidenceGrade.FAILED
        }
    }

private fun applySort(rows: List<PeakTableRow>, sort: PeakTableSort): List<PeakTableRow> =
    when (sort) {
        PeakTableSort.RT_ASC -> rows.sortedBy { it.rtApex }
        PeakTableSort.RT_DESC -> rows.sortedByDescending { it.rtApex }
        PeakTableSort.AREA_DESC -> rows.sortedByDescending { it.area }
        PeakTableSort.SNR_DESC -> rows.sortedByDescending { it.snr }
    }

private fun formatCompact(value: Double): String = when {
    kotlin.math.abs(value) >= 1_000_000 -> "%.1fM".format(value / 1_000_000)
    kotlin.math.abs(value) >= 1_000 -> "%.1fK".format(value / 1_000)
    kotlin.math.abs(value) >= 1 -> "%.1f".format(value)
    else -> "%.3f".format(value)
}

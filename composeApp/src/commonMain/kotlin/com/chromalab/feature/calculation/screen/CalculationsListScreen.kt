package com.chromalab.feature.calculation.screen

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chromalab.core.data.DatabaseProvider
import com.chromalab.core.data.entity.ChromatogramEntity
import com.chromalab.core.data.model.SourceType
import kotlinx.coroutines.launch

/**
 * Calculations tab — lists all saved chromatograms.
 *
 * Each card shows:
 * - Source icon (camera, gallery, CSV, etc.)
 * - Date and time of creation
 * - Ion channel (if available)
 * - Quality score indicator
 *
 * Actions:
 * - Tap «Анализировать» → navigates to AnalysisFlowScreen
 * - Tap «Удалить» → confirmation dialog → removes from Room
 *
 * Empty state: illustrated message with guidance.
 */
@Composable
fun CalculationsListScreen(
    onAnalyze: (signalId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dao = remember { DatabaseProvider.getDatabase().chromatogramDao() }
    val chromatograms by dao.getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var deleteConfirmId by remember { mutableStateOf<Long?>(null) }

    if (chromatograms.isEmpty()) {
        // ===== Empty State =====
        EmptyState()
    } else {
        // ===== List =====
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    "Хроматограммы",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    "${chromatograms.size} записей",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items(chromatograms, key = { it.id }) { item ->
                ChromatogramCard(
                    chromatogram = item,
                    onAnalyze = { onAnalyze(item.id.toString()) },
                    onDelete = { deleteConfirmId = item.id },
                )
            }
        }
    }

    // ===== Delete Confirmation Dialog =====
    deleteConfirmId?.let { id ->
        val item = chromatograms.find { it.id == id }
        AlertDialog(
            onDismissRequest = { deleteConfirmId = null },
            icon = {
                Icon(
                    Icons.Filled.DeleteForever,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("Удалить хроматограмму?") },
            text = {
                Text(
                    "Запись от ${formatDate(item?.createdAt ?: 0)} " +
                    "и все связанные расчёты будут удалены безвозвратно.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            dao.deleteById(id)
                        }
                        deleteConfirmId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmId = null }) {
                    Text("Отмена")
                }
            },
        )
    }
}

// ─── Chromatogram Card ──────────────────────────────────────────

/**
 * Card for a single chromatogram record in the list.
 * Shows source icon, date, quality score, and action buttons.
 */
@Composable
private fun ChromatogramCard(
    chromatogram: ChromatogramEntity,
    onAnalyze: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Source icon
                val (icon, iconColor) = sourceIcon(chromatogram.sourceType)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = chromatogram.sourceType.name,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        formatDate(chromatogram.createdAt),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            sourceLabel(chromatogram.sourceType),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (chromatogram.ionChannel != null) {
                            Text(
                                " · ${chromatogram.ionChannel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Quality badge
                chromatogram.qualityScore?.let { score ->
                    QualityBadge(score)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Удалить")
                }
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(onClick = onAnalyze) {
                    Icon(Icons.Filled.Science, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Анализировать")
                }
            }
        }
    }
}

// ─── Empty State ────────────────────────────────────────────────

/**
 * Full-screen empty state shown when no chromatograms are saved.
 * Guides the user to capture or import data.
 */
@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Science,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Нет хроматограмм",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Сфотографируйте хроматограмму или\nимпортируйте CSV-файл для начала работы.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 280.dp),
            )
        }
    }
}

// ─── Quality Badge ──────────────────────────────────────────────

/**
 * Compact quality indicator: colored dot + percentage label.
 * Green ≥ 0.7, amber ≥ 0.4, red < 0.4.
 */
@Composable
private fun QualityBadge(score: Float) {
    val color = when {
        score >= 0.7f -> Color(0xFF81C784) // Success green
        score >= 0.4f -> Color(0xFFFFD54F) // Warning amber
        else -> Color(0xFFE57373)          // Error red
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "${(score * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = color,
            )
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────

/** Map source type to (icon, color) pair. */
private fun sourceIcon(type: SourceType) = when (type) {
    SourceType.PHOTO -> Icons.Filled.CameraAlt to Color(0xFF8AB4F8)
    SourceType.GALLERY -> Icons.Filled.PhotoLibrary to Color(0xFFCE93D8)
    SourceType.VALIDATION_FIXTURE -> Icons.Filled.BugReport to Color(0xFFFFD54F)
    SourceType.CSV -> Icons.Filled.TableChart to Color(0xFF80CBC4)
    SourceType.PDF -> Icons.Filled.PictureAsPdf to Color(0xFFE57373)
    SourceType.MZML -> Icons.Filled.Biotech to Color(0xFFFFCC80)
    SourceType.MANUAL -> Icons.Filled.Edit to Color(0xFFA3ACB9)
}

/** Human-readable label for source type. */
private fun sourceLabel(type: SourceType) = when (type) {
    SourceType.PHOTO -> "Фото"
    SourceType.GALLERY -> "Галерея"
    SourceType.VALIDATION_FIXTURE -> "Validation fixture"
    SourceType.CSV -> "CSV"
    SourceType.PDF -> "PDF"
    SourceType.MZML -> "mzML"
    SourceType.MANUAL -> "Вручную"
}

/**
 * Format epoch millis to a human-readable date string.
 * Uses platform-agnostic calculation (no external dependency).
 * Format: "dd.MM.yyyy HH:mm"
 */
private fun formatDate(epochMillis: Long): String {
    if (epochMillis == 0L) return "—"
    // Simple epoch → date conversion for display purposes.
    // Accuracy: ±1 day for timezone offset, acceptable for list display.
    val totalSeconds = epochMillis / 1000
    val totalMinutes = totalSeconds / 60
    val totalHours = totalMinutes / 60
    val totalDays = totalHours / 24

    val hour = ((totalHours % 24) + 24) % 24
    val minute = ((totalMinutes % 60) + 60) % 60

    // Days since epoch → year/month/day (simplified Gregorian)
    var days = totalDays.toInt()
    var year = 1970
    while (true) {
        val daysInYear = if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 366 else 365
        if (days < daysInYear) break
        days -= daysInYear
        year++
    }
    val isLeap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    val monthDays = intArrayOf(31, if (isLeap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var month = 0
    while (month < 12 && days >= monthDays[month]) {
        days -= monthDays[month]
        month++
    }
    val day = days + 1
    month += 1

    return "%02d.%02d.%04d %02d:%02d".format(day, month, year, hour, minute)
}

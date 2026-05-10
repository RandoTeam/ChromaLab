package com.chromalab.feature.capture

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chromalab.core.data.DatabaseProvider
import com.chromalab.core.data.entity.ChromatogramEntity
import com.chromalab.core.data.model.SourceType
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.signal.GraphPoint
import com.chromalab.feature.processing.signal.SignalMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * File import screen — parses CSV/JSON files containing chromatogram data.
 *
 * Workflow:
 * 1. User taps "Выбрать файл" → SAF file picker (callback from parent)
 * 2. File content is parsed: 2 columns (Time, Intensity) → DigitalSignal
 * 3. Preview chart is displayed with signal statistics
 * 4. User taps "Сохранить" → Room insert → navigate to Analysis
 *
 * Supported formats:
 * - CSV/TXT: auto-detect delimiter (comma, tab, semicolon)
 * - JSON: array of {time, intensity} objects
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileImportScreen(
    onPickFile: () -> Unit,
    fileContent: String?,
    fileName: String?,
    onSaved: (signalId: Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var parsedSignal by remember { mutableStateOf<DigitalSignal?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var pointCount by remember { mutableStateOf(0) }

    // Parse file content when it changes
    LaunchedEffect(fileContent) {
        if (fileContent == null) {
            parsedSignal = null
            parseError = null
            return@LaunchedEffect
        }
        try {
            val signal = withContext(Dispatchers.Default) {
                parseChromatogramFile(fileContent, fileName ?: "import")
            }
            parsedSignal = signal
            pointCount = signal.points.size
            parseError = null
            println("IMPORT[PARSE] OK: ${signal.points.size} points, " +
                    "time=${signal.points.firstOrNull()?.time}..${signal.points.lastOrNull()?.time}")
        } catch (e: Exception) {
            parseError = e.message ?: "Ошибка парсинга"
            parsedSignal = null
            println("IMPORT[PARSE] Error: ${e.message}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Импорт файла") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (parsedSignal == null) {
                // ===== No file selected — show picker prompt =====
                FilePickerPrompt(
                    fileName = fileName,
                    parseError = parseError,
                    onPickFile = onPickFile,
                )
            } else {
                // ===== File parsed — show preview =====
                val signal = parsedSignal!!

                // File info card
                FileInfoCard(
                    fileName = fileName ?: "файл",
                    pointCount = signal.points.size,
                    timeRange = signal.timeRange,
                    timeUnit = signal.timeUnit,
                    maxIntensity = signal.maxIntensity,
                )

                // Signal preview chart
                SignalPreviewChart(
                    signal = signal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )

                Spacer(Modifier.weight(1f))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onPickFile,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.FolderOpen, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Другой файл")
                    }
                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                val id = saveSignalToRoom(signal, fileName)
                                isSaving = false
                                onSaved(id)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving,
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Filled.Save, null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}

// ─── File Picker Prompt ─────────────────────────────────────────

/**
 * Empty state prompting the user to select a CSV or JSON file.
 */
@Composable
private fun FilePickerPrompt(
    fileName: String?,
    parseError: String?,
    onPickFile: () -> Unit,
) {
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
                                Color(0xFF80CBC4),
                                Color(0xFF4DB6AC),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.UploadFile,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Импорт хроматограммы",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Поддерживаются форматы CSV и JSON.\nФайл должен содержать колонки Time и Intensity.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))

            // Error message
            if (parseError != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.ErrorOutline,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            parseError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            FilledTonalButton(onClick = onPickFile) {
                Icon(Icons.Filled.FolderOpen, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Выбрать файл")
            }
        }
    }
}

// ─── File Info Card ─────────────────────────────────────────────

/**
 * Compact card showing parsed file statistics.
 */
@Composable
private fun FileInfoCard(
    fileName: String,
    pointCount: Int,
    timeRange: Float,
    timeUnit: String,
    maxIntensity: Float,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Description,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                StatItem("Точки", "$pointCount")
                StatItem("Диапазон", "%.1f $timeUnit".format(timeRange))
                StatItem("Макс", "%.0f".format(maxIntensity))
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Signal Preview Chart ───────────────────────────────────────

/**
 * Minimal line chart previewing the imported signal.
 * Draws the intensity curve over a subtle grid background.
 */
@Composable
private fun SignalPreviewChart(
    signal: DigitalSignal,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val points = signal.points
            if (points.size < 2) return@Canvas

            val minT = points.first().time
            val maxT = points.last().time
            val rangeT = (maxT - minT).coerceAtLeast(1f)
            val minI = signal.minIntensity
            val maxI = signal.maxIntensity
            val rangeI = (maxI - minI).coerceAtLeast(1f)

            val w = size.width
            val h = size.height

            // Grid lines (4 horizontal)
            for (i in 0..4) {
                val y = h * i / 4f
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            }

            // Signal path
            val path = Path()
            points.forEachIndexed { idx, pt ->
                val x = ((pt.time - minT) / rangeT) * w
                val y = h - ((pt.intensity - minI) / rangeI) * h
                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, lineColor, style = Stroke(width = 2f))
        }
    }
}

// ─── CSV/JSON Parser ────────────────────────────────────────────

/**
 * Parse chromatogram data from CSV or JSON content.
 *
 * CSV format: auto-detects delimiter (comma, tab, semicolon).
 * Expects at least 2 numeric columns (time, intensity).
 * First row may be a header — detected and skipped if non-numeric.
 *
 * JSON format: array of objects with "time" and "intensity" keys.
 *
 * @throws IllegalArgumentException if the file cannot be parsed.
 */
internal fun parseChromatogramFile(content: String, fileName: String): DigitalSignal {
    val trimmed = content.trim()

    // Detect JSON
    if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
        return parseJson(trimmed, fileName)
    }

    // CSV/TXT
    return parseCsv(trimmed, fileName)
}

/**
 * Parse CSV content with auto-delimiter detection.
 * Supports: comma, tab, semicolon.
 */
private fun parseCsv(content: String, fileName: String): DigitalSignal {
    val lines = content.lines().filter { it.isNotBlank() }
    if (lines.size < 2) {
        throw IllegalArgumentException("Файл слишком короткий (${lines.size} строк)")
    }

    // Detect delimiter by counting candidates in the first data line
    val delimiters = listOf(',', '\t', ';')
    val delimiter = delimiters.maxByOrNull { d ->
        lines.take(3).sumOf { line -> line.count { it == d } }
    } ?: ','

    // Check if the first line is a header (non-numeric)
    val firstLineFields = lines[0].split(delimiter).map { it.trim() }
    val isHeader = firstLineFields.any { field ->
        field.toDoubleOrNull() == null && field.isNotEmpty()
    }

    val dataLines = if (isHeader) lines.drop(1) else lines
    val points = mutableListOf<GraphPoint>()

    for ((idx, line) in dataLines.withIndex()) {
        val fields = line.split(delimiter).map { it.trim() }
        if (fields.size < 2) continue

        val time = fields[0].toFloatOrNull() ?: continue
        val intensity = fields[1].toFloatOrNull() ?: continue

        points.add(
            GraphPoint(
                index = idx,
                pixelX = idx,
                pixelY = intensity,
                time = time,
                intensity = intensity,
                confidence = 1.0f,
                isInterpolated = false,
            )
        )
    }

    if (points.isEmpty()) {
        throw IllegalArgumentException(
            "Не удалось извлечь данные. Проверьте формат: " +
            "2 колонки (Time, Intensity), разделитель: '${delimiter}'"
        )
    }

    // Sort by time
    val sorted = points.sortedBy { it.time }

    return DigitalSignal(
        points = sorted,
        timeUnit = "мин",
        intensityUnit = "mAU",
        metadata = SignalMetadata(
            sourceImage = fileName,
            totalPoints = sorted.size,
            duplicatesRemoved = 0,
            gapCount = 0,
            sortValid = true,
            timestamp = System.currentTimeMillis(),
        ),
    )
}

/**
 * Parse JSON array of {time, intensity} objects.
 * Supports: [{"time":1.0,"intensity":100.0}, ...]
 */
private fun parseJson(content: String, fileName: String): DigitalSignal {
    // Simple JSON parsing without kotlinx.serialization dependency
    // Extract numeric values from {"time":X, "intensity":Y} patterns
    val timeRegex = Regex(""""time"\s*:\s*(-?[\d.]+)""")
    val intensityRegex = Regex(""""intensity"\s*:\s*(-?[\d.]+)""")

    val times = timeRegex.findAll(content).map { it.groupValues[1].toFloat() }.toList()
    val intensities = intensityRegex.findAll(content).map { it.groupValues[1].toFloat() }.toList()

    val count = minOf(times.size, intensities.size)
    if (count == 0) {
        throw IllegalArgumentException(
            "JSON: не найдены поля \"time\" и \"intensity\""
        )
    }

    val points = (0 until count).map { idx ->
        GraphPoint(
            index = idx,
            pixelX = idx,
            pixelY = intensities[idx],
            time = times[idx],
            intensity = intensities[idx],
            confidence = 1.0f,
            isInterpolated = false,
        )
    }.sortedBy { it.time }

    return DigitalSignal(
        points = points,
        timeUnit = "мин",
        intensityUnit = "mAU",
        metadata = SignalMetadata(
            sourceImage = fileName,
            totalPoints = points.size,
            duplicatesRemoved = 0,
            gapCount = 0,
            sortValid = true,
            timestamp = System.currentTimeMillis(),
        ),
    )
}

// ─── Room Save ──────────────────────────────────────────────────

/**
 * Save a parsed DigitalSignal to Room as a ChromatogramEntity.
 * Returns the new record ID.
 */
private suspend fun saveSignalToRoom(
    signal: DigitalSignal,
    fileName: String?,
): Long = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    val dao = DatabaseProvider.getDatabase().chromatogramDao()
    val sourceType = when {
        fileName?.endsWith(".csv", ignoreCase = true) == true -> SourceType.CSV
        fileName?.endsWith(".json", ignoreCase = true) == true -> SourceType.CSV // treat as data import
        else -> SourceType.MANUAL
    }
    val entity = ChromatogramEntity(
        sampleId = 0,
        sourceType = sourceType,
        filePath = fileName,
        timeRangeStart = signal.points.firstOrNull()?.time?.toDouble(),
        timeRangeEnd = signal.points.lastOrNull()?.time?.toDouble(),
        intensityUnit = signal.intensityUnit,
        qualityScore = signal.highConfidenceRatio,
        dataPoints = kotlinx.serialization.json.Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(
                GraphPoint.serializer(),
            ),
            signal.points,
        ),
        createdAt = now,
        updatedAt = now,
    )
    val id = dao.insert(entity)
    println("IMPORT[SAVE] Saved to Room: id=$id, points=${signal.points.size}, source=$sourceType")
    id
}

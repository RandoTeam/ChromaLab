package com.chromalab.feature.calculation.flow

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.core.data.DatabaseProvider
import com.chromalab.core.data.entity.ChromatogramEntity
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.signal.GraphPoint
import com.chromalab.feature.processing.signal.SignalMetadata
import com.chromalab.feature.calculation.core.CalculationEngine
import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.calculation.core.CalculationParams
import com.chromalab.feature.calculation.core.PeakResult
import com.chromalab.feature.calculation.algorithm.*
import com.chromalab.feature.calculation.ui.ChromatogramChart
import com.chromalab.feature.calculation.ui.PeakTable
import com.chromalab.feature.calculation.ui.PeakTableRow
import com.chromalab.feature.calculation.ui.PeakTableSort
import com.chromalab.feature.calculation.ui.PeakTableFilter
import com.chromalab.feature.calculation.ui.PeakDetailsContent
import com.chromalab.feature.calculation.ui.PeakDetailsData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

/**
 * Analysis flow orchestrator — Phase 2 user scenario.
 *
 * Scenario:
 * 1. Open signal → see chart
 * 2. Toggle layers (raw / smoothed / baseline / corrected / peaks)
 * 3. Press "Найти пики" → peaks appear
 * 4. Tap peak → PeakDetails card (RT, height, area, width, S/N)
 * 5. Drag boundaries / add / reject peaks
 * 6. Choose noise region, change baseline
 * 7. Metrics recalculate after every edit
 * 8. Save CalculationRun → Export CSV/JSON
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisFlowScreen(
    signalId: String,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableStateOf(AnalysisStep.FIRST) }
    var peaksFound by remember { mutableStateOf(false) }

    // Signal loaded from Room
    var signal by remember { mutableStateOf<DigitalSignal?>(null) }
    var entity by remember { mutableStateOf<ChromatogramEntity?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    // Phase 2 calculation result
    var calculationRun by remember { mutableStateOf<CalculationRun?>(null) }
    var isCalculating by remember { mutableStateOf(false) }
    val calcScope = rememberCoroutineScope()

    // Peak selection state
    var selectedPeakIndex by remember { mutableStateOf(-1) }
    var showPeakSheet by remember { mutableStateOf(false) }

    // Load signal from Room on first composition
    LaunchedEffect(signalId) {
        val id = signalId.toLongOrNull()
        if (id == null) {
            loadError = "Некорректный ID сигнала"
            return@LaunchedEffect
        }
        try {
            val loaded = withContext(Dispatchers.IO) {
                DatabaseProvider.getDatabase().chromatogramDao().getById(id)
            }
            if (loaded == null) {
                loadError = "Сигнал ID=$id не найден в базе"
                return@LaunchedEffect
            }
            entity = loaded
            // Deserialize dataPoints JSON → List<GraphPoint>
            val json = loaded.dataPoints
            if (json != null) {
                val points = Json.decodeFromString(
                    ListSerializer(GraphPoint.serializer()), json,
                )
                signal = DigitalSignal(
                    points = points,
                    timeUnit = "мин",
                    intensityUnit = loaded.intensityUnit ?: "mAU",
                    metadata = SignalMetadata(
                        sourceImage = loaded.filePath ?: "",
                        totalPoints = points.size,
                        duplicatesRemoved = 0,
                        gapCount = 0,
                        sortValid = true,
                        timestamp = loaded.createdAt,
                    ),
                )
            } else {
                loadError = "Нет данных в записи ID=$id"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            loadError = "Ошибка загрузки: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            Column {
                LinearProgressIndicator(
                    progress = { (currentStep.index + 1).toFloat() / currentStep.totalSteps },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Шаг ${currentStep.index + 1} / ${currentStep.totalSteps}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        currentStep.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(
                        onClick = {
                            val prev = currentStep.prev()
                            if (prev != null) currentStep = prev else onCancel()
                        },
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("Назад")
                    }

                    // "Find peaks" button on PEAK_DETECTION step
                    if (currentStep == AnalysisStep.PEAK_DETECTION && !peaksFound) {
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                val sig = signal ?: return@Button
                                isCalculating = true
                                calcScope.launch {
                                    try {
                                        val run = withContext(Dispatchers.Default) {
                                            CalculationEngine.execute(
                                                signal = sig,
                                                sourceId = signalId,
                                                params = CalculationParams(),
                                            )
                                        }
                                        calculationRun = run
                                        peaksFound = true
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isCalculating = false
                                    }
                                }
                            },
                            enabled = signal != null && !isCalculating,
                            modifier = Modifier.height(48.dp),
                        ) {
                            if (isCalculating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Анализ...")
                            } else {
                                Icon(Icons.Filled.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Найти пики")
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = {
                                val next = currentStep.next()
                                if (next != null) currentStep = next else onFinish()
                            },
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text(if (currentStep.next() != null) "Далее" else "Завершить")
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState.index > initialState.index) {
                        slideInHorizontally { it / 3 } + fadeIn() togetherWith
                            slideOutHorizontally { -it / 3 } + fadeOut()
                    } else {
                        slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                            slideOutHorizontally { it / 3 } + fadeOut()
                    }
                },
                label = "analysis_step",
            ) { step ->
                AnalysisStepContent(
                    step = step,
                    signalId = signalId,
                    signal = signal,
                    loadError = loadError,
                    calculationRun = calculationRun,
                    peaksFound = peaksFound,
                    selectedPeakIndex = selectedPeakIndex,
                    onPeakSelected = { idx ->
                        selectedPeakIndex = idx
                        showPeakSheet = true
                    },
                )
            }
        }
    }

    // Peak details bottom sheet
    if (showPeakSheet && calculationRun != null && selectedPeakIndex in calculationRun!!.peaks.indices) {
        ModalBottomSheet(
            onDismissRequest = { showPeakSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            val peakData = remember(selectedPeakIndex, calculationRun) {
                buildPeakDetailsData(calculationRun!!.peaks[selectedPeakIndex])
            }
            PeakDetailsContent(
                data = peakData,
                onRejectPeak = {
                    showPeakSheet = false
                },
            )
        }
    }
}

@Composable
private fun AnalysisStepContent(
    step: AnalysisStep,
    signalId: String,
    signal: DigitalSignal?,
    loadError: String?,
    calculationRun: CalculationRun?,
    peaksFound: Boolean,
    selectedPeakIndex: Int = -1,
    onPeakSelected: ((Int) -> Unit)? = null,
) {
    // Layer visibility state — persisted across steps
    var visibleLayers by remember {
        mutableStateOf(setOf("raw", "corrected"))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        // Step title
        Text(
            step.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        when {
            // Error state
            loadError != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        loadError,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Loading state
            signal == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text("Загрузка сигнала...")
                    }
                }
            }

            // Signal loaded — show chart
            else -> {
                // Build chart state based on current step
                val chartState = remember(calculationRun, visibleLayers, peaksFound, selectedPeakIndex) {
                    if (calculationRun != null) {
                        CalculationToChartMapper.buildChartState(
                            run = calculationRun,
                            visibleLayers = visibleLayers,
                            selectedPeakIndex = selectedPeakIndex,
                        )
                    } else {
                        CalculationToChartMapper.buildRawChartState(signal)
                    }
                }

                // Chart — flexible height, shrinks when table visible
                val chartWeight = if (
                    step == AnalysisStep.PEAK_REVIEW ||
                    step == AnalysisStep.RESULTS
                ) 0.5f else 1f

                ChromatogramChart(
                    state = chartState,
                    onPeakTap = onPeakSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(chartWeight),
                )

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Step-specific bottom content
                when (step) {
                    AnalysisStep.SIGNAL_OVERVIEW -> {
                        // Signal summary card
                        SignalSummaryCard(signal)
                    }

                    AnalysisStep.LAYER_SELECTION -> {
                        // Layer toggle chips
                        LayerToggleChips(
                            hasSmoothed = calculationRun?.signals?.smoothed != null,
                            hasBaseline = calculationRun?.signals?.baseline != null,
                            hasCorrected = calculationRun?.signals?.baselineCorrected != null,
                            visibleLayers = visibleLayers,
                            onToggle = { layerId ->
                                visibleLayers = if (layerId in visibleLayers) {
                                    visibleLayers - layerId
                                } else {
                                    visibleLayers + layerId
                                }
                            },
                        )
                    }

                    AnalysisStep.PEAK_DETECTION -> {
                        if (peaksFound && calculationRun != null) {
                            Text(
                                "Найдено пиков: ${calculationRun.peaks.size}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Text(
                                "Нажмите «Найти пики» для запуска анализа",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    AnalysisStep.NOISE_BASELINE -> {
                        Text(
                            "Выберите noise region, смените baseline method",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    else -> {
                        // PEAK_REVIEW, RESULTS — show PeakTable
                        if (calculationRun != null && calculationRun.peaks.isNotEmpty()) {
                            val tableRows = remember(calculationRun) {
                                buildPeakTableRows(calculationRun.peaks)
                            }
                            var tableSort by remember { mutableStateOf(PeakTableSort.RT_ASC) }
                            var tableFilter by remember { mutableStateOf(PeakTableFilter.ALL) }

                            PeakTable(
                                rows = tableRows,
                                sort = tableSort,
                                filter = tableFilter,
                                selectedIndex = selectedPeakIndex,
                                onRowTap = { onPeakSelected?.invoke(it) },
                                onSortChange = { tableSort = it },
                                onFilterChange = { tableFilter = it },
                                modifier = Modifier.weight(0.5f),
                            )
                        }
                    }
                }

                // Signal ID footer
                Text(
                    "Signal ID: ${signalId.toLongOrNull() ?: "\u2014"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
        }
    }
}

// \u2500\u2500\u2500 PeakResult \u2192 PeakTableRow mapper \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500

private fun buildPeakTableRows(peaks: List<PeakResult>): List<PeakTableRow> {
    val totalArea = peaks.sumOf { it.area }
    return peaks.mapIndexed { i, p ->
        PeakTableRow(
            index = i,
            rtApex = p.rtApex,
            height = p.height,
            area = p.area,
            areaPercent = if (totalArea > 0) p.area / totalArea * 100.0 else 0.0,
            widthBase = p.widthBase,
            snr = p.snr,
            prominence = p.height, // use height as proxy when prominence not in PeakResult
            tailingFactor = p.tailingFactor,
            confidenceGrade = p.confidence,
            overlapStatus = p.overlapStatus,
            isManuallyEdited = p.status == com.chromalab.feature.calculation.core.PeakStatus.MANUAL ||
                p.status == com.chromalab.feature.calculation.core.PeakStatus.CORRECTED,
            warningCount = p.warnings.size,
        )
    }
}

// \u2500\u2500\u2500 PeakResult \u2192 PeakDetailsData mapper \u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500

private fun buildPeakDetailsData(peak: PeakResult): PeakDetailsData {
    return PeakDetailsData(
        peakId = peak.peakId,
        metrics = PeakMetrics(
            rtApex = peak.rtApex,
            rtCentroid = peak.rtCentroid ?: peak.rtApex,
            height = peak.height,
            area = peak.area,
            widthBase = peak.widthBase,
            widthHalfHeight = peak.widthHalfHeight ?: 0.0,
            widthHalfProminence = 0.0,
            prominence = peak.height,
            leftBaseTime = peak.leftBoundaryTime,
            rightBaseTime = peak.rightBoundaryTime,
            snrValue = peak.snr,
            snrFlag = if (peak.snr >= 10) SnrFlag.QUANTITATION
                else if (peak.snr >= 3) SnrFlag.DETECTABLE
                else SnrFlag.LOW,
            overlapStatus = peak.overlapStatus,
            boundaryMethod = BoundaryMethod.entries.find { it.name == peak.boundaryMethod }
                ?: BoundaryMethod.LOCAL_MINIMA,
            boundaryConfidence = 0.8,
            isManuallyEdited = peak.status == com.chromalab.feature.calculation.core.PeakStatus.MANUAL,
            tailingFactor = peak.tailingFactor,
            asymmetryFactor = peak.asymmetryFactor,
            plateCount = peak.plateCount,
            warnings = peak.warnings,
        ),
        confidence = PeakConfidence(
            grade = peak.confidence,
            score = when (peak.confidence) {
                ConfidenceGrade.HIGH -> 0.9
                ConfidenceGrade.MEDIUM -> 0.6
                ConfidenceGrade.LOW -> 0.3
                ConfidenceGrade.FAILED -> 0.0
            },
            factors = emptyList(),
            reasons = emptyList(),
        ),
        integration = IntegrationResult(
            totalArea = peak.area,
            positiveArea = peak.area,
            negativeArea = 0.0,
            method = IntegrationMethod.entries.find { it.name == peak.integrationMethod }
                ?: IntegrationMethod.TRAPEZOIDAL,
            startTime = peak.leftBoundaryTime,
            endTime = peak.rightBoundaryTime,
            pointCount = 0,
            clampedNegative = false,
        ),
        baselineMethod = peak.baselineMethod,
    )
}

// ─── Signal Summary Card ────────────────────────────────────────

@Composable
private fun SignalSummaryCard(signal: DigitalSignal) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            SummaryRow("Точек", "${signal.points.size}")
            SummaryRow(
                "Время",
                "${"%.2f".format(signal.points.firstOrNull()?.time ?: 0f)} — " +
                    "${"%.2f".format(signal.points.lastOrNull()?.time ?: 0f)} ${signal.timeUnit}",
            )
            SummaryRow(
                "Интенсивность",
                "${"%.1f".format(signal.minIntensity)} — " +
                    "${"%.1f".format(signal.maxIntensity)} ${signal.intensityUnit}",
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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

// ─── Layer Toggle Chips ─────────────────────────────────────────

@Composable
private fun LayerToggleChips(
    hasSmoothed: Boolean,
    hasBaseline: Boolean,
    hasCorrected: Boolean,
    visibleLayers: Set<String>,
    onToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            "Слои сигнала",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.fillMaxWidth(),
        ) {
            LayerChip("Raw", "raw", Color(0xFF42A5F5), visibleLayers, onToggle)
            if (hasSmoothed) {
                LayerChip("Smoothed", "smoothed", Color(0xFF80DEEA), visibleLayers, onToggle)
            }
            if (hasBaseline) {
                LayerChip("Baseline", "baseline", Color(0xFF9E9E9E), visibleLayers, onToggle)
            }
            if (hasCorrected) {
                LayerChip("Corrected", "corrected", Color(0xFF66BB6A), visibleLayers, onToggle)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayerChip(
    label: String,
    layerId: String,
    color: Color,
    visibleLayers: Set<String>,
    onToggle: (String) -> Unit,
) {
    FilterChip(
        selected = layerId in visibleLayers,
        onClick = { onToggle(layerId) },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (layerId in visibleLayers) color else color.copy(alpha = 0.3f),
                        shape = androidx.compose.foundation.shape.CircleShape,
                    ),
            )
        },
    )
}

private fun stepDescription(step: AnalysisStep, peaksFound: Boolean): String = when (step) {
    AnalysisStep.SIGNAL_OVERVIEW -> "Загрузка и валидация оцифрованного сигнала"
    AnalysisStep.LAYER_SELECTION -> "Переключатели: raw / smoothed / baseline / corrected / peaks"
    AnalysisStep.PEAK_DETECTION ->
        if (peaksFound) "Пики найдены — нажмите Далее для просмотра"
        else "Нажмите «Найти пики» для запуска анализа"
    AnalysisStep.PEAK_REVIEW -> "Нажмите на пик для просмотра RT, height, area, width, S/N"
    AnalysisStep.PEAK_CORRECTION -> "Двигайте границы, добавляйте/отклоняйте пики"
    AnalysisStep.NOISE_BASELINE -> "Выберите noise region, смените baseline method"
    AnalysisStep.RESULTS -> "Таблица пиков — метрики пересчитаны"
    AnalysisStep.EXPORT -> "Сохранение CalculationRun и экспорт CSV/JSON"
}


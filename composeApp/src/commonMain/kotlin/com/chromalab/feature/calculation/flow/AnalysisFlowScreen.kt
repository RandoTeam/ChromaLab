package com.chromalab.feature.calculation.flow

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.core.data.DatabaseProvider
import com.chromalab.core.data.entity.ChromatogramEntity
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.signal.GraphPoint
import com.chromalab.feature.processing.signal.SignalMetadata
import kotlinx.coroutines.Dispatchers
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
                            onClick = { peaksFound = true },
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Найти пики")
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
                AnalysisStepContent(step, signalId, signal, loadError, peaksFound)
            }
        }
    }
}

@Composable
private fun AnalysisStepContent(
    step: AnalysisStep,
    signalId: String,
    signal: DigitalSignal?,
    loadError: String?,
    peaksFound: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            step.label,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        // Show signal summary on first step, placeholder on others
        if (step == AnalysisStep.SIGNAL_OVERVIEW) {
            if (loadError != null) {
                Text(
                    loadError,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (signal != null) {
                Text(
                    "Точек: ${signal.points.size}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Время: ${"%.2f".format(signal.points.firstOrNull()?.time ?: 0f)} — " +
                        "${"%.2f".format(signal.points.lastOrNull()?.time ?: 0f)} ${signal.timeUnit}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Интенсивность: ${"%.1f".format(signal.minIntensity)} — " +
                        "${"%.1f".format(signal.maxIntensity)} ${signal.intensityUnit}",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text("Загрузка сигнала...")
            }
        } else {
            Text(
                stepDescription(step, peaksFound),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            "Signal ID: ${signalId.toLongOrNull() ?: "—"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
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

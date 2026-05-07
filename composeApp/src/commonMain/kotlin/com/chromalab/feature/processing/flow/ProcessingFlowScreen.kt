package com.chromalab.feature.processing.flow

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.chromalab.core.ui.theme.Spacing

/**
 * Processing flow orchestrator.
 *
 * Manages the step-by-step pipeline with consistent UX:
 * - Always show current step (progress header)
 * - Always allow going back
 * - One main action per step
 * - Animated transitions between steps
 */
@Composable
fun ProcessingFlowScreen(
    imagePath: String,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableStateOf(ProcessingStep.FIRST) }

    Scaffold(
        topBar = { StepProgressHeader(currentStep) },
        bottomBar = {
            StepBottomBar(
                onAccept = {
                    val next = currentStep.next()
                    if (next != null) {
                        currentStep = next
                    } else {
                        onFinish()
                    }
                },
                onBack = {
                    val prev = currentStep.prev()
                    if (prev != null) {
                        currentStep = prev
                    } else {
                        onCancel()
                    }
                },
                acceptLabel = if (currentStep.next() != null) "Принять" else "Завершить",
                acceptEnabled = true,
                showCorrect = currentStep in listOf(
                    ProcessingStep.CROP_REVIEW,
                    ProcessingStep.PERSPECTIVE,
                    ProcessingStep.GRAPH_ROI,
                    ProcessingStep.AXIS_DETECTION,
                    ProcessingStep.CURVE_EDITOR,
                ),
                onCorrect = {
                    // Placeholder — will open correction mode per step
                },
            )
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
                label = "step_transition",
            ) { step ->
                StepContent(step, imagePath)
            }
        }
    }
}

/**
 * Renders content for a given processing step.
 * Each step displays its name, description, and relevant controls.
 */
@Composable
private fun StepContent(
    step: ProcessingStep,
    imagePath: String,
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

        Text(
            stepDescription(step),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            imagePath.substringAfterLast('/').substringAfterLast('\\'),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
        )

        // Zoom hint for interactive steps
        if (step in listOf(
                ProcessingStep.CROP_REVIEW,
                ProcessingStep.PERSPECTIVE,
                ProcessingStep.GRAPH_ROI,
                ProcessingStep.AXIS_DETECTION,
                ProcessingStep.CURVE_EDITOR,
            )
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))
            ZoomPanHint()
        }

        // Expandable params for processing steps
        if (step in listOf(
                ProcessingStep.X_CALIBRATION,
                ProcessingStep.Y_CALIBRATION,
                ProcessingStep.CURVE_EXTRACTION,
                ProcessingStep.SIGNAL_PREVIEW,
            )
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))
            ExpandableParamsSection {
                Text(
                    "Параметры будут отображены при подключении обработки",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun stepDescription(step: ProcessingStep): String = when (step) {
    ProcessingStep.IMAGE_QUALITY -> "Проверка резкости, контраста и освещения"
    ProcessingStep.CROP_REVIEW -> "Автоматическая обрезка по границам документа"
    ProcessingStep.PERSPECTIVE -> "Коррекция перспективных искажений"
    ProcessingStep.GRAPH_SELECTION -> "Выбор области с хроматограммой"
    ProcessingStep.GRAPH_ROI -> "Точная настройка области графика"
    ProcessingStep.AXIS_DETECTION -> "Определение осей X и Y"
    ProcessingStep.X_CALIBRATION -> "Привязка шкалы времени к пикселям"
    ProcessingStep.Y_CALIBRATION -> "Привязка шкалы интенсивности к пикселям"
    ProcessingStep.OCR_SUGGESTION -> "Распознавание числовых подписей осей"
    ProcessingStep.CURVE_EXTRACTION -> "Извлечение кривой из изображения"
    ProcessingStep.CURVE_EDITOR -> "Проверка и ручная коррекция кривой"
    ProcessingStep.SIGNAL_PREVIEW -> "Предпросмотр цифрового сигнала"
    ProcessingStep.QUALITY_REPORT -> "Оценка качества оцифровки"
    ProcessingStep.EXPORT -> "Экспорт данных в CSV / JSON"
}

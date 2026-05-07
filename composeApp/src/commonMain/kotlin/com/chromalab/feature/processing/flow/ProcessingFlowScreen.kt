package com.chromalab.feature.processing.flow

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing

/**
 * Processing flow orchestrator.
 *
 * Manages the step-by-step pipeline:
 * photo → quality → crop → perspective → graph → axes → calibration →
 * OCR → curve → correction → signal → quality report → export
 *
 * Each step renders its own screen and calls onNext/onBack.
 * The orchestrator tracks current step and shows a progress indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingFlowScreen(
    imagePath: String,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentStep by remember { mutableStateOf(ProcessingStep.FIRST) }

    Scaffold(
        topBar = {
            Column {
                // Step indicator
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
                        "Шаг ${currentStep.index + 1} из ${currentStep.totalSteps}",
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
                        slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                            slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "step_transition",
            ) { step ->
                StepContent(
                    step = step,
                    imagePath = imagePath,
                    onNext = {
                        val next = step.next()
                        if (next != null) {
                            currentStep = next
                        } else {
                            onFinish()
                        }
                    },
                    onBack = {
                        val prev = step.prev()
                        if (prev != null) {
                            currentStep = prev
                        } else {
                            onCancel()
                        }
                    },
                )
            }
        }
    }
}

/**
 * Renders the screen for a given processing step.
 * Each step gets onNext (accept) and onBack (go back) callbacks.
 */
@Composable
private fun StepContent(
    step: ProcessingStep,
    imagePath: String,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    // Each step renders a placeholder that delegates to the real screen.
    // Real screens accept onAccept/onBack — we map onNext to onAccept.
    when (step) {
        ProcessingStep.IMAGE_QUALITY -> StepPlaceholder(step, imagePath, onNext, onBack)
        ProcessingStep.CROP_REVIEW -> StepPlaceholder(step, imagePath, onNext, onBack)
        ProcessingStep.PERSPECTIVE -> StepPlaceholder(step, imagePath, onNext, onBack)
        ProcessingStep.GRAPH_SELECTION -> StepPlaceholder(step, imagePath, onNext, onBack)
        ProcessingStep.GRAPH_ROI -> StepPlaceholder(step, imagePath, onNext, onBack)
        ProcessingStep.AXIS_DETECTION -> StepPlaceholder(step, imagePath, onNext, onBack)
        ProcessingStep.X_CALIBRATION -> StepPlaceholder(step, imagePath, onNext, onBack)
        ProcessingStep.Y_CALIBRATION -> StepPlaceholder(step, imagePath, onNext, onBack)
        ProcessingStep.OCR_SUGGESTION -> StepPlaceholder(step, imagePath, onNext, onBack)
        ProcessingStep.CURVE_EXTRACTION -> StepPlaceholder(step, imagePath, onNext, onBack)
        ProcessingStep.CURVE_EDITOR -> StepPlaceholder(step, imagePath, onNext, onBack)
        ProcessingStep.SIGNAL_PREVIEW -> StepPlaceholder(step, imagePath, onNext, onBack)
        ProcessingStep.QUALITY_REPORT -> StepPlaceholder(step, imagePath, onNext, onBack)
        ProcessingStep.EXPORT -> StepPlaceholder(step, imagePath, onNext, onBack)
    }
}

/**
 * Step placeholder — shows the step name + image path + Accept/Back buttons.
 * Will be replaced with real processing screens as platform logic is integrated.
 */
@Composable
private fun StepPlaceholder(
    step: ProcessingStep,
    imagePath: String,
    onNext: () -> Unit,
    onBack: () -> Unit,
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
            "Шаг ${step.index + 1} из ${step.totalSteps}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            imagePath.substringAfterLast('/'),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            OutlinedButton(onClick = onBack) {
                Text("Назад")
            }
            Button(onClick = onNext) {
                Text(if (step.next() != null) "Принять" else "Завершить")
            }
        }
    }
}

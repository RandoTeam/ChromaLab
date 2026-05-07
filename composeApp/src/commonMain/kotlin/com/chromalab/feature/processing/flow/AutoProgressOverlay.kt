package com.chromalab.feature.processing.flow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing

/**
 * Full-screen overlay shown during auto-advance.
 * Displays a checklist of all auto-steps with live progress.
 *
 * ✓ Качество фото
 * ✓ Обрезка
 * ● Перспектива...       ← current
 * ○ Поиск графика
 * ○ Определение осей
 */
@Composable
fun AutoProgressOverlay(
    currentStep: ProcessingStep,
    isProcessing: Boolean,
    modifier: Modifier = Modifier,
) {
    // Only show during auto-advance steps
    val isAutoStep = currentStep.autoAdvance != AutoAdvancePolicy.NEVER

    AnimatedVisibility(
        visible = isAutoStep && isProcessing,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                ) {
                    Text(
                        "Автоматическая обработка",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Overall progress bar
                    val progress by animateFloatAsState(
                        targetValue = (currentStep.index + 1).toFloat() / ProcessingStep.entries.size,
                        label = "progress",
                    )
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))

                    // Step checklist — show only auto-advance steps for clarity
                    ProcessingStep.entries.forEach { step ->
                        val state = when {
                            step.index < currentStep.index -> StepState.DONE
                            step.index == currentStep.index -> StepState.ACTIVE
                            else -> StepState.PENDING
                        }
                        // Show all steps but dim NEVER-policy ones less
                        StepRow(step, state)
                    }
                }
            }
        }
    }
}

private enum class StepState { DONE, ACTIVE, PENDING }

@Composable
private fun StepRow(step: ProcessingStep, state: StepState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
    ) {
        // Status indicator
        when (state) {
            StepState.DONE -> {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                )
            }
            StepState.ACTIVE -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            StepState.PENDING -> {
                Box(
                    modifier = Modifier.size(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.outlineVariant),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(Spacing.sm))

        Text(
            step.label,
            style = MaterialTheme.typography.bodySmall,
            color = when (state) {
                StepState.DONE -> MaterialTheme.colorScheme.primary
                StepState.ACTIVE -> MaterialTheme.colorScheme.onSurface
                StepState.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
        )

        // Show "auto" badge for auto-skip steps
        if (step.autoAdvance == AutoAdvancePolicy.ALWAYS && state == StepState.DONE) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "авто",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            )
        }
    }
}

package com.chromalab.feature.processing.flow

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing

/**
 * Standard step bottom bar — one primary action + back.
 *
 * UX rule: every screen has exactly one main next step.
 * Large primary button for Accept, outlined for Back.
 */
@Composable
fun StepBottomBar(
    onAccept: () -> Unit,
    onBack: () -> Unit,
    acceptLabel: String = "Принять",
    backLabel: String = "Назад",
    acceptEnabled: Boolean = true,
    showCorrect: Boolean = false,
    onCorrect: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Back
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.height(48.dp),
            ) {
                Text(backLabel)
            }

            // Correct (optional)
            if (showCorrect && onCorrect != null) {
                OutlinedButton(
                    onClick = onCorrect,
                    modifier = Modifier.height(48.dp),
                ) {
                    Text("Исправить")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Accept — large primary button
            Button(
                onClick = onAccept,
                enabled = acceptEnabled,
                modifier = Modifier.height(48.dp),
            ) {
                Text(acceptLabel)
            }
        }
    }
}

/**
 * Step progress header — shows current step name and position.
 *
 * UX rule: always show the current stage.
 */
@Composable
fun StepProgressHeader(
    step: ProcessingStep,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { (step.index + 1).toFloat() / step.totalSteps },
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
                "Шаг ${step.index + 1} / ${step.totalSteps}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                step.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Expandable technical parameters section.
 *
 * UX rule: technical parameters are hidden by default in a collapsible block.
 */
@Composable
fun ExpandableParamsSection(
    title: String = "Технические параметры",
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(
                        start = Spacing.sm,
                        end = Spacing.sm,
                        bottom = Spacing.sm,
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    content = content,
                )
            }
        }
    }
}

/**
 * Magnifier / loupe button for precise point placement.
 *
 * UX rule: available on screens with manual marking.
 */
@Composable
fun MagnifierToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = enabled,
        onClick = { onToggle(!enabled) },
        label = { Text("🔍 Лупа") },
        modifier = modifier,
    )
}

/**
 * Zoom/Pan instruction hint.
 *
 * UX rule: show zoom/pan instructions on screens with interactive canvas.
 */
@Composable
fun ZoomPanHint(
    modifier: Modifier = Modifier,
) {
    Text(
        "Щипок — масштаб  •  Свайп — перемещение",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = Spacing.md),
    )
}

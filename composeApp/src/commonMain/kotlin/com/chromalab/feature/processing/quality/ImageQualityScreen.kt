package com.chromalab.feature.processing.quality

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing

/**
 * Image quality review screen.
 * Shows all 7 metrics, overall status, and warnings.
 * User can always proceed (even with poor quality) — warning is recorded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageQualityScreen(
    report: ImageQualityReport,
    onProceed: () -> Unit,
    onRetake: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Качество фото") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
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
                        .padding(Spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Переснять")
                    }
                    Button(
                        onClick = onProceed,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Продолжить")
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Overall status card
            OverallStatusCard(report.overallLevel)

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Individual metrics
            MetricRow(Icons.Filled.BlurOn, "Резкость", report.blurScore)
            MetricRow(Icons.Filled.WbSunny, "Яркость", report.brightnessScore)
            MetricRow(Icons.Filled.Contrast, "Контраст", report.contrastScore)
            MetricRow(Icons.Filled.Flare, "Блики", report.glareScore)
            MetricRow(Icons.Filled.WbShade, "Освещение", report.shadowScore)
            MetricRow(Icons.Filled.CropFree, "Заполнение", report.frameFillScore)
            MetricRow(Icons.Filled.ScreenRotation, "Перекос", report.skewScore)

            // Warnings
            if (report.hasWarnings) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    "Предупреждения",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                report.warnings.forEach { warning ->
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text(
                            warning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OverallStatusCard(level: QualityLevel) {
    val containerColor by animateColorAsState(
        targetValue = when (level) {
            QualityLevel.GOOD -> Color(0xFF1B5E20).copy(alpha = 0.15f)
            QualityLevel.ACCEPTABLE -> Color(0xFFE65100).copy(alpha = 0.15f)
            QualityLevel.POOR -> MaterialTheme.colorScheme.errorContainer
        },
    )
    val contentColor = when (level) {
        QualityLevel.GOOD -> Color(0xFF4CAF50)
        QualityLevel.ACCEPTABLE -> Color(0xFFFF9800)
        QualityLevel.POOR -> MaterialTheme.colorScheme.error
    }
    val icon = when (level) {
        QualityLevel.GOOD -> Icons.Filled.CheckCircle
        QualityLevel.ACCEPTABLE -> Icons.Filled.Warning
        QualityLevel.POOR -> Icons.Filled.Error
    }
    val text = when (level) {
        QualityLevel.GOOD -> "Хорошее качество"
        QualityLevel.ACCEPTABLE -> "Допустимое качество"
        QualityLevel.POOR -> "Низкое качество"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@Composable
private fun MetricRow(icon: ImageVector, label: String, metric: QualityMetric) {
    val statusColor = when (metric.level) {
        QualityLevel.GOOD -> Color(0xFF4CAF50)
        QualityLevel.ACCEPTABLE -> Color(0xFFFF9800)
        QualityLevel.POOR -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(metric.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                imageVector = when (metric.level) {
                    QualityLevel.GOOD -> Icons.Filled.CheckCircle
                    QualityLevel.ACCEPTABLE -> Icons.Filled.Warning
                    QualityLevel.POOR -> Icons.Filled.Cancel
                },
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

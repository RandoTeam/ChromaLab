package com.chromalab.feature.processing.quality

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing

/**
 * Shows the final digitization quality report with stage-by-stage scoring.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualityReportContent(
    report: DigitizationQualityReport,
    onAccept: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Отчёт о качестве") },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OutlinedButton(onClick = onBack) {
                    Text("Назад")
                }
                Button(onClick = onAccept) {
                    Text("Продолжить")
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            // Overall score
            OverallScoreCard(report.overall)

            Spacer(Modifier.height(Spacing.xs))

            // Stage scores
            StageRow("📷 Качество фото", report.imageQuality)
            StageRow("📄 Определение документа", report.documentDetection)
            StageRow("📊 Область графика", report.graphDetection)
            StageRow("📐 Калибровка осей", report.axisCalibration)
            StageRow("📈 Извлечение кривой", report.curveExtraction)

            // Warnings
            val warnings = report.allWarnings
            if (warnings.isNotEmpty()) {
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    "Предупреждения",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                warnings.forEach { warning ->
                    Text(
                        "• $warning",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun OverallScoreCard(overall: StageQuality) {
    val (color, label) = when (overall.status) {
        QualityStatus.GOOD -> Pair(Color(0xFF4CAF50), "Отлично")
        QualityStatus.ACCEPTABLE -> Pair(Color(0xFFFFA726), "Приемлемо")
        QualityStatus.RISKY -> Pair(Color(0xFFFF7043), "Рискованно")
        QualityStatus.FAILED -> Pair(Color(0xFFE53935), "Недостаточно")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "${(overall.score * 100).toInt()}%",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                color = color,
            )
        }
    }
}

@Composable
private fun StageRow(label: String, stage: StageQuality) {
    val (color, _) = when (stage.status) {
        QualityStatus.GOOD -> Pair(Color(0xFF4CAF50), "✓")
        QualityStatus.ACCEPTABLE -> Pair(Color(0xFFFFA726), "~")
        QualityStatus.RISKY -> Pair(Color(0xFFFF7043), "!")
        QualityStatus.FAILED -> Pair(Color(0xFFE53935), "✗")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        LinearProgressIndicator(
            progress = { stage.score },
            modifier = Modifier
                .width(100.dp)
                .height(8.dp),
            color = color,
            trackColor = color.copy(alpha = 0.15f),
        )
        Text(
            "${(stage.score * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(40.dp),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

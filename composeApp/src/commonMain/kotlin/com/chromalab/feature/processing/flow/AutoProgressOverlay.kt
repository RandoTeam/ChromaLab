package com.chromalab.feature.processing.flow

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromalab.feature.processing.sweep.AutoSweepEngine

// ─── Premium Color Palette ──────────────────────────────────────────
// Non-standard: deep ocean teal → electric violet → warm amber accents
private val ChromaGradientStart = Color(0xFF0D9488)   // Teal-600
private val ChromaGradientMid   = Color(0xFF6D28D9)   // Violet-700
private val ChromaGradientEnd   = Color(0xFF7C3AED)   // Violet-600
private val ChromaAccent        = Color(0xFFF59E0B)   // Amber-500
private val ChromaGlow          = Color(0xFF2DD4BF)   // Teal-400
private val ChromaDone          = Color(0xFF10B981)   // Emerald-500
private val ChromaPending       = Color(0xFF475569)   // Slate-600
private val ChromaSurface       = Color(0xFF0F172A)   // Slate-900
private val ChromaSurfaceAlt    = Color(0xFF1E293B)   // Slate-800
private val ChromaText          = Color(0xFFF1F5F9)   // Slate-100
private val ChromaTextDim       = Color(0xFF94A3B8)   // Slate-400
private val ChromaTrack         = Color(0xFF334155)   // Slate-700

/**
 * Premium full-screen processing overlay.
 *
 * Features:
 * - Gradient arc ring with animated progress
 * - Animated percentage counter (smooth interpolation)
 * - Crossfade step description text
 * - Compact step checklist with Material icons
 * - Sweep progress detail (when active)
 * - Dark theme with teal/violet/amber palette
 */
@Composable
fun AutoProgressOverlay(
    currentStep: ProcessingStep,
    isProcessing: Boolean,
    sweepProgress: AutoSweepEngine.SweepProgress? = null,
    bestSweepConfig: String? = null,
    currentGraphIndex: Int = 0,
    totalGraphs: Int = 1,
    modifier: Modifier = Modifier,
) {
    val isAutoStep = currentStep.autoAdvance != AutoAdvancePolicy.NEVER

    AnimatedVisibility(
        visible = isAutoStep && isProcessing,
        enter = fadeIn(animationSpec = tween(400)),
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(ChromaSurface),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ─── Graph counter (multi-graph only) ───
                if (totalGraphs > 1) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = ChromaSurfaceAlt,
                    ) {
                        Text(
                            "График ${currentGraphIndex + 1} / $totalGraphs",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = ChromaAccent,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ─── Gradient Arc Ring + Percentage ───
                val targetProgress = (currentStep.index + 1).toFloat() / ProcessingStep.entries.size
                val animatedProgress by animateFloatAsState(
                    targetValue = targetProgress,
                    animationSpec = tween(
                        durationMillis = 800,
                        easing = FastOutSlowInEasing,
                    ),
                    label = "ring_progress",
                )

                // Glow pulse animation
                val infiniteTransition = rememberInfiniteTransition(label = "glow")
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "glow_alpha",
                )

                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Background ring
                    val canvasPadding = 8.dp
                    Canvas(modifier = Modifier.fillMaxSize().padding(canvasPadding)) {
                        val strokeWidth = 10.dp.toPx()
                        val ringSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        // Track (dark)
                        drawArc(
                            color = ChromaTrack,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = ringSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )

                        // Progress arc with gradient
                        val sweepAngle = animatedProgress * 360f
                        drawArc(
                            brush = Brush.sweepGradient(
                                0f to ChromaGradientStart,
                                0.5f to ChromaGradientMid,
                                1f to ChromaGradientEnd,
                            ),
                            startAngle = -90f,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = topLeft,
                            size = ringSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        )

                        // Glow dot at the tip of progress arc
                        // Radius from center to the middle of the stroke
                        val glowAngleRad = Math.toRadians((-90f + sweepAngle).toDouble())
                        val ringRadius = ringSize.width / 2
                        val cx = center.x + ringRadius * kotlin.math.cos(glowAngleRad).toFloat()
                        val cy = center.y + ringRadius * kotlin.math.sin(glowAngleRad).toFloat()
                        drawCircle(
                            color = ChromaGlow.copy(alpha = glowAlpha),
                            radius = strokeWidth * 1.5f,
                            center = Offset(cx, cy),
                        )
                    }

                    // Percentage text
                    val percent = (animatedProgress * 100).toInt()
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$percent",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = ChromaText,
                            fontSize = 48.sp,
                        )
                        Text(
                            "%",
                            style = MaterialTheme.typography.titleSmall,
                            color = ChromaTextDim,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ─── Animated step description ───
                AnimatedContent(
                    targetState = currentStep.label,
                    transitionSpec = {
                        fadeIn(tween(400)) + slideInVertically { it / 2 } togetherWith
                            fadeOut(tween(200)) + slideOutVertically { -it / 2 }
                    },
                    label = "step_label",
                ) { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ChromaText,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Step description hint — crossfade in sync with label above
                AnimatedContent(
                    targetState = stepDescription(currentStep),
                    transitionSpec = {
                        fadeIn(tween(400)) togetherWith fadeOut(tween(200))
                    },
                    label = "step_desc",
                ) { desc ->
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = ChromaTextDim,
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ─── Horizontal progress bar ───
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ChromaTrack),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ChromaGradientStart, ChromaGradientMid, ChromaGradientEnd)
                                )
                            ),
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ─── Compact step checklist ───
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ChromaSurfaceAlt,
                    modifier = Modifier.fillMaxWidth(0.9f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        ProcessingStep.entries.forEach { step ->
                            val state = when {
                                step.index < currentStep.index -> StepVisualState.DONE
                                step.index == currentStep.index -> StepVisualState.ACTIVE
                                else -> StepVisualState.PENDING
                            }
                            PremiumStepRow(step, state)

                            // Sweep detail
                            if (step == ProcessingStep.GRAPH_SELECTION && state == StepVisualState.ACTIVE) {
                                val sp = sweepProgress
                                if (sp != null && sp.totalConfigs > 0) {
                                    SweepDetailCompact(sp)
                                }
                            }
                        }

                        // Best config badge
                        if (bestSweepConfig != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.padding(start = 30.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = ChromaAccent,
                                    modifier = Modifier.size(12.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    bestSweepConfig,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ChromaAccent,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Step Visual State ──────────────────────────────────────────────

private enum class StepVisualState { DONE, ACTIVE, PENDING }

/**
 * Map each ProcessingStep to a Material icon (no emojis).
 */
private fun stepIcon(step: ProcessingStep): ImageVector = when (step) {
    ProcessingStep.IMAGE_QUALITY -> Icons.Filled.PhotoCamera
    ProcessingStep.CROP_REVIEW -> Icons.Filled.Crop
    ProcessingStep.PERSPECTIVE -> Icons.Filled.Transform
    ProcessingStep.GRAPH_SELECTION -> Icons.Filled.GridOn
    ProcessingStep.GRAPH_ROI -> Icons.Filled.CropFree
    ProcessingStep.AXIS_DETECTION -> Icons.Filled.Straighten
    ProcessingStep.OCR_SUGGESTION -> Icons.Filled.TextFields
    ProcessingStep.X_CALIBRATION -> Icons.Filled.SpaceBar
    ProcessingStep.Y_CALIBRATION -> Icons.Filled.Height
    ProcessingStep.CURVE_EXTRACTION -> Icons.Filled.ShowChart
    ProcessingStep.CURVE_EDITOR -> Icons.Filled.Edit
    ProcessingStep.SIGNAL_PREVIEW -> Icons.Filled.Timeline
    ProcessingStep.QUALITY_REPORT -> Icons.Filled.VerifiedUser
    ProcessingStep.EXPORT -> Icons.Filled.Assessment
}

/**
 * Human-readable description for each step.
 */
private fun stepDescription(step: ProcessingStep): String = when (step) {
    ProcessingStep.IMAGE_QUALITY -> "Оценка резкости и контрастности"
    ProcessingStep.CROP_REVIEW -> "Определение границ документа"
    ProcessingStep.PERSPECTIVE -> "Коррекция искажений перспективы"
    ProcessingStep.GRAPH_SELECTION -> "Поиск области с графиком"
    ProcessingStep.GRAPH_ROI -> "Уточнение границ графика"
    ProcessingStep.AXIS_DETECTION -> "Распознавание координатных осей"
    ProcessingStep.OCR_SUGGESTION -> "Считывание подписей и меток"
    ProcessingStep.X_CALIBRATION -> "Калибровка временной оси"
    ProcessingStep.Y_CALIBRATION -> "Калибровка оси интенсивности"
    ProcessingStep.CURVE_EXTRACTION -> "Трассировка кривой сигнала"
    ProcessingStep.CURVE_EDITOR -> "Автоматическая коррекция кривой"
    ProcessingStep.SIGNAL_PREVIEW -> "Построение цифрового графика"
    ProcessingStep.QUALITY_REPORT -> "Контроль качества оцифровки"
    ProcessingStep.EXPORT -> "Формирование отчёта"
}

@Composable
private fun PremiumStepRow(step: ProcessingStep, state: StepVisualState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        // State indicator
        when (state) {
            StepVisualState.DONE -> {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(ChromaDone.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = ChromaDone,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            StepVisualState.ACTIVE -> {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(ChromaGradientStart.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.5.dp,
                        color = ChromaGlow,
                    )
                }
            }
            StepVisualState.PENDING -> {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(ChromaPending.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        stepIcon(step),
                        contentDescription = null,
                        tint = ChromaPending,
                        modifier = Modifier.size(10.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Step label
        Text(
            step.label,
            style = MaterialTheme.typography.bodySmall,
            color = when (state) {
                StepVisualState.DONE -> ChromaDone
                StepVisualState.ACTIVE -> ChromaText
                StepVisualState.PENDING -> ChromaPending
            },
            fontWeight = if (state == StepVisualState.ACTIVE) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )

        // Trailing icon: done checkmark for completed, clock for pending
        if (state == StepVisualState.DONE) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = ChromaDone.copy(alpha = 0.4f),
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

/**
 * Compact sweep detail — shown inline below GRAPH_SELECTION step.
 */
@Composable
private fun SweepDetailCompact(progress: AutoSweepEngine.SweepProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 30.dp, top = 2.dp, bottom = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (progress.currentConfig == 0) "Подготовка..."
                else "Конфиг ${progress.currentConfig}/${progress.totalConfigs}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = ChromaAccent,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                progress.configName,
                style = MaterialTheme.typography.labelSmall,
                color = ChromaTextDim,
                fontSize = 10.sp,
            )
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Mini sweep progress bar
        val sweepFraction by animateFloatAsState(
            targetValue = if (progress.totalConfigs > 0) {
                progress.currentConfig.toFloat() / progress.totalConfigs
            } else 0f,
            label = "sweep",
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(ChromaTrack),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(sweepFraction)
                    .clip(RoundedCornerShape(1.dp))
                    .background(ChromaAccent),
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Phase label with Material icon
        val (phaseIcon, phaseLabel) = when (progress.phase) {
            "detect" -> Icons.Filled.Search to "Определение графика"
            "ocr" -> Icons.Filled.TextFields to "Распознавание текста"
            "axes" -> Icons.Filled.Straighten to "Определение осей"
            "preprocess" -> Icons.Filled.Tune to "Предобработка"
            "curve" -> Icons.Filled.ShowChart to "Извлечение кривой"
            else -> Icons.Filled.Settings to progress.phase
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                phaseIcon,
                contentDescription = null,
                tint = ChromaTextDim.copy(alpha = 0.6f),
                modifier = Modifier.size(10.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                phaseLabel,
                style = MaterialTheme.typography.labelSmall,
                color = ChromaTextDim.copy(alpha = 0.6f),
                fontSize = 10.sp,
            )
        }
    }
}

package com.chromalab.feature.calculation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chromalab.core.ui.theme.Spacing

/**
 * Algorithm settings UI (§2.26).
 *
 * Sections: Smoothing, Baseline, Peak Detection, Boundaries,
 * Integration, Noise/S/N, Confidence, Advanced.
 *
 * Rules:
 * - Presets at top (Conservative, Balanced, Sensitive, Manual Review)
 * - Each section collapsible
 * - Changes require explicit "Пересчитать" — no silent recalculation
 * - Reset to defaults available
 */

// ─── Settings data model ────────────────────────────────────────

data class AlgorithmSettings(
    // Smoothing
    val smoothingEnabled: Boolean = true,
    val smoothingWindowSize: Int = 7,
    val smoothingPolyOrder: Int = 3,

    // Baseline
    val baselineMethod: BaselineMethodOption = BaselineMethodOption.ALS,
    val alsLambda: Double = 1e6,
    val alsPenalty: Double = 0.01,
    val alsIterations: Int = 10,
    val snipIterations: Int = 40,

    // Peak detection
    val minHeight: Double = 0.0,
    val minProminence: Double = 0.0,
    val noiseK: Double = 3.0,
    val minDistance: Int = 5,
    val minWidth: Int = 3,
    val maxWidth: Int = Int.MAX_VALUE,

    // Boundaries
    val boundaryMethod: BoundaryMethodOption = BoundaryMethodOption.LOCAL_MINIMA,
    val percentHeight: Double = 0.01,

    // Integration
    val clampNegative: Boolean = false,
    val useInterpolatedBoundaries: Boolean = true,

    // Noise / S/N
    val noiseMethod: NoiseMethodOption = NoiseMethodOption.MAD,
    val noiseAutoRegion: Boolean = true,

    // Preset
    val presetName: String = "Balanced",
) {
    companion object {
        fun defaults() = AlgorithmSettings()
    }
}

enum class BaselineMethodOption(val label: String) {
    MANUAL("Ручная линейная"),
    ALS("ALS"),
    SNIP("SNIP"),
}

enum class BoundaryMethodOption(val label: String) {
    PROMINENCE_BASES("Prominence bases"),
    LOCAL_MINIMA("Локальные минимумы"),
    BASELINE_INTERSECTION("Пересечение baseline"),
    PERCENT_HEIGHT("% от высоты"),
}

enum class NoiseMethodOption(val label: String) {
    PEAK_TO_PEAK("Peak-to-Peak"),
    RMS("RMS"),
    MAD("MAD (robust)"),
}

// ─── Settings Screen ────────────────────────────────────────────

@Composable
fun AlgorithmSettingsPanel(
    settings: AlgorithmSettings,
    onSettingsChange: (AlgorithmSettings) -> Unit,
    onRecalculate: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var local by remember(settings) { mutableStateOf(settings) }
    val hasChanges = local != settings

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // Preset selector
        PresetSelector(
            selected = AlgorithmPreset.entries.firstOrNull { it.name.equals(local.presetName, ignoreCase = true) }
                ?: AlgorithmPreset.BALANCED,
            onSelect = { preset ->
                local = applyPreset(preset)
            },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // Sections
        SmoothingSection(local) { local = it }
        BaselineSection(local) { local = it }
        PeakDetectionSection(local) { local = it }
        BoundariesSection(local) { local = it }
        IntegrationSection(local) { local = it }
        NoiseSectionUI(local) { local = it }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // Action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            OutlinedButton(
                onClick = {
                    local = AlgorithmSettings.defaults()
                    onReset()
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Сброс", style = MaterialTheme.typography.labelSmall)
            }
            Button(
                onClick = {
                    onSettingsChange(local)
                    onRecalculate()
                },
                modifier = Modifier.weight(1f),
                enabled = hasChanges,
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Пересчитать", style = MaterialTheme.typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))
    }
}

// ─── Sections ───────────────────────────────────────────────────

@Composable
private fun SmoothingSection(s: AlgorithmSettings, onChange: (AlgorithmSettings) -> Unit) {
    SettingsSection("Сглаживание") {
        SwitchRow("Включено", s.smoothingEnabled) { onChange(s.copy(smoothingEnabled = it)) }
        if (s.smoothingEnabled) {
            SliderRow("Окно", s.smoothingWindowSize.toFloat(), 3f, 31f, 1f) {
                onChange(s.copy(smoothingWindowSize = it.toInt().let { v -> if (v % 2 == 0) v + 1 else v }))
            }
            SliderRow("Полином", s.smoothingPolyOrder.toFloat(), 1f, 5f, 1f) {
                onChange(s.copy(smoothingPolyOrder = it.toInt()))
            }
        }
    }
}

@Composable
private fun BaselineSection(s: AlgorithmSettings, onChange: (AlgorithmSettings) -> Unit) {
    SettingsSection("Baseline") {
        OptionRow("Метод", BaselineMethodOption.entries, s.baselineMethod) {
            onChange(s.copy(baselineMethod = it))
        }
        when (s.baselineMethod) {
            BaselineMethodOption.ALS -> {
                SliderRow("Lambda (log10)", kotlin.math.log10(s.alsLambda).toFloat(), 3f, 9f, 0.5f) {
                    onChange(s.copy(alsLambda = Math.pow(10.0, it.toDouble())))
                }
                SliderRow("Penalty", s.alsPenalty.toFloat(), 0.001f, 0.1f, 0.001f) {
                    onChange(s.copy(alsPenalty = it.toDouble()))
                }
                SliderRow("Итерации", s.alsIterations.toFloat(), 1f, 30f, 1f) {
                    onChange(s.copy(alsIterations = it.toInt()))
                }
            }
            BaselineMethodOption.SNIP -> {
                SliderRow("Итерации", s.snipIterations.toFloat(), 10f, 100f, 1f) {
                    onChange(s.copy(snipIterations = it.toInt()))
                }
            }
            BaselineMethodOption.MANUAL -> { /* No params */ }
        }
    }
}

@Composable
private fun PeakDetectionSection(s: AlgorithmSettings, onChange: (AlgorithmSettings) -> Unit) {
    SettingsSection("Детекция пиков") {
        SliderRow("Noise K", s.noiseK.toFloat(), 1f, 10f, 0.5f) {
            onChange(s.copy(noiseK = it.toDouble()))
        }
        SliderRow("Мин. расстояние", s.minDistance.toFloat(), 1f, 50f, 1f) {
            onChange(s.copy(minDistance = it.toInt()))
        }
        SliderRow("Мин. ширина", s.minWidth.toFloat(), 1f, 20f, 1f) {
            onChange(s.copy(minWidth = it.toInt()))
        }
    }
}

@Composable
private fun BoundariesSection(s: AlgorithmSettings, onChange: (AlgorithmSettings) -> Unit) {
    SettingsSection("Границы") {
        OptionRow("Метод", BoundaryMethodOption.entries, s.boundaryMethod) {
            onChange(s.copy(boundaryMethod = it))
        }
        if (s.boundaryMethod == BoundaryMethodOption.PERCENT_HEIGHT) {
            SliderRow("% высоты", (s.percentHeight * 100).toFloat(), 0.5f, 10f, 0.5f) {
                onChange(s.copy(percentHeight = it.toDouble() / 100.0))
            }
        }
    }
}

@Composable
private fun IntegrationSection(s: AlgorithmSettings, onChange: (AlgorithmSettings) -> Unit) {
    SettingsSection("Интеграция") {
        SwitchRow("Обнулять отрицательные", s.clampNegative) {
            onChange(s.copy(clampNegative = it))
        }
        SwitchRow("Интерполяция границ", s.useInterpolatedBoundaries) {
            onChange(s.copy(useInterpolatedBoundaries = it))
        }
    }
}

@Composable
private fun NoiseSectionUI(s: AlgorithmSettings, onChange: (AlgorithmSettings) -> Unit) {
    SettingsSection("Шум / S/N") {
        OptionRow("Метод шума", NoiseMethodOption.entries, s.noiseMethod) {
            onChange(s.copy(noiseMethod = it))
        }
        SwitchRow("Авто-регион", s.noiseAutoRegion) {
            onChange(s.copy(noiseAutoRegion = it))
        }
    }
}

// ─── Reusable components ────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(24.dp)) {
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null, modifier = Modifier.size(18.dp),
                )
            }
        }
        if (expanded) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs),
                    content = content,
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderRow(label: String, value: Float, min: Float, max: Float, step: Float, onChange: (Float) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (step >= 1f) "${value.toInt()}" else "%.1f".format(value),
                style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = min..max,
            steps = ((max - min) / step).toInt() - 1,
        )
    }
}

@Composable
private fun <T : Enum<T>> OptionRow(
    label: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = {
                        Text(
                            (option as? BaselineMethodOption)?.label
                                ?: (option as? BoundaryMethodOption)?.label
                                ?: (option as? NoiseMethodOption)?.label
                                ?: option.name,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ─── Preset application ─────────────────────────────────────────

private fun applyPreset(preset: AlgorithmPreset): AlgorithmSettings = when (preset) {
    AlgorithmPreset.CONSERVATIVE -> AlgorithmSettings(
        noiseK = 5.0, minDistance = 10, minWidth = 5,
        presetName = "Conservative",
    )
    AlgorithmPreset.BALANCED -> AlgorithmSettings.defaults()
    AlgorithmPreset.SENSITIVE -> AlgorithmSettings(
        noiseK = 2.0, minDistance = 3, minWidth = 2,
        presetName = "Sensitive",
    )
    AlgorithmPreset.MANUAL_REVIEW -> AlgorithmSettings(
        noiseK = 10.0, minDistance = 20, minWidth = 5,
        presetName = "Manual Review",
    )
}

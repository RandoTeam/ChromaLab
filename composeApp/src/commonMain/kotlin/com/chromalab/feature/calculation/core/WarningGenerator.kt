package com.chromalab.feature.calculation.core

/**
 * Comprehensive warning system for chromatogram analysis (§2.28).
 *
 * Generates human-readable, severity-graded warnings from pipeline results.
 * Warnings are NEVER hidden — all are reported for full transparency.
 *
 * Categories:
 * 1. Peak quality: low S/N, smoothing artifacts, overlap, shoulder
 * 2. Baseline issues: area impact, signal crossing, failed fit
 * 3. Integration issues: negative area, low quality digitization
 * 4. Overall: orientational result, low confidence
 */

// ─── Warning model ──────────────────────────────────────────────

data class PipelineWarning(
    val code: WarningCode,
    val severity: WarningSeverity,
    val message: String,
    val peakIndex: Int? = null,
    val stage: String,
)

enum class WarningCode {
    // Peak quality
    LOW_SNR,
    PEAK_FROM_SMOOTHING,
    PEAK_OVERLAP,
    SHOULDER_PEAK,
    MANUAL_BOUNDARIES,
    UNRESOLVED_OVERLAP,

    // Baseline
    BASELINE_AFFECTS_AREA,
    NOISE_REGION_PROBLEM,
    BASELINE_ABOVE_SIGNAL,
    BASELINE_CROSSES_SIGNAL,
    BASELINE_FIT_FAILED,

    // Integration
    NEGATIVE_AREA,
    LOW_DIGITIZATION_QUALITY,
    INTERPOLATED_BOUNDARIES,
    LOW_CONFIDENCE,

    // Overall
    RESULT_ORIENTATIONAL,
    NO_PEAKS_DETECTED,
    TOO_MANY_PEAKS,
    SIGNAL_TOO_SHORT,
}

// ─── Warning generator ──────────────────────────────────────────

object WarningGenerator {

    /**
     * Generate all warnings from pipeline analysis results.
     *
     * Accepts generic metric maps to stay decoupled from algorithm internals.
     */
    fun generate(context: WarningContext): List<PipelineWarning> {
        val warnings = mutableListOf<PipelineWarning>()

        // Signal-level warnings
        generateSignalWarnings(context, warnings)

        // Baseline warnings
        generateBaselineWarnings(context, warnings)

        // Per-peak warnings
        context.peaks.forEachIndexed { i, peak ->
            generatePeakWarnings(i, peak, context, warnings)
        }

        // Overall assessment
        generateOverallWarnings(context, warnings)

        return warnings
    }

    // ─── Signal ─────────────────────────────────────────────────

    private fun generateSignalWarnings(ctx: WarningContext, out: MutableList<PipelineWarning>) {
        if (ctx.signalPointCount < 20) {
            out += PipelineWarning(
                WarningCode.SIGNAL_TOO_SHORT, WarningSeverity.SERIOUS,
                "Сигнал содержит менее 20 точек (${ctx.signalPointCount}). Результаты ненадёжны.",
                stage = "signal",
            )
        }
        if (ctx.hasLowDigitizationQuality) {
            out += PipelineWarning(
                WarningCode.LOW_DIGITIZATION_QUALITY, WarningSeverity.CAUTION,
                "Низкое качество оцифровки. Артефакты ступенек могут влиять на результаты.",
                stage = "signal",
            )
        }
    }

    // ─── Baseline ───────────────────────────────────────────────

    private fun generateBaselineWarnings(ctx: WarningContext, out: MutableList<PipelineWarning>) {
        if (ctx.baselineFitFailed) {
            out += PipelineWarning(
                WarningCode.BASELINE_FIT_FAILED, WarningSeverity.FAILED,
                "Расчёт baseline не удался. Все площади и высоты ненадёжны.",
                stage = "baseline",
            )
        }
        if (ctx.baselineAboveSignalPercent > 5.0) {
            out += PipelineWarning(
                WarningCode.BASELINE_ABOVE_SIGNAL, WarningSeverity.SERIOUS,
                "Baseline выше сигнала в ${"%.1f".format(ctx.baselineAboveSignalPercent)}% точек.",
                stage = "baseline",
            )
        }
        if (ctx.baselineCrossesSignalCount > 3) {
            out += PipelineWarning(
                WarningCode.BASELINE_CROSSES_SIGNAL, WarningSeverity.CAUTION,
                "Baseline пересекает сигнал ${ctx.baselineCrossesSignalCount} раз. Возможны артефакты.",
                stage = "baseline",
            )
        }
        if (ctx.noiseRegionTooSmall) {
            out += PipelineWarning(
                WarningCode.NOISE_REGION_PROBLEM, WarningSeverity.CAUTION,
                "Noise region слишком мал или содержит пики. S/N может быть неточным.",
                stage = "noise",
            )
        }
    }

    // ─── Per-peak ───────────────────────────────────────────────

    private fun generatePeakWarnings(
        index: Int,
        peak: PeakWarningData,
        ctx: WarningContext,
        out: MutableList<PipelineWarning>,
    ) {
        if (peak.snr < 3.0) {
            out += PipelineWarning(
                WarningCode.LOW_SNR,
                if (peak.snr < 1.5) WarningSeverity.SERIOUS else WarningSeverity.CAUTION,
                "Пик #${index + 1}: S/N = ${"%.1f".format(peak.snr)}. " +
                    if (peak.snr < 1.5) "Ниже предела обнаружения." else "Ниже предела количественного определения.",
                peakIndex = index, stage = "peak_quality",
            )
        }

        if (peak.isFromSmoothing) {
            out += PipelineWarning(
                WarningCode.PEAK_FROM_SMOOTHING, WarningSeverity.CAUTION,
                "Пик #${index + 1}: обнаружен только после сглаживания. Возможен артефакт.",
                peakIndex = index, stage = "peak_quality",
            )
        }

        if (peak.isOverlapping) {
            val severity = if (peak.isUnresolved) WarningSeverity.SERIOUS else WarningSeverity.CAUTION
            val code = if (peak.isUnresolved) WarningCode.UNRESOLVED_OVERLAP else WarningCode.PEAK_OVERLAP
            out += PipelineWarning(
                code, severity,
                "Пик #${index + 1}: " + if (peak.isUnresolved)
                    "Неразрешённое перекрытие. Площадь ненадёжна. Требуется ручная коррекция."
                else "Частичное перекрытие с соседним пиком. Площадь приблизительна.",
                peakIndex = index, stage = "overlap",
            )
        }

        if (peak.isShoulder) {
            out += PipelineWarning(
                WarningCode.SHOULDER_PEAK, WarningSeverity.CAUTION,
                "Пик #${index + 1}: плечо на склоне основного пика. Рекомендуется проверить.",
                peakIndex = index, stage = "overlap",
            )
        }

        if (peak.hasManualBoundaries) {
            out += PipelineWarning(
                WarningCode.MANUAL_BOUNDARIES, WarningSeverity.INFO,
                "Пик #${index + 1}: границы установлены вручную.",
                peakIndex = index, stage = "boundaries",
            )
        }

        if (peak.negativeAreaPercent > 1.0) {
            val severity = if (peak.negativeAreaPercent > 20.0) WarningSeverity.SERIOUS else WarningSeverity.CAUTION
            out += PipelineWarning(
                WarningCode.NEGATIVE_AREA, severity,
                "Пик #${index + 1}: ${"%.1f".format(peak.negativeAreaPercent)}% площади отрицательна. " +
                    "Проверьте baseline.",
                peakIndex = index, stage = "integration",
            )
        }

        if (peak.hasInterpolatedBoundaries) {
            out += PipelineWarning(
                WarningCode.INTERPOLATED_BOUNDARIES, WarningSeverity.INFO,
                "Пик #${index + 1}: границы интерполированы между точками измерения.",
                peakIndex = index, stage = "integration",
            )
        }

        if (peak.confidenceScore < 0.5) {
            out += PipelineWarning(
                WarningCode.LOW_CONFIDENCE, WarningSeverity.CAUTION,
                "Пик #${index + 1}: низкая уверенность (${"%.0f".format(peak.confidenceScore * 100)}%). " +
                    "Рекомендуется ручная проверка.",
                peakIndex = index, stage = "confidence",
            )
        }

        if (ctx.baselineAboveSignalPercent > 5.0 && peak.area > 0) {
            out += PipelineWarning(
                WarningCode.BASELINE_AFFECTS_AREA, WarningSeverity.CAUTION,
                "Пик #${index + 1}: baseline может влиять на точность площади.",
                peakIndex = index, stage = "baseline",
            )
        }
    }

    // ─── Overall ────────────────────────────────────────────────

    private fun generateOverallWarnings(ctx: WarningContext, out: MutableList<PipelineWarning>) {
        if (ctx.peaks.isEmpty()) {
            out += PipelineWarning(
                WarningCode.NO_PEAKS_DETECTED, WarningSeverity.INFO,
                "Пики не обнаружены. Попробуйте понизить пороги или использовать Sensitive preset.",
                stage = "overall",
            )
        }

        if (ctx.peaks.size > 100) {
            out += PipelineWarning(
                WarningCode.TOO_MANY_PEAKS, WarningSeverity.CAUTION,
                "Обнаружено ${ctx.peaks.size} пиков. Возможно, пороги слишком низкие.",
                stage = "overall",
            )
        }

        val hasSerious = out.any { it.severity == WarningSeverity.SERIOUS || it.severity == WarningSeverity.FAILED }
        if (hasSerious) {
            out += PipelineWarning(
                WarningCode.RESULT_ORIENTATIONAL, WarningSeverity.SERIOUS,
                "Результат ориентировочный. Обнаружены серьёзные проблемы. См. предупреждения выше.",
                stage = "overall",
            )
        }
    }
}

// ─── Warning context (input) ────────────────────────────────────

data class WarningContext(
    val signalPointCount: Int,
    val hasLowDigitizationQuality: Boolean = false,
    val baselineFitFailed: Boolean = false,
    val baselineAboveSignalPercent: Double = 0.0,
    val baselineCrossesSignalCount: Int = 0,
    val noiseRegionTooSmall: Boolean = false,
    val peaks: List<PeakWarningData> = emptyList(),
)

data class PeakWarningData(
    val snr: Double,
    val area: Double,
    val confidenceScore: Double,
    val negativeAreaPercent: Double = 0.0,
    val isFromSmoothing: Boolean = false,
    val isOverlapping: Boolean = false,
    val isUnresolved: Boolean = false,
    val isShoulder: Boolean = false,
    val hasManualBoundaries: Boolean = false,
    val hasInterpolatedBoundaries: Boolean = false,
)

// ─── Summary helpers ────────────────────────────────────────────

/** Count warnings by severity. */
fun List<PipelineWarning>.countBySeverity(): Map<WarningSeverity, Int> =
    groupBy { it.severity }.mapValues { it.value.size }

/** Get worst severity. */
fun List<PipelineWarning>.worstSeverity(): WarningSeverity =
    maxOfOrNull { it.severity.ordinal }?.let { WarningSeverity.entries[it] } ?: WarningSeverity.INFO

/** Filter to specific peak. */
fun List<PipelineWarning>.forPeak(index: Int): List<PipelineWarning> =
    filter { it.peakIndex == index }

/** Filter to global (non-peak) warnings. */
fun List<PipelineWarning>.global(): List<PipelineWarning> =
    filter { it.peakIndex == null }

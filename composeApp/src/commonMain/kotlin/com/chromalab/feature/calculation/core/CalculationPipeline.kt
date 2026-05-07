package com.chromalab.feature.calculation.core

/**
 * Phase 2 — Calculation Pipeline.
 *
 * Fixed pipeline order (immutable):
 *   1. Input validation
 *   2. Optional smoothing
 *   3. Baseline estimation
 *   4. Baseline correction
 *   5. Noise estimation
 *   6. Peak detection
 *   7. Peak boundary detection
 *   8. Peak integration
 *   9. Peak metric calculation
 *  10. Warnings / confidence
 *  11. Manual corrections
 *  12. Recalculation
 *
 * Contract:
 * - All calculation functions are pure (no side effects, no randomness).
 * - Uses Double for numeric precision (not Float).
 * - Same signal + same params + same manual edits = same result.
 * - Raw signal is NEVER modified — all derived signals stored separately.
 * - Every recalculation creates a new CalculationRun (old runs are immutable).
 */
object CalculationPipeline {
    const val PIPELINE_VERSION = "2.0.0"
    const val ALGORITHM_VERSION = "2.0.0"
}

/**
 * Pipeline step identifiers for traceability.
 */
enum class PipelineStage(val index: Int, val label: String) {
    INPUT_VALIDATION(1, "Валидация входа"),
    SMOOTHING(2, "Сглаживание"),
    BASELINE_ESTIMATION(3, "Оценка baseline"),
    BASELINE_CORRECTION(4, "Коррекция baseline"),
    NOISE_ESTIMATION(5, "Оценка шума"),
    PEAK_DETECTION(6, "Поиск пиков"),
    PEAK_BOUNDARIES(7, "Границы пиков"),
    PEAK_INTEGRATION(8, "Интегрирование"),
    PEAK_METRICS(9, "Метрики пиков"),
    WARNINGS(10, "Предупреждения"),
    MANUAL_CORRECTIONS(11, "Ручные правки"),
    RECALCULATION(12, "Пересчёт"),
}

package com.chromalab.feature.calculation.core

import com.chromalab.feature.calculation.algorithm.BaselineDispatcher
import com.chromalab.feature.calculation.algorithm.BaselineMethod
import com.chromalab.feature.calculation.algorithm.SavitzkyGolayFilter
import com.chromalab.feature.processing.signal.DigitalSignal

/**
 * Calculation Engine — the deterministic pipeline executor.
 *
 * Executes the fixed pipeline order (§2.7):
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
 *  11. Manual corrections (applied externally)
 *  12. Recalculation (new run)
 *
 * Contract:
 * - Every run produces a new immutable CalculationRun
 * - Old runs are NEVER overwritten
 * - Same signal + same params + same edits = same result
 * - pipelineVersion and algorithmVersion are always recorded
 * - All parameters are stored in the run for reproducibility
 */
object CalculationEngine {

    /**
     * Execute the full calculation pipeline.
     *
     * @param signal        Raw signal from Phase 1
     * @param sourceId      Unique identifier for the source signal
     * @param params        Algorithm parameters (includes all settings)
     * @param manualEdits   List of manual corrections (empty for first run)
     * @param runId         Unique ID for this calculation run
     * @return Immutable CalculationRun with all results
     */
    fun execute(
        signal: DigitalSignal,
        sourceId: String,
        params: CalculationParams = CalculationParams(),
        manualEdits: List<ManualEdit> = emptyList(),
        runId: String = generateRunId(),
    ): CalculationRun {
        // Stage 1: Input validation
        val (calcSignal, validation) = SignalValidator.validate(signal, sourceId)
        val raw = calcSignal.points

        // Stage 2: Optional smoothing (§2.8 Savitzky-Golay)
        val smoothed: List<SignalPoint>? = if (params.smoothingEnabled && raw.size >= params.smoothingWindowSize) {
            SavitzkyGolayFilter.smooth(
                points = raw,
                windowSize = params.smoothingWindowSize,
                polynomialOrder = params.smoothingPolynomialOrder,
            )
        } else null

        // Stage 3: Baseline estimation (§2.9)
        val baselineMethod = BaselineMethod.entries.find { it.name == params.baselineMethod }
            ?: BaselineMethod.NONE
        val baselineResult = BaselineDispatcher.estimate(baselineMethod, raw)
        val baseline: List<Double>? = if (baselineMethod != BaselineMethod.NONE) {
            baselineResult.baseline
        } else null

        // Stage 4: Build signal bundle (baseline correction happens here)
        val signals = SignalModelBuilder.build(
            raw = raw,
            smoothed = smoothed,
            baseline = baseline,
            useSmoothedForIntegration = params.useSmoothedForIntegration,
        )

        // Stage 5: Noise estimation
        // Placeholder — real implementation in §2.14

        // Stage 6: Peak detection
        // Placeholder — real implementation in §2.16

        // Stage 7: Peak boundary detection
        // Placeholder — real implementation in §2.17

        // Stage 8: Peak integration
        // Placeholder — real implementation in §2.19

        // Stage 9: Peak metric calculation
        // Placeholder — real implementation in §2.20

        // Stage 10: Warnings / confidence
        val warnings = buildWarnings(validation, params)

        // Stages 11-12: Manual corrections applied in recalculation
        val peaks = emptyList<PeakResult>()

        return CalculationRun(
            id = runId,
            sourceSignalId = sourceId,
            pipelineVersion = CalculationPipeline.PIPELINE_VERSION,
            algorithmVersion = CalculationPipeline.ALGORITHM_VERSION,
            params = params,
            validation = validation,
            signals = signals,
            peaks = peaks,
            warnings = warnings,
            manualEditsCsv = "",
            timestamp = currentTimeMillis(),
        )
    }

    /**
     * Recalculate with new parameters — creates a NEW CalculationRun.
     * The old run is NEVER overwritten.
     */
    fun recalculate(
        signal: DigitalSignal,
        sourceId: String,
        newParams: CalculationParams,
        manualEdits: List<ManualEdit> = emptyList(),
    ): CalculationRun {
        return execute(
            signal = signal,
            sourceId = sourceId,
            params = newParams,
            manualEdits = manualEdits,
            runId = generateRunId(),
        )
    }

    private fun buildWarnings(
        validation: ValidationResult,
        params: CalculationParams,
    ): List<CalculationWarning> {
        val warnings = mutableListOf<CalculationWarning>()

        // Validation warnings
        validation.warnings.forEach { msg ->
            warnings.add(
                CalculationWarning(
                    message = msg,
                    severity = WarningSeverity.CAUTION,
                    stage = PipelineStage.INPUT_VALIDATION.label,
                )
            )
        }

        // Smoothing warnings
        if (params.smoothingEnabled) {
            warnings.add(
                CalculationWarning(
                    message = "Сглаживание включено — высота и ширина пиков могут отличаться от исходных",
                    severity = WarningSeverity.INFO,
                    stage = PipelineStage.SMOOTHING.label,
                )
            )
        }

        return warnings
    }

    private fun generateRunId(): String {
        return "run_${currentTimeMillis()}"
    }
}

/**
 * Platform time — returns epoch millis.
 * Defined in commonMain, implemented per platform.
 */
expect fun currentTimeMillis(): Long

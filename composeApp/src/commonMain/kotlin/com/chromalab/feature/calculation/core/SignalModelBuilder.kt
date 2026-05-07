package com.chromalab.feature.calculation.core

/**
 * Signal model manager — builds and manages derived signals.
 *
 * Key rules (§2.6):
 * - Raw signal is NEVER modified
 * - All derived signals stored separately
 * - Default detection: smoothed baseline-corrected signal
 * - Default integration: raw - baseline (NOT smoothed)
 * - Smoothed signal for final area only if explicitly enabled
 * - UI always shows which signal is displayed
 * - Report stores which signal was used for detection & integration
 */
object SignalModelBuilder {

    /**
     * Build a SignalBundle from the raw signal, optionally adding smoothed & baseline layers.
     *
     * @param raw           The validated raw signal points
     * @param smoothed      Smoothed signal (null if smoothing disabled)
     * @param baseline      Estimated baseline values (one per point)
     * @param useSmoothedForIntegration  If true, use smoothed signal for integration (NOT default)
     * @return SignalBundle with all derived signals and source tracking
     */
    fun build(
        raw: List<SignalPoint>,
        smoothed: List<SignalPoint>? = null,
        baseline: List<Double>? = null,
        useSmoothedForIntegration: Boolean = false,
    ): SignalBundle {
        // Baseline-corrected raw signal
        val baselineCorrected = if (baseline != null && baseline.size == raw.size) {
            raw.mapIndexed { i, p ->
                p.copy(intensity = p.intensity - baseline[i])
            }
        } else null

        // Detection signal: prefer smoothed + corrected
        val detectionSource = when {
            smoothed != null && baselineCorrected != null -> SignalSource.SMOOTHED_BASELINE_CORRECTED
            baselineCorrected != null -> SignalSource.BASELINE_CORRECTED
            smoothed != null -> SignalSource.SMOOTHED
            else -> SignalSource.RAW
        }

        // Integration signal: raw - baseline by default (NOT smoothed)
        val integrationSource = when {
            useSmoothedForIntegration && smoothed != null && baseline != null ->
                SignalSource.SMOOTHED_BASELINE_CORRECTED
            baselineCorrected != null -> SignalSource.BASELINE_CORRECTED
            else -> SignalSource.RAW
        }

        return SignalBundle(
            raw = raw,
            smoothed = smoothed,
            baseline = baseline,
            baselineCorrected = baselineCorrected,
            signalUsedForDetection = detectionSource,
            signalUsedForIntegration = integrationSource,
        )
    }

    /**
     * Get the actual signal points for a given source.
     * Used by chart UI to display the correct layer,
     * and by algorithms to pick the right data.
     */
    fun getSignal(bundle: SignalBundle, source: SignalSource): List<SignalPoint> {
        return when (source) {
            SignalSource.RAW -> bundle.raw
            SignalSource.SMOOTHED -> bundle.smoothed ?: bundle.raw
            SignalSource.BASELINE_CORRECTED -> bundle.baselineCorrected ?: bundle.raw
            SignalSource.SMOOTHED_BASELINE_CORRECTED -> {
                // Smoothed + baseline corrected: apply baseline to smoothed
                val smoothed = bundle.smoothed ?: bundle.raw
                val bl = bundle.baseline
                if (bl != null && bl.size == smoothed.size) {
                    smoothed.mapIndexed { i, p -> p.copy(intensity = p.intensity - bl[i]) }
                } else {
                    bundle.baselineCorrected ?: smoothed
                }
            }
        }
    }

    /**
     * Human-readable label for the signal source (for UI display).
     */
    fun sourceLabel(source: SignalSource): String = when (source) {
        SignalSource.RAW -> "Исходный сигнал"
        SignalSource.SMOOTHED -> "Сглаженный сигнал"
        SignalSource.BASELINE_CORRECTED -> "Скорректированный (raw − baseline)"
        SignalSource.SMOOTHED_BASELINE_CORRECTED -> "Сглаженный скорректированный"
    }
}

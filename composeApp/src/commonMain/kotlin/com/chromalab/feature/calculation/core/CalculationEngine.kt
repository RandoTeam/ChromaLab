package com.chromalab.feature.calculation.core

import com.chromalab.feature.calculation.algorithm.*
import com.chromalab.feature.processing.signal.DigitalSignal
import kotlin.math.abs

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
        val baselineMethod = parseBaselineMethod(params.baselineMethod)
        val boundaryMethod = parseBoundaryMethod(params.boundaryMethod)
        val integrationMethod = parseIntegrationMethod(params.integrationMethod)
        val maxPeakWidth = params.maxPeakWidth.takeIf { it > 0 && it < Int.MAX_VALUE } ?: 0
        val baselineResult = estimateBaseline(baselineMethod, params, raw)
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

        // Pick the right signal arrays for detection and integration
        val detectionPoints = SignalModelBuilder.getSignal(signals, signals.signalUsedForDetection)
        val integrationPoints = SignalModelBuilder.getSignal(signals, signals.signalUsedForIntegration)

        // Stage 5: Noise estimation (§2.14)
        val noiseMethod = parseNoiseMethod(params.noiseMethod)
        val noiseResult = NoiseEstimator.estimate(
            points = detectionPoints,
            method = noiseMethod,
        )

        // Stage 6: Peak detection (§2.16)
        val detection = PeakDetector.detect(
            points = detectionPoints,
            minHeight = params.minPeakHeight,
            minProminence = params.minPeakProminence,
            minDistance = params.minPeakDistance,
            minWidth = params.minPeakWidth,
            maxWidth = maxPeakWidth,
            noiseLevel = noiseResult.noiseValue,
            noiseK = params.minSnr,
        )

        // Stage 7: Peak boundary detection (§2.17)
        val candidates = detection.accepted.sortedBy { it.apexTime }
        val neighborIndices = candidates.map { it.index }
        val boundaries = candidates.map { candidate ->
            PeakBoundaryDetector.detect(
                points = integrationPoints,
                peakIndex = candidate.index,
                method = boundaryMethod,
                percentHeight = params.boundaryPercentHeight.coerceIn(0.001, 1.0),
                neighborPeaks = neighborIndices,
            )
        }

        // Stage 7.5: Overlap classification (§2.18)
        val overlaps = PeakOverlapClassifier.classify(
            points = detectionPoints,
            peaks = candidates,
            boundaries = boundaries,
        )

        // Stage 8: Peak integration (§2.19)
        val integrations = boundaries.map { boundary ->
            when (integrationMethod) {
                IntegrationMethod.TRAPEZOIDAL -> PeakIntegrator.integrate(
                    points = integrationPoints,
                    leftIndex = boundary.leftIndex,
                    rightIndex = boundary.rightIndex,
                    clampNegative = params.clampNegative,
                )
                IntegrationMethod.TRAPEZOIDAL_INTERPOLATED -> PeakIntegrator.integrateInterpolated(
                    points = integrationPoints,
                    leftTime = boundary.leftTime,
                    rightTime = boundary.rightTime,
                    clampNegative = params.clampNegative,
                )
            }
        }

        // Total area for area% calculation
        val totalArea = integrations.sumOf { abs(it.totalArea) }.coerceAtLeast(1e-15)

        // Stage 9: Peak metrics + confidence → PeakResult (§2.20)
        val peaks = candidates.mapIndexed { idx, candidate ->
            val boundary = boundaries[idx]
            val integration = integrations[idx]
            val overlap = overlaps.getOrNull(idx)

            // S/N for this peak
            val snr = SnrCalculator.calculate(
                peakHeight = candidate.apexIntensity,
                noiseResult = noiseResult,
            )

            // Full metrics
            val metrics = PeakMetricsCalculator.calculate(
                points = integrationPoints,
                peak = candidate,
                boundary = boundary,
                integration = integration,
                snr = snr,
                overlap = overlap,
            )

            // Confidence grading
            val negFraction = if (integration.positiveArea > 0) {
                abs(integration.negativeArea) / integration.positiveArea
            } else 0.0
            val confidence = PeakConfidenceCalculator.calculate(
                metrics = metrics,
                negativeAreaFraction = negFraction,
            )

            // Resolution to previous peak
            val resolution = if (idx > 0) {
                val prevBoundary = boundaries[idx - 1]
                PeakMetricsCalculator.calculateResolution(
                    rt1 = candidates[idx - 1].apexTime,
                    widthBase1 = prevBoundary.widthTime,
                    rt2 = candidate.apexTime,
                    widthBase2 = boundary.widthTime,
                )
            } else null

            PeakResult(
                peakId = idx,
                status = PeakStatus.AUTO,
                rtApex = metrics.rtApex,
                rtCentroid = metrics.rtCentroid,
                height = metrics.height,
                area = metrics.area,
                widthBase = metrics.widthBase,
                widthHalfHeight = metrics.widthHalfHeight,
                prominence = metrics.prominence,
                snr = metrics.snrValue,
                snrMethod = snr.method.label,
                baselineMethod = baselineMethod.name,
                integrationMethod = integration.method.name,
                confidence = confidence.grade,
                overlapStatus = metrics.overlapStatus,
                leftBoundaryTime = metrics.leftBaseTime,
                rightBoundaryTime = metrics.rightBaseTime,
                boundaryMethod = boundary.method.name,
                warnings = metrics.warnings + confidence.reasons,
                tailingFactor = metrics.tailingFactor,
                asymmetryFactor = metrics.asymmetryFactor,
                plateCount = metrics.plateCount,
                resolution = resolution,
                areaPercent = if (totalArea > 0) abs(metrics.area) / totalArea * 100.0 else 0.0,
            )
        }

        // Stage 10: Warnings / confidence
        val warnings = buildWarnings(validation, params, noiseResult, detection, peaks)

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
        noiseResult: NoiseResult,
        detection: PeakDetectionResult,
        peaks: List<PeakResult>,
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

        // Noise warnings
        noiseResult.warnings.forEach { msg ->
            warnings.add(
                CalculationWarning(
                    message = msg,
                    severity = WarningSeverity.INFO,
                    stage = PipelineStage.NOISE_ESTIMATION.label,
                )
            )
        }

        // Peak detection summary
        if (peaks.isEmpty() && detection.totalCandidates > 0) {
            warnings.add(
                CalculationWarning(
                    message = "Обнаружено ${detection.totalCandidates} кандидатов, но все отклонены фильтрами",
                    severity = WarningSeverity.CAUTION,
                    stage = PipelineStage.PEAK_DETECTION.label,
                )
            )
        } else if (peaks.isEmpty()) {
            warnings.add(
                CalculationWarning(
                    message = "Пики не обнаружены — проверьте сигнал и параметры",
                    severity = WarningSeverity.SERIOUS,
                    stage = PipelineStage.PEAK_DETECTION.label,
                )
            )
        }

        // Low-confidence peak warnings
        val lowConfPeaks = peaks.filter { it.confidence == ConfidenceGrade.LOW || it.confidence == ConfidenceGrade.FAILED }
        if (lowConfPeaks.isNotEmpty()) {
            warnings.add(
                CalculationWarning(
                    message = "${lowConfPeaks.size} пик(ов) с низкой уверенностью — проверьте вручную",
                    severity = WarningSeverity.CAUTION,
                    stage = PipelineStage.PEAK_METRICS.label,
                )
            )
        }

        return warnings
    }

    private fun generateRunId(): String {
        return "run_${currentTimeMillis()}"
    }

    private fun parseBaselineMethod(value: String): BaselineMethod {
        return BaselineMethod.entries.find { it.name.equals(value, ignoreCase = true) }
            ?: BaselineMethod.NONE
    }

    private fun parseNoiseMethod(value: String): NoiseMethod {
        val normalized = value.replace('-', '_').uppercase()
        return NoiseMethod.entries.find {
            it.name == normalized || it.label.equals(value, ignoreCase = true)
        } ?: NoiseMethod.PEAK_TO_PEAK
    }

    private fun parseBoundaryMethod(value: String): BoundaryMethod {
        val normalized = value.replace('-', '_').uppercase()
        return BoundaryMethod.entries.find {
            it.name == normalized || it.label.equals(value, ignoreCase = true)
        } ?: BoundaryMethod.LOCAL_MINIMA
    }

    private fun parseIntegrationMethod(value: String): IntegrationMethod {
        val normalized = value.replace('-', '_').uppercase()
        return IntegrationMethod.entries.find {
            it.name == normalized || it.label.equals(value, ignoreCase = true)
        } ?: IntegrationMethod.TRAPEZOIDAL
    }

    private fun estimateBaseline(
        method: BaselineMethod,
        params: CalculationParams,
        points: List<SignalPoint>,
    ): BaselineResult {
        val estimator = when (method) {
            BaselineMethod.NONE -> NoneBaselineEstimator
            BaselineMethod.MANUAL_LINEAR -> ManualLinearBaselineEstimator.auto()
            BaselineMethod.ALS -> AlsBaselineEstimator(
                lambda = params.baselineLambda,
                p = params.baselineP,
                maxIterations = params.baselineIterations,
            )
            BaselineMethod.SNIP -> SnipBaselineEstimator(
                iterations = params.baselineIterations,
                useLlsTransform = true,
            )
        }
        return estimator.estimate(points)
    }
}

/**
 * Platform time — returns epoch millis.
 * Defined in commonMain, implemented per platform.
 */
expect fun currentTimeMillis(): Long

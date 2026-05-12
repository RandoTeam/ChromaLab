package com.chromalab.feature.reports

import com.chromalab.feature.calculation.algorithm.CompoundSource
import com.chromalab.feature.calculation.algorithm.ConfidenceGrade
import com.chromalab.feature.calculation.algorithm.DistributionAnalyzer
import com.chromalab.feature.calculation.algorithm.DistributionResult
import com.chromalab.feature.calculation.algorithm.MethodQualityAnalyzer
import com.chromalab.feature.calculation.algorithm.MethodQualityResult
import com.chromalab.feature.calculation.core.CalculationParams
import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.calculation.core.CalculationWarning
import com.chromalab.feature.calculation.core.PeakResult
import com.chromalab.feature.calculation.core.SignalPoint
import com.chromalab.feature.calculation.core.WarningSeverity
import kotlin.math.abs
import kotlin.math.sqrt

data class CalculationRunReportOptions(
    val appVersion: String? = null,
    val inputSourceType: InputSourceType = InputSourceType.UNKNOWN,
    val sourceName: String? = null,
    val detectedGraphCount: Int = 1,
    val analysisStartedAtEpochMillis: Long? = null,
    val analysisCompletedAtEpochMillis: Long? = null,
    val totalAnalysisDurationMillis: Long? = null,
    val selectedModel: ModelExecutionInfo? = null,
    val executedModel: ModelExecutionInfo? = null,
    val executedRuntime: ExecutedRuntime = ExecutedRuntime.DETERMINISTIC,
    val deviceName: String? = null,
    val processingMode: ProcessingMode = ProcessingMode.EXPORT_ONLY,
    val stageTimings: List<ReportStageTiming> = emptyList(),
    val graphIndex: Int = 1,
    val graphSourceMetadata: GraphSourceMetadata? = null,
    val identification: ChromatogramIdentification? = null,
    val axisCalibration: ReportAxisCalibration? = null,
    val additionalReportWarnings: List<ReportWarning> = emptyList(),
    val additionalGraphWarnings: List<ReportWarning> = emptyList(),
)

/**
 * Bridges completed deterministic calculations into the strict chromatogram report contract.
 *
 * This mapper does not invent source-image, OCR, model, or Kovats values that are not present in
 * CalculationRun. Missing upstream data is rendered explicitly by ReportMarkdownRenderer and is
 * also surfaced as report warnings.
 */
object CalculationRunReportMapper {

    fun map(
        run: CalculationRun,
        options: CalculationRunReportOptions = CalculationRunReportOptions(),
    ): ChromatogramReport {
        val graphIndex = options.graphIndex.coerceAtLeast(1)
        val sortedPeaks = run.peaks.sortedBy { it.rtApex }
        val distribution = run.distribution ?: DistributionAnalyzer.analyze(sortedPeaks)
        val methodQuality = run.methodQuality ?: MethodQualityAnalyzer.analyze(sortedPeaks, run.signals)
        val graphWarnings = buildGraphWarnings(run, sortedPeaks, graphIndex, options)

        return ChromatogramReport(
            metadata = ReportMetadata(
                reportId = "calculation_${run.id}",
                appVersion = options.appVersion,
                analysisStartedAtEpochMillis = options.analysisStartedAtEpochMillis,
                analysisCompletedAtEpochMillis = options.analysisCompletedAtEpochMillis ?: run.timestamp,
                totalAnalysisDurationMillis = options.totalAnalysisDurationMillis,
                inputSourceType = options.inputSourceType,
                sourceName = options.sourceName ?: run.sourceSignalId,
                detectedGraphCount = options.detectedGraphCount.coerceAtLeast(1),
                selectedModel = options.selectedModel,
                executedModel = options.executedModel,
                executedRuntime = options.executedRuntime,
                deviceName = options.deviceName,
                processingMode = options.processingMode,
                stageTimings = options.stageTimings,
            ),
            graphs = listOf(
                GraphReport(
                    graphIndex = graphIndex,
                    source = buildSourceMetadata(run.params, options.graphSourceMetadata),
                    identification = options.identification ?: buildIdentification(options.sourceName ?: run.sourceSignalId),
                    axisCalibration = options.axisCalibration ?: buildAxisCalibration(run, graphIndex),
                    signal = buildSignalReport(run),
                    peaks = sortedPeaks.mapIndexed { index, peak ->
                        peak.toReportPeak(index + 1, run, graphIndex)
                    },
                    quality = buildQualityReport(sortedPeaks, run.params, distribution, methodQuality, graphWarnings),
                    kovats = buildKovatsReport(sortedPeaks),
                    interpretation = buildInterpretation(sortedPeaks, run, distribution),
                    sectionStatus = buildSectionStatus(run, sortedPeaks),
                    warnings = graphWarnings,
                ),
            ),
            warnings = buildReportWarnings(options, graphIndex),
        )
    }

    private fun buildSourceMetadata(
        params: CalculationParams,
        upstream: GraphSourceMetadata?,
    ): GraphSourceMetadata {
        val calculationSteps = buildCalculationPreprocessingSteps(params)
        return upstream?.copy(
            preprocessingSteps = (upstream.preprocessingSteps + calculationSteps).distinct(),
            scanMode = upstream.scanMode ?: "calculation-run-export",
        ) ?: GraphSourceMetadata(
            preprocessingSteps = calculationSteps,
            scanMode = "calculation-run-export",
        )
    }

    private fun buildCalculationPreprocessingSteps(params: CalculationParams): List<String> =
        buildList {
            add("CalculationRun report mapping")
            if (params.smoothingEnabled) {
                add("Smoothing: Savitzky-Golay")
            }
            add("Baseline: ${params.baselineMethod}")
            add("Peak detection and integration completed before report mapping")
        }

    private fun buildIdentification(sourceName: String): ChromatogramIdentification =
        ChromatogramIdentification(
            analysisType = ReportTextValue.notCalculated(),
            chromatogramMode = calculatedText("digitized signal calculation"),
            sampleName = calculatedText(sourceName),
            samplePathOrInstrumentLabel = calculatedText(sourceName),
        )

    private fun buildAxisCalibration(run: CalculationRun, graphIndex: Int): ReportAxisCalibration {
        val raw = run.signals.raw
        val timeMin = raw.minOfOrNull { it.time }
        val timeMax = raw.maxOfOrNull { it.time }
        val intensityMin = raw.minOfOrNull { it.intensity }
        val intensityMax = raw.maxOfOrNull { it.intensity }

        return ReportAxisCalibration(
            xAxis = AxisReport(
                label = calculatedText("Retention time"),
                unit = calculatedText("min"),
                visibleMinimum = calculatedDouble(timeMin, "min"),
                visibleMaximum = calculatedDouble(timeMax, "min"),
                majorTicks = buildTicks(timeMin, timeMax, "min"),
            ),
            yAxis = AxisReport(
                label = calculatedText("Intensity"),
                unit = calculatedText("a.u."),
                visibleMinimum = calculatedDouble(intensityMin, "a.u."),
                visibleMaximum = calculatedDouble(intensityMax, "a.u."),
                majorTicks = buildTicks(intensityMin, intensityMax, "a.u."),
            ),
        )
    }

    private fun buildSignalReport(run: CalculationRun): SignalAndBaselineReport {
        val baseline = run.signals.baseline
        val rmsNoise = estimateQuietWindowRms(run.signals.baselineCorrected ?: run.signals.raw)

        return SignalAndBaselineReport(
            pointCount = run.signals.raw.size,
            smoothingMethod = if (run.params.smoothingEnabled) "Savitzky-Golay" else "off",
            smoothingParameters = if (run.params.smoothingEnabled) {
                mapOf(
                    "window" to run.params.smoothingWindowSize.toString(),
                    "polynomialOrder" to run.params.smoothingPolynomialOrder.toString(),
                )
            } else {
                emptyMap()
            },
            baselineMethod = run.params.baselineMethod,
            baselineParameters = buildBaselineParameters(run.params),
            baselineMean = calculatedDouble(baseline?.takeIf { it.isNotEmpty() }?.average(), "a.u."),
            baselineDrift = calculatedDouble(baseline?.takeIf { it.size >= 2 }?.let { it.last() - it.first() }, "a.u."),
            rmsNoise = calculatedDouble(rmsNoise, "a.u."),
            noiseMethod = if (rmsNoise != null) {
                "${run.params.noiseMethod}; quiet-window RMS estimate"
            } else {
                run.params.noiseMethod
            },
            correctedSignalAvailable = run.signals.baselineCorrected != null,
        )
    }

    private fun buildQualityReport(
        peaks: List<PeakResult>,
        params: CalculationParams,
        distribution: DistributionResult?,
        methodQuality: MethodQualityResult?,
        graphWarnings: List<ReportWarning>,
    ): ChromatographicQualityReport {
        val snrs = peaks.mapNotNull { it.snr.takeIfUsable() }
        val heights = peaks.mapNotNull { it.height.takeIfUsable() }
        val areas = peaks.mapNotNull { abs(it.area).takeIfUsable() }
        val resolutions = peaks.mapNotNull { it.resolution?.takeIfUsable() }
        val plates = peaks.mapNotNull { it.plateCount?.toDouble()?.takeIfUsable() }
        val totalArea = areas.sum().takeIf { it > 0.0 }
        val dominantPeakNumber = distribution?.dominantPeak?.peakIndex?.plus(1)
            ?: peaks.indices.maxByOrNull { abs(peaks[it].area) }?.plus(1)

        return ChromatographicQualityReport(
            totalDetectedPeaks = peaks.size,
            significantPeakCount = peaks.count { it.snr >= params.minSnr },
            significantPeakSnrThreshold = params.minSnr,
            meanSnr = calculatedDouble(snrs.takeIf { it.isNotEmpty() }?.average()),
            medianSnr = calculatedDouble(snrs.medianOrNull()),
            maximumPeakHeight = calculatedDouble(heights.maxOrNull(), "a.u."),
            dominantPeakNumber = dominantPeakNumber,
            baselineQuality = methodQuality?.let { calculatedText(it.grade.name) } ?: ReportTextValue.notCalculated(),
            averageResolution = calculatedDouble(resolutions.takeIf { it.isNotEmpty() }?.average()),
            minimumResolution = calculatedDouble(resolutions.minOrNull()),
            theoreticalPlates = calculatedDouble(plates.takeIf { it.isNotEmpty() }?.average()),
            globalIntegratedArea = calculatedDouble(totalArea),
            areaNormalizationStatus = totalArea?.let {
                calculatedText("normalized by absolute integrated area")
            } ?: ReportTextValue.notCalculated(),
            anomalies = graphWarnings
                .filter { it.severity == ReportSeverity.SERIOUS || it.severity == ReportSeverity.FAILED }
                .map {
                    ReportAnomaly(
                        code = it.code,
                        message = it.message,
                        peakNumber = it.peakNumber,
                        severity = it.severity,
                    )
                },
        )
    }

    private fun buildKovatsReport(peaks: List<PeakResult>): KovatsIndexReport =
        KovatsIndexReport(
            status = ReportValueStatus.NOT_CALCULATED,
            results = peaks.mapNotNull { peak ->
                peak.compoundName?.takeIf { it.isNotBlank() }?.let { name ->
                    KovatsIndexResult(
                        peakNumber = peak.peakId + 1,
                        compoundName = name,
                        retentionTime = peak.rtApex,
                        calculationKind = KovatsCalculationKind.NOT_CALCULABLE,
                    )
                }
            },
            missingDataNotes = listOf(
                "CalculationRun does not contain n-paraffin reference retention times, literature ranges, or a local Kovats knowledge-pack result yet.",
            ),
        )

    private fun buildInterpretation(
        peaks: List<PeakResult>,
        run: CalculationRun,
        distribution: DistributionResult?,
    ): ChemicalInterpretationReport {
        val totalArea = peaks.sumOf { abs(it.area) }
        val distributionBuckets = run.geochemistry?.let { geochemistry ->
            peaks.sortedBy { it.rtApex }.mapIndexed { index, peak ->
                val carbonNumber = geochemistry.firstCarbonNumber + index
                val area = abs(peak.area)
                DistributionBucket(
                    label = "C$carbonNumber",
                    area = calculatedDouble(area),
                    areaPercent = calculatedDouble(if (totalArea > 0.0) area / totalArea * 100.0 else null, "%"),
                    peakCount = 1,
                )
            }
        } ?: emptyList()

        val notes = buildList {
            distribution?.let {
                add("Dominant peak #${it.dominantPeak.peakIndex + 1} contributes ${"%.2f".format(it.dominantPeak.areaPercent)}% of the integrated area.")
            }
            run.geochemistry?.let {
                add("Geochemical index calculation is attached to the run for C${it.firstCarbonNumber}-C${it.lastCarbonNumber}.")
            }
        }

        val hasCompoundAssignments = peaks.any { !it.compoundName.isNullOrBlank() }

        return ChemicalInterpretationReport(
            distributionByCarbonNumber = distributionBuckets,
            likelyCompoundClass = if (hasCompoundAssignments) {
                inferredText("compound assignments present in CalculationRun", 0.5, ReportValueSource.LOCAL_KNOWLEDGE)
            } else {
                ReportTextValue.notCalculated()
            },
            domainContextNotes = notes,
            unresolvedAssignments = if (hasCompoundAssignments) {
                emptyList()
            } else {
                listOf("No compound assignment source is attached to this CalculationRun.")
            },
        )
    }

    private fun buildSectionStatus(
        run: CalculationRun,
        peaks: List<PeakResult>,
    ): ReportSectionStatus =
        ReportSectionStatus(
            overview = ReportValueStatus.INFERRED,
            graphPreparation = ReportValueStatus.NOT_CALCULATED,
            axisCalibration = if (run.signals.raw.isNotEmpty()) ReportValueStatus.INFERRED else ReportValueStatus.FAILED,
            peakTable = if (peaks.isNotEmpty()) ReportValueStatus.CALCULATED else ReportValueStatus.FAILED,
            graphOverlay = if (run.signals.raw.isNotEmpty()) ReportValueStatus.CALCULATED else ReportValueStatus.FAILED,
            chromatographicQuality = if (peaks.isNotEmpty()) ReportValueStatus.CALCULATED else ReportValueStatus.FAILED,
            kovatsAnalysis = ReportValueStatus.NOT_CALCULATED,
            chemicalInterpretation = if (run.geochemistry != null || peaks.any { !it.compoundName.isNullOrBlank() }) {
                ReportValueStatus.INFERRED
            } else {
                ReportValueStatus.NOT_CALCULATED
            },
            technicalAppendix = ReportValueStatus.CALCULATED,
        )

    private fun PeakResult.toReportPeak(number: Int, run: CalculationRun, graphIndex: Int): ReportPeak {
        val apex = run.signals.raw.nearestSample(rtApex)
        val baselineAtApex = apex?.position?.let { run.signals.baseline?.getOrNull(it) }
        val areaPercentValue = areaPercent.takeIf { it > 0.0 } ?: run.peaks.areaPercentFor(this)

        return ReportPeak(
            number = number,
            retentionTime = calculatedDouble(rtApex, "min"),
            apexPixel = apex?.point?.let { PixelPoint(it.pixelX, it.pixelY) },
            absoluteApexIntensity = calculatedDouble(apex?.point?.intensity, "a.u."),
            baselineAtApex = calculatedDouble(baselineAtApex, "a.u."),
            heightAboveBaseline = calculatedDouble(height, "a.u."),
            startRetentionTime = calculatedDouble(leftBoundaryTime, "min"),
            endRetentionTime = calculatedDouble(rightBoundaryTime, "min"),
            widthAtBase = calculatedDouble(widthBase, "min"),
            fwhm = calculatedDouble(widthHalfHeight, "min"),
            integratedArea = calculatedDouble(area),
            areaPercent = calculatedDouble(areaPercentValue, "%"),
            signalToNoise = calculatedDouble(snr),
            asymmetry = calculatedDouble(asymmetryFactor),
            overlapClass = calculatedText(overlapStatus.name),
            boundaryMethod = boundaryMethod,
            integrationMethod = integrationMethod,
            confidence = confidence.toNumericConfidence(),
            compound = toCompoundAssignment(),
            flags = buildList {
                if (warnings.isNotEmpty()) addAll(warnings)
                if (confidence == ConfidenceGrade.LOW || confidence == ConfidenceGrade.FAILED) {
                    add("low confidence")
                }
            },
            warnings = warnings.map {
                ReportWarning(
                    code = "calculation_run.peak_warning",
                    message = it,
                    severity = ReportSeverity.WARNING,
                    stage = "peak_metrics",
                    graphIndex = graphIndex,
                    peakNumber = number,
                )
            },
        )
    }

    private fun PeakResult.toCompoundAssignment(): CompoundAssignment? {
        if (compoundName.isNullOrBlank() && compoundSource == CompoundSource.NONE) {
            return null
        }

        val source = when (compoundSource) {
            CompoundSource.MANUAL -> ReportValueSource.USER
            CompoundSource.TEMPLATE,
            CompoundSource.AUTO_SERIES -> ReportValueSource.LOCAL_KNOWLEDGE
            CompoundSource.NONE -> ReportValueSource.UNKNOWN
        }

        return CompoundAssignment(
            probableName = compoundName?.takeIf { it.isNotBlank() }?.let {
                inferredText(it, compoundSource.assignmentConfidence(), source)
            } ?: ReportTextValue.notCalculated(),
            assignmentConfidence = compoundSource.assignmentConfidence(),
            assignmentBasis = compoundSource.name,
        )
    }

    private fun buildReportWarnings(
        options: CalculationRunReportOptions,
        graphIndex: Int,
    ): List<ReportWarning> = buildList {
        if (!options.hasRuntimeMetadata()) {
            add(
                ReportWarning(
                    code = "calculation_run.runtime_metadata_missing",
                    message = "This report was generated without selected/executed model metadata; only the default deterministic runtime is recorded.",
                    severity = ReportSeverity.INFO,
                    stage = "report_mapping",
                    graphIndex = graphIndex,
                ),
            )
        }

        val selectedRuntime = options.selectedModel?.runtime
        val executedRuntime = options.executedModel?.runtime ?: options.executedRuntime
        if (selectedRuntime != null &&
            selectedRuntime != ExecutedRuntime.UNKNOWN &&
            executedRuntime != ExecutedRuntime.UNKNOWN &&
            selectedRuntime != executedRuntime
        ) {
            add(
                ReportWarning(
                    code = "metadata.model_runtime_mismatch",
                    message = "Selected model runtime differs from the executed runtime.",
                    severity = ReportSeverity.WARNING,
                    stage = "metadata",
                    graphIndex = graphIndex,
                ),
            )
        }

        addAll(options.additionalReportWarnings)
    }

    private fun buildGraphWarnings(
        run: CalculationRun,
        peaks: List<PeakResult>,
        graphIndex: Int,
        options: CalculationRunReportOptions,
    ): List<ReportWarning> = buildList {
        if (!options.hasGraphSourceMetadata()) {
            add(
                ReportWarning(
                    code = "calculation_run.graph_source_metadata_missing",
                    message = "Detected graph bounds, crop confidence, OCR confidence, and original preprocessing filters are not available in the report options.",
                    severity = ReportSeverity.INFO,
                    stage = "graph_preparation",
                    graphIndex = graphIndex,
                ),
            )
        }

        if (options.axisCalibration == null) {
            add(
                ReportWarning(
                    code = "calculation_run.axis_upstream_metadata_missing",
                    message = "Axis calibration was inferred from the digitized CalculationRun signal; original OCR calibration confidence is not available in the report options.",
                    severity = ReportSeverity.INFO,
                    stage = "axis_calibration",
                    graphIndex = graphIndex,
                ),
            )
        }

        if (options.identification == null) {
            add(
                ReportWarning(
                    code = "calculation_run.identification_metadata_missing",
                    message = "Chromatogram title, ion/channel, and sample identity were not passed from upstream OCR or model extraction.",
                    severity = ReportSeverity.INFO,
                    stage = "identification",
                    graphIndex = graphIndex,
                ),
            )
        }

        if (!options.hasGraphSourceMetadata() && !options.hasRuntimeMetadata()) {
            add(
                ReportWarning(
                    code = "calculation_run.report_scope",
                    message = "This report was generated from CalculationRun data only; source-image crop, OCR, and neural model-stage metadata were not supplied.",
                    severity = ReportSeverity.INFO,
                    stage = "report_mapping",
                    graphIndex = graphIndex,
                ),
            )
        }

        if (run.signals.raw.isEmpty()) {
            add(
                ReportWarning(
                    code = "calculation_run.signal_empty",
                    message = "CalculationRun contains no raw signal points.",
                    severity = ReportSeverity.FAILED,
                    stage = "signal",
                    graphIndex = graphIndex,
                ),
            )
        }

        if (peaks.isEmpty()) {
            add(
                ReportWarning(
                    code = "calculation_run.peaks_empty",
                    message = "CalculationRun contains no detected peaks.",
                    severity = ReportSeverity.SERIOUS,
                    stage = "peak_detection",
                    graphIndex = graphIndex,
                ),
            )
        }

        if (peaks.none { !it.compoundName.isNullOrBlank() }) {
            add(
                ReportWarning(
                    code = "calculation_run.compounds_missing",
                    message = "No compound assignments are attached to this CalculationRun.",
                    severity = ReportSeverity.INFO,
                    stage = "chemical_interpretation",
                    graphIndex = graphIndex,
                ),
            )
        }

        run.warnings.forEach { add(it.toReportWarning(graphIndex)) }
        addAll(options.additionalGraphWarnings)
    }

    private fun CalculationRunReportOptions.hasRuntimeMetadata(): Boolean =
        selectedModel != null ||
            executedModel != null ||
            (executedRuntime != ExecutedRuntime.DETERMINISTIC && executedRuntime != ExecutedRuntime.UNKNOWN)

    private fun CalculationRunReportOptions.hasGraphSourceMetadata(): Boolean {
        val source = graphSourceMetadata ?: return false
        return source.sourceImageBounds != null ||
            source.detectedGraphBounds != null ||
            source.cropConfidence != null ||
            source.preprocessingSteps.isNotEmpty() ||
            source.selectedPreparationVariant != null ||
            source.rejectedPreparationVariants.isNotEmpty() ||
            source.scanMode != null ||
            source.titleOcrConfidence != null ||
            source.axisOcrConfidence != null ||
            source.tickOcrConfidence != null ||
            source.manuallyAdjusted
    }

    private fun CalculationWarning.toReportWarning(graphIndex: Int): ReportWarning =
        ReportWarning(
            code = "calculation_run.${stage.lowercase().replace(' ', '_')}",
            message = message,
            severity = severity.toReportSeverity(),
            stage = stage,
            graphIndex = graphIndex,
            peakNumber = peakId?.plus(1),
        )

    private fun WarningSeverity.toReportSeverity(): ReportSeverity =
        when (this) {
            WarningSeverity.INFO -> ReportSeverity.INFO
            WarningSeverity.CAUTION -> ReportSeverity.WARNING
            WarningSeverity.SERIOUS -> ReportSeverity.SERIOUS
            WarningSeverity.FAILED -> ReportSeverity.FAILED
        }

    private fun ConfidenceGrade.toNumericConfidence(): Double =
        when (this) {
            ConfidenceGrade.HIGH -> 0.95
            ConfidenceGrade.MEDIUM -> 0.70
            ConfidenceGrade.LOW -> 0.40
            ConfidenceGrade.FAILED -> 0.0
        }

    private fun CompoundSource.assignmentConfidence(): Double? =
        when (this) {
            CompoundSource.MANUAL -> 1.0
            CompoundSource.TEMPLATE -> 0.75
            CompoundSource.AUTO_SERIES -> 0.65
            CompoundSource.NONE -> null
        }

    private fun buildBaselineParameters(params: CalculationParams): Map<String, String> =
        buildMap {
            put("lambda", params.baselineLambda.toString())
            put("p", params.baselineP.toString())
            put("iterations", params.baselineIterations.toString())
            put("clampNegative", params.clampNegative.toString())
            put("boundaryMethod", params.boundaryMethod)
            put("boundaryPercentHeight", params.boundaryPercentHeight.toString())
            put("integrationMethod", params.integrationMethod)
            put("maxPeakWidth", params.maxPeakWidth.toString())
        }

    private fun buildTicks(minimum: Double?, maximum: Double?, unit: String): List<ReportDoubleValue> {
        val min = minimum.takeIfUsable() ?: return emptyList()
        val max = maximum.takeIfUsable() ?: return emptyList()
        if (max <= min) {
            return emptyList()
        }
        return (0..5).map { index ->
            calculatedDouble(min + (max - min) * index / 5.0, unit)
        }
    }

    private fun estimateQuietWindowRms(points: List<SignalPoint>): Double? {
        if (points.size < 10) return null

        val windowSize = (points.size / 20).coerceIn(10, 200).coerceAtMost(points.size)
        val step = (windowSize / 2).coerceAtLeast(1)
        var bestRms: Double? = null
        var start = 0

        while (start + windowSize <= points.size) {
            val values = points.subList(start, start + windowSize).map { it.intensity }
            val mean = values.average()
            val rms = sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
            if (bestRms == null || rms < bestRms) {
                bestRms = rms
            }
            start += step
        }

        return bestRms
    }

    private fun List<SignalPoint>.nearestSample(time: Double): SignalSample? =
        withIndex().minByOrNull { abs(it.value.time - time) }?.let { SignalSample(it.value, it.index) }

    private fun List<PeakResult>.areaPercentFor(peak: PeakResult): Double? {
        val totalArea = sumOf { abs(it.area) }
        return if (totalArea > 0.0) abs(peak.area) / totalArea * 100.0 else null
    }

    private fun List<Double>.medianOrNull(): Double? {
        if (isEmpty()) return null
        val sorted = sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    private fun calculatedText(
        value: String,
        confidence: Double? = null,
        source: ReportValueSource = ReportValueSource.DETERMINISTIC,
    ): ReportTextValue = ReportTextValue.calculated(value, confidence, source)

    private fun inferredText(
        value: String,
        confidence: Double? = null,
        source: ReportValueSource = ReportValueSource.UNKNOWN,
    ): ReportTextValue = ReportTextValue(value, ReportValueStatus.INFERRED, confidence, source)

    private fun calculatedDouble(
        value: Double?,
        unit: String? = null,
        confidence: Double? = null,
    ): ReportDoubleValue {
        val usableValue = value.takeIfUsable()
        return if (usableValue != null) {
            ReportDoubleValue.calculated(usableValue, unit, confidence)
        } else {
            ReportDoubleValue.notCalculated(unit)
        }
    }

    private fun Double?.isUsable(): Boolean =
        this != null && !isNaN() && !isInfinite()

    private fun Double?.takeIfUsable(): Double? =
        if (isUsable()) this else null

    private fun Double.takeIfUsable(): Double? =
        if (!isNaN() && !isInfinite()) this else null

    private data class SignalSample(
        val point: SignalPoint,
        val position: Int,
    )
}

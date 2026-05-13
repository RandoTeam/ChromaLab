package com.chromalab.feature.reports

import com.chromalab.feature.calculation.algorithm.ConfidenceGrade
import com.chromalab.feature.calculation.algorithm.OverlapStatus
import com.chromalab.feature.calculation.core.CalculationParams
import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.calculation.core.PeakResult
import com.chromalab.feature.calculation.core.PeakStatus
import com.chromalab.feature.calculation.core.SignalBundle
import com.chromalab.feature.calculation.core.SignalPoint
import com.chromalab.feature.calculation.core.SignalSource
import com.chromalab.feature.calculation.core.ValidationResult
import com.chromalab.feature.reports.fixtures.BelyiTigrIon92ReportFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportWarningRuleEngineTest {

    @Test
    fun ruleEngineFlagsGraphQualityAndPeakRisks() {
        val base = BelyiTigrIon92ReportFixture.buildReport()
        val graph = base.graphs.single()
        val unresolvedDominantPeak = graph.peaks.first().copy(
            areaPercent = ReportDoubleValue.calculated(65.0, "%"),
            overlapClass = ReportTextValue.calculated("UNRESOLVED"),
        )
        val report = ReportWarningRuleEngine.apply(
            base.copy(
                graphs = listOf(
                    graph.copy(
                        source = graph.source.copy(cropConfidence = 0.64),
                        signal = graph.signal.copy(
                            correctedSignalAvailable = false,
                            rmsNoise = ReportDoubleValue.calculated(2_100.0, "counts"),
                            baselineDrift = ReportDoubleValue.calculated(1_700.0, "counts"),
                        ),
                        peaks = listOf(unresolvedDominantPeak) + graph.peaks.drop(1),
                        quality = graph.quality.copy(
                            maximumPeakHeight = ReportDoubleValue.calculated(9_500.0, "counts"),
                            baselineQuality = ReportTextValue.calculated("POOR"),
                            minimumResolution = ReportDoubleValue.calculated(0.72),
                            anomalies = emptyList(),
                        ),
                    ),
                ),
            ),
        )

        val warnings = report.graphs.single().warnings
        assertHasWarning(warnings, "graph.crop_confidence_low", ReportSeverity.SERIOUS)
        assertHasWarning(warnings, "baseline.quality_poor", ReportSeverity.SERIOUS)
        assertHasWarning(warnings, "baseline.correction_missing", ReportSeverity.WARNING)
        assertHasWarning(warnings, "baseline.noise_high", ReportSeverity.SERIOUS)
        assertHasWarning(warnings, "baseline.drift_high", ReportSeverity.SERIOUS)
        assertHasWarning(warnings, "separation.minimum_resolution_low", ReportSeverity.SERIOUS)
        assertHasWarning(warnings, "peak.coelution_suspected", ReportSeverity.SERIOUS)
        assertHasWarning(warnings, "peak.dominant_peak_review", ReportSeverity.SERIOUS)
        assertTrue(report.graphs.single().quality.anomalies.any { it.code == "peak.coelution_suspected" })
    }

    @Test
    fun ruleEngineFlagsUnsupportedOrMismatchedRuntimeForFullAnalysis() {
        val base = BelyiTigrIon92ReportFixture.buildReport()
        val ggufSelectedLiteRtExecuted = ReportWarningRuleEngine.apply(
            base.copy(
                metadata = base.metadata.copy(
                    selectedModel = ModelExecutionInfo(
                        modelId = "tiny-gguf-vlm",
                        modelName = "Tiny GGUF VLM",
                        runtime = ExecutedRuntime.GGUF,
                    ),
                    executedModel = ModelExecutionInfo(
                        modelId = "gemma-litert",
                        modelName = "Gemma LiteRT",
                        runtime = ExecutedRuntime.LITERT,
                    ),
                    executedRuntime = ExecutedRuntime.LITERT,
                    processingMode = ProcessingMode.FULL_ANALYSIS,
                ),
            ),
        )
        assertHasWarning(
            ggufSelectedLiteRtExecuted.warnings,
            "runtime.selected_gguf_not_executed",
            ReportSeverity.SERIOUS,
        )

        val deterministicFullAnalysis = ReportWarningRuleEngine.apply(
            base.copy(
                metadata = base.metadata.copy(
                    selectedModel = null,
                    executedModel = null,
                    executedRuntime = ExecutedRuntime.DETERMINISTIC,
                    processingMode = ProcessingMode.FULL_ANALYSIS,
                ),
            ),
        )
        assertHasWarning(
            deterministicFullAnalysis.warnings,
            "runtime.full_analysis_without_neural_vision",
            ReportSeverity.FAILED,
        )
    }

    @Test
    fun calculationRunMapperAppliesWarningRules() {
        val report = CalculationRunReportMapper.map(
            run = calculationRun(
                peaks = listOf(
                    peakResult(
                        rtApex = 1.0,
                        overlapStatus = OverlapStatus.UNRESOLVED,
                        areaPercent = 72.0,
                    ),
                    peakResult(
                        rtApex = 2.0,
                        overlapStatus = OverlapStatus.ISOLATED,
                        areaPercent = 28.0,
                    ),
                ),
            ),
            options = CalculationRunReportOptions(
                processingMode = ProcessingMode.FULL_ANALYSIS,
                executedRuntime = ExecutedRuntime.DETERMINISTIC,
                graphSourceMetadata = GraphSourceMetadata(
                    sourceImageBounds = PixelRect(0, 0, 800, 500),
                    detectedGraphBounds = PixelRect(20, 40, 720, 420),
                    cropConfidence = 0.62,
                ),
            ),
        )

        assertHasWarning(
            report.warnings,
            "runtime.full_analysis_without_neural_vision",
            ReportSeverity.FAILED,
        )
        val graphWarnings = report.graphs.single().warnings
        assertHasWarning(graphWarnings, "graph.crop_confidence_low", ReportSeverity.SERIOUS)
        assertHasWarning(graphWarnings, "peak.coelution_suspected", ReportSeverity.SERIOUS)
        assertHasWarning(graphWarnings, "peak.dominant_peak_review", ReportSeverity.SERIOUS)
    }

    private fun assertHasWarning(
        warnings: List<ReportWarning>,
        code: String,
        severity: ReportSeverity,
    ) {
        val warning = warnings.firstOrNull { it.code == code }
        assertTrue(warning != null, "Missing warning: $code in ${warnings.map { it.code }}")
        assertEquals(severity, warning.severity, "Unexpected severity for $code")
    }

    private fun calculationRun(peaks: List<PeakResult>): CalculationRun =
        CalculationRun(
            id = "warning-rule-run",
            sourceSignalId = "warning-rule-signal",
            pipelineVersion = "test",
            algorithmVersion = "test",
            params = CalculationParams(),
            validation = ValidationResult(
                isValid = true,
                pointCount = 3,
                isSorted = true,
                duplicateTimeCount = 0,
                gapCount = 0,
                nanCount = 0,
                infinityCount = 0,
                negativeIntensityCount = 0,
                isUniformSpacing = true,
                avgTimeStep = 1.0,
                maxTimeStepDeviation = 0.0,
                warnings = emptyList(),
            ),
            signals = SignalBundle(
                raw = listOf(
                    signalPoint(0, 0.0, 0.0),
                    signalPoint(1, 1.0, 100.0),
                    signalPoint(2, 2.0, 40.0),
                ),
                smoothed = null,
                baseline = listOf(0.0, 0.0, 0.0),
                baselineCorrected = null,
                signalUsedForDetection = SignalSource.RAW,
                signalUsedForIntegration = SignalSource.RAW,
            ),
            peaks = peaks,
            warnings = emptyList(),
            timestamp = 1_000L,
        )

    private fun signalPoint(index: Int, time: Double, intensity: Double): SignalPoint =
        SignalPoint(
            index = index,
            time = time,
            intensity = intensity,
            pixelX = index,
            pixelY = intensity,
            confidence = 1.0,
            isInterpolated = false,
        )

    private fun peakResult(
        rtApex: Double,
        overlapStatus: OverlapStatus,
        areaPercent: Double,
    ): PeakResult =
        PeakResult(
            peakId = (rtApex - 1.0).toInt(),
            status = PeakStatus.AUTO,
            rtApex = rtApex,
            rtCentroid = null,
            height = if (rtApex == 1.0) 100.0 else 40.0,
            area = areaPercent,
            widthBase = 0.2,
            widthHalfHeight = 0.1,
            prominence = 20.0,
            snr = 20.0,
            snrMethod = "RMS",
            baselineMethod = "ALS",
            integrationMethod = "trapezoidal",
            confidence = ConfidenceGrade.HIGH,
            overlapStatus = overlapStatus,
            leftBoundaryTime = rtApex - 0.1,
            rightBoundaryTime = rtApex + 0.1,
            boundaryMethod = "LOCAL_MINIMA",
            warnings = emptyList(),
            resolution = if (rtApex == 1.0) null else 0.8,
            areaPercent = areaPercent,
        )
}

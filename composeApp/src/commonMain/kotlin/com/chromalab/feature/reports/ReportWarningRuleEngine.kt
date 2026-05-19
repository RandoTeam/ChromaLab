package com.chromalab.feature.reports

import kotlin.math.abs

/**
 * Conservative quality rules for the structured chromatogram report.
 *
 * These rules do not create numeric results. They only mark conditions that make the report
 * scientifically risky or incomplete enough to require review before interpretation.
 */
object ReportWarningRuleEngine {

    fun apply(report: ChromatogramReport): ChromatogramReport {
        val reportGraphIndex = report.graphs.singleOrNull()?.graphIndex
        val reportWarnings = report.warnings + evaluateReport(report.metadata, reportGraphIndex)
        val graphs = report.graphs.map { graph ->
            val ruleWarnings = evaluateGraph(graph)
            graph.copy(
                warnings = (graph.warnings + ruleWarnings).deduplicateWarnings(),
                quality = graph.quality.copy(
                    anomalies = (graph.quality.anomalies + ruleWarnings.toAnomalies()).deduplicateAnomalies(),
                ),
            )
        }
        return report.copy(
            warnings = reportWarnings.deduplicateWarnings(),
            graphs = graphs,
        )
    }

    fun evaluateReport(
        metadata: ReportMetadata,
        graphIndex: Int? = null,
    ): List<ReportWarning> = buildList {
        val executedRuntime = metadata.resolvedExecutedRuntime()

        if (metadata.processingMode == ProcessingMode.FULL_ANALYSIS ||
            metadata.processingMode == ProcessingMode.AUTO_DIAGNOSTIC
        ) {
            when (executedRuntime) {
                ExecutedRuntime.DETERMINISTIC -> add(
                    ReportWarning(
                        code = "runtime.full_analysis_without_neural_vision",
                        message = "Full chromatogram analysis was completed with deterministic-only runtime; neural vision stages were not executed.",
                        severity = ReportSeverity.FAILED,
                        stage = "model_runtime",
                        graphIndex = graphIndex,
                    ),
                )

                ExecutedRuntime.OCR -> add(
                    ReportWarning(
                        code = "runtime.ocr_only_full_analysis",
                        message = "Full chromatogram analysis used OCR-only runtime; image reasoning and model validation are not available.",
                        severity = ReportSeverity.FAILED,
                        stage = "model_runtime",
                        graphIndex = graphIndex,
                    ),
                )

                ExecutedRuntime.UNKNOWN -> add(
                    ReportWarning(
                        code = "runtime.executed_unknown",
                        message = "Executed model runtime is unknown; the report cannot prove which analysis engine produced the result.",
                        severity = ReportSeverity.SERIOUS,
                        stage = "model_runtime",
                        graphIndex = graphIndex,
                    ),
                )

                ExecutedRuntime.LITERT,
                ExecutedRuntime.GGUF,
                ExecutedRuntime.MIXED -> Unit
            }
        }

        val selectedRuntime = metadata.selectedModel?.runtime
        if (selectedRuntime == ExecutedRuntime.GGUF &&
            executedRuntime != ExecutedRuntime.GGUF &&
            executedRuntime != ExecutedRuntime.UNKNOWN
        ) {
            add(
                ReportWarning(
                    code = "runtime.selected_gguf_not_executed",
                    message = "A GGUF model was selected, but the executed runtime was ${executedRuntime.name}; this report must not be used as GGUF validation.",
                    severity = ReportSeverity.SERIOUS,
                    stage = "model_runtime",
                    graphIndex = graphIndex,
                ),
            )
        }
    }

    fun evaluateGraph(graph: GraphReport): List<ReportWarning> = buildList {
        addAll(evaluateCrop(graph))
        addAll(evaluateBaseline(graph))
        addAll(evaluateSeparation(graph))
        addAll(evaluatePeakLevelRisks(graph))
    }

    private fun evaluateCrop(graph: GraphReport): List<ReportWarning> = buildList {
        val cropConfidence = graph.source.cropConfidence.takeIfUsable() ?: return@buildList
        val severity = when {
            cropConfidence < WEAK_CROP_SERIOUS_THRESHOLD -> ReportSeverity.SERIOUS
            cropConfidence < WEAK_CROP_WARNING_THRESHOLD -> ReportSeverity.WARNING
            else -> null
        } ?: return@buildList

        add(
            ReportWarning(
                code = if (severity == ReportSeverity.SERIOUS) {
                    "graph.crop_confidence_low"
                } else {
                    "graph.crop_review_required"
                },
                message = "Graph crop confidence is ${cropConfidence.renderPercent()}; graph bounds should be reviewed before trusting axis and peak metrics.",
                severity = severity,
                stage = "graph_preparation",
                graphIndex = graph.graphIndex,
            ),
        )
    }

    private fun evaluateBaseline(graph: GraphReport): List<ReportWarning> = buildList {
        val baselineQuality = graph.quality.baselineQuality.value?.trim()?.uppercase()
        if (baselineQuality == "POOR") {
            add(
                ReportWarning(
                    code = "baseline.quality_poor",
                    message = "Baseline quality is poor; peak heights, areas, and S/N values require review.",
                    severity = ReportSeverity.SERIOUS,
                    stage = "signal_baseline",
                    graphIndex = graph.graphIndex,
                ),
            )
        }

        if (!graph.signal.correctedSignalAvailable && graph.peaks.isNotEmpty()) {
            add(
                ReportWarning(
                    code = "baseline.correction_missing",
                    message = "Baseline-corrected signal is not available while peaks are reported.",
                    severity = ReportSeverity.WARNING,
                    stage = "signal_baseline",
                    graphIndex = graph.graphIndex,
                ),
            )
        }

        val maximumHeight = graph.quality.maximumPeakHeight.value.takeIfUsable()?.takeIf { it > 0.0 }
            ?: return@buildList
        val noiseRatio = graph.signal.rmsNoise.value.takeIfUsable()?.let { abs(it) / maximumHeight }
        if (noiseRatio != null && noiseRatio >= BASELINE_NOISE_WARNING_RATIO) {
            add(
                ReportWarning(
                    code = "baseline.noise_high",
                    message = "Estimated RMS noise is ${noiseRatio.renderPercent()} of the maximum peak height.",
                    severity = if (noiseRatio >= BASELINE_NOISE_SERIOUS_RATIO) {
                        ReportSeverity.SERIOUS
                    } else {
                        ReportSeverity.WARNING
                    },
                    stage = "signal_baseline",
                    graphIndex = graph.graphIndex,
                ),
            )
        }

        val driftRatio = graph.signal.baselineDrift.value.takeIfUsable()?.let { abs(it) / maximumHeight }
        if (driftRatio != null && driftRatio >= BASELINE_DRIFT_WARNING_RATIO) {
            add(
                ReportWarning(
                    code = "baseline.drift_high",
                    message = "Baseline drift is ${driftRatio.renderPercent()} of the maximum peak height.",
                    severity = if (driftRatio >= BASELINE_DRIFT_SERIOUS_RATIO) {
                        ReportSeverity.SERIOUS
                    } else {
                        ReportSeverity.WARNING
                    },
                    stage = "signal_baseline",
                    graphIndex = graph.graphIndex,
                ),
            )
        }
    }

    private fun evaluateSeparation(graph: GraphReport): List<ReportWarning> = buildList {
        val minimumResolution = graph.quality.minimumResolution.value.takeIfUsable() ?: return@buildList
        val severity = when {
            minimumResolution < MINIMUM_RESOLUTION_SERIOUS -> ReportSeverity.SERIOUS
            minimumResolution < MINIMUM_RESOLUTION_WARNING -> ReportSeverity.WARNING
            else -> null
        } ?: return@buildList

        add(
            ReportWarning(
                code = "separation.minimum_resolution_low",
                message = "Minimum peak resolution is ${"%.2f".format(minimumResolution)}; co-elution can affect integration and assignments.",
                severity = severity,
                stage = "chromatographic_quality",
                graphIndex = graph.graphIndex,
            ),
        )
    }

    private fun evaluatePeakLevelRisks(graph: GraphReport): List<ReportWarning> = buildList {
        graph.peaks.forEach { peak ->
            peak.overlapWarning(graph.graphIndex)?.let(::add)
        }

        val dominant = graph.peaks.maxByOrNull { it.areaPercent.value.takeIfUsable() ?: -1.0 }
        val dominantAreaPercent = dominant?.areaPercent?.value?.takeIfUsable()
        if (dominant != null && dominantAreaPercent != null && dominantAreaPercent >= DOMINANT_AREA_WARNING_PERCENT) {
            add(
                ReportWarning(
                    code = "peak.dominant_peak_review",
                    message = "Peak #${dominant.number} contributes ${"%.1f".format(dominantAreaPercent)}% of normalized area; review as possible contamination, internal standard, solvent/front artifact, or co-elution before interpretation.",
                    severity = if (dominantAreaPercent >= DOMINANT_AREA_SERIOUS_PERCENT) {
                        ReportSeverity.SERIOUS
                    } else {
                        ReportSeverity.WARNING
                    },
                    stage = "chemical_interpretation",
                    graphIndex = graph.graphIndex,
                    peakNumber = dominant.number,
                ),
            )
        }

        val heightOutlier = graph.heightOutlierPeak()
        if (heightOutlier != null) {
            add(
                ReportWarning(
                    code = "peak.dominant_height_review",
                    message = "Peak #${heightOutlier.number} is much taller than the median peak; review it before using distribution or compound conclusions.",
                    severity = ReportSeverity.WARNING,
                    stage = "chemical_interpretation",
                    graphIndex = graph.graphIndex,
                    peakNumber = heightOutlier.number,
                ),
            )
        }
    }

    private fun ReportPeak.overlapWarning(graphIndex: Int): ReportWarning? {
        val status = overlapClass.value?.trim()?.uppercase() ?: return null
        val severity = when {
            "UNRESOLVED" in status -> ReportSeverity.SERIOUS
            "SHOULDER" in status -> ReportSeverity.WARNING
            "PARTIALLY_OVERLAPPED" in status || "PARTIALLY OVERLAPPED" in status -> ReportSeverity.WARNING
            else -> return null
        }
        return ReportWarning(
            code = "peak.coelution_suspected",
            message = "Peak #$number has overlap status '$status'; area and assignment need review before release-quality interpretation.",
            severity = severity,
            stage = "peak_integration",
            graphIndex = graphIndex,
            peakNumber = number,
        )
    }

    private fun GraphReport.heightOutlierPeak(): ReportPeak? {
        val heights = peaks.mapNotNull { it.heightAboveBaseline.value.takeIfUsable() }.sorted()
        if (heights.size < MIN_HEIGHT_OUTLIER_PEAK_COUNT) return null
        val median = heights.median()
        if (median <= 0.0) return null
        return peaks
            .mapNotNull { peak ->
                peak.heightAboveBaseline.value.takeIfUsable()?.let { height -> peak to height }
            }
            .maxByOrNull { it.second }
            ?.takeIf { it.second / median >= DOMINANT_HEIGHT_RATIO_WARNING }
            ?.first
    }

    private fun ReportMetadata.resolvedExecutedRuntime(): ExecutedRuntime {
        val modelRuntime = executedModel?.runtime
        return if (modelRuntime != null && modelRuntime != ExecutedRuntime.UNKNOWN) {
            modelRuntime
        } else {
            executedRuntime
        }
    }

    private fun List<ReportWarning>.toAnomalies(): List<ReportAnomaly> =
        filter { it.severity == ReportSeverity.SERIOUS || it.severity == ReportSeverity.FAILED }
            .map { warning ->
                ReportAnomaly(
                    code = warning.code,
                    message = warning.message,
                    peakNumber = warning.peakNumber,
                    severity = warning.severity,
                )
            }

    private fun List<ReportWarning>.deduplicateWarnings(): List<ReportWarning> =
        distinctBy { WarningKey(it.code, it.stage, it.graphIndex, it.peakNumber) }

    private fun List<ReportAnomaly>.deduplicateAnomalies(): List<ReportAnomaly> =
        distinctBy { AnomalyKey(it.code, it.peakNumber) }

    private fun List<Double>.median(): Double {
        val middle = size / 2
        return if (size % 2 == 0) {
            (this[middle - 1] + this[middle]) / 2.0
        } else {
            this[middle]
        }
    }

    private fun Double?.takeIfUsable(): Double? =
        if (this != null && !isNaN() && !isInfinite()) this else null

    private fun Double.renderPercent(): String =
        "${"%.1f".format(this * 100.0)}%"

    private data class WarningKey(
        val code: String,
        val stage: String?,
        val graphIndex: Int?,
        val peakNumber: Int?,
    )

    private data class AnomalyKey(
        val code: String,
        val peakNumber: Int?,
    )

    private const val WEAK_CROP_SERIOUS_THRESHOLD = 0.70
    private const val WEAK_CROP_WARNING_THRESHOLD = 0.85
    private const val BASELINE_NOISE_WARNING_RATIO = 0.10
    private const val BASELINE_NOISE_SERIOUS_RATIO = 0.20
    private const val BASELINE_DRIFT_WARNING_RATIO = 0.05
    private const val BASELINE_DRIFT_SERIOUS_RATIO = 0.15
    private const val MINIMUM_RESOLUTION_WARNING = 1.50
    private const val MINIMUM_RESOLUTION_SERIOUS = 1.00
    private const val DOMINANT_AREA_WARNING_PERCENT = 40.0
    private const val DOMINANT_AREA_SERIOUS_PERCENT = 60.0
    private const val DOMINANT_HEIGHT_RATIO_WARNING = 10.0
    private const val MIN_HEIGHT_OUTLIER_PEAK_COUNT = 3
}

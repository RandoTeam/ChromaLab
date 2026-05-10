package com.chromalab.feature.calculation.export

import com.chromalab.feature.calculation.core.*

/**
 * Maps CalculationRun → ExportData for CSV/JSON/HTML export.
 */
object CalculationRunToExportMapper {

    fun map(run: CalculationRun): ExportData {
        val totalArea = run.peaks.sumOf { it.area }

        return ExportData(
            runId = run.id,
            sourceSignalId = run.sourceSignalId,
            pipelineVersion = run.pipelineVersion,
            algorithmVersion = run.algorithmVersion,
            presetName = run.params.presetName,
            paramsJson = buildParamsJson(run.params),
            peaks = run.peaks.map { mapPeak(it, totalArea) },
            correctedSignal = run.signals.baselineCorrected?.map {
                ExportPoint(it.time, it.intensity)
            } ?: run.signals.raw.map { ExportPoint(it.time, it.intensity) },
            baseline = run.signals.baseline?.let { bl ->
                run.signals.raw.mapIndexed { i, pt ->
                    ExportPoint(pt.time, if (i < bl.size) bl[i] else 0.0)
                }
            } ?: emptyList(),
            noiseRegions = emptyList(), // filled when noise regions are tracked
            manualEdits = ManualEditLog(),
            warnings = run.warnings.map {
                ExportWarning(
                    code = it.stage,
                    severity = it.severity.name,
                    message = it.message,
                    peakIndex = it.peakId,
                    stage = it.stage,
                )
            },
            timestamp = run.timestamp,
        )
    }

    private fun mapPeak(peak: PeakResult, totalArea: Double): ExportPeak {
        return ExportPeak(
            peakId = peak.peakId,
            status = peak.status.name,
            rtApex = peak.rtApex,
            rtCentroid = peak.rtCentroid ?: peak.rtApex,
            height = peak.height,
            area = peak.area,
            widthBase = peak.widthBase,
            widthHalfHeight = peak.widthHalfHeight ?: 0.0,
            prominence = peak.prominence,
            snr = peak.snr,
            snrMethod = peak.snrMethod,
            baselineMethod = peak.baselineMethod,
            integrationMethod = peak.integrationMethod,
            confidenceGrade = peak.confidence.name,
            confidenceScore = when (peak.confidence) {
                com.chromalab.feature.calculation.algorithm.ConfidenceGrade.HIGH -> 0.9
                com.chromalab.feature.calculation.algorithm.ConfidenceGrade.MEDIUM -> 0.6
                com.chromalab.feature.calculation.algorithm.ConfidenceGrade.LOW -> 0.3
                com.chromalab.feature.calculation.algorithm.ConfidenceGrade.FAILED -> 0.0
            },
            overlapStatus = peak.overlapStatus.name,
            boundaryMethod = peak.boundaryMethod,
            leftBoundary = peak.leftBoundaryTime,
            rightBoundary = peak.rightBoundaryTime,
            positiveArea = peak.area,
            negativeArea = 0.0,
            isManuallyEdited = peak.status == PeakStatus.MANUAL || peak.status == PeakStatus.CORRECTED,
            warnings = peak.warnings,
            tailingFactor = peak.tailingFactor,
            asymmetryFactor = peak.asymmetryFactor,
            plateCount = peak.plateCount,
            resolution = peak.resolution,
            areaPercent = if (totalArea > 0) peak.area / totalArea * 100.0 else 0.0,
        )
    }

    private fun buildParamsJson(params: CalculationParams): String = buildString {
        appendLine("{")
        appendLine("  \"presetName\": \"${params.presetName}\",")
        appendLine("  \"smoothingEnabled\": ${params.smoothingEnabled},")
        appendLine("  \"smoothingWindowSize\": ${params.smoothingWindowSize},")
        appendLine("  \"smoothingPolynomialOrder\": ${params.smoothingPolynomialOrder},")
        appendLine("  \"baselineMethod\": \"${params.baselineMethod}\",")
        appendLine("  \"baselineLambda\": ${params.baselineLambda},")
        appendLine("  \"baselineP\": ${params.baselineP},")
        appendLine("  \"baselineIterations\": ${params.baselineIterations},")
        appendLine("  \"minSnr\": ${params.minSnr},")
        appendLine("  \"noiseMethod\": \"${params.noiseMethod}\",")
        appendLine("  \"integrationMethod\": \"${params.integrationMethod}\"")
        appendLine("}")
    }
}

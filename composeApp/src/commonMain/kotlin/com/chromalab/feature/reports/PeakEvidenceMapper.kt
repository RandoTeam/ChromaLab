package com.chromalab.feature.reports

import com.chromalab.feature.calculation.algorithm.ConfidenceGrade
import com.chromalab.feature.calculation.algorithm.OverlapStatus
import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.calculation.core.PeakResult
import com.chromalab.feature.calculation.core.PeakStatus
import com.chromalab.feature.calculation.core.SignalPoint
import kotlin.math.abs

object PeakEvidenceMapper {
    fun map(run: CalculationRun, sortedPeaks: List<PeakResult> = run.peaks.sortedBy { it.rtApex }): List<PeakEvidence> =
        sortedPeaks.mapIndexed { index, peak ->
            mapPeak(run, peak, index + 1)
        }

    private fun mapPeak(run: CalculationRun, peak: PeakResult, peakNumber: Int): PeakEvidence {
        val apex = run.signals.raw.nearestSample(peak.rtApex)
            ?.takeIf { it.isCloseTo(peak.rtApex, run) }
        val baselineAtApex = apex?.position?.let { run.signals.baseline?.getOrNull(it) }
        val areaPercent = peak.areaPercent.takeIf { it > 0.0 } ?: run.peaks.areaPercentFor(peak)
        val warnings = buildWarnings(run, peak, apex)
        val status = peak.toPeakEvidenceStatus(run, apex, warnings)
        val gateStatus = status.toGateStatus()

        return PeakEvidence(
            evidenceId = "calculation:${run.id}:peak:${peak.peakId}",
            peakId = peak.peakId.toString(),
            peakNumber = peakNumber,
            status = status,
            gateStatus = gateStatus,
            retentionTime = metric(peak.rtApex, "min"),
            apexPixel = apex?.point?.let { PixelPoint(it.pixelX, it.pixelY) },
            apexPointIndex = apex?.point?.index,
            localMaximumEvidence = apex != null && peak.height.isUsable() && peak.height > 0.0,
            height = metric(peak.height, "a.u.").reviewIf(peak.height <= 0.0, "peak.height_non_positive"),
            area = metric(peak.area),
            areaPercent = metric(areaPercent, "%"),
            widthAtBase = metric(peak.widthBase, "min").reviewIf(peak.widthBase <= 0.0, "peak.width_invalid"),
            fwhm = metric(peak.widthHalfHeight, "min", unknownWarning = "peak.metric.fwhm_unknown"),
            signalToNoise = snrMetric(peak.snr, run.params.minSnr),
            prominence = metric(peak.prominence, "a.u.").reviewIf(peak.prominence <= 0.0, "peak.prominence_non_positive"),
            baselineAtApex = metric(baselineAtApex, "a.u.", unknownWarning = "peak.metric.baseline_at_apex_unknown"),
            boundaryEvidence = boundaryEvidence(peak),
            artifactStatus = peak.toArtifactStatus(),
            overlapStatus = peak.overlapStatus.toEvidenceStatus(),
            provenance = PeakProvenance(
                source = peak.toEvidenceSource(),
                calculationRunId = run.id,
                sourceSignalId = run.sourceSignalId,
                pipelineVersion = run.pipelineVersion,
                algorithmVersion = run.algorithmVersion,
                traceSourceId = run.sourceSignalId,
                userVisibleIntervention = peak.status == PeakStatus.MANUAL || peak.status == PeakStatus.CORRECTED,
            ),
            flags = buildFlags(peak, status),
            warnings = warnings,
        )
    }

    private fun buildWarnings(
        run: CalculationRun,
        peak: PeakResult,
        apex: SignalSample?,
    ): List<String> = buildList {
        if (apex == null) add("peak.apex_link_missing")
        if (!peak.rtApex.isUsable()) add("peak.rt_invalid")
        if (!peak.height.isUsable() || peak.height <= 0.0) add("peak.height_invalid")
        if (!peak.widthBase.isUsable() || peak.widthBase <= 0.0) add("peak.width_invalid")
        if (peak.leftBoundaryTime >= peak.rightBoundaryTime) add("peak.boundary_invalid")
        if (!peak.snr.isUsable()) add("peak.snr_unknown")
        if (peak.snr.isUsable() && peak.snr < run.params.minSnr) add("peak.snr_below_threshold")
        if (peak.widthHalfHeight == null || !peak.widthHalfHeight.isUsable()) add("peak.metric.fwhm_unknown")
        if (peak.confidence == ConfidenceGrade.LOW) add("peak.confidence_low")
        if (peak.confidence == ConfidenceGrade.FAILED) add("peak.confidence_failed")
        if (peak.overlapStatus == OverlapStatus.SHOULDER) add("peak.shoulder_review")
        if (peak.overlapStatus == OverlapStatus.PARTIALLY_OVERLAPPED) add("peak.overlap_review")
        if (peak.overlapStatus == OverlapStatus.UNRESOLVED) add("peak.unresolved_overlap_review")
        addAll(peak.warnings)
    }.distinct()

    private fun PeakResult.toPeakEvidenceStatus(
        run: CalculationRun,
        apex: SignalSample?,
        warnings: List<String>,
    ): PeakEvidenceStatus =
        when {
            status == PeakStatus.REJECTED -> PeakEvidenceStatus.ARTIFACT_REJECTED
            apex == null || confidence == ConfidenceGrade.FAILED || warnings.any { it.endsWith("_invalid") } ->
                PeakEvidenceStatus.INVALID
            status == PeakStatus.MANUAL -> PeakEvidenceStatus.USER_CONFIRMED
            status == PeakStatus.CORRECTED -> PeakEvidenceStatus.USER_EDITED
            overlapStatus == OverlapStatus.SHOULDER -> PeakEvidenceStatus.SHOULDER_REVIEW
            overlapStatus == OverlapStatus.PARTIALLY_OVERLAPPED || overlapStatus == OverlapStatus.UNRESOLVED ->
                PeakEvidenceStatus.OVERLAP_REVIEW
            status == PeakStatus.LOW_CONFIDENCE ||
                confidence == ConfidenceGrade.LOW ||
                (snr.isUsable() && snr < run.params.minSnr) -> PeakEvidenceStatus.AUTO_REVIEW
            else -> PeakEvidenceStatus.AUTO_VALID
        }

    private fun PeakEvidenceStatus.toGateStatus(): PeakGateStatus =
        when (this) {
            PeakEvidenceStatus.AUTO_VALID,
            PeakEvidenceStatus.USER_CONFIRMED,
            PeakEvidenceStatus.USER_EDITED -> PeakGateStatus.VALID
            PeakEvidenceStatus.AUTO_REVIEW,
            PeakEvidenceStatus.SHOULDER_REVIEW,
            PeakEvidenceStatus.OVERLAP_REVIEW -> PeakGateStatus.REVIEW
            PeakEvidenceStatus.USER_REJECTED,
            PeakEvidenceStatus.ARTIFACT_REJECTED,
            PeakEvidenceStatus.NOISE_REJECTED,
            PeakEvidenceStatus.INVALID -> PeakGateStatus.INVALID
        }

    private fun PeakResult.toArtifactStatus(): PeakArtifactStatus =
        when (status) {
            PeakStatus.REJECTED -> PeakArtifactStatus.ARTIFACT_REJECTED
            else -> PeakArtifactStatus.NONE
        }

    private fun PeakResult.toEvidenceSource(): PeakEvidenceSource =
        when (status) {
            PeakStatus.MANUAL -> PeakEvidenceSource.USER_CONFIRMED
            PeakStatus.CORRECTED -> PeakEvidenceSource.USER_EDITED
            PeakStatus.REJECTED -> PeakEvidenceSource.USER_REJECTED
            else -> PeakEvidenceSource.AUTO_DETECTED
        }

    private fun OverlapStatus.toEvidenceStatus(): PeakOverlapEvidenceStatus =
        when (this) {
            OverlapStatus.ISOLATED -> PeakOverlapEvidenceStatus.ISOLATED
            OverlapStatus.SHOULDER -> PeakOverlapEvidenceStatus.SHOULDER_REVIEW
            OverlapStatus.PARTIALLY_OVERLAPPED -> PeakOverlapEvidenceStatus.OVERLAP_REVIEW
            OverlapStatus.UNRESOLVED -> PeakOverlapEvidenceStatus.UNRESOLVED_REVIEW
        }

    private fun buildFlags(peak: PeakResult, status: PeakEvidenceStatus): List<String> =
        buildList {
            add(status.name)
            if (peak.overlapStatus != OverlapStatus.ISOLATED) add(peak.overlapStatus.name)
            if (peak.confidence == ConfidenceGrade.LOW || peak.confidence == ConfidenceGrade.FAILED) {
                add("LOW_CONFIDENCE")
            }
            if (peak.status == PeakStatus.REJECTED) add("REJECTED_BY_CALCULATION_RUN")
        }.distinct()

    private fun boundaryEvidence(peak: PeakResult): PeakBoundaryEvidence {
        val valid = peak.leftBoundaryTime.isUsable() &&
            peak.rightBoundaryTime.isUsable() &&
            peak.leftBoundaryTime < peak.rightBoundaryTime
        return PeakBoundaryEvidence(
            startRetentionTime = metric(peak.leftBoundaryTime, "min"),
            endRetentionTime = metric(peak.rightBoundaryTime, "min"),
            method = peak.boundaryMethod,
            integrationMethod = peak.integrationMethod,
            baselineMethod = peak.baselineMethod,
            status = if (valid) PeakMetricEvidenceStatus.CALCULATED else PeakMetricEvidenceStatus.INVALID,
            warnings = if (valid) emptyList() else listOf("peak.boundary_invalid"),
        )
    }

    private fun snrMetric(value: Double, threshold: Double): PeakMetricEvidence =
        when {
            !value.isUsable() -> PeakMetricEvidence.unknown(warning = "peak.snr_unknown")
            value < threshold -> PeakMetricEvidence.review(value, warning = "peak.snr_below_threshold")
            else -> PeakMetricEvidence.calculated(value)
        }

    private fun metric(value: Double?, unit: String? = null, unknownWarning: String = "metric unavailable"): PeakMetricEvidence =
        PeakMetricEvidence.calculated(value, unit).let { metric ->
            if (metric.status == PeakMetricEvidenceStatus.UNKNOWN) {
                PeakMetricEvidence.unknown(unit, unknownWarning)
            } else {
                metric
            }
        }

    private fun PeakMetricEvidence.reviewIf(condition: Boolean, warning: String): PeakMetricEvidence =
        if (condition) copy(status = PeakMetricEvidenceStatus.REVIEW, warning = warning) else this

    private fun List<SignalPoint>.nearestSample(time: Double): SignalSample? =
        withIndex().minByOrNull { abs(it.value.time - time) }?.let { SignalSample(it.value, it.index) }

    private fun SignalSample.isCloseTo(time: Double, run: CalculationRun): Boolean {
        val spacing = run.validation.avgTimeStep.takeIf { it.isFinite() && it > 0.0 }
            ?: run.signals.raw.estimateMedianSpacing()
            ?: return true
        return abs(point.time - time) <= spacing * 1.5
    }

    private fun List<SignalPoint>.estimateMedianSpacing(): Double? =
        zipWithNext { left, right -> abs(right.time - left.time) }
            .filter { it.isFinite() && it > 0.0 }
            .sorted()
            .let { spacings ->
                if (spacings.isEmpty()) null else spacings[spacings.size / 2]
            }

    private fun List<PeakResult>.areaPercentFor(peak: PeakResult): Double? {
        val totalArea = sumOf { abs(it.area) }
        return if (totalArea > 0.0) abs(peak.area) / totalArea * 100.0 else null
    }

    private fun Double?.isUsable(): Boolean =
        this != null && isFinite()

    private fun Double.isUsable(): Boolean = isFinite()

    private data class SignalSample(
        val point: SignalPoint,
        val position: Int,
    )
}

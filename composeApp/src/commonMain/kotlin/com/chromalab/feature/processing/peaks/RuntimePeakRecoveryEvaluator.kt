package com.chromalab.feature.processing.peaks

import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.calculation.core.PeakResult
import com.chromalab.feature.calculation.core.SignalPoint
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class RuntimePeakRecoveryEvaluation(
    val candidates: List<RecoveredPeakCandidate>,
) {
    val runtimeRecoveredPeaks: List<RecoveredPeakCandidate>
        get() = candidates.filter { it.isProductionReportable }

    val testOnlyRecoveredPeaks: List<RecoveredPeakCandidate>
        get() = candidates.filter {
            it.status != RecoveredPeakCandidateStatus.REJECTED &&
                RecoveredPeakCandidateFlag.FIXTURE_HINT_ONLY in it.flags
        }

    val rejectedRecoveredCandidates: List<RecoveredPeakCandidate>
        get() = candidates.filter { it.status == RecoveredPeakCandidateStatus.REJECTED }
}

class RuntimePeakRecoveryEvaluator {
    fun evaluate(
        run: CalculationRun,
        peakLabelEvidence: List<PeakLabelEvidence>,
        geometryReportStatus: GeometryReportStatus?,
        xCalibrationStatus: CalibrationFitStatus?,
        yCalibrationStatus: CalibrationFitStatus?,
    ): RuntimePeakRecoveryEvaluation {
        val sortedSignal = (run.signals.baselineCorrected ?: run.signals.smoothed ?: run.signals.raw)
            .sortedBy { it.time }
        val existingPeaks = run.peaks.sortedBy { it.rtApex }
        val candidates = peakLabelEvidence
            .filter { it.parsedRetentionTime != null }
            .mapIndexed { index, evidence ->
                evaluateEvidence(
                    evidenceId = "label_${index + 1}",
                    evidence = evidence,
                    signal = sortedSignal,
                    existingPeaks = existingPeaks,
                    geometryReportStatus = geometryReportStatus,
                    xCalibrationStatus = xCalibrationStatus,
                    yCalibrationStatus = yCalibrationStatus,
                )
            }
        return RuntimePeakRecoveryEvaluation(candidates)
    }

    private fun evaluateEvidence(
        evidenceId: String,
        evidence: PeakLabelEvidence,
        signal: List<SignalPoint>,
        existingPeaks: List<PeakResult>,
        geometryReportStatus: GeometryReportStatus?,
        xCalibrationStatus: CalibrationFitStatus?,
        yCalibrationStatus: CalibrationFitStatus?,
    ): RecoveredPeakCandidate {
        val labelRt = evidence.parsedRetentionTime
            ?: return rejected(evidenceId, evidence, 0.0, "label_rt_missing")
        val fixtureOnly = evidence.source == PeakLabelEvidenceSource.FIXTURE_HINT || !evidence.isRuntimeEvidence
        val baseFlags = buildList {
            if (fixtureOnly) add(RecoveredPeakCandidateFlag.FIXTURE_HINT_ONLY)
        }

        if (!fixtureOnly && evidence.source !in PRODUCTION_SOURCES) {
            return rejected(evidenceId, evidence, labelRt, "peak_label_source_not_runtime", baseFlags)
        }
        if (!fixtureOnly && (evidence.status == PeakLabelEvidenceStatus.REJECTED || evidence.confidence < MIN_OCR_CONFIDENCE)) {
            return rejected(
                evidenceId = evidenceId,
                evidence = evidence,
                labelRt = labelRt,
                reason = "ocr_confidence_too_low",
                flags = baseFlags + RecoveredPeakCandidateFlag.OCR_CONFIDENCE_REJECTED,
            )
        }
        if (!calibrationAllowsRecovery(geometryReportStatus, xCalibrationStatus, yCalibrationStatus)) {
            return rejected(
                evidenceId = evidenceId,
                evidence = evidence,
                labelRt = labelRt,
                reason = "calibration_not_valid_or_review",
                flags = baseFlags + RecoveredPeakCandidateFlag.CALIBRATION_INVALID_REJECTED,
            )
        }
        if (signal.size < MIN_SIGNAL_POINTS) {
            return rejected(evidenceId, evidence, labelRt, "signal_too_sparse", baseFlags)
        }
        val firstTime = signal.first().time
        val lastTime = signal.last().time
        if (labelRt !in firstTime..lastTime) {
            return rejected(
                evidenceId = evidenceId,
                evidence = evidence,
                labelRt = labelRt,
                reason = "label_rt_outside_signal_range",
                flags = baseFlags + RecoveredPeakCandidateFlag.OUTSIDE_PLOT_REJECTED,
            )
        }

        val avgStep = averageStep(signal)
        val range = (lastTime - firstTime).coerceAtLeast(avgStep)
        val duplicateTolerance = max(avgStep * 3.0, range * 0.004)
        if (existingPeaks.any { abs(it.rtApex - labelRt) <= duplicateTolerance }) {
            return rejected(
                evidenceId = evidenceId,
                evidence = evidence,
                labelRt = labelRt,
                reason = "duplicate_existing_peak",
                flags = baseFlags + RecoveredPeakCandidateFlag.DUPLICATE_REJECTED,
            )
        }

        val halfWindow = max(avgStep * 5.0, range * 0.012).coerceAtMost(range * 0.08)
        val local = signal
            .filter { it.time in (labelRt - halfWindow)..(labelRt + halfWindow) }
            .takeIf { it.size >= 3 }
            ?: return rejected(evidenceId, evidence, labelRt, "local_window_too_sparse", baseFlags)
        val apex = local.maxByOrNull { it.intensity }
            ?: return rejected(evidenceId, evidence, labelRt, "local_maximum_missing", baseFlags)
        val apexIndex = local.indexOf(apex)
        val left = local.getOrNull(apexIndex - 1)
        val right = local.getOrNull(apexIndex + 1)
        val localBaseline = estimateLocalBaseline(local)
        val localHeight = apex.intensity - localBaseline
        val noise = estimateNoise(signal).coerceAtLeast(MIN_NOISE_FLOOR)
        val localSnr = localHeight / noise
        val localProminence = estimateLocalProminence(local, apex, localBaseline)
        val curvature = if (left != null && right != null && localHeight > 0.0) {
            ((2.0 * apex.intensity - left.intensity - right.intensity) / localHeight).coerceAtLeast(0.0)
        } else {
            0.0
        }
        val widthEstimate = estimateWidth(local, apex, localBaseline)
        val window = RecoveredPeakIntegrationWindow(
            startRt = local.first().time,
            endRt = local.last().time,
        )
        val verified = localHeight > 0.0 &&
            localProminence > 0.0 &&
            (localSnr >= REVIEW_SNR || curvature >= REVIEW_CURVATURE)
        if (!verified) {
            return RecoveredPeakCandidate(
                sourceEvidenceId = evidenceId,
                labelRt = labelRt,
                nearestLocalMaximumRt = apex.time,
                rtDelta = abs(apex.time - labelRt),
                localHeight = localHeight,
                localSNR = localSnr,
                localProminence = localProminence,
                localCurvatureScore = curvature,
                localBaseline = localBaseline,
                widthEstimate = widthEstimate,
                integrationWindow = window,
                sourceEvidence = evidence,
                status = RecoveredPeakCandidateStatus.REJECTED,
                flags = baseFlags + RecoveredPeakCandidateFlag.FLAT_SIGNAL_REJECTED,
                rejectionReason = "local_signal_evidence_insufficient",
            )
        }

        val verificationFlags = buildList {
            add(RecoveredPeakCandidateFlag.LOW_RESOLUTION_RECOVERED)
            add(RecoveredPeakCandidateFlag.LABEL_EVIDENCE_VERIFIED)
            if (!fixtureOnly) add(RecoveredPeakCandidateFlag.RUNTIME_OCR_VERIFIED)
        }

        return RecoveredPeakCandidate(
            sourceEvidenceId = evidenceId,
            labelRt = labelRt,
            nearestLocalMaximumRt = apex.time,
            rtDelta = abs(apex.time - labelRt),
            localHeight = localHeight,
            localSNR = localSnr,
            localProminence = localProminence,
            localCurvatureScore = curvature,
            localBaseline = localBaseline,
            widthEstimate = widthEstimate,
            integrationWindow = window,
            sourceEvidence = evidence,
            status = RecoveredPeakCandidateStatus.REVIEW,
            flags = baseFlags + verificationFlags,
        )
    }

    private fun calibrationAllowsRecovery(
        geometryReportStatus: GeometryReportStatus?,
        xCalibrationStatus: CalibrationFitStatus?,
        yCalibrationStatus: CalibrationFitStatus?,
    ): Boolean {
        if (geometryReportStatus == GeometryReportStatus.DIAGNOSTIC_ONLY) return false
        if (xCalibrationStatus !in RECOVERY_CALIBRATION_STATUSES) return false
        if (yCalibrationStatus !in RECOVERY_CALIBRATION_STATUSES) return false
        return geometryReportStatus == null ||
            geometryReportStatus == GeometryReportStatus.SCIENTIFIC_READY ||
            geometryReportStatus == GeometryReportStatus.REVIEW_READY
    }

    private fun rejected(
        evidenceId: String,
        evidence: PeakLabelEvidence,
        labelRt: Double,
        reason: String,
        flags: List<RecoveredPeakCandidateFlag> = emptyList(),
    ): RecoveredPeakCandidate =
        RecoveredPeakCandidate(
            sourceEvidenceId = evidenceId,
            labelRt = labelRt,
            sourceEvidence = evidence,
            status = RecoveredPeakCandidateStatus.REJECTED,
            flags = flags,
            rejectionReason = reason,
        )

    private fun averageStep(signal: List<SignalPoint>): Double {
        val steps = signal.zipWithNext { a, b -> b.time - a.time }
            .filter { it > 0.0 }
        return steps.average().takeIf { it.isFinite() && it > 0.0 } ?: 1.0
    }

    private fun estimateLocalBaseline(local: List<SignalPoint>): Double {
        val edgeCount = (local.size / 5).coerceIn(1, 5)
        val edges = local.take(edgeCount) + local.takeLast(edgeCount)
        return edges.map { it.intensity }.sorted().let { values ->
            values.take((values.size / 2).coerceAtLeast(1)).average()
        }
    }

    private fun estimateLocalProminence(
        local: List<SignalPoint>,
        apex: SignalPoint,
        baseline: Double,
    ): Double {
        val leftMin = local.takeWhile { it.time <= apex.time }.minOfOrNull { it.intensity } ?: baseline
        val rightMin = local.dropWhile { it.time < apex.time }.minOfOrNull { it.intensity } ?: baseline
        return apex.intensity - max(leftMin, rightMin)
    }

    private fun estimateWidth(
        local: List<SignalPoint>,
        apex: SignalPoint,
        baseline: Double,
    ): Double? {
        val halfHeight = baseline + (apex.intensity - baseline) / 2.0
        val left = local.lastOrNull { it.time < apex.time && it.intensity <= halfHeight }
        val right = local.firstOrNull { it.time > apex.time && it.intensity <= halfHeight }
        return if (left != null && right != null && right.time > left.time) right.time - left.time else null
    }

    private fun estimateNoise(signal: List<SignalPoint>): Double {
        val diffs = signal.zipWithNext { a, b -> abs(b.intensity - a.intensity) }
            .filter { it.isFinite() }
            .sorted()
        if (diffs.isEmpty()) return MIN_NOISE_FLOOR
        val median = diffs[diffs.size / 2]
        val rms = sqrt(diffs.take((diffs.size / 4).coerceAtLeast(1)).sumOf { it * it } / (diffs.size / 4).coerceAtLeast(1))
        return min(median * 1.4826, rms.takeIf { it.isFinite() && it > 0.0 } ?: median).coerceAtLeast(MIN_NOISE_FLOOR)
    }

    private companion object {
        private val PRODUCTION_SOURCES = setOf(
            PeakLabelEvidenceSource.ML_KIT,
            PeakLabelEvidenceSource.VLM,
            PeakLabelEvidenceSource.BOTH,
        )
        private val RECOVERY_CALIBRATION_STATUSES = setOf(
            CalibrationFitStatus.VALID,
            CalibrationFitStatus.REVIEW,
        )
        private const val MIN_SIGNAL_POINTS = 5
        private const val MIN_OCR_CONFIDENCE = 0.45f
        private const val REVIEW_SNR = 1.5
        private const val REVIEW_CURVATURE = 0.12
        private const val MIN_NOISE_FLOOR = 1.0e-9
    }
}

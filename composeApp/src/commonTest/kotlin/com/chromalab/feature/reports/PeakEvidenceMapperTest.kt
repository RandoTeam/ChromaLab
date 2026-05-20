package com.chromalab.feature.reports

import com.chromalab.feature.calculation.algorithm.CompoundSource
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PeakEvidenceMapperTest {

    @Test
    fun calculationRunPeakMapsToAutoValidPeakEvidence() {
        val evidence = PeakEvidenceMapper.map(run()).single()

        assertEquals(PeakEvidenceStatus.AUTO_VALID, evidence.status)
        assertEquals(PeakGateStatus.VALID, evidence.gateStatus)
        assertEquals(5.0, evidence.retentionTime.value)
        assertEquals(5, evidence.apexPointIndex)
        assertTrue(evidence.localMaximumEvidence)
        assertEquals(PeakMetricEvidenceStatus.CALCULATED, evidence.height.status)
        assertEquals(PeakMetricEvidenceStatus.CALCULATED, evidence.area.status)
        assertTrue(evidence.isReportable)
    }

    @Test
    fun missingOptionalFwhmBecomesUnknownWithoutFabrication() {
        val evidence = PeakEvidenceMapper.map(
            run(peaks = listOf(peak(widthHalfHeight = null))),
        ).single()

        assertEquals(PeakMetricEvidenceStatus.UNKNOWN, evidence.fwhm.status)
        assertEquals(null, evidence.fwhm.value)
        assertTrue(evidence.warnings.contains("peak.metric.fwhm_unknown"))
    }

    @Test
    fun peakWithoutApexLinkBecomesInvalid() {
        val evidence = PeakEvidenceMapper.map(
            run(peaks = listOf(peak(rtApex = 50.0))),
        ).single()

        assertEquals(PeakEvidenceStatus.INVALID, evidence.status)
        assertEquals(PeakGateStatus.INVALID, evidence.gateStatus)
        assertFalse(evidence.isReportable)
        assertTrue(evidence.warnings.contains("peak.apex_link_missing"))
    }

    @Test
    fun shoulderAndOverlapBecomeReviewEvidence() {
        val shoulder = PeakEvidenceMapper.map(
            run(peaks = listOf(peak(overlapStatus = OverlapStatus.SHOULDER))),
        ).single()
        val overlap = PeakEvidenceMapper.map(
            run(peaks = listOf(peak(overlapStatus = OverlapStatus.PARTIALLY_OVERLAPPED))),
        ).single()

        assertEquals(PeakEvidenceStatus.SHOULDER_REVIEW, shoulder.status)
        assertEquals(PeakGateStatus.REVIEW, shoulder.gateStatus)
        assertEquals(PeakEvidenceStatus.OVERLAP_REVIEW, overlap.status)
        assertEquals(PeakGateStatus.REVIEW, overlap.gateStatus)
    }

    @Test
    fun rejectedPeakIsNotReportable() {
        val evidence = PeakEvidenceMapper.map(
            run(peaks = listOf(peak(status = PeakStatus.REJECTED))),
        ).single()

        assertEquals(PeakEvidenceStatus.ARTIFACT_REJECTED, evidence.status)
        assertEquals(PeakGateStatus.INVALID, evidence.gateStatus)
        assertFalse(evidence.isReportable)
    }

    private fun run(
        peaks: List<PeakResult> = listOf(peak()),
        params: CalculationParams = CalculationParams(minSnr = 3.0),
    ): CalculationRun =
        CalculationRun(
            id = "run-1",
            sourceSignalId = "trace-1",
            pipelineVersion = "test-pipeline",
            algorithmVersion = "test-algorithm",
            params = params,
            validation = ValidationResult(
                isValid = true,
                pointCount = signal.size,
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
                raw = signal,
                smoothed = null,
                baseline = List(signal.size) { 0.0 },
                baselineCorrected = signal,
                signalUsedForDetection = SignalSource.RAW,
                signalUsedForIntegration = SignalSource.RAW,
            ),
            peaks = peaks,
            warnings = emptyList(),
            timestamp = 123L,
        )

    private fun peak(
        status: PeakStatus = PeakStatus.AUTO,
        rtApex: Double = 5.0,
        widthHalfHeight: Double? = 1.0,
        overlapStatus: OverlapStatus = OverlapStatus.ISOLATED,
    ): PeakResult =
        PeakResult(
            peakId = 1,
            status = status,
            rtApex = rtApex,
            rtCentroid = rtApex,
            height = 10.0,
            area = 30.0,
            widthBase = 2.0,
            widthHalfHeight = widthHalfHeight,
            prominence = 8.0,
            snr = 5.0,
            snrMethod = "test",
            baselineMethod = "ALS",
            integrationMethod = "trapezoidal",
            confidence = ConfidenceGrade.HIGH,
            overlapStatus = overlapStatus,
            leftBoundaryTime = 4.0,
            rightBoundaryTime = 6.0,
            boundaryMethod = "LOCAL_MINIMA",
            warnings = emptyList(),
            areaPercent = 100.0,
            compoundSource = CompoundSource.NONE,
        )

    private val signal: List<SignalPoint> =
        (0..10).map { index ->
            SignalPoint(
                index = index,
                time = index.toDouble(),
                intensity = if (index == 5) 10.0 else index.toDouble(),
                pixelX = index,
                pixelY = 100.0 - index,
                confidence = 1.0,
                isInterpolated = false,
            )
        }
}

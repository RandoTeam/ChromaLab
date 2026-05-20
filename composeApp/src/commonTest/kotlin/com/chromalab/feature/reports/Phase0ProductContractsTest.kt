package com.chromalab.feature.reports

import com.chromalab.feature.processing.axis.AxisLine
import com.chromalab.feature.processing.geometry.AxisCalibrationFit
import com.chromalab.feature.processing.geometry.AxisGeometry
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryAxis
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import com.chromalab.feature.processing.geometry.GeometryTrace
import com.chromalab.feature.processing.geometry.TickGeometry
import com.chromalab.feature.processing.geometry.TickPixelPosition
import com.chromalab.feature.reports.fixtures.BelyiTigrIon92ReportFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Phase0ProductContractsTest {

    @Test
    fun phase0ProductModesAreExplicit() {
        assertTrue(ProcessingMode.entries.contains(ProcessingMode.AUTONOMOUS_PRODUCTION))
        assertTrue(ProcessingMode.entries.contains(ProcessingMode.AUTO_DIAGNOSTIC))
        assertTrue(ProcessingMode.entries.contains(ProcessingMode.ASSISTED_REVIEW))
        assertTrue(ProcessingMode.entries.contains(ProcessingMode.GUIDED_PRODUCTION))
        assertTrue(ProcessingMode.entries.contains(ProcessingMode.MANUAL_ADVANCED))
    }

    @Test
    fun releaseGateBlocksMissingEvidencePackage() {
        val report = releaseReadyShapeReport()

        val gate = ReportReleaseGateEvaluator.evaluate(
            report = report,
            evidencePackageStatus = EvidenceGateStatus.MISSING,
        )

        assertEquals(ReportGateStatus.DIAGNOSTIC_ONLY, gate.status)
        assertTrue(gate.blockingReasons.contains("evidence_package.missing"))
    }

    @Test
    fun releaseGateRequiresGraphPlotCalibrationTraceAndSourceEvidence() {
        val report = releaseReadyShapeReport()

        val gate = ReportReleaseGateEvaluator.evaluate(
            report = report,
            validation = ReportContractValidationResult(isComplete = true, findings = emptyList()),
            evidencePackageStatus = EvidenceGateStatus.VALID,
        )

        assertEquals(ReportGateStatus.REVIEW_ONLY, gate.status)
        assertEquals(EvidenceGateStatus.VALID, gate.evidence.graphPanelStatus)
        assertEquals(EvidenceGateStatus.VALID, gate.evidence.plotAreaStatus)
        assertEquals(EvidenceGateStatus.VALID, gate.evidence.xCalibrationStatus)
        assertEquals(EvidenceGateStatus.VALID, gate.evidence.yCalibrationStatus)
        assertEquals(EvidenceGateStatus.VALID, gate.evidence.traceStatus)
        assertTrue(gate.blockingReasons.isEmpty())
    }

    @Test
    fun autonomousProductionRequiredAutomaticEvidenceDoesNotNeedUserConfirmation() {
        val base = releaseReadyShapeReport().withAutoValidPeakEvidence()
        val report = base.copy(
            metadata = base.metadata.copy(processingMode = ProcessingMode.AUTONOMOUS_PRODUCTION),
            warnings = emptyList(),
            graphs = base.graphs.map { it.copy(warnings = emptyList()) },
        )

        val gate = ReportReleaseGateEvaluator.evaluate(
            report = report,
            validation = ReportContractValidationResult(isComplete = true, findings = emptyList()),
            evidencePackageStatus = EvidenceGateStatus.VALID,
        )

        assertEquals(ReportGateStatus.RELEASE_READY, gate.status, gate.toString())
        assertEquals(EvidenceGateStatus.VALID, gate.evidence.traceStatus)
        assertEquals(EvidenceGateStatus.VALID, gate.evidence.peakReviewStatus)
        assertFalse(gate.blockingReasons.any { it.startsWith("trace.") })
    }

    @Test
    fun autoDiagnosticCannotBeReleaseReadyWithoutPeakEvidenceGate() {
        val report = releaseReadyShapeReport().copy(
            graphs = releaseReadyShapeReport().graphs.map { graph ->
                graph.copy(peakRecovery = graph.peakRecovery.copy(peakEvidenceTable = emptyList()))
            },
        )

        val gate = ReportReleaseGateEvaluator.evaluate(
            report = report,
            evidencePackageStatus = EvidenceGateStatus.VALID,
        )

        assertEquals(ReportGateStatus.REVIEW_ONLY, gate.status)
        assertEquals(EvidenceGateStatus.REVIEW, gate.evidence.peakReviewStatus)
        assertTrue(gate.reviewReasons.contains("peak_review.review"))
    }

    @Test
    fun invalidCalibrationForcesDiagnosticGate() {
        val base = releaseReadyShapeReport()
        val graph = base.graphs.single()
        val report = base.copy(
            graphs = listOf(
                graph.copy(
                    axisCalibration = graph.axisCalibration.copy(
                        xCalibrationFit = AxisCalibrationFit(
                            axis = GeometryAxis.X,
                            status = CalibrationFitStatus.INVALID,
                        ),
                    ),
                ),
            ),
        )

        val gate = ReportReleaseGateEvaluator.evaluate(
            report = report,
            evidencePackageStatus = EvidenceGateStatus.VALID,
        )

        assertEquals(ReportGateStatus.DIAGNOSTIC_ONLY, gate.status)
        assertTrue(gate.blockingReasons.contains("x_calibration.invalid"))
    }

    @Test
    fun vlmBoundaryPolicyForbidsNumericChromatographicMetrics() {
        VlmEvidenceTaskType.entries.forEach { task ->
            NumericChromatographicMetric.entries.forEach { metric ->
                assertFalse(VlmBoundaryPolicy.canPopulateNumericMetric(task, metric))
            }
        }
        assertFalse(VlmBoundaryPolicy.canUseValueSourceForNumericMetric(ReportValueSource.VISION_MODEL))
        assertFalse(VlmBoundaryPolicy.canUseValueSourceForNumericMetric(ReportValueSource.MODEL_SUGGESTED))
        assertFalse(VlmBoundaryPolicy.canUseValueSourceForNumericMetric(ReportValueSource.OCR))
        assertTrue(VlmBoundaryPolicy.canUseValueSourceForNumericMetric(ReportValueSource.DETERMINISTIC))
        assertTrue(VlmBoundaryPolicy.canUseValueSourceForNumericMetric(ReportValueSource.USER))
    }

    private fun releaseReadyShapeReport(): ChromatogramReport {
        val base = BelyiTigrIon92ReportFixture.buildReport()
        val graph = base.graphs.single()
        val trace = graph.source.geometryTrace ?: GeometryTrace()
        return base.copy(
            metadata = base.metadata.copy(
                processingMode = ProcessingMode.AUTO_DIAGNOSTIC,
            ),
            graphs = listOf(
                graph.copy(
                    source = graph.source.copy(
                        geometryReportStatus = GeometryReportStatus.SCIENTIFIC_READY,
                        geometryTrace = trace.copy(
                            originalImagePath = "original.png",
                            normalizedImagePath = "normalized.png",
                            selectedGraphPanelBounds = trace.selectedGraphPanelBounds
                                ?: com.chromalab.feature.processing.geometry.GraphPanelBounds(
                                    region = com.chromalab.feature.processing.graph.GraphRegion(0, 0, 100, 100),
                                    candidateSource = com.chromalab.feature.processing.geometry.GeometryCandidateSource.CV,
                                    confidence = 0.95f,
                                ),
                            selectedPlotAreaBounds = trace.selectedPlotAreaBounds
                                ?: com.chromalab.feature.processing.geometry.PlotAreaBounds(
                                    region = com.chromalab.feature.processing.graph.GraphRegion(10, 10, 80, 80),
                                    parentGraphPanelRegion = com.chromalab.feature.processing.graph.GraphRegion(0, 0, 100, 100),
                                    confidence = 0.90f,
                                ),
                            finalCenterlineOverlayPath = "centerline.png",
                            axisGeometry = AxisGeometry(
                                xAxisLinePx = AxisLine(10f, 90f, 90f, 90f),
                                yAxisLinePx = AxisLine(10f, 10f, 10f, 90f),
                                axisConfidence = 0.95f,
                            ),
                            tickGeometry = TickGeometry(
                                xTicks = listOf(TickPixelPosition(10f, confidence = 0.95f)),
                                yTicks = listOf(TickPixelPosition(90f, confidence = 0.95f)),
                            ),
                        ),
                    ),
                    axisCalibration = graph.axisCalibration.copy(
                        xCalibrationFit = AxisCalibrationFit(
                            axis = GeometryAxis.X,
                            status = CalibrationFitStatus.VALID,
                        ),
                        yCalibrationFit = AxisCalibrationFit(
                            axis = GeometryAxis.Y,
                            status = CalibrationFitStatus.VALID,
                        ),
                    ),
                ),
            ),
        )
    }

    private fun ChromatogramReport.withAutoValidPeakEvidence(): ChromatogramReport =
        copy(
            graphs = graphs.map { graph ->
                graph.copy(
                    peakRecovery = graph.peakRecovery.copy(
                        peakEvidenceTable = graph.peaks.map { peak ->
                            PeakEvidence(
                                evidenceId = "evidence:${peak.number}",
                                peakId = peak.number.toString(),
                                peakNumber = peak.number,
                                status = PeakEvidenceStatus.AUTO_VALID,
                                gateStatus = PeakGateStatus.VALID,
                                retentionTime = PeakMetricEvidence.calculated(peak.retentionTime.value, "min"),
                                apexPointIndex = peak.number,
                                localMaximumEvidence = true,
                                height = PeakMetricEvidence.calculated(
                                    peak.heightAboveBaseline.value ?: 1.0,
                                    "a.u.",
                                ),
                                area = PeakMetricEvidence.calculated(peak.integratedArea.value ?: 1.0),
                                boundaryEvidence = PeakBoundaryEvidence(
                                    startRetentionTime = PeakMetricEvidence.calculated(
                                        peak.startRetentionTime.value ?: 0.0,
                                        "min",
                                    ),
                                    endRetentionTime = PeakMetricEvidence.calculated(
                                        peak.endRetentionTime.value ?: 1.0,
                                        "min",
                                    ),
                                    status = PeakMetricEvidenceStatus.CALCULATED,
                                ),
                            )
                        },
                    ),
                )
            },
        )
}

package com.chromalab.feature.reports

import com.chromalab.feature.processing.axis.AxisLine
import com.chromalab.feature.processing.geometry.AxisCalibrationFit
import com.chromalab.feature.processing.geometry.AxisGeometry
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryAxis
import com.chromalab.feature.processing.geometry.GeometryCandidateSource
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import com.chromalab.feature.processing.geometry.GeometryTrace
import com.chromalab.feature.processing.geometry.GraphPanelBounds
import com.chromalab.feature.processing.geometry.PlotAreaBounds
import com.chromalab.feature.processing.geometry.TickGeometry
import com.chromalab.feature.processing.geometry.TickPixelPosition
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.reports.fixtures.BelyiTigrIon92ReportFixture
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfessionalReportGateRenderingTest {

    @Test
    fun htmlCanRenderReleaseReadyReportWhenAllEvidenceGatesAreExplicitlySatisfied() {
        val report = releaseReadyReport()
        val validation = ReportContractValidationResult(isComplete = true, findings = emptyList())
        val uiContract = ChromatogramReportUiContractBuilder.build(
            report = report,
            validation = validation,
            evidencePackageStatus = EvidenceGateStatus.VALID,
        )

        val html = ReportHtmlRenderer.render(report, uiContract)

        assertTrue(html.contains("RELEASE_READY"), html)
        assertTrue(html.contains("allowed by current evidence gate"), html)
        assertTrue(html.contains("<td>Graph panel</td>"), html)
        assertTrue(html.contains("<td>Evidence package</td>"), html)
        assertTrue(html.contains("<th>Evidence</th>"), html)
        assertTrue(html.contains("AUTO_VALID"), html)
        assertFalse(html.contains("not release-ready.</strong>"), html)
    }

    @Test
    fun markdownKeepsDiagnosticGateVisibleWhenEvidencePackageIsMissing() {
        val markdown = ReportMarkdownRenderer.render(releaseReadyReport())

        assertTrue(markdown.contains("| Report gate | DIAGNOSTIC_ONLY |"), markdown)
        assertTrue(markdown.contains("evidence_package.missing"), markdown)
        assertTrue(markdown.contains("| 1 | AUTO_VALID | VALID |"), markdown)
    }

    private fun releaseReadyReport(): ChromatogramReport {
        val base = BelyiTigrIon92ReportFixture.buildReport()
        val graph = base.graphs.single()
        val trace = graph.source.geometryTrace ?: GeometryTrace()
        val releaseGraph = graph.copy(
            source = graph.source.copy(
                geometryReportStatus = GeometryReportStatus.SCIENTIFIC_READY,
                geometryTrace = trace.copy(
                    originalImagePath = "original.png",
                    normalizedImagePath = "normalized.png",
                    selectedGraphPanelBounds = GraphPanelBounds(
                        region = GraphRegion(0, 0, 100, 100),
                        candidateSource = GeometryCandidateSource.CV,
                        confidence = 0.95f,
                    ),
                    selectedPlotAreaBounds = PlotAreaBounds(
                        region = GraphRegion(10, 10, 80, 80),
                        parentGraphPanelRegion = GraphRegion(0, 0, 100, 100),
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
                xCalibrationFit = AxisCalibrationFit(axis = GeometryAxis.X, status = CalibrationFitStatus.VALID),
                yCalibrationFit = AxisCalibrationFit(axis = GeometryAxis.Y, status = CalibrationFitStatus.VALID),
            ),
        )
        return base.copy(
            metadata = base.metadata.copy(processingMode = ProcessingMode.AUTONOMOUS_PRODUCTION),
            graphs = listOf(releaseGraph.withAutoValidPeakEvidence()),
            warnings = emptyList(),
        )
    }

    private fun GraphReport.withAutoValidPeakEvidence(): GraphReport =
        copy(
            peakRecovery = peakRecovery.copy(
                peakEvidenceTable = peaks.map { peak ->
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
                        areaPercent = PeakMetricEvidence.calculated(peak.areaPercent.value ?: 1.0, "%"),
                        fwhm = PeakMetricEvidence.calculated(peak.fwhm.value ?: 0.1, "min"),
                        signalToNoise = PeakMetricEvidence.calculated(peak.signalToNoise.value ?: 3.0),
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
}

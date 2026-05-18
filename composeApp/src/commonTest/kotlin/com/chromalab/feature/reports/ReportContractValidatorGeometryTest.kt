package com.chromalab.feature.reports

import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.AxisCalibrationFit
import com.chromalab.feature.processing.geometry.GeometryAxis
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import com.chromalab.feature.reports.fixtures.BelyiTigrIon92ReportFixture
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportContractValidatorGeometryTest {

    @Test
    fun diagnosticOnlyGeometryBlocksReleaseQualityReport() {
        val base = BelyiTigrIon92ReportFixture.buildReport()
        val graph = base.graphs.single()
        val report = base.copy(
            graphs = listOf(
                graph.copy(
                    source = graph.source.copy(
                        geometryReportStatus = GeometryReportStatus.DIAGNOSTIC_ONLY,
                    ),
                ),
            ),
        )

        val validation = ReportContractValidator.validate(report)

        assertFalse(validation.isComplete)
        assertTrue(validation.findings.any { it.code == "geometry.diagnostic_only" })
    }

    @Test
    fun reviewGeometryProducesReviewFindingWithoutBlockingContract() {
        val base = BelyiTigrIon92ReportFixture.buildReport()
        val graph = base.graphs.single()
        val report = base.copy(
            graphs = listOf(
                graph.copy(
                    source = graph.source.copy(
                        geometryReportStatus = GeometryReportStatus.REVIEW_READY,
                    ),
                ),
            ),
        )

        val validation = ReportContractValidator.validate(report)

        assertTrue(validation.findings.any { it.code == "geometry.review_grade" })
    }

    @Test
    fun invalidAxisFitBlocksReportContract() {
        val base = BelyiTigrIon92ReportFixture.buildReport()
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

        val validation = ReportContractValidator.validate(report)

        assertFalse(validation.isComplete)
        assertTrue(validation.findings.any { it.code == "axis.calibration_fit_invalid" })
    }
}

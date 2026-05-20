package com.chromalab.feature.reports

import com.chromalab.feature.reports.fixtures.BelyiTigrIon92ReportFixture
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportContractValidatorVlmBoundaryTest {
    @Test
    fun reportNumericPeakMetricsCannotUseVisionModelSource() {
        val report = BelyiTigrIon92ReportFixture.buildReport()
        val graph = report.graphs.first()
        val peak = graph.peaks.first()
        val poisonedPeak = peak.copy(
            retentionTime = peak.retentionTime.copy(source = ReportValueSource.VISION_MODEL),
            integratedArea = peak.integratedArea.copy(source = ReportValueSource.MODEL_SUGGESTED),
        )
        val poisonedReport = report.copy(
            graphs = listOf(
                graph.copy(
                    peaks = listOf(poisonedPeak) + graph.peaks.drop(1),
                ),
            ) + report.graphs.drop(1),
        )

        val validation = ReportContractValidator.validate(poisonedReport)

        assertEquals(2, validation.findings.count { it.code == "peak.model_numeric_source_forbidden" })
        assertTrue(validation.findings.any { it.severity == ReportContractSeverity.ERROR })
    }
}


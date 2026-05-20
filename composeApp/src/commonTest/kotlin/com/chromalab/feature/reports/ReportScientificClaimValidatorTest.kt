package com.chromalab.feature.reports

import com.chromalab.feature.reports.fixtures.BelyiTigrIon92ReportFixture
import kotlin.test.Test
import kotlin.test.assertTrue

class ReportScientificClaimValidatorTest {

    @Test
    fun validatorMarksKnowledgeOnlyCompoundNamesAsHypotheses() {
        val validation = ReportContractValidator.validate(BelyiTigrIon92ReportFixture.buildReport())

        assertTrue(
            validation.findings.any {
                it.code == "peak.compound_assignment_evidence_missing" && it.peakNumber == 1
            },
            validation.findings.toString(),
        )
    }

    @Test
    fun validatorRejectsCalculatedKovatsWithoutReferenceRetentionSeries() {
        val base = BelyiTigrIon92ReportFixture.buildReport()
        val graph = base.graphs.single()
        val report = base.copy(
            graphs = listOf(
                graph.copy(
                    kovats = graph.kovats.copy(
                        results = graph.kovats.results.mapIndexed { index, result ->
                            if (index == 0) {
                                result.copy(
                                    calculatedIndex = ReportDoubleValue.calculated(
                                        value = 875.0,
                                        unit = "RI",
                                        source = ReportValueSource.DETERMINISTIC,
                                    ),
                                )
                            } else {
                                result
                            }
                        },
                        referenceRetentionTimes = emptyList(),
                    ),
                ),
            ),
        )

        val validation = ReportContractValidator.validate(report)

        assertTrue(
            validation.findings.any { it.code == "kovats.reference_series_missing_for_calculated_index" },
            validation.findings.toString(),
        )
    }
}

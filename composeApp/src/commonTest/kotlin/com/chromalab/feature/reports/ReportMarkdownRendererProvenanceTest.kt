package com.chromalab.feature.reports

import com.chromalab.feature.reports.fixtures.BelyiTigrIon92ReportFixture
import kotlin.test.Test
import kotlin.test.assertTrue

class ReportMarkdownRendererProvenanceTest {

    @Test
    fun rendererIncludesValueProvenanceAuditRows() {
        val markdown = ReportMarkdownRenderer.render(BelyiTigrIon92ReportFixture.buildReport())

        assertTrue(markdown.contains("| Report gate | DIAGNOSTIC_ONLY |"), markdown)
        assertTrue(markdown.contains("Report gate evidence:"), markdown)
        assertTrue(markdown.contains("| Evidence package | MISSING | blocks release-ready output |"), markdown)
        assertTrue(markdown.contains("### Value provenance"), markdown)
        assertTrue(
            markdown.contains("| 1 | identification.ionOrChannel | DETECTED | OCR | 93.00% |"),
            markdown,
        )
        assertTrue(
            markdown.contains("| 1 | interpretation.likelyCompoundClass | INFERRED | LOCAL_KNOWLEDGE | 62.00% |"),
            markdown,
        )
    }

    @Test
    fun rendererDistinguishesModelSuggestedAssignmentsFromLocalKnowledge() {
        val base = BelyiTigrIon92ReportFixture.buildReport()
        val graph = base.graphs.single()
        val peak = graph.peaks.first()
        val modelSuggestedPeak = peak.copy(
            retentionTime = ReportDoubleValue.calculated(
                value = 8.4,
                unit = "min",
                confidence = 0.98,
                source = ReportValueSource.DETERMINISTIC,
            ),
            compound = peak.compound?.copy(
                probableName = ReportTextValue(
                    value = "model suggested aromatic candidate",
                    status = ReportValueStatus.INFERRED,
                    confidence = 0.44,
                    source = ReportValueSource.MODEL_SUGGESTED,
                ),
            ),
        )
        val report = base.copy(graphs = listOf(graph.copy(peaks = listOf(modelSuggestedPeak))))

        val markdown = ReportMarkdownRenderer.render(report)

        assertTrue(
            markdown.contains("| 1 | peak[1].compound.probableName | INFERRED | MODEL_SUGGESTED | 44.00% |"),
            markdown,
        )
        assertTrue(
            markdown.contains("| 1 | peak[1].retentionTime | CALCULATED | DETERMINISTIC | 98.00% |"),
            markdown,
        )
    }
}

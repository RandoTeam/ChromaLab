package com.chromalab.feature.reports

import com.chromalab.feature.reports.fixtures.BelyiTigrIon92ReportFixture
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportHtmlRendererTest {

    @Test
    fun rendererCreatesPrintReadyHtmlFromStructuredReport() {
        val html = ReportHtmlRenderer.render(BelyiTigrIon92ReportFixture.buildReport())

        assertTrue(html.startsWith("<!DOCTYPE html>"), html)
        assertTrue(html.contains("@page { size: A4; margin: 14mm; }"), html)
        assertTrue(html.contains("@media print"), html)
        assertTrue(html.contains("ChromaLab offline analytical report"), html)
        assertTrue(html.contains("<h2>1. Overview</h2>"), html)
        assertTrue(html.contains("<table>"), html)
        assertTrue(html.contains("Value provenance"), html)
        assertTrue(html.contains("Fixture executed vision model"), html)
    }

    @Test
    fun rendererEscapesReportValuesInHtml() {
        val base = BelyiTigrIon92ReportFixture.buildReport()
        val report = base.copy(
            metadata = base.metadata.copy(sourceName = "<bad & source>"),
            warnings = base.warnings + ReportWarning(
                code = "html.escape",
                message = "Use <tag> & quotes \"now\"",
                severity = ReportSeverity.WARNING,
                stage = "test",
                graphIndex = 1,
            ),
        )

        val html = ReportHtmlRenderer.render(report)

        assertTrue(html.contains("&lt;bad &amp; source&gt;"), html)
        assertTrue(html.contains("Use &lt;tag&gt; &amp; quotes &quot;now&quot;"), html)
        assertFalse(html.contains("<bad & source>"), html)
        assertFalse(html.contains("Use <tag>"), html)
    }
}

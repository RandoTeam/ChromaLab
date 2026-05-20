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
        assertTrue(html.contains("data-ui-schema=\"chromalab.chromatogram_report_ui.v2\""), html)
        assertTrue(html.contains("<h2>1. Release gate</h2>"), html)
        assertTrue(html.contains("<h2>2. Overview</h2>"), html)
        assertTrue(html.contains("Release-quality claim"), html)
        assertTrue(html.contains("Report gate evidence"), html)
        assertTrue(html.contains("Evidence package"), html)
        assertTrue(html.contains("Peak evidence"), html)
        assertTrue(html.contains("Privacy class"), html)
        assertFalse(html.contains("NEVER_SHARED_BY_DEFAULT"), html)
        assertTrue(html.contains("<table>"), html)
        assertTrue(html.contains("Selected graph focus"), html)
        assertTrue(html.contains("Curve extraction overlay"), html)
        assertTrue(html.contains("Peak integration overlay"), html)
        assertTrue(html.contains("Value provenance"), html)
        assertTrue(html.contains("Knowledge Pack citations"), html)
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

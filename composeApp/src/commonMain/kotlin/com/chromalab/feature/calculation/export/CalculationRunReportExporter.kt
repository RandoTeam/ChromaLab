package com.chromalab.feature.calculation.export

import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.reports.CalculationRunReportMapper
import com.chromalab.feature.reports.CalculationRunReportOptions
import com.chromalab.feature.reports.ChromatogramReport
import com.chromalab.feature.reports.ReportContractValidationResult
import com.chromalab.feature.reports.ReportContractValidator
import com.chromalab.feature.reports.ReportHtmlRenderer
import com.chromalab.feature.reports.ReportMarkdownRenderer

object CalculationRunReportExporter {

    fun buildReport(
        run: CalculationRun,
        options: CalculationRunReportOptions = CalculationRunReportOptions(),
    ): ChromatogramReport =
        CalculationRunReportMapper.map(run, options)

    fun validate(
        run: CalculationRun,
        options: CalculationRunReportOptions = CalculationRunReportOptions(),
    ): ReportContractValidationResult =
        ReportContractValidator.validate(buildReport(run, options))

    fun exportMarkdown(
        run: CalculationRun,
        options: CalculationRunReportOptions = CalculationRunReportOptions(),
    ): String =
        ReportMarkdownRenderer.render(buildReport(run, options))

    fun exportHtml(
        run: CalculationRun,
        options: CalculationRunReportOptions = CalculationRunReportOptions(),
    ): String =
        ReportHtmlRenderer.render(buildReport(run, options))
}

fun generateExportBundle(run: CalculationRun): Map<String, String> {
    val exportData = CalculationRunToExportMapper.map(run)
    return generateExportBundle(exportData) + mapOf(
        "chromatogram_report.md" to CalculationRunReportExporter.exportMarkdown(run),
        "chromatogram_report.html" to CalculationRunReportExporter.exportHtml(run),
    )
}

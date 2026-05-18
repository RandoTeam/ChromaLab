package com.chromalab.feature.calculation.export

import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.processing.debug.DebugPackageExporter
import com.chromalab.feature.reports.CalculationRunReportMapper
import com.chromalab.feature.reports.CalculationRunReportOptions
import com.chromalab.feature.reports.ChromatogramReportUiContract
import com.chromalab.feature.reports.ChromatogramReportUiContractBuilder
import com.chromalab.feature.reports.ChromatogramReport
import com.chromalab.feature.reports.ReportContractValidationResult
import com.chromalab.feature.reports.ReportContractValidator
import com.chromalab.feature.reports.ReportHtmlRenderer
import com.chromalab.feature.reports.ReportMarkdownRenderer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object CalculationRunReportExporter {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

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

    fun buildUiContract(
        run: CalculationRun,
        options: CalculationRunReportOptions = CalculationRunReportOptions(),
    ): ChromatogramReportUiContract {
        val report = buildReport(run, options)
        return ChromatogramReportUiContractBuilder.build(report, ReportContractValidator.validate(report))
    }

    fun exportUiContractJson(
        run: CalculationRun,
        options: CalculationRunReportOptions = CalculationRunReportOptions(),
    ): String =
        json.encodeToString(buildUiContract(run, options))

    fun exportMarkdown(
        run: CalculationRun,
        options: CalculationRunReportOptions = CalculationRunReportOptions(),
    ): String =
        ReportMarkdownRenderer.render(buildReport(run, options))

    fun exportHtml(
        run: CalculationRun,
        options: CalculationRunReportOptions = CalculationRunReportOptions(),
    ): String {
        val report = buildReport(run, options)
        val validation = ReportContractValidator.validate(report)
        return ReportHtmlRenderer.render(
            report = report,
            uiContract = ChromatogramReportUiContractBuilder.build(report, validation),
        )
    }

    fun exportRuntimeEvidencePackageJson(
        run: CalculationRun,
        options: CalculationRunReportOptions = CalculationRunReportOptions(),
    ): String =
        DebugPackageExporter.exportRuntimeEvidencePackage(buildReport(run, options))
}

fun generateExportBundle(run: CalculationRun): Map<String, String> {
    val exportData = CalculationRunToExportMapper.map(run)
    return generateExportBundle(exportData) + mapOf(
        "chromatogram_report_ui_contract.json" to CalculationRunReportExporter.exportUiContractJson(run),
        "chromatogram_report.md" to CalculationRunReportExporter.exportMarkdown(run),
        "chromatogram_report.html" to CalculationRunReportExporter.exportHtml(run),
    )
}

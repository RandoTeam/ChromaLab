package com.chromalab.feature.calculation.export

import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.calculation.core.PeakResult
import com.chromalab.feature.calculation.algorithm.ConfidenceGrade
import com.chromalab.feature.calculation.algorithm.OverlapStatus

/**
 * HTML report generator for chromatographic analysis results.
 *
 * Produces a self-contained HTML file with:
 * - Dark theme (Lab Precision Dark) for screen
 * - Light theme for print (@media print)
 * - All peak data, system suitability, parameters
 * - A4-optimized layout
 *
 * Pure function: CalculationRun → String (HTML).
 */
object ReportExporter {

    fun export(run: CalculationRun): String {
        val totalArea = run.peaks.sumOf { it.area }
        val peaks = run.peaks

        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"ru\">")
            appendLine("<head>")
            appendLine("<meta charset=\"utf-8\">")
            appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
            appendLine("<title>ChromaLab — Отчёт анализа ${run.id}</title>")
            appendLine(CSS)
            appendLine("</head>")
            appendLine("<body>")

            // Header
            appendLine("<header>")
            appendLine("<h1>ChromaLab — Отчёт хроматографического анализа</h1>")
            appendLine("<p class=\"meta\">ID: ${run.id} · Pipeline: ${run.pipelineVersion} · Algorithm: ${run.algorithmVersion}</p>")
            appendLine("</header>")

            // Section 1: Summary
            appendLine("<section>")
            appendLine("<h2>Сводка</h2>")
            appendLine("<table class=\"summary\">")
            summaryRow("Пресет", run.params.presetName)
            summaryRow("Пиков", "${peaks.size}")
            summaryRow("Σ Площадь", formatNumber(totalArea))
            val rtMin = peaks.minOfOrNull { it.rtApex } ?: 0.0
            val rtMax = peaks.maxOfOrNull { it.rtApex } ?: 0.0
            summaryRow("Диапазон RT", "${"%.3f".format(rtMin)} — ${"%.3f".format(rtMax)} мин")
            val highCount = peaks.count { it.confidence == ConfidenceGrade.HIGH }
            val medCount = peaks.count { it.confidence == ConfidenceGrade.MEDIUM }
            val lowCount = peaks.count { it.confidence == ConfidenceGrade.LOW }
            summaryRow("Confidence", "${highCount}H · ${medCount}M · ${lowCount}L")
            summaryRow("Предупреждений", "${run.warnings.size}")
            appendLine("</table>")
            appendLine("</section>")

            // Section 2: Peak Results Table
            appendLine("<section>")
            appendLine("<h2>Таблица пиков</h2>")
            appendLine("<table class=\"data\">")
            appendLine("<thead><tr>")
            appendLine("<th>#</th><th>RT (мин)</th><th>Высота</th><th>Площадь</th>")
            appendLine("<th>Area%</th><th>S/N</th><th>W<sub>base</sub></th><th>Confidence</th>")
            appendLine("</tr></thead>")
            appendLine("<tbody>")
            peaks.forEachIndexed { i, p ->
                val areaPercent = if (totalArea > 0) p.area / totalArea * 100.0 else 0.0
                val cls = when (p.confidence) {
                    ConfidenceGrade.HIGH -> "high"
                    ConfidenceGrade.MEDIUM -> "med"
                    ConfidenceGrade.LOW -> "low"
                    ConfidenceGrade.FAILED -> "fail"
                }
                appendLine("<tr>")
                appendLine("<td>${i + 1}</td>")
                appendLine("<td>${"%.3f".format(p.rtApex)}</td>")
                appendLine("<td>${formatNumber(p.height)}</td>")
                appendLine("<td>${formatNumber(p.area)}</td>")
                appendLine("<td>${"%.1f".format(areaPercent)}%</td>")
                appendLine("<td>${"%.1f".format(p.snr)}</td>")
                appendLine("<td>${"%.3f".format(p.widthBase)}</td>")
                appendLine("<td class=\"$cls\">${p.confidence.name}</td>")
                appendLine("</tr>")
            }
            appendLine("</tbody>")
            appendLine("<tfoot><tr>")
            appendLine("<td colspan=\"3\"><strong>Σ: ${peaks.size} пиков</strong></td>")
            appendLine("<td><strong>${formatNumber(totalArea)}</strong></td>")
            appendLine("<td><strong>100%</strong></td>")
            appendLine("<td colspan=\"3\"></td>")
            appendLine("</tr></tfoot>")
            appendLine("</table>")
            appendLine("</section>")

            // Section 3: System Suitability (USP)
            appendLine("<section>")
            appendLine("<h2>Системная пригодность (USP)</h2>")
            appendLine("<table class=\"data\">")
            appendLine("<thead><tr>")
            appendLine("<th>Пик</th><th>Tailing (T)</th><th>Asymmetry (As)</th>")
            appendLine("<th>Plates (N)</th><th>Resolution (Rs)</th>")
            appendLine("</tr></thead>")
            appendLine("<tbody>")
            peaks.forEachIndexed { i, p ->
                val tailCls = when {
                    p.tailingFactor > 2.0 -> "fail"
                    p.tailingFactor > 1.5 -> "med"
                    else -> ""
                }
                appendLine("<tr>")
                appendLine("<td>#${i + 1}</td>")
                appendLine("<td class=\"$tailCls\">${"%.3f".format(p.tailingFactor)}</td>")
                appendLine("<td>${"%.3f".format(p.asymmetryFactor)}</td>")
                appendLine("<td>${p.plateCount?.let { formatNumber(it.toDouble()) } ?: "—"}</td>")
                appendLine("<td>${p.resolution?.let { "%.2f".format(it) } ?: "—"}</td>")
                appendLine("</tr>")
            }
            appendLine("</tbody>")
            appendLine("</table>")
            appendLine("</section>")

            // Section 4: Quality
            val overlaps = peaks.count {
                it.overlapStatus != OverlapStatus.ISOLATED
            }
            if (overlaps > 0 || run.warnings.isNotEmpty()) {
                appendLine("<section>")
                appendLine("<h2>Качество</h2>")
                appendLine("<ul>")
                if (overlaps > 0) {
                    val shoulders = peaks.count { it.overlapStatus == OverlapStatus.SHOULDER }
                    val partial = peaks.count { it.overlapStatus == OverlapStatus.PARTIALLY_OVERLAPPED }
                    val unresolved = peaks.count { it.overlapStatus == OverlapStatus.UNRESOLVED }
                    appendLine("<li>Перекрытия: $shoulders shoulder, $partial partial, $unresolved unresolved</li>")
                }
                run.warnings.forEach { w ->
                    appendLine("<li class=\"warn-${w.severity.name.lowercase()}\">[${w.severity.name}] ${escHtml(w.message)}</li>")
                }
                appendLine("</ul>")
                appendLine("</section>")
            }

            // Section 5: Parameters
            appendLine("<section>")
            appendLine("<h2>Параметры алгоритма</h2>")
            appendLine("<table class=\"summary\">")
            val p = run.params
            summaryRow("Smoothing", if (p.smoothingEnabled) "SG(${p.smoothingWindowSize},${p.smoothingPolynomialOrder})" else "Off")
            summaryRow("Baseline", "${p.baselineMethod} (λ=${p.baselineLambda}, p=${p.baselineP}, iter=${p.baselineIterations})")
            summaryRow("Integration", p.integrationMethod)
            summaryRow("Noise", p.noiseMethod)
            summaryRow("Min S/N", "${"%.1f".format(p.minSnr)}")
            summaryRow("Min height", "${"%.1f".format(p.minPeakHeight)}")
            summaryRow("Min prominence", "${"%.1f".format(p.minPeakProminence)}")
            summaryRow("Min distance", "${p.minPeakDistance} pts")
            summaryRow("Min width", "${p.minPeakWidth} pts")
            appendLine("</table>")
            appendLine("</section>")

            // Footer
            appendLine("<footer>")
            appendLine("<p>Сгенерировано ChromaLab ${run.pipelineVersion} · ${run.algorithmVersion}</p>")
            appendLine("</footer>")

            appendLine("</body>")
            appendLine("</html>")
        }
    }

    private fun StringBuilder.summaryRow(label: String, value: String) {
        appendLine("<tr><td class=\"label\">$label</td><td>$value</td></tr>")
    }

    private fun formatNumber(value: Double): String = when {
        value >= 1_000_000 -> "%.2fM".format(value / 1_000_000)
        value >= 1_000 -> "%,.0f".format(value)
        else -> "%.2f".format(value)
    }

    private fun escHtml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private val CSS = """
<style>
  :root {
    --bg: #1a1a2e; --surface: #16213e; --border: #2a3a5c;
    --text: #e0e0e0; --text2: #8b99b0; --accent: #6366f1;
    --high: #4ade80; --med: #fbbf24; --low: #f87171; --fail: #ef4444;
  }
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body {
    font-family: 'Inter', 'Segoe UI', system-ui, sans-serif;
    background: var(--bg); color: var(--text);
    padding: 32px; max-width: 900px; margin: 0 auto;
    line-height: 1.6;
  }
  header { margin-bottom: 24px; border-bottom: 2px solid var(--accent); padding-bottom: 16px; }
  h1 { font-size: 1.4rem; color: var(--accent); }
  h2 { font-size: 1.1rem; margin: 20px 0 10px; color: var(--accent); border-bottom: 1px solid var(--border); padding-bottom: 4px; }
  .meta { color: var(--text2); font-size: 0.85rem; }
  section { margin-bottom: 16px; }
  table { width: 100%; border-collapse: collapse; font-size: 0.85rem; }
  table.summary td { padding: 4px 8px; border-bottom: 1px solid var(--border); }
  table.summary td.label { color: var(--text2); width: 40%; }
  table.data th, table.data td { padding: 6px 8px; text-align: left; border-bottom: 1px solid var(--border); }
  table.data th { background: var(--surface); color: var(--text2); font-weight: 600; }
  table.data tfoot td { border-top: 2px solid var(--border); }
  .high { color: var(--high); font-weight: 600; }
  .med { color: var(--med); }
  .low { color: var(--low); }
  .fail { color: var(--fail); font-weight: 700; }
  .warn-serious, .warn-failed { color: var(--fail); }
  .warn-caution { color: var(--med); }
  .warn-info { color: var(--text2); }
  footer { margin-top: 32px; padding-top: 12px; border-top: 1px solid var(--border); color: var(--text2); font-size: 0.75rem; text-align: center; }
  ul { padding-left: 20px; }
  li { margin: 4px 0; }

  @media print {
    :root { --bg: #fff; --surface: #f5f5f5; --border: #ddd; --text: #111; --text2: #666; --accent: #333; }
    body { padding: 0; font-size: 10pt; }
    h1 { font-size: 14pt; }
    h2 { font-size: 11pt; }
    table.data th { background: #eee; }
    footer { font-size: 8pt; }
  }
</style>
""".trimIndent()
}

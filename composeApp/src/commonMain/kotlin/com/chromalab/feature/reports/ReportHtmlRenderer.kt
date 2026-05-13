package com.chromalab.feature.reports

/**
 * Print-ready HTML renderer backed by the strict structured report contract.
 *
 * The HTML output intentionally reuses [ReportMarkdownRenderer] as its content source so Markdown,
 * HTML, and future PDF export keep the same section order and values.
 */
object ReportHtmlRenderer {

    fun render(report: ChromatogramReport): String {
        val markdown = ReportMarkdownRenderer.render(report)
        val title = report.documentTitle()

        return buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"en\">")
            appendLine("<head>")
            appendLine("<meta charset=\"utf-8\">")
            appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
            appendLine("<title>${title.escapeHtml()}</title>")
            appendLine(STYLE)
            appendLine("</head>")
            appendLine("<body>")
            appendLine("<article class=\"report\">")
            renderCover(report, title)
            appendLine("<main class=\"content\">")
            append(markdown.toHtmlBody())
            appendLine("</main>")
            appendLine("</article>")
            appendLine("</body>")
            appendLine("</html>")
        }
    }

    private fun StringBuilder.renderCover(report: ChromatogramReport, title: String) {
        val metadata = report.metadata
        appendLine("<header class=\"cover\">")
        appendLine("<div class=\"eyebrow\">ChromaLab offline analytical report</div>")
        appendLine("<h1>${title.escapeHtml()}</h1>")
        appendLine("<p class=\"cover-summary\">${report.summaryLine().escapeHtml()}</p>")
        appendLine("<div class=\"meta-grid\">")
        metaTile("Report ID", metadata.reportId)
        metaTile("Graphs", metadata.detectedGraphCount.toString())
        metaTile("Source", metadata.sourceName ?: metadata.inputSourceType.name)
        metaTile("Runtime", metadata.executedRuntime.name)
        metaTile("Model", metadata.executedModel.renderModelLabel())
        metaTile("Duration", metadata.totalAnalysisDurationMillis.renderDuration())
        metaTile("Device", metadata.deviceName ?: "not recorded")
        metaTile("Mode", metadata.processingMode.name)
        appendLine("</div>")
        appendLine("</header>")
    }

    private fun StringBuilder.metaTile(label: String, value: String) {
        appendLine("<section class=\"meta-tile\">")
        appendLine("<span>${label.escapeHtml()}</span>")
        appendLine("<strong>${value.escapeHtml()}</strong>")
        appendLine("</section>")
    }

    private fun String.toHtmlBody(): String {
        val lines = lines()
        val html = StringBuilder()
        var index = if (lines.firstOrNull()?.startsWith("# ") == true) 1 else 0

        while (index < lines.size) {
            val line = lines[index]
            when {
                line.isBlank() -> index++
                line.isTableRow() -> {
                    val tableLines = mutableListOf<String>()
                    while (index < lines.size && lines[index].isTableRow()) {
                        tableLines += lines[index]
                        index++
                    }
                    html.append(renderTable(tableLines))
                }
                line.isListItem() -> {
                    val items = mutableListOf<String>()
                    while (index < lines.size && lines[index].isListItem()) {
                        items += lines[index].trim().removePrefix("-").trim()
                        index++
                    }
                    html.appendLine("<ul>")
                    items.forEach { item ->
                        html.appendLine("<li>${item.inlineMarkdownToHtml()}</li>")
                    }
                    html.appendLine("</ul>")
                }
                line.startsWith("### ") -> {
                    html.appendLine("<h3>${line.removePrefix("### ").inlineMarkdownToHtml()}</h3>")
                    index++
                }
                line.startsWith("## ") -> {
                    html.appendLine("<h2>${line.removePrefix("## ").inlineMarkdownToHtml()}</h2>")
                    index++
                }
                line.startsWith("# ") -> {
                    html.appendLine("<h1>${line.removePrefix("# ").inlineMarkdownToHtml()}</h1>")
                    index++
                }
                else -> {
                    val paragraph = mutableListOf<String>()
                    while (index < lines.size && !lines[index].startsBlock()) {
                        paragraph += lines[index]
                        index++
                    }
                    val text = paragraph.joinToString(" ").trim()
                    if (text.isNotEmpty()) {
                        html.appendLine("<p>${text.inlineMarkdownToHtml()}</p>")
                    }
                }
            }
        }
        return html.toString()
    }

    private fun renderTable(lines: List<String>): String {
        val rows = lines
            .map { splitTableRow(it) }
            .filter { it.isNotEmpty() }
        if (rows.isEmpty()) return ""

        val hasHeader = rows.size > 1 && rows[1].isSeparatorRow()
        val header = if (hasHeader) rows.first() else emptyList()
        val body = if (hasHeader) rows.drop(2) else rows

        return buildString {
            appendLine("<div class=\"table-wrap\">")
            appendLine("<table>")
            if (header.isNotEmpty()) {
                appendLine("<thead>")
                appendLine("<tr>")
                header.forEach { cell -> appendLine("<th>${cell.inlineMarkdownToHtml()}</th>") }
                appendLine("</tr>")
                appendLine("</thead>")
            }
            appendLine("<tbody>")
            body.forEach { row ->
                appendLine("<tr>")
                row.forEach { cell -> appendLine("<td>${cell.inlineMarkdownToHtml()}</td>") }
                appendLine("</tr>")
            }
            appendLine("</tbody>")
            appendLine("</table>")
            appendLine("</div>")
        }
    }

    private fun splitTableRow(line: String): List<String> {
        val content = line.trim().removePrefix("|").removeSuffix("|")
        val cells = mutableListOf<String>()
        val cell = StringBuilder()
        var escaped = false

        content.forEach { char ->
            when {
                char == '|' && !escaped -> {
                    cells += cell.toString().trim().replace("\\|", "|")
                    cell.clear()
                }
                else -> {
                    cell.append(char)
                    escaped = char == '\\' && !escaped
                    if (char != '\\') escaped = false
                }
            }
        }
        cells += cell.toString().trim().replace("\\|", "|")
        return cells
    }

    private fun List<String>.isSeparatorRow(): Boolean =
        isNotEmpty() && all { cell ->
            cell.all { char -> char == '-' || char == ':' || char.isWhitespace() }
        }

    private fun String.startsBlock(): Boolean =
        isBlank() || startsWith("#") || isTableRow() || isListItem()

    private fun String.isTableRow(): Boolean =
        trimStart().startsWith("|")

    private fun String.isListItem(): Boolean =
        trimStart().startsWith("- ")

    private fun String.inlineMarkdownToHtml(): String =
        replace("\\|", "|").escapeHtml()

    private fun ChromatogramReport.documentTitle(): String =
        graphs.asSequence()
            .mapNotNull { it.identification.chromatogramTitle.value?.takeIf { value -> value.isNotBlank() } }
            .firstOrNull()
            ?: "ChromaLab chromatogram report"

    private fun ChromatogramReport.summaryLine(): String {
        val graphCount = metadata.detectedGraphCount
        val peakCount = graphs.sumOf { graph -> graph.quality.totalDetectedPeaks ?: graph.peaks.size }
        val model = metadata.executedModel.renderModelLabel()
        val duration = metadata.totalAnalysisDurationMillis.renderDuration()
        return "$graphCount graph(s), $peakCount peak(s), model $model, duration $duration"
    }

    private fun ModelExecutionInfo?.renderModelLabel(): String =
        this?.modelName?.takeIf { it.isNotBlank() }
            ?: this?.modelId?.takeIf { it.isNotBlank() }
            ?: "not recorded"

    private fun Long?.renderDuration(): String =
        when {
            this == null -> "not recorded"
            this < 1_000L -> "$this ms"
            this < 60_000L -> "${this / 1_000L}.${(this % 1_000L) / 100L} s"
            else -> "${this / 60_000L} min ${(this % 60_000L) / 1_000L} s"
        }

    private fun String.escapeHtml(): String =
        replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    private val STYLE = """
        <style>
          @page { size: A4; margin: 14mm; }
          :root {
            --page: #f6f8fb;
            --paper: #ffffff;
            --ink: #172033;
            --muted: #657084;
            --line: #d9e0ea;
            --soft: #eef3f8;
            --accent: #245b7d;
            --accent-soft: #e5f1f6;
          }
          * { box-sizing: border-box; }
          body {
            margin: 0;
            background: var(--page);
            color: var(--ink);
            font-family: Inter, "Segoe UI", Roboto, Arial, sans-serif;
            line-height: 1.5;
          }
          .report {
            width: min(1100px, calc(100% - 32px));
            margin: 24px auto;
            background: var(--paper);
            border: 1px solid var(--line);
            border-radius: 10px;
            box-shadow: 0 18px 50px rgba(27, 43, 65, 0.10);
            overflow: hidden;
          }
          .cover {
            padding: 32px 36px;
            border-bottom: 1px solid var(--line);
            background: linear-gradient(180deg, #ffffff 0%, #f7fbfd 100%);
          }
          .eyebrow {
            color: var(--accent);
            font-size: 12px;
            font-weight: 700;
            letter-spacing: 0;
            text-transform: uppercase;
          }
          h1, h2, h3 { letter-spacing: 0; line-height: 1.25; }
          h1 { margin: 8px 0 10px; font-size: 30px; }
          h2 {
            margin: 30px 0 12px;
            padding-bottom: 8px;
            border-bottom: 1px solid var(--line);
            color: var(--accent);
            font-size: 20px;
          }
          h3 { margin: 22px 0 10px; font-size: 16px; }
          .cover-summary { margin: 0 0 18px; color: var(--muted); }
          .meta-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
            gap: 10px;
          }
          .meta-tile {
            min-height: 72px;
            padding: 10px 12px;
            border: 1px solid var(--line);
            border-radius: 8px;
            background: rgba(255, 255, 255, 0.78);
          }
          .meta-tile span {
            display: block;
            margin-bottom: 4px;
            color: var(--muted);
            font-size: 11px;
            font-weight: 700;
            text-transform: uppercase;
          }
          .meta-tile strong {
            display: block;
            font-size: 13px;
            overflow-wrap: anywhere;
          }
          .content { padding: 0 36px 36px; }
          p { margin: 8px 0 14px; }
          ul { margin: 8px 0 14px; padding-left: 22px; }
          li { margin: 4px 0; }
          .table-wrap {
            width: 100%;
            margin: 10px 0 18px;
            overflow-x: auto;
            border: 1px solid var(--line);
            border-radius: 8px;
          }
          table {
            width: 100%;
            border-collapse: collapse;
            font-size: 13px;
          }
          th, td {
            padding: 8px 10px;
            border-bottom: 1px solid var(--line);
            text-align: left;
            vertical-align: top;
          }
          th {
            background: var(--soft);
            color: #344154;
            font-weight: 700;
          }
          tr:last-child td { border-bottom: 0; }
          tbody tr:nth-child(even) td { background: #fbfdff; }

          @media print {
            body { background: #ffffff; }
            .report {
              width: 100%;
              margin: 0;
              border: 0;
              border-radius: 0;
              box-shadow: none;
            }
            .cover { padding: 0 0 14px; background: #ffffff; }
            .content { padding: 0; }
            h1 { font-size: 22pt; }
            h2 { break-after: avoid; font-size: 15pt; }
            h3 { break-after: avoid; font-size: 12pt; }
            table { font-size: 8.5pt; }
            tr, .meta-tile { break-inside: avoid; }
            .table-wrap { overflow: visible; border-radius: 0; }
          }
        </style>
    """.trimIndent()
}

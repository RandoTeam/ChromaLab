package com.chromalab.feature.processing.bench

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object OfflineAnalysisAuditArtifacts {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    fun toJson(audit: OfflineAnalysisAudit): String =
        json.encodeToString(audit)

    fun toSummaryMarkdown(audit: OfflineAnalysisAudit): String = buildString {
        appendLine("# ChromaLab Offline Analysis Audit")
        appendLine()
        appendLine("| Field | Value |")
        appendLine("| --- | --- |")
        appendLine("| Source | `${audit.sourceId}` |")
        appendLine("| Image size | ${audit.imageWidth ?: "unknown"} x ${audit.imageHeight ?: "unknown"} |")
        appendLine("| Expected graphs | ${audit.expectedGraphCount ?: "not specified"} |")
        appendLine("| Detected graphs | ${audit.detectedGraphCount} |")
        appendLine("| Ready for calculation | ${audit.readyForCalculation} |")
        appendLine("| Blocked at stage | ${audit.blockedAtStage ?: "not blocked"} |")
        appendLine()

        appendLine("## Stage Timeline")
        appendLine()
        appendLine("| Stage | Graph | Status | Duration | Message |")
        appendLine("| --- | ---: | --- | ---: | --- |")
        audit.stages.forEach { stage ->
            appendLine(
                "| `${stage.stage}` | ${stage.graphIndex ?: ""} | ${stage.status} | ${stage.durationMillis} ms | ${stage.message.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Graph Candidates")
        appendLine()
        appendLine("| # | Region | Accepted | Area | Aspect | Rejection reasons |")
        appendLine("| ---: | --- | --- | ---: | ---: | --- |")
        audit.graphCandidates.forEach { candidate ->
            appendLine(
                "| ${candidate.graphIndex} | ${candidate.region.renderRegion()} | ${candidate.accepted} | ${candidate.areaRatio.renderPercent()} | ${candidate.aspectRatio.renderNumber()} | ${candidate.rejectionReasons.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Per-Graph Audit")
        appendLine()
        appendLine("| Graph | Region | Prep variant | OCR | X ticks | Y ticks | Axes | Curve points | Curve coverage | Curve usable |")
        appendLine("| ---: | --- | --- | --- | ---: | ---: | --- | ---: | ---: | --- |")
        audit.graphs.forEach { graph ->
            appendLine(
                "| ${graph.graphIndex} | ${graph.region.renderRegion()} | ${graph.selectedPreprocessingVariant ?: "none"} | ${graph.ocrStatus} | ${graph.xSuggestionCount} | ${graph.ySuggestionCount} | ${graph.axesDetected} | ${graph.curvePointCount} | ${graph.curveCoverage.renderPercent()} | ${graph.curveUsable} |",
            )
        }
        appendLine()

        appendLine("## Preprocessing Variant Ranking")
        appendLine()
        appendLine("| Graph | Rank | Selected | Variant | Score | Dark pixels | Edges | Contrast | H-lines | V-lines | Warnings |")
        appendLine("| ---: | ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- |")
        audit.graphs.forEach { graph ->
            graph.preprocessingVariantScores.forEach { variant ->
                appendLine(
                    "| ${graph.graphIndex} | ${variant.rank} | ${variant.selected} | ${variant.variantId} | ${variant.score.renderNumber()} | ${variant.darkPixelRatio.renderPercent()} | ${variant.edgeDensity.renderPercent()} | ${variant.contrast.renderPercent()} | ${variant.horizontalLineStrength.renderPercent()} | ${variant.verticalLineStrength.renderPercent()} | ${variant.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
                )
            }
        }
        if (audit.graphs.all { it.preprocessingVariantScores.isEmpty() }) {
            appendLine("|  |  |  | none |  |  |  |  |  |  | no preprocessing variant scores |")
        }
        appendLine()

        appendLine("## Warnings")
        appendLine()
        if (audit.warnings.isEmpty()) {
            appendLine("- none")
        } else {
            audit.warnings.forEach { warning ->
                appendLine("- `$warning`")
            }
        }
    }
}

private fun com.chromalab.feature.processing.graph.GraphRegion.renderRegion(): String =
    "${width}x${height} @ ${x},${y}"

private fun Float.renderPercent(): String =
    "${(this * 100f).renderNumber()}%"

private fun Float.renderNumber(): String =
    when {
        this.isNaN() || this.isInfinite() -> "n/a"
        this >= 100f -> this.toInt().toString()
        this >= 10f -> "%.1f".format(this)
        else -> "%.3f".format(this)
    }

private fun String.escapeTable(): String =
    replace("|", "\\|").replace("\n", " ")

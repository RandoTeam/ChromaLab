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
        appendLine("| Orientation corrected | ${audit.orientationCorrection?.wasRotated ?: false} |")
        appendLine("| Orientation rotation | ${audit.orientationCorrection?.rotationDegrees ?: 0} deg |")
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
        appendLine("| Graph | Region | Plot area | Crop QA | Boundary QA | Prep variant | OCR | X ticks | Y ticks | Axes | Axis conf. | Calibration | Mask pixels | Curve points | Curve coverage | Curve usable | Signal ready |")
        appendLine("| ---: | --- | --- | --- | --- | --- | --- | ---: | ---: | --- | ---: | --- | ---: | ---: | ---: | --- | --- |")
        audit.graphs.forEach { graph ->
            appendLine(
                "| ${graph.graphIndex} | ${graph.region.renderRegion()} | ${graph.plotArea.region?.renderRegion() ?: "not detected"} | ${graph.cropQuality.acceptedForCalculation} | ${graph.cropBoundaryRisk.acceptedForCalculation} | ${graph.selectedPreprocessingVariant ?: "none"} | ${graph.ocrStatus} | ${graph.xSuggestionCount} | ${graph.ySuggestionCount} | ${graph.axesDetected} | ${graph.axisConfidence.renderNumber()} | ${graph.axisCalibration.ready} | ${graph.curveMaskCleanPixelCount} | ${graph.curvePointCount} | ${graph.curveCoverage.renderPercent()} | ${graph.curveUsable} | ${graph.signal.ready} |",
            )
        }
        appendLine()

        appendLine("## Graph Refinement")
        appendLine()
        appendLine("| Graph | Original region | Refined region | Changed | Area reduction | Warnings |")
        appendLine("| ---: | --- | --- | --- | ---: | --- |")
        audit.graphs.forEach { graph ->
            appendLine(
                "| ${graph.graphIndex} | ${graph.originalRegion.renderRegion()} | ${graph.region.renderRegion()} | ${graph.refinement.changed} | ${graph.refinement.areaReductionRatio.renderPercent()} | ${graph.refinement.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Crop Quality")
        appendLine()
        appendLine("| Graph | Area | Original area | Edge contacts | Full image | Broad edge crop | Unresolved broad context | Rotated/page risk | 90-degree risk | Calculation-ready | Warnings |")
        appendLine("| ---: | ---: | ---: | ---: | --- | --- | --- | --- | --- | --- | --- |")
        audit.graphs.forEach { graph ->
            appendLine(
                "| ${graph.graphIndex} | ${graph.cropQuality.areaRatio.renderPercent()} | ${graph.cropQuality.originalAreaRatio.renderPercent()} | ${graph.cropQuality.edgeContactCount} | ${graph.cropQuality.fullImage} | ${graph.cropQuality.broadEdgeCrop} | ${graph.cropQuality.unresolvedBroadContext} | ${graph.cropQuality.possibleRotatedPage} | ${graph.cropQuality.rightAngleRotationSuspected} | ${graph.cropQuality.acceptedForCalculation} | ${graph.cropQuality.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Crop Boundary Risk")
        appendLine()
        appendLine("| Graph | Top clipping risk | Top dark runs | Top dark pixels | Calculation-ready | Warnings |")
        appendLine("| ---: | --- | ---: | ---: | --- | --- |")
        audit.graphs.forEach { graph ->
            appendLine(
                "| ${graph.graphIndex} | ${graph.cropBoundaryRisk.topSignalClippingRisk} | ${graph.cropBoundaryRisk.topTouchingDarkRunCount} | ${graph.cropBoundaryRisk.topDarkPixelRatio.renderPercent()} | ${graph.cropBoundaryRisk.acceptedForCalculation} | ${graph.cropBoundaryRisk.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Plot Area")
        appendLine()
        appendLine("| Graph | Panel region | Plot region | Detected | Area inside panel | Warnings |")
        appendLine("| ---: | --- | --- | --- | ---: | --- |")
        audit.graphs.forEach { graph ->
            appendLine(
                "| ${graph.graphIndex} | ${graph.region.renderRegion()} | ${graph.plotArea.region?.renderRegion() ?: "not detected"} | ${graph.plotArea.detected} | ${graph.plotArea.areaRatioWithinPanel.renderPercent()} | ${graph.plotArea.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Axis Calibration")
        appendLine()
        appendLine("| Graph | Ready | Source | X points | Y points | X pixel span | Y pixel span | X value span | Y value span | Units | Warnings |")
        appendLine("| ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |")
        audit.graphs.forEach { graph ->
            val calibration = graph.axisCalibration
            val units = listOfNotNull(calibration.xUnit?.let { "X=$it" }, calibration.yUnit?.let { "Y=$it" })
                .joinToString("; ")
                .ifBlank { "unknown" }
            appendLine(
                "| ${graph.graphIndex} | ${calibration.ready} | ${calibration.source} | ${calibration.xCandidateCount} | ${calibration.yCandidateCount} | ${calibration.xPixelSpan.renderNumber()} | ${calibration.yPixelSpan.renderNumber()} | ${calibration.xValueSpan.renderNumber()} | ${calibration.yValueSpan.renderNumber()} | ${units.escapeTable()} | ${calibration.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Curve Mask")
        appendLine()
        appendLine("| Graph | Available | Raw pixels | Clean pixels | Suppression ratio | Suppression passes |")
        appendLine("| ---: | --- | ---: | ---: | ---: | --- |")
        audit.graphs.forEach { graph ->
            val ratio = if (graph.curveMaskRawPixelCount > 0) {
                graph.curveMaskCleanPixelCount.toFloat() / graph.curveMaskRawPixelCount.toFloat()
            } else {
                0f
            }
            appendLine(
                "| ${graph.graphIndex} | ${graph.curveMaskAvailable} | ${graph.curveMaskRawPixelCount} | ${graph.curveMaskCleanPixelCount} | ${ratio.renderPercent()} | ${graph.curveMaskSuppressionApplied.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Signal Conversion")
        appendLine()
        appendLine("| Graph | Ready | Points | Time start | Time end | Time range | Intensity min | Intensity max | Intensity range | Duplicates | Gaps | Sort valid | Warnings |")
        appendLine("| ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |")
        audit.graphs.forEach { graph ->
            val signal = graph.signal
            appendLine(
                "| ${graph.graphIndex} | ${signal.ready} | ${signal.pointCount} | ${signal.timeStart?.renderNumber() ?: "n/a"} | ${signal.timeEnd?.renderNumber() ?: "n/a"} | ${signal.timeRange.renderNumber()} | ${signal.intensityMin?.renderNumber() ?: "n/a"} | ${signal.intensityMax?.renderNumber() ?: "n/a"} | ${signal.intensityRange.renderNumber()} | ${signal.duplicateCount} | ${signal.gapCount} | ${signal.sortValid} | ${signal.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
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

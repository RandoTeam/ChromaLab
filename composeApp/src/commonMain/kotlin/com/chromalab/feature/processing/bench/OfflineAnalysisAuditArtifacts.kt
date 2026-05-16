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
        appendLine("| Graph | Region | Plot area | Crop QA | Boundary QA | Prep variant | OCR | X ticks | Y ticks | Axes | Axis conf. | Calibration | Mask pixels | Curve points | Curve coverage | Curve usable | Signal ready | Peak ready | Metrics ready | Sanity ready |")
        appendLine("| ---: | --- | --- | --- | --- | --- | --- | ---: | ---: | --- | ---: | --- | ---: | ---: | ---: | --- | --- | --- | --- | --- |")
        audit.graphs.forEach { graph ->
            appendLine(
                "| ${graph.graphIndex} | ${graph.region.renderRegion()} | ${graph.plotArea.region?.renderRegion() ?: "not detected"} | ${graph.cropQuality.acceptedForCalculation} | ${graph.cropBoundaryRisk.acceptedForCalculation} | ${graph.selectedPreprocessingVariant ?: "none"} | ${graph.ocrStatus} | ${graph.xSuggestionCount} | ${graph.ySuggestionCount} | ${graph.axesDetected} | ${graph.axisConfidence.renderNumber()} | ${graph.axisCalibration.ready} | ${graph.curveMaskCleanPixelCount} | ${graph.curvePointCount} | ${graph.curveCoverage.renderPercent()} | ${graph.curveUsable} | ${graph.signal.ready} | ${graph.peakDetection.ready} | ${graph.peakMetrics.ready} | ${graph.peakSanity.ready} |",
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

        appendLine("## Trace Artifact Diagnostics")
        appendLine()
        appendLine("| Graph | Available | Artifact pixels | Artifact ratio | Hypothesis pixels | Hypothesis retained | Hypothesis coverage | Relaxation allowed | Floating components | Floating pixels | Vertical lines | Horizontal lines | Top-band components | Baseline row | Warnings | Hypothesis warnings |")
        appendLine("| ---: | --- | ---: | ---: | ---: | ---: | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- |")
        audit.graphs.forEach { graph ->
            val artifacts = graph.traceArtifacts
            appendLine(
                "| ${graph.graphIndex} | ${artifacts.available} | ${artifacts.artifactPixelCount} | ${artifacts.artifactPixelRatio.renderPercent()} | ${artifacts.cleanupHypothesisPixelCount} | ${artifacts.cleanupHypothesisRetainedRatio.renderPercent()} | ${artifacts.cleanupHypothesisColumnCoverage.renderPercent()} | ${artifacts.thresholdRelaxationAllowed} | ${artifacts.floatingComponentCount} | ${artifacts.floatingPixelCount} | ${artifacts.verticalLineComponentCount} | ${artifacts.horizontalLineComponentCount} | ${artifacts.topBandComponentCount} | ${artifacts.baselineRow ?: "n/a"} | ${artifacts.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} | ${artifacts.cleanupHypothesisWarnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
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

        appendLine("## Peak Detection")
        appendLine()
        appendLine("| Graph | Ready | Profile | Peaks | Base peaks | Tuned peaks | Significant | Dominant time | Dominant height | Dominant area | Baseline | Boundary | Integration | Clamp negative | Max width | Min S/N | Warnings |")
        appendLine("| ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- | --- | --- | ---: | ---: | --- |")
        audit.graphs.forEach { graph ->
            val peaks = graph.peakDetection
            appendLine(
                "| ${graph.graphIndex} | ${peaks.ready} | ${peaks.detectionProfile ?: "n/a"} | ${peaks.peakCount} | ${peaks.basePeakCount ?: "n/a"} | ${peaks.tunedPeakCount ?: "n/a"} | ${peaks.significantPeakCount} | ${peaks.dominantPeakTime?.renderNumber() ?: "n/a"} | ${peaks.dominantPeakHeight?.renderNumber() ?: "n/a"} | ${peaks.dominantPeakAreaPercent?.renderNumber() ?: "n/a"} | ${peaks.baselineMethod ?: "n/a"} | ${peaks.boundaryMethod ?: "n/a"} | ${peaks.integrationMethod ?: "n/a"} | ${peaks.clampNegative ?: "n/a"} | ${peaks.maxPeakWidth ?: "n/a"} | ${peaks.minSnr?.renderNumber() ?: "n/a"} | ${peaks.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Peak Candidate Diagnostics")
        appendLine()
        appendLine("| Graph | Detection signal | Noise | Noise method | Controlled tuning | Tuning reason | Threshold relaxation | Guard reason | Candidates | Rejected | Top rejection reasons |")
        appendLine("| ---: | --- | ---: | --- | --- | --- | --- | --- | ---: | ---: | --- |")
        audit.graphs.forEach { graph ->
            val peaks = graph.peakDetection
            appendLine(
                "| ${graph.graphIndex} | ${peaks.detectionSignalSource ?: "n/a"} | ${peaks.noiseLevel?.renderNumber() ?: "n/a"} | ${peaks.noiseMethod ?: "n/a"} | ${peaks.controlledTuningApplied} | ${(peaks.controlledTuningReason ?: "none").escapeTable()} | ${peaks.thresholdRelaxationAllowed ?: "n/a"} | ${(peaks.thresholdRelaxationGuardReason ?: "none").escapeTable()} | ${peaks.candidateCount ?: "n/a"} | ${peaks.rejectedCandidateCount ?: "n/a"} | ${peaks.rejectionReasons.renderRejectionReasons().escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Guarded Peak Quality")
        appendLine()
        appendLine("| Graph | Available | Review peaks | Accepted | Low default S/N | Low area share | Narrow boundary | Warnings |")
        appendLine("| ---: | --- | ---: | --- | ---: | ---: | ---: | --- |")
        audit.graphs.forEach { graph ->
            val quality = graph.peakDetection.guardedQualityReview
            appendLine(
                "| ${graph.graphIndex} | ${quality.available} | ${quality.reviewPeakCount} | ${quality.acceptedForGuardedCompleteness} | ${quality.lowDefaultSnrCount} | ${quality.lowAreaShareCount} | ${quality.narrowBoundaryCount} | ${quality.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Sparse Trace Peak Quality")
        appendLine()
        appendLine("| Graph | Available | Sparse | Localized | Review peaks | Low S/N | Low area share | Low confidence | Overlap review | Report confidence text | Warnings |")
        appendLine("| ---: | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |")
        audit.graphs.forEach { graph ->
            val quality = graph.peakDetection.sparseTraceQualityReview
            appendLine(
                "| ${graph.graphIndex} | ${quality.available} | ${quality.sparseTrace} | ${quality.localizedSparseTrace} | ${quality.reviewPeakCount} | ${quality.lowSnrCount} | ${quality.lowAreaShareCount} | ${quality.lowConfidenceCount} | ${quality.overlapReviewCount} | ${quality.requiresReportConfidenceText} | ${quality.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Peak Table Snapshot")
        appendLine()
        appendLine("| Graph | Peak | RT apex | Left | Right | Width | FWHM | Tailing | Asymmetry | Height | Area | Area % | S/N | Confidence | Overlap | Compound status | Compound | Formula status | Carbon status | Kovats status | Quality flags | Warnings |")
        appendLine("| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- | --- | --- | --- | --- | --- | --- | ---: |")
        audit.graphs.forEach { graph ->
            graph.peakDetection.peaks.forEach { peak ->
                appendLine(
                    "| ${graph.graphIndex} | ${peak.peakNumber} | ${peak.rtApex.renderNumber()} | ${peak.leftBoundaryTime.renderNumber()} | ${peak.rightBoundaryTime.renderNumber()} | ${peak.widthBase.renderNumber()} | ${peak.widthHalfHeight?.renderNumber() ?: "not calculated"} | ${peak.tailingFactor.renderNumber()} | ${peak.asymmetryFactor.renderNumber()} | ${peak.height.renderNumber()} | ${peak.area.renderNumber()} | ${peak.areaPercent.renderNumber()} | ${peak.snr.renderNumber()} | ${peak.confidence} | ${peak.overlapStatus} | ${peak.assignment.probableCompoundStatus} | ${(peak.assignment.probableCompoundName ?: "not calculated").escapeTable()} | ${peak.assignment.formulaStatus} | ${peak.assignment.carbonNumberStatus} | ${peak.assignment.kovatsIndexStatus} | ${peak.qualityFlags.joinToString("; ").ifBlank { "none" }.escapeTable()} | ${peak.warningCount} |",
                )
            }
        }
        if (audit.graphs.all { it.peakDetection.peaks.isEmpty() }) {
            appendLine("|  |  |  |  |  |  |  |  |  |  |  |  |  | none | none | NOT_CALCULATED | not calculated | NOT_CALCULATED | NOT_CALCULATED | NOT_CALCULATED | none |  |")
        }
        appendLine()

        appendLine("## Peak Metrics Review")
        appendLine()
        appendLine("| Graph | Ready | RT order | Total area | Area % sum | Max height | First RT | Last RT | Min width | Max width | Invalid nums | Invalid bounds | Non-positive area | Non-positive height | Missing width | Low S/N | Low confidence | Overlap review | Peak warnings | Warnings |")
        appendLine("| ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |")
        audit.graphs.forEach { graph ->
            val metrics = graph.peakMetrics
            appendLine(
                "| ${graph.graphIndex} | ${metrics.ready} | ${metrics.orderedByRetentionTime} | ${metrics.totalAbsArea.renderNumber()} | ${metrics.areaPercentSum.renderNumber()} | ${metrics.maximumHeight?.renderNumber() ?: "n/a"} | ${metrics.firstPeakTime?.renderNumber() ?: "n/a"} | ${metrics.lastPeakTime?.renderNumber() ?: "n/a"} | ${metrics.minBoundaryWidth?.renderNumber() ?: "n/a"} | ${metrics.maxBoundaryWidth?.renderNumber() ?: "n/a"} | ${metrics.invalidNumericCount} | ${metrics.invalidBoundaryCount} | ${metrics.nonPositiveAreaCount} | ${metrics.nonPositiveHeightCount} | ${metrics.missingWidthCount} | ${metrics.lowSnrCount} | ${metrics.lowConfidenceCount} | ${metrics.unresolvedOverlapCount} | ${metrics.peakWarningCount} | ${metrics.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Peak Sanity")
        appendLine()
        appendLine("| Graph | Ready | Expectations | Min peaks | Expected apexes | Matched | Missing apexes | Unexpected | Tolerance | Warnings |")
        appendLine("| ---: | --- | --- | ---: | --- | ---: | --- | ---: | ---: | --- |")
        audit.graphs.forEach { graph ->
            val sanity = graph.peakSanity
            appendLine(
                "| ${graph.graphIndex} | ${sanity.ready} | ${sanity.expectationProvided} | ${sanity.minPeakCount ?: "n/a"} | ${sanity.expectedApexTimes.joinToString(", ") { it.renderNumber() }.ifBlank { "none" }} | ${sanity.detectedExpectedPeakCount} | ${sanity.missingExpectedApexTimes.joinToString(", ") { it.renderNumber() }.ifBlank { "none" }} | ${sanity.unexpectedPeakCount} | ${sanity.apexTolerance.renderNumber()} | ${sanity.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Structured Report Contract")
        appendLine()
        appendLine("| Ready | Graphs | Sections | Ready sections | Warning sections | Blocked sections | Warnings |")
        appendLine("| --- | ---: | ---: | ---: | ---: | ---: | --- |")
        val report = audit.reportContract
        appendLine(
            "| ${report.ready} | ${report.graphCount} | ${report.sectionCount} | ${report.readySectionCount} | ${report.warningSectionCount} | ${report.blockedSectionCount} | ${report.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
        )
        appendLine()

        appendLine("## Structured Report Sections")
        appendLine()
        appendLine("| Graph | Section | Status | Missing fields | Warnings |")
        appendLine("| ---: | --- | --- | --- | --- |")
        report.sections.forEach { section ->
            appendLine(
                "| ${section.graphIndex ?: ""} | ${section.section} | ${section.status} | ${section.missingFields.joinToString("; ").ifBlank { "none" }.escapeTable()} | ${section.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        if (report.sections.isEmpty()) {
            appendLine("|  | none | BLOCKED | report_validation_not_available | none |")
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

private fun Double.renderNumber(): String =
    when {
        isNaN() || isInfinite() -> "n/a"
        this >= 100.0 -> toInt().toString()
        this >= 10.0 -> "%.1f".format(this)
        else -> "%.3f".format(this)
    }

private fun List<OfflinePeakRejectionAudit>.renderRejectionReasons(): String =
    joinToString("; ") { "${it.reason}:${it.count}" }.ifBlank { "none" }

private fun String.escapeTable(): String =
    replace("|", "\\|").replace("\n", " ")

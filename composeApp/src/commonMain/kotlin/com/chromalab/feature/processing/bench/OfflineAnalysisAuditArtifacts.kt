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

    fun toCalibratedReportUiContractJson(audit: OfflineAnalysisAudit): String =
        json.encodeToString(OfflineCalibratedReportUiContractBuilder.build(audit))

    fun toCalibratedReportMarkdown(audit: OfflineAnalysisAudit): String = buildString {
        appendLine("# ChromaLab Calibrated Chromatogram Report")
        appendLine()
        appendLine("## Overview")
        appendLine()
        appendLine("| Field | Value |")
        appendLine("| --- | --- |")
        reportRow("Source", audit.sourceId)
        reportRow("Image size", audit.imageWidth?.let { width -> "${width} x ${audit.imageHeight ?: "unknown"}" } ?: "unknown")
        reportRow("Detected graphs", audit.detectedGraphCount.toString())
        reportRow("Expected graphs", audit.expectedGraphCount?.toString() ?: "not specified")
        reportRow("Report readiness", if (audit.reportContract.ready) "ready" else "not ready")
        reportRow("Blocked stage", audit.blockedAtStage?.humanizeCode() ?: "not blocked")
        reportRow("Orientation correction", if (audit.orientationCorrection?.wasRotated == true) {
            "${audit.orientationCorrection.rotationDegrees} deg"
        } else {
            "not required"
        })
        reportRow("Stage duration", "${audit.stages.sumOf { it.durationMillis }} ms")
        appendLine()

        appendLine("## Key Warnings")
        appendLine()
        renderHumanWarningList((audit.reportContract.warnings + audit.warnings).distinct())
        appendLine()

        audit.graphs.sortedBy { it.graphIndex }.forEach { graph ->
            appendLine("## Graph ${graph.graphIndex} Report")
            appendLine()
            renderGraphPreparation(graph)
            renderVisualEvidence(graph)
            renderAxisCalibration(graph)
            renderPeakTable(graph)
            renderChromatographicQuality(graph)
            renderKovatsAndInterpretation(graph)
            renderSectionReadiness(audit, graph)
        }

        appendLine("## Technical Appendix")
        appendLine()
        renderRawWarningCodes(audit)
        renderStageTimeline(audit)
        renderRawReportSections(audit)
    }

    fun toSummaryMarkdown(audit: OfflineAnalysisAudit): String = buildString {
        appendLine("# ChromaLab Offline Analysis Audit")
        appendLine()
        appendLine("| Field | Value |")
        appendLine("| --- | --- |")
        appendLine("| Source | `${audit.sourceId}` |")
        appendLine("| Image size | ${audit.imageWidth ?: "unknown"} x ${audit.imageHeight ?: "unknown"} |")
        appendLine("| Orientation corrected | ${audit.orientationCorrection?.wasRotated ?: false} |")
        appendLine("| Orientation rotation | ${audit.orientationCorrection?.rotationDegrees ?: 0} deg |")
        appendLine("| Perspective geometry ready | ${audit.perspectiveGeometry.ready} |")
        appendLine("| Perspective document trusted | ${audit.perspectiveGeometry.documentTrusted} |")
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

        appendLine("## Perspective Geometry Contract")
        appendLine()
        appendLine("| Field | Value |")
        appendLine("| --- | --- |")
        val perspective = audit.perspectiveGeometry
        appendLine("| Ready | ${perspective.ready} |")
        appendLine("| Image size | ${perspective.imageWidth ?: "unknown"} x ${perspective.imageHeight ?: "unknown"} |")
        appendLine("| Document detected | ${perspective.documentDetected} |")
        appendLine("| Document trusted | ${perspective.documentTrusted} |")
        appendLine("| Document method | ${perspective.documentMethod ?: "n/a"} |")
        appendLine("| Document confidence | ${perspective.documentConfidence.renderNumber()} |")
        appendLine("| Document area | ${perspective.documentAreaRatio.renderPercent()} |")
        appendLine("| Document aspect | ${perspective.documentAspectRatio.renderNumber()} |")
        appendLine("| Graph panels | ${perspective.graphPanelCount} |")
        appendLine("| Plot areas | ${perspective.plotAreaCount} |")
        appendLine("| Plot geometry ready | ${perspective.plotGeometryReady} |")
        appendLine("| Perspective required | ${perspective.perspectiveTransformRequired} |")
        appendLine("| Perspective applied | ${perspective.perspectiveApplied} |")
        appendLine("| Corner displacement | ${perspective.normalizedCornerDisplacement.renderPercent()} |")
        appendLine("| Max skew angle | ${perspective.maxSkewAngleDegrees.renderNumber()} deg |")
        appendLine("| Residual metrics required | ${perspective.residualMetricsRequired} |")
        appendLine("| Warnings | ${perspective.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |")
        appendLine()
        appendLine("| Residual candidate count | ${perspective.residualMetrics.candidateCount} |")
        appendLine("| Residual accepted candidates | ${perspective.residualMetrics.acceptedCandidateCount} |")
        appendLine("| Residual accepted plots | ${perspective.residualMetrics.acceptedPlotAreaCandidateCount} |")
        appendLine("| Residual max corner displacement | ${perspective.residualMetrics.maxNormalizedCornerDisplacement.renderPercent()} |")
        appendLine("| Residual max skew | ${perspective.residualMetrics.maxSkewAngleDegrees.renderNumber()} deg |")
        appendLine("| Residual max orthogonality | ${perspective.residualMetrics.maxOrthogonalityResidualDegrees.renderNumber()} deg |")
        appendLine("| Residual quality ready | ${perspective.residualMetrics.residualQualityReady} |")
        appendLine("| Residual warnings | ${perspective.residualMetrics.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |")
        appendLine()

        appendLine("## Perspective Quadrilateral Candidates")
        appendLine()
        appendLine("| Kind | Graph | Source | Bounds | Accepted | Area | Aspect | Corner residual | Skew | Orthogonality | Straightness | Score | Warnings |")
        appendLine("| --- | ---: | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |")
        perspective.candidates.forEach { candidate ->
            appendLine(
                "| ${candidate.kind} | ${candidate.graphIndex ?: ""} | ${candidate.source.escapeTable()} | ${candidate.bounds.renderRegion()} | ${candidate.accepted} | ${candidate.areaRatio.renderPercent()} | ${candidate.aspectRatio.renderNumber()} | ${candidate.normalizedCornerDisplacement.renderPercent()} | ${candidate.maxSkewAngleDegrees.renderNumber()} | ${candidate.orthogonalityResidualDegrees.renderNumber()} | ${candidate.maxSideStraightnessResidualPx.renderNumber()} | ${candidate.score.renderNumber()} | ${candidate.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
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
        appendLine("| Graph | Panel region | Plot region | Detected | Status | Area inside panel | Text contamination | Text elements | Warnings |")
        appendLine("| ---: | --- | --- | --- | --- | ---: | ---: | ---: | --- |")
        audit.graphs.forEach { graph ->
            appendLine(
                "| ${graph.graphIndex} | ${graph.region.renderRegion()} | ${graph.plotArea.region?.renderRegion() ?: "not detected"} | ${graph.plotArea.detected} | ${graph.plotArea.status} | ${graph.plotArea.areaRatioWithinPanel.renderPercent()} | ${graph.plotArea.textContaminationScore.renderNumber()} | ${graph.plotArea.textElementCountInsidePlot} | ${graph.plotArea.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Axis And Tick Geometry")
        appendLine()
        appendLine("| Graph | Available | Source | Plot region | Lines | H lines | V lines | X ticks | Y ticks | OCR matching-ready | Warnings |")
        appendLine("| ---: | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |")
        audit.graphs.forEach { graph ->
            val geometry = graph.axisTickGeometry
            appendLine(
                "| ${graph.graphIndex} | ${geometry.available} | ${geometry.source.escapeTable()} | ${geometry.plotRegion?.renderRegion() ?: "not detected"} | ${geometry.lineSegmentCount} | ${geometry.horizontalLineCount} | ${geometry.verticalLineCount} | ${geometry.xTickCount} | ${geometry.yTickCount} | ${geometry.readyForOcrValueMatching} | ${geometry.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Axis Calibration")
        appendLine()
        appendLine("| Graph | Ready | Source | X points | Y points | X pixel span | Y pixel span | X value span | Y value span | X residual | Y residual | Residual fit | Units | Warnings |")
        appendLine("| ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- | --- |")
        audit.graphs.forEach { graph ->
            val calibration = graph.axisCalibration
            val units = listOfNotNull(calibration.xUnit?.let { "X=$it" }, calibration.yUnit?.let { "Y=$it" })
                .joinToString("; ")
                .ifBlank { "unknown" }
            appendLine(
                "| ${graph.graphIndex} | ${calibration.ready} | ${calibration.source} | ${calibration.xCandidateCount} | ${calibration.yCandidateCount} | ${calibration.xPixelSpan.renderNumber()} | ${calibration.yPixelSpan.renderNumber()} | ${calibration.xValueSpan.renderNumber()} | ${calibration.yValueSpan.renderNumber()} | ${calibration.xFitResidual.renderNumber()} | ${calibration.yFitResidual.renderNumber()} | ${calibration.residualFitReady} | ${units.escapeTable()} | ${calibration.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
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

        appendLine("## Trace Centerline")
        appendLine()
        appendLine("| Graph | Available | Method | Selected | Decision | Overlay | Matched | Match ratio | Median delta | P95 delta | Max delta | Large delta threshold | Large delta columns | Large delta ratio | Branch-near large | Signal above centerline | Signal below centerline | Branch-pruned method | Branch-pruned decision | Branch-pruned overlay | Branch-pruned removed | Branch-pruned interpolated | Branch-pruned matched | Branch-pruned P95 | Branch-pruned large | Branch-pruned P95 improvement | Branch-pruned large reduction | Trunk path method | Trunk path decision | Trunk path overlay | Trunk path selected | Trunk path components | Trunk path nodes | Trunk path edges | Trunk path endpoints | Trunk path junctions | Trunk path pixels | Trunk path columns | Trunk path coverage | Trunk path spurs | Trunk path matched | Trunk path P95 | Trunk path large | Trunk path P95 improvement | Trunk path large reduction | Centerline columns | Centerline coverage | Skeleton pixels | Skeleton columns | Skeleton coverage | Skeleton points | Fallback points | Wide columns | Branch columns | Warnings |")
        appendLine("| ---: | --- | --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |")
        audit.graphs.forEach { graph ->
            val centerline = graph.curveCenterline
            appendLine(
                "| ${graph.graphIndex} | ${centerline.available} | ${centerline.method.escapeTable()} | ${centerline.selectedForSignal} | ${centerline.selectionDecision.escapeTable()} | ${centerline.parityOverlayGenerated} | ${centerline.matchedColumnCount} | ${centerline.matchedColumnRatio.renderPercent()} | ${centerline.medianAbsDeltaPx.renderNumber()} | ${centerline.p95AbsDeltaPx.renderNumber()} | ${centerline.maxAbsDeltaPx.renderNumber()} | ${centerline.largeDeltaThresholdPx.renderNumber()} | ${centerline.largeDeltaColumnCount} | ${centerline.largeDeltaColumnRatio.renderPercent()} | ${centerline.largeDeltaNearBranchColumnCount} (${centerline.largeDeltaNearBranchColumnRatio.renderPercent()}) | ${centerline.largeDeltaSignalAboveCenterlineColumnCount} (${centerline.largeDeltaSignalAboveCenterlineColumnRatio.renderPercent()}) | ${centerline.largeDeltaSignalBelowCenterlineColumnCount} (${centerline.largeDeltaSignalBelowCenterlineColumnRatio.renderPercent()}) | ${centerline.branchPrunedMethod.escapeTable()} | ${centerline.branchPrunedDecision.escapeTable()} | ${centerline.branchPrunedOverlayGenerated} | ${centerline.branchPrunedRemovedColumnCount} | ${centerline.branchPrunedInterpolatedColumnCount} | ${centerline.branchPrunedMatchedColumnCount} (${centerline.branchPrunedMatchedColumnRatio.renderPercent()}) | ${centerline.branchPrunedP95AbsDeltaPx.renderNumber()} | ${centerline.branchPrunedLargeDeltaColumnCount} (${centerline.branchPrunedLargeDeltaColumnRatio.renderPercent()}) | ${centerline.branchPrunedP95DeltaImprovementPx.renderNumber()} | ${centerline.branchPrunedLargeDeltaReductionCount} | ${centerline.trunkPathMethod.escapeTable()} | ${centerline.trunkPathDecision.escapeTable()} | ${centerline.trunkPathOverlayGenerated} | ${centerline.trunkPathSelectedForSignal} | ${centerline.trunkPathComponentCount} | ${centerline.trunkPathNodeCount} | ${centerline.trunkPathEdgeCount} | ${centerline.trunkPathEndpointCount} | ${centerline.trunkPathJunctionCount} | ${centerline.trunkPathPixelCount} | ${centerline.trunkPathColumnCount} | ${centerline.trunkPathCoverage.renderPercent()} | ${centerline.trunkPathSpurPixelCount} | ${centerline.trunkPathMatchedColumnCount} (${centerline.trunkPathMatchedColumnRatio.renderPercent()}) | ${centerline.trunkPathP95AbsDeltaPx.renderNumber()} | ${centerline.trunkPathLargeDeltaColumnCount} (${centerline.trunkPathLargeDeltaColumnRatio.renderPercent()}) | ${centerline.trunkPathP95DeltaImprovementPx.renderNumber()} | ${centerline.trunkPathLargeDeltaReductionCount} | ${centerline.centerlineColumnCount} | ${centerline.centerlineCoverage.renderPercent()} | ${centerline.skeletonPixelCount} | ${centerline.skeletonColumnCount} | ${centerline.skeletonCoverage.renderPercent()} | ${centerline.skeletonPointCount} | ${centerline.fallbackPointCount} | ${centerline.wideClusterColumnCount} | ${centerline.branchColumnCount} | ${centerline.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Fragment Reconstruction Review")
        appendLine()
        appendLine("| Graph | Available | Method | Decision | Overlay | Selected | Components | Retained | Discarded | Guide columns | Guide max distance | Guide matched pixels | Guide rejected pixels | Guide rejected interpolation | Raw columns | Interpolated columns | Columns | Coverage | Max gap | Matched | Match ratio | Median delta | P95 delta | Max delta | Large deltas | Large delta ratio | P95 improvement | Large reduction | Residual gate | Residual columns | Peak-top candidates | Branch/edge ambiguity | Baseline gaps | Frame/text artifacts | Crop boundary | Signal-guide mismatch | Unclassified |")
        appendLine("| ---: | --- | --- | --- | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |")
        audit.graphs.forEach { graph ->
            val centerline = graph.curveCenterline
            appendLine(
                "| ${graph.graphIndex} | ${centerline.fragmentReconstructionAvailable} | ${centerline.fragmentReconstructionMethod.escapeTable()} | ${centerline.fragmentReconstructionDecision.escapeTable()} | ${centerline.fragmentReconstructionOverlayGenerated} | ${centerline.fragmentReconstructionSelectedForSignal} | ${centerline.fragmentReconstructionComponentCount} | ${centerline.fragmentReconstructionRetainedComponentCount} | ${centerline.fragmentReconstructionDiscardedComponentCount} | ${centerline.fragmentReconstructionGuideColumnCount} | ${centerline.fragmentReconstructionGuideMaxDistancePx} | ${centerline.fragmentReconstructionGuideMatchedPixelCount} | ${centerline.fragmentReconstructionGuideRejectedPixelCount} | ${centerline.fragmentReconstructionGuideRejectedInterpolatedColumnCount} | ${centerline.fragmentReconstructionRawColumnCount} | ${centerline.fragmentReconstructionInterpolatedColumnCount} | ${centerline.fragmentReconstructionColumnCount} | ${centerline.fragmentReconstructionCoverage.renderPercent()} | ${centerline.fragmentReconstructionMaxInterpolatedGapPx} | ${centerline.fragmentReconstructionMatchedColumnCount} | ${centerline.fragmentReconstructionMatchedColumnRatio.renderPercent()} | ${centerline.fragmentReconstructionMedianAbsDeltaPx.renderNumber()} | ${centerline.fragmentReconstructionP95AbsDeltaPx.renderNumber()} | ${centerline.fragmentReconstructionMaxAbsDeltaPx.renderNumber()} | ${centerline.fragmentReconstructionLargeDeltaColumnCount} | ${centerline.fragmentReconstructionLargeDeltaColumnRatio.renderPercent()} | ${centerline.fragmentReconstructionP95DeltaImprovementPx.renderNumber()} | ${centerline.fragmentReconstructionLargeDeltaReductionCount} | ${centerline.fragmentReconstructionResidualAcceptanceGate.escapeTable()} | ${centerline.fragmentReconstructionResidualColumnCount} | ${centerline.fragmentReconstructionResidualPeakTopCandidateColumnCount} | ${centerline.fragmentReconstructionResidualBranchEdgeAmbiguityColumnCount} | ${centerline.fragmentReconstructionResidualBaselineGapColumnCount} | ${centerline.fragmentReconstructionResidualFrameTextArtifactColumnCount} | ${centerline.fragmentReconstructionResidualCropBoundaryColumnCount} | ${centerline.fragmentReconstructionResidualSignalGuideMismatchColumnCount} | ${centerline.fragmentReconstructionResidualUnclassifiedColumnCount} |",
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
        appendLine("| Graph | Ready | Profile | Raw peaks | Production reportable | Test-only reportable | Runtime recovered | Test-only recovered | Base peaks | Tuned peaks | Significant | Dominant time | Dominant height | Dominant area | Baseline | Boundary | Integration | Clamp negative | Max width | Min S/N | Warnings |")
        appendLine("| ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- | --- | --- | ---: | ---: | --- |")
        audit.graphs.forEach { graph ->
            val peaks = graph.peakDetection
            appendLine(
                "| ${graph.graphIndex} | ${peaks.ready} | ${peaks.detectionProfile ?: "n/a"} | ${peaks.rawDetectedPeakCount} | ${peaks.productionReportablePeakCount} | ${peaks.testOnlyReportablePeakCount} | ${peaks.runtimeRecoveredPeakCount} | ${peaks.testOnlyRecoveredPeakCount} | ${peaks.basePeakCount ?: "n/a"} | ${peaks.tunedPeakCount ?: "n/a"} | ${peaks.significantPeakCount} | ${peaks.dominantPeakTime?.renderNumber() ?: "n/a"} | ${peaks.dominantPeakHeight?.renderNumber() ?: "n/a"} | ${peaks.dominantPeakAreaPercent?.renderNumber() ?: "n/a"} | ${peaks.baselineMethod ?: "n/a"} | ${peaks.boundaryMethod ?: "n/a"} | ${peaks.integrationMethod ?: "n/a"} | ${peaks.clampNegative ?: "n/a"} | ${peaks.maxPeakWidth ?: "n/a"} | ${peaks.minSnr?.renderNumber() ?: "n/a"} | ${peaks.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
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

        appendLine("## Peak Label Evidence")
        appendLine()
        appendLine("| Graph | Label | Parsed RT | Status | Source | Runtime evidence | Scope | Engine | Confidence | Crop | Warnings |")
        appendLine("| ---: | --- | ---: | --- | --- | --- | --- | --- | ---: | --- | --- |")
        audit.graphs.forEach { graph ->
            graph.peakDetection.peakLabelEvidence.forEach { label ->
                appendLine(
                    "| ${graph.graphIndex} | ${label.rawText.escapeTable()} | ${label.parsedRetentionTime?.renderNumber() ?: "n/a"} | ${label.status} | ${label.source} | ${label.isRuntimeEvidence} | ${label.evidenceScope} | ${label.ocrEngine} | ${label.confidence.renderNumber()} | ${(label.localCropPath ?: "n/a").escapeTable()} | ${label.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
                )
            }
        }
        if (audit.graphs.all { it.peakDetection.peakLabelEvidence.isEmpty() }) {
            appendLine("|  | none |  | none | none | false | none | none |  | none | none |")
        }
        appendLine()

        appendLine("## Recovered Peak Candidates")
        appendLine()
        appendLine("| Graph | Target RT | Local max RT | Delta | Apex intensity | Height | S/N | Prominence | Curvature | Width estimate | Window | Source | Runtime evidence | Status | Confidence | Flags | Rejection |")
        appendLine("| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- | --- | --- | ---: | --- | --- |")
        audit.graphs.forEach { graph ->
            graph.peakDetection.recoveredPeaks.forEach { peak ->
                val window = if (peak.integrationWindowStart != null && peak.integrationWindowEnd != null) {
                    "${peak.integrationWindowStart.renderNumber()}-${peak.integrationWindowEnd.renderNumber()}"
                } else {
                    "n/a"
                }
                appendLine(
                    "| ${graph.graphIndex} | ${peak.targetRt.renderNumber()} | ${peak.nearestLocalMaximumRt?.renderNumber() ?: "n/a"} | ${peak.rtDelta?.renderNumber() ?: "n/a"} | ${peak.nearestLocalMaximumIntensity?.renderNumber() ?: "n/a"} | ${peak.localHeight.renderNumber()} | ${peak.localSnr.renderNumber()} | ${peak.localProminence.renderNumber()} | ${peak.localCurvatureScore.renderNumber()} | ${peak.localWidthEstimate?.renderNumber() ?: "n/a"} | $window | ${peak.sourceEvidence} | ${peak.isRuntimeEvidence} | ${peak.status} | ${peak.confidence.renderNumber()} | ${peak.qualityFlags.joinToString("; ").ifBlank { "none" }.escapeTable()} | ${(peak.rejectionReason ?: "none").escapeTable()} |",
                )
            }
        }
        if (audit.graphs.all { it.peakDetection.recoveredPeaks.isEmpty() }) {
            appendLine("|  |  |  |  |  |  |  |  |  |  | none | none | false | none |  | none | none |")
        }
        appendLine()

        appendLine("## Dense Series Classification")
        appendLine()
        appendLine("| Graph | Available | Status | Raw candidates | Validated | Reportable | Significant | Series members | Artifacts | Review | Median spacing | Spacing CV | Area trend | Confidence | Warnings |")
        appendLine("| ---: | --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | ---: | --- |")
        audit.graphs.forEach { graph ->
            val dense = graph.peakDetection.denseSeries
            appendLine(
                "| ${graph.graphIndex} | ${dense.available} | ${dense.status} | ${dense.rawCandidatePeakCount} | ${dense.validatedPeakCount} | ${dense.reportablePeakCount} | ${dense.significantPeakCount} | ${dense.seriesMemberCount} | ${dense.rejectedArtifactPeakCount} | ${dense.reviewPeakCount} | ${dense.medianSpacing?.renderNumber() ?: "n/a"} | ${dense.spacingCv?.renderNumber() ?: "n/a"} | ${dense.areaTrend} | ${dense.confidence.renderNumber()} | ${dense.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        appendLine()

        appendLine("## Dense Series Peak Classes")
        appendLine()
        appendLine("| Graph | Peak | RT | Apex X | Apex Y | Apex Y error | Height | Area | S/N | Prominence | FWHM | Overlap | Baseline | Width | Artifact distance | Candidate line only | Validated apex | Trace evidence | Artifact score | Class | Warnings |")
        appendLine("| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- | --- | ---: | --- | --- | --- | ---: | --- | --- |")
        audit.graphs.forEach { graph ->
            graph.peakDetection.denseSeries.peaks.forEach { peak ->
                appendLine(
                    "| ${graph.graphIndex} | ${peak.peakNumber} | ${peak.rt.renderNumber()} | ${peak.apexPixelX?.renderNumber() ?: "n/a"} | ${peak.apexPixelY?.renderNumber() ?: "n/a"} | ${peak.apexYAlignmentErrorPx?.renderNumber() ?: "n/a"} | ${peak.height.renderNumber()} | ${peak.area.renderNumber()} | ${peak.snr.renderNumber()} | ${peak.prominence.renderNumber()} | ${peak.widthFwhm?.renderNumber() ?: "n/a"} | ${peak.overlapStatus} | ${peak.localBaselineQuality} | ${peak.widthPlausibility} | ${peak.nearestArtifactDistancePx?.renderNumber() ?: "n/a"} | ${peak.isCandidateLineOnly} | ${peak.isValidatedApex} | ${peak.traceEvidenceStatus} | ${peak.artifactSuspicionScore.renderNumber()} | ${peak.classification} | ${peak.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
                )
            }
        }
        if (audit.graphs.all { it.peakDetection.denseSeries.peaks.isEmpty() }) {
            appendLine("|  |  |  |  |  |  |  | none | none | none |  | none | none |")
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

    private fun StringBuilder.renderGraphPreparation(graph: OfflineGraphAudit) {
        appendLine("### Source And Graph Preparation")
        appendLine()
        appendLine("| Field | Value |")
        appendLine("| --- | --- |")
        reportRow("Graph panel bounds", graph.region.renderRegion())
        reportRow("Plot area bounds", graph.plotArea.region?.renderRegion() ?: "not detected")
        reportRow("Crop accepted", if (graph.cropQuality.acceptedForCalculation) "yes" else "no")
        reportRow("Boundary accepted", if (graph.cropBoundaryRisk.acceptedForCalculation) "yes" else "no")
        reportRow("Selected preprocessing", graph.selectedPreprocessingVariant ?: "not selected")
        reportRow("OCR status", graph.ocrStatus.name.humanizeCode())
        reportRow("Curve points", graph.curvePointCount.toString())
        reportRow("Curve coverage", graph.curveCoverage.renderPercent())
        reportRow("Centerline coverage", graph.curveCenterline.centerlineCoverage.renderPercent())
        reportRow("Skeleton support", graph.curveCenterline.skeletonCoverage.renderPercent())
        reportRow("Centerline signal decision", graph.curveCenterline.selectionDecision.humanizeCode())
        reportRow("Centerline P95 delta", "${graph.curveCenterline.p95AbsDeltaPx.renderNumber()} px")
        reportRow("Centerline large-delta columns", graph.curveCenterline.largeDeltaColumnCount.toString())
        reportRow("Large deltas near branches", graph.curveCenterline.largeDeltaNearBranchColumnCount.toString())
        reportRow("Large deltas with signal above centerline", graph.curveCenterline.largeDeltaSignalAboveCenterlineColumnCount.toString())
        reportRow("Large deltas with signal below centerline", graph.curveCenterline.largeDeltaSignalBelowCenterlineColumnCount.toString())
        reportRow("Branch-pruned centerline decision", graph.curveCenterline.branchPrunedDecision.humanizeCode())
        reportRow("Branch-pruned interpolated columns", graph.curveCenterline.branchPrunedInterpolatedColumnCount.toString())
        reportRow("Branch-pruned centerline P95 delta", "${graph.curveCenterline.branchPrunedP95AbsDeltaPx.renderNumber()} px")
        reportRow("Branch-pruned P95 improvement", "${graph.curveCenterline.branchPrunedP95DeltaImprovementPx.renderNumber()} px")
        reportRow("Trunk-path centerline decision", graph.curveCenterline.trunkPathDecision.humanizeCode())
        reportRow("Trunk-path components", graph.curveCenterline.trunkPathComponentCount.toString())
        reportRow("Trunk-path coverage", graph.curveCenterline.trunkPathCoverage.renderPercent())
        reportRow("Trunk-path P95 delta", "${graph.curveCenterline.trunkPathP95AbsDeltaPx.renderNumber()} px")
        reportRow("Fragment reconstruction decision", graph.curveCenterline.fragmentReconstructionDecision.humanizeCode())
        reportRow("Fragment reconstruction retained components", graph.curveCenterline.fragmentReconstructionRetainedComponentCount.toString())
        reportRow("Fragment reconstruction guide distance", "${graph.curveCenterline.fragmentReconstructionGuideMaxDistancePx} px")
        reportRow("Fragment reconstruction coverage", graph.curveCenterline.fragmentReconstructionCoverage.renderPercent())
        reportRow("Fragment reconstruction P95 delta", "${graph.curveCenterline.fragmentReconstructionP95AbsDeltaPx.renderNumber()} px")
        reportRow("Fragment reconstruction residual gate", graph.curveCenterline.fragmentReconstructionResidualAcceptanceGate.humanizeCode())
        reportRow("Fragment reconstruction residual columns", graph.curveCenterline.fragmentReconstructionResidualColumnCount.toString())
        reportRow("Fragment residual peak-top candidates", graph.curveCenterline.fragmentReconstructionResidualPeakTopCandidateColumnCount.toString())
        reportRow("Fragment residual branch/edge ambiguity", graph.curveCenterline.fragmentReconstructionResidualBranchEdgeAmbiguityColumnCount.toString())
        reportRow("Fragment residual baseline gaps", graph.curveCenterline.fragmentReconstructionResidualBaselineGapColumnCount.toString())
        reportRow("Fragment residual frame/text artifacts", graph.curveCenterline.fragmentReconstructionResidualFrameTextArtifactColumnCount.toString())
        reportRow("Fragment residual crop boundary", graph.curveCenterline.fragmentReconstructionResidualCropBoundaryColumnCount.toString())
        reportRow("Fragment residual signal-guide mismatch", graph.curveCenterline.fragmentReconstructionResidualSignalGuideMismatchColumnCount.toString())
        appendLine()
        renderHumanWarningList(
            (
                graph.refinement.warnings +
                    graph.cropQuality.warnings +
                    graph.cropBoundaryRisk.warnings +
                    graph.plotArea.warnings +
                    graph.warnings
                ).distinct(),
        )
        appendLine()
    }

    private fun StringBuilder.renderAxisCalibration(graph: OfflineGraphAudit) {
        val calibration = graph.axisCalibration
        appendLine("### Axis Calibration")
        appendLine()
        appendLine("| Axis | Unit | Pixel span | Value span | Tick candidates | Fit residual | Residual ratio |")
        appendLine("| --- | --- | ---: | ---: | ---: | ---: | ---: |")
        appendLine(
            "| X | ${(calibration.xUnit ?: "not detected").escapeTable()} | ${calibration.xPixelSpan.renderNumber()} | ${calibration.xValueSpan.renderNumber()} | ${calibration.xCandidateCount} | ${calibration.xFitResidual.renderNumber()} | ${calibration.xFitResidualRatio.renderPercent()} |",
        )
        appendLine(
            "| Y | ${(calibration.yUnit ?: "not detected").escapeTable()} | ${calibration.yPixelSpan.renderNumber()} | ${calibration.yValueSpan.renderNumber()} | ${calibration.yCandidateCount} | ${calibration.yFitResidual.renderNumber()} | ${calibration.yFitResidualRatio.renderPercent()} |",
        )
        appendLine()
        appendLine("- Calibration source: ${calibration.source.name.humanizeCode()}")
        appendLine("- Calibration ready: ${if (calibration.ready) "yes" else "no"}")
        appendLine("- Residual fit ready: ${if (calibration.residualFitReady) "yes" else "no"}")
        appendLine("- Geometry confidence: ${graph.axisConfidence.renderPercent()}")
        appendLine()
        renderHumanWarningList(calibration.warnings)
        appendLine()
    }

    private fun StringBuilder.renderVisualEvidence(graph: OfflineGraphAudit) {
        appendLine("### Visual Evidence")
        appendLine()
        appendLine("| Evidence | Artifact | Report placement | Status |")
        appendLine("| --- | --- | --- | --- |")
        visualEvidenceRow(
            label = "Graph candidates and selected panel",
            path = "graph_candidates.png",
            placement = "Preparation review and technical appendix",
            status = "generated",
        )
        visualEvidenceRow(
            label = "Selected preprocessing crop",
            path = "selected_preprocessing_graph_${graph.graphIndex}.png",
            placement = "Preparation section",
            status = if (graph.selectedPreprocessingImagePath != null) "generated" else "not available",
        )
        visualEvidenceRow(
            label = "Manual calibration focus",
            path = "manual_calibration_graph_${graph.graphIndex}.png",
            placement = "Axis calibration section",
            status = if (graph.plotArea.region != null) "generated" else "not available",
        )
        visualEvidenceRow(
            label = "Curve extraction overlay",
            path = "graph_${graph.graphIndex}/curve_overlay.png",
            placement = "Rendered graph section",
            status = if (graph.curveUsable || graph.curvePointCount > 0) "generated" else "not available",
        )
        visualEvidenceRow(
            label = "Centerline parity overlay",
            path = "graph_${graph.graphIndex}/centerline_parity_overlay.png",
            placement = "Technical appendix and trace acceptance review",
            status = if (graph.curveCenterline.parityOverlayGenerated) "generated" else "not available",
        )
        visualEvidenceRow(
            label = "Branch-pruned centerline overlay",
            path = "graph_${graph.graphIndex}/centerline_branch_pruned_overlay.png",
            placement = "Technical appendix and trace acceptance review",
            status = if (graph.curveCenterline.branchPrunedOverlayGenerated) "generated" else "not available",
        )
        visualEvidenceRow(
            label = "Skeleton graph trunk-path overlay",
            path = "graph_${graph.graphIndex}/centerline_trunk_path_overlay.png",
            placement = "Technical appendix and trace acceptance review",
            status = if (graph.curveCenterline.trunkPathOverlayGenerated) "generated" else "not available",
        )
        visualEvidenceRow(
            label = "Fragment reconstruction overlay",
            path = "graph_${graph.graphIndex}/centerline_fragment_reconstruction_overlay.png",
            placement = "Technical appendix and trace acceptance review",
            status = if (graph.curveCenterline.fragmentReconstructionOverlayGenerated) "generated" else "not available",
        )
        visualEvidenceRow(
            label = "Peak integration overlay",
            path = "peak_overlay_graph_${graph.graphIndex}.png",
            placement = "Peak table cross-check",
            status = if (graph.peakMetrics.ready && graph.peakDetection.peaks.isNotEmpty()) {
                "generated"
            } else {
                "not available until peak metrics pass"
            },
        )
        visualEvidenceRow(
            label = "Trace artifact mask",
            path = "graph_${graph.graphIndex}/trace_artifacts.png",
            placement = "Technical appendix",
            status = if (graph.traceArtifacts.available) "generated" else "not available",
        )
        visualEvidenceRow(
            label = "Trace cleanup hypothesis",
            path = "graph_${graph.graphIndex}/trace_artifact_suppressed_mask.png",
            placement = "Technical appendix",
            status = if (graph.traceArtifacts.cleanupHypothesisMaskPath != null) "generated" else "not available",
        )
        appendLine()
    }

    private fun StringBuilder.renderPeakTable(graph: OfflineGraphAudit) {
        appendLine("### Peak Table")
        appendLine()
        if (graph.peakDetection.peaks.isEmpty()) {
            appendLine("No calculated peak rows are available.")
            appendLine()
            return
        }

        appendLine("| # | RT apex | Left | Right | Height | Area | Area % | FWHM | W base | S/N | Tailing | Asymmetry | Compound | Formula | C# | Kovats | Confidence | Flags |")
        appendLine("| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- | --- | --- | --- | --- | --- |")
        graph.peakDetection.peaks.sortedBy { it.rtApex }.forEach { peak ->
            appendLine(
                listOf(
                    peak.peakNumber.toString(),
                    peak.rtApex.renderNumber(),
                    peak.leftBoundaryTime.renderNumber(),
                    peak.rightBoundaryTime.renderNumber(),
                    peak.height.renderNumber(),
                    peak.area.renderNumber(),
                    peak.areaPercent.renderNumber(),
                    peak.widthHalfHeight?.renderNumber() ?: "not calculated",
                    peak.widthBase.renderNumber(),
                    peak.snr.renderNumber(),
                    peak.tailingFactor.renderNumber(),
                    peak.asymmetryFactor.renderNumber(),
                    peak.assignment.probableCompoundName ?: peak.assignment.probableCompoundStatus.renderReportValueStatus(),
                    peak.assignment.formula ?: peak.assignment.formulaStatus.renderReportValueStatus(),
                    peak.assignment.carbonNumber ?: peak.assignment.carbonNumberStatus.renderReportValueStatus(),
                    peak.assignment.kovatsIndex?.renderNumber() ?: peak.assignment.kovatsIndexStatus.renderReportValueStatus(),
                    peak.confidence.humanizeCode(),
                    peak.qualityFlags.joinToString("; ") { it.humanizeCode() }.ifBlank { "none" },
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |") { it.escapeTable() },
            )
        }
        appendLine()
        renderHumanWarningList(
            (
                graph.peakDetection.warnings +
                    graph.peakMetrics.warnings +
                    graph.peakSanity.warnings
                ).distinct(),
        )
        appendLine()
    }

    private fun StringBuilder.renderChromatographicQuality(graph: OfflineGraphAudit) {
        val peaks = graph.peakDetection
        val metrics = graph.peakMetrics
        appendLine("### Chromatographic Quality")
        appendLine()
        appendLine("| Metric | Value |")
        appendLine("| --- | --- |")
        reportRow("Peak count", peaks.peakCount.toString())
        reportRow("Significant peaks", peaks.significantPeakCount.toString())
        reportRow("Dominant peak RT", peaks.dominantPeakTime?.renderNumber() ?: "not calculated")
        reportRow("Dominant peak height", peaks.dominantPeakHeight?.renderNumber() ?: "not calculated")
        reportRow("Dominant peak area share", peaks.dominantPeakAreaPercent?.renderNumber() ?: "not calculated")
        reportRow("Total integrated area", metrics.totalAbsArea.renderNumber())
        reportRow("Area percent sum", metrics.areaPercentSum.renderNumber())
        reportRow("Noise estimate", peaks.noiseLevel?.renderNumber() ?: "not calculated")
        reportRow("Noise method", peaks.noiseMethod ?: "not calculated")
        reportRow("Baseline method", peaks.baselineMethod ?: "not calculated")
        reportRow("Boundary method", peaks.boundaryMethod ?: "not calculated")
        reportRow("Integration method", peaks.integrationMethod ?: "not calculated")
        reportRow("Clamp negative", peaks.clampNegative?.toString() ?: "not calculated")
        reportRow("Max peak width", peaks.maxPeakWidth?.toString() ?: "not calculated")
        reportRow("Minimum S/N", peaks.minSnr?.renderNumber() ?: "not calculated")
        appendLine()
    }

    private fun StringBuilder.renderKovatsAndInterpretation(graph: OfflineGraphAudit) {
        appendLine("### Kovats And Interpretation")
        appendLine()
        appendLine("- Kovats index values are not calculated yet because no local reference series is attached to this audit.")
        appendLine("- Compound names, formulas, carbon numbers, and interpretation remain explicit not-calculated fields unless supported by local knowledge, library evidence, or user data.")
        if (graph.peakDetection.sparseTraceQualityReview.requiresReportConfidenceText) {
            appendLine("- Sparse ion trace evidence limits interpretation confidence and requires visual/report review.")
        }
        if (graph.peakDetection.controlledTuningApplied) {
            appendLine("- Guarded completeness recovered additional visible peaks; review lower-confidence rows before chemical interpretation.")
        }
        appendLine()
    }

    private fun StringBuilder.renderSectionReadiness(
        audit: OfflineAnalysisAudit,
        graph: OfflineGraphAudit,
    ) {
        val sections = audit.reportContract.sections.filter { it.graphIndex == graph.graphIndex }
        appendLine("### Report Section Readiness")
        appendLine()
        appendLine("| Section | Status | Notes |")
        appendLine("| --- | --- | --- |")
        sections.forEach { section ->
            val notes = (section.missingFields + section.warnings)
                .distinct()
                .joinToString("; ") { it.humanizeCode() }
                .ifBlank { "none" }
            appendLine(
                "| ${section.section.humanizeCode().escapeTable()} | ${section.status.name.humanizeCode()} | ${notes.escapeTable()} |",
            )
        }
        if (sections.isEmpty()) {
            appendLine("| none | not calculated | report validation is not available |")
        }
        appendLine()
    }

    private fun StringBuilder.renderHumanWarningList(warnings: List<String>) {
        if (warnings.isEmpty()) {
            appendLine("- No report warnings.")
            return
        }
        warnings.forEach { warning -> appendLine("- ${warning.humanizeCode().escapeTable()}") }
    }

    private fun StringBuilder.renderRawWarningCodes(audit: OfflineAnalysisAudit) {
        appendLine("### Raw Warning Codes")
        appendLine()
        appendLine("| Scope | Graph | Code |")
        appendLine("| --- | ---: | --- |")
        val rows = buildList {
            audit.warnings.forEach { add(Triple("pipeline", null, it)) }
            audit.reportContract.warnings.forEach { add(Triple("report_contract", null, it)) }
            audit.graphs.forEach { graph ->
                graph.warnings.forEach { add(Triple("graph", graph.graphIndex, it)) }
                graph.axisCalibration.warnings.forEach { add(Triple("axis_calibration", graph.graphIndex, it)) }
                graph.peakDetection.warnings.forEach { add(Triple("peak_detection", graph.graphIndex, it)) }
                graph.peakMetrics.warnings.forEach { add(Triple("peak_metrics", graph.graphIndex, it)) }
                graph.peakSanity.warnings.forEach { add(Triple("peak_sanity", graph.graphIndex, it)) }
            }
        }.distinct()
        if (rows.isEmpty()) {
            appendLine("| none |  | none |")
        } else {
            rows.forEach { (scope, graph, code) ->
                appendLine("| ${scope.escapeTable()} | ${graph ?: ""} | `${code.escapeTable()}` |")
            }
        }
        appendLine()
    }

    private fun StringBuilder.renderStageTimeline(audit: OfflineAnalysisAudit) {
        appendLine("### Stage Timeline")
        appendLine()
        appendLine("| Stage | Graph | Status | Duration | Message |")
        appendLine("| --- | ---: | --- | ---: | --- |")
        audit.stages.forEach { stage ->
            appendLine(
                "| `${stage.stage.escapeTable()}` | ${stage.graphIndex ?: ""} | ${stage.status} | ${stage.durationMillis} ms | ${stage.message.escapeTable()} |",
            )
        }
        appendLine()
    }

    private fun StringBuilder.renderRawReportSections(audit: OfflineAnalysisAudit) {
        appendLine("### Raw Report Contract Sections")
        appendLine()
        appendLine("| Graph | Section | Status | Missing fields | Warnings |")
        appendLine("| ---: | --- | --- | --- | --- |")
        audit.reportContract.sections.forEach { section ->
            appendLine(
                "| ${section.graphIndex ?: ""} | `${section.section}` | ${section.status} | ${section.missingFields.joinToString("; ").ifBlank { "none" }.escapeTable()} | ${section.warnings.joinToString("; ").ifBlank { "none" }.escapeTable()} |",
            )
        }
        if (audit.reportContract.sections.isEmpty()) {
            appendLine("|  | none | BLOCKED | report_validation_not_available | none |")
        }
    }

    private fun StringBuilder.reportRow(label: String, value: String) {
        appendLine("| ${label.escapeTable()} | ${value.escapeTable()} |")
    }

    private fun StringBuilder.visualEvidenceRow(
        label: String,
        path: String,
        placement: String,
        status: String,
    ) {
        appendLine("| ${label.escapeTable()} | `${path.escapeTable()}` | ${placement.escapeTable()} | ${status.escapeTable()} |")
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

private fun String.renderReportValueStatus(): String =
    when (this) {
        "CALCULATED" -> "calculated"
        "DETECTED" -> "detected"
        "INFERRED" -> "inferred"
        "NOT_DETECTED" -> "not detected"
        "INSUFFICIENT_CONFIDENCE" -> "insufficient confidence"
        "FAILED" -> "failed"
        "NOT_CALCULATED" -> "not calculated"
        else -> humanizeCode()
    }

private fun String.humanizeCode(): String {
    when (this) {
        "peak_detection.sparse_trace_report_confidence_required",
        "report.peak_table.sparse_trace_confidence_text_required",
        "report.graph_overlay.sparse_trace_visual_review_required",
        "report.quality.sparse_trace_quality_text_required" ->
            return "Sparse trace report confidence required"

        "peak_detection.sparse_trace_localized_review_required",
        "curve_extract.sparse_trace_localized_review_required" ->
            return "Localized sparse trace requires visual review"

        "peak_detection.sparse_trace_low_area_share_peaks" ->
            return "Sparse trace contains low-area peaks that need review"

        "peak_detection.sparse_trace_overlap_review_required" ->
            return "Sparse trace contains overlapping peaks that need review"

        "report.peak_table.compound_assignments_not_calculated",
        "report.interpretation.compound_assignments_missing" ->
            return "Compound assignments are not calculated"

        "report.peak_table.kovats_values_not_calculated",
        "report.kovats.reference_series_missing",
        "report.kovats.must_render_not_calculated_state" ->
            return "Kovats values are not calculated because the reference series is missing"

        "report.interpretation.local_knowledge_pack_required" ->
            return "Local domain knowledge pack is required before chemical interpretation"

        "report.overview.pipeline_warnings_present" ->
            return "Pipeline warnings are available in the technical appendix"
    }
    val words = replace('.', ' ')
        .replace('_', ' ')
        .replace('-', ' ')
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
    if (words.isEmpty()) return this
    return words.joinToString(" ") { word ->
        when {
            word.equals("rt", ignoreCase = true) -> "RT"
            word.equals("snr", ignoreCase = true) -> "S/N"
            word.equals("ocr", ignoreCase = true) -> "OCR"
            word.equals("xic", ignoreCase = true) -> "XIC"
            word.equals("tic", ignoreCase = true) -> "TIC"
            word.equals("fwhm", ignoreCase = true) -> "FWHM"
            word.length <= 2 -> word.uppercase()
            else -> word.lowercase()
        }
    }.replaceFirstChar { char -> char.uppercase() }
}

private fun String.escapeTable(): String =
    replace("|", "\\|").replace("\n", " ")

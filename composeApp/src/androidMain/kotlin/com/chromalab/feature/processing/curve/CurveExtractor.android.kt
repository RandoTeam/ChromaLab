package com.chromalab.feature.processing.curve

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Android curve extractor.
 *
 * The extractor keeps the public point-per-column signal contract and audits a
 * skeleton centerline candidate separately until parity review accepts a switch.
 */
actual class CurveExtractor actual constructor() {

    actual fun extract(
        maskPath: String,
        maskWidth: Int,
        maskHeight: Int,
        outputDir: String,
    ): CurveExtractionResult {
        val bitmap = BitmapFactory.decodeFile(maskPath)
            ?: return emptyResult(maskWidth)

        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        bitmap.recycle()

        val mask = BooleanArray(w * h) { i ->
            val p = pixels[i]
            ((p shr 16) and 0xFF) > 128
        }
        val skeletonMask = skeletonize(mask, w, h)

        val rawPoints = mutableListOf<CurvePoint>()
        val gapColumns = mutableListOf<Int>()
        var skeletonPointCount = 0
        var fallbackPointCount = 0
        var wideClusterColumnCount = 0
        var branchColumnCount = 0
        val centerlineCandidateByX = mutableMapOf<Int, Float>()
        val branchCandidateColumns = mutableSetOf<Int>()
        val lowerContourSamples = mutableListOf<Int>()

        for (x in 0 until w) {
            val skeletonCandidates = mutableListOf<Int>()
            val candidates = mutableListOf<Int>()
            for (y in 0 until h) {
                if (skeletonMask[y * w + x]) skeletonCandidates.add(y)
                if (mask[y * w + x]) candidates.add(y)
            }

            val skeletonCluster = bestSignalCluster(skeletonCandidates, h)
            if (skeletonCluster != null) {
                val branchCandidate = clusters(skeletonCandidates).size > 1 || skeletonCluster.size > 2
                if (branchCandidate) {
                    branchColumnCount++
                    branchCandidateColumns += x
                }
                centerlineCandidateByX[x] = skeletonCluster.average().toFloat()
                skeletonPointCount++
            }

            if (candidates.isEmpty()) {
                gapColumns.add(x)
                continue
            }

            val cluster = bestSignalCluster(candidates, h)
            if (cluster == null) {
                gapColumns.add(x)
                continue
            }
            lowerContourSamples += cluster.last()
            if (cluster.size > 8) wideClusterColumnCount++
            rawPoints.add(CurvePoint(x, cluster.first().toFloat(), cluster.confidence()))
            fallbackPointCount++
        }

        val outlierCount = 0
        val shortInterpolatedPoints = interpolateShortGaps(rawPoints, gapColumns, maxGap = 6)
        val knownPointsByX = (rawPoints + shortInterpolatedPoints).associateBy { it.pixelX }
        val baselineFilledPoints = fillBaselineGaps(
            knownPointsByX = knownPointsByX,
            fillRange = rawPoints.xFillRange(),
            baselineY = estimateBaselineY(lowerContourSamples, h),
        )
        val allPoints = (knownPointsByX.values + baselineFilledPoints).sortedBy { it.pixelX }
        val interpolatedCount = shortInterpolatedPoints.size + baselineFilledPoints.size
        val branchPrunedCenterline = selectBranchPrunedCenterline(
            centerlineCandidateByX = centerlineCandidateByX,
            branchCandidateColumns = branchCandidateColumns,
            signalPoints = rawPoints,
        )
        val trunkPathCenterline = SkeletonGraphTrunkPathExtractor.extract(
            skeletonMask = skeletonMask,
            width = w,
            height = h,
        )
        val fragmentReconstruction = FragmentedTraceReconstructionExtractor.extract(
            skeletonMask = skeletonMask,
            width = w,
            height = h,
            signalGuideByX = rawPoints.associate { it.pixelX to it.pixelY },
        )
        val fragmentReconstructionResidualReview = ReconstructedTraceResidualClassifier.classify(
            signalPoints = rawPoints,
            reconstructedByX = fragmentReconstruction.pointsByX,
            branchCandidateColumns = branchCandidateColumns,
            width = w,
            height = h,
            largeDeltaThresholdPx = CENTERLINE_LARGE_DELTA_THRESHOLD_PX,
            guideMaxDistancePx = fragmentReconstruction.guideMaxDistancePx,
        )
        val initialCenterlineAudit = buildCenterlineAudit(
            skeletonMask = skeletonMask,
            totalColumns = w,
            centerlineColumns = rawPoints.size,
            skeletonPointCount = skeletonPointCount,
            fallbackPointCount = fallbackPointCount,
            wideClusterColumnCount = wideClusterColumnCount,
            branchColumnCount = branchColumnCount,
            signalPoints = rawPoints,
            centerlineCandidateByX = centerlineCandidateByX,
            branchPrunedCenterline = branchPrunedCenterline,
            trunkPathCenterline = trunkPathCenterline,
            fragmentReconstruction = fragmentReconstruction,
            fragmentReconstructionResidualReview = fragmentReconstructionResidualReview,
            branchCandidateColumns = branchCandidateColumns,
        )

        val overlayPath = File(outputDir).also { it.mkdirs() }
            .let { File(it, "curve_overlay.png").absolutePath }
        saveCurveOverlay(mask, skeletonMask, allPoints, w, h, overlayPath)
        val parityOverlayPath = File(outputDir)
            .let { File(it, "centerline_parity_overlay.png").absolutePath }
        saveCenterlineParityOverlay(
            mask = mask,
            skeletonMask = skeletonMask,
            signalPoints = rawPoints,
            centerlineCandidateByX = centerlineCandidateByX,
            branchCandidateColumns = branchCandidateColumns,
            w = w,
            h = h,
            path = parityOverlayPath,
        )
        val prunedOverlayPath = File(outputDir)
            .let { File(it, "centerline_branch_pruned_overlay.png").absolutePath }
        saveBranchPrunedCenterlineOverlay(
            mask = mask,
            skeletonMask = skeletonMask,
            signalPoints = rawPoints,
            centerlineCandidateByX = centerlineCandidateByX,
            branchPrunedCenterlineByX = branchPrunedCenterline.pointsByX,
            branchCandidateColumns = branchCandidateColumns,
            w = w,
            h = h,
            path = prunedOverlayPath,
        )
        val trunkPathOverlayPath = File(outputDir)
            .let { File(it, "centerline_trunk_path_overlay.png").absolutePath }
        saveTrunkPathCenterlineOverlay(
            mask = mask,
            skeletonMask = skeletonMask,
            signalPoints = rawPoints,
            branchPrunedCenterlineByX = branchPrunedCenterline.pointsByX,
            trunkPathCenterlineByX = trunkPathCenterline.pointsByX,
            w = w,
            h = h,
            path = trunkPathOverlayPath,
        )
        val fragmentReconstructionOverlayPath = File(outputDir)
            .let { File(it, "centerline_fragment_reconstruction_overlay.png").absolutePath }
        saveFragmentReconstructionOverlay(
            mask = mask,
            skeletonMask = skeletonMask,
            signalPoints = rawPoints,
            branchPrunedCenterlineByX = branchPrunedCenterline.pointsByX,
            trunkPathCenterlineByX = trunkPathCenterline.pointsByX,
            fragmentReconstructionByX = fragmentReconstruction.pointsByX,
            residualReview = fragmentReconstructionResidualReview,
            w = w,
            h = h,
            path = fragmentReconstructionOverlayPath,
        )
        val centerlineAudit = initialCenterlineAudit.copy(
            parityOverlayGenerated = true,
            branchPrunedOverlayGenerated = true,
            trunkPathOverlayGenerated = true,
            fragmentReconstructionOverlayGenerated = true,
        )

        val result = CurveExtractionResult(
            points = allPoints.sortedBy { it.pixelX },
            maskImagePath = overlayPath,
            totalColumns = w,
            extractedColumns = rawPoints.size,
            interpolatedColumns = interpolatedCount,
            outlierCount = outlierCount,
            centerlineAudit = centerlineAudit,
            warnings = emptyList(),
            timestamp = System.currentTimeMillis(),
        )
        val auditWarnings = buildList {
            if (result.coverage < 0.35f && !result.isSparseTraceUsable) add("curve_extract.low_column_coverage")
            if (result.isSparseTraceUsable && result.coverage <= 0.3f) {
                add("curve_extract.sparse_trace_low_column_coverage_accepted")
            }
            if (result.isLocalizedSparseTrace) add("curve_extract.sparse_trace_localized_review_required")
            if (outlierCount > result.points.size * 0.1f) add("curve_extract.many_outliers")
            if (baselineFilledPoints.isNotEmpty()) add("curve_extract.baseline_filled_sparse_columns")
        }

        return result.copy(warnings = auditWarnings)
    }

    private fun bestSignalCluster(sorted: List<Int>, height: Int): List<Int>? {
        val supportStart = height * 0.34f
        val supportedClusters = clusters(sorted).filter { it.last() >= supportStart }
        if (supportedClusters.isEmpty()) return null
        return supportedClusters.maxWithOrNull(
            compareBy<List<Int>> { cluster ->
                val span = cluster.last() - cluster.first() + 1
                val bottomSupport = (cluster.last().toFloat() / height.coerceAtLeast(1).toFloat() * 24f).toInt()
                span + bottomSupport
            }.thenBy { it.last() },
        )
    }

    private fun clusters(sorted: List<Int>): List<List<Int>> {
        if (sorted.isEmpty()) return emptyList()
        val values = sorted.sorted()
        val result = mutableListOf<List<Int>>()
        var start = 0
        for (index in 1 until values.size) {
            if (values[index] - values[index - 1] > 2) {
                result += values.subList(start, index)
                start = index
            }
        }
        result += values.subList(start, values.size)
        return result
    }

    private fun List<Int>.confidence(): Float =
        when {
            size <= 4 -> CurvePoint.HIGH_CONFIDENCE
            size <= 18 -> 0.8f
            else -> CurvePoint.LOW_CONFIDENCE
        }

    private fun estimateBaselineY(samples: List<Int>, height: Int): Float {
        if (height <= 0) return 0f
        if (samples.isEmpty()) return (height - 1).coerceAtLeast(0).toFloat()
        val sorted = samples.sorted()
        val index = ((sorted.size - 1) * 0.85f).roundToInt().coerceIn(sorted.indices)
        return sorted[index].coerceIn(0, height - 1).toFloat()
    }

    private fun fillBaselineGaps(
        knownPointsByX: Map<Int, CurvePoint>,
        fillRange: IntRange,
        baselineY: Float,
    ): List<CurvePoint> {
        if (fillRange.isEmpty()) return emptyList()
        return fillRange.mapNotNull { x ->
            if (x in knownPointsByX) {
                null
            } else {
                CurvePoint(
                    pixelX = x,
                    pixelY = baselineY,
                    confidence = CurvePoint.INTERPOLATED,
                )
            }
        }
    }

    private fun List<CurvePoint>.xFillRange(): IntRange {
        if (isEmpty()) return IntRange.EMPTY
        return minOf { it.pixelX }..maxOf { it.pixelX }
    }

    private fun interpolateShortGaps(
        known: List<CurvePoint>,
        gaps: List<Int>,
        maxGap: Int,
    ): List<CurvePoint> {
        if (known.size < 2 || gaps.isEmpty()) return emptyList()
        val byX = known.associateBy { it.pixelX }
        val result = mutableListOf<CurvePoint>()
        var index = 0
        while (index < gaps.size) {
            val startX = gaps[index]
            var endX = startX
            while (index + 1 < gaps.size && gaps[index + 1] == endX + 1) {
                index++
                endX = gaps[index]
            }
            val gapWidth = endX - startX + 1
            val left = byX[startX - 1]
            val right = byX[endX + 1]
            if (gapWidth <= maxGap && left != null && right != null) {
                for (x in startX..endX) {
                    val t = (x - left.pixelX).toFloat() / (right.pixelX - left.pixelX).toFloat()
                    result += CurvePoint(
                        pixelX = x,
                        pixelY = left.pixelY + t * (right.pixelY - left.pixelY),
                        confidence = CurvePoint.INTERPOLATED,
                    )
                }
            }
            index++
        }
        return result
    }

    private fun buildCenterlineAudit(
        skeletonMask: BooleanArray,
        totalColumns: Int,
        centerlineColumns: Int,
        skeletonPointCount: Int,
        fallbackPointCount: Int,
        wideClusterColumnCount: Int,
        branchColumnCount: Int,
        signalPoints: List<CurvePoint>,
        centerlineCandidateByX: Map<Int, Float>,
        branchPrunedCenterline: BranchPrunedCenterline,
        trunkPathCenterline: SkeletonGraphTrunkPath,
        fragmentReconstruction: FragmentedTraceReconstruction,
        fragmentReconstructionResidualReview: ReconstructedTraceResidualReview,
        branchCandidateColumns: Set<Int>,
    ): CurveCenterlineAudit {
        val skeletonPixelCount = skeletonMask.count { it }
        val skeletonColumns = if (totalColumns > 0) {
            (0 until totalColumns).count { x ->
                var found = false
                var y = x
                while (y < skeletonMask.size && !found) {
                    found = skeletonMask[y]
                    y += totalColumns
                }
                found
            }
        } else {
            0
        }
        val centerlineCoverage = if (totalColumns > 0) centerlineColumns.toFloat() / totalColumns else 0f
        val skeletonCoverage = if (totalColumns > 0) skeletonColumns.toFloat() / totalColumns else 0f
        val parity = signalPoints.centerlineParity(centerlineCandidateByX, branchCandidateColumns)
        val branchPrunedParity = signalPoints.centerlineParity(branchPrunedCenterline.pointsByX, branchCandidateColumns)
        val trunkPathParity = signalPoints.centerlineParity(trunkPathCenterline.pointsByX, branchCandidateColumns)
        val fragmentReconstructionParity = signalPoints.centerlineParity(
            fragmentReconstruction.pointsByX,
            branchCandidateColumns,
        )
        val branchPrunedDecision = branchPrunedDecision(
            original = parity,
            pruned = branchPrunedParity,
        )
        val trunkPathDecision = trunkPathDecision(
            original = parity,
            branchPruned = branchPrunedParity,
            trunkPath = trunkPathParity,
        )
        val fragmentReconstructionDecision = fragmentReconstructionDecision(
            original = parity,
            branchPruned = branchPrunedParity,
            trunkPath = trunkPathParity,
            fragmentReconstruction = fragmentReconstructionParity,
        )
        val branchRatio = if (centerlineColumns > 0) {
            branchColumnCount.toFloat() / centerlineColumns.toFloat()
        } else {
            0f
        }
        val selectionDecision = when {
            !parity.compared -> "preserve_legacy_no_centerline_overlap"
            parity.matchedColumnRatio < 0.45f -> "preserve_legacy_low_centerline_overlap"
            parity.p95AbsDeltaPx > CENTERLINE_LARGE_DELTA_THRESHOLD_PX -> "preserve_legacy_large_centerline_delta"
            branchRatio > 0.20f && branchColumnCount > 8 -> "preserve_legacy_branching_review_required"
            else -> "centerline_candidate_ready_for_visual_review"
        }
        val warnings = buildList {
            if (skeletonPixelCount == 0) add("curve_centerline.no_skeleton_pixels")
            if (skeletonPointCount == 0 && fallbackPointCount > 0) add("curve_centerline.cluster_center_fallback_only")
            if (skeletonPointCount > 0 && fallbackPointCount > skeletonPointCount) {
                add("curve_centerline.low_skeleton_support")
            }
            if (centerlineCoverage < 0.05f) add("curve_centerline.low_centerline_coverage")
            if (!parity.compared) add("curve_centerline.parity_not_available")
            if (parity.compared && parity.p95AbsDeltaPx > CENTERLINE_LARGE_DELTA_THRESHOLD_PX) {
                add("curve_centerline.large_signal_delta")
            }
            if (branchColumnCount > centerlineColumns * 0.20f && branchColumnCount > 8) {
                add("curve_centerline.many_branch_columns")
            }
            if (trunkPathCenterline.available && trunkPathParity.matchedColumnRatio < 0.45f) {
                add("curve_centerline.trunk_path_low_overlap")
            }
            if (fragmentReconstruction.available && fragmentReconstructionParity.matchedColumnRatio < 0.45f) {
                add("curve_centerline.fragment_reconstruction_low_overlap")
            }
        }
        return CurveCenterlineAudit(
            available = centerlineColumns > 0,
            method = "zhang_suen_centerline_candidate_signal_preserved",
            selectionDecision = selectionDecision,
            selectedForSignal = false,
            parityCompared = parity.compared,
            matchedColumnCount = parity.matchedColumnCount,
            matchedColumnRatio = parity.matchedColumnRatio,
            medianAbsDeltaPx = parity.medianAbsDeltaPx,
            p95AbsDeltaPx = parity.p95AbsDeltaPx,
            maxAbsDeltaPx = parity.maxAbsDeltaPx,
            largeDeltaThresholdPx = CENTERLINE_LARGE_DELTA_THRESHOLD_PX,
            largeDeltaColumnCount = parity.largeDeltaColumnCount,
            largeDeltaColumnRatio = parity.largeDeltaColumnRatio,
            largeDeltaNearBranchColumnCount = parity.largeDeltaNearBranchColumnCount,
            largeDeltaNearBranchColumnRatio = parity.largeDeltaNearBranchColumnRatio,
            largeDeltaSignalAboveCenterlineColumnCount = parity.largeDeltaSignalAboveCenterlineColumnCount,
            largeDeltaSignalAboveCenterlineColumnRatio = parity.largeDeltaSignalAboveCenterlineColumnRatio,
            largeDeltaSignalBelowCenterlineColumnCount = parity.largeDeltaSignalBelowCenterlineColumnCount,
            largeDeltaSignalBelowCenterlineColumnRatio = parity.largeDeltaSignalBelowCenterlineColumnRatio,
            branchPrunedAvailable = branchPrunedParity.compared,
            branchPrunedMethod = branchPrunedCenterline.method,
            branchPrunedDecision = branchPrunedDecision,
            branchPrunedSelectedForSignal = false,
            branchPrunedRemovedColumnCount = branchPrunedCenterline.removedColumnCount,
            branchPrunedInterpolatedColumnCount = branchPrunedCenterline.interpolatedColumnCount,
            branchPrunedMatchedColumnCount = branchPrunedParity.matchedColumnCount,
            branchPrunedMatchedColumnRatio = branchPrunedParity.matchedColumnRatio,
            branchPrunedMedianAbsDeltaPx = branchPrunedParity.medianAbsDeltaPx,
            branchPrunedP95AbsDeltaPx = branchPrunedParity.p95AbsDeltaPx,
            branchPrunedMaxAbsDeltaPx = branchPrunedParity.maxAbsDeltaPx,
            branchPrunedLargeDeltaColumnCount = branchPrunedParity.largeDeltaColumnCount,
            branchPrunedLargeDeltaColumnRatio = branchPrunedParity.largeDeltaColumnRatio,
            branchPrunedP95DeltaImprovementPx = (parity.p95AbsDeltaPx - branchPrunedParity.p95AbsDeltaPx).coerceAtLeast(0f),
            branchPrunedLargeDeltaReductionCount = (parity.largeDeltaColumnCount - branchPrunedParity.largeDeltaColumnCount)
                .coerceAtLeast(0),
            trunkPathAvailable = trunkPathCenterline.available,
            trunkPathMethod = trunkPathCenterline.method,
            trunkPathDecision = trunkPathDecision,
            trunkPathSelectedForSignal = false,
            trunkPathComponentCount = trunkPathCenterline.componentCount,
            trunkPathNodeCount = trunkPathCenterline.nodeCount,
            trunkPathEdgeCount = trunkPathCenterline.edgeCount,
            trunkPathEndpointCount = trunkPathCenterline.endpointCount,
            trunkPathJunctionCount = trunkPathCenterline.junctionCount,
            trunkPathPixelCount = trunkPathCenterline.trunkPixelCount,
            trunkPathColumnCount = trunkPathCenterline.trunkColumnCount,
            trunkPathCoverage = if (totalColumns > 0) {
                trunkPathCenterline.trunkColumnCount.toFloat() / totalColumns.toFloat()
            } else {
                0f
            },
            trunkPathSpurPixelCount = trunkPathCenterline.spurPixelCount,
            trunkPathMatchedColumnCount = trunkPathParity.matchedColumnCount,
            trunkPathMatchedColumnRatio = trunkPathParity.matchedColumnRatio,
            trunkPathMedianAbsDeltaPx = trunkPathParity.medianAbsDeltaPx,
            trunkPathP95AbsDeltaPx = trunkPathParity.p95AbsDeltaPx,
            trunkPathMaxAbsDeltaPx = trunkPathParity.maxAbsDeltaPx,
            trunkPathLargeDeltaColumnCount = trunkPathParity.largeDeltaColumnCount,
            trunkPathLargeDeltaColumnRatio = trunkPathParity.largeDeltaColumnRatio,
            trunkPathP95DeltaImprovementPx = (parity.p95AbsDeltaPx - trunkPathParity.p95AbsDeltaPx).coerceAtLeast(0f),
            trunkPathLargeDeltaReductionCount = (parity.largeDeltaColumnCount - trunkPathParity.largeDeltaColumnCount)
                .coerceAtLeast(0),
            fragmentReconstructionAvailable = fragmentReconstruction.available,
            fragmentReconstructionMethod = fragmentReconstruction.method,
            fragmentReconstructionDecision = fragmentReconstructionDecision,
            fragmentReconstructionSelectedForSignal = false,
            fragmentReconstructionComponentCount = fragmentReconstruction.componentCount,
            fragmentReconstructionRetainedComponentCount = fragmentReconstruction.retainedComponentCount,
            fragmentReconstructionDiscardedComponentCount = fragmentReconstruction.discardedComponentCount,
            fragmentReconstructionGuideColumnCount = fragmentReconstruction.guideColumnCount,
            fragmentReconstructionGuideMaxDistancePx = fragmentReconstruction.guideMaxDistancePx,
            fragmentReconstructionGuideMatchedPixelCount = fragmentReconstruction.guideMatchedPixelCount,
            fragmentReconstructionGuideRejectedPixelCount = fragmentReconstruction.guideRejectedPixelCount,
            fragmentReconstructionGuideRejectedInterpolatedColumnCount = fragmentReconstruction.guideRejectedInterpolatedColumnCount,
            fragmentReconstructionRawColumnCount = fragmentReconstruction.rawColumnCount,
            fragmentReconstructionInterpolatedColumnCount = fragmentReconstruction.interpolatedColumnCount,
            fragmentReconstructionColumnCount = fragmentReconstruction.pointsByX.size,
            fragmentReconstructionCoverage = if (totalColumns > 0) {
                fragmentReconstruction.pointsByX.size.toFloat() / totalColumns.toFloat()
            } else {
                0f
            },
            fragmentReconstructionMaxInterpolatedGapPx = fragmentReconstruction.maxInterpolatedGapPx,
            fragmentReconstructionMatchedColumnCount = fragmentReconstructionParity.matchedColumnCount,
            fragmentReconstructionMatchedColumnRatio = fragmentReconstructionParity.matchedColumnRatio,
            fragmentReconstructionMedianAbsDeltaPx = fragmentReconstructionParity.medianAbsDeltaPx,
            fragmentReconstructionP95AbsDeltaPx = fragmentReconstructionParity.p95AbsDeltaPx,
            fragmentReconstructionMaxAbsDeltaPx = fragmentReconstructionParity.maxAbsDeltaPx,
            fragmentReconstructionLargeDeltaColumnCount = fragmentReconstructionParity.largeDeltaColumnCount,
            fragmentReconstructionLargeDeltaColumnRatio = fragmentReconstructionParity.largeDeltaColumnRatio,
            fragmentReconstructionP95DeltaImprovementPx = (parity.p95AbsDeltaPx - fragmentReconstructionParity.p95AbsDeltaPx)
                .coerceAtLeast(0f),
            fragmentReconstructionLargeDeltaReductionCount = (parity.largeDeltaColumnCount - fragmentReconstructionParity.largeDeltaColumnCount)
                .coerceAtLeast(0),
            fragmentReconstructionResidualAcceptanceGate = fragmentReconstructionResidualReview.acceptanceGate,
            fragmentReconstructionResidualColumnCount = fragmentReconstructionResidualReview.residualColumnCount,
            fragmentReconstructionResidualPeakTopCandidateColumnCount =
                fragmentReconstructionResidualReview.peakTopCandidateColumnCount,
            fragmentReconstructionResidualBranchEdgeAmbiguityColumnCount =
                fragmentReconstructionResidualReview.branchEdgeAmbiguityColumnCount,
            fragmentReconstructionResidualBaselineGapColumnCount =
                fragmentReconstructionResidualReview.baselineGapColumnCount,
            fragmentReconstructionResidualFrameTextArtifactColumnCount =
                fragmentReconstructionResidualReview.frameTextArtifactColumnCount,
            fragmentReconstructionResidualCropBoundaryColumnCount =
                fragmentReconstructionResidualReview.cropBoundaryColumnCount,
            fragmentReconstructionResidualSignalGuideMismatchColumnCount =
                fragmentReconstructionResidualReview.signalGuideMismatchColumnCount,
            fragmentReconstructionResidualUnclassifiedColumnCount =
                fragmentReconstructionResidualReview.unclassifiedColumnCount,
            skeletonPixelCount = skeletonPixelCount,
            skeletonColumnCount = skeletonColumns,
            centerlineColumnCount = centerlineColumns,
            skeletonPointCount = skeletonPointCount,
            fallbackPointCount = fallbackPointCount,
            wideClusterColumnCount = wideClusterColumnCount,
            branchColumnCount = branchColumnCount,
            centerlineCoverage = centerlineCoverage,
            skeletonCoverage = skeletonCoverage,
            warnings = warnings,
        )
    }

    private fun List<CurvePoint>.centerlineParity(
        centerlineCandidateByX: Map<Int, Float>,
        branchCandidateColumns: Set<Int>,
    ): CenterlineParity {
        if (isEmpty() || centerlineCandidateByX.isEmpty()) return CenterlineParity()
        val comparisons = mapNotNull { point ->
            centerlineCandidateByX[point.pixelX]?.let { centerlineY ->
                CenterlineDelta(
                    x = point.pixelX,
                    signedDeltaPx = centerlineY - point.pixelY,
                    absDeltaPx = abs(centerlineY - point.pixelY),
                    nearBranch = branchCandidateColumns.hasNear(point.pixelX, radius = 2),
                )
            }
        }
        if (comparisons.isEmpty()) return CenterlineParity()
        val deltas = comparisons.map { it.absDeltaPx }.sorted()
        val largeDeltas = comparisons.filter { it.absDeltaPx > CENTERLINE_LARGE_DELTA_THRESHOLD_PX }
        val nearBranchCount = largeDeltas.count { it.nearBranch }
        val signalAboveCount = largeDeltas.count { it.signedDeltaPx > CENTERLINE_LARGE_DELTA_THRESHOLD_PX }
        val signalBelowCount = largeDeltas.count { it.signedDeltaPx < -CENTERLINE_LARGE_DELTA_THRESHOLD_PX }
        return CenterlineParity(
            compared = true,
            matchedColumnCount = deltas.size,
            matchedColumnRatio = deltas.size.toFloat() / size.toFloat(),
            medianAbsDeltaPx = deltas.percentile(0.50f),
            p95AbsDeltaPx = deltas.percentile(0.95f),
            maxAbsDeltaPx = deltas.last(),
            largeDeltaColumnCount = largeDeltas.size,
            largeDeltaColumnRatio = largeDeltas.size.toFloat() / deltas.size.toFloat(),
            largeDeltaNearBranchColumnCount = nearBranchCount,
            largeDeltaNearBranchColumnRatio = nearBranchCount.toFloat() / largeDeltas.size.coerceAtLeast(1).toFloat(),
            largeDeltaSignalAboveCenterlineColumnCount = signalAboveCount,
            largeDeltaSignalAboveCenterlineColumnRatio = signalAboveCount.toFloat() / largeDeltas.size.coerceAtLeast(1).toFloat(),
            largeDeltaSignalBelowCenterlineColumnCount = signalBelowCount,
            largeDeltaSignalBelowCenterlineColumnRatio = signalBelowCount.toFloat() / largeDeltas.size.coerceAtLeast(1).toFloat(),
        )
    }

    private fun Set<Int>.hasNear(value: Int, radius: Int): Boolean =
        (value - radius..value + radius).any { it in this }

    private fun selectBranchPrunedCenterline(
        centerlineCandidateByX: Map<Int, Float>,
        branchCandidateColumns: Set<Int>,
        signalPoints: List<CurvePoint>,
    ): BranchPrunedCenterline {
        val dropped = centerlineCandidateByX.branchDroppedCandidate(branchCandidateColumns)
        val continuity = centerlineCandidateByX.branchPrunedContinuityCandidate(branchCandidateColumns)
        val droppedParity = signalPoints.centerlineParity(dropped.pointsByX, branchCandidateColumns)
        val continuityParity = signalPoints.centerlineParity(continuity.pointsByX, branchCandidateColumns)
        return if (continuityParity.isMetricSafeComparedTo(droppedParity)) continuity else dropped
    }

    private fun Map<Int, Float>.branchDroppedCandidate(
        branchCandidateColumns: Set<Int>,
    ): BranchPrunedCenterline {
        if (isEmpty()) return BranchPrunedCenterline(pointsByX = emptyMap())
        val branchNearColumns = keys
            .filter { x -> branchCandidateColumns.hasNear(x, radius = CENTERLINE_BRANCH_NEAR_RADIUS) }
            .toSet()
        return BranchPrunedCenterline(
            pointsByX = filterKeys { it !in branchNearColumns }.toSortedMap(),
            method = "drop_branch_neighborhood_columns_radius_2",
            removedColumnCount = branchNearColumns.size,
        )
    }

    private fun Map<Int, Float>.branchPrunedContinuityCandidate(
        branchCandidateColumns: Set<Int>,
    ): BranchPrunedCenterline {
        if (isEmpty()) return BranchPrunedCenterline(pointsByX = emptyMap())
        val sortedKeys = keys.sorted()
        val branchNearColumns = sortedKeys
            .filter { x -> branchCandidateColumns.hasNear(x, radius = CENTERLINE_BRANCH_NEAR_RADIUS) }
            .toSet()
        if (branchNearColumns.isEmpty()) {
            return BranchPrunedCenterline(
                pointsByX = toSortedMap(),
                method = "branch_pruned_continuity_no_branch_columns",
            )
        }

        val stableKeys = sortedKeys.filter { it !in branchNearColumns }
        if (stableKeys.size < 2) {
            return BranchPrunedCenterline(
                pointsByX = filterKeys { it !in branchNearColumns }.toSortedMap(),
                method = BRANCH_PRUNED_CONTINUITY_METHOD,
                removedColumnCount = branchNearColumns.size,
            )
        }

        val stableSet = stableKeys.toSet()
        val result = mutableMapOf<Int, Float>()
        var interpolatedColumnCount = 0
        var removedColumnCount = 0
        sortedKeys.forEach { x ->
            val originalY = this[x] ?: return@forEach
            if (x !in branchNearColumns) {
                result[x] = originalY
                return@forEach
            }

            val left = stableKeys.lastOrNull { it < x }
            val right = stableKeys.firstOrNull { it > x }
            val gap = if (left != null && right != null) right - left else Int.MAX_VALUE
            if (
                left != null &&
                right != null &&
                left in stableSet &&
                right in stableSet &&
                gap <= CENTERLINE_BRANCH_INTERPOLATION_MAX_GAP
            ) {
                val leftY = this[left] ?: originalY
                val rightY = this[right] ?: originalY
                val ratio = (x - left).toFloat() / gap.toFloat()
                result[x] = leftY + (rightY - leftY) * ratio
                interpolatedColumnCount++
            } else {
                removedColumnCount++
            }
        }

        return BranchPrunedCenterline(
            pointsByX = result.toSortedMap(),
            method = BRANCH_PRUNED_CONTINUITY_METHOD,
            removedColumnCount = removedColumnCount,
            interpolatedColumnCount = interpolatedColumnCount,
        )
    }

    private fun CenterlineParity.isMetricSafeComparedTo(previous: CenterlineParity): Boolean {
        if (!compared) return false
        if (!previous.compared) return true
        val overlapImproved = matchedColumnRatio >= previous.matchedColumnRatio + 0.05f
        val p95Improved = p95AbsDeltaPx + 1f < previous.p95AbsDeltaPx
        val largeDeltaReduced = largeDeltaColumnCount + 5 < previous.largeDeltaColumnCount
        val p95Preserved = p95AbsDeltaPx <= previous.p95AbsDeltaPx + 1f
        val largeDeltaPreserved = largeDeltaColumnCount <= previous.largeDeltaColumnCount + 5
        return (overlapImproved || p95Improved || largeDeltaReduced) && p95Preserved && largeDeltaPreserved
    }

    private fun branchPrunedDecision(
        original: CenterlineParity,
        pruned: CenterlineParity,
    ): String =
        when {
            !pruned.compared -> "branch_pruned_not_available"
            pruned.matchedColumnRatio < 0.45f -> "branch_pruned_low_overlap"
            original.compared && pruned.p95AbsDeltaPx >= original.p95AbsDeltaPx -> "branch_pruned_no_p95_improvement"
            pruned.p95AbsDeltaPx > CENTERLINE_LARGE_DELTA_THRESHOLD_PX -> "branch_pruned_improved_but_large_delta"
            else -> "branch_pruned_candidate_ready_for_visual_review"
        }

    private fun trunkPathDecision(
        original: CenterlineParity,
        branchPruned: CenterlineParity,
        trunkPath: CenterlineParity,
    ): String {
        if (!trunkPath.compared) return "trunk_path_not_available"
        if (trunkPath.matchedColumnRatio < 0.45f) return "trunk_path_low_overlap"
        val referenceP95 = listOfNotNull(
            original.takeIf { it.compared }?.p95AbsDeltaPx,
            branchPruned.takeIf { it.compared }?.p95AbsDeltaPx,
        ).minOrNull() ?: Float.MAX_VALUE
        val referenceLargeDeltas = listOfNotNull(
            original.takeIf { it.compared }?.largeDeltaColumnCount,
            branchPruned.takeIf { it.compared }?.largeDeltaColumnCount,
        ).minOrNull() ?: Int.MAX_VALUE
        return when {
            trunkPath.p95AbsDeltaPx >= referenceP95 &&
                trunkPath.largeDeltaColumnCount >= referenceLargeDeltas -> "trunk_path_no_metric_improvement"
            trunkPath.p95AbsDeltaPx > CENTERLINE_LARGE_DELTA_THRESHOLD_PX -> "trunk_path_improved_but_large_delta"
            else -> "trunk_path_candidate_ready_for_visual_review"
        }
    }

    private fun fragmentReconstructionDecision(
        original: CenterlineParity,
        branchPruned: CenterlineParity,
        trunkPath: CenterlineParity,
        fragmentReconstruction: CenterlineParity,
    ): String {
        if (!fragmentReconstruction.compared) return "fragment_reconstruction_not_available"
        if (fragmentReconstruction.matchedColumnRatio < 0.45f) return "fragment_reconstruction_low_overlap"
        val referenceP95 = listOfNotNull(
            original.takeIf { it.compared }?.p95AbsDeltaPx,
            branchPruned.takeIf { it.compared }?.p95AbsDeltaPx,
            trunkPath.takeIf { it.compared }?.p95AbsDeltaPx,
        ).minOrNull() ?: Float.MAX_VALUE
        val referenceLargeDeltas = listOfNotNull(
            original.takeIf { it.compared }?.largeDeltaColumnCount,
            branchPruned.takeIf { it.compared }?.largeDeltaColumnCount,
            trunkPath.takeIf { it.compared }?.largeDeltaColumnCount,
        ).minOrNull() ?: Int.MAX_VALUE
        return when {
            fragmentReconstruction.p95AbsDeltaPx >= referenceP95 &&
                fragmentReconstruction.largeDeltaColumnCount >= referenceLargeDeltas ->
                "fragment_reconstruction_no_metric_improvement"
            fragmentReconstruction.p95AbsDeltaPx > CENTERLINE_LARGE_DELTA_THRESHOLD_PX ->
                "fragment_reconstruction_improved_but_large_delta"
            else -> "fragment_reconstruction_candidate_ready_for_visual_review"
        }
    }

    private fun List<Float>.percentile(fraction: Float): Float {
        if (isEmpty()) return 0f
        val index = ((size - 1) * fraction.coerceIn(0f, 1f)).roundToInt()
        return this[index.coerceIn(indices)]
    }

    private fun skeletonize(mask: BooleanArray, width: Int, height: Int): BooleanArray {
        val skeleton = mask.copyOf()
        if (width < 3 || height < 3) return skeleton
        var changed: Boolean
        var iteration = 0
        do {
            changed = false
            val firstPass = mutableListOf<Int>()
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    val index = y * width + x
                    if (!skeleton[index]) continue
                    if (skeleton.shouldRemoveZhangSuen(width, x, y, firstPass = true)) {
                        firstPass += index
                    }
                }
            }
            firstPass.forEach { skeleton[it] = false }
            changed = changed || firstPass.isNotEmpty()

            val secondPass = mutableListOf<Int>()
            for (y in 1 until height - 1) {
                for (x in 1 until width - 1) {
                    val index = y * width + x
                    if (!skeleton[index]) continue
                    if (skeleton.shouldRemoveZhangSuen(width, x, y, firstPass = false)) {
                        secondPass += index
                    }
                }
            }
            secondPass.forEach { skeleton[it] = false }
            changed = changed || secondPass.isNotEmpty()
            iteration++
        } while (changed && iteration < 80)
        return skeleton
    }

    private fun BooleanArray.shouldRemoveZhangSuen(
        width: Int,
        x: Int,
        y: Int,
        firstPass: Boolean,
    ): Boolean {
        val p2 = this[(y - 1) * width + x]
        val p3 = this[(y - 1) * width + x + 1]
        val p4 = this[y * width + x + 1]
        val p5 = this[(y + 1) * width + x + 1]
        val p6 = this[(y + 1) * width + x]
        val p7 = this[(y + 1) * width + x - 1]
        val p8 = this[y * width + x - 1]
        val p9 = this[(y - 1) * width + x - 1]
        val neighbors = listOf(p2, p3, p4, p5, p6, p7, p8, p9)
        val activeNeighbors = neighbors.count { it }
        if (activeNeighbors !in 2..6) return false
        val transitions = (neighbors + p2)
            .zipWithNext()
            .count { (current, next) -> !current && next }
        if (transitions != 1) return false
        return if (firstPass) {
            !(p2 && p4 && p6) && !(p4 && p6 && p8)
        } else {
            !(p2 && p4 && p8) && !(p2 && p6 && p8)
        }
    }

    private fun saveCurveOverlay(
        mask: BooleanArray,
        skeletonMask: BooleanArray,
        points: List<CurvePoint>,
        w: Int,
        h: Int,
        path: String,
    ) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val bgPixels = IntArray(w * h) { i ->
            when {
                skeletonMask[i] -> 0xFF_45_BF_68.toInt()
                mask[i] -> 0xFF_40_40_40.toInt()
                else -> 0xFF_10_10_10.toInt()
            }
        }
        bmp.setPixels(bgPixels, 0, w, 0, 0, w, h)

        val canvas = Canvas(bmp)
        val paint = Paint().apply {
            color = Color.RED
            strokeWidth = 2f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }

        val sorted = points.sortedBy { it.pixelX }
        for (i in 0 until sorted.size - 1) {
            val p1 = sorted[i]
            val p2 = sorted[i + 1]
            if (p2.pixelX - p1.pixelX <= 3) {
                canvas.drawLine(
                    p1.pixelX.toFloat(),
                    p1.pixelY,
                    p2.pixelX.toFloat(),
                    p2.pixelY,
                    paint,
                )
            }
        }

        val interpPaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.FILL
        }
        for (p in sorted) {
            if (p.confidence == CurvePoint.INTERPOLATED) {
                canvas.drawCircle(p.pixelX.toFloat(), p.pixelY, 2f, interpPaint)
            }
        }

        FileOutputStream(path).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        bmp.recycle()
    }

    private fun saveCenterlineParityOverlay(
        mask: BooleanArray,
        skeletonMask: BooleanArray,
        signalPoints: List<CurvePoint>,
        centerlineCandidateByX: Map<Int, Float>,
        branchCandidateColumns: Set<Int>,
        w: Int,
        h: Int,
        path: String,
    ) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val bgPixels = IntArray(w * h) { i ->
            when {
                skeletonMask[i] -> 0xFF_6E_D6_9A.toInt()
                mask[i] -> 0xFF_4A_4A_4A.toInt()
                else -> 0xFF_12_12_12.toInt()
            }
        }
        bmp.setPixels(bgPixels, 0, w, 0, 0, w, h)

        val canvas = Canvas(bmp)
        drawFloatPolyline(canvas, centerlineCandidateByX, Color.CYAN, 2f)
        drawFloatPolyline(canvas, signalPoints.associate { it.pixelX to it.pixelY }, Color.RED, 2f)

        val largeDeltaPaint = Paint().apply {
            color = Color.rgb(251, 140, 0)
            alpha = 216
            strokeWidth = 1f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        val markerPaint = Paint().apply {
            color = Color.rgb(251, 140, 0)
            alpha = 216
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        signalPoints.forEach { point ->
            val centerlineY = centerlineCandidateByX[point.pixelX] ?: return@forEach
            val delta = abs(centerlineY - point.pixelY)
            if (delta > CENTERLINE_LARGE_DELTA_THRESHOLD_PX) {
                largeDeltaPaint.color = when {
                    branchCandidateColumns.hasNear(point.pixelX, radius = 2) -> Color.rgb(171, 71, 188)
                    centerlineY > point.pixelY -> Color.rgb(251, 140, 0)
                    else -> Color.rgb(255, 213, 79)
                }
                markerPaint.color = largeDeltaPaint.color
                canvas.drawLine(point.pixelX.toFloat(), point.pixelY, point.pixelX.toFloat(), centerlineY, largeDeltaPaint)
                canvas.drawCircle(point.pixelX.toFloat(), centerlineY, 2f, markerPaint)
            }
        }

        FileOutputStream(path).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        bmp.recycle()
    }

    private fun saveBranchPrunedCenterlineOverlay(
        mask: BooleanArray,
        skeletonMask: BooleanArray,
        signalPoints: List<CurvePoint>,
        centerlineCandidateByX: Map<Int, Float>,
        branchPrunedCenterlineByX: Map<Int, Float>,
        branchCandidateColumns: Set<Int>,
        w: Int,
        h: Int,
        path: String,
    ) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val bgPixels = IntArray(w * h) { i ->
            when {
                skeletonMask[i] -> 0xFF_6E_D6_9A.toInt()
                mask[i] -> 0xFF_4A_4A_4A.toInt()
                else -> 0xFF_12_12_12.toInt()
            }
        }
        bmp.setPixels(bgPixels, 0, w, 0, 0, w, h)

        val canvas = Canvas(bmp)
        drawFloatPolyline(canvas, centerlineCandidateByX, Color.CYAN, 1.5f)
        drawFloatPolyline(canvas, signalPoints.associate { it.pixelX to it.pixelY }, Color.RED, 2f)
        drawFloatPolyline(canvas, branchPrunedCenterlineByX, Color.rgb(67, 160, 71), 2.5f)

        val removedPaint = Paint().apply {
            color = Color.rgb(171, 71, 188)
            alpha = 216
            strokeWidth = 1f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        val markerPaint = Paint().apply {
            color = Color.rgb(171, 71, 188)
            alpha = 216
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        branchCandidateColumns.forEach { x ->
            val y = centerlineCandidateByX[x] ?: return@forEach
            canvas.drawLine(x.toFloat(), 0f, x.toFloat(), (h - 1).toFloat(), removedPaint)
            canvas.drawCircle(x.toFloat(), y, 2f, markerPaint)
        }

        FileOutputStream(path).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        bmp.recycle()
    }

    private fun saveTrunkPathCenterlineOverlay(
        mask: BooleanArray,
        skeletonMask: BooleanArray,
        signalPoints: List<CurvePoint>,
        branchPrunedCenterlineByX: Map<Int, Float>,
        trunkPathCenterlineByX: Map<Int, Float>,
        w: Int,
        h: Int,
        path: String,
    ) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val bgPixels = IntArray(w * h) { i ->
            when {
                skeletonMask[i] -> 0xFF_6E_D6_9A.toInt()
                mask[i] -> 0xFF_4A_4A_4A.toInt()
                else -> 0xFF_12_12_12.toInt()
            }
        }
        bmp.setPixels(bgPixels, 0, w, 0, 0, w, h)

        val canvas = Canvas(bmp)
        drawFloatPolyline(canvas, signalPoints.associate { it.pixelX to it.pixelY }, Color.RED, 2f)
        drawFloatPolyline(canvas, branchPrunedCenterlineByX, Color.rgb(67, 160, 71), 1.8f)
        drawFloatPolyline(canvas, trunkPathCenterlineByX, Color.rgb(30, 136, 229), 2.8f)

        FileOutputStream(path).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        bmp.recycle()
    }

    private fun saveFragmentReconstructionOverlay(
        mask: BooleanArray,
        skeletonMask: BooleanArray,
        signalPoints: List<CurvePoint>,
        branchPrunedCenterlineByX: Map<Int, Float>,
        trunkPathCenterlineByX: Map<Int, Float>,
        fragmentReconstructionByX: Map<Int, Float>,
        residualReview: ReconstructedTraceResidualReview,
        w: Int,
        h: Int,
        path: String,
    ) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val bgPixels = IntArray(w * h) { i ->
            when {
                skeletonMask[i] -> 0xFF_6E_D6_9A.toInt()
                mask[i] -> 0xFF_4A_4A_4A.toInt()
                else -> 0xFF_12_12_12.toInt()
            }
        }
        bmp.setPixels(bgPixels, 0, w, 0, 0, w, h)

        val canvas = Canvas(bmp)
        drawFloatPolyline(canvas, signalPoints.associate { it.pixelX to it.pixelY }, Color.RED, 2f)
        drawFloatPolyline(canvas, branchPrunedCenterlineByX, Color.rgb(67, 160, 71), 1.4f)
        drawFloatPolyline(canvas, trunkPathCenterlineByX, Color.rgb(30, 136, 229), 1.8f)
        drawFloatPolyline(canvas, fragmentReconstructionByX, Color.rgb(142, 36, 170), 2.8f)
        drawResidualMarkers(canvas, residualReview.residuals)

        FileOutputStream(path).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        bmp.recycle()
    }

    private fun drawResidualMarkers(
        canvas: Canvas,
        residuals: List<ReconstructedTraceResidual>,
    ) {
        val linePaint = Paint().apply {
            strokeWidth = 1.2f
            alpha = 230
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        val markerPaint = Paint().apply {
            alpha = 230
            isAntiAlias = true
            style = Paint.Style.FILL
        }
        residuals.forEach { residual ->
            val color = residual.classification.androidColor()
            linePaint.color = color
            markerPaint.color = color
            canvas.drawLine(
                residual.x.toFloat(),
                residual.signalY,
                residual.x.toFloat(),
                residual.reconstructedY,
                linePaint,
            )
            canvas.drawCircle(residual.x.toFloat(), residual.reconstructedY, 2f, markerPaint)
        }
    }

    private fun ReconstructedTraceResidualClass.androidColor(): Int =
        when (this) {
            ReconstructedTraceResidualClass.PEAK_TOP_CANDIDATE -> Color.rgb(0, 150, 136)
            ReconstructedTraceResidualClass.BRANCH_EDGE_AMBIGUITY -> Color.rgb(171, 71, 188)
            ReconstructedTraceResidualClass.BASELINE_GAP -> Color.rgb(255, 179, 0)
            ReconstructedTraceResidualClass.FRAME_TEXT_ARTIFACT -> Color.rgb(216, 27, 96)
            ReconstructedTraceResidualClass.CROP_BOUNDARY -> Color.rgb(94, 53, 177)
            ReconstructedTraceResidualClass.SIGNAL_GUIDE_MISMATCH -> Color.rgb(244, 81, 30)
            ReconstructedTraceResidualClass.UNCLASSIFIED -> Color.rgb(84, 96, 104)
        }

    private fun drawFloatPolyline(
        canvas: Canvas,
        pointsByX: Map<Int, Float>,
        color: Int,
        strokeWidth: Float,
    ) {
        val paint = Paint().apply {
            this.color = color
            this.strokeWidth = strokeWidth
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        var previousX: Int? = null
        var previousY: Float? = null
        pointsByX.toSortedMap().forEach { (x, y) ->
            val lastX = previousX
            val lastY = previousY
            if (lastX != null && lastY != null && x - lastX <= 3) {
                canvas.drawLine(lastX.toFloat(), lastY, x.toFloat(), y, paint)
            }
            previousX = x
            previousY = y
        }
    }

    private fun emptyResult(totalColumns: Int): CurveExtractionResult = CurveExtractionResult(
        points = emptyList(),
        maskImagePath = null,
        totalColumns = totalColumns,
        extractedColumns = 0,
        interpolatedColumns = 0,
        outlierCount = 0,
        centerlineAudit = CurveCenterlineAudit(),
        warnings = listOf("curve_extract.mask_not_readable"),
        timestamp = System.currentTimeMillis(),
    )
}

private data class BranchPrunedCenterline(
    val pointsByX: Map<Int, Float>,
    val method: String = "not_available",
    val removedColumnCount: Int = 0,
    val interpolatedColumnCount: Int = 0,
)

private data class CenterlineParity(
    val compared: Boolean = false,
    val matchedColumnCount: Int = 0,
    val matchedColumnRatio: Float = 0f,
    val medianAbsDeltaPx: Float = 0f,
    val p95AbsDeltaPx: Float = 0f,
    val maxAbsDeltaPx: Float = 0f,
    val largeDeltaColumnCount: Int = 0,
    val largeDeltaColumnRatio: Float = 0f,
    val largeDeltaNearBranchColumnCount: Int = 0,
    val largeDeltaNearBranchColumnRatio: Float = 0f,
    val largeDeltaSignalAboveCenterlineColumnCount: Int = 0,
    val largeDeltaSignalAboveCenterlineColumnRatio: Float = 0f,
    val largeDeltaSignalBelowCenterlineColumnCount: Int = 0,
    val largeDeltaSignalBelowCenterlineColumnRatio: Float = 0f,
)

private data class CenterlineDelta(
    val x: Int,
    val signedDeltaPx: Float,
    val absDeltaPx: Float,
    val nearBranch: Boolean,
)

private const val CENTERLINE_LARGE_DELTA_THRESHOLD_PX = 6f
private const val CENTERLINE_BRANCH_NEAR_RADIUS = 2
private const val CENTERLINE_BRANCH_INTERPOLATION_MAX_GAP = 18
private const val BRANCH_PRUNED_CONTINUITY_METHOD =
    "continuity_interpolated_branch_neighborhood_radius_2_max_gap_18"

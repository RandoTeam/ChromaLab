package com.chromalab.feature.processing.curve

import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.roundToInt

actual class CurveExtractor actual constructor() {
    actual fun extract(
        maskPath: String,
        maskWidth: Int,
        maskHeight: Int,
        outputDir: String,
    ): CurveExtractionResult {
        val image = ImageIO.read(File(maskPath))
            ?: return emptyResult(maskWidth)
        val width = image.width
        val height = image.height
        val mask = BooleanArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                mask[y * width + x] = image.getRGB(x, y).isWhiteMaskPixel()
            }
        }
        image.flush()

        val skeletonMask = skeletonize(mask, width, height)
        val rawPoints = mutableListOf<CurvePoint>()
        val gapColumns = mutableListOf<Int>()
        var skeletonPointCount = 0
        var fallbackPointCount = 0
        var wideClusterColumnCount = 0
        var branchColumnCount = 0
        val centerlineCandidateByX = mutableMapOf<Int, Float>()
        val branchCandidateColumns = mutableSetOf<Int>()
        for (x in 0 until width) {
            val skeletonCandidates = mutableListOf<Int>()
            val candidates = mutableListOf<Int>()
            for (y in 0 until height) {
                if (skeletonMask[y * width + x]) skeletonCandidates += y
                if (mask[y * width + x]) candidates += y
            }

            val skeletonCluster = skeletonCandidates.bestSignalCluster(height)
            if (skeletonCluster != null) {
                val branchCandidate = skeletonCandidates.clusters(maxGap = 2).size > 1 || skeletonCluster.size > 2
                if (branchCandidate) {
                    branchColumnCount++
                    branchCandidateColumns += x
                }
                centerlineCandidateByX[x] = skeletonCluster.centerY()
                skeletonPointCount++
            }

            if (candidates.isEmpty()) {
                gapColumns += x
                continue
            }

            val cluster = candidates.bestSignalCluster(height)
            if (cluster == null) {
                gapColumns += x
                continue
            }
            if (cluster.size > 8) wideClusterColumnCount++
            rawPoints += CurvePoint(
                pixelX = x,
                pixelY = cluster.first().toFloat(),
                confidence = cluster.confidence(),
            )
            fallbackPointCount++
        }

        val interpolatedPoints = interpolateShortGaps(rawPoints, gapColumns, maxGap = 6)
        val points = (rawPoints + interpolatedPoints).sortedBy { it.pixelX }
        val branchPrunedCenterline = selectBranchPrunedCenterline(
            centerlineCandidateByX = centerlineCandidateByX,
            branchCandidateColumns = branchCandidateColumns,
            signalPoints = rawPoints,
        )
        val initialCenterlineAudit = buildCenterlineAudit(
            skeletonMask = skeletonMask,
            totalColumns = width,
            centerlineColumns = rawPoints.size,
            skeletonPointCount = skeletonPointCount,
            fallbackPointCount = fallbackPointCount,
            wideClusterColumnCount = wideClusterColumnCount,
            branchColumnCount = branchColumnCount,
            signalPoints = rawPoints,
            centerlineCandidateByX = centerlineCandidateByX,
            branchPrunedCenterline = branchPrunedCenterline,
            branchCandidateColumns = branchCandidateColumns,
        )
        val overlayPath = File(outputDir).also { it.mkdirs() }
            .resolve("curve_overlay.png")
            .absolutePath
        saveOverlay(mask, skeletonMask, points, width, height, overlayPath)
        val parityOverlayPath = File(outputDir)
            .resolve("centerline_parity_overlay.png")
            .absolutePath
        saveCenterlineParityOverlay(
            mask = mask,
            skeletonMask = skeletonMask,
            signalPoints = rawPoints,
            centerlineCandidateByX = centerlineCandidateByX,
            branchCandidateColumns = branchCandidateColumns,
            width = width,
            height = height,
            path = parityOverlayPath,
        )
        val prunedOverlayPath = File(outputDir)
            .resolve("centerline_branch_pruned_overlay.png")
            .absolutePath
        saveBranchPrunedCenterlineOverlay(
            mask = mask,
            skeletonMask = skeletonMask,
            signalPoints = rawPoints,
            centerlineCandidateByX = centerlineCandidateByX,
            branchPrunedCenterlineByX = branchPrunedCenterline.pointsByX,
            branchCandidateColumns = branchCandidateColumns,
            width = width,
            height = height,
            path = prunedOverlayPath,
        )
        val centerlineAudit = initialCenterlineAudit.copy(
            parityOverlayGenerated = true,
            branchPrunedOverlayGenerated = true,
        )

        val result = CurveExtractionResult(
            points = points,
            maskImagePath = overlayPath,
            totalColumns = width,
            extractedColumns = rawPoints.size,
            interpolatedColumns = interpolatedPoints.size,
            outlierCount = 0,
            centerlineAudit = centerlineAudit,
            warnings = emptyList(),
            timestamp = System.currentTimeMillis(),
        )
        val warnings = buildList {
            if (result.coverage < 0.35f && !result.isSparseTraceUsable) add("curve_extract.low_column_coverage")
            if (result.isSparseTraceUsable && result.coverage <= 0.3f) {
                add("curve_extract.sparse_trace_low_column_coverage_accepted")
            }
            if (result.isLocalizedSparseTrace) add("curve_extract.sparse_trace_localized_review_required")
            if (interpolatedPoints.size > rawPoints.size * 0.35f) add("curve_extract.many_short_gap_interpolations")
            if (rawPoints.isEmpty()) add("curve_extract.no_curve_points")
        }

        return result.copy(warnings = warnings)
    }

    private fun List<Int>.bestSignalCluster(height: Int): List<Int>? {
        val clusters = clusters(maxGap = 2)
        val supportStart = height * 0.34f
        val supportedClusters = clusters.filter { it.last() >= supportStart }
        if (supportedClusters.isEmpty()) return null
        return supportedClusters.maxWithOrNull(
            compareBy<List<Int>> { cluster ->
                val span = cluster.last() - cluster.first() + 1
                val bottomSupport = (cluster.last().toFloat() / height.coerceAtLeast(1).toFloat() * 24f).toInt()
                span + bottomSupport
            }.thenBy { it.last() },
        )
    }

    private fun List<Int>.clusters(maxGap: Int): List<List<Int>> {
        if (isEmpty()) return emptyList()
        val sorted = sorted()
        val clusters = mutableListOf<List<Int>>()
        var start = 0
        for (index in 1 until sorted.size) {
            if (sorted[index] - sorted[index - 1] > maxGap) {
                clusters += sorted.subList(start, index)
                start = index
            }
        }
        clusters += sorted.subList(start, sorted.size)
        return clusters
    }

    private fun List<Int>.confidence(): Float =
        when {
            size <= 4 -> CurvePoint.HIGH_CONFIDENCE
            size <= 18 -> 0.8f
            else -> CurvePoint.LOW_CONFIDENCE
        }

    private fun List<Int>.centerY(): Float =
        average().toFloat()

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
        val branchPrunedDecision = branchPrunedDecision(
            original = parity,
            pruned = branchPrunedParity,
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
            if (skeletonPointCount > 0 && fallbackPointCount > skeletonPointCount) add("curve_centerline.low_skeleton_support")
            if (centerlineCoverage < 0.05f) add("curve_centerline.low_centerline_coverage")
            if (!parity.compared) add("curve_centerline.parity_not_available")
            if (parity.compared && parity.p95AbsDeltaPx > CENTERLINE_LARGE_DELTA_THRESHOLD_PX) {
                add("curve_centerline.large_signal_delta")
            }
            if (branchColumnCount > centerlineColumns * 0.20f && branchColumnCount > 8) {
                add("curve_centerline.many_branch_columns")
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

    private fun saveOverlay(
        mask: BooleanArray,
        skeletonMask: BooleanArray,
        points: List<CurvePoint>,
        width: Int,
        height: Int,
        path: String,
    ) {
        val overlay = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                overlay.setRGB(
                    x,
                    y,
                    when {
                        skeletonMask[index] -> SKELETON_RGB
                        mask[index] -> MASK_RGB
                        else -> BACKGROUND_RGB
                    },
                )
            }
        }

        val graphics = overlay.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.color = Color(0xE5, 0x39, 0x35)
            graphics.stroke = BasicStroke(2f)
            val sorted = points.sortedBy { it.pixelX }
            for (i in 0 until sorted.lastIndex) {
                val first = sorted[i]
                val second = sorted[i + 1]
                if (second.pixelX - first.pixelX <= 3) {
                    graphics.drawLine(
                        first.pixelX,
                        first.pixelY.toInt(),
                        second.pixelX,
                        second.pixelY.toInt(),
                    )
                }
            }
            graphics.color = Color(0xFF, 0xD5, 0x4F)
            sorted.filter { it.confidence == CurvePoint.INTERPOLATED }.forEach { point ->
                graphics.fillOval(point.pixelX - 1, point.pixelY.toInt() - 1, 3, 3)
            }
        } finally {
            graphics.dispose()
        }
        ImageIO.write(overlay, "png", File(path))
        overlay.flush()
    }

    private fun saveCenterlineParityOverlay(
        mask: BooleanArray,
        skeletonMask: BooleanArray,
        signalPoints: List<CurvePoint>,
        centerlineCandidateByX: Map<Int, Float>,
        branchCandidateColumns: Set<Int>,
        width: Int,
        height: Int,
        path: String,
    ) {
        val overlay = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                overlay.setRGB(
                    x,
                    y,
                    when {
                        skeletonMask[index] -> PARITY_SKELETON_RGB
                        mask[index] -> PARITY_MASK_RGB
                        else -> PARITY_BACKGROUND_RGB
                    },
                )
            }
        }

        val signalByX = signalPoints.associateBy { it.pixelX }
        val graphics = overlay.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            drawFloatPolyline(graphics, centerlineCandidateByX, Color(0x00, 0xBC, 0xD4), 2f)
            drawFloatPolyline(graphics, signalByX.mapValues { it.value.pixelY }, Color(0xE5, 0x39, 0x35), 2f)
            graphics.stroke = BasicStroke(1f)
            signalByX.forEach { (x, point) ->
                val centerlineY = centerlineCandidateByX[x] ?: return@forEach
                val delta = abs(centerlineY - point.pixelY)
                if (delta > CENTERLINE_LARGE_DELTA_THRESHOLD_PX) {
                    graphics.color = when {
                        branchCandidateColumns.hasNear(x, radius = 2) -> Color(0xAB, 0x47, 0xBC, 0xD8)
                        centerlineY > point.pixelY -> Color(0xFB, 0x8C, 0x00, 0xD8)
                        else -> Color(0xFF, 0xD5, 0x4F, 0xD8)
                    }
                    graphics.drawLine(x, point.pixelY.roundToInt(), x, centerlineY.roundToInt())
                    graphics.fillOval(x - 2, centerlineY.roundToInt() - 2, 5, 5)
                }
            }
        } finally {
            graphics.dispose()
        }
        ImageIO.write(overlay, "png", File(path))
        overlay.flush()
    }

    private fun saveBranchPrunedCenterlineOverlay(
        mask: BooleanArray,
        skeletonMask: BooleanArray,
        signalPoints: List<CurvePoint>,
        centerlineCandidateByX: Map<Int, Float>,
        branchPrunedCenterlineByX: Map<Int, Float>,
        branchCandidateColumns: Set<Int>,
        width: Int,
        height: Int,
        path: String,
    ) {
        val overlay = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                overlay.setRGB(
                    x,
                    y,
                    when {
                        skeletonMask[index] -> PARITY_SKELETON_RGB
                        mask[index] -> PARITY_MASK_RGB
                        else -> PARITY_BACKGROUND_RGB
                    },
                )
            }
        }

        val graphics = overlay.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            drawFloatPolyline(graphics, centerlineCandidateByX, Color(0x00, 0xBC, 0xD4, 0x88), 1.5f)
            drawFloatPolyline(graphics, signalPoints.associate { it.pixelX to it.pixelY }, Color(0xE5, 0x39, 0x35), 2f)
            drawFloatPolyline(graphics, branchPrunedCenterlineByX, Color(0x43, 0xA0, 0x47), 2.5f)
            graphics.stroke = BasicStroke(1f)
            graphics.color = Color(0xAB, 0x47, 0xBC, 0xD8)
            branchCandidateColumns.forEach { x ->
                val y = centerlineCandidateByX[x]?.roundToInt() ?: return@forEach
                graphics.drawLine(x, 0, x, height - 1)
                graphics.fillOval(x - 2, y - 2, 5, 5)
            }
        } finally {
            graphics.dispose()
        }
        ImageIO.write(overlay, "png", File(path))
        overlay.flush()
    }

    private fun drawFloatPolyline(
        graphics: java.awt.Graphics2D,
        pointsByX: Map<Int, Float>,
        color: Color,
        strokeWidth: Float,
    ) {
        val sorted = pointsByX.toSortedMap()
        graphics.color = color
        graphics.stroke = BasicStroke(strokeWidth)
        var previous: Map.Entry<Int, Float>? = null
        sorted.forEach { entry ->
            val last = previous
            if (last != null && entry.key - last.key <= 3) {
                graphics.drawLine(last.key, last.value.roundToInt(), entry.key, entry.value.roundToInt())
            }
            previous = entry
        }
    }

    private fun emptyResult(totalColumns: Int): CurveExtractionResult =
        CurveExtractionResult(
            points = emptyList(),
            maskImagePath = null,
            totalColumns = totalColumns,
            extractedColumns = 0,
            interpolatedColumns = 0,
            outlierCount = 0,
            warnings = listOf("curve_extract.mask_not_readable"),
            timestamp = System.currentTimeMillis(),
        )

    private companion object {
        private const val BACKGROUND_RGB = -0x00EFEFF0
        private const val MASK_RGB = -0x00BFBFC0
        private const val SKELETON_RGB = -0x00A040A0
        private const val PARITY_BACKGROUND_RGB = -0x00F8F8F9
        private const val PARITY_MASK_RGB = -0x00D6D6D7
        private const val PARITY_SKELETON_RGB = -0x0090D0B0
        private const val CENTERLINE_LARGE_DELTA_THRESHOLD_PX = 6f
        private const val CENTERLINE_BRANCH_NEAR_RADIUS = 2
        private const val CENTERLINE_BRANCH_INTERPOLATION_MAX_GAP = 18
        private const val BRANCH_PRUNED_CONTINUITY_METHOD =
            "continuity_interpolated_branch_neighborhood_radius_2_max_gap_18"
    }
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

private fun Int.isWhiteMaskPixel(): Boolean =
    ((this shr 16) and 0xFF) > 128 ||
        ((this shr 8) and 0xFF) > 128 ||
        (this and 0xFF) > 128

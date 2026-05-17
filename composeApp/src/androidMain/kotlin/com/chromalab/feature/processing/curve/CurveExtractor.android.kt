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

            val cluster = clusters(candidates).maxByOrNull { it.size } ?: emptyList()
            if (cluster.size > 8) wideClusterColumnCount++
            val confidence = if (cluster.size <= 5) CurvePoint.HIGH_CONFIDENCE else CurvePoint.LOW_CONFIDENCE
            rawPoints.add(CurvePoint(x, cluster.average().toFloat(), confidence))
            fallbackPointCount++
        }

        val outlierCount = removeOutliers(rawPoints, windowSize = 15, threshold = 3.0f)
        val allPoints = interpolateGaps(rawPoints, gapColumns, w)
        val interpolatedCount = allPoints.count { it.confidence == CurvePoint.INTERPOLATED }
        val branchPrunedCenterlineByX = centerlineCandidateByX.withoutBranchColumns(branchCandidateColumns)
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
            branchPrunedCenterlineByX = branchPrunedCenterlineByX,
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
            branchPrunedCenterlineByX = branchPrunedCenterlineByX,
            branchCandidateColumns = branchCandidateColumns,
            w = w,
            h = h,
            path = prunedOverlayPath,
        )
        val centerlineAudit = initialCenterlineAudit.copy(
            parityOverlayGenerated = true,
            branchPrunedOverlayGenerated = true,
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

    private fun removeOutliers(
        points: MutableList<CurvePoint>,
        windowSize: Int,
        threshold: Float,
    ): Int {
        if (points.size < windowSize) return 0

        val toRemove = mutableSetOf<Int>()
        for (i in points.indices) {
            val start = maxOf(0, i - windowSize / 2)
            val end = minOf(points.size, i + windowSize / 2 + 1)
            val window = points.subList(start, end).map { it.pixelY }.sorted()
            val median = window[window.size / 2]
            val mad = window.map { abs(it - median) }.sorted()[window.size / 2]
            val madScale = if (mad > 0) mad * 1.4826f else 1f

            if (abs(points[i].pixelY - median) > threshold * madScale) {
                toRemove.add(i)
            }
        }

        toRemove.sortedDescending().forEach { points.removeAt(it) }
        return toRemove.size
    }

    private fun interpolateGaps(
        known: List<CurvePoint>,
        gaps: List<Int>,
        totalWidth: Int,
    ): List<CurvePoint> {
        if (known.isEmpty()) return emptyList()

        val result = known.toMutableList()
        for (gapX in gaps) {
            val left = known.lastOrNull { it.pixelX < gapX }
            val right = known.firstOrNull { it.pixelX > gapX }

            val interpolatedY = when {
                left != null && right != null -> {
                    val t = (gapX - left.pixelX).toFloat() / (right.pixelX - left.pixelX)
                    left.pixelY + t * (right.pixelY - left.pixelY)
                }
                left != null -> left.pixelY
                right != null -> right.pixelY
                else -> continue
            }

            result.add(CurvePoint(gapX, interpolatedY, CurvePoint.INTERPOLATED))
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
        branchPrunedCenterlineByX: Map<Int, Float>,
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
        val branchPrunedParity = signalPoints.centerlineParity(branchPrunedCenterlineByX, branchCandidateColumns)
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
            branchPrunedMethod = "drop_branch_neighborhood_columns_radius_2",
            branchPrunedDecision = branchPrunedDecision,
            branchPrunedSelectedForSignal = false,
            branchPrunedRemovedColumnCount = centerlineCandidateByX.size - branchPrunedCenterlineByX.size,
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

    private fun Map<Int, Float>.withoutBranchColumns(branchCandidateColumns: Set<Int>): Map<Int, Float> =
        filterKeys { x -> !branchCandidateColumns.hasNear(x, radius = 2) }

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

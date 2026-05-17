package com.chromalab.feature.processing.curve

import kotlin.math.abs
import kotlin.math.roundToInt

data class FragmentedTraceReconstruction(
    val pointsByX: Map<Int, Float> = emptyMap(),
    val method: String = "not_available",
    val componentCount: Int = 0,
    val retainedComponentCount: Int = 0,
    val discardedComponentCount: Int = 0,
    val guideColumnCount: Int = 0,
    val guideMaxDistancePx: Int = 0,
    val guideMatchedPixelCount: Int = 0,
    val guideRejectedPixelCount: Int = 0,
    val guideRejectedInterpolatedColumnCount: Int = 0,
    val rawColumnCount: Int = 0,
    val interpolatedColumnCount: Int = 0,
    val maxInterpolatedGapPx: Int = 0,
) {
    val available: Boolean get() = pointsByX.isNotEmpty()
}

object FragmentedTraceReconstructionExtractor {
    fun extract(
        skeletonMask: BooleanArray,
        width: Int,
        height: Int,
        signalGuideByX: Map<Int, Float> = emptyMap(),
    ): FragmentedTraceReconstruction {
        if (width <= 0 || height <= 0 || skeletonMask.isEmpty()) return FragmentedTraceReconstruction()

        val guideMaxDistancePx = signalGuideMaxDistancePx(height, signalGuideByX)
        val labels = IntArray(skeletonMask.size) { UNVISITED_COMPONENT }
        val components = mutableListOf<ComponentStats>()
        var componentId = 0
        skeletonMask.indices.forEach { index ->
            if (!skeletonMask[index] || labels[index] != UNVISITED_COMPONENT) return@forEach
            components += floodComponent(
                skeletonMask = skeletonMask,
                labels = labels,
                width = width,
                startIndex = index,
                componentId = componentId,
                signalGuideByX = signalGuideByX,
                guideMaxDistancePx = guideMaxDistancePx,
            )
            componentId++
        }
        if (components.isEmpty()) return FragmentedTraceReconstruction()

        val retainedIds = components.indices
            .filter { id -> components[id].isRetained }
            .toSet()
        val rawPointsByX = mutableMapOf<Int, Float>()
        var guideMatchedPixelCount = 0
        var guideRejectedPixelCount = 0
        labels.forEachIndexed { index, component ->
            if (component !in retainedIds) return@forEachIndexed
            val x = index % width
            val y = index / width
            if (signalGuideByX.isNotEmpty()) {
                val guideY = signalGuideByX.nearestGuideY(x)
                if (guideY == null || abs(y.toFloat() - guideY) > guideMaxDistancePx) {
                    guideRejectedPixelCount++
                    return@forEachIndexed
                }
                guideMatchedPixelCount++
            }
            val previous = rawPointsByX[x]
            if (previous == null || y < previous) rawPointsByX[x] = y.toFloat()
        }

        val maxInterpolationGap = maxOf(
            MIN_INTERPOLATION_GAP_PX,
            minOf(MAX_INTERPOLATION_GAP_PX, (width * MAX_INTERPOLATION_GAP_RATIO).toInt()),
        )
        val interpolated = rawPointsByX.interpolateShortGaps(
            maxGap = maxInterpolationGap,
            signalGuideByX = signalGuideByX,
            guideMaxDistancePx = guideMaxDistancePx,
        )

        return FragmentedTraceReconstruction(
            pointsByX = (rawPointsByX + interpolated.pointsByX).toSortedMap(),
            method = if (signalGuideByX.isEmpty()) UNGUIDED_METHOD else SIGNAL_GUIDED_METHOD,
            componentCount = components.size,
            retainedComponentCount = retainedIds.size,
            discardedComponentCount = components.size - retainedIds.size,
            guideColumnCount = signalGuideByX.size,
            guideMaxDistancePx = guideMaxDistancePx,
            guideMatchedPixelCount = guideMatchedPixelCount,
            guideRejectedPixelCount = guideRejectedPixelCount,
            guideRejectedInterpolatedColumnCount = interpolated.rejectedByGuideCount,
            rawColumnCount = rawPointsByX.size,
            interpolatedColumnCount = interpolated.pointsByX.size,
            maxInterpolatedGapPx = maxInterpolationGap,
        )
    }

    private fun floodComponent(
        skeletonMask: BooleanArray,
        labels: IntArray,
        width: Int,
        startIndex: Int,
        componentId: Int,
        signalGuideByX: Map<Int, Float>,
        guideMaxDistancePx: Int,
    ): ComponentStats {
        val queue = IntArray(skeletonMask.size)
        var head = 0
        var tail = 0
        queue[tail++] = startIndex
        labels[startIndex] = componentId
        var size = 0
        var minX = width
        var maxX = 0
        var guidePixelCount = 0
        while (head < tail) {
            val pixel = queue[head++]
            val x = pixel % width
            val y = pixel / width
            minX = minOf(minX, x)
            maxX = maxOf(maxX, x)
            size++
            val guideY = signalGuideByX.nearestGuideY(x)
            if (guideY != null && abs(y.toFloat() - guideY) <= guideMaxDistancePx) {
                guidePixelCount++
            }
            forEachNeighbor(pixel, width, skeletonMask.size / width) { neighbor ->
                if (skeletonMask[neighbor] && labels[neighbor] == UNVISITED_COMPONENT) {
                    labels[neighbor] = componentId
                    queue[tail++] = neighbor
                }
            }
        }
        return ComponentStats(
            size = size,
            xSpan = maxX - minX + 1,
            guidePixelCount = guidePixelCount,
            hasSignalGuide = signalGuideByX.isNotEmpty(),
        )
    }

    private fun Map<Int, Float>.interpolateShortGaps(
        maxGap: Int,
        signalGuideByX: Map<Int, Float>,
        guideMaxDistancePx: Int,
    ): InterpolationResult {
        if (size < 2) return InterpolationResult()
        val sortedKeys = keys.sorted()
        val result = mutableMapOf<Int, Float>()
        var rejectedByGuideCount = 0
        for (index in 0 until sortedKeys.lastIndex) {
            val leftX = sortedKeys[index]
            val rightX = sortedKeys[index + 1]
            val gapWidth = rightX - leftX - 1
            if (gapWidth !in 1..maxGap) continue
            val leftY = getValue(leftX)
            val rightY = getValue(rightX)
            val span = rightX - leftX
            for (x in leftX + 1 until rightX) {
                val ratio = (x - leftX).toFloat() / span.toFloat()
                val interpolatedY = leftY + (rightY - leftY) * ratio
                val guideY = signalGuideByX.nearestGuideY(x)
                if (signalGuideByX.isNotEmpty() && guideY == null) {
                    rejectedByGuideCount++
                } else if (guideY != null && abs(interpolatedY - guideY) > guideMaxDistancePx) {
                    rejectedByGuideCount++
                } else {
                    result[x] = interpolatedY
                }
            }
        }
        return InterpolationResult(
            pointsByX = result,
            rejectedByGuideCount = rejectedByGuideCount,
        )
    }

    private fun signalGuideMaxDistancePx(height: Int, signalGuideByX: Map<Int, Float>): Int {
        if (signalGuideByX.isEmpty()) return 0
        return maxOf(
            MIN_SIGNAL_GUIDE_DISTANCE_PX,
            minOf(MAX_SIGNAL_GUIDE_DISTANCE_PX, (height * SIGNAL_GUIDE_DISTANCE_RATIO).roundToInt()),
        )
    }

    private fun Map<Int, Float>.nearestGuideY(x: Int): Float? {
        if (isEmpty()) return null
        this[x]?.let { return it }
        for (offset in 1..SIGNAL_GUIDE_X_RADIUS) {
            this[x - offset]?.let { return it }
            this[x + offset]?.let { return it }
        }
        return null
    }

    private inline fun forEachNeighbor(
        index: Int,
        width: Int,
        height: Int,
        block: (Int) -> Unit,
    ) {
        val x = index % width
        val y = index / width
        for (dy in -1..1) {
            for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height) {
                    block(ny * width + nx)
                }
            }
        }
    }

    private data class ComponentStats(
        val size: Int,
        val xSpan: Int,
        val guidePixelCount: Int,
        val hasSignalGuide: Boolean,
    ) {
        val isRetained: Boolean
            get() {
                if (size < MIN_COMPONENT_SIZE && xSpan < MIN_COMPONENT_X_SPAN) return false
                if (!hasSignalGuide) return true
                return guidePixelCount >= MIN_SIGNAL_GUIDE_COMPONENT_PIXELS ||
                    guidePixelCount.toFloat() / size.coerceAtLeast(1).toFloat() >= MIN_SIGNAL_GUIDE_COMPONENT_RATIO
            }
    }

    private data class InterpolationResult(
        val pointsByX: Map<Int, Float> = emptyMap(),
        val rejectedByGuideCount: Int = 0,
    )

    private const val UNGUIDED_METHOD = "fragmented_skeleton_component_top_envelope_short_gap_interpolation_audit_only"
    private const val SIGNAL_GUIDED_METHOD =
        "signal_guided_fragmented_skeleton_component_top_envelope_short_gap_interpolation_audit_only"
    private const val MIN_COMPONENT_SIZE = 3
    private const val MIN_COMPONENT_X_SPAN = 2
    private const val MIN_SIGNAL_GUIDE_COMPONENT_PIXELS = 2
    private const val MIN_SIGNAL_GUIDE_COMPONENT_RATIO = 0.05f
    private const val MIN_SIGNAL_GUIDE_DISTANCE_PX = 18
    private const val MAX_SIGNAL_GUIDE_DISTANCE_PX = 72
    private const val SIGNAL_GUIDE_DISTANCE_RATIO = 0.10f
    private const val SIGNAL_GUIDE_X_RADIUS = 4
    private const val MIN_INTERPOLATION_GAP_PX = 8
    private const val MAX_INTERPOLATION_GAP_PX = 24
    private const val MAX_INTERPOLATION_GAP_RATIO = 0.03f
    private const val UNVISITED_COMPONENT = -1
}

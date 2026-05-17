package com.chromalab.feature.processing.curve

data class FragmentedTraceReconstruction(
    val pointsByX: Map<Int, Float> = emptyMap(),
    val method: String = "not_available",
    val componentCount: Int = 0,
    val retainedComponentCount: Int = 0,
    val discardedComponentCount: Int = 0,
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
    ): FragmentedTraceReconstruction {
        if (width <= 0 || height <= 0 || skeletonMask.isEmpty()) return FragmentedTraceReconstruction()

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
            )
            componentId++
        }
        if (components.isEmpty()) return FragmentedTraceReconstruction()

        val retainedIds = components.indices
            .filter { id -> components[id].isRetained }
            .toSet()
        val rawPointsByX = mutableMapOf<Int, Float>()
        labels.forEachIndexed { index, component ->
            if (component !in retainedIds) return@forEachIndexed
            val x = index % width
            val y = index / width
            val previous = rawPointsByX[x]
            if (previous == null || y < previous) rawPointsByX[x] = y.toFloat()
        }

        val maxInterpolationGap = maxOf(
            MIN_INTERPOLATION_GAP_PX,
            minOf(MAX_INTERPOLATION_GAP_PX, (width * MAX_INTERPOLATION_GAP_RATIO).toInt()),
        )
        val interpolated = rawPointsByX.interpolateShortGaps(maxInterpolationGap)

        return FragmentedTraceReconstruction(
            pointsByX = (rawPointsByX + interpolated).toSortedMap(),
            method = METHOD,
            componentCount = components.size,
            retainedComponentCount = retainedIds.size,
            discardedComponentCount = components.size - retainedIds.size,
            rawColumnCount = rawPointsByX.size,
            interpolatedColumnCount = interpolated.size,
            maxInterpolatedGapPx = maxInterpolationGap,
        )
    }

    private fun floodComponent(
        skeletonMask: BooleanArray,
        labels: IntArray,
        width: Int,
        startIndex: Int,
        componentId: Int,
    ): ComponentStats {
        val queue = IntArray(skeletonMask.size)
        var head = 0
        var tail = 0
        queue[tail++] = startIndex
        labels[startIndex] = componentId
        var size = 0
        var minX = width
        var maxX = 0
        while (head < tail) {
            val pixel = queue[head++]
            val x = pixel % width
            minX = minOf(minX, x)
            maxX = maxOf(maxX, x)
            size++
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
        )
    }

    private fun Map<Int, Float>.interpolateShortGaps(maxGap: Int): Map<Int, Float> {
        if (size < 2) return emptyMap()
        val sortedKeys = keys.sorted()
        val result = mutableMapOf<Int, Float>()
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
                result[x] = leftY + (rightY - leftY) * ratio
            }
        }
        return result
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
    ) {
        val isRetained: Boolean
            get() = size >= MIN_COMPONENT_SIZE || xSpan >= MIN_COMPONENT_X_SPAN
    }

    private const val METHOD = "fragmented_skeleton_component_top_envelope_short_gap_interpolation_audit_only"
    private const val MIN_COMPONENT_SIZE = 3
    private const val MIN_COMPONENT_X_SPAN = 2
    private const val MIN_INTERPOLATION_GAP_PX = 8
    private const val MAX_INTERPOLATION_GAP_PX = 24
    private const val MAX_INTERPOLATION_GAP_RATIO = 0.03f
    private const val UNVISITED_COMPONENT = -1
}

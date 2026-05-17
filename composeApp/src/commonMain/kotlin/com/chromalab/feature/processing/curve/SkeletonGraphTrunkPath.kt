package com.chromalab.feature.processing.curve

import kotlin.math.abs

data class SkeletonGraphTrunkPath(
    val pointsByX: Map<Int, Float> = emptyMap(),
    val method: String = "not_available",
    val componentCount: Int = 0,
    val nodeCount: Int = 0,
    val edgeCount: Int = 0,
    val endpointCount: Int = 0,
    val junctionCount: Int = 0,
    val trunkPixelCount: Int = 0,
    val trunkColumnCount: Int = 0,
    val spurPixelCount: Int = 0,
) {
    val available: Boolean get() = pointsByX.isNotEmpty()
}

object SkeletonGraphTrunkPathExtractor {
    fun extract(
        skeletonMask: BooleanArray,
        width: Int,
        height: Int,
    ): SkeletonGraphTrunkPath {
        if (width <= 0 || height <= 0 || skeletonMask.isEmpty()) return SkeletonGraphTrunkPath()
        val componentLabels = IntArray(skeletonMask.size) { UNVISITED_COMPONENT }
        val componentSizes = mutableListOf<Int>()
        val componentMinX = mutableListOf<Int>()
        val componentMaxX = mutableListOf<Int>()
        var componentId = 0
        for (index in skeletonMask.indices) {
            if (!skeletonMask[index] || componentLabels[index] != UNVISITED_COMPONENT) continue
            val bounds = floodComponent(
                skeletonMask = skeletonMask,
                labels = componentLabels,
                width = width,
                height = height,
                startIndex = index,
                componentId = componentId,
            )
            componentSizes += bounds.size
            componentMinX += bounds.minX
            componentMaxX += bounds.maxX
            componentId++
        }
        if (componentId == 0) return SkeletonGraphTrunkPath()

        val selectedComponent = componentSizes.indices.maxWithOrNull(
            compareBy<Int> { id -> componentMaxX[id] - componentMinX[id] }
                .thenBy { id -> componentSizes[id] },
        ) ?: return SkeletonGraphTrunkPath(componentCount = componentId)
        val skeletonPixels = componentLabels.indices.filter { componentLabels[it] != UNVISITED_COMPONENT }
        if (skeletonPixels.isEmpty()) return SkeletonGraphTrunkPath(componentCount = componentId)
        val degreeByIndex = IntArray(skeletonMask.size)
        var endpointCount = 0
        var junctionCount = 0
        var edgeTouchCount = 0
        skeletonPixels.forEach { pixel ->
            val degree = skeletonMask.neighborCount(pixel, width, height)
            degreeByIndex[pixel] = degree
            if (degree <= 1) endpointCount++
            if (degree >= 3) junctionCount++
            edgeTouchCount += degree
        }

        val endpoints = skeletonPixels.filter { degreeByIndex[it] <= 1 }
        val endpointBridges = buildEndpointBridges(
            endpoints = endpoints,
            componentLabels = componentLabels,
            width = width,
            height = height,
        )
        val candidatePixels = if (endpoints.size >= 2) endpoints else skeletonPixels
        val leftCandidates = candidatePixels.sortedWith(
            compareBy<Int> { it % width }.thenByDescending { it / width },
        ).take(MAX_SIDE_CANDIDATES)
        val rightCandidates = candidatePixels.sortedWith(
            compareByDescending<Int> { it % width }.thenByDescending { it / width },
        ).take(MAX_SIDE_CANDIDATES)

        val selectedComponentPixels = skeletonPixels.filter { componentLabels[it] == selectedComponent }
        val bestPath = selectBestPath(
            skeletonMask = skeletonMask,
            componentLabels = componentLabels,
            endpointBridges = endpointBridges,
            allowedComponent = null,
            width = width,
            height = height,
            leftCandidates = leftCandidates,
            rightCandidates = rightCandidates,
            minX = componentMinX.minOrNull() ?: 0,
            maxX = componentMaxX.maxOrNull() ?: width - 1,
        ).ifEmpty {
            val componentEndpoints = selectedComponentPixels.filter { degreeByIndex[it] <= 1 }
            val componentCandidates = if (componentEndpoints.size >= 2) componentEndpoints else selectedComponentPixels
            selectBestPath(
                skeletonMask = skeletonMask,
                componentLabels = componentLabels,
                endpointBridges = emptyMap(),
                allowedComponent = selectedComponent,
                width = width,
                height = height,
                leftCandidates = componentCandidates.sortedWith(
                    compareBy<Int> { it % width }.thenByDescending { it / width },
                ).take(MAX_SIDE_CANDIDATES),
                rightCandidates = componentCandidates.sortedWith(
                    compareByDescending<Int> { it % width }.thenByDescending { it / width },
                ).take(MAX_SIDE_CANDIDATES),
                minX = componentMinX[selectedComponent],
                maxX = componentMaxX[selectedComponent],
            )
        }
        val pathPixels = bestPath.ifEmpty { selectedComponentPixels }
        val method = if (bestPath.isEmpty()) {
            COMPONENT_TOP_ENVELOPE_FALLBACK_METHOD
        } else {
            TRUNK_PATH_METHOD
        }
        val pointsByX = pathPixels
            .groupBy { it % width }
            .mapValues { (_, pixels) -> pixels.minOf { it / width }.toFloat() }
            .toSortedMap()

        return SkeletonGraphTrunkPath(
            pointsByX = pointsByX,
            method = method,
            componentCount = componentId,
            nodeCount = skeletonPixels.size,
            edgeCount = edgeTouchCount / 2 + endpointBridges.values.sumOf { it.size } / 2,
            endpointCount = endpointCount,
            junctionCount = junctionCount,
            trunkPixelCount = pathPixels.size,
            trunkColumnCount = pointsByX.size,
            spurPixelCount = (skeletonPixels.size - pathPixels.toSet().size).coerceAtLeast(0),
        )
    }

    private fun floodComponent(
        skeletonMask: BooleanArray,
        labels: IntArray,
        width: Int,
        height: Int,
        startIndex: Int,
        componentId: Int,
    ): ComponentBounds {
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
            forEachNeighbor(pixel, width, height) { neighbor ->
                if (skeletonMask[neighbor] && labels[neighbor] == UNVISITED_COMPONENT) {
                    labels[neighbor] = componentId
                    queue[tail++] = neighbor
                }
            }
        }
        return ComponentBounds(size = size, minX = minX, maxX = maxX)
    }

    private fun selectBestPath(
        skeletonMask: BooleanArray,
        componentLabels: IntArray,
        endpointBridges: Map<Int, List<Int>>,
        allowedComponent: Int?,
        width: Int,
        height: Int,
        leftCandidates: List<Int>,
        rightCandidates: List<Int>,
        minX: Int,
        maxX: Int,
    ): List<Int> {
        if (leftCandidates.isEmpty() || rightCandidates.isEmpty()) return emptyList()
        var best = emptyList<Int>()
        var bestScore = Float.NEGATIVE_INFINITY
        leftCandidates.forEach { start ->
            val tree = bfsTree(
                skeletonMask = skeletonMask,
                componentLabels = componentLabels,
                endpointBridges = endpointBridges,
                allowedComponent = allowedComponent,
                width = width,
                height = height,
                start = start,
            )
            rightCandidates.forEach { end ->
                if (start == end || tree.distance[end] < 0) return@forEach
                val path = tree.pathTo(end)
                if (path.isEmpty()) return@forEach
                val startX = start % width
                val endX = end % width
                val xSpan = abs(endX - startX)
                val score = xSpan * 8f +
                    path.size.toFloat() -
                    abs(startX - minX) * 2f -
                    abs(endX - maxX) * 2f
                if (score > bestScore) {
                    bestScore = score
                    best = path
                }
            }
        }
        return best
    }

    private fun bfsTree(
        skeletonMask: BooleanArray,
        componentLabels: IntArray,
        endpointBridges: Map<Int, List<Int>>,
        allowedComponent: Int?,
        width: Int,
        height: Int,
        start: Int,
    ): BfsTree {
        val previous = IntArray(skeletonMask.size) { NO_PREVIOUS }
        val distance = IntArray(skeletonMask.size) { UNREACHED_DISTANCE }
        val queue = IntArray(skeletonMask.size)
        var head = 0
        var tail = 0
        queue[tail++] = start
        previous[start] = PATH_START
        distance[start] = 0
        while (head < tail) {
            val pixel = queue[head++]
            forEachNeighbor(pixel, width, height) { neighbor ->
                if (
                    skeletonMask[neighbor] &&
                    componentLabels[neighbor] != UNVISITED_COMPONENT &&
                    (allowedComponent == null || componentLabels[neighbor] == allowedComponent) &&
                    distance[neighbor] == UNREACHED_DISTANCE
                ) {
                    previous[neighbor] = pixel
                    distance[neighbor] = distance[pixel] + 1
                    queue[tail++] = neighbor
                }
            }
            endpointBridges[pixel].orEmpty().forEach { bridged ->
                if (
                    componentLabels[bridged] != UNVISITED_COMPONENT &&
                    (allowedComponent == null || componentLabels[bridged] == allowedComponent) &&
                    distance[bridged] == UNREACHED_DISTANCE
                ) {
                    previous[bridged] = pixel
                    distance[bridged] = distance[pixel] + 1
                    queue[tail++] = bridged
                }
            }
        }
        return BfsTree(previous = previous, distance = distance)
    }

    private fun buildEndpointBridges(
        endpoints: List<Int>,
        componentLabels: IntArray,
        width: Int,
        height: Int,
    ): Map<Int, List<Int>> {
        if (endpoints.size < 2) return emptyMap()
        val maxYGap = maxOf(8, (height * MAX_BRIDGE_Y_GAP_RATIO).toInt())
        val mutable = mutableMapOf<Int, MutableList<Int>>()
        for (i in endpoints.indices) {
            val first = endpoints[i]
            val firstComponent = componentLabels[first]
            val firstX = first % width
            val firstY = first / width
            for (j in i + 1 until endpoints.size) {
                val second = endpoints[j]
                val secondComponent = componentLabels[second]
                if (firstComponent == secondComponent) continue
                val secondX = second % width
                val secondY = second / width
                val xGap = abs(secondX - firstX)
                val yGap = abs(secondY - firstY)
                if (xGap in 1..MAX_COMPONENT_BRIDGE_X_GAP && yGap <= maxYGap) {
                    mutable.getOrPut(first) { mutableListOf() } += second
                    mutable.getOrPut(second) { mutableListOf() } += first
                }
            }
        }
        return mutable
    }

    private fun BooleanArray.neighborCount(index: Int, width: Int, height: Int): Int {
        var count = 0
        forEachNeighbor(index, width, height) { neighbor ->
            if (this[neighbor]) count++
        }
        return count
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

    private data class ComponentBounds(
        val size: Int,
        val minX: Int,
        val maxX: Int,
    )

    private data class BfsTree(
        val previous: IntArray,
        val distance: IntArray,
    ) {
        fun pathTo(end: Int): List<Int> {
            if (distance[end] < 0) return emptyList()
            val result = mutableListOf<Int>()
            var current = end
            while (current >= 0) {
                result += current
                current = previous[current]
            }
            return result.asReversed()
        }
    }

    private const val MAX_SIDE_CANDIDATES = 16
    private const val MAX_COMPONENT_BRIDGE_X_GAP = 10
    private const val MAX_BRIDGE_Y_GAP_RATIO = 0.08f
    private const val UNVISITED_COMPONENT = -1
    private const val NO_PREVIOUS = -2
    private const val PATH_START = -1
    private const val UNREACHED_DISTANCE = -1
    private const val TRUNK_PATH_METHOD = "skeleton_graph_gap_bridged_left_right_trunk_path_top_envelope_audit_only"
    private const val COMPONENT_TOP_ENVELOPE_FALLBACK_METHOD =
        "skeleton_graph_component_top_envelope_fallback_audit_only"
}

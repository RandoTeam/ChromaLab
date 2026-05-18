package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion
import kotlin.math.abs
import kotlin.math.sqrt

class GraphMultiplicityResolver(
    private val duplicateIouThreshold: Float = 0.46f,
    private val nestedContainmentThreshold: Float = 0.72f,
    private val sameAxisHorizontalOverlapThreshold: Float = 0.72f,
    private val minDistinctGapRatio: Float = 0.035f,
) {
    fun resolve(
        candidates: List<GraphPanelBounds>,
        imageWidth: Int,
        imageHeight: Int,
        vlmGraphCountHint: Int? = null,
    ): GraphMultiplicityResolution {
        if (candidates.isEmpty()) {
            return GraphMultiplicityResolution(
                multiplicityStatus = GraphMultiplicityStatus.INVALID,
                warnings = listOf("multiplicity.no_graph_panel_candidates"),
            )
        }

        val selected = mutableListOf<GraphPanelBounds>()
        val duplicate = mutableListOf<RejectedGraphPanelCandidate>()
        val nested = mutableListOf<RejectedGraphPanelCandidate>()
        val subregion = mutableListOf<RejectedGraphPanelCandidate>()

        candidates.sortedWith(candidateComparator()).forEach { candidate ->
            val conflict = selected
                .mapNotNull { accepted -> candidate.classifyAgainst(accepted, imageWidth, imageHeight) }
                .firstOrNull()

            if (conflict == null) {
                selected += candidate
            } else {
                when {
                    GraphPanelRejectionReason.NESTED_INSIDE_SELECTED_PANEL in conflict.reasons -> nested += conflict
                    GraphPanelRejectionReason.DUPLICATE_IOU in conflict.reasons -> duplicate += conflict
                    else -> subregion += conflict
                }
            }
        }

        val sortedSelected = selected.sortedWith(readingOrderComparator())
        val distinct = sortedSelected.size
        val warnings = buildList {
            if (vlmGraphCountHint != null && vlmGraphCountHint > distinct) {
                add("multiplicity.vlm_count_advisory_reduced:$vlmGraphCountHint->$distinct")
            }
            if (duplicate.isNotEmpty()) add("multiplicity.duplicate_panels_rejected:${duplicate.size}")
            if (nested.isNotEmpty()) add("multiplicity.nested_panels_rejected:${nested.size}")
            if (subregion.isNotEmpty()) add("multiplicity.subregions_rejected:${subregion.size}")
            if (distinct == 1 && candidates.size > 1) add("multiplicity.single_physical_graph_from_overlapping_candidates")
        }

        return GraphMultiplicityResolution(
            resolvedGraphPanels = sortedSelected,
            rejectedDuplicatePanels = duplicate,
            rejectedNestedPanels = nested,
            rejectedSubregions = subregion,
            multiplicityStatus = when {
                sortedSelected.isEmpty() -> GraphMultiplicityStatus.INVALID
                sortedSelected.size == 1 -> {
                    if (vlmGraphCountHint != null && vlmGraphCountHint > 1) {
                        GraphMultiplicityStatus.SINGLE_GRAPH
                    } else {
                        GraphMultiplicityStatus.SINGLE_GRAPH
                    }
                }
                sortedSelected.areSpatiallyDistinct(imageHeight) -> GraphMultiplicityStatus.MULTI_GRAPH_VALID
                else -> GraphMultiplicityStatus.MULTI_GRAPH_REVIEW
            },
            warnings = warnings,
        )
    }

    private fun candidateComparator(): Comparator<GraphPanelBounds> =
        compareByDescending<GraphPanelBounds> { it.scoreBreakdown.total }
            .thenByDescending { it.confidence }
            .thenByDescending { it.region.area }

    private fun readingOrderComparator(): Comparator<GraphPanelBounds> =
        compareBy<GraphPanelBounds> { it.region.y }
            .thenBy { it.region.x }

    private fun GraphPanelBounds.classifyAgainst(
        accepted: GraphPanelBounds,
        imageWidth: Int,
        imageHeight: Int,
    ): RejectedGraphPanelCandidate? {
        val overlap = region.intersectionArea(accepted.region)
        if (overlap <= 0) return null

        val iou = overlap.toFloat() / (region.area + accepted.region.area - overlap).coerceAtLeast(1).toFloat()
        val containmentInAccepted = overlap.toFloat() / region.area.coerceAtLeast(1).toFloat()
        val acceptedContainedInCandidate = overlap.toFloat() / accepted.region.area.coerceAtLeast(1).toFloat()
        val sameAxis = region.sameAxisSystemAs(accepted.region, imageWidth, imageHeight)
        val reasons = buildList {
            if (iou >= duplicateIouThreshold) add(GraphPanelRejectionReason.DUPLICATE_IOU)
            if (containmentInAccepted >= nestedContainmentThreshold) {
                add(GraphPanelRejectionReason.NESTED_INSIDE_SELECTED_PANEL)
            }
            if (sameAxis) add(GraphPanelRejectionReason.SAME_AXIS_SYSTEM)
            if (acceptedContainedInCandidate >= nestedContainmentThreshold && scoreBreakdown.total <= accepted.scoreBreakdown.total) {
                add(GraphPanelRejectionReason.SUBREGION_NOT_GRAPH_PANEL)
            }
        }
        if (reasons.isEmpty()) return null

        return RejectedGraphPanelCandidate(
            candidate = this,
            rejectedAgainst = accepted,
            reasons = reasons.distinct(),
            iou = iou,
            containmentRatio = maxOf(containmentInAccepted, acceptedContainedInCandidate),
            notes = listOf(
                "multiplicity.iou:${iou.format2()}",
                "multiplicity.containment:${maxOf(containmentInAccepted, acceptedContainedInCandidate).format2()}",
                "multiplicity.same_axis_system:$sameAxis",
            ),
        )
    }

    private fun GraphRegion.sameAxisSystemAs(
        other: GraphRegion,
        imageWidth: Int,
        imageHeight: Int,
    ): Boolean {
        val horizontalOverlap = horizontalOverlapRatio(other)
        val verticalOverlap = verticalOverlapRatio(other)
        val centerDistance = centerDistanceTo(other)
        val imageDiagonal = sqrt(
            (imageWidth * imageWidth + imageHeight * imageHeight).toFloat(),
        ).coerceAtLeast(1f)
        return horizontalOverlap >= sameAxisHorizontalOverlapThreshold &&
            verticalOverlap >= 0.28f &&
            centerDistance / imageDiagonal <= 0.34f
    }

    private fun List<GraphPanelBounds>.areSpatiallyDistinct(imageHeight: Int): Boolean {
        if (size <= 1) return true
        val minGapPx = (imageHeight * minDistinctGapRatio).coerceAtLeast(8f)
        for (i in 0 until lastIndex) {
            val current = this[i].region
            val next = this[i + 1].region
            val verticalGap = next.y - current.bottom
            val horizontalGap = next.x - current.right
            val overlapsVertically = current.verticalOverlapRatio(next) > 0.10f
            val overlapsHorizontally = current.horizontalOverlapRatio(next) > 0.10f
            val separated = verticalGap >= minGapPx || horizontalGap >= minGapPx
            if (!separated && (overlapsVertically || overlapsHorizontally)) return false
        }
        return true
    }
}

internal fun GraphRegion.intersectionArea(other: GraphRegion): Int {
    val left = maxOf(x, other.x)
    val top = maxOf(y, other.y)
    val right = minOf(right, other.right)
    val bottom = minOf(bottom, other.bottom)
    return (right - left).coerceAtLeast(0) * (bottom - top).coerceAtLeast(0)
}

internal fun GraphRegion.horizontalOverlapRatio(other: GraphRegion): Float {
    val overlap = minOf(right, other.right) - maxOf(x, other.x)
    if (overlap <= 0) return 0f
    return overlap.toFloat() / minOf(width, other.width).coerceAtLeast(1).toFloat()
}

internal fun GraphRegion.verticalOverlapRatio(other: GraphRegion): Float {
    val overlap = minOf(bottom, other.bottom) - maxOf(y, other.y)
    if (overlap <= 0) return 0f
    return overlap.toFloat() / minOf(height, other.height).coerceAtLeast(1).toFloat()
}

private fun GraphRegion.centerDistanceTo(other: GraphRegion): Float {
    val dx = (x + width / 2f) - (other.x + other.width / 2f)
    val dy = (y + height / 2f) - (other.y + other.height / 2f)
    return sqrt(dx * dx + dy * dy)
}

private fun Float.format2(): String =
    (kotlin.math.round(this * 100f) / 100f).toString()

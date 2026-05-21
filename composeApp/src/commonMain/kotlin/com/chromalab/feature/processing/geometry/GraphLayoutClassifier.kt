package com.chromalab.feature.processing.geometry

import kotlin.math.abs

class GraphLayoutClassifier {
    fun classify(
        resolvedPanels: List<GraphPanelBounds>,
        rejectedDuplicatePanels: List<RejectedGraphPanelCandidate> = emptyList(),
        rejectedNestedPanels: List<RejectedGraphPanelCandidate> = emptyList(),
        rejectedSubregions: List<RejectedGraphPanelCandidate> = emptyList(),
        imageWidth: Int,
        imageHeight: Int,
    ): GraphLayoutClassification {
        if (resolvedPanels.isEmpty()) {
            return GraphLayoutClassification(
                layoutClass = GraphLayoutClass.UNKNOWN_REVIEW,
                physicalGraphCount = 0,
                confidence = 0f,
                reviewReasons = listOf("layout.no_resolved_panels"),
            )
        }

        val panels = resolvedPanels.sortedWith(compareBy<GraphPanelBounds> { it.region.y }.thenBy { it.region.x })
        if (panels.size == 1) {
            return classifySinglePanel(panels.single(), imageWidth, imageHeight)
        }

        val verticalStack = panels.isVerticalStack()
        val sharedHorizontalFrame = verticalStack && panels.horizontalOverlapRange() >= 0.78f
        val sameAxisReview = panels.hasSameAxisOverlap(imageHeight)
        val nestedOrDuplicateEvidence = rejectedDuplicatePanels.isNotEmpty() ||
            rejectedNestedPanels.isNotEmpty() ||
            rejectedSubregions.any { GraphPanelRejectionReason.SAME_AXIS_SYSTEM in it.reasons }
        if (sameAxisReview && nestedOrDuplicateEvidence) {
            return GraphLayoutClassification(
                layoutClass = GraphLayoutClass.DENSE_PEAK_SINGLE_AXIS,
                physicalGraphCount = 1,
                panelGroups = listOf(
                    GraphLayoutPanelGroup(
                        groupId = "single-axis-merged",
                        panelIndexes = panels.indices.map { it + 1 },
                        sharedXAxis = true,
                        sharedYAxis = true,
                        notes = listOf("layout.same_axis_overlap_candidates_collapsed"),
                    ),
                ),
                confidence = 0.74f,
                reviewReasons = listOf("layout.duplicate_or_nested_candidates_not_separate_graphs"),
            )
        }

        if (sharedHorizontalFrame) {
            val className = when {
                panels.size == 2 -> GraphLayoutClass.TWO_GRAPH_PAGE
                panels.size >= 3 -> GraphLayoutClass.STACKED_TRACES_SHARED_AXIS
                else -> GraphLayoutClass.UNKNOWN_REVIEW
            }
            return GraphLayoutClassification(
                layoutClass = className,
                physicalGraphCount = panels.size,
                panelGroups = panels.mapIndexed { index, _ ->
                    GraphLayoutPanelGroup(
                        groupId = "panel-${index + 1}",
                        panelIndexes = listOf(index + 1),
                        sharedXAxis = panels.size >= 3,
                        sharedYAxis = false,
                    )
                },
                confidence = 0.82f,
                reviewReasons = listOf("layout.vertical_panel_stack"),
            )
        }

        val className = when {
            panels.size >= 2 -> GraphLayoutClass.MULTI_PANEL_SEPARATE_AXES
            else -> GraphLayoutClass.UNKNOWN_REVIEW
        }
        return GraphLayoutClassification(
            layoutClass = className,
            physicalGraphCount = panels.size,
            panelGroups = panels.mapIndexed { index, _ ->
                GraphLayoutPanelGroup(
                    groupId = "panel-${index + 1}",
                    panelIndexes = listOf(index + 1),
                    sharedXAxis = sharedHorizontalFrame && panels.size >= 3,
                    sharedYAxis = false,
                )
            },
            confidence = if (sharedHorizontalFrame) 0.82f else 0.64f,
            reviewReasons = buildList {
                if (sharedHorizontalFrame) add("layout.vertical_panel_stack")
                if (!sharedHorizontalFrame) add("layout.multi_panel_separate_axes_review")
            },
        )
    }

    private fun classifySinglePanel(
        panel: GraphPanelBounds,
        imageWidth: Int,
        imageHeight: Int,
    ): GraphLayoutClassification {
        val imageArea = (imageWidth * imageHeight).coerceAtLeast(1)
        val areaRatio = panel.region.area.toFloat() / imageArea.toFloat()
        val aspect = panel.region.width.toFloat() / panel.region.height.coerceAtLeast(1).toFloat()
        val layoutClass = when {
            areaRatio < 0.08f -> GraphLayoutClass.LOW_RES_EXPORT_GRAPH
            panel.candidateSource == GeometryCandidateSource.SCREENSHOT_EMBEDDED_CHART -> GraphLayoutClass.EMBEDDED_SCREENSHOT_GRAPH
            aspect < 0.72f -> GraphLayoutClass.ROTATED_PAGE_GRAPH
            panel.scoreBreakdown.tracePixelDensity >= 8f && panel.scoreBreakdown.axisVisibility >= 12f ->
                GraphLayoutClass.SINGLE_TRACE_SINGLE_AXIS
            else -> GraphLayoutClass.UNKNOWN_REVIEW
        }
        return GraphLayoutClassification(
            layoutClass = layoutClass,
            physicalGraphCount = 1,
            panelGroups = listOf(GraphLayoutPanelGroup(groupId = "panel-1", panelIndexes = listOf(1))),
            confidence = when (layoutClass) {
                GraphLayoutClass.UNKNOWN_REVIEW -> 0.42f
                GraphLayoutClass.LOW_RES_EXPORT_GRAPH -> 0.58f
                else -> 0.76f
            },
            reviewReasons = buildList {
                add("layout.single_panel")
                if (layoutClass == GraphLayoutClass.UNKNOWN_REVIEW) add("layout.single_panel_low_semantic_confidence")
            },
        )
    }

    private fun List<GraphPanelBounds>.hasSameAxisOverlap(imageHeight: Int): Boolean {
        if (size < 2) return false
        for (i in 0 until lastIndex) {
            val current = this[i].region
            val next = this[i + 1].region
            val horizontal = current.horizontalOverlapRatio(next)
            val vertical = current.verticalOverlapRatio(next)
            val yGap = next.y - current.bottom
            val nearTouching = yGap <= maxOf(10, imageHeight / 120)
            val widthSimilar = abs(current.width - next.width).toFloat() / maxOf(current.width, next.width).coerceAtLeast(1) <= 0.18f
            if (horizontal >= 0.72f && (vertical > 0f || nearTouching) && widthSimilar) return true
        }
        return false
    }

    private fun List<GraphPanelBounds>.isVerticalStack(): Boolean =
        zipWithNext().all { (a, b) ->
            b.region.y >= a.region.y &&
                a.region.horizontalOverlapRatio(b.region) >= 0.55f
        }

    private fun List<GraphPanelBounds>.horizontalOverlapRange(): Float =
        zipWithNext()
            .map { (a, b) -> a.region.horizontalOverlapRatio(b.region) }
            .minOrNull()
            ?: 0f
}

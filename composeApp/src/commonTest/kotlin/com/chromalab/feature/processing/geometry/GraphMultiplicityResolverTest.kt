package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GraphMultiplicityResolverTest {
    private val resolver = GraphMultiplicityResolver()

    @Test
    fun overlappingCandidatesResolveToOnePhysicalGraph() {
        val resolution = resolver.resolve(
            candidates = listOf(
                panel(54, 35, 483, 276, 84f, "VLM hint"),
                panel(37, 25, 505, 296, 79f, "expanded"),
                panel(54, 35, 483, 276, 75f, "duplicate"),
                panel(246, 27, 296, 274, 68f, "subregion"),
                panel(256, 36, 284, 256, 62f, "subregion 2"),
            ),
            imageWidth = 542,
            imageHeight = 353,
            vlmGraphCountHint = 6,
        )

        assertEquals(GraphMultiplicityStatus.SINGLE_GRAPH, resolution.multiplicityStatus)
        assertEquals(1, resolution.resolvedGraphPanels.size)
        val allRejected = resolution.rejectedDuplicatePanels +
            resolution.rejectedNestedPanels +
            resolution.rejectedSubregions
        assertEquals(4, allRejected.size)
        assertTrue(
            resolution.rejectedNestedPanels.isNotEmpty() || resolution.rejectedSubregions.isNotEmpty(),
            "Nested/subregion candidates must not become separate graph reports.",
        )
        assertTrue(resolution.warnings.any { it.startsWith("multiplicity.vlm_count_advisory_reduced") })
    }

    @Test
    fun spatiallyDistinctStackedPanelsRemainMultipleGraphs() {
        val resolution = resolver.resolve(
            candidates = listOf(
                panel(40, 40, 460, 180, 82f, "top graph"),
                panel(42, 300, 458, 185, 80f, "bottom graph"),
            ),
            imageWidth = 560,
            imageHeight = 560,
        )

        assertEquals(GraphMultiplicityStatus.MULTI_GRAPH_VALID, resolution.multiplicityStatus)
        assertEquals(2, resolution.resolvedGraphPanels.size)
        assertTrue(resolution.rejectedDuplicatePanels.isEmpty())
    }

    @Test
    fun densePeakSubregionIsRejectedAsSameAxisSystem() {
        val resolution = resolver.resolve(
            candidates = listOf(
                panel(35, 30, 500, 285, 86f, "full panel"),
                panel(235, 45, 260, 250, 73f, "dense peak cluster"),
            ),
            imageWidth = 560,
            imageHeight = 360,
        )

        assertEquals(1, resolution.resolvedGraphPanels.size)
        val rejected = resolution.rejectedNestedPanels + resolution.rejectedSubregions + resolution.rejectedDuplicatePanels
        assertTrue(rejected.any { GraphPanelRejectionReason.SAME_AXIS_SYSTEM in it.reasons })
    }

    private fun panel(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        totalScore: Float,
        label: String,
    ): GraphPanelBounds =
        GraphPanelBounds(
            region = GraphRegion(x, y, width, height, label),
            candidateSource = GeometryCandidateSource.CV,
            confidence = totalScore / 100f,
            scoreBreakdown = RoiCandidateScoreBreakdown(total = totalScore),
        )
}

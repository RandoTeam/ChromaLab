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
    fun ticIonSemanticHintPreservesResolvedFourPanelCount() {
        val resolution = resolver.resolve(
            candidates = listOf(
                panel(40, 20, 460, 110, 82f, "tic"),
                panel(40, 155, 460, 110, 81f, "ion 71"),
                panel(40, 290, 460, 110, 80f, "ion 92"),
                panel(40, 425, 460, 110, 79f, "ion 105"),
            ),
            imageWidth = 560,
            imageHeight = 560,
            deterministicTextHints = listOf("TIC", "Ion 71", "SIM"),
        )

        assertEquals(GraphMultiplicityStatus.MULTI_GRAPH_VALID, resolution.multiplicityStatus)
        assertEquals(4, resolution.resolvedGraphPanels.size)
        assertEquals(GraphLayoutClass.TIC_PLUS_ION_PANELS, resolution.layoutClassification?.layoutClass)
        assertEquals(4, resolution.layoutClassification?.physicalGraphCount)
    }

    @Test
    fun vlmGraphCountHintCannotIncreaseDeterministicPhysicalGraphCount() {
        val resolution = resolver.resolve(
            candidates = listOf(
                panel(40, 40, 460, 180, 82f, "top graph"),
                panel(42, 300, 458, 185, 80f, "bottom graph"),
            ),
            imageWidth = 560,
            imageHeight = 560,
            vlmGraphCountHint = 5,
        )

        assertEquals(GraphMultiplicityStatus.MULTI_GRAPH_VALID, resolution.multiplicityStatus)
        assertEquals(2, resolution.resolvedGraphPanels.size)
        assertEquals(2, resolution.layoutClassification?.physicalGraphCount)
        assertTrue(resolution.warnings.any { it.startsWith("multiplicity.vlm_count_advisory_reduced") })
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

    @Test
    fun pageFallbackDoesNotSuppressDetectedPanelCandidates() {
        val resolution = resolver.resolve(
            candidates = listOf(
                panel(
                    x = 0,
                    y = 0,
                    width = 600,
                    height = 800,
                    totalScore = 96f,
                    label = "full image fallback",
                    source = GeometryCandidateSource.FULL_IMAGE_FALLBACK,
                ),
                panel(48, 210, 510, 310, 72f, "detected chart panel"),
            ),
            imageWidth = 600,
            imageHeight = 800,
        )

        assertEquals(1, resolution.resolvedGraphPanels.size)
        assertEquals("detected chart panel", resolution.resolvedGraphPanels.single().region.label)
        assertTrue("multiplicity.full_image_fallback_suppressed" in resolution.warnings)
    }

    @Test
    fun singlePhysicalGraphKeepsRejectedPanelsAsRetryAlternates() {
        val broadPageCandidate = panel(0, 324, 576, 521, 142f, "broad page")
        val calibratedPanelCandidate = panel(0, 418, 576, 425, 175f, "chart panel")
        val resolution = resolver.resolve(
            candidates = listOf(broadPageCandidate, calibratedPanelCandidate),
            imageWidth = 576,
            imageHeight = 1280,
        )

        val retryCandidates = resolution.retryGraphPanelCandidatesForSinglePhysicalGraph(
            listOf(broadPageCandidate, calibratedPanelCandidate),
        )

        assertEquals(1, resolution.layoutClassification?.physicalGraphCount)
        assertTrue(retryCandidates.any { it.region.label == "broad page" })
        assertTrue(retryCandidates.any { it.region.label == "chart panel" })
        assertEquals(resolution.resolvedGraphPanels.first().region.label, retryCandidates.first().region.label)
    }

    private fun panel(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        totalScore: Float,
        label: String,
        source: GeometryCandidateSource = GeometryCandidateSource.CV,
    ): GraphPanelBounds =
        GraphPanelBounds(
            region = GraphRegion(x, y, width, height, label),
            candidateSource = source,
            confidence = totalScore / 100f,
            scoreBreakdown = RoiCandidateScoreBreakdown(total = totalScore),
        )
}

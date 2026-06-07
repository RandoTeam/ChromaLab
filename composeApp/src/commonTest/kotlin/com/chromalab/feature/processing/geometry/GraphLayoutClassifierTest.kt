package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GraphLayoutClassifierTest {
    private val classifier = GraphLayoutClassifier()

    @Test
    fun densePeakSameAxisCandidatesCollapseToOnePhysicalGraph() {
        val panels = listOf(
            panel(0, 100, 600, 250, 140f),
            panel(0, 340, 596, 270, 130f),
        )
        val classification = classifier.classify(
            resolvedPanels = panels,
            rejectedSubregions = listOf(
                RejectedGraphPanelCandidate(
                    candidate = panels[1],
                    rejectedAgainst = panels[0],
                    reasons = listOf(GraphPanelRejectionReason.SAME_AXIS_SYSTEM),
                ),
            ),
            imageWidth = 640,
            imageHeight = 900,
        )

        assertEquals(GraphLayoutClass.DENSE_PEAK_SINGLE_AXIS, classification.layoutClass)
        assertEquals(1, classification.physicalGraphCount)
        assertTrue("layout.duplicate_or_nested_candidates_not_separate_graphs" in classification.reviewReasons)
    }

    @Test
    fun stackedTracePanelsRemainMultipleGraphUnits() {
        val panels = listOf(
            panel(20, 10, 560, 120),
            panel(20, 155, 560, 120),
            panel(20, 300, 560, 120),
            panel(20, 445, 560, 120),
        )
        val classification = classifier.classify(
            resolvedPanels = panels,
            imageWidth = 640,
            imageHeight = 700,
        )

        assertEquals(GraphLayoutClass.STACKED_TRACES_SHARED_AXIS, classification.layoutClass)
        assertEquals(4, classification.physicalGraphCount)
    }

    @Test
    fun ticIonTextHintNamesLayoutButDoesNotCreateGraphUnits() {
        val panels = listOf(
            panel(20, 10, 560, 120),
            panel(20, 155, 560, 120),
            panel(20, 300, 560, 120),
            panel(20, 445, 560, 120),
        )
        val classification = classifier.classify(
            resolvedPanels = panels,
            imageWidth = 640,
            imageHeight = 700,
            deterministicTextHints = listOf("TIC", "Ion 71", "Ion 92"),
        )
        val singlePanelClassification = classifier.classify(
            resolvedPanels = listOf(panel(20, 10, 560, 560)),
            imageWidth = 640,
            imageHeight = 700,
            deterministicTextHints = listOf("TIC", "Ion 71", "Ion 92"),
        )

        assertEquals(GraphLayoutClass.TIC_PLUS_ION_PANELS, classification.layoutClass)
        assertEquals(4, classification.physicalGraphCount)
        assertTrue("layout.tic_ion_semantic_hint" in classification.reviewReasons)
        assertEquals(1, singlePanelClassification.physicalGraphCount)
    }

    @Test
    fun twoGraphPhotoPageKeepsTwoPanels() {
        val panels = listOf(
            panel(30, 120, 540, 260),
            panel(30, 470, 540, 260),
        )
        val classification = classifier.classify(
            resolvedPanels = panels,
            imageWidth = 640,
            imageHeight = 900,
        )

        assertEquals(GraphLayoutClass.TWO_GRAPH_PAGE, classification.layoutClass)
        assertEquals(2, classification.physicalGraphCount)
    }

    private fun panel(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        score: Float = 100f,
    ): GraphPanelBounds =
        GraphPanelBounds(
            region = GraphRegion(x, y, width, height),
            candidateSource = GeometryCandidateSource.CV,
            confidence = 0.9f,
            scoreBreakdown = RoiCandidateScoreBreakdown(
                axisVisibility = 16f,
                tracePixelDensity = 8f,
                total = score,
            ),
        )
}

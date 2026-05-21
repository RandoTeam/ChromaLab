package com.chromalab.feature.processing.sweep

import com.chromalab.feature.processing.geometry.GraphMultiplicityStatus
import com.chromalab.feature.processing.graph.DetectionConfidence
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.graph.GraphRegionResult
import com.chromalab.feature.processing.pipeline.DetectionMethod
import kotlin.test.Test
import kotlin.test.assertEquals

class AutoSweepGraphSelectionTest {
    @Test
    fun multiGraphValidationStartsFromReadingOrderRegion() {
        val first = GraphRegion(20, 20, 400, 180, "top")
        val second = GraphRegion(20, 250, 400, 180, "bottom")
        val geometryWinner = second

        val selected = chooseSweepGraphRegion(
            overrideRegion = null,
            graphResult = graphResult(second, first),
            geometryRegion = geometryWinner,
            geometryMultiplicityStatus = GraphMultiplicityStatus.MULTI_GRAPH_VALID,
        )

        assertEquals(first, selected)
    }

    @Test
    fun singleGraphReviewKeepsValidatedGeometryWinner() {
        val broad = GraphRegion(0, 0, 500, 500, "broad")
        val geometryWinner = GraphRegion(40, 80, 420, 260, "validated")

        val selected = chooseSweepGraphRegion(
            overrideRegion = null,
            graphResult = graphResult(broad),
            geometryRegion = geometryWinner,
            geometryMultiplicityStatus = GraphMultiplicityStatus.SINGLE_GRAPH,
        )

        assertEquals(geometryWinner, selected)
    }

    @Test
    fun overrideRegionRemainsPrimary() {
        val override = GraphRegion(10, 10, 100, 100, "override")
        val selected = chooseSweepGraphRegion(
            overrideRegion = override,
            graphResult = graphResult(GraphRegion(0, 0, 500, 500, "detected")),
            geometryRegion = null,
            geometryMultiplicityStatus = GraphMultiplicityStatus.MULTI_GRAPH_VALID,
        )

        assertEquals(override, selected)
    }

    private fun graphResult(vararg regions: GraphRegion): GraphRegionResult =
        GraphRegionResult(
            regions = regions.toList(),
            detectionMethod = DetectionMethod.AUTO,
            confidence = DetectionConfidence.HIGH,
            imageWidth = 600,
            imageHeight = 600,
            timestamp = 1L,
        )
}

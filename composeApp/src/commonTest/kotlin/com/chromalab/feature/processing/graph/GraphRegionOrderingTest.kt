package com.chromalab.feature.processing.graph

import com.chromalab.feature.processing.pipeline.DetectionMethod
import kotlin.test.Test
import kotlin.test.assertEquals

class GraphRegionOrderingTest {

    @Test
    fun sortedRegionsOrdersRowsTopToBottomAndRegionsLeftToRight() {
        val topLeft = GraphRegion(x = 100, y = 120, width = 300, height = 180, label = "top-left")
        val topRight = GraphRegion(x = 600, y = 100, width = 300, height = 200, label = "top-right")
        val bottomLeft = GraphRegion(x = 100, y = 420, width = 300, height = 200, label = "bottom-left")
        val bottomRight = GraphRegion(x = 620, y = 430, width = 300, height = 180, label = "bottom-right")

        val result = GraphRegionResult(
            regions = listOf(bottomRight, topRight, bottomLeft, topLeft),
            detectionMethod = DetectionMethod.AUTO,
            confidence = DetectionConfidence.HIGH,
            imageWidth = 1_000,
            imageHeight = 800,
            timestamp = 0L,
        )

        assertEquals(
            listOf("top-left", "top-right", "bottom-left", "bottom-right"),
            result.sortedRegions.map { it.label },
        )
        assertEquals(
            listOf("top-left", "top-right", "bottom-left", "bottom-right"),
            result.filteredRegions.map { it.label },
        )
    }
}

package com.chromalab.feature.processing.calibration

import com.chromalab.feature.processing.graph.GraphRegion
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class ManualCalibrationCoordinateMappingTest {

    @Test
    fun xMappingConvertsFullImageViewCoordinatesToRegionRelativePixels() {
        val region = GraphRegion(x = 100, y = 50, width = 400, height = 300)
        val viewX = regionPixelXToView(
            regionPixelX = 25f,
            viewWidth = 1000f,
            sourceImageWidth = 1000,
            graphRegion = region,
        )

        val regionPixel = viewXToRegionPixel(
            viewX = viewX,
            viewWidth = 1000f,
            sourceImageWidth = 1000,
            graphRegion = region,
        )

        assertClose(25f, regionPixel)
    }

    @Test
    fun yMappingConvertsFullImageViewCoordinatesToRegionRelativePixels() {
        val region = GraphRegion(x = 100, y = 50, width = 400, height = 300)
        val viewY = regionPixelYToView(
            regionPixelY = 275f,
            viewHeight = 800f,
            sourceImageHeight = 800,
            graphRegion = region,
        )

        val regionPixel = viewYToRegionPixel(
            viewY = viewY,
            viewHeight = 800f,
            sourceImageHeight = 800,
            graphRegion = region,
        )

        assertClose(275f, regionPixel)
    }

    @Test
    fun focusedXMappingTreatsGraphRegionAsTheWholeView() {
        val region = GraphRegion(x = 100, y = 50, width = 400, height = 300)
        val viewX = regionPixelXToView(
            regionPixelX = 200f,
            viewWidth = 1000f,
            sourceImageWidth = 1000,
            graphRegion = region,
            focusGraphRegion = true,
        )

        val regionPixel = viewXToRegionPixel(
            viewX = viewX,
            viewWidth = 1000f,
            sourceImageWidth = 1000,
            graphRegion = region,
            focusGraphRegion = true,
        )

        assertClose(500f, viewX)
        assertClose(200f, regionPixel)
    }

    @Test
    fun focusedSourceMappingSubtractsGraphRegionOrigin() {
        val region = GraphRegion(x = 100, y = 50, width = 400, height = 300)

        val viewX = sourceXToView(
            sourceX = 300f,
            viewWidth = 1000f,
            sourceImageWidth = 1000,
            graphRegion = region,
            focusGraphRegion = true,
        )
        val viewY = sourceYToView(
            sourceY = 200f,
            viewHeight = 600f,
            sourceImageHeight = 800,
            graphRegion = region,
            focusGraphRegion = true,
        )

        assertClose(500f, viewX)
        assertClose(300f, viewY)
    }

    private fun assertClose(expected: Float, actual: Float) {
        assertTrue(
            abs(expected - actual) < 0.0001f,
            "Expected $actual to be close to $expected",
        )
    }
}

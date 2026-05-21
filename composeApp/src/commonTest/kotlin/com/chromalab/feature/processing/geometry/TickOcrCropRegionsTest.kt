package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion
import kotlin.test.Test
import kotlin.test.assertTrue

class TickOcrCropRegionsTest {
    @Test
    fun xTickCropsStayNearXAxisLabelBand() {
        val panel = GraphRegion(76, 244, 884, 388)
        val plot = GraphRegion(76, 244, 839, 302)
        val crops = buildTickCropRegions(
            imageWidth = 1024,
            imageHeight = 1280,
            panelRegion = panel,
            plotRegion = plot,
            tickGeometry = TickGeometry(
                xTicks = listOf(TickPixelPosition(194f)),
                yTicks = emptyList(),
            ),
        )

        val xCrop = crops.single { it.axis == GeometryAxis.X }
        assertTrue(xCrop.cropRegion.y >= plot.bottom - plot.height * 0.06f)
        assertTrue(xCrop.cropRegion.bottom <= panel.bottom + plot.height * 0.06f)
    }

    @Test
    fun yTickCropsPreserveReadableLeftLabelBand() {
        val panel = GraphRegion(16, 208, 600, 396)
        val plot = GraphRegion(16, 208, 600, 390)
        val crops = buildTickCropRegions(
            imageWidth = 768,
            imageHeight = 1024,
            panelRegion = panel,
            plotRegion = plot,
            tickGeometry = TickGeometry(
                xTicks = emptyList(),
                yTicks = listOf(TickPixelPosition(350f)),
            ),
        )

        val yCrop = crops.single { it.axis == GeometryAxis.Y }
        assertTrue(yCrop.cropRegion.width >= 48)
        assertTrue(yCrop.cropRegion.height >= 26)
        assertTrue(yCrop.cropRegion.x <= panel.x)
    }
}

package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TickOcrCropArtifactWriterTest {

    @Test
    fun writesLocalCropsAroundDeterministicTickPositions() {
        val root = Files.createTempDirectory("chromalab-tick-crops-test")
        val imagePath = root.resolve("panel.png")
        val image = BufferedImage(240, 180, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color.WHITE
        g.fillRect(0, 0, image.width, image.height)
        g.color = Color.BLACK
        g.drawRect(50, 20, 160, 110)
        g.drawString("5.00", 78, 150)
        g.drawString("100", 16, 86)
        g.dispose()
        ImageIO.write(image, "png", imagePath.toFile())
        image.flush()

        val crops = TickOcrCropArtifactWriter().writeTickCrops(
            imagePath = imagePath.toAbsolutePath().toString(),
            outputDir = root.resolve("out").toAbsolutePath().toString(),
            panelRegion = GraphRegion(0, 0, 230, 170, "panel"),
            plotRegion = GraphRegion(50, 20, 161, 111, "plot"),
            tickGeometry = TickGeometry(
                xTicks = listOf(TickPixelPosition(90f, TickDirection.DOWN, 0.9f)),
                yTicks = listOf(TickPixelPosition(82f, TickDirection.LEFT, 0.9f)),
            ),
            candidateIndex = 0,
        )

        assertEquals(2, crops.size)
        assertTrue(crops.all { it.path.endsWith(".png") })
        assertTrue(crops.all { java.io.File(it.path).isFile })
        assertTrue(crops.first { it.axis == GeometryAxis.X }.cropRegion.bottom > 130)
        assertTrue(crops.first { it.axis == GeometryAxis.Y }.cropRegion.x < 50)
    }
}

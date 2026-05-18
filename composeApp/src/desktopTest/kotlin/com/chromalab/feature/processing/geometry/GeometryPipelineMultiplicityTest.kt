package com.chromalab.feature.processing.geometry

import java.io.File
import java.awt.BasicStroke
import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import com.chromalab.feature.processing.graph.DetectionConfidence
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.graph.GraphRegionResult
import com.chromalab.feature.processing.pipeline.DetectionMethod
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GeometryPipelineMultiplicityTest {
    @Test
    fun duplicateCandidateFixtureResolvesToOnePhysicalGraph() = runBlocking {
        val imageFile = File("src/desktopTest/resources/fixtures/chromatogram_bench/bench_08_mz71_duplicate_candidate.jpg")
        val image = ImageIO.read(imageFile)
        val outputDir = kotlin.io.path.createTempDirectory("chromalab_geometry_multiplicity").toFile()

        val result = GeometryPipelineRunner().run(
            imagePath = imageFile.absolutePath,
            outputDir = outputDir.absolutePath,
            imageWidth = image.width,
            imageHeight = image.height,
            runVlmHint = false,
            runTickOcr = false,
        )
        image.flush()
        outputDir.deleteRecursively()

        val multiplicity = result.trace.multiplicityResolution
        assertNotNull(multiplicity)
        assertEquals(GraphMultiplicityStatus.SINGLE_GRAPH, multiplicity.multiplicityStatus)
        assertEquals(1, multiplicity.resolvedGraphPanels.size)
    }

    @Test
    fun rightSideSubregionDoesNotWinOverAlreadyCroppedFullChartPanel() = runBlocking {
        val image = buildAlreadyCroppedChartImage()
        val imageFile = kotlin.io.path.createTempFile("chromalab_right_side_subregion", ".png").toFile()
        ImageIO.write(image, "png", imageFile)
        val outputDir = kotlin.io.path.createTempDirectory("chromalab_right_side_subregion_out").toFile()
        val rightSideSubregion = GraphRegion(256, 36, 284, 256, "legacy right-side CV subregion")
        val cachedCv = GraphRegionResult(
            regions = listOf(rightSideSubregion),
            detectionMethod = DetectionMethod.AUTO,
            confidence = DetectionConfidence.HIGH,
            imageWidth = image.width,
            imageHeight = image.height,
            warnings = emptyList(),
            timestamp = 1L,
        )

        val result = GeometryPipelineRunner().run(
            imagePath = imageFile.absolutePath,
            outputDir = outputDir.absolutePath,
            imageWidth = image.width,
            imageHeight = image.height,
            cachedGraphResult = cachedCv,
            runVlmHint = false,
            runTickOcr = false,
        )

        val selected = result.graphPanelBounds?.region
        image.flush()
        imageFile.delete()
        outputDir.deleteRecursively()

        assertNotNull(selected, "Expected a graph panel candidate to be selected")
        assertTrue(selected.x <= image.width * 0.08f, "Selected graphPanel must include the left Y-axis/tick region: $selected")
        assertTrue(selected.width >= image.width * 0.88f, "Selected graphPanel must preserve full horizontal trace: $selected")
        assertEquals(GraphMultiplicityStatus.SINGLE_GRAPH, result.trace.multiplicityResolution?.multiplicityStatus)
    }

    private fun buildAlreadyCroppedChartImage(): BufferedImage {
        val image = BufferedImage(542, 353, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color.WHITE
        g.fillRect(0, 0, image.width, image.height)
        g.color = Color.BLACK
        g.font = g.font.deriveFont(12f)
        g.drawString("Ion 71.00 (70.70 to 71.70): BELYI TIGR_1.D\\data.ms", 165, 45)
        g.drawString("Abundance", 4, 18)
        g.drawString("Time-->", 4, 342)
        g.stroke = BasicStroke(1f)
        val left = 58
        val top = 70
        val right = 528
        val bottom = 316
        g.drawLine(left, top, left, bottom)
        g.drawLine(left, bottom, right, bottom)
        for (i in 0..8) {
            val y = bottom - i * 28
            g.drawLine(left - 4, y, left, y)
            g.drawString((i * 50000).toString(), 10, y + 4)
        }
        for (i in 0..10) {
            val x = left + i * 44
            g.drawLine(x, bottom, x, bottom + 4)
            if (i > 0) g.drawString("${i * 5}.00", x - 12, bottom + 20)
        }
        val heights = listOf(180, 240, 220, 260, 230, 210, 235, 225, 245, 205, 190, 175, 160, 130)
        heights.forEachIndexed { index, height ->
            val x = left + 35 + index * 31
            g.drawLine(x, bottom - 2, x, (bottom - height).coerceAtLeast(top + 4))
            g.drawLine(x - 1, bottom - 8, x + 2, bottom - 8)
        }
        g.dispose()
        return image
    }
}

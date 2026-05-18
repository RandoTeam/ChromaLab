package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphPlotAreaDetector
import java.awt.BasicStroke
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScreenshotEmbeddedChartDetectorTest {
    @Test
    fun findsWhiteChromatogramPanelInsideDarkPhoneScreenshot() {
        val imageFile = File("src/desktopTest/resources/fixtures/chromatogram_bench/bench_08_mz71_duplicate_candidate.jpg")
        val image = ImageIO.read(imageFile)
        val result = ScreenshotEmbeddedChartDetector().detect(
            imagePath = imageFile.absolutePath,
            imageWidth = image.width,
            imageHeight = image.height,
        )
        image.flush()

        val candidate = result.candidates.firstOrNull()
        assertNotNull(candidate, "Expected an accepted screenshot embedded chart candidate; warnings=${result.warnings}")
        val panel = candidate.graphPanel
        val imageArea = image.width.toFloat() * image.height.toFloat()
        val areaRatio = panel.area.toFloat() / imageArea

        assertTrue(areaRatio in 0.14f..0.40f, "Panel should be the embedded chart, not the full screenshot: $panel ratio=$areaRatio")
        assertTrue(panel.y > image.height * 0.25f, "Panel should not select status bar/PDF toolbar UI: $panel")
        assertTrue(panel.width > image.width * 0.75f, "Panel should preserve title/axis/tick label width: $panel")
        assertTrue(candidate.scoreBreakdown.total > 35f, "Candidate should have deterministic CV evidence: ${candidate.scoreBreakdown}")
    }

    @Test
    fun derivedPlotAreaIsInsideDetectedGraphPanel() {
        val imageFile = File("src/desktopTest/resources/fixtures/chromatogram_bench/bench_08_mz71_duplicate_candidate.jpg")
        val image = ImageIO.read(imageFile)
        val candidate = ScreenshotEmbeddedChartDetector().detect(
            imagePath = imageFile.absolutePath,
            imageWidth = image.width,
            imageHeight = image.height,
        ).candidates.first()

        val plot = GraphPlotAreaDetector().detect(
            imagePath = imageFile.absolutePath,
            panelRegion = candidate.graphPanel,
            imageWidth = image.width,
            imageHeight = image.height,
        ).plotArea
        image.flush()

        assertNotNull(plot, "PlotArea detector should derive plot area from screenshot chart panel")
        assertTrue(plot.x >= candidate.graphPanel.x)
        assertTrue(plot.y >= candidate.graphPanel.y)
        assertTrue(plot.right <= candidate.graphPanel.right)
        assertTrue(plot.bottom <= candidate.graphPanel.bottom)
    }

    @Test
    fun acceptsAlreadyCroppedWhiteChartPanel() {
        val image = buildAlreadyCroppedChartImage()
        val imageFile = kotlin.io.path.createTempFile("chromalab_already_cropped_chart", ".png").toFile()
        ImageIO.write(image, "png", imageFile)

        val result = ScreenshotEmbeddedChartDetector().detect(
            imagePath = imageFile.absolutePath,
            imageWidth = image.width,
            imageHeight = image.height,
        )
        image.flush()
        imageFile.delete()

        val candidate = result.candidates.firstOrNull {
            "screenshot_chart.already_cropped_chart_panel" in it.warnings
        }
        assertNotNull(candidate, "Already-cropped chart panel should be accepted; warnings=${result.warnings}")
        assertTrue("screenshot_chart.no_accepted_bright_panel" !in result.warnings)
        assertTrue(candidate.graphPanel.width >= image.width - 8)
        assertTrue(candidate.graphPanel.height >= image.height - 8)
        assertTrue(candidate.scoreBreakdown.total > 35f)
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

package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphPlotAreaDetector
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
}

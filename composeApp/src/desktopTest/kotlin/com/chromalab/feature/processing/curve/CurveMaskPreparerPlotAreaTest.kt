package com.chromalab.feature.processing.curve

import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.pipeline.DetectionMethod
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CurveMaskPreparerPlotAreaTest {

    @Test
    fun plotAreaOnlyPreparationCropsOutTickLabelsAndCaptions() {
        val root = Files.createTempDirectory("chromalab-plot-area-mask-test")
        val imagePath = root.resolve("panel.png")
        val image = BufferedImage(320, 190, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color.WHITE
        g.fillRect(0, 0, image.width, image.height)
        g.color = Color.BLACK
        g.font = Font("SansSerif", Font.PLAIN, 14)
        g.drawString("Ion 71.00 test title", 86, 18)
        g.drawString("0", 45, 148)
        g.drawString("10.00", 92, 168)
        g.drawString("Time -->", 120, 184)
        g.drawRect(60, 28, 220, 116)
        g.drawPolyline(
            intArrayOf(62, 90, 130, 170, 220, 278),
            intArrayOf(136, 120, 78, 126, 56, 132),
            6,
        )
        g.dispose()
        ImageIO.write(image, "png", imagePath.toFile())
        image.flush()

        val plotArea = GraphRegion(60, 28, 221, 117, "plot area")
        val result = CurveMaskPreparer().prepare(
            imagePath = imagePath.toAbsolutePath().toString(),
            graphRegion = plotArea,
            axes = AxesResult(
                xAxis = null,
                yAxis = null,
                origin = null,
                detectionMethod = DetectionMethod.AUTO,
                confidence = 0f,
                timestamp = System.currentTimeMillis(),
            ),
            outputDir = root.resolve("mask").toAbsolutePath().toString(),
        )

        assertEquals(plotArea.width, result.maskWidth)
        assertEquals(plotArea.height, result.maskHeight)
        val cropPath = assertNotNull(result.plotAreaCropPath)
        val crop = ImageIO.read(java.io.File(cropPath))
        assertNotNull(crop)
        assertEquals(plotArea.width, crop.width)
        assertEquals(plotArea.height, crop.height)
        assertTrue(result.rawPixelCount > 0, "plot area crop must still include trace pixels")
        crop.flush()
    }

    @Test
    fun peakAnnotationTextBoxesAreSuppressedBeforeCleanCurveMask() {
        val root = Files.createTempDirectory("chromalab-plot-text-suppression-test")
        val imagePath = root.resolve("plot.png")
        val image = BufferedImage(260, 140, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color.WHITE
        g.fillRect(0, 0, image.width, image.height)
        g.color = Color.BLACK
        g.drawRect(10, 10, 230, 100)
        g.drawPolyline(
            intArrayOf(12, 50, 90, 130, 180, 238),
            intArrayOf(102, 92, 58, 96, 44, 98),
            6,
        )
        g.font = Font("SansSerif", Font.PLAIN, 14)
        g.drawString("5.610", 82, 34)
        g.dispose()
        ImageIO.write(image, "png", imagePath.toFile())
        image.flush()

        val textRegion = GraphRegion(82, 20, 44, 18, "5.610 label")
        val result = CurveMaskPreparer().prepare(
            imagePath = imagePath.toAbsolutePath().toString(),
            graphRegion = GraphRegion(10, 10, 231, 101, "plot area"),
            axes = AxesResult(
                xAxis = null,
                yAxis = null,
                origin = null,
                detectionMethod = DetectionMethod.AUTO,
                confidence = 0f,
                timestamp = System.currentTimeMillis(),
            ),
            outputDir = root.resolve("mask").toAbsolutePath().toString(),
            textSuppressionRegions = listOf(
                CurveMaskTextSuppressionRegion(
                    region = textRegion,
                    classification = "PEAK_ANNOTATION",
                    source = "ML_KIT",
                    reason = "test_peak_annotation_suppression",
                ),
            ),
        )

        assertTrue(result.suppressionApplied.contains("ocr_text_boxes"))
        assertEquals(1, result.textSuppressionRegions.size)
        assertNotNull(result.textSuppressionOverlayPath)
        assertTrue(result.cleanPixelCount < result.rawPixelCount)
    }
}

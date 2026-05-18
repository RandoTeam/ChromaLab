package com.chromalab.feature.processing.calibration

import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.axis.AxisLine
import com.chromalab.feature.processing.axis.AxisOrigin
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrTextElement
import com.chromalab.feature.processing.pipeline.DetectionMethod
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AxisAutoCalibrationTest {

    private val graphRegion = GraphRegion(x = 256, y = 36, width = 284, height = 256)

    private val axes = AxesResult(
        xAxis = AxisLine(x1 = 256f, y1 = 292f, x2 = 540f, y2 = 292f),
        yAxis = AxisLine(x1 = 256f, y1 = 36f, x2 = 256f, y2 = 292f),
        origin = AxisOrigin(x = 256f, y = 292f),
        detectionMethod = DetectionMethod.AUTO,
        confidence = 0.9f,
        timestamp = 1L,
    )

    @Test
    fun xCalibrationFallsBackToAxisGeometryWhenOcrAnchorsAreOutsideGraphRegion() {
        val ocr = ocrResult(
            xValues = listOf(0f, 10f, 15f, 20f, 25f, 30f, 35f, 40f, 45f, 50f, 55f),
            yValues = listOf(0f, 100000f),
            elements = listOf(
                element("10.00", 10f, x = 10f, y = 320f),
                element("55.00", 55f, x = 80f, y = 320f),
            ),
        )

        val calibration = ocr.buildAutomaticXAxisCalibration(graphRegion, axes)
            ?: error("Expected X calibration")

        assertEquals(0f, calibration.calibration.point1.pixelPos)
        assertEquals(0f, calibration.calibration.point1.realValue)
        assertEquals(284f, calibration.calibration.point2.pixelPos)
        assertEquals(55f, calibration.calibration.point2.realValue)
        assertTrue(calibration.calibration.scale > 0f)
        assertEquals("min", calibration.unit)
    }

    @Test
    fun yCalibrationFallsBackToAxisGeometryWhenOcrAnchorsAreOutsideGraphRegion() {
        val ocr = ocrResult(
            xValues = listOf(0f, 55f),
            yValues = listOf(0f, 50000f, 100000f, 150000f, 200000f, 250000f, 300000f, 350000f, 400000f),
            elements = listOf(
                element("0", 0f, x = 10f, y = 600f),
                element("400000", 400000f, x = 10f, y = 80f),
            ),
        )

        val calibration = ocr.buildAutomaticYAxisCalibration(graphRegion, axes)
            ?: error("Expected Y calibration")

        assertEquals(256f, calibration.calibration.point1.pixelPos)
        assertEquals(0f, calibration.calibration.point1.realValue)
        assertEquals(0f, calibration.calibration.point2.pixelPos)
        assertEquals(400000f, calibration.calibration.point2.realValue)
        assertTrue(calibration.calibration.scale < 0f)
        assertEquals("counts", calibration.unit)
    }

    private fun ocrResult(
        xValues: List<Float>,
        yValues: List<Float>,
        elements: List<OcrTextElement>,
    ) = AxisOcrResult(
        rawElements = elements,
        suggestedXValues = xValues,
        suggestedYValues = yValues,
        xUnit = "min",
        yUnit = "counts",
        timestamp = 1L,
    )

    private fun element(
        text: String,
        value: Float,
        x: Float,
        y: Float,
    ) = OcrTextElement(
        text = text,
        numericValue = value,
        x = x,
        y = y,
        width = 10f,
        height = 10f,
        confidence = 0.9f,
    )
}

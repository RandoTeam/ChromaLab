package com.chromalab.feature.processing.calibration

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrTextElement
import kotlin.test.Test
import kotlin.test.assertEquals

class AxisCalibrationAnchorsTest {

    private val graphRegion = GraphRegion(x = 100, y = 50, width = 400, height = 300)

    @Test
    fun xAnchorsUseSuggestedValuesNearBottomOfGraphPanel() {
        val result = ocrResult(
            xValues = listOf(10f, 20f),
            yValues = listOf(0f, 100f),
            elements = listOf(
                element("10", 10f, x = 150f, y = 315f),
                element("20", 20f, x = 450f, y = 315f),
                element("10 title", 10f, x = 250f, y = 80f),
                element("100 y", 100f, x = 105f, y = 120f),
            ),
        )

        val anchors = result.xCalibrationAnchors(graphRegion)

        assertEquals(listOf(10f, 20f), anchors.map { it.value })
        assertEquals(listOf(155f, 455f), anchors.map { it.sourceX })
    }

    @Test
    fun yAnchorsUseSuggestedValuesNearLeftOfGraphPanel() {
        val result = ocrResult(
            xValues = listOf(10f, 20f),
            yValues = listOf(0f, 100f),
            elements = listOf(
                element("0", 0f, x = 105f, y = 320f),
                element("100", 100f, x = 105f, y = 90f),
                element("100 title", 100f, x = 280f, y = 80f),
                element("20 x", 20f, x = 450f, y = 315f),
            ),
        )

        val anchors = result.yCalibrationAnchors(graphRegion)

        assertEquals(listOf(0f, 100f), anchors.map { it.value })
        assertEquals(listOf(325f, 95f), anchors.map { it.sourceY })
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
        confidence: Float = 0.9f,
    ) = OcrTextElement(
        text = text,
        numericValue = value,
        x = x,
        y = y,
        width = 10f,
        height = 10f,
        confidence = confidence,
    )
}

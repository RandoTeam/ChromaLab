package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrStatus
import com.chromalab.feature.processing.ocr.OcrTextElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TickOcrMatcherTest {
    @Test
    fun sourceAxisKeepsYLabelFromBecomingXAnchor() {
        val result = TickOcrMatcher.toTickOcrResult(
            ocr = ocr(
                OcrTextElement(
                    text = "10000",
                    numericValue = 10000f,
                    x = 92f,
                    y = 148f,
                    width = 42f,
                    height = 12f,
                    confidence = 0.83f,
                    sourceAxis = GeometryAxis.Y.name,
                    sourceTickPixelPosition = 150f,
                    sourceCropPath = "y_150.png",
                ),
            ),
            panelRegion = GraphRegion(80, 40, 420, 260),
            ticks = TickGeometry(
                xTicks = listOf(TickPixelPosition(110f), TickPixelPosition(190f), TickPixelPosition(270f)),
                yTicks = listOf(TickPixelPosition(75f), TickPixelPosition(150f), TickPixelPosition(225f)),
            ),
            cropArtifacts = listOf(
                TickOcrCropArtifact(GeometryAxis.Y, 150f, GraphRegion(20, 140, 80, 22), "y_150.png"),
            ),
        )

        assertEquals(0, result.acceptedItems.count { it.axis == GeometryAxis.X })
        assertEquals(1, result.acceptedItems.count { it.axis == GeometryAxis.Y })
        assertEquals("y_150.png", result.acceptedItems.single().localCropPath)
    }

    @Test
    fun sourceTickWithoutDeterministicPositionStaysSemanticOnly() {
        val result = TickOcrMatcher.toTickOcrResult(
            ocr = ocr(
                OcrTextElement(
                    text = "35.00",
                    numericValue = 35f,
                    x = 208f,
                    y = 304f,
                    width = 38f,
                    height = 14f,
                    confidence = 0.78f,
                    sourceAxis = GeometryAxis.X.name,
                    sourceTickPixelPosition = 222f,
                    sourceCropPath = "x_222.png",
                ),
            ),
            panelRegion = GraphRegion(80, 40, 420, 260),
            ticks = TickGeometry(xTicks = listOf(TickPixelPosition(110f), TickPixelPosition(190f))),
        )

        assertEquals(TickOcrItemStatus.SEMANTIC_ONLY, result.items.single().status)
        assertNull(result.items.single().tickPixelPosition)
        assertEquals("tick_ocr.source_tick_without_matching_deterministic_position", result.items.single().rejectionReason)
    }

    @Test
    fun numericValueWithoutDeterministicTickPixelIsNotAccepted() {
        val result = TickOcrMatcher.toTickOcrResult(
            ocr = ocr(
                OcrTextElement(
                    text = "5.610",
                    numericValue = 5.61f,
                    x = 250f,
                    y = 150f,
                    width = 34f,
                    height = 13f,
                    confidence = 0.72f,
                ),
            ),
            panelRegion = GraphRegion(80, 40, 420, 260),
            ticks = TickGeometry(
                xTicks = listOf(TickPixelPosition(110f), TickPixelPosition(190f), TickPixelPosition(270f)),
                yTicks = listOf(TickPixelPosition(75f), TickPixelPosition(150f), TickPixelPosition(225f)),
            ),
        )

        assertEquals(emptyList(), result.acceptedItems)
    }

    private fun ocr(vararg elements: OcrTextElement): AxisOcrResult =
        AxisOcrResult(
            rawElements = elements.toList(),
            suggestedXValues = emptyList(),
            suggestedYValues = emptyList(),
            xUnit = null,
            yUnit = null,
            status = OcrStatus.NOT_AVAILABLE,
            timestamp = 1L,
        )
}

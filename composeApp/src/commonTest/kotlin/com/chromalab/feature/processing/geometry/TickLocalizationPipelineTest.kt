package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.axis.AxisLine
import com.chromalab.feature.processing.graph.GraphRegion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TickLocalizationPipelineTest {
    @Test
    fun ocrNumberWithoutTickPixelIsRejectedAsSubreason() {
        val result = TickLocalizationPipeline.evaluate(
            plotAreaBounds = plot(),
            axisGeometry = axes(),
            tickGeometry = ticks(),
            tickOcrResult = TickOcrResult(
                items = listOf(
                    TickOcrItem(
                        axis = GeometryAxis.X,
                        rawText = "5.00",
                        parsedNumericValue = 5.0,
                        status = TickOcrItemStatus.SEMANTIC_ONLY,
                        rejectionReason = "tick_ocr.numeric_value_without_deterministic_tick_position",
                    ),
                ),
            ),
            xCalibrationFit = AxisCalibrationFit(GeometryAxis.X, status = CalibrationFitStatus.INVALID),
            yCalibrationFit = AxisCalibrationFit(GeometryAxis.Y, status = CalibrationFitStatus.INVALID),
        )

        assertTrue(TickLocalizationFailureSubreason.OCR_NUMERIC_NO_TICK_PIXEL in result.subreasons)
        assertTrue(TickLocalizationFailureSubreason.INSUFFICIENT_X_ANCHORS in result.subreasons)
        assertTrue(TickLocalizationFailureSubreason.INSUFFICIENT_Y_ANCHORS in result.subreasons)
    }

    @Test
    fun nonMonotonicAnchorsAreReviewSubreason() {
        val result = TickLocalizationPipeline.evaluate(
            plotAreaBounds = plot(),
            axisGeometry = axes(),
            tickGeometry = ticks(),
            tickOcrResult = TickOcrResult(
                items = listOf(
                    accepted(GeometryAxis.X, 10f, 10.0),
                    accepted(GeometryAxis.X, 20f, 15.0),
                    accepted(GeometryAxis.X, 30f, 12.0),
                    accepted(GeometryAxis.Y, 10f, 100.0),
                    accepted(GeometryAxis.Y, 20f, 200.0),
                ),
            ),
            xCalibrationFit = AxisCalibrationFit(GeometryAxis.X, status = CalibrationFitStatus.REVIEW),
            yCalibrationFit = AxisCalibrationFit(GeometryAxis.Y, status = CalibrationFitStatus.REVIEW),
        )

        assertEquals(CalibrationFitStatus.REVIEW, result.status)
        assertTrue(TickLocalizationFailureSubreason.NON_MONOTONIC_TICK_VALUES in result.subreasons)
    }

    @Test
    fun missingLabelTextProducesNoNumericSubreason() {
        val result = TickLocalizationPipeline.evaluate(
            plotAreaBounds = plot(),
            axisGeometry = axes(),
            tickGeometry = ticks(),
            tickOcrResult = TickOcrResult(
                items = emptyList(),
                warnings = listOf("tick_crop_ocr.no_numeric_text:0"),
            ),
            xCalibrationFit = AxisCalibrationFit(GeometryAxis.X, status = CalibrationFitStatus.INVALID),
            yCalibrationFit = AxisCalibrationFit(GeometryAxis.Y, status = CalibrationFitStatus.INVALID),
        )

        assertTrue(TickLocalizationFailureSubreason.OCR_NO_NUMERIC_TEXT in result.subreasons)
    }

    private fun accepted(axis: GeometryAxis, pixel: Float, value: Double): TickOcrItem =
        TickOcrItem(
            axis = axis,
            tickPixelPosition = pixel,
            rawText = value.toString(),
            parsedNumericValue = value,
            status = TickOcrItemStatus.ACCEPTED,
        )

    private fun plot(): PlotAreaBounds =
        PlotAreaBounds(
            region = GraphRegion(10, 10, 100, 80),
            parentGraphPanelRegion = GraphRegion(0, 0, 120, 110),
            confidence = 0.9f,
        )

    private fun axes(): AxisGeometry =
        AxisGeometry(
            xAxisLinePx = AxisLine(10f, 90f, 110f, 90f),
            yAxisLinePx = AxisLine(10f, 10f, 10f, 90f),
            axisConfidence = 0.8f,
        )

    private fun ticks(): TickGeometry =
        TickGeometry(
            xTicks = listOf(TickPixelPosition(10f), TickPixelPosition(20f), TickPixelPosition(30f)),
            yTicks = listOf(TickPixelPosition(10f), TickPixelPosition(20f), TickPixelPosition(30f)),
        )
}

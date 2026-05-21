package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrElementSourceKind
import com.chromalab.feature.processing.ocr.OcrTextElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AxisScaleResolverTest {
    private val resolver = AxisScaleResolver()
    private val panel = GraphRegion(0, 0, 260, 220)
    private val plot = GraphRegion(40, 30, 180, 140)
    private val axes = AxisGeometry(axisConfidence = 0.9f)

    @Test
    fun usesOcrLabelBoxesWhenExplicitTicksAreMissing() {
        val result = resolver.resolve(
            panelRegion = panel,
            plotRegion = plot,
            axisGeometry = axes,
            tickGeometry = TickGeometry(),
            tickOcrResult = TickOcrResult(),
            axisLabelOcrResult = ocrResult(
                xLabels = listOf(label("0", 90f, 178f), label("10", 155f, 178f), label("20", 220f, 178f)),
                yLabels = listOf(label("0", 18f, 165f), label("100", 18f, 100f), label("200", 18f, 35f)),
            ),
        )

        assertEquals(CalibrationFitStatus.REVIEW, result.status)
        assertEquals(3, result.xAnchors.size)
        assertEquals(3, result.yAnchors.size)
        assertTrue(result.xAnchors.all { it.evidenceType == AxisScaleEvidenceType.OCR_LABEL_BOX })
        assertTrue(result.subreasons.contains(AxisScaleFailureSubreason.TICK_MARKS_MISSING_BUT_LABELS_AVAILABLE))
    }

    @Test
    fun rejectsVlmPositionsAsCalibrationGeometry() {
        val result = resolver.resolve(
            panelRegion = panel,
            plotRegion = plot,
            axisGeometry = axes,
            tickGeometry = TickGeometry(),
            tickOcrResult = TickOcrResult(),
            axisLabelOcrResult = ocrResult(
                xLabels = listOf(
                    label("0", 40f, 178f, sourceKind = OcrElementSourceKind.VLM_AXIS_EXTRACTION),
                    label("10", 130f, 178f, sourceKind = OcrElementSourceKind.VLM_AXIS_EXTRACTION),
                ),
                yLabels = emptyList(),
            ),
        )

        assertEquals(CalibrationFitStatus.INVALID, result.status)
        assertTrue(result.rejectedAnchors.all { it.evidenceType == AxisScaleEvidenceType.SEMANTIC_TEXT_REJECTED })
        assertTrue(result.subreasons.contains(AxisScaleFailureSubreason.INSUFFICIENT_SCALE_ANCHORS))
    }

    @Test
    fun rejectsTitleIonTextAsScaleLabel() {
        val result = resolver.resolve(
            panelRegion = panel,
            plotRegion = plot,
            axisGeometry = axes,
            tickGeometry = TickGeometry(),
            tickOcrResult = TickOcrResult(),
            axisLabelOcrResult = ocrResult(
                xLabels = listOf(label("Ion 71.00", 130f, 178f, numeric = 71f)),
                yLabels = emptyList(),
            ),
        )

        assertTrue(result.subreasons.contains(AxisScaleFailureSubreason.TITLE_ION_TEXT_REJECTED_AS_SCALE_LABEL))
        assertFalse(result.xAnchors.any { it.numericValue == 71.0 })
    }

    private fun ocrResult(
        xLabels: List<OcrTextElement>,
        yLabels: List<OcrTextElement>,
    ): AxisOcrResult =
        AxisOcrResult(
            rawElements = xLabels + yLabels,
            suggestedXValues = xLabels.mapNotNull { it.numericValue },
            suggestedYValues = yLabels.mapNotNull { it.numericValue }.sortedDescending(),
            xUnit = "min",
            yUnit = "counts",
            confidence = 0.82f,
            timestamp = 1L,
        )

    private fun label(
        text: String,
        centerX: Float,
        centerY: Float,
        numeric: Float = text.toFloat(),
        sourceKind: OcrElementSourceKind = OcrElementSourceKind.ML_KIT,
    ): OcrTextElement =
        OcrTextElement(
            text = text,
            numericValue = numeric,
            x = centerX - 6f,
            y = centerY - 5f,
            width = 12f,
            height = 10f,
            confidence = 0.82f,
            sourceKind = sourceKind,
        )
}

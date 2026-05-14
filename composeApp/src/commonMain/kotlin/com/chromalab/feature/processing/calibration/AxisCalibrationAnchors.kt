package com.chromalab.feature.processing.calibration

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrTextElement
import kotlin.math.abs

internal data class AxisCalibrationAnchor(
    val value: Float,
    val sourceX: Float,
    val sourceY: Float,
    val confidence: Float,
)

internal fun AxisOcrResult?.xCalibrationAnchors(
    graphRegion: GraphRegion,
): List<AxisCalibrationAnchor> {
    val result = this ?: return emptyList()
    val suggestedValues = result.suggestedXValues
    if (suggestedValues.size < 2) return emptyList()

    val yLowerBound = graphRegion.y + graphRegion.height * 0.45f
    return result.rawElements
        .mapNotNull { element ->
            val value = element.numericValue ?: return@mapNotNull null
            if (!suggestedValues.containsValue(value)) return@mapNotNull null

            val centerX = element.centerX
            val centerY = element.centerY
            if (centerX !in graphRegion.x.toFloat()..(graphRegion.x + graphRegion.width).toFloat()) {
                return@mapNotNull null
            }
            if (centerY < yLowerBound) return@mapNotNull null

            AxisCalibrationAnchor(
                value = value,
                sourceX = centerX,
                sourceY = centerY,
                confidence = element.confidence,
            )
        }
        .distinctByValue()
        .sortedBy { it.value }
}

internal fun AxisOcrResult?.yCalibrationAnchors(
    graphRegion: GraphRegion,
): List<AxisCalibrationAnchor> {
    val result = this ?: return emptyList()
    val suggestedValues = result.suggestedYValues
    if (suggestedValues.size < 2) return emptyList()

    val xRightBound = graphRegion.x + graphRegion.width * 0.35f
    return result.rawElements
        .mapNotNull { element ->
            val value = element.numericValue ?: return@mapNotNull null
            if (!suggestedValues.containsValue(value)) return@mapNotNull null

            val centerX = element.centerX
            val centerY = element.centerY
            if (centerX > xRightBound) return@mapNotNull null
            if (centerY !in graphRegion.y.toFloat()..(graphRegion.y + graphRegion.height).toFloat()) {
                return@mapNotNull null
            }

            AxisCalibrationAnchor(
                value = value,
                sourceX = centerX,
                sourceY = centerY,
                confidence = element.confidence,
            )
        }
        .distinctByValue()
        .sortedBy { it.value }
}

private fun List<AxisCalibrationAnchor>.distinctByValue(): List<AxisCalibrationAnchor> =
    groupBy { it.value }
        .values
        .map { anchors -> anchors.maxBy { it.confidence } }

private fun List<Float>.containsValue(value: Float): Boolean =
    any { abs(it - value) < 0.0001f }

private val OcrTextElement.centerX: Float
    get() = x + width / 2f

private val OcrTextElement.centerY: Float
    get() = y + height / 2f

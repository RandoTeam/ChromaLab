package com.chromalab.feature.processing.calibration

import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.ocr.AxisOcrResult

internal fun AxisOcrResult?.buildAutomaticXAxisCalibration(
    graphRegion: GraphRegion,
    axes: AxesResult?,
): XAxisCalibration? {
    val result = this ?: return null
    val values = result.suggestedXValues.distinct().sorted()
    if (values.size < 2) return null

    val anchors = result.xCalibrationAnchors(graphRegion)
    val calibration = if (anchors.size >= 2) {
        val first = anchors.first()
        val last = anchors.last()
        LinearCalibration(
            point1 = CalibrationPoint(first.sourceX - graphRegion.x.toFloat(), first.value),
            point2 = CalibrationPoint(last.sourceX - graphRegion.x.toFloat(), last.value),
        )
    } else {
        val leftPx = ((axes?.origin?.x ?: axes?.xAxis?.x1 ?: graphRegion.x.toFloat()) - graphRegion.x)
            .coerceIn(0f, graphRegion.width.toFloat())
        val rightPx = ((axes?.xAxis?.x2 ?: (graphRegion.x + graphRegion.width).toFloat()) - graphRegion.x)
            .coerceIn(0f, graphRegion.width.toFloat())
        if (rightPx - leftPx < graphRegion.width * MIN_AXIS_SPAN_RATIO) return null
        LinearCalibration(
            point1 = CalibrationPoint(leftPx, values.first()),
            point2 = CalibrationPoint(rightPx, values.last()),
        )
    }

    return calibration
        .takeIf { it.isValid && it.scale > 0f }
        ?.let {
            XAxisCalibration(
                calibration = it,
                unit = result.xUnit ?: "min",
                timestamp = System.currentTimeMillis(),
            )
        }
}

internal fun AxisOcrResult?.buildAutomaticYAxisCalibration(
    graphRegion: GraphRegion,
    axes: AxesResult?,
): YAxisCalibration? {
    val result = this ?: return null
    val values = result.suggestedYValues.distinct().sorted()
    if (values.size < 2) return null

    val anchors = result.yCalibrationAnchors(graphRegion)
    val calibration = if (anchors.size >= 2) {
        val bottom = anchors.first()
        val top = anchors.last()
        LinearCalibration(
            point1 = CalibrationPoint(bottom.sourceY - graphRegion.y.toFloat(), bottom.value),
            point2 = CalibrationPoint(top.sourceY - graphRegion.y.toFloat(), top.value),
        )
    } else {
        val bottomPx = ((axes?.origin?.y ?: axes?.xAxis?.y1 ?: (graphRegion.y + graphRegion.height).toFloat()) - graphRegion.y)
            .coerceIn(0f, graphRegion.height.toFloat())
        val topPx = ((axes?.yAxis?.y1 ?: graphRegion.y.toFloat()) - graphRegion.y)
            .coerceIn(0f, graphRegion.height.toFloat())
        if (bottomPx - topPx < graphRegion.height * MIN_AXIS_SPAN_RATIO) return null
        LinearCalibration(
            point1 = CalibrationPoint(bottomPx, values.first()),
            point2 = CalibrationPoint(topPx, values.last()),
        )
    }

    return calibration
        .takeIf { it.isValid && it.scale < 0f }
        ?.let {
            YAxisCalibration(
                calibration = it,
                unit = result.yUnit ?: "counts",
                timestamp = System.currentTimeMillis(),
            )
        }
}

private const val MIN_AXIS_SPAN_RATIO = 0.25f

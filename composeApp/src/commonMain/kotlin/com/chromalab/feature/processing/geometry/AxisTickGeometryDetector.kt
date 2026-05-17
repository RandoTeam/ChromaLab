package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.axis.AxisLine
import com.chromalab.feature.processing.axis.AxisOrigin
import com.chromalab.feature.processing.graph.GraphRegion

data class AxisTickGeometryResult(
    val available: Boolean,
    val source: String,
    val plotRegion: GraphRegion?,
    val xAxis: AxisLine?,
    val yAxis: AxisLine?,
    val origin: AxisOrigin?,
    val lineSegmentCount: Int,
    val horizontalLineCount: Int,
    val verticalLineCount: Int,
    val xTickPositions: List<Float>,
    val yTickPositions: List<Float>,
    val readyForOcrValueMatching: Boolean,
    val warnings: List<String> = emptyList(),
)

expect class AxisTickGeometryDetector() {
    fun detect(
        imagePath: String,
        graphIndex: Int,
        panelRegion: GraphRegion,
        plotRegion: GraphRegion?,
    ): AxisTickGeometryResult
}

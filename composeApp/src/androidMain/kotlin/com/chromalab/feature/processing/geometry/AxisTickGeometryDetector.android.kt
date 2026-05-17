package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion

actual class AxisTickGeometryDetector actual constructor() {
    actual fun detect(
        imagePath: String,
        graphIndex: Int,
        panelRegion: GraphRegion,
        plotRegion: GraphRegion?,
    ): AxisTickGeometryResult =
        AxisTickGeometryResult(
            available = false,
            source = "android_backend_not_enabled",
            plotRegion = plotRegion,
            xAxis = null,
            yAxis = null,
            origin = null,
            lineSegmentCount = 0,
            horizontalLineCount = 0,
            verticalLineCount = 0,
            xTickPositions = emptyList(),
            yTickPositions = emptyList(),
            readyForOcrValueMatching = false,
            warnings = listOf("axis_tick_geometry.android_backend_not_enabled"),
        )
}

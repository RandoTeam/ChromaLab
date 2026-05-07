package com.chromalab.feature.processing.axis

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.pipeline.DetectionMethod

actual class AxisDetector actual constructor() {
    actual fun detect(imagePath: String, graphRegion: GraphRegion): AxesResult =
        AxesResult(
            xAxis = null, yAxis = null, origin = null,
            detectionMethod = DetectionMethod.MANUAL,
            confidence = 0f,
            warnings = listOf("Desktop — укажите оси вручную"),
            timestamp = System.currentTimeMillis(),
        )
}

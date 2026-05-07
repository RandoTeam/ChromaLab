package com.chromalab.feature.processing.graph

import com.chromalab.feature.processing.pipeline.DetectionMethod

actual class GraphRegionDetector actual constructor() {
    actual fun detect(imagePath: String, imageWidth: Int, imageHeight: Int): GraphRegionResult =
        GraphRegionResult(
            regions = listOf(GraphRegion(0, 0, imageWidth, imageHeight)),
            detectionMethod = DetectionMethod.MANUAL,
            confidence = DetectionConfidence.LOW,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            warnings = listOf("Desktop — авто-определение недоступно"),
            timestamp = System.currentTimeMillis(),
        )
}

package com.chromalab.feature.processing.curve

import com.chromalab.feature.processing.axis.AxesResult
import com.chromalab.feature.processing.graph.GraphRegion

actual class CurveMaskPreparer actual constructor() {
    actual fun prepare(
        imagePath: String,
        graphRegion: GraphRegion,
        axes: AxesResult,
        outputDir: String,
    ): CurveMaskResult = CurveMaskResult(
        rawMaskPath = null,
        cleanMaskPath = null,
        graphRegion = graphRegion,
        maskWidth = 0,
        maskHeight = 0,
        rawPixelCount = 0,
        cleanPixelCount = 0,
        suppressionApplied = emptyList(),
        timestamp = System.currentTimeMillis(),
    )
}

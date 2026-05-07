package com.chromalab.feature.processing.ocr

import com.chromalab.feature.processing.graph.GraphRegion

actual class AxisOcrReader actual constructor() {
    actual suspend fun readAxisLabels(
        imagePath: String,
        graphRegion: GraphRegion,
    ): AxisOcrResult = AxisOcrResult(
        rawElements = emptyList(),
        suggestedXValues = emptyList(),
        suggestedYValues = emptyList(),
        xUnit = null,
        yUnit = null,
        status = OcrStatus.NOT_AVAILABLE,
        timestamp = System.currentTimeMillis(),
    )
}

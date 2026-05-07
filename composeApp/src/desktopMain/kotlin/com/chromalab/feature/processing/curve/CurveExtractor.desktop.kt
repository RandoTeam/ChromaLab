package com.chromalab.feature.processing.curve

actual class CurveExtractor actual constructor() {
    actual fun extract(
        maskPath: String,
        maskWidth: Int,
        maskHeight: Int,
        outputDir: String,
    ): CurveExtractionResult = CurveExtractionResult(
        points = emptyList(),
        maskImagePath = null,
        totalColumns = maskWidth,
        extractedColumns = 0,
        interpolatedColumns = 0,
        outlierCount = 0,
        warnings = listOf("Desktop — не реализовано"),
        timestamp = System.currentTimeMillis(),
    )
}

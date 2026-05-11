package com.chromalab.feature.processing.inference

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.ocr.AxisOcrReader
import com.chromalab.feature.processing.ocr.AxisOcrResult

/**
 * Desktop implementation — delegates directly to AxisOcrReader.
 * No VLM engine available on desktop.
 */
actual class ChartAnalysisReader actual constructor() {

    private val fallbackOcr = AxisOcrReader()

    actual suspend fun readAxisLabels(
        imagePath: String,
        graphRegion: GraphRegion,
    ): AxisOcrResult = fallbackOcr.readAxisLabels(imagePath, graphRegion)

    /** No VLM on desktop — always returns null. */
    actual suspend fun detectGraphRegion(
        imagePath: String,
        imageWidth: Int,
        imageHeight: Int,
    ): GraphBounds? = null

    /** No VLM on desktop — always returns null. */
    actual suspend fun detectAxisStructure(imagePath: String): AxisStructure? = null
}

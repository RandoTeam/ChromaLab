package com.chromalab.feature.processing.inference

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.geometry.TickOcrCropArtifact
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

    actual suspend fun readTickLabelCrops(crops: List<TickOcrCropArtifact>): AxisOcrResult =
        fallbackOcr.readTickLabelCrops(crops)

    /** No VLM on desktop — always returns null. */
    actual suspend fun detectGraphRegion(
        imagePath: String,
        imageWidth: Int,
        imageHeight: Int,
    ): GraphBounds? = null

    /** No VLM on desktop — always returns null. */
    actual suspend fun detectAxisStructure(imagePath: String): AxisStructure? = null

    /** No VLM on desktop — always returns false. */
    actual suspend fun ensureModelLoaded(onProgress: ((String) -> Unit)?): Boolean = false

    actual fun currentModelSnapshot(): ActiveInferenceModelSnapshot = ActiveInferenceModelSnapshot()
}

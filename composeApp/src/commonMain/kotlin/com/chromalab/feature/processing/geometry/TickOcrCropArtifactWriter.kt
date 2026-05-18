package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion

data class TickOcrCropArtifact(
    val axis: GeometryAxis,
    val tickPixelPosition: Float,
    val cropRegion: GraphRegion,
    val path: String,
)

expect class TickOcrCropArtifactWriter() {
    fun writeTickCrops(
        imagePath: String,
        outputDir: String,
        panelRegion: GraphRegion,
        plotRegion: GraphRegion,
        tickGeometry: TickGeometry,
        candidateIndex: Int,
    ): List<TickOcrCropArtifact>
}

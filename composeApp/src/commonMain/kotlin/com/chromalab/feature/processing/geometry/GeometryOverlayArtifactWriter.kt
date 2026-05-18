package com.chromalab.feature.processing.geometry

import kotlinx.serialization.Serializable

@Serializable
data class GeometryOverlayArtifactResult(
    val graphPanelOverlayPath: String? = null,
    val plotAreaOverlayPath: String? = null,
    val axisOverlayPath: String? = null,
    val tickOverlayPath: String? = null,
    val warnings: List<String> = emptyList(),
)

expect class GeometryOverlayArtifactWriter() {
    fun writeOverlays(
        imagePath: String,
        outputDir: String,
        imageWidth: Int,
        imageHeight: Int,
        candidates: List<GraphPanelBounds>,
        selectedPanel: GraphPanelBounds?,
        selectedPlotArea: PlotAreaBounds?,
        axisGeometry: AxisGeometry,
        tickGeometry: TickGeometry,
    ): GeometryOverlayArtifactResult
}

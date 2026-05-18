package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion
import kotlin.math.roundToInt

internal fun buildTickCropRegions(
    imageWidth: Int,
    imageHeight: Int,
    panelRegion: GraphRegion,
    plotRegion: GraphRegion,
    tickGeometry: TickGeometry,
): List<TickOcrCropArtifact> {
    val xCropWidth = (plotRegion.width * 0.16f).roundToInt().coerceIn(34, 140)
    val xCropTop = (plotRegion.bottom - plotRegion.height * 0.05f).roundToInt()
    val xCropBottom = (panelRegion.bottom + plotRegion.height * 0.05f).roundToInt()
    val xCropHeight = (xCropBottom - xCropTop).coerceAtLeast(24)

    val yCropLeft = (panelRegion.x - plotRegion.width * 0.03f).roundToInt()
    val yCropRight = (plotRegion.x + plotRegion.width * 0.08f).roundToInt()
    val yCropWidth = (yCropRight - yCropLeft).coerceAtLeast(34)
    val yCropHeight = (plotRegion.height * 0.10f).roundToInt().coerceIn(22, 72)

    val xCrops = tickGeometry.xTicks.mapNotNull { tick ->
        val center = tick.pixelCoordinate.roundToInt()
        val region = GraphRegion(
            x = center - xCropWidth / 2,
            y = xCropTop,
            width = xCropWidth,
            height = xCropHeight,
            label = "X tick OCR crop",
        ).clampToImage(imageWidth, imageHeight)
        region.takeIf { it.width > 4 && it.height > 4 }?.let {
            TickOcrCropArtifact(GeometryAxis.X, tick.pixelCoordinate, it, path = "")
        }
    }
    val yCrops = tickGeometry.yTicks.mapNotNull { tick ->
        val center = tick.pixelCoordinate.roundToInt()
        val region = GraphRegion(
            x = yCropLeft,
            y = center - yCropHeight / 2,
            width = yCropWidth,
            height = yCropHeight,
            label = "Y tick OCR crop",
        ).clampToImage(imageWidth, imageHeight)
        region.takeIf { it.width > 4 && it.height > 4 }?.let {
            TickOcrCropArtifact(GeometryAxis.Y, tick.pixelCoordinate, it, path = "")
        }
    }
    return xCrops + yCrops
}

private fun GraphRegion.clampToImage(imageWidth: Int, imageHeight: Int): GraphRegion {
    val safeX = x.coerceIn(0, (imageWidth - 1).coerceAtLeast(0))
    val safeY = y.coerceIn(0, (imageHeight - 1).coerceAtLeast(0))
    val safeRight = right.coerceIn(safeX + 1, imageWidth.coerceAtLeast(safeX + 1))
    val safeBottom = bottom.coerceIn(safeY + 1, imageHeight.coerceAtLeast(safeY + 1))
    return copy(x = safeX, y = safeY, width = safeRight - safeX, height = safeBottom - safeY)
}

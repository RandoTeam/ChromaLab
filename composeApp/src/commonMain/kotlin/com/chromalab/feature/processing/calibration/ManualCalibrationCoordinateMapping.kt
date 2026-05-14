package com.chromalab.feature.processing.calibration

import com.chromalab.feature.processing.graph.GraphRegion

internal fun viewXToRegionPixel(
    viewX: Float,
    viewWidth: Float,
    sourceImageWidth: Int,
    graphRegion: GraphRegion,
): Float {
    val sourceX = viewX / sourceXScale(viewWidth, sourceImageWidth)
    return sourceX - graphRegion.x
}

internal fun viewYToRegionPixel(
    viewY: Float,
    viewHeight: Float,
    sourceImageHeight: Int,
    graphRegion: GraphRegion,
): Float {
    val sourceY = viewY / sourceYScale(viewHeight, sourceImageHeight)
    return sourceY - graphRegion.y
}

internal fun regionPixelXToView(
    regionPixelX: Float,
    viewWidth: Float,
    sourceImageWidth: Int,
    graphRegion: GraphRegion,
): Float = (graphRegion.x + regionPixelX) * sourceXScale(viewWidth, sourceImageWidth)

internal fun regionPixelYToView(
    regionPixelY: Float,
    viewHeight: Float,
    sourceImageHeight: Int,
    graphRegion: GraphRegion,
): Float = (graphRegion.y + regionPixelY) * sourceYScale(viewHeight, sourceImageHeight)

internal fun sourceXToView(
    sourceX: Float,
    viewWidth: Float,
    sourceImageWidth: Int,
): Float = sourceX * sourceXScale(viewWidth, sourceImageWidth)

internal fun sourceYToView(
    sourceY: Float,
    viewHeight: Float,
    sourceImageHeight: Int,
): Float = sourceY * sourceYScale(viewHeight, sourceImageHeight)

private fun sourceXScale(viewWidth: Float, sourceImageWidth: Int): Float =
    if (sourceImageWidth > 0) viewWidth / sourceImageWidth else 1f

private fun sourceYScale(viewHeight: Float, sourceImageHeight: Int): Float =
    if (sourceImageHeight > 0) viewHeight / sourceImageHeight else 1f

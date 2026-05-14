package com.chromalab.feature.processing.calibration

import com.chromalab.feature.processing.graph.GraphRegion

internal fun viewXToRegionPixel(
    viewX: Float,
    viewWidth: Float,
    sourceImageWidth: Int,
    graphRegion: GraphRegion,
    focusGraphRegion: Boolean = false,
): Float {
    if (focusGraphRegion) {
        return viewX / regionXScale(viewWidth, graphRegion)
    }
    val sourceX = viewX / sourceXScale(viewWidth, sourceImageWidth)
    return sourceX - graphRegion.x
}

internal fun viewYToRegionPixel(
    viewY: Float,
    viewHeight: Float,
    sourceImageHeight: Int,
    graphRegion: GraphRegion,
    focusGraphRegion: Boolean = false,
): Float {
    if (focusGraphRegion) {
        return viewY / regionYScale(viewHeight, graphRegion)
    }
    val sourceY = viewY / sourceYScale(viewHeight, sourceImageHeight)
    return sourceY - graphRegion.y
}

internal fun regionPixelXToView(
    regionPixelX: Float,
    viewWidth: Float,
    sourceImageWidth: Int,
    graphRegion: GraphRegion,
    focusGraphRegion: Boolean = false,
): Float =
    if (focusGraphRegion) {
        regionPixelX * regionXScale(viewWidth, graphRegion)
    } else {
        (graphRegion.x + regionPixelX) * sourceXScale(viewWidth, sourceImageWidth)
    }

internal fun regionPixelYToView(
    regionPixelY: Float,
    viewHeight: Float,
    sourceImageHeight: Int,
    graphRegion: GraphRegion,
    focusGraphRegion: Boolean = false,
): Float =
    if (focusGraphRegion) {
        regionPixelY * regionYScale(viewHeight, graphRegion)
    } else {
        (graphRegion.y + regionPixelY) * sourceYScale(viewHeight, sourceImageHeight)
    }

internal fun sourceXToView(
    sourceX: Float,
    viewWidth: Float,
    sourceImageWidth: Int,
    graphRegion: GraphRegion? = null,
    focusGraphRegion: Boolean = false,
): Float =
    if (focusGraphRegion && graphRegion != null) {
        (sourceX - graphRegion.x) * regionXScale(viewWidth, graphRegion)
    } else {
        sourceX * sourceXScale(viewWidth, sourceImageWidth)
    }

internal fun sourceYToView(
    sourceY: Float,
    viewHeight: Float,
    sourceImageHeight: Int,
    graphRegion: GraphRegion? = null,
    focusGraphRegion: Boolean = false,
): Float =
    if (focusGraphRegion && graphRegion != null) {
        (sourceY - graphRegion.y) * regionYScale(viewHeight, graphRegion)
    } else {
        sourceY * sourceYScale(viewHeight, sourceImageHeight)
    }

internal fun focusedImageScaleX(sourceImageWidth: Int, graphRegion: GraphRegion): Float =
    sourceImageWidth.coerceAtLeast(1).toFloat() / graphRegion.width.coerceAtLeast(1).toFloat()

internal fun focusedImageScaleY(sourceImageHeight: Int, graphRegion: GraphRegion): Float =
    sourceImageHeight.coerceAtLeast(1).toFloat() / graphRegion.height.coerceAtLeast(1).toFloat()

internal fun focusedImageTranslationX(viewWidth: Float, graphRegion: GraphRegion): Float =
    -graphRegion.x * regionXScale(viewWidth, graphRegion)

internal fun focusedImageTranslationY(viewHeight: Float, graphRegion: GraphRegion): Float =
    -graphRegion.y * regionYScale(viewHeight, graphRegion)

private fun sourceXScale(viewWidth: Float, sourceImageWidth: Int): Float =
    if (sourceImageWidth > 0) viewWidth / sourceImageWidth else 1f

private fun sourceYScale(viewHeight: Float, sourceImageHeight: Int): Float =
    if (sourceImageHeight > 0) viewHeight / sourceImageHeight else 1f

private fun regionXScale(viewWidth: Float, graphRegion: GraphRegion): Float =
    if (graphRegion.width > 0) viewWidth / graphRegion.width else 1f

private fun regionYScale(viewHeight: Float, graphRegion: GraphRegion): Float =
    if (graphRegion.height > 0) viewHeight / graphRegion.height else 1f

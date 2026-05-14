package com.chromalab.feature.processing.graph

import android.graphics.BitmapFactory

internal actual class GraphRegionRefinementSampler actual constructor() {
    actual fun sample(
        imagePath: String,
        region: GraphRegion,
    ): GraphRegionRefinementSampleResult {
        val bitmap = BitmapFactory.decodeFile(imagePath)
            ?: return GraphRegionRefinementSampleResult(sample = null, warning = "graph_refine.image_not_readable")
        val clamped = region.clampTo(bitmap.width, bitmap.height)
            ?: return GraphRegionRefinementSampleResult(sample = null, warning = "graph_refine.region_out_of_image").also {
                bitmap.recycle()
            }
        val gray = IntArray(clamped.width * clamped.height)
        for (y in 0 until clamped.height) {
            for (x in 0 until clamped.width) {
                gray[y * clamped.width + x] = bitmap.getPixel(clamped.x + x, clamped.y + y).toGraphRefinementGray()
            }
        }
        bitmap.recycle()

        return GraphRegionRefinementSampleResult(
            sample = GraphRegionRefinementSample(
                width = clamped.width,
                height = clamped.height,
                gray = gray,
            ),
        )
    }
}

private fun GraphRegion.clampTo(imageWidth: Int, imageHeight: Int): GraphRegion? {
    val x1 = x.coerceIn(0, imageWidth)
    val y1 = y.coerceIn(0, imageHeight)
    val x2 = right.coerceIn(x1, imageWidth)
    val y2 = bottom.coerceIn(y1, imageHeight)
    if (x2 - x1 <= 1 || y2 - y1 <= 1) return null
    return copy(x = x1, y = y1, width = x2 - x1, height = y2 - y1)
}

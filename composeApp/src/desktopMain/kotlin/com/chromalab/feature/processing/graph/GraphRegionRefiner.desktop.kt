package com.chromalab.feature.processing.graph

import java.io.File
import javax.imageio.ImageIO

internal actual class GraphRegionRefinementSampler actual constructor() {
    actual fun sample(
        imagePath: String,
        region: GraphRegion,
    ): GraphRegionRefinementSampleResult {
        val image = ImageIO.read(File(imagePath))
            ?: return GraphRegionRefinementSampleResult(sample = null, warning = "graph_refine.image_not_readable")
        val clamped = region.clampTo(image.width, image.height)
            ?: return GraphRegionRefinementSampleResult(sample = null, warning = "graph_refine.region_out_of_image").also {
                image.flush()
            }
        val gray = IntArray(clamped.width * clamped.height)
        for (y in 0 until clamped.height) {
            for (x in 0 until clamped.width) {
                gray[y * clamped.width + x] = image.getRGB(clamped.x + x, clamped.y + y).toGraphRefinementGray()
            }
        }
        image.flush()

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

package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion
import java.io.File
import javax.imageio.ImageIO

actual class TickOcrCropArtifactWriter actual constructor() {
    actual fun writeTickCrops(
        imagePath: String,
        outputDir: String,
        panelRegion: GraphRegion,
        plotRegion: GraphRegion,
        tickGeometry: TickGeometry,
        candidateIndex: Int,
    ): List<TickOcrCropArtifact> {
        val source = ImageIO.read(File(imagePath)) ?: return emptyList()
        val dir = File(outputDir, "geometry_tick_crops/candidate_${candidateIndex + 1}").also { it.mkdirs() }
        return try {
            buildTickCropRegions(source.width, source.height, panelRegion, plotRegion, tickGeometry)
                .mapIndexedNotNull { index, crop ->
                    val path = File(dir, "${crop.axis.name.lowercase()}_${index.toString().padStart(2, '0')}.png")
                        .absolutePath
                    val image = source.getSubimage(crop.cropRegion.x, crop.cropRegion.y, crop.cropRegion.width, crop.cropRegion.height)
                    if (ImageIO.write(image, "png", File(path))) crop.copy(path = path) else null
                }
        } finally {
            source.flush()
        }
    }
}

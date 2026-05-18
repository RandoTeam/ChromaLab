package com.chromalab.feature.processing.geometry

import com.chromalab.feature.processing.graph.GraphRegion
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.io.File
import javax.imageio.ImageIO

actual class GeometryOverlayArtifactWriter actual constructor() {
    actual fun writeOverlays(
        imagePath: String,
        outputDir: String,
        imageWidth: Int,
        imageHeight: Int,
        candidates: List<GraphPanelBounds>,
        selectedPanel: GraphPanelBounds?,
        selectedPlotArea: PlotAreaBounds?,
        axisGeometry: AxisGeometry,
        tickGeometry: TickGeometry,
    ): GeometryOverlayArtifactResult {
        val source = ImageIO.read(File(imagePath))
            ?: return GeometryOverlayArtifactResult(warnings = listOf("geometry_overlay.desktop_image_not_readable"))
        val dir = File(outputDir, "geometry_overlays").also { it.mkdirs() }
        return try {
            GeometryOverlayArtifactResult(
                graphPanelOverlayPath = writeOverlay(source, File(dir, "graph_panel_overlay.png")) {
                    color = Color(255, 171, 64, 190)
                    stroke = BasicStroke(2f)
                    candidates.take(12).forEach { drawRegion(it.region) }
                    selectedPanel?.region?.let {
                        color = Color(0, 200, 83, 230)
                        stroke = BasicStroke(4f)
                        drawRegion(it)
                    }
                },
                plotAreaOverlayPath = writeOverlay(source, File(dir, "plot_area_overlay.png")) {
                    selectedPanel?.region?.let {
                        color = Color(33, 150, 243, 190)
                        stroke = BasicStroke(3f)
                        drawRegion(it)
                    }
                    selectedPlotArea?.region?.let {
                        color = Color(233, 30, 99, 230)
                        stroke = BasicStroke(4f)
                        drawRegion(it)
                    }
                },
                axisOverlayPath = writeOverlay(source, File(dir, "axis_overlay.png")) {
                    selectedPlotArea?.region?.let {
                        color = Color(233, 30, 99, 150)
                        stroke = BasicStroke(2f)
                        drawRegion(it)
                    }
                    color = Color(0, 188, 212, 230)
                    stroke = BasicStroke(4f)
                    axisGeometry.xAxisLinePx?.let { drawLine(it.x1.toInt(), it.y1.toInt(), it.x2.toInt(), it.y2.toInt()) }
                    color = Color(76, 175, 80, 230)
                    axisGeometry.yAxisLinePx?.let { drawLine(it.x1.toInt(), it.y1.toInt(), it.x2.toInt(), it.y2.toInt()) }
                },
                tickOverlayPath = writeOverlay(source, File(dir, "tick_overlay.png")) {
                    selectedPlotArea?.region?.let {
                        color = Color(233, 30, 99, 110)
                        stroke = BasicStroke(2f)
                        drawRegion(it)
                    }
                    selectedPlotArea?.region?.let { plot ->
                        color = Color(0, 188, 212, 230)
                        stroke = BasicStroke(2f)
                        tickGeometry.xTicks.forEach { tick ->
                            val x = tick.pixelCoordinate.toInt()
                            drawLine(x, plot.bottom - 8, x, plot.bottom + 8)
                        }
                        color = Color(76, 175, 80, 230)
                        tickGeometry.yTicks.forEach { tick ->
                            val y = tick.pixelCoordinate.toInt()
                            drawLine(plot.x - 8, y, plot.x + 8, y)
                        }
                    }
                },
            )
        } catch (e: Exception) {
            GeometryOverlayArtifactResult(warnings = listOf("geometry_overlay.desktop_write_failed:${e::class.simpleName}"))
        } finally {
            source.flush()
        }
    }

    private fun writeOverlay(
        source: java.awt.image.BufferedImage,
        output: File,
        draw: Graphics2D.() -> Unit,
    ): String? {
        val overlay = java.awt.image.BufferedImage(source.width, source.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val graphics = overlay.createGraphics()
        return try {
            graphics.drawImage(source, 0, 0, null)
            graphics.draw()
            if (ImageIO.write(overlay, "png", output)) output.absolutePath else null
        } finally {
            graphics.dispose()
            overlay.flush()
        }
    }

    private fun Graphics2D.drawRegion(region: GraphRegion) {
        drawRect(region.x, region.y, region.width.coerceAtLeast(1), region.height.coerceAtLeast(1))
    }
}

package com.chromalab.feature.processing.geometry

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.chromalab.feature.processing.graph.GraphRegion
import java.io.File
import java.io.FileOutputStream

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
        val source = BitmapFactory.decodeFile(imagePath)
            ?: return GeometryOverlayArtifactResult(warnings = listOf("geometry_overlay.android_image_not_readable"))
        val dir = File(outputDir, "geometry_overlays").also { it.mkdirs() }
        return try {
            GeometryOverlayArtifactResult(
                graphPanelOverlayPath = writeOverlay(source, File(dir, "graph_panel_overlay.png")) { canvas ->
                    val paint = stroke(Color.argb(190, 255, 171, 64), 2f)
                    candidates.take(12).forEach { canvas.drawRegion(it.region, paint) }
                    selectedPanel?.region?.let { canvas.drawRegion(it, stroke(Color.argb(230, 0, 200, 83), 4f)) }
                },
                plotAreaOverlayPath = writeOverlay(source, File(dir, "plot_area_overlay.png")) { canvas ->
                    selectedPanel?.region?.let { canvas.drawRegion(it, stroke(Color.argb(190, 33, 150, 243), 3f)) }
                    selectedPlotArea?.region?.let { canvas.drawRegion(it, stroke(Color.argb(230, 233, 30, 99), 4f)) }
                },
                axisOverlayPath = writeOverlay(source, File(dir, "axis_overlay.png")) { canvas ->
                    selectedPlotArea?.region?.let { canvas.drawRegion(it, stroke(Color.argb(150, 233, 30, 99), 2f)) }
                    axisGeometry.xAxisLinePx?.let {
                        canvas.drawLine(it.x1, it.y1, it.x2, it.y2, stroke(Color.argb(230, 0, 188, 212), 4f))
                    }
                    axisGeometry.yAxisLinePx?.let {
                        canvas.drawLine(it.x1, it.y1, it.x2, it.y2, stroke(Color.argb(230, 76, 175, 80), 4f))
                    }
                },
                tickOverlayPath = writeOverlay(source, File(dir, "tick_overlay.png")) { canvas ->
                    selectedPlotArea?.region?.let { plot ->
                        canvas.drawRegion(plot, stroke(Color.argb(120, 233, 30, 99), 2f))
                        val xPaint = stroke(Color.argb(230, 0, 188, 212), 2f)
                        tickGeometry.xTicks.forEach { tick ->
                            val x = tick.pixelCoordinate
                            canvas.drawLine(x, (plot.bottom - 8).toFloat(), x, (plot.bottom + 8).toFloat(), xPaint)
                        }
                        val yPaint = stroke(Color.argb(230, 76, 175, 80), 2f)
                        tickGeometry.yTicks.forEach { tick ->
                            val y = tick.pixelCoordinate
                            canvas.drawLine((plot.x - 8).toFloat(), y, (plot.x + 8).toFloat(), y, yPaint)
                        }
                    }
                },
            )
        } catch (e: Exception) {
            GeometryOverlayArtifactResult(warnings = listOf("geometry_overlay.android_write_failed:${e::class.simpleName}"))
        } finally {
            source.recycle()
        }
    }

    private fun writeOverlay(
        source: Bitmap,
        output: File,
        draw: (Canvas) -> Unit,
    ): String? {
        val overlay = source.copy(Bitmap.Config.ARGB_8888, true) ?: return null
        return try {
            draw(Canvas(overlay))
            val saved = FileOutputStream(output).use { out -> overlay.compress(Bitmap.CompressFormat.PNG, 100, out) }
            output.absolutePath.takeIf { saved }
        } finally {
            overlay.recycle()
        }
    }

    private fun stroke(color: Int, width: Float): Paint =
        Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = width
            this.color = color
        }

    private fun Canvas.drawRegion(region: GraphRegion, paint: Paint) {
        drawRect(
            region.x.toFloat(),
            region.y.toFloat(),
            region.right.toFloat(),
            region.bottom.toFloat(),
            paint,
        )
    }
}

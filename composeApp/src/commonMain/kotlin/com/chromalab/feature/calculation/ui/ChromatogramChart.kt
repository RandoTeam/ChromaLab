package com.chromalab.feature.calculation.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Interactive chromatogram chart (§2.22).
 *
 * Canvas-based Compose chart for displaying chromatogram data with:
 * - Multiple signal layers (raw, smoothed, baseline, corrected)
 * - Peak markers (apex, boundaries, integration area)
 * - Noise region shading
 * - Zoom, pan, tap-to-select interactions
 * - Downsampling for performance with large datasets
 */

// ─── Data model ─────────────────────────────────────────────────

data class ChartPoint(val time: Double, val intensity: Double)

data class ChartPeakMarker(
    val apexTime: Double,
    val apexIntensity: Double,
    val leftBoundaryTime: Double,
    val rightBoundaryTime: Double,
    val label: String = "",
    val confidenceColor: Color = Color(0xFF4CAF50),
    val isSelected: Boolean = false,
)

data class ChartNoiseRegion(
    val startTime: Double,
    val endTime: Double,
)

data class ChartLayer(
    val id: String,
    val points: List<ChartPoint>,
    val color: Color,
    val strokeWidth: Float = 2f,
    val visible: Boolean = true,
)

data class ChromatogramChartState(
    val layers: List<ChartLayer> = emptyList(),
    val peaks: List<ChartPeakMarker> = emptyList(),
    val noiseRegion: ChartNoiseRegion? = null,
    val integrationFillAlpha: Float = 0.15f,
)

// ─── Chart viewport ─────────────────────────────────────────────

data class ChartViewport(
    val timeMin: Double,
    val timeMax: Double,
    val intensityMin: Double,
    val intensityMax: Double,
) {
    val timeRange get() = timeMax - timeMin
    val intensityRange get() = intensityMax - intensityMin

    fun zoom(factor: Float, centerTime: Double, centerIntensity: Double): ChartViewport {
        val f = factor.toDouble()
        val newTimeRange = timeRange / f
        val newIntRange = intensityRange / f
        val timeFrac = if (timeRange > 0) (centerTime - timeMin) / timeRange else 0.5
        val intFrac = if (intensityRange > 0) (centerIntensity - intensityMin) / intensityRange else 0.5
        return ChartViewport(
            timeMin = centerTime - newTimeRange * timeFrac,
            timeMax = centerTime + newTimeRange * (1.0 - timeFrac),
            intensityMin = centerIntensity - newIntRange * intFrac,
            intensityMax = centerIntensity + newIntRange * (1.0 - intFrac),
        )
    }

    fun pan(deltaTime: Double, deltaIntensity: Double): ChartViewport {
        return copy(
            timeMin = timeMin + deltaTime,
            timeMax = timeMax + deltaTime,
            intensityMin = intensityMin + deltaIntensity,
            intensityMax = intensityMax + deltaIntensity,
        )
    }
}

// ─── Composable ─────────────────────────────────────────────────

@Composable
fun ChromatogramChart(
    state: ChromatogramChartState,
    modifier: Modifier = Modifier,
    onPeakTap: ((Int) -> Unit)? = null,
    axisPadding: Float = 48f,
) {
    // Compute full data bounds
    val fullViewport = remember(state.layers) {
        computeFullViewport(state.layers)
    }

    var viewport by remember(fullViewport) { mutableStateOf(fullViewport) }

    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val chartWidth = size.width - axisPadding
                    val chartHeight = size.height - axisPadding
                    if (chartWidth <= 0 || chartHeight <= 0) return@detectTransformGestures

                    // Pan
                    val dtTime = -pan.x / chartWidth * viewport.timeRange
                    val dtInt = pan.y / chartHeight * viewport.intensityRange

                    // Zoom
                    val centerTime = viewport.timeMin + (centroid.x - axisPadding) / chartWidth * viewport.timeRange
                    val centerInt = viewport.intensityMax - centroid.y / chartHeight * viewport.intensityRange

                    viewport = viewport
                        .pan(dtTime, dtInt)
                        .zoom(zoom, centerTime, centerInt)
                }
            }
            .pointerInput(state.peaks) {
                detectTapGestures { offset ->
                    if (onPeakTap == null || state.peaks.isEmpty()) return@detectTapGestures
                    val chartWidth = size.width - axisPadding
                    val chartHeight = size.height - axisPadding
                    if (chartWidth <= 0 || chartHeight <= 0) return@detectTapGestures

                    val tapTime = viewport.timeMin + (offset.x - axisPadding) / chartWidth * viewport.timeRange

                    // Find nearest peak
                    val nearest = state.peaks.withIndex().minByOrNull {
                        abs(it.value.apexTime - tapTime)
                    }
                    if (nearest != null) {
                        val peakX = axisPadding + ((nearest.value.apexTime - viewport.timeMin) / viewport.timeRange * chartWidth).toFloat()
                        if (abs(offset.x - peakX) < 30f) {
                            onPeakTap(nearest.index)
                        }
                    }
                }
            }
    ) {
        val chartLeft = axisPadding
        val chartWidth = size.width - axisPadding
        val chartHeight = size.height - axisPadding
        if (chartWidth <= 0 || chartHeight <= 0) return@Canvas

        // Draw axes
        drawAxes(chartLeft, chartWidth, chartHeight, viewport, textMeasurer)

        // Clip to chart area
        clipRect(
            left = chartLeft,
            top = 0f,
            right = size.width,
            bottom = chartHeight,
        ) {
            // Noise region
            state.noiseRegion?.let { noise ->
                drawNoiseRegion(noise, viewport, chartLeft, chartWidth, chartHeight)
            }

            // Integration areas (shaded)
            state.peaks.forEach { peak ->
                drawIntegrationArea(
                    peak, state.layers, viewport, chartLeft, chartWidth, chartHeight,
                    state.integrationFillAlpha,
                )
            }

            // Signal layers
            state.layers.filter { it.visible }.forEach { layer ->
                drawSignalLayer(layer, viewport, chartLeft, chartWidth, chartHeight)
            }

            // Peak markers
            state.peaks.forEachIndexed { _, peak ->
                drawPeakMarker(peak, viewport, chartLeft, chartWidth, chartHeight)
            }
        }
    }
}

/**
 * Reset viewport to fit all data.
 */
fun resetViewport(layers: List<ChartLayer>): ChartViewport = computeFullViewport(layers)

// ─── Drawing helpers ────────────────────────────────────────────

private fun DrawScope.drawAxes(
    chartLeft: Float,
    chartWidth: Float,
    chartHeight: Float,
    viewport: ChartViewport,
    textMeasurer: TextMeasurer,
) {
    val axisColor = Color(0xFF9E9E9E)
    val textStyle = TextStyle(fontSize = 9.sp, color = axisColor)

    // Y axis
    drawLine(axisColor, Offset(chartLeft, 0f), Offset(chartLeft, chartHeight), strokeWidth = 1f)
    // X axis
    drawLine(axisColor, Offset(chartLeft, chartHeight), Offset(chartLeft + chartWidth, chartHeight), strokeWidth = 1f)

    // X ticks (time)
    val xTicks = niceTickCount(viewport.timeMin, viewport.timeMax, 6)
    xTicks.forEach { t ->
        val x = chartLeft + ((t - viewport.timeMin) / viewport.timeRange * chartWidth).toFloat()
        if (x in chartLeft..chartLeft + chartWidth) {
            drawLine(axisColor, Offset(x, chartHeight), Offset(x, chartHeight + 4f))
            val label = "%.1f".format(t)
            val measured = textMeasurer.measure(label, textStyle)
            drawText(measured, topLeft = Offset(x - measured.size.width / 2f, chartHeight + 6f))
        }
    }

    // Y ticks (intensity)
    val yTicks = niceTickCount(viewport.intensityMin, viewport.intensityMax, 5)
    yTicks.forEach { v ->
        val y = chartHeight - ((v - viewport.intensityMin) / viewport.intensityRange * chartHeight).toFloat()
        if (y in 0f..chartHeight) {
            drawLine(axisColor, Offset(chartLeft - 4f, y), Offset(chartLeft, y))
            val label = formatIntensity(v)
            val measured = textMeasurer.measure(label, textStyle)
            drawText(measured, topLeft = Offset(chartLeft - measured.size.width - 6f, y - measured.size.height / 2f))
        }
    }
}

private fun DrawScope.drawSignalLayer(
    layer: ChartLayer,
    viewport: ChartViewport,
    chartLeft: Float,
    chartWidth: Float,
    chartHeight: Float,
) {
    val points = downsample(layer.points, viewport, chartWidth.toInt())
    if (points.size < 2) return

    val path = Path()
    var started = false

    points.forEach { p ->
        val x = chartLeft + ((p.time - viewport.timeMin) / viewport.timeRange * chartWidth).toFloat()
        val y = chartHeight - ((p.intensity - viewport.intensityMin) / viewport.intensityRange * chartHeight).toFloat()
        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path,
        color = layer.color,
        style = Stroke(width = layer.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun DrawScope.drawPeakMarker(
    peak: ChartPeakMarker,
    viewport: ChartViewport,
    chartLeft: Float,
    chartWidth: Float,
    chartHeight: Float,
) {
    val apexX = chartLeft + ((peak.apexTime - viewport.timeMin) / viewport.timeRange * chartWidth).toFloat()
    val apexY = chartHeight - ((peak.apexIntensity - viewport.intensityMin) / viewport.intensityRange * chartHeight).toFloat()
    val leftX = chartLeft + ((peak.leftBoundaryTime - viewport.timeMin) / viewport.timeRange * chartWidth).toFloat()
    val rightX = chartLeft + ((peak.rightBoundaryTime - viewport.timeMin) / viewport.timeRange * chartWidth).toFloat()

    // Boundary lines
    val boundaryColor = Color(0xFF66BB6A)
    drawLine(boundaryColor, Offset(leftX, 0f), Offset(leftX, chartHeight), strokeWidth = 1f)
    drawLine(boundaryColor, Offset(rightX, 0f), Offset(rightX, chartHeight), strokeWidth = 1f)

    // Apex marker
    val markerSize = if (peak.isSelected) 8f else 5f
    drawCircle(
        color = peak.confidenceColor,
        radius = markerSize,
        center = Offset(apexX, apexY),
    )

    // Selection ring
    if (peak.isSelected) {
        drawCircle(
            color = peak.confidenceColor,
            radius = markerSize + 3f,
            center = Offset(apexX, apexY),
            style = Stroke(width = 2f),
        )
    }
}

private fun DrawScope.drawIntegrationArea(
    peak: ChartPeakMarker,
    layers: List<ChartLayer>,
    viewport: ChartViewport,
    chartLeft: Float,
    chartWidth: Float,
    chartHeight: Float,
    alpha: Float,
) {
    // Use first visible layer for integration shading
    val layer = layers.firstOrNull { it.visible && it.id == "corrected" }
        ?: layers.firstOrNull { it.visible } ?: return

    val inRange = layer.points.filter {
        it.time >= peak.leftBoundaryTime && it.time <= peak.rightBoundaryTime
    }
    if (inRange.size < 2) return

    val path = Path()
    val baseY = chartHeight // baseline = 0 for corrected signal

    val firstX = chartLeft + ((inRange.first().time - viewport.timeMin) / viewport.timeRange * chartWidth).toFloat()
    path.moveTo(firstX, baseY)

    inRange.forEach { p ->
        val x = chartLeft + ((p.time - viewport.timeMin) / viewport.timeRange * chartWidth).toFloat()
        val y = chartHeight - ((p.intensity - viewport.intensityMin) / viewport.intensityRange * chartHeight).toFloat()
        path.lineTo(x, y)
    }

    val lastX = chartLeft + ((inRange.last().time - viewport.timeMin) / viewport.timeRange * chartWidth).toFloat()
    path.lineTo(lastX, baseY)
    path.close()

    drawPath(path, color = Color(0xFF42A5F5).copy(alpha = alpha))
}

private fun DrawScope.drawNoiseRegion(
    noise: ChartNoiseRegion,
    viewport: ChartViewport,
    chartLeft: Float,
    chartWidth: Float,
    chartHeight: Float,
) {
    val x1 = chartLeft + ((noise.startTime - viewport.timeMin) / viewport.timeRange * chartWidth).toFloat()
    val x2 = chartLeft + ((noise.endTime - viewport.timeMin) / viewport.timeRange * chartWidth).toFloat()
    val left = max(x1, chartLeft)
    val right = min(x2, chartLeft + chartWidth)
    if (right > left) {
        drawRect(
            color = Color(0xFFBDBDBD).copy(alpha = 0.12f),
            topLeft = Offset(left, 0f),
            size = Size(right - left, chartHeight),
        )
    }
}

// ─── Utilities ──────────────────────────────────────────────────

private fun computeFullViewport(layers: List<ChartLayer>): ChartViewport {
    val allPoints = layers.flatMap { it.points }
    if (allPoints.isEmpty()) {
        return ChartViewport(0.0, 1.0, 0.0, 1.0)
    }
    val tMin = allPoints.minOf { it.time }
    val tMax = allPoints.maxOf { it.time }
    val iMin = allPoints.minOf { it.intensity }
    val iMax = allPoints.maxOf { it.intensity }
    val tPad = (tMax - tMin) * 0.02
    val iPad = (iMax - iMin) * 0.05
    return ChartViewport(
        timeMin = tMin - tPad,
        timeMax = tMax + tPad,
        intensityMin = min(0.0, iMin - iPad),
        intensityMax = iMax + iPad,
    )
}

/**
 * Downsample points for display: keep at most ~2× pixel width points.
 * Uses min/max per bucket to preserve peak shape.
 */
private fun downsample(
    points: List<ChartPoint>,
    viewport: ChartViewport,
    pixelWidth: Int,
): List<ChartPoint> {
    // Filter to viewport
    val visible = points.filter { it.time >= viewport.timeMin && it.time <= viewport.timeMax }
    val targetCount = pixelWidth * 2
    if (visible.size <= targetCount) return visible

    val bucketSize = visible.size / targetCount
    val result = mutableListOf<ChartPoint>()
    for (i in visible.indices step bucketSize) {
        val end = min(i + bucketSize, visible.size)
        val bucket = visible.subList(i, end)
        // Keep both min and max intensity to preserve peaks
        val minP = bucket.minByOrNull { it.intensity } ?: continue
        val maxP = bucket.maxByOrNull { it.intensity } ?: continue
        if (minP.time < maxP.time) {
            result.add(minP)
            result.add(maxP)
        } else {
            result.add(maxP)
            result.add(minP)
        }
    }
    return result
}

private fun niceTickCount(min: Double, max: Double, targetCount: Int): List<Double> {
    val range = max - min
    if (range <= 0) return listOf(min)
    val rawStep = range / targetCount
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(rawStep)))
    val normalized = rawStep / magnitude
    val niceStep = when {
        normalized <= 1.5 -> 1.0
        normalized <= 3.0 -> 2.0
        normalized <= 7.0 -> 5.0
        else -> 10.0
    } * magnitude

    val ticks = mutableListOf<Double>()
    var tick = Math.ceil(min / niceStep) * niceStep
    while (tick <= max) {
        ticks.add(tick)
        tick += niceStep
    }
    return ticks
}

private fun formatIntensity(value: Double): String = when {
    abs(value) >= 1_000_000 -> "%.1fM".format(value / 1_000_000)
    abs(value) >= 1_000 -> "%.1fK".format(value / 1_000)
    abs(value) >= 1 -> "%.0f".format(value)
    else -> "%.2f".format(value)
}

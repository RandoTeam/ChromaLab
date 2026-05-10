package com.chromalab.feature.calculation.flow

import androidx.compose.ui.graphics.Color
import com.chromalab.feature.calculation.algorithm.ConfidenceGrade
import com.chromalab.feature.calculation.core.*
import com.chromalab.feature.calculation.ui.*
import com.chromalab.feature.processing.signal.DigitalSignal

/**
 * Maps CalculationRun data → ChromatogramChart data models.
 *
 * Converts:
 *  - DigitalSignal → ChartLayer (raw)
 *  - SignalBundle → multiple ChartLayers (raw, smoothed, baseline, corrected)
 *  - PeakResult list → ChartPeakMarker list
 */
object CalculationToChartMapper {

    // ─── Layer colors ───────────────────────────────────────────

    private val COLOR_RAW = Color(0xFF42A5F5)       // blue
    private val COLOR_SMOOTHED = Color(0xFF80DEEA)   // cyan
    private val COLOR_BASELINE = Color(0xFF9E9E9E)   // grey
    private val COLOR_CORRECTED = Color(0xFF66BB6A)  // green

    // ─── Signal → Layers ────────────────────────────────────────

    /**
     * Create a single raw layer from DigitalSignal (pre-calculation).
     */
    fun signalToLayer(signal: DigitalSignal): ChartLayer {
        return ChartLayer(
            id = "raw",
            points = signal.points.map { ChartPoint(it.time.toDouble(), it.intensity.toDouble()) },
            color = COLOR_RAW,
            strokeWidth = 2f,
            visible = true,
        )
    }

    /**
     * Create all available layers from a SignalBundle.
     *
     * @param signals from CalculationRun.signals
     * @param visibleLayers set of layer IDs to show (null = show all)
     * @return list of ChartLayers
     */
    fun signalBundleToLayers(
        signals: SignalBundle,
        visibleLayers: Set<String>? = null,
    ): List<ChartLayer> {
        val layers = mutableListOf<ChartLayer>()

        // Raw — always present
        layers.add(
            ChartLayer(
                id = "raw",
                points = signals.raw.map { ChartPoint(it.time, it.intensity) },
                color = COLOR_RAW,
                strokeWidth = 1.5f,
                visible = visibleLayers?.contains("raw") ?: true,
            )
        )

        // Smoothed
        signals.smoothed?.let { pts ->
            layers.add(
                ChartLayer(
                    id = "smoothed",
                    points = pts.map { ChartPoint(it.time, it.intensity) },
                    color = COLOR_SMOOTHED,
                    strokeWidth = 2f,
                    visible = visibleLayers?.contains("smoothed") ?: false,
                )
            )
        }

        // Baseline
        signals.baseline?.let { baselineValues ->
            if (baselineValues.size == signals.raw.size) {
                layers.add(
                    ChartLayer(
                        id = "baseline",
                        points = signals.raw.mapIndexed { i, pt ->
                            ChartPoint(pt.time, baselineValues[i])
                        },
                        color = COLOR_BASELINE,
                        strokeWidth = 1.5f,
                        visible = visibleLayers?.contains("baseline") ?: false,
                    )
                )
            }
        }

        // Baseline-corrected
        signals.baselineCorrected?.let { pts ->
            layers.add(
                ChartLayer(
                    id = "corrected",
                    points = pts.map { ChartPoint(it.time, it.intensity) },
                    color = COLOR_CORRECTED,
                    strokeWidth = 2.5f,
                    visible = visibleLayers?.contains("corrected") ?: true,
                )
            )
        }

        return layers
    }

    // ─── Peaks → Markers ────────────────────────────────────────

    /**
     * Convert PeakResult list to chart markers.
     */
    fun peaksToMarkers(
        peaks: List<PeakResult>,
        selectedPeakIndex: Int = -1,
    ): List<ChartPeakMarker> {
        return peaks.map { peak ->
            ChartPeakMarker(
                apexTime = peak.rtApex,
                apexIntensity = peak.height,
                leftBoundaryTime = peak.leftBoundaryTime,
                rightBoundaryTime = peak.rightBoundaryTime,
                label = "#${peak.peakId + 1}",
                confidenceColor = confidenceColor(peak.confidence),
                isSelected = peak.peakId == selectedPeakIndex,
            )
        }
    }

    // ─── Full chart state ───────────────────────────────────────

    /**
     * Build complete ChromatogramChartState from a CalculationRun.
     */
    fun buildChartState(
        run: CalculationRun,
        visibleLayers: Set<String>? = null,
        selectedPeakIndex: Int = -1,
    ): ChromatogramChartState {
        return ChromatogramChartState(
            layers = signalBundleToLayers(run.signals, visibleLayers),
            peaks = peaksToMarkers(run.peaks, selectedPeakIndex),
        )
    }

    /**
     * Build chart state from raw signal only (pre-calculation).
     */
    fun buildRawChartState(signal: DigitalSignal): ChromatogramChartState {
        return ChromatogramChartState(
            layers = listOf(signalToLayer(signal)),
        )
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private fun confidenceColor(grade: ConfidenceGrade): Color = when (grade) {
        ConfidenceGrade.HIGH -> Color(0xFF4CAF50)
        ConfidenceGrade.MEDIUM -> Color(0xFFFFA726)
        ConfidenceGrade.LOW -> Color(0xFFEF5350)
        ConfidenceGrade.FAILED -> Color(0xFFD32F2F)
    }
}

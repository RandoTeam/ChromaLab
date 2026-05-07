package com.chromalab.feature.processing.testing

import com.chromalab.feature.processing.curve.CurvePoint
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Synthetic chromatogram generator for testing.
 * Generates curves with known peak positions, heights, and widths.
 * All parameters are deterministic — same seed = same curve.
 */
object SyntheticChromatogramGenerator {

    /**
     * A synthetic Gaussian peak with known position and shape.
     */
    data class SyntheticPeak(
        val center: Float,  // X position (pixel)
        val height: Float,  // Peak height (pixels from baseline)
        val width: Float,   // Standard deviation (pixels)
    )

    /**
     * Generate a clean synthetic chromatogram curve.
     *
     * @param width Total width in pixels
     * @param baseline Y baseline position (pixels)
     * @param peaks List of Gaussian peaks
     * @return List of CurvePoints with known coordinates
     */
    fun generateClean(
        width: Int,
        baseline: Float,
        peaks: List<SyntheticPeak>,
    ): List<CurvePoint> {
        return (0 until width).map { x ->
            val peakContribution = peaks.sumOf { peak ->
                val dx = (x - peak.center) / peak.width
                (peak.height * exp(-0.5 * dx * dx)).toDouble()
            }.toFloat()

            // Y goes up = lower pixel value (graph convention)
            val y = baseline - peakContribution
            CurvePoint(x, y, CurvePoint.HIGH_CONFIDENCE)
        }
    }

    /**
     * Add deterministic noise to a curve.
     *
     * @param seed Seed for deterministic pseudo-random noise
     * @param amplitude Max noise amplitude in pixels
     */
    fun addNoise(
        points: List<CurvePoint>,
        seed: Int,
        amplitude: Float,
    ): List<CurvePoint> {
        return points.mapIndexed { i, p ->
            // Simple deterministic noise: sin-based hash
            val noise = amplitude * sin((i * 127 + seed * 311).toFloat() * 0.1f)
            p.copy(pixelY = p.pixelY + noise)
        }
    }

    /**
     * Simulate weak contrast: reduce peak heights.
     */
    fun weakContrast(
        points: List<CurvePoint>,
        baseline: Float,
        factor: Float = 0.3f,
    ): List<CurvePoint> {
        return points.map { p ->
            val delta = baseline - p.pixelY
            p.copy(pixelY = baseline - delta * factor)
        }
    }

    /**
     * Simulate baseline tilt (perspective/skew).
     */
    fun addTilt(
        points: List<CurvePoint>,
        slopePerPixel: Float = 0.05f,
    ): List<CurvePoint> {
        return points.map { p ->
            p.copy(pixelY = p.pixelY + p.pixelX * slopePerPixel)
        }
    }

    /**
     * Standard test chromatogram: 3 peaks of different sizes.
     */
    fun standardTestCurve(width: Int = 500, baseline: Float = 400f): List<CurvePoint> {
        val peaks = listOf(
            SyntheticPeak(center = 100f, height = 80f, width = 15f),
            SyntheticPeak(center = 250f, height = 200f, width = 20f),
            SyntheticPeak(center = 400f, height = 120f, width = 12f),
        )
        return generateClean(width, baseline, peaks)
    }

    /**
     * Complex test chromatogram: overlapping peaks, shoulder, small peak.
     */
    fun complexTestCurve(width: Int = 500, baseline: Float = 400f): List<CurvePoint> {
        val peaks = listOf(
            SyntheticPeak(center = 80f, height = 30f, width = 8f),    // Small
            SyntheticPeak(center = 200f, height = 180f, width = 25f), // Large
            SyntheticPeak(center = 230f, height = 90f, width = 15f),  // Shoulder
            SyntheticPeak(center = 350f, height = 150f, width = 18f), // Medium
            SyntheticPeak(center = 450f, height = 40f, width = 10f),  // Small
        )
        return generateClean(width, baseline, peaks)
    }
}

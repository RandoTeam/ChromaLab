package com.chromalab.feature.processing.curve

import com.chromalab.feature.processing.graph.GraphRegion
import kotlinx.serialization.Serializable

/**
 * Result of curve preparation stage.
 * Contains binary masks for the curve candidate pixels.
 */
@Serializable
data class CurveMaskResult(
    /** Path to raw binary mask (before axis/grid suppression) */
    val rawMaskPath: String?,
    /** Path to cleaned binary mask (after suppression) */
    val cleanMaskPath: String?,
    /** Graph region these masks correspond to */
    val graphRegion: GraphRegion,
    /** Width of the mask image */
    val maskWidth: Int,
    /** Height of the mask image */
    val maskHeight: Int,
    /** Multiplier from mask-local coordinates back to original graph ROI coordinates */
    val coordinateScale: Float = 1f,
    /** Number of candidate pixels in raw mask */
    val rawPixelCount: Int,
    /** Number of candidate pixels in cleaned mask */
    val cleanPixelCount: Int,
    /** Which suppression passes were applied */
    val suppressionApplied: List<String>,
    /** Non-destructive audit of internal artifact risk in the cleaned mask */
    val traceArtifactAudit: CurveTraceArtifactAudit = CurveTraceArtifactAudit(),
    val timestamp: Long,
) {
    /** Ratio of cleaned to raw — lower means more was suppressed */
    val suppressionRatio: Float
        get() = if (rawPixelCount > 0) cleanPixelCount.toFloat() / rawPixelCount else 1f
}

@Serializable
data class CurveTraceArtifactAudit(
    val available: Boolean = false,
    val artifactMaskPath: String? = null,
    val baselineRow: Int? = null,
    val artifactPixelCount: Int = 0,
    val artifactPixelRatio: Float = 0f,
    val floatingComponentCount: Int = 0,
    val floatingPixelCount: Int = 0,
    val verticalLineComponentCount: Int = 0,
    val horizontalLineComponentCount: Int = 0,
    val topBandComponentCount: Int = 0,
    val warnings: List<String> = emptyList(),
)

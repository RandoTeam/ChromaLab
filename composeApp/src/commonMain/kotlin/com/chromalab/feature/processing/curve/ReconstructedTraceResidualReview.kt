package com.chromalab.feature.processing.curve

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class ReconstructedTraceResidualReview(
    val compared: Boolean = false,
    val acceptanceGate: String = "not_available",
    val residuals: List<ReconstructedTraceResidual> = emptyList(),
    val peakTopCandidateColumnCount: Int = 0,
    val branchEdgeAmbiguityColumnCount: Int = 0,
    val baselineGapColumnCount: Int = 0,
    val frameTextArtifactColumnCount: Int = 0,
    val cropBoundaryColumnCount: Int = 0,
    val signalGuideMismatchColumnCount: Int = 0,
    val unclassifiedColumnCount: Int = 0,
) {
    val residualColumnCount: Int get() = residuals.size
}

data class ReconstructedTraceResidual(
    val x: Int,
    val signalY: Float,
    val reconstructedY: Float,
    val absDeltaPx: Float,
    val classification: ReconstructedTraceResidualClass,
)

enum class ReconstructedTraceResidualClass(val code: String) {
    PEAK_TOP_CANDIDATE("peak_top_candidate"),
    BRANCH_EDGE_AMBIGUITY("branch_edge_ambiguity"),
    BASELINE_GAP("baseline_gap"),
    FRAME_TEXT_ARTIFACT("frame_text_artifact"),
    CROP_BOUNDARY("crop_boundary"),
    SIGNAL_GUIDE_MISMATCH("signal_guide_mismatch"),
    UNCLASSIFIED("unclassified"),
}

object ReconstructedTraceResidualClassifier {
    fun classify(
        signalPoints: List<CurvePoint>,
        reconstructedByX: Map<Int, Float>,
        branchCandidateColumns: Set<Int>,
        width: Int,
        height: Int,
        largeDeltaThresholdPx: Float,
        guideMaxDistancePx: Int,
    ): ReconstructedTraceResidualReview {
        if (signalPoints.isEmpty() || reconstructedByX.isEmpty() || width <= 0 || height <= 0) {
            return ReconstructedTraceResidualReview()
        }

        val edgeX = max(4, (width * EDGE_BAND_RATIO).roundToInt())
        val edgeY = max(4, (height * EDGE_BAND_RATIO).roundToInt())
        val topBandY = max(edgeY * 2, (height * TOP_ARTIFACT_BAND_RATIO).roundToInt())
        val guideDistance = guideMaxDistancePx.takeIf { it > 0 } ?: max(largeDeltaThresholdPx.roundToInt() * 3, 12)

        val residuals = signalPoints.mapNotNull { signal ->
            val reconstructedY = reconstructedByX[signal.pixelX] ?: return@mapNotNull null
            val delta = reconstructedY - signal.pixelY
            val absDelta = abs(delta)
            if (absDelta <= largeDeltaThresholdPx) return@mapNotNull null

            ReconstructedTraceResidual(
                x = signal.pixelX,
                signalY = signal.pixelY,
                reconstructedY = reconstructedY,
                absDeltaPx = absDelta,
                classification = classifyResidual(
                    x = signal.pixelX,
                    signalY = signal.pixelY,
                    reconstructedY = reconstructedY,
                    signedDeltaPx = delta,
                    absDeltaPx = absDelta,
                    signalConfidence = signal.confidence,
                    branchCandidateColumns = branchCandidateColumns,
                    width = width,
                    height = height,
                    edgeX = edgeX,
                    edgeY = edgeY,
                    topBandY = topBandY,
                    guideDistance = guideDistance.toFloat(),
                    largeDeltaThresholdPx = largeDeltaThresholdPx,
                ),
            )
        }

        if (residuals.isEmpty()) {
            return ReconstructedTraceResidualReview(
                compared = true,
                acceptanceGate = "residual_taxonomy_clear",
            )
        }

        return ReconstructedTraceResidualReview(
            compared = true,
            acceptanceGate = acceptanceGate(residuals),
            residuals = residuals,
            peakTopCandidateColumnCount = residuals.countClass(ReconstructedTraceResidualClass.PEAK_TOP_CANDIDATE),
            branchEdgeAmbiguityColumnCount = residuals.countClass(ReconstructedTraceResidualClass.BRANCH_EDGE_AMBIGUITY),
            baselineGapColumnCount = residuals.countClass(ReconstructedTraceResidualClass.BASELINE_GAP),
            frameTextArtifactColumnCount = residuals.countClass(ReconstructedTraceResidualClass.FRAME_TEXT_ARTIFACT),
            cropBoundaryColumnCount = residuals.countClass(ReconstructedTraceResidualClass.CROP_BOUNDARY),
            signalGuideMismatchColumnCount = residuals.countClass(ReconstructedTraceResidualClass.SIGNAL_GUIDE_MISMATCH),
            unclassifiedColumnCount = residuals.countClass(ReconstructedTraceResidualClass.UNCLASSIFIED),
        )
    }

    private fun classifyResidual(
        x: Int,
        signalY: Float,
        reconstructedY: Float,
        signedDeltaPx: Float,
        absDeltaPx: Float,
        signalConfidence: Float,
        branchCandidateColumns: Set<Int>,
        width: Int,
        height: Int,
        edgeX: Int,
        edgeY: Int,
        topBandY: Int,
        guideDistance: Float,
        largeDeltaThresholdPx: Float,
    ): ReconstructedTraceResidualClass {
        val reconstructedAboveSignal = signedDeltaPx < -largeDeltaThresholdPx
        val reconstructedBelowSignal = signedDeltaPx > largeDeltaThresholdPx
        val nearCropBoundary = x <= edgeX ||
            x >= width - 1 - edgeX ||
            reconstructedY <= edgeY ||
            signalY <= edgeY ||
            reconstructedY >= height - 1 - edgeY ||
            signalY >= height - 1 - edgeY
        val nearBranch = branchCandidateColumns.hasNearResidual(x, radius = BRANCH_EDGE_RADIUS)
        val likelyTopArtifact = reconstructedAboveSignal &&
            reconstructedY <= topBandY &&
            absDeltaPx > largeDeltaThresholdPx * 2f
        val severeGuideMismatch = absDeltaPx > guideDistance

        return when {
            nearCropBoundary -> ReconstructedTraceResidualClass.CROP_BOUNDARY
            nearBranch -> ReconstructedTraceResidualClass.BRANCH_EDGE_AMBIGUITY
            likelyTopArtifact -> ReconstructedTraceResidualClass.FRAME_TEXT_ARTIFACT
            severeGuideMismatch -> ReconstructedTraceResidualClass.SIGNAL_GUIDE_MISMATCH
            reconstructedAboveSignal -> ReconstructedTraceResidualClass.PEAK_TOP_CANDIDATE
            reconstructedBelowSignal || signalConfidence <= CurvePoint.INTERPOLATED ->
                ReconstructedTraceResidualClass.BASELINE_GAP
            else -> ReconstructedTraceResidualClass.UNCLASSIFIED
        }
    }

    private fun acceptanceGate(residuals: List<ReconstructedTraceResidual>): String {
        val counts = residuals.groupingBy { it.classification }.eachCount()
        return when {
            counts.getOrDefault(ReconstructedTraceResidualClass.FRAME_TEXT_ARTIFACT, 0) > 0 ->
                "blocked_possible_frame_text_artifacts"
            counts.getOrDefault(ReconstructedTraceResidualClass.CROP_BOUNDARY, 0) > 0 ->
                "blocked_crop_boundary_residuals"
            counts.getOrDefault(ReconstructedTraceResidualClass.SIGNAL_GUIDE_MISMATCH, 0) > 0 ->
                "blocked_signal_guide_mismatch"
            counts.getOrDefault(ReconstructedTraceResidualClass.BRANCH_EDGE_AMBIGUITY, 0) > residuals.size / 2 ->
                "blocked_branch_edge_residual_review"
            else -> "requires_visual_residual_review"
        }
    }

    private fun List<ReconstructedTraceResidual>.countClass(
        classification: ReconstructedTraceResidualClass,
    ): Int = count { it.classification == classification }

    private fun Set<Int>.hasNearResidual(value: Int, radius: Int): Boolean =
        (value - radius..value + radius).any { it in this }

    private const val EDGE_BAND_RATIO = 0.02f
    private const val TOP_ARTIFACT_BAND_RATIO = 0.08f
    private const val BRANCH_EDGE_RADIUS = 2
}

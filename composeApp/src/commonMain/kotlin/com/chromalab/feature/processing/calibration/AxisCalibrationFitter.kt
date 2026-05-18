package com.chromalab.feature.processing.calibration

import com.chromalab.feature.processing.geometry.AxisCalibrationFit
import com.chromalab.feature.processing.geometry.CalibrationAnchorEvidence
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryAxis
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class AxisCalibrationFitter(
    private val minAnchorsForValid: Int = 3,
    private val minAnchorsForReview: Int = 2,
    private val strictMaxResidualPx: Double = 3.0,
    private val strictRmsePx: Double = 1.5,
    private val strictR2: Double = 0.995,
) {
    fun fit(
        axis: GeometryAxis,
        anchors: List<CalibrationAnchorEvidence>,
        axisLengthPx: Float? = null,
        geometryCleanliness: Float = 0f,
    ): AxisCalibrationFit {
        val finiteAnchors = anchors
            .filter { it.tickPixelPosition.isFinite() && it.value.isFinite() }
            .distinctBy { anchor ->
                "${anchor.tickPixelPosition.roundKey()}:${anchor.value.roundKey()}"
            }
            .sortedBy { it.tickPixelPosition }
        val rejected = anchors
            .filterNot { it in finiteAnchors }
            .map { it.copy(rejectionReason = it.rejectionReason ?: "calibration.anchor_non_finite_or_duplicate") }

        if (finiteAnchors.size < minAnchorsForReview) {
            return AxisCalibrationFit(
                axis = axis,
                acceptedAnchors = emptyList(),
                rejectedAnchors = rejected + finiteAnchors.map {
                    it.copy(rejectionReason = "calibration.not_enough_anchors")
                },
                confidence = 0f,
                status = CalibrationFitStatus.INVALID,
                warnings = listOf("calibration.${axis.name.lowercase()}.not_enough_anchors"),
            )
        }

        val initial = finiteAnchors.linearFit()
            ?: return invalidFit(axis, finiteAnchors, rejected, "calibration.fit_degenerate")
        val spanPx = axisLengthPx?.takeIf { it > 0f }?.toDouble()
            ?: (finiteAnchors.last().tickPixelPosition - finiteAnchors.first().tickPixelPosition)
                .toDouble()
                .takeIf { it > 0.0 }
            ?: 1.0
        val maxResidualThreshold = maxOf(strictMaxResidualPx, spanPx * 0.0075)
        val rmseThreshold = maxOf(strictRmsePx, spanPx * 0.0035)
        val initialMetrics = initial.metrics(finiteAnchors)
        val outlierIndexes = if (
            finiteAnchors.size > minAnchorsForValid &&
            initialMetrics.maxResidualPx > maxResidualThreshold
        ) {
            finiteAnchors.robustInlierIndexes(maxResidualThreshold)
                ?.let { inliers -> finiteAnchors.indices.filterNot { it in inliers }.toSet() }
                ?: initialMetrics.residualsPx
                    .withIndex()
                    .maxByOrNull { it.value }
                    ?.index
                    ?.let { setOf(it) }
                ?: emptySet()
        } else {
            emptySet()
        }
        val accepted = finiteAnchors.filterIndexed { index, _ -> index !in outlierIndexes }
        val outliers = finiteAnchors.filterIndexed { index, _ -> index in outlierIndexes }
            .map { it.copy(rejectionReason = "calibration.outlier_residual_px") }

        if (accepted.size < minAnchorsForReview) {
            return invalidFit(axis, accepted, rejected + outliers, "calibration.fit_rejected_too_many_anchors")
        }

        val finalFit = accepted.linearFit()
            ?: return invalidFit(axis, accepted, rejected + outliers, "calibration.fit_degenerate_after_rejection")
        val metrics = finalFit.metrics(accepted)
        val enoughAnchorsForValid = accepted.size >= minAnchorsForValid ||
            (accepted.size == 2 && geometryCleanliness >= 0.97f && metrics.maxResidualPx <= 0.25)
        val strictResiduals = metrics.maxResidualPx <= maxResidualThreshold &&
            metrics.rmsePx <= rmseThreshold &&
            metrics.r2 >= strictR2
        val status = when {
            enoughAnchorsForValid && strictResiduals -> CalibrationFitStatus.VALID
            accepted.size >= minAnchorsForReview && finalFit.slope != 0.0 -> CalibrationFitStatus.REVIEW
            else -> CalibrationFitStatus.INVALID
        }
        val confidence = when (status) {
            CalibrationFitStatus.VALID -> (
                0.72f +
                    (accepted.size.coerceAtMost(8) - minAnchorsForValid).coerceAtLeast(0) * 0.035f +
                    geometryCleanliness.coerceIn(0f, 1f) * 0.12f
                ).coerceIn(0f, 0.98f)
            CalibrationFitStatus.REVIEW -> (
                0.38f +
                    accepted.size.coerceAtMost(4) * 0.06f +
                    geometryCleanliness.coerceIn(0f, 1f) * 0.08f
                ).coerceIn(0f, 0.72f)
            CalibrationFitStatus.INVALID -> 0f
        }
        val warnings = buildList {
            if (accepted.size == 2) add("calibration.${axis.name.lowercase()}.two_anchor_review")
            if (!strictResiduals) add("calibration.${axis.name.lowercase()}.residuals_review")
            if (outliers.isNotEmpty()) add("calibration.${axis.name.lowercase()}.outliers_rejected:${outliers.size}")
            if (status == CalibrationFitStatus.INVALID) add("calibration.${axis.name.lowercase()}.invalid")
        }

        return AxisCalibrationFit(
            axis = axis,
            acceptedAnchors = accepted,
            rejectedAnchors = rejected + outliers,
            slope = finalFit.slope,
            intercept = finalFit.intercept,
            residualsPx = metrics.residualsPx,
            residualsUnit = metrics.residualsUnit,
            maxResidualPx = metrics.maxResidualPx,
            rmsePx = metrics.rmsePx,
            r2 = metrics.r2,
            confidence = confidence,
            status = status,
            warnings = warnings,
        )
    }

    private fun invalidFit(
        axis: GeometryAxis,
        accepted: List<CalibrationAnchorEvidence>,
        rejected: List<CalibrationAnchorEvidence>,
        warning: String,
    ): AxisCalibrationFit =
        AxisCalibrationFit(
            axis = axis,
            acceptedAnchors = accepted,
            rejectedAnchors = rejected,
            status = CalibrationFitStatus.INVALID,
            confidence = 0f,
            warnings = listOf(warning),
        )
}

fun AxisCalibrationFit.toLinearCalibrationOrNull(): LinearCalibration? {
    val fitSlope = slope ?: return null
    val fitIntercept = intercept ?: return null
    if (fitSlope == 0.0 || acceptedAnchors.size < 2) return null
    val first = acceptedAnchors.minBy { it.tickPixelPosition }
    val last = acceptedAnchors.maxBy { it.tickPixelPosition }
    if (first.tickPixelPosition == last.tickPixelPosition) return null
    return LinearCalibration(
        point1 = CalibrationPoint(
            pixelPos = first.tickPixelPosition,
            realValue = (fitSlope * first.tickPixelPosition + fitIntercept).toFloat(),
        ),
        point2 = CalibrationPoint(
            pixelPos = last.tickPixelPosition,
            realValue = (fitSlope * last.tickPixelPosition + fitIntercept).toFloat(),
        ),
    )
}

private data class LeastSquaresLine(
    val slope: Double,
    val intercept: Double,
)

private data class FitMetrics(
    val residualsPx: List<Double>,
    val residualsUnit: List<Double>,
    val maxResidualPx: Double,
    val rmsePx: Double,
    val r2: Double,
)

private fun List<CalibrationAnchorEvidence>.linearFit(): LeastSquaresLine? {
    if (size < 2) return null
    val n = size.toDouble()
    val meanX = sumOf { it.tickPixelPosition.toDouble() } / n
    val meanY = sumOf { it.value } / n
    val sxx = sumOf { (it.tickPixelPosition.toDouble() - meanX).pow(2.0) }
    if (sxx == 0.0) return null
    val sxy = sumOf { (it.tickPixelPosition.toDouble() - meanX) * (it.value - meanY) }
    val slope = sxy / sxx
    if (slope == 0.0 || !slope.isFinite()) return null
    val intercept = meanY - slope * meanX
    if (!intercept.isFinite()) return null
    return LeastSquaresLine(slope = slope, intercept = intercept)
}

private fun List<CalibrationAnchorEvidence>.robustInlierIndexes(maxResidualPx: Double): Set<Int>? {
    if (size < 4) return null
    var best: Set<Int>? = null
    var bestRmse = Double.POSITIVE_INFINITY
    for (first in indices) {
        for (second in first + 1 until size) {
            val line = listOf(this[first], this[second]).linearFit() ?: continue
            val metrics = line.metrics(this)
            val inliers = metrics.residualsPx
                .mapIndexedNotNull { index, residual ->
                    if (residual <= maxResidualPx) index else null
                }
                .toSet()
            if (inliers.size < 2) continue
            val inlierMetrics = line.metrics(inliers.map { this[it] })
            val currentBest = best
            val better = currentBest == null ||
                inliers.size > currentBest.size ||
                (inliers.size == currentBest.size && inlierMetrics.rmsePx < bestRmse)
            if (better) {
                best = inliers
                bestRmse = inlierMetrics.rmsePx
            }
        }
    }
    return best?.takeIf { it.size >= 2 && it.size < size }
}

private fun LeastSquaresLine.metrics(anchors: List<CalibrationAnchorEvidence>): FitMetrics {
    val residualsUnit = anchors.map { anchor ->
        anchor.value - (slope * anchor.tickPixelPosition + intercept)
    }
    val residualsPx = residualsUnit.map { abs(it / slope) }
    val maxResidualPx = residualsPx.maxOrNull() ?: 0.0
    val rmsePx = sqrt(residualsPx.map { it * it }.averageOrZero())
    val meanValue = anchors.map { it.value }.averageOrZero()
    val ssTot = anchors.sumOf { (it.value - meanValue).pow(2.0) }
    val ssRes = residualsUnit.sumOf { it * it }
    val r2 = if (ssTot == 0.0) 1.0 else (1.0 - ssRes / ssTot).coerceIn(0.0, 1.0)
    return FitMetrics(
        residualsPx = residualsPx,
        residualsUnit = residualsUnit,
        maxResidualPx = maxResidualPx,
        rmsePx = rmsePx,
        r2 = r2,
    )
}

private fun Iterable<Double>.averageOrZero(): Double {
    var count = 0
    var sum = 0.0
    forEach {
        sum += it
        count++
    }
    return if (count == 0) 0.0 else sum / count
}

private fun Float.roundKey(): String = (this * 1000f).toInt().toString()

private fun Double.roundKey(): String = (this * 1000.0).toLong().toString()

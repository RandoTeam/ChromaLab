package com.chromalab.feature.processing.guided

import com.chromalab.feature.processing.calibration.AxisCalibrationFitter
import com.chromalab.feature.processing.geometry.CalibrationAnchorEvidence
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryAxis
import com.chromalab.feature.processing.geometry.GeometryPoint
import com.chromalab.feature.processing.graph.GraphRegion
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

private const val CALIBRATION_POINT_KEY_SCALE = 1000f

@Serializable
enum class CalibrationEditorStatus {
    INCOMPLETE,
    VALID,
    REVIEW,
    INVALID,
}

@Serializable
data class CalibrationAnchorPlacementState(
    val selectedAxis: CalibrationAxis = CalibrationAxis.X,
    val selectedAnchorId: String? = null,
    val xUnitLabel: String = "min",
    val yUnitLabel: String = "a.u.",
    val source: CalibrationAnchorSource = CalibrationAnchorSource.MANUAL,
)

@Serializable
data class CalibrationAxisFitSummary(
    val axis: CalibrationAxis,
    val slope: Double? = null,
    val intercept: Double? = null,
    val residualReport: CalibrationResidualReport = CalibrationResidualReport(axis = axis),
    val status: CalibrationFitStatus = CalibrationFitStatus.INVALID,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class CalibrationEditorEvaluation(
    val xFit: CalibrationAxisFitSummary = CalibrationAxisFitSummary(CalibrationAxis.X),
    val yFit: CalibrationAxisFitSummary = CalibrationAxisFitSummary(CalibrationAxis.Y),
    val issues: List<GuidedRoiValidationIssue> = emptyList(),
    val gateStatus: GuidedWorkflowGateStatus = GuidedWorkflowGateStatus.MISSING,
    val editorStatus: CalibrationEditorStatus = CalibrationEditorStatus.INCOMPLETE,
) {
    val hasErrors: Boolean
        get() = issues.any { it.severity == GuidedRoiValidationSeverity.ERROR }

    val warningCodes: List<String>
        get() = issues
            .filter { it.severity == GuidedRoiValidationSeverity.WARNING }
            .map { it.code }

    val errorCodes: List<String>
        get() = issues
            .filter { it.severity == GuidedRoiValidationSeverity.ERROR }
            .map { it.code }

    val canConfirm: Boolean
        get() = !hasErrors &&
            xFit.status != CalibrationFitStatus.INVALID &&
            yFit.status != CalibrationFitStatus.INVALID
}

@Serializable
data class CalibrationAnchorEditorSnapshot(
    val imageWidth: Int,
    val imageHeight: Int,
    val graphPanelBounds: GraphRegion? = null,
    val plotAreaBounds: GraphRegion? = null,
    val anchors: List<ManualCalibrationAnchor> = emptyList(),
    val placement: CalibrationAnchorPlacementState = CalibrationAnchorPlacementState(),
    val overlayArtifactPath: String? = null,
) {
    val evaluation: CalibrationEditorEvaluation
        get() = GuidedCalibrationEditorReducer.evaluate(this)

    val selectedAnchor: ManualCalibrationAnchor?
        get() = anchors.firstOrNull { it.anchorId == placement.selectedAnchorId }
}

object GuidedCalibrationEditorReducer {
    private val fitter = AxisCalibrationFitter()

    fun initialSnapshot(
        imageWidth: Int,
        imageHeight: Int,
        graphPanelBounds: GraphRegion?,
        plotAreaBounds: GraphRegion?,
        anchors: List<ManualCalibrationAnchor> = emptyList(),
    ): CalibrationAnchorEditorSnapshot =
        CalibrationAnchorEditorSnapshot(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            graphPanelBounds = graphPanelBounds,
            plotAreaBounds = plotAreaBounds,
            anchors = anchors,
        )

    fun setSelectedAxis(
        snapshot: CalibrationAnchorEditorSnapshot,
        axis: CalibrationAxis,
    ): CalibrationAnchorEditorSnapshot =
        snapshot.copy(
            placement = snapshot.placement.copy(selectedAxis = axis),
        )

    fun setUnitLabels(
        snapshot: CalibrationAnchorEditorSnapshot,
        xUnitLabel: String,
        yUnitLabel: String,
    ): CalibrationAnchorEditorSnapshot =
        snapshot.copy(
            placement = snapshot.placement.copy(
                xUnitLabel = xUnitLabel.ifBlank { snapshot.placement.xUnitLabel },
                yUnitLabel = yUnitLabel.ifBlank { snapshot.placement.yUnitLabel },
            ),
            anchors = snapshot.anchors.map { anchor ->
                anchor.copy(
                    unitLabel = if (anchor.axis == CalibrationAxis.X) {
                        xUnitLabel.ifBlank { snapshot.placement.xUnitLabel }
                    } else {
                        yUnitLabel.ifBlank { snapshot.placement.yUnitLabel }
                    },
                )
            },
        )

    fun addAnchor(
        snapshot: CalibrationAnchorEditorSnapshot,
        pixel: GeometryPoint,
        userProvenance: GuidedUserProvenance,
        relatedImageId: String,
        timestampEpochMillis: Long,
        value: Double = Double.NaN,
    ): CalibrationAnchorEditorSnapshot {
        val plotArea = snapshot.plotAreaBounds ?: return snapshot
        val clampedPixel = pixel.coerceInside(plotArea)
        val axis = snapshot.placement.selectedAxis
        val anchor = ManualCalibrationAnchor(
            anchorId = "cal_${timestampEpochMillis}_${axis.name}_${snapshot.anchors.size + 1}",
            axis = axis,
            pixel = clampedPixel,
            value = value,
            unitLabel = if (axis == CalibrationAxis.X) snapshot.placement.xUnitLabel else snapshot.placement.yUnitLabel,
            source = CalibrationAnchorSource.USER_CLICK,
            status = CalibrationAnchorStatus.CANDIDATE,
            timestampEpochMillis = timestampEpochMillis,
            userProvenance = userProvenance,
            relatedImageId = relatedImageId,
        )
        return snapshot.copy(
            anchors = snapshot.anchors + anchor,
            placement = snapshot.placement.copy(selectedAnchorId = anchor.anchorId),
        )
    }

    fun moveAnchor(
        snapshot: CalibrationAnchorEditorSnapshot,
        anchorId: String,
        pixel: GeometryPoint,
    ): CalibrationAnchorEditorSnapshot {
        val plotArea = snapshot.plotAreaBounds ?: return snapshot
        val clampedPixel = pixel.coerceInside(plotArea)
        return snapshot.copy(
            anchors = snapshot.anchors.map { anchor ->
                if (anchor.anchorId == anchorId) {
                    anchor.copy(pixel = clampedPixel)
                } else {
                    anchor
                }
            },
            placement = snapshot.placement.copy(selectedAnchorId = anchorId),
        )
    }

    fun removeAnchor(
        snapshot: CalibrationAnchorEditorSnapshot,
        anchorId: String,
    ): CalibrationAnchorEditorSnapshot =
        snapshot.copy(
            anchors = snapshot.anchors.filterNot { it.anchorId == anchorId },
            placement = snapshot.placement.copy(
                selectedAnchorId = snapshot.placement.selectedAnchorId.takeIf { it != anchorId },
            ),
        )

    fun setAnchorValue(
        snapshot: CalibrationAnchorEditorSnapshot,
        anchorId: String,
        value: Double,
    ): CalibrationAnchorEditorSnapshot =
        snapshot.copy(
            anchors = snapshot.anchors.map { anchor ->
                if (anchor.anchorId == anchorId) {
                    anchor.copy(
                        value = value,
                        source = if (anchor.source == CalibrationAnchorSource.USER_CLICK) {
                            CalibrationAnchorSource.USER_NUMERIC_ENTRY
                        } else {
                            anchor.source
                        },
                    )
                } else {
                    anchor
                }
            },
            placement = snapshot.placement.copy(selectedAnchorId = anchorId),
        )

    fun setAnchorAxis(
        snapshot: CalibrationAnchorEditorSnapshot,
        anchorId: String,
        axis: CalibrationAxis,
    ): CalibrationAnchorEditorSnapshot =
        snapshot.copy(
            anchors = snapshot.anchors.map { anchor ->
                if (anchor.anchorId == anchorId) {
                    anchor.copy(
                        axis = axis,
                        unitLabel = if (axis == CalibrationAxis.X) {
                            snapshot.placement.xUnitLabel
                        } else {
                            snapshot.placement.yUnitLabel
                        },
                        source = CalibrationAnchorSource.USER_EDITED_AUTO_SUGGESTION,
                    )
                } else {
                    anchor
                }
            },
            placement = snapshot.placement.copy(
                selectedAnchorId = anchorId,
                selectedAxis = axis,
            ),
        )

    fun resetAnchors(snapshot: CalibrationAnchorEditorSnapshot): CalibrationAnchorEditorSnapshot =
        snapshot.copy(
            anchors = emptyList(),
            placement = snapshot.placement.copy(selectedAnchorId = null),
        )

    fun evaluate(snapshot: CalibrationAnchorEditorSnapshot): CalibrationEditorEvaluation {
        val issues = mutableListOf<GuidedRoiValidationIssue>()
        val plotArea = snapshot.plotAreaBounds
        if (plotArea == null) {
            issues += error("calibration.plot_area_missing", "Calibration requires a confirmed plot area.")
        }
        val graphPanel = snapshot.graphPanelBounds
        if (graphPanel == null) {
            issues += error("calibration.graph_panel_missing", "Calibration requires a confirmed graph panel.")
        }

        val xAnchors = snapshot.anchors.filter { it.axis == CalibrationAxis.X }
        val yAnchors = snapshot.anchors.filter { it.axis == CalibrationAxis.Y }

        if (xAnchors.size < UserCalibrationSet.MIN_ANCHORS_PER_AXIS) {
            issues += error("calibration.x.not_enough_anchors", "At least two X calibration anchors are required.")
        }
        if (yAnchors.size < UserCalibrationSet.MIN_ANCHORS_PER_AXIS) {
            issues += error("calibration.y.not_enough_anchors", "At least two Y calibration anchors are required.")
        }

        snapshot.anchors
            .filterNot { it.value.isFinite() }
            .forEach { anchor ->
                issues += error("calibration.${anchor.axis.name.lowercase()}.value_non_finite", "Calibration anchor value must be numeric.")
            }

        issues += duplicatePixelIssues(CalibrationAxis.X, xAnchors)
        issues += duplicatePixelIssues(CalibrationAxis.Y, yAnchors)
        issues += monotonicityIssues(CalibrationAxis.X, xAnchors)
        issues += monotonicityIssues(CalibrationAxis.Y, yAnchors)

        val xFit = fitAxis(CalibrationAxis.X, xAnchors, plotArea)
        val yFit = fitAxis(CalibrationAxis.Y, yAnchors, plotArea)
        issues += xFit.warnings.map { warning(CalibrationAxis.X, it) }
        issues += yFit.warnings.map { warning(CalibrationAxis.Y, it) }

        if (xFit.status == CalibrationFitStatus.INVALID && xAnchors.size >= 2) {
            issues += error("calibration.x.fit_invalid", "X calibration fit is invalid.")
        }
        if (yFit.status == CalibrationFitStatus.INVALID && yAnchors.size >= 2) {
            issues += error("calibration.y.fit_invalid", "Y calibration fit is invalid.")
        }

        val hasErrors = issues.any { it.severity == GuidedRoiValidationSeverity.ERROR }
        val hasWarnings = issues.any { it.severity == GuidedRoiValidationSeverity.WARNING }
        val gateStatus = when {
            hasErrors -> GuidedWorkflowGateStatus.INVALID
            xFit.status == CalibrationFitStatus.VALID && yFit.status == CalibrationFitStatus.VALID && !hasWarnings ->
                GuidedWorkflowGateStatus.USER_CONFIRMED
            xFit.status != CalibrationFitStatus.INVALID && yFit.status != CalibrationFitStatus.INVALID ->
                GuidedWorkflowGateStatus.REVIEW_REQUIRED
            else -> GuidedWorkflowGateStatus.INVALID
        }
        val editorStatus = when (gateStatus) {
            GuidedWorkflowGateStatus.USER_CONFIRMED -> CalibrationEditorStatus.VALID
            GuidedWorkflowGateStatus.REVIEW_REQUIRED -> CalibrationEditorStatus.REVIEW
            GuidedWorkflowGateStatus.INVALID -> CalibrationEditorStatus.INVALID
            else -> CalibrationEditorStatus.INCOMPLETE
        }

        return CalibrationEditorEvaluation(
            xFit = xFit,
            yFit = yFit,
            issues = issues.distinctBy { it.code },
            gateStatus = gateStatus,
            editorStatus = editorStatus,
        )
    }

    fun confirmCalibration(
        state: GuidedDigitizationState,
        snapshot: CalibrationAnchorEditorSnapshot,
        userProvenance: GuidedUserProvenance,
        timestampEpochMillis: Long,
        overlayArtifactPath: String? = snapshot.overlayArtifactPath,
    ): GuidedDigitizationState {
        val plotAreaConfirmation = state.plotAreaConfirmation?.confirmedPlotArea
        require(plotAreaConfirmation?.confirmationStatus == UserConfirmationStatus.CONFIRMED) {
            "Calibration confirmation requires confirmed plotArea."
        }
        val image = requireNotNull(state.image) {
            "Calibration confirmation requires guided image reference."
        }
        val evaluation = evaluate(snapshot)
        require(evaluation.canConfirm) {
            "Calibration cannot be confirmed: ${(evaluation.errorCodes + evaluation.warningCodes).joinToString(",")}"
        }

        val xAcceptedIds = evaluation.xFit.residualReport.acceptedAnchorIds.toSet()
        val yAcceptedIds = evaluation.yFit.residualReport.acceptedAnchorIds.toSet()
        val rejectedIds = evaluation.xFit.residualReport.rejectedAnchorIds.toSet() +
            evaluation.yFit.residualReport.rejectedAnchorIds.toSet()
        val acceptedIds = xAcceptedIds + yAcceptedIds
        val anchors = snapshot.anchors.map { anchor ->
            anchor.copy(
                status = when {
                    anchor.anchorId in acceptedIds -> CalibrationAnchorStatus.ACCEPTED
                    anchor.anchorId in rejectedIds -> CalibrationAnchorStatus.OUTLIER
                    !anchor.value.isFinite() -> CalibrationAnchorStatus.REJECTED
                    else -> anchor.status
                },
                unitLabel = if (anchor.axis == CalibrationAxis.X) {
                    snapshot.placement.xUnitLabel
                } else {
                    snapshot.placement.yUnitLabel
                },
            )
        }
        val calibrationSet = UserCalibrationSet(
            calibrationSetId = "calibration_$timestampEpochMillis",
            anchors = anchors,
            source = snapshot.placement.source,
            xUnitLabel = snapshot.placement.xUnitLabel,
            yUnitLabel = snapshot.placement.yUnitLabel,
            residualReports = listOf(
                evaluation.xFit.residualReport,
                evaluation.yFit.residualReport,
            ),
            timestampEpochMillis = timestampEpochMillis,
            userProvenance = userProvenance,
            gateStatus = evaluation.gateStatus,
            warnings = evaluation.warningCodes,
        )

        return state.copy(
            currentStep = GuidedWorkflowStep.CALIBRATION_VALIDATED,
            stepStatuses = state.stepStatuses + mapOf(
                GuidedWorkflowStep.AXIS_TICKS_SUGGESTED to GuidedStepStatus.SKIPPED,
                GuidedWorkflowStep.CALIBRATION_POINTS_CONFIRMED to if (evaluation.gateStatus == GuidedWorkflowGateStatus.REVIEW_REQUIRED) {
                    GuidedStepStatus.REVIEW_REQUIRED
                } else {
                    GuidedStepStatus.CONFIRMED
                },
                GuidedWorkflowStep.CALIBRATION_VALIDATED to if (evaluation.gateStatus == GuidedWorkflowGateStatus.REVIEW_REQUIRED) {
                    GuidedStepStatus.REVIEW_REQUIRED
                } else {
                    GuidedStepStatus.VALIDATED
                },
            ),
            calibration = UserConfirmedCalibration(
                calibrationSet = calibrationSet,
                source = snapshot.placement.source,
                confirmationStatus = UserConfirmationStatus.CONFIRMED,
                timestampEpochMillis = timestampEpochMillis,
                userProvenance = userProvenance,
                overlayArtifactPath = overlayArtifactPath,
                validationWarnings = evaluation.warningCodes,
                gateStatus = evaluation.gateStatus,
            ),
            updatedAtEpochMillis = timestampEpochMillis,
            auditTrail = state.auditTrail + GuidedWorkflowAuditEntry(
                timestampEpochMillis = timestampEpochMillis,
                step = GuidedWorkflowStep.CALIBRATION_VALIDATED,
                action = "calibration_confirmed:${evaluation.gateStatus.name.lowercase()}",
                actor = "user",
                details = evaluation.warningCodes.takeIf { it.isNotEmpty() }?.joinToString(","),
            ),
        )
    }

    private fun fitAxis(
        axis: CalibrationAxis,
        anchors: List<ManualCalibrationAnchor>,
        plotArea: GraphRegion?,
    ): CalibrationAxisFitSummary {
        val evidence = anchors
            .filter { it.value.isFinite() }
            .map { anchor ->
                CalibrationAnchorEvidence(
                    axis = axis.toGeometryAxis(),
                    tickPixelPosition = anchor.axisPixelPosition(),
                    value = anchor.value,
                    rawText = anchor.anchorId,
                    confidence = 1f,
                )
            }
        val fit = fitter.fit(
            axis = axis.toGeometryAxis(),
            anchors = evidence,
            axisLengthPx = plotArea?.let { if (axis == CalibrationAxis.X) it.width.toFloat() else it.height.toFloat() },
            geometryCleanliness = 1f,
        )
        return CalibrationAxisFitSummary(
            axis = axis,
            slope = fit.slope,
            intercept = fit.intercept,
            residualReport = fit.toResidualReport(axis),
            status = fit.status,
            warnings = fit.warnings,
        )
    }

    private fun duplicatePixelIssues(
        axis: CalibrationAxis,
        anchors: List<ManualCalibrationAnchor>,
    ): List<GuidedRoiValidationIssue> =
        anchors
            .filter { it.value.isFinite() }
            .groupBy { it.axisPixelPosition().roundKey() }
            .filterValues { group -> group.map { it.value.roundKey() }.distinct().size > 1 }
            .map {
                error(
                    "calibration.${axis.name.lowercase()}.duplicate_pixel_conflict",
                    "${axis.name} calibration has duplicate pixel positions with different values.",
                )
            }

    private fun monotonicityIssues(
        axis: CalibrationAxis,
        anchors: List<ManualCalibrationAnchor>,
    ): List<GuidedRoiValidationIssue> {
        val finite = anchors.filter { it.value.isFinite() }.sortedBy { it.axisPixelPosition() }
        if (finite.size < 2) return emptyList()
        val deltas = finite.zipWithNext { left, right -> right.value - left.value }
        if (deltas.any { it == 0.0 || !it.isFinite() }) {
            return listOf(
                error(
                    "calibration.${axis.name.lowercase()}.non_monotonic_values",
                    "${axis.name} calibration values must be monotonic.",
                ),
            )
        }
        val increasing = deltas.all { it > 0.0 }
        val decreasing = deltas.all { it < 0.0 }
        if (!increasing && !decreasing) {
            return listOf(
                error(
                    "calibration.${axis.name.lowercase()}.non_monotonic_values",
                    "${axis.name} calibration values must be monotonic.",
                ),
            )
        }
        return when {
            axis == CalibrationAxis.X && decreasing -> listOf(
                warning("calibration.x.reversed_direction_review", "X calibration direction is reversed and must remain review-grade."),
            )
            axis == CalibrationAxis.Y && increasing -> listOf(
                warning("calibration.y.positive_slope_review", "Y calibration increases downward and must remain review-grade."),
            )
            else -> emptyList()
        }
    }

    private fun com.chromalab.feature.processing.geometry.AxisCalibrationFit.toResidualReport(
        axis: CalibrationAxis,
    ): CalibrationResidualReport =
        CalibrationResidualReport(
            axis = axis,
            acceptedAnchorIds = acceptedAnchors.mapNotNull { it.rawText },
            rejectedAnchorIds = rejectedAnchors.mapNotNull { it.rawText },
            residuals = acceptedAnchors.mapIndexed { index, anchor ->
                CalibrationResidual(
                    anchorId = anchor.rawText ?: "${axis.name.lowercase()}_$index",
                    residualPx = residualsPx.getOrElse(index) { 0.0 },
                    residualUnit = residualsUnit.getOrElse(index) { 0.0 },
                )
            },
            maxResidualPx = maxResidualPx,
            rmsePx = rmsePx,
            r2 = r2,
            monotonicityStatus = when (status) {
                CalibrationFitStatus.VALID -> CalibrationMonotonicityStatus.VALID
                CalibrationFitStatus.REVIEW -> CalibrationMonotonicityStatus.REVIEW
                CalibrationFitStatus.INVALID -> CalibrationMonotonicityStatus.INVALID
            },
            gateStatus = when (status) {
                CalibrationFitStatus.VALID -> GuidedWorkflowGateStatus.USER_CONFIRMED
                CalibrationFitStatus.REVIEW -> GuidedWorkflowGateStatus.REVIEW_REQUIRED
                CalibrationFitStatus.INVALID -> GuidedWorkflowGateStatus.INVALID
            },
            warnings = warnings,
        )

    private fun error(code: String, message: String): GuidedRoiValidationIssue =
        GuidedRoiValidationIssue(code, GuidedRoiValidationSeverity.ERROR, message)

    private fun warning(code: String, message: String): GuidedRoiValidationIssue =
        GuidedRoiValidationIssue(code, GuidedRoiValidationSeverity.WARNING, message)

    private fun warning(axis: CalibrationAxis, code: String): GuidedRoiValidationIssue =
        warning(code, "${axis.name} calibration requires review: $code")
}

fun CalibrationAxis.toGeometryAxis(): GeometryAxis =
    when (this) {
        CalibrationAxis.X -> GeometryAxis.X
        CalibrationAxis.Y -> GeometryAxis.Y
    }

fun ManualCalibrationAnchor.axisPixelPosition(): Float =
    when (axis) {
        CalibrationAxis.X -> pixel.x
        CalibrationAxis.Y -> pixel.y
    }

private fun GeometryPoint.coerceInside(region: GraphRegion): GeometryPoint =
    GeometryPoint(
        x = x.coerceIn(region.x.toFloat(), region.right.toFloat()),
        y = y.coerceIn(region.y.toFloat(), region.bottom.toFloat()),
    )

private fun Float.roundKey(): Int = (this * CALIBRATION_POINT_KEY_SCALE).roundToInt()

private fun Double.roundKey(): Long = (this * CALIBRATION_POINT_KEY_SCALE).toLong()

package com.chromalab.feature.processing.guided

import com.chromalab.feature.processing.curve.CurveExtractionResult
import com.chromalab.feature.processing.graph.GraphRegion
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

private const val TRACE_VALID_MIN_POINT_COUNT = 24
private const val TRACE_REVIEW_MIN_POINT_COUNT = 8
private const val TRACE_VALID_COLUMN_COVERAGE = 0.30
private const val TRACE_REVIEW_COLUMN_COVERAGE = 0.05
private const val TRACE_VALID_CONFIDENCE = 0.70
private const val TRACE_REVIEW_CONFIDENCE = 0.35
private const val TRACE_VALID_TEXT_CONTAMINATION_MAX = 0.35
private const val TRACE_INVALID_TEXT_CONTAMINATION_MIN = 0.65
private const val TRACE_VALID_FRAME_TOUCH_MAX = 0.20
private const val TRACE_INVALID_FRAME_TOUCH_MIN = 0.45
private const val TRACE_VALID_MAX_GAP_RATIO = 0.25
private const val TRACE_REVIEW_MAX_GAP_RATIO = 0.55

@Serializable
enum class TraceOverlayEditorStatus {
    SUGGESTED,
    VALID,
    REVIEW,
    INVALID,
    REJECTED,
}

@Serializable
data class TraceOverlayEditorSnapshot(
    val imageWidth: Int,
    val imageHeight: Int,
    val graphPanelBounds: GraphRegion? = null,
    val plotAreaBounds: GraphRegion? = null,
    val sourceTraceId: String = "trace_auto",
    val tracePoints: List<TraceOverlayPoint> = emptyList(),
    val autoTracePoints: List<TraceOverlayPoint> = tracePoints,
    val qualitySummary: TraceQualitySummary = TraceQualitySummary(),
    val source: TraceOverlaySource = TraceOverlaySource.AUTO_EXTRACTED,
    val calibrationSetId: String? = null,
    val calibratedTraceRequired: Boolean = true,
    val overlayArtifactPath: String? = null,
    val maskArtifactPath: String? = null,
    val centerlineArtifactPath: String? = null,
    val warnings: List<String> = emptyList(),
    val rejectionReason: String? = null,
) {
    val evaluation: TraceOverlayEvaluation
        get() = GuidedTraceOverlayReducer.evaluate(this)
}

@Serializable
data class TraceOverlayEvaluation(
    val qualityStatus: TraceQualityStatus = TraceQualityStatus.MISSING,
    val editorStatus: TraceOverlayEditorStatus = TraceOverlayEditorStatus.SUGGESTED,
    val gateStatus: TraceGateStatus = TraceGateStatus.MISSING,
    val qualitySummary: TraceQualitySummary = TraceQualitySummary(),
    val issues: List<GuidedRoiValidationIssue> = emptyList(),
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

    val canAcceptValid: Boolean
        get() = !hasErrors && qualityStatus == TraceQualityStatus.VALID

    val canAcceptReview: Boolean
        get() = !hasErrors && qualityStatus != TraceQualityStatus.INVALID && qualityStatus != TraceQualityStatus.MISSING
}

object GuidedTraceOverlayReducer {
    fun initialSnapshot(
        imageWidth: Int,
        imageHeight: Int,
        graphPanelBounds: GraphRegion?,
        plotAreaBounds: GraphRegion?,
        sourceTraceId: String = "trace_auto",
        tracePoints: List<TraceOverlayPoint> = emptyList(),
        qualitySummary: TraceQualitySummary = TraceQualitySummary(),
        calibrationSetId: String? = null,
        calibratedTraceRequired: Boolean = true,
        overlayArtifactPath: String? = null,
        maskArtifactPath: String? = null,
        centerlineArtifactPath: String? = null,
        warnings: List<String> = emptyList(),
    ): TraceOverlayEditorSnapshot =
        TraceOverlayEditorSnapshot(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            graphPanelBounds = graphPanelBounds,
            plotAreaBounds = plotAreaBounds,
            sourceTraceId = sourceTraceId,
            tracePoints = tracePoints,
            autoTracePoints = tracePoints,
            qualitySummary = qualitySummary,
            calibrationSetId = calibrationSetId,
            calibratedTraceRequired = calibratedTraceRequired,
            overlayArtifactPath = overlayArtifactPath,
            maskArtifactPath = maskArtifactPath,
            centerlineArtifactPath = centerlineArtifactPath,
            warnings = warnings,
        )

    fun fromCurveExtraction(
        imageWidth: Int,
        imageHeight: Int,
        graphPanelBounds: GraphRegion?,
        plotAreaBounds: GraphRegion?,
        sourceTraceId: String,
        result: CurveExtractionResult,
        overlayArtifactPath: String? = null,
        centerlineArtifactPath: String? = null,
        calibrationSetId: String? = null,
    ): TraceOverlayEditorSnapshot {
        val offsetX = plotAreaBounds?.x ?: 0
        val offsetY = plotAreaBounds?.y ?: 0
        val points = result.points.map { point ->
            TraceOverlayPoint(
                x = (offsetX + point.pixelX).toFloat(),
                y = offsetY + point.pixelY,
                confidence = point.confidence,
            )
        }
        val audit = result.centerlineAudit
        val summary = TraceQualitySummary(
            pointCount = points.size,
            columnCoverageRatio = result.coverage.toDouble(),
            maxGapColumns = result.estimatedMaxGapColumns(),
            componentCount = audit.trunkPathComponentCount.takeIf { it > 0 }
                ?: audit.fragmentReconstructionComponentCount.takeIf { it > 0 },
            branchPointCount = audit.trunkPathJunctionCount.takeIf { it > 0 } ?: audit.branchColumnCount.takeIf { it > 0 },
            selectedComponentCoverage = audit.trunkPathCoverage.takeIf { it > 0f }?.toDouble()
                ?: audit.centerlineCoverage.takeIf { it > 0f }?.toDouble(),
            textContaminationScore = null,
            baselineTouchRatio = null,
            frameTouchRatio = audit.largeDeltaColumnRatio.takeIf { it > 0f }?.toDouble(),
            traceConfidence = points.map { it.confidence.toDouble() }.averageOrNull(),
            confidence = points.map { it.confidence.toDouble() }.averageOrNull(),
        )
        return initialSnapshot(
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            graphPanelBounds = graphPanelBounds,
            plotAreaBounds = plotAreaBounds,
            sourceTraceId = sourceTraceId,
            tracePoints = points,
            qualitySummary = summary,
            calibrationSetId = calibrationSetId,
            overlayArtifactPath = overlayArtifactPath,
            maskArtifactPath = result.maskImagePath,
            centerlineArtifactPath = centerlineArtifactPath,
            warnings = result.warnings + audit.warnings,
        )
    }

    fun resetToSuggestion(snapshot: TraceOverlayEditorSnapshot): TraceOverlayEditorSnapshot =
        snapshot.copy(
            tracePoints = snapshot.autoTracePoints,
            source = TraceOverlaySource.AUTO_EXTRACTED,
            rejectionReason = null,
        )

    fun rejectTrace(
        state: GuidedDigitizationState,
        snapshot: TraceOverlayEditorSnapshot,
        userProvenance: GuidedUserProvenance,
        timestampEpochMillis: Long,
        reason: String,
    ): GuidedDigitizationState =
        confirmInternal(
            state = state,
            snapshot = snapshot.copy(rejectionReason = reason),
            userProvenance = userProvenance,
            timestampEpochMillis = timestampEpochMillis,
            confirmationStatus = UserConfirmationStatus.REJECTED,
            source = TraceOverlaySource.USER_REJECTED,
            editDecision = TraceEditDecision.REJECT_AUTO,
            traceConfirmationStatus = TraceConfirmationStatus.USER_REJECTED,
            gateStatus = TraceGateStatus.INVALID,
            warnings = snapshot.warnings + listOf("trace.user_rejected"),
            action = "trace_rejected",
        )

    fun acceptTrace(
        state: GuidedDigitizationState,
        snapshot: TraceOverlayEditorSnapshot,
        userProvenance: GuidedUserProvenance,
        timestampEpochMillis: Long,
        reviewGrade: Boolean,
        overlayArtifactPath: String? = snapshot.overlayArtifactPath,
    ): GuidedDigitizationState {
        val evaluation = evaluate(snapshot)
        require(evaluation.canAcceptReview) {
            "Trace cannot be confirmed: ${(evaluation.errorCodes + evaluation.warningCodes).joinToString(",")}"
        }
        if (!reviewGrade) {
            require(evaluation.canAcceptValid) {
                "Trace valid confirmation requires VALID trace quality."
            }
        }
        val gateStatus = if (reviewGrade || evaluation.qualityStatus == TraceQualityStatus.REVIEW) {
            TraceGateStatus.REVIEW_REQUIRED
        } else {
            TraceGateStatus.USER_CONFIRMED
        }
        val source = if (gateStatus == TraceGateStatus.REVIEW_REQUIRED) {
            TraceOverlaySource.USER_REVIEW_CONFIRMED
        } else {
            TraceOverlaySource.USER_CONFIRMED
        }
        val traceConfirmationStatus = if (gateStatus == TraceGateStatus.REVIEW_REQUIRED) {
            TraceConfirmationStatus.USER_CONFIRMED_REVIEW
        } else {
            TraceConfirmationStatus.USER_CONFIRMED_VALID
        }
        val editDecision = if (gateStatus == TraceGateStatus.REVIEW_REQUIRED) {
            TraceEditDecision.MARK_REVIEW
        } else {
            TraceEditDecision.ACCEPT_AUTO
        }
        return confirmInternal(
            state = state,
            snapshot = snapshot.copy(overlayArtifactPath = overlayArtifactPath),
            userProvenance = userProvenance,
            timestampEpochMillis = timestampEpochMillis,
            confirmationStatus = UserConfirmationStatus.CONFIRMED,
            source = source,
            editDecision = editDecision,
            traceConfirmationStatus = traceConfirmationStatus,
            gateStatus = gateStatus,
            warnings = evaluation.warningCodes + snapshot.warnings,
            action = "trace_confirmed:${gateStatus.name.lowercase()}",
        )
    }

    fun evaluate(snapshot: TraceOverlayEditorSnapshot): TraceOverlayEvaluation {
        val issues = mutableListOf<GuidedRoiValidationIssue>()
        val plotArea = snapshot.plotAreaBounds
        if (plotArea == null) {
            issues += error("trace.plot_area_missing", "Trace confirmation requires a confirmed plot area.")
        }
        if (snapshot.tracePoints.isEmpty()) {
            issues += error("trace.points_missing", "Trace confirmation requires extracted trace points.")
        }
        if (snapshot.calibratedTraceRequired && snapshot.calibrationSetId == null) {
            issues += error("trace.calibration_missing", "Trace confirmation requires confirmed calibration evidence.")
        }
        if (plotArea != null && snapshot.tracePoints.any { !it.isInside(plotArea) }) {
            issues += error("trace.points_outside_plot_area", "Trace points must stay inside the confirmed plot area.")
        }

        val computedSummary = snapshot.qualitySummary.normalized(snapshot.tracePoints, plotArea)
        if (computedSummary.pointCount == null) {
            issues += warning("trace.metrics.point_count_unknown", "Trace point count is missing from quality evidence.")
        }
        if (computedSummary.columnCoverageRatio == null) {
            issues += warning("trace.metrics.column_coverage_unknown", "Trace column coverage is missing from quality evidence.")
        }
        if (computedSummary.maxGapColumns == null) {
            issues += warning("trace.metrics.max_gap_unknown", "Trace gap evidence is missing from quality evidence.")
        }
        if (computedSummary.traceConfidence == null && computedSummary.confidence == null) {
            issues += warning("trace.metrics.confidence_unknown", "Trace confidence is missing from quality evidence.")
        }

        issues += qualityIssues(computedSummary, plotArea)
        snapshot.warnings.forEach { issues += warning(it, "Trace extraction warning: $it") }

        val hasErrors = issues.any { it.severity == GuidedRoiValidationSeverity.ERROR }
        val hasWarnings = issues.any { it.severity == GuidedRoiValidationSeverity.WARNING }
        val qualityStatus = when {
            hasErrors -> TraceQualityStatus.INVALID
            snapshot.tracePoints.isEmpty() -> TraceQualityStatus.MISSING
            hasWarnings -> TraceQualityStatus.REVIEW
            else -> TraceQualityStatus.VALID
        }
        val gateStatus = when (qualityStatus) {
            TraceQualityStatus.VALID -> TraceGateStatus.USER_CONFIRMED
            TraceQualityStatus.REVIEW -> TraceGateStatus.REVIEW_REQUIRED
            TraceQualityStatus.INVALID -> TraceGateStatus.INVALID
            TraceQualityStatus.MISSING -> TraceGateStatus.MISSING
        }
        val editorStatus = when (qualityStatus) {
            TraceQualityStatus.VALID -> TraceOverlayEditorStatus.VALID
            TraceQualityStatus.REVIEW -> TraceOverlayEditorStatus.REVIEW
            TraceQualityStatus.INVALID -> TraceOverlayEditorStatus.INVALID
            TraceQualityStatus.MISSING -> TraceOverlayEditorStatus.SUGGESTED
        }
        return TraceOverlayEvaluation(
            qualityStatus = qualityStatus,
            editorStatus = editorStatus,
            gateStatus = gateStatus,
            qualitySummary = computedSummary,
            issues = issues.distinctBy { it.code },
        )
    }

    private fun confirmInternal(
        state: GuidedDigitizationState,
        snapshot: TraceOverlayEditorSnapshot,
        userProvenance: GuidedUserProvenance,
        timestampEpochMillis: Long,
        confirmationStatus: UserConfirmationStatus,
        source: TraceOverlaySource,
        editDecision: TraceEditDecision,
        traceConfirmationStatus: TraceConfirmationStatus,
        gateStatus: TraceGateStatus,
        warnings: List<String>,
        action: String,
    ): GuidedDigitizationState {
        val confirmedPlotArea = state.plotAreaConfirmation?.confirmedPlotArea
        require(confirmedPlotArea?.confirmationStatus == UserConfirmationStatus.CONFIRMED) {
            "Trace confirmation requires confirmed plotArea."
        }
        val calibration = state.calibration
        if (snapshot.calibratedTraceRequired) {
            require(calibration?.confirmationStatus == UserConfirmationStatus.CONFIRMED) {
                "Trace confirmation requires confirmed calibration."
            }
        }
        val image = requireNotNull(state.image) {
            "Trace confirmation requires guided image reference."
        }
        val evaluation = evaluate(snapshot)
        val evidence = TraceConfirmationEvidence(
            sourceTraceId = snapshot.sourceTraceId,
            overlayArtifactPath = snapshot.overlayArtifactPath,
            maskArtifactPath = snapshot.maskArtifactPath,
            centerlineArtifactPath = snapshot.centerlineArtifactPath,
            qualityStatus = if (gateStatus == TraceGateStatus.INVALID && source == TraceOverlaySource.USER_REJECTED) {
                TraceQualityStatus.INVALID
            } else {
                evaluation.qualityStatus
            },
            qualitySummary = evaluation.qualitySummary,
            warnings = warnings.distinct(),
            source = source,
            tracePoints = snapshot.tracePoints,
            plotAreaBounds = snapshot.plotAreaBounds,
            calibrationSetId = snapshot.calibrationSetId ?: calibration?.calibrationSet?.calibrationSetId,
            rejectionReason = snapshot.rejectionReason,
        )
        val trace = UserConfirmedTrace(
            sourceTraceId = snapshot.sourceTraceId,
            confirmationStatus = confirmationStatus,
            editDecisions = listOf(editDecision),
            evidence = evidence,
            timestampEpochMillis = timestampEpochMillis,
            userProvenance = userProvenance,
            validationWarnings = warnings.distinct(),
            gateStatus = gateStatus,
            source = source,
            traceConfirmationStatus = traceConfirmationStatus,
        )
        return state.copy(
            currentStep = GuidedWorkflowStep.TRACE_CONFIRMED,
            stepStatuses = state.stepStatuses + mapOf(
                GuidedWorkflowStep.TRACE_EXTRACTED to if (gateStatus == TraceGateStatus.INVALID) {
                    GuidedStepStatus.REJECTED
                } else {
                    GuidedStepStatus.SUGGESTED
                },
                GuidedWorkflowStep.TRACE_CONFIRMED to when (gateStatus) {
                    TraceGateStatus.USER_CONFIRMED -> GuidedStepStatus.CONFIRMED
                    TraceGateStatus.REVIEW_REQUIRED -> GuidedStepStatus.REVIEW_REQUIRED
                    TraceGateStatus.INVALID -> GuidedStepStatus.REJECTED
                    TraceGateStatus.MISSING -> GuidedStepStatus.BLOCKED
                },
            ),
            trace = trace,
            updatedAtEpochMillis = timestampEpochMillis,
            auditTrail = state.auditTrail + GuidedWorkflowAuditEntry(
                timestampEpochMillis = timestampEpochMillis,
                step = GuidedWorkflowStep.TRACE_CONFIRMED,
                action = action,
                actor = "user",
                details = buildString {
                    append("sourceTraceId=${snapshot.sourceTraceId}")
                    append(";imageId=${image.imageId}")
                    snapshot.rejectionReason?.let { append(";reason=$it") }
                    if (warnings.isNotEmpty()) append(";warnings=${warnings.distinct().joinToString(",")}")
                },
            ),
        )
    }

    private fun qualityIssues(
        summary: TraceQualitySummary,
        plotArea: GraphRegion?,
    ): List<GuidedRoiValidationIssue> {
        val issues = mutableListOf<GuidedRoiValidationIssue>()
        val pointCount = summary.pointCount
        when {
            pointCount == null -> Unit
            pointCount == 0 -> issues += error("trace.points_missing", "Trace confirmation requires extracted trace points.")
            pointCount < TRACE_REVIEW_MIN_POINT_COUNT ->
                issues += error("trace.point_count_too_low", "Trace has too few points to confirm.")
            pointCount < TRACE_VALID_MIN_POINT_COUNT ->
                issues += warning("trace.point_count_review", "Trace point count is low and requires review.")
        }

        val coverage = summary.columnCoverageRatio
        when {
            coverage == null -> Unit
            coverage < TRACE_REVIEW_COLUMN_COVERAGE ->
                issues += error("trace.column_coverage_too_sparse", "Trace column coverage is too sparse.")
            coverage < TRACE_VALID_COLUMN_COVERAGE ->
                issues += warning("trace.column_coverage_review", "Trace column coverage is sparse and requires review.")
        }

        val maxGap = summary.maxGapColumns
        val plotWidth = plotArea?.width?.coerceAtLeast(1)
        if (maxGap != null && plotWidth != null) {
            val gapRatio = maxGap.toDouble() / plotWidth.toDouble()
            when {
                gapRatio > TRACE_REVIEW_MAX_GAP_RATIO ->
                    issues += error("trace.max_gap_too_large", "Trace has a blocking gap across the plot area.")
                gapRatio > TRACE_VALID_MAX_GAP_RATIO ->
                    issues += warning("trace.max_gap_review", "Trace has large gaps and requires review.")
            }
        }

        val contamination = summary.textContaminationScore
        when {
            contamination == null -> Unit
            contamination >= TRACE_INVALID_TEXT_CONTAMINATION_MIN ->
                issues += error("trace.text_contamination_invalid", "Trace is severely contaminated by text evidence.")
            contamination > TRACE_VALID_TEXT_CONTAMINATION_MAX ->
                issues += warning("trace.text_contamination_review", "Trace has text contamination and requires review.")
        }

        val frameTouch = summary.frameTouchRatio
        when {
            frameTouch == null -> Unit
            frameTouch >= TRACE_INVALID_FRAME_TOUCH_MIN ->
                issues += error("trace.frame_touch_invalid", "Trace appears dominated by frame or border contact.")
            frameTouch > TRACE_VALID_FRAME_TOUCH_MAX ->
                issues += warning("trace.frame_touch_review", "Trace touches frame/border evidence and requires review.")
        }

        val confidence = summary.traceConfidence ?: summary.confidence
        when {
            confidence == null -> Unit
            confidence < TRACE_REVIEW_CONFIDENCE ->
                issues += error("trace.confidence_too_low", "Trace confidence is too low.")
            confidence < TRACE_VALID_CONFIDENCE ->
                issues += warning("trace.confidence_review", "Trace confidence is marginal and requires review.")
        }
        if ((summary.componentCount ?: 1) > 3) {
            issues += warning("trace.fragmented_review", "Trace has multiple components and requires review.")
        }
        if ((summary.branchPointCount ?: 0) > 8) {
            issues += warning("trace.branch_points_review", "Trace has branch-like structure and requires review.")
        }
        return issues
    }

    private fun TraceQualitySummary.normalized(
        points: List<TraceOverlayPoint>,
        plotArea: GraphRegion?,
    ): TraceQualitySummary {
        val inferredCoverage = columnCoverageRatio ?: points.columnCoverage(plotArea)
        val inferredMaxGap = maxGapColumns ?: points.maxGapColumns()
        val inferredConfidence = traceConfidence ?: confidence ?: points.map { it.confidence.toDouble() }.averageOrNull()
        return copy(
            pointCount = pointCount ?: points.size,
            columnCoverageRatio = inferredCoverage,
            maxGapColumns = inferredMaxGap,
            traceConfidence = inferredConfidence,
            confidence = confidence ?: inferredConfidence,
        )
    }

    private fun TraceOverlayPoint.isInside(region: GraphRegion): Boolean =
        x >= region.x &&
            y >= region.y &&
            x <= region.right &&
            y <= region.bottom

    private fun List<TraceOverlayPoint>.columnCoverage(plotArea: GraphRegion?): Double? {
        val width = plotArea?.width?.takeIf { it > 0 } ?: return null
        if (isEmpty()) return 0.0
        return map { it.x.roundToInt() }
            .distinct()
            .size
            .toDouble()
            .div(width.toDouble())
            .coerceIn(0.0, 1.0)
    }

    private fun List<TraceOverlayPoint>.maxGapColumns(): Int? {
        if (size < 2) return null
        val xs = map { it.x.roundToInt() }.distinct().sorted()
        return xs.zipWithNext { left, right -> right - left }.maxOrNull()
    }

    private fun CurveExtractionResult.estimatedMaxGapColumns(): Int? {
        if (points.size < 2) return null
        val xs = points.map { it.pixelX }.distinct().sorted()
        return xs.zipWithNext { left, right -> right - left }.maxOrNull()
    }

    private fun Iterable<Double>.averageOrNull(): Double? {
        var count = 0
        var sum = 0.0
        forEach { value ->
            sum += value
            count++
        }
        return if (count == 0) null else sum / count
    }

    private fun error(code: String, message: String): GuidedRoiValidationIssue =
        GuidedRoiValidationIssue(code, GuidedRoiValidationSeverity.ERROR, message)

    private fun warning(code: String, message: String): GuidedRoiValidationIssue =
        GuidedRoiValidationIssue(code, GuidedRoiValidationSeverity.WARNING, message)
}

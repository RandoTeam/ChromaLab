package com.chromalab.feature.processing.guided

import com.chromalab.feature.processing.geometry.GeometryPoint
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.reports.EvidenceGateStatus
import com.chromalab.feature.reports.GateEvidence
import com.chromalab.feature.reports.ProcessingMode
import com.chromalab.feature.reports.ReportGateStatus
import kotlinx.serialization.Serializable

const val CURRENT_GUIDED_DIGITIZATION_STATE_SCHEMA = "1.0.0-phase-1"

@Serializable
enum class GuidedDigitizationMode {
    AUTO_DIAGNOSTIC,
    GUIDED_PRODUCTION,
    MANUAL_ADVANCED,
}

fun GuidedDigitizationMode.toProcessingMode(): ProcessingMode =
    when (this) {
        GuidedDigitizationMode.AUTO_DIAGNOSTIC -> ProcessingMode.AUTO_DIAGNOSTIC
        GuidedDigitizationMode.GUIDED_PRODUCTION -> ProcessingMode.GUIDED_PRODUCTION
        GuidedDigitizationMode.MANUAL_ADVANCED -> ProcessingMode.MANUAL_ADVANCED
    }

@Serializable
enum class GuidedWorkflowStep {
    IMAGE_LOADED,
    GRAPH_PANEL_SUGGESTED,
    GRAPH_PANEL_CONFIRMED,
    PLOT_AREA_SUGGESTED,
    PLOT_AREA_CONFIRMED,
    AXIS_TICKS_SUGGESTED,
    CALIBRATION_POINTS_CONFIRMED,
    CALIBRATION_VALIDATED,
    TRACE_EXTRACTED,
    TRACE_CONFIRMED,
    PEAKS_DETECTED,
    PEAKS_CONFIRMED,
    REPORT_READY,
    DIAGNOSTIC_ONLY,
}

@Serializable
enum class GuidedStepStatus {
    NOT_STARTED,
    SUGGESTED,
    IN_PROGRESS,
    CONFIRMED,
    VALIDATED,
    REVIEW_REQUIRED,
    REJECTED,
    BLOCKED,
    SKIPPED,
}

@Serializable
enum class UserConfirmationStatus {
    NOT_REQUIRED,
    NOT_REQUESTED,
    PENDING,
    CONFIRMED,
    REJECTED,
    REVISED,
    INVALIDATED,
}

@Serializable
enum class GuidedWorkflowGateStatus {
    SATISFIED,
    USER_CONFIRMED,
    REVIEW_REQUIRED,
    INVALID,
    MISSING,
    BLOCKED,
    NOT_APPLICABLE,
}

@Serializable
data class GuidedImageReference(
    val imageId: String,
    val originalImagePath: String? = null,
    val normalizedImagePath: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val sourceLabel: String? = null,
)

@Serializable
data class GuidedUserProvenance(
    val userIdHash: String? = null,
    val sessionId: String,
    val deviceIdHash: String? = null,
    val appVersion: String? = null,
)

@Serializable
data class GuidedWorkflowAuditEntry(
    val timestampEpochMillis: Long,
    val step: GuidedWorkflowStep,
    val action: String,
    val actor: String = "user",
    val details: String? = null,
)

@Serializable
data class RoiConfirmationEvidence(
    val relatedImageId: String,
    val relatedImagePath: String? = null,
    val overlayArtifactPath: String? = null,
    val sourceArtifactPath: String? = null,
    val validatorFindingCodes: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)

@Serializable
enum class RoiEditSource {
    AUTO_SUGGESTION,
    USER_DRAG,
    USER_NUMERIC_ENTRY,
    IMPORTED_TEMPLATE,
    MANUAL_ADVANCED,
}

@Serializable
data class UserConfirmedGraphPanel(
    val bounds: GraphRegion,
    val source: RoiEditSource,
    val confirmationStatus: UserConfirmationStatus,
    val timestampEpochMillis: Long,
    val userProvenance: GuidedUserProvenance,
    val relatedImageId: String,
    val relatedImagePath: String? = null,
    val overlayArtifactPath: String? = null,
    val validationWarnings: List<String> = emptyList(),
    val gateStatus: GuidedWorkflowGateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
)

@Serializable
data class UserConfirmedPlotArea(
    val bounds: GraphRegion,
    val parentGraphPanelBounds: GraphRegion,
    val source: RoiEditSource,
    val confirmationStatus: UserConfirmationStatus,
    val timestampEpochMillis: Long,
    val userProvenance: GuidedUserProvenance,
    val relatedImageId: String,
    val relatedImagePath: String? = null,
    val overlayArtifactPath: String? = null,
    val validationWarnings: List<String> = emptyList(),
    val gateStatus: GuidedWorkflowGateStatus = GuidedWorkflowGateStatus.USER_CONFIRMED,
)

@Serializable
data class GraphPanelConfirmation(
    val suggestedBounds: GraphRegion? = null,
    val confirmedGraphPanel: UserConfirmedGraphPanel? = null,
    val evidence: RoiConfirmationEvidence? = null,
    val confirmationStatus: UserConfirmationStatus = UserConfirmationStatus.NOT_REQUESTED,
    val gateStatus: GuidedWorkflowGateStatus = GuidedWorkflowGateStatus.MISSING,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class PlotAreaConfirmation(
    val suggestedBounds: GraphRegion? = null,
    val confirmedPlotArea: UserConfirmedPlotArea? = null,
    val evidence: RoiConfirmationEvidence? = null,
    val confirmationStatus: UserConfirmationStatus = UserConfirmationStatus.NOT_REQUESTED,
    val gateStatus: GuidedWorkflowGateStatus = GuidedWorkflowGateStatus.MISSING,
    val warnings: List<String> = emptyList(),
)

@Serializable
enum class CalibrationAxis {
    X,
    Y,
}

@Serializable
enum class CalibrationAnchorSource {
    USER_CLICK,
    USER_NUMERIC_ENTRY,
    IMPORTED_METHOD,
    AUTO_SUGGESTION_ACCEPTED,
    MANUAL_ADVANCED,
}

@Serializable
enum class CalibrationAnchorStatus {
    CANDIDATE,
    ACCEPTED,
    REJECTED,
    OUTLIER,
    DUPLICATE,
}

@Serializable
enum class CalibrationMonotonicityStatus {
    VALID,
    REVIEW,
    INVALID,
    UNKNOWN,
}

@Serializable
data class ManualCalibrationAnchor(
    val anchorId: String,
    val axis: CalibrationAxis,
    val pixel: GeometryPoint,
    val value: Double,
    val unitLabel: String? = null,
    val source: CalibrationAnchorSource,
    val status: CalibrationAnchorStatus = CalibrationAnchorStatus.CANDIDATE,
    val timestampEpochMillis: Long,
    val userProvenance: GuidedUserProvenance,
    val relatedImageId: String,
    val overlayArtifactPath: String? = null,
    val validationWarnings: List<String> = emptyList(),
)

@Serializable
data class CalibrationResidual(
    val anchorId: String,
    val residualPx: Double,
    val residualUnit: Double,
)

@Serializable
data class CalibrationResidualReport(
    val axis: CalibrationAxis,
    val acceptedAnchorIds: List<String> = emptyList(),
    val rejectedAnchorIds: List<String> = emptyList(),
    val residuals: List<CalibrationResidual> = emptyList(),
    val maxResidualPx: Double? = null,
    val rmsePx: Double? = null,
    val r2: Double? = null,
    val monotonicityStatus: CalibrationMonotonicityStatus = CalibrationMonotonicityStatus.UNKNOWN,
    val gateStatus: GuidedWorkflowGateStatus = GuidedWorkflowGateStatus.MISSING,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class UserCalibrationSet(
    val calibrationSetId: String,
    val anchors: List<ManualCalibrationAnchor> = emptyList(),
    val xUnitLabel: String? = null,
    val yUnitLabel: String? = null,
    val residualReports: List<CalibrationResidualReport> = emptyList(),
    val timestampEpochMillis: Long,
    val userProvenance: GuidedUserProvenance,
    val gateStatus: GuidedWorkflowGateStatus = GuidedWorkflowGateStatus.MISSING,
    val warnings: List<String> = emptyList(),
) {
    fun acceptedAnchors(axis: CalibrationAxis): List<ManualCalibrationAnchor> =
        anchors.filter { it.axis == axis && it.status == CalibrationAnchorStatus.ACCEPTED }

    fun hasMinimumAnchors(): Boolean =
        acceptedAnchors(CalibrationAxis.X).size >= MIN_ANCHORS_PER_AXIS &&
            acceptedAnchors(CalibrationAxis.Y).size >= MIN_ANCHORS_PER_AXIS

    fun isRobustFitReady(): Boolean =
        acceptedAnchors(CalibrationAxis.X).size >= ROBUST_ANCHORS_PER_AXIS &&
            acceptedAnchors(CalibrationAxis.Y).size >= ROBUST_ANCHORS_PER_AXIS

    companion object {
        const val MIN_ANCHORS_PER_AXIS = 2
        const val ROBUST_ANCHORS_PER_AXIS = 3
    }
}

@Serializable
data class UserConfirmedCalibration(
    val calibrationSet: UserCalibrationSet,
    val confirmationStatus: UserConfirmationStatus,
    val timestampEpochMillis: Long,
    val userProvenance: GuidedUserProvenance,
    val overlayArtifactPath: String? = null,
    val validationWarnings: List<String> = emptyList(),
    val gateStatus: GuidedWorkflowGateStatus = GuidedWorkflowGateStatus.MISSING,
) {
    val hasMinimumAnchors: Boolean
        get() = calibrationSet.hasMinimumAnchors()
}

@Serializable
enum class TraceQualityStatus {
    VALID,
    REVIEW,
    INVALID,
    MISSING,
}

@Serializable
enum class TraceEditDecision {
    ACCEPT_AUTO,
    REJECT_AUTO,
    MARK_REVIEW,
    TRIM_SEGMENT,
    REDRAW_SEGMENT,
    IMPORT_TRACE,
}

@Serializable
enum class TraceGateStatus {
    USER_CONFIRMED,
    REVIEW_REQUIRED,
    INVALID,
    MISSING,
}

@Serializable
data class TraceQualitySummary(
    val pointCount: Int? = null,
    val columnCoverageRatio: Double? = null,
    val maxGapColumns: Int? = null,
    val componentCount: Int? = null,
    val branchPointCount: Int? = null,
    val textContaminationScore: Double? = null,
    val confidence: Double? = null,
)

@Serializable
data class TraceConfirmationEvidence(
    val sourceTraceId: String? = null,
    val overlayArtifactPath: String? = null,
    val maskArtifactPath: String? = null,
    val centerlineArtifactPath: String? = null,
    val qualityStatus: TraceQualityStatus = TraceQualityStatus.MISSING,
    val qualitySummary: TraceQualitySummary = TraceQualitySummary(),
    val warnings: List<String> = emptyList(),
)

@Serializable
data class UserConfirmedTrace(
    val sourceTraceId: String,
    val confirmationStatus: UserConfirmationStatus,
    val editDecisions: List<TraceEditDecision> = emptyList(),
    val evidence: TraceConfirmationEvidence,
    val timestampEpochMillis: Long,
    val userProvenance: GuidedUserProvenance,
    val validationWarnings: List<String> = emptyList(),
    val gateStatus: TraceGateStatus = TraceGateStatus.MISSING,
)

@Serializable
enum class PeakEditAction {
    ADD,
    REMOVE,
    MERGE,
    SPLIT,
    ADJUST_BOUNDARY,
    MARK_SHOULDER,
    MARK_REVIEW,
    ACCEPT_AUTO,
}

@Serializable
enum class PeakReviewStatus {
    NOT_REVIEWED,
    REVIEW,
    USER_CONFIRMED,
    USER_REJECTED,
    INVALID,
}

@Serializable
enum class PeakReviewGateStatus {
    USER_CONFIRMED,
    REVIEW_REQUIRED,
    INVALID,
    MISSING,
}

@Serializable
data class UserPeakEditDecision(
    val decisionId: String,
    val action: PeakEditAction,
    val peakId: String? = null,
    val targetPeakIds: List<String> = emptyList(),
    val reason: String? = null,
    val timestampEpochMillis: Long,
    val userProvenance: GuidedUserProvenance,
    val evidenceOverlayPath: String? = null,
)

@Serializable
data class UserConfirmedPeakSet(
    val peakSetId: String,
    val decisions: List<UserPeakEditDecision> = emptyList(),
    val reportablePeakIds: List<String> = emptyList(),
    val rejectedPeakIds: List<String> = emptyList(),
    val reviewPeakIds: List<String> = emptyList(),
    val reviewStatus: PeakReviewStatus = PeakReviewStatus.NOT_REVIEWED,
    val timestampEpochMillis: Long,
    val userProvenance: GuidedUserProvenance,
    val gateStatus: PeakReviewGateStatus = PeakReviewGateStatus.MISSING,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class GuidedDigitizationState(
    val stateId: String,
    val schemaVersion: String = CURRENT_GUIDED_DIGITIZATION_STATE_SCHEMA,
    val mode: GuidedDigitizationMode = GuidedDigitizationMode.AUTO_DIAGNOSTIC,
    val currentStep: GuidedWorkflowStep = GuidedWorkflowStep.IMAGE_LOADED,
    val stepStatuses: Map<GuidedWorkflowStep, GuidedStepStatus> = defaultStepStatuses(),
    val image: GuidedImageReference? = null,
    val graphPanelConfirmation: GraphPanelConfirmation? = null,
    val plotAreaConfirmation: PlotAreaConfirmation? = null,
    val calibration: UserConfirmedCalibration? = null,
    val trace: UserConfirmedTrace? = null,
    val peaks: UserConfirmedPeakSet? = null,
    val autoDiagnosticEvidence: GateEvidence = GateEvidence(),
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val auditTrail: List<GuidedWorkflowAuditEntry> = emptyList(),
    val warnings: List<String> = emptyList(),
) {
    val isGuidedReleaseMode: Boolean
        get() = mode == GuidedDigitizationMode.GUIDED_PRODUCTION ||
            mode == GuidedDigitizationMode.MANUAL_ADVANCED

    fun withStepStatus(
        step: GuidedWorkflowStep,
        status: GuidedStepStatus,
        timestampEpochMillis: Long = updatedAtEpochMillis,
    ): GuidedDigitizationState =
        copy(
            stepStatuses = stepStatuses + (step to status),
            updatedAtEpochMillis = timestampEpochMillis,
        )

    companion object {
        fun defaultStepStatuses(): Map<GuidedWorkflowStep, GuidedStepStatus> =
            GuidedWorkflowStep.entries.associateWith { step ->
                if (step == GuidedWorkflowStep.IMAGE_LOADED) {
                    GuidedStepStatus.IN_PROGRESS
                } else {
                    GuidedStepStatus.NOT_STARTED
                }
            }
    }
}

@Serializable
data class GuidedWorkflowGateEvaluation(
    val status: ReportGateStatus,
    val evidence: GateEvidence,
    val blockingReasons: List<String> = emptyList(),
    val reviewReasons: List<String> = emptyList(),
)

object GuidedDigitizationStateMachine {
    val orderedSteps: List<GuidedWorkflowStep> = GuidedWorkflowStep.entries

    fun canTransitionTo(
        state: GuidedDigitizationState,
        targetStep: GuidedWorkflowStep,
    ): Boolean {
        if (targetStep == state.currentStep) return true
        if (targetStep == GuidedWorkflowStep.DIAGNOSTIC_ONLY) return true

        val currentIndex = orderedSteps.indexOf(state.currentStep)
        val targetIndex = orderedSteps.indexOf(targetStep)
        if (currentIndex < 0 || targetIndex < 0) return false
        return targetIndex == currentIndex + 1
    }

    fun transitionTo(
        state: GuidedDigitizationState,
        targetStep: GuidedWorkflowStep,
        timestampEpochMillis: Long,
        action: String = "transition",
        actor: String = "system",
    ): GuidedDigitizationState {
        require(canTransitionTo(state, targetStep)) {
            "Invalid guided workflow transition from ${state.currentStep} to $targetStep"
        }

        return state.copy(
            currentStep = targetStep,
            stepStatuses = state.stepStatuses + (targetStep to targetStep.defaultStatus()),
            updatedAtEpochMillis = timestampEpochMillis,
            auditTrail = state.auditTrail + GuidedWorkflowAuditEntry(
                timestampEpochMillis = timestampEpochMillis,
                step = targetStep,
                action = action,
                actor = actor,
            ),
        )
    }

    private fun GuidedWorkflowStep.defaultStatus(): GuidedStepStatus =
        when {
            name.endsWith("_SUGGESTED") -> GuidedStepStatus.SUGGESTED
            name.endsWith("_CONFIRMED") -> GuidedStepStatus.IN_PROGRESS
            this == GuidedWorkflowStep.CALIBRATION_VALIDATED -> GuidedStepStatus.IN_PROGRESS
            this == GuidedWorkflowStep.TRACE_EXTRACTED -> GuidedStepStatus.SUGGESTED
            this == GuidedWorkflowStep.PEAKS_DETECTED -> GuidedStepStatus.SUGGESTED
            this == GuidedWorkflowStep.REPORT_READY -> GuidedStepStatus.IN_PROGRESS
            this == GuidedWorkflowStep.DIAGNOSTIC_ONLY -> GuidedStepStatus.BLOCKED
            else -> GuidedStepStatus.IN_PROGRESS
        }
}

object GuidedReportGateMapper {
    fun evaluate(state: GuidedDigitizationState): GuidedWorkflowGateEvaluation {
        val evidence = if (state.mode == GuidedDigitizationMode.AUTO_DIAGNOSTIC) {
            state.autoDiagnosticEvidence.copy(
                userConfirmationStatus = EvidenceGateStatus.NOT_APPLICABLE,
            )
        } else {
            GateEvidence(
                graphPanelStatus = state.graphPanelConfirmation.toEvidenceGate(),
                plotAreaStatus = state.plotAreaConfirmation.toEvidenceGate(),
                xCalibrationStatus = state.calibration.toCalibrationEvidenceGate(CalibrationAxis.X),
                yCalibrationStatus = state.calibration.toCalibrationEvidenceGate(CalibrationAxis.Y),
                traceStatus = state.trace.toEvidenceGate(),
                peakReviewStatus = state.peaks.toEvidenceGate(),
                evidencePackageStatus = state.autoDiagnosticEvidence.evidencePackageStatus,
                sourceProvenanceStatus = if (state.image?.normalizedImagePath != null) {
                    EvidenceGateStatus.VALID
                } else {
                    state.autoDiagnosticEvidence.sourceProvenanceStatus
                },
                userConfirmationStatus = state.userConfirmationGate(),
                vlmEvidenceStatus = EvidenceGateStatus.NOT_APPLICABLE,
            )
        }

        val blocking = evidence.phase1BlockingReasons()
        val review = evidence.phase1ReviewReasons()
        val status = when {
            blocking.any { it == "image.missing" } -> ReportGateStatus.BLOCKED
            blocking.isNotEmpty() -> ReportGateStatus.DIAGNOSTIC_ONLY
            review.isNotEmpty() -> ReportGateStatus.REVIEW_ONLY
            else -> ReportGateStatus.RELEASE_READY
        }

        return GuidedWorkflowGateEvaluation(
            status = status,
            evidence = evidence,
            blockingReasons = blocking.distinct(),
            reviewReasons = review.distinct(),
        )
    }

    private fun GraphPanelConfirmation?.toEvidenceGate(): EvidenceGateStatus =
        when {
            this?.confirmedGraphPanel?.confirmationStatus == UserConfirmationStatus.CONFIRMED &&
                this.confirmedGraphPanel.gateStatus.isReleaseSatisfying() -> EvidenceGateStatus.USER_CONFIRMED
            this?.gateStatus == GuidedWorkflowGateStatus.REVIEW_REQUIRED -> EvidenceGateStatus.REVIEW
            this?.gateStatus == GuidedWorkflowGateStatus.INVALID ||
                this?.gateStatus == GuidedWorkflowGateStatus.BLOCKED -> EvidenceGateStatus.INVALID
            else -> EvidenceGateStatus.MISSING
        }

    private fun PlotAreaConfirmation?.toEvidenceGate(): EvidenceGateStatus =
        when {
            this?.confirmedPlotArea?.confirmationStatus == UserConfirmationStatus.CONFIRMED &&
                this.confirmedPlotArea.gateStatus.isReleaseSatisfying() -> EvidenceGateStatus.USER_CONFIRMED
            this?.gateStatus == GuidedWorkflowGateStatus.REVIEW_REQUIRED -> EvidenceGateStatus.REVIEW
            this?.gateStatus == GuidedWorkflowGateStatus.INVALID ||
                this?.gateStatus == GuidedWorkflowGateStatus.BLOCKED -> EvidenceGateStatus.INVALID
            else -> EvidenceGateStatus.MISSING
        }

    private fun UserConfirmedCalibration?.toCalibrationEvidenceGate(axis: CalibrationAxis): EvidenceGateStatus =
        when {
            this == null -> EvidenceGateStatus.MISSING
            confirmationStatus != UserConfirmationStatus.CONFIRMED -> EvidenceGateStatus.MISSING
            calibrationSet.acceptedAnchors(axis).size < UserCalibrationSet.MIN_ANCHORS_PER_AXIS ->
                EvidenceGateStatus.INVALID
            gateStatus == GuidedWorkflowGateStatus.REVIEW_REQUIRED ||
                !calibrationSet.isRobustFitReady() -> EvidenceGateStatus.REVIEW
            gateStatus.isReleaseSatisfying() -> EvidenceGateStatus.USER_CONFIRMED
            gateStatus == GuidedWorkflowGateStatus.INVALID ||
                gateStatus == GuidedWorkflowGateStatus.BLOCKED -> EvidenceGateStatus.INVALID
            else -> EvidenceGateStatus.MISSING
        }

    private fun UserConfirmedTrace?.toEvidenceGate(): EvidenceGateStatus =
        when {
            this == null -> EvidenceGateStatus.MISSING
            confirmationStatus == UserConfirmationStatus.CONFIRMED &&
                gateStatus == TraceGateStatus.USER_CONFIRMED -> EvidenceGateStatus.USER_CONFIRMED
            gateStatus == TraceGateStatus.REVIEW_REQUIRED -> EvidenceGateStatus.REVIEW
            gateStatus == TraceGateStatus.INVALID -> EvidenceGateStatus.INVALID
            else -> EvidenceGateStatus.MISSING
        }

    private fun UserConfirmedPeakSet?.toEvidenceGate(): EvidenceGateStatus =
        when {
            this == null -> EvidenceGateStatus.MISSING
            gateStatus == PeakReviewGateStatus.USER_CONFIRMED -> EvidenceGateStatus.USER_CONFIRMED
            gateStatus == PeakReviewGateStatus.REVIEW_REQUIRED -> EvidenceGateStatus.REVIEW
            gateStatus == PeakReviewGateStatus.INVALID -> EvidenceGateStatus.INVALID
            else -> EvidenceGateStatus.MISSING
        }

    private fun GuidedDigitizationState.userConfirmationGate(): EvidenceGateStatus =
        if (
            graphPanelConfirmation.toEvidenceGate() == EvidenceGateStatus.USER_CONFIRMED &&
            plotAreaConfirmation.toEvidenceGate() == EvidenceGateStatus.USER_CONFIRMED &&
            calibration.toCalibrationEvidenceGate(CalibrationAxis.X) in userSatisfiedOrReview &&
            calibration.toCalibrationEvidenceGate(CalibrationAxis.Y) in userSatisfiedOrReview &&
            trace.toEvidenceGate() in userSatisfiedOrReview
        ) {
            EvidenceGateStatus.USER_CONFIRMED
        } else {
            EvidenceGateStatus.MISSING
        }

    private fun GuidedWorkflowGateStatus.isReleaseSatisfying(): Boolean =
        this == GuidedWorkflowGateStatus.SATISFIED ||
            this == GuidedWorkflowGateStatus.USER_CONFIRMED

    private fun GateEvidence.phase1BlockingReasons(): List<String> {
        val reasons = mutableListOf<String>()
        val required = listOf(
            "graph_panel" to graphPanelStatus,
            "plot_area" to plotAreaStatus,
            "x_calibration" to xCalibrationStatus,
            "y_calibration" to yCalibrationStatus,
            "trace" to traceStatus,
            "evidence_package" to evidencePackageStatus,
            "source_provenance" to sourceProvenanceStatus,
        )
        required.forEach { (name, status) ->
            when (status) {
                EvidenceGateStatus.INVALID -> reasons += "$name.invalid"
                EvidenceGateStatus.MISSING -> reasons += "$name.missing"
                else -> Unit
            }
        }
        return reasons
    }

    private fun GateEvidence.phase1ReviewReasons(): List<String> =
        listOf(
            "graph_panel" to graphPanelStatus,
            "plot_area" to plotAreaStatus,
            "x_calibration" to xCalibrationStatus,
            "y_calibration" to yCalibrationStatus,
            "trace" to traceStatus,
            "peak_review" to peakReviewStatus,
        ).mapNotNull { (name, status) ->
            if (status == EvidenceGateStatus.REVIEW) "$name.review" else null
        }

    private val userSatisfiedOrReview = setOf(
        EvidenceGateStatus.USER_CONFIRMED,
        EvidenceGateStatus.REVIEW,
    )
}

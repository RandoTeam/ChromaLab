package com.chromalab.feature.processing.peaks

import kotlinx.serialization.Serializable

@Serializable
enum class RecoveredPeakSource {
    LABEL_EVIDENCE,
}

@Serializable
enum class RecoveredPeakCandidateStatus {
    ACCEPTED,
    REVIEW,
    REJECTED,
}

@Serializable
enum class RecoveredPeakCandidateFlag {
    LOW_RESOLUTION_RECOVERED,
    LABEL_EVIDENCE_VERIFIED,
    RUNTIME_OCR_VERIFIED,
    FIXTURE_HINT_ONLY,
    DUPLICATE_REJECTED,
    FLAT_SIGNAL_REJECTED,
    OUTSIDE_PLOT_REJECTED,
    CALIBRATION_INVALID_REJECTED,
    OCR_CONFIDENCE_REJECTED,
    TEXT_MASK_OVERLAP_REJECTED,
    FRAME_GRID_ARTIFACT_REJECTED,
}

@Serializable
data class RecoveredPeakIntegrationWindow(
    val startRt: Double,
    val endRt: Double,
)

@Serializable
data class RecoveredPeakCandidate(
    val source: RecoveredPeakSource = RecoveredPeakSource.LABEL_EVIDENCE,
    val sourceEvidenceId: String,
    val labelRt: Double,
    val nearestLocalMaximumRt: Double? = null,
    val rtDelta: Double? = null,
    val localHeight: Double? = null,
    val localSNR: Double? = null,
    val localProminence: Double? = null,
    val localCurvatureScore: Double? = null,
    val localBaseline: Double? = null,
    val widthEstimate: Double? = null,
    val integrationWindow: RecoveredPeakIntegrationWindow? = null,
    val sourceEvidence: PeakLabelEvidence? = null,
    val status: RecoveredPeakCandidateStatus = RecoveredPeakCandidateStatus.REJECTED,
    val flags: List<RecoveredPeakCandidateFlag> = emptyList(),
    val rejectionReason: String? = null,
) {
    val isProductionEvidence: Boolean
        get() = sourceEvidence?.isRuntimeEvidence == true &&
            sourceEvidence.source != PeakLabelEvidenceSource.FIXTURE_HINT

    val isProductionReportable: Boolean
        get() = isProductionEvidence &&
            status != RecoveredPeakCandidateStatus.REJECTED &&
            RecoveredPeakCandidateFlag.LABEL_EVIDENCE_VERIFIED in flags
}

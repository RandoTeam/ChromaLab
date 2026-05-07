package com.chromalab.feature.calculation.core

import kotlinx.serialization.Serializable

/**
 * Input signal for the calculation pipeline.
 *
 * Uses Double precision for all numeric calculations.
 * This is the Phase 2 entry point — accepts data from Phase 1's DigitalSignal.
 */
@Serializable
data class CalculationSignal(
    val points: List<SignalPoint>,
    val timeUnit: String,
    val intensityUnit: String,
    val sourceId: String,
)

/**
 * A single signal point with Double precision.
 */
@Serializable
data class SignalPoint(
    val index: Int,
    val time: Double,
    val intensity: Double,
    val pixelX: Int,
    val pixelY: Double,
    val confidence: Double,
    val isInterpolated: Boolean,
)

/**
 * Result of input validation.
 */
@Serializable
data class ValidationResult(
    val isValid: Boolean,
    val pointCount: Int,
    val isSorted: Boolean,
    val duplicateTimeCount: Int,
    val gapCount: Int,
    val nanCount: Int,
    val infinityCount: Int,
    val negativeIntensityCount: Int,
    val isUniformSpacing: Boolean,
    val avgTimeStep: Double,
    val maxTimeStepDeviation: Double,
    val warnings: List<String>,
)

/**
 * Signal model — all derived signals stored separately.
 * Raw signal is NEVER modified.
 */
@Serializable
data class SignalBundle(
    val raw: List<SignalPoint>,
    val smoothed: List<SignalPoint>?,
    val baseline: List<Double>?,
    val baselineCorrected: List<SignalPoint>?,
    val signalUsedForDetection: SignalSource,
    val signalUsedForIntegration: SignalSource,
)

enum class SignalSource {
    RAW,
    SMOOTHED,
    BASELINE_CORRECTED,
    SMOOTHED_BASELINE_CORRECTED,
}

/**
 * A single calculation run — immutable record.
 * New params/edits = new CalculationRun.
 */
@Serializable
data class CalculationRun(
    val id: String,
    val sourceSignalId: String,
    val pipelineVersion: String,
    val algorithmVersion: String,
    val params: CalculationParams,
    val validation: ValidationResult,
    val signals: SignalBundle,
    val peaks: List<PeakResult>,
    val warnings: List<CalculationWarning>,
    val manualEdits: List<ManualEdit>,
    val timestamp: Long,
)

/**
 * All algorithm parameters for reproducibility.
 */
@Serializable
data class CalculationParams(
    val smoothingEnabled: Boolean = true,
    val smoothingWindowSize: Int = 7,
    val smoothingPolynomialOrder: Int = 2,
    val baselineMethod: String = "ALS",
    val baselineLambda: Double = 1e6,
    val baselineP: Double = 0.01,
    val baselineIterations: Int = 10,
    val minPeakHeight: Double = 0.0,
    val minPeakProminence: Double = 0.0,
    val minPeakDistance: Int = 5,
    val minPeakWidth: Int = 3,
    val minSnr: Double = 3.0,
    val noiseMethod: String = "peak_to_peak",
    val integrationMethod: String = "trapezoidal",
    val useSmoothedForIntegration: Boolean = false,
    val presetName: String = "Balanced",
)

/**
 * Result for a single detected peak.
 */
@Serializable
data class PeakResult(
    val peakId: Int,
    val status: PeakStatus,
    val rtApex: Double,
    val rtCentroid: Double?,
    val height: Double,
    val area: Double,
    val widthBase: Double,
    val widthHalfHeight: Double?,
    val prominence: Double,
    val snr: Double,
    val snrMethod: String,
    val baselineMethod: String,
    val integrationMethod: String,
    val confidence: PeakConfidence,
    val overlapStatus: OverlapStatus,
    val leftBoundaryTime: Double,
    val rightBoundaryTime: Double,
    val boundaryMethod: String,
    val warnings: List<String>,
)

enum class PeakStatus {
    AUTO,
    MANUAL,
    CORRECTED,
    REJECTED,
    LOW_CONFIDENCE,
}

enum class PeakConfidence {
    HIGH,
    MEDIUM,
    LOW,
    FAILED,
}

enum class OverlapStatus {
    ISOLATED,
    PARTIALLY_OVERLAPPED,
    SHOULDER,
    UNRESOLVED,
}

/**
 * Warning with severity.
 */
@Serializable
data class CalculationWarning(
    val message: String,
    val severity: WarningSeverity,
    val peakId: Int? = null,
    val stage: String,
)

enum class WarningSeverity {
    INFO,
    CAUTION,
    SERIOUS,
    FAILED,
}

/**
 * Manual edit record for audit trail.
 */
@Serializable
data class ManualEdit(
    val editId: Int,
    val peakId: Int?,
    val editType: EditType,
    val oldValue: String?,
    val newValue: String?,
    val reason: String?,
    val timestamp: Long,
)

enum class EditType {
    BOUNDARY_LEFT,
    BOUNDARY_RIGHT,
    ADD_PEAK,
    REJECT_PEAK,
    RESTORE_PEAK,
    CHANGE_APEX,
    CHANGE_NOISE_REGION,
    CHANGE_BASELINE_METHOD,
}

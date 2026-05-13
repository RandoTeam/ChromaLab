package com.chromalab.feature.knowledge

import kotlin.math.abs
import kotlin.math.ln

data class NParaffinReferenceRetention(
    val carbonNumber: Int,
    val retentionTime: Double,
)

data class KovatsIndexCalculationInput(
    val targetRetentionTime: Double,
    val referenceRetentions: List<NParaffinReferenceRetention>,
    val formula: KovatsIndexFormula = KovatsIndexFormula.VAN_DEN_DOOL_KRATZ_LINEAR,
)

data class KovatsIndexCalculationResult(
    val status: KovatsIndexCalculationStatus,
    val kind: KovatsIndexCalculationKind = KovatsIndexCalculationKind.NOT_CALCULABLE,
    val value: Double? = null,
    val lowerReference: NParaffinReferenceRetention? = null,
    val upperReference: NParaffinReferenceRetention? = null,
    val message: String? = null,
) {
    val isCalculated: Boolean = status == KovatsIndexCalculationStatus.CALCULATED && value != null
}

enum class KovatsIndexFormula {
    VAN_DEN_DOOL_KRATZ_LINEAR,
    KOVATS_ISOTHERMAL_LOG,
}

enum class KovatsIndexCalculationStatus {
    CALCULATED,
    INSUFFICIENT_REFERENCE_POINTS,
    INVALID_TARGET_RETENTION_TIME,
    INVALID_REFERENCE_SERIES,
    TARGET_OUTSIDE_REFERENCE_RANGE,
    MISSING_ADJACENT_REFERENCE,
}

enum class KovatsIndexCalculationKind {
    DIRECT_REFERENCE,
    INTERPOLATED,
    NOT_CALCULABLE,
}

object KovatsIndexCalculator {

    fun calculate(input: KovatsIndexCalculationInput): KovatsIndexCalculationResult {
        val target = input.targetRetentionTime
        if (!target.isUsablePositive()) {
            return notCalculable(
                KovatsIndexCalculationStatus.INVALID_TARGET_RETENTION_TIME,
                "Target retention time must be positive and finite.",
            )
        }

        val references = input.referenceRetentions.sortedBy { it.carbonNumber }
        val validationError = validateReferences(references)
        if (validationError != null) {
            return validationError
        }

        references.firstOrNull { it.retentionTime.nearlyEquals(target) }?.let { reference ->
            return KovatsIndexCalculationResult(
                status = KovatsIndexCalculationStatus.CALCULATED,
                kind = KovatsIndexCalculationKind.DIRECT_REFERENCE,
                value = reference.carbonNumber * 100.0,
                lowerReference = reference,
                upperReference = reference,
            )
        }

        val lower = references.lastOrNull { it.retentionTime < target }
        val upper = references.firstOrNull { it.retentionTime > target }
        if (lower == null || upper == null) {
            return notCalculable(
                KovatsIndexCalculationStatus.TARGET_OUTSIDE_REFERENCE_RANGE,
                "Target retention time is outside the supplied n-paraffin reference range.",
            )
        }
        if (upper.carbonNumber != lower.carbonNumber + 1) {
            return notCalculable(
                KovatsIndexCalculationStatus.MISSING_ADJACENT_REFERENCE,
                "The bracketing n-paraffins must have adjacent carbon numbers.",
                lower,
                upper,
            )
        }

        val value = when (input.formula) {
            KovatsIndexFormula.VAN_DEN_DOOL_KRATZ_LINEAR -> {
                100.0 * lower.carbonNumber +
                    100.0 * (target - lower.retentionTime) / (upper.retentionTime - lower.retentionTime)
            }
            KovatsIndexFormula.KOVATS_ISOTHERMAL_LOG -> {
                100.0 * lower.carbonNumber +
                    100.0 * (ln(target) - ln(lower.retentionTime)) /
                    (ln(upper.retentionTime) - ln(lower.retentionTime))
            }
        }

        return KovatsIndexCalculationResult(
            status = KovatsIndexCalculationStatus.CALCULATED,
            kind = KovatsIndexCalculationKind.INTERPOLATED,
            value = value,
            lowerReference = lower,
            upperReference = upper,
        )
    }

    private fun validateReferences(
        references: List<NParaffinReferenceRetention>,
    ): KovatsIndexCalculationResult? {
        if (references.size < 2) {
            return notCalculable(
                KovatsIndexCalculationStatus.INSUFFICIENT_REFERENCE_POINTS,
                "At least two n-paraffin reference retention times are required.",
            )
        }
        if (references.any { it.carbonNumber <= 0 || !it.retentionTime.isUsablePositive() }) {
            return notCalculable(
                KovatsIndexCalculationStatus.INVALID_REFERENCE_SERIES,
                "Reference carbon numbers and retention times must be positive and finite.",
            )
        }
        references.zipWithNext().forEach { (previous, next) ->
            if (next.carbonNumber <= previous.carbonNumber || next.retentionTime <= previous.retentionTime) {
                return notCalculable(
                    KovatsIndexCalculationStatus.INVALID_REFERENCE_SERIES,
                    "n-Paraffin references must increase by carbon number and retention time.",
                    previous,
                    next,
                )
            }
        }
        return null
    }

    private fun notCalculable(
        status: KovatsIndexCalculationStatus,
        message: String,
        lower: NParaffinReferenceRetention? = null,
        upper: NParaffinReferenceRetention? = null,
    ): KovatsIndexCalculationResult =
        KovatsIndexCalculationResult(
            status = status,
            lowerReference = lower,
            upperReference = upper,
            message = message,
        )

    private fun Double.isUsablePositive(): Boolean =
        isFinite() && this > 0.0

    private fun Double.nearlyEquals(other: Double): Boolean =
        abs(this - other) <= 1e-9
}

package com.chromalab.feature.processing.curve

import kotlinx.serialization.Serializable

/**
 * A single manual edit action applied to the curve.
 */
@Serializable
data class CurveEdit(
    val type: EditType,
    /** Affected X range (pixel coordinates) */
    val fromX: Int,
    val toX: Int,
    /** New Y value (for MOVE edits) */
    val newY: Float? = null,
    val timestamp: Long,
) {
    @Serializable
    enum class EditType {
        /** Points in range were deleted */
        DELETE,
        /** Points in range were marked unreliable */
        MARK_UNRELIABLE,
        /** Points were moved/corrected by dragging */
        MOVE,
    }
}

/**
 * Complete manual edit log for reproducibility.
 * Every manual change is recorded so the result can be audited.
 */
@Serializable
data class ManualEditLog(
    val edits: List<CurveEdit>,
    val originalPointCount: Int,
    val finalPointCount: Int,
    val timestamp: Long,
)

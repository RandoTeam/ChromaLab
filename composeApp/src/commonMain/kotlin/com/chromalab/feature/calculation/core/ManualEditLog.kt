package com.chromalab.feature.calculation.core

/**
 * Manual peak correction system (§2.25).
 *
 * Records all manual edits to peaks as an immutable audit log.
 * After each edit, metrics must be recalculated.
 *
 * Design:
 * - Every edit creates a ManualEdit entry with old/new values
 * - Edits are append-only — never mutated or deleted
 * - Undo = add a reverse edit (restore original value)
 * - Full traceability: timestamp, type, reason, who changed
 */

// ─── Edit types ─────────────────────────────────────────────────

enum class EditType(val label: String) {
    BOUNDARY_LEFT("Левая граница"),
    BOUNDARY_RIGHT("Правая граница"),
    APEX_MOVED("Apex перемещён"),
    PEAK_ADDED("Пик добавлен вручную"),
    PEAK_REMOVED("Пик удалён"),
    PEAK_RESTORED("Пик восстановлен"),
    NOISE_REGION("Noise region изменён"),
    BASELINE_METHOD("Baseline метод изменён"),
    PEAK_ACCEPTED("Пик принят"),
    PEAK_REJECTED("Пик отклонён"),
}

// ─── Manual edit entry ──────────────────────────────────────────

data class ManualEdit(
    val peakIndex: Int,
    val editType: EditType,
    val oldValue: String,
    val newValue: String,
    val timestamp: Long,
    val reason: String = "",
) {
    companion object {
        fun boundaryLeft(peakIndex: Int, oldTime: Double, newTime: Double, reason: String = "") =
            ManualEdit(peakIndex, EditType.BOUNDARY_LEFT,
                "%.4f".format(oldTime), "%.4f".format(newTime),
                nextTimestamp(), reason)

        fun boundaryRight(peakIndex: Int, oldTime: Double, newTime: Double, reason: String = "") =
            ManualEdit(peakIndex, EditType.BOUNDARY_RIGHT,
                "%.4f".format(oldTime), "%.4f".format(newTime),
                nextTimestamp(), reason)

        fun apexMoved(peakIndex: Int, oldTime: Double, newTime: Double, reason: String = "") =
            ManualEdit(peakIndex, EditType.APEX_MOVED,
                "%.4f".format(oldTime), "%.4f".format(newTime),
                nextTimestamp(), reason)

        fun peakAdded(peakIndex: Int, apexTime: Double, reason: String = "") =
            ManualEdit(peakIndex, EditType.PEAK_ADDED,
                "", "%.4f".format(apexTime),
                nextTimestamp(), reason)

        fun peakRemoved(peakIndex: Int, apexTime: Double, reason: String = "") =
            ManualEdit(peakIndex, EditType.PEAK_REMOVED,
                "%.4f".format(apexTime), "",
                nextTimestamp(), reason)

        fun peakRestored(peakIndex: Int, apexTime: Double, reason: String = "") =
            ManualEdit(peakIndex, EditType.PEAK_RESTORED,
                "", "%.4f".format(apexTime),
                nextTimestamp(), reason)

        fun noiseRegion(peakIndex: Int, oldRegion: String, newRegion: String, reason: String = "") =
            ManualEdit(peakIndex, EditType.NOISE_REGION,
                oldRegion, newRegion,
                nextTimestamp(), reason)

        fun baselineMethod(peakIndex: Int, oldMethod: String, newMethod: String, reason: String = "") =
            ManualEdit(peakIndex, EditType.BASELINE_METHOD,
                oldMethod, newMethod,
                nextTimestamp(), reason)

        fun peakAccepted(peakIndex: Int, reason: String = "") =
            ManualEdit(peakIndex, EditType.PEAK_ACCEPTED,
                "pending", "accepted",
                nextTimestamp(), reason)

        fun peakRejected(peakIndex: Int, reason: String = "") =
            ManualEdit(peakIndex, EditType.PEAK_REJECTED,
                "pending", "rejected",
                nextTimestamp(), reason)

        /**
         * Monotonic edit counter — guaranteed unique ordering.
         * Actual wall-clock time injected at storage layer if needed.
         */
        private var editCounter = 0L
        private fun nextTimestamp(): Long = ++editCounter
    }
}

// ─── Edit log ───────────────────────────────────────────────────

/**
 * Immutable, append-only edit log.
 *
 * Thread-safe: each mutation returns a new instance.
 */
data class ManualEditLog(
    val edits: List<ManualEdit> = emptyList(),
) {
    /** Append an edit. Returns new log. */
    fun append(edit: ManualEdit): ManualEditLog =
        copy(edits = edits + edit)

    /** Get all edits for a specific peak. */
    fun forPeak(peakIndex: Int): List<ManualEdit> =
        edits.filter { it.peakIndex == peakIndex }

    /** Check if a peak has been manually edited. */
    fun isEdited(peakIndex: Int): Boolean =
        edits.any { it.peakIndex == peakIndex }

    /** Count total edits. */
    val totalEdits: Int get() = edits.size

    /** Get unique edited peak indices. */
    val editedPeaks: Set<Int> get() = edits.map { it.peakIndex }.toSet()

    /** Check if a peak was rejected. */
    fun isRejected(peakIndex: Int): Boolean =
        forPeak(peakIndex).lastOrNull {
            it.editType == EditType.PEAK_REJECTED || it.editType == EditType.PEAK_ACCEPTED
        }?.editType == EditType.PEAK_REJECTED

    /** Check if a peak was manually added. */
    fun isManuallyAdded(peakIndex: Int): Boolean =
        forPeak(peakIndex).any { it.editType == EditType.PEAK_ADDED }

    /** Export log as CSV for audit trail. */
    fun toCsv(): String {
        val header = "PeakIndex,EditType,OldValue,NewValue,Timestamp,Reason"
        val lines = edits.map { e ->
            listOf(
                e.peakIndex,
                e.editType.name,
                "\"${e.oldValue}\"",
                "\"${e.newValue}\"",
                e.timestamp,
                "\"${e.reason}\"",
            ).joinToString(",")
        }
        return (listOf(header) + lines).joinToString("\n")
    }
}

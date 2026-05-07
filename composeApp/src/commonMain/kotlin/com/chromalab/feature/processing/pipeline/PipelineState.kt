package com.chromalab.feature.processing.pipeline

import kotlinx.serialization.Serializable

/**
 * State of a single digitization session.
 * Tracks current stage, validation level, and all intermediate results.
 *
 * Key principle: ALL photo-based results are ESTIMATED by default.
 * No expert conclusions are made at this stage.
 * No substance identification is performed.
 */
@Serializable
data class PipelineState(
    val pipelineVersion: String = PIPELINE_VERSION,
    val currentStage: PipelineStage = PipelineStage.CAPTURE,
    val validationLevel: ValidationLevel = ValidationLevel.ESTIMATED,
    val warnings: List<String> = emptyList(),
    val startedAt: Long = 0,
    val completedAt: Long? = null,
) {
    val isComplete: Boolean get() = currentStage == PipelineStage.COMPLETE
    val isEstimated: Boolean get() = validationLevel == ValidationLevel.ESTIMATED

    fun advance(to: PipelineStage): PipelineState =
        copy(currentStage = to)

    fun addWarning(warning: String): PipelineState =
        copy(warnings = warnings + warning)
}

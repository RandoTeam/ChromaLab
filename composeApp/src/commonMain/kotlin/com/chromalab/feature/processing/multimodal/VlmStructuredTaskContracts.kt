package com.chromalab.feature.processing.multimodal

import kotlinx.serialization.Serializable

@Serializable
data class VlmStructuredTaskContract(
    val taskType: StageJudgeTaskType,
    val promptId: String,
    val allowedOutputFields: Set<String>,
    val requiredOutputFields: Set<String>,
    val forbiddenNumericFields: Set<ForbiddenVlmNumericField> = ForbiddenVlmBoundaryPolicy.forbiddenNumericFields,
    val timeoutMillis: Long,
    val fallbackBehavior: String,
)

@Serializable
data class VlmJsonBoundaryValidation(
    val accepted: Boolean,
    val rejectedForbiddenFields: Set<ForbiddenVlmNumericField> = emptySet(),
    val missingRequiredFields: Set<String> = emptySet(),
    val unknownFields: Set<String> = emptySet(),
)

object VlmStructuredTaskContracts {
    private val knowledgeFields = setOf(
        "used_entry_ids",
        "decision",
        "unsupported_claims",
        "explanation",
    )

    val all: List<VlmStructuredTaskContract> = listOf(
        VlmStructuredTaskContract(
            taskType = StageJudgeTaskType.OCR_CROP_READ,
            promptId = "vlm_ocr_crop_read_v1",
            allowedOutputFields = setOf("text", "raw_text", "normalized_text", "text_type", "confidence", "warnings") + knowledgeFields,
            requiredOutputFields = setOf("text_type", "confidence"),
            timeoutMillis = 6_000L,
            fallbackBehavior = "Record REVIEW or TIMEOUT and keep deterministic/OCR path authoritative.",
        ),
        VlmStructuredTaskContract(
            taskType = StageJudgeTaskType.TEXT_REGION_CLASSIFY,
            promptId = "vlm_text_region_classify_v1",
            allowedOutputFields = setOf("text_type", "confidence", "warnings") + knowledgeFields,
            requiredOutputFields = setOf("text_type", "confidence"),
            timeoutMillis = 6_000L,
            fallbackBehavior = "Use local text heuristics and mark semantic classification REVIEW.",
        ),
        VlmStructuredTaskContract(
            taskType = StageJudgeTaskType.GRAPH_PANEL_CANDIDATE_JUDGE,
            promptId = "vlm_graph_panel_candidate_judge_v1",
            allowedOutputFields = setOf("verdict", "confidence", "warnings", "retry_recommendations") + knowledgeFields,
            requiredOutputFields = setOf("verdict", "confidence"),
            timeoutMillis = 8_000L,
            fallbackBehavior = "Proceed with deterministic candidate if geometry gates pass; otherwise REVIEW.",
        ),
        VlmStructuredTaskContract(
            taskType = StageJudgeTaskType.PLOT_AREA_CANDIDATE_JUDGE,
            promptId = "vlm_plot_area_candidate_judge_v1",
            allowedOutputFields = setOf("verdict", "confidence", "warnings", "retry_recommendations") + knowledgeFields,
            requiredOutputFields = setOf("verdict", "confidence"),
            timeoutMillis = 8_000L,
            fallbackBehavior = "Proceed with deterministic plotArea if axis/trace gates pass; otherwise REVIEW.",
        ),
        VlmStructuredTaskContract(
            taskType = StageJudgeTaskType.AXIS_TICK_VISIBILITY_JUDGE,
            promptId = "vlm_axis_tick_visibility_judge_v1",
            allowedOutputFields = setOf("verdict", "confidence", "warnings", "retry_recommendations") + knowledgeFields,
            requiredOutputFields = setOf("verdict", "confidence"),
            timeoutMillis = 8_000L,
            fallbackBehavior = "Use deterministic tick positions and OCR crop labels only.",
        ),
        VlmStructuredTaskContract(
            taskType = StageJudgeTaskType.TRACE_OVERLAY_JUDGE,
            promptId = "vlm_trace_overlay_judge_v1",
            allowedOutputFields = setOf("verdict", "confidence", "warnings", "retry_recommendations") + knowledgeFields,
            requiredOutputFields = setOf("verdict", "confidence"),
            timeoutMillis = 8_000L,
            fallbackBehavior = "Use trace quality metrics and mark trace REVIEW if uncertain.",
        ),
        VlmStructuredTaskContract(
            taskType = StageJudgeTaskType.PEAK_EVIDENCE_JUDGE,
            promptId = "vlm_peak_evidence_judge_v1",
            allowedOutputFields = setOf("verdict", "confidence", "warnings", "retry_recommendations") + knowledgeFields,
            requiredOutputFields = setOf("verdict", "confidence"),
            timeoutMillis = 8_000L,
            fallbackBehavior = "Use deterministic peak evidence gates; VLM cannot create or measure peaks.",
        ),
        VlmStructuredTaskContract(
            taskType = StageJudgeTaskType.REPORT_WARNING_SUMMARY,
            promptId = "vlm_report_warning_summary_v1",
            allowedOutputFields = setOf("summary", "warnings", "confidence") + knowledgeFields,
            requiredOutputFields = setOf("summary", "used_entry_ids"),
            timeoutMillis = 8_000L,
            fallbackBehavior = "Show deterministic warning list without model explanation.",
        ),
    )

    fun contractFor(taskType: StageJudgeTaskType): VlmStructuredTaskContract =
        all.first { it.taskType == taskType }
}

object ForbiddenVlmBoundaryPolicy {
    val forbiddenNumericFields: Set<ForbiddenVlmNumericField> = ForbiddenVlmNumericField.entries.toSet()

    private val fieldAliases: Map<ForbiddenVlmNumericField, Set<String>> = mapOf(
        ForbiddenVlmNumericField.RT to setOf("rt", "retention_time", "retentionTime", "peak_rt", "peakRt"),
        ForbiddenVlmNumericField.HEIGHT to setOf("height", "peak_height", "peakHeight"),
        ForbiddenVlmNumericField.AREA to setOf("area", "peak_area", "peakArea"),
        ForbiddenVlmNumericField.FWHM to setOf("fwhm", "width_half_height", "widthHalfHeight"),
        ForbiddenVlmNumericField.SIGNAL_TO_NOISE to setOf("snr", "signal_to_noise", "signalToNoise"),
        ForbiddenVlmNumericField.BASELINE to setOf("baseline", "baseline_at_apex", "baselineAtApex"),
        ForbiddenVlmNumericField.KOVATS to setOf("kovats", "retention_index", "retentionIndex"),
        ForbiddenVlmNumericField.EXACT_PIXEL_GEOMETRY to setOf("x", "y", "pixel_x", "pixel_y", "pixelX", "pixelY", "coordinates", "bounds"),
        ForbiddenVlmNumericField.CALIBRATION_COEFFICIENT to setOf("slope", "intercept", "calibration", "calibration_coefficients"),
    )

    fun validateRawJsonFields(
        rawJson: String,
        contract: VlmStructuredTaskContract,
    ): VlmJsonBoundaryValidation {
        val fields = extractJsonFieldNames(rawJson)
        val rejectedForbidden = fieldAliases
            .filterValues { aliases -> aliases.any { alias -> alias in fields } }
            .keys
            .toSet()
        val missing = contract.requiredOutputFields.filterNot { it in fields }.toSet()
        val unknown = fields - contract.allowedOutputFields - fieldAliases.values.flatten().toSet()
        return VlmJsonBoundaryValidation(
            accepted = rejectedForbidden.isEmpty() && missing.isEmpty(),
            rejectedForbiddenFields = rejectedForbidden,
            missingRequiredFields = missing,
            unknownFields = unknown,
        )
    }

    private fun extractJsonFieldNames(rawJson: String): Set<String> =
        Regex(""""([^"]+)"\s*:""")
            .findAll(rawJson)
            .map { it.groupValues[1] }
            .toSet()
}

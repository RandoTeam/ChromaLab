package com.chromalab.feature.processing.multimodal

import kotlinx.serialization.Serializable

@Serializable
enum class StageJudgeTaskType {
    GRAPH_PANEL_CANDIDATE_JUDGE,
    PLOT_AREA_CANDIDATE_JUDGE,
    AXIS_TICK_VISIBILITY_JUDGE,
    OCR_CROP_READ,
    TEXT_REGION_CLASSIFY,
    TRACE_OVERLAY_JUDGE,
    PEAK_EVIDENCE_JUDGE,
    REPORT_WARNING_SUMMARY,
}

@Serializable
enum class StageJudgeSource {
    CV,
    ML_KIT,
    VLM,
    OCR,
    BOTH,
    DETERMINISTIC,
    SYSTEM,
}

@Serializable
enum class StageJudgeVerdict {
    PASS,
    REVIEW,
    FAIL,
    INCONCLUSIVE,
    TIMEOUT,
    MODEL_UNAVAILABLE,
}

@Serializable
enum class StageJudgeWarningSeverity {
    INFO,
    WARNING,
    BLOCKING,
}

@Serializable
data class StageJudgeConfidence(
    val value: Float = 0f,
    val label: String? = null,
    val calibration: String? = null,
) {
    init {
        require(value in 0f..1f) { "StageJudgeConfidence.value must be in 0..1" }
    }
}

@Serializable
data class StageJudgeWarning(
    val code: String,
    val message: String,
    val severity: StageJudgeWarningSeverity = StageJudgeWarningSeverity.WARNING,
)

@Serializable
enum class StageRetryAction {
    EXPAND_GRAPH_PANEL_LEFT,
    EXPAND_GRAPH_PANEL_RIGHT,
    EXPAND_GRAPH_PANEL_UP,
    EXPAND_GRAPH_PANEL_DOWN,
    EXPAND_PLOT_AREA,
    RETRY_OCR_LARGER_CROP,
    RETRY_OCR_CONTRAST_ENHANCED_CROP,
    RUN_VLM_LOCAL_CROP_OCR,
    MARK_TITLE_CHANNEL_NON_PEAK,
    REQUEST_ASSISTED_REVIEW,
    CREATE_FINAL_METRIC,
    CREATE_PEAK_FROM_TEXT,
    ACCEPT_INVALID_CALIBRATION,
    OVERRIDE_DETERMINISTIC_RESIDUALS,
}

@Serializable
data class StageRetryRecommendation(
    val action: StageRetryAction,
    val reason: String,
    val targetId: String? = null,
)

@Serializable
enum class ForbiddenVlmNumericField {
    RT,
    HEIGHT,
    AREA,
    FWHM,
    SIGNAL_TO_NOISE,
    BASELINE,
    KOVATS,
    EXACT_PIXEL_GEOMETRY,
    CALIBRATION_COEFFICIENT,
}

@Serializable
enum class MultimodalTextRegionClass {
    PEAK_ANNOTATION,
    TICK_LABEL,
    AXIS_LABEL,
    TITLE_OR_CHANNEL,
    PAGE_TEXT,
    UNKNOWN_TEXT,
}

@Serializable
data class VlmOcrCropResult(
    val resultId: String,
    val taskId: String,
    val source: StageJudgeSource,
    val localCropPath: String? = null,
    val rawText: String = "",
    val normalizedText: String = rawText.trim(),
    val parsedText: String? = null,
    val textClass: MultimodalTextRegionClass = MultimodalTextRegionClass.UNKNOWN_TEXT,
    val confidence: StageJudgeConfidence = StageJudgeConfidence(),
    val durationMillis: Long? = null,
    val runtimeProfileId: String? = null,
    val rejectedForbiddenFields: List<ForbiddenVlmNumericField> = emptyList(),
    val acceptedNumericFields: List<ForbiddenVlmNumericField> = emptyList(),
    val warnings: List<StageJudgeWarning> = emptyList(),
)

@Serializable
data class OcrVlmDisagreement(
    val disagreementId: String,
    val mlKitResultId: String? = null,
    val vlmResultId: String? = null,
    val disagreementType: String,
    val verdict: StageJudgeVerdict = StageJudgeVerdict.REVIEW,
    val resolutionReason: String? = null,
)

@Serializable
data class OverlayJudgeResult(
    val resultId: String,
    val taskId: String,
    val overlayImagePath: String,
    val verdict: StageJudgeVerdict,
    val confidence: StageJudgeConfidence = StageJudgeConfidence(),
    val warnings: List<StageJudgeWarning> = emptyList(),
)

@Serializable
data class ModelRuntimeProfile(
    val profileId: String,
    val taskId: String,
    val modelId: String,
    val runtimeBackend: String,
    val inputImageWidth: Int? = null,
    val inputImageHeight: Int? = null,
    val cropWidth: Int? = null,
    val cropHeight: Int? = null,
    val durationMillis: Long? = null,
    val timeoutMillis: Long? = null,
    val timedOut: Boolean = false,
    val success: Boolean = false,
    val cacheHit: Boolean = false,
    val memoryBeforeMb: Long? = null,
    val memoryAfterMb: Long? = null,
    val thermalWarning: String? = null,
    val errorCode: String? = null,
)

@Serializable
data class AutonomousStageJudgeResult(
    val taskId: String,
    val graphIndex: Int? = null,
    val taskType: StageJudgeTaskType,
    val source: StageJudgeSource,
    val verdict: StageJudgeVerdict,
    val confidence: StageJudgeConfidence = StageJudgeConfidence(),
    val cropPath: String? = null,
    val overlayPath: String? = null,
    val linkedEvidenceIds: List<String> = emptyList(),
    val ocrCropResultIds: List<String> = emptyList(),
    val overlayJudgeResultIds: List<String> = emptyList(),
    val modelRuntimeProfileId: String? = null,
    val retryRecommendations: List<StageRetryRecommendation> = emptyList(),
    val rejectedForbiddenFields: List<ForbiddenVlmNumericField> = emptyList(),
    val acceptedNumericFields: List<ForbiddenVlmNumericField> = emptyList(),
    val warnings: List<StageJudgeWarning> = emptyList(),
)

object StageRetryPolicy {
    private val forbiddenActions = setOf(
        StageRetryAction.CREATE_FINAL_METRIC,
        StageRetryAction.CREATE_PEAK_FROM_TEXT,
        StageRetryAction.ACCEPT_INVALID_CALIBRATION,
        StageRetryAction.OVERRIDE_DETERMINISTIC_RESIDUALS,
    )

    fun isAllowed(recommendation: StageRetryRecommendation): Boolean =
        recommendation.action !in forbiddenActions
}


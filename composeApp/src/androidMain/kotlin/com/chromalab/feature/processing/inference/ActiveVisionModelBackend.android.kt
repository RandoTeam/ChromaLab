package com.chromalab.feature.processing.inference

import android.util.Log
import com.chromalab.feature.processing.multimodal.ForbiddenVlmBoundaryPolicy
import com.chromalab.feature.processing.multimodal.ModelRuntimeProfile
import com.chromalab.feature.processing.multimodal.StageJudgeTaskType
import com.chromalab.feature.processing.multimodal.VlmStructuredTaskContracts

private const val VISION_BACKEND_TAG = "ChromaLabVLM"

class ActiveVisionModelBackend : VisionModelBackend {
    override suspend fun readLocalTextCrop(
        cropImagePath: String,
        context: VisionLocalTextCropContext,
    ): VisionLocalTextCropResult? {
        val engine = VlmEngineHolder.activeEngine ?: return null
        if (!engine.isLoaded() || !engine.supportsImageInput()) return null
        val config = VlmEngineHolder.activeConfig
        val prompt = ChartPrompts.localTextCropPrompt(
            style = config?.promptStyle ?: PromptStyle.RAW,
            context = context,
        )
        val taskId = "vlm_local_crop:${cropImagePath.hashCode()}"
        val startedAt = System.currentTimeMillis()
        val timeoutMillis = VlmStructuredTaskContracts.contractFor(StageJudgeTaskType.OCR_CROP_READ).timeoutMillis
        return try {
            VlmEngineHolder.isInferring = true
            val raw = engine.inferRaw(
                imagePath = cropImagePath,
                prompt = prompt,
                options = GenerationOptions(
                    maxTokens = 128,
                    timeoutMs = timeoutMillis,
                    temperature = 0f,
                    topP = 1f,
                    topK = 0,
                    repeatPenalty = config?.repeatPenalty ?: 1.05f,
                    repeatLastN = config?.repeatLastN ?: 32,
                ),
            )
            val durationMillis = System.currentTimeMillis() - startedAt
            parseLocalTextCropResponse(
                raw = raw,
                runtimeProfile = runtimeProfile(
                    taskId = taskId,
                    durationMillis = durationMillis,
                    timeoutMillis = timeoutMillis,
                    success = true,
                    timedOut = false,
                ),
            )
        } catch (e: Exception) {
            val durationMillis = System.currentTimeMillis() - startedAt
            Log.w(VISION_BACKEND_TAG, "Local crop VLM OCR failed: ${e.message}", e)
            VisionLocalTextCropResult(
                rawText = "",
                textType = VisionTextRegionType.UNKNOWN_TEXT,
                confidence = 0f,
                warnings = listOf("peak_label_ocr.vlm_local_crop_failed:${e.javaClass.simpleName}"),
                runtimeProfile = runtimeProfile(
                    taskId = taskId,
                    durationMillis = durationMillis,
                    timeoutMillis = timeoutMillis,
                    success = false,
                    timedOut = durationMillis >= timeoutMillis,
                    errorCode = e.javaClass.simpleName,
                ),
            )
        } finally {
            VlmEngineHolder.isInferring = false
        }
    }

    override suspend fun classifyTextRegion(
        cropImagePath: String,
        graphContext: String?,
    ): VisionTextRegionType? =
        readLocalTextCrop(
            cropImagePath = cropImagePath,
            context = VisionLocalTextCropContext(
                cropKind = "classification_only",
                insidePlotArea = true,
                graphContext = graphContext,
            ),
        )?.textType

    override suspend fun judgeGraphOverlay(
        overlayImagePath: String,
        candidates: List<String>,
    ): VisionGraphOverlayJudgement? = null

    override suspend fun summarizeGeometryWarnings(warnings: List<String>): String? = null
}

private fun runtimeProfile(
    taskId: String,
    durationMillis: Long,
    timeoutMillis: Long,
    success: Boolean,
    timedOut: Boolean,
    errorCode: String? = null,
): ModelRuntimeProfile =
    ModelRuntimeProfile(
        profileId = "runtime:$taskId",
        taskId = taskId,
        modelId = VlmEngineHolder.executedModel?.modelId
            ?: VlmEngineHolder.selectedModel?.modelId
            ?: "active-vlm-model",
        runtimeBackend = VlmEngineHolder.executedModel?.backendLabel
            ?: VlmEngineHolder.activeEngine?.getBackendName()
            ?: "ACTIVE_VLM",
        durationMillis = durationMillis,
        timeoutMillis = timeoutMillis,
        timedOut = timedOut,
        success = success,
        cacheHit = false,
        errorCode = errorCode,
    )

private fun parseLocalTextCropResponse(
    raw: String,
    runtimeProfile: ModelRuntimeProfile,
): VisionLocalTextCropResult? {
    val contract = VlmStructuredTaskContracts.contractFor(StageJudgeTaskType.OCR_CROP_READ)
    val boundary = ForbiddenVlmBoundaryPolicy.validateRawJsonFields(raw, contract)
    val json = raw.substringAfter('{', missingDelimiterValue = raw)
        .substringBeforeLast('}', missingDelimiterValue = raw)
        .let { "{$it}" }
    val text = Regex(""""text"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.getOrNull(1)
        ?.replace("\\\"", "\"")
        ?.trim()
        ?: Regex(""""raw_text"\s*:\s*"([^"]*)"""").find(json)?.groupValues?.getOrNull(1)?.trim()
        ?: raw.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    val parsed = Regex(""""parsed_retention_time"\s*:\s*(-?\d+(?:[.,]\d+)?)""")
        .find(json)
        ?.groupValues
        ?.getOrNull(1)
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?.takeIf { boundary.rejectedForbiddenFields.isEmpty() }
        ?: parseRtLikeText(text)
    val typeName = Regex(""""text_type"\s*:\s*"([A-Z_]+)"""")
        .find(json)
        ?.groupValues
        ?.getOrNull(1)
    val confidence = Regex(""""confidence"\s*:\s*(0(?:\.\d+)?|1(?:\.0+)?)""")
        .find(json)
        ?.groupValues
        ?.getOrNull(1)
        ?.toFloatOrNull()
        ?.coerceIn(0f, 1f)
        ?: if (text.isNotBlank()) 0.55f else 0f
    if (text.isBlank() && parsed == null) {
        return VisionLocalTextCropResult(
            rawText = "",
            confidence = 0f,
            warnings = listOf("peak_label_ocr.vlm_empty_or_unparseable_response"),
            rejectedForbiddenFields = boundary.rejectedForbiddenFields.toList(),
            runtimeProfile = runtimeProfile.copy(success = false),
        )
    }
    return VisionLocalTextCropResult(
        rawText = text,
        normalizedText = text.trim().replace(Regex("\\s+"), " "),
        parsedRetentionTime = parsed,
        textType = typeName?.let { runCatching { VisionTextRegionType.valueOf(it) }.getOrNull() }
            ?: VisionTextRegionType.UNKNOWN_TEXT,
        confidence = confidence,
        warnings = buildList {
            add("peak_label_ocr.vlm_local_crop_fallback")
            if (boundary.rejectedForbiddenFields.isNotEmpty()) {
                add("vlm.boundary.rejected_forbidden_fields:${boundary.rejectedForbiddenFields.joinToString("|")}")
            }
            if (boundary.missingRequiredFields.isNotEmpty()) {
                add("vlm.boundary.missing_required_fields:${boundary.missingRequiredFields.joinToString("|")}")
            }
        },
        rejectedForbiddenFields = boundary.rejectedForbiddenFields.toList(),
        runtimeProfile = runtimeProfile,
    )
}

private fun parseRtLikeText(text: String): Double? =
    Regex("""(?<!\d)(\d{1,3}[.,]\d{1,4})(?!\d)""")
        .find(text.replace(" ", ""))
        ?.groupValues
        ?.getOrNull(1)
        ?.replace(',', '.')
        ?.toDoubleOrNull()

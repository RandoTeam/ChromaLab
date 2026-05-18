package com.chromalab.feature.processing.inference

import android.util.Log

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
        return try {
            VlmEngineHolder.isInferring = true
            val raw = engine.inferRaw(
                imagePath = cropImagePath,
                prompt = prompt,
                options = GenerationOptions(
                    maxTokens = 128,
                    timeoutMs = 180_000L,
                    temperature = 0f,
                    topP = 1f,
                    topK = 0,
                    repeatPenalty = config?.repeatPenalty ?: 1.05f,
                    repeatLastN = config?.repeatLastN ?: 32,
                ),
            )
            parseLocalTextCropResponse(raw)
        } catch (e: Exception) {
            Log.w(VISION_BACKEND_TAG, "Local crop VLM OCR failed: ${e.message}", e)
            null
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

private fun parseLocalTextCropResponse(raw: String): VisionLocalTextCropResult? {
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
    if (text.isBlank() && parsed == null) return null
    return VisionLocalTextCropResult(
        rawText = text,
        normalizedText = text.trim().replace(Regex("\\s+"), " "),
        parsedRetentionTime = parsed,
        textType = typeName?.let { runCatching { VisionTextRegionType.valueOf(it) }.getOrNull() }
            ?: if (parsed != null) VisionTextRegionType.PEAK_ANNOTATION else VisionTextRegionType.UNKNOWN_TEXT,
        confidence = confidence,
        warnings = listOf("peak_label_ocr.vlm_local_crop_fallback"),
    )
}

private fun parseRtLikeText(text: String): Double? =
    Regex("""(?<!\d)(\d{1,3}[.,]\d{1,4})(?!\d)""")
        .find(text.replace(" ", ""))
        ?.groupValues
        ?.getOrNull(1)
        ?.replace(',', '.')
        ?.toDoubleOrNull()

package com.chromalab.feature.processing.inference

import kotlinx.serialization.Serializable

@Serializable
enum class VisionTextRegionType {
    PEAK_ANNOTATION,
    TICK_LABEL,
    AXIS_LABEL,
    TITLE_OR_CHANNEL,
    PAGE_TEXT,
    UNKNOWN_TEXT,
}

@Serializable
data class VisionLocalTextCropContext(
    val cropKind: String,
    val insidePlotArea: Boolean,
    val graphContext: String? = null,
)

@Serializable
data class VisionLocalTextCropResult(
    val rawText: String,
    val normalizedText: String = rawText.trim(),
    val parsedRetentionTime: Double? = null,
    val textType: VisionTextRegionType = VisionTextRegionType.UNKNOWN_TEXT,
    val confidence: Float = 0f,
    val warnings: List<String> = emptyList(),
)

@Serializable
data class VisionGraphOverlayJudgement(
    val warnings: List<String> = emptyList(),
    val confidence: Float = 0f,
)

interface VisionModelBackend {
    suspend fun readLocalTextCrop(
        cropImagePath: String,
        context: VisionLocalTextCropContext,
    ): VisionLocalTextCropResult?

    suspend fun classifyTextRegion(
        cropImagePath: String,
        graphContext: String? = null,
    ): VisionTextRegionType?

    suspend fun judgeGraphOverlay(
        overlayImagePath: String,
        candidates: List<String>,
    ): VisionGraphOverlayJudgement?

    suspend fun summarizeGeometryWarnings(warnings: List<String>): String?
}

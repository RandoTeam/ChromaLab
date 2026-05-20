package com.chromalab.feature.processing.peaks

import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.multimodal.ForbiddenVlmNumericField
import com.chromalab.feature.processing.multimodal.ModelRuntimeProfile
import kotlinx.serialization.Serializable

@Serializable
enum class PeakLabelEvidenceSource {
    ML_KIT,
    VLM,
    BOTH,
    FIXTURE_HINT,
}

@Serializable
enum class PeakLabelEvidenceStatus {
    VALID_TEXT,
    AMBIGUOUS_TEXT,
    REJECTED,
}

@Serializable
enum class PeakLabelTextClassification {
    PEAK_ANNOTATION,
    TICK_LABEL,
    AXIS_LABEL,
    TITLE_OR_CHANNEL,
    PAGE_TEXT,
    UNKNOWN_TEXT,
}

@Serializable
data class PeakLabelEvidence(
    val rawText: String,
    val normalizedText: String = rawText.trim(),
    val parsedRetentionTime: Double? = null,
    val labelBoxPx: GraphRegion? = null,
    val cropBoundsPx: GraphRegion? = null,
    val linkedGraphPanelBounds: GraphRegion? = null,
    val linkedPlotAreaBounds: GraphRegion? = null,
    val localCropPath: String? = null,
    val source: PeakLabelEvidenceSource,
    val confidence: Float = 0f,
    val status: PeakLabelEvidenceStatus = PeakLabelEvidenceStatus.REJECTED,
    val textClassification: PeakLabelTextClassification = PeakLabelTextClassification.UNKNOWN_TEXT,
    val isRuntimeEvidence: Boolean = source != PeakLabelEvidenceSource.FIXTURE_HINT,
    val rejectionReason: String? = null,
    val warnings: List<String> = emptyList(),
    val rejectedForbiddenFields: List<ForbiddenVlmNumericField> = emptyList(),
    val runtimeProfile: ModelRuntimeProfile? = null,
)

@Serializable
data class PeakLabelEvidenceResult(
    val labels: List<PeakLabelEvidence> = emptyList(),
    val cropPaths: List<String> = emptyList(),
    val cropBoundsOverlayPath: String? = null,
    val textClassificationOverlayPath: String? = null,
    val warnings: List<String> = emptyList(),
)

object PeakLabelTextHeuristics {
    fun normalize(text: String): String = text.trim().replace(Regex("\\s+"), " ")

    fun isTitleOrIonChannelText(text: String): Boolean {
        val normalized = normalize(text).lowercase()
        val hasChannelKeyword = normalized.contains("ion") ||
            normalized.contains("m/z") ||
            normalized.contains("tic") ||
            normalized.contains("xic") ||
            normalized.contains("data.ms") ||
            normalized.contains(".d")
        val hasIonRange = Regex("""\(?\s*\d{1,4}(?:[.,]\d+)?\s*(?:to|-|–)\s*\d{1,4}(?:[.,]\d+)?\s*\)?""")
            .containsMatchIn(normalized)
        return hasChannelKeyword || hasIonRange
    }
}

expect class PeakLabelEvidenceReader() {
    suspend fun readPeakLabels(
        imagePath: String,
        outputDir: String,
        graphPanelBounds: GraphRegion,
        plotAreaBounds: GraphRegion,
    ): PeakLabelEvidenceResult
}

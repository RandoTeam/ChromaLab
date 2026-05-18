package com.chromalab.feature.processing.peaks

import com.chromalab.feature.processing.graph.GraphRegion
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
    val warnings: List<String> = emptyList(),
)

@Serializable
data class PeakLabelEvidenceResult(
    val labels: List<PeakLabelEvidence> = emptyList(),
    val cropPaths: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)

expect class PeakLabelEvidenceReader() {
    suspend fun readPeakLabels(
        imagePath: String,
        outputDir: String,
        graphPanelBounds: GraphRegion,
        plotAreaBounds: GraphRegion,
    ): PeakLabelEvidenceResult
}

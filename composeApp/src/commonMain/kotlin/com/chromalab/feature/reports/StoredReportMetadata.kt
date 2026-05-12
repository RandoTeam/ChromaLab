package com.chromalab.feature.reports

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val STORED_REPORT_METADATA_KIND = "chromalab.report-metadata"
const val STORED_REPORT_METADATA_VERSION = 1

/**
 * Stable metadata envelope stored in ChromatogramEntity.algorithmConfig.
 *
 * Processing stages can write this small JSON payload after image normalization, crop, OCR,
 * model execution, and axis calibration. Report export reads it back without guessing.
 */
@Serializable
data class StoredReportMetadata(
    val kind: String = STORED_REPORT_METADATA_KIND,
    val version: Int = STORED_REPORT_METADATA_VERSION,
    val appVersion: String? = null,
    val inputSourceType: InputSourceType? = null,
    val sourceName: String? = null,
    val detectedGraphCount: Int? = null,
    val analysisStartedAtEpochMillis: Long? = null,
    val analysisCompletedAtEpochMillis: Long? = null,
    val totalAnalysisDurationMillis: Long? = null,
    val selectedModel: ModelExecutionInfo? = null,
    val executedModel: ModelExecutionInfo? = null,
    val executedRuntime: ExecutedRuntime? = null,
    val deviceName: String? = null,
    val processingMode: ProcessingMode? = null,
    val stageTimings: List<ReportStageTiming> = emptyList(),
    val graphs: List<StoredGraphReportMetadata> = emptyList(),
    val warnings: List<ReportWarning> = emptyList(),
)

@Serializable
data class StoredGraphReportMetadata(
    val graphIndex: Int = 1,
    val source: GraphSourceMetadata? = null,
    val identification: ChromatogramIdentification? = null,
    val axisCalibration: ReportAxisCalibration? = null,
    val warnings: List<ReportWarning> = emptyList(),
)

object StoredReportMetadataCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(metadata: StoredReportMetadata): String = json.encodeToString(metadata)

    fun decodeOrNull(raw: String?): StoredReportMetadata? {
        val content = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!content.contains(STORED_REPORT_METADATA_KIND)) return null

        return runCatching { json.decodeFromString<StoredReportMetadata>(content) }
            .getOrNull()
            ?.takeIf { it.kind == STORED_REPORT_METADATA_KIND }
            ?.takeIf { it.version == STORED_REPORT_METADATA_VERSION }
    }
}

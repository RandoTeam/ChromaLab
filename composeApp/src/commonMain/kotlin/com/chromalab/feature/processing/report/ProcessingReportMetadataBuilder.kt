package com.chromalab.feature.processing.report

import com.chromalab.core.data.model.SourceType
import com.chromalab.feature.reports.ExecutedRuntime
import com.chromalab.feature.reports.GraphSourceMetadata
import com.chromalab.feature.reports.InputSourceType
import com.chromalab.feature.reports.ProcessingMode
import com.chromalab.feature.reports.StoredGraphReportMetadata
import com.chromalab.feature.reports.StoredReportMetadata
import com.chromalab.feature.reports.StoredReportMetadataCodec

fun buildProcessingReportMetadataConfig(
    sourcePath: String,
    processedPath: String?,
    sourceType: SourceType,
    graphIndex: Int,
    detectedGraphCount: Int,
    signalPointCount: Int,
    analysisStartedAtEpochMillis: Long,
    analysisCompletedAtEpochMillis: Long,
): String =
    StoredReportMetadataCodec.encode(
        buildProcessingStoredReportMetadata(
            sourcePath = sourcePath,
            processedPath = processedPath,
            sourceType = sourceType,
            graphIndex = graphIndex,
            detectedGraphCount = detectedGraphCount,
            signalPointCount = signalPointCount,
            analysisStartedAtEpochMillis = analysisStartedAtEpochMillis,
            analysisCompletedAtEpochMillis = analysisCompletedAtEpochMillis,
        ),
    )

fun buildProcessingStoredReportMetadata(
    sourcePath: String,
    processedPath: String?,
    sourceType: SourceType,
    graphIndex: Int,
    detectedGraphCount: Int,
    signalPointCount: Int,
    analysisStartedAtEpochMillis: Long,
    analysisCompletedAtEpochMillis: Long,
): StoredReportMetadata {
    val startedAt = analysisStartedAtEpochMillis.takeIf { it > 0 }
    val completedAt = analysisCompletedAtEpochMillis.takeIf { it > 0 }
    val duration = if (startedAt != null && completedAt != null && completedAt >= startedAt) {
        completedAt - startedAt
    } else {
        null
    }

    return StoredReportMetadata(
        inputSourceType = sourceType.toReportInputSourceType(),
        sourceName = displayName(processedPath) ?: displayName(sourcePath) ?: sourcePath,
        detectedGraphCount = detectedGraphCount.coerceAtLeast(1),
        analysisStartedAtEpochMillis = startedAt,
        analysisCompletedAtEpochMillis = completedAt,
        totalAnalysisDurationMillis = duration,
        executedRuntime = ExecutedRuntime.UNKNOWN,
        processingMode = ProcessingMode.FULL_ANALYSIS,
        graphs = listOf(
            StoredGraphReportMetadata(
                graphIndex = graphIndex.coerceAtLeast(1),
                source = GraphSourceMetadata(
                    preprocessingSteps = buildList {
                        add("ProcessingFlowScreen auto-save")
                        add("Photo analysis pipeline completed before calculation export")
                        add("Digitized signal points: ${signalPointCount.coerceAtLeast(0)}")
                        if (!processedPath.isNullOrBlank() && processedPath != sourcePath) {
                            add("Processed image path differs from original source")
                        }
                    },
                    scanMode = sourceType.toScanMode(),
                ),
            ),
        ),
    )
}

private fun SourceType.toReportInputSourceType(): InputSourceType =
    when (this) {
        SourceType.PHOTO -> InputSourceType.CAMERA_CAPTURE
        SourceType.GALLERY -> InputSourceType.SMART_SCAN_GALLERY
        SourceType.PDF,
        SourceType.CSV,
        SourceType.MZML -> InputSourceType.FILE_IMPORT
        SourceType.MANUAL -> InputSourceType.UNKNOWN
    }

private fun SourceType.toScanMode(): String =
    when (this) {
        SourceType.PHOTO -> "photo-processing-flow"
        SourceType.GALLERY -> "smart-scan-gallery-processing-flow"
        SourceType.PDF -> "pdf-processing-flow"
        SourceType.CSV -> "csv-import"
        SourceType.MZML -> "mzml-import"
        SourceType.MANUAL -> "manual-entry"
    }

private fun displayName(path: String?): String? =
    path
        ?.takeIf { it.isNotBlank() }
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.takeIf { it.isNotBlank() }

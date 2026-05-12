package com.chromalab.feature.processing.report

import com.chromalab.core.data.model.SourceType
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.reports.ExecutedRuntime
import com.chromalab.feature.reports.GraphPreparationVariantMetadata
import com.chromalab.feature.reports.GraphSourceMetadata
import com.chromalab.feature.reports.InputSourceType
import com.chromalab.feature.reports.ModelExecutionInfo
import com.chromalab.feature.reports.PixelRect
import com.chromalab.feature.reports.ProcessingMode
import com.chromalab.feature.reports.ReportStageTiming
import com.chromalab.feature.reports.ReportWarning
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
    sourceImageBounds: PixelRect? = null,
    detectedGraphBounds: PixelRect? = null,
    cropConfidence: Double? = null,
    preprocessingSteps: List<String> = emptyList(),
    preparationVariants: List<GraphPreparationVariantMetadata> = emptyList(),
    scanMode: String? = null,
    axisOcrResult: AxisOcrResult? = null,
    titleOcrConfidence: Double? = null,
    axisOcrConfidence: Double? = null,
    tickOcrConfidence: Double? = null,
    selectedModel: ModelExecutionInfo? = null,
    executedModel: ModelExecutionInfo? = null,
    executedRuntime: ExecutedRuntime = executedModel?.runtime ?: selectedModel?.runtime ?: ExecutedRuntime.UNKNOWN,
    deviceName: String? = null,
    stageTimings: List<ReportStageTiming> = emptyList(),
    graphWarnings: List<ReportWarning> = emptyList(),
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
            sourceImageBounds = sourceImageBounds,
            detectedGraphBounds = detectedGraphBounds,
            cropConfidence = cropConfidence,
            preprocessingSteps = preprocessingSteps,
            preparationVariants = preparationVariants,
            scanMode = scanMode,
            axisOcrResult = axisOcrResult,
            titleOcrConfidence = titleOcrConfidence,
            axisOcrConfidence = axisOcrConfidence,
            tickOcrConfidence = tickOcrConfidence,
            selectedModel = selectedModel,
            executedModel = executedModel,
            executedRuntime = executedRuntime,
            deviceName = deviceName,
            stageTimings = stageTimings,
            graphWarnings = graphWarnings,
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
    sourceImageBounds: PixelRect? = null,
    detectedGraphBounds: PixelRect? = null,
    cropConfidence: Double? = null,
    preprocessingSteps: List<String> = emptyList(),
    preparationVariants: List<GraphPreparationVariantMetadata> = emptyList(),
    scanMode: String? = null,
    axisOcrResult: AxisOcrResult? = null,
    titleOcrConfidence: Double? = null,
    axisOcrConfidence: Double? = null,
    tickOcrConfidence: Double? = null,
    selectedModel: ModelExecutionInfo? = null,
    executedModel: ModelExecutionInfo? = null,
    executedRuntime: ExecutedRuntime = executedModel?.runtime ?: selectedModel?.runtime ?: ExecutedRuntime.UNKNOWN,
    deviceName: String? = null,
    stageTimings: List<ReportStageTiming> = emptyList(),
    graphWarnings: List<ReportWarning> = emptyList(),
): StoredReportMetadata {
    val startedAt = analysisStartedAtEpochMillis.takeIf { it > 0 }
    val completedAt = analysisCompletedAtEpochMillis.takeIf { it > 0 }
    val duration = if (startedAt != null && completedAt != null && completedAt >= startedAt) {
        completedAt - startedAt
    } else {
        null
    }
    val selectedPreparationVariant = preparationVariants
        .firstOrNull { it.selected }
        ?: preparationVariants.minByOrNull { it.rank }
    val rejectedPreparationVariants = preparationVariants
        .filterNot { it == selectedPreparationVariant }
        .sortedBy { it.rank }
    val ocrExtraction = axisOcrResult.toProcessingOcrReportExtraction(detectedGraphBounds)

    return StoredReportMetadata(
        inputSourceType = sourceType.toReportInputSourceType(),
        sourceName = displayName(processedPath) ?: displayName(sourcePath) ?: sourcePath,
        detectedGraphCount = detectedGraphCount.coerceAtLeast(1),
        analysisStartedAtEpochMillis = startedAt,
        analysisCompletedAtEpochMillis = completedAt,
        totalAnalysisDurationMillis = duration,
        selectedModel = selectedModel,
        executedModel = executedModel,
        executedRuntime = executedRuntime,
        deviceName = deviceName?.takeIf { it.isNotBlank() },
        processingMode = ProcessingMode.FULL_ANALYSIS,
        stageTimings = stageTimings
            .filter { it.stageId.isNotBlank() && it.durationMillis >= 0L }
            .distinctBy { it.stageId },
        graphs = listOf(
            StoredGraphReportMetadata(
                graphIndex = graphIndex.coerceAtLeast(1),
                warnings = graphWarnings.toStoredGraphWarnings(graphIndex.coerceAtLeast(1)),
                identification = ocrExtraction.identification,
                axisCalibration = ocrExtraction.axisCalibration,
                source = GraphSourceMetadata(
                    sourceImageBounds = sourceImageBounds,
                    detectedGraphBounds = detectedGraphBounds,
                    cropConfidence = cropConfidence?.coerceIn(0.0, 1.0),
                    titleOcrConfidence = (titleOcrConfidence ?: ocrExtraction.titleOcrConfidence)?.coerceIn(0.0, 1.0),
                    axisOcrConfidence = axisOcrConfidence?.coerceIn(0.0, 1.0),
                    tickOcrConfidence = tickOcrConfidence?.coerceIn(0.0, 1.0),
                    selectedPreparationVariant = selectedPreparationVariant,
                    rejectedPreparationVariants = rejectedPreparationVariants,
                    preprocessingSteps = buildList {
                        add("ProcessingFlowScreen auto-save")
                        add("Photo analysis pipeline completed before calculation export")
                        add("Digitized signal points: ${signalPointCount.coerceAtLeast(0)}")
                        addAll(preprocessingSteps.filter { it.isNotBlank() })
                        if (!processedPath.isNullOrBlank() && processedPath != sourcePath) {
                            add("Processed image path differs from original source")
                        }
                    }.distinct(),
                    scanMode = scanMode?.takeIf { it.isNotBlank() } ?: sourceType.toScanMode(),
                ),
            ),
        ),
    )
}

private fun List<ReportWarning>.toStoredGraphWarnings(graphIndex: Int): List<ReportWarning> =
    filter { it.code.isNotBlank() && it.message.isNotBlank() }
        .map { warning ->
            warning.copy(graphIndex = graphIndex)
        }
        .distinctBy { warning ->
            listOf(
                warning.code,
                warning.stage.orEmpty(),
                warning.graphIndex?.toString().orEmpty(),
                warning.peakNumber?.toString().orEmpty(),
                warning.message,
            ).joinToString("|")
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

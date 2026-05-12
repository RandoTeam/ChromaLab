package com.chromalab.feature.reports

import com.chromalab.core.data.entity.ChromatogramEntity
import com.chromalab.core.data.model.SourceType
import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.processing.signal.DigitalSignal

fun buildCalculationReportOptions(
    run: CalculationRun,
    chromatogram: ChromatogramEntity?,
    signal: DigitalSignal?,
): CalculationRunReportOptions {
    val stored = StoredReportMetadataCodec.decodeOrNull(chromatogram?.algorithmConfig)
    val storedGraph = stored?.graphs
        ?.firstOrNull { it.graphIndex == 1 }
        ?: stored?.graphs?.firstOrNull()
    val fallbackSourceName = sourceName(chromatogram, signal) ?: run.sourceSignalId
    val sourceName = stored?.sourceName?.takeIf { it.isNotBlank() } ?: fallbackSourceName
    val fallbackSource = buildGraphSourceMetadata(chromatogram, signal, stored != null)

    return CalculationRunReportOptions(
        appVersion = stored?.appVersion,
        inputSourceType = stored?.inputSourceType ?: chromatogram?.sourceType.toReportInputSourceType(),
        sourceName = sourceName,
        detectedGraphCount = stored?.detectedGraphCount
            ?: stored?.graphs?.size?.takeIf { it > 0 }
            ?: 1,
        analysisStartedAtEpochMillis = stored?.analysisStartedAtEpochMillis,
        analysisCompletedAtEpochMillis = stored?.analysisCompletedAtEpochMillis ?: run.timestamp,
        totalAnalysisDurationMillis = stored?.totalAnalysisDurationMillis,
        selectedModel = stored?.selectedModel,
        executedModel = stored?.executedModel,
        executedRuntime = stored?.executedRuntime ?: ExecutedRuntime.DETERMINISTIC,
        deviceName = stored?.deviceName,
        processingMode = stored?.processingMode ?: ProcessingMode.EXPORT_ONLY,
        graphIndex = storedGraph?.graphIndex?.coerceAtLeast(1) ?: 1,
        graphSourceMetadata = mergeGraphSourceMetadata(storedGraph?.source, fallbackSource),
        identification = storedGraph?.identification ?: buildChromatogramIdentification(chromatogram, sourceName),
        axisCalibration = storedGraph?.axisCalibration,
        additionalReportWarnings = stored?.warnings.orEmpty(),
        additionalGraphWarnings = storedGraph?.warnings.orEmpty(),
    )
}

private fun mergeGraphSourceMetadata(
    stored: GraphSourceMetadata?,
    fallback: GraphSourceMetadata?,
): GraphSourceMetadata? {
    if (stored == null) return fallback
    if (fallback == null) return stored

    return stored.copy(
        preprocessingSteps = (stored.preprocessingSteps + fallback.preprocessingSteps).distinct(),
        scanMode = stored.scanMode ?: fallback.scanMode,
        manuallyAdjusted = stored.manuallyAdjusted || fallback.manuallyAdjusted,
    )
}

private fun buildGraphSourceMetadata(
    chromatogram: ChromatogramEntity?,
    signal: DigitalSignal?,
    storedMetadataAvailable: Boolean,
): GraphSourceMetadata? {
    if (chromatogram == null && signal == null && !storedMetadataAvailable) return null

    val preprocessingSteps = buildList {
        if (storedMetadataAvailable) {
            add("Stored report metadata loaded from algorithmConfig")
        }
        if (chromatogram != null) {
            add("Loaded chromatogram record from local database")
        }
        signal?.metadata?.totalPoints?.let { add("Restored $it digitized points") }
        chromatogram?.sourceType?.let { add("Source type: ${it.name}") }
    }

    return GraphSourceMetadata(
        preprocessingSteps = preprocessingSteps,
        scanMode = chromatogram?.sourceType?.name?.lowercase() ?: "stored-signal",
    )
}

private fun buildChromatogramIdentification(
    chromatogram: ChromatogramEntity?,
    sourceName: String,
): ChromatogramIdentification =
    ChromatogramIdentification(
        chromatogramMode = ReportTextValue.calculated(
            value = "digitized signal calculation",
            source = ReportValueSource.DETERMINISTIC,
        ),
        ionOrChannel = chromatogram?.ionChannel?.takeIf { it.isNotBlank() }?.let {
            ReportTextValue.calculated(it, source = ReportValueSource.IMPORTED_FILE)
        } ?: ReportTextValue.notCalculated(),
        sampleName = ReportTextValue.calculated(sourceName, source = ReportValueSource.IMPORTED_FILE),
        samplePathOrInstrumentLabel = chromatogram?.filePath?.takeIf { it.isNotBlank() }?.let {
            ReportTextValue.calculated(it, source = ReportValueSource.IMPORTED_FILE)
        } ?: ReportTextValue.calculated(sourceName, source = ReportValueSource.IMPORTED_FILE),
    )

private fun SourceType?.toReportInputSourceType(): InputSourceType =
    when (this) {
        SourceType.PHOTO -> InputSourceType.CAMERA_CAPTURE
        SourceType.GALLERY -> InputSourceType.SMART_SCAN_GALLERY
        SourceType.PDF,
        SourceType.CSV,
        SourceType.MZML -> InputSourceType.FILE_IMPORT
        SourceType.MANUAL,
        null -> InputSourceType.UNKNOWN
    }

private fun sourceName(
    chromatogram: ChromatogramEntity?,
    signal: DigitalSignal?,
): String? =
    displayName(chromatogram?.filePath)
        ?: displayName(signal?.metadata?.sourceImage)
        ?: chromatogram?.ionChannel?.takeIf { it.isNotBlank() }
        ?: chromatogram?.id?.let { "chromatogram_$it" }

private fun displayName(path: String?): String? =
    path
        ?.takeIf { it.isNotBlank() }
        ?.substringAfterLast('/')
        ?.substringAfterLast('\\')
        ?.takeIf { it.isNotBlank() }

package com.chromalab.feature.reports

import com.chromalab.core.data.entity.ChromatogramEntity
import com.chromalab.core.data.model.SourceType
import com.chromalab.feature.calculation.core.CalculationParams
import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.calculation.core.SignalBundle
import com.chromalab.feature.calculation.core.SignalPoint
import com.chromalab.feature.calculation.core.SignalSource
import com.chromalab.feature.calculation.core.ValidationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoredReportMetadataTest {

    @Test
    fun codecRoundTripsReportMetadataEnvelope() {
        val metadata = StoredReportMetadata(
            inputSourceType = InputSourceType.SMART_SCAN_GALLERY,
            sourceName = "BELIY TIGR_1.D/data.ms",
            detectedGraphCount = 2,
            executedRuntime = ExecutedRuntime.LITERT,
            graphs = listOf(
                StoredGraphReportMetadata(
                    graphIndex = 1,
                    source = GraphSourceMetadata(
                        detectedGraphBounds = PixelRect(x = 10, y = 20, width = 300, height = 180),
                        cropConfidence = 0.92,
                        preprocessingSteps = listOf("ML Kit document scan"),
                        scanMode = "smart-scan-gallery",
                    ),
                    identification = ChromatogramIdentification(
                        ionOrChannel = ReportTextValue(
                            value = "m/z 92.00",
                            status = ReportValueStatus.DETECTED,
                            confidence = 0.9,
                            source = ReportValueSource.OCR,
                        ),
                    ),
                ),
            ),
        )

        val decoded = StoredReportMetadataCodec.decodeOrNull(StoredReportMetadataCodec.encode(metadata))

        assertNotNull(decoded)
        assertEquals(STORED_REPORT_METADATA_KIND, decoded.kind)
        assertEquals(2, decoded.detectedGraphCount)
        assertEquals(ExecutedRuntime.LITERT, decoded.executedRuntime)
        assertEquals("m/z 92.00", decoded.graphs.single().identification?.ionOrChannel?.value)
    }

    @Test
    fun codecIgnoresUnrelatedAlgorithmConfigJson() {
        assertNull(StoredReportMetadataCodec.decodeOrNull("""{"baselineMethod":"ALS"}"""))
    }

    @Test
    fun optionsBuilderPrefersStoredMetadataOverFallbackFields() {
        val stored = StoredReportMetadata(
            inputSourceType = InputSourceType.SMART_SCAN_GALLERY,
            sourceName = "BELIY TIGR_1.D/data.ms",
            detectedGraphCount = 2,
            analysisStartedAtEpochMillis = 1000L,
            analysisCompletedAtEpochMillis = 3000L,
            totalAnalysisDurationMillis = 2000L,
            selectedModel = ModelExecutionInfo(
                modelId = "gemma-litert",
                modelName = "Gemma LiteRT",
                runtime = ExecutedRuntime.LITERT,
            ),
            executedModel = ModelExecutionInfo(
                modelId = "gemma-litert",
                modelName = "Gemma LiteRT",
                runtime = ExecutedRuntime.LITERT,
            ),
            executedRuntime = ExecutedRuntime.LITERT,
            processingMode = ProcessingMode.FULL_ANALYSIS,
            graphs = listOf(
                StoredGraphReportMetadata(
                    graphIndex = 1,
                    source = GraphSourceMetadata(
                        sourceImageBounds = PixelRect(0, 0, 600, 400),
                        detectedGraphBounds = PixelRect(20, 40, 500, 260),
                        cropConfidence = 0.91,
                        preprocessingSteps = listOf("Auto crop"),
                        scanMode = "smart-scan-gallery",
                        titleOcrConfidence = 0.83,
                    ),
                    identification = ChromatogramIdentification(
                        ionOrChannel = ReportTextValue(
                            value = "m/z 92.00",
                            status = ReportValueStatus.DETECTED,
                            confidence = 0.9,
                            source = ReportValueSource.OCR,
                        ),
                    ),
                    warnings = listOf(
                        ReportWarning(
                            code = "graph.crop_review_required",
                            message = "Crop confidence is below the release threshold.",
                            severity = ReportSeverity.WARNING,
                            stage = "graph_preparation",
                            graphIndex = 1,
                        ),
                    ),
                ),
            ),
        )
        val chromatogram = chromatogramEntity(
            sourceType = SourceType.PHOTO,
            filePath = """C:\samples\source.png""",
            algorithmConfig = StoredReportMetadataCodec.encode(stored),
        )

        val options = buildCalculationReportOptions(
            run = calculationRun(),
            chromatogram = chromatogram,
            signal = null,
        )

        assertEquals(InputSourceType.SMART_SCAN_GALLERY, options.inputSourceType)
        assertEquals("BELIY TIGR_1.D/data.ms", options.sourceName)
        assertEquals(2, options.detectedGraphCount)
        assertEquals(1000L, options.analysisStartedAtEpochMillis)
        assertEquals(3000L, options.analysisCompletedAtEpochMillis)
        assertEquals(2000L, options.totalAnalysisDurationMillis)
        assertEquals(ExecutedRuntime.LITERT, options.executedRuntime)
        assertEquals(ProcessingMode.FULL_ANALYSIS, options.processingMode)
        assertEquals("m/z 92.00", options.identification?.ionOrChannel?.value)
        assertEquals(0.91, options.graphSourceMetadata?.cropConfidence)
        assertTrue(options.graphSourceMetadata?.preprocessingSteps.orEmpty().contains("Auto crop"))
        assertTrue(
            options.graphSourceMetadata?.preprocessingSteps.orEmpty()
                .contains("Stored report metadata loaded from algorithmConfig"),
        )
        assertEquals("graph.crop_review_required", options.additionalGraphWarnings.single().code)
    }

    private fun chromatogramEntity(
        sourceType: SourceType,
        filePath: String?,
        algorithmConfig: String?,
    ): ChromatogramEntity =
        ChromatogramEntity(
            id = 7L,
            sampleId = 1L,
            sourceType = sourceType,
            filePath = filePath,
            ionChannel = null,
            timeRangeStart = null,
            timeRangeEnd = null,
            intensityUnit = "a.u.",
            qualityScore = null,
            dataPoints = null,
            algorithmConfig = algorithmConfig,
            createdAt = 100L,
            updatedAt = 200L,
        )

    private fun calculationRun(): CalculationRun =
        CalculationRun(
            id = "run-1",
            sourceSignalId = "signal-1",
            pipelineVersion = "test",
            algorithmVersion = "test",
            params = CalculationParams(),
            validation = ValidationResult(
                isValid = true,
                pointCount = 1,
                isSorted = true,
                duplicateTimeCount = 0,
                gapCount = 0,
                nanCount = 0,
                infinityCount = 0,
                negativeIntensityCount = 0,
                isUniformSpacing = true,
                avgTimeStep = 1.0,
                maxTimeStepDeviation = 0.0,
                warnings = emptyList(),
            ),
            signals = SignalBundle(
                raw = listOf(
                    SignalPoint(
                        index = 0,
                        time = 0.0,
                        intensity = 1.0,
                        pixelX = 0,
                        pixelY = 0.0,
                        confidence = 1.0,
                        isInterpolated = false,
                    ),
                ),
                smoothed = null,
                baseline = null,
                baselineCorrected = null,
                signalUsedForDetection = SignalSource.RAW,
                signalUsedForIntegration = SignalSource.RAW,
            ),
            peaks = emptyList(),
            warnings = emptyList(),
            timestamp = 4000L,
        )
}

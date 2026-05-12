package com.chromalab.feature.reports

import com.chromalab.core.data.entity.ChromatogramEntity
import com.chromalab.core.data.model.SourceType
import com.chromalab.feature.calculation.algorithm.ConfidenceGrade
import com.chromalab.feature.calculation.algorithm.OverlapStatus
import com.chromalab.feature.calculation.core.CalculationParams
import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.calculation.core.CalculationWarning
import com.chromalab.feature.calculation.core.PeakResult
import com.chromalab.feature.calculation.core.PeakStatus
import com.chromalab.feature.calculation.core.SignalBundle
import com.chromalab.feature.calculation.core.SignalPoint
import com.chromalab.feature.calculation.core.SignalSource
import com.chromalab.feature.calculation.core.ValidationResult
import com.chromalab.feature.calculation.core.WarningSeverity
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
                        selectedPreparationVariant = GraphPreparationVariantMetadata(
                            rank = 1,
                            configName = "contrast",
                            inputVariant = "contrast",
                            score = 220.0,
                            selected = true,
                            scoreBreakdown = "graph[conf=HIGH], curve[cov=97%]",
                        ),
                        rejectedPreparationVariants = listOf(
                            GraphPreparationVariantMetadata(
                                rank = 2,
                                configName = "binary",
                                inputVariant = "binary",
                                score = 140.0,
                                selected = false,
                                scoreBreakdown = "graph[conf=HIGH], curve[cov=62%]",
                            ),
                        ),
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
        assertEquals("contrast", decoded.graphs.single().source?.selectedPreparationVariant?.configName)
        assertEquals("binary", decoded.graphs.single().source?.rejectedPreparationVariants?.single()?.configName)
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
            stageTimings = listOf(
                ReportStageTiming("IMAGE_QUALITY", "IMAGE_QUALITY", 100L),
                ReportStageTiming("OCR_SUGGESTION", "OCR_SUGGESTION", 300L),
            ),
            graphs = listOf(
                StoredGraphReportMetadata(
                    graphIndex = 1,
                    source = GraphSourceMetadata(
                        sourceImageBounds = PixelRect(0, 0, 600, 400),
                        detectedGraphBounds = PixelRect(20, 40, 500, 260),
                        cropConfidence = 0.91,
                        preprocessingSteps = listOf("Auto crop"),
                        selectedPreparationVariant = GraphPreparationVariantMetadata(
                            rank = 1,
                            configName = "high_contrast",
                            inputVariant = "contrast",
                            score = 235.0,
                            selected = true,
                        ),
                        rejectedPreparationVariants = listOf(
                            GraphPreparationVariantMetadata(
                                rank = 2,
                                configName = "scan_style",
                                inputVariant = "scan_style",
                                score = 202.0,
                                selected = false,
                            ),
                        ),
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
        assertEquals(2, options.stageTimings.size)
        assertEquals(300L, options.stageTimings.last().durationMillis)
        assertEquals("m/z 92.00", options.identification?.ionOrChannel?.value)
        assertEquals(0.91, options.graphSourceMetadata?.cropConfidence)
        assertEquals("high_contrast", options.graphSourceMetadata?.selectedPreparationVariant?.configName)
        assertEquals("scan_style", options.graphSourceMetadata?.rejectedPreparationVariants?.single()?.configName)
        assertTrue(options.graphSourceMetadata?.preprocessingSteps.orEmpty().contains("Auto crop"))
        assertTrue(
            options.graphSourceMetadata?.preprocessingSteps.orEmpty()
                .contains("Stored report metadata loaded from algorithmConfig"),
        )
        assertEquals("graph.crop_review_required", options.additionalGraphWarnings.single().code)
    }

    @Test
    fun optionsBuilderUsesStoredGraphIndexFromCurrentChromatogramRecord() {
        val stored = StoredReportMetadata(
            sourceName = "multi_graph_photo.jpg",
            detectedGraphCount = 3,
            graphs = listOf(
                StoredGraphReportMetadata(
                    graphIndex = 2,
                    source = GraphSourceMetadata(
                        detectedGraphBounds = PixelRect(40, 120, 500, 260),
                        preprocessingSteps = listOf("Graph 2 selected crop"),
                    ),
                ),
            ),
        )
        val chromatogram = chromatogramEntity(
            sourceType = SourceType.PHOTO,
            filePath = """C:\samples\multi_graph_photo.jpg""",
            algorithmConfig = StoredReportMetadataCodec.encode(stored),
        )

        val options = buildCalculationReportOptions(
            run = calculationRun(),
            chromatogram = chromatogram,
            signal = null,
        )

        assertEquals(2, options.graphIndex)
        assertEquals(3, options.detectedGraphCount)
        assertEquals(PixelRect(40, 120, 500, 260), options.graphSourceMetadata?.detectedGraphBounds)
        assertTrue(options.graphSourceMetadata?.preprocessingSteps.orEmpty().contains("Graph 2 selected crop"))
    }

    @Test
    fun reportMapperBindsPeaksAndWarningsToCurrentGraphIndex() {
        val run = calculationRun(
            peaks = listOf(
                peakResult(
                    peakId = 0,
                    rtApex = 0.5,
                    warning = "peak belongs to graph 2",
                ),
            ),
            warnings = listOf(
                CalculationWarning(
                    message = "calculation warning belongs to graph 2",
                    severity = WarningSeverity.CAUTION,
                    stage = "peak_detection",
                ),
            ),
        )

        val report = CalculationRunReportMapper.map(
            run = run,
            options = CalculationRunReportOptions(
                detectedGraphCount = 2,
                graphIndex = 2,
                graphSourceMetadata = GraphSourceMetadata(
                    detectedGraphBounds = PixelRect(20, 260, 320, 190),
                ),
                additionalGraphWarnings = listOf(
                    ReportWarning(
                        code = "legacy.graph.warning",
                        message = "legacy graph warning had the wrong index",
                        severity = ReportSeverity.WARNING,
                        stage = "graph_detection",
                        graphIndex = 1,
                    ),
                ),
            ),
        )

        val graph = report.graphs.single()
        assertEquals(2, graph.graphIndex)
        assertEquals(2, graph.peaks.single().warnings.single().graphIndex)
        assertEquals(2, graph.warnings.first { it.code == "calculation_run.peak_detection" }.graphIndex)
        assertEquals(2, graph.warnings.first { it.code == "legacy.graph.warning" }.graphIndex)
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

    private fun calculationRun(
        peaks: List<PeakResult> = emptyList(),
        warnings: List<CalculationWarning> = emptyList(),
    ): CalculationRun =
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
            peaks = peaks,
            warnings = warnings,
            timestamp = 4000L,
        )

    private fun peakResult(
        peakId: Int,
        rtApex: Double,
        warning: String,
    ): PeakResult =
        PeakResult(
            peakId = peakId,
            status = PeakStatus.AUTO,
            rtApex = rtApex,
            rtCentroid = null,
            height = 10.0,
            area = 25.0,
            widthBase = 0.2,
            widthHalfHeight = 0.1,
            prominence = 8.0,
            snr = 12.0,
            snrMethod = "RMS",
            baselineMethod = "ALS",
            integrationMethod = "trapezoidal",
            confidence = ConfidenceGrade.HIGH,
            overlapStatus = OverlapStatus.ISOLATED,
            leftBoundaryTime = rtApex - 0.1,
            rightBoundaryTime = rtApex + 0.1,
            boundaryMethod = "LOCAL_MINIMA",
            warnings = listOf(warning),
            areaPercent = 100.0,
        )
}

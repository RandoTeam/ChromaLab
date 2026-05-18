package com.chromalab.feature.processing.report

import com.chromalab.core.data.model.SourceType
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import com.chromalab.feature.processing.geometry.GeometryTrace
import com.chromalab.feature.processing.ocr.AxisOcrResult
import com.chromalab.feature.processing.ocr.OcrTextElement
import com.chromalab.feature.reports.AxisCalibrationCandidateStatus
import com.chromalab.feature.reports.ExecutedRuntime
import com.chromalab.feature.reports.GraphPreparationVariantMetadata
import com.chromalab.feature.reports.InputSourceType
import com.chromalab.feature.reports.ModelExecutionInfo
import com.chromalab.feature.reports.PixelRect
import com.chromalab.feature.reports.ProcessingMode
import com.chromalab.feature.reports.ReportSeverity
import com.chromalab.feature.reports.ReportStageTiming
import com.chromalab.feature.reports.ReportValueSource
import com.chromalab.feature.reports.ReportValueStatus
import com.chromalab.feature.reports.ReportWarning
import com.chromalab.feature.reports.StoredReportMetadataCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProcessingReportMetadataBuilderTest {

    @Test
    fun processingMetadataConfigContainsStableReportEnvelope() {
        val config = buildProcessingReportMetadataConfig(
            sourcePath = """C:\input\raw_photo.jpg""",
            processedPath = """C:\input\normalized_photo.jpg""",
            sourceType = SourceType.PHOTO,
            graphIndex = 2,
            detectedGraphCount = 3,
            signalPointCount = 512,
            analysisStartedAtEpochMillis = 1_000L,
            analysisCompletedAtEpochMillis = 2_750L,
            sourceImageBounds = PixelRect(0, 0, 1200, 800),
            detectedGraphBounds = PixelRect(120, 160, 900, 420),
            cropConfidence = 0.82,
            preprocessingSteps = listOf("EXIF normalized", "Auto-sweep selected config: high_contrast"),
            preparationVariants = listOf(
                GraphPreparationVariantMetadata(
                    rank = 1,
                    configName = "high_contrast",
                    inputVariant = "contrast",
                    score = 233.5,
                    selected = true,
                    scoreBreakdown = "graph[conf=HIGH], curve[cov=98%]",
                ),
                GraphPreparationVariantMetadata(
                    rank = 2,
                    configName = "scan_style",
                    inputVariant = "scan_style",
                    score = 211.0,
                    selected = false,
                    scoreBreakdown = "graph[conf=HIGH], curve[cov=82%]",
                ),
            ),
            scanMode = "photo-processing-flow/high_contrast",
            titleOcrConfidence = null,
            axisOcrConfidence = 0.76,
            tickOcrConfidence = 0.88,
            selectedModel = ModelExecutionInfo(
                modelId = "gemma-litert",
                modelName = "Gemma LiteRT",
                runtime = ExecutedRuntime.LITERT,
            ),
            executedModel = ModelExecutionInfo(
                modelId = "gemma-litert",
                modelName = "Gemma LiteRT",
                runtime = ExecutedRuntime.LITERT,
                backendLabel = "LiteRT GPU",
            ),
            deviceName = "Pixel Test",
            stageTimings = listOf(
                ReportStageTiming("IMAGE_QUALITY", "IMAGE_QUALITY", 125L),
                ReportStageTiming("GRAPH_SELECTION", "GRAPH_SELECTION", 900L),
                ReportStageTiming("OCR_SUGGESTION", "OCR_SUGGESTION", 450L),
            ),
            graphWarnings = listOf(
                ReportWarning(
                    code = "axis.ocr_ticks_incomplete",
                    message = "Axis OCR did not produce enough X/Y tick suggestions.",
                    severity = ReportSeverity.WARNING,
                    stage = "axis_ocr",
                ),
            ),
        )

        val metadata = StoredReportMetadataCodec.decodeOrNull(config)

        assertNotNull(metadata)
        assertEquals(InputSourceType.CAMERA_CAPTURE, metadata.inputSourceType)
        assertEquals("normalized_photo.jpg", metadata.sourceName)
        assertEquals(3, metadata.detectedGraphCount)
        assertEquals(1_750L, metadata.totalAnalysisDurationMillis)
        assertEquals("gemma-litert", metadata.selectedModel?.modelId)
        assertEquals("LiteRT GPU", metadata.executedModel?.backendLabel)
        assertEquals(ExecutedRuntime.LITERT, metadata.executedRuntime)
        assertEquals("Pixel Test", metadata.deviceName)
        assertEquals(ProcessingMode.FULL_ANALYSIS, metadata.processingMode)
        assertEquals(5, metadata.stageTimings.size)
        assertEquals(450L, metadata.stageTimings.single { it.stageId == "OCR_SUGGESTION" }.durationMillis)
        assertEquals(900L, metadata.stageTimings.single { it.stageId == "model.graph_region" }.durationMillis)
        assertEquals(900L, metadata.stageTimings.single { it.stageId == "model.title_ion_axis" }.durationMillis)
        assertEquals(emptyList(), metadata.warnings)

        val graph = metadata.graphs.single()
        assertEquals(2, graph.graphIndex)
        assertEquals("photo-processing-flow/high_contrast", graph.source?.scanMode)
        assertEquals(PixelRect(0, 0, 1200, 800), graph.source?.sourceImageBounds)
        assertEquals(PixelRect(120, 160, 900, 420), graph.source?.detectedGraphBounds)
        assertEquals(0.82, graph.source?.cropConfidence)
        assertEquals("high_contrast", graph.source?.selectedPreparationVariant?.configName)
        assertEquals("contrast", graph.source?.selectedPreparationVariant?.inputVariant)
        assertEquals(233.5, graph.source?.selectedPreparationVariant?.score)
        assertEquals("scan_style", graph.source?.rejectedPreparationVariants?.single()?.configName)
        assertEquals(null, graph.source?.titleOcrConfidence)
        assertEquals(0.76, graph.source?.axisOcrConfidence)
        assertEquals(0.88, graph.source?.tickOcrConfidence)
        assertEquals("axis.ocr_ticks_incomplete", graph.warnings.single().code)
        assertEquals(2, graph.warnings.single().graphIndex)
        assertTrue(
            graph.source?.preprocessingSteps.orEmpty()
                .contains("Digitized signal points: 512"),
        )
        assertTrue(
            graph.source?.preprocessingSteps.orEmpty()
                .contains("Auto-sweep selected config: high_contrast"),
        )
    }

    @Test
    fun selectedModelRuntimeIsNotUsedAsExecutedRuntime() {
        val config = buildProcessingReportMetadataConfig(
            sourcePath = """C:\input\raw_photo.jpg""",
            processedPath = null,
            sourceType = SourceType.PHOTO,
            graphIndex = 1,
            detectedGraphCount = 1,
            signalPointCount = 256,
            analysisStartedAtEpochMillis = 1_000L,
            analysisCompletedAtEpochMillis = 2_000L,
            selectedModel = ModelExecutionInfo(
                modelId = "tiny-gguf-vlm",
                modelName = "Tiny GGUF VLM",
                runtime = ExecutedRuntime.GGUF,
            ),
            executedModel = null,
        )

        val metadata = StoredReportMetadataCodec.decodeOrNull(config)

        assertNotNull(metadata)
        assertEquals(ExecutedRuntime.GGUF, metadata.selectedModel?.runtime)
        assertNull(metadata.executedModel)
        assertEquals(ExecutedRuntime.UNKNOWN, metadata.executedRuntime)
    }

    @Test
    fun processingMetadataConfigStoresGeometryTraceStatus() {
        val config = buildProcessingReportMetadataConfig(
            sourcePath = "raw.jpg",
            processedPath = "normalized.jpg",
            sourceType = SourceType.PHOTO,
            graphIndex = 1,
            detectedGraphCount = 1,
            signalPointCount = 0,
            analysisStartedAtEpochMillis = 100L,
            analysisCompletedAtEpochMillis = 200L,
            geometryReportStatus = GeometryReportStatus.REVIEW_READY,
            geometryTrace = GeometryTrace(
                originalImagePath = "raw.jpg",
                normalizedImagePath = "normalized.jpg",
                warnings = listOf("calibration.x.two_anchor_review"),
            ),
        )

        val graph = StoredReportMetadataCodec.decodeOrNull(config)?.graphs?.single()

        assertNotNull(graph)
        assertEquals(GeometryReportStatus.REVIEW_READY, graph.geometryReportStatus)
        assertEquals(GeometryReportStatus.REVIEW_READY, graph.source?.geometryReportStatus)
        assertEquals("raw.jpg", graph.geometryTrace?.originalImagePath)
        assertEquals("calibration.x.two_anchor_review", graph.geometryTrace?.warnings?.single())
    }

    @Test
    fun processingMetadataRecordsRequiredModelStageFailuresWhenExecutedModelMissing() {
        val config = buildProcessingReportMetadataConfig(
            sourcePath = """C:\input\raw_photo.jpg""",
            processedPath = null,
            sourceType = SourceType.PHOTO,
            graphIndex = 1,
            detectedGraphCount = 1,
            signalPointCount = 256,
            analysisStartedAtEpochMillis = 1_000L,
            analysisCompletedAtEpochMillis = 2_000L,
            selectedModel = ModelExecutionInfo(
                modelId = "tiny-gguf-vlm",
                modelName = "Tiny GGUF VLM",
                runtime = ExecutedRuntime.GGUF,
            ),
            executedModel = null,
            stageTimings = listOf(
                ReportStageTiming("IMAGE_QUALITY", "IMAGE_QUALITY", 125L),
            ),
        )

        val metadata = StoredReportMetadataCodec.decodeOrNull(config)

        assertNotNull(metadata)
        assertEquals(ExecutedRuntime.UNKNOWN, metadata.executedRuntime)
        assertTrue(metadata.warnings.any { it.code == "model.execution_missing" })
        assertTrue(metadata.warnings.any { it.code == "model.graph_region.required_vision_failed" })
        assertTrue(metadata.warnings.any { it.code == "model.title_ion_axis.required_vision_failed" })
        assertTrue(metadata.warnings.all { it.graphIndex == 1 })
    }

    @Test
    fun processingMetadataAuditLineExposesDeviceValidationFields() {
        val config = buildProcessingReportMetadataConfig(
            sourcePath = """C:\input\raw_photo.jpg""",
            processedPath = """C:\input\normalized_photo.jpg""",
            sourceType = SourceType.PHOTO,
            graphIndex = 1,
            detectedGraphCount = 1,
            signalPointCount = 256,
            analysisStartedAtEpochMillis = 1_000L,
            analysisCompletedAtEpochMillis = 2_000L,
            selectedModel = ModelExecutionInfo(
                modelId = "tiny-gguf-vlm",
                modelName = "Tiny GGUF VLM",
                runtime = ExecutedRuntime.GGUF,
            ),
            executedModel = null,
            deviceName = "Xiaomi Mi 8",
            stageTimings = listOf(
                ReportStageTiming("IMAGE_QUALITY", "IMAGE_QUALITY", 125L),
            ),
        )

        val line = buildProcessingReportMetadataAuditLine(config, chromatogramId = 42L)

        assertTrue(line.startsWith("REPORT_AUDIT"))
        assertTrue(line.contains("chromatogramId=42"))
        assertTrue(line.contains("source=normalized_photo.jpg"))
        assertTrue(line.contains("selected=tiny-gguf-vlm/GGUF/none"))
        assertTrue(line.contains("executed=none"))
        assertTrue(line.contains("runtime=UNKNOWN"))
        assertTrue(line.contains("device=Xiaomi_Mi_8"))
        assertTrue(line.contains("timings=IMAGE_QUALITY:125"))
        assertTrue(line.contains("model.graph_region.required_vision_failed:FAILED"))
        assertTrue(line.contains("model.title_ion_axis.required_vision_failed:FAILED"))
    }

    @Test
    fun processingMetadataExtractsOcrIdentificationAndAxisLabels() {
        val title = """Ion 92.00 (91.70 to 92.70): BELIY TIGR_1.D\data.ms"""
        val config = buildProcessingReportMetadataConfig(
            sourcePath = """C:\input\raw_photo.jpg""",
            processedPath = """C:\input\normalized_photo.jpg""",
            sourceType = SourceType.PHOTO,
            graphIndex = 1,
            detectedGraphCount = 1,
            signalPointCount = 512,
            analysisStartedAtEpochMillis = 1_000L,
            analysisCompletedAtEpochMillis = 2_750L,
            detectedGraphBounds = PixelRect(100, 100, 400, 300),
            axisOcrResult = AxisOcrResult(
                rawElements = listOf(
                    OcrTextElement(
                        text = title,
                        numericValue = null,
                        x = 165f,
                        y = 105f,
                        width = 330f,
                        height = 18f,
                        confidence = 0.86f,
                    ),
                    OcrTextElement(
                        text = "Abundance",
                        numericValue = null,
                        x = 35f,
                        y = 128f,
                        width = 74f,
                        height = 16f,
                        confidence = 0.82f,
                    ),
                    OcrTextElement(
                        text = "Time-->",
                        numericValue = null,
                        x = 80f,
                        y = 426f,
                        width = 60f,
                        height = 14f,
                        confidence = 0.80f,
                    ),
                    OcrTextElement(
                        text = "10.00",
                        numericValue = 10f,
                        x = 100f,
                        y = 410f,
                        width = 20f,
                        height = 14f,
                        confidence = 0.78f,
                    ),
                    OcrTextElement(
                        text = "25.00",
                        numericValue = 25f,
                        x = 480f,
                        y = 410f,
                        width = 20f,
                        height = 14f,
                        confidence = 0.78f,
                    ),
                    OcrTextElement(
                        text = "10000",
                        numericValue = 10_000f,
                        x = 55f,
                        y = 100f,
                        width = 40f,
                        height = 16f,
                        confidence = 0.79f,
                    ),
                    OcrTextElement(
                        text = "0",
                        numericValue = 0f,
                        x = 55f,
                        y = 392f,
                        width = 12f,
                        height = 16f,
                        confidence = 0.79f,
                    ),
                    OcrTextElement(
                        text = """Ion 44.00: OTHER_SAMPLE.D\data.ms""",
                        numericValue = null,
                        x = 900f,
                        y = 105f,
                        width = 280f,
                        height = 18f,
                        confidence = 0.95f,
                    ),
                ),
                suggestedXValues = listOf(10f, 15f, 20f, 25f),
                suggestedYValues = listOf(10_000f, 9_000f, 8_000f, 0f),
                xUnit = "Time-->",
                yUnit = "Abundance",
                confidence = 0.82f,
                timestamp = 1_234L,
            ),
        )

        val graph = StoredReportMetadataCodec.decodeOrNull(config)?.graphs?.single()

        assertNotNull(graph)
        assertEquals(title, graph.identification?.chromatogramTitle?.value)
        assertEquals(ReportValueStatus.DETECTED, graph.identification?.chromatogramTitle?.status)
        assertEquals("m/z 92.00", graph.identification?.ionOrChannel?.value)
        assertEquals("91.70 to 92.70", graph.identification?.ionRange?.value)
        assertEquals("BELIY TIGR_1", graph.identification?.sampleName?.value)
        assertEquals("""BELIY TIGR_1.D\data.ms""", graph.identification?.samplePathOrInstrumentLabel?.value)
        assertEquals(0.86, graph.source?.titleOcrConfidence ?: -1.0, 0.0000001)
        assertEquals("Time", graph.axisCalibration?.xAxis?.label?.value)
        assertEquals("min", graph.axisCalibration?.xAxis?.unit?.value)
        assertEquals(ReportValueStatus.INFERRED, graph.axisCalibration?.xAxis?.unit?.status)
        assertEquals(ReportValueSource.LOCAL_KNOWLEDGE, graph.axisCalibration?.xAxis?.unit?.source)
        assertEquals(listOf(10.0, 15.0, 20.0, 25.0), graph.axisCalibration?.xAxis?.majorTicks?.map { it.value })
        assertEquals("Abundance", graph.axisCalibration?.yAxis?.label?.value)
        assertEquals("counts", graph.axisCalibration?.yAxis?.unit?.value)
        assertEquals(listOf(0.0, 8_000.0, 9_000.0, 10_000.0), graph.axisCalibration?.yAxis?.majorTicks?.map { it.value })
        val candidates = graph.axisCalibration?.calibrationCandidates.orEmpty()
        val xCandidate = candidates.single { it.candidateId == "ocr-x-axis" }
        val yCandidate = candidates.single { it.candidateId == "ocr-y-axis" }
        assertEquals(AxisCalibrationCandidateStatus.VALIDATED, xCandidate.status)
        assertEquals(AxisCalibrationCandidateStatus.VALIDATED, yCandidate.status)
        assertEquals(ReportValueSource.OCR, xCandidate.source)
        assertEquals("min", xCandidate.unit)
        assertEquals(10.0, xCandidate.points.first { it.value == 10.0 }.pixel)
        assertEquals(390.0, xCandidate.points.first { it.value == 25.0 }.pixel)
        assertEquals("counts", yCandidate.unit)
        assertEquals(8.0, yCandidate.points.first { it.value == 10_000.0 }.pixel)
        assertEquals(300.0, yCandidate.points.first { it.value == 0.0 }.pixel)
        val transform = assertNotNull(graph.axisCalibration?.pixelToUnitTransform)
        assertEquals("ocr-validated-linear-axis-fit", transform.method)
        assertEquals(0.039473684210526314, transform.xScale, 0.0000001)
        assertEquals(9.605263157894736, transform.xOffset, 0.0000001)
        assertEquals(-34.24657534246575, transform.yScale, 0.0000001)
        assertEquals(10_273.972602739726, transform.yOffset, 0.0000001)
        assertEquals(0.95625, graph.axisCalibration.calibrationConfidence ?: -1.0, 0.0000001)
        assertEquals(emptyList(), graph.axisCalibration.warnings)
    }

    @Test
    fun processingMetadataRejectsInvalidAxisCalibrationCandidate() {
        val config = buildProcessingReportMetadataConfig(
            sourcePath = """C:\input\raw_photo.jpg""",
            processedPath = """C:\input\normalized_photo.jpg""",
            sourceType = SourceType.PHOTO,
            graphIndex = 1,
            detectedGraphCount = 1,
            signalPointCount = 512,
            analysisStartedAtEpochMillis = 1_000L,
            analysisCompletedAtEpochMillis = 2_750L,
            detectedGraphBounds = PixelRect(100, 100, 400, 300),
            axisOcrResult = AxisOcrResult(
                rawElements = listOf(
                    OcrTextElement(
                        text = "10.00",
                        numericValue = 10f,
                        x = 390f,
                        y = 410f,
                        width = 20f,
                        height = 14f,
                        confidence = 0.78f,
                    ),
                    OcrTextElement(
                        text = "20.00",
                        numericValue = 20f,
                        x = 190f,
                        y = 410f,
                        width = 20f,
                        height = 14f,
                        confidence = 0.78f,
                    ),
                    OcrTextElement(
                        text = "30.00",
                        numericValue = 30f,
                        x = 540f,
                        y = 410f,
                        width = 20f,
                        height = 14f,
                        confidence = 0.78f,
                    ),
                ),
                suggestedXValues = listOf(10f, 20f, 30f),
                suggestedYValues = emptyList(),
                xUnit = "Time-->",
                yUnit = null,
                confidence = 0.82f,
                timestamp = 1_234L,
            ),
        )

        val graph = StoredReportMetadataCodec.decodeOrNull(config)?.graphs?.single()
        val xCandidate = graph?.axisCalibration?.calibrationCandidates
            .orEmpty()
            .single { it.candidateId == "ocr-x-axis" }

        assertEquals(AxisCalibrationCandidateStatus.REJECTED, xCandidate.status)
        assertTrue(xCandidate.rejectionReasons.any { it.contains("monotonic") })
        assertTrue(xCandidate.rejectionReasons.any { it.contains("spacing") })
        assertTrue(xCandidate.rejectionReasons.any { it.contains("visible graph range") })
        assertEquals(null, graph?.axisCalibration?.pixelToUnitTransform)
        assertEquals(null, graph?.axisCalibration?.calibrationConfidence)
        val warningCodes = graph?.axisCalibration?.warnings.orEmpty().map { it.code }
        assertTrue("axis.x.geometry_inconsistent" in warningCodes)
        assertTrue("axis.y.ticks_missing" in warningCodes)
        assertTrue("axis.y.candidate_missing" in warningCodes)
        assertTrue("axis.transform_missing" in warningCodes)
        assertEquals(ReportSeverity.SERIOUS, graph?.axisCalibration?.warnings?.single { it.code == "axis.transform_missing" }?.severity)
    }

    @Test
    fun processingMetadataWarnsAboutWeakOcrAndTiltedTickAlignment() {
        val config = buildProcessingReportMetadataConfig(
            sourcePath = """C:\input\raw_photo.jpg""",
            processedPath = """C:\input\normalized_photo.jpg""",
            sourceType = SourceType.PHOTO,
            graphIndex = 1,
            detectedGraphCount = 1,
            signalPointCount = 512,
            analysisStartedAtEpochMillis = 1_000L,
            analysisCompletedAtEpochMillis = 2_750L,
            detectedGraphBounds = PixelRect(100, 100, 400, 300),
            axisOcrResult = AxisOcrResult(
                rawElements = listOf(
                    OcrTextElement(
                        text = "10.00",
                        numericValue = 10f,
                        x = 105f,
                        y = 405f,
                        width = 20f,
                        height = 14f,
                        confidence = 0.62f,
                    ),
                    OcrTextElement(
                        text = "20.00",
                        numericValue = 20f,
                        x = 255f,
                        y = 435f,
                        width = 20f,
                        height = 14f,
                        confidence = 0.62f,
                    ),
                    OcrTextElement(
                        text = "30.00",
                        numericValue = 30f,
                        x = 405f,
                        y = 465f,
                        width = 20f,
                        height = 14f,
                        confidence = 0.62f,
                    ),
                    OcrTextElement(
                        text = "10000",
                        numericValue = 10_000f,
                        x = 55f,
                        y = 100f,
                        width = 40f,
                        height = 16f,
                        confidence = 0.62f,
                    ),
                    OcrTextElement(
                        text = "0",
                        numericValue = 0f,
                        x = 55f,
                        y = 392f,
                        width = 12f,
                        height = 16f,
                        confidence = 0.62f,
                    ),
                ),
                suggestedXValues = listOf(10f, 20f, 30f),
                suggestedYValues = listOf(10_000f, 0f),
                xUnit = "Time-->",
                yUnit = "Abundance",
                confidence = 0.62f,
                timestamp = 1_234L,
            ),
        )

        val graph = StoredReportMetadataCodec.decodeOrNull(config)?.graphs?.single()
        val warningCodes = graph?.axisCalibration?.warnings.orEmpty().map { it.code }

        assertNotNull(graph?.axisCalibration?.pixelToUnitTransform)
        assertTrue("axis.ocr_confidence_weak" in warningCodes)
        assertTrue("axis.x.tick_ocr_confidence_weak" in warningCodes)
        assertTrue("axis.y.tick_ocr_confidence_weak" in warningCodes)
        assertTrue("axis.x.alignment_tilt_suspected" in warningCodes)
    }

    @Test
    fun processingMetadataKeepsGraphRecordsIsolated() {
        val graph1Config = buildProcessingReportMetadataConfig(
            sourcePath = """C:\input\raw_photo.jpg""",
            processedPath = """C:\input\graph_1.png""",
            sourceType = SourceType.PHOTO,
            graphIndex = 1,
            detectedGraphCount = 2,
            signalPointCount = 101,
            analysisStartedAtEpochMillis = 1_000L,
            analysisCompletedAtEpochMillis = 2_000L,
            detectedGraphBounds = PixelRect(10, 20, 300, 180),
            preparationVariants = listOf(
                GraphPreparationVariantMetadata(
                    rank = 1,
                    configName = "graph_1_contrast",
                    inputVariant = "contrast",
                    score = 210.0,
                    selected = true,
                ),
            ),
            graphWarnings = listOf(
                ReportWarning(
                    code = "graph_1.warning",
                    message = "Graph 1 warning",
                    severity = ReportSeverity.WARNING,
                    stage = "graph_detection",
                    graphIndex = 2,
                ),
            ),
        )
        val graph2Config = buildProcessingReportMetadataConfig(
            sourcePath = """C:\input\raw_photo.jpg""",
            processedPath = """C:\input\graph_2.png""",
            sourceType = SourceType.PHOTO,
            graphIndex = 2,
            detectedGraphCount = 2,
            signalPointCount = 202,
            analysisStartedAtEpochMillis = 1_000L,
            analysisCompletedAtEpochMillis = 2_000L,
            detectedGraphBounds = PixelRect(20, 260, 320, 190),
            preparationVariants = listOf(
                GraphPreparationVariantMetadata(
                    rank = 1,
                    configName = "graph_2_scan_style",
                    inputVariant = "scan_style",
                    score = 230.0,
                    selected = true,
                ),
            ),
            graphWarnings = listOf(
                ReportWarning(
                    code = "graph_2.warning",
                    message = "Graph 2 warning",
                    severity = ReportSeverity.WARNING,
                    stage = "axis_ocr",
                    graphIndex = 1,
                ),
            ),
        )

        val graph1 = StoredReportMetadataCodec.decodeOrNull(graph1Config)?.graphs?.single()
        val graph2 = StoredReportMetadataCodec.decodeOrNull(graph2Config)?.graphs?.single()

        assertNotNull(graph1)
        assertNotNull(graph2)
        assertEquals(1, graph1.graphIndex)
        assertEquals(2, graph2.graphIndex)
        assertEquals(PixelRect(10, 20, 300, 180), graph1.source?.detectedGraphBounds)
        assertEquals(PixelRect(20, 260, 320, 190), graph2.source?.detectedGraphBounds)
        assertEquals("graph_1_contrast", graph1.source?.selectedPreparationVariant?.configName)
        assertEquals("graph_2_scan_style", graph2.source?.selectedPreparationVariant?.configName)
        assertEquals("graph_1.warning", graph1.warnings.single().code)
        assertEquals("graph_2.warning", graph2.warnings.single().code)
        assertEquals(1, graph1.warnings.single().graphIndex)
        assertEquals(2, graph2.warnings.single().graphIndex)
        assertTrue(graph1.source?.preprocessingSteps.orEmpty().contains("Digitized signal points: 101"))
        assertTrue(graph2.source?.preprocessingSteps.orEmpty().contains("Digitized signal points: 202"))
    }
}

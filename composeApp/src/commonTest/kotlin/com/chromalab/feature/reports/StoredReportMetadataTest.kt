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
import com.chromalab.feature.calculation.export.CalculationRunReportExporter
import com.chromalab.feature.processing.debug.RuntimeEvidencePackage
import com.chromalab.feature.processing.report.buildProcessingReportMetadataConfig
import com.chromalab.feature.processing.geometry.AxisCalibrationFit
import com.chromalab.feature.processing.geometry.CalibrationFitStatus
import com.chromalab.feature.processing.geometry.GeometryAxis
import com.chromalab.feature.processing.geometry.GeometryReportStatus
import com.chromalab.feature.processing.geometry.GeometryTrace
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.peaks.PeakLabelEvidence
import com.chromalab.feature.processing.peaks.PeakLabelEvidenceSource
import com.chromalab.feature.processing.peaks.PeakLabelEvidenceStatus
import com.chromalab.feature.processing.peaks.PeakLabelTextClassification
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidateFlag
import com.chromalab.feature.processing.peaks.RecoveredPeakCandidateStatus
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoredReportMetadataTest {
    private val json = Json {
        ignoreUnknownKeys = false
        encodeDefaults = true
    }

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
    fun codecRoundTripsGeometryTraceAndReportStatus() {
        val metadata = StoredReportMetadata(
            inputSourceType = InputSourceType.CAMERA_CAPTURE,
            sourceName = "geometry_trace_fixture.jpg",
            detectedGraphCount = 1,
            graphs = listOf(
                StoredGraphReportMetadata(
                    graphIndex = 1,
                    geometryReportStatus = GeometryReportStatus.DIAGNOSTIC_ONLY,
                    geometryTrace = GeometryTrace(
                        originalImagePath = "raw.jpg",
                        normalizedImagePath = "normalized.jpg",
                        warnings = listOf("geometry.calibration.invalid"),
                    ),
                    source = GraphSourceMetadata(
                        geometryReportStatus = GeometryReportStatus.DIAGNOSTIC_ONLY,
                        geometryTrace = GeometryTrace(
                            originalImagePath = "raw.jpg",
                            normalizedImagePath = "normalized.jpg",
                        ),
                    ),
                ),
            ),
        )

        val decoded = StoredReportMetadataCodec.decodeOrNull(StoredReportMetadataCodec.encode(metadata))

        assertNotNull(decoded)
        val graph = decoded.graphs.single()
        assertEquals(GeometryReportStatus.DIAGNOSTIC_ONLY, graph.geometryReportStatus)
        assertEquals("raw.jpg", graph.geometryTrace?.originalImagePath)
        assertEquals(GeometryReportStatus.DIAGNOSTIC_ONLY, graph.source?.geometryReportStatus)
        assertEquals("normalized.jpg", graph.source?.geometryTrace?.normalizedImagePath)
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

    @Test
    fun savedProcessingModelAuditSurvivesOptionsBuilderAndFinalReport() {
        val config = buildProcessingReportMetadataConfig(
            sourcePath = """C:\samples\mi8_photo.jpg""",
            processedPath = """C:\samples\mi8_photo_crop.jpg""",
            sourceType = SourceType.PHOTO,
            graphIndex = 1,
            detectedGraphCount = 1,
            signalPointCount = 3,
            analysisStartedAtEpochMillis = 1_000L,
            analysisCompletedAtEpochMillis = 2_500L,
            sourceImageBounds = PixelRect(0, 0, 900, 700),
            detectedGraphBounds = PixelRect(30, 50, 820, 560),
            cropConfidence = 0.88,
            selectedModel = ModelExecutionInfo(
                modelId = "qwen3-vl-2b-q4km",
                modelName = "Qwen3-VL 2B Q4_K_M",
                runtime = ExecutedRuntime.GGUF,
            ),
            executedModel = null,
            executedRuntime = ExecutedRuntime.UNKNOWN,
            deviceName = "Xiaomi Mi 8",
            stageTimings = listOf(
                ReportStageTiming("IMAGE_QUALITY", "IMAGE_QUALITY", 125L),
            ),
        )
        val chromatogram = chromatogramEntity(
            sourceType = SourceType.PHOTO,
            filePath = """C:\samples\mi8_photo.jpg""",
            algorithmConfig = config,
        )
        val run = calculationRun(
            peaks = listOf(
                peakResult(
                    peakId = 0,
                    rtApex = 1.0,
                    warning = "fixture peak",
                ),
            ),
        )

        val options = buildCalculationReportOptions(
            run = run,
            chromatogram = chromatogram,
            signal = null,
        )
        val report = CalculationRunReportMapper.map(run = run, options = options)
        val warningCodes = report.warnings.map { it.code }

        assertEquals(ProcessingMode.FULL_ANALYSIS, options.processingMode)
        assertEquals(ExecutedRuntime.UNKNOWN, options.executedRuntime)
        assertEquals("qwen3-vl-2b-q4km", options.selectedModel?.modelId)
        assertEquals("Xiaomi Mi 8", options.deviceName)
        assertEquals(125L, options.stageTimings.single { it.stageId == "IMAGE_QUALITY" }.durationMillis)
        assertTrue(options.additionalReportWarnings.any { it.code == "model.execution_missing" })
        assertTrue(options.additionalReportWarnings.any { it.code == "model.title_ion_axis.required_vision_failed" })
        assertEquals("qwen3-vl-2b-q4km", report.metadata.selectedModel?.modelId)
        assertNull(report.metadata.executedModel)
        assertTrue(warningCodes.contains("model.execution_missing"))
        assertTrue(warningCodes.contains("model.title_ion_axis.required_vision_failed"))
        assertTrue(warningCodes.contains("runtime.executed_unknown"))
        assertEquals(
            ReportSeverity.FAILED,
            report.warnings.single { it.code == "model.title_ion_axis.required_vision_failed" }.severity,
        )
        assertEquals(
            ReportSeverity.SERIOUS,
            report.warnings.single { it.code == "runtime.executed_unknown" }.severity,
        )
    }

    @Test
    fun reportMapperUsesLocalKnowledgeWithoutInventingKovatsRetentionTimes() {
        val report = CalculationRunReportMapper.map(
            run = calculationRun(
                peaks = listOf(
                    peakResult(
                        peakId = 0,
                        rtApex = 5.0,
                        warning = "fixture peak",
                    ),
                ),
            ),
            options = CalculationRunReportOptions(
                identification = ChromatogramIdentification(
                    chromatogramTitle = ReportTextValue(
                        value = "Ion 92.00 (91.70 to 92.70): BELIY TIGR_1.D\\data.ms",
                        status = ReportValueStatus.DETECTED,
                        confidence = 0.9,
                        source = ReportValueSource.OCR,
                    ),
                    analysisType = ReportTextValue(
                        value = "GC-MS",
                        status = ReportValueStatus.DETECTED,
                        confidence = 0.9,
                        source = ReportValueSource.OCR,
                    ),
                    chromatogramMode = ReportTextValue(
                        value = "EIC",
                        status = ReportValueStatus.DETECTED,
                        confidence = 0.9,
                        source = ReportValueSource.OCR,
                    ),
                    ionOrChannel = ReportTextValue(
                        value = "m/z 92.00",
                        status = ReportValueStatus.DETECTED,
                        confidence = 0.9,
                        source = ReportValueSource.OCR,
                    ),
                    ionRange = ReportTextValue(
                        value = "91.70 to 92.70",
                        status = ReportValueStatus.DETECTED,
                        confidence = 0.9,
                        source = ReportValueSource.OCR,
                    ),
                ),
            ),
        )

        val graph = report.graphs.single()

        assertEquals("Monocyclic alkylbenzenes", graph.interpretation.likelyCompoundClass.value)
        assertEquals(ReportValueStatus.INFERRED, graph.interpretation.likelyCompoundClass.status)
        assertEquals(ReportValueSource.LOCAL_KNOWLEDGE, graph.interpretation.likelyCompoundClass.source)
        assertTrue(graph.interpretation.domainContextNotes.any { it.contains("m/z 92") })
        assertTrue(graph.interpretation.unresolvedAssignments.any { it.contains("retention-index") })
        assertEquals(ReportValueStatus.INFERRED, graph.sectionStatus.chemicalInterpretation)
        assertEquals(ReportValueStatus.NOT_CALCULATED, graph.kovats.status)
        assertEquals("I = 100*z + 100*(RT(x)-RT(z))/(RT(z+1)-RT(z))", graph.kovats.formula)
        assertTrue(graph.kovats.referenceRetentionTimes.isEmpty())
        assertTrue(graph.kovats.missingDataNotes.any { it.contains("Measured n-paraffin reference retention times") })
    }

    @Test
    fun fixtureHintCannotProduceProductionReportableRecoveredPeak() {
        val report = CalculationRunReportMapper.map(
            run = calculationRun(
                peaks = emptyList(),
                rawPoints = localPeakSignal(apexRt = 5.61),
            ),
            options = CalculationRunReportOptions(
                graphSourceMetadata = GraphSourceMetadata(
                    geometryReportStatus = GeometryReportStatus.REVIEW_READY,
                    geometryTrace = recoveryTrace(
                        PeakLabelEvidence(
                            rawText = "5.610",
                            parsedRetentionTime = 5.610,
                            source = PeakLabelEvidenceSource.FIXTURE_HINT,
                            status = PeakLabelEvidenceStatus.VALID_TEXT,
                            textClassification = PeakLabelTextClassification.PEAK_ANNOTATION,
                            isRuntimeEvidence = false,
                        ),
                    ),
                ),
            ),
        )

        val recovery = report.graphs.single().peakRecovery

        assertEquals(0, recovery.runtimeRecoveredPeaks.size)
        assertEquals(1, recovery.testOnlyRecoveredPeaks.size)
        assertEquals(0, recovery.productionReportablePeaks)
        assertTrue(recovery.testOnlyRecoveredPeaks.single().flags.contains(RecoveredPeakCandidateFlag.FIXTURE_HINT_ONLY))
    }

    @Test
    fun runtimePeakLabelEvidenceRecoversReviewGradePeakOnlyAfterLocalSignalVerification() {
        val report = CalculationRunReportMapper.map(
            run = calculationRun(
                peaks = emptyList(),
                rawPoints = localPeakSignal(apexRt = 5.61),
            ),
            options = CalculationRunReportOptions(
                graphSourceMetadata = GraphSourceMetadata(
                    geometryReportStatus = GeometryReportStatus.REVIEW_READY,
                    geometryTrace = recoveryTrace(
                        PeakLabelEvidence(
                            rawText = "5.610",
                            parsedRetentionTime = 5.610,
                            labelBoxPx = GraphRegion(10, 10, 28, 12, "label"),
                            localCropPath = "peak_label_crops/label_5610.png",
                            source = PeakLabelEvidenceSource.ML_KIT,
                            confidence = 0.82f,
                            status = PeakLabelEvidenceStatus.VALID_TEXT,
                            textClassification = PeakLabelTextClassification.PEAK_ANNOTATION,
                            isRuntimeEvidence = true,
                        ),
                    ),
                ),
            ),
        )

        val recovered = report.graphs.single().peakRecovery.runtimeRecoveredPeaks.single()

        assertEquals(RecoveredPeakCandidateStatus.REVIEW, recovered.status)
        assertTrue(recovered.flags.contains(RecoveredPeakCandidateFlag.RUNTIME_OCR_VERIFIED))
        assertTrue(recovered.flags.contains(RecoveredPeakCandidateFlag.LABEL_EVIDENCE_VERIFIED))
        assertTrue(recovered.localSNR?.let { it > 1.5 } == true)
        assertEquals(1, report.graphs.single().peakRecovery.productionReportablePeaks)
        assertEquals(1, report.graphs.single().peakRecovery.reviewGradePeaks)
    }

    @Test
    fun runtimeRecoveredPeakRequiresLocalSignalMaximum() {
        val report = CalculationRunReportMapper.map(
            run = calculationRun(
                peaks = emptyList(),
                rawPoints = flatSignal(),
            ),
            options = CalculationRunReportOptions(
                graphSourceMetadata = GraphSourceMetadata(
                    geometryReportStatus = GeometryReportStatus.REVIEW_READY,
                    geometryTrace = recoveryTrace(
                        PeakLabelEvidence(
                            rawText = "8.560",
                            parsedRetentionTime = 8.560,
                            source = PeakLabelEvidenceSource.ML_KIT,
                            confidence = 0.86f,
                            status = PeakLabelEvidenceStatus.VALID_TEXT,
                            textClassification = PeakLabelTextClassification.PEAK_ANNOTATION,
                            isRuntimeEvidence = true,
                        ),
                    ),
                ),
            ),
        )

        val rejected = report.graphs.single().peakRecovery.rejectedRecoveredCandidates.single()

        assertEquals(RecoveredPeakCandidateStatus.REJECTED, rejected.status)
        assertTrue(rejected.flags.contains(RecoveredPeakCandidateFlag.FLAT_SIGNAL_REJECTED))
        assertEquals(0, report.graphs.single().peakRecovery.productionReportablePeaks)
    }

    @Test
    fun runtimeRecoveredPeakRequiresReviewOrValidCalibration() {
        val report = CalculationRunReportMapper.map(
            run = calculationRun(
                peaks = emptyList(),
                rawPoints = localPeakSignal(apexRt = 5.61),
            ),
            options = CalculationRunReportOptions(
                graphSourceMetadata = GraphSourceMetadata(
                    geometryReportStatus = GeometryReportStatus.REVIEW_READY,
                    geometryTrace = GeometryTrace(
                        peakLabelEvidence = listOf(
                            PeakLabelEvidence(
                                rawText = "5.610",
                                parsedRetentionTime = 5.610,
                                source = PeakLabelEvidenceSource.ML_KIT,
                                confidence = 0.86f,
                                status = PeakLabelEvidenceStatus.VALID_TEXT,
                                textClassification = PeakLabelTextClassification.PEAK_ANNOTATION,
                                isRuntimeEvidence = true,
                            ),
                        ),
                    ),
                ),
            ),
        )

        val rejected = report.graphs.single().peakRecovery.rejectedRecoveredCandidates.single()

        assertEquals(RecoveredPeakCandidateStatus.REJECTED, rejected.status)
        assertTrue(rejected.flags.contains(RecoveredPeakCandidateFlag.CALIBRATION_INVALID_REJECTED))
        assertEquals(0, report.graphs.single().peakRecovery.productionReportablePeaks)
    }

    @Test
    fun runtimeEvidencePackageContainsRuntimeCropPathsAndRecoveryCounts() {
        val run = calculationRun(
            peaks = emptyList(),
            rawPoints = localPeakSignal(apexRt = 5.61),
        )
        val options = CalculationRunReportOptions(
            graphSourceMetadata = GraphSourceMetadata(
                geometryReportStatus = GeometryReportStatus.REVIEW_READY,
                geometryTrace = recoveryTrace(
                    PeakLabelEvidence(
                        rawText = "5.610",
                        parsedRetentionTime = 5.610,
                        labelBoxPx = GraphRegion(10, 10, 28, 12, "label"),
                        cropBoundsPx = GraphRegion(8, 8, 80, 40, "crop"),
                        localCropPath = "/tmp/peak_label_crops/label_5610.png",
                        source = PeakLabelEvidenceSource.ML_KIT,
                        confidence = 0.84f,
                        status = PeakLabelEvidenceStatus.VALID_TEXT,
                        textClassification = PeakLabelTextClassification.PEAK_ANNOTATION,
                        isRuntimeEvidence = true,
                    ),
                ).copy(
                    peakLabelCropPaths = listOf("/tmp/peak_label_crops/label_5610.png"),
                    peakLabelCropBoundsOverlayPath = "/tmp/peak_label_crop_bounds_overlay.png",
                    peakLabelTextClassificationOverlayPath = "/tmp/peak_label_text_classification_overlay.png",
                    curveTextSuppressionOverlayPath = "/tmp/mask_text_suppression_overlay.png",
                ),
            ),
        )

        val evidencePackage = json.decodeFromString<RuntimeEvidencePackage>(
            CalculationRunReportExporter.exportRuntimeEvidencePackageJson(run, options),
        )
        val graph = evidencePackage.graphs.single()

        assertEquals(1, graph.peakLabelEvidence.size)
        assertEquals(PeakLabelEvidenceSource.ML_KIT, graph.peakLabelEvidence.single().source)
        assertEquals("/tmp/peak_label_crops/label_5610.png", graph.peakLabelEvidence.single().localCropPath)
        assertEquals(1, graph.runtimeRecoveredPeaks.size)
        assertEquals(0, graph.testOnlyRecoveredPeaks.size)
        assertEquals(1, graph.summaryCounts.runtimeRecoveredPeaks)
        assertEquals(1, graph.summaryCounts.productionReportablePeaks)
        assertEquals("/tmp/peak_label_crop_bounds_overlay.png", graph.artifactPaths.peakLabelCropBoundsOverlayPath)
        assertEquals("/tmp/mask_text_suppression_overlay.png", graph.artifactPaths.textSuppressionOverlayPath)
        assertTrue(graph.productionRuntimeEvidenceOnly)
    }

    @Test
    fun vlmLocalCropEvidenceCanRecoverReviewPeakButDoesNotProvidePeakMetrics() {
        val report = CalculationRunReportMapper.map(
            run = calculationRun(
                peaks = emptyList(),
                rawPoints = localPeakSignal(apexRt = 8.56),
            ),
            options = CalculationRunReportOptions(
                graphSourceMetadata = GraphSourceMetadata(
                    geometryReportStatus = GeometryReportStatus.REVIEW_READY,
                    geometryTrace = recoveryTrace(
                        PeakLabelEvidence(
                            rawText = "8.560",
                            parsedRetentionTime = 8.560,
                            localCropPath = "peak_label_crops/vlm_8560.png",
                            source = PeakLabelEvidenceSource.VLM,
                            confidence = 0.72f,
                            status = PeakLabelEvidenceStatus.VALID_TEXT,
                            textClassification = PeakLabelTextClassification.PEAK_ANNOTATION,
                            isRuntimeEvidence = true,
                            warnings = listOf("peak_label_ocr.vlm_text_only_no_peak_metrics"),
                        ),
                    ),
                ),
            ),
        )

        val recovered = report.graphs.single().peakRecovery.runtimeRecoveredPeaks.single()

        assertEquals(RecoveredPeakCandidateStatus.REVIEW, recovered.status)
        assertEquals(PeakLabelEvidenceSource.VLM, recovered.sourceEvidence?.source)
        assertTrue(recovered.flags.contains(RecoveredPeakCandidateFlag.RUNTIME_OCR_VERIFIED))
        assertTrue(recovered.flags.contains(RecoveredPeakCandidateFlag.LABEL_EVIDENCE_VERIFIED))
        assertNotNull(recovered.localHeight)
        assertNotNull(recovered.localSNR)
        assertNotNull(recovered.localProminence)
    }

    @Test
    fun mlKitAndVlmDuplicateLabelDoesNotCreateDuplicateRecoveredPeaks() {
        val report = CalculationRunReportMapper.map(
            run = calculationRun(
                peaks = emptyList(),
                rawPoints = localPeakSignal(apexRt = 5.61),
            ),
            options = CalculationRunReportOptions(
                graphSourceMetadata = GraphSourceMetadata(
                    geometryReportStatus = GeometryReportStatus.REVIEW_READY,
                    geometryTrace = recoveryTrace(
                        PeakLabelEvidence(
                            rawText = "5.610",
                            parsedRetentionTime = 5.610,
                            source = PeakLabelEvidenceSource.ML_KIT,
                            confidence = 0.82f,
                            status = PeakLabelEvidenceStatus.VALID_TEXT,
                            textClassification = PeakLabelTextClassification.PEAK_ANNOTATION,
                            isRuntimeEvidence = true,
                        ),
                        PeakLabelEvidence(
                            rawText = "5.610",
                            parsedRetentionTime = 5.610,
                            source = PeakLabelEvidenceSource.VLM,
                            confidence = 0.76f,
                            status = PeakLabelEvidenceStatus.VALID_TEXT,
                            textClassification = PeakLabelTextClassification.PEAK_ANNOTATION,
                            isRuntimeEvidence = true,
                        ),
                    ),
                ),
            ),
        )

        val recovery = report.graphs.single().peakRecovery

        assertEquals(1, recovery.runtimeRecoveredPeaks.size)
        assertEquals(1, recovery.rejectedRecoveredCandidates.size)
        assertTrue(recovery.rejectedRecoveredCandidates.single().flags.contains(RecoveredPeakCandidateFlag.DUPLICATE_REJECTED))
        assertEquals(1, recovery.productionReportablePeaks)
    }

    @Test
    fun runtimeRecoveryDoesNotDuplicateExistingDetectedPeak() {
        val report = CalculationRunReportMapper.map(
            run = calculationRun(
                peaks = listOf(peakResult(1, 5.61, "existing")),
                rawPoints = localPeakSignal(apexRt = 5.61),
            ),
            options = CalculationRunReportOptions(
                graphSourceMetadata = GraphSourceMetadata(
                    geometryReportStatus = GeometryReportStatus.REVIEW_READY,
                    geometryTrace = recoveryTrace(
                        PeakLabelEvidence(
                            rawText = "5.610",
                            parsedRetentionTime = 5.610,
                            source = PeakLabelEvidenceSource.ML_KIT,
                            confidence = 0.90f,
                            status = PeakLabelEvidenceStatus.VALID_TEXT,
                            textClassification = PeakLabelTextClassification.PEAK_ANNOTATION,
                            isRuntimeEvidence = true,
                        ),
                    ),
                ),
            ),
        )

        val recovery = report.graphs.single().peakRecovery

        assertEquals(0, recovery.runtimeRecoveredPeaks.size)
        assertEquals(1, recovery.rejectedRecoveredCandidates.size)
        assertTrue(recovery.rejectedRecoveredCandidates.single().flags.contains(RecoveredPeakCandidateFlag.DUPLICATE_REJECTED))
        assertEquals(1, recovery.productionReportablePeaks)
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
        rawPoints: List<SignalPoint> = listOf(
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
    ): CalculationRun =
        CalculationRun(
            id = "run-1",
            sourceSignalId = "signal-1",
            pipelineVersion = "test",
            algorithmVersion = "test",
            params = CalculationParams(),
            validation = ValidationResult(
                isValid = true,
                pointCount = rawPoints.size,
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
                raw = rawPoints,
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

    private fun recoveryTrace(vararg evidence: PeakLabelEvidence): GeometryTrace =
        GeometryTrace(
            peakLabelEvidence = evidence.toList(),
            xCalibrationFit = AxisCalibrationFit(
                axis = GeometryAxis.X,
                status = CalibrationFitStatus.REVIEW,
                confidence = 0.62f,
            ),
            yCalibrationFit = AxisCalibrationFit(
                axis = GeometryAxis.Y,
                status = CalibrationFitStatus.REVIEW,
                confidence = 0.62f,
            ),
        )

    private fun localPeakSignal(apexRt: Double): List<SignalPoint> =
        (0..100).map { index ->
            val time = index / 10.0
            val distance = kotlin.math.abs(time - apexRt)
            val intensity = 10.0 + 90.0 * kotlin.math.exp(-(distance * distance) / 0.012)
            SignalPoint(
                index = index,
                time = time,
                intensity = intensity,
                pixelX = index,
                pixelY = 100.0 - intensity,
                confidence = 0.95,
                isInterpolated = false,
            )
        }

    private fun flatSignal(): List<SignalPoint> =
        (0..100).map { index ->
            SignalPoint(
                index = index,
                time = index / 10.0,
                intensity = 10.0,
                pixelX = index,
                pixelY = 90.0,
                confidence = 0.95,
                isInterpolated = false,
            )
        }

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

package com.chromalab.feature.calculation.flow

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chromalab.core.ui.theme.Spacing
import com.chromalab.core.data.DatabaseProvider
import com.chromalab.core.data.entity.ChromatogramEntity
import com.chromalab.feature.processing.signal.DigitalSignal
import com.chromalab.feature.processing.signal.GraphPoint
import com.chromalab.feature.processing.signal.SignalMetadata
import com.chromalab.feature.calculation.core.CalculationEngine
import com.chromalab.feature.calculation.core.CalculationRun
import com.chromalab.feature.calculation.core.CalculationParams
import com.chromalab.feature.calculation.core.PeakResult
import com.chromalab.feature.calculation.algorithm.*
import com.chromalab.feature.calculation.ui.AlgorithmSettings
import com.chromalab.feature.calculation.ui.AlgorithmSettingsPanel
import com.chromalab.feature.calculation.ui.BaselineMethodOption
import com.chromalab.feature.calculation.ui.NoiseMethodOption
import com.chromalab.feature.calculation.ui.PeakDetailsContent
import com.chromalab.feature.calculation.ui.PeakDetailsData
import com.chromalab.feature.calculation.screen.ResultsSummaryScreen
import com.chromalab.feature.calculation.screen.ExportCalculationScreen
import com.chromalab.feature.calculation.export.CalculationRunReportExporter
import com.chromalab.feature.processing.export.FileSharer
import com.chromalab.feature.processing.geometry.GeometryStageTiming
import com.chromalab.feature.calculation.algorithm.DistributionAnalyzer
import com.chromalab.feature.calculation.algorithm.PatternAnalyzer
import com.chromalab.feature.calculation.algorithm.MethodQualityAnalyzer
import com.chromalab.feature.calculation.algorithm.GeochemicalCalculator
import com.chromalab.feature.calculation.algorithm.CompoundSource
import com.chromalab.feature.reports.CalculationRunReportOptions
import com.chromalab.feature.reports.ChromatogramReport
import com.chromalab.feature.reports.InputSourceType
import com.chromalab.feature.reports.StoredReportMetadata
import com.chromalab.feature.reports.StructuredReportPreview
import com.chromalab.feature.reports.buildCalculationReportOptions
import com.chromalab.feature.reports.StoredReportMetadataCodec
import com.chromalab.feature.validation.AutonomousValidationArtifactRecord
import com.chromalab.feature.validation.AutonomousValidationArtifactExporter
import com.chromalab.feature.validation.AutonomousValidationFixtureContracts
import com.chromalab.feature.validation.AutonomousValidationRunArtifactManifest
import com.chromalab.feature.validation.ValidationArtifactSaveResult
import com.chromalab.feature.validation.WHITE_TIGER_ION71_FIXTURE_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer

// ─── Premium overlay palette (matches AutoProgressOverlay) ──────
private val AnalysisSurface    = Color(0xFF0F172A)
private val AnalysisSurfaceAlt = Color(0xFF1E293B)
private val AnalysisGradStart  = Color(0xFF0D9488)
private val AnalysisGradMid    = Color(0xFF6D28D9)
private val AnalysisGradEnd    = Color(0xFF7C3AED)
private val AnalysisGlow       = Color(0xFF2DD4BF)
private val AnalysisDone       = Color(0xFF10B981)
private val AnalysisAccent     = Color(0xFFF59E0B)
private val AnalysisTrack      = Color(0xFF334155)
private val AnalysisText       = Color(0xFFF1F5F9)
private val AnalysisTextDim    = Color(0xFF94A3B8)

/**
 * Analysis flow — fully automatic.
 *
 * Flow: load signal → auto-calculate → show report.
 * No manual steps. The user sees a premium loading overlay,
 * then the full results report with interactive chart and peak details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisFlowScreen(
    signalId: String,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // ─── State ───
    var signal by remember { mutableStateOf<DigitalSignal?>(null) }
    var sourceChromatogram by remember { mutableStateOf<ChromatogramEntity?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var calculationRun by remember { mutableStateOf<CalculationRun?>(null) }
    var calcPhase by remember { mutableStateOf("Загрузка сигнала…") }
    var showExport by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var resultMode by remember { mutableStateOf(AnalysisResultMode.REPORT) }
    var algorithmSettings by remember { mutableStateOf(AlgorithmSettings.defaults()) }
    var recalculationKey by remember { mutableIntStateOf(0) }
    var activeSignalId by remember(signalId) { mutableStateOf(signalId) }
    var graphEntries by remember { mutableStateOf<List<AnalysisGraphEntry>>(emptyList()) }

    // Peak details state
    var selectedPeakIndex by remember { mutableStateOf(-1) }
    var showPeakSheet by remember { mutableStateOf(false) }

    // ─── Auto-pipeline: load signal → run calculation ───
    LaunchedEffect(activeSignalId, recalculationKey) {
        val id = activeSignalId.toLongOrNull()
        if (id == null) {
            loadError = "Некорректный ID сигнала"
            return@LaunchedEffect
        }

        loadError = null
        sourceChromatogram = null
        calculationRun = null
        showExport = false
        resultMode = AnalysisResultMode.REPORT
        graphEntries = emptyList()
        selectedPeakIndex = -1
        showPeakSheet = false

        // Phase 1: Load from Room
        calcPhase = "Загрузка сигнала…"
        try {
            val loaded = withContext(Dispatchers.IO) {
                DatabaseProvider.getDatabase().chromatogramDao().getById(id)
            }
            if (loaded == null) {
                loadError = "Сигнал ID=$id не найден в базе"
                return@LaunchedEffect
            }

            sourceChromatogram = loaded
            graphEntries = withContext(Dispatchers.IO) {
                loadAnalysisGraphEntries(loaded)
            }

            val json = loaded.dataPoints
            if (json == null) {
                loadError = "Нет данных в записи ID=$id"
                return@LaunchedEffect
            }

            val points = Json.decodeFromString(
                ListSerializer(GraphPoint.serializer()), json,
            )
            val sig = DigitalSignal(
                points = points,
                timeUnit = "мин",
                intensityUnit = loaded.intensityUnit ?: "mAU",
                metadata = SignalMetadata(
                    sourceImage = loaded.filePath ?: "",
                    totalPoints = points.size,
                    duplicatesRemoved = 0,
                    gapCount = 0,
                    sortValid = true,
                    timestamp = loaded.createdAt,
                ),
            )
            signal = sig

            // Phase 2: Smoothing + Baseline
            calcPhase = "Сглаживание и baseline…"
            val run = withContext(Dispatchers.Default) {
                CalculationEngine.execute(
                    signal = sig,
                    sourceId = activeSignalId,
                    params = algorithmSettings.toCalculationParams(),
                )
            }

            // Phase 3: Distribution analysis
            calcPhase = "Статистика распределения…"
            val dist = withContext(Dispatchers.Default) {
                DistributionAnalyzer.analyze(run.peaks)
            }

            // Phase 4: Pattern analysis
            calcPhase = "Анализ паттернов…"
            val patt = withContext(Dispatchers.Default) {
                PatternAnalyzer.analyze(run.peaks, run.signals)
            }

            // Phase 5: Method quality
            calcPhase = "Оценка качества метода…"
            val mq = withContext(Dispatchers.Default) {
                MethodQualityAnalyzer.analyze(run.peaks, run.signals)
            }

            // Phase 6: Geochemistry + auto-label
            calcPhase = "Геохимические индексы…"
            val sortedPeaks = run.peaks.sortedBy { it.rtApex }
            val geochem = if (patt?.homologousSeries?.detected == true) {
                withContext(Dispatchers.Default) {
                    GeochemicalCalculator.calculate(sortedPeaks)
                }
            } else null

            // Auto-label peaks with carbon numbers if series detected
            val labeledPeaks = if (geochem != null) {
                sortedPeaks.mapIndexed { i, p ->
                    val cn = geochem.firstCarbonNumber + i
                    p.copy(
                        compoundName = "C$cn",
                        compoundSource = CompoundSource.AUTO_SERIES,
                    )
                }
            } else run.peaks

            calculationRun = run.copy(
                peaks = labeledPeaks,
                distribution = dist,
                pattern = patt,
                methodQuality = mq,
                geochemistry = geochem,
            )

        } catch (e: Exception) {
            e.printStackTrace()
            loadError = "Ошибка анализа: ${e.message}"
        }
    }

    // ─── UI ───
    val isCalculating = signal == null || (loadError == null && calculationRun == null)

    Scaffold(
        bottomBar = {
            // Show bottom bar only when report is ready
            if (calculationRun != null && !showExport) {
                Surface(
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Filled.ArrowBack, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Назад")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        OutlinedButton(
                            onClick = { showSettings = true },
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Filled.Tune, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Настройки")
                        }
                        Button(
                            onClick = { showExport = true },
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Filled.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Экспорт")
                        }
                    }
                }
            } else if (showExport) {
                Surface(
                    tonalElevation = 3.dp,
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        OutlinedButton(
                            onClick = { showExport = false },
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text("← Отчёт")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = onFinish,
                            modifier = Modifier.height(48.dp),
                        ) {
                            Icon(Icons.Filled.Done, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Завершить")
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                // Error state
                loadError != null -> {
                    ErrorContent(
                        message = loadError!!,
                        onRetry = onCancel,
                    )
                }

                // Calculating — premium overlay
                isCalculating -> {
                    AnalysisProgressOverlay(
                        phase = calcPhase,
                    )
                }

                // Report ready
                calculationRun != null -> {
                    AnimatedContent(
                        targetState = showExport,
                        transitionSpec = {
                            if (targetState) {
                                slideInHorizontally { it / 3 } + fadeIn() togetherWith
                                    slideOutHorizontally { -it / 3 } + fadeOut()
                            } else {
                                slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                                    slideOutHorizontally { it / 3 } + fadeOut()
                            }
                        },
                        label = "report_export",
                    ) { isExport ->
                        if (isExport) {
                            ExportCalculationScreen(
                                run = calculationRun!!,
                                modifier = Modifier.fillMaxSize(),
                                reportOptions = buildCalculationReportOptions(
                                    run = calculationRun!!,
                                    chromatogram = sourceChromatogram,
                                    signal = signal,
                                ),
                            )
                        } else {
                            Column(modifier = Modifier.fillMaxSize()) {
                                GraphResultSwitcher(
                                    entries = graphEntries,
                                    activeSignalId = activeSignalId,
                                    onSelect = { entry ->
                                        if (entry.id.toString() != activeSignalId) {
                                            activeSignalId = entry.id.toString()
                                        }
                                    },
                                )
                                AnalysisResultModeSwitcher(
                                    mode = resultMode,
                                    onModeChange = { resultMode = it },
                                )
                                when (resultMode) {
                                    AnalysisResultMode.REPORT -> AnalysisStructuredReportScreen(
                                        run = calculationRun!!,
                                        chromatogram = sourceChromatogram,
                                        signal = signal,
                                        modifier = Modifier.weight(1f),
                                    )
                                    AnalysisResultMode.DIAGNOSTICS -> ResultsSummaryScreen(
                                        run = calculationRun!!,
                                        onPeakTap = { idx ->
                                            selectedPeakIndex = idx
                                            showPeakSheet = true
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Peak details bottom sheet
    if (showPeakSheet && calculationRun != null && selectedPeakIndex in calculationRun!!.peaks.indices) {
        ModalBottomSheet(
            onDismissRequest = { showPeakSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            val peakData = remember(selectedPeakIndex, calculationRun) {
                buildPeakDetailsData(calculationRun!!.peaks[selectedPeakIndex])
            }
            PeakDetailsContent(
                data = peakData,
                onRejectPeak = {
                    showPeakSheet = false
                },
            )
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            AlgorithmSettingsPanel(
                settings = algorithmSettings,
                onSettingsChange = { settings ->
                    algorithmSettings = settings
                },
                onRecalculate = {
                    showSettings = false
                    recalculationKey++
                },
                onReset = {
                    algorithmSettings = AlgorithmSettings.defaults()
                    showSettings = false
                    recalculationKey++
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─── Result report surface ──────────────────────────────────────

private enum class AnalysisResultMode {
    REPORT,
    DIAGNOSTICS,
}

@Composable
private fun AnalysisResultModeSwitcher(
    mode: AnalysisResultMode,
    onModeChange: (AnalysisResultMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        AnalysisResultModeButton(
            selected = mode == AnalysisResultMode.REPORT,
            label = "Отчет",
            icon = Icons.Filled.Description,
            onClick = { onModeChange(AnalysisResultMode.REPORT) },
            modifier = Modifier.weight(1f),
        )
        AnalysisResultModeButton(
            selected = mode == AnalysisResultMode.DIAGNOSTICS,
            label = "Метрики",
            icon = Icons.Filled.Insights,
            onClick = { onModeChange(AnalysisResultMode.DIAGNOSTICS) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AnalysisResultModeButton(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content: @Composable RowScope.() -> Unit = {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            content = content,
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            content = content,
        )
    }
}

@Composable
private fun AnalysisStructuredReportScreen(
    run: CalculationRun,
    chromatogram: ChromatogramEntity?,
    signal: DigitalSignal?,
    modifier: Modifier = Modifier,
) {
    val reportOptions = remember(run, chromatogram, signal) {
        buildCalculationReportOptions(
            run = run,
            chromatogram = chromatogram,
            signal = signal,
        )
    }
    val report = remember(run, reportOptions) {
        CalculationRunReportExporter.buildReport(run, reportOptions)
    }
    val validation = remember(run, reportOptions) {
        CalculationRunReportExporter.validate(run, reportOptions)
    }
    val uiContract = remember(run, reportOptions) {
        CalculationRunReportExporter.buildUiContract(run, reportOptions)
    }
    val graphOverlays = remember(run, report, reportOptions) {
        val graphIndex = report.graphs.singleOrNull()?.graphIndex ?: reportOptions.graphIndex.coerceAtLeast(1)
        mapOf(
            graphIndex to CalculationToChartMapper.buildChartState(
                run = run,
                visibleLayers = setOf("raw", "baseline", "corrected"),
            ),
        )
    }
    LaunchedEffect(run.id, report.metadata.reportId) {
        withContext(Dispatchers.IO) {
            runCatching {
                val evidenceJson = CalculationRunReportExporter.exportRuntimeEvidencePackageJson(run, reportOptions)
                val validationJson = CalculationRunReportExporter.exportRuntimeEvidenceValidationJson(run, reportOptions)
                val validationMarkdown = CalculationRunReportExporter.exportRuntimeEvidenceValidationMarkdown(run, reportOptions)
                val runtimeEvidenceArtifacts = listOf(
                    ValidationTextArtifact(
                        slot = "runtime_evidence_package",
                        fileName = "runtime_evidence_package_${run.id}.json",
                        content = evidenceJson,
                        mimeType = "application/json",
                    ),
                    ValidationTextArtifact(
                        slot = "runtime_evidence_validation_json",
                        fileName = "runtime_evidence_validation_${run.id}.json",
                        content = validationJson,
                        mimeType = "application/json",
                    ),
                    ValidationTextArtifact(
                        slot = "runtime_evidence_validation_markdown",
                        fileName = "runtime_evidence_validation_${run.id}.md",
                        content = validationMarkdown,
                        mimeType = "text/markdown",
                    ),
                )
                runtimeEvidenceArtifacts.forEach { artifact ->
                    val result = FileSharer.saveText(artifact.fileName, artifact.content, artifact.mimeType)
                    println("ANALYSIS[RUNTIME_EVIDENCE_EXPORT] ${artifact.fileName} success=${result.success} location=${result.location ?: result.message}")
                }
                if (report.metadata.inputSourceType == InputSourceType.VALIDATION_FIXTURE) {
                    exportAutonomousValidationArtifacts(
                        run = run,
                        report = report,
                        reportOptions = reportOptions,
                        runtimeEvidenceArtifacts = runtimeEvidenceArtifacts,
                    )
                }
            }.onFailure { error ->
                println("ANALYSIS[RUNTIME_EVIDENCE_EXPORT] failed=${error.message}")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        StructuredReportPreview(
            report = report,
            validation = validation,
            graphOverlays = graphOverlays,
            uiContract = uiContract,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
    }
}

private val validationArtifactJson = Json {
    prettyPrint = true
    encodeDefaults = true
}

private data class ValidationTextArtifact(
    val slot: String,
    val fileName: String,
    val content: String,
    val mimeType: String,
)

private data class ValidationOverlayArtifact(
    val slot: String,
    val graphIndex: Int,
    val sourcePath: String?,
)

private fun exportAutonomousValidationArtifacts(
    run: CalculationRun,
    report: ChromatogramReport,
    reportOptions: CalculationRunReportOptions,
    runtimeEvidenceArtifacts: List<ValidationTextArtifact>,
) {
    val runId = AutonomousValidationArtifactExporter.activeRunId()
    if (runId.isNullOrBlank()) {
        println("ANALYSIS[VALIDATION_FIXTURE_EXPORT] skipped=no_active_validation_run")
        return
    }

    val textArtifacts = runtimeEvidenceArtifacts + listOf(
        ValidationTextArtifact(
            slot = "final_report_contract_json",
            fileName = "final_report_contract_${run.id}.json",
            content = CalculationRunReportExporter.exportReportContractJson(run, reportOptions),
            mimeType = "application/json",
        ),
        ValidationTextArtifact(
            slot = "report_html",
            fileName = "report_${run.id}.html",
            content = CalculationRunReportExporter.exportHtml(run, reportOptions),
            mimeType = "text/html",
        ),
        ValidationTextArtifact(
            slot = "report_markdown",
            fileName = "report_${run.id}.md",
            content = CalculationRunReportExporter.exportMarkdown(run, reportOptions),
            mimeType = "text/markdown",
        ),
        ValidationTextArtifact(
            slot = "stage_timings",
            fileName = "stage_timings_${run.id}.json",
            content = report.stageTimingsJson(),
            mimeType = "application/json",
        ),
        ValidationTextArtifact(
            slot = "log_summary",
            fileName = "log_summary_${run.id}.md",
            content = report.validationLogSummary(runId, run.id),
            mimeType = "text/markdown",
        ),
    )

    val textRecords = textArtifacts.map { artifact ->
        AutonomousValidationArtifactExporter
            .saveTextArtifact(artifact.fileName, artifact.content, artifact.mimeType)
            .toArtifactRecord(artifact.slot, artifact.fileName)
            .also { record ->
                println(
                    "ANALYSIS[VALIDATION_FIXTURE_EXPORT] ${artifact.fileName} " +
                        "available=${record.available} location=${record.location ?: record.missingReason}",
                )
            }
    }

    val overlayRecords = report.validationOverlayArtifacts().map { artifact ->
        val fileName = "graph_${artifact.graphIndex}_${artifact.slot}.${artifact.sourcePath.extensionOrDefault("png")}"
        AutonomousValidationArtifactExporter
            .copyFileArtifact(artifact.sourcePath, fileName, artifact.sourcePath.mimeTypeOrDefault())
            .toArtifactRecord(artifact.slot, fileName)
            .also { record ->
                println(
                    "ANALYSIS[VALIDATION_FIXTURE_EXPORT] ${fileName} " +
                        "available=${record.available} location=${record.location ?: record.missingReason}",
                )
            }
    }

    val manifestFileName = "artifact_manifest_${run.id}.json"
    val manifestRecord = AutonomousValidationArtifactRecord(
        slot = "artifact_manifest",
        fileName = manifestFileName,
        available = true,
        location = "${validationPublicDirectory(runId)}/$manifestFileName",
    )
    val manifest = AutonomousValidationRunArtifactManifest(
        runId = runId,
        fixtureId = validationFixtureIdFromRunId(runId),
        publicArtifactDirectory = validationPublicDirectory(runId),
        records = completeRequiredValidationSlots(textRecords + overlayRecords + manifestRecord),
    )
    val manifestJson = validationArtifactJson.encodeToString(
        AutonomousValidationRunArtifactManifest.serializer(),
        manifest,
    )
    val manifestResult = AutonomousValidationArtifactExporter.saveTextArtifact(
        manifestFileName,
        manifestJson,
        "application/json",
    )
    println(
        "ANALYSIS[VALIDATION_FIXTURE_EXPORT] $manifestFileName " +
            "success=${manifestResult.success} location=${manifestResult.location ?: manifestResult.message}",
    )
}

private fun ChromatogramReport.stageTimingsJson(): String =
    validationArtifactJson.encodeToString(
        ListSerializer(GeometryStageTiming.serializer()),
        graphs.flatMap { graph ->
            graph.source.geometryTrace?.timings.orEmpty()
        },
    )

private fun ChromatogramReport.validationOverlayArtifacts(): List<ValidationOverlayArtifact> {
    if (graphs.isEmpty()) {
        return AutonomousValidationFixtureContracts.requiredOverlayArtifactSlots.map { slot ->
            ValidationOverlayArtifact(slot = slot, graphIndex = 0, sourcePath = null)
        }
    }
    return graphs.flatMap { graph ->
        val trace = graph.source.geometryTrace
        listOf(
            ValidationOverlayArtifact("graph_panel_overlay", graph.graphIndex, trace?.selectedGraphPanelOverlayPath),
            ValidationOverlayArtifact("plot_area_overlay", graph.graphIndex, trace?.selectedPlotAreaOverlayPath),
            ValidationOverlayArtifact("axis_tick_overlay", graph.graphIndex, trace?.tickOverlayPath ?: trace?.axisOverlayPath),
            ValidationOverlayArtifact("calibration_overlay", graph.graphIndex, trace?.calibrationFitOverlayPath),
            ValidationOverlayArtifact(
                "trace_overlay",
                graph.graphIndex,
                trace?.finalCenterlineOverlayPath ?: trace?.curveSelectedComponentPath ?: trace?.curveSkeletonPath,
            ),
            ValidationOverlayArtifact("peak_overlay", graph.graphIndex, trace?.finalCenterlineOverlayPath),
        )
    }
}

private fun ValidationArtifactSaveResult.toArtifactRecord(
    slot: String,
    fileName: String,
): AutonomousValidationArtifactRecord =
    AutonomousValidationArtifactRecord(
        slot = slot,
        fileName = fileName,
        available = success,
        location = location,
        missingReason = if (success) null else message,
    )

private fun completeRequiredValidationSlots(
    records: List<AutonomousValidationArtifactRecord>,
): List<AutonomousValidationArtifactRecord> {
    val existingSlots = records.map { it.slot }.toSet()
    val requiredSlots = AutonomousValidationFixtureContracts.requiredTextArtifactSlots +
        AutonomousValidationFixtureContracts.requiredOverlayArtifactSlots +
        AutonomousValidationFixtureContracts.requiredSupplementalArtifactSlots
    return records + requiredSlots
        .filterNot(existingSlots::contains)
        .map { slot ->
            AutonomousValidationArtifactRecord(
                slot = slot,
                fileName = "$slot.missing",
                available = false,
                missingReason = "Artifact slot was not produced by the validation fixture export path.",
            )
        }
}

private fun validationPublicDirectory(runId: String): String =
    "/sdcard/Download/ChromaLab/validation/$runId"

private fun ChromatogramReport.validationLogSummary(
    runId: String,
    calculationRunId: String,
): String = buildString {
    appendLine("# Autonomous Validation Fixture Log Summary")
    appendLine()
    appendLine("- Run id: `$runId`")
    appendLine("- Fixture id: `${validationFixtureIdFromRunId(runId)}`")
    appendLine("- Calculation run id: `$calculationRunId`")
    appendLine("- Report id: `${metadata.reportId}`")
    appendLine("- Input source: `${metadata.inputSourceType}`")
    appendLine("- Runtime failure class: `${metadata.runtimeFailureClass ?: "none"}`")
    appendLine("- Graphs: ${graphs.size}")
    appendLine()
    appendLine("## Stage Timings")
    appendLine()
    appendLine("| Graph | Stage | Duration ms |")
    appendLine("| ---: | --- | ---: |")
    val rows = graphs.flatMap { graph ->
        graph.source.geometryTrace?.timings.orEmpty().map { timing ->
            Triple(graph.graphIndex, timing.stageId, timing.durationMillis)
        }
    }
    if (rows.isEmpty()) {
        appendLine("| - | no geometry timings exported | - |")
    } else {
        rows.forEach { (graphIndex, stageId, durationMillis) ->
            appendLine("| $graphIndex | `$stageId` | $durationMillis |")
        }
    }
    appendLine()
    appendLine("Full logcat capture is intentionally external to the app and remains a diagnostic-only artifact.")
}

private fun validationFixtureIdFromRunId(runId: String): String =
    runId.replace(Regex("""_\d{8}_\d{6}$"""), "")
        .ifBlank { WHITE_TIGER_ION71_FIXTURE_ID }

private fun String?.extensionOrDefault(default: String): String {
    val extension = this
        ?.substringAfterLast('.', missingDelimiterValue = default)
        ?.lowercase()
        ?.filter { it.isLetterOrDigit() }
        ?.takeIf { it.isNotBlank() && it.length <= 5 }
    return extension ?: default
}

private fun String?.mimeTypeOrDefault(): String =
    when (extensionOrDefault("png")) {
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "json" -> "application/json"
        "md" -> "text/markdown"
        "html" -> "text/html"
        else -> "image/png"
    }

@Composable
private fun GraphResultSwitcher(
    entries: List<AnalysisGraphEntry>,
    activeSignalId: String,
    onSelect: (AnalysisGraphEntry) -> Unit,
) {
    if (entries.size <= 1) return

    val totalGraphs = entries.maxOfOrNull { it.detectedGraphCount } ?: entries.size
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Text(
                text = "Multi-graph result: ${entries.size}/$totalGraphs reports",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                entries.forEach { entry ->
                    val active = entry.id.toString() == activeSignalId
                    val label = "Graph ${entry.graphIndex}"
                    if (active) {
                        Button(
                            onClick = { onSelect(entry) },
                            modifier = Modifier.height(40.dp),
                        ) {
                            Text(label)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSelect(entry) },
                            modifier = Modifier.height(40.dp),
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}

// ─── Premium Analysis Progress Overlay ──────────────────────────

/**
 * Full-screen overlay shown while auto-calculation runs.
 * Matches the AutoProgressOverlay design language.
 */
@Composable
private fun AnalysisProgressOverlay(
    phase: String,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "analysis")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow",
    )
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
        ),
        label = "rotation",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AnalysisSurface),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            // ─── Animated ring ───
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    val ringSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                    // Track
                    drawArc(
                        color = AnalysisTrack,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = ringSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )

                    // Spinning gradient arc (120° segment)
                    drawArc(
                        brush = Brush.sweepGradient(
                            0f to AnalysisGradStart,
                            0.5f to AnalysisGradMid,
                            1f to AnalysisGradEnd,
                        ),
                        startAngle = ringRotation,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = ringSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    )

                    // Glow dot at arc tip
                    val tipAngleRad = Math.toRadians((ringRotation + 120f).toDouble())
                    val r = ringSize.width / 2
                    val cx = center.x + r * kotlin.math.cos(tipAngleRad).toFloat()
                    val cy = center.y + r * kotlin.math.sin(tipAngleRad).toFloat()
                    drawCircle(
                        color = AnalysisGlow.copy(alpha = glowAlpha),
                        radius = strokeWidth * 1.5f,
                        center = Offset(cx, cy),
                    )
                }

                // Center icon
                Icon(
                    Icons.Filled.Science,
                    contentDescription = null,
                    tint = AnalysisGlow.copy(alpha = 0.7f),
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Title
            Text(
                "Полный анализ",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AnalysisText,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Current phase — animated crossfade
            AnimatedContent(
                targetState = phase,
                transitionSpec = {
                    fadeIn(tween(400)) togetherWith fadeOut(tween(200))
                },
                label = "phase",
            ) { text ->
                Text(
                    text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AnalysisTextDim,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Steps checklist
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AnalysisSurfaceAlt,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val steps = listOf(
                        "Загрузка сигнала" to Icons.Filled.Download,
                        "Сглаживание и baseline" to Icons.Filled.Tune,
                        "Поиск пиков" to Icons.Filled.Search,
                        "Статистика распределения" to Icons.Filled.BarChart,
                        "Анализ паттернов" to Icons.Filled.Insights,
                        "Оценка качества метода" to Icons.Filled.VerifiedUser,
                        "Геохимические индексы" to Icons.Filled.Science,
                    )

                    val currentIdx = when {
                        phase.startsWith("Загрузка") -> 0
                        phase.startsWith("Сглаживание") -> 1
                        phase.startsWith("Поиск") || phase.contains("baseline") -> 2
                        phase.startsWith("Статистика") -> 3
                        phase.startsWith("Анализ") -> 4
                        phase.startsWith("Оценка") -> 5
                        phase.startsWith("Геохим") -> 6
                        else -> 1
                    }

                    steps.forEachIndexed { i, (label, icon) ->
                        val isDone = i < currentIdx
                        val isActive = i == currentIdx
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                        ) {
                            // Status indicator
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isDone -> AnalysisDone.copy(alpha = 0.15f)
                                            isActive -> AnalysisGradStart.copy(alpha = 0.2f)
                                            else -> Color(0xFF475569).copy(alpha = 0.2f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    isDone -> Icon(
                                        Icons.Filled.Check,
                                        null,
                                        tint = AnalysisDone,
                                        modifier = Modifier.size(12.dp),
                                    )
                                    isActive -> CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 1.5.dp,
                                        color = AnalysisGlow,
                                    )
                                    else -> Icon(
                                        icon,
                                        null,
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(10.dp),
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                label,
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    isDone -> AnalysisDone
                                    isActive -> AnalysisText
                                    else -> Color(0xFF475569)
                                },
                                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            )

                            if (isDone) {
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    tint = AnalysisDone.copy(alpha = 0.4f),
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Error Content ──────────────────────────────────────────────

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Filled.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Ошибка анализа",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Назад")
            }
        }
    }
}

// ─── PeakResult → PeakDetailsData mapper ────────────────────────

private data class AnalysisGraphEntry(
    val id: Long,
    val graphIndex: Int,
    val detectedGraphCount: Int,
)

private suspend fun loadAnalysisGraphEntries(current: ChromatogramEntity): List<AnalysisGraphEntry> {
    val currentMetadata = StoredReportMetadataCodec.decodeOrNull(current.algorithmConfig)
    val currentEntry = current.toAnalysisGraphEntry(currentMetadata)
    val detectedGraphCount = currentEntry.detectedGraphCount
    if (detectedGraphCount <= 1) return listOf(currentEntry)

    val chromatograms = DatabaseProvider.getDatabase()
        .chromatogramDao()
        .getBySampleId(current.sampleId)
        .first()
    val currentStartedAt = currentMetadata?.analysisStartedAtEpochMillis

    val entries = chromatograms.mapNotNull { chromatogram ->
        val metadata = StoredReportMetadataCodec.decodeOrNull(chromatogram.algorithmConfig)
            ?: return@mapNotNull null
        val graphIndex = metadata.primaryGraphIndex() ?: return@mapNotNull null
        val sameAnalysis = if (currentStartedAt != null) {
            metadata.analysisStartedAtEpochMillis == currentStartedAt
        } else {
            metadata.detectedGraphCount == detectedGraphCount
        }
        if (!sameAnalysis) return@mapNotNull null

        AnalysisGraphEntry(
            id = chromatogram.id,
            graphIndex = graphIndex.coerceAtLeast(1),
            detectedGraphCount = metadata.detectedGraphCount?.coerceAtLeast(1) ?: detectedGraphCount,
        )
    }

    return (entries + currentEntry)
        .distinctBy { it.id }
        .sortedWith(compareBy<AnalysisGraphEntry> { it.graphIndex }.thenBy { it.id })
}

private fun ChromatogramEntity.toAnalysisGraphEntry(metadata: StoredReportMetadata?): AnalysisGraphEntry {
    val graphIndex = metadata.primaryGraphIndex() ?: 1
    val detectedGraphCount = metadata?.detectedGraphCount
        ?: metadata?.graphs?.size?.takeIf { it > 0 }
        ?: 1
    return AnalysisGraphEntry(
        id = id,
        graphIndex = graphIndex.coerceAtLeast(1),
        detectedGraphCount = detectedGraphCount.coerceAtLeast(1),
    )
}

private fun StoredReportMetadata?.primaryGraphIndex(): Int? =
    this?.graphs?.singleOrNull()?.graphIndex
        ?: this?.graphs?.firstOrNull()?.graphIndex

private fun AlgorithmSettings.toCalculationParams(): CalculationParams {
    val window = smoothingWindowSize
        .coerceAtLeast(3)
        .let { if (it % 2 == 0) it + 1 else it }
    val polynomialOrder = smoothingPolyOrder.coerceIn(0, window - 1)
    val baselineIterations = when (baselineMethod) {
        BaselineMethodOption.SNIP -> snipIterations
        else -> alsIterations
    }
    val baselineMethodName = when (baselineMethod) {
        BaselineMethodOption.MANUAL -> "MANUAL_LINEAR"
        BaselineMethodOption.ALS -> "ALS"
        BaselineMethodOption.SNIP -> "SNIP"
    }
    val noiseMethodName = when (noiseMethod) {
        NoiseMethodOption.PEAK_TO_PEAK -> "PEAK_TO_PEAK"
        NoiseMethodOption.RMS -> "RMS"
        NoiseMethodOption.MAD -> "MAD"
    }

    return CalculationParams(
        smoothingEnabled = smoothingEnabled,
        smoothingWindowSize = window,
        smoothingPolynomialOrder = polynomialOrder,
        baselineMethod = baselineMethodName,
        baselineLambda = alsLambda,
        baselineP = alsPenalty,
        baselineIterations = baselineIterations,
        minPeakHeight = minHeight,
        minPeakProminence = minProminence,
        minPeakDistance = minDistance,
        minPeakWidth = minWidth,
        maxPeakWidth = if (maxWidth == Int.MAX_VALUE) 0 else maxWidth.coerceAtLeast(0),
        minSnr = noiseK,
        noiseMethod = noiseMethodName,
        integrationMethod = if (useInterpolatedBoundaries) "trapezoidal_interpolated" else "trapezoidal",
        boundaryMethod = boundaryMethod.name,
        boundaryPercentHeight = percentHeight.coerceIn(0.001, 1.0),
        clampNegative = clampNegative,
        presetName = presetName,
    )
}

private fun buildPeakDetailsData(peak: PeakResult): PeakDetailsData {
    return PeakDetailsData(
        peakId = peak.peakId,
        metrics = PeakMetrics(
            rtApex = peak.rtApex,
            rtCentroid = peak.rtCentroid ?: peak.rtApex,
            height = peak.height,
            area = peak.area,
            widthBase = peak.widthBase,
            widthHalfHeight = peak.widthHalfHeight ?: 0.0,
            widthHalfProminence = 0.0,
            prominence = peak.height,
            leftBaseTime = peak.leftBoundaryTime,
            rightBaseTime = peak.rightBoundaryTime,
            snrValue = peak.snr,
            snrFlag = if (peak.snr >= 10) SnrFlag.QUANTITATION
                else if (peak.snr >= 3) SnrFlag.DETECTABLE
                else SnrFlag.LOW,
            overlapStatus = peak.overlapStatus,
            boundaryMethod = BoundaryMethod.entries.find { it.name == peak.boundaryMethod }
                ?: BoundaryMethod.LOCAL_MINIMA,
            boundaryConfidence = 0.8,
            isManuallyEdited = peak.status == com.chromalab.feature.calculation.core.PeakStatus.MANUAL,
            tailingFactor = peak.tailingFactor,
            asymmetryFactor = peak.asymmetryFactor,
            plateCount = peak.plateCount,
            warnings = peak.warnings,
        ),
        confidence = PeakConfidence(
            grade = peak.confidence,
            score = when (peak.confidence) {
                ConfidenceGrade.HIGH -> 0.9
                ConfidenceGrade.MEDIUM -> 0.6
                ConfidenceGrade.LOW -> 0.3
                ConfidenceGrade.FAILED -> 0.0
            },
            factors = emptyList(),
            reasons = emptyList(),
        ),
        integration = IntegrationResult(
            totalArea = peak.area,
            positiveArea = peak.area,
            negativeArea = 0.0,
            method = IntegrationMethod.entries.find { it.name == peak.integrationMethod }
                ?: IntegrationMethod.TRAPEZOIDAL,
            startTime = peak.leftBoundaryTime,
            endTime = peak.rightBoundaryTime,
            pointCount = 0,
            clampedNegative = false,
        ),
        baselineMethod = peak.baselineMethod,
    )
}

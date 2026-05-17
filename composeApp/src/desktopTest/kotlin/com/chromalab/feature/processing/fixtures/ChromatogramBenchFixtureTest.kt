package com.chromalab.feature.processing.fixtures

import com.chromalab.feature.processing.bench.OfflineAnalysisAudit
import com.chromalab.feature.processing.bench.OfflineAnalysisAuditArtifacts
import com.chromalab.feature.processing.bench.OfflineAnalysisInput
import com.chromalab.feature.processing.bench.OfflineAnalysisRunner
import com.chromalab.feature.processing.bench.OfflineAxisCalibrationPointAudit
import com.chromalab.feature.processing.bench.OfflineAxisCalibrationSource
import com.chromalab.feature.processing.bench.OfflineCalibratedReportUiContract
import com.chromalab.feature.processing.bench.OfflineGraphAudit
import com.chromalab.feature.processing.bench.OfflineManualAxisCalibrationInput
import com.chromalab.feature.processing.bench.OfflineManualAxisCalibrationPointInput
import com.chromalab.feature.processing.bench.OfflinePeakSanityExpectationInput
import com.chromalab.feature.processing.bench.OfflineReportContractSectionStatus
import com.chromalab.feature.processing.bench.OfflineReportUiPlacement
import com.chromalab.feature.processing.bench.OfflineStageStatus
import com.chromalab.feature.processing.document.DocumentCorners
import com.chromalab.feature.processing.document.ImagePoint
import com.chromalab.feature.processing.geometry.CvGeometryAuditWriter
import com.chromalab.feature.processing.geometry.CvGeometryInputGraph
import com.chromalab.feature.processing.graph.GraphRegion
import com.chromalab.feature.processing.perspective.PerspectiveWarper
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val reportContractJson = Json {
    ignoreUnknownKeys = false
}

class ChromatogramBenchFixtureTest {

    @Test
    fun benchResourcesMatchManifestIdentity() {
        ChromatogramBenchFixtures.all.forEach { fixture ->
            val bytes = fixture.resourceBytes()

            assertEquals(fixture.sizeBytes, bytes.size, "${fixture.id} byte size changed")
            assertEquals(fixture.sha256, bytes.sha256(), "${fixture.id} SHA-256 changed")

            val image = ImageIO.read(ByteArrayInputStream(bytes))
            assertNotNull(image, "${fixture.id} must remain a readable image")
            assertEquals(fixture.width, image.width, "${fixture.id} width changed")
            assertEquals(fixture.height, image.height, "${fixture.id} height changed")
        }
    }

    @Test
    fun benchExpectedFactsRemainExplicit() {
        assertEquals(8, ChromatogramBenchFixtures.all.size)

        ChromatogramBenchFixtures.all.forEach { fixture ->
            assertTrue(fixture.expectedGraphCount >= 1, "${fixture.id} must define graph count")
            assertTrue(fixture.tags.isNotEmpty(), "${fixture.id} must define fixture tags")
            assertEquals("not_locked", fixture.numericTruthStatus)
        }

        val byId = ChromatogramBenchFixtures.all.associateBy { it.id }
        assertEquals(2, byId.getValue("bench_01_mz71_screenshot_page").expectedGraphCount)
        assertTrue(byId.getValue("bench_01_mz71_screenshot_page").strictGraphCount)
        assertEquals(4, byId.getValue("bench_04_stacked_xic_resolution").expectedGraphCount)
        assertTrue(byId.getValue("bench_04_stacked_xic_resolution").strictGraphCount)
        assertEquals(4, byId.getValue("bench_05_tic_plus_ions").expectedGraphCount)
        assertTrue(byId.getValue("bench_05_tic_plus_ions").strictGraphCount)
        assertEquals(2, byId.getValue("bench_06_photo_two_graphs_page").expectedGraphCount)
        assertTrue(byId.getValue("bench_06_photo_two_graphs_page").strictGraphCount)
        assertTrue(byId.getValue("bench_07_rotated_page_photo").requiresRotationCorrection)
        assertTrue("near_duplicate_candidate" in byId.getValue("bench_08_mz71_duplicate_candidate").tags)
    }

    @Test
    fun desktopPerspectiveWarperProducesMeasuredOutput() {
        val fixture = ChromatogramBenchFixtures.all.first { it.id == "bench_06_photo_two_graphs_page" }
        val root = Files.createTempDirectory("chromalab-perspective-warper")
        val inputPath = root.resolve("${fixture.id}.${fixture.extension}")
        Files.write(inputPath, fixture.resourceBytes())

        val result = PerspectiveWarper().warp(
            imagePath = inputPath.toAbsolutePath().toString(),
            corners = DocumentCorners(
                topLeft = ImagePoint(84f, 40f),
                topRight = ImagePoint(914f, 0f),
                bottomRight = ImagePoint(890f, 1230f),
                bottomLeft = ImagePoint(74f, 1270f),
            ),
            outputDir = root.resolve("perspective").toAbsolutePath().toString(),
        )

        assertNotNull(result, "desktop perspective warper must return a measured result")
        assertTrue(result.outputWidth > 0, "desktop perspective output width must be measured")
        assertTrue(result.outputHeight > 0, "desktop perspective output height must be measured")
        assertTrue(result.maxWarpDistance > 0f, "desktop perspective warp distance must be measured")
        assertFalse(
            result.homography.values.contentEquals(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)),
            "desktop perspective warper must not return the identity matrix for skewed corners",
        )
        val warped = ImageIO.read(Path.of(result.correctedPath).toFile())
        assertNotNull(warped, "desktop perspective output image must be readable")
        assertEquals(result.outputWidth, warped.width)
        assertEquals(result.outputHeight, warped.height)
        warped.flush()
    }

    @Test
    fun offlineRunnerProducesStageAuditForEveryBenchFixture() = runBlocking {
        val runner = OfflineAnalysisRunner()
        val root = Files.createTempDirectory("chromalab-bench-runner")

        ChromatogramBenchFixtures.all.forEach { fixture ->
            val inputPath = root.resolve("${fixture.id}.${fixture.extension}")
            Files.write(inputPath, fixture.resourceBytes())
            val outputDir = root.resolve("${fixture.id}_out")

            val audit = runner.run(
                OfflineAnalysisInput(
                    sourceId = fixture.id,
                    imagePath = inputPath.toAbsolutePath().toString(),
                    outputDir = outputDir.toAbsolutePath().toString(),
                    expectedGraphCount = fixture.expectedGraphCount,
                )
            )
            writeAuditArtifacts(audit, imagePath = inputPath, outputDir = outputDir)

            assertEquals(fixture.expectedAnalysisWidth, audit.imageWidth, "${fixture.id} analysis width")
            assertEquals(fixture.expectedAnalysisHeight, audit.imageHeight, "${fixture.id} analysis height")
            assertTrue(audit.stages.any { it.stage == "normalize" && it.status == OfflineStageStatus.SUCCESS })
            assertTrue(audit.stages.any { it.stage == "orientation_correct" && it.status == OfflineStageStatus.SUCCESS })
            assertTrue(audit.stages.any { it.stage == "perspective_geometry" && it.status == OfflineStageStatus.SUCCESS })
            assertTrue(audit.stages.any { it.stage == "graph_region" && it.status == OfflineStageStatus.SUCCESS })
            assertTrue(audit.stages.any { it.stage == "graph_refine" && it.status == OfflineStageStatus.SUCCESS })
            assertTrue(audit.stages.any { it.stage == "preprocess_rank" && it.status == OfflineStageStatus.SUCCESS })
            assertTrue(audit.stages.any { it.stage == "plot_area" && it.status == OfflineStageStatus.SUCCESS })
            assertTrue(audit.stages.any { it.stage == "axis_detect" && it.status == OfflineStageStatus.SUCCESS })
            assertTrue(audit.stages.any { it.stage == "axis_calibration" && it.status == OfflineStageStatus.SUCCESS })
            assertTrue(audit.stages.any { it.stage == "curve_extract" && it.status == OfflineStageStatus.SUCCESS })
            assertTrue(audit.graphCandidates.isNotEmpty(), "${fixture.id} must expose graph candidate audit")
            assertTrue(audit.graphs.isNotEmpty(), "${fixture.id} must expose per-graph audit")
            assertEquals(fixture.expectedAnalysisWidth, audit.perspectiveGeometry.imageWidth, "${fixture.id} perspective geometry width")
            assertEquals(fixture.expectedAnalysisHeight, audit.perspectiveGeometry.imageHeight, "${fixture.id} perspective geometry height")
            assertEquals(audit.graphs.size, audit.perspectiveGeometry.graphPanelCount, "${fixture.id} perspective geometry graph count")
            assertEquals(audit.graphs.size, audit.perspectiveGeometry.plotAreaCount, "${fixture.id} perspective geometry plot count")
            assertTrue(audit.perspectiveGeometry.plotGeometryReady, "${fixture.id} perspective geometry must see all plot areas")
            assertEquals(
                1 + audit.graphs.size * 2,
                audit.perspectiveGeometry.residualMetrics.candidateCount,
                "${fixture.id} must expose document, graph-panel, and plot-area quadrilateral candidates",
            )
            assertEquals(
                audit.graphs.size,
                audit.perspectiveGeometry.residualMetrics.acceptedPlotAreaCandidateCount,
                "${fixture.id} must accept one plot-area quadrilateral per graph",
            )
            assertTrue(
                audit.perspectiveGeometry.candidates.any { it.kind.name == "DOCUMENT" },
                "${fixture.id} must expose a document quadrilateral candidate",
            )
            assertTrue(
                audit.perspectiveGeometry.candidates.any { it.kind.name == "PLOT_AREA" && it.accepted },
                "${fixture.id} must expose accepted plot-area quadrilateral candidates",
            )
            assertTrue(audit.perspectiveGeometry.residualMetricsRequired, "${fixture.id} must keep residual metrics in the geometry contract")
            assertTrue(
                audit.graphs.all { it.preprocessingVariantScores.isNotEmpty() },
                "${fixture.id} must rank preprocessing variants",
            )
            assertTrue(
                audit.graphs.all { it.selectedPreprocessingVariant != null },
                "${fixture.id} must select preprocessing variant",
            )
            assertTrue(
                audit.graphs.all { it.selectedPreprocessingImagePath != null },
                "${fixture.id} must record selected preprocessing image path",
            )
            assertTrue(
                audit.graphs.all { it.cropQuality.areaRatio > 0f },
                "${fixture.id} must expose crop quality area",
            )
            assertTrue(
                audit.graphs.all { it.cropBoundaryRisk.topDarkPixelRatio >= 0f },
                "${fixture.id} must expose crop-boundary risk diagnostics",
            )
            assertTrue(
                audit.graphs.all { it.plotArea.areaRatioWithinPanel >= 0f },
                "${fixture.id} must expose plot-area diagnostics",
            )
            assertTrue(
                audit.graphs.all { it.plotArea.detected && it.plotArea.region != null },
                "${fixture.id} must detect audited plot-area bounds for every graph",
            )
            assertTrue(
                audit.graphs.all { it.axesDetected && it.originDetected },
                "${fixture.id} must detect desktop axis geometry and origin for every graph",
            )
            assertTrue(
                audit.graphs.all { it.axisConfidence > 0f },
                "${fixture.id} must expose non-zero axis detection confidence",
            )
            assertTrue(
                audit.graphs.all { !it.axisCalibration.ready },
                "${fixture.id} must keep desktop calculation blocked until scale calibration is confirmed",
            )
            assertTrue(
                audit.graphs.all { "axis_calibration.manual_required" in it.axisCalibration.warnings },
                "${fixture.id} must expose the manual calibration contract while desktop OCR is unavailable",
            )
            assertTrue(
                audit.graphs.all { it.curveMaskAvailable },
                "${fixture.id} must write desktop curve masks for every graph",
            )
            assertTrue(
                audit.graphs.all { it.curveMaskRawPixelCount > 0 && it.curveMaskCleanPixelCount > 0 },
                "${fixture.id} must expose non-empty curve-mask pixel counts",
            )
            assertTrue(
                audit.graphs.all { it.curveMaskCleanPixelCount <= it.curveMaskRawPixelCount },
                "${fixture.id} curve-mask cleanup must not increase candidate pixels",
            )
            assertTrue(
                audit.graphs.all { it.curvePointCount > 0 },
                "${fixture.id} must extract desktop curve points for every graph",
            )
            assertTrue(
                audit.graphs.all { it.curveCoverage > 0f },
                "${fixture.id} must expose non-zero curve extraction coverage",
            )
            assertTrue(
                audit.graphs.all { !it.signal.ready },
                "${fixture.id} must not convert a calibrated signal before axis calibration is confirmed",
            )
            assertTrue(
                audit.graphs.all { "signal_convert.axis_calibration_required" in it.signal.warnings },
                "${fixture.id} must expose the signal conversion gate while axis calibration is missing",
            )
            assertTrue(
                audit.graphs.all { !it.peakDetection.ready },
                "${fixture.id} must not run peak detection before calibrated signal data exists",
            )
            assertTrue(
                audit.graphs.all { "peak_detection.signal_required" in it.peakDetection.warnings },
                "${fixture.id} must expose the peak-detection gate while signal conversion is missing",
            )
            assertTrue(
                audit.graphs.all { !it.peakMetrics.ready },
                "${fixture.id} must not mark peak metrics ready before peak detection exists",
            )
            assertTrue(
                audit.graphs.all { "peak_metrics.peak_detection_required" in it.peakMetrics.warnings },
                "${fixture.id} must expose the peak-metrics gate while peak detection is missing",
            )
            assertTrue(
                audit.graphs.all { !it.peakSanity.ready },
                "${fixture.id} must not mark peak sanity ready before peak metrics exist",
            )
            assertTrue(
                audit.graphs.all { "peak_sanity.peak_metrics_required" in it.peakSanity.warnings },
                "${fixture.id} must expose the peak-sanity gate while peak metrics are missing",
            )
            fixture.expectedCropBounds.forEach { expectedCrop ->
                val graph = assertNotNull(
                    audit.graphs.firstOrNull { it.graphIndex == expectedCrop.graphIndex },
                    "${fixture.id} graph ${expectedCrop.graphIndex} must exist for crop-bound contract",
                )
                expectedCrop.assertMatches(graph.region, fixture.id)
            }
            if (fixture.requiresGraphRefinement) {
                assertTrue(
                    audit.graphs.any { it.refinement.changed },
                    "${fixture.id} must refine at least one broad graph crop",
                )
            }
            if (fixture.requiresUnresolvedBroadContext) {
                assertTrue(
                    audit.graphs.any { it.cropQuality.unresolvedBroadContext },
                    "${fixture.id} must keep unresolved broad context blocked after weak refinement",
                )
            }
            if (fixture.requiresRotationCorrection) {
                assertTrue(
                    audit.orientationCorrection?.wasRotated == true,
                    "${fixture.id} must rotate before graph detection",
                )
                assertTrue(
                    audit.graphs.none { it.cropQuality.rightAngleRotationSuspected },
                    "${fixture.id} must not carry right-angle rotation risk after orientation correction",
                )
                val graph = audit.graphs.single()
                assertTrue(
                    graph.region.x <= fixture.expectedAnalysisWidth * 0.10f,
                    "${fixture.id} must preserve the full left graph panel after boundary correction",
                )
                assertTrue(
                    graph.region.width >= fixture.expectedAnalysisWidth * 0.80f,
                    "${fixture.id} must keep the full visible chromatogram panel after boundary correction",
                )
            }
            assertTrue(audit.blockedAtStage != null, "${fixture.id} should be blocked honestly until axis OCR/calibration exists")
            assertTrue(audit.blockedAtStage != "plot_area", "${fixture.id} should pass the plot-area gate")
            assertTrue(audit.blockedAtStage != "axis_detect", "${fixture.id} should pass the axis-geometry gate")
            if (audit.graphs.all { it.cropQuality.acceptedForCalculation && it.cropBoundaryRisk.acceptedForCalculation }) {
                assertEquals("axis_calibration", audit.blockedAtStage, "${fixture.id} should stop at the scale-calibration gate")
            }

            assertTrue(Files.size(outputDir.resolve("audit.json")) > 0L, "${fixture.id} audit JSON must be written")
            assertTrue(
                Files.readString(outputDir.resolve("audit.json")).contains("\"perspectiveGeometry\""),
                "${fixture.id} audit JSON must expose the perspective geometry contract",
            )
            assertTrue(
                Files.readString(outputDir.resolve("audit.json")).contains("\"residualMetrics\""),
                "${fixture.id} audit JSON must expose perspective residual metrics",
            )
            assertTrue(Files.size(outputDir.resolve("audit_summary.md")) > 0L, "${fixture.id} audit summary must be written")
            assertTrue(
                Files.size(outputDir.resolve("calibrated_report.md")) > 0L,
                "${fixture.id} calibrated report artifact must be written",
            )
            assertTrue(
                Files.size(outputDir.resolve("calibrated_report_ui_contract.json")) > 0L,
                "${fixture.id} calibrated report UI contract must be written",
            )
            assertTrue(Files.size(outputDir.resolve("graph_candidates.png")) > 0L, "${fixture.id} graph overlay must be written")
            assertTrue(Files.size(outputDir.resolve("cv_geometry_audit.json")) > 0L, "${fixture.id} CV geometry JSON must be written")
            assertTrue(Files.size(outputDir.resolve("cv_geometry_overlay.png")) > 0L, "${fixture.id} CV geometry overlay must be written")
            assertTrue(
                Files.readString(outputDir.resolve("cv_geometry_audit.json")).contains("\"graphs\""),
                "${fixture.id} CV geometry JSON must expose graph audits",
            )
            audit.graphs.forEach { graph ->
                assertTrue(
                    Files.size(outputDir.resolve("selected_preprocessing_graph_${graph.graphIndex}.png")) > 0L,
                    "${fixture.id} graph ${graph.graphIndex} selected preprocessing crop must be written",
                )
                assertTrue(
                    Files.size(outputDir.resolve("graph_${graph.graphIndex}").resolve("mask_raw.png")) > 0L,
                    "${fixture.id} graph ${graph.graphIndex} raw curve mask must be written",
                )
                assertTrue(
                    Files.size(outputDir.resolve("graph_${graph.graphIndex}").resolve("mask_clean.png")) > 0L,
                    "${fixture.id} graph ${graph.graphIndex} clean curve mask must be written",
                )
                assertTrue(
                    graph.traceArtifacts.available,
                    "${fixture.id} graph ${graph.graphIndex} must expose trace-artifact diagnostics",
                )
                assertTrue(
                    Files.size(outputDir.resolve("graph_${graph.graphIndex}").resolve("trace_artifacts.png")) > 0L,
                    "${fixture.id} graph ${graph.graphIndex} trace artifact mask must be written",
                )
                assertTrue(
                    Files.size(outputDir.resolve("graph_${graph.graphIndex}").resolve("trace_artifact_suppressed_mask.png")) > 0L,
                    "${fixture.id} graph ${graph.graphIndex} trace cleanup hypothesis mask must be written",
                )
                assertTrue(
                    Files.size(outputDir.resolve("graph_${graph.graphIndex}").resolve("curve_overlay.png")) > 0L,
                    "${fixture.id} graph ${graph.graphIndex} curve overlay must be written",
                )
                assertTrue(
                    Files.size(outputDir.resolve("manual_calibration_graph_${graph.graphIndex}.png")) > 0L,
                    "${fixture.id} graph ${graph.graphIndex} manual calibration focus artifact must be written",
                )
                assertManualCalibrationFocusArtifact(fixture.id, audit, graph, outputDir)
            }

            if (fixture.strictGraphCount) {
                assertEquals(fixture.expectedGraphCount, audit.detectedGraphCount, "${fixture.id} detected graph count")
            } else if (audit.detectedGraphCount != fixture.expectedGraphCount) {
                assertTrue(
                    audit.warnings.any { it.startsWith("graph.count_mismatch.") },
                    "${fixture.id} must expose current desktop graph split limitation",
                )
            }
            if (fixture.requiresCropQualityWarning) {
                assertTrue(
                    audit.graphs.any { !it.cropQuality.acceptedForCalculation },
                    "${fixture.id} must flag context-heavy crop as not calculation-ready",
                )
            } else {
                assertTrue(
                    audit.graphs.all { it.cropQuality.acceptedForCalculation },
                    "${fixture.id} crops should remain calculation-ready at the crop-quality gate",
                )
            }
        }
    }

    @Test
    fun offlineRunnerAcceptsConfirmedManualAxisCalibration() = runBlocking {
        val runner = OfflineAnalysisRunner()
        val fixture = ChromatogramBenchFixtures.all.first { it.id == "bench_03_small_tic_export" }
        val root = Files.createTempDirectory("chromalab-manual-calibration")
        val inputPath = root.resolve("${fixture.id}.${fixture.extension}")
        Files.write(inputPath, fixture.resourceBytes())

        val audit = runner.run(
            OfflineAnalysisInput(
                sourceId = fixture.id,
                imagePath = inputPath.toAbsolutePath().toString(),
                outputDir = root.resolve("${fixture.id}_out").toAbsolutePath().toString(),
                expectedGraphCount = fixture.expectedGraphCount,
                manualAxisCalibrations = listOf(
                    OfflineManualAxisCalibrationInput(
                        graphIndex = 1,
                        xPoints = listOf(
                            OfflineManualAxisCalibrationPointInput(pixel = 0f, value = 0f, label = "0.00"),
                            OfflineManualAxisCalibrationPointInput(pixel = 220f, value = 10f, label = "10.00"),
                        ),
                        yPoints = listOf(
                            OfflineManualAxisCalibrationPointInput(pixel = 0f, value = 1_000f, label = "1000"),
                            OfflineManualAxisCalibrationPointInput(pixel = 120f, value = 0f, label = "0"),
                        ),
                        xUnit = "min",
                        yUnit = "abundance",
                    ),
                ),
            ),
        )

        val graph = audit.graphs.single()
        assertTrue(graph.axisCalibration.ready, "manual calibration must satisfy the calibration gate")
        assertEquals(OfflineAxisCalibrationSource.MANUAL_CONFIRMED, graph.axisCalibration.source)
        assertEquals(2, graph.axisCalibration.xCandidateCount)
        assertEquals(2, graph.axisCalibration.yCandidateCount)
        assertEquals("min", graph.axisCalibration.xUnit)
        assertEquals("abundance", graph.axisCalibration.yUnit)
        assertTrue(
            "axis_calibration.manual_required" !in graph.axisCalibration.warnings,
            "confirmed manual points must not keep the manual-required warning",
        )
        assertTrue(graph.signal.ready, "confirmed manual calibration must unlock signal conversion")
        assertTrue(graph.signal.pointCount > 0, "signal conversion must expose calibrated points")
        assertTrue(graph.signal.timeRange > 0f, "signal conversion must expose positive time range")
        assertTrue(graph.signal.intensityRange >= 0f, "signal conversion must expose intensity range")
        assertTrue(graph.signal.sortValid, "signal conversion must keep calibrated points sorted")
        assertTrue(
            audit.stages.any { it.stage == "signal_convert" && it.status == OfflineStageStatus.SUCCESS },
            "confirmed manual calibration must run the signal conversion stage",
        )
        assertTrue(graph.peakDetection.ready, "confirmed manual calibration must unlock audited peak detection")
        assertTrue(graph.peakDetection.peakCount > 0, "peak detection must find real candidates on the clean TIC fixture")
        assertTrue(
            audit.stages.any { it.stage == "peak_detection" && it.status == OfflineStageStatus.SUCCESS },
            "confirmed manual calibration must run the peak detection stage",
        )
        assertTrue(graph.peakMetrics.ready, "confirmed manual calibration must unlock peak metrics review")
        assertTrue(graph.peakMetrics.totalAbsArea > 0.0, "peak metrics must expose positive total integrated area")
        assertEquals(0, graph.peakMetrics.invalidBoundaryCount, "peak metrics must reject invalid boundaries")
        assertEquals(0, graph.peakMetrics.nonPositiveAreaCount, "peak metrics must reject non-positive peak areas")
        assertTrue(
            audit.stages.any { it.stage == "peak_metrics" && it.status == OfflineStageStatus.SUCCESS },
            "confirmed manual calibration must run the peak metrics review stage",
        )
        assertTrue(graph.peakSanity.ready, "confirmed manual calibration without fixture expectations must pass generic peak sanity")
        assertTrue(
            audit.stages.any { it.stage == "peak_sanity" && it.status == OfflineStageStatus.SUCCESS },
            "confirmed manual calibration must run the peak sanity stage",
        )
        assertTrue(
            audit.stages.any { it.stage == "report_validation" && it.status == OfflineStageStatus.SUCCESS },
            "confirmed manual calibration must run the structured report validation stage",
        )
        assertTrue(audit.blockedAtStage != "axis_calibration", "manual calibration must pass the scale gate")
        assertTrue(audit.blockedAtStage != "signal_convert", "manual calibration must pass the signal conversion gate")
        assertTrue(audit.blockedAtStage != "peak_detection", "manual calibration must pass the peak detection gate")
        assertTrue(audit.blockedAtStage != "peak_metrics", "manual calibration must pass the peak metrics gate")
        assertTrue(audit.blockedAtStage != "peak_sanity", "manual calibration without fixture expectations must pass peak sanity")
    }

    @Test
    fun offlineRunnerDetectsPeaksOnBestAndHardManualCalibratedFixtures() = runBlocking {
        val runner = OfflineAnalysisRunner()
        val root = Files.createTempDirectory("chromalab-peak-detection-fixtures")

        val best = runWithPlotManualCalibration(
            runner = runner,
            root = root,
            fixture = ChromatogramBenchFixtures.all.first { it.id == "bench_03_small_tic_export" },
        )
        assertPeakDetectionFixture(best, expectedGraphs = 1, minTotalPeaks = 5)

        val twoGraphs = runWithPlotManualCalibration(
            runner = runner,
            root = root,
            fixture = ChromatogramBenchFixtures.all.first { it.id == "bench_06_photo_two_graphs_page" },
        )
        assertPeakDetectionFixture(twoGraphs, expectedGraphs = 2, minTotalPeaks = 4)
        assertRightFrameSuppression(twoGraphs)
        assertTraceArtifactDiagnostics(twoGraphs)
        assertArtifactGuardProtectsBench06Graph2(twoGraphs)

        val rotated = runWithPlotManualCalibration(
            runner = runner,
            root = root,
            fixture = ChromatogramBenchFixtures.all.first { it.id == "bench_07_rotated_page_photo" },
        )
        assertTrue(rotated.orientationCorrection?.wasRotated == true, "rotated fixture must still use orientation correction")
        assertPeakDetectionFixture(rotated, expectedGraphs = 1, minTotalPeaks = 4)
        assertRightFrameSuppression(rotated)
        assertTraceArtifactDiagnostics(rotated)
    }

    @Test
    fun guardedCompletenessReviewStaysBoundedAcrossAdditionalHardFixtures() = runBlocking {
        val runner = OfflineAnalysisRunner()
        val root = Files.createTempDirectory("chromalab-guarded-completeness-fixtures")
        val fixturesById = ChromatogramBenchFixtures.all.associateBy { it.id }
        val additionalHardFixtures = listOf(
            "bench_01_mz71_screenshot_page",
            "bench_02_mz92_belyi_tigr",
            "bench_08_mz71_duplicate_candidate",
        ).map { fixturesById.getValue(it) }

        val auditsById = additionalHardFixtures.associate { fixture ->
            val audit = runWithPlotManualCalibration(
                runner = runner,
                root = root,
                fixture = fixture,
            )
            assertPeakDetectionFixture(
                audit = audit,
                expectedGraphs = fixture.expectedGraphCount,
                minTotalPeaks = fixture.expectedGraphCount,
            )
            assertTraceArtifactDiagnostics(audit)
            assertGuardedQualityReviewContract(audit)
            fixture.id to audit
        }

        assertAdditionalGuardedCompletenessScope(auditsById)
    }

    @Test
    fun sparseStackedIonPanelsConvertToSignalsBeforePeakReview() = runBlocking {
        val runner = OfflineAnalysisRunner()
        val root = Files.createTempDirectory("chromalab-sparse-stacked-ion-fixtures")
        val fixturesById = ChromatogramBenchFixtures.all.associateBy { it.id }
        val stackedIonFixtures = listOf(
            "bench_04_stacked_xic_resolution",
            "bench_05_tic_plus_ions",
        ).map { fixturesById.getValue(it) }

        val auditsById = stackedIonFixtures.associate { fixture ->
            val audit = runWithPlotManualCalibration(
                runner = runner,
                root = root,
                fixture = fixture,
            )
            assertEquals(fixture.expectedGraphCount, audit.graphs.size, "${fixture.id} calibrated graph count")
            assertTrue(
                audit.graphs.all { it.curveUsable },
                "${fixture.id} must treat sparse but spatially supported ion traces as usable curves",
            )
            assertTrue(
                audit.graphs.all { it.signal.ready },
                "${fixture.id} must convert every stacked ion panel to calibrated signal before peak review",
            )
            assertTrue(
                audit.graphs.none { "signal_convert.curve_points_required" in it.signal.warnings },
                "${fixture.id} must not block sparse stacked ion panels at signal conversion",
            )
            fixture.id to audit
        }

        assertSparseStackedIonSignalScope(auditsById)
        assertSparseStackedIonPeakQualityScope(auditsById)
        auditsById.values.forEach { assertReportContractAudit(it) }
        auditsById.values.forEach { assertCalibratedReportArtifact(it) }
        auditsById.values.forEach { assertCalibratedReportUiContractArtifact(it) }
    }

    private suspend fun runWithPlotManualCalibration(
        runner: OfflineAnalysisRunner,
        root: Path,
        fixture: ChromatogramBenchFixture,
    ): OfflineAnalysisAudit {
        val inputPath = root.resolve("${fixture.id}.${fixture.extension}")
        Files.write(inputPath, fixture.resourceBytes())

        val preview = runner.run(
            OfflineAnalysisInput(
                sourceId = "${fixture.id}_preview",
                imagePath = inputPath.toAbsolutePath().toString(),
                outputDir = root.resolve("${fixture.id}_preview").toAbsolutePath().toString(),
                expectedGraphCount = fixture.expectedGraphCount,
            )
        )
        assertEquals(fixture.expectedGraphCount, preview.graphs.size, "${fixture.id} preview graph count")

        val xEndValue = manualCalibrationXEndValue(fixture)
        val calibrations = preview.graphs.map { graph ->
            val plot = assertNotNull(graph.plotArea.region, "${fixture.id} graph ${graph.graphIndex} plot area")
            OfflineManualAxisCalibrationInput(
                graphIndex = graph.graphIndex,
                xPoints = listOf(
                    OfflineManualAxisCalibrationPointInput(pixel = 0f, value = 0f, label = "0"),
                    OfflineManualAxisCalibrationPointInput(
                        pixel = (plot.width - 1).coerceAtLeast(24).toFloat(),
                        value = xEndValue,
                        label = xEndValue.toString(),
                    ),
                ),
                yPoints = listOf(
                    OfflineManualAxisCalibrationPointInput(pixel = 0f, value = 1_000f, label = "1000"),
                    OfflineManualAxisCalibrationPointInput(
                        pixel = (plot.height - 1).coerceAtLeast(24).toFloat(),
                        value = 0f,
                        label = "0",
                    ),
                ),
                xUnit = if ("seconds_axis" in fixture.tags) "s" else "min",
                yUnit = "abundance",
            )
        }

        return runner.run(
            OfflineAnalysisInput(
                sourceId = fixture.id,
                imagePath = inputPath.toAbsolutePath().toString(),
                outputDir = root.resolve("${fixture.id}_calibrated").toAbsolutePath().toString(),
                expectedGraphCount = fixture.expectedGraphCount,
                manualAxisCalibrations = calibrations,
                peakSanityExpectations = peakSanityExpectationsFor(fixture),
            )
        ).also { audit ->
            writeAuditArtifacts(
                audit = audit,
                imagePath = inputPath,
                outputDir = Path.of(audit.outputDir),
            )
        }
    }

    private fun assertPeakDetectionFixture(
        audit: OfflineAnalysisAudit,
        expectedGraphs: Int,
        minTotalPeaks: Int,
        expectPeakSanityReady: Boolean = true,
    ) {
        assertEquals(expectedGraphs, audit.graphs.size, "${audit.sourceId} calibrated graph count")
        assertTrue(audit.graphs.all { it.signal.ready }, "${audit.sourceId} must convert signal on every graph")
        assertTrue(audit.graphs.all { it.peakDetection.ready }, "${audit.sourceId} must detect peaks on every graph")
        assertTrue(audit.graphs.all { it.peakMetrics.ready }, "${audit.sourceId} must pass peak metrics review on every graph")
        assertTrue(
            audit.graphs.sumOf { it.peakDetection.peakCount } >= minTotalPeaks,
            "${audit.sourceId} must detect visible peaks on real fixture examples",
        )
        assertTrue(
            audit.graphs.all { it.peakMetrics.invalidBoundaryCount == 0 },
            "${audit.sourceId} must not accept invalid peak boundaries",
        )
        assertTrue(
            audit.graphs.all { it.peakMetrics.totalAbsArea > 0.0 },
            "${audit.sourceId} must produce positive integrated peak area",
        )
        audit.graphs.forEach { graph ->
            assertTrue(
                graph.peakDetection.peaks.isNotEmpty(),
                "${audit.sourceId} graph ${graph.graphIndex} must expose per-peak audit rows",
            )
            val candidateCount = assertNotNull(
                graph.peakDetection.candidateCount,
                "${audit.sourceId} graph ${graph.graphIndex} must expose peak candidate diagnostics",
            )
            val rejectedCandidateCount = assertNotNull(
                graph.peakDetection.rejectedCandidateCount,
                "${audit.sourceId} graph ${graph.graphIndex} must expose rejected peak candidate diagnostics",
            )
            assertTrue(
                candidateCount >= graph.peakDetection.peakCount,
                "${audit.sourceId} graph ${graph.graphIndex} candidate count must cover accepted peaks",
            )
            assertTrue(
                rejectedCandidateCount >= 0,
                "${audit.sourceId} graph ${graph.graphIndex} rejected candidate count must be non-negative",
            )
            assertEquals(
                candidateCount - graph.peakDetection.peakCount,
                rejectedCandidateCount,
                "${audit.sourceId} graph ${graph.graphIndex} rejected candidates must match candidate minus accepted count",
            )
            assertNotNull(
                graph.peakDetection.noiseLevel,
                "${audit.sourceId} graph ${graph.graphIndex} must expose noise diagnostics",
            )
            assertNotNull(
                graph.peakDetection.detectionSignalSource,
                "${audit.sourceId} graph ${graph.graphIndex} must expose detection signal source",
            )
            assertTrue(
                graph.peakDetection.peaks.all { it.leftBoundaryTime < it.rtApex && it.rtApex < it.rightBoundaryTime },
                "${audit.sourceId} graph ${graph.graphIndex} must expose sane per-peak boundaries",
            )
            assertTrue(
                graph.peakDetection.peaks.all { it.widthBase > 0.0 },
                "${audit.sourceId} graph ${graph.graphIndex} must expose positive per-peak widths",
            )
            assertPeakOverlayArtifact(audit, graph)
        }
        assertTrue(
            audit.stages.any { it.stage == "peak_detection" && it.status == OfflineStageStatus.SUCCESS },
            "${audit.sourceId} must record peak_detection success stages",
        )
        assertTrue(
            audit.stages.any { it.stage == "peak_metrics" && it.status == OfflineStageStatus.SUCCESS },
            "${audit.sourceId} must record peak_metrics success stages",
        )
        assertTrue(
            audit.stages.any { it.stage == "peak_sanity" && it.status == OfflineStageStatus.SUCCESS },
            "${audit.sourceId} must record peak_sanity success stages",
        )
        assertReportContractAudit(audit)
        assertCalibratedReportArtifact(audit)
        assertCalibratedReportUiContractArtifact(audit)
        assertTrue(audit.blockedAtStage != "peak_detection", "${audit.sourceId} must pass peak detection gate")
        assertTrue(audit.blockedAtStage != "peak_metrics", "${audit.sourceId} must pass peak metrics gate")
        if (expectPeakSanityReady) {
            assertTrue(audit.graphs.all { it.peakSanity.ready }, "${audit.sourceId} must pass fixture peak sanity")
            assertTrue(audit.blockedAtStage != "peak_sanity", "${audit.sourceId} must pass peak sanity gate")
        } else {
            assertTrue(audit.graphs.any { !it.peakSanity.ready }, "${audit.sourceId} must expose failed fixture peak sanity")
            assertEquals("peak_sanity", audit.blockedAtStage, "${audit.sourceId} must block calculation at peak sanity")
            assertTrue(
                audit.graphs.any {
                    "peak_sanity.expected_apex_missing" in it.peakSanity.warnings ||
                        "peak_sanity.min_peak_count_not_met" in it.peakSanity.warnings
                },
                "${audit.sourceId} must explain why fixture peak sanity failed",
            )
        }
    }

    private fun assertReportContractAudit(audit: OfflineAnalysisAudit) {
        val report = audit.reportContract
        val expectedSections = setOf(
            "overview",
            "source_and_graph_preparation",
            "axis_calibration",
            "peak_table",
            "interactive_or_rendered_graph",
            "chromatographic_quality",
            "kovats_index_analysis",
            "distribution_and_chemical_interpretation",
            "warnings_and_red_flags",
            "technical_appendix",
        )

        if (audit.blockedAtStage == null) {
            assertTrue(
                audit.stages.any { it.stage == "report_validation" && it.status == OfflineStageStatus.SUCCESS },
                "${audit.sourceId} must run report_validation after calculation readiness",
            )
        } else {
            assertTrue(
                audit.stages.any { it.stage == "report_validation" && it.status == OfflineStageStatus.SKIPPED },
                "${audit.sourceId} must skip report_validation when calculation is blocked at ${audit.blockedAtStage}",
            )
        }
        assertEquals(audit.graphs.size, report.graphCount, "${audit.sourceId} report graph count")
        expectedSections.forEach { section ->
            assertTrue(
                report.sections.any { it.section == section },
                "${audit.sourceId} report contract must include $section",
            )
        }
        audit.graphs.forEach { graph ->
            expectedSections
                .filterNot { it == "overview" || it == "technical_appendix" }
                .forEach { section ->
                    assertTrue(
                        report.sections.any { it.graphIndex == graph.graphIndex && it.section == section },
                        "${audit.sourceId} graph ${graph.graphIndex} must include report section $section",
                    )
                }
            graph.peakDetection.peaks.forEach { peak ->
                val fwhm = assertNotNull(
                    peak.widthHalfHeight,
                    "${audit.sourceId} graph ${graph.graphIndex} peak ${peak.peakNumber} must expose FWHM",
                )
                assertTrue(fwhm > 0.0, "${audit.sourceId} graph ${graph.graphIndex} peak ${peak.peakNumber} FWHM")
                assertTrue(
                    peak.tailingFactor > 0.0,
                    "${audit.sourceId} graph ${graph.graphIndex} peak ${peak.peakNumber} must expose positive tailing factor",
                )
                assertTrue(
                    peak.asymmetryFactor > 0.0,
                    "${audit.sourceId} graph ${graph.graphIndex} peak ${peak.peakNumber} must expose positive asymmetry factor",
                )
                assertTrue(
                    peak.assignment.probableCompoundStatus.isNotBlank() &&
                        peak.assignment.formulaStatus.isNotBlank() &&
                        peak.assignment.compoundClassStatus.isNotBlank() &&
                        peak.assignment.carbonNumberStatus.isNotBlank() &&
                        peak.assignment.kovatsIndexStatus.isNotBlank(),
                    "${audit.sourceId} graph ${graph.graphIndex} peak ${peak.peakNumber} must expose explicit assignment statuses",
                )
            }
        }

        val peakTableSections = report.sections.filter { it.section == "peak_table" }
        peakTableSections.forEach { section ->
            assertFalse(
                "peak_fwhm_column" in section.missingFields,
                "${audit.sourceId} graph ${section.graphIndex} must no longer mark FWHM as a report-contract gap",
            )
            assertFalse(
                "peak_tailing_column" in section.missingFields,
                "${audit.sourceId} graph ${section.graphIndex} must no longer mark tailing as a report-contract gap",
            )
            assertFalse(
                "peak_asymmetry_column" in section.missingFields,
                "${audit.sourceId} graph ${section.graphIndex} must no longer mark asymmetry as a report-contract gap",
            )
            assertFalse(
                "compound_candidate_columns" in section.missingFields,
                "${audit.sourceId} graph ${section.graphIndex} must expose compound/Kovats columns as explicit not-calculated statuses",
            )
        }
        if (audit.blockedAtStage == null) {
            assertTrue(
                report.ready,
                "${audit.sourceId} report contract must become ready once peak-table columns are explicit",
            )
        }
        val allowedBlockedSections = allowedReportBlockedSections(audit.blockedAtStage)
        assertTrue(
            report.sections
                .filter { it.status == OfflineReportContractSectionStatus.BLOCKED }
                .all { it.section in allowedBlockedSections },
            "${audit.sourceId} must only block report validation in sections explained by current pipeline state",
        )
    }

    private fun assertCalibratedReportArtifact(audit: OfflineAnalysisAudit) {
        val reportPath = Path.of(audit.outputDir).resolve("calibrated_report.md")
        assertTrue(Files.size(reportPath) > 0L, "${audit.sourceId} calibrated report artifact must be non-empty")
        val report = Files.readString(reportPath)

        assertTrue(
            report.contains("# ChromaLab Calibrated Chromatogram Report"),
            "${audit.sourceId} calibrated report must use the user-facing report title",
        )
        assertTrue(report.contains("## Overview"), "${audit.sourceId} calibrated report must include overview")
        assertTrue(report.contains("## Key Warnings"), "${audit.sourceId} calibrated report must include key warnings")
        assertTrue(report.contains("## Technical Appendix"), "${audit.sourceId} calibrated report must include appendix")
        assertTrue(report.contains("### Visual Evidence"), "${audit.sourceId} calibrated report must include visual evidence")
        assertTrue(
            report.contains("graph_candidates.png"),
            "${audit.sourceId} calibrated report must reference the graph-candidate overlay",
        )
        assertTrue(
            Files.size(Path.of(audit.outputDir).resolve("graph_candidates.png")) > 0L,
            "${audit.sourceId} graph-candidate overlay must exist for report evidence",
        )
        assertTrue(
            report.contains("| # | RT apex | Left | Right | Height | Area | Area % | FWHM | W base | S/N |"),
            "${audit.sourceId} calibrated report must render the full peak-table columns",
        )
        assertTrue(
            report.contains("not calculated"),
            "${audit.sourceId} calibrated report must show missing chemistry explicitly",
        )

        var previousGraphIndex = -1
        audit.graphs.sortedBy { it.graphIndex }.forEach { graph ->
            val marker = "## Graph ${graph.graphIndex} Report"
            val currentGraphIndex = report.indexOf(marker)
            assertTrue(currentGraphIndex >= 0, "${audit.sourceId} calibrated report must include $marker")
            assertTrue(
                currentGraphIndex > previousGraphIndex,
                "${audit.sourceId} calibrated report must preserve graph/report ordering",
            )
            assertVisualEvidenceArtifact(audit, graph, report)
            previousGraphIndex = currentGraphIndex
        }

        val appendixIndex = report.indexOf("## Technical Appendix")
        assertTrue(appendixIndex > 0, "${audit.sourceId} calibrated report must place appendix after main sections")
        val mainReport = report.substring(0, appendixIndex)
        val appendix = report.substring(appendixIndex)
        assertFalse(
            mainReport.contains("peak_detection.sparse_trace_report_confidence_required"),
            "${audit.sourceId} main report must keep raw sparse warning codes out of the user-facing warning surface",
        )
        if (audit.graphs.any { "peak_detection.sparse_trace_report_confidence_required" in it.peakDetection.warnings }) {
            assertTrue(
                mainReport.contains("Sparse trace report confidence required"),
                "${audit.sourceId} main report must include human sparse trace confidence wording",
            )
            assertTrue(
                appendix.contains("peak_detection.sparse_trace_report_confidence_required"),
                "${audit.sourceId} appendix must keep the raw sparse warning code",
            )
        }
    }

    private fun assertVisualEvidenceArtifact(
        audit: OfflineAnalysisAudit,
        graph: OfflineGraphAudit,
        report: String,
    ) {
        val outputDir = Path.of(audit.outputDir)
        val requiredArtifacts = buildList {
            add("selected_preprocessing_graph_${graph.graphIndex}.png")
            add("manual_calibration_graph_${graph.graphIndex}.png")
            add("graph_${graph.graphIndex}/curve_overlay.png")
            add("graph_${graph.graphIndex}/trace_artifacts.png")
            add("graph_${graph.graphIndex}/trace_artifact_suppressed_mask.png")
            if (graph.peakMetrics.ready && graph.peakDetection.peaks.isNotEmpty()) {
                add("peak_overlay_graph_${graph.graphIndex}.png")
            }
        }

        requiredArtifacts.forEach { relativePath ->
            assertTrue(
                report.contains(relativePath),
                "${audit.sourceId} graph ${graph.graphIndex} calibrated report must reference $relativePath",
            )
            assertTrue(
                Files.size(outputDir.resolve(relativePath)) > 0L,
                "${audit.sourceId} graph ${graph.graphIndex} visual evidence artifact $relativePath must exist",
            )
        }
    }

    private fun assertCalibratedReportUiContractArtifact(audit: OfflineAnalysisAudit) {
        val contractPath = Path.of(audit.outputDir).resolve("calibrated_report_ui_contract.json")
        assertTrue(Files.size(contractPath) > 0L, "${audit.sourceId} UI contract artifact must be non-empty")
        val contract = reportContractJson.decodeFromString<OfflineCalibratedReportUiContract>(
            Files.readString(contractPath),
        )

        assertEquals(audit.sourceId, contract.sourceId, "${audit.sourceId} UI contract source")
        assertEquals(audit.graphs.size, contract.graphCount, "${audit.sourceId} UI contract graph count")
        assertFalse(contract.rawMarkdownIsFinalUi, "${audit.sourceId} raw Markdown must not be marked as final phone UI")
        assertEquals("calibrated_report.md", contract.markdownArtifactPath, "${audit.sourceId} Markdown artifact path")
        assertFalse(
            contract.primarySurface.rawWarningCodesVisible,
            "${audit.sourceId} main report surface must not expose raw warning codes",
        )
        assertTrue(
            contract.primarySurface.sections.any {
                it.sectionId == "overview" && it.placement == OfflineReportUiPlacement.MAIN_REPORT
            },
            "${audit.sourceId} UI contract must expose overview as a main report section",
        )
        assertTrue(
            contract.exportArtifacts.any {
                it.artifactPath == "calibrated_report_ui_contract.json" && !it.userFacing
            },
            "${audit.sourceId} UI contract artifact must be available for app/export wiring",
        )
        assertEquals(
            audit.graphs.sortedBy { it.graphIndex }.map { it.graphIndex },
            contract.graphs.map { it.graphIndex },
            "${audit.sourceId} UI contract must preserve graph order",
        )
        assertTrue(
            contract.technicalAppendix.rawWarningCodesVisible,
            "${audit.sourceId} technical appendix must keep raw warning codes visible",
        )

        audit.graphs.forEach { graph ->
            val graphContract = contract.graphs.single { it.graphIndex == graph.graphIndex }
            assertTrue(
                graphContract.sections.none {
                    it.placement == OfflineReportUiPlacement.MAIN_REPORT && it.rawWarningCodesVisible
                },
                "${audit.sourceId} graph ${graph.graphIndex} main UI sections must not expose raw warning codes",
            )
            assertTrue(
                graphContract.sections.any {
                    it.sectionId == "visual_evidence" &&
                        it.placement == OfflineReportUiPlacement.MAIN_REPORT &&
                        it.sourceContractSection == "interactive_or_rendered_graph"
                },
                "${audit.sourceId} graph ${graph.graphIndex} must map visual evidence into the main report",
            )
            assertVisualEvidenceContractEntry(
                audit = audit,
                graph = graph,
                contract = contract,
                evidenceId = "manual_calibration_focus",
                expectedPath = "manual_calibration_graph_${graph.graphIndex}.png",
                expectedPlacement = OfflineReportUiPlacement.MAIN_REPORT,
                expectedNearSection = "axis_calibration",
                requiredForMobile = true,
            )
            assertVisualEvidenceContractEntry(
                audit = audit,
                graph = graph,
                contract = contract,
                evidenceId = "curve_overlay",
                expectedPath = "graph_${graph.graphIndex}/curve_overlay.png",
                expectedPlacement = OfflineReportUiPlacement.MAIN_REPORT,
                expectedNearSection = "interactive_or_rendered_graph",
                requiredForMobile = true,
            )
            if (graph.peakMetrics.ready && graph.peakDetection.peaks.isNotEmpty()) {
                assertVisualEvidenceContractEntry(
                    audit = audit,
                    graph = graph,
                    contract = contract,
                    evidenceId = "peak_overlay",
                    expectedPath = "peak_overlay_graph_${graph.graphIndex}.png",
                    expectedPlacement = OfflineReportUiPlacement.MAIN_REPORT,
                    expectedNearSection = "peak_table",
                    requiredForMobile = true,
                )
            }
            assertVisualEvidenceContractEntry(
                audit = audit,
                graph = graph,
                contract = contract,
                evidenceId = "trace_artifact_mask",
                expectedPath = "graph_${graph.graphIndex}/trace_artifacts.png",
                expectedPlacement = OfflineReportUiPlacement.TECHNICAL_APPENDIX,
                expectedNearSection = "trace_artifact_masks",
                requiredForMobile = false,
            )
        }
    }

    private fun assertVisualEvidenceContractEntry(
        audit: OfflineAnalysisAudit,
        graph: OfflineGraphAudit,
        contract: OfflineCalibratedReportUiContract,
        evidenceId: String,
        expectedPath: String,
        expectedPlacement: OfflineReportUiPlacement,
        expectedNearSection: String,
        requiredForMobile: Boolean,
    ) {
        val graphContract = contract.graphs.single { it.graphIndex == graph.graphIndex }
        val evidence = graphContract.visualEvidence.single { it.evidenceId == evidenceId }
        assertEquals(expectedPath, evidence.artifactPath, "${audit.sourceId} graph ${graph.graphIndex} $evidenceId path")
        assertEquals(
            expectedPlacement,
            evidence.placement,
            "${audit.sourceId} graph ${graph.graphIndex} $evidenceId placement",
        )
        assertEquals(
            expectedNearSection,
            evidence.nearSectionId,
            "${audit.sourceId} graph ${graph.graphIndex} $evidenceId nearby section",
        )
        assertEquals(
            requiredForMobile,
            evidence.requiredForMobile,
            "${audit.sourceId} graph ${graph.graphIndex} $evidenceId mobile requirement",
        )
        if (evidence.generatedStatus == "generated") {
            assertTrue(
                Files.size(Path.of(audit.outputDir).resolve(expectedPath)) > 0L,
                "${audit.sourceId} graph ${graph.graphIndex} $evidenceId generated artifact must exist",
            )
        }
    }

    private fun allowedReportBlockedSections(blockedAtStage: String?): Set<String> =
        buildSet {
            when (blockedAtStage) {
                "crop_quality",
                "plot_area" -> add("source_and_graph_preparation")
                "axis_detect",
                "axis_calibration" -> add("axis_calibration")
                "curve_extract" -> add("interactive_or_rendered_graph")
                "signal_convert" -> {
                    add("interactive_or_rendered_graph")
                    add("chromatographic_quality")
                }
                "peak_detection",
                "peak_metrics",
                "peak_sanity" -> {
                    add("peak_table")
                    add("chromatographic_quality")
                    add("warnings_and_red_flags")
                }
            }
        }

    private fun manualCalibrationXEndValue(fixture: ChromatogramBenchFixture): Float =
        when {
            fixture.id == "bench_03_small_tic_export" -> 15f
            "seconds_axis" in fixture.tags -> 150f
            else -> 60f
        }

    private fun assertRightFrameSuppression(
        audit: OfflineAnalysisAudit,
        cutoffFraction: Double = 0.92,
    ) {
        audit.graphs.forEach { graph ->
            assertTrue(
                "right_frame_lines" in graph.curveMaskSuppressionApplied,
                "${audit.sourceId} graph ${graph.graphIndex} must suppress right-frame line artifacts",
            )
            val start = graph.signal.timeStart?.toDouble()
            val cutoff = start?.plus(graph.signal.timeRange.toDouble() * cutoffFraction)
            if (cutoff != null) {
                assertTrue(
                    graph.peakDetection.peaks.all { it.rtApex < cutoff },
                    "${audit.sourceId} graph ${graph.graphIndex} must not accept peaks on the right plot frame",
                )
            }
        }
    }

    private fun assertTraceArtifactDiagnostics(audit: OfflineAnalysisAudit) {
        audit.graphs.forEach { graph ->
            val artifacts = graph.traceArtifacts
            assertTrue(
                artifacts.available,
                "${audit.sourceId} graph ${graph.graphIndex} must expose trace-artifact audit",
            )
            assertNotNull(
                artifacts.artifactMaskPath,
                "${audit.sourceId} graph ${graph.graphIndex} must write a trace-artifact mask path",
            )
            assertNotNull(
                artifacts.cleanupHypothesisMaskPath,
                "${audit.sourceId} graph ${graph.graphIndex} must write a trace cleanup hypothesis mask path",
            )
            assertTrue(
                Files.size(Path.of(artifacts.artifactMaskPath)) > 0L,
                "${audit.sourceId} graph ${graph.graphIndex} trace-artifact mask must be non-empty",
            )
            assertTrue(
                Files.size(Path.of(artifacts.cleanupHypothesisMaskPath)) > 0L,
                "${audit.sourceId} graph ${graph.graphIndex} trace cleanup hypothesis mask must be non-empty",
            )
            assertTrue(
                artifacts.artifactPixelRatio >= 0f,
                "${audit.sourceId} graph ${graph.graphIndex} artifact ratio must be non-negative",
            )
            assertTrue(
                artifacts.cleanupHypothesisRetainedRatio in 0f..1f,
                "${audit.sourceId} graph ${graph.graphIndex} cleanup hypothesis retained ratio must be bounded",
            )
            assertTrue(
                artifacts.cleanupHypothesisColumnCoverage >= 0f,
                "${audit.sourceId} graph ${graph.graphIndex} cleanup hypothesis coverage must be non-negative",
            )
        }
    }

    private fun assertArtifactGuardProtectsBench06Graph2(audit: OfflineAnalysisAudit) {
        val graph1 = assertNotNull(
            audit.graphs.firstOrNull { it.graphIndex == 1 },
            "${audit.sourceId} graph 1 must exist for artifact guard review",
        )
        val graph2 = assertNotNull(
            audit.graphs.firstOrNull { it.graphIndex == 2 },
            "${audit.sourceId} graph 2 must exist for artifact guard review",
        )

        assertTrue(
            graph1.traceArtifacts.thresholdRelaxationAllowed,
            "${audit.sourceId} graph 1 must remain eligible for later protected threshold review",
        )
        assertTrue(
            graph1.peakDetection.controlledTuningApplied,
            "${audit.sourceId} graph 1 must apply guarded completeness tuning after artifact review",
        )
        assertEquals(
            "guarded_completeness",
            graph1.peakDetection.detectionProfile,
            "${audit.sourceId} graph 1 must record the guarded detection profile",
        )
        assertTrue(
            graph1.peakDetection.peakCount > (graph1.peakDetection.basePeakCount ?: 0),
            "${audit.sourceId} graph 1 guarded tuning must increase accepted peak completeness",
        )
        val quality = graph1.peakDetection.guardedQualityReview
        assertTrue(
            quality.available,
            "${audit.sourceId} graph 1 guarded tuning must expose peak-quality review",
        )
        assertTrue(
            quality.acceptedForGuardedCompleteness,
            "${audit.sourceId} graph 1 guarded tuning must pass peak-quality controls",
        )
        assertEquals(
            graph1.peakDetection.peakCount,
            quality.reviewPeakCount,
            "${audit.sourceId} graph 1 quality review must cover the selected peak table",
        )
        assertTrue(
            quality.lowDefaultSnrCount in 1..4,
            "${audit.sourceId} graph 1 quality review must isolate only a small number of lower-S/N recovered peaks",
        )
        assertEquals(
            0,
            quality.lowAreaShareCount,
            "${audit.sourceId} graph 1 guarded tuning must not accept low-area-share peaks",
        )
        assertTrue(
            graph1.peakDetection.peaks.any { "guarded_peak.low_default_snr" in it.qualityFlags },
            "${audit.sourceId} graph 1 must flag lower-S/N recovered peaks for review",
        )
        assertTrue(
            !graph2.traceArtifacts.thresholdRelaxationAllowed,
            "${audit.sourceId} graph 2 must block threshold relaxation while internal artifacts are high",
        )
        assertTrue(
            !graph2.peakDetection.controlledTuningApplied,
            "${audit.sourceId} graph 2 must not apply controlled tuning while artifact guard is blocked",
        )
        assertTrue(
            !graph2.peakDetection.guardedQualityReview.available,
            "${audit.sourceId} graph 2 must not expose guarded quality review without guarded tuning",
        )
        assertTrue(
            "trace_artifact.threshold_relaxation_blocked" in graph2.traceArtifacts.cleanupHypothesisWarnings,
            "${audit.sourceId} graph 2 must explain the artifact guard",
        )
        assertTrue(
            "peak_detection.threshold_relaxation_blocked_by_trace_artifacts" in graph2.peakDetection.warnings,
            "${audit.sourceId} graph 2 peak detection must carry the artifact guard",
        )
    }

    private fun assertGuardedQualityReviewContract(audit: OfflineAnalysisAudit) {
        audit.graphs.forEach { graph ->
            val quality = graph.peakDetection.guardedQualityReview
            if (graph.peakDetection.controlledTuningApplied) {
                assertEquals(
                    "guarded_completeness",
                    graph.peakDetection.detectionProfile,
                    "${audit.sourceId} graph ${graph.graphIndex} guarded tuning must record the guarded profile",
                )
                assertTrue(
                    quality.available,
                    "${audit.sourceId} graph ${graph.graphIndex} guarded tuning must expose quality review",
                )
                assertTrue(
                    quality.acceptedForGuardedCompleteness,
                    "${audit.sourceId} graph ${graph.graphIndex} guarded tuning must pass quality review",
                )
                assertEquals(
                    graph.peakDetection.peakCount,
                    quality.reviewPeakCount,
                    "${audit.sourceId} graph ${graph.graphIndex} guarded quality review must cover selected peaks",
                )
                assertTrue(
                    graph.peakDetection.peakCount > (graph.peakDetection.basePeakCount ?: 0),
                    "${audit.sourceId} graph ${graph.graphIndex} guarded tuning must improve completeness",
                )
            } else if (quality.available) {
                assertTrue(
                    !quality.acceptedForGuardedCompleteness || graph.peakDetection.controlledTuningReason != null,
                    "${audit.sourceId} graph ${graph.graphIndex} rejected guarded review must explain why it was not selected",
                )
            } else {
                assertTrue(
                    graph.peakDetection.peaks.all { it.qualityFlags.isEmpty() },
                    "${audit.sourceId} graph ${graph.graphIndex} default profile must not expose guarded-only peak flags",
                )
            }
        }
    }

    private fun assertAdditionalGuardedCompletenessScope(auditsById: Map<String, OfflineAnalysisAudit>) {
        val bench01 = auditsById.getValue("bench_01_mz71_screenshot_page")
        assertTrue(
            bench01.graphs.all {
                it.peakDetection.thresholdRelaxationAllowed == false &&
                    !it.peakDetection.controlledTuningApplied &&
                    !it.peakDetection.guardedQualityReview.available
            },
            "bench_01 must stay on default detection because artifact risk blocks guarded completeness",
        )

        val bench02 = auditsById.getValue("bench_02_mz92_belyi_tigr").graphs.single()
        assertTrue(
            bench02.peakDetection.thresholdRelaxationAllowed == true,
            "bench_02 should expose that artifact guard allows threshold review",
        )
        assertTrue(
            !bench02.peakDetection.controlledTuningApplied,
            "bench_02 must stay on default detection because the base peak table is not under-detected",
        )
        assertTrue(
            !bench02.peakDetection.guardedQualityReview.available,
            "bench_02 must not create guarded quality review when tuning is not attempted",
        )

        val bench08 = auditsById.getValue("bench_08_mz71_duplicate_candidate").graphs.single()
        val quality = bench08.peakDetection.guardedQualityReview
        assertTrue(
            bench08.peakDetection.controlledTuningApplied,
            "bench_08 must broaden guarded completeness beyond bench_06",
        )
        assertEquals(
            5,
            bench08.peakDetection.basePeakCount,
            "bench_08 guarded tuning must start from an under-detected base table",
        )
        assertEquals(
            9,
            bench08.peakDetection.tunedPeakCount,
            "bench_08 guarded tuning must expose the reviewed tuned peak count",
        )
        assertEquals(
            9,
            quality.reviewPeakCount,
            "bench_08 quality review must cover the full guarded peak table",
        )
        assertEquals(
            1,
            quality.lowDefaultSnrCount,
            "bench_08 should isolate only one lower-S/N recovered peak",
        )
        assertEquals(
            0,
            quality.lowAreaShareCount,
            "bench_08 guarded tuning must not accept low-area-share peaks",
        )
        assertEquals(
            0,
            quality.narrowBoundaryCount,
            "bench_08 guarded tuning must not accept narrow-boundary peaks",
        )
    }

    private fun assertSparseStackedIonSignalScope(auditsById: Map<String, OfflineAnalysisAudit>) {
        val expectedSparseGraphs = mapOf(
            "bench_04_stacked_xic_resolution" to setOf(3, 4),
            "bench_05_tic_plus_ions" to setOf(2, 3, 4),
        )
        auditsById.forEach { (fixtureId, audit) ->
            val sparseGraphs = expectedSparseGraphs.getValue(fixtureId)
            audit.graphs.forEach { graph ->
                val isExpectedSparseGraph = graph.graphIndex in sparseGraphs
                if (isExpectedSparseGraph) {
                    assertTrue(
                        graph.curveCoverage <= 0.3f,
                        "$fixtureId graph ${graph.graphIndex} must exercise the sparse trace path",
                    )
                    assertTrue(
                        "curve_extract.sparse_trace_low_column_coverage_accepted" in graph.warnings,
                        "$fixtureId graph ${graph.graphIndex} must audit sparse trace acceptance",
                    )
                    if (fixtureId == "bench_04_stacked_xic_resolution" && graph.graphIndex == 4) {
                        assertTrue(
                            "curve_extract.sparse_trace_localized_review_required" in graph.warnings,
                            "$fixtureId graph ${graph.graphIndex} must flag localized sparse evidence for review",
                        )
                    }
                    assertTrue(
                        graph.peakDetection.ready,
                        "$fixtureId graph ${graph.graphIndex} must reach peak detection after sparse signal conversion",
                    )
                } else {
                    assertTrue(
                        "curve_extract.sparse_trace_low_column_coverage_accepted" !in graph.warnings,
                        "$fixtureId graph ${graph.graphIndex} must not mark dense traces as sparse",
                    )
                }
            }
        }
    }

    private fun assertSparseStackedIonPeakQualityScope(auditsById: Map<String, OfflineAnalysisAudit>) {
        val expectedSparseGraphs = mapOf(
            "bench_04_stacked_xic_resolution" to mapOf(
                3 to SparsePeakQualityExpectation(peakCount = 4),
                4 to SparsePeakQualityExpectation(peakCount = 1, localized = true),
            ),
            "bench_05_tic_plus_ions" to mapOf(
                2 to SparsePeakQualityExpectation(peakCount = 4),
                3 to SparsePeakQualityExpectation(peakCount = 9, lowAreaShareCount = 4, overlapReviewCount = 6),
                4 to SparsePeakQualityExpectation(peakCount = 4, overlapReviewCount = 4),
            ),
        )
        auditsById.forEach { (fixtureId, audit) ->
            val sparseGraphs = expectedSparseGraphs.getValue(fixtureId)
            audit.graphs.forEach { graph ->
                val expectation = sparseGraphs[graph.graphIndex]
                val review = graph.peakDetection.sparseTraceQualityReview
                if (expectation == null) {
                    assertFalse(
                        review.available,
                        "$fixtureId graph ${graph.graphIndex} must not run sparse peak quality review for dense traces",
                    )
                    assertTrue(
                        graph.peakDetection.peaks.none { peak ->
                            peak.qualityFlags.any { it.startsWith("sparse_") || it.startsWith("sparse.") }
                        },
                        "$fixtureId graph ${graph.graphIndex} must not attach sparse peak flags to dense traces",
                    )
                    return@forEach
                }

                assertTrue(
                    review.available,
                    "$fixtureId graph ${graph.graphIndex} must expose sparse peak quality review",
                )
                assertTrue(
                    review.sparseTrace,
                    "$fixtureId graph ${graph.graphIndex} must mark sparse trace context",
                )
                assertEquals(
                    expectation.localized,
                    review.localizedSparseTrace,
                    "$fixtureId graph ${graph.graphIndex} localized sparse trace state",
                )
                assertEquals(
                    expectation.peakCount,
                    review.reviewPeakCount,
                    "$fixtureId graph ${graph.graphIndex} sparse reviewed peak count",
                )
                assertEquals(
                    expectation.lowAreaShareCount,
                    review.lowAreaShareCount,
                    "$fixtureId graph ${graph.graphIndex} sparse low-area peak count",
                )
                assertEquals(
                    expectation.overlapReviewCount,
                    review.overlapReviewCount,
                    "$fixtureId graph ${graph.graphIndex} sparse overlap review count",
                )
                assertTrue(
                    review.requiresReportConfidenceText,
                    "$fixtureId graph ${graph.graphIndex} must require report confidence text",
                )
                assertTrue(
                    "peak_detection.sparse_trace_report_confidence_required" in graph.peakDetection.warnings,
                    "$fixtureId graph ${graph.graphIndex} must carry sparse report-confidence warning",
                )
                if (expectation.localized) {
                    assertTrue(
                        "peak_detection.sparse_trace_localized_review_required" in graph.peakDetection.warnings,
                        "$fixtureId graph ${graph.graphIndex} must carry localized sparse peak warning",
                    )
                }
                if (expectation.lowAreaShareCount > 0) {
                    assertTrue(
                        "peak_detection.sparse_trace_low_area_share_peaks" in graph.peakDetection.warnings,
                        "$fixtureId graph ${graph.graphIndex} must carry low-area sparse peak warning",
                    )
                }
                if (expectation.overlapReviewCount > 0) {
                    assertTrue(
                        "peak_detection.sparse_trace_overlap_review_required" in graph.peakDetection.warnings,
                        "$fixtureId graph ${graph.graphIndex} must carry sparse overlap warning",
                    )
                }
                assertFalse(
                    graph.peakDetection.controlledTuningApplied,
                    "$fixtureId graph ${graph.graphIndex} sparse trace review must not apply guarded threshold relaxation",
                )
                assertNull(
                    graph.peakDetection.tunedPeakCount,
                    "$fixtureId graph ${graph.graphIndex} sparse trace review must keep tuned peak count empty",
                )
                assertEquals(
                    "default",
                    graph.peakDetection.detectionProfile,
                    "$fixtureId graph ${graph.graphIndex} sparse trace review must keep default detection profile",
                )
                assertEquals(
                    3.0,
                    graph.peakDetection.minSnr ?: 0.0,
                    0.001,
                    "$fixtureId graph ${graph.graphIndex} sparse trace review must not lower the S/N threshold",
                )
                assertTrue(
                    graph.peakDetection.peaks.all { "sparse_trace.low_column_coverage" in it.qualityFlags },
                    "$fixtureId graph ${graph.graphIndex} sparse peaks must carry low-column-coverage flags",
                )
                val peakTableSection = audit.reportContract.sections.single {
                    it.graphIndex == graph.graphIndex && it.section == "peak_table"
                }
                assertTrue(
                    "report.peak_table.sparse_trace_confidence_text_required" in peakTableSection.warnings,
                    "$fixtureId graph ${graph.graphIndex} report contract must require sparse trace confidence text",
                )
                if (expectation.localized) {
                    assertTrue(
                        graph.peakDetection.peaks.all { "sparse_trace.localized_evidence" in it.qualityFlags },
                        "$fixtureId graph ${graph.graphIndex} localized sparse peaks must carry localized evidence flags",
                    )
                }
            }
        }
    }

    private data class SparsePeakQualityExpectation(
        val peakCount: Int,
        val localized: Boolean = false,
        val lowAreaShareCount: Int = 0,
        val overlapReviewCount: Int = 0,
    )

    private fun peakSanityExpectationsFor(fixture: ChromatogramBenchFixture): List<OfflinePeakSanityExpectationInput> =
        when (fixture.id) {
            "bench_03_small_tic_export" -> listOf(
                OfflinePeakSanityExpectationInput(
                    graphIndex = 1,
                    expectedApexTimes = listOf(3.244, 3.890, 4.647, 5.610, 8.560),
                    apexTolerance = 0.45,
                    minPeakCount = 5,
                ),
            )
            "bench_06_photo_two_graphs_page" -> listOf(
                OfflinePeakSanityExpectationInput(graphIndex = 1, minPeakCount = 1, lockExpectedApexTimes = false),
                OfflinePeakSanityExpectationInput(graphIndex = 2, minPeakCount = 1, lockExpectedApexTimes = false),
            )
            "bench_07_rotated_page_photo" -> listOf(
                OfflinePeakSanityExpectationInput(graphIndex = 1, minPeakCount = 10, lockExpectedApexTimes = false),
            )
            else -> emptyList()
        }
}

private object ChromatogramBenchFixtures {
    val all: List<ChromatogramBenchFixture> = listOf(
        ChromatogramBenchFixture(
            id = "bench_01_mz71_screenshot_page",
            resourcePath = "$RESOURCE_ROOT/bench_01_mz71_screenshot_page.jpg",
            sha256 = "8EB64774D29F93C1CCB3D4F9035C96F9075121551600B2814BA7966021ECFD01",
            sizeBytes = 128_010,
            width = 964,
            height = 1280,
            expectedGraphCount = 2,
            expectedIon = "m/z 217 and m/z 218",
            expectedTitle = "Ion 217.00 / Ion 218.00: 0301002.D",
            strictGraphCount = true,
            expectedCropBounds = listOf(
                ExpectedGraphCrop(graphIndex = 1, x = 91, y = 278, width = 815, height = 475, tolerancePx = 12),
                ExpectedGraphCrop(graphIndex = 2, x = 24, y = 704, width = 885, height = 473, tolerancePx = 12),
            ),
            tags = setOf("printed_page_photo", "two_graphs", "m_z_217_218", "crop_required"),
        ),
        ChromatogramBenchFixture(
            id = "bench_02_mz92_belyi_tigr",
            resourcePath = "$RESOURCE_ROOT/bench_02_mz92_belyi_tigr.jpg",
            sha256 = "D1F0A55F6491E6FA7E3857086FDCCE97CDD3723A4F786D40000480F9A4B8BDFE",
            sizeBytes = 57_090,
            width = 576,
            height = 1280,
            expectedGraphCount = 1,
            expectedIon = "m/z 92.00",
            expectedTitle = "Ion 92.00 (91.70 to 92.70): BELIY TIGR_1.D\\data.ms",
            expectedCropBounds = listOf(ExpectedGraphCrop(graphIndex = 1, x = 10, y = 480, width = 558, height = 364)),
            tags = setOf("phone_screenshot", "document_context", "m_z_92", "belyi_tigr_reference"),
        ),
        ChromatogramBenchFixture(
            id = "bench_03_small_tic_export",
            resourcePath = "$RESOURCE_ROOT/bench_03_small_tic_export.jpg",
            sha256 = "C36A405C937741C6DE834AD9FD3196E658CE42ECBA03AFBBF1D2B47D00437DF4",
            sizeBytes = 6_682,
            width = 381,
            height = 132,
            expectedGraphCount = 1,
            expectedIon = null,
            expectedTitle = "TIC Scan CK-1.D",
            expectedCropBounds = listOf(ExpectedGraphCrop(graphIndex = 1, x = 0, y = 0, width = 381, height = 132)),
            tags = setOf("small_export", "tic", "low_resolution", "labeled_peaks"),
        ),
        ChromatogramBenchFixture(
            id = "bench_04_stacked_xic_resolution",
            resourcePath = "$RESOURCE_ROOT/bench_04_stacked_xic_resolution.png",
            sha256 = "27D998C2ACA33B12DC3700BFCC88FC3EFB15FAF2A3FE51317AFD220F0E2C3C25",
            sizeBytes = 165_615,
            width = 534,
            height = 1110,
            expectedGraphCount = 4,
            expectedIon = "XIC 198.0315",
            expectedTitle = null,
            strictGraphCount = true,
            expectedCropBounds = listOf(
                ExpectedGraphCrop(graphIndex = 1, x = 22, y = 0, width = 512, height = 238),
                ExpectedGraphCrop(graphIndex = 2, x = 22, y = 258, width = 512, height = 250),
                ExpectedGraphCrop(graphIndex = 3, x = 22, y = 542, width = 512, height = 250),
                ExpectedGraphCrop(graphIndex = 4, x = 22, y = 822, width = 512, height = 248),
            ),
            tags = setOf("stacked_graphs", "xic", "seconds_axis", "multiple_mass_windows"),
        ),
        ChromatogramBenchFixture(
            id = "bench_05_tic_plus_ions",
            resourcePath = "$RESOURCE_ROOT/bench_05_tic_plus_ions.png",
            sha256 = "3F715862316D9D24394F18CC21DFA4C42D6BAC694DD4E1DD8F743BF94984E1F9",
            sizeBytes = 162_230,
            width = 683,
            height = 807,
            expectedGraphCount = 4,
            expectedIon = "m/z 326/360/394",
            expectedTitle = "TIC: NERPA1.D",
            strictGraphCount = true,
            expectedCropBounds = listOf(
                ExpectedGraphCrop(graphIndex = 1, x = 79, y = 20, width = 604, height = 350),
                ExpectedGraphCrop(graphIndex = 2, x = 81, y = 354, width = 602, height = 161),
                ExpectedGraphCrop(graphIndex = 3, x = 81, y = 499, width = 602, height = 154),
                ExpectedGraphCrop(graphIndex = 4, x = 81, y = 637, width = 602, height = 154),
            ),
            tags = setOf("multi_panel", "tic", "ion_traces", "russian_labels"),
        ),
        ChromatogramBenchFixture(
            id = "bench_06_photo_two_graphs_page",
            resourcePath = "$RESOURCE_ROOT/bench_06_photo_two_graphs_page.jpg",
            sha256 = "04B95E7D8B992FE31708AEAB46E6582329AF1F48B54DB300EF4B664A4EDCB090",
            sizeBytes = 106_950,
            width = 964,
            height = 1280,
            expectedGraphCount = 2,
            expectedIon = "m/z 83 and m/z 92",
            expectedTitle = null,
            strictGraphCount = true,
            expectedCropBounds = listOf(
                ExpectedGraphCrop(graphIndex = 1, x = 137, y = 242, width = 736, height = 414, tolerancePx = 12),
                ExpectedGraphCrop(graphIndex = 2, x = 131, y = 649, width = 785, height = 511, tolerancePx = 12),
            ),
            tags = setOf("printed_page_photo", "two_graphs", "perspective_distortion", "crop_required"),
        ),
        ChromatogramBenchFixture(
            id = "bench_07_rotated_page_photo",
            resourcePath = "$RESOURCE_ROOT/bench_07_rotated_page_photo.jpg",
            sha256 = "83B60C45E6D9C66BAC5B60A150EE9B283D9BC07E6B5EF4FDF8BD7A107EC3A105",
            sizeBytes = 103_875,
            width = 1280,
            height = 964,
            expectedGraphCount = 1,
            expectedIon = "m/z 71.00",
            expectedTitle = "Ion 71.00 (70.70 to 71.70): BELIY TIGR_1.D\\data.ms",
            requiresRotationCorrection = true,
            expectedAnalysisWidth = 964,
            expectedAnalysisHeight = 1280,
            tags = setOf("rotated_page", "printed_page_photo", "orientation_correction", "m_z_71"),
        ),
        ChromatogramBenchFixture(
            id = "bench_08_mz71_duplicate_candidate",
            resourcePath = "$RESOURCE_ROOT/bench_08_mz71_duplicate_candidate.jpg",
            sha256 = "C1EF5E8BC1BD3BB3F9921A9A102FE8B3BADB7ED6BF9EC5E49DFD6EE54D15E0C6",
            sizeBytes = 70_617,
            width = 576,
            height = 1280,
            expectedGraphCount = 1,
            expectedIon = "m/z 71.00",
            expectedTitle = "Ion 71.00 (70.70 to 71.70): BELIY TIGR_1.D\\data.ms",
            expectedCropBounds = listOf(ExpectedGraphCrop(graphIndex = 1, x = 14, y = 492, width = 548, height = 290)),
            tags = setOf("phone_screenshot", "document_context", "m_z_71", "near_duplicate_candidate"),
        ),
    )

    private const val RESOURCE_ROOT = "fixtures/chromatogram_bench"
}

private data class ChromatogramBenchFixture(
    val id: String,
    val resourcePath: String,
    val sha256: String,
    val sizeBytes: Int,
    val width: Int,
    val height: Int,
    val expectedAnalysisWidth: Int = width,
    val expectedAnalysisHeight: Int = height,
    val expectedGraphCount: Int,
    val expectedIon: String?,
    val expectedTitle: String?,
    val requiresRotationCorrection: Boolean = false,
    val strictGraphCount: Boolean = expectedGraphCount == 1,
    val requiresCropQualityWarning: Boolean = false,
    val requiresGraphRefinement: Boolean = false,
    val requiresUnresolvedBroadContext: Boolean = false,
    val expectedCropBounds: List<ExpectedGraphCrop> = emptyList(),
    val numericTruthStatus: String = "not_locked",
    val tags: Set<String>,
) {
    val extension: String
        get() = resourcePath.substringAfterLast('.')

    fun resourceBytes(): ByteArray {
        val stream = requireNotNull(
            Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath),
        ) {
            "Missing chromatogram bench fixture resource: $resourcePath"
        }
        return stream.use { it.readBytes() }
    }
}

private data class ExpectedGraphCrop(
    val graphIndex: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val tolerancePx: Int = 8,
) {
    fun assertMatches(actual: GraphRegion, fixtureId: String) {
        assertWithinTolerance(actual.x, x, "$fixtureId graph $graphIndex crop x")
        assertWithinTolerance(actual.y, y, "$fixtureId graph $graphIndex crop y")
        assertWithinTolerance(actual.width, width, "$fixtureId graph $graphIndex crop width")
        assertWithinTolerance(actual.height, height, "$fixtureId graph $graphIndex crop height")
    }

    private fun assertWithinTolerance(actual: Int, expected: Int, message: String) {
        val delta = kotlin.math.abs(actual - expected)
        assertTrue(delta <= tolerancePx, "$message expected $expected +/- $tolerancePx, actual $actual")
    }
}

private fun ByteArray.sha256(): String =
    MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString(separator = "") { byte -> "%02X".format(byte) }

private fun writeAuditArtifacts(
    audit: OfflineAnalysisAudit,
    imagePath: Path,
    outputDir: Path,
) {
    Files.createDirectories(outputDir)
    Files.writeString(outputDir.resolve("audit.json"), OfflineAnalysisAuditArtifacts.toJson(audit))
    Files.writeString(outputDir.resolve("audit_summary.md"), OfflineAnalysisAuditArtifacts.toSummaryMarkdown(audit))
    Files.writeString(
        outputDir.resolve("calibrated_report_ui_contract.json"),
        OfflineAnalysisAuditArtifacts.toCalibratedReportUiContractJson(audit),
    )
    Files.writeString(outputDir.resolve("calibrated_report.md"), OfflineAnalysisAuditArtifacts.toCalibratedReportMarkdown(audit))
    val overlayImagePath = audit.orientationCorrection?.imagePath?.let { Path.of(it) } ?: imagePath
    writeGraphCandidateOverlay(audit, overlayImagePath, outputDir.resolve("graph_candidates.png"))
    writeSelectedPreprocessingCrops(audit, outputDir)
    writeManualCalibrationFocusArtifacts(audit, overlayImagePath, outputDir)
    CvGeometryAuditWriter.write(
        imagePath = overlayImagePath,
        graphRegions = audit.graphs.map { graph ->
            CvGeometryInputGraph(
                graphIndex = graph.graphIndex,
                region = graph.region,
            )
        },
        outputDir = outputDir,
    )
    writePeakOverlayArtifacts(audit, outputDir)
}

private fun writeGraphCandidateOverlay(
    audit: OfflineAnalysisAudit,
    imagePath: Path,
    outputPath: Path,
) {
    val source = assertNotNull(
        ImageIO.read(imagePath.toFile()),
        "${audit.sourceId} source image must be readable for overlay",
    )
    val image = BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    try {
        graphics.drawImage(source, 0, 0, null)
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.stroke = BasicStroke((source.width.coerceAtLeast(source.height) / 240f).coerceAtLeast(2f))
        graphics.font = Font(Font.SANS_SERIF, Font.BOLD, (source.width / 34).coerceIn(12, 28))

        audit.graphCandidates.forEach { candidate ->
            val color = if (candidate.accepted) Color(0x1B, 0x8A, 0x3A, 220) else Color(0xD3, 0x2F, 0x2F, 220)
            graphics.color = color
            val region = candidate.region
            graphics.drawRect(region.x, region.y, region.width.coerceAtLeast(1), region.height.coerceAtLeast(1))
            graphics.drawString(
                "#${candidate.graphIndex} ${if (candidate.accepted) "accepted" else "rejected"}",
                (region.x + 8).coerceAtMost(source.width - 1),
                (region.y + 24).coerceIn(24, source.height - 1),
            )
        }

        graphics.color = Color(0x15, 0x65, 0xC0, 230)
        graphics.stroke = BasicStroke((source.width.coerceAtLeast(source.height) / 140f).coerceAtLeast(3f))
        audit.graphs.forEach { graph ->
            val region = graph.region
            graphics.drawRect(region.x, region.y, region.width.coerceAtLeast(1), region.height.coerceAtLeast(1))
        }

        graphics.color = Color(0xFF, 0x8F, 0x00, 230)
        graphics.stroke = BasicStroke((source.width.coerceAtLeast(source.height) / 180f).coerceAtLeast(2f))
        audit.graphs.forEach { graph ->
            val region = graph.plotArea.region ?: return@forEach
            graphics.drawRect(region.x, region.y, region.width.coerceAtLeast(1), region.height.coerceAtLeast(1))
        }
    } finally {
        graphics.dispose()
    }

    ImageIO.write(image, "png", outputPath.toFile())
}

private fun writeSelectedPreprocessingCrops(
    audit: OfflineAnalysisAudit,
    outputDir: Path,
) {
    audit.graphs.forEach { graph ->
        val imagePath = assertNotNull(
            graph.selectedPreprocessingImagePath,
            "${audit.sourceId} graph ${graph.graphIndex} must record selected preprocessing image",
        )
        val source = assertNotNull(
            ImageIO.read(Path.of(imagePath).toFile()),
            "${audit.sourceId} graph ${graph.graphIndex} selected preprocessing image must be readable",
        )
        val region = graph.region
        val x = region.x.coerceIn(0, source.width - 1)
        val y = region.y.coerceIn(0, source.height - 1)
        val width = region.width.coerceIn(1, source.width - x)
        val height = region.height.coerceIn(1, source.height - y)
        val crop = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = crop.createGraphics()
        try {
            graphics.drawImage(
                source,
                0,
                0,
                width,
                height,
                x,
                y,
                x + width,
                y + height,
                null,
            )
        } finally {
            graphics.dispose()
            source.flush()
        }
        ImageIO.write(crop, "png", outputDir.resolve("selected_preprocessing_graph_${graph.graphIndex}.png").toFile())
        crop.flush()
    }
}

private fun writeManualCalibrationFocusArtifacts(
    audit: OfflineAnalysisAudit,
    imagePath: Path,
    outputDir: Path,
) {
    val source = assertNotNull(
        ImageIO.read(imagePath.toFile()),
        "${audit.sourceId} source image must be readable for manual calibration focus artifacts",
    )
    try {
        audit.graphs.forEach { graph ->
            val region = graph.region.clampedTo(source.width, source.height)
            val focus = BufferedImage(region.width, region.height, BufferedImage.TYPE_INT_ARGB)
            val graphics = focus.createGraphics()
            try {
                graphics.drawImage(
                    source,
                    0,
                    0,
                    region.width,
                    region.height,
                    region.x,
                    region.y,
                    region.x + region.width,
                    region.y + region.height,
                    null,
                )
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                graphics.stroke = BasicStroke((region.width.coerceAtLeast(region.height) / 160f).coerceAtLeast(2f))
                graphics.font = Font(Font.SANS_SERIF, Font.BOLD, (region.width / 32).coerceIn(11, 22))

                graph.plotArea.region?.let { plot ->
                    val x = (plot.x - region.x).coerceIn(0, region.width - 1)
                    val y = (plot.y - region.y).coerceIn(0, region.height - 1)
                    val right = (plot.x + plot.width - region.x).coerceIn(0, region.width)
                    val bottom = (plot.y + plot.height - region.y).coerceIn(0, region.height)
                    val width = (right - x).coerceAtLeast(1)
                    val height = (bottom - y).coerceAtLeast(1)

                    graphics.color = Color(0x15, 0x65, 0xC0, 230)
                    graphics.drawRect(x, y, width, height)
                    graphics.color = Color(0x4C, 0xAF, 0x50, 210)
                    graphics.drawLine(x, bottom, right, bottom)
                    graphics.color = Color(0x21, 0x96, 0xF3, 210)
                    graphics.drawLine(x, y, x, bottom)
                }

                graphics.color = Color(0xFF, 0x8F, 0x00, 230)
                graphics.drawString(
                    "manual calibration focus #${graph.graphIndex}",
                    8,
                    (region.height - 10).coerceAtLeast(18),
                )
            } finally {
                graphics.dispose()
            }
            ImageIO.write(
                focus,
                "png",
                outputDir.resolve("manual_calibration_graph_${graph.graphIndex}.png").toFile(),
            )
            focus.flush()
        }
    } finally {
        source.flush()
    }
}

private fun writePeakOverlayArtifacts(
    audit: OfflineAnalysisAudit,
    outputDir: Path,
) {
    audit.graphs
        .filter { it.peakMetrics.ready && it.peakDetection.peaks.isNotEmpty() }
        .forEach { graph ->
            val focusPath = outputDir.resolve("manual_calibration_graph_${graph.graphIndex}.png")
            if (!Files.exists(focusPath)) return@forEach
            val focus = assertNotNull(
                ImageIO.read(focusPath.toFile()),
                "${audit.sourceId} graph ${graph.graphIndex} manual focus artifact must be readable for peak overlay",
            )
            val overlay = BufferedImage(focus.width, focus.height, BufferedImage.TYPE_INT_ARGB)
            val graphics = overlay.createGraphics()
            try {
                graphics.drawImage(focus, 0, 0, null)
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                graphics.stroke = BasicStroke((focus.width.coerceAtLeast(focus.height) / 260f).coerceAtLeast(2f))
                graphics.font = Font(Font.SANS_SERIF, Font.BOLD, (focus.width / 44).coerceIn(10, 18))

                val plot = graph.plotArea.region ?: return@forEach
                val plotLocalX = (plot.x - graph.region.x).coerceIn(0, focus.width - 1)
                val plotLocalY = (plot.y - graph.region.y).coerceIn(0, focus.height - 1)
                val plotRight = (plotLocalX + plot.width).coerceIn(0, focus.width)
                val plotBottom = (plotLocalY + plot.height).coerceIn(0, focus.height)
                val xPair = graph.axisCalibration.xCandidates.maxPixelSpanPair() ?: return@forEach
                val yPair = graph.axisCalibration.yCandidates.maxPixelSpanPair() ?: return@forEach

                graph.peakDetection.peaks.forEach { peak ->
                    val left = (plotLocalX + xPair.valueToPixel(peak.leftBoundaryTime)).roundToInt()
                        .coerceIn(plotLocalX, plotRight)
                    val apex = (plotLocalX + xPair.valueToPixel(peak.rtApex)).roundToInt()
                        .coerceIn(plotLocalX, plotRight)
                    val right = (plotLocalX + xPair.valueToPixel(peak.rightBoundaryTime)).roundToInt()
                        .coerceIn(plotLocalX, plotRight)
                    val apexY = (plotLocalY + yPair.valueToPixel(peak.height)).roundToInt()
                        .coerceIn(plotLocalY, plotBottom)

                    graphics.color = Color(0x15, 0x65, 0xC0, 170)
                    graphics.drawLine(left, plotLocalY, left, plotBottom)
                    graphics.drawLine(right, plotLocalY, right, plotBottom)
                    graphics.color = Color(0xD3, 0x2F, 0x2F, 210)
                    graphics.drawLine(apex, plotLocalY, apex, plotBottom)
                    graphics.fillOval(apex - 4, apexY - 4, 8, 8)
                    graphics.drawString(
                        peak.peakNumber.toString(),
                        (apex + 4).coerceAtMost(focus.width - 18),
                        (apexY - 6).coerceAtLeast(12),
                    )
                }
            } finally {
                graphics.dispose()
                focus.flush()
            }
            ImageIO.write(overlay, "png", outputDir.resolve("peak_overlay_graph_${graph.graphIndex}.png").toFile())
            overlay.flush()
        }
}

private fun GraphRegion.clampedTo(imageWidth: Int, imageHeight: Int): GraphRegion {
    val x = this.x.coerceIn(0, imageWidth - 1)
    val y = this.y.coerceIn(0, imageHeight - 1)
    val width = this.width.coerceIn(1, imageWidth - x)
    val height = this.height.coerceIn(1, imageHeight - y)
    return GraphRegion(x = x, y = y, width = width, height = height, label = label)
}

private fun List<OfflineAxisCalibrationPointAudit>.maxPixelSpanPair():
    Pair<OfflineAxisCalibrationPointAudit, OfflineAxisCalibrationPointAudit>? {
    if (size < 2) return null
    val sorted = sortedBy { it.pixel }
    return sorted.first() to sorted.last()
}

private fun Pair<OfflineAxisCalibrationPointAudit, OfflineAxisCalibrationPointAudit>.valueToPixel(value: Double): Double {
    val pixelSpan = second.pixel - first.pixel
    val valueSpan = second.value - first.value
    if (pixelSpan == 0f || valueSpan == 0f) return first.pixel.toDouble()
    return first.pixel + ((value - first.value) / valueSpan) * pixelSpan
}

private fun assertManualCalibrationFocusArtifact(
    fixtureId: String,
    audit: OfflineAnalysisAudit,
    graph: OfflineGraphAudit,
    outputDir: Path,
) {
    val imageWidth = assertNotNull(audit.imageWidth, "$fixtureId must expose analysis image width")
    val imageHeight = assertNotNull(audit.imageHeight, "$fixtureId must expose analysis image height")
    val expectedRegion = graph.region.clampedTo(imageWidth, imageHeight)
    val artifactPath = outputDir.resolve("manual_calibration_graph_${graph.graphIndex}.png")
    val artifact = assertNotNull(
        ImageIO.read(artifactPath.toFile()),
        "$fixtureId graph ${graph.graphIndex} manual calibration focus artifact must be readable",
    )
    try {
        assertEquals(
            expectedRegion.width,
            artifact.width,
            "$fixtureId graph ${graph.graphIndex} manual focus width must match graph panel width",
        )
        assertEquals(
            expectedRegion.height,
            artifact.height,
            "$fixtureId graph ${graph.graphIndex} manual focus height must match graph panel height",
        )

        if (!expectedRegion.isFullImage(imageWidth, imageHeight)) {
            val fullArea = imageWidth.toLong() * imageHeight.toLong()
            val focusArea = artifact.width.toLong() * artifact.height.toLong()
            assertTrue(
                focusArea < fullArea,
                "$fixtureId graph ${graph.graphIndex} manual focus must not show the full source page",
            )
        }

        val plotArea = assertNotNull(
            graph.plotArea.region,
            "$fixtureId graph ${graph.graphIndex} must expose plot area for manual focus review",
        )
        val leftMargin = plotArea.x - expectedRegion.x
        val topMargin = plotArea.y - expectedRegion.y
        val rightMargin = expectedRegion.x + expectedRegion.width - (plotArea.x + plotArea.width)
        val bottomMargin = expectedRegion.y + expectedRegion.height - (plotArea.y + plotArea.height)
        assertTrue(
            leftMargin >= 0 && topMargin >= 0 && rightMargin >= 0 && bottomMargin >= 0,
            "$fixtureId graph ${graph.graphIndex} plot area must be inside manual focus panel",
        )
        assertTrue(
            leftMargin > 0 || topMargin > 0 || rightMargin > 0 || bottomMargin > 0,
            "$fixtureId graph ${graph.graphIndex} manual focus must preserve graph-panel context outside plot area",
        )

        val plotRatio = (plotArea.width.toFloat() * plotArea.height.toFloat()) /
            (expectedRegion.width.toFloat() * expectedRegion.height.toFloat())
        assertTrue(
            plotRatio in 0.05f..0.98f,
            "$fixtureId graph ${graph.graphIndex} plot area ratio in manual focus is suspicious: $plotRatio",
        )
    } finally {
        artifact.flush()
    }
}

private fun assertPeakOverlayArtifact(
    audit: OfflineAnalysisAudit,
    graph: OfflineGraphAudit,
) {
    val outputDir = Path.of(audit.outputDir)
    val overlayPath = outputDir.resolve("peak_overlay_graph_${graph.graphIndex}.png")
    val focusPath = outputDir.resolve("manual_calibration_graph_${graph.graphIndex}.png")
    assertTrue(Files.size(overlayPath) > 0L, "${audit.sourceId} graph ${graph.graphIndex} peak overlay must be written")
    val overlay = assertNotNull(
        ImageIO.read(overlayPath.toFile()),
        "${audit.sourceId} graph ${graph.graphIndex} peak overlay must be readable",
    )
    val focus = assertNotNull(
        ImageIO.read(focusPath.toFile()),
        "${audit.sourceId} graph ${graph.graphIndex} manual focus artifact must be readable for overlay comparison",
    )
    try {
        assertEquals(focus.width, overlay.width, "${audit.sourceId} graph ${graph.graphIndex} overlay width")
        assertEquals(focus.height, overlay.height, "${audit.sourceId} graph ${graph.graphIndex} overlay height")
        assertTrue(
            overlay.hasPeakOverlayColor(),
            "${audit.sourceId} graph ${graph.graphIndex} overlay must contain drawn peak markers",
        )
    } finally {
        overlay.flush()
        focus.flush()
    }
}

private fun BufferedImage.hasPeakOverlayColor(): Boolean {
    var markerPixels = 0
    var y = 0
    while (y < height) {
        var x = 0
        while (x < width) {
            val argb = getRGB(x, y)
            val alpha = argb ushr 24 and 0xFF
            val red = argb ushr 16 and 0xFF
            val green = argb ushr 8 and 0xFF
            val blue = argb and 0xFF
            if (alpha > 150 && red > 150 && green < 110 && blue < 110) {
                markerPixels++
                if (markerPixels >= 4) return true
            }
            x++
        }
        y++
    }
    return false
}

private fun GraphRegion.isFullImage(imageWidth: Int, imageHeight: Int): Boolean =
    x == 0 && y == 0 && width >= imageWidth && height >= imageHeight

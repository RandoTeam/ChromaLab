package com.chromalab.feature.processing.debug

import com.chromalab.core.data.model.SourceType
import com.chromalab.feature.processing.geometry.SourceProvenance
import com.chromalab.feature.processing.geometry.SourceType as GeometrySourceType
import com.chromalab.feature.processing.report.buildProcessingStoredReportMetadata
import com.chromalab.feature.reports.InputSourceType
import com.chromalab.feature.reports.ReportGateStatus
import com.chromalab.feature.reports.RuntimeFailureClass
import com.chromalab.feature.validation.AutonomousValidationArtifactRecord
import com.chromalab.feature.validation.AutonomousValidationFixtureContracts
import com.chromalab.feature.validation.AutonomousValidationFixtureMetadata
import com.chromalab.feature.validation.AutonomousValidationModelComparisonSummary
import com.chromalab.feature.validation.AutonomousValidationModelMode
import com.chromalab.feature.validation.AutonomousValidationRunDecision
import com.chromalab.feature.validation.AutonomousValidationRunStart
import com.chromalab.feature.validation.AutonomousValidationRunArtifactManifest
import com.chromalab.feature.validation.AutonomousValidationSuiteRunSummary
import com.chromalab.feature.validation.PHASE9B_ANDROID_VALIDATION_FIXTURE_IDS
import com.chromalab.feature.validation.WHITE_TIGER_ION71_FIXTURE_ID
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AutonomousValidationFixtureContractTest {
    @Test
    fun fixtureMetadataAcceptsWhiteTigerIon71Fixture() {
        val metadata = AutonomousValidationFixtureMetadata(
            fixtureId = WHITE_TIGER_ION71_FIXTURE_ID,
            displayName = "White Tiger Ion 71 validation chromatogram",
            assetImagePath = "validation/white_tiger_ion71_fixture.jpg",
            expectedGraphCount = 1,
            expectedMode = "AUTO_DIAGNOSTIC_OR_REVIEW_UNTIL_AXIS_CALIBRATION_IS_FIXED",
            expectedReportGate = ReportGateStatus.DIAGNOSTIC_ONLY,
            knownHistoricalFailures = listOf(
                RuntimeFailureClass.AXIS_DETECTION_FAILURE,
                RuntimeFailureClass.CALIBRATION_FAILURE,
            ),
        )

        assertTrue(AutonomousValidationFixtureContracts.validateMetadata(metadata).isEmpty())
        assertTrue(AutonomousValidationFixtureContracts.isSupportedFixture(metadata.fixtureId))
    }

    @Test
    fun fixtureMetadataRejectsUnsupportedFixture() {
        val metadata = AutonomousValidationFixtureMetadata(
            fixtureId = "unknown_fixture",
            displayName = "Unknown",
            assetImagePath = "validation/missing.png",
            expectedGraphCount = 1,
            expectedMode = "AUTO_DIAGNOSTIC",
            expectedReportGate = ReportGateStatus.BLOCKED,
            knownHistoricalFailures = listOf(RuntimeFailureClass.UNKNOWN_FAILURE),
        )

        assertTrue(
            AutonomousValidationFixtureContracts
                .validateMetadata(metadata)
                .any { it.startsWith("fixture_id_unsupported") },
        )
    }

    @Test
    fun phase9bFixtureSuiteSelectsAtLeastFiveFixtures() {
        assertTrue(PHASE9B_ANDROID_VALIDATION_FIXTURE_IDS.size >= 5)
        assertTrue(WHITE_TIGER_ION71_FIXTURE_ID in PHASE9B_ANDROID_VALIDATION_FIXTURE_IDS)
        assertTrue("bench_03_small_tic_export" in PHASE9B_ANDROID_VALIDATION_FIXTURE_IDS)
        assertTrue("bench_06_photo_two_graphs_page" in PHASE9B_ANDROID_VALIDATION_FIXTURE_IDS)
    }

    @Test
    fun phase9bFixtureMetadataSetValidates() {
        val metadata = PHASE9B_ANDROID_VALIDATION_FIXTURE_IDS.map { fixtureId ->
            AutonomousValidationFixtureMetadata(
                fixtureId = fixtureId,
                displayName = "Fixture $fixtureId",
                assetImagePath = "validation/$fixtureId.jpg",
                expectedGraphCount = if (fixtureId.contains("04") || fixtureId.contains("05")) 4 else 1,
                expectedMode = "AUTONOMOUS_REVIEW",
                expectedReportGate = ReportGateStatus.REVIEW_ONLY,
                knownHistoricalFailures = listOf(RuntimeFailureClass.UNKNOWN_FAILURE),
            )
        }

        assertTrue(AutonomousValidationFixtureContracts.validatePhase9bFixtureSet(metadata).isEmpty())
    }

    @Test
    fun validationModelModeParsesAdbExtras() {
        assertEquals(
            AutonomousValidationModelMode.DETERMINISTIC_ONLY,
            AutonomousValidationModelMode.parse(null),
        )
        assertEquals(
            AutonomousValidationModelMode.DETERMINISTIC_ONLY,
            AutonomousValidationModelMode.parse("no_model"),
        )
        assertEquals(
            AutonomousValidationModelMode.MODEL_ENABLED,
            AutonomousValidationModelMode.parse("model_enabled"),
        )
        assertEquals(
            AutonomousValidationModelMode.MODEL_ENABLED,
            AutonomousValidationModelMode.parse("gemma"),
        )
    }

    @Test
    fun validationRunStartPreservesModelMode() {
        val runStart = AutonomousValidationRunStart(
            fixtureId = WHITE_TIGER_ION71_FIXTURE_ID,
            runId = "white_tiger_ion71_20260520_120000",
            fixtureDisplayName = "White Tiger Ion 71 validation chromatogram",
            sourceImagePath = "/app/files/captures/validation/fixture.jpg",
            workingDirectory = "/app/files/captures/validation/white_tiger_ion71_20260520_120000",
            publicArtifactDirectory = "/sdcard/Download/ChromaLab/validation/white_tiger_ion71_20260520_120000",
            modelMode = AutonomousValidationModelMode.MODEL_ENABLED,
        )

        val encoded = Json.encodeToString(AutonomousValidationRunStart.serializer(), runStart)
        val decoded = Json.decodeFromString(AutonomousValidationRunStart.serializer(), encoded)

        assertEquals(AutonomousValidationModelMode.MODEL_ENABLED, decoded.modelMode)
    }

    @Test
    fun suiteRunBlocksWhenValidatorArtifactsAreMissing() {
        val summary = AutonomousValidationSuiteRunSummary(
            fixtureId = WHITE_TIGER_ION71_FIXTURE_ID,
            modelMode = AutonomousValidationModelMode.DETERMINISTIC_ONLY,
            expectedGraphCount = 1,
            graphCount = 1,
            reportGate = ReportGateStatus.REVIEW_ONLY,
            validatorVerdict = "REVIEW",
            runtimeFailureClass = RuntimeFailureClass.VLM_SEMANTIC_LAYER_UNAVAILABLE,
            runtimeEvidencePackageAvailable = true,
            validatorJsonAvailable = false,
            validatorMarkdownAvailable = true,
            finalReportJsonAvailable = true,
            exportManifestAvailable = true,
        )

        assertEquals(
            AutonomousValidationRunDecision.BLOCKED,
            AutonomousValidationFixtureContracts.evaluateRun(summary),
        )
    }

    @Test
    fun suiteRunBlocksWhenTerminalExportManifestIsMissing() {
        val summary = AutonomousValidationSuiteRunSummary(
            fixtureId = "bench_01_mz71_screenshot_page",
            modelMode = AutonomousValidationModelMode.DETERMINISTIC_ONLY,
            expectedGraphCount = 2,
            graphCount = null,
            reportGate = null,
            validatorVerdict = null,
            runtimeFailureClass = RuntimeFailureClass.PERFORMANCE_TIMEOUT,
            runtimeEvidencePackageAvailable = false,
            validatorJsonAvailable = false,
            validatorMarkdownAvailable = false,
            finalReportJsonAvailable = false,
            exportManifestAvailable = false,
        )

        assertEquals(
            AutonomousValidationRunDecision.BLOCKED,
            AutonomousValidationFixtureContracts.evaluateRun(summary),
        )
        assertTrue(AutonomousValidationFixtureContracts.validateRunSummary(summary).isEmpty())
    }

    @Test
    fun suiteRunRequiresFailureClassWhenBlocked() {
        val summary = AutonomousValidationSuiteRunSummary(
            fixtureId = WHITE_TIGER_ION71_FIXTURE_ID,
            modelMode = AutonomousValidationModelMode.MODEL_ENABLED,
            expectedGraphCount = 1,
            graphCount = 0,
            reportGate = ReportGateStatus.BLOCKED,
            validatorVerdict = "FAIL",
            runtimeFailureClass = null,
            runtimeEvidencePackageAvailable = true,
            validatorJsonAvailable = true,
            validatorMarkdownAvailable = true,
            finalReportJsonAvailable = true,
            exportManifestAvailable = true,
        )

        assertTrue(
            AutonomousValidationFixtureContracts
                .validateRunSummary(summary)
                .any { it.startsWith("runtime_failure_class_required") },
        )
    }

    @Test
    fun modelComparisonRejectsGraphCountRegressionAndMetricOverride() {
        val summary = AutonomousValidationModelComparisonSummary(
            fixtureId = WHITE_TIGER_ION71_FIXTURE_ID,
            deterministicRunId = "white_tiger_ion71_20260520_120000",
            modelEnabledRunId = "white_tiger_ion71_20260520_121000",
            deterministicGraphCount = 1,
            modelEnabledGraphCount = 0,
            deterministicNumericFingerprint = "rt=1.0|area=2.0",
            modelEnabledNumericFingerprint = "rt=9.0|area=9.0",
            modelChangedDeterministicMetrics = true,
        )

        val issues = AutonomousValidationFixtureContracts.validateModelComparison(summary)
        assertTrue("model_enabled_graph_count_regression:$WHITE_TIGER_ION71_FIXTURE_ID" in issues)
        assertTrue("model_enabled_numeric_metric_override:$WHITE_TIGER_ION71_FIXTURE_ID" in issues)
    }

    @Test
    fun sourceProvenanceSupportsValidationFixtureInput() {
        val provenance = SourceProvenance(
            originalImagePath = "/app/files/captures/validation/fixture.jpg",
            normalizedImagePath = "/app/files/captures/validation/fixture_normalized.png",
            sourceType = GeometrySourceType.VALIDATION_FIXTURE,
            originalWidth = 576,
            originalHeight = 1024,
            normalizedWidth = 576,
            normalizedHeight = 1024,
        )

        assertEquals(GeometrySourceType.VALIDATION_FIXTURE, provenance.sourceType)
    }

    @Test
    fun processingMetadataMapsValidationFixtureToReportInputSource() {
        val metadata = buildProcessingStoredReportMetadata(
            sourcePath = "/app/files/captures/validation/fixture.jpg",
            processedPath = "/app/files/captures/validation/fixture_processed.png",
            sourceType = SourceType.VALIDATION_FIXTURE,
            graphIndex = 1,
            detectedGraphCount = 1,
            signalPointCount = 128,
            analysisStartedAtEpochMillis = 10L,
            analysisCompletedAtEpochMillis = 20L,
        )

        assertEquals(InputSourceType.VALIDATION_FIXTURE, metadata.inputSourceType)
        assertEquals("validation-fixture-processing-flow", metadata.graphs.single().source?.scanMode)
    }

    @Test
    fun artifactManifestContainsRequiredValidationSlots() {
        val records = (
            AutonomousValidationFixtureContracts.requiredTextArtifactSlots +
                AutonomousValidationFixtureContracts.requiredOverlayArtifactSlots +
                AutonomousValidationFixtureContracts.requiredSupplementalArtifactSlots
            ).map { slot ->
            AutonomousValidationArtifactRecord(
                slot = slot,
                fileName = "$slot.placeholder",
                available = slot == "artifact_manifest",
                missingReason = if (slot == "artifact_manifest") null else "Not produced in contract-only test.",
            )
        }
        val manifest = AutonomousValidationRunArtifactManifest(
            runId = "white_tiger_ion71_20260520_120000",
            fixtureId = WHITE_TIGER_ION71_FIXTURE_ID,
            publicArtifactDirectory = "/sdcard/Download/ChromaLab/validation/white_tiger_ion71_20260520_120000",
            records = records,
        )
        val slots = manifest.records.map { it.slot }.toSet()

        AutonomousValidationFixtureContracts.requiredTextArtifactSlots.forEach { slot ->
            assertTrue(slot in slots, "Missing text artifact slot $slot")
        }
        AutonomousValidationFixtureContracts.requiredOverlayArtifactSlots.forEach { slot ->
            assertTrue(slot in slots, "Missing overlay artifact slot $slot")
        }
        AutonomousValidationFixtureContracts.requiredSupplementalArtifactSlots.forEach { slot ->
            assertTrue(slot in slots, "Missing supplemental artifact slot $slot")
        }
    }
}

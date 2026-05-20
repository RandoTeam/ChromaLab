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
import com.chromalab.feature.validation.AutonomousValidationModelMode
import com.chromalab.feature.validation.AutonomousValidationRunStart
import com.chromalab.feature.validation.AutonomousValidationRunArtifactManifest
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

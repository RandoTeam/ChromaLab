package com.chromalab.feature.validation

import com.chromalab.feature.reports.ReportGateStatus
import com.chromalab.feature.reports.RuntimeFailureClass
import kotlinx.serialization.Serializable

const val WHITE_TIGER_ION71_FIXTURE_ID = "white_tiger_ion71"
const val WHITE_TIGER_ION71_FIXTURE_METADATA_ASSET = "validation/white_tiger_ion71_fixture.metadata.json"

@Serializable
data class AutonomousValidationFixtureMetadata(
    val fixtureId: String,
    val displayName: String,
    val assetImagePath: String,
    val expectedGraphCount: Int,
    val expectedMode: String,
    val expectedReportGate: ReportGateStatus,
    val knownHistoricalFailures: List<RuntimeFailureClass>,
    val notes: List<String> = emptyList(),
)

@Serializable
data class AutonomousValidationRunStart(
    val runId: String,
    val fixtureId: String,
    val fixtureDisplayName: String,
    val sourceImagePath: String,
    val workingDirectory: String,
    val publicArtifactDirectory: String,
)

@Serializable
data class AutonomousValidationArtifactRecord(
    val slot: String,
    val fileName: String,
    val expected: Boolean = true,
    val available: Boolean,
    val location: String? = null,
    val missingReason: String? = null,
)

@Serializable
data class AutonomousValidationRunArtifactManifest(
    val runId: String,
    val fixtureId: String,
    val sourceType: String = "VALIDATION_FIXTURE",
    val publicArtifactDirectory: String,
    val records: List<AutonomousValidationArtifactRecord>,
)

data class ValidationArtifactSaveResult(
    val success: Boolean,
    val location: String? = null,
    val message: String,
)

object AutonomousValidationFixtureContracts {
    val requiredTextArtifactSlots: List<String> = listOf(
        "runtime_evidence_package",
        "runtime_evidence_validation_json",
        "runtime_evidence_validation_markdown",
        "final_report_contract_json",
        "report_html",
        "report_markdown",
        "stage_timings",
        "log_summary",
        "artifact_manifest",
    )

    val requiredOverlayArtifactSlots: List<String> = listOf(
        "graph_panel_overlay",
        "plot_area_overlay",
        "axis_tick_overlay",
        "calibration_overlay",
        "trace_overlay",
        "peak_overlay",
    )

    val requiredSupplementalArtifactSlots: List<String> = listOf(
        "final_screen_screenshot",
    )

    fun isSupportedFixture(fixtureId: String): Boolean =
        fixtureId == WHITE_TIGER_ION71_FIXTURE_ID

    fun validateMetadata(metadata: AutonomousValidationFixtureMetadata): List<String> = buildList {
        if (metadata.fixtureId.isBlank()) add("fixture_id_missing")
        if (!isSupportedFixture(metadata.fixtureId)) add("fixture_id_unsupported:${metadata.fixtureId}")
        if (metadata.displayName.isBlank()) add("display_name_missing")
        if (metadata.assetImagePath.isBlank()) add("asset_image_path_missing")
        if (metadata.expectedGraphCount <= 0) add("expected_graph_count_invalid")
        if (metadata.knownHistoricalFailures.isEmpty()) add("known_historical_failures_missing")
    }
}

expect object AutonomousValidationArtifactExporter {
    fun activeRunId(): String?
    fun saveTextArtifact(fileName: String, content: String, mimeType: String): ValidationArtifactSaveResult
    fun copyFileArtifact(sourcePath: String?, fileName: String, mimeType: String): ValidationArtifactSaveResult
}

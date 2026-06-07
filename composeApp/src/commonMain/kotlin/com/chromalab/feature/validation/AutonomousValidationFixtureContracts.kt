package com.chromalab.feature.validation

import com.chromalab.feature.reports.ReportGateStatus
import com.chromalab.feature.reports.RuntimeFailureClass
import kotlinx.serialization.Serializable

const val WHITE_TIGER_ION71_FIXTURE_ID = "white_tiger_ion71"
const val WHITE_TIGER_ION71_FIXTURE_METADATA_ASSET = "validation/white_tiger_ion71_fixture.metadata.json"
const val PHASE9B_ANDROID_VALIDATION_SUITE_ID = "phase9b_all"

private val phase9bFixtureMetadataAssets: Map<String, String> = mapOf(
    WHITE_TIGER_ION71_FIXTURE_ID to WHITE_TIGER_ION71_FIXTURE_METADATA_ASSET,
    "bench_01_mz71_screenshot_page" to "validation/bench_01_mz71_screenshot_page.metadata.json",
    "bench_02_mz92_belyi_tigr" to "validation/bench_02_mz92_belyi_tigr.metadata.json",
    "bench_03_small_tic_export" to "validation/bench_03_small_tic_export.metadata.json",
    "bench_04_stacked_xic_resolution" to "validation/bench_04_stacked_xic_resolution.metadata.json",
    "bench_05_tic_plus_ions" to "validation/bench_05_tic_plus_ions.metadata.json",
    "bench_06_photo_two_graphs_page" to "validation/bench_06_photo_two_graphs_page.metadata.json",
    "bench_07_rotated_page_photo" to "validation/bench_07_rotated_page_photo.metadata.json",
)

val PHASE9B_ANDROID_VALIDATION_FIXTURE_IDS: List<String> =
    phase9bFixtureMetadataAssets.keys.toList()

@Serializable
enum class AutonomousValidationModelMode {
    DETERMINISTIC_ONLY,
    MODEL_ENABLED;

    companion object {
        fun parse(value: String?): AutonomousValidationModelMode =
            when (value?.trim()?.lowercase()) {
                "model_enabled", "model-enabled", "model", "full", "full_analysis", "vlm", "gemma" -> MODEL_ENABLED
                "deterministic", "deterministic_only", "deterministic-only", "no_model", "no-model", "disabled" -> DETERMINISTIC_ONLY
                else -> DETERMINISTIC_ONLY
            }
    }
}

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
    val modelMode: AutonomousValidationModelMode,
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

@Serializable
enum class AutonomousValidationRunDecision {
    PASS,
    REVIEW,
    FAIL,
    BLOCKED,
}

@Serializable
data class AutonomousValidationSuiteRunSummary(
    val fixtureId: String,
    val modelMode: AutonomousValidationModelMode,
    val runId: String? = null,
    val expectedGraphCount: Int,
    val graphCount: Int? = null,
    val reportGate: ReportGateStatus? = null,
    val validatorVerdict: String? = null,
    val runtimeFailureClass: RuntimeFailureClass? = null,
    val runtimeEvidencePackageAvailable: Boolean = false,
    val validatorJsonAvailable: Boolean = false,
    val validatorMarkdownAvailable: Boolean = false,
    val finalReportJsonAvailable: Boolean = false,
    val exportManifestAvailable: Boolean = false,
    val criticalBlocker: Boolean = false,
)

@Serializable
data class AutonomousValidationModelComparisonSummary(
    val fixtureId: String,
    val deterministicRunId: String,
    val modelEnabledRunId: String,
    val deterministicGraphCount: Int,
    val modelEnabledGraphCount: Int,
    val deterministicNumericFingerprint: String? = null,
    val modelEnabledNumericFingerprint: String? = null,
    val modelChangedDeterministicMetrics: Boolean = false,
)

data class ValidationArtifactSaveResult(
    val success: Boolean,
    val location: String? = null,
    val message: String,
)

object AutonomousValidationFixtureContracts {
    const val graphFailurePackageSlot: String = "graph_failure_package"

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

    val requiredArtifactSlots: List<String> =
        requiredTextArtifactSlots + requiredOverlayArtifactSlots + requiredSupplementalArtifactSlots

    val phase9bFixtureIds: List<String> = PHASE9B_ANDROID_VALIDATION_FIXTURE_IDS

    val phase9bModelModes: List<AutonomousValidationModelMode> = listOf(
        AutonomousValidationModelMode.DETERMINISTIC_ONLY,
        AutonomousValidationModelMode.MODEL_ENABLED,
    )

    fun isSupportedFixture(fixtureId: String): Boolean =
        fixtureId in phase9bFixtureMetadataAssets

    fun metadataAssetFor(fixtureId: String): String? =
        phase9bFixtureMetadataAssets[fixtureId]

    fun validateMetadata(metadata: AutonomousValidationFixtureMetadata): List<String> = buildList {
        if (metadata.fixtureId.isBlank()) add("fixture_id_missing")
        if (!isSupportedFixture(metadata.fixtureId)) add("fixture_id_unsupported:${metadata.fixtureId}")
        if (metadata.displayName.isBlank()) add("display_name_missing")
        if (metadata.assetImagePath.isBlank()) add("asset_image_path_missing")
        if (metadata.expectedGraphCount <= 0) add("expected_graph_count_invalid")
        if (metadata.knownHistoricalFailures.isEmpty()) add("known_historical_failures_missing")
    }

    fun validatePhase9bFixtureSet(metadata: List<AutonomousValidationFixtureMetadata>): List<String> = buildList {
        val ids = metadata.map { it.fixtureId }
        val duplicates = ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        duplicates.forEach { add("fixture_id_duplicate:$it") }
        val missing = phase9bFixtureIds.filterNot(ids::contains)
        missing.forEach { add("phase9b_fixture_missing:$it") }
        if (metadata.size < 5) add("phase9b_fixture_count_below_minimum:${metadata.size}")
        metadata.flatMap { fixture ->
            validateMetadata(fixture).map { issue -> "${fixture.fixtureId}:$issue" }
        }.forEach(::add)
    }

    fun evaluateRun(summary: AutonomousValidationSuiteRunSummary): AutonomousValidationRunDecision =
        when {
            summary.criticalBlocker -> AutonomousValidationRunDecision.BLOCKED
            !summary.runtimeEvidencePackageAvailable ||
                !summary.validatorJsonAvailable ||
                !summary.validatorMarkdownAvailable ||
                !summary.finalReportJsonAvailable ||
                !summary.exportManifestAvailable -> AutonomousValidationRunDecision.BLOCKED
            summary.graphCount != null && summary.graphCount != summary.expectedGraphCount -> AutonomousValidationRunDecision.FAIL
            summary.validatorVerdict == "FAIL" -> AutonomousValidationRunDecision.FAIL
            summary.reportGate == ReportGateStatus.RELEASE_READY && summary.validatorVerdict == "PASS" ->
                AutonomousValidationRunDecision.PASS
            summary.validatorVerdict == "PASS" || summary.validatorVerdict == "REVIEW" ->
                AutonomousValidationRunDecision.REVIEW
            else -> AutonomousValidationRunDecision.FAIL
        }

    fun validateRunSummary(summary: AutonomousValidationSuiteRunSummary): List<String> = buildList {
        val decision = evaluateRun(summary)
        if (!isSupportedFixture(summary.fixtureId)) add("fixture_id_unsupported:${summary.fixtureId}")
        if (summary.expectedGraphCount <= 0) add("expected_graph_count_invalid")
        if (!summary.runtimeEvidencePackageAvailable) add("runtime_evidence_package_missing:${summary.fixtureId}:${summary.modelMode}")
        if (!summary.validatorJsonAvailable) add("validator_json_missing:${summary.fixtureId}:${summary.modelMode}")
        if (!summary.validatorMarkdownAvailable) add("validator_markdown_missing:${summary.fixtureId}:${summary.modelMode}")
        if (!summary.finalReportJsonAvailable) add("final_report_json_missing:${summary.fixtureId}:${summary.modelMode}")
        if (!summary.exportManifestAvailable) add("export_manifest_missing:${summary.fixtureId}:${summary.modelMode}")
        if (decision == AutonomousValidationRunDecision.FAIL || decision == AutonomousValidationRunDecision.BLOCKED) {
            if (summary.runtimeFailureClass == null) add("runtime_failure_class_required:${summary.fixtureId}:${summary.modelMode}")
        }
    }

    fun validateArtifactManifest(
        manifest: AutonomousValidationRunArtifactManifest,
        reportGate: ReportGateStatus? = null,
        runtimeFailureClass: RuntimeFailureClass? = null,
    ): List<String> = buildList {
        if (manifest.runId.isBlank()) add("manifest_run_id_missing")
        if (!isSupportedFixture(manifest.fixtureId)) add("manifest_fixture_id_unsupported:${manifest.fixtureId}")
        if (manifest.publicArtifactDirectory.isBlank()) add("manifest_public_directory_missing")

        val duplicateSlots = manifest.records
            .map { it.slot }
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        duplicateSlots.forEach { add("manifest_duplicate_slot:$it") }

        val slots = manifest.records.map { it.slot }.toSet()
        requiredArtifactSlots
            .filterNot(slots::contains)
            .forEach { add("manifest_required_slot_missing:$it") }

        val graphFailureRequired = reportGate == ReportGateStatus.BLOCKED ||
            runtimeFailureClass?.let(graphStageFailureClasses::contains) == true
        if (graphFailureRequired && graphFailurePackageSlot !in slots) {
            add("manifest_graph_failure_package_missing")
        }

        manifest.records.forEach { record ->
            if (record.slot.isBlank()) add("manifest_record_slot_missing")
            if (record.fileName.isBlank()) add("manifest_record_file_name_missing:${record.slot}")
            if (record.expected && !record.available && record.missingReason.isNullOrBlank()) {
                add("manifest_missing_reason_required:${record.slot}")
            }
            if (record.available && record.location.isNullOrBlank()) {
                add("manifest_available_location_missing:${record.slot}")
            }
        }
    }

    fun validateModelComparison(summary: AutonomousValidationModelComparisonSummary): List<String> = buildList {
        if (!isSupportedFixture(summary.fixtureId)) add("fixture_id_unsupported:${summary.fixtureId}")
        if (summary.deterministicGraphCount != summary.modelEnabledGraphCount) {
            add("model_enabled_graph_count_regression:${summary.fixtureId}")
        }
        if (summary.modelChangedDeterministicMetrics) {
            add("model_enabled_numeric_metric_override:${summary.fixtureId}")
        }
    }

    private val graphStageFailureClasses: Set<RuntimeFailureClass> = setOf(
        RuntimeFailureClass.GRAPH_PANEL_FAILURE,
        RuntimeFailureClass.MULTI_GRAPH_SPLIT_FAILURE,
        RuntimeFailureClass.PLOT_AREA_FAILURE,
        RuntimeFailureClass.AXIS_DETECTION_FAILURE,
        RuntimeFailureClass.TICK_LOCALIZATION_FAILURE,
        RuntimeFailureClass.OCR_TICK_FAILURE,
        RuntimeFailureClass.CALIBRATION_FAILURE,
        RuntimeFailureClass.TRACE_EXTRACTION_FAILURE,
        RuntimeFailureClass.PEAK_DETECTION_FAILURE,
        RuntimeFailureClass.PEAK_EVIDENCE_FAILURE,
        RuntimeFailureClass.CV_FALLBACK_GRAPH_PANEL_FAILURE,
        RuntimeFailureClass.PERFORMANCE_TIMEOUT,
    )
}

expect object AutonomousValidationArtifactExporter {
    fun activeRunId(): String?
    fun saveTextArtifact(fileName: String, content: String, mimeType: String): ValidationArtifactSaveResult
    fun copyFileArtifact(sourcePath: String?, fileName: String, mimeType: String): ValidationArtifactSaveResult
}

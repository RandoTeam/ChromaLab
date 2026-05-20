package com.chromalab.feature.processing.debug

import com.chromalab.feature.processing.storage.SessionWriter
import com.chromalab.feature.processing.geometry.GeometryPipelineResult
import com.chromalab.feature.processing.model.ModelAvailabilityDiagnostic
import com.chromalab.feature.reports.ChromatogramReport
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Exports a complete debug package: all intermediate images + data files.
 * Used for diagnostics and reproducing issues.
 */
object DebugPackageExporter {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }

    /**
     * Export debug info to JSON.
     */
    fun exportDebugInfo(info: DebugInfo): String = json.encodeToString(info)

    fun exportRuntimeEvidencePackage(
        report: ChromatogramReport,
        modelAvailabilityDiagnostics: List<ModelAvailabilityDiagnostic> = emptyList(),
    ): String =
        json.encodeToString(
            RuntimeEvidencePackageBuilder.build(
                report = report,
                modelAvailabilityDiagnostics = modelAvailabilityDiagnostics,
            ),
        )

    fun exportRoiFailureEvidencePackage(
        stageId: String,
        failureReason: String,
        geometryResult: GeometryPipelineResult?,
        originalImagePath: String?,
        normalizedImagePath: String?,
    ): String = json.encodeToString(
        RuntimeRoiFailureEvidencePackage(
            generatedAtEpochMillis = System.currentTimeMillis(),
            stageId = stageId,
            failureReason = failureReason,
            originalImagePath = originalImagePath,
            normalizedImagePath = normalizedImagePath,
            graphPanelCandidates = geometryResult?.trace?.roiCandidates.orEmpty(),
            selectedGraphPanel = geometryResult?.graphPanelBounds,
            selectedPlotArea = geometryResult?.plotAreaBounds,
            warnings = geometryResult?.warnings.orEmpty(),
            timings = geometryResult?.trace?.timings.orEmpty(),
        ),
    )

    fun validateRuntimeEvidencePackageJson(packageJson: String): String =
        RuntimeEvidencePackageValidator.exportJson(
            RuntimeEvidencePackageValidator.validateJson(packageJson),
        )

    fun validateRuntimeEvidencePackageMarkdown(packageJson: String): String =
        RuntimeEvidencePackageValidator.renderMarkdown(
            RuntimeEvidencePackageValidator.validateJson(packageJson),
        )

    /**
     * Write full debug package to the session directory.
     * Copies all intermediate images and writes debug_info.json.
     */
    fun writeDebugPackage(
        writer: SessionWriter,
        debugInfo: DebugInfo,
        intermediatePaths: Map<String, String>,
    ): List<String> {
        val files = mutableListOf<String>()

        // Write debug_info.json
        val infoPath = writer.writeText("debug_info.json", exportDebugInfo(debugInfo))
        files.add(infoPath)

        // Copy intermediate images
        for ((name, path) in intermediatePaths) {
            val copied = writer.copyFile(path, "debug_$name")
            if (copied != null) files.add(copied)
        }

        return files
    }

    fun writeRuntimeEvidencePackage(
        writer: SessionWriter,
        report: ChromatogramReport,
        graphIndex: Int? = null,
        modelAvailabilityDiagnostics: List<ModelAvailabilityDiagnostic> = emptyList(),
    ): String {
        val suffix = graphIndex?.let { "_graph_$it" }.orEmpty()
        return writer.writeText(
            filename = "runtime_evidence_package$suffix.json",
            content = exportRuntimeEvidencePackage(report, modelAvailabilityDiagnostics),
        )
    }

    fun writeRuntimeEvidencePackageWithValidation(
        writer: SessionWriter,
        report: ChromatogramReport,
        graphIndex: Int? = null,
        modelAvailabilityDiagnostics: List<ModelAvailabilityDiagnostic> = emptyList(),
    ): List<String> {
        val suffix = graphIndex?.let { "_graph_$it" }.orEmpty()
        val evidenceJson = exportRuntimeEvidencePackage(report, modelAvailabilityDiagnostics)
        val evidencePath = writer.writeText(
            filename = "runtime_evidence_package$suffix.json",
            content = evidenceJson,
        )
        val validationJsonPath = writer.writeText(
            filename = "runtime_evidence_validation$suffix.json",
            content = validateRuntimeEvidencePackageJson(evidenceJson),
        )
        val validationMarkdownPath = writer.writeText(
            filename = "runtime_evidence_validation$suffix.md",
            content = validateRuntimeEvidencePackageMarkdown(evidenceJson),
        )
        return listOf(evidencePath, validationJsonPath, validationMarkdownPath)
    }

    fun writeRoiFailureEvidencePackage(
        writer: SessionWriter,
        stageId: String,
        failureReason: String,
        geometryResult: GeometryPipelineResult?,
        originalImagePath: String?,
        normalizedImagePath: String?,
    ): String =
        writer.writeText(
            filename = "runtime_evidence_roi_failure.json",
            content = exportRoiFailureEvidencePackage(
                stageId = stageId,
                failureReason = failureReason,
                geometryResult = geometryResult,
                originalImagePath = originalImagePath,
                normalizedImagePath = normalizedImagePath,
            ),
        )
}

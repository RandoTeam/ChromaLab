package com.chromalab.feature.validation

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class AutonomousValidationFixtureRunner(
    private val context: Context,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun prepareFixture(
        fixtureId: String = WHITE_TIGER_ION71_FIXTURE_ID,
        modelMode: AutonomousValidationModelMode = AutonomousValidationModelMode.DETERMINISTIC_ONLY,
    ): Result<AutonomousValidationRunStart> =
        runCatching {
            if (!AutonomousValidationFixtureContracts.isSupportedFixture(fixtureId)) {
                error("Unsupported validation fixture: $fixtureId")
            }
            val metadata = loadMetadata()
            val metadataIssues = AutonomousValidationFixtureContracts.validateMetadata(metadata)
            if (metadataIssues.isNotEmpty()) {
                error("Invalid validation fixture metadata: ${metadataIssues.joinToString()}")
            }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
            val runId = "${metadata.fixtureId}_$timestamp"
            val runDir = File(context.filesDir, "captures/validation/$runId").apply { mkdirs() }
            val targetImage = File(runDir, metadata.assetImagePath.substringAfterLast('/'))
            context.assets.open(metadata.assetImagePath).use { input ->
                targetImage.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            File(runDir, "validation_fixture_metadata.json").writeText(json.encodeToString(AutonomousValidationFixtureMetadata.serializer(), metadata))
            val start = AutonomousValidationRunStart(
                runId = runId,
                fixtureId = metadata.fixtureId,
                fixtureDisplayName = metadata.displayName,
                modelMode = modelMode,
                sourceImagePath = targetImage.absolutePath,
                workingDirectory = runDir.absolutePath,
                publicArtifactDirectory = "/sdcard/Download/ChromaLab/validation/$runId",
            )
            File(runDir, "validation_fixture_start.json").writeText(json.encodeToString(AutonomousValidationRunStart.serializer(), start))
            start
        }

    private fun loadMetadata(): AutonomousValidationFixtureMetadata =
        context.assets.open(WHITE_TIGER_ION71_FIXTURE_METADATA_ASSET).bufferedReader().use { reader ->
            json.decodeFromString(AutonomousValidationFixtureMetadata.serializer(), reader.readText())
        }
}

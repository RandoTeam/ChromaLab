package com.chromalab.feature.processing.debug

import com.chromalab.feature.validation.AutonomousValidationFixtureContracts
import com.chromalab.feature.validation.AutonomousValidationFixtureMetadata
import com.chromalab.feature.validation.PHASE9B_ANDROID_VALIDATION_FIXTURE_IDS
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AutonomousValidationFixtureAssetTest {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun whiteTigerIon71AssetAndMetadataAreBundled() {
        val metadataFile = File("src/androidMain/assets/validation/white_tiger_ion71_fixture.metadata.json")
        assertTrue(metadataFile.isFile, "Missing validation fixture metadata asset.")

        val metadata = json.decodeFromString(
            AutonomousValidationFixtureMetadata.serializer(),
            metadataFile.readText(),
        )
        assertTrue(
            AutonomousValidationFixtureContracts.validateMetadata(metadata).isEmpty(),
            "Validation fixture metadata should satisfy the shared contract.",
        )

        val imageFile = File("src/androidMain/assets/${metadata.assetImagePath}")
        assertTrue(imageFile.isFile, "Missing validation fixture image asset.")
        assertTrue(imageFile.length() > 0L, "Validation fixture image asset is empty.")
    }

    @Test
    fun phase9bFixtureSuiteAssetsAndMetadataAreBundled() {
        val metadata = PHASE9B_ANDROID_VALIDATION_FIXTURE_IDS.map { fixtureId ->
            val metadataAsset = AutonomousValidationFixtureContracts.metadataAssetFor(fixtureId)
            assertTrue(metadataAsset != null, "Missing metadata asset mapping for $fixtureId.")
            val metadataFile = File("src/androidMain/assets/$metadataAsset")
            assertTrue(metadataFile.isFile, "Missing metadata asset for $fixtureId.")
            json.decodeFromString(
                AutonomousValidationFixtureMetadata.serializer(),
                metadataFile.readText(),
            )
        }

        val issues = AutonomousValidationFixtureContracts.validatePhase9bFixtureSet(metadata)
        assertTrue(issues.isEmpty(), "Phase 9B fixture metadata issues: $issues")

        metadata.forEach { fixture ->
            val imageFile = File("src/androidMain/assets/${fixture.assetImagePath}")
            assertTrue(imageFile.isFile, "Missing validation fixture image asset for ${fixture.fixtureId}.")
            assertTrue(imageFile.length() > 0L, "Validation fixture image asset is empty for ${fixture.fixtureId}.")
        }
    }
}

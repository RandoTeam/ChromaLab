package com.chromalab.feature.processing.debug

import com.chromalab.feature.validation.AutonomousValidationFixtureContracts
import com.chromalab.feature.validation.AutonomousValidationFixtureMetadata
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
}

package com.chromalab.feature.knowledge

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertTrue

class KnowledgeBuilderArtifactTest {

    @Test
    fun sourceRegisterDocumentsLicenseStatusAndBundlePolicy() {
        val sourcesFile = workspaceFile("tools/knowledge-builder/sources.yaml")

        assertTrue(sourcesFile.isFile, "tools/knowledge-builder/sources.yaml must exist.")
        val text = sourcesFile.readText()
        listOf(
            "chromalab-curated-v2",
            "chebi-cc-by-4",
            "pubchem-provenance-policy",
            "nist-srd-review-required",
            "license_status:",
            "can_bundle:",
            "can_transform:",
        ).forEach { needle ->
            assertTrue(text.contains(needle), "sources.yaml must contain $needle")
        }
    }

    @Test
    fun buildManifestRecordsRejectedSources() {
        val manifestFile = workspaceFile("tools/knowledge-builder/output/knowledge_build_manifest_v2.json")
        val rejectedFile = workspaceFile("tools/knowledge-builder/output/rejected_sources_v2.md")

        assertTrue(manifestFile.isFile, "builder manifest must be generated.")
        assertTrue(rejectedFile.isFile, "rejected-source report must be generated.")

        val manifest = json.parseToJsonElement(manifestFile.readText()).jsonObject
        val rejected = manifest.getValue("rejectedSourceIds").jsonArray.map { it.jsonPrimitive.content }.toSet()

        assertTrue("pubchem-provenance-policy" in rejected)
        assertTrue("nist-srd-review-required" in rejected)
        assertTrue(manifest.getValue("entryCount").jsonPrimitive.content.toInt() > 0)
    }

    private fun workspaceFile(path: String): File =
        listOf(File("../$path"), File(path)).first { it.exists() || it.parentFile?.exists() == true }

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}

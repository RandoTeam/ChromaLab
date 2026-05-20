package com.chromalab.feature.knowledge

import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChromaLabKnowledgeSeedFileTest {

    @Test
    fun docsSeedJsonFileDecodesAndMatchesRuntimeSeed() {
        val seedFile = listOf(
            File("../docs/knowledge/chromalab_knowledge_seed_v1.json"),
            File("docs/knowledge/chromalab_knowledge_seed_v1.json"),
        ).firstOrNull { it.isFile }

        assertTrue(seedFile?.isFile == true, "docs/knowledge/chromalab_knowledge_seed_v1.json must exist.")

        val decoded = json.decodeFromString<KnowledgePackVersion>(seedFile.readText())
        val validation = KnowledgeUsePolicyValidator.validatePack(decoded)

        assertTrue(validation.isValid, validation.errors.joinToString { "${it.path}: ${it.message}" })
        assertEquals(ChromaLabKnowledgeSeedV1.pack.version, decoded.version)
        assertEquals(
            ChromaLabKnowledgeSeedV1.pack.entries.map { it.entryId }.toSet(),
            decoded.entries.map { it.entryId }.toSet(),
        )
    }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

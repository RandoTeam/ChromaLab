package com.chromalab.feature.knowledge

import com.chromalab.feature.processing.multimodal.MultimodalTextRegionClass
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KnowledgeRetrievalLayerTest {

    @Test
    fun seedJsonValidates() {
        val decoded = json.decodeFromString<KnowledgePackVersion>(ChromaLabKnowledgeSeedV1.seedJson)
        val validation = KnowledgeUsePolicyValidator.validatePack(decoded)

        assertTrue(validation.isValid, validation.errors.joinToString { "${it.path}: ${it.message}" })
        assertEquals(CHROMALAB_KNOWLEDGE_PACK_VERSION_V1, decoded.version)
        assertTrue(decoded.entries.any { it.entryId == "kp-rule-ion-range-title-channel" })
    }

    @Test
    fun searchReturnsGlossaryTermByAlias() {
        val context = KnowledgeRetrievalEngine.search(
            query = KnowledgeSearchQuery("What does S/N mean in a chromatogram?"),
        )

        assertEquals("kp-glossary-sn", context.results.first().entryId)
    }

    @Test
    fun ionRangeRetrievesTitleOrChannelRule() {
        val context = KnowledgeRetrievalEngine.search(
            query = KnowledgeSearchQuery("Ion 71.00 (70.70 to 71.70) text classification"),
        )

        assertEquals("kp-rule-ion-range-title-channel", context.results.first().entryId)
        assertEquals(
            MultimodalTextRegionClass.TITLE_OR_CHANNEL,
            KnowledgeTextClassificationPolicy.classify("71.70", "Ion 71.00 (70.70 to 71.70)"),
        )
    }

    @Test
    fun ionRangeNumberCannotBecomePeakAnnotation() {
        assertFalse(
            KnowledgeTextClassificationPolicy.canBecomePeakAnnotation(
                text = "71.70",
                surroundingText = "Ion 71.00 (70.70 to 71.70)",
                hasLocalSignalVerification = true,
            ),
        )
    }

    @Test
    fun plotAnnotationCanRetrievePeakAnnotationRule() {
        val context = KnowledgeRetrievalEngine.search(
            query = KnowledgeSearchQuery("5.610 near plot annotation with local signal verification"),
        )

        assertEquals("kp-rule-peak-label-signal-verification", context.results.first().entryId)
        assertEquals(
            MultimodalTextRegionClass.PEAK_ANNOTATION,
            KnowledgeTextClassificationPolicy.classify(
                text = "5.610",
                surroundingText = "5.610",
                hasLocalSignalVerification = true,
            ),
        )
    }

    @Test
    fun kovatsCaveatRequiresReferenceSeries() {
        val context = KnowledgeRetrievalEngine.search(
            query = KnowledgeSearchQuery("Can report Kovats without n-alkane reference series?"),
        )

        assertTrue(context.results.any { it.entryId == "kp-caveat-no-kovats-without-reference" })
    }

    @Test
    fun knowledgeEntryCannotCreateNumericPeakMetric() {
        val context = KnowledgeRetrievalEngine.search(
            query = KnowledgeSearchQuery("knowledge cannot measure peak area"),
        )
        val result = KnowledgeUsePolicyValidator.validateOutput(
            output = KnowledgeGroundedVlmOutput(
                outputId = "knowledge-output:1",
                taskId = "report-warning:1",
                usedEntryIds = listOf("kp-safety-knowledge-cannot-measure"),
                decision = "reject_metric",
                confidence = 0.98f,
                explanation = "Knowledge cannot measure peak area.",
                attemptedUses = listOf("create_numeric_peak_metric"),
                createdNumericPeakMetric = true,
            ),
            retrievalContexts = listOf(context),
        )

        assertEquals(KnowledgeUseValidationVerdict.REJECTED, result.verdict)
        assertTrue(result.issues.any { it.code == "knowledge.created_numeric_peak_metric" })
    }

    @Test
    fun vlmScientificExplanationRequiresUsedEntryIds() {
        val result = KnowledgeUsePolicyValidator.validateOutput(
            output = KnowledgeGroundedVlmOutput(
                outputId = "knowledge-output:2",
                taskId = "report-warning:2",
                usedEntryIds = emptyList(),
                decision = "explain_warning",
                confidence = 0.7f,
                explanation = "No Kovats index can be reported.",
            ),
            retrievalContexts = emptyList(),
        )

        assertEquals(KnowledgeUseValidationVerdict.REVIEW, result.verdict)
        assertTrue(result.issues.any { it.code == "knowledge.used_entry_ids_missing" })
    }

    @Test
    fun unsupportedClaimBecomesReview() {
        val context = KnowledgeRetrievalEngine.search(
            query = KnowledgeSearchQuery("compound assignment requires explicit evidence"),
        )
        val result = KnowledgeUsePolicyValidator.validateOutput(
            output = KnowledgeGroundedVlmOutput(
                outputId = "knowledge-output:3",
                taskId = "report-warning:3",
                usedEntryIds = listOf("kp-caveat-no-compound-without-evidence"),
                decision = "review",
                confidence = 0.6f,
                explanation = "The compound is probably toluene.",
                unsupportedClaims = listOf("compound identity inferred without library evidence"),
            ),
            retrievalContexts = listOf(context),
        )

        assertEquals(KnowledgeUseValidationVerdict.REVIEW, result.verdict)
        assertTrue(result.issues.any { it.code == "knowledge.unsupported_claims" })
    }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
}

package com.chromalab.feature.knowledge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KnowledgePackV2Test {

    @Test
    fun seedV2ValidatesWithUniqueIds() {
        val validation = KnowledgePackValidator.validatePack(ChromaLabKnowledgeSeedV2.pack)

        assertTrue(validation.isValid, validation.errors.joinToString { "${it.path}: ${it.message}" })
        assertEquals(
            ChromaLabKnowledgeSeedV2.pack.entries.size,
            ChromaLabKnowledgeSeedV2.pack.entries.map { it.entryId }.toSet().size,
        )
        assertTrue(ChromaLabKnowledgeSeedV2.pack.entries.any { it.entryId == "kp2-compound-stub-n-c10-alkane" })
        assertTrue(ChromaLabKnowledgeSeedV2.pack.entries.any { it.entryId == "kp2-compound-stub-n-c40-alkane" })
    }

    @Test
    fun exactAliasLookupWorks() {
        val context = KnowledgeRetrievalEngine.search(
            pack = ChromaLabKnowledgeSeedV2.pack,
            query = KnowledgeSearchQuery(
                rawQuery = "S/N",
                exactAliasOnly = true,
            ),
        )

        assertEquals("kp2-term-sn", context.results.first().entryId)
    }

    @Test
    fun typeLanguageAndAllowedUseFiltersWork() {
        val context = KnowledgeRetrievalEngine.search(
            pack = ChromaLabKnowledgeSeedV2.pack,
            query = KnowledgeSearchQuery(
                rawQuery = "ion channel title not peak label",
                requestedTypes = listOf(KnowledgeEntryType.TEXT_CLASSIFICATION_RULE),
                languages = listOf("en"),
                allowedUses = listOf("text_classification"),
            ),
        )

        assertTrue(context.results.isNotEmpty())
        assertTrue(context.results.all { it.entry.type == KnowledgeEntryType.TEXT_CLASSIFICATION_RULE })
        assertTrue(context.results.all { it.entry.language == "en" })
        assertTrue(context.results.all { "text_classification" in it.entry.policy.allowedUse })
    }

    @Test
    fun forbiddenUseAndSourceRefsArePreservedInResults() {
        val result = KnowledgeRetrievalEngine.search(
            pack = ChromaLabKnowledgeSeedV2.pack,
            query = KnowledgeSearchQuery("knowledge cannot create numeric metrics"),
        ).results.first()

        assertTrue("create_numeric_peak_metric" in result.forbiddenUse)
        assertTrue(result.sourceRefs.isNotEmpty())
    }

    @Test
    fun ionRangeRetrievesTitleRuleAndCannotBecomePeakAnnotation() {
        val context = KnowledgeRetrievalEngine.search(
            pack = ChromaLabKnowledgeSeedV2.pack,
            query = KnowledgeSearchQuery("Ion 71.00 (70.70 to 71.70)"),
        )

        assertEquals("kp2-rule-ion-title-not-peak", context.results.first().entryId)
        assertFalse(
            KnowledgeTextClassificationPolicy.canBecomePeakAnnotation(
                text = "71.70",
                surroundingText = "Ion 71.00 (70.70 to 71.70)",
                hasLocalSignalVerification = true,
            ),
        )
    }

    @Test
    fun mzAndMassRangePatternsAreNotPeakAnnotations() {
        val examples = listOf(
            "Ion 92.00 (91.70 to 92.70)",
            "m/z 57",
            "mass range 70.70 to 71.70",
        )

        examples.forEach { text ->
            assertFalse(
                KnowledgeTextClassificationPolicy.canBecomePeakAnnotation(
                    text = "71.70",
                    surroundingText = text,
                    hasLocalSignalVerification = true,
                ),
                "$text must not become a peak annotation.",
            )
        }
    }

    @Test
    fun plotAnnotationRetrievesSignalVerificationRule() {
        val context = KnowledgeRetrievalEngine.search(
            pack = ChromaLabKnowledgeSeedV2.pack,
            query = KnowledgeSearchQuery("5.610 near plot annotation requires local signal verification"),
        )

        assertEquals("kp2-rule-peak-annotation-signal-verified", context.results.first().entryId)
    }

    @Test
    fun kovatsAndCompoundCaveatsAreAvailable() {
        val kovats = KnowledgeRetrievalEngine.search(
            pack = ChromaLabKnowledgeSeedV2.pack,
            query = KnowledgeSearchQuery("Kovats without reference series"),
        )
        val compound = KnowledgeRetrievalEngine.search(
            pack = ChromaLabKnowledgeSeedV2.pack,
            query = KnowledgeSearchQuery("compound assignment without explicit evidence"),
        )

        assertTrue(kovats.results.any { it.entryId == "kp2-caveat-no-kovats-without-reference" })
        assertTrue(compound.results.any { it.entryId == "kp2-caveat-no-compound-assignment" })
    }

    @Test
    fun knowledgeCannotCreateMetricsAndVlmMustCiteEntries() {
        val context = KnowledgeRetrievalEngine.search(
            pack = ChromaLabKnowledgeSeedV2.pack,
            query = KnowledgeSearchQuery("knowledge cannot create numeric metrics"),
        )
        val rejected = KnowledgeUsePolicyValidator.validateOutput(
            output = KnowledgeGroundedVlmOutput(
                outputId = "kp2-output:metric",
                taskId = "report-warning",
                usedEntryIds = listOf("kp2-safety-knowledge-cannot-measure"),
                decision = "reject_metric",
                confidence = 0.99f,
                explanation = "Knowledge cannot create peak area.",
                attemptedUses = listOf("create_numeric_peak_metric"),
                createdNumericPeakMetric = true,
            ),
            retrievalContexts = listOf(context),
        )
        val uncited = KnowledgeUsePolicyValidator.validateOutput(
            output = KnowledgeGroundedVlmOutput(
                outputId = "kp2-output:uncited",
                taskId = "report-warning",
                usedEntryIds = emptyList(),
                decision = "review",
                confidence = 0.4f,
                explanation = "This is a scientific report explanation.",
            ),
            retrievalContexts = listOf(context),
        )
        val unsupported = KnowledgeUsePolicyValidator.validateOutput(
            output = KnowledgeGroundedVlmOutput(
                outputId = "kp2-output:unsupported",
                taskId = "report-warning",
                usedEntryIds = listOf("kp2-caveat-no-compound-assignment"),
                decision = "review",
                confidence = 0.5f,
                explanation = "Compound identity was inferred.",
                unsupportedClaims = listOf("compound identity without evidence"),
            ),
            retrievalContexts = listOf(
                KnowledgeRetrievalEngine.search(
                    pack = ChromaLabKnowledgeSeedV2.pack,
                    query = KnowledgeSearchQuery("compound assignment without explicit evidence"),
                ),
            ),
        )

        assertEquals(KnowledgeUseValidationVerdict.REJECTED, rejected.verdict)
        assertEquals(KnowledgeUseValidationVerdict.REVIEW, uncited.verdict)
        assertEquals(KnowledgeUseValidationVerdict.REVIEW, unsupported.verdict)
    }
}

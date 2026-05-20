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
        assertTrue(ChromaLabKnowledgeSeedV2.pack.entries.all { EvidenceClaimScope.NOT_MEASUREMENT in it.claimScopes })
        assertTrue(ChromaLabKnowledgeSeedV2.pack.sourceRefs.any { it.trustTier == KnowledgeSourceTrustTier.TIER_3_LINK_ONLY_RESTRICTED })
    }

    @Test
    fun validatorFailsClosedForRestrictedSourcesAndClaimScopes() {
        val basePack = ChromaLabKnowledgeSeedV2.pack
        val restrictedEntry = basePack.entries.first().copy(
            entryId = "test-restricted-entry",
            sourceRefIds = listOf("pubchem-provenance-policy"),
            licenseStatus = KnowledgeLicenseStatus.NEEDS_REVIEW,
            trustTier = KnowledgeSourceTrustTier.TIER_3_LINK_ONLY_RESTRICTED,
        )
        val missingClaimScopeEntry = basePack.entries.first().copy(
            entryId = "test-missing-not-measurement",
            claimScopes = listOf(EvidenceClaimScope.EXPLANATION_ONLY),
        )

        val validation = KnowledgePackValidator.validatePack(
            basePack.copy(entries = listOf(restrictedEntry, missingClaimScopeEntry)),
        )

        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { it.path.contains("test-restricted-entry") && it.path.contains("sourceRefIds") })
        assertTrue(validation.errors.any { it.path.contains("test-restricted-entry") && it.path.contains("trustTier") })
        assertTrue(validation.errors.any { it.path.contains("test-missing-not-measurement") && it.path.contains("claimScopes") })
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
    fun compactRetrievalCardsExposeOnlyBoundedGemmaFields() {
        val context = KnowledgeRetrievalEngine.search(
            pack = ChromaLabKnowledgeSeedV2.pack,
            query = KnowledgeSearchQuery("calibration required release metrics"),
        )
        val cards = KnowledgeUsePolicyValidator.cardsFor(context, ChromaLabKnowledgeSeedV2.pack)

        assertTrue(cards.isNotEmpty())
        assertTrue(cards.all { it.shortCard.length <= 420 })
        assertTrue(cards.all { it.sourceRefs.isNotEmpty() })
        assertTrue(cards.all { it.forbiddenUse.contains("create_numeric_peak_metric") })
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

    @Test
    fun forbiddenCompoundAndKovatsUsesAreRejected() {
        val compoundContext = KnowledgeRetrievalEngine.search(
            pack = ChromaLabKnowledgeSeedV2.pack,
            query = KnowledgeSearchQuery("compound assignment without explicit evidence"),
        )
        val kovatsContext = KnowledgeRetrievalEngine.search(
            pack = ChromaLabKnowledgeSeedV2.pack,
            query = KnowledgeSearchQuery("Kovats without reference series"),
        )

        val compound = KnowledgeUsePolicyValidator.validateOutput(
            output = KnowledgeGroundedVlmOutput(
                outputId = "kp2-output:compound-forbidden",
                taskId = "report-warning",
                usedEntryIds = listOf("kp2-caveat-no-compound-assignment"),
                decision = "reject",
                confidence = 0.9f,
                explanation = "Compound identification requires evidence.",
                attemptedUses = listOf("identify_compound_without_evidence"),
            ),
            retrievalContexts = listOf(compoundContext),
        )
        val kovats = KnowledgeUsePolicyValidator.validateOutput(
            output = KnowledgeGroundedVlmOutput(
                outputId = "kp2-output:kovats-forbidden",
                taskId = "report-warning",
                usedEntryIds = listOf("kp2-caveat-no-kovats-without-reference"),
                decision = "reject",
                confidence = 0.9f,
                explanation = "Kovats index requires a reference series.",
                attemptedUses = listOf("fabricate_kovats"),
            ),
            retrievalContexts = listOf(kovatsContext),
        )

        assertEquals(KnowledgeUseValidationVerdict.REJECTED, compound.verdict)
        assertEquals(KnowledgeUseValidationVerdict.REJECTED, kovats.verdict)
    }

    @Test
    fun adversarialTextRulesSeparateChannelMetadataFromPeakAnnotation() {
        assertEquals(
            KnowledgeTextRuleClass.TITLE_OR_CHANNEL,
            KnowledgeRuleEngine.classifyText("71.70", "Ion 71.00 (70.70 to 71.70)").textClass,
        )
        assertEquals(
            KnowledgeTextRuleClass.ION_CHANNEL_TERM,
            KnowledgeRuleEngine.classifyText("m/z 57", "m/z 57").textClass,
        )
        assertEquals(
            KnowledgeTextRuleClass.METHOD_METADATA,
            KnowledgeRuleEngine.classifyText("SIM 85", "SIM 85").textClass,
        )
        assertEquals(
            KnowledgeTextRuleClass.PEAK_ANNOTATION_CANDIDATE,
            KnowledgeRuleEngine.classifyText(
                text = "5.610",
                surroundingText = "5.610",
                isNearApex = true,
                hasLocalSignalVerification = true,
            ).textClass,
        )
        assertEquals(
            KnowledgeTextRuleClass.NOT_PEAK_ANNOTATION,
            KnowledgeRuleEngine.classifyText(
                text = "5.610",
                surroundingText = "Title 5.610",
                isInTitleOrHeader = true,
                isNearApex = true,
                hasLocalSignalVerification = true,
            ).textClass,
        )
    }

    @Test
    fun ruleLayerProducesReleaseGateCaveatsOnly() {
        val caveats = KnowledgeRuleEngine.caveatsForReleaseGate(listOf("kovats", "calibration"))

        assertTrue(caveats.any { it.ruleId == "kp2-caveat-no-kovats-without-reference" })
        assertTrue(caveats.any { it.ruleId == "kp2-caveat-calibration-required" })
        assertTrue(caveats.all { it.textClass == KnowledgeTextRuleClass.NOT_PEAK_ANNOTATION })
    }

    @Test
    fun compoundNameWithoutEvidenceStaysCaveatNotIdentificationClaim() {
        val context = KnowledgeRetrievalEngine.search(
            pack = ChromaLabKnowledgeSeedV2.pack,
            query = KnowledgeSearchQuery("compound name without spectral RI library evidence"),
        )

        assertTrue(context.results.any { it.entryId == "kp2-caveat-no-compound-assignment" })
        assertTrue(context.results.first { it.entryId == "kp2-caveat-no-compound-assignment" }
            .entry.policy.forbiddenUse.contains("identify_compound_without_evidence"))
    }
}

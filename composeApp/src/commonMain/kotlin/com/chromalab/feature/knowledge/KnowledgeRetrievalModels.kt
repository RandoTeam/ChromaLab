package com.chromalab.feature.knowledge

import com.chromalab.feature.processing.multimodal.MultimodalTextRegionClass
import kotlinx.serialization.Serializable

const val CHROMALAB_KNOWLEDGE_PACK_SCHEMA_VERSION = "chromalab-knowledge-pack-1.0"
const val CHROMALAB_KNOWLEDGE_PACK_ID = "chromalab-knowledge"
const val CHROMALAB_KNOWLEDGE_PACK_VERSION_V1 = "chromalab-knowledge-v1"
const val CHROMALAB_KNOWLEDGE_PACK_VERSION_V2 = "chromalab-knowledge-v2"

@Serializable
data class KnowledgePackVersion(
    val packId: String = CHROMALAB_KNOWLEDGE_PACK_ID,
    val version: String = CHROMALAB_KNOWLEDGE_PACK_VERSION_V1,
    val schemaVersion: String = CHROMALAB_KNOWLEDGE_PACK_SCHEMA_VERSION,
    val title: String,
    val description: String,
    val sourceRefs: List<KnowledgeSourceRef>,
    val entries: List<KnowledgeEntry>,
)

@Serializable
data class KnowledgeEntry(
    val entryId: String,
    val version: String = CHROMALAB_KNOWLEDGE_PACK_VERSION_V1,
    val type: KnowledgeEntryType,
    val canonicalLabel: String = entryId,
    val language: String = "en",
    val shortText: String,
    val longText: String? = null,
    val aliases: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val sourceRefIds: List<String>,
    val licenseStatus: KnowledgeLicenseStatus = KnowledgeLicenseStatus.INTERNAL_CURATED,
    val trustTier: KnowledgeSourceTrustTier = KnowledgeSourceTrustTier.INTERNAL_CURATED,
    val claimScopes: List<EvidenceClaimScope> = listOf(EvidenceClaimScope.NOT_MEASUREMENT),
    val confidence: Float = 1f,
    val lastReviewed: String = "2026-05-20",
    val tags: List<String> = emptyList(),
    val policy: KnowledgeUsePolicy,
)

@Serializable
enum class KnowledgeEntryType {
    GLOSSARY_TERM,
    TEXT_CLASSIFICATION_RULE,
    REPORT_CAVEAT,
    METHOD_RULE,
    CHEMICAL_SYNONYM,
    COMPOUND_CLASS,
    ION_CHANNEL_TERM,
    RETENTION_INDEX_RULE,
    PROMPT_SNIPPET,
    SAFETY_BOUNDARY,
    KNOWN_PATTERN,
    UNIT_TERM,
    AXIS_TERM,
    MASS_SPECTROMETRY_TERM,
    CHROMATOGRAPHY_METHOD_TERM,
    COMPOUND_REFERENCE_STUB,
}

@Serializable
data class KnowledgeSourceRef(
    val sourceId: String,
    val label: String,
    val url: String? = null,
    val citation: String? = null,
    val license: String? = null,
    val licenseStatus: KnowledgeLicenseStatus = KnowledgeLicenseStatus.INTERNAL_CURATED,
    val trustTier: KnowledgeSourceTrustTier = KnowledgeSourceTrustTier.INTERNAL_CURATED,
    val attributionRequired: Boolean = false,
    val canBundle: Boolean = false,
    val canTransform: Boolean = false,
    val apiLookupOnly: Boolean = false,
    val notes: String? = null,
)

@Serializable
enum class KnowledgeLicenseStatus {
    INTERNAL_CURATED,
    OPEN_VERIFIED,
    ATTRIBUTION_REQUIRED,
    API_ONLY,
    REJECTED,
    NEEDS_REVIEW,
    PROPRIETARY_FORBIDDEN,
}

@Serializable
enum class KnowledgeSourceTrustTier {
    TIER_0_INTERNAL_CURATED,
    TIER_1_OPEN_REFERENCE,
    TIER_2_OPEN_SPECTRAL_REFERENCE,
    TIER_3_LINK_ONLY_RESTRICTED,
    TIER_4_REJECTED,
    INTERNAL_CURATED,
    OFFICIAL_STANDARD,
    OFFICIAL_DATABASE,
    OPEN_ONTOLOGY,
    PEER_REVIEWED,
    MAINTAINED_REPOSITORY,
    USER_SUPPLIED_UNVERIFIED,
    REJECTED,
}

@Serializable
enum class EvidenceClaimScope {
    EXPLANATION_ONLY,
    TEXT_CLASSIFICATION,
    REPORT_CAVEAT,
    RETRIEVAL_CONTEXT,
    COMPOUND_DICTIONARY,
    SPECTRAL_REFERENCE_LINK,
    NOT_MEASUREMENT,
}

@Serializable
data class KnowledgeAttribution(
    val sourceId: String,
    val requiredText: String,
    val licenseUrl: String? = null,
    val accessDate: String? = null,
)

@Serializable
data class KnowledgeBuildManifest(
    val packId: String,
    val version: String,
    val schemaVersion: String,
    val generatedAt: String,
    val builderVersion: String,
    val sourceIds: List<String>,
    val entryCount: Int,
    val rejectedSourceIds: List<String> = emptyList(),
)

@Serializable
data class KnowledgeValidationIssue(
    val severity: KnowledgeValidationSeverity,
    val path: String,
    val code: String,
    val message: String,
)

@Serializable
enum class KnowledgeValidationSeverity {
    WARNING,
    ERROR,
}

@Serializable
data class KnowledgeUsePolicy(
    val allowedUse: List<String>,
    val forbiddenUse: List<String>,
)

@Serializable
data class KnowledgeSearchQuery(
    val rawQuery: String,
    val requestedTypes: List<KnowledgeEntryType> = emptyList(),
    val languages: List<String> = emptyList(),
    val allowedUses: List<String> = emptyList(),
    val exactAliasOnly: Boolean = false,
    val maxResults: Int = 8,
)

@Serializable
data class KnowledgeSearchResult(
    val entryId: String,
    val entry: KnowledgeEntry,
    val score: Double,
    val matchedTerms: List<String> = emptyList(),
    val sourceRefs: List<KnowledgeSourceRef> = emptyList(),
    val forbiddenUse: List<String> = entry.policy.forbiddenUse,
)

@Serializable
data class KnowledgeRetrievalContext(
    val retrievalId: String,
    val knowledgePackVersion: String,
    val query: KnowledgeSearchQuery,
    val results: List<KnowledgeSearchResult>,
)

@Serializable
data class KnowledgeGroundedSnippet(
    val entryId: String,
    val version: String,
    val shortText: String,
    val allowedUse: List<String>,
    val forbiddenUse: List<String>,
    val sourceRefs: List<KnowledgeSourceRef>,
)

@Serializable
data class KnowledgeRetrievalCard(
    val entryId: String,
    val shortCard: String,
    val allowedUse: List<String>,
    val forbiddenUse: List<String>,
    val sourceRefs: List<KnowledgeSourceRef>,
    val confidence: Float,
    val trustTier: KnowledgeSourceTrustTier,
)

@Serializable
data class KnowledgeGroundedVlmOutput(
    val outputId: String,
    val taskId: String,
    val usedEntryIds: List<String>,
    val decision: String,
    val confidence: Float,
    val explanation: String,
    val unsupportedClaims: List<String> = emptyList(),
    val attemptedUses: List<String> = emptyList(),
    val rejectedForbiddenUses: List<String> = emptyList(),
    val createdNumericPeakMetric: Boolean = false,
    val overrodeCalibrationOrIntegration: Boolean = false,
)

@Serializable
data class KnowledgeUseValidationIssue(
    val code: String,
    val message: String,
)

@Serializable
enum class KnowledgeUseValidationVerdict {
    ACCEPTED,
    REVIEW,
    REJECTED,
}

@Serializable
data class KnowledgeUseValidationResult(
    val verdict: KnowledgeUseValidationVerdict,
    val issues: List<KnowledgeUseValidationIssue> = emptyList(),
)

object KnowledgePackValidator {
    private val nonBundleableLicenseStatuses = setOf(
        KnowledgeLicenseStatus.API_ONLY,
        KnowledgeLicenseStatus.REJECTED,
        KnowledgeLicenseStatus.NEEDS_REVIEW,
        KnowledgeLicenseStatus.PROPRIETARY_FORBIDDEN,
    )
    private val nonBundleableTrustTiers = setOf(
        KnowledgeSourceTrustTier.TIER_3_LINK_ONLY_RESTRICTED,
        KnowledgeSourceTrustTier.TIER_4_REJECTED,
        KnowledgeSourceTrustTier.REJECTED,
        KnowledgeSourceTrustTier.USER_SUPPLIED_UNVERIFIED,
    )
    private val legacyBundleableTrustTiers = setOf(
        KnowledgeSourceTrustTier.INTERNAL_CURATED,
        KnowledgeSourceTrustTier.OFFICIAL_STANDARD,
        KnowledgeSourceTrustTier.OPEN_ONTOLOGY,
        KnowledgeSourceTrustTier.PEER_REVIEWED,
        KnowledgeSourceTrustTier.MAINTAINED_REPOSITORY,
    )
    private val forbiddenMetricUses = setOf(
        "fabricate_rt",
        "fabricate_height",
        "fabricate_area",
        "fabricate_fwhm",
        "fabricate_sn",
        "fabricate_baseline",
        "fabricate_kovats",
        "create_numeric_peak_metric",
        "override_calibration",
        "override_integration",
        "identify_compound_without_evidence",
    )

    fun validatePack(pack: KnowledgePackVersion): KnowledgePackValidationResult {
        val errors = mutableListOf<KnowledgePackIssue>()
        val warnings = mutableListOf<KnowledgePackIssue>()
        if (pack.packId.isBlank()) errors.add(KnowledgePackIssue("packId", "value must not be blank."))
        if (pack.version.isBlank()) errors.add(KnowledgePackIssue("version", "value must not be blank."))
        if (!pack.version.matches(Regex("""[A-Za-z0-9_.:-]+"""))) {
            errors.add(KnowledgePackIssue("version", "version must be stable and machine-readable."))
        }
        val sourceIds = pack.sourceRefs.map { it.sourceId }.toSet()
        val sourcesById = pack.sourceRefs.associateBy { it.sourceId }
        if (sourceIds.size != pack.sourceRefs.size) errors.add(KnowledgePackIssue("sourceRefs", "source IDs must be unique."))
        val entryIds = pack.entries.map { it.entryId }
        if (entryIds.toSet().size != entryIds.size) errors.add(KnowledgePackIssue("entries", "entry IDs must be unique."))
        pack.sourceRefs.forEach { source ->
            if (source.sourceId.isBlank()) errors.add(KnowledgePackIssue("sourceRefs.sourceId", "value must not be blank."))
            if (source.label.isBlank()) errors.add(KnowledgePackIssue("sourceRefs[${source.sourceId}].label", "value must not be blank."))
            if (source.license.isNullOrBlank()) warnings.add(KnowledgePackIssue("sourceRefs[${source.sourceId}].license", "license text is recommended."))
            if (source.licenseStatus == KnowledgeLicenseStatus.PROPRIETARY_FORBIDDEN && source.canBundle) {
                errors.add(KnowledgePackIssue("sourceRefs[${source.sourceId}].canBundle", "forbidden proprietary source cannot be marked bundleable."))
            }
            if (source.licenseStatus in nonBundleableLicenseStatuses && source.canBundle) {
                errors.add(KnowledgePackIssue("sourceRefs[${source.sourceId}].canBundle", "source with ${source.licenseStatus} must not be marked bundleable."))
            }
            if (source.trustTier in nonBundleableTrustTiers && source.canBundle) {
                errors.add(KnowledgePackIssue("sourceRefs[${source.sourceId}].trustTier", "restricted or rejected source tier must not be marked bundleable."))
            }
        }
        pack.entries.forEach { entry ->
            if (entry.entryId.isBlank()) errors.add(KnowledgePackIssue("entry.entryId", "value must not be blank."))
            if (entry.canonicalLabel.isBlank()) errors.add(KnowledgePackIssue("entry[${entry.entryId}].canonicalLabel", "value must not be blank."))
            if (entry.language !in setOf("en", "ru", "und")) {
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].language", "language must be 'en', 'ru', or 'und'."))
            }
            if (entry.shortText.isBlank()) errors.add(KnowledgePackIssue("entry[${entry.entryId}].shortText", "value must not be blank."))
            if (entry.lastReviewed.isBlank()) errors.add(KnowledgePackIssue("entry[${entry.entryId}].lastReviewed", "last reviewed date is required."))
            if (entry.confidence !in 0f..1f) errors.add(KnowledgePackIssue("entry[${entry.entryId}].confidence", "confidence must be between 0 and 1."))
            val normalizedAliases = entry.aliases.map { it.trim().lowercase() }
            if (entry.aliases.any { it.isBlank() }) {
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].aliases", "aliases must not be blank."))
            }
            if (normalizedAliases.toSet().size != normalizedAliases.size) {
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].aliases", "aliases must be unique after normalization."))
            }
            if (entry.sourceRefIds.isEmpty()) errors.add(KnowledgePackIssue("entry[${entry.entryId}].sourceRefIds", "at least one source reference is required."))
            if (entry.claimScopes.isEmpty()) errors.add(KnowledgePackIssue("entry[${entry.entryId}].claimScopes", "at least one claim scope is required."))
            if (EvidenceClaimScope.NOT_MEASUREMENT !in entry.claimScopes) {
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].claimScopes", "every bundled entry must declare NOT_MEASUREMENT."))
            }
            entry.sourceRefIds.filterNot(sourceIds::contains).forEach { missing ->
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].sourceRefIds", "unknown source ref '$missing'."))
            }
            if (entry.policy.allowedUse.isEmpty()) {
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].allowedUse", "at least one allowed use is required."))
            }
            if (entry.policy.forbiddenUse.isEmpty()) {
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].forbiddenUse", "at least one forbidden use is required."))
            }
            if (entry.policy.allowedUse.any { it.isBlank() } || entry.policy.forbiddenUse.any { it.isBlank() }) {
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].policy", "allowed and forbidden use entries must not be blank."))
            }
            if (entry.licenseStatus == KnowledgeLicenseStatus.PROPRIETARY_FORBIDDEN) {
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].licenseStatus", "proprietary forbidden entries must not be bundled."))
            }
            if (entry.licenseStatus in nonBundleableLicenseStatuses) {
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].licenseStatus", "entry from ${entry.licenseStatus} source must not be bundled."))
            }
            if (entry.trustTier in nonBundleableTrustTiers) {
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].trustTier", "entry from restricted or rejected source tier must not be bundled."))
            }
            entry.sourceRefIds.mapNotNull(sourcesById::get).forEach { source ->
                if (!source.isBundleableForEntries() || source.licenseStatus in nonBundleableLicenseStatuses || source.trustTier in nonBundleableTrustTiers) {
                    errors.add(
                        KnowledgePackIssue(
                            "entry[${entry.entryId}].sourceRefIds",
                            "entry cannot bundle source '${source.sourceId}' with status ${source.licenseStatus} and tier ${source.trustTier}.",
                        ),
                    )
                }
            }
            if (entry.shortText.contains(Regex("""(?i)\b(?:measured\s+)?(?:rt|height|area|fwhm|s/n|snr|baseline|kovats)\s*[:=]\s*[0-9]""")) &&
                "test_fixture_only" !in entry.policy.forbiddenUse
            ) {
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].shortText", "entries must not contain sample-specific measured metrics."))
            }
        }
        return KnowledgePackValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
        )
    }

    private fun KnowledgeSourceRef.isBundleableForEntries(): Boolean =
        canBundle ||
            licenseStatus == KnowledgeLicenseStatus.INTERNAL_CURATED ||
            trustTier in legacyBundleableTrustTiers

    fun validateOutput(
        output: KnowledgeGroundedVlmOutput,
        retrievalContexts: List<KnowledgeRetrievalContext>,
    ): KnowledgeUseValidationResult {
        val issues = mutableListOf<KnowledgeUseValidationIssue>()
        val retrievedEntries = retrievalContexts
            .flatMap { it.results.map { result -> result.entry } }
            .associateBy { it.entryId }

        if (output.explanation.isNotBlank() && output.usedEntryIds.isEmpty()) {
            issues.add(
                KnowledgeUseValidationIssue(
                    code = "knowledge.used_entry_ids_missing",
                    message = "Scientific or report explanation must cite used_entry_ids.",
                ),
            )
        }

        output.usedEntryIds.filterNot(retrievedEntries::containsKey).forEach { missing ->
            issues.add(
                KnowledgeUseValidationIssue(
                    code = "knowledge.used_entry_id_not_retrieved",
                    message = "Used knowledge entry '$missing' is not present in retrieval context.",
                ),
            )
        }

        val usedEntries = output.usedEntryIds.mapNotNull(retrievedEntries::get)
        val attemptedForbidden = output.attemptedUses
            .filter { use -> use in forbiddenMetricUses || usedEntries.any { entry -> use in entry.policy.forbiddenUse } }
        attemptedForbidden.forEach { use ->
            issues.add(
                KnowledgeUseValidationIssue(
                    code = "knowledge.forbidden_use",
                    message = "Knowledge entry was used for forbidden purpose '$use'.",
                ),
            )
        }

        if (output.createdNumericPeakMetric) {
            issues.add(
                KnowledgeUseValidationIssue(
                    code = "knowledge.created_numeric_peak_metric",
                    message = "Knowledge output attempted to create numeric chromatographic peak metrics.",
                ),
            )
        }
        if (output.overrodeCalibrationOrIntegration) {
            issues.add(
                KnowledgeUseValidationIssue(
                    code = "knowledge.overrode_calibration_or_integration",
                    message = "Knowledge output attempted to override deterministic calibration or integration.",
                ),
            )
        }
        if (output.unsupportedClaims.isNotEmpty()) {
            issues.add(
                KnowledgeUseValidationIssue(
                    code = "knowledge.unsupported_claims",
                    message = "Knowledge-grounded output contains unsupported claims: ${output.unsupportedClaims.joinToString()}.",
                ),
            )
        }

        val rejected = issues.any {
            it.code == "knowledge.forbidden_use" ||
                it.code == "knowledge.used_entry_id_not_retrieved" ||
                it.code == "knowledge.created_numeric_peak_metric" ||
                it.code == "knowledge.overrode_calibration_or_integration"
        }
        return KnowledgeUseValidationResult(
            verdict = when {
                rejected -> KnowledgeUseValidationVerdict.REJECTED
                issues.isNotEmpty() -> KnowledgeUseValidationVerdict.REVIEW
                else -> KnowledgeUseValidationVerdict.ACCEPTED
            },
            issues = issues,
        )
    }

    fun snippetsFor(
        context: KnowledgeRetrievalContext,
        pack: KnowledgePackVersion,
    ): List<KnowledgeGroundedSnippet> {
        val sourceRefs = pack.sourceRefs.associateBy { it.sourceId }
        return context.results.map { result ->
            KnowledgeGroundedSnippet(
                entryId = result.entry.entryId,
                version = result.entry.version,
                shortText = result.entry.shortText,
                allowedUse = result.entry.policy.allowedUse,
                forbiddenUse = result.entry.policy.forbiddenUse,
                sourceRefs = result.entry.sourceRefIds.mapNotNull(sourceRefs::get),
            )
        }
    }

    fun cardsFor(
        context: KnowledgeRetrievalContext,
        pack: KnowledgePackVersion,
    ): List<KnowledgeRetrievalCard> {
        val sourceRefs = pack.sourceRefs.associateBy { it.sourceId }
        return context.results.map { result ->
            KnowledgeRetrievalCard(
                entryId = result.entry.entryId,
                shortCard = result.entry.shortText.take(420),
                allowedUse = result.entry.policy.allowedUse,
                forbiddenUse = result.entry.policy.forbiddenUse,
                sourceRefs = result.entry.sourceRefIds.mapNotNull(sourceRefs::get),
                confidence = result.entry.confidence,
                trustTier = result.entry.trustTier,
            )
        }
    }
}

object KnowledgeUsePolicyValidator {
    fun validatePack(pack: KnowledgePackVersion): KnowledgePackValidationResult =
        KnowledgePackValidator.validatePack(pack)

    fun validateOutput(
        output: KnowledgeGroundedVlmOutput,
        retrievalContexts: List<KnowledgeRetrievalContext>,
    ): KnowledgeUseValidationResult =
        KnowledgePackValidator.validateOutput(output, retrievalContexts)

    fun snippetsFor(
        context: KnowledgeRetrievalContext,
        pack: KnowledgePackVersion,
    ): List<KnowledgeGroundedSnippet> =
        KnowledgePackValidator.snippetsFor(context, pack)

    fun cardsFor(
        context: KnowledgeRetrievalContext,
        pack: KnowledgePackVersion,
    ): List<KnowledgeRetrievalCard> =
        KnowledgePackValidator.cardsFor(context, pack)
}

object KnowledgeTextClassificationPolicy {
    private val ionRangePattern = Regex("""(?i)\b(?:ion|m/z|xic|eic|sim)\b.*\([^\)]*\bto\b[^\)]*\)""")
    private val ionOrMassContextPattern = Regex("""(?i)\b(?:ion|m/z|mass\s*range|xic|eic|sim)\b""")
    private val numericPattern = Regex("""^\s*[0-9]+(?:[.,][0-9]+)?\s*$""")

    fun classify(
        text: String,
        surroundingText: String = text,
        hasLocalSignalVerification: Boolean = false,
        hasLinkedTickPosition: Boolean = false,
    ): MultimodalTextRegionClass =
        when {
            ionRangePattern.containsMatchIn(surroundingText) -> MultimodalTextRegionClass.TITLE_OR_CHANNEL
            ionOrMassContextPattern.containsMatchIn(surroundingText) && numericPattern.matches(text) -> MultimodalTextRegionClass.TITLE_OR_CHANNEL
            hasLinkedTickPosition && numericPattern.matches(text) -> MultimodalTextRegionClass.TICK_LABEL
            hasLocalSignalVerification && numericPattern.matches(text) -> MultimodalTextRegionClass.PEAK_ANNOTATION
            surroundingText.contains("abundance", ignoreCase = true) ||
                surroundingText.contains("retention", ignoreCase = true) -> MultimodalTextRegionClass.AXIS_LABEL
            else -> MultimodalTextRegionClass.UNKNOWN_TEXT
        }

    fun canBecomePeakAnnotation(
        text: String,
        surroundingText: String = text,
        hasLocalSignalVerification: Boolean,
    ): Boolean =
        classify(
            text = text,
            surroundingText = surroundingText,
            hasLocalSignalVerification = hasLocalSignalVerification,
        ) == MultimodalTextRegionClass.PEAK_ANNOTATION
}

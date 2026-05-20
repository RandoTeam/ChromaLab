package com.chromalab.feature.knowledge

import com.chromalab.feature.processing.multimodal.MultimodalTextRegionClass
import kotlinx.serialization.Serializable

const val CHROMALAB_KNOWLEDGE_PACK_SCHEMA_VERSION = "chromalab-knowledge-pack-1.0"
const val CHROMALAB_KNOWLEDGE_PACK_ID = "chromalab-knowledge"
const val CHROMALAB_KNOWLEDGE_PACK_VERSION_V1 = "chromalab-knowledge-v1"

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
    val shortText: String,
    val aliases: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val sourceRefIds: List<String>,
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
}

@Serializable
data class KnowledgeSourceRef(
    val sourceId: String,
    val label: String,
    val url: String? = null,
    val citation: String? = null,
    val license: String? = null,
    val notes: String? = null,
)

@Serializable
data class KnowledgeUsePolicy(
    val allowedUse: List<String>,
    val forbiddenUse: List<String>,
)

@Serializable
data class KnowledgeSearchQuery(
    val rawQuery: String,
    val requestedTypes: List<KnowledgeEntryType> = emptyList(),
    val maxResults: Int = 8,
)

@Serializable
data class KnowledgeSearchResult(
    val entryId: String,
    val entry: KnowledgeEntry,
    val score: Double,
    val matchedTerms: List<String> = emptyList(),
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

object KnowledgeUsePolicyValidator {
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
        if (pack.packId.isBlank()) errors.add(KnowledgePackIssue("packId", "value must not be blank."))
        if (pack.version.isBlank()) errors.add(KnowledgePackIssue("version", "value must not be blank."))
        val sourceIds = pack.sourceRefs.map { it.sourceId }.toSet()
        if (sourceIds.size != pack.sourceRefs.size) errors.add(KnowledgePackIssue("sourceRefs", "source IDs must be unique."))
        val entryIds = pack.entries.map { it.entryId }
        if (entryIds.toSet().size != entryIds.size) errors.add(KnowledgePackIssue("entries", "entry IDs must be unique."))
        pack.sourceRefs.forEach { source ->
            if (source.sourceId.isBlank()) errors.add(KnowledgePackIssue("sourceRefs.sourceId", "value must not be blank."))
            if (source.label.isBlank()) errors.add(KnowledgePackIssue("sourceRefs[${source.sourceId}].label", "value must not be blank."))
        }
        pack.entries.forEach { entry ->
            if (entry.entryId.isBlank()) errors.add(KnowledgePackIssue("entry.entryId", "value must not be blank."))
            if (entry.shortText.isBlank()) errors.add(KnowledgePackIssue("entry[${entry.entryId}].shortText", "value must not be blank."))
            if (entry.sourceRefIds.isEmpty()) errors.add(KnowledgePackIssue("entry[${entry.entryId}].sourceRefIds", "at least one source reference is required."))
            entry.sourceRefIds.filterNot(sourceIds::contains).forEach { missing ->
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].sourceRefIds", "unknown source ref '$missing'."))
            }
            if (entry.policy.allowedUse.isEmpty()) {
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].allowedUse", "at least one allowed use is required."))
            }
            if (entry.policy.forbiddenUse.isEmpty()) {
                errors.add(KnowledgePackIssue("entry[${entry.entryId}].forbiddenUse", "at least one forbidden use is required."))
            }
        }
        return KnowledgePackValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = emptyList(),
        )
    }

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
}

object KnowledgeTextClassificationPolicy {
    private val ionRangePattern = Regex("""(?i)\b(?:ion|m/z|xic|eic|sim)\b.*\([^\)]*\bto\b[^\)]*\)""")
    private val numericPattern = Regex("""^\s*[0-9]+(?:[.,][0-9]+)?\s*$""")

    fun classify(
        text: String,
        surroundingText: String = text,
        hasLocalSignalVerification: Boolean = false,
        hasLinkedTickPosition: Boolean = false,
    ): MultimodalTextRegionClass =
        when {
            ionRangePattern.containsMatchIn(surroundingText) -> MultimodalTextRegionClass.TITLE_OR_CHANNEL
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

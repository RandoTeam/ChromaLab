package com.chromalab.feature.knowledge

import kotlinx.serialization.Serializable

const val CURRENT_LOCAL_KNOWLEDGE_PACK_SCHEMA = "1.0.0-phase-7.1"

@Serializable
data class LocalKnowledgePack(
    val id: String,
    val name: String,
    val schemaVersion: String = CURRENT_LOCAL_KNOWLEDGE_PACK_SCHEMA,
    val revision: String,
    val description: String? = null,
    val sources: List<KnowledgeSource> = emptyList(),
    val chromatogramTypes: List<ChromatogramTypeDefinition> = emptyList(),
    val ionFragments: List<IonFragmentDefinition> = emptyList(),
    val compoundClasses: List<CompoundClassDefinition> = emptyList(),
    val carbonNumberSeries: List<CarbonNumberSeriesDefinition> = emptyList(),
    val kovatsLibraries: List<KovatsReferenceLibrary> = emptyList(),
)

@Serializable
data class KnowledgeSource(
    val id: String,
    val label: String,
    val citation: String? = null,
    val sourceType: KnowledgeSourceType = KnowledgeSourceType.INTERNAL_CURATED,
    val licenseStatus: KnowledgeLicenseStatus = KnowledgeLicenseStatus.INTERNAL_CURATED,
    val trustTier: KnowledgeSourceTrustTier = KnowledgeSourceTrustTier.TIER_0_INTERNAL_CURATED,
    val canBundle: Boolean = sourceType == KnowledgeSourceType.INTERNAL_CURATED,
    val attributionRequired: Boolean = false,
    val notes: List<String> = emptyList(),
)

@Serializable
enum class KnowledgeSourceType {
    INTERNAL_CURATED,
    LITERATURE,
    INSTRUMENT_METHOD,
    USER_PROVIDED,
    UNKNOWN,
}

@Serializable
data class ChromatogramTypeDefinition(
    val id: String,
    val label: String,
    val analysisType: String,
    val chromatogramMode: String,
    val expectedXAxisUnit: String,
    val expectedYAxisUnit: String,
    val supportedIonFragmentIds: List<String> = emptyList(),
    val targetCompoundClassIds: List<String> = emptyList(),
    val interpretationNotes: List<KnowledgeStatement> = emptyList(),
    val sourceIds: List<String> = emptyList(),
)

@Serializable
data class IonFragmentDefinition(
    val id: String,
    val nominalMz: Double,
    val mzWindow: KnowledgeDoubleRange? = null,
    val label: String,
    val ionization: IonizationMode = IonizationMode.UNKNOWN,
    val diagnosticForCompoundClassIds: List<String> = emptyList(),
    val relatedIonFragmentIds: List<String> = emptyList(),
    val interpretation: List<KnowledgeStatement> = emptyList(),
    val cautions: List<KnowledgeStatement> = emptyList(),
    val sourceIds: List<String> = emptyList(),
)

@Serializable
enum class IonizationMode {
    ELECTRON_IONIZATION,
    CHEMICAL_IONIZATION,
    ESI_POSITIVE,
    ESI_NEGATIVE,
    UNKNOWN,
}

@Serializable
data class CompoundClassDefinition(
    val id: String,
    val label: String,
    val parentClassId: String? = null,
    val description: String? = null,
    val formulaPattern: String? = null,
    val carbonNumberRange: KnowledgeIntRange? = null,
    val diagnosticIonFragmentIds: List<String> = emptyList(),
    val homologousSeriesIds: List<String> = emptyList(),
    val interpretationNotes: List<KnowledgeStatement> = emptyList(),
    val assignmentCautions: List<KnowledgeStatement> = emptyList(),
    val sourceIds: List<String> = emptyList(),
)

@Serializable
data class CarbonNumberSeriesDefinition(
    val id: String,
    val label: String,
    val compoundClassId: String,
    val carbonNumberRange: KnowledgeIntRange,
    val retentionOrder: RetentionOrder = RetentionOrder.UNKNOWN,
    val memberNameTemplate: String? = null,
    val expectedIonFragmentIds: List<String> = emptyList(),
    val interpretationNotes: List<KnowledgeStatement> = emptyList(),
    val sourceIds: List<String> = emptyList(),
)

@Serializable
enum class RetentionOrder {
    INCREASES_WITH_CARBON_NUMBER,
    DECREASES_WITH_CARBON_NUMBER,
    METHOD_DEPENDENT,
    UNKNOWN,
}

@Serializable
data class KovatsReferenceLibrary(
    val id: String,
    val label: String,
    val chromatogramTypeId: String? = null,
    val stationaryPhase: String? = null,
    val temperatureProgram: String? = null,
    val referenceSeriesId: String? = null,
    val entries: List<KovatsReferenceEntry> = emptyList(),
    val sourceIds: List<String> = emptyList(),
)

@Serializable
data class KovatsReferenceEntry(
    val compoundName: String,
    val compoundClassId: String? = null,
    val formula: String? = null,
    val carbonNumber: Int? = null,
    val kovatsIndex: Double? = null,
    val kovatsRange: KnowledgeDoubleRange? = null,
    val evidence: KnowledgeEvidence = KnowledgeEvidence.CURATED,
    val sourceIds: List<String> = emptyList(),
    val claimScopes: List<EvidenceClaimScope> = listOf(
        EvidenceClaimScope.RETRIEVAL_CONTEXT,
        EvidenceClaimScope.NOT_MEASUREMENT,
    ),
)

@Serializable
data class KnowledgeStatement(
    val text: String,
    val evidence: KnowledgeEvidence = KnowledgeEvidence.CURATED,
    val confidence: Double? = null,
    val sourceIds: List<String> = emptyList(),
    val claimScopes: List<EvidenceClaimScope> = listOf(
        EvidenceClaimScope.EXPLANATION_ONLY,
        EvidenceClaimScope.NOT_MEASUREMENT,
    ),
)

@Serializable
enum class KnowledgeEvidence {
    CURATED,
    LITERATURE,
    INFERRED_RULE,
    USER_PROVIDED,
    MODEL_SUGGESTED,
    UNKNOWN,
}

@Serializable
data class KnowledgeIntRange(
    val minimum: Int,
    val maximum: Int,
) {
    fun isValid(): Boolean = minimum <= maximum
}

@Serializable
data class KnowledgeDoubleRange(
    val minimum: Double,
    val maximum: Double,
    val unit: String? = null,
) {
    fun isValid(): Boolean = minimum <= maximum
}

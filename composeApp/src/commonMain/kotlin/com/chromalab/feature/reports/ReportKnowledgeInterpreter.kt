package com.chromalab.feature.reports

import com.chromalab.feature.knowledge.ChromatogramTypeDefinition
import com.chromalab.feature.knowledge.CompoundClassDefinition
import com.chromalab.feature.knowledge.EvidenceClaimScope
import com.chromalab.feature.knowledge.IonFragmentDefinition
import com.chromalab.feature.knowledge.KnowledgeEntryType
import com.chromalab.feature.knowledge.KnowledgeSource
import com.chromalab.feature.knowledge.KnowledgeSourceRef
import com.chromalab.feature.knowledge.KnowledgeSourceTrustTier
import com.chromalab.feature.knowledge.KnowledgeStatement
import com.chromalab.feature.knowledge.LocalKnowledgePack

data class ReportKnowledgeContext(
    val chromatogramType: ChromatogramTypeDefinition? = null,
    val ionFragment: IonFragmentDefinition? = null,
    val compoundClasses: List<CompoundClassDefinition> = emptyList(),
    val domainContextNotes: List<String> = emptyList(),
    val assignmentCautions: List<String> = emptyList(),
    val likelyCompoundClass: ReportTextValue = ReportTextValue.notCalculated(),
    val knowledgePackVersion: String? = null,
    val citations: List<ReportKnowledgeCitation> = emptyList(),
) {
    val hasEvidence: Boolean =
        chromatogramType != null || ionFragment != null || compoundClasses.isNotEmpty()
}

object ReportKnowledgeInterpreter {

    private val explicitMzPattern = Regex("""(?i)(?:m/z|ion|xic)\s*[\(:=]?\s*([0-9]+(?:[.,][0-9]+)?)""")
    private val numericPattern = Regex("""[0-9]+(?:[.,][0-9]+)?""")

    fun interpret(
        identification: ChromatogramIdentification,
        pack: LocalKnowledgePack?,
    ): ReportKnowledgeContext {
        if (pack == null) return ReportKnowledgeContext()

        val chromatogramType = pack.findChromatogramType(identification)
        val ionFragment = pack.findIonFragment(identification)
        val compoundClasses = pack.findCompoundClasses(chromatogramType, ionFragment)

        val notes = buildList {
            chromatogramType?.let { type ->
                add("Matched local chromatogram type: ${type.label} (${type.id}).")
                addAll(type.interpretationNotes.renderStatements("Type"))
            }
            ionFragment?.let { ion ->
                add("Matched local ion/channel: ${ion.label} (${ion.id}).")
                addAll(ion.interpretation.renderStatements("Ion"))
            }
            compoundClasses.forEach { compoundClass ->
                add("Candidate class from local knowledge: ${compoundClass.label} (${compoundClass.id}).")
                addAll(compoundClass.interpretationNotes.renderStatements("Class"))
            }
        }.distinct()

        val cautions = buildList {
            ionFragment?.let { addAll(it.cautions.renderStatements("Ion caution")) }
            compoundClasses.forEach { compoundClass ->
                addAll(compoundClass.assignmentCautions.renderStatements("Assignment caution"))
            }
            if (ionFragment != null && compoundClasses.isNotEmpty()) {
                add("Local knowledge matched channel/class evidence only; peak-level compound names remain hypotheses until retention-index, spectrum/library, method-target, or user evidence is attached.")
            }
        }.distinct()

        return ReportKnowledgeContext(
            chromatogramType = chromatogramType,
            ionFragment = ionFragment,
            compoundClasses = compoundClasses,
            domainContextNotes = notes,
            assignmentCautions = cautions,
            likelyCompoundClass = compoundClasses.firstOrNull()?.let { compoundClass ->
                ReportTextValue(
                    value = compoundClass.label,
                    status = ReportValueStatus.INFERRED,
                    confidence = compoundClass.knowledgeConfidence(ionFragment),
                    source = ReportValueSource.LOCAL_KNOWLEDGE,
                )
            } ?: ReportTextValue.notCalculated(),
            knowledgePackVersion = pack.revision,
            citations = buildCitations(
                pack = pack,
                chromatogramType = chromatogramType,
                ionFragment = ionFragment,
                compoundClasses = compoundClasses,
            ),
        )
    }

    private fun buildCitations(
        pack: LocalKnowledgePack,
        chromatogramType: ChromatogramTypeDefinition?,
        ionFragment: IonFragmentDefinition?,
        compoundClasses: List<CompoundClassDefinition>,
    ): List<ReportKnowledgeCitation> =
        buildList {
            chromatogramType?.let { type ->
                val record = type.toCitationRecord(pack)
                add(
                    ReportKnowledgeCitation(
                        citationId = "knowledge:${type.id}",
                        knowledgePackVersion = pack.revision,
                        usedEntryIds = listOf(type.id),
                        usedEntryRecords = listOf(record),
                        explanationTarget = ReportKnowledgeExplanationTarget.REPORT_SUMMARY,
                        generatedBy = ReportKnowledgeGeneratedBy.KNOWLEDGE_PACK,
                        explanation = "Matched local chromatogram type '${type.label}' for report context.",
                    ),
                )
            }
            ionFragment?.let { ion ->
                val record = ion.toCitationRecord(pack)
                add(
                    ReportKnowledgeCitation(
                        citationId = "knowledge:${ion.id}",
                        knowledgePackVersion = pack.revision,
                        usedEntryIds = listOf(ion.id),
                        usedEntryRecords = listOf(record),
                        explanationTarget = ReportKnowledgeExplanationTarget.AXIS_LABEL_EXPLANATION,
                        generatedBy = ReportKnowledgeGeneratedBy.KNOWLEDGE_PACK,
                        explanation = "Matched local ion/channel '${ion.label}' for text classification and warning context.",
                    ),
                )
            }
            compoundClasses.forEach { compoundClass ->
                val record = compoundClass.toCitationRecord(pack)
                add(
                    ReportKnowledgeCitation(
                        citationId = "knowledge:${compoundClass.id}",
                        knowledgePackVersion = pack.revision,
                        usedEntryIds = listOf(compoundClass.id),
                        usedEntryRecords = listOf(record),
                        explanationTarget = ReportKnowledgeExplanationTarget.COMPOUND_HYPOTHESIS,
                        generatedBy = ReportKnowledgeGeneratedBy.KNOWLEDGE_PACK,
                        explanation = "Matched local compound-class context '${compoundClass.label}'. This is not a compound identification.",
                    ),
                )
            }
        }.distinctBy { it.citationId }

    private fun LocalKnowledgePack.findChromatogramType(
        identification: ChromatogramIdentification,
    ): ChromatogramTypeDefinition? {
        val analysis = identification.analysisType.value.orEmpty().uppercase()
        val mode = identification.chromatogramMode.value.orEmpty().uppercase()
        val normalizedMode = mode.normalizedChromatogramMode()

        return chromatogramTypes.firstOrNull { type ->
            val analysisMatches = analysis.isBlank() || analysis.contains(type.analysisType.uppercase())
            val modeMatches = normalizedMode == type.chromatogramMode.uppercase() ||
                mode.contains(type.chromatogramMode.uppercase()) ||
                mode.contains(type.label.uppercase())
            analysisMatches && modeMatches
        }
    }

    private fun LocalKnowledgePack.findIonFragment(
        identification: ChromatogramIdentification,
    ): IonFragmentDefinition? {
        val mz = identification.extractMzValue() ?: return null
        return ionFragments
            .filter { ion ->
                ion.mzWindow?.let { mz in it.minimum..it.maximum }
                    ?: (kotlin.math.abs(ion.nominalMz - mz) <= 0.5)
            }
            .minByOrNull { kotlin.math.abs(it.nominalMz - mz) }
    }

    private fun LocalKnowledgePack.findCompoundClasses(
        chromatogramType: ChromatogramTypeDefinition?,
        ionFragment: IonFragmentDefinition?,
    ): List<CompoundClassDefinition> {
        val ids = buildList {
            addAll(chromatogramType?.targetCompoundClassIds.orEmpty())
            addAll(ionFragment?.diagnosticForCompoundClassIds.orEmpty())
        }.distinct()

        return ids.mapNotNull { id -> compoundClasses.firstOrNull { it.id == id } }
    }

    private fun ChromatogramIdentification.extractMzValue(): Double? {
        listOfNotNull(ionOrChannel.value, chromatogramTitle.value).forEach { text ->
            val explicit = explicitMzPattern.find(text)?.groupValues?.getOrNull(1)?.parseDecimal()
            if (explicit != null) return explicit
        }

        ionRange.value?.let { rangeText ->
            val rangeValues = numericPattern.findAll(rangeText)
                .mapNotNull { it.value.parseDecimal() }
                .take(2)
                .toList()
            if (rangeValues.size == 2) {
                return (rangeValues[0] + rangeValues[1]) / 2.0
            }
            rangeValues.firstOrNull()?.let { return it }
        }

        val text = listOfNotNull(ionOrChannel.value, ionRange.value, chromatogramTitle.value).joinToString(" ")
        val explicit = explicitMzPattern.find(text)?.groupValues?.getOrNull(1)?.parseDecimal()
        if (explicit != null) return explicit

        return numericPattern.find(text)
            ?.value
            ?.parseDecimal()
    }

    private fun String.parseDecimal(): Double? =
        replace(',', '.').toDoubleOrNull()

    private fun String.normalizedChromatogramMode(): String =
        when {
            contains("TIC") || contains("TOTAL ION") -> "TIC"
            contains("SIM") || contains("SELECTED ION") -> "SIM"
            contains("XIC") || contains("EXACT") -> "XIC"
            contains("EIC") || contains("EXTRACTED ION") -> "EIC"
            else -> this
        }

    private fun List<KnowledgeStatement>.renderStatements(prefix: String): List<String> =
        map { statement ->
            val sources = statement.sourceIds.takeIf { it.isNotEmpty() }?.joinToString(", ")
                ?.let { " [$it]" }
                .orEmpty()
            "$prefix$sources: ${statement.text}"
        }

    private fun CompoundClassDefinition.knowledgeConfidence(ionFragment: IonFragmentDefinition?): Double =
        when {
            ionFragment != null && id in ionFragment.diagnosticForCompoundClassIds -> 0.65
            interpretationNotes.any { it.confidence != null } -> interpretationNotes.mapNotNull { it.confidence }.average()
            else -> 0.50
        }.coerceIn(0.0, 1.0)

    private fun ChromatogramTypeDefinition.toCitationRecord(pack: LocalKnowledgePack): ReportKnowledgeEntryRecord =
        ReportKnowledgeEntryRecord(
            entryId = id,
            entryType = KnowledgeEntryType.CHROMATOGRAPHY_METHOD_TERM,
            claimScope = statementScopes(interpretationNotes),
            allowedUse = defaultAllowedUse("chromatogram type context and report caveats"),
            forbiddenUse = defaultForbiddenUse(),
            sourceRefs = sourceRefs(sourceIds, pack),
            trustTier = trustTier(sourceIds, pack),
        )

    private fun IonFragmentDefinition.toCitationRecord(pack: LocalKnowledgePack): ReportKnowledgeEntryRecord =
        ReportKnowledgeEntryRecord(
            entryId = id,
            entryType = KnowledgeEntryType.ION_CHANNEL_TERM,
            claimScope = statementScopes(interpretation + cautions),
            allowedUse = defaultAllowedUse("ion/channel text classification"),
            forbiddenUse = defaultForbiddenUse(),
            sourceRefs = sourceRefs(sourceIds, pack),
            trustTier = trustTier(sourceIds, pack),
        )

    private fun CompoundClassDefinition.toCitationRecord(pack: LocalKnowledgePack): ReportKnowledgeEntryRecord =
        ReportKnowledgeEntryRecord(
            entryId = id,
            entryType = KnowledgeEntryType.COMPOUND_CLASS,
            claimScope = statementScopes(interpretationNotes + assignmentCautions),
            allowedUse = defaultAllowedUse("compound-class explanation and hypothesis labeling"),
            forbiddenUse = defaultForbiddenUse() + "identify a peak compound without explicit spectral, retention-index, method-target, library, or user evidence",
            sourceRefs = sourceRefs(sourceIds, pack),
            trustTier = trustTier(sourceIds, pack),
        )

    private fun statementScopes(statements: List<KnowledgeStatement>): List<EvidenceClaimScope> =
        statements
            .flatMap { it.claimScopes }
            .ifEmpty {
                listOf(
                    EvidenceClaimScope.EXPLANATION_ONLY,
                    EvidenceClaimScope.RETRIEVAL_CONTEXT,
                    EvidenceClaimScope.NOT_MEASUREMENT,
                )
            }
            .distinct()

    private fun defaultAllowedUse(purpose: String): List<String> =
        listOf(
            purpose,
            "ground report caveats",
            "support semantic interpretation only",
        )

    private fun defaultForbiddenUse(): List<String> =
        listOf(
            "create measured RT",
            "create peak height",
            "create peak area",
            "create FWHM",
            "create S/N",
            "create baseline",
            "create Kovats/retention index",
            "override calibration or deterministic integration",
        )

    private fun sourceRefs(sourceIds: List<String>, pack: LocalKnowledgePack): List<KnowledgeSourceRef> {
        val sourcesById = pack.sources.associateBy { it.id }
        return sourceIds.mapNotNull { sourceId -> sourcesById[sourceId]?.toRef() }
            .ifEmpty {
                listOf(
                    KnowledgeSourceRef(
                        sourceId = "chromalab_internal_curated",
                        label = "ChromaLab internal curated report knowledge",
                        licenseStatus = com.chromalab.feature.knowledge.KnowledgeLicenseStatus.INTERNAL_CURATED,
                        trustTier = KnowledgeSourceTrustTier.TIER_0_INTERNAL_CURATED,
                        canBundle = true,
                        canTransform = true,
                    ),
                )
            }
    }

    private fun KnowledgeSource.toRef(): KnowledgeSourceRef =
        KnowledgeSourceRef(
            sourceId = id,
            label = label,
            citation = citation,
            licenseStatus = licenseStatus,
            trustTier = trustTier,
            attributionRequired = attributionRequired,
            canBundle = canBundle,
            canTransform = canBundle,
            notes = notes.joinToString("; ").ifBlank { null },
        )

    private fun trustTier(sourceIds: List<String>, pack: LocalKnowledgePack): KnowledgeSourceTrustTier =
        sourceIds.mapNotNull { id -> pack.sources.firstOrNull { it.id == id }?.trustTier }
            .minByOrNull { it.ordinal }
            ?: KnowledgeSourceTrustTier.TIER_0_INTERNAL_CURATED
}

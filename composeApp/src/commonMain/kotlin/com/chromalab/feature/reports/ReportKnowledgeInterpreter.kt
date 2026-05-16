package com.chromalab.feature.reports

import com.chromalab.feature.knowledge.ChromatogramTypeDefinition
import com.chromalab.feature.knowledge.CompoundClassDefinition
import com.chromalab.feature.knowledge.IonFragmentDefinition
import com.chromalab.feature.knowledge.KnowledgeStatement
import com.chromalab.feature.knowledge.LocalKnowledgePack

data class ReportKnowledgeContext(
    val chromatogramType: ChromatogramTypeDefinition? = null,
    val ionFragment: IonFragmentDefinition? = null,
    val compoundClasses: List<CompoundClassDefinition> = emptyList(),
    val domainContextNotes: List<String> = emptyList(),
    val assignmentCautions: List<String> = emptyList(),
    val likelyCompoundClass: ReportTextValue = ReportTextValue.notCalculated(),
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
        )
    }

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
}

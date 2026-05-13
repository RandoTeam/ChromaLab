package com.chromalab.feature.knowledge

object LocalKnowledgePackValidator {

    fun validate(pack: LocalKnowledgePack): KnowledgePackValidationResult {
        val errors = mutableListOf<KnowledgePackIssue>()
        val warnings = mutableListOf<KnowledgePackIssue>()

        requireNonBlank("pack.id", pack.id, errors)
        requireNonBlank("pack.name", pack.name, errors)
        requireNonBlank("pack.revision", pack.revision, errors)

        validateUniqueIds("sources", pack.sources.map { it.id }, errors)
        validateUniqueIds("chromatogramTypes", pack.chromatogramTypes.map { it.id }, errors)
        validateUniqueIds("ionFragments", pack.ionFragments.map { it.id }, errors)
        validateUniqueIds("compoundClasses", pack.compoundClasses.map { it.id }, errors)
        validateUniqueIds("carbonNumberSeries", pack.carbonNumberSeries.map { it.id }, errors)
        validateUniqueIds("kovatsLibraries", pack.kovatsLibraries.map { it.id }, errors)

        val sourceIds = pack.sources.map { it.id }.toSet()
        val chromatogramTypeIds = pack.chromatogramTypes.map { it.id }.toSet()
        val ionFragmentIds = pack.ionFragments.map { it.id }.toSet()
        val compoundClassIds = pack.compoundClasses.map { it.id }.toSet()
        val seriesIds = pack.carbonNumberSeries.map { it.id }.toSet()

        pack.sources.forEach { source ->
            requireNonBlank("source.id", source.id, errors)
            requireNonBlank("source[${source.id}].label", source.label, errors)
        }

        pack.chromatogramTypes.forEach { type ->
            requireNonBlank("chromatogramType.id", type.id, errors)
            requireNonBlank("chromatogramType[${type.id}].label", type.label, errors)
            requireNonBlank("chromatogramType[${type.id}].analysisType", type.analysisType, errors)
            requireNonBlank("chromatogramType[${type.id}].chromatogramMode", type.chromatogramMode, errors)
            requireNonBlank("chromatogramType[${type.id}].expectedXAxisUnit", type.expectedXAxisUnit, errors)
            requireNonBlank("chromatogramType[${type.id}].expectedYAxisUnit", type.expectedYAxisUnit, errors)
            validateKnownIds(type.sourceIds, sourceIds, "chromatogramType[${type.id}].sourceIds", errors)
            validateKnownIds(type.supportedIonFragmentIds, ionFragmentIds, "chromatogramType[${type.id}].supportedIonFragmentIds", errors)
            validateKnownIds(type.targetCompoundClassIds, compoundClassIds, "chromatogramType[${type.id}].targetCompoundClassIds", errors)
            validateStatements(type.interpretationNotes, sourceIds, "chromatogramType[${type.id}].interpretationNotes", errors)
        }

        pack.ionFragments.forEach { ion ->
            requireNonBlank("ionFragment.id", ion.id, errors)
            requireNonBlank("ionFragment[${ion.id}].label", ion.label, errors)
            if (ion.nominalMz <= 0.0) {
                errors.add(KnowledgePackIssue("ionFragment[${ion.id}].nominalMz", "nominal m/z must be positive."))
            }
            if (ion.mzWindow?.isValid() == false) {
                errors.add(KnowledgePackIssue("ionFragment[${ion.id}].mzWindow", "m/z window minimum must be <= maximum."))
            }
            validateKnownIds(ion.sourceIds, sourceIds, "ionFragment[${ion.id}].sourceIds", errors)
            validateKnownIds(ion.diagnosticForCompoundClassIds, compoundClassIds, "ionFragment[${ion.id}].diagnosticForCompoundClassIds", errors)
            validateKnownIds(ion.relatedIonFragmentIds, ionFragmentIds, "ionFragment[${ion.id}].relatedIonFragmentIds", errors)
            validateStatements(ion.interpretation, sourceIds, "ionFragment[${ion.id}].interpretation", errors)
            validateStatements(ion.cautions, sourceIds, "ionFragment[${ion.id}].cautions", errors)
        }

        pack.compoundClasses.forEach { compoundClass ->
            requireNonBlank("compoundClass.id", compoundClass.id, errors)
            requireNonBlank("compoundClass[${compoundClass.id}].label", compoundClass.label, errors)
            compoundClass.parentClassId?.let {
                validateKnownIds(listOf(it), compoundClassIds, "compoundClass[${compoundClass.id}].parentClassId", errors)
            }
            if (compoundClass.carbonNumberRange?.isValid() == false) {
                errors.add(KnowledgePackIssue("compoundClass[${compoundClass.id}].carbonNumberRange", "carbon range minimum must be <= maximum."))
            }
            validateKnownIds(compoundClass.sourceIds, sourceIds, "compoundClass[${compoundClass.id}].sourceIds", errors)
            validateKnownIds(compoundClass.diagnosticIonFragmentIds, ionFragmentIds, "compoundClass[${compoundClass.id}].diagnosticIonFragmentIds", errors)
            validateKnownIds(compoundClass.homologousSeriesIds, seriesIds, "compoundClass[${compoundClass.id}].homologousSeriesIds", errors)
            validateStatements(compoundClass.interpretationNotes, sourceIds, "compoundClass[${compoundClass.id}].interpretationNotes", errors)
            validateStatements(compoundClass.assignmentCautions, sourceIds, "compoundClass[${compoundClass.id}].assignmentCautions", errors)
        }

        pack.carbonNumberSeries.forEach { series ->
            requireNonBlank("carbonNumberSeries.id", series.id, errors)
            requireNonBlank("carbonNumberSeries[${series.id}].label", series.label, errors)
            if (!series.carbonNumberRange.isValid()) {
                errors.add(KnowledgePackIssue("carbonNumberSeries[${series.id}].carbonNumberRange", "carbon range minimum must be <= maximum."))
            }
            validateKnownIds(listOf(series.compoundClassId), compoundClassIds, "carbonNumberSeries[${series.id}].compoundClassId", errors)
            validateKnownIds(series.expectedIonFragmentIds, ionFragmentIds, "carbonNumberSeries[${series.id}].expectedIonFragmentIds", errors)
            validateKnownIds(series.sourceIds, sourceIds, "carbonNumberSeries[${series.id}].sourceIds", errors)
            validateStatements(series.interpretationNotes, sourceIds, "carbonNumberSeries[${series.id}].interpretationNotes", errors)
        }

        pack.kovatsLibraries.forEach { library ->
            requireNonBlank("kovatsLibrary.id", library.id, errors)
            requireNonBlank("kovatsLibrary[${library.id}].label", library.label, errors)
            library.chromatogramTypeId?.let {
                validateKnownIds(listOf(it), chromatogramTypeIds, "kovatsLibrary[${library.id}].chromatogramTypeId", errors)
            }
            library.referenceSeriesId?.let {
                validateKnownIds(listOf(it), seriesIds, "kovatsLibrary[${library.id}].referenceSeriesId", errors)
            }
            validateKnownIds(library.sourceIds, sourceIds, "kovatsLibrary[${library.id}].sourceIds", errors)
            if (library.entries.isEmpty()) {
                warnings.add(KnowledgePackIssue("kovatsLibrary[${library.id}].entries", "Kovats library has no entries yet."))
            }
            library.entries.forEachIndexed { index, entry ->
                val path = "kovatsLibrary[${library.id}].entries[$index]"
                requireNonBlank("$path.compoundName", entry.compoundName, errors)
                entry.compoundClassId?.let {
                    validateKnownIds(listOf(it), compoundClassIds, "$path.compoundClassId", errors)
                }
                if (entry.carbonNumber != null && entry.carbonNumber <= 0) {
                    errors.add(KnowledgePackIssue("$path.carbonNumber", "carbon number must be positive."))
                }
                if (entry.kovatsIndex == null && entry.kovatsRange == null) {
                    errors.add(KnowledgePackIssue(path, "Kovats entry must provide either kovatsIndex or kovatsRange."))
                }
                if (entry.kovatsRange?.isValid() == false) {
                    errors.add(KnowledgePackIssue("$path.kovatsRange", "Kovats range minimum must be <= maximum."))
                }
                validateKnownIds(entry.sourceIds, sourceIds, "$path.sourceIds", errors)
            }
        }

        return KnowledgePackValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
        )
    }

    private fun requireNonBlank(path: String, value: String, errors: MutableList<KnowledgePackIssue>) {
        if (value.isBlank()) {
            errors.add(KnowledgePackIssue(path, "value must not be blank."))
        }
    }

    private fun validateUniqueIds(
        collectionName: String,
        ids: List<String>,
        errors: MutableList<KnowledgePackIssue>,
    ) {
        ids.filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .filterValues { count -> count > 1 }
            .keys
            .forEach { duplicateId ->
                errors.add(KnowledgePackIssue(collectionName, "duplicate id '$duplicateId'."))
            }
    }

    private fun validateKnownIds(
        ids: List<String>,
        knownIds: Set<String>,
        path: String,
        errors: MutableList<KnowledgePackIssue>,
    ) {
        ids.forEach { id ->
            if (id.isBlank()) {
                errors.add(KnowledgePackIssue(path, "referenced id must not be blank."))
            } else if (id !in knownIds) {
                errors.add(KnowledgePackIssue(path, "unknown referenced id '$id'."))
            }
        }
    }

    private fun validateStatements(
        statements: List<KnowledgeStatement>,
        sourceIds: Set<String>,
        path: String,
        errors: MutableList<KnowledgePackIssue>,
    ) {
        statements.forEachIndexed { index, statement ->
            val statementPath = "$path[$index]"
            requireNonBlank("$statementPath.text", statement.text, errors)
            statement.confidence?.let { confidence ->
                if (confidence !in 0.0..1.0) {
                    errors.add(KnowledgePackIssue("$statementPath.confidence", "confidence must be between 0 and 1."))
                }
            }
            validateKnownIds(statement.sourceIds, sourceIds, "$statementPath.sourceIds", errors)
        }
    }
}

data class KnowledgePackValidationResult(
    val isValid: Boolean,
    val errors: List<KnowledgePackIssue>,
    val warnings: List<KnowledgePackIssue>,
)

data class KnowledgePackIssue(
    val path: String,
    val message: String,
)

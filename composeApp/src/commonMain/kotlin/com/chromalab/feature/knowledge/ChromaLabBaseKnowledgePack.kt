package com.chromalab.feature.knowledge

object ChromaLabBaseKnowledgePack {

    val pack: LocalKnowledgePack
        get() = buildPack()

    private fun buildPack(): LocalKnowledgePack = LocalKnowledgePack(
        id = "chromalab-base",
        name = "ChromaLab Base Knowledge Pack",
        revision = "2026-05-13-phase-7.3",
        description = "Conservative offline reference facts for GC-MS chromatogram interpretation.",
        sources = listOf(
            KnowledgeSource(
                id = "nist-webbook-srd69",
                label = "NIST Chemistry WebBook, SRD 69",
                citation = "https://webbook.nist.gov/chemistry/",
                sourceType = KnowledgeSourceType.LITERATURE,
                notes = listOf("Used as the curated source family for EI spectrum availability, formulas, molecular weights, and non-polar RI records."),
            ),
            KnowledgeSource(
                id = "nist-gc-retention-data",
                label = "NIST Chemistry WebBook: Gas Chromatographic Retention Data",
                citation = "https://webbook.nist.gov/chemistry/gc-ri/",
                sourceType = KnowledgeSourceType.LITERATURE,
                notes = listOf("Defines retention-index data types and the bracketing n-alkane reference-series formulas used for isothermal Kovats and temperature-programmed Van den Dool/Kratz RI calculations."),
            ),
            KnowledgeSource(
                id = "nist-toluene",
                label = "NIST Chemistry WebBook: Toluene",
                citation = "https://webbook.nist.gov/cgi/cbook.cgi?ID=C108883",
                sourceType = KnowledgeSourceType.LITERATURE,
                notes = listOf("Formula C7H8, molecular weight 92.1384, EI mass spectrum available, non-polar RI records available."),
            ),
            KnowledgeSource(
                id = "nist-ethylbenzene",
                label = "NIST Chemistry WebBook: Ethylbenzene",
                citation = "https://webbook.nist.gov/cgi/cbook.cgi?ID=C100414",
                sourceType = KnowledgeSourceType.LITERATURE,
                notes = listOf("Formula C8H10, molecular weight 106.1650, EI mass spectrum available, non-polar RI records available."),
            ),
            KnowledgeSource(
                id = "nist-m-xylene",
                label = "NIST Chemistry WebBook: m-Xylene",
                citation = "https://webbook.nist.gov/cgi/cbook.cgi?ID=C108383",
                sourceType = KnowledgeSourceType.LITERATURE,
                notes = listOf("Formula C8H10, molecular weight 106.1650, EI mass spectrum available, non-polar RI records available."),
            ),
            KnowledgeSource(
                id = "nist-p-xylene",
                label = "NIST Chemistry WebBook: p-Xylene",
                citation = "https://webbook.nist.gov/cgi/cbook.cgi?ID=C106423",
                sourceType = KnowledgeSourceType.LITERATURE,
                notes = listOf("Formula C8H10, molecular weight 106.1650, EI mass spectrum available, non-polar RI records available."),
            ),
            KnowledgeSource(
                id = "nist-o-xylene",
                label = "NIST Chemistry WebBook: o-Xylene",
                citation = "https://webbook.nist.gov/cgi/cbook.cgi?ID=C95476",
                sourceType = KnowledgeSourceType.LITERATURE,
                notes = listOf("Formula C8H10, molecular weight 106.1650, EI mass spectrum available, non-polar RI records available."),
            ),
        ),
        chromatogramTypes = listOf(gcMsEiEic),
        ionFragments = listOf(eiMz92, eiMz91),
        compoundClasses = listOf(monoAlkylbenzenes, nParaffins),
        carbonNumberSeries = listOf(alkylbenzeneSeries, nParaffinReferenceSeries),
        kovatsLibraries = listOf(nonPolarAlkylbenzeneSeedRi, nParaffinReferenceRiScale),
    )

    private val gcMsEiEic = ChromatogramTypeDefinition(
        id = "gc-ms-ei-eic",
        label = "GC-MS EI extracted ion chromatogram",
        analysisType = "GC-MS",
        chromatogramMode = "EIC",
        expectedXAxisUnit = "min",
        expectedYAxisUnit = "counts",
        supportedIonFragmentIds = listOf("ei-mz-92", "ei-mz-91"),
        targetCompoundClassIds = listOf("monoalkylbenzenes"),
        interpretationNotes = listOf(
            KnowledgeStatement(
                text = "An extracted ion chromatogram is channel evidence only; compound names require retention behavior, spectrum context, and confidence flags.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("nist-webbook-srd69"),
            ),
        ),
        sourceIds = listOf("nist-webbook-srd69"),
    )

    private val eiMz92 = IonFragmentDefinition(
        id = "ei-mz-92",
        nominalMz = 92.0,
        mzWindow = KnowledgeDoubleRange(91.70, 92.70, "m/z"),
        label = "m/z 92 EI channel",
        ionization = IonizationMode.ELECTRON_IONIZATION,
        diagnosticForCompoundClassIds = listOf("monoalkylbenzenes"),
        relatedIonFragmentIds = listOf("ei-mz-91"),
        interpretation = listOf(
            KnowledgeStatement(
                text = "The m/z 92 channel is directly compatible with the molecular ion of toluene (C7H8, molecular weight 92.1384).",
                evidence = KnowledgeEvidence.LITERATURE,
                confidence = 0.9,
                sourceIds = listOf("nist-toluene"),
            ),
            KnowledgeStatement(
                text = "In alkylbenzene-oriented GC-MS work, m/z 92 is aromatic-channel evidence but is not a standalone compound assignment.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 0.8,
                sourceIds = listOf("nist-webbook-srd69"),
            ),
        ),
        cautions = listOf(
            KnowledgeStatement(
                text = "Do not identify a peak as toluene or an alkylbenzene from m/z 92 alone; require retention index, adjacent ions, full spectrum, or user/library confirmation.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("nist-webbook-srd69", "nist-toluene"),
            ),
        ),
        sourceIds = listOf("nist-toluene"),
    )

    private val eiMz91 = IonFragmentDefinition(
        id = "ei-mz-91",
        nominalMz = 91.0,
        mzWindow = KnowledgeDoubleRange(90.70, 91.70, "m/z"),
        label = "m/z 91 related alkylbenzene channel",
        ionization = IonizationMode.ELECTRON_IONIZATION,
        diagnosticForCompoundClassIds = listOf("monoalkylbenzenes"),
        relatedIonFragmentIds = listOf("ei-mz-92"),
        interpretation = listOf(
            KnowledgeStatement(
                text = "m/z 91 is a related EI channel commonly checked with alkylbenzene candidates, but it still requires spectrum and retention confirmation.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 0.75,
                sourceIds = listOf("nist-webbook-srd69"),
            ),
        ),
        cautions = listOf(
            KnowledgeStatement(
                text = "Treat m/z 91 as supporting evidence only; it is not specific enough for release-quality compound naming by itself.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("nist-webbook-srd69"),
            ),
        ),
        sourceIds = listOf("nist-webbook-srd69"),
    )

    private val monoAlkylbenzenes = CompoundClassDefinition(
        id = "monoalkylbenzenes",
        label = "Monocyclic alkylbenzenes",
        description = "Benzene ring with alkyl substitution; this pack uses it as a conservative aromatic hydrocarbon candidate class.",
        formulaPattern = "CnH2n-6",
        carbonNumberRange = KnowledgeIntRange(7, 30),
        diagnosticIonFragmentIds = listOf("ei-mz-91", "ei-mz-92"),
        homologousSeriesIds = listOf("monoalkylbenzene-carbon-series"),
        interpretationNotes = listOf(
            KnowledgeStatement(
                text = "For homologous alkylbenzene candidates, retention generally increases with carbon number on non-polar GC phases.",
                evidence = KnowledgeEvidence.INFERRED_RULE,
                confidence = 0.75,
                sourceIds = listOf("nist-ethylbenzene", "nist-m-xylene", "nist-p-xylene", "nist-o-xylene"),
            ),
        ),
        assignmentCautions = listOf(
            KnowledgeStatement(
                text = "Xylene and ethylbenzene isomers share formula C8H10; retention index and full-spectrum evidence are needed to separate candidates.",
                evidence = KnowledgeEvidence.LITERATURE,
                confidence = 0.9,
                sourceIds = listOf("nist-ethylbenzene", "nist-m-xylene", "nist-p-xylene", "nist-o-xylene"),
            ),
        ),
        sourceIds = listOf("nist-webbook-srd69"),
    )

    private val alkylbenzeneSeries = CarbonNumberSeriesDefinition(
        id = "monoalkylbenzene-carbon-series",
        label = "Monocyclic alkylbenzene carbon-number series",
        compoundClassId = "monoalkylbenzenes",
        carbonNumberRange = KnowledgeIntRange(7, 30),
        retentionOrder = RetentionOrder.INCREASES_WITH_CARBON_NUMBER,
        memberNameTemplate = "C{n} alkylbenzene candidate",
        expectedIonFragmentIds = listOf("ei-mz-91", "ei-mz-92"),
        interpretationNotes = listOf(
            KnowledgeStatement(
                text = "Use carbon-number labels as candidate grouping, not as verified compound names, unless Kovats and spectrum evidence support the assignment.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("nist-webbook-srd69"),
            ),
        ),
        sourceIds = listOf("nist-webbook-srd69"),
    )

    private val nParaffins = CompoundClassDefinition(
        id = "n-paraffins",
        label = "Normal paraffins (n-alkanes)",
        description = "Straight-chain saturated hydrocarbons used as the reference series for Kovats retention-index calculations.",
        formulaPattern = "CnH2n+2",
        carbonNumberRange = KnowledgeIntRange(7, 30),
        homologousSeriesIds = listOf("n-paraffin-reference-series"),
        interpretationNotes = listOf(
            KnowledgeStatement(
                text = "Reference n-paraffin retention times must come from the same chromatographic method or run family as the target sample before Kovats values are calculated.",
                evidence = KnowledgeEvidence.LITERATURE,
                confidence = 1.0,
                sourceIds = listOf("nist-gc-retention-data"),
            ),
        ),
        assignmentCautions = listOf(
            KnowledgeStatement(
                text = "The built-in n-paraffin entries define the retention-index scale only; they do not replace measured reference retention times.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("nist-gc-retention-data"),
            ),
        ),
        sourceIds = listOf("nist-gc-retention-data"),
    )

    private val nParaffinReferenceSeries = CarbonNumberSeriesDefinition(
        id = "n-paraffin-reference-series",
        label = "n-Paraffin Kovats reference series",
        compoundClassId = "n-paraffins",
        carbonNumberRange = KnowledgeIntRange(7, 30),
        retentionOrder = RetentionOrder.INCREASES_WITH_CARBON_NUMBER,
        memberNameTemplate = "n-C{n} paraffin reference",
        interpretationNotes = listOf(
            KnowledgeStatement(
                text = "Kovats calculation requires the two adjacent n-paraffin references that elute immediately before and after the target peak.",
                evidence = KnowledgeEvidence.LITERATURE,
                confidence = 1.0,
                sourceIds = listOf("nist-gc-retention-data"),
            ),
        ),
        sourceIds = listOf("nist-gc-retention-data"),
    )

    private val nonPolarAlkylbenzeneSeedRi = KovatsReferenceLibrary(
        id = "nist-nonpolar-ramp-alkylbenzene-seed",
        label = "NIST non-polar ramp RI seed: C7-C8 alkylbenzenes",
        chromatogramTypeId = "gc-ms-ei-eic",
        stationaryPhase = "Non-polar capillary phases; Van den Dool and Kratz RI, temperature ramp",
        temperatureProgram = "Multiple NIST WebBook literature records; use as broad seed ranges only.",
        referenceSeriesId = "monoalkylbenzene-carbon-series",
        entries = listOf(
            KovatsReferenceEntry(
                compoundName = "Toluene",
                compoundClassId = "monoalkylbenzenes",
                formula = "C7H8",
                carbonNumber = 7,
                kovatsRange = KnowledgeDoubleRange(769.0, 796.8, "RI"),
                evidence = KnowledgeEvidence.LITERATURE,
                sourceIds = listOf("nist-toluene"),
            ),
            KovatsReferenceEntry(
                compoundName = "Ethylbenzene",
                compoundClassId = "monoalkylbenzenes",
                formula = "C8H10",
                carbonNumber = 8,
                kovatsRange = KnowledgeDoubleRange(850.8, 893.0, "RI"),
                evidence = KnowledgeEvidence.LITERATURE,
                sourceIds = listOf("nist-ethylbenzene"),
            ),
            KovatsReferenceEntry(
                compoundName = "m-Xylene",
                compoundClassId = "monoalkylbenzenes",
                formula = "C8H10",
                carbonNumber = 8,
                kovatsRange = KnowledgeDoubleRange(838.0, 898.0, "RI"),
                evidence = KnowledgeEvidence.LITERATURE,
                sourceIds = listOf("nist-m-xylene"),
            ),
            KovatsReferenceEntry(
                compoundName = "p-Xylene",
                compoundClassId = "monoalkylbenzenes",
                formula = "C8H10",
                carbonNumber = 8,
                kovatsRange = KnowledgeDoubleRange(847.0, 884.0, "RI"),
                evidence = KnowledgeEvidence.LITERATURE,
                sourceIds = listOf("nist-p-xylene"),
            ),
            KovatsReferenceEntry(
                compoundName = "o-Xylene",
                compoundClassId = "monoalkylbenzenes",
                formula = "C8H10",
                carbonNumber = 8,
                kovatsRange = KnowledgeDoubleRange(862.0, 896.0, "RI"),
                evidence = KnowledgeEvidence.LITERATURE,
                sourceIds = listOf("nist-o-xylene"),
            ),
        ),
        sourceIds = listOf("nist-webbook-srd69"),
    )

    private val nParaffinReferenceRiScale = KovatsReferenceLibrary(
        id = "kovats-n-paraffin-reference-scale-c7-c30",
        label = "Kovats n-paraffin reference scale: C7-C30",
        stationaryPhase = "Method-specific; observed reference retention times must be measured under the same chromatographic conditions.",
        temperatureProgram = "Supports both isothermal Kovats and temperature-programmed Van den Dool/Kratz formulas when real reference RTs are supplied.",
        referenceSeriesId = "n-paraffin-reference-series",
        entries = (7..30).map { carbonNumber ->
            KovatsReferenceEntry(
                compoundName = normalAlkaneName(carbonNumber),
                compoundClassId = "n-paraffins",
                formula = normalAlkaneFormula(carbonNumber),
                carbonNumber = carbonNumber,
                kovatsIndex = carbonNumber * 100.0,
                evidence = KnowledgeEvidence.LITERATURE,
                sourceIds = listOf("nist-gc-retention-data"),
            )
        },
        sourceIds = listOf("nist-gc-retention-data"),
    )

    private fun normalAlkaneFormula(carbonNumber: Int): String =
        "C${carbonNumber}H${carbonNumber * 2 + 2}"

    private fun normalAlkaneName(carbonNumber: Int): String =
        when (carbonNumber) {
            7 -> "n-Heptane"
            8 -> "n-Octane"
            9 -> "n-Nonane"
            10 -> "n-Decane"
            11 -> "n-Undecane"
            12 -> "n-Dodecane"
            13 -> "n-Tridecane"
            14 -> "n-Tetradecane"
            15 -> "n-Pentadecane"
            16 -> "n-Hexadecane"
            17 -> "n-Heptadecane"
            18 -> "n-Octadecane"
            19 -> "n-Nonadecane"
            20 -> "n-Eicosane"
            21 -> "n-Heneicosane"
            22 -> "n-Docosane"
            23 -> "n-Tricosane"
            24 -> "n-Tetracosane"
            25 -> "n-Pentacosane"
            26 -> "n-Hexacosane"
            27 -> "n-Heptacosane"
            28 -> "n-Octacosane"
            29 -> "n-Nonacosane"
            30 -> "n-Triacontane"
            else -> "n-C$carbonNumber alkane"
        }
}

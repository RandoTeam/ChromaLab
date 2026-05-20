package com.chromalab.feature.knowledge

object ChromaLabBaseKnowledgePack {

    val pack: LocalKnowledgePack
        get() = buildPack()

    private fun buildPack(): LocalKnowledgePack = LocalKnowledgePack(
        id = "chromalab-base",
        name = "ChromaLab Base Knowledge Pack",
        revision = "2026-05-20-phase-6-unblock",
        description = "Conservative offline ChromaLab-authored rules for GC-MS chromatogram interpretation. Restricted external sources are retained as link-only references and do not provide bundled measurement data.",
        sources = listOf(
            KnowledgeSource(
                id = "nist-webbook-srd69",
                label = "NIST Chemistry WebBook, SRD 69",
                citation = "https://webbook.nist.gov/chemistry/",
                sourceType = KnowledgeSourceType.LITERATURE,
                licenseStatus = KnowledgeLicenseStatus.NEEDS_REVIEW,
                trustTier = KnowledgeSourceTrustTier.TIER_3_LINK_ONLY_RESTRICTED,
                canBundle = false,
                notes = listOf("Link-only restricted source. Do not bundle NIST spectral, compound, or retention data without explicit source/license review."),
            ),
            KnowledgeSource(
                id = "nist-gc-retention-data",
                label = "NIST Chemistry WebBook: Gas Chromatographic Retention Data",
                citation = "https://webbook.nist.gov/chemistry/gc-ri/",
                sourceType = KnowledgeSourceType.LITERATURE,
                licenseStatus = KnowledgeLicenseStatus.NEEDS_REVIEW,
                trustTier = KnowledgeSourceTrustTier.TIER_3_LINK_ONLY_RESTRICTED,
                canBundle = false,
                notes = listOf("Link-only restricted source for future manual reference review. Built-in pack must not bundle NIST GC retention-index tables."),
            ),
            KnowledgeSource(
                id = "nist-toluene",
                label = "NIST Chemistry WebBook: Toluene",
                citation = "https://webbook.nist.gov/cgi/cbook.cgi?ID=C108883",
                sourceType = KnowledgeSourceType.LITERATURE,
                licenseStatus = KnowledgeLicenseStatus.NEEDS_REVIEW,
                trustTier = KnowledgeSourceTrustTier.TIER_3_LINK_ONLY_RESTRICTED,
                canBundle = false,
                notes = listOf("Link-only restricted compound page. Do not bundle formula, mass-spectrum, or retention-index values without license review."),
            ),
            KnowledgeSource(
                id = "nist-ethylbenzene",
                label = "NIST Chemistry WebBook: Ethylbenzene",
                citation = "https://webbook.nist.gov/cgi/cbook.cgi?ID=C100414",
                sourceType = KnowledgeSourceType.LITERATURE,
                licenseStatus = KnowledgeLicenseStatus.NEEDS_REVIEW,
                trustTier = KnowledgeSourceTrustTier.TIER_3_LINK_ONLY_RESTRICTED,
                canBundle = false,
                notes = listOf("Link-only restricted compound page. Do not bundle formula, mass-spectrum, or retention-index values without license review."),
            ),
            KnowledgeSource(
                id = "nist-m-xylene",
                label = "NIST Chemistry WebBook: m-Xylene",
                citation = "https://webbook.nist.gov/cgi/cbook.cgi?ID=C108383",
                sourceType = KnowledgeSourceType.LITERATURE,
                licenseStatus = KnowledgeLicenseStatus.NEEDS_REVIEW,
                trustTier = KnowledgeSourceTrustTier.TIER_3_LINK_ONLY_RESTRICTED,
                canBundle = false,
                notes = listOf("Link-only restricted compound page. Do not bundle formula, mass-spectrum, or retention-index values without license review."),
            ),
            KnowledgeSource(
                id = "nist-p-xylene",
                label = "NIST Chemistry WebBook: p-Xylene",
                citation = "https://webbook.nist.gov/cgi/cbook.cgi?ID=C106423",
                sourceType = KnowledgeSourceType.LITERATURE,
                licenseStatus = KnowledgeLicenseStatus.NEEDS_REVIEW,
                trustTier = KnowledgeSourceTrustTier.TIER_3_LINK_ONLY_RESTRICTED,
                canBundle = false,
                notes = listOf("Link-only restricted compound page. Do not bundle formula, mass-spectrum, or retention-index values without license review."),
            ),
            KnowledgeSource(
                id = "nist-o-xylene",
                label = "NIST Chemistry WebBook: o-Xylene",
                citation = "https://webbook.nist.gov/cgi/cbook.cgi?ID=C95476",
                sourceType = KnowledgeSourceType.LITERATURE,
                licenseStatus = KnowledgeLicenseStatus.NEEDS_REVIEW,
                trustTier = KnowledgeSourceTrustTier.TIER_3_LINK_ONLY_RESTRICTED,
                canBundle = false,
                notes = listOf("Link-only restricted compound page. Do not bundle formula, mass-spectrum, or retention-index values without license review."),
            ),
            KnowledgeSource(
                id = "chromalab-reference-report-contract",
                label = "ChromaLab reference report contract",
                sourceType = KnowledgeSourceType.INTERNAL_CURATED,
                notes = listOf("Internal contract derived from the supplied Belyi Tigr reference report format: identify graph type, ion/channel, axes, peak table, Kovats status, interpretation basis, and confidence limits explicitly."),
            ),
            KnowledgeSource(
                id = "chromalab-petroleum-gcms-curation",
                label = "ChromaLab conservative petroleum GC-MS curation",
                sourceType = KnowledgeSourceType.INTERNAL_CURATED,
                notes = listOf("Used for conservative chromatogram-channel labeling only. It must not produce final compound names without retention-index, spectrum/library, or user confirmation."),
            ),
        ),
        chromatogramTypes = listOf(gcMsEiTic, gcMsEiEic, gcMsEiXic, gcMsEiSim),
        ionFragments = listOf(
            eiMz57,
            eiMz71,
            eiMz83,
            eiMz91,
            eiMz92,
            eiMz191,
            eiMz217,
            eiMz218,
            eiMz1980315,
            eiMz326,
            eiMz360,
            eiMz394,
        ),
        compoundClasses = listOf(monoAlkylbenzenes, nParaffins, petroleumBiomarkers, methodTargetedExtracts),
        carbonNumberSeries = listOf(alkylbenzeneSeries, nParaffinReferenceSeries),
        kovatsLibraries = listOf(nParaffinReferenceRiScale),
    )

    private val gcMsEiTic = ChromatogramTypeDefinition(
        id = "gc-ms-ei-tic",
        label = "GC-MS EI total ion chromatogram",
        analysisType = "GC-MS",
        chromatogramMode = "TIC",
        expectedXAxisUnit = "min",
        expectedYAxisUnit = "counts",
        supportedIonFragmentIds = listOf("ei-mz-57", "ei-mz-71", "ei-mz-83", "ei-mz-91", "ei-mz-92", "ei-mz-191", "ei-mz-217", "ei-mz-218"),
        targetCompoundClassIds = listOf("n-paraffins", "monoalkylbenzenes", "petroleum-biomarkers"),
        interpretationNotes = listOf(
            KnowledgeStatement(
                text = "A TIC is a broad signal summary. Peak detection, baseline quality, and co-elution warnings are valid, but compound-class labels require extracted-ion, spectrum, retention-index, or user/library evidence.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("chromalab-reference-report-contract"),
            ),
        ),
        sourceIds = listOf("chromalab-reference-report-contract"),
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
                sourceIds = listOf("chromalab-reference-report-contract"),
            ),
        ),
        sourceIds = listOf("chromalab-reference-report-contract"),
    )

    private val gcMsEiXic = ChromatogramTypeDefinition(
        id = "gc-ms-ei-xic",
        label = "GC-MS extracted ion chromatogram / exact-mass XIC",
        analysisType = "GC-MS",
        chromatogramMode = "XIC",
        expectedXAxisUnit = "min",
        expectedYAxisUnit = "counts",
        supportedIonFragmentIds = listOf("ei-mz-198-0315", "ei-mz-326", "ei-mz-360", "ei-mz-394"),
        targetCompoundClassIds = listOf("method-targeted-extracts"),
        interpretationNotes = listOf(
            KnowledgeStatement(
                text = "An exact-mass or narrow-window XIC is method-targeted evidence. The report may preserve the channel and measured peaks, but formula and compound names stay unresolved unless the method/library supplies them.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("chromalab-petroleum-gcms-curation"),
            ),
        ),
        sourceIds = listOf("chromalab-petroleum-gcms-curation"),
    )

    private val gcMsEiSim = ChromatogramTypeDefinition(
        id = "gc-ms-ei-sim",
        label = "GC-MS selected ion monitoring chromatogram",
        analysisType = "GC-MS",
        chromatogramMode = "SIM",
        expectedXAxisUnit = "min",
        expectedYAxisUnit = "counts",
        supportedIonFragmentIds = listOf("ei-mz-71", "ei-mz-83", "ei-mz-191", "ei-mz-217", "ei-mz-218"),
        targetCompoundClassIds = listOf("n-paraffins", "petroleum-biomarkers"),
        interpretationNotes = listOf(
            KnowledgeStatement(
                text = "SIM panels should be interpreted graph-by-graph and ion-by-ion. A channel can support a compound-class hypothesis, but it is not a standalone compound identification.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("chromalab-reference-report-contract"),
            ),
        ),
        sourceIds = listOf("chromalab-reference-report-contract"),
    )

    private val eiMz57 = IonFragmentDefinition(
        id = "ei-mz-57",
        nominalMz = 57.0,
        mzWindow = KnowledgeDoubleRange(56.70, 57.70, "m/z"),
        label = "m/z 57 EI hydrocarbon channel",
        ionization = IonizationMode.ELECTRON_IONIZATION,
        diagnosticForCompoundClassIds = listOf("n-paraffins"),
        relatedIonFragmentIds = listOf("ei-mz-71"),
        interpretation = listOf(
            KnowledgeStatement(
                text = "m/z 57 is treated as general saturated-hydrocarbon fragment evidence in this pack, useful for n-paraffin-pattern review but not specific enough for a peak name.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 0.75,
                sourceIds = listOf("chromalab-petroleum-gcms-curation"),
            ),
        ),
        cautions = listOf(channelNotStandalone("m/z 57")),
        sourceIds = listOf("chromalab-petroleum-gcms-curation"),
    )

    private val eiMz71 = IonFragmentDefinition(
        id = "ei-mz-71",
        nominalMz = 71.0,
        mzWindow = KnowledgeDoubleRange(70.70, 71.70, "m/z"),
        label = "m/z 71 EI alkane-series channel",
        ionization = IonizationMode.ELECTRON_IONIZATION,
        diagnosticForCompoundClassIds = listOf("n-paraffins"),
        relatedIonFragmentIds = listOf("ei-mz-57", "ei-mz-83"),
        interpretation = listOf(
            KnowledgeStatement(
                text = "m/z 71 is a conservative n-paraffin / saturated-hydrocarbon series channel for report grouping when the chromatogram shows a homologous retention pattern.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 0.75,
                sourceIds = listOf("chromalab-petroleum-gcms-curation"),
            ),
        ),
        cautions = listOf(channelNotStandalone("m/z 71")),
        sourceIds = listOf("chromalab-petroleum-gcms-curation"),
    )

    private val eiMz83 = IonFragmentDefinition(
        id = "ei-mz-83",
        nominalMz = 83.0,
        mzWindow = KnowledgeDoubleRange(82.70, 83.70, "m/z"),
        label = "m/z 83 EI hydrocarbon channel",
        ionization = IonizationMode.ELECTRON_IONIZATION,
        diagnosticForCompoundClassIds = listOf("n-paraffins", "petroleum-biomarkers"),
        relatedIonFragmentIds = listOf("ei-mz-71"),
        interpretation = listOf(
            KnowledgeStatement(
                text = "m/z 83 is stored as hydrocarbon-channel evidence for SIM/EIC panels. It can guide class-level review but must remain lower confidence without method context.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 0.65,
                sourceIds = listOf("chromalab-petroleum-gcms-curation"),
            ),
        ),
        cautions = listOf(channelNotStandalone("m/z 83")),
        sourceIds = listOf("chromalab-petroleum-gcms-curation"),
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
                text = "The m/z 92 channel is aromatic-channel evidence in this pack. It must remain a channel/class hint unless retention, spectrum/library, method, or user evidence supports a compound assignment.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 0.8,
                sourceIds = listOf("chromalab-petroleum-gcms-curation"),
            ),
            KnowledgeStatement(
                text = "In alkylbenzene-oriented GC-MS work, m/z 92 is aromatic-channel evidence but is not a standalone compound assignment.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 0.8,
                sourceIds = listOf("chromalab-petroleum-gcms-curation"),
            ),
        ),
        cautions = listOf(
            KnowledgeStatement(
                text = "Do not identify a peak as toluene or an alkylbenzene from m/z 92 alone; require retention index, adjacent ions, full spectrum, or user/library confirmation.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("chromalab-reference-report-contract"),
            ),
        ),
        sourceIds = listOf("chromalab-petroleum-gcms-curation"),
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
                sourceIds = listOf("chromalab-petroleum-gcms-curation"),
            ),
        ),
        cautions = listOf(
            KnowledgeStatement(
                text = "Treat m/z 91 as supporting evidence only; it is not specific enough for release-quality compound naming by itself.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("chromalab-reference-report-contract"),
            ),
        ),
        sourceIds = listOf("chromalab-petroleum-gcms-curation"),
    )

    private val eiMz191 = IonFragmentDefinition(
        id = "ei-mz-191",
        nominalMz = 191.0,
        mzWindow = KnowledgeDoubleRange(190.70, 191.70, "m/z"),
        label = "m/z 191 petroleum biomarker SIM channel",
        ionization = IonizationMode.ELECTRON_IONIZATION,
        diagnosticForCompoundClassIds = listOf("petroleum-biomarkers"),
        relatedIonFragmentIds = listOf("ei-mz-217", "ei-mz-218"),
        interpretation = listOf(
            KnowledgeStatement(
                text = "m/z 191 is treated as petroleum-biomarker channel evidence in this pack. Report text must keep any terpane/hopane assignment as a hypothesis until method/library evidence is available.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 0.65,
                sourceIds = listOf("chromalab-petroleum-gcms-curation"),
            ),
        ),
        cautions = listOf(channelNotStandalone("m/z 191")),
        sourceIds = listOf("chromalab-petroleum-gcms-curation"),
    )

    private val eiMz217 = IonFragmentDefinition(
        id = "ei-mz-217",
        nominalMz = 217.0,
        mzWindow = KnowledgeDoubleRange(216.70, 217.70, "m/z"),
        label = "m/z 217 petroleum biomarker SIM channel",
        ionization = IonizationMode.ELECTRON_IONIZATION,
        diagnosticForCompoundClassIds = listOf("petroleum-biomarkers"),
        relatedIonFragmentIds = listOf("ei-mz-218", "ei-mz-191"),
        interpretation = listOf(
            KnowledgeStatement(
                text = "m/z 217 is stored as biomarker-channel evidence for petroleum GC-MS work. It supports a class-level hypothesis only without matching retention order and library context.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 0.65,
                sourceIds = listOf("chromalab-petroleum-gcms-curation"),
            ),
        ),
        cautions = listOf(channelNotStandalone("m/z 217")),
        sourceIds = listOf("chromalab-petroleum-gcms-curation"),
    )

    private val eiMz218 = IonFragmentDefinition(
        id = "ei-mz-218",
        nominalMz = 218.0,
        mzWindow = KnowledgeDoubleRange(217.70, 218.70, "m/z"),
        label = "m/z 218 related biomarker SIM channel",
        ionization = IonizationMode.ELECTRON_IONIZATION,
        diagnosticForCompoundClassIds = listOf("petroleum-biomarkers"),
        relatedIonFragmentIds = listOf("ei-mz-217", "ei-mz-191"),
        interpretation = listOf(
            KnowledgeStatement(
                text = "m/z 218 is stored as related biomarker-channel evidence and should be interpreted together with the method's expected ion set and retention pattern.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 0.60,
                sourceIds = listOf("chromalab-petroleum-gcms-curation"),
            ),
        ),
        cautions = listOf(channelNotStandalone("m/z 218")),
        sourceIds = listOf("chromalab-petroleum-gcms-curation"),
    )

    private val eiMz1980315 = IonFragmentDefinition(
        id = "ei-mz-198-0315",
        nominalMz = 198.0315,
        mzWindow = KnowledgeDoubleRange(198.0313, 198.0317, "m/z"),
        label = "m/z 198.0315 exact-mass XIC",
        ionization = IonizationMode.UNKNOWN,
        diagnosticForCompoundClassIds = listOf("method-targeted-extracts"),
        interpretation = listOf(
            KnowledgeStatement(
                text = "m/z 198.0315 is retained as exact-channel evidence. The app must report the measured channel and resolution window, but compound/formula assignment requires the method target list.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("chromalab-reference-report-contract"),
            ),
        ),
        cautions = listOf(channelNotStandalone("m/z 198.0315")),
        sourceIds = listOf("chromalab-reference-report-contract"),
    )

    private val eiMz326 = highMassTargetIon("ei-mz-326", 326.0)
    private val eiMz360 = highMassTargetIon("ei-mz-360", 360.0)
    private val eiMz394 = highMassTargetIon("ei-mz-394", 394.0)

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
                sourceIds = listOf("chromalab-petroleum-gcms-curation"),
            ),
        ),
        assignmentCautions = listOf(
            KnowledgeStatement(
                text = "Xylene and ethylbenzene isomers share formula C8H10; retention index and full-spectrum evidence are needed to separate candidates.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 0.9,
                sourceIds = listOf("chromalab-reference-report-contract"),
            ),
        ),
        sourceIds = listOf("chromalab-petroleum-gcms-curation"),
    )

    private val petroleumBiomarkers = CompoundClassDefinition(
        id = "petroleum-biomarkers",
        label = "Petroleum biomarker channels",
        description = "Class-level bucket for petroleum geochemistry SIM/EIC channels. Built-in notes are conservative and never assign a peak without method/library support.",
        carbonNumberRange = KnowledgeIntRange(15, 40),
        diagnosticIonFragmentIds = listOf("ei-mz-83", "ei-mz-191", "ei-mz-217", "ei-mz-218"),
        interpretationNotes = listOf(
            KnowledgeStatement(
                text = "Biomarker-channel reports should emphasize ion channel, retention order, peak ratios, and confidence flags before any compound names.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("chromalab-petroleum-gcms-curation"),
            ),
        ),
        assignmentCautions = listOf(
            KnowledgeStatement(
                text = "Do not infer sterane, terpane, hopane, or related biomarker names from one ion channel alone.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("chromalab-petroleum-gcms-curation"),
            ),
        ),
        sourceIds = listOf("chromalab-petroleum-gcms-curation"),
    )

    private val methodTargetedExtracts = CompoundClassDefinition(
        id = "method-targeted-extracts",
        label = "Method-targeted extracted channels",
        description = "Exact-mass or high-mass extracted channels whose meaning depends on a supplied instrument method or target list.",
        diagnosticIonFragmentIds = listOf("ei-mz-198-0315", "ei-mz-326", "ei-mz-360", "ei-mz-394"),
        interpretationNotes = listOf(
            KnowledgeStatement(
                text = "Report the channel, resolution window, peaks, and quality metrics, but keep compound identity unresolved until the method target list is attached.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("chromalab-reference-report-contract"),
            ),
        ),
        assignmentCautions = listOf(
            KnowledgeStatement(
                text = "Exact m/z alone is not a release-quality identification without formula/adduct rules, isotope checks, retention evidence, or a library target.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("chromalab-reference-report-contract"),
            ),
        ),
        sourceIds = listOf("chromalab-reference-report-contract"),
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
                sourceIds = listOf("chromalab-reference-report-contract"),
            ),
        ),
        sourceIds = listOf("chromalab-reference-report-contract"),
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
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("chromalab-reference-report-contract"),
            ),
        ),
        assignmentCautions = listOf(
            KnowledgeStatement(
                text = "The built-in n-paraffin entries define the retention-index scale only; they do not replace measured reference retention times.",
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("chromalab-reference-report-contract"),
            ),
        ),
        sourceIds = listOf("chromalab-reference-report-contract"),
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
                evidence = KnowledgeEvidence.CURATED,
                confidence = 1.0,
                sourceIds = listOf("chromalab-reference-report-contract"),
            ),
        ),
        sourceIds = listOf("chromalab-reference-report-contract"),
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
                evidence = KnowledgeEvidence.CURATED,
                sourceIds = listOf("chromalab-reference-report-contract"),
            )
        },
        sourceIds = listOf("chromalab-reference-report-contract"),
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

    private fun highMassTargetIon(id: String, nominalMz: Double): IonFragmentDefinition =
        IonFragmentDefinition(
            id = id,
            nominalMz = nominalMz,
            mzWindow = KnowledgeDoubleRange(nominalMz - 0.30, nominalMz + 0.30, "m/z"),
            label = "m/z ${nominalMz.toInt()} high-mass extracted channel",
            ionization = IonizationMode.UNKNOWN,
            diagnosticForCompoundClassIds = listOf("method-targeted-extracts"),
            interpretation = listOf(
                KnowledgeStatement(
                    text = "This high-mass extracted channel is preserved as method-targeted evidence. The app must not infer a compound name unless the target method or library supplies one.",
                    evidence = KnowledgeEvidence.CURATED,
                    confidence = 1.0,
                    sourceIds = listOf("chromalab-reference-report-contract"),
                ),
            ),
            cautions = listOf(channelNotStandalone("m/z ${nominalMz.toInt()}")),
            sourceIds = listOf("chromalab-reference-report-contract"),
        )

    private fun channelNotStandalone(label: String): KnowledgeStatement =
        KnowledgeStatement(
            text = "$label is channel evidence only; final compound assignment requires retention-index, spectrum/library, method target, or user confirmation.",
            evidence = KnowledgeEvidence.CURATED,
            confidence = 1.0,
            sourceIds = listOf("chromalab-reference-report-contract"),
        )
}

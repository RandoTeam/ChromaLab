package com.chromalab.feature.knowledge

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ChromaLabKnowledgeSeedV2 {
    val pack: KnowledgePackVersion = KnowledgePackVersion(
        version = CHROMALAB_KNOWLEDGE_PACK_VERSION_V2,
        title = "ChromaLab Knowledge Seed v2",
        description = "Expanded curated offline-first knowledge pack for chromatogram terminology, OCR/text classification, report caveats, safe VLM grounding, and compound-class stubs. It contains no measured sample RT, peak area, spectrum, or proprietary library data.",
        sourceRefs = sourceRefs(),
        entries = entries(),
    )

    val seedJson: String
        get() = json.encodeToString(pack)

    private fun sourceRefs(): List<KnowledgeSourceRef> = listOf(
        KnowledgeSourceRef(
            sourceId = "chromalab-curated-v2",
            label = "ChromaLab curated chromatography safety rules v2",
            citation = "Internal conservative rules derived from ChromaLab Phase 0-6C evidence gates.",
            license = "Project internal curated content",
            licenseStatus = KnowledgeLicenseStatus.INTERNAL_CURATED,
            trustTier = KnowledgeSourceTrustTier.INTERNAL_CURATED,
            canBundle = true,
            canTransform = true,
            notes = "Primary source for text classification rules, release caveats, and safe prompt snippets.",
        ),
        KnowledgeSourceRef(
            sourceId = "w3c-prov-o",
            label = "W3C PROV-O",
            url = "https://www.w3.org/TR/prov-o/",
            citation = "W3C PROV-O recommendation.",
            license = "W3C document license",
            licenseStatus = KnowledgeLicenseStatus.OPEN_VERIFIED,
            trustTier = KnowledgeSourceTrustTier.OFFICIAL_STANDARD,
            attributionRequired = true,
            canBundle = true,
            canTransform = true,
            notes = "Used to align knowledge and report provenance vocabulary.",
        ),
        KnowledgeSourceRef(
            sourceId = "chebi-cc-by-4",
            label = "ChEBI, Chemical Entities of Biological Interest",
            url = "https://www.ebi.ac.uk/chebi/aboutChebiForward.do",
            citation = "EMBL-EBI ChEBI data, CC BY 4.0 subject to EMBL-EBI terms.",
            license = "CC BY 4.0",
            licenseStatus = KnowledgeLicenseStatus.ATTRIBUTION_REQUIRED,
            trustTier = KnowledgeSourceTrustTier.OPEN_ONTOLOGY,
            attributionRequired = true,
            canBundle = true,
            canTransform = true,
            notes = "Permitted candidate for future curated chemical synonyms after attribution and source snapshot review.",
        ),
        KnowledgeSourceRef(
            sourceId = "pubchem-provenance-policy",
            label = "PubChem download and provenance policy",
            url = "https://pubchem.ncbi.nlm.nih.gov/docs/downloads",
            citation = "PubChem download documentation and NCBI/NLM policies.",
            license = "NCBI/NLM policies plus contributor-specific source terms",
            licenseStatus = KnowledgeLicenseStatus.NEEDS_REVIEW,
            trustTier = KnowledgeSourceTrustTier.OFFICIAL_DATABASE,
            attributionRequired = true,
            canBundle = false,
            canTransform = false,
            apiLookupOnly = true,
            notes = "Use only through reviewed source definitions until contributor-level licenses are checked.",
        ),
        KnowledgeSourceRef(
            sourceId = "nist-srd-review-required",
            label = "NIST Chemistry WebBook / AMDIS / SRD policy review",
            url = "https://webbook.nist.gov/chemistry/",
            citation = "NIST Chemistry WebBook, Standard Reference Database 69.",
            license = "NIST Standard Reference Data Program terms; bundling requires explicit product review",
            licenseStatus = KnowledgeLicenseStatus.NEEDS_REVIEW,
            trustTier = KnowledgeSourceTrustTier.OFFICIAL_DATABASE,
            attributionRequired = true,
            canBundle = false,
            canTransform = false,
            apiLookupOnly = false,
            notes = "Referenced for policy and caveats only here; no NIST spectral, RI, AMDIS, or WebBook database data is bundled.",
        ),
    )

    private fun entries(): List<KnowledgeEntry> =
        listOf(
            glossary("kp2-term-chromatogram", "Chromatogram", "A chromatogram is a signal trace plotted against retention coordinate, usually time, and must be interpreted through calibrated axes and trace evidence.", listOf("chromatogram", "chromatographic trace")),
            glossary("kp2-term-retention-time", "Retention time", "Retention time (RT) is the calibrated X coordinate of a chromatographic event, normally reported at the peak apex after X-axis calibration.", listOf("RT", "retention time", "peak time")),
            glossary("kp2-term-peak", "Peak", "A chromatographic peak is a local signal feature with apex, boundaries, baseline context, and integration evidence.", listOf("peak", "chromatographic peak")),
            glossary("kp2-term-peak-apex", "Peak apex", "Peak apex is the local maximum point of a detected peak on the extracted trace.", listOf("apex", "peak apex", "local maximum")),
            glossary("kp2-term-peak-area", "Peak area", "Peak area is the deterministic integration result between accepted boundaries; knowledge entries cannot create it.", listOf("area", "peak area", "integrated area")),
            glossary("kp2-term-area-percent", "Area percent", "Area percent is a derived relative value based on measured peak areas and report policy.", listOf("area percent", "area %", "relative area")),
            glossary("kp2-term-abundance", "Abundance", "Abundance is a Y-axis detector signal magnitude, not a compound identity.", listOf("abundance", "counts")),
            glossary("kp2-term-intensity", "Intensity", "Intensity is detector signal magnitude and must be linked to Y calibration before release-quality metric claims.", listOf("intensity", "signal intensity")),
            glossary("kp2-term-baseline", "Baseline", "Baseline is the local signal background used by deterministic integration and quality review.", listOf("baseline", "background")),
            glossary("kp2-term-baseline-drift", "Baseline drift", "Baseline drift is slow background movement that can affect integration boundaries and should trigger review when severe.", listOf("baseline drift")),
            glossary("kp2-term-noise", "Noise", "Noise is local signal variation used for quality review and S/N calculations when measured.", listOf("noise", "signal noise")),
            glossary("kp2-term-rms-noise", "RMS noise", "RMS noise is a measured noise estimate; it must not be inferred from glossary knowledge.", listOf("RMS noise", "root mean square noise")),
            glossary("kp2-term-sn", "Signal-to-noise", "S/N is signal-to-noise ratio and requires measured signal and measured noise evidence.", listOf("S/N", "snr", "signal-to-noise")),
            glossary("kp2-term-fwhm", "FWHM", "FWHM is full width at half maximum and must come from measured trace and peak evidence.", listOf("FWHM", "full width at half maximum")),
            glossary("kp2-term-peak-width", "Peak width", "Peak width describes the span of a peak under an explicit width definition such as FWHM or integration bounds.", listOf("peak width", "width")),
            glossary("kp2-term-integration-boundary", "Integration boundary", "Integration boundaries delimit deterministic area calculation and must be visible in evidence when reported.", listOf("integration boundary", "peak boundary")),
            glossary("kp2-term-shoulder-peak", "Shoulder peak", "A shoulder peak is a partially resolved feature on the side of a larger peak and normally requires review.", listOf("shoulder", "shoulder peak")),
            glossary("kp2-term-overlap", "Overlapping peak", "Overlapping peaks share signal and require boundary/provenance caveats before release-quality integration.", listOf("overlap", "overlapping peak")),
            glossary("kp2-term-coelution", "Coelution", "Coelution means two or more compounds or chromatographic features elute together and cannot be separated by image evidence alone.", listOf("coelution", "co-elution")),
            glossary("kp2-term-resolution", "Resolution", "Resolution describes chromatographic separation quality; it is not inferred without peak width and spacing evidence.", listOf("resolution", "chromatographic resolution")),
            glossary("kp2-term-tailing-asymmetry", "Tailing/asymmetry", "Tailing or asymmetry is a peak-shape review flag and should not be converted into a compound identity.", listOf("tailing", "asymmetry", "peak asymmetry")),
            msTerm("kp2-ms-tic", "TIC", "A total ion chromatogram summarizes total ion current over retention time.", listOf("TIC", "total ion chromatogram")),
            msTerm("kp2-ms-eic", "EIC", "An extracted ion chromatogram shows signal from a selected m/z or mass window.", listOf("EIC", "extracted ion chromatogram")),
            msTerm("kp2-ms-sim", "SIM", "Selected ion monitoring tracks chosen ion channels and does not by itself identify compounds.", listOf("SIM", "selected ion monitoring")),
            msTerm("kp2-ms-scan", "SCAN", "SCAN mode records mass spectra over a range and may accompany TIC data, but report identities still require evidence.", listOf("SCAN", "scan mode")),
            msTerm("kp2-ms-mz", "m/z", "m/z is mass-to-charge ratio; values in ion/channel headings are channel metadata, not retention-time labels.", listOf("m/z", "mass-to-charge", "mass charge")),
            ionTerm("kp2-ion-ion", "Ion", "Ion labels identify a mass channel or ion selection and must not be treated as peak annotations.", listOf("ion", "ion label")),
            ionTerm("kp2-ion-channel", "Ion channel", "Ion/channel text such as Ion 71.00 (70.70 to 71.70) names a signal channel, not a peak RT.", listOf("ion channel", "channel", "ion range")),
            ionTerm("kp2-ion-mass-range", "Mass range", "Mass range values in parentheses describe the m/z extraction window and are not retention times.", listOf("mass range", "70.70 to 71.70")),
            msTerm("kp2-ms-extracted-ion-chromatogram", "Extracted ion chromatogram", "An extracted ion chromatogram is channel-specific trace evidence, not standalone compound identification.", listOf("extracted ion chromatogram", "XIC", "EIC")),
            msTerm("kp2-ms-selected-ion-monitoring", "Selected ion monitoring", "Selected ion monitoring panels must preserve channel provenance and confidence limits.", listOf("selected ion monitoring", "SIM chromatogram")),
            axisTerm("kp2-axis-x", "X-axis", "The X-axis maps pixel position to retention coordinate after calibration.", listOf("x-axis", "time axis")),
            axisTerm("kp2-axis-y", "Y-axis", "The Y-axis maps pixel position to abundance or intensity after calibration.", listOf("y-axis", "abundance axis")),
            axisTerm("kp2-axis-tick-label", "Tick label", "Tick labels may calibrate an axis only when linked to deterministic tick geometry.", listOf("tick label", "axis tick")),
            axisTerm("kp2-axis-axis-label", "Axis label", "Axis labels describe units or signal meaning and are not peak labels.", listOf("axis label", "Ret.Time", "Abundance")),
            axisTerm("kp2-axis-plot-area", "Plot area", "Plot area is the coordinate rectangle containing trace/grid/frame content, excluding titles and labels.", listOf("plot area", "plotArea")),
            axisTerm("kp2-axis-graph-panel", "Graph panel", "Graph panel is the full chart block, including title/channel text, axes, tick labels, and plot area.", listOf("graph panel", "graphPanel", "chart panel")),
            axisTerm("kp2-axis-title", "Title", "Title/header text describes the graph or channel and must not become peak evidence.", listOf("title", "header")),
            axisTerm("kp2-axis-legend", "Legend", "Legend text labels series or channels and is not a peak annotation.", listOf("legend", "series label")),
            axisTerm("kp2-axis-grid-line", "Grid line", "Grid lines are visual guides and must be suppressed before trace or peak evidence is accepted.", listOf("grid line", "grid")),
            axisTerm("kp2-axis-calibration-anchor", "Calibration anchor", "A calibration anchor pairs deterministic pixel geometry with an OCR/user value; text alone is not enough.", listOf("calibration anchor", "axis anchor")),
            retentionRule("kp2-ri-kovats-index", "Kovats index", "Kovats index requires valid reference-series retention data and cannot be calculated from glossary knowledge.", listOf("Kovats index", "Kovats")),
            retentionRule("kp2-ri-retention-index", "Retention index", "Retention index is a derived chromatographic value requiring method-compatible reference evidence.", listOf("retention index", "RI")),
            retentionRule("kp2-ri-n-alkane-reference", "n-alkane reference series", "n-alkane reference series entries define the RI scale, but measured reference RTs must come from the same method or run family.", listOf("n-alkane reference series", "normal alkane reference")),
            pattern("kp2-ri-c10-c40-pattern", "C10-C40 n-alkane pattern", "C10-C40 labels follow straight-chain saturated hydrocarbon naming; this is a naming stub only.", listOf("C10-C40", "n-C10 to n-C40")),
            caveat("kp2-ri-interpolation-caveat", "Retention-index interpolation caveat", "RI interpolation must use adjacent reference alkanes around the target peak and preserve the method context.", listOf("interpolation caveat", "RI interpolation")),
            caveat("kp2-ri-missing-reference-caveat", "Missing reference caveat", "If the reference series is missing or not method-compatible, RI/Kovats must be omitted or marked unavailable.", listOf("missing reference caveat", "no RI references")),
            compoundClass("kp2-class-alkane", "Alkane", "Alkanes are saturated hydrocarbons; class terminology cannot identify a sample peak without evidence.", listOf("alkane", "paraffin")),
            compoundClass("kp2-class-n-alkane", "n-Alkane", "n-alkanes are straight-chain alkanes and may be used as reference stubs when measured reference RTs are supplied.", listOf("n-alkane", "normal alkane", "n-paraffin")),
            compoundClass("kp2-class-hydrocarbon", "Hydrocarbon", "Hydrocarbon class labels describe compounds containing carbon and hydrogen; they are report grouping terms, not IDs.", listOf("hydrocarbon")),
            compoundClass("kp2-class-aromatic-hydrocarbon", "Aromatic hydrocarbon", "Aromatic hydrocarbon is a compound-class term requiring spectral/retention evidence before sample assignment.", listOf("aromatic hydrocarbon", "alkylbenzene")),
            compoundClass("kp2-class-biomarker", "Biomarker", "Petroleum biomarker labels require method and library context; an ion channel alone is insufficient.", listOf("biomarker", "petroleum biomarker")),
            compoundClass("kp2-class-sterane", "Sterane", "Sterane is a biomarker class term and must not be assigned from a single ion/channel image alone.", listOf("sterane", "steranes")),
            compoundClass("kp2-class-terpane", "Terpane", "Terpane is a biomarker class term and must not be assigned without validated method/library evidence.", listOf("terpane", "terpanes")),
            compoundClass("kp2-class-pah", "PAH", "PAH means polycyclic aromatic hydrocarbon; class-level wording requires supporting evidence and caveats.", listOf("PAH", "polycyclic aromatic hydrocarbon")),
            compoundClass("kp2-class-compound-class", "Compound class", "Compound class terms are semantic groupings and are weaker evidence than compound identification.", listOf("compound class", "class assignment")),
            compoundClass("kp2-class-ucm", "Unresolved complex mixture", "An unresolved complex mixture is a broad chromatographic feature; image analysis should report it as review context, not a list of compounds.", listOf("unresolved complex mixture", "UCM")),
            textRule("kp2-rule-ion-title-not-peak", "Ion/channel title is not peak label", "Ion/channel title text, including numeric m/z windows, is TITLE_OR_CHANNEL and must not create PeakLabelEvidence.", listOf("Ion 71.00 (70.70 to 71.70)", "ion title not peak", "TITLE_OR_CHANNEL")),
            textRule("kp2-rule-mz-not-rt", "m/z value is not retention time", "An m/z value or mass range number is not a retention-time peak label.", listOf("m/z value not RT", "mass number not retention time")),
            textRule("kp2-rule-mass-range-not-peak-rt", "Mass range number is not peak RT", "Numbers inside an ion/mass range are channel metadata and must be classified away from peak annotation.", listOf("70.70 to 71.70", "mass range number")),
            textRule("kp2-rule-title-header-not-peak", "Title/header numbers are not peak annotations", "Numbers in title/header regions are graph metadata unless local plot and signal verification proves otherwise.", listOf("title numbers", "header numbers")),
            textRule("kp2-rule-peak-annotation-signal-verified", "Peak annotation requires signal verification", "Peak annotations require plot proximity and local signal verification before they can seed a peak review candidate.", listOf("5.610", "peak label", "signal verification")),
            textRule("kp2-rule-tick-label-geometry", "Tick label requires geometry linkage", "Tick labels participate in calibration only when linked to deterministic tick pixel positions.", listOf("tick geometry", "tick label linkage")),
            textRule("kp2-rule-axis-label-not-peak", "Axis label is not peak label", "Axis labels such as Abundance, Intensity, or Retention Time must not become peak annotations.", listOf("axis label not peak", "Abundance label")),
            textRule("kp2-rule-legend-not-peak", "Legend text is not peak label", "Legend/series text labels signals or channels and cannot create peak evidence.", listOf("legend text", "legend not peak")),
            caveat("kp2-caveat-calibration-required", "Calibration required for release metrics", "No release-quality RT, height, area, FWHM, or S/N claim is allowed without valid calibration evidence.", listOf("calibration required", "release metrics require calibration")),
            caveat("kp2-caveat-trace-required", "Trace evidence required for peak metrics", "Peak metrics require linked trace, boundaries, and peak evidence; knowledge cannot fill missing trace evidence.", listOf("trace evidence required", "peak metrics require trace")),
            caveat("kp2-caveat-no-compound-assignment", "No compound assignment without explicit evidence", "Compound assignment requires explicit library, spectrum, retention-index, method, or user-confirmed evidence.", listOf("compound assignment requires explicit evidence", "no compound assignment")),
            caveat("kp2-caveat-no-kovats-without-reference", "No Kovats without reference series", "Kovats/RI must not be reported as calculated without valid same-method reference-series data.", listOf("no Kovats without reference series", "no RI references")),
            caveat("kp2-caveat-review-grade-peaks", "Review-grade peaks must be labelled", "Peaks with weak, shoulder, overlap, sparse-trace, or low-S/N evidence must be clearly labelled review-grade.", listOf("review-grade peaks", "peak review label")),
            caveat("kp2-caveat-vlm-semantic-only", "VLM explanation is semantic assistance only", "VLM may explain warnings and classify text, but deterministic CV/math owns numeric measurements.", listOf("VLM semantic only", "model cannot measure")),
            caveat("kp2-caveat-image-uncertainty", "Image-derived analysis has uncertainty", "Image-derived chromatogram analysis must expose calibration, trace, OCR, and peak evidence limits.", listOf("image uncertainty", "image-derived analysis")),
            safety("kp2-safety-knowledge-cannot-measure", "Knowledge cannot create numeric metrics", "Knowledge can explain and classify; it cannot fabricate RT, height, area, FWHM, S/N, baseline, Kovats, or integration boundaries.", listOf("knowledge cannot measure", "create numeric metric forbidden")),
            snippet("kp2-snippet-roi-warning", "ROI warning explanation", "Explain that graph/plot selection is uncertain, list missing evidence, and request autonomous retry or assisted review.", listOf("ROI warning")),
            snippet("kp2-snippet-calibration-invalid", "Calibration invalid warning", "Explain that calibration is invalid because required axis/tick anchors or residual checks failed.", listOf("calibration invalid warning")),
            snippet("kp2-snippet-sparse-trace", "Sparse trace warning", "Explain sparse trace as incomplete signal coverage and keep downstream peak metrics review-grade.", listOf("sparse trace warning")),
            snippet("kp2-snippet-peak-overlap", "Peak overlap warning", "Explain overlapping peaks as shared signal requiring review of boundaries and area caveats.", listOf("peak overlap warning")),
            snippet("kp2-snippet-shoulder-peak", "Shoulder peak warning", "Explain shoulder peaks as partially resolved features requiring review-grade labeling.", listOf("shoulder peak warning")),
            snippet("kp2-snippet-ocr-ambiguity", "OCR ambiguity warning", "Explain ambiguous OCR with crop provenance and avoid converting uncertain text into metrics.", listOf("OCR ambiguity warning")),
            snippet("kp2-snippet-vlm-disagreement", "VLM disagreement warning", "Explain ML Kit/VLM disagreement as semantic review evidence, not a final measurement.", listOf("VLM disagreement warning")),
            snippet("kp2-snippet-diagnostic-only", "Diagnostic-only report explanation", "Explain that missing gates block release-quality reporting and list evidence needed to advance.", listOf("diagnostic-only explanation")),
        ) + (10..40).map { carbon -> normalAlkaneStub(carbon) }

    private fun glossary(id: String, label: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.GLOSSARY_TERM, label, text, aliases, listOf("chromatography", "glossary"))

    private fun msTerm(id: String, label: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.MASS_SPECTROMETRY_TERM, label, text, aliases, listOf("GC/MS", "mass spectrometry"))

    private fun ionTerm(id: String, label: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.ION_CHANNEL_TERM, label, text, aliases, listOf("ion channel", "text classification"))

    private fun axisTerm(id: String, label: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.AXIS_TERM, label, text, aliases, listOf("axis", "geometry"))

    private fun retentionRule(id: String, label: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.RETENTION_INDEX_RULE, label, text, aliases, listOf("retention index", "Kovats"))

    private fun pattern(id: String, label: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.KNOWN_PATTERN, label, text, aliases, listOf("pattern", "n-alkane"))

    private fun compoundClass(id: String, label: String, text: String, aliases: List<String>) =
        entry(
            id = id,
            type = KnowledgeEntryType.COMPOUND_CLASS,
            label = label,
            text = text,
            aliases = aliases,
            tags = listOf("chemical class", "domain dictionary"),
            sourceRefIds = listOf("chromalab-curated-v2", "chebi-cc-by-4"),
            licenseStatus = KnowledgeLicenseStatus.INTERNAL_CURATED,
            trustTier = KnowledgeSourceTrustTier.INTERNAL_CURATED,
        )

    private fun textRule(id: String, label: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.TEXT_CLASSIFICATION_RULE, label, text, aliases, listOf("OCR", "VLM", "text classification"))

    private fun caveat(id: String, label: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.REPORT_CAVEAT, label, text, aliases, listOf("report caveat", "release gate"))

    private fun safety(id: String, label: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.SAFETY_BOUNDARY, label, text, aliases, listOf("safety", "forbidden use"))

    private fun snippet(id: String, label: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.PROMPT_SNIPPET, label, text, aliases, listOf("prompt", "warning explanation"))

    private fun normalAlkaneStub(carbon: Int): KnowledgeEntry =
        entry(
            id = "kp2-compound-stub-n-c$carbon-alkane",
            type = KnowledgeEntryType.COMPOUND_REFERENCE_STUB,
            label = "n-C$carbon alkane",
            text = "n-C$carbon alkane is a straight-chain saturated hydrocarbon reference-label stub with formula C${carbon}H${carbon * 2 + 2}; it contains no measured RT, spectrum, RI record, or sample identification.",
            aliases = listOf("C$carbon", "n-C$carbon", normalAlkaneName(carbon), "C$carbon n-alkane"),
            tags = listOf("n-alkane", "compound reference stub", "C10-C40"),
            sourceRefIds = listOf("chromalab-curated-v2", "chebi-cc-by-4"),
            licenseStatus = KnowledgeLicenseStatus.INTERNAL_CURATED,
            trustTier = KnowledgeSourceTrustTier.INTERNAL_CURATED,
        )

    private fun entry(
        id: String,
        type: KnowledgeEntryType,
        label: String,
        text: String,
        aliases: List<String>,
        tags: List<String>,
        sourceRefIds: List<String> = listOf("chromalab-curated-v2"),
        licenseStatus: KnowledgeLicenseStatus = KnowledgeLicenseStatus.INTERNAL_CURATED,
        trustTier: KnowledgeSourceTrustTier = KnowledgeSourceTrustTier.INTERNAL_CURATED,
    ): KnowledgeEntry =
        KnowledgeEntry(
            entryId = id,
            version = CHROMALAB_KNOWLEDGE_PACK_VERSION_V2,
            type = type,
            canonicalLabel = label,
            language = "en",
            shortText = text,
            aliases = aliases,
            keywords = tags + aliases,
            sourceRefIds = sourceRefIds,
            licenseStatus = licenseStatus,
            trustTier = trustTier,
            confidence = 1f,
            lastReviewed = "2026-05-20",
            tags = tags,
            policy = KnowledgeUsePolicy(
                allowedUse = listOf(
                    "semantic_explanation",
                    "text_classification",
                    "warning_explanation",
                    "prompt_grounding",
                    "report_caveat",
                    "synonym_lookup",
                ),
                forbiddenUse = listOf(
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
                    "test_fixture_only",
                ),
            ),
        )

    private fun normalAlkaneName(carbon: Int): String =
        when (carbon) {
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
            31 -> "n-Hentriacontane"
            32 -> "n-Dotriacontane"
            33 -> "n-Tritriacontane"
            34 -> "n-Tetratriacontane"
            35 -> "n-Pentatriacontane"
            36 -> "n-Hexatriacontane"
            37 -> "n-Heptatriacontane"
            38 -> "n-Octatriacontane"
            39 -> "n-Nonatriacontane"
            40 -> "n-Tetracontane"
            else -> "n-C$carbon alkane"
        }

    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }
}

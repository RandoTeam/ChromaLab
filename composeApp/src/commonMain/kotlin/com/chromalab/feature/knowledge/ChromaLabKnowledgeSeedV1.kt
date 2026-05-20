package com.chromalab.feature.knowledge

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ChromaLabKnowledgeSeedV1 {
    val pack: KnowledgePackVersion = KnowledgePackVersion(
        title = "ChromaLab Knowledge Seed v1",
        description = "Small curated offline-first seed for chromatogram terminology, text classification rules, report caveats, and safe prompt snippets.",
        sourceRefs = listOf(
            KnowledgeSourceRef(
                sourceId = "chromalab-curated-v1",
                label = "ChromaLab curated chromatogram analysis rules",
                citation = "Internal conservative rules derived from ChromaLab Phase 0-6 release gates.",
                license = "Project internal",
                notes = "Used for product safety boundaries and report caveats; not a chemical database.",
            ),
            KnowledgeSourceRef(
                sourceId = "nist-webbook-srd69",
                label = "NIST Chemistry WebBook, SRD 69",
                url = "https://webbook.nist.gov/chemistry/",
                citation = "NIST Chemistry WebBook, NIST Standard Reference Database 69.",
                license = "NIST Standard Reference Data Program terms apply",
                notes = "Referenced for general source policy and retention-index context; no bulk data is bundled here.",
            ),
            KnowledgeSourceRef(
                sourceId = "nist-gc-ri",
                label = "NIST Chemistry WebBook: Gas Chromatographic Retention Data",
                url = "https://webbook.nist.gov/chemistry/gc-ri/",
                citation = "NIST gas chromatographic retention data documentation.",
                license = "NIST Standard Reference Data Program terms apply",
                notes = "Used only for the retention-index caveat that same-method n-alkane references are required.",
            ),
            KnowledgeSourceRef(
                sourceId = "w3c-prov-o",
                label = "W3C PROV-O",
                url = "https://www.w3.org/TR/prov-o/",
                citation = "W3C Provenance Ontology recommendation.",
                license = "W3C document license",
                notes = "Used for provenance vocabulary and audit-trail expectations.",
            ),
        ),
        entries = entries(),
    )

    val seedJson: String
        get() = json.encodeToString(pack)

    private fun entries(): List<KnowledgeEntry> = listOf(
        glossary("kp-glossary-rt", "Retention time (RT) is the calibrated time coordinate for a chromatographic peak apex.", listOf("RT", "retention time", "peak time")),
        glossary("kp-glossary-abundance", "Abundance is the detector signal magnitude plotted on the Y axis; it is not a compound identity.", listOf("abundance", "intensity", "counts")),
        glossary("kp-glossary-tic", "A total ion chromatogram (TIC) summarizes total ion current over time.", listOf("TIC", "total ion chromatogram")),
        glossary("kp-glossary-eic", "An extracted ion chromatogram (EIC) shows signal for a selected ion or m/z window.", listOf("EIC", "extracted ion chromatogram")),
        glossary("kp-glossary-sim", "Selected ion monitoring (SIM) tracks one or more selected ions/channels over time.", listOf("SIM", "selected ion monitoring")),
        glossary("kp-glossary-mz", "m/z is mass-to-charge ratio and describes an ion/channel label, not retention time.", listOf("m/z", "mass-to-charge", "mass charge")),
        glossary("kp-glossary-ion-channel", "Ion/channel text describes the chromatogram channel such as Ion 71.00 (70.70 to 71.70).", listOf("ion", "channel", "ion range")),
        glossary("kp-glossary-baseline", "Baseline is the local signal background used by deterministic integration; it must not be invented by a model.", listOf("baseline", "background")),
        glossary("kp-glossary-noise", "Noise is local signal variation used for review and S/N evidence when measured.", listOf("noise", "signal noise")),
        glossary("kp-glossary-sn", "S/N is signal-to-noise ratio and requires measured signal and noise evidence.", listOf("S/N", "snr", "signal-to-noise")),
        glossary("kp-glossary-fwhm", "FWHM is full width at half maximum and must come from measured trace/peak evidence.", listOf("FWHM", "full width at half maximum")),
        glossary("kp-glossary-area", "Peak area is the deterministic integration result over accepted boundaries.", listOf("area", "peak area")),
        glossary("kp-glossary-area-percent", "Area percent is a derived report value based on measured/integrated areas.", listOf("area percent", "area %", "relative area")),
        glossary("kp-glossary-peak-apex", "Peak apex is the local maximum point of a chromatographic peak on the extracted trace.", listOf("apex", "peak apex", "local maximum")),
        glossary("kp-glossary-shoulder", "A shoulder peak is a partially resolved peak adjacent to or riding on another peak.", listOf("shoulder", "shoulder peak")),
        glossary("kp-glossary-overlap", "Overlap or coelution means nearby peaks share signal and require review-grade evidence.", listOf("overlap", "coelution", "co-elution")),
        retentionRule("kp-ri-kovats", "Kovats / retention index values require valid reference-series evidence and cannot be inferred from the knowledge pack alone.", listOf("Kovats", "retention index", "RI")),
        retentionRule("kp-ri-n-alkane-reference", "n-alkane reference series entries define the RI scale, but measured reference retention times must come from the same method/run family.", listOf("n-alkane reference series", "normal alkane", "paraffin reference")),
        chemical("kp-chemical-n-alkane-c10-c40", "n-alkane C10-C40 naming follows the straight-chain saturated hydrocarbon pattern CnH2n+2; this is a naming pattern, not an identification.", listOf("C10", "C40", "n-alkane", "normal alkane")),
        chemical("kp-compound-class-hydrocarbons", "Hydrocarbon classes can support report grouping only when ion/channel, retention, and evidence gates support the claim.", listOf("hydrocarbon", "alkane", "alkylbenzene")),
        ionTerm("kp-ion-channel-terminology", "Common GC/MS channel labels include TIC, EIC, XIC, SIM, Ion, and m/z; numeric ion ranges are channel metadata.", listOf("GC/MS labels", "Ion 71.00", "XIC", "SIM")),
        textRule("kp-rule-ion-range-title-channel", "Text like Ion 71.00 (70.70 to 71.70) is TITLE_OR_CHANNEL, not PEAK_ANNOTATION; numeric values in the mass range are not RT labels.", listOf("Ion 71.00 (70.70 to 71.70)", "ion range is not peak RT", "TITLE_OR_CHANNEL")),
        textRule("kp-rule-peak-label-signal-verification", "A numeric text region can become PEAK_ANNOTATION only when its location is compatible with a plot annotation and local signal verification confirms a nearby peak.", listOf("peak label", "5.610", "local signal verification", "plot annotation")),
        textRule("kp-rule-tick-label-linkage", "Tick labels require deterministic tick-position linkage; OCR text without tick geometry cannot calibrate an axis.", listOf("tick label", "tick-position linkage", "axis tick OCR")),
        textRule("kp-rule-axis-label-not-peak", "Axis labels such as Abundance or Retention Time are AXIS_LABEL text and must not become peak labels.", listOf("axis label", "Abundance", "Retention Time")),
        caveat("kp-caveat-no-kovats-without-reference", "Do not report Kovats/RI as calculated unless valid same-method n-alkane reference-series retention times are present.", listOf("no Kovats without reference series", "RI caveat")),
        caveat("kp-caveat-no-compound-without-evidence", "Do not assign compound names without explicit evidence such as validated library/spectrum/method/RI/user confirmation.", listOf("compound assignment requires explicit evidence", "no compound assignment")),
        caveat("kp-caveat-release-needs-calibration", "No release-quality report is allowed without valid calibration evidence and a complete runtime evidence package.", listOf("release-quality requires calibration evidence", "calibration evidence")),
        caveat("kp-caveat-no-metric-without-evidence", "No peak metric claim is allowed without linked trace, calibration, and peak evidence.", listOf("no peak metric without evidence", "trace evidence")),
        safety("kp-safety-knowledge-cannot-measure", "Knowledge can explain and classify; it cannot measure RT, height, area, FWHM, S/N, baseline, Kovats, or integration boundaries.", listOf("knowledge cannot measure", "numeric metrics forbidden")),
        promptSnippet("kp-prompt-warning-explanation", "Explain warnings using cited entries only. If evidence is missing, say what is missing and keep the result REVIEW/DIAGNOSTIC.", listOf("warning explanation template", "unsupported claim rejection")),
    )

    private fun glossary(id: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.GLOSSARY_TERM, text, aliases, listOf("chromatography", "glossary"))

    private fun textRule(id: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.TEXT_CLASSIFICATION_RULE, text, aliases, listOf("text classification", "OCR", "VLM"))

    private fun caveat(id: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.REPORT_CAVEAT, text, aliases, listOf("report caveat", "release gate"))

    private fun retentionRule(id: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.RETENTION_INDEX_RULE, text, aliases, listOf("retention index", "Kovats"))

    private fun chemical(id: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.COMPOUND_CLASS, text, aliases, listOf("chemical dictionary", "domain dictionary"))

    private fun ionTerm(id: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.ION_CHANNEL_TERM, text, aliases, listOf("ion channel", "GC/MS"))

    private fun safety(id: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.SAFETY_BOUNDARY, text, aliases, listOf("safety", "forbidden use"))

    private fun promptSnippet(id: String, text: String, aliases: List<String>) =
        entry(id, KnowledgeEntryType.PROMPT_SNIPPET, text, aliases, listOf("prompt", "template"))

    private fun entry(
        id: String,
        type: KnowledgeEntryType,
        text: String,
        aliases: List<String>,
        keywords: List<String>,
    ): KnowledgeEntry =
        KnowledgeEntry(
            entryId = id,
            type = type,
            shortText = text,
            aliases = aliases,
            keywords = keywords,
            sourceRefIds = listOf("chromalab-curated-v1"),
            policy = KnowledgeUsePolicy(
                allowedUse = listOf(
                    "semantic_explanation",
                    "text_classification",
                    "warning_explanation",
                    "prompt_grounding",
                    "report_caveat",
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
                ),
            ),
        )

    private val json = Json {
        encodeDefaults = true
        prettyPrint = true
    }
}

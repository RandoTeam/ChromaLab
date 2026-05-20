# ChromaLab Local Knowledge Pack

Phase 6B adds the local retrieval/citation layer for VLM/OCR semantic grounding. Earlier
Phase 7.1 defines the offline knowledge-pack schema. Phase 7.2 adds the first conservative
GC-MS EI `m/z 92` and alkylbenzene-oriented reference data. Phase 7.3 adds n-paraffin
reference-series support for Kovats calculations. Phase 7.4 expands the built-in
chromatogram/ion coverage and wires the local knowledge into structured report
interpretation.

The goal is to keep chemical interpretation conservative and auditable:

- numeric results still come from graph preparation, calibration, curve extraction, and `CalculationEngine`;
- the knowledge pack provides local facts, labels, ranges, and interpretation notes;
- every fact can carry evidence type, confidence, and source IDs;
- dangling references or invalid ranges are rejected before interpretation uses the pack.
- VLM/report explanations cite retrieved `used_entry_ids`;
- knowledge entries can explain/classify but cannot create numeric chromatographic metrics.

## Schema Location

```text
composeApp/src/commonMain/kotlin/com/chromalab/feature/knowledge/LocalKnowledgePackModels.kt
composeApp/src/commonMain/kotlin/com/chromalab/feature/knowledge/LocalKnowledgePackValidator.kt
composeApp/src/commonMain/kotlin/com/chromalab/feature/knowledge/ChromaLabBaseKnowledgePack.kt
composeApp/src/commonMain/kotlin/com/chromalab/feature/knowledge/KnowledgeRetrievalModels.kt
composeApp/src/commonMain/kotlin/com/chromalab/feature/knowledge/KnowledgeRetrievalEngine.kt
composeApp/src/commonMain/kotlin/com/chromalab/feature/knowledge/ChromaLabKnowledgeSeedV1.kt
composeApp/src/commonMain/kotlin/com/chromalab/feature/knowledge/KovatsIndexCalculator.kt
composeApp/src/commonMain/kotlin/com/chromalab/feature/reports/ReportKnowledgeInterpreter.kt
docs/knowledge/chromalab_knowledge_seed_v1.json
```

## Phase 6B Retrieval Layer

The Phase 6B seed and retrieval layer uses:

- `KnowledgeEntry`
- `KnowledgeEntryType`
- `KnowledgeSourceRef`
- `KnowledgeUsePolicy`
- `KnowledgeSearchQuery`
- `KnowledgeSearchResult`
- `KnowledgeRetrievalContext`
- `KnowledgePackVersion`

V1 search is local/offline lexical BM25-style retrieval. Android SQLite FTS5 / Room is the
preferred persistent implementation path for a future larger pack.

## Top-Level Groups

`LocalKnowledgePack` contains:

- `sources`: curated, literature, instrument-method, or user-provided provenance records.
- `chromatogramTypes`: analysis type and mode contracts such as GC-MS EIC, expected axis units, supported ions, and target compound classes.
- `ionFragments`: diagnostic ion/channel definitions with m/z windows, ionization mode, related ions, interpretation notes, and cautions.
- `compoundClasses`: chemical classes, formula patterns, carbon-number ranges, diagnostic ions, and assignment cautions.
- `carbonNumberSeries`: homologous-series contracts for C-number ranges, retention order, naming templates, and expected ions.
- `kovatsLibraries`: reference libraries for retention-index lookup with method context, reference series, and per-compound Kovats entries.

## Validator Contract

`LocalKnowledgePackValidator` checks:

- required IDs and labels are non-blank;
- IDs are unique within each top-level group;
- references point to known sources, ions, compound classes, chromatogram types, or homologous series;
- m/z values and carbon numbers are positive;
- integer and double ranges are ordered;
- Kovats entries provide either a single index or an index range.

Empty Kovats libraries are allowed as schema drafts, but they produce warnings.

## Base Pack Data

`ChromaLabBaseKnowledgePack` is the first built-in offline knowledge pack.

It currently includes:

- NIST Chemistry WebBook source records for SRD 69, toluene, ethylbenzene, m-xylene, p-xylene, and o-xylene.
- GC-MS TIC, EIC, XIC, and SIM chromatogram type contracts.
- The `ei-mz-92` channel with the `91.70..92.70 m/z` window.
- Related channels used by the current bench/reference images: `m/z 57`, `71`, `83`,
  `91`, `191`, `217`, `218`, `198.0315`, `326`, `360`, and `394`.
- A conservative `monoalkylbenzenes` compound class.
- Conservative `n-paraffins`, `petroleum-biomarkers`, and `method-targeted-extracts`
  classes.
- A `monoalkylbenzene-carbon-series` candidate series for C7-C30.
- A non-polar temperature-ramp Kovats seed library for C7-C8 alkylbenzenes.
- A `n-paraffins` compound class for straight-chain alkane references.
- A `n-paraffin-reference-series` for C7-C30.
- A C7-C30 Kovats reference scale where each n-paraffin anchor has RI `100 * carbonNumber`.

The built-in pack is intentionally conservative:

- `m/z 92` is compatible with the molecular ion of toluene, but it is not enough to assign a peak as toluene.
- Alkylbenzene labels are candidate labels until retention index, adjacent ions, full spectrum, or user/library confirmation support the assignment.
- Xylene and ethylbenzene isomers are kept as separate reference entries, but their shared formula and overlapping RI behavior must be treated as an ambiguity, not a resolved identity.
- The C7-C8 RI records are broad seed ranges from NIST WebBook literature records; they are suitable for candidate ranking and warnings, not final release-quality naming by themselves.
- The n-paraffin built-in entries define the RI scale only. They do not provide measured reference retention times for a user's chromatographic method.
- TIC/EIC/XIC/SIM channel matches can produce class-level report hypotheses, but never
  final peak names without retention-index, spectrum/library, method-target, or user
  evidence.
- Exact-mass and high-mass XIC channels are reported as method-targeted channels until
  the instrument method or target list is attached.

## Kovats Calculation Support

`KovatsIndexCalculator` supports two formulas:

- `VAN_DEN_DOOL_KRATZ_LINEAR` for temperature-programmed GC.
- `KOVATS_ISOTHERMAL_LOG` for isothermal Kovats calculations.

The calculator requires:

- positive finite target retention time;
- at least two n-paraffin reference retention times;
- reference retention times that increase with carbon number;
- the adjacent bracketing n-paraffins that elute immediately before and after the target peak.

If the measured reference series is missing, non-monotonic, out of range, or lacks adjacent
bracketing references, the calculator returns a non-calculable status instead of estimating a weak
Kovats value.

## Report Integration

`ReportKnowledgeInterpreter` maps report identification metadata to local knowledge:

- chromatogram mode (`TIC`, `EIC`, `XIC`, `SIM`);
- extracted ion/channel values such as `Ion 92.00`, `m/z 92.00`, and comma-decimal XIC labels;
- candidate compound classes and source/caution notes.

The report mapper may now show a local-knowledge class hypothesis and provenance notes.
It still keeps Kovats status as `NOT_CALCULATED` when same-method reference retention
times are absent. The built-in n-paraffin RI scale is shown as local context, not as fake
measured retention-time data.

## Source Links

- NIST Chemistry WebBook, SRD 69: <https://webbook.nist.gov/chemistry/>
- NIST GC retention data: <https://webbook.nist.gov/chemistry/gc-ri/>
- Toluene: <https://webbook.nist.gov/cgi/cbook.cgi?ID=C108883>
- Ethylbenzene: <https://webbook.nist.gov/cgi/cbook.cgi?ID=C100414>
- m-Xylene: <https://webbook.nist.gov/cgi/cbook.cgi?ID=C108383>
- p-Xylene: <https://webbook.nist.gov/cgi/cbook.cgi?ID=C106423>
- o-Xylene: <https://webbook.nist.gov/cgi/cbook.cgi?ID=C95476>

## Phase Boundaries

Phase 7.4 intentionally does not:

- assign compounds to peaks;
- generate warning rules for co-elution, contamination, weak baseline, weak crop, or unsupported runtimes;
- redesign the final mobile report UI;
- replace measured reference retention times with built-in RI anchors.

Those are later Phase 7 subphases.

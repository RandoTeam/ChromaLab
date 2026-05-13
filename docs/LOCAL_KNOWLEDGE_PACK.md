# ChromaLab Local Knowledge Pack

Phase 7.1 defines the offline knowledge-pack schema. Phase 7.2 adds the first conservative
GC-MS EI `m/z 92` and alkylbenzene-oriented reference data.

The goal is to keep chemical interpretation conservative and auditable:

- numeric results still come from graph preparation, calibration, curve extraction, and `CalculationEngine`;
- the knowledge pack provides local facts, labels, ranges, and interpretation notes;
- every fact can carry evidence type, confidence, and source IDs;
- dangling references or invalid ranges are rejected before interpretation uses the pack.

## Schema Location

```text
composeApp/src/commonMain/kotlin/com/chromalab/feature/knowledge/LocalKnowledgePackModels.kt
composeApp/src/commonMain/kotlin/com/chromalab/feature/knowledge/LocalKnowledgePackValidator.kt
composeApp/src/commonMain/kotlin/com/chromalab/feature/knowledge/ChromaLabBaseKnowledgePack.kt
```

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
- A `gc-ms-ei-eic` chromatogram type for GC-MS EI extracted ion chromatograms.
- The `ei-mz-92` channel with the `91.70..92.70 m/z` window.
- The related `ei-mz-91` support channel.
- A conservative `monoalkylbenzenes` compound class.
- A `monoalkylbenzene-carbon-series` candidate series for C7-C30.
- A non-polar temperature-ramp Kovats seed library for C7-C8 alkylbenzenes.

The built-in pack is intentionally conservative:

- `m/z 92` is compatible with the molecular ion of toluene, but it is not enough to assign a peak as toluene.
- Alkylbenzene labels are candidate labels until retention index, adjacent ions, full spectrum, or user/library confirmation support the assignment.
- Xylene and ethylbenzene isomers are kept as separate reference entries, but their shared formula and overlapping RI behavior must be treated as an ambiguity, not a resolved identity.
- The C7-C8 RI records are broad seed ranges from NIST WebBook literature records; they are suitable for candidate ranking and warnings, not final release-quality naming by themselves.

## Source Links

- NIST Chemistry WebBook, SRD 69: <https://webbook.nist.gov/chemistry/>
- Toluene: <https://webbook.nist.gov/cgi/cbook.cgi?ID=C108883>
- Ethylbenzene: <https://webbook.nist.gov/cgi/cbook.cgi?ID=C100414>
- m-Xylene: <https://webbook.nist.gov/cgi/cbook.cgi?ID=C108383>
- p-Xylene: <https://webbook.nist.gov/cgi/cbook.cgi?ID=C106423>
- o-Xylene: <https://webbook.nist.gov/cgi/cbook.cgi?ID=C95476>

## Phase Boundaries

Phase 7.2 intentionally does not:

- calculate Kovats indices;
- assign compounds to peaks;
- generate warning rules for co-elution, contamination, weak baseline, weak crop, or unsupported runtimes;
- change report rendering or UI.

Those are later Phase 7 subphases.

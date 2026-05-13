# ChromaLab Local Knowledge Pack

Phase 7.1 defines the offline knowledge-pack schema. It does not add production chemistry facts yet.

The goal is to keep chemical interpretation conservative and auditable:

- numeric results still come from graph preparation, calibration, curve extraction, and `CalculationEngine`;
- the knowledge pack provides local facts, labels, ranges, and interpretation notes;
- every fact can carry evidence type, confidence, and source IDs;
- dangling references or invalid ranges are rejected before interpretation uses the pack.

## Schema Location

```text
composeApp/src/commonMain/kotlin/com/chromalab/feature/knowledge/LocalKnowledgePackModels.kt
composeApp/src/commonMain/kotlin/com/chromalab/feature/knowledge/LocalKnowledgePackValidator.kt
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

## Phase Boundaries

This phase intentionally does not:

- add the real `m/z 92` alkylbenzene data;
- calculate Kovats indices;
- assign compounds to peaks;
- generate warning rules for co-elution, contamination, weak baseline, weak crop, or unsupported runtimes;
- change report rendering or UI.

Those are later Phase 7 subphases.

# Phase 6 Unblock Research

Date: 2026-05-20

Scope: unblock Phase 6 runtime evidence, VLM/OCR boundary enforcement, Android model budget policy, and Knowledge Pack licensing alignment. Model knowledge is treated as outdated; implementation decisions are based on current official documentation, standards, and maintained source references.

## Source Quality Triage

| Source | Type | Relevance | Decision affected | Adopt / reject |
|---|---|---|---|---|
| Android LiteRT custom ML docs: https://developer.android.com/ai/custom | Official Android docs | Current Android on-device inference guidance. | Keep VLM calls bounded, on-device, profileable, and mode-aware. | Adopt for model-runtime budget policy. |
| ML Kit Text Recognition v2 Android: https://developers.google.com/ml-kit/vision/text-recognition/v2/android | Official Google docs | Current on-device text-recognition API and packaging guidance. | Preserve ML Kit/local OCR as deterministic text source, then use VLM only as crop fallback/classifier. | Adopt for OCR crop provenance policy. |
| SQLite FTS5 docs: https://www.sqlite.org/fts5.html | Official SQLite docs | Offline lexical retrieval and BM25 behavior. | Keep Knowledge Pack retrieval local/offline-first; defer vector search. | Adopt for future local retrieval packaging. |
| W3C PROV-O: https://www.w3.org/TR/prov-o/ | W3C Recommendation | Provenance vocabulary for entities, activities, and derivation. | Require evidence packages to record model task, crop, runtime profile, source refs, and user/model provenance. | Adopt for provenance structure. |
| PubChem PUG REST/PUG View docs: https://pubchem.ncbi.nlm.nih.gov/docs/pug-rest and https://pubchem.ncbi.nlm.nih.gov/docs/pug-view | Official PubChem docs | Programmatic access and contributor-attributed data. | Do not bundle PubChem-derived records without contributor/source review. | Link/API-reviewed only for now. |
| NIST Chemistry WebBook / SRD 69: https://webbook.nist.gov/ and https://www.nist.gov/programs-projects/nist-chemistry-webbook | Official NIST pages | WebBook is Standard Reference Data with spectra/GC data and NIST terms. | Treat NIST/WebBook/AMDIS as restricted link-only until explicit licensing review. | Do not bundle derived data in app seed. |
| ChEBI downloads: https://www.ebi.ac.uk/chebi/downloadsForward.do | Official EBI download page | Public chemistry ontology candidate. | Keep ChEBI as attribution-required future source; no bulk import in Phase 6 unblock. | Future candidate only. |
| OPSIN project/source references | Maintained/open source project references | Optional chemical-name normalization. | Builder-side enrichment only; never compound-identification proof. | Future optional builder step only. |

## Decisions

- VLM local crop OCR must run under the task-specific structured contract and bounded timeout.
- `parsed_retention_time` is treated as a forbidden numeric field in VLM JSON. VLM may copy visible text but must not provide measured RT or peak metrics.
- Runtime evidence packages must preserve crop provenance, stage judge rows, rejected forbidden fields, and model runtime profiles whenever VLM-backed peak-label evidence is present.
- Knowledge Pack entries may explain, classify, and provide caveats, but must declare `NOT_MEASUREMENT` claim scope and cannot fabricate measured RT, height, area, FWHM, S/N, baseline, Kovats, calibration, or integration.
- Restricted sources such as NIST/WebBook/AMDIS remain link-only and non-bundleable until licensing review clears a specific source transformation.

## Not Adopted

- Weak blogs, marketing benchmark claims, and uncited VLM capability claims were not used.
- Cloud retrieval was not introduced.
- Vector search was not introduced; the current pack size is served by deterministic local lexical retrieval and rules.
- No large chemical or spectral database dump was bundled.

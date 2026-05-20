# Phase 6 Research: Local Knowledge Pack and Retrieval Layer

Date: 2026-05-20

## Sources Used

- [SQLite FTS5](https://www.sqlite.org/fts5.html): official SQLite documentation for local full-text search, BM25 ranking, snippets, and tokenizers. Decision impact: ChromaLab v1 should use local lexical retrieval; the code added in this slice uses an equivalent in-memory BM25-style ranker and keeps Android Room FTS5 as the future persistent path.
- [AndroidX Room Fts5](https://developer.android.com/reference/androidx/room3/Fts5): official Android API reference showing current FTS5 support and bundled SQLite driver notes. Decision impact: future Android persistence can use Room/SQLite FTS5 without cloud retrieval.
- [LiteRT-LM GitHub](https://github.com/google-ai-edge/LiteRT-LM): maintained runtime repository documenting LiteRT-LM deployment and Gemma 4 support. Decision impact: Gemma-4-E4B LiteRT-LM is documented as FULL_ANALYSIS primary; E2B as FAST/fallback.
- [LiteRT Android](https://ai.google.dev/edge/litert/android): official LiteRT Android docs. Decision impact: model calls must be bounded and local; no cloud dependency is introduced by the knowledge pack.
- [PubChem Downloads](https://pubchem.ncbi.nlm.nih.gov/docs/downloads): official PubChem download/provenance guidance. Decision impact: do not bundle large external chemical databases without source and licensing review.
- [ChEBI data access](https://www.ebi.ac.uk/training/online/courses/chebi-quick-tour/retrieving-data-from-chebi/): official ChEBI training page noting downloads and CC BY 4.0 terms for training material. Decision impact: ChEBI can be evaluated later, but any bundled ontology subset needs attribution/versioning.
- [NIST Mass Spectrometry Data Center](https://chemdata.nist.gov/): official NIST MS data center page for libraries/tools. Decision impact: NIST/AMDIS/library data must not be copied as seed data; only source policy/caveats are documented.
- [W3C PROV-O](https://www.w3.org/TR/prov-o/): provenance standard. Decision impact: retrieved knowledge entries and VLM explanations must carry source IDs and used entry IDs.

## Decisions

- V1 retrieval is local/offline-first and lexical. It does not require cloud APIs or hidden remote lookup.
- The seed is small, curated, versioned, and source-referenced. It contains glossary, text classification rules, caveats, safety boundaries, and prompt snippets; it does not contain bulk PubChem, ChEBI, NIST, or AMDIS datasets.
- Knowledge can explain/classify and ground report warnings. It cannot create RT, height, area, FWHM, S/N, baseline, Kovats, calibration coefficients, integration boundaries, or compound identities.
- VLM outputs using knowledge must cite `used_entry_ids`. Missing citations produce REVIEW; forbidden uses are rejected.

## What Not To Adopt

- Do not bundle NIST/AMDIS spectral-library data or PubChem/ChEBI bulk dumps in v1.
- Do not use vector retrieval until local model size, privacy, and benchmark value are demonstrated.
- Do not let local knowledge override deterministic CV/calibration/trace/peak evidence.

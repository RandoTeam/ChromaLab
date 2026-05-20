# Knowledge Pack Data Acquisition Future

This document defines the future path for expanding the local knowledge pack without creating licensing, privacy, or scientific-claim risks.

## V1 Baseline

The current seed is curated by ChromaLab and kept small. It covers glossary terms, text classification rules, caveats, and safety boundaries.

## Candidate Future Sources

- ChEBI ontology subsets for chemical class names, subject to attribution and subset review.
- PubChem identifiers or synonyms for explicit user-provided target lists, subject to data-source provenance review.
- NIST WebBook references for user-readable source links and caveats, not bundled spectral-library data.
- User/imported method target lists when the user owns or supplies the data.

## Import Requirements

Before adding any external data:

1. Document source terms and license.
2. Store exact version/date.
3. Keep entries source-referenced.
4. Add tests for forbidden use boundaries.
5. Keep measured values separate from reference facts.
6. Do not add cloud lookup.

## Explicitly Deferred

- Bulk PubChem/ChEBI import.
- NIST/AMDIS library bundling.
- Vector retrieval.
- Automated compound assignment from knowledge alone.

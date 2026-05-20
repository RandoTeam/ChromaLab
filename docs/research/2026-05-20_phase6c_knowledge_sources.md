# Phase 6C Knowledge Sources Research

Date: 2026-05-20

## Scope

Phase 6C expands ChromaLab's local/offline Knowledge Pack for semantic grounding, report caveats, OCR/text classification, and chromatography terminology. The pack must not bundle uncontrolled external chemical databases and must never create measured chromatographic metrics.

## Source Review

| Source | URL | License / usage status | Bundle? | Transform? | Attribution | Decision |
|---|---|---:|---:|---:|---:|---|
| ChromaLab-authored glossary/rules | Internal | Project internal curated content | Yes | Yes | Internal citation | Primary seed source for v2. |
| ChEBI | https://www.ebi.ac.uk/chebi/aboutChebiForward.do | ChEBI states website data is available under CC BY 4.0 and governed by EMBL-EBI terms. | Candidate after attribution manifest | Candidate after attribution manifest | Required | Accept as future source for curated synonyms/classes, not bulk import in Phase 6C. |
| ChEBI training/download docs | https://www.ebi.ac.uk/training/online/courses/chebi-quick-tour/retrieving-data-from-chebi/ | EMBL-EBI training material indicates ChEBI can be retrieved through downloads, web services, SPARQL, and OLS. | Candidate after license snapshot | Candidate after license snapshot | Required | Useful for future connector design. |
| PubChem downloads | https://pubchem.ncbi.nlm.nih.gov/docs/downloads | PubChem is free to use and mostly downloadable, but contributor-specific licensing may apply and must be checked per data source. | No broad bundle yet | No broad transform yet | Source-specific | Accept as future API/source-reviewed metadata path only. |
| NCBI/NLM policies | https://www.ncbi.nlm.nih.gov/home/about/policies/ | NCBI places no own restriction on many molecular databases, but contributors may hold rights and NCBI cannot transfer them. | No broad bundle from this alone | No broad transform from this alone | Acknowledgment requested for public-domain pages | Require source-level license review. |
| NIST Chemistry WebBook / SRD 69 | https://webbook.nist.gov/chemistry/ | Standard Reference Data Program data; redistribution/product packaging requires explicit review. | No | No | Required if cited | Reject bundling in Phase 6C. Keep only policy/caveat references. |
| NIST GC RI page | https://webbook.nist.gov/chemistry/gc-ri/ | Useful public reference for GC retention data context, but data bundling remains license-sensitive. | No | No | Required if cited | Use only for caveats that RI requires method/reference context. |
| NIST AMDIS resources | https://chemdata.nist.gov/dokuwiki/doku.php?id=chemdata:start | AMDIS tool/docs are public, but NIST spectral library/database content is separate and license-sensitive. | No | No | Required if cited | Do not bundle AMDIS/NIST library data. |
| W3C PROV-O | https://www.w3.org/TR/prov-o/ | W3C Recommendation and document license. | Yes for references | Yes for provenance mapping | Required | Use for provenance vocabulary and audit-trail documentation. |

## Decisions Affected

- Knowledge Pack v2 uses ChromaLab-authored entries as the committed seed.
- ChEBI is listed as a future open-licensed source candidate, not imported wholesale in this task.
- PubChem is treated as source-reviewed/API-only until contributor licenses are resolved.
- NIST/AMDIS/WebBook content is not bundled; Phase 6C only records caveats and source-policy guidance.
- Any source without explicit license metadata is rejected from production bundle generation.

## What Not To Adopt

- No proprietary spectral library data.
- No blind PubChem bulk dumps.
- No NIST/AMDIS/WebBook database redistribution.
- No model-generated chemistry facts without source references.
- No scraped web pages without permission and source policy review.

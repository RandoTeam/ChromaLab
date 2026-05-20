# Phase 6C Knowledge Licensing Research

Date: 2026-05-20

## Research Question

Which chemistry and chromatography knowledge sources can ChromaLab safely bundle in a local Android Knowledge Pack, and which must remain source-reviewed, API-only, or rejected?

## Findings

| Source | Current license / policy finding | Can bundle? | Can transform? | API-only? | Rejected reason if any |
|---|---|---:|---:|---:|---|
| ChromaLab curated rules | ChromaLab-authored project content. | Yes | Yes | No | Accepted. |
| ChEBI | ChEBI About page says data is available under Creative Commons License CC BY 4.0 and EMBL-EBI terms. | Candidate | Candidate | No | Not imported wholesale until attribution and snapshot policy are implemented. |
| PubChem | PubChem downloads documentation says most data is readily downloadable, but contributor datasets can have their own license terms; users should check data source licensing. | Not yet | Not yet | Yes for reviewed lookups | Rejected for Phase 6C bulk bundling because contributor-level license review is unresolved. |
| NCBI/NLM molecular data policy | NCBI itself places no restrictions on molecular data, but submitters may have rights and NCBI cannot provide unrestricted third-party permission. | Not enough alone | Not enough alone | Yes, with source review | Requires source-level license metadata before bundling. |
| NIST Chemistry WebBook / SRD 69 | Official SRD database source. Product redistribution is not cleared by general page access. | No | No | No uncontrolled API | Rejected until explicit licensing review. |
| NIST AMDIS / mass spectral data | AMDIS tool access is public, while NIST mass spectral libraries are separate data products. | No | No | No uncontrolled API | Rejected for bundled knowledge data. |
| W3C PROV-O | W3C Recommendation; suitable for provenance concept references. | Yes, as citation/reference | Yes | No | Accepted for provenance documentation. |

## License Register Implications

- Every source definition must carry `license_status`, `can_bundle`, `can_transform`, `api_lookup_only`, and attribution flags.
- The builder must fail closed when source license metadata is missing.
- `PROPRIETARY_FORBIDDEN`, `REJECTED`, or `NEEDS_REVIEW` sources cannot produce bundled entries unless explicitly overridden by a later signed source policy update.
- NIST and PubChem may be cited in docs or used for future reviewed source connectors; they are not part of the committed seed data.

## Source Quality Triage

Priority order:

1. Official source pages and policies.
2. Maintained official APIs/download documentation.
3. Standards such as W3C PROV.
4. Peer-reviewed or authoritative scientific references.
5. Project-authored conservative rules.

Rejected:

- Weak blogs.
- Forum claims about database licensing.
- Marketing model claims.
- Any source without explicit attribution and redistribution review.

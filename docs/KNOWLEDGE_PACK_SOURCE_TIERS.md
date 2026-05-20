# Knowledge Pack Source Tiers

Date: 2026-05-20

## Tiers

| Tier | Meaning | Bundle policy |
|---|---|---|
| `TIER_0_INTERNAL_CURATED` | ChromaLab-authored conservative rules, caveats, and glossary cards. | Bundle allowed. |
| `TIER_1_OPEN_REFERENCE` | Open reference/standard/ontology with verified license and attribution. | Bundle allowed only when license permits and attribution is generated. |
| `TIER_2_OPEN_SPECTRAL_REFERENCE` | Open spectral/reference data with explicit redistribution permission. | Not used in Phase 6C-2; future use requires source-specific review. |
| `TIER_3_LINK_ONLY_RESTRICTED` | Useful official source, but data bundling/transform is not cleared. | Link/cite only, no bundled data. |
| `TIER_4_REJECTED` | Source rejected because license, provenance, or quality is insufficient. | Forbidden. |

## Current Source Mapping

- ChromaLab curated entries: `TIER_0_INTERNAL_CURATED`
- ChEBI: `TIER_1_OPEN_REFERENCE` future candidate with attribution
- W3C PROV-O: `TIER_1_OPEN_REFERENCE`
- OPSIN: `TIER_1_OPEN_REFERENCE` optional builder tool, not compound proof
- PubChem: `TIER_3_LINK_ONLY_RESTRICTED` until contributor licenses are reviewed
- NIST/AMDIS/WebBook database data: `TIER_3_LINK_ONLY_RESTRICTED` in Phase 6C-2

No `TIER_2_OPEN_SPECTRAL_REFERENCE` data is bundled yet.

## Validator Enforcement

`KnowledgePackValidator` fails closed when a bundled entry references a source that is not bundleable, has `NEEDS_REVIEW`, `API_ONLY`, `REJECTED`, or `PROPRIETARY_FORBIDDEN` license status, or uses a restricted/rejected trust tier. Restricted sources may remain in the source register for policy documentation, but they cannot back committed production entries.

Legacy v1 internal-curated sources remain valid for backward compatibility; new source definitions must use the explicit tier names above.

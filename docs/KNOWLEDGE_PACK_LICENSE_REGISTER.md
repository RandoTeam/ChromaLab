# Knowledge Pack License Register

Date: 2026-05-20

This register controls which sources may contribute to bundled ChromaLab Knowledge Packs.

| Source ID | Source | License / policy | Bundle status | Transform status | Attribution | Phase 6C decision |
|---|---|---|---|---|---|---|
| `chromalab-curated-v2` | ChromaLab curated rules and caveats | Project internal curated content | Allowed | Allowed | Internal citation | Accepted for seed v2. |
| `chebi-cc-by-4` | ChEBI | CC BY 4.0 plus EMBL-EBI terms | Candidate allowed after attribution manifest | Candidate allowed after attribution manifest | Required | Future reviewed source; no bulk import in Phase 6C. |
| `pubchem-provenance-policy` | PubChem | NCBI/NLM policies plus contributor-specific terms | Not allowed yet | Not allowed yet | Source-specific | API/source-reviewed only until contributor license review is complete. |
| `nist-srd-review-required` | NIST Chemistry WebBook / AMDIS / SRD | NIST Standard Reference Data Program terms | Not allowed | Not allowed | Required if cited | Rejected for bundled Phase 6C data. |
| `w3c-prov-o` | W3C PROV-O | W3C document license | Allowed as provenance reference | Allowed as provenance reference | Required | Accepted for provenance documentation. |
| `opsin-builder-optional` | OPSIN | MIT | Not bundled as app data | Builder-side transform candidate | Required if used | Optional name normalization only; not compound ID evidence. |
| `nist-webbook-link-only` | Legacy/base pack NIST/WebBook references | NIST SRD/WebBook terms require explicit review before bundling derived data | Link-only / not bundleable | Not allowed until reviewed | Required if cited | Retained only as restricted source metadata; no NIST-derived RI seed entries are bundled. |

## Enforcement

- Builder source definitions must include `license_status`, `can_bundle`, `can_transform`, `api_lookup_only`, and attribution flags.
- `NEEDS_REVIEW`, `REJECTED`, and `PROPRIETARY_FORBIDDEN` sources cannot produce bundled production entries.
- Sources marked `api_lookup_only` must not be silently cached or repackaged as bundled data.
- Any future source update must add a license-register row before entries are generated.

## Phase 6C Bundle Contents

Seed v2 bundles ChromaLab-authored terminology, report caveats, text-classification rules, and reference-label stubs. It does not bundle PubChem, NIST, AMDIS, WebBook, proprietary spectral libraries, or large ChEBI/PubChem dumps.

## Legacy/Base Pack Alignment

The code-level base pack is aligned with this register by adding `licenseStatus`, `trustTier`, `canBundle`, and claim-scope fields to `KnowledgeSource`, `KovatsReferenceEntry`, and `KnowledgeStatement`. Restricted sources must be non-bundleable, and local knowledge statements must declare `NOT_MEASUREMENT`. NIST/WebBook source records are kept as link-only provenance references and do not provide bundled Kovats/RI or spectral seed data.

# Knowledge Pack Source Policy

The ChromaLab Knowledge Pack must stay source-referenced, versioned, and legally reviewable.

## Accepted Source Classes

- Internal ChromaLab safety and report-gate rules.
- Official documentation.
- Maintained open-source repositories with compatible licenses.
- Peer-reviewed or authoritative scientific references.
- Standards such as W3C PROV.
- Small manually curated facts with explicit source references.

## Restricted Source Classes

- PubChem, ChEBI, NIST, AMDIS, and other chemical databases may be referenced, but bulk data must not be bundled until source terms, attribution, scope, and redistribution constraints are reviewed.
- NIST/AMDIS spectral libraries and proprietary/vendor libraries must not be copied into the app without licensing review.
- Weak blogs, uncited claims, marketing pages, and unverified benchmark posts cannot drive production behavior.

## Seed V1 Policy

`chromalab_knowledge_seed_v1.json` contains only small curated terminology, caveats, and safety rules. It deliberately excludes large compound libraries, spectral libraries, retention-index databases, and vendor datasets.

## Provenance Fields

Every entry must include:

- stable `entryId`;
- `version`;
- `type`;
- `shortText`;
- `sourceRefIds`;
- explicit `allowedUse`;
- explicit `forbiddenUse`.

## Safety Review

Any future data import must document:

- source URL;
- license/terms;
- exact subset;
- attribution requirements;
- transformation steps;
- tests proving knowledge cannot create measured chromatographic values.
# Phase 6C Source Policy Update

Date: 2026-05-20

ChromaLab Knowledge Pack sources are fail-closed. A source may contribute bundled app data only when its license, attribution, transform, and redistribution status are explicit.

## Allowed Source Categories

1. ChromaLab-authored glossary, report caveats, VLM safety boundaries, and text-classification rules.
2. Public/open licensed chemistry vocabularies after license verification and attribution manifest creation.
3. Public API-derived metadata only when source-level terms allow packaging.
4. Future user-supplied local packs with explicit provenance.
5. Small curated internal seed entries with explicit allowed and forbidden use policies.

## Forbidden Until Cleared

1. Proprietary database dumps.
2. NIST/AMDIS/WebBook data bundled blindly.
3. Scraped web data without permission.
4. Marketing or model-generated chemistry facts without source references.
5. Any source missing license or attribution review.

## Builder Rules

- `NEEDS_REVIEW`, `REJECTED`, or `PROPRIETARY_FORBIDDEN` sources cannot produce bundled entries.
- `api_lookup_only` sources may be queried only through a later explicit connector and must not be silently cached into the pack.
- Every entry must carry `allowedUse` and `forbiddenUse`.
- Every entry must forbid metric fabrication, calibration override, integration override, and unsupported compound identification.
- Knowledge may explain and classify; it cannot measure.

## Current Phase 6C Decisions

- ChromaLab internal entries are bundled.
- ChEBI is a future candidate source under CC BY 4.0 attribution, but no bulk ChEBI import is committed in Phase 6C.
- PubChem is a future source-reviewed/API path only because contributor licenses vary.
- NIST/AMDIS/WebBook database content is not bundled.

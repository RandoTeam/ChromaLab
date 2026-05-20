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

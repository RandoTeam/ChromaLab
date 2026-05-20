# Knowledge Pack Claim Policy

Date: 2026-05-20

Every `KnowledgeEntry` declares claim scopes.

## Claim Scopes

- `EXPLANATION_ONLY`
- `TEXT_CLASSIFICATION`
- `REPORT_CAVEAT`
- `RETRIEVAL_CONTEXT`
- `COMPOUND_DICTIONARY`
- `SPECTRAL_REFERENCE_LINK`
- `NOT_MEASUREMENT`

All bundled entries must include `NOT_MEASUREMENT`.

## Forbidden Claims

Knowledge entries must never create:

- measured RT;
- height;
- area;
- FWHM;
- S/N;
- baseline;
- Kovats;
- compound identification without explicit evidence.

## Interpretation

A compound dictionary entry can normalize terms and synonyms. It cannot identify a sample peak. A spectral reference link can point to reviewed source material. It cannot become spectral match evidence unless a later validated spectral workflow supplies the actual match.

## Enforcement

`KnowledgePackValidator` rejects bundled entries without `NOT_MEASUREMENT` and rejects metric-like measured values embedded in production knowledge text.

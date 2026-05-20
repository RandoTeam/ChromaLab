# VLM JSON Schema Boundaries

VLM output is accepted only through task-specific structured contracts.

## Allowed VLM Roles

- read local crop text;
- classify text regions;
- judge overlays;
- explain warnings;
- recommend bounded retries.

## Forbidden VLM Fields

If present, these fields are rejected and recorded as warnings:

- final RT;
- peak height;
- peak area;
- FWHM;
- S/N;
- baseline;
- Kovats;
- exact pixel geometry used for calculations;
- calibration coefficients.

## Enforcement

`ForbiddenVlmBoundaryPolicy` scans structured output field names and returns rejected forbidden fields. Runtime evidence validator fails any stage or crop result that accepts forbidden numeric fields.

The correct flow is:

1. VLM returns text or judgement.
2. Schema boundary rejects forbidden numeric fields.
3. Deterministic CV/OCR/math validates geometry, calibration, trace, and peaks.
4. Report provenance records whether VLM assisted semantically.


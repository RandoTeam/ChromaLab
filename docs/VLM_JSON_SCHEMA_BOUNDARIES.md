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

## Phase 6 Unblock Enforcement Matrix

| Task | Accepted VLM fields | Explicitly rejected fields | Runtime action |
|---|---|---|---|
| Local OCR crop read | `text`, `raw_text`, `normalized_text`, `text_type`, `confidence`, `warnings`, `used_entry_ids`, `decision`, `unsupported_claims`, `explanation` | `rt`, `retention_time`, `parsed_retention_time`, `height`, `area`, `fwhm`, `snr`, `baseline`, `kovats`, `x`, `y`, `bounds`, `slope`, `intercept` | Rejected fields are stored on `VisionLocalTextCropResult`, propagated to `PeakLabelEvidence`, and exported as `VlmOcrCropResult.rejectedForbiddenFields`. |
| Graph/plot/axis advisory judge | `verdict`, `confidence`, `warnings`, `retry_recommendations` plus bounded knowledge fields | final pixel coordinates, calibration coefficients, metric fields | Deterministic CV/tick geometry remains authoritative; VLM output may only recommend review/retry. |
| Trace/peak overlay judge | `verdict`, `confidence`, `warnings`, `retry_recommendations` plus bounded knowledge fields | peak metrics, trace-derived measured values, integration boundaries | Deterministic trace and peak evidence gates remain authoritative. |
| Report warning summary | `summary`, `warnings`, `confidence`, `used_entry_ids` | any measured metric, compound identification without evidence | Missing `used_entry_ids` or unsupported claims force REVIEW/REJECTED evidence. |

The local crop prompt no longer asks for `parsed_retention_time`. If an older model returns that field anyway, the boundary policy rejects it and records the rejection. Numeric-looking visible text may still be copied as OCR text; downstream deterministic crop context and signal verification decide whether it is a peak annotation candidate.

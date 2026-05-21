# Phase 9G Calibration Strategy Ensemble

## Purpose

Phase 9G replaces single-path calibration selection with deterministic strategy arbitration. The ensemble is designed to prevent a new resolver from regressing a previously valid calibration path.

## Strategies

| Strategy | Status |
| --- | --- |
| `LEGACY_TICK_LOCALIZATION` | Active. Rebuilds anchors from deterministic tick OCR and preserves the pre-9F path. |
| `AXIS_SCALE_RESOLVER` | Active. Uses Phase 9F axis-scale evidence and subreasons. |
| `OCR_LABEL_BOX_DIRECT_FIT` | Active, but label-box-only fits are downgraded to review. |
| `GRID_FRAME_PROJECTION` | Stubbed as invalid until paired grid/label evidence is available. |
| `REGULAR_SEQUENCE_FIT` | Active when monotonic OCR label sequences exist. |
| `FRAME_ENDPOINT_REVIEW_FALLBACK` | Present in contract but disabled from selection in Phase 9G until endpoint direction semantics are proven. |

## Arbitration Rules

- `VALID` beats `REVIEW`; `REVIEW` beats `INVALID`.
- Same-status candidates are ranked by score using accepted anchor count, residuals, confidence, and strategy trust.
- Title, ion, m/z, SIM, scan, and method-range text are rejected as scale labels.
- Candidates with no pixel geometry are invalid.
- The legacy path receives a regression-shield trust bonus when it remains valid/review.
- All strategy results are kept in runtime trace evidence; terminal graph failures export selected and rejected strategy ids.

## Scientific Boundaries

`E2B_BASELINE` is the supported production FAST/weaker-device model mode. It remains part of every Android acceptance rerun and is not removed or demoted to experimental-only status.

No VLM/E2B output can provide calibration pixel geometry or numeric chromatographic calibration. VLM/OCR text remains advisory unless paired with deterministic geometry and accepted by the calibration fitter. E2B may improve local crop OCR, text classification, Knowledge Pack grounded explanation, warning explanation, and overlay review warnings, but deterministic graph count, geometry, calibration coefficients, chromatographic metrics, and report gates remain primary.

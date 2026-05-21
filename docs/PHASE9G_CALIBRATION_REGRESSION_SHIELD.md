# Phase 9G Calibration Regression Shield

## Protected Regression

`white_tiger_ion71` regressed from Phase 8D `REVIEW_ONLY` with valid X/Y calibration to Phase 9F `BLOCKED` with `TICK_LOCALIZATION_FAILURE`.

## Shield Behavior

- The legacy tick-localization strategy remains available.
- The new axis-scale resolver is treated as one candidate, not the exclusive calibration path.
- Arbitration exports the selected strategy for X and Y.
- Validator output now fails graph-stage calibration packages that omit selected/rejected strategy evidence.
- Android suite summaries now include selected strategy fields and aggregate all graph failure packages rather than only the first failure package.

## Required Evidence For Future Closure

Phase 9 cannot be accepted unless Android rerun evidence shows:

- `white_tiger_ion71` no longer regresses to `BLOCKED` solely because `AxisScaleResolver` failed.
- E2B/model-enabled mode does not alter deterministic calibration strategy selection without deterministic rejection evidence.
- Remaining blocked calibration failures contain selected and rejected strategy evidence.

## E2B Baseline Shield

`E2B_BASELINE` is a supported production FAST/weaker-device mode, not an experimental-only model path. The regression shield therefore requires E2B to run beside deterministic baseline for every Android fixture rerun.

E2B is allowed to improve local crop OCR, semantic classification, Knowledge Pack grounded explanations, warning explanations, and overlay review warnings. E2B is only advisory for graph candidate quality, plotArea warning, axis/tick visibility warning, and multi-graph suspicion.

The shield fails if E2B erases deterministic graph candidates, changes graph count by itself, creates pixel coordinates, creates calibration coefficients, creates chromatographic metrics, overrides deterministic peak metrics, or identifies compounds without explicit evidence. If E2B disagrees with deterministic geometry, deterministic evidence remains primary; the run is marked `REVIEW` with disagreement evidence unless deterministic evidence independently fails.

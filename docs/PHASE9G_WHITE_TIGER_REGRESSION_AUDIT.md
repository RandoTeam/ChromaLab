# Phase 9G White Tiger Regression Audit

## Inputs

- Phase 8D successful run: `artifacts/phase8d-android-validation/white_tiger_ion71_20260520_184550/`
- Phase 9F regressed run: `artifacts/phase9f-multi-fixture-android-final/white_tiger_ion71/deterministic/white_tiger_ion71_20260521_111238/`

## Phase 8D Working Evidence

| Field | Evidence |
| --- | --- |
| Report gate | `REVIEW_ONLY` |
| Graph count | 1 |
| Runtime failure class | `VLM_SEMANTIC_LAYER_UNAVAILABLE` only |
| X calibration | `VALID`, 6 accepted anchors, residual-backed fit |
| Y calibration | `VALID`, 5 accepted anchors, residual-backed fit |
| Selected region pattern | Lower chromatogram panel / expanded graph region, not the page title area |

## Phase 9F Regression Evidence

| Field | Evidence |
| --- | --- |
| Report gate | `BLOCKED` |
| Runtime failure class | `TICK_LOCALIZATION_FAILURE` |
| Selected graphPanel | `GraphRegion(x=0, y=0, width=576, height=527)` |
| Selected plotArea | `GraphRegion(x=0, y=0, width=576, height=495)` |
| X anchors | 3 attempted anchors, contaminated by title/ion/Y-scale text |
| Y anchors | 0 usable anchors |
| Subreasons | `INSUFFICIENT_Y_ANCHORS`, `OCR_NO_NUMERIC_TEXT`, `LABEL_SEQUENCE_NON_MONOTONIC`, `TITLE_ION_TEXT_REJECTED_AS_SCALE_LABEL` |

## Root Cause

Phase 9F made the new axis-scale path effectively exclusive for the selected candidate. The resolver selected an upper page/title graph candidate and rejected or failed Y scale evidence. The previously successful legacy tick/OCR calibration path was no longer preserved as a competing candidate, so a new invalid resolver path replaced a previously valid calibration outcome.

## Phase 9G Repair Requirement

The system must run multiple deterministic calibration strategies and arbitrate them. `AxisScaleResolver` may fail or be rejected, but it must not erase a valid/review legacy calibration unless it proves that legacy fit invalid with residual, monotonicity, or forbidden-label evidence.

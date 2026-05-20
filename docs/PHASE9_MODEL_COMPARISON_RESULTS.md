# Phase 9 Model Comparison Results

Date: 2026-05-20

## Deterministic Versus Model-Enabled

| Metric | No model `192547` | E2B model-enabled `192400` | Decision |
| --- | --- | --- | --- |
| Graph count | 1 | 1 | No graph regression. |
| Report gate | REVIEW_ONLY | REVIEW_ONLY | No release overclaim. |
| Validator verdict | REVIEW | PASS | Model-enabled package validates after deferred activation. |
| Blocking issues | 0 | 0 | No critical blockers. |
| Model status | DISABLED | AVAILABLE | E2B activation verified. |
| Selected/executed model | none / none | `gemma4-e2b` / `gemma4-e2b` | Fallback model used. |
| Geometry timings | geometry pipeline about 5.3 s | geometry pipeline about 5.3 s | Deferring model load avoids geometry slowdown. |
| VLM numeric metrics | none | none | Boundary preserved. |
| Unsupported model claims | none observed | none observed | Boundary preserved. |

## Earlier Regression And Fix

The pre-fix model-enabled run `white_tiger_ion71_20260520_191649` loaded E2B before geometry. Local crop/full-image VLM calls timed out and the run terminated at Y calibration with `TICK_LOCALIZATION_FAILURE`.

The Phase 9 hardening change keeps validation fixture model activation after deterministic calibration. This makes the comparison meaningful: VLM availability is tested without letting model load or timeout disturb graphPanel, plotArea, tick, or calibration evidence.

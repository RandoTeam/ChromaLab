# Phase 8C VLM Model Availability Audit

## Context

Android validation run `white_tiger_ion71_20260520_162317` exported terminal artifacts but stopped at `IMAGE_QUALITY` with `runtimeFailureClass = VLM_MODEL_UNAVAILABLE`. The deterministic graphPanel/plotArea/axis path was not reached.

## Expected Models

| Mode | Model | Runtime | Packaging policy |
| --- | --- | --- | --- |
| `FULL_ANALYSIS` | `Gemma-4-E4B LiteRT-LM` | LiteRT-LM | Local device model; do not commit model file to repo. |
| `FAST` / fallback | `Gemma-4-E2B LiteRT-LM` | LiteRT-LM | Local device model; do not commit model file to repo. |
| Compatibility | GGUF vision pair | llama.cpp | Optional benchmark path; requires base GGUF plus separate mmproj. |

## Current Runtime Findings

| Item | Finding |
| --- | --- |
| Validation package | `com.chromalab.app.validation` installs beside production app. |
| Model assets in APK | No large VLM assets are bundled. |
| Model discovery path | `ModelManager` scans app-private `files/models/<model-id>/`. |
| Prior selected model | No usable chromatogram VLM was selected/executed in the failed run. |
| Prior failure behavior | `ProcessingFlowScreen` threw the model-required error before normalization/geometry fallback could complete. |
| Security issue found | Arbitrary imported non-builtin vision models were previously accepted for chromatogram VLM loading. Phase 8C now requires the same chromatogram allow-list for imported models. |

## Post-Fix Android Result

Rerun `white_tiger_ion71_20260520_170118`:

- model availability status: `NOT_CONFIGURED`;
- deterministic fallback stages reached: `GRAPH_SELECTION`, `GRAPH_ROI`, `AXIS_DETECTION`, `OCR_SUGGESTION`, `X_CALIBRATION`, `Y_CALIBRATION`;
- terminal class: `TICK_LOCALIZATION_FAILURE`;
- validator verdict: `FAIL`, with `package.graphs_missing` from the terminal failure report structure;
- no `package.deterministic_fallback_not_attempted` finding.

## Required Behavior

- Model diagnostics must record selected/executed model ids, backend expectation, load attempt/result, sanitized error, fallback attempt, and status.
- `VLM_MODEL_UNAVAILABLE` may block VLM semantic tasks, but it must not prevent deterministic CV graphPanel/plotArea/axis attempts.
- If graphPanel still fails after deterministic fallback, the terminal class should be `GRAPH_PANEL_FAILURE` or `CV_FALLBACK_GRAPH_PANEL_FAILURE`, not primary `VLM_MODEL_UNAVAILABLE`.

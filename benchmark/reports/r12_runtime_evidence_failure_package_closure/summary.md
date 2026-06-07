# R12 Runtime Evidence And Failure Package Closure

Verdict: `R12_RUNTIME_EVIDENCE_FAILURE_PACKAGE_CLOSED`

Production impact: `VALIDATOR_AND_AUDIT_CONTRACT_ONLY`

Cases: `16`
Fixtures: `8`
Core artifact complete: `16/16`
No-export states: `0`
Blocked runs: `4`
Blocked with graph failure package: `4/4`
Blocked missing first failing stage: `0`
Review-only runs: `12`
Release-ready runs: `0`

R12 audits evidence/export closure only. It does not change Android runtime behavior, chromatographic math, model policy, report gates, or CalculationEngine.

## Fixture Results

| Fixture | Mode | Gate | Validator | Graphs | Calibration | Trace | Peaks | Failure | Stage | Core artifacts | Failure package | Decision | Reason |
|---|---|---|---|---:|---|---|---:|---|---|---|---|---|---|
| `bench_01_mz71_screenshot_page` | deterministic | `BLOCKED` | `REVIEW` | 1 | X:VALID / Y:INVALID | MISSING | 0 | `TICK_LOCALIZATION_FAILURE` | `Y_CALIBRATION` | yes | yes | `EVIDENCE_REVIEW` | optional overlays missing with explicit reasons |
| `bench_01_mz71_screenshot_page` | E2B | `BLOCKED` | `REVIEW` | 1 | X:VALID / Y:INVALID | MISSING | 0 | `TICK_LOCALIZATION_FAILURE` | `Y_CALIBRATION` | yes | yes | `EVIDENCE_REVIEW` | optional overlays missing with explicit reasons |
| `bench_02_mz92_belyi_tigr` | deterministic | `REVIEW_ONLY` | `REVIEW` | 1 | X:INFERRED / Y:INFERRED | AVAILABLE | 1 | `GRAPH_PANEL_FAILURE` | `-` | yes | - | `EVIDENCE_COMPLETE` | core evidence/export artifacts are present |
| `bench_02_mz92_belyi_tigr` | E2B | `REVIEW_ONLY` | `PASS` | 1 | X:INFERRED / Y:INFERRED | AVAILABLE | 1 | `GRAPH_PANEL_FAILURE` | `-` | yes | - | `EVIDENCE_COMPLETE` | core evidence/export artifacts are present |
| `bench_03_small_tic_export` | deterministic | `REVIEW_ONLY` | `REVIEW` | 1 | X:INFERRED / Y:INFERRED | AVAILABLE | 3 | `PEAK_EVIDENCE_FAILURE` | `-` | yes | - | `EVIDENCE_COMPLETE` | core evidence/export artifacts are present |
| `bench_03_small_tic_export` | E2B | `REVIEW_ONLY` | `PASS` | 1 | X:INFERRED / Y:INFERRED | AVAILABLE | 3 | `PEAK_EVIDENCE_FAILURE` | `-` | yes | - | `EVIDENCE_COMPLETE` | core evidence/export artifacts are present |
| `bench_04_stacked_xic_resolution` | deterministic | `REVIEW_ONLY` | `REVIEW` | 1 | X:INFERRED / Y:INFERRED | AVAILABLE | 1 | `VLM_SEMANTIC_LAYER_UNAVAILABLE` | `-` | yes | - | `EVIDENCE_COMPLETE` | core evidence/export artifacts are present |
| `bench_04_stacked_xic_resolution` | E2B | `REVIEW_ONLY` | `PASS` | 1 | X:INFERRED / Y:INFERRED | AVAILABLE | 1 | `UNKNOWN_FAILURE` | `-` | yes | - | `EVIDENCE_COMPLETE` | core evidence/export artifacts are present |
| `bench_05_tic_plus_ions` | deterministic | `BLOCKED` | `REVIEW` | 1 | X:VALID / Y:REVIEW | MISSING | 0 | `CALIBRATION_FAILURE` | `Y_CALIBRATION` | yes | yes | `EVIDENCE_REVIEW` | optional overlays missing with explicit reasons |
| `bench_05_tic_plus_ions` | E2B | `BLOCKED` | `REVIEW` | 1 | X:VALID / Y:REVIEW | MISSING | 0 | `CALIBRATION_FAILURE` | `Y_CALIBRATION` | yes | yes | `EVIDENCE_REVIEW` | optional overlays missing with explicit reasons |
| `bench_06_photo_two_graphs_page` | deterministic | `REVIEW_ONLY` | `REVIEW` | 1 | X:INFERRED / Y:INFERRED | AVAILABLE | 1 | `VLM_SEMANTIC_LAYER_UNAVAILABLE` | `-` | yes | - | `EVIDENCE_COMPLETE` | core evidence/export artifacts are present |
| `bench_06_photo_two_graphs_page` | E2B | `REVIEW_ONLY` | `PASS` | 1 | X:INFERRED / Y:INFERRED | AVAILABLE | 1 | `UNKNOWN_FAILURE` | `-` | yes | - | `EVIDENCE_COMPLETE` | core evidence/export artifacts are present |
| `bench_07_rotated_page_photo` | deterministic | `REVIEW_ONLY` | `REVIEW` | 1 | X:INFERRED / Y:INFERRED | AVAILABLE | 1 | `VLM_SEMANTIC_LAYER_UNAVAILABLE` | `-` | yes | - | `EVIDENCE_COMPLETE` | core evidence/export artifacts are present |
| `bench_07_rotated_page_photo` | E2B | `REVIEW_ONLY` | `PASS` | 1 | X:INFERRED / Y:INFERRED | AVAILABLE | 1 | `UNKNOWN_FAILURE` | `-` | yes | - | `EVIDENCE_COMPLETE` | core evidence/export artifacts are present |
| `white_tiger_ion71` | deterministic | `REVIEW_ONLY` | `REVIEW` | 1 | X:INFERRED / Y:INFERRED | AVAILABLE | 12 | `PEAK_EVIDENCE_FAILURE` | `-` | yes | - | `EVIDENCE_COMPLETE` | core evidence/export artifacts are present |
| `white_tiger_ion71` | E2B | `REVIEW_ONLY` | `PASS` | 1 | X:INFERRED / Y:INFERRED | AVAILABLE | 12 | `PEAK_EVIDENCE_FAILURE` | `-` | yes | - | `EVIDENCE_COMPLETE` | core evidence/export artifacts are present |

## Next Required Work

- Produce Android runtime OCR anchor rows equivalent to R10/R11 benchmark rows.
- Persist or explicitly explain all graph-level crop and overlay artifacts for blocked runs.
- Keep BLOCKED runs blocked until graph/calibration/trace/peak evidence is complete.
- Keep E2B advisory-only and unable to alter graph count, calibration, trace, peaks, metrics, or report gates.

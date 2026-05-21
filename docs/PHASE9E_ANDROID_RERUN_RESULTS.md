# Phase 9E Android Rerun Results

Final artifact root: `artifacts/phase9e-multi-fixture-android-final2/`

Run command:

```powershell
.\tools\phase9b\run_android_validation_suite.ps1 `
  -SuiteId phase9e_all `
  -OutputRoot artifacts\phase9e-multi-fixture-android-final2 `
  -SummaryPrefix phase9e_final2 `
  -TimeoutSeconds 900
```

Summary files:

- `artifacts/phase9e-multi-fixture-android-final2/phase9e_final2_suite_summary.json`
- `artifacts/phase9e-multi-fixture-android-final2/phase9e_final2_suite_summary.md`
- `artifacts/phase9e-multi-fixture-android-final2/phase9e_final2_suite_summary_phase9e.md`

## Suite Outcome

| Metric | Result |
| --- | ---: |
| Fixtures | 8 |
| Modes | deterministic, model_enabled |
| Total runs | 16 |
| Exports complete | 16 |
| `REVIEW_ONLY` | 2 |
| `DIAGNOSTIC_ONLY` | 2 |
| `BLOCKED` | 12 |
| Validator `PASS` | 1 |
| Validator `REVIEW` | 13 |
| Validator `FAIL` | 2 |

## Final Table

| Fixture | Mode | Layout | Expected graphs | Detected graphs | Report graphs | X anchors | Y anchors | X cal | Y cal | Gate | Validator | Failure | Subreason | Decision |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- | --- | --- | --- | --- | --- |
| `white_tiger_ion71` | deterministic | `DENSE_PEAK_SINGLE_AXIS` | 1 | 1 | 0 | 1 | 0 | `INVALID` | `INVALID` | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `INSUFFICIENT_X_ANCHORS`; `INSUFFICIENT_Y_ANCHORS`; `OCR_NO_NUMERIC_TEXT` | BLOCKED |
| `white_tiger_ion71` | model_enabled | `DENSE_PEAK_SINGLE_AXIS` | 1 | 1 | 0 | 1 | 0 | `INVALID` | `INVALID` | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `INSUFFICIENT_X_ANCHORS`; `INSUFFICIENT_Y_ANCHORS`; `OCR_NO_NUMERIC_TEXT` | BLOCKED |
| `bench_01_mz71_screenshot_page` | deterministic | `DENSE_PEAK_SINGLE_AXIS` | 2 | 1 | 0 | 5 | 0 | `REVIEW` | `INVALID` | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `NON_MONOTONIC_TICK_VALUES`; `INSUFFICIENT_Y_ANCHORS`; `OCR_NO_NUMERIC_TEXT`; `HIGH_RESIDUALS` | BLOCKED |
| `bench_01_mz71_screenshot_page` | model_enabled | `DENSE_PEAK_SINGLE_AXIS` | 2 | 1 | 0 | 5 | 0 | `REVIEW` | `INVALID` | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `NON_MONOTONIC_TICK_VALUES`; `INSUFFICIENT_Y_ANCHORS`; `OCR_NO_NUMERIC_TEXT`; `HIGH_RESIDUALS` | BLOCKED |
| `bench_02_mz92_belyi_tigr` | deterministic | `DENSE_PEAK_SINGLE_AXIS` | 1 | 1 | 0 | 0 | 0 | `INVALID` | `INVALID` | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `TICK_MARKS_MISSING`; `OCR_NO_NUMERIC_TEXT`; `INSUFFICIENT_X_ANCHORS`; `INSUFFICIENT_Y_ANCHORS` | BLOCKED |
| `bench_02_mz92_belyi_tigr` | model_enabled | `DENSE_PEAK_SINGLE_AXIS` | 1 | 1 | 0 | 0 | 0 | `INVALID` | `INVALID` | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `TICK_MARKS_MISSING`; `OCR_NO_NUMERIC_TEXT`; `INSUFFICIENT_X_ANCHORS`; `INSUFFICIENT_Y_ANCHORS` | BLOCKED |
| `bench_03_small_tic_export` | deterministic | `SINGLE_TRACE_SINGLE_AXIS` | 1 | 1 | 1 | n/a | n/a | n/a | n/a | `REVIEW_ONLY` | `REVIEW` | `GRAPH_PANEL_FAILURE` | n/a | REVIEW |
| `bench_03_small_tic_export` | model_enabled | `SINGLE_TRACE_SINGLE_AXIS` | 1 | 1 | 1 | n/a | n/a | n/a | n/a | `REVIEW_ONLY` | `PASS` | `GRAPH_PANEL_FAILURE` | n/a | REVIEW |
| `bench_04_stacked_xic_resolution` | deterministic | `DENSE_PEAK_SINGLE_AXIS` | 4 | 1 | 0 | 1 | 1 | `INVALID` | `INVALID` | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `INSUFFICIENT_X_ANCHORS`; `INSUFFICIENT_Y_ANCHORS`; `OCR_NO_NUMERIC_TEXT` | BLOCKED |
| `bench_04_stacked_xic_resolution` | model_enabled | `DENSE_PEAK_SINGLE_AXIS` | 4 | 1 | 0 | 1 | 1 | `INVALID` | `INVALID` | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `INSUFFICIENT_X_ANCHORS`; `INSUFFICIENT_Y_ANCHORS`; `OCR_NO_NUMERIC_TEXT` | BLOCKED |
| `bench_05_tic_plus_ions` | deterministic | `DENSE_PEAK_SINGLE_AXIS` | 4 | 1 | 0 | 0 | 0 | `INVALID` | `INVALID` | `BLOCKED` | `FAIL` | `TICK_LOCALIZATION_FAILURE` | `PLOT_FRAME_MISSING`; `TICK_MARKS_MISSING`; `OCR_NO_NUMERIC_TEXT`; `INSUFFICIENT_X_ANCHORS`; `INSUFFICIENT_Y_ANCHORS` | BLOCKED |
| `bench_05_tic_plus_ions` | model_enabled | `DENSE_PEAK_SINGLE_AXIS` | 4 | 1 | 0 | 0 | 0 | `INVALID` | `INVALID` | `BLOCKED` | `FAIL` | `TICK_LOCALIZATION_FAILURE` | `PLOT_FRAME_MISSING`; `TICK_MARKS_MISSING`; `OCR_NO_NUMERIC_TEXT`; `INSUFFICIENT_X_ANCHORS`; `INSUFFICIENT_Y_ANCHORS` | BLOCKED |
| `bench_06_photo_two_graphs_page` | deterministic | `DENSE_PEAK_SINGLE_AXIS` | 2 | 2 | 1 | n/a | n/a | n/a | n/a | `DIAGNOSTIC_ONLY` | `REVIEW` | `GRAPH_PANEL_FAILURE` | n/a | REVIEW |
| `bench_06_photo_two_graphs_page` | model_enabled | `DENSE_PEAK_SINGLE_AXIS` | 2 | 2 | 1 | n/a | n/a | n/a | n/a | `DIAGNOSTIC_ONLY` | `REVIEW` | `GRAPH_PANEL_FAILURE` | n/a | REVIEW |
| `bench_07_rotated_page_photo` | deterministic | `ROTATED_PAGE_GRAPH` | 1 | 1 | 0 | 1 | 3 | `INVALID` | `REVIEW` | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `INSUFFICIENT_X_ANCHORS`; `OCR_NO_NUMERIC_TEXT`; `HIGH_RESIDUALS` | BLOCKED |
| `bench_07_rotated_page_photo` | model_enabled | `ROTATED_PAGE_GRAPH` | 1 | 1 | 0 | 1 | 3 | `INVALID` | `REVIEW` | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `INSUFFICIENT_X_ANCHORS`; `OCR_NO_NUMERIC_TEXT`; `HIGH_RESIDUALS` | BLOCKED |

## Deterministic vs E2B

E2B/model-enabled mode did not reduce graph count, calibration status, or numeric metric evidence relative to deterministic mode in the final rerun. The model path remains advisory for these fixtures.

## Decision

Phase 9E remains blocked. The redesign added layout/tick evidence and exact tick subreasons, but did not close the runtime blockers. Product, QA, and Scientific acceptance remain blocked because required fixtures still produce `BLOCKED` gates.

# Phase 9C Android Rerun Results

Date: 2026-05-20

Device: `10AF5M15FY003YL`
Package: `com.chromalab.app.validation`
Runner: `tools/phase9b/run_android_validation_suite.ps1 -SuiteId phase9c_all -OutputRoot artifacts/phase9c-multi-fixture-android -SummaryPrefix phase9c`

Artifacts are intentionally local and not committed:

```text
artifacts/phase9c-multi-fixture-android/
/sdcard/Download/ChromaLab/validation/<run_id>/
```

## Final Suite

| Fixture | Mode | Run id | Graphs | Expected | Gate | Validator | Failure | Export | Decision |
| --- | --- | --- | ---: | ---: | --- | --- | --- | --- | --- |
| `white_tiger_ion71` | deterministic | `white_tiger_ion71_20260520_220650` | 1 | 1 | REVIEW_ONLY | REVIEW | `VLM_SEMANTIC_LAYER_UNAVAILABLE` | complete | REVIEW |
| `white_tiger_ion71` | E2B | `white_tiger_ion71_20260520_220727` | 1 | 1 | REVIEW_ONLY | PASS | `UNKNOWN_FAILURE` | complete | REVIEW |
| `bench_01_mz71_screenshot_page` | deterministic | `bench_01_mz71_screenshot_page_20260520_220815` | 1 | 2 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED |
| `bench_01_mz71_screenshot_page` | E2B | `bench_01_mz71_screenshot_page_20260520_220907` | 1 | 2 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED |
| `bench_02_mz92_belyi_tigr` | deterministic | `bench_02_mz92_belyi_tigr_20260520_221000` | 1 | 1 | DIAGNOSTIC_ONLY | REVIEW | `GRAPH_PANEL_FAILURE` | complete | REVIEW |
| `bench_02_mz92_belyi_tigr` | E2B | `bench_02_mz92_belyi_tigr_20260520_221103` | 1 | 1 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED |
| `bench_03_small_tic_export` | deterministic | `bench_03_small_tic_export_20260520_221338` | 1 | 1 | DIAGNOSTIC_ONLY | REVIEW | `GRAPH_PANEL_FAILURE` | complete | REVIEW |
| `bench_03_small_tic_export` | E2B | `bench_03_small_tic_export_20260520_221350` | 1 | 1 | DIAGNOSTIC_ONLY | REVIEW | `GRAPH_PANEL_FAILURE` | complete | REVIEW |
| `bench_04_stacked_xic_resolution` | deterministic | `bench_04_stacked_xic_resolution_20260520_221417` | 1 | 4 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED |
| `bench_04_stacked_xic_resolution` | E2B | `bench_04_stacked_xic_resolution_20260520_221444` | 1 | 4 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED |
| `bench_05_tic_plus_ions` | deterministic | `bench_05_tic_plus_ions_20260520_221511` | 1 | 4 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED |
| `bench_05_tic_plus_ions` | E2B | `bench_05_tic_plus_ions_20260520_221538` | 1 | 4 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED |
| `bench_06_photo_two_graphs_page` | deterministic | `bench_06_photo_two_graphs_page_20260520_221605` | 1 | 2 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED |
| `bench_06_photo_two_graphs_page` | E2B | `bench_06_photo_two_graphs_page_20260520_221658` | 1 | 2 | BLOCKED | REVIEW | `TICK_LOCALIZATION_FAILURE` | complete | BLOCKED |
| `bench_07_rotated_page_photo` | deterministic | `bench_07_rotated_page_photo_20260520_221751` | 1 | 1 | REVIEW_ONLY | REVIEW | `VLM_SEMANTIC_LAYER_UNAVAILABLE` | complete | REVIEW |
| `bench_07_rotated_page_photo` | E2B | `bench_07_rotated_page_photo_20260520_221854` | 1 | 1 | REVIEW_ONLY | PASS | `UNKNOWN_FAILURE` | complete | REVIEW |

## Result

- 16/16 runs exported runtime evidence package, validator JSON, validator Markdown, final report contract JSON, report export, stage timings, and manifest.
- The Phase 9C code repairs fixed artifact traceability and reduced ambiguous zero-graph summaries by counting graph failure packages separately.
- Phase 9C did not close runtime acceptance. Five fixture classes still produce `BLOCKED` graph-stage outcomes, mostly at `TICK_LOCALIZATION_FAILURE`.
- E2B still regresses `bench_02_mz92_belyi_tigr` from deterministic `DIAGNOSTIC_ONLY` to model-enabled `BLOCKED`.

Verdict: `PHASE_9B_BLOCKED_RUNTIME_FAILURE`.

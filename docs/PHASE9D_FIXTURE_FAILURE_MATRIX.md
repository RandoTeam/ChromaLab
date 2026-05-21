# Phase 9D Fixture Failure Matrix

Final source: `artifacts/phase9d-final-multi-fixture-android/phase9d_final_suite_summary.json`

| Fixture | Deterministic Result | E2B Result | E2B Regression | Final Classification |
| --- | --- | --- | --- | --- |
| `white_tiger_ion71` | 1 graph, REVIEW_ONLY, validator REVIEW | 1 graph, REVIEW_ONLY, validator PASS | No | REVIEW |
| `bench_01_mz71_screenshot_page` | 1 graph package, BLOCKED, `TICK_LOCALIZATION_FAILURE` | 1 graph package, BLOCKED, `TICK_LOCALIZATION_FAILURE` | No | BLOCKED |
| `bench_02_mz92_belyi_tigr` | 1 report graph, DIAGNOSTIC_ONLY, metadata count 2 | 1 report graph, DIAGNOSTIC_ONLY, metadata count 2 | No; zero-graph regression closed | DIAGNOSTIC / graph-count blocker |
| `bench_03_small_tic_export` | 1 graph, REVIEW_ONLY, validator REVIEW | 1 graph, REVIEW_ONLY, validator PASS | No | REVIEW |
| `bench_04_stacked_xic_resolution` | 1 graph package, BLOCKED, `TICK_LOCALIZATION_FAILURE` | 1 graph package, BLOCKED, `TICK_LOCALIZATION_FAILURE` | No | BLOCKED |
| `bench_05_tic_plus_ions` | 1 graph package, BLOCKED, `TICK_LOCALIZATION_FAILURE` | 1 graph package, BLOCKED, `TICK_LOCALIZATION_FAILURE` | No | BLOCKED |
| `bench_06_photo_two_graphs_page` | 1 graph package, BLOCKED, `TICK_LOCALIZATION_FAILURE` | 1 graph package, BLOCKED, `TICK_LOCALIZATION_FAILURE` | No | BLOCKED |
| `bench_07_rotated_page_photo` | 1 graph, REVIEW_ONLY, validator REVIEW | 1 graph, REVIEW_ONLY, validator PASS | No | REVIEW |

## Matrix Decision

Phase 9D does not meet acceptance. Four fixture classes remain BLOCKED and one fixture still has graph-count metadata disagreement. Product, QA, and Scientific reviewers do not approve Phase 10 start.

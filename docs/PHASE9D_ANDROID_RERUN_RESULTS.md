# Phase 9D Android Rerun Results

Final suite:

- Package: `com.chromalab.app.validation`
- Suite: `phase9d_final`
- Artifact root: `artifacts/phase9d-final-multi-fixture-android/`
- Fixtures: 8
- Modes: deterministic and E2B/model-enabled
- Runs: 16
- Export completeness: 16/16

| Fixture | Deterministic | E2B | Decision |
| --- | --- | --- | --- |
| `white_tiger_ion71` | 1 graph, REVIEW_ONLY, validator REVIEW | 1 graph, REVIEW_ONLY, validator PASS | REVIEW |
| `bench_01_mz71_screenshot_page` | 1 graph package, BLOCKED, tick localization | 1 graph package, BLOCKED, tick localization | BLOCKED |
| `bench_02_mz92_belyi_tigr` | 1 report graph, DIAGNOSTIC_ONLY, metadata count 2 | 1 report graph, DIAGNOSTIC_ONLY, metadata count 2 | DIAGNOSTIC |
| `bench_03_small_tic_export` | 1 graph, REVIEW_ONLY, validator REVIEW | 1 graph, REVIEW_ONLY, validator PASS | REVIEW |
| `bench_04_stacked_xic_resolution` | 1 graph package, BLOCKED, tick localization | 1 graph package, BLOCKED, tick localization | BLOCKED |
| `bench_05_tic_plus_ions` | 1 graph package, BLOCKED, tick localization | 1 graph package, BLOCKED, tick localization | BLOCKED |
| `bench_06_photo_two_graphs_page` | 1 graph package, BLOCKED, tick localization | 1 graph package, BLOCKED, tick localization | BLOCKED |
| `bench_07_rotated_page_photo` | 1 graph, REVIEW_ONLY, validator REVIEW | 1 graph, REVIEW_ONLY, validator PASS | REVIEW |

## Deterministic vs E2B

E2B no longer suppresses deterministic graph candidates in the final Phase 9D suite. No run showed E2B reducing graph count or calibration status relative to deterministic output.

## Verdict

`PHASE_9B_BLOCKED_RUNTIME_FAILURE`

Phase 10 must not start.

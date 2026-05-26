# Phase 9I Android Rerun Results

Date: 2026-05-26

Device: `10AF5M15FY003YL`

Validation package: `com.chromalab.app.validation`

## Artifact Roots

```text
artifacts/phase9i-pre-audit-bench01-bench05/
artifacts/phase9i-final-android-targeted/
artifacts/phase9i-final-android/
```

## Targeted Rerun

Command scope:

- `bench_01_mz71_screenshot_page`
- `bench_05_tic_plus_ions`
- deterministic and E2B modes
- 360 second suite timeout

| Fixture | Mode | Report graphs | Failure packages | Gate | Validator | Failure | Export |
| --- | --- | ---: | ---: | --- | --- | --- | --- |
| `bench_01_mz71_screenshot_page` | deterministic | 0 | 1 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | PASS |
| `bench_01_mz71_screenshot_page` | E2B | 0 | 1 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | PASS |
| `bench_05_tic_plus_ions` | deterministic | 0 | 1 | `BLOCKED` | `REVIEW` | `CALIBRATION_FAILURE` | PASS |
| `bench_05_tic_plus_ions` | E2B | 0 | 1 | `BLOCKED` | `REVIEW` | `CALIBRATION_FAILURE` | PASS |

## Full 8-Fixture Rerun

Final suite:

```text
artifacts/phase9i-final-android/phase9i_all8_final_suite_summary.md
artifacts/phase9i-final-android/phase9i_all8_final_suite_summary_phase9g.md
```

| Fixture | Deterministic | E2B | Export | Phase 9I decision |
| --- | --- | --- | --- | --- |
| `white_tiger_ion71` | 1 graph, `REVIEW_ONLY`, validator `REVIEW` | 1 graph, `REVIEW_ONLY`, validator `PASS` | 2/2 | Stable witness |
| `bench_01_mz71_screenshot_page` | 0 report graphs, 1 failure package, `BLOCKED`, `TICK_LOCALIZATION_FAILURE` | 0 report graphs, 1 failure package, `BLOCKED`, `TICK_LOCALIZATION_FAILURE` | 2/2 | Still blocks Phase 9 |
| `bench_02_mz92_belyi_tigr` | 1 graph, `REVIEW_ONLY`, validator `REVIEW` | 1 graph, `REVIEW_ONLY`, validator `PASS` | 2/2 | Stable witness |
| `bench_03_small_tic_export` | 1 graph, `REVIEW_ONLY`, validator `REVIEW` | 1 graph, `REVIEW_ONLY`, validator `PASS` | 2/2 | Stable witness |
| `bench_04_stacked_xic_resolution` | 1 graph, `REVIEW_ONLY`, validator `REVIEW` | 1 graph, `REVIEW_ONLY`, validator `PASS` | 2/2 | Stable witness |
| `bench_05_tic_plus_ions` | 0 report graphs, 1 failure package, `BLOCKED`, `CALIBRATION_FAILURE` | 0 report graphs, 1 failure package, `BLOCKED`, `CALIBRATION_FAILURE` | 2/2 | Still blocks Phase 9 |
| `bench_06_photo_two_graphs_page` | 1 graph, `REVIEW_ONLY`, validator `REVIEW` | 1 graph, `REVIEW_ONLY`, validator `PASS` | 2/2 | Stable witness |
| `bench_07_rotated_page_photo` | 1 graph, `REVIEW_ONLY`, validator `REVIEW` | 1 graph, `REVIEW_ONLY`, validator `PASS` | 2/2 | Stable witness |

## E2B Comparison

E2B did not regress deterministic graph count, gate, failure class, export status, or numeric metrics in Phase 9I.

E2B was neutral on the target blockers:

- `bench_01`: deterministic and E2B both fail at insufficient Y anchors.
- `bench_05`: deterministic and E2B both fail at Y calibration direction inconsistency.

## Result

Phase 9I closes the no-export ambiguity but does not close the two target runtime blockers. Final verdict remains `PHASE_9B_BLOCKED_RUNTIME_FAILURE`.

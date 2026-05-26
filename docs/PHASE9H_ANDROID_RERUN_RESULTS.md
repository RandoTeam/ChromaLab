# Phase 9H Android Rerun Results

## Device

- Device: `10AF5M15FY003YL`
- Package: `com.chromalab.app.validation`
- Artifact roots:
  - Scoped: `artifacts/phase9h-white-tiger-bench03-bench07-android-final2/`
  - Full suite: `artifacts/phase9h-all8-android-final/`

## Scoped Rerun

| Fixture | Mode | Gate | Validator | Export |
| --- | --- | --- | --- | --- |
| `white_tiger_ion71` | deterministic | `REVIEW_ONLY` | `REVIEW` | complete |
| `white_tiger_ion71` | E2B | `REVIEW_ONLY` | `PASS` | complete |
| `bench_03_small_tic_export` | deterministic | `REVIEW_ONLY` | `REVIEW` | complete |
| `bench_03_small_tic_export` | E2B | `REVIEW_ONLY` | `PASS` | complete |
| `bench_07_rotated_page_photo` | deterministic | `REVIEW_ONLY` | `REVIEW` | complete |
| `bench_07_rotated_page_photo` | E2B | `REVIEW_ONLY` | `PASS` | complete |

## Full 8-Fixture Rerun

| Fixture | Deterministic | E2B | Decision |
| --- | --- | --- | --- |
| `white_tiger_ion71` | `REVIEW_ONLY`, validator `REVIEW` | `REVIEW_ONLY`, validator `PASS` | fixed |
| `bench_01_mz71_screenshot_page` | runner timeout / no export | `BLOCKED`, validator `REVIEW` | blocked |
| `bench_02_mz92_belyi_tigr` | `REVIEW_ONLY`, validator `REVIEW` | `REVIEW_ONLY`, validator `PASS` | improved |
| `bench_03_small_tic_export` | `REVIEW_ONLY`, validator `REVIEW` | `REVIEW_ONLY`, validator `PASS` | stable |
| `bench_04_stacked_xic_resolution` | `REVIEW_ONLY`, validator `REVIEW` | `REVIEW_ONLY`, validator `PASS` | stable |
| `bench_05_tic_plus_ions` | `BLOCKED`, validator `REVIEW` | `BLOCKED`, validator `REVIEW` | blocked |
| `bench_06_photo_two_graphs_page` | `REVIEW_ONLY`, validator `REVIEW` | `REVIEW_ONLY`, validator `PASS` | improved |
| `bench_07_rotated_page_photo` | `REVIEW_ONLY`, validator `REVIEW` | `REVIEW_ONLY`, validator `PASS` | stable |

## E2B Comparison

E2B did not regress deterministic graph count, calibration, trace/peak progression, or report gate in the Phase 9H reruns. Where deterministic reached `REVIEW_ONLY`, E2B matched the report gate and improved validator verdict to `PASS`.

E2B remains blocked from creating pixel geometry, calibration coefficients, chromatographic metrics, graph count changes, peak metric overrides, or compound identities.

## Remaining Android Blockers

- `bench_01_mz71_screenshot_page` deterministic run did not export before timeout; model-enabled mode remains `BLOCKED` with `TICK_LOCALIZATION_FAILURE`.
- `bench_05_tic_plus_ions` remains `BLOCKED` in both modes with `TICK_LOCALIZATION_FAILURE`.
- Phase 9 is not accepted.

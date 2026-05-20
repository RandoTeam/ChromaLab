# Phase 9C Fixture Failure Matrix

Source inputs: `docs/PHASE9B_ANDROID_FIXTURE_RESULTS.md`, `docs/PHASE9B_MODEL_COMPARISON.md`, and `artifacts/phase9b-multi-fixture-android/`.

| Fixture | Expected graphs | Deterministic before Phase 9C | E2B before Phase 9C | Primary failure | Evidence status | Phase 9C action |
| --- | ---: | --- | --- | --- | --- | --- |
| `white_tiger_ion71` | 1 | 1 graph, `REVIEW_ONLY`, validator `REVIEW` | 1 graph, `REVIEW_ONLY`, validator `PASS` | none critical | complete | Non-regression rerun. |
| `bench_01_mz71_screenshot_page` | 2 | 0 graphs, `BLOCKED`, `TICK_LOCALIZATION_FAILURE` | 0 graphs, `BLOCKED`, `TICK_LOCALIZATION_FAILURE` | tick/OCR anchor pairing | graph package present | Repair local tick OCR provenance and rerun. |
| `bench_02_mz92_belyi_tigr` | 1 | 2 reported graphs, `DIAGNOSTIC_ONLY` | 0 graphs, `BLOCKED` | graph count summary + model comparison | evidence complete but summary ambiguous | Fix suite graph count fields and E2B comparison; rerun. |
| `bench_03_small_tic_export` | 1 | 1 graph, `DIAGNOSTIC_ONLY` | 1 graph, `DIAGNOSTIC_ONLY` | low-resolution diagnostic gate | complete | Rerun and classify. |
| `bench_04_stacked_xic_resolution` | 4 | 0 graphs, `BLOCKED` | 0 graphs, `BLOCKED` | stacked panel tick/calibration evidence | graph package expected | Rerun after tick repair; classify remaining stage precisely. |
| `bench_05_tic_plus_ions` | 4 | 0 graphs, `BLOCKED` | 0 graphs, `BLOCKED` | multi-panel tick/calibration evidence | graph package expected | Rerun after tick repair; classify remaining stage precisely. |
| `bench_06_photo_two_graphs_page` | 2 | 0 graphs, `BLOCKED` | 0 graphs, `BLOCKED` | Y values accepted as X anchors | graph package present | Repair crop-axis binding and rerun. |
| `bench_07_rotated_page_photo` | 1 | 1 graph, `REVIEW_ONLY` | 1 graph, `REVIEW_ONLY`, validator `PASS` | none critical | complete | Non-regression rerun. |

## Acceptance Rules

- Any `BLOCKED` run after repair remains a Phase 9C blocker unless Product, QA, and Scientific review explicitly classify it as unsupported with complete evidence.
- E2B mode must not reduce graph count, calibration status, or deterministic numeric evidence relative to deterministic mode.
- A terminal calibration failure may have zero completed report graphs, but it must still export graph-level failure packages.

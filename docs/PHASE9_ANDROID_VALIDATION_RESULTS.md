# Phase 9 Android Validation Results

Date: 2026-05-20

## Fixture Runs

| Run | Mode | Graphs | X/Y calibration | Report gate | Validator | Blocking issues | Runtime failure class | Notes |
| --- | --- | ---: | --- | --- | --- | ---: | --- | --- |
| `white_tiger_ion71_20260520_192547` | Deterministic no-model | 1 | VALID / VALID | REVIEW_ONLY | REVIEW | 0 | VLM_SEMANTIC_LAYER_UNAVAILABLE | Expected baseline warning; exports completed. |
| `white_tiger_ion71_20260520_192400` | Model-enabled E2B fallback | 1 | VALID / VALID | REVIEW_ONLY | PASS | 0 | UNKNOWN_FAILURE | E2B loaded after deterministic calibration; no numeric regression. |
| `white_tiger_ion71_20260520_191649` | Earlier model-enabled attempt | 0 | failed before report graph | BLOCKED | REVIEW | 0 | TICK_LOCALIZATION_FAILURE | Used as failure evidence; fixed by deferring model activation until after deterministic calibration. |

## Artifact Roots

Pulled local artifact copies:

```text
artifacts/phase9-android-validation/white_tiger_ion71_20260520_192547/
artifacts/phase9-android-validation/white_tiger_ion71_20260520_192400/
```

Device export roots:

```text
/sdcard/Download/ChromaLab/validation/white_tiger_ion71_20260520_192547/
/sdcard/Download/ChromaLab/validation/white_tiger_ion71_20260520_192400/
```

## Result

The final code path validates both required modes on real Android:

- deterministic no-model path remains REVIEW_ONLY with one graph and valid calibration;
- model-enabled E2B path remains REVIEW_ONLY with one graph and valid calibration;
- model activation no longer corrupts graph selection, tick localization, or calibration.

Phase 9 does not claim RELEASE_READY for this fixture because trace/peak/report evidence remains review-grade.

## Phase 9B Correction

The single-fixture result above is no longer sufficient for Phase 9 acceptance. Phase 9B ran eight fixtures in deterministic and E2B model-enabled modes.

| Fixture | Deterministic result | E2B result | Acceptance impact |
| --- | --- | --- | --- |
| `white_tiger_ion71` | REVIEW_ONLY / REVIEW / 1 graph | REVIEW_ONLY / PASS / 1 graph | Review-only, not release-ready. |
| `bench_01_mz71_screenshot_page` | BLOCKED / REVIEW / 0 graphs | BLOCKED / REVIEW / 0 graphs | Blocks photographed-page multi-graph acceptance. |
| `bench_02_mz92_belyi_tigr` | DIAGNOSTIC_ONLY / REVIEW / 2 graphs | BLOCKED / REVIEW / 0 graphs | Blocks single-graph and E2B comparison acceptance. |
| `bench_03_small_tic_export` | DIAGNOSTIC_ONLY / REVIEW / 1 graph | DIAGNOSTIC_ONLY / REVIEW / 1 graph | Diagnostic only. |
| `bench_04_stacked_xic_resolution` | BLOCKED / REVIEW / 0 graphs | BLOCKED / REVIEW / 0 graphs | Blocks stacked multi-graph acceptance. |
| `bench_05_tic_plus_ions` | BLOCKED / REVIEW / 0 graphs | BLOCKED / REVIEW / 0 graphs | Blocks TIC/ion multi-graph acceptance. |
| `bench_06_photo_two_graphs_page` | BLOCKED / REVIEW / 0 graphs | BLOCKED / REVIEW / 0 graphs | Blocks photographed two-graph acceptance. |
| `bench_07_rotated_page_photo` | REVIEW_ONLY / REVIEW / 1 graph | REVIEW_ONLY / PASS / 1 graph | Review-only, not release-ready. |

Artifact root:

```text
artifacts/phase9b-multi-fixture-android/
```

Phase 9B verdict is `PHASE_9B_BLOCKED_RUNTIME_FAILURE`. Phase 10 may not start.

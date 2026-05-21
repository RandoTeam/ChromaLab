# Phase 9F Android Rerun Results

Final artifact root: `artifacts/phase9f-multi-fixture-android-final/`

Suite command:

```powershell
.\tools\phase9b\run_android_validation_suite.ps1 -SuiteId phase9f_all_final -OutputRoot artifacts\phase9f-multi-fixture-android-final -SummaryPrefix phase9f_final -TimeoutSeconds 900
```

## Summary

| Outcome | Count |
| --- | ---: |
| Total runs | 16 |
| Exports pulled | 16 |
| `BLOCKED` | 8 |
| `REVIEW_ONLY` | 8 |
| `RELEASE_READY` | 0 |
| Validator `PASS` | 4 |
| Validator `REVIEW` | 10 |
| Validator `FAIL` | 2 |

## Per-Fixture Table

| Fixture | Mode | Report graphs | Expected graphs | Gate | Validator | Failure | X anchors | Y anchors | Scale subreason | Export |
| --- | --- | ---: | ---: | --- | --- | --- | ---: | ---: | --- | --- |
| `white_tiger_ion71` | deterministic | 1 | 1 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | 1 | 0 | `LABEL_SEQUENCE_NON_MONOTONIC`, `INSUFFICIENT_SCALE_ANCHORS`, `TITLE_ION_TEXT_REJECTED_AS_SCALE_LABEL` | yes |
| `white_tiger_ion71` | model_enabled | 1 | 1 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | 1 | 0 | same as deterministic | yes |
| `bench_01_mz71_screenshot_page` | deterministic | 1 | 2 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | 5 | 0 | `LABEL_SEQUENCE_NON_MONOTONIC`, `INSUFFICIENT_SCALE_ANCHORS`, `TITLE_ION_TEXT_REJECTED_AS_SCALE_LABEL` | yes |
| `bench_01_mz71_screenshot_page` | model_enabled | 1 | 2 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | 5 | 0 | same as deterministic | yes |
| `bench_02_mz92_belyi_tigr` | deterministic | 1 | 1 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | 0 | 0 | `INSUFFICIENT_SCALE_ANCHORS`, `TITLE_ION_TEXT_REJECTED_AS_SCALE_LABEL` | yes |
| `bench_02_mz92_belyi_tigr` | model_enabled | 1 | 1 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | 0 | 0 | same as deterministic | yes |
| `bench_03_small_tic_export` | deterministic | 1 | 1 | `REVIEW_ONLY` | `REVIEW` | `PEAK_EVIDENCE_FAILURE` | n/a | n/a | n/a | yes |
| `bench_03_small_tic_export` | model_enabled | 1 | 1 | `REVIEW_ONLY` | `PASS` | `PEAK_EVIDENCE_FAILURE` | n/a | n/a | n/a | yes |
| `bench_04_stacked_xic_resolution` | deterministic | 1 | 4 | `REVIEW_ONLY` | `REVIEW` | `VLM_SEMANTIC_LAYER_UNAVAILABLE` | n/a | n/a | n/a | yes |
| `bench_04_stacked_xic_resolution` | model_enabled | 1 | 4 | `REVIEW_ONLY` | `PASS` | `UNKNOWN_FAILURE` | n/a | n/a | n/a | yes |
| `bench_05_tic_plus_ions` | deterministic | 1 | 4 | `BLOCKED` | `FAIL` | `TICK_LOCALIZATION_FAILURE` | 0 | 0 | `AXIS_FRAME_INCONSISTENT` | yes |
| `bench_05_tic_plus_ions` | model_enabled | 1 | 4 | `BLOCKED` | `FAIL` | `TICK_LOCALIZATION_FAILURE` | 0 | 0 | `AXIS_FRAME_INCONSISTENT` | yes |
| `bench_06_photo_two_graphs_page` | deterministic | 1 | 2 | `REVIEW_ONLY` | `REVIEW` | `VLM_SEMANTIC_LAYER_UNAVAILABLE` | n/a | n/a | n/a | yes |
| `bench_06_photo_two_graphs_page` | model_enabled | 1 | 2 | `REVIEW_ONLY` | `PASS` | `UNKNOWN_FAILURE` | n/a | n/a | n/a | yes |
| `bench_07_rotated_page_photo` | deterministic | 1 | 1 | `REVIEW_ONLY` | `REVIEW` | `GRAPH_PANEL_FAILURE` | n/a | n/a | n/a | yes |
| `bench_07_rotated_page_photo` | model_enabled | 1 | 1 | `REVIEW_ONLY` | `PASS` | `GRAPH_PANEL_FAILURE` | n/a | n/a | n/a | yes |

## Result

Phase 9F improved several fixtures from `BLOCKED` to `REVIEW_ONLY`, but it did not close Phase 9. Required supported fixtures still block due missing Y anchors, insufficient scale anchors, plot-frame inconsistency, and incomplete multi-panel report graph propagation.

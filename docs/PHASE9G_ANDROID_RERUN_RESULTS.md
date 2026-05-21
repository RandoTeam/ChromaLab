# Phase 9G Android Rerun Results

Status: completed.

Artifacts:

- First complete suite: `artifacts/phase9g-multi-fixture-android-final/`
- Final suite after graph failure missing-reason patch: `artifacts/phase9g-multi-fixture-android-final2/`

Final suite:

- Suite id: `phase9g_all_final2`
- Package: `com.chromalab.app.validation`
- Fixtures: 8
- Modes: deterministic and model-enabled / E2B
- Runs: 16
- Exports complete: 16/16
- Validator blocking issues: 0
- Release-ready: 0
- Review-only: 8
- Blocked: 8

## Final Rerun Table

| Fixture | Mode | Report graphs | Failure packages | Gate | Validator | Failure | X strategy | Y strategy | Decision |
| --- | --- | ---: | ---: | --- | --- | --- | --- | --- | --- |
| `white_tiger_ion71` | deterministic | 0 | 1 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `AXIS_SCALE_RESOLVER` | `LEGACY_TICK_LOCALIZATION` | BLOCKED |
| `white_tiger_ion71` | model_enabled | 0 | 1 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `AXIS_SCALE_RESOLVER` | `LEGACY_TICK_LOCALIZATION` | BLOCKED |
| `bench_01_mz71_screenshot_page` | deterministic | 0 | 1 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `LEGACY_TICK_LOCALIZATION` | `LEGACY_TICK_LOCALIZATION` | BLOCKED |
| `bench_01_mz71_screenshot_page` | model_enabled | 0 | 1 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `LEGACY_TICK_LOCALIZATION` | `LEGACY_TICK_LOCALIZATION` | BLOCKED |
| `bench_02_mz92_belyi_tigr` | deterministic | 0 | 1 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `LEGACY_TICK_LOCALIZATION` | `AXIS_SCALE_RESOLVER` | BLOCKED |
| `bench_02_mz92_belyi_tigr` | model_enabled | 0 | 1 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | `LEGACY_TICK_LOCALIZATION` | `AXIS_SCALE_RESOLVER` | BLOCKED |
| `bench_03_small_tic_export` | deterministic | 1 | 0 | `REVIEW_ONLY` | `REVIEW` | `PEAK_EVIDENCE_FAILURE` | - | - | REVIEW |
| `bench_03_small_tic_export` | model_enabled | 1 | 0 | `REVIEW_ONLY` | `PASS` | `PEAK_EVIDENCE_FAILURE` | - | - | REVIEW |
| `bench_04_stacked_xic_resolution` | deterministic | 1 | 0 | `REVIEW_ONLY` | `REVIEW` | `VLM_SEMANTIC_LAYER_UNAVAILABLE` | - | - | REVIEW |
| `bench_04_stacked_xic_resolution` | model_enabled | 1 | 0 | `REVIEW_ONLY` | `PASS` | `UNKNOWN_FAILURE` | - | - | REVIEW |
| `bench_05_tic_plus_ions` | deterministic | 0 | 1 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | - | - | BLOCKED |
| `bench_05_tic_plus_ions` | model_enabled | 0 | 1 | `BLOCKED` | `REVIEW` | `TICK_LOCALIZATION_FAILURE` | - | - | BLOCKED |
| `bench_06_photo_two_graphs_page` | deterministic | 1 | 0 | `REVIEW_ONLY` | `REVIEW` | `VLM_SEMANTIC_LAYER_UNAVAILABLE` | - | - | REVIEW |
| `bench_06_photo_two_graphs_page` | model_enabled | 1 | 0 | `REVIEW_ONLY` | `PASS` | `UNKNOWN_FAILURE` | - | - | REVIEW |
| `bench_07_rotated_page_photo` | deterministic | 1 | 0 | `REVIEW_ONLY` | `REVIEW` | `GRAPH_PANEL_FAILURE` | - | - | REVIEW |
| `bench_07_rotated_page_photo` | model_enabled | 1 | 0 | `REVIEW_ONLY` | `PASS` | `GRAPH_PANEL_FAILURE` | - | - | REVIEW |

## Deterministic vs E2B

E2B did not reduce graph count, erase deterministic graph packages, or change calibration strategy outcomes in the final suite. Model-enabled rows either matched deterministic `BLOCKED` status for tick/calibration failures or improved validator verdict from `REVIEW` to `PASS` for review-only rows.

## E2B Baseline Mode Decision

E2B is retained as `E2B_BASELINE`, the supported production FAST/weaker-device model mode. The final Phase 9G suite tested E2B for every fixture alongside deterministic baseline.

Allowed E2B effects remain limited to local crop OCR, title/ion/tick/peak text classification, Knowledge Pack grounded explanations, warning explanations, overlay review warnings, and advisory graph/plot/tick quality signals. E2B is not allowed to erase deterministic graph candidates, change graph count by itself, create pixel coordinates, create calibration coefficients, create chromatographic metrics, override deterministic peak metrics, or identify compounds without explicit evidence.

Final Phase 9G outcome: no E2B regression was observed. E2B is safe to keep as FAST/weaker-device baseline under advisory-only constraints, but Phase 9 remains blocked by deterministic calibration/layout failures.

## Remaining Runtime Blockers

- `white_tiger_ion71` remains regressed to `BLOCKED`; legacy Y is selected but X still selects invalid axis-scale evidence and accepted anchors are insufficient.
- `bench_01_mz71_screenshot_page` remains `BLOCKED` with non-monotonic/high-residual tick evidence and insufficient Y anchors.
- `bench_02_mz92_belyi_tigr` remains `BLOCKED` with insufficient X anchors.
- `bench_05_tic_plus_ions` remains `BLOCKED` due plot-frame/tick candidate absence, but the graph failure package now records an explicit no-tick-candidate reason and validator verdict is `REVIEW`, not `FAIL`.

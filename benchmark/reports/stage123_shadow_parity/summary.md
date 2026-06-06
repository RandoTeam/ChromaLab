# R2 Stage 1-3 Shadow Parity Harness

Verdict: `R2_SHADOW_PARITY_HARNESS_READY_PRODUCTION_UNCHANGED`

Production impact: `NONE_SHADOW_ONLY`

Records: `36`
Fixtures: `8`
Sources: `6`

R2 does not change Android runtime behavior, validators, report gates, graph-count expectations, chromatographic math, model policy, or CalculationEngine.

## Source Summary

| Source | Kind | Records | Graph count pass | Layout pass | Runtime readiness | Promotion decision |
|---|---|---:|---:|---:|---|---|
| `android_phase9j_current` | `ACTIVE_ANDROID_BASELINE` | 16 | 8 | 2 | `ACTIVE_RUNTIME_RECORDED_OUTPUT` | `BASELINE_RECORD_ONLY` |
| `annotation_text_role_page_context_upper_bound_v1` | `ANNOTATION_UPPER_BOUND` | 4 | 4 | 4 | `UPPER_BOUND_REQUIRES_AUTOMATIC_PAGE_CONTEXT_AND_TEXT_ROLE_EXTRACTION` | `DO_NOT_PROMOTE_UPPER_BOUND_ONLY` |
| `annotation_text_role_panel_family_v1` | `ANNOTATION_UPPER_BOUND` | 4 | 4 | 3 | `UPPER_BOUND_REQUIRES_AUTOMATIC_OCR_TEXT_ROLE_EXTRACTION` | `DO_NOT_PROMOTE_UPPER_BOUND_ONLY` |
| `full_width_axis_projection_v1` | `PC_PROTOTYPE` | 4 | 3 | 1 | `PC_RESEARCH_PROTOTYPE_NOT_RUNTIME_READY` | `DO_NOT_PROMOTE_RESEARCH_ONLY` |
| `geometry_only_panel_count_v1` | `ANNOTATION_UPPER_BOUND` | 4 | 4 | 2 | `PROTOTYPE_GEOMETRY_ONLY` | `DO_NOT_PROMOTE_UPPER_BOUND_ONLY` |
| `label_band_assisted_axis_projection_v1` | `PC_PROTOTYPE` | 4 | 4 | 2 | `PC_RESEARCH_PROTOTYPE_NOT_RUNTIME_READY` | `DO_NOT_PROMOTE_RESEARCH_ONLY` |

## Active Baseline Records

| Fixture | Mode | Expected graphs | Detected graphs | Graph score | Expected layout | Predicted layout | Layout score | Failure |
|---|---|---:|---:|---|---|---|---|---|
| `bench_01_mz71_screenshot_page` | `deterministic` | 2 | 1 | FAIL | MULTI_PANEL_SEPARATE_AXES | UNKNOWN_REVIEW | MISSING | `TICK_LOCALIZATION_FAILURE` |
| `bench_01_mz71_screenshot_page` | `model_enabled` | 2 | 1 | FAIL | MULTI_PANEL_SEPARATE_AXES | UNKNOWN_REVIEW | MISSING | `TICK_LOCALIZATION_FAILURE` |
| `bench_02_mz92_belyi_tigr` | `deterministic` | 1 | 1 | PASS | SINGLE_TRACE_SINGLE_AXIS | DENSE_PEAK_SINGLE_AXIS | FAIL | `GRAPH_PANEL_FAILURE` |
| `bench_02_mz92_belyi_tigr` | `model_enabled` | 1 | 1 | PASS | SINGLE_TRACE_SINGLE_AXIS | DENSE_PEAK_SINGLE_AXIS | FAIL | `GRAPH_PANEL_FAILURE` |
| `bench_03_small_tic_export` | `deterministic` | 1 | 1 | PASS | LOW_RES_EXPORT_GRAPH | SINGLE_TRACE_SINGLE_AXIS | FAIL | `PEAK_EVIDENCE_FAILURE` |
| `bench_03_small_tic_export` | `model_enabled` | 1 | 1 | PASS | LOW_RES_EXPORT_GRAPH | SINGLE_TRACE_SINGLE_AXIS | FAIL | `PEAK_EVIDENCE_FAILURE` |
| `bench_04_stacked_xic_resolution` | `deterministic` | 4 | 1 | FAIL | MULTI_PANEL_SEPARATE_AXES | DENSE_PEAK_SINGLE_AXIS | FAIL | `VLM_SEMANTIC_LAYER_UNAVAILABLE` |
| `bench_04_stacked_xic_resolution` | `model_enabled` | 4 | 1 | FAIL | MULTI_PANEL_SEPARATE_AXES | DENSE_PEAK_SINGLE_AXIS | FAIL | `UNKNOWN_FAILURE` |
| `bench_05_tic_plus_ions` | `deterministic` | 4 | 1 | FAIL | TIC_PLUS_ION_PANELS | UNKNOWN_REVIEW | MISSING | `CALIBRATION_FAILURE` |
| `bench_05_tic_plus_ions` | `model_enabled` | 4 | 1 | FAIL | TIC_PLUS_ION_PANELS | UNKNOWN_REVIEW | MISSING | `CALIBRATION_FAILURE` |
| `bench_06_photo_two_graphs_page` | `deterministic` | 2 | 1 | FAIL | TWO_GRAPH_PAGE | DENSE_PEAK_SINGLE_AXIS | FAIL | `VLM_SEMANTIC_LAYER_UNAVAILABLE` |
| `bench_06_photo_two_graphs_page` | `model_enabled` | 2 | 1 | FAIL | TWO_GRAPH_PAGE | DENSE_PEAK_SINGLE_AXIS | FAIL | `UNKNOWN_FAILURE` |
| `bench_07_rotated_page_photo` | `deterministic` | 1 | 1 | PASS | ROTATED_PAGE_GRAPH | ROTATED_PAGE_GRAPH | PASS | `VLM_SEMANTIC_LAYER_UNAVAILABLE` |
| `bench_07_rotated_page_photo` | `model_enabled` | 1 | 1 | PASS | ROTATED_PAGE_GRAPH | ROTATED_PAGE_GRAPH | PASS | `UNKNOWN_FAILURE` |
| `white_tiger_ion71` | `deterministic` | 1 | 1 | PASS | SINGLE_TRACE_SINGLE_AXIS | DENSE_PEAK_SINGLE_AXIS | FAIL | `PEAK_EVIDENCE_FAILURE` |
| `white_tiger_ion71` | `model_enabled` | 1 | 1 | PASS | SINGLE_TRACE_SINGLE_AXIS | DENSE_PEAK_SINGLE_AXIS | FAIL | `PEAK_EVIDENCE_FAILURE` |

## E2B Stage 1-3 Comparison

| Fixture | Deterministic graphs | E2B graphs | Delta | Layout changed | Decision |
|---|---:|---:|---:|---|---|
| `bench_01_mz71_screenshot_page` | 1 | 1 | 0 | False | `E2B_NEUTRAL_FOR_STAGE123` |
| `bench_02_mz92_belyi_tigr` | 1 | 1 | 0 | False | `E2B_NEUTRAL_FOR_STAGE123` |
| `bench_03_small_tic_export` | 1 | 1 | 0 | False | `E2B_NEUTRAL_FOR_STAGE123` |
| `bench_04_stacked_xic_resolution` | 1 | 1 | 0 | False | `E2B_NEUTRAL_FOR_STAGE123` |
| `bench_05_tic_plus_ions` | 1 | 1 | 0 | False | `E2B_NEUTRAL_FOR_STAGE123` |
| `bench_06_photo_two_graphs_page` | 1 | 1 | 0 | False | `E2B_NEUTRAL_FOR_STAGE123` |
| `bench_07_rotated_page_photo` | 1 | 1 | 0 | False | `E2B_NEUTRAL_FOR_STAGE123` |
| `white_tiger_ion71` | 1 | 1 | 0 | False | `E2B_NEUTRAL_FOR_STAGE123` |

## Next Required Work

- Add a real Rust/Kotlin Stage 1 image-preparation candidate that emits normalized-image evidence.
- Add a Rust/Kotlin graph-discovery candidate with graphPanel candidate/rejection tables.
- Add automatic OCR text-role/page-context extraction before using DRC7-style semantic layout classification.
- Keep shadow records out of production report gates until promotion criteria pass.

# Phase 9J Benchmark Score Summary

Input root: `benchmark/examples/phase9j_truth_audit`

Records: `16`
Fixtures: `8`

## Counts

| Metric | Counts |
| --- | --- |
| Decisions | `{'BLOCKED': 4, 'REVIEW': 12}` |
| Report gates | `{'BLOCKED': 4, 'REVIEW_ONLY': 12}` |
| Validator verdicts | `{'REVIEW': 10, 'PASS': 6}` |
| Failure classes | `{'TICK_LOCALIZATION_FAILURE': 2, 'GRAPH_PANEL_FAILURE': 2, 'PEAK_EVIDENCE_FAILURE': 4, 'VLM_SEMANTIC_LAYER_UNAVAILABLE': 3, 'UNKNOWN_FAILURE': 3, 'CALIBRATION_FAILURE': 2}` |
| Annotation priority | `{'P0': 8, 'P1': 7, 'P2': 1}` |

## Stage Status Counts

| Stage | Counts |
| --- | --- |
| `graph_panel` | `{'REVIEW': 8, 'PASS': 8}` |
| `calibration` | `{'FAIL': 2, 'REVIEW': 14}` |
| `trace` | `{'FAIL': 4, 'REVIEW': 12}` |
| `peaks` | `{'FAIL': 4, 'REVIEW': 12}` |
| `report_claims` | `{'FAIL': 4, 'REVIEW': 12}` |
| `evidence_package` | `{'PASS': 16}` |

## Case Table

| Case | Decision | Gate | Failure | Graphs | Calibration | Trace | Peaks | Priority |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `phase9j_bench_01_mz71_screenshot_page_deterministic` | BLOCKED | BLOCKED | TICK_LOCALIZATION_FAILURE | 1/2 | X:VALID Y:INVALID | MISSING | MISSING (0) | P0 |
| `phase9j_bench_01_mz71_screenshot_page_model_enabled` | BLOCKED | BLOCKED | TICK_LOCALIZATION_FAILURE | 1/2 | X:VALID Y:INVALID | MISSING | MISSING (0) | P0 |
| `phase9j_bench_02_mz92_belyi_tigr_deterministic` | REVIEW | REVIEW_ONLY | GRAPH_PANEL_FAILURE | 1/1 | X:REVIEW Y:REVIEW | REVIEW | REVIEW (1) | P1 |
| `phase9j_bench_02_mz92_belyi_tigr_model_enabled` | REVIEW | REVIEW_ONLY | GRAPH_PANEL_FAILURE | 1/1 | X:REVIEW Y:REVIEW | REVIEW | REVIEW (1) | P1 |
| `phase9j_bench_03_small_tic_export_deterministic` | REVIEW | REVIEW_ONLY | PEAK_EVIDENCE_FAILURE | 1/1 | X:REVIEW Y:REVIEW | REVIEW | REVIEW (3) | P1 |
| `phase9j_bench_03_small_tic_export_model_enabled` | REVIEW | REVIEW_ONLY | PEAK_EVIDENCE_FAILURE | 1/1 | X:REVIEW Y:REVIEW | REVIEW | REVIEW (3) | P1 |
| `phase9j_bench_04_stacked_xic_resolution_deterministic` | REVIEW | REVIEW_ONLY | VLM_SEMANTIC_LAYER_UNAVAILABLE | 1/4 | X:REVIEW Y:REVIEW | REVIEW | REVIEW (1) | P0 |
| `phase9j_bench_04_stacked_xic_resolution_model_enabled` | REVIEW | REVIEW_ONLY | UNKNOWN_FAILURE | 1/4 | X:REVIEW Y:REVIEW | REVIEW | REVIEW (1) | P0 |
| `phase9j_bench_05_tic_plus_ions_deterministic` | BLOCKED | BLOCKED | CALIBRATION_FAILURE | 1/4 | X:VALID Y:REVIEW | MISSING | MISSING (0) | P0 |
| `phase9j_bench_05_tic_plus_ions_model_enabled` | BLOCKED | BLOCKED | CALIBRATION_FAILURE | 1/4 | X:VALID Y:REVIEW | MISSING | MISSING (0) | P0 |
| `phase9j_bench_06_photo_two_graphs_page_deterministic` | REVIEW | REVIEW_ONLY | VLM_SEMANTIC_LAYER_UNAVAILABLE | 1/2 | X:REVIEW Y:REVIEW | REVIEW | REVIEW (1) | P0 |
| `phase9j_bench_06_photo_two_graphs_page_model_enabled` | REVIEW | REVIEW_ONLY | UNKNOWN_FAILURE | 1/2 | X:REVIEW Y:REVIEW | REVIEW | REVIEW (1) | P0 |
| `phase9j_bench_07_rotated_page_photo_deterministic` | REVIEW | REVIEW_ONLY | VLM_SEMANTIC_LAYER_UNAVAILABLE | 1/1 | X:REVIEW Y:REVIEW | REVIEW | REVIEW (1) | P2 |
| `phase9j_bench_07_rotated_page_photo_model_enabled` | REVIEW | REVIEW_ONLY | UNKNOWN_FAILURE | 1/1 | X:REVIEW Y:REVIEW | REVIEW | REVIEW (1) | P1 |
| `phase9j_white_tiger_ion71_deterministic` | REVIEW | REVIEW_ONLY | PEAK_EVIDENCE_FAILURE | 1/1 | X:REVIEW Y:REVIEW | REVIEW | REVIEW (12) | P1 |
| `phase9j_white_tiger_ion71_model_enabled` | REVIEW | REVIEW_ONLY | PEAK_EVIDENCE_FAILURE | 1/1 | X:REVIEW Y:REVIEW | REVIEW | REVIEW (12) | P1 |

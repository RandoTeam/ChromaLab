# Phase 9J Benchmark Records Summary

Source: `artifacts/phase9j-truth-audit/phase9j_summary.json`

Record count: `16`

## Counts

| Category | Counts |
| --- | --- |
| Product decisions | `{'REVIEW': 12, 'BLOCKED': 4}` |
| Report gates | `{'REVIEW_ONLY': 12, 'BLOCKED': 4}` |
| Validator verdicts | `{'REVIEW': 10, 'PASS': 6}` |
| Failure classes | `{'PEAK_EVIDENCE_FAILURE': 4, 'TICK_LOCALIZATION_FAILURE': 2, 'GRAPH_PANEL_FAILURE': 2, 'VLM_SEMANTIC_LAYER_UNAVAILABLE': 3, 'UNKNOWN_FAILURE': 3, 'CALIBRATION_FAILURE': 2}` |

## Records

| Case | Expected graphs | Detected graphs | Gate | Validator | Failure | Stage | Peaks | Decision | Reason |
| --- | ---: | ---: | --- | --- | --- | --- | ---: | --- | --- |
| `phase9j_white_tiger_ion71_deterministic` | 1 | 1 | REVIEW_ONLY | REVIEW | PEAK_EVIDENCE_FAILURE |  | 12 | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `phase9j_white_tiger_ion71_model_enabled` | 1 | 1 | REVIEW_ONLY | PASS | PEAK_EVIDENCE_FAILURE |  | 12 | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `phase9j_bench_01_mz71_screenshot_page_deterministic` | 2 | 1 | BLOCKED | REVIEW | TICK_LOCALIZATION_FAILURE | Y_CALIBRATION | 0 | BLOCKED | Y calibration blocked by insufficient usable anchors after OCR/tick pairing. |
| `phase9j_bench_01_mz71_screenshot_page_model_enabled` | 2 | 1 | BLOCKED | REVIEW | TICK_LOCALIZATION_FAILURE | Y_CALIBRATION | 0 | BLOCKED | Y calibration blocked by insufficient usable anchors after OCR/tick pairing. |
| `phase9j_bench_02_mz92_belyi_tigr_deterministic` | 1 | 1 | REVIEW_ONLY | REVIEW | GRAPH_PANEL_FAILURE |  | 1 | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `phase9j_bench_02_mz92_belyi_tigr_model_enabled` | 1 | 1 | REVIEW_ONLY | PASS | GRAPH_PANEL_FAILURE |  | 1 | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `phase9j_bench_03_small_tic_export_deterministic` | 1 | 1 | REVIEW_ONLY | REVIEW | PEAK_EVIDENCE_FAILURE |  | 3 | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `phase9j_bench_03_small_tic_export_model_enabled` | 1 | 1 | REVIEW_ONLY | PASS | PEAK_EVIDENCE_FAILURE |  | 3 | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `phase9j_bench_04_stacked_xic_resolution_deterministic` | 4 | 1 | REVIEW_ONLY | REVIEW | VLM_SEMANTIC_LAYER_UNAVAILABLE |  | 1 | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `phase9j_bench_04_stacked_xic_resolution_model_enabled` | 4 | 1 | REVIEW_ONLY | PASS | UNKNOWN_FAILURE |  | 1 | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `phase9j_bench_05_tic_plus_ions_deterministic` | 4 | 1 | BLOCKED | REVIEW | CALIBRATION_FAILURE | Y_CALIBRATION | 0 | BLOCKED | TIC+ions layout not propagated and Y calibration direction is inconsistent. |
| `phase9j_bench_05_tic_plus_ions_model_enabled` | 4 | 1 | BLOCKED | REVIEW | CALIBRATION_FAILURE | Y_CALIBRATION | 0 | BLOCKED | TIC+ions layout not propagated and Y calibration direction is inconsistent. |
| `phase9j_bench_06_photo_two_graphs_page_deterministic` | 2 | 1 | REVIEW_ONLY | REVIEW | VLM_SEMANTIC_LAYER_UNAVAILABLE |  | 1 | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `phase9j_bench_06_photo_two_graphs_page_model_enabled` | 2 | 1 | REVIEW_ONLY | PASS | UNKNOWN_FAILURE |  | 1 | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `phase9j_bench_07_rotated_page_photo_deterministic` | 1 | 1 | REVIEW_ONLY | REVIEW | VLM_SEMANTIC_LAYER_UNAVAILABLE |  | 1 | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |
| `phase9j_bench_07_rotated_page_photo_model_enabled` | 1 | 1 | REVIEW_ONLY | PASS | UNKNOWN_FAILURE |  | 1 | REVIEW | Produces report/peaks but evidence gate remains review-grade; not release-ready. |

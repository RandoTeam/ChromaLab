# R11 Runtime Calibration Closure Candidate

Verdict: `R11_RUNTIME_CALIBRATION_CLOSURE_CANDIDATE_REVIEW`

Production impact: `NONE_SHADOW_ONLY`

Records: `8`
Fixtures: `8`
Graph-count pass: `8/8`
Layout-class pass: `8/8`
Selected calibration graphs: `12`
Accepted bridge anchors: `155`
Rejected bridge anchors: `20`
Missing source crop files: `155`

R11 feeds R10 runtime OCR bridge rows into shadow calibration strategy fits.
It remains shadow-only and does not change Android runtime behavior, validators, report gates, chromatographic math, model policy, or CalculationEngine.

Contact sheet: `benchmark/reports/r11_runtime_calibration_closure_candidate/contact_sheet_runtime_calibration_closure.png`

## Fixture Results

| Fixture | Graphs | Layout | Status | Selected calibration graphs | Accepted anchors | Missing crop files | Target blocker note | Overlay | Detail |
|---|---:|---|---|---:|---:|---:|---|---|---|
| `bench_01_mz71_screenshot_page` | 2 | `MULTI_PANEL_SEPARATE_AXES` | `REVIEW` | 2 | 25 | 25 | R10 bridge rows produce review-grade graph calibration fits; Android Y-calibration remains blocked until equivalent runtime OCR rows and crop files are exported. | `benchmark/reports/r11_runtime_calibration_closure_candidate/overlays/bench_01_mz71_screenshot_page_runtime_calibration_closure_overlay.png` | `benchmark/reports/r11_runtime_calibration_closure_candidate/details/bench_01_mz71_screenshot_page_runtime_calibration_closure_detail.json` |
| `bench_02_mz92_belyi_tigr` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | `MISSING` | 0 | 0 | 0 | - | `benchmark/reports/r11_runtime_calibration_closure_candidate/overlays/bench_02_mz92_belyi_tigr_runtime_calibration_closure_overlay.png` | `benchmark/reports/r11_runtime_calibration_closure_candidate/details/bench_02_mz92_belyi_tigr_runtime_calibration_closure_detail.json` |
| `bench_03_small_tic_export` | 1 | `LOW_RES_EXPORT_GRAPH` | `MISSING` | 0 | 0 | 0 | - | `benchmark/reports/r11_runtime_calibration_closure_candidate/overlays/bench_03_small_tic_export_runtime_calibration_closure_overlay.png` | `benchmark/reports/r11_runtime_calibration_closure_candidate/details/bench_03_small_tic_export_runtime_calibration_closure_detail.json` |
| `bench_04_stacked_xic_resolution` | 4 | `MULTI_PANEL_SEPARATE_AXES` | `REVIEW` | 4 | 52 | 52 | - | `benchmark/reports/r11_runtime_calibration_closure_candidate/overlays/bench_04_stacked_xic_resolution_runtime_calibration_closure_overlay.png` | `benchmark/reports/r11_runtime_calibration_closure_candidate/details/bench_04_stacked_xic_resolution_runtime_calibration_closure_detail.json` |
| `bench_05_tic_plus_ions` | 4 | `TIC_PLUS_ION_PANELS` | `REVIEW` | 4 | 40 | 40 | R10 bridge rows produce review-grade graph calibration fits for TIC+ions panels; Android layout propagation and Y-calibration evidence remain runtime blockers. | `benchmark/reports/r11_runtime_calibration_closure_candidate/overlays/bench_05_tic_plus_ions_runtime_calibration_closure_overlay.png` | `benchmark/reports/r11_runtime_calibration_closure_candidate/details/bench_05_tic_plus_ions_runtime_calibration_closure_detail.json` |
| `bench_06_photo_two_graphs_page` | 2 | `TWO_GRAPH_PAGE` | `REVIEW` | 2 | 38 | 38 | - | `benchmark/reports/r11_runtime_calibration_closure_candidate/overlays/bench_06_photo_two_graphs_page_runtime_calibration_closure_overlay.png` | `benchmark/reports/r11_runtime_calibration_closure_candidate/details/bench_06_photo_two_graphs_page_runtime_calibration_closure_detail.json` |
| `bench_07_rotated_page_photo` | 1 | `ROTATED_PAGE_GRAPH` | `MISSING` | 0 | 0 | 0 | - | `benchmark/reports/r11_runtime_calibration_closure_candidate/overlays/bench_07_rotated_page_photo_runtime_calibration_closure_overlay.png` | `benchmark/reports/r11_runtime_calibration_closure_candidate/details/bench_07_rotated_page_photo_runtime_calibration_closure_detail.json` |
| `white_tiger_ion71` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | `MISSING` | 0 | 0 | 0 | - | `benchmark/reports/r11_runtime_calibration_closure_candidate/overlays/white_tiger_ion71_runtime_calibration_closure_overlay.png` | `benchmark/reports/r11_runtime_calibration_closure_candidate/details/white_tiger_ion71_runtime_calibration_closure_detail.json` |

## Target Blocker Findings

- `bench_01_mz71_screenshot_page`: R10 bridge rows produce review-grade graph calibration fits; Android Y-calibration remains blocked until equivalent runtime OCR rows and crop files are exported.
- `bench_05_tic_plus_ions`: R10 bridge rows produce review-grade graph calibration fits for TIC+ions panels; Android layout propagation and Y-calibration evidence remain runtime blockers.

## Next Required Work

- Produce equivalent OCR anchor rows from Android runtime evidence packages, not only R10 benchmark rows.
- Persist source crop image files for accepted anchors before promotion.
- Compare shadow-selected calibration strategies with Android RuntimeEvidencePackage selected/rejected strategy export.
- Keep E2B advisory-only and unable to alter calibration strategy selection or metrics.

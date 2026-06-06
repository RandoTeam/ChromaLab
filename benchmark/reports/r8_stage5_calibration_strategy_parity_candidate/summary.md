# R8 Stage 5 Calibration Strategy Parity Candidate

Verdict: `R8_STAGE5_CALIBRATION_STRATEGY_PARITY_REVIEW`

Production impact: `NONE_SHADOW_ONLY`

Records: `8`
Fixtures: `8`
Graph-count pass: `8/8`
Layout-class pass: `8/8`
Annotated truth fixtures: `4`
Selected calibration graph count: `12`
Annotated anchor count: `194`
Selected X mean RMSE: `0.000287`
Selected Y mean RMSE: `0.231479`

R8 consumes R7 axis/frame/scale evidence and DR-C4 manual-review tick/text annotations.
It compares shadow calibration strategies and keeps manual anchors as scoring truth only.
It does not change Android runtime behavior, validators, report gates, chromatographic math, model policy, or CalculationEngine.

Contact sheet: `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/contact_sheet_calibration_strategy.png`

## Fixture Results

| Fixture | Graphs | Layout | Selected graphs | Anchors | X RMSE | Y RMSE | Overlay | Detail |
|---|---:|---|---:|---:|---:|---:|---|---|
| `bench_01_mz71_screenshot_page` | 2 | `MULTI_PANEL_SEPARATE_AXES` | 2 | 30 | 0.0 | 0.002577 | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/overlays/bench_01_mz71_screenshot_page_calibration_strategy_overlay.png` | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/details/bench_01_mz71_screenshot_page_calibration_strategy_detail.json` |
| `bench_02_mz92_belyi_tigr` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0 | -1.0 | -1.0 | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/overlays/bench_02_mz92_belyi_tigr_calibration_strategy_overlay.png` | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/details/bench_02_mz92_belyi_tigr_calibration_strategy_detail.json` |
| `bench_03_small_tic_export` | 1 | `LOW_RES_EXPORT_GRAPH` | 0 | 0 | -1.0 | -1.0 | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/overlays/bench_03_small_tic_export_calibration_strategy_overlay.png` | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/details/bench_03_small_tic_export_calibration_strategy_detail.json` |
| `bench_04_stacked_xic_resolution` | 4 | `MULTI_PANEL_SEPARATE_AXES` | 4 | 56 | 0.000863 | 0.0 | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/overlays/bench_04_stacked_xic_resolution_calibration_strategy_overlay.png` | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/details/bench_04_stacked_xic_resolution_calibration_strategy_detail.json` |
| `bench_05_tic_plus_ions` | 4 | `TIC_PLUS_ION_PANELS` | 4 | 56 | 5.6e-05 | 0.0 | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/overlays/bench_05_tic_plus_ions_calibration_strategy_overlay.png` | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/details/bench_05_tic_plus_ions_calibration_strategy_detail.json` |
| `bench_06_photo_two_graphs_page` | 2 | `TWO_GRAPH_PAGE` | 2 | 52 | 0.000229 | 0.923339 | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/overlays/bench_06_photo_two_graphs_page_calibration_strategy_overlay.png` | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/details/bench_06_photo_two_graphs_page_calibration_strategy_detail.json` |
| `bench_07_rotated_page_photo` | 1 | `ROTATED_PAGE_GRAPH` | 0 | 0 | -1.0 | -1.0 | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/overlays/bench_07_rotated_page_photo_calibration_strategy_overlay.png` | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/details/bench_07_rotated_page_photo_calibration_strategy_detail.json` |
| `white_tiger_ion71` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0 | -1.0 | -1.0 | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/overlays/white_tiger_ion71_calibration_strategy_overlay.png` | `benchmark/reports/r8_stage5_calibration_strategy_parity_candidate/details/white_tiger_ion71_calibration_strategy_detail.json` |

## Next Required Work

- Keep R8 calibration strategy parity shadow-only until automatic OCR/runtime anchors are measured.
- Use selected/rejected strategy tables to guide runtime calibration ensemble parity.
- Do not treat DRC4 manual-review anchor fits as production calibration evidence.
- Do not promote to Android runtime until calibration candidates are produced from automatic evidence.

# R7 Stage 4 Axis, Frame, And Scale Evidence Candidate

Verdict: `R7_STAGE4_AXIS_FRAME_SCALE_EVIDENCE_REVIEW`

Production impact: `NONE_SHADOW_ONLY`

Records: `8`
Fixtures: `8`
Graph-count pass: `8/8`
Layout-class pass: `8/8`
Annotated truth fixtures: `4`
Manual-review scale graph count: `12`
Mean x-axis support: `0.519346`
Mean y-axis support: `0.45788`
Annotated x-axis mean pixel error: `21.482143`
Annotated y-axis mean pixel error: `110.5`

R7 consumes R6 plotArea/layout candidates and DR-C4 manual-review tick/text annotations.
It adds PC-side axis/frame/grid/label-band evidence and manual-review scale-fit scoring where annotation truth exists.
It does not change Android runtime behavior, validators, report gates, chromatographic math, model policy, or CalculationEngine.

Contact sheet: `benchmark/reports/r7_stage4_axis_frame_scale_candidate/contact_sheet_axis_frame_scale.png`

## Fixture Results

| Fixture | Graphs | Layout | Scale graphs | X support | Y support | X error | Y error | Overlay | Detail |
|---|---:|---|---:|---:|---:|---:|---:|---|---|
| `bench_01_mz71_screenshot_page` | 2 | `MULTI_PANEL_SEPARATE_AXES` | 2 | 0.304077 | 0.089208 | 16.928572 | 158.0 | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/overlays/bench_01_mz71_screenshot_page_axis_frame_scale_overlay.png` | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/details/bench_01_mz71_screenshot_page_axis_frame_scale_detail.json` |
| `bench_02_mz92_belyi_tigr` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0.615707 | 1.0 | -1.0 | -1.0 | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/overlays/bench_02_mz92_belyi_tigr_axis_frame_scale_overlay.png` | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/details/bench_02_mz92_belyi_tigr_axis_frame_scale_detail.json` |
| `bench_03_small_tic_export` | 1 | `LOW_RES_EXPORT_GRAPH` | 0 | 0.449367 | 0.147917 | -1.0 | -1.0 | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/overlays/bench_03_small_tic_export_axis_frame_scale_overlay.png` | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/details/bench_03_small_tic_export_axis_frame_scale_detail.json` |
| `bench_04_stacked_xic_resolution` | 4 | `MULTI_PANEL_SEPARATE_AXES` | 4 | 0.692673 | 0.188661 | 1.75 | 15.75 | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/overlays/bench_04_stacked_xic_resolution_axis_frame_scale_overlay.png` | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/details/bench_04_stacked_xic_resolution_axis_frame_scale_detail.json` |
| `bench_05_tic_plus_ions` | 4 | `TIC_PLUS_ION_PANELS` | 4 | 0.389771 | 0.10458 | 0.75 | 25.75 | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/overlays/bench_05_tic_plus_ions_axis_frame_scale_overlay.png` | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/details/bench_05_tic_plus_ions_axis_frame_scale_detail.json` |
| `bench_06_photo_two_graphs_page` | 2 | `TWO_GRAPH_PAGE` | 2 | 0.697805 | 0.64773 | 66.5 | 242.5 | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/overlays/bench_06_photo_two_graphs_page_axis_frame_scale_overlay.png` | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/details/bench_06_photo_two_graphs_page_axis_frame_scale_detail.json` |
| `bench_07_rotated_page_photo` | 1 | `ROTATED_PAGE_GRAPH` | 0 | 0.390451 | 0.484943 | -1.0 | -1.0 | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/overlays/bench_07_rotated_page_photo_axis_frame_scale_overlay.png` | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/details/bench_07_rotated_page_photo_axis_frame_scale_detail.json` |
| `white_tiger_ion71` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0.61492 | 1.0 | -1.0 | -1.0 | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/overlays/white_tiger_ion71_axis_frame_scale_overlay.png` | `benchmark/reports/r7_stage4_axis_frame_scale_candidate/details/white_tiger_ion71_axis_frame_scale_detail.json` |

## Next Required Work

- Keep R7 axis/frame/scale evidence shadow-only until automatic OCR/scale anchors are measured.
- Use R7 support and alignment metrics to guide Stage 5 calibration strategy parity.
- Do not treat manual-review DRC4 anchor fits as runtime calibration evidence.
- Do not promote to Android runtime until axis/frame/scale output beats or equals current evidence gates.

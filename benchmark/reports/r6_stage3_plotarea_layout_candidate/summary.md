# R6 Stage 3 PlotArea And Layout Candidate

Verdict: `R6_STAGE3_LAYOUT_READY_PLOTAREA_REVIEW`

Production impact: `NONE_SHADOW_ONLY`

Records: `8`
Fixtures: `8`
Graph-count pass: `8/8`
Layout-class pass: `8/8`
Annotated truth fixtures: `4`
Annotated graphPanel mean IoU: `0.739372`
Annotated plotArea mean IoU: `0.62146`

R6 consumes R5 graphPanel candidates and partial manual P0 layout annotations.
It adds PC-side plotArea candidates and graph-layout classification. It does not change Android runtime behavior, validators, report gates, chromatographic math, model policy, or CalculationEngine.

Contact sheet: `benchmark/reports/r6_stage3_plotarea_layout_candidate/contact_sheet_plotarea_layout.png`

## Fixture Results

| Fixture | Expected graphs | Detected graphs | Layout | Layout score | PlotArea IoU | Overlay | Detail |
|---|---:|---:|---|---|---:|---|---|
| `bench_01_mz71_screenshot_page` | 2 | 2 | `MULTI_PANEL_SEPARATE_AXES` | PASS | 0.508 | `benchmark/reports/r6_stage3_plotarea_layout_candidate/overlays/bench_01_mz71_screenshot_page_plotarea_layout_overlay.png` | `benchmark/reports/r6_stage3_plotarea_layout_candidate/details/bench_01_mz71_screenshot_page_plotarea_layout_detail.json` |
| `bench_02_mz92_belyi_tigr` | 1 | 1 | `SINGLE_TRACE_SINGLE_AXIS` | PASS | - | `benchmark/reports/r6_stage3_plotarea_layout_candidate/overlays/bench_02_mz92_belyi_tigr_plotarea_layout_overlay.png` | `benchmark/reports/r6_stage3_plotarea_layout_candidate/details/bench_02_mz92_belyi_tigr_plotarea_layout_detail.json` |
| `bench_03_small_tic_export` | 1 | 1 | `LOW_RES_EXPORT_GRAPH` | PASS | - | `benchmark/reports/r6_stage3_plotarea_layout_candidate/overlays/bench_03_small_tic_export_plotarea_layout_overlay.png` | `benchmark/reports/r6_stage3_plotarea_layout_candidate/details/bench_03_small_tic_export_plotarea_layout_detail.json` |
| `bench_04_stacked_xic_resolution` | 4 | 4 | `MULTI_PANEL_SEPARATE_AXES` | PASS | 0.856 | `benchmark/reports/r6_stage3_plotarea_layout_candidate/overlays/bench_04_stacked_xic_resolution_plotarea_layout_overlay.png` | `benchmark/reports/r6_stage3_plotarea_layout_candidate/details/bench_04_stacked_xic_resolution_plotarea_layout_detail.json` |
| `bench_05_tic_plus_ions` | 4 | 4 | `TIC_PLUS_ION_PANELS` | PASS | 0.771 | `benchmark/reports/r6_stage3_plotarea_layout_candidate/overlays/bench_05_tic_plus_ions_plotarea_layout_overlay.png` | `benchmark/reports/r6_stage3_plotarea_layout_candidate/details/bench_05_tic_plus_ions_plotarea_layout_detail.json` |
| `bench_06_photo_two_graphs_page` | 2 | 2 | `TWO_GRAPH_PAGE` | PASS | 0.351 | `benchmark/reports/r6_stage3_plotarea_layout_candidate/overlays/bench_06_photo_two_graphs_page_plotarea_layout_overlay.png` | `benchmark/reports/r6_stage3_plotarea_layout_candidate/details/bench_06_photo_two_graphs_page_plotarea_layout_detail.json` |
| `bench_07_rotated_page_photo` | 1 | 1 | `ROTATED_PAGE_GRAPH` | PASS | - | `benchmark/reports/r6_stage3_plotarea_layout_candidate/overlays/bench_07_rotated_page_photo_plotarea_layout_overlay.png` | `benchmark/reports/r6_stage3_plotarea_layout_candidate/details/bench_07_rotated_page_photo_plotarea_layout_detail.json` |
| `white_tiger_ion71` | 1 | 1 | `SINGLE_TRACE_SINGLE_AXIS` | PASS | - | `benchmark/reports/r6_stage3_plotarea_layout_candidate/overlays/white_tiger_ion71_plotarea_layout_overlay.png` | `benchmark/reports/r6_stage3_plotarea_layout_candidate/details/white_tiger_ion71_plotarea_layout_detail.json` |

## Next Required Work

- Keep R6 plotArea/layout shadow-only until axis/scale evidence is measured.
- Use R6 candidates to drive Stage 4 axis and scale evidence, not Android runtime promotion.
- Improve plotArea localization for photo/page cases before production integration.
- Do not treat layout pass as calibration, trace, peak, or report readiness.

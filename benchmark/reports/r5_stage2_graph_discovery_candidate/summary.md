# R5 Stage 2 Graph Discovery Candidate

Verdict: `R5_STAGE2_GRAPH_DISCOVERY_COUNT_READY_FOR_STAGE3_SHADOW`

Production impact: `NONE_SHADOW_ONLY`

Records: `8`
Fixtures: `8`
Graph-count pass: `8/8`

R5 consumes R4 Rust Stage 1 evidence and builds PC-side graphPanel candidate evidence.
It does not change Android runtime behavior, validators, report gates, graph-count metadata, chromatographic math, model policy, or CalculationEngine.

Graph count is scored here. GraphPanel localization remains candidate-only until Stage 3 IoU/plotArea scoring.

Contact sheet: `benchmark/reports/r5_stage2_graph_discovery_candidate/contact_sheet_graph_discovery.png`

## Fixture Results

| Fixture | Expected graphs | Detected graphs | Graph count | Selected rows | R4 variant | Overlay |
|---|---:|---:|---|---|---|---|
| `bench_01_mz71_screenshot_page` | 2 | 2 | PASS | 715, 1112 | `sharpened_autocontrast` | `benchmark/reports/r5_stage2_graph_discovery_candidate/overlays/bench_01_mz71_screenshot_page_graph_discovery_overlay.png` |
| `bench_02_mz92_belyi_tigr` | 1 | 1 | PASS | 903 | `sharpened_autocontrast` | `benchmark/reports/r5_stage2_graph_discovery_candidate/overlays/bench_02_mz92_belyi_tigr_graph_discovery_overlay.png` |
| `bench_03_small_tic_export` | 1 | 1 | PASS | 99 | `autocontrast` | `benchmark/reports/r5_stage2_graph_discovery_candidate/overlays/bench_03_small_tic_export_graph_discovery_overlay.png` |
| `bench_04_stacked_xic_resolution` | 4 | 4 | PASS | 226, 496, 779, 1058 | `autocontrast` | `benchmark/reports/r5_stage2_graph_discovery_candidate/overlays/bench_04_stacked_xic_resolution_graph_discovery_overlay.png` |
| `bench_05_tic_plus_ions` | 4 | 4 | PASS | 362, 506, 644, 783 | `autocontrast` | `benchmark/reports/r5_stage2_graph_discovery_candidate/overlays/bench_05_tic_plus_ions_graph_discovery_overlay.png` |
| `bench_06_photo_two_graphs_page` | 2 | 2 | PASS | 622, 1237 | `sharpened_autocontrast` | `benchmark/reports/r5_stage2_graph_discovery_candidate/overlays/bench_06_photo_two_graphs_page_graph_discovery_overlay.png` |
| `bench_07_rotated_page_photo` | 1 | 1 | PASS | 963 | `sharpened_autocontrast` | `benchmark/reports/r5_stage2_graph_discovery_candidate/overlays/bench_07_rotated_page_photo_graph_discovery_overlay.png` |
| `white_tiger_ion71` | 1 | 1 | PASS | 838 | `sharpened_autocontrast` | `benchmark/reports/r5_stage2_graph_discovery_candidate/overlays/white_tiger_ion71_graph_discovery_overlay.png` |

## Next Required Work

- Keep R5 graph discovery shadow-only until Stage 3 plotArea/layout semantics are measured.
- Add Stage 3 plotArea and semantic layout candidate before any Android runtime promotion.
- Do not treat R5 graph-count pass as calibration or report readiness.
- Keep Android runtime unchanged until Stage 1-3 promotion gates pass.

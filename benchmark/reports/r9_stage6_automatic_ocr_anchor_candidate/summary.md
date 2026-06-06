# R9 Stage 6 Automatic OCR Anchor Candidate

Verdict: `R9_STAGE6_AUTOMATIC_OCR_ANCHOR_CANDIDATE_REVIEW`

Production impact: `NONE_SHADOW_ONLY`

Records: `8`
Fixtures: `8`
Graph-count pass: `8/8`
Layout-class pass: `8/8`
Annotated truth fixtures: `4`
Automatic OCR candidate graphs: `12`
Valid candidate graphs: `9`
Review candidate graphs: `3`
Accepted OCR anchors: `155`
Rejected OCR anchors: `20`
Mean fit RMSE px: `0.725662`
Mean truth tick RMSE px: `12.601638`

R9 consumes R8 calibration strategy evidence and DRD/DRE OCR benchmark outputs.
It measures automatic OCR anchor readiness from PC-side safe OCR evidence and keeps it shadow-only.
It does not change Android runtime behavior, validators, report gates, chromatographic math, model policy, or CalculationEngine.

Contact sheet: `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/contact_sheet_automatic_ocr_anchor_candidate.png`

## Fixture Results

| Fixture | Graphs | Layout | OCR candidate graphs | Valid | Review | Anchors | Mean fit RMSE px | Mean truth tick RMSE px | Overlay | Detail |
|---|---:|---|---:|---:|---:|---:|---:|---:|---|---|
| `bench_01_mz71_screenshot_page` | 2 | `MULTI_PANEL_SEPARATE_AXES` | 2 | 2 | 0 | 25 | 1.513375 | 15.0924 | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/overlays/bench_01_mz71_screenshot_page_automatic_ocr_anchor_overlay.png` | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/details/bench_01_mz71_screenshot_page_automatic_ocr_anchor_detail.json` |
| `bench_02_mz92_belyi_tigr` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0 | 0 | 0 | -1.0 | -1.0 | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/overlays/bench_02_mz92_belyi_tigr_automatic_ocr_anchor_overlay.png` | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/details/bench_02_mz92_belyi_tigr_automatic_ocr_anchor_detail.json` |
| `bench_03_small_tic_export` | 1 | `LOW_RES_EXPORT_GRAPH` | 0 | 0 | 0 | 0 | -1.0 | -1.0 | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/overlays/bench_03_small_tic_export_automatic_ocr_anchor_overlay.png` | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/details/bench_03_small_tic_export_automatic_ocr_anchor_detail.json` |
| `bench_04_stacked_xic_resolution` | 4 | `MULTI_PANEL_SEPARATE_AXES` | 4 | 4 | 0 | 52 | 0.377188 | 7.0346 | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/overlays/bench_04_stacked_xic_resolution_automatic_ocr_anchor_overlay.png` | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/details/bench_04_stacked_xic_resolution_automatic_ocr_anchor_detail.json` |
| `bench_05_tic_plus_ions` | 4 | `TIC_PLUS_ION_PANELS` | 4 | 1 | 3 | 40 | 0.198962 | 12.98115 | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/overlays/bench_05_tic_plus_ions_automatic_ocr_anchor_overlay.png` | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/details/bench_05_tic_plus_ions_automatic_ocr_anchor_detail.json` |
| `bench_06_photo_two_graphs_page` | 2 | `TWO_GRAPH_PAGE` | 2 | 2 | 0 | 38 | 0.813125 | 15.2984 | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/overlays/bench_06_photo_two_graphs_page_automatic_ocr_anchor_overlay.png` | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/details/bench_06_photo_two_graphs_page_automatic_ocr_anchor_detail.json` |
| `bench_07_rotated_page_photo` | 1 | `ROTATED_PAGE_GRAPH` | 0 | 0 | 0 | 0 | -1.0 | -1.0 | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/overlays/bench_07_rotated_page_photo_automatic_ocr_anchor_overlay.png` | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/details/bench_07_rotated_page_photo_automatic_ocr_anchor_detail.json` |
| `white_tiger_ion71` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0 | 0 | 0 | -1.0 | -1.0 | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/overlays/white_tiger_ion71_automatic_ocr_anchor_overlay.png` | `benchmark/reports/r9_stage6_automatic_ocr_anchor_candidate/details/white_tiger_ion71_automatic_ocr_anchor_detail.json` |

## Next Required Work

- Keep R9 automatic OCR anchors shadow-only until the same anchor rows are produced by Android or Rust runtime.
- Bridge DRE6 safe OCR anchor generation into the replacement pipeline without adding duplicate stale layers.
- Require per-anchor provenance, forbidden-text rejection, residuals, and graph-level failure packages before runtime promotion.
- Do not promote OCR-derived scale anchors when there is no pixel geometry or when title/ion/m/z text is the source.

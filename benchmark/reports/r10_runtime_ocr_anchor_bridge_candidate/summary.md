# R10 Runtime OCR Anchor Bridge Candidate

Verdict: `R10_RUNTIME_OCR_ANCHOR_BRIDGE_CANDIDATE_REVIEW`

Production impact: `NONE_SHADOW_ONLY`

Records: `8`
Fixtures: `8`
Graph-count pass: `8/8`
Layout-class pass: `8/8`
Scoreable fixtures: `4`
Anchor-count parity pass: `4/4`
Bridge accepted anchors: `155`
Bridge rejected anchors: `20`
Missing source crop files: `155`

R10 converts R9 safe OCR anchor evidence into runtime-shaped rows and validates them through the Rust OCR anchor bridge contract.
It remains shadow-only and does not change Android runtime behavior, validators, report gates, chromatographic math, model policy, or CalculationEngine.

Contact sheet: `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/contact_sheet_runtime_ocr_anchor_bridge.png`

## Fixture Results

| Fixture | Graphs | Layout | R9 anchors | Bridge anchors | Rejected | Missing crop files | Parity | Overlay | Detail |
|---|---:|---|---:|---:|---:|---:|---|---|---|
| `bench_01_mz71_screenshot_page` | 2 | `MULTI_PANEL_SEPARATE_AXES` | 25 | 25 | 1 | 25 | `True` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/overlays/bench_01_mz71_screenshot_page_runtime_ocr_anchor_bridge_overlay.png` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/details/bench_01_mz71_screenshot_page_runtime_ocr_anchor_bridge_detail.json` |
| `bench_02_mz92_belyi_tigr` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0 | 0 | 0 | `True` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/overlays/bench_02_mz92_belyi_tigr_runtime_ocr_anchor_bridge_overlay.png` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/details/bench_02_mz92_belyi_tigr_runtime_ocr_anchor_bridge_detail.json` |
| `bench_03_small_tic_export` | 1 | `LOW_RES_EXPORT_GRAPH` | 0 | 0 | 0 | 0 | `True` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/overlays/bench_03_small_tic_export_runtime_ocr_anchor_bridge_overlay.png` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/details/bench_03_small_tic_export_runtime_ocr_anchor_bridge_detail.json` |
| `bench_04_stacked_xic_resolution` | 4 | `MULTI_PANEL_SEPARATE_AXES` | 52 | 52 | 0 | 52 | `True` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/overlays/bench_04_stacked_xic_resolution_runtime_ocr_anchor_bridge_overlay.png` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/details/bench_04_stacked_xic_resolution_runtime_ocr_anchor_bridge_detail.json` |
| `bench_05_tic_plus_ions` | 4 | `TIC_PLUS_ION_PANELS` | 40 | 40 | 10 | 40 | `True` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/overlays/bench_05_tic_plus_ions_runtime_ocr_anchor_bridge_overlay.png` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/details/bench_05_tic_plus_ions_runtime_ocr_anchor_bridge_detail.json` |
| `bench_06_photo_two_graphs_page` | 2 | `TWO_GRAPH_PAGE` | 38 | 38 | 9 | 38 | `True` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/overlays/bench_06_photo_two_graphs_page_runtime_ocr_anchor_bridge_overlay.png` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/details/bench_06_photo_two_graphs_page_runtime_ocr_anchor_bridge_detail.json` |
| `bench_07_rotated_page_photo` | 1 | `ROTATED_PAGE_GRAPH` | 0 | 0 | 0 | 0 | `True` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/overlays/bench_07_rotated_page_photo_runtime_ocr_anchor_bridge_overlay.png` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/details/bench_07_rotated_page_photo_runtime_ocr_anchor_bridge_detail.json` |
| `white_tiger_ion71` | 1 | `SINGLE_TRACE_SINGLE_AXIS` | 0 | 0 | 0 | 0 | `True` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/overlays/white_tiger_ion71_runtime_ocr_anchor_bridge_overlay.png` | `benchmark/reports/r10_runtime_ocr_anchor_bridge_candidate/details/white_tiger_ion71_runtime_ocr_anchor_bridge_detail.json` |

## Next Required Work

- Produce the same bridge rows from Android or direct Rust OCR/crop runtime, not only from R9 benchmark evidence.
- Persist real source crop image paths for accepted anchors before promotion.
- Feed bridge rows into calibration ensemble shadow comparison in R11.
- Keep VLM advisory-only and reject any model-derived pixel geometry or numeric calibration values.

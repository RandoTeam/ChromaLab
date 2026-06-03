# DR-E2 Robust Axis Scale Fit And Outlier Rejection Benchmark

Verdict: `ROBUST_AXIS_SCALE_FIT_IMPROVES_OUTLIERS_NOT_ACCEPTANCE_READY`
OCR variant: `rapidocr_rgb_x3_p2_v1`
Usable axes DR-E1 -> DR-E2: `15` -> `18`

## Graph Summary

| Fixture | Graph | DR-E1 | DR-E2 | X strategy | X status | X anchors | Y strategy | Y status | Y anchors |
| --- | --- | --- | --- | --- | --- | ---: | --- | --- | ---: |
| `bench_01_mz71_screenshot_page` | `bench_01_graph_1` | `VALID` | `VALID` | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 7 | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 7 |
| `bench_01_mz71_screenshot_page` | `bench_01_graph_2` | `PARTIAL` | `VALID` | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 5 | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 6 |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_1` | `VALID` | `VALID` | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 7 | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 6 |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_2` | `VALID` | `VALID` | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 7 | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 7 |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_3` | `PARTIAL` | `PARTIAL` | `RANSAC_RESIDUAL_TRIMMED_FIT` | `INVALID` | 0 | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 6 |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_4` | `VALID` | `VALID` | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 7 | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 5 |
| `bench_05_tic_plus_ions` | `bench_05_graph_1` | `VALID` | `VALID` | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 6 | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 5 |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | `PARTIAL` | `PARTIAL` | `RANSAC_RESIDUAL_TRIMMED_FIT` | `INVALID` | 0 | `RANSAC_RESIDUAL_TRIMMED_FIT` | `REVIEW` | 2 |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `INVALID` | `INVALID` | `RANSAC_RESIDUAL_TRIMMED_FIT` | `INVALID` | 0 | `RANSAC_RESIDUAL_TRIMMED_FIT` | `INVALID` | 1 |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | `PARTIAL` | `PARTIAL` | `RANSAC_RESIDUAL_TRIMMED_FIT` | `INVALID` | 0 | `RANSAC_RESIDUAL_TRIMMED_FIT` | `REVIEW` | 2 |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_1` | `PARTIAL` | `VALID` | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 10 | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 7 |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | `INVALID` | `PARTIAL` | `RANSAC_RESIDUAL_TRIMMED_FIT` | `INVALID` | 0 | `RANSAC_RESIDUAL_TRIMMED_FIT` | `VALID` | 10 |

## Counts

Axis statuses: `{'VALID': 16, 'INVALID': 6, 'REVIEW': 2}`
Graph decisions: `{'VALID': 7, 'PARTIAL': 4, 'INVALID': 1}`
Selected strategies: `{'RANSAC_RESIDUAL_TRIMMED_FIT': 24}`

## Interpretation

- Robust fitting can rescue high-residual and non-monotonic safe OCR evidence only when enough monotonic inliers exist.
- Two-anchor fallback is review-only, never release-ready.
- Missing anchors remain explicit missing evidence; no tick labels are fabricated.
- This remains a benchmark contract and does not change Android calibration.

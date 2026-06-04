# DR-E5 Repaired Crop Pipeline End-To-End Calibration Benchmark

Verdict: `REPAIRED_CROP_PIPELINE_IMPROVES_CALIBRATION_NOT_ACCEPTANCE_READY`
Baseline OCR variant: `rapidocr_rgb_x3_p2_v1`
Repaired OCR method: `rapidocr_repaired_x_label_band_rgb_x3_p2_v1`
Added repaired anchors: `40`
Usable axes DR-E2 -> DR-E5: `18` -> `22`
Usable graphs DR-E2 -> DR-E5: `7` -> `10`

## Graph Calibration Summary

| Fixture | Graph | DR-E2 | DR-E5 | Delta | X DR-E2 -> DR-E5 | X anchors | Y DR-E2 -> DR-E5 | Y anchors |
| --- | --- | --- | --- | --- | --- | ---: | --- | ---: |
| `bench_01_mz71_screenshot_page` | `bench_01_graph_1` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 7->7 | `VALID` -> `VALID` | 7->7 |
| `bench_01_mz71_screenshot_page` | `bench_01_graph_2` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 5->5 | `VALID` -> `VALID` | 6->6 |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_1` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 7->7 | `VALID` -> `VALID` | 6->6 |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_2` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 7->7 | `VALID` -> `VALID` | 7->7 |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_3` | `PARTIAL` | `VALID` | `IMPROVED` | `INVALID` -> `VALID` | 0->7 | `VALID` -> `VALID` | 6->6 |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_4` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 7->7 | `VALID` -> `VALID` | 5->5 |
| `bench_05_tic_plus_ions` | `bench_05_graph_1` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 6->6 | `VALID` -> `VALID` | 5->5 |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | `PARTIAL` | `REVIEW` | `IMPROVED` | `INVALID` -> `VALID` | 0->11 | `REVIEW` -> `REVIEW` | 2->2 |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `INVALID` | `PARTIAL` | `IMPROVED` | `INVALID` -> `VALID` | 0->10 | `INVALID` -> `INVALID` | 1->1 |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | `PARTIAL` | `REVIEW` | `IMPROVED` | `INVALID` -> `REVIEW` | 0->2 | `REVIEW` -> `REVIEW` | 2->2 |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_1` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 10->10 | `VALID` -> `VALID` | 7->7 |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | `PARTIAL` | `PARTIAL` | `UNCHANGED` | `INVALID` -> `INVALID` | 0->0 | `VALID` -> `VALID` | 10->10 |

## Repaired Axis Effects

| Fixture | Graph | Axis | Added repaired anchors | DR-E2 | DR-E5 | Accepted anchors | Failure reason |
| --- | --- | --- | ---: | --- | --- | ---: | --- |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_3` | `X` | 7 | `INVALID` | `VALID` | 7 | `` |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | `X` | 11 | `INVALID` | `VALID` | 11 | `` |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `X` | 11 | `INVALID` | `VALID` | 10 | `` |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | `X` | 11 | `INVALID` | `REVIEW` | 2 | `` |

## Remaining Partial Or Invalid Graphs

| Fixture | Graph | Decision | X status/reason | Y status/reason |
| --- | --- | --- | --- | --- |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `PARTIAL` | `VALID` / `` | `INVALID` / `INSUFFICIENT_SCALE_ANCHORS` |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | `PARTIAL` | `INVALID` / `INSUFFICIENT_SCALE_ANCHORS` | `VALID` / `` |

## Counts

Axis statuses: `{'VALID': 19, 'REVIEW': 3, 'INVALID': 2}`
Graph decisions: `{'VALID': 8, 'REVIEW': 2, 'PARTIAL': 2}`
Baseline graph decisions: `{'VALID': 7, 'PARTIAL': 4, 'INVALID': 1}`
Selected strategies: `{'RANSAC_RESIDUAL_TRIMMED_FIT': 24}`
Improved graphs: `4`
Regressed graphs: `0`

## Interpretation

- Repaired label-band crops are evaluated through the same robust-fit rules as DR-E2.
- Added OCR labels remain evidence only; they are not fabricated coordinates or calibration values.
- `bench_04_graph_3` and `bench_05_graph_2/3/4` become graph-level calibration candidates.
- `bench_06_graph_2` remains partial because the repaired crop coverage did not recover X anchors.
- This remains a benchmark prototype and does not alter Android runtime crop planning or calibration.

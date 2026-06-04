# DR-E6 Remaining Axis OCR Recovery Benchmark

Verdict: `REMAINING_AXIS_OCR_RECOVERY_ALL_TARGET_AXES_USABLE_NOT_RUNTIME_READY`
Usable axes DR-E5 -> DR-E6: `22` -> `24`
Usable graphs DR-E5 -> DR-E6: `10` -> `12`
Added target fallback anchors: `12`
Aggressive sweep safe target rows: `131`

## Target Axis Recovery

| Fixture | Graph | Axis | Truth labels | Recovered fallback anchors | Remaining missing | Recovered labels |
| --- | --- | --- | ---: | ---: | ---: | --- |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `Y` | 2 | 2 | 0 | `0, 10 000` |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | `X` | 11 | 11 | 0 | `5.00, 10.00, 15.00, 20.00, 25.00, 30.00, 35.00, 40.00, 45.00, 50.00, 55.00` |

## Graph Calibration Summary

| Fixture | Graph | DR-E5 | DR-E6 | Delta | X DR-E5 -> DR-E6 | X anchors | Y DR-E5 -> DR-E6 | Y anchors |
| --- | --- | --- | --- | --- | --- | ---: | --- | ---: |
| `bench_01_mz71_screenshot_page` | `bench_01_graph_1` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 7->7 | `VALID` -> `VALID` | 7->7 |
| `bench_01_mz71_screenshot_page` | `bench_01_graph_2` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 5->5 | `VALID` -> `VALID` | 6->6 |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_1` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 7->7 | `VALID` -> `VALID` | 6->6 |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_2` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 7->7 | `VALID` -> `VALID` | 7->7 |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_3` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 7->7 | `VALID` -> `VALID` | 6->6 |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_4` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 7->7 | `VALID` -> `VALID` | 5->5 |
| `bench_05_tic_plus_ions` | `bench_05_graph_1` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 6->6 | `VALID` -> `VALID` | 5->5 |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | `REVIEW` | `REVIEW` | `UNCHANGED` | `VALID` -> `VALID` | 11->11 | `REVIEW` -> `REVIEW` | 2->2 |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `PARTIAL` | `REVIEW` | `IMPROVED` | `VALID` -> `VALID` | 10->10 | `INVALID` -> `REVIEW` | 1->2 |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | `REVIEW` | `REVIEW` | `UNCHANGED` | `REVIEW` -> `REVIEW` | 2->2 | `REVIEW` -> `REVIEW` | 2->2 |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_1` | `VALID` | `VALID` | `UNCHANGED` | `VALID` -> `VALID` | 10->10 | `VALID` -> `VALID` | 7->7 |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | `PARTIAL` | `VALID` | `IMPROVED` | `INVALID` -> `VALID` | 0->11 | `VALID` -> `VALID` | 10->10 |

## Remaining Partial Or Invalid Graphs

| Fixture | Graph | Decision | X status/reason | Y status/reason |
| --- | --- | --- | --- | --- |

## Counts

Axis statuses: `{'VALID': 20, 'REVIEW': 4}`
Graph decisions: `{'VALID': 9, 'REVIEW': 3}`
Anchor sources: `{'DR_E1_BASELINE_SAFE_OCR': 123, 'DR_E4_REPAIRED_LABEL_BAND_SAFE_OCR': 40, 'DR_E6_TARGET_AXIS_OCR_FALLBACK': 12}`
Aggressive sweep crops: `22`
Aggressive sweep rows: `431`
Improved graphs: `2`
Regressed graphs: `0`

## Interpretation

- Per-axis OCR variant fallback recovers `bench_05_graph_3` Y calibration evidence.
- Aggressive photo X-band OCR recovers all 11 safe X anchors for `bench_06_graph_2`.
- All 12 PC-side graph calibration cases are now `VALID` or `REVIEW` under the existing robust-fit gate.
- The result is still not Android acceptance; it is evidence that the next implementation candidate should include per-axis OCR fallback and deep photo X-band crops.
- This remains a benchmark prototype and does not alter Android runtime crop planning or calibration.

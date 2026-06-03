# DR-E1 Axis Scale Candidate Builder From Safe Owned OCR

Verdict: `AXIS_SCALE_CANDIDATES_PARTIAL_SAFE_EVIDENCE_NOT_ACCEPTANCE_READY`
Best OCR variant: `rapidocr_rgb_x3_p2_v1`
Graphs: `12`
Safe anchors: `123`

## Graph Candidate Summary

| Fixture | Graph | X status | X anchors | X RMSE | Y status | Y anchors | Y RMSE | Decision |
| --- | --- | --- | ---: | ---: | --- | ---: | ---: | --- |
| `bench_01_mz71_screenshot_page` | `bench_01_graph_1` | `VALID` | 7 | 3.5466 | `VALID` | 7 | 0.3797 | `VALID` |
| `bench_01_mz71_screenshot_page` | `bench_01_graph_2` | `INVALID` | 6 | 103.7997 | `VALID` | 6 | 0.5994 | `PARTIAL` |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_1` | `VALID` | 7 | 0.4798 | `VALID` | 6 | 0.2378 | `VALID` |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_2` | `VALID` | 7 | 0.5613 | `VALID` | 7 | 0.2352 | `VALID` |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_3` | `INVALID` | 0 | - | `VALID` | 6 | 0.2071 | `PARTIAL` |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_4` | `VALID` | 7 | 0.4954 | `VALID` | 5 | 0.1894 | `VALID` |
| `bench_05_tic_plus_ions` | `bench_05_graph_1` | `VALID` | 6 | 0.5495 | `VALID` | 5 | 0.1997 | `VALID` |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | `INVALID` | 0 | - | `REVIEW` | 2 | 0.0 | `PARTIAL` |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `INVALID` | 0 | - | `INVALID` | 1 | - | `INVALID` |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | `INVALID` | 0 | - | `REVIEW` | 2 | 0.0 | `PARTIAL` |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_1` | `VALID` | 10 | 0.7423 | `INVALID` | 14 | - | `PARTIAL` |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | `INVALID` | 0 | - | `INVALID` | 12 | - | `INVALID` |

## Counts

Graph decisions: `{'VALID': 5, 'PARTIAL': 5, 'INVALID': 2}`
Axis statuses: `{'VALID': 13, 'INVALID': 9, 'REVIEW': 2}`

## Interpretation

- Safe owned OCR can build usable scale candidates on several axes.
- This is still benchmark evidence only; it does not alter Android calibration.
- Missing OCR labels remain explicit missing anchors, not fabricated tick values.
- The next step should score calibration candidates against truth and define release/review gates for incomplete safe OCR evidence.

# DR-E4 Label-Band Crop Coverage Repair Prototype

Verdict: `LABEL_BAND_COVERAGE_REPAIR_RECOVERS_SCALE_CANDIDATES`
Method: `rapidocr_repaired_x_label_band_rgb_x3_p2_v1`
Target X axes: `5`
Repaired crops: `35`
Recovered safe X anchors: `40`

## Axis Recovery Summary

| Fixture | Graph | Truth X labels | Baseline safe | Repaired safe | Recovered | Remaining missing |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_3` | 7 | 0 | 7 | 7 | 0 |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | 11 | 0 | 11 | 11 | 0 |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | 11 | 0 | 11 | 11 | 0 |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | 11 | 0 | 11 | 11 | 0 |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | 11 | 0 | 0 | 0 | 11 |

## Robust Fit After Repair

| Fixture | Graph | Baseline X | Repaired X | Repaired anchors | Failure reason |
| --- | --- | --- | --- | ---: | --- |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_3` | `INVALID` | `VALID` | 7 | `` |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | `INVALID` | `VALID` | 9 | `` |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `INVALID` | `VALID` | 9 | `` |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | `INVALID` | `REVIEW` | 3 | `` |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | `INVALID` | `INVALID` | 0 | `INSUFFICIENT_SCALE_ANCHORS` |

## Overlays

- `bench_05_tic_plus_ions`: `benchmark/reports/dre4_label_band_crop_coverage_repair/overlays/bench_05_tic_plus_ions_dre4_repaired_x_label_bands.png`
- `bench_06_photo_two_graphs_page`: `benchmark/reports/dre4_label_band_crop_coverage_repair/overlays/bench_06_photo_two_graphs_page_dre4_repaired_x_label_bands.png`
- `bench_04_stacked_xic_resolution`: `benchmark/reports/dre4_label_band_crop_coverage_repair/overlays/bench_04_stacked_xic_resolution_dre4_repaired_x_label_bands.png`

## Interpretation

- Extending X label bands below tight graph panels can recover anchors that DR-E3 classified as coverage gaps.
- Overlapping dense tiles are now measured separately from wide crops.
- Recovered anchors are still OCR evidence only; robust fit must accept them before calibration can use them.
- This remains a benchmark prototype and does not alter Android runtime crop planning.

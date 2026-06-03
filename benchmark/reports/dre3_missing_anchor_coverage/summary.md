# DR-E3 Missing Anchor Recovery And Label-Band Coverage Benchmark

Verdict: `MISSING_ANCHOR_RECOVERY_BLOCKED_BY_CROP_COVERAGE`
OCR variant: `rapidocr_rgb_x3_p2_v1`
Target non-valid axes: `8`
Missing anchors inspected: `52`

## Axis Root Cause Summary

| Fixture | Graph | Axis | Truth labels | Safe anchors | Missing | Primary root cause | Recommended fix |
| --- | --- | --- | ---: | ---: | ---: | --- | --- |
| `bench_04_stacked_xic_resolution` | `bench_04_graph_3` | `X` | 7 | 0 | 7 | `CROP_COVERAGE_MISSING` | repair axis label-band crop coverage |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | `X` | 11 | 0 | 11 | `OCR_DETECTION_EMPTY_FOR_CROP` | split or enhance dense label-band OCR crops |
| `bench_05_tic_plus_ions` | `bench_05_graph_2` | `Y` | 2 | 2 | 0 | `NO_MISSING_ANCHORS` | none |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `X` | 11 | 0 | 11 | `CROP_COVERAGE_MISSING` | repair axis label-band crop coverage |
| `bench_05_tic_plus_ions` | `bench_05_graph_3` | `Y` | 2 | 1 | 1 | `OCR_ROLE_OR_SAFETY_REJECTED` | repair text role/safety classifier for owned labels |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | `X` | 11 | 0 | 11 | `OCR_DETECTION_EMPTY_FOR_CROP` | split or enhance dense label-band OCR crops |
| `bench_05_tic_plus_ions` | `bench_05_graph_4` | `Y` | 2 | 2 | 0 | `NO_MISSING_ANCHORS` | none |
| `bench_06_photo_two_graphs_page` | `bench_06_graph_2` | `X` | 11 | 0 | 11 | `OCR_DETECTION_EMPTY_FOR_CROP` | split or enhance dense label-band OCR crops |

## Root Cause Counts

`{'CROP_COVERAGE_MISSING': 18, 'OCR_DETECTION_EMPTY_FOR_CROP': 33, 'OCR_ROLE_OR_SAFETY_REJECTED': 1}`

## Interpretation

- Remaining blockers are mostly missing safe OCR anchors, not robust-fit failures.
- Covered labels with no OCR detection point to crop subdivision/preprocessing gaps.
- Nearby detections that do not match point to OCR box-to-label matching gaps.
- No calibration labels are fabricated; every missing anchor remains explicit evidence.

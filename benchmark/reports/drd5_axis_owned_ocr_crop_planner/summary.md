# DR-D5 Axis-Owned OCR Crop Planner Prototype

Verdict: `AXIS_OWNED_OCR_CROP_PLANNER_IMPROVES_SCOPE_NOT_ACCEPTANCE_READY`
Fixtures: `4`
Crop plans: `34`
Owned truth labels: `210`

## Method Summary

| Method | Owned recall | Tick recall | Safe role accuracy | Safe false ticks | Rejected unmatched numeric | Mean crop time |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `rapidocr_axis_owned_crops_x2_v1` | 0.7048 | 0.7049 | 0.8581 | 0 | 12 | 1.5400s |
| `easyocr_en_axis_owned_crops_x2_v1` | 0.7190 | 0.7104 | 0.6424 | 0 | 20 | 0.7091s |

## Fixture Summary

| Fixture | Method | Crops | Owned recall | Tick recall | Safe role accuracy | Rejected unmatched numeric |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| `bench_01_mz71_screenshot_page` | `rapidocr_axis_owned_crops_x2_v1` | 6 | 0.8824 | 0.8667 | 0.9333 | 1 |
| `bench_04_stacked_xic_resolution` | `rapidocr_axis_owned_crops_x2_v1` | 10 | 0.7937 | 0.8036 | 0.9000 | 9 |
| `bench_05_tic_plus_ions` | `rapidocr_axis_owned_crops_x2_v1` | 12 | 0.5088 | 0.4667 | 0.6207 | 0 |
| `bench_06_photo_two_graphs_page` | `rapidocr_axis_owned_crops_x2_v1` | 6 | 0.6964 | 0.7115 | 0.9231 | 2 |
| `bench_01_mz71_screenshot_page` | `easyocr_en_axis_owned_crops_x2_v1` | 6 | 0.8824 | 0.8667 | 0.8667 | 6 |
| `bench_04_stacked_xic_resolution` | `easyocr_en_axis_owned_crops_x2_v1` | 10 | 0.7778 | 0.7679 | 0.8776 | 10 |
| `bench_05_tic_plus_ions` | `easyocr_en_axis_owned_crops_x2_v1` | 12 | 0.5263 | 0.4889 | 0.2667 | 3 |
| `bench_06_photo_two_graphs_page` | `easyocr_en_axis_owned_crops_x2_v1` | 6 | 0.7500 | 0.7500 | 0.4762 | 1 |

## Crop Plan Overlays

- `bench_01_mz71_screenshot_page`: `benchmark/reports/drd5_axis_owned_ocr_crop_planner/overlays/bench_01_mz71_screenshot_page_axis_owned_crop_plan_overlay.png`
- `bench_04_stacked_xic_resolution`: `benchmark/reports/drd5_axis_owned_ocr_crop_planner/overlays/bench_04_stacked_xic_resolution_axis_owned_crop_plan_overlay.png`
- `bench_05_tic_plus_ions`: `benchmark/reports/drd5_axis_owned_ocr_crop_planner/overlays/bench_05_tic_plus_ions_axis_owned_crop_plan_overlay.png`
- `bench_06_photo_two_graphs_page`: `benchmark/reports/drd5_axis_owned_ocr_crop_planner/overlays/bench_06_photo_two_graphs_page_axis_owned_crop_plan_overlay.png`

## Interpretation

- Axis-owned crops reduce OCR scope and preserve deterministic ownership rules.
- This prototype still uses DR-C3 annotation geometry, so it is not Android runtime integration.
- Numeric OCR boxes without matched geometry-owned labels are rejected before calibration.
- The next implementation work must replace annotation geometry with automatic graph/axis label-band crop generation.

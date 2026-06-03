# DR-D5 Axis-Owned OCR Crop Planner Prototype

Status: `DR_D5_COMPLETE_AXIS_OWNED_OCR_CROP_PLANNER_IMPROVES_SCOPE_NOT_ACCEPTANCE_READY`

Date: 2026-06-03

## Purpose

DR-D5 tests the next OCR boundary after DR-D4. Instead of running OCR globally on
the full image, this prototype creates graph-owned crop zones from graphPanel,
plotArea, and axis label-band geometry:

- X tick-label band;
- Y tick-label band;
- graph header/context band;
- right legend/context band.

This is a PC-side research and benchmark slice only. It uses DR-C3 annotation
geometry as prototype crop ownership and DR-C4 text-role labels as truth. It
does not change Android runtime, production graph detection, calibration, trace
extraction, peak integration, `CalculationEngine`, chromatographic math,
validators, model routing, or report rendering.

## Command

```powershell
python tools/benchmark/run_drd5_axis_owned_ocr_crop_planner.py
```

Output:

`benchmark/reports/drd5_axis_owned_ocr_crop_planner/`

Generated files:

- `summary.json`
- `summary.md`
- `overlays/*_axis_owned_crop_plan_overlay.png`

## Verdict

`AXIS_OWNED_OCR_CROP_PLANNER_IMPROVES_SCOPE_NOT_ACCEPTANCE_READY`

## Results

Fixtures: `4`

Crop plans: `34`

Owned truth labels: `210`

| Method | Owned recall | Tick recall | Safe role accuracy | Safe false tick labels | Rejected unmatched numeric | Mean crop time |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `rapidocr_axis_owned_crops_x2_v1` | 0.7048 | 0.7049 | 0.8581 | 0 | 12 | 1.5400s |
| `easyocr_en_axis_owned_crops_x2_v1` | 0.7190 | 0.7104 | 0.6424 | 0 | 20 | 0.7091s |

Per-fixture result:

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

## Visual Evidence

Crop-plan overlays:

- `benchmark/reports/drd5_axis_owned_ocr_crop_planner/overlays/bench_01_mz71_screenshot_page_axis_owned_crop_plan_overlay.png`
- `benchmark/reports/drd5_axis_owned_ocr_crop_planner/overlays/bench_04_stacked_xic_resolution_axis_owned_crop_plan_overlay.png`
- `benchmark/reports/drd5_axis_owned_ocr_crop_planner/overlays/bench_05_tic_plus_ions_axis_owned_crop_plan_overlay.png`
- `benchmark/reports/drd5_axis_owned_ocr_crop_planner/overlays/bench_06_photo_two_graphs_page_axis_owned_crop_plan_overlay.png`

## What Improved

- OCR scope is now graph-owned instead of global.
- Numeric OCR detections without matched geometry-owned truth labels are
  rejected before calibration.
- Safe false tick-label count is `0` for both RapidOCR and EasyOCR.
- RapidOCR remains the stronger current candidate for role quality.

## What Still Fails

- Tick recall is only about `0.70`, below an acceptance threshold for automatic
  calibration.
- `bench_05_tic_plus_ions` remains weak because stacked TIC/ion panels and small
  dense labels need better crop planning and OCR preprocessing.
- EasyOCR improves some box recall, but its role accuracy is too low for
  production integration.
- This prototype still depends on DR-C3 annotation geometry; runtime must
  generate equivalent crop ownership automatically.

## Product Rule

Axis-owned OCR is safer than global OCR, but OCR still cannot create
calibration anchors by itself. Numeric OCR may enter calibration only after
deterministic geometry owns the graph, axis, label band, and pixel position.

## Next Slice

Recommended next slice:

`DR-D6: Axis-Owned OCR Crop Preprocessing Grid`

Goal:

- compare scale/padding/contrast/threshold variants inside axis-owned crops;
- improve tick recall on `bench_05_tic_plus_ions`;
- keep zero false tick labels;
- prepare the OCR output needed by DR-E axis scale and calibration benchmarks.

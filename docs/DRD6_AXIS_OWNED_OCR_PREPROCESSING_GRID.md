# DR-D6 Axis-Owned OCR Crop Preprocessing Grid

Status: `DR_D6_COMPLETE_AXIS_OWNED_OCR_PREPROCESSING_IMPROVES_RECALL_NOT_ACCEPTANCE_READY`

Date: 2026-06-03

## Purpose

DR-D6 tests whether OCR quality improves when preprocessing is applied inside
axis-owned crop zones from DR-D5. This is still a PC-side research and benchmark
slice only. It does not change Android runtime, production graph detection,
calibration, trace extraction, peak integration, `CalculationEngine`,
chromatographic math, validators, model routing, or report rendering.

The benchmark compares:

- scale factors;
- small crop padding;
- RGB crops;
- autocontrast crops;
- sharpened autocontrast crops;
- percentile binary threshold crops.

## Command

```powershell
python tools/benchmark/run_drd6_axis_owned_ocr_preprocessing_grid.py
```

Output:

`benchmark/reports/drd6_axis_owned_ocr_preprocessing_grid/`

Generated files:

- `summary.json`
- `summary.md`

## Verdict

`AXIS_OWNED_OCR_PREPROCESSING_IMPROVES_RECALL_NOT_ACCEPTANCE_READY`

## Results

Fixtures: `4`

Crop plans: `34`

DR-D5 best tick recall: `0.7104`

DR-D6 best tick recall: `0.7322`

Best variant: `rapidocr_rgb_x3_p2_v1`

| Variant | Engine | Mode | Tick recall | Safe role accuracy | False tick labels | Rejected unmatched numeric | Mean crop time |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: |
| `rapidocr_rgb_x2_p0_v1` | `rapidocr` | `rgb` | 0.7049 | 0.8581 | 0 | 12 | 1.3149s |
| `rapidocr_rgb_x3_p2_v1` | `rapidocr` | `rgb` | 0.7322 | 0.8397 | 0 | 21 | 1.3044s |
| `rapidocr_autocontrast_x3_p4_v1` | `rapidocr` | `autocontrast` | 0.7213 | 0.8289 | 0 | 18 | 1.3526s |
| `rapidocr_sharp_autocontrast_x3_p4_v1` | `rapidocr` | `sharp_autocontrast` | 0.7104 | 0.8355 | 0 | 15 | 1.1601s |
| `rapidocr_binary65_x3_p4_v1` | `rapidocr` | `binary65` | 0.3989 | 0.6667 | 0 | 22 | 1.0589s |
| `rapidocr_binary75_x4_p6_v1` | `rapidocr` | `binary75` | 0.4153 | 0.6702 | 0 | 10 | 1.3113s |
| `easyocr_rgb_x2_p0_v1` | `easyocr` | `rgb` | 0.7104 | 0.6424 | 0 | 20 | 0.9507s |
| `easyocr_autocontrast_x3_p4_v1` | `easyocr` | `autocontrast` | 0.7213 | 0.5823 | 0 | 22 | 1.4107s |

Best variant by fixture:

| Fixture | Tick recall | Safe role accuracy | False tick labels |
| --- | ---: | ---: | ---: |
| `bench_01_mz71_screenshot_page` | 0.9000 | 0.9677 | 0 |
| `bench_04_stacked_xic_resolution` | 0.8393 | 0.9038 | 0 |
| `bench_05_tic_plus_ions` | 0.4889 | 0.5312 | 0 |
| `bench_06_photo_two_graphs_page` | 0.7308 | 0.9024 | 0 |

## What Improved

- `rapidocr_rgb_x3_p2_v1` improves tick recall from `0.7104` to `0.7322`.
- The best variant keeps safe false tick-label count at `0`.
- RGB upscale with small padding beats aggressive thresholding.
- Binary threshold variants are not suitable for these current label crops.

## What Still Fails

- Tick recall is still below a production calibration threshold.
- `bench_05_tic_plus_ions` remains the hard blocker because small stacked-panel
  labels are incomplete even with preprocessing.
- EasyOCR remains useful as a secondary benchmark, but its role accuracy is too
  low for the current pipeline.

## Product Rule

The safe OCR output can support calibration only as owned label evidence.
Missing labels remain missing. OCR must not fabricate tick labels, pixel
geometry, calibration coefficients, or chromatographic metrics.

## Next Slice

Next slice completed:

`DR-E1: Axis Scale Candidate Builder From Safe Owned OCR`

Result:

- documented in `docs/DRE1_AXIS_SCALE_CANDIDATE_BUILDER_FROM_SAFE_OCR.md`;
- safe owned OCR labels were converted into X/Y scale candidates;
- results are partial: 13 valid axes, 2 review axes, 9 invalid axes;
- failure reasons are explicit: insufficient anchors, high residuals, and
  non-monotonic label sequences.

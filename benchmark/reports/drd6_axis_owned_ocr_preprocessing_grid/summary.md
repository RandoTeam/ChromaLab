# DR-D6 Axis-Owned OCR Crop Preprocessing Grid

Verdict: `AXIS_OWNED_OCR_PREPROCESSING_IMPROVES_RECALL_NOT_ACCEPTANCE_READY`
Fixtures: `4`
Crop plans: `34`
Best variant: `rapidocr_rgb_x3_p2_v1`

## Variant Summary

| Variant | Engine | Mode | Tick recall | Safe role accuracy | False ticks | Rejected unmatched numeric | Mean crop time |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: |
| `rapidocr_rgb_x2_p0_v1` | `rapidocr` | `rgb` | 0.7049 | 0.8581 | 0 | 12 | 1.3149s |
| `rapidocr_rgb_x3_p2_v1` | `rapidocr` | `rgb` | 0.7322 | 0.8397 | 0 | 21 | 1.3044s |
| `rapidocr_autocontrast_x3_p4_v1` | `rapidocr` | `autocontrast` | 0.7213 | 0.8289 | 0 | 18 | 1.3526s |
| `rapidocr_sharp_autocontrast_x3_p4_v1` | `rapidocr` | `sharp_autocontrast` | 0.7104 | 0.8355 | 0 | 15 | 1.1601s |
| `rapidocr_binary65_x3_p4_v1` | `rapidocr` | `binary65` | 0.3989 | 0.6667 | 0 | 22 | 1.0589s |
| `rapidocr_binary75_x4_p6_v1` | `rapidocr` | `binary75` | 0.4153 | 0.6702 | 0 | 10 | 1.3113s |
| `easyocr_rgb_x2_p0_v1` | `easyocr` | `rgb` | 0.7104 | 0.6424 | 0 | 20 | 0.9507s |
| `easyocr_autocontrast_x3_p4_v1` | `easyocr` | `autocontrast` | 0.7213 | 0.5823 | 0 | 22 | 1.4107s |

## Best Fixture Results

| Fixture | Variant | Tick recall | Safe role accuracy | False ticks |
| --- | --- | ---: | ---: | ---: |
| `bench_01_mz71_screenshot_page` | `rapidocr_rgb_x3_p2_v1` | 0.9000 | 0.9677 | 0 |
| `bench_04_stacked_xic_resolution` | `rapidocr_rgb_x3_p2_v1` | 0.8393 | 0.9038 | 0 |
| `bench_05_tic_plus_ions` | `rapidocr_rgb_x3_p2_v1` | 0.4889 | 0.5312 | 0 |
| `bench_06_photo_two_graphs_page` | `rapidocr_rgb_x3_p2_v1` | 0.7308 | 0.9024 | 0 |

## Interpretation

- Preprocessing can improve individual crop behavior, but the current grid is not acceptance-ready.
- Zero false tick labels remains mandatory; variants with safety failures are rejected.
- `bench_05_tic_plus_ions` remains the hard OCR case because small stacked-panel labels are incomplete.
- The next work should move into DR-E axis-scale/calibration using only safe owned label evidence and explicit missing-label reasons.

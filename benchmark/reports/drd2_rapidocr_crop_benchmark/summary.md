# DR-D2 RapidOCR Crop OCR Benchmark

Verdict: `RAPIDOCR_INSTALLED_CROP_OCR_WORKS_BUT_NOT_ACCEPTANCE_READY`
Text labels: `230`

## Installed OCR Stack

| Package | Version |
| --- | --- |
| `rapidocr` | `3.8.1` |
| `onnxruntime` | `1.26.0` |
| `opencv-python` | `4.13.0.92` |
| `numpy` | `2.4.6` |
| `Pillow` | `12.2.0` |

## Method Summary

| Method | Non-empty OCR | Exact text | Mean similarity | Role accuracy | False tick labels | Forbidden numeric rejected | Mean time/label |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `rapidocr_rec_padded_x3_v1` | 205/230 | 68/230 | 0.6178 | 120/230 | 4 | 12/16 | 0.0524s |
| `rapidocr_rec_binary_x4_v1` | 187/230 | 52/230 | 0.4869 | 96/230 | 4 | 15/16 | 0.0439s |

## Fixture Feature Scores

| Method | Fixture | TIC title | Ion/mz metadata | Legend | Forbidden numeric safety |
| --- | --- | --- | --- | --- | --- |
| `rapidocr_rec_padded_x3_v1` | `bench_01_mz71_screenshot_page` | PASS | PASS | PASS | PASS |
| `rapidocr_rec_padded_x3_v1` | `bench_04_stacked_xic_resolution` | PASS | PASS | FAIL | FAIL |
| `rapidocr_rec_padded_x3_v1` | `bench_05_tic_plus_ions` | PASS | FAIL | PASS | PASS |
| `rapidocr_rec_padded_x3_v1` | `bench_06_photo_two_graphs_page` | PASS | FAIL | PASS | FAIL |
| `rapidocr_rec_binary_x4_v1` | `bench_01_mz71_screenshot_page` | PASS | FAIL | PASS | FAIL |
| `rapidocr_rec_binary_x4_v1` | `bench_04_stacked_xic_resolution` | PASS | PASS | FAIL | FAIL |
| `rapidocr_rec_binary_x4_v1` | `bench_05_tic_plus_ions` | PASS | FAIL | PASS | PASS |
| `rapidocr_rec_binary_x4_v1` | `bench_06_photo_two_graphs_page` | PASS | FAIL | PASS | FAIL |

## Interpretation

- RapidOCR is now installed and model loading/download has been verified.
- Recognition-only crop OCR is fast enough for PC benchmarking, but text accuracy is not yet sufficient for runtime acceptance.
- The benchmark exposes exact text, role, and forbidden-numeric safety scores without changing production code.
- Next work should improve crop generation/preprocessing and compare another OCR engine or OCR variant before Android parity.

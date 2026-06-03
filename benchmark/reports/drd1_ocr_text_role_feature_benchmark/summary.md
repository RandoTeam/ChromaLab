# DR-D1 OCR Text-Role Feature Benchmark

Verdict: `TEXT_ROLE_RULES_WORK_WITH_PERFECT_TEXT_REAL_OCR_NOT_AVAILABLE`
Annotated text labels: `230`

## Local OCR Availability

| Engine | Available |
| --- | --- |
| Python module `pytesseract` | `False` |
| Python module `easyocr` | `False` |
| Python module `paddleocr` | `False` |
| Python module `rapidocr_onnxruntime` | `False` |
| Python module `onnxruntime` | `False` |
| Python module `cv2` | `False` |
| Executable `tesseract` | `False` |
| Executable `magick` | `False` |

## Method Summary

| Method | Readiness | Role accuracy | Correct labels | False tick labels | Forbidden numeric rejected | Semantic feature pass |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| `no_ocr_available_v1` | `CURRENT_LOCAL_ENGINE_BASELINE` | 0.0000 | 0/230 | 0 | 1.0000 | 11/16 |
| `regex_on_perfect_text_v1` | `UPPER_BOUND_REQUIRES_AUTOMATIC_OCR_TEXT` | 0.9435 | 217/230 | 0 | 1.0000 | 16/16 |
| `annotation_role_oracle_v1` | `ORACLE_NOT_RUNTIME` | 1.0000 | 230/230 | 0 | 1.0000 | 16/16 |

## Fixture Feature Scores

| Method | Fixture | TIC title | Ion/mz metadata | Legend | Forbidden numeric safety |
| --- | --- | --- | --- | --- | --- |
| `no_ocr_available_v1` | `bench_01_mz71_screenshot_page` | PASS | FAIL | PASS | PASS |
| `no_ocr_available_v1` | `bench_04_stacked_xic_resolution` | PASS | PASS | FAIL | PASS |
| `no_ocr_available_v1` | `bench_05_tic_plus_ions` | FAIL | FAIL | PASS | PASS |
| `no_ocr_available_v1` | `bench_06_photo_two_graphs_page` | PASS | FAIL | PASS | PASS |
| `regex_on_perfect_text_v1` | `bench_01_mz71_screenshot_page` | PASS | PASS | PASS | PASS |
| `regex_on_perfect_text_v1` | `bench_04_stacked_xic_resolution` | PASS | PASS | PASS | PASS |
| `regex_on_perfect_text_v1` | `bench_05_tic_plus_ions` | PASS | PASS | PASS | PASS |
| `regex_on_perfect_text_v1` | `bench_06_photo_two_graphs_page` | PASS | PASS | PASS | PASS |
| `annotation_role_oracle_v1` | `bench_01_mz71_screenshot_page` | PASS | PASS | PASS | PASS |
| `annotation_role_oracle_v1` | `bench_04_stacked_xic_resolution` | PASS | PASS | PASS | PASS |
| `annotation_role_oracle_v1` | `bench_05_tic_plus_ions` | PASS | PASS | PASS | PASS |
| `annotation_role_oracle_v1` | `bench_06_photo_two_graphs_page` | PASS | PASS | PASS | PASS |

## Interpretation

- No local OCR engine is currently available in this Python environment, so the real OCR baseline is `missing_text`.
- Regex classification on perfect text reaches the text-role feature target without turning Ion/TIC/legend numbers into tick labels.
- This does not prove OCR works; it proves the next implementation must first produce reliable text strings and boxes.
- The next slice should install or integrate an OCR engine and score real crop OCR against these same DR-D1 targets.

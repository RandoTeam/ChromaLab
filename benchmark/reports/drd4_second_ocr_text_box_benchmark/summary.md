# DR-D4 Second OCR Engine And Text-Box Detection Benchmark

Verdict: `SECOND_OCR_INSTALLED_TEXT_BOX_DETECTION_NOT_ACCEPTANCE_READY`
Fixtures: `4`
Truth labels: `230`

## Method Summary

| Method | Box recall | Safe role accuracy | Safe false tick labels | Unmatched numeric boxes | Mean time/fixture |
| --- | ---: | ---: | ---: | ---: | ---: |
| `rapidocr_full_detection_v1` | 0.7000 | 0.8696 | 0 | 21 | 3.4593s |
| `easyocr_en_full_detection_v1` | 0.7130 | 0.6463 | 0 | 24 | 6.5622s |

## Fixture Summary

| Fixture | Method | Detections | Matched truth | Box recall | Safe role accuracy | Safe false ticks | Time |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| `bench_01_mz71_screenshot_page` | `rapidocr_full_detection_v1` | 48 | 34/36 | 0.9444 | 0.9706 | 0 | 3.4555s |
| `bench_04_stacked_xic_resolution` | `rapidocr_full_detection_v1` | 65 | 61/68 | 0.8971 | 0.8852 | 0 | 3.3587s |
| `bench_05_tic_plus_ions` | `rapidocr_full_detection_v1` | 31 | 27/68 | 0.3971 | 0.6296 | 0 | 3.2848s |
| `bench_06_photo_two_graphs_page` | `rapidocr_full_detection_v1` | 69 | 39/58 | 0.6724 | 0.9231 | 0 | 3.7384s |
| `bench_01_mz71_screenshot_page` | `easyocr_en_full_detection_v1` | 71 | 32/36 | 0.8889 | 0.8125 | 0 | 7.4704s |
| `bench_04_stacked_xic_resolution` | `easyocr_en_full_detection_v1` | 66 | 59/68 | 0.8676 | 0.9322 | 0 | 4.4444s |
| `bench_05_tic_plus_ions` | `easyocr_en_full_detection_v1` | 35 | 31/68 | 0.4559 | 0.3548 | 0 | 4.4161s |
| `bench_06_photo_two_graphs_page` | `easyocr_en_full_detection_v1` | 91 | 42/58 | 0.7241 | 0.3333 | 0 | 9.9178s |

## Interpretation

- EasyOCR is installed and can run as a second PC OCR benchmark engine.
- Full-image text-box detection is still not acceptance-ready for Android integration.
- The context safety gate remains mandatory: pure numeric OCR outside geometry-owned tick context is rejected.
- Runtime integration should wait until text boxes are generated from deterministic graph/axis label bands, not global OCR alone.

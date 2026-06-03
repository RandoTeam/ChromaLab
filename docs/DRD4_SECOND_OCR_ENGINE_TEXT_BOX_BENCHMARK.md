# DR-D4 Second OCR Engine And Text-Box Detection Benchmark

Status: `DR_D4_COMPLETE_SECOND_OCR_INSTALLED_TEXT_BOX_DETECTION_NOT_ACCEPTANCE_READY`

Date: 2026-06-03

## Purpose

DR-D4 installs and benchmarks a second OCR candidate, EasyOCR, against RapidOCR
on full-image text-box detection. DR-D2 and DR-D3 used truth-box crops. This
slice asks a harder runtime question:

Can OCR find text boxes on the whole chromatogram image, classify their roles,
and avoid unsafe numeric tick-label mistakes?

This is a research and benchmark slice only. It does not change Android runtime,
production graph detection, calibration, trace extraction, peak integration,
`CalculationEngine`, chromatographic math, validators, model routing, or report
rendering.

## Command

Install PC OCR benchmark dependencies:

```powershell
python -m pip install -r tools/benchmark/ocr-requirements.txt
```

Run the benchmark:

```powershell
python tools/benchmark/run_drd4_second_ocr_text_box_benchmark.py
```

Output:

`benchmark/reports/drd4_second_ocr_text_box_benchmark/`

Generated files:

- `summary.json`
- `summary.md`

## Environment

Installed benchmark OCR stack:

| Package | Version |
| --- | --- |
| `rapidocr` | `3.8.1` |
| `onnxruntime` | `1.26.0` |
| `easyocr` | `1.7.2` |
| `torch` | `2.12.0` |
| `torchvision` | `0.27.0` |
| `opencv-python-headless` | `4.13.0.92` |

EasyOCR model loading was verified locally. The first run downloaded EasyOCR
detector/recognizer files under the user model cache. Subsequent runs completed
without model setup errors.

## Verdict

`SECOND_OCR_INSTALLED_TEXT_BOX_DETECTION_NOT_ACCEPTANCE_READY`

## Results

The benchmark used the four P0 DR-C4 text-role annotated fixtures:

- `bench_01_mz71_screenshot_page`
- `bench_04_stacked_xic_resolution`
- `bench_05_tic_plus_ions`
- `bench_06_photo_two_graphs_page`

Total truth labels: `230`.

| Method | Box recall | Safe role accuracy | Safe false tick labels | Unmatched numeric boxes | Mean time/fixture |
| --- | ---: | ---: | ---: | ---: | ---: |
| `rapidocr_full_detection_v1` | 0.7000 | 0.8696 | 0 | 21 | 3.4593s |
| `easyocr_en_full_detection_v1` | 0.7130 | 0.6463 | 0 | 24 | 6.5622s |

Per-fixture result:

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

- EasyOCR is usable as a second PC benchmark engine, but not yet a production
  Android integration candidate.
- RapidOCR remains faster and has higher matched safe role accuracy.
- EasyOCR slightly improves full-image box recall, but role accuracy falls
  sharply on `bench_05_tic_plus_ions` and `bench_06_photo_two_graphs_page`.
- Both engines require the context safety gate: pure numeric OCR must be
  rejected outside deterministic tick-label candidate context.
- Full-image OCR alone is not enough. The next OCR work should generate
  geometry-owned graph/axis/label-band crops first, then run OCR inside those
  constrained regions.

## Product Rule

OCR can read text. OCR cannot own calibration geometry.

Numeric OCR may enter calibration only after deterministic graph/axis/tick or
label-band geometry proves axis ownership. Global OCR boxes must not become tick
labels or anchors by themselves.

## Next Slice

Recommended next slice:

`DR-D5: Axis-Owned OCR Crop Planner Prototype`

Goal:

- generate OCR crops from detected graph panels, plot areas, and axis label
  bands;
- compare RapidOCR and EasyOCR inside those constrained crops;
- preserve the context safety gate;
- reject unmatched numeric boxes before calibration.

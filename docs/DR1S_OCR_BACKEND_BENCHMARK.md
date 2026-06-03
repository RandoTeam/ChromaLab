# DR-1S OCR Backend Benchmark

Status: `DR1S_EVIDENCE_COMPLETE`

Scope: PC-only OCR backend benchmark over DR-1R axis-label crop sweep artifacts. This phase does not change `CalculationEngine`, chromatographic math, validators, report gates, Android runtime behavior, or production calibration selection.

## Task Classification

- OCR backend benchmark
- Axis label crop evaluation
- Parallel PC validation
- Research-backed runtime selection
- QA / regression evidence

## Agents Activated

- Orchestrator: keeps DR-1S limited to OCR benchmarking and documentation.
- Research Intelligence: checks current OCR backend docs and known runtime constraints.
- Geometry / Calibration Core: owns axis label crop geometry and calibration evidence boundaries.
- OCR / VLM Text Semantics: owns OCR text extraction and title/ion/m/z rejection policy.
- QA / Regression: owns repeatable runner, result summaries, and validation commands.
- Scientific Reporting / Validation: verifies OCR text is not promoted into chromatographic metrics.
- Product Acceptance: decides whether a backend is useful enough for the next integration phase.
- Security / Privacy: keeps generated artifacts under ignored build/artifact roots and avoids unlicensed external images.

## Skills Used

- `current-web-research-deep`
- `source-quality-triage`
- `research-synthesis`
- `SKILL_16_DEEP_RESEARCH_METHOD_DISCOVERY`
- `ocr-local-crops`
- `ocr-crop-benchmark`
- `geometry-calibration-robust-fit`
- `evidence-package-validator`
- `regression-benchmark-golden`
- `test-plan-authoring`
- `definition-of-done`

## Current Method Sources

- PaddleOCR PP-OCRv5 documentation: https://www.paddleocr.ai/main/en/version3.x/pipeline_usage/OCR.html
- PaddleOCR on-device deployment: https://www.paddleocr.ai/main/en/version3.x/inference_deployment/cross_platform/on_device_deployment.html
- RapidOCR PyPI package: https://pypi.org/project/rapidocr/
- RapidOCR ONNXRuntime package: https://pypi.org/project/rapidocr-onnxruntime/
- Tesseract image quality guide: https://tesseract-ocr.github.io/tessdoc/ImproveQuality.html
- PP-OCRv5 paper: https://arxiv.org/abs/2603.24373

## Runner

Script:

`tools/chromatogram-bench/run_axis_label_ocr_benchmark.py`

Default command:

```powershell
python tools/chromatogram-bench/run_axis_label_ocr_benchmark.py `
  --input-root build/dr1r-axis-label-crop-sweep `
  --output-root build/dr1s-ocr-backend-benchmark `
  --workers 10
```

Supported backends:

- `rapidocr_python`
- `tesseract_cli_psm7_numeric`
- `tesseract_cli_psm6_text`
- `paddleocr_cli`

The runner:

- scans `axis_label_crop_sweep_graph_*.json`;
- runs available OCR engines over crop variants;
- uses a thread pool for parallel subprocess/Python engine execution;
- records backend availability, timeout, text, numeric tokens, crop hash, warnings, and duration;
- writes CSV, JSON, and Markdown summaries.

## Boundary Rules

- OCR output is text evidence only.
- OCR cannot create pixel coordinates.
- OCR cannot create calibration coefficients.
- OCR cannot create RT, height, area, FWHM, S/N, baseline, Kovats, or compound identity.
- Numeric text remains unusable for calibration unless paired to deterministic geometry in a later phase.

## Initial Environment Finding

The repository PC currently has Python and PIL, but no usable OCR backend in the default environment:

- Tesseract CLI is not on `PATH`.
- `pytesseract`, `paddleocr`, `easyocr`, `cv2`, and `torch` are not importable in default Python.
- Python 3.11 is available through `uv`, which is the right target for optional OCR package experiments because several OCR wheels do not support Python 3.13/3.14 yet.

## Environment Setup Used For Real OCR

System Python 3.13/3.14 did not have OCR backends. For DR-1S, a temporary ignored venv was created under:

`artifacts/dr1s-ocr-venv`

Commands used:

```powershell
uv venv artifacts\dr1s-ocr-venv --python C:\Users\Ilia\AppData\Roaming\uv\python\cpython-3.11.15-windows-x86_64-none\python.exe
uv pip install --python artifacts\dr1s-ocr-venv\Scripts\python.exe rapidocr onnxruntime
```

Installed OCR/runtime packages:

| Package | Version |
| --- | --- |
| rapidocr | 3.8.1 |
| onnxruntime | 1.26.0 |
| opencv-python | 4.13.0.92 |
| numpy | 2.4.6 |
| pillow | 12.2.0 |

RapidOCR downloaded these ignored local model files inside the venv:

| Model file | Size |
| --- | ---: |
| `ch_PP-OCRv4_det_mobile.onnx` | 4,745,517 |
| `ch_PP-OCRv4_rec_mobile.onnx` | 10,857,958 |
| `ch_ppocr_mobile_v2.0_cls_mobile.onnx` | 585,532 |

Important: this is a real OCR benchmark, but RapidOCR's default downloaded models in this run are PP-OCRv4 mobile ONNX models, not PP-OCRv5. PP-OCRv5/PaddleOCR remains a separate benchmark target.

## Baseline Backend Discovery

Default PC environment command:

```powershell
python tools/chromatogram-bench/run_axis_label_ocr_benchmark.py `
  --input-root build/dr1r-axis-label-crop-sweep `
  --output-root build/dr1s-ocr-backend-benchmark `
  --workers 10
```

Result:

- 378 crop variants found.
- 0 available OCR engines in default Python/system PATH.
- Tesseract CLI was not installed.
- PaddleOCR CLI was not installed.
- RapidOCR was not installed before the temporary venv setup.

## RapidOCR Full Benchmark

Command:

```powershell
artifacts\dr1s-ocr-venv\Scripts\python.exe tools\chromatogram-bench\run_axis_label_ocr_benchmark.py `
  --input-root build\dr1r-axis-label-crop-sweep `
  --output-root build\dr1s-ocr-backend-benchmark-rapidocr-final `
  --engines rapidocr_python `
  --workers 10 `
  --timeout-seconds 60
```

Output root:

`C:\VietnAi\Hromotograth\build\dr1s-ocr-backend-benchmark-rapidocr-final`

Outputs:

- `dr1s_ocr_backend_results.csv`
- `dr1s_ocr_backend_summary.json`
- `dr1s_ocr_backend_summary.md`

## Result Summary

| Metric | Value |
| --- | ---: |
| CPU logical processors | 12 |
| Workers requested | 10 |
| Crop variants | 378 |
| OCR results | 378 |
| Text hits | 284 |
| Numeric hits | 242 |
| Elapsed | 556,590 ms |

The full all-variant CPU/ONNX run is useful but not fast enough for routine inner-loop use. A future quick benchmark should restrict variants to the strongest candidates, such as `scale4_contrast`, `scale4_grayscale`, and `scale4_otsu_threshold`.

## Fixture Summary

| Fixture | Graphs | Variants | Text hits | Numeric hits | Main reading |
| --- | ---: | ---: | ---: | ---: | --- |
| belyi_tigr_ion92_extra | 1 | 21 | 18 | 15 | Y-axis labels readable; X label weak |
| bench_01_mz71_screenshot_page | 2 | 42 | 42 | 42 | X/Y labels readable, but title/ion text also produces forbidden numbers |
| bench_02_mz92_belyi_tigr | 1 | 21 | 18 | 15 | Y-axis labels readable; X label weak |
| bench_03_small_tic_export | 1 | 21 | 21 | 21 | Text/numeric OCR is available but noisy |
| bench_04_stacked_xic_resolution | 4 | 84 | 30 | 15 | Weakest OCR fixture; many panels produce no reliable X labels |
| bench_05_tic_plus_ions | 4 | 84 | 50 | 45 | Y labels/title text readable; X label band produced no hits |
| bench_06_photo_two_graphs_page | 2 | 42 | 42 | 40 | Strong OCR signal; candidate for geometry/axis pairing |
| bench_07_rotated_page_photo | 1 | 21 | 21 | 21 | Strong OCR signal consistent with prior replay success |
| bench_08_mz71_duplicate_candidate | 1 | 21 | 21 | 14 | X/Y labels readable; title band has no numeric hit |
| white_tiger_ion71_fixture | 1 | 21 | 21 | 14 | X/Y labels readable; title band has no numeric hit |

## Band-Level Findings

- `bench_01` proves the crop sweep can expose axis labels, but it also reads `Ion 217.00`, file ids, and title numbers. These must stay rejected as scale labels.
- `bench_05` has zero X label-band hits despite Y/title hits, so its current blocker is likely label-band geometry/crop placement for X, not merely absence of OCR.
- `bench_04` remains a hard layout/OCR stress case. Several stacked XIC panels have low text hits and need expanded crop bands or a layout-aware panel crop strategy.
- White Tiger and `bench_07` both have readable X/Y OCR under RapidOCR, so the next repair path should pair OCR boxes/text to deterministic geometry rather than add more tick-only logic.

## Variant-Level Findings

| Variant | Total | Text hits | Numeric hits |
| --- | ---: | ---: | ---: |
| `scale4_otsu_threshold` | 54 | 42 | 37 |
| `scale4_grayscale` | 54 | 38 | 35 |
| `scale2_grayscale` | 54 | 40 | 35 |
| `scale4_contrast` | 54 | 40 | 35 |
| `grayscale` | 54 | 40 | 34 |
| `scale4_otsu_threshold_inverted` | 54 | 43 | 33 |
| `original` | 54 | 41 | 33 |

There is no single universally best preprocessing variant. The runner should keep multiple variants and let later label-pairing logic score consistency.

## Product/Scientific Boundary

DR-1S confirms real OCR can recover many numeric strings from the generated axis label crops. It does not prove calibration correctness by itself.

Required next gate:

- OCR numeric text must be classified by band.
- Title/ion/m/z/file/sample numbers must be rejected.
- Numeric axis labels must be paired to deterministic pixel geometry.
- Calibration can only consume accepted label-to-geometry evidence.

## Next Step

Recommended next wave: DR-1T, OCR text classification and numeric label filtering.

Focus:

- separate X tick labels, Y tick labels, title, ion, file id, and metadata text;
- reject title/ion/m/z/file ids before any scale resolver sees them;
- build a per-band OCR consensus table across crop variants;
- choose a small fast variant subset for routine regression runs;
- keep RapidOCR as a PC benchmark backend while separately testing PaddleOCR PP-OCRv5.

# 2026-06-03 DR-1S OCR Backend Benchmark Research

Scope: current OCR backend research for PC crop benchmark execution. This research informed only the DR-1S benchmark runner and temporary OCR environment.

## Sources Checked

- PaddleOCR OCR pipeline:
  https://www.paddleocr.ai/main/en/version3.x/pipeline_usage/OCR.html
  - Relevant because current PaddleOCR defaults to PP-OCRv5 and exposes detector/recognizer model controls.
  - Adopted now: runner supports `paddleocr_cli` as an optional backend.
  - Not adopted now: installing PaddleOCR into the project runtime.

- PaddleOCR on-device deployment:
  https://www.paddleocr.ai/main/en/version3.x/inference_deployment/cross_platform/on_device_deployment.html
  - Relevant because Android/on-device OCR remains the product target.
  - Adopted now: keep mobile OCR as a benchmark candidate.
  - Not adopted now: Android runtime integration.

- RapidOCR PyPI:
  https://pypi.org/project/rapidocr/
  - Relevant because it is a current CPU/ONNX OCR package and installed quickly in a temporary Python 3.11 venv.
  - Adopted now: real PC OCR benchmark backend.
  - Caveat: default downloaded models in this run were PP-OCRv4 mobile ONNX models.

- RapidOCR ONNXRuntime package:
  https://pypi.org/project/rapidocr-onnxruntime/
  - Relevant because RapidOCR requires ONNXRuntime; import-only discovery is not sufficient.
  - Adopted now: backend discovery now requires both `rapidocr` and `onnxruntime`.

- Tesseract image quality guide:
  https://tesseract-ocr.github.io/tessdoc/ImproveQuality.html
  - Relevant because DR-1R crop variants follow rescale/threshold/contrast guidance.
  - Not adopted now: Tesseract CLI is not installed on this PC.

- PP-OCRv5 paper:
  https://arxiv.org/abs/2603.24373
  - Relevant because PP-OCRv5 is the newer lightweight OCR target.
  - Not adopted now: this phase benchmarked RapidOCR default ONNX models; PP-OCRv5 remains a separate benchmark.

## Decision

Use RapidOCR as the first real PC OCR backend because it installs and runs on the current machine. Do not treat its OCR strings as calibration authority. The next implementation should classify and filter OCR text before any label-to-axis pairing or scale resolver work.

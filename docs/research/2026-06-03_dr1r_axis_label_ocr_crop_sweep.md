# 2026-06-03 DR-1R Axis-Label OCR Crop Sweep Research

Scope: current-method research for axis-label crop preprocessing and external graph fixture sources. This research informed the DR-1R diagnostic crop sweep only; it did not change production calibration behavior.

## Sources Checked

- Plot2Spectra paper:
  https://pubs.rsc.org/en/content/articlehtml/2022/dd/d1dd00036e
  - Relevant because it describes automatic plot region detection, edge-constrained axis alignment, scene text detection/recognition for tick labels, and plot line extraction.
  - Adopted now: axis-label crops must carry geometry provenance and preprocessing variants.
  - Not adopted now: model training, semantic segmentation, optical-flow extraction.

- Plot2Spec repository:
  https://github.com/MaterialEyes/Plot2Spec
  - Relevant because it includes example spectroscopy plot images and implementation artifacts under GPL-3.0.
  - Adopted now: external fixture candidate catalog entry.
  - Not adopted now: committing third-party images without attribution and license review.

- PaddleOCR OCR pipeline docs:
  https://www.paddleocr.ai/main/en/version3.x/pipeline_usage/OCR.html
  - Relevant because PP-OCRv5 mobile detector/recognizer is a current OCR candidate and supports explicit model selection.
  - Adopted now: record PaddleOCR as a DR-1S OCR benchmark candidate.
  - Not adopted now: adding a new dependency/runtime in DR-1R.

- PaddleOCR on-device deployment docs:
  https://www.paddleocr.ai/main/en/version3.x/inference_deployment/cross_platform/on_device_deployment.html
  - Relevant because Android/offline deployment is the product target.
  - Adopted now: record mobile/on-device OCR as a candidate path.
  - Not adopted now: Android Paddle Lite integration.

- Tesseract image quality guidance:
  https://tesseract-ocr.github.io/tessdoc/ImproveQuality.html
  - Relevant because it recommends rescaling, binarisation, deskewing, border handling, and page segmentation mode selection.
  - Adopted now: DR-1R crop variants include scaling, grayscale, contrast, and Otsu thresholding.
  - Not adopted now: invoking Tesseract, because no local CLI/backend is available.

- PlotDigitizer docs:
  https://plotdigitizer.com/docs
  - Relevant as a calibration quality reference.
  - Adopted now: keep calibration point spacing and known-value evidence as quality criteria.
  - Not adopted now: manual marker workflow as the product path.

- Scatteract paper:
  https://arxiv.org/abs/1704.06687
  - Relevant because it combines OCR and robust regression to map pixels into chart coordinates.
  - Adopted now: reinforce that OCR text must be paired with pixel geometry.
  - Not adopted now: scatter-specific model architecture.

## Decision

DR-1R should remain an OCR crop evidence phase. The next useful implementation step is a real OCR backend benchmark over the generated crop corpus, not another calibration/tick heuristic.

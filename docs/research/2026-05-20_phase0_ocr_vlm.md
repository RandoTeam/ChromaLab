# Phase 0 Research - OCR / VLM / Text Semantics

Scope: safe OCR/VLM boundaries, local crop provenance, and title/ion text restrictions.

## Sources Checked

- Google ML Kit Text Recognition v2 for Android:
  https://developers.google.com/ml-kit/vision/text-recognition/v2/android
  - Relevant because Android OCR should provide text blocks/lines/elements from images.
  - Decision affected: OCR evidence requires crop path, raw text, parsed text, confidence, and classification.
  - Do not adopt: full-image OCR as sufficient peak-label evidence by itself.

- CameraX MLKit Analyzer:
  https://developer.android.com/media/camera/camerax/mlkitanalyzer
  - Relevant to future camera-integrated OCR/analysis flow.
  - Decision affected: image acquisition must preserve provenance before OCR.
  - Do not adopt: camera pipeline changes in Phase 0.

- OpenAI Structured Outputs guide:
  https://platform.openai.com/docs/guides/structured-outputs
  - Relevant because VLM/OCR crop fallback should be typed and schema-bound where supported.
  - Decision affected: VLM output is task-typed and never used for numeric chromatographic metrics.
  - Do not adopt: assuming every local/mobile model enforces JSON schema equally.

- Google AI Edge LiteRT GPU delegates:
  https://ai.google.dev/edge/litert/performance/gpu
  - Relevant because on-device VLM/OCR helper stages can use accelerators but still have operation limits.
  - Decision affected: VLM failure or timeout must not silently become numeric evidence.
  - Do not adopt: delegate availability as a scientific-quality guarantee.

## Phase 0 Decisions

- VLM task types are limited to `OCR_CROP`, `TEXT_CLASSIFICATION`, `OVERLAY_JUDGE`, `WARNING_SUMMARY`, and `GRAPH_HINT`.
- VLM cannot populate RT, height, area, FWHM, S/N, baseline, Kovats, or exact pixel geometry.
- Title/ion ranges such as `Ion 71.00 (70.70 to 71.70)` are not peak labels.

## Explicit Non-Adoptions

- No VLM peak calculation.
- No VLM pixel coordinates for calibration.
- No prompt-only trust path.

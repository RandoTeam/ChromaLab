# Phase 6 Research: VLM/OCR Benchmarking

Date: 2026-05-20

## Sources

- ML Kit Text Recognition v2 Android docs: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
  - Relevance: official local OCR API, bundled/unbundled behavior, image quality requirements.
  - Decision: benchmark rows must preserve crop path, OCR source, confidence, runtime, and failure reason.
  - Do not adopt: confidence-only acceptance without class and text comparison.
- OCR metric references using character error rate and word error rate:
  - IJDAR article: https://link.springer.com/article/10.1007/s10032-020-00359-9
  - MathWorks OCR metrics docs: https://www.mathworks.com/help/vision/ref/ocrmetrics.html
  - Relevance: CER/WER are standard OCR evaluation measures.
  - Decision: Phase 6 harness uses character error rate because chromatogram labels are short numeric strings.
  - Do not adopt: document-level OCR scores as evidence for small tick/peak-label crops.
- Qwen-VL paper: https://arxiv.org/abs/2308.12966
  - Relevance: VLMs can read text but are still generative and need validation.
  - Decision: VLM OCR fallback is review-grade semantic evidence until deterministic text/crop rules accept it.
  - Do not adopt: VLM localization or numeric coordinate output as calculation input.

## Phase 6 Benchmark Rules

1. Every benchmark case has expected text, expected class, crop kind, and crop path.
2. ML Kit and VLM observations are recorded separately.
3. Disagreement is REVIEW unless deterministic acceptance rules pass.
4. Final accepted OCR text is semantic evidence only; calibration and metrics still require deterministic pixel anchors.
5. Benchmark reports export JSON and Markdown for review.


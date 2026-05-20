# On-Device VLM/OCR Research - 2026-05-20

## Scope

This note supports the Phase 4 realignment while preserving strict VLM/OCR boundaries.

## Source Matrix

| Source | Quality | Relevance | Decision affected | Not adopted |
| --- | --- | --- | --- | --- |
| [ML Kit Text Recognition v2 for Android](https://developers.google.com/ml-kit/vision/text-recognition/v2/android) | Official Google docs | On-device OCR exposes text blocks, lines, elements, bounding boxes, and confidence-like structure. Image quality and text pixel size affect accuracy. | OCR evidence must use local crops with paths/bounds and confidence; full-image OCR alone is insufficient. | Do not parse OCR text as calibration without deterministic tick pixels. |
| [LiteRT for Android](https://ai.google.dev/edge/litert/android) | Official Google AI Edge docs | Current Android on-device inference supports CPU/GPU/NPU APIs but is hardware/runtime dependent. | VLM tasks must be bounded, cached, and evidence-backed; speed is not scientific proof. | Do not block deterministic pipeline on slow VLM. |
| [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) | Maintained reference app | Useful reference for on-device model UX/runtime experimentation. | Model state/UI and runtime evidence should be explicit. | Do not assume experimental gallery behavior is production validation. |
| [Gemma model card](https://ai.google.dev/gemma/docs/core/model_card) | Official model documentation | Model capabilities and limits should be read from model docs, not guessed. | VLM outputs need task-scoped contracts and warnings. | Do not use VLM for numeric chromatographic measurements. |

## Decisions

- VLM may read local text crops, classify text, judge overlays, and explain warnings.
- VLM must not provide final numeric geometry, RT, height, area, FWHM, S/N, baseline, Kovats, or final peak metrics.
- OCR/VLM evidence must include crop path, raw text, parsed text, task type, confidence, and rejection reason when invalid.
- If VLM disagrees with deterministic evidence, deterministic validation controls the gate and the report records a warning.

## Rejected Sources / Claims

- End-to-end chart extraction demos do not prove chromatogram-grade measurements.
- OCR text from title/ion ranges must not become peak label evidence.
- Weak benchmark or forum claims must not drive scientific release gates.

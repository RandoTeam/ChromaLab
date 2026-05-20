# Phase 6 Research: Autonomous Multimodal Intelligence

Date: 2026-05-20

## Sources

- Google ML Kit Text Recognition for Android: https://developers.google.com/ml-kit/vision/text-recognition/v2/android
  - Relevance: current official API and performance guidance for local OCR.
  - Decision: OCR evidence must be based on local crops with image quality and crop provenance recorded.
  - Do not adopt: full-image OCR as the only evidence source for peak labels or ticks.
- Google AI Edge LiteRT for Android: https://ai.google.dev/edge/litert/android
  - Relevance: current Android runtime API status and device deployment constraints.
  - Decision: model calls need runtime profile entries with backend, duration, timeout, and success/failure.
  - Do not adopt: unbounded model calls or mandatory VLM waits when deterministic evidence is strong.
- Google AI Edge Gallery: https://github.com/google-ai-edge/gallery
  - Relevance: maintained reference for offline on-device multimodal AI UX and model experimentation.
  - Decision: local model features should remain optional assistants with explicit status and diagnostics.
  - Do not adopt: demo-app confidence as scientific evidence.
- LiteRT-LM structured output guidance: https://developers.googleblog.com/blazing-fast-on-device-genai-with-litert-lm/
  - Relevance: current guidance that structured output and constrained decoding reduce parser failures.
  - Decision: ChromaLab VLM outputs need strict JSON task contracts and rejection of forbidden fields.
  - Do not adopt: model-generated numeric chromatographic metrics.
- W3C PROV namespace: https://www.w3.org/ns/prov
  - Relevance: authoritative provenance vocabulary for entities, activities, and agents.
  - Decision: runtime evidence should record model calls as activities and keep deterministic metrics traceable.
  - Do not adopt: claims of regulatory compliance without formal validation.

## Phase 6 Decisions

1. CV remains owner of pixel geometry, masks, trace extraction, and pixel evidence.
2. OCR reads local crops and stores crop provenance, raw text, normalized text, class, confidence, and duration.
3. VLM can only assist with local crop OCR fallback, semantic classification, overlay judging, retry recommendations, and warning summaries.
4. VLM JSON contracts must reject RT, height, area, FWHM, S/N, baseline, Kovats, exact geometry, and calibration coefficients.
5. Model runtime profiles are required whenever VLM output is used as evidence.
6. Timeout is a first-class verdict and must not block deterministic progress.


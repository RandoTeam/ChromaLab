# Phase 9 Android Model Validation Research

Date: 2026-05-20

## Source Triage

| Source | URL | Phase 9 use | Quality decision |
| --- | --- | --- | --- |
| Google AI Edge LiteRT for Android | https://ai.google.dev/edge/litert/android | Confirms LiteRT is the Android on-device runtime family and documents supported Android integration patterns. | Accepted: official Google AI Edge documentation. |
| Google AI Edge LiteRT-LM repository | https://github.com/google-ai-edge/LiteRT-LM | Confirms LiteRT-LM is intended for local edge LLM deployment and supports Android builds/benchmarking. | Accepted: maintained official repository. |
| Google AI Gemma mobile deployment | https://ai.google.dev/gemma/docs/integrations/mobile | Confirms mobile deployment paths for Gemma-family models and reinforces local model handling. | Accepted: official Google AI documentation. |
| ML Kit Text Recognition v2 Android | https://developers.google.com/ml-kit/vision/text-recognition/v2/android | Confirms local OCR is available on Android and should remain the first deterministic text reader for tick crops. | Accepted: official Google Developers documentation. |
| Android app-specific storage | https://developer.android.com/training/data-storage/app-specific | Supports app-private model storage and scoped export decisions. | Accepted: official Android documentation. |
| Android Perfetto tool | https://developer.android.com/tools/perfetto | Supports future deeper runtime profiling through ADB. | Accepted: official Android documentation. |
| Perfetto tracing docs | https://perfetto.dev/docs/ | Supports system-level performance trace planning. | Accepted: official Perfetto documentation. |

Rejected sources: marketing summaries, uncited benchmark claims, Reddit reports, and unofficial model capability claims were not used for implementation decisions.

## Decisions

- Keep model files out of the repository; use app-private device model storage for validation.
- Keep ML Kit/local deterministic OCR and CV as the geometry/calibration authority.
- Use Gemma/LiteRT only after deterministic geometry/calibration in validation fixture comparisons, so model load or timeout cannot corrupt numeric evidence.
- Record model diagnostics in runtime evidence and exported manifests.
- Use bounded model calls and document Perfetto as the next profiling tool when deeper runtime traces are needed.

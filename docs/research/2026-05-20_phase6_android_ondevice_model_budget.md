# Phase 6 Research: Android On-Device Model Budget

Date: 2026-05-20

## Sources

- LiteRT Android docs: https://ai.google.dev/edge/litert/android
  - Relevance: official current Android runtime APIs and supported versions.
  - Decision: model budget entries must record backend, duration, timeout, crop/image size, and success/failure.
  - Do not adopt: running VLM as a mandatory ROI source.
- LiteRT-LM Google Developers Blog: https://developers.googleblog.com/blazing-fast-on-device-genai-with-litert-lm/
  - Relevance: current discussion of memory efficiency, context caching, structured output, and constrained decoding.
  - Decision: prefer crop-level calls, cache per image/crop, and record cache hit/miss.
  - Do not adopt: assuming structured output is reliable unless validated by schema checks.
- Android Compose touch/accessibility docs:
  - https://developer.android.com/develop/ui/compose/touch-input
  - https://developer.android.com/develop/ui/compose/accessibility
  - Relevance: if Phase 6 diagnostics become user-facing, status and review controls need accessible labeling.
  - Decision: no UI changes in this slice; future diagnostics must expose state without relying only on color.

## Runtime Budget Policy

1. Deterministic CV/OCR stages must continue when VLM is unavailable or timed out.
2. Full-image VLM is avoided when deterministic candidates are strong.
3. Local crops are preferred over full graph images.
4. Every VLM task has a bounded timeout and runtime profile.
5. FULL_ANALYSIS may run deeper checks; FAST mode should use minimal VLM.
6. Runtime profiles are evidence artifacts, not user-facing model internals.


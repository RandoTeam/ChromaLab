# Phase 9D Android Runtime Repair Research

Date: 2026-05-21

Scope: Android validation runner reliability, on-device model/runtime handling, storage/export privacy, and performance diagnostics for Phase 9D.

## Sources Reviewed

| Source | URL | Applied Decision |
| --- | --- | --- |
| Android app-specific storage | https://developer.android.com/training/data-storage/app-specific | Keep validation artifacts under app/export-specific locations and treat pulled bundles as diagnostic, not user-facing report output. |
| Android storage overview | https://developer.android.com/training/data-storage | Keep user report exports separate from raw logs and diagnostic artifacts; avoid broad storage assumptions. |
| Google AI Edge LiteRT Android | https://ai.google.dev/edge/litert/android | Model-enabled validation must report model availability/load status and must not be required for deterministic graph geometry. |
| Android Perfetto tooling | https://developer.android.com/tools/perfetto | Phase 9D records stage timings and logcat excerpts; deeper Perfetto tracing remains a later performance-hardening task. |
| Perfetto Android tracing | https://perfetto.dev/docs/getting-started/system-tracing | Real-device runtime profiling can be expanded with traces once runtime correctness blockers are closed. |

## Resulting Constraints

- E2B/Gemma output remains advisory for OCR/semantic/judge stages.
- Deterministic graphPanel, plotArea, tick, and calibration stages must run without model input.
- Diagnostic bundles may include logcat and app-private paths; user reports must not.
- Runtime performance is tracked through stage timings in exported manifests; Phase 9D did not optimize beyond blocker repair.

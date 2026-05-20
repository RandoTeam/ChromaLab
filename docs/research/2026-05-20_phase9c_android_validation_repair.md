# Phase 9C Android Validation Repair Research

Date: 2026-05-20

Scope: Android validation runner reliability, local model/runtime diagnostics, export storage, and performance profiling for multi-fixture chromatogram validation.

## Sources Reviewed

| Source | URL | Quality / status | Phase 9C decision |
| --- | --- | --- | --- |
| Android Debug Bridge documentation | https://developer.android.com/guide/developing/tools/adb.html | Official Android documentation, current. | Keep validation fixture suite launchable through explicit `adb shell am start` actions with string extras. |
| Android `adb` tool reference | https://developer.android.com/tools/adb | Official Android tooling documentation, current. | Keep suite runner output and logcat collection script-driven so camera/gallery navigation is not required. |
| Android app-specific files / scoped storage | https://developer.android.com/training/data-storage/files/external | Official Android storage documentation, current. | Keep validation exports under the documented ChromaLab validation artifact directory and avoid broad storage assumptions in product reports. |
| Android data and file storage overview | https://developer.android.com/guide/topics/data/data-storage.html | Official Android documentation, current. | Keep diagnostic artifacts separate from user-facing reports; do not expose raw logs in normal report exports. |
| LiteRT for Android | https://ai.google.dev/edge/litert/android | Official Google AI Edge documentation, current. | Treat model loading/inference as local, bounded runtime work; model availability must be reported, not assumed. |
| LiteRT-LM repository | https://github.com/google-ai-edge/LiteRT-LM | Maintained upstream repository. | Gemma E2B/E4B model behavior must be validated on device; no model output may become deterministic geometry/calibration authority. |
| Perfetto Android tooling | https://developer.android.com/tools/perfetto | Official Android performance tooling documentation, current. | Continue collecting stage timings in validation artifacts; Perfetto remains optional for deeper Phase 10+ profiling. |

## Synthesis

- Fixture validation should remain acquisition-free and repeatable through ADB actions.
- Export artifacts must be complete enough to diagnose graph-stage failures without committing pulled raw logs or private device paths.
- LiteRT/Gemma availability must be diagnostic evidence only. Model-enabled mode can add semantic/OCR evidence after deterministic geometry/calibration, but cannot erase deterministic candidates or alter numeric metrics.
- Timing collection in per-stage artifacts is sufficient for Phase 9C triage; full Perfetto traces are useful later if runtime stalls remain after correctness blockers are closed.

## Rejected Sources

Marketing-only posts, Reddit anecdotes, and uncited benchmark claims were not used for implementation decisions. They can hint at device/runtime risk but cannot define acceptance rules.

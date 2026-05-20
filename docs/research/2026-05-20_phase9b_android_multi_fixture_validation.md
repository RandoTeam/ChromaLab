# Phase 9B Android Multi-Fixture Validation Research

Date: 2026-05-20

## Source Triage

| Source | URL | Use | Quality |
| --- | --- | --- | --- |
| Android Debug Bridge docs | https://developer.android.com/tools/adb | Supports the fixture runner using `adb shell am start` for explicit validation intents and `adb pull` for artifact collection. | Official Android documentation; accepted. |
| Android app-specific storage docs | https://developer.android.com/training/data-storage/app-specific | Confirms app-private model storage is appropriate for model assets and device-side validation setup, while exported reports use explicit shared/report paths. | Official Android documentation; accepted. |
| Android data/file storage overview | https://developer.android.com/training/data-storage/ | Supports separating app-private data, shared exports, and diagnostic artifacts. | Official Android documentation; accepted. |
| Android Perfetto docs | https://developer.android.com/tools/perfetto | Supports future deeper timing/memory traces if Phase 9B runtime blockers need profiling beyond exported stage timings. | Official Android documentation; accepted. |
| LiteRT Android docs | https://ai.google.dev/edge/litert/android | Supports LiteRT as the on-device inference backend and reinforces explicit local model deployment. | Official Google AI Edge documentation; accepted. |

## Decisions

- Use an ADB-driven suite runner instead of camera/gallery navigation, because Phase 9B validates the autonomous pipeline after image acquisition and must avoid picker ambiguity.
- Keep model assets in app-private storage for validation-package discovery; do not introduce cloud download or external lookup.
- Pull evidence artifacts to local `artifacts/phase9b-multi-fixture-android/` and keep bulky artifacts uncommitted.
- Treat exported stage timings as sufficient for Phase 9B blocker classification; reserve Perfetto traces for later performance tuning when a runtime stage exceeds budget.

## Rejected Sources

Weak blogs, Stack Overflow snippets, and generic ADB command cheat sheets were not used as implementation drivers. Official Android and Google AI Edge documentation was sufficient for the validation runner and storage/performance policy.

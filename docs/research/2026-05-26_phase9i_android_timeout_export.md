# Phase 9I Android Timeout / Export Research

Date: 2026-05-26

## Purpose

Phase 9I needed current implementation context for Android validation timeouts, export reliability, and OCR crop constraints while avoiding any weakening of chromatographic validators.

## Sources Checked

- Android Developers, Kotlin coroutines guidance: structured coroutine work should be cancellable and owned by an appropriate scope. This supports bounded validation waits and explicit terminal failure evidence rather than indefinite runner hangs.
- Android Developers, shared storage / MediaStore Downloads: Android 10+ stores downloaded files through `MediaStore.Downloads`; validation artifacts should remain under the app-controlled ChromaLab validation export root and not leak into normal user reports.
- Google ML Kit Text Recognition v2 for Android: text recognition quality depends on sufficient image resolution/focus, and text is returned with structured blocks/lines/elements. This supports keeping OCR evidence as text/box evidence only, with deterministic geometry owning calibration pixels.

URLs:

- https://developer.android.com/kotlin/coroutines/coroutines-adv
- https://developer.android.com/training/data-storage/files/media
- https://developers.google.com/ml-kit/vision/text-recognition/v2/android

## Phase 9I Application

- The validation runner timeout was increased to avoid false no-export outcomes on slow real-device runs.
- Terminal no-export remains invalid and is covered by regression tests.
- OCR/VLM text remains non-authoritative for graph/tick pixel geometry and calibration coefficients.
- Validation exports remain diagnostic artifacts and are not treated as user report content.

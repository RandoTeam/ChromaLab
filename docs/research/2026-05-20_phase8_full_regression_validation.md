# Phase 8 Full Regression Validation Research

Date: 2026-05-20

## Research Scope

Phase 8 needs current guidance for real-device regression, performance capture, privacy-safe evidence export, accessible report status presentation, and provenance records. Model knowledge may be outdated, so only official documentation and standards are used as decision drivers.

## Sources Used

| Source | Type | Use in Phase 8 |
| --- | --- | --- |
| Android Developers, [Test from the command line](https://developer.android.com/studio/test/command-line) | Official current Android documentation | Defines real-device/emulator command-line validation using `adb shell am instrument`, instrumentation options, and connected device execution. |
| Android Developers, [Write a Macrobenchmark](https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview) | Official current Android performance documentation | Supports Phase 8 timing policy: runtime/performance artifacts should include benchmark-style JSON summaries and traces when device benchmarks are available. |
| Android Developers, [Data and file storage overview](https://developer.android.com/training/data-storage/) | Official current Android privacy/storage documentation | Supports export separation: app-private evidence stays internal by default, shareable reports use explicit user-facing export surfaces, and hardcoded file paths are avoided. |
| Android Developers, [FileProvider](https://developer.android.com/reference/androidx/core/content/FileProvider.html) | Official AndroidX API documentation | Supports secure share/export policy: report sharing should use `content://` URIs rather than exposing filesystem paths. |
| Android Developers, [Accessibility in Jetpack Compose](https://developer.android.com/codelabs/jetpack-compose-accessibility) | Official Compose accessibility codelab | Confirms status controls and report navigation must expose content descriptions/state labels and maintain 48dp touch targets for interactive review actions. |
| Android Developers, [Compose accessibility API defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults) | Official current Compose documentation | Confirms status labels must not rely only on color and that Compose/Material APIs provide minimum touch target defaults that still need verification for custom report controls. |
| W3C, [PROV namespace and family](https://www.w3.org/ns/prov) | Official standard namespace | Supports evidence package provenance: each report artifact should link entity, activity, agent/model, and source data records in a machine-readable audit trail. |

## Source Quality Triage

- Accepted: official Android/Compose documentation, AndroidX API reference, and W3C PROV standards.
- Rejected as implementation drivers: blogs, marketing claims, unofficial benchmark numbers, and examples that do not state current Android/Compose behavior.

## Decisions For Phase 8

1. Real-device validation must use a connected device or emulator and record the exact command, device name, report/evidence artifacts, and relevant log excerpt. If no device is attached, Phase 8 cannot be claimed as full real-device closed.
2. Regression summaries should be structured JSON plus human-readable Markdown so CI, reviewers, and Product Acceptance can inspect the same evidence.
3. Export privacy must separate `USER_REPORT`, `TECHNICAL_EVIDENCE`, and `DIAGNOSTIC_BUNDLE`; raw prompts, raw logs, and never-shared artifacts must not enter user reports.
4. Performance data should record total duration plus stage timings for graph ROI, VLM/OCR, calibration, trace extraction, peak calculation, and report rendering.
5. Accessibility acceptance should verify that report gate statuses are text labels, not color-only chips.
6. Provenance should track source image, normalized image, graph/plot decisions, calibration, trace, peak evidence, model/runtime profiles, Knowledge Pack entry IDs, and validator outputs.

## Adoption / Rejection Notes

- Adopted command-line Android validation guidance, but no connected device was available in the current workspace at audit time.
- Adopted Android storage/FileProvider guidance for export safety; no new storage or sharing implementation is introduced in Phase 8.
- Adopted W3C PROV as provenance vocabulary guidance; Phase 8 records provenance requirements and tests contract presence without introducing a new provenance dependency.

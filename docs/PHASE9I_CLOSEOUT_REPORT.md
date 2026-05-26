# Phase 9I Closeout Report

Date: 2026-05-26

Verdict: `PHASE_9B_BLOCKED_RUNTIME_FAILURE`

Phase 10 may start: **No**

## Task Classification

Android real-device runtime blocker closure, timeout/export reliability, graph/tick/calibration evidence, TIC + ion layout review, E2B advisory safety, QA regression, product acceptance.

## Agents Activated

| Agent | Output |
| --- | --- |
| Orchestrator | Scoped Phase 9I to `bench_01` and `bench_05`; Phase 10 remained blocked. |
| Android Performance & On-Device AI | Fixed suite timeout budget and verified 16/16 final Android exports. |
| Geometry / Calibration Core | Preserved calibration gates and added precise Y calibration failure messaging. |
| OCR / VLM / Text Semantics | Confirmed OCR/VLM did not become geometry or calibration authority. |
| VLM Evaluation | Confirmed E2B did not regress deterministic behavior in target or full reruns. |
| Trace Extraction / Peak Review | Confirmed trace/peak metrics are not generated from invalid calibration. |
| Scientific Reporting & Validation | Kept `BLOCKED` where Y calibration evidence is insufficient or direction-inconsistent. |
| Chromatography SME | Confirmed `bench_05` expected graph count remains 4 pending real layout repair. |
| QA / Regression | Added missing-export regression coverage and ran targeted plus full Android suites. |
| Product Acceptance | Rejected Phase 9 closure because supported fixtures remain BLOCKED. |
| Security & Privacy | Confirmed artifacts are validation diagnostics and not committed as user reports. |

## Skills Used

`real-device-validation`, `android-runtime-profiling`, `timeout-cache-design`, `thermal-memory-guardrails`, `geometry-calibration-robust-fit`, `ocr-local-crops`, `vlm-safe-assistant`, `evidence-package-validator`, `regression-benchmark-golden`, `trace-extraction-masks`, `peak-review-integration`, `chromatography-domain-review`, `peak-metric-semantics`, `secure-export-review`, `log-safety-audit`, `test-plan-authoring`, `definition-of-done`, `current-web-research-deep`, `source-quality-triage`, `research-synthesis`.

## Research Notes

Created `docs/research/2026-05-26_phase9i_android_timeout_export.md`.

## Changes

- Increased Android validation suite timeout default from 240 seconds to 360 seconds.
- Added precise timeout text classification for `timed out` failures.
- Added precise Y calibration failure messages for insufficient anchors, invalid/review fit status, and direction-inconsistent Y fits.
- Added regression coverage for missing terminal export manifests.

## bench_01 Result

The previous deterministic timeout/no-export condition is closed. The final Phase 9I deterministic and E2B runs both exported complete terminal artifacts.

Remaining blocker:

- `TICK_LOCALIZATION_FAILURE`
- first failing stage: `Y_CALIBRATION`
- cause: insufficient Y calibration anchors after OCR/tick pairing

## bench_05 Result

The failure class is now more precise:

- `CALIBRATION_FAILURE`
- first failing stage: `Y_CALIBRATION`
- cause: Y calibration evidence is review-grade but direction-inconsistent

The fixture remains blocked because producing calibrated signal/peak metrics from this Y fit would be scientifically unsafe.

## Android Rerun

Final full Android rerun:

```text
artifacts/phase9i-final-android/
```

Summary:

- 8 fixtures x 2 modes.
- 16/16 exports complete.
- No app crash.
- No missing RuntimeEvidencePackage.
- No missing validator JSON/Markdown.
- No E2B regression observed.
- `bench_01` and `bench_05` remain `BLOCKED`.

## Product / QA / Scientific Decision

- Product: blocks Phase 9 acceptance because two supported fixtures remain blocked.
- QA: export reliability improved, but runtime blockers remain reproducible.
- Scientific: blocks converting either target fixture to REVIEW because calibration evidence is incomplete/unsafe.

## Final Verdict

`PHASE_9B_BLOCKED_RUNTIME_FAILURE`

Phase 10 must not start.

# Phase 9I Bench 01 / Bench 05 Audit

Date: 2026-05-26

## Scope

Phase 9I inspected the remaining Android runtime blockers after Phase 9H:

- `bench_01_mz71_screenshot_page`
- `bench_05_tic_plus_ions`

The audit used actual Android artifacts from Phase 9H, Phase 9I pre-audit reruns, targeted Phase 9I reruns, and the final all-fixture Phase 9I suite. It did not rely on summary text alone.

## Activated Agents

| Agent | Audit output |
| --- | --- |
| Orchestrator | Kept scope limited to bench_01/bench_05 and blocked Phase 10. |
| Android Performance & On-Device AI | Identified bench_01 deterministic no-export as runner timeout budget, then verified export with longer wait. |
| Geometry / Calibration Core | Reviewed graphPanel/plotArea, tick evidence, calibration strategy selection, and failure classes. |
| OCR / VLM / Text Semantics | Confirmed OCR/VLM text did not become calibration geometry. |
| VLM Evaluation | Verified E2B did not degrade deterministic graph count, failure class, or gate for the target fixtures. |
| Trace Extraction / Peak Review | Confirmed trace/peak stages are not reached for blocked target fixtures because calibrated signal conversion is unavailable. |
| Scientific Reporting & Validation | Required blocked status where usable Y calibration evidence is absent or direction-inconsistent. |
| Chromatography SME | Reviewed bench_05 TIC + ion layout expectation. |
| QA / Regression | Added missing-export regression coverage and ran targeted/all-fixture Android reruns. |
| Product Acceptance | Blocked Phase 9 acceptance because supported fixtures still end BLOCKED. |
| Security & Privacy | Verified validation artifacts remain diagnostic artifacts, not user report data. |

## bench_01_mz71_screenshot_page

| Item | Deterministic | E2B |
| --- | --- | --- |
| Phase 9H symptom | Runner timeout waiting for manifest; no pulled export | `BLOCKED` with export |
| Phase 9I final run | `bench_01_mz71_screenshot_page_20260526_184458` | `bench_01_mz71_screenshot_page_20260526_184601` |
| Export status | Complete | Complete |
| RuntimeEvidencePackage | Present | Present |
| Validator JSON/Markdown | Present | Present |
| Final report JSON/HTML/Markdown | Present | Present |
| Report gate | `BLOCKED` | `BLOCKED` |
| Validator | `REVIEW` | `REVIEW` |
| Runtime failure class | `TICK_LOCALIZATION_FAILURE` | `TICK_LOCALIZATION_FAILURE` |
| First failing stage | `Y_CALIBRATION` | `Y_CALIBRATION` |
| Failure reason | Insufficient Y calibration anchors after OCR/tick pairing | Same |
| E2B regression | None observed | N/A |

### Findings

The previous deterministic no-export condition was not reproduced after increasing the validation suite wait budget to 360 seconds. The final deterministic and E2B runs both exported runtime evidence, validator JSON/Markdown, report contract JSON, report HTML/Markdown, final screen capture, graph failure package, log summary, logcat excerpt, stage timings, model diagnostics, and artifact manifest.

The remaining blocker is scientific/runtime, not export reliability. The graph stage reaches graphPanel, plotArea, X scale evidence, and Y calibration attempt. X calibration selects `AXIS_SCALE_RESOLVER`, while Y selects `LEGACY_TICK_LOCALIZATION` but has insufficient accepted anchors. The terminal failure remains `TICK_LOCALIZATION_FAILURE`.

Evidence subreasons from the final suite:

- Tick: `INSUFFICIENT_Y_ANCHORS`, `OCR_NO_NUMERIC_TEXT`, `HIGH_RESIDUALS`
- Scale: `LABEL_SEQUENCE_NON_MONOTONIC`, `INSUFFICIENT_SCALE_ANCHORS`, `TITLE_ION_TEXT_REJECTED_AS_SCALE_LABEL`

### Root Cause

`bench_01` still lacks enough trustworthy Y-axis calibration anchors after deterministic OCR/tick pairing. The timeout/no-export symptom was a suite-runner wait budget issue; the app path now produces terminal evidence, but the calibration evidence remains insufficient.

## bench_05_tic_plus_ions

| Item | Deterministic | E2B |
| --- | --- | --- |
| Phase 9I final run | `bench_05_tic_plus_ions_20260526_185353` | `bench_05_tic_plus_ions_20260526_185451` |
| Export status | Complete | Complete |
| RuntimeEvidencePackage | Present | Present |
| Validator JSON/Markdown | Present | Present |
| Final report JSON/HTML/Markdown | Present | Present |
| Report gate | `BLOCKED` | `BLOCKED` |
| Validator | `REVIEW` | `REVIEW` |
| Runtime failure class | `CALIBRATION_FAILURE` | `CALIBRATION_FAILURE` |
| First failing stage | `Y_CALIBRATION` | `Y_CALIBRATION` |
| Failure reason | Y review evidence has invalid axis direction | Same |
| E2B regression | None observed | N/A |

### Findings

Phase 9I improved the failure classification for `bench_05`. The fixture no longer reports a generic tick-localization terminal failure when it has review-grade Y evidence. The selected Y evidence is rejected from autonomous signal conversion because its fit direction is inconsistent with chromatographic image coordinates: intensity must decrease as pixel Y increases.

Evidence subreasons from the final suite:

- Tick: `NON_MONOTONIC_TICK_VALUES`, `OCR_NO_NUMERIC_TEXT`, `HIGH_RESIDUALS`
- Scale: `LABEL_SEQUENCE_NON_MONOTONIC`, `SCALE_FIT_HIGH_RESIDUAL`, `TITLE_ION_TEXT_REJECTED_AS_SCALE_LABEL`

### Root Cause

The runtime collapses the TIC + ion fixture into one graph failure package and cannot form a valid Y calibration for that selected panel. The current Y anchors are not safe for pixel-to-intensity conversion because the fitted direction is inverted. Keeping the result BLOCKED is the correct scientific gate.

## Timeout / Export Reliability

The validation suite timeout default was increased from 240 seconds to 360 seconds. This is a runner reliability change, not an analysis shortcut. It allows slow Android validation runs to finish terminal artifact export instead of producing only a local `runner_failure.txt`.

The final Phase 9I all-fixture Android suite produced 16/16 exports. No app crash or missing RuntimeEvidencePackage was observed in the final suite.

## Product Decision

Phase 9 remains blocked. `bench_01` and `bench_05` still terminate as `BLOCKED` supported fixtures, but Phase 9I made the failures auditable and removed the no-export ambiguity for `bench_01`.

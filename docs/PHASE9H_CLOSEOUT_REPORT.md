# Phase 9H Closeout Report

Verdict: `PHASE_9H_WHITE_TIGER_FIXED_PHASE9_STILL_BLOCKED`

Phase 10 may start: no.

## Task Classification

White Tiger calibration regression closure, calibration arbitration/candidate retry repair, Android fixture rerun, E2B comparison, runtime evidence/export review, QA regression.

## Agents Activated

| Agent | Output |
| --- | --- |
| Orchestrator | Scoped Phase 9H to White Tiger regression and blocked Phase 10. |
| Geometry / Calibration Core | Identified candidate starvation before arbitration and repaired single-graph retry evaluation. |
| QA / Regression | Added regression tests and ran Android scoped/full reruns. |
| Scientific Reporting & Validation | Confirmed no fake calibration or gate weakening. |
| Android Performance & On-Device AI | Verified device rerun command and diagnosed Android bitmap recycle regression. |
| Product Acceptance | Phase 10 remains blocked because other fixtures remain blocked. |
| OCR / VLM / Text Semantics | Confirmed forbidden text remains rejected as calibration labels. |
| Security & Privacy | Kept pulled Android artifacts under ignored `artifacts/`; no bulk artifacts committed. |
| VLM Evaluation | Confirmed E2B did not regress deterministic output in reruns. |

## Skills Used

`real-device-validation`, `geometry-calibration-robust-fit`, `evidence-package-validator`, `regression-benchmark-golden`, `android-runtime-profiling`, `ocr-local-crops`, `vlm-safe-assistant`, `test-plan-authoring`, `secure-export-review`, `log-safety-audit`, `definition-of-done`, `current-web-research-deep`, `source-quality-triage`, `research-synthesis`.

## Research Notes

Created `docs/research/2026-05-26_phase9h_calibration_arbitration.md`.

## Code Changes

- Preserved rejected overlapping single-physical-graph candidates as retry alternatives.
- Kept primary resolved candidates first to avoid no-regression fixture displacement.
- Added explicit calibration selection/rejection reason enum values.
- Fixed Android `AxisDetector` bitmap recycle handling for whole-image crops.

## White Tiger Result

White Tiger is restored:

- Deterministic: `REVIEW_ONLY`, validator `REVIEW`, graph count 1.
- E2B: `REVIEW_ONLY`, validator `PASS`, graph count 1.

## No-Regression Check

Scoped no-regression fixtures:

- `bench_03_small_tic_export`: `REVIEW_ONLY` in deterministic and E2B.
- `bench_07_rotated_page_photo`: `REVIEW_ONLY` in deterministic and E2B.

## Full Suite Result

Full 8-fixture rerun was performed. Phase 9 remains blocked:

- `bench_01_mz71_screenshot_page`: deterministic timeout/no export; E2B `BLOCKED`.
- `bench_05_tic_plus_ions`: deterministic and E2B `BLOCKED`.

## Product / QA / Scientific Decision

- Product: Phase 10 remains blocked.
- QA: White Tiger regression is fixed, but full suite still has blocked cases.
- Scientific: Calibration gates remain honest; no unsupported release-ready claim was introduced.

## Validation

- `git diff --check`: passed.
- `.\gradlew.bat :composeApp:compileKotlinDesktop`: passed.
- `.\gradlew.bat :composeApp:assembleAndroidMain`: passed.
- `.\gradlew.bat :androidApp:assembleValidation`: passed.
- Calibration/multiplicity targeted tests: passed.
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.debug.*"`: passed.
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.fixtures.ChromatogramBenchFixtureTest"`: passed.
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.reports.*"`: passed.
- `.\gradlew.bat :composeApp:desktopTest --rerun-tasks`: passed.
- Android scoped rerun: passed with White Tiger fixed.
- Android full 8-fixture rerun: completed; Phase 9 still blocked.

## Final Verdict

`PHASE_9H_WHITE_TIGER_FIXED_PHASE9_STILL_BLOCKED`

Phase 10 must not start.

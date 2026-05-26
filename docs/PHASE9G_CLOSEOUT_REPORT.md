# Phase 9G Closeout Report

Verdict: `PHASE_9B_BLOCKED_RUNTIME_FAILURE`

Phase 10 may start: no.

## Task Classification

Calibration strategy ensemble, axis/tick calibration regression shield, Android multi-fixture runtime validation, runtime evidence validator, E2B advisory safety, multi-panel propagation audit, scientific/product/QA acceptance.

## Agents Activated

| Agent / Squad | Output |
| --- | --- |
| Orchestrator | Coordinated Phase 9G scope, blocked Phase 10, enforced CalculationEngine boundary. |
| Geometry / Calibration Core | Implemented deterministic calibration strategy ensemble and arbitration. |
| QA / Regression | Added regression tests and stricter validator requirements for calibration strategy evidence. |
| Scientific Reporting & Validation | Reviewed gate honesty; review-grade fits do not become release-ready. |
| OCR / VLM / Text Semantics | Confirmed VLM text remains non-authoritative and forbidden labels are rejected. |
| VLM Evaluation | Confirmed E2B/VLM cannot provide pixel geometry or numeric calibration. |
| Android Performance & On-Device AI | Reviewed suite runner and artifact export surface. |
| Chromatography SME | Reviewed multi-panel caveats; no graph-count metadata changed without signoff. |
| Product Acceptance | Blocks Phase 10 until Android rerun closes or precisely classifies runtime blockers. |
| Security & Privacy | Ensured local Android artifact tree is ignored and not committed. |

## Skills Used

`real-device-validation`, `geometry-calibration-robust-fit`, `ocr-local-crops`, `vlm-safe-assistant`, `evidence-package-validator`, `regression-benchmark-golden`, `android-runtime-profiling`, `trace-extraction-masks`, `peak-review-integration`, `chromatography-domain-review`, `peak-metric-semantics`, `report-gate-provenance`, `scientific-report-provenance`, `secure-export-review`, `log-safety-audit`, `test-plan-authoring`, `definition-of-done`, `current-web-research-deep`, `source-quality-triage`, `research-synthesis`.

## Code Result

- Added `CalibrationStrategyEnsemble`.
- Added calibration strategy/result/arbitration contracts.
- Preserved legacy tick localization as a competing deterministic strategy.
- Kept `AxisScaleResolver` active but no longer exclusive.
- Disabled frame-endpoint fallback selection pending stronger evidence.
- Exported selected/rejected calibration strategy evidence in graph-stage failure packages.
- Updated validator Markdown/JSON summaries.
- Updated Android suite summaries to include Phase 9G strategy aggregation.

## Android Rerun

Final artifact root: `artifacts/phase9g-multi-fixture-android-final2/`

| Metric | Result |
| --- | ---: |
| Fixtures | 8 |
| Modes | deterministic + E2B |
| Runs | 16 |
| Exports complete | 16 |
| Validator blocking issues | 0 |
| `REVIEW_ONLY` | 8 |
| `BLOCKED` | 8 |
| `RELEASE_READY` | 0 |

## Deterministic vs E2B

E2B did not regress deterministic graph count, calibration strategy selection, or report gate. Blocked fixtures remain blocked in both modes with matching failure classes. Review-only fixtures generally improve from validator `REVIEW` to `PASS` in E2B mode, but they are not release-ready.

## E2B Baseline Acceptance

`E2B_BASELINE` is the supported production model policy for FAST mode and weaker-device users. Phase 9G does not remove E2B, demote it to experimental-only, or treat it as optional-only. Every Android fixture rerun included deterministic baseline and E2B mode.

E2B helped on review-only fixtures by improving validator verdicts from `REVIEW` to `PASS` in model-enabled runs where deterministic geometry/report gates were already stable. E2B was neutral on blocked tick/calibration fixtures: it matched deterministic graph count, calibration strategy selection, report gate, and failure class. No final Phase 9G rerun showed E2B erasing graph candidates, reducing graph count, changing deterministic calibration strategy selection, altering peak metrics, or making a release-ready overclaim.

E2B remains advisory for graph candidate quality, plotArea warnings, axis/tick visibility warnings, multi-graph suspicion, local OCR/text classification, Knowledge Pack grounded explanations, and warning explanations. It must not create pixel coordinates, calibration coefficients, RT, height, area, FWHM, S/N, baseline, Kovats values, deterministic peak metrics, graph count decisions, or compound identities without explicit evidence.

E2B is safe enough to keep as the intended FAST/weaker-device baseline under the current advisory-only constraints, but Phase 9 still cannot be accepted because deterministic calibration/layout blockers remain. Any future E2B disagreement with deterministic geometry must preserve the deterministic candidate, mark `REVIEW`, store disagreement evidence, and avoid converting to `BLOCKED` unless deterministic evidence also fails.

## Remaining Blockers

1. `white_tiger_ion71` remains `BLOCKED` in both modes. This means the Phase 9F regression is not closed.
2. `bench_01_mz71_screenshot_page` remains `BLOCKED` due non-monotonic/high-residual tick evidence and insufficient Y anchors.
3. `bench_02_mz92_belyi_tigr` remains `BLOCKED` due insufficient X anchors.
4. `bench_05_tic_plus_ions` remains `BLOCKED` due missing plot/tick candidate evidence, now with explicit missing-candidate reason.
5. Multi-panel/report propagation is still incomplete for stacked/TIC/two-page classes.

## Acceptance

- Product Acceptance: blocks Phase 10. Supported fixtures still end in `BLOCKED`.
- QA / Regression: blocks Phase 10. Exports are complete and validator blocking issues are closed, but runtime acceptance is not met.
- Scientific Reporting / Chromatography SME: blocks Phase 10. Calibration gates remain honest, but insufficient anchors/frame evidence still prevent release/report readiness.

## Validation

- `git diff --check`: passed.
- `.\gradlew.bat :composeApp:compileKotlinDesktop`: passed.
- `.\gradlew.bat :composeApp:assembleAndroidMain`: passed.
- `.\gradlew.bat :androidApp:assembleValidation`: passed.
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.debug.*"`: passed.
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.fixtures.ChromatogramBenchFixtureTest"`: passed.
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.reports.*"`: passed.
- `.\gradlew.bat :composeApp:desktopTest --rerun-tasks`: passed.
- Android suite `phase9g_all_final2`: completed 16/16 exports with no validator blocking issues.

## Final Verdict

`PHASE_9B_BLOCKED_RUNTIME_FAILURE`

Phase 10 must not start.

## Phase 9H Follow-Up

Phase 9H fixed the immediate White Tiger regression by preserving rejected overlapping single-physical-graph candidates as retry alternatives. White Tiger now returns `REVIEW_ONLY` in deterministic and E2B modes, but Phase 9 remains blocked by other Android fixture failures.

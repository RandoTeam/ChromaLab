# Phase 9E Closeout Report

Verdict: `PHASE_9B_BLOCKED_RUNTIME_FAILURE`

Phase 10 may start: no.

## Task Classification

Graph layout semantics, deterministic tick localization, OCR tick pairing, calibration evidence, runtime evidence export, Android validation, model advisory safety, QA regression.

## Agents / Squads

| Squad | Agents | Output |
| --- | --- | --- |
| A Graph Layout Semantics | Geometry / Calibration, Chromatography SME, Product Acceptance, QA | Defined layout taxonomy, implemented deterministic `GraphLayoutClassifier`, and added page/fallback suppression in `GraphMultiplicityResolver`. |
| B Tick Localization Architecture | Geometry / Calibration, OCR/Text, Scientific Reporting, QA | Implemented `TickLocalizationPipeline`, tick subreason model, and runtime validator enforcement for tick/calibration failures. |
| C Android Runtime Evidence | Android Performance, QA, Security | Added Phase 9E suite summary fields and verified 16/16 Android exports were collected. |
| D Model Advisory Safety | VLM Evaluation, OCR/Text, Android Performance, QA | Verified model-enabled mode did not erase deterministic candidates or alter numeric geometry/calibration outcomes in the final rerun. |

## Skills Used

`real-device-validation`, `regression-benchmark-golden`, `golden-artifact-testing`, `evidence-package-validator`, `test-plan-authoring`, `definition-of-done`, `android-runtime-profiling`, `on-device-model-budgeting`, `timeout-cache-design`, `thermal-memory-guardrails`, `log-safety-audit`, `geometry-calibration-robust-fit`, `report-gate-provenance`, `ocr-local-crops`, `vlm-safe-assistant`, `vlm-evaluation-harness`, `structured-vlm-json-contract`, `vlm-hallucination-audit`, `ocr-crop-benchmark`, `trace-extraction-masks`, `peak-review-integration`, `chromatography-domain-review`, `peak-metric-semantics`, `scientific-report-provenance`, `evidence-gated-reporting`, `uncertainty-labeling`, `audit-trail-design`, `scientific-caveat-writing`, `android-storage-privacy`, `artifact-redaction`, `secure-export-review`.

## Changes Made

- Added first-class graph layout taxonomy/classes and deterministic layout classification.
- Added tick localization subreasons and runtime graph failure evidence fields.
- Validator now requires tick subreasons for tick/calibration graph-stage failures.
- Validation terminal failure logs include layout class and tick subreasons.
- Android validation suite summary includes layout class, expected graph count, report/failure-package graph counts, anchor counts, calibration status, and tick subreasons.
- Added guardrails so full-image fallback candidates do not suppress detected graph candidates when those candidates exist.
- No `CalculationEngine`, peak detection math, chromatographic math, or VLM numeric boundaries were modified.

## Android Rerun

Final rerun root: `artifacts/phase9e-multi-fixture-android-final2/`

| Outcome | Count |
| --- | ---: |
| Total runs | 16 |
| Exports complete | 16 |
| `REVIEW_ONLY` | 2 |
| `DIAGNOSTIC_ONLY` | 2 |
| `BLOCKED` | 12 |
| Validator `PASS` | 1 |
| Validator `REVIEW` | 13 |
| Validator `FAIL` | 2 |

See `docs/PHASE9E_ANDROID_RERUN_RESULTS.md` for the per-fixture table.

## Remaining Blockers

1. Tick localization remains blocking on `white_tiger_ion71`, `bench_01_mz71_screenshot_page`, `bench_02_mz92_belyi_tigr`, `bench_04_stacked_xic_resolution`, `bench_05_tic_plus_ions`, and `bench_07_rotated_page_photo`.
2. The dominant subreasons are `OCR_NO_NUMERIC_TEXT`, `INSUFFICIENT_X_ANCHORS`, `INSUFFICIENT_Y_ANCHORS`, `HIGH_RESIDUALS`, `NON_MONOTONIC_TICK_VALUES`, `TICK_MARKS_MISSING`, and `PLOT_FRAME_MISSING`.
3. Multi-panel semantics remain incomplete for stacked and TIC-plus-ion fixtures: `bench_04` and `bench_05` still detect one graph against expected four, while `bench_06` detects two graph candidates but only produces one report graph.
4. Product, QA, and Scientific acceptance all remain blocked because supported validation fixtures still end in `BLOCKED`.

## Acceptance Decision

- Product Acceptance: blocked. Autonomous production cannot proceed with 12/16 runs blocked.
- QA / Regression: blocked. Failures are better classified, but runtime acceptance criteria are not met.
- Scientific Reporting / Chromatography SME: blocked. Calibration evidence is insufficient for blocked fixtures; reports correctly avoid release-ready overclaim.

## Final Verdict

`PHASE_9B_BLOCKED_RUNTIME_FAILURE`

Phase 10 must not start.

# Phase 9F Closeout Report

Verdict: `PHASE_9B_BLOCKED_RUNTIME_FAILURE`

Phase 10 may start: no.

## Task Classification

Axis scale calibration evidence, OCR label geometry, grid/frame/projection evidence, Android real-device validation, runtime evidence validator, multi-fixture acceptance, model advisory safety, scientific/product/QA release gating.

## Agents / Squads

| Squad | Agents | Output |
| --- | --- | --- |
| A Axis Scale Resolver Architecture | Geometry / Calibration, Scientific Reporting, QA | Added `AxisScaleResolver`, evidence types, subreasons, validation tests, and runtime evidence fields. |
| B OCR Label Geometry | OCR/Text, VLM Evaluation, Geometry, QA | Added OCR element source tagging, label-band scale evidence, semantic text rejection, and tests for VLM/text rejection. |
| C Grid / Frame / Projection Evidence | Geometry, Trace/Peak, QA | Added label projection to nearest deterministic tick where available; preserved invalid status when frame/scale evidence is insufficient. |
| D Android Multi-Fixture Closure | Android Performance, QA, Product, Security | Reran all eight fixtures in deterministic and model-enabled modes; 16/16 exports were pulled. |
| E Chromatography Layout Semantics | Chromatography SME, Geometry, Product, QA | Kept graph-count expectations unchanged; documented remaining multi-panel propagation blockers. |

## Skills Used

`real-device-validation`, `geometry-calibration-robust-fit`, `ocr-local-crops`, `vlm-safe-assistant`, `evidence-package-validator`, `regression-benchmark-golden`, `android-runtime-profiling`, `trace-extraction-masks`, `peak-review-integration`, `chromatography-domain-review`, `peak-metric-semantics`, `report-gate-provenance`, `scientific-report-provenance`, `secure-export-review`, `log-safety-audit`, `test-plan-authoring`, `definition-of-done`, `current-web-research-deep`, `source-quality-triage`, `research-synthesis`.

## Code Changes

- Added `AxisScaleResolver` and axis-scale evidence contracts.
- Added OCR source tagging so VLM-derived axis text cannot become calibration geometry.
- Wired axis-label OCR into `GeometryPipelineRunner` before calibration fitting.
- Added runtime evidence/validator output for scale subreasons and evidence types.
- Extended Android suite summaries with Phase 9F scale fields.
- Added unit coverage for OCR-label-box anchors, VLM geometry rejection, and title/ion rejection.

## Android Result

Final artifact root: `artifacts/phase9f-multi-fixture-android-final/`

| Metric | Result |
| --- | ---: |
| Fixtures | 8 |
| Modes | deterministic + model_enabled |
| Runs | 16 |
| Exports complete | 16 |
| `REVIEW_ONLY` | 8 |
| `BLOCKED` | 8 |
| `RELEASE_READY` | 0 |
| E2B graph-count regression | 0 observed |

See `docs/PHASE9F_ANDROID_RERUN_RESULTS.md`.

## Remaining Blockers

1. `white_tiger_ion71` is still `BLOCKED` in both modes due insufficient Y scale anchors.
2. `bench_01_mz71_screenshot_page` is still `BLOCKED` in both modes and still reports one graph against expected two.
3. `bench_02_mz92_belyi_tigr` is still `BLOCKED` in both modes due insufficient scale anchors.
4. `bench_05_tic_plus_ions` is still `BLOCKED` with validator `FAIL` due plot-frame inconsistency and missing scale anchors.
5. Multi-panel report propagation remains incomplete for `bench_04`, `bench_05`, and `bench_06`; expected panel counts are not represented in final report graph counts.

## Acceptance Decision

- Product Acceptance: blocked. Autonomous production acceptance cannot proceed while supported fixtures still block.
- QA / Regression: blocked. Evidence/export reliability improved, but runtime acceptance criteria are not met.
- Scientific Reporting / Chromatography SME: blocked. Calibration gates are honest and should remain blocking for insufficient anchors/frame evidence.

## Final Verdict

`PHASE_9B_BLOCKED_RUNTIME_FAILURE`

Phase 10 must not start.

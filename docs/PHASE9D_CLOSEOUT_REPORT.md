# Phase 9D Closeout Report

Verdict: `PHASE_9B_BLOCKED_RUNTIME_FAILURE`

Phase 10 may start: no.

## Agents / Squads

| Squad | Agents | Output |
| --- | --- | --- |
| A Geometry / Multi-Graph | Geometry, Chromatography SME, QA | Closed the E2B zero-graph symptom on `bench_02`; multi-panel graph-count handling remains blocked. |
| B Tick / Calibration | Geometry, OCR/Text, Scientific, QA | Added crop upscaling and safer candidate ranking; tick localization remains blocked on required fixtures. |
| C E2B Regression | VLM Evaluation, Android Runtime, OCR/Text, QA | Disabled validation VLM geometry hints and deferred model loading; E2B no longer erases deterministic graph candidates. |
| D Evidence / Validator / Runtime Exports | QA, Scientific, Security, Product | Confirmed all final suite runs exported artifacts; Product/QA/Scientific still block acceptance. |

## Skills Used

`real-device-validation`, `regression-benchmark-golden`, `golden-artifact-testing`, `evidence-package-validator`, `test-plan-authoring`, `definition-of-done`, `android-runtime-profiling`, `on-device-model-budgeting`, `timeout-cache-design`, `thermal-memory-guardrails`, `log-safety-audit`, `geometry-calibration-robust-fit`, `report-gate-provenance`, `ocr-local-crops`, `vlm-safe-assistant`, `vlm-evaluation-harness`, `structured-vlm-json-contract`, `vlm-hallucination-audit`, `ocr-crop-benchmark`, `trace-extraction-masks`, `peak-review-integration`, `chromatography-domain-review`, `peak-metric-semantics`, `scientific-report-provenance`, `evidence-gated-reporting`, `uncertainty-labeling`, `audit-trail-design`, `scientific-caveat-writing`, `android-storage-privacy`, `artifact-redaction`, `secure-export-review`.

## Repairs Completed

- E2B no longer regresses `bench_02_mz92_belyi_tigr` from deterministic candidate output to zero graphs.
- Validation fixture geometry/calibration remains deterministic-first in both modes.
- All 16 final Android runs exported runtime evidence, validator JSON/Markdown, report contract JSON, report export, and manifest.
- Harmful tick hard-rejection experiment was removed after it regressed `bench_07`.

## Remaining Blockers

1. `bench_01`, `bench_04`, `bench_05`, and `bench_06` remain BLOCKED on `TICK_LOCALIZATION_FAILURE`.
2. Multi-panel graph count and panel semantics remain incomplete for stacked XIC, TIC+ions, and two-graph page fixtures.
3. `bench_02` no longer has E2B zero-graph regression, but it still records `metadataDetectedGraphCount=2` for an expected one-graph fixture.
4. Product, QA, and Scientific acceptance are blocked because required fixtures remain BLOCKED.

## Android Final Suite

Artifact root: `artifacts/phase9d-final-multi-fixture-android/`

| Outcome | Count |
| --- | ---: |
| Runs | 16 |
| Export complete | 16 |
| REVIEW_ONLY | 6 |
| DIAGNOSTIC_ONLY | 2 |
| BLOCKED | 8 |
| RELEASE_READY | 0 |

## Safety Review

- CalculationEngine was not modified.
- No chromatographic math changes were made.
- VLM/E2B remains advisory and cannot create numeric metrics.
- User-facing reports remain separated from diagnostic logs and validation artifacts.

## Final Decision

Phase 9 remains blocked. Phase 10 must not start until tick localization and multi-panel graph-count blockers are closed or formally rejected as unsupported by Product, QA, and Scientific reviewers with evidence.

# Phase 8 Full Regression Across All Graphs

Verdict at creation: `PHASE_8_REVIEW_READY_ANDROID_DEFERRED`

## Purpose

Phase 8 establishes the full autonomous regression gate for ChromaLab. It does not change `CalculationEngine`, chromatographic math, peak detection math, VLM boundaries, or image-analysis algorithms. It formalizes the dataset, failure taxonomy, evidence artifacts, report goldens, and validation commands needed to prove whether `AUTONOMOUS_PRODUCTION` works across known real-image classes.

## Mandatory Start Sequence

| Item | Phase 8 decision |
| --- | --- |
| Task classification | Full regression testing, real Android image validation, visual/text goldens, runtime evidence validation, autonomous graph detection, multi-graph handling, geometry/calibration/trace/peak evidence, report contract/export validation, Knowledge/VLM grounded explanation validation, privacy/export safety, performance timing, and product acceptance. |
| Agents activated | Orchestrator, Research Intelligence, QA/Regression, Product Acceptance, Geometry/Calibration Core, Trace Extraction/Peak Review, Chromatography SME, Scientific Reporting/Validation, VLM Evaluation, OCR/VLM/Text Semantics, Android Performance/On-Device AI, Security/Privacy, Mobile UX Architect, Visual Design System, Compose/KMP UI, Accessibility/Localization. |
| Skills selected | `current-web-research-deep`, `source-quality-triage`, `research-synthesis`, `regression-benchmark-golden`, `golden-artifact-testing`, `real-device-validation`, `test-plan-authoring`, `definition-of-done`, `geometry-calibration-robust-fit`, `evidence-package-validator`, `report-gate-provenance`, `trace-extraction-masks`, `peak-review-integration`, `chromatography-domain-review`, `peak-metric-semantics`, `ocr-local-crops`, `vlm-safe-assistant`, `vlm-evaluation-harness`, `structured-vlm-json-contract`, `vlm-hallucination-audit`, `ocr-crop-benchmark`, `scientific-report-provenance`, `evidence-gated-reporting`, `uncertainty-labeling`, `audit-trail-design`, `scientific-caveat-writing`, `android-runtime-profiling`, `on-device-model-budgeting`, `timeout-cache-design`, `thermal-memory-guardrails`, `mobile-ux-flow-design`, `visual-design-system`, `scientific-ui-color-system`, `typography-scale`, `component-audit`, `contrast-touch-target-audit`, `localization-ru-en`, `android-storage-privacy`, `artifact-redaction`, `secure-export-review`, `log-safety-audit`. |
| Agents skipped | None. Phase 8 touches all mandated domains. |
| Required web research | Saved in `docs/research/2026-05-20_phase8_full_regression_validation.md`. |
| Dataset inventory | `docs/CHROMATOGRAM_REGRESSION_DATASET.md`. |
| Regression matrix | Existing matrix updated with Phase 8 acceptance rows. |
| Real-device plan | `docs/PHASE8_REAL_ANDROID_VALIDATION.md`; no device was attached to `adb` during this run. |
| Failure triage protocol | `docs/CHROMATOGRAM_FAILURE_TAXONOMY.md`. |
| Closeout conditions | Desktop/contracts/tests pass, dataset and taxonomy validate, report goldens validate, privacy rules hold, Android run is either completed or explicitly deferred by Product Acceptance. |

## Scope Boundaries

Allowed:

- Documentation for dataset, regression runner, visual goldens, failure taxonomy, Android validation, and closeout.
- Test-only Phase 8 regression inventory, summary runner, report golden assertions, privacy checks, and overclaim checks.
- Updates to regression architecture documentation.

Forbidden:

- `CalculationEngine` changes.
- Chromatographic math changes.
- Peak detection math changes.
- VLM numeric-boundary changes.
- Fixture-specific coordinate hacks.
- Treating failing data as expected without evidence and Product Acceptance signoff.

## Regression Obligations

Phase 8 must preserve:

- Phase 0 report gates and terminal-state evidence.
- Phase 1 shared contracts.
- Phase 2 ROI editor tests.
- Phase 3 calibration editor tests.
- Phase 4 trace evidence tests.
- Phase 5 peak evidence tests.
- Phase 6 VLM/Knowledge safety tests.
- Phase 7/7B report contract, citation, privacy, and golden assertions.
- `RuntimeEvidencePackageValidatorTest`.
- `ChromatogramBenchFixtureTest`.

## Closeout Result

Desktop/test-contract regression was strengthened. Real Android validation was not executed because `adb devices` returned no connected device. Phase 8 is therefore review-ready with an Android validation deferral, not fully closed for Phase 9.

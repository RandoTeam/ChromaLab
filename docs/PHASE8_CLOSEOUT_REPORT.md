# Phase 8 Closeout Report

Verdict: `PHASE_8_REVIEW_READY_ANDROID_DEFERRED`

Phase 9 may start: **No**. Real Android validation was not run because no device or emulator was attached to `adb`. Desktop/report/evidence regression was strengthened and passed, but Phase 8 is not fully closed for production acceptance.

Phase 8B update: a fixture-driven Android validation mode now exists so future validation can run without camera/gallery/photo picker ambiguity. Phase 9 still may not start until that fixture mode is executed on a real Android device and artifacts are inspected.

## Task Classification

Full regression testing, real Android image validation planning, visual/text golden artifacts, runtime evidence validation, autonomous graph detection coverage, multi-graph handling, geometry/calibration/trace/peak evidence, report contract/export validation, Knowledge/VLM grounded explanations, privacy/export safety, performance timing, product acceptance, accessibility/localization, and closeout governance.

## Agents Activated

| Agent | Inspected items | Failures found | Fixes made or deferred | Remaining risks |
| --- | --- | --- | --- | --- |
| Orchestrator | Protocol, Phase 8 scope, previous closeouts, regression docs, validation commands. | Real-device validation unavailable; Phase 8 cannot fully close. | Added Phase 8 execution docs and closeout verdict. | Phase 9 is blocked until Android evidence is produced or Product Acceptance accepts deferral. |
| Research Intelligence Agent | Official Android test, benchmark, storage, FileProvider, Compose accessibility, and W3C PROV sources. | No weak-source blockers. | Added `docs/research/2026-05-20_phase8_full_regression_validation.md`. | Research does not replace real-device evidence. |
| QA / Regression Agent | Bench fixtures, report tests, validator tests, regression matrix. | Missing formal Phase 8 dataset, failure taxonomy, and runner/golden acceptance layer. | Added dataset inventory, failure taxonomy, Phase 8 runner/golden tests, and docs. | Real-image desktop fixtures still prove diagnostic gates, not fully autonomous release readiness. |
| Product Acceptance Agent | Product mode docs, report gates, Phase 8 acceptance. | Android validation gap blocks full product acceptance. | Classified Phase 8 as review-ready, not closed. | Product may accept an Android deferral later, but it is not accepted here. |
| Geometry / Calibration Core Agent | Bench ROI/plotArea/calibration expectations and failure taxonomy. | Historical graphPanel and pseudo-graph failures needed explicit rows. | Added dataset entries for right-side crop and six pseudo-graph Android failure class. | Runtime package still has terminal state but not a first-class production failure-class field. |
| Trace Extraction / Peak Review Agent | Trace/peak evidence docs, report peak evidence tests. | Sparse trace and peak evidence failure classes needed explicit acceptance rows. | Added taxonomy and regression assertions for trace/peak evidence gates. | Manual/guided review remains fallback only. |
| Chromatography SME Agent | Peak metric semantics, compound/Kovats caveat tests. | Kovats without reference series must be diagnostic, not release/review pass. | Golden case asserts `DIAGNOSTIC_ONLY` for Kovats without reference series. | No new chromatographic math was introduced. |
| Scientific Reporting & Validation Agent | Report contract, Knowledge citations, release gates, provenance docs. | Report surfaces could pass tests while hiding some evidence gates on mobile. | Updated Compose report preview to show all evidence gate rows and visible evidence status labels. | Screenshot-level visual regression remains deferred. |
| VLM Evaluation Agent | VLM/Knowledge overclaim tests and report citation records. | VLM/Knowledge overclaims must stay blocked. | Phase 8 tests assert missing `used_entry_ids`, unsupported claims, and numeric metric attempts block release. | No VLM runtime behavior changed. |
| OCR / VLM / Text Semantics Agent | Ion/m/z/label regression docs and Knowledge rules. | Ion/channel metadata remains a historical label-confusion risk. | Dataset keeps Ion 71 / White Tiger cases explicit. | Android OCR validation still pending. |
| Android Performance & On-Device AI Agent | `adb`, runtime profile docs, model timing requirements. | No device attached; no Android timing, memory, thermal, or logcat evidence. | Added real Android validation checklist and documented blocked status. | Performance validation remains desktop/test-only. |
| Security & Privacy Agent | Report export privacy, artifact privacy classes, Android storage/export research. | Normal report exports must exclude raw logs and never-shared artifacts; broader export UI gating remains follow-up. | Phase 8 tests preserve `NEVER_SHARED_BY_DEFAULT` exclusion from report exports. | Real-device export privacy needs artifact inspection. |
| Mobile UX Architect Agent | Structured report preview and report gate visibility. | Mobile preview truncated evidence gates. | Removed gate-row truncation. | Rich multi-graph selector remains future work. |
| Visual Design System Agent | Evidence chip status visibility. | Status was semantic-only and could be interpreted as color-only. | Evidence chips now render visible status text. | Screenshot-level visual polish not baselined. |
| Compose/KMP UI Implementation Agent | Compose report preview. | Missing visible gate/status details. | Minimal UI patch only; no calculation or analysis logic touched. | No Compose screenshot test harness yet. |
| Accessibility & Localization Agent | Evidence chip semantics and status visibility. | Status labels were not visible. | Visible status text now matches semantic status names. | Full TalkBack pass remains real-device work. |

## Skills Used

`current-web-research-deep`, `source-quality-triage`, `research-synthesis`, `regression-benchmark-golden`, `golden-artifact-testing`, `real-device-validation`, `test-plan-authoring`, `definition-of-done`, `geometry-calibration-robust-fit`, `evidence-package-validator`, `report-gate-provenance`, `trace-extraction-masks`, `peak-review-integration`, `chromatography-domain-review`, `peak-metric-semantics`, `ocr-local-crops`, `vlm-safe-assistant`, `vlm-evaluation-harness`, `structured-vlm-json-contract`, `vlm-hallucination-audit`, `ocr-crop-benchmark`, `scientific-report-provenance`, `evidence-gated-reporting`, `uncertainty-labeling`, `audit-trail-design`, `scientific-caveat-writing`, `android-runtime-profiling`, `on-device-model-budgeting`, `timeout-cache-design`, `thermal-memory-guardrails`, `mobile-ux-flow-design`, `visual-design-system`, `scientific-ui-color-system`, `typography-scale`, `component-audit`, `contrast-touch-target-audit`, `localization-ru-en`, `android-storage-privacy`, `artifact-redaction`, `secure-export-review`, `log-safety-audit`.

## Research Notes

- `docs/research/2026-05-20_phase8_full_regression_validation.md`

## Dataset Inventory Summary

- Formal dataset: `docs/CHROMATOGRAM_REGRESSION_DATASET.md`.
- Real fixture classes covered: clean screenshot, Android screenshot/embedded graph, cropped white chart panel, mild perspective photo, strong orientation/perspective photo, title/ion text, labels inside plotArea, weak/faint peaks, dense peaks, real multi-graph pages, many ROI candidates, Ion 71 / White Tiger, bench_03, bench_04, bench_08, and known Android failures.
- Synthetic negative acceptance rows cover missing tick labels, invalid calibration, and six pseudo-graph Android failure class.

## Regression Runner Summary

- Test-only runner/golden coverage: `composeApp/src/commonTest/kotlin/com/chromalab/feature/reports/Phase8FullRegressionAcceptanceTest.kt`.
- Output contracts validated: `phase8_regression_summary.json` and `phase8_regression_summary.md`.
- The runner validates repo-relative artifact paths, dataset status, failure class, report gate, validator references, timing fields, and report export paths.

## Visual / Golden Artifact Summary

Structured JSON/HTML/Markdown goldens cover:

- release-ready single graph;
- review-only single graph;
- diagnostic missing calibration;
- blocked missing graph evidence;
- multi-graph report;
- Knowledge/VLM grounded explanation report;
- compound hypothesis without identity evidence;
- Kovats caveat without reference series.

Screenshot-level goldens are not added in this slice and remain a future hardening item.

## Failure Taxonomy Summary

Created `docs/CHROMATOGRAM_FAILURE_TAXONOMY.md` with all Phase 8 classes, evidence requirements, severity, retry policy, Assisted Review policy, and release-block decision.

## Real Android Validation Result

Not executed. `adb devices` returned no connected device. Required checklist and artifact list are in `docs/PHASE8_REAL_ANDROID_VALIDATION.md`.

## Report Validation Result

Report package tests passed. Phase 8 adds checks for report gates, Knowledge/VLM overclaims, multi-graph per-graph evidence, terminal-state evidence packages, JSON/HTML/Markdown exports, and privacy manifest exclusions.

## Security / Privacy Validation Result

User report export artifacts still exclude `NEVER_SHARED_BY_DEFAULT`. Diagnostic evidence remains separate in the report UI contract. Real-device export inspection is still required.

## Performance Validation Result

Desktop compile/test validation passed. The Phase 8 runner records timing fields in summary contracts. Android runtime, memory, thermal, and logcat evidence were not collected because no device was attached.

## Failing Dataset Items And Decisions

| Dataset item | Decision |
| --- | --- |
| `bench_02_mz92_belyi_tigr` | Keep `REVIEW_ONLY` until Android OCR/tick/calibration evidence proves release readiness. |
| `bench_08_mz71_duplicate_candidate` | Keep `REVIEW_ONLY`; right-side crop and pseudo-graph failures remain explicit. |
| `bench_03_small_tic_export` | Keep `DIAGNOSTIC_ONLY`; low-resolution label recovery remains test-only without runtime OCR evidence. |
| `bench_06_photo_two_graphs_page` | Keep `REVIEW_ONLY`; perspective and multi-graph evidence require Android artifact run. |
| `bench_07_rotated_page_photo` | Keep `REVIEW_ONLY`; orientation correction must be verified on device. |
| `bench_04_stacked_xic_resolution` | Keep `REVIEW_ONLY`; multi-graph ordering covered by desktop fixture, release evidence pending. |
| `bench_05_tic_plus_ions` | Keep `REVIEW_ONLY`; OCR/text-classification validation pending Android run. |
| Synthetic missing tick labels | Keep `DIAGNOSTIC_ONLY`; release-ready must remain blocked. |
| Synthetic invalid calibration | Keep `DIAGNOSTIC_ONLY`; release-ready must remain blocked. |
| Synthetic six pseudo-graph failure | Keep `BLOCKED`; must not be accepted as expected behavior without Product/QA signoff. |

## Files Changed

- `composeApp/src/commonMain/kotlin/com/chromalab/feature/reports/StructuredReportPreview.kt`
- `composeApp/src/commonTest/kotlin/com/chromalab/feature/reports/Phase8FullRegressionAcceptanceTest.kt`
- `docs/PHASE8_FULL_REGRESSION_ALL_GRAPHS.md`
- `docs/CHROMATOGRAM_REGRESSION_DATASET.md`
- `docs/PHASE8_REGRESSION_RUNNER.md`
- `docs/PHASE8_VISUAL_GOLDENS.md`
- `docs/CHROMATOGRAM_FAILURE_TAXONOMY.md`
- `docs/PHASE8_REAL_ANDROID_VALIDATION.md`
- `docs/PHASE8_CLOSEOUT_REPORT.md`
- `docs/research/2026-05-20_phase8_full_regression_validation.md`
- `docs/CHROMATOGRAM_REGRESSION_MATRIX.md`
- `docs/AUTONOMOUS_PRODUCTION_ARCHITECTURE.md`
- `docs/AUTONOMOUS_ANALYSIS_EVIDENCE_GATES.md`
- `docs/agent-orchestration/phases/PHASE_08_FULL_REGRESSION_ALL_GRAPHS.md`

## Validation

Passed:

- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.reports.Phase8FullRegressionAcceptanceTest"`
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.reports.*"`
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.debug.RuntimeEvidencePackageValidatorTest"`
- `.\gradlew.bat :composeApp:compileKotlinDesktop`
- `.\gradlew.bat :composeApp:assembleAndroidMain`
- `.\gradlew.bat :composeApp:desktopTest --tests "com.chromalab.feature.processing.fixtures.ChromatogramBenchFixtureTest"`
- `git diff --check`
- `.\gradlew.bat :composeApp:desktopTest --rerun-tasks`

## Open Risks

- Real Android validation was not run.
- RuntimeEvidencePackage still has terminal state, but not a first-class structured Phase 8 failure-class field.
- Broader export workflow gating for technical/diagnostic artifacts needs Android UI validation.
- Screenshot-level visual goldens and full TalkBack checks remain future hardening work.

## Phase 9 Readiness

Phase 9 may not start from this closeout state. Required unblock: run the documented real Android validation and either close the Android evidence gap or obtain explicit Product Acceptance deferral.

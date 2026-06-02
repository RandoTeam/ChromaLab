# Agent Skill Selection Protocol

Status: MANDATORY
Owner: `AGENT_00_ORCHESTRATOR`
Applies to: every future ChromaLab Codex task after Phase 2

## Purpose

This protocol prevents ChromaLab work from being executed by one generic worker when the task requires specialist review. Every future request must begin with explicit task classification, agent selection, skill selection, scope boundaries, regression obligations, and final response requirements.

This protocol is orchestration governance only. It does not change application logic, `CalculationEngine`, geometry, OCR, VLM, reports, Android runtime, UI behavior, tests, or chromatographic math.

## Mandatory Start Sequence

Before any phase, implementation, release, validation, or non-trivial documentation task begins, the Orchestrator must record:

1. Task classification.
2. Required agent selection.
3. Required skill selection.
4. Scope boundaries.
5. Regression obligations.
6. Final response format.

No phase or implementation task may start until the Orchestrator explicitly lists:

- activated agents;
- activated skills;
- why each agent and skill was selected;
- agents intentionally not activated and why;
- validation requirements;
- regression requirements against previous phases.

## Task Classification Matrix

Classify every request into one or more domains:

| ID | Domain | Typical triggers |
| ---: | --- | --- |
| 1 | Research / current methods | methods, libraries, APIs, best practices, scientific methods, current model/runtime behavior |
| 2 | Product architecture | modes, gates, workflow ownership, product contracts |
| 3 | Mobile UX | user journey, confirmation flow, error recovery, mobile screen behavior |
| 4 | Visual design system | colors, typography, report layout, polished visual presentation |
| 5 | Compose/KMP UI implementation | Compose screen/component/state code |
| 6 | Guided workflow / state machine | guided/manual state, transitions, persistence, restoration |
| 7 | Geometry / ROI / plotArea | graphPanel, plotArea, perspective, ROI selection, multiplicity |
| 8 | Axis / tick / calibration | axes, ticks, anchors, residuals, robust fit, calibration status |
| 9 | OCR / ML Kit | local OCR crops, ML Kit recognition, text parsing |
| 10 | VLM / local model inference | Gemma, Qwen, LiteRT, GGUF, VLM prompts, model runtime behavior |
| 11 | Trace extraction | masks, skeletons, centerlines, curve reconstruction |
| 12 | Peak review / integration | peak edits, FWHM, S/N, area, baseline, integration windows |
| 13 | Chromatography domain semantics | RT meaning, Kovats/RI, baseline/noise, scientific interpretation |
| 14 | Scientific reporting / provenance | report contracts, evidence gates, caveats, audit trail |
| 15 | Runtime evidence / validator | RuntimeEvidencePackage, validator JSON/Markdown, golden artifacts |
| 16 | Android performance / memory / thermal | device runtime, image rendering performance, inference latency, thermal behavior |
| 17 | Security / privacy / storage / export | user images, logs, reports, exports, sharing, redaction |
| 18 | Accessibility / localization | RU/EN strings, content descriptions, contrast, touch targets |
| 19 | QA / regression / golden artifacts | tests, fixture matrix, benchmark acceptance, validation commands |
| 20 | Product acceptance / release gating | closeout, release decisions, beta/release readiness |

## Default Always-On Agents

For every non-trivial task, activate:

1. `AGENT_00_ORCHESTRATOR`
2. `research_intelligence_agent`
3. `qa_regression_agent`
4. `product_acceptance_agent`

Skip rules:

- Research Intelligence Agent may be skipped only for tiny formatting or documentation typo tasks that do not change technical meaning.
- QA / Regression Agent may never be skipped for code, contracts, reports, validation, UI, evidence, phase work, release work, or any task that can affect behavior or claims.
- Product Acceptance Agent is mandatory before every phase closeout and release decision.

## Domain-Specific Agent Rules

| Domain touched | Mandatory additional agents |
| --- | --- |
| UI flow, guided workflow, user steps, error recovery | `mobile_ux_architect_agent`, `compose_kmp_ui_agent`, `accessibility_localization_agent`, `product_acceptance_agent` |
| Visual style, colors, typography, report layout, visual polish | `visual_design_system_agent`, `mobile_ux_architect_agent`, `accessibility_localization_agent` |
| Compose/KMP implementation | `compose_kmp_ui_agent`; add `android_performance_ondevice_ai_agent` if image rendering, gestures, performance, or memory are involved |
| graphPanel, plotArea, ROI, perspective, axes, ticks, calibration | `AGENT_02_GEOMETRY_CALIBRATION`, `qa_regression_agent`, `scientific_reporting_validation_agent` |
| OCR, ML Kit, local crops, text classification | `AGENT_03_OCR_VLM_TEXT`, `vlm_evaluation_agent`, `research_intelligence_agent`, `qa_regression_agent` |
| VLM, Gemma, Qwen, LiteRT, GGUF, local inference | `vlm_evaluation_agent`, `android_performance_ondevice_ai_agent`, `security_privacy_agent`, `qa_regression_agent` |
| trace extraction, masks, skeletons, curve extraction | `AGENT_04_TRACE_PEAK_REVIEW`, `AGENT_02_GEOMETRY_CALIBRATION`, `qa_regression_agent` |
| peaks, integration, FWHM, S/N, baseline, Kovats, retention index, chromatographic semantics | `chromatography_sme_agent`, `scientific_reporting_validation_agent`, `AGENT_04_TRACE_PEAK_REVIEW`, `qa_regression_agent` |
| report contract, release gates, scientific claims, evidence, audit trail | `scientific_reporting_validation_agent`, `product_acceptance_agent`; add `security_privacy_agent` if reports or artifacts are exported |
| runtime evidence packages, validators, golden files, benchmarks | `qa_regression_agent`, `scientific_reporting_validation_agent`, `security_privacy_agent`, `product_acceptance_agent` |
| Android storage, images, logs, evidence export, file sharing | `security_privacy_agent`, `android_performance_ondevice_ai_agent`, `qa_regression_agent` |
| user-facing Russian/English text | `accessibility_localization_agent`, `mobile_ux_architect_agent`, `product_acceptance_agent` |

## Mandatory Skill Selection Rules

For every technical task:

- `current-web-research-deep`
- `source-quality-triage`
- `research-synthesis`

For deep method discovery before core analysis changes:

- `SKILL_16_DEEP_RESEARCH_METHOD_DISCOVERY`
- `method-comparison-matrix`
- `regression-benchmark-golden`

For UI / UX:

- `mobile-ux-flow-design`
- `interaction-state-machine`
- `error-recovery-ux`
- `compose-kmp-implementation`
- `zoom-pan-annotation-ui`
- `state-restoration`
- `contrast-touch-target-audit`
- `localization-ru-en`

For visual design:

- `visual-design-system`
- `scientific-ui-color-system`
- `typography-scale`
- `component-audit`
- `contrast-touch-target-audit`

For geometry / calibration:

- `geometry-calibration-robust-fit`
- `evidence-package-validator`
- `regression-benchmark-golden`
- `report-gate-provenance`

For OCR / VLM:

- `ocr-local-crops`
- `vlm-safe-assistant`
- `vlm-evaluation-harness`
- `structured-vlm-json-contract`
- `vlm-hallucination-audit`
- `ocr-crop-benchmark`

For trace / peaks:

- `trace-extraction-masks`
- `peak-review-integration`
- `chromatography-domain-review`
- `peak-metric-semantics`

For reports / scientific evidence:

- `scientific-report-provenance`
- `evidence-gated-reporting`
- `uncertainty-labeling`
- `audit-trail-design`
- `scientific-caveat-writing`

For Android runtime / model performance:

- `android-runtime-profiling`
- `on-device-model-budgeting`
- `timeout-cache-design`
- `thermal-memory-guardrails`

For QA:

- `golden-artifact-testing`
- `real-device-validation`
- `test-plan-authoring`
- `definition-of-done`

For security/privacy:

- `android-storage-privacy`
- `artifact-redaction`
- `secure-export-review`
- `log-safety-audit`

## Web Research Rule

Every technical, UI, UX, VLM, OCR, Android, scientific, security, or reporting task must include current web research because model knowledge may be outdated.

Research must prefer:

1. official documentation;
2. maintained repositories;
3. standards;
4. peer-reviewed or authoritative scientific sources;
5. current API/library docs.

Reject as implementation drivers:

- weak blogs;
- unsourced claims;
- outdated API examples;
- marketing claims;
- unverified benchmark claims.

Research notes must be saved under:

```text
docs/research/
```

## No Single-Agent Phase Rule

No phase can be closed if only one or two agents were used, unless the task is explicitly a tiny documentation-only typo patch.

For phases, minimum required agents are:

- `AGENT_00_ORCHESTRATOR`
- `research_intelligence_agent`
- `qa_regression_agent`
- `product_acceptance_agent`
- all domain-specific agents required by the activation matrix.

Phase closeout must list:

- all activated agents;
- agents intentionally skipped;
- skills used;
- tests run;
- regression status;
- remaining risks.

## Regression Rule

After Phase 2 and every later phase:

- rerun all previous phase acceptance checks;
- rerun guided contract tests;
- rerun relevant report/evidence gates;
- rerun full `desktopTest` unless explicitly too slow;
- if full tests are skipped, explain why and run targeted equivalent tests.

A new phase cannot close if it breaks:

- Phase 0 gates;
- Phase 1 contracts;
- Phase 2 guided ROI editor behavior;
- existing `ChromatogramBenchFixtureTest`;
- `RuntimeEvidencePackageValidatorTest`.

## VLM Boundary Rule

Any task involving VLM must restate this boundary.

VLM may:

- read local text crops;
- classify text regions;
- judge overlays;
- explain warnings;
- assist with title, ion, and axis labels.

VLM must not:

- provide numeric geometry used directly for calculations;
- calculate RT;
- calculate height;
- calculate area;
- calculate FWHM;
- calculate S/N;
- calculate baseline;
- calculate Kovats;
- create final peak metrics.

## Stop Conditions

The Orchestrator must stop or downgrade to `REVIEW_ONLY` when:

- required agents are missing;
- required skills are missing;
- current web research is required but absent;
- source quality triage is absent for research-driven decisions;
- a phase tries to start before the previous phase closeout allows it;
- the task would rewrite `CalculationEngine` without an isolated proven bug;
- VLM is proposed as numeric measurement source;
- regression requirements are skipped without justification;
- evidence/export requirements are removed or weakened.

## Future Final Response Template

Every future Codex final response must include:

1. Task classification.
2. Agents activated.
3. Skills used.
4. Research notes created or reason research was not needed.
5. Files changed.
6. Application code changed: yes/no.
7. Tests run.
8. Regression against previous phases.
9. Open risks.
10. Whether next phase may start.
11. Commit hash.

## Enforcement

- `AGENT_00_ORCHESTRATOR.md` must reference this protocol before implementation planning.
- `CODEX_REQUEST_ORDER.md` and `EXPANDED_CODEX_REQUEST_ORDER.md` must require it.
- `agent_activation_matrix.yaml` must declare it as the universal selection protocol.
- Phase closeout reports must list activated agents and skills according to this protocol.

## Definition of Done

This protocol is satisfied only when a task has:

- explicit classification;
- explicit agent and skill selection;
- explicit skipped-agent rationale when applicable;
- clear phase/scope boundaries;
- validation and regression plan;
- final response in the required format;
- no silent single-agent execution for non-trivial work.

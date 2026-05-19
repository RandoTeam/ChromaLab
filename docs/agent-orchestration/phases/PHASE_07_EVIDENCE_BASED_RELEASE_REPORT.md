# Phase 7: Evidence-Based Release Report

## Phase Name

Evidence-Based Release Report

## Product Goal

Final report only after gates pass; otherwise diagnostic/review. This phase must move ChromaLab toward honest, evidence-gated mobile chromatogram digitization without pretending fully automatic photo analysis is production-ready by default.

Product modes:
- AUTO_DIAGNOSTIC: automatic attempt, diagnostic by default, every terminal state exports evidence.
- GUIDED_PRODUCTION: reliable target workflow, user confirms graphPanel, plotArea, calibration anchors, trace, and peaks before release-quality output.
- MANUAL_ADVANCED: fallback for difficult images, user can manually define geometry, calibration, trace, and peak decisions.

## Scope

- Define or implement only the contracts and tasks assigned to this phase.
- Preserve previous phase guarantees and regression expectations.
- Produce evidence, validation notes, and closeout decisions.

## Out Of Scope

- Broad CalculationEngine rewrite.
- Fixture-specific hacks, hardcoded image coordinates, or expected peak shortcuts.
- VLM numeric measurement.
- Claiming release-quality output without required evidence.
- Starting later phase work before this phase closes.

## Required Agents

- AGENT_00_ORCHESTRATOR
- research_intelligence_agent
- qa_regression_agent
- product_acceptance_agent
- Domain agents required by `expansion/config/agent_activation_matrix.yaml`

## Required Skills

- current-web-research-deep
- source-quality-triage
- evidence-gated-reporting
- test-plan-authoring
- real-device-validation when runtime behavior is touched
- scientific-report-provenance when report semantics are touched

## Required Web Research

Current web research requirement:
Assume model knowledge is outdated. Before changing behavior or approving a technical direction, use current-web-research-deep and source-quality-triage, prefer official docs and maintained repositories, save notes under docs/research/, and document adoption/rejection rationale.

## Inputs

- Previous phase closeout reports.
- Current pipeline audit and regression matrix.
- Known Android failures and bench fixture failures.
- Existing contracts, report gates, evidence package validator output, and runtime artifacts.

## Implementation Workstreams

1. Orchestrator defines scope, forbidden changes, activated agents, and closeout gates.
2. Research Intelligence produces current source notes and method comparison.
3. Domain agents define contracts and acceptance criteria.
4. QA/Regression defines fixture, validator, and real-device checks.
5. Product Acceptance decides complete, review-only, blocked, or rework.

## Expected Files / Contracts

- Phase brief and closeout report.
- Updated protocol or contract files if this phase changes policy.
- Research notes under `docs/research/` when modern methods/APIs are involved.
- Updated regression matrix rows for new failure classes.

## Evidence Requirements

Evidence requirements:
source provenance, graphPanel and plotArea decisions, axis/tick attempts, calibration anchors and residuals, OCR/VLM crop provenance, curve masks, selected trace overlay, peak overlay, report contract JSON, validator JSON/Markdown, timings, device/runtime/model metadata, and explicit terminal status.

## VLM / LLM Boundaries

VLM/LLM boundary:
Allowed: local crop OCR, title/ion/channel/axis-label reading, text classification, overlay judging, and warning explanation.
Forbidden: exact numeric geometry for calculation, RT as final measurement, height, area, FWHM, S/N, baseline, Kovats/retention index, final peak count, or chromatographic quantitative metrics.

## UI / UX Requirements

If this phase touches guided workflow, report UX, export UX, or visual style, activate Mobile UX Architect, Visual Design System, Compose/KMP UI, Accessibility/Localization, and QA.

## Scientific / Domain Requirements

If this phase touches peak metrics, RT, baseline, S/N, area, FWHM, Kovats/retention index, or report language, activate Chromatography SME and Scientific Reporting & Validation.

## Acceptance Criteria

- Phase objective is satisfied without crossing into later phases.
- Required agents and skills are activated and recorded.
- Research notes exist where required.
- Evidence/export/report gates are not weakened.
- Existing regression guarantees remain intact or failures are classified.
- No app-code changes occur unless explicitly allowed by this phase.

## Tests

- Documentation/config: `git diff --check`, registry parse, link/file existence audit.
- Implementation: targeted tests plus the minimum regression set from Orchestrator.
- Runtime: real Android evidence package, validator JSON/Markdown, final report contract, overlays, timings, and logcat.

## Regression Requirements

After Phase 2 and every later phase, re-check all previous phase guarantees. If a full suite is skipped, record why and classify risk.

## Phase Closeout Checklist

- Research notes reviewed.
- Agent handoffs complete.
- Tests and validation recorded.
- Evidence artifacts listed.
- Risks and blocked items documented.
- Product Acceptance signoff included.
- Orchestrator decision recorded.

## Risks

- Treating diagnostic output as production-ready.
- Hiding missing geometry/calibration under polished UI.
- Allowing VLM output to become numeric truth.
- Relaxing fixtures without evidence.

## Next Phase Handoff

The next phase may start only after closeout, validation status, regression status, and unresolved risks are explicit.

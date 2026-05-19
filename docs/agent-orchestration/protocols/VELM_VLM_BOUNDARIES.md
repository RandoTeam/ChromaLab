# Velm Vlm Boundaries

## Purpose

Defines enforceable ChromaLab orchestration rules for velm vlm boundaries.

## Applicability

- All phases and prompts that reference this protocol.
- Any handoff where this protocol topic is touched.
- Documentation/configuration work and later implementation phases.

## Mandatory Rules

- State the active product mode.
- Preserve CalculationEngine unless an isolated reproducible bug is proven.
- Use current web research and source-quality triage for modern APIs, methods, models, UX, security, or scientific claims.
- Require evidence before release-quality claims.
- Record validation status and residual risk.

## Prohibited Actions

- Hardcoded image-specific logic or fixed peak counts.
- VLM numeric truth for geometry or chromatographic metrics.
- Silent diagnostic-only output without evidence.
- Weakening regression expectations to make current output pass.

## Required Artifacts

Evidence requirements:
source provenance, graphPanel and plotArea decisions, axis/tick attempts, calibration anchors and residuals, OCR/VLM crop provenance, curve masks, selected trace overlay, peak overlay, report contract JSON, validator JSON/Markdown, timings, device/runtime/model metadata, and explicit terminal status.

## Enforcement Mechanism

The Orchestrator checks this protocol at phase start, phase closeout, commit review, and product acceptance. QA/Regression records failures.

## Validation Procedure

1. Inspect changed files and active phase.
2. Verify required agents and skills.
3. Verify research notes and source triage.
4. Verify evidence package or explicit not-applicable reason.
5. Verify tests and regression status.
6. Classify PASS, REVIEW, FAIL, or BLOCKED.

## Examples

- VLM reads local crop text with crop path and confidence: allowed.
- VLM supplies peak area or RT as final measurement: prohibited.
- Clean report UI with missing calibration: diagnostic or blocked.

## Failure Handling

Stop work, write a failure note, set phase to REVIEW_ONLY or BLOCKED, and create a follow-up task owned by the right agent.

## Owner Agent

- AGENT_00_ORCHESTRATOR
- qa_regression_agent
- product_acceptance_agent

## Related Skills

- current-web-research-deep
- source-quality-triage
- evidence-gated-reporting
- test-plan-authoring

## Definition of Done

- Protocol applied.
- Required artifacts or exclusions recorded.
- No prohibited action remains.
- Acceptance and failure handling are clear.

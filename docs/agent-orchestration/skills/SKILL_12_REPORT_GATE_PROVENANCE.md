# Skill: SKILL_12_REPORT_GATE_PROVENANCE

## Skill Name

Report Gate & Provenance

## Skill Purpose

Supports report gate & provenance for ChromaLab orchestration. The skill converts a broad ChromaLab task into concrete decisions, artifacts, validation criteria, and safe handoff notes.

## When To Use

- release vs diagnostic reports
- A phase, prompt, protocol, or agent asks for this capability.
- A decision could affect release-quality analysis, user trust, scientific reporting, evidence export, or device behavior.

## When Not To Use

Do not use for unrelated cleanup, typo-only edits, or work outside orchestration. Do not use it to justify changing application behavior unless an active phase explicitly allows that implementation.

## Required Context

- User request and active phase.
- Relevant files, contracts, and registry entries.
- Product mode affected and report gate status.
- Known constraints: CalculationEngine protection, VLM boundaries, no fixture-specific fixes.

## Procedure

1. State the decision or artifact this skill must produce.
2. Confirm scope and forbidden changes.
3. Apply current-web-research-deep and source-quality-triage when current methods, APIs, libraries, UX patterns, or scientific claims are involved.
4. Inspect existing ChromaLab docs/contracts before proposing new structure.
5. Define inputs, outputs, artifacts, failure modes, and validation.
6. Cross-check against evidence gates and previous phase guarantees.
7. Produce a handoff that another agent can execute without guessing.

## Current Web Research Requirement

Current web research requirement:
Assume model knowledge is outdated. Before changing behavior or approving a technical direction, use current-web-research-deep and source-quality-triage, prefer official docs and maintained repositories, save notes under docs/research/, and document adoption/rejection rationale.

## Source Quality Requirements

- Prefer official docs, maintained repositories, peer-reviewed/domain references, and current vendor documentation.
- Do not use forum posts, marketing claims, or AI summaries as sole authority.
- Record source class, freshness, relevance, limitations, and adoption decision.

## Inputs

- Task brief and active mode.
- Files/contracts in scope.
- Prior failure classes and regression matrix entries.
- Research notes and source matrix when required.

## Outputs

- report contract gates
- Acceptance criteria and validation commands.
- Evidence artifact list.
- Risks, blockers, and handoff owner.

## Artifacts Produced

- Markdown decision note or implementation brief.
- Source matrix when research is required.
- Checklist for validator, report gate, or manual review.
- Links to generated evidence files when runtime behavior is affected.

## ChromaLab-Specific Application

Product modes:
- AUTO_DIAGNOSTIC: automatic attempt, diagnostic by default, every terminal state exports evidence.
- GUIDED_PRODUCTION: reliable target workflow, user confirms graphPanel, plotArea, calibration anchors, trace, and peaks before release-quality output.
- MANUAL_ADVANCED: fallback for difficult images, user can manually define geometry, calibration, trace, and peak decisions.

VLM/LLM boundary:
Allowed: local crop OCR, title/ion/channel/axis-label reading, text classification, overlay judging, and warning explanation.
Forbidden: exact numeric geometry for calculation, RT as final measurement, height, area, FWHM, S/N, baseline, Kovats/retention index, final peak count, or chromatographic quantitative metrics.

CalculationEngine must not be changed by this skill. If calculations look wrong, first verify image input, graphPanel, plotArea, axes/ticks, calibration, trace extraction, peak review, and report provenance.

## Validation Criteria

- Output names exact files and artifacts.
- Evidence gates cannot be skipped.
- Regression checks for previous guarantees are included.
- Diagnostic, review-grade, and release-ready output are distinguished.

## Anti-Patterns

- Generic advice without ChromaLab file or artifact references.
- Hiding weak evidence behind polished report language.
- Treating VLM output as measurement.
- Relaxing fixtures without evidence-backed justification.

## Example Use In A Task

An agent invokes `Report Gate & Provenance`, inspects active contracts, runs current research if the topic is unstable, compares options, writes a decision note, defines required artifacts, and hands off only after Orchestrator accepts the evidence plan.

## Failure Modes

- Missing source triage.
- Unclear output owner.
- No validation path.
- Evidence artifacts not named.
- Prior phase regression omitted.

## Required Handoff

```markdown
## Skill Handoff: SKILL_12_REPORT_GATE_PROVENANCE
- Decision needed:
- Sources used:
- Files/contracts affected:
- Output artifact:
- Validation:
- Risks:
- Next agent:
```

## Related Agents

- research_intelligence_agent
- qa_regression_agent
- product_acceptance_agent

## Related Skills

- current-web-research-deep
- source-quality-triage
- test-plan-authoring
- definition-of-done

## Definition of Done

- The skill produced actionable, ChromaLab-specific output.
- Required research/source triage was completed or explicitly deferred with risk.
- Evidence, validation, and handoff are sufficient for the next agent.

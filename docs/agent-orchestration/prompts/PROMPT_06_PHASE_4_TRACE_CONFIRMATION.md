# Prompt: PROMPT_06_PHASE_4_TRACE_CONFIRMATION

## Context

You are working in the ChromaLab repository using the installed agent orchestration system.

## Goal

Execute `PROMPT_06_PHASE_4_TRACE_CONFIRMATION` with Orchestrator governance and phase-specific gates.

## Scope

- Use required agents from the activation matrix.
- Keep work within the named phase or setup task.
- Produce evidence, validation, and closeout notes.

## Out Of Scope

- Application code changes when documentation/configuration-only.
- CalculationEngine rewrite.
- Geometry, OCR, VLM, Android runtime, report, UI, or scientific calculation changes unless the active phase explicitly allows them.
- Phase jumping.

## Required Agents

- AGENT_00_ORCHESTRATOR
- research_intelligence_agent
- qa_regression_agent
- product_acceptance_agent
- Additional domain agents required by activation matrix.

## Required Skills

- current-web-research-deep
- source-quality-triage
- definition-of-done
- test-plan-authoring

## Required Web Research

Current web research requirement:
Assume model knowledge is outdated. Before changing behavior or approving a technical direction, use current-web-research-deep and source-quality-triage, prefer official docs and maintained repositories, save notes under docs/research/, and document adoption/rejection rationale.

## Files To Inspect

- `docs/agent-orchestration/agents/AGENT_00_ORCHESTRATOR.md`
- `docs/agent-orchestration/expansion/config/agent_activation_matrix.yaml`
- Relevant phase, protocol, prompt, skill, and registry files.

## Required Changes

- Make only changes allowed by the active phase.
- Update docs/contracts/registries when behavior expectations change.
- Preserve previous phase guarantees.

## Tests

- Always run `git diff --check` for docs/config changes.
- Parse JSON/YAML registries when edited.
- Run Gradle/runtime tests only when product behavior changes.

## Validation

Record changed files, evidence artifacts, research notes, tests, failures, and risk classification.

## Commit Instructions

Create one focused commit. Do not stage unrelated dirty files.

## Final Response Format

Changed:
- ...

Files:
- ...

Validation:
- ...

Notes:
- ...

## Anti-Overfit Rules

No fixture-specific hacks, no hardcoded image coordinates, no VLM numeric truth, no weakened regression expectations, and no production-ready claims without evidence gates.

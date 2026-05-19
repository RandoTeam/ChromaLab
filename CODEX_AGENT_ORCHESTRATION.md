# ChromaLab Agent Orchestration

ChromaLab uses an Orchestrator + subagent workflow for future production-grade
chromatogram analysis work. The installed orchestration pack lives at:

```text
docs/agent-orchestration/
```

Core entry points:

- `docs/agent-orchestration/README.md`
- `docs/agent-orchestration/CODEX_BOOTSTRAP_PROMPT.md`
- `docs/agent-orchestration/agents/AGENT_00_ORCHESTRATOR.md`
- `docs/agent-orchestration/prompts/CODEX_REQUEST_ORDER.md`

`docs/agent-orchestration/agents/AGENT_00_ORCHESTRATOR.md` is the source of
truth for agent activation, phase gates, web research requirements, regression
policy, evidence policy, VLM boundaries, and stop conditions.

## Operating Notes

- Full-auto analysis is diagnostic, not production.
- `GUIDED_PRODUCTION` is the reliable target path.
- `MANUAL_ADVANCED` is the fallback for difficult photos, screenshots, or failed
  geometry.
- VLM is an OCR, semantic, and judge assistant only.
- VLM must not provide numeric geometry or chromatographic metrics.
- `CalculationEngine` must not be rewritten without a proven isolated bug.
- Every phase requires current web research because model knowledge may be outdated.
- Every phase requires tests and regression against previous phases.
- Every terminal runtime state must export evidence.

This file is a bootstrap note only. It does not start implementation of Phase 0 or
any later orchestration phase.

## Expanded Team Rules

- The Orchestrator must not close phases using only one or two agents when the
  phase scope requires broader review.
- Phase work must activate agents based on the activation matrix.
- Research Intelligence Agent is mandatory for all phases that require current
  technical methods.
- QA / Regression Agent is mandatory for all phases.
- Product Acceptance Agent is mandatory before phase closeout.
- Scientific Reporting & Validation Agent is mandatory for report, release gate,
  evidence, or scientific claims.
- Chromatography SME Agent is mandatory for peak metrics, chromatographic
  semantics, Kovats/retention index, baseline/noise/area interpretation.
- Mobile UX Architect and Visual Design System agents are mandatory for Guided UI,
  report UX, export UX, and visual style changes.
- VLM Evaluation Agent is mandatory for OCR/VLM/model behavior changes.
- Android Performance & On-Device AI Agent is mandatory for runtime, model,
  latency, memory, thermal, and device validation changes.
- Security & Privacy Agent is mandatory for file export, evidence packages, user
  images, logs, and report sharing.
- Accessibility & Localization Agent is mandatory for user-facing UI/report text.

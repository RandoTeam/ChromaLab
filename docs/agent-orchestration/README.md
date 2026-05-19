# ChromaLab Agent Orchestration Pack

## Purpose

This pack coordinates agents, skills, phases, protocols, prompts, templates, and registries for evidence-gated ChromaLab development.

Product modes:
- AUTO_DIAGNOSTIC: automatic attempt, diagnostic by default, every terminal state exports evidence.
- GUIDED_PRODUCTION: reliable target workflow, user confirms graphPanel, plotArea, calibration anchors, trace, and peaks before release-quality output.
- MANUAL_ADVANCED: fallback for difficult images, user can manually define geometry, calibration, trace, and peak decisions.

VLM/LLM boundary:
Allowed: local crop OCR, title/ion/channel/axis-label reading, text classification, overlay judging, and warning explanation.
Forbidden: exact numeric geometry for calculation, RT as final measurement, height, area, FWHM, S/N, baseline, Kovats/retention index, final peak count, or chromatographic quantitative metrics.

## Operating Model

1. Orchestrator owns phase boundaries, activation, gates, regression, and closeout.
2. Research Intelligence and source-quality triage are mandatory for current technical methods.
3. QA / Regression and Product Acceptance are mandatory before closeout.
4. Evidence and report gates decide release-ready, review-only, diagnostic-only, or blocked status.

## Entry Points

- `AGENTS.md`
- `CODEX_BOOTSTRAP_PROMPT.md`
- `agents/AGENT_00_ORCHESTRATOR.md`
- `prompts/CODEX_REQUEST_ORDER.md`
- `config/agent_registry.json`
- `config/skills_registry.json`
- `config/phase_registry.yaml`
- `expansion/README.md`

## Validation

Docs/config changes require `git diff --check`; config edits require registry parse validation.

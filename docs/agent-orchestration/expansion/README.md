# ChromaLab Agent & Skill Expansion Pack

This pack extends the existing ChromaLab agent orchestration system.

Purpose:
- force broader, deeper, non-stop work distribution;
- prevent Codex from using only 1-2 agents for complex phases;
- add specialist agents for research, UI/UX, visual design, chromatography, mobile performance, QA, security, documentation, and product acceptance;
- add reusable skill cards that agents can invoke per task;
- define activation rules so each phase uses the right specialists and does not close without cross-agent review.

Install target:
`docs/agent-orchestration-expansion/`

Recommended integration:
- keep the original orchestration pack;
- install this pack as an add-on;
- merge `config/additional_agent_registry.json` and `config/additional_skills_registry.json` into the existing agent/skill index or reference them from root `AGENTS.md`.

Core rule:
For any phase that changes product behavior, Orchestrator must activate at least:
1. Research Intelligence Agent
2. Domain-specific implementation agent
3. QA / Regression Agent
4. Evidence / Report Gate Agent
5. Product Acceptance Agent

For UI-facing phases, also activate:
- Mobile UX Architect
- Visual Design System Agent
- Accessibility & Localization Agent

For chromatographic/scientific phases, also activate:
- Chromatography SME Agent
- Scientific Reporting & Validation Agent

For Android/runtime/VLM phases, also activate:
- Android Performance & On-Device AI Agent
- VLM Evaluation Agent

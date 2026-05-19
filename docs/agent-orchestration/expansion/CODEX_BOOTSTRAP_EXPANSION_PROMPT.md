# Codex Bootstrap Prompt — Agent & Skill Expansion Pack

You are working in the ChromaLab repository.

An add-on ZIP pack is provided:

`chromalab_agent_skill_expansion_pack.zip`

Goal:
Install this pack as an extension to the existing ChromaLab agent orchestration system.

This is a documentation/configuration/bootstrap task only.
Do not modify application logic.
Do not modify CalculationEngine.
Do not start a phase implementation.

Steps:

1. Locate and unpack the ZIP into a temporary folder.

2. Install the pack into:

`docs/agent-orchestration-expansion/`

3. Verify these files exist:

- `docs/agent-orchestration-expansion/README.md`
- `docs/agent-orchestration-expansion/AGENTS_EXPANSION.md`
- `docs/agent-orchestration-expansion/SKILLS_EXPANSION.md`
- `docs/agent-orchestration-expansion/config/additional_agent_registry.json`
- `docs/agent-orchestration-expansion/config/additional_skills_registry.json`
- `docs/agent-orchestration-expansion/config/agent_activation_matrix.yaml`
- `docs/agent-orchestration-expansion/prompts/PHASE0_EXPANDED_ORCHESTRATION_PATCH.md`
- `docs/agent-orchestration-expansion/prompts/ORCHESTRATOR_AGENT_ACTIVATION_RULES.md`

4. Update root `AGENTS.md` carefully.

Do not overwrite existing instructions.
Add a section:

`## ChromaLab Expanded Agent Orchestration`

Reference:
- `docs/agent-orchestration-expansion/README.md`
- `docs/agent-orchestration-expansion/AGENTS_EXPANSION.md`
- `docs/agent-orchestration-expansion/SKILLS_EXPANSION.md`
- `docs/agent-orchestration-expansion/config/agent_activation_matrix.yaml`

5. Update or create root note:

`CODEX_AGENT_ORCHESTRATION_EXPANSION.md`

It must say:
- complex phases must not be handled by only 1-2 agents;
- every phase must declare activated agents and used skills;
- current web research is mandatory for every technical/design/domain phase;
- product/UI phases require UX + visual design review;
- scientific/reporting phases require chromatography SME review;
- Android/VLM/runtime phases require performance/on-device-AI review;
- every phase requires QA and regression review;
- phase closure requires cross-agent sign-off.

6. Do not begin Phase 0 implementation.
Only install and prepare.

7. Run:
- `git diff --check`

Optional:
- run Gradle only if repository policy requires it for doc/config changes.

8. Commit changes.

Suggested commit message:
`Add expanded ChromaLab agent and skill orchestration pack`

Final response:
- installed path;
- files added;
- whether root `AGENTS.md` was merged;
- whether `CODEX_AGENT_ORCHESTRATION_EXPANSION.md` was created;
- validation result;
- commit hash;
- confirmation that no application logic was modified.

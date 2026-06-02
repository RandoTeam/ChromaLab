# Deep Research Method Discovery Setup

Date: 2026-06-02

## What Changed

Added a new orchestration skill:

```text
docs/agent-orchestration/skills/SKILL_16_DEEP_RESEARCH_METHOD_DISCOVERY.md
```

It is now registered in:

- `docs/agent-orchestration/config/skills.json`
- `docs/agent-orchestration/config/skills_registry.json`
- `docs/agent-orchestration/config/skills.yaml`
- `docs/agent-orchestration/skills/SKILL_INDEX.md`

The Orchestrator now references it in:

- `docs/agent-orchestration/agents/AGENT_00_ORCHESTRATOR.md`
- `docs/agent-orchestration/protocols/AGENT_SKILL_SELECTION_PROTOCOL.md`

## Why

The current failures may be caused by deterministic graph/axis/calibration/trace
methods rather than LLM/OSR model availability. Future fixes should not proceed by
guessing. They must first run a deep method-discovery pass across maintained GitHub
repositories, papers, official docs, technical forums, Reddit, and scientific
software.

## Required Workflow Before Future Core Analysis Changes

1. Pick one pipeline stage only.
2. Inspect current ChromaLab artifacts and failing fixtures.
3. Run deep current research using `SKILL_16_DEEP_RESEARCH_METHOD_DISCOVERY`.
4. Produce a source matrix with adoption/rejection decisions.
5. Define prototype tests and acceptance gates.
6. Only then implement a focused phase.

## Bootstrap Research Note

Created:

```text
docs/research/2026-06-02_deep_method_discovery_bootstrap.md
```

Initial research candidates:

- ChartRecover for chart element detection and coordinate transformation.
- ExChart for decomposed chart extraction and VLM limitations.
- Scatteract for OCR plus robust regression.
- WebPlotDigitizer for calibration discipline.
- hplc-py for later baseline/peak review after calibration works.
- Reddit/community threads as weak signals, not implementation authority.

## Recommended Next Phase

```text
DR-1: Graph Layout and Axis Scale Deep Research
```

This should be research-only first. No geometry/calibration code changes should
start until the DR-1 source matrix and fixture audit are complete.

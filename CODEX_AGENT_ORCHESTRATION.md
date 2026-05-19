# ChromaLab Agent Orchestration

ChromaLab uses an Orchestrator + subagent workflow for future production-grade
chromatogram analysis work. The installed orchestration pack lives at:

```text
docs/agent-orchestration/
```

Core entry points:

- `docs/agent-orchestration/README.md`
- `docs/agent-orchestration/CODEX_BOOTSTRAP_PROMPT.md`
- `docs/agent-orchestration/prompts/CODEX_REQUEST_ORDER.md`

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

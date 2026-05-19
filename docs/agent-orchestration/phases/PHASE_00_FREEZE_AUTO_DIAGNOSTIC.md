# Freeze Full Auto as AUTO_DIAGNOSTIC

## Goal

Stop treating current full-auto as production path.

## Research requirement

Your model knowledge may be outdated. Before coding, search current official docs, maintained examples, and relevant methods for this phase. Save notes to `docs/research/YYYY-MM-DD_phase_00.md`.

## Active agents

- Orchestrator
- Agent 1 if UI/state affected
- Agent 2 if geometry/calibration affected
- Agent 3 if OCR/VLM affected
- Agent 4 if trace/peaks affected
- Agent 5 always for QA/evidence/release gate
- Agent 6 if Android runtime/performance/storage affected
- Agent 7 if report/provenance affected

## Tasks

1. Write implementation plan.
2. Confirm selected skills.
3. Add/update contracts.
4. Implement code.
5. Add tests.
6. Update evidence/validator if runtime state changes.
7. Update docs.
8. Produce phase closeout.

## Acceptance

- Phase-specific tests pass.
- Core compile/build tests pass.
- No hardcoded sample-specific logic.
- No VLM numeric truth leakage.
- Evidence package behavior correct.
- Report gating correct.

## Regression rule

After Phase 2 and all later phases, rerun all previous phase checks before closing. If anything regresses, reopen the relevant phase.

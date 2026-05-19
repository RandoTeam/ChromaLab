# Phase 0 Expansion Patch Prompt

Use this prompt after installing the expansion pack if Phase 0 used too few agents.

You are working in the ChromaLab repository.

Phase 0 previously used too few agents. Re-open Phase 0 as REVIEW and run an expanded cross-agent pass.

Do not modify application logic unless a specific missing gate/test/doc requires a minimal safe change.
Do not rewrite CalculationEngine.
Do not start Phase 1.

Activate at least:
- Research Intelligence Agent
- QA / Regression Agent
- Scientific Reporting & Validation Agent
- Chromatography SME Agent
- VLM Evaluation Agent
- Product Acceptance Agent
- Security & Privacy Agent if runtime evidence/export is touched
- Mobile UX Architect if UI text/mode naming is touched
- Visual Design System Agent if any UI/report styling is touched

Required checks:
1. Verify Phase 0 research notes exist and are current.
2. Verify AUTO_DIAGNOSTIC / GUIDED_PRODUCTION / MANUAL_ADVANCED are documented.
3. Verify release gates prevent false release-quality claims.
4. Verify RuntimeEvidencePackage terminal-state guarantees are documented/tested.
5. Verify VLM boundary rules are documented/tested.
6. Verify chromatographic terminology and report caveats are SME-reviewed.
7. Verify validator/report gate tests exist.
8. Verify product acceptance criteria are explicit.
9. Verify privacy/storage/export risks for evidence packages are recorded.

Create:
- docs/PHASE0_EXPANDED_AGENT_REVIEW.md
- docs/PHASE0_PRODUCT_ACCEPTANCE_REVIEW.md
- docs/PHASE0_SME_SCIENTIFIC_REVIEW.md
- docs/PHASE0_VLM_BOUNDARY_REVIEW.md
- docs/PHASE0_QA_REGRESSION_REVIEW.md

Run:
- git diff --check
- compileKotlinDesktop
- assembleAndroidMain
- desktopTest --rerun-tasks if feasible
- targeted validator/report gate tests

Final response:
- agents activated;
- skills used;
- files changed;
- tests run;
- whether Phase 0 can close;
- commit hash.

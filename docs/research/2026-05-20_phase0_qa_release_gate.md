# Phase 0 Research - QA / Evidence / Release Gate

Scope: validator requirements, regression matrix, Android export, and release gate.

## Sources Checked

- Android Storage Access Framework:
  https://developer.android.com/guide/topics/providers/document-provider
  - Relevant because users must be able to export evidence packages and reports.
  - Decision affected: evidence package export remains a release gate and future UI requirement.
  - Do not adopt: new file-provider implementation in Phase 0.

- Android shared document/file access:
  https://developer.android.com/training/data-storage/shared/documents-files
  - Relevant because runtime evidence packages should be inspectable outside the app.
  - Decision affected: real-device validation checklist must require exported JSON/Markdown/artifacts.
  - Do not adopt: broad storage permission changes now.

- SCION golden-file testing:
  https://docs.scion.org/en/latest/dev/testing/goldenfiles.html
  - Relevant because image/pipeline regressions need stable fixture outputs.
  - Decision affected: regression matrix tracks previous real failure classes.
  - Do not adopt: golden-file-only approval without semantic validator checks.

- Android ANR/performance vitals:
  https://developer.android.com/topic/performance/vitals/anr
  - Relevant because long VLM/ROI stages previously blocked without useful diagnostics.
  - Decision affected: stage timings and terminal-state evidence are required.
  - Do not adopt: treating runtime speed as more important than scientific honesty.

## Phase 0 Decisions

- Validator supports evidence package health separately from release readiness.
- Terminal states must export evidence, including failures.
- Release gate combines geometry, calibration, trace, provenance, evidence package, and report validation.

## Explicit Non-Adoptions

- No release claim without exported evidence.
- No silent `DIAGNOSTIC_ONLY`.
- No hardcoded fixture pass logic.

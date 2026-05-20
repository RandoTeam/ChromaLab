# Phase 7 Export Report Spec

Date: 2026-05-20

## Export Types

| Artifact | Privacy class | Shared by default | Purpose |
| --- | --- | --- | --- |
| `chromatogram_report.html` | USER_REPORT | Yes | Primary professional report export. |
| `chromatogram_report.md` | USER_REPORT | Yes | Portable text report export. |
| `chromatogram_report_ui_contract.json` | TECHNICAL_EVIDENCE | No | Structured report rendering contract. |
| `calculation.json` | TECHNICAL_EVIDENCE | No | Deterministic calculation data and settings. |
| `runtime_evidence_package.json` | DIAGNOSTIC_BUNDLE | No | Diagnostic evidence package. |
| `validator_report.json` | TECHNICAL_EVIDENCE | No | Machine-readable validator result. |
| `validator_report.md` | TECHNICAL_EVIDENCE | No | Human-readable validation summary. |
| raw device logs | NEVER_SHARED_BY_DEFAULT | No | Not listed in the normal report manifest; developer debugging only after explicit redaction workflow. |

## Export Rules

- User-facing reports may include visible sample/source labels but not raw logs, full prompts, or debug traces.
- Diagnostic evidence packages can contain artifact links and runtime details, but require explicit diagnostic export.
- Raw logs are never included in normal report sharing.
- `NEVER_SHARED_BY_DEFAULT` artifacts are not listed in user-facing HTML/Markdown report manifests.
- HTML and Markdown must include report gate, warnings, peak/evidence tables, provenance summaries, and model/runtime summary.

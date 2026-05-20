# Phase 0 Research - Security / Privacy / Exports

Date: 2026-05-20
Phase: Phase 0
Agents: Security & Privacy Agent, QA / Regression Agent
Skills: `current-web-research-deep`, `source-quality-triage`, `secure-export-review`, `runtime-validation`
Research confidence: HIGH for export principles; MEDIUM for later UX implementation

## Research Question

What security/privacy constraints must govern evidence packages, user images, logs, and report exports?

## Sources Reviewed

| Source | Tier | Relevance | Limitation | Decision impact |
| --- | --- | --- | --- | --- |
| [Android Storage Access Framework](https://developer.android.com/guide/topics/providers/document-provider) | Official docs | User-mediated document open/create/export path. | Phase 0 does not implement export UI changes. | Evidence package export should use user-approved storage paths in later phases. |
| [Android shared documents/files](https://developer.android.com/training/data-storage/shared/documents-files) | Official docs | Current shared storage behavior and restrictions. | Does not define ChromaLab package schema. | Evidence/report exports should avoid broad storage permissions. |
| [Android data and file storage overview](https://developer.android.com/guide/topics/data/data-storage.html) | Official docs | Distinguishes app-specific and shared storage. | General platform guidance. | Internal artifacts can remain app-specific; user export should be explicit. |
| [FDA audit trail guidance](https://www.fda.gov/regulatory-information/search-fda-guidance-documents/part-11-electronic-records-electronic-signatures-scope-and-application) | Official guidance | Audit trail and record-copy concepts. | Regulatory overhead not adopted. | Evidence packages must preserve enough provenance to reconstruct results. |

## Findings

- Evidence packages may contain original user images, paths, model metadata, raw OCR/VLM text, and logs.
- Export/share actions require explicit user intent and later redaction review.
- Auditability and privacy can conflict; Phase 0 must document this as a risk rather than silently dropping evidence.

## Phase 0 Decision

Every terminal state must require evidence export, but later phases must review redaction, storage location, and share UX before public release.

## Rejected Approaches

- Broad storage permissions as a shortcut.
- Logging private source paths or model outputs without export/privacy review.
- Skipping evidence because artifacts may contain private data.

## Required Validation

- Evidence package validator must detect missing artifacts.
- Future export tests must open/share JSON, Markdown, HTML, PDF, and artifact bundles safely.

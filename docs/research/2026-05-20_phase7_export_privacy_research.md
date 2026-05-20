# Phase 7 Export Privacy Research

Date: 2026-05-20

## Scope

Research current Android export/storage privacy guidance and apply it to ChromaLab user-facing reports, diagnostic evidence, and developer debug artifacts.

## Sources Used

| Source | URL | Quality tier | Relevant finding | Phase 7 decision |
| --- | --- | --- | --- | --- |
| Android Developers, shared documents/files | https://developer.android.com/training/data-storage/shared/documents-files | Official current Android docs | Storage Access Framework lets users choose where files are created/opened and improves user control/privacy. | User-facing exports should be explicit artifacts; diagnostic bundles must not be silently shared. |
| Android Developers, Storage Access Framework | https://developer.android.com/guide/topics/providers/document-provider | Official current Android docs | SAF exposes standard document access through user-selected providers and URIs. | Report export contract classifies shareable reports separately from diagnostic evidence and raw logs. |
| Android secure file sharing | https://developer.android.com/training/secure-file-sharing | Official Android docs | Apps should share files through controlled content URIs rather than raw filesystem paths. | Phase 7 documents raw logs as never-shared-by-default and keeps model prompts/internal traces out of user-facing reports. |

## Decisions

- HTML and Markdown are user-facing reports.
- UI contract, calculation JSON, validator JSON/Markdown are technical evidence.
- Runtime evidence package is diagnostic-only by default.
- Raw device logs are never shared by default.
- The report contract now includes artifact privacy classes and redaction policies.

## Rejected Sources

Third-party export snippets and generic cloud-reporting examples were not used.

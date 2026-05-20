# Phase 7 Report Privacy Review

Date: 2026-05-20

## Review Result

PASS with follow-up risks.

## Implemented

- Export artifacts now carry privacy class, redaction policy, and diagnostic-only marker.
- User-facing HTML/Markdown reports are separated from technical evidence and raw logs.
- Runtime evidence package is diagnostic-only by default.
- Raw device logs are marked `NEVER_SHARED_BY_DEFAULT`.

## Privacy Rules

- Do not include raw logs in user-facing reports.
- Do not expose full prompts/model internals in normal exports.
- Do not bundle raw source images or crops into user-facing reports unless the user explicitly exports evidence.
- Use Android SAF/share flows with user choice for exported artifacts.

## Remaining Risks

- Existing renderers can still display source labels and sample/instrument labels because these are user-visible report fields. Future export settings should support redacted display names.
- Desktop file sharing still logs absolute paths in platform code; this was not changed in Phase 7 because export runtime behavior was out of scope.

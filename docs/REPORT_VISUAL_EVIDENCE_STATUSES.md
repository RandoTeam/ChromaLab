# Report Visual Evidence Statuses

Status: Phase 7B hardening contract

## Purpose

Report UI, HTML, Markdown, and JSON exports use typed evidence statuses so visual report evidence is consistent and testable.

## Status Enum

`ReportVisualEvidenceStatus` values:

- `PASS`
- `REVIEW`
- `FAIL`
- `MISSING`
- `NOT_APPLICABLE`
- `AUTO_VALID`
- `AUTO_REVIEW`
- `USER_CONFIRMED`
- `USER_EDITED`
- `BLOCKED`
- `DIAGNOSTIC_ONLY`

## Evidence Surfaces

Typed statuses are used for:

- graphPanel evidence;
- plotArea and graph preparation evidence;
- calibration evidence;
- trace and curve overlay evidence;
- peak integration evidence;
- OCR/peak-label evidence;
- Knowledge Pack citation evidence;
- release gate evidence summaries.

## Rendering Rules

Ready visual chips are restricted to `PASS`, `AUTO_VALID`, `USER_CONFIRMED`, and `USER_EDITED`.

`MISSING`, `REVIEW`, `FAIL`, `BLOCKED`, and `DIAGNOSTIC_ONLY` must stay visible and must not be styled as complete evidence.

Status text must be rendered with the chip so color is not the only signal. Compose semantic labels include the status name.

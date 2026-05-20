# Autonomous Analysis Evidence Gates

## Purpose

This document defines how autonomous evidence differs from assisted/manual evidence.

## Gate Sources

| Source class | Evidence state | Meaning |
| --- | --- | --- |
| deterministic automatic stage | `VALID` | The stage passed deterministic validation and has artifacts/provenance. |
| deterministic automatic stage with uncertainty | `REVIEW` | The stage produced evidence but needs review. |
| deterministic automatic stage failed | `INVALID` or `MISSING` | Release is blocked or diagnostic-only. |
| assisted user correction | `USER_CONFIRMED` | User corrected/confirmed evidence, and provenance is recorded. |
| manual expert entry | `USER_CONFIRMED` with manual source | Expert fallback evidence with full provenance. |

## Trace Gate

An autonomous trace may be `VALID` only when:

- plotArea exists;
- trace points exist and lie inside plotArea;
- point count, column coverage, gap, confidence, contamination, and frame-touch metrics pass;
- overlay and centerline artifacts are present;
- trace source is `AUTO_EXTRACTED`;
- no blocking trace warnings remain.

An assisted trace may be `USER_CONFIRMED` only when the user explicitly accepts a valid trace in `ASSISTED_REVIEW` or `MANUAL_ADVANCED`.

Review-grade trace acceptance remains `REVIEW`, not release-ready.

## Peak Gate

An autonomous peak gate may be `VALID` only when every reportable peak has a `PeakEvidence` row with:

- linked trace/calculation provenance;
- apex point or pixel evidence;
- local maximum evidence;
- nonzero height;
- calculated area;
- valid integration boundary evidence;
- no artifact/noise rejection;
- no blocking overlap or shoulder state.

Peaks with low S/N, missing optional metrics, shoulder/overlap status, recovered-label provenance, weak baseline, or unresolved boundaries are `REVIEW`. Rejected artifact/noise peaks are `INVALID` and excluded from reportable counts.

## VLM/OCR Gate Boundary

VLM/OCR can support text and semantic gates. It cannot directly satisfy numeric geometry, calibration, trace, or peak metric gates.

## Multimodal Stage Judge Gate

Stage judge evidence is `VALID` only when task ids, crop/overlay provenance, verdicts, runtime profiles, and rejected forbidden fields are recorded. It is `REVIEW` when VLM/OCR disagrees, times out, or gives inconclusive semantic evidence. It is `INVALID` when forbidden numeric fields are accepted, crop provenance is missing, or a retry recommendation attempts to fabricate metrics or override deterministic validation.

## Evidence Package Requirement

Every terminal state must include evidence artifacts if the stage ran:

- original/normalized image;
- selected/rejected candidates;
- calibration anchors/residuals;
- OCR/VLM crops;
- masks/centerline/trace overlay;
- report contract JSON;
- validator JSON/Markdown;
- timings and runtime metadata.

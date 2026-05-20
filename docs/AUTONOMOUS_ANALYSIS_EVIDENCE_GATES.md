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
# Phase 6C Knowledge Evidence Gate

Date: 2026-05-20

Knowledge-grounded VLM/OCR/report outputs must satisfy these checks:

- `knowledge_pack_version` is recorded when knowledge was used.
- Retrieved entry IDs are present in the retrieval context.
- Scientific explanations cite `used_entry_ids`.
- `unsupported_claims` are captured and force REVIEW/REJECTED.
- Forbidden uses reject the output.
- Knowledge never creates RT, height, area, FWHM, S/N, baseline, Kovats, calibration, integration, or final compound identity.

Passing this gate does not by itself make a report RELEASE_READY. It only proves semantic explanations and text classifications are grounded safely.

## Phase 7 Report Surface Enforcement

Phase 7 makes the evidence gates visible in the professional report layer:

- Mobile, HTML, and Markdown surfaces show report gate status and release-quality claim state.
- The report UI contract exposes the full `GateEvidence` matrix and reason codes.
- Peak tables include evidence status and gate status before numeric metrics.
- User-facing exports are separated from diagnostic evidence packages and raw logs.
- Knowledge/model-only compound labels are rendered as hypotheses unless explicit identity evidence exists.
- Calculated Kovats/RI values require explicit reference-series retention times.

## Phase 8 Regression Evidence Gate

For full regression, every dataset item must have an explicit expected autonomous status and required artifact list. The release gate may pass only when these artifacts exist and agree:

- RuntimeEvidencePackage JSON;
- validator JSON and Markdown;
- final report contract JSON;
- HTML/Markdown export when report generation is available;
- graphPanel/plotArea overlays;
- axis/tick/calibration evidence;
- trace overlay;
- peak overlay and peak evidence table;
- Knowledge/VLM citation or rejection records when used;
- privacy manifest;
- stage timings and model/runtime profile.

If any terminal state lacks evidence, the dataset item is `DIAGNOSTIC_ONLY` or `BLOCKED`. If Android validation is unavailable, Phase 8 remains review-ready until the real-device checklist is executed or Product Acceptance signs off on the deferral.

## Phase 9 Model Evidence Gate

Model-enabled Android validation may pass review only when:

- deterministic graphPanel, plotArea, tick, calibration, trace, and peak evidence are still present;
- selected and executed model ids are recorded when a model loads;
- model unavailability or timeout is represented as semantic/model evidence, not as fabricated numeric evidence;
- no VLM/Knowledge output creates RT, height, area, FWHM, S/N, baseline, Kovats, calibration coefficients, or integration boundaries;
- report gate status remains review/diagnostic unless all deterministic release gates pass.

Phase 9 E2B fallback evidence meets this gate. E4B FULL_ANALYSIS remains unvalidated on the attached device.

# ChromaLab Release Gates

Release gates prevent ChromaLab from presenting incomplete automatic analysis as production science. Phase 4 realigns those gates around an autonomous-first path with assisted/manual repair as fallback.

## Gate Statuses

| Status | Meaning | User/product implication |
| --- | --- | --- |
| `RELEASE_READY` | Required evidence is valid automatically or explicitly user-confirmed/manual with provenance, and the report validator has no blocking issues. | Scientific report may be presented as release-quality for the captured evidence. |
| `REVIEW_ONLY` | Core evidence exists but at least one required or supporting gate is review-grade. | Numeric output may be shown with review warnings and evidence; not a final release claim. |
| `DIAGNOSTIC_ONLY` | Required evidence is missing, invalid, or blocked by validator findings. | Show diagnostic evidence and reasons; do not present a release-quality peak table or scientific conclusion. |
| `BLOCKED` | Pipeline failed before a report can support diagnostic interpretation. | Export failure evidence and stop. |

## Required Release Gates

A report can be `RELEASE_READY` only when all required gates pass:

| Gate | Acceptable release states | Blocks release when |
| --- | --- | --- |
| graphPanel | `VALID` or `USER_CONFIRMED` | Missing, invalid, partial, duplicate, nested, or unproven graph panel. |
| plotArea | `VALID` or `USER_CONFIRMED` | Missing, outside graphPanel, includes title/axis text as signal, or cuts trace/axes. |
| X calibration | `VALID` or `USER_CONFIRMED` | Missing, invalid, high residuals, non-monotonic anchors, or OCR-only values without tick pixels. |
| Y calibration | `VALID` or `USER_CONFIRMED` | Missing, invalid, high residuals, non-monotonic anchors, or OCR-only values without tick pixels. |
| trace | `VALID` or `USER_CONFIRMED` | Missing centerline, sparse/fragmented trace without review evidence, text/grid contamination, or weak coverage. |
| evidence package | `VALID` | Missing package, missing terminal state, missing core artifacts, or validator blocking issues. |
| source provenance | `VALID` | Missing original/normalized image path or source metadata. |

Supporting gates are still recorded:

- axis status;
- tick status;
- peak review status;
- VLM evidence status;
- user confirmation status;
- report contract validation status.

Supporting gates may produce `REVIEW_ONLY` or `DIAGNOSTIC_ONLY` depending on severity. Future phases may promote additional supporting gates to required gates if real-device validation shows the current set is insufficient.

## Evidence Gate States

| Evidence state | Meaning |
| --- | --- |
| `VALID` | Deterministic or validated automatic evidence satisfies the gate. |
| `USER_CONFIRMED` | Assisted/manual workflow captured explicit user confirmation or correction. |
| `REVIEW` | Evidence exists but is marginal, incomplete, or uncertain. |
| `INVALID` | Evidence exists and fails required quality checks. |
| `MISSING` | Evidence is absent. |
| `NOT_APPLICABLE` | Gate does not apply to this report/mode. |

## Product Mode Interaction

### AUTONOMOUS_PRODUCTION

- Primary production target.
- May become `RELEASE_READY` only if every required gate is `VALID`, the evidence package exists, source provenance is present, and validator findings are non-blocking.
- Must not use manual/user-edited evidence as automatic evidence.
- Must still disclose VLM/OCR use as auxiliary evidence only.

### AUTO_DIAGNOSTIC

- Automatic diagnostic attempt.
- Does not consume user confirmations as release evidence.
- Produces `REVIEW_ONLY`, `DIAGNOSTIC_ONLY`, or `BLOCKED` when gates are incomplete.
- Must export evidence for every terminal state.

### ASSISTED_REVIEW

- Review/repair path for failed or low-confidence autonomous stages.
- May satisfy geometry, calibration, trace, and later peak gates through explicit stored user confirmation.
- User intervention must remain visible in report provenance and evidence packages.
- Review-grade user acceptance remains `REVIEW_ONLY` unless a later explicit release policy says otherwise.

### MANUAL_ADVANCED

- Expert fallback.
- May satisfy gates through full manual geometry/calibration/trace/peak definitions.
- Manual UI and future editors must record provenance, warnings, artifacts, and audit trail.

### GUIDED_PRODUCTION

- Deprecated compatibility alias for earlier Phase 1-4 docs and serialized state.
- Treat as `ASSISTED_REVIEW` behavior in new architecture.
- Do not use it as the primary target for new phase work.

## Assisted Review Mapping

Phase 1-4 review components write confirmation contracts:

- `GraphPanelConfirmation`
- `PlotAreaConfirmation`
- `UserConfirmedCalibration`
- `UserConfirmedTrace`
- future `UserConfirmedPeakSet`

Mapping rules:

- `ASSISTED_REVIEW`, deprecated `GUIDED_PRODUCTION`, and `MANUAL_ADVANCED` can map confirmed evidence to `USER_CONFIRMED`.
- `AUTO_DIAGNOSTIC` cannot use guided/user confirmation objects as release evidence.
- `AUTONOMOUS_PRODUCTION` uses automatic `VALID` evidence, not `USER_CONFIRMED` evidence.
- evidence package and source provenance must still be `VALID`.
- review-grade corrections produce `REVIEW_ONLY`, not silent release.

## Phase 4 Trace Gate Mapping

Phase 4 now means autonomous trace extraction plus evidence review.

Mapping rules:

- automatic valid trace maps to `EvidenceGateStatus.VALID` only when trace quality is `VALID`, trace points are inside plotArea, and required overlay/centerline artifacts exist;
- user accepted valid trace maps to `EvidenceGateStatus.USER_CONFIRMED` in `ASSISTED_REVIEW` or `MANUAL_ADVANCED`;
- accepted review-grade trace maps to `EvidenceGateStatus.REVIEW`;
- rejected or invalid trace maps to `EvidenceGateStatus.INVALID`;
- missing trace maps to `EvidenceGateStatus.MISSING`;
- `AUTO_DIAGNOSTIC` ignores assisted/manual trace confirmation objects.

Phase 4 does not implement peak review. Phase 5 must add peak evidence before peak-specific claims are treated as reviewed or release-ready.

## Terminal-State Evidence Requirement

Every terminal state must export a runtime evidence package or failure evidence package:

- `PASS`
- `REVIEW`
- `FAIL`
- `DIAGNOSTIC_ONLY`
- `ROI_FAILURE`
- `CALIBRATION_FAILURE`
- `CURVE_FAILURE`
- `OCR_FAILURE`
- `VLM_TIMEOUT`
- `FATAL_PIPELINE_ERROR`

Minimum evidence:

- original image and normalized image when available;
- selected/rejected graphPanel and plotArea candidates;
- graph multiplicity decisions;
- axis/tick attempts;
- calibration anchors and residuals;
- OCR/VLM local crops and text classification;
- curve masks and selected trace overlay;
- peak overlay and peak/recovery decisions when available;
- report contract JSON when available;
- validator JSON and Markdown when available;
- stage timings;
- device/model/runtime metadata;
- terminal reason and warnings.

Validator `FAIL` with evidence is acceptable for diagnostic work. Silent failure without evidence is a blocking product defect.

## VLM Boundary

VLM/LLM is allowed only for:

- local crop OCR;
- title / ion / channel / axis-label reading;
- text classification;
- overlay judging;
- rough graph hints;
- warning explanation.

VLM/LLM is forbidden from producing:

- exact pixel geometry used for calculations;
- peak RT as final measurement;
- height;
- area;
- FWHM;
- S/N;
- baseline;
- Kovats / retention index;
- final peak count;
- chromatographic quantitative metrics.

VLM output must preserve provenance: task type, local crop or overlay path, raw output, parsed output, confidence, and rejection reason when invalid.

## Acceptance

The autonomous-first release-gate contract is accepted when:

- product modes are explicit;
- gate statuses are explicit;
- terminal states are explicit;
- VLM boundaries are explicit and tested;
- autonomous valid evidence and user-confirmed evidence are distinct;
- review/manual intervention is visible in provenance;
- current pipeline risks are documented;
- regression matrix exists and is updated by future phases.

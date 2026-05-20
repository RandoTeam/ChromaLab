# ChromaLab Release Gates

Phase 0 freezes the previous fully automatic photo/screenshot workflow as `AUTO_DIAGNOSTIC` and defines the gate contract that prevents diagnostic output from looking release-quality.

## Gate Statuses

| Status | Meaning | User/product implication |
| --- | --- | --- |
| `RELEASE_READY` | Required evidence is valid or user-confirmed, and the report validator has no blocking issues. | Scientific report may be presented as release-quality for the captured evidence. |
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
| `VALID` | Deterministic or validated evidence satisfies the gate. |
| `USER_CONFIRMED` | Future guided/manual workflows captured explicit user confirmation. |
| `REVIEW` | Evidence exists but is marginal, incomplete, or uncertain. |
| `INVALID` | Evidence exists and fails required quality checks. |
| `MISSING` | Evidence is absent. |
| `NOT_APPLICABLE` | Gate does not apply to this report/mode. |

## Product Mode Interaction

### AUTO_DIAGNOSTIC

- Diagnostic by default.
- May become `RELEASE_READY` only if every required gate is valid.
- Must export evidence for every terminal state.
- Must not hide missing evidence behind polished report UI.

### GUIDED_PRODUCTION

- Future main production path.
- May satisfy geometry, calibration, trace, and peak gates through explicit stored user confirmation.
- Phase 1 adds shared confirmation contracts and `GuidedReportGateMapper`.
- Guided UI is not implemented yet.

### MANUAL_ADVANCED

- Future fallback for hard images.
- May satisfy gates through full manual geometry/calibration/trace/peak definitions.
- Phase 1 adds the shared state contracts needed for future manual fallback.
- Manual UI is not implemented yet.

## Phase 1 Guided Mapping

Phase 1 adds:

- `GuidedDigitizationState`
- `GuidedReportGateMapper`
- geometry confirmation contracts
- calibration confirmation contracts
- trace confirmation contracts
- peak review contracts

Mapping rules:

- `GUIDED_PRODUCTION` and `MANUAL_ADVANCED` can map confirmed graphPanel, plotArea, calibration, and trace evidence to `USER_CONFIRMED`.
- `AUTO_DIAGNOSTIC` cannot use guided/user confirmation objects as release evidence.
- evidence package and source provenance must still be `VALID`.
- two-anchor calibration is structurally valid but review-grade by default until robust validation or explicit future policy.
- missing guided confirmations produce `DIAGNOSTIC_ONLY` or `BLOCKED`, never `RELEASE_READY`.

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

## Current Code Contract

The current code already includes Phase 0 gate contracts in:

- `composeApp/src/commonMain/kotlin/com/chromalab/feature/reports/Phase0ProductContracts.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/debug/RuntimeEvidencePackage.kt`
- `composeApp/src/commonMain/kotlin/com/chromalab/feature/processing/debug/RuntimeEvidencePackageValidator.kt`

Phase 0 does not rewrite `CalculationEngine`, geometry, OCR, VLM, trace extraction, peak detection, or report math.

## Acceptance

Phase 0 release gates are accepted when:

- product modes are explicit;
- gate statuses are explicit;
- terminal states are explicit;
- VLM boundaries are explicit and tested;
- current pipeline risks are documented;
- regression matrix exists;
- closeout report records validation and open risks.

Phase 1 acceptance adds contract tests for guided state transitions, serialization, calibration anchor minimums, and release-gate mapping.

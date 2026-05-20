# ChromaLab Product Modes

Phase 0 freezes the previous fully automatic photo/screenshot pipeline as `AUTO_DIAGNOSTIC`. This is a product honesty reset: the app may continue to attempt automatic analysis, but it must not silently present weak evidence as production science.

## Modes

| Mode | Purpose | Release-quality allowed? | Phase 0 state |
| --- | --- | --- | --- |
| `AUTO_DIAGNOSTIC` | Automatic attempt for quick diagnostics, candidate geometry, OCR, trace, and peak suggestions. | Only when every release gate passes. Otherwise `REVIEW_ONLY`, `DIAGNOSTIC_ONLY`, or `BLOCKED`. | Explicit contract and metadata. |
| `GUIDED_PRODUCTION` | Future main workflow where the user confirms graphPanel, plotArea, calibration anchors, trace, and peaks. | Yes, after evidence or explicit user-confirmation gates pass. | Contract only; UI is not implemented in Phase 0. |
| `MANUAL_ADVANCED` | Future fallback for hard images where the user manually defines geometry, calibration, trace, and peak decisions. | Yes, after manual evidence is captured and validated. | Contract only; UI is not implemented in Phase 0. |

## AUTO_DIAGNOSTIC Rules

`AUTO_DIAGNOSTIC`:

- may run automatically from camera, gallery, screenshot, scan, or file input;
- may generate graphPanel, plotArea, axis/tick, OCR, trace, peak, and report suggestions;
- may produce a diagnostic report;
- must export evidence for every terminal state;
- must clearly label review/diagnostic output;
- may be `RELEASE_READY` only if graphPanel, plotArea, X calibration, Y calibration, trace, evidence package, source provenance, and validator gates pass.

`AUTO_DIAGNOSTIC` must not:

- hide missing geometry, invalid calibration, sparse trace, missing overlays, or VLM failure;
- use VLM/LLM output as numeric truth;
- produce a release-quality peak table when required evidence is missing;
- use fixture hints or image-specific branches as production evidence;
- rewrite `CalculationEngine` to compensate for upstream failures.

## GUIDED_PRODUCTION Rules

`GUIDED_PRODUCTION` is the future reliable path.

The user must confirm:

- graphPanel;
- plotArea;
- X and Y calibration anchors;
- extracted trace overlay;
- peak apexes, boundaries, and review decisions;
- report evidence package.

User confirmation must be explicit, persisted, and visible in the report/evidence package as `USER_CONFIRMED`. Phase 0 does not implement this UI.

Phase 1 adds the shared contract layer for this mode:

- `GuidedDigitizationState`;
- graphPanel and plotArea confirmation contracts;
- calibration anchor and residual contracts;
- trace confirmation contracts;
- peak review decision contracts;
- guided-to-release-gate mapping.

The Phase 1 contract still does not implement the Guided UI. It only defines the state that future screens must write.

## MANUAL_ADVANCED Rules

`MANUAL_ADVANCED` is the future fallback for difficult photos, rotated scans, weak traces, missing tick labels, or failed automatic geometry.

It may allow the user to define:

- graph bounds;
- plot bounds;
- axis/tick anchors;
- calibration values;
- trace points or corrections;
- peak decisions.

Manual values must still carry provenance and review status. Phase 0 does not implement this UI.

Phase 1 defines the same confirmation contracts for future manual fallback. Manual values can satisfy release gates only when they are persisted as user-confirmed evidence and remain visible in report provenance.

## Report Mode Requirements

Every final report or evidence package must identify:

- processing mode;
- gate status;
- terminal state;
- evidence package status;
- user confirmation status if applicable;
- VLM evidence status if applicable.

Legacy `FULL_ANALYSIS` labels must not be interpreted as production-ready unless the Phase 0 gates pass.

## VLM Boundary

VLM/LLM may assist with:

- local crop OCR;
- title / ion / channel / axis-label reading;
- text classification;
- rough graph hints;
- overlay judging;
- warning explanation.

VLM/LLM must not provide:

- exact numeric geometry used for calculation;
- RT as final measurement;
- height;
- area;
- FWHM;
- S/N;
- baseline;
- Kovats / retention index;
- final peak count;
- chromatographic quantitative metrics.

## Current Status

Phase 0 defines modes and gates. Phase 1 adds shared guided/manual state contracts and tests. The app still does not implement Guided or Manual UI workflows, and full-auto is still not production-ready by default.
